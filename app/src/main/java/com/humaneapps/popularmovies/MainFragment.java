/*
 * Copyright (C) 2017 Vladimir Markovic
 */

package com.humaneapps.popularmovies;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Spinner;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.humaneapps.popularmovies.databinding.FragmentMainBinding;

/**
 * Contains main (start) screen which displays movie posters in a grid
 * Movies are ordered by popular or top rated, which is determined with a spinner, which also has
 * a position for showing favourite movies stored in database.
 * Every movie poster can be clicked on to display a screen with movie details (within DetailsFragment)
 * Data is pulled from themoviedb.org via http connection.
 */
public class MainFragment extends Fragment {

    // For storing instance of MainActivity (in onActivityCreated) for calling its public
    // methods and fields and passing as context.
    private MainActivity mMainActivity;
    private PopularMovies mApplication;

    // NOTE: I am learning more about performance at the moment. It is a question mark for me if what
    // I am doing with keeping instance of MainActivity for passing as context and calling it's
    // methods is good. My guess is its not the best practice. Also I am not sure I am releasing
    // resources everywhere where I should properly. I would appreciate if you would take the time
    // to comment on this in particular.

    // For displaying movie posters
    RecyclerView recyclerView;
    // Spinner for selection to display 'popular', 'top rated' or 'favourite' movies.
    Spinner mSpnSort;
    // For preserving recycler view scroll when changing sort selection in the above spinner
    // (not used for preserving state on rotation).
    private final int[] mRVScroll = {0, 0, 0};
    // Used for preserving state of recycler view (visible) item position on rotation.
    private final int[] mRVPosition = {0, 0, 0};


    // Required empty public constructor
    public MainFragment() {}


    // For instantiating MainFragment
    public static MainFragment newInstance() {return new MainFragment();}


    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        // Store context into mMainActivity for passing as context until activity is created.
        mMainActivity = (MainActivity) context;
        mApplication = (PopularMovies) ((Activity)context).getApplication();
    } // End onAttach method.


    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Inflate fragment_main containing RecyclerView for displaying movie posters.
        FragmentMainBinding binding = FragmentMainBinding.inflate(inflater, container, false);
        recyclerView = binding.rvPosters;
        mSpnSort = binding.spnSort;
        // Return inflated view.
        return binding.getRoot();
    } // End onCreateView method.


    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        // Store instance of MainActivity for calling its methods and fields and passing as context.
        mMainActivity = (MainActivity) getActivity();
        // Populate spinner with two sort options: "Sort by most popular" and "Sort by top rated'
        Util.populateAndCustomizeSpinner(mSpnSort,
                mApplication.getResources().getStringArray(R.array.sort_display), mApplication,
                android.R.layout.simple_spinner_item, R.layout.spinner_item,
                R.dimen.text_size_s, R.color.colorTextLight, 0, 0
        );

        // Set RecyclerView for displaying movie posters
        recyclerView.setLayoutManager(mApplication.posterAdapter.getGridLayoutManager());
        recyclerView.setNestedScrollingEnabled(false);
        recyclerView.setAdapter(mApplication.posterAdapter);

        // Set spinner listener.
        mSpnSort.post(new Runnable() {
            @Override
            public void run() {
                mSpnSort.setOnItemSelectedListener(mOnItemSelectedListener);
            }
        });
        // If offline start with favourites selected
        if (!Util.isOnline(mApplication)) {
            mSpnSort.setSelection(Util.SPINNER_INDEX_FAVOURITE, false);
        }
        // Restore state on rotation
        if (savedInstanceState != null) {
            // Preserve previous user sort selection on rotation
            if (Util.isOnline(mApplication)) {
                mSpnSort.setSelection(mMainActivity.spinnerIndex, false);
            }
            final int popularScroll = savedInstanceState.getInt(Util.PARAM_SCROLL_POPULAR, 0);
            final int topRatedScroll = savedInstanceState.getInt(Util.PARAM_SCROLL_TOP_RATED, 0);
            final int favouriteScroll = savedInstanceState.getInt(Util.PARAM_SCROLL_FAVOURITE, 0);
            recyclerView.post(new Runnable() {
                @Override
                public void run() {
                    // Read saved scroll position of recycler view for popular.
                    mRVPosition[Util.SPINNER_INDEX_POPULAR] = popularScroll;
                    // Read saved scroll position of recycler view for top_rated.
                    mRVPosition[Util.SPINNER_INDEX_TOP_RATED] = topRatedScroll;
                    // Read saved scroll position of recycler view for favourite.
                    mRVPosition[Util.SPINNER_INDEX_FAVOURITE] = favouriteScroll;
                    recyclerView.scrollToPosition(mRVPosition[mMainActivity.spinnerIndex]);
                }
            });

        }

    } // End onActivityCreated


    @Override
    public void onResume() {
        super.onResume();
        // Set title in title bar.
        getActivity().setTitle(getString(R.string.app_name));
    }


    // Save state on rotation
    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        // Save scroll position for the current spinner selection
        mRVPosition[mMainActivity.spinnerIndex] = ((GridLayoutManager)
                recyclerView.getLayoutManager()).findLastCompletelyVisibleItemPosition();
        // Preserve recycler view scroll.
        outState.putInt(Util.PARAM_SCROLL_POPULAR, mRVPosition[Util.SPINNER_INDEX_POPULAR]);
        outState.putInt(Util.PARAM_SCROLL_TOP_RATED, mRVPosition[Util.SPINNER_INDEX_TOP_RATED]);
        outState.putInt(Util.PARAM_SCROLL_FAVOURITE, mRVPosition[Util.SPINNER_INDEX_FAVOURITE]);
    }


    private void restoreScroll() {
        // Restore recycler view scroll position for this spinner selection, from stored.
        recyclerView.scrollToPosition(0);
        recyclerView.post(new Runnable() {
            @Override
            public void run() {
                if (mRVScroll[mMainActivity.spinnerIndex] != 0) {
                    recyclerView.scrollBy(0, mRVScroll[mMainActivity.spinnerIndex]);
                } else {
                    recyclerView.scrollToPosition(mRVPosition[mMainActivity.spinnerIndex]);
                }
            }
        });
   }


    // Listener for spinner
    private final AdapterView.OnItemSelectedListener mOnItemSelectedListener = new AdapterView.OnItemSelectedListener() {
        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
            if (parent.getId() == R.id.spnSort) {
                // Save scroll for the previous spinner selection
                mRVScroll[mMainActivity.spinnerIndex] = recyclerView.computeVerticalScrollOffset();
                mRVPosition[mMainActivity.spinnerIndex] = ((GridLayoutManager)
                        recyclerView.getLayoutManager()).findLastCompletelyVisibleItemPosition();
                // Store spinner selection.
                mMainActivity.spinnerIndex = position;

                if (mMainActivity.showingFavourites()) {
                    // If position corresponds to showing favourites.
                    mApplication.posterAdapter.showFavourite();
                    mMainActivity.removeMessage();
                } else {
                    // If showing popular or top rated:

                    // Reflect message status to online/offline status.
                    if (!Util.isOnline(mMainActivity)) {
                        mMainActivity.showOfflineMessage();
                    } else {
                        mMainActivity.removeMessage();
                    }
                    // Restore data if stored, or fetch from start (page 1).
                    if (mMainActivity.hasData()) {
                        mMainActivity.addPages();
                    } else {
                        // Update from page 1
                        mApplication.posterAdapter.clear();
                        mMainActivity.fetch(1);
                        return;
                    }
                }

                restoreScroll();
            }
        }


        @Override
        public void onNothingSelected(AdapterView<?> adapterView) {}
    };

}