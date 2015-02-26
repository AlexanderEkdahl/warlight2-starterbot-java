package commanders;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;

import map.Map;
import map.Pathfinder;
import map.Pathfinder.Path;
import map.PathfinderWeighter;
import map.Region;
import map.SuperRegion;
import concepts.ActionProposal;
import concepts.Plan;
import bot.Values;

public class DefensiveCommander implements TemplateCommander {

	private double calculateWeight(Region r, HashMap<SuperRegion, Double> superRegionWorths, HashMap<SuperRegion, Double> superRegionCosts,
			HashMap<Integer, Integer> needDefence) {
		double worth = Math.pow((Double) Values.deficitDefenceExponentialMultiplier,
				needDefence.get(r.getId()) * superRegionWorths.get(r.getSuperRegion()));
		double cost = superRegionCosts.get(r.getSuperRegion());
		double weight = worth / cost;
		return weight;
	}

	private double calculateWorth(SuperRegion s) {
		double worth = Values.rewardDefenseImportanceMultiplier * s.getArmiesReward();
		return worth;
	}

	private HashMap<SuperRegion, Double> calculateWorths(Map map) {
		HashMap<SuperRegion, Double> worths = new HashMap<SuperRegion, Double>();
		for (SuperRegion s : map.getSuperRegions()) {
			worths.put(s, calculateWorth(s));
		}
		return worths;
	}

	private double calculateCost(SuperRegion s) {
		double cost = (s.getFronts().size() * Values.multipleFrontPenalty) + (s.getTotalThreateningForce() * Values.costMultiplierDefendingAgainstEnemy);
		return cost;
	}

	private HashMap<SuperRegion, Double> calculateCosts(Map map) {
		HashMap<SuperRegion, Double> costs = new HashMap<SuperRegion, Double>();
		for (SuperRegion s : map.getSuperRegions()) {
			costs.put(s, calculateCost(s));
		}
		return costs;
	}

	@Override
	public ArrayList<ActionProposal> getActionProposals(Map map, Set<Integer> available, Pathfinder pathfinder) {

		ArrayList<ActionProposal> proposals = new ArrayList<ActionProposal>();
		ArrayList<Region> fronts = map.getOwnedFrontRegions();
		HashMap<Integer, Integer> needDefence = new HashMap<Integer, Integer>();
		ArrayList<Region> needDefenceRegions = new ArrayList<Region>();
		HashMap<SuperRegion, Double> superRegionWorths = calculateWorths(map);
		HashMap<SuperRegion, Double> superRegionCosts = calculateCosts(map);

		for (Region r : fronts) {
			// for all the interesting regions, calculate if they defense
			needDefence.put(r.getId(), Values.calculateRequiredForcesDefend(r) - r.getArmies());
			needDefenceRegions.add(r);
		}

		for (Integer r : available) {
			// if this region is in need of defence and has too few currently on
			// it to defend, don't fucking attack anyone else you dipshit, at
			// least that's what I think but hey I'm just the defensivecommander
			// who cares what I think
			// if it needs to be defended set a proposal to contain the needed
			// amount of forces
			if (needDefence.get(r) != null) {
				int disposed = needDefence.get(r);
				double weight = calculateWeight(map.getRegion(r), superRegionWorths, superRegionCosts, needDefence);
				proposals.add(new ActionProposal(weight, map.getRegion(r), map.getRegion(r), disposed, new Plan(map.getRegion(r), map.getRegion(r)
						.getSuperRegion()), "DefensiveCommander"));
			}

//			else {
//				ArrayList<Path> paths = pathfinder.getPathToRegionsFromRegion(map.getRegion(r), needDefenceRegions);
//				for (Path path : paths) {
//					int totalRequired = needDefence.get(path.getTarget().getId());
//					if (totalRequired < 1) {
//						continue;
//					}
//					double currentCost = path.getDistance() + superRegionCosts.get(path.getTarget().getSuperRegion());
//					double currentWorth = superRegionWorths.get(path.getTarget().getSuperRegion());
//					double currentWeight = currentWorth / currentCost;
//
//					for (int i = 1; i < path.getPath().size(); i++) {
//						totalRequired += Values.calculateRequiredForcesAttack(path.getPath().get(i));
//					}
//					proposals.add(new ActionProposal(currentWeight, map.getRegion(r), path.getPath().get(1), totalRequired, new Plan(path.getTarget(), path
//							.getTarget().getSuperRegion()), "DefensiveCommander"));
//
//				}
//			}

		}

		return proposals;
	}

}
