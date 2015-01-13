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

	public BotMain() {
		oc = new OffensiveCommander();
		dc = new DefensiveCommander();
		gc = new GriefCommander();
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
		HashMap<SuperRegion, Integer> superRegionSatisfied = calculateSuperRegionSatisfaction(state);
		proposals.addAll(oc.getActionProposals(state));
		proposals.addAll(gc.getActionProposals(state));

		Collections.sort(proposals);

		int currentProposalnr = 0;
		while (currentProposalnr < proposals.size()) {
			ActionProposal currentProposal = proposals.get(currentProposalnr);
			Region currentOriginRegion = currentProposal.getOrigin();
			Region currentTargetRegion = currentProposal.getTarget();
			SuperRegion currentTargetSuperRegion = currentProposal.getTarget()
					.getSuperRegion();
			int required = currentProposal.getForces();

			if (available.contains(currentOriginRegion)
					&& superRegionSatisfied.get(currentTargetSuperRegion) > 0) {
				int roomLeft = superRegionSatisfied
						.get(currentTargetSuperRegion);
				int disposed = Math.min(roomLeft, required);

				if (Values.calculateRequiredForcesAttack(
						state.getMyPlayerName(), currentTargetRegion) < disposed) {
					// doublecheck that it isn't a stupid attack
					orders.add(new AttackTransferMove(state.getMyPlayerName(),
							currentOriginRegion, currentTargetRegion, disposed));
					available.remove(currentOriginRegion);
					superRegionSatisfied.put(currentTargetSuperRegion,
							superRegionSatisfied.get(currentTargetSuperRegion)
									- disposed);
					System.err.println(currentProposal.toString());
				}

			}

			currentProposalnr++;
		}

		return orders;
	}

	private HashMap<Region, Integer> calculateAvailable(BotState state) {
		HashMap<Region, Integer> available = new HashMap<Region, Integer>();

		for (Region r : state.getFullMap().getOwnedRegions(
				state.getMyPlayerName())) {
			available.put(r, r.getArmies() - 1);

		}
		return available;
	}

	private HashMap<SuperRegion, Integer> calculateSuperRegionSatisfaction(
			BotState state) {
		HashMap<SuperRegion, Integer> roomLeft = new HashMap<SuperRegion, Integer>();
		for (SuperRegion s : state.getFullMap().getSuperRegions()) {
			roomLeft.put(
					s,
					(int) (Values.calculateRequiredForcesAttack(
							state.getMyPlayerName(), s) * 1.5));
		}
		return roomLeft;
	}
}
