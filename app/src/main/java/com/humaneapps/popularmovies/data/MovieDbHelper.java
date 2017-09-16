/*
 * Copyright (C) 2017 Vladimir Markovic
 */

package com.humaneapps.popularmovies.data;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Creates and updates database 'movies.db'.
 */
class MovieDbHelper extends SQLiteOpenHelper {

    private static final int DB_VERSION = 1;
    private static final String DB_NAME = "movies.db";


    // constructor
    MovieDbHelper(Context context) {super(context, DB_NAME, null, DB_VERSION);}


    @Override
    public void onCreate(SQLiteDatabase sqLiteDb) {
        final String SQL_CREATE_MOVIES_TABLE = "CREATE TABLE " +
                MoviesContract.TableMovies.TABLE_NAME + " (" +
                MoviesContract.TableMovies._ID + " INTEGER PRIMARY KEY, " +
                MoviesContract.COLUMN_TMDB_ID + " TEXT NOT NULL UNIQUE ON CONFLICT REPLACE, " +
                MoviesContract.COLUMN_DATA_JSON + " TEXT NOT NULL, " +
                MoviesContract.COLUMN_REVIEWS_JSON + " TEXT" +
                ");";

        sqLiteDb.execSQL(SQL_CREATE_MOVIES_TABLE);
    }


    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDb, int oldVersion, int newVersion) {}


} // End MovieDbHelper class.