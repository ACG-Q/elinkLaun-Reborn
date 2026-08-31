package io.github.reborn.einklauncher.ftpservice;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.os.Environment;
import android.os.IBinder;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.URLDecoder;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 轻量级 HTTP 文件服务器，替代 FTP，用于局域网内浏览器访问设备文件。
 * 纯 Java 实现，无第三方依赖。
 */
public class HttpService extends Service {

  private static final String TAG = "HttpService";
  public static final int DEFAULT_PORT = 2333;
  public static final String PORT_PREFERENCE_KEY = "ftpPort";

  public static final String ACTION_STARTED = "io.github.reborn.einklauncher.ftpservice.HttpService.HTTPSERVER_STARTED";
  public static final String ACTION_STOPPED = "io.github.reborn.einklauncher.ftpservice.HttpService.HTTPSERVER_STOPPED";
  public static final String ACTION_FAILEDTOSTART = "io.github.reborn.einklauncher.ftpservice.HttpService.HTTPSERVER_FAILEDTOSTART";
  public static final String ACTION_START_HTTPSERVER = "io.github.reborn.einklauncher.ftpservice.HttpService.ACTION_START_HTTPSERVER";
  public static final String ACTION_STOP_HTTPSERVER = "io.github.reborn.einklauncher.ftpservice.HttpService.ACTION_STOP_HTTPSERVER";

  private static int port = DEFAULT_PORT;
  private int pendingPort = -1;
  private ServerSocket serverSocket;
  private ExecutorService threadPool;
  private volatile boolean running = false;

  public static int getDefaultPortFromPreferences(SharedPreferences prefs) {
    try {
      return prefs.getInt(PORT_PREFERENCE_KEY, DEFAULT_PORT);
    } catch (ClassCastException ex) {
      changePort(prefs, DEFAULT_PORT);
      return DEFAULT_PORT;
    }
  }

  public static void changePort(SharedPreferences prefs, int port) {
    prefs.edit().putInt(PORT_PREFERENCE_KEY, port).commit();
  }

  @Override
  public int onStartCommand(Intent intent, int flags, int startId) {
    if (running) {
      return START_STICKY;
    }
    if (intent != null && intent.hasExtra("port")) {
      pendingPort = intent.getIntExtra("port", DEFAULT_PORT);
    }
    new Thread(this::startServer).start();
    return START_STICKY;
  }

  @Override
  public IBinder onBind(Intent intent) {
    return null;
  }

  private void startServer() {
    try {
      if (pendingPort > 0) {
        port = pendingPort;
        pendingPort = -1;
      } else {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        port = getDefaultPortFromPreferences(prefs);
      }
      serverSocket = new ServerSocket(port);
      serverSocket.setReuseAddress(true);
      threadPool = Executors.newCachedThreadPool();
      running = true;
      sendBroadcast(new Intent(ACTION_STARTED));
      Log.i(TAG, "HTTP server started on port " + port);

      while (running) {
        try {
          Socket client = serverSocket.accept();
          threadPool.execute(() -> handleClient(client));
        } catch (IOException e) {
          if (running) {
            Log.e(TAG, "Accept error", e);
          }
        }
      }
    } catch (IOException e) {
      Log.e(TAG, "Failed to start server", e);
      sendBroadcast(new Intent(ACTION_FAILEDTOSTART));
    }
  }

  private void handleClient(Socket client) {
    try {
      client.setSoTimeout(30000);
      InputStream is = client.getInputStream();
      OutputStream os = client.getOutputStream();

      BufferedReader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
      String requestLine = reader.readLine();
      if (requestLine == null || requestLine.isEmpty()) {
        client.close();
        return;
      }

      String[] parts = requestLine.split(" ");
      if (parts.length < 2) {
        client.close();
        return;
      }

      String method = parts[0];
      String uri = parts[1];

      String path;
      int qIdx = uri.indexOf('?');
      if (qIdx >= 0) {
        path = URLDecoder.decode(uri.substring(0, qIdx), "UTF-8");
      } else {
        path = URLDecoder.decode(uri, "UTF-8");
      }

      String contentLengthStr = null;
      String contentType = null;
      String line;
      while ((line = reader.readLine()) != null && !line.isEmpty()) {
        String lower = line.toLowerCase();
        if (lower.startsWith("content-length:")) {
          contentLengthStr = line.substring(15).trim();
        } else if (lower.startsWith("content-type:")) {
          contentType = line.substring(13).trim();
        }
      }

      if ("/".equals(path)) {
        path = "/";
      }

      File rootDir = Environment.getExternalStorageDirectory();

      if ("GET".equals(method)) {
        handleGet(path, uri, rootDir, os);
      } else if ("POST".equals(method)) {
        handlePost(path, rootDir, is, contentLengthStr, contentType, os);
      } else if ("DELETE".equals(method)) {
        handleDelete(path, rootDir, os);
      } else {
        sendResponse(os, 405, "Method Not Allowed", "text/plain", "405 Method Not Allowed");
      }

      client.close();
    } catch (Exception e) {
      Log.e(TAG, "Handle client error", e);
      try { client.close(); } catch (IOException ignored) {}
    }
  }

  // ===== Shared CSS & Components =====

