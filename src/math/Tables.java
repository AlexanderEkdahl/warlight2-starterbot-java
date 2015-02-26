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
	private static Tables tables;
	private static HashMap<Integer, Double> internalHopsPenalty;
	private static HashMap<Integer, Double> sizePenalty;

	private Tables(){
		internalHopsPenalty = new HashMap<Integer, Double>();
		sizePenalty = new HashMap<Integer, Double>();
	}
	
	public static Tables getInstance() {
		if (tables == null) {
			tables = new Tables();
		}
		return tables;

	};
	
	public void introCalculation(Map map){
		for (SuperRegion s : map.getSuperRegions()){
			sizePenalty.put(s.getId(), Math.pow(Values.superRegionExponentialMultiplier, (double) s.getSubRegions().size()));
			internalHopsPenalty.put(s.getId(), Math.pow(Values.internalHopsExponentialPenalty, (double) calculateMaxInternalHops(s, map)));
			
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

	public void setInternalHopsPenalty(HashMap<Integer, Double> internalHopsPenalty) {
		Tables.internalHopsPenalty = internalHopsPenalty;
	}

	public Double getSizePenaltyFor(SuperRegion s) {
		return sizePenalty.get(s.getId());
	}

	public void setSizePenalty(HashMap<Integer, Double> sizePenalty) {
		Tables.sizePenalty = sizePenalty;
	}



}
