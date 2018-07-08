package com.mendhak.gpslogger;

import java.util.ArrayList;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;
import android.util.Log;

public class GPSDataContentProvider extends ContentProvider {

	private static final String TAG = "GPSDataContentProvider";

    public static final String DATABASE_NAME = "semgpsdata.db";
    private static final int DATABASE_VERSION = 1;
    public static final String POINT_TABLE_NAME = "gpspoints";

    public static final String AUTHORITY = "com.semih";
    public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/gpspoint");
    
    
    
    private static final UriMatcher uriMatcher ;
    
    static {
        uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        uriMatcher.addURI(AUTHORITY, "gpspoint", DATABASE_VERSION);
    }
    
    /**
     * This class helps open, create, and upgrade the database file.
     */
    public static class DatabaseHelper extends SQLiteOpenHelper {

        DatabaseHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
         try {
         Log.i(TAG, "Creating table " + POINT_TABLE_NAME);    
         db.execSQL("CREATE TABLE " + POINT_TABLE_NAME + " ("
        		 + GPSData.GPSPoint.KEY_ID + " INTEGER PRIMARY KEY,"
                 + GPSData.GPSPoint.SRG_ID + " REAL,"
        		 + GPSData.GPSPoint.GUN + " REAL,"
        		 + GPSData.GPSPoint.LATITUDE + " REAL,"
                 + GPSData.GPSPoint.LONGITUDE + " REAL,"
                 + GPSData.GPSPoint.ALTITUDE + " REAL,"
                 + GPSData.GPSPoint.PRESSURE + " DOUBLE,"
                 + GPSData.GPSPoint.TPRESSURE + " REAL,"
                 + GPSData.GPSPoint.BHEIGHT + " REAL,"
                 + GPSData.GPSPoint.TEMPERATURE + " REAL,"
                 + GPSData.GPSPoint.HUMIDITY + " REAL,"
                 + GPSData.GPSPoint.FLOOR + " REAL,"
                 + GPSData.GPSPoint.KATDEGIS + " REAL,"
                 + GPSData.GPSPoint.PROVIDER + " REAL,"
                 + GPSData.GPSPoint.SAT + " REAL,"
                 + GPSData.GPSPoint.USER + " REAL,"
                 + GPSData.GPSPoint.PHONE + " REAL,"
                 + GPSData.GPSPoint.SAY + " REAL,"
                 + GPSData.GPSPoint.GIRKNTRL + " REAL,"
                 + GPSData.GPSPoint.DEGKNTRL + " REAL,"
                 + GPSData.GPSPoint.INTERVAL + " REAL,"
                 + GPSData.GPSPoint.KatYuk + " REAL,"
                 + GPSData.GPSPoint.MSF + " REAL,"
                 + GPSData.GPSPoint.GX + " REAL,"
                 + GPSData.GPSPoint.GY + " REAL,"
                 + GPSData.GPSPoint.GZ + " REAL,"
                 + GPSData.GPSPoint.C1 + " REAL,"
                 + GPSData.GPSPoint.C2 + " REAL,"
                 + GPSData.GPSPoint.K1 + " REAL,"
                 + GPSData.GPSPoint.K2 + " REAL,"
                 + GPSData.GPSPoint.K3 + " REAL,"
                 + GPSData.GPSPoint.K4 + " REAL,"
                 + GPSData.GPSPoint.K5 + " REAL,"
                 + GPSData.GPSPoint.K6 + " REAL,"
                 + GPSData.GPSPoint.K7 + " REAL,"
                 + GPSData.GPSPoint.K8 + " REAL,"
                 + GPSData.GPSPoint.K9 + " REAL,"
                 + GPSData.GPSPoint.K10 + " REAL,"
                 + GPSData.GPSPoint.K11 + " REAL,"
                 + GPSData.GPSPoint.K12 + " REAL,"
                 + GPSData.GPSPoint.K13 + " REAL,"
                 + GPSData.GPSPoint.K14 + " REAL,"
                 + GPSData.GPSPoint.K15 + " REAL,"
                 + GPSData.GPSPoint.K16 + " REAL,"
                 + GPSData.GPSPoint.K17 + " REAL,"
                 + GPSData.GPSPoint.K18 + " REAL,"
                 + GPSData.GPSPoint.K19 + " REAL,"
                 + GPSData.GPSPoint.K20 + " REAL,"
                 + GPSData.GPSPoint.K21 + " REAL,"
                 + GPSData.GPSPoint.K22 + " REAL,"
                 + GPSData.GPSPoint.K23 + " REAL,"
                 + GPSData.GPSPoint.K24 + " REAL,"
                 + GPSData.GPSPoint.K25 + " REAL,"
                 + GPSData.GPSPoint.K26 + " REAL,"
                 + GPSData.GPSPoint.K27 + " REAL,"
                 + GPSData.GPSPoint.K28 + " REAL,"
                 + GPSData.GPSPoint.K29 + " REAL,"
                 + GPSData.GPSPoint.K30 + " REAL,"
                 + GPSData.GPSPoint.K31 + " REAL,"
                 + GPSData.GPSPoint.K32 + " REAL,"
                 + GPSData.GPSPoint.K33 + " REAL,"
                 + GPSData.GPSPoint.K34 + " REAL,"
                 + GPSData.GPSPoint.K35 + " REAL,"
                 + GPSData.GPSPoint.K36 + " REAL,"
                 + GPSData.GPSPoint.K37 + " REAL,"
                 + GPSData.GPSPoint.K38 + " REAL,"
                 + GPSData.GPSPoint.K39 + " REAL,"
                 + GPSData.GPSPoint.K40 + " REAL,"
                 + GPSData.GPSPoint.K41 + " REAL,"
                 + GPSData.GPSPoint.K42 + " REAL,"
                 + GPSData.GPSPoint.K43 + " REAL,"
                 + GPSData.GPSPoint.K44 + " REAL,"
                 + GPSData.GPSPoint.K45 + " REAL,"
                 + GPSData.GPSPoint.K46 + " REAL,"
                 + GPSData.GPSPoint.TIME + " INTEGER"
                 + ");");
         } catch (SQLiteException e) {
          Log.e(TAG, e.toString());
         }
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            Log.w(TAG, "Upgrading database from version " + oldVersion + " to "
                    + newVersion + ", which will destroy all old data");
            db.execSQL("DROP TABLE IF EXISTS " + POINT_TABLE_NAME);
            onCreate(db);
        }
        
