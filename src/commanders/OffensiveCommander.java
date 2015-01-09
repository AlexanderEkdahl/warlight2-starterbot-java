package commanders;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;

import concepts.ActionProposal;
import concepts.PlacementProposal;
import concepts.Plan;
import bot.BotState;
import bot.Values;
import map.*;
import move.AttackTransferMove;

public class OffensiveCommander extends TemplateCommander {
	private static final int rewardMultiplier = 40;
	private static final int costMultiplier = 1;
	private static final int staticRegionBonus = 10;

	@Override
	public ArrayList<PlacementProposal> getPlacementProposals(BotState state) {
		Map currentMap = state.getVisibleMap();

		// if we don't have any super regions, prioritize expansion greatly
		if (currentMap.getOwnedSuperRegions(state.getMyPlayerName()).size() < 1) {
			selfImportance = 1000;
		} else {
			selfImportance = 1;
		}

		ArrayList<Plan> plans = calculatePlans(state);
		ArrayList<PlacementProposal> attackPlans = new ArrayList<PlacementProposal>();
		for (Plan p : plans) {
			p.setWeight(p.getWeight() + selfImportance);
		}
		PlacementProposal tempProposal;
		for (Plan p : plans) {
			tempProposal = prepareAttack(p, state);
			if (tempProposal != null) {
				attackPlans.add(prepareAttack(p, state));
			}

		}
		return attackPlans;
	}

	private PlacementProposal prepareAttack(Plan p, BotState state) {
		SuperRegion sr = p.getSr();
		ArrayList<Region> owned = state.getVisibleMap().getOwnedRegions(
				state.getMyPlayerName());

		ArrayList<Region> neighbors;
		for (Region r : owned) {
			neighbors = r.getNeighbors();
			for (Region n : neighbors) {
				if (n.getSuperRegion().equals(sr)
						&& !n.getPlayerName().equals(state.getMyPlayerName())) {
					return new PlacementProposal(p.getWeight(), r,
							Values.calculateRequiredForcesAttack(
									state.getMyPlayerName(), sr), p,
							"OffensiveCommander");
				}
			}

		}
		return null;

	}

	private ArrayList<Plan> calculatePlans(BotState state) {
		ArrayList<Plan> plans = new ArrayList<Plan>();
		HashSet<SuperRegion> possibleTargets = new HashSet<SuperRegion>();

		possibleTargets.addAll(state.getVisibleMap().getSuperRegions());

		// exclude owned superregions
		possibleTargets.removeAll((state.getVisibleMap()
				.getOwnedSuperRegions(state.getMyPlayerName())));

		for (SuperRegion s : possibleTargets) {
			plans.add(new Plan(s, calculateWorth(s, state)));
		}
		return plans;
	}

	private int calculateWorth(SuperRegion s, BotState state) {
		if (s.getArmiesReward() < 1) {
			return -1000;
		} else {
			int pathCost = 100;
			for (Region r : s.getSubRegions()) {
				if (r.getPlayerName().equals(state.getMyPlayerName())) {
					pathCost = 0;
					break;
				}
			}
			int cost = Values.calculateRequiredForcesAttack(
					state.getMyPlayerName(), s);
			int reward = s.getArmiesReward();
			return (reward * rewardMultiplier) + staticRegionBonus
					- (cost * costMultiplier) - pathCost;

		}
	}

	@Override
	public ArrayList<ActionProposal> getActionProposals(BotState state) {
		ArrayList<ActionProposal> proposals = new ArrayList<ActionProposal>();

		ArrayList<Plan> attackPlans = calculatePlans(state);

		for (Plan p : attackPlans) {
			p.setWeight(p.getWeight() + selfImportance);
		}
		Collections.sort(attackPlans);
		ArrayList<Region> neighbors;

		ArrayList<Region> owned = state.getVisibleMap().getOwnedRegions(
				state.getMyPlayerName());
		ArrayList<Region> available = (ArrayList<Region>) owned.clone();
		for (Plan p : attackPlans) {
			int forcesRequired = Values.calculateRequiredForcesAttack(
					state.getMyPlayerName(), p.getSr());

			outerLoop: for (Region r : owned) {
				neighbors = r.getNeighbors();
				for (Region n : neighbors) {
					if (n.getSuperRegion().equals(p.getSr())
							&& !(n.getPlayerName().equals(state
									.getMyPlayerName()))
							&& r.getArmies() > Values
									.calculateRequiredForcesAttack(
											state.getMyPlayerName(), n)) {
						int forcesAvailable = r.getArmies() - 1;
						int forcesDisposed = forcesAvailable;
						proposals.add(new ActionProposal(p.getWeight(), r, n,
								forcesDisposed, p, "OffensiveCommander"));
						available.remove(r);
						break outerLoop;

					}

				}
			}

		}

		// move idle units up to the most important superRegion with high
		// importance

		SuperRegion importantSuperRegion = attackPlans.get(0).getSr();

		for (Region r : available) {
			final String myName = state.getMyPlayerName();
			Pathfinder pathfinder = new Pathfinder(state.getVisibleMap(),
					new PathfinderWeighter() {
						public int weight(Region nodeA, Region nodeB) {
							if (!nodeB.getPlayerName().equals(myName)) {
								return nodeB.getArmies();
							}
							return 1;
						}
					});
			pathfinder.execute(r);

			for (Region subr : importantSuperRegion.getSubRegions()) {
				if (!subr.equals(r)) {
					proposals.add(new ActionProposal(selfImportance - 100, r,
							pathfinder.getPath(subr).get(1), r.getArmies() - 1,
							attackPlans.get(0), "OffensiveCommander"));
					break;
				}
			}
		}

		return proposals;

	}
}
