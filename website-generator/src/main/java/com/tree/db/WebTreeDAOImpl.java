package com.tree.db;

import com.tree.beans.*;
import org.sqlite.SQLiteConfig;

import java.sql.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Properties;

public class WebTreeDAOImpl implements WebTreeDAO {
    private final String dbUrl;
    private final Properties dbProps;

    public WebTreeDAOImpl(String dbPath) {
        this.dbUrl = "jdbc:sqlite:" + dbPath;
        SQLiteConfig config = new SQLiteConfig();
        config.setReadOnly(true);
        this.dbProps = config.toProperties();
    }

    private Connection getConnection() throws SQLException {
        return DriverManager.getConnection(dbUrl, dbProps);
    }

    @Override
    public List<Collection> getAllCollections() {
        List<Collection> list = new ArrayList<>();
        String sql = "SELECT id, name, selected FROM Collection ORDER BY name ASC";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(new Collection(
                        rs.getInt("id"),
                        rs.getString("name"),
                        rs.getInt("selected") == 1
                ));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    @Override
    public Collection getCollectionById(int collectionId) {
        String sql = "SELECT id, name, selected FROM Collection WHERE id = ?";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, collectionId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new Collection(
                            rs.getInt("id"),
                            rs.getString("name"),
                            rs.getInt("selected") == 1
                    );
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public TreeStatistics getStatistics() {
        TreeStatistics stats = new TreeStatistics();
        String totalSql = "SELECT status, COUNT(*) as count FROM Tree WHERE locationId IS NOT NULL GROUP BY status";
        String speciesSql = "SELECT COUNT(DISTINCT UPPER(latinName)) FROM Tree WHERE locationId IS NOT NULL";
        String photoSql = "SELECT COUNT(*) FROM Image";
        String scionSql = "SELECT COUNT(*) FROM TreeScion";

        try (Connection conn = getConnection()) {
            try (PreparedStatement ps = conn.prepareStatement(totalSql);
                 ResultSet rs = ps.executeQuery()) {
                int total = 0;
                while (rs.next()) {
                    String status = rs.getString("status");
                    int count = rs.getInt("count");
                    total += count;
                    if ("verified".equalsIgnoreCase(status)) {
                        stats.setVerifiedCount(count);
                    } else if ("lost".equalsIgnoreCase(status)) {
                        stats.setLostCount(count);
                    } else {
                        stats.setUnverifiedCount(stats.getUnverifiedCount() + count);
                    }
                }
                stats.setTotalCount(total);
            }

            try (PreparedStatement ps = conn.prepareStatement(speciesSql);
                 ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    stats.setSpeciesCount(rs.getInt(1));
                }
            }

            try (PreparedStatement ps = conn.prepareStatement(photoSql);
                 ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    stats.setPhotoCount(rs.getInt(1));
                }
            }

            try (PreparedStatement ps = conn.prepareStatement(scionSql);
                 ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    stats.setScionCount(rs.getInt(1));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return stats;
    }

    @Override
    public TreeStatistics getCollectionStatistics(int collectionId) {
        TreeStatistics stats = new TreeStatistics();
        String totalSql = "SELECT status, COUNT(*) as count FROM Tree WHERE collectionId = ? AND locationId IS NOT NULL GROUP BY status";
        String speciesSql = "SELECT COUNT(DISTINCT UPPER(latinName)) FROM Tree WHERE collectionId = ? AND locationId IS NOT NULL";

        try (Connection conn = getConnection()) {
            try (PreparedStatement ps = conn.prepareStatement(totalSql)) {
                ps.setInt(1, collectionId);
                try (ResultSet rs = ps.executeQuery()) {
                    int total = 0;
                    while (rs.next()) {
                        String status = rs.getString("status");
                        int count = rs.getInt("count");
                        total += count;
                        if ("verified".equalsIgnoreCase(status)) {
                            stats.setVerifiedCount(count);
                        } else if ("lost".equalsIgnoreCase(status)) {
                            stats.setLostCount(count);
                        } else {
                            stats.setUnverifiedCount(stats.getUnverifiedCount() + count);
                        }
                    }
                    stats.setTotalCount(total);
                }
            }

            try (PreparedStatement ps = conn.prepareStatement(speciesSql)) {
                ps.setInt(1, collectionId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        stats.setSpeciesCount(rs.getInt(1));
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return stats;
    }

    @Override
    public List<Tree> getAllPlantedTrees() {
        return queryTrees("WHERE t.locationId IS NOT NULL ORDER BY t.label ASC, t.treeId ASC", null);
    }

    @Override
    public List<Tree> getTreesForCollection(int collectionId) {
        return queryTrees("WHERE t.collectionId = ? AND t.locationId IS NOT NULL ORDER BY t.label ASC, t.treeId ASC", collectionId);
    }

    @Override
    public Tree getTreeById(int treeId) {
        List<Tree> trees = queryTrees("WHERE t.treeId = ?", treeId);
        if (trees.isEmpty()) return null;
        Tree tree = trees.get(0);
        tree.setImages(getImagesForTree(treeId));
        tree.setNotes(getNotesForTree(treeId));
        tree.setScions(getScionsForTree(treeId));
        return tree;
    }

    private List<Tree> queryTrees(String whereClause, Integer param) {
        List<Tree> list = new ArrayList<>();
        String sql = "SELECT t.treeId, t.latinName, t.locationId, t.collectionId, t.datePlanted, " +
                "t.origin, t.rootstock, t.variety, t.located, t.label, t.status, " +
                "s.englishName, s.frenchName, s.characteristics, s.otherNames, s.fruitingDescription, " +
                "c.name as collectionName, l.latitude, l.longitude, " +
                "(SELECT imagePath FROM Image WHERE treeId = t.treeId ORDER BY dateAdded DESC LIMIT 1) as primaryImagePath " +
                "FROM Tree t " +
                "LEFT JOIN TreeSpecies s ON UPPER(t.latinName) = UPPER(s.latinName) " +
                "LEFT JOIN Collection c ON t.collectionId = c.id " +
                "LEFT JOIN Location l ON t.locationId = l.locationId " +
                whereClause;

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            if (param != null) {
                ps.setInt(1, param);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Tree tree = new Tree();
                    tree.setTreeId(rs.getInt("treeId"));
                    tree.setLatinName(rs.getString("latinName"));
                    int locId = rs.getInt("locationId");
                    if (!rs.wasNull()) {
                        tree.setLocationId(locId);
                        tree.setLocation(new Location(locId, rs.getDouble("latitude"), rs.getDouble("longitude")));
                    }
                    int colId = rs.getInt("collectionId");
                    if (!rs.wasNull()) {
                        tree.setCollectionId(colId);
                    }
                    Date datePlanted = parseDate(rs.getString("datePlanted"));
                    tree.setDatePlanted(datePlanted);
                    tree.setOrigin(rs.getString("origin"));
                    tree.setRootstock(rs.getString("rootstock"));
                    tree.setVariety(rs.getString("variety"));
                    tree.setLocated(rs.getString("located"));
                    tree.setLabel(rs.getString("label"));
                    tree.setStatus(rs.getString("status"));
                    tree.setEnglishName(rs.getString("englishName"));
                    tree.setFrenchName(rs.getString("frenchName"));
                    tree.setCharacteristics(rs.getString("characteristics"));
                    tree.setOtherNames(rs.getString("otherNames"));
                    tree.setFruitingDescription(rs.getString("fruitingDescription"));
                    tree.setCollectionName(rs.getString("collectionName"));
                    tree.setPrimaryImagePath(rs.getString("primaryImagePath"));

                    list.add(tree);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    @Override
    public List<TreeForMap> getAllTreesForMap() {
        return queryTreesForMap("WHERE t.locationId IS NOT NULL", null);
    }

    @Override
    public List<TreeForMap> getTreesForMap(int collectionId) {
        return queryTreesForMap("WHERE t.collectionId = ? AND t.locationId IS NOT NULL", collectionId);
    }

    private List<TreeForMap> queryTreesForMap(String whereClause, Integer param) {
        List<TreeForMap> list = new ArrayList<>();
        String sql = "SELECT t.treeId, t.latinName, t.variety, t.rootstock, t.label, t.status, t.collectionId, " +
                "s.englishName, c.name as collectionName, l.latitude, l.longitude, " +
                "(SELECT imagePath FROM Image WHERE treeId = t.treeId ORDER BY dateAdded DESC LIMIT 1) as latestImagePath " +
                "FROM Tree t " +
                "JOIN Location l ON t.locationId = l.locationId " +
                "LEFT JOIN TreeSpecies s ON UPPER(t.latinName) = UPPER(s.latinName) " +
                "LEFT JOIN Collection c ON t.collectionId = c.id " +
                whereClause + " AND (t.status IS NULL OR LOWER(t.status) != 'lost')";

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            if (param != null) {
                ps.setInt(1, param);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    TreeForMap tfm = new TreeForMap();
                    tfm.setId(rs.getInt("treeId"));
                    tfm.setLatinName(rs.getString("latinName"));
                    tfm.setVariety(rs.getString("variety"));
                    tfm.setRootstock(rs.getString("rootstock"));
                    tfm.setLabel(rs.getString("label"));
                    tfm.setStatus(rs.getString("status"));
                    tfm.setCollectionId(rs.getInt("collectionId"));
                    tfm.setCollectionName(rs.getString("collectionName"));
                    tfm.setEnglishName(rs.getString("englishName"));
                    tfm.setLatitude(rs.getDouble("latitude"));
                    tfm.setLongitude(rs.getDouble("longitude"));
                    tfm.setLatestImagePath(rs.getString("latestImagePath"));
                    list.add(tfm);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    @Override
    public List<Image> getImagesForTree(int treeId) {
        List<Image> list = new ArrayList<>();
        String sql = "SELECT i.imageId, i.treeId, i.dateAdded, i.imagePath, " +
                "(SELECT n.description FROM Note n WHERE n.imageId = i.imageId LIMIT 1) as caption " +
                "FROM Image i WHERE i.treeId = ? ORDER BY i.dateAdded DESC";

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, treeId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Image img = new Image();
                    img.setImageId(rs.getInt("imageId"));
                    img.setTreeId(rs.getInt("treeId"));
                    img.setDateTaken(parseDate(rs.getString("dateAdded")));
                    img.setImageUrlOrFileName(rs.getString("imagePath"));
                    img.setCaption(rs.getString("caption"));
                    list.add(img);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    @Override
    public List<Note> getNotesForTree(int treeId) {
        List<Note> list = new ArrayList<>();
        String sql = "SELECT noteID, treeId, dateWritten, description, imageId FROM Note WHERE treeId = ? ORDER BY dateWritten DESC";

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, treeId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Note note = new Note();
                    note.setNoteId(rs.getInt("noteID"));
                    note.setTreeId(rs.getInt("treeId"));
                    note.setDateWritten(parseDate(rs.getString("dateWritten")));
                    note.setDescription(rs.getString("description"));
                    int imgId = rs.getInt("imageId");
                    if (!rs.wasNull()) {
                        note.setImageId(imgId);
                    }
                    list.add(note);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    private static java.util.Date parseDate(String str) {
        if (str == null || str.trim().isEmpty()) return null;
        str = str.trim();
        String[] formats = {
                "yyyy-MM-dd HH:mm:ss",
                "yyyy-MM-dd'T'HH:mm:ss",
                "yyyy-MM-dd",
                "yyyy/MM/dd",
                "dd/MM/yyyy",
                "MMM dd, yyyy"
        };
        for (String fmt : formats) {
            try {
                java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat(fmt, java.util.Locale.US);
                return sdf.parse(str);
            } catch (Exception ignored) {}
        }
        try {
            long millis = Long.parseLong(str);
            return new java.util.Date(millis);
        } catch (Exception ignored) {}
        return null;
    }

    @Override
    public List<Scion> getScionsForTree(int treeId) {
        List<Scion> list = new ArrayList<>();
        String sql = "SELECT s.scionId, s.species, s.variety, s.source, s.attached, s.fruitingStartMonth, s.fruitingDescription " +
                "FROM Scion s JOIN TreeScion ts ON s.scionId = ts.scionId WHERE ts.treeId = ?";

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, treeId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Scion scion = new Scion();
                    scion.setScionId(rs.getInt("scionId"));
                    scion.setSpecies(rs.getString("species"));
                    scion.setVariety(rs.getString("variety"));
                    scion.setSource(rs.getString("source"));
                    scion.setAttached(rs.getInt("attached") == 1);
                    int m = rs.getInt("fruitingStartMonth");
                    if (!rs.wasNull()) scion.setFruitingStartMonth(m);
                    scion.setFruitingDescription(rs.getString("fruitingDescription"));
                    list.add(scion);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    @Override
    public List<TreeSpecies> getAllSpecies() {
        List<TreeSpecies> list = new ArrayList<>();
        String sql = "SELECT latinName, englishName, frenchName, characteristics, otherNames, fruitingStartMonth, fruitingDescription " +
                "FROM TreeSpecies ORDER BY englishName ASC, latinName ASC";

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                TreeSpecies s = new TreeSpecies();
                s.setLatinName(rs.getString("latinName"));
                s.setEnglishName(rs.getString("englishName"));
                s.setFrenchName(rs.getString("frenchName"));
                s.setCharacteristics(rs.getString("characteristics"));
                s.setOtherNames(rs.getString("otherNames"));
                int m = rs.getInt("fruitingStartMonth");
                if (!rs.wasNull()) s.setFruitingStartMonth(m);
                s.setFruitingDescription(rs.getString("fruitingDescription"));
                list.add(s);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    @Override
    public TreeSpecies getSpeciesByLatinName(String latinName) {
        String sql = "SELECT latinName, englishName, frenchName, characteristics, otherNames, fruitingStartMonth, fruitingDescription " +
                "FROM TreeSpecies WHERE UPPER(latinName) = UPPER(?)";

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, latinName);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    TreeSpecies s = new TreeSpecies();
                    s.setLatinName(rs.getString("latinName"));
                    s.setEnglishName(rs.getString("englishName"));
                    s.setFrenchName(rs.getString("frenchName"));
                    s.setCharacteristics(rs.getString("characteristics"));
                    s.setOtherNames(rs.getString("otherNames"));
                    int m = rs.getInt("fruitingStartMonth");
                    if (!rs.wasNull()) s.setFruitingStartMonth(m);
                    s.setFruitingDescription(rs.getString("fruitingDescription"));
                    return s;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }
}
