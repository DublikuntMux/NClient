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
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import com.dublikunt.nclient.async.database.export.Exporter
import com.dublikunt.nclient.async.database.export.Manager
import com.dublikunt.nclient.components.activities.GeneralActivity
import com.dublikunt.nclient.components.views.GeneralPreferenceFragment
import com.dublikunt.nclient.settings.*
import com.dublikunt.nclient.settings.Global.isExternalStorageManager
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class SettingsActivity : GeneralActivity() {
    lateinit var fragment: GeneralPreferenceFragment
    private lateinit var IMPORT_ZIP: ActivityResultLauncher<String>
    private lateinit var SAVE_SETTINGS: ActivityResultLauncher<String>
    private lateinit var REQUEST_STORAGE_MANAGER: ActivityResultLauncher<Any?>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        registerActivities()
        setContentView(R.layout.activity_settings)
        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar!!.setTitle(R.string.settings)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        supportActionBar!!.setDisplayShowTitleEnabled(true)
        fragment =
            (supportFragmentManager.findFragmentById(R.id.fragment) as GeneralPreferenceFragment?)!!
        fragment.setAct(this)
        fragment.setType(
            Type.values()[intent.getIntExtra(
                "$packageName.TYPE",
                Type.MAIN.ordinal
            )]
        )
    }

    private fun registerActivities() {
        IMPORT_ZIP =
            registerForActivityResult(ActivityResultContracts.GetContent()) { selectedFile: Uri? ->
                if (selectedFile == null) return@registerForActivityResult
                importSettings(selectedFile)
            }
        SAVE_SETTINGS = registerForActivityResult(object :
            ActivityResultContracts.CreateDocument("application/zip") {
            override fun createIntent(context: Context, input: String): Intent {
                val i: Intent = super.createIntent(context, input)
                i.type = "application/zip"
                return i
            }
        }, ActivityResultCallback registerForActivityResult@{ selectedFile: Uri? ->
            if (selectedFile == null) return@registerForActivityResult
            exportSettings(selectedFile)
        })
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            REQUEST_STORAGE_MANAGER =
                registerForActivityResult(object : ActivityResultContract<Any?, Any?>() {
                    @RequiresApi(api = Build.VERSION_CODES.R)
                    override fun createIntent(context: Context, input: Any?): Intent {
                        val i = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                        i.data = Uri.parse("package:$packageName")
                        return i
                    }

                    override fun parseResult(resultCode: Int, intent: Intent?) {
                    }
                }) {
                    if (isExternalStorageManager) {
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
        }.start()
    }

    fun importSettings() {
        IMPORT_ZIP.launch("application/zip")
    }

    fun exportSettings() {
        val name = Exporter.defaultExportName(this)
        SAVE_SETTINGS.launch(name)
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
        val builder = MaterialAlertDialogBuilder(this)
        builder.setIcon(R.drawable.ic_file_download)
        builder.setTitle(R.string.requesting_storage_access)
        builder.setMessage(R.string.request_storage_manager_summary)
        builder.setPositiveButton(R.string.ok) { _: DialogInterface?, _: Int ->
            REQUEST_STORAGE_MANAGER.launch(
                null
            )
        }
            .setNegativeButton(R.string.cancel, null).show()
    }

    enum class Type {
        MAIN, COLUMN, DATA
    }
}