        public int getDbCount() {
            String countQuery = "SELECT  * FROM " + POINT_TABLE_NAME;
    		SQLiteDatabase db = this.getReadableDatabase();
    		Cursor cursor = db.rawQuery(countQuery, null);

    		int count = cursor.getCount();
    		cursor.close();

    		// return count
    		return count;
    		
    	} 
        
        public void deleteDb(long id) {
    		SQLiteDatabase db = this.getWritableDatabase();
    		db.delete(POINT_TABLE_NAME, GPSData.GPSPoint.SRG_ID + " = ?",
    				new String[] { String.valueOf(id) });
    		db.close();
    	}
        
        public void updateDb (String Column, int rowId, String ColumnName, double number){
            SQLiteDatabase db = this.getWritableDatabase();

            ContentValues cv = new ContentValues();
            cv.put(ColumnName, number);
            db.update(POINT_TABLE_NAME, cv, Column + "= ?", new String[] {String.valueOf(rowId) });
            db.close();
        }
        
        public void updateId (String Column, double rowId, String ColumnName, double number){
            SQLiteDatabase db = this.getWritableDatabase();

            ContentValues cv = new ContentValues();
            cv.put(ColumnName, number);
            db.update(POINT_TABLE_NAME, cv, Column + "= ?", new String[] {String.valueOf(rowId) });
            db.close();
        }
        
        
        public void updateDbs (String Column, int rowId, String ColumnName, String strin){
            SQLiteDatabase db = this.getWritableDatabase();

            ContentValues cv = new ContentValues();
            cv.put(ColumnName, strin);
            db.update(POINT_TABLE_NAME, cv, Column + "= ?", new String[] {String.valueOf(rowId) });
            db.close();
        }
       
