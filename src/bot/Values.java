package bot;

import imaginary.EnemyAppreciator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Set;

import commanders.OffensiveCommander;
import concepts.Outcome;
import map.Map;
import map.Region;
import map.SuperRegion;

public class Values {

	// ////// REQUIRED FORCES FOR CERTAIN ACTIONS

	public static final int extraArmiesDefence = 1;
	public static final int unknownRegionAppreciatedRequiredForcesAttack = 5;

	// ////// REWARDS

	public static final double staticPocketDefence = 30;
	public static final double rewardMultiplier = 40;
	public static final double regionConnectionBonus = 1;
	public static final double staticRegionBonus = 0;
	public static final double valueDenialMultiplier = 18;
	public static final double rewardDefenseImportanceMultiplier = 30;
	public static final double rewardGriefDefenseMultiplier = 20;

	// ////// COSTS

	public static final double costMultiplierEnemy = 2;
	public static final double costMultiplierNeutral = 4;
	public static final double staticCostUnknown = 9;
	public static final double staticCostUnknownEnemy = 6;
	public static final double multipleFrontPenalty = 5;
	public static final double staticRegionCost = 3;
	public static final double costMultiplierDefendingAgainstEnemy = 0.5;
	public static final double superRegionExponentialMultiplier = 1.1;
	public static final double enemyVicinityExponentialPenalty = 1.2;

	// ////// SATISFACTION

	public static final double maxSuperRegionSatisfactionMultiplier = 1.5;
	public static final double maxRegionSatisfactionMultiplier = 1;

	private static double startingRegion(SuperRegion s) {
		double initialNeutral = s.getInitialNeutralCount();
		double subRegions = s.getSubRegions().size();
		double reward = s.getArmiesReward();

		double weight = reward * 1.5 / ((initialNeutral * 2) + subRegions);
		return weight;
	}

	public static Region getBestStartRegion(ArrayList<Region> pickableStartingRegions, Map map) {
		Region maxRegion = pickableStartingRegions.get(0);
		double maxValue = Double.MIN_VALUE;

		Region startingRegion = OffensiveCommander.determineStartPosition(pickableStartingRegions, map);

		return startingRegion;

	}

	public static double calculateSuperRegionWorth(SuperRegion s) {
		if (s.getArmiesReward() < 1) {
			return -1;
		} else {

			int reward = s.getArmiesReward();
			return ((reward * Values.rewardMultiplier) + Values.staticRegionBonus);

		}
	}


	public static void testOutcome(){
		Outcome outcome1 = calculateAttackOutcome(3, 1);
		System.out.println("3 against 1: attacking:" + outcome1.getAttackingArmies() + " defending: " + outcome1.getDefendingArmies());
		outcome1 = calculateAttackOutcome(10, 5);
		System.out.println("10 against 5: attacking:" + outcome1.getAttackingArmies() + " defending: " + outcome1.getDefendingArmies());
		outcome1 = calculateAttackOutcome(5, 10);
		System.out.println("5 against 10: attacking:" + outcome1.getAttackingArmies() + " defending: " + outcome1.getDefendingArmies());
		outcome1 = calculateAttackOutcome(3, 2);
		System.out.println("3 against 2: attacking:" + outcome1.getAttackingArmies() + " defending: " + outcome1.getDefendingArmies());
		outcome1 = calculateAttackOutcome(1, 1);
		System.out.println("1 against 1: attacking:" + outcome1.getAttackingArmies() + " defending: " + outcome1.getDefendingArmies());
	}
	public static Outcome calculateAttackOutcome(int attacking, int defending) {
		if (defending < 1){
			return null;
		}
		
		if (attacking == 1) {
			return new Outcome(0, Math.max(1, defending - 1));
		}

		int defendingCopy = defending;
		defending = (int) Math.round(Math.max(defending - ((0.6 * attacking)), 0));
		attacking = (int) Math.round(Math.max(attacking - ((0.7 * defendingCopy)), 0)) ;

		return new Outcome(attacking, defending);

	}

	public static double calculateRegionWeighedCost(Region r) {
		// enemy region
		double totalCost = 0;
		totalCost += calculateRegionInitialCost(r);
		totalCost += staticRegionCost;

		return totalCost;
	}

	public static double calculateRegionInSuperRegionsWeighedCost(Region r) {
		double totalCost = 0;
		totalCost += calculateRegionInitialCost(r);
		if (!r.getPlayerName().equals(BotState.getMyName())) {
			totalCost += staticRegionCost;
		}
		return totalCost;

	}

