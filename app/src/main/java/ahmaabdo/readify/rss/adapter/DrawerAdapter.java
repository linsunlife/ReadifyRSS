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

package ahmaabdo.readify.rss.adapter;

import ahmaabdo.readify.rss.Constants;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.amulyakhare.textdrawable.TextDrawable;
import com.amulyakhare.textdrawable.util.ColorGenerator;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import ahmaabdo.readify.rss.MainApplication;
import ahmaabdo.readify.rss.R;
import ahmaabdo.readify.rss.provider.FeedData;
import ahmaabdo.readify.rss.provider.FeedData.EntryColumns;
import ahmaabdo.readify.rss.provider.FeedData.FeedColumns;
import ahmaabdo.readify.rss.utils.PrefUtils;
import ahmaabdo.readify.rss.utils.StringUtils;
import ahmaabdo.readify.rss.utils.UiUtils;

public class DrawerAdapter extends BaseAdapter {

    private static final int POS_ID = 0;
    private static final int POS_URL = 1;
    private static final int POS_NAME = 2;
    private static final int POS_IS_GROUP = 3;
    private static final int POS_ICON = 4;
    private static final int POS_LAST_UPDATE = 5;
    private static final int POS_ERROR = 6;
    private static final int POS_IS_GROUP_EXPANDED = 7;

    private static final int GROUP_TEXT_COLOR = Color.parseColor("#BBBBBB");

    private static final String COLON = MainApplication.getContext().getString(R.string.colon);

    private static final int CACHE_MAX_ENTRIES = 100;
    private final Map<Long, String> mFormattedDateCache = new LinkedHashMap<Long, String>(CACHE_MAX_ENTRIES + 1, .75F, true) {
        @Override
        public boolean removeEldestEntry(Entry<Long, String> eldest) {
            return size() > CACHE_MAX_ENTRIES;
        }
    };

    private int mSelectedItem;
    private final Context mContext;
    private Cursor mFeedsCursor;
    private int mAllNumber = 0;
    private int mRecentNumber = 0;
    private int mLaterReadingNumber = 0;
    private int mFavoritesNumber = 0;
    private HashMap<Long, Integer> entriesNumbers = new HashMap<>();

    public DrawerAdapter(Context context, Cursor feedCursor) {
        mContext = context;
        mFeedsCursor = feedCursor;
        getEntriesNumbers();
    }

    public void setSelectedItem(int selectedItem) {
        if (mSelectedItem != selectedItem) {
            mSelectedItem = selectedItem;
            notifyDataSetChanged();
        }
    }

    public void setCursor(Cursor feedCursor) {
        if (mFeedsCursor != null)
            mFeedsCursor.close();
        mFeedsCursor = feedCursor;
        getEntriesNumbers();
    }

    public View getView(final int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = inflater.inflate(R.layout.item_drawer_list, parent, false);

            ViewHolder holder = new ViewHolder();
            holder.iconView = convertView.findViewById(android.R.id.icon);
            holder.titleTxt = convertView.findViewById(android.R.id.text1);
            holder.stateTxt = convertView.findViewById(android.R.id.text2);
            holder.entriesNumberTxt = convertView.findViewById(R.id.entries_number);
            holder.separator = convertView.findViewById(R.id.separator);
            convertView.setTag(R.id.holder, holder);
        }
        ViewHolder holder = (ViewHolder) convertView.getTag(R.id.holder);

