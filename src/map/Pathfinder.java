package map;

import java.util.*;

import bot.Values;

public class Pathfinder {
	private PathfinderWeighter pathfinderWeighter;
	private HashMap<Region, Double> distances;
	private Map map;

	public Pathfinder(Map map, PathfinderWeighter pathfinderWeighter) {
		this.map = map;
		this.pathfinderWeighter = pathfinderWeighter;
	}

	public Pathfinder(Map map) {
		this(map, new PathfinderWeighter() {
			public double weight(Region nodeA, Region nodeB) {
				return 1;
			}
		});
	}

	public Path getPathToSuperRegionFromRegionOwnedByPlayer(SuperRegion superRegion, String playerName) {
		for (Iterator<Path> iterator = distanceIterator(map.getOwnedRegions(playerName)); iterator.hasNext();) {
			Path path = iterator.next();
			if (path.getTarget().getSuperRegion() == superRegion) {
				return path;
			}
		}

		return null;
	}

	public Path getPathToRegionOwnedByPlayer(Region origin, String playerName) {
		for (Iterator<Path> iterator = distanceIterator(origin); iterator.hasNext();) {
			Path path = iterator.next();

			if (path.getTarget().getPlayerName().equals(playerName)) {
				return path;
			}
		}

		return null;
	}

	public Path getPath(Region origin, Region target) {
		for (Iterator<Path> iterator = distanceIterator(origin); iterator.hasNext();) {
			Path next = iterator.next();

			if (next.getTarget() == target) {
				return next;
			}
		}

		return null;
	}

	public Path getPathToSuperRegionFromRegion(SuperRegion superRegion, Region origin) {
		for (Iterator<Path> iterator = distanceIterator(origin); iterator.hasNext();) {
			Path path = iterator.next();

			if (path.getTarget().getSuperRegion() == superRegion) {
				return path;
			}
		}

		return null;
	}

	public ArrayList<Path> getPathToAllRegionsNotOwnedByPlayerFromRegion(Region origin, String playerName) {
		ArrayList<Path> paths = new ArrayList<Path>();

		for (Iterator<Path> iterator = distanceIterator(origin); iterator.hasNext();) {
			Path path = iterator.next();

			if (!path.getTarget().getPlayerName().equals(playerName)) {
				paths.add(path);
			}
		}

		return paths;
	}

	public ArrayList<Path> getPathToRegionsFromRegion(Region origin, ArrayList<Region> targetRegions) {
		ArrayList<Path> paths = new ArrayList<Path>();

		for (Iterator<Path> iterator = distanceIterator(origin); iterator.hasNext();) {
			Path path = iterator.next();

			if (targetRegions.contains(path.getTarget())) {
				paths.add(path);
			}

		}
		return paths;

	}

	public class Path {
		private Double distance;
		private LinkedList<Region> path;

		private Path(Double double1, LinkedList<Region> path) {
			this.distance = double1;
			this.path = path;
		}

		public double getDistance() {
			return distance;
		}

		public List<Region> getPath() {
			return path;
		}

		public Region getOrigin() {
			return path.getFirst();
		}

		public Region getTarget() {
			return path.getLast();
		}

		public String toString() {
			return "Path{" + "distance=" + distance + ", path=" + path + '}';
		}
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

	static <K, V extends Comparable<? super V>> SortedSet<java.util.Map.Entry<K, V>> entriesSortedByValues(java.util.Map<K, V> map) {
		SortedSet<java.util.Map.Entry<K, V>> sortedEntries = new TreeSet<java.util.Map.Entry<K, V>>(new Comparator<java.util.Map.Entry<K, V>>() {
			@Override
			public int compare(java.util.Map.Entry<K, V> e1, java.util.Map.Entry<K, V> e2) {
				int res = e1.getValue().compareTo(e2.getValue());
				return res != 0 ? res : 1; // Special fix to preserve
											// items with equal values
			}
		});
		sortedEntries.addAll(map.entrySet());
		return sortedEntries;
	}

	private Iterator<Path> distanceIterator(Collection<Region> regions) {
		HashMap<Region, Region> previous = new HashMap<Region, Region>();
		distances = new HashMap<Region, Double>();

		for (Region region : regions) {
			distances.put(region, Double.valueOf(0));
		}

		for (Iterator<Region> iterator = new BFSIterator(regions); iterator.hasNext();) {
			Region next = iterator.next();

			for (Region neighbor : next.getNeighbors()) {
				double distance = getComputedDistance(next) + pathfinderWeighter.weight(next, neighbor);

				if (distance < getComputedDistance(neighbor)) {
					distances.put(neighbor, distance);
					previous.put(neighbor, next);
				}
			}
		}

		LinkedList<Path> paths = new LinkedList<Path>();

		for (java.util.Map.Entry<Region, Double> entry : entriesSortedByValues(distances)) {
			LinkedList<Region> path = new LinkedList<Region>();
			Region step = entry.getKey();

			if (previous.get(step) != null) {
				path.addFirst(step);
				while (previous.get(step) != null) {
					step = previous.get(step);
					path.addFirst(step);
				}

				paths.add(new Path(entry.getValue(), path));
			}
		}

		return paths.iterator();
	}

	public Iterator<Path> distanceIterator(Region origin) {
		return distanceIterator(java.util.Collections.singleton(origin));
	}

	private Double getComputedDistance(Region node) {
		Double d = distances.get(node);

		if (d == null) {
			return Double.MAX_VALUE;
		} else {
			return d;
		}
	}

	public static Pathfinder getSimplePathfinder(Map map) {
		Pathfinder pathfinder = new Pathfinder(map, new PathfinderWeighter() {
			public double weight(Region nodeA, Region nodeB) {
				return 1;

			}
		});
		return pathfinder;
	}

	public static Pathfinder getWeighedCostPathfinder(Map map) {
		Pathfinder pathfinder = new Pathfinder(map, new PathfinderWeighter() {
			public double weight(Region nodeA, Region nodeB) {
				return Values.calculateRegionWeighedCost(nodeB);

			}
		});
		return pathfinder;
	}

	// public static void blargh(String[] args) {
	// Map m = new Map();
	//
	// SuperRegion superRegion = new SuperRegion(0, 0);
	// SuperRegion superRegion2 = new SuperRegion(0, 0);
	// m.add(superRegion);
	//
	// Region node1 = new Region(1, superRegion, "player1", 0);
	// Region node2 = new Region(2, superRegion, "player1", 0);
	// Region node3 = new Region(3, superRegion, "player1", 0);
	// Region node4 = new Region(4, superRegion2, "player2", 0);
	// Region node5 = new Region(5, superRegion2, "player2", 0);
	//
	// node1.addNeighbor(node3);
	// node3.addNeighbor(node2);
	// node2.addNeighbor(node5);
	// // node3.addNeighbor(node4);
	// node4.addNeighbor(node5);
	//
	// m.add(node1);
	// m.add(node2);
	// m.add(node3);
	// m.add(node4);
	// m.add(node5);
	//
	// Pathfinder Pathfinder = new Pathfinder(m);

	// Iterator<Path> it = Pathfinder.distanceIterator(node1);
	// while (it.hasNext()){
	// System.out.println(it.next().getTarget());
	// }

	// System.out.println(Pathfinder.getDistanceBetweenRegions(node1,
	// node4));
	// Pathfinder.getPlayerInnerRegions("player1");

	// for (Region region : Pathfinder.getPlayerInnerRegions(m, "player1"))
	// {
	// System.out.println(region.getId());
	// }
}
