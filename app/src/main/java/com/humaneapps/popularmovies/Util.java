/*
 * Copyright (C) 2017 Vladimir Markovic
 */

package com.humaneapps.popularmovies;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.OkUrlFactory;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;


/**
 * Common constants and code.
 */

public class Util {

    // Acceptable poster and backdrop widths (and heights) to specify for method getFullPosterUrl(int posterSize)
    // packed in an array for offsetting quality for user preference.
    public static final Integer[] POSTER_WIDTHS = {154, 185, 300, 342, 500, 780, 1000, 1280, 1920};
    public static final Integer[] BACKDROP_WIDTHS = {154, 184, 300, 342, 500, 780, 1000, 1280, 1920};
    static final Integer[] POSTER_HEIGHTS = {231, 277, 450, 513, 750, 1170, 1500, 1920, 2844};
    static final Integer[] BACKDROP_HEIGHTS = {87, 104, 169, 192, 281, 439, 563, 720, 1080};
    // Indices for default poster and backdrop widths.
    public static final int DEFAULT_POSTER_INDEX = 2;
    public static final int DEFAULT_BACKDROP_INDEX = 4;

    // Groups as present in the 'grouping' column in the db (can be any combination space separated)
    // put in an array. i.e. 'popular favourite' or 'top_rated favourite'
    // or 'popular top_rated favourite' or just 'popular'
    public static final String[] GROUPS = new String[]{"popular", "top_rated", "favourite"};

    // Spinner options.
    static final int SPINNER_INDEX_POPULAR = 0;
    static final int SPINNER_INDEX_TOP_RATED = 1;
    static final int SPINNER_INDEX_FAVOURITE = 2;

    // Movies are split on good (green), ok (magenta) and bad (red) according to rating.
    // The two constants bellow define the cutoff points.
    static final int RATING_LIMIT_GOOD = 7;
    static final int RATING_LIMIT_OK = 5;

    // Passing parameters (for intents and bundles).
    public static final String PARAM_SPINNER_INDEX = "spinner_index";
    static final String PARAM_SCROLL_POPULAR = "popular_scroll";
    static final String PARAM_SCROLL_TOP_RATED = "top_rated_scroll";
    static final String PARAM_SCROLL_FAVOURITE = "favourite_scroll";
    public static final String PARAM_PAGE = "page_number";
    static final String PARAM_PAGE_POPULAR = "page_popular";
    static final String PARAM_PAGE_TOP_RATED = "page_top_rated";
    static final String PARAM_POPULAR_JSONS = "popular_jsons";
    static final String PARAM_TOP_RATED_JSONS = "top_rated_jsons";
    static final String PARAM_DETAIL_SCROLL = "detail_scroll";
    static final String PARAM_SHOWN_REVIEW = "shown_review";
    public static final String PARAM_POSTER_NAME = "poster_name";
    public static final String PARAM_BACKDROP_NAME = "backdrop_name";
    public static final String PARAM_POSTER_WIDTH = "poster_width";
    public static final String PARAM_BACKDROP_WIDTH = "backdrop_width";
    public static final String PARAM_SERVICE_BROADCAST = "service_broadcast";

    // These are not arbitrary as they are present in Json strings as such. I.e. "videos".
    // cannot be substituted for "trailers".
    static final String IDENTIFIER_VIDEOS = "videos";
    static final String IDENTIFIER_REVIEWS = "reviews";

    // Maximum number of videos / reviews to display.
    static final int MAX_VIDEOS = 3;
    static final int MAX_REVIEWS = 3;

    // Parts used to construct full poster url.
    private static final String IMAGE_SCHEME = "http";
    private static final String IMAGE_AUTHORITY = "image.tmdb.org";

    // Used in a provider as a flag whether to notify notifyChange or not.
    public static final String DO_NOT_NOTIFY_CHANGE = "do_not_notify_change";

    // Star symbol representing favourite movie (yellow when favourite, black when not).
    static final String STAR_SYMBOL = "&#10029;";


    // Check for internet connection.
    static boolean isOnline(Context context) {
        ConnectivityManager cm =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();
        return netInfo != null && netInfo.isConnectedOrConnecting();
    }


