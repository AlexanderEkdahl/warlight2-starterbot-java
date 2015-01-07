package concepts;

import map.Region;

public class ActionProposal implements Comparable<ActionProposal> {
	private float weight;
	private Region origin;
	private Region target;
	private int requiredForces;

	public ActionProposal(float weight, Region origin, Region target, int requiredForces) {
		this.origin = origin;
		this.weight = weight;
		this.requiredForces = requiredForces;
	}

	public float getWeight() {
		return weight;
	}

	public void setWeight(float weight) {
		this.weight = weight;
	}

	public Region getOrigin() {
		return origin;
	}

	public void setOrigin(Region region) {
		this.origin = region;
	}

	public Region getTarget() {
		return target;
	}

	public void setTarget(Region target) {
		this.target = target;
	}

	public int getRequiredForces() {
		return requiredForces;
	}

	public void setRequiredForces(int requiredForces) {
		this.requiredForces = requiredForces;
	}

	@Override
	public int compareTo(ActionProposal otherProposal) {
		if (otherProposal.getWeight() > weight) {
			return -1;
		} else {
			return 1;
		}
	}

}
