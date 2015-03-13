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

	public ArrayList<PlaceArmiesMove> getPlaceArmiesMoves(BotState state, Long timeOut) {
		long startTime = System.currentTimeMillis();

		if (timeOut < 10000) {
			Values.defensiveCommanderUseSmallPlacements = false;
			System.err.println("Using performance - cheap defensive strategy");
		} else {
			Values.defensiveCommanderUseSmallPlacements = true;
			System.err.println("Using performance - expensive defensive strategy");
		}
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
		// HashMap<Integer, Integer> attacking = new HashMap<Integer,
		// Integer>();
		HashMap<Integer, HashMap<Integer, Integer>> attackingAgainst = new HashMap<Integer, HashMap<Integer, Integer>>();
		HashMap<FromTo, Integer> decisions = new HashMap<FromTo, Integer>();
		HashMap<FromTo, Integer> backupDecisions = new HashMap<FromTo, Integer>();
		ArrayList<PotentialAttack> potentialAttacks = new ArrayList<PotentialAttack>();
		HashMap<Integer, Integer> availablePotential = new HashMap<Integer, Integer>();
		HashMap<FromTo, Integer> potentialAttackDecisions = new HashMap<FromTo, Integer>();
		HashMap<Integer, Integer> startingEnemyForces = new HashMap<Integer, Integer>();

		Map speculativeMap = original.duplicate();
		HashMap<Integer, Integer> available = new HashMap<Integer, Integer>();
		for (Region r : original.getOwnedRegions(BotState.getMyName())) {
			available.put(r.getId(), r.getArmies() - 1);
		}
		for (Region r : original.getUnOwnedRegions()) {
			startingEnemyForces.put(r.getId(), r.getArmies());
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
							backupDecisions.put(currentMove, backupDecisions.get(currentMove) + required);
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
							addToIntegerHashMap(available, currentOriginRegion.getId(), placed);
							currentOriginRegion.setArmies(currentOriginRegion.getArmies() + placed);
						}
						disposed = initiallyAvailable + placed;
						armiesLeft -= placed;
					} else {
						disposed = required;
					}

					if (!currentTargetRegion.getPlayerName().equals(BotState.getMyName()) && !(currentTargetRegion.getArmies() < 3) && disposed < 2) {
						continue;
					}

					somethingWasDone = true;
					System.err.println(currentProposal.toString());

					addToIntegerHashMap(available, currentOriginRegion.getId(), -disposed);

					if (currentProposal.getPlan().getActionType().equals(ActionType.DEFEND)) {
						// attack is the best defence
						if (currentOriginRegion.equals(currentFinalTargetRegion)) {
							addToIntegerHashMap(availablePotential, currentOriginRegion.getId(), disposed);
							addPotentialAttacks(potentialAttacks, speculativeMap.getRegion(currentOriginRegion.getId()), availablePotential);
							usePotentialAttacks(potentialAttacks, satisfaction, potentialAttackDecisions, speculativeMap, availablePotential, attackingAgainst,
									startingEnemyForces);
							break;
						}
					} else {
						FromTo currentMove = new FromTo(currentOriginRegion.getId(), currentTargetRegion.getId());
						addMove(currentMove, decisions, disposed, speculativeMap, satisfaction, attackingAgainst, startingEnemyForces);

						break;
					}

				}
			}
		}

		// for (Integer i : availablePotential.keySet()) {
		// addPotentialAttacks(potentialAttacks, speculativeMap.getRegion(i),
		// availablePotential);
		// }
		// for (PotentialAttack p : potentialAttacks) {
		// if ((availablePotential.get(p.getFrom()) != null) &&
		// availablePotential.get(p.getFrom()) > 1) {
		// int disposed = Math.min(Math.min(availablePotential.get(p.getFrom()),
		// speculativeMap.getRegion(p.getFrom()).getArmies() - 1),
		// p.getForces());
		// FromTo currentMove = new FromTo(p.getFrom(), p.getTo());
		// addMove(currentMove, potentialAttackDecisions, disposed,
		// speculativeMap, satisfaction, attackingAgainst, startingEnemyForces);
		// availablePotential.put(p.getFrom(),
		// availablePotential.get(p.getFrom()) - disposed);
		// System.err.println("Potential Attack from: " + p.getFrom() + " To " +
		// p.getTo() + " With " + p.getForces());
		// }
		//
		// }

		// add backup proposals
		Set<FromTo> backupKeys = backupDecisions.keySet();
		for (FromTo f : backupKeys) {
			int disposed = Math.min(available.get(f.getR1()), backupDecisions.get(f));
			if (disposed > 0 && satisfaction.get(f.getR2()) > -10) {
				addMove(f, decisions, disposed, speculativeMap, satisfaction, attackingAgainst, startingEnemyForces);
				available.put(f.getR1(), available.get(f.getR1()) - disposed);
			}
		}

		// exclude bad attacks from moves
		Set<Integer> aKeys = attackingAgainst.keySet();
		ArrayList<Integer> badAttacks = new ArrayList<Integer>();
		ArrayList<Integer> badPotentialAttacks = new ArrayList<Integer>();

		for (Integer r : aKeys) {
			if ((!speculativeMap.getRegion(r).getPlayerName().equals((BotState.getMyName())))) {
				badPotentialAttacks.add(r);
				if (speculativeMap.getRegion(r).getPlayerName().equals(("neutral"))) {
					badAttacks.add(r);
				}
			}
		}

		Set<FromTo> keys = potentialAttackDecisions.keySet();
		for (FromTo f : keys) {
			if (!badPotentialAttacks.contains(f.getR2())) {
				decisions.put(f, potentialAttackDecisions.get(f));

			}
		}

		keys = decisions.keySet();
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

	private void usePotentialAttacks(ArrayList<PotentialAttack> potentialAttacks, HashMap<Integer, Integer> satisfaction,
			HashMap<FromTo, Integer> potentialAttackDecisions, Map map, HashMap<Integer, Integer> availablePotential,
			HashMap<Integer, HashMap<Integer, Integer>> attackingAgainst, HashMap<Integer, Integer> startingEnemyForces) {
		ArrayList<PotentialAttack> used = new ArrayList<PotentialAttack>();
		for (PotentialAttack p : potentialAttacks) {
			FromTo currentMove = new FromTo(p.getFrom(), p.getTo());
			addMove(currentMove, potentialAttackDecisions, p.getForces(), map, satisfaction, attackingAgainst, startingEnemyForces);
			availablePotential.put(p.getFrom(), availablePotential.get(p.getFrom()) - p.getForces());
			used.add(p);
			System.err.println("Potential Attack from: " + p.getFrom() + " To " + p.getTo() + " With " + p.getForces());

		}
		potentialAttacks.removeAll(used);

	}

	private void addToIntegerHashMap(HashMap<Integer, Integer> hashMap, int id, int number) {
		if (hashMap.get(id) == null) {
			hashMap.put(id, number);
		} else {
			hashMap.put(id, hashMap.get(id) + number);
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

	private void addMove(FromTo currentMove, HashMap<FromTo, Integer> decisions, int disposed, Map map, HashMap<Integer, Integer> satisfaction,
			HashMap<Integer, HashMap<Integer, Integer>> attackingAgainst, HashMap<Integer, Integer> startingEnemyForces) {
		if (decisions.get(currentMove) == null) {
			decisions.put(currentMove, disposed);
		} else {
			decisions.put(currentMove, decisions.get(currentMove) + disposed);
		}

		if (!map.getRegion(currentMove.getR2()).getPlayerName().equals(BotState.getMyName())) {
			addAttacking(currentMove.getR2(), currentMove.getR1(), disposed, map, satisfaction, attackingAgainst, startingEnemyForces);
		} else {
			map.getRegion(currentMove.getR1()).setArmies(map.getRegion(currentMove.getR1()).getArmies() - disposed);
			map.getRegion(currentMove.getR2()).setArmies(map.getRegion(currentMove.getR2()).getArmies() + disposed);
		}

	}

	private void addAttacking(Integer currentTargetRegion, Integer currentOriginRegion, int disposed, Map map, HashMap<Integer, Integer> satisfaction,
			HashMap<Integer, HashMap<Integer, Integer>> attackingAgainst, HashMap<Integer, Integer> startingEnemyForces) {
		if (attackingAgainst.get(currentTargetRegion) == null) {
			attackingAgainst.put(currentTargetRegion, new HashMap<Integer, Integer>());
		}
		if (attackingAgainst.get(currentTargetRegion).get(currentOriginRegion) == null) {
			attackingAgainst.get(currentTargetRegion).put(currentOriginRegion, disposed);
		} else {
			attackingAgainst.get(currentTargetRegion).put(currentOriginRegion, attackingAgainst.get(currentTargetRegion).get(currentOriginRegion) + disposed);
		}
		attackingAgainst.get(currentTargetRegion).get(currentOriginRegion);

		calculateOutcomeForRegion(currentTargetRegion, currentOriginRegion, disposed, map, satisfaction, attackingAgainst, startingEnemyForces);

	}

	private void calculateOutcomeForRegion(Integer currentTargetRegion, Integer currentOriginRegion, int disposed, Map map,
			HashMap<Integer, Integer> satisfaction, HashMap<Integer, HashMap<Integer, Integer>> attackingAgainst, HashMap<Integer, Integer> startingEnemyForces) {
		int defending = startingEnemyForces.get(currentTargetRegion);
		int latestAttackingLeft = 0;
		int latestAttackingTotal = 0;
		Outcome currentOutcome = null;
		for (Integer tempOriginRegion : attackingAgainst.get(currentTargetRegion).keySet()) {
			if (defending > 0) {
				latestAttackingTotal = attackingAgainst.get(currentTargetRegion).get(tempOriginRegion);
				currentOutcome = Values.calculateAttackOutcome(latestAttackingTotal, defending);
				defending = currentOutcome.getDefendingArmies();
				latestAttackingLeft = currentOutcome.getAttackingArmies();
				if (defending < 1) {
					map.getRegion(currentTargetRegion).setArmies(latestAttackingLeft);
					map.getRegion(currentTargetRegion).setPlayerName(BotState.getMyName());
					map.getRegion(currentOriginRegion).setArmies(map.getRegion(currentOriginRegion).getArmies() - latestAttackingTotal);
					satisfaction.put(currentTargetRegion, Values.calculateRequiredForcesDefend(map.getRegion(currentTargetRegion)) - latestAttackingLeft);
				} else {
					int lost = latestAttackingTotal - latestAttackingLeft;
					map.getRegion(currentTargetRegion).setArmies(defending);
					map.getRegion(currentOriginRegion).setArmies(map.getRegion(currentOriginRegion).getArmies() - lost);

				}
			}

		}
	}
}
