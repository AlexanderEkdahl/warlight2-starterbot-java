package bot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Set;

import map.Region;
import map.SuperRegion;

public class Values {
	public static final int costMultiplierEnemy = 1;
	public static final float costMultiplierDefendingAgainstEnemy = 0.5f;
	
	public static final int costMultiplierNeutral = 3;
	public static final int staticCostUnknown = 5;
	public static final int staticCostOwned = 10;
	public static final int staticCostUnknownEnemy = 8;
	public static final int multipleFrontPenalty = 5;
	public static final float maxSuperRegionSatisfactionMultiplier = 1.5f;
	public static final int maxRegionSatisfactionMultiplier = 1;
	public static final int staticRegionCost = 3;

	private static float startingRegion(SuperRegion s) {
		if (s.getArmiesReward() == 0) {
			return Integer.MIN_VALUE;
		}

		return (s.getArmiesReward() * 2)
				/ (s.getInitialNeutralCount() + s.getSubRegions().size());
	}

	public static Region getBestStartRegion(
			ArrayList<Region> pickableStartingRegions) {
		Region maxRegion = null;
		float maxValue = Integer.MIN_VALUE;
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

	public static int calculateRegionWeighedCost(String mName, String eName,
			Region r) {
		if (r.getPlayerName().equals(eName)) {
			if (r.getVisible()) {
				return r.getArmies() * costMultiplierEnemy;
			} else {
				return staticCostUnknownEnemy;
			}
		} else if (r.getPlayerName().equals("neutral")) {
			if (r.getVisible() == false && r.getWasteland()) {
				return costMultiplierNeutral * 10;
			} else {
				return r.getArmies() * costMultiplierNeutral;
			}

		} else if (r.getPlayerName().equals("unknown")) {
			return staticCostUnknown;
		}
		return staticCostOwned;
	}

	public static int calculateRegionInSuperRegionWeighedCost(String mName,
			String eName, Region r) {
		if (r.getPlayerName().equals(eName)) {
			if (r.getVisible()) {
				return r.getArmies() * costMultiplierEnemy;
			} else {
				return staticCostUnknownEnemy;
			}
		} else if (r.getPlayerName().equals("neutral")) {
			if (r.getVisible() == false && r.getWasteland()) {
				return costMultiplierNeutral * 10;
			} else {
				return r.getArmies() * costMultiplierNeutral;
			}

		} else if (r.getPlayerName().equals("unknown")) {
			return staticCostUnknown;
		}
		return 0;
	}

	public static int calculateSuperRegionWeighedCost(String mName,
			String eName, SuperRegion sr) {
		int totalCost = 1;
		for (Region r : sr.getSubRegions()) {
			totalCost += calculateRegionInSuperRegionWeighedCost(mName, eName,
					r) + staticRegionCost;
		}
		return totalCost;
	}

	public static int calculateRequiredForcesAttack(String mName, Region r) {

		// these numbers will be prone to change

		int armySize = r.getArmies();
		if (r.getPlayerName().equals("unknown")) {
			return 6;
		} else if (r.getPlayerName().equals(mName)) {
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

	public static int calculateRequiredForcesAttackTotalVictory(String myName,
			Region r) {

		// these numbers will be prone to change

		int armySize = r.getArmies();
		if (r.getPlayerName().equals("unknown")) {
			return 100;
		} else if (r.getPlayerName().equals(myName)) {
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

	public static int calculateRequiredForcesAttack(String myName, SuperRegion s) {
		int totalRequired = 1;
		ArrayList<Region> regions = s.getSubRegions();

		for (Region r : regions) {
			totalRequired += calculateRequiredForcesAttack(myName, r);
		}

		return totalRequired;

	}

	public static int calculateRequiredForcesDefend(String mName, String eName,
			SuperRegion s) {
		return s.getTotalThreateningForce(eName)
				- s.getTotalFriendlyForce(mName);
	}

	public static HashMap<SuperRegion, Integer> calculateSuperRegionSatisfaction(
			BotState state) {
		String mName = state.getMyPlayerName();
		String eName = state.getOpponentPlayerName();
		HashMap<SuperRegion, Integer> roomLeft = new HashMap<SuperRegion, Integer>();
		for (SuperRegion s : state.getFullMap().getSuperRegions()) {
			if (s.ownedByPlayer(state.getMyPlayerName())) {
				roomLeft.put(s,
						Values.calculateRequiredForcesDefend(mName, eName, s));
			} else {
				roomLeft.put(
						s,
						(int) ((Values.calculateRequiredForcesAttack(mName, s)) * maxSuperRegionSatisfactionMultiplier));
			}

		}
		return roomLeft;
	}

	public static HashMap<Region, Integer> calculateRegionSatisfaction(
			BotState state) {
		HashMap<Region, Integer> roomLeft = new HashMap<Region, Integer>();
		String mName = state.getMyPlayerName();
		String eName = state.getOpponentPlayerName();

		for (Region r : state.getFullMap().getRegionList()) {
			if (!r.getPlayerName().equals(mName))
				roomLeft.put(
						r,
						calculateRequiredForcesAttackTotalVictory(
								state.getMyPlayerName(), r));
			else {
				roomLeft.put(r,
						Values.calculateRequiredForcesDefend(mName, eName, r));
			}

		}

		return roomLeft;
	}

	public static Integer calculateRequiredForcesDefend(String mName,
			String eName, Region r) {
		return r.getHighestThreateningForce(eName) - r.getArmies();
	}

}
