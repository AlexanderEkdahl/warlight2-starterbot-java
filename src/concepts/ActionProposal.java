package concepts;

import map.Region;

public class ActionProposal {
	private float weight;
	private Region region;
	private int requiredForces;
	
	public ActionProposal(float weight, Region region, int requiredForces){
		this.region = region;
		this.weight = weight;
		this.requiredForces = requiredForces;
	}

	public float getWeight() {
		return weight;
	}

	public void setWeight(float weight) {
		this.weight = weight;
	}

	public Region getRegion() {
		return region;
	}

	public void setRegion(Region region) {
		this.region = region;
	}

	public int getRequiredForces() {
		return requiredForces;
	}

	public void setRequiredForces(int requiredForces) {
		this.requiredForces = requiredForces;
	}

}
