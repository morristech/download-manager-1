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

import rx.Subscriber;
import rx.Subscription;

/**
 * Created by henrytao on 4/15/16.
 */
class SubscriptionUtils {

  public static <T> void onComplete(Subscriber<T> subscriber) {
    if (subscriber != null && !subscriber.isUnsubscribed()) {
      subscriber.onCompleted();
    }
  }

  public static <T> void onError(Subscriber<T> subscriber, Throwable throwable) {
    if (subscriber != null && !subscriber.isUnsubscribed()) {
      subscriber.onError(throwable);
    }
  }

  public static <T> void onNext(Subscriber<T> subscriber, T data) {
    if (subscriber != null && !subscriber.isUnsubscribed()) {
      subscriber.onNext(data);
    }
  }

  public static <T> void onNext(Subscriber<T> subscriber) {
    onNext(subscriber, null);
  }

  public static <T> void onNextAndComplete(Subscriber<T> subscriber, T data) {
    onNext(subscriber, data);
    onComplete(subscriber);
  }

  public static <T> void onNextAndComplete(Subscriber<T> subscriber) {
    onNextAndComplete(subscriber, null);
  }

  public static void unsubscribe(Subscription subscription) {
    if (subscription != null && !subscription.isUnsubscribed()) {
      subscription.unsubscribe();
    }
  }
}
