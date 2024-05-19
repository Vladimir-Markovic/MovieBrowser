/*
 * Copyright (C) 2017 Vladimir Markovic
 */

package com.humaneapps.popularmovies;


import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityOptionsCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.humaneapps.popularmovies.data.MoviesContract;
import com.humaneapps.popularmovies.databinding.PosterItemBinding;
import com.humaneapps.popularmovies.service.ServiceSaveImages;

import java.io.File;
import java.util.ArrayList;


/**
 * Adapter for RecyclerView displaying movie posters in the Main Fragment.
 */
class PosterAdapter extends RecyclerView.Adapter<PosterAdapter.MyViewHolder> {

    // For storing instance of RecyclerView (in onAttachedToRecyclerView) for calling its public methods.
    RecyclerView recyclerView;
    // For storing passed instance of MainActivity for calling its public methods and fields and passing as context.
    private final MainActivity mMainActivity;
    private final PopularMovies mApplication;

    // NOTE: I am learning more about performance at the moment. It is a question mark for me if what
    // I am doing with keeping instance of MainActivity for passing as context and calling it's
    // methods is good. My guess is its not the best practice. Also I am not sure I am releasing
    // resources everywhere where I should properly. I would appreciate if you would take the time
    // to comment on this in particular.

    // Used only for two pane tablet layout for reflecting changes when fav button is clicked in RecyclerView.
    private DetailsFragment detailsFragment;
    // For detecting connection changes
    private boolean mWasOnline;
    // Array of Lists of JSON strings representing total fetched data. Size of array is 3 - for
    // each spinner selection - popular, top rated and favourite
    @SuppressWarnings("unchecked")
    final ArrayList<String>[] jsonStrings = new ArrayList[Util.GROUPS.length];
    // List of Movie objects representing currently displayed adapter data.
    private final ArrayList<Movie> mMovies = new ArrayList<>();
    // List of tmdbId of all movies stored in db as favourites.
    private final ArrayList<Integer> mFavTmdbIds = new ArrayList<>();
    // For storing column details
    private int mColumnWidth, mColumnHeight, mNumberOfColumns;
    // For storing and returning current item count.
    private int mItemCount;


    // Constructor
    PosterAdapter(MainActivity mainActivity) {
        // Store passed instance of MainActivity for calling its methods and fields and passing as context.
        mMainActivity = mainActivity;
        mApplication = (PopularMovies) mainActivity.getApplication();
        // Get preferred poster size.
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(mApplication);
        int posterSize = Integer.parseInt(sharedPreferences.getString(
                mMainActivity.getString(R.string.pref_poster_size_key),
                mMainActivity.getString(R.string.pref_poster_size_default)));
        // Using preferred poster size set number of columns and store their width and height.
        setColumns(posterSize);
        // Populate favourite tmdbIds array list from db. Sort by _ID to correspond to mMovies
        // when displaying favourites.
        Uri uri = MoviesContract.getTableUri(MoviesContract.TableMovies.TABLE_NAME);
        Cursor cursor = mainActivity.getContentResolver().query(uri,
                new String[]{"_ID", MoviesContract.COLUMN_TMDB_ID}, null, null, "_ID");
        if (cursor != null) {
            while (cursor.moveToNext()) {
                mFavTmdbIds.add(cursor.getInt(1));
            }
            cursor.close();
        }
        // Store details fragment if already created (two pane only).
        if (mApplication.isTwoPane()) {
            detailsFragment = (DetailsFragment)
                    mMainActivity.getSupportFragmentManager()
                            .findFragmentByTag(mMainActivity.getString(R.string.title_details));
        }
        // Instantiate jsonStrings ArrayList.
        for (int i = 0; i < 3; i++) {
            jsonStrings[i] = new ArrayList<>();
        }
        // Instantiate flag for connection switch.
        mWasOnline = Util.isOnline(mApplication);
    }


