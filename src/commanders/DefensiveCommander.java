package commanders;

import java.util.ArrayList;

///////////// 201 % undone

import java.util.Collection;
import java.util.HashMap;

import map.Region;
import map.SuperRegion;
import concepts.ActionProposal;
import concepts.PlacementProposal;
import concepts.Plan;
import bot.BotState;

public class DefensiveCommander extends TemplateCommander {
	private static final int staticPocketDefence = 50;
	private static final int staticSuperRegionDefence = 10;
	private static final int rewardDefenseImportanceMultiplier = 15;
	private static final int enemyTroopMatching = 1;

	@Override
	public ArrayList<PlacementProposal> getPlacementProposals(BotState state) {
		HashMap<Integer, Float> worth = new HashMap<Integer, Float>();
		ArrayList<Region> vulnerableRegions = state.getFullMap()
				.getOwnedFrontRegions(state);

		// pockets are solitary tiles without connection to friendly tiles

		// rewardBlockers are tiles that single handedly prevent the enemy from
		// cashing in on that sweet super region reward

		ArrayList<SuperRegion> vulnerableSuperRegions = state.getFullMap()
				.getOwnedFrontSuperRegions(state);

		ArrayList<PlacementProposal> proposals = calculatePlans(state,
				vulnerableSuperRegions);
		return proposals;
	}

	private ArrayList<PlacementProposal> calculatePlans(BotState state,
			ArrayList<SuperRegion> vulnerableSuperRegions) {
		ArrayList<PlacementProposal> placementProposals = new ArrayList<PlacementProposal>();
		ArrayList<Plan> plans = new ArrayList<Plan>();
		
		placementProposals.addAll(organizePocketDefence(state));
		placementProposals.addAll(organizeOwnedSuperRegionDefence(state));
		

		
		return placementProposals;
	}

	private  ArrayList<PlacementProposal> organizeOwnedSuperRegionDefence(
			BotState state) {
		ArrayList<SuperRegion> vulnerable = state.getFullMap().getOwnedFrontSuperRegions(state);
		
		for (SuperRegion s : vulnerable){
			// better to focus on superRegions that are cheaply defended
			ArrayList<Region> front = s.getFronts(state.getOpponentPlayerName());
			s.getTotalThreateningForce(state.getOpponentPlayerName());
			
		}
		return null;
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
				pocketPlacementProposals
						.add(new PlacementProposal(weight, r, r
								.getSuperRegion(), difference + 2,
								"DefensiveCommander"));
			}

		}
		return pocketPlacementProposals;
	}

	private SuperRegion calculateVulnerableSuperRegion() {
		return null;
		// TODO Auto-generated method stub

	}

	private ArrayList<Plan> calculatePlans(BotState state) {
		return null;

	}

	@Override
	public ArrayList<ActionProposal> getActionProposals(BotState state,
			AttackSatisfaction as) {
		ArrayList<ActionProposal> proposals = new ArrayList<ActionProposal>();
		// TODO Auto-generated method stub
		return proposals;
	}

}
