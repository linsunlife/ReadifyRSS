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

package ahmaabdo.readify.rss.adapter;

import ahmaabdo.readify.rss.activity.EntryActivity;
import android.content.*;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.provider.BaseColumns;
import android.text.Html;
import android.text.TextUtils;
import android.view.*;
import android.widget.*;

import com.amulyakhare.textdrawable.TextDrawable;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CenterCrop;

import ahmaabdo.readify.rss.Constants;
import ahmaabdo.readify.rss.MainApplication;
import ahmaabdo.readify.rss.R;
import ahmaabdo.readify.rss.provider.FeedData;
import ahmaabdo.readify.rss.provider.FeedData.EntryColumns;
import ahmaabdo.readify.rss.provider.FeedData.FeedColumns;
import ahmaabdo.readify.rss.utils.NetworkUtils;
import ahmaabdo.readify.rss.utils.PrefUtils;
import ahmaabdo.readify.rss.utils.StringUtils;
import jp.wasabeef.glide.transformations.RoundedCornersTransformation;

import java.util.ArrayList;
import java.util.List;

public class EntriesCursorAdapter extends ResourceCursorAdapter {

    private final Uri mUri;
    private final boolean mShowFeedInfo;
    private int mIdPos, mTitlePos, mMainImgPos, mDatePos, mAuthorPos, mIsReadPos, mFavoritePos, mFeedNamePos;
    private long mEntriesListDisplayDate;

    public EntriesCursorAdapter(Context context, Uri uri, Cursor cursor, boolean showFeedInfo, long entriesListDisplayDate) {
        super(context, R.layout.item_entry_list, cursor, 0);
        mUri = uri;
        mShowFeedInfo = showFeedInfo;
        mEntriesListDisplayDate = entriesListDisplayDate;

        reinit(cursor);
    }

    @Override
    public void bindView(final View view, final Context context, Cursor cursor) {
        if (view.getTag(R.id.holder) == null) {
            ViewHolder holder = new ViewHolder();
            holder.titleTextView = view.findViewById(android.R.id.text1);
            holder.dateTextView = view.findViewById(android.R.id.text2);
            holder.authorTextView = view.findViewById(R.id.author);
            holder.mainImgView = view.findViewById(R.id.main_icon);
            holder.starImgView = view.findViewById(R.id.favorite_icon);
            view.setTag(R.id.holder, holder);
        }

        final ViewHolder holder = (ViewHolder) view.getTag(R.id.holder);

        String titleText = cursor.getString(mTitlePos);
        holder.titleTextView.setText(titleText);

        String authorText = cursor.getString(mAuthorPos);
        if (authorText == null || authorText.isEmpty()) {
            holder.authorTextView.setVisibility(View.GONE);
        } else {
            holder.authorTextView.setText(authorText);
            holder.authorTextView.setVisibility(View.VISIBLE);
        }

        long entryID = cursor.getLong(mIdPos);
        String mainImgUrl = cursor.getString(mMainImgPos);
        mainImgUrl = TextUtils.isEmpty(mainImgUrl) ? null : NetworkUtils.getDownloadedOrDistantImageUrl(entryID, mainImgUrl);

        if (mainImgUrl != null && PrefUtils.getBoolean(PrefUtils.DISPLAY_IMAGES, true)) {
            holder.mainImgView.setVisibility(View.VISIBLE);
            int radius = 30; // 圆角半径，单位为像素
            int margin = 0; // 可选参数，设置圆角与 ImageView 边缘的距离
            TextDrawable textDrawable = TextDrawable.builder().buildRoundRect("!", Color.GRAY, radius);
            Glide.with(context)
                    .load(mainImgUrl)
                    .bitmapTransform(new CenterCrop(context), new RoundedCornersTransformation(context, radius, margin))
                    .error(textDrawable)
                    .into(holder.mainImgView);
        } else {
            holder.mainImgView.setVisibility(View.GONE);
        }

        holder.isFavorite = cursor.getInt(mFavoritePos) == 1;
        if (holder.isFavorite) {
            holder.starImgView.setImageResource(PrefUtils.getBoolean(PrefUtils.LIGHT_THEME, false) ? R.drawable.favorite_light : R.drawable.favorite_dark);
            holder.starImgView.setVisibility(View.VISIBLE);
        }
        else {
            holder.starImgView.setVisibility(View.GONE);
        }

        if (mShowFeedInfo && mFeedNamePos > -1) {
            String feedName = cursor.getString(mFeedNamePos);
            if (feedName != null) {
                holder.dateTextView.setText(Html.fromHtml(new StringBuilder("<font color='#247ab0'>").append(feedName).append("</font>").append(Constants.COMMA_SPACE).append(StringUtils.getDateTimeString(cursor.getLong(mDatePos))).toString()));
            } else {
                holder.dateTextView.setText(StringUtils.getDateTimeString(cursor.getLong(mDatePos)));
            }
        } else {
            holder.dateTextView.setText(StringUtils.getDateTimeString(cursor.getLong(mDatePos)));
        }

        if (cursor.isNull(mIsReadPos)) {
            holder.titleTextView.setEnabled(true);
            holder.dateTextView.setEnabled(true);
            holder.authorTextView.setEnabled(true);
            holder.isRead = false;
        } else {
            holder.titleTextView.setEnabled(false);
            holder.dateTextView.setEnabled(false);
            holder.authorTextView.setEnabled(false);
            holder.isRead = true;
        }
    }

