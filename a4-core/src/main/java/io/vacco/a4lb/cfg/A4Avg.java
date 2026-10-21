package io.vacco.a4lb.cfg;

/**
 * Fixed-size rolling average. Instances are updated by multiple proxy threads,
 * so updates and reads are synchronized.
 */
public class A4Avg {

  private float[] values;
  private int size;
  private int currentIndex;
  private int count;
  private float sum;

  public volatile float val;

  public A4Avg init(int n) {
    if (n <= 0) {
      throw new IllegalArgumentException("Rolling average size must be greater than 0");
    }
    synchronized (this) {
      size = n;
      values = new float[size];
      currentIndex = 0;
      count = 0;
      sum = 0;
      val = 0;
    }
    return this;
  }

  public synchronized void update(float newVal) {
    if (values == null) {
      throw new IllegalStateException("Rolling average has not been initialized");
    }
    sum -= values[currentIndex];
    values[currentIndex] = newVal;
    sum += newVal;
    currentIndex = (currentIndex + 1) % size;
    if (count < size) {
      count = count + 1;
    }
    val = sum / count;
  }

  public synchronized float get() {
    return val;
  }

  public synchronized void reset() {
    if (values != null) {
      java.util.Arrays.fill(values, 0);
    }
    currentIndex = 0;
    count = 0;
    sum = 0;
    val = 0;
  }

}
