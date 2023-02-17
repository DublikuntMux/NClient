package com.dublikunt.nclient.adapters;

import android.database.Cursor;
import android.util.JsonWriter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;

import com.dublikunt.nclient.R;
import com.dublikunt.nclient.TagFilterActivity;
import com.dublikunt.nclient.api.components.Tag;
import com.dublikunt.nclient.enums.TagStatus;
import com.dublikunt.nclient.enums.TagType;
import com.dublikunt.nclient.async.database.Queries;
import com.dublikunt.nclient.settings.AuthRequest;
import com.dublikunt.nclient.settings.Global;
import com.dublikunt.nclient.settings.Login;
import com.dublikunt.nclient.settings.Tags;
import com.dublikunt.nclient.utility.ImageDownloadUtility;
import com.dublikunt.nclient.utility.LogUtility;
import com.dublikunt.nclient.utility.Utility;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Locale;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.RequestBody;
import okhttp3.Response;

public class TagsAdapter extends RecyclerView.Adapter<TagsAdapter.ViewHolder> implements Filterable {
    private final TagFilterActivity context;
    private final boolean logged = Login.isLogged();
    private final TagType type;
    private final TagMode tagMode;
    private String lastQuery = null;
    private boolean wasSortedByName;
    private Cursor cursor = null;
    private boolean force = false;

    public TagsAdapter(TagFilterActivity cont, String query, boolean online) {
        this.context = cont;
        this.type = null;
        this.tagMode = online ? TagMode.ONLINE : TagMode.OFFLINE;
        getFilter().filter(query);
    }

    public TagsAdapter(TagFilterActivity cont, String query, TagType type) {
        this.context = cont;
        this.type = type;
        this.tagMode = TagMode.TYPE;
        getFilter().filter(query);
    }

    /**
    * Writes a tag to the JSON. This is used to write tags that don't have a type in the schema
    *
    * @param jw - The JsonWriter to write to
    * @param tag - The tag to write to the JSON. This must be a
    */
    private static void writeTag(JsonWriter jw, Tag tag) throws IOException {
        jw.beginObject();
        jw.name("id").value(tag.getId());
        jw.name("name").value(tag.getName());
        jw.name("type").value(tag.getTypeSingleName());
        jw.endObject();
    }

    /**
    * Returns a Filter that can be used to filter the list. The Filter returned by this method is capable of performing filtering based on the query that was passed in and the user's preference.
    *
    *
    * @return a Filter that can be used to filter the list according to the user's preference ; null if there is no
    */
    @Override
    public Filter getFilter() {
        return new Filter() {
            /**
            * Filters the tags by the constraint. This is called by #filter ( CharSequence ) to perform the filtering.
            *
            * @param constraint - the constraint that should be used to filter the tags
            *
            * @return an object containing the number of tags that match the
            */
            @Override
            protected FilterResults performFiltering(CharSequence constraint) {
                FilterResults results = new FilterResults();
                // Set the constraint to the empty string.
                if (constraint == null) constraint = "";
                force = false;
                wasSortedByName = Tags.isSortedByName();

                lastQuery = constraint.toString();
                Cursor tags = Queries.TagTable.getFilterCursor(lastQuery, type, tagMode == TagMode.ONLINE, Tags.isSortedByName());
                results.count = tags.getCount();
                results.values = tags;

                LogUtility.download(results.count + "," + results.values);
                return results;
            }

            /**
            * Publishes the results of filtering. This is called by #filter ( CharSequence FilterResults ) to notify the listeners of the changes to the filter's results.
            *
            * @param constraint - the constraint that was used to filter the results
            * @param results - the results of filtering to be published to the
            */
            @Override
            protected void publishResults(CharSequence constraint, FilterResults results) {
                // Returns true if there are no results.
                if (results.count == -1) return;
                Cursor newCursor = (Cursor) results.values;
                int oldCount = getItemCount(), newCount = results.count;
                // Closes the cursor and closes the cursor.
                if (cursor != null) cursor.close();
                cursor = newCursor;
                // Notify the user that the item range has been inserted or removed.
                if (newCount > oldCount) notifyItemRangeInserted(oldCount, newCount - oldCount);
                else notifyItemRangeRemoved(newCount, oldCount - newCount);
                notifyItemRangeChanged(0, Math.min(newCount, oldCount));
            }
        };
    }

