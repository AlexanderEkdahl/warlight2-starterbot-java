package concepts;

public class Value {
  float sum = 0;

  public Value(float... values) {
    for (float value : values) {
      sum += Math.pow(value, 2);
    }
  }

  public float getValue() {
    return (float)Math.sqrt(sum);
  }
}
