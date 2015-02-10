package commanders;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;

import concepts.ActionProposal;
import concepts.PlacementProposal;
import concepts.Plan;
import bot.BotState;
import bot.Values;
import map.*;
import map.Pathfinder.Path;

public class OffensiveCommander extends TemplateCommander {

	@Override
	public ArrayList<PlacementProposal> getPlacementProposals(Map map) {

		ArrayList<PlacementProposal> attackPlans;
		attackPlans = prepareAttacks(map);

		return attackPlans;
	}

	public static Region determineStartPosition(ArrayList<Region> possiblePicks, Map map) {

		Map modifiedMap = map.duplicate();

		Region maxRegion = null;
		double maxWeight = Double.MIN_VALUE;
		ArrayList<Region> unOwned = map.getUnOwnedRegions();

		for (Region r : possiblePicks) {
			String beforeStatus = modifiedMap.getRegion(r.getId()).getPlayerName();
			modifiedMap.getRegion(r.getId()).setPlayerName(BotState.getMyName());
			double weight = calculateStartRegionWorth(r, map);
			if (weight > maxWeight) {
				maxWeight = weight;
				maxRegion = r;
			}
			modifiedMap.getRegion(r.getId()).setPlayerName(beforeStatus);
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
		HashMap<SuperRegion, Double> worths = calculateWorth(map);
		ArrayList<Path> paths = pathfinder.getPathToRegionsFromRegion(region, map.getUnOwnedRegions());

		for (Path path : paths) {
			double weight = calculatePathWeight(path, worths, map);
			if (weight > maxWeight) {
				maxWeight = weight;
			}
		}

		return maxWeight;

	}

	private ArrayList<PlacementProposal> prepareAttacks(Map map) {
		HashMap<SuperRegion, Double> worths = calculateWorth(map);
		ArrayList<PlacementProposal> proposals = new ArrayList<PlacementProposal>();

		Pathfinder pathfinder = new Pathfinder(map, new PathfinderWeighter() {
			public double weight(Region nodeA, Region nodeB) {
				return Values.calculateRegionWeighedCost(nodeB);

			}
		});
		ArrayList<Region> unOwned = map.getUnOwnedRegions();

		for (Region r : map.getOwnedRegions(BotState.getMyName())) {
			ArrayList<Path> paths = pathfinder.getPathToRegionsFromRegion(r, unOwned);
			for (Path path : paths) {
				double weight = calculatePathWeight(path, worths, map);
				int totalRequired = 0;
				for (int i = 1; i < path.getPath().size(); i++) {
					totalRequired += Values.calculateRequiredForcesAttackTotalVictory(path.getPath().get(i));
				}

				if (totalRequired < 1) {
					continue;
				}

				proposals.add(new PlacementProposal(weight, path.getOrigin(), new Plan(path.getTarget(), path.getTarget().getSuperRegion()), totalRequired,
						"OffensiveCommander"));
			}
		}

		return proposals;
	}

	private static double calculatePathWeight(Path path, HashMap<SuperRegion, Double> worths, Map map) {
		double worth = worths.get(path.getTarget().getSuperRegion());
		double cost = path.getDistance() - Values.calculateRegionWeighedCost(path.getTarget())
				+ Values.calculateSuperRegionWeighedCost(path.getTarget().getSuperRegion(), map);
		double weight = worth / cost;

		return weight;
	}

	private static HashMap<SuperRegion, Double> calculateWorth(Map map) {
		HashMap<SuperRegion, Double> worth = new HashMap<SuperRegion, Double>();
		ArrayList<SuperRegion> possibleTargets = map.getSuperRegions();

		for (SuperRegion s : possibleTargets) {
			if (s.ownedByPlayer(BotState.getMyName())) {
				worth.put(s, -1d);
			} else {
				worth.put(s, Values.calculateSuperRegionWorth(s));
			}
		}
		return worth;
	}

	@Override
	public ArrayList<ActionProposal> getActionProposals(Map map) {
		ArrayList<ActionProposal> proposals = new ArrayList<ActionProposal>();
		HashMap<SuperRegion, Double> ranking = calculateWorth(map);

		ArrayList<Region> available = map.getOwnedRegions(BotState.getMyName());

		Pathfinder pathfinder = new Pathfinder(map, new PathfinderWeighter() {
			public double weight(Region nodeA, Region nodeB) {

				return Values.calculateRegionWeighedCost(nodeB);

			}
		});

		double currentWeight;
		ArrayList<Path> paths;

		// calculate plans for every sector

		for (Region r : available) {
			if (r.getArmies() < 2) {
				continue;
			}
			paths = pathfinder.getPathToAllRegionsNotOwnedByPlayerFromRegion(r, BotState.getMyName());
			for (Path path : paths) {
				double weight = calculatePathWeight(path, ranking, map);

				int totalRequired = 0;
				for (int i = 1; i < path.getPath().size(); i++) {
					totalRequired += Values.calculateRequiredForcesAttackTotalVictory(path.getPath().get(i));
				}

				int disposed = Math.min(totalRequired, r.getArmies() - 1);

				proposals.add(new ActionProposal(weight, r, path.getPath().get(1), disposed, new Plan(path.getTarget(), path.getTarget().getSuperRegion()),
						"OffensiveCommander"));

			}

		}

		return proposals;
	}
}
