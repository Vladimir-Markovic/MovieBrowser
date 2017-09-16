/*
 * Copyright (C) 2017 Vladimir Markovic
 */

package com.humaneapps.popularmovies;


import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v7.preference.ListPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceFragmentCompat;
import android.support.v7.preference.PreferenceManager;
import android.support.v7.preference.SwitchPreferenceCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.humaneapps.popularmovies.service.ServiceDeleteImages;

/**
 * Displays and handles preferences using PreferenceFragment.
 */
public class SettingsFragment extends PreferenceFragmentCompat implements Preference.OnPreferenceChangeListener {


    // Required public constructor
    public SettingsFragment() {}


    // For storing instance of MainActivity (in onActivityCreated) for calling its public methods
    // and fields and passing as context.
    private MainActivity mMainActivity;
    private PopularMovies mApplication;

    // NOTE: I am learning more about performance at the moment. It is a question mark for me if what
    // I am doing with keeping instance of MainActivity for passing as context and calling its
    // methods is good. My guess is it's not the best practice. Also I am not sure I am releasing
    // resources everywhere where I should properly. I would appreciate if you would take the time
    // to comment on this in particular.

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        // Store context into mMainActivity for passing as context until activity is created.
        mMainActivity = (MainActivity) context;
    }


    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        // Store instance of MainActivity for calling its methods and fields and passing as context.
        mMainActivity = (MainActivity) getActivity();
        mApplication = (PopularMovies) getActivity().getApplication();
    }


    // Called during onCreate(Bundle) to supply the preferences for this fragment.
    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        // Inflate the layout for this fragment
        setPreferencesFromResource(R.xml.preferences, rootKey);
    }


    // Instantiate preferences, set their summary and listeners.
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate view.
        View view = super.onCreateView(inflater, container, savedInstanceState);
        if (view != null) { view.setBackgroundColor(Color.WHITE); }

        // Get poster size preference, set its summary and onPreferenceChange listener.
        ListPreference posterSizePreference = (ListPreference) getPreferenceManager()
                .findPreference(mMainActivity.getString(R.string.pref_poster_size_key));
        setPreferenceSummary(posterSizePreference);
        posterSizePreference.setOnPreferenceChangeListener(this);

        // Get image quality preference, set its summary and onPreferenceChange listener.
        ListPreference imageQualityPreference = (ListPreference) getPreferenceManager()
                .findPreference(mMainActivity.getString(R.string.pref_image_quality_key));
        setPreferenceSummary(imageQualityPreference);
        imageQualityPreference.setOnPreferenceChangeListener(this);

        // Get save images preference, set its summary and onPreferenceChange listener.
        SwitchPreferenceCompat saveImagesPreference = (SwitchPreferenceCompat) getPreferenceManager()
                .findPreference(mMainActivity.getString(R.string.pref_save_images_key));
        setPreferenceSummary(saveImagesPreference);
        saveImagesPreference.setOnPreferenceChangeListener(this);

        return view;
    }


    // Set title bar title to settings fragment title.
    @Override
    public void onResume() {
        super.onResume();
        getActivity().setTitle(mMainActivity.getString(R.string.title_settings));
    }


    // Overloaded to simplify method call for passing only preference as value we can get.
    // Still method with both preference and value as arguments is needed as they are both
    // returned by the onPreferenceChange method in which setPreferenceSummary is called.
    private void setPreferenceSummary(Preference preference) {
        setPreferenceSummary(preference, getPreferenceValueAsString(preference));
    }


    // Get preference value for the specified preference.
    private String getPreferenceValueAsString(Preference preference) {
        // On error - if passed preference is return without setting.
        if (preference == null) { return ""; }

        // Get the current preference value to return.
        String strPrefValue;
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(preference.getContext());
        if (preference instanceof SwitchPreferenceCompat) {
            strPrefValue = sp.getBoolean(preference.getKey(), true) + "";
        } else {
            strPrefValue = sp.getString(preference.getKey(), "");
        }

        return strPrefValue;
    } // End getPreferenceValueAsString


    /* Sets preference summary to specified value.
     * @param preference - preference for which to update summary.
     */
    private void setPreferenceSummary(Preference preference, String strPrefValue) {
        // On error - if passed preference is null return without setting
        if (preference == null) { return; }

        // Set summary for specified preference.
        if (preference instanceof ListPreference) {
            // For list preferences, look up the correct display value in
            // the preference's 'entries' list (since they have separate labels/values).
            ListPreference listPreference = (ListPreference) preference;
            int prefIndex = listPreference.findIndexOfValue(strPrefValue);
            if (prefIndex >= 0) {
                preference.setSummary(listPreference.getEntries()[prefIndex]);
            }
        } else {
            // For other preferences, set the summary to the value's simple string representation.
            preference.setSummary(strPrefValue);
        }

    } // End setPreferenceSummary method.


    /**
     * Deal with preference changes and update preference summary to new value after user changes it.
     *
     * @param preference - preference that changed.
     * @param value      - updated preference value.
     * @return true if all went well.
     */
    public boolean onPreferenceChange(Preference preference, Object value) {

        String key = preference.getKey();

        if (key.equals(mMainActivity.getString(R.string.pref_poster_size_key))) {
            // Poster size preference :
            // Set RecyclerView (column No) to new poster size and force it to redraw.
            int posterSize = Integer.parseInt(value.toString());
            mApplication.posterAdapter.setColumns(posterSize);
            mApplication.posterAdapter.refreshRecyclerView();
            mApplication.setRecyclerViewLayoutManager();
            mMainActivity.restore();

        } else if (key.equals(mMainActivity.getString(R.string.pref_image_quality_key))) {
            // Image quality preference:
            // Set image indices to reflect newly set quality in form of index offset.
            int imageQualityOffset = Integer.parseInt(value.toString());
            mApplication.setPosterAndBackdropIndices(imageQualityOffset);
            // Delete all saved images for current favourite movies - to be re-saved at new resolution.
            Intent deleteImagesIntent = new Intent(mMainActivity, ServiceDeleteImages.class);
            deleteImagesIntent.putExtra(getString(R.string.pref_save_images_key), false);
            mMainActivity.startService(deleteImagesIntent);
            // It online, re-fetch data to reflect image quality change.
            if (Util.isOnline(mMainActivity)) {
                mMainActivity.fetch(1);
            }
        } else if (key.equals(mMainActivity.getString(R.string.pref_save_images_key))) {
            // Save images preference:
            // Set boolean flag to reflect newly set value.
            mApplication.setSavingImages((boolean) value);
            // Force Recycler view to redraw to reflect if it needs to show images or titles.
            mApplication.posterAdapter.refreshRecyclerView();
            // If showing titles offline when save images was off the offline message was displayed.
            // Remove it when switching back to on remove that message.
            if (mMainActivity.showingFavourites()) { mMainActivity.removeMessage(); }
        }

        // Set summary for changed preference to reflect the change.
        setPreferenceSummary(preference, value.toString());
        return true;
    } // End method onPreferenceChange(Preference preference, Object value).


} // End SettingsFragment
