package com.dublikunt.nclient.components.views;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.JsonWriter;
import android.util.Pair;
import android.view.View;
import android.webkit.CookieManager;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatAutoCompleteTextView;
import androidx.preference.ListPreference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.SeekBarPreference;

import com.dublikunt.nclient.CopyToClipboardActivity;
import com.dublikunt.nclient.PINActivity;
import com.dublikunt.nclient.R;
import com.dublikunt.nclient.SettingsActivity;
import com.dublikunt.nclient.StatusManagerActivity;
import com.dublikunt.nclient.components.LocaleManager;
import com.dublikunt.nclient.components.launcher.LauncherCalculator;
import com.dublikunt.nclient.components.launcher.LauncherReal;
import com.dublikunt.nclient.settings.Global;
import com.dublikunt.nclient.settings.Login;
import com.dublikunt.nclient.utility.LogUtility;
import com.dublikunt.nclient.utility.Utility;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class GeneralPreferenceFragment extends PreferenceFragmentCompat {
    private SettingsActivity act;

    public void setAct(SettingsActivity act) {
        this.act = act;
    }

    public void setType(@NonNull SettingsActivity.Type type) {
        switch (type) {
            case MAIN:
                mainMenu();
                break;
            case COLUMN:
                columnMenu();
                break;
            case DATA:
                dataMenu();
                break;
        }
    }

    private void dataMenu() {
        addPreferencesFromResource(R.xml.settings_data);
        SeekBarPreference mobile = findPreference(getString(R.string.key_mobile_usage));
        SeekBarPreference wifi = findPreference(getString(R.string.key_wifi_usage));
        mobile.setOnPreferenceChangeListener((preference, newValue) -> {
            mobile.setTitle(getDataUsageString((Integer) newValue));
            return true;
        });
        wifi.setOnPreferenceChangeListener((preference, newValue) -> {
            wifi.setTitle(getDataUsageString((Integer) newValue));
            return true;
        });
        mobile.setTitle(getDataUsageString(mobile.getValue()));
        wifi.setTitle(getDataUsageString(wifi.getValue()));
        mobile.setUpdatesContinuously(true);
        wifi.setUpdatesContinuously(true);
    }

    private int getDataUsageString(int val) {
        switch (val) {
            case 0:
                return R.string.data_usage_no;
            case 1:
                return R.string.data_usage_thumb;
            case 2:
                return R.string.data_usage_full;
        }
        return R.string.data_usage_full;
    }

    private void fillRoba() {
        ArrayList<Pair<String, String>> languages = new ArrayList<>(LocaleManager.LANGUAGES.length);
        Locale actualLocale = Global.getLanguage(act);
        for (Locale l : LocaleManager.LANGUAGES) {
            languages.add(new Pair<>(l.toString(), l.getDisplayName(actualLocale)));
        }
        Collections.sort(languages, Comparator.comparing(o -> o.second));
        languages.add(0, new Pair<>(getString(R.string.key_default_value), getString(R.string.system_default)));
        ListPreference preference = findPreference(getString(R.string.key_language));
        assert preference != null;

        String[] languagesEntry = new String[languages.size()];
        String[] languagesNames = new String[languages.size()];
        for (int i = 0; i < languages.size(); i++) {
            Pair<String, String> lang = languages.get(i);
            languagesEntry[i] = lang.first;
            languagesNames[i] = Character.toUpperCase(lang.second.charAt(0)) + lang.second.substring(1);
        }

        preference.setEntryValues(languagesEntry);
        preference.setEntries(languagesNames);

    }

    private void mainMenu() {
        addPreferencesFromResource(R.xml.settings);

        fillRoba();

        findPreference("status_screen").setOnPreferenceClickListener(preference -> {
            Intent i = new Intent(act, StatusManagerActivity.class);
            act.runOnUiThread(() -> act.startActivity(i));
            return false;
        });
        findPreference("col_screen").setOnPreferenceClickListener(preference -> {
            Intent i = new Intent(act, SettingsActivity.class);
            i.putExtra(act.getPackageName() + ".TYPE", SettingsActivity.Type.COLUMN.ordinal());
            act.runOnUiThread(() -> act.startActivity(i));
            return false;
        });
        findPreference("data_screen").setOnPreferenceClickListener(preference -> {
            Intent i = new Intent(act, SettingsActivity.class);
            i.putExtra(act.getPackageName() + ".TYPE", SettingsActivity.Type.DATA.ordinal());
            act.runOnUiThread(() -> act.startActivity(i));
            return false;
        });
        findPreference(getString(R.string.key_fake_icon)).setOnPreferenceChangeListener((preference, newValue) -> {
            PackageManager pm = act.getPackageManager();
            ComponentName name1 = new ComponentName(act, LauncherReal.class);
            ComponentName name2 = new ComponentName(act, LauncherCalculator.class);
            if ((boolean) newValue) {
                changeLauncher(pm, name1, false);
                changeLauncher(pm, name2, true);
            } else {
                changeLauncher(pm, name1, true);
                changeLauncher(pm, name2, false);
            }
            return true;
        });
        findPreference(getString(R.string.key_use_account_tag)).setEnabled(Login.isLogged());

        findPreference(getString(R.string.key_theme_select)).setOnPreferenceChangeListener((preference, newValue) -> {
            act.recreate();
            return true;
        });
        findPreference(getString(R.string.key_language)).setOnPreferenceChangeListener((preference, newValue) -> {
            act.recreate();
            return true;
        });
        findPreference("has_pin").setOnPreferenceChangeListener((preference, newValue) -> {
            if (newValue.equals(Boolean.TRUE)) {
                Intent i = new Intent(act, PINActivity.class);
                i.putExtra(act.getPackageName() + ".SET", true);
                startActivity(i);
                act.finish();
                return false;
            }
            act.getSharedPreferences("Settings", 0).edit().remove("pin").apply();
            return true;
        });

        findPreference("version").setTitle(getString(R.string.app_version_format, Global.getVersionName(getContext())));
        initStoragePaths(findPreference(getString(R.string.key_save_path)));
        double cacheSize = Global.recursiveSize(act.getCacheDir()) / ((double) (1 << 20));
        findPreference(getString(R.string.key_save_path)).setOnPreferenceChangeListener((preference, newValue) -> {
            if (!newValue.equals(getString(R.string.custom_path))) return true;
            manageCustomPath();
            return false;
        });
        findPreference(getString(R.string.key_cache)).setSummary(getString(R.string.cache_size_formatted, cacheSize));
        findPreference(getString(R.string.key_cookie)).setOnPreferenceClickListener(preference -> {
            Login.clearCookies();
            CookieManager.getInstance().removeAllCookie();
            return true;
        });
        findPreference(getString(R.string.key_cache)).setOnPreferenceClickListener(preference -> {
            MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(act);
            builder.setTitle(R.string.clear_cache);
            builder.setPositiveButton(R.string.yes, (dialog, which) -> {
                Global.recursiveDelete(act.getCacheDir());
                act.runOnUiThread(() -> {
                    Toast.makeText(act, act.getString(R.string.cache_cleared), Toast.LENGTH_SHORT).show();
                    double cSize = Global.recursiveSize(act.getCacheDir()) / ((double) (2 << 20));
                    findPreference(getString(R.string.key_cache)).setSummary(getString(R.string.cache_size_formatted, cSize));
                });

            }).setNegativeButton(R.string.no, null).setCancelable(true);
            builder.show();

            return true;
        });
        findPreference("copy_settings").setOnPreferenceClickListener(preference -> {
            try {
                CopyToClipboardActivity.copyTextToClipboard(getContext(), getDataSettings(getContext()));
            } catch (IOException e) {
                e.printStackTrace();
            }
            return true;
        });
        findPreference("export").setOnPreferenceClickListener(preference -> {
            act.exportSettings();
            return true;
        });
        findPreference("import").setOnPreferenceClickListener(preference -> {
            act.importSettings();
            return true;
        });
    }

    public void manageCustomPath() {
        if (!Global.isExternalStorageManager()) {
            act.requestStorageManager();
            return;
        }
        final String key = getString(R.string.key_save_path);
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(act);
        AppCompatAutoCompleteTextView edit = (AppCompatAutoCompleteTextView) View.inflate(act, R.layout.autocomplete_entry, null);
        edit.setHint(R.string.insert_path);
        builder.setView(edit);
        builder.setTitle(R.string.insert_path);
        builder.setPositiveButton(R.string.ok, (dialog, which) -> {
            act.getSharedPreferences("Settings", Context.MODE_PRIVATE).edit().putString(key, edit.getText().toString()).apply();
            findPreference(key).setSummary(edit.getText().toString());
        }).setNegativeButton(R.string.cancel, null).show();
    }

    private void changeLauncher(@NonNull PackageManager pm, ComponentName name, boolean enabled) {
        int enableState = enabled ? PackageManager.COMPONENT_ENABLED_STATE_ENABLED : PackageManager.COMPONENT_ENABLED_STATE_DISABLED;
        pm.setComponentEnabledSetting(name, enableState, PackageManager.DONT_KILL_APP);
    }

    private void initStoragePaths(ListPreference storagePreference) {
        List<File> files = Global.getUsableFolders(act);
        List<CharSequence> strings = new ArrayList<>(files.size() + 1);
        for (File f : files) {
            if (f != null)
                strings.add(f.getAbsolutePath());
        }
        strings.add(getString(R.string.custom_path));
        storagePreference.setEntries(strings.toArray(new CharSequence[0]));
        storagePreference.setEntryValues(strings.toArray(new CharSequence[0]));
        storagePreference.setSummary(
            act.getSharedPreferences("Settings", Context.MODE_PRIVATE)
                .getString(getString(R.string.key_save_path), Global.MainFolder.getParent())
        );
        storagePreference.setOnPreferenceChangeListener((preference, newValue) -> {
            preference.setSummary(newValue.toString());
            return true;
        });
    }

    @NonNull
    private String getDataSettings(Context context) throws IOException {
        String[] names = new String[]{"Settings", "ScrapedTags"};
        StringWriter sw = new StringWriter();
        JsonWriter writer = new JsonWriter(sw);
        writer.setIndent("\t");

        writer.beginObject();
        for (String name : names)
            processSharedFromName(writer, context, name);
        writer.endObject();

        writer.flush();
        String settings = sw.toString();
        writer.close();

        LogUtility.d(settings);
        return settings;
    }

    private void processSharedFromName(@NonNull JsonWriter writer, @NonNull Context context, String name) throws IOException {
        writer.name(name);
        writer.beginObject();
        SharedPreferences preferences = context.getSharedPreferences(name, 0);
        for (Map.Entry<String, ?> entry : preferences.getAll().entrySet()) {
            writeEntry(writer, entry);
        }
        writer.endObject();
    }

    private void writeEntry(@NonNull JsonWriter writer, @NonNull Map.Entry<String, ?> entry) throws IOException {
        writer.name(entry.getKey());
        if (entry.getValue() instanceof Integer) writer.value((Integer) entry.getValue());
        else if (entry.getValue() instanceof Boolean) writer.value((Boolean) entry.getValue());
        else if (entry.getValue() instanceof String) writer.value((String) entry.getValue());
        else if (entry.getValue() instanceof Long) writer.value((Long) entry.getValue());
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        getPreferenceManager().setSharedPreferencesName("Settings");
    }

    private void columnMenu() {
        addPreferencesFromResource(R.xml.settings_column);
    }

}
