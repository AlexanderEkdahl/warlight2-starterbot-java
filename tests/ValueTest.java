import static org.junit.Assert.assertEquals;

import org.junit.Test;

import concepts.Value;

public class ValueTest {
  @Test
  public void testValue() {
    Value value = new Value(3.0f, 4.0f);
    assertEquals(5.0f, value.getValue(), 0.01f);
    value.addValues(20f);
    assertEquals(20.61f, value.getValue(), 0.01f);
  }

  @Test
  public void testDifferentValueInstances() {
    Value value1 = new Value(3.0f);
    Value value2 = new Value(4.0f);
    value1.addValue(value2);
    assertEquals(5.0f, value1.getValue(), 0.01f);
    value1.addValues(20f);
    assertEquals(20.61f, value1.getValue(), 0.01f);
  }

  @Test
  public void testCompareValues() {
    Value value1 = new Value(3.0f);
    Value value2 = new Value(4.0f);
    assertEquals(1, value1.compareTo(value2));
    assertEquals(-1, value2.compareTo(value1));
    assertEquals(0, value1.compareTo(value1));
  }
}
