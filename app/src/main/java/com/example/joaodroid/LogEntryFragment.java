package com.example.joaodroid;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.joaodroid.LogReader.LogEntry;
import com.google.android.material.snackbar.Snackbar;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

/**
 * A fragment representing a list of Items.
 * <p/>
 * Activities containing this fragment MUST implement the {@link OnListFragmentInteractionListener}
 * interface.
 */
public class LogEntryFragment extends Fragment {

    public class SwipeToDeleteCallback extends ItemTouchHelper.SimpleCallback {
        private View mView;
        private LogEntryRecyclerViewAdapter mAdapter;

        public SwipeToDeleteCallback(LogEntryRecyclerViewAdapter adapter, View view) {
            super(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT);
            mAdapter = adapter;
            mView = view;
        }

        @Override
        public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
            return false;
        }

        @Override
        public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
            int position = viewHolder.getAdapterPosition();
            mAdapter.deleteItem(position);
            showUndoSnackbar();
        }

        private void showUndoSnackbar() {
            Snackbar snackbar = Snackbar.make(mView, "Log entry was deleted", Snackbar.LENGTH_LONG);
            snackbar.setAction("Undo", v -> undoDelete());
            snackbar.show();
        }

        private void undoDelete() {
            mAdapter.undoDelete();
        }
    }

    // TODO: Customize parameter argument names
    private static final String ARG_COLUMN_COUNT = "column-count";
    // TODO: Customize parameters
    private int mColumnCount = 1;
    private OnListFragmentInteractionListener mListener;

    public LogEntryRecyclerViewAdapter mAdapter;
    private List<LogEntry> items;
    private String query;

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public LogEntryFragment() {
    }

    // TODO: Customize parameter initialization
    @SuppressWarnings("unused")
    public static LogEntryFragment newInstance(int columnCount) {
        LogEntryFragment fragment = new LogEntryFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_COLUMN_COUNT, columnCount);
        fragment.setArguments(args);
        return fragment;
    }

    private ArrayList<String> tokenize(String s) {
        String[] arr = s.toLowerCase().split("[ \n\t]+|[:=(),.'?]");
        return new ArrayList<>(Arrays.asList(arr));
    }

    private void updateList() {
        items = new ArrayList<>();

        ArrayList<LogEntry> items2 = LogReader.getLogs();

        // Search.
        if (query != null && !query.equals("")) {
            if (LogReader.logEntriesByTag.keySet().contains(query.toLowerCase())) {
                items = LogReader.logEntriesByTag.get(query.toLowerCase());
            } else {
                ArrayList<String> query_tkns = tokenize(query);
                HashMap<String, Integer> dict = new HashMap<>();
                for (int i = 0; i < items2.size(); i++) {
                    ArrayList<String> tkns = new ArrayList<>();
                    tkns.addAll(tokenize(items2.get(i).title));
                    tkns.addAll(tokenize(items2.get(i).getContent()));
                    for (String t : tkns) {
                        if (dict.containsKey(t)) {
                            dict.put(t, dict.get(t) + 1);
                        } else {
                            dict.put(t, 1);
                        }
                    }
                }

                for (int i = 0; i < items2.size(); i++) {
                    ArrayList<String> tkns = new ArrayList<>();
                    tkns.addAll(tokenize(items2.get(i).title));
                    tkns.addAll(tokenize(items2.get(i).getContent()));

                    double score = 0.0;
                    double norm = 0.0;
                    for (String t : tkns) {
                        if (dict.containsKey(t)) {
                            if (query_tkns.contains(t)) {
                                score += Math.pow(1.0 / (double) dict.get(t), 2);
                            }
                            norm += Math.pow(1.0 / (double) dict.get(t), 2);
                        }
                    }
                    if (norm > 0) {
                        score /= Math.sqrt(norm);
                    }
                    items2.get(i).score = score;
                }

                for (int i = 0; i < items2.size(); i++) {
                    if (items2.get(i).score == 0) continue;
                    else items.add(items2.get(i));
                }

                Collections.sort(items, new Comparator<LogEntry>() {
                    public int compare(LogEntry o1, LogEntry o2) {
                        if (o2.score == o1.score) return 0;
                        if (o2.score > o1.score) return 1;
                        return -1;
                    }
                });
            }
        } else {
            items = items2;
            Collections.sort(items, new Comparator<LogEntry>() {
                public int compare(LogEntry o1, LogEntry o2) {
                    return o2.modifiedAt.compareTo(o1.modifiedAt);
                }
            });
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        updateList();

        ((LogActivity) getActivity()).setFragmentRefreshListener(new LogActivity.FragmentRefreshListener() {
            @Override
            public void onRefresh(String q) {
                query = q;
                LogReader.update(getContext());
                updateList();
                mAdapter.setItems(items);
                mAdapter.notifyDataSetChanged();
            }
        });
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_logentry_list, container, false);

        // Set the adapter
        if (view instanceof RecyclerView) {
            Context context = view.getContext();
            RecyclerView recyclerView = (RecyclerView) view;
            recyclerView.addItemDecoration(new DividerItemDecoration(recyclerView.getContext(), DividerItemDecoration.VERTICAL));
            if (mColumnCount <= 1) {
                recyclerView.setLayoutManager(new LinearLayoutManager(context));
            } else {
                recyclerView.setLayoutManager(new GridLayoutManager(context, mColumnCount));
            }

            mAdapter = new LogEntryRecyclerViewAdapter(items, mListener);
            recyclerView.setAdapter(mAdapter);
            ItemTouchHelper itemTouchHelper = new
                    ItemTouchHelper(new SwipeToDeleteCallback(mAdapter, view));
            itemTouchHelper.attachToRecyclerView(recyclerView);
        }

        return view;
    }


    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnListFragmentInteractionListener) {
            mListener = (OnListFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnListFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p/>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnListFragmentInteractionListener {
        // TODO: Update argument type and name
        void onListFragmentInteraction(LogEntry item);
    }
}
