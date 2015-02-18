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
import concepts.Outcome;
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

		// where the magic happens
		generateOrders(speculativeMap, state.getStartingArmies());

		return placeOrders;
	}

	public ArrayList<AttackTransferMove> getAttackTransferMoves(BotState state, Long timeOut) {
		return moveOrders;

	}

	private void generateOrders(Map original, int armiesLeft) {
		placeOrders = new ArrayList<PlaceArmiesMove>();
		moveOrders = new ArrayList<AttackTransferMove>();
		Map speculativeMap = original.duplicate();
		HashMap<Integer, Integer> offensiveSatisfaction = Values.calculateRegionOffensiveSatisfaction(speculativeMap);
		HashMap<Integer, Integer> defensiveSatisfaction = Values.calculateRegionDefensiveSatisfaction(speculativeMap);
		HashMap<Integer, Integer> attacking = new HashMap<Integer, Integer>();
		HashMap<FromTo, Integer> decisions = new HashMap<FromTo, Integer>();
		HashMap<Integer, Boolean> hasOnlyOnesAttacking = new HashMap<Integer, Boolean>();
		ArrayList<PotentialAttack> potentialAttacks = new ArrayList<PotentialAttack>();

		HashMap<Region, Integer> available = new HashMap<Region, Integer>();
		for (Region r : original.getOwnedRegions(BotState.getMyName())) {
			available.put(r, r.getArmies() - 1);
		}
		HashMap<Integer, Integer> availablePotential = new HashMap<Integer, Integer>();

		// TODO decide how to merge proposals
		ArrayList<ActionProposal> proposals = new ArrayList<ActionProposal>();

		boolean somethingWasDone = true;

		while (somethingWasDone) {
			somethingWasDone = false;
			proposals.addAll(oc.getActionProposals(speculativeMap, available.keySet()));
			proposals.addAll(gc.getActionProposals(speculativeMap, available.keySet()));
			proposals.addAll(dc.getActionProposals(speculativeMap, available.keySet()));

			Collections.sort(proposals);

			for (int i = 0; i < proposals.size(); i++) {
				ActionProposal currentProposal = proposals.get(i);
				Region currentOriginRegion = currentProposal.getOrigin();
				Region currentTargetRegion = currentProposal.getTarget();
				Region currentFinalTargetRegion = currentProposal.getPlan().getR();
				int required = currentProposal.getForces();

				// decision has been made to go forward with the proposal
				if ((available.get(currentOriginRegion) > 0 || armiesLeft > 0) && required > 0) {
					
					if (currentProposal.getPlan().getActionType().equals(ActionType.DEFEND)) {
						if (defensiveSatisfaction.get(currentFinalTargetRegion.getId()) <= 0) {
							continue;
						} else {
							required = Math.min(defensiveSatisfaction.get(currentFinalTargetRegion.getId()), required);
						}
					}
					if (currentProposal.getPlan().getActionType().equals(ActionType.ATTACK)) {
						if (offensiveSatisfaction.get(currentFinalTargetRegion.getId()) <= 0) {
							continue;
						} else {
							required = Math.min(offensiveSatisfaction.get(currentFinalTargetRegion.getId()), required);
						}
					}
					somethingWasDone = true;
					int disposed;
					if (available.get(currentOriginRegion) < required) {
						int initiallyAvailable = available.get(currentOriginRegion);
						int placed = Math.min(required - initiallyAvailable, armiesLeft);
						disposed = initiallyAvailable + placed;
						if (placed > 0) {
							placeOrders.add(new PlaceArmiesMove(BotState.getMyName(), currentOriginRegion, placed));
							System.err.println("Placed " + placed + " at " + currentOriginRegion);
							currentOriginRegion.setArmies(currentOriginRegion.getArmies() + placed);
							available.put(currentOriginRegion, available.get(currentOriginRegion) + placed);
						}
						armiesLeft -= placed;
					} else {
						disposed = required;
					}

					if (currentProposal.getPlan().getActionType().equals(ActionType.DEFEND)) {
						defensiveSatisfaction.put(currentFinalTargetRegion.getId(), defensiveSatisfaction.get(currentFinalTargetRegion.getId()) - disposed);
						// attack is the best defence
						if (currentOriginRegion.equals(currentFinalTargetRegion)) {
							addPotentialAttacks(potentialAttacks, currentOriginRegion, available, availablePotential);
							System.err.println(currentProposal.toString());
							break;
						}
					}


					FromTo currentMove = new FromTo(currentOriginRegion.getId(), currentTargetRegion.getId());
					addMove(currentMove, decisions, disposed);

					System.err.println(currentProposal.toString());
					available.put(currentOriginRegion, available.get(currentOriginRegion) - disposed);

					// modify map
					if (isAttack(currentFinalTargetRegion)){
						offensiveSatisfaction.put(currentFinalTargetRegion.getId(), offensiveSatisfaction.get(currentFinalTargetRegion.getId()) - disposed);
					}
					if (isAttack(currentTargetRegion)) {
						addAttacking(currentTargetRegion.getId(), attacking, disposed, hasOnlyOnesAttacking);
						Outcome outcome = Values.calculateAttackOutcome(disposed, currentTargetRegion.getArmies());
						int defendingLeft = outcome.getDefendingArmies();
						int attackingLeft = outcome.getAttackingArmies();
						if (defendingLeft == 0) {
							currentTargetRegion.setArmies(attackingLeft);
							currentTargetRegion.setPlayerName(BotState.getMyName());
							defensiveSatisfaction.put(currentTargetRegion.getId(), defensiveSatisfaction.get(currentTargetRegion.getId()) - attackingLeft);
						} else {
							currentTargetRegion.setArmies(defendingLeft);
						}
						// since this is an attack we will
						// search for potential attacks to help
						usePotentialAttacks(availablePotential, potentialAttacks, currentTargetRegion, attacking, decisions,
								hasOnlyOnesAttacking, speculativeMap);

					} else {
						currentTargetRegion.setArmies(currentTargetRegion.getArmies() + disposed);

					}
					currentOriginRegion.setArmies(currentOriginRegion.getArmies() - disposed);

					break;
				}

			}
		}

		// add leftover potentialattacks to the pile of attacks

		for (PotentialAttack p : potentialAttacks) {
			int disposed = Math.min(Math.min(availablePotential.get(p.getFrom()), speculativeMap.getRegion(p.getFrom()).getArmies() - 1), p.getForces());
			addAttacking(p.getTo(), attacking, disposed, hasOnlyOnesAttacking);
			FromTo currentMove = new FromTo(p.getFrom(), p.getTo());
			addMove(currentMove, decisions, disposed);
			availablePotential.put(p.getFrom(), availablePotential.get(p.getFrom()) - disposed);
		}

		// exclude bad attacks from moves
		Set<Integer> aKeys = attacking.keySet();
		ArrayList<Integer> badAttacks = new ArrayList<Integer>();
		for (Integer r : aKeys) {
			if (isAttack(original.getRegion(r))) {
				if (hasOnlyOnesAttacking.get(r)) {
					badAttacks.add(r);
					System.err.println("Cancelled attack against: " + r);
				} else if (!speculativeMap.getRegion(r).getPlayerName().equals(BotState.getMyName())) {
					badAttacks.add(r);
					System.err.println("Cancelled attack against: " + r);
				}
			}
		}

		Set<FromTo> keys = decisions.keySet();

		for (FromTo f : keys) {
			if (!badAttacks.contains(f.getR2())) {
				if (decisions.get(f) == 1) {
					moveOrders.add(0,
							new AttackTransferMove(BotState.getMyName(), original.getRegion(f.getR1()), original.getRegion(f.getR2()), decisions.get(f)));
				} else {
					moveOrders
							.add(new AttackTransferMove(BotState.getMyName(), original.getRegion(f.getR1()), original.getRegion(f.getR2()), decisions.get(f)));
				}
			}

		}

		if (armiesLeft > 0) {
			placeOrders.add(new PlaceArmiesMove(BotState.getMyName(), original.getOwnedRegions(BotState.getMyName()).get(0), armiesLeft));
			armiesLeft = 0;
		}

	}

	private boolean isAttack(Region currentTargetRegion) {
		return !currentTargetRegion.getPlayerName().equals(BotState.getMyName());
	}

	private void addPotentialAttacks(ArrayList<PotentialAttack> potentialAttacks, Region currentOriginRegion, HashMap<Region, Integer> available,
			HashMap<Integer, Integer> availablePotential) {
		potentialAttacks.addAll(generatePotentialAttacks(currentOriginRegion, available));
		int used = Math.min(Values.calculateRequiredForcesDefend(currentOriginRegion), available.get(currentOriginRegion));
		System.err.println("Generated potential attacks for " + currentOriginRegion.getId() + " there are " + potentialAttacks.size()
				+ " potential attacks in total");
		int left = available.get(currentOriginRegion) - used;
		System.err.println("was " + available.get(currentOriginRegion));
		available.put(currentOriginRegion, left);
		System.err.println(available.get(currentOriginRegion) + " left");
		availablePotential.put(currentOriginRegion.getId(), used);

	}

	private void usePotentialAttacks(HashMap<Integer, Integer> availablePotential, ArrayList<PotentialAttack> potentialAttacks, Region currentTargetRegion,
			HashMap<Integer, Integer> attacking, HashMap<FromTo, Integer> decisions, HashMap<Integer, Boolean> hasOnlyOnesAttacking, Map map) {
		ArrayList<PotentialAttack> allPotentialAttacks = (ArrayList<PotentialAttack>) potentialAttacks.clone();
		for (PotentialAttack p : allPotentialAttacks) {
			if (p.getTo().equals(currentTargetRegion)) {
				int disposed = Math.min(Math.min(availablePotential.get(p.getFrom()), map.getRegion(p.getFrom()).getArmies() - 1), p.getForces());
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

	private void addMove(FromTo currentMove, HashMap<FromTo, Integer> decisions, int disposed) {
		if (decisions.get(currentMove) == null) {
			decisions.put(currentMove, disposed);
		} else {
			decisions.put(currentMove, decisions.get(currentMove) + disposed);
		}

	}

	private void addAttacking(Integer currentTargetRegion, HashMap<Integer, Integer> attacking, int disposed, HashMap<Integer, Boolean> hasOnlyOnesAttacking) {
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
