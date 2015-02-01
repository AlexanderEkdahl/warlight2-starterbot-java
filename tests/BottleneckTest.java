import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.junit.Before;

import map.*;

public class BottleneckTest {
  private Map map;
  private SuperRegion superRegion;
  private Region node1, node2, node3, node4, node5, node6, node7, node8;
  private Pathfinder Pathfinder;

  @Before
  public void setup() {
    map = new Map();

    superRegion = new SuperRegion(0, 0);
    map.add(superRegion);

    node1 = new Region(1, superRegion);
    node2 = new Region(2, superRegion);
    node3 = new Region(3, superRegion);
    node4 = new Region(4, superRegion);
    node5 = new Region(5, superRegion);
    node6 = new Region(6, superRegion);
    node7 = new Region(7, superRegion);
    node8 = new Region(8, superRegion);

    node1.addNeighbor(node2);
    node1.addNeighbor(node3);
    node2.addNeighbor(node3);
    node2.addNeighbor(node4);
    node3.addNeighbor(node4);
    node3.addNeighbor(node6);
    node4.addNeighbor(node5);
    node5.addNeighbor(node6);
    node6.addNeighbor(node7);
    node7.addNeighbor(node8);

    map.add(node1);
    map.add(node2);
    map.add(node3);
    map.add(node4);
    map.add(node5);
    map.add(node6);
    map.add(node7);
    map.add(node8);
  }

  @Test
  public void testBottlenecks() {
    map.computeBottlenecks();
  }
}
