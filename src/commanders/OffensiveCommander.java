package commanders;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import concepts.ActionProposal;
import concepts.Plan;
import bot.BotState;
import bot.Values;
import map.*;
import map.Pathfinder.Path;

public class OffensiveCommander {

	public static Region determineStartPosition(ArrayList<Region> possiblePicks, Map map) {
		Region maxRegion = null;
		double maxWeight = Double.MIN_VALUE;
		for (Region r : possiblePicks) {
			String beforeStatus = map.getRegion(r.getId()).getPlayerName();
			map.getRegion(r.getId()).setPlayerName(BotState.getMyName());
			double weight = calculateStartRegionWorth(r, map);
			if (weight > maxWeight) {
				maxWeight = weight;
				maxRegion = r;
			}
			map.getRegion(r.getId()).setPlayerName(beforeStatus);
		}

		return maxRegion;

	}

	private static double calculateStartRegionWorth(Region region, Map map) {
		Pathfinder pathfinder = new Pathfinder(map, new PathfinderWeighter() {
			public double weight(Region nodeA, Region nodeB) {
				return Values.calculateRegionWeighedCost(nodeB);

			}
		});

		double maxWeight = Double.MIN_VALUE;
		HashMap<SuperRegion, Double> superRegionWorths = calculateSuperRegionWorth(map);
		HashMap<Region, Double> regionWorths = calculateRegionWorth(map);
		ArrayList<Path> paths = pathfinder.getPathToRegionsFromRegion(region, map.getUnOwnedRegions());

		for (Path path : paths) {
			double weight = calculatePathWeight(path, superRegionWorths, regionWorths, map, 0, 0);
			if (weight > maxWeight) {
				maxWeight = weight;
			}
		}

		return maxWeight;

	}

	private static HashMap<Region, Double> calculateRegionWorth(Map map) {
		HashMap<Region, Double> worths = new HashMap<Region, Double>();

		for (Region r : map.getRegionList()) {
			worths.put(r, Values.calculateRegionOffensiveWorth(r));
		}

		return worths;
	}

	private static double calculatePathWeight(Path path, HashMap<SuperRegion, Double> superRegionWorths, HashMap<Region, Double> regionWorths, Map map,
			int totalRequired, int available) {
		double worth = superRegionWorths.get(path.getTarget().getSuperRegion()) + regionWorths.get(path.getTarget());
		ArrayList<Region> regionsAttacked = new ArrayList<Region>(path.getPath());
		regionsAttacked.remove(0);
		double currentCost = Values.calculatePathCost(regionsAttacked, totalRequired, available);
		// for (int i = 1; i< path.getPath().size(); i++){
		// currentCost +=
		// Values.calculateRegionWeighedCost(path.getPath().get(i));
		// }
		for (Region r : regionsAttacked) {
			if (r.getSuperRegion().equals(path.getTarget().getSuperRegion())) {
				currentCost -= Values.calculateRegionInitialCost(r);
			}
		}
		currentCost += Values.calculateSuperRegionWeighedCost(path.getTarget().getSuperRegion(), map);
		// int needPlaced = Math.max(0, totalRequired -
		// path.getOrigin().getArmies());
		// currentCost += needPlaced * Values.needsPlacementPenalty;

		double weight = worth / currentCost;

		return weight;
	}

	private static HashMap<SuperRegion, Double> calculateSuperRegionWorth(Map map) {
		HashMap<SuperRegion, Double> worth = new HashMap<SuperRegion, Double>();
		ArrayList<SuperRegion> possibleTargets = map.getSuperRegions();

		for (SuperRegion s : possibleTargets) {
			if (s.ownedByPlayer(BotState.getMyName())) {
				worth.put(s, 0d);
			} else {
				worth.put(s, Values.calculateSuperRegionWorth(s));
			}
		}
		return worth;
	}

	public ArrayList<ActionProposal> getActionProposals(Map map, Set<Integer> availableRegions, Pathfinder pathfinder, HashMap<Integer, Integer> availableForces) {
		ArrayList<ActionProposal> proposals = new ArrayList<ActionProposal>();
		HashMap<SuperRegion, Double> superRegionWorths = calculateSuperRegionWorth(map);
		HashMap<Region, Double> regionWorths = calculateRegionWorth(map);
		ArrayList<Path> paths;

		// calculate plans for every sector

		for (Integer r : availableRegions) {
			paths = pathfinder.getPathToAllRegionsNotOwnedByPlayerFromRegion(map.getRegion(r), BotState.getMyName());
			for (Path path : paths) {
				ArrayList<Region> regionsAttacked = new ArrayList<Region>(path.getPath());
				regionsAttacked.remove(0);
				int totalRequired = Values.calculateRequiredForcesForRegions(regionsAttacked);
				double weight = calculatePathWeight(path, superRegionWorths, regionWorths, map, totalRequired, availableForces.get(r));
				// weight /=
				proposals.add(new ActionProposal(weight, map.getRegion(r), path.getPath().get(1), totalRequired, new Plan(path.getTarget(), path.getTarget()
						.getSuperRegion()), "OffensiveCommander"));

			}

		}

		return proposals;
	}
}
