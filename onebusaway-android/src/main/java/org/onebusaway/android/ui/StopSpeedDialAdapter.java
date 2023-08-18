/*
 * Copyright (C) 2017 Rodrigo Carvalho (carvalhorr@gmail.com)
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

import android.content.Context;
import android.graphics.Color;
import android.os.Build;
import android.widget.TextView;

import org.jetbrains.annotations.NotNull;
import org.onebusaway.android.map.googlemapsv2.LayerInfo;
import org.onebusaway.android.util.LayerUtils;

import java.util.ArrayList;
import java.util.List;

import uk.co.markormesher.android_fab.SpeedDialMenuAdapter;
import uk.co.markormesher.android_fab.SpeedDialMenuItem;

/**
 * Control the display of the available layers options in a speed dial when the layers Floating
 * Action Button is clicked.
 */
public class StopSpeedDialAdapter extends SpeedDialMenuAdapter {

    private final Context context;

    /**
     * Hold information of which layers are activatedLayers
     */
    //private Boolean[] activatedLayers;

    /**
     * Hold information of all available layers
     */
    private LayerInfo[] layers;

    /**
     * Listener to be called when a layer option is activatedLayers/deativated. It supports multiple.
     * Currently there is one listener added to actually add/remove the layer on the map and another
     * one to update the speed dial menu state.
     */
    private List<StopSpeedDialListener> stopSpeedDialListeners = new ArrayList<>();

    private int mLayerCount = 4;

    public StopSpeedDialAdapter(Context context) {
        this.context = context;
        setupLayers();
    }

    public void addStopSpeedDialListener(StopSpeedDialListener listener) {
        stopSpeedDialListeners.add(listener);
    }

    private void setupLayers() {
        layers = new LayerInfo[mLayerCount];

        int i = 0;
        layers[i++] = LayerUtils.stopReportIssueLayerInfo;
        layers[i++] = LayerUtils.stopHideAlertLayerInfo;
        layers[i++] = LayerUtils.stopFilterRouteLayerInfo;
        layers[i] = LayerUtils.stopInfoLayerInfo;
    }

    @Override
    public int getCount() {
        return mLayerCount;
    }

    /**
     * Gets the menu item to display at the specified position in the range 0 to `getCount() - 1`.
     * See `SpeedDialMenuItem` for more details.
     * Note: positions start at zero closest to the FAB and increase for items further away.
     * @return the menu item to display at the specified position
     */
    @SuppressWarnings("deprecation")
    @Override
    public SpeedDialMenuItem getMenuItem(Context context, int position) {
        LayerInfo layer = layers[position];

        SpeedDialMenuItem menuItem = new SpeedDialMenuItem(context, layer.getIconDrawableId(), layer.getLayerlabel());
        return menuItem;
    }

    /**
     * Apply formatting to the `TextView` used for the label of the menu item at the given position.
     * Note: positions start at zero closest to the FAB and increase for items further away.
     *
     * @param context
     * @param position
     * @param label
     */
    @Override
    public void onPrepareItemLabel(@NotNull Context context, int position, @NotNull TextView label) {
        LayerInfo layer = layers[position];

        // Set a solid background for the speed dial item label so you can see the text over the map
        label.setText(layer.getLayerlabel());
        label.setTextColor(Color.WHITE);
        int labelDrawableId = layer.getLabelBackgroundDrawableId();;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            label.setBackground(context.getResources().getDrawable(labelDrawableId));
        } else {
            label.setBackgroundDrawable(context.getResources().getDrawable(labelDrawableId));
        }
    }

    /**
     * Handler for click events on menu items.
     * The position passed corresponds to positions passed to `getMenuItem()`.
     * @return `true` to close the menu after the click; `false` to leave it open
     */
    @Override
    public boolean onMenuItemClick(int position) {
        if (position < mLayerCount) {
            for (StopSpeedDialListener listener : stopSpeedDialListeners) {
                if (listener != null) {
                    listener.onStopSpeedDialClicked(layers[position]);
                }
            }
            return false;
        } else {
            return super.onMenuItemClick(position);
        }
    }

    @SuppressWarnings("deprecation")
    @Override
    public int getBackgroundColour(int position) {
        return layers[position].getLayerColor();
    }

    @Override
    public float fabRotationDegrees() {
        return 45.0f;
    }

    /**
     * Interface that any class wishing to respond to layer activation/deactivation must implement.
     */
    public interface StopSpeedDialListener {
        void onStopSpeedDialClicked(LayerInfo layer);
    }
}