  private static final String CSS = "*{margin:0;padding:0;box-sizing:border-box;}"
      + "body{font-family:system-ui,-apple-system,sans-serif;background:#f0f0f0;color:#1a1a1a;"
      + "min-height:100dvh;padding-bottom:64px;-webkit-tap-highlight-color:transparent;}"
      + "a{color:#1a73e8;text-decoration:none;}"
      + ".topbar{position:sticky;top:0;z-index:100;background:#fff;border-bottom:1px solid #e0e0e0;"
      + "padding:12px 16px;display:flex;align-items:center;gap:12px;}"
      + ".topbar h1{font-size:18px;font-weight:700;flex:1;}"
      + ".topbar .back{width:40px;height:40px;display:flex;align-items:center;justify-content:center;"
      + "border:none;background:none;cursor:pointer;border-radius:8px;flex-shrink:0;}"
      + ".topbar .back:active{background:#f0f0f0;}"
      + ".nav{position:fixed;bottom:0;left:0;right:0;z-index:100;background:#fff;"
      + "border-top:1px solid #e0e0e0;display:flex;height:56px;}"
      + ".nav a{flex:1;display:flex;flex-direction:column;align-items:center;justify-content:center;"
      + "gap:2px;font-size:10px;color:#666;text-decoration:none;transition:color .15s;}"
      + ".nav a.active{color:#1a73e8;font-weight:600;}"
      + ".nav svg{width:22px;height:22px;}"
      + ".content{padding:16px;max-width:600px;margin:0 auto;}"
      + ".card{background:#fff;border-radius:12px;border:1px solid #e0e0e0;overflow:hidden;}"
      + ".card+.card{margin-top:12px;}"
      + ".card-header{padding:16px;font-weight:600;font-size:15px;border-bottom:1px solid #f0f0f0;}"
      + ".btn{display:inline-flex;align-items:center;justify-content:center;gap:8px;"
      + "padding:10px 20px;border-radius:8px;border:1px solid #e0e0e0;background:#fff;"
      + "font-size:14px;font-weight:500;cursor:pointer;transition:all .15s;min-height:44px;}"
      + ".btn:active{transform:scale(0.97);}"
      + ".btn-primary{background:#1a73e8;color:#fff;border-color:#1a73e8;}"
      + ".btn-primary:active{background:#1557b0;}"
      + ".btn-danger{color:#d93025;border-color:#d93025;}"
      + ".btn-danger:active{background:#d93025;color:#fff;}"
      + ".btn-sm{padding:6px 12px;font-size:12px;min-height:32px;border-radius:6px;}"
      + ".row{padding:12px 16px;display:flex;align-items:center;gap:12px;min-height:48px;}"
      + ".row+.row{border-top:1px solid #f0f0f0;}"
      + ".row:active{background:#f8f9fa;}"
      + ".row-icon{width:20px;height:20px;color:#666;flex-shrink:0;}"
      + ".row-text{flex:1;min-width:0;}"
      + ".row-title{font-size:14px;font-weight:500;word-break:break-all;}"
      + ".row-sub{font-size:12px;color:#888;margin-top:2px;}"
      + ".row-action{flex-shrink:0;color:#888;}"
      + ".badge{display:inline-block;padding:2px 8px;border-radius:10px;font-size:11px;font-weight:600;}"
      + ".badge-blue{background:#e8f0fe;color:#1a73e8;}"
      + ".badge-green{background:#e6f4ea;color:#1e8e3e;}"
      + ".badge-red{background:#fce8e6;color:#d93025;}"
      + ".empty{text-align:center;padding:48px 16px;color:#888;}"
      + ".empty svg{width:48px;height:48px;margin-bottom:12px;opacity:0.4;}"
      + ".empty p{font-size:14px;margin-top:8px;}"
      + ".grid{display:grid;grid-template-columns:repeat(2,1fr);gap:12px;}"
      + ".grid-card{background:#fff;border-radius:12px;border:1px solid #e0e0e0;padding:20px 16px;"
      + "text-align:center;text-decoration:none;color:#1a1a1a;display:flex;flex-direction:column;"
      + "align-items:center;gap:8px;transition:all .15s;min-height:100px;}"
      + ".grid-card:active{background:#f8f9fa;transform:scale(0.97);}"
      + ".grid-card svg{width:28px;height:28px;color:#1a73e8;}"
      + ".grid-card .label{font-size:13px;font-weight:500;}"
      + ".grid-card .desc{font-size:11px;color:#888;}"
      + ".upload-zone{border:2px dashed #d0d0d0;border-radius:12px;padding:24px;text-align:center;"
      + "cursor:pointer;transition:all .15s;margin-top:12px;}"
      + ".upload-zone:active{border-color:#1a73e8;background:#f8f9fa;}"
      + ".status-bar{display:flex;gap:8px;flex-wrap:wrap;margin-bottom:12px;}"
      + ".stat{flex:1;min-width:120px;background:#fff;border-radius:10px;border:1px solid #e0e0e0;"
      + "padding:12px;text-align:center;}"
      + ".stat .val{font-size:20px;font-weight:700;color:#1a73e8;}"
      + ".stat .lbl{font-size:11px;color:#888;margin-top:2px;}"
      + "table{width:100%;border-collapse:collapse;}"
      + "th,td{padding:10px 12px;text-align:left;font-size:13px;}"
      + "th{background:#fafafa;font-weight:600;font-size:11px;color:#888;text-transform:uppercase;"
      + "letter-spacing:0.5px;border-bottom:1px solid #e0e0e0;}"
      + "td{border-bottom:1px solid #f0f0f0;}"
      + "tr:active{background:#f8f9fa;}"
      + "input[type=text],input[type=number]{width:100%;padding:10px 12px;border:1px solid #e0e0e0;"
      + "border-radius:8px;font-size:14px;min-height:44px;background:#fff;}"
      + "input:focus{outline:none;border-color:#1a73e8;box-shadow:0 0 0 3px rgba(26,115,232,0.15);}"
      + ".toast{position:fixed;bottom:72px;left:50%;transform:translateX(-50%);background:#1a1a1a;"
      + "color:#fff;padding:10px 20px;border-radius:8px;font-size:13px;z-index:200;"
      + "opacity:0;transition:opacity .2s;pointer-events:none;}"
      + ".toast.show{opacity:1;}"
      + ".icon-grid{display:grid;grid-template-columns:repeat(3,1fr);gap:12px;}"
      + ".icon-item{background:#fff;border-radius:12px;border:2px solid #e0e0e0;padding:16px 8px;"
      + "text-align:center;cursor:pointer;transition:all .15s;}"
      + ".icon-item:active{border-color:#1a73e8;transform:scale(0.97);}"
      + ".icon-item.selected{border-color:#1a73e8;background:#e8f0fe;}"
      + ".icon-item img{width:48px;height:48px;border-radius:8px;margin-bottom:6px;}"
      + ".icon-item .name{font-size:11px;color:#666;word-break:break-all;line-height:1.2;}"
      + "@media(max-width:380px){.grid{grid-template-columns:1fr;}.icon-grid{grid-template-columns:repeat(2,1fr);}}";