        if (holder != null) {
            if (position == mSelectedItem) {
                holder.titleTxt.setTextColor(ContextCompat.getColor(mContext, PrefUtils.getBoolean(PrefUtils.LIGHT_THEME, true) ? R.color.light_primary_color : R.color.dark_accent_color));
            } else {
                holder.titleTxt.setTextColor(ContextCompat.getColor(mContext, PrefUtils.getBoolean(PrefUtils.LIGHT_THEME, true) ? R.color.light_base_text : R.color.dark_base_text));
            }

            // default init
            holder.iconView.setImageDrawable(null);
            holder.iconView.clearColorFilter();
            holder.titleTxt.setText("");
            holder.titleTxt.setAllCaps(false);
            holder.stateTxt.setVisibility(View.GONE);
            holder.entriesNumberTxt.setText("");
            convertView.setPadding(0, 0, 0, 0);
            holder.separator.setVisibility(View.GONE);

            Integer entriesNumber = null;
            if (position == 0 || position == 1 || position == 2 || position == 3) {
                if (position == 0) {
                    entriesNumber = mAllNumber;
                    holder.titleTxt.setText(R.string.all);
                    holder.iconView.setImageResource(R.drawable.ic_statusbar_rss);
                }
                else if (position == 1) {
                    entriesNumber = mRecentNumber;
                    holder.titleTxt.setText(R.string.recent);
                    holder.iconView.setImageResource(R.drawable.ic_reorder);
                }
                else if (position == 2) {
                    entriesNumber = mLaterReadingNumber;
                    holder.titleTxt.setText(R.string.later_reading);
                    holder.iconView.setImageResource(R.drawable.ic_book_black);
                }
                else {
                    entriesNumber = mFavoritesNumber;
                    holder.titleTxt.setText(R.string.favorites);
                    holder.iconView.setImageResource(R.drawable.ic_star);
                    holder.separator.setVisibility(View.VISIBLE);
                }

                if (position == mSelectedItem) {
                    holder.iconView.setColorFilter(ContextCompat.getColor(mContext, PrefUtils.getBoolean(PrefUtils.LIGHT_THEME, true) ? R.color.light_primary_color : R.color.dark_accent_color));
                } else {
                    holder.iconView.setColorFilter(ContextCompat.getColor(mContext, PrefUtils.getBoolean(PrefUtils.LIGHT_THEME, true) ? R.color.light_base_text : R.color.dark_base_text));
                }
            }
            if (mFeedsCursor != null && mFeedsCursor.moveToPosition(position - 4)) {
                final long id = mFeedsCursor.getLong(POS_ID);
                entriesNumber = entriesNumbers.get(id);
                holder.titleTxt.setText((mFeedsCursor.isNull(POS_NAME) ? mFeedsCursor.getString(POS_URL) : mFeedsCursor.getString(POS_NAME)));

                if (mFeedsCursor.getInt(POS_IS_GROUP) == 1) {
                    holder.titleTxt.setAllCaps(true);
                    holder.iconView.setImageResource(isGroupExpanded(position) ? R.drawable.group_expanded : R.drawable.group_collapsed);
                    holder.iconView.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            ContentResolver cr = MainApplication.getContext().getContentResolver();
                            ContentValues values = new ContentValues();
                            values.put(FeedData.FeedColumns.IS_GROUP_EXPANDED, isGroupExpanded(position) ? null : 1);
                            cr.update(FeedData.FeedColumns.CONTENT_URI(getItemId(position)), values, null, null);
                        }
                    });
                    holder.titleTxt.setTextColor(GROUP_TEXT_COLOR);
                    holder.titleTxt.setAllCaps(false);
                } else {
                    holder.stateTxt.setVisibility(View.VISIBLE);

                    if (mFeedsCursor.isNull(POS_ERROR)) {
                        long timestamp = mFeedsCursor.getLong(POS_LAST_UPDATE);

                        // Date formatting is expensive, look at the cache
                        String formattedDate = mFormattedDateCache.get(timestamp);
                        if (formattedDate == null) {

                            formattedDate = mContext.getString(R.string.update) + COLON;

                            if (timestamp == 0) {
                                formattedDate += mContext.getString(R.string.never);
                            } else {
                                formattedDate += StringUtils.getDateTimeString(timestamp);
                            }

                            mFormattedDateCache.put(timestamp, formattedDate);
                        }

                        holder.stateTxt.setText(formattedDate);
                    } else {
                        holder.stateTxt.setText(new StringBuilder(mContext.getString(R.string.error)).append(COLON).append(mFeedsCursor.getString(POS_ERROR)));
                    }

                    Bitmap bitmap = UiUtils.getFaviconBitmap(id, mFeedsCursor, POS_ICON);
                    if (bitmap != null) {
                        holder.iconView.setImageBitmap(bitmap);
                    } else {
                        ColorGenerator generator = ColorGenerator.DEFAULT;
                        int color = generator.getColor(id);
                        TextDrawable textDrawable = TextDrawable.builder().buildRound(holder.titleTxt.getText().toString().substring(0, 1).toUpperCase(), color);
                        holder.iconView.setImageDrawable(textDrawable);
                    }
                }
                if ((mFeedsCursor.isNull(POS_NAME) ? mFeedsCursor.getString(POS_URL) : mFeedsCursor.getString(POS_NAME)).startsWith("ERROR:")) {
                    LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                    convertView = inflater.inflate(R.layout.item_drawer_null, parent, false);
                    return convertView;
                }
            }
            if (entriesNumber != null && entriesNumber > 0) {
                holder.entriesNumberTxt.setText(String.valueOf(entriesNumber));
            }
        }
        return convertView;
    }

    @Override
    public int getCount() {
        if (mFeedsCursor != null) {
            return mFeedsCursor.getCount() + 4;
        }
        return 0;
    }

    @Override
    public Object getItem(int position) {
        return null;
    }

    @Override
    public long getItemId(int position) {
        if (mFeedsCursor != null && mFeedsCursor.moveToPosition(position - 4)) {
            return mFeedsCursor.getLong(POS_ID);
        }

        return -1;
    }

    public byte[] getItemIcon(int position) {
        if (mFeedsCursor != null && mFeedsCursor.moveToPosition(position - 4)) {
            return mFeedsCursor.getBlob(POS_ICON);
        }

        return null;
    }

    public String getItemName(int position) {
        if (mFeedsCursor != null && mFeedsCursor.moveToPosition(position - 4)) {
            return mFeedsCursor.isNull(POS_NAME) ? mFeedsCursor.getString(POS_URL) : mFeedsCursor.getString(POS_NAME);
        }

        return null;
    }

    public boolean isItemAGroup(int position) {
        return mFeedsCursor != null && mFeedsCursor.moveToPosition(position - 4) && mFeedsCursor.getInt(POS_IS_GROUP) == 1;

    }

    private boolean isGroupExpanded(int position) {
        return mFeedsCursor != null && mFeedsCursor.moveToPosition(position - 4) && mFeedsCursor.getInt(POS_IS_GROUP_EXPANDED) == 1;
    }

    private void getEntriesNumbers() {
        mAllNumber = mRecentNumber = mLaterReadingNumber = mFavoritesNumber = 0;
        entriesNumbers = new HashMap<>();

        if (mFeedsCursor == null)
            return;

        new Thread(){
            @Override
            public void run() {
                boolean showRead = PrefUtils.getBoolean(PrefUtils.SHOW_READ, true);
                ContentResolver contentResolver = mContext.getContentResolver();

                // Gets the numbers of entries
                // all entries
                Cursor cursor = contentResolver.query(EntryColumns.ALL_ENTRIES_CONTENT_URI, new String[]{Constants.DB_COUNT},
                        showRead ? null : EntryColumns.WHERE_UNREAD, null, null);
                if (cursor != null) {
                    if (cursor.moveToFirst())
                        mAllNumber = cursor.getInt(0);
                    cursor.close();
                }

                // recent entries
                cursor = contentResolver.query(EntryColumns.RECENT_ENTRIES_CONTENT_URI, new String[]{Constants.DB_COUNT},
                        showRead ? null : EntryColumns.WHERE_UNREAD, null, null);
                if (cursor != null) {
                    if (cursor.moveToFirst())
                        mRecentNumber = cursor.getInt(0);
                    cursor.close();
                }

                // later reading entries
                cursor = contentResolver.query(EntryColumns.LATER_READING_ENTRIES_CONTENT_URI, new String[]{Constants.DB_COUNT},
                        null, null, null);
                if (cursor != null) {
                    if (cursor.moveToFirst())
                        mLaterReadingNumber = cursor.getInt(0);
                    cursor.close();
                }

                // favorite entries
                cursor = contentResolver.query(EntryColumns.FAVORITES_CONTENT_URI, new String[]{Constants.DB_COUNT},
                        null, null, null);
                if (cursor != null) {
                    if (cursor.moveToFirst())
                        mFavoritesNumber = cursor.getInt(0);
                    cursor.close();
                }

                // entries in feeds
                cursor = contentResolver.query(EntryColumns.CONTENT_URI, new String[]{EntryColumns.FEED_ID, Constants.DB_COUNT},
                        (showRead ? "1=1" : EntryColumns.WHERE_UNREAD) + ") GROUP BY (" + EntryColumns.FEED_ID, null, null);
                if (cursor != null) {
                    while (cursor.moveToNext()) {
                        long feedId = cursor.getLong(0);
                        int number = cursor.getInt(1);
                        entriesNumbers.put(feedId, number);
                    }
                    cursor.close();
                }

                // entries in groups
                cursor = contentResolver.query(FeedColumns.CONTENT_URI, new String[]{FeedColumns._ID, FeedColumns.GROUP_ID},
                        FeedColumns.GROUP_ID + Constants.DB_IS_NOT_NULL, null, null);
                if (cursor != null) {
                    while (cursor.moveToNext()) {
                        long feedId = cursor.getLong(0);
                        long groupId = cursor.getLong(1);
                        Integer number = entriesNumbers.get(feedId);
                        Integer sum = entriesNumbers.get(groupId);
                        if (sum == null) sum = 0;
                        if (number != null) sum += number;
                        entriesNumbers.put(groupId, sum);
                    }
                    cursor.close();
                }

                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        notifyDataSetChanged();
                    }
                });
            }
        }.start();
    }

    private static class ViewHolder {
        public ImageView iconView;
        public TextView titleTxt;
        public TextView stateTxt;
        public TextView entriesNumberTxt;
        public View separator;
    }
}
