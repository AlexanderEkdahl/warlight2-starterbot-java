package commanders;

import java.util.ArrayList;

///////////// 201 % undone

import java.util.Collection;
import java.util.HashMap;

import map.Pathfinder2;
import map.Pathfinder2.Path;
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
	private static final int rewardDefenseImportanceMultiplier = 40;
	private static final int multipleFrontPenalty = 5;
	private static final int costOfMovingThroughFriendlyTerritory = 4;

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

//		placementProposals.addAll(organizePocketDefence(state));
		placementProposals.addAll(organizeOwnedSuperRegionDefence(state));

		return placementProposals;
	}

	private ArrayList<PlacementProposal> organizeOwnedSuperRegionDefence(
			BotState state) {
		ArrayList<SuperRegion> vulnerable = state.getFullMap()
				.getOwnedFrontSuperRegions(state);

		ArrayList<PlacementProposal> placementProposals = new ArrayList<PlacementProposal>();

		for (SuperRegion s : vulnerable) {
			// it's generally a good thing to focus on superRegions that are
			// cheaply defended
			ArrayList<Region> front = s
					.getFronts(state.getOpponentPlayerName());
			int ownedArmies = 0;
			for (Region r : front) {
				ownedArmies += r.getArmies();
			}
			int enemyArmies = s.getTotalThreateningForce(state
					.getOpponentPlayerName());
			// determine if more dudes are needed

			if (enemyArmies > ownedArmies) {
				float worth = calculateWorth(s);
				float cost = multipleFrontPenalty * front.size();
				float weight = worth / cost;
				for (Region r : front) {
					int needed = r.getArmies()
							- r.getHighestThreateningForce(state
									.getOpponentPlayerName());
					if (needed > 0) {
						placementProposals.add(new PlacementProposal(weight, r,
								new Plan(r, s), needed, "DefensiveCommander"));
					}
				}

			}

		}
		return placementProposals;
	}

	private float calculateWorth(SuperRegion s) {
		return staticSuperRegionDefence + rewardDefenseImportanceMultiplier
				* s.getArmiesReward();
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

		Pathfinder2 pathfinder = new Pathfinder2(state.getFullMap(),
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
				proposals.add(new ActionProposal(calculateImportance(r), r, r,
						r.getArmies(), new Plan(r, r.getSuperRegion()),
						"DefensiveCommander"));
			} else {
				ArrayList<Path> paths = pathfinder.getPathToRegionsFromRegion(
						r, needHelpRegions, mName);
				for (Path path : paths){
					float currentCost = path.getDistance();
					float currentWorth = calculateWorth(r.getSuperRegion());
					float currentWeight = currentWorth / currentCost;
					
					
					int totalRequired = needHelp.get(path.getTarget());
					for (int i = 1; i < path.getPath().size(); i++) {
						totalRequired += Values
								.calculateRequiredForcesAttackTotalVictory(mName,
										path.getPath().get(i));
					}
					int disposed = Math.min(totalRequired, r.getArmies() - 1);
					
					proposals.add(new ActionProposal(currentWeight, r, path
							.getPath().get(1), disposed, new Plan(path.getTarget(),
							path.getTarget().getSuperRegion()), "DefensiveCommander"));
					
				}
			}

		}

		return proposals;
	}

	private float calculateImportance(Region r) {
		// TODO Auto-generated method stub
		return 0;
	}


}
