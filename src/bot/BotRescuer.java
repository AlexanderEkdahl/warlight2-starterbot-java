package bot;

import java.lang.Exception;
import map.Region;
import map.SuperRegion;
import java.util.ArrayList;
import move.AttackTransferMove;
import move.PlaceArmiesMove;

public class BotRescuer extends BotMain {
  public BotRescuer() {
    super();
  }

  public Region getStartingRegion(BotState state, Long timeOut) {
    try {
      return super.getStartingRegion(state, timeOut);
    } catch (Exception e) {
      e.printStackTrace();
    }

    return new Region(1, new SuperRegion(1, 1));
  }

  // right now it just takes the highest priority tasks and executes them
  public ArrayList<PlaceArmiesMove> getPlaceArmiesMoves(BotState state,
  Long timeOut) {
    try {
      return super.getPlaceArmiesMoves(state, timeOut);
    } catch (Exception e) {
      e.printStackTrace();
    }

    return new ArrayList<PlaceArmiesMove>();
  }

  public ArrayList<AttackTransferMove> getAttackTransferMoves(BotState state,
  Long timeOut) {
    try {
      return super.getAttackTransferMoves(state, timeOut);
    } catch (Exception e) {
      e.printStackTrace();
    }

    return new ArrayList<AttackTransferMove>();
  }
}
