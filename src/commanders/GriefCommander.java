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
	public ArrayList<PlacementProposal> getPlacementProposals(BotState state) {

		ArrayList<PlacementProposal> proposals = new ArrayList<PlacementProposal>();

		// don't start griefing too early
		if (state.getRoundNumber() < 2) {
			return proposals;
		}

		HashMap<Integer, Double> worth = new HashMap<Integer, Double>();
		worth = calculatePlans(state);

		ArrayList<PlacementProposal> attackPlans;
		attackPlans = prepareAttacks(worth, state);

		return attackPlans;
	}

	private ArrayList<PlacementProposal> prepareAttacks(HashMap<Integer, Double> worth, BotState state) {
		Set<Integer> keys = worth.keySet();
		Map map = state.getFullMap();
		ArrayList<PlacementProposal> proposals = new ArrayList<PlacementProposal>();

		Pathfinder pathfinder = new Pathfinder(state.getFullMap(), new PathfinderWeighter() {
			public double weight(Region nodeA, Region nodeB) {
				return Values.calculateRegionWeighedCost(nodeB);

			}
		});

		for (Integer s : keys) {
			Path path = pathfinder.getPathToSuperRegionFromRegionOwnedByPlayer(map.getSuperRegion(s), state.getMyPlayerName());

			if (path == null) {
				// Super region is already controlled
				continue;
			}
			int required = -path.getOrigin().getArmies() + 1;
			for (int i = 1; i < path.getPath().size(); i++) {
				required += Values.calculateRequiredForcesAttack(path.getPath().get(i));
			}
			if (required < 1) {
				continue;
			}
			double cost = path.getDistance();

			double value = worth.get(s) / cost;
			proposals.add(new PlacementProposal(value, path.getOrigin(), new Plan(path.getTarget(), path.getTarget().getSuperRegion()), required,
					"GriefCommander"));

		}

		return proposals;
	}

	private HashMap<Integer, Double> calculatePlans(BotState state) {
		ArrayList<SuperRegion> enemySuperRegions = state.getFullMap().getSuspectedOwnedSuperRegions(state.getOpponentPlayerName());

		HashMap<Integer, Double> plans = new HashMap<Integer, Double>();

		for (SuperRegion s : enemySuperRegions) {
			// for every super region they have, calculate how to execute an
			// eventual attack and how much it would be worth

			double reward = s.getArmiesReward();

			plans.put(s.getId(), reward * Values.valueDenialMultiplier);

		}
		return plans;
	}

	@Override
	public ArrayList<ActionProposal> getActionProposals(BotState state) {
		ArrayList<ActionProposal> proposals = new ArrayList<ActionProposal>();

		// don't start griefing too early
		if (state.getRoundNumber() < 3) {
			return proposals;
		}
		proposals = new ArrayList<ActionProposal>();
		HashMap<Integer, Double> ranking = calculatePlans(state);

		ArrayList<Region> available = state.getFullMap().getOwnedRegions(state.getMyPlayerName());
		Pathfinder pathfinder = new Pathfinder(state.getFullMap(), new PathfinderWeighter() {
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
				if (ranking.get(path.getTarget().getSuperRegion().getId()) == null) {
					// no interest in this path
					continue;
				}
				SuperRegion targetSuperRegion = path.getTarget().getSuperRegion();
				double currentPathCost = path.getDistance() - Values.calculateRegionWeighedCost(path.getTarget());
				double currentSuperRegionCost = Values.calculateSuperRegionWeighedCost(targetSuperRegion);
				double currentWorth = ranking.get(path.getTarget().getSuperRegion().getId());
				currentWeight = currentWorth / (currentSuperRegionCost + currentPathCost);
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
