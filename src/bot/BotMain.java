/**
 * Warlight AI Game Bot
 *
 * Last update: April 02, 2014
 *
 * @author Jim van Eeden
 * @version 1.0
 * @License MIT License (http://opensource.org/Licenses/MIT)
 */

package bot;

/**
 * This is a simple bot that does random (but correct) moves.
 * This class implements the Bot interface and overrides its Move methods.
 * You can implement these methods yourself very easily now,
 * since you can retrieve all information about the match from variable â€œstateâ€�.
 * When the bot decided on the move to make, it returns an ArrayList of Moves.
 * The bot is started by creating a Parser to which you add
 * a new instance of your bot, and then the parser is started.
 */

import java.util.ArrayList;
import java.util.Collections;

import commanders.DefensiveCommander;
import commanders.GriefCommander;
import commanders.OffensiveCommander;
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
		BotParser parser = new BotParser(new BotMain(), System.in);
		parser.run();
	}

	public BotMain() {
		oc = new OffensiveCommander();
		dc = new DefensiveCommander();
		gc = new GriefCommander();

	}

	@Override
	/**
	 * A method that returns which region the bot would like to start on, the pickable regions are stored in the BotState.
	 * The bots are asked in turn (ABBAABBAAB) where they would like to start and return a single region each time they are asked.
	 * This method returns the smallest super region
	 */
	public Region getStartingRegion(BotState state, Long timeOut) {
		Region startPosition;
		startPosition = Values.getBestStartRegion(state
				.getPickableStartingRegions());
		return startPosition;
	}

	@Override
	/**
	 * This method is called for at first part of each round.
	 * @return The list of PlaceArmiesMoves for one round
	 */
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
		System.err.println("Round: " + state.getRoundNumber() + " PlaceArmiesMoves");
		while (armiesLeft > 0) {
			if (currentProposalnr >= proposals.size()) {
				orders.add(new PlaceArmiesMove(state.getMyPlayerName(), state
						.getVisibleMap()
						.getOwnedRegions(state.getMyPlayerName()).get(0),
						armiesLeft));
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

	@Override
	/**
	 * This method is called for at the second part of each round. This example attacks if a region has
	 * more than 6 armies on it, and transfers if it has less than 6 and a neighboring owned region.
	 * @return The list of PlaceArmiesMoves for one round
	 */
	public ArrayList<AttackTransferMove> getAttackTransferMoves(BotState state,
			Long timeOut) {
		ArrayList<AttackTransferMove> orders = new ArrayList<AttackTransferMove>();
		ArrayList<ActionProposal> proposals = new ArrayList<ActionProposal>();

		ArrayList<Region> availableRegions = state.getVisibleMap()
				.getOwnedRegions(state.getMyPlayerName());

		proposals.addAll(oc.getActionProposals(state));
		proposals.addAll(gc.getActionProposals(state));

		Collections.sort(proposals);

		int currentProposalnr = 0;
		ActionProposal currentProposal;

		System.err.println("Round: " + state.getRoundNumber() + " AttackTransferMove");
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
