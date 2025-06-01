package edu.sdccd.cisc191.game;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;

/*
 * Shipyard class for managing spaceship constructions and upgrades
 * Uses I/O streams for saving.loading shipyard state
 * Uses ExecutorService for concurrent ship building
 */

/*
 * Features added:
 * Ship Construction: Player can build ships asynchronously (multithreading)
 * Ship Upgrades: Upgrades ships, increasing their stats
 * Save and Load: Fleet persists between game sessions using file I/O (OOS)
 * Concurrency Handling: Uses ExecutorService for shipbuilding
 * Interactive Testing: main method allows quick testing of shipyard features
 */

public class Shipyard {
    private final Map<String, GalacticShip> availableShips;
    private final List<GalacticShip> playerFleet;
    private final ExecutorService shipBuilderPool;
    private final String saveFile = "resources/ships.json"; // Stores the list of ships

    // Constructs a Shipyard with predefined ship options
    public Shipyard() {
        this.availableShips = new HashMap<>();
        this.playerFleet = new ArrayList<>();
        this.shipBuilderPool = Executors.newFixedThreadPool(2); // Allows 2 ships to be built at a time

        initializeShipyard();
        try {
            loadShipyardState();
        } catch (Exception e) {
            System.err.println("Error loading shipyard state at startup: " + e.getMessage());
        }
    }

    // Initializes default ship types available in the shipyard
    private void initializeShipyard() {
        availableShips.put("Fighter", new GalacticShip("Fighter", 100, 20));
        availableShips.put("Cruiser", new GalacticShip("Cruiser", 200, 40));
        availableShips.put("Battleship", new GalacticShip("Battleship", 300, 60));
    }


    // Displays available ships and their stats
    public void displayAvailableShips() {
        System.out.println("Available Ships:");
        for (String key : availableShips.keySet()) {
            GalacticShip ship = availableShips.get(key);
            System.out.println("- " + ship.getName() + " | Health: " + ship.getHealth() + " | Attack: " + ship.getAttackPower());
        }
    }

    /* Asynchronously builds a new spaceship for the player
     * @param shipType The type of ship to construct
     */
    public void buildShip(String shipType) {
        if (!availableShips.containsKey(shipType)) {
            System.out.println("Invalid ship type.");
            return;
        }

        System.out.println("Building " + shipType + "...");
        shipBuilderPool.submit(() -> {
            try {
                Thread.sleep(2000); // Simulate shipbuilding time
                GalacticShip template = availableShips.get(shipType);
                if (template == null) {
                    System.err.println("Template not found for ship type: " + shipType);
                    return;
                }
                GalacticShip newShip = new GalacticShip(shipType, availableShips.get(shipType).getHealth(), availableShips.get(shipType).getAttackPower());
                synchronized (playerFleet) {
                    playerFleet.add(newShip);
                }
                System.out.println(shipType + " construction complete! Added to fleet.");
                saveShipyardState(); // Save updated fleet after ship is built
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.err.println("Ship building interrupted for " + shipType);
            } catch (Exception e) {
                System.err.println("Unexpected error during ship build: " + e.getMessage());
            }
        });
    }

    /*
     * Upgrades a player's ship by increasing its health and attack power
     * @param shipName The name of the ship to upgrade
     */

    public void upgradeShip(String shipName) {
        synchronized (playerFleet) {
            for (GalacticShip ship : playerFleet) {
                if (ship.getName().equalsIgnoreCase(shipName)) {
                    ship.takeDamage(-50); // Increases health by 50
                    System.out.println(shipName + " upgraded! New Health: " + ship.getHealth());
                    try {
                        saveShipyardState(); // Save fleet upgrades
                    } catch (Exception e) {
                        System.err.println("Error saving state after upgrade: " + e.getMessage());
                    }
                    return;
                }
            }
        }
        System.out.println("Ship not found in your fleet.");
    }

    // Displays the player's current fleet
    public void displayPlayerFleet() {
        System.out.println("\nYour Fleet: ");
        synchronized (playerFleet) {
            if (playerFleet.isEmpty()) {
                System.out.println("No ships in fleet.");
            } else {
                for (GalacticShip ship : playerFleet) {
                    System.out.println("- " + ship.getName() + " | Health: " + ship.getHealth() + " | Attack: " + ship.getAttackPower());
                }
            }
        }
    }

    /*
     * Retrieves the player's fleet
     * @return List of GalacticShips in the player's fleet
     */
    public List<GalacticShip> getPlayerFleet() {
        return playerFleet;
    }

    // Saves the player's fleet to a file using ObjectOutputStream
    private void saveShipyardState() {
        File file = new File(saveFile);
        File parentDir = file.getParentFile();
        if (parentDir != null && !parentDir.exists()) {
            parentDir.mkdirs();
        }
        try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(file))) {
            out.writeObject(playerFleet);
            System.out.println("Shipyard state saved.");
        } catch (IOException e) {
            System.err.println("Error saving shipyard state: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("Unexpected error saving shipyard state: " + e.getMessage());
        }
    }

    // Load the player's fleet from a file using ObjectInputStream
    private void loadShipyardState() {
        File file = new File(saveFile);
        if (!file.exists()) {
            System.out.println("No shipyard save file found. Starting fresh.");
            return;
        }
        try (ObjectInputStream in = new ObjectInputStream(new FileInputStream(saveFile))) {
            Object obj = in.readObject();
            if (obj instanceof List) {
                List<?> loadedFleet = (List<?>) obj;
                synchronized (playerFleet) {
                    playerFleet.clear();
                    for (Object shipObj : loadedFleet) {
                        if (shipObj instanceof GalacticShip) {
                            playerFleet.add((GalacticShip) shipObj);
                        }
                    }
                }
                System.out.println("Shipyard state loaded.");
            } else {
                System.err.println("Shipyard save file format invalid.");
            }
        } catch (IOException e) {
            System.err.println("Error loading shipyard state: " + e.getMessage());
        } catch (ClassNotFoundException e) {
            System.err.println("Ship class definition not found while loading: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("Unexpected error loading shipyard state: " + e.getMessage());
        }
    }

    /**
     * Shuts down the ExecutorService for clean-up.
     * Waits for all queued shipbuilding tasks to finish.
     */
    public void shutdown() {
        shipBuilderPool.shutdown();
        try {
            if (!shipBuilderPool.awaitTermination(3, TimeUnit.SECONDS)) {
                shipBuilderPool.shutdownNow();
                System.out.println("Forced shutdown of shipBuilderPool.");
            }
        } catch (InterruptedException e) {
            shipBuilderPool.shutdownNow();
            Thread.currentThread().interrupt();
            System.err.println("Interrupted during shipBuilderPool shutdown.");
        }
    }

    // Test the Shipyard functionality
    /**
     * Standalone test method to demonstrate Shipyard functionality.
     * Builds, upgrades, and displays ships.
     * @param args unused
     */
    public static void main(String[] args) {
        Shipyard shipyard = new Shipyard();

        shipyard.displayAvailableShips();
        shipyard.buildShip("Fighter");
        shipyard.buildShip("Cruiser");

        try {
            Thread.sleep(5000); // Wait for ships to be built
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("Test interrupted while waiting for ships to build.");
        }

        shipyard.displayPlayerFleet();
        shipyard.upgradeShip("Fighter");
        shipyard.displayPlayerFleet();

        shipyard.shutdown();
    }

}
