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

import imaginary.EnemyAppreciator;

import java.util.ArrayList;
import java.util.HashSet;

import map.Map;
import map.Region;
import map.SuperRegion;
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

	public BotState() {
		roundNumber = 0;
		map.initAppreciator();
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
			// Do nothing
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
	}

	// visible regions are given to the bot with player and armies info
	public void updateMap(String[] mapInput) {
		map.updateMap(mapInput);
		

	}

	public void readOpponentMoves(String[] moveInput) {
		map.readOpponentMoves(moveInput);

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

}
