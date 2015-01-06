package bot;

import java.util.ArrayList;
import java.util.LinkedList;

import map.Map;
import map.Region;
import map.SuperRegion;
import move.AttackTransferMove;
import move.PlaceArmiesMove;

public class OffensiveCommander {

	private LinkedList<SuperRegion> ranking;
	private Region baseOfAttack;
	private Region targetRegion;
	private String myName;

	public OffensiveCommander(BotState currentState) {
		this.myName = currentState.getMyPlayerName();

		// totalt slumpad rank atm
		ranking = currentState.getFullMap().getSuperRegions();
	}

	// placera trupper f�r anfall
	public ArrayList<PlaceArmiesMove> Placement(int forces, Map currentMap) {
		EvaluatePriorities();

		LinkedList<Region> tempNeighbors;
		LinkedList<Region> owned = currentMap.getOwned();

		// hitta region som tillh�r superregion med h�gst prioritet som vi har
		// tillg�ng till
		int currentBest = Integer.MAX_VALUE;

		outer: for (Region r : owned) {
			tempNeighbors = r.getNeighbors();
			for (Region n : tempNeighbors) {
				if (ranking.indexOf(n.getSuperRegion()) < currentBest
						&& !n.ownedByPlayer(myName)) {
					currentBest = ranking.indexOf(n.getSuperRegion());
					baseOfAttack = r;
					targetRegion = n;
				}

			}

		}

		// alla p� samma tile
		ArrayList<PlaceArmiesMove> placeArmiesMoves = new ArrayList<PlaceArmiesMove>();
		placeArmiesMoves.add(new PlaceArmiesMove(myName, baseOfAttack, forces));
		return placeArmiesMoves;

	}

	public ArrayList<AttackTransferMove> Attack() {
		ArrayList<AttackTransferMove> attackTransferMoves = new ArrayList<AttackTransferMove>();

		// full fart
		if (targetRegion != null && baseOfAttack != null) {
			attackTransferMoves.add(new AttackTransferMove(myName,
					baseOfAttack, targetRegion, baseOfAttack.getArmies() - 1));

		} else {
			// finns ingen attack planerad

		}
		EvaluatePriorities();

		baseOfAttack = null;
		targetRegion = null;
		return attackTransferMoves;

	}

	private void EvaluatePriorities() {
		// lol im just a dumb computer pls help me think

	}

}
