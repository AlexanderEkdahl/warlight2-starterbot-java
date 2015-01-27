import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.junit.Before;

import map.*;
import map.Pathfinder.Path;
import java.util.Iterator;

public class PathfinderTest {
  private Map map;
  private SuperRegion superRegion1, superRegion2;
  private Region node1, node2, node3, node4, node5;
  private Pathfinder Pathfinder;

  @Before
  public void setup() {
    map = new Map();

    superRegion1 = new SuperRegion(0, 0);
    superRegion2 = new SuperRegion(0, 0);
    map.add(superRegion1);

    node1 = new Region(1, superRegion1, "player1", 0);
    node2 = new Region(2, superRegion1, "player1", 0);
    node3 = new Region(3, superRegion1, "player1", 0);
    node4 = new Region(4, superRegion2, "player2", 0);
    node5 = new Region(5, superRegion2, "player2", 0);

    node1.addNeighbor(node3);
    node3.addNeighbor(node2);
    node2.addNeighbor(node5);
    node4.addNeighbor(node5);

    map.add(node1);
    map.add(node2);
    map.add(node3);
    map.add(node4);
    map.add(node5);

    Pathfinder = new Pathfinder(map);
  }

  @Test
  public void testDistanceIterator() {
    Iterator<Path> it = Pathfinder.distanceIterator(node1);
    assert(it.hasNext());
    Path path = it.next();
    assertEquals(node3, path.getTarget());
    assertEquals(1, path.getDistance());
    assert(it.hasNext());
    path = it.next();
    assertEquals(node2, path.getTarget());
    assertEquals(2, path.getDistance());
    assert(it.hasNext());
    path = it.next();
    assertEquals(node5, path.getTarget());
    assertEquals(3, path.getDistance());
    assert(it.hasNext());
    path = it.next();
    assertEquals(node4, path.getTarget());
    assertEquals(4, path.getDistance());
    assert(!it.hasNext());
  }
}
