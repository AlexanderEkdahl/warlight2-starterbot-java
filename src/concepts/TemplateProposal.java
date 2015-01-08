package concepts;

import map.Region;

public abstract class TemplateProposal implements Comparable<TemplateProposal> {
	protected float weight;
	protected Region target;
	protected int forces;
	protected Plan plan;

	public TemplateProposal(float weight, Region target, int forces, Plan plan) {
		this.weight = weight;
		this.target = target;
		this.forces = forces;
		this.plan = plan;
	}

	public Plan getPlan() {
		return plan;
	}

	public void setPlan(Plan plan) {
		this.plan = plan;
	}

	public float getWeight() {
		return weight;
	}

	public void setWeight(float weight) {
		this.weight = weight;
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

	public void setForces(int forces) {
		this.forces = forces;
	}

	public int compareTo(TemplateProposal otherProposal) {
		if (otherProposal.getWeight() > weight) {
			return 1;
		} else {
			return -1;
		}
	}

	public String toString() {
		return "This proposal has a weight of " + weight + " a target of " + target.getId()
				+ " with " + forces + " forces.";
	}

}
