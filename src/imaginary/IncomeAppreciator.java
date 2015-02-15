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

    System.err.println("IncomeAppreciator: observedIncome: " + currentObservedIncome);

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

    System.err.println("IncomeAppreciator: knownOwnedSuperRegions: " + knownOwnedSuperRegions + " with a minimum income of: " + currentKnownMinimumIncome);

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
    System.err.println("IncomeAppreciator: potentialIncomes: " + currentPotentialIncome);

    return currentPotentialIncome;
  }

  // this is not good enough... there should be an update before update_map in order
  // to calculate diff
  // The information given from update_map represents the income that will be given for the next round

  // in order to make educated guesses about enemy income and their observed income
  // the map has to be stored. The update_map gives information about the round to come but does not
  // represent the potential/actual income the player had when he placed his units

  // solution. save a copy of the previous map
  // calculcate potential and whatnot, see if there is any conclusive evidence of any super regions

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
    int lastObservedIncome = observedIncome();
    // If observed income is higher than the penultimate potential income the
    // player must own all potential regions.
    if (lastPotentialIncome.size() > 1
      && lastObservedIncome > lastPotentialIncome.get(lastPotentialIncome.size() - 2)) {
        System.err.println("AWESOME! - There is inconclusive evidence that the enemy owns hidden regions")
    }
    observedIncome.add(lastObservedIncome);

    int currentKnownMinimumIncome = knownIncome();
    knownMinimumIncome.add(currentKnownMinimumIncome);
    potentialIncomes.add(potentialIncome(currentKnownMinimumIncome));
  }

  public int income() {
    // TODO
    return 5;
  }
}
