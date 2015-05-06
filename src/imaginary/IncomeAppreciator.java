package imaginary;

import java.util.*;

import bot.BotState;
import map.*;
import move.*;

public class IncomeAppreciator {
	private BotState state;
	private ArrayList<Integer> lastPotentialIncome;
	private ArrayList<SuperRegion> lastPotentiallyOwnedSuperRegions;
	private static int currentIncome;

	private static ArrayList<ArrayList<SuperRegion>> powerset(ArrayList<SuperRegion> set) {
		if (set == null)
			return null;
		return powerset(set, 0);
	}

	private static ArrayList<ArrayList<SuperRegion>> powerset(ArrayList<SuperRegion> set, int index) {
		ArrayList<ArrayList<SuperRegion>> powSet;
		// add empty set
		if (index == set.size()) {
			powSet = new ArrayList<ArrayList<SuperRegion>>();
			powSet.add(new ArrayList<SuperRegion>());
		} else {
			SuperRegion first = set.get(index);
			// generate powerset for the rest
			powSet = powerset(set, index + 1);
			ArrayList<ArrayList<SuperRegion>> allSubsets = new ArrayList<ArrayList<SuperRegion>>();
			for (ArrayList<SuperRegion> subset : powSet) {
				ArrayList<SuperRegion> newSubset = new ArrayList<SuperRegion>();
				newSubset.addAll(subset);
				newSubset.add(first);
				allSubsets.add(newSubset);
			}
			powSet.addAll(allSubsets);
		}
		return powSet;
	}

	public IncomeAppreciator(BotState state) {
		this.state = state;
	}

	private int observedIncome() {
		int currentObservedIncome = 0;

		for (Move move : state.getOpponentMoves(state.getRoundNumber())) {
			if (move instanceof PlaceArmiesMove) {
				currentObservedIncome += ((PlaceArmiesMove) move).getArmies();
			}
		}

		return currentObservedIncome;
	}

	private int knownIncome() {
		ArrayList<SuperRegion> knownOwnedSuperRegions = new ArrayList<SuperRegion>();

		for (SuperRegion superRegion : state.getFullMap().getSuperRegions()) {
			if (superRegion.ownedByPlayer(state.getMyOpponentName())) {
				knownOwnedSuperRegions.add(superRegion);
			}
		}

		int currentKnownMinimumIncome = 5;
		for (SuperRegion superRegion : knownOwnedSuperRegions) {
			currentKnownMinimumIncome += superRegion.getArmiesReward();
		}

		return currentKnownMinimumIncome;
	}

	private ArrayList<SuperRegion> potentiallyOwnedSuperRegions() {
		ArrayList<SuperRegion> potentiallyOwnedSuperRegions = new ArrayList<SuperRegion>();

		superRegions: for (SuperRegion superRegion : state.getFullMap().getSuperRegions()) {
			if (!superRegion.ownedByPlayer(state.getMyOpponentName())) {
				for (Region region : superRegion.getSubRegions()) {
					if (region.getVisible() && !region.getPlayerName().equals(state.getMyOpponentName())) {
						continue superRegions;
					}
				}
				potentiallyOwnedSuperRegions.add(superRegion);
			}
		}

		return potentiallyOwnedSuperRegions;
	}

	private ArrayList<Integer> potentialIncome(int minimumIncome) {
		ArrayList<Integer> currentPotentialIncome = new ArrayList<Integer>();

		for (ArrayList<SuperRegion> subset : powerset(potentiallyOwnedSuperRegions())) {
			int subsetPotentialIncome = minimumIncome;
			for (SuperRegion superRegion : subset) {
				subsetPotentialIncome += superRegion.getArmiesReward();
			}
			currentPotentialIncome.add(subsetPotentialIncome);
		}

		Collections.sort(currentPotentialIncome);

		return currentPotentialIncome;
	}

	// If observed income is higher than the penultimate potential income the
	// player must own all potential regions.
	public void evidenceOfOwnership(int currentObservedIncome) {
		// The algorithm can be improved...
		if (lastPotentialIncome.size() > 1 && currentObservedIncome > lastPotentialIncome.get(lastPotentialIncome.size() - 2)) {
			System.err.println("\tThere is inconclusive evidence that the enemy owns hidden regions");
			System.err.println("\tHe placed " + currentObservedIncome + " armies last round");
			System.err.println("\tEnemy must own the following: ");

			for (SuperRegion superRegion : lastPotentiallyOwnedSuperRegions) {
				System.err.println("\t\t" + superRegion);
				for (Region region : superRegion.getSubRegions()) {
					// In case of extremely broken logic - remove the following
					// line and
					// this class wont do anything
					region.setPlayerName(state.getMyOpponentName());
				}
			}
			System.err.println();
		}
	}

	// When this method executes the current map state matches that of the moves
	// made
	// by the opponent that round.
	public void updateMap() {
		int currentKnownMinimumIncome = knownIncome();
		lastPotentialIncome = potentialIncome(currentKnownMinimumIncome);
		lastPotentiallyOwnedSuperRegions = potentiallyOwnedSuperRegions();
	}

	// When this method executes the current map state matches the upcoming
	// round
	public void updateMoves() {
		int currentObservedIncome = observedIncome();

		System.err.println("IncomeAppreciator: ");
		evidenceOfOwnership(currentObservedIncome);

		int currentKnownMinimumIncome = knownIncome();
		// System.err.println("\tcurrentKnownMinimumIncome: " +
		// currentKnownMinimumIncome);
		// System.err.println("\tpotentialIncomes: " +
		// potentialIncome(currentKnownMinimumIncome));
		// System.err.println("\tobservedIncome: " + currentObservedIncome);
	}

	public static int getIncome() {
		return currentIncome;
	}

	public int income() {
		// Known: 8
		// Potential: [8, 10]
		// Observed 9
		// Result: 10

		// Known: 5
		// Potential: [5, 10, 11, 16]
		// Observed: 11
		// Result: 11-16 + He owns the region yielding 6

		// Temporary solution
		int minimum = knownIncome();

		if (observedIncome() > minimum) {
			minimum = observedIncome();
		}

		int maximum = potentialIncome(knownIncome()).get(potentialIncome(knownIncome()).size() - 1);
		currentIncome = minimum + ((maximum - minimum) / 4);

		return currentIncome;

		// if we have observed lets say 9, and the enemy lost nothing, the enemy
		// still has 9 minimum
		// improvements: diff the copy of the previous round map and current. If
		// the player must have lost
		// super regions that can be deducted from their income
	}
}
