// Copyright 2015 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.chrome.browser.preferences;

import android.os.Bundle;
import android.os.Handler;
import android.preference.Preference;
import android.preference.PreferenceFragment;


import org.chromium.base.CommandLine;
import org.chromium.chrome.R;
import org.chromium.chrome.browser.PasswordUIView;
import org.chromium.chrome.browser.autofill.PersonalDataManager;
import org.chromium.chrome.browser.net.spdyproxy.DataReductionProxySettings;
import org.chromium.chrome.browser.partnercustomizations.HomepageManager;
import org.chromium.chrome.browser.preferences.datareduction.DataReductionPreferences;
import org.chromium.chrome.browser.signin.SigninManager;
import org.chromium.chrome.browser.signin.SigninManager.SignInStateObserver;

/**
 * The main settings screen, shown when the user first opens Settings.
 */
public class MainPreferences extends PreferenceFragment implements SignInStateObserver {

    public static final String PREF_SIGN_IN = "sign_in";
    public static final String PREF_SEARCH_ENGINE = "search_engine";
    public static final String PREF_NIGHT_MODE = "night_mode";
    public static final String PREF_POWERSAVE_MODE = "powersave_mode";
    public static final String PREF_DOCUMENT_MODE = "document_mode";
    public static final String PREF_AUTOFILL_SETTINGS = "autofill_settings";
    public static final String PREF_SAVED_PASSWORDS = "saved_passwords";
    public static final String PREF_BACKGROUND_AUDIO = "background_audio";
    public static final String PREF_HOMEPAGE = "homepage";
    public static final String PREF_DATA_REDUCTION = "data_reduction";

    public static final String ACCOUNT_PICKER_DIALOG_TAG = "account_picker_dialog_tag";
    public static final String EXTRA_SHOW_SEARCH_ENGINE_PICKER = "show_search_engine_picker";

    private SignInPreference mSignInPreference;
    private ManagedPreferenceDelegate mManagedPreferenceDelegate;

    private boolean mShowSearchEnginePicker;

