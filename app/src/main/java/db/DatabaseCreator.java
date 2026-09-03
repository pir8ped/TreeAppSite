package db;

import android.database.sqlite.SQLiteOpenHelper;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import java.io.IOException;

import util.TreeImporter;

public class DatabaseCreator extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "mydatabase.db";
    private static final int DATABASE_VERSION = 51;
    private static final String TAG = "DatabaseCreator";
    private static DatabaseCreator instance;
    private final Context context;

    // DAO instances
    private CollectionDAO collectionDAO;
    private TreeDAO treeDAO;
    private LocationDAO locationDAO;
    private NoteDAO noteDAO;
    private ScionDAO scionDAO;
    private TreeScionDAO treeScionDAO;
    private boolean daosInitialized = false;

    /*
     * Tree import functionality preserved but commented out as it's no longer
     * needed
     * // Flag to track tree import status
     * private boolean treesImported = false;
     */

    // Table Names
    private static final String TABLE_COLLECTION = "Collection";
    private static final String TABLE_TREE_SPECIES = "TreeSpecies";
    private static final String TABLE_TREE = "Tree";
    private static final String TABLE_LOCATION = "Location";
    private static final String TABLE_NOTE = "Note";
    private static final String TABLE_REMINDER = "Reminder";
    private static final String TABLE_IMAGE = "Image";
    private static final String TABLE_SCION = "Scion";
    private static final String TABLE_TREE_SCION = "TreeScion";

    private DatabaseCreator(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        this.context = context.getApplicationContext();
        Log.d(TAG, "DatabaseCreator initialized with context: " + context);
        Log.d(TAG, "Database path: " + context.getDatabasePath(DATABASE_NAME).getAbsolutePath());
    }

    @Override
    public void onConfigure(SQLiteDatabase db) {
        super.onConfigure(db);
        db.setForeignKeyConstraintsEnabled(true);
        Log.d(TAG, "Foreign key constraints enabled");
    }

    public static synchronized DatabaseCreator getInstance() {
        if (instance == null) {
            throw new IllegalStateException("DatabaseCreator must be initialized with a context");
        }
        return instance;
    }

    public static void initialize(Context context) {
        if (instance == null) {
            instance = new DatabaseCreator(context);
        }
    }

    /*
     * Tree import functionality preserved but commented out as it's no longer
     * needed
     * /**
     * Import trees from CSV file.
     * This method ensures trees are only imported once.
     */
    // public synchronized void importTreesIfNeeded() {
    // if (treesImported) {
    // Log.d(TAG, "Trees already imported, skipping import");
    // return;
    // }
    //
    // SQLiteDatabase db = getWritableDatabase();
    //
    // // Make sure DAOs are initialized
    // if (!daosInitialized) {
    // initializeDAOs();
    // }
    //
    // Log.i(TAG, "Starting tree import...");
    // try {
    // // Pass all DAOs to TreeImporter
    // TreeImporter importer = new TreeImporter(
    // context,
    // db,
    // collectionDAO,
    // treeDAO,
    // locationDAO,
    // noteDAO,
    // true
    // );
    // importer.importTrees(context);
    // Log.i(TAG, "Tree import completed successfully");
    // treesImported = true;
    // } catch (IOException e) {
    // Log.e(TAG, "Error importing trees: " + e.getMessage(), e);
    // }
    // }
    // */

    /**
     * Initialize all DAOs. This method should be called after the database is
     * created
     * but before any DAOs are requested by other components.
     */
    public synchronized void initializeDAOs() {
        if (daosInitialized) {
            return;
        }

        Log.d(TAG, "Initializing DAOs");
        // We'll create the DAOs directly - no need to get the database here
        // since DAOs will get it from DatabaseCreator.getInstance() when needed

        collectionDAO = new CollectionDAOImpl();
        treeDAO = new TreeDAOImpl();
        locationDAO = new LocationDAOImpl();
        noteDAO = new NoteDAOImpl();
        scionDAO = new ScionDAOImpl();
        treeScionDAO = new TreeScionDAOImpl();

        daosInitialized = true;
        Log.d(TAG, "DAOs initialized successfully");
    }

    // Getter methods for DAOs

    public CollectionDAO getCollectionDAO() {
        if (!daosInitialized) {
            initializeDAOs();
        }
        return collectionDAO;
    }

    public TreeDAO getTreeDAO() {
        if (!daosInitialized) {
            initializeDAOs();
        }
        return treeDAO;
    }

    public LocationDAO getLocationDAO() {
        if (!daosInitialized) {
            initializeDAOs();
        }
        return locationDAO;
    }

    public NoteDAO getNoteDAO() {
        if (!daosInitialized) {
            initializeDAOs();
        }
        return noteDAO;
    }

    public ScionDAO getScionDAO() {
        if (!daosInitialized) {
            initializeDAOs();
        }
        return scionDAO;
    }

    public TreeScionDAO getTreeScionDAO() {
        if (!daosInitialized) {
            initializeDAOs();
        }
        return treeScionDAO;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        Log.i(TAG, "Creating database tables...");

        try {
            // Create tables
            db.execSQL(CREATE_TABLE_COLLECTION);
            db.execSQL(CREATE_TABLE_TREE_SPECIES);
            db.execSQL(CREATE_TABLE_TREE);
            db.execSQL(CREATE_TABLE_LOCATION);
            db.execSQL(CREATE_TABLE_NOTE);
            db.execSQL(CREATE_TABLE_REMINDER);
            db.execSQL(CREATE_TABLE_IMAGE);

            // Create indexes
            db.execSQL(CREATE_INDEX_LOCATION_COORDINATES);
            db.execSQL(CREATE_INDEX_NOTE_TREE_ID);
            db.execSQL(CREATE_INDEX_REMINDER_TREE_ID);
            db.execSQL(CREATE_INDEX_IMAGE_TREE_ID);
            db.execSQL(CREATE_INDEX_TREE_COLLECTION);
            db.execSQL(CREATE_INDEX_TREE_LABEL_COLLECTION);

            Log.i(TAG, "Tables and indexes created successfully");

            // First, bulk upload tree species data
            Log.i(TAG, "Starting tree species import...");
            TreeSpeciesBulkUploader.bulkUploadTreeSpecies(context, db);

            // Ensure the TreeSpecies are committed to the database
            db.setTransactionSuccessful();
            db.endTransaction();
            db.beginTransaction();

            // Initialize DAOs
            initializeDAOs();

            // Note: Tree import is now handled by importTreesIfNeeded()
            // and will be called from BaseActivity

        } catch (Exception e) {
            Log.e(TAG, "Error during database creation: " + e.getMessage());
            e.printStackTrace();
        }
        Log.i(TAG, "Database initialization completed successfully");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.i(TAG, "Upgrading database from version " + oldVersion + " to " + newVersion);

        // Create automatic backup before migration
        try {
            com.john.TreeApp.utils.DatabaseBackupManager backupManager = new com.john.TreeApp.utils.DatabaseBackupManager(
                    context);
            String backupPath = backupManager.createLocalBackup();
            Log.i(TAG, "Created automatic backup before migration: " + backupPath);
        } catch (Exception e) {
            Log.e(TAG, "Warning: Could not create backup before migration: " + e.getMessage());
            // Continue with migration even if backup fails
        }

        try {
            // Apply migrations based on version
            if (oldVersion < 45) {
                // Add Scion and TreeScion tables
                Log.i(TAG, "Adding Scion and TreeScion tables...");
                db.execSQL(CREATE_TABLE_SCION);
                db.execSQL(CREATE_TABLE_TREE_SCION);
                db.execSQL(CREATE_INDEX_TREE_SCION_TREE_ID);
                db.execSQL(CREATE_INDEX_TREE_SCION_SCION_ID);
                db.execSQL(CREATE_INDEX_SCION_SPECIES);
                Log.i(TAG, "Successfully added Scion tables and indexes");
            }
            if (oldVersion < 46) {
                // Add quantity column to Scion table (later replaced in v47)
                Log.i(TAG, "Adding quantity column to Scion table...");
                db.execSQL("ALTER TABLE Scion ADD COLUMN quantity INTEGER NOT NULL DEFAULT 1");
                Log.i(TAG, "Successfully added quantity column");
            }
            if (oldVersion < 47) {
                // Replace quantity with attached boolean
                Log.i(TAG, "Replacing quantity with attached field...");
                // Add attached column
                db.execSQL("ALTER TABLE Scion ADD COLUMN attached INTEGER NOT NULL DEFAULT 0");
                // Remove quantity column by recreating table
                db.execSQL("CREATE TABLE Scion_new (" +
                        "scionId INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        "species TEXT NOT NULL, " +
                        "variety TEXT, " +
                        "source TEXT, " +
                        "attached INTEGER NOT NULL DEFAULT 0" +
                        ")");
                db.execSQL("INSERT INTO Scion_new (scionId, species, variety, source, attached) " +
                        "SELECT scionId, species, variety, source, attached FROM Scion");
                db.execSQL("DROP TABLE Scion");
                db.execSQL("ALTER TABLE Scion_new RENAME TO Scion");
                // Recreate index
                db.execSQL(CREATE_INDEX_SCION_SPECIES);
                Log.i(TAG, "Successfully replaced quantity with attached field");
            }
            if (oldVersion < 48) {
                Log.i(TAG, "Adding fruiting columns to TreeSpecies and Scion tables...");
                db.execSQL("ALTER TABLE TreeSpecies ADD COLUMN fruitingStartMonth INTEGER");
                db.execSQL("ALTER TABLE TreeSpecies ADD COLUMN fruitingDescription TEXT");
                db.execSQL("ALTER TABLE Scion ADD COLUMN fruitingStartMonth INTEGER");
                db.execSQL("ALTER TABLE Scion ADD COLUMN fruitingDescription TEXT");
                Log.i(TAG, "Successfully added fruiting columns");
            }
            if (oldVersion < 49) {
                Log.i(TAG, "Adding status column to Tree table...");
                db.execSQL("ALTER TABLE Tree ADD COLUMN status TEXT NOT NULL DEFAULT 'unverified'");
                Log.i(TAG, "Successfully added status column");
            }
            if (oldVersion < 50) {
                Log.i(TAG, "Adding dateWritten and isUrgent columns to Reminder table...");
                db.execSQL("ALTER TABLE Reminder ADD COLUMN dateWritten DATE");
                db.execSQL("ALTER TABLE Reminder ADD COLUMN isUrgent INTEGER NOT NULL DEFAULT 0");
                Log.i(TAG, "Successfully added Reminder columns");
            }
            if (oldVersion < 51) {
                Log.i(TAG, "Adding imageId column to Note table...");
                db.execSQL("ALTER TABLE Note ADD COLUMN imageId INTEGER");
                db.execSQL("CREATE INDEX idx_note_image_id ON Note(imageId)");
                Log.i(TAG, "Successfully added imageId column to Note table");
            }

            Log.i(TAG, "Database upgrade completed successfully");
        } catch (Exception e) {
            Log.e(TAG, "Error during database upgrade: " + e.getMessage());
            e.printStackTrace();
            throw e; // Re-throw to ensure upgrade fails if there's an error
        }
    }

    // SQL commands to create tables
    private static final String CREATE_TABLE_COLLECTION = "CREATE TABLE IF NOT EXISTS " + TABLE_COLLECTION + " (" +
            "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
            "name TEXT NOT NULL, " +
            "selected INTEGER DEFAULT 0);";

    private static final String CREATE_TABLE_TREE_SPECIES = "CREATE TABLE " + TABLE_TREE_SPECIES + " (" +
            "latinName TEXT PRIMARY KEY, " +
            "englishName TEXT, " +
            "frenchName TEXT, " +
            "characteristics TEXT, " +
            "otherNames TEXT, " +
            "fruitingStartMonth INTEGER, " +
            "fruitingDescription TEXT" +
            ");";

    private static final String CREATE_TABLE_TREE = "CREATE TABLE " + TABLE_TREE + " (" +
            "treeId INTEGER PRIMARY KEY AUTOINCREMENT, " +
            "latinName TEXT NOT NULL, " +
            "locationId INTEGER, " +
            "collectionId INTEGER NOT NULL, " +
            "datePlanted DATE DEFAULT (datetime('now')), " +
            "origin TEXT, " +
            "rootstock TEXT, " +
            "variety TEXT, " +
            "located TEXT, " +
            "label TEXT, " +
            "status TEXT NOT NULL DEFAULT 'unverified', " +
            "FOREIGN KEY(latinName) REFERENCES " + TABLE_TREE_SPECIES + "(latinName) ON DELETE CASCADE, " +
            "FOREIGN KEY(locationId) REFERENCES " + TABLE_LOCATION + "(locationId) ON DELETE SET NULL, " +
            "FOREIGN KEY(collectionId) REFERENCES " + TABLE_COLLECTION + "(id) ON DELETE CASCADE" +
            ");";

    private static final String CREATE_TABLE_LOCATION = "CREATE TABLE " + TABLE_LOCATION + " (" +
            "locationId INTEGER PRIMARY KEY AUTOINCREMENT, " +
            "latitude REAL NOT NULL, " +
            "longitude REAL NOT NULL " +
            ");";

    private static final String CREATE_TABLE_NOTE = "CREATE TABLE " + TABLE_NOTE + " (" +
            "noteID INTEGER PRIMARY KEY AUTOINCREMENT, " +
            "treeId INTEGER NOT NULL, " +
            "dateWritten DATE, " +
            "description TEXT, " +
            "imageId INTEGER, " +
            "FOREIGN KEY(treeId) REFERENCES " + TABLE_TREE + "(treeId) ON DELETE CASCADE, " +
            "FOREIGN KEY(imageId) REFERENCES " + TABLE_IMAGE + "(imageId) ON DELETE SET NULL" +
            ");";

    private static final String CREATE_TABLE_REMINDER = "CREATE TABLE " + TABLE_REMINDER + " (" +
            "reminderId INTEGER PRIMARY KEY AUTOINCREMENT, " +
            "treeId INTEGER NOT NULL, " +
            "dateWritten DATE, " +
            "reminderDate DATE, " +
            "description TEXT, " +
            "isUrgent INTEGER NOT NULL DEFAULT 0, " +
            "FOREIGN KEY(treeId) REFERENCES " + TABLE_TREE + "(treeId) ON DELETE CASCADE" +
            ");";

    private static final String CREATE_TABLE_IMAGE = "CREATE TABLE " + TABLE_IMAGE + " (" +
            "imageId INTEGER PRIMARY KEY AUTOINCREMENT, " +
            "treeId INTEGER NOT NULL, " +
            "imagePath TEXT NOT NULL, " +
            "dateAdded DATE DEFAULT (datetime('now')), " +
            "FOREIGN KEY(treeId) REFERENCES " + TABLE_TREE + "(treeId) ON DELETE CASCADE" +
            ");";

    private static final String CREATE_INDEX_LOCATION_COORDINATES = "CREATE INDEX idx_location_coordinates ON "
            + TABLE_LOCATION + "(latitude, longitude);";

    private static final String CREATE_INDEX_NOTE_TREE_ID = "CREATE INDEX idx_note_tree_id ON " + TABLE_NOTE
            + "(treeId);";

    private static final String CREATE_INDEX_REMINDER_TREE_ID = "CREATE INDEX idx_reminder_tree_id ON " + TABLE_REMINDER
            + "(treeId);";

    private static final String CREATE_INDEX_IMAGE_TREE_ID = "CREATE INDEX idx_image_tree_id ON " + TABLE_IMAGE
            + "(treeId);";

    private static final String CREATE_INDEX_TREE_COLLECTION = "CREATE INDEX idx_tree_collection ON " + TABLE_TREE
            + "(collectionId);";

    private static final String CREATE_INDEX_TREE_LABEL_COLLECTION = "CREATE INDEX idx_tree_label_collection ON "
            + TABLE_TREE + "(label, collectionId);";
    private static final String CREATE_TABLE_SCION = "CREATE TABLE IF NOT EXISTS " + TABLE_SCION + " (" +
            "scionId INTEGER PRIMARY KEY AUTOINCREMENT, " +
            "species TEXT NOT NULL, " +
            "variety TEXT, " +
            "source TEXT, " +
            "attached INTEGER NOT NULL DEFAULT 0, " +
            "fruitingStartMonth INTEGER, " +
            "fruitingDescription TEXT" +
            ");";

    private static final String CREATE_TABLE_TREE_SCION = "CREATE TABLE IF NOT EXISTS " + TABLE_TREE_SCION + " (" +
            "treeScionId INTEGER PRIMARY KEY AUTOINCREMENT, " +
            "treeId INTEGER NOT NULL, " +
            "scionId INTEGER NOT NULL, " +
            "dateAdded DATE DEFAULT (datetime('now')), " +
            "FOREIGN KEY(treeId) REFERENCES " + TABLE_TREE + "(treeId) ON DELETE CASCADE, " +
            "FOREIGN KEY(scionId) REFERENCES " + TABLE_SCION + "(scionId) ON DELETE CASCADE" +
            ");";

    private static final String CREATE_INDEX_TREE_SCION_TREE_ID = "CREATE INDEX idx_tree_scion_tree_id ON "
            + TABLE_TREE_SCION + "(treeId);";

    private static final String CREATE_INDEX_TREE_SCION_SCION_ID = "CREATE INDEX idx_tree_scion_scion_id ON "
            + TABLE_TREE_SCION + "(scionId);";

    private static final String CREATE_INDEX_SCION_SPECIES = "CREATE INDEX idx_scion_species ON " + TABLE_SCION
            + "(species);";
}
