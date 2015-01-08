package commanders;

import java.util.ArrayList;

import concepts.ActionProposal;
import concepts.PlacementProposal;
import bot.BotState;

public abstract class TemplateCommander {
	protected int selfImportance;

	public TemplateCommander() {
		selfImportance = 0;
	}

	public abstract ArrayList<PlacementProposal> getPlacementProposals(
			BotState state);

	public abstract ArrayList<ActionProposal> getActionProposals(BotState state);

}
