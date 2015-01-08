package commanders;

import java.util.ArrayList;

import map.SuperRegion;
import concepts.ActionProposal;
import concepts.PlacementProposal;
import bot.BotState;

public class DefensiveCommander extends TemplateCommander {

	@Override
	public ArrayList<PlacementProposal> getPlacementProposals(BotState state) {
		if (state.getVisibleMap().getOwnedSuperRegions(state.getMyPlayerName()).size() < 1){
			return null;
		}
		SuperRegion vulnerable = calculateVulnerableSuperRegion();
		ArrayList<PlacementProposal> proposals = organizeDefense(vulnerable);
		return proposals;
	}

	private ArrayList<PlacementProposal> organizeDefense(SuperRegion vulnerable) {
		// TODO Auto-generated method stub
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


}
