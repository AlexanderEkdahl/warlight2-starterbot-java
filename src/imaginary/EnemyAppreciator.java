package imaginary;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

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

	public Set<Region> getDefiniteEnemyPositions() {
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

		ArrayList<PlaceArmiesMove> latestPlacements = enemyPlacements.get(BotState.getRoundNumber() - 1);
		ArrayList<PlaceArmiesMove> nextLatestPlacements = enemyPlacements.get(BotState.getRoundNumber() - 2);

		enemyPlacedArmies = placeOnLastPlacements(enemyPlacedArmies, latestPlacements, nextLatestPlacements);
		Set<Region> vulnerable = speculativeMap.getAllEnemyVulnerableRegions();
		Set<Region> annoying = speculativeMap.getallAnnoyingRegions();
		Set<Region> directlyThreatening = speculativeMap.getAllRegionsThreateningOwnedSuperRegions();
		Set<Region> otherwiseThreatening = speculativeMap.getAllRegionsThreateningOwnedRegions();
		Set<Region> allEnemyOwned = speculativeMap.getEnemyRegions();

		Set<Region> tier1 = new HashSet<Region>(directlyThreatening);
		tier1.retainAll(vulnerable);
		Set<Region> tier2 = new HashSet<Region>(directlyThreatening);
		tier2.addAll(vulnerable);
		tier2.addAll(annoying);
		Set<Region> tier4 = new HashSet<Region>(otherwiseThreatening);
		Set<Region> tier5 = new HashSet<Region>(allEnemyOwned);

		if (tier1.size() > 0) {
			System.err.println("EnemyAppreciator placing enemy forces on tier 1 regions, there are " + tier1.size());
			placeAllOn(tier1, enemyPlacedArmies);
			enemyPlacedArmies = 0;
		} else if (tier2.size() > 0) {
			System.err.println("EnemyAppreciator placing enemy forces on tier 2 regions, there are " + tier2.size());
			placeAllOn(tier2, enemyPlacedArmies);
			enemyPlacedArmies = 0;
		}
		if ((tier4.size() > 0 || tier5.size() > 0) && enemyPlacedArmies > 0) {
			System.err.println("EnemyAppreciator placing enemy forces on tier 4 & 5 regions, there are " + (tier4.size() + tier5.size()));
			Random rand = new Random();
			ArrayList<Region> tier4List = new ArrayList<Region>();
			ArrayList<Region> tier5List = new ArrayList<Region>();

			tier4List.addAll(tier4);
			tier5List.addAll(tier5);
			while (enemyPlacedArmies > 0) {
				int selected = rand.nextInt(tier4.size() + tier5.size());
				if (selected < tier4.size()) {
					placeArmies(tier4List.get(selected), 1);
				} else {
					selected -= tier4.size();
					placeArmies(tier5List.get(selected), 1);
				}
				enemyPlacedArmies--;
			}
		}

	}

	private void placeAllOn(Set<Region> regions, int enemyPlacedArmies) {
		int armiesPerRegion = enemyPlacedArmies / regions.size();
		for (Region r : regions) {
			placeArmies(r, armiesPerRegion);
			enemyPlacedArmies -= armiesPerRegion;
		}
		if (enemyPlacedArmies > 0) {
			placeArmies(regions.iterator().next(), enemyPlacedArmies);
		}

	}

	private int placeOnLastPlacements(int enemyPlacedArmies, ArrayList<PlaceArmiesMove> latestPlacements, ArrayList<PlaceArmiesMove> nextLatestPlacements) {
		boolean hasPrinted = false;
		if (latestPlacements != null) {
			for (PlaceArmiesMove p : latestPlacements) {
				Region region = speculativeMap.getRegion(p.getRegion().getId());
				if (region.getPlayerName().equals(BotState.getMyOpponentName()) && region.hasNeighborWithOtherOwner()) {
					if (!hasPrinted) {
						System.err.println("EnemyAppreciator placing enemy forces on regions that have been used by the enemy before");
						hasPrinted = true;
					}
					placeArmies(region, p.getArmies());
					enemyPlacedArmies -= p.getArmies();

				}

			}
		}

		return enemyPlacedArmies;
	}

	public void placeArmies(Region r, int disposed) {
		r.setArmies(r.getArmies() + disposed);
		System.err.println("Appreciated number of armies on region " + r.getId() + " to " + r.getArmies());
	}

	private int estimatePlacedArmies() {
		int totalArmies;
		if (BotState.isUsingIncomeAppreciator()) {
			totalArmies = IncomeAppreciator.getIncome();
			return totalArmies;
		} else {
			totalArmies = 5;
			for (SuperRegion s : speculativeMap.getSuspectedOwnedSuperRegions(BotState.getMyOpponentName())) {
				totalArmies += s.getArmiesReward();
			}
			System.err.println("EnemyAppreciator: Enemy income: " + totalArmies);

		}

		return totalArmies;
	}

}
