package com.dublikunt.nclient.classes

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.MarginLayoutParams
import android.widget.ImageView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import com.dublikunt.nclient.R
import com.dublikunt.nclient.classes.MultichoiceAdapter.MultichoiceViewHolder
import com.dublikunt.nclient.utility.LogUtility.download

abstract class MultichoiceAdapter<D, T : RecyclerView.ViewHolder?> :
    RecyclerView.Adapter<MultichoiceViewHolder<T>>() {
    private val listeners: MutableList<MultichoiceListener> = ArrayList(3)
    var mode = Mode.NORMAL
        private set
    private val map: HashMap<Long, D> = object : HashMap<Long, D>() {
        override fun put(key: Long, value: D): D? {
            val res = super.put(key, value)
            if (size == 1) startSelecting()
            changeSelecting()
            return res
        }

        override fun remove(key: Long): D? {
            val res = super.remove(key)
            if (isEmpty()) endSelecting()
            changeSelecting()
            return res
        }

        override fun clear() {
            super.clear()
            endSelecting()
            changeSelecting()
        }
    }

    init {
        setHasStableIds(true)
    }

    private fun changeSelecting() {
        for (listener in listeners) listener.choiceChanged()
    }

    /**
     * Used only to do a put
     */
    protected abstract fun getItemAt(position: Int): D
    protected abstract fun getMaster(holder: T): ViewGroup
    protected abstract fun defaultMasterAction(position: Int)
    protected abstract fun onBindMultichoiceViewHolder(holder: T, position: Int)
    protected abstract fun onCreateMultichoiceViewHolder(parent: ViewGroup, viewType: Int): T
    abstract override fun getItemId(position: Int): Long
    private fun startSelecting() {
        mode = Mode.SELECTING
        for (listener in listeners) listener.firstChoice()
    }

    private fun endSelecting() {
        mode = Mode.NORMAL
        for (listener in listeners) listener.noMoreChoices()
    }

    fun addListener(listener: MultichoiceListener) {
        listeners.add(listener)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MultichoiceViewHolder<T> {
        val innerLayout = onCreateMultichoiceViewHolder(parent, viewType)
        val master = getMaster(innerLayout)
        val multiLayout = LayoutInflater.from(parent.context)
            .inflate(R.layout.multichoice_adapter, master, true) as ConstraintLayout
        return MultichoiceViewHolder(multiLayout, innerLayout)
    }

    override fun onBindViewHolder(holder: MultichoiceViewHolder<T>, position: Int) {
        val isSelected = map.containsKey(getItemId(holder.bindingAdapterPosition))
        val master: View = getMaster(holder.innerHolder)
        updateLayoutParams(master, holder.censor, isSelected)
        master.setOnClickListener {
            when (mode) {
                Mode.SELECTING -> toggleSelection(holder.bindingAdapterPosition)
                Mode.NORMAL -> defaultMasterAction(holder.bindingAdapterPosition)
            }
        }
        master.setOnLongClickListener { v: View? ->
            map[getItemId(holder.bindingAdapterPosition)] =
                getItemAt(holder.bindingAdapterPosition)
            notifyItemChanged(holder.bindingAdapterPosition)
            true
        }
        holder.censor.visibility = if (isSelected) View.VISIBLE else View.GONE
        holder.checkmark.visibility = if (isSelected) View.VISIBLE else View.GONE
        holder.censor.setOnClickListener { v: View? -> toggleSelection(holder.bindingAdapterPosition) }
        onBindMultichoiceViewHolder(holder.innerHolder, holder.bindingAdapterPosition)
    }

    private fun updateLayoutParams(master: View?, multichoiceHolder: View?, isSelected: Boolean) {
        if (master == null) return
        val margin = if (isSelected) 8 else 0
        val params = master.layoutParams as MarginLayoutParams
        params.setMargins(margin, margin, margin, margin)
        master.layoutParams = params
        if (isSelected && multichoiceHolder != null) {
            master.post(Runnable {
                val multiParam = multichoiceHolder.layoutParams
                multiParam.width = master.width
                multiParam.height = master.height
                download("Multiparam: " + multiParam.width + ", " + multiParam.height)
                multichoiceHolder.layoutParams = multiParam
            })
        }
    }

    private fun toggleSelection(position: Int) {
        val id = getItemId(position)
        if (map.containsKey(id)) map.remove(id) else map[id] = getItemAt(position)
        notifyItemChanged(position)
    }

    fun selectAll() {
        val count = itemCount
        for (i in 0 until count) map[getItemId(i)] = getItemAt(i)
        notifyItemRangeChanged(0, count)
    }

    val selected: Collection<D>
        get() = map.values

    fun deselectAll() {
        map.clear()
        notifyItemRangeChanged(0, itemCount)
    }

    enum class Mode {
        NORMAL, SELECTING
    }

    interface MultichoiceListener {
        fun firstChoice()
        fun noMoreChoices()
        fun choiceChanged()
    }

    open class DefaultMultichoiceListener : MultichoiceListener {
        override fun firstChoice() {}
        override fun noMoreChoices() {}
        override fun choiceChanged() {}
    }

    class MultichoiceViewHolder<T : RecyclerView.ViewHolder?>(
        val multichoiceHolder: ConstraintLayout,
        val innerHolder: T
    ) : RecyclerView.ViewHolder(
        innerHolder!!.itemView
    ) {
        val censor: View = multichoiceHolder.findViewById(R.id.censor)
        val checkmark: ImageView = multichoiceHolder.findViewById(R.id.checkmark)

    }
}
