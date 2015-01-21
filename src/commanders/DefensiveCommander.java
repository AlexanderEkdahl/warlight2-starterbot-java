package commanders;

import java.util.ArrayList;

///////////// 201 % undone

import java.util.Collection;
import java.util.HashMap;

import map.Pathfinder2;
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
	private static final int staticSuperRegionDefence = 10;
	private static final int rewardDefenseImportanceMultiplier = 15;
	private static final int multipleFrontPenalty = 10;
	private static final int costOfMovingThroughFriendlyTerritory = 4;

	@Override
	public ArrayList<PlacementProposal> getPlacementProposals(BotState state) {
		HashMap<Integer, Float> worth = new HashMap<Integer, Float>();
		ArrayList<Region> vulnerableRegions = state.getFullMap()
				.getOwnedFrontRegions(state);

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
							- r.getTotalThreateningForce(state
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
		return staticSuperRegionDefence
		+ rewardDefenseImportanceMultiplier
		* s.getArmiesReward();
	}

	private ArrayList<PlacementProposal> organizePocketDefence(BotState state) {
		ArrayList<Region> pockets = state.getFullMap().getPockets(state);
		ArrayList<PlacementProposal> pocketPlacementProposals = new ArrayList<PlacementProposal>();

		for (Region r : pockets) {
			int totalEnemies = 0;
			for (Region n : r.getNeighbors()) {
				if (n.getPlayerName().equals(state.getOpponentPlayerName())) {
					totalEnemies += n.getArmies();
				}
			}
			if (r.getArmies() < totalEnemies) {
				int difference = totalEnemies - r.getArmies();
				int weight = staticPocketDefence;
				pocketPlacementProposals.add(new PlacementProposal(weight, r,
						new Plan(r, r.getSuperRegion()), difference + 2,
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
		HashMap<SuperRegion, Integer> neededSuperRegion = new HashMap<SuperRegion, Integer>();

		final String eName = state.getOpponentPlayerName();
		final String mName = state.getMyPlayerName();
		for (Region r : fronts) {
			needHelp.put(r, r.getTotalThreateningForce(eName) - r.getArmies());
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
			if (needHelp.get(r) > 0) {
				proposals.add(new ActionProposal(calculateImportance(r), r, r,
						r.getArmies(), new Plan(r, r.getSuperRegion()),
						"DefensiveCommander"));
			}
			
			
			pathfinder.getPathToRegionsFromRegion(r, needHelpRegions, mName);
		}

		ArrayList<SuperRegion> vulnerable = state.getFullMap()
				.getOwnedFrontSuperRegions(state);

		return proposals;
	}

	private float calculateImportance(Region r) {
		// TODO Auto-generated method stub
		return 0;
	}

	private ArrayList<Region> determineBestDefensivePositions() {
		// TODO Auto-generated method stub
		return null;
	}

}
