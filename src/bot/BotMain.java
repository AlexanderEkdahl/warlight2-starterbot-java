package bot;

import imaginary.EnemyAppreciator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
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
import map.SuperRegion;
import move.AttackTransferMove;
import move.PlaceArmiesMove;

public class BotMain implements Bot {
	public enum Situation {
		CONFLICTED, ONESIDED
	}

	private OffensiveCommander oc;
	private DefensiveCommander dc;
	private GriefCommander gc;
	private ArrayList<PlaceArmiesMove> placeOrders;
	private ArrayList<AttackTransferMove> moveOrders;
	private HashMap<Integer, Situation> situations;

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
		generateOrders(state.getFullMap(), speculativeMap, state.getStartingArmies());

		long endTime = System.currentTimeMillis();
		System.err.println("Generating orders took " + (endTime - startTime) + " ms");
		return placeOrders;
	}

	public ArrayList<AttackTransferMove> getAttackTransferMoves(BotState state, Long timeOut) {
		return moveOrders;

	}

	private ArrayList<ActionProposal> generateProposals(Map map, Set<Integer> interesting, HashMap<Integer, Integer> currentlyDefending,
			HashMap<Integer, Integer> available) {
		ArrayList<ActionProposal> proposals = new ArrayList<ActionProposal>();

		Pathfinder pathfinder = new Pathfinder(map, new PathfinderWeighter() {
			public double weight(Region nodeA, Region nodeB) {
				return Values.calculateRegionWeighedCost(nodeB);

			}
		});

		proposals.addAll(oc.getActionProposals(map, interesting, pathfinder, available));
		proposals.addAll(gc.getActionProposals(map, interesting, pathfinder, available));
		proposals.addAll(dc.getActionProposals(map, interesting, pathfinder, currentlyDefending, available));

		return proposals;
	}

	private void generateOrders(Map unTouchedMap, Map appreciatedMap, int armiesLeft) {

		placeOrders = new ArrayList<PlaceArmiesMove>();
		moveOrders = new ArrayList<AttackTransferMove>();

		// initiate the bazinga-load of data structures
		HashMap<Integer, Integer> satisfaction = Values.calculateRegionSatisfaction(appreciatedMap);
		HashMap<Integer, HashMap<Integer, Integer>> attackingAgainst = new HashMap<Integer, HashMap<Integer, Integer>>();
		HashMap<FromTo, Integer> decisions = new HashMap<FromTo, Integer>();
		HashMap<Integer, Integer> placeDecisions = new HashMap<Integer, Integer>();
		HashMap<FromTo, Integer> backupDecisions = new HashMap<FromTo, Integer>();
		ArrayList<PotentialAttack> potentialAttacks = new ArrayList<PotentialAttack>();
		HashMap<Integer, Integer> availablePotential = new HashMap<Integer, Integer>();
		HashMap<FromTo, Integer> potentialAttackDecisions = new HashMap<FromTo, Integer>();
		HashMap<Integer, Integer> startingEnemyForces = new HashMap<Integer, Integer>();
		HashMap<Integer, Integer> currentlyDefending = new HashMap<Integer, Integer>();
		HashMap<FromTo, Integer> accountedLosses = new HashMap<FromTo, Integer>();

		Map speculativeMap = appreciatedMap.duplicate();
		HashMap<Integer, Integer> available = new HashMap<Integer, Integer>();
		for (Region r : appreciatedMap.getOwnedRegions(BotState.getMyName())) {
			available.put(r.getId(), r.getArmies() - 1);
			currentlyDefending.put(r.getId(), 0);
		}
		for (Region r : appreciatedMap.getUnOwnedRegions()) {
			startingEnemyForces.put(r.getId(), r.getArmies());
		}

		Set<Integer> interestingKeys = available.keySet();

		boolean somethingWasDone = true;
		// TODO decide how to merge proposals
		ArrayList<ActionProposal> proposals;

		System.err.println("Decided proposals:");
		while (somethingWasDone) {
			somethingWasDone = false;
			if (armiesLeft < 1) {
				interestingKeys = getInteresting(available);
			}
			proposals = generateProposals(speculativeMap, interestingKeys, currentlyDefending, available);

			Collections.sort(proposals);

			for (int i = 0; i < proposals.size(); i++) {

				// enable bamboozlement
				// int counter = 1;
				// int maxI = i;
				// while ((i + counter) < proposals.size()-1 &&
				// (proposals.get(i).getPlan().getR() == proposals.get(i +
				// counter).getPlan().getR())
				// && (proposals.get(i).getWeight() == proposals.get(i +
				// counter).getWeight())) {
				// maxI = available.get(proposals.get(maxI).getOrigin().getId())
				// < available.get(proposals.get(i +
				// counter).getOrigin().getId()) ? i + counter
				// : maxI;
				// counter++;
				// System.err.println("Determined potential other starting point: "
				// + proposals.get(i + counter).getOrigin().getId() +
				// " for attack against: "
				// + proposals.get(i).getTarget().getId());
				// }
				// i = maxI;

				ActionProposal currentProposal = proposals.get(i);
				Region currentOriginRegion = speculativeMap.getRegion(currentProposal.getOrigin().getId());
				Region currentTargetRegion = speculativeMap.getRegion(currentProposal.getTarget().getId());
				Region currentFinalTargetRegion = speculativeMap.getRegion(currentProposal.getPlan().getR().getId());
				int required = currentProposal.getForces();

				// decision has been made to go forward with the proposal
				if ((available.get(currentOriginRegion.getId()) > 0 || armiesLeft > 0) && required > 0) {
					FromTo currentMove = new FromTo(currentOriginRegion.getId(), currentTargetRegion.getId());

					// check satisfaction and create backup decisions
					if (required > satisfaction.get(currentFinalTargetRegion.getId())) {
						int roomLeft = satisfaction.get(currentFinalTargetRegion.getId());
						int forcesNotUsed = required - roomLeft;
						required = roomLeft;
						if (backupDecisions.get(currentMove) == null) {
							backupDecisions.put(currentMove, forcesNotUsed);
						} else {
							backupDecisions.put(currentMove, backupDecisions.get(currentMove) + forcesNotUsed);
						}
						if (required < 1) {
							continue;
						}
					}

					// potentially place new forces
					int disposed;
					if (available.get(currentOriginRegion.getId()) < required) {
						int initiallyAvailable = available.get(currentOriginRegion.getId());
						int placed = Math.min(required - initiallyAvailable, armiesLeft);
						if (placed > 0) {
							addToIntegerHashMap(placeDecisions, currentOriginRegion.getId(), placed);
							addToIntegerHashMap(available, currentOriginRegion.getId(), placed);
							currentOriginRegion.setArmies(currentOriginRegion.getArmies() + placed);
						}
						disposed = initiallyAvailable + placed;
						armiesLeft -= placed;
					} else {
						disposed = required;
					}

					if (!currentTargetRegion.getPlayerName().equals(BotState.getMyName()) && (currentTargetRegion.getArmies() < 3) && (disposed < 2)) {
						// tis a silly attack
						continue;
					}
					somethingWasDone = true;
					System.err.println(currentProposal.toString() + " disposed: " + disposed);

					// remove used forces
					addToIntegerHashMap(available, currentOriginRegion.getId(), -disposed);

					// add satisfaction
					addToIntegerHashMap(satisfaction, currentFinalTargetRegion.getId(), -disposed);

					// potentially add potentialattacks
					if (currentProposal.getPlan().getActionType().equals(ActionType.DEFEND) && currentProposal.getOrigin().equals(currentProposal.getTarget())) {
						// attack is the best defence
						if (currentOriginRegion.equals(currentFinalTargetRegion)) {
							addToIntegerHashMap(availablePotential, currentOriginRegion.getId(), disposed);
							addToIntegerHashMap(currentlyDefending, currentOriginRegion.getId(), disposed);
							addPotentialAttacks(potentialAttacks, speculativeMap.getRegion(currentOriginRegion.getId()), availablePotential);
							usePotentialAttacks(potentialAttacks, potentialAttackDecisions, speculativeMap, availablePotential, attackingAgainst,
									startingEnemyForces, currentlyDefending, accountedLosses);
							break;
						}
					} else {
						currentMove = new FromTo(currentOriginRegion.getId(), currentTargetRegion.getId());
						addMove(currentMove, decisions, disposed, speculativeMap, attackingAgainst, startingEnemyForces, currentlyDefending, accountedLosses);
						break;
					}

				}
			}
		}

		// for (Integer i : availablePotential.keySet()) {
		// addPotentialAttacks(potentialAttacks, speculativeMap.getRegion(i),
		// availablePotential);
		// }
		// usePotentialAttacks(potentialAttacks, satisfaction,
		// potentialAttackDecisions, speculativeMap, availablePotential,
		// attackingAgainst,
		// startingEnemyForces, currentlyDefending);

		// add backup proposals
		Set<FromTo> backupKeys = backupDecisions.keySet();
		System.err.println("BackupDecisions:");
		for (FromTo f : backupKeys) {
			int disposed = Math.min(available.get(f.getR1()), backupDecisions.get(f));
			if (disposed > 0) {
				System.err.println("from " + f.getR1() + " to " + f.getR2());
				addMove(f, decisions, disposed, speculativeMap, attackingAgainst, startingEnemyForces, currentlyDefending, accountedLosses);
				available.put(f.getR1(), available.get(f.getR1()) - disposed);
			}
		}

		// exclude bad attacks from moves
		Set<Integer> aKeys = attackingAgainst.keySet();
		ArrayList<Integer> badAttacks = new ArrayList<Integer>();
		ArrayList<Integer> badPotentialAttacks = new ArrayList<Integer>();

		System.err.println("Cancelled attacks:");
		for (Integer r : aKeys) {
			if ((!speculativeMap.getRegion(r).getPlayerName().equals((BotState.getMyName())))) {
				badPotentialAttacks.add(r);
				System.err.println("PotentialAttacks: " + r);
				if (speculativeMap.getRegion(r).getPlayerName().equals(("neutral"))) {
					badAttacks.add(r);
					System.err.println("All attacks: " + r);
				}
			}
		}

		Set<FromTo> keys = potentialAttackDecisions.keySet();
		for (FromTo f : keys) {
			if (!badPotentialAttacks.contains(f.getR2())) {
				if (decisions.get(f) == null) {
					decisions.put(f, potentialAttackDecisions.get(f));
				} else {
					decisions.put(f, decisions.get(f) + potentialAttackDecisions.get(f));
				}
			}
		}

		// ArrayList<AttackTransferMove> performedLast = new
		// ArrayList<AttackTransferMove>();
		keys = decisions.keySet();
		for (FromTo f : keys) {
			if (!badAttacks.contains(f.getR2())) {
				// if (decisions.get(f) == 1) {
				// performedLast.add(0, new
				// AttackTransferMove(BotState.getMyName(),
				// appreciatedMap.getRegion(f.getR1()),
				// appreciatedMap.getRegion(f.getR2()),
				// decisions.get(f)));
				// } else {
				moveOrders.add(new AttackTransferMove(BotState.getMyName(), appreciatedMap.getRegion(f.getR1()), appreciatedMap.getRegion(f.getR2()), decisions
						.get(f)));
				// }
			}

		}
		// for (AttackTransferMove a : performedLast) {
		// moveOrders.add(a);
		// }

		System.err.println("Placements:");
		for (Integer i : placeDecisions.keySet()) {
			placeOrders.add(new PlaceArmiesMove(BotState.getMyName(), appreciatedMap.getRegion(i), placeDecisions.get(i)));
			System.err.println(placeDecisions.get(i) + " at " + appreciatedMap.getRegion(i));

		}
		if (armiesLeft > 0) {
			placeOrders.add(new PlaceArmiesMove(BotState.getMyName(), appreciatedMap.getOwnedRegions(BotState.getMyName()).get(0), armiesLeft));
			System.err.println(armiesLeft + " at " + appreciatedMap.getOwnedRegions(BotState.getMyName()).get(0) + " because they are not needed anywhere");
			armiesLeft = 0;
		}

	}

	private void usePotentialAttacks(ArrayList<PotentialAttack> potentialAttacks, HashMap<FromTo, Integer> potentialAttackDecisions, Map map,
			HashMap<Integer, Integer> availablePotential, HashMap<Integer, HashMap<Integer, Integer>> attackingAgainst,
			HashMap<Integer, Integer> startingEnemyForces, HashMap<Integer, Integer> currentlyDefending, HashMap<FromTo, Integer> accountedLosses) {
		ArrayList<PotentialAttack> used = new ArrayList<PotentialAttack>();
		for (PotentialAttack p : potentialAttacks) {
			FromTo currentMove = new FromTo(p.getFrom(), p.getTo());
			addMove(currentMove, potentialAttackDecisions, p.getForces(), map, attackingAgainst, startingEnemyForces, currentlyDefending, accountedLosses);
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
			int requiredToDefend = Values.calculateRequiredForcesDefendAgainstRegions(defendingAgainst);
			if (requiredToDefend < availablePotential.get(currentOriginRegion.getId())) {
				int disposed = availablePotential.get(currentOriginRegion.getId()) - requiredToDefend;
				potentialAttacks.add(new PotentialAttack(currentOriginRegion, r, disposed));
			}
		}
		return potentialAttacks;
	}

	private void addMove(FromTo currentMove, HashMap<FromTo, Integer> decisions, int disposed, Map map,
			HashMap<Integer, HashMap<Integer, Integer>> attackingAgainst, HashMap<Integer, Integer> startingEnemyForces,
			HashMap<Integer, Integer> currentlyDefending, HashMap<FromTo, Integer> accountedLosses) {
		if (decisions.get(currentMove) == null) {
			decisions.put(currentMove, disposed);
		} else {
			decisions.put(currentMove, decisions.get(currentMove) + disposed);
		}
		if (!map.getRegion(currentMove.getR2()).getPlayerName().equals(BotState.getMyName())) {
			addAttacking(currentMove.getR2(), currentMove.getR1(), disposed, attackingAgainst);
			modifyMapBasedOnAttack(map, currentMove, attackingAgainst.get(currentMove.getR2()), startingEnemyForces, currentlyDefending, accountedLosses);
		} else {
			map.getRegion(currentMove.getR1()).setArmies(map.getRegion(currentMove.getR1()).getArmies() - disposed);
			map.getRegion(currentMove.getR2()).setArmies(map.getRegion(currentMove.getR2()).getArmies() + disposed);
			addToIntegerHashMap(currentlyDefending, currentMove.getR2(), disposed);
		}

	}

	private void modifyMapBasedOnAttack(Map map, FromTo currentMove, HashMap<Integer, Integer> attackingAgainst, HashMap<Integer, Integer> startingEnemyForces,
			HashMap<Integer, Integer> currentlyDefending, HashMap<FromTo, Integer> accountedLosses) {

		Region target = map.getRegion(currentMove.getR2());
		HashMap<Integer, Integer> attackingRemaining = new HashMap<Integer, Integer>();
		int defendingLeft = calculateOutcomeForAttack(startingEnemyForces.get(currentMove.getR2()), attackingAgainst, attackingRemaining);

		// modify the defending region
		if (defendingLeft > 0) {
			target.setArmies(defendingLeft);
		} else if (defendingLeft == 0) {
			System.err.println("MAJOR MALFUNCTION IN MODIFYMAPBASEDONATTACK, DEFENDING IS 0\n\n\n\n\n\n");
		} else {
			// We took it!
			target.setPlayerName(BotState.getMyName());
			target.setArmies(-defendingLeft);
			currentlyDefending.put(target.getId(), -defendingLeft);
		}

		// modify the attacking region
		if (accountedLosses.get(currentMove) == null) {
			accountedLosses.put(currentMove, 0);
		}
		int additionallyLost = (attackingAgainst.get(currentMove.getR1()) - attackingRemaining.get(currentMove.getR1())) - accountedLosses.get(currentMove);
		if (additionallyLost < 0) {
			System.err.println("MAJOR MALFUNCTION IN MODIFYMAPBASEDONATTACK, LOST IS NEGATIVE\n\n\n\n\n\n");
		}
		accountedLosses.put(currentMove, accountedLosses.get(currentMove) + additionallyLost);
		map.getRegion(currentMove.getR1()).setArmies(map.getRegion(currentMove.getR1()).getArmies() - additionallyLost);
		if (map.getRegion(currentMove.getR1()).getArmies() < 0) {
			System.err.println("MAJOR MALFUNCTION IN MODIFYMAPBASEDONATTACK, MAP ARMIES IS NEGATIVE\n\n\n\n\n\n");
		}

	}

	private void addAttacking(Integer currentTargetRegion, Integer currentOriginRegion, int disposed,
			HashMap<Integer, HashMap<Integer, Integer>> attackingAgainst) {
		if (attackingAgainst.get(currentTargetRegion) == null) {
			attackingAgainst.put(currentTargetRegion, new HashMap<Integer, Integer>());
		}
		if (attackingAgainst.get(currentTargetRegion).get(currentOriginRegion) == null) {
			attackingAgainst.get(currentTargetRegion).put(currentOriginRegion, disposed);
		} else {
			attackingAgainst.get(currentTargetRegion).put(currentOriginRegion, attackingAgainst.get(currentTargetRegion).get(currentOriginRegion) + disposed);
		}

	}

	private int calculateOutcomeForAttack(int defending, HashMap<Integer, Integer> attacking, HashMap<Integer, Integer> attackingRemaining) {

		for (Integer i : attacking.keySet()) {
			boolean taken = (defending > 0) ? false : true;
			if (taken) {
				defending -= attacking.get(i);
				attackingRemaining.put(i, 0);
			} else {
				Outcome currentOutcome = Values.calculateAttackOutcome(attacking.get(i), defending);
				defending = currentOutcome.getDefendingArmies();
				if (defending > 0) {
					attackingRemaining.put(i, currentOutcome.getAttackingArmies());
				} else {
					attackingRemaining.put(i, 0);
					defending = -currentOutcome.getAttackingArmies();
				}
			}

		}

		return defending;
	}

}
