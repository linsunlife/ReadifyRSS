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
 * <p/>
 * <p/>
 * Some parts of this software are based on "Sparse rss" under the MIT license (see
 * below). Please refers to the original project to identify which parts are under the
 * MIT license.
 * <p/>
 * Copyright (c) 2010-2012 Stefan Handschuh
 * <p/>
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * <p/>
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * <p/>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package ahmaabdo.readify.rss.activity;

import ahmaabdo.readify.rss.utils.FileUtils;
import ahmaabdo.readify.rss.utils.ToastUtils;
import android.app.AlertDialog;
import android.app.LoaderManager;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.CursorLoader;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.Loader;
import android.content.res.TypedArray;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.view.ActionMode;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.SimpleAdapter;
import android.widget.Spinner;
import android.widget.TabHost;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URLEncoder;
import java.util.*;

import ahmaabdo.readify.rss.Constants;
import ahmaabdo.readify.rss.R;
import ahmaabdo.readify.rss.adapter.FiltersCursorAdapter;
import ahmaabdo.readify.rss.loader.BaseLoader;
import ahmaabdo.readify.rss.provider.FeedData.FeedColumns;
import ahmaabdo.readify.rss.provider.FeedData.FilterColumns;
import ahmaabdo.readify.rss.provider.FeedDataContentProvider;
import ahmaabdo.readify.rss.utils.NetworkUtils;
import ahmaabdo.readify.rss.utils.UiUtils;

public class EditFeedActivity extends BaseActivity implements LoaderManager.LoaderCallbacks<Cursor> {
    static final String FEED_SEARCH_TITLE = "title";
    static final String FEED_SEARCH_URL = "feedId";
    static final String FEED_SEARCH_DESC = "description";
    private static final String STATE_CURRENT_TAB = "STATE_CURRENT_TAB";
    private final ActionMode.Callback mFilterActionModeCallback = new ActionMode.Callback() {

        // Called when the action mode is created; startActionMode() was called
        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            // Inflate a menu resource providing context menu items
            MenuInflater inflater = mode.getMenuInflater();
            inflater.inflate(R.menu.edit_context_menu, menu);
            return true;
        }

