package concepts;

import map.*;

public class ActionProposal extends TemplateProposal {
	private Region origin;

	public ActionProposal(float weight, Region origin, Region target,
			int requiredForces, SuperRegion plan, String issuedBy) {
		super(weight, target, plan, requiredForces, issuedBy);
		this.origin = origin;
	}

	public Region getOrigin() {
		return origin;
	}

	@Override
	public String toString() {
		return "Weight: " + weight + " Region: " + target.getId() + " From: "
				+ origin.getId() + " Forces: " + forces + " Issued By: "
				+ issuedBy + " Plan: " + plan.getId();
	}

}
