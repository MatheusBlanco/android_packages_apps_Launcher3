package com.android.launcher3.qsb;

import static android.appwidget.AppWidgetProviderInfo.WIDGET_CATEGORY_SEARCHBOX;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProviderInfo;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Process;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;

import java.util.ArrayList;
import java.util.List;
/**
 * Utility to find widgets eligible for use as the Hotseat QSB.
 */
public final class HotseatQsbWidgetProvider {
    private static final String CRDROID_DEFAULT = "default";

    private HotseatQsbWidgetProvider(){}

    /**
     * Data class representing a selectable widget option.
     */
    public static final class WidgetOption {
        @NonNull public final String id;
        @NonNull public final String label;

        // Null for crDroid default
        @Nullable public final ComponentName componentName;
        // Wether this is a searchbox-category widget (google search widget)
        public final boolean isSearchBox;

        public WidgetOption(@NonNull String id, @NonNull String label, @Nullable ComponentName componentName, boolean isSearchBox) {
            this.id = id;
            this.label = label;
            this.componentName = componentName;
            this.isSearchBox = isSearchBox;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    /**
     * Returns all available widget options for the hotseat QSB.
     * Always includes "default" (crDroid's custom QsbLayout).
     *
     * This must be called on a background thread.
     */
    @WorkerThread
    public static List<WidgetOption> getWidgetOptions(Context context) {
        List<WidgetOption> options = new ArrayList<>();

        // 1. crDroid default QSB
        options.add(
            new WidgetOption(
                CRDROID_DEFAULT,
                context.getString(com.android.launcher3.R.string.dock_search_widget_default),
                null,
                true
            )
        );

        // 2. Google search widget (WIDGET_CATEGORY_SEARCHBOX)
        AppWidgetProviderInfo searchWidget = QsbContainerView.getSearchWidgetProviderInfo(context);
        if (searchWidget != null) {
            String label = searchWidget.loadLabel(context.getPackageManager());
            options.add(
                new WidgetOption(
                    searchWidget.provider.flattenToString(),
                    label != null ? label.toString() : searchWidget.provider.getPackageName(),
                    searchWidget.provider,
                    true
                )
            );
        }

        // 3. All searchbox-category widgets from all apps
        AppWidgetManager widgetManager = AppWidgetManager.getInstance(context);

        for (AppWidgetProviderInfo info :
                widgetManager.getInstalledProvidersForProfile(Process.myUserHandle())) {
            // Only include widgets with the searchbox category
            if ((info.widgetCategory & WIDGET_CATEGORY_SEARCHBOX) == 0) continue;

            // Skip if it's already the search widget we added above
            if (searchWidget != null && searchWidget.provider.equals(info.provider)) continue;

            String appLabel = info.loadLabel(context.getPackageManager()).toString();
            String widgetLabel = null;
            try {
                widgetLabel = context.getPackageManager()
                        .getActivityInfo(info.provider, 0).loadLabel(context.getPackageManager())
                        .toString();
            } catch (PackageManager.NameNotFoundException ignored) {}
            String displayLabel = widgetLabel != null && !widgetLabel.equals(appLabel)
                    ? appLabel + " — " + widgetLabel : appLabel;
            options.add(
                new WidgetOption(
                    info.provider.flattenToString(),
                    displayLabel,
                    info.provider,
                    true
                )
            );
        }

        return options;
    }
}
