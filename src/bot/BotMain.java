package bot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

import commanders.*;
import concepts.ActionProposal;
import concepts.PlacementProposal;
import concepts.Plan;
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

		for (PlaceArmiesMove p : orders) {
			Region r = p.getRegion();
			r.setArmies(r.getArmies() + p.getArmies());
		}

		return orders;
	}

	public ArrayList<AttackTransferMove> getAttackTransferMoves(BotState state,
			Long timeOut) {
		ArrayList<AttackTransferMove> orders = new ArrayList<AttackTransferMove>();
		ArrayList<ActionProposal> proposals = new ArrayList<ActionProposal>();
		ArrayList<Region> available = state.getFullMap().getOwnedRegions(
				state.getMyPlayerName());
		ArrayList<ActionProposal> backUpProposals = new ArrayList<ActionProposal>();
		HashMap<SuperRegion, Integer> superRegionSatisfied = Values
				.calculateSuperRegionSatisfaction(state);
		HashMap<Region, Integer> regionSatisfied = Values
				.calculateRegionSatisfaction(state);
		proposals.addAll(oc.getActionProposals(state));
		proposals.addAll(gc.getActionProposals(state));

		Collections.sort(proposals);

		for (int i = 0; i < proposals.size(); i++) {
			ActionProposal currentProposal = proposals.get(i);
			Region currentOriginRegion = currentProposal.getOrigin();
			Region currentTargetRegion = currentProposal.getTarget();
			Plan currentPlan = currentProposal.getPlan();
			SuperRegion currentTargetSuperRegion = currentPlan.getSr();
			Region currentFinalTargetRegion = currentPlan.getR();
			int required = currentProposal.getForces();

			if (superRegionSatisfied.get(currentTargetSuperRegion) < 1) {
				backUpProposals.add(currentProposal);
				continue;
			}
			if (regionSatisfied.get(currentFinalTargetRegion) < 1) {
				backUpProposals.add(currentProposal);
				continue;
			}

			if (available.contains(currentOriginRegion)) {

				if (Values.calculateRequiredForcesAttack(
						state.getMyPlayerName(), currentTargetRegion) < required) {
					// doublecheck that it isn't a stupid attack
					orders.add(new AttackTransferMove(state.getMyPlayerName(),
							currentOriginRegion, currentTargetRegion, required));
					superRegionSatisfied.put(currentTargetSuperRegion,
							superRegionSatisfied.get(currentTargetSuperRegion)
									- required);
					regionSatisfied
							.put(currentTargetRegion,
									regionSatisfied.get(currentTargetRegion)
											- required);

					System.err.println(currentProposal.toString());
				}
				available.remove(currentOriginRegion);

			}

		}
		for (int i = 0; i < backUpProposals.size(); i++) {

			ActionProposal currentProposal = backUpProposals.get(i);
			Region currentOriginRegion = currentProposal.getOrigin();
			Region currentTargetRegion = currentProposal.getTarget();
			int required = currentProposal.getForces();
			if (available.contains(currentOriginRegion)) {
				if (Values.calculateRequiredForcesAttack(
						state.getMyPlayerName(), currentTargetRegion) < required) {
					orders.add(new AttackTransferMove(state.getMyPlayerName(),
							currentOriginRegion, currentTargetRegion, required));
					available.remove(currentOriginRegion);
				}
			}
		}

		return orders;
	}

}
