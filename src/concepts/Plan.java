package concepts;

import map.Region;
import map.SuperRegion;

public class Plan implements Comparable<Plan> {
	SuperRegion sr;
	Region r;
	float weight;

	public Plan(Region r, SuperRegion sr) {
		this.sr = sr;
		this.r = r;
		weight = 0;
	}

	public Plan(SuperRegion sr, int weight) {
		this.sr = sr;
		this.weight = weight;
	}

	public float getWeight() {
		return weight;
	}

	public void setWeight(float f) {
		this.weight = f;
	}

	public void setSr(SuperRegion sr) {
		this.sr = sr;
	}

	public SuperRegion getSr() {
		return sr;
	}
	
	public Region getR(){
		return r;
	}
	
	public String toString(){
		return ("Region: " + r + " SuperRegion: " + sr);
	}
 
	@Override
	public int compareTo(Plan otherPlan) {
		if (otherPlan.getWeight() > weight) {
			return 1;
		} else if (otherPlan.getWeight() == weight) {
			return 0;
		} else {
			return -1;
		}
	}

}
