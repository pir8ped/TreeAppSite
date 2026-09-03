package db;

import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

public class DAOBase {
    private static final String TAG = "DAOBase";

    protected SQLiteDatabase getDatabase() {
        return DatabaseCreator.getInstance().getWritableDatabase();
    }

    /**
     * Constructor for when you need to use a specific database instance.
     * Typically used for transaction management or testing.
     */
    public DAOBase(SQLiteDatabase database) {
        // This constructor can remain for specific cases, but we should prefer dynamic
        // access
    }

    /**
     * Default constructor.
     */
    public DAOBase() {
    }

    // /**
    // * Constructor that specifies whether a writable database is needed.
    // * Use this for DAOs that perform write operations.
    // */
    // public DAOBase(boolean needsWritable) {
    // this.database = needsWritable ? getWritableDatabase() :
    // getReadableDatabase();
    // }

    /**
     * Gets a readable database instance. Use this for all read operations.
     * This is more performant than getWritableDatabase() when you only need to
     * read.
     */
    // protected static SQLiteDatabase getReadableDatabase() {
    // return DatabaseCreator.getInstance().getReadableDatabase();
    // }

    /**
     * Gets a writable database instance. Only use this when you need to write to
     * the database.
     * This should be used for insert, update, and delete operations.
     */
    protected static SQLiteDatabase getWritableDatabase() {
        return DatabaseCreator.getInstance().getWritableDatabase();
    }

    /**
     * Executes operations within a transaction. Automatically handles
     * begin/commit/rollback.
     * 
     * @param operations The database operations to execute within the transaction
     * @return true if transaction was successful, false if it was rolled back
     */
    protected boolean executeInTransaction(TransactionOperations operations) {
        if (!getDatabase().isOpen()) {
            Log.e(TAG, "Database is not open");
            return false;
        }

        getDatabase().beginTransaction();
        try {
            operations.execute();
            getDatabase().setTransactionSuccessful();
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Transaction failed: " + e.getMessage());
            return false;
        } finally {
            getDatabase().endTransaction();
        }
    }

    /**
     * Checks if the database is ready for operations
     * 
     * @return true if database is open and ready for operations
     */
    protected boolean isDatabaseReady() {
        return getDatabase() != null && getDatabase().isOpen();
    }

    /**
     * Interface for transaction operations
     */
    protected interface TransactionOperations {
        void execute() throws Exception;
    }

    /**
     * Safely closes a cursor if it's not null
     * 
     * @param cursor The cursor to close
     */
    protected static void closeCursor(android.database.Cursor cursor) {
        if (cursor != null && !cursor.isClosed()) {
            cursor.close();
        }
    }
}
