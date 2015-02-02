package bot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Set;

import map.Region;
import map.SuperRegion;

public class Values {

	// ////// REQUIRED FORCES FOR CERTAIN ACTIONS

	public static final int extraArmiesDefence = 5;
	public static final int staticPocketDefence = 30;
	public static final int staticSuperRegionDefence = 0;

	// ////// REWARDS

	public static final float rewardMultiplier = 40;
	public static final float staticRegionBonus = 0;
	public static final float valueDenialMultiplier = 12;
	public static final float rewardDefenseImportanceMultiplier = 40;

	// ////// COSTS

	public static final float costMultiplierEnemy = 2;
	public static final float costMultiplierNeutral = 4;
	public static final float staticCostUnknown = 5;
	public static final float staticCostOwned = 5;
	public static final float staticCostUnknownEnemy = 8;
	public static final float multipleFrontPenalty = 5;
	public static final float staticRegionCost = 3;
	public static final float costMultiplierDefendingAgainstEnemy = 0.5f;

	// ////// SATISFACTION

	public static final float maxSuperRegionSatisfactionMultiplier = 1.5f;
	public static final int maxRegionSatisfactionMultiplier = 1;

	private static float startingRegion(SuperRegion s) {
		float initialNeutral = s.getInitialNeutralCount();
		float subRegions = s.getSubRegions().size();
		float reward = s.getArmiesReward();

		float weight = reward / ((initialNeutral * 2) + subRegions);
		return weight;
	}

	public static Region getBestStartRegion(ArrayList<Region> pickableStartingRegions) {
		Region maxRegion = null;
		float maxValue = Float.MIN_VALUE;
		for (Region currentRegion : pickableStartingRegions) {
			SuperRegion superRegion = currentRegion.getSuperRegion();
			float value = Values.startingRegion(superRegion);
			if (value >= maxValue) {
				maxValue = value;
				maxRegion = currentRegion;
			}
		}

		return maxRegion;

	}

	public static float calculateSuperRegionWorth(SuperRegion s) {
		if (s.getArmiesReward() < 1) {
			return -1;
		} else {
			int reward = s.getArmiesReward();
			return ((reward * Values.rewardMultiplier) + Values.staticRegionBonus);

		}
	}

	public static float calculateRegionWeighedCost(Region r) {
		if (r.getPlayerName().equals(BotState.getMyOpponentName())) {
			if (r.getVisible()) {
				return r.getArmies() * costMultiplierEnemy + staticRegionCost;
			} else {
				return staticCostUnknownEnemy;
			}
		} else if (r.getPlayerName().equals("neutral")) {
			if (r.getVisible() == false && r.getWasteland()) {
				return costMultiplierNeutral * 10 + staticRegionCost;
			} else if (r.getVisible() == false && !r.getWasteland()) {
				return staticCostUnknown + staticRegionCost;
			} else {
				return r.getArmies() * costMultiplierNeutral + staticRegionCost;
			}

		} else if (r.getPlayerName().equals("unknown")) {
			if (r.getWasteland()) {
				return costMultiplierNeutral * 10 + staticRegionCost;
			} else {
				return staticCostUnknown + staticRegionCost;
			}
		} else if (r.getPlayerName().equals(BotState.getMyName())) {
			return staticRegionCost;
		} else {
			// this shouldn't happen
			return (Float) null;
		}
	}

	public static float calculateRegionInSuperRegionWeighedCost(Region r) {
		if (r.getPlayerName().equals(BotState.getMyOpponentName())) {
			if (r.getVisible()) {
				return r.getArmies() * costMultiplierEnemy + staticRegionCost;
			} else {
				return staticCostUnknownEnemy;
			}
		} else if (r.getPlayerName().equals("neutral")) {
			if (r.getVisible() == false && r.getWasteland()) {
				return costMultiplierNeutral * 10 + staticRegionCost;
			} else if (r.getVisible() == false && !r.getWasteland()) {
				return staticCostUnknown + staticRegionCost;
			} else {
				return r.getArmies() * costMultiplierNeutral + staticRegionCost;
			}
		} else if (r.getPlayerName().equals("unknown")) {
			if (r.getWasteland()) {
				return costMultiplierNeutral * 10 + staticRegionCost;
			} else {
				return staticCostUnknown + staticRegionCost;
			}
		} else if (r.getPlayerName().equals(BotState.getMyName())) {
			return 0;
		} else {
			// this shouldn't happen
			return (Float) null;
		}
	}

