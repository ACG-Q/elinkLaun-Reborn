package io.github.reborn.einklauncher.ftpservice;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class FTPReceiver extends BroadcastReceiver {

  static final String TAG = FTPReceiver.class.getSimpleName();

  @Override
  public void onReceive(Context context, Intent intent) {
    Log.v(TAG, "Received: " + intent.getAction());

    try {
      if (intent.getAction().equals(HttpService.ACTION_START_HTTPSERVER)) {
        Intent serverService = new Intent(context, HttpService.class);
        if (intent.hasExtra("port")) {
          serverService.putExtra("port", intent.getIntExtra("port", HttpService.DEFAULT_PORT));
        }
        if (!HttpService.isRunning()) {
          context.startService(serverService);
        }
      } else if (intent.getAction().equals(HttpService.ACTION_STOP_HTTPSERVER)) {
        Intent serverService = new Intent(context, HttpService.class);
        context.stopService(serverService);
      }
    } catch (Exception e) {
      Log.e(TAG, "Failed to start/stop on intent " + e.getMessage());
    }
  }
}
