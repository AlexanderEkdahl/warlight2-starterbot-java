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
		for (Region r : map.getRegionList()) {
			latestInformationRecievedTurn.put(r, BotState.getRoundNumber());
		}
		speculate();

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

	public Map getSpeculativeMap() {
		return speculativeMap;

	}

	private void speculate() {
		int enemyPlacedArmies = estimatePlacedArmies();
		ArrayList<Region> vulnerable = speculativeMap.getAllEnemyVulnerableRegions();
		ArrayList<Region> annoying = speculativeMap.getallAnnoyingRegions();
		ArrayList<Region> directlyThreatening = speculativeMap.getAllRegionsThreateningOwnedSuperRegions();

		if (directlyThreatening.size() > 0) {
			int armiesPerRegion = enemyPlacedArmies / directlyThreatening.size();
			for (Region r : directlyThreatening) {
				r.setArmies(r.getArmies() + armiesPerRegion);
				System.err.println("Appreciated number of armies on " + r.getId() + " to " + r.getArmies());
			}
		} else if (annoying.size() > 0) {
			int armiesPerRegion = enemyPlacedArmies / annoying.size();
			for (Region r : annoying) {
				r.setArmies(r.getArmies() + armiesPerRegion);
				System.err.println("Appreciated number of armies on " + r.getId() + " to " + r.getArmies());
			}
		} else {
			if (speculativeMap.getEnemyRegions().size() == 0) {
				return;
			}
			else{
				speculativeMap.getEnemyRegions().get(0).setArmies(speculativeMap.getEnemyRegions().get(0).getArmies() + enemyPlacedArmies);
			}
		}
	}

	private int estimatePlacedArmies() {
		int totalArmies = 5;
		for (SuperRegion s : speculativeMap.getSuspectedOwnedSuperRegions(BotState.getMyOpponentName())) {
			totalArmies += s.getArmiesReward();

		}
		return totalArmies;
	}

}
