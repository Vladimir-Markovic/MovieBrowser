/*
 * Copyright (C) 2017 Vladimir Markovic
 */

package com.humaneapps.popularmovies.service;

import android.app.IntentService;
import android.content.Intent;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.humaneapps.popularmovies.Util;
import com.humaneapps.popularmovies.data.MoviesContract;
import com.squareup.okhttp.OkHttpClient;

/**
 * Fetches movie data using http url connection to api.themoviedb.org, for specified page number
 * and group ('popular' or 'top_rated' and return it in JSON string format via broadcast.
 */
public class ServiceFetch extends IntentService {

    // For not allowing another start until running one finishes.
    private boolean mFirstRun = true;

    private final OkHttpClient mOkHttpClient = new OkHttpClient();


    public ServiceFetch() { super("ServiceFetch"); }


    @Override
    protected void onHandleIntent(Intent intent) {

        // will hold the JSON string to return via broadcast.
        String jsonString = null;

        // only allow service to run the code once
        if (mFirstRun) {
            // disable other runs.
            mFirstRun = false;
            // get passed in page number and group index to get the group from group array in Util.
            int pageNumber = intent.getIntExtra(Util.PARAM_PAGE, 1);
            int groupIndex = intent.getIntExtra(Util.PARAM_SPINNER_INDEX, 0);
            // get the jsonString to broadcast.
            jsonString = fetchMovieData(pageNumber, groupIndex);
        } // End if first run.

        // Send broadcast regardless of the run attempt number. Null will indicate failure.
        sendBroadcast(jsonString);

    } // End onHandleIntent.


    private String fetchMovieData(int pageNumber, int groupIndex) {

        // Used to compose data 'fetching' url.
        final String PAGE_PARAM = "page";

        // get group ('popular' or 'top_rated') from array in Util using passed in group index.
        String group = Util.GROUPS[groupIndex];
        // Construct the URL for the themoviedb.com 'popular' or 'top_rated' query.
        String urlString = Util.makeUrlString(group, PAGE_PARAM, pageNumber + "");
        // Get Json String via http url connection using above url.
        return Util.fetchJsonString(urlString, mOkHttpClient);

    }


    // Send the resulting JSON string back to MainActivity. Null indicates failure.
    private void sendBroadcast(String result) {
        Intent intent = new Intent(Util.PARAM_SERVICE_BROADCAST);
        intent.putExtra(MoviesContract.COLUMN_DATA_JSON, result);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

} // End class ServiceFetch.