    public MainPreferences() {
        setHasOptionsMenu(true);
        mManagedPreferenceDelegate = createManagedPreferenceDelegate();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState == null && getArguments() != null
                && getArguments().getBoolean(EXTRA_SHOW_SEARCH_ENGINE_PICKER, false)) {
            mShowSearchEnginePicker = true;
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        SigninManager.get(getActivity()).addSignInStateObserver(this);

        // updatePreferences() must be called before setupSignInPref as updatePreferences loads
        // the SignInPreference.
        updatePreferences();
        setupSignInPref();

        if (mShowSearchEnginePicker) {
            mShowSearchEnginePicker = false;
            ((SearchEnginePreference) findPreference(PREF_SEARCH_ENGINE)).showDialog();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        SigninManager.get(getActivity()).removeSignInStateObserver(this);
        clearSignInPref();
    }

    private void updatePreferences() {
        if (getPreferenceScreen() != null) getPreferenceScreen().removeAll();
        addPreferencesFromResource(R.xml.main_preferences);

        Preference nightMode = findPreference(PREF_NIGHT_MODE);
        setOnOffSummary(nightMode,
                PrefServiceBridge.getInstance().getNightModeEnabled());
        Preference powersaveMode = findPreference(PREF_POWERSAVE_MODE);
        setOnOffSummary(powersaveMode,
                PrefServiceBridge.getInstance().getPowersaveModeEnabled());

        ChromeBasePreference autofillPref =
                (ChromeBasePreference) findPreference(PREF_AUTOFILL_SETTINGS);
        setOnOffSummary(autofillPref, PersonalDataManager.isAutofillEnabled());
        autofillPref.setManagedPreferenceDelegate(mManagedPreferenceDelegate);

        ChromeBasePreference passwordsPref =
                (ChromeBasePreference) findPreference(PREF_SAVED_PASSWORDS);
        if (PasswordUIView.shouldUseSmartLockBranding()) {
            passwordsPref.setTitle(getResources().getString(
                    R.string.prefs_smart_lock_for_passwords));
        }
        setOnOffSummary(passwordsPref,
                PrefServiceBridge.getInstance().isRememberPasswordsEnabled());
        passwordsPref.setManagedPreferenceDelegate(mManagedPreferenceDelegate);

        Preference backgroundAudio = findPreference(PREF_BACKGROUND_AUDIO);
        setOnOffSummary(backgroundAudio,
                PrefServiceBridge.getInstance().getBackgroundAudioEnabled());

        BrowserHomepagePreferences homepagePref = (BrowserHomepagePreferences)
                findPreference(PREF_HOMEPAGE);
        if (!HomepageManager.isHomepageEnabled(getActivity())) {
            getPreferenceScreen().removePreference(homepagePref);
        } else {
            homepagePref.setSummary(homepagePref.getDisplayString());
            if (getArguments() != null) {
                homepagePref.setCurrentURL(getArguments().getString(
                        BrowserHomepagePreferences.CURRENT_URL));
            }
        }

        ChromeBasePreference dataReduction =
                (ChromeBasePreference) findPreference(PREF_DATA_REDUCTION);

        if (CommandLine.getInstance().hasSwitch(
                    DataReductionPreferences.ENABLE_DATA_REDUCTION_PROXY) &&
                DataReductionProxySettings.getInstance().isDataReductionProxyAllowed()) {
            dataReduction.setSummary(
                    DataReductionPreferences.generateSummary(getResources()));
            dataReduction.setManagedPreferenceDelegate(mManagedPreferenceDelegate);
        } else {
            getPreferenceScreen().removePreference(dataReduction);
        }
    }

    private void setOnOffSummary(Preference pref, boolean isOn) {
        pref.setSummary(getResources().getString(isOn ? R.string.text_on : R.string.text_off));
    }

    private void setupSignInPref() {
        mSignInPreference = (SignInPreference) findPreference(PREF_SIGN_IN);
        mSignInPreference.registerForUpdates();
        mSignInPreference.setEnabled(true);
    }

    private void clearSignInPref() {
        if (mSignInPreference != null) {
            mSignInPreference.unregisterForUpdates();
            mSignInPreference = null;
        }
    }

    // SignInStateObserver

    @Override
    public void onSignedIn() {
        // After signing in or out of a managed account, preferences may change or become enabled
        // or disabled.
        new Handler().post(new Runnable() {
            @Override
            public void run() {
                updatePreferences();
            }
        });
    }

    @Override
    public void onSignedOut() {
        updatePreferences();
    }

    private ManagedPreferenceDelegate createManagedPreferenceDelegate() {
        return new ManagedPreferenceDelegate() {
            @Override
            public boolean isPreferenceControlledByPolicy(Preference preference) {
                if (PREF_AUTOFILL_SETTINGS.equals(preference.getKey())) {
                    return PersonalDataManager.isAutofillManaged();
                }
                if (PREF_SAVED_PASSWORDS.equals(preference.getKey())) {
                    return PrefServiceBridge.getInstance().isRememberPasswordsManaged();
                }
                if (PREF_DATA_REDUCTION.equals(preference.getKey())) {
                    return DataReductionProxySettings.getInstance().isDataReductionProxyManaged();
                }
                return false;
            }

            @Override
            public boolean isPreferenceClickDisabledByPolicy(Preference preference) {
                if (PREF_AUTOFILL_SETTINGS.equals(preference.getKey())) {
                    return PersonalDataManager.isAutofillManaged()
                            && !PersonalDataManager.isAutofillEnabled();
                }
                if (PREF_SAVED_PASSWORDS.equals(preference.getKey())) {
                    PrefServiceBridge prefs = PrefServiceBridge.getInstance();
                    return prefs.isRememberPasswordsManaged()
                            && !prefs.isRememberPasswordsEnabled();
                }
                if (PREF_DATA_REDUCTION.equals(preference.getKey())) {
                    DataReductionProxySettings settings = DataReductionProxySettings.getInstance();
                    return settings.isDataReductionProxyManaged()
                            && !settings.isDataReductionProxyEnabled();
                }
                return super.isPreferenceClickDisabledByPolicy(preference);
            }
        };
    }
}
