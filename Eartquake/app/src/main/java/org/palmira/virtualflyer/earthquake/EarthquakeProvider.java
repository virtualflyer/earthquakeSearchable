package org.palmira.virtualflyer.earthquake;

import android.app.SearchManager;
import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;
import android.database.SQLException;

import java.util.HashMap;

/**
 * Created by matteo on 21/09/15.
 */
public class EarthquakeProvider extends ContentProvider {

    public static final Uri CONTENT_URI = Uri.parse("content://org.palmira.virtualflyer.earthquakeprovider/earthquakes");
    public static final String KEY_ID="_id";
    public static final String KEY_DATE="date";
    public static final String KEY_DETAILS="details";
    public static final String KEY_SUMMARY="summary";
    public static final String KEY_LOCATION_LAT="latitude";
    public static final String KEY_LOCATION_LONG="longitude";
    public static final String KEY_MAGNITUDE="magnitude";
    public static final String KEY_LINK="link";


    EarthquakeDatabaseHelper dbHelper;

    @Override
    public boolean onCreate(){
        Context context = getContext();
        dbHelper=new EarthquakeDatabaseHelper(context, EarthquakeDatabaseHelper.DATABASE_NAME,null,EarthquakeDatabaseHelper.DATABASE_VERSION);
        return true;
    }


    private static final int QUAKES=1;
    private static final int QUAKE_ID=2;
    private static final int SEARCH=3;

    private static final UriMatcher uriMatcher;

    static{
        uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        uriMatcher.addURI("org.palmira.virtualflyer.earthquakeprovider","earthquakes",QUAKES);
        uriMatcher.addURI("org.palmira.virtualflyer.earthquakeprovider","earthquakes/#",QUAKE_ID);
        uriMatcher.addURI("org.palmira.virtualflyer.earthquakeprovider",SearchManager.SUGGEST_URI_PATH_QUERY,SEARCH);
        uriMatcher.addURI("org.palmira.virtualflyer.earthquakeprovider",SearchManager.SUGGEST_URI_PATH_QUERY+"/*",SEARCH);
        uriMatcher.addURI("org.palmira.virtualflyer.earthquakeprovider",SearchManager.SUGGEST_URI_PATH_SHORTCUT,SEARCH);
        uriMatcher.addURI("org.palmira.virtualflyer.earthquakeprovider",SearchManager.SUGGEST_URI_PATH_SHORTCUT+"/*",SEARCH);
    }

    @Override
    public String getType(Uri uri){
        switch (uriMatcher.match(uri)){
            case QUAKES: return "vnd.android.cursor.dir/vnd.palmira.virtualflyer.earthquake";
            case QUAKE_ID: return "vnd.android.cursor.item/vnd.palmira.virtualflyer.earthquake";
            case SEARCH: return SearchManager.SUGGEST_MIME_TYPE;
            default: throw new IllegalArgumentException("Unsupported URI: "+uri);

        }
    }

    @Override
    public Cursor query(Uri uri,String[] projection, String selection, String[] selectionArgs,String sort){
        SQLiteDatabase database = dbHelper.getWritableDatabase();
        SQLiteQueryBuilder qb= new SQLiteQueryBuilder();
        qb.setTables(EarthquakeDatabaseHelper.EARTHQUAKE_TABLE);
        switch (uriMatcher.match(uri)){
            case QUAKE_ID   :   qb.appendWhere(KEY_ID+"="+uri.getPathSegments().get(1));
                                break;
            case SEARCH     :   qb.appendWhere(KEY_SUMMARY+" LIKE \"%"+uri.getPathSegments().get(1)+"%\"");
                                qb.setProjectionMap(SEARCH_PROJECTION_MAP);
                                Log.d("EARTH", "Ricerca al via");
                                break;
            default         :   break;
        }
        String orderBy;
        if (TextUtils.isEmpty(sort)){
            orderBy=KEY_DATE;
        } else {
            orderBy=sort;
        }

        Cursor c = qb.query(database, projection, selection, selectionArgs,null,null,orderBy);
        c.setNotificationUri(getContext().getContentResolver(),uri);
        return c;
    }