  private String navBar(String active) {
    String fm = "/fm".equals(active) ? "active" : "";
    String apk = "/apk".equals(active) ? "active" : "";
    String icons = "/icons".equals(active) ? "active" : "";
    String settings = "/settings".equals(active) ? "active" : "";
    return "<nav class=\"nav\">"
        + "<a href=\"/\" class=\"" + ("".equals(active) ? "active" : "") + "\">"
        + "<svg viewBox=\"0 0 24 24\" fill=\"none\" stroke=\"currentColor\" stroke-width=\"2\"><path d=\"M3 12l2-2m0 0l7-7 7 7M5 10v10a1 1 0 001 1h3m10-11l2 2m-2-2v10a1 1 0 01-1 1h-3m-4 0a1 1 0 01-1-1v-4a1 1 0 011-1h2a1 1 0 011 1v4a1 1 0 01-1 1h-2z\"/></svg>"
        + "Home</a>"
        + "<a href=\"/fm\" class=\"" + fm + "\">"
        + "<svg viewBox=\"0 0 24 24\" fill=\"none\" stroke=\"currentColor\" stroke-width=\"2\"><path d=\"M3 7v10a2 2 0 002 2h14a2 2 0 002-2V9a2 2 0 00-2-2h-6l-2-2H5a2 2 0 00-2 2z\"/></svg>"
        + "Files</a>"
        + "<a href=\"/apk\" class=\"" + apk + "\">"
        + "<svg viewBox=\"0 0 24 24\" fill=\"none\" stroke=\"currentColor\" stroke-width=\"2\"><path d=\"M12 18h.01M8 21h8a2 2 0 002-2V5a2 2 0 00-2-2H8a2 2 0 00-2 2v14a2 2 0 002 2z\"/></svg>"
        + "Apps</a>"
        + "<a href=\"/icons\" class=\"" + icons + "\">"
        + "<svg viewBox=\"0 0 24 24\" fill=\"none\" stroke=\"currentColor\" stroke-width=\"2\"><path d=\"M4 16l4.586-4.586a2 2 0 012.828 0L16 16m-2-2l1.586-1.586a2 2 0 012.828 0L20 14m-6-6h.01M6 20h12a2 2 0 002-2V6a2 2 0 00-2-2H6a2 2 0 00-2 2v12a2 2 0 002 2z\"/></svg>"
        + "Icons</a>"
        + "<a href=\"/settings\" class=\"" + settings + "\">"
        + "<svg viewBox=\"0 0 24 24\" fill=\"none\" stroke=\"currentColor\" stroke-width=\"2\"><path d=\"M10.325 4.317c.426-1.756 2.924-1.756 3.35 0a1.724 1.724 0 002.573 1.066c1.543-.94 3.31.826 2.37 2.37a1.724 1.724 0 001.066 2.573c1.756.426 1.756 2.924 0 3.35a1.724 1.724 0 00-1.066 2.573c.94 1.543-.826 3.31-2.37 2.37a1.724 1.724 0 00-2.573 1.066c-.426 1.756-2.924 1.756-3.35 0a1.724 1.724 0 00-2.573-1.066c-1.543.94-3.31-.826-2.37-2.37a1.724 1.724 0 00-1.066-2.573c-1.756-.426-1.756-2.924 0-3.35a1.724 1.724 0 001.066-2.573c-.94-1.543.826-3.31 2.37-2.37.996.608 2.296.07 2.573-1.066z\"/><circle cx=\"12\" cy=\"12\" r=\"3\"/></svg>"
        + "Settings</a>"
        + "</nav>";
  }

  private String toastHtml() {
    return "<div class=\"toast\" id=\"toast\"></div>"
        + "<script>function showToast(m){var t=document.getElementById('toast');t.textContent=m;"
        + "t.classList.add('show');setTimeout(function(){t.classList.remove('show')},3000);}</script>";
  }

  // ===== GET =====

  private void handleGet(String path, String fullUri, File rootDir, OutputStream os) throws IOException {
    if ("/".equals(path)) {
      sendHomePage(rootDir, os);
      return;
    }
    if ("/fm".equals(path)) {
      String dirPath = extractQueryParam(fullUri, "path");
      sendFileManagerPage(dirPath, rootDir, os);
      return;
    }
    if ("/apk".equals(path)) {
      sendApkManagerPage(rootDir, os);
      return;
    }
    if ("/settings".equals(path)) {
      sendSystemSettingsPage(os);
      return;
    }
    if ("/icons".equals(path)) {
      sendIconManagerPage(os);
      return;
    }

    File file = new File(rootDir, path);

    if (!file.exists()) {
      sendResponse(os, 404, "Not Found", "text/html", buildErrorPage(404, "File Not Found"));
      return;
    }

    if (file.isDirectory()) {
      String redirectPath = file.getAbsolutePath();
      String resp = "<!DOCTYPE html><html><head><meta charset=\"UTF-8\">"
          + "<meta http-equiv=\"refresh\" content=\"0;url=/fm?path=" + escapeHtml(redirectPath) + "\">"
          + "</head><body></body></html>";
      sendResponse(os, 302, "Redirect", "text/html; charset=UTF-8", resp);
    } else {
      sendFile(file, os);
    }
  }

  // ===== Home Page =====

  private void sendHomePage(File rootDir, OutputStream os) throws IOException {
    File[] rootFiles = rootDir.listFiles();
    int fileCount = 0;
    long totalSize = 0;
    if (rootFiles != null) {
      for (File f : rootFiles) {
        if (!f.getName().startsWith(".")) {
          fileCount++;
          if (f.isFile()) totalSize += f.length();
        }
      }
    }
    StringBuilder sb = new StringBuilder();
    sb.append("<!DOCTYPE html><html><head><meta charset=\"UTF-8\">");
    sb.append("<meta name=\"viewport\" content=\"width=device-width,initial-scale=1\">");
    sb.append("<title>E-Ink Manager</title><style>").append(CSS).append("</style></head><body>");
    sb.append("<div class=\"topbar\"><h1>E-Ink Manager</h1></div>");
    sb.append("<div class=\"content\">");
    sb.append("<div class=\"status-bar\">");
    sb.append("<div class=\"stat\"><div class=\"val\">").append(fileCount).append("</div><div class=\"lbl\">Files</div></div>");
    sb.append("<div class=\"stat\"><div class=\"val\">").append(formatSize(totalSize)).append("</div><div class=\"lbl\">Total Size</div></div>");
    sb.append("</div>");
    sb.append("<div class=\"grid\">");
    sb.append("<a href=\"/fm\" class=\"grid-card\">");
    sb.append("<svg viewBox=\"0 0 24 24\" fill=\"none\" stroke=\"currentColor\" stroke-width=\"2\"><path d=\"M3 7v10a2 2 0 002 2h14a2 2 0 002-2V9a2 2 0 00-2-2h-6l-2-2H5a2 2 0 00-2 2z\"/></svg>");
    sb.append("<div class=\"label\">File Manager</div><div class=\"desc\">Browse & manage files</div></a>");
    sb.append("<a href=\"/apk\" class=\"grid-card\">");
    sb.append("<svg viewBox=\"0 0 24 24\" fill=\"none\" stroke=\"currentColor\" stroke-width=\"2\"><path d=\"M12 18h.01M8 21h8a2 2 0 002-2V5a2 2 0 00-2-2H8a2 2 0 00-2 2v14a2 2 0 002 2z\"/></svg>");
    sb.append("<div class=\"label\">APK Manager</div><div class=\"desc\">Install apps</div></a>");
    sb.append("<a href=\"/icons\" class=\"grid-card\">");
    sb.append("<svg viewBox=\"0 0 24 24\" fill=\"none\" stroke=\"currentColor\" stroke-width=\"2\"><path d=\"M4 16l4.586-4.586a2 2 0 012.828 0L16 16m-2-2l1.586-1.586a2 2 0 012.828 0L20 14m-6-6h.01M6 20h12a2 2 0 002-2V6a2 2 0 00-2-2H6a2 2 0 00-2 2v12a2 2 0 002 2z\"/></svg>");
    sb.append("<div class=\"label\">Icon Manager</div><div class=\"desc\">Customize icons</div></a>");
    sb.append("<a href=\"/settings\" class=\"grid-card\">");
    sb.append("<svg viewBox=\"0 0 24 24\" fill=\"none\" stroke=\"currentColor\" stroke-width=\"2\"><path d=\"M10.325 4.317c.426-1.756 2.924-1.756 3.35 0a1.724 1.724 0 002.573 1.066c1.543-.94 3.31.826 2.37 2.37a1.724 1.724 0 001.066 2.573c1.756.426 1.756 2.924 0 3.35a1.724 1.724 0 00-1.066 2.573c.94 1.543-.826 3.31-2.37 2.37a1.724 1.724 0 00-2.573 1.066c-.426 1.756-2.924 1.756-3.35 0a1.724 1.724 0 00-2.573-1.066c-1.543.94-3.31-.826-2.37-2.37a1.724 1.724 0 00-1.066-2.573c-1.756-.426-1.756-2.924 0-3.35a1.724 1.724 0 001.066-2.573c-.94-1.543.826-3.31 2.37-2.37.996.608 2.296.07 2.573-1.066z\"/><circle cx=\"12\" cy=\"12\" r=\"3\"/></svg>");
    sb.append("<div class=\"label\">System Settings</div><div class=\"desc\">Device settings</div></a>");
    sb.append("</div></div>");
    sb.append(navBar("/"));
    sb.append("</body></html>");
    sendResponse(os, 200, "OK", "text/html; charset=UTF-8", sb.toString());
  }

