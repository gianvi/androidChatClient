package freem.android;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;

public class MessagesContentProvider extends ContentProvider {

	public static final String AUTHORITY = "freem.android.messages";

	public static final Uri THREADS_URI = Uri.parse("content://" + MessagesContentProvider.AUTHORITY + "/threads");

	public static final Uri MESSAGES_URI = Uri.parse("content://" + MessagesContentProvider.AUTHORITY + "/messages");

	private static final String DATABASE_NAME = "messages.db";

	private static final int DATABASE_VERSION = 1;

	private class DatabaseHelper extends SQLiteOpenHelper {

		public DatabaseHelper(Context context) {
			super(context, DATABASE_NAME, null, DATABASE_VERSION);
		}

		@Override
		public void onCreate(SQLiteDatabase db) {
			db.execSQL("CREATE TABLE threads (_id INTEGER PRIMARY KEY AUTOINCREMENT, phone_number TEXT NOT NULL, unread INTEGER NOT NULL, last_update INTEGER NOT NULL);");
			db.execSQL("CREATE TABLE messages (_id INTEGER PRIMARY KEY, thread INTEGER NOT NULL, direction INTEGER NOT NULL, message TEXT NOT NULL, ts_sent INTEGER NOT NULL);");
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		}

	}

	private DatabaseHelper mDatabaseHelper;

	@Override
	public boolean onCreate() {
		mDatabaseHelper = new DatabaseHelper(getContext());
		return true;
	}

	@Override
	public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
		SQLiteDatabase db = mDatabaseHelper.getReadableDatabase();
		Cursor c = db.query(uri.getLastPathSegment(), projection, selection, selectionArgs, null, null, sortOrder);
		c.setNotificationUri(getContext().getContentResolver(), uri);
		return c;
	}

	@Override
	public Uri insert(Uri uri, ContentValues values) {
		SQLiteDatabase db = mDatabaseHelper.getWritableDatabase();
		long rowId = db.insert(uri.getLastPathSegment(), null, values);
		if (rowId == -1) {
			throw new SQLException("Failed to insert row");
		}
		getContext().getContentResolver().notifyChange(uri, null);
		return Uri.withAppendedPath(uri, String.valueOf(rowId));
	}

	@Override
	public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
		SQLiteDatabase db = mDatabaseHelper.getWritableDatabase();
		int count = db.update(uri.getLastPathSegment(), values, selection, selectionArgs);
		getContext().getContentResolver().notifyChange(uri, null);
		return count;
	}

	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs) {
		SQLiteDatabase db = mDatabaseHelper.getWritableDatabase();
		int count = db.delete(uri.getLastPathSegment(), selection, selectionArgs);
		getContext().getContentResolver().notifyChange(uri, null);
		return count;
	}

	@Override
	public String getType(Uri uri) {
		return uri.getLastPathSegment();
	}
	
}
