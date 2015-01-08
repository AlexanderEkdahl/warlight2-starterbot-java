package concepts;

import map.Region;

public class ActionProposal extends TemplateProposal {
	private Region origin;

	public ActionProposal(float weight, Region origin, Region target, int requiredForces, Plan plan) {
		super(weight,target,requiredForces, plan);
		this.origin = origin;
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

	public int getForces() {
		return forces;
	}

	public void setForcess(int forces) {
		this.forces = forces;
	}


}
