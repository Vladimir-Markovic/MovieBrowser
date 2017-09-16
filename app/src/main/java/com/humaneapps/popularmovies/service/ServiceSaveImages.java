/*
 * Copyright (C) 2017 Vladimir Markovic
 */

package com.humaneapps.popularmovies.service;

import android.app.IntentService;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import com.humaneapps.popularmovies.Util;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.OkUrlFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Fetch and save images for a favourite movie using urls formed from passed poster and backdrop
 * names and widths.
 */
public class ServiceSaveImages extends IntentService {


    private final OkHttpClient mOkHttpClient = new OkHttpClient();


    public ServiceSaveImages() { super("ServiceSaveImages"); }


    @Override
    protected void onHandleIntent(Intent intent) {

        // Get passed poster and backdrop names and widths.
        String posterName = intent.getStringExtra(Util.PARAM_POSTER_NAME);
        String backdropName = intent.getStringExtra(Util.PARAM_BACKDROP_NAME);
        int posterWidth = intent.getIntExtra(Util.PARAM_POSTER_WIDTH,
                Util.POSTER_WIDTHS[Util.DEFAULT_POSTER_INDEX]);
        int backdropWidth = intent.getIntExtra(Util.PARAM_BACKDROP_WIDTH,
                Util.BACKDROP_WIDTHS[Util.DEFAULT_BACKDROP_INDEX]);

        // Fetch and save poster using url formed from passed poster name and width.
        fetchAndSaveImage(Util.getImageUrl(posterName, posterWidth));
        // Fetch and save backdrop using url formed from passed backdrop name and width.
        fetchAndSaveImage(Util.getImageUrl(backdropName, backdropWidth));

    } // End onHandleIntent.


    private boolean fetchAndSaveImage(URL url) {

        if (url == null) {return false;}

        // If image name has trailing backslash, remove it
        String urlString = url.toString();
        String fileName = urlString.substring(urlString.lastIndexOf("/") + 1, urlString.length());

        // Create access to directory for saving files or make it if it doesn't exist.
        File fileDir = new File(Util.getInternalFilePath(getApplicationContext()));
        if (!fileDir.exists() && !fileDir.mkdirs()) {
            return false;
        }
        // Create access to image file.
        File imgFile = new File(fileDir, fileName);
        // If image was previously saved, don't fetch and save again.
        if (imgFile.exists()) { return false; }

        try {
            // Open url connection
            HttpURLConnection urlConnection = new OkUrlFactory(mOkHttpClient).open(url);
            urlConnection.setRequestMethod("GET");
            urlConnection.connect();

            // Open input stream and use it for reading in the bitmap.
            InputStream inputStream = urlConnection.getInputStream();
            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);

            // Save the image file
            FileOutputStream fos = new FileOutputStream(imgFile);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos);
            fos.close();

            urlConnection.disconnect();

            return true;

        } catch (IOException | NullPointerException e) {return false;}

    } // End fetchAndSaveImage method.


} // End class ServiceSaveImages.
