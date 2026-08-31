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
import android.content.res.AssetManager;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

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

  // ===== GET =====

  private void handleGet(String path, String fullUri, File rootDir, OutputStream os) throws IOException {
    if ("/api/stats".equals(path)) { sendJsonStats(rootDir, os); return; }
    if ("/api/files".equals(path)) { sendJsonFiles(extractQueryParam(fullUri, "path"), rootDir, os); return; }
    if ("/api/apks".equals(path)) { sendJsonApks(rootDir, os); return; }
    if ("/api/icons".equals(path)) { sendJsonIcons(os); return; }

    if ("/".equals(path) || "/index.html".equals(path)) { sendAssetFile("index.html", os); return; }
    if (path.startsWith("/css/") || path.startsWith("/js/")) { sendAssetFile(path.substring(1), os); return; }

    File file = new File(rootDir, path);
    if (!file.exists()) { sendResponse(os, 404, "Not Found", "text/html", buildErrorPage(404, "File Not Found")); return; }
    if (file.isDirectory()) { sendJsonFiles(file.getAbsolutePath(), rootDir, os); return; }
    sendFile(file, os);
  }

  // ===== Static file serving =====

  private void sendAssetFile(String assetPath, OutputStream os) throws IOException {
    try {
      InputStream is = getAssets().open("web/" + assetPath);
      String mime = getMimeType(assetPath);
      byte[] header = ("HTTP/1.1 200 OK\r\nContent-Type: " + mime + "\r\nConnection: close\r\n\r\n").getBytes("UTF-8");
      os.write(header);
      byte[] buf = new byte[8192];
      int n;
      while ((n = is.read(buf)) != -1) os.write(buf, 0, n);
      is.close();
    } catch (IOException e) {
      sendResponse(os, 404, "Not Found", "text/plain", "404 Not Found");
    }
  }

  private String getMimeType(String path) {
    String lower = path.toLowerCase();
    if (lower.endsWith(".html") || lower.endsWith(".htm")) return "text/html";
    if (lower.endsWith(".css")) return "text/css";
    if (lower.endsWith(".js")) return "application/javascript";
    if (lower.endsWith(".json")) return "application/json";
    if (lower.endsWith(".png")) return "image/png";
    if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) return "image/jpeg";
    if (lower.endsWith(".gif")) return "image/gif";
    if (lower.endsWith(".svg")) return "image/svg+xml";
    if (lower.endsWith(".ico")) return "image/x-icon";
    if (lower.endsWith(".woff2")) return "font/woff2";
    if (lower.endsWith(".woff")) return "font/woff";
    if (lower.endsWith(".ttf")) return "font/ttf";
    return "application/octet-stream";
  }

  // ===== JSON API =====

  private void sendJsonStats(File rootDir, OutputStream os) throws IOException {
    try {
      File[] files = rootDir.listFiles();
      int count = 0;
      long total = 0;
      if (files != null) {
        for (File f : files) {
          if (!f.getName().startsWith(".")) {
            count++;
            if (f.isFile()) total += f.length();
          }
        }
      }
      JSONObject json = new JSONObject();
      json.put("fileCount", count);
      json.put("totalSize", total);
      json.put("totalSizeHuman", formatSize(total));
      sendJsonResponse(os, json.toString());
    } catch (JSONException e) {
      throw new IOException("JSON error", e);
    }
  }

  private void sendJsonFiles(String dirPath, File rootDir, OutputStream os) throws IOException {
    try {
      File dir = (dirPath != null && !dirPath.isEmpty()) ? new File(dirPath) : rootDir;
      if (!dir.exists() || !dir.isDirectory()) dir = rootDir;
      File[] files = dir.listFiles();
      if (files == null) files = new File[0];
      java.util.Arrays.sort(files, (a, b) -> {
        if (a.isDirectory() != b.isDirectory()) return a.isDirectory() ? -1 : 1;
        return a.getName().compareToIgnoreCase(b.getName());
      });
      JSONObject json = new JSONObject();
      json.put("currentPath", dir.getAbsolutePath());
      json.put("parentPath", dir.getParent());
      JSONArray arr = new JSONArray();
      for (File f : files) {
        if (f.getName().startsWith(".")) continue;
        JSONObject item = new JSONObject();
        item.put("name", f.getName());
        item.put("path", f.getAbsolutePath());
        item.put("isDir", f.isDirectory());
        item.put("size", f.isFile() ? f.length() : 0);
        item.put("sizeHuman", f.isFile() ? formatSize(f.length()) : "");
        arr.put(item);
      }
      json.put("items", arr);
      sendJsonResponse(os, json.toString());
    } catch (JSONException e) {
      throw new IOException("JSON error", e);
    }
  }

  private void sendJsonApks(File rootDir, OutputStream os) throws IOException {
    try {
      java.util.List<File> apkFiles = new java.util.ArrayList<>();
      String[] dirs = { rootDir.getAbsolutePath() + "/Download", rootDir.getAbsolutePath() + "/Downloads", "/sdcard/Download" };
      for (String d : dirs) {
        File dir = new File(d);
        if (!dir.exists() || !dir.isDirectory()) continue;
        File[] found = dir.listFiles();
        if (found != null) {
          for (File f : found) {
            if (f.getName().toLowerCase().endsWith(".apk") && f.isFile()) apkFiles.add(f);
          }
        }
      }
      JSONObject json = new JSONObject();
      JSONArray arr = new JSONArray();
      for (File f : apkFiles) {
        JSONObject item = new JSONObject();
        item.put("name", f.getName());
        item.put("path", f.getAbsolutePath());
        item.put("parent", f.getParent());
        item.put("size", f.length());
        item.put("sizeHuman", formatSize(f.length()));
        arr.put(item);
      }
      json.put("items", arr);
      sendJsonResponse(os, json.toString());
    } catch (JSONException e) {
      throw new IOException("JSON error", e);
    }
  }

  private void sendJsonIcons(OutputStream os) throws IOException {
    try {
      JSONObject json = new JSONObject();
      JSONObject current = new JSONObject();
      current.put("lock", new JSONObject().put("label", "Lock Screen"));
      current.put("wifi", new JSONObject().put("label", "WiFi"));
      current.put("http", new JSONObject().put("label", "HTTP Server"));
      json.put("currentIcons", current);
      JSONArray arr = new JSONArray();
      String[] defaults = {"lock", "wifi", "http", "settings", "folder", "file", "image", "music", "video"};
      for (String name : defaults) {
        arr.put(new JSONObject().put("name", name).put("type", "default"));
      }
      File iconDir = new File(getExternalCacheDir(), "custom_icons");
      if (iconDir.exists() && iconDir.isDirectory()) {
        File[] custom = iconDir.listFiles();
        if (custom != null) {
          for (File f : custom) {
            String n = f.getName();
            if (n.endsWith(".png") || n.endsWith(".jpg") || n.endsWith(".webp")) {
              arr.put(new JSONObject().put("name", n).put("type", "custom").put("url", "/custom_icons/" + n));
            }
          }
        }
      }
      json.put("availableIcons", arr);
      sendJsonResponse(os, json.toString());
    } catch (JSONException e) {
      throw new IOException("JSON error", e);
    }
  }

  private void sendJsonResponse(OutputStream os, String json) throws IOException {
    byte[] body = json.getBytes("UTF-8");
    String header = "HTTP/1.1 200 OK\r\nContent-Type: application/json; charset=UTF-8\r\n"
        + "Content-Length: " + body.length + "\r\nConnection: close\r\n\r\n";
    os.write(header.getBytes("UTF-8"));
    os.write(body);
    os.flush();
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
    if (path.startsWith("/api/upload")) {
      try {
        String targetPath = extractQueryParam(path, "path");
        File targetDir = (targetPath != null && !targetPath.isEmpty()) ? new File(targetPath) : rootDir;
        if (!targetDir.exists() || !targetDir.isDirectory()) {
          sendJsonResponse(os, new JSONObject().put("success", false).put("error", "Target is not a directory").toString());
          return;
        }
        if (contentType == null || !contentType.contains("multipart/form-data")) {
          sendJsonResponse(os, new JSONObject().put("success", false).put("error", "Expected multipart/form-data").toString());
          return;
        }
        int contentLength = 0;
        if (contentLengthStr != null) {
          try { contentLength = Integer.parseInt(contentLengthStr); } catch (NumberFormatException ignored) {}
        }
        if (contentLength <= 0) {
          sendJsonResponse(os, new JSONObject().put("success", false).put("error", "Missing Content-Length").toString());
          return;
        }
        String boundary = extractBoundary(contentType);
        if (boundary == null) {
          sendJsonResponse(os, new JSONObject().put("success", false).put("error", "Missing boundary").toString());
          return;
        }
        byte[] allBytes = readFully(is, contentLength);
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
          if (headerEnd < 0) headerEnd = findBytes(allBytes, "\n\n".getBytes("UTF-8"), pos);
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
          if (dataEnd < 0) dataEnd = findBytes(allBytes, ("\n" + delimiter).getBytes("UTF-8"), headerEnd);
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
        sendJsonResponse(os, new JSONObject().put("success", true).put("uploaded", uploaded).toString());
      } catch (JSONException e) {
        throw new IOException("JSON error", e);
      }
      return;
    }

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
    if (path.startsWith("/api/files")) {
      try {
        String filePath = extractQueryParam(path, "path");
        if (filePath == null || filePath.isEmpty()) {
          sendJsonResponse(os, new JSONObject().put("success", false).put("error", "Missing path").toString());
          return;
        }
        File file = new File(filePath);
        boolean deleted = file.exists() && file.delete();
        sendJsonResponse(os, new JSONObject().put("success", deleted).toString());
      } catch (JSONException e) {
        throw new IOException("JSON error", e);
      }
      return;
    }
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
