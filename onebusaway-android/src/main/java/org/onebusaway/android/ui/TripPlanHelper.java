package org.onebusaway.android.ui;

import android.content.Context;
import android.location.Location;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import com.google.android.gms.common.api.GoogleApiClient;

import org.onebusaway.android.R;
import org.onebusaway.android.app.Application;
import org.onebusaway.android.directions.util.CustomAddress;
import org.onebusaway.android.directions.util.TripPlanAddresses;
import org.onebusaway.android.provider.ObaContract;

import java.util.ArrayList;

public class TripPlanHelper {

    private Context mContext;
    private AlertDialog mSelectPlanDlg;
    private TripPlanListItemSelectedListener mListener;

    interface TripPlanListItemSelectedListener {
        void onTripPlanListItemSelected(TripPlanAddresses tripPlan);
    }

    public TripPlanHelper(Context context) {
        mContext = context;
    }

    public void setTripPlanListItemSelectedListener(TripPlanListItemSelectedListener listener) {
        mListener = listener;
    }

    public void loadTrips() {
        ArrayList<TripPlanAddresses> tripPlans = ObaContract.TripPlans.getAllTripPlans(mContext);

        ListView listView = new ListView(mContext);
        // Create the adapter to convert the array to views
        TripPlanArrayAdapter adapter = new TripPlanArrayAdapter(mContext, tripPlans);
        // Attach the adapter to a ListView
        listView.setAdapter(adapter);


        final AlertDialog.Builder builder = new AlertDialog.Builder(mContext, R.style.CustomAlertDialog);
        builder.setTitle(mContext.getString(R.string.trip_plan_select_dialog_title));
        if (tripPlans.isEmpty()) {
            builder.setMessage(mContext.getString(R.string.trip_plan_select_dialog_empty_message));
        }
        builder.setCancelable(true);

        // Set up the input
        builder.setView(listView);
        builder.setNegativeButton(
                mContext.getString(R.string.cancel),
                (dialog, which) -> dialog.cancel());
        mSelectPlanDlg = builder.show();
    }

    class TripPlanArrayAdapter extends ArrayAdapter<TripPlanAddresses> {
        final private ArrayList<TripPlanAddresses> mTripPlans;
        final private Context mContext;

        public TripPlanArrayAdapter(Context context, ArrayList<TripPlanAddresses> tripPlans) {
            super(context, 0, tripPlans);
            mContext = context;
            mTripPlans = tripPlans;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            // Get the data item for this position
            TripPlanAddresses tripPlan = getItem(position);
            // Check if an existing view is being reused, otherwise inflate the view
            if (convertView == null) {
                convertView = LayoutInflater.from(getContext()).inflate(R.layout.trip_plan_listitem, parent, false);
            }
            // Lookup view for data population
            TextView tvName = convertView.findViewById(R.id.name);
            TextView tvFrom = convertView.findViewById(R.id.from);
            TextView tvTo = convertView.findViewById(R.id.to);
            ImageView btnDeletePlan = convertView.findViewById(R.id.delete_trip_plan);

            // Populate the data into the template view using the data object
            tvName.setText(tripPlan.getPlanName());

            String currentLocation = mContext.getString(R.string.tripplanner_current_location);
            String fromLine1 = tripPlan.getFromAddress().getAddressLine(0);
            String toLine1 = tripPlan.getToAddress().getAddressLine(0);

            if (currentLocation.equals(fromLine1)) {
                tvFrom.setVisibility(View.GONE);
            } else {
                tvFrom.setText(fromLine1);
            }

            if (currentLocation.equals(toLine1)) {
                tvTo.setVisibility(View.GONE);
            } else {
                tvTo.setText(toLine1);
            }

            convertView.setTag(tripPlan);
            btnDeletePlan.setOnClickListener(v -> {
                AlertDialog.Builder builder = new AlertDialog.Builder(mContext, R.style.CustomAlertDialog);
                builder.setMessage(mContext.getString(
                        R.string.trip_plan_select_dialog_confirm_delete));
                builder.setCancelable(true);
                builder.setNegativeButton(mContext.getString(R.string.cancel), (dialog, id) -> {
                    dialog.cancel();
                });
                builder.setPositiveButton(mContext.getString(R.string.ok),
                        (dialog, id) -> {
                            int removed = ObaContract.TripPlans.delete(mContext, tripPlan.getId());
                            if (removed > 0) {
                                mTripPlans.remove(tripPlan);
                                notifyDataSetChanged();
                                Toast.makeText(mContext, mContext.getResources().getString(R.string.trip_plan_select_dialog_deleted_message), Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(mContext, mContext.getResources().getString(R.string.trip_plan_select_dialog_deleted_message1), Toast.LENGTH_SHORT).show();
                            }
                            dialog.dismiss();
                        });
                AlertDialog d = builder.create();
                d.show();
            });

            convertView.setOnClickListener(v -> {
                TripPlanAddresses selected = (TripPlanAddresses) v.getTag();

                String currentLocationLine = mContext.getString(R.string.tripplanner_current_location);
                String fromAddrLine1 = selected.getFromAddress().getAddressLine(0);
                String toAddrLine1 = selected.getToAddress().getAddressLine(0);

                if (currentLocationLine.equals(fromAddrLine1)) {
                    selected.setFromAddress(null);
                }

                if (currentLocationLine.equals(toAddrLine1)) {
                    selected.setToAddress(null);
                }

                if (mListener != null) {
                    mListener.onTripPlanListItemSelected(selected);
                }

                if (mSelectPlanDlg != null) {
                    mSelectPlanDlg.dismiss();
                }
            });

            // Return the completed view to render on screen
            return convertView;
        }
    }

    public static CustomAddress makeAddressFromLocation(Context mContext, GoogleApiClient mGoogleApiClient) {
        CustomAddress address = CustomAddress.getEmptyAddress();

        Location loc = Application.getLastKnownLocation(mContext, mGoogleApiClient);
        if (loc == null) {
            if (mContext != null) {
                Toast.makeText(mContext, mContext.getString(R.string.no_location_permission), Toast.LENGTH_SHORT).show();
            }
        } else {
            address.setLatitude(loc.getLatitude());
            address.setLongitude(loc.getLongitude());
        }

        address.setAddressLine(0, mContext.getString(R.string.tripplanner_current_location));
        return address;
    }
}
