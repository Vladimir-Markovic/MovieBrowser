/*
 * Copyright (C) 2017 Vladimir Markovic
 */

package com.humaneapps.popularmovies;

import android.content.ContentValues;

import com.humaneapps.popularmovies.data.MoviesContract;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.DateFormatSymbols;

/**
 * Constructed by extracting movie details from passed in JSON string or JSoNObject, Movie object
 * holds formatted details for the movie which can be accessed via its getters.
 *
 * NOTE: I have looked into Parcelable and it seams really useful (thank you for the suggestion in stage 1).
 * I will try to learn how to implement it for my final project since I was able to use Json string
 * for preserving state here.
 */
public class Movie {

    private String mDataJson;
    private int mTmdbId;
    private String mPoster;
    private String mBackdrop;
    private String mTitle;
    private String mReleaseDate;
    private String mOverview;
    private double mRating;
    private int mVoteCount;
    private double mPopularity;
    private String mReviewsJson;
    private final ContentValues mValues = new ContentValues();


    public Movie(String jsonString) {
        JSONObject movieJsonObject;
        try {
            movieJsonObject = new JSONObject(jsonString);
            populate(movieJsonObject);
        } catch (JSONException e) {
            // No need to do anything - just skips creating Movie object on exception.
        }
    }


    Movie(JSONObject movieJsonObject) { populate(movieJsonObject); }


    private void populate(JSONObject movieJsonObject) {
        // These are the names of the JSON objects that need to be extracted.
        final String TMDB_ID = "id";
        final String TMDB_POSTER = "poster_path";
        final String TMDB_BACKDROP = "backdrop_path";
        final String TMDB_TITLE = "title";
        final String TMDB_RELEASE_DATE = "release_date";
        final String TMDB_OVERVIEW = "overview";
        final String TMDB_RATING = "vote_average";
        final String TMDB_VOTES = "vote_count";
        final String TMDB_POPULARITY = "popularity";

        mDataJson = movieJsonObject.toString();

        try {
            mTmdbId = movieJsonObject.getInt(TMDB_ID);
            // If unable to extract data for this movie, skip it.
        } catch (JSONException e) { return; }

        try {
            mTitle = movieJsonObject.getString(TMDB_TITLE);
            mPoster = movieJsonObject.getString(TMDB_POSTER);
            if ("/".equals(mPoster.substring(0, 1))) {mPoster = mPoster.substring(1);}
        } catch (JSONException e) { return; }

        try {
            mBackdrop = movieJsonObject.getString(TMDB_BACKDROP);
            if ("/".equals(mBackdrop.substring(0, 1))) {
                mBackdrop = mBackdrop.substring(1);
            }
            // If unable to put non-essential columns, put empty string inside instead.
        } catch (JSONException e) {
            mBackdrop = null;
        }

        try {
            mRating = movieJsonObject.getDouble(TMDB_RATING);
        } catch (JSONException e) {
            mRating = 0d;
        }

        try {
            mReleaseDate = movieJsonObject.getString(TMDB_RELEASE_DATE);
        } catch (JSONException e) {
            mReleaseDate = "";
        }

        try {
            mOverview = movieJsonObject.getString(TMDB_OVERVIEW);
        } catch (JSONException e) {
            mOverview = "";
        }
        try {
            mVoteCount = movieJsonObject.getInt(TMDB_VOTES);
        } catch (JSONException e) {
            mVoteCount = 0;
        }
        try {
            mPopularity = movieJsonObject.getDouble(TMDB_POPULARITY);
        } catch (JSONException e) {
            mPopularity = 0d;
        }

        // In same order as table columns in MovieDbHelper.
        mValues.put(MoviesContract.COLUMN_TMDB_ID, mTmdbId);
        mValues.put(MoviesContract.COLUMN_DATA_JSON, mDataJson);
    }


    String getDataJson() {
        return mDataJson;
    }


    int getTmdbId() {
        return mTmdbId;
    }


    public String getPoster() { return mPoster; }


    public String getBackdrop() { return mBackdrop; }


    String getTitle() { return mTitle; }


    String getOverview() {
        return mOverview;
    }


    double getRating() { return mRating; }


    int getVotes() { return mVoteCount; }


    @SuppressWarnings("unused")
    double getPopularity() { return mPopularity; }


    ContentValues getContentValues() { return mValues; }


    /**
     * Format and return release date
     *
     * @return release date - format as "21 May 2016"
     */
    String getReleaseDate() {

        String year;
        int iMonth;
        String sMonth;
        String day;

        try {
            year = mReleaseDate.substring(0, 4);
        } catch (NullPointerException e) {
            return null;
        } catch (IndexOutOfBoundsException e) {
            return null;
        }

        try {
            iMonth = Integer.parseInt(mReleaseDate.substring(5, 7));
            day = mReleaseDate.substring(8);
            sMonth = new DateFormatSymbols().getMonths()[iMonth - 1];
        } catch (IndexOutOfBoundsException e) {
            return year;
        } catch (NumberFormatException e) {
            return year;
        }

        // Example: '21 May 2015'
        return day + " " + sMonth + " " + year;
    }


    String getReviewsJson() { return mReviewsJson; }


    void setReviewsJson(String reviewsJsonString) {
        mReviewsJson = reviewsJsonString;
        mValues.put(MoviesContract.COLUMN_REVIEWS_JSON, reviewsJsonString);
    }


} // End class Movie
