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

	public boolean getSuspectedOwnedSuperRegion() {
		int total = 0;
		int totalRequired = getSubRegions().size() / 2;
		for (Region r : getSubRegions()) {
			total += r.getSuspectedOwnedRegion();
		}
		if (total >= totalRequired) {
			return true;
		}
		return false;

	}

	public SuperRegion duplicate() {
		SuperRegion newSuperRegion = new SuperRegion(this.id, this.armiesReward);
		return newSuperRegion;

	}

	public String toString() {
		return "SuperRegion: " + id + " Reward: " + armiesReward;
	}

	public ArrayList<Region> getUnownedRegions() {
		ArrayList<Region> unowned = new ArrayList<Region>();
		for (Region r : subRegions) {
			if (!r.getPlayerName().equals(BotState.getMyName())) {
				unowned.add(r);
			}
		}

		return unowned;
	}

	public Collection<? extends Region> getAnnoyingRegions() {
		// determine if contested
		ArrayList<Region> annoyingRegions = new ArrayList<Region>();
		if (isContested()) {
			for (Region r : subRegions) {
				if (r.getPlayerName().equals(BotState.getMyOpponentName()) && r.hasNeighborWithName(BotState.getMyName())) {
					annoyingRegions.add(r);

				}
			}
		}
		return annoyingRegions;
	}

	private boolean isContested() {
		// contested is defined as having presense of both players
		boolean hasFriendlyPresence = false;
		boolean hasEnemyPresence = false;

		for (Region r : subRegions) {
			if (r.getPlayerName().equals(BotState.getMyName())) {
				hasFriendlyPresence = true;
			} else if (r.getPlayerName().equals(BotState.getMyOpponentName())) {
				hasEnemyPresence = true;
			}
		}
		return (hasFriendlyPresence && hasEnemyPresence);
	}

	public boolean isFront() {
		for (Region r : subRegions) {
			if (r.isFront()) {
				return true;
			}
		}
		return false;
	}

}
