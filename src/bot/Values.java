package bot;

import java.util.ArrayList;

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

public static int calculateRequiredForcesAttack(int armySize){
	int forces = 0;
	
	if (armySize <= 3){
		return armySize + 2;
	}
	else if (armySize <= 5){
		return armySize + 4;
	}
	
	// small chance of success
	forces[0] = r.getArmies() + 1;
	// good chance of success
	forces[1] = (int) (r.getArmies() * 1.5 + 2);
	// certain success
	forces[2] = r.getArmies() * 2 + 2;
	return forces;
	
}

public static int[] calculateRequiredForces(SuperRegion s){
	int[] 
	return null;
	
}
}
