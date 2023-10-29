package io.vacco.a4lb.util;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

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
      onExpired.accept(removeAndGet(key));
      return null;
    }
    return entry.getEntry();
  }

  public V removeAndGet(K key) {
    if (key == null) {
      return null;
    }
    CacheEntry<V> entry = cache.get(key);
    if (entry != null) {
      cacheSize.decrementAndGet();
      return cache.remove(key).getEntry();
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
    var exists = cache.containsKey(key);
    if (!exists) {
      cacheSize.incrementAndGet();
      while (cacheSize.get() > maxSize) {
        removeAndGet(queue.poll());
      }
    }
    cache.put(key, new CacheEntry<V>(expireBy, value));
    queue.add(key);
  }

  public void clear() {
    cache.clear();
  }

  public Collection<CacheEntry<V>> values() {
    return cache.values();
  }

}
