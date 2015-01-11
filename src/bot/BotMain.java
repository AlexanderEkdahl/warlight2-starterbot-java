package bot;

import java.util.ArrayList;
import java.util.Collections;

import commanders.*;
import concepts.ActionProposal;
import concepts.PlacementProposal;
import map.Region;
import move.AttackTransferMove;
import move.PlaceArmiesMove;

public class BotMain implements Bot {
	private OffensiveCommander oc;
	private DefensiveCommander dc;
	private GriefCommander gc;

	public static void main(String[] args) {
		new BotParser(new BotMain()).run();
	}

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
		while (armiesLeft > 0) {
			if (currentProposalnr >= proposals.size()) {
				orders.add(new PlaceArmiesMove(state.getMyPlayerName(), state
						.getFullMap().getOwnedRegions(state.getMyPlayerName())
						.get(0), armiesLeft));
				return orders;
			}
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

		ArrayList<Region> availableRegions = state.getFullMap()
				.getOwnedRegions(state.getMyPlayerName());

		AttackSatisfaction as = new AttackSatisfaction(state, state.getFullMap().getSuperRegions());
		proposals.addAll(oc.getActionProposals(state), as);
		proposals.addAll(gc.getActionProposals(state), as);

		Collections.sort(proposals);

		int currentProposalnr = 0;
		ActionProposal currentProposal;

		while (availableRegions.size() > 0
				&& currentProposalnr < proposals.size()) {
			currentProposal = proposals.get(currentProposalnr);
			System.err.println(currentProposal.toString());
			orders.add(new AttackTransferMove(state.getMyPlayerName(),
					currentProposal.getOrigin(), currentProposal.getTarget(),
					currentProposal.getForces()));
			availableRegions.remove(currentProposal.getOrigin());
			currentProposalnr++;
		}

		return orders;
	}
}
