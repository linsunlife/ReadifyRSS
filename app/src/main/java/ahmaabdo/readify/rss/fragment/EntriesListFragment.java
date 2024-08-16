/**
 * spaRSS
 * <p/>
 * Copyright (c) 2015-2016 Arnaud Renaud-Goud
 * Copyright (c) 2012-2015 Frederic Julian
 * <p/>
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * <p/>
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * <p/>
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package ahmaabdo.readify.rss.fragment;

import android.app.LoaderManager;
import android.content.*;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.BaseColumns;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.SearchView;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import com.daimajia.androidanimations.library.Techniques;
import com.daimajia.androidanimations.library.YoYo;

import java.util.ArrayList;
import java.util.Date;

import ahmaabdo.readify.rss.Constants;
import ahmaabdo.readify.rss.R;
import ahmaabdo.readify.rss.activity.EditFeedsListActivity;
import ahmaabdo.readify.rss.activity.GeneralPrefsActivity;
import ahmaabdo.readify.rss.adapter.EntriesCursorAdapter;
import ahmaabdo.readify.rss.provider.FeedData;
import ahmaabdo.readify.rss.provider.FeedData.EntryColumns;
import ahmaabdo.readify.rss.provider.FeedDataContentProvider;
import ahmaabdo.readify.rss.service.FetcherService;
import ahmaabdo.readify.rss.utils.PrefUtils;
import ahmaabdo.readify.rss.utils.UiUtils;

public class EntriesListFragment extends SwipeRefreshListFragment implements ViewTreeObserver.OnScrollChangedListener {

    private static final String STATE_CURRENT_URI = "STATE_CURRENT_URI";
    private static final String STATE_ORIGINAL_URI = "STATE_ORIGINAL_URI";
    private static final String STATE_SHOW_FEED_INFO = "STATE_SHOW_FEED_INFO";
    private static final String STATE_LIST_DISPLAY_DATE = "STATE_LIST_DISPLAY_DATE";

    private static final int ENTRIES_LOADER_ID = 1;
    private static final int NEW_ENTRIES_NUMBER_LOADER_ID = 2;

    SwipeRefreshLayout mySwipeRefreshLayout;
    private final OnSharedPreferenceChangeListener mPrefListener = new OnSharedPreferenceChangeListener() {
        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            if (PrefUtils.SHOW_READ.equals(key)) {
                mListDisplayDate = new Date().getTime();
                getLoaderManager().restartLoader(ENTRIES_LOADER_ID, null, mEntriesLoader);
            } else if (PrefUtils.IS_REFRESHING.equals(key)) {
                refreshSwipeProgress();
            }
        }
    };
    private Button mRefreshListBtn;
    private Uri mUri, mOriginalUri;
    private boolean mShowFeedInfo = false;
    private EntriesCursorAdapter mEntriesCursorAdapter;
    private Cursor mJustMarkedAsReadEntries;
    private ListView mListView;
    private long mListDisplayDate = new Date().getTime();
    private final LoaderManager.LoaderCallbacks<Cursor> mEntriesLoader = new LoaderManager.LoaderCallbacks<Cursor>() {
        @Override
        public Loader<Cursor> onCreateLoader(int id, Bundle args) {
            String entriesOrder = PrefUtils.getBoolean(PrefUtils.DISPLAY_OLDEST_FIRST, false) ? Constants.DB_ASC : Constants.DB_DESC;
            String where = "(" + EntryColumns.FETCH_DATE + Constants.DB_IS_NULL + Constants.DB_OR + EntryColumns.FETCH_DATE + "<=" + mListDisplayDate + ')';
            if (!FeedData.shouldShowReadEntries(mUri)) {
                where += Constants.DB_AND + "(" + EntryColumns.WHERE_UNREAD + Constants.DB_OR + EntryColumns.READ_DATE + ">" + mListDisplayDate + ")";
            }
            CursorLoader cursorLoader = new CursorLoader(getActivity(), mUri, null, where, null, EntryColumns.DATE + entriesOrder);
            cursorLoader.setUpdateThrottle(150);
            return cursorLoader;
        }

        @Override
        public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
            mEntriesCursorAdapter.swapCursor(data);
        }

        @Override
        public void onLoaderReset(Loader<Cursor> loader) {
            mEntriesCursorAdapter.swapCursor(Constants.EMPTY_CURSOR);
        }
    };
    private Menu menu;
    private int mNewEntriesNumber, mOldUnreadEntriesNumber = -1;
    private boolean mAutoRefreshDisplayDate = false;
    private final LoaderManager.LoaderCallbacks<Cursor> mEntriesNumberLoader = new LoaderManager.LoaderCallbacks<Cursor>() {
        @Override
        public Loader<Cursor> onCreateLoader(int id, Bundle args) {
            CursorLoader cursorLoader = new CursorLoader(getActivity(), mUri, new String[]{"SUM(" + EntryColumns.FETCH_DATE + '>' + mListDisplayDate + ")", "SUM(" + EntryColumns.FETCH_DATE + "<=" + mListDisplayDate + Constants.DB_AND + EntryColumns.WHERE_UNREAD + ")"}, null, null, null);
            cursorLoader.setUpdateThrottle(150);
            return cursorLoader;
        }

        @Override
        public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
            data.moveToFirst();
            mNewEntriesNumber = data.getInt(0);
            mOldUnreadEntriesNumber = data.getInt(1);

            if (mAutoRefreshDisplayDate && mNewEntriesNumber != 0 && mOldUnreadEntriesNumber == 0) {
                mListDisplayDate = new Date().getTime();
                restartLoaders();
            } else {
                refreshUI();
            }


            mAutoRefreshDisplayDate = false;
        }

        @Override
        public void onLoaderReset(Loader<Cursor> loader) {
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        setHasOptionsMenu(true);
        super.onCreate(savedInstanceState);


        if (savedInstanceState != null) {
            mUri = savedInstanceState.getParcelable(STATE_CURRENT_URI);
            mOriginalUri = savedInstanceState.getParcelable(STATE_ORIGINAL_URI);
            mShowFeedInfo = savedInstanceState.getBoolean(STATE_SHOW_FEED_INFO);
            mListDisplayDate = savedInstanceState.getLong(STATE_LIST_DISPLAY_DATE);

            mEntriesCursorAdapter = new EntriesCursorAdapter(getActivity(), mUri, Constants.EMPTY_CURSOR, mShowFeedInfo, mListDisplayDate);
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        mListView.getViewTreeObserver().addOnScrollChangedListener(this);
        refreshSwipeProgress();
        PrefUtils.registerOnPrefChangeListener(mPrefListener);

        if (mUri != null) {
            // If the list is empty when we are going back here, try with the last display date
            if (mNewEntriesNumber != 0 && mOldUnreadEntriesNumber == 0) {
                mListDisplayDate = new Date().getTime();
            } else {
                mAutoRefreshDisplayDate = true; // We will try to update the list after if necessary
            }

            restartLoaders();
        }
    }

    @Override
    public void onScrollChanged() {
        if (mListView.getFirstVisiblePosition() == 0) {
            mySwipeRefreshLayout.setEnabled(true);
        } else {
            mySwipeRefreshLayout.setEnabled(false);
        }
    }

    @Override
    public View inflateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_entry_list, container, true);
        int background = PrefUtils.getBoolean(PrefUtils.LIGHT_THEME, true) ? R.color.light_entries_list_background : R.color.dark_entries_list_background;
        rootView.setBackgroundColor(ContextCompat.getColor(getContext(), background));

        if (mEntriesCursorAdapter != null) {
            setListAdapter(mEntriesCursorAdapter);
        }

        mySwipeRefreshLayout = (SwipeRefreshLayout) rootView.findViewById(R.id.swipe_refresh);
        mySwipeRefreshLayout.setOnRefreshListener(
                new SwipeRefreshLayout.OnRefreshListener() {
                    @Override
                    public void onRefresh() {
                        startRefresh();
                    }
                });

        mListView = (ListView) rootView.findViewById(android.R.id.list);

        if (PrefUtils.getBoolean(PrefUtils.DISPLAY_TIP, true)) {
            final TextView header = new TextView(mListView.getContext());
            header.setMinimumHeight(UiUtils.dpToPixel(70));
            int footerPadding = UiUtils.dpToPixel(10);
            header.setPadding(footerPadding, footerPadding, footerPadding, footerPadding);
            header.setText(R.string.tip_sentence);
            header.setGravity(Gravity.CENTER_VERTICAL);
            header.setCompoundDrawablePadding(UiUtils.dpToPixel(5));
            header.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_info_outline, 0, R.drawable.ic_cancel, 0);
            header.setClickable(true);
            header.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    mListView.removeHeaderView(header);
                    PrefUtils.putBoolean(PrefUtils.DISPLAY_TIP, false);
                }
            });
            mListView.addHeaderView(header);
        }

        UiUtils.addEmptyFooterView(mListView, 90);

        mRefreshListBtn = (Button) rootView.findViewById(R.id.refreshListBtn);
        mRefreshListBtn.setBackgroundResource(PrefUtils.getBoolean(PrefUtils.LIGHT_THEME, true) ? R.drawable.bg_refresh_list_button_selector : R.drawable.bg_refresh_list_button_selector_dark);
        mRefreshListBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mNewEntriesNumber = 0;
                mListDisplayDate = new Date().getTime();
                refreshUI();
                if (mUri != null) {
                    restartLoaders();
                }
            }
        });


        disableSwipe();

        return rootView;
    }

    @Override
    public void onStop() {
        if (mJustMarkedAsReadEntries != null && !mJustMarkedAsReadEntries.isClosed()) {
            mJustMarkedAsReadEntries.close();
        }
        PrefUtils.unregisterOnPrefChangeListener(mPrefListener);
        refreshUI();
        super.onStop();
        mListView.getViewTreeObserver().removeOnScrollChangedListener(this);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putParcelable(STATE_CURRENT_URI, mUri);
        outState.putParcelable(STATE_ORIGINAL_URI, mOriginalUri);
        outState.putBoolean(STATE_SHOW_FEED_INFO, mShowFeedInfo);
        outState.putLong(STATE_LIST_DISPLAY_DATE, mListDisplayDate);
        refreshUI();
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onRefresh() {
        startRefresh();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        this.menu = menu;
        menu.clear(); // This is needed to remove a bug on Android 4.0.3
        inflater.inflate(R.menu.entry_list, menu);

        MenuItem searchItem = menu.findItem(R.id.menu_search);
        final SearchView searchView = (SearchView) searchItem.getActionView();
        if (EntryColumns.isSearchUri(mUri)) {
            searchItem.expandActionView();
            searchView.post(new Runnable() { // Without that, it just does not work
                @Override
                public void run() {
                    searchView.setQuery(mUri.getLastPathSegment(), false);
                    searchView.clearFocus();
                }
            });

        }

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                if (TextUtils.isEmpty(newText)) {
                    setData(mOriginalUri, true, true);
                } else {
                    setData(EntryColumns.SEARCH_URI(newText), true, true);
                }
                return false;
            }
        });
        searchView.setOnCloseListener(new SearchView.OnCloseListener() {
            @Override
            public boolean onClose() {
                setData(mOriginalUri, mShowFeedInfo, false);
                return false;
            }
        });

        if (!PrefUtils.getBoolean(PrefUtils.SHOW_READ, true)) {
            menu.findItem(R.id.menu_unread).setIcon(R.drawable.nav_read);
        } else {
            menu.findItem(R.id.menu_unread).setIcon(R.drawable.nav_unread);
        }

        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {

            case R.id.menu_unread:
                if (!PrefUtils.getBoolean(PrefUtils.SHOW_READ, true)) {
                    PrefUtils.putBoolean(PrefUtils.SHOW_READ, true);
                    menu.findItem(R.id.menu_unread).setIcon(R.drawable.nav_unread);
                    return true;

                } else {
                    PrefUtils.putBoolean(PrefUtils.SHOW_READ, false);
                    menu.findItem(R.id.menu_unread).setIcon(R.drawable.nav_read);

                }
                return true;

            case R.id.manage_feeds: {
                startActivity(new Intent(getActivity(), EditFeedsListActivity.class));
                return true;
            }
            case R.id.settings: {
                startActivity(new Intent(getActivity(), GeneralPrefsActivity.class));
                return true;
            }

        }
        return super.onOptionsItemSelected(item);
    }


    private void startRefresh() {
        if (!PrefUtils.getBoolean(PrefUtils.IS_REFRESHING, false)) {
            Intent intent = new Intent(getActivity(), FetcherService.class);
            intent.setAction(FetcherService.ACTION_REFRESH_FEEDS);
            if (mUri != null) {
                int match = FeedDataContentProvider.URI_MATCHER.match(mUri);
                if (match == FeedDataContentProvider.URI_ENTRIES_FOR_FEED) {
                    intent.putExtra(Constants.FEED_ID, mUri.getPathSegments().get(1));
                } else if (match == FeedDataContentProvider.URI_ENTRIES_FOR_GROUP) {
                    intent.putExtra(Constants.GROUP_ID, mUri.getPathSegments().get(1));
                }
            }
            getActivity().startService(intent);
        }
        refreshSwipeProgress();
    }

    public Uri getUri() {
        return mOriginalUri;
    }

    public void setData(Uri uri, boolean showFeedInfo) {
        setData(uri, showFeedInfo, false);
    }

    public void setData(Uri uri, boolean showFeedInfo, boolean isSearchUri) {
        mUri = uri;
        if (!isSearchUri) {
            mOriginalUri = mUri;
            mShowFeedInfo = showFeedInfo;
        }

        mListDisplayDate = new Date().getTime();
        mEntriesCursorAdapter = new EntriesCursorAdapter(getActivity(), mUri, Constants.EMPTY_CURSOR, showFeedInfo, mListDisplayDate);
        setListAdapter(mEntriesCursorAdapter);

        if (mUri != null) {
            restartLoaders();
        }
        refreshUI();
    }

    private void restartLoaders() {
        LoaderManager loaderManager = getLoaderManager();
        loaderManager.restartLoader(ENTRIES_LOADER_ID, null, mEntriesLoader);
        loaderManager.restartLoader(NEW_ENTRIES_NUMBER_LOADER_ID, null, mEntriesNumberLoader);
    }

    private void refreshUI() {
        if (mNewEntriesNumber > 0) {
            if (mRefreshListBtn.getVisibility() == View.GONE)
                YoYo.with(Techniques.BounceInDown).duration(500).playOn(mRefreshListBtn);
            mRefreshListBtn.setText(getResources().getQuantityString(R.plurals.number_of_new_entries, mNewEntriesNumber, mNewEntriesNumber));
            mRefreshListBtn.setVisibility(View.VISIBLE);
        } else {
            mRefreshListBtn.setVisibility(View.GONE);
        }
    }

    private void refreshSwipeProgress() {
        if (PrefUtils.getBoolean(PrefUtils.IS_REFRESHING, false)) {
            showSwipeProgress();
            mySwipeRefreshLayout.setRefreshing(true);
        } else {
            hideSwipeProgress();
            mySwipeRefreshLayout.setRefreshing(false);
        }
    }

    public void readAll() {
        final ContentResolver cr = getContext().getContentResolver();
        Snackbar snackbar = Snackbar.make(getActivity().findViewById(R.id.coordinator_layout), R.string.marked_as_read, Snackbar.LENGTH_LONG)
                .setActionTextColor(ContextCompat.getColor(getContext(), R.color.light_primary_color))
                .setAction(R.string.undo, new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        new Thread() {
                            @Override
                            public void run() {
                                if (mJustMarkedAsReadEntries != null && !mJustMarkedAsReadEntries.isClosed()) {
                                    ArrayList<Integer> ids = new ArrayList<>();
                                    while (mJustMarkedAsReadEntries.moveToNext()) {
                                        ids.add(mJustMarkedAsReadEntries.getInt(0));
                                    }
                                    String where = BaseColumns._ID + " IN (" + TextUtils.join(",", ids) + ')';
                                    cr.update(FeedData.EntryColumns.CONTENT_URI, FeedData.getUnreadContentValues(), where, null);

                                    mJustMarkedAsReadEntries.close();
                                }
                            }
                        }.start();
                    }
                });
        snackbar.getView().setBackgroundResource(R.color.material_grey_900);
        snackbar.show();

        new Thread() {
            @Override
            public void run() {
                if (mJustMarkedAsReadEntries != null && !mJustMarkedAsReadEntries.isClosed()) {
                    mJustMarkedAsReadEntries.close();
                }
                String where = EntryColumns.WHERE_UNREAD;
                mJustMarkedAsReadEntries = cr.query(mUri, new String[]{BaseColumns._ID}, where, null, null);
                cr.update(mUri, FeedData.getReadContentValues(), where, null);
            }
        }.start();
        // If we are on "all items" uri, we can remove the notification here
        if (EntryColumns.CONTENT_URI.equals(mUri) && Constants.NOTIF_MGR != null) {
            Constants.NOTIF_MGR.cancel(0);
        }
    }

}
