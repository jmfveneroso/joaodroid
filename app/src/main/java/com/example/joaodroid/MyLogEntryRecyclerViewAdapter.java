package com.example.joaodroid;

import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.example.joaodroid.LogEntryFragment.OnListFragmentInteractionListener;
import com.example.joaodroid.dummy.DummyContent.DummyItem;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.List;

/**
 * {@link RecyclerView.Adapter} that can display a {@link DummyItem} and makes a call to the
 * specified {@link OnListFragmentInteractionListener}.
 * TODO: Replace the implementation with code for your data type.
 */
public class MyLogEntryRecyclerViewAdapter extends RecyclerView.Adapter<MyLogEntryRecyclerViewAdapter.ViewHolder> {

    private List<DummyItem> mValues;
    private final OnListFragmentInteractionListener mListener;

    public MyLogEntryRecyclerViewAdapter(List<DummyItem> items, OnListFragmentInteractionListener listener) {
        mValues = items;
        mListener = listener;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.fragment_logentry, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, int position) {
        holder.mItem = mValues.get(position);

        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String strDate = dateFormat.format(holder.mItem.timestamp);

        StringBuilder sb = new StringBuilder();
        for (String t : mValues.get(position).tags) {
            sb.append(t + " ");
        }

        if (mValues.get(position).score > 0) {
            strDate += " : " + String.format("%.5f", mValues.get(position).score);;
        }
        holder.mIdView.setText(strDate);
        holder.mTitleView.setText(mValues.get(position).title);
        holder.mTagsView.setText(sb.toString());

        holder.mView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (null != mListener) {
                    // Notify the active callbacks interface (the activity, if the
                    // fragment is attached to one) that an item has been selected.
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
        public final TextView mIdView;
        public final TextView mTitleView;
        public final TextView mTagsView;
        public DummyItem mItem;

        public ViewHolder(View view) {
            super(view);
            mView = view;
            mIdView = (TextView) view.findViewById(R.id.id_timestamp);
            mTitleView = (TextView) view.findViewById(R.id.timestamp);
            mTagsView = (TextView) view.findViewById(R.id.tags);
        }

        @Override
        public String toString() {
            return super.toString() + " '" + mTitleView.getText() + "'";
        }
    }

    public void setItems(List<DummyItem> items) {
        mValues = items;
    }
}
