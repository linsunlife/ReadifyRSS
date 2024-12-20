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

import ahmaabdo.readify.rss.Constants;
import ahmaabdo.readify.rss.R;
import ahmaabdo.readify.rss.activity.AboutApp;
import ahmaabdo.readify.rss.service.RefreshService;
import ahmaabdo.readify.rss.utils.FileUtils;
import ahmaabdo.readify.rss.utils.PrefUtils;
import ahmaabdo.readify.rss.utils.ToastUtils;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.support.v4.provider.DocumentFile;
import android.util.Log;
import com.bumptech.glide.Glide;
import org.json.JSONObject;

import java.io.*;
import java.util.Iterator;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class GeneralPrefsFragment extends PreferenceFragment {

    private static final String TAG = GeneralPrefsFragment.class.getSimpleName();

    private static final String DB_FILE_NAME = "data.db";
    private static final String SETTINGS_FILE_NAME = "settings.json";
    private static final String BACKUP_FILE_NAME = "Readify.zip";

    private static final int REQUEST_CODE_RESTORE = 1;
    private static final int REQUEST_CODE_SELECT_BACKUP_PATH = 2;
    private static final int REQUEST_CODE_BACKUP_MISSING_BACKUP_PATH = 3;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.general_preferences);

        findPreference(PrefUtils.REFRESH_ENABLED).setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                Activity activity = getActivity();
                if (activity != null) {
                    if (Boolean.TRUE.equals(newValue)) {
                        activity.startService(new Intent(activity, RefreshService.class));
                    } else {
                        PrefUtils.putLong(PrefUtils.LAST_SCHEDULED_REFRESH, 0);
                        activity.stopService(new Intent(activity, RefreshService.class));
                    }
                }
                return true;
            }
        });

        findPreference(PrefUtils.LIGHT_THEME).setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                PrefUtils.putBoolean(PrefUtils.LIGHT_THEME, Boolean.TRUE.equals(newValue));
                PreferenceManager.getDefaultSharedPreferences(getContext()).edit().commit(); // to be sure all prefs are written
                android.os.Process.killProcess(android.os.Process.myPid()); // Restart the app
                // this return statement will never be reached
                return true;
            }
        });

        findPreference(PrefUtils.ABOUT).setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                startActivity(new Intent(getActivity(), AboutApp.class));
                return true;
            }
        });

        Preference backupPathPreference = findPreference(PrefUtils.BACKUP_PATH);
        backupPathPreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                startActivityForResult(new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE), REQUEST_CODE_SELECT_BACKUP_PATH);
                return true;
            }
        });
        String backupPath = PrefUtils.getString(PrefUtils.BACKUP_PATH, null);
        if (backupPath != null) {
            backupPathPreference.setSummary(backupPath);
        }

        findPreference(PrefUtils.BACKUP).setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                if (PrefUtils.getString(PrefUtils.BACKUP_PATH, null) == null) {
                    new AlertDialog.Builder(getActivity())
                            .setTitle(R.string.question_backup_path_permission_title)
                            .setMessage(R.string.question_backup_path_permission)
                            .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    startActivityForResult(new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE), REQUEST_CODE_BACKUP_MISSING_BACKUP_PATH);
                                }
                            }).setNegativeButton(android.R.string.no, null).show();
                } else
                    backup();
                return true;
            }
        });

        findPreference(PrefUtils.RESTORE).setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                intent.setType("application/zip");
                startActivityForResult(intent, REQUEST_CODE_RESTORE);
                return true;
            }
        });

        findPreference(PrefUtils.CLEAR_IMAGE_CACHE).setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                final Context context = getContext();
                final ProgressDialog progressDialog = new ProgressDialog(context);
                progressDialog.setMessage(getString(R.string.loading));
                progressDialog.setCancelable(false);
                progressDialog.show();

                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            long keepTime = Long.parseLong(PrefUtils.getString(PrefUtils.KEEP_TIME, "4")) * 86400000l;
                            long keepDateBorderTime = keepTime > 0 ? System.currentTimeMillis() - keepTime : 0;
                            clearOldImageCache(Glide.getPhotoCacheDir(context), "^[a-fA-F0-9]{64}\\.[0-9]$", keepDateBorderTime);
                            clearOldImageCache(new File(context.getCacheDir(), "WebView/Default/HTTP Cache/Cache_Data"), "^[a-fA-F0-9]{16}_[0-9]$", keepDateBorderTime);
                            ToastUtils.showLong(R.string.action_finished);
                        } catch (final Exception e) {
                            Log.e(TAG, "Failed to clear image cache", e);
                            ToastUtils.showLong(String.format(getString(R.string.action_failed), e.getMessage()));
                        } finally {
                            progressDialog.cancel();
                        }
                    }
                }).start();
                return true;
            }
        });

    }

    @Override
    public void onResume() {
        super.onResume();
    }

    private void backup() {
        final Context context = getContext();
        final ProgressDialog progressDialog = new ProgressDialog(context);
        progressDialog.setMessage(getString(R.string.loading));
        progressDialog.setCancelable(false);
        progressDialog.show();

        new Thread(new Runnable() { // To not block the UI
            @Override
            public void run() {
                try {
                    // Temporarily store backups in the cache directory
                    File cacheDir = new File(context.getCacheDir(), "backup");
                    if (cacheDir.exists()) {
                        FileUtils.deleteFileOrDir(cacheDir);
                    }
                    cacheDir.mkdirs();

                    // Back up the database
                    File databaseFile = context.getDatabasePath(Constants.DATABASE_NAME);
                    FileUtils.copy(databaseFile, new File(cacheDir, DB_FILE_NAME));

                    // Back up the app settings
                    Map<String, ?> prefs = PrefUtils.getAll();
                    prefs.remove(PrefUtils.BACKUP_PATH); // Skip the backup path
                    JSONObject jsonObject = new JSONObject(prefs);
                    FileWriter fileWriter = new FileWriter(new File(cacheDir, SETTINGS_FILE_NAME));
                    fileWriter.write(jsonObject.toString(4));
                    fileWriter.flush();
                    fileWriter.close();

                    // Create the final backup file path
                    String backupPath = PrefUtils.getString(PrefUtils.BACKUP_PATH, null);
                    DocumentFile backupFolder = DocumentFile.fromTreeUri(context, Uri.parse(backupPath));
                    DocumentFile backupFile = backupFolder.findFile(BACKUP_FILE_NAME);
                    if (backupFile != null && backupFile.exists()) {
                        backupFile.delete();
                    }
                    backupFile = backupFolder.createFile("application/zip", BACKUP_FILE_NAME);

                    // Zip up the backup data
                    OutputStream outputStream = context.getContentResolver().openOutputStream(backupFile.getUri());
                    ZipOutputStream zipOutputStream = new ZipOutputStream(outputStream);
                    putNextEntry(zipOutputStream, cacheDir, DB_FILE_NAME);
                    putNextEntry(zipOutputStream, cacheDir, SETTINGS_FILE_NAME);
                    zipOutputStream.close();
                    outputStream.close();

                    // Clear the temporary files under the cache directory
                    FileUtils.deleteFileOrDir(cacheDir);

                    ToastUtils.showLong(R.string.action_finished);
                } catch (final Exception e) {
                    Log.e(TAG, "Failed to backup data", e);
                    ToastUtils.showLong(String.format(getString(R.string.action_failed), e.getMessage()));
                } finally {
                    progressDialog.cancel();
                }
            }
        }).start();
    }

    private void putNextEntry(ZipOutputStream zipOutputStream, File sourceDir, String fileName) throws IOException {
        zipOutputStream.putNextEntry(new ZipEntry(fileName));
        FileInputStream inputStream = new FileInputStream(new File(sourceDir, fileName));
        FileUtils.write(inputStream, zipOutputStream);
        inputStream.close();
    }

    private void restore(final Uri uri) {
        final Context context = getContext();
        final ProgressDialog progressDialog = new ProgressDialog(context);
        progressDialog.setMessage(getString(R.string.loading));
        progressDialog.setCancelable(false);
        progressDialog.show();

        new Thread(new Runnable() { // To not block the UI
            @Override
            public void run() {
                try {
                    // Unzip the backup file
                    InputStream inputStream = context.getContentResolver().openInputStream(uri);
                    ZipInputStream zipInputStream = new ZipInputStream(inputStream);
                    ZipEntry zipEntry;
                    boolean findDbEntry = false;
                    boolean findSettingsEntry = false;
                    while ((zipEntry = zipInputStream.getNextEntry()) != null) {
                        String entryName = zipEntry.getName();

                        if (DB_FILE_NAME.equals(entryName)) { // Restore the database
                            findDbEntry = true;
                            File databaseFile = context.getDatabasePath(Constants.DATABASE_NAME);
                            FileUtils.deleteFileOrDir(databaseFile);
                            FileOutputStream outputStream = new FileOutputStream(databaseFile);
                            FileUtils.write(zipInputStream, outputStream);
                            outputStream.close();
                        } else if (SETTINGS_FILE_NAME.equals(entryName)) { // Restore the app settings
                            findSettingsEntry = true;
                            JSONObject jsonObject = new JSONObject(FileUtils.getString(zipInputStream));
                            Iterator<String> keys = jsonObject.keys();
                            while (keys.hasNext()) {
                                String key = keys.next();
                                Object value = jsonObject.get(key);
                                if (PrefUtils.BACKUP_PATH.equals(key)) // Skip restoring the backup path
                                    continue;
                                if (value instanceof String)
                                    PrefUtils.putString(key, (String) value);
                                else if (value instanceof Integer)
                                    PrefUtils.putInt(key, (Integer) value);
                                else if (value instanceof Long)
                                    PrefUtils.putLong(key, (Long) value);
                                else if (value instanceof Boolean)
                                    PrefUtils.putBoolean(key, (Boolean) value);
                                else
                                    Log.e(TAG, "Illegal pref: key=" + key + ", value=" + value);
                            }
                        } else
                            Log.e(TAG, "Illegal file: " + entryName);
                    }

                    zipInputStream.close();
                    inputStream.close();
                    if (!findDbEntry || !findSettingsEntry)
                        throw new IllegalStateException(getString(R.string.message_invalid_backup_file));

                    // Restart the app
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            new AlertDialog.Builder(getActivity())
                                    .setTitle(R.string.action_finished)
                                    .setMessage(R.string.question_restart)
                                    .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            android.os.Process.killProcess(android.os.Process.myPid());
                                        }
                                    }).setNegativeButton(android.R.string.no, null).show();
                        }
                    });
                } catch (final Exception e) {
                    Log.e(TAG, "Failed to restore data", e);
                    ToastUtils.showLong(String.format(getString(R.string.action_failed), e.getMessage()));
                } finally {
                    progressDialog.cancel();
                }
            }
        }).start();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, final Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK) {
            Uri uri = data.getData();
            if (requestCode == REQUEST_CODE_RESTORE) {
                restore(uri);
            } else if (requestCode == REQUEST_CODE_SELECT_BACKUP_PATH) {
                saveBackupPath(uri);
            } else if (requestCode == REQUEST_CODE_BACKUP_MISSING_BACKUP_PATH) {
                saveBackupPath(uri);
                backup();
            }
        }
    }

    private void saveBackupPath(Uri uri) {
        findPreference(PrefUtils.BACKUP_PATH).setSummary(uri.toString());
        PrefUtils.putString(PrefUtils.BACKUP_PATH, uri.toString());
        getContext().getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
    }

    private void clearOldImageCache(File imageCacheDir, String regex, final long keepDateBorderTime) {
        if (imageCacheDir.exists()) {
            final Pattern pattern = Pattern.compile(regex);
            File[] files = imageCacheDir.listFiles(new FilenameFilter() {
                @Override
                public boolean accept(File dir, String name) {
                    return pattern.matcher(name).matches() && new File(dir, name).lastModified() < keepDateBorderTime;
                }
            });
            if (files != null) {
                for (File file : files) {
                    file.delete();
                }
            }
        }
    }

}
