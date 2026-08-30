package io.github.reborn.einklauncher;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.AbsoluteSizeSpan;
import android.text.style.StyleSpan;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;

import java.io.File;
import java.io.FileReader;

public class CrashDetailPage extends Activity {

  private TextView tvContent;
  private String fullErrorText;

  @Override
  protected void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    initViews();
  }

  private void initViews() {
    LinearLayout root = new LinearLayout(this);
    root.setOrientation(LinearLayout.VERTICAL);
    root.setBackgroundColor(Color.WHITE);

    int pad = Utils.dp2Px(this, 12);

    LinearLayout contentWrap = new LinearLayout(this);
    contentWrap.setOrientation(LinearLayout.VERTICAL);
    contentWrap.setPadding(pad, pad, pad, pad);

    ScrollView scrollView = new ScrollView(this);
    tvContent = new TextView(this);
    tvContent.setTextColor(Color.BLACK);
    tvContent.setTextSize(13);
    tvContent.setTypeface(Typeface.MONOSPACE);
    tvContent.setLineSpacing(1.2f, 1.2f);

    scrollView.addView(tvContent);

    LinearLayout btnRow = new LinearLayout(this);
    btnRow.setOrientation(LinearLayout.HORIZONTAL);
    btnRow.setPadding(0, pad, 0, 0);

    TextView btnCopy = makeButton("Copy Error");
    TextView btnRestart = makeButton("Restart Launcher");

    LinearLayout.LayoutParams lpBtn = new LinearLayout.LayoutParams(0, Utils.dp2Px(this, 44), 1);
    lpBtn.rightMargin = pad / 2;
    btnRow.addView(btnCopy, lpBtn);

    LinearLayout.LayoutParams lpBtn2 = new LinearLayout.LayoutParams(0, Utils.dp2Px(this, 44), 1);
    lpBtn2.leftMargin = pad / 2;
    btnRow.addView(btnRestart, lpBtn2);

    contentWrap.addView(scrollView, new LinearLayout.LayoutParams(-1, -1, 1));
    contentWrap.addView(btnRow, new LinearLayout.LayoutParams(-1, -2));

    root.addView(contentWrap, new LinearLayout.LayoutParams(-1, -1));

    setContentView(root);

    btnCopy.setOnClickListener(v -> copyErrorToClipboard());
    btnRestart.setOnClickListener(v -> {
      Intent intent = new Intent(CrashDetailPage.this, Launcher.class);
      intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
      startActivity(intent);
      finish();
    });
  }

  private TextView makeButton(String text) {
    TextView btn = new TextView(this);
    btn.setText(text);
    btn.setTextColor(Color.BLACK);
    btn.setGravity(Gravity.CENTER);
    btn.setBackgroundColor(Color.WHITE);
    btn.setBackgroundResource(android.R.drawable.btn_default);
    return btn;
  }

  private void copyErrorToClipboard() {
    if (fullErrorText == null || fullErrorText.isEmpty()) return;
    ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
    ClipData clip = ClipData.newPlainText("Crash Report", fullErrorText);
    cm.setPrimaryClip(clip);
    Toast.makeText(this, "Copied to clipboard", Toast.LENGTH_SHORT).show();
  }

  private void fillErrorContent() {
    tvContent.setText("");
    StringBuilder sb = new StringBuilder();

    String title = "Oh! It's Crashed.";
    SpannableString titleSpan = new SpannableString(title);
    titleSpan.setSpan(new AbsoluteSizeSpan(22, true), 0, title.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
    titleSpan.setSpan(new StyleSpan(Typeface.BOLD), 0, title.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
    tvContent.append(titleSpan);
    tvContent.append("\n━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n");

    sb.append("Please screenshot and report the issue.\n");
    sb.append("telegram : https://t.me/ElnkLauncher\n");
    sb.append("github   : https://github.com/Modificator/E-Ink-Launcher\n");
    sb.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n\n");
    tvContent.append(sb.toString());

    if (getIntent().hasExtra("crashFile")) {
      String fileName = getIntent().getStringExtra("crashFile");
      File crashFile = new File(getExternalFilesDir("crash"), fileName);
      try {
        char[] readData = new char[(int) crashFile.length()];
        FileReader reader = new FileReader(crashFile);
        reader.read(readData);
        String crashLog = new String(readData);
        tvContent.append(crashLog);
        fullErrorText = title + "\n" + sb.toString() + crashLog;
        reader.close();
      } catch (Throwable e) {
        tvContent.append("Failed to read crash file: " + e.getMessage());
        fullErrorText = title + "\n" + sb.toString() + "Failed to read crash file: " + e.getMessage();
      }
    } else {
      fullErrorText = title + "\n" + sb.toString();
    }
  }

  @Override
  protected void onStart() {
    super.onStart();
    fillErrorContent();
  }
}
