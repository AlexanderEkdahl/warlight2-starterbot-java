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

import bot.BotState;

public class SuperRegion {
	private int id;
	private int armiesReward;
	private ArrayList<Region> subRegions;

	public SuperRegion(int id, int armiesReward) {
		this.id = id;
		this.armiesReward = armiesReward;
		subRegions = new ArrayList<Region>();
	}

	public void addSubRegion(Region subRegion) {
		if (!subRegions.contains(subRegion))
			subRegions.add(subRegion);
	}

	/**
	 * @return A string with the name of the player that fully owns this
	 *         SuperRegion
	 */
	public boolean ownedByPlayer(String name) {
		for (Region region : subRegions) {
			if (!name.equals(region.getPlayerName()))
				return false;
		}
		return true;
	}

	/**
	 * @return The id of this SuperRegion
	 */
	public int getId() {
		return id;
	}

	/**
	 * @return The number of armies a Player is rewarded when he fully owns this
	 *         SuperRegion
	 */
	public int getArmiesReward() {
		return armiesReward;
	}

	/**
	 * @return A list with the Regions that are part of this SuperRegion
	 */
	public ArrayList<Region> getSubRegions() {
		return subRegions;
	}

	/**
	 * @return The number of neutrals in this superregion at the start of the
	 *         game
	 */
	public int getInitialNeutralCount() {
		int neutrals = 0;

		for (Region region : this.getSubRegions()) {
			neutrals += 2;

			// Wasteland contains 10 neutral enemies
			if (region.getWasteland()) {
				neutrals += 8;
			}
		}

		return neutrals;
	}

	public ArrayList<Region> getFronts() {
		ArrayList<Region> fronts = new ArrayList<Region>();
		for (Region r : subRegions) {
			for (Region n : r.getNeighbors()) {
				if (n.getPlayerName().equals(BotState.getMyOpponentName())) {
					fronts.add(r);
					break;
				}
			}

		}
		return fronts;

	}

	public int getTotalThreateningForce() {
		ArrayList<Region> checked = new ArrayList<Region>();
		int totalForce = 0;
		for (Region r : subRegions) {
			for (Region n : r.getNeighbors()) {
				if (n.getPlayerName().equals(BotState.getMyOpponentName()) && !checked.contains(n)) {
					checked.add(n);
					totalForce += n.getArmies() - 1;
				}
			}

		}

		return totalForce;

	}

	public int getTotalFriendlyForce(String myName) {
		int totalForce = 0;
		for (Region r : getSubRegions()) {
			if (r.getPlayerName().equals(myName)) {
				totalForce += r.getArmies() - 1;
			}
		}
		return totalForce;
	}
	
	public boolean getSuspectedOwnedSuperRegion(String opponentPlayerName) {
		int total = 0;
		int totalRequired = getSubRegions().size() / 2;
		for (Region r : getSubRegions()) {
			total += r.getSuspectedOwnedRegion(opponentPlayerName);
		}
		if (total >= totalRequired) {
			return true;
		}
		return false;

	}
	public SuperRegion duplicate(){
		SuperRegion newSuperRegion = new SuperRegion(this.id, this.armiesReward);
		
		return newSuperRegion;
		
	}

}
