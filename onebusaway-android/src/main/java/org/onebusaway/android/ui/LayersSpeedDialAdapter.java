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
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Build;
import android.preference.PreferenceManager;
import android.widget.TextView;

import androidx.constraintlayout.helper.widget.Layer;

import org.jetbrains.annotations.NotNull;
import org.onebusaway.android.R;
import org.onebusaway.android.map.googlemapsv2.LayerInfo;
import org.onebusaway.android.util.LayerUtils;
import org.onebusaway.android.util.UIUtils;

import java.util.ArrayList;
import java.util.List;

import uk.co.markormesher.android_fab.SpeedDialMenuAdapter;
import uk.co.markormesher.android_fab.SpeedDialMenuItem;

/**
 * Control the display of the available layers options in a speed dial when the layers Floating
 * Action Button is clicked.
 */
public class LayersSpeedDialAdapter extends SpeedDialMenuAdapter {

    private final Context context;

    /**
     * Hold information of which layers are activatedLayers
     */
    private Boolean[] activatedLayers;

    /**
     * Hold information of all available layers
     */
    private LayerInfo[] layers;

    /**
     * Listener to be called when a layer option is activatedLayers/deativated. It supports multiple.
     * Currently there is one listener added to actually add/remove the layer on the map and another
     * one to update the speed dial menu state.
     */
    private List<LayerActivationListener> layerActivationListeners = new ArrayList<>();

    private int mLayerCount = 5;

    private final boolean mSmallDisplay;

    public LayersSpeedDialAdapter(Context context) {
        this.context = context;
        this.mSmallDisplay = UIUtils.isSmallDisplay(context);

        if (mSmallDisplay) {
            mLayerCount = 3;
        }

        setupLayers();
        activatedLayers = new Boolean[mLayerCount];

        refreshActiveLayerStates();
    }

    public void addLayerActivationListener(LayerActivationListener listener) {
        layerActivationListeners.add(listener);
    }

    private void setupLayers() {
        layers = new LayerInfo[mLayerCount];

        int i = 0;

        if (LayerUtils.isBikeshareLayerVisible()) {
            layers[i++] = LayerUtils.bikeshareLayerInfo;
        }

        if (mSmallDisplay) {
            layers[i++] = LayerUtils.mapstyleStandardLayerInfo;
            layers[i++] = LayerUtils.mapstyleNightLayerInfo;
            if (!LayerUtils.isBikeshareLayerVisible()) {
                layers[i] = LayerUtils.mapstyleRetroLayerInfo;
            }
        } else {
            layers[i++] = LayerUtils.mapstyleStandardLayerInfo;
            layers[i++] = LayerUtils.mapstyleNightLayerInfo;
            layers[i++] = LayerUtils.mapstyleRetroLayerInfo;
            layers[i++] = LayerUtils.mapstyleAubergineLayerInfo;
            if (!LayerUtils.isBikeshareLayerVisible()) {
                layers[i] = LayerUtils.mapstyleGrayscaleLayerInfo;
            }
        }

        // layers[i] = LayerUtils.mapstyleSilverLayerInfo;
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
        // Refresh active layer info
        refreshActiveLayerStates();

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
        // Refresh active layer info
        refreshActiveLayerStates();

        LayerInfo layer = layers[position];

        // Set a solid background for the speed dial item label so you can see the text over the map
        label.setText(layer.getLayerlabel());
        label.setTextColor(Color.WHITE);
        int labelDrawableId;
        if (activatedLayers[position]) {
            labelDrawableId = layer.getLabelBackgroundDrawableId();
        } else {
            labelDrawableId = R.drawable.speed_dial_disabled_item_label;
        }
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
        if (position < activatedLayers.length) {
            if (activatedLayers[position]) {
                for (LayerActivationListener listener : layerActivationListeners) {
                    if (listener != null) {
                        listener.onDeactivateLayer(layers[position]);
                    }
                }
            } else {
                for (LayerActivationListener listener : layerActivationListeners) {
                    if (listener != null) {
                        listener.onActivateLayer(layers[position]);
                    }
                }
            }

            LayerInfo selectedLayer = layers[position];
            int group = selectedLayer.getGroup();
            if (getGroupMemberCount(group) > 1) {
                for (int i = 0; i < activatedLayers.length; i++) {
                    if (i == position) {
                        activatedLayers[i] = true;
                        persistSelection(i);
                        continue;
                    }
                    if (layers[i].getGroup() == group) {
                        activatedLayers[i] = false;
                        persistSelection(i);
                    }
                }
            } else {
                activatedLayers[position] = !activatedLayers[position];
                persistSelection(position);
            }
            return false;
        } else {
            return super.onMenuItemClick(position);
        }
    }

    /**
     * Store the layer activation state in the default shared preferences.
     * @param position position of the menu clicked
     */
    private void persistSelection(int position) {
        SharedPreferences sp = PreferenceManager
                .getDefaultSharedPreferences(context);
        sp.edit().putBoolean(layers[position].getSharedPreferenceKey(), activatedLayers[position]).apply();
    }

    private int getGroupMemberCount(int group) {
        int count = 0;
        for (LayerInfo layer : layers) {
            if (layer.getGroup() == group) {
                count++;
            }
        }

        return count;
    }

    private void refreshActiveLayerStates() {
        int i = 0;

        if (LayerUtils.isBikeshareLayerVisible()) {
            activatedLayers[i++] = LayerUtils.isBikeshareLayerVisible();
        }

        if (mSmallDisplay) {
            activatedLayers[i++] = LayerUtils.isMapstyleStandardLayerVisible();
            activatedLayers[i++] = LayerUtils.isMapstyleNightLayerVisible();
            // add this layer if bikeshare is not enabled
            if (!LayerUtils.isBikeshareLayerVisible()) {
                activatedLayers[i] = LayerUtils.isMapstyleRetroLayerVisible();
            }
        } else {
            activatedLayers[i++] = LayerUtils.isMapstyleStandardLayerVisible();
            activatedLayers[i++] = LayerUtils.isMapstyleNightLayerVisible();
            activatedLayers[i++] = LayerUtils.isMapstyleRetroLayerVisible();
            activatedLayers[i++] = LayerUtils.isMapstyleAubergineLayerVisible();
            // add this layer if bikeshare is not enabled
            if (!LayerUtils.isBikeshareLayerVisible()) {
                activatedLayers[i] = LayerUtils.isMapstyleGrayscaleLayerVisible();
            }
        }

        // activatedLayers[i] = LayerUtils.isMapstyleSilverLayerVisible();
    }

    @SuppressWarnings("deprecation")
    @Override
    public int getBackgroundColour(int position) {
        // Refresh active layer info
        refreshActiveLayerStates();

        int activatedColor = layers[position].getLayerColor();
        int deactivatedColor = context.getResources().getColor(R.color.layer_disabled);
        return activatedLayers[position] ?
                activatedColor : deactivatedColor;
    }

    @Override
    public float fabRotationDegrees() {
        return 45.0f;
    }

    /**
     * Interface that any class wishing to respond to layer activation/deactivation must implement.
     */
    public interface LayerActivationListener {
        void onActivateLayer(LayerInfo layer);
        void onDeactivateLayer(LayerInfo layer);
    }
}
