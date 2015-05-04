package commanders;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;

import map.Map;
import map.Pathfinder;
import map.Pathfinder.Path;
import map.Region;
import map.SuperRegion;
import math.Tables;
import concepts.ActionProposal;
import concepts.Plan;
import bot.BotState;
import bot.Values;

public class DefensiveCommander {

	private double calculateWorth(Region r, HashMap<Region, Double> regionWorths,HashMap<Integer, Integer> needDefence) {
		Tables tables = Tables.getInstance();
		double worth = tables.getDeficitDefenceExponentialMultiplierFor(needDefence.get(r.getId())) * regionWorths.get(r);
		return worth;
	}

	private double calculateWorth(SuperRegion s) {
		double worth = Values.rewardDefenseImportanceMultiplier * s.getArmiesReward();
		return worth;
	}

	private HashMap<SuperRegion, Double> calculateWorths(Map map) {
		HashMap<SuperRegion, Double> worths = new HashMap<SuperRegion, Double>();
		for (SuperRegion s : map.getOwnedSuperRegions(BotState.getMyName())) {
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

	private ArrayList<Region> calculateDefenceInheritance(Map map, HashMap<Region, Double> regionWorths, HashMap<Region, Double> regionCosts) {
		ArrayList<Region> inherited = new ArrayList<Region>();
		ArrayList<Region> frontRegionsNotInOwnedSuperRegion = map.getOwnedFrontRegions();
		frontRegionsNotInOwnedSuperRegion.removeAll(map.getOwnedSuperRegionRegions());

		for (Region r : frontRegionsNotInOwnedSuperRegion) {
			for (Region n : r.getNeighbors()) {
				if (map.getOwnedSuperRegionRegions().contains(n.getSuperRegion())) {
					// if here then this region is protecting owned superregions
					if (regionWorths.get(r) == null) {
						regionWorths.put(r, regionWorths.get(n) * Values.rewardDefenseInheritanceMultiplier);
						regionCosts.put(r, regionCosts.get(n));
					} else {
						regionWorths.put(r, regionWorths.get(r) + (regionWorths.get(n) * Values.rewardDefenseInheritanceMultiplier));
						regionCosts.put(r, regionCosts.get(r));
					}

					if (!inherited.contains(r)) {
						inherited.add(r);
					}

					if (r.getSuperRegion().ownedByPlayer(BotState.getMyName())) {
						System.err
								.println("MAJOR MALFUNCTION calculateDefenceInheritance IS CALCULATING DEFENCE INHERITANCE FOR REGIONS IN OWNED SUPERREGIONS");

						break;
					}
				}

			}

		}

		return inherited;
	}

	public ArrayList<ActionProposal> getActionProposals(Map map, Set<Integer> interesting, Pathfinder pathfinder, HashMap<Integer, Integer> currentlyDefending,
			HashMap<Integer, Integer> availableForces) {

		ArrayList<ActionProposal> proposals = new ArrayList<ActionProposal>();
		ArrayList<Region> interestingFronts = map.getOwnedSuperRegionFrontRegions();
		HashMap<Integer, Integer> needDefence = new HashMap<Integer, Integer>();
		HashMap<SuperRegion, Double> superRegionWorths = calculateWorths(map);
		HashMap<SuperRegion, Double> superRegionCosts = calculateCosts(map);
		HashMap<Region, Double> regionWorths = new HashMap<Region, Double>();
		HashMap<Region, Double> regionCosts = new HashMap<Region, Double>();

		for (Region r : map.getRegionList()) {
			regionWorths.put(r, 0d);
			regionCosts.put(r, 0d);
		}

		for (SuperRegion sr : map.getOwnedSuperRegions(BotState.getMyName())) {
			for (Region r : sr.getSubRegions()) {
				regionWorths.put(r, superRegionWorths.get(r.getSuperRegion()));
				regionCosts.put(r, superRegionCosts.get(r.getSuperRegion()));
			}
		}

		// add inheritancedefence
		ArrayList<Region> inheritedDefenceRegions = calculateDefenceInheritance(map, regionWorths, regionCosts);
		interestingFronts.addAll(inheritedDefenceRegions);

		for (Region r : interestingFronts) {
			// for all the interesting regions, calculate if they defense
			int need = Math.max(Values.calculateRequiredForcesDefend(r) - currentlyDefending.get(r.getId()), 0);
			needDefence.put(r.getId(), need);

		}

		for (Integer r : interesting) {
			if (needDefence.get(r) != null && needDefence.get(r) > 0) {
				int disposed = needDefence.get(r);
				if (Values.tryToUseSmallDefensivePlacements && Values.defensiveCommanderUseSmallPlacements) {
					disposed = 1;
				}
				double worth = calculateWorth(map.getRegion(r), regionWorths,needDefence);

				int needPlaced = disposed;
				double currentCost = regionCosts.get(map.getRegion(r));
				double weight = worth / currentCost;
				proposals.add(new ActionProposal(weight, map.getRegion(r), map.getRegion(r), disposed, new Plan(map.getRegion(r), map.getRegion(r)
						.getSuperRegion()), "DefensiveCommander"));
			} else {
				ArrayList<Path> paths = pathfinder.getPathToRegionsFromRegion(map.getRegion(r), interestingFronts);
				for (Path path : paths) {
					int totalRequired = needDefence.get(path.getTarget().getId());
					if (totalRequired < 1) {
						continue;
					}
					ArrayList<Region> regionsAttacked = new ArrayList<Region>(path.getPath());
					regionsAttacked.remove(0);
					totalRequired += Values.calculateRequiredForcesForRegions(regionsAttacked);
					totalRequired = Math.min(availableForces.get(r), totalRequired);
					double currentCost = Values.calculatePathCost(regionsAttacked, totalRequired, availableForces.get(r));
					double currentWorth = calculateWorth(path.getTarget(), regionWorths, needDefence);
					double currentWeight = currentWorth / currentCost;

					proposals.add(new ActionProposal(currentWeight, map.getRegion(r), path.getPath().get(1), totalRequired, new Plan(path.getTarget(), path
							.getTarget().getSuperRegion()), "DefensiveCommander"));

				}
			}

		}

		return proposals;
	}

}
