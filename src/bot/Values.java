package bot;

import java.util.ArrayList;
import java.util.LinkedList;

import map.Region;
import map.SuperRegion;

public class Values {
  private static float startingRegion(int neutrals, int reward) {
    if (reward == 0) {
      return Integer.MIN_VALUE;
    }

    return (reward * 3) - neutrals;
  }

public static Region getBestStartRegion(ArrayList<Region> pickableStartingRegions) {
	Region maxRegion = null;
	float maxValue = Integer.MIN_VALUE;
	for (Region currentRegion : pickableStartingRegions) {
		SuperRegion superRegion = currentRegion.getSuperRegion();

		float value = Values.startingRegion(superRegion.getInitialNeutralCount(), superRegion.getArmiesReward());
		if (value > maxValue) {
			maxValue = value;
			maxRegion = currentRegion;
		}
	}
	
	return maxRegion;
	
}

public static int calculateRequiredForcesAttack(Region r){
	
	// these numbers will be prone to change
	int armySize = r.getArmies();
	if (armySize <= 2){
		return armySize +1;
	}
	if (armySize <= 3){
		return armySize + 2;
	}
	else if (armySize <= 5){
		return armySize + 3;
	}
	else{
		return (int) (armySize * 1.5);
	}

	
}

public static int calculateRequiredForcesAttack(SuperRegion s){
	int totalRequired = 0;
	ArrayList<Region> regions = s.getSubRegions();
	
	// must leave one dude on each tile
	totalRequired += regions.size();
	
	for (Region r : regions){
		totalRequired += calculateRequiredForcesAttack(r);
	}
	
	
	return totalRequired;
	
}
}
