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
			ArrayList<Path> paths = pathfinder.getPathToRegionsFromRegion(r, unOwned, BotState.getMyName());
			for (Path path : paths) {
				double worth = worths.get(path.getTarget().getSuperRegion());
				double cost = path.getDistance() - Values.calculateRegionWeighedCost(path.getTarget())
						+ Values.calculateSuperRegionWeighedCost(path.getTarget().getSuperRegion());
				double weight = worth / cost;
				int required = Values.calculateRequiredForcesAttack(path.getPath().get(1));

				if (required < 1) {
					continue;
				}

				proposals.add(new PlacementProposal(weight, path.getOrigin(), new Plan(path.getTarget(), path.getTarget().getSuperRegion()), required,
						"OffensiveCommander"));
			}
		}

		return proposals;
	}

	private HashMap<SuperRegion, Double> calculateWorth(Map map) {
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
				SuperRegion targetSuperRegion = path.getTarget().getSuperRegion();
				double currentPathCost = path.getDistance() - Values.calculateRegionWeighedCost(path.getTarget());
				double currentSuperRegionCost = Values.calculateSuperRegionWeighedCost(targetSuperRegion);
				double currentWorth = ranking.get(path.getTarget().getSuperRegion());
				currentWeight = currentWorth / (currentSuperRegionCost + currentPathCost);
				int totalRequired = 0;
				totalRequired = Values.calculateRequiredForcesAttackTotalVictory(path.getPath().get(1));

				int disposed = Math.min(totalRequired, r.getArmies() - 1);

				proposals.add(new ActionProposal(currentWeight, r, path.getPath().get(1), disposed, new Plan(path.getTarget(), targetSuperRegion),
						"OffensiveCommander"));

			}

		}

		return proposals;
	}
}
