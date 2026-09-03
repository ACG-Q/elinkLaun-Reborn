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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PushbackInputStream;
import java.io.RandomAccessFile;
import java.io.BufferedReader;
import java.lang.reflect.Method;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.URLDecoder;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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

    private static volatile int port = DEFAULT_PORT;
    private int pendingPort = -1;
    private ServerSocket serverSocket;
    private ExecutorService threadPool;
    private volatile boolean running = false;
    private static HttpService instance;

    // --- Chunked Upload ---
    private static final int CHUNK_SIZE = 256 * 1024;
    private static final ConcurrentHashMap<String, UploadSession> uploadSessions = new ConcurrentHashMap<>();
    private static final long SESSION_TIMEOUT_MS = 5 * 60 * 1000;
    private static File uploadTempDir;

    // --- Static asset file extensions ---
    private static final Set<String> STATIC_EXTENSIONS = new HashSet<>(Arrays.asList(
        ".js", ".css", ".html", ".htm", ".json", ".xml", ".txt", ".log",
        ".png", ".jpg", ".jpeg", ".gif", ".webp", ".svg", ".ico",
        ".pdf", ".zip", ".mp3", ".mp4", ".apk",
        ".woff", ".woff2", ".ttf", ".map"
    ));

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
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (IOException ignored) {}
        if (threadPool != null) {
            threadPool.shutdownNow();
        }
        stopForeground(true);
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
            threadPool = Executors.newCachedThreadPool();
            running = true;
            startForegroundWithNotification("Listening on port " + port);
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

            PushbackInputStream pis = new PushbackInputStream(is, 8192);
            BufferedReader reader = new BufferedReader(new InputStreamReader(pis, "UTF-8"));
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
            String line;
            while ((line = reader.readLine()) != null && !line.isEmpty()) {
                String lower = line.toLowerCase();
                if (lower.startsWith("content-length:")) {
                    contentLengthStr = line.substring(15).trim();
                }
            }

            File rootDir = Environment.getExternalStorageDirectory();

            if ("GET".equals(method)) {
                handleGet(path, uri, rootDir, os);
            } else if ("POST".equals(method)) {
                handlePost(path, uri, rootDir, pis, contentLengthStr, os);
            } else if ("DELETE".equals(method)) {
                handleDelete(path, uri, rootDir, os);
            } else {
                sendResponse(os, 405, "Method Not Allowed", "text/plain", "405 Method Not Allowed");
            }

            client.close();
        } catch (Exception e) {
            Log.e(TAG, "Handle client error", e);
            try { client.close(); } catch (IOException ignored) {}
        }
    }

    // ==================== GET Routing ====================

    private void handleGet(String path, String fullUri, File rootDir, OutputStream os) throws IOException {
        // --- JSON API ---
        if ("/api/stats".equals(path)) { sendJsonStats(rootDir, os); return; }
        if ("/api/files".equals(path)) { sendJsonFiles(extractQueryParam(fullUri, "path"), rootDir, os); return; }
        if ("/api/apps".equals(path)) { sendJsonApps(os); return; }
        if ("/api/app-icon".equals(path)) { sendImageIcon(extractQueryParam(fullUri, "pkg"), os); return; }
        if ("/api/icons".equals(path)) { sendJsonIcons(os); return; }
        if ("/api/device".equals(path)) { sendJsonDevice(os); return; }
        if ("/api/battery".equals(path)) { sendJsonBattery(os); return; }
        if ("/api/storage".equals(path)) { sendJsonStorage(rootDir, os); return; }
        if ("/api/wifi-status".equals(path)) { sendJsonWifiStatus(os); return; }
        if ("/api/volume".equals(path)) { sendJsonVolume(os); return; }
        if ("/api/brightness".equals(path)) { sendJsonBrightness(os); return; }
        if ("/api/rotation".equals(path)) { sendJsonRotation(os); return; }

        // --- Custom icons ---
        if (path.startsWith("/custom_icons/")) { sendCustomIconFile(path, os); return; }

        // --- Web UI: index.html ---
        if ("/".equals(path) || "/index.html".equals(path)) {
            sendAssetFile("index.html", os);
            return;
        }

        // --- Static assets from Android assets/ directory ---
        // Handles Vite-built flat file structure (e.g. /index-DXHHVp9j.js, /index-BkFr89pq.css)
        if (isStaticAsset(path)) {
            String assetPath = path.startsWith("/") ? path.substring(1) : path;
            if (assetExists(assetPath)) {
                sendAssetFile(assetPath, os);
                return;
            }
        }

        // --- File system: directories ---
        File file = new File(rootDir, path);
        if (!file.exists()) {
            sendResponse(os, 404, "Not Found", "text/html", buildErrorPage(404, "File Not Found"));
            return;
        }
        if (file.isDirectory()) {
            sendJsonFiles(file.getAbsolutePath(), rootDir, os);
            return;
        }

        // --- File system: regular files ---
        sendFile(file, os);
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
        try {
            InputStream is = getAssets().open(assetPath);
            is.close();
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    private void sendAssetFile(String assetPath, OutputStream os) throws IOException {
        InputStream is = null;
        try {
            is = getAssets().open(assetPath);
            String mime = getMimeType(assetPath);
            byte[] fileBytes = readAllBytes(is);
            sendBinaryResponse(os, 200, "OK", mime, fileBytes);
        } catch (IOException e) {
            sendResponse(os, 404, "Not Found", "text/plain", "404 Not Found");
        } finally {
            if (is != null) {
                try { is.close(); } catch (IOException ignored) {}
            }
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

    // ==================== MIME Type ====================

    private static String getMimeType(String path) {
        String lower = path.toLowerCase();
        if (lower.endsWith(".html") || lower.endsWith(".htm")) return "text/html; charset=UTF-8";
        if (lower.endsWith(".css")) return "text/css; charset=UTF-8";
        if (lower.endsWith(".js") || lower.endsWith(".mjs")) return "application/javascript; charset=UTF-8";
        if (lower.endsWith(".json")) return "application/json; charset=UTF-8";
        if (lower.endsWith(".xml")) return "text/xml; charset=UTF-8";
        if (lower.endsWith(".txt") || lower.endsWith(".log")) return "text/plain; charset=UTF-8";
        if (lower.endsWith(".png")) return "image/png";
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) return "image/jpeg";
        if (lower.endsWith(".gif")) return "image/gif";
        if (lower.endsWith(".webp")) return "image/webp";
        if (lower.endsWith(".svg")) return "image/svg+xml";
        if (lower.endsWith(".ico")) return "image/x-icon";
        if (lower.endsWith(".pdf")) return "application/pdf";
        if (lower.endsWith(".zip")) return "application/zip";
        if (lower.endsWith(".mp3")) return "audio/mpeg";
        if (lower.endsWith(".mp4")) return "video/mp4";
        if (lower.endsWith(".apk")) return "application/vnd.android.package-archive";
        if (lower.endsWith(".woff2")) return "font/woff2";
        if (lower.endsWith(".woff")) return "font/woff";
        if (lower.endsWith(".ttf")) return "font/ttf";
        if (lower.endsWith(".map")) return "application/json";
        return "application/octet-stream";
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
            throw new IOException("JSON error", e);
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
            throw new IOException("JSON error", e);
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
            throw new IOException("JSON error", e);
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

            for (ResolveInfo ri : list) {
                if ("io.github.reborn.einklauncher.Launcher".equals(ri.activityInfo.name)) continue;
                String pkg = ri.activityInfo.packageName;
                JSONObject item = new JSONObject();
                item.put("name", ri.loadLabel(getPackageManager()).toString());
                item.put("packageName", pkg);
                item.put("hasCustomIcon", new File(iconDir, pkg + ".png").exists());
                arr.put(item);
            }

            String[] virtualPkgs = {"E-ink_Launcher.Lock", "E-ink_Launcher.WiFi", "E-ink_Launcher.WiFiOff", "E-ink_Launcher.HttpServer"};
            String[] virtualNames = {"OneKey Lock", "WiFi Control", "WiFi Off", "HTTP Server"};
            for (int i = 0; i < virtualPkgs.length; i++) {
                JSONObject item = new JSONObject();
                item.put("name", virtualNames[i]);
                item.put("packageName", virtualPkgs[i]);
                item.put("isVirtual", true);
                item.put("hasCustomIcon", new File(iconDir, virtualPkgs[i] + ".png").exists());
                arr.put(item);
            }

            json.put("items", arr);
            sendJsonResponse(os, json.toString());
        } catch (JSONException e) {
            throw new IOException("JSON error", e);
        }
    }

    private void sendImageIcon(String pkg, OutputStream os) throws IOException {
        if (pkg == null || pkg.isEmpty()) {
            sendEmptyIcon(os);
            return;
        }
        try {
            Bitmap icon = null;
            if ("E-ink_Launcher.Lock".equals(pkg)) {
                icon = BitmapFactory.decodeResource(getResources(), R.drawable.ic_onekeylock);
            } else if ("E-ink_Launcher.WiFi".equals(pkg)) {
                icon = BitmapFactory.decodeResource(getResources(), R.drawable.wifi_on);
            } else if ("E-ink_Launcher.HttpServer".equals(pkg)) {
                icon = BitmapFactory.decodeResource(getResources(), R.drawable.http_server);
            } else if ("E-ink_Launcher.WiFiOff".equals(pkg)) {
                icon = BitmapFactory.decodeResource(getResources(), R.drawable.wifi_off);
            } else {
                Drawable d = getPackageManager().getApplicationIcon(pkg);
                icon = drawableToBitmap(d);
            }
            if (icon != null) {
                Bitmap scaled = Bitmap.createScaledBitmap(icon, 96, 96, true);
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                scaled.compress(Bitmap.CompressFormat.PNG, 100, baos);
                byte[] body = baos.toByteArray();
                sendBinaryResponse(os, 200, "OK", "image/png", body);
            } else {
                sendEmptyIcon(os);
            }
        } catch (Exception e) {
            sendEmptyIcon(os);
        }
    }

    private void sendEmptyIcon(OutputStream os) throws IOException {
        Bitmap bmp = Bitmap.createBitmap(96, 96, Bitmap.Config.ARGB_8888);
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
            sendResponse(os, 403, "Forbidden", "text/plain", "403 Forbidden");
            return;
        }
        File iconFile = new File(getExternalCacheDir(), "custom_icons/" + fileName);
        if (!iconFile.exists()) {
            sendResponse(os, 404, "Not Found", "text/plain", "404 Not Found");
            return;
        }
        String mime = "image/png";
        if (fileName.endsWith(".jpg") || fileName.endsWith(".jpeg")) mime = "image/jpeg";
        else if (fileName.endsWith(".webp")) mime = "image/webp";

        byte[] header = buildHeader(200, "OK", mime, iconFile.length(), null);
        // Add cache header
        String cacheHeader = "Cache-Control: max-age=86400\r\n";
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
            throw new IOException("JSON error", e);
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
            }
            sendJsonResponse(os, json.toString());
        } catch (JSONException e) {
            throw new IOException("JSON error", e);
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
            throw new IOException("JSON error", e);
        }
    }

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
                            json.put("ssid", info.getSSID());
                            json.put("bssid", info.getBSSID());
                            json.put("rssi", info.getRssi());
                            json.put("linkSpeed", info.getLinkSpeed());
                            json.put("networkId", info.getNetworkId());
                        }
                    } catch (Exception ignored) {}
                }
            }
            sendJsonResponse(os, json.toString());
        } catch (JSONException e) {
            throw new IOException("JSON error", e);
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
            }
            sendJsonResponse(os, json.toString());
        } catch (JSONException e) {
            throw new IOException("JSON error", e);
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
            JSONObject json = new JSONObject();
            json.put("value", brightness);
            sendJsonResponse(os, json.toString());
        } catch (Exception e) {
            sendJsonError(os, "Cannot read brightness");
        }
    }

    private void sendJsonRotation(OutputStream os) throws IOException {
        try {
            int rotation = Settings.System.getInt(getContentResolver(), Settings.System.ACCELEROMETER_ROTATION, 0);
            JSONObject json = new JSONObject();
            json.put("enabled", rotation == 1);
            sendJsonResponse(os, json.toString());
        } catch (Exception e) {
            sendJsonError(os, "Cannot read rotation");
        }
    }

    // ==================== POST Routing ====================

    private void handlePost(String path, String fullUri, File rootDir, PushbackInputStream pis,
        String contentLengthStr, OutputStream os) throws IOException {
        if (path.startsWith("/api/upload")) {
            handleChunkedUpload(path, fullUri, pis, contentLengthStr, os);
            return;
        }
        if (path.startsWith("/api/volume")) {
            handleSetVolume(fullUri, os);
            return;
        }
        if (path.startsWith("/api/brightness")) {
            handleSetBrightness(fullUri, os);
            return;
        }
        if (path.startsWith("/api/rotation")) {
            handleSetRotation(fullUri, os);
            return;
        }
        if (path.startsWith("/api/icons/upload") || path.startsWith("/api/icons/replace")) {
            handleChunkedUpload("/api/upload/start", fullUri, pis, contentLengthStr, os);
            return;
        }
        if (path.startsWith("/api/icons/assign")) {
            handleIconAssign(fullUri, os);
            return;
        }
        if (path.startsWith("/api/open-settings")) {
            handleOpenSettings(fullUri, os);
            return;
        }
        if (path.startsWith("/api/app-install")) {
            handleAppInstall(fullUri, os);
            return;
        }
        if (path.startsWith("/api/app-uninstall")) {
            handleAppUninstall(fullUri, os);
            return;
        }
        if (path.startsWith("/api/app-open")) {
            handleAppOpen(fullUri, os);
            return;
        }
        sendResponse(os, 404, "Not Found", "application/json", "{\"error\":\"Unknown POST endpoint\"}");
    }

    private void handleSetVolume(String fullUri, OutputStream os) throws IOException {
        try {
            String streamName = extractQueryParam(fullUri, "stream");
            String valueStr = extractQueryParam(fullUri, "value");
            AudioManager am = (AudioManager) getSystemService(AUDIO_SERVICE);
            if (am == null || streamName == null || valueStr == null) {
                sendJsonError(os, "Missing parameters");
                return;
            }
            int streamType;
            switch (streamName) {
                case "music": streamType = AudioManager.STREAM_MUSIC; break;
                case "ring": streamType = AudioManager.STREAM_RING; break;
                case "notification": streamType = AudioManager.STREAM_NOTIFICATION; break;
                case "alarm": streamType = AudioManager.STREAM_ALARM; break;
                default: sendJsonError(os, "Invalid stream"); return;
            }
            int value = Integer.parseInt(valueStr);
            am.setStreamVolume(streamType, value, 0);
            sendJsonOk(os);
        } catch (Exception e) {
            sendJsonError(os, "Volume error");
        }
    }

    private void handleSetBrightness(String fullUri, OutputStream os) throws IOException {
        try {
            String brightnessStr = extractQueryParam(fullUri, "value");
            if (brightnessStr == null) {
                sendJsonError(os, "Missing value");
                return;
            }
            int value = Integer.parseInt(brightnessStr);
            Settings.System.putInt(getContentResolver(), Settings.System.SCREEN_BRIGHTNESS, value);
            sendJsonOk(os);
        } catch (Exception e) {
            sendJsonError(os, "Brightness error");
        }
    }

    private void handleSetRotation(String fullUri, OutputStream os) throws IOException {
        try {
            String enabledStr = extractQueryParam(fullUri, "enabled");
            if (enabledStr == null) {
                sendJsonError(os, "Missing enabled");
                return;
            }
            int val = "true".equals(enabledStr) ? 1 : 0;
            Settings.System.putInt(getContentResolver(), Settings.System.ACCELEROMETER_ROTATION, val);
            sendJsonOk(os);
        } catch (Exception e) {
            sendJsonError(os, "Rotation error");
        }
    }

    private void handleIconAssign(String fullUri, OutputStream os) throws IOException {
        try {
            String slot = extractQueryParam(fullUri, "slot");
            String icon = extractQueryParam(fullUri, "icon");
            if (slot == null || icon == null) {
                sendJsonError(os, "Missing slot or icon");
                return;
            }
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
            prefs.edit().putString("icon_" + slot, icon).apply();
            sendJsonOk(os);
        } catch (Exception e) {
            sendJsonError(os, "Assign error");
        }
    }

    private void handleOpenSettings(String fullUri, OutputStream os) throws IOException {
        try {
            String action = extractQueryParam(fullUri, "action");
            if (action != null && !action.isEmpty()) {
                Intent intent = new Intent(action);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                sendJsonOk(os);
            } else {
                sendJsonError(os, "Missing action");
            }
        } catch (Exception e) {
            sendJsonError(os, e.getMessage());
        }
    }

    private void handleAppInstall(String fullUri, OutputStream os) throws IOException {
        try {
            String targetPath = extractQueryParam(fullUri, "path");
            if (targetPath == null || targetPath.isEmpty()) {
                sendJsonError(os, "Missing path");
                return;
            }
            File apkFile = new File(targetPath);
            if (!apkFile.exists() || !apkFile.getName().toLowerCase().endsWith(".apk")) {
                sendJsonError(os, "Invalid APK file");
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
            startActivity(intent);
            sendJsonOk(os);
        } catch (Exception e) {
            sendJsonError(os, e.getMessage());
        }
    }

    private void handleAppUninstall(String fullUri, OutputStream os) throws IOException {
        try {
            String pkg = extractQueryParam(fullUri, "pkg");
            if (pkg == null || pkg.isEmpty()) {
                sendJsonError(os, "Missing pkg");
                return;
            }
            Intent intent = new Intent(Intent.ACTION_DELETE, Uri.parse("package:" + pkg));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            sendJsonOk(os);
        } catch (Exception e) {
            sendJsonError(os, e.getMessage());
        }
    }

    private void handleAppOpen(String fullUri, OutputStream os) throws IOException {
        try {
            String pkg = extractQueryParam(fullUri, "pkg");
            if (pkg == null || pkg.isEmpty()) {
                sendJsonError(os, "Missing pkg");
                return;
            }
            Intent launchIntent = getPackageManager().getLaunchIntentForPackage(pkg);
            if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(launchIntent);
                sendJsonOk(os);
            } else {
                sendJsonError(os, "No launch intent");
            }
        } catch (Exception e) {
            sendJsonError(os, e.getMessage());
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
                File file = new File(filePath);
                boolean deleted = file.exists() && file.delete();
                sendJsonResponse(os, new JSONObject().put("success", deleted).toString());
            } catch (JSONException e) {
                throw new IOException("JSON error", e);
            }
            return;
        }
    }

    // ==================== Chunked Upload ====================

    private static class UploadSession {
        String sessionId;
        File tempFile;
        String action;
        String targetPath;
        String pkg;
        String fileName;
        long totalSize;
        int totalChunks;
        boolean[] confirmed;
        long createdAt;
    }

    private File getUploadTempDir() {
        if (uploadTempDir == null) {
            uploadTempDir = new File(getExternalCacheDir(), "uploads");
            if (!uploadTempDir.exists()) uploadTempDir.mkdirs();
        }
        return uploadTempDir;
    }

    private void startCleanupTimer() {
        new Thread(() -> {
            while (running) {
                try {
                    Thread.sleep(2 * 60 * 1000);
                } catch (InterruptedException e) {
                    break;
                }
                long now = System.currentTimeMillis();
                for (String sid : uploadSessions.keySet().toArray(new String[0])) {
                    UploadSession s = uploadSessions.get(sid);
                    if (s != null && now - s.createdAt > SESSION_TIMEOUT_MS) {
                        removeSession(sid);
                    }
                }
            }
        }, "upload-cleanup").start();
    }

    private void removeSession(String sessionId) {
        UploadSession session = uploadSessions.remove(sessionId);
        if (session != null && session.tempFile.exists()) {
            session.tempFile.delete();
        }
    }

    private void handleChunkedUpload(String path, String fullUri, PushbackInputStream pis,
        String contentLengthStr, OutputStream os) throws IOException {
        if ("/api/upload/start".equals(path)) {
            int contentLength = 0;
            if (contentLengthStr != null) {
                try { contentLength = Integer.parseInt(contentLengthStr); } catch (NumberFormatException ignored) {}
            }
            byte[] bodyBytes = readFully(pis, contentLength > 0 ? contentLength : 4096);
            String body = new String(bodyBytes, "UTF-8");
            handleUploadStart(body, os);
            return;
        }

        int qIdx = fullUri.indexOf('?');
        String queryStr = qIdx >= 0 ? fullUri.substring(qIdx + 1) : "";

        if ("/api/upload/chunk".equals(path)) {
            handleUploadChunk(queryStr, pis, os);
            return;
        }

        if ("/api/upload/complete".equals(path)) {
            handleUploadComplete(queryStr, os);
            return;
        }

        sendJsonError(os, "Unknown upload endpoint");
    }

    private void handleUploadStart(String body, OutputStream os) throws IOException {
        try {
            JSONObject req = new JSONObject(body);
            String filename = req.optString("filename", "");
            long size = req.optLong("size", 0);
            String action = req.optString("action", "file");
            String targetPath = req.optString("targetPath", "");
            String pkg = req.optString("pkg", "");

            if (filename.isEmpty() || size <= 0) {
                sendJsonError(os, "Missing filename or size");
                return;
            }

            String sessionId = UUID.randomUUID().toString();
            int totalChunks = (int) Math.ceil((double) size / CHUNK_SIZE);
            File tempFile = new File(getUploadTempDir(), sessionId + ".part");

            UploadSession session = new UploadSession();
            session.sessionId = sessionId;
            session.tempFile = tempFile;
            session.action = action;
            session.targetPath = targetPath;
            session.pkg = pkg;
            session.fileName = filename;
            session.totalSize = size;
            session.totalChunks = totalChunks;
            session.confirmed = new boolean[totalChunks];
            session.createdAt = System.currentTimeMillis();

            uploadSessions.put(sessionId, session);

            JSONObject resp = new JSONObject();
            resp.put("success", true);
            resp.put("sessionId", sessionId);
            resp.put("totalChunks", totalChunks);
            resp.put("chunkSize", CHUNK_SIZE);
            sendJsonResponse(os, resp.toString());
        } catch (JSONException e) {
            sendJsonError(os, "Invalid JSON");
        }
    }

    private void handleUploadChunk(String queryStr, InputStream is, OutputStream os) throws IOException {
        String sessionId = extractQueryParam(queryStr, "sessionId");
        String chunkIndexStr = extractQueryParam(queryStr, "chunkIndex");

        if (sessionId == null) {
            sendJsonError(os, "Missing sessionId");
            return;
        }

        UploadSession session = uploadSessions.get(sessionId);
        if (session == null) {
            sendJsonError(os, "Session not found");
            return;
        }

        int chunkIndex;
        if (chunkIndexStr != null) {
            try { chunkIndex = Integer.parseInt(chunkIndexStr); } catch (NumberFormatException e) {
                sendJsonError(os, "Invalid chunkIndex");
                return;
            }
        } else {
            sendJsonError(os, "Missing chunkIndex");
            return;
        }

        if (chunkIndex < 0 || chunkIndex >= session.totalChunks) {
            sendJsonError(os, "Chunk index out of range");
            return;
        }

        if (session.confirmed[chunkIndex]) {
            long offset = 0;
            for (int i = 0; i < chunkIndex; i++) {
                if (session.confirmed[i]) offset += CHUNK_SIZE;
            }
            try {
                JSONObject resp = new JSONObject();
                resp.put("success", true);
                resp.put("offset", offset);
                sendJsonResponse(os, resp.toString());
            } catch (JSONException e) {
                sendJsonError(os, "Response error");
            }
            return;
        }

        long expectedSize = CHUNK_SIZE;
        if (chunkIndex == session.totalChunks - 1) {
            expectedSize = session.totalSize - (long) chunkIndex * CHUNK_SIZE;
        }

        RandomAccessFile raf = new RandomAccessFile(session.tempFile, "rw");
        raf.seek((long) chunkIndex * CHUNK_SIZE);

        byte[] buf = new byte[8192];
        long totalRead = 0;
        while (totalRead < expectedSize) {
            int toRead = (int) Math.min(buf.length, expectedSize - totalRead);
            int read = is.read(buf, 0, toRead);
            if (read < 0) break;
            raf.write(buf, 0, read);
            totalRead += read;
        }
        raf.close();

        if (totalRead < expectedSize) {
            sendJsonError(os, "Incomplete chunk: expected " + expectedSize + ", got " + totalRead);
            return;
        }

        session.confirmed[chunkIndex] = true;

        long offset = 0;
        for (int i = 0; i < chunkIndex; i++) {
            if (session.confirmed[i]) offset += CHUNK_SIZE;
        }
        offset += totalRead;

        try {
            JSONObject resp = new JSONObject();
            resp.put("success", true);
            resp.put("offset", offset);
            sendJsonResponse(os, resp.toString());
        } catch (JSONException e) {
            sendJsonError(os, "Response error");
        }
    }

    private void handleUploadComplete(String queryStr, OutputStream os) throws IOException {
        String sessionId = extractQueryParam(queryStr, "sessionId");
        if (sessionId == null) {
            sendJsonError(os, "Missing sessionId");
            return;
        }

        UploadSession session = uploadSessions.get(sessionId);
        if (session == null) {
            sendJsonError(os, "Session not found");
            return;
        }

        for (boolean b : session.confirmed) {
            if (!b) {
                sendJsonError(os, "Upload incomplete");
                return;
            }
        }

        if (session.tempFile.length() != session.totalSize) {
            sendJsonError(os, "File size mismatch");
            return;
        }

        String resultPath = null;

        try {
            if ("file".equals(session.action)) {
                File targetDir = session.targetPath.isEmpty() ? Environment.getExternalStorageDirectory() : new File(session.targetPath);
                if (!targetDir.exists() || !targetDir.isDirectory()) {
                    sendJsonError(os, "Target directory invalid");
                    return;
                }
                File dest = new File(targetDir, session.fileName);
                session.tempFile.renameTo(dest);
                resultPath = dest.getAbsolutePath();
            } else if ("icon-upload".equals(session.action)) {
                File iconDir = new File(getExternalCacheDir(), "custom_icons");
                if (!iconDir.exists()) iconDir.mkdirs();
                File dest = new File(iconDir, session.fileName);
                session.tempFile.renameTo(dest);
                resultPath = dest.getAbsolutePath();
            } else if ("icon-replace".equals(session.action)) {
                if (session.pkg == null || session.pkg.isEmpty()) {
                    sendJsonError(os, "Missing pkg");
                    return;
                }
                File iconDir = new File(getExternalCacheDir(), "custom_icons");
                if (!iconDir.exists()) iconDir.mkdirs();
                File dest = new File(iconDir, session.pkg + ".png");
                session.tempFile.renameTo(dest);
                resultPath = dest.getAbsolutePath();

                File documentsIconDir = new File(
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS),
                    "E-Ink Launcher/icon");
                if (!documentsIconDir.exists()) documentsIconDir.mkdirs();
                File documentsIcon = new File(documentsIconDir, session.pkg + ".png");
                try (FileInputStream fis = new FileInputStream(dest);
                     FileOutputStream fos2 = new FileOutputStream(documentsIcon)) {
                    copyStream(fis, fos2);
                }
            } else if ("install".equals(session.action)) {
                File downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
                if (!downloadDir.exists()) downloadDir.mkdirs();
                File dest = new File(downloadDir, session.fileName);
                session.tempFile.renameTo(dest);
                resultPath = dest.getAbsolutePath();

                Intent intent = new Intent(Intent.ACTION_VIEW);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    intent.setDataAndType(
                        androidx.core.content.FileProvider.getUriForFile(HttpService.this, getPackageName() + ".fileProvider", dest),
                        "application/vnd.android.package-archive");
                    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                } else {
                    intent.setDataAndType(Uri.fromFile(dest), "application/vnd.android.package-archive");
                }
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
            }
        } catch (Exception e) {
            sendJsonError(os, e.getMessage());
            return;
        } finally {
            removeSession(sessionId);
        }

        try {
            JSONObject resp = new JSONObject();
            resp.put("success", true);
            if (resultPath != null) resp.put("path", resultPath);
            sendJsonResponse(os, resp.toString());
        } catch (JSONException e) {
            sendJsonError(os, "Response error");
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

    private static final String CHANNEL_ID = "http_server";

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
        byte[] buf = new byte[8192];
        int n;
        while ((n = in.read(buf)) != -1) out.write(buf, 0, n);
    }

    private static byte[] readAllBytes(InputStream is) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buf = new byte[8192];
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

    private byte[] readFully(PushbackInputStream pis, int length) throws IOException {
        byte[] data = new byte[length];
        int offset = 0;
        while (offset < length) {
            int read = pis.read(data, offset, length - offset);
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
