package com.dublikunt.nclient.components.views

import android.content.*
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.JsonWriter
import android.util.Pair
import android.view.View
import android.widget.Toast
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SeekBarPreference
import com.dublikunt.nclient.CopyToClipboardActivity.Companion.copyTextToClipboard
import com.dublikunt.nclient.PINActivity
import com.dublikunt.nclient.R
import com.dublikunt.nclient.SettingsActivity
import com.dublikunt.nclient.StatusManagerActivity
import com.dublikunt.nclient.components.LocaleManager
import com.dublikunt.nclient.components.launcher.LauncherCalculator
import com.dublikunt.nclient.components.launcher.LauncherReal
import com.dublikunt.nclient.settings.Global
import com.dublikunt.nclient.settings.Global.isExternalStorageManager
import com.dublikunt.nclient.settings.Login
import com.dublikunt.nclient.utility.LogUtility.download
import com.dublikunt.nclient.utility.Utility
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import java.io.IOException
import java.io.StringWriter

class GeneralPreferenceFragment : PreferenceFragmentCompat() {
    private lateinit var act: SettingsActivity
    fun setAct(act: SettingsActivity) {
        this.act = act
    }

    fun setType(type: SettingsActivity.Type?) {
        when (type) {
            SettingsActivity.Type.MAIN -> mainMenu()
            SettingsActivity.Type.COLUMN -> columnMenu()
            SettingsActivity.Type.DATA -> dataMenu()
            else -> mainMenu()
        }
    }

    private fun dataMenu() {
        addPreferencesFromResource(R.xml.settings_data)
        val mobile = findPreference<SeekBarPreference>(getString(R.string.key_mobile_usage))
        val wifi = findPreference<SeekBarPreference>(getString(R.string.key_wifi_usage))
        mobile!!.onPreferenceChangeListener =
            Preference.OnPreferenceChangeListener { _: Preference?, newValue: Any ->
                mobile.setTitle(getDataUsageString(newValue as Int))
                true
            }
        wifi!!.onPreferenceChangeListener =
            Preference.OnPreferenceChangeListener { _: Preference?, newValue: Any ->
                wifi.setTitle(getDataUsageString(newValue as Int))
                true
            }
        mobile.setTitle(getDataUsageString(mobile.value))
        wifi.setTitle(getDataUsageString(wifi.value))
        mobile.updatesContinuously = true
        wifi.updatesContinuously = true
    }

    private fun getDataUsageString(`val`: Int): Int {
        when (`val`) {
            0 -> return R.string.data_usage_no
            1 -> return R.string.data_usage_thumb
            2 -> return R.string.data_usage_full
        }
        return R.string.data_usage_full
    }

    private fun fillRoba() {
        val languages = ArrayList<Pair<String, String>>(LocaleManager.LANGUAGES.size)
        val actualLocale = Global.getLanguage(act)
        for (l in LocaleManager.LANGUAGES) {
            languages.add(Pair(l.toString(), l.getDisplayName(actualLocale)))
        }
        languages.sortBy { o: Pair<String, String> -> o.second }
        languages.add(
            0,
            Pair(getString(R.string.key_default_value), getString(R.string.system_default))
        )
        val preference = findPreference<ListPreference>(getString(R.string.key_language))!!
        val languagesEntry = arrayOfNulls<String>(languages.size)
        val languagesNames = arrayOfNulls<String>(languages.size)
        for (i in languages.indices) {
            val lang = languages[i]
            languagesEntry[i] = lang.first
            languagesNames[i] = lang.second[0].uppercaseChar().toString() + lang.second.substring(1)
        }
        preference.entryValues = languagesEntry
        preference.entries = languagesNames
    }

