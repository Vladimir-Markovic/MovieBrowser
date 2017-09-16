/*
 * Copyright (C) 2017 Vladimir Markovic
 */

package com.humaneapps.popularmovies.data;

import android.content.ContentProvider;
import android.content.ContentProviderOperation;
import android.content.ContentProviderResult;
import android.content.ContentValues;
import android.content.Context;
import android.content.OperationApplicationException;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.humaneapps.popularmovies.Util;

import java.util.ArrayList;

/**
 * Communicates with database 'movies.db'.
 */
public class MovieProvider extends ContentProvider {

    private MovieDbHelper mMovieDbHelper;
    private Context mContext;


    @Override
    public boolean onCreate() {
        mContext = getContext();
        mMovieDbHelper = new MovieDbHelper(mContext);
        return true;
    }


    @Override
    public String getType(@NonNull Uri uri) {
        String tableName = uri.getPathSegments().get(0);
        String id;
        int pathSegmentCount = uri.getPathSegments().size();
        if (pathSegmentCount > 1) {
            id = uri.getPathSegments().get(1);
            if (TextUtils.isDigitsOnly(id)) {
                return MoviesContract.getTypeItem(tableName);
            } else {
                id = uri.getLastPathSegment();
                if (TextUtils.isDigitsOnly(id)) {
                    return MoviesContract.getTypeItem(tableName);
                } else {
                    return MoviesContract.getTypeDir(tableName);
                }
            }
        } else { return MoviesContract.getTypeDir(tableName); }
    }


    @Nullable
    @Override
    public Cursor query(@NonNull Uri uri, String[] projection, String selection, String[] selectionArgs,
                        String sortOrder) {
        Cursor retCursor;
        // if last path segment equals DO_NOT_NOTIFY_CHANGE constant it's a signal not to notify change
        String notify = uri.getLastPathSegment();
        String tableName = uri.getPathSegments().get(0);
        retCursor = mMovieDbHelper.getReadableDatabase().query(
                tableName, projection, selection, selectionArgs, null, null, sortOrder);
        if (!notify.equals(Util.DO_NOT_NOTIFY_CHANGE)) {
            retCursor.setNotificationUri(mContext.getContentResolver(), uri);
        }
        return retCursor;
    }


    @Override
    public int update(@NonNull Uri uri, ContentValues contentValues, String selection, String[] selectionArgs) {
        final SQLiteDatabase db = mMovieDbHelper.getWritableDatabase();
        // if last path segment equals DO_NOT_NOTIFY_CHANGE constant it's a signal not to notify change
        String notify = uri.getLastPathSegment();
        String tableName = uri.getPathSegments().get(0);
        int noOfRowsUpdated = db.update(tableName, contentValues, selection, selectionArgs);
        if (noOfRowsUpdated > 0 && !notify.equals(Util.DO_NOT_NOTIFY_CHANGE)) {
            mContext.getContentResolver().notifyChange(uri, null);
        }
        return noOfRowsUpdated;
    }


    @Nullable
    @Override
    public Uri insert(@NonNull Uri uri, ContentValues contentValues) {
        final SQLiteDatabase db = mMovieDbHelper.getWritableDatabase();
        Uri returnUri = null;
        // if last path segment equals DO_NOT_NOTIFY_CHANGE constant it's a signal not to notify change
        String notify = uri.getLastPathSegment();
        String tableName = uri.getPathSegments().get(0);
        long _id = db.insert(tableName, null, contentValues);
        if (_id != -1) {
            returnUri = MoviesContract.buildUriWithId(_id, tableName);
        }
        if (!notify.equals(Util.DO_NOT_NOTIFY_CHANGE)) {
            mContext.getContentResolver().notifyChange(uri, null);
        }
        return returnUri;
    }


    @Override
    public int bulkInsert(@NonNull Uri uri, @NonNull ContentValues[] values) {

        final SQLiteDatabase db = mMovieDbHelper.getWritableDatabase();
        // if last path segment equals DO_NOT_NOTIFY_CHANGE constant it's a signal not to notify change
        String notify = uri.getLastPathSegment();
        String tableName = uri.getPathSegments().get(0);

        db.beginTransaction();
        int returnCount = 0;
        for (ContentValues value : values) {
            long _id = db.insert(tableName, null, value);
            if (_id != -1) { returnCount++; }
        }
        try {
            db.setTransactionSuccessful();
        } catch (IllegalStateException e) {
            throw new IllegalStateException(e.getMessage());
        } finally {
            db.endTransaction();
        }
        if (!notify.equals(Util.DO_NOT_NOTIFY_CHANGE)) {
            mContext.getContentResolver().notifyChange(uri, null);
        }
        return returnCount;

    } // End bulkInsert.


    @Override
    public int delete(@NonNull Uri uri, String selection, String[] selectionArgs) {
        final SQLiteDatabase db = mMovieDbHelper.getWritableDatabase();
        int noOfRowsDeleted;
        // if last path segment equals DO_NOT_NOTIFY_CHANGE constant it's a signal not to notify change
        String notify = uri.getLastPathSegment();
        String tableName = uri.getPathSegments().get(0);
        if (selection == null) {
            selection = "1";
        }
        noOfRowsDeleted = db.delete(tableName, selection, selectionArgs);

        if (noOfRowsDeleted > 0 && !notify.equals(Util.DO_NOT_NOTIFY_CHANGE)) {
            mContext.getContentResolver().notifyChange(uri, null);
        }
        return noOfRowsDeleted;
    }


    @NonNull
    @Override
    public ContentProviderResult[] applyBatch(@NonNull ArrayList<ContentProviderOperation> operations)
            throws OperationApplicationException {

        final SQLiteDatabase db = mMovieDbHelper.getWritableDatabase();
        db.beginTransaction();
        try {
            final int numOperations = operations.size();
            final ContentProviderResult[] results = new ContentProviderResult[numOperations];
            for (int i = 0; i < numOperations; i++) {
                results[i] = operations.get(i).apply(this, results, i);
            }
            db.setTransactionSuccessful();
            return results;
        } finally {
            db.endTransaction();
        }
    }


} // End class ContentProvider