        public double getPressure (int rowID) {
        	double uname = 0;
        	SQLiteDatabase db = this.getReadableDatabase();
        	
        	String where= GPSData.GPSPoint.SRG_ID + "=" + rowID;
        	String order = GPSData.GPSPoint.PRESSURE;
        	
        	Cursor mCursor= db.query (POINT_TABLE_NAME, null, where, null, null, null,order); 

        	    while (mCursor.moveToNext()) {
        	        uname = mCursor.getDouble(mCursor.getColumnIndex(GPSData.GPSPoint.PRESSURE));

        	    }
        	    
        	    mCursor.close();
        	    
        	    return uname;
        }
        
        public double getGravity (int rowID) {
        	double unameg = 0;
        	SQLiteDatabase db = this.getReadableDatabase();
        	
        	String where= GPSData.GPSPoint.SRG_ID + "=" + rowID;
        	String order = GPSData.GPSPoint.GZ;
        	
        	Cursor mCursor= db.query (POINT_TABLE_NAME, null, where, null, null, null,order); 

        	    while (mCursor.moveToNext()) {
        	        unameg = mCursor.getDouble(mCursor.getColumnIndex(GPSData.GPSPoint.GZ));

        	    }
        	    
        	    mCursor.close();
        	    
        	    return unameg;
        }
        
        
        public double getBHeight (int rowID) {
        	double uname1 = 0;
        	SQLiteDatabase db = this.getReadableDatabase();
        	
        	String where= GPSData.GPSPoint.SRG_ID + "=" + rowID;
        	String order = GPSData.GPSPoint.BHEIGHT;
        	
        	Cursor mCursor= db.query (POINT_TABLE_NAME, null, where, null, null, null,order); 

        	    while (mCursor.moveToNext()) {
        	        uname1 = mCursor.getDouble(mCursor.getColumnIndex(GPSData.GPSPoint.BHEIGHT));

        	    }
        	    mCursor.close();
        	    return uname1;
        	
        }
        
        public double getTemp (int rowID) {
        	double uname2 = 0;
        	SQLiteDatabase db = this.getReadableDatabase();
        	
        	String where= GPSData.GPSPoint.SRG_ID + "=" + rowID;
        	String order = GPSData.GPSPoint.TEMPERATURE;
        	
        	Cursor mCursor= db.query (POINT_TABLE_NAME, null, where, null, null, null,order); 

        	    while (mCursor.moveToNext()) {
        	        uname2 = mCursor.getDouble(mCursor.getColumnIndex(GPSData.GPSPoint.TEMPERATURE));

        	    }
        	    mCursor.close();
        	    return uname2;
        	
        }
        
        public double getHumidity (int rowID) {
        	double uname3 = 0;
        	SQLiteDatabase db = this.getReadableDatabase();
        	
        	String where= GPSData.GPSPoint.SRG_ID + "=" + rowID;
        	String order = GPSData.GPSPoint.HUMIDITY;
        	
        	Cursor mCursor= db.query (POINT_TABLE_NAME, null, where, null, null, null,order); 

        	    while (mCursor.moveToNext()) {
        	        uname3 = mCursor.getDouble(mCursor.getColumnIndex(GPSData.GPSPoint.HUMIDITY));

        	    }
        	    mCursor.close();
        	    return uname3;
        	
        }
        
        public int getEsay (int rowID) {
        	int uname = 0;
        	SQLiteDatabase db = this.getReadableDatabase();
        	
        	String where= GPSData.GPSPoint.SRG_ID + "=" + rowID;
        	String order = GPSData.GPSPoint.ESAY;
        	
        	Cursor mCursor= db.query (POINT_TABLE_NAME, null, where, null, null, null,order); 

        	    while (mCursor.moveToNext()) {
        	        uname = mCursor.getInt(mCursor.getColumnIndex(GPSData.GPSPoint.ESAY));

        	    }
        	    mCursor.close();
        	    return uname;	
        }
        
        public int getSay (int rowID) {
        	int uname = 0;
        	SQLiteDatabase db = this.getReadableDatabase();
        	
        	String where= GPSData.GPSPoint.SRG_ID + "=" + rowID;
        	String order = GPSData.GPSPoint.SAY;
        	
        	Cursor mCursor= db.query (POINT_TABLE_NAME, null, where, null, null, null,order); 

        	    while (mCursor.moveToNext()) {
        	        uname = mCursor.getInt(mCursor.getColumnIndex(GPSData.GPSPoint.SAY));

        	    }
        	    mCursor.close();
        	    return uname;
        	
        }
        
