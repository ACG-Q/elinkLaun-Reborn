package io.github.reborn.einklauncher;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Environment;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import io.github.reborn.einklauncher.R;
import io.github.reborn.einklauncher.Utils;

public class ApkManager extends Activity {

  private ListView lvApk;
  private Button btnScan;
  private Button btnBatchUninstall;
  private TextView tvInfo;

  private final List<ApkItem> apkList = new ArrayList<>();
  private ApkAdapter adapter;
  private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());

  private static class ApkItem {
    File file;
    String packageName;
    String versionName;
    int versionCode;
    boolean selected;
  }

  @Override
  protected void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    initViews();
    scanApks();
  }

  private void initViews() {
    LinearLayout root = new LinearLayout(this);
    root.setOrientation(LinearLayout.VERTICAL);
    root.setBackgroundColor(android.graphics.Color.WHITE);

    int pad = Utils.dp2Px(this, 10);
    root.setPadding(pad, pad, pad, pad);

    TextView title = new TextView(this);
    title.setText(getString(R.string.apk_manager));
    title.setTextSize(20);
    title.setTypeface(null, android.graphics.Typeface.BOLD);
    title.setTextColor(android.graphics.Color.BLACK);
    root.addView(title);
    root.addView(makeDivider());

    btnScan = new Button(this);
    btnScan.setText(getString(R.string.apk_manager_scan));
    root.addView(btnScan, new LinearLayout.LayoutParams(-1, Utils.dp2Px(this, 44)));
    root.addView(makeDivider());

    btnBatchUninstall = new Button(this);
    btnBatchUninstall.setText(getString(R.string.apk_manager_batch_uninstall));
    btnBatchUninstall.setEnabled(false);
    root.addView(btnBatchUninstall, new LinearLayout.LayoutParams(-1, Utils.dp2Px(this, 44)));
    root.addView(makeDivider());

    tvInfo = new TextView(this);
    tvInfo.setTextColor(android.graphics.Color.DKGRAY);
    tvInfo.setTextSize(14);
    root.addView(tvInfo);

    lvApk = new ListView(this);
    lvApk.setDivider(new android.graphics.drawable.ColorDrawable(android.graphics.Color.BLACK));
    lvApk.setDividerHeight(1);
    root.addView(lvApk, new LinearLayout.LayoutParams(-1, 0, 1));

    Button btnBack = new Button(this);
    btnBack.setText(getString(R.string.exit));
    root.addView(btnBack, new LinearLayout.LayoutParams(-1, Utils.dp2Px(this, 44)));

    setContentView(root);

    btnScan.setOnClickListener(v -> scanApks());
    btnBatchUninstall.setOnClickListener(v -> batchUninstall());
    btnBack.setOnClickListener(v -> finish());
  }

  private void scanApks() {
    apkList.clear();
    String[] dirs = {
        Environment.getExternalStorageDirectory().getAbsolutePath() + "/Download",
        Environment.getExternalStorageDirectory().getAbsolutePath() + "/Downloads",
        "/sdcard/Download"
    };
    PackageManager pm = getPackageManager();
    for (String dirPath : dirs) {
      File dir = new File(dirPath);
      if (!dir.exists() || !dir.isDirectory()) continue;
      File[] files = dir.listFiles();
      if (files == null) continue;
      for (File f : files) {
        if (!f.getName().toLowerCase().endsWith(".apk")) continue;
        PackageInfo pkgInfo = getPackageInfo(pm, f.getAbsolutePath());
        ApkItem item = new ApkItem();
        item.file = f;
        item.versionName = pkgInfo != null ? pkgInfo.versionName : "?";
        item.versionCode = pkgInfo != null ? pkgInfo.versionCode : 0;
        item.packageName = pkgInfo != null ? pkgInfo.packageName : "?";
        apkList.add(item);
      }
    }
    adapter = new ApkAdapter(this, apkList);
    lvApk.setAdapter(adapter);
    updateInfo();
  }

  private PackageInfo getPackageInfo(PackageManager pm, String apkPath) {
    try {
      return pm.getPackageArchiveInfo(apkPath, 0);
    } catch (Exception e) {
      return null;
    }
  }

  private void updateInfo() {
    int count = apkList.size();
    int selected = 0;
    for (ApkItem item : apkList) {
      if (item.selected) selected++;
    }
    tvInfo.setText(count + " APK(s) found, " + selected + " selected");
    btnBatchUninstall.setEnabled(selected > 0);
  }

  private void batchUninstall() {
    List<ApkItem> toUninstall = new ArrayList<>();
    for (ApkItem item : apkList) {
      if (item.selected) toUninstall.add(item);
    }
    if (toUninstall.isEmpty()) return;

    new AlertDialog.Builder(this)
        .setTitle("Uninstall " + toUninstall.size() + " app(s)?")
        .setPositiveButton(getString(R.string.dialog_confim), (dialog, which) -> {
          for (ApkItem item : toUninstall) {
            Intent intent = new Intent(Intent.ACTION_DELETE,
                android.net.Uri.parse("package:" + item.packageName));
            startActivity(intent);
          }
          scanApks();
        })
        .setNegativeButton(getString(R.string.dialog_cancel), null)
        .show();
  }

  private View makeDivider() {
    View d = new View(this);
    d.setBackgroundColor(android.graphics.Color.BLACK);
    LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, 1);
    lp.topMargin = Utils.dp2Px(this, 4);
    lp.bottomMargin = Utils.dp2Px(this, 4);
    d.setLayoutParams(lp);
    return d;
  }

  private static class ApkAdapter extends BaseAdapter {
    private final Context ctx;
    private final List<ApkItem> items;

    ApkAdapter(Context ctx, List<ApkItem> items) {
      this.ctx = ctx;
      this.items = items;
    }

    @Override
    public int getCount() {
      return items.size();
    }

    @Override
    public Object getItem(int position) {
      return items.get(position);
    }

    @Override
    public long getItemId(int position) {
      return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
      ViewHolder vh;
      if (convertView == null) {
        convertView = LayoutInflater.from(ctx).inflate(R.layout.apk_item, parent, false);
        vh = new ViewHolder();
        vh.icon = convertView.findViewById(R.id.apkIcon);
        vh.name = convertView.findViewById(R.id.apkName);
        vh.detail = convertView.findViewById(R.id.apkDetail);
        vh.cbSelect = convertView.findViewById(R.id.apkSelect);
        vh.btnAction = convertView.findViewById(R.id.apkAction);
        convertView.setTag(vh);
      } else {
        vh = (ViewHolder) convertView.getTag();
      }

      ApkItem item = items.get(position);
      vh.icon.setImageResource(R.drawable.ic_apk_box);
      vh.name.setText(item.file.getName());
      String detail = "Pkg: " + item.packageName + " | v" + item.versionName;
      if (item.packageName.equals("?")) {
        detail = ctx.getString(R.string.type_apk) + " | " + formatSize(item.file.length());
      }
      vh.detail.setText(detail);
      vh.cbSelect.setChecked(item.selected);
      vh.cbSelect.setOnCheckedChangeListener((buttonView, isChecked) -> {
        item.selected = isChecked;
        ((ApkManager) ctx).updateInfo();
      });
      vh.btnAction.setText(ctx.getString(R.string.apk_manager_install));
      vh.btnAction.setOnClickListener(v -> {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        android.net.Uri uri;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
          uri = androidx.core.content.FileProvider.getUriForFile(
              ctx, ctx.getPackageName() + ".fileProvider", item.file);
        } else {
          uri = android.net.Uri.fromFile(item.file);
        }
        intent.setDataAndType(uri, "application/vnd.android.package-archive");
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        ctx.startActivity(intent);
      });
      return convertView;
    }

    private String formatSize(long bytes) {
      if (bytes < 1024) return bytes + " B";
      if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
      return String.format("%.1f MB", bytes / (1024.0 * 1024));
    }

    static class ViewHolder {
      ImageView icon;
      TextView name;
      TextView detail;
      CheckBox cbSelect;
      Button btnAction;
    }
  }
}
