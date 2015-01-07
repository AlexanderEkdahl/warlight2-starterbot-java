package commanders;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;

import concepts.ActionProposal;
import concepts.PlacementProposal;
import bot.BotState;
import bot.Values;
import map.Map;
import map.Region;
import map.SuperRegion;
import move.AttackTransferMove;
import move.PlaceArmiesMove;

public class OffensiveCommander extends TemplateCommander {
	private ArrayList<SuperRegion> ranking;
	private int selfImportance;


//	public ArrayList<AttackTransferMove> Attack(BotState currentState) {
//		ArrayList<AttackTransferMove> attackTransferMoves = new ArrayList<AttackTransferMove>();
//		ArrayList<Region> available = currentState.getVisibleMap()
//				.getOwnedRegions(currentState.getMyPlayerName());
//		// planned main attack
//		if (targetRegion != null && baseOfAttack != null) {
//			attackTransferMoves.add(new AttackTransferMove(currentState
//					.getMyPlayerName(), baseOfAttack, targetRegion,
//					baseOfAttack.getArmies() - 1));
//
//			available.remove(baseOfAttack);
//		}
//
//		// resten anfaller eller f-rflyttas randomly
//
//		for (Region r : available) {
//			if (r.getArmies() > 1) {
//				attackTransferMoves.add(improvisedAction(r, currentState));
//			}
//		}
//		baseOfAttack = null;
//		targetRegion = null;
//		return attackTransferMoves;
//
//	}

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

	@Override
	public ArrayList<PlacementProposal> getPlacementProposals(BotState state) {
		Map currentMap = state.getVisibleMap();

		// if we don't have any super regions, prioritize expansion greatly
		if (currentMap.getOwnedSuperRegions(state.getMyPlayerName()).size() < 1) {
			selfImportance = 10;
		} else {
			selfImportance = 1;
		}
		SuperRegion wantedSuperRegion = calculateWantedSuperRegion(state);
		ArrayList<PlacementProposal> attackPlans = prepareAttack(
				wantedSuperRegion, state);

		return attackPlans;
	}

	private ArrayList<PlacementProposal> prepareAttack(
			SuperRegion wantedSuperRegion, BotState state) {
		ArrayList<PlacementProposal> proposals = new ArrayList<PlacementProposal>();

		HashSet<Region> possibleBases = new HashSet<Region>();
		ArrayList<Region> neighbors;
		for (Region r : state.getVisibleMap().getOwnedRegions(
				state.getMyPlayerName())) {
			neighbors = r.getNeighbors();
			for (Region n : neighbors) {
				if (r.getSuperRegion().equals(wantedSuperRegion)
						&& r.getPlayerName() != state.getMyPlayerName()) {
					possibleBases.add(r);
				}
			}

		}

		// create proposals that are based on the appreciated cost of attacking
		// and the forces available
		for (Region r : possibleBases) {
			proposals.add(new PlacementProposal(selfImportance, r, Values
					.calculateRequiredForcesAttack(state.getMyPlayerName(),
							wantedSuperRegion)
					- r.getArmies()));
		}
		return proposals;
	}

	private SuperRegion calculateWantedSuperRegion(BotState state) {
		SuperRegion cheapest = null;
		int cheapestCost = Integer.MAX_VALUE;
		// find the cheapest where we have presence or neighbors that still
		// gives a reward
		HashSet<SuperRegion> hasPresence = new HashSet<SuperRegion>();
		ArrayList<Region> neighbors;

		for (Region r : state.getVisibleMap().getOwnedRegions(
				state.getMyPlayerName())) {
			neighbors = r.getNeighbors();
			for (Region n : neighbors) {
				hasPresence.add(n.getSuperRegion());
			}
		}
		for (SuperRegion s : hasPresence) {
			if (s.getArmiesReward() > 1) {
				if (Values.calculateRequiredForcesAttack(
						state.getMyPlayerName(), s) < cheapestCost) {
					cheapest = s;
				}
			}

		}
		return cheapest;

	}

	@Override
	public ArrayList<ActionProposal> getActionProposals(BotState state) {
		// TODO Auto-generated method stub
		return null;
	}

}