        // Called each time the action mode is shown. Always called after onCreateActionMode, but
        // may be called multiple times if the mode is invalidated.
        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            return false; // Return false if nothing is done
        }

        // Called when the user selects a contextual menu item
        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {

            switch (item.getItemId()) {
                case R.id.menu_edit:
                    Cursor c = mFiltersCursorAdapter.getCursor();
                    if (c.moveToPosition(mFiltersCursorAdapter.getSelectedFilter())) {
                        final View dialogView = getLayoutInflater().inflate(R.layout.dialog_filter_edit, null);
                        final EditText filterText = (EditText) dialogView.findViewById(R.id.filterText);
                        final CheckBox regexCheckBox = (CheckBox) dialogView.findViewById(R.id.regexCheckBox);
                        final RadioButton applyTitleRadio = (RadioButton) dialogView.findViewById(R.id.applyTitleRadio);
                        final RadioButton applyContentRadio = (RadioButton) dialogView.findViewById(R.id.applyContentRadio);
                        final RadioButton acceptRadio = (RadioButton) dialogView.findViewById(R.id.acceptRadio);
                        final RadioButton rejectRadio = (RadioButton) dialogView.findViewById(R.id.rejectRadio);

                        filterText.setText(c.getString(c.getColumnIndex(FilterColumns.FILTER_TEXT)));
                        regexCheckBox.setChecked(c.getInt(c.getColumnIndex(FilterColumns.IS_REGEX)) == 1);
                        if (c.getInt(c.getColumnIndex(FilterColumns.IS_APPLIED_TO_TITLE)) == 1) {
                            applyTitleRadio.setChecked(true);
                        } else {
                            applyContentRadio.setChecked(true);
                        }
                        if (c.getInt(c.getColumnIndex(FilterColumns.IS_ACCEPT_RULE)) == 1) {
                            acceptRadio.setChecked(true);
                        } else {
                            rejectRadio.setChecked(true);
                        }

                        final long filterId = mFiltersCursorAdapter.getItemId(mFiltersCursorAdapter.getSelectedFilter());
                        new AlertDialog.Builder(EditFeedActivity.this) //
                                .setTitle(R.string.filter_edit_title) //
                                .setView(dialogView) //
                                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        new Thread() {
                                            @Override
                                            public void run() {
                                                String filter = filterText.getText().toString();
                                                if (!filter.isEmpty()) {
                                                    ContentResolver cr = getContentResolver();
                                                    ContentValues values = new ContentValues();
                                                    values.put(FilterColumns.FILTER_TEXT, filter);
                                                    values.put(FilterColumns.IS_REGEX, regexCheckBox.isChecked());
                                                    values.put(FilterColumns.IS_APPLIED_TO_TITLE, applyTitleRadio.isChecked());
                                                    values.put(FilterColumns.IS_ACCEPT_RULE, acceptRadio.isChecked());
                                                    if (cr.update(FilterColumns.CONTENT_URI, values, FilterColumns._ID + '=' + filterId, null) > 0) {
                                                        cr.notifyChange(
                                                                FilterColumns.FILTERS_FOR_FEED_CONTENT_URI(getIntent().getData().getLastPathSegment()),
                                                                null);
                                                    }
                                                }
                                            }
                                        }.start();
                                    }
                                }).setNegativeButton(android.R.string.cancel, null).show();
                    }

                    mode.finish(); // Action picked, so close the CAB
                    return true;
                case R.id.menu_delete:
                    final long filterId = mFiltersCursorAdapter.getItemId(mFiltersCursorAdapter.getSelectedFilter());
                    new AlertDialog.Builder(EditFeedActivity.this) //
                            .setIcon(android.R.drawable.ic_dialog_alert) //
                            .setTitle(R.string.filter_delete_title) //
                            .setMessage(R.string.question_delete_filter) //
                            .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    new Thread() {
                                        @Override
                                        public void run() {
                                            ContentResolver cr = getContentResolver();
                                            if (cr.delete(FilterColumns.CONTENT_URI, FilterColumns._ID + '=' + filterId, null) > 0) {
                                                cr.notifyChange(FilterColumns.FILTERS_FOR_FEED_CONTENT_URI(getIntent().getData().getLastPathSegment()),
                                                        null);
                                            }
                                        }
                                    }.start();
                                }
                            }).setNegativeButton(android.R.string.no, null).show();

                    mode.finish(); // Action picked, so close the CAB
                    return true;
                default:
                    return false;
            }
        }

        // Called when the user exits the action mode
        @Override
        public void onDestroyActionMode(ActionMode mode) {
            mFiltersCursorAdapter.setSelectedFilter(-1);
            mFiltersListView.invalidateViews();
        }
    };
    private TabHost mTabHost;
    private TextView mNameTextView, mUrlTextView, mGroupTextView;
    private EditText mNameEditText, mUrlEditText;
    private EditText mCookieNameEditText, mCookieValueEditText;
    private EditText mLoginHTTPAuthEditText, mPasswordHTTPAuthEditText;
    private Spinner mGroupSpinner, mKeepTime;
    private CheckBox mRetrieveFulltextCb;
    private ListView mFiltersListView;
    private FiltersCursorAdapter mFiltersCursorAdapter;
    private boolean mIsGroup;
    private Long mGroupId;
    private List<Long> mGroupIds;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        UiUtils.setPreferenceTheme(this);
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_feed_edit);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        setResult(RESULT_CANCELED);

        Intent intent = getIntent();

        mTabHost = (TabHost) findViewById(R.id.tabHost);
        mNameEditText = (EditText) findViewById(R.id.feed_title);
        mNameTextView = (TextView) findViewById(R.id.name_textview);
        mUrlEditText = (EditText) findViewById(R.id.feed_url);
        mUrlTextView = (TextView) findViewById(R.id.url_textview);
        mGroupSpinner = (Spinner) findViewById(R.id.settings_groups);
        mGroupTextView = (TextView) findViewById(R.id.group_textview);
        mCookieNameEditText = (EditText) findViewById(R.id.feed_cookiename);
        mCookieValueEditText = (EditText) findViewById(R.id.feed_cookievalue);
        mLoginHTTPAuthEditText = (EditText) findViewById(R.id.feed_loginHttpAuth);
        mPasswordHTTPAuthEditText = (EditText) findViewById(R.id.feed_passwordHttpAuth);
        mKeepTime = (Spinner) findViewById(R.id.settings_keep_times);
        mRetrieveFulltextCb = (CheckBox) findViewById(R.id.retrieve_fulltext);
        mFiltersListView = (ListView) findViewById(android.R.id.list);
        View tabWidget = findViewById(android.R.id.tabs);
        View buttonLayout = findViewById(R.id.button_layout);

        mTabHost.setup();
        mTabHost.addTab(mTabHost.newTabSpec("feedTab").setIndicator(getString(R.string.tab_feed_title)).setContent(R.id.feed_tab));
        mTabHost.addTab(mTabHost.newTabSpec("filtersTab").setIndicator(getString(R.string.tab_filters_title)).setContent(R.id.filters_tab));
        mTabHost.addTab(mTabHost.newTabSpec("advancedTab").setIndicator(getString(R.string.tab_advanced_title)).setContent(R.id.advanced_tab));

        mTabHost.setOnTabChangedListener(new TabHost.OnTabChangeListener() {
            @Override
            public void onTabChanged(String s) {
                invalidateOptionsMenu();
            }
        });

        if (savedInstanceState != null) {
            mTabHost.setCurrentTab(savedInstanceState.getInt(STATE_CURRENT_TAB));
        }

        if (intent.getAction().equals(Intent.ACTION_INSERT)) {
            setTitle(R.string.new_feed_title);

            //Forcing the keyboard to appear
            getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);

            tabWidget.setVisibility(View.GONE);
            mNameEditText.setVisibility(View.INVISIBLE);
            mNameTextView.setVisibility(View.INVISIBLE);

            String[] selectedValues = getResources().getStringArray(R.array.settings_keep_time_values);
            mKeepTime.setSelection(selectedValues.length - 1);
            mRetrieveFulltextCb.setChecked(false);
        } else if (intent.getAction().equals(Intent.ACTION_VIEW)) {
            setTitle(R.string.new_feed_title);

            tabWidget.setVisibility(View.GONE);
            mUrlEditText.setText(intent.getDataString());
            String[] selectedValues = getResources().getStringArray(R.array.settings_keep_time_values);
            mKeepTime.setSelection(selectedValues.length - 1);
            mRetrieveFulltextCb.setChecked(false);
        } else if (intent.getAction().equals(Intent.ACTION_EDIT)) {
            if (savedInstanceState != null)
                return;

            Cursor cursor = getContentResolver().query(intent.getData(),
                    new String[]{FeedColumns.IS_GROUP, FeedColumns.NAME,
                            FeedColumns.URL, FeedColumns.GROUP_ID,
                            FeedColumns.RETRIEVE_FULLTEXT, FeedColumns.COOKIE_NAME,
                            FeedColumns.COOKIE_VALUE, FeedColumns.HTTP_AUTH_LOGIN,
                            FeedColumns.HTTP_AUTH_PASSWORD, FeedColumns.KEEP_TIME},
                    null, null, null);
            if (!cursor.moveToFirst()) {
                cursor.close();
                ToastUtils.showShort(R.string.error);
                finish();
                return;
            }

            buttonLayout.setVisibility(View.GONE);
            mIsGroup = cursor.getInt(0) == 1;

            if (mIsGroup) {
                setTitle(R.string.manage_group_title);
                tabWidget.setVisibility(View.GONE);
                mUrlEditText.setVisibility(View.INVISIBLE);
                mUrlTextView.setVisibility(View.INVISIBLE);
                mRetrieveFulltextCb.setVisibility(View.INVISIBLE);
                mNameEditText.setText(cursor.getString(1));
            }
            else {
                setTitle(R.string.manage_feed_title);

                mFiltersCursorAdapter = new FiltersCursorAdapter(this, Constants.EMPTY_CURSOR);
                mFiltersListView.setAdapter(mFiltersCursorAdapter);
                mFiltersListView.setOnItemLongClickListener(new OnItemLongClickListener() {
                    @Override
                    public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                        startSupportActionMode(mFilterActionModeCallback);
                        mFiltersCursorAdapter.setSelectedFilter(position);
                        mFiltersListView.invalidateViews();
                        return true;
                    }
                });

                getLoaderManager().initLoader(0, null, this);

                mNameEditText.setText(cursor.getString(1));
                mUrlEditText.setText(cursor.getString(2));

                mGroupSpinner.setVisibility(View.VISIBLE);
                mGroupTextView.setVisibility(View.VISIBLE);

                mGroupId = cursor.getLong(3);
                mGroupIds = new ArrayList<>();
                mGroupIds.add(null);

                String GROUP_NAME = "name";
                List<Map<String, String>> data = new ArrayList<>();
                Map<String, String> map = new HashMap<>();
                map.put(GROUP_NAME, getString(R.string.feed_group_none));
                data.add(map);

                Cursor cursor2 = getContentResolver().query(FeedColumns.GROUPS_CONTENT_URI,
                        new String[]{FeedColumns._ID, FeedColumns.NAME},
                        FeedColumns.IS_GROUP + Constants.DB_IS_TRUE,
                        null, null);
                while (cursor2.moveToNext()) {
                    mGroupIds.add(cursor2.getLong(0));
                    map = new HashMap<>();
                    map.put(GROUP_NAME, cursor2.getString(1));
                    data.add(map);
                }
                cursor2.close();

                // create the grid item mapping
                String[] from = new String[]{GROUP_NAME};
                int[] to = new int[]{android.R.id.text1};
                // fill in the grid_item layout
                SimpleAdapter adapter = new SimpleAdapter(EditFeedActivity.this, data,
                        android.R.layout.simple_spinner_dropdown_item, from, to);
                mGroupSpinner.setAdapter(adapter);
                mGroupSpinner.setSelection(Math.max(mGroupIds.indexOf(mGroupId), 0));

                mRetrieveFulltextCb.setChecked(cursor.getInt(4) == 1);
                mCookieNameEditText.setText(cursor.getString(5));
                mCookieValueEditText.setText(cursor.getString(6));
                mLoginHTTPAuthEditText.setText(cursor.getString(7));
                mPasswordHTTPAuthEditText.setText(cursor.getString(8));
                Integer intDate = cursor.getInt(9);
                String[] selectedValues = getResources().getStringArray(R.array.settings_keep_time_values);
                int index = Arrays.asList(selectedValues).indexOf(String.valueOf(intDate));
                mKeepTime.setSelection(index >= 0 ? index : selectedValues.length - 1);
            }
            cursor.close();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putInt(STATE_CURRENT_TAB, mTabHost.getCurrentTab());
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (getIntent().getAction().equals(Intent.ACTION_EDIT)) {
            ContentResolver cr = getContentResolver();
            Cursor cursor = null;
            try {
                if (mIsGroup) {
                    ContentValues values = new ContentValues();
                    values.put(FeedColumns.NAME, mNameEditText.getText().toString().trim());
                    cr.update(getIntent().getData(), values, null, null);
                    return;
                }

                String url = mUrlEditText.getText().toString();
                cursor = getContentResolver().query(FeedColumns.CONTENT_URI, FeedColumns.PROJECTION_ID,
                        FeedColumns.URL + Constants.DB_ARG, new String[]{url}, null);

                if (cursor != null && cursor.moveToFirst() && !getIntent().getData().getLastPathSegment().equals(cursor.getString(0))) {
                    ToastUtils.showLong(R.string.error_feed_url_exists);
                } else {
                    ContentValues values = new ContentValues();

                    if (!url.startsWith(Constants.HTTP_SCHEME) && !url.startsWith(Constants.HTTPS_SCHEME)) {
                        url = Constants.HTTP_SCHEME + url;
                    }
                    values.put(FeedColumns.URL, url);

                    Long newGroupId = mGroupIds.get(mGroupSpinner.getSelectedItemPosition());
                    values.put(FeedColumns.GROUP_ID, newGroupId);
                    if (!Objects.equals(mGroupId, newGroupId))
                        values.put(FeedColumns.PRIORITY, 1);

                    String name = mNameEditText.getText().toString();
                    String cookieName = mCookieNameEditText.getText().toString();
                    String cookieValue = mCookieValueEditText.getText().toString();
                    String loginHTTPAuth = mLoginHTTPAuthEditText.getText().toString();
                    String passwordHTTPAuth = mPasswordHTTPAuthEditText.getText().toString();

                    values.put(FeedColumns.NAME, name.trim().length() > 0 ? name : null);
                    values.put(FeedColumns.RETRIEVE_FULLTEXT, mRetrieveFulltextCb.isChecked() ? 1 : null);
                    values.put(FeedColumns.COOKIE_NAME, cookieName.trim().length() > 0 ? cookieName : "");
                    values.put(FeedColumns.COOKIE_VALUE, cookieValue.trim().length() > 0 ? cookieValue : "");
                    values.put(FeedColumns.HTTP_AUTH_LOGIN, loginHTTPAuth.trim().length() > 0 ? loginHTTPAuth : "");
                    values.put(FeedColumns.HTTP_AUTH_PASSWORD, passwordHTTPAuth.trim().length() > 0 ? passwordHTTPAuth : "");
                    final TypedArray selectedValues = getResources().obtainTypedArray(R.array.settings_keep_time_values);
                    values.put(FeedColumns.KEEP_TIME, selectedValues.getInt(mKeepTime.getSelectedItemPosition(), 0));
                    values.put(FeedColumns.FETCH_MODE, 0);

                    cr.update(getIntent().getData(), values, null, null);
                }
            } catch (Exception e) {
                ToastUtils.showLong(String.format(getString(R.string.action_failed), e.getMessage()));
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.edit_feed, menu);

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        if (mTabHost.getCurrentTab() == 0) {
            menu.findItem(R.id.menu_delete_feed).setVisible(true);
            menu.findItem(R.id.menu_add_filter).setVisible(false);
            menu.findItem(R.id.menu_help_filter).setVisible(false);
        } else if (mTabHost.getCurrentTab() == 1) {
            menu.findItem(R.id.menu_delete_feed).setVisible(false);
            menu.findItem(R.id.menu_add_filter).setVisible(true);
            menu.findItem(R.id.menu_help_filter).setVisible(true);
        } else {
            menu.findItem(R.id.menu_delete_feed).setVisible(false);
            menu.findItem(R.id.menu_add_filter).setVisible(false);
            menu.findItem(R.id.menu_help_filter).setVisible(false);
        }

        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
            case R.id.menu_delete_feed:
                new AlertDialog.Builder(this)
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .setMessage(R.string.question_delete_feed)
                        .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int id) {
                                new Thread() {
                                    @Override
                                    public void run() {
                                        try {
                                            String feedId = getIntent().getData().getLastPathSegment();
                                            Uri uri = mIsGroup ? FeedColumns.GROUPS_CONTENT_URI(feedId) : FeedColumns.CONTENT_URI(feedId);
                                            if (getContentResolver().delete(uri, null, null) > 0) {
                                                finish();
                                                return;
                                            }
                                            ToastUtils.showShort(R.string.error);
                                        } catch (Exception e) {
                                            ToastUtils.showLong(String.format(getString(R.string.action_failed), e.getMessage()));
                                        }
                                    }
                                }.start();
                            }
                        }).setNegativeButton(android.R.string.cancel, null).show();
                return true;
            case R.id.menu_add_filter: {
                final View dialogView = getLayoutInflater().inflate(R.layout.dialog_filter_edit, null);

                new AlertDialog.Builder(this) //
                        .setTitle(R.string.filter_add_title) //
                        .setView(dialogView) //
                        .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int id) {
                                String filterText = ((EditText) dialogView.findViewById(R.id.filterText)).getText().toString();
                                if (filterText.length() != 0) {
                                    String feedId = getIntent().getData().getLastPathSegment();

                                    ContentValues values = new ContentValues();
                                    values.put(FilterColumns.FILTER_TEXT, filterText);
                                    values.put(FilterColumns.IS_REGEX, ((CheckBox) dialogView.findViewById(R.id.regexCheckBox)).isChecked());
                                    values.put(FilterColumns.IS_APPLIED_TO_TITLE, ((RadioButton) dialogView.findViewById(R.id.applyTitleRadio)).isChecked());
                                    values.put(FilterColumns.IS_ACCEPT_RULE, ((RadioButton) dialogView.findViewById(R.id.acceptRadio)).isChecked());

                                    ContentResolver cr = getContentResolver();
                                    cr.insert(FilterColumns.FILTERS_FOR_FEED_CONTENT_URI(feedId), values);
                                }
                            }
                        }).setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                    }
                }).show();
                return true;
            }
            case R.id.menu_help_filter:
                Intent intent = new Intent();
                intent.setAction(Intent.ACTION_VIEW);
                intent.addCategory(Intent.CATEGORY_BROWSABLE);
                intent.setData(Uri.parse("https://github.com/Etuldan/spaRSS/wiki/How-to-use-the-Feed-Filter"));
                startActivity(intent);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public void onClickOk(View view) {
        // only in insert mode
        final String name = mNameEditText.getText().toString().trim();
        final String urlOrSearch = mUrlEditText.getText().toString().trim();
        if (urlOrSearch.isEmpty()) {
            UiUtils.showMessage(EditFeedActivity.this, R.string.error_feed_error);
        }

        if (!urlOrSearch.contains(".") || !urlOrSearch.contains("/") || urlOrSearch.contains(" ")) {
            final ProgressDialog pd = new ProgressDialog(EditFeedActivity.this);
            pd.setMessage(getString(R.string.loading));
            pd.setCancelable(true);
            pd.setIndeterminate(true);
            pd.show();

            getLoaderManager().restartLoader(1, null, new LoaderManager.LoaderCallbacks<ArrayList<HashMap<String, String>>>() {

                @Override
                public Loader<ArrayList<HashMap<String, String>>> onCreateLoader(int id, Bundle args) {
                    String encodedSearchText = urlOrSearch;
                    try {
                        encodedSearchText = URLEncoder.encode(urlOrSearch, Constants.UTF8);
                    } catch (UnsupportedEncodingException ignored) {
                    }

                    return new GetFeedSearchResultsLoader(EditFeedActivity.this, encodedSearchText);
                }

                @Override
                public void onLoadFinished(Loader<ArrayList<HashMap<String, String>>> loader, final ArrayList<HashMap<String, String>> data) {
                    pd.cancel();

                    if (data == null) {
                        UiUtils.showMessage(EditFeedActivity.this, R.string.error);
                    } else if (data.isEmpty()) {
                        UiUtils.showMessage(EditFeedActivity.this, R.string.no_result);
                    } else {
                        AlertDialog.Builder builder = new AlertDialog.Builder(EditFeedActivity.this);
                        builder.setTitle(R.string.feed_search);

                        // create the grid item mapping
                        String[] from = new String[]{FEED_SEARCH_TITLE, FEED_SEARCH_DESC};
                        int[] to = new int[]{android.R.id.text1, android.R.id.text2};

                        // fill in the grid_item layout
                        SimpleAdapter adapter = new SimpleAdapter(EditFeedActivity.this, data, R.layout.item_search_result, from,
                                to);
                        builder.setSingleChoiceItems(adapter, -1, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                boolean added = FeedDataContentProvider.addFeed(EditFeedActivity.this, data.get(which).get(FEED_SEARCH_URL), name.isEmpty() ? data.get(which).get(FEED_SEARCH_TITLE) : name, mRetrieveFulltextCb.isChecked());
                                if (added)
                                    ToastUtils.showShort(R.string.action_finished);
                            }
                        });
                        builder.show();
                    }
                }

                @Override
                public void onLoaderReset(Loader<ArrayList<HashMap<String, String>>> loader) {
                }
            });
        } else {
            FeedDataContentProvider.addFeed(EditFeedActivity.this, urlOrSearch, name, mRetrieveFulltextCb.isChecked());

            setResult(RESULT_OK);
            finish();
        }
    }

    public void onClickCancel(View view) {
        finish();
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        CursorLoader cursorLoader = new CursorLoader(this, FilterColumns.FILTERS_FOR_FEED_CONTENT_URI(getIntent().getData().getLastPathSegment()),
                null, null, null, FilterColumns.IS_ACCEPT_RULE + Constants.DB_DESC);
        cursorLoader.setUpdateThrottle(Constants.UPDATE_THROTTLE_DELAY);
        return cursorLoader;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        mFiltersCursorAdapter.swapCursor(data);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mFiltersCursorAdapter.swapCursor(Constants.EMPTY_CURSOR);
    }
}

