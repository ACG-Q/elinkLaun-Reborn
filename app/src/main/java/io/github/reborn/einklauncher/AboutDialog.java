package io.github.reborn.einklauncher;

import android.app.AlertDialog;
import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

public class AboutDialog {

  private final Context context;

  private AboutDialog(Context context) {
    this.context = context;
  }

  public static AboutDialog getInstance(Context context) {
    return new AboutDialog(context);
  }

  private View initLayout() {
    LinearLayout root = new LinearLayout(context);
    int padding = Utils.dp2Px(context, 15);
    root.setPadding(padding, padding, padding, padding);
    root.setOrientation(LinearLayout.VERTICAL);
    root.setBackgroundColor(0xffffffff);

    // 标题
    TextView title = new TextView(context);
    title.setText(R.string.app_name);
    title.setTextSize(30);
    title.setTextColor(0xff000000);
    root.addView(title);

    // 版本号
    TextView version = new TextView(context);
    version.setText("v" + BuildConfig.VERSION_NAME);
    version.setTextSize(16);
    version.setTextColor(0xff666666);
    LinearLayout.LayoutParams versionLP = new LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    versionLP.topMargin = Utils.dp2Px(context, 4);
    root.addView(version, versionLP);

    addDivider(root);

    // 主要功能
    TextView features = new TextView(context);
    features.setText(R.string.about_features);
    features.setTextSize(15);
    features.setTextColor(0xff000000);
    features.setLineSpacing(0, 1.3f);
    LinearLayout.LayoutParams featuresLP = new LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    featuresLP.topMargin = Utils.dp2Px(context, 8);
    featuresLP.bottomMargin = Utils.dp2Px(context, 8);
    root.addView(features, featuresLP);

    addDivider(root);

    // 自定义图标说明
    TextView customIconTitle = new TextView(context);
    customIconTitle.setText(R.string.about_custom_icon_title);
    customIconTitle.setTextSize(16);
    customIconTitle.setTextColor(0xff000000);
    customIconTitle.setPadding(0, Utils.dp2Px(context, 8), 0, 0);
    root.addView(customIconTitle);

    TextView customIconInfo = new TextView(context);
    customIconInfo.setText(R.string.about_custom_icon_info);
    customIconInfo.setTextSize(14);
    customIconInfo.setTextColor(0xff333333);
    customIconInfo.setLineSpacing(0, 1.3f);
    LinearLayout.LayoutParams customIconLP = new LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    customIconLP.topMargin = Utils.dp2Px(context, 4);
    customIconLP.bottomMargin = Utils.dp2Px(context, 8);
    root.addView(customIconInfo, customIconLP);

    addDivider(root);

    // 自定义图标文件名列表
    TextView iconNames = new TextView(context);
    iconNames.setText(R.string.about_custom_icon_filenames);
    iconNames.setTextSize(13);
    iconNames.setTextColor(0xff555555);
    iconNames.setLineSpacing(0, 1.3f);
    LinearLayout.LayoutParams iconNamesLP = new LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    iconNamesLP.topMargin = Utils.dp2Px(context, 8);
    iconNamesLP.bottomMargin = Utils.dp2Px(context, 8);
    root.addView(iconNames, iconNamesLP);

    addDivider(root);

    // 目录说明
    TextView pathInfo = new TextView(context);
    pathInfo.setText(R.string.about_icon_path);
    pathInfo.setTextSize(13);
    pathInfo.setTextColor(0xff555555);
    pathInfo.setPadding(0, Utils.dp2Px(context, 8), 0, 0);
    root.addView(pathInfo);

    addDivider(root);

    // 开发者信息
    TextView devTitle = new TextView(context);
    devTitle.setText(R.string.about_developers);
    devTitle.setTextSize(16);
    devTitle.setTextColor(0xff000000);
    devTitle.setPadding(0, Utils.dp2Px(context, 8), 0, 0);
    root.addView(devTitle);

    TextView devInfo = new TextView(context);
    devInfo.setText(R.string.about_developer_info);
    devInfo.setTextSize(14);
    devInfo.setTextColor(0xff333333);
    devInfo.setLineSpacing(0, 1.3f);
    LinearLayout.LayoutParams devInfoLP = new LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    devInfoLP.topMargin = Utils.dp2Px(context, 4);
    devInfoLP.bottomMargin = Utils.dp2Px(context, 8);
    root.addView(devInfo, devInfoLP);

    return root;
  }

  private void addDivider(LinearLayout parent) {
    View line = new View(context);
    line.setBackgroundColor(0xff000000);
    parent.addView(line, new ViewGroup.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT, Utils.dp2Px(context, 1)));
  }

  public void show() {
    new AlertDialog.Builder(context)
        .setView(initLayout())
        .setPositiveButton(R.string.dialog_close, null)
        .show();
  }
}
