/*
 * Copyright (C) 2017 Vladimir Markovic
 */

package com.humaneapps.popularmovies;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.StateListDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.text.Html;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Display;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatButton;
import androidx.appcompat.widget.ShareActionProvider;
import androidx.core.content.ContextCompat;
import androidx.core.view.MenuItemCompat;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.humaneapps.popularmovies.data.MoviesContract;
import com.humaneapps.popularmovies.databinding.FragmentDetailsBinding;
import com.humaneapps.popularmovies.service.ServiceSaveImages;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.Locale;

/**
 * Displays movie details: backdrop image, movie title, release date, overview, rating, vote number,
 * videos and reviews
 * Details are passed as a json string from PosterAdapter. Videos and Reviews are fetched.
 */
public class DetailsFragment extends Fragment {

    // Base url for forming video url o show video when user clicks on one.
    private final String YOUTUBE_BASE_URL_STRING = "http://www.youtube.com/watch?v=";
    // For sharing first of the videos.
    private ShareActionProvider mShareActionProvider;
    // For storing instance of MainActivity (in onActivityCreated) for calling its public
    // methods and fields and passing as context.
    private Activity mActivity;
    private PopularMovies mApplication;

    // NOTE: I am learning more about performance at the moment. It is a question mark for me if what
    // I am doing with keeping instance of MainActivity for passing as context and calling its
    // methods is good. My guess is it's not the best practice. Also I am not sure I am releasing
    // resources everywhere where I should properly. I would appreciate if you would take the time
    // to comment on this in particular.

    // Views for displaying the movie details.
    ScrollView mScvDetails;
    LinearLayout mLlDetailFragmentLayout;
    FrameLayout mFlPosterAndRating;
    ImageView mImvPoster;
    TextView mTxvTitle;
    TextView mTxvReleaseDate;
    TextView mTxvOverview;
    TextView mTxvRating;
    TextView mTxvVotes;
    TextView mTxvVideos;
    LinearLayout mLlVideos;
    RadioGroup mRgReviewButtons;
    private RadioButton[] mRbReviews;
    TextView mTxvReviewDisplay;
    Button mBtnFavourite;

    // For holding some of the needed movie details.
    private int mTmdbId;
    private String mTitle;
    // For holding videos keys (forms end of the url).
    private String[] mVideos = new String[Util.MAX_VIDEOS];
    // For holding reviews.
    private String[] mReviews = new String[Util.MAX_REVIEWS];
    // Holds passed JSON string containing the movie details - for persisting state on rotation.
    private String mJsonString;
    // Used in tablets to identify if a new movie is clicked to be displayed (as DF is not recreated)
    private boolean mNewMovie = true;
    // To store info whether the movie is in favourites or not.
    private Boolean mIsFavourite;
    // For restoring scrollview scroll position on rotation.
    private int mScrollPreserve = 0;
    // Custom object for holding (and formatting) movie details.
    private Movie mMovie;


    // Required empty public constructor.
    public DetailsFragment() {}


    // For instantiating DetailsFragment.
    public static DetailsFragment newInstance() { return new DetailsFragment(); }


