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
import concepts.PlacementProposal;
import concepts.Plan;
import bot.BotState;
import bot.Values;

public class DefensiveCommander extends TemplateCommander {

	private ArrayList<PlacementProposal> organizeSuperRegionDefence(Map map) {

		ArrayList<SuperRegion> vulnerable = map.getOwnedFrontSuperRegions();

		ArrayList<PlacementProposal> placementProposals = new ArrayList<PlacementProposal>();
		for (SuperRegion s : vulnerable) {
			double weight = calculateWeight(s);
			Region mostVulnerableRegion = null;

			HashMap<Region, Integer> ownForces = new HashMap<Region, Integer>();

			for (Region r : s.getFronts()) {
				ownForces.put(r, 1);
			}
			// keep on assigning dudes until we have at least extraArmiesDefence
			// more in every region
			// and add them in the most vulnerable region

			int minDiff = Integer.MAX_VALUE;
			for (Region r : s.getFronts()) {
				int currentDiff = ownForces.get(r) - r.getHighestThreateningForce();
				if (currentDiff < minDiff) {
					minDiff = currentDiff;
					mostVulnerableRegion = r;
				}

			}
			while (minDiff < Values.extraArmiesDefence) {
				minDiff = Integer.MAX_VALUE;
				for (Region r : s.getFronts()) {
					int currentDiff = ownForces.get(r) - r.getHighestThreateningForce();
					if (currentDiff < minDiff) {
						minDiff = currentDiff;
						mostVulnerableRegion = r;
					}

				}
				placementProposals.add(new PlacementProposal(weight, mostVulnerableRegion,
						new Plan(mostVulnerableRegion, mostVulnerableRegion.getSuperRegion()), 1, "DefensiveCommander"));
				ownForces.put(mostVulnerableRegion, ownForces.get(mostVulnerableRegion) + 1);

			}

		}
		return placementProposals;
	}

	private double calculateWeight(SuperRegion s) {
		double worth = calculateWorth(s);
		double cost = calculateCost(s);
		double weight = worth / cost;
		return weight;
	}

	private double calculateWorth(SuperRegion s) {
		double worth = Values.rewardDefenseImportanceMultiplier * s.getArmiesReward();
		return worth;
	}

	private double calculateCost(SuperRegion s) {
		double cost = (s.getFronts().size() * Values.multipleFrontPenalty) + (s.getTotalThreateningForce() * Values.costMultiplierDefendingAgainstEnemy);
		return cost;
	}

	@Override
	public ArrayList<ActionProposal> getActionProposals(Map map, Set<Region> available) {

		ArrayList<ActionProposal> proposals = new ArrayList<ActionProposal>();
		ArrayList<Region> fronts = map.getOwnedFrontRegions();
//		ArrayList<Region> rewardBlockers = map.getRewardBlockers();
		HashMap<Region, Integer> needDefence = new HashMap<Region, Integer>();

		for (Region r : fronts) {
			// for all the interesting regions, calculate if they defense
			needDefence.put(r, Values.calculateRequiredForcesDefend(r));

		}
		
		Pathfinder pathfinder = new Pathfinder(map, new PathfinderWeighter() {
			public double weight(Region nodeA, Region nodeB) {
				return Values.calculateRegionWeighedCost(nodeB);

			}
		});

		for (Region r : available) {
			// if this region is in need of defence and has too few currently on
			// it to defend, don't fucking attack anyone else you dipshit, at
			// least that's what I think but hey I'm just the defensivecommander
			// who cares what I think
			// if it needs to be defended set a proposal to contain the needed
			// amount of forces
			if (fronts.contains(r)) {
				int disposed = needDefence.get(r);
				proposals.add(new ActionProposal(calculateWeight(r.getSuperRegion()), r, r, disposed, new Plan(r, r.getSuperRegion()), "DefensiveCommander"));
			}

			else {
				ArrayList<Path> paths = pathfinder.getPathToRegionsFromRegion(r, fronts);
				for (Path path : paths) {
					int totalRequired = needDefence.get(path.getTarget());
					if (totalRequired < 1) {
						continue;
					}
					double currentCost = path.getDistance() + calculateCost(path.getTarget().getSuperRegion());
					double currentWorth = calculateWorth(path.getTarget().getSuperRegion());
					double currentWeight = currentWorth / currentCost;

					for (int i = 1; i < path.getPath().size(); i++) {
						totalRequired += Values.calculateRequiredForcesAttack(path.getPath().get(i));
					}
					proposals.add(new ActionProposal(currentWeight, r, path.getPath().get(1), totalRequired, new Plan(path.getTarget(), path.getTarget()
							.getSuperRegion()), "DefensiveCommander"));

				}
			}

		}

		return proposals;
	}

}
