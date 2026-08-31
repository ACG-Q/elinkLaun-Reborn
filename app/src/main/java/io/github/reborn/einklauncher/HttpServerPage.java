package io.github.reborn.einklauncher;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;

import io.github.reborn.einklauncher.ftpservice.HttpService;

public class HttpServerPage extends Activity {

  private TextView tvStatus;
  private TextView tvAddress;
  private EditText etPort;
  private Button btnToggle;
  private Button btnOpenBrowser;
  private Button btnCopy;

  private final android.content.BroadcastReceiver receiver = new android.content.BroadcastReceiver() {
    @Override
    public void onReceive(Context context, Intent intent) {
      String action = intent.getAction();
      if (HttpService.ACTION_STARTED.equals(action)) {
        updateStatus(true);
      } else if (HttpService.ACTION_STOPPED.equals(action)) {
        updateStatus(false);
      } else if (HttpService.ACTION_FAILEDTOSTART.equals(action)) {
        updateStatus(false);
        tvStatus.setText("Failed to start");
      }
    }
  };

  @Override
  protected void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    initViews();
  }

  private void initViews() {
    LinearLayout root = new LinearLayout(this);
    root.setOrientation(LinearLayout.VERTICAL);
    root.setBackgroundColor(android.graphics.Color.WHITE);

    int pad = Utils.dp2Px(this, 14);
    root.setPadding(pad, pad, pad, pad);

    // Title
    TextView tvTitle = new TextView(this);
    tvTitle.setText("HTTP File Server");
    tvTitle.setTextSize(22);
    tvTitle.setTypeface(null, android.graphics.Typeface.BOLD);
    tvTitle.setTextColor(android.graphics.Color.BLACK);
    root.addView(tvTitle);

    View divider = makeDivider();
    root.addView(divider, new ViewGroup.LayoutParams(-1, 1));

    // Status
    tvStatus = new TextView(this);
    tvStatus.setText("Status: Stopped");
    tvStatus.setTextSize(16);
    tvStatus.setTextColor(android.graphics.Color.BLACK);
    root.addView(tvStatus);

    // Address
    tvAddress = new TextView(this);
    tvAddress.setText("Address: (none)");
    tvAddress.setTextSize(14);
    tvAddress.setTextColor(android.graphics.Color.DKGRAY);
    root.addView(tvAddress);

    root.addView(makeDivider());

    // Port input
    LinearLayout portRow = new LinearLayout(this);
    portRow.setOrientation(LinearLayout.HORIZONTAL);
    portRow.setGravity(Gravity.CENTER_VERTICAL);

    TextView tvPortLabel = new TextView(this);
    tvPortLabel.setText("Port: ");
    tvPortLabel.setTextSize(16);
    tvPortLabel.setTextColor(android.graphics.Color.BLACK);
    portRow.addView(tvPortLabel);

    etPort = new EditText(this);
    etPort.setText(String.valueOf(HttpService.getDefaultPortFromPreferences(
        PreferenceManager.getDefaultSharedPreferences(this))));
    etPort.setInputType(InputType.TYPE_CLASS_NUMBER);
    etPort.setTextSize(16);
    etPort.setWidth(Utils.dp2Px(this, 100));
    portRow.addView(etPort);

    root.addView(portRow);

    // Toggle button
    btnToggle = new Button(this);
    btnToggle.setText("Start Server");
    btnToggle.setTextSize(16);
    root.addView(btnToggle, new LinearLayout.LayoutParams(-1, Utils.dp2Px(this, 48)));

    root.addView(makeDivider());

    // Action buttons row
    LinearLayout btnRow = new LinearLayout(this);
    btnRow.setOrientation(LinearLayout.HORIZONTAL);

    btnOpenBrowser = new Button(this);
    btnOpenBrowser.setText("Open in Browser");
    btnOpenBrowser.setEnabled(false);
    btnOpenBrowser.setTextSize(14);
    LinearLayout.LayoutParams lp1 = new LinearLayout.LayoutParams(0, Utils.dp2Px(this, 44), 1);
    lp1.rightMargin = pad / 2;
    btnRow.addView(btnOpenBrowser, lp1);

    btnCopy = new Button(this);
    btnCopy.setText("Copy Address");
    btnCopy.setEnabled(false);
    btnCopy.setTextSize(14);
    LinearLayout.LayoutParams lp2 = new LinearLayout.LayoutParams(0, Utils.dp2Px(this, 44), 1);
    lp2.leftMargin = pad / 2;
    btnRow.addView(btnCopy, lp2);

    root.addView(btnRow);

    // Spacer
    View spacer = new View(this);
    root.addView(spacer, new LinearLayout.LayoutParams(-1, 0, 1));

    // Back button
    Button btnBack = new Button(this);
    btnBack.setText("Back to Launcher");
    btnBack.setTextSize(16);
    root.addView(btnBack, new LinearLayout.LayoutParams(-1, Utils.dp2Px(this, 44)));

    setContentView(root);

    // Click handlers
    btnToggle.setOnClickListener(v -> toggleServer());
    btnOpenBrowser.setOnClickListener(v -> openInBrowser());
    btnCopy.setOnClickListener(v -> copyAddress());
    btnBack.setOnClickListener(v -> finish());

    updateStatus(HttpService.isRunning());
  }

  private View makeDivider() {
    View d = new View(this);
    d.setBackgroundColor(android.graphics.Color.BLACK);
    LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, 1);
    lp.topMargin = Utils.dp2Px(this, 8);
    lp.bottomMargin = Utils.dp2Px(this, 8);
    d.setLayoutParams(lp);
    return d;
  }

  private void toggleServer() {
    if (HttpService.isRunning()) {
      stopServer();
    } else {
      startServer();
    }
  }

  private void startServer() {
    if (!HttpService.isConnectedToWifi(this)) {
      Toast.makeText(this, "Please connect to WiFi first", Toast.LENGTH_SHORT).show();
      return;
    }

    int port;
    try {
      port = Integer.parseInt(etPort.getText().toString().trim());
      if (port <= 0 || port > 65535) {
        etPort.setError("Port must be 1-65535");
        return;
      }
    } catch (NumberFormatException e) {
      etPort.setError("Invalid port");
      return;
    }

    if (!HttpService.isPortAvailable(port)) {
      Toast.makeText(this, "Port " + port + " is already in use", Toast.LENGTH_LONG).show();
      return;
    }

    HttpService.changePort(
        PreferenceManager.getDefaultSharedPreferences(this), port);
    Intent startIntent = new Intent(this, HttpService.class);
    startIntent.putExtra("port", port);
    startService(startIntent);
  }

  private void stopServer() {
    Intent stopIntent = new Intent(this, HttpService.class);
    stopService(stopIntent);
  }

  private void updateStatus(boolean running) {
    if (running) {
      tvStatus.setText("Status: Running");
      btnToggle.setText("Stop Server");
      btnOpenBrowser.setEnabled(true);
      btnCopy.setEnabled(true);
      String addr = getAddressString();
      tvAddress.setText("Address: " + (addr != null ? addr : "(none)"));
    } else {
      tvStatus.setText("Status: Stopped");
      btnToggle.setText("Start Server");
      btnOpenBrowser.setEnabled(false);
      btnCopy.setEnabled(false);
      tvAddress.setText("Address: (none)");
    }
  }

  private String getAddressString() {
    java.net.InetAddress addr = HttpService.getLocalInetAddress(this);
    if (addr == null) return null;
    return "http://" + addr.getHostAddress() + ":" + HttpService.getPort();
  }

  private void openInBrowser() {
    String addr = getAddressString();
    if (addr == null) {
      Toast.makeText(this, "No address available", Toast.LENGTH_SHORT).show();
      return;
    }
    Intent intent = new Intent(Intent.ACTION_VIEW, android.net.Uri.parse(addr));
    startActivity(intent);
  }

  private void copyAddress() {
    String addr = getAddressString();
    if (addr == null) {
      Toast.makeText(this, "No address to copy", Toast.LENGTH_SHORT).show();
      return;
    }
    ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
    cm.setPrimaryClip(ClipData.newPlainText("HTTP Server Address", addr));
    Toast.makeText(this, "Address copied", Toast.LENGTH_SHORT).show();
  }

  @Override
  protected void onResume() {
    super.onResume();
    IntentFilter filter = new IntentFilter();
    filter.addAction(HttpService.ACTION_STARTED);
    filter.addAction(HttpService.ACTION_STOPPED);
    filter.addAction(HttpService.ACTION_FAILEDTOSTART);
    registerReceiver(receiver, filter);
    updateStatus(HttpService.isRunning());
  }

  @Override
  protected void onPause() {
    super.onPause();
    try {
      unregisterReceiver(receiver);
    } catch (IllegalArgumentException ignored) {
    }
  }
}
