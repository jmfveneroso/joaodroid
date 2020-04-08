package com.example.joaodroid;

import androidx.recyclerview.widget.RecyclerView;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.example.joaodroid.LogEntryFragment.OnListFragmentInteractionListener;
import com.example.joaodroid.LogReader.LogEntry;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * {@link RecyclerView.Adapter} that can display a {@link LogEntry} and makes a call to the
 * specified {@link OnListFragmentInteractionListener}.
 * TODO: Replace the implementation with code for your data type.
 */
public class LogEntryRecyclerViewAdapter extends RecyclerView.Adapter<LogEntryRecyclerViewAdapter.ViewHolder> {

    private List<LogEntry> mValues;
    private final OnListFragmentInteractionListener mListener;
    private ViewGroup mParent;
    private LogEntry mRecentlyDeletedItem;
    private int mRecentlyDeletedPosition;

    public LogEntryRecyclerViewAdapter(List<LogEntry> items, OnListFragmentInteractionListener listener) {
        mValues = items;
        mListener = listener;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.fragment_logentry, parent, false);
        mParent = parent;
        return new ViewHolder(view);
    }

    public String getTimeString(LocalDateTime date) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        return formatter.format(date);
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, int position) {
        holder.mItem = mValues.get(position);

        holder.mModifiedAtView.setText(getTimeString(holder.mItem.modifiedAt));
        holder.mCreatedAtView.setText(getTimeString(holder.mItem.datetime));

        StringBuilder sb = new StringBuilder();
        for (String t : mValues.get(position).tags) {
            sb.append(t + " ");
        }

        /*if (mValues.get(position).score > 0) {
            strDate += " : " + String.format("%.5f", mValues.get(position).score);
        }*/

        holder.mTitleView.setText(mValues.get(position).title);
        holder.mTagsView.setText(sb.toString());

        holder.mView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (null != mListener) {
                    mListener.onListFragmentInteraction(holder.mItem);
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return mValues.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        public final View mView;
        public final TextView mCreatedAtView;
        public final TextView mModifiedAtView;
        public final TextView mTitleView;
        public final TextView mTagsView;
        public LogEntry mItem;

        public ViewHolder(View view) {
            super(view);
            mView = view;
            mCreatedAtView = view.findViewById(R.id.created_at);
            mModifiedAtView = view.findViewById(R.id.modified_at);
            mTitleView = view.findViewById(R.id.timestamp);
            mTagsView = view.findViewById(R.id.tags);
        }

        @Override
        public String toString() {
            return super.toString() + " '" + mTitleView.getText() + "'";
        }
    }

    public void setItems(List<LogEntry> items) {
        mValues = items;
    }

    public void deleteItem(int position) {
        LogEntry logEntry = mValues.get(position);
        mRecentlyDeletedItem = logEntry;
        mRecentlyDeletedPosition = position;

        LogReader.deleteLogEntry(mParent.getContext(), logEntry);
        mValues.remove(position);
        notifyItemRemoved(position);
    }

    public void undoDelete() {
        LogReader.reinsertLogEntry(mParent.getContext(), mRecentlyDeletedItem);
        mValues.add(mRecentlyDeletedPosition, mRecentlyDeletedItem);

        notifyItemInserted(mRecentlyDeletedPosition);
        mRecentlyDeletedItem = null;
        mRecentlyDeletedPosition = -1;
    }
}
