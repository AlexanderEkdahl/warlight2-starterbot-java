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
import java.util.HashSet;
import java.util.Set;

import bot.BotState;

public class Region {
	private int id;
	private ArrayList<Region> neighbors;

	private SuperRegion superRegion;
	private int armies;
	private String playerName;
	private boolean wasteland;
	private boolean visible;

	public Region(int id, SuperRegion superRegion) {
		this(id, superRegion, "unknown", 0);
	}

	public Region(int id, SuperRegion superRegion, String playerName, int armies) {
		this.id = id;
		this.superRegion = superRegion;
		this.neighbors = new ArrayList<Region>();
		this.playerName = playerName;
		this.armies = armies;

		superRegion.addSubRegion(this);
	}

	public Region(int id, SuperRegion superRegion, String playerName, int armies, boolean visible, boolean wasteland) {
		this.id = id;
		this.superRegion = superRegion;
		this.neighbors = new ArrayList<Region>();
		this.playerName = playerName;
		this.armies = armies;
		this.visible = visible;
		this.wasteland = wasteland;
	}

	public void addNeighbor(Region neighbor) {
		if (!neighbors.contains(neighbor)) {
			neighbors.add(neighbor);
			neighbor.addNeighbor(this);
		}
	}

	/**
	 * @param region
	 *            a Region object
	 * @return True if this Region is a neighbor of given Region, false
	 *         otherwise
	 */
	public boolean isNeighbor(Region region) {
		if (neighbors.contains(region))
			return true;
		return false;
	}

	/**
	 * @param armies
	 *            Sets the number of armies that are on this Region
	 */
	public void setArmies(int armies) {
		this.armies = armies;
	}

	/**
	 * @param playerName
	 *            Sets the Name of the player that this Region belongs to
	 */
	public void setPlayerName(String playerName) {
		this.playerName = playerName;
	}

	/**
	 * @return The id of this Region
	 */
	public int getId() {
		return id;
	}

	/**
	 * @return A list of this Region's neighboring Regions
	 */
	public ArrayList<Region> getNeighbors() {
		return neighbors;
	}

	/**
	 * @return The SuperRegion this Region is part of
	 */
	public SuperRegion getSuperRegion() {
		return superRegion;
	}

	/**
	 * @return The number of armies on this region
	 */
	public int getArmies() {
		return armies;
	}

	/**
	 * @return A string with the name of the player that owns this region
	 */
	public String getPlayerName() {
		return playerName;
	}

	public void setWasteland(boolean wasteland) {
		this.wasteland = wasteland;
	}

	@Override
	public String toString() {
		return "Region{" + "id=" + id + ", armies=" + armies + ", playerName=" + playerName + ", visible=" + visible + '}';
	}

	public boolean getWasteland() {
		return wasteland;
	}

	public void setVisible(boolean visible) {
		this.visible = visible;
	}

	public boolean getVisible() {
		return visible;
	}

	public void setNeighbors(ArrayList<Region> neighbors) {
		this.neighbors = neighbors;
	}

	public int getTotalThreateningForce() {
		int totalForce = 0;
		for (Region n : neighbors) {
			if (n.getPlayerName().equals(BotState.getMyOpponentName())) {
				totalForce += n.getArmies() - 1;
			}

		}
		return totalForce;
	}

	public int getHighestThreateningForce() {
		int maxForce = 0;
		for (Region n : neighbors) {
			if (n.getPlayerName().equals(BotState.getMyOpponentName())) {
				if (n.getArmies() - 1 > maxForce) {
					maxForce = n.getArmies();
				}
			}

		}
		return maxForce;
	}

	public ArrayList<Region> getEnemyNeighbors() {
		ArrayList<Region> enemyNeighbors = new ArrayList<Region>();
		for (Region r : neighbors) {
			if (r.getPlayerName().equals(BotState.getMyOpponentName())) {
				enemyNeighbors.add(r);
			}
		}
		return enemyNeighbors;

	}

	public int getSuspectedOwnedRegion() {
		if (playerName.equals(BotState.getMyOpponentName())) {
			return 1;
		}
		else if (!visible){
			return 0;
		}
		else{
			return -1000;
		}
	}

	public Region duplicateInto(SuperRegion s) {
		Region newRegion = new Region(this.id, s, this.playerName, this.armies, this.visible, this.wasteland);
		s.addSubRegion(newRegion);
		return newRegion;

	}

	public boolean hasNeighborWithName(String myName) {
		for (Region n : getNeighbors()) {
			if (n.getPlayerName().equals(myName)) {
				return true;
			}
		}
		return false;
	}

	public boolean hasNeighborWithOtherOwner() {
		for (Region n : neighbors) {
			if (!n.getPlayerName().equals(playerName)) {
				return true;
			}
		}

		return false;

	}

//	public boolean isOnlyFriendlyRegionInSuperRegion() {
//		if (playerName != BotState.getMyName()){
//			return false;
//		}
//		for (Region r : superRegion.getSubRegions()) {
//			if (!r.equals(this) && !r.getPlayerName().equals(BotState.getMyOpponentName())) {
//				return false;
//			}
//		}
//
//		return true;
//	}

	public Collection<? extends Region> getUnOwnedNeighbors() {
		Set<Region> unOwnedNeighbors = new HashSet<Region>();
		for (Region n : neighbors){
			if (!n.getPlayerName().equals(BotState.getMyName())){
				unOwnedNeighbors.add(n);
			}
		}
		
		return unOwnedNeighbors;
	}
	
	public boolean isFront(){
		for (Region n : neighbors){
			if (n.getPlayerName().equals(BotState.getMyOpponentName())){
				return true;
			}
		}
		
		return false;
	}

}
