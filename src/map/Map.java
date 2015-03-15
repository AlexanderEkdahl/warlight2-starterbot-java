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

import imaginary.EnemyAppreciator;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import map.Pathfinder.Path;
import bot.BotState;

public class Map {
	private HashMap<Integer, Region> regions;
	private ArrayList<SuperRegion> superRegions;
	private EnemyAppreciator appreciator;

	public Map() {
		this.regions = new HashMap<Integer, Region>();
		this.superRegions = new ArrayList<SuperRegion>();
	}

	public void initAppreciator() {
		appreciator = new EnemyAppreciator(duplicate());
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
		for (SuperRegion sr : getSuperRegions()) {
			if (sr.ownedByPlayer(name)) {
				owned.add(sr);
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
			if (sr.getSuspectedOwnedSuperRegion()) {
				suspected.add(sr);
			}
		}

		return suspected;
	}

	public ArrayList<Region> getOwnedSuperRegionFrontRegions() {
		ArrayList<SuperRegion> ownedSuperRegions = getOwnedSuperRegions(BotState.getMyName());
		ArrayList<Region> ownedRegionsInOwnedSuperRegions = new ArrayList<Region>();
		ArrayList<Region> front = new ArrayList<Region>();

		for (SuperRegion s : ownedSuperRegions) {
			ownedRegionsInOwnedSuperRegions.addAll(s.getSubRegions());
		}

		for (Region r : ownedRegionsInOwnedSuperRegions) {
			if (r.isFront()){
				front.add(r);
			}
		}

		return front;

	}

	public ArrayList<SuperRegion> getOwnedFrontSuperRegions() {
		ArrayList<SuperRegion> sFront = new ArrayList<SuperRegion>();
		ArrayList<SuperRegion> ownedSuperRegions = getOwnedSuperRegions(BotState.getMyName());

		for (SuperRegion s : ownedSuperRegions) {
			if (s.getArmiesReward() > 0 && s.getFronts().size() > 0) {
				sFront.add(s);
			}
		}

		return sFront;
	}

//	public ArrayList<Region> getPockets() {
//		ArrayList<Region> owned = getOwnedRegions(BotState.getMyName());
//		ArrayList<Region> pockets = new ArrayList<Region>();
//
//		outerLoop: for (Region r : owned) {
//			for (Region n : r.getNeighbors()) {
//				if (n.getPlayerName().equals(BotState.getMyName())) {
//					continue outerLoop;
//				}
//			}
//			pockets.add(r);
//		}
//
//		return pockets;
//	}

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

	public Set<Region> getEnemyRegions() {
		Set<Region> enemyRegions = new HashSet<Region>();
		for (Region r : regions.values()) {
			if (r.getPlayerName().equals(BotState.getMyOpponentName())) {
				enemyRegions.add(r);
			}
		}
		return enemyRegions;
	}

	public Map duplicate() {

		HashMap<Integer, Region> regionDuplicateNumbers = new HashMap<Integer, Region>();
		HashMap<Integer, SuperRegion> superRegionDuplicateNumbers = new HashMap<Integer, SuperRegion>();

		Map newMap = new Map();

		for (SuperRegion s : superRegions) {
			SuperRegion superRegionDuplicate = s.duplicate();
			newMap.add(superRegionDuplicate);
			superRegionDuplicateNumbers.put(s.getId(), superRegionDuplicate);
		}
		for (Region r : regions.values()) {
			Region regionDuplicate = r.duplicateInto(superRegionDuplicateNumbers.get(r.getSuperRegion().getId()));
			newMap.add(regionDuplicate);
			regionDuplicateNumbers.put(regionDuplicate.getId(), regionDuplicate);

		}
		for (Region r : regions.values()) {
			for (Region n : r.getNeighbors()) {
				regionDuplicateNumbers.get(r.getId()).addNeighbor(regionDuplicateNumbers.get(n.getId()));
			}
		}

		return newMap;
	}

	// public ArrayList<Region> getInterestingRegions(){
	// // interesting is defined as not being the same owned by the same person
	// as all tiles next to it
	// ArrayList<Region> interestingRegions = new ArrayList<Region>();
	// for (Region r : regions.values()){
	// String owner = r.getPlayerName();
	// for (Region n : r.getNeighbors()){
	//
	// }
	// }
	//
	// return null;
	//
	// }
	public void updateMap(String[] mapInput) {
		ArrayList<Region> visibleRegions = new ArrayList<Region>();
		HashSet<Region> invisibleRegions = new HashSet<Region>(regions.values());

		for (int i = 1; i < mapInput.length; i++) {
			try {
				Region region = getRegion(Integer.parseInt(mapInput[i]));
				String playerName = mapInput[i + 1];
				int armies = Integer.parseInt(mapInput[i + 2]);

				region.setPlayerName(playerName);
				region.setArmies(armies);
				visibleRegions.add(region);
				i += 2;
			} catch (Exception e) {
				System.err.println("Unable to parse Map Update " + e.getMessage());
			}
		}

		for (Region region : visibleRegions) {
			region.setVisible(true);
			invisibleRegions.remove(region);
		}

		for (Region region : invisibleRegions) {
			region.setVisible(false);
			if (region.getPlayerName().equals(BotState.getMyName())) {
				System.err.println("Region: " + region.getId() + " was lost out of sight. It must have been taken by the enemy.");
				region.setPlayerName(BotState.getMyOpponentName());
			}
		}
		appreciator.updateMap(duplicate());

	}

	public EnemyAppreciator getAppreciator() {
		return appreciator;
	}

	public void setAppreciator(EnemyAppreciator appreciator) {
		this.appreciator = appreciator;
	}

	public void setupMap(String[] mapInput) {
		if (mapInput[1].equals("super_regions")) {
			for (int i = 2; i < mapInput.length; i++) {
				try {
					int superRegionId = Integer.parseInt(mapInput[i]);
					i++;
					int reward = Integer.parseInt(mapInput[i]);
					add(new SuperRegion(superRegionId, reward));
				} catch (Exception e) {
					System.err.println("Unable to parse SuperRegions");
				}
			}
		} else if (mapInput[1].equals("regions")) {
			for (int i = 2; i < mapInput.length; i++) {
				try {
					int regionId = Integer.parseInt(mapInput[i]);
					i++;
					int superRegionId = Integer.parseInt(mapInput[i]);
					SuperRegion superRegion = getSuperRegion(superRegionId);
					add(new Region(regionId, superRegion));
				} catch (Exception e) {
					System.err.println("Unable to parse Regions " + e.getMessage());
				}
			}
		} else if (mapInput[1].equals("neighbors")) {
			for (int i = 2; i < mapInput.length; i++) {
				try {
					Region region = getRegion(Integer.parseInt(mapInput[i]));
					i++;
					String[] neighborIds = mapInput[i].split(",");
					for (int j = 0; j < neighborIds.length; j++) {
						Region neighbor = getRegion(Integer.parseInt(neighborIds[j]));
						region.addNeighbor(neighbor);
					}
				} catch (Exception e) {
					System.err.println("Unable to parse Neighbors " + e.getMessage());
				}
			}
			// map.computeBottlenecks();
		} else if (mapInput[1].equals("wastelands")) {
			for (int i = 2; i < mapInput.length; i++) {
				try {
					Region region = getRegion(Integer.parseInt(mapInput[i]));
					region.setWasteland(true);
					region.setArmies(6);
					region.setPlayerName("neutral");
				} catch (Exception e) {
					System.err.println("Unable to parse wastelands " + e.getMessage());
				}
			}
		} else if (mapInput[1].equals("opponent_starting_regions")) {
			for (int i = 2; i < mapInput.length; i++) {
				try {
					Region region = getRegion(Integer.parseInt(mapInput[i]));
					region.setPlayerName(BotState.getMyOpponentName());
				} catch (Exception e) {
					System.err.println("Unable to parse opponent_starting_regions " + e.getMessage());
				}
			}
		} else {
			System.err.println("Did not parse previous setup_map");
		}

		appreciator.setMap(duplicate());

	}

	public void readOpponentMoves(String[] moveInput) {
		appreciator.readOpponentMoves(moveInput);
	}

	public Set<Region> getAllRegionsThreateningOwnedSuperRegions() {
		Set<Region> threatening = new HashSet<Region>();
		for (Region r : getEnemyRegions()) {
			for (Region n : r.getNeighbors()) {
				if (n.getSuperRegion().ownedByPlayer(BotState.getMyName())) {
					threatening.add(r);
					break;
				}
			}
		}

		return threatening;

	}

	public Set<Region> getAllRegionsThreateningOwnedRegions() {
		Set<Region> threatening = new HashSet<Region>();
		for (Region r : getOwnedRegions(BotState.getMyName())) {
			for (Region n : r.getNeighbors()) {
				if (n.getPlayerName().equals(BotState.getMyOpponentName()) && !threatening.contains(n)) {
					threatening.add(n);
				}

			}
		}
		return threatening;
	}

	public HashMap<Region, Integer> getRegionUsableArmies() {
		HashMap<Region, Integer> armies = new HashMap<Region, Integer>();
		for (Region r : regions.values()) {
			armies.put(r, r.getArmies() - 1);
		}
		return armies;
	}

	public Set<Region> getAllEnemyVulnerableRegions() {
		Set<Region> vulnerable = new HashSet<Region>();
		for (SuperRegion s : getSuspectedOwnedSuperRegions(BotState.getMyOpponentName())) {
			for (Region r : s.getSubRegions()) {
				if (r.getPlayerName().equals(BotState.getMyOpponentName())) {
					for (Region n : r.getNeighbors()) {
						if (n.getPlayerName().equals(BotState.getMyName())) {
							vulnerable.add(r);
							break;
						}
					}
				}

			}
		}
		// TODO Auto-generated method stub
		return vulnerable;
	}

	public Set<Region> getallAnnoyingRegions() {
		Set<Region> annoyingRegions = new HashSet<Region>();
		for (SuperRegion s : superRegions) {
			annoyingRegions.addAll(s.getAnnoyingRegions());

		}
		return annoyingRegions;
	}

//	public ArrayList<Region> getOwnedRewardBlockers() {
//		ArrayList<Region> rewardBlockers = new ArrayList<Region>();
//		for (Region r : getOwnedRegions(BotState.getMyName())) {
//			if (r.isOnlyFriendlyRegionInSuperRegion()) {
//				rewardBlockers.add(r);
//			}
//		}
//
//		return rewardBlockers;
//	}

	public ArrayList<Region> getOwnedFrontRegions() {
		ArrayList<Region> frontRegions = new ArrayList<Region>();
		for (Region r : getOwnedRegions(BotState.getMyName())) {
			if (r.isFront()) {
				frontRegions.add(r);
			}
		}

		return frontRegions;
	}

	public ArrayList<SuperRegion> getProtectedSuperRegions() {
		ArrayList<SuperRegion> protectedSuperRegions = getOwnedSuperRegions(BotState.getMyName());
		protectedSuperRegions.removeAll(getOwnedFrontSuperRegions());
		return protectedSuperRegions;
	}

	public Collection<?> getOwnedSuperRegionRegions() {
		ArrayList<Region> owned = new ArrayList<Region>();
		for (SuperRegion sr : getOwnedSuperRegions(BotState.getMyName())){
			owned.addAll(sr.getSubRegions());
		}
		
		return owned;
	}

}
