package com.dublikunt.nclient.adapters

import android.content.DialogInterface
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import com.dublikunt.nclient.R
import com.dublikunt.nclient.components.status.Status
import com.dublikunt.nclient.components.status.StatusManager
import com.dublikunt.nclient.settings.Global
import com.dublikunt.nclient.utility.Utility
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.skydoves.colorpickerview.ColorEnvelope
import com.skydoves.colorpickerview.ColorPickerDialog
import com.skydoves.colorpickerview.listeners.ColorEnvelopeListener

class StatusManagerAdapter(activity: AppCompatActivity) :
    RecyclerView.Adapter<StatusManagerAdapter.ViewHolder>() {
    private val statusList: MutableList<Status> = StatusManager.toList() as MutableList<Status>
    private val activity: AppCompatActivity
    private var newColor = 0

    init {
        this.activity = activity
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val resId = R.layout.entry_status
        val view = LayoutInflater.from(activity).inflate(resId, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        if (holder.bindingAdapterPosition == statusList.size) {
            holder.name.setText(R.string.add)
            holder.color.visibility = View.INVISIBLE
            holder.color.setBackgroundColor(Color.TRANSPARENT)
            holder.cancel.setIconResource(R.drawable.ic_add)
            Global.setTint(holder.cancel.icon)
            holder.cancel.setOnClickListener(null)
            holder.master.setOnClickListener { updateStatus(null) }
            return
        }
        val status = statusList[holder.bindingAdapterPosition]
        holder.name.text = status.name
        holder.color.visibility = View.VISIBLE
        holder.color.setBackgroundColor(status.opaqueColor())
        holder.cancel.setIconResource(R.drawable.ic_close)
        holder.master.setOnClickListener { updateStatus(status) }
        holder.cancel.setOnClickListener {
            StatusManager.remove(status)
            notifyItemRemoved(statusList.indexOf(status))
            statusList.remove(status)
        }
    }

    override fun getItemCount(): Int {
        return statusList.size + 1
    }

    override fun getItemViewType(position: Int): Int {
        return if (position == statusList.size) 1 else 0
    }

    private fun updateStatus(status: Status?) {
        val builder = MaterialAlertDialogBuilder(activity)
        val layout = View.inflate(activity, R.layout.dialog_add_status, null) as LinearLayout
        val name = layout.findViewById<TextInputEditText>(R.id.name)
        val btnColor = layout.findViewById<MaterialButton>(R.id.color)
        val color = status?.opaqueColor() ?: (Utility.RANDOM.nextInt() or -0x1000000)
        newColor = color
        btnColor.setBackgroundColor(color)
        name.setText(status?.name ?: "")
        btnColor.setOnClickListener {
            ColorPickerDialog.Builder(activity)
                .setTitle(R.string.сolor_selection)
                .setPositiveButton(R.string.confirm,
                    ColorEnvelopeListener { envelope: ColorEnvelope, _: Boolean ->
                        if (envelope.color == Color.WHITE || envelope.color == Color.BLACK) {
                            Toast.makeText(
                                activity,
                                R.string.invalid_color_selected,
                                Toast.LENGTH_SHORT
                            ).show()
                            return@ColorEnvelopeListener
                        }
                        newColor = envelope.color
                        btnColor.setBackgroundColor(envelope.color)
                    })
                .setNegativeButton(
                    R.string.cancel
                ) { dialogInterface: DialogInterface, _: Int -> dialogInterface.dismiss() }
                .attachAlphaSlideBar(false)
                .attachBrightnessSlideBar(true)
                .setBottomSpace(12)
                .show()
        }
        builder.setView(layout)
        builder.setTitle(if (status == null) R.string.create_new_status else R.string.update_status)
        builder.setPositiveButton(R.string.ok) { _: DialogInterface?, _: Int ->
            val newName = name.text.toString()
            if (newName.length < 2) {
                Toast.makeText(activity, R.string.name_too_short, Toast.LENGTH_SHORT).show()
                return@setPositiveButton
            }
            if (StatusManager.getByName(newName) != null && newName != status!!.name) {
                Toast.makeText(activity, R.string.duplicated_name, Toast.LENGTH_SHORT).show()
                return@setPositiveButton
            }
            val newStatus = StatusManager.updateStatus(status, name.text.toString(), newColor)
            if (status == null) {
                statusList.add(newStatus)
                statusList.sortWith { o1: Status, o2: Status ->
                    o1.name.compareTo(
                        o2.name,
                        ignoreCase = true
                    )
                }
                val index = statusList.indexOf(newStatus)
                notifyItemInserted(index)
            } else {
                val oldIndex = statusList.indexOf(status)
                statusList[oldIndex] = newStatus
                statusList.sortWith { o1: Status, o2: Status ->
                    o1.name.compareTo(
                        o2.name,
                        ignoreCase = true
                    )
                }
                val newIndex = statusList.indexOf(newStatus)
                notifyItemMoved(oldIndex, newIndex)
                notifyItemChanged(newIndex)
            }
        }
        builder.setNegativeButton(R.string.cancel, null)
        builder.show()
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var master: LinearLayout
        var color: MaterialButton
        var cancel: MaterialButton
        var name: TextView

        init {
            name = itemView.findViewById(R.id.name)
            cancel = itemView.findViewById(R.id.cancelButton)
            color = itemView.findViewById(R.id.color)
            master = itemView.findViewById(R.id.master_layout)
        }
    }
}
