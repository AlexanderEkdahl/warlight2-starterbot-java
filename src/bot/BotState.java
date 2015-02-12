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
import imaginary.IncomeAppreciator;

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
	private ArrayList<ArrayList<Move>> opponentMoves;
	private IncomeAppreciator incomeAppreciator;

	public BotState() {
		roundNumber = 0;
		map.initAppreciator();
		opponentMoves = new ArrayList<ArrayList<Move>>();
		incomeAppreciator = new IncomeAppreciator(this);
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
		// if (mapInput[1].equals("super_regions")) {
		// for (int i = 2; i < mapInput.length; i++) {
		// try {
		// int superRegionId = Integer.parseInt(mapInput[i]);
		// i++;
		// int reward = Integer.parseInt(mapInput[i]);
		// map.add(new SuperRegion(superRegionId, reward));
		// } catch (Exception e) {
		// System.err.println("Unable to parse SuperRegions");
		// }
		// }
		// } else if (mapInput[1].equals("regions")) {
		// for (int i = 2; i < mapInput.length; i++) {
		// try {
		// int regionId = Integer.parseInt(mapInput[i]);
		// i++;
		// int superRegionId = Integer.parseInt(mapInput[i]);
		// SuperRegion superRegion = map.getSuperRegion(superRegionId);
		// map.add(new Region(regionId, superRegion));
		// } catch (Exception e) {
		// System.err.println("Unable to parse Regions " + e.getMessage());
		// }
		// }
		// } else if (mapInput[1].equals("neighbors")) {
		// for (int i = 2; i < mapInput.length; i++) {
		// try {
		// Region region = map.getRegion(Integer.parseInt(mapInput[i]));
		// i++;
		// String[] neighborIds = mapInput[i].split(",");
		// for (int j = 0; j < neighborIds.length; j++) {
		// Region neighbor = map.getRegion(Integer.parseInt(neighborIds[j]));
		// region.addNeighbor(neighbor);
		// }
		// } catch (Exception e) {
		// System.err.println("Unable to parse Neighbors " + e.getMessage());
		// }
		// }
		// // map.computeBottlenecks();
		// } else if (mapInput[1].equals("wastelands")) {
		// for (int i = 2; i < mapInput.length; i++) {
		// try {
		// Region region = map.getRegion(Integer.parseInt(mapInput[i]));
		// region.setWasteland(true);
		// } catch (Exception e) {
		// System.err.println("Unable to parse wastelands " + e.getMessage());
		// }
		// }
		// } else if (mapInput[1].equals("opponent_starting_regions")) {
		// for (int i = 2; i < mapInput.length; i++) {
		// try {
		// Region region = map.getRegion(Integer.parseInt(mapInput[i]));
		// region.setPlayerName(opponentName);
		// } catch (Exception e) {
		// System.err.println("Unable to parse opponent_starting_regions " +
		// e.getMessage());
		// }
		// }
		// } else {
		// System.err.println("Did not parse previous setup_map");
		// }

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
		// ArrayList<Region> visibleRegions = new ArrayList<Region>();
		// HashSet<Region> invisibleRegions = new
		// HashSet<Region>(map.getRegions().values());
		//
		// for (int i = 1; i < mapInput.length; i++) {
		// try {
		// Region region = map.getRegion(Integer.parseInt(mapInput[i]));
		// String playerName = mapInput[i + 1];
		// int armies = Integer.parseInt(mapInput[i + 2]);
		//
		// region.setPlayerName(playerName);
		// region.setArmies(armies);
		// visibleRegions.add(region);
		// i += 2;
		// } catch (Exception e) {
		// System.err.println("Unable to parse Map Update " + e.getMessage());
		// }
		// }
		//
		// for (Region region : visibleRegions) {
		// region.setVisible(true);
		// invisibleRegions.remove(region);
		// }
		//
		// for (Region region : invisibleRegions) {
		// region.setVisible(false);
		// if (region.getPlayerName().equals(myName)) {
		// System.err.println("Region: " + region.getId() +
		// " was lost out of sight. It must have been taken by the enemy.");
		// region.setPlayerName(opponentName);
		// }
		// }

	}

	public void readOpponentMoves(String[] moveInput) {
		map.readOpponentMoves(moveInput); // this is bad... map should not contain state
		opponentMoves.add(new ArrayList<Move>());
		for(int i=1; i<moveInput.length; i++)
		{
			try {
				Move move;
				if(moveInput[i+1].equals("place_armies")) {
					Region region = map.getRegion(Integer.parseInt(moveInput[i+2]));
					String playerName = moveInput[i];
					int armies = Integer.parseInt(moveInput[i+3]);
					move = new PlaceArmiesMove(playerName, region, armies);
					i += 3;
				}
				else if(moveInput[i+1].equals("attack/transfer")) {
					Region fromRegion = map.getRegion(Integer.parseInt(moveInput[i+2]));
					Region toRegion = map.getRegion(Integer.parseInt(moveInput[i+3]));

					String playerName = moveInput[i];
					int armies = Integer.parseInt(moveInput[i+4]);
					move = new AttackTransferMove(playerName, fromRegion, toRegion, armies);
					i += 4;
				}
				else { //never happens
					continue;
				}
				opponentMoves.get(roundNumber - 1).add(move);
			}
			catch(Exception e) {
				System.err.println("Unable to parse Opponent moves " + e.getMessage());
			}
		}
		incomeAppreciator.update();
		System.err.println("Enemy income: " + incomeAppreciator.income());
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

	public ArrayList<Move> getOpponentMoves(int round){
		return opponentMoves.get(round - 1);
	}
}
