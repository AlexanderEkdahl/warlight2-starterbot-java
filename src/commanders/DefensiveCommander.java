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
	private static final int staticPocketDefence = 40;
	private static final int staticSuperRegionDefence = 10;
	private static final int rewardDefenseImportanceMultiplier = 15;
	private static final int MultipleFrontPenalty = 10;
	private static final int enemyTroopMatching = 1;
	private static final int costOfMovingThroughFriendlyTerritory = 5;

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
				float worth = staticSuperRegionDefence
						+ rewardDefenseImportanceMultiplier
						* s.getArmiesReward();
				float cost = MultipleFrontPenalty * front.size();
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
		HashMap<SuperRegion, Integer> neededSuperRegion = new HashMap<SuperRegion, Integer>();

		final String eName = state.getOpponentPlayerName();
		final String mName = state.getMyPlayerName();
		for (Region r : fronts) {
			if (r.getTotalThreateningForce(eName) > r.getArmies()) {
				needHelp.put(r,
						r.getTotalThreateningForce(eName) - r.getArmies());
			}
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

		
		for (Region r : available){
			
		}

		ArrayList<SuperRegion> vulnerable = state.getFullMap()
				.getOwnedFrontSuperRegions(state);

		return proposals;
	}

	private ArrayList<Region> determineBestDefensivePositions() {
		// TODO Auto-generated method stub
		return null;
	}

}
