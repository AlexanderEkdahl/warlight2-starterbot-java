package commanders;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;

import map.Map;
import map.Pathfinder;
import map.Region;
import map.SuperRegion;
import map.Pathfinder.Path;
import bot.BotState;
import bot.Values;
import concepts.ActionProposal;
import concepts.Plan;

public class GriefCommander {

	private HashMap<SuperRegion, Double> calculateWorth(Map map) {
		HashMap<SuperRegion, Double> worths = new HashMap<SuperRegion, Double>();

		for (SuperRegion s : map.getSuperRegions()) {
			if (s.getSuspectedOwnedSuperRegion()) {
				double reward = s.getArmiesReward();
				worths.put(s, reward * Values.valueDenialMultiplier);
			} else {
				worths.put(s, 0d);
			}
		}
		return worths;
	}

	public ArrayList<ActionProposal> getActionProposals(Map map, Set<Integer> interesting, Pathfinder pathfinder, HashMap<Integer, Integer> availableForces) {
		ArrayList<ActionProposal> proposals = new ArrayList<ActionProposal>();
		proposals = new ArrayList<ActionProposal>();
		HashMap<SuperRegion, Double> ranking = calculateWorth(map);

		double currentWeight;
		ArrayList<Path> paths;

		// calculate plans for every sector
		for (Integer r : interesting) {
			paths = pathfinder.getPathToAllRegionsNotOwnedByPlayerFromRegion(map.getRegion(r), BotState.getMyName());
			for (Path path : paths) {
				ArrayList<Region> regionsAttacked = new ArrayList<Region>(path.getPath());
				regionsAttacked.remove(0);
				int totalRequired = Values.calculateRequiredForcesForRegions(regionsAttacked);
				double currentPathCost = Values.calculatePathCost(regionsAttacked, totalRequired, availableForces.get(r));
				double currentWorth = ranking.get(path.getTarget().getSuperRegion());
				currentWeight = currentWorth / currentPathCost;
				proposals.add(new ActionProposal(currentWeight, map.getRegion(r), path.getPath().get(1), totalRequired, new Plan(path.getTarget(),
						path.getTarget().getSuperRegion()), "GriefCommander"));

			}

		}
		return proposals;
	}

}
