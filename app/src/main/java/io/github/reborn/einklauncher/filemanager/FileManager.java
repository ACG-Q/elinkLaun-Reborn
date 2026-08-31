package io.github.reborn.einklauncher.filemanager;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;

import java.io.File;
import java.util.Arrays;
import java.util.Comparator;

import io.github.reborn.einklauncher.R;
import io.github.reborn.einklauncher.Utils;

public class FileManager extends Activity {

  private ListView lvFiles;
  private TextView tvPath;
  private TextView btnParent;
  private TextView btnExit;

  private File currentDir;
  private File[] files;

  private static final String KEY_DEFAULT_PATH = "file_manager_default_path";
  private static final String DEFAULT_PATH = Environment.getExternalStorageDirectory().getAbsolutePath();

  @Override
  protected void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    initViews();
    loadDefaultPath();
  }

  private void initViews() {
    LinearLayout root = new LinearLayout(this);
    root.setOrientation(LinearLayout.VERTICAL);
    root.setBackgroundColor(android.graphics.Color.WHITE);

    int pad = Utils.dp2Px(this, 10);
    root.setPadding(pad, pad, pad, pad);

    root.addView(makeDivider());

    tvPath = new TextView(this);
    tvPath.setTextColor(android.graphics.Color.BLACK);
    tvPath.setTextSize(18);
    tvPath.setSingleLine(true);
    tvPath.setEllipsize(TextUtils.TruncateAt.START);
    root.addView(tvPath);

    root.addView(makeDivider());

    lvFiles = new ListView(this);
    lvFiles.setDivider(new android.graphics.drawable.ColorDrawable(android.graphics.Color.BLACK));
    lvFiles.setDividerHeight(1);
    root.addView(lvFiles, new LinearLayout.LayoutParams(-1, 0, 1));

    root.addView(makeDivider());

    LinearLayout btnRow = new LinearLayout(this);
    btnRow.setOrientation(LinearLayout.HORIZONTAL);

    btnParent = new TextView(this);
    btnParent.setText(getString(R.string.parent_folder));
    btnParent.setTextColor(android.graphics.Color.BLACK);
    btnParent.setTextSize(18);
    btnParent.setGravity(Gravity.CENTER);
    btnParent.setPadding(pad, pad, pad, pad);
    btnParent.setBackgroundResource(android.R.drawable.btn_default);
    LinearLayout.LayoutParams lp1 = new LinearLayout.LayoutParams(0, -2, 1);
    lp1.rightMargin = pad / 2;
    btnRow.addView(btnParent, lp1);

    btnExit = new TextView(this);
    btnExit.setText(getString(R.string.exit));
    btnExit.setTextColor(android.graphics.Color.BLACK);
    btnExit.setTextSize(18);
    btnExit.setGravity(Gravity.CENTER);
    btnExit.setPadding(pad, pad, pad, pad);
    btnExit.setBackgroundResource(android.R.drawable.btn_default);
    LinearLayout.LayoutParams lp2 = new LinearLayout.LayoutParams(0, -2, 1);
    lp2.leftMargin = pad / 2;
    btnRow.addView(btnExit, lp2);

    root.addView(btnRow);

    setContentView(root);

    btnParent.setOnClickListener(v -> goParent());
    btnExit.setOnClickListener(v -> finish());
    lvFiles.setOnItemClickListener((parent, view, position, id) -> onItemClick(position));
  }

  private void loadDefaultPath() {
    String path = PreferenceManager.getDefaultSharedPreferences(this)
        .getString(KEY_DEFAULT_PATH, DEFAULT_PATH);
    File dir = new File(path);
    if (!dir.exists() || !dir.isDirectory()) {
      dir = new File(DEFAULT_PATH);
    }
    openDir(dir);
  }

  private void openDir(File dir) {
    if (!dir.canRead() || !dir.isDirectory()) {
      Toast.makeText(this, getString(R.string.file_manager_error_access), Toast.LENGTH_SHORT).show();
      return;
    }
    currentDir = dir;
    files = dir.listFiles();
    if (files == null) files = new File[0];
    Arrays.sort(files, (a, b) -> {
      if (a.isDirectory() && !b.isDirectory()) return -1;
      if (!a.isDirectory() && b.isDirectory()) return 1;
      return a.getName().compareToIgnoreCase(b.getName());
    });
    tvPath.setText(dir.getAbsolutePath());
    btnParent.setVisibility(dir.getParent() == null ? View.GONE : View.VISIBLE);
    lvFiles.setAdapter(new FileAdapter(this, files));
  }

  private void goParent() {
    if (currentDir == null || currentDir.getParent() == null) return;
    openDir(currentDir.getParentFile());
  }

  private void onItemClick(int position) {
    if (position < 0 || position >= files.length) return;
    File file = files[position];
    if (file.isDirectory()) {
      openDir(file);
    } else {
      openFile(file);
    }
  }

  private void openFile(File file) {
    Uri uri;
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
      uri = FileProvider.getUriForFile(this, getPackageName() + ".fileProvider", file);
    } else {
      uri = Uri.fromFile(file);
    }
    String mime = getMimeType(file);
    Intent intent = new Intent(Intent.ACTION_VIEW);
    intent.setDataAndType(uri, mime);
    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
    try {
      startActivity(intent);
    } catch (Exception e) {
      Toast.makeText(this, getString(R.string.file_manager_no_app), Toast.LENGTH_SHORT).show();
    }
  }

  private String getMimeType(File file) {
    String name = file.getName().toLowerCase();
    if (name.endsWith(".apk")) return "application/vnd.android.package-archive";
    if (name.endsWith(".pdf")) return "application/pdf";
    if (name.endsWith(".png") || name.endsWith(".jpg") || name.endsWith(".jpeg"))
      return "image/*";
    if (name.endsWith(".mp3") || name.endsWith(".wav") || name.endsWith(".flac"))
      return "audio/*";
    if (name.endsWith(".mp4") || name.endsWith(".avi") || name.endsWith(".mkv"))
      return "video/*";
    if (name.endsWith(".zip") || name.endsWith(".rar") || name.endsWith(".7z"))
      return "application/zip";
    if (name.endsWith(".doc") || name.endsWith(".docx")) return "application/msword";
    if (name.endsWith(".xls") || name.endsWith(".xlsx")) return "application/vnd.ms-excel";
    if (name.endsWith(".ppt") || name.endsWith(".pptx")) return "application/vnd.ms-powerpoint";
    return "*/*";
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

  private static class FileAdapter extends BaseAdapter {
    private final Context ctx;
    private final File[] files;

    FileAdapter(Context ctx, File[] files) {
      this.ctx = ctx;
      this.files = files;
    }

    @Override
    public int getCount() {
      return files.length;
    }

    @Override
    public Object getItem(int position) {
      return files[position];
    }

    @Override
    public long getItemId(int position) {
      return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
      ViewHolder vh;
      if (convertView == null) {
        convertView = LayoutInflater.from(ctx).inflate(R.layout.file_item, parent, false);
        vh = new ViewHolder();
        vh.icon = convertView.findViewById(R.id.fileIcon);
        vh.name = convertView.findViewById(R.id.fileName);
        vh.size = convertView.findViewById(R.id.fileSize);
        convertView.setTag(vh);
      } else {
        vh = (ViewHolder) convertView.getTag();
      }

      File f = files[position];
      if (f.isDirectory()) {
        vh.icon.setImageResource(R.drawable.ic_folder_gray_48dp);
        vh.size.setText("");
      } else {
        vh.icon.setImageResource(getFileIcon(f));
        vh.size.setText(formatSize(f.length()));
      }
      vh.name.setText(f.getName());
      return convertView;
    }

    private int getFileIcon(File f) {
      String n = f.getName().toLowerCase();
      if (n.endsWith(".apk")) return R.drawable.ic_apk_box;
      if (n.endsWith(".pdf")) return R.drawable.ic_pdf_box;
      if (n.endsWith(".png") || n.endsWith(".jpg") || n.endsWith(".jpeg"))
        return R.drawable.ic_image_box;
      if (n.endsWith(".mp3") || n.endsWith(".wav")) return R.drawable.ic_music_box;
      if (n.endsWith(".mp4") || n.endsWith(".avi")) return R.drawable.ic_video_box;
      if (n.endsWith(".zip") || n.endsWith(".rar")) return R.drawable.ic_zip_box;
      if (n.endsWith(".doc") || n.endsWith(".docx")) return R.drawable.ic_word_box;
      if (n.endsWith(".xls") || n.endsWith(".xlsx")) return R.drawable.ic_excel_box;
      if (n.endsWith(".ppt") || n.endsWith(".pptx")) return R.drawable.ic_powerpoint_box;
      return R.drawable.ic_file_gray_116dp;
    }

    private String formatSize(long bytes) {
      if (bytes < 1024) return bytes + " B";
      if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
      if (bytes < 1024 * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024));
      return String.format("%.1f GB", bytes / (1024.0 * 1024 * 1024));
    }

    static class ViewHolder {
      ImageView icon;
      TextView name;
      TextView size;
    }
  }
}
