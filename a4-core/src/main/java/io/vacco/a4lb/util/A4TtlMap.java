package io.vacco.a4lb.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/**
 * Bounded TTL map. A key may be re-inserted (refreshing its TTL); the queue
 * keeps insertion order for size-based eviction, while stale queue entries are
 * ignored when the key has already been replaced or removed.
 *
 * <p>Refreshing an existing entry updates its expiry without moving it in the
 * eviction queue. This keeps activity updates O(1); max-size eviction remains
 * insertion-order rather than strict LRU.</p>
 */
public class A4TtlMap<K, V> {

  public static class CacheEntry<V> {
    private final long expireBy;
    private final V entry;

    public CacheEntry(long expireBy, V entry) {
      super();
      this.expireBy = expireBy;
      this.entry = entry;
    }

    public long getExpireBy() {
      return expireBy;
    }

    public V getEntry() {
      return entry;
    }
  }

  private final Map<K, CacheEntry<V>> cache;
  private final Queue<K> queue;
  private final int maxSize;
  private final AtomicInteger cacheSize = new AtomicInteger();

  private final Consumer<V> onExpired;

  public A4TtlMap(int maxSize, Consumer<V> onExpired) {
    if (maxSize <= 0) {
      throw new IllegalArgumentException("Max size must be greater than 0");
    }
    this.onExpired = Objects.requireNonNull(onExpired);
    this.maxSize = maxSize;
    cache = new ConcurrentHashMap<>(maxSize);
    queue = new ConcurrentLinkedQueue<>();
  }

  private static boolean isExpired(CacheEntry<?> entry, long now) {
    return entry.getExpireBy() != -1 && now > entry.getExpireBy();
  }

  public V get(K key) {
    if (key == null) {
      throw new IllegalArgumentException("Invalid Key.");
    }
    var entry = cache.get(key);
    if (entry == null) {
      return null;
    }
    if (isExpired(entry, System.currentTimeMillis())) {
      if (cache.remove(key, entry)) {
        cacheSize.decrementAndGet();
        onExpired.accept(entry.getEntry());
      }
      return null;
    }
    return entry.getEntry();
  }

  /**
   * Extends the expiry of an existing entry without changing its value or
   * eviction-queue position. Returns false if the key is absent or expired.
   */
  public boolean refresh(K key, int ttlMs) {
    if (key == null) {
      throw new IllegalArgumentException("Invalid Key.");
    }
    var entry = cache.get(key);
    if (entry == null) {
      return false;
    }
    var now = System.currentTimeMillis();
    if (isExpired(entry, now)) {
      if (cache.remove(key, entry)) {
        cacheSize.decrementAndGet();
        onExpired.accept(entry.getEntry());
      }
      return false;
    }
    var expireBy = ttlMs != -1 ? now + ttlMs : ttlMs;
    return cache.replace(key, entry, new CacheEntry<>(expireBy, entry.getEntry()));
  }

  /**
   * Removes a specific key/value pair. This is useful for closing a session
   * without accidentally removing a newer session that replaced it.
   */
  public boolean remove(K key, V value) {
    if (key == null) {
      throw new IllegalArgumentException("Invalid Key.");
    }
    var entry = cache.get(key);
    if (entry != null && entry.getEntry() == value && cache.remove(key, entry)) {
      cacheSize.decrementAndGet();
      return true;
    }
    return false;
  }

  public V removeAndGet(K key) {
    if (key == null) {
      return null;
    }
    CacheEntry<V> entry = cache.remove(key);
    if (entry != null) {
      cacheSize.decrementAndGet();
      return entry.getEntry();
    }
    return null;
  }

  /**
   * Removes and notifies every entry expired at {@code now}. Entries refreshed
   * while the sweep is running are kept because removal is conditional on the
   * exact CacheEntry observed by the sweep.
   */
  public List<V> removeExpired(long now) {
    var expired = new ArrayList<V>();
    for (var e : cache.entrySet()) {
      var entry = e.getValue();
      if (isExpired(entry, now) && cache.remove(e.getKey(), entry)) {
        cacheSize.decrementAndGet();
        expired.add(entry.getEntry());
        onExpired.accept(entry.getEntry());
      }
    }
    return expired;
  }

  public void put(K key, V value, int ttlMs) {
    if (key == null) {
      throw new IllegalArgumentException("Invalid Key.");
    }
    if (value == null) {
      throw new IllegalArgumentException("Invalid Value.");
    }
    var expireBy = ttlMs != -1 ? System.currentTimeMillis() + ttlMs : ttlMs;
    var previous = cache.put(key, new CacheEntry<V>(expireBy, value));
    if (previous == null) {
      cacheSize.incrementAndGet();
    } else {
      queue.remove(key);
    }
    queue.add(key);
    evictOverflow();
  }

  private void evictOverflow() {
    while (cacheSize.get() > maxSize) {
      var oldest = queue.poll();
      if (oldest == null) {
        return;
      }
      var removed = cache.remove(oldest);
      if (removed != null) {
        cacheSize.decrementAndGet();
        onExpired.accept(removed.getEntry());
      }
    }
  }

  public void clear() {
    var entries = new ArrayList<CacheEntry<V>>(cache.values());
    cache.clear();
    queue.clear();
    cacheSize.set(0);
    entries.forEach(e -> onExpired.accept(e.getEntry()));
  }

  public int size() {
    return cacheSize.get();
  }

  public Collection<CacheEntry<V>> values() {
    return cache.values();
  }

}
