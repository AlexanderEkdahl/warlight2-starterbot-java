package commanders;

import java.util.ArrayList;
import java.util.HashMap;

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

	@Override
	public ArrayList<PlacementProposal> getPlacementProposals(BotState state) {
		// pockets are solitary tiles without connection to friendly tiles

		ArrayList<SuperRegion> vulnerableSuperRegions = state.getFullMap().getOwnedFrontSuperRegions(state);

		ArrayList<PlacementProposal> proposals = calculatePlans(state, vulnerableSuperRegions);
		return proposals;
	}

	private ArrayList<PlacementProposal> calculatePlans(BotState state, ArrayList<SuperRegion> vulnerableSuperRegions) {
		ArrayList<PlacementProposal> placementProposals = new ArrayList<PlacementProposal>();

		// placementProposals.addAll(organizePocketDefence(state));
		placementProposals.addAll(organizeSuperRegionDefence(state));

		return placementProposals;
	}

	private ArrayList<PlacementProposal> organizeSuperRegionDefence(BotState state) {

		ArrayList<SuperRegion> vulnerable = state.getFullMap().getOwnedFrontSuperRegions(state);
		String mName = state.getMyPlayerName();
		String eName = state.getOpponentPlayerName();

		ArrayList<PlacementProposal> placementProposals = new ArrayList<PlacementProposal>();
		for (SuperRegion s : vulnerable) {
			double weight = calculateWeight(s);
			Region mostVulnerableRegion = null;

			HashMap<Region, Integer> ownForces = new HashMap<Region, Integer>();

			for (Region r : s.getFronts(eName)) {
				ownForces.put(r, r.getArmies());
			}
			// keep on assigning dudes until we have at least extraArmiesDefence
			// more in every region
			// and add them in the most vulnerable region

			int minDiff = Integer.MAX_VALUE;
			for (Region r : s.getFronts(eName)) {
				int currentDiff = ownForces.get(r) - r.getHighestThreateningForce();
				if (currentDiff < minDiff) {
					minDiff = currentDiff;
					mostVulnerableRegion = r;
				}

			}
			while (minDiff < Values.extraArmiesDefence) {
				minDiff = Integer.MAX_VALUE;
				for (Region r : s.getFronts(eName)) {
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
		double worth = Values.staticSuperRegionDefence + Values.rewardDefenseImportanceMultiplier * s.getArmiesReward();
		return worth;
	}

	private double calculateCost(SuperRegion s) {
		double cost = (s.getFronts(BotState.getMyOpponentName()).size() * Values.multipleFrontPenalty)
				+ (s.getTotalThreateningForce(BotState.getMyOpponentName()) * Values.costMultiplierDefendingAgainstEnemy);
		return cost;
	}

	private ArrayList<PlacementProposal> organizePocketDefence(BotState state) {
		ArrayList<Region> pockets = state.getFullMap().getPockets(state);
		ArrayList<PlacementProposal> pocketPlacementProposals = new ArrayList<PlacementProposal>();
		ArrayList<Region> needHelpRegions = state.getFullMap().getOwnedFrontRegions(state);

		for (Region r : pockets) {
			int highestEnemy = r.getHighestThreateningForce();

			if (r.getArmies() < highestEnemy) {
				int difference = highestEnemy - r.getArmies();
				int weight = Values.staticPocketDefence;
				pocketPlacementProposals.add(new PlacementProposal(weight, r, new Plan(r, r.getSuperRegion()), difference + 1, "DefensiveCommander"));
			}

		}
		return pocketPlacementProposals;
	}

	@Override
	public ArrayList<ActionProposal> getActionProposals(BotState state) {
		ArrayList<ActionProposal> proposals = new ArrayList<ActionProposal>();
		ArrayList<Region> fronts = state.getFullMap().getOwnedFrontRegions(state);
		HashMap<Region, Integer> needDefence = new HashMap<Region, Integer>();
		ArrayList<Region> needHelpRegions = (ArrayList<Region>) fronts.clone();

		final String eName = state.getOpponentPlayerName();
		final String mName = state.getMyPlayerName();
		for (Region r : fronts) {
			// for all the interesting regions, calculate if they defence
			needDefence.put(r, Values.calculateRequiredForcesDefend(r));

		}
		System.err.println("There are " + fronts.size() + " fronts");

		ArrayList<Region> available = state.getFullMap().getOwnedRegions(state.getMyPlayerName());

		Pathfinder pathfinder = new Pathfinder(state.getFullMap(), new PathfinderWeighter() {
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
			if (needDefence.get(r) != null) {
				int disposed = Math.min(needDefence.get(r), r.getArmies() - 1);
				proposals.add(new ActionProposal(calculateWeight(r.getSuperRegion()), r, r, disposed, new Plan(r, r.getSuperRegion()), "DefensiveCommander"));
				needDefence.put(r, needDefence.get(r) - disposed);
			}

			// else {
			// ArrayList<Path> paths = pathfinder.getPathToRegionsFromRegion(r,
			// needHelpRegions, mName);
			// for (Path path : paths) {
			// int totalRequired = needDefence.get(path.getTarget());
			// if (totalRequired < 1) {
			// continue;
			// }
			// double currentCost = path.getDistance() +
			// calculateCost(path.getTarget().getSuperRegion());
			// double currentWorth = calculateWorth(r.getSuperRegion());
			// double currentWeight = currentWorth / currentCost;
			//
			// for (int i = 1; i < path.getPath().size(); i++) {
			// totalRequired +=
			// Values.calculateRequiredForcesAttackTotalVictory(path.getPath().get(i));
			// }
			// int disposed = Math.min(totalRequired, r.getArmies() - 1);
			//
			// proposals.add(new ActionProposal(currentWeight, r,
			// path.getPath().get(1), disposed, new Plan(path.getTarget(),
			// path.getTarget()
			// .getSuperRegion()), "DefensiveCommander"));
			//
			// }
			// }

		}

		return proposals;
	}

}
