package concepts;

import bot.BotState;
import map.*;
import move.PlaceArmiesMove;

public class PlacementProposal extends TemplateProposal {

	public PlacementProposal(float weight, Region region, SuperRegion sr,  int requiredForces,
			String issuedBy) {
		super(weight, region, sr, requiredForces, issuedBy);
	}

	@Override
	public String toString() {
		return "Weight: " + weight + " Region: " + target.getId() + " Forces: "
				+ forces + " Issued By: " + issuedBy + " Plan: "
				+ plan.getId();
	}

}
