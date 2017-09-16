/*
 * Copyright (C) 2017 Vladimir Markovic
 */

package com.humaneapps.popularmovies.data;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.net.Uri;
import android.provider.BaseColumns;

/**
 * Database constants and methods for table, column names and uris.
 */
public class MoviesContract {


    // Content scheme
    private static final String CONTENT_SCHEME = "content";
    // The unique name for the content provider
    private static final String CONTENT_AUTHORITY = "com.humaneapps.popularmovies";

    // Table columns:;
    public static final String COLUMN_TMDB_ID = "tmdb_id";
    public static final String COLUMN_DATA_JSON = "data_json";
    public static final String COLUMN_REVIEWS_JSON = "reviews_json";


    public static final class TableMovies implements BaseColumns {
        public static final String TABLE_NAME = "favourite_movies";
    }


    static String getTypeItem(String tableName) {
        return ContentResolver.CURSOR_ITEM_BASE_TYPE + "/" + CONTENT_AUTHORITY + "/" + tableName;
    }


    static String getTypeDir(String tableName) {
        return ContentResolver.CURSOR_DIR_BASE_TYPE + "/" + CONTENT_AUTHORITY + "/" + tableName;
    }


    public static Uri getTableUri(String tableName) {
        return new Uri.Builder()
                .scheme(CONTENT_SCHEME)
                .authority(CONTENT_AUTHORITY)
                .appendPath(tableName)
                .build();
    }


    static Uri buildUriWithId(long id, String tableName) {
        return ContentUris.withAppendedId(getTableUri(tableName), id);
    }

} // End MovieContract class
