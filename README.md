#TODO

- Ant script to upload output.tar.gz(possible without a key?)

- fuck bitches get money


## Strategy
 - Make the bot defend owned SuperRegions if neccesary, possibly important Regions as well, Definitely done by DefensiveCommander.
 - Make the bot defend and value bridgeheads (solitary regions) , probably best done by DefensiveCommander
 - Make the bot better at using the appropriate forces when performing an action, not moving them across the map.
 - Fix the placement bug.
 - Possibly investigate initial Regoin picking.

# Usage

Run simple integration test from the commandline

    ant clean jar && java -jar bin/Warlight.jar < fixtures/simple

Test Pathfinder

    ant jar -Dmain-class=map.Pathfinder2 && java -jar bin/Warlight.jar

# Links

- http://gamedev.stackexchange.com/questions/21519/complex-game-ai-for-turn-based-strategy-games

``` java
Pathfinder2 pathfinder2 = new Pathfinder2(m);

Path path = pathfinder2.getShortestPathToSuperRegionFromRegionOwnedByPlayer(superRegion, "player1");

System.err.println("Total distance: " + path.getDistance());
System.err.println("Origin region: " + path.getOrigin());

for (Region region : path.getPath()) {
  System.out.print(region);
}
```
