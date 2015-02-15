package commanders;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;

import map.Map;
import map.Region;
import concepts.ActionProposal;
import concepts.PlacementProposal;
import bot.BotState;

public abstract class TemplateCommander {
	protected int selfImportance;

	public TemplateCommander() {
		selfImportance = 0;
	}
	public abstract ArrayList<ActionProposal> getActionProposals(Map map, Set<Region> available);

}
