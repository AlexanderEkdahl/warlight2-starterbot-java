package bot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import commanders.OffensiveCommander;
import concepts.Outcome;
import map.Map;
import map.Pathfinder;
import map.Pathfinder.Path;
import map.Region;
import map.SuperRegion;
import math.Tables;

public class Values {
	private static HashMap<Integer, Double> latestTurnsNeededResult = new HashMap<Integer, Double>();
	private static HashMap<Integer, Integer> latestTurnsNeededCalculated = new HashMap<Integer, Integer>();

	// ENUMS

	public enum DefenseMode {
		MAX_ONLY, ALL, Percentage_OF_MAX_ONLY
	}

	// //// MODES

	public static final DefenseMode defenseMode = DefenseMode.ALL;
	public static final boolean tryToUseSmallDefensivePlacements = true;

	// ////// REQUIRED FORCES FOR CERTAIN ACTIONS
	public static final int unknownRegionAppreciatedRequiredForcesAttack = 3;
	public static final double percentageOfAttackingNeededForDefence = 0.8;

	// ////// REWARDS

	public static final double rewardMultiplier = 210;
	public static final double regionConnectionBonus = 0.2;
	public static final double staticRegionBonus = 0;
	public static final double valueDenialMultiplier = 20;
	public static final double rewardDefenseImportanceMultiplier = 35;
	public static final double rewardDefenseInheritanceMultiplier = 0.45;
	public static final double deficitDefenceExponentialMultiplier = 1.02;

	// ////// COSTS

	public static final double costUnitMultiplier = 14;
	public static final double costMultiplierEnemy = (4 / 5) * costUnitMultiplier;
	public static final double costMultiplierNeutral = 1 * costUnitMultiplier;
	public static final double staticCostUnknown = costMultiplierNeutral * 2;
	public static final double staticCostUnknownNeutral = costMultiplierNeutral * 2;
	public static final double staticCostUnknownEnemy = costMultiplierEnemy * 2;
	public static final double needsPlacementPenalty = 2;

	public static final double staticRegionCost = 9;
	public static final double superRegionSizeExponentialPenalty = 1.14;
	public static final double enemyVicinityExponentialPenalty = 1.12;
	public static final double internalHopsExponentialPenalty = 1.05;
	public static final double turnsNeededToTakeExponentialPenalty = 1.2;
	// public static final double multipleFrontExponentialPenalty = 1.1;

	// ////// PERFORMANCE
	public static boolean defensiveCommanderUseSmallPlacements = true;

	// //////

	public static Region getBestStartRegion(ArrayList<Region> pickableStartingRegions, Map map) {
		Region startingRegion = OffensiveCommander.determineStartPosition(pickableStartingRegions, map);
		return startingRegion;

	}

	public static double calculateSuperRegionWorth(SuperRegion s) {
		if (s.getArmiesReward() < 1) {
			return 0.001;
		} else {

			int reward = s.getArmiesReward();
			return ((reward * Values.rewardMultiplier) + Values.staticRegionBonus);

		}
	}