    @Override
    public View getView(final int position, final View convertView, final ViewGroup parent) {
        final View view = super.getView(position, convertView, parent);
        final long id = EntriesCursorAdapter.this.getItemId(position);

        // 单击
        view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (id >= 0) { // should not happen, but I had a crash with this on PlayStore...
                    Intent intent = new Intent(MainApplication.getContext(), EntryActivity.class);
                    intent.setData(ContentUris.withAppendedId(mUri, id));
                    intent.putExtra(Constants.EntriesListDisplayDate, mEntriesListDisplayDate);
                    MainApplication.getContext().startActivity(intent);
                }
            }
        });

        // 长按
        view.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(final View v) {
                PopupMenu popupMenu = new PopupMenu(v.getContext(), v);
                popupMenu.getMenuInflater().inflate(R.menu.entry_list_popup_menu, popupMenu.getMenu());
                popupMenu.setGravity(Gravity.END);
                popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        List<Long> ids = new ArrayList<>();
                        switch (item.getItemId()) {
                            case R.id.read_above:
                                for (int i = 0; i < position; i++) {
                                    ids.add(EntriesCursorAdapter.this.getItemId(i));
                                }
                                readEntries(ids);
                                return true;
                            case R.id.read_below:
                                for (int i = position + 1; i < EntriesCursorAdapter.this.getCount(); i++) {
                                    ids.add(EntriesCursorAdapter.this.getItemId(i));
                                }
                                readEntries(ids);
                                return true;
                            case R.id.toggle_read_state:
                                toggleReadState(id, v);
                                return true;
                            case R.id.toggle_favorite_state:
                                toggleFavoriteState(id, v);
                                return true;
                            default:
                                return false;
                        }
                    }
                });
                popupMenu.show();
                return true;
            }
        });

        // 左右滑动
        view.setOnTouchListener(new View.OnTouchListener() {
            private float startX = 0;
            private float startY = 0;
            private final float touchSlop = ViewConfiguration.get(view.getContext()).getScaledTouchSlop();
            private boolean isPressed = false;
            private final Handler handler = new Handler(Looper.getMainLooper());
            private final Runnable longPressRunnable = new Runnable() {
                @Override
                public void run() {
                    if (isPressed) {
                        view.performLongClick();
                        // 长按事件触发后，将 isPressed 设置为 false，避免后续触发点击事件
                        isPressed = false;
                    }
                }
            };

            @Override
            public boolean onTouch(final View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        startX = event.getRawX();
                        startY = event.getRawY();
                        // 标志位，表示已经按下
                        isPressed = true;
                        // 发送延时消息，长按时间到达后执行 longPressRunnable
                        handler.postDelayed(longPressRunnable, ViewConfiguration.getLongPressTimeout());
                        return true;
                    case MotionEvent.ACTION_MOVE:
                        float distanceX = Math.abs(event.getRawX() - startX);
                        float distanceY = Math.abs(event.getRawY() - startY);
                        // 判断是否有移动
                        if (distanceX > 0 || distanceY > 0) {
                            // 移除延时消息，取消长按事件
                            isPressed = false;
                            handler.removeCallbacks(longPressRunnable);
                        }
                        // 在水平方向上的滑动距离大于竖直方向的，则认为是有效的滑动
                        if (distanceX > distanceY && distanceX > touchSlop) {
                            // 请求父视图不要拦截事件
                            v.getParent().requestDisallowInterceptTouchEvent(true);
                            // 实现滑动效果
                            v.setTranslationX(event.getRawX() - startX);
                        }
                        return true;
                    case MotionEvent.ACTION_UP:
                        // 手指抬起时，移除延时消息，避免误触发长按事件
                        handler.removeCallbacks(longPressRunnable);
                        // 允许父视图拦截事件，以便父视图可以处理后续的点击事件
                        v.getParent().requestDisallowInterceptTouchEvent(false);
                        float translationX = v.getTranslationX();
                        // 动画恢复原位
                        if (translationX != 0)
                            v.animate().translationX(0).setDuration(200).start();
                        // 根据滑动距离判断左滑还是右滑，并执行相应操作
                        if (translationX > touchSlop) {
                            // 右滑操作
                            toggleFavoriteState(id, v);
                        } else if (translationX < -touchSlop) {
                            // 左滑操作
                            toggleReadState(id, v);
                        } else if (isPressed) {
                            v.performClick();
                            isPressed = false;
                        }
                        return true;
                    default:
                        isPressed = false;
                        handler.removeCallbacks(longPressRunnable);
                        v.getParent().requestDisallowInterceptTouchEvent(false);
                        if (v.getTranslationX() != 0)
                            v.animate().translationX(0).setDuration(200).start();
                        return false;
                }
            }
        });
        return view;
    }

    private void readEntries(final List<Long> ids) {
        new Thread() {
            @Override
            public void run() {
                ContentResolver cr = MainApplication.getContext().getContentResolver();
                String where = BaseColumns._ID + " IN (" + TextUtils.join(",", ids) + ')';
                cr.update(mUri, FeedData.getReadContentValues(), where, null);
            }
        }.start();
    }

    public void toggleReadState(final long id, View view) {
        final ViewHolder holder = (ViewHolder) view.getTag(R.id.holder);

        if (holder != null) { // should not happen, but I had a crash with this on PlayStore...
            holder.isRead = !holder.isRead;

            if (holder.isRead) {
                holder.titleTextView.setEnabled(false);
                holder.dateTextView.setEnabled(false);
            } else {
                holder.titleTextView.setEnabled(true);
                holder.dateTextView.setEnabled(true);
            }

            new Thread() {
                @Override
                public void run() {
                    ContentResolver cr = MainApplication.getContext().getContentResolver();
                    Uri entryUri = ContentUris.withAppendedId(mUri, id);
                    cr.update(entryUri, holder.isRead ? FeedData.getReadContentValues() : FeedData.getUnreadContentValues(), null, null);
                }
            }.start();
        }
    }

    public void toggleFavoriteState(final long id, View view) {
        final ViewHolder holder = (ViewHolder) view.getTag(R.id.holder);
        if (holder != null) { // should not happen, but I had a crash with this on PlayStore...
            holder.isFavorite = !holder.isFavorite;

            new Thread() {
                @Override
                public void run() {
                    ContentValues values = new ContentValues();
                    values.put(EntryColumns.IS_FAVORITE, holder.isFavorite ? 1 : 0);

                    ContentResolver cr = MainApplication.getContext().getContentResolver();
                    Uri entryUri = ContentUris.withAppendedId(mUri, id);
                    cr.update(entryUri, values, null, null);
                }
            }.start();
        }
    }


    @Override
    public void changeCursor(Cursor cursor) {
        reinit(cursor);
        super.changeCursor(cursor);
    }

    @Override
    public Cursor swapCursor(Cursor newCursor) {
        reinit(newCursor);
        return super.swapCursor(newCursor);
    }

    @Override
    public void notifyDataSetChanged() {
        reinit(null);
        super.notifyDataSetChanged();
    }

    @Override
    public void notifyDataSetInvalidated() {
        reinit(null);
        super.notifyDataSetInvalidated();
    }

    private void reinit(Cursor cursor) {
        if (cursor != null && cursor.getCount() > 0) {
            mIdPos = cursor.getColumnIndex(EntryColumns._ID);
            mTitlePos = cursor.getColumnIndex(EntryColumns.TITLE);
            mMainImgPos = cursor.getColumnIndex(EntryColumns.IMAGE_URL);
            mDatePos = cursor.getColumnIndex(EntryColumns.DATE);
            mAuthorPos = cursor.getColumnIndex(EntryColumns.AUTHOR);
            mIsReadPos = cursor.getColumnIndex(EntryColumns.IS_READ);
            mFavoritePos = cursor.getColumnIndex(EntryColumns.IS_FAVORITE);
            mFeedNamePos = cursor.getColumnIndex(FeedColumns.NAME);
        }
    }

    private static class ViewHolder {
        public TextView authorTextView, dateTextView, titleTextView;
        public ImageView mainImgView, starImgView;
        public boolean isRead, isFavorite;
    }
}
