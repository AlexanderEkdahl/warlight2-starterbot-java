import static org.junit.Assert.assertEquals;

import org.junit.Test;

import concepts.Value;

public class ValueTest {
  @Test
  public void testValue() {
    Value value = new Value(3.0f, 4.0f);
    assertEquals(5.0f, value.getValue(), 0.01f);
  }
}
