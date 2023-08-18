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
package org.onebusaway.android.util;

import android.os.Build;
import android.text.TextUtils;

import org.onebusaway.android.R;
import org.onebusaway.android.app.Application;
import org.onebusaway.android.map.googlemapsv2.LayerInfo;

/**
 * Utility methods related to creating layers. Currently only methods related to the bikeshare layer
 * are present as this is the only layer at the moment.
 *
 * Created by carvalhorr on 7/21/17.
 */

public class LayerUtils {
    public static int mLayerColor = R.color.theme_muted;
    /**
     * @return true if the bikeshare layer is active and visible
     */
    public static boolean isBikeshareLayerVisible() {
        return Application.isBikeshareEnabled() && Application.getPrefs().getBoolean(
                Application.get().getString(R.string.preference_key_layer_bikeshare_visible), true);
    }

    public static boolean isMapstyleGrayscaleLayerVisible() {
        return Application.getPrefs().getBoolean(
                Application.get().getString(R.string.preference_key_layer_mapstyle_grayscale_visible), false);
    }

    public static boolean isMapstyleNightLayerVisible() {
        return Application.getPrefs().getBoolean(
                Application.get().getString(R.string.preference_key_layer_mapstyle_night_visible), true);
    }

    public static boolean isMapstyleRetroLayerVisible() {
        return Application.getPrefs().getBoolean(
                Application.get().getString(R.string.preference_key_layer_mapstyle_retro_visible), false);
    }

    public static boolean isMapstyleAubergineLayerVisible() {
        return Application.getPrefs().getBoolean(
                Application.get().getString(R.string.preference_key_layer_mapstyle_aubergine_visible), false);
    }

    public static boolean isMapstyleDarkLayerVisible() {
        return Application.getPrefs().getBoolean(
                Application.get().getString(R.string.preference_key_layer_mapstyle_dark_visible), false);
    }

    public static boolean isMapstyleStandardLayerVisible() {
        return Application.getPrefs().getBoolean(
                Application.get().getString(R.string.preference_key_layer_mapstyle_standard_visible), false);
    }

    public static boolean isMapstyleSilverLayerVisible() {
        return Application.getPrefs().getBoolean(
                Application.get().getString(R.string.preference_key_layer_mapstyle_silver_visible), false);
    }
    /**
     * Information necessary to create Speed Dial menu on the Layers FAB.
     * @return LayerInfo instance for bikeshare layer
     */
    public static final LayerInfo bikeshareLayerInfo = new LayerInfo() {
        @Override
        public String getLayerlabel() {
            return Application.get().getString(R.string.layers_speedial_bikeshare_label);
        }

        @Override
        public int getLabelBackgroundDrawableId() {
            return R.drawable.speed_dial_bikeshare_item_label;
        }

        @Override
        public int getIconDrawableId() {
            return R.drawable.ic_directions_bike_white;
        }


        @Override
        public int getLayerColor() {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                return Application.get().getColor(mLayerColor);
            } else {
                //noinspection deprecation
                return Application.get().getResources().getColor(mLayerColor);
            }
        }

        @Override
        public String getSharedPreferenceKey() {
            return Application.get().getString(R.string.preference_key_layer_bikeshare_visible);
        }

