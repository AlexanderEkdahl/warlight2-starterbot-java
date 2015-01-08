package concepts;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;

import map.SuperRegion;



///// 200 % undone

public class ProposalMerger {

	public static ArrayList<TemplateProposal> merge (ArrayList<TemplateProposal> list1, ArrayList<TemplateProposal> list2){
		ArrayList<TemplateProposal> mergedProposals = new ArrayList<TemplateProposal>();
		
		Collections.sort(list1);
		Collections.sort(list2);
		// this is just one way of merging, perhaps you will have a better idea SANDOR
		// right now it's just merging the plans, to see whether both are interested in a region
		
		HashSet<Plan> plans = new HashSet<Plan>();
		ArrayList<SuperRegion> planWorth = new ArrayList<SuperRegion>();
		
		for (TemplateProposal t : list1){
			if (!planWorth.contains(t.getPlan().getSr())){
				planWorth.add(t.getPlan().getSr());
			}
			
		}
		
		
		
			
		
		
		return list2;
		
	}
}
