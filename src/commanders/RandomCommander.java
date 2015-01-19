package commanders;

import java.util.Random;
import java.util.ArrayList;

import bot.*;
import concepts.*;
import map.*;

public class RandomCommander extends TemplateCommander {
  @Override
  public ArrayList<PlacementProposal> getPlacementProposals(BotState state) {
    ArrayList<PlacementProposal> proposals = new ArrayList<PlacementProposal>();
    ArrayList<Region> regions = state.getFullMap().getOwnedRegions(state.getMyPlayerName());
    Random random = new Random();

    for (int i = 0; i < state.getStartingArmies(); i++) {
      Region region = regions.get(random.nextInt(regions.size()));
      PlacementProposal proposal = new PlacementProposal(Integer.MIN_VALUE, region, new Plan(region, region.getSuperRegion()), 1, "RandomCommander");
      proposals.add(proposal);
    }

    return proposals;
  }

  @Override
  public ArrayList<ActionProposal> getActionProposals(BotState state) {
    return new ArrayList<ActionProposal>();
  }
}
