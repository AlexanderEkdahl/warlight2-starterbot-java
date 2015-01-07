package commanders;

import java.util.ArrayList;
import java.util.LinkedList;

import concepts.ActionProposal;
import concepts.PlacementProposal;
import bot.BotState;
import map.Map;
import map.Region;
import map.SuperRegion;
import move.AttackTransferMove;
import move.PlaceArmiesMove;

public class OffensiveCommander extends TemplateCommander{
	private ArrayList<SuperRegion> ranking;
	private Region baseOfAttack;
	private Region targetRegion;


	// placera trupper f-r anfall
	public ArrayList<PlaceArmiesMove> Placement(int forces,
			BotState currentState) {
		EvaluatePriorities(currentState);

		ArrayList<Region> tempNeighbors;
		ArrayList<Region> owned = currentState.getVisibleMap().getOwned(currentState.getMyPlayerName());

		// hitta region som tillh-r superregion med h-gst prioritet som vi har
		// tillg-ng till
		int currentBest = Integer.MAX_VALUE;

		for (Region r : owned) {
			tempNeighbors = r.getNeighbors();
			for (Region n : tempNeighbors) {
				if (ranking.indexOf(n.getSuperRegion()) < currentBest
						&& !n.getPlayerName().equals(currentState.getMyPlayerName())) {
					currentBest = ranking.indexOf(n.getSuperRegion());
					baseOfAttack = r;
					targetRegion = n;
				}

			}

		}
		// alla p- samma tile
		ArrayList<PlaceArmiesMove> placeArmiesMoves = new ArrayList<PlaceArmiesMove>();
		placeArmiesMoves.add(new PlaceArmiesMove(
				currentState.getMyPlayerName(), baseOfAttack, forces));
		return placeArmiesMoves;

	}

	public ArrayList<AttackTransferMove> Attack(BotState currentState) {
		EvaluatePriorities(currentState);
		
		ArrayList<AttackTransferMove> attackTransferMoves = new ArrayList<AttackTransferMove>();
		ArrayList<Region> available = currentState.getVisibleMap().getOwned(
				currentState.getMyPlayerName());
		// planned main attack
		if (targetRegion != null && baseOfAttack != null) {
			attackTransferMoves.add(new AttackTransferMove(currentState
					.getMyPlayerName(), baseOfAttack, targetRegion,
					baseOfAttack.getArmies() - 1));

			available.remove(baseOfAttack);
		}

		// resten anfaller eller f-rflyttas randomly

		for (Region r : available) {
			if (r.getArmies() > 1) {
				attackTransferMoves.add(improvisedAction(r, currentState));
			}
		}

		

		baseOfAttack = null;
		targetRegion = null;
		return attackTransferMoves;

	}

	private AttackTransferMove improvisedAction(Region r, BotState currentState) {
		ArrayList<Region> tempNeighbors = r.getNeighbors();
		for (Region n : tempNeighbors) {
			if (!n.getPlayerName().equals(currentState.getMyPlayerName())) {
				return (new AttackTransferMove(currentState.getMyPlayerName(),
						r, n, r.getArmies() - 1));

			}
		}

		// nobody to attack

		return (new AttackTransferMove(currentState.getMyPlayerName(), r, r
				.getNeighbors().get(0), r.getArmies() - 1));

	}

	public void setPrioritySuperRegion(SuperRegion sr) {
		ranking.remove(sr);
		ranking.add(0, sr);
	}

	private void EvaluatePriorities(BotState CurrentState) {
		// lol im just a dumb computer pls help me think
		
		

	}

	@Override
	public ArrayList<PlacementProposal> getPlacementProposals(BotState state) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ArrayList<ActionProposal> getActionProposals(BotState state) {
		// TODO Auto-generated method stub
		return null;
	}

}
