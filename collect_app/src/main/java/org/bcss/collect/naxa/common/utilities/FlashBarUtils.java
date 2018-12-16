package org.bcss.collect.naxa.common.utilities;

import android.app.Activity;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.view.View;

import org.bcss.collect.android.R;
import org.bcss.collect.android.utilities.ToastUtils;
import org.bcss.collect.naxa.onboarding.DownloadActivity;
import org.bcss.collect.naxa.sync.DownloadActivityRefresh;

import java.util.concurrent.TimeUnit;

import timber.log.Timber;

public class FlashBarUtils {

    public static void showOutOfSyncMsg(@NonNull int outOfSyncUid, @NonNull Activity context, @NonNull String message) {
        if (message.isEmpty()) {
            return;
        }

        try {
            View rootView = context.getWindow().getDecorView().getRootView();
            Snackbar snack = Snackbar.make(rootView, message, Snackbar.LENGTH_INDEFINITE);
            snack.setActionTextColor(ContextCompat.getColor(context, R.color.colorPrimaryLight));
            snack.setAction("Resolve", v -> {
                DownloadActivityRefresh.start(context,outOfSyncUid);
            });

            SnackbarHelper.configSnackbar(context, snack);
            snack.show();
        } catch (Exception e) {
            Timber.e(e);
            ToastUtils.showLongToast(message);
        }
    }


    public static void showErrorFlashbar(@NonNull Activity context, @NonNull String message) {
        showFlashbar(context, message, false);
    }

    public static void showFlashbar(@NonNull Activity context, @NonNull String message, boolean progressIcon) {


        if (message.isEmpty()) {
            return;
        }


        try {
            View rootView = context.getWindow().getDecorView().getRootView();
            Snackbar snack = Snackbar.make(rootView, message, Snackbar.LENGTH_LONG);
            snack.setActionTextColor(ContextCompat.getColor(context, R.color.colorPrimaryLight));
            snack.setAction("Resolve", v -> {

            });
            SnackbarHelper.configSnackbar(context, snack);
            snack.show();
        } catch (Exception e) {
            Timber.e(e);
            ToastUtils.showLongToast(message);


        }
    }
}
