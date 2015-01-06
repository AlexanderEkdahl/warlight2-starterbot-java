package bot;

import java.util.ArrayList;
import java.util.LinkedList;

import map.Map;
import map.Region;
import map.SuperRegion;
import move.PlaceArmiesMove;

public class OffensiveCommander {
	
	private LinkedList<SuperRegion> ranking;
	private Region baseOfAttack;
	private SuperRegion targetSuperRegion;
	private Region targetRegion;
	private String myName;
	
	
	
	public OffensiveCommander(BotState currentState){
		this.myName = currentState.getMyPlayerName();
		
		// totalt slumpad rank atm
		ranking = currentState.getFullMap().getSuperRegions();
	}
	
	
	// placera trupper för anfall
	public ArrayList<PlaceArmiesMove> Placement(int forces, Map currentMap){
		EvaluatePriorities();
		
		LinkedList<Region> tempNeighbors;
		LinkedList<Region> owned = currentMap.getOwned();
		
		//hitta region som tillhör superregion med högst prioritet som vi har tillgång till
		
		for (int i = 0; i<ranking.size(); i++){
			for (Region r : owned){
				tempNeighbors = r.getNeighbors();
				for (Region n: tempNeighbors){
					if (n.getSuperRegion() == ranking.get(i) && !n.ownedByPlayer(myName)){
						baseOfAttack = r;
						targetRegion = n;
					};
				}
			}
			
			
		}
		
		
		// alla ska med
		ArrayList<PlaceArmiesMove> placeArmiesMoves = new ArrayList<PlaceArmiesMove>();
		
			placeArmiesMoves
			.add(new PlaceArmiesMove(myName, baseOfAttack, forces));
			
		return placeArmiesMoves;
		
		
		
	}
	
	
	public void Attack(){
		if (targetRegion != null){
			
		}
		EvaluatePriorities();
		
		
		
		
		
	}
	
	
	private void EvaluatePriorities() {
		// lol im just a dumb computer pls help me think
		
	}

	

}
