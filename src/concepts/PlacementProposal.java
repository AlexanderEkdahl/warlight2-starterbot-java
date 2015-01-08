package concepts;

import bot.BotState;
import map.Region;
import move.PlaceArmiesMove;

public class PlacementProposal extends TemplateProposal{

	public PlacementProposal(float weight, Region region, int requiredForces, Plan plan) {
		super(weight, region, requiredForces, plan);
	}

	public float getWeight() {
		return weight;
	}

	public void setWeight(int weight) {
		this.weight = weight;
	}

	public Region getTarget() {
		return target;
	}

	public void setTarget(Region target) {
		this.target = target;
	}

	public int getdForces() {
		return forces;
	}

	public void setf(int forces) {
		this.forces = forces;
	}

}
