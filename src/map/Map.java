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
				System.err
						.println("SuperRegion cannot be added: id already exists.");
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
	
	public ArrayList<Region> getUnOwnedRegionsInSuperRegion(String name, SuperRegion s){
		ArrayList<Region> unOwned = new ArrayList<Region>();
		for (Region r : s.getSubRegions()){
			if (!r.getPlayerName().equals(name)){
				unOwned.add(r);
			}
		}
		
		return unOwned;
		
	}


	private int getSuspectedOwnedRegion(Region region, String opponentPlayerName) {
		if (region.getPlayerName().equals(opponentPlayerName)) {
			return 1;
		} else if (region.getPlayerName().equals("unknown")) {
			return 0;
		} else
			return -10000;
	}

	private boolean getSuspectedOwnedSuperRegion(SuperRegion superRegion,
			String opponentPlayerName) {
		int total = 0;
		for (Region r : superRegion.getSubRegions()) {
			total += getSuspectedOwnedRegion(r, opponentPlayerName);
		}
		if (total > 0) {
			return true;
		}
		return false;

	}

	public ArrayList<SuperRegion> getSuspectedOwnedSuperRegions(
			String opponentPlayerName) {
		ArrayList<SuperRegion> suspected = new ArrayList<SuperRegion>();
		ArrayList<SuperRegion> owned = new ArrayList<SuperRegion>();
		for (SuperRegion sr : getSuperRegions()) {
			if (getSuspectedOwnedSuperRegion(sr, opponentPlayerName))
				suspected.add(sr);

		}

		return suspected;
	}
	

	public ArrayList<Region> getOwnedFrontRegions(BotState state) {

		ArrayList<SuperRegion> ownedSuperRegions = getOwnedSuperRegions(state.getMyPlayerName());
		ArrayList<Region> ownedRegionsInOwnedSuperRegions = new ArrayList<Region>();
		ArrayList<Region> neighbors;
		ArrayList<Region> front = new ArrayList<Region>();
		
		for (SuperRegion s : ownedSuperRegions){
			ownedRegionsInOwnedSuperRegions.addAll(s.getSubRegions());
		}
		
		for (Region r : ownedRegionsInOwnedSuperRegions) {
			neighbors = r.getNeighbors();
			for (Region n : neighbors) {
				if (n.getPlayerName().equals(state.getOpponentPlayerName())) {
					front.add(r);
					continue;
				}
			}
		}

		return front;

	}

	

	public ArrayList<SuperRegion> getOwnedFrontSuperRegions(BotState state) {
		ArrayList<SuperRegion> sFront = new ArrayList<SuperRegion>();
		ArrayList<Region> rFront = getOwnedFrontRegions(state);
		for (Region r : rFront) {
			if (!sFront.contains(r.getSuperRegion())) {
				sFront.add(r.getSuperRegion());
			}
		}

		return sFront;
	}

	public ArrayList<Region> getPockets(BotState state) {
		ArrayList<Region> owned = getOwnedRegions(state.getMyPlayerName());
		ArrayList<Region> pockets = new ArrayList<Region>();
		
		outerLoop:
		for (Region r : owned){
			for (Region n :  r.getNeighbors()){
				if (n.getPlayerName().equals(state.getMyPlayerName())){
					continue outerLoop;
				}
			}
			pockets.add(r);
		}
		
		
		
		return pockets;
	}

	public ArrayList<Region> getRewardBlockers(BotState state) {
		// TODO Auto-generated method stub
		return null;
	}
	
	 

}