    private fun mainMenu() {
        addPreferencesFromResource(R.xml.settings)
        fillRoba()
        findPreference<Preference>("status_screen")!!.onPreferenceClickListener =
            Preference.OnPreferenceClickListener {
                val i = Intent(act, StatusManagerActivity::class.java)
                act.runOnUiThread { act.startActivity(i) }
                false
            }
        findPreference<Preference>("col_screen")!!.onPreferenceClickListener =
            Preference.OnPreferenceClickListener {
                val i = Intent(act, SettingsActivity::class.java)
                i.putExtra(act.packageName + ".TYPE", SettingsActivity.Type.COLUMN.ordinal)
                act.runOnUiThread { act.startActivity(i) }
                false
            }
        findPreference<Preference>("data_screen")!!.onPreferenceClickListener =
            Preference.OnPreferenceClickListener {
                val i = Intent(act, SettingsActivity::class.java)
                i.putExtra(act.packageName + ".TYPE", SettingsActivity.Type.DATA.ordinal)
                act.runOnUiThread { act.startActivity(i) }
                false
            }
        findPreference<Preference>(getString(R.string.key_fake_icon))!!.onPreferenceChangeListener =
            Preference.OnPreferenceChangeListener { _: Preference?, newValue: Any ->
                val pm = act.packageManager
                val name1 = ComponentName(act, LauncherReal::class.java)
                val name2 = ComponentName(act, LauncherCalculator::class.java)
                if (newValue as Boolean) {
                    changeLauncher(pm, name1, false)
                    changeLauncher(pm, name2, true)
                } else {
                    changeLauncher(pm, name1, true)
                    changeLauncher(pm, name2, false)
                }
                true
            }
        findPreference<Preference>(getString(R.string.key_use_account_tag))!!.isEnabled =
            Login.isLogged()
        findPreference<Preference>(getString(R.string.key_theme_select))!!.onPreferenceChangeListener =
            Preference.OnPreferenceChangeListener { _: Preference?, _: Any? ->
                act.recreate()
                true
            }
        findPreference<Preference>(getString(R.string.key_language))!!.onPreferenceChangeListener =
            Preference.OnPreferenceChangeListener { _: Preference?, _: Any? ->
                act.recreate()
                true
            }
        findPreference<Preference>("has_pin")!!.onPreferenceChangeListener =
            Preference.OnPreferenceChangeListener setOnPreferenceChangeListener@{ _: Preference?, newValue: Any ->
                if (newValue == java.lang.Boolean.TRUE) {
                    val i = Intent(act, PINActivity::class.java)
                    i.putExtra(act.packageName + ".SET", true)
                    startActivity(i)
                    act.finish()
                    return@setOnPreferenceChangeListener false
                }
                act.getSharedPreferences("Settings", 0).edit().remove("pin").apply()
                true
            }
        findPreference<Preference>("version")!!.title = getString(
            R.string.app_version_format, context?.let {
                Global.getVersionName(
                    it
                )
            }
        )
        initStoragePaths(findPreference(getString(R.string.key_save_path)))
        val cacheSize = Global.recursiveSize(act.cacheDir) / (1 shl 20).toDouble()
        findPreference<Preference>(getString(R.string.key_save_path))!!.onPreferenceChangeListener =
            Preference.OnPreferenceChangeListener setOnPreferenceChangeListener@{ _: Preference?, newValue: Any ->
                if (newValue != getString(R.string.custom_path)) return@setOnPreferenceChangeListener true
                manageCustomPath()
                false
            }
        //clear cache if pressed
        findPreference<Preference>(getString(R.string.key_cache))!!.summary =
            getString(R.string.cache_size_formatted, cacheSize)
        findPreference<Preference>(getString(R.string.key_cookie))!!.onPreferenceClickListener =
            Preference.OnPreferenceClickListener {
                Login.clearCookies()
                true
            }
        findPreference<Preference>(getString(R.string.key_cache))!!.onPreferenceClickListener =
            Preference.OnPreferenceClickListener {
                val builder = MaterialAlertDialogBuilder(
                    act
                )
                builder.setTitle(R.string.clear_cache)
                builder.setPositiveButton(R.string.yes) { _: DialogInterface?, _: Int ->
                    Global.recursiveDelete(
                        act.cacheDir
                    )
                    act.runOnUiThread {
                        Toast.makeText(
                            act,
                            act.getString(R.string.cache_cleared),
                            Toast.LENGTH_SHORT
                        ).show()
                        val cSize = Global.recursiveSize(act.cacheDir) / (2 shl 20).toDouble()
                        findPreference<Preference>(getString(R.string.key_cache))!!.summary =
                            getString(R.string.cache_size_formatted, cSize)
                    }
                }.setNegativeButton(R.string.no, null).setCancelable(true)
                builder.show()
                true
            }
        findPreference<Preference>("copy_settings")!!.onPreferenceClickListener =
            Preference.OnPreferenceClickListener {
                try {
                    copyTextToClipboard(requireContext(), getDataSettings(context))
                } catch (e: IOException) {
                    e.printStackTrace()
                }
                true
            }
        findPreference<Preference>("export")!!.onPreferenceClickListener =
            Preference.OnPreferenceClickListener {
                act.exportSettings()
                true
            }
        findPreference<Preference>("import")!!.onPreferenceClickListener =
            Preference.OnPreferenceClickListener {
                act.importSettings()
                true
            }
        val mirror = findPreference<ListPreference>(getString(R.string.key_site_mirror))
        mirror!!.summary = act.getSharedPreferences("Settings", Context.MODE_PRIVATE)
            .getString(getString(R.string.key_site_mirror), Utility.ORIGINAL_URL)
        mirror.onPreferenceChangeListener =
            Preference.OnPreferenceChangeListener { preference: Preference, newValue: Any ->
                preference.summary = newValue.toString()
                true
            }
    }

