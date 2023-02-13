package com.dublikunt.nclientv2.adapters;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import com.dublikunt.nclientv2.R;
import com.dublikunt.nclientv2.components.status.Status;
import com.dublikunt.nclientv2.components.status.StatusManager;
import com.dublikunt.nclientv2.settings.Global;
import com.dublikunt.nclientv2.utility.Utility;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.skydoves.colorpickerview.ColorPickerDialog;
import com.skydoves.colorpickerview.listeners.ColorEnvelopeListener;

import java.util.List;

public class StatusManagerAdapter extends RecyclerView.Adapter<StatusManagerAdapter.ViewHolder> {
    private final List<Status> statusList;
    private final AppCompatActivity activity;
    private int newColor;

    public StatusManagerAdapter(AppCompatActivity activity) {
        statusList = StatusManager.toList();
        this.activity = activity;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        int resId = R.layout.entry_status;
        View view = LayoutInflater.from(activity).inflate(resId, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        if (holder.getBindingAdapterPosition() == statusList.size()) {
            holder.name.setText(R.string.add);
            holder.color.setVisibility(View.INVISIBLE);
            holder.color.setBackgroundColor(Color.TRANSPARENT);
            holder.cancel.setIconResource(R.drawable.ic_add);
            Global.setTint(holder.cancel.getIcon());
            holder.cancel.setOnClickListener(null);
            holder.master.setOnClickListener(v -> updateStatus(null));
            return;
        }
        Status status = statusList.get(holder.getBindingAdapterPosition());
        holder.name.setText(status.name);
        holder.color.setVisibility(View.VISIBLE);
        holder.color.setBackgroundColor(status.opaqueColor());

        holder.cancel.setIconResource(R.drawable.ic_close);
        holder.master.setOnClickListener(v -> updateStatus(status));
        holder.cancel.setOnClickListener(v -> {
            StatusManager.remove(status);
            notifyItemRemoved(statusList.indexOf(status));
            statusList.remove(status);
        });
    }

    @Override
    public int getItemCount() {
        return statusList.size() + 1;
    }

    @Override
    public int getItemViewType(int position) {
        return position == statusList.size() ? 1 : 0;
    }

    private void updateStatus(@Nullable Status status) {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(activity);
        LinearLayout layout = (LinearLayout) View.inflate(activity, R.layout.dialog_add_status, null);
        TextInputEditText name = layout.findViewById(R.id.name);
        MaterialButton btnColor = layout.findViewById(R.id.color);
        int color = status == null ? Utility.RANDOM.nextInt() | 0xff000000 : status.opaqueColor();
        newColor = color;
        btnColor.setBackgroundColor(color);
        name.setText(status == null ? "" : status.name);
        btnColor.setOnClickListener(v ->
            new ColorPickerDialog.Builder(activity)
                .setTitle(R.string.сolor_selection)
                .setPositiveButton(R.string.confirm,
                    (ColorEnvelopeListener) (envelope, fromUser) -> {
                        if (envelope.getColor() == Color.WHITE || envelope.getColor() == Color.BLACK) {
                            Toast.makeText(activity, R.string.invalid_color_selected, Toast.LENGTH_SHORT).show();
                            return;
                        }
                        newColor = envelope.getColor();
                        btnColor.setBackgroundColor(envelope.getColor());
                    })
                .setNegativeButton(R.string.cancel,
                    (dialogInterface, i) -> dialogInterface.dismiss())
                .attachAlphaSlideBar(false)
                .attachBrightnessSlideBar(true)
                .setBottomSpace(12)
                .show());
        builder.setView(layout);
        builder.setTitle(status == null ? R.string.create_new_status : R.string.update_status);
        builder.setPositiveButton(R.string.ok, (dialog, which) -> {
            String newName = name.getText().toString();
            if (newName.length() < 2) {
                Toast.makeText(activity, R.string.name_too_short, Toast.LENGTH_SHORT).show();
                return;
            }
            if (StatusManager.getByName(newName) != null && !newName.equals(status.name)) {
                Toast.makeText(activity, R.string.duplicated_name, Toast.LENGTH_SHORT).show();
                return;
            }
            Status newStatus = StatusManager.updateStatus(status, name.getText().toString(), newColor);
            if (status == null) {
                statusList.add(newStatus);
                statusList.sort((o1, o2) -> o1.name.compareToIgnoreCase(o2.name));
                int index = statusList.indexOf(newStatus);
                notifyItemInserted(index);
            } else {
                int oldIndex = statusList.indexOf(status);
                statusList.set(oldIndex, newStatus);
                statusList.sort((o1, o2) -> o1.name.compareToIgnoreCase(o2.name));
                int newIndex = statusList.indexOf(newStatus);
                notifyItemMoved(oldIndex, newIndex);
                notifyItemChanged(newIndex);
            }

        });
        builder.setNegativeButton(R.string.cancel, null);
        builder.show();
    }


    public static class ViewHolder extends RecyclerView.ViewHolder {
        LinearLayout master;
        MaterialButton color;
        MaterialButton cancel;
        TextView name;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.name);
            cancel = itemView.findViewById(R.id.cancelButton);
            color = itemView.findViewById(R.id.color);
            master = itemView.findViewById(R.id.master_layout);
        }
    }
}