        @Override
        public int getGroup() {
            return 0;
        }
    };

    public static final LayerInfo mapstyleGrayscaleLayerInfo = new LayerInfo() {
        @Override
        public String getLayerlabel() {
            return Application.get().getString(R.string.layers_speedial_mapstyle_grayscale_label);
        }

        @Override
        public int getLabelBackgroundDrawableId() {
            return R.drawable.speed_dial_bikeshare_item_label;
        }

        @Override
        public int getIconDrawableId() {
            return R.drawable.ic_map_layer_grayscale;
        }

        @Override
        public int getLayerColor() {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                return Application.get().getColor(R.color.theme_muted);
            } else {
                //noinspection deprecation
                return Application.get().getResources().getColor(R.color.theme_muted);
            }
        }

        @Override
        public String getSharedPreferenceKey() {
            return Application.get().getString(R.string.preference_key_layer_mapstyle_grayscale_visible);
        }

        @Override
        public int getGroup() {
            return 1;
        }
    };

    public static final LayerInfo mapstyleNightLayerInfo = new LayerInfo() {
        @Override
        public String getLayerlabel() {
            return Application.get().getString(R.string.layers_speedial_mapstyle_night_label);
        }

        @Override
        public int getLabelBackgroundDrawableId() {
            return R.drawable.speed_dial_bikeshare_item_label;
        }

        @Override
        public int getIconDrawableId() {
            return R.drawable.ic_map_layer_night;
        }


        @Override
        public int getLayerColor() {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                return Application.get().getColor(R.color.theme_muted);
            } else {
                //noinspection deprecation
                return Application.get().getResources().getColor(R.color.theme_muted);
            }
        }

        @Override
        public String getSharedPreferenceKey() {
            return Application.get().getString(R.string.preference_key_layer_mapstyle_night_visible);
        }

        @Override
        public int getGroup() {
            return 1;
        }
    };

    public static final LayerInfo mapstyleRetroLayerInfo = new LayerInfo() {
        @Override
        public String getLayerlabel() {
            return Application.get().getString(R.string.layers_speedial_mapstyle_retro_label);
        }

        @Override
        public int getLabelBackgroundDrawableId() {
            return R.drawable.speed_dial_bikeshare_item_label;
        }

        @Override
        public int getIconDrawableId() {
            return R.drawable.ic_map_layer_retro;
        }


        @Override
        public int getLayerColor() {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                return Application.get().getColor(mLayerColor);
            } else {
                //noinspection deprecation
                return Application.get().getResources().getColor(mLayerColor);
            }
        }

        @Override
        public String getSharedPreferenceKey() {
            return Application.get().getString(R.string.preference_key_layer_mapstyle_retro_visible);
        }

        @Override
        public int getGroup() {
            return 1;
        }
    };

    public static final LayerInfo mapstyleAubergineLayerInfo = new LayerInfo() {
        @Override
        public String getLayerlabel() {
            return Application.get().getString(R.string.layers_speedial_mapstyle_aubergine_label);
        }

        @Override
        public int getLabelBackgroundDrawableId() {
            return R.drawable.speed_dial_bikeshare_item_label;
        }

        @Override
        public int getIconDrawableId() {
            return R.drawable.ic_map_layer_aubergine;
        }


        @Override
        public int getLayerColor() {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                return Application.get().getColor(mLayerColor);
            } else {
                //noinspection deprecation
                return Application.get().getResources().getColor(mLayerColor);
            }
        }

        @Override
        public String getSharedPreferenceKey() {
            return Application.get().getString(R.string.preference_key_layer_mapstyle_aubergine_visible);
        }

        @Override
        public int getGroup() {
            return 1;
        }
    };

    public static final LayerInfo mapstyleDarkLayerInfo = new LayerInfo() {
        @Override
        public String getLayerlabel() {
            return Application.get().getString(R.string.layers_speedial_mapstyle_dark_label);
        }

        @Override
        public int getLabelBackgroundDrawableId() {
            return R.drawable.speed_dial_bikeshare_item_label;
        }

        @Override
        public int getIconDrawableId() {
            return R.drawable.ic_map_layer_dark;
        }


        @Override
        public int getLayerColor() {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                return Application.get().getColor(mLayerColor);
            } else {
                //noinspection deprecation
                return Application.get().getResources().getColor(mLayerColor);
            }
        }

        @Override
        public String getSharedPreferenceKey() {
            return Application.get().getString(R.string.preference_key_layer_mapstyle_dark_visible);
        }

        @Override
        public int getGroup() {
            return 1;
        }
    };

    public static final LayerInfo mapstyleStandardLayerInfo = new LayerInfo() {
        @Override
        public String getLayerlabel() {
            return Application.get().getString(R.string.layers_speedial_mapstyle_standard_label);
        }

        @Override
        public int getLabelBackgroundDrawableId() {
            return R.drawable.speed_dial_bikeshare_item_label;
        }

        @Override
        public int getIconDrawableId() {
            return R.drawable.ic_map_layer_standard;
        }


        @Override
        public int getLayerColor() {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                return Application.get().getColor(mLayerColor);
            } else {
                //noinspection deprecation
                return Application.get().getResources().getColor(mLayerColor);
            }
        }

        @Override
        public String getSharedPreferenceKey() {
            return Application.get().getString(R.string.preference_key_layer_mapstyle_standard_visible);
        }

        @Override
        public int getGroup() {
            return 1;
        }
    };

    public static final LayerInfo mapstyleSilverLayerInfo = new LayerInfo() {
        @Override
        public String getLayerlabel() {
            return Application.get().getString(R.string.layers_speedial_mapstyle_silver_label);
        }

        @Override
        public int getLabelBackgroundDrawableId() {
            return R.drawable.speed_dial_bikeshare_item_label;
        }

        @Override
        public int getIconDrawableId() {
            return R.drawable.ic_map_layer_silver;
        }


        @Override
        public int getLayerColor() {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                return Application.get().getColor(mLayerColor);
            } else {
                //noinspection deprecation
                return Application.get().getResources().getColor(mLayerColor);
            }
        }

        @Override
        public String getSharedPreferenceKey() {
            return Application.get().getString(R.string.preference_key_layer_mapstyle_silver_visible);
        }

        @Override
        public int getGroup() {
            return 1;
        }
    };

    public static final LayerInfo stopInfoLayerInfo = new LayerInfo() {
        @Override
        public String getLayerlabel() {
            return Application.get().getString(R.string.layers_speedial_stop_stop_info_label);
        }

        @Override
        public int getLabelBackgroundDrawableId() {
            return R.drawable.speed_dial_bikeshare_item_label;
        }

        @Override
        public int getIconDrawableId() {
            return R.drawable.ic_info;
        }


        @Override
        public int getLayerColor() {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                return Application.get().getColor(mLayerColor);
            } else {
                //noinspection deprecation
                return Application.get().getResources().getColor(mLayerColor);
            }
        }

        @Override
        public String getSharedPreferenceKey() {
            return null;
        }

        @Override
        public int getGroup() {
            return 0;
        }
    };

    public static final LayerInfo stopPlanTripLayerInfo = new LayerInfo() {
        @Override
        public String getLayerlabel() {
            return Application.get().getString(R.string.layers_speedial_stop_plan_trip_label);
        }

        @Override
        public int getLabelBackgroundDrawableId() {
            return R.drawable.speed_dial_bikeshare_item_label;
        }

        @Override
        public int getIconDrawableId() {
            return R.drawable.ic_trip_white;
        }


        @Override
        public int getLayerColor() {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                return Application.get().getColor(mLayerColor);
            } else {
                //noinspection deprecation
                return Application.get().getResources().getColor(mLayerColor);
            }
        }

        @Override
        public String getSharedPreferenceKey() {
            return null;
        }

        @Override
        public int getGroup() {
            return 0;
        }
    };

    public static final LayerInfo stopFilterRouteLayerInfo = new LayerInfo() {
        @Override
        public String getLayerlabel() {
            return Application.get().getString(R.string.layers_speedial_stop_filter_route_label);
        }

        @Override
        public int getLabelBackgroundDrawableId() {
            return R.drawable.speed_dial_bikeshare_item_label;
        }

        @Override
        public int getIconDrawableId() {
            return R.drawable.ic_filter;
        }


        @Override
        public int getLayerColor() {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                return Application.get().getColor(mLayerColor);
            } else {
                //noinspection deprecation
                return Application.get().getResources().getColor(mLayerColor);
            }
        }

        @Override
        public String getSharedPreferenceKey() {
            return null;
        }

        @Override
        public int getGroup() {
            return 0;
        }
    };

    public static final LayerInfo stopHideAlertLayerInfo = new LayerInfo() {
        @Override
        public String getLayerlabel() {
            return Application.get().getString(R.string.layers_speedial_stop_hide_alert_label);
        }

        @Override
        public int getLabelBackgroundDrawableId() {
            return R.drawable.speed_dial_bikeshare_item_label;
        }

        @Override
        public int getIconDrawableId() {
            return R.drawable.ic_hide;
        }


        @Override
        public int getLayerColor() {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                return Application.get().getColor(mLayerColor);
            } else {
                //noinspection deprecation
                return Application.get().getResources().getColor(mLayerColor);
            }
        }

        @Override
        public String getSharedPreferenceKey() {
            return null;
        }

        @Override
        public int getGroup() {
            return 0;
        }
    };

    public static final LayerInfo stopReportIssueLayerInfo = new LayerInfo() {
        @Override
        public String getLayerlabel() {
            return Application.get().getString(R.string.layers_speedial_stop_report_issue_label);
        }

        @Override
        public int getLabelBackgroundDrawableId() {
            return R.drawable.speed_dial_bikeshare_item_label;
        }

        @Override
        public int getIconDrawableId() {
            return R.drawable.ic_report_issue;
        }


        @Override
        public int getLayerColor() {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                return Application.get().getColor(mLayerColor);
            } else {
                //noinspection deprecation
                return Application.get().getResources().getColor(mLayerColor);
            }
        }

        @Override
        public String getSharedPreferenceKey() {
            return null;
        }

        @Override
        public int getGroup() {
            return 0;
        }
    };
}
