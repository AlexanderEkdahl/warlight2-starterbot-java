package imaginary;

import java.util.ArrayList;

import map.Map;
import map.Region;

public class EnemyAppreciator {
	private Map speculativeMap;
	private static EnemyAppreciator appreciator;
	
	private EnemyAppreciator(){
	}
	
	public static EnemyAppreciator getInstance(){
		if (appreciator == null){
			appreciator = new EnemyAppreciator();
		}
		return appreciator;
	}
	
	
	public void setMap(Map map){
		speculativeMap = (Map) map.clone();
	}
	
	public void updateMap(Map map){
		for (Region r : map.getRegionList()){
			if (r.getVisible()){
				Region region = this.speculativeMap.getRegion(r.getId());
				region = (Region) r.clone();
			}
		}
		
	}

	
	public ArrayList<Region> getDefiniteEnemyPositions(){
		return speculativeMap.getEnemyRegions();
	}

}
