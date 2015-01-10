package commanders;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Set;

import java.util.HashSet;
import java.util.List;

import concepts.ActionProposal;
import concepts.PlacementProposal;
import concepts.Plan;
import bot.BotState;
import bot.Values;
import map.*;
import map.Pathfinder2.Path;
import move.AttackTransferMove;

public class OffensiveCommander extends TemplateCommander {
	private static final int rewardMultiplier = 40;
	private static final int staticRegionBonus = 10;
	private static final int offencePenaltyOfMovingThroughOwnRegion = 5;
	private HashMap<Integer, Float> worth;;

	@Override
	public ArrayList<PlacementProposal> getPlacementProposals(BotState state) {
		Map currentMap = state.getVisibleMap();
		worth = new HashMap<Integer, Float>();

		// if we don't have any super regions, prioritize expansion greatly
		if (currentMap.getOwnedSuperRegions(state.getMyPlayerName()).size() < 1) {
			selfImportance = 1000;
		} else {
			selfImportance = 1;
		}

		worth = calculatePlans(state);
		ArrayList<PlacementProposal> attackPlans;
		PlacementProposal tempProposal;
		Set<Integer> keys = worth.keySet();
		attackPlans = prepareAttacks(worth, state);

		return attackPlans;
	}

	private ArrayList<PlacementProposal> prepareAttacks(
			HashMap<Integer, Float> worth, BotState state) {

		ArrayList<PlacementProposal> proposals = new ArrayList<PlacementProposal>();
		final String oName = state.getOpponentPlayerName();
		String mName = state.getMyPlayerName();
		Map map = state.getVisibleMap();

		Pathfinder2 pathfinder = new Pathfinder2(state.getVisibleMap(),
				new PathfinderWeighter() {
					public int weight(Region nodeA, Region nodeB) {
						return Values.calculateRegionWeighedCost(oName, nodeB);

					}
				});

		Set<Integer> keys = worth.keySet();
		for (Integer s : keys) {
			Path path = pathfinder.getPathToSuperRegionFromRegionOwnedByPlayer(
					map.getSuperRegion(s), state.getMyPlayerName());

			float cost = path.getDistance()
					- Values.calculateRegionWeighedCost(oName, path.getTarget())
					+ Values.calculateSuperRegionWeighedCost(oName,
							map.getSuperRegion(s));
			int required = Values.calculateRequiredForcesAttack(mName,
					map.getSuperRegion(s));

			float value = worth.get(s) - cost;
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
		possibleTargets.removeAll((state.getVisibleMap()
				.getOwnedSuperRegions(state.getMyPlayerName())));

		for (SuperRegion s : possibleTargets) {
			worth.put(s.getId(), calculateWorth(s, state) + selfImportance);
		}
		return worth;
	}

	private float calculateWorth(SuperRegion s, BotState state) {
		if (s.getArmiesReward() < 1) {
			return -100;
		} else {
			int reward = s.getArmiesReward();
			return ((reward * rewardMultiplier) + staticRegionBonus);

		}
	}

	@Override
	public ArrayList<ActionProposal> getActionProposals(BotState state) {
		ArrayList<ActionProposal> proposals = new ArrayList<ActionProposal>();
		HashMap<Integer, Float> ranking = calculatePlans(state);

		Set<Integer> keys = ranking.keySet();
		ArrayList<Region> owned = state.getVisibleMap().getOwnedRegions(
				state.getMyPlayerName());
		ArrayList<Region> available = (ArrayList<Region>) owned.clone();
		final String mName = state.getMyPlayerName();
		final String eName = state.getOpponentPlayerName();
		Pathfinder2 pathfinder = new Pathfinder2(state.getVisibleMap(),
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

		float maxWeight;
		float currentCost;
		float currentWeight;
		float costOfPath;
		Path bestPath;
		Path currentPath = null;
		SuperRegion bestPlan = null;

		// calculate plans for every sector
		for (Region r : available) {
			if (r.getArmies() < 2) {
				break;
			}

			bestPath = null;
			maxWeight = Integer.MIN_VALUE;
			ArrayList<Path> paths;
			paths = pathfinder.getPathToAllRegionsNotOwnedByPlayerFromRegion(r,
					mName);

			for (Path path : paths) {
				if (ranking.get(path.getTarget().getSuperRegion().getId()) == null) {
					break;
				}
				float currentPathCost = path.getDistance()
						- Values.calculateRegionWeighedCost(eName,
								path.getTarget());
				float currentSuperRegionCost = Values
						.calculateSuperRegionWeighedCost(eName, path
								.getTarget().getSuperRegion());
				float currentWorth = ranking.get(path.getTarget()
						.getSuperRegion().getId());
				currentWeight = currentWorth - currentSuperRegionCost
						- currentPathCost;

				if (currentWeight > maxWeight) {
					maxWeight = currentWeight;
					bestPath = path;
					bestPlan = path.getTarget().getSuperRegion();
				}

			}
			if (bestPath != null) {
				int calculatedTotalCost = Values.calculateRequiredForcesAttack(
						mName, bestPath.getTarget().getSuperRegion())
						+ bestPath.getDistance()
						- Values.calculateRegionWeighedCost(eName,
								bestPath.getTarget());

				int deployed = Math.min(calculatedTotalCost, r.getArmies() - 1);
				proposals.add(new ActionProposal(maxWeight, r, bestPath
						.getPath().get(1), deployed, bestPlan,
						"OffensiveCommander"));
			}
		}

		return proposals;
	}
}