  // ===== File Manager =====

  private void sendFileManagerPage(String dirPath, File rootDir, OutputStream os) throws IOException {
    File dir;
    if (dirPath != null && !dirPath.isEmpty()) {
      dir = new File(dirPath);
      if (!dir.exists() || !dir.isDirectory()) dir = rootDir;
    } else {
      dir = rootDir;
    }
    File[] files = dir.listFiles();
    if (files == null) files = new File[0];
    java.util.Arrays.sort(files, (a, b) -> {
      if (a.isDirectory() != b.isDirectory()) return a.isDirectory() ? -1 : 1;
      return a.getName().compareToIgnoreCase(b.getName());
    });
    StringBuilder sb = new StringBuilder();
    sb.append("<!DOCTYPE html><html><head><meta charset=\"UTF-8\">");
    sb.append("<meta name=\"viewport\" content=\"width=device-width,initial-scale=1\">");
    sb.append("<title>Files - E-Ink Manager</title><style>").append(CSS).append("</style></head><body>");
    sb.append("<div class=\"topbar\">");
    if (dir.getParent() != null) {
      sb.append("<a href=\"/fm?path=").append(escapeHtml(dir.getParent())).append("\" class=\"back\">");
      sb.append("<svg viewBox=\"0 0 24 24\" fill=\"none\" stroke=\"currentColor\" stroke-width=\"2\" width=\"20\" height=\"20\"><path d=\"M15 19l-7-7 7-7\"/></svg></a>");
    }
    sb.append("<h1>Files</h1></div>");
    sb.append("<div class=\"content\">");
    sb.append("<div class=\"card\"><div class=\"row\" style=\"font-size:12px;color:#888;\">");
    sb.append(escapeHtml(dir.getAbsolutePath()));
    sb.append("</div></div>");
    sb.append("<div class=\"card\">");
    if (files.length == 0) {
      sb.append("<div class=\"empty\"><svg viewBox=\"0 0 24 24\" fill=\"none\" stroke=\"currentColor\" stroke-width=\"1.5\"><path d=\"M3 7v10a2 2 0 002 2h14a2 2 0 002-2V9a2 2 0 00-2-2h-6l-2-2H5a2 2 0 00-2 2z\"/></svg><p>Empty directory</p></div>");
    } else {
      for (File f : files) {
        if (f.getName().startsWith(".")) continue;
        sb.append("<div class=\"row\">");
        if (f.isDirectory()) {
          sb.append("<svg class=\"row-icon\" viewBox=\"0 0 24 24\" fill=\"none\" stroke=\"currentColor\" stroke-width=\"2\"><path d=\"M3 7v10a2 2 0 002 2h14a2 2 0 002-2V9a2 2 0 00-2-2h-6l-2-2H5a2 2 0 00-2 2z\"/></svg>");
          sb.append("<div class=\"row-text\"><a href=\"/fm?path=").append(escapeHtml(f.getAbsolutePath())).append("\" class=\"row-title\">").append(escapeHtml(f.getName())).append("</a></div>");
        } else {
          sb.append("<svg class=\"row-icon\" viewBox=\"0 0 24 24\" fill=\"none\" stroke=\"currentColor\" stroke-width=\"2\"><path d=\"M7 21h10a2 2 0 002-2V9.414a1 1 0 00-.293-.707l-5.414-5.414A1 1 0 0012.586 3H7a2 2 0 00-2 2v14a2 2 0 002 2z\"/></svg>");
          sb.append("<div class=\"row-text\"><a href=\"").append(escapeHtml(f.getAbsolutePath())).append("\" download class=\"row-title\">").append(escapeHtml(f.getName())).append("</a>");
          sb.append("<div class=\"row-sub\">").append(formatSize(f.length())).append("</div></div>");
        }
        sb.append("<div class=\"row-action\"><button class=\"btn btn-sm btn-danger\" onclick=\"if(confirm('Delete?'))fetch('").append(escapeHtml(f.getAbsolutePath())).append("',{method:'DELETE'}).then(function(){location.reload()})\">Delete</button></div>");
        sb.append("</div>");
      }
    }
    sb.append("</div>");
    sb.append("<form class=\"upload-zone\" method=\"POST\" enctype=\"multipart/form-data\" action=\"").append(escapeHtml(dir.getAbsolutePath())).append("\">");
    sb.append("<svg viewBox=\"0 0 24 24\" fill=\"none\" stroke=\"currentColor\" stroke-width=\"2\" width=\"24\" height=\"24\" style=\"color:#1a73e8\"><path d=\"M12 5v14m-7-7h14\"/></svg>");
    sb.append("<div style=\"font-size:13px;font-weight:500\">Tap to upload files</div>");
    sb.append("<input type=\"file\" name=\"file\" multiple style=\"display:none\" onchange=\"this.form.submit()\">");
    sb.append("</form></div>");
    sb.append(navBar("/fm"));
    sb.append(toastHtml());
    sb.append("<script>document.querySelector('.upload-zone').addEventListener('click',function(e){if(e.target.tagName!=='INPUT')this.querySelector('input').click()});</script>");
    sb.append("</body></html>");
    sendResponse(os, 200, "OK", "text/html; charset=UTF-8", sb.toString());
  }

