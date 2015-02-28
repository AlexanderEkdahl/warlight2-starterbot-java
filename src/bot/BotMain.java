package bot;

import imaginary.EnemyAppreciator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import commanders.*;
import concepts.ActionProposal;
import concepts.ActionType;
import concepts.FromTo;
import concepts.Outcome;
import concepts.PotentialAttack;
import map.Map;
import map.Pathfinder;
import map.PathfinderWeighter;
import map.Region;
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
		long startTime = System.currentTimeMillis();

		EnemyAppreciator appreciator = state.getFullMap().getAppreciator();
		Map speculativeMap = appreciator.getSpeculativeMap();
		// where the magic happens
		generateOrders(speculativeMap, state.getStartingArmies());

		long endTime = System.currentTimeMillis();
		System.err.println("Generating orders took " + (endTime - startTime) + " ms");
		return placeOrders;
	}

	public ArrayList<AttackTransferMove> getAttackTransferMoves(BotState state, Long timeOut) {
		return moveOrders;

	}

	private ArrayList<ActionProposal> getProposals(Map map, Set<Integer> available) {
		ArrayList<ActionProposal> proposals = new ArrayList<ActionProposal>();

		Pathfinder pathfinder = new Pathfinder(map, new PathfinderWeighter() {
			public double weight(Region nodeA, Region nodeB) {
				return Values.calculateRegionWeighedCost(nodeB);

			}
		});

		proposals.addAll(oc.getActionProposals(map, available, pathfinder));
		proposals.addAll(gc.getActionProposals(map, available, pathfinder));
		proposals.addAll(dc.getActionProposals(map, available, pathfinder));

		return proposals;
	}

	private void generateOrders(Map original, int armiesLeft) {

		placeOrders = new ArrayList<PlaceArmiesMove>();
		moveOrders = new ArrayList<AttackTransferMove>();
		HashMap<Integer, Integer> satisfaction = Values.calculateRegionSatisfaction(original);
		HashMap<Integer, Integer> attacking = new HashMap<Integer, Integer>();
		HashMap<FromTo, Integer> decisions = new HashMap<FromTo, Integer>();
		HashMap<FromTo, Integer> backupDecisions = new HashMap<FromTo, Integer>();
		HashMap<Integer, Boolean> hasOnlyOnesAttacking = new HashMap<Integer, Boolean>();
		ArrayList<PotentialAttack> potentialAttacks = new ArrayList<PotentialAttack>();
		HashMap<Integer, Integer> availablePotential = new HashMap<Integer, Integer>();

		Map speculativeMap = original.duplicate();
		HashMap<Integer, Integer> available = new HashMap<Integer, Integer>();
		for (Region r : original.getOwnedRegions(BotState.getMyName())) {
			available.put(r.getId(), r.getArmies() - 1);
			availablePotential.put(r.getId(), 0);
		}
		Set<Integer> interestingKeys = available.keySet();

		boolean somethingWasDone = true;
		// TODO decide how to merge proposals
		ArrayList<ActionProposal> proposals;

		while (somethingWasDone) {
			somethingWasDone = false;
			if (armiesLeft < 1) {
				interestingKeys = getInteresting(available);
			}

			proposals = getProposals(speculativeMap, interestingKeys);

			Collections.sort(proposals);

			for (int i = 0; i < proposals.size(); i++) {
				ActionProposal currentProposal = proposals.get(i);
				Region currentOriginRegion = speculativeMap.getRegion(currentProposal.getOrigin().getId());
				Region currentTargetRegion = speculativeMap.getRegion(currentProposal.getTarget().getId());
				Region currentFinalTargetRegion = speculativeMap.getRegion(currentProposal.getPlan().getR().getId());
				int required = currentProposal.getForces();

				// decision has been made to go forward with the proposal
				if ((available.get(currentOriginRegion.getId()) > 0 || armiesLeft > 0) && required > 0) {
					// check satisfaction
					if (satisfaction.get(currentFinalTargetRegion.getId()) <= 0) {
						FromTo currentMove = new FromTo(currentOriginRegion.getId(), currentTargetRegion.getId());
						if (backupDecisions.get(currentMove) == null) {
							backupDecisions.put(currentMove, required);
						} else {
							decisions.put(currentMove, backupDecisions.get(currentMove) + required);
						}
						continue;
					} else {
						required = Math.min(satisfaction.get(currentFinalTargetRegion.getId()), required);
					}

					int disposed;
					if (available.get(currentOriginRegion.getId()) < required) {
						int initiallyAvailable = available.get(currentOriginRegion.getId());
						int placed = Math.min(required - initiallyAvailable, armiesLeft);
						if (placed > 0) {
							placeOrders.add(new PlaceArmiesMove(BotState.getMyName(), currentOriginRegion, placed));
							System.err.println("Placed " + placed + " at " + currentOriginRegion);
							available.put(currentOriginRegion.getId(), available.get(currentOriginRegion.getId()) + placed);
							currentOriginRegion.setArmies(currentOriginRegion.getArmies() + placed);
						}
						disposed = initiallyAvailable + placed;
						armiesLeft -= placed;
					} else {
						disposed = required;
					}

					somethingWasDone = true;
					System.err.println(currentProposal.toString());
					satisfaction.put(currentFinalTargetRegion.getId(), satisfaction.get(currentFinalTargetRegion.getId()) - disposed);

					if (currentProposal.getPlan().getActionType().equals(ActionType.DEFEND)) {
						// attack is the best defence
						if (currentOriginRegion.equals(currentFinalTargetRegion)) {
							available.put(currentOriginRegion.getId(), available.get(currentOriginRegion.getId()) - disposed);
							availablePotential.put(currentOriginRegion.getId(), availablePotential.get(currentOriginRegion.getId()) + disposed);
							addPotentialAttacks(potentialAttacks, currentOriginRegion, availablePotential);
							break;
						}
					}

					FromTo currentMove = new FromTo(currentOriginRegion.getId(), currentTargetRegion.getId());
					addMove(currentMove, decisions, disposed, hasOnlyOnesAttacking, speculativeMap, satisfaction, attacking);
					available.put(currentOriginRegion.getId(), available.get(currentOriginRegion.getId()) - disposed);

					if (!currentTargetRegion.getPlayerName().equals(BotState.getMyName())) {
						usePotentialAttacks(availablePotential, potentialAttacks, currentTargetRegion, attacking, decisions, hasOnlyOnesAttacking,
								speculativeMap, satisfaction);
					}
					break;
				}
			}
		}

		// add leftover potentialattacks to the pile of attacks

		for (PotentialAttack p : potentialAttacks) {
			if (!(availablePotential.get(p.getFrom()) == null) && availablePotential.get(p.getFrom()) > 1) {
				int disposed = Math.min(Math.min(availablePotential.get(p.getFrom()), speculativeMap.getRegion(p.getFrom()).getArmies() - 1), p.getForces());
				FromTo currentMove = new FromTo(p.getFrom(), p.getTo());
				addMove(currentMove, decisions, disposed, hasOnlyOnesAttacking, speculativeMap, satisfaction, attacking);
				availablePotential.put(p.getFrom(), availablePotential.get(p.getFrom()) - disposed);
			}

		}

		// add backup proposals
		Set<FromTo> backupKeys = backupDecisions.keySet();
		for (FromTo f : backupKeys) {
			int disposed = Math.min(available.get(f.getR1()), backupDecisions.get(f));
			if (disposed > 0) {
				addMove(f, decisions, disposed, hasOnlyOnesAttacking, speculativeMap, satisfaction, attacking);
				available.put(f.getR1(), available.get(f.getR1()) - disposed);
			}
		}

		// exclude bad attacks from moves
		Set<Integer> aKeys = attacking.keySet();
		ArrayList<Integer> badAttacks = new ArrayList<Integer>();

		for (Integer r : aKeys) {
			if ((!speculativeMap.getRegion(r).getPlayerName().equals((BotState.getMyName())))) {
				if (speculativeMap.getRegion(r).getPlayerName().equals((BotState.getMyOpponentName()))) {
					if (speculativeMap.getRegion(r).getArmies() > Values.maximumEnemyForcesAllowedPotentiallyLeftAfterAttack) {
						badAttacks.add(r);

					}
				} else if (speculativeMap.getRegion(r).getPlayerName().equals("neutral")) {
					badAttacks.add(r);
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

	private Set<Integer> getInteresting(HashMap<Integer, Integer> available) {
		Set<Integer> stillInteresting = new HashSet<Integer>();
		for (Integer i : available.keySet()) {
			if (available.get(i) > 0) {
				stillInteresting.add(i);
			}
		}
		return stillInteresting;
	}

	private void addPotentialAttacks(ArrayList<PotentialAttack> potentialAttacks, Region currentOriginRegion, HashMap<Integer, Integer> availablePotential) {
		potentialAttacks.addAll(generatePotentialAttacks(currentOriginRegion, availablePotential));

	}

	// potential attacks are attacks that may be performed from a tile if it
	// means that the tile is still left decently defended against other
	// threatening tiles
	private ArrayList<PotentialAttack> generatePotentialAttacks(Region currentOriginRegion, HashMap<Integer, Integer> availablePotential) {
		ArrayList<Region> enemyRegions = currentOriginRegion.getEnemyNeighbors();
		ArrayList<PotentialAttack> potentialAttacks = new ArrayList<PotentialAttack>();
		ArrayList<Region> defendingAgainst = new ArrayList<Region>();
		for (Region r : enemyRegions) {
			defendingAgainst.clear();
			for (Region r2 : enemyRegions) {
				if (!r.equals(r2)) {
					defendingAgainst.add(r2);
				}
			}
			int requiredToDefend = Values.calculateRequiredForcesDefendRegionAgainstSpecificRegions(defendingAgainst);
			if (requiredToDefend < availablePotential.get(currentOriginRegion.getId())) {
				int disposed = availablePotential.get(currentOriginRegion.getId()) - requiredToDefend;
				potentialAttacks.add(new PotentialAttack(currentOriginRegion, r, disposed));
			}
		}
		return potentialAttacks;
	}

	private void usePotentialAttacks(HashMap<Integer, Integer> availablePotential, ArrayList<PotentialAttack> potentialAttacks, Region currentTargetRegion,
			HashMap<Integer, Integer> attacking, HashMap<FromTo, Integer> decisions, HashMap<Integer, Boolean> hasOnlyOnesAttacking, Map map,
			HashMap<Integer, Integer> satisfaction) {
		ArrayList<PotentialAttack> used = new ArrayList<PotentialAttack>();

		for (PotentialAttack p : potentialAttacks) {
			if (p.getTo().equals(currentTargetRegion.getId()) && availablePotential.get(p.getFrom()) != null) {
				int disposed = Math.min(Math.min(availablePotential.get(p.getFrom()), map.getRegion(p.getFrom()).getArmies() - 1), p.getForces());
				FromTo currentMove = new FromTo(p.getFrom(), p.getTo());
				addMove(currentMove, decisions, disposed, hasOnlyOnesAttacking, map, satisfaction, attacking);
				System.err.println("Potential Attack from: " + p.getFrom());
				used.add(p);
				availablePotential.put(p.getFrom(), availablePotential.get(p.getFrom() - disposed));

			}
		}
		for (PotentialAttack usedP : used) {
			potentialAttacks.remove(usedP);
		}
	}

	private void addMove(FromTo currentMove, HashMap<FromTo, Integer> decisions, int disposed, HashMap<Integer, Boolean> hasOnlyOnesAttacking, Map map,
			HashMap<Integer, Integer> satisfaction, HashMap<Integer, Integer> attacking) {
		if (decisions.get(currentMove) == null) {
			decisions.put(currentMove, disposed);
		} else {
			decisions.put(currentMove, decisions.get(currentMove) + disposed);
		}

		if (!map.getRegion(currentMove.getR2()).getPlayerName().equals(BotState.getMyName())) {
			addAttacking(currentMove.getR2(), attacking, disposed, hasOnlyOnesAttacking, map, satisfaction);
		} else {
			map.getRegion(currentMove.getR1()).setArmies(map.getRegion(currentMove.getR1()).getArmies() - disposed);
			map.getRegion(currentMove.getR2()).setArmies(map.getRegion(currentMove.getR2()).getArmies() + disposed);
		}

	}

	private void addAttacking(Integer currentTargetRegion, HashMap<Integer, Integer> attacking, int disposed, HashMap<Integer, Boolean> hasOnlyOnesAttacking,
			Map map, HashMap<Integer, Integer> satisfaction) {
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
		Outcome outcome = Values.calculateAttackOutcome(disposed, map.getRegion(currentTargetRegion).getArmies());
		int defendingLeft = outcome.getDefendingArmies();
		int attackingLeft = outcome.getAttackingArmies();
		if (defendingLeft == 0) {
			map.getRegion(currentTargetRegion).setArmies(attackingLeft);
			map.getRegion(currentTargetRegion).setPlayerName(BotState.getMyName());
			satisfaction.put(currentTargetRegion, Values.calculateRequiredForcesDefend(map.getRegion(currentTargetRegion)) - attackingLeft);
		} else {
			map.getRegion(currentTargetRegion).setArmies(defendingLeft);
		}

	}

}
