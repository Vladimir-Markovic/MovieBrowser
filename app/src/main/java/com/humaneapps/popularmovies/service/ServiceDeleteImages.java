/*
 * Copyright (C) 2017 Vladimir Markovic
 */

package com.humaneapps.popularmovies.service;

import android.app.IntentService;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;

import com.humaneapps.popularmovies.R;
import com.humaneapps.popularmovies.Util;
import com.humaneapps.popularmovies.Movie;
import com.humaneapps.popularmovies.data.MoviesContract;

import java.io.File;
import java.util.ArrayList;

/**
 * Depending on the keepFavImages boolean passed through the intent:
 *  - if false is passed, delete all the saved images for favourite movies,
 *  - if true (or nothing - true is default) is passed, perform a function of 'garbage collection':
 *    delete only images for movies which were favourite before but are not any more.
 */
public class ServiceDeleteImages extends IntentService {

    // For not allowing another start until running one finishes.
    private boolean mFirstRun = true;


    public ServiceDeleteImages() { super("ServiceDeleteImages"); }


    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Override
    protected void onHandleIntent(Intent intent) {

        if (mFirstRun) {

            mFirstRun = false;

            // For determining whether to leave images of movies in favourites or to delete all.
            boolean keepFavImages = intent.getBooleanExtra(getString(R.string.pref_save_images_key), true);

            ArrayList<String> posters = new ArrayList<>();
            ArrayList<String> backdrops = new ArrayList<>();
            // Get the lists of image names for all favourite movies (saved in favourite_movies table).
            Uri uri = MoviesContract.getTableUri(MoviesContract.TableMovies.TABLE_NAME);
            Cursor cursor = getContentResolver().query(uri,
                    new String[] { MoviesContract.COLUMN_DATA_JSON }, null, null, null);
            if (cursor != null) {
                while (cursor.moveToNext()) {
                    Movie movie = new Movie(cursor.getString(0));
                    posters.add(movie.getPoster());
                    backdrops.add(movie.getBackdrop());
                }
                cursor.close();
            }
            // Get directory where files are stored.
            File fileDir = new File(Util.getInternalFilePath(this));
            if (fileDir.exists()) {
                // Traverse through all the files in the dir
                File[] files = fileDir.listFiles();
                for (File file : files) {
                    if (keepFavImages) {
                        // If keeping favourite images, skip deletion of images for favourites.
                        // Otherwise delete them all.
                        if (posters.contains(file.getName()) || backdrops.contains(file.getName())) {
                            continue;
                        }
                    }
                    file.delete();
                }
            }

        } // End if first run.

    } // End onHandleIntent.


} // End class ServiceDeleteImages.
