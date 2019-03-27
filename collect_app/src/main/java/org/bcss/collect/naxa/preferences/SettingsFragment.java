package org.bcss.collect.naxa.preferences;

import android.app.TimePickerDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.SwitchPreference;
import android.text.format.DateFormat;
import android.util.AttributeSet;
import android.view.Window;
import android.widget.TimePicker;

import org.bcss.collect.android.BuildConfig;
import org.bcss.collect.android.R;
import org.bcss.collect.naxa.common.FieldSightNotificationUtils;
import org.bcss.collect.naxa.jobs.LocalNotificationJob;
import org.odk.collect.android.utilities.ThemeUtils;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;

import timber.log.Timber;

import static org.bcss.collect.naxa.preferences.SettingsKeys.KEY_NOTIFICATION_SAMPLE;
import static org.bcss.collect.naxa.preferences.SettingsKeys.KEY_NOTIFICATION_TIME;


public class SettingsFragment extends PreferenceFragment implements Preference.OnPreferenceClickListener, SharedPreferences.OnSharedPreferenceChangeListener {

    private SharedPreferences sharedPreferences;
    private final boolean NOT_SYNCING = false;
    ThemeUtils themeUtils = new ThemeUtils(SettingsFragment.this.getActivity());
    CustomTimePickerDialog customTimePickerDialog = null;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.fieldsight_preferences);
        findPreference(KEY_NOTIFICATION_TIME).setOnPreferenceClickListener(this);
        findPreference(KEY_NOTIFICATION_SAMPLE).setOnPreferenceClickListener(this);
        setupSharedPreferences();
        setupNotificationToggle();
        setupUpdateButton();
        customTimePickerDialog = new CustomTimePickerDialog(getActivity(), new TimePickerDialog.OnTimeSetListener() {
            @Override
            public void onTimeSet(TimePicker view, int hourOfDay, int minute) {

            }
        }, 0, 0);

    }


    private void setupUpdateButton() {
        Preference preference = findPreference("app_update");
        preference.setOnPreferenceClickListener(this);
        String title = getString(R.string.app_name).concat(": ").concat(BuildConfig.VERSION_NAME);
        preference.setTitle(title);
        preference.setSummary("Check for update");
    }


    private void setupNotificationToggle() {
        SwitchPreference switchPreferenceDaily = (SwitchPreference) findPreference("switch_notification_daily");
        SwitchPreference switchPreferenceWeek = (SwitchPreference) findPreference("switch_notification_weekly");
        SwitchPreference switchPreferenceMonth = (SwitchPreference) findPreference("switch_notification_monthly");

        switchPreferenceDaily.setSummaryOff("You will no longer receive notifications");
        switchPreferenceWeek.setSummaryOff("You will no longer receive notifications");
        switchPreferenceMonth.setSummaryOff("You will no longer receive notifications");
        switchPreferenceDaily.setSummaryOn("You will receive all notifications");
        switchPreferenceWeek.setSummaryOn("You will receive all notifications");
        switchPreferenceMonth.setSummaryOn("You will receive all notifications");
    }


    @Override
    public boolean onPreferenceClick(Preference preference) {
        switch (preference.getKey()) {
            case KEY_NOTIFICATION_TIME:
                customTimePickerDialog.show();
                break;

            case KEY_NOTIFICATION_SAMPLE:
                LocalNotificationJob.runJobImmediately();
                break;
        }
        return false;
    }

    private void setupSharedPreferences() {
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
        sharedPreferences.registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        switch (key) {
            case KEY_NOTIFICATION_TIME:

        }
    }


    private class CustomTimePickerDialog extends TimePickerDialog {
        private final String dialogTitle = getContext().getString(R.string.select_time);

        CustomTimePickerDialog(Context context, OnTimeSetListener callBack, int hour, int minute) {
            super(context, android.R.style.Theme_Holo_Light_Dialog, callBack, hour, minute, DateFormat.is24HourFormat(context));
            setTitle(dialogTitle);
            fixSpinner(context, hour, minute, DateFormat.is24HourFormat(context));

            Window window = getWindow();
            if (window != null) {
                window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            }
        }

        public void setTitle(CharSequence title) {
            super.setTitle(dialogTitle);
        }

        /**
         * Workaround for this bug: https://code.google.com/p/android/issues/detail?id=222208
         * In Android 7.0 Nougat, spinner mode for the TimePicker in TimePickerDialog is
         * incorrectly displayed as clock, even when the theme specifies otherwise.
         * <p>
         * Source: https://gist.github.com/jeffdgr8/6bc5f990bf0c13a7334ce385d482af9f
         */
        @SuppressWarnings("deprecation")
        private void fixSpinner(Context context, int hourOfDay, int minute, boolean is24HourView) {
            // android:timePickerMode spinner and clock began in Lollipop
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                try {
                    // Get the theme's android:timePickerMode
                    final int MODE_SPINNER = 2;
                    Class<?> styleableClass = Class.forName("com.android.internal.R$styleable");
                    Field timePickerStyleableField = styleableClass.getField("TimePicker");
                    int[] timePickerStyleable = (int[]) timePickerStyleableField.get(null);
                    final TypedArray a = context.obtainStyledAttributes(null, timePickerStyleable,
                            android.R.attr.timePickerStyle, 0);
                    Field timePickerModeStyleableField = styleableClass.getField("TimePicker_timePickerMode");
                    int timePickerModeStyleable = timePickerModeStyleableField.getInt(null);
                    final int mode = a.getInt(timePickerModeStyleable, MODE_SPINNER);
                    a.recycle();

                    if (mode == MODE_SPINNER) {
                        Field field = findField(TimePickerDialog.class, TimePicker.class, "mTimePicker");
                        if (field == null) {
                            Timber.e("Reflection failed: Couldn't find field 'mTimePicker'");
                            return;
                        }

                        TimePicker timePicker = (TimePicker) field.get(this);
                        Class<?> delegateClass = Class.forName("android.widget.TimePicker$TimePickerDelegate");
                        Field delegateField = findField(TimePicker.class, delegateClass, "mDelegate");

                        if (delegateField == null) {
                            Timber.e("Reflection failed: Couldn't find field 'mDelegate'");
                            return;
                        }
                        Object delegate = delegateField.get(timePicker);

                        Class<?> spinnerDelegateClass;
                        if (Build.VERSION.SDK_INT != Build.VERSION_CODES.LOLLIPOP) {
                            spinnerDelegateClass = Class.forName("android.widget.TimePickerSpinnerDelegate");
                        } else {
                            // TimePickerSpinnerDelegate was initially misnamed in API 21!
                            spinnerDelegateClass = Class.forName("android.widget.TimePickerClockDelegate");
                        }

                        // In 7.0 Nougat for some reason the timePickerMode is ignored and the
                        // delegate is TimePickerClockDelegate
                        if (delegate.getClass() != spinnerDelegateClass) {
                            delegateField.set(timePicker, null); // throw out the TimePickerClockDelegate!
                            timePicker.removeAllViews(); // remove the TimePickerClockDelegate views
                            Constructor spinnerDelegateConstructor = spinnerDelegateClass
                                    .getConstructor(TimePicker.class, Context.class,
                                            AttributeSet.class, int.class, int.class);
                            spinnerDelegateConstructor.setAccessible(true);

                            // Instantiate a TimePickerSpinnerDelegate
                            delegate = spinnerDelegateConstructor.newInstance(timePicker, context,
                                    null, android.R.attr.timePickerStyle, 0);

                            // set the TimePicker.mDelegate to the spinner delegate
                            delegateField.set(timePicker, delegate);

                            // Set up the TimePicker again, with the TimePickerSpinnerDelegate
                            timePicker.setIs24HourView(is24HourView);
                            timePicker.setCurrentHour(hourOfDay);
                            timePicker.setCurrentMinute(minute);
                            timePicker.setOnTimeChangedListener(this);
                        }
                    }
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        }

        private Field findField(Class objectClass, Class fieldClass, String expectedName) {
            try {
                Field field = objectClass.getDeclaredField(expectedName);
                field.setAccessible(true);
                return field;
            } catch (NoSuchFieldException e) {
                Timber.i(e); // ignore
            }

            // search for it if it wasn't found under the expected ivar name
            for (Field searchField : objectClass.getDeclaredFields()) {
                if (searchField.getType() == fieldClass) {
                    searchField.setAccessible(true);
                    return searchField;
                }
            }
            return null;
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        sharedPreferences.unregisterOnSharedPreferenceChangeListener(this);
    }
}