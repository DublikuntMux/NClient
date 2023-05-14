package com.dublikunt.nclient

import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.ArrayAdapter
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.SearchView
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.RecyclerView
import com.dublikunt.nclient.adapters.HistoryAdapter
import com.dublikunt.nclient.api.comments.Ranges
import com.dublikunt.nclient.api.comments.Tag
import com.dublikunt.nclient.async.database.Queries
import com.dublikunt.nclient.components.activities.GeneralActivity
import com.dublikunt.nclient.components.widgets.ChipTag
import com.dublikunt.nclient.components.widgets.CustomLinearLayoutManager
import com.dublikunt.nclient.enums.Language
import com.dublikunt.nclient.enums.SpecialTagIds
import com.dublikunt.nclient.enums.TagStatus
import com.dublikunt.nclient.enums.TagType
import com.dublikunt.nclient.settings.DefaultDialogs
import com.dublikunt.nclient.settings.DefaultDialogs.CustomDialogResults
import com.dublikunt.nclient.settings.DefaultDialogs.DialogResults
import com.dublikunt.nclient.settings.Global
import com.dublikunt.nclient.settings.Login
import com.dublikunt.nclient.utility.LogUtility
import com.dublikunt.nclient.utility.Utility
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import java.util.Locale

class SearchActivity : GeneralActivity() {
    private val ranges = Ranges()
    private val tags = ArrayList<ChipTag>()
    private val addChip = arrayOfNulls<Chip>(TagType.values.size)
    private lateinit var groups: Array<ChipGroup?>
    private lateinit var searchView: SearchView
    private lateinit var autoComplete: MaterialAutoCompleteTextView
    private lateinit var loadedTag: TagType
    private lateinit var adapter: HistoryAdapter
    private var advanced = false
    private var temporaryUnit: Ranges.TimeUnit? = null
    private var inputMethodManager: InputMethodManager? = null
    private lateinit var alertDialog: AlertDialog
    fun setQuery(str: String?, submit: Boolean) {
        runOnUiThread { searchView.setQuery(str, submit) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_search)
        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        assert(supportActionBar != null)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        supportActionBar!!.setDisplayShowTitleEnabled(false)
        inputMethodManager = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager

        searchView = findViewById(R.id.search)
        val recyclerView = findViewById<RecyclerView>(R.id.recycler)
        groups = arrayOf(
            null,
            findViewById(R.id.parody_group),
            findViewById(R.id.character_group),
            findViewById(R.id.tag_group),
            findViewById(R.id.artist_group),
            findViewById(R.id.group_group),
            findViewById(R.id.language_group),
            findViewById(R.id.category_group)
        )
        initRanges()
        adapter = HistoryAdapter(this)
        autoComplete = layoutInflater.inflate(
            R.layout.autocomplete_entry,
            findViewById(R.id.appbar),
            false
        ) as MaterialAutoCompleteTextView
        autoComplete.setOnEditorActionListener { _: TextView?, actionId: Int, _: KeyEvent? ->
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                alertDialog.dismiss()
                createChip()
                return@setOnEditorActionListener true
            }
            false
        }