	private static double calculateRegionInitialCost(Region r) {
		// enemy region
		if (r.getPlayerName().equals(BotState.getMyOpponentName())) {
			if (r.getVisible()) {
				return r.getArmies() * costMultiplierEnemy;
			} else {
				return staticCostUnknownEnemy;
			}
			// neutral region
		} else if (r.getPlayerName().equals("neutral")) {
			if (r.getVisible() == false && r.getWasteland()) {
				return costMultiplierNeutral * 10;
			} else if (r.getVisible() == false && !r.getWasteland()) {
				return staticCostUnknown;
			} else {
				return r.getArmies() * costMultiplierNeutral;
			}

			// unknown region
		} else if (r.getPlayerName().equals("unknown")) {
			if (r.getWasteland()) {
				return costMultiplierNeutral * 10;
			} else {
				return staticCostUnknown;
			}

			// my region
		} else if (r.getPlayerName().equals(BotState.getMyName())) {
			return 0;
		} else {
			// this shouldn't happen
			return (Double) null;
		}

	}

	public static double calculateSuperRegionWeighedCost(SuperRegion sr, Map map) {
		double totalCost = 1;
		for (Region r : sr.getSubRegions()) {
			totalCost += calculateRegionInSuperRegionsWeighedCost(r);
		}

		// add some kind of exponential growth to discourage attacking enormous
		// regions
		totalCost *= Math.pow(superRegionExponentialMultiplier, sr.getSubRegions().size());
		totalCost *= calculateSuperRegionVulnerability(sr, map);

		return totalCost;
	}

	private static double calculateSuperRegionVulnerability(SuperRegion sr, Map map) {
		// determine if this superregion borders an enemy region and if so how
		// many

		ArrayList<Region> enemyPositions = map.getEnemyRegions();
		ArrayList<Region> enemyNeighbors = new ArrayList<Region>();
		for (Region r : enemyPositions) {
			for (Region n : r.getNeighbors()) {
				if (n.getSuperRegion().getId() == (sr.getId())) {
					enemyNeighbors.add(r);
					break;
				}
			}
		}
		if (enemyNeighbors.size() == 0) {
			// calculate instead the distance to the closest enemy
		}

		return Math.pow(enemyVicinityExponentialPenalty, enemyNeighbors.size());

	}

	public static int calculateRequiredForcesAttack(Region r) {

		// minimum number of forces required for an attack, should probably be
		// able to win

		int armySize = r.getArmies();
		if (r.getPlayerName().equals("unknown")) {
			return unknownRegionAppreciatedRequiredForcesAttack;
		} else if (r.getPlayerName().equals(BotState.getMyName())) {
			return 0;
		}

		else if (armySize <= 2) {
			return armySize + 1;
		} else if (armySize <= 3) {
			return armySize + 2;
		} else if (armySize <= 5) {
			return armySize + 3;
		} else {
			return (int) (armySize * 1.5);
		}

	}

	public static int calculateRequiredForcesAttackTotalVictory(Region r) {

		// maximum number of forces required for an attack, should definitely be
		// able to win

		int armySize = r.getArmies();
		if (r.getPlayerName().equals("unknown")) {
			return unknownRegionAppreciatedRequiredForcesAttack;
		} else if (r.getPlayerName().equals(BotState.getMyName())) {
			return 0;
		} else if (r.getPlayerName().equals("neutral")) {
			if (armySize <= 2) {
				return armySize + 1;
			} else {
				return (int) (armySize * 2);
			}
		} else if (armySize <= 3) {
			return armySize + 3;
		} else if (armySize <= 5) {
			return armySize + 6;
		} else {
			return (int) (armySize * 2);
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

	public static HashMap<SuperRegion, Integer> calculateSuperRegionSatisfaction(Map map) {
		HashMap<SuperRegion, Integer> roomLeft = new HashMap<SuperRegion, Integer>();
		for (SuperRegion s : map.getSuperRegions()) {
			if (s.ownedByPlayer(BotState.getMyName())) {
				roomLeft.put(s, Integer.MAX_VALUE);
			} else {
				roomLeft.put(s, (int) ((Values.calculateRequiredForcesAttack(s)) * maxSuperRegionSatisfactionMultiplier));
			}

		}
		return roomLeft;
	}

	public static HashMap<Region, Integer> calculateRegionSatisfaction(Map map) {
		HashMap<Region, Integer> roomLeft = new HashMap<Region, Integer>();
		for (Region r : map.getRegionList()) {
			if (!r.getPlayerName().equals(BotState.getMyName()))
				roomLeft.put(r, calculateRequiredForcesAttackTotalVictory(r));
			else {
				roomLeft.put(r, Values.calculateRequiredForcesDefend(r));
			}

		}

		return roomLeft;
	}

	public static Double calculateRegionOffensiveWorth(Region r) {
		int untakenRegions = 0;
		for (Region n : r.getNeighbors()) {
			if (!n.getPlayerName().equals(BotState.getMyName())) {
				untakenRegions++;
			}
		}
		return untakenRegions * regionConnectionBonus;

	}

}
