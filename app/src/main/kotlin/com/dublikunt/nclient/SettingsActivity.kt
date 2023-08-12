package com.dublikunt.nclient

import android.annotation.TargetApi
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.contract.ActivityResultContracts.CreateDocument
import androidx.annotation.RequiresApi
import androidx.appcompat.widget.Toolbar
import com.dublikunt.nclient.async.database.export.Exporter
import com.dublikunt.nclient.async.database.export.Manager
import com.dublikunt.nclient.components.views.GeneralPreferenceFragment
import com.dublikunt.nclient.settings.Global
import com.dublikunt.nclient.utility.LogUtility
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.io.File

class SettingsActivity : GeneralActivity() {
    enum class Type { MAIN, COLUMN, DATA }

    lateinit var fragment: GeneralPreferenceFragment
    private lateinit var importZip: ActivityResultLauncher<String>
    private lateinit var saveSettings: ActivityResultLauncher<String>
    private var requestStorageManager: ActivityResultLauncher<Any?>? = null
    private var selectedItem = 0
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        registerActivities()
        //Global.initActivity(this);
        setContentView(R.layout.activity_settings)
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar!!.setTitle(R.string.settings)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        supportActionBar!!.setDisplayShowTitleEnabled(true)
        fragment = supportFragmentManager.findFragmentById(R.id.fragment) as GeneralPreferenceFragment
        fragment.setAct(this)
        fragment.setType(
            Type.values()[intent.getIntExtra(
                "$packageName.TYPE",
                Type.MAIN.ordinal
            )]
        )
    }

    private fun registerActivities() {
        importZip = registerForActivityResult<String, Uri>(ActivityResultContracts.GetContent()) { selectedFile: Uri? ->
            if (selectedFile == null) return@registerForActivityResult
            importSettings(selectedFile)
        }
        saveSettings = registerForActivityResult<String, Uri>(object :
            CreateDocument("application/zip") {
        }) { selectedFile: Uri? ->
            if (selectedFile == null) return@registerForActivityResult
            exportSettings(selectedFile)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            requestStorageManager =
                registerForActivityResult(object : ActivityResultContract<Any?, Any?>() {
                    @RequiresApi(api = Build.VERSION_CODES.R)
                    override fun createIntent(context: Context, input: Any?): Intent {
                        val i = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                        i.data = Uri.parse("package:$packageName")
                        return i
                    }

                    override fun parseResult(resultCode: Int, intent: Intent?): Any? {
                        return null
                    }
                }) {
                    if (Global.isExternalStorageManager()) {
                        fragment.manageCustomPath()
                    }
                }
        }
    }

    private fun importSettings(selectedFile: Uri) {
        Manager(selectedFile, this, false) {
            Toast.makeText(this, R.string.import_finished, Toast.LENGTH_SHORT).show()
            finish()
        }.start()
    }

    private fun exportSettings(selectedFile: Uri) {
        Manager(selectedFile, this, true) {
            Toast.makeText(
                this,
                R.string.export_finished,
                Toast.LENGTH_SHORT
            ).show()
        }
            .start()
    }

    fun importSettings() {
        importZip.launch("application/zip")
    }

    private fun importOldVersion() {
        val files = Global.BackupFolder.list()
        if (files == null || files.isEmpty()) return
        selectedItem = 0
        val builder = MaterialAlertDialogBuilder(this)
        builder.setSingleChoiceItems(files, 0) { _: DialogInterface, which: Int ->
            LogUtility.d(which)
            selectedItem = which
        }
        builder.setPositiveButton(R.string.ok) { _: DialogInterface, _: Int ->
            importSettings(
                Uri.fromFile(File(Global.BackupFolder, files[selectedItem]))
            )
        }
            .setNegativeButton(R.string.cancel, null)
        builder.show()
    }

    fun exportSettings() {
        val name = Exporter.defaultExportName(this)
        saveSettings.launch(name)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    @TargetApi(Build.VERSION_CODES.R)
    fun requestStorageManager() {
        if (requestStorageManager == null) {
            Toast.makeText(this, R.string.failed, Toast.LENGTH_SHORT).show()
            return
        }
        val builder = MaterialAlertDialogBuilder(this)
        builder.setIcon(R.drawable.ic_file_download)
        builder.setTitle(R.string.requesting_storage_access)
        builder.setMessage(R.string.request_storage_manager_summary)
        builder.setPositiveButton(R.string.ok) { _: DialogInterface, _: Int ->
            requestStorageManager!!.launch(
                null
            )
        }
            .setNegativeButton(R.string.cancel, null).show()
    }
}