    /**
    * Creates and returns a ViewHolder to be used for the view. Override this method to inflate the view before it is added to the View hierarchy.
    *
    * @param parent - The ViewGroup in which the view will be placed.
    * @param viewType - The type of view being added to the View hierarchy
    */
    @NonNull
    @Override
    public TagsAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new TagsAdapter.ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.entry_tag_layout, parent, false));
    }

    /**
    * Binds the view to the position. This is called by the adapter when it is about to create a new view.
    *
    * @param holder - The view holder to bind to. Must not be null.
    * @param position - The position of the tag in the list. Must be between 0 and #getCount
    */
    @Override
    public void onBindViewHolder(@NonNull final TagsAdapter.ViewHolder holder, int position) {
        cursor.moveToPosition(position);
        final Tag ent = Queries.TagTable.cursorToTag(cursor);
        holder.title.setText(ent.getName());
        holder.count.setText(String.format(Locale.US, "%d", ent.getCount()));
        holder.master.setOnClickListener(v -> {
            // Update the tag mode.
            switch (tagMode) {
                case OFFLINE:
                case TYPE:
                    // Update the tag status of the tag.
                    if (Tags.maxTagReached() && ent.getStatus() == TagStatus.DEFAULT) {
                        context.runOnUiThread(() -> Toast.makeText(context, context.getString(R.string.tags_max_reached, Tags.MAXTAGS), Toast.LENGTH_LONG).show());
                    } else {
                        Tags.updateStatus(ent);
                        updateLogo(holder.imgView, ent.getStatus());
                    }
                    break;
                case ONLINE:
                    try {
                        onlineTagUpdate(ent, !Login.isOnlineTags(ent), holder.imgView);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    break;
            }

        });
        // Show a dialog to show a blacklist dialog if tag mode is onLINE.
        if (tagMode != TagMode.ONLINE && logged) holder.master.setOnLongClickListener(view -> {
            // Show a dialog to show a blacklist dialog if not online tags.
            if (!Login.isOnlineTags(ent)) showBlacklistDialog(ent, holder.imgView);
            else
                Toast.makeText(context, R.string.tag_already_in_blacklist, Toast.LENGTH_SHORT).show();
            return true;
        });
        updateLogo(holder.imgView, tagMode == TagMode.ONLINE ? TagStatus.AVOIDED : ent.getStatus());
    }

    /**
    * Returns the number of items in the cursor. This is a hint to the adapter that it should be able to return an item count
    */
    @Override
    public int getItemCount() {
        return cursor == null ? 0 : cursor.getCount();
    }

    /**
    * Shows a dialog asking if the user wants to add a tag to the blacklist. It's shown when the user clicks on the star icon in the black list
    *
    * @param tag - The tag to whom we want to add
    * @param imgView - The image view that will be used to fill the
    */
    private void showBlacklistDialog(final Tag tag, final ImageView imgView) {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(context);
        builder.setIcon(R.drawable.ic_star_border).setTitle(R.string.add_to_online_blacklist).setMessage(R.string.are_you_sure);
        builder.setPositiveButton(R.string.yes, (dialogInterface, i) -> {
            try {
                onlineTagUpdate(tag, true, imgView);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).setNegativeButton(R.string.no, null).show();
    }

    /**
    * Adds or removes a tag to / from the currently logged in user's tag list. This is used to allow the user to set or remove tags from their profile.
    *
    * @param tag - The tag to add or remove. If add is true the tag will be added to the profile. If add is false the tag will be removed from
    * @param add
    * @param imgView
    */
    private void onlineTagUpdate(final Tag tag, final boolean add, final ImageView imgView) throws IOException {
        // Returns true if the user is logged in or not.
        if (!Login.isLogged() || Login.getUser() == null) return;
        StringWriter sw = new StringWriter();
        JsonWriter jw = new JsonWriter(sw);
        jw.beginObject().name("added").beginArray();
        // Write the tag to the JW.
        if (add) writeTag(jw, tag);
        jw.endArray().name("removed").beginArray();
        // Write the tag to the JW.
        if (!add) writeTag(jw, tag);
        jw.endArray().endObject();

        final String url = String.format(Locale.US, Utility.getBaseUrl() + "users/%d/%s/blacklist", Login.getUser().getId(), Login.getUser().getCodename());
        final RequestBody ss = RequestBody.create(MediaType.get("application/json"), sw.toString());
        new AuthRequest(url, url, new Callback() {
            /**
            * Called when there is an error in the response. This is a no - op for calls that fail to complete.
            *
            * @param call - The Call that failed. Can be null.
            * @param e - The exception that caused the failure. Can be null
            */
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
            }

            /**
            * Called when the server responds. This is the callback method that will be called on every call. The implementation of this method in this class does not need to be synchronized as it is called in multiple threads
            *
            * @param call - The Call that sent the response
            * @param response - The Response that was
            */
            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                // Add or remove a tag.
                if (response.body().string().contains("ok")) {
                    // Add or remove a tag.
                    if (add) Login.addOnlineTag(tag);
                    else Login.removeOnlineTag(tag);
                    // Update the logo if the tag mode is onLINE.
                    if (tagMode == TagMode.ONLINE)
                        updateLogo(imgView, add ? TagStatus.AVOIDED : TagStatus.DEFAULT);
                }
            }
        }).setMethod("POST", ss).start();
    }

    /**
    * Updates the logo based on the status. This is called from background thread so we don't need to worry about thread safety
    *
    * @param img - The image to update.
    * @param s - The status of the tag ( void accepted or ignored
    */
    private void updateLogo(ImageView img, TagStatus s) {
        context.runOnUiThread(() -> {
            // Set the image drawable to the current state.
            switch (s) {
                case DEFAULT:
                    img.setImageDrawable(null);
                    break;//ImageDownloadUtility.loadImage(R.drawable.ic_void,img); break;
                case ACCEPTED:
                    ImageDownloadUtility.loadImage(R.drawable.ic_check, img);
                    Global.setTint(img.getDrawable());
                    break;
                case AVOIDED:
                    ImageDownloadUtility.loadImage(R.drawable.ic_close, img);
                    Global.setTint(img.getDrawable());
                    break;
            }
        });

    }

    /**
    * Adds a new item to the filter. This is called when the user clicks on the item button to make sure it is in the list
    */
    public void addItem() {
        force = true;
        getFilter().filter(lastQuery);
    }

    private enum TagMode {ONLINE, OFFLINE, TYPE}

    static class ViewHolder extends RecyclerView.ViewHolder {
        final ImageView imgView;
        final TextView title, count;
        final ConstraintLayout master;

        ViewHolder(View v) {
            super(v);
            imgView = v.findViewById(R.id.image);
            title = v.findViewById(R.id.title);
            count = v.findViewById(R.id.count);
            master = v.findViewById(R.id.master_layout);
        }
    }


}
