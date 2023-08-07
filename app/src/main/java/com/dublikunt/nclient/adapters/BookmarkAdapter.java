package com.dublikunt.nclient.adapters;

import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;

import com.dublikunt.nclient.BookmarkActivity;
import com.dublikunt.nclient.MainActivity;
import com.dublikunt.nclient.R;
import com.dublikunt.nclient.async.database.Queries;
import com.dublikunt.nclient.components.classes.Bookmark;
import com.dublikunt.nclient.utility.IntentUtility;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textview.MaterialTextView;

import java.util.List;

public class BookmarkAdapter extends RecyclerView.Adapter<BookmarkAdapter.ViewHolder> {
    private static final int LAYOUT = R.layout.bookmark_layout;

    private final List<Bookmark> bookmarks;
    private final BookmarkActivity bookmarkActivity;

    public BookmarkAdapter(BookmarkActivity bookmarkActivity) {
        this.bookmarkActivity = bookmarkActivity;
        this.bookmarks = Queries.BookmarkTable.getBookmarks();
    }


    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(LayoutInflater.from(parent.getContext()).inflate(LAYOUT, parent, false));
    }


    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int pos) {
        final int position = holder.getBindingAdapterPosition();
        Bookmark bookmark = bookmarks.get(position);

        holder.queryText.setText(bookmark.toString());
        holder.pageLabel.setText(bookmarkActivity.getString(R.string.bookmark_page_format, bookmark.page));

        holder.deleteButton.setOnClickListener(v -> removeBookmarkAtPosition(position));

        holder.rootLayout.setOnClickListener(v -> loadBookmark(bookmark));
    }

    private void loadBookmark(@NonNull Bookmark bookmark) {
        Intent i = new Intent(bookmarkActivity, MainActivity.class);
        i.putExtra(bookmarkActivity.getPackageName() + ".BYBOOKMARK", true);
        i.putExtra(bookmarkActivity.getPackageName() + ".INSPECTOR", bookmark.createInspector(bookmarkActivity, null));
        IntentUtility.startAnotherActivity(bookmarkActivity, i);
    }

    private void removeBookmarkAtPosition(int position) {
        if (position >= bookmarks.size()) return;
        Bookmark bookmark = bookmarks.get(position);
        bookmark.deleteBookmark();
        bookmarks.remove(bookmark);
        bookmarkActivity.runOnUiThread(() -> notifyItemRemoved(position));
    }

    @Override
    public int getItemCount() {
        return bookmarks.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final MaterialButton deleteButton;
        final MaterialTextView queryText;
        final MaterialTextView pageLabel;
        final ConstraintLayout rootLayout;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            deleteButton = itemView.findViewById(R.id.remove_button);
            pageLabel = itemView.findViewById(R.id.page);
            queryText = itemView.findViewById(R.id.title);
            rootLayout = itemView.findViewById(R.id.master_layout);
        }
    }
}
