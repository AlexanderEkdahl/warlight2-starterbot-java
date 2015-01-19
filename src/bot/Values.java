package bot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;

import map.Region;
import map.SuperRegion;

public class Values {
	private static final int costMultiplierEnemy = 1;
	private static final int costMultiplierNeutral = 3;
	private static final int staticCostUnknown = 10;
	private static final int staticCostOwned = 10;
	private static final int staticCostUnknownEnemy = 3;
	private static final int maxSuperRegionSatisfactionMultiplier = 1;
	private static final int maxRegionSatisfactionMultiplier = 2;

	private static float startingRegion(int neutrals, int reward) {
		if (reward == 0) {
			return Integer.MIN_VALUE;
		}

		return (reward * 2) / neutrals;
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

	public static int calculateRegionWeighedCost(String mName, String eName, Region r) {
		if (r.getPlayerName().equals(eName)) {
			if (r.getVisible()) {
				return r.getArmies() * costMultiplierEnemy;
			} else {
				return staticCostUnknownEnemy;
			}
		} else if (r.getPlayerName().equals("neutral")) {
			return r.getArmies() * costMultiplierNeutral;
		} else if (r.getPlayerName().equals("unknown")) {
			return staticCostUnknown;
		} else if (r.getWasteland() && !r.getPlayerName().equals(mName)) {
			return costMultiplierNeutral * 10;
		}
		return staticCostOwned;
	}

	public static int calculateRegionInSuperRegionWeighedCost(String enemyName,
			Region r) {
		if (r.getPlayerName().equals(enemyName)) {
			if (r.getVisible()) {
				return r.getArmies() * costMultiplierEnemy;
			} else {
				return staticCostUnknownEnemy;
			}
		} else if (r.getPlayerName().equals("neutral")) {
			return r.getArmies() * costMultiplierNeutral;
		} else if (r.getPlayerName().equals("unknown")) {
			return staticCostUnknown;
		} else if (r.getWasteland() && !r.getPlayerName().equals(enemyName)) {
			return costMultiplierNeutral * 10;
		}
		return 1;
	}

	public static int calculateSuperRegionWeighedCost(String enemyName,
			SuperRegion sr) {
		int totalCost = 1;
		for (Region r : sr.getSubRegions()) {
			totalCost += calculateRegionInSuperRegionWeighedCost(enemyName, r);
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
		if (armySize <= 3) {
			return armySize + 2;
		} else if (armySize <= 5) {
			return armySize + 3;
		} else {
			return (int) (armySize * 1.5);
		}

	}

	public static int calculateRequiredForcesAttackTotalVictory(String myName,
			Region r) {

		// these numbers will be prone to change

		int armySize = r.getArmies();
		if (r.getPlayerName().equals("unknown")) {
			return 100;
		} else if (r.getPlayerName().equals(myName)) {
			return 0;
		}
		else if (r.getPlayerName().equals("neutral")){
			return (int) (armySize *2.5);
		}
		if (armySize <= 3) {
			return armySize + 5;
		} else if (armySize <= 5) {
			return armySize + 7;
		} else {
			return (int) (armySize * 2.5);
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

	public static HashMap<SuperRegion, Integer> calculateSuperRegionSatisfaction(
			BotState state) {
		HashMap<SuperRegion, Integer> roomLeft = new HashMap<SuperRegion, Integer>();
		for (SuperRegion s : state.getFullMap().getSuperRegions()) {
			roomLeft.put(
					s,
					((Values.calculateRequiredForcesAttack(
							state.getMyPlayerName(), s)) * maxSuperRegionSatisfactionMultiplier));
		}
		return roomLeft;
	}

	public static HashMap<Region, Integer> calculateRegionSatisfaction(
			BotState state) {
		HashMap<Region, Integer> roomLeft = new HashMap<Region, Integer>();
		for (Region r : state.getFullMap().getRegionList()) {
			roomLeft.put(
					r,
					calculateRequiredForcesAttackTotalVictory(
							state.getMyPlayerName(), r));
		}

		return roomLeft;
	}
}
