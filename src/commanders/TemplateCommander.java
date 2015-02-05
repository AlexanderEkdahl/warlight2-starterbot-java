package commanders;

import java.util.ArrayList;

import map.Map;
import concepts.ActionProposal;
import concepts.PlacementProposal;
import bot.BotState;

public abstract class TemplateCommander {
	protected int selfImportance;

	public TemplateCommander() {
		selfImportance = 0;
	}

	public abstract ArrayList<PlacementProposal> getPlacementProposals(Map map);

	public abstract ArrayList<ActionProposal> getActionProposals(Map map);

}