    fun manageCustomPath() {
        if (!isExternalStorageManager) {
            act.requestStorageManager()
            return
        }
        val key = getString(R.string.key_save_path)
        val builder = MaterialAlertDialogBuilder(act)
        val edit =
            View.inflate(act, R.layout.autocomplete_entry, null) as MaterialAutoCompleteTextView
        edit.setHint(R.string.insert_path)
        builder.setView(edit)
        builder.setTitle(R.string.insert_path)
        builder.setPositiveButton(R.string.ok) { _: DialogInterface?, _: Int ->
            act.getSharedPreferences("Settings", Context.MODE_PRIVATE).edit()
                .putString(key, edit.text.toString()).apply()
            findPreference<Preference>(key)!!.summary = edit.text.toString()
        }.setNegativeButton(R.string.cancel, null).show()
    }

    private fun changeLauncher(pm: PackageManager, name: ComponentName, enabled: Boolean) {
        val enableState =
            if (enabled) PackageManager.COMPONENT_ENABLED_STATE_ENABLED else PackageManager.COMPONENT_ENABLED_STATE_DISABLED
        pm.setComponentEnabledSetting(name, enableState, PackageManager.DONT_KILL_APP)
    }

    private fun initStoragePaths(storagePreference: ListPreference?) {
        if (!Global.hasStoragePermission(act)) {
            storagePreference!!.isVisible = false
            return
        }
        val files = Global.getUsableFolders(act)
        val strings: MutableList<CharSequence> = ArrayList(files.size + 1)
        for (f in files) {
            strings.add(f.absolutePath)
        }
        strings.add(getString(R.string.custom_path))
        storagePreference!!.entries = strings.toTypedArray()
        storagePreference.entryValues = strings.toTypedArray()
        storagePreference.summary = act.getSharedPreferences("Settings", Context.MODE_PRIVATE)
            .getString(getString(R.string.key_save_path), Global.MAINFOLDER.parent)
        storagePreference.onPreferenceChangeListener =
            Preference.OnPreferenceChangeListener { preference: Preference, newValue: Any ->
                preference.summary = newValue.toString()
                true
            }
    }

    @Throws(IOException::class)
    private fun getDataSettings(context: Context?): String {
        val names = arrayOf("Settings", "ScrapedTags")
        val sw = StringWriter()
        val writer = JsonWriter(sw)
        writer.setIndent("\t")
        writer.beginObject()
        for (name in names) processSharedFromName(writer, context, name)
        writer.endObject()
        writer.flush()
        val settings = sw.toString()
        writer.close()
        download(settings)
        return settings
    }

    @Throws(IOException::class)
    private fun processSharedFromName(writer: JsonWriter, context: Context?, name: String) {
        writer.name(name)
        writer.beginObject()
        val preferences = requireContext().getSharedPreferences(name, 0)
        for (entry in preferences.all.entries) {
            writeEntry(writer, entry)
        }
        writer.endObject()
    }

    @Throws(IOException::class)
    private fun writeEntry(writer: JsonWriter, entry: Map.Entry<String, *>) {
        writer.name(entry.key)
        when (entry.value) {
            is Int -> writer.value(entry.value as Int?)
            is Boolean -> writer.value(
                (entry.value as Boolean?)!!
            )
            is String -> writer.value(entry.value as String?)
            is Long -> writer.value(
                entry.value as Long?
            )
        }
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        preferenceManager.sharedPreferencesName = "Settings"
    }

    private fun columnMenu() {
        addPreferencesFromResource(R.xml.settings_column)
    }
}
