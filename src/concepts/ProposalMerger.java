package concepts;

import java.util.ArrayList;

public class ProposalMerger {

	public static ArrayList<TemplateProposal> merge (ArrayList<TemplateProposal> list1, ArrayList<TemplateProposal> list2){
		ArrayList<TemplateProposal> mergedProposals = new ArrayList<TemplateProposal>();
		
		mergedProposals.addAll(list1);

		boolean attached = false;
		

		for (TemplateProposal p1 : list1){
			for (TemplateProposal p2 : list2){
				attached = true;
				int addedWeight = 0;
				if (p1.getPlan().equals(p2.getPlan())){
					// if they have the same superregion in sights
					// add 1 / 2 of the others weight to them
					addedWeight += p2.getWeight()/2;
					if (mergedProposals.contains(p2)){
						int p2Weight = 
						mergedProposals.remove(p2);
					}
					p2.setWeight(p1.getWeight() / 2 + p2.getWeight());
					mergedProposals.add(p2);
					attached = true;
				}	
			}
			
			
			if (attached = false){
				// proposal had nothing in common with another proposal
				mergedProposals.add(p1);
			}
			
		}
		
		for (TemplateProposal p1 : list1){
			if (!mergedProposals.contains(p1)){
				
			}
		}
		
		return list2;
		
	}
}