  // ===== APK Manager =====

  private void sendApkManagerPage(File rootDir, OutputStream os) throws IOException {
    java.util.List<File> apkFiles = new java.util.ArrayList<>();
    String[] scanDirs = { rootDir.getAbsolutePath() + "/Download", rootDir.getAbsolutePath() + "/Downloads", "/sdcard/Download" };
    for (String dirPath : scanDirs) {
      File dir = new File(dirPath);
      if (!dir.exists() || !dir.isDirectory()) continue;
      File[] found = dir.listFiles();
      if (found == null) continue;
      for (File f : found) {
        if (f.getName().toLowerCase().endsWith(".apk") && f.isFile()) apkFiles.add(f);
      }
    }
    StringBuilder sb = new StringBuilder();
    sb.append("<!DOCTYPE html><html><head><meta charset=\"UTF-8\">");
    sb.append("<meta name=\"viewport\" content=\"width=device-width,initial-scale=1\">");
    sb.append("<title>Apps - E-Ink Manager</title><style>").append(CSS).append("</style></head><body>");
    sb.append("<div class=\"topbar\"><h1>APK Manager</h1></div>");
    sb.append("<div class=\"content\">");
    if (apkFiles.isEmpty()) {
      sb.append("<div class=\"empty\"><svg viewBox=\"0 0 24 24\" fill=\"none\" stroke=\"currentColor\" stroke-width=\"1.5\"><path d=\"M12 18h.01M8 21h8a2 2 0 002-2V5a2 2 0 00-2-2H8a2 2 0 00-2 2v14a2 2 0 002 2z\"/></svg>");
      sb.append("<p>No APK files found in Download folders</p></div>");
    } else {
      sb.append("<div class=\"status-bar\"><div class=\"stat\"><div class=\"val\">").append(apkFiles.size()).append("</div><div class=\"lbl\">APK Files</div></div></div>");
      sb.append("<div class=\"card\">");
      for (File f : apkFiles) {
        sb.append("<div class=\"row\">");
        sb.append("<svg class=\"row-icon\" viewBox=\"0 0 24 24\" fill=\"none\" stroke=\"currentColor\" stroke-width=\"2\"><path d=\"M12 18h.01M8 21h8a2 2 0 002-2V5a2 2 0 00-2-2H8a2 2 0 00-2 2v14a2 2 0 002 2z\"/></svg>");
        sb.append("<div class=\"row-text\"><div class=\"row-title\">").append(escapeHtml(f.getName())).append("</div>");
        sb.append("<div class=\"row-sub\">").append(formatSize(f.length())).append(" &middot; ").append(escapeHtml(f.getParent())).append("</div></div>");
        sb.append("<div class=\"row-action\"><a href=\"").append(escapeHtml(f.getAbsolutePath())).append("\" download class=\"btn btn-sm btn-primary\">Install</a></div>");
        sb.append("</div>");
      }
      sb.append("</div>");
    }
    sb.append("</div>");
    sb.append(navBar("/apk"));
    sb.append("</body></html>");
    sendResponse(os, 200, "OK", "text/html; charset=UTF-8", sb.toString());
  }

  // ===== Icon Manager =====

  private void sendIconManagerPage(OutputStream os) throws IOException {
    String customIconDir = getExternalCacheDir() != null ? getExternalCacheDir().getAbsolutePath() + "/custom_icons" : null;
    java.util.Map<String, String> currentIcons = new java.util.LinkedHashMap<>();
    currentIcons.put("lock", "Lock Screen");
    currentIcons.put("wifi", "WiFi");
    currentIcons.put("http", "HTTP Server");
    java.util.List<String[]> availableIcons = new java.util.ArrayList<>();
    String[] defaultIcons = {"lock", "wifi", "http", "settings", "folder", "file", "image", "music", "video"};
    for (String icon : defaultIcons) {
      availableIcons.add(new String[]{icon, icon});
    }
    if (customIconDir != null) {
      File dir = new File(customIconDir);
      if (dir.exists() && dir.isDirectory()) {
        File[] customFiles = dir.listFiles();
        if (customFiles != null) {
          for (File f : customFiles) {
            String name = f.getName();
            if (name.endsWith(".png") || name.endsWith(".jpg") || name.endsWith(".webp")) {
              availableIcons.add(new String[]{name, "custom/" + name});
            }
          }
        }
      }
    }
    StringBuilder sb = new StringBuilder();
    sb.append("<!DOCTYPE html><html><head><meta charset=\"UTF-8\">");
    sb.append("<meta name=\"viewport\" content=\"width=device-width,initial-scale=1\">");
    sb.append("<title>Icons - E-Ink Manager</title><style>").append(CSS).append("</style></head><body>");
    sb.append("<div class=\"topbar\"><h1>Icon Manager</h1></div>");
    sb.append("<div class=\"content\">");
    sb.append("<div class=\"card\"><div class=\"card-header\">Current Icons</div>");
    for (java.util.Map.Entry<String, String> entry : currentIcons.entrySet()) {
      sb.append("<div class=\"row\">");
      sb.append("<svg class=\"row-icon\" viewBox=\"0 0 24 24\" fill=\"none\" stroke=\"currentColor\" stroke-width=\"2\"><circle cx=\"12\" cy=\"12\" r=\"10\"/></svg>");
      sb.append("<div class=\"row-text\"><div class=\"row-title\">").append(entry.getValue()).append("</div>");
      sb.append("<div class=\"row-sub\">").append(entry.getKey()).append(".png</div></div>");
      sb.append("<div class=\"row-action\"><button class=\"btn btn-sm\" onclick=\"showToast('Long press icon to replace')\">Change</button></div>");
      sb.append("</div>");
    }
    sb.append("</div>");
    sb.append("<div class=\"card\"><div class=\"card-header\">Available Icons</div>");
    sb.append("<div class=\"icon-grid\">");
    for (String[] icon : availableIcons) {
      sb.append("<div class=\"icon-item\" onclick=\"showToast('Selected: ").append(escapeHtml(icon[0])).append("')\">");
      sb.append("<div style=\"width:48px;height:48px;background:#f0f0f0;border-radius:8px;display:flex;align-items:center;justify-content:center;margin:0 auto 6px;\">");
      sb.append("<svg viewBox=\"0 0 24 24\" fill=\"none\" stroke=\"#666\" stroke-width=\"2\" width=\"24\" height=\"24\"><circle cx=\"12\" cy=\"12\" r=\"10\"/></svg>");
      sb.append("</div><div class=\"name\">").append(escapeHtml(icon[0])).append("</div></div>");
    }
    sb.append("</div></div>");
    sb.append("<div class=\"upload-zone\" id=\"uploadZone\">");
    sb.append("<svg viewBox=\"0 0 24 24\" fill=\"none\" stroke=\"currentColor\" stroke-width=\"2\" width=\"24\" height=\"24\" style=\"color:#1a73e8\"><path d=\"M12 5v14m-7-7h14\"/></svg>");
    sb.append("<div style=\"font-size:13px;font-weight:500\">Upload custom icon (PNG, 96x96)</div>");
    sb.append("<input type=\"file\" id=\"iconUpload\" accept=\"image/*\" style=\"display:none\">");
    sb.append("</div>");
    sb.append("<p style=\"font-size:12px;color:#888;margin-top:8px;text-align:center;\">Icons are stored in the app's cache directory.<br>Recommended size: 96x96px, PNG format.</p>");
    sb.append("</div>");
    sb.append(navBar("/icons"));
    sb.append(toastHtml());
    sb.append("<script>document.getElementById('uploadZone').addEventListener('click',function(){document.getElementById('iconUpload').click()});</script>");
    sb.append("</body></html>");
    sendResponse(os, 200, "OK", "text/html; charset=UTF-8", sb.toString());
  }

