/*
 * Copyright (C) 2020-22 CorvusROM
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

package com.android.settings.corvus.fragments;

import android.content.ContentResolver;
import android.os.Bundle;
import android.os.SystemProperties;
import android.provider.Settings;
import android.graphics.Color;

import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;
import androidx.preference.SwitchPreference;

import com.android.internal.logging.nano.MetricsProto;
import com.android.settings.R;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settingslib.search.SearchIndexable;

import com.android.internal.util.corvus.CorvusUtils;

@SearchIndexable
public class Statusbar extends SettingsPreferenceFragment
        implements Preference.OnPreferenceChangeListener {

    private static final String COMBINED_SIGNAL_ICONS = "combined_status_bar_signal_icons";

    private SwitchPreference mEnableCombinedSignalIcons;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final ContentResolver resolver = getActivity().getContentResolver();

        addPreferencesFromResource(R.xml.statusbar);

        final PreferenceScreen screen = getPreferenceScreen();

        mEnableCombinedSignalIcons = (SwitchPreference) findPreference(COMBINED_SIGNAL_ICONS);
        String def = Settings.System.getString(getContentResolver(),
                 COMBINED_SIGNAL_ICONS);
        mEnableCombinedSignalIcons.setChecked(def != null && Integer.parseInt(def) == 1);
        mEnableCombinedSignalIcons.setOnPreferenceChangeListener(this);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if  (preference == mEnableCombinedSignalIcons) {
            boolean value = (Boolean) newValue;
            Settings.System.putString(getActivity().getContentResolver(),
                    COMBINED_SIGNAL_ICONS, value ? "1" : "0");
            CorvusUtils.showSystemUiRestartDialog(getActivity());
            return true;
        }
        return false;
    }

    @Override
    public int getMetricsCategory() {
        return MetricsProto.MetricsEvent.CORVUS;
    }

    public static final BaseSearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider(R.xml.statusbar);
}
