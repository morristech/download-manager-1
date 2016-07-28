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
import android.content.Intent;
import android.net.Uri;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import me.henrytao.downloadmanager.DownloadManager.Request;
import me.henrytao.downloadmanager.Info;
import rx.Observable;
import rx.subjects.BehaviorSubject;

/**
 * Created by henrytao on 7/27/16.
 */
public class DownloadBus {

  private static DownloadBus sDefault;

  public static DownloadBus getInstance(Context context) {
    if (sDefault == null) {
      synchronized (DownloadBus.class) {
        if (sDefault == null) {
          sDefault = new DownloadBus(context);
        }
      }
    }
    return sDefault;
  }

  private final Context mContext;

  private final DownloadDbHelper mDownloadDbHelper;

  private final Map<Long, BehaviorSubject<Info>> maps = new HashMap<>();

  private Uri mTempPath;

  public DownloadBus(Context context) {
    mContext = context.getApplicationContext();
    mDownloadDbHelper = DownloadDbHelper.create(mContext);
  }

  public void downloaded(long id, long contentLength) {
    if (mDownloadDbHelper.updateState(id, DownloadInfo.State.DOWNLOADED)) {
      get(id).onNext(new Info(Info.State.DOWNLOADED, contentLength, contentLength));
      delete(id);
    }
  }

  public void downloading(long id, long bytesRead, long contentLength) {
    get(id).onNext(new Info(Info.State.DOWNLOADING, bytesRead, contentLength));
  }

  public long enqueue(Request request) {
    request.validate();
    return enqueue(mDownloadDbHelper.insert(DownloadInfo.create(request, getTempPath(), UUID.randomUUID().toString())));
  }

  public void error(long id, Throwable throwable) {
    get(id).onNext(new Info(Info.State.ERROR, throwable));
  }

  public Intent getIntentService(long id) {
    Intent intent = new Intent(mContext, DownloadService.class);
    intent.putExtra(DownloadService.EXTRA_DOWNLOAD_ID, id);
    return intent;
  }

  public synchronized Info.State getState(long id) {
    BehaviorSubject<Info> subject = get(id);
    if (!subject.hasValue()) {
      DownloadInfo downloadInfo = mDownloadDbHelper.find(id);
      if (downloadInfo != null) {
        switch (downloadInfo.getState()) {
          case DOWNLOADED:
            subject.onNext(new Info(Info.State.DOWNLOADED, downloadInfo.getContentLength(), downloadInfo.getContentLength()));
            break;
          case PAUSED:
            subject.onNext(new Info(Info.State.PAUSED, 0, 0));
            break;
          default:
            subject.onNext(new Info(Info.State.QUEUEING, 0, 0));
            break;
        }
      } else {
        subject.onNext(new Info(Info.State.INVALID, 0, 0));
      }
    }
    return subject.getValue().state;
  }

  public void initialize() {
    List<DownloadInfo> downloadings = mDownloadDbHelper.findAllDownloading();
    for (DownloadInfo downloadInfo : downloadings) {
      enqueue(downloadInfo.getId());
    }
  }

  public void invalid(long id) {
    get(id).onNext(new Info(Info.State.INVALID, 0, 0));
  }

  public Observable<Info> observe(long id) {
    return Observable.just((Info) null)
        .map(info -> {
          getState(id);
          return info;
        })
        .mergeWith(get(id))
        .filter(info -> info != null)
        .flatMap(info -> {
          if (info.state == Info.State.ERROR) {
            return Observable.error(info.getThrowable());
          }
          return Observable.just(info);
        });
  }

  public void pause(long id) {
    if (mDownloadDbHelper.updateState(id, DownloadInfo.State.PAUSED)) {
      get(id).onNext(new Info(Info.State.PAUSED, 0, 0));
    }
  }

  public void resume(long id) {
    if (mDownloadDbHelper.updateState(id, DownloadInfo.State.DOWNLOADING)) {
      enqueue(id);
      get(id).onNext(new Info(Info.State.RESUMED, 0, 0));
    }
  }

  public void started(long id, long bytesRead, long contentLength) {
    get(id).onNext(new Info(Info.State.STARTED, bytesRead, contentLength));
  }

  private void delete(long id) {
    BehaviorSubject<Info> subject = get(id);
    subject.onCompleted();
    maps.remove(id);
  }

  private long enqueue(long id) {
    Intent intent = getIntentService(id);
    mContext.startService(intent);
    get(id).onNext(new Info(Info.State.QUEUEING, 0, 0));
    return id;
  }

  private BehaviorSubject<Info> get(long id) {
    if (!maps.containsKey(id)) {
      maps.put(id, BehaviorSubject.create());
    }
    return maps.get(id);
  }

  private Uri getTempPath() {
    return mTempPath != null ? mTempPath : Uri.fromFile(mContext.getCacheDir());
  }

  public void setTempPath(Uri tempPath) {
    mTempPath = tempPath;
  }
}