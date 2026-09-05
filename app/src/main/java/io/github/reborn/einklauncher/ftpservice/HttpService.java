package io.github.reborn.einklauncher.ftpservice;

import io.github.reborn.einklauncher.R;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Environment;
import android.os.IBinder;
import android.os.StatFs;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;

import java.lang.reflect.Method;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.URLDecoder;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * 轻量级 HTTP 文件服务器，用于局域网内浏览器访问设备文件。
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

    // ==================== Request Abstraction ====================

    static class Request {
        final String method;
        final String path;
        final String fullUri;
        final Map<String, String> headers;
        final String contentLength;
        final InputStream bodyStream;

        Request(String method, String path, String fullUri, Map<String, String> headers, String contentLength, InputStream bodyStream) {
            this.method = method;
            this.path = path;
            this.fullUri = fullUri;
            this.headers = headers;
            this.contentLength = contentLength;
            this.bodyStream = bodyStream;
        }
    }

    // ==================== Route Handler Interface ====================

    interface RouteHandler {
        void handle(Request req, OutputStream os) throws IOException;
    }

    // ==================== Upload Action Enum ====================

    enum UploadAction {
        FILE, ICON_UPLOAD, ICON_REPLACE, INSTALL;

        static UploadAction fromString(String action) {
            if (action == null) return FILE;
            switch (action) {
                case "icon-upload": return ICON_UPLOAD;
                case "icon-replace": return ICON_REPLACE;
                case "install": return INSTALL;
                default: return FILE;
            }
        }
    }

    // ==================== Configuration Constants ====================

    private static final int BUFFER_SIZE = 64 * 1024;
    private static final int MAX_THREAD_POOL_SIZE = 10;
    private static final int CLIENT_SOCKET_TIMEOUT = 30000;
    private static final int MAX_REQUEST_LINE_LENGTH = 8192;
    private static final int MAX_CONTENT_LENGTH = 100 * 1024 * 1024; // 100MB limit
    private static final int CHUNK_SIZE = 256 * 1024;
    private static final long SESSION_TIMEOUT_MS = 5 * 60 * 1000;
    private static final String CHANNEL_ID = "http_server";

    // ==================== Virtual Icons ====================

    private static final LinkedHashMap<String, int[]> VIRTUAL_ICONS = new LinkedHashMap<>();
    static {
        VIRTUAL_ICONS.put("E-ink_Launcher.Lock", new int[]{R.drawable.ic_onekeylock});
        VIRTUAL_ICONS.put("E-ink_Launcher.WiFi", new int[]{R.drawable.wifi_on});
        VIRTUAL_ICONS.put("E-ink_Launcher.WiFiOff", new int[]{R.drawable.wifi_off});
        VIRTUAL_ICONS.put("E-ink_Launcher.HttpServer", new int[]{R.drawable.http_server});
    }

    private static final String[] VIRTUAL_NAMES = {"OneKey Lock", "WiFi Control", "WiFi Off", "HTTP Server"};

    // ==================== MIME Type Map ====================

    private static final Map<String, String> MIME_MAP = new HashMap<>();
    static {
        MIME_MAP.put("html", "text/html; charset=UTF-8");
        MIME_MAP.put("htm", "text/html; charset=UTF-8");
        MIME_MAP.put("css", "text/css; charset=UTF-8");
        MIME_MAP.put("js", "application/javascript; charset=UTF-8");
        MIME_MAP.put("mjs", "application/javascript; charset=UTF-8");
        MIME_MAP.put("json", "application/json; charset=UTF-8");
        MIME_MAP.put("xml", "text/xml; charset=UTF-8");
        MIME_MAP.put("txt", "text/plain; charset=UTF-8");
        MIME_MAP.put("log", "text/plain; charset=UTF-8");
        MIME_MAP.put("png", "image/png");
        MIME_MAP.put("jpg", "image/jpeg");
        MIME_MAP.put("jpeg", "image/jpeg");
        MIME_MAP.put("gif", "image/gif");
        MIME_MAP.put("webp", "image/webp");
        MIME_MAP.put("svg", "image/svg+xml");
        MIME_MAP.put("ico", "image/x-icon");
        MIME_MAP.put("pdf", "application/pdf");
        MIME_MAP.put("zip", "application/zip");
        MIME_MAP.put("mp3", "audio/mpeg");
        MIME_MAP.put("mp4", "video/mp4");
        MIME_MAP.put("apk", "application/vnd.android.package-archive");
        MIME_MAP.put("woff2", "font/woff2");
        MIME_MAP.put("woff", "font/woff");
        MIME_MAP.put("ttf", "font/ttf");
        MIME_MAP.put("map", "application/json");
    }

    // ==================== Static asset file extensions ====================

    private static final Set<String> STATIC_EXTENSIONS = MIME_MAP.keySet();

    private static String getMimeType(String fileName) {
        int dot = fileName.lastIndexOf('.');
        if (dot < 0) return "application/octet-stream";
        String ext = fileName.substring(dot + 1).toLowerCase();
        String mime = MIME_MAP.get(ext);
        return mime != null ? mime : "application/octet-stream";
    }

    // ==================== Instance State ====================

    private static volatile int port = DEFAULT_PORT;
    private int pendingPort = -1;
    private ServerSocket serverSocket;
    private ExecutorService threadPool;
    private volatile boolean running = false;
    private static HttpService instance;
    private static final ConcurrentHashMap<String, UploadSession> uploadSessions = new ConcurrentHashMap<>();
    private static File uploadTempDir;

    // ==================== Route Tables ====================

    private final Map<String, RouteHandler> getRoutes = new HashMap<>();
    private final Map<String, RouteHandler> postRoutes = new HashMap<>();

    private void initRoutes() {
        // GET routes
        get("GET", "/api/stats", (req, os) -> sendJsonStats(Environment.getExternalStorageDirectory(), os));
        get("GET", "/api/files", (req, os) -> sendJsonFiles(extractQueryParam(req.fullUri, "path"), Environment.getExternalStorageDirectory(), os));
        get("GET", "/api/apps", (req, os) -> sendJsonApps(os));
        get("GET", "/api/app-icon", (req, os) -> sendImageIcon(extractQueryParam(req.fullUri, "pkg"), os));
        get("GET", "/api/icons", (req, os) -> sendJsonIcons(os));
        get("GET", "/api/device", (req, os) -> sendJsonDevice(os));
        get("GET", "/api/battery", (req, os) -> sendJsonBattery(os));
        get("GET", "/api/storage", (req, os) -> sendJsonStorage(Environment.getExternalStorageDirectory(), os));
        get("GET", "/api/wifi-status", (req, os) -> sendJsonWifiStatus(os));
        get("GET", "/api/volume", (req, os) -> sendJsonVolume(os));
        get("GET", "/api/brightness", (req, os) -> sendJsonBrightness(os));
        get("GET", "/api/rotation", (req, os) -> sendJsonRotation(os));

        // POST routes
        post("POST", "/api/upload/start", (req, os) -> handleChunkedUpload(req.path, req.fullUri, req.bodyStream, req.contentLength, os));
        post("POST", "/api/upload/chunk", (req, os) -> handleChunkedUpload(req.path, req.fullUri, req.bodyStream, req.contentLength, os));
        post("POST", "/api/upload/complete", (req, os) -> handleChunkedUpload(req.path, req.fullUri, req.bodyStream, req.contentLength, os));
        post("POST", "/api/volume", (req, os) -> handleSetVolume(req.fullUri, os));
        post("POST", "/api/brightness", (req, os) -> handleSetBrightness(req.fullUri, os));
        post("POST", "/api/rotation", (req, os) -> handleSetRotation(req.fullUri, os));
        post("POST", "/api/icons/assign", (req, os) -> handleIconAssign(req.fullUri, os));
        post("POST", "/api/open-settings", (req, os) -> handleOpenSettings(req.fullUri, os));
        post("POST", "/api/app-install", (req, os) -> handleAppInstall(req.fullUri, os));
        post("POST", "/api/app-uninstall", (req, os) -> handleAppUninstall(req.fullUri, os));
        post("POST", "/api/app-open", (req, os) -> handleAppOpen(req.fullUri, os));
    }

    private void get(String method, String path, RouteHandler handler) {
        getRoutes.put(path, handler);
    }

    private void post(String method, String path, RouteHandler handler) {
        postRoutes.put(path, handler);
    }

    // ==================== Preferences ====================

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

    public static int getPort() {
        return port;
    }

    public static boolean isRunning() {
        return instance != null && instance.running;
    }

    // ==================== Service Lifecycle ====================

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        createNotificationChannel();
        getUploadTempDir();
        startCleanupTimer();
        initRoutes();
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

    @Override
    public void onDestroy() {
        instance = null;
        running = false;
        stopCleanupTimer();
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
                Log.d(TAG, "Server socket closed");
            }
        } catch (IOException e) {
            Log.w(TAG, "Error closing server socket: " + e.getMessage());
        }
        if (threadPool != null) {
            threadPool.shutdownNow();
            Log.d(TAG, "Thread pool shut down");
        }
        stopForeground(true);
        sendBroadcast(new Intent(ACTION_STOPPED));
        Log.i(TAG, "HTTP server stopped on port " + port);
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

    // ==================== Server Core ====================

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
            threadPool = Executors.newFixedThreadPool(MAX_THREAD_POOL_SIZE);
            running = true;
            startForegroundWithNotification("Listening on port " + port);
            sendBroadcast(new Intent(ACTION_STARTED));
            Log.i(TAG, "HTTP server started on port " + port + " with thread pool size " + MAX_THREAD_POOL_SIZE);

            while (running) {
                try {
                    Socket client = serverSocket.accept();
                    Log.d(TAG, "New connection from " + client.getRemoteSocketAddress());
                    threadPool.execute(() -> handleClient(client));
                } catch (IOException e) {
                    if (running) {
                        Log.e(TAG, "Accept error: " + e.getMessage(), e);
                    }
                }
            }
        } catch (IOException e) {
            Log.e(TAG, "Failed to start server on port " + port + ": " + e.getMessage(), e);
            sendBroadcast(new Intent(ACTION_FAILEDTOSTART));
        }
    }

    private void handleClient(Socket client) {
        try {
            client.setSoTimeout(CLIENT_SOCKET_TIMEOUT);
            try (InputStream is = new BufferedInputStream(client.getInputStream(), BUFFER_SIZE);
                 OutputStream os = client.getOutputStream()) {

                Request req = parseRequest(is, client);
                if (req == null) return;

                RouteHandler handler = null;
                if ("GET".equals(req.method)) {
                    handler = getRoutes.get(req.path);
                } else if ("POST".equals(req.method)) {
                    handler = postRoutes.get(req.path);
                    if (handler == null && req.path.startsWith("/api/icons/upload")) {
                        handler = (r, out) -> handleChunkedUpload("/api/upload/start", r.fullUri, null, r.contentLength, out);
                    }
                    if (handler == null && req.path.startsWith("/api/icons/replace")) {
                        handler = (r, out) -> handleChunkedUpload("/api/upload/start", r.fullUri, null, r.contentLength, out);
                    }
                } else if ("DELETE".equals(req.method)) {
                    if (req.path.startsWith("/api/files")) {
                        handler = (r, out) -> handleDelete(r.path, r.fullUri, Environment.getExternalStorageDirectory(), out);
                    }
                }

                if (handler != null) {
                    handler.handle(req, os);
                } else if ("GET".equals(req.method)) {
                    handleGetFallback(req, os);
                } else {
                    sendResponse(os, 405, "Method Not Allowed", "text/plain", "405 Method Not Allowed");
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Handle client error", e);
        } finally {
            try { client.close(); } catch (IOException ignored) {}
        }
    }

    private Request parseRequest(InputStream is, Socket client) throws IOException {
        String requestLine = readLine(is);
        if (requestLine == null || requestLine.isEmpty()) {
            Log.d(TAG, "Empty request from " + client.getRemoteSocketAddress());
            client.close();
            return null;
        }
        if (requestLine.length() > MAX_REQUEST_LINE_LENGTH) {
            Log.w(TAG, "Request line too long (" + requestLine.length() + " chars) from " + client.getRemoteSocketAddress());
            sendResponse(client.getOutputStream(), 414, "Request URI Too Long", "text/plain", "414 Request URI Too Long");
            client.close();
            return null;
        }
        Log.d(TAG, "Request from " + client.getRemoteSocketAddress() + ": " + requestLine);

        String[] parts = requestLine.split(" ");
        if (parts.length < 2) {
            Log.w(TAG, "Malformed request line: " + requestLine);
            sendResponse(client.getOutputStream(), 400, "Bad Request", "text/plain", "400 Bad Request");
            client.close();
            return null;
        }

        String method = parts[0];
        String uri = parts[1];
        String path = uri.indexOf('?') >= 0 ? URLDecoder.decode(uri.substring(0, uri.indexOf('?')), "UTF-8") : URLDecoder.decode(uri, "UTF-8");

        Map<String, String> headers = new HashMap<>();
        String contentLengthStr = null;
        String line;
        while ((line = readLine(is)) != null && !line.isEmpty()) {
            String lower = line.toLowerCase();
            if (lower.startsWith("content-length:")) {
                contentLengthStr = line.substring(15).trim();
            }
            int colonIdx = line.indexOf(':');
            if (colonIdx > 0) {
                headers.put(line.substring(0, colonIdx).trim().toLowerCase(), line.substring(colonIdx + 1).trim());
            }
        }

        if (contentLengthStr != null) {
            try {
                int contentLength = Integer.parseInt(contentLengthStr);
                if (contentLength > MAX_CONTENT_LENGTH) {
                    Log.w(TAG, "Content-Length exceeds limit: " + contentLength);
                    sendResponse(client.getOutputStream(), 413, "Payload Too Large", "text/plain", "413 Payload Too Large");
                    client.close();
                    return null;
                }
            } catch (NumberFormatException e) {
                Log.w(TAG, "Invalid Content-Length: " + contentLengthStr);
            }
        }

        return new Request(method, path, uri, headers, contentLengthStr, is);
    }

    private void handleGetFallback(Request req, OutputStream os) throws IOException {
        String path = req.path;
        File rootDir = Environment.getExternalStorageDirectory();

        if (path.startsWith("/custom_icons/")) {
            sendCustomIconFile(path, os);
            return;
        }

        if ("/".equals(path) || "/index.html".equals(path)) {
            sendAssetFile("index.html", os);
            return;
        }

        if (isStaticAsset(path)) {
            String assetPath = path.startsWith("/") ? path.substring(1) : path;
            if (assetExists(assetPath)) {
                sendAssetFile(assetPath, os);
                return;
            }
        }

        File file = new File(rootDir, path);
        if (!file.exists()) {
            Log.d(TAG, "File not found: " + path);
            sendResponse(os, 404, "Not Found", "text/html", buildErrorPage(404, "File Not Found"));
            return;
        }
        if (!isPathSafe(path, rootDir)) {
            Log.w(TAG, "Path traversal attempt blocked: " + path);
            sendResponse(os, 403, "Forbidden", "text/html", buildErrorPage(403, "Access Denied"));
            return;
        }
        if (file.isDirectory()) {
            sendJsonFiles(file.getAbsolutePath(), rootDir, os);
            return;
        }
        sendFile(file, os);
    }

    private String readLine(InputStream is) throws IOException {
        StringBuilder sb = new StringBuilder();
        int cur;
        while ((cur = is.read()) != -1) {
            if (cur == '\n') {
                break;
            }
            if (cur != '\r') {
                sb.append((char) cur);
            }
        }
        if (cur == -1 && sb.length() == 0) {
            return null;
        }
        return sb.toString();
    }

    // ==================== Security Helpers ====================

    private boolean isPathSafe(String path, File rootDir) {
        try {
            File file = path.startsWith("/") ? new File(rootDir, path) : new File(path);
            String canonicalPath = file.getCanonicalPath();
            String rootPath = rootDir.getCanonicalPath();
            return canonicalPath.startsWith(rootPath);
        } catch (IOException e) {
            return false;
        }
    }

    private boolean isValidPackageName(String pkg) {
        if (pkg == null || pkg.isEmpty()) return false;
        return pkg.matches("^[a-zA-Z0-9_][a-zA-Z0-9._]*$");
    }

    // ==================== Static File Serving ====================

    private boolean isStaticAsset(String path) {
        String lower = path.toLowerCase();
        for (String ext : STATIC_EXTENSIONS) {
            if (lower.endsWith(ext)) return true;
        }
        return false;
    }

    private boolean assetExists(String assetPath) {
        try (InputStream is = getAssets().open(assetPath)) {
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    private void sendAssetFile(String assetPath, OutputStream os) throws IOException {
        try (InputStream is = getAssets().open(assetPath)) {
            String mime = getMimeType(assetPath);
            byte[] fileBytes = readAllBytes(is);
            sendBinaryResponse(os, 200, "OK", mime, fileBytes);
        } catch (IOException e) {
            sendResponse(os, 404, "Not Found", "text/plain", "404 Not Found");
        }
    }

    private void sendFile(File file, OutputStream os) throws IOException {
        String contentType = getMimeType(file.getName());
        byte[] header = buildHeader(200, "OK", contentType, file.length(), "attachment; filename=\"" + file.getName() + "\"");
        os.write(header);
        os.flush();
        try (FileInputStream fis = new FileInputStream(file)) {
            copyStream(fis, os);
        }
    }

    // ==================== JSON API: Stats & Files ====================

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
            Log.e(TAG, "Failed to build stats JSON: " + e.getMessage());
            throw new IOException("JSON error while building stats", e);
        }
    }

    private void sendJsonFiles(String dirPath, File rootDir, OutputStream os) throws IOException {
        try {
            File dir = (dirPath != null && !dirPath.isEmpty()) ? new File(dirPath) : rootDir;
            if (!dir.exists() || !dir.isDirectory()) dir = rootDir;
            File[] files = dir.listFiles();
            if (files == null) files = new File[0];
            Arrays.sort(files, (a, b) -> {
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
            Log.e(TAG, "Failed to build file list JSON: " + e.getMessage());
            throw new IOException("JSON error while building file list", e);
        }
    }

    // ==================== JSON API: Apps ====================

    private void sendJsonApps(OutputStream os) throws IOException {
        try {
            Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
            mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);
            List<ResolveInfo> list = getPackageManager().queryIntentActivities(mainIntent, 0);

            JSONObject json = new JSONObject();
            JSONArray arr = new JSONArray();

            for (ResolveInfo ri : list) {
                if ("io.github.reborn.einklauncher.Launcher".equals(ri.activityInfo.name)) continue;
                JSONObject item = new JSONObject();
                item.put("name", ri.loadLabel(getPackageManager()).toString());
                item.put("packageName", ri.activityInfo.packageName);
                item.put("isSystem", (ri.activityInfo.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0);
                arr.put(item);
            }

            json.put("items", arr);
            sendJsonResponse(os, json.toString());
        } catch (JSONException e) {
            Log.e(TAG, "Failed to build apps list JSON: " + e.getMessage());
            throw new IOException("JSON error while building apps list", e);
        }
    }

    // ==================== JSON API: Icons ====================

    private void sendJsonIcons(OutputStream os) throws IOException {
        try {
            Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
            mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);
            List<ResolveInfo> list = getPackageManager().queryIntentActivities(mainIntent, 0);

            File iconDir = new File(getExternalCacheDir(), "custom_icons");
            if (!iconDir.exists()) iconDir.mkdirs();

            JSONObject json = new JSONObject();
            JSONArray arr = new JSONArray();

            int idx = 0;
            for (Map.Entry<String, int[]> entry : VIRTUAL_ICONS.entrySet()) {
                JSONObject item = new JSONObject();
                item.put("name", VIRTUAL_NAMES[idx++]);
                item.put("packageName", entry.getKey());
                item.put("isVirtual", true);
                item.put("hasCustomIcon", new File(iconDir, entry.getKey() + ".png").exists());
                arr.put(item);
            }

            for (ResolveInfo ri : list) {
                if ("io.github.reborn.einklauncher.Launcher".equals(ri.activityInfo.name)) continue;
                String pkg = ri.activityInfo.packageName;
                JSONObject item = new JSONObject();
                item.put("name", ri.loadLabel(getPackageManager()).toString());
                item.put("packageName", pkg);
                item.put("hasCustomIcon", new File(iconDir, pkg + ".png").exists());
                arr.put(item);
            }

            json.put("items", arr);
            sendJsonResponse(os, json.toString());
        } catch (JSONException e) {
            Log.e(TAG, "Failed to build icons list JSON: " + e.getMessage());
            throw new IOException("JSON error while building icons list", e);
        }
    }

    private void sendImageIcon(String pkg, OutputStream os) throws IOException {
        if (pkg == null || pkg.isEmpty()) {
            sendEmptyIcon(os);
            return;
        }
        try {
            Bitmap icon = null;

            File customIcon = new File(getExternalCacheDir(), "custom_icons/" + pkg + ".png");
            if (!customIcon.exists()) {
                customIcon = new File(
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS),
                    "E-Ink Launcher/icon/" + pkg + ".png");
            }
            if (customIcon.exists()) {
                icon = BitmapFactory.decodeFile(customIcon.getAbsolutePath());
            }

            if (icon == null) {
                int[] res = VIRTUAL_ICONS.get(pkg);
                if (res != null) {
                    icon = BitmapFactory.decodeResource(getResources(), res[0]);
                } else {
                    Drawable d = getPackageManager().getApplicationIcon(pkg);
                    icon = drawableToBitmap(d);
                }
            }

            if (icon != null) {
                Bitmap scaled = Bitmap.createScaledBitmap(icon, UPLOAD_ICON_SIZE, UPLOAD_ICON_SIZE, true);
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                scaled.compress(Bitmap.CompressFormat.PNG, 100, baos);
                sendBinaryResponse(os, 200, "OK", "image/png", baos.toByteArray());
            } else {
                Log.w(TAG, "Failed to load icon for package: " + pkg);
                sendEmptyIcon(os);
            }
        } catch (SecurityException e) {
            Log.w(TAG, "Permission denied loading icon for package " + pkg + ": " + e.getMessage());
            sendEmptyIcon(os);
        } catch (Exception e) {
            Log.w(TAG, "Error loading icon for package " + pkg + ": " + e.getMessage());
            sendEmptyIcon(os);
        }
    }

    private void sendEmptyIcon(OutputStream os) throws IOException {
        Bitmap bmp = Bitmap.createBitmap(UPLOAD_ICON_SIZE, UPLOAD_ICON_SIZE, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(bmp);
        c.drawColor(Color.TRANSPARENT);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bmp.compress(Bitmap.CompressFormat.PNG, 100, baos);
        bmp.recycle();
        sendBinaryResponse(os, 200, "OK", "image/png", baos.toByteArray());
    }

    private void sendCustomIconFile(String path, OutputStream os) throws IOException {
        String fileName = path.substring("/custom_icons/".length());
        if (fileName.contains("..") || fileName.contains("/") || fileName.contains("\\")) {
            Log.w(TAG, "Path traversal attempt in custom icon request: " + path);
            sendResponse(os, 403, "Forbidden", "text/plain", "403 Forbidden");
            return;
        }
        File iconFile = new File(getExternalCacheDir(), "custom_icons/" + fileName);
        if (!iconFile.exists()) {
            Log.d(TAG, "Custom icon not found: " + fileName);
            sendResponse(os, 404, "Not Found", "text/plain", "404 Not Found");
            return;
        }
        String mime = getMimeType(fileName);
        byte[] header = buildHeader(200, "OK", mime, iconFile.length(), null);
        byte[] fullHeader = new String(header, "UTF-8").replace("Connection: close\r\n\r\n",
            "Cache-Control: max-age=86400\r\nConnection: close\r\n\r\n").getBytes("UTF-8");
        os.write(fullHeader);
        try (FileInputStream fis = new FileInputStream(iconFile)) {
            copyStream(fis, os);
        }
    }

    // ==================== JSON API: Device Info ====================

    private void sendJsonDevice(OutputStream os) throws IOException {
        try {
            JSONObject json = new JSONObject();
            json.put("model", Build.MODEL);
            json.put("manufacturer", Build.MANUFACTURER);
            json.put("brand", Build.BRAND);
            json.put("device", Build.DEVICE);
            json.put("board", Build.BOARD);
            json.put("hardware", Build.HARDWARE);
            json.put("release", Build.VERSION.RELEASE);
            json.put("sdkInt", Build.VERSION.SDK_INT);
            json.put("incremental", Build.VERSION.INCREMENTAL);
            json.put("host", Build.HOST);
            sendJsonResponse(os, json.toString());
        } catch (JSONException e) {
            Log.e(TAG, "Failed to build device info JSON: " + e.getMessage());
            throw new IOException("JSON error while building device info", e);
        }
    }

    private void sendJsonBattery(OutputStream os) throws IOException {
        try {
            android.content.IntentFilter filter = new android.content.IntentFilter(Intent.ACTION_BATTERY_CHANGED);
            Intent batteryStatus = registerReceiver(null, filter);
            JSONObject json = new JSONObject();
            if (batteryStatus != null) {
                int level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
                int scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
                int pct = (level * 100) / scale;
                int status = batteryStatus.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
                int health = batteryStatus.getIntExtra(BatteryManager.EXTRA_HEALTH, -1);
                int temp = batteryStatus.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0);
                int voltage = batteryStatus.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0);
                String tech = batteryStatus.getStringExtra(BatteryManager.EXTRA_TECHNOLOGY);
                json.put("level", pct);
                json.put("status", status);
                json.put("statusText", batteryStatusText(status));
                json.put("health", health);
                json.put("healthText", batteryHealthText(health));
                json.put("temperature", String.format("%.1f\u00B0C", temp / 10.0));
                json.put("voltage", String.format("%.2fV", voltage / 1000.0));
                json.put("technology", tech != null ? tech : "Unknown");
            } else {
                Log.w(TAG, "Battery status unavailable");
            }
            sendJsonResponse(os, json.toString());
        } catch (JSONException e) {
            Log.e(TAG, "Failed to build battery info JSON: " + e.getMessage());
            throw new IOException("JSON error while building battery info", e);
        }
    }

    private String batteryStatusText(int status) {
        switch (status) {
            case BatteryManager.BATTERY_STATUS_CHARGING: return "Charging";
            case BatteryManager.BATTERY_STATUS_DISCHARGING: return "Discharging";
            case BatteryManager.BATTERY_STATUS_FULL: return "Full";
            case BatteryManager.BATTERY_STATUS_NOT_CHARGING: return "Not Charging";
            default: return "Unknown";
        }
    }

    private String batteryHealthText(int health) {
        switch (health) {
            case BatteryManager.BATTERY_HEALTH_GOOD: return "Good";
            case BatteryManager.BATTERY_HEALTH_OVERHEAT: return "Overheat";
            case BatteryManager.BATTERY_HEALTH_DEAD: return "Dead";
            case BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE: return "Over Voltage";
            case BatteryManager.BATTERY_HEALTH_COLD: return "Cold";
            default: return "Unknown";
        }
    }

    private void sendJsonStorage(File rootDir, OutputStream os) throws IOException {
        try {
            StatFs stat = new StatFs(rootDir.getAbsolutePath());
            long total = stat.getTotalBytes();
            long available = stat.getAvailableBytes();
            long used = total - available;
            JSONObject json = new JSONObject();
            json.put("total", total);
            json.put("totalHuman", formatSize(total));
            json.put("used", used);
            json.put("usedHuman", formatSize(used));
            json.put("available", available);
            json.put("availableHuman", formatSize(available));
            sendJsonResponse(os, json.toString());
        } catch (JSONException e) {
            Log.e(TAG, "Failed to build storage info JSON: " + e.getMessage());
            throw new IOException("JSON error while building storage info", e);
        } catch (Exception e) {
            Log.e(TAG, "Failed to read storage info: " + e.getMessage());
            sendJsonError(os, "Failed to read storage information: " + e.getMessage());
        }
    }

    @SuppressWarnings("deprecation")
    private void sendJsonWifiStatus(OutputStream os) throws IOException {
        try {
            WifiManager wm = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
            JSONObject json = new JSONObject();
            if (wm != null) {
                int state = wm.getWifiState();
                json.put("state", state);
                json.put("stateText", wifiStateText(state));
                json.put("enabled", wm.isWifiEnabled());
                if (wm.isWifiEnabled()) {
                    try {
                        android.net.wifi.WifiInfo info = wm.getConnectionInfo();
                        if (info != null) {
                            String ssid = info.getSSID();
                            if (ssid != null && (ssid.contains("<unknown") || ssid.startsWith("0x") || ssid.isEmpty())) {
                                ssid = null;
                            }
                            if (ssid != null) {
                                ssid = ssid.replace("\"", "");
                            }
                            json.put("ssid", ssid);
                            json.put("bssid", info.getBSSID());
                            json.put("rssi", info.getRssi());
                            json.put("linkSpeed", info.getLinkSpeed());
                            json.put("networkId", info.getNetworkId());
                        }
                    } catch (SecurityException e) {
                        Log.w(TAG, "Permission denied reading WiFi info: " + e.getMessage());
                    } catch (Exception e) {
                        Log.w(TAG, "Error reading WiFi info: " + e.getMessage());
                    }
                }
            } else {
                Log.w(TAG, "WiFi service unavailable");
            }
            sendJsonResponse(os, json.toString());
        } catch (JSONException e) {
            Log.e(TAG, "Failed to build WiFi status JSON: " + e.getMessage());
            throw new IOException("JSON error while building WiFi status", e);
        }
    }

    private String wifiStateText(int state) {
        switch (state) {
            case WifiManager.WIFI_STATE_DISABLED: return "Disabled";
            case WifiManager.WIFI_STATE_DISABLING: return "Disabling";
            case WifiManager.WIFI_STATE_ENABLED: return "Enabled";
            case WifiManager.WIFI_STATE_ENABLING: return "Enabling";
            case WifiManager.WIFI_STATE_UNKNOWN: return "Unknown";
            default: return "Unknown";
        }
    }

    private void sendJsonVolume(OutputStream os) throws IOException {
        try {
            AudioManager am = (AudioManager) getSystemService(AUDIO_SERVICE);
            JSONObject json = new JSONObject();
            if (am != null) {
                putVolumeStream(json, "music", am, AudioManager.STREAM_MUSIC);
                putVolumeStream(json, "ring", am, AudioManager.STREAM_RING);
                putVolumeStream(json, "notification", am, AudioManager.STREAM_NOTIFICATION);
                putVolumeStream(json, "alarm", am, AudioManager.STREAM_ALARM);
            } else {
                Log.w(TAG, "Audio service unavailable");
            }
            sendJsonResponse(os, json.toString());
        } catch (JSONException e) {
            Log.e(TAG, "Failed to build volume info JSON: " + e.getMessage());
            throw new IOException("JSON error while building volume info", e);
        }
    }

    private void putVolumeStream(JSONObject json, String key, AudioManager am, int stream) throws JSONException {
        JSONObject s = new JSONObject();
        s.put("current", am.getStreamVolume(stream));
        s.put("max", am.getStreamMaxVolume(stream));
        s.put("min", am.getStreamMinVolume(stream));
        json.put(key, s);
    }

    private void sendJsonBrightness(OutputStream os) throws IOException {
        try {
            int brightness = Settings.System.getInt(getContentResolver(), Settings.System.SCREEN_BRIGHTNESS, 0);
            int mode = Settings.System.getInt(getContentResolver(), Settings.System.SCREEN_BRIGHTNESS_MODE, 0);
            JSONObject json = new JSONObject();
            json.put("value", brightness);
            json.put("autoMode", mode == Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC);
            sendJsonResponse(os, json.toString());
        } catch (SecurityException e) {
            Log.w(TAG, "Permission denied reading brightness: " + e.getMessage());
            sendJsonError(os, "Permission denied: cannot read brightness settings");
        } catch (Exception e) {
            Log.e(TAG, "Failed to read brightness: " + e.getMessage());
            sendJsonError(os, "Failed to read brightness: " + e.getMessage());
        }
    }

    private void sendJsonRotation(OutputStream os) throws IOException {
        try {
            int rotation = Settings.System.getInt(getContentResolver(), Settings.System.ACCELEROMETER_ROTATION, 0);
            JSONObject json = new JSONObject();
            json.put("enabled", rotation == 1);
            sendJsonResponse(os, json.toString());
        } catch (SecurityException e) {
            Log.w(TAG, "Permission denied reading rotation: " + e.getMessage());
            sendJsonError(os, "Permission denied: cannot read rotation settings");
        } catch (Exception e) {
            Log.e(TAG, "Failed to read rotation: " + e.getMessage());
            sendJsonError(os, "Failed to read rotation: " + e.getMessage());
        }
    }

    // ==================== POST Handlers ====================

    private void handleSetVolume(String fullUri, OutputStream os) throws IOException {
        try {
            String streamName = extractQueryParam(fullUri, "stream");
            String valueStr = extractQueryParam(fullUri, "value");
            
            AudioManager am = (AudioManager) getSystemService(AUDIO_SERVICE);
            if (am == null) {
                sendJsonError(os, "Audio service not available");
                return;
            }
            if (!requireParam(streamName, "stream", os)) return;
            if (!requireParam(valueStr, "value", os)) return;
            
            int streamType;
            switch (streamName) {
                case "music": streamType = AudioManager.STREAM_MUSIC; break;
                case "ring": streamType = AudioManager.STREAM_RING; break;
                case "notification": streamType = AudioManager.STREAM_NOTIFICATION; break;
                case "alarm": streamType = AudioManager.STREAM_ALARM; break;
                default: 
                    sendJsonError(os, "Invalid stream '" + streamName + "'. Valid values: music, ring, notification, alarm"); 
                    return;
            }
            
            int value = parseIntParam(valueStr, "volume", os);
            if (value == Integer.MIN_VALUE) return;
            
            int maxVolume = am.getStreamMaxVolume(streamType);
            int minVolume = am.getStreamMinVolume(streamType);
            if (value < minVolume || value > maxVolume) {
                sendJsonError(os, "Volume value " + value + " out of range [" + minVolume + ", " + maxVolume + "]");
                return;
            }
            am.setStreamVolume(streamType, value, 0);
            sendJsonOk(os);
        } catch (SecurityException e) {
            sendJsonError(os, "Permission denied: cannot modify volume settings");
        } catch (Exception e) {
            sendJsonError(os, "Volume error: " + e.getMessage());
        }
    }

    private void handleSetBrightness(String fullUri, OutputStream os) throws IOException {
        try {
            if (!requireWriteSettings(os)) return;

            String autoModeStr = extractQueryParam(fullUri, "autoMode");
            if (autoModeStr != null) {
                int mode = "true".equals(autoModeStr)
                    ? Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC
                    : Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL;
                Settings.System.putInt(getContentResolver(), Settings.System.SCREEN_BRIGHTNESS_MODE, mode);
                sendJsonOk(os);
                return;
            }

            String brightnessStr = extractQueryParam(fullUri, "value");
            if (!requireParam(brightnessStr, "value", os)) return;

            int value = parseIntParam(brightnessStr, "brightness", os);
            if (value == Integer.MIN_VALUE) return;

            if (value < 0 || value > 255) {
                sendJsonError(os, "Brightness value " + value + " out of range [0, 255]");
                return;
            }
            Settings.System.putInt(getContentResolver(),
                Settings.System.SCREEN_BRIGHTNESS_MODE,
                Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL);
            Settings.System.putInt(getContentResolver(), Settings.System.SCREEN_BRIGHTNESS, value);
            sendJsonOk(os);
        } catch (SecurityException e) {
            sendJsonError(os, "Permission denied: cannot modify brightness settings");
        } catch (Exception e) {
            sendJsonError(os, "Brightness error: " + e.getMessage());
        }
    }

    private void handleSetRotation(String fullUri, OutputStream os) throws IOException {
        try {
            String enabledStr = extractQueryParam(fullUri, "enabled");
            if (!requireParam(enabledStr, "enabled", os)) return;
            if (!requireWriteSettings(os)) return;

            int val;
            if ("true".equals(enabledStr)) {
                val = 1;
            } else if ("false".equals(enabledStr)) {
                val = 0;
            } else {
                sendJsonError(os, "Invalid value '" + enabledStr + "'. Expected 'true' or 'false'");
                return;
            }
            Settings.System.putInt(getContentResolver(), Settings.System.ACCELEROMETER_ROTATION, val);
            sendJsonOk(os);
        } catch (SecurityException e) {
            sendJsonError(os, "Permission denied: cannot modify rotation settings");
        } catch (Exception e) {
            sendJsonError(os, "Rotation error: " + e.getMessage());
        }
    }

    private void handleIconAssign(String fullUri, OutputStream os) throws IOException {
        try {
            String slot = extractQueryParam(fullUri, "slot");
            String icon = extractQueryParam(fullUri, "icon");
            if (slot == null || slot.isEmpty()) {
                sendJsonError(os, "Missing 'slot' parameter. Expected slot identifier");
                return;
            }
            if (icon == null || icon.isEmpty()) {
                sendJsonError(os, "Missing 'icon' parameter. Expected icon file path or identifier");
                return;
            }
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
            prefs.edit().putString("icon_" + slot, icon).apply();
            sendJsonOk(os);
        } catch (Exception e) {
            sendJsonError(os, "Failed to assign icon: " + e.getMessage());
        }
    }

    private void handleOpenSettings(String fullUri, OutputStream os) throws IOException {
        String action = extractQueryParam(fullUri, "action");
        if (action == null || action.isEmpty()) {
            sendJsonError(os, "Missing 'action' parameter");
            return;
        }
        try {
            Intent intent = new Intent(action);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            sendJsonOk(os);
        } catch (SecurityException e) {
            sendJsonError(os, "Permission denied: " + action);
        } catch (Exception e) {
            sendJsonError(os, "Failed to open settings: " + e.getMessage());
        }
    }

    private void handleAppInstall(String fullUri, OutputStream os) throws IOException {
        try {
            String targetPath = extractQueryParam(fullUri, "path");
            if (targetPath == null || targetPath.isEmpty()) {
                sendJsonError(os, "Missing 'path' parameter. Expected APK file path");
                return;
            }
            File apkFile = new File(targetPath);
            if (!apkFile.exists()) {
                sendJsonError(os, "APK file not found: " + targetPath);
                return;
            }
            if (!apkFile.getName().toLowerCase().endsWith(".apk")) {
                sendJsonError(os, "Invalid file type: expected .apk file, got " + apkFile.getName());
                return;
            }
            if (!apkFile.canRead()) {
                sendJsonError(os, "Cannot read APK file: permission denied");
                return;
            }
            Intent intent = new Intent(Intent.ACTION_VIEW);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                intent.setDataAndType(
                    androidx.core.content.FileProvider.getUriForFile(this, getPackageName() + ".fileProvider", apkFile),
                    "application/vnd.android.package-archive");
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            } else {
                intent.setDataAndType(Uri.fromFile(apkFile), "application/vnd.android.package-archive");
            }
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            sendInstallNotification(apkFile.getName(), intent);
            sendJsonOk(os);
        } catch (SecurityException e) {
            sendJsonError(os, "Permission denied: cannot install APK");
        } catch (Exception e) {
            sendJsonError(os, "Failed to install APK: " + e.getMessage());
        }
    }

    private void sendInstallNotification(String fileName, Intent installIntent) {
        String channelId = "install_channel";
        NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                channelId, "APK 安装", NotificationManager.IMPORTANCE_HIGH);
            nm.createNotificationChannel(channel);
        }
        PendingIntent pi = PendingIntent.getActivity(this, 0, installIntent,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        Notification.Builder builder;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder = new Notification.Builder(this, channelId);
        } else {
            builder = new Notification.Builder(this);
        }
        Notification notification = builder
            .setSmallIcon(R.drawable.http_server)
            .setContentTitle("APK 已就绪")
            .setContentText("点击安装 " + fileName)
            .setContentIntent(pi)
            .setAutoCancel(true)
            .build();
        nm.notify((int) System.currentTimeMillis(), notification);
    }

    private void handleAppUninstall(String fullUri, OutputStream os) throws IOException {
        String pkg = extractQueryParam(fullUri, "pkg");
        if (pkg == null || pkg.isEmpty()) {
            sendJsonError(os, "Missing 'pkg' parameter. Expected package name");
            return;
        }
        if (!isValidPackageName(pkg)) {
            sendJsonError(os, "Invalid package name format: " + pkg);
            return;
        }
        try {
            Intent intent = new Intent(Intent.ACTION_DELETE, Uri.parse("package:" + pkg));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            sendJsonOk(os);
        } catch (SecurityException e) {
            sendJsonError(os, "Permission denied: cannot uninstall package " + pkg);
        } catch (Exception e) {
            sendJsonError(os, "Failed to uninstall package: " + e.getMessage());
        }
    }

    private void handleAppOpen(String fullUri, OutputStream os) throws IOException {
        String pkg = extractQueryParam(fullUri, "pkg");
        if (pkg == null || pkg.isEmpty()) {
            sendJsonError(os, "Missing 'pkg' parameter. Expected package name");
            return;
        }
        if (!isValidPackageName(pkg)) {
            sendJsonError(os, "Invalid package name format: " + pkg);
            return;
        }
        try {
            Intent launchIntent = getPackageManager().getLaunchIntentForPackage(pkg);
            if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(launchIntent);
                sendJsonOk(os);
            } else {
                sendJsonError(os, "No launch intent found for package: " + pkg + ". App may not be installed or has no launcher activity");
            }
        } catch (SecurityException e) {
            sendJsonError(os, "Permission denied: cannot open package " + pkg);
        } catch (Exception e) {
            sendJsonError(os, "Failed to open app: " + e.getMessage());
        }
    }

    // ==================== DELETE Routing ====================

    private void handleDelete(String path, String fullUri, File rootDir, OutputStream os) throws IOException {
        if (path.startsWith("/api/files")) {
            try {
                String filePath = extractQueryParam(fullUri, "path");
                if (filePath == null || filePath.isEmpty()) {
                    sendJsonError(os, "Missing path");
                    return;
                }
                if (!isPathSafe(filePath, rootDir)) {
                    sendJsonError(os, "Access denied: path outside root directory");
                    return;
                }
                File file = new File(filePath);
                if (!file.exists()) {
                    sendJsonError(os, "File not found");
                    return;
                }
                boolean deleted = file.delete();
                if (deleted) {
                    sendJsonResponse(os, new JSONObject().put("success", true).toString());
                } else {
                    sendJsonError(os, "Failed to delete file: permission denied or file in use");
                }
            } catch (JSONException e) {
                throw new IOException("JSON error", e);
            }
            return;
        }
    }

    // ==================== Chunked Upload ====================

    private static final int UPLOAD_ICON_SIZE = 96;

    static class UploadSession {
        final String sessionId;
        final File tempFile;
        final UploadAction action;
        final String targetPath;
        final String pkg;
        final String fileName;
        final long totalSize;
        final int totalChunks;
        final boolean[] confirmed;
        final long createdAt;

        UploadSession(String sessionId, File tempFile, UploadAction action, String targetPath,
                      String pkg, String fileName, long totalSize) {
            this.sessionId = sessionId;
            this.tempFile = tempFile;
            this.action = action;
            this.targetPath = targetPath;
            this.pkg = pkg;
            this.fileName = fileName;
            this.totalSize = totalSize;
            this.totalChunks = (int) Math.ceil((double) totalSize / CHUNK_SIZE);
            this.confirmed = new boolean[this.totalChunks];
            this.createdAt = System.currentTimeMillis();
        }

        boolean isComplete() {
            for (boolean b : confirmed) {
                if (!b) return false;
            }
            return true;
        }
    }

    private File getUploadTempDir() {
        if (uploadTempDir == null) {
            uploadTempDir = new File(getExternalCacheDir(), "uploads");
            if (!uploadTempDir.exists()) uploadTempDir.mkdirs();
        }
        return uploadTempDir;
    }

    private ScheduledExecutorService cleanupScheduler;
    private ScheduledFuture<?> cleanupFuture;

    private void startCleanupTimer() {
        cleanupScheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "upload-cleanup");
            t.setDaemon(true);
            return t;
        });
        cleanupFuture = cleanupScheduler.scheduleWithFixedDelay(
            this::cleanupExpiredSessions, 2, 2, TimeUnit.MINUTES);
    }

    private void stopCleanupTimer() {
        if (cleanupFuture != null) cleanupFuture.cancel(false);
        if (cleanupScheduler != null) cleanupScheduler.shutdownNow();
    }

    private void cleanupExpiredSessions() {
        long now = System.currentTimeMillis();
        for (String sid : uploadSessions.keySet().toArray(new String[0])) {
            UploadSession s = uploadSessions.get(sid);
            if (s != null && now - s.createdAt > SESSION_TIMEOUT_MS) {
                UploadSession removed = uploadSessions.remove(sid);
                if (removed != null && removed.tempFile.exists()) removed.tempFile.delete();
            }
        }
    }

    // ==================== Upload Helpers ====================

    private int parseContentLength(String contentLengthStr) {
        if (contentLengthStr == null || contentLengthStr.isEmpty()) return 0;
        try {
            return Integer.parseInt(contentLengthStr.trim());
        } catch (NumberFormatException e) {
            Log.w(TAG, "Invalid Content-Length: " + contentLengthStr);
            return 0;
        }
    }

    private UploadSession requireSession(String sessionId, OutputStream os) throws IOException {
        if (sessionId == null || sessionId.isEmpty()) {
            sendJsonError(os, "Missing 'sessionId' parameter");
            return null;
        }
        UploadSession session = uploadSessions.get(sessionId);
        if (session == null) {
            sendJsonError(os, "Session not found or expired: " + sessionId);
            return null;
        }
        return session;
    }

    private int requireChunkIndex(String chunkIndexStr, int maxChunks, OutputStream os) throws IOException {
        if (chunkIndexStr == null || chunkIndexStr.isEmpty()) {
            sendJsonError(os, "Missing 'chunkIndex' parameter");
            return -1;
        }
        int idx;
        try {
            idx = Integer.parseInt(chunkIndexStr);
        } catch (NumberFormatException e) {
            sendJsonError(os, "Invalid chunk index: " + chunkIndexStr);
            return -1;
        }
        if (idx < 0 || idx >= maxChunks) {
            sendJsonError(os, "Chunk index out of range [0, " + (maxChunks - 1) + "]");
            return -1;
        }
        return idx;
    }

    private void moveFile(File src, File dest) throws IOException {
        dest.getParentFile().mkdirs();
        Files.move(src.toPath(), dest.toPath(), StandardCopyOption.REPLACE_EXISTING);
    }

    private JSONObject newJsonOk() throws JSONException {
        return new JSONObject().put("success", true);
    }

    // ==================== Permission & Settings Helpers ====================

    private boolean requireWriteSettings(OutputStream os) throws IOException {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.System.canWrite(this)) {
            try {
                Intent intent = new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS);
                intent.setData(Uri.parse("package:" + getPackageName()));
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
            } catch (Exception e) {
                Log.w(TAG, "Failed to open write settings permission page: " + e.getMessage());
            }
            sendJsonError(os, "Permission denied: please grant \"Modify system settings\" permission");
            return false;
        }
        return true;
    }

    private boolean requireParam(String value, String name, OutputStream os) throws IOException {
        if (value == null || value.isEmpty()) {
            sendJsonError(os, "Missing '" + name + "' parameter");
            return false;
        }
        return true;
    }

    private int parseIntParam(String valueStr, String name, OutputStream os) throws IOException {
        if (!requireParam(valueStr, name, os)) return Integer.MIN_VALUE;
        try {
            return Integer.parseInt(valueStr);
        } catch (NumberFormatException e) {
            sendJsonError(os, "Invalid " + name + " value '" + valueStr + "'. Must be an integer");
            return Integer.MIN_VALUE;
        }
    }

    // ==================== Upload Route Handlers ====================

    private void handleUploadStart(InputStream is, String contentLengthStr, OutputStream os) throws IOException {
        int contentLength = parseContentLength(contentLengthStr);
        if (contentLength <= 0 || contentLength > MAX_CONTENT_LENGTH) {
            sendJsonError(os, "Invalid Content-Length for upload start");
            return;
        }

        JSONObject req;
        try {
            byte[] bodyBytes = readFully(is, contentLength);
            req = new JSONObject(new String(bodyBytes, "UTF-8"));
        } catch (JSONException e) {
            sendJsonError(os, "Invalid JSON: " + e.getMessage());
            return;
        }

        String filename = req.optString("filename", "");
        long size = req.optLong("size", 0);
        UploadAction action = UploadAction.fromString(req.optString("action", "file"));
        String targetPath = req.optString("targetPath", "");
        String pkg = req.optString("pkg", "");

        if (filename.isEmpty()) {
            sendJsonError(os, "Missing 'filename'");
            return;
        }
        if (size <= 0) {
            sendJsonError(os, "Invalid file size: " + size);
            return;
        }
        if (filename.contains("..") || filename.contains("/") || filename.contains("\\")) {
            sendJsonError(os, "Invalid filename");
            return;
        }

        String sessionId = UUID.randomUUID().toString();
        File tempFile = new File(getUploadTempDir(), sessionId + ".part");
        UploadSession session = new UploadSession(sessionId, tempFile, action, targetPath, pkg, filename, size);
        uploadSessions.put(sessionId, session);

        Log.i(TAG, "Upload started: " + sessionId + " -> " + filename + " (" + size + " bytes)");

        try {
            JSONObject resp = newJsonOk()
                .put("sessionId", sessionId)
                .put("totalChunks", session.totalChunks)
                .put("chunkSize", CHUNK_SIZE);
            sendJsonResponse(os, resp.toString());
        } catch (JSONException e) {
            sendJsonError(os, "Response error");
        }
    }

    private void handleUploadChunk(String fullUri, InputStream is, String contentLengthStr, OutputStream os) throws IOException {
        String sessionId = extractQueryParam(fullUri, "sessionId");
        String chunkIndexStr = extractQueryParam(fullUri, "chunkIndex");

        Log.d(TAG, "Chunk upload: sessionId=" + sessionId + ", chunkIndex=" + chunkIndexStr + ", contentLength=" + contentLengthStr);

        UploadSession session = requireSession(sessionId, os);
        if (session == null) return;

        int chunkIndex = requireChunkIndex(chunkIndexStr, session.totalChunks, os);
        if (chunkIndex < 0) return;

        if (session.confirmed[chunkIndex]) {
            sendJsonOk(os);
            return;
        }

        long expectedSize = (chunkIndex == session.totalChunks - 1)
            ? session.totalSize - (long) chunkIndex * CHUNK_SIZE
            : CHUNK_SIZE;
        int contentLength = parseContentLength(contentLengthStr);
        if (contentLength > 0 && contentLength < expectedSize) {
            expectedSize = contentLength;
        }

        Log.d(TAG, "Chunk " + chunkIndex + ": expectedSize=" + expectedSize);

        long totalRead = 0;
        try (RandomAccessFile raf = new RandomAccessFile(session.tempFile, "rw")) {
            raf.seek((long) chunkIndex * CHUNK_SIZE);
            byte[] buf = new byte[BUFFER_SIZE];
            while (totalRead < expectedSize) {
                int toRead = (int) Math.min(buf.length, expectedSize - totalRead);
                int read = is.read(buf, 0, toRead);
                if (read < 0) break;
                raf.write(buf, 0, read);
                totalRead += read;
            }
        }

        Log.d(TAG, "Chunk " + chunkIndex + ": read " + totalRead + " bytes");

        if (totalRead < expectedSize) {
            sendJsonError(os, "Incomplete chunk: " + totalRead + "/" + expectedSize + " bytes");
            return;
        }

        session.confirmed[chunkIndex] = true;
        Log.d(TAG, "Chunk " + chunkIndex + " confirmed for " + sessionId);

        try {
            long offset = (long) chunkIndex * CHUNK_SIZE + totalRead;
            sendJsonResponse(os, newJsonOk().put("offset", offset).toString());
        } catch (JSONException e) {
            sendJsonError(os, "Response error");
        }
    }

    private void handleUploadComplete(String fullUri, OutputStream os) throws IOException {
        String sessionId = extractQueryParam(fullUri, "sessionId");

        UploadSession session = requireSession(sessionId, os);
        if (session == null) return;

        if (!session.isComplete()) {
            int first = 0;
            for (int i = 0; i < session.confirmed.length; i++) {
                if (!session.confirmed[i]) { first = i; break; }
            }
            sendJsonError(os, "Upload incomplete: chunk " + first + " not uploaded");
            return;
        }

        if (session.tempFile.length() != session.totalSize) {
            sendJsonError(os, "Size mismatch: expected " + session.totalSize + ", got " + session.tempFile.length());
            return;
        }

        try {
            String resultPath = finalizeUpload(session);
            JSONObject resp = newJsonOk();
            if (resultPath != null) resp.put("path", resultPath);
            sendJsonResponse(os, resp.toString());
        } catch (SecurityException e) {
            sendJsonError(os, "Permission denied: " + e.getMessage());
        } catch (Exception e) {
            Log.e(TAG, "Upload finalize error: " + e.getMessage(), e);
            sendJsonError(os, "Upload error: " + e.getMessage());
        } finally {
            UploadSession removed = uploadSessions.remove(sessionId);
            if (removed != null && removed.tempFile.exists()) removed.tempFile.delete();
        }
    }

    // ==================== Upload Finalization ====================

    private String finalizeUpload(UploadSession session) throws IOException {
        switch (session.action) {
            case FILE: return finalizeFileUpload(session);
            case ICON_UPLOAD: return finalizeIconUpload(session);
            case ICON_REPLACE: return finalizeIconReplace(session);
            case INSTALL: return finalizeInstall(session);
            default: throw new IOException("Unknown upload action: " + session.action);
        }
    }

    private String finalizeFileUpload(UploadSession session) throws IOException {
        File targetDir = session.targetPath.isEmpty()
            ? Environment.getExternalStorageDirectory()
            : new File(session.targetPath);
        if (!targetDir.exists() || !targetDir.isDirectory()) {
            throw new IOException("Target directory does not exist: " + targetDir);
        }
        File dest = new File(targetDir, session.fileName);
        moveFile(session.tempFile, dest);
        Log.i(TAG, "File uploaded: " + dest.getAbsolutePath());
        return dest.getAbsolutePath();
    }

    private String finalizeIconUpload(UploadSession session) throws IOException {
        File iconDir = new File(getExternalCacheDir(), "custom_icons");
        iconDir.mkdirs();
        File dest = new File(iconDir, session.fileName);
        moveFile(session.tempFile, dest);
        Log.i(TAG, "Icon uploaded: " + dest.getAbsolutePath());

        String pkg = session.fileName.contains(".")
            ? session.fileName.substring(0, session.fileName.lastIndexOf('.'))
            : session.fileName;
        syncIconToDocuments(dest, pkg);
        return dest.getAbsolutePath();
    }

    private String finalizeIconReplace(UploadSession session) throws IOException {
        if (session.pkg == null || !isValidPackageName(session.pkg)) {
            throw new IOException("Invalid package name: " + session.pkg);
        }
        File iconDir = new File(getExternalCacheDir(), "custom_icons");
        iconDir.mkdirs();
        File dest = new File(iconDir, session.pkg + ".png");
        moveFile(session.tempFile, dest);
        Log.i(TAG, "Icon replaced for: " + session.pkg);

        syncIconToDocuments(dest, session.pkg);
        return dest.getAbsolutePath();
    }

    private void syncIconToDocuments(File iconFile, String pkg) {
        try {
            File documentsDir = new File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS),
                "E-Ink Launcher/icon");
            documentsDir.mkdirs();
            File dest = new File(documentsDir, pkg + ".png");
            Files.copy(iconFile.toPath(), dest.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            Log.w(TAG, "Failed to sync icon to Documents: " + e.getMessage());
        }
    }

    private String finalizeInstall(UploadSession session) throws IOException {
        File downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        downloadDir.mkdirs();
        File dest = new File(downloadDir, session.fileName);
        moveFile(session.tempFile, dest);
        Log.i(TAG, "APK saved: " + dest.getAbsolutePath());
        return dest.getAbsolutePath();
    }

    private void launchInstaller(File apkFile) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            intent.setDataAndType(
                androidx.core.content.FileProvider.getUriForFile(this, getPackageName() + ".fileProvider", apkFile),
                "application/vnd.android.package-archive");
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        } else {
            intent.setDataAndType(Uri.fromFile(apkFile), "application/vnd.android.package-archive");
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }

    // ==================== Upload Dispatcher ====================

    private void handleChunkedUpload(String path, String fullUri, InputStream is,
        String contentLengthStr, OutputStream os) throws IOException {
        switch (path) {
            case "/api/upload/start":
                handleUploadStart(is, contentLengthStr, os);
                break;
            case "/api/upload/chunk":
                handleUploadChunk(fullUri, is, contentLengthStr, os);
                break;
            case "/api/upload/complete":
                handleUploadComplete(fullUri, os);
                break;
            default:
                sendJsonError(os, "Unknown upload endpoint: " + path);
        }
    }

    // ==================== HTTP Response Helpers ====================

    private byte[] buildHeader(int code, String status, String contentType, long contentLength, String contentDisposition) {
        StringBuilder sb = new StringBuilder();
        sb.append("HTTP/1.1 ").append(code).append(" ").append(status).append("\r\n");
        sb.append("Content-Type: ").append(contentType).append("\r\n");
        sb.append("Content-Length: ").append(contentLength).append("\r\n");
        if (contentDisposition != null) {
            sb.append("Content-Disposition: ").append(contentDisposition).append("\r\n");
        }
        sb.append("Connection: close\r\n");
        sb.append("\r\n");
        return sb.toString().getBytes();
    }

    private void sendResponse(OutputStream os, int code, String status,
        String contentType, String body) throws IOException {
        byte[] bodyBytes = body.getBytes("UTF-8");
        byte[] header = buildHeader(code, status, contentType, bodyBytes.length, null);
        os.write(header);
        os.write(bodyBytes);
        os.flush();
    }

    private void sendBinaryResponse(OutputStream os, int code, String status,
        String contentType, byte[] body) throws IOException {
        byte[] header = buildHeader(code, status, contentType, body.length, null);
        os.write(header);
        os.write(body);
        os.flush();
    }

    private void sendJsonResponse(OutputStream os, String json) throws IOException {
        byte[] body = json.getBytes("UTF-8");
        byte[] header = buildHeader(200, "OK", "application/json; charset=UTF-8", body.length, null);
        os.write(header);
        os.write(body);
        os.flush();
    }

    private void sendJsonOk(OutputStream os) throws IOException {
        sendJsonResponse(os, "{\"success\":true}");
    }

    private void sendJsonError(OutputStream os, String error) throws IOException {
        try {
            JSONObject json = new JSONObject();
            json.put("success", false);
            json.put("error", error);
            sendJsonResponse(os, json.toString());
        } catch (JSONException e) {
            throw new IOException("JSON error", e);
        }
    }

    private String buildErrorPage(int code, String message) {
        return "<!DOCTYPE html><html><head><meta charset=\"UTF-8\"></head><body>"
            + "<h1>" + code + "</h1><p>" + message + "</p>"
            + "<p><a href=\"/\">Back to root</a></p></body></html>";
    }

    // ==================== Notification ====================

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID, "HTTP Server", NotificationManager.IMPORTANCE_LOW);
            channel.setDescription("HTTP file server status");
            NotificationManager nm = getSystemService(NotificationManager.class);
            if (nm != null) nm.createNotificationChannel(channel);
        }
    }

    private void startForegroundWithNotification(String text) {
        Intent stopIntent = new Intent(this, HttpService.class);
        stopIntent.setAction(ACTION_STOP_HTTPSERVER);
        PendingIntent stopPi = PendingIntent.getService(this, 2, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Notification.Builder builder;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder = new Notification.Builder(this, CHANNEL_ID);
        } else {
            builder = new Notification.Builder(this);
        }
        Notification notification = builder
            .setSmallIcon(android.R.drawable.ic_menu_manage)
            .setContentTitle("E-Ink HTTP Server")
            .setContentText(text)
            .setContentIntent(stopPi)
            .setOngoing(true)
            .build();
        startForeground(1, notification);
    }

    // ==================== Utility Methods ====================

    private static void copyStream(InputStream in, OutputStream out) throws IOException {
        byte[] buf = new byte[BUFFER_SIZE];
        int n;
        while ((n = in.read(buf)) != -1) out.write(buf, 0, n);
    }

    private static byte[] readAllBytes(InputStream is) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buf = new byte[BUFFER_SIZE];
        int n;
        while ((n = is.read(buf)) != -1) baos.write(buf, 0, n);
        return baos.toByteArray();
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

    private Bitmap drawableToBitmap(Drawable drawable) {
        if (drawable instanceof BitmapDrawable) {
            return ((BitmapDrawable) drawable).getBitmap();
        }
        int w = drawable.getIntrinsicWidth();
        int h = drawable.getIntrinsicHeight();
        if (w <= 0) w = 96;
        if (h <= 0) h = 96;
        Bitmap bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bmp);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);
        return bmp;
    }

    private String formatSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024));
        return String.format("%.1f GB", bytes / (1024.0 * 1024 * 1024));
    }

    // ==================== Network Detection ====================

    @SuppressWarnings("deprecation")
    public static boolean isConnectedToLocalNetwork(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo ni = cm.getActiveNetworkInfo();
        if (ni != null && ni.isConnected()) {
            int type = ni.getType();
            if (type == ConnectivityManager.TYPE_WIFI || type == ConnectivityManager.TYPE_ETHERNET) return true;
        }
        // Check WiFi hotspot
        try {
            WifiManager wm = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
            Method method = wm.getClass().getDeclaredMethod("isWifiApEnabled");
            if ((Boolean) method.invoke(wm)) return true;
        } catch (Exception ignored) {}
        // Check USB tethering
        try {
            for (NetworkInterface netInterface : Collections.list(NetworkInterface.getNetworkInterfaces())) {
                if (netInterface.getDisplayName().startsWith("rndis")) return true;
            }
        } catch (SocketException ignored) {}
        return false;
    }

    @SuppressWarnings("deprecation")
    public static boolean isConnectedToWifi(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo ni = cm.getActiveNetworkInfo();
        return ni != null && ni.isConnected() && ni.getType() == ConnectivityManager.TYPE_WIFI;
    }

    @SuppressWarnings("deprecation")
    public static InetAddress getLocalInetAddress(Context context) {
        if (!isConnectedToLocalNetwork(context)) return null;
        if (isConnectedToWifi(context)) {
            WifiManager wm = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
            int ipAddress = wm.getConnectionInfo().getIpAddress();
            if (ipAddress != 0) return intToInet(ipAddress);
        }
        try {
            for (NetworkInterface netInterface : Collections.list(NetworkInterface.getNetworkInterfaces())) {
                for (InetAddress address : Collections.list(netInterface.getInetAddresses())) {
                    if (!address.isLoopbackAddress() && !address.isLinkLocalAddress()) return address;
                }
            }
        } catch (Exception ignored) {}
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
        try (ServerSocket ss = new ServerSocket(checkPort);
             DatagramSocket ds = new DatagramSocket(checkPort)) {
            ss.setReuseAddress(true);
            ds.setReuseAddress(true);
            return true;
        } catch (IOException e) {
            Log.d(TAG, "Port " + checkPort + " is not available: " + e.getMessage());
            return false;
        }
    }
}
