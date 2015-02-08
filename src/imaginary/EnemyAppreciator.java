package imaginary;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import bot.BotState;
import map.Map;
import map.Region;
import map.SuperRegion;
import move.AttackTransferMove;
import move.PlaceArmiesMove;

public class EnemyAppreciator {
	private Map speculativeMap;
	private HashMap<Region, Integer> latestInformationRecievedTurn;
	private HashMap<Integer, ArrayList<PlaceArmiesMove>> enemyPlacements;
	private HashMap<Integer, ArrayList<AttackTransferMove>> enemyMoves;

	public EnemyAppreciator(Map map) {
		this.speculativeMap = map;
		latestInformationRecievedTurn = new HashMap<Region, Integer>();
		enemyPlacements = new HashMap<Integer, ArrayList<PlaceArmiesMove>>();
		enemyMoves = new HashMap<Integer, ArrayList<AttackTransferMove>>();
	}

	public void setMap(Map map) {
		this.speculativeMap = map;
		for (Region r : map.getRegionList()) {
			latestInformationRecievedTurn.put(r, BotState.getRoundNumber());
		}

	}

	public void updateMap(Map map) {
		this.speculativeMap = map;
		// for (Region r : map.getRegionList()) {
		// Region region = this.speculativeMap.getRegion(r.getId());
		// if (r.getVisible()) {
		// region.setPlayerName(r.getPlayerName());
		// region.setArmies(r.getArmies());
		// region.setVisible(true);
		// latestInformationRecievedTurn.put(r, BotState.getRoundNumber());
		// } else {
		// region.setVisible(false);
		// }
		// }

	}

	public ArrayList<Region> getDefiniteEnemyPositions() {
		return speculativeMap.getEnemyRegions();
	}

	public void readOpponentMoves(String[] moveInput) {
		ArrayList<PlaceArmiesMove> newPlacements = new ArrayList<PlaceArmiesMove>();
		ArrayList<AttackTransferMove> newMoves = new ArrayList<AttackTransferMove>();

		for (int i = 1; i < moveInput.length;) {
			if (moveInput[i + 1].equals("place_armies")) {
				Region region = speculativeMap.getRegion(Integer.parseInt(moveInput[i + 2]));
				int armies = Integer.parseInt(moveInput[i + 3]);
				newPlacements.add(new PlaceArmiesMove(BotState.getMyOpponentName(), region, armies));
				i += 4;
			} else if (moveInput[i + 1].equals("attack/transfer")) {
				Region from = speculativeMap.getRegion(Integer.parseInt(moveInput[i + 2]));
				Region to = speculativeMap.getRegion(Integer.parseInt(moveInput[i + 3]));
				int armies = Integer.parseInt(moveInput[i + 4]);
				newMoves.add(new AttackTransferMove(BotState.getMyOpponentName(), from, to, armies));
				i += 5;

			} else {
				System.out.println("FATAL ERROR WE'RE ALL GONNA DIE");
			}
		}

		enemyPlacements.put(BotState.getRoundNumber(), newPlacements);
		enemyMoves.put(BotState.getRoundNumber(), newMoves);

	}

	public Map getSpeculativePlacementMap() {
		return speculativeMap;

	}

	public void speculate() {
		int enemyPlacedArmies = estimatePlacedArmies();
		ArrayList<Region> directlyThreatening = speculativeMap.getAllRegionsThreateningOwnedSuperRegions();
		ArrayList<Region> otherwiseThreatening = speculativeMap.getAllRegionsThreateningOwnedRegions();

		if (directlyThreatening.size() > 0) {
			int armiesPerRegion = enemyPlacedArmies / directlyThreatening.size();
			for (Region r : directlyThreatening) {
				r.setArmies(r.getArmies() + armiesPerRegion);
				System.err.println("Appreciated number of armies on " + r.getId() + " to " + r.getArmies());
			}

		}

		else if (otherwiseThreatening.size() > 0) {
			int armiesPerRegion = enemyPlacedArmies / otherwiseThreatening.size();
			for (Region r : otherwiseThreatening) {
				r.setArmies(r.getArmies() + armiesPerRegion);
				System.err.println("Appreciated number of armies on " + r.getId() + " to " + r.getArmies());
			}
		} else {
			int armiesPerRegion = enemyPlacedArmies / speculativeMap.getEnemyRegions().size();
			for (Region r : speculativeMap.getEnemyRegions()) {
				r.setArmies(r.getArmies() + armiesPerRegion);
				System.err.println("Appreciated number of armies on " + r.getId() + " to " + r.getArmies());
			}
		}
	}

	private int estimatePlacedArmies() {
		int totalArmies = 5;
		for (SuperRegion s : speculativeMap.getSuspectedOwnedSuperRegions(BotState.getMyOpponentName())) {
			totalArmies += s.getArmiesReward();

		}
		// TODO Auto-generated method stub
		return totalArmies;
	}

}