  // ===== System Settings =====

  private void sendSystemSettingsPage(OutputStream os) throws IOException {
    StringBuilder sb = new StringBuilder();
    sb.append("<!DOCTYPE html><html><head><meta charset=\"UTF-8\">");
    sb.append("<meta name=\"viewport\" content=\"width=device-width,initial-scale=1\">");
    sb.append("<title>Settings - E-Ink Manager</title><style>").append(CSS).append("</style></head><body>");
    sb.append("<div class=\"topbar\"><h1>System Settings</h1></div>");
    sb.append("<div class=\"content\">");
    sb.append("<div class=\"card\">");
    String[][] settings = {
      {"WiFi", "android.settings.WIFI_SETTINGS", "M3 7v10a2 2 0 002 2h14a2 2 0 002-2V9a2 2 0 00-2-2h-6l-2-2H5a2 2 0 00-2 2z"},
      {"Display", "android.settings.DISPLAY_SETTINGS", "M12 3v1m0 16v1m9-9h-1M4 12H3m15.364 6.364l-.707-.707M6.343 6.343l-.707-.707m12.728 0l-.707.707M6.343 17.657l-.707.707M16 12a4 4 0 11-8 0 4 4 0 018 0z"},
      {"Sound", "android.settings.SOUND_SETTINGS", "M15.536 8.464a5 5 0 010 7.072m2.828-9.9a9 9 0 010 12.728M5.586 15H4a1 1 0 01-1-1v-4a1 1 0 011-1h1.586l4.707-4.707C10.923 3.663 12 4.109 12 5v14c0 .891-1.077 1.337-1.707.707L5.586 15z"},
      {"Apps", "android.settings.APPLICATION_DETAILS_SETTINGS", "M9.75 17L9 20l-1 1h8l-1-1-.75-3M3 13h18M5 17h14a2 2 0 002-2V5a2 2 0 00-2-2H5a2 2 0 00-2 2v10a2 2 0 002 2z"},
      {"Developer", "android.settings.APPLICATION_DEVELOPMENT_SETTINGS", "M10 20l4-16m4 4l4 4-4 4M6 16l-4-4 4-4"},
      {"Battery", "android.settings.BATTERY_SAVER_SETTINGS", "M17 6h-2V4a2 2 0 00-2-2H9a2 2 0 00-2 2v2H5a2 2 0 00-2 2v10a2 2 0 002 2h14a2 2 0 002-2V8a2 2 0 00-2-2z"},
      {"Storage", "android.settings.INTERNAL_STORAGE_SETTINGS", "M4 7v10c0 2.21 3.582 4 8 4s8-1.79 8-4V7M4 7c0 2.21 3.582 4 8 4s8-1.79 8-4M4 7c0-2.21 3.582-4 8-4s8 1.79 8 4m0 5c0 2.21-3.582 4-8 4s-8-1.79-8-4"},
      {"Notifications", "android.settings.NOTIFICATION_LISTENER_SETTINGS", "M15 17h5l-1.405-1.405A2.032 2.032 0 0118 14.158V11a6.002 6.002 0 00-4-5.659V5a2 2 0 10-4 0v.341C7.67 6.165 6 8.388 6 11v3.159c0 .538-.214 1.055-.595 1.436L4 17h5m6 0v1a3 3 0 11-6 0v-1m6 0H9"},
      {"Location", "android.settings.LOCATION_SOURCE_SETTINGS", "M17.657 16.657L13.414 20.9a1.998 1.998 0 01-2.827 0l-4.244-4.243a8 8 0 1111.314 0z"},
      {"Security", "android.settings.SECURITY_SETTINGS", "M12 15v2m-6 4h12a2 2 0 002-2v-6a2 2 0 00-2-2H6a2 2 0 00-2 2v6a2 2 0 002 2zm10-10V7a4 4 0 00-8 0v4h8z"}
    };
    for (String[] s : settings) {
      sb.append("<a href=\"").append(s[1]).append("\" class=\"row\" style=\"text-decoration:none;color:inherit;\">");
      sb.append("<svg class=\"row-icon\" viewBox=\"0 0 24 24\" fill=\"none\" stroke=\"currentColor\" stroke-width=\"2\"><path d=\"").append(s[2]).append("\"/></svg>");
      sb.append("<div class=\"row-text\"><div class=\"row-title\">").append(s[0]).append("</div></div>");
      sb.append("<svg class=\"row-action\" viewBox=\"0 0 24 24\" fill=\"none\" stroke=\"currentColor\" stroke-width=\"2\" width=\"16\" height=\"16\"><path d=\"M9 5l7 7-7 7\"/></svg>");
      sb.append("</a>");
    }
    sb.append("</div></div>");
    sb.append(navBar("/settings"));
    sb.append("</body></html>");
    sendResponse(os, 200, "OK", "text/html; charset=UTF-8", sb.toString());
  }

  // ===== POST (Upload) =====

