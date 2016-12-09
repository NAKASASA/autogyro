package com.zlsadesign.autogyro;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

class AutogyroNotification {

  private int NOTIFICATION_ID = 284;

  private Notification notification;
  private RemoteViews remote_view;

  private NotificationManager notification_manager;

  private Context context;

  private boolean visible = false;

  AutogyroNotification(Context context) {
    this.context = context;

    notification_manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

    Notification.Builder builder = new Notification.Builder(context)
        .setCategory(Notification.CATEGORY_SYSTEM)
        .setPriority(Notification.PRIORITY_LOW)
        .setSmallIcon(R.drawable.ic_notify);

    remote_view = new RemoteViews(context.getPackageName(), R.layout.view_notification);
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
      builder.setCustomContentView(remote_view);
    } else {
      builder.setContent(remote_view);
    }

    Intent intent = new Intent(AutogyroService.ACTION_ROTATE_LEFT);
    remote_view.setOnClickPendingIntent(R.id.rotate_left, PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT));

    intent = new Intent(AutogyroService.ACTION_ROTATE_RIGHT);
    remote_view.setOnClickPendingIntent(R.id.rotate_right, PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT));

    intent = new Intent(AutogyroService.ACTION_FLIP);
    remote_view.setOnClickPendingIntent(R.id.flip, PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT));

    intent = new Intent(context, MainActivity.class);
    remote_view.setOnClickPendingIntent(R.id.settings, PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT));

    notification = builder.build();

    EventBus.getDefault().register(this);

    checkButton("left");
    checkButton("right");
    checkButton("flip");

    show();
  }

  void destroy() {
    EventBus.getDefault().unregister(this);
    hide();
  }

  void show() {
    if(shouldShowNotification())
      notification_manager.notify(NOTIFICATION_ID, notification);
  }

  void hide() {
    notification_manager.cancel(NOTIFICATION_ID);
  }

  @Subscribe(threadMode = ThreadMode.MAIN)
  public void onNotificationChangeEvent(NotificationChangeEvent event) {
    Log.d("NotificationChangeEvent", "command " + event.getCommand() + ", button " + event.getButton());

    if(event.getCommand().equals(NotificationChangeEvent.COMMAND_SHOW)) {
      show();
    } else if(event.getCommand().equals(NotificationChangeEvent.COMMAND_HIDE)) {
      hide();

    } else if(event.getCommand().equals(NotificationChangeEvent.COMMAND_BUTTON_SHOW)) {
      showButton(event.getButton());
    } else if(event.getCommand().equals(NotificationChangeEvent.COMMAND_BUTTON_HIDE)) {
      hideButton(event.getButton());
    }
  }

  private void checkButton(String button) {
    if(shouldShowButton(button)) {
      showButton(button);
    } else {
      hideButton(button);
    }
  }

  private SharedPreferences getPrefs() {
    return PreferenceManager.getDefaultSharedPreferences(context);
  }

  private boolean shouldShowNotification() {
    return getPrefs().getBoolean("show_notification", false);
  }

  private boolean shouldShowButton(String button) {
    return getPrefs().getBoolean("show_notification_" + button, false);
  }

  public Notification getNotification() {
    return notification;
  }

  private int getIdFromButton(String button) {
    int id = R.id.rotate_left;

    switch (button) {
      case "left":
        id = R.id.rotate_left;
        break;
      case "right":
        id = R.id.rotate_right;
        break;
      case "flip":
        id = R.id.flip;
        break;
    }

    return id;
  }

  private void setButtonVisibility(String button, int visibility) {
    remote_view.setViewVisibility(getIdFromButton(button), visibility);

    if(!shouldShowButton("left") && !shouldShowButton("right") && !shouldShowButton("flip")) {
      hide();
    } else {
      show();
    }
  }

  private void showButton(String button) {
    setButtonVisibility(button, View.VISIBLE);
  }

  private void hideButton(String button) {
    setButtonVisibility(button, View.GONE);
  }

}