    @Override
    public Uri insert (Uri _uri,ContentValues _initialValues) {
        SQLiteDatabase database = dbHelper.getWritableDatabase();
        long rowID= database.insert(EarthquakeDatabaseHelper.EARTHQUAKE_TABLE,"quake",_initialValues);
        if (rowID>0){
            Uri uri= ContentUris.withAppendedId(CONTENT_URI,rowID);
            getContext().getContentResolver().notifyChange(uri,null);
            return uri;
        }
        throw new SQLException("Failed to insert row into "+ _uri);
    }

    @Override
    public int delete (Uri uri, String where, String[] whereArgs){
        SQLiteDatabase database= dbHelper.getWritableDatabase();

        int count;
        switch (uriMatcher.match(uri)){
            case QUAKES:
                count=database.delete(EarthquakeDatabaseHelper.EARTHQUAKE_TABLE,where,whereArgs);
                break;
            case QUAKE_ID:
                String segment = uri.getPathSegments().get(1);
                count = database.delete(EarthquakeDatabaseHelper.EARTHQUAKE_TABLE,KEY_ID+"="+segment+(!TextUtils.isEmpty(where)?" AND ("+where+')' :""),whereArgs);
                break;
            default: throw new IllegalArgumentException("Unsupported Uri: "+uri);
        }
        getContext().getContentResolver().notifyChange(uri,null);
        return count;
    }

    @Override
    public int update (Uri uri,ContentValues values, String where, String[] wArgs){
        SQLiteDatabase database = dbHelper.getWritableDatabase();
        int count;
        switch (uriMatcher.match(uri)){
            case QUAKES:
                count = database.update(EarthquakeDatabaseHelper.EARTHQUAKE_TABLE,values,where,wArgs);
                break;
            case QUAKE_ID:
                String segment = uri.getPathSegments().get(1);
                count=database.update(EarthquakeDatabaseHelper.EARTHQUAKE_TABLE,values,KEY_ID+"="+segment+(TextUtils.isEmpty(where)?" AND ("+where+')': ""),wArgs);
                break;
            default: throw new IllegalArgumentException("Unknown URI "+uri);
        }
        getContext().getContentResolver().notifyChange(uri,null);
        return count;
    }


    private static class EarthquakeDatabaseHelper extends SQLiteOpenHelper {
        private static final String TAG="EarthquakeProvider";
        private static final String DATABASE_NAME = "earthquakes.db";
        private static final int DATABASE_VERSION=1;
        private static final String EARTHQUAKE_TABLE="earthquakes";

        private static final String DATABASE_CREATE=
                "create table "+EARTHQUAKE_TABLE+" ("
                +KEY_ID+" integer primary key autoincrement, "
                +KEY_DATE+" INTEGER, "
                +KEY_DETAILS+" TEXT, "
                +KEY_SUMMARY+" TEXT, "
                +KEY_LOCATION_LAT+" FLOAT, "
                +KEY_LOCATION_LONG+" FLOAT, "
                +KEY_MAGNITUDE+" FLOAT, "
                +KEY_LINK+" TEXT);";
        private SQLiteDatabase earthquakeDB;
        public EarthquakeDatabaseHelper (Context context, String name, SQLiteDatabase.CursorFactory factory,int version){
            super (context, name, factory, version);
        }

        @Override
        public void onCreate(SQLiteDatabase db){
            db.execSQL(DATABASE_CREATE);

        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion){
            Log.w(TAG, "Upgrading database from version " + oldVersion + " to " + newVersion + ", which will destory all data");
            db.execSQL("DROP TABLE IF EXISTS " + EARTHQUAKE_TABLE);
            onCreate(db);
        }
    }

    private static final HashMap<String,String> SEARCH_PROJECTION_MAP;
    static {
        SEARCH_PROJECTION_MAP=new HashMap<String, String>();
        SEARCH_PROJECTION_MAP.put(SearchManager.SUGGEST_COLUMN_TEXT_1,KEY_SUMMARY+" AS "+SearchManager.SUGGEST_COLUMN_TEXT_1);
        SEARCH_PROJECTION_MAP.put("_id",KEY_ID+" AS "+"_id");
    }

}
