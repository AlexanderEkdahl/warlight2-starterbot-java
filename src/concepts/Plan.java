package concepts;

import map.SuperRegion;

public class Plan implements Comparable<Plan>{
	SuperRegion sr;
	float weight;
	
	public Plan(SuperRegion sr){
		this.sr = sr;
		weight = 0;
	}
	public Plan(SuperRegion sr, int weight){
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
	public SuperRegion getSr(){
		return sr;
	}
	@Override
	public int compareTo(Plan otherPlan) {
		if (otherPlan.getWeight() > weight){
			return 1;
		}
		else{
			return -1;
		}
	}

}
