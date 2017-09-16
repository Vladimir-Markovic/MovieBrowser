/*
 * Copyright (C) 2017 Vladimir Markovic
 */

package com.humaneapps.popularmovies;

import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.preference.PreferenceManager;
import android.support.v7.widget.Toolbar;

import com.humaneapps.popularmovies.data.MoviesContract;

/**
 * Details activity contains DetailsFragment - only used in phones.
 */
public class DetailsActivity extends AppCompatActivity {


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_details);

        PopularMovies application = (PopularMovies) getApplication();
        // Get and store orientation.
        application.setLandscape(getResources().getConfiguration().orientation
                == Configuration.ORIENTATION_LANDSCAPE);

        // Set mode for detecting split screen with both main and details fragments (in tablet landscape)
        if (application.isTwoPane()) { finish(); }

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);

        Intent intent = getIntent();
        String jsonString = "";

        if (intent.hasExtra(MoviesContract.COLUMN_DATA_JSON)) {
            jsonString = intent.getStringExtra(MoviesContract.COLUMN_DATA_JSON);
        }
        Bundle bundle = new Bundle();
        bundle.putString(MoviesContract.COLUMN_DATA_JSON, jsonString);
        if (savedInstanceState == null) {
            showFragment(DetailsFragment.newInstance(), getString(R.string.title_details), bundle);
        }

    } // End onCreate


    /**
     * For showing Details Fragment.
     *
     * @param fragment       - instance of the fragment to be shown.
     * @param title          - title of the fragment to be shown - added to transaction and used as tag.
     */
    public void showFragment(Fragment fragment, String title, Bundle bundle) {
        // If fragment is already created don't add another on top. Uses title as tag.

        fragment.setArguments(bundle);

        if (getSupportFragmentManager().findFragmentByTag(title) == null) {
            // Create new fragment transaction
            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            // Add this fragment to fragment_container.
            transaction.add(R.id.fragmentContainer, fragment, title);
            // Commit the transaction
            transaction.commit();
        }
    } // End showFragment


} // End DetailsActivity class

