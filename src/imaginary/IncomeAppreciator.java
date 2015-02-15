package imaginary;

import java.util.*;

import bot.BotState;
import map.*;
import move.*;

public class IncomeAppreciator {
  private BotState state;
  private ArrayList<Integer> lastPotentialIncome;
  private ArrayList<ArrayList<Integer>> potentialIncomes;
  private ArrayList<Integer> observedIncome;
  private ArrayList<Integer> knownMinimumIncome;

  private static ArrayList<ArrayList<SuperRegion>> powerset(ArrayList<SuperRegion> set) {
    if (set == null) return null;
    return powerset(set, 0);
  }

  private static ArrayList<ArrayList<SuperRegion>> powerset(ArrayList<SuperRegion> set, int index) {
    ArrayList<ArrayList<SuperRegion>> powSet;
    //add empty set
    if (index == set.size()) {
      powSet = new ArrayList<ArrayList<SuperRegion>>();
      powSet.add(new ArrayList<SuperRegion>());
    } else {
      SuperRegion first = set.get(index);
      //generate powerset for the rest
      powSet = powerset(set, index+1);
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
    potentialIncomes = new ArrayList<ArrayList<Integer>>();
    observedIncome = new ArrayList<Integer>();
    knownMinimumIncome = new ArrayList<Integer>();
  }

  private int observedIncome() {
    int currentObservedIncome = 0;

    for (Move move : state.getOpponentMoves(state.getRoundNumber())) {
      if (move instanceof PlaceArmiesMove) {
        currentObservedIncome += ((PlaceArmiesMove)move).getArmies();
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

    superRegions:
    for (SuperRegion superRegion : state.getFullMap().getSuperRegions()) {
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

  // if we have observed lets say 9, and the enemy lost nothing, the enemy still has 9 minimum
  // improvements: diff the copy of the previous round map and current. If the player must have lost
  // super regions that can be deducted from their income

  // When this method executes the current map state matches that of the moves made
  // by the opponent that round.
  public void updateMap() {
    int currentKnownMinimumIncome = knownIncome();
    lastPotentialIncome = potentialIncome(currentKnownMinimumIncome);
  }

  // When this method executes the current map state matches the upcoming round
  public void updateMoves() {
    int currentObservedIncome = observedIncome();

    // If observed income is higher than the penultimate potential income the
    // player must own all potential regions.
    if (lastPotentialIncome.size() > 1
      && currentObservedIncome > lastPotentialIncome.get(lastPotentialIncome.size() - 2)) {
        System.err.println("AWESOME! - There is inconclusive evidence that the enemy owns hidden regions");
    }
    observedIncome.add(currentObservedIncome);

    int currentKnownMinimumIncome = knownIncome();
    knownMinimumIncome.add(currentKnownMinimumIncome);
    potentialIncomes.add(potentialIncome(currentKnownMinimumIncome));
    System.err.println("IncomeAppreciator: ");
    System.err.println("\tcurrentKnownMinimumIncome: " + currentKnownMinimumIncome);
    System.err.println("\tpotentialIncomes: " + potentialIncome(currentKnownMinimumIncome));
    System.err.println("\tobservedIncome: " + currentObservedIncome);
  }

  public int income() {
    // Known 8
    // Potential 8, 10
    // Observed 9
    return 5;
  }
}
