package map;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.LinkedList;
import java.util.HashMap;

interface PathfinderWeighter {
  int weight(Region a, Region b);
}

public class Pathfinder {
  private final Collection<Region> nodes;
  private HashSet<Region> settledNodes;
  private HashSet<Region> unSettledNodes;
  private HashMap<Region, Region> predecessors;
  private HashMap<Region, Integer> distance;
  private PathfinderWeighter pathfinderWeighter;

  public Pathfinder(Map map, PathfinderWeighter pathfinderWeighter) {
    nodes = map.getRegionList();
    this.pathfinderWeighter = pathfinderWeighter;
  }

  public void execute(Region source) {
    settledNodes = new HashSet<Region>();
    unSettledNodes = new HashSet<Region>();
    distance = new HashMap<Region, Integer>();
    predecessors = new HashMap<Region, Region>();
    distance.put(source, 0);
    unSettledNodes.add(source);
    while (unSettledNodes.size() > 0) {
      Region node = getMinimum(unSettledNodes);
      settledNodes.add(node);
      unSettledNodes.remove(node);
      findMinimalDistances(node);
    }
  }

  private void findMinimalDistances(Region node) {
    List<Region> adjacentNodes = getUnvisitedNeighbors(node);
    for (Region target : adjacentNodes) {
      if (getShortestDistance(target) > getShortestDistance(node)
      + getDistance(node, target)) {
        distance.put(target, getShortestDistance(node)
        + getDistance(node, target));
        predecessors.put(target, node);
        unSettledNodes.add(target);
      }
    }

  }

  private Region getMinimum(HashSet<Region> nodes) {
    Region minimum = null;
    for (Region node : nodes) {
      if (minimum == null) {
        minimum = node;
      } else {
        if (getShortestDistance(node) < getShortestDistance(minimum)) {
          minimum = node;
        }
      }
    }
    return minimum;
  }

  private int getShortestDistance(Region destination) {
    Integer d = distance.get(destination);
    if (d == null) {
      return Integer.MAX_VALUE;
    } else {
      return d;
    }
  }

  private int getDistance(Region nodeA, Region nodeB) {
    return pathfinderWeighter.weight(nodeA, nodeB);
    // for (Region currentNode : nodeA.getNeighbors()) {
    //   if(currentNode == nodeB) {
    //     return 1;
    //   }
    // }
    // throw new RuntimeException("Should not happen");
  }

  private LinkedList<Region> getUnvisitedNeighbors(Region node) {
    LinkedList<Region> neighbors = new LinkedList<Region>();
    for (Region currentNode : node.getNeighbors()) {
      if (!settledNodes.contains(currentNode)) {
        neighbors.add(currentNode);
      }
    }
    return neighbors;
  }

  public LinkedList<Region> getPath(Region target) {
    LinkedList<Region> path = new LinkedList<Region>();
    Region step = target;
    // check if a path exists
    if (predecessors.get(step) == null) {
      throw new RuntimeException("There should always be a way");
    }
    path.add(step);
    while (predecessors.get(step) != null) {
      step = predecessors.get(step);
      path.add(step);
    }
    // Put it into the correct order
    Collections.reverse(path);
    return path;
  }

  private static void test() {
    Map m = new Map();

    SuperRegion superRegion = new SuperRegion(0, 0);
    m.add(superRegion);

    Region a = new Region(1, superRegion);
    Region b = new Region(2, superRegion);
    Region c = new Region(3, superRegion);
    Region d = new Region(4, superRegion);

    a.addNeighbor(b);
    b.addNeighbor(c);
    c.addNeighbor(d);
    // d.addNeighbor(a);

    m.add(a);
    m.add(b);
    m.add(c);
    m.add(d);

    Pathfinder pathfinder = new Pathfinder(m, new PathfinderWeighter() {
      public int weight(Region nodeA, Region nodeB) {
        if (nodeB.getId() == 2) {
          return 100;
        }
        return 3;
      }
    });
    pathfinder.execute(a);

    System.out.println(pathfinder.getShortestDistance(c));

    for (Region node : pathfinder.getPath(c)) {
      System.out.print(node.getId() + " ");
    }
    System.out.println();
  }
}
