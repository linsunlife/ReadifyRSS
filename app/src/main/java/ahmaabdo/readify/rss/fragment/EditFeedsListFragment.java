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

package ahmaabdo.readify.rss.fragment;

import ahmaabdo.readify.rss.activity.EditFeedActivity;
import ahmaabdo.readify.rss.utils.ToastUtils;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ListFragment;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.EditText;
import android.widget.ExpandableListView;
import android.widget.ListView;

import ahmaabdo.readify.rss.R;
import ahmaabdo.readify.rss.activity.AddGoogleNewsActivity;
import ahmaabdo.readify.rss.adapter.FeedsCursorAdapter;
import ahmaabdo.readify.rss.parser.OPML;
import ahmaabdo.readify.rss.provider.FeedData.FeedColumns;
import ahmaabdo.readify.rss.view.DragNDropExpandableListView;
import ahmaabdo.readify.rss.view.DragNDropListener;

public class EditFeedsListFragment extends ListFragment {

    private static final int REQUEST_CODE_IMPORT_OPML = 1;
    private static final int REQUEST_CODE_EXPORT_OPML = 2;

    private DragNDropExpandableListView mListView;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        setHasOptionsMenu(true);
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_edit_feed_list, container, false);

        mListView = rootView.findViewById(android.R.id.list);
        mListView.setOnChildClickListener(new ExpandableListView.OnChildClickListener() {
            @Override
            public boolean onChildClick(ExpandableListView parent, View v, int groupPosition, int childPosition, long id) {
                startActivity(new Intent(Intent.ACTION_EDIT, FeedColumns.CONTENT_URI(id), getActivity(), EditFeedActivity.class));
                return true;
            }
        });
        mListView.setOnGroupClickListener(new ExpandableListView.OnGroupClickListener() {
            @Override
            public boolean onGroupClick(ExpandableListView parent, View v, int groupPosition, long id) {
                if (v.findViewById(R.id.indicator).getVisibility() != View.VISIBLE) { // This is no a real group
                    startActivity(new Intent(Intent.ACTION_EDIT, FeedColumns.CONTENT_URI(id), getActivity(), EditFeedActivity.class));
                    return true;
                }
                return false;
            }
        });
        mListView.setOnItemLongClickListener(new OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, final View view, final int position, long id) {
                new AlertDialog.Builder(getActivity())
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .setMessage(R.string.question_delete_feed)
                        .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                new Thread() {
                                    @Override
                                    public void run() {
                                        try {
                                            boolean isGroup = view.findViewById(R.id.indicator).getVisibility() == View.VISIBLE;
                                            long feedId = mListView.getItemIdAtPosition(position);
                                            Uri uri = isGroup ? FeedColumns.GROUPS_CONTENT_URI(feedId) : FeedColumns.CONTENT_URI(feedId);
                                            if (getActivity().getContentResolver().delete(uri, null, null) <= 0)
                                                ToastUtils.showShort(R.string.error);
                                        } catch (Exception e) {
                                            ToastUtils.showLong(String.format(getString(R.string.action_failed), e.getMessage()));
                                        }
                                    }
                                }.start();
                            }
                        }).setNegativeButton(android.R.string.no, null).show();

                return true;
            }
        });

        mListView.setAdapter(new FeedsCursorAdapter(getActivity(), FeedColumns.GROUPS_CONTENT_URI));

        mListView.setDragNDropListener(new DragNDropListener() {
            boolean fromHasGroupIndicator = false;

            @Override
            public void onStopDrag(View itemView) {
            }

            @Override
            public void onStartDrag(View itemView) {
                fromHasGroupIndicator = itemView.findViewById(R.id.indicator).getVisibility() == View.VISIBLE;
            }

            @Override
            public void onDrop(final int flatPosFrom, final int flatPosTo) {
                final boolean fromIsGroup = ExpandableListView.getPackedPositionType(mListView.getExpandableListPosition(flatPosFrom)) == ExpandableListView.PACKED_POSITION_TYPE_GROUP;
                final boolean toIsGroup = ExpandableListView.getPackedPositionType(mListView.getExpandableListPosition(flatPosTo)) == ExpandableListView.PACKED_POSITION_TYPE_GROUP;

                final boolean fromIsFeedWithoutGroup = fromIsGroup && !fromHasGroupIndicator;

                View toView = mListView.getChildAt(flatPosTo - mListView.getFirstVisiblePosition());
                boolean toIsFeedWithoutGroup = toIsGroup && toView.findViewById(R.id.indicator).getVisibility() != View.VISIBLE;

                final long packedPosTo = mListView.getExpandableListPosition(flatPosTo);
                final int packedGroupPosTo = ExpandableListView.getPackedPositionGroup(packedPosTo);

                if ((fromIsFeedWithoutGroup || !fromIsGroup) && toIsGroup && !toIsFeedWithoutGroup) {
                    new AlertDialog.Builder(getActivity()) //
                            .setTitle(R.string.to_group_title) //
                            .setMessage(R.string.to_group_message) //
                            .setPositiveButton(R.string.to_group_into, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    ContentValues values = new ContentValues();
                                    values.put(FeedColumns.PRIORITY, 1);
                                    values.put(FeedColumns.GROUP_ID, mListView.getItemIdAtPosition(flatPosTo));

                                    ContentResolver cr = getActivity().getContentResolver();
                                    cr.update(FeedColumns.CONTENT_URI(mListView.getItemIdAtPosition(flatPosFrom)), values, null, null);
                                    cr.notifyChange(FeedColumns.GROUPS_CONTENT_URI, null);
                                }
                            }).setNegativeButton(R.string.to_group_above, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            moveItem(fromIsGroup, toIsGroup, fromIsFeedWithoutGroup, packedPosTo, packedGroupPosTo, flatPosFrom);
                        }
                    }).show();
                } else {
                    moveItem(fromIsGroup, toIsGroup, fromIsFeedWithoutGroup, packedPosTo, packedGroupPosTo, flatPosFrom);
                }
            }

            @Override
            public void onDrag(int x, int y, ListView listView) {
            }
        });

        return rootView;
    }

    private void moveItem(boolean fromIsGroup, boolean toIsGroup, boolean fromIsFeedWithoutGroup, long packedPosTo, int packedGroupPosTo,
                          int flatPosFrom) {
        ContentValues values = new ContentValues();
        ContentResolver cr = getActivity().getContentResolver();

        if (fromIsGroup && toIsGroup) {
            values.put(FeedColumns.PRIORITY, packedGroupPosTo + 1);
            cr.update(FeedColumns.CONTENT_URI(mListView.getItemIdAtPosition(flatPosFrom)), values, null, null);
        } else if (!fromIsGroup && toIsGroup) {
            values.put(FeedColumns.PRIORITY, packedGroupPosTo + 1);
            values.putNull(FeedColumns.GROUP_ID);
            cr.update(FeedColumns.CONTENT_URI(mListView.getItemIdAtPosition(flatPosFrom)), values, null, null);
        } else if ((!fromIsGroup && !toIsGroup) || (fromIsFeedWithoutGroup && !toIsGroup)) {
            int groupPrio = ExpandableListView.getPackedPositionChild(packedPosTo) + 1;
            values.put(FeedColumns.PRIORITY, groupPrio);

            int flatGroupPosTo = mListView.getFlatListPosition(ExpandableListView.getPackedPositionForGroup(packedGroupPosTo));
            values.put(FeedColumns.GROUP_ID, mListView.getItemIdAtPosition(flatGroupPosTo));
            cr.update(FeedColumns.CONTENT_URI(mListView.getItemIdAtPosition(flatPosFrom)), values, null, null);
        }
    }

    @Override
    public void onDestroy() {
        getLoaderManager().destroyLoader(0); // This is needed to avoid an activity leak!
        super.onDestroy();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.feed_list, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_add_feed: {
                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                builder.setTitle(R.string.menu_add_feed)
                        .setItems(new CharSequence[]{getString(R.string.add_custom_feed), getString(R.string.google_news_title)}, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                if (which == 0) {
                                    startActivity(new Intent(Intent.ACTION_INSERT, FeedColumns.CONTENT_URI, getActivity(), EditFeedActivity.class));
                                } else {
                                    startActivity(new Intent(getActivity(), AddGoogleNewsActivity.class));
                                }
                            }
                        });
                builder.show();
                return true;
            }
            case R.id.menu_add_group: {
                final EditText input = new EditText(getActivity());
                input.setSingleLine(true);
                new AlertDialog.Builder(getActivity()) //
                        .setTitle(R.string.add_group_title) //
                        .setView(input) //
                        // .setMessage(R.string.add_group_sentence) //
                        .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                new Thread() {
                                    @Override
                                    public void run() {
                                        String groupName = input.getText().toString();
                                        if (!groupName.isEmpty()) {
                                            ContentResolver cr = getActivity().getContentResolver();
                                            ContentValues values = new ContentValues();
                                            values.put(FeedColumns.IS_GROUP, true);
                                            values.put(FeedColumns.NAME, groupName);
                                            cr.insert(FeedColumns.GROUPS_CONTENT_URI, values);
                                        }
                                    }
                                }.start();
                            }
                        }).setNegativeButton(android.R.string.cancel, null).show();
                return true;
            }
            case R.id.menu_export:
                Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
                intent.putExtra(Intent.EXTRA_TITLE, "Readify_" + System.currentTimeMillis() + ".opml"); // 设置默认文件名
                intent.setType("*/*");
                startActivityForResult(intent, REQUEST_CODE_EXPORT_OPML);
                return true;
            case R.id.menu_import: {
                Intent intent1 = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                intent1.setType("*/*");
                startActivityForResult(intent1, REQUEST_CODE_IMPORT_OPML);
                return true;
            }
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, final Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK) {
            final Uri uri = data.getData();
            final ContentResolver contentResolver = getActivity().getContentResolver();
            if (requestCode == REQUEST_CODE_IMPORT_OPML) {
                new Thread(new Runnable() { // To not block the UI
                    @Override
                    public void run() {
                        try {
                            OPML.importFrom(contentResolver.openInputStream(uri));
                        } catch (Exception e) {
                            ToastUtils.showLong(R.string.error_feed_import);
                        }
                    }
                }).start();
            } else if (requestCode == REQUEST_CODE_EXPORT_OPML) {
                new Thread(new Runnable() {
                    @SuppressLint("StringFormatInvalid")
                    @Override
                    public void run() {
                        try {
                            OPML.exportTo(contentResolver.openOutputStream(uri));
                            ToastUtils.showLong(String.format(getString(R.string.message_exported_to), uri.getPath()));
                        } catch (Exception e) {
                            ToastUtils.showLong(R.string.error_feed_export);
                        }
                    }
                }).start();
            }
        }
    }

}
