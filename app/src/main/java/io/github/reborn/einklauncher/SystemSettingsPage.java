package io.github.reborn.einklauncher;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

import io.github.reborn.einklauncher.R;
import io.github.reborn.einklauncher.Utils;

public class SystemSettingsPage extends Activity {

  private ListView lvSettings;

  private static class SettingItem {
    String title;
    String summary;
    String action;
    int iconRes;
  }

  @Override
  protected void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    initViews();
  }

  private void initViews() {
    LinearLayout root = new LinearLayout(this);
    root.setOrientation(LinearLayout.VERTICAL);
    root.setBackgroundColor(android.graphics.Color.WHITE);

    int pad = Utils.dp2Px(this, 10);
    root.setPadding(pad, pad, pad, pad);

    TextView title = new TextView(this);
    title.setText(getString(R.string.system_settings));
    title.setTextSize(20);
    title.setTypeface(null, android.graphics.Typeface.BOLD);
    title.setTextColor(android.graphics.Color.BLACK);
    root.addView(title);
    root.addView(makeDivider());

    lvSettings = new ListView(this);
    lvSettings.setDivider(new android.graphics.drawable.ColorDrawable(android.graphics.Color.BLACK));
    lvSettings.setDividerHeight(1);
    root.addView(lvSettings, new LinearLayout.LayoutParams(-1, 0, 1));

    Button btnBack = new Button(this);
    btnBack.setText(getString(R.string.exit));
    root.addView(btnBack, new LinearLayout.LayoutParams(-1, Utils.dp2Px(this, 44)));

    setContentView(root);

    List<SettingItem> items = new ArrayList<>();
    SettingItem wifi = new SettingItem();
    wifi.title = getString(R.string.system_settings_wifi);
    wifi.summary = "";
    wifi.action = Settings.ACTION_WIFI_SETTINGS;
    wifi.iconRes = R.drawable.wifi_on;
    items.add(wifi);

    SettingItem display = new SettingItem();
    display.title = getString(R.string.system_settings_display);
    display.summary = "";
    display.action = Settings.ACTION_DISPLAY_SETTINGS;
    display.iconRes = R.drawable.brightness_on;
    items.add(display);

    SettingItem sound = new SettingItem();
    sound.title = getString(R.string.system_settings_sound);
    sound.summary = "";
    sound.action = Settings.ACTION_SOUND_SETTINGS;
    sound.iconRes = R.drawable.setting_wifi1;
    items.add(sound);

    SettingItem apps = new SettingItem();
    apps.title = getString(R.string.system_settings_apps);
    apps.summary = "";
    apps.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS;
    apps.iconRes = R.drawable.ic_visibility;
    items.add(apps);

    SettingItem dev = new SettingItem();
    dev.title = getString(R.string.system_settings_developer);
    dev.summary = "";
    dev.action = Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS;
    dev.iconRes = R.drawable.ic_visibility_off;
    items.add(dev);

    SettingItem battery = new SettingItem();
    battery.title = getString(R.string.system_settings_battery);
    battery.summary = "";
    battery.action = Settings.ACTION_BATTERY_SAVER_SETTINGS;
    battery.iconRes = R.drawable.ic_visibility;
    items.add(battery);

    SettingItem storage = new SettingItem();
    storage.title = getString(R.string.system_settings_storage);
    storage.summary = "";
    storage.action = Settings.ACTION_INTERNAL_STORAGE_SETTINGS;
    storage.iconRes = R.drawable.ic_zip_box;
    items.add(storage);

    SettingItem notification = new SettingItem();
    notification.title = getString(R.string.system_settings_notification);
    notification.summary = "";
    notification.action = Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS;
    notification.iconRes = R.drawable.setting_wifi3;
    items.add(notification);

    SettingItem location = new SettingItem();
    location.title = getString(R.string.system_settings_location);
    location.summary = "";
    location.action = Settings.ACTION_LOCATION_SOURCE_SETTINGS;
    location.iconRes = R.drawable.setting_wifi4;
    items.add(location);

    SettingItem security = new SettingItem();
    security.title = getString(R.string.system_settings_security);
    security.summary = "";
    security.action = Settings.ACTION_SECURITY_SETTINGS;
    security.iconRes = R.drawable.ic_visibility_off;
    items.add(security);

    lvSettings.setAdapter(new SettingsAdapter(this, items));
    btnBack.setOnClickListener(v -> finish());
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

  private static class SettingsAdapter extends BaseAdapter {
    private final Context ctx;
    private final List<SettingItem> items;

    SettingsAdapter(Context ctx, List<SettingItem> items) {
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
        convertView = LayoutInflater.from(ctx).inflate(R.layout.settings_item, parent, false);
        vh = new ViewHolder();
        vh.icon = convertView.findViewById(R.id.settingIcon);
        vh.title = convertView.findViewById(R.id.settingTitle);
        vh.summary = convertView.findViewById(R.id.settingSummary);
        convertView.setTag(vh);
      } else {
        vh = (ViewHolder) convertView.getTag();
      }

      SettingItem item = items.get(position);
      vh.icon.setImageResource(item.iconRes);
      vh.title.setText(item.title);
      vh.summary.setText(item.summary);
      convertView.setOnClickListener(v -> {
        try {
          Intent intent = new Intent(item.action);
          ctx.startActivity(intent);
        } catch (Exception e) {
          Toast.makeText(ctx, "Cannot open setting", Toast.LENGTH_SHORT).show();
        }
      });
      return convertView;
    }

    static class ViewHolder {
      ImageView icon;
      TextView title;
      TextView summary;
    }
  }
}