	public static Outcome calculateAttackOutcome(int attacking, int defending) {
		if (defending < 1) {
			return new Outcome(attacking, 0);
		}
		if (attacking == 1) {
			return new Outcome(0, Math.max(1, defending - 1));
		}
		int defendingCopy = defending;
		defending = (int) Math.round(Math.max(defending - ((0.6 * attacking)), 0));
		attacking = (int) Math.round(Math.max(attacking - ((0.82 * defendingCopy)), 0));

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

	@SuppressWarnings("null")
	public static double calculateRegionInitialCost(Region r) {
		if (r.getPlayerName().equals(BotState.getMyName())) {
			return 0;
		}
		if (r.getVisible()) {
			if (r.getPlayerName().equals(BotState.getMyOpponentName())) {
				return r.getArmies() * costMultiplierEnemy;
			} else if (r.getPlayerName().equals("neutral")) {
				return r.getArmies() * costMultiplierNeutral;
			}
		} else {
			if (r.getPlayerName().equals(BotState.getMyOpponentName())) {
				return staticCostUnknownEnemy;
			} else if (r.getWasteland()) {
				return costMultiplierNeutral * 6;
			} else if (r.getPlayerName().equals("neutral")) {
				return staticCostUnknownNeutral;
			} else if (r.getPlayerName().equals("unknown")) {
				return staticCostUnknown;
			}
		}

		// this shouldn't happen
		return (Double) null;

	}

	public static double calculateSuperRegionWeighedCost(SuperRegion sr, Map map) {
		double totalCost = 1;
		for (Region r : sr.getSubRegions()) {
			totalCost += calculateRegionInSuperRegionsWeighedCost(r);
		}

		// advanced modifiers
		Tables tables = Tables.getInstance();
		totalCost *= tables.getInternalHopsPenaltyFor(sr);
		totalCost *= tables.getSizePenaltyFor(sr);
		totalCost *= calculateSuperRegionVulnerability(sr, map);
		totalCost *= calculateSuperRegionTurnsNeededToTake(sr, map);
		// totalCost *= calculateFrontsOpened(sr,map);

		return totalCost;
	}

	// private static double calculateFrontsOpened(SuperRegion sr, Map map) {
	// Set<Region> alreadyHasContact = new HashSet<Region>();
	// Set<Region> contactWithSuperRegion = new HashSet<Region>();
	//
	// for (Region r : map.getOwnedRegions(BotState.getMyName())){
	// alreadyHasContact.addAll(r.getUnOwnedNeighbors());
	// }
	// for (Region r : sr.getSubRegions()){
	// contactWithSuperRegion.addAll(r.getUnOwnedNeighbors());
	// }
	//
	// contactWithSuperRegion.removeAll(alreadyHasContact);
	// Tables tables = Tables.getInstance();
	//
	//
	// return
	// tables.getMultipleFrontExponentialPenaltyFor(contactWithSuperRegion.size());
	//
	// }

	private static double calculateSuperRegionTurnsNeededToTake(SuperRegion sr, Map map) {

		if (latestTurnsNeededCalculated.get(sr.getId()) != null && latestTurnsNeededCalculated.get(sr.getId()) == BotState.getRoundNumber()) {
			return latestTurnsNeededResult.get(sr.getId());
		}
		Pathfinder p = Pathfinder.getSimplePathfinder(map);
		int max = 0;
		if (map.getOwnedRegions(BotState.getMyName()).size() > 1) {
			for (Region r : sr.getSubRegions()) {
				max = Math.max(max, (int) p.getPathToRegionOwnedByPlayer(r, BotState.getMyName()).getDistance());

			}
		} else {
			return 1;
		}
		// TODO Auto-generated method stub

		Tables tables = Tables.getInstance();
		double valueForMax = tables.getTurnsNeededToTakeExponentialPenaltyFor(max);
		latestTurnsNeededCalculated.put(sr.getId(), BotState.getRoundNumber());
		latestTurnsNeededResult.put(sr.getId(), valueForMax);
		return valueForMax;
	}

	private static double calculateSuperRegionVulnerability(SuperRegion sr, Map map) {
		// determine if this superregion borders an enemy region and if so how
		// many

		Set<Region> enemyPositions = map.getEnemyRegions();
		Set<Region> enemyNeighbors = new HashSet<Region>();
		for (Region r : enemyPositions) {
			for (Region n : r.getNeighbors()) {
				if (n.getSuperRegion().getId() == (sr.getId())) {
					enemyNeighbors.add(r);
					break;
				}
			}
		}
		int nbr = enemyNeighbors.size();
		if (nbr < 1) {
			Pathfinder pathfinder = Pathfinder.getSimplePathfinder(map);
			Path path = pathfinder.getPathToSuperRegionFromRegionOwnedByPlayer(sr, BotState.getMyOpponentName());
			if (path == null) {
				nbr = 1;
			} else {
				nbr = -(path.getPath().size() - 1) / 2;
			}
		}
		Tables table = Tables.getInstance();
		return table.getEnemyVicinityExponentialPenaltyFor(nbr);

	}

	public static int calculateRequiredForcesAttack(Region r) {

		// minimum number of forces required for an attack, should probably be
		// able to win

		int armySize = r.getArmies();
		if (r.getPlayerName().equals("unknown")) {
			return unknownRegionAppreciatedRequiredForcesAttack;
		} else if (r.getPlayerName().equals(BotState.getMyName())) {
			return 0;
		} else if (armySize <= 2) {
			return armySize + 1;
		} else if (armySize <= 3) {
			return armySize + 2;
		} else if (armySize <= 4) {
			return armySize + 3;
		} else if (armySize <= 6) {
			return armySize + 4;
		} else if (armySize <= 7) {
			return armySize + 5;
		} else {
			return (int) (armySize * 1.7);
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
			return calculateRequiredForcesAttack(r);
		} else if (armySize <= 3) {
			return armySize + 4;
		} else if (armySize <= 5) {
			return armySize + 6;
		} else {
			return (int) (armySize * 2.4);
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

	public static int calculateRequiredForcesDefendAgainstRegions(ArrayList<Region> regions) {
		int total = 0;
		if (defenseMode == DefenseMode.MAX_ONLY) {
			for (Region r : regions) {
				total = Math.max(r.getArmies() - 1, total);
			}
		} else if (defenseMode == DefenseMode.ALL) {
			for (Region r : regions) {
				total += r.getArmies() - 1;
			}
		} else if (defenseMode == DefenseMode.Percentage_OF_MAX_ONLY) {
			for (Region r : regions) {
				total = Math.max(r.getArmies() - 1, total);
			}
			total *= percentageOfAttackingNeededForDefence;
		}

		return total;
	}

	public static int calculateRequiredForcesDefend(Region region) {
		ArrayList<Region> enemyNeighbors = region.getEnemyNeighbors();
		return calculateRequiredForcesDefendAgainstRegions(enemyNeighbors);

	}

	public static HashMap<Integer, Integer> calculateRegionSatisfaction(Map map) {
		HashMap<Integer, Integer> roomLeft = new HashMap<Integer, Integer>();
		for (Region r : map.getRegionList()) {
			if (!r.getPlayerName().equals(BotState.getMyName()))
				roomLeft.put(r.getId(), calculateRequiredForcesAttackTotalVictory(r));
			else {
				roomLeft.put(r.getId(), Values.calculateRequiredForcesDefend(r));
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

	public static int calculateRequiredForcesForRegions(List<Region> path) {
		int total = 0;
		int left = 0;
		int currentRequired = 0;
		for (Region r : path) {
			if (!r.getPlayerName().equals(BotState.getMyName())) {
				currentRequired = calculateRequiredForcesAttack(r) - left;
				left = calculateAttackOutcome(currentRequired, r.getArmies()).getAttackingArmies();
				left--;
				total += currentRequired;
			}
		}
		return total;

	}

	public static int calculateRequiredForcesForSuperRegion(SuperRegion s) {
		return (calculateRequiredForcesForRegions(s.getSubRegions()));
	}

	public static double calculatePathCost(ArrayList<Region> regions, int totalRequired, int available) {
		double currentPathCost = 0;
		for (Region r : regions) {
			currentPathCost += calculateRegionWeighedCost(r);
		}
		int needPlaced = Math.max(0, totalRequired - available);
		currentPathCost += needPlaced * Values.needsPlacementPenalty;
		return currentPathCost;
	}

}
