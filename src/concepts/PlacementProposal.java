package concepts;

import bot.BotState;
import map.Region;
import move.PlaceArmiesMove;

public class PlacementProposal extends TemplateProposal {

	public PlacementProposal(float weight, Region region, int requiredForces,
			Plan plan, String issuedBy) {
		super(weight, region, requiredForces, plan, issuedBy);
	}

	@Override
	public String toString() {
		return "Weight: " + weight + " Region: " + target + " Forces: "
				+ forces + " Issued By: " + issuedBy + " Plan: "
				+ plan.getSr().getId() + " Plan weight: " + plan.getWeight();
	}

}
