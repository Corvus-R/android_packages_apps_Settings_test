/*
 * Copyright (C) 2018 The Android Open Source Project
 *               2022 CorvusOS
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

package com.android.settings.homepage;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.animation.LayoutTransition;
import android.app.ActivityManager;
import android.app.settings.SettingsEnums;
import android.content.res.ColorStateList;
import android.content.Intent;
import android.os.Bundle;
import android.os.SystemProperties;
import android.os.Build;
import android.util.FeatureFlagUtils;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Button;
import android.widget.Toolbar;
import androidx.viewpager.widget.ViewPager;

import com.android.settings.corvus.CorvusSettings;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.bottomsheet.BottomSheetDialog;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.fragment.app.FragmentPagerAdapter;

import com.android.settings.R;
import com.android.settings.Utils;
import com.android.settings.accounts.AvatarViewMixin;
import com.android.settings.core.CategoryMixin;
import com.android.settings.core.FeatureFlags;
import com.android.settings.homepage.contextualcards.ContextualCardsFragment;
import com.android.settings.overlay.FeatureFactory;
import com.android.settingslib.core.lifecycle.HideNonSystemOverlayMixin;

import java.util.ArrayList;
/** Settings homepage activity */
public class SettingsHomepageActivity extends FragmentActivity implements
        CategoryMixin.CategoryHandler {

    private static final String TAG = "SettingsHomepageActivity";

    private static final long HOMEPAGE_LOADING_TIMEOUT_MS = 300;

    private View mHomepageView;
    private View mSuggestionView;
    private CategoryMixin mCategoryMixin;

    private TabLayout mTabLayout;
    private ViewPager mViewPager;
    private int[] tabIcons = {
            R.drawable.tab_ic_device,
            R.drawable.tab_ic_corvus
    };

    @Override
    public CategoryMixin getCategoryMixin() {
        return mCategoryMixin;
    }

    /**
     * Shows the homepage and shows/hides the suggestion together. Only allows to be executed once
     * to avoid the flicker caused by the suggestion suddenly appearing/disappearing.
     */
    public void showHomepageWithSuggestion(boolean showSuggestion) {
        if (mHomepageView == null) {
            return;
        }
        Log.i(TAG, "showHomepageWithSuggestion: " + showSuggestion);
        mSuggestionView.setVisibility(showSuggestion ? View.VISIBLE : View.GONE);
        mHomepageView.setVisibility(View.VISIBLE);
        mHomepageView = null;
    }

    Button btnRavenDesk;
    ImageView btnCorvusVersion;
    TextView crvsVersion, crvsMaintainer, crvsDevice, crvsBuildDate, crvsBuildType;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings_homepage_container);

        final View appBar = findViewById(R.id.app_bar_container);
        appBar.setMinimumHeight(getSearchBoxHeight());
        
        initHomepageContainer();
        setupTabIcons();

        final Toolbar toolbar = findViewById(R.id.search_action_bar);
        FeatureFactory.getFactory(this).getSearchFeatureProvider()
                .initSearchToolbar(this /* activity */, toolbar, SettingsEnums.SETTINGS_HOMEPAGE);

        getLifecycle().addObserver(new HideNonSystemOverlayMixin(this));
        mCategoryMixin = new CategoryMixin(this);
        getLifecycle().addObserver(mCategoryMixin);

        if (!getSystemService(ActivityManager.class).isLowRamDevice()) {
            // Only allow features on high ram devices.
            final ImageView avatarView = findViewById(R.id.account_avatar);
            if (AvatarViewMixin.isAvatarSupported(this)) {
                avatarView.setVisibility(View.VISIBLE);
                getLifecycle().addObserver(new AvatarViewMixin(this, avatarView));
            }

            showSuggestionFragment();

            if (FeatureFlagUtils.isEnabled(this, FeatureFlags.CONTEXTUAL_HOME)) {
                showFragment(new ContextualCardsFragment(), R.id.contextual_cards_content);
            }
        }

        btnCorvusVersion = findViewById(R.id.btnCorvusVersion);
        btnCorvusVersion.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    showBottomSheetDialog();
                }
            });
    }

    private void showBottomSheetDialog() {
        final BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(this, R.style.CorvusBottomSheetDialogTheme);
        bottomSheetDialog.setContentView(R.layout.corvus_bottom_sheet);

        crvsDevice = bottomSheetDialog.findViewById(R.id.corvus_device);
        crvsVersion = bottomSheetDialog.findViewById(R.id.corvus_version);
        crvsMaintainer = bottomSheetDialog.findViewById(R.id.corvus_maintainer);
        crvsBuildDate = bottomSheetDialog.findViewById(R.id.corvus_build_date);
        crvsBuildType = bottomSheetDialog.findViewById(R.id.corvus_build_type);

        String buildDate = SystemProperties.get("ro.build.date").substring(0,10);

        crvsDevice.setText(SystemProperties.get("ro.product.device") + "(" + SystemProperties.get("ro.product.model") + ")");
        crvsVersion.setText("Corvus_v"
                + SystemProperties.get("ro.corvus.build.version")
                + "-"
                + SystemProperties.get("ro.corvus.codename"));
        crvsMaintainer.setText(SystemProperties.get("ro.corvus.maintainer"));
        crvsBuildDate.setText(buildDate);
        String buildType = SystemProperties.get("ro.corvus.build.type");
        crvsBuildType.setText(buildType);

        // Initialise intent for Ravendesk
        btnRavenDesk = bottomSheetDialog.findViewById(R.id.btn_ravendesk);

        if(buildType.equals("Official")){
          btnRavenDesk.setVisibility(View.VISIBLE);
        }

        assert btnRavenDesk != null;
        btnRavenDesk.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Intent nIntent = new Intent(Intent.ACTION_MAIN);
                nIntent.setClassName("com.corvus.ravendesk",
                        "com.corvus.ravendesk.MainActivity");
                startActivity(nIntent);
            }
        });

        bottomSheetDialog.show();
    }

    private void showSuggestionFragment() {
        final Class<? extends Fragment> fragment = FeatureFactory.getFactory(this)
                .getSuggestionFeatureProvider(this).getContextualSuggestionFragment();
        if (fragment == null) {
            return;
        }

        mSuggestionView = findViewById(R.id.suggestion_content);
        mHomepageView = findViewById(R.id.settings_homepage_container);
        // Hide the homepage for preparing the suggestion.
        mHomepageView.setVisibility(View.GONE);
        // Schedule a timer to show the homepage and hide the suggestion on timeout.
        mHomepageView.postDelayed(() -> showHomepageWithSuggestion(false),
                HOMEPAGE_LOADING_TIMEOUT_MS);
        try {
            showFragment(fragment.getConstructor().newInstance(), R.id.suggestion_content);
        } catch (Exception e) {
            Log.w(TAG, "Cannot show fragment", e);
        }
    }

    private void showFragment(Fragment fragment, int id) {
        final FragmentManager fragmentManager = getSupportFragmentManager();
        final FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        final Fragment showFragment = fragmentManager.findFragmentById(id);

        if (showFragment == null) {
            fragmentTransaction.add(id, fragment);
        } else {
            fragmentTransaction.show(showFragment);
        }
        fragmentTransaction.commit();
    }

    private void setupTabIcons() {
        mTabLayout.getTabAt(0).setIcon(tabIcons[0]);
        mTabLayout.getTabAt(1).setIcon(tabIcons[1]);
    }

    private void initHomepageContainer() {
        mTabLayout = findViewById(R.id.tab_layout);
        mViewPager = findViewById(R.id.viewPager);

        mTabLayout.setupWithViewPager(mViewPager);
        // setupTabTextColor(mTabLayout);
        ViewPagerAdapter viewPagerAdapter = new ViewPagerAdapter(getSupportFragmentManager(), FragmentPagerAdapter.BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT);
        viewPagerAdapter.addFragment(new TopLevelSettings(), "Device Settings");
        viewPagerAdapter.addFragment(new CorvusSettings(), "Corvus Settings");
        mViewPager.setAdapter(viewPagerAdapter);
    }

    private int getSearchBoxHeight() {
        final int searchBarHeight = getResources().getDimensionPixelSize(R.dimen.search_bar_height);
        final int searchBarMargin = getResources().getDimensionPixelSize(R.dimen.search_bar_margin);
        return searchBarHeight + searchBarMargin * 2;
    }

    static class ViewPagerAdapter extends FragmentPagerAdapter {

        private final ArrayList<Fragment> fragmentArrayList = new ArrayList<>();
        private final ArrayList<String> fragmentTitle = new ArrayList<>();

        public ViewPagerAdapter(@NonNull FragmentManager fm, int behavior) {
            super(fm, behavior);
        }

        @NonNull
        @Override
        public Fragment getItem(int position) {
            return fragmentArrayList.get(position);
        }

        @Override
        public int getCount() {
            return fragmentArrayList.size();
        }

        public void addFragment(Fragment fragment, String title){
            fragmentArrayList.add(fragment);
            fragmentTitle.add(title);
        }

        @Nullable
        @Override
        public CharSequence getPageTitle(int position) {
            return fragmentTitle.get(position);
        }
    }
}
