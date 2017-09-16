package com.humaneapps.popularmovies;

import android.app.Application;
import android.content.SharedPreferences;
import android.support.v7.preference.PreferenceManager;
import android.util.DisplayMetrics;

/**
 */

public class PopularMovies extends Application {

    // For storing mode - phone or tablet (twoPane) and orientation.
    private boolean mTwoPane = false;
    // For holding status of the preference whether to save images for favourites.
    private boolean mSaveImages;
    private boolean mIsLandscape;
    // For determining screen width and density.
    private DisplayMetrics mDisplayMetrics;
    // For determining which Image/backdrop width to use from Image/backdrop widths arrays.
    private int mPosterIndex = Util.DEFAULT_POSTER_INDEX;
    private int mBackdropIndex = Util.DEFAULT_BACKDROP_INDEX;
    // For holding spacing between columns in RecyclerView. Used also in ImageAdapter.
    private int mPosterSpacing;
    // For holding fragment widths - which differ in landscape and portrait for tablets.
    private int mFragMainWidth, mFragDetailsWidth;

    // For displaying posters in RecyclerView; has related code (clicks, saving to favourites, etc.).
    public PosterAdapter posterAdapter;


    public boolean isTwoPane() { return mTwoPane; }


    public boolean isLandscape() { return mIsLandscape; }


    public boolean saveImages() { return mSaveImages; }


    int getPosterSpacing() { return mPosterSpacing; }


    int getFragMainWidth() { return mFragMainWidth; }


    int getFragDetailsWidth() { return mFragDetailsWidth; }


    public void setDisplayMetrics(DisplayMetrics displayMetrics) { mDisplayMetrics = displayMetrics; }


    public void setTwoPane(boolean tabletMode) { mTwoPane = tabletMode; }


    public void setLandscape(boolean isLandscape) { mIsLandscape = isLandscape; }


    public void setSavingImages(boolean saveImage) { mSaveImages = saveImage; }


    // For determining Image (grid view column) width, grid view padding, etc.
    float getDisplayDensity() { return mDisplayMetrics.density; }


    int getScreenHeight() { return mDisplayMetrics.heightPixels; }


    public int getPosterWidth() { return Util.POSTER_WIDTHS[mPosterIndex]; }


    public int getBackdropWidth() { return Util.BACKDROP_WIDTHS[mBackdropIndex]; }


    // Height to width poster ratio.
    float getPosterRatio() {
        return (float) Util.POSTER_HEIGHTS[mPosterIndex] / (float) Util.POSTER_WIDTHS[mPosterIndex];
    }


    // Height to width backdrop ratio.
    float getBackdropRatio() {
        return (float) Util.BACKDROP_HEIGHTS[mBackdropIndex] / (float) Util.BACKDROP_WIDTHS[mBackdropIndex];
    }


    void setFragmentWidths() {
        // Get poster spacing from resource. It is called half_poster_spacing because it is applied
        // as poster margin and as RecyclerView padding thus the space between posters will be twice that.
        mPosterSpacing = (int) (getResources().getDimension(R.dimen.half_poster_spacing) * 2);

        // Set mode (twoPane) and fragment widths:
        if (mTwoPane) {
            // Calculate fragment widths:
            // Get MainFragment width.
            if (mIsLandscape) {
                // In tablet landscape set widths from resource - set to be 2/5 Main and 3/5 Details fragment.
                float fmParts = getResources().getInteger(R.integer.parts_fm);
                float fdParts = getResources().getInteger(R.integer.parts_fd);
                mFragMainWidth = (int) ((mDisplayMetrics.widthPixels / (fmParts + fdParts)) * fmParts);
            } else {
                // In tablet portrait RecyclerView has one large (preference) column.
                // Set that size and determine the rest according to it.
                // Get large poster size preference.
                int posterSize = Integer.parseInt(getString(R.string.pref_poster_size_default));
                // Adjust column width by density
                int columnWidth = (int) (posterSize * mDisplayMetrics.density);
                // Determine MainFragment width - one column plus spacing on both sides.
                mFragMainWidth = columnWidth + mPosterSpacing * 2;
            }
            // In tablet, DetailsFragment width is screen width minus MainFragment width.
            mFragDetailsWidth = mDisplayMetrics.widthPixels - mFragMainWidth;

        } else {
            // In phones fragment widths correspond to the screen width.
            mFragMainWidth = mDisplayMetrics.widthPixels;
            mFragDetailsWidth = mFragMainWidth;
        }

    }


    // Determine which poster and backdrop width to fetch depending on the screen size, resolution
    // and user preferences for image quality and poster size.
    // Code will determine what is the index (in image widths array) of image width most suited.
    // imageQualityOffset specifies how many indices to go back from most suited, and is specified
    // by user choice in preference for image quality.
    // It is also used in SettingsFragment so imageQualityOffset cannot be got from preferences
    // inside the method, but has to be passed as a parameter.
    void setPosterAndBackdropIndices(int imageQualityOffset) {

        // Get all shared preferences.
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        // Get user preferred poster width.
        int fetchPosterWidth = Integer.parseInt(sharedPreferences.getString(
                getString(R.string.pref_poster_size_key),
                getString(R.string.pref_poster_size_default)));
        fetchPosterWidth = (int) (fetchPosterWidth * mDisplayMetrics.density);
        // Find the first poster width bigger than the largest user option and choose it for fetch
        // (if there is one - if the screen is not bigger than the largest one)
        for (int i = 0; i < Util.POSTER_WIDTHS.length; i++) {
            if (Util.POSTER_WIDTHS[i] - fetchPosterWidth > 0) {
                if (i - imageQualityOffset > 0) {
                    mPosterIndex = i - imageQualityOffset;
                } else {
                    mPosterIndex = 0;
                }
                break;
            }
        }

        // Get screen width - the shorter side.
        int shorterSide;
        if (mIsLandscape) { shorterSide = mDisplayMetrics.heightPixels; } else {
            shorterSide = mDisplayMetrics.widthPixels;
        }
        // First, for phones, make fetchBackdropWidth correspond to the shorter screen size.
        int fetchBackdropWidth = shorterSide;
        // For tablets make fetchBackdropWidth correspond to fragment detail width.
        if (mTwoPane) {fetchBackdropWidth = mFragDetailsWidth;}
        // Find the first backdrop width bigger than the determined fetchBackdropWidth and choose
        // it for fetch (if there is one - if the screen is not bigger than the largest one)
        for (int i = 0; i < Util.BACKDROP_WIDTHS.length; i++) {
            if (Util.BACKDROP_WIDTHS[i] - fetchBackdropWidth > 0) {
                if (i - imageQualityOffset > 0) {
                    mBackdropIndex = i - imageQualityOffset;
                } else {
                    mBackdropIndex = 0;
                }
                break;
            }
        }
    }


    public void setRecyclerViewLayoutManager() {
        posterAdapter.recyclerView.setLayoutManager(posterAdapter.getGridLayoutManager());
    }

}
