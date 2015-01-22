package concepts;

public class Value implements Comparable<Value> {
  private float sum = 0;

  public Value(float... values) {
    for (float value : values) {
      addValue(value);
    }
  }

  public void addValues(float... values) {
    for (float value : values) {
      sum += Math.pow(value, 2);
    }
  }

  public void addValues(Value... values) {
    for (Value value : values) {
      sum += value.sum;
    }
  }

  public void addValue(float value) {
    addValues(value);
  }

  public void addValue(Value value) {
    addValues(value);
  }

  public float getValue() {
    return (float)Math.sqrt(sum);
  }

  @Override
  public int compareTo(Value otherValue) {
    if (otherValue.getValue() > getValue()) {
      return 1;
    } else if (otherValue.getValue() == getValue()) {
      return 0;
    } else {
      return -1;
    }
  }
}
