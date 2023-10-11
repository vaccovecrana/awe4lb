package io.vacco.a4lb.cfg;

public class A4Avg {

  private float[] values;
  private int size;
  private int currentIndex;
  private float sum;

  public float val;

  public A4Avg init(int n) {
    if (n <= 0) {
      throw new IllegalArgumentException("Rolling average size must be greater than 0");
    }
    size = n;
    values = new float[size];
    currentIndex = 0;
    sum = 0;
    return this;
  }

  public void update(float newVal) {
    sum -= values[currentIndex];
    values[currentIndex] = newVal;
    sum += newVal;
    currentIndex = (currentIndex + 1) % size;
    if (currentIndex == 0) {
      val = sum / size;
    } else {
      val = sum / currentIndex;
    }
  }

}