    // Enable custom menu for adding share item for sharing the first video.
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }


    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        // Store context into mActivity for passing as context until activity is created.
        mActivity = (Activity) context;

    } // End onAttach method.


    @SuppressWarnings("deprecation")
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        FragmentDetailsBinding binding = FragmentDetailsBinding.inflate(inflater, container, false);

        mScvDetails = binding.scvDetailsRoot;
        mLlDetailFragmentLayout = binding.llDetailFragmentLayout;
        mFlPosterAndRating = binding.flDetailPosterAndRating;
        mImvPoster = binding.imvPosterDetail;
        mTxvTitle = binding.txvTitleDetail;
        mTxvReleaseDate = binding.txvReleaseDateDetail;
        mTxvOverview = binding.txvOverviewDetail;
        mTxvRating = binding.txvRatingDetail;
        mTxvVotes = binding.txvVotesDetail;
        mTxvVideos = binding.txvVideos;
        mLlVideos = binding.llVideos;
        mRgReviewButtons = binding.rgReviewButtons;
        mTxvReviewDisplay = binding.txvReviewDisplay;
        mBtnFavourite = binding.btnFavourite;

        // Set listener to favourite button to add/remove to/from favourites.
        mBtnFavourite.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                favourite(mIsFavourite = isItFavourite());
            }
        });
        mBtnFavourite.setText(Html.fromHtml(Util.STAR_SYMBOL));

        // Return inflated root view.
        return binding.getRoot();

    } // End onCreateView method


    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        // Store instance of MainActivity for calling its methods and fields and passing as context.
        mActivity = getActivity();
        mApplication = (PopularMovies) mActivity.getApplication();

        // Init display metrics.
        Display display = mActivity.getWindowManager().getDefaultDisplay();
        DisplayMetrics mDisplayMetrics = new DisplayMetrics();
        display.getMetrics(mDisplayMetrics);
        mApplication.setDisplayMetrics(mDisplayMetrics);
        // Get and store orientation.
        mApplication.setLandscape(
                getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE);

        // Set mode (twoPane) and fragment widths:
        if (mActivity instanceof MainActivity) {
            // If two-pane tablet mode, set it to true.
            mApplication.setTwoPane(true);
        } else {
            mApplication.setTwoPane(false);
        }
        mApplication.setFragmentWidths();

        if (savedInstanceState == null) {
            // If DetailsFragment is freshly created, get the JSON string passed via bundle from PosterAdapter.
            mJsonString = getArguments().getString(MoviesContract.COLUMN_DATA_JSON);
        } else {
            // On rotation, preserve state from saved.
            mNewMovie = false;
            mShownReviewIndex = savedInstanceState.getInt(Util.PARAM_SHOWN_REVIEW, -1);
            mScrollPreserve = savedInstanceState.getInt(Util.PARAM_DETAIL_SCROLL, 0);
            mJsonString = savedInstanceState.getString(MoviesContract.COLUMN_DATA_JSON);
        }
        // show movie clicked in MainFragment RecyclerView.
        loadJsonString(mJsonString);
    }


    // Save state on rotation.
    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(MoviesContract.COLUMN_DATA_JSON, mJsonString);
        outState.putInt(Util.PARAM_SHOWN_REVIEW, mShownReviewIndex);
        outState.putInt(Util.PARAM_DETAIL_SCROLL, mScvDetails.getScrollY());
    }


    @Override
    public void onResume() {
        super.onResume();
        // Change main title in title bar to indicate showing movie details, but only for phones.
        // For tablet, don't change to keep 'Popular Movies' as title always.
        if (!mApplication.isTwoPane()) {
            getActivity().setTitle(getString(R.string.title_details));
        }
    }


    // Add share menu item for sharing first video functionality.
    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);

        // Inflate menu_fragment_detail containing menu item 'share'.
        inflater.inflate(R.menu.menu_fragment_detail, menu);

        // Create ShareActionProvider for share menu option.
        MenuItem shareMenuItem = menu.findItem(R.id.action_share_movie);
        mShareActionProvider = (ShareActionProvider) MenuItemCompat.getActionProvider(shareMenuItem);

        // Set intent for created ShareActionProvider
        if (mShareActionProvider != null && mVideos != null && mVideos.length > 0) {
            mShareActionProvider.setShareIntent(createShareIntent());
        }

    }


    // Create and return share intent to share first video.
    private Intent createShareIntent() {
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.setType("text/plain");
        // Share the url of the first video.
        String extra = YOUTUBE_BASE_URL_STRING + mVideos[0] + "\n";
        // Include also the video title.
        if (mTitle != null) { extra = mTitle + "\n\n" + extra; }
        intent.putExtra(Intent.EXTRA_TEXT, extra);
        intent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.share_movie_subject));
        return intent;
    }


    // Extract passed in JSON string into custom Movie object for holding and display movie details.
    private void loadJsonString(String jsonString) {

        // If ill passed JSON string, notify user that details are unavailable and return.
        if (jsonString == null) {
            mTxvTitle.setText(getString(R.string.error_details_missing));
            return;
        }
        // Create custom movie object for holding and formatting movie data.
        mMovie = new Movie(jsonString);
        // Store details needed in other methods in member variables
        mTmdbId = mMovie.getTmdbId();
        mTitle = mMovie.getTitle();
        // If main details are unavailable, notify user and return.
        if (mTitle == null && mMovie.getPoster() == null) {
            mTxvTitle.setText(getString(R.string.error_details_missing));
            return;
        }

        mLlDetailFragmentLayout.setMinimumHeight(mApplication.getScreenHeight());

        // Determine if movie is in favourites and set favourite start button colour accordingly.
        // Also store this info for later referring to info if movie is favourite or not.
        // isItFavourite() will set reviewsJson in mMovie object as well.
        changeFavColor(mIsFavourite = isItFavourite());
        // fetch and display videos.
        fetchVideosOrReviews(Util.IDENTIFIER_VIDEOS);

        if (mIsFavourite && mMovie.getReviewsJson() != null) {
            // If movie is in favourites and has data in column reviews_json, reviewsJson might have
            // been set in called isItFavourite() method. If so just show reviews.
            populateReviews(mMovie.getReviewsJson());
        } else {
            // If reviewsJson is unavailable (if movie is not favourite or if it was saved to
            // favourites while offline), fetch it and then show reviews.
            fetchVideosOrReviews(Util.IDENTIFIER_REVIEWS);
        }

        // Show backdrop image using glide. This method is complex and has several options for showing
        // image (first try from file if movie is favourite, then from url if it's not and online, then
        // display title as image if all else failed).
        showImageWithGlide();

        // Show title, release date and overview.
        // No need to check three bellow for null value as it will just leave TextView 'blank' if so.
        mTxvTitle.setText(mTitle);
        mTxvReleaseDate.setText(mMovie.getReleaseDate());
        mTxvOverview.setText(mMovie.getOverview());

        // Get rating and number of votes.
        double tmdbRating = mMovie.getRating();
        int tmdbVoteCount = mMovie.getVotes();

        // Format and display rating. Color it to red if bad, magenta if ok and green if good,
        // - classified by the specified rating limit constants.
        if (tmdbRating >= 0) {
            mTxvRating.setText(String.format(Locale.US, "%.1f", tmdbRating));
            // Color rating in different color depending how high it is:
            if (tmdbRating > Util.RATING_LIMIT_GOOD) {
                // if rating is good (above limit defined for 'good'), display it in green,
                mTxvRating.setTextColor(Color.GREEN);
            } else {
                if (tmdbRating > Util.RATING_LIMIT_OK) {
                    // if rating is ok (above limit defined for 'ok'), display it in magenta,
                    mTxvRating.setTextColor(Color.MAGENTA);
                } else {
                    // if rating is below ok it's 'bad', display it in red.
                    mTxvRating.setTextColor(Color.RED);
                }
            }
        } else {
            // If rating is -1, Movie object was ill constructed and data wasn't properly set.
            mTxvRating.setText("");
        }

        // Format and display number of votes.
        if (tmdbVoteCount > 0) {
            mTxvVotes.setText(String.format(Locale.US, "%1d %2s", tmdbVoteCount, getString(R.string.votes)));
        } else {
            // If rating is -1, Movie object was ill constructed and data wasn't properly set
            mTxvVotes.setText("");
        }

        // If onCreateOptionsMenu has already happened, update the share intent (if there is something to share).
        if (mShareActionProvider != null && mVideos != null && mVideos.length > 0) {
            mShareActionProvider.setShareIntent(createShareIntent());
        }

    } // End loadJsonString


    /**
     * Create and display buttons for showing videos.
     *
     * @param videosJsonString - fetched JSON string containing videos info to extract.
     */
    private void populateVideos(String videosJsonString) {

        // Get JsonArray of videos from passed JSON string.
        JSONArray videosJsonArray = Util.getJsonArrayResults(videosJsonString);
        if (videosJsonArray == null) { return; }

        // Constant representing what is to be extracted from JSON.
        final String TMDB_VIDEO_KEY = "key";
        // Determine how many videos to display (specified as constant in Util).
        int videosCount = Util.getLimitedJsonArrayCount(videosJsonArray, Util.MAX_VIDEOS);

        // If there are any videos:
        if (videosCount > 0) {

            // For tablet, reset views from previously shown movie
            mLlVideos.removeAllViews();
            mTxvVideos.setText("");

            // Only display word videos (bellow buttons) if there are any videos and relating buttons.
            mTxvVideos.setText(getString(R.string.videos));
            // For holding videos keys.
            mVideos = new String[videosCount];
            // Create buttons array for storing created buttons.
            AppCompatButton[] acbVideos = new AppCompatButton[videosCount];


            // For each video in videosJsonArray up to videosCount limit:
            for (int i = 0; i < videosCount; i++) {
                String videoKey;
                try {
                    // Get the JSON object representing each video.
                    JSONObject videosJsonArrayJSONObjectJsonObject = videosJsonArray.getJSONObject(i);
                    // Get key for that video (forms end of the url)
                    videoKey = videosJsonArrayJSONObjectJsonObject.getString(TMDB_VIDEO_KEY);
                } catch (JSONException e) {
                    continue;
                }
                // Store video key.
                mVideos[i] = videoKey;
                // If video button not already added, create and add it.
                if (mLlVideos.findViewById(i) == null) {
                    // Name video buttons (its text) with numbering.
                    String videoButtonText = "" + (i + 1);
                    // Create new button and store into button array.
                    acbVideos[i] = new AppCompatButton(mActivity);
                    // Set id to index to be able to refer to it to check if it was already created.
                    acbVideos[i].setId(i);
                    // Set button text, text size, background color, look, margins and layout params.
                    acbVideos[i].setText(videoButtonText);
                    acbVideos[i].setTextSize(TypedValue.COMPLEX_UNIT_PX,
                            mApplication.getResources().getDimension(R.dimen.text_size_m)
                    );
                    acbVideos[i].setBackgroundTintList(ColorStateList.valueOf(
                            ContextCompat.getColor(mApplication, R.color.colorAccent)));
                    acbVideos[i].setCompoundDrawablesWithIntrinsicBounds(
                            ContextCompat.getDrawable(mApplication, android.R.drawable.ic_media_play), null, null, null);
                    LinearLayout.LayoutParams llParams = Util.getLlParams(0);
                    int margin;
                    if (mApplication.isLandscape() && !mApplication.isTwoPane()) {
                        margin = (int) getResources().getDimension(R.dimen.gap_m);
                    } else {
                        margin = (int) getResources().getDimension(R.dimen.gap_s);
                    }
                    llParams.setMargins(margin, margin, margin, margin/10);
                    acbVideos[i].setLayoutParams(llParams);
                    acbVideos[i].setTypeface(Typeface.defaultFromStyle(Typeface.BOLD));
                    // Set button onClickListener to show video when clicked.
                    acbVideos[i].setOnClickListener(showVideoListener);
                    // Add the button to display.
                    mLlVideos.addView(acbVideos[i]);
                }
            } // End for each video.

        } // End if there are any videos

    } // End populateVideos method


    private void populateReviews(String reviewsJsonString) {

        // Get JsonArray of reviews from passed JSON string.
        JSONArray reviewsJsonArray = Util.getJsonArrayResults(reviewsJsonString);
        if (reviewsJsonArray == null) { return; }

        // Constants representing what is to be extracted from JSON.
        final String TMDB_REVIEW_AUTHOR = "author";
        final String TMDB_REVIEW_CONTENT = "content";

        // Determine how many reviews to display (specified as constant in Util).
        int reviewsCount = Util.getLimitedJsonArrayCount(reviewsJsonArray, Util.MAX_REVIEWS);

        // If there are any reviews:
        if (reviewsCount > 0) {

            // For tablet, reset views from previously shown movie
            mRgReviewButtons.removeAllViews();
            mTxvReviewDisplay.setText("");

            // For holding reviews.
            mReviews = new String[reviewsCount];
            // For holding review buttons to be able to refer to them to determine which one is shown.
            mRbReviews = new RadioButton[reviewsCount];

            // For each review in reviewsJsonArray up to reviewsCount limit:
            for (int i = 0; i < reviewsCount; i++) {
                String author;
                String review;
                try {
                    // Get the JSON object representing each review.
                    JSONObject reviewJsonObject = reviewsJsonArray.getJSONObject(i);
                    // Get author and review.
                    author = reviewJsonObject.getString(TMDB_REVIEW_AUTHOR);
                    review = reviewJsonObject.getString(TMDB_REVIEW_CONTENT);
                } catch (JSONException e) { continue; }

                // Append author to it's review and store as one.
                mReviews[i] = author + "\n\n" + review;

                // If review button not already added, create and add it.
                if (mRgReviewButtons.findViewWithTag(i) == null) {
                    // Name review buttons (its text) with 'Review ' + numbering.
                    String reviewButtonText = getString(R.string.review) + " " + (i + 1);
                    // Create new button and store into button array.
                    mRbReviews[i] = new RadioButton(mActivity);
                    // Set id to index to be able to refer to it to check if it was already created.
                    mRbReviews[i].setId(i);
                    // Set button text, text size, background, look, padding, layout params and width.
                    mRbReviews[i].setText(reviewButtonText);
                    mRbReviews[i].setTextSize(TypedValue.COMPLEX_UNIT_PX,
                            mApplication.getResources().getDimension(R.dimen.text_size_m)
                    );
                    mRbReviews[i].setButtonDrawable(new StateListDrawable());
                    mRbReviews[i].setBackgroundColor(ContextCompat.getColor(mApplication, R.color.colorGray));
                    int paddingTopBottom = (int) mApplication.getResources().getDimension(R.dimen.spinner_ver_padding);
                    mRbReviews[i].setPadding(0, paddingTopBottom, 0, paddingTopBottom);
                    mRbReviews[i].setGravity(Gravity.CENTER);
                    mRbReviews[i].setLayoutParams(Util.getLlParams(1.0f));
                    mRbReviews[i].setWidth(mApplication.getFragDetailsWidth() / reviewsCount);
                    // Set button onClickListener to show review when clicked.
                    mRbReviews[i].setOnClickListener(showReviewsListener);
                    // Add the button to display.
                    mRgReviewButtons.addView(mRbReviews[i]);
                }
            } // End for each review.

            // If review was shown, re-show it (i.e. on rotation)
            if (mShownReviewIndex > -1 && mRbReviews.length > 0) {
                int shownReviewIndex = mShownReviewIndex;
                mShownReviewIndex = -1;
                showReview(mRbReviews[shownReviewIndex]);
            }

        } // End if there are any reviews

        // Reset scroll to 0 for new movie or restore scroll on rotation.
        if (mNewMovie) {
            mScvDetails.post(new Runnable() {
                @Override
                public void run() {mScvDetails.scrollTo(0, 0);}
            });
        } else {
            mScvDetails.post(new Runnable() {
                @Override
                public void run() {mScvDetails.scrollTo(0, mScrollPreserve);}
            });
            mNewMovie = false;
        }

    } // End populateReviews method


    // When video is clicked show it in Youtube app or if not installed then in a browser.
    private void showVideo(View view) {

        // Index is stored in views id.
        int videoIndex = view.getId();

        // Intent for showing video in Youtube app
        Intent appIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("vnd.youtube:" + mVideos[videoIndex]));
        // Intent for showing video in a browser.
        Intent webIntent = new Intent(Intent.ACTION_VIEW,
                Uri.parse(YOUTUBE_BASE_URL_STRING + mVideos[videoIndex]));

        // try to show video in a Youtube app.
        if (appIntent.resolveActivity(getContext().getPackageManager()) != null) {
            startActivity(appIntent);
        } else {
            // If Youtube app is not installed on the device, try to show video in a browser.
            if (webIntent.resolveActivity(getContext().getPackageManager()) != null) {
                startActivity(webIntent);
            } else {
                // If also no browser is installed, notify user that action cannot be performed, and to install.
                Toast.makeText(mActivity, getString(R.string.no_video_app), Toast.LENGTH_SHORT).show();
            }
        }

    } // End showVideo method.


    // Used to store index of the shown review to un-show it if another is clicked or to re-show it on rotation.
    private int mShownReviewIndex = -1;


    // Show clicked review and hide previously shown review if any. If clicked on the shown review, hide it.
    private void showReview(View view) {

        // Index is stored in views id.
        int reviewIndex = view.getId();

        if (mShownReviewIndex == reviewIndex) {
            // If clicked on the review that is showing set to gray, hide it and set shown index to -1.
            mRgReviewButtons.clearCheck();
            mTxvReviewDisplay.setText("");
            view.setBackgroundColor(ContextCompat.getColor(mApplication, R.color.colorGray));
            mShownReviewIndex = -1;
        } else {
            // if clicked on a review that is not showing:
            if (mShownReviewIndex > -1) {
                // If another review was previously showing, set that review radio-button to gray.
                mRbReviews[mShownReviewIndex].setBackgroundColor(ContextCompat.getColor(mApplication, R.color.colorGray));
            }
            // Set currently chosen review radio-button to accent.
            view.setBackgroundColor(ContextCompat.getColor(mApplication, R.color.colorAccent));
            // Show clicked review and set shown review index to its index.
            mTxvReviewDisplay.setText(mReviews[reviewIndex]);
            mShownReviewIndex = reviewIndex;
        }

    } // End showReview method.


    // Listeners fow clicking on a video or review.
    private final View.OnClickListener showVideoListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            showVideo(view);
        }
    };
    private final View.OnClickListener showReviewsListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            showReview(view);
        }
    };

    // AsyncResponse for fetching videos or reviews task.
    // If used to fetch videos, just populate videos with fetched JSON string.
    // If used to fetch reviews, apart from populating reviews, if it is favourite also update fetched
    // reviews JSON string in db as it might not be written if it was put in db while offline.
    private final TaskVideosOrReviews.AsyncResponseVideosOrReviews mAsyncResponseVR =
            new TaskVideosOrReviews.AsyncResponseVideosOrReviews() {
                @Override
                public void processFinish(String[] strArReturn) {
                    if (mActivity == null) { return; }
                    // returned string array's size is 2. [0] - identifier; [1] - resulting JSON string.
                    if (strArReturn != null && strArReturn.length == 2) {
                        // To identify if fetched videos or reviews.
                        String identifier = strArReturn[0];
                        // Resulting fetched JSON string for videos or reviews.
                        String jsonArrayString = strArReturn[1];

                        if (identifier.equals(Util.IDENTIFIER_VIDEOS)) {
                            // If fetched videos, populate videos from fetched JSON.
                            populateVideos(jsonArrayString);
                        } else {
                            // If fetched reviews:
                            if (jsonArrayString != null) {
                                populateReviews(jsonArrayString);
                                // This method will also add reviews JSON to Movie ContentValues.
                                mMovie.setReviewsJson(jsonArrayString);
                                // If favourite, update reviews JSON in db.
                                if (mIsFavourite) {
                                    TaskFavourite taskFavourite = new TaskFavourite(mAsyncResponseFavourite,
                                            mApplication, mMovie.getContentValues(), mTmdbId);
                                    taskFavourite.execute();
                                }
                            }
                        }
                    }

                }
            };


    // Start a task to fetch videos or reviews depending on the passed in identifier.
    private void fetchVideosOrReviews(String identifier) {
        if (Util.isOnline(mApplication)) {
            TaskVideosOrReviews taskTaskVideosOrReviews = new TaskVideosOrReviews(mAsyncResponseVR);
            taskTaskVideosOrReviews.execute(identifier, mTmdbId + "");
        }
    }


    // Start AsyncTask to write favourite into db.
    private void favourite(Boolean isFavourite) {

        if (isFavourite == null) { return; }

        // Set color of the fav star button in both RecyclerView and DetailsFragment to correspond
        // to 'favourite' status. Do it at start to trick the user to feel that change is instantaneous,
        // and revert change on completion if unsuccessful.
        mApplication.posterAdapter.changeFavColor(mTmdbId, mMovie, isFavourite);
        if (isFavourite) {
            mBtnFavourite.setTextColor(Color.BLACK);
        } else {
            mBtnFavourite.setTextColor(Color.YELLOW);
        }

        // Start AsyncTask to write movie as favourite into db or delete it if it's already in table.
        TaskFavourite taskFavourite = new TaskFavourite(mAsyncResponseFavourite,
                mApplication, mMovie.getContentValues(), mTmdbId);
        taskFavourite.execute(isFavourite);

    } // End favourite method


    // AsyncResponse for write favourite to db task which can be used to write to or delete from db
    // (if isFavourite boolean is passed as argument) or to update movie - with reviews JSON
    // (if no argument is present). For resulting isFavourite - Insert and Update return true,
    // delete returns false and on error null is returned.
    private final TaskFavourite.AsyncResponseFavourite mAsyncResponseFavourite =
            new TaskFavourite.AsyncResponseFavourite() {
                @Override
                public void processFinish(Boolean isFavourite) {
                    if (mActivity == null) { return; }
                    if (isFavourite != null) {
                        // Save images if user chose to do so for favourites (in preferences).
                        // This will be done on insert or update. On insert images need to be saved
                        // for new favourite movie. On update retry, as they might have not ben saved
                        // if put to favourites while offline. Images are not deleted if movie is
                        // removed from favourites (when isFavourite == false) as user might click
                        // by mistake or might just play and click the button several times.
                        // These are 'garbage collected' on Activity 'cold' recreation.
                        if (mApplication.saveImages() && isFavourite) {
                            File fileDir = new File(Util.getInternalFilePath(mApplication));
                            File imgDirFile = new File(fileDir, mMovie.getPoster());
                            if (!imgDirFile.exists()) {
                                saveImages();
                            }
                        }
                    } else {
                        // if there was an error, revert changes made in favourite() method.
                        mIsFavourite = isItFavourite();
                        mApplication.posterAdapter.changeFavColor(mTmdbId, mMovie, mIsFavourite);
                        changeFavColor(mIsFavourite);
                    }
                }
            };


    // Look into db to see if movie is present in favourite_movies table. If yes also set mMovie
    // reviews JSON. mIsFavourite field is usually also set with the returned value.
    private Boolean isItFavourite() {
        Boolean isFavourite = null;
        Uri tableUri = MoviesContract.getTableUri(MoviesContract.TableMovies.TABLE_NAME);
        Cursor cursor = mApplication.getContentResolver().query(tableUri,
                new String[]{MoviesContract.COLUMN_TMDB_ID, MoviesContract.COLUMN_REVIEWS_JSON},
                MoviesContract.COLUMN_TMDB_ID + " = ?", new String[]{mTmdbId + ""}, null);
        if (cursor != null) {
            // If movie is favourite also set mMovie reviews JSON.
            if (isFavourite = cursor.moveToFirst()) {
                mMovie.setReviewsJson(cursor.getString(1));
            }
            cursor.close();
        }
        return isFavourite;
    }


    // Change fav button star color to correspond to passed isFavourite boolean. Also called from PosterAdapter.
    void changeFavColor(Boolean isFavourite) {
        if (isFavourite == null) { return; }
        if (isFavourite) {
            mBtnFavourite.setTextColor(Color.YELLOW);
        } else {
            mBtnFavourite.setTextColor(Color.BLACK);
        }
    }


    // Show backdrop image using glide. This method is complex and has several options for showing
    // image (first try from file if movie is favourite, then from url if it's not and online, then
    // display title as image if all else failed).
    // Also called from PosterAdapter to show title instead of image when going from online to
    // offline (and vice versa) if non favourite movie is shown or not saving images for favourites.
    void showImageWithGlide() {

        // Get full url strings
        final String backdropPathFull = Util.getFullImagePath(mMovie.getBackdrop(), mApplication.getBackdropWidth());
        final String posterPathFull = Util.getFullImagePath(mMovie.getPoster(), mApplication.getPosterWidth());


        // Prepare the placeholder size. Placeholder image is plain with same color as the background.
        GradientDrawable placeholder = new GradientDrawable();
        int backdropWidth = mApplication.getFragDetailsWidth();
        int backdropHeight = (int) (backdropWidth * mApplication.getBackdropRatio());
        placeholder.setShape(GradientDrawable.RECTANGLE);
        placeholder.setColor(ContextCompat.getColor(mApplication, R.color.colorGray));
        placeholder.setSize(backdropWidth, backdropHeight);

        // Add backdrop image using Glide

        // If move is in favourites use the backdrop image file if saved
        if (mApplication.saveImages() && mIsFavourite) {
            // Try to get backdrop image file.
            File backdropFile = Util.getFile(Util.getInternalFilePath(mApplication), mMovie.getBackdrop());
            // If successfully got backdrop image file show it using Glide.
            if (backdropFile != null) {
                Glide.with(mImvPoster.getContext()).load(backdropFile)
                        .override(backdropWidth, backdropHeight).placeholder(placeholder)
                        .into(mImvPoster);
            } else {
                // // If unsuccessful with backdrop image file and online, get backdrop image from url
                if (Util.isOnline(mApplication)) {
                    glideImageFromUrl(backdropPathFull, posterPathFull, backdropWidth, backdropHeight, placeholder);
                    saveImages();
                } else {
                    // If unsuccessful with backdrop image file and offline, try to get poster image file.
                    File posterFile = Util.getFile(Util.getInternalFilePath(mApplication), mMovie.getPoster());
                    if (posterFile != null) {
                        // If successfully got poster image file show it using Glide.
                        Glide.with(mImvPoster.getContext()).load(posterFile)
                                .override(backdropWidth, backdropHeight).placeholder(placeholder)
                                .into(mImvPoster);
                    } else {
                        // If unsuccessful with poster image file, show title as image if available.
                        noImage();
                    }
                }
            }
        } else {
            // if not saving favourite images:
            if (Util.isOnline(mApplication)) {
                // if online, load image from url.
                glideImageFromUrl(backdropPathFull, posterPathFull, backdropWidth, backdropHeight, placeholder);
            } else {
                // If offline load title as image if it's available.
                noImage();
            }
        }
    } // End showImageWithGlide method.


    // Load image from url using Glide
    private void glideImageFromUrl(final String backdropPathFull, final String posterPathFull
            , final int backdropWidth, final int backdropHeight, final GradientDrawable placeholder) {

        Glide.with(mImvPoster.getContext()).load(backdropPathFull)
                .override(backdropWidth, backdropHeight).placeholder(placeholder)
                // If cannot display backdrop from url, try displaying poster from url.
                .listener(new RequestListener<Drawable>() {
                    @Override
                    public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
                        Glide.with(mImvPoster.getContext()).load(posterPathFull)
                                .override(backdropWidth, backdropHeight).placeholder(placeholder)
                                // If cannot display poster from url, display title as image.
                                .listener(new RequestListener<Drawable>() {
                                    @Override
                                    public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
                                        noImage();
                                        return false;
                                    }

                                    @Override
                                    public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                                        return false;
                                    }
                                }).into(mImvPoster); // Load poster if successfully fetched it from url.
                        return true;
                    }

                    @Override
                    public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                        return false;
                    }
                }).into(mImvPoster); // Load backdrop if successfully fetched it from url.

    }


    // If title is available, set title as backdrop image.
    private void noImage() {
        mFlPosterAndRating.setBackgroundColor(ContextCompat.getColor(mActivity, R.color.colorTitleBgd));
    }


    // Call ServiceSaveImages to fetch and save poster and backdrop for shown movie (when added to favourites).
    private void saveImages() {
        if (mApplication.saveImages()) {
            Intent intent = new Intent(mActivity, ServiceSaveImages.class);
            intent.putExtra(Util.PARAM_POSTER_NAME, mMovie.getPoster());
            intent.putExtra(Util.PARAM_BACKDROP_NAME, mMovie.getBackdrop());
            intent.putExtra(Util.PARAM_POSTER_WIDTH, mApplication.getPosterWidth());
            intent.putExtra(Util.PARAM_BACKDROP_WIDTH, mApplication.getBackdropWidth());
            mActivity.startService(intent);
        }
    }


    // Used in tablet mode to display a new move when clicked (as DF is not recreated)
    public void setJsonString(String jsonString) {
        // Reset stored index if a review was shown for the previous movie
        mShownReviewIndex = -1;
        // Indicate that new movie needs to be shown
        mNewMovie = true;
        // Save passed in JSON string for persisting state on rotation.
        mJsonString = jsonString;
        // show movie clicked in MainFragment RecyclerView.
        loadJsonString(jsonString);
    }


    // Used in tabled in PosterAdapter to determine if clicked movie is the same as movie shown in DF.
    int getTmdbId() { return mTmdbId; }


} // End DetailsFragment class
