package bot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Set;

import commanders.*;
import concepts.ActionProposal;
import concepts.FromTo;
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
		proposals.addAll(dc.getPlacementProposals(state));
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
				System.err.println(currentProposal.toString());
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
		HashMap<Region, Integer> attacking = new HashMap<Region, Integer>();
		// ArrayList<Region> available = state.getFullMap().getOwnedRegions(
		// state.getMyPlayerName());
		HashMap<Region, Integer> available = new HashMap<Region, Integer>();
		for (Region r : state.getFullMap().getOwnedRegions(
				state.getMyPlayerName())) {
			available.put(r, r.getArmies() - 1);
		}
		ArrayList<ActionProposal> backUpProposals = new ArrayList<ActionProposal>();
		HashMap<SuperRegion, Integer> superRegionSatisfied = Values
				.calculateSuperRegionSatisfaction(state);
		HashMap<Region, Integer> regionSatisfied = Values
				.calculateRegionSatisfaction(state);
		HashMap<Region, Integer> awaitingBackup = new HashMap<Region, Integer>();
		HashMap<FromTo, Integer> decisions = new HashMap<FromTo, Integer>();
		proposals.addAll(oc.getActionProposals(state));
		proposals.addAll(gc.getActionProposals(state));
		proposals.addAll(dc.getActionProposals(state));
		
		String mName = state.getMyPlayerName();
		String eName = state.getOpponentPlayerName();

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

			if (available.get(currentOriginRegion) > 0 && required > 0) {

				int disposed = Math.min(required,
						available.get(currentOriginRegion));

					FromTo currentMove = new FromTo(currentOriginRegion,
							currentTargetRegion);
					if (decisions.get(currentMove) == null) {
						decisions.put(currentMove, disposed);
					} else {
						decisions.put(currentMove, decisions.get(currentMove)
								+ disposed);
					}
					if (!currentTargetRegion.getPlayerName().equals(mName)){
						if (attacking.get(currentTargetRegion) == null){
							attacking.put(currentTargetRegion, disposed);
						}
						else{
							attacking.put(currentTargetRegion, attacking.get(currentTargetRegion) + disposed);
						}
					}

					superRegionSatisfied.put(currentTargetSuperRegion,
							superRegionSatisfied.get(currentTargetSuperRegion)
									- disposed);
					regionSatisfied.put(currentFinalTargetRegion,
							regionSatisfied.get(currentFinalTargetRegion)
									- disposed);
					System.err.println(currentProposal.toString());
					available.put(currentOriginRegion,
							available.get(currentOriginRegion) - disposed);
				}

			

		}
		for (int i = 0; i < backUpProposals.size(); i++) {
			
			ActionProposal currentProposal = backUpProposals.get(i);
			
			Region currentOriginRegion = currentProposal.getOrigin();
			Region currentTargetRegion = currentProposal.getTarget();
			int required = currentProposal.getForces();
			if (available.get(currentOriginRegion) > 0 && required > 0) {
				int disposed = Math.min(required,
						available.get(currentOriginRegion));

					FromTo currentMove = new FromTo(currentOriginRegion,
							currentTargetRegion);
					System.err.println("Backup Proposal: "+ currentProposal.toString());
					if (decisions.get(currentMove) == null) {
						decisions.put(currentMove, disposed);
					} else {
						decisions.put(currentMove, decisions.get(currentMove)
								+ disposed);
					}
					if (!currentTargetRegion.getPlayerName().equals(mName)){
						if (attacking.get(currentTargetRegion) == null){
							attacking.put(currentTargetRegion, disposed);
						}
						else{
							attacking.put(currentTargetRegion, attacking.get(currentTargetRegion) + disposed);
						}
					}
					available.put(currentOriginRegion,
							available.get(currentOriginRegion) - disposed);
				
			}
		}
		
		Set<Region> aKeys = attacking.keySet();
		ArrayList<Region> badAttacks = new ArrayList<Region>();
		for (Region r : aKeys){
			if (Values.calculateRequiredForcesAttack(mName, r) > attacking.get(r)){
				badAttacks.add(r);
			}
		}

		Set<FromTo> keys = decisions.keySet();
		
		for (FromTo f : keys) {
			if (!badAttacks.contains(f.getR2())){
				orders.add(new AttackTransferMove(state.getMyPlayerName(), f
						.getR1(), f.getR2(), decisions.get(f)));
			}
			
		}

		return orders;
	}
}