    // Populate and customize spinner text size and color and padding.
    static void populateAndCustomizeSpinner(Spinner spinner, String[] array, Context context,
                                            int itemLayout, int dropDownItemLayout,
                                            int textSizeId, int textColorId,
                                            final int topBottom, final int leftRight) {
        final float textSize = context.getResources().getDimension(textSizeId);
        final int textColor = ContextCompat.getColor(context, textColorId);
        ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(context, itemLayout, array) {
            @Override
            @NonNull
            public View getView(int position, View convertView, @NonNull ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                TextView textView = view.findViewById(android.R.id.text1);
                textView.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize);
                textView.setTextColor(textColor);
                textView.setPadding(leftRight, topBottom, leftRight, topBottom);
                textView.setSingleLine(false);
                return view;
            }
        };
        arrayAdapter.setDropDownViewResource(dropDownItemLayout);
        spinner.setAdapter(arrayAdapter);
    }


    // Used in DetailsFragment for populating videos and reviews.
    static LinearLayout.LayoutParams getLlParams(float weight) {
        return new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT, weight);
    }


    // Construct the image string path as for themoviedb.org for given width.
    static String getFullImagePath(String imageName, int imageWidth) {
        return new Uri.Builder().scheme(IMAGE_SCHEME)
                .authority(IMAGE_AUTHORITY)
                .appendPath("t")
                .appendPath("p")
                .appendPath("w" + imageWidth)
                .appendPath(imageName).build().toString();
    }


    // Construct the image URL for themoviedb.org for given width.
    public static URL getImageUrl(String imageName, int imageWidth) {
        URL url;
        try {
            url = new URL(getFullImagePath(imageName, imageWidth));
        } catch (MalformedURLException e) {
            url = null;
        }
        return url;
    }


    // Extract JSONArray from given jason string for match phrase 'results'.
    static JSONArray getJsonArrayResults(String jsonString) {
        return getJsonArray(jsonString, TMDB_RESULTS);
    }


    // Extract JSONArray from given jason string using given match phrase.
    private static JSONArray getJsonArray(String jsonString, String match) {
        if (jsonString == null) { return null; }
        try {
            // Create JSON object from passed JSON string.
            JSONObject jsonObject = new JSONObject(jsonString);
            // Return JSON array
            return jsonObject.getJSONArray(match);
        } catch (JSONException e) { return null; }
    }


    // Used for limiting reviews and videos to maximum number defined in the above constants.
    static int getLimitedJsonArrayCount(JSONArray jsonArray, int limit) {
        if (jsonArray == null) { return 0; }
        int count = jsonArray.length();
        if (count > limit) { return limit; }
        return count;
    }


    // Phrase for top level extracting from JSON string.
    private static final String TMDB_RESULTS = "results";


    // Fetch and return formatted videos JSON string for given movie tmdb id.
    @Nullable
    static String fetchVideosJsonString(@NonNull String tmdbId, OkHttpClient okHttpClient) {
        return makeReturnJsonString(fetchVideosJsonArray(tmdbId, okHttpClient));
    }


    // Fetch and return formatted reviews JSON string for given movie tmdb id.
    @Nullable
    static String fetchReviewsJsonString(@NonNull String tmdbId, OkHttpClient okHttpClient) {
        return makeReturnJsonString(fetchReviewsJsonArray(tmdbId, okHttpClient));
    }


    // Add top level phrase packed in JSON object to JSON array, forming returning JSON string.
    @Nullable
    private static String makeReturnJsonString(JSONArray jsonArray) {
        try {
            JSONObject returnJsonObject = new JSONObject();
            returnJsonObject.put(TMDB_RESULTS, jsonArray);
            return returnJsonObject.toString();
        } catch (JSONException e) {return null;}
    }


    // Fetch videos JSON string and extract what's needed into returning JSONArray.
    @Nullable
    private static JSONArray fetchVideosJsonArray(@NonNull String tmdbId, OkHttpClient okHttpClient) {

        // These are the names of the JSON objects contained in the videos JSON string used for extraction.
        final String TMDB_VIDEO_WEBSITE = "site";
        final String TMDB_VIDEO_KEY = "key";
        final String TMDB_VIDEO_NAME = "name";
        final String TMDB_VIDEO_TYPE = "type";

        // Form url string for fetching.
        String urlString = makeUrlString(tmdbId, Util.IDENTIFIER_VIDEOS);
        // Get JSON String via http url connection using above url string.
        String jsonString = fetchJsonString(urlString, okHttpClient);
        // Get top level json array (of all videos for given movie).
        JSONArray videosJsonArray = getJsonArray(jsonString, TMDB_RESULTS);
        if (videosJsonArray == null) {return null;}
        // Get number of videos. If higher then determined limit, set it at the limit.
        int videosCount = Util.getLimitedJsonArrayCount(videosJsonArray, Util.MAX_VIDEOS);
        if (videosCount == 0) {return null;}

        try {
            // Define returning JSONArray.
            JSONArray returnJsonArray = new JSONArray();
            for (int i = 0; i < videosCount; i++) {
                JSONObject returnVideoJsonObject = new JSONObject();
                // Get the JSON object representing each video.
                JSONObject videoJsonObject = videosJsonArray.getJSONObject(i);
                String site = videoJsonObject.getString(TMDB_VIDEO_WEBSITE);
                // Store only YouTube videos.
                if ("YouTube".equals(site)) {
                    returnVideoJsonObject.put(TMDB_VIDEO_KEY, videoJsonObject.getString(TMDB_VIDEO_KEY));
                    returnVideoJsonObject.put(TMDB_VIDEO_NAME, videoJsonObject.getString(TMDB_VIDEO_NAME));
                    returnVideoJsonObject.put(TMDB_VIDEO_TYPE, videoJsonObject.getString(TMDB_VIDEO_TYPE));
                    returnJsonArray.put(i, returnVideoJsonObject);
                }
            }
            return returnJsonArray;

        } catch (JSONException e) {return null;}

    } // End fetchVideosJsonString method


    // Fetch reviews JSON string and extract what's needed into returning JSONArray.
    @Nullable
    private static JSONArray fetchReviewsJsonArray(@NonNull String tmdbId, OkHttpClient okHttpClient) {

        // These are the names of the JSON objects contained in the reviews JSON string used for extraction.
        final String TMDB_REVIEW_AUTHOR = "author";
        final String TMDB_REVIEW_CONTENT = "content";

        String urlString = makeUrlString(tmdbId, Util.IDENTIFIER_REVIEWS);
        // Get Json String via http url connection using above url string
        String jsonString = fetchJsonString(urlString, okHttpClient);
        // Get top level json array (of all reviews for given movie).
        JSONArray reviewsJsonArray = getJsonArray(jsonString, TMDB_RESULTS);
        if (reviewsJsonArray == null) {return null;}
        // Get number of reviews. If higher then determined limit, set it at the limit.
        int reviewsCount = Util.getLimitedJsonArrayCount(reviewsJsonArray, Util.MAX_REVIEWS);
        if (reviewsCount == 0) {return null;}

        try {
            // Define returning JSONArray.
            JSONArray returnJsonArray = new JSONArray();
            for (int i = 0; i < reviewsCount; i++) {
                JSONObject returnReviewJsonObject = new JSONObject();
                // Get the JSON object representing each review.
                JSONObject reviewJsonObject = reviewsJsonArray.getJSONObject(i);
                // Get review author and content and put into returning JSONArray.
                returnReviewJsonObject.put(TMDB_REVIEW_AUTHOR, reviewJsonObject.getString(TMDB_REVIEW_AUTHOR));
                returnReviewJsonObject.put(TMDB_REVIEW_CONTENT, reviewJsonObject.getString(TMDB_REVIEW_CONTENT));
                returnJsonArray.put(returnReviewJsonObject);
            }

            return returnJsonArray;

        } catch (JSONException e) {return null;}

    } // End fetchReviewsJsonString method.


    // Fetch JSON string from url (for videos or reviews and in ServiceFetch for movies popular or top_rated).
    @Nullable
    public static String fetchJsonString(String urlString, OkHttpClient okHttpClient) {

        BufferedReader bufferedReader;
        // For composing the input stream into a String. It will be a JSON string.
        StringBuilder strBuilder;
        try {
            URL url = new URL(urlString);
            // Open url connection
            HttpURLConnection urlConnection = new OkUrlFactory(okHttpClient).open(url);
            urlConnection.setRequestMethod("GET");
            urlConnection.connect();

            // Open input stream and use it to create buffered reader for reading input.
            InputStream inputStream = urlConnection.getInputStream();
            bufferedReader = new BufferedReader(new InputStreamReader(inputStream));

            // For composing the input stream into a String. It will be a JSON string.
            strBuilder = new StringBuilder();
            strBuilder.append("");
            // Read every line of the input JSON string.
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                // Add a newline to make it easier for reading JSON string.
                line += "\n";
                strBuilder.append(line);
            }
            // If stream was empty there is no point in parsing so return null.
            if (strBuilder.length() == 0) {return null;}
            // Close url connection, buffered reader and its input stream.
            urlConnection.disconnect();

        } catch (IOException | NullPointerException e) {return null;}

        try {
            bufferedReader.close();
        } catch (IOException e) {
            Log.e(" fetchJsonString", "Error closing stream: " + e);
        }

        return strBuilder.toString();

    } // End fetchJsonString method


    // Extracts movies data from JSON string into ArrayList of Movie objects - used in PosterAdapter.
    static ArrayList<Movie> extractMovieDataFromJson(String jsonString) {

        if (jsonString == null) {return null;}

        // Returning ArrayList of Movie objects.
        ArrayList<Movie> movieArrayList = new ArrayList<>();

        // For skipping adult movies.
        final String TMDB_ADULT = "adult";

        JSONObject movieJsonObject;
        JSONArray moviesArray;

        try {
            // Create JSON object from passed JSON string.
            JSONObject movieListJsonObject = new JSONObject(jsonString);
            // Extract JSONArray of movies ('results' tag)
            moviesArray = movieListJsonObject.getJSONArray(TMDB_RESULTS);
            // If unable to extract movie data return without updating. There will be a retry
            // from MainFragment using mDateStoredData to check when was the last update.
        } catch (JSONException e) {return null;}

        for (int i = 0; i < moviesArray.length(); i++) {

            try {
                // Get the JSON object representing each movie.
                movieJsonObject = moviesArray.getJSONObject(i);
                // Skip adult movies.
                if (movieJsonObject.getBoolean(TMDB_ADULT)) { continue; }

            } catch (JSONException e) { continue; }

            // Construct a Movie object from extracted JSONObject.
            Movie movie = new Movie(movieJsonObject);
            // Add this Movie object to returning ArrayList.
            movieArrayList.add(movie);
        }

        return movieArrayList;

    } // End method extractMovieDataFromJson(String jsonString).


    // Make url for fetching JSON string for videos, reviews and in ServiceFetch for movies popular or top_rated.
    public static String makeUrlString(String... params) {

        // Parts used to construct full url.
        final String SCHEME_PARAM = "http";
        final String AUTHORITY_PARAM = "api.themoviedb.org";
        final String BASE_PATH_PARAM = "3/movie";
        final String API_KEY_PARAM = "api_key";

        // params size determines which code to execute.
        // From ServiceFetch 1st param is 'popular' or 'top_rated', 2nd is 'page' and 3rd is page number.
        // For videos/reviews 1st param is tmdbId and 2nd is 'reviews' or 'videos'
        // So params length 3 fetches movies popular/top rated and length 2 fetches reviews/videos.

        Uri.Builder uriBuilder = new Uri.Builder()
                .scheme(SCHEME_PARAM)
                .authority(AUTHORITY_PARAM)
                .appendEncodedPath(BASE_PATH_PARAM)
                // popular or top_rated if from ServiceFetch, or
                // <tmdbid> if from TaskVideosOrReviews
                .appendPath(params[0]);
        if (params.length == 3) {
            // page=<page>
            uriBuilder.appendQueryParameter(params[1], params[2]);
        } else if (params.length == 2) {
            // reviews or videos
            uriBuilder.appendPath(params[1]);
        }
        // api_key=<key>
        uriBuilder.appendQueryParameter(API_KEY_PARAM, BuildConfig.MY_MOVIE_DB_API_KEY);

        return uriBuilder.build().toString();

    } // End makeUrlString method.


    // Get file path to directory where to save images.
    public static String getInternalFilePath(Context context) {
        return context.getFilesDir() + "/img";
    }


    // Get file path to directory in external storage where to save images.
    @SuppressWarnings("unused")
    public static String getExternalFilePath(Context context) {
        String root = Environment.getExternalStorageDirectory().toString();
        String appName = context.getString(R.string.app_name);
        return root + "/" + appName + "/img";
    }


    // Try to get the passed file (name) in passed dir. Used getting image file (poster or backdrop)
    // if it exists. Return null if it doesn't.
    static File getFile(String dir, String fileName) {
        if (dir == null || fileName == null) { return null; }
        File file;
        try {
            file = new File(dir, fileName);
        } catch (NullPointerException | IllegalArgumentException e) {
            file = null;
        }
        if (file != null && file.exists()) {
            return file;
        } else {
            return null;
        }
    }


} // End Util class
