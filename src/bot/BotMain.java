package bot;

import imaginary.EnemyAppreciator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Set;

import commanders.*;
import concepts.ActionProposal;
import concepts.ActionType;
import concepts.FromTo;
import concepts.PlacementProposal;
import concepts.Plan;
import concepts.PotentialAttack;
import map.Map;
import map.Region;
import map.SuperRegion;
import move.AttackTransferMove;
import move.PlaceArmiesMove;

public class BotMain implements Bot {
	private OffensiveCommander oc;
	private DefensiveCommander dc;
	private GriefCommander gc;
	private ArrayList<PlaceArmiesMove> placeOrders;
	private ArrayList<AttackTransferMove> moveOrders;

	public BotMain() {
		oc = new OffensiveCommander();
		dc = new DefensiveCommander();
		gc = new GriefCommander();

	}

	public Region getStartingRegion(BotState state, Long timeOut) {
		Region startPosition = Values.getBestStartRegion(state.getPickableStartingRegions(), state.getFullMap());
		return startPosition;
	}

	// right now it just takes the highest priority tasks and executes them
	public ArrayList<PlaceArmiesMove> getPlaceArmiesMoves(BotState state, Long timeOut) {

		EnemyAppreciator appreciator = state.getFullMap().getAppreciator();
		appreciator.setMap(state.getFullMap().duplicate());
		Map speculativeMap = appreciator.getSpeculativeMap();

		ArrayList<ActionProposal> proposals = new ArrayList<ActionProposal>();
		proposals.addAll(oc.getActionProposals(speculativeMap));
		proposals.addAll(gc.getActionProposals(speculativeMap));
		proposals.addAll(dc.getActionProposals(speculativeMap));
		Collections.sort(proposals);

		// where the magic happens
		generateOrders(speculativeMap, state.getStartingArmies());

		return placeOrders;
	}

	public ArrayList<AttackTransferMove> getAttackTransferMoves(BotState state, Long timeOut) {
		return moveOrders;

	}

