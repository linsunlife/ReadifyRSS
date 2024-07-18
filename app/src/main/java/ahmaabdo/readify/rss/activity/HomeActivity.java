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

package ahmaabdo.readify.rss.activity;

import android.app.AlertDialog;
import android.app.LoaderManager;
import android.content.*;
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.BaseColumns;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import ahmaabdo.readify.rss.Constants;
import ahmaabdo.readify.rss.R;
import ahmaabdo.readify.rss.adapter.DrawerAdapter;
import ahmaabdo.readify.rss.fragment.EntriesListFragment;
import ahmaabdo.readify.rss.provider.FeedData;
import ahmaabdo.readify.rss.provider.FeedData.EntryColumns;
import ahmaabdo.readify.rss.provider.FeedData.FeedColumns;
import ahmaabdo.readify.rss.service.FetcherService;
import ahmaabdo.readify.rss.service.RefreshService;
import ahmaabdo.readify.rss.utils.PrefUtils;
import ahmaabdo.readify.rss.utils.UiUtils;

import java.util.ArrayList;

public class HomeActivity extends BaseActivity implements LoaderManager.LoaderCallbacks<Cursor> {

    private static final int LOADER_ID = 0;
    private final SharedPreferences.OnSharedPreferenceChangeListener mShowReadListener = new SharedPreferences.OnSharedPreferenceChangeListener() {
        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            if (PrefUtils.SHOW_READ.equals(key)) {
                getLoaderManager().restartLoader(LOADER_ID, null, HomeActivity.this);

            }
        }
    };
    Uri newUri;
    boolean showFeedInfo = true;
    private EntriesListFragment mEntriesFragment;
    private DrawerLayout mDrawerLayout;
    private View mLeftDrawer;
    private ListView mDrawerList;
    private DrawerAdapter mDrawerAdapter = null;
    private ActionBarDrawerToggle mDrawerToggle;
    private CharSequence mTitle;
    private BitmapDrawable mIcon;
    private Cursor mJustMarkedAsReadEntries;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        UiUtils.setPreferenceTheme(this);
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_home);

        mEntriesFragment = (EntriesListFragment) getFragmentManager().findFragmentById(R.id.entries_list_fragment);

        mTitle = getTitle();

        boolean useLightTheme = PrefUtils.getBoolean(PrefUtils.LIGHT_THEME, true);
        Toolbar toolbar = findViewById(R.id.toolbar);
        if (!useLightTheme)
            toolbar.setPopupTheme(R.style.ThemeOverlay_AppCompat_Dark);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        FloatingActionButton readAllFloatingActionButton = findViewById(R.id.fab_read_all);
        readAllFloatingActionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final ContentResolver cr = getContentResolver();
                Snackbar snackbar = Snackbar.make(findViewById(R.id.coordinator_layout), R.string.marked_as_read, Snackbar.LENGTH_LONG)
                        .setActionTextColor(ContextCompat.getColor(getApplicationContext(), R.color.light_primary_color))
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
                        mJustMarkedAsReadEntries = cr.query(newUri, new String[]{BaseColumns._ID}, where, null, null);
                        cr.update(newUri, FeedData.getReadContentValues(), where, null);
                    }
                }.start();
                // If we are on "all items" uri, we can remove the notification here
                if (EntryColumns.CONTENT_URI.equals(newUri) && Constants.NOTIF_MGR != null) {
                    Constants.NOTIF_MGR.cancel(0);
                }
            }
        });

        mLeftDrawer = findViewById(R.id.left_drawer);
        mDrawerList = findViewById(R.id.drawer_list);
        mDrawerList.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
        mDrawerList.setOnItemClickListener(new ListView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                selectDrawerItem(position);
                if (mDrawerLayout != null) {
                    mDrawerLayout.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            mDrawerLayout.closeDrawer(mLeftDrawer);
                        }
                    }, 50);
                }
            }
        });
        mDrawerList.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                if (id > 0) {
                    startActivity(new Intent(Intent.ACTION_EDIT, FeedColumns.CONTENT_URI(id), getApplicationContext(), EditFeedActivity.class));
                    return true;
                }
                return false;
            }
        });

        mLeftDrawer.setBackgroundColor((ContextCompat.getColor(getApplicationContext(), useLightTheme ? R.color.light_primary_color : R.color.dark_background)));
        mDrawerList.setBackgroundColor((ContextCompat.getColor(getApplicationContext(), useLightTheme ? R.color.light_background : R.color.dark_primary_color_dark)));
        mDrawerLayout = findViewById(R.id.drawer_layout);
        if (mDrawerLayout != null) {
            mDrawerLayout.setDrawerShadow(R.drawable.drawer_shadow, GravityCompat.START);
            mDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout, R.string.drawer_open, R.string.drawer_close) {
                @Override
                public void onDrawerSlide(View drawerView, float slideOffset) {
                    super.onDrawerSlide(drawerView, 0);
                }
            };
            mDrawerLayout.addDrawerListener(mDrawerToggle);

        }

        getLoaderManager().initLoader(LOADER_ID, null, this);

        if (PrefUtils.getBoolean(PrefUtils.REFRESH_ENABLED, true)) {
            // starts the service independent to this activity
            startService(new Intent(this, RefreshService.class));
        } else {
            stopService(new Intent(this, RefreshService.class));
        }
        if (PrefUtils.getBoolean(PrefUtils.REFRESH_ON_OPEN_ENABLED, false)) {
            if (!PrefUtils.getBoolean(PrefUtils.IS_REFRESHING, false)) {
                startService(new Intent(HomeActivity.this, FetcherService.class).setAction(FetcherService.ACTION_REFRESH_FEEDS));
            }
        }
    }

    private void selectDrawerItem(int position) {
        PrefUtils.putInt(PrefUtils.DRAWER_POSITION, position);

        if (mDrawerAdapter == null)
            return;

        mDrawerAdapter.setSelectedItem(position);
        mIcon = null;

        switch (position) {
            case 0:
                newUri = EntryColumns.ALL_ENTRIES_CONTENT_URI;
                showFeedInfo = true;
                break;
            case 1:
                newUri = EntryColumns.FAVORITES_CONTENT_URI;
                showFeedInfo = true;
                break;
            default:
                long feedOrGroupId = mDrawerAdapter.getItemId(position);
                if (feedOrGroupId < 0) {
                    selectDrawerItem(0);
                    return;
                }
                if (mDrawerAdapter.isItemAGroup(position)) {
                    newUri = EntryColumns.ENTRIES_FOR_GROUP_CONTENT_URI(feedOrGroupId);
                    showFeedInfo = true;
                } else {
                    byte[] iconBytes = mDrawerAdapter.getItemIcon(position);
                    Bitmap bitmap = UiUtils.getScaledBitmap(iconBytes, 24);
                    if (bitmap != null) {
                        mIcon = new BitmapDrawable(getResources(), bitmap);
                    }

                    newUri = EntryColumns.ENTRIES_FOR_FEED_CONTENT_URI(feedOrGroupId);
                    showFeedInfo = false;
                }

                mTitle = mDrawerAdapter.getItemName(position);
                break;
        }

        if (!newUri.equals(mEntriesFragment.getUri())) {
            mEntriesFragment.setData(newUri, showFeedInfo);
        }

        mDrawerList.setItemChecked(position, true);

        // First open => we open the drawer for you
        if (PrefUtils.getBoolean(PrefUtils.FIRST_OPEN, true)) {
            PrefUtils.putBoolean(PrefUtils.FIRST_OPEN, false);
            if (mDrawerLayout != null) {
                mDrawerLayout.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        mDrawerLayout.openDrawer(mLeftDrawer);
                    }
                }, 500);
            }

            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(R.string.welcome_title)
                    .setItems(new CharSequence[]{getString(R.string.google_news_title), getString(R.string.add_custom_feed)}, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            if (which == 1) {
                                startActivity(new Intent(Intent.ACTION_INSERT, FeedColumns.CONTENT_URI, getApplicationContext(), EditFeedActivity.class));
                            } else {
                                startActivity(new Intent(HomeActivity.this, AddGoogleNewsActivity.class));
                            }
                        }
                    });
            builder.show();
        }
        refreshTitle(0);
    }

    @Override
    protected void onResume() {
        super.onResume();
        PrefUtils.registerOnPrefChangeListener(mShowReadListener);
    }

    @Override
    protected void onPause() {
        PrefUtils.unregisterOnPrefChangeListener(mShowReadListener);
        super.onPause();
    }

    @Override
    public void onStop() {
        if (mJustMarkedAsReadEntries != null && !mJustMarkedAsReadEntries.isClosed()) {
            mJustMarkedAsReadEntries.close();
        }
        super.onStop();
    }

    @Override
    public void finish() {
        if (mDrawerLayout != null) {
            if (mDrawerLayout.isDrawerOpen(mLeftDrawer)) {
                mDrawerLayout.closeDrawer(mLeftDrawer);
                return;
            }
        }
        super.finish();
    }

    public void onBackPressed() {
        // Before exiting from app the navigation drawer is opened
        if (mDrawerLayout != null && !mDrawerLayout.isDrawerOpen(GravityCompat.START)) {
            mDrawerLayout.openDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return mDrawerToggle != null && mDrawerToggle.onOptionsItemSelected(item) || super.onOptionsItemSelected(item);
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        // Sync the toggle state after onRestoreInstanceState has occurred.
        if (mDrawerToggle != null) {
            mDrawerToggle.syncState();
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (mDrawerToggle != null) {
            mDrawerToggle.onConfigurationChanged(newConfig);
        }
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        CursorLoader cursorLoader = new CursorLoader(this,
                FeedColumns.GROUPED_FEEDS_CONTENT_URI,
                new String[]{FeedColumns._ID, FeedColumns.URL, FeedColumns.NAME,
                        FeedColumns.IS_GROUP, FeedColumns.ICON, FeedColumns.LAST_UPDATE,
                        FeedColumns.ERROR, FeedColumns.IS_GROUP_EXPANDED},
                FeedColumns.IS_GROUP + Constants.DB_IS_TRUE + Constants.DB_OR +
                        FeedColumns.GROUP_ID + Constants.DB_IS_NULL + Constants.DB_OR +
                        FeedColumns.GROUP_ID + " IN (SELECT " + FeedColumns._ID +
                        " FROM " + FeedColumns.TABLE_NAME +
                        " WHERE " + FeedColumns.IS_GROUP_EXPANDED + Constants.DB_IS_TRUE + ")",
                null,
                null
        );
        cursorLoader.setUpdateThrottle(Constants.UPDATE_THROTTLE_DELAY);
        return cursorLoader;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        if (mDrawerAdapter != null) {
            mDrawerAdapter.setCursor(cursor);
        } else {
            mDrawerAdapter = new DrawerAdapter(this, cursor);
            mDrawerList.post(new Runnable() {
                public void run() {
                    mDrawerList.setAdapter(mDrawerAdapter);
                    selectDrawerItem(PrefUtils.getInt(PrefUtils.DRAWER_POSITION, 0));
                }
            });
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> cursorLoader) {
        if (mDrawerAdapter == null)
            return;

        mDrawerAdapter.setCursor(null);
    }

    public void refreshTitle(int mNewEntriesNumber) {
        switch (PrefUtils.getInt(PrefUtils.DRAWER_POSITION, 0)) {
            case 0:
                getSupportActionBar().setTitle(R.string.all);
                break;
            case 1:
                getSupportActionBar().setTitle(R.string.favorites);
                break;
            default:
                getSupportActionBar().setTitle(mTitle);
                break;
        }
        if (mNewEntriesNumber != 0) {
            getSupportActionBar().setTitle(getSupportActionBar().getTitle().toString() + " (" + String.valueOf(mNewEntriesNumber) + ")");
        }
        invalidateOptionsMenu();
    }
}
