package map;

import java.util.*;

public class Pathfinder2 {
    private PathfinderWeighter pathfinderWeighter;
    private HashMap<Region, Integer> distances;

    public Pathfinder2(PathfinderWeighter pathfinderWeighter) {
        this.pathfinderWeighter = pathfinderWeighter;
    }

    public Pathfinder2() {
        this(new PathfinderWeighter() {
            public int weight(Region nodeA, Region nodeB) {
                return 1;
            }
        });
    }

    public Iterator<Region> iterator(Region origin) {
        return new BFSIterator(origin);
    }

    private class BFSIterator implements Iterator<Region> {
        private LinkedList<Region> queue;
        private HashSet<Region> visited;

        private BFSIterator(Region origin) {
            queue = new LinkedList<Region>();
            visited = new HashSet<Region>();
            visited.add(origin);
            queue.add(origin);
        }

        private BFSIterator(Collection regions) {
            queue = new LinkedList<Region>(regions);
            visited = new HashSet<Region>(regions);
        }

        public void remove() {
            throw new UnsupportedOperationException();
        }

        public Region next() {
            Region region = queue.poll();
            for (Region neighbor : region.getNeighbors()) {
                if (!visited.contains(neighbor)) {
                    visited.add(neighbor);
                    queue.add(neighbor);
                }
            }
            return region;
        }

        public boolean hasNext() {
            return !queue.isEmpty();
        }
    }

    public Region getNearestOwnedRegionToSuperRegion(SuperRegion superRegion, String playerName) {
        for (Iterator<Region> iterator = new BFSIterator(superRegion.getSubRegions()); iterator.hasNext(); ) {
            Region next = iterator.next();

            if (next.getPlayerName().equals(playerName)) {
                return next;
            }
        }

        return null;
    }

    public Region getNearestOwnedRegion(Region origin, String playerName) {
        for (Iterator<Region> iterator = new BFSIterator(origin); iterator.hasNext(); ) {
            Region next = iterator.next();

            if (next.getPlayerName().equals(playerName)) {
                return next;
            }
        }

        return null;
    }

    public List<Region> getShortestPath(Region origin, Region target) {
        HashMap<Region, Region> previous = new HashMap<Region, Region>();
        distances = new HashMap<Region, Integer>();
        distances.put(origin, 0);

        for (Iterator<Region> iterator = new BFSIterator(origin); iterator.hasNext(); ) {
            Region next = iterator.next();

            for (Region neighbor : next.getNeighbors()) {
                int distance = getComputedDistance(next) + getNodeDistance(next, neighbor);

                if (distance < getComputedDistance(neighbor)) {
                    distances.put(neighbor, distance);
                    previous.put(neighbor, next);
                }
            }
        }

        LinkedList<Region> path = new LinkedList<Region>();
        Region step = target;

        if (previous.get(step) == null) {
            throw new RuntimeException("There should always be a way");
        }

        path.addFirst(step);
        while (previous.get(step) != null) {
            step = previous.get(step);
            path.addFirst(step);
        }

        return path;
    }

    public int getDistanceBetweenRegions(Region origin, Region target) {
        getShortestPath(origin, target);
        return getComputedDistance(target);
    }

    public List<Region> getPlayerInnerRegions(Map map, String playerName) {
        ArrayList<Region> innerRegions = new ArrayList<Region>();

        outer:
        for (Iterator<Region> iterator = new BFSIterator(map.getOwnedRegions(playerName)); iterator.hasNext(); ) {
            Region next = iterator.next();

            for (Region neighbor : next.getNeighbors()) {
                System.out.println(next.getId() + " neighbor " + neighbor.getPlayerName());
                if (!neighbor.getPlayerName().equals(playerName)) {
                    System.out.println("Neighboring other stuff");
                    continue outer;
                }
            }

            innerRegions.add(next);
        }

        return innerRegions;
    }

    private int getComputedDistance(Region node) {
        Integer d = distances.get(node);

        if (d == null) {
            return 100000;
        } else {
            return d;
        }
    }

    private int getNodeDistance(Region nodeA, Region nodeB) {
        return pathfinderWeighter.weight(nodeA, nodeB);
    }

    public static void test(String[] args) {
        Map m = new Map();

        SuperRegion superRegion = new SuperRegion(0, 0);
        SuperRegion superRegion2 = new SuperRegion(0, 0);
        m.add(superRegion);

        Region node1 = new Region(1, superRegion, "player1", 0);
        Region node2 = new Region(2, superRegion, "player1", 0);
        Region node3 = new Region(3, superRegion, "player1", 0);
        Region node4 = new Region(4, superRegion2, "player2", 0);
        Region node5 = new Region(5, superRegion2, "player2", 0);

        node1.addNeighbor(node3);
        node3.addNeighbor(node2);
        node2.addNeighbor(node5);
        // node3.addNeighbor(node4);
        node4.addNeighbor(node5);

        m.add(node1);
        m.add(node2);
        m.add(node3);
        m.add(node4);
        m.add(node5);

        Pathfinder2 pathfinder2 = new Pathfinder2();

        // for (Region region : pathfinder2.getShortestPath(node1, node2)) {
        //   System.out.print(region.getId() + " ");
        // }
        // System.out.println();

        // Region nearest = pathfinder2.getNearestOwnedRegion(node3, "player1");

        // System.out.println(pathfinder2.getDistanceBetweenRegions(node1, node4));
        // pathfinder2.getPlayerInnerRegions("player1");

        for (Region region : pathfinder2.getPlayerInnerRegions(m, "player1")) {
            System.out.println(region.getId());
        }
    }
}
