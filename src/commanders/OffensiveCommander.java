package commanders;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;

import concepts.ActionProposal;
import concepts.PlacementProposal;
import bot.BotState;
import bot.Values;
import map.*;
import map.Pathfinder2.Path;

public class OffensiveCommander extends TemplateCommander {
	private static final int rewardMultiplier = 40;
	private static final int staticRegionBonus = 30;
	private static final int offencePenaltyOfMovingThroughOwnRegion = 8;

	@Override
	public ArrayList<PlacementProposal> getPlacementProposals(BotState state) {
		Map currentMap = state.getFullMap();
		HashMap<Integer, Float> worth = new HashMap<Integer, Float>();

		// if we don't have any super regions, prioritize expansion greatly
		if (currentMap.getOwnedSuperRegions(state.getMyPlayerName()).size() < 1) {
			selfImportance = 1000;
		} else {
			selfImportance = 1;
		}

		worth = calculatePlans(state);
		ArrayList<PlacementProposal> attackPlans;
		attackPlans = prepareAttacks(worth, state);

		return attackPlans;
	}

	private ArrayList<PlacementProposal> prepareAttacks(
			HashMap<Integer, Float> worth, BotState state) {

		ArrayList<PlacementProposal> proposals = new ArrayList<PlacementProposal>();
		final String oName = state.getOpponentPlayerName();
		String mName = state.getMyPlayerName();
		Map map = state.getFullMap();

		Pathfinder2 pathfinder = new Pathfinder2(state.getFullMap(),
				new PathfinderWeighter() {
					public int weight(Region nodeA, Region nodeB) {
						return Values.calculateRegionWeighedCost(oName, nodeB);

					}
				});

		Set<Integer> keys = worth.keySet();
		for (Integer s : keys) {
			Path path = pathfinder.getPathToSuperRegionFromRegionOwnedByPlayer(
					map.getSuperRegion(s), state.getMyPlayerName());

			int required = Values.calculateRequiredForcesAttack(mName,
					map.getSuperRegion(s));
			if (path == null) {
				// Super region is already controlled
				continue;
			}
			float cost = path.getDistance()
					- Values.calculateRegionWeighedCost(oName, path.getTarget())
					+ Values.calculateSuperRegionWeighedCost(oName,
							map.getSuperRegion(s));

			float value = worth.get(s) / cost;
			proposals.add(new PlacementProposal(value, path.getOrigin(), map
					.getSuperRegion(s), required, "OffensiveCommander"));

		}

		return proposals;
	}

	private HashMap<Integer, Float> calculatePlans(BotState state) {
		HashMap<Integer, Float> worth = new HashMap<Integer, Float>();
		ArrayList<SuperRegion> possibleTargets = state.getFullMap()
				.getSuperRegions();

		// exclude owned superregions
		// possibleTargets.removeAll((state.getFullMap()
		// .getOwnedSuperRegions(state.getMyPlayerName())));

		for (SuperRegion s : possibleTargets) {
			worth.put(s.getId(), calculateWorth(s, state) + selfImportance);
		}
		return worth;
	}

	private float calculateWorth(SuperRegion s, BotState state) {
		if (s.getArmiesReward() < 1) {
			return -1;
		} else {
			int reward = s.getArmiesReward();
			return ((reward * rewardMultiplier) + staticRegionBonus);

		}
	}

	@Override
	public ArrayList<ActionProposal> getActionProposals(BotState state) {
		ArrayList<ActionProposal> proposals = new ArrayList<ActionProposal>();
		HashMap<Integer, Float> ranking = calculatePlans(state);

		ArrayList<Region> available = state.getFullMap().getOwnedRegions(
				state.getMyPlayerName());
		final String mName = state.getMyPlayerName();
		final String eName = state.getOpponentPlayerName();
		Pathfinder2 pathfinder = new Pathfinder2(state.getFullMap(),
				new PathfinderWeighter() {
					public int weight(Region nodeA, Region nodeB) {
						if (nodeB.getPlayerName().equals(mName)) {
							return offencePenaltyOfMovingThroughOwnRegion;
						} else {
							return Values.calculateRegionWeighedCost(eName,
									nodeB);
						}

					}
				});

		float currentWeight;
		ArrayList<Path> paths;

		// calculate plans for every sector

		for (Region r : available) {
			if (r.getArmies() < 2) {
				continue;
			}
			paths = pathfinder.getPathToAllRegionsNotOwnedByPlayerFromRegion(r,
					mName);
			for (Path path : paths) {
				if (ranking.get(path.getTarget().getSuperRegion().getId()) == null) {
					// no interest in this path
					continue;
				}
				SuperRegion targetSuperRegion = path.getTarget()
						.getSuperRegion();
				float currentPathCost = path.getDistance()
						- Values.calculateRegionWeighedCost(eName,
								path.getTarget());
				float currentSuperRegionCost = Values
						.calculateSuperRegionWeighedCost(eName,
								targetSuperRegion);
				float currentWorth = ranking.get(path.getTarget()
						.getSuperRegion().getId());
				currentWeight = currentWorth
						/ (currentSuperRegionCost + currentPathCost);
				int totalRequired = 0;
				for (int i = 1; i < path.getPath().size() - 1; i++) {
					totalRequired += Values.calculateRequiredForcesAttack(
							mName, path.getPath().get(i));
				}
				proposals.add(new ActionProposal(currentWeight, r, path
						.getPath().get(1), totalRequired, targetSuperRegion,
						"OffensiveCommander"));

			}

		}
		return proposals;
	}
}
