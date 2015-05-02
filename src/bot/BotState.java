/**
 * Warlight AI Game Bot
 *
 * Last update: April 02, 2014
 *
 * @author Jim van Eeden
 * @version 1.0
 * @License MIT License (http://opensource.org/Licenses/MIT)
 */

package bot;

import imaginary.IncomeAppreciator;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import map.Map;
import map.Region;
import map.SuperRegion;
import math.Tables;
import move.AttackTransferMove;
import move.PlaceArmiesMove;
import move.Move;

public class BotState {
	private static String myName = "";
	private static String opponentName = "";
	private Map map = new Map();
	private ArrayList<Region> pickableStartingRegions;
	private int startingArmies;
	private int maxRounds;
	private static int roundNumber;
	private long totalTimebank;
	private long timePerMove;
	private ArrayList<ArrayList<Move>> opponentMoves;
	private static boolean isUsingIncomeAppreciator = true;
	private IncomeAppreciator incomeAppreciator;

	private Set<Integer> wasOncePickable = new HashSet<Integer>();

	public BotState() {
		roundNumber = 0;
		map.initAppreciator();
		opponentMoves = new ArrayList<ArrayList<Move>>();
		incomeAppreciator = new IncomeAppreciator(this);
		pickableStartingRegions = new ArrayList<Region>();

	}

	public void updateSettings(String key, String value) {
		if (key.equals("your_bot")) { // bot's own name
			myName = value;
		} else if (key.equals("opponent_bot")) // opponent's name
			opponentName = value;
		else if (key.equals("max_rounds"))
			maxRounds = Integer.parseInt(value);
		else if (key.equals("timebank"))
			totalTimebank = Long.parseLong(value);
		else if (key.equals("time_per_move"))
			timePerMove = Long.parseLong(value);
		else if (key.equals("starting_armies")) {
			startingArmies = Integer.parseInt(value);
			roundNumber++; // next round
		} else if (key.equals("starting_regions")) {
			for (String s : value.split(" ")) {
				int id = Integer.parseInt(s);
				wasOncePickable.add(id);
			}
			Tables.getInstance().introCalculation(map);
			setRegularRegions();
		}
	}

	public void setStartingRegions(String[] value) {
		pickableStartingRegions.addAll(getRegions(value));
	}

	private ArrayList<Region> getRegions(String[] value) {
		ArrayList<Region> regions = new ArrayList<Region>();
		for (int i = 2; i < value.length; i++) {
			try {
				int regionId = Integer.parseInt(value[i]);
				Region region = map.getRegion(regionId);
				regions.add(region);
			} catch (Exception e) {
				System.err.println("Unable to parse pickable regions " + e.getMessage());
			}
		}
		return regions;
	}

	private void setRegularRegions() {
		for (Region r : map.getRegionList()) {
			if (!r.getWasteland()) {
				r.setArmies(2);
			}
		}

	}

	// initial map is given to the bot with all the information except for
	// player and armies info
	public void setupMap(String[] mapInput) {
		map.setupMap(mapInput);

	}

	public void setPickableStartingRegions(String[] mapInput) {
		pickableStartingRegions = new ArrayList<Region>();
		for (int i = 2; i < mapInput.length; i++) {
			try {
				int regionId = Integer.parseInt(mapInput[i]);
				Region pickableRegion = map.getRegion(regionId);
				pickableStartingRegions.add(pickableRegion);
			} catch (Exception e) {
				System.err.println("Unable to parse pickable regions " + e.getMessage());
			}
		}
		for (Integer i : wasOncePickable) {
			if (!pickableStartingRegions.contains(map.getRegion(i)) && !map.getRegion(i).getPlayerName().equals(BotState.getMyName())) {
				map.getRegion(i).setPlayerName(BotState.getMyOpponentName());
				System.err.println("Determined that enemy has picked region " + i);
			}
		}
	}

	// visible regions are given to the bot with player and armies info
	public void updateMap(String[] mapInput) {
		if (map.getRegionList().size() < 70) {
			isUsingIncomeAppreciator = true;
			incomeAppreciator.updateMap();
		} else {
			isUsingIncomeAppreciator = false;
		}

		map.updateMap(mapInput);
		if (isUsingIncomeAppreciator) {
			incomeAppreciator.updateMoves();
			System.err.println("IncomeAppreciator: Enemy income: " + incomeAppreciator.income());
		}
	}

	public void readOpponentMoves(String[] moveInput) {

		map.readOpponentMoves(moveInput); // this is bad... map should not
											// contain state
		opponentMoves.add(new ArrayList<Move>());
		for (int i = 1; i < moveInput.length; i++) {
			try {
				Move move;
				if (moveInput[i + 1].equals("place_armies")) {
					Region region = map.getRegion(Integer.parseInt(moveInput[i + 2]));
					String playerName = moveInput[i];
					int armies = Integer.parseInt(moveInput[i + 3]);
					move = new PlaceArmiesMove(playerName, region, armies);
					i += 3;
				} else if (moveInput[i + 1].equals("attack/transfer")) {
					Region fromRegion = map.getRegion(Integer.parseInt(moveInput[i + 2]));
					Region toRegion = map.getRegion(Integer.parseInt(moveInput[i + 3]));

					String playerName = moveInput[i];
					int armies = Integer.parseInt(moveInput[i + 4]);
					move = new AttackTransferMove(playerName, fromRegion, toRegion, armies);
					i += 4;
				} else { // never happens
					continue;
				}
				opponentMoves.get(roundNumber - 1).add(move);
			} catch (Exception e) {
				System.err.println("Unable to parse Opponent moves " + e.getMessage());
			}
		}
		// if (isUsingIncomeAppreciator){
		// incomeAppreciator.updateMoves();
		// System.err.println("Enemy income: " + incomeAppreciator.income());
		// }

	}

	public String getMyPlayerName() {
		return myName;
	}

	public String getOpponentPlayerName() {
		return opponentName;
	}

	public int getStartingArmies() {
		return startingArmies;
	}

	public static int getRoundNumber() {
		return roundNumber;
	}

	public Map getFullMap() {
		return map;
	}

	public ArrayList<Region> getPickableStartingRegions() {
		return pickableStartingRegions;
	}

	public static String getMyName() {
		return myName;
	}

	public static String getMyOpponentName() {
		return opponentName;
	}

	public ArrayList<Move> getOpponentMoves(int round) {
		if (round < 2){
			return new ArrayList<Move>();
		}
		return opponentMoves.get(round - 2);
	}

	public static boolean isUsingIncomeAppreciator() {
		// TODO Auto-generated method stub
		return isUsingIncomeAppreciator;
	}
}
