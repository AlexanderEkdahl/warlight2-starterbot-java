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
import java.util.LinkedList;

public class SuperRegion {
	private int id;
	private int armiesReward;
	private LinkedList<Region> subRegions;

	public SuperRegion(int id, int armiesReward) {
		this.id = id;
		this.armiesReward = armiesReward;
		subRegions = new LinkedList<Region>();
	}

	public void addSubRegion(Region subRegion) {
		if(!subRegions.contains(subRegion))
			subRegions.add(subRegion);
	}

	/**
	 * @return A string with the name of the player that fully owns this SuperRegion
	 */
	public boolean ownedByPlayer(String name)	{
		for(Region region : subRegions) {
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
	 * @return The number of armies a Player is rewarded when he fully owns this SuperRegion
	 */
	public int getArmiesReward() {
		return armiesReward;
	}

	/**
	 * @return A list with the Regions that are part of this SuperRegion
	 */
	public LinkedList<Region> getSubRegions() {
		return subRegions;
	}

	/**
	* @return The number of neutrals in this superregion at the start of the game
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
}
