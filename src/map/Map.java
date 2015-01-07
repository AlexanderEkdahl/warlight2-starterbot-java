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

	public Map()
	{
		this.regions = new HashMap<Integer, Region>();
		this.superRegions = new ArrayList<SuperRegion>();
	}

	/**
	 * add a Region to the map
	 * @param region : Region to be added
	 */
	public void add(Region region)
	{
		regions.put(region.getId(), region);
	}

	/**
	 * add a SuperRegion to the map
	 * @param superRegion : SuperRegion to be added
	 */
	public void add(SuperRegion superRegion)
	{
		for(SuperRegion s : superRegions)
			if(s.getId() == superRegion.getId())
			{
				System.err.println("SuperRegion cannot be added: id already exists.");
				return;
			}
		superRegions.add(superRegion);
	}

	/**
	 * @return : a new Map object exactly the same as this one
	 */
	public Map getMapCopy() {
		Map newMap = new Map();

		for(SuperRegion sr : superRegions) //copy superRegions
		{
			SuperRegion newSuperRegion = new SuperRegion(sr.getId(), sr.getArmiesReward());
			newMap.add(newSuperRegion);
		}
		for(Region r : regions.values()) //copy regions
		{
			Region newRegion = new Region(r.getId(), newMap.getSuperRegion(r.getSuperRegion().getId()), r.getPlayerName(), r.getArmies());
			newMap.add(newRegion);
		}
		for(Region r : regions.values()) //add neighbors to copied regions
		{
			Region newRegion = newMap.getRegion(r.getId());
			for(Region neighbor : r.getNeighbors())
				newRegion.addNeighbor(newMap.getRegion(neighbor.getId()));
		}

		return newMap;
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
	 * @param id : a Region id number
	 * @return : the matching Region object
	 */
	public Region getRegion(int id)
	{
		return regions.get(id);
	}

	/**
	 * @param id : a SuperRegion id number
	 * @return : the matching SuperRegion object
	 */
	public SuperRegion getSuperRegion(int id)
	{
		for(SuperRegion superRegion : superRegions)
			if(superRegion.getId() == id)
				return superRegion;
		return null;
	}

	public ArrayList<Region> getOwned(String name){
		ArrayList<Region> owned = new ArrayList<Region>();

		for (Region r : regions.values()){
			if (name.equals(r.getPlayerName())) {
				owned.add(r);
			}
		}

		return owned;
	}
}
