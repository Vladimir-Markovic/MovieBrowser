/*
 * Copyright (C) 2017 Vladimir Markovic
 */

package com.humaneapps.popularmovies;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.preference.PreferenceManager;
import android.support.v7.widget.Toolbar;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Display;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.humaneapps.popularmovies.data.MoviesContract;
import com.humaneapps.popularmovies.service.ServiceDeleteImages;
import com.humaneapps.popularmovies.service.ServiceFetch;

import java.util.ArrayList;

/**
 * Starting activity - contains code for init, fetching data and displaying messages and commonly
 * accessed (posterAdapter, spinnerIndex, fragment widths, etc).
 */
public class MainActivity extends AppCompatActivity {

    // For holding and persisting current fetch mPage for each group, 'popular' and 'top_rated'.
    // Third one is there just to prevent IndexOutOfBoundsException, though it's not used.
    private final int[] mPage = {1, 1, 1};

    // For storing sort selection.
    int spinnerIndex = 0;


    private PopularMovies mApplication;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);

        mApplication = (PopularMovies) getApplication();

        // Get shared preferences
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        // Get indicator whether to save images for favourites.
        mApplication.setSavingImages(sharedPreferences.getBoolean(getString(R.string.pref_save_images_key), true));
        // Code in setPosterAndBackdropIndices will determine what is the index (in image widths array)
        // of image width most suited. imageQualityOffset specifies how many indices to go back from
        // most suited, and is specified by user choice in preference for image quality.
        int imageQualityOffset = Integer.parseInt(sharedPreferences.getString(
                getString(R.string.pref_image_quality_key),
                getString(R.string.pref_image_quality_default)));

        // Init display metrics.
        Display display = getWindowManager().getDefaultDisplay();
        DisplayMetrics mDisplayMetrics = new DisplayMetrics();
        display.getMetrics(mDisplayMetrics);
        mApplication.setDisplayMetrics(mDisplayMetrics);

        // Get and store orientation.
        mApplication.setLandscape(
                getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE);

        // If fragmentMain is present it is tablet mode and no need to add it as it's contained in
        // the resource layout. In phones need to add it to fragmentContainer.

        // Set mode (twoPane) and fragment widths:
        if (findViewById(R.id.fragmentMain) != null) {
            // If two-pane tablet mode, set it to true.
            mApplication.setTwoPane(true);
        } else {
            // If phone mode, set twoPane to reflect.
            mApplication.setTwoPane(false);
            // In phone mode need to add MainFragment if not added to fragmentContainer.
            if (findViewById(R.id.fragmentContainer) != null) {
                if (savedInstanceState == null) {
                    showFragment(MainFragment.newInstance(), getString(R.string.app_name), false);
                }
            }
        }
        mApplication.setFragmentWidths();
        if (mApplication.isTwoPane()) {
            // Set MainFragment width to calculated.
            Fragment mainFragment = getSupportFragmentManager().findFragmentByTag(getString(R.string.app_name));
            if (mainFragment != null) {
                View mfView = mainFragment.getView();
                ViewGroup.LayoutParams params;
                if (mfView != null) {
                    params = mainFragment.getView().getLayoutParams();
                    params.width = mApplication.getFragMainWidth();
                    mfView.setLayoutParams(params);
                }
            }
        }

        // Code in setPosterAndBackdropIndices will determine what is the index (in image widths array)
        // of image width most suited. imageQualityOffset specifies how many indices to go back from
        // most suited, and is specified by user choice in preference for image quality.
        mApplication.setPosterAndBackdropIndices(imageQualityOffset);

        // Create PosterAdapter object.
        mApplication.posterAdapter = new PosterAdapter(this);

        if (savedInstanceState != null) {
            // Preserve state on rotation.
            spinnerIndex = savedInstanceState.getInt(Util.PARAM_SPINNER_INDEX, 0);
            mPage[Util.SPINNER_INDEX_POPULAR] = savedInstanceState.getInt(
                    Util.PARAM_PAGE_POPULAR, 1);
            mPage[Util.SPINNER_INDEX_TOP_RATED] = savedInstanceState.getInt(
                    Util.PARAM_PAGE_TOP_RATED, 1);
            ArrayList<String> tempArrayList = savedInstanceState.getStringArrayList(Util.PARAM_POPULAR_JSONS);
            if (tempArrayList != null) {
                mApplication.posterAdapter.jsonStrings[Util.SPINNER_INDEX_POPULAR].addAll(tempArrayList);
            }
            tempArrayList = savedInstanceState.getStringArrayList(Util.PARAM_TOP_RATED_JSONS);
            if (tempArrayList != null) {
                mApplication.posterAdapter.jsonStrings[Util.SPINNER_INDEX_TOP_RATED].addAll(tempArrayList);
            }
        } else {
            // If 'cold' start, 'garbage collect' images of ex-favourites (or delete all if user
            // changed preference for saving images to off).
            Intent deleteImagesIntent = new Intent(this, ServiceDeleteImages.class);
            deleteImagesIntent.putExtra(getString(R.string.pref_save_images_key), mApplication.saveImages());
            startService(deleteImagesIntent);
        }

    } // End onCreate


    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        // Save state on rotation.
        outState.putInt(Util.PARAM_SPINNER_INDEX, spinnerIndex);
        outState.putInt(Util.PARAM_PAGE_POPULAR, mPage[Util.SPINNER_INDEX_POPULAR]);
        outState.putInt(Util.PARAM_PAGE_TOP_RATED, mPage[Util.SPINNER_INDEX_TOP_RATED]);
        outState.putStringArrayList(Util.PARAM_POPULAR_JSONS,
                mApplication.posterAdapter.jsonStrings[Util.SPINNER_INDEX_POPULAR]);
        outState.putStringArrayList(Util.PARAM_TOP_RATED_JSONS,
                mApplication.posterAdapter.jsonStrings[Util.SPINNER_INDEX_TOP_RATED]);
    }


    // Inflate the menu and add items to the action bar if it is present.
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_activity_main, menu);
        return true;
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Get action id.
        int id = item.getItemId();

        // For action 'settings':
        if (id == R.id.action_settings) {
            // Show SettingsFragment
            showFragment(new SettingsFragment(), getString(R.string.title_settings), true);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }


    /**
     * For showing Main or Settings Fragment.
     *
     * @param fragment       - instance of the fragment to be shown.
     * @param title          - title of the fragment to be shown - added to transaction and used as tag.
     * @param addToBackStack - boolean to determine whether to add fragment transaction to back stack.
     */
    private void showFragment(Fragment fragment, String title, boolean addToBackStack) {
        // If fragment is already created don't add another on top. Uses title as tag.
        if (getSupportFragmentManager().findFragmentByTag(title) == null) {
            // Create new fragment transaction
            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            // Add this fragment to fragment_container.
            transaction.add(R.id.fragmentContainer, fragment, title);
            // If so specified, add the transaction to the back stack so the user can navigate back
            // (no for MainFragment, yes for SettingsFragment).
            if (addToBackStack) { transaction.addToBackStack(title); }
            // Commit the transaction
            transaction.commit();
        }
    } // End showFragment


    // Reset screen titles for phones. For tablets title is always application name.
    @Override
    public void onBackPressed() {
        super.onBackPressed();
        // Get back stack count after back press
        int backStackCount = getSupportFragmentManager().getBackStackEntryCount();
        if (backStackCount == 0) {
            // Reset title to main fragment when coming back to it from other fragments.
            // Since it's first and not added to back stack it's showing at count 0.
            setTitle(getString(R.string.app_name));
        } else {
            // Reset title to details fragment when coming back from settings fragment as fragment
            // titles are set as tag when adding to back stack (in phones).
            if (!mApplication.isTwoPane()) {
                try {
                    setTitle(getSupportFragmentManager().getBackStackEntryAt(backStackCount - 1).getName());
                } catch (IndexOutOfBoundsException e) {
                    setTitle(getString(R.string.title_details));
                }
            }
        }
    } // End onBackPressed


    // Execute service for fetching updated movie data online, if online.
    void fetch(int page) {

        if (Util.isOnline(this)) {

            // Never fetch if showing favourites (this would add unwanted new pages).
            if (showingFavourites()) { return; }

            // Update current mPage number.
            this.mPage[spinnerIndex] = page;

            // Start ServiceFetch with passed arguments for mPage and spinner index (group).
            Intent intent = new Intent(this, ServiceFetch.class);
            intent.putExtra(Util.PARAM_SPINNER_INDEX, spinnerIndex);
            intent.putExtra(Util.PARAM_PAGE, page);
            startService(intent);
        }
    } // End fetch method.


    // Calculate next mPage number and fetch
    void nextPage() {
        int nextPage = mPage[spinnerIndex] + 1;
        fetch(nextPage);
    }


    // Receive resulting JSON string of movies from ServiceFetch.
    private final BroadcastReceiver mLocalServiceFetchReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            if (intent.hasExtra(MoviesContract.COLUMN_DATA_JSON)) {
                // Get fetched JSON string of movies.
                String jsonString = intent.getStringExtra(MoviesContract.COLUMN_DATA_JSON);
                // Message might have been shown if offline. Remove it if it was as we're back online.
                removeMessage();
                // Prevent exceptions and unwanted situations.
                if (jsonString == null || showingFavourites()) { return; }
                // For mPage 1, clear ArrayList of JSON strings for current spinner index in PosterAdapter.
                if (mPage[spinnerIndex] == 1) { clearJsonStrings(); }
                // Add movies to poster adapter from JSON string. This also adds JSON string to
                // ArrayList of JSON strings for current spinner index in PosterAdapter.
                mApplication.posterAdapter.addPage(jsonString);
            }
        }

    };


    @Override
    protected void onResume() {
        super.onResume();
        // Register receiver for broadcasts from ServiceFetch.
        LocalBroadcastManager.getInstance(this).registerReceiver(mLocalServiceFetchReceiver,
                new IntentFilter(Util.PARAM_SERVICE_BROADCAST));
        // Show preserved fetched data on rotation.
        restore();
    }


    @Override
    public void onPause() {
        super.onPause();
        // Unregister receiver for broadcasts from ServiceFetch.
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mLocalServiceFetchReceiver);
    }


    // Show preserved fetched data on rotation.
    void restore() {
        // Get current RecyclerView scroll position.
        int scrollPreserve = mApplication.posterAdapter.recyclerView.computeVerticalScrollOffset();
        // If was showing favourites, just re-show favorites again. If showing popular or top_rated,
        // if there is data fetched, reshow that data, if not start fetch from mPage 1.
        if (showingFavourites()) {
            mApplication.posterAdapter.showFavourite();
        } else {
            if (hasData()) {
                addPages();
            } else {
                mApplication.posterAdapter.clear();
                fetch(1);
                // Don't preserve scroll if restarting from mPage 1; return.
                return;
            }
        }
        // Restore RecyclerView scroll position above stored.
        mApplication.posterAdapter.recyclerView.scrollToPosition(0);
        mApplication.posterAdapter.recyclerView.scrollBy(0, scrollPreserve);
    }


    // While offline, showOfflineMessage() keeps doing retry() until back online to restore
    // (in specified intervals). retry() in turn calls showOfflineMessage() again if still offline.
    private void retry() {
        if (Util.isOnline(this)) {
            restore();
            // If showing favourites or data exists remove message here. Otherwise, message will be
            // removed when data is displayed after fetch(), onReceive from ServiceFetch.
            if (showingFavourites() || hasData()) { removeMessage(); }
        } else { showOfflineMessage(); }
    }


    // Show message to turn internet on (if there is no internet connection and not showing favourites).
    // Message will be shown while showing favourite only while offline if specified to save favourite
    // images and there is a movie in favourites without saved image to show.
    void showOfflineMessage(boolean... params) {

        // Don't show message if showing favourites.
        boolean stopMessage = showingFavourites();
        // params[0] is used to force showing message even if showingFavourites, for case explained above.
        if (params.length > 0) { stopMessage = !params[0]; }
        // If showing favourites and not forcing message, don't show it.
        if (stopMessage) { return; }
        // Interval to retry again to check if can restore online.
        final int RETRY_MILLIS = 5000;
        // Show 'You are offline' message passing the message and optional icon resource id.
        showTopYellowMessage(getString(R.string.message_offline), 0);
        // Retry to restore online after specified interval.
        final Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() { retry(); }
        }, RETRY_MILLIS);

    } // End showReloadMessage method


    // Method for specifying a message and optional icon for addTopYellowMessage method.
    private void showTopYellowMessage(String message, int imageId) {
        if (message == null || "".equals(message)) { return; }
        addTopYellowMessage(message, imageId,
                getResources().getDimension(R.dimen.text_size_xs),
                (int) getResources().getDimension(R.dimen.gap_xxs));
    }


    // Show a message with yellow background at top of the screen, with specified text size and padding.
    private void addTopYellowMessage(String message, int imageId, float messageTextSize, int padding) {
        // If old message is showing, remove it
        removeMessage();
        addMessage(message, imageId, messageTextSize, padding, Color.YELLOW, 0);
    }


    // Views for adding a message to user
    private LinearLayout mLinearLayout;
    private LinearLayout mMainLayout;
    private TextView mTxvMessage;
    private ImageView mImvLoading;


    // Show message text view and icon and set them
    private void addMessage(String messageText, int imageId, float messageTextSize, int padding,
                            int bgdColor, int insertPosition) {

        // Get layout into which to insert a message. It is main fragment.
        if (mApplication.isTwoPane()) {
            mMainLayout = (LinearLayout) findViewById(R.id.spnSort).getParent();
        } else {
            mMainLayout = findViewById(R.id.llFragmentMain);
        }
        if (mMainLayout == null) { return; }

        // I
        if (mLinearLayout == null || mLinearLayout.indexOfChild(mImvLoading) == -1) {

            ViewGroup.LayoutParams paramsMatchWrap = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            ViewGroup.LayoutParams paramsWrapWrap = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);

            // Set containing linear layout for the message
            mLinearLayout = new LinearLayout(this);
            mLinearLayout.setBackgroundColor(bgdColor);
            mLinearLayout.setGravity(Gravity.CENTER);

            mLinearLayout.setLayoutParams(paramsMatchWrap);
            mLinearLayout.setOrientation(LinearLayout.HORIZONTAL);

            // Set message text view
            if (mTxvMessage == null || mLinearLayout.indexOfChild(mTxvMessage) == -1) {
                mTxvMessage = new TextView(this);
                mTxvMessage.setLayoutParams(paramsWrapWrap);

                mTxvMessage.setGravity(Gravity.CENTER);
                if (padding == -1) {
                    mTxvMessage.setPadding(
                            (int) getResources().getDimension(R.dimen.gap_l),
                            (int) getResources().getDimension(R.dimen.gap_l),
                            (int) getResources().getDimension(R.dimen.gap_m),
                            (int) getResources().getDimension(R.dimen.gap_l)
                    );
                } else {
                    mTxvMessage.setPadding(padding, padding, padding, padding);
                }
                mTxvMessage.setTextSize(TypedValue.COMPLEX_UNIT_PX, messageTextSize);
                mTxvMessage.setTextColor(Color.BLACK);
                mTxvMessage.setText(messageText);
                mLinearLayout.addView(mTxvMessage);
            }

            // Set icon
            if (imageId > 0) {
                if (mImvLoading == null || mLinearLayout.indexOfChild(mImvLoading) == -1) {
                    mImvLoading = new ImageView(this);
                    mImvLoading.setLayoutParams(paramsWrapWrap);
                    mImvLoading.setImageResource(imageId);
                    Glide.with(getApplicationContext()).load(imageId).asGif()
                            .override(
                                    (int) (messageTextSize + 1 + 2 * padding),
                                    (int) (messageTextSize + 1 + 2 * padding))
                            .diskCacheStrategy(DiskCacheStrategy.SOURCE).into(mImvLoading);
                    mLinearLayout.addView(mImvLoading);
                }
            }

            // Add containing layout for message into a inserting layout.
            mMainLayout.addView(mLinearLayout, insertPosition);

        } // End if mLinearLayout != null

    } // End addMessage method.


    // Remove error message text view, icon image and containing layout.
    void removeMessage() {
        if (mMainLayout != null) {
            if (mLinearLayout != null && mMainLayout.indexOfChild(mLinearLayout) != -1) {
                if (mTxvMessage != null && mLinearLayout.indexOfChild(mTxvMessage) != -1) {
                    mLinearLayout.removeView(mTxvMessage);
                    mTxvMessage = null;
                }
                if (mImvLoading != null && mLinearLayout.indexOfChild(mImvLoading) != -1) {
                    mLinearLayout.removeView(mImvLoading);
                    mImvLoading = null;
                }
                mMainLayout.removeView(mLinearLayout);
                mLinearLayout = null;
            }

            mMainLayout = null;
        }
    } // End removeMessage method.


    // Return true if spinner index corresponds to favourite.
    boolean showingFavourites() {
        return spinnerIndex == Util.SPINNER_INDEX_FAVOURITE;
    }


    // Return true if there is any json string containing movie data stored.
    boolean hasData() {
        return mApplication.posterAdapter.jsonStrings[spinnerIndex].size() > 0;
    }


    // Clear all stored movie data for current spinner selection.
    private void clearJsonStrings() { mApplication.posterAdapter.jsonStrings[spinnerIndex].clear(); }


    // Restore movie data from stored JSON strings for current spinner selection.
    void addPages() { mApplication.posterAdapter.addPages(
            mApplication.posterAdapter.jsonStrings[spinnerIndex]); }


    @Override
    protected void onDestroy() {
        mApplication.posterAdapter = null;
        super.onDestroy();
    }


} // End MainActivity class

