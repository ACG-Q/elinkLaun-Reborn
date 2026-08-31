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
        handleGet(path, rootDir, os);
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

  // ===== GET =====

  private void handleGet(String path, File rootDir, OutputStream os) throws IOException {
    if ("/fm".equals(path)) {
      sendFileManagerPage(path, rootDir, os);
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

    File file = new File(rootDir, path);

    if (!file.exists()) {
      sendResponse(os, 404, "Not Found", "text/html", buildErrorPage(404, "File Not Found"));
      return;
    }

    if (file.isDirectory()) {
      sendDirectoryListing(path, file, os);
    } else {
      sendFile(file, os);
    }
  }

  private void sendDirectoryListing(String path, File dir, OutputStream os) throws IOException {
    File[] files = dir.listFiles();
    StringBuilder sb = new StringBuilder();
    sb.append("<!DOCTYPE html><html><head><meta charset=\"UTF-8\">");
    sb.append("<meta name=\"viewport\" content=\"width=device-width,initial-scale=1\">");
    sb.append("<title>E-Ink Launcher - ");
    sb.append(escapeHtml(path));
    sb.append("</title><style>");
    sb.append("*{margin:0;padding:0;box-sizing:border-box;}");
    sb.append("body{font-family:system-ui,sans-serif;background:#f5f5f5;color:#333;padding:16px;}");
    sb.append("h1{font-size:18px;margin-bottom:12px;word-break:break-all;}");
    sb.append(".item{display:flex;align-items:center;padding:10px 12px;margin:4px 0;");
    sb.append("background:#fff;border-radius:6px;text-decoration:none;color:#333;");
    sb.append("border:1px solid #e0e0e0;}");
    sb.append(".item:hover{background:#e8f0fe;border-color:#4285f4;}");
    sb.append(".item.dir{font-weight:600;}");
    sb.append(".name{flex:1;word-break:break-all;font-size:14px;}");
    sb.append(".size{color:#888;font-size:12px;margin-left:8px;white-space:nowrap;}");
    sb.append(".del{color:#d32f2f;margin-left:8px;font-size:12px;text-decoration:none;");
    sb.append("padding:4px 8px;border-radius:4px;border:1px solid #d32f2f;background:#fff;}");
    sb.append(".del:hover{background:#d32f2f;color:#fff;}");
    sb.append("form{margin-top:16px;padding:16px;background:#fff;border-radius:6px;");
    sb.append("border:1px solid #e0e0e0;}");
    sb.append("input[type=file]{margin-right:8px;}");
    sb.append("button{padding:6px 16px;background:#4285f4;color:#fff;border:none;");
    sb.append("border-radius:4px;cursor:pointer;font-size:14px;}");
    sb.append("button:hover{background:#3367d6;}");
    sb.append("</style></head><body>");
    sb.append("<h1>");
    sb.append(escapeHtml(path.equals("/") ? "Root" : path));
    sb.append("</h1>");

    if (!"/".equals(path)) {
      String parent = path.endsWith("/") ? new File(path).getParent() : new File(path).getParent();
      if (parent == null) parent = "/";
      sb.append("<a class=\"item dir\" href=\"");
      sb.append(escapeHtml(parent.equals("/") ? "/" : parent + "/"));
      sb.append("\"><span class=\"name\">.. (Parent)</span></a>");
    }

    if (files != null) {
      java.util.Arrays.sort(files, (a, b) -> {
        if (a.isDirectory() != b.isDirectory()) {
          return a.isDirectory() ? -1 : 1;
        }
        return a.getName().compareToIgnoreCase(b.getName());
      });

      for (File f : files) {
        if (f.getName().startsWith(".")) continue;
        sb.append("<a class=\"item");
        if (f.isDirectory()) sb.append(" dir");
        String href = path.endsWith("/") ? path + f.getName() : path + "/" + f.getName();
        if (f.isDirectory()) href += "/";
        sb.append("\" href=\"");
        sb.append(escapeHtml(href));
        sb.append("\"><span class=\"name\">");
        sb.append(escapeHtml(f.getName()));
        sb.append("</span>");
        if (f.isFile()) {
          sb.append("<span class=\"size\">");
          sb.append(formatSize(f.length()));
          sb.append("</span>");
        }
        sb.append("<span class=\"del\" onclick=\"event.preventDefault();if(confirm('Delete?");
        sb.append("'))fetch('");
        sb.append(escapeHtml(href));
        sb.append("',{method:'DELETE'}).then(()=>location.reload());\">X</span>");
        sb.append("</a>");
      }
    }

    sb.append("<form method=\"POST\" enctype=\"multipart/form-data\" action=\"");
    sb.append(escapeHtml(path.endsWith("/") ? path : path + "/"));
    sb.append("\"><input type=\"file\" name=\"file\" multiple required>");
    sb.append("<button type=\"submit\">Upload</button></form>");
    sb.append("</body></html>");

    sendResponse(os, 200, "OK", "text/html; charset=UTF-8", sb.toString());
  }

  // ===== Web UI: File Manager =====

  private void sendFileManagerPage(String path, File rootDir, OutputStream os) throws IOException {
    File dir = new File(rootDir, path);
    if (!dir.exists() || !dir.isDirectory()) dir = rootDir;
    File[] files = dir.listFiles();
    if (files == null) files = new File[0];
    java.util.Arrays.sort(files, (a, b) -> {
      if (a.isDirectory() != b.isDirectory()) return a.isDirectory() ? -1 : 1;
      return a.getName().compareToIgnoreCase(b.getName());
    });
    StringBuilder sb = new StringBuilder();
    sb.append("<!DOCTYPE html><html><head><meta charset=\"UTF-8\"><meta name=\"viewport\" content=\"width=device-width,initial-scale=1\">");
    sb.append("<title>File Manager</title><style>");
    sb.append("*{margin:0;padding:0;box-sizing:border-box;}");
    sb.append("body{font-family:system-ui,sans-serif;background:#f5f5f5;color:#333;padding:12px;}");
    sb.append("h1{font-size:18px;margin-bottom:8px;}");
    sb.append(".nav{display:flex;gap:8px;margin-bottom:12px;flex-wrap:wrap;}");
    sb.append(".nav a{padding:6px 14px;background:#fff;border:1px solid #ddd;border-radius:6px;text-decoration:none;color:#333;font-size:13px;}");
    sb.append(".nav a:hover{background:#e8f0fe;border-color:#4285f4;}");
    sb.append(".nav a.active{background:#4285f4;color:#fff;border-color:#4285f4;}");
    sb.append(".path{font-size:13px;color:#666;margin-bottom:10px;word-break:break-all;}");
    sb.append("table{width:100%;border-collapse:collapse;background:#fff;border-radius:6px;overflow:hidden;}");
    sb.append("th,td{padding:10px 12px;text-align:left;border-bottom:1px solid #e8e8e8;font-size:14px;}");
    sb.append("th{background:#fafafa;font-weight:600;font-size:12px;color:#888;text-transform:uppercase;}");
    sb.append("tr:hover{background:#f8f9fa;}");
    sb.append(".name{color:#333;text-decoration:none;}");
    sb.append(".name.dir{font-weight:600;color:#1a73e8;}");
    sb.append(".size{color:#888;font-size:12px;white-space:nowrap;}");
    sb.append(".actions{white-space:nowrap;}");
    sb.append(".btn{padding:4px 10px;border-radius:4px;border:1px solid #ccc;background:#fff;text-decoration:none;font-size:12px;margin-left:4px;cursor:pointer;}");
    sb.append(".btn.del{color:#d32f2f;border-color:#d32f2f;}.btn.del:hover{background:#d32f2f;color:#fff;}");
    sb.append(".btn.dl{color:#188038;border-color:#188038;}.btn.dl:hover{background:#188038;color:#fff;}");
    sb.append(".upload{margin-top:12px;padding:12px;background:#fff;border-radius:6px;border:1px solid #e0e0e0;}");
    sb.append(".empty{text-align:center;padding:40px;color:#999;font-size:14px;}");
    sb.append("</style></head><body>");
    sb.append("<h1>\ud83d\udcc1 File Manager</h1>");
    sb.append("<div class=\"nav\"><a href=\"/fm\" class=\"active\">Files</a><a href=\"/apk\">APK Manager</a><a href=\"/settings\">Settings</a></div>");
    sb.append("<div class=\"path\">\ud83d\udcc2 ").append(escapeHtml(dir.getAbsolutePath())).append("</div>");
    String parent = dir.getParent();
    if (parent != null) {
      sb.append("<table><tr><th>Name</th><th>Size</th><th>Actions</th></tr>");
      sb.append("<tr><td colspan=\"3\"><a class=\"name dir\" href=\"/fm?path=").append(escapeHtml(parent)).append("\">\u2b06 .. (Parent)</a></td></tr>");
    } else {
      sb.append("<table><tr><th>Name</th><th>Size</th><th>Actions</th></tr>");
    }
    if (files.length == 0) {
      sb.append("<tr><td colspan=\"3\" class=\"empty\">Empty directory</td></tr>");
    } else {
      for (File f : files) {
        sb.append("<tr>");
        String icon = f.isDirectory() ? "\ud83d\udcc1" : "\ud83d\udcc4";
        String href = "/fm?path=" + escapeHtml(f.getAbsolutePath());
        sb.append("<td><a class=\"name").append(f.isDirectory() ? " dir" : "").append("\" href=\"").append(href).append("\">").append(icon).append(" ").append(escapeHtml(f.getName())).append("</a></td>");
        sb.append("<td class=\"size\">"); if (f.isFile()) sb.append(formatSize(f.length())); sb.append("</td>");
        sb.append("<td class=\"actions\">");
        if (f.isFile()) sb.append("<a class=\"btn dl\" href=\"").append(escapeHtml(f.getAbsolutePath())).append("\" download>Download</a>");
        sb.append("<a class=\"btn del\" href=\"#\" onclick=\"if(confirm('Delete?'))fetch('").append(escapeHtml(f.getAbsolutePath())).append("',{method:'DELETE'}).then(()=>location.reload());\">Delete</a>");
        sb.append("</td></tr>");
      }
    }
    sb.append("</table>");
    sb.append("<form class=\"upload\" method=\"POST\" enctype=\"multipart/form-data\" action=\"").append(escapeHtml(dir.getAbsolutePath())).append("\">");
    sb.append("<input type=\"file\" name=\"file\" multiple style=\"margin-right:8px;\">");
    sb.append("<button type=\"submit\" style=\"padding:6px 16px;background:#4285f4;color:#fff;border:none;border-radius:4px;cursor:pointer;font-size:14px;\">Upload</button>");
    sb.append("</form></body></html>");
    sendResponse(os, 200, "OK", "text/html; charset=UTF-8", sb.toString());
  }

  // ===== Web UI: APK Manager =====

  private void sendApkManagerPage(File rootDir, OutputStream os) throws IOException {
    java.util.List<File> apkFiles = new java.util.ArrayList<>();
    String[] scanDirs = { rootDir.getAbsolutePath() + "/Download", rootDir.getAbsolutePath() + "/Downloads", "/sdcard/Download" };
    for (String dirPath : scanDirs) {
      File dir = new File(dirPath);
      if (!dir.exists() || !dir.isDirectory()) continue;
      File[] found = dir.listFiles();
      if (found == null) continue;
      for (File f : found) { if (f.getName().toLowerCase().endsWith(".apk") && f.isFile()) apkFiles.add(f); }
    }
    StringBuilder sb = new StringBuilder();
    sb.append("<!DOCTYPE html><html><head><meta charset=\"UTF-8\"><meta name=\"viewport\" content=\"width=device-width,initial-scale=1\">");
    sb.append("<title>APK Manager</title><style>");
    sb.append("*{margin:0;padding:0;box-sizing:border-box;}");
    sb.append("body{font-family:system-ui,sans-serif;background:#f5f5f5;color:#333;padding:12px;}");
    sb.append("h1{font-size:18px;margin-bottom:8px;}");
    sb.append(".nav{display:flex;gap:8px;margin-bottom:12px;flex-wrap:wrap;}");
    sb.append(".nav a{padding:6px 14px;background:#fff;border:1px solid #ddd;border-radius:6px;text-decoration:none;color:#333;font-size:13px;}");
    sb.append(".nav a:hover{background:#e8f0fe;border-color:#4285f4;}");
    sb.append(".nav a.active{background:#4285f4;color:#fff;border-color:#4285f4;}");
    sb.append("table{width:100%;border-collapse:collapse;background:#fff;border-radius:6px;overflow:hidden;}");
    sb.append("th,td{padding:10px 12px;text-align:left;border-bottom:1px solid #e8e8e8;font-size:14px;}");
    sb.append("th{background:#fafafa;font-weight:600;font-size:12px;color:#888;}");
    sb.append("tr:hover{background:#f8f9fa;}");
    sb.append(".btn{padding:6px 14px;border-radius:4px;border:1px solid #188038;background:#fff;color:#188038;text-decoration:none;font-size:13px;cursor:pointer;}");
    sb.append(".btn:hover{background:#188038;color:#fff;}");
    sb.append(".empty{text-align:center;padding:40px;color:#999;font-size:14px;}");
    sb.append("</style></head><body>");
    sb.append("<h1>\ud83d\udce6 APK Manager</h1>");
    sb.append("<div class=\"nav\"><a href=\"/fm\">Files</a><a href=\"/apk\" class=\"active\">APK Manager</a><a href=\"/settings\">Settings</a></div>");
    if (apkFiles.isEmpty()) {
      sb.append("<div class=\"empty\">No APK files found in Download folders</div>");
    } else {
      sb.append("<table><tr><th>File</th><th>Size</th><th>Location</th><th>Action</th></tr>");
      for (File f : apkFiles) {
        sb.append("<tr><td>").append(escapeHtml(f.getName())).append("</td>");
        sb.append("<td>").append(formatSize(f.length())).append("</td>");
        sb.append("<td>").append(escapeHtml(f.getParent())).append("</td>");
        sb.append("<td><a class=\"btn\" href=\"").append(escapeHtml(f.getAbsolutePath())).append("\" download>Install</a></td></tr>");
      }
      sb.append("</table>");
    }
    sb.append("</body></html>");
    sendResponse(os, 200, "OK", "text/html; charset=UTF-8", sb.toString());
  }

  // ===== Web UI: System Settings =====

  private void sendSystemSettingsPage(OutputStream os) throws IOException {
    java.util.List<SettingEntry> settings = new java.util.ArrayList<>();
    settings.add(new SettingEntry("WiFi", "Wi-Fi settings", Settings.ACTION_WIFI_SETTINGS));
    settings.add(new SettingEntry("Display", "Brightness, screen timeout", Settings.ACTION_DISPLAY_SETTINGS));
    settings.add(new SettingEntry("Sound", "Ringtone, volume", Settings.ACTION_SOUND_SETTINGS));
    settings.add(new SettingEntry("Apps", "App management", Settings.ACTION_APPLICATION_DETAILS_SETTINGS));
    settings.add(new SettingEntry("Developer", "Developer options", Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS));
    settings.add(new SettingEntry("Battery", "Battery saver", Settings.ACTION_BATTERY_SAVER_SETTINGS));
    settings.add(new SettingEntry("Storage", "Internal storage", Settings.ACTION_INTERNAL_STORAGE_SETTINGS));
    settings.add(new SettingEntry("Notifications", "Notification access", Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS));
    settings.add(new SettingEntry("Location", "Location services", Settings.ACTION_LOCATION_SOURCE_SETTINGS));
    settings.add(new SettingEntry("Security", "Lock screen, encryption", Settings.ACTION_SECURITY_SETTINGS));
    StringBuilder sb = new StringBuilder();
    sb.append("<!DOCTYPE html><html><head><meta charset=\"UTF-8\"><meta name=\"viewport\" content=\"width=device-width,initial-scale=1\">");
    sb.append("<title>System Settings</title><style>");
    sb.append("*{margin:0;padding:0;box-sizing:border-box;}");
    sb.append("body{font-family:system-ui,sans-serif;background:#f5f5f5;color:#333;padding:12px;}");
    sb.append("h1{font-size:18px;margin-bottom:8px;}");
    sb.append(".nav{display:flex;gap:8px;margin-bottom:12px;flex-wrap:wrap;}");
    sb.append(".nav a{padding:6px 14px;background:#fff;border:1px solid #ddd;border-radius:6px;text-decoration:none;color:#333;font-size:13px;}");
    sb.append(".nav a:hover{background:#e8f0fe;border-color:#4285f4;}");
    sb.append(".nav a.active{background:#4285f4;color:#fff;border-color:#4285f4;}");
    sb.append(".grid{display:grid;grid-template-columns:repeat(auto-fill,minmax(160px,1fr));gap:10px;}");
    sb.append(".card{background:#fff;border-radius:8px;padding:16px;text-align:center;border:1px solid #e0e0e0;text-decoration:none;color:#333;cursor:pointer;}");
    sb.append(".card:hover{border-color:#4285f4;background:#f8f9fa;}");
    sb.append(".icon{font-size:32px;margin-bottom:8px;}");
    sb.append(".title{font-weight:600;font-size:14px;}");
    sb.append(".desc{font-size:11px;color:#888;margin-top:4px;}");
    sb.append("</style></head><body>");
    sb.append("<h1>\u2699\ufe0f System Settings</h1>");
    sb.append("<div class=\"nav\"><a href=\"/fm\">Files</a><a href=\"/apk\">APK Manager</a><a href=\"/settings\" class=\"active\">Settings</a></div>");
    sb.append("<div class=\"grid\">");
    for (SettingEntry s : settings) {
      sb.append("<a class=\"card\" href=\"").append(escapeHtml(s.action)).append("\">");
      sb.append("<div class=\"icon\">").append(s.icon).append("</div>");
      sb.append("<div class=\"title\">").append(escapeHtml(s.title)).append("</div>");
      sb.append("<div class=\"desc\">").append(escapeHtml(s.desc)).append("</div>");
      sb.append("</a>");
    }
    sb.append("</div></body></html>");
    sendResponse(os, 200, "OK", "text/html; charset=UTF-8", sb.toString());
  }

  private static class SettingEntry {
    String title; String desc; String action; String icon;
    SettingEntry(String title, String desc, String action) {
      this.title = title; this.desc = desc; this.action = action;
      switch (title) {
        case "WiFi": this.icon = "\ud83d\udcf1"; break;
        case "Display": this.icon = "\u2600\ufe0f"; break;
        case "Sound": this.icon = "\ud83d\udd0a"; break;
        case "Apps": this.icon = "\ud83d\udcf1"; break;
        case "Developer": this.icon = "\ud83d\udd27"; break;
        case "Battery": this.icon = "\ud83d\udd0b"; break;
        case "Storage": this.icon = "\ud83d\udcbe"; break;
        case "Notifications": this.icon = "\ud83d\udd14"; break;
        case "Location": this.icon = "\ud83d\udccd"; break;
        case "Security": this.icon = "\ud83d\udd12"; break;
        default: this.icon = "\u2699\ufe0f";
      }
    }
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
