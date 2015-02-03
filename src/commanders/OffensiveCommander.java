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
	public ArrayList<PlacementProposal> getPlacementProposals(BotState state) {
		HashMap<Integer, Double> worth = new HashMap<Integer, Double>();

		worth = calculatePlans(state);
		ArrayList<PlacementProposal> attackPlans;
		attackPlans = prepareAttacks(worth, state);

		return attackPlans;
	}

	private ArrayList<PlacementProposal> prepareAttacks(HashMap<Integer, Double> worth, BotState state) {

		ArrayList<PlacementProposal> proposals = new ArrayList<PlacementProposal>();
		Map map = state.getFullMap();

		Pathfinder pathfinder = new Pathfinder(state.getFullMap(), new PathfinderWeighter() {
			public double weight(Region nodeA, Region nodeB) {
				return Values.calculateRegionWeighedCost(nodeB);

			}
		});

		Set<Integer> keys = worth.keySet();
		for (Integer s : keys) {
			Path path = pathfinder.getPathToSuperRegionFromRegionOwnedByPlayer(map.getSuperRegion(s), state.getMyPlayerName());

			if (path == null) {
				// Super region is already controlled
				continue;
			}
			int required = Values.calculateRequiredForcesAttack(path.getTarget().getSuperRegion()) - path.getOrigin().getArmies() + 1;
			if (required < 1) {
				continue;
			}
			required += Values.calculateRequiredForcesDefend(path.getTarget());

			double cost = path.getDistance() - Values.calculateRegionWeighedCost(path.getTarget())
					+ Values.calculateSuperRegionWeighedCost(map.getSuperRegion(s));

			double value = worth.get(s) / cost;
			proposals.add(new PlacementProposal(value, path.getOrigin(), new Plan(path.getOrigin(), path.getOrigin().getSuperRegion()), required,
					"OffensiveCommander"));

		}

		return proposals;
	}

	private HashMap<Integer, Double> calculatePlans(BotState state) {
		HashMap<Integer, Double> worth = new HashMap<Integer, Double>();
		ArrayList<SuperRegion> possibleTargets = state.getFullMap().getSuperRegions();

		for (SuperRegion s : possibleTargets) {
			worth.put(s.getId(), Values.calculateSuperRegionWorth(s));
		}
		return worth;
	}

	@Override
	public ArrayList<ActionProposal> getActionProposals(BotState state) {
		ArrayList<ActionProposal> proposals = new ArrayList<ActionProposal>();
		HashMap<Integer, Double> ranking = calculatePlans(state);

		ArrayList<Region> available = state.getFullMap().getOwnedRegions(state.getMyPlayerName());

		final String mName = state.getMyPlayerName();
		final String eName = state.getOpponentPlayerName();
		Pathfinder pathfinder = new Pathfinder(state.getFullMap(), new PathfinderWeighter() {
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
			paths = pathfinder.getPathToAllRegionsNotOwnedByPlayerFromRegion(r, mName);
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
				totalRequired += Values.calculateRequiredForcesDefend(path.getTarget());

				int disposed = Math.min(totalRequired, r.getArmies() - 1);

				proposals.add(new ActionProposal(currentWeight, r, path.getPath().get(1), disposed, new Plan(path.getTarget(), targetSuperRegion),
						"OffensiveCommander"));

			}

		}

		return proposals;
	}
}
