package commanders;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
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
import concepts.Plan;

public class GriefCommander implements TemplateCommander {

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

	@Override
	public ArrayList<ActionProposal> getActionProposals(Map map, Set<Integer> available, Pathfinder pathfinder) {
		ArrayList<ActionProposal> proposals = new ArrayList<ActionProposal>();

		proposals = new ArrayList<ActionProposal>();
		HashMap<SuperRegion, Double> ranking = calculateWorth(map);


		double currentWeight;
		ArrayList<Path> paths;

		// calculate plans for every sector
		for (Integer r : available) {
			paths = pathfinder.getPathToAllRegionsNotOwnedByPlayerFromRegion(map.getRegion(r), BotState.getMyName());
			for (Path path : paths) {
				if (path.getTarget().getPlayerName().equals(BotState.getMyName())){
					continue;
				}
				double currentPathCost = path.getDistance();
				double currentWorth = ranking.get(path.getTarget().getSuperRegion());
				currentWeight = currentWorth / currentPathCost;
				ArrayList<Region> regionsAttacked = new ArrayList(path.getPath());
				regionsAttacked.remove(0);
				int totalRequired = Values.calculateRequiredForcesForRegions(regionsAttacked);

				proposals.add(new ActionProposal(currentWeight, map.getRegion(r), path.getPath().get(1), totalRequired, new Plan(path.getTarget(),
						path.getTarget().getSuperRegion()), "GriefCommander"));

			}

		}
		return proposals;
	}

}
