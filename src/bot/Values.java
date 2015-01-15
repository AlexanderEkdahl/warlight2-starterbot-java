package bot;

import java.util.ArrayList;
import java.util.LinkedList;

import map.Region;
import map.SuperRegion;

public class Values {
	public static final int costMultiplierEnemy = 1;
	public static final int costMultiplierNeutral = 3;
	public static final int staticCostUnknown = 10;
	public static final int staticCostOwned = 8;
	public static final int staticCostUnknownEnemy = 10;

	private static float startingRegion(int neutrals, int reward) {
		if (reward == 0) {
			return Integer.MIN_VALUE;
		}

		return (reward * 3) - neutrals;
	}

	public static Region getBestStartRegion(
			ArrayList<Region> pickableStartingRegions) {
		Region maxRegion = null;
		float maxValue = Integer.MIN_VALUE;
		for (Region currentRegion : pickableStartingRegions) {
			SuperRegion superRegion = currentRegion.getSuperRegion();

			float value = Values.startingRegion(
					superRegion.getInitialNeutralCount(),
					superRegion.getArmiesReward());
			if (value >= maxValue) {
				maxValue = value;
				maxRegion = currentRegion;
			}
		}

		return maxRegion;

	}

	public static int calculateRegionWeighedCost(String enemyName, Region r) {
		if (r.getPlayerName().equals(enemyName)) {
			if(r.getVisible()){
				return r.getArmies() * costMultiplierEnemy;
			}
			else{
				return staticCostUnknownEnemy;
			}
		}
		else if(r.getWasteland()){
			return costMultiplierNeutral * 10;
		}
		else if (r.getPlayerName().equals("neutral")) {
			return r.getArmies() * costMultiplierNeutral;
		} else if (r.getPlayerName().equals("unknown")) {
			return staticCostUnknown;
		}
		return staticCostOwned;
	}

	public static int calculateSuperRegionWeighedCost(String enemyName,
			SuperRegion sr) {
		int totalCost = 1;
		for (Region r : sr.getSubRegions()) {
			totalCost += calculateRegionWeighedCost(enemyName, r);
		}
		return totalCost;
	}

	public static int calculateRequiredForcesAttack(String myName, Region r) {

		// these numbers will be prone to change

		int armySize = r.getArmies();
		if (r.getPlayerName().equals("unknown")) {
			return 5;
		} else if (r.getPlayerName().equals(myName)) {
			return 1;
		}

		else if (armySize <= 2) {
			return armySize + 3;
		} else if (armySize <= 3) {
			return armySize + 4;
		} else if (armySize <= 5) {
			return armySize + 6;
		} else {
			return (int) (armySize * 1.5);
		}

	}

	public static int calculateRequiredForcesAttack(String myName, SuperRegion s) {
		int totalRequired = 1;
		ArrayList<Region> regions = s.getSubRegions();

		for (Region r : regions) {
			totalRequired += calculateRequiredForcesAttack(myName, r);
		}

		// System.err.println("The calculated cost of attacking the SuperRegion "
		// + s.getId() + " is " + totalRequired);
		return totalRequired;

	}
}
