package bot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

import commanders.*;
import concepts.ActionProposal;
import concepts.PlacementProposal;
import map.Region;
import map.SuperRegion;
import move.AttackTransferMove;
import move.PlaceArmiesMove;

public class BotMain implements Bot {
	private OffensiveCommander oc;
	private DefensiveCommander dc;
	private GriefCommander gc;
	private RandomCommander rc;

	public BotMain() {
		oc = new OffensiveCommander();
		dc = new DefensiveCommander();
		gc = new GriefCommander();
		rc = new RandomCommander();
	}

	public Region getStartingRegion(BotState state, Long timeOut) {
		Region startPosition = Values.getBestStartRegion(state
				.getPickableStartingRegions());
		return startPosition;
	}

	// right now it just takes the highest priority tasks and executes them
	public ArrayList<PlaceArmiesMove> getPlaceArmiesMoves(BotState state,
			Long timeOut) {

		ArrayList<PlaceArmiesMove> orders = new ArrayList<PlaceArmiesMove>();

		int armiesLeft = state.getStartingArmies();

		// TODO decide how to merge proposals
		ArrayList<PlacementProposal> proposals = new ArrayList<PlacementProposal>();
		proposals.addAll(oc.getPlacementProposals(state));
		proposals.addAll(gc.getPlacementProposals(state));
		proposals.addAll(rc.getPlacementProposals(state));
		Collections.sort(proposals);
		int currentProposalnr = 0;
		PlacementProposal currentProposal;
		while (armiesLeft > 0 && currentProposalnr < proposals.size()) {
			currentProposal = proposals.get(currentProposalnr);
			System.err.println(currentProposal.toString());
			if (currentProposal.getForces() > armiesLeft) {
				orders.add(new PlaceArmiesMove(state.getMyPlayerName(),
						currentProposal.getTarget(), armiesLeft));
				armiesLeft = 0;
			}

			else {
				orders.add(new PlaceArmiesMove(state.getMyPlayerName(),
						currentProposal.getTarget(), currentProposal
								.getForces()));

				armiesLeft -= currentProposal.getForces();
			}
			currentProposalnr++;

		}
		if (armiesLeft > 0) {
			orders.add(new PlaceArmiesMove(state.getMyPlayerName(), state
					.getFullMap().getOwnedRegions(state.getMyPlayerName())
					.get(0), armiesLeft));

		}

		return orders;
	}

	public ArrayList<AttackTransferMove> getAttackTransferMoves(BotState state,
			Long timeOut) {
		ArrayList<AttackTransferMove> orders = new ArrayList<AttackTransferMove>();
		ArrayList<ActionProposal> proposals = new ArrayList<ActionProposal>();

		// HashMap<Region, Integer> available = calculateAvailable(state);

		ArrayList<Region> available = state.getFullMap().getOwnedRegions(
				state.getMyPlayerName());
		// HashMap<Region, Integer> regionSatisfied =
		// calculateRegionSatisfaction();
		proposals.addAll(oc.getActionProposals(state));
		proposals.addAll(gc.getActionProposals(state));

		Collections.sort(proposals);

		int currentProposalnr = 0;
		while (currentProposalnr < proposals.size()) {

			ActionProposal currentProposal = proposals.get(currentProposalnr);
			Region currentOriginRegion = currentProposal.getOrigin();
			Region currentTargetRegion = currentProposal.getTarget();
			int required = currentProposal.getForces();

			// if (superRegionSatisfied.get(currentTargetSuperRegion) < 1){
			// continue;
			// }

			if (available.contains(currentOriginRegion)) {

				if (Values.calculateRequiredForcesAttack(
						state.getMyPlayerName(), currentTargetRegion) < required) {
					// doublecheck that it isn't a stupid attack
					orders.add(new AttackTransferMove(state.getMyPlayerName(),
							currentOriginRegion, currentTargetRegion, required));

					System.err.println(currentProposal.toString());
				}
				available.remove(currentOriginRegion);

			}

			currentProposalnr++;
		}

		return orders;
	}

}
