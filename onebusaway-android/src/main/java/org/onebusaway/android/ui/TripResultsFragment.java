/*
 * Copyright (C) 2012-2013 Paul Watts (paulcwatts@gmail.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.onebusaway.android.ui;

import com.google.android.ads.nativetemplates.TemplateView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.tabs.TabLayout;

import org.onebusaway.android.BuildConfig;
import org.onebusaway.android.R;
import org.onebusaway.android.app.Application;
import org.onebusaway.android.directions.model.Direction;
import org.onebusaway.android.directions.realtime.RealtimeService;
import org.onebusaway.android.directions.util.ConversionUtils;
import org.onebusaway.android.directions.util.CustomAddress;
import org.onebusaway.android.directions.util.DirectionExpandableListAdapter;
import org.onebusaway.android.directions.util.DirectionsGenerator;
import org.onebusaway.android.directions.util.FareUtils;
import org.onebusaway.android.directions.util.OTPConstants;
import org.onebusaway.android.directions.util.TripPlanAddresses;
import org.onebusaway.android.directions.util.TripRequestBuilder;
import org.onebusaway.android.map.MapParams;
import org.onebusaway.android.map.googlemapsv2.BaseMapFragment;
import org.onebusaway.android.provider.ObaContract;
import org.opentripplanner.api.model.Itinerary;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ExpandableListView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import androidx.annotation.DrawableRes;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import au.mymetro.android.ads.AdsManager;

public class TripResultsFragment extends Fragment {

    private static final String TAG = "TripResultsFragment";

    private static final int LIST_TAB_POSITION = 0;
    private static final int MAP_TAB_POSITION = 1;

    private View mDirectionsFrame;
    private BaseMapFragment mMapFragment;
    private ExpandableListView mDirectionsListView;
    private View mMapFragmentFrame;
    private FloatingActionButton mSaveTripPlanFab;
    private boolean mShowingMap = false;

    private RoutingOptionPicker[] mOptions = new RoutingOptionPicker[3];

    private Listener mListener;

    private Bundle mMapBundle = new Bundle();

    private Bundle mBuilderBundle;

    private AdsManager adsManager;

    /**
     * This listener is a helper for the parent activity to handle the sliding panel,
     * which interacts with sliding views (i.e., list view and map view) in subtle ways.
     */
    public interface Listener {

        /**
         * Called when the result views have been created
         *
         * @param containerView the view which contains the directions list and the map
         * @param listView the directions list view
         * @param mapView the map frame
         */
        void onResultViewCreated(View containerView, ListView listView, View mapView);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final View view = inflater.inflate(R.layout.fragment_trip_plan_results, container, false);

        mDirectionsFrame = view.findViewById(R.id.directionsFrame);
        mDirectionsListView = (ExpandableListView) view.findViewById(R.id.directionsListView);
        mMapFragmentFrame = view.findViewById(R.id.mapFragment);
        mSaveTripPlanFab = view.findViewById(R.id.btn_save_trip_plan);

        mBuilderBundle = getArguments();

        mOptions[0] = new RoutingOptionPicker(view, R.id.option1LinearLayout, R.id.option1Title, R.id.option1Duration, R.id.option1Interval, R.id.option1Fare);
        mOptions[1] = new RoutingOptionPicker(view, R.id.option2LinearLayout, R.id.option2Title, R.id.option2Duration, R.id.option2Interval, R.id.option2Fare);
        mOptions[2] = new RoutingOptionPicker(view, R.id.option3LinearLayout, R.id.option3Title, R.id.option3Duration, R.id.option3Interval, R.id.option3Fare);

        int rank = getArguments().getInt(OTPConstants.SELECTED_ITINERARY); // defaults to 0
        mShowingMap = getArguments().getBoolean(OTPConstants.SHOW_MAP);

        initInfoAndMap(rank);

        TabLayout tabLayout = (TabLayout) view.findViewById(R.id.tab_layout_switch_view);
        tabLayout.setOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                boolean show = (tab.getPosition() == MAP_TAB_POSITION);
                showMap(show);
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
                // unused
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
                // unused
            }
        });

        setTabDrawable(tabLayout.getTabAt(LIST_TAB_POSITION), R.drawable.ic_list);
        setTabDrawable(tabLayout.getTabAt(MAP_TAB_POSITION), R.drawable.ic_arrivals_styleb_action_map);

        if (mShowingMap) {
            tabLayout.getTabAt(MAP_TAB_POSITION).select();
        }

        if (mListener != null) {
            mListener.onResultViewCreated(mDirectionsFrame, mDirectionsListView, mMapFragmentFrame);
        }

        mSaveTripPlanFab.setOnClickListener(v -> {
            saveTrip();
        });

        if (BuildConfig.ENABLE_ADMOB) {
            adsManager = new AdsManager((AppCompatActivity) requireActivity());
            TemplateView template = view.findViewById(R.id.trip_result_ad_template);
            adsManager.loadNativeAd(template);
        }

        return view;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            NavHelp.goUp(getActivity());
            return true;
        }
        if (item.getItemId() == R.id.show_on_map) {
            showMap(true);
        }
        if (item.getItemId() == R.id.list) {
            showMap(false);
        }
        return false;
    }

    /**
     * Set the listener for this fragment.
     *
     * @param listener the new listener
     */
    public void setListener(Listener listener) {
        mListener = listener;
    }

    /**
     * Get whether map is showing.
     *
     * @return true if map is showing, false otherwise
     */
    public boolean isMapShowing() {
        return mShowingMap;
    }

    private void setTabDrawable(TabLayout.Tab tab, @DrawableRes int res) {
        View view = tab.getCustomView();
        TextView tv = ((TextView) view.findViewById(android.R.id.text1));

        Drawable drawable = getResources().getDrawable(res);

        int dp = (int) getResources().getDimension(R.dimen.trip_results_icon_size);
        drawable.setBounds(0, 0, dp, dp);

        drawable.setColorFilter(getResources().getColor(R.color.trip_option_icon_tint), PorterDuff.Mode.SRC_IN);

        tv.setCompoundDrawables(drawable, null, null, null);
    }

    private void initMap(int trip) {
        Itinerary itinerary = getItineraries().get(trip);
        mMapBundle.putString(MapParams.MODE, MapParams.MODE_DIRECTIONS);
        mMapBundle.putSerializable(MapParams.ITINERARY, itinerary);

        Intent intent = new Intent().putExtras(mMapBundle);
        getActivity().setIntent(intent);

        FragmentManager fm = getChildFragmentManager();
        if (mMapFragment == null) {
            // First check to see if an instance of BaseMapFragment already exists
            mMapFragment = (BaseMapFragment) fm.findFragmentByTag(BaseMapFragment.TAG);

            if (mMapFragment == null) {
                // No existing fragment was found, so create a new one
                Log.d(TAG, "Creating new BaseMapFragment");
                mMapFragment = BaseMapFragment.newInstance();
                fm.beginTransaction()
                        .add(R.id.mapFragment, mMapFragment, BaseMapFragment.TAG)
                        .commit();
            }
        }
    }

    private void showMap(boolean show) {

        mShowingMap = show;
        if (show) {
            mMapFragmentFrame.bringToFront();
            mMapFragment.setMapMode(MapParams.MODE_DIRECTIONS, mMapBundle);
        } else {
            mDirectionsListView.bringToFront();
        }

        getArguments().putBoolean(OTPConstants.SHOW_MAP, mShowingMap);
    }

    private void saveTrip() {
        CustomAddress mFromAddress = mBuilderBundle.getParcelable(TripRequestBuilder.FROM_ADDRESS);
        CustomAddress mToAddress = mBuilderBundle.getParcelable(TripRequestBuilder.TO_ADDRESS);
        if (mFromAddress != null && mToAddress != null) {
            final AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity());
            builder.setTitle(getString(R.string.trip_plan_save_dialog_title));
            builder.setMessage(getString(R.string.trip_plan_save_dialog_message));
            builder.setCancelable(true);

            // Set up the input
            final View dialogView = LayoutInflater.from(requireActivity()).inflate(R.layout.trip_planner_name_input, null);
            final EditText input = dialogView.findViewById(R.id.trip_planner_name_txt);
            input.requestFocus();
            builder.setView(dialogView);
            builder.setPositiveButton(getString(R.string.trip_plan_save_dialog_save_button), null);
            builder.setNegativeButton(getString(R.string.cancel), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.cancel();
                }
            });
            AlertDialog dialog = builder.show();
            Button button = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            button.setOnClickListener(view1 -> {
                String planName = input.getText().toString().trim();
                if(TextUtils.isEmpty(planName)){
                    input.setError(getString(R.string.trip_plan_save_dialog_empty_error));
                } else {
                    TripPlanAddresses tripPlan = new TripPlanAddresses();
                    tripPlan.setPlanName(planName);
                    tripPlan.setFromAddress(mFromAddress);
                    tripPlan.setToAddress(mToAddress);
                    ObaContract.TripPlans.insertOrUpdate(requireActivity(), tripPlan);
                    dialog.dismiss();
                    Toast.makeText(requireActivity(),
                            getResources().getString(R.string.trip_plan_save_dialog_saved_message),
                            Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void initInfoAndMap(int trip) {

        initMap(trip);

        for (int i = 0; i < mOptions.length; i++) {
            mOptions[i].setItinerary(i);
        }

        mOptions[trip].select();

        showMap(mShowingMap);
    }

    public void displayNewResults() {
        int rank = getArguments().getInt(OTPConstants.SELECTED_ITINERARY);
        showMap(mShowingMap);
        initInfoAndMap(rank);
    }

    private String toDateFmt(long ms) {
        Date d = new Date(ms);
        String s = new SimpleDateFormat(OTPConstants.TRIP_RESULTS_TIME_STRING_FORMAT_SUMMARY, Locale.getDefault()).format(d);
        return s.substring(0, 6).toLowerCase();
    }

    private String formatTimeString(String ms, double durationSec) {
        long start = Long.parseLong(ms);
        String fromString = toDateFmt(start);
        String toString = toDateFmt(start + (long) durationSec);
        return fromString + " - " + toString;
    }

    private List<Itinerary> getItineraries() {
        return (List<Itinerary>) getArguments().getSerializable(OTPConstants.ITINERARIES);
    }

    private class RoutingOptionPicker {
        LinearLayout linearLayout;
        TextView titleView;
        TextView durationView;
        TextView intervalView;
        TextView fareView;

        Itinerary itinerary;
        int rank;

        RoutingOptionPicker(View view, int linearLayout, int titleView, int durationView, int intervalView, int fareView) {
            this.linearLayout = (LinearLayout) view.findViewById(linearLayout);
            this.titleView = (TextView) view.findViewById(titleView);
            this.durationView = (TextView) view.findViewById(durationView);
            this.intervalView = (TextView) view.findViewById(intervalView);
            this.fareView = (TextView) view.findViewById(fareView);

            this.linearLayout.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    RoutingOptionPicker.this.select();
                }
            });
        }

        void select() {
            for (RoutingOptionPicker picker : mOptions) {
                picker.linearLayout.setBackgroundColor(getResources().getColor(R.color.trip_option_background));
            }
            linearLayout.setBackgroundResource(R.drawable.trip_option_selected_item);

            getArguments().putInt(OTPConstants.SELECTED_ITINERARY, rank);

            updateInfo();
            updateMap();
        }


        void setItinerary(int rank) {
            List<Itinerary> trips = getItineraries();
            if (rank >= trips.size()) {
                this.itinerary = null;
                linearLayout.setVisibility(View.GONE);
                return;
            }

            this.itinerary = trips.get(rank);
            this.rank = rank;

            String title = new DirectionsGenerator(itinerary.legs, getContext()).getItineraryTitle();
            String duration = ConversionUtils.getFormattedDurationTextNoSeconds(itinerary.duration, false, getContext());
            String interval = formatTimeString(itinerary.startTime, itinerary.duration * 1000);
            String fare = FareUtils.getFare(itinerary.fare);

            titleView.setText(title);
            durationView.setText(duration);
            intervalView.setText(interval);
            if (TextUtils.isEmpty(fare)) {
                fareView.setVisibility(View.GONE);
            } else {
                fareView.setVisibility(View.VISIBLE);
                fareView.setText(fare);
            }
        }

        void updateInfo() {
            DirectionsGenerator gen = new DirectionsGenerator(itinerary.legs, getActivity().getApplicationContext());
            List<Direction> directions = gen.getDirections();
            Direction direction_data[] = directions.toArray(new Direction[directions.size()]);

            DirectionExpandableListAdapter adapter = new DirectionExpandableListAdapter(
                    getActivity(),
                    R.layout.list_direction_item, R.layout.list_subdirection_item, direction_data);

            mDirectionsListView.setAdapter(adapter);

            mDirectionsListView.setGroupIndicator(null);

            Context context = Application.get().getApplicationContext();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
                NotificationChannel channel = manager.getNotificationChannel(Application.CHANNEL_TRIP_PLAN_UPDATES_ID);
                if(channel.getImportance() != NotificationManager.IMPORTANCE_NONE){
                    RealtimeService.start(getActivity(), getArguments());
                }
            } else {
                if (Application.getPrefs()
                        .getBoolean(getString(R.string.preference_key_trip_plan_notifications), true)) {

                    RealtimeService.start(getActivity(), getArguments());
                }
            }
        }

        void updateMap() {
            mMapBundle.putSerializable(MapParams.ITINERARY, itinerary);
            mMapFragment.setMapMode(MapParams.MODE_DIRECTIONS, mMapBundle);
        }
    }
}
