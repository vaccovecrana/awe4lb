package io.vacco.a4lb.util;

import java.util.Collection;
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

    public long getExpireBy() { return expireBy; }
    public V getEntry() { return entry; }
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

  public V get(K key) {
    if (key == null) {
      throw new IllegalArgumentException("Invalid Key.");
    }
    var entry = cache.get(key);
    if (entry == null) {
      return null;
    }
    long timestamp = entry.getExpireBy();
    if (timestamp != -1 && System.currentTimeMillis() > timestamp) {
      if (cache.remove(key, entry)) {
        cacheSize.decrementAndGet();
        onExpired.accept(entry.getEntry());
      }
      return null;
    }
    return entry.getEntry();
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
      }
    }
  }

  public void clear() {
    cache.clear();
    queue.clear();
    cacheSize.set(0);
  }

  public int size() {
    return cacheSize.get();
  }

  public Collection<CacheEntry<V>> values() {
    return cache.values();
  }

}
