package commanders;

import java.util.ArrayList;

///////////// 201 % undone

import java.util.Collection;
import java.util.HashMap;

import map.Pathfinder;
import map.Pathfinder.Path;
import map.PathfinderWeighter;
import map.Region;
import map.SuperRegion;
import concepts.ActionProposal;
import concepts.PlacementProposal;
import concepts.Plan;
import bot.BotState;
import bot.Values;

public class DefensiveCommander extends TemplateCommander {
	private static final int staticPocketDefence = 30;
	private static final int staticSuperRegionDefence = 0;
	private static final int rewardDefenseImportanceMultiplier = 30;

	private static final int costOfMovingThroughFriendlyTerritory = 4;
	private static final int extraArmiesDefence = 5;

	@Override
	public ArrayList<PlacementProposal> getPlacementProposals(BotState state) {
		// pockets are solitary tiles without connection to friendly tiles

		ArrayList<SuperRegion> vulnerableSuperRegions = state.getFullMap()
				.getOwnedFrontSuperRegions(state);

		ArrayList<PlacementProposal> proposals = calculatePlans(state,
				vulnerableSuperRegions);
		return proposals;
	}

	private ArrayList<PlacementProposal> calculatePlans(BotState state,
			ArrayList<SuperRegion> vulnerableSuperRegions) {
		ArrayList<PlacementProposal> placementProposals = new ArrayList<PlacementProposal>();

		placementProposals.addAll(organizePocketDefence(state));
		placementProposals.addAll(organizeSuperRegionDefence(state));

		return placementProposals;
	}

	private ArrayList<PlacementProposal> organizeSuperRegionDefence(
			BotState state) {

		ArrayList<SuperRegion> vulnerable = state.getFullMap()
				.getOwnedFrontSuperRegions(state);
		String mName = state.getMyPlayerName();
		String eName = state.getOpponentPlayerName();

		ArrayList<PlacementProposal> placementProposals = new ArrayList<PlacementProposal>();
		for (SuperRegion s : vulnerable) {
			float weight = calculateWeight(s, eName);
			Region mostVulnerableRegion = null;
			float currentRequiredQuota;
			int diff = s.getTotalFriendlyForce(mName)
					- s.getTotalThreateningForce(eName);

			HashMap<Region, Integer> ownForces = new HashMap<Region, Integer>();

			for (Region r : s.getFronts(eName)) {
				ownForces.put(r, r.getArmies());
			}
			// keep on assigning dudes until we have at least 5 more in our
			// superregion than they have
			// and add them in the most vulnerable region
			while (diff < extraArmiesDefence) {
				float minRequiredQuota = Float.MAX_VALUE;
				for (Region r : s.getFronts(eName)) {
					currentRequiredQuota = ownForces.get(r)
							/ r.getHighestThreateningForce(eName);
					if (currentRequiredQuota < minRequiredQuota) {
						minRequiredQuota = currentRequiredQuota;
						mostVulnerableRegion = r;
					}

				}
				placementProposals.add(new PlacementProposal(weight,
						mostVulnerableRegion, new Plan(mostVulnerableRegion,
								mostVulnerableRegion.getSuperRegion()), 1,
						"DefensiveCommander"));
				ownForces.put(mostVulnerableRegion,
						ownForces.get(mostVulnerableRegion) + 1);
				diff++;
			}

		}
		return placementProposals;
	}

	private float calculateWeight(SuperRegion s, String eName) {
		float worth = calculateWorth(s);
		float cost = calculateCost(s, eName);
		float weight = worth / cost;
		return weight;
	}

	private float calculateWorth(SuperRegion s) {
		float worth = staticSuperRegionDefence
				+ rewardDefenseImportanceMultiplier * s.getArmiesReward();
		return worth;
	}

	private float calculateCost(SuperRegion s, String eName) {
		float cost = (s.getFronts(eName).size() * Values.multipleFrontPenalty)
				+ (s.getTotalThreateningForce(eName) * Values.costMultiplierDefendingAgainstEnemy);
		return cost;
	}

	private ArrayList<PlacementProposal> organizePocketDefence(BotState state) {
		ArrayList<Region> pockets = state.getFullMap().getPockets(state);
		ArrayList<PlacementProposal> pocketPlacementProposals = new ArrayList<PlacementProposal>();
		String eName = state.getOpponentPlayerName();

		for (Region r : pockets) {
			int highestEnemy = r.getHighestThreateningForce(eName);

			if (r.getArmies() < highestEnemy) {
				int difference = highestEnemy - r.getArmies();
				int weight = staticPocketDefence;
				pocketPlacementProposals.add(new PlacementProposal(weight, r,
						new Plan(r, r.getSuperRegion()), difference + 1,
						"DefensiveCommander"));
			}

		}
		return pocketPlacementProposals;
	}

	private ArrayList<Plan> calculatePlans(BotState state) {
		return null;

	}

	@Override
	public ArrayList<ActionProposal> getActionProposals(BotState state) {
		ArrayList<ActionProposal> proposals = new ArrayList<ActionProposal>();
		ArrayList<Region> fronts = state.getFullMap().getOwnedFrontRegions(
				state);
		HashMap<Region, Integer> needHelp = new HashMap<Region, Integer>();
		ArrayList<Region> needHelpRegions = new ArrayList<Region>();

		final String eName = state.getOpponentPlayerName();
		final String mName = state.getMyPlayerName();
		for (Region r : fronts) {
			// for all the interesting regions, calculate if they need more
			// defence
			needHelp.put(r, r.getHighestThreateningForce(eName) - r.getArmies());
			needHelpRegions.add(r);

		}

		ArrayList<Region> available = state.getFullMap().getOwnedRegions(
				state.getMyPlayerName());

		Pathfinder pathfinder = new Pathfinder(state.getFullMap(),
				new PathfinderWeighter() {
					public int weight(Region nodeA, Region nodeB) {
						if (nodeB.getPlayerName().equals(mName)) {
							return costOfMovingThroughFriendlyTerritory;
						} else {
							return Values.calculateRegionWeighedCost(mName,
									eName, nodeB);
						}

					}
				});

		for (Region r : available) {
			// if this region is in need of defence and has too few currently on
			// it to defend, don't fucking attack anyone else you dipshit, at
			// least that's what I think but hey I'm just the defensivecommander
			// who cares what I think
			if (needHelp.get(r) != null && needHelp.get(r) > 0) {
				proposals.add(new ActionProposal(calculateWeight(
						r.getSuperRegion(), eName), r, r, r.getArmies(),
						new Plan(r, r.getSuperRegion()), "DefensiveCommander"));
			} else {
				ArrayList<Path> paths = pathfinder.getPathToRegionsFromRegion(
						r, needHelpRegions, mName);
				for (Path path : paths) {
					float currentCost = path.getDistance()
							+ calculateCost(path.getTarget().getSuperRegion(),
									eName);
					float currentWorth = calculateWorth(r.getSuperRegion());
					float currentWeight = currentWorth / currentCost;

					int totalRequired = needHelp.get(path.getTarget());
					for (int i = 1; i < path.getPath().size(); i++) {
						totalRequired += Values
								.calculateRequiredForcesAttackTotalVictory(
										mName, path.getPath().get(i));
					}
					int disposed = Math.min(totalRequired, r.getArmies() - 1);

					proposals.add(new ActionProposal(currentWeight, r, path
							.getPath().get(1), disposed, new Plan(path
							.getTarget(), path.getTarget().getSuperRegion()),
							"DefensiveCommander"));

				}
			}

		}

		return proposals;
	}

}
