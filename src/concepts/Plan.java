package concepts;

import bot.BotState;
import map.Region;
import map.SuperRegion;

public class Plan {
	private ActionType actionType;
	
	
	
	public ActionType getActionType() {
		return actionType;
	}

	public void setActionType(ActionType aType) {
		this.actionType = aType;
	}

	private SuperRegion sr;
	private Region r;

	public Plan(Region r, SuperRegion sr) {
		this.sr = sr;
		this.r = r;
		this.actionType = r.getPlayerName().equals(BotState.getMyName()) ?  ActionType.DEFEND : ActionType.ATTACK;
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
	
//	public String toString(){
//		return ("Region: " + r + " SuperRegion: " + sr);
//	}
 


}