	private void generateOrders(Map map, int armiesLeft) {
		HashMap<Region, Integer> attacking = new HashMap<Region, Integer>();
		placeOrders = new ArrayList<PlaceArmiesMove>();
		moveOrders = new ArrayList<AttackTransferMove>();

		ArrayList<ActionProposal> backUpProposals = new ArrayList<ActionProposal>();
		HashMap<SuperRegion, Integer> superRegionSatisfied = Values.calculateSuperRegionSatisfaction(map);
		HashMap<Region, Integer> regionSatisfied = Values.calculateRegionSatisfaction(map);
		HashMap<FromTo, Integer> decisions = new HashMap<FromTo, Integer>();
		HashMap<Region, Boolean> hasOnlyOnesAttacking = new HashMap<Region, Boolean>();
		ArrayList<PotentialAttack> potentialAttacks = new ArrayList<PotentialAttack>();

		HashMap<Region, Integer> available = new HashMap<Region, Integer>();
		for (Region r : map.getOwnedRegions(BotState.getMyName())) {
			available.put(r, r.getArmies() - 1);
		}
		HashMap<Region, Integer> availablePotential = new HashMap<Region, Integer>();

		// TODO decide how to merge proposals
		ArrayList<ActionProposal> proposals = new ArrayList<ActionProposal>();
		proposals.addAll(oc.getActionProposals(map));
		proposals.addAll(gc.getActionProposals(map));
		proposals.addAll(dc.getActionProposals(map));

		Collections.sort(proposals);

		for (int i = 0; i < proposals.size(); i++) {
			ActionProposal currentProposal = proposals.get(i);
			Region currentOriginRegion = currentProposal.getOrigin();
			Region currentTargetRegion = currentProposal.getTarget();
			Plan currentPlan = currentProposal.getPlan();
			SuperRegion currentTargetSuperRegion = currentPlan.getSr();
			Region currentFinalTargetRegion = currentPlan.getR();
			int required = currentProposal.getForces();

			// check for satisfaction
			if (alreadySatisfied(currentFinalTargetRegion, currentTargetSuperRegion, regionSatisfied, superRegionSatisfied)) {
				backUpProposals.add(currentProposal);
				continue;
			}

			// decision has been made to go forward with the proposal
			if ((available.get(currentOriginRegion) > 0 || armiesLeft > 0) && required > 0) {
				int disposed;
				int wanted = Math.min(required, regionSatisfied.get(currentFinalTargetRegion));
				if (available.get(currentOriginRegion) < wanted) {
					int initiallyAvailable = available.get(currentOriginRegion);
					int placed = Math.min(wanted - initiallyAvailable, armiesLeft);
					disposed = initiallyAvailable + placed;
					if (placed > 0) {
						placeOrders.add(new PlaceArmiesMove(BotState.getMyName(), currentOriginRegion, placed));
						System.err.println("Placed " + placed + " at " + currentOriginRegion);

					}
					available.put(currentOriginRegion, disposed);
					armiesLeft -= placed;
				} else {
					disposed = Math.min(wanted, available.get(currentOriginRegion));
				}

				// attack is the best defence
				if (currentProposal.getPlan().getActionType().equals(ActionType.DEFEND) && currentProposal.getOrigin().equals(currentProposal.getTarget())) {
					addPotentialAttacks(potentialAttacks, currentOriginRegion, available, availablePotential);
					System.err.println(currentProposal.toString());
					continue;
				}

				FromTo currentMove = new FromTo(currentOriginRegion, currentTargetRegion);
				addMove(currentMove, decisions, disposed);

				// add satisfaction
				addSatisfaction(currentTargetRegion, currentFinalTargetRegion, disposed, regionSatisfied, superRegionSatisfied);
				System.err.println(currentProposal.toString());
				available.put(currentOriginRegion, available.get(currentOriginRegion) - disposed);

				if (!currentTargetRegion.getPlayerName().equals(BotState.getMyName())) {
					addAttacking(currentTargetRegion, attacking, disposed, hasOnlyOnesAttacking);
					// since this is an attack we will
					// search for potential attacks to help
					if (alreadySatisfied(currentFinalTargetRegion, currentTargetSuperRegion, regionSatisfied, superRegionSatisfied)) {
						continue;
					}
					usePotentialAttacks(availablePotential, potentialAttacks, currentTargetRegion, attacking, decisions, hasOnlyOnesAttacking);

				}

			}

		}

		// backup proposals are moves that are ordered towards already satisfied
		// areas
		for (int i = 0; i < backUpProposals.size(); i++) {
			ActionProposal currentProposal = backUpProposals.get(i);

			Region currentOriginRegion = currentProposal.getOrigin();
			Region currentTargetRegion = currentProposal.getTarget();
			int required = currentProposal.getForces();
			if (available.get(currentOriginRegion) > 0 && required > 0) {
				int disposed = Math.min(required, available.get(currentOriginRegion));
				FromTo currentMove = new FromTo(currentOriginRegion, currentTargetRegion);
				System.err.println("Backup Proposal: " + currentProposal.toString());
				addMove(currentMove, decisions, disposed);
				available.put(currentOriginRegion, available.get(currentOriginRegion) - disposed);

				if (!currentTargetRegion.getPlayerName().equals(BotState.getMyName())) {
					addAttacking(currentTargetRegion, attacking, disposed, hasOnlyOnesAttacking);
					// since this is an attack we will
					// search for potential attacks to help
					usePotentialAttacks(availablePotential, potentialAttacks, currentTargetRegion, attacking, decisions, hasOnlyOnesAttacking);
				}

			}
		}

		// add leftover potentialattacks to the pile of attacks

		for (PotentialAttack p : potentialAttacks) {
			int disposed = Math.min(Math.min(availablePotential.get(p.getFrom()), p.getFrom().getArmies() - 1), p.getForces());
			addAttacking(p.getTo(), attacking, disposed, hasOnlyOnesAttacking);
			FromTo currentMove = new FromTo(p.getFrom(), p.getTo());
			addMove(currentMove, decisions, disposed);
			availablePotential.put(p.getFrom(), availablePotential.get(p.getFrom()) - disposed);
		}

		// exclude bad attacks from moves
		Set<Region> aKeys = attacking.keySet();
		ArrayList<Region> badAttacks = new ArrayList<Region>();
		for (Region r : aKeys) {
			if (!r.getPlayerName().equals(BotState.getMyName())) {
				if (hasOnlyOnesAttacking.get(r)) {
					badAttacks.add(r);
				} else if (!r.getPlayerName().equals(BotState.getMyName()) && Values.calculateRequiredForcesAttack(r) > attacking.get(r)) {
					badAttacks.add(r);
				}
			}
		}

		Set<FromTo> keys = decisions.keySet();

		for (FromTo f : keys) {
			if (!badAttacks.contains(f.getR2())) {
				if (decisions.get(f) == 1) {
					moveOrders.add(0, new AttackTransferMove(BotState.getMyName(), f.getR1(), f.getR2(), decisions.get(f)));
				} else {
					moveOrders.add(new AttackTransferMove(BotState.getMyName(), f.getR1(), f.getR2(), decisions.get(f)));
				}
			}

		}

		if (armiesLeft > 0) {
			placeOrders.add(new PlaceArmiesMove(BotState.getMyName(), map.getOwnedRegions(BotState.getMyName()).get(0), armiesLeft));
			armiesLeft = 0;
		}

	}

