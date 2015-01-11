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

import java.util.ArrayList;

import map.Map;
import map.Region;
import map.SuperRegion;

import move.AttackTransferMove;
import move.PlaceArmiesMove;
import move.Move;

public class BotState {

	private String myName = "";
	private String opponentName = "";

	private Map fullMap = new Map(); // This map is known from the start,
											// contains all the regions and how
											// they are connected, doesn't
											// change after initialization

	private ArrayList<Region> pickableStartingRegions; // 2 randomly chosen
														// regions from each
														// superregion are
														// given, which the bot
														// can chose to start
														// with

	private int startingArmies; // number of armies the player can place on map
	private int maxRounds;
	private int roundNumber;

	private long totalTimebank; // total time that can be in the timebank
	private long timePerMove; // the amount of time that is added to the
								// timebank per requested move

	public BotState() {
		roundNumber = 0;
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
		int i, regionId, superRegionId, wastelandId, reward;

		if (mapInput[1].equals("super_regions")) {
			for (i = 2; i < mapInput.length; i++) {
				try {
					superRegionId = Integer.parseInt(mapInput[i]);
					i++;
					reward = Integer.parseInt(mapInput[i]);
					fullMap.add(new SuperRegion(superRegionId, reward));
				} catch (Exception e) {
					System.err.println("Unable to parse SuperRegions");
				}
			}
		} else if (mapInput[1].equals("regions")) {
			for (i = 2; i < mapInput.length; i++) {
				try {
					regionId = Integer.parseInt(mapInput[i]);
					i++;
					superRegionId = Integer.parseInt(mapInput[i]);
					SuperRegion superRegion = fullMap
							.getSuperRegion(superRegionId);
					fullMap.add(new Region(regionId, superRegion));
				} catch (Exception e) {
					System.err.println("Unable to parse Regions "
							+ e.getMessage());
				}
			}
		} else if (mapInput[1].equals("neighbors")) {
			for (i = 2; i < mapInput.length; i++) {
				try {
					Region region = fullMap.getRegion(Integer
							.parseInt(mapInput[i]));
					i++;
					String[] neighborIds = mapInput[i].split(",");
					for (int j = 0; j < neighborIds.length; j++) {
						Region neighbor = fullMap.getRegion(Integer
								.parseInt(neighborIds[j]));
						region.addNeighbor(neighbor);
					}
				} catch (Exception e) {
					System.err.println("Unable to parse Neighbors "
							+ e.getMessage());
				}
			}
		} else if (mapInput[1].equals("wastelands")) {
			for (i = 2; i < mapInput.length; i++) {
				try {
					Region region = fullMap.getRegion(Integer
					.parseInt(mapInput[i]));
					region.setWasteland(true);
				} catch (Exception e) {
					System.err.println("Unable to parse wastelands "
					+ e.getMessage());
				}
			}
		} else if (mapInput[1].equals("opponent_starting_regions")) {
			for (i = 2; i < mapInput.length; i++) {
				try {
					Region region = fullMap.getRegion(Integer
					.parseInt(mapInput[i]));
					region.setPlayerName(opponentName);
				} catch (Exception e) {
					System.err.println("Unable to parse opponent_starting_regions "
					+ e.getMessage());
				}
			}
		} else {
			System.err.println("Did not parse previous setup_map");
		}
	}

	// regions from wich a player is able to pick his preferred starting region
	public void setPickableStartingRegions(String[] mapInput) {
		pickableStartingRegions = new ArrayList<Region>();
		for (int i = 2; i < mapInput.length; i++) {
			int regionId;
			try {
				regionId = Integer.parseInt(mapInput[i]);
				Region pickableRegion = fullMap.getRegion(regionId);
				pickableStartingRegions.add(pickableRegion);
			} catch (Exception e) {
				System.err.println("Unable to parse pickable regions "
						+ e.getMessage());
			}
		}
	}

	// visible regions are given to the bot with player and armies info
	public void updateMap(String[] mapInput) {
		ArrayList<Region> visibleRegions = new ArrayList<Region>();

		for (int i = 1; i < mapInput.length; i++) {
			try {
				Region region = fullMap.getRegion(Integer.parseInt(mapInput[i]));
				String playerName = mapInput[i + 1];
				int armies = Integer.parseInt(mapInput[i + 2]);

				region.setPlayerName(playerName);
				region.setArmies(armies);
				visibleRegions.add(region);
				i += 2;
			} catch (Exception e) {
				System.err.println("Unable to parse Map Update "
						+ e.getMessage());
			}
		}

		for (Region region : fullMap.getRegions().values()) {
			region.setVisible(false);
		}

		for (Region region : visibleRegions) {
			region.setVisible(true);
		}
	}

	public void readOpponentMoves(String[] moveInput) {
		// Does nothing for now, current is state is given from update_map
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

	public int getRoundNumber() {
		return roundNumber;
	}

	public Map getFullMap() {
		return fullMap;
	}

	public ArrayList<Region> getPickableStartingRegions() {
		return pickableStartingRegions;
	}

}
