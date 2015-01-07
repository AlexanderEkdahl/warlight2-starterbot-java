package concepts;

import map.Region;

public class PlacementProposal {
	private float weight;
	private Region region;
	private int requiredForces;
	
	public PlacementProposal(float weight, Region region,int requiredForces) {
		this.weight = weight;
		this.region = region;
		this.requiredForces = requiredForces;
	}

	public float getWeight() {
		return weight;
	}

	public void setWeight(int weight) {
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