	private void addPotentialAttacks(ArrayList<PotentialAttack> potentialAttacks, Region currentOriginRegion, HashMap<Region, Integer> available,
			HashMap<Region, Integer> availablePotential) {
		potentialAttacks.addAll(generatePotentialAttacks(currentOriginRegion, available));
		int used = Math.min(Values.calculateRequiredForcesDefend(currentOriginRegion), available.get(currentOriginRegion));
		System.err.println("Generated potential attacks for " + currentOriginRegion.getId() + " there are " + potentialAttacks.size()
				+ " potential attacks in total");
		int left = available.get(currentOriginRegion) - used;
		System.err.println("was " + available.get(currentOriginRegion));
		available.put(currentOriginRegion, left);
		System.err.println(available.get(currentOriginRegion) + " left");
		availablePotential.put(currentOriginRegion, used);

	}

	private void usePotentialAttacks(HashMap<Region, Integer> availablePotential, ArrayList<PotentialAttack> potentialAttacks, Region currentTargetRegion,
			HashMap<Region, Integer> attacking, HashMap<FromTo, Integer> decisions, HashMap<Region, Boolean> hasOnlyOnesAttacking) {
		ArrayList<PotentialAttack> allPotentialAttacks = (ArrayList<PotentialAttack>) potentialAttacks.clone();
		for (PotentialAttack p : allPotentialAttacks) {
			if (p.getTo().equals(currentTargetRegion)) {
				int disposed = Math.min(Math.min(availablePotential.get(p.getFrom()), p.getFrom().getArmies() - 1), p.getForces());
				addAttacking(p.getTo(), attacking, disposed, hasOnlyOnesAttacking);
				FromTo currentMove = new FromTo(p.getFrom(), p.getTo());
				addMove(currentMove, decisions, disposed);
				System.err.println("Potential Attack from: " + p.getFrom());
				potentialAttacks.remove(p);
				availablePotential.put(p.getFrom(), availablePotential.get(p.getFrom()) - disposed);

			}
		}
	}

	// potential attacks are attacks that may be performed from a tile if it
	// means that the tile is still left decently defended against other
	// threatening tiles
	private ArrayList<PotentialAttack> generatePotentialAttacks(Region currentOriginRegion, HashMap<Region, Integer> available) {
		ArrayList<Region> enemyRegions = currentOriginRegion.getEnemyNeighbors();
		ArrayList<PotentialAttack> potentialAttacks = new ArrayList<PotentialAttack>();
		ArrayList<Region> allEnemyRegions = (ArrayList<Region>) enemyRegions.clone();
		for (Region r : enemyRegions) {
			allEnemyRegions.remove(r);
			int requiredToDefend = Values.calculateRequiredForcesDefendRegionAgainstSpecificRegions(allEnemyRegions);
			if (requiredToDefend < available.get(currentOriginRegion)) {
				int disposed = available.get(currentOriginRegion) - requiredToDefend;
				potentialAttacks.add(new PotentialAttack(currentOriginRegion, r, disposed));
			}
			allEnemyRegions.add(r);
		}
		return potentialAttacks;
	}

	private boolean alreadySatisfied(Region currentFinalTargetRegion, SuperRegion currentTargetSuperRegion, HashMap<Region, Integer> regionSatisfied,
			HashMap<SuperRegion, Integer> superRegionSatisfied) {
		if (superRegionSatisfied.get(currentTargetSuperRegion) < 1) {
			return true;
		}
		if (regionSatisfied.get(currentFinalTargetRegion) < 1) {
			return true;
		}
		return false;
	}

	private void addSatisfaction(Region currentTargetRegion, Region currentFinalTargetRegion, int disposed, HashMap<Region, Integer> regionSatisfied,
			HashMap<SuperRegion, Integer> superRegionSatisfied) {
		SuperRegion currentTargetSuperRegion = currentTargetRegion.getSuperRegion();
		superRegionSatisfied.put(currentTargetSuperRegion, superRegionSatisfied.get(currentTargetSuperRegion) - disposed);
		regionSatisfied.put(currentFinalTargetRegion, regionSatisfied.get(currentFinalTargetRegion) - disposed);

	}

	private void addMove(FromTo currentMove, HashMap<FromTo, Integer> decisions, int disposed) {
		if (decisions.get(currentMove) == null) {
			decisions.put(currentMove, disposed);
		} else {
			decisions.put(currentMove, decisions.get(currentMove) + disposed);
		}

	}

	private void addAttacking(Region currentTargetRegion, HashMap<Region, Integer> attacking, int disposed, HashMap<Region, Boolean> hasOnlyOnesAttacking) {
		if (attacking.get(currentTargetRegion) == null) {
			attacking.put(currentTargetRegion, disposed);
		} else {
			attacking.put(currentTargetRegion, attacking.get(currentTargetRegion) + disposed);
		}
		if (disposed > 1) {
			hasOnlyOnesAttacking.put(currentTargetRegion, false);
		} else {
			if (hasOnlyOnesAttacking.get(currentTargetRegion) == null) {
				hasOnlyOnesAttacking.put(currentTargetRegion, true);
			}
		}
	}

}