    @Override
    public void onAttachedToRecyclerView(@NonNull RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);
        // Store instance of attached RecyclerView for calling its public methods.
        this.recyclerView = recyclerView;
    }


    @Override
    public void onViewAttachedToWindow(@NonNull MyViewHolder holder) {
        super.onViewAttachedToWindow(holder);
        // When reach end of RecyclerView, fetch another page.
        if (holder.getAdapterPosition() == mItemCount - mNumberOfColumns) {
            mMainActivity.nextPage();
        }
    }


    // Create and return custom ViewHolder.
    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
        View rootView = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.poster_item, viewGroup, false);
        return new MyViewHolder(rootView);
    }


    @Override
    public void onBindViewHolder(final MyViewHolder myViewHolder, int position) {

        // If something went wrong skip - don't display that movie.
        if (position == -1 || position >= mMovies.size() || myViewHolder == null ||
                myViewHolder.imvPoster == null || myViewHolder.btnFav == null) { return; }

        // Get Movie object for given position.
        Movie movie = mMovies.get(position);

        // If something is wrong with that movie data skip it - don't display that movie.
        if (movie.getPoster() == null && movie.getTitle() == null) { return; }

        // Set tmdbId of this movie to button view as tag, to be able to refer to it later.
        myViewHolder.btnFav.setTag(movie.getTmdbId());
        myViewHolder.txvPoster.setVisibility(View.GONE);

        if (mApplication.isTwoPane() && detailsFragment == null) {
            // In tablet create DetailsFragment and show first movie on start.
            detailsFragment = showDetailsFragment(movie.getDataJson());
        }
        // Determine if connection status changed (from online to offline or vice versa)
        if ((Util.isOnline(mApplication) & !mWasOnline) ||
                (!Util.isOnline(mApplication) & mWasOnline)) {
            // Re-show image in details fragment. If movie is not favourite or is but user chose
            // not to save images for favourites, offline title is shown instead of image if data
            // exists. When going back online show image instead of title. If going offline from
            // online title is shown instead of image.
            if (mApplication.isTwoPane() && detailsFragment != null) {
                detailsFragment.showImageWithGlide();
            }
            // Remove all views to force RecyclerView redraw. Also destroy drawing cache and refresh
            // drawable state.
            refreshRecyclerView();
            // Show or remove message as needed.
            if (mWasOnline) {
                mMainActivity.showOfflineMessage();
            } else {
                mMainActivity.removeMessage();
            }
            // Store new connection state.
            mWasOnline = Util.isOnline(mMainActivity);
        }
        // Store if movie is in favourites.
        boolean isFavourite = mFavTmdbIds.contains(movie.getTmdbId());
        // Add poster image to RecyclerView using Glide. This method is complex and has several
        // options for showing image (first try from file if movie is favourite, then from url if
        // it's not and online, then display title as image if all else failed).
        showImageWithGlide(movie, myViewHolder);
        // Color the fav button star to reflect if movie is favourite or not.
        if (isFavourite) {
            myViewHolder.btnFav.setTextColor(Color.YELLOW);
        } else {
            myViewHolder.btnFav.setTextColor(Color.BLACK);
        }

        // Set listener for clicking on the poster.
        View.OnClickListener posterClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View viewPoster) {

                // Get ViewHolder for this view.
                MyViewHolder myViewHolder = (MyViewHolder) recyclerView.findContainingViewHolder(viewPoster);
                if (myViewHolder == null) { return; }
                // Find position of this view in adapter.
                final int position = myViewHolder.getAdapterPosition();
                // On error display message to user and return.
                if (position >= mMovies.size()) {
                    Toast.makeText(mMainActivity, mApplication.getString(
                            R.string.error_details_missing), Toast.LENGTH_SHORT).show();
                    return;
                }
                // Get movie at given position and pass its JSON string to DetailsFragment.
                Movie movie = mMovies.get(position);
                if (mApplication.isTwoPane()) {
                    // In two pane store the returned details fragment to be able to refer to it.
                    detailsFragment = showDetailsFragment(movie.getDataJson());
                } else {
                    showDetailsActivity(viewPoster, movie.getDataJson());
                }

            } // End onClick method.

        }; // End poster OnClickListener.

        myViewHolder.imvPoster.setOnClickListener(posterClickListener);
        myViewHolder.txvPoster.setOnClickListener(posterClickListener);

        // Set listener for clicking on the favourite star button.
        myViewHolder.btnFav.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View viewFav) {

                // Get ViewHolder for this view.
                MyViewHolder myViewHolder = (MyViewHolder) recyclerView.findContainingViewHolder(viewFav);
                if (myViewHolder == null) { return; }
                // Find position of this view in adapter
                final int position = myViewHolder.getAdapterPosition();
                // Store this view as button, to be able to change its text color.
                final Button btnFav = (Button) viewFav;

                // On error display message to user and return.
                if (position >= mMovies.size()) {
                    Toast.makeText(mMainActivity, mApplication.getString(
                            R.string.error_storing_favourite), Toast.LENGTH_SHORT).show();
                    return;
                }
                // Get movie at given position and its tmdbId.
                final Movie movie = mMovies.get(position);
                final int tmdbId = movie.getTmdbId();
                // Store if movie is in favourites.
                final boolean isFavourite = mFavTmdbIds.contains(tmdbId);
                // In tablet, if displayed movie is the same as clicked, change color to star button
                // in DetailsFragment also, to correspond.
                if (mApplication.isTwoPane() && tmdbId == detailsFragment.getTmdbId()) {
                    detailsFragment.changeFavColor(!isFavourite);
                }
                // Change color to star button in RecyclerView to reflect if movie is favourite or not
                // and apply changes to reflect in mFavTmdbIds.
                if (isFavourite) {
                    btnFav.setTextColor(Color.BLACK);
                    removeFav(tmdbId);
                } else {
                    btnFav.setTextColor(Color.YELLOW);
                    addFav(tmdbId, movie);
                }

                // AsyncResponse for async TaskFavourite.
                final TaskFavourite.AsyncResponseFavourite asyncResponseFavourite = new TaskFavourite.AsyncResponseFavourite() {
                    @Override
                    public void processFinish(Boolean isFavourite) {

                        // True is returned when insert new movie to favourites is successful and when
                        // update existing favourite movie is successful.
                        // False is returned when delete favourite movie is successful
                        // Null is returned when any of the above is unsuccessful. Like if user
                        // double clicks when adding new movie to favourites, than update will fail
                        // as it is removed before it can be updated.

                        if (isFavourite == null) {
                            // On fail revert - re-color accordingly to reflect current state.
                            // Get current state
                            isFavourite = isItFavourite(tmdbId);
                            if (isFavourite == null) { return; }
                            // In tablet, if displayed movie is the same as clicked, change color
                            // to star button in DetailsFragment also, to correspond.
                            if (mApplication.isTwoPane() && tmdbId == detailsFragment.getTmdbId()) {
                                detailsFragment.changeFavColor(isFavourite);
                            }
                            // Change color to star button in RecyclerView to reflect and revert changes.
                            if (isFavourite) {
                                btnFav.setTextColor(Color.YELLOW);
                                addFav(tmdbId, movie);
                            } else {
                                btnFav.setTextColor(Color.BLACK);
                                removeFav(tmdbId);
                            }

                        }
                    }
                };
                // AsyncResponse for async TaskVideosOrReviews - for fetching reviews.
                TaskVideosOrReviews.AsyncResponseVideosOrReviews mAsyncResponseVR =
                        new TaskVideosOrReviews.AsyncResponseVideosOrReviews() {
                            @Override
                            public void processFinish(String[] strArReturn) {
                                if (strArReturn != null && strArReturn.length == 2) {
                                    // Store returned reviews JSON string.
                                    movie.setReviewsJson(strArReturn[1]);
                                    // Update movie in favourite table with reviews json string.
                                    TaskFavourite taskFavourite = new TaskFavourite(asyncResponseFavourite,
                                            mApplication, movie.getContentValues(), tmdbId);
                                    taskFavourite.execute();
                                }
                            }
                        };

                // Apply changes to db.
                if (isFavourite) {
                    // If it is favourite, delete movie from favourite_movies table.
                    TaskFavourite taskFavourite = new TaskFavourite(asyncResponseFavourite, mApplication,
                            null, tmdbId);
                    taskFavourite.execute(true);
                } else {
                    // If not yet favourite, insert movie into favourite_movies table.
                    TaskFavourite taskFavourite = new TaskFavourite(asyncResponseFavourite, mApplication,
                            movie.getContentValues(), tmdbId);
                    taskFavourite.execute(false);

                    if (Util.isOnline(mApplication)) {
                        // Fetch reviews to update reviews_json column for that movie, if online.
                        TaskVideosOrReviews taskTaskVideosOrReviews = new TaskVideosOrReviews(mAsyncResponseVR);
                        taskTaskVideosOrReviews.execute(Util.IDENTIFIER_REVIEWS, tmdbId + "");
                        // Save images for movie added to favourites, if user chose so, online and not saved.
                        if (mApplication.saveImages()) {
                            File fileDir = new File(Util.getInternalFilePath(mApplication));
                            File imgDirFile = new File(fileDir, movie.getPoster());
                            if (!imgDirFile.exists()) {
                                saveImages(movie);
                            }
                        }
                    }
                }

            } // End onClick
        }); // End favListener

    } // onBindViewHolder


    @Override
    public int getItemCount() { return mItemCount; }


    // Clear adapter data, set item count, notify data change.
    void clear() { mMovies.clear(); myNotifyDataSetChanged(); }


    // Add movies to poster adapter from JSON string. This also adds JSON string to
    // ArrayList of JSON strings for current spinner index in PosterAdapter.
    void addPage(String jsonString) {
        mMovies.addAll(Util.extractMovieDataFromJson(jsonString));
        jsonStrings[mMainActivity.spinnerIndex].add(jsonString);
        // Update item count and notify data item range inserted at end position.
        myNotifyItemRangeInsertedAtEnd(mItemCount);
    }


    // Restore movie data from stored JSON strings for current spinner selection.
    void addPages(ArrayList<String> jsonStrings) {
        mMovies.clear();
        for (String jsonString : jsonStrings) {
            mMovies.addAll(Util.extractMovieDataFromJson(jsonString));
        }
        // Update item count and notify data set changed.
        myNotifyDataSetChanged();
    }


    // Used in poster click to show detail fragment.
    private DetailsFragment showDetailsFragment(String jsonString) {

        String fragTitle = mMainActivity.getString(R.string.title_details);
        // Bundle to pass to DetailsFragment
        Bundle bundle = new Bundle();
        bundle.putString(MoviesContract.COLUMN_DATA_JSON, jsonString);


        // Try to get details fragment finding it by tag (title).
        DetailsFragment detailsFragment = (DetailsFragment)
                mMainActivity.getSupportFragmentManager()
                        .findFragmentByTag(fragTitle);
        // If fragment is already created don't add another on top.
        if (detailsFragment == null) {
            // Create details fragment.
            detailsFragment = DetailsFragment.newInstance();
            detailsFragment.setArguments(bundle);
            // Create new fragment transaction
            FragmentTransaction transaction = mMainActivity.getSupportFragmentManager().beginTransaction();
            // Add details fragment to fragment_container.
            transaction.add(R.id.fragmentContainer, detailsFragment, fragTitle);
            if (!mApplication.isTwoPane()) {
                // In phones, add the transaction to the back stack so the user can navigate back.
                transaction.addToBackStack(fragTitle);
            }
            // Commit the transaction
            transaction.commit();
        } else {
            if (mApplication.isTwoPane() && detailsFragment.isResumed()) {
                // In tablets, if details fragment is already added, just swap its data.
                detailsFragment.setJsonString(jsonString);
            }
        }

        return detailsFragment;
    }


    private void showDetailsActivity(View view, String jsonString) {
        Intent intent = new Intent(mApplication, DetailsActivity.class);
        intent.putExtra(MoviesContract.COLUMN_DATA_JSON, jsonString);
        ActivityOptionsCompat options = ActivityOptionsCompat.makeSceneTransitionAnimation(
                mMainActivity, view, mApplication.getString(R.string.shared_element_image));
        mMainActivity.startActivity(intent, options.toBundle());
    }


    // Swap PosterAdapter data to data from db favourite_movies table.
    void showFavourite() {
        // Populate mMovies from db. Sort by _ID to correspond to mFavTmdbIds.
        Uri uri = MoviesContract.getTableUri(MoviesContract.TableMovies.TABLE_NAME);
        Cursor cursor = mApplication.getContentResolver().query(uri,
                new String[]{"_ID", MoviesContract.COLUMN_DATA_JSON}, null, null, "_ID");
        if (cursor != null) {
            clear();
            while (cursor.moveToNext()) {
                Movie movie = new Movie(cursor.getString(1));
                mMovies.add(movie);
            }
            cursor.close();
            // Update item count and notify data set changed.
            myNotifyDataSetChanged();
        }
    }


    // Add specified tmdbId to mFavTmdbIds, and if displaying favourites specified movie to mMovies.
    private void addFav(int tmdbId, Movie movie) {
        if (!mFavTmdbIds.contains(tmdbId)) {
            mFavTmdbIds.add(tmdbId);
            if (mMainActivity.showingFavourites()) {
                mMovies.add(movie);
                // Update item count and notify data item inserted at end position.
                myNotifyItemInsertedAtEnd();
            }
        }
    }


    // Remove specified tmdbId from mFavTmdbIds, and if showing favourites corresponding movie from mMovies.
    private void removeFav(int tmdbId) {
        int positionInFav = mFavTmdbIds.indexOf(tmdbId);
        if (positionInFav > -1) {
            mFavTmdbIds.remove(positionInFav);
            if (mMainActivity.showingFavourites()) {
                mMovies.remove(positionInFav);
                // Update item count and notify data item removed at given position.
                myNotifyItemRemoved(positionInFav);
            }
        }
    }


    // Used in tablets from DetailsFragment to reflect changes for the corresponding movie in the
    // RecyclerView when star button is clicked in the DetailsFragment.
    void changeFavColor(int tmdbId, Movie movie, Boolean isFavourite) {
        if (isFavourite == null) { return; }
        // Reflect changes in mFavTmdbIds, and if showing favourites - mMovies also.
        if (isFavourite) {
            removeFav(tmdbId);
        } else {
            addFav(tmdbId, movie);
        }
        // If showing favourites moves are added and removed from RecyclerView so don't try to
        // reflect changes in star button color.
        if (!mMainActivity.showingFavourites()) {
            // If showing popular or top rated, change star button color for the corresponding
            // movie to reflect if favourite state.
            // Use tag stored in onBindViewHolder to refer to the view by tmdbId.
            View viewFav = recyclerView.findViewWithTag(tmdbId);
            if (viewFav != null) {
                MyViewHolder myViewHolder = (MyViewHolder) recyclerView.findContainingViewHolder(viewFav);
                if (myViewHolder != null) {
                    if (isFavourite) {
                        myViewHolder.btnFav.setTextColor(Color.BLACK);
                    } else {
                        myViewHolder.btnFav.setTextColor(Color.YELLOW);
                    }
                }
            }
        }
    }


    // Check if movie is in favourites table in db.
    private Boolean isItFavourite(int tmdbId) {
        Boolean isFavourite = null;
        Uri tableUri = MoviesContract.getTableUri(MoviesContract.TableMovies.TABLE_NAME);
        Cursor cursor = mApplication.getContentResolver().query(tableUri,
                new String[]{MoviesContract.COLUMN_TMDB_ID},
                MoviesContract.COLUMN_TMDB_ID + " = ?", new String[]{tmdbId + ""}, null);
        if (cursor != null) {
            isFavourite = cursor.moveToFirst();
            cursor.close();
        }
        return isFavourite;
    }


    // Add poster image to RecyclerView using Glide. This method is complex and has several
    // options for showing image (first try from file if movie is favourite, then from url if
    // it's not and online, then display title as image if all else failed).
    private void showImageWithGlide(Movie movie, MyViewHolder myViewHolder) {
        final ImageView imageView = myViewHolder.imvPoster;
        // Prepare the placeholder size. Placeholder image is plain with same color as the background.
        GradientDrawable placeholder = new GradientDrawable();
        placeholder.setShape(GradientDrawable.RECTANGLE);
        placeholder.setColor(ContextCompat.getColor(mApplication, R.color.colorGray));
        placeholder.setSize(mColumnWidth, mColumnHeight);

        // Add poster image using Glide

        // If showing favourites use the poster image file if saved
        if (mApplication.saveImages() && mMainActivity.showingFavourites()) {
            // Try to get poster image file.
            File posterFile = Util.getFile(Util.getInternalFilePath(mApplication), movie.getPoster());
            // If successfully got poster image file show it using Glide.
            if (posterFile != null) {
                Glide.with(mApplication).load(posterFile)
                        .override(mColumnWidth, mColumnHeight).placeholder(placeholder)
                        .into(imageView);
            } else {
                //If unsuccessful with poster image file and online, get poster image from url
                if (Util.isOnline(mApplication)) {
                    glideImageFromUrl(movie, placeholder, myViewHolder);
                    saveImages(movie); // Save images for movies saved to favourite when offline.
                } else {
                    // If unsuccessful with poster image file and offline, try to get backdrop image file.
                    File backdropFile = Util.getFile(Util.getInternalFilePath(mApplication), movie.getBackdrop());
                    // If successfully got backdrop image file show it using Glide.
                    if (backdropFile != null) {
                        Glide.with(mApplication).load(backdropFile)
                                .override(mColumnWidth, mColumnHeight).placeholder(placeholder)
                                .into(imageView);
                    } else {
                        // If unsuccessful with backdrop image file, show title as image if available.
                        setTitleAsPoster(movie.getTitle(), myViewHolder);
                        mMainActivity.showOfflineMessage(true);
                    }
                }
            }
        } else {
            // If favourite images are not saved or not showing favourites:
            if (Util.isOnline(mApplication)) {
                // If online show poster image from url.
                glideImageFromUrl(movie, placeholder, myViewHolder);
            } else {
                // If offline show title as image.
                setTitleAsPoster(movie.getTitle(), myViewHolder);
            }
        }
    } // End showImageWithGlide method.


    // Show image from url using Glide.
    private void glideImageFromUrl(final Movie movie, final GradientDrawable placeholder,
                                   final MyViewHolder myViewHolder) {

        // Get full url strings.
        final String posterPathFull = Util.getFullImagePath(movie.getPoster(), mApplication.getPosterWidth());
        final String backdropPathFull = Util.getFullImagePath(movie.getBackdrop(), mApplication.getBackdropWidth());
        final ImageView imageView = myViewHolder.imvPoster;

        // Try to 'Glide' poster image from specified url string.
        Glide.with(mApplication).load(posterPathFull)
                .override(mColumnWidth, mColumnHeight).placeholder(placeholder)
                // If cannot display poster from url, try displaying backdrop from url.
                .listener(new RequestListener<Drawable>() {
                    @Override
                    public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
//                        Glide.with(mApplication).load(backdropPathFull) TODO
//                                .override(mColumnWidth, mColumnHeight).placeholder(placeholder)
//                                // If cannot display backdrop from url, display title as image.
//                                .listener(new RequestListener<Drawable>() {
//                                    @Override
//                                    public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
//                                        setTitleAsPoster(movie.getTitle(), myViewHolder);
//                                        return true;
//                                    }
//                                    @Override
//                                    public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
//                                        return false;
//                                    }
//                                }).into(imageView); // Load poster if successfully fetched it from url.
                        return true;
                    }
                    @Override
                    public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                        return false;
                    }
                }).into(imageView); // Load poster if successfully fetched it from url.

    }


    // If title is available, set title as poster image.
    private void setTitleAsPoster(String title, MyViewHolder myViewHolder) {
        if (title != null) {
            myViewHolder.txvPoster.setVisibility(View.VISIBLE);
            myViewHolder.txvPoster.setText(title);
        }
    }


    // Call ServiceSaveImages to fetch and save poster and backdrop for shown movie (when added to favourites).
    private void saveImages(Movie movie) {
        if (mApplication.saveImages()) {
            Intent intent = new Intent(mApplication, ServiceSaveImages.class);
            intent.putExtra(Util.PARAM_POSTER_NAME, movie.getPoster());
            intent.putExtra(Util.PARAM_BACKDROP_NAME, movie.getBackdrop());
            intent.putExtra(Util.PARAM_POSTER_WIDTH, mApplication.getPosterWidth());
            intent.putExtra(Util.PARAM_BACKDROP_WIDTH, mApplication.getBackdropWidth());
            mApplication.startService(intent);
        }
    }


    // Remove all views to force RecyclerView redraw. Also destroy drawing cache and refresh drawable state.
    void refreshRecyclerView() {
        recyclerView.removeAllViews();
        recyclerView.destroyDrawingCache();
        recyclerView.refreshDrawableState();
    }


    // Set number of columns and store column width and height.
    void setColumns(int posterSize) {
        float density = mApplication.getDisplayDensity();
        int spacing = mApplication.getPosterSpacing();
        int approximateColumnWidth = (int) (posterSize * density);
        mNumberOfColumns = mApplication.getFragMainWidth() / approximateColumnWidth;
        mColumnWidth = (mApplication.getFragMainWidth() - spacing * (mNumberOfColumns + 1)) / mNumberOfColumns;
        mColumnHeight = (int) (mColumnWidth * mApplication.getPosterRatio());
    }


    // Used in Main and Settings fragments to set layout manager to recycler view, for changing
    // number of columns - on rotation.
    GridLayoutManager getGridLayoutManager() {
        GridLayoutManager gridLayoutManager = new GridLayoutManager(mApplication, mNumberOfColumns);
        gridLayoutManager.setSmoothScrollbarEnabled(true);
        gridLayoutManager.setAutoMeasureEnabled(false);
        gridLayoutManager.setSpanCount(mNumberOfColumns);
        return gridLayoutManager;
    }


    // Update item count and notify data set changed.
    private void myNotifyDataSetChanged() {
        mItemCount = mMovies.size();
        notifyDataSetChanged();
    }


    // Update item count and notify data item removed at given position.
    private void myNotifyItemRemoved(int position) {
        mItemCount = mMovies.size();
        if (mApplication.isTwoPane()) {
            notifyDataSetChanged();
        } else {
            notifyItemRemoved(position);
        }
    }


    // Update item count and notify data item inserted at end position.
    private void myNotifyItemInsertedAtEnd() {
        mItemCount = mMovies.size();
        if (mApplication.isTwoPane()) {
            notifyDataSetChanged();
        } else {
            notifyItemInserted(mItemCount - 1);
        }
    }


    // Update item count and notify data item range inserted at end position.
    private void myNotifyItemRangeInsertedAtEnd(int positionStart) {
        mItemCount = mMovies.size();
        if (mApplication.isTwoPane()) {
            notifyDataSetChanged();
        } else {
            notifyItemRangeInserted(positionStart, mItemCount - positionStart + 1);
        }
    }


    // Custom ViewHolder class for use in RecyclerView.Adapter pattern for holding views.
    class MyViewHolder extends RecyclerView.ViewHolder {

        ImageView imvPoster;
        Button btnFav;
        TextView txvPoster;


        @SuppressWarnings("deprecation")
        MyViewHolder(View view) {
            super(view);
            PosterItemBinding binding = PosterItemBinding.bind(view);
            imvPoster = binding.imvPoster;
            btnFav = binding.btnFavouriteInMain;
            txvPoster = binding.txvPoster;

            btnFav.setText(Html.fromHtml(Util.STAR_SYMBOL));
            txvPoster.setVisibility(View.GONE);
            txvPoster.setWidth(mColumnWidth);
            txvPoster.setHeight(mColumnHeight);
        }
    }


} // End PosterAdapter class.