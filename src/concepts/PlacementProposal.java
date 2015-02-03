package concepts;

import bot.BotState;
import map.*;
import move.PlaceArmiesMove;

public class PlacementProposal extends TemplateProposal {

	public PlacementProposal(double weight, Region region, Plan plan,  int requiredForces,
			String issuedBy) {
		super(weight, region, plan, requiredForces, issuedBy);
	}

	@Override
	public String toString() {
		return "Weight: " + weight + " Region: " + target.getId() + " Forces: "
				+ forces + " Issued By: " + issuedBy + " Plan: "
				+ plan.toString();
	}

}
