package concepts;

import map.SuperRegion;

public class Plan {
	SuperRegion sr;
	int weight;
	
	public Plan(SuperRegion sr){
		this.sr = sr;
		weight = 0;
	}
	public Plan(SuperRegion sr, int weight){
		this.sr = sr;
		this.weight = weight;
	}
	
	public int getWeight() {
		return weight;
	}
	public void setWeight(int weight) {
		this.weight = weight;
	}
	public void setSr(SuperRegion sr) {
		this.sr = sr;
	}
	public SuperRegion getSr(){
		return sr;
	}

}
