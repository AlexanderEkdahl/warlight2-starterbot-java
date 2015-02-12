package commanders;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Set;

import map.Map;
import map.Pathfinder;
import map.PathfinderWeighter;
import map.Region;
import map.SuperRegion;
import map.Pathfinder.Path;
import bot.BotState;
import bot.Values;
import concepts.ActionProposal;
import concepts.PlacementProposal;
import concepts.Plan;

public class GriefCommander extends TemplateCommander {

	private HashMap<SuperRegion, Double> calculateWorth(Map map) {
		HashMap<SuperRegion, Double> worths = new HashMap<SuperRegion, Double>();

		for (SuperRegion s : map.getSuperRegions()) {
			if (s.getSuspectedOwnedSuperRegion(BotState.getMyOpponentName())) {
				double reward = s.getArmiesReward();
				worths.put(s, reward * Values.valueDenialMultiplier);
			} else {
				worths.put(s, -1d);
			}

		}
		return worths;
	}

	@Override
	public ArrayList<ActionProposal> getActionProposals(Map map) {
		ArrayList<ActionProposal> proposals = new ArrayList<ActionProposal>();

		proposals = new ArrayList<ActionProposal>();
		HashMap<SuperRegion, Double> ranking = calculateWorth(map);

		ArrayList<Region> available = map.getOwnedRegions(BotState.getMyName());
		Pathfinder pathfinder = new Pathfinder(map, new PathfinderWeighter() {
			public double weight(Region nodeA, Region nodeB) {
				return Values.calculateRegionWeighedCost(nodeB);

			}
		});

		double currentWeight;
		ArrayList<Path> paths;

		// calculate plans for every sector
		for (Region r : available) {
			paths = pathfinder.getPathToAllRegionsNotOwnedByPlayerFromRegion(r, BotState.getMyName());
			for (Path path : paths) {
				SuperRegion targetSuperRegion = path.getTarget().getSuperRegion();
				double currentPathCost = path.getDistance();
				double currentWorth = ranking.get(path.getTarget().getSuperRegion());
				currentWeight = currentWorth / currentPathCost;
				int totalRequired = 0;
				for (int i = 1; i < path.getPath().size(); i++) {
					totalRequired += Values.calculateRequiredForcesAttackTotalVictory(path.getPath().get(i));
				}

				proposals.add(new ActionProposal(currentWeight, r, path.getPath().get(1), totalRequired, new Plan(path.getTarget(), targetSuperRegion),
						"GriefCommander"));

			}

		}
		return proposals;
	}

}
