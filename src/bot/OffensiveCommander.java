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

	// placera trupper f-r anfall
	public ArrayList<PlaceArmiesMove> Placement(int forces, BotState currentState) {
		EvaluatePriorities();

		LinkedList<Region> tempNeighbors;
		LinkedList<Region> owned = currentState.getFullMap().getOwned();

		// hitta region som tillh-r superregion med h-gst prioritet som vi har
		// tillg-ng till
		int currentBest = Integer.MAX_VALUE;

		for (Region r : owned) {
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
		// alla p- samma tile
		ArrayList<PlaceArmiesMove> placeArmiesMoves = new ArrayList<PlaceArmiesMove>();
		placeArmiesMoves.add(new PlaceArmiesMove(myName, baseOfAttack, forces));
		return placeArmiesMoves;

	}

	public ArrayList<AttackTransferMove> Attack(BotState currentState) {
		ArrayList<AttackTransferMove> attackTransferMoves = new ArrayList<AttackTransferMove>();

		LinkedList<Region> available = currentState.getFullMap().getOwned();
		// huvudattack
		if (targetRegion != null && baseOfAttack != null) {
			attackTransferMoves.add(new AttackTransferMove(myName,
					baseOfAttack, targetRegion, baseOfAttack.getArmies() - 1));

			available.remove(baseOfAttack);
		}


		// resten anfaller eller f-rflyttas
		for (Region r : available) {
			if (r.getArmies() > 1) {
				improvisedAction(r);

			}
		}

		EvaluatePriorities();

		baseOfAttack = null;
		targetRegion = null;
		return attackTransferMoves;

	}

	private AttackTransferMove improvisedAction(Region r) {
		LinkedList<Region> tempNeighbors = r.getNeighbors();
		for (Region n : tempNeighbors) {
			if (!n.ownedByPlayer(myName)) {
				return (new AttackTransferMove(myName, r, n, r.getArmies() - 1));

			}
		}

		// fanns ingen att anfalla, f-rflytta ist till n-n position

		return (new AttackTransferMove(myName, r, r.getNeighbors().get(0),
				r.getArmies() - 1));

	}

	private void EvaluatePriorities() {
		// lol im just a dumb computer pls help me think

	}

}
