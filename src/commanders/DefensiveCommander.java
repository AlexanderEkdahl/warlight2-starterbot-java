package commanders;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import map.Map;
import map.Pathfinder;
import map.Pathfinder.Path;
import map.PathfinderWeighter;
import map.Region;
import map.SuperRegion;
import math.Tables;
import concepts.ActionProposal;
import concepts.Plan;
import bot.Values;

public class DefensiveCommander implements TemplateCommander {

	private double calculateWeight(Region r, HashMap<SuperRegion, Double> superRegionWorths, HashMap<SuperRegion, Double> superRegionCosts,
			HashMap<Integer, Integer> needDefence) {
		Tables tables = Tables.getInstance();
		double worth = tables.getDeficitDefenceExponentialMultiplierFor(needDefence.get(r.getId())) * superRegionWorths.get(r.getSuperRegion());
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

	private HashMap<SuperRegion, Double> calculateCosts(Map map) {
		HashMap<SuperRegion, Double> costs = new HashMap<SuperRegion, Double>();
		for (SuperRegion s : map.getSuperRegions()) {
			costs.put(s, Values.calculateSuperRegionWeighedCost(s, map));
		}
		return costs;
	}

	@Override
	public ArrayList<ActionProposal> getActionProposals(Map map, Set<Integer> available, Pathfinder pathfinder) {

		ArrayList<ActionProposal> proposals = new ArrayList<ActionProposal>();
		ArrayList<Region> fronts = map.getOwnedFrontRegions();
		ArrayList<Region> rewardBlockers = map.getOwnedRewardBlockers();
		HashMap<Integer, Integer> needDefence = new HashMap<Integer, Integer>();
		ArrayList<Region> needDefenceRegions = new ArrayList<Region>();
		HashMap<SuperRegion, Double> superRegionWorths = calculateWorths(map);
		HashMap<SuperRegion, Double> superRegionCosts = calculateCosts(map);

		for (Region r : fronts) {
			// for all the interesting regions, calculate if they defense
			int need = Math.max(Values.calculateRequiredForcesDefend(r) - r.getArmies(), 0);
			needDefence.put(r.getId(), need);
			needDefenceRegions.add(r);

		}
		for (Region r : rewardBlockers) {
			int need = Math.max(Values.calculateRequiredForcesDefendRewardBlocker(r) - r.getArmies(), 0);
			needDefence.put(r.getId(), need);
			needDefenceRegions.add(r);
		}

		for (Integer r : available) {
			// if this region is in need of defence and has too few currently on
			// it to defend, don't fucking attack anyone else you dipshit, at
			// least that's what I think but hey I'm just the defensivecommander
			// who cares what I think
			// if it needs to be defended set a proposal to contain the needed
			// amount of forces
			if (needDefence.get(r) != null && needDefence.get(r) > 0) {
				int disposed = needDefence.get(r);
				if (Values.defensiveCommanderUseSmallPlacements) {
					disposed = 1;
				}
				double weight = calculateWeight(map.getRegion(r), superRegionWorths, superRegionCosts, needDefence);
				proposals.add(new ActionProposal(weight, map.getRegion(r), map.getRegion(r), disposed, new Plan(map.getRegion(r), map.getRegion(r)
						.getSuperRegion()), "DefensiveCommander"));
			}

			else {
				ArrayList<Path> paths = pathfinder.getPathToRegionsFromRegion(map.getRegion(r), needDefenceRegions);
				for (Path path : paths) {
					int totalRequired = needDefence.get(path.getTarget().getId());
					if (totalRequired < 1) {
						continue;
					}
					double currentCost = path.getDistance() + superRegionCosts.get(path.getTarget().getSuperRegion());
					double currentWorth = superRegionWorths.get(path.getTarget().getSuperRegion());
					double currentWeight = currentWorth / currentCost;
					ArrayList<Region> regionsAttacked = new ArrayList(path.getPath());
					regionsAttacked.remove(0);
					totalRequired += Values.calculateRequiredForcesForRegions(regionsAttacked);
					proposals.add(new ActionProposal(currentWeight, map.getRegion(r), path.getPath().get(1), totalRequired, new Plan(path.getTarget(), path
							.getTarget().getSuperRegion()), "DefensiveCommander"));

				}
			}

		}

		return proposals;
	}

}
