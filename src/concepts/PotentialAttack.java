package concepts;

import map.Region;

public class PotentialAttack {
	private FromTo fromTo;
	private int forces;
	
	public PotentialAttack(Region from, Region to, int forces) {
		
		this.fromTo = new FromTo(from.getId(), to.getId());
		this.forces = forces;
	}
	public Integer getFrom() {
		return fromTo.getR1();
	}
	public Integer getTo(){
		return fromTo.getR2();
	}

	public int getForces() {
		return forces;
	}
	public void setForces(int forces) {
		this.forces = forces;
	}
	

}