  private void sendFile(File file, OutputStream os) throws IOException {
    String name = file.getName().toLowerCase();
    String contentType = "application/octet-stream";
    if (name.endsWith(".html") || name.endsWith(".htm")) contentType = "text/html";
    else if (name.endsWith(".css")) contentType = "text/css";
    else if (name.endsWith(".js")) contentType = "application/javascript";
    else if (name.endsWith(".json")) contentType = "application/json";
    else if (name.endsWith(".xml")) contentType = "text/xml";
    else if (name.endsWith(".txt") || name.endsWith(".log")) contentType = "text/plain";
    else if (name.endsWith(".png")) contentType = "image/png";
    else if (name.endsWith(".jpg") || name.endsWith(".jpeg")) contentType = "image/jpeg";
    else if (name.endsWith(".gif")) contentType = "image/gif";
    else if (name.endsWith(".webp")) contentType = "image/webp";
    else if (name.endsWith(".svg")) contentType = "image/svg+xml";
    else if (name.endsWith(".pdf")) contentType = "application/pdf";
    else if (name.endsWith(".zip")) contentType = "application/zip";
    else if (name.endsWith(".mp3")) contentType = "audio/mpeg";
    else if (name.endsWith(".mp4")) contentType = "video/mp4";
    else if (name.endsWith(".apk")) contentType = "application/vnd.android.package-archive";

    String header = "HTTP/1.1 200 OK\r\n"
        + "Content-Type: " + contentType + "\r\n"
        + "Content-Length: " + file.length() + "\r\n"
        + "Content-Disposition: attachment; filename=\"" + file.getName() + "\"\r\n"
        + "Connection: close\r\n"
        + "\r\n";
    os.write(header.getBytes("UTF-8"));
    os.flush();

    FileInputStream fis = new FileInputStream(file);
    byte[] buffer = new byte[8192];
    int bytesRead;
    while ((bytesRead = fis.read(buffer)) != -1) {
      os.write(buffer, 0, bytesRead);
    }
    fis.close();
  }

  // ===== POST (Upload) =====

  private void handlePost(String path, File rootDir, InputStream is,
      String contentLengthStr, String contentType, OutputStream os) throws IOException {
    if (contentType == null || !contentType.contains("multipart/form-data")) {
      sendResponse(os, 400, "Bad Request", "text/plain", "Expected multipart/form-data");
      return;
    }

    int contentLength = 0;
    if (contentLengthStr != null) {
      try { contentLength = Integer.parseInt(contentLengthStr); } catch (NumberFormatException ignored) {}
    }
    if (contentLength <= 0) {
      sendResponse(os, 400, "Bad Request", "text/plain", "Missing Content-Length");
      return;
    }

    String boundary = extractBoundary(contentType);
    if (boundary == null) {
      sendResponse(os, 400, "Bad Request", "text/plain", "Missing boundary");
      return;
    }

    byte[] allBytes = readFully(is, contentLength);
    File targetDir = new File(rootDir, path);
    if (!targetDir.exists() || !targetDir.isDirectory()) {
      sendResponse(os, 400, "Bad Request", "text/plain", "Target is not a directory");
      return;
    }

    int uploaded = 0;
    String delimiter = "--" + boundary;
    int pos = findBytes(allBytes, delimiter.getBytes("UTF-8"));
    while (pos >= 0) {
      pos += delimiter.length();
      if (pos + 2 <= allBytes.length && allBytes[pos] == '\r' && allBytes[pos + 1] == '\n') {
        pos += 2;
      } else if (pos + 2 <= allBytes.length && allBytes[pos] == '\n') {
        pos += 1;
      }

      int headerEnd = findBytes(allBytes, "\r\n\r\n".getBytes("UTF-8"), pos);
      if (headerEnd < 0) {
        headerEnd = findBytes(allBytes, "\n\n".getBytes("UTF-8"), pos);
      }
      if (headerEnd < 0) break;

      String partHeader;
      if (allBytes[headerEnd] == '\r') {
        partHeader = new String(allBytes, pos, headerEnd - pos, "UTF-8");
        headerEnd += 4;
      } else {
        partHeader = new String(allBytes, pos, headerEnd - pos, "UTF-8");
        headerEnd += 2;
      }

      String fileName = extractFileName(partHeader);
      if (fileName == null) {
        int nextBoundary = findBytes(allBytes, delimiter.getBytes("UTF-8"), headerEnd);
        if (nextBoundary < 0) break;
        pos = nextBoundary;
        continue;
      }

      int dataEnd = findBytes(allBytes, ("\r\n" + delimiter).getBytes("UTF-8"), headerEnd);
      if (dataEnd < 0) {
        dataEnd = findBytes(allBytes, ("\n" + delimiter).getBytes("UTF-8"), headerEnd);
      }
      if (dataEnd < 0) break;

      byte[] fileData = new byte[dataEnd - headerEnd];
      System.arraycopy(allBytes, headerEnd, fileData, 0, fileData.length);

      File outFile = new File(targetDir, fileName);
      FileOutputStream fos = new FileOutputStream(outFile);
      fos.write(fileData);
      fos.close();
      uploaded++;

      int nextBoundary = findBytes(allBytes, delimiter.getBytes("UTF-8"), dataEnd);
      if (nextBoundary < 0) break;
      pos = nextBoundary;
    }

    String resp = "<html><body><h2>Uploaded " + uploaded + " file(s)</h2>"
        + "<p><a href=\"" + escapeHtml(path) + "\">Back</a></p></body></html>";
    sendResponse(os, 200, "OK", "text/html; charset=UTF-8", resp);
  }

  // ===== DELETE =====

  private void handleDelete(String path, File rootDir, OutputStream os) throws IOException {
    File file = new File(rootDir, path);
    if (!file.exists()) {
      sendResponse(os, 404, "Not Found", "text/plain", "404 Not Found");
      return;
    }
    if (file.delete()) {
      sendResponse(os, 200, "OK", "text/plain", "Deleted");
    } else {
      sendResponse(os, 500, "Internal Server Error", "text/plain", "Failed to delete");
    }
  }

  // ===== Utilities =====

  private void sendResponse(OutputStream os, int code, String status,
      String contentType, String body) throws IOException {
    byte[] bodyBytes = body.getBytes("UTF-8");
    String header = "HTTP/1.1 " + code + " " + status + "\r\n"
        + "Content-Type: " + contentType + "\r\n"
        + "Content-Length: " + bodyBytes.length + "\r\n"
        + "Connection: close\r\n"
        + "\r\n";
    os.write(header.getBytes("UTF-8"));
    os.write(bodyBytes);
    os.flush();
  }

  private String buildErrorPage(int code, String message) {
    return "<!DOCTYPE html><html><head><meta charset=\"UTF-8\"></head><body>"
        + "<h1>" + code + "</h1><p>" + message + "</p>"
        + "<p><a href=\"/\">Back to root</a></p></body></html>";
  }

  private String extractBoundary(String contentType) {
    String[] parts = contentType.split(";");
    for (String part : parts) {
      String trimmed = part.trim();
      if (trimmed.startsWith("boundary=")) {
        String b = trimmed.substring(9).trim();
        if (b.startsWith("\"") && b.endsWith("\"")) {
          b = b.substring(1, b.length() - 1);
        }
        return b;
      }
    }
    return null;
  }

