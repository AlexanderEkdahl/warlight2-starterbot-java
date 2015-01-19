package commanders;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Set;

import map.Map;
import map.Pathfinder;
import map.Pathfinder2;
import map.PathfinderWeighter;
import map.Region;
import map.SuperRegion;
import map.Pathfinder2.Path;
import bot.BotState;
import bot.Values;
import concepts.ActionProposal;
import concepts.PlacementProposal;
import concepts.Plan;

public class GriefCommander extends TemplateCommander {
	public static final int valueDenialMultiplier = 10;

	@Override
	public ArrayList<PlacementProposal> getPlacementProposals(BotState state) {

		ArrayList<PlacementProposal> proposals = new ArrayList<PlacementProposal>();

		// don't start griefing too early
		if (state.getRoundNumber() < 3) {
			return proposals;
		}
		ArrayList<Region> enemyRegions = state.getFullMap().getOwnedRegions(
				state.getOpponentPlayerName());

		HashMap<Integer, Float> worth = new HashMap<Integer, Float>();
		worth = calculatePlans(state);

		ArrayList<PlacementProposal> attackPlans;
		attackPlans = prepareAttacks(worth, state);

		return attackPlans;
	}

	private ArrayList<PlacementProposal> prepareAttacks(
			HashMap<Integer, Float> worth, BotState state) {
		Set<Integer> keys = worth.keySet();
		final String oName = state.getOpponentPlayerName();
		final String mName = state.getMyPlayerName();
		Map map = state.getFullMap();
		ArrayList<PlacementProposal> proposals = new ArrayList<PlacementProposal>();

		Pathfinder2 pathfinder = new Pathfinder2(state.getFullMap(),
				new PathfinderWeighter() {
					public int weight(Region nodeA, Region nodeB) {
						return Values.calculateRegionWeighedCost(mName, oName,
								nodeB);

					}
				});

		for (Integer s : keys) {
			Path path = pathfinder.getPathToSuperRegionFromRegionOwnedByPlayer(
					map.getSuperRegion(s), state.getMyPlayerName());

			if (path == null) {
				// Super region is already controlled
				continue;
			}
			int required = -path.getOrigin().getArmies() + 1;
			for (int i = 1; i < path.getPath().size(); i++) {
				required += Values.calculateRequiredForcesAttack(mName, path
						.getPath().get(i));
			}
			if (required < 1) {
				break;
			}
			float cost = path.getDistance()
					- Values.calculateRegionWeighedCost(mName, oName,
							path.getTarget())
					+ Values.calculateSuperRegionWeighedCost(oName,
							map.getSuperRegion(s));

			float value = worth.get(s) / cost;
			proposals.add(new PlacementProposal(value, path.getOrigin(),
					new Plan(path.getOrigin(), path.getOrigin()
							.getSuperRegion()), required, "GriefCommander"));

		}

		return proposals;
	}

	private HashMap<Integer, Float> calculatePlans(BotState state) {
		ArrayList<SuperRegion> enemySuperRegions = state.getFullMap()
				.getSuspectedOwnedSuperRegions(state.getOpponentPlayerName());

		HashMap<Integer, Float> plans = new HashMap<Integer, Float>();

		for (SuperRegion s : enemySuperRegions) {
			// for every super region they have, calculate how to execute an
			// eventual attack and how much it would be worth

			float reward = s.getArmiesReward();

			plans.put(s.getId(), reward * valueDenialMultiplier);

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
		HashMap<Integer, Float> ranking = calculatePlans(state);

		ArrayList<Region> available = state.getFullMap().getOwnedRegions(
				state.getMyPlayerName());
		final String mName = state.getMyPlayerName();
		final String eName = state.getOpponentPlayerName();
		Pathfinder2 pathfinder = new Pathfinder2(state.getFullMap(),
				new PathfinderWeighter() {
					public int weight(Region nodeA, Region nodeB) {
						if (nodeB.getPlayerName().equals(mName)) {
							return 5;
						} else {
							return Values.calculateRegionWeighedCost(mName,
									eName, nodeB);
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
						- Values.calculateRegionWeighedCost(mName, eName,
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
				totalRequired += Values.calculateRequiredForcesAttack(mName,
						targetSuperRegion);

				int disposed = Math.min(totalRequired, r.getArmies() - 1);
				disposed = Math.max(r.getArmies() / 2, disposed);

				proposals.add(new ActionProposal(currentWeight, r, path
						.getPath().get(1), disposed, new Plan(path.getTarget(),
						targetSuperRegion), "GriefCommander"));

			}

		}
		return proposals;
	}

}
