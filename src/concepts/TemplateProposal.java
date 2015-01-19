package concepts;

import map.Region;

public abstract class TemplateProposal implements Comparable<TemplateProposal> {
	protected float weight;
	protected Region target;
	protected int forces;
	protected Plan plan;
	protected String issuedBy;

	public TemplateProposal(float weight, Region target, Plan plan, int forces,
			String issuedBy) {
		this.weight = weight;
		this.target = target;
		this.forces = forces;
		this.issuedBy = issuedBy;
		this.plan = plan;
	}

	public Plan getPlan() {
		return plan;
	}

	public void SuperRegion(Plan plan) {
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

	public String getIssuedBy() {
		return issuedBy;
	}

	public int compareTo(TemplateProposal otherProposal) {
		if (otherProposal.getWeight() > weight) {
			return 1;
		} else if (otherProposal.getWeight() == weight) {
			return 0;

		} else {
			return -1;
		}
	}

	public abstract String toString();

}
