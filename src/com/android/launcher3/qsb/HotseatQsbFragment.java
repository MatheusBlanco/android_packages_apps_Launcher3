/*
 * Copyright (C) 2024 The Android Open Source Project
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

package com.android.launcher3.qsb;

import android.annotation.Nullable;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProviderInfo;
import android.content.ComponentName;
import android.content.Context;
import android.os.Bundle;
import android.os.Process;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.android.launcher3.DeviceProfile;
import com.android.launcher3.LauncherPrefs;
import com.android.launcher3.R;
import com.android.launcher3.Utilities;
import com.android.launcher3.dagger.LauncherComponentProvider;
import com.android.launcher3.views.ActivityContext;

/**
 * A QSB fragment specifically for the hotseat that binds a user-selected widget
 * instead of using a fake custom view.
 */
public class HotseatQsbFragment extends QsbContainerView.QsbFragment {

    @Override
    public void onInit(Bundle savedInstanceState) {
        mKeyWidgetId = "qsb_hotseat_widget_id";
        super.onInit(savedInstanceState);
    }

    @Override
    public boolean isQsbEnabled() {
        return Utilities.showQSB(getContext());
    }

    @Override
    protected Bundle createBindOptions() {
        ActivityContext activityContext = ActivityContext.lookupContext(getContext());
        DeviceProfile dp = activityContext.getDeviceProfile();
        int columnSpan = dp.getHotseatColumnSpan();
        return LauncherComponentProvider.get(getContext())
                .getWidgetSizeHandler().getWidgetSizeOptions(columnSpan, 1);
    }

    @Nullable
    private ComponentName getSelectedWidgetComponent() {
        String selected = LauncherPrefs.DOCK_SEARCH_WIDGET.get(getContext());
        if ("default".equals(selected)) {
            return null;
        }
        return ComponentName.unflattenFromString(selected);
    }

    @Override
    protected AppWidgetProviderInfo getSearchWidgetProvider() {
        ComponentName selected = getSelectedWidgetComponent();
        if (selected == null) {
            return null;
        }
        AppWidgetManager widgetManager = AppWidgetManager.getInstance(getContext());
        for (AppWidgetProviderInfo info :
                widgetManager.getInstalledProvidersForProfile(Process.myUserHandle())) {
            if (info.provider.equals(selected)) {
                return info;
            }
        }
        return null;
    }

    @Override
    protected View getDefaultView(ViewGroup container, boolean showSetupIcon) {
        ComponentName selected = getSelectedWidgetComponent();
        if (selected == null) {
            // crDroid default - show the custom QsbLayout
            Context context = getContext();
            boolean usePixelStyle = LauncherPrefs.DOCK_SEARCH_PIXEL_STYLE.get(context);
            int layoutRes = usePixelStyle
                    ? R.layout.search_container_hotseat_pixel
                    : R.layout.search_container_hotseat;
            return LayoutInflater.from(context).inflate(layoutRes, container, false);
        }
        // A specific widget was chosen but binding failed.
        // Use the parent's default view which shows a setup button,
        // but hide the search button to avoid launching Google Search.
        View v = super.getDefaultView(container, showSetupIcon);
        View searchBtn = v.findViewById(R.id.btn_qsb_search);
        if (searchBtn != null) {
            searchBtn.setVisibility(View.GONE);
        }
        return v;
    }
}
