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

	private static Tables tables;
	private static HashMap<Integer, Double> internalHopsPenalty;
	private static HashMap<Integer, Double> sizePenalty;
	private static HashMap<Integer, Double> deficitDefenceExponentialMultiplier;
	private static HashMap<Integer, Double> enemyVicinityExponentialPenalty;
	private static HashMap<Integer, Double> multipleFrontExponentialPenalty;

	private Tables() {
		internalHopsPenalty = new HashMap<Integer, Double>();
		sizePenalty = new HashMap<Integer, Double>();
		deficitDefenceExponentialMultiplier = new HashMap<Integer, Double>();
		enemyVicinityExponentialPenalty = new HashMap<Integer, Double>();
		multipleFrontExponentialPenalty = new HashMap<Integer, Double>();
	}

	public static Tables getInstance() {
		if (tables == null) {
			tables = new Tables();
		}
		return tables;

	};

	public void introCalculation(Map map) {
		for (SuperRegion s : map.getSuperRegions()) {
			sizePenalty.put(s.getId(), Math.pow(Values.superRegionSizeExponentialPenalty, (double) s.getSubRegions().size()));
			internalHopsPenalty.put(s.getId(), Math.pow(Values.internalHopsExponentialPenalty, (double) calculateMaxInternalHops(s, map)));
		}

		for (int i = 0; i <= maxCalc; i++) {
			deficitDefenceExponentialMultiplier.put(i, Math.pow(Values.deficitDefenceExponentialMultiplier, i));
			enemyVicinityExponentialPenalty.put(i, Math.pow(Values.enemyVicinityExponentialPenalty, i));
			multipleFrontExponentialPenalty.put(i, Math.pow(Values.multipleFrontExponentialPenalty, i));
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

	public Double getInternalHopsPenaltyFor(SuperRegion s) {
		return internalHopsPenalty.get(s.getId());
	}

	public Double getSizePenaltyFor(SuperRegion s) {
		return sizePenalty.get(s.getId());
	}

	public Double getEnemyVicinityExponentialPenaltyFor(int i) {
		i = Math.min(i, maxCalc);
		return enemyVicinityExponentialPenalty.get(i);
	}

	public Double getDeficitDefenceExponentialMultiplierFor(int i) {
		i = Math.min(i, maxCalc);
		return deficitDefenceExponentialMultiplier.get(i);
	}
	public Double getMultipleFrontExponentialPenaltyFor(int i) {
		i = Math.min(i, maxCalc);
		return multipleFrontExponentialPenalty.get(i);
	}

}
