/*
 * Copyright (C) 2017 Vladimir Markovic
 */

package com.humaneapps.popularmovies;

import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.annotation.Nullable;

import com.humaneapps.popularmovies.data.MoviesContract;

/**
 * Used to:
 *  1) Insert movie to favourite_movies table in the database.
 *  2) Update favourite movie with reviews json string
 *  3) Delete favourite movie from the favourite_movies table.
 */

class TaskFavourite extends AsyncTask<Boolean, Void, Boolean> {

    // Used to get content resolver.
    private final Context mContext;
    // Will contain passed in info for inserting and updating.
    private final ContentValues mContentValues;
    // Used as selection argument for deleting and updating.
    private final int mTmdbId;

    // Async response for returning the result.
    private AsyncResponseFavourite mAsyncResponse = null;

    // 'Return' Boolean isFavourite: true  - favourite (after insert and update,
    //                               false - not favourite (after delete),
    //                               null  - on error.
    interface AsyncResponseFavourite {
        void processFinish(Boolean isFavourite);
    }


    // Constructor
    TaskFavourite(AsyncResponseFavourite asyncResponse, Context context, ContentValues values,
                  int tmdbId) {
        mAsyncResponse = asyncResponse;
        mContext = context;
        mContentValues = values;
        mTmdbId = tmdbId;
    }


    /**
     *
     * @param params - if passed in boolean param[0] tells if the movie was in favourites or not. If
     *               it was delete it. If it wasn't insert it. If no params were passed in, this
     *               option is used to update reviews_json column for movie already in favourites.
     * @return - boolean showing if the movie is in favourites after task completion. Return null if
     *           inserting, updating or deleting failed.
     */
    @Override
    protected Boolean doInBackground(Boolean... params) {

        // Make uri and arguments for content resolver operations.
        Uri tableUri = MoviesContract.getTableUri(MoviesContract.TableMovies.TABLE_NAME);
        String selection = MoviesContract.COLUMN_TMDB_ID + " = ?";
        String[] selectionArg = new String[] { mTmdbId + "" };
        // For returning result.
        Boolean becameFavourite = null;

        // param[0] is a trigger to what action the task will perform. When passed it represents
        // whether the movie is already in favourites or not, and if yes it will be removed so
        // delete action is performed; if not, it will be inserted. If it is omitted this triggers
        // update function expecting that the movie is already in favourites.

        if (params.length > 0) {

            boolean isFavourite = params[0];

            if (isFavourite) {
                // Delete
                int rows = mContext.getContentResolver().delete(tableUri, selection, selectionArg);
                if (rows > 0) { becameFavourite = false; }
            } else {
                // Insert
                Uri returnUri = mContext.getContentResolver().insert(tableUri, mContentValues);
                if (returnUri != null) { becameFavourite = true; }
            }
        } else {
            // Update
            int rows = mContext.getContentResolver().update(tableUri, mContentValues, selection, selectionArg);
            if (rows > 0) { becameFavourite = true; }
        }

        return becameFavourite;

    }

    // 'Return' Boolean isFavourite: true  - favourite (after insert and update,
    //                               false - not favourite (after delete),
    //                               null  - on error.
    @Override
    protected void onPostExecute(@Nullable Boolean isFavourite) {
        mAsyncResponse.processFinish(isFavourite);
    } // End onPostExecute


} // End class TaskFavourite (AsyncTask)
