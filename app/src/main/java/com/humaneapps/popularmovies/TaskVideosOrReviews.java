/*
 * Copyright (C) 2017 Vladimir Markovic
 */

package com.humaneapps.popularmovies;

import android.os.AsyncTask;

import com.squareup.okhttp.OkHttpClient;

/**
 * Uses passed in identifier to know whether to fetch videos or reviews JSON string.
 */

class TaskVideosOrReviews extends AsyncTask<String, Void, String[]> {

    private final OkHttpClient mOkHttpClient = new OkHttpClient();

    // Async response for returning the result.
    private AsyncResponseVideosOrReviews mAsyncResponse = null;

    // 'Return' size 2 string array: [0] - identifier telling if fetched videos or reviews;
    //                               [1] - returning JSON string.
    interface AsyncResponseVideosOrReviews {
        void processFinish(String[] strArReturn);
    }


    // Constructor
    TaskVideosOrReviews(AsyncResponseVideosOrReviews asyncResponse) {
        mAsyncResponse = asyncResponse;
    }


    @Override
    protected String[] doInBackground(String... params) {
        // For knowing whether to fetch videos or reviews.
        String identifier = params[0];
        // Which movie to fetch for.
        String tmdbId = params[1];
        // Returning JSON string.
        String jsonArrayString;

        if (identifier.equals(Util.IDENTIFIER_VIDEOS)) {
            jsonArrayString = Util.fetchVideosJsonString(tmdbId, mOkHttpClient);
        } else {
            jsonArrayString = Util.fetchReviewsJsonString(tmdbId, mOkHttpClient);
        }

        if (jsonArrayString == null) { return null; }

        return new String[] { identifier, jsonArrayString };
    }


    // 'Return' size 2 string array: [0] - identifier telling if fetched videos or reviews;
    //                               [1] - returning JSON string.
    @Override
    protected void onPostExecute(String[] strArReturn) {
        mAsyncResponse.processFinish(strArReturn);
    }


} // End class TaskFavourite (AsyncTask)
