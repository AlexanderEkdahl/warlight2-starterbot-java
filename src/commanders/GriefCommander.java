package commanders;

import java.util.ArrayList;
import java.util.LinkedList;

import map.Map;
import map.Pathfinder;
import map.PathfinderWeighter;
import map.Region;
import map.SuperRegion;
import bot.BotState;
import bot.Values;
import concepts.ActionProposal;
import concepts.PlacementProposal;
import concepts.Plan;

public class GriefCommander extends TemplateCommander {
	public static final int valueDenialMultiplier = 20;

	@Override
	public ArrayList<PlacementProposal> getPlacementProposals(BotState state) {

		ArrayList<PlacementProposal> proposals = new ArrayList<PlacementProposal>();

		// don't start griefing too early
		if (state.getRoundNumber() < 3) {
			return proposals;
		}
		ArrayList<Region> enemyRegions = state.getVisibleMap().getOwnedRegions(
				state.getOpponentPlayerName());

		ArrayList<Plan> plans = calculatePlans(state);

		for (Plan p : plans) {
			proposals.add(prepareAttack(p, state));
		}

		return proposals;
	}

	private ArrayList<Plan> calculatePlans(BotState state) {
		ArrayList<SuperRegion> enemySuperRegions = state.getVisibleMap()
				.getSuspectedOwnedSuperRegions(state.getOpponentPlayerName());
		ArrayList<Plan> plans = new ArrayList<Plan>();

		for (SuperRegion s : enemySuperRegions) {
			// for every super region they have, calculate how to execute an
			// eventual attack and how much it would cost/be worth

			int reward = s.getArmiesReward();

			plans.add(new Plan(s, reward * valueDenialMultiplier));

		}
		return plans;
	}

	private PlacementProposal prepareAttack(Plan p, BotState state) {

		SuperRegion s = p.getSr();
		Region nearestRegion;
		LinkedList<Region> path;

		final String myName = state.getMyPlayerName();
		final String enemyName = state.getOpponentPlayerName();
		Pathfinder pathfinder = new Pathfinder(state.getVisibleMap(),
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
		// targetRegion should be changed to closest, right now it isn't
		Region targetRegion = s.getSubRegions().get(0);
		nearestRegion = pathfinder.getNearestOwnedRegion(targetRegion,
				state.getMyPlayerName());

		pathfinder.execute(nearestRegion);
		path = pathfinder.getPath(targetRegion);

		// calculate how worth it the path would be
		// and how many would be needed

		int totalCost = 0;
		int demands = 0;
		for (int i = 1; i < path.size(); i++) {
			totalCost += Values.calculateRegionWeighedCost(enemyName,
					path.get(i));
			demands += Values
					.calculateRequiredForcesAttack(myName, path.get(i));
		}

		demands += Values.calculateRequiredForcesAttack(myName, targetRegion);
		float worth = p.getWeight() - totalCost;
		return new PlacementProposal(worth, nearestRegion, p.getSr(), demands,
				"GriefCommander");

	}

	@Override
	public ArrayList<ActionProposal> getActionProposals(BotState state) {
		ArrayList<ActionProposal> proposals = new ArrayList<ActionProposal>();

		// don't start griefing too early
		if (state.getRoundNumber() < 3) {
			return proposals;
		}
		ArrayList<Plan> plans = calculatePlans(state);

		ArrayList<Region> owned = state.getVisibleMap().getOwnedRegions(
				state.getMyPlayerName());

		ActionProposal tempProposal;
		if (plans.size() > 0) {
			for (Region r : owned) {
				if (r.getArmies() < 2) {
					break;
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
		Map map = state.getVisibleMap();
		float maxWorth = Integer.MIN_VALUE;
		SuperRegion sr;
		LinkedList<Region> path;
		int totalCost = 0;
		Region bestTarget = null;
		Plan bestPlan = null;
		for (Plan p : plans) {
			sr = p.getSr();
			Pathfinder pathfinder = new Pathfinder(state.getVisibleMap(),
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
			float currentWorth = p.getWeight() - totalCost;

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