	public static int calculateSuperRegionWeighedCost(SuperRegion sr) {
		int totalCost = 1;
		for (Region r : sr.getSubRegions()) {
			totalCost += calculateRegionInSuperRegionWeighedCost(r);
		}
		return totalCost;
	}

	public static int calculateRequiredForcesAttack(Region r) {

		// these numbers will be prone to change

		int armySize = r.getArmies();
		if (r.getPlayerName().equals("unknown")) {
			return 6;
		} else if (r.getPlayerName().equals(BotState.getMyName())) {
			return 0;
		}

		else if (armySize <= 3) {
			return armySize + 1;
		} else if (armySize <= 5) {
			return armySize + 3;
		} else {
			return (int) (armySize * 1.5);
		}

	}

	public static int calculateRequiredForcesAttackTotalVictory(Region r) {

		// these numbers will be prone to change

		int armySize = r.getArmies();
		if (r.getPlayerName().equals("unknown")) {
			return 10;
		} else if (r.getPlayerName().equals(BotState.getMyName())) {
			return 0;
		} else if (r.getPlayerName().equals("neutral")) {
			if (armySize <= 2) {
				return armySize + 1;
			} else {
				return (int) (armySize * 2);
			}
		} else if (armySize <= 3) {
			return armySize + 8;
		} else if (armySize <= 5) {
			return armySize + 11;
		} else {
			return (int) (armySize * 2.5);
		}

	}

	public static int calculateRequiredForcesAttack(SuperRegion s) {
		int totalRequired = 1;
		ArrayList<Region> regions = s.getSubRegions();

		for (Region r : regions) {
			totalRequired += calculateRequiredForcesAttack(r);
		}

		return totalRequired;

	}

	public static int calculateRequiredForcesDefend(SuperRegion superRegion) {
		int total = 0;
		for (Region r : superRegion.getSubRegions()){
			total += calculateRequiredForcesDefend(r);
		}
		return total;
	}

	public static int calculateRequiredForcesDefendRegionAgainstSpecificRegions(ArrayList<Region> regions) {
		int total = 0;
		for (Region r : regions) {
			total += r.getArmies() - 1;
		}

		if (regions.size() > 0) {
			total += extraArmiesDefence;
		}

		return total;
	}

	public static int calculateRequiredForcesDefend(Region region) {
		int total = 0;
		ArrayList<Region> enemyNeighbors = region.getEnemyNeighbors();

		for (Region r : enemyNeighbors) {
			total += r.getArmies() - 1;
		}
		if (enemyNeighbors.size() > 0) {
			total += extraArmiesDefence;
		}

		return total;

	}

	public static HashMap<SuperRegion, Integer> calculateSuperRegionSatisfaction(BotState state) {
		String mName = state.getMyPlayerName();
		String eName = state.getOpponentPlayerName();
		HashMap<SuperRegion, Integer> roomLeft = new HashMap<SuperRegion, Integer>();
		for (SuperRegion s : state.getFullMap().getSuperRegions()) {
			if (s.ownedByPlayer(state.getMyPlayerName())) {
				roomLeft.put(s, Integer.MAX_VALUE);
			} else {
				roomLeft.put(s, (int) ((Values.calculateRequiredForcesAttack(s)) * maxSuperRegionSatisfactionMultiplier));
			}

		}
		return roomLeft;
	}

	public static HashMap<Region, Integer> calculateRegionSatisfaction(BotState state) {
		HashMap<Region, Integer> roomLeft = new HashMap<Region, Integer>();

		for (Region r : state.getFullMap().getRegionList()) {
			if (!r.getPlayerName().equals(BotState.getMyName()))
				roomLeft.put(r, calculateRequiredForcesAttackTotalVictory(r));
			else {
				roomLeft.put(r, Values.calculateRequiredForcesDefend(r));
			}

		}

		return roomLeft;
	}

}
