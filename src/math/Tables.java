package math;

import java.util.ArrayList;
import java.util.HashMap;

import bot.Values;
import map.Map;
import map.Pathfinder;
import map.PathfinderWeighter;
import map.Region;
import map.SuperRegion;
import map.Pathfinder.Path;

public class Tables {
	private static final int maxCalc = 30;
	private static final int minCalc = -10;

	private static Tables tables;
	private static HashMap<Integer, Double> internalHopsPenalty;
	private static HashMap<Integer, Double> sizePenalty;
	private static double[] deficitDefenceExponentialMultiplier;
	private static double[] enemyVicinityExponentialPenalty;
	private static double[] turnsNeededToTakeExponentialPenalty;

	private Tables() {
		internalHopsPenalty = new HashMap<Integer, Double>();
		sizePenalty = new HashMap<Integer, Double>();
		deficitDefenceExponentialMultiplier = new double[maxCalc+1];
		enemyVicinityExponentialPenalty = new double[maxCalc+1];
		turnsNeededToTakeExponentialPenalty = new double[maxCalc+1];
	}

	public static Tables getInstance() {
		if (tables == null) {
			tables = new Tables();
		}
		return tables;

	};

	public void introCalculation(Map map) {
		for (SuperRegion s : map.getSuperRegions()) {
			sizePenalty.put(s.getId(), Math.pow(Values.superRegionSizeExponentialPenalty, s.getSubRegions().size()));
			internalHopsPenalty.put(s.getId(), Math.pow(Values.internalHopsExponentialPenalty, calculateMaxInternalHops(s, map)));
		}

		for (int i = 0; i <= maxCalc; i++) {
			deficitDefenceExponentialMultiplier[i] = Math.pow(Values.deficitDefenceExponentialMultiplier, i);
			enemyVicinityExponentialPenalty[i] = Math.pow(Values.enemyVicinityExponentialPenalty, i);
			turnsNeededToTakeExponentialPenalty[i] = Math.pow(Values.turnsNeededToTakeExponentialPenalty, i);
		}

	}

	private int calculateMaxInternalHops(SuperRegion sr, Map map) {
		Pathfinder pathfinder = new Pathfinder(map, new PathfinderWeighter() {
			public double weight(Region nodeA, Region nodeB) {
				return 1;

			}
		});
		int maxHops = 0;
		ArrayList<Region> targetRegions;
		for (Region r : sr.getSubRegions()) {
			targetRegions = (ArrayList<Region>) sr.getSubRegions().clone();
			targetRegions.remove(r);
			ArrayList<Path> paths = pathfinder.getPathToRegionsFromRegion(r, targetRegions);
			for (Path p : paths) {
				if (p.getDistance() > maxHops) {
					maxHops = (int) p.getDistance();
				}
			}
		}
		return maxHops;
	}

	private double determineOut(int i, double[] array) {
		i = Math.max(minCalc, Math.min(i, maxCalc));
		double returnValue = (i<0) ? 1/array[-i] : array[i];
		return returnValue;
		
	}

	public Double getInternalHopsPenaltyFor(SuperRegion s) {
		return internalHopsPenalty.get(s.getId());
	}

	public Double getSizePenaltyFor(SuperRegion s) {
		return sizePenalty.get(s.getId());
	}

	public Double getEnemyVicinityExponentialPenaltyFor(int i) {
		return determineOut(i,enemyVicinityExponentialPenalty);
	}

	public Double getDeficitDefenceExponentialMultiplierFor(int i) {
		return determineOut(i,deficitDefenceExponentialMultiplier);

	}
	
	public Double getTurnsNeededToTakeExponentialPenaltyFor(int i){
		return determineOut(i,turnsNeededToTakeExponentialPenalty);
	}


}
