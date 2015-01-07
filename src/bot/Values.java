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
}
