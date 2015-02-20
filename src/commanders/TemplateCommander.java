package commanders;

import java.util.ArrayList;
import java.util.Set;

import map.Map;
import map.Region;
import concepts.ActionProposal;

public interface TemplateCommander {
	
	public ArrayList<ActionProposal> getActionProposals(Map map, Set<Integer> available);

}
