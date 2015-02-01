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
		HashMap<Integer, Float> worth = new HashMap<Integer, Float>();

		worth = calculatePlans(state);
		ArrayList<PlacementProposal> attackPlans;
		attackPlans = prepareAttacks(worth, state);

		return attackPlans;
	}

	private ArrayList<PlacementProposal> prepareAttacks(HashMap<Integer, Float> worth, BotState state) {

		ArrayList<PlacementProposal> proposals = new ArrayList<PlacementProposal>();
		final String oName = state.getOpponentPlayerName();
		final String mName = state.getMyPlayerName();
		Map map = state.getFullMap();

		Pathfinder pathfinder = new Pathfinder(state.getFullMap(), new PathfinderWeighter() {
			public float weight(Region nodeA, Region nodeB) {
				return Values.calculateRegionWeighedCost(mName, oName, nodeB);

			}
		});

		Set<Integer> keys = worth.keySet();
		for (Integer s : keys) {
			Path path = pathfinder.getPathToSuperRegionFromRegionOwnedByPlayer(map.getSuperRegion(s), state.getMyPlayerName());

			if (path == null) {
				// Super region is already controlled
				continue;
			}
			int required = Values.calculateRequiredForcesAttack(mName, path.getTarget().getSuperRegion()) - path.getOrigin().getArmies() + 1;
			if (required < 1) {
				continue;
			}

			float cost = path.getDistance() - Values.calculateRegionWeighedCost(mName, oName, path.getTarget())
					+ Values.calculateSuperRegionWeighedCost(map.getSuperRegion(s));

			float value = worth.get(s) / cost;
			proposals.add(new PlacementProposal(value, path.getOrigin(), new Plan(path.getOrigin(), path.getOrigin().getSuperRegion()), required,
					"OffensiveCommander"));

		}

		return proposals;
	}

	private HashMap<Integer, Float> calculatePlans(BotState state) {
		HashMap<Integer, Float> worth = new HashMap<Integer, Float>();
		ArrayList<SuperRegion> possibleTargets = state.getFullMap().getSuperRegions();

		for (SuperRegion s : possibleTargets) {
			worth.put(s.getId(), Values.calculateSuperRegionWorth(s));
		}
		return worth;
	}

	@Override
	public ArrayList<ActionProposal> getActionProposals(BotState state) {
		ArrayList<ActionProposal> proposals = new ArrayList<ActionProposal>();
		HashMap<Integer, Float> ranking = calculatePlans(state);

		ArrayList<Region> available = state.getFullMap().getOwnedRegions(state.getMyPlayerName());

		final String mName = state.getMyPlayerName();
		final String eName = state.getOpponentPlayerName();
		Pathfinder pathfinder = new Pathfinder(state.getFullMap(), new PathfinderWeighter() {
			public float weight(Region nodeA, Region nodeB) {

				return Values.calculateRegionWeighedCost(mName, eName, nodeB);

			}
		});

		float currentWeight;
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
				float currentPathCost = path.getDistance() - Values.calculateRegionWeighedCost(mName, eName, path.getTarget());
				float currentSuperRegionCost = Values.calculateSuperRegionWeighedCost(targetSuperRegion);
				float currentWorth = ranking.get(path.getTarget().getSuperRegion().getId());
				currentWeight = currentWorth / (currentSuperRegionCost + currentPathCost);
				int totalRequired = 0;
				for (int i = 1; i < path.getPath().size(); i++) {
					totalRequired += Values.calculateRequiredForcesAttackTotalVictory(mName, path.getPath().get(i));
				}

				int disposed = Math.min(totalRequired, r.getArmies() - 1);

				proposals.add(new ActionProposal(currentWeight, r, path.getPath().get(1), disposed, new Plan(path.getTarget(), targetSuperRegion),
						"OffensiveCommander"));

			}

		}

		return proposals;
	}
}
