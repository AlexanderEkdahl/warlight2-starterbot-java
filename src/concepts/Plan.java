package concepts;

import map.Region;
import map.SuperRegion;
import java.util.HashSet;

public class Plan implements Comparable<Plan> {
	private Region region; // TODO: Should be removed
	private SuperRegion superRegion; // TODO: Should be removed
	private HashSet<Region> regions;
	private float weight = 0;

	public Plan(Region region, SuperRegion superRegion) {
		this.region = region; // TODO: Should be removed
		this.superRegion = superRegion; // TODO: Should be removed
		regions = new HashSet<Region>();
		regions.add(region);
		regions.addAll(superRegion.getSubRegions());
	}

	public Plan(SuperRegion sr, int weight) {
		this.superRegion = superRegion; // TODO: Should be removed
		this.weight = weight; // TODO: Should be removed
		regions = new HashSet<Region>(sr.getSubRegions());
	}

	public float getWeight() {
		return weight;
	}

	public void setWeight(float weight) {
		this.weight = weight;
	}

	// TODO: Should be removed
	public SuperRegion getSuperRegion() {
		return superRegion;
	}

	// TODO: Should be removed
	public Region getRegion(){
		return region;
	}

	public String toString(){
		String s = "Region(";
		int i = 0;
		for (Region region : regions) {
			s += region.getId();
			if (++i != regions.size()) {
				s += ", ";
			}
		}

		return s + ")";
	}

	// Returns the number of regions that the two plans have in commmon
	public int similarity(Plan otherPlan) {
		HashSet<Region> intersection = (HashSet<Region>) regions.clone();
		intersection.retainAll(otherPlan.regions);
		return intersection.size();
	}

	@Override
	public int compareTo(Plan otherPlan) {
		if (otherPlan.getWeight() > weight) {
			return 1;
		} else if (otherPlan.getWeight() == weight) {
			return 0;
		} else {
			return -1;
		}
	}

}
