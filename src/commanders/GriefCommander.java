package commanders;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Set;

import map.Map;
import map.Pathfinder;
import map.PathfinderWeighter;
import map.Region;
import map.SuperRegion;
import map.Pathfinder.Path;
import bot.BotState;
import bot.Values;
import concepts.ActionProposal;
import concepts.PlacementProposal;
import concepts.Plan;

public class GriefCommander extends TemplateCommander {

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

		ArrayList<SuperRegion> interestingSuperRegions = map.getSuspectedOwnedSuperRegions(BotState.getMyOpponentName());
		ArrayList<Region> interestingRegions = new ArrayList<Region>();
		for (SuperRegion s : interestingSuperRegions) {
			interestingRegions.addAll(s.getSubRegions());
		}

		for (Region r : map.getOwnedRegions(BotState.getMyName())) {
			ArrayList<Path> paths = pathfinder.getPathToRegionsFromRegion(r, interestingRegions);
			for (Path path : paths) {
				double worth = worths.get(path.getTarget().getSuperRegion());
				double cost = path.getDistance();
				double weight = worth / cost;
				int totalRequired = 0;
				for (int i = 1; i < path.getPath().size(); i++) {
					totalRequired += Values.calculateRequiredForcesAttackTotalVictory(path.getPath().get(i));
				}

				if (totalRequired < 1) {
					continue;
				}

				proposals.add(new PlacementProposal(weight, path.getOrigin(), new Plan(path.getTarget(), path.getTarget().getSuperRegion()), totalRequired,
						"GriefCommander"));
			}
		}

		return proposals;
	}

	private HashMap<SuperRegion, Double> calculateWorth(Map map) {
		HashMap<SuperRegion, Double> worths = new HashMap<SuperRegion, Double>();

		for (SuperRegion s : map.getSuperRegions()) {
			if (s.getSuspectedOwnedSuperRegion(BotState.getMyOpponentName())) {
				double reward = s.getArmiesReward();
				worths.put(s, reward * Values.valueDenialMultiplier);
			} else {
				worths.put(s, -1d);
			}

		}
		return worths;
	}

	@Override
	public ArrayList<ActionProposal> getActionProposals(Map map) {
		ArrayList<ActionProposal> proposals = new ArrayList<ActionProposal>();

		proposals = new ArrayList<ActionProposal>();
		HashMap<SuperRegion, Double> ranking = calculateWorth(map);

		ArrayList<Region> available = map.getOwnedRegions(BotState.getMyName());
		Pathfinder pathfinder = new Pathfinder(map, new PathfinderWeighter() {
			public double weight(Region nodeA, Region nodeB) {
				if (nodeB.getPlayerName().equals(BotState.getMyName())) {
					return 5;
				} else {
					return Values.calculateRegionWeighedCost(nodeB);
				}

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
				double currentPathCost = path.getDistance();
				double currentWorth = ranking.get(path.getTarget().getSuperRegion());
				currentWeight = currentWorth / currentPathCost;
				int totalRequired = 0;
				for (int i = 1; i < path.getPath().size(); i++) {
					totalRequired += Values.calculateRequiredForcesAttackTotalVictory(path.getPath().get(i));
				}
				int disposed = Math.min(totalRequired, r.getArmies() - 1);

				proposals.add(new ActionProposal(currentWeight, r, path.getPath().get(1), disposed, new Plan(path.getTarget(), targetSuperRegion),
						"GriefCommander"));

			}

		}
		return proposals;
	}

}
