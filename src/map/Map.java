/**
 * Warlight AI Game Bot
 *
 * Last update: April 02, 2014
 *
 * @author Jim van Eeden
 * @version 1.0
 * @License MIT License (http://opensource.org/Licenses/MIT)
 */

package map;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;

import map.Pathfinder.Path;
import bot.BotState;

public class Map {
	private HashMap<Integer, Region> regions;
	private ArrayList<SuperRegion> superRegions;

	public Map() {
		this.regions = new HashMap<Integer, Region>();
		this.superRegions = new ArrayList<SuperRegion>();
	}

	/**
	 * add a Region to the map
	 * 
	 * @param region
	 *            : Region to be added
	 */
	public void add(Region region) {
		regions.put(region.getId(), region);
	}

	/**
	 * add a SuperRegion to the map
	 * 
	 * @param superRegion
	 *            : SuperRegion to be added
	 */
	public void add(SuperRegion superRegion) {
		for (SuperRegion s : superRegions)
			if (s.getId() == superRegion.getId()) {
				System.err.println("SuperRegion cannot be added: id already exists.");
				return;
			}
		superRegions.add(superRegion);
	}

	/**
	 * @return : the list of all Regions in this map
	 */
	public HashMap<Integer, Region> getRegions() {
		return regions;
	}

	/**
	 * @return : the list of all Regions in this map
	 */
	public Collection<Region> getRegionList() {
		return regions.values();
	}

	/**
	 * @return : the list of all SuperRegions in this map
	 */
	public ArrayList<SuperRegion> getSuperRegions() {
		return superRegions;
	}

	/**
	 * @param id
	 *            : a Region id number
	 * @return : the matching Region object
	 */
	public Region getRegion(int id) {
		return regions.get(id);
	}

	/**
	 * @param id
	 *            : a SuperRegion id number
	 * @return : the matching SuperRegion object
	 */
	public SuperRegion getSuperRegion(int id) {
		for (SuperRegion superRegion : superRegions)
			if (superRegion.getId() == id)
				return superRegion;
		return null;
	}

	public ArrayList<Region> getOwnedRegions(String name) {
		ArrayList<Region> owned = new ArrayList<Region>();

		for (Region r : regions.values()) {
			if (r.getPlayerName().equals(name)) {
				owned.add(r);
			}
		}

		return owned;
	}

	public ArrayList<SuperRegion> getOwnedSuperRegions(String name) {
		ArrayList<SuperRegion> owned = new ArrayList<SuperRegion>();
		for (SuperRegion r : getSuperRegions()) {
			if (r.ownedByPlayer(name)) {
				owned.add(r);
			}
		}

		return owned;

	}

	public ArrayList<Region> getUnOwnedRegionsInSuperRegion(String name, SuperRegion s) {
		ArrayList<Region> unOwned = new ArrayList<Region>();
		for (Region r : s.getSubRegions()) {
			if (!r.getPlayerName().equals(name)) {
				unOwned.add(r);
			}
		}

		return unOwned;

	}

	public ArrayList<SuperRegion> getSuspectedOwnedSuperRegions(String opponentPlayerName) {
		ArrayList<SuperRegion> suspected = new ArrayList<SuperRegion>();
		for (SuperRegion sr : getSuperRegions()) {
			if (sr.getSuspectedOwnedSuperRegion(opponentPlayerName));
				suspected.add(sr);
		}

		return suspected;
	}

	public ArrayList<Region> getOwnedFrontRegions() {
		ArrayList<SuperRegion> ownedSuperRegions = getOwnedSuperRegions(BotState.getMyName());
		ArrayList<Region> ownedRegionsInOwnedSuperRegions = new ArrayList<Region>();
		ArrayList<Region> neighbors;
		ArrayList<Region> front = new ArrayList<Region>();

		for (SuperRegion s : ownedSuperRegions) {
			ownedRegionsInOwnedSuperRegions.addAll(s.getSubRegions());
		}

		for (Region r : ownedRegionsInOwnedSuperRegions) {
			neighbors = r.getNeighbors();
			for (Region n : neighbors) {
				if (n.getPlayerName().equals(BotState.getMyOpponentName())) {
					front.add(r);
					continue;
				}
			}
		}

		return front;

	}

	public ArrayList<SuperRegion> getOwnedFrontSuperRegions() {
		ArrayList<SuperRegion> sFront = new ArrayList<SuperRegion>();
		ArrayList<SuperRegion> ownedSuperRegions = getOwnedSuperRegions(BotState.getMyName());

		for (SuperRegion s : ownedSuperRegions) {
			if (s.getFronts().size() > 0) {
				sFront.add(s);
			}
		}

		return sFront;
	}

	public ArrayList<Region> getPockets() {
		ArrayList<Region> owned = getOwnedRegions(BotState.getMyName());
		ArrayList<Region> pockets = new ArrayList<Region>();

		outerLoop: for (Region r : owned) {
			for (Region n : r.getNeighbors()) {
				if (n.getPlayerName().equals(BotState.getMyName())) {
					continue outerLoop;
				}
			}
			pockets.add(r);
		}

		return pockets;
	}

	// remove me later
	static <K, V extends Comparable<? super V>> java.util.SortedSet<java.util.Map.Entry<K, V>> entriesSortedByValues(java.util.Map<K, V> map) {
		java.util.SortedSet<java.util.Map.Entry<K, V>> sortedEntries = new java.util.TreeSet<java.util.Map.Entry<K, V>>(
				new java.util.Comparator<java.util.Map.Entry<K, V>>() {
					@Override
					public int compare(java.util.Map.Entry<K, V> e1, java.util.Map.Entry<K, V> e2) {
						int res = e2.getValue().compareTo(e1.getValue());
						return res != 0 ? res : 1; // Special fix to preserve
													// items with equal values
					}
				});
		sortedEntries.addAll(map.entrySet());
		return sortedEntries;
	}

	public void computeBottlenecks() {
		Pathfinder pathfinder = new Pathfinder(this);
		HashMap<Region, Double> traffic = new HashMap<Region, Double>();

		for (Region region : regions.values()) {
			for (Iterator<Path> iterator = pathfinder.distanceIterator(region); iterator.hasNext();) {
				Path path = iterator.next();

				double currentTraffic = 1 / path.getDistance();
				if (traffic.containsKey(path.getTarget())) {
					traffic.put(path.getTarget(), traffic.get(path.getTarget()) + currentTraffic);
				} else {
					traffic.put(path.getTarget(), currentTraffic);
				}
			}
		}

		System.err.println("---- Computing bottlenecks ----");
		for (java.util.Map.Entry<Region, Double> entry : entriesSortedByValues(traffic)) {
			System.err.println("Region " + entry.getKey().getId() + " : " + entry.getValue());
		}
		System.err.println("-------------------------------");
	}

	public ArrayList<Region> getUnOwnedRegions() {
		ArrayList<Region> unOwned = new ArrayList<Region>();
		for (SuperRegion s : superRegions) {
			unOwned.addAll(getUnOwnedRegionsInSuperRegion(BotState.getMyName(), s));

		}
		return unOwned;
	}

	public ArrayList<Region> getEnemyRegions() {
		ArrayList<Region> enemyRegions = new ArrayList<Region>();
		for (Region r : regions.values()) {
			if (r.getPlayerName().equals(BotState.getMyOpponentName())) {
				enemyRegions.add(r);
			}
		}
		return enemyRegions;
	}

}
