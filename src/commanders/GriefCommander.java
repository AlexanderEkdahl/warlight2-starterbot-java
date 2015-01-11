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

public class GriefCommander extends TemplateCommander {
	public static final int valueDenialMultiplier = 20;

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
						return Values.calculateRegionWeighedCost(oName, nodeB);

					}
				});

		for (Integer s : keys) {
			Path path = pathfinder.getPathToSuperRegionFromRegionOwnedByPlayer(
					map.getSuperRegion(s), state.getMyPlayerName());

			
			if (path == null) {
				System.err
						.println("ALEX YOU DID IT AGAIN YOU LOUSY EXCUSE OF A PROGRAMMER, GRIEFCOMMANDER CAN'T PLACE");
				continue;
			}
			int required = path.getDistance();
			float cost = path.getDistance()
					- Values.calculateRegionWeighedCost(oName, path.getTarget())
					+ Values.calculateSuperRegionWeighedCost(oName,
							map.getSuperRegion(s));

			float value = worth.get(s) / cost;
			proposals.add(new PlacementProposal(value, path.getOrigin(), map
					.getSuperRegion(s), required, "GriefCommander"));

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
		HashMap<Integer, Float> plans = calculatePlans(state);

		ArrayList<Region> owned = state.getFullMap().getOwnedRegions(
				state.getMyPlayerName());

		ActionProposal tempProposal;
		if (plans.size() > 0) {
			for (Region r : owned) {
				if (r.getArmies() < 2) {
					continue;
				}
				tempProposal = createOrder(r, state, plans);
				if (tempProposal != null) {

				}
				proposals.add(createOrder(r, state, plans));
			}
		}

		return proposals;
	}

	private ActionProposal createOrder(Region r, BotState state,
			ArrayList<Plan> plans) {

		final String enemyName = state.getOpponentPlayerName();
		Map map = state.getFullMap();
		float maxWorth = Integer.MIN_VALUE;
		SuperRegion sr;
		LinkedList<Region> path;
		int totalCost = 0;
		Region bestTarget = null;
		Plan bestPlan = null;
		for (Plan p : plans) {
			sr = p.getSr();
			Pathfinder pathfinder = new Pathfinder(state.getFullMap(),
					new PathfinderWeighter() {
						public int weight(Region nodeA, Region nodeB) {
							if (nodeB.getPlayerName().equals(enemyName)) {
								return nodeB.getArmies()
										* Values.costMultiplierEnemy;
							} else if (nodeB.getPlayerName().equals("neutral")) {
								return nodeB.getArmies()
										* Values.costMultiplierNeutral;
							} else if (nodeB.getPlayerName().equals("unknown")) {
								return Values.staticCostUnknown;
							}
							return Values.staticCostOwned;
						}
					});

			pathfinder.execute(r);

			// should be the closest in the region, not .get(0)
			path = pathfinder.getPath(p.getSr().getSubRegions().get(0));
			for (int i = 1; i < path.size(); i++) {
				totalCost += Values.calculateRegionWeighedCost(enemyName,
						path.get(i));

			}
			float currentWorth = p.getWeight() / totalCost;

			if (maxWorth < currentWorth) {
				maxWorth = currentWorth;
				bestTarget = path.get(1);
				bestPlan = p;
			}

		}

		int calculatedForcesRequired = Values.calculateRequiredForcesAttack(
				state.getMyPlayerName(), bestTarget);

		if (calculatedForcesRequired > r.getArmies()) {
			return new ActionProposal(bestPlan.getWeight() - totalCost, r, r,
					r.getArmies() - 1, bestPlan.getSr(), "GriefCommander");
		} else {
			return new ActionProposal(bestPlan.getWeight() - totalCost, r,
					bestTarget, r.getArmies() - 1, bestPlan.getSr(),
					"GriefCommander");
		}

	}
}
