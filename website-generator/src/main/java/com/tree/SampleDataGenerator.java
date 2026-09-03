package com.tree;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

public class SampleDataGenerator {
    public static void main(String[] args) {
        String dbPath = "data/database.db";
        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:" + dbPath);
             Statement stmt = conn.createStatement()) {

            // Drop existing tables
            stmt.executeUpdate("DROP TABLE IF EXISTS TreeScion");
            stmt.executeUpdate("DROP TABLE IF EXISTS Scion");
            stmt.executeUpdate("DROP TABLE IF EXISTS Note");
            stmt.executeUpdate("DROP TABLE IF EXISTS Image");
            stmt.executeUpdate("DROP TABLE IF EXISTS Tree");
            stmt.executeUpdate("DROP TABLE IF EXISTS Location");
            stmt.executeUpdate("DROP TABLE IF EXISTS TreeSpecies");
            stmt.executeUpdate("DROP TABLE IF EXISTS Collection");

            // Create Tables
            stmt.executeUpdate("CREATE TABLE Collection (id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT NOT NULL, selected INTEGER DEFAULT 0)");
            stmt.executeUpdate("CREATE TABLE TreeSpecies (latinName TEXT PRIMARY KEY, englishName TEXT, frenchName TEXT, characteristics TEXT, otherNames TEXT, fruitingStartMonth INTEGER, fruitingDescription TEXT)");
            stmt.executeUpdate("CREATE TABLE Location (locationId INTEGER PRIMARY KEY AUTOINCREMENT, latitude REAL NOT NULL, longitude REAL NOT NULL)");
            stmt.executeUpdate("CREATE TABLE Tree (treeId INTEGER PRIMARY KEY AUTOINCREMENT, latinName TEXT NOT NULL, locationId INTEGER, collectionId INTEGER NOT NULL, datePlanted DATE, origin TEXT, rootstock TEXT, variety TEXT, located TEXT, label TEXT, status TEXT NOT NULL DEFAULT 'unverified')");
            stmt.executeUpdate("CREATE TABLE Image (imageId INTEGER PRIMARY KEY AUTOINCREMENT, treeId INTEGER NOT NULL, imagePath TEXT NOT NULL, dateAdded DATE)");
            stmt.executeUpdate("CREATE TABLE Note (noteID INTEGER PRIMARY KEY AUTOINCREMENT, treeId INTEGER NOT NULL, dateWritten DATE, description TEXT, imageId INTEGER)");
            stmt.executeUpdate("CREATE TABLE Scion (scionId INTEGER PRIMARY KEY AUTOINCREMENT, species TEXT NOT NULL, variety TEXT, source TEXT, attached INTEGER NOT NULL DEFAULT 0, fruitingStartMonth INTEGER, fruitingDescription TEXT)");
            stmt.executeUpdate("CREATE TABLE TreeScion (treeScionId INTEGER PRIMARY KEY AUTOINCREMENT, treeId INTEGER NOT NULL, scionId INTEGER NOT NULL, dateAdded DATE)");

            // Insert Collections
            stmt.executeUpdate("INSERT INTO Collection (id, name, selected) VALUES (1, 'Main Orchard', 1)");
            stmt.executeUpdate("INSERT INTO Collection (id, name, selected) VALUES (2, 'North Paddock', 0)");

            // Insert Species
            stmt.executeUpdate("INSERT INTO TreeSpecies VALUES ('Malus domestica', 'Apple', 'Pommier', 'Deciduous fruit tree', 'Domestic Apple', 9, 'Harvest September to October')");
            stmt.executeUpdate("INSERT INTO TreeSpecies VALUES ('Pyrus communis', 'Pear', 'Poirier', 'European pear tree', 'Common Pear', 8, 'Harvest late August through October')");
            stmt.executeUpdate("INSERT INTO TreeSpecies VALUES ('Prunus avium', 'Sweet Cherry', 'Cerisier', 'Flowering fruit tree', 'Wild Cherry', 7, 'Harvest July')");
            stmt.executeUpdate("INSERT INTO TreeSpecies VALUES ('Prunus domestica', 'Plum', 'Prunier', 'Stone fruit tree', 'European Plum', 8, 'Harvest August to September')");

            // Insert Locations (Centred on a sample orchard)
            stmt.executeUpdate("INSERT INTO Location (locationId, latitude, longitude) VALUES (1, 51.5074, -0.1278)");
            stmt.executeUpdate("INSERT INTO Location (locationId, latitude, longitude) VALUES (2, 51.5078, -0.1282)");
            stmt.executeUpdate("INSERT INTO Location (locationId, latitude, longitude) VALUES (3, 51.5072, -0.1273)");
            stmt.executeUpdate("INSERT INTO Location (locationId, latitude, longitude) VALUES (4, 51.5085, -0.1265)");

            // Insert Trees
            stmt.executeUpdate("INSERT INTO Tree (treeId, latinName, locationId, collectionId, datePlanted, origin, rootstock, variety, located, label, status) " +
                    "VALUES (1, 'Malus domestica', 1, 1, '2022-03-15', 'Local Heritage Nursery', 'MM106', 'Bramley', 'Row 1, Plot A', 'T-01', 'verified')");
            stmt.executeUpdate("INSERT INTO Tree (treeId, latinName, locationId, collectionId, datePlanted, origin, rootstock, variety, located, label, status) " +
                    "VALUES (2, 'Pyrus communis', 2, 1, '2022-03-15', 'Orchard Club', 'Quince A', 'Conference', 'Row 1, Plot B', 'T-02', 'verified')");
            stmt.executeUpdate("INSERT INTO Tree (treeId, latinName, locationId, collectionId, datePlanted, origin, rootstock, variety, located, label, status) " +
                    "VALUES (3, 'Prunus avium', 3, 1, '2023-04-10', 'Direct Graft', 'Colt', 'Stella', 'Row 2, Plot A', 'T-03', 'unverified')");
            stmt.executeUpdate("INSERT INTO Tree (treeId, latinName, locationId, collectionId, datePlanted, origin, rootstock, variety, located, label, status) " +
                    "VALUES (4, 'Prunus domestica', 4, 2, '2021-11-20', 'Farm Exchange', 'St Julien A', 'Victoria', 'North Meadow', 'N-01', 'verified')");

            // Insert Notes
            stmt.executeUpdate("INSERT INTO Note (noteID, treeId, dateWritten, description, imageId) " +
                    "VALUES (1, 1, '2023-08-15', 'Heavy blossom set this spring. Pruned water shoots in July.', NULL)");
            stmt.executeUpdate("INSERT INTO Note (noteID, treeId, dateWritten, description, imageId) " +
                    "VALUES (2, 2, '2023-09-02', 'Excellent fruit size, ready for harvesting next week.', NULL)");

            // Insert Scions
            stmt.executeUpdate("INSERT INTO Scion (scionId, species, variety, source, attached, fruitingStartMonth, fruitingDescription) " +
                    "VALUES (1, 'Malus domestica', 'Discovery', 'Heritage Orchard', 1, 8, 'Early dessert apple')");
            stmt.executeUpdate("INSERT INTO TreeScion (treeScionId, treeId, scionId, dateAdded) VALUES (1, 1, 1, '2023-04-01')");

            System.out.println("Sample SQLite database created successfully at " + dbPath);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
