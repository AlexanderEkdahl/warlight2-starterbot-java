package commanders;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import bot.BotState;
import bot.Values;
import map.SuperRegion;

public class AttackSatisfaction {
	private HashMap<SuperRegion, Integer> roomLeft;
	
	public AttackSatisfaction(BotState state, ArrayList<SuperRegion> superRegions){
		  roomLeft = new HashMap<SuperRegion, Integer>();
		  for (SuperRegion s : superRegions){
			  roomLeft.put(s, Values.calculateRequiredForcesAttack(state.getMyPlayerName(), s));
		  }
	}

	public HashMap<SuperRegion, Integer> getRoomLeft() {
		return roomLeft;
	}

	public void setRoomLeft(HashMap<SuperRegion, Integer> roomLeft) {
		this.roomLeft = roomLeft;
	}

}