  private String extractQueryParam(String uri, String name) {
    int qIdx = uri.indexOf('?');
    if (qIdx < 0) return null;
    String query = uri.substring(qIdx + 1);
    String[] pairs = query.split("&");
    for (String pair : pairs) {
      String[] kv = pair.split("=", 2);
      if (kv.length == 2 && name.equals(kv[0])) {
        try {
          return URLDecoder.decode(kv[1], "UTF-8");
        } catch (Exception e) {
          return kv[1];
        }
      }
    }
    return null;
  }

  private String extractFileName(String partHeader) {
    String[] lines = partHeader.split("\r\n");
    for (String line : lines) {
      String lower = line.toLowerCase();
      if (lower.contains("content-disposition")) {
        int fnIdx = lower.indexOf("filename=\"");
        if (fnIdx >= 0) {
          int start = fnIdx + 10;
          int end = line.indexOf('"', start);
          if (end > start) {
            return line.substring(start, end);
          }
        }
      }
    }
    return null;
  }

  private byte[] readFully(InputStream is, int length) throws IOException {
    byte[] data = new byte[length];
    int offset = 0;
    while (offset < length) {
      int read = is.read(data, offset, length - offset);
      if (read < 0) break;
      offset += read;
    }
    return data;
  }

  private int findBytes(byte[] haystack, byte[] needle) {
    return findBytes(haystack, needle, 0);
  }

  private int findBytes(byte[] haystack, byte[] needle, int start) {
    outer:
    for (int i = start; i <= haystack.length - needle.length; i++) {
      for (int j = 0; j < needle.length; j++) {
        if (haystack[i + j] != needle[j]) continue outer;
      }
      return i;
    }
    return -1;
  }

  private String escapeHtml(String s) {
    if (s == null) return "";
    return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
        .replace("\"", "&quot;").replace("'", "&#39;");
  }

  private String formatSize(long bytes) {
    if (bytes < 1024) return bytes + " B";
    if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
    if (bytes < 1024 * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024));
    return String.format("%.1f GB", bytes / (1024.0 * 1024 * 1024));
  }

  // ===== Lifecycle =====

  @Override
  public void onDestroy() {
    instance = null;
    running = false;
    try {
      if (serverSocket != null && !serverSocket.isClosed()) {
        serverSocket.close();
      }
    } catch (IOException ignored) {}
    if (threadPool != null) {
      threadPool.shutdownNow();
    }
    sendBroadcast(new Intent(ACTION_STOPPED));
    Log.i(TAG, "HTTP server stopped");
    super.onDestroy();
  }

  @Override
  public void onTaskRemoved(Intent rootIntent) {
    super.onTaskRemoved(rootIntent);
    Intent restartService = new Intent(getApplicationContext(), this.getClass());
    restartService.setPackage(getPackageName());
    PendingIntent pi = PendingIntent.getService(
        getApplicationContext(), 1, restartService,
        PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_IMMUTABLE);
    AlarmManager alarm = (AlarmManager) getApplicationContext()
        .getSystemService(Context.ALARM_SERVICE);
    alarm.set(AlarmManager.ELAPSED_REALTIME, SystemClock.elapsedRealtime() + 2000, pi);
  }

  // ===== Static helpers =====

  public static boolean isRunning() {
    return instance != null && instance.running;
  }

  private static HttpService instance;

  @Override
  public void onCreate() {
    super.onCreate();
    instance = this;
  }

  public static int getPort() {
    return port;
  }

  public static boolean isConnectedToLocalNetwork(Context context) {
    boolean connected = false;
    ConnectivityManager cm = (ConnectivityManager) context
        .getSystemService(Context.CONNECTIVITY_SERVICE);
    NetworkInfo ni = cm.getActiveNetworkInfo();
    connected = ni != null
        && ni.isConnected()
        && (ni.getType() & (ConnectivityManager.TYPE_WIFI | ConnectivityManager.TYPE_ETHERNET)) != 0;
    if (!connected) {
      WifiManager wm = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
      try {
        Method method = wm.getClass().getDeclaredMethod("isWifiApEnabled");
        connected = (Boolean) method.invoke(wm);
      } catch (Exception e) {
        // ignore
      }
    }
    if (!connected) {
      try {
        for (NetworkInterface netInterface : Collections.list(NetworkInterface.getNetworkInterfaces())) {
          if (netInterface.getDisplayName().startsWith("rndis")) {
            connected = true;
          }
        }
      } catch (SocketException e) {
        // ignore
      }
    }
    return connected;
  }

  public static boolean isConnectedToWifi(Context context) {
    ConnectivityManager cm = (ConnectivityManager) context
        .getSystemService(Context.CONNECTIVITY_SERVICE);
    NetworkInfo ni = cm.getActiveNetworkInfo();
    return ni != null && ni.isConnected()
        && ni.getType() == ConnectivityManager.TYPE_WIFI;
  }

  public static InetAddress getLocalInetAddress(Context context) {
    if (!isConnectedToLocalNetwork(context)) {
      return null;
    }
    if (isConnectedToWifi(context)) {
      WifiManager wm = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
      int ipAddress = wm.getConnectionInfo().getIpAddress();
      if (ipAddress == 0) return null;
      return intToInet(ipAddress);
    }
    try {
      Enumeration<NetworkInterface> netinterfaces = NetworkInterface.getNetworkInterfaces();
      while (netinterfaces.hasMoreElements()) {
        NetworkInterface netinterface = netinterfaces.nextElement();
        Enumeration<InetAddress> adresses = netinterface.getInetAddresses();
        while (adresses.hasMoreElements()) {
          InetAddress address = adresses.nextElement();
          if (!address.isLoopbackAddress() && !address.isLinkLocalAddress()) {
            return address;
          }
        }
      }
    } catch (Exception e) {
      // ignore
    }
    return null;
  }

  public static InetAddress intToInet(int value) {
    byte[] bytes = new byte[4];
    for (int i = 0; i < 4; i++) {
      bytes[i] = (byte) (value >> (i * 8));
    }
    try {
      return InetAddress.getByAddress(bytes);
    } catch (UnknownHostException e) {
      return null;
    }
  }

  public static boolean isPortAvailable(int checkPort) {
    ServerSocket ss = null;
    DatagramSocket ds = null;
    try {
      ss = new ServerSocket(checkPort);
      ss.setReuseAddress(true);
      ds = new DatagramSocket(checkPort);
      ds.setReuseAddress(true);
      return true;
    } catch (IOException e) {
      return false;
    } finally {
      if (ds != null) ds.close();
      if (ss != null) {
        try { ss.close(); } catch (IOException ignored) {}
      }
    }
  }
}
