/*
 * Copyright (C) 2010-2015 Paul Watts (paulcwatts@gmail.com),
 * University of South Florida (sjbarbeau@gmail.com)
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
package au.mymetro.android.ui;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;

import androidx.appcompat.app.ActionBar;
import androidx.core.content.ContextCompat;
import androidx.core.content.pm.ShortcutInfoCompat;
import androidx.core.content.pm.ShortcutManagerCompat;

import org.onebusaway.android.R;
import org.onebusaway.android.app.Application;
import org.onebusaway.android.ui.MyStarredStopsFragment;
import org.onebusaway.android.ui.MyTabActivityBase;
import org.onebusaway.android.ui.TabListener;
import org.onebusaway.android.util.PreferenceUtils;
import org.onebusaway.android.util.UIUtils;

public class MyStarredStopsAndRoutesActivity extends MyTabActivityBase {
    private static final String TAG = "StarredActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent myIntent = getIntent();
        if (Intent.ACTION_CREATE_SHORTCUT.equals(myIntent.getAction())) {
            ShortcutInfoCompat shortcut = getShortcut();
            ShortcutManagerCompat.requestPinShortcut(this, shortcut, null);
            setResult(RESULT_OK, shortcut.getIntent());
            finish();
        }

        final Resources res = getResources();
        final ActionBar bar = getSupportActionBar();
        bar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
        bar.setDisplayOptions(0, ActionBar.DISPLAY_SHOW_TITLE);

        bar.addTab(bar.newTab()
                .setTag(MyStarredStopsFragment.TAB_NAME)
                .setText(res.getString(R.string.my_recent_stops))
                .setIcon(ContextCompat.getDrawable(this, R.drawable.ic_tab_recent))
                .setTabListener(new TabListener<MyStarredStopsFragment>(
                        this,
                        MyStarredStopsFragment.TAB_NAME,
                        MyStarredStopsFragment.class)));
        bar.addTab(bar.newTab()
                .setTag(MyStarredRoutesFragment.TAB_NAME)
                .setText(res.getString(R.string.my_recent_routes))
                .setIcon(ContextCompat.getDrawable(this, R.drawable.ic_tab_recent))
                .setTabListener(new TabListener<MyStarredRoutesFragment>(
                        this,
                        MyStarredRoutesFragment.TAB_NAME,
                        MyStarredRoutesFragment.class)));

        restoreDefaultTab();

        UIUtils.setupActionBar(this);
        setTitle(R.string.my_starred_title);
    }

    @Override
    protected String getLastTabPref() {
        return "MyStarredStopsAndRoutesActivity.LastTab";
    }

    /**
     * Override default tab handling behavior of MyTabActivityBase - use tab text instead of tag
     * for saving to preference. See #585.
     */
    @Override
    protected void restoreDefaultTab() {
        final String def;
        if (mDefaultTab != null) {
            def = mDefaultTab;
            if (def != null) {
                // Find this tab...
                final ActionBar bar = getSupportActionBar();
                for (int i = 0; i < bar.getTabCount(); ++i) {
                    ActionBar.Tab tab = bar.getTabAt(i);
                    // Still use tab.getTag() here, as its driven by intent or saved instance state
                    if (def.equals(tab.getTag())) {
                        tab.select();
                    }
                }
            }
        } else {
            SharedPreferences settings = Application.getPrefs();
            def = settings.getString(getLastTabPref(), null);
            if (def != null) {
                // Find this tab...
                final ActionBar bar = getSupportActionBar();
                for (int i = 0; i < bar.getTabCount(); ++i) {
                    ActionBar.Tab tab = bar.getTabAt(i);
                    if (def.equals(tab.getText())) {
                        tab.select();
                    }
                }
            }
        }

    }

    /**
     * Override default tab handling behavior of MyTabActivityBase - use tab text instead of tag
     * for saving to preference. See #585.
     */
    @Override
    public void onDestroy() {
        // If there was a tab in the intent, don't save it
        if (mDefaultTab == null) {
            final ActionBar bar = getSupportActionBar();
            final ActionBar.Tab tab = bar.getSelectedTab();
            PreferenceUtils.saveString(getLastTabPref(), (String) tab.getText());

            // Assign a value to mDefaultTab so that super().onDestroy() doesn't overwrite the
            // preference we set above.  FIXME - this is a total hack.
            mDefaultTab = "hack";
        }

        Log.d(TAG, "onDestroy");

        super.onDestroy();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            // Ensure the software and hardware back buttons have the same behavior
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private ShortcutInfoCompat getShortcut() {
        return UIUtils.makeShortcutInfo(this,
                getString(R.string.my_starred_title),
                new Intent(this, MyStarredStopsAndRoutesActivity.class),
                R.drawable.ic_history);
    }
}
