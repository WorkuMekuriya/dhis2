package org.dhis2.data.service;

import android.app.NotificationManager;
import android.content.Context;

import androidx.annotation.NonNull;

import org.dhis2.commons.di.dagger.PerService;
import org.dhis2.commons.prefs.PreferenceProvider;
import org.dhis2.data.service.workManager.WorkManagerController;
import org.dhis2.utils.analytics.AnalyticsHelper;
import org.hisp.dhis.android.core.D2;

import dagger.Module;
import dagger.Provides;

@Module
public class ReservedValuesWorkerModule {

    @Provides
    @PerService
    NotificationManager notificationManager(@NonNull Context context) {
        return (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
    }

    @Provides
    @PerService
    SyncRepository syncRepository(@NonNull D2 d2) {
        return new SyncRepositoryImpl(d2);
    }

    @Provides
    @PerService
    SyncPresenter syncPresenter(
            @NonNull D2 d2,
            @NonNull PreferenceProvider preferences,
            @NonNull WorkManagerController workManagerController,
            @NonNull AnalyticsHelper analyticsHelper,
            @NonNull SyncStatusController syncStatusController,
            @NonNull SyncRepository syncRepository
    ) {
        return new SyncPresenterImpl(d2, preferences, workManagerController, analyticsHelper, syncStatusController, syncRepository);
    }
}