        recyclerView.layoutManager = CustomLinearLayoutManager(this)
        recyclerView.adapter = adapter
        recyclerView.addItemDecoration(DividerItemDecoration(this, DividerItemDecoration.VERTICAL))
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String): Boolean {
                var query = query
                query = query.trim { it <= ' ' }
                if (query.isEmpty() && !advanced) return true
                if (query.isNotEmpty()) adapter.addHistory(query)
                val i = Intent(this@SearchActivity, MainActivity::class.java)
                i.putExtra("$packageName.SEARCHMODE", true)
                i.putExtra("$packageName.QUERY", query)
                i.putExtra("$packageName.ADVANCED", advanced)
                if (advanced) {
                    val tt = ArrayList<Tag>(tags.size)
                    for (t in tags) if (t.tag.status == TagStatus.ACCEPTED) tt.add(t.tag)
                    i.putParcelableArrayListExtra("$packageName.TAGS", tt)
                    i.putExtra("$packageName.RANGES", ranges)
                }
                runOnUiThread {
                    startActivity(i)
                    finish()
                }
                return true
            }

            override fun onQueryTextChange(newText: String): Boolean {
                return false
            }
        })
        populateGroup()
        searchView.requestFocus()
    }

    private fun createPageBuilder(
        title: Int,
        min: Int,
        max: Int,
        actual: Int,
        results: DialogResults
    ) {
        var min = min
        var actual = actual
        min = 1.coerceAtLeast(min)
        actual = actual.coerceAtLeast(min)
        DefaultDialogs.pageChangerDialog(
            DefaultDialogs.Builder(this)
                .setTitle(title)
                .setMax(max)
                .setMin(min)
                .setDrawable(R.drawable.ic_search)
                .setActual(actual)
                .setYesbtn(R.string.ok)
                .setNobtn(R.string.cancel)
                .setMaybebtn(R.string.reset)
                .setDialogs(results)
        )
    }

    private fun initRanges() {
        val pageRangeLayout = findViewById<LinearLayout>(R.id.page_range)
        val uploadRangeLayout = findViewById<LinearLayout>(R.id.upload_range)
        (pageRangeLayout.findViewById<View>(R.id.title) as TextView).setText(R.string.page_range)
        (uploadRangeLayout.findViewById<View>(R.id.title) as TextView).setText(R.string.upload_time)
        val fromPage = pageRangeLayout.findViewById<MaterialButton>(R.id.fromButton)
        val toPage = pageRangeLayout.findViewById<MaterialButton>(R.id.toButton)
        val fromDate = uploadRangeLayout.findViewById<MaterialButton>(R.id.fromButton)
        val toDate = uploadRangeLayout.findViewById<MaterialButton>(R.id.toButton)
        fromPage.setOnClickListener {
            createPageBuilder(
                R.string.from_page,
                0,
                2000,
                ranges.fromPage,
                object : CustomDialogResults() {
                    override fun positive(actual: Int) {
                        ranges.fromPage = actual
                        fromPage.text = String.format(Locale.US, "%d", actual)
                        if (ranges.fromPage > ranges.toPage) {
                            ranges.toPage = Ranges.UNDEFINED
                            toPage.text = ""
                        }
                        advanced = true
                    }

                    override fun neutral() {
                        ranges.fromPage = Ranges.UNDEFINED
                        fromPage.text = ""
                    }
                })
        }
        toPage.setOnClickListener {
            createPageBuilder(
                R.string.to_page,
                ranges.fromPage,
                2000,
                ranges.toPage,
                object : CustomDialogResults() {
                    override fun positive(actual: Int) {
                        ranges.toPage = actual
                        toPage.text = String.format(Locale.US, "%d", actual)
                        advanced = true
                    }

                    override fun neutral() {
                        ranges.toPage = Ranges.UNDEFINED
                        toPage.text = ""
                    }
                })
        }
        fromDate.setOnClickListener { showUnitDialog(fromDate, true) }
        toDate.setOnClickListener { showUnitDialog(toDate, false) }
    }

    private fun showUnitDialog(button: MaterialButton, from: Boolean) {
        var i = 0
        val strings = arrayOfNulls<String>(Ranges.TimeUnit.values().size)
        for (unit in Ranges.TimeUnit.values()) strings[i++] = getString(unit.string)
        val builder = MaterialAlertDialogBuilder(this)
        builder.setTitle(R.string.choose_unit)
        builder.setIcon(R.drawable.ic_search)
        builder.setAdapter(
            ArrayAdapter(
                this,
                android.R.layout.simple_dropdown_item_1line,
                strings
            )
        ) { _: DialogInterface?, which: Int ->
            temporaryUnit = Ranges.TimeUnit.values()[which]
            createPageBuilder(
                if (from) R.string.from_time else R.string.to_time,
                1,
                if (temporaryUnit == Ranges.TimeUnit.YEAR) 10 else 100,
                1,
                object : CustomDialogResults() {
                    override fun positive(actual: Int) {
                        if (from) {
                            ranges.fromDateUnit = temporaryUnit
                            ranges.fromDate = actual
                        } else {
                            ranges.toDateUnit = temporaryUnit
                            ranges.toDate = actual
                        }
                        button.text = String.format(
                            Locale.US,
                            "%d %c",
                            actual,
                            temporaryUnit!!.`val`.uppercaseChar()
                        )
                        advanced = true
                    }

                    override fun neutral() {
                        if (from) {
                            ranges.fromDateUnit = Ranges.UNDEFINED_DATE
                            ranges.fromDate = Ranges.UNDEFINED
                        } else {
                            ranges.toDateUnit = Ranges.UNDEFINED_DATE
                            ranges.toDate = Ranges.UNDEFINED
                        }
                        button.text = ""
                    }
                })
        }
        builder.setNeutralButton(R.string.reset) { _: DialogInterface?, _: Int ->
            if (from) {
                ranges.fromDateUnit = Ranges.UNDEFINED_DATE
                ranges.fromDate = Ranges.UNDEFINED
            } else {
                ranges.toDateUnit = Ranges.UNDEFINED_DATE
                ranges.toDate = Ranges.UNDEFINED
            }
            button.text = ""
        }
        builder.show()
    }

    private fun populateGroup() {
        for (type in arrayOf(
            TagType.TAG,
            TagType.PARODY,
            TagType.CHARACTER,
            TagType.ARTIST,
            TagType.GROUP
        )) {
            for (t in Queries.TagTable.getTopTags(
                type,
                Global.getFavoriteLimit(this)
            )) addChipTag(t, close = true, canBeAvoided = true)
        }

        for (t in Queries.TagTable.allFiltered) if (!tagAlreadyExist(t)) addChipTag(
            t,
            close = true,
            canBeAvoided = true
        )

        for (t in Queries.TagTable.getTrueAllType(TagType.CATEGORY)) addChipTag(
            t,
            close = false,
            canBeAvoided = false
        )

        for (t in Queries.TagTable.getTrueAllType(TagType.LANGUAGE)) {
            if (t.id == SpecialTagIds.LANGUAGE_ENGLISH.toInt() && Global.onlyLanguage == Language.ENGLISH) t.status =
                TagStatus.ACCEPTED else if (t.id == SpecialTagIds.LANGUAGE_JAPANESE.toInt() && Global.onlyLanguage == Language.JAPANESE) t.status =
                TagStatus.ACCEPTED else if (t.id == SpecialTagIds.LANGUAGE_CHINESE.toInt() && Global.onlyLanguage == Language.CHINESE) t.status =
                TagStatus.ACCEPTED
            addChipTag(t, false, canBeAvoided = false)
        }

        if (Login.useAccountTag()) for (t in Queries.TagTable.allOnlineBlacklisted) if (!tagAlreadyExist(
                t
            )
        ) addChipTag(t, true, canBeAvoided = true)

        for (type in TagType.values) {
            if (type === TagType.UNKNOWN || type === TagType.LANGUAGE || type === TagType.CATEGORY) {
                addChip[type.id.toInt()] = null
                continue
            }
            val cg = getGroup(type)
            val add = createAddChip(type, cg)
            addChip[type.id.toInt()] = add
            cg!!.addView(add)
        }
    }

    private fun createAddChip(type: TagType, group: ChipGroup?): Chip {
        val c = layoutInflater.inflate(R.layout.chip_layout, group, false) as Chip
        c.isCloseIconVisible = false
        c.setChipIconResource(R.drawable.ic_add)
        c.text = getString(R.string.add)
        c.setOnClickListener { loadTag(type) }
        Global.setTint(c.chipIcon)
        return c
    }

    private fun tagAlreadyExist(tag: Tag): Boolean {
        for (t in tags) {
            if (t.tag.name == tag.name) return true
        }
        return false
    }

    private fun addChipTag(t: Tag, close: Boolean, canBeAvoided: Boolean) {
        val cg = getGroup(t.type)
        val c = layoutInflater.inflate(R.layout.chip_layout_entry, cg, false) as ChipTag
        c.init(t, close, canBeAvoided)
        c.setOnCloseIconClickListener {
            cg!!.removeView(c)
            tags.remove(c)
            advanced = true
        }
        c.setOnClickListener {
            c.updateStatus()
            advanced = true
        }
        cg!!.addView(c)
        tags.add(c)
    }

    private fun loadDropdown(type: TagType) {
        val allTags = Queries.TagTable.getAllTagOfType(type)
        val tagNames = arrayOfNulls<String>(allTags.size)
        var i = 0
        for (t in allTags) tagNames[i++] = t.name
        autoComplete.setAdapter(
            ArrayAdapter(
                this@SearchActivity,
                android.R.layout.simple_dropdown_item_1line,
                tagNames
            )
        )
        loadedTag = type
    }

    private fun loadTag(type: TagType) {
        if (type !== loadedTag) loadDropdown(type)
        addDialog()
        autoComplete.requestFocus()
        inputMethodManager!!.showSoftInput(autoComplete, InputMethodManager.SHOW_IMPLICIT)
    }

    private fun getGroup(type: TagType): ChipGroup? {
        return groups[type.id.toInt()]
    }

    private fun addDialog() {
        val builder = MaterialAlertDialogBuilder(this)
        builder.setView(autoComplete)
        autoComplete.setText("")
        builder.setPositiveButton(R.string.ok) { _: DialogInterface?, _: Int -> createChip() }
        builder.setCancelable(true).setNegativeButton(R.string.cancel, null)
        builder.setTitle(R.string.insert_tag_name)
        alertDialog = try {
            builder.show()
        } catch (e: IllegalStateException) {
            (autoComplete.parent as ViewGroup).removeView(autoComplete)
            builder.show()
        }
    }

    private fun createChip() {
        val name = autoComplete.text.toString().lowercase()
        var tag = Queries.TagTable.searchTag(name, loadedTag)
        if (tag == null) tag = Tag(name, 0, customId++, loadedTag, TagStatus.ACCEPTED)
        LogUtility.download("CREATED WITH ID: " + tag.id)
        if (tagAlreadyExist(tag)) return

        if (getGroup(loadedTag) != null) getGroup(loadedTag)!!.removeView(addChip[loadedTag.id.toInt()])
        addChipTag(tag, close = true, canBeAvoided = true)
        getGroup(loadedTag)!!.addView(addChip[loadedTag.id.toInt()])
        inputMethodManager!!.hideSoftInputFromWindow(
            searchView.windowToken,
            InputMethodManager.SHOW_IMPLICIT
        )
        autoComplete.setText("")
        advanced = true
        if (autoComplete.parent != null) (autoComplete.parent as ViewGroup).removeView(
            autoComplete
        )
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.search, menu)
        Utility.tintMenu(menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        } else if (item.itemId == R.id.view_groups) {
            val v = findViewById<View>(R.id.groups)
            val isVisible = v.visibility == View.VISIBLE
            v.visibility = if (isVisible) View.GONE else View.VISIBLE
            item.setIcon(if (isVisible) R.drawable.ic_add else R.drawable.ic_close)
            Global.setTint(item.icon)
        }
        return super.onOptionsItemSelected(item)
    }

    companion object {
        private const val CUSTOM_ID_START = 100000000
        private var customId = CUSTOM_ID_START
    }
}
