package commanders;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;

import concepts.ActionProposal;
import concepts.PlacementProposal;
import concepts.Plan;
import bot.BotState;
import bot.Values;
import map.*;
import map.Pathfinder.Path;

public class OffensiveCommander extends TemplateCommander {


	public static Region determineStartPosition(ArrayList<Region> possiblePicks, Map map) {

		Map modifiedMap = map.duplicate();

		Region maxRegion = null;
		double maxWeight = Double.MIN_VALUE;
		ArrayList<Region> unOwned = map.getUnOwnedRegions();

		for (Region r : possiblePicks) {
			String beforeStatus = modifiedMap.getRegion(r.getId()).getPlayerName();
			modifiedMap.getRegion(r.getId()).setPlayerName(BotState.getMyName());
			double weight = calculateStartRegionWorth(r, map);
			if (weight > maxWeight) {
				maxWeight = weight;
				maxRegion = r;
			}
			modifiedMap.getRegion(r.getId()).setPlayerName(beforeStatus);
		}

		return maxRegion;

	}

	private static double calculateStartRegionWorth(Region region, Map map) {
		Pathfinder pathfinder = new Pathfinder(map, new PathfinderWeighter() {
			public double weight(Region nodeA, Region nodeB) {
				return Values.calculateRegionWeighedCost(nodeB);

			}
		});

		double maxWeight = Double.MIN_VALUE;
		HashMap<SuperRegion, Double> worths = calculateWorth(map);
		ArrayList<Path> paths = pathfinder.getPathToRegionsFromRegion(region, map.getUnOwnedRegions());

		for (Path path : paths) {
			double weight = calculatePathWeight(path, worths, map);
			if (weight > maxWeight) {
				maxWeight = weight;
			}
		}

		return maxWeight;

	}

	

	private static double calculatePathWeight(Path path, HashMap<SuperRegion, Double> worths, Map map) {
		double worth = worths.get(path.getTarget().getSuperRegion());
		double cost = path.getDistance() - Values.calculateRegionWeighedCost(path.getTarget())
				+ Values.calculateSuperRegionWeighedCost(path.getTarget().getSuperRegion(), map);
		double weight = worth / cost;

		return weight;
	}

	private static HashMap<SuperRegion, Double> calculateWorth(Map map) {
		HashMap<SuperRegion, Double> worth = new HashMap<SuperRegion, Double>();
		ArrayList<SuperRegion> possibleTargets = map.getSuperRegions();

		for (SuperRegion s : possibleTargets) {
			if (s.ownedByPlayer(BotState.getMyName())) {
				worth.put(s, -1d);
			} else {
				worth.put(s, Values.calculateSuperRegionWorth(s));
			}
		}
		return worth;
	}

	@Override
	public ArrayList<ActionProposal> getActionProposals(Map map) {
		ArrayList<ActionProposal> proposals = new ArrayList<ActionProposal>();
		HashMap<SuperRegion, Double> ranking = calculateWorth(map);

		ArrayList<Region> available = map.getOwnedRegions(BotState.getMyName());

		Pathfinder pathfinder = new Pathfinder(map, new PathfinderWeighter() {
			public double weight(Region nodeA, Region nodeB) {

				return Values.calculateRegionWeighedCost(nodeB);

			}
		});
		ArrayList<Path> paths;

		// calculate plans for every sector

		for (Region r : available) {
			paths = pathfinder.getPathToAllRegionsNotOwnedByPlayerFromRegion(r, BotState.getMyName());
			for (Path path : paths) {
				double weight = calculatePathWeight(path, ranking, map);
				int totalRequired = 0;
				for (int i = 1; i < path.getPath().size(); i++) {
					totalRequired += Values.calculateRequiredForcesAttackTotalVictory(path.getPath().get(i));
				}
				proposals.add(new ActionProposal(weight, r, path.getPath().get(1), totalRequired, new Plan(path.getTarget(), path.getTarget().getSuperRegion()),
						"OffensiveCommander"));

			}

		}

		return proposals;
	}
}