/**
 * A custom Loader that loads feed search results from the google WS.
 */
class GetFeedSearchResultsLoader extends BaseLoader<ArrayList<HashMap<String, String>>> {
    private static final String TAG = GetFeedSearchResultsLoader.class.getSimpleName();
    private final String mSearchText;

    public GetFeedSearchResultsLoader(Context context, String searchText) {
        super(context);
        mSearchText = searchText;
    }

    /**
     * This is where the bulk of our work is done. This function is called in a background thread and should generate a new set of data to be
     * published by the loader.
     */
    @Override
    public ArrayList<HashMap<String, String>> loadInBackground() {
        try {
            HttpURLConnection conn = NetworkUtils.setupConnection("http://cloud.feedly.com/v3/search/feeds?count=20&locale=" + getContext().getResources().getConfiguration().locale.getLanguage() + "&query=" + mSearchText);
            try {
                InputStream inputStream = conn.getInputStream();
                String jsonStr = FileUtils.getString(inputStream);
                inputStream.close();

                // Parse results
                final ArrayList<HashMap<String, String>> results = new ArrayList<>();
                JSONArray entries = new JSONObject(jsonStr).getJSONArray("results");
                for (int i = 0; i < entries.length(); i++) {
                    try {
                        JSONObject entry = (JSONObject) entries.get(i);
                        String url = entry.get(EditFeedActivity.FEED_SEARCH_URL).toString().replace("feed/", "");
                        if (!url.isEmpty()) {
                            HashMap<String, String> map = new HashMap<>();
                            map.put(EditFeedActivity.FEED_SEARCH_TITLE, Html.fromHtml(entry.get(EditFeedActivity.FEED_SEARCH_TITLE).toString())
                                    .toString());
                            map.put(EditFeedActivity.FEED_SEARCH_URL, url);
                            map.put(EditFeedActivity.FEED_SEARCH_DESC, Html.fromHtml(entry.get(EditFeedActivity.FEED_SEARCH_DESC).toString()).toString());

                            results.add(map);
                        }
                    } catch (Exception ignored) {
                    }
                }

                return results;
            } finally {
                conn.disconnect();
            }
        } catch (Exception e) {
            return null;
        }
    }
}
