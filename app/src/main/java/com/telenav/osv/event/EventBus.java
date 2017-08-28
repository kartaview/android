package com.telenav.osv.event;

import com.telenav.osv.utils.Log;
import org.greenrobot.eventbus.EventBusBuilder;

/**
 * Eventbus wrapper
 * Created by Kalman on 07/11/2016.
 */
public class EventBus {

  private static final String TAG = "EventBus";

  private static final org.greenrobot.eventbus.EventBus mBus = getEventBus();

  private static org.greenrobot.eventbus.EventBus getEventBus() {
    EventBusBuilder builder = org.greenrobot.eventbus.EventBus.builder();
    builder.installDefaultEventBus();
    builder.logNoSubscriberMessages(false);
    return builder.build();
  }

  public static void register(Object subscriber) {
    if (subscriber != null) {
      Log.d(TAG, "register: " + subscriber.getClass().getSimpleName());
      try {
        mBus.register(subscriber);
      } catch (Exception ignored) {
      }
    }
  }

  public static void unregister(Object subscriber) {
    if (subscriber != null) {
      Log.d(TAG, "unregister: " + subscriber.getClass().getSimpleName());
      try {
        mBus.unregister(subscriber);
      } catch (Exception ignored) {
      }
    }
  }

  public static void post(OSVEvent event) {
    if (event != null) {
      mBus.post(event);
    }
  }

  public static void postSticky(OSVStickyEvent event) {
    if (event != null) {
      mBus.postSticky(event.getStickyClass(), event);
    }
  }

  public static <T> void clear(Class<T> type) {
    if (type != null) {
      mBus.removeStickyEvent(type);
    }
  }
}
