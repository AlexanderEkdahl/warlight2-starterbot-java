package commanders;

import java.util.ArrayList;

///////////// 201 % undone

import map.Region;
import map.SuperRegion;
import concepts.ActionProposal;
import concepts.PlacementProposal;
import concepts.Plan;
import bot.BotState;

public class DefensiveCommander extends TemplateCommander {
	private static final int staticRewardDefence = 10;
	private static final int rewardDefenseImportanceMultiplier = 15;
	private static final int enemyTroopMatching = 1;

	@Override
	public ArrayList<PlacementProposal> getPlacementProposals(BotState state) {

		ArrayList<Region> vulnerableRegions = state.getFullMap()
				.getOwnedFrontRegions(state);
		ArrayList<SuperRegion> vulnerableSuperRegions = state.getFullMap()
				.getOwnedFrontSuperRegions(state);
		ArrayList<Plan> proposals = calculatePlans(vulnerableSuperRegions);
		return null;
	}

	private ArrayList<Plan> calculatePlans(ArrayList<SuperRegion> vulnerableSuperRegions) {
		ArrayList<Plan> plans = new ArrayList<Plan>();
		int enemyForces;
		int reward;
		
		ArrayList<Region> enemyFront = new ArrayList<Region>();
		
		for (SuperRegion s : vulnerableSuperRegions){
//			for ()
			
		}
		return null;
	}

	private SuperRegion calculateVulnerableSuperRegion() {
		return null;
		// TODO Auto-generated method stub

	}

	@Override
	public ArrayList<ActionProposal> getActionProposals(BotState state) {
		// TODO Auto-generated method stub
		return null;
	}

	private ArrayList<Plan> calculatePlans(BotState state) {
		return null;

	}

}
