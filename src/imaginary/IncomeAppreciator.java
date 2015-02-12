package imaginary;

import java.util.*;

import bot.BotState;
import map.*;
import move.*;

public class IncomeAppreciator {
  private BotState state;
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

  private void observedIncome() {
    int currentObservedIncome = 0;

    for (Move move : state.getOpponentMoves(state.getRoundNumber())) {
      if (move instanceof PlaceArmiesMove) {
        currentObservedIncome += ((PlaceArmiesMove)move).getArmies();
      }
    }

    observedIncome.add(currentObservedIncome);
    System.err.println("IncomeAppreciator: last observedIncome: " + currentObservedIncome);
  }

  private void knownIncome() {
    ArrayList<SuperRegion> knownOwnedSuperRegions = new ArrayList<SuperRegion>();

    for (SuperRegion superRegion : state.getFullMap().getSuperRegions()) {
      if (superRegion.ownedByPlayer(state.getMyOpponentName())) {
        knownOwnedSuperRegions.add(superRegion);
      }
    }

    int currentknownMinimumIncome = 5;
    for (SuperRegion superRegion : knownOwnedSuperRegions) {
      currentknownMinimumIncome += superRegion.getArmiesReward();
    }

    knownMinimumIncome.add(currentknownMinimumIncome);
    System.err.println("IncomeAppreciator: last knownOwnedSuperRegions: " + knownOwnedSuperRegions);
  }

  private void potentialIncome() {
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

    ArrayList<Integer> currentPotentialIncome = new ArrayList<Integer>();
    for (ArrayList<SuperRegion> subset : powerset(potentiallyOwnedSuperRegions)) {
      int subsetPotentialIncome = knownMinimumIncome.get(knownMinimumIncome.size() - 1);
      for (SuperRegion superRegion : subset) {
        subsetPotentialIncome += superRegion.getArmiesReward();
      }
      currentPotentialIncome.add(subsetPotentialIncome);
    }
    potentialIncomes.add(currentPotentialIncome);
    System.err.println("IncomeAppreciator: last potentialIncomes: " + currentPotentialIncome);
  }

  // this is not good enough... there should be an update before update_map in order
  // to calculate diff
  public void update() {
    knownIncome();
    potentialIncome();
    observedIncome();
  }

  public int income() {
    // TODO
    return 5;
  }
}
