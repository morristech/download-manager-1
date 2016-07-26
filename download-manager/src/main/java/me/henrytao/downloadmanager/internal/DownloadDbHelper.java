/*
 * Copyright 2016 "Henry Tao <hi@henrytao.me>"
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package me.henrytao.downloadmanager.internal;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import me.henrytao.downloadmanager.data.DownloadInfo;

/**
 * Created by henrytao on 7/26/16.
 */
public class DownloadDbHelper extends SQLiteOpenHelper {

  public static final String DATABASE_NAME = "download-manager.db";

  public static final int DATABASE_VERSION = 1;

  private static final String C_COMMA = " , ";

  private static final String C_TEXT = " TEXT ";

  public static DownloadDbHelper create(Context context) {
    return new DownloadDbHelper(context);
  }

  protected DownloadDbHelper(Context context) {
    super(context.getApplicationContext(), DATABASE_NAME, null, DATABASE_VERSION);
  }

  @Override
  public void onCreate(SQLiteDatabase db) {
    createDownloadInfoTable(db);
  }

  @Override
  public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    // do nothing for now
  }

  public DownloadInfo find(long downloadId) {
    SQLiteDatabase db = getReadableDatabase();
    DownloadInfo downloadInfo = null;
    Cursor cursor = db.query(
        DownloadInfo.TABLE_NAME,
        new String[]{DownloadInfo.Fields._ID, DownloadInfo.Fields.URL, DownloadInfo.Fields.DEST_PATH, DownloadInfo.Fields.DEST_TITLE},
        DownloadInfo.Fields._ID + " = ?",
        new String[]{String.valueOf(downloadId)},
        null,
        null,
        null);
    if (cursor.moveToFirst()) {
      downloadInfo = DownloadInfo.create(cursor);
    }
    if (!cursor.isClosed()) {
      cursor.close();
    }
    db.close();
    return downloadInfo;
  }

  public long insert(DownloadInfo downloadInfo) {
    SQLiteDatabase db = getWritableDatabase();
    long id = db.insert(DownloadInfo.TABLE_NAME, null, downloadInfo.toContentValues());
    db.close();
    return id;
  }

  private void createDownloadInfoTable(SQLiteDatabase db) {
    db.execSQL("CREATE TABLE IF NOT EXISTS " + DownloadInfo.TABLE_NAME + " ( "
        + DownloadInfo.Fields._ID + " INTEGER PRIMARY KEY AUTOINCREMENT" + C_COMMA
        + DownloadInfo.Fields.URL + C_TEXT + C_COMMA
        + DownloadInfo.Fields.DEST_PATH + C_TEXT + C_COMMA
        + DownloadInfo.Fields.DEST_TITLE + C_TEXT
        + " )");
  }
}