        public int getEKK (int rowID) {
        	int unEKK = 0;
        	SQLiteDatabase db = this.getReadableDatabase();
        	
        	String where= GPSData.GPSPoint.SRG_ID + "=" + rowID;
        	String order = GPSData.GPSPoint.DEGKNTRL;
        	
        	Cursor mCursor= db.query (POINT_TABLE_NAME, null, where, null, null, null,order); 

        	    while (mCursor.moveToNext()) {
        	        unEKK = mCursor.getInt(mCursor.getColumnIndex(GPSData.GPSPoint.DEGKNTRL));

        	    }
        	    mCursor.close();
        	    return unEKK;
        }
        
        public double getTime (int rowID) {
        	double untimes = 0;
        	SQLiteDatabase db = this.getReadableDatabase();
        	
        	String where= GPSData.GPSPoint.SRG_ID + "=" + rowID;
        	String order = GPSData.GPSPoint.GUN;
        	
        	Cursor mCursor= db.query (POINT_TABLE_NAME, null, where, null, null, null,order); 

        	    while (mCursor.moveToNext()) {
        	        untimes = mCursor.getDouble(mCursor.getColumnIndex(GPSData.GPSPoint.GUN));

        	    }
        	    mCursor.close();
        	    return untimes;
        }
        
        public double getlat (int rowID) {
        	double uname4 = 0;
        	SQLiteDatabase db = this.getReadableDatabase();
        	
        	String where= GPSData.GPSPoint.SRG_ID + "=" + rowID;
        	String order = GPSData.GPSPoint.LATITUDE;
        	
        	Cursor mCursor= db.query (POINT_TABLE_NAME, null, where, null, null, null,order); 

        	    while (mCursor.moveToNext()) {
        	        uname4 = mCursor.getDouble(mCursor.getColumnIndex(GPSData.GPSPoint.LATITUDE));

        	    }
        	    mCursor.close();
        	    return uname4;
        }
        
        public double getlong (int rowID) {
        	double uname5 = 0;
        	SQLiteDatabase db = this.getReadableDatabase();
        	
        	String where= GPSData.GPSPoint.SRG_ID + "=" + rowID;
        	String order = GPSData.GPSPoint.LONGITUDE;
        	
        	Cursor mCursor= db.query (POINT_TABLE_NAME, null, where, null, null, null,order); 

        	    while (mCursor.moveToNext()) {
        	        uname5 = mCursor.getDouble(mCursor.getColumnIndex(GPSData.GPSPoint.LONGITUDE));

        	    }
        	    mCursor.close();
        	    return uname5;
        }
        
                 
		        
    }

    private DatabaseHelper mOpenHelper;

 
	public boolean onCreate() {
        mOpenHelper = new DatabaseHelper(getContext());
        return true;
    }


 @Override
 public int delete(Uri arg0, String arg1, String[] arg2) {
  // TODO Auto-generated method stub
  return 0;
 }

 
 @Override
 public String getType(Uri uri) {
  Log.i(TAG, "getting type for " + uri.toString());
  // TODO Auto-generated method stub
  return null;
 }


 @Override
 public Uri insert(Uri uri, ContentValues values) {
  Log.e(TAG, "inserting value " + values.toString());
  
        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
     long rowId = db.insert(POINT_TABLE_NAME, "", values);
         if (rowId > 0) {
             Uri noteUri = ContentUris.withAppendedId(GPSDataContentProvider.CONTENT_URI, rowId);
             getContext().getContentResolver().notifyChange(noteUri, null);
             return noteUri;
         }

         throw new SQLException("Failed to insert row into " + uri);
 }


 @Override
 public Cursor query(Uri uri, String[] projection, String selection,
   String[] selectionArgs, String sortOrder) {
  // TODO Auto-generated method stub
  return null;
 }


 @Override
 public int update(Uri uri, ContentValues values, String selection,
   String[] selectionArgs) {
  // TODO Auto-generated method stub
  return 0;
 }
}
