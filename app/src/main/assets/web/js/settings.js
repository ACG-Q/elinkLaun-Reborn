var Settings = {
  render: function(el) {
    var items = [
      { name: 'WiFi',        action: 'android.settings.WIFI_SETTINGS',                     icon: ICONS.wifi },
      { name: 'Display',     action: 'android.settings.DISPLAY_SETTINGS',                  icon: ICONS.display },
      { name: 'Sound',       action: 'android.settings.SOUND_SETTINGS',                    icon: ICONS.sound },
      { name: 'Apps',        action: 'android.settings.APPLICATION_DETAILS_SETTINGS',      icon: ICONS.apps },
      { name: 'Developer',   action: 'android.settings.APPLICATION_DEVELOPMENT_SETTINGS',  icon: ICONS.code },
      { name: 'Battery',     action: 'android.settings.BATTERY_SAVER_SETTINGS',            icon: ICONS.battery },
      { name: 'Storage',     action: 'android.settings.INTERNAL_STORAGE_SETTINGS',         icon: ICONS.storage },
      { name: 'Notifications', action: 'android.settings.NOTIFICATION_LISTENER_SETTINGS',  icon: ICONS.bell },
      { name: 'Location',    action: 'android.settings.LOCATION_SOURCE_SETTINGS',          icon: ICONS.pin },
      { name: 'Security',    action: 'android.settings.SECURITY_SETTINGS',                 icon: ICONS.lock }
    ];

    var html = '<div class="card">';
    items.forEach(function(s) {
      html += '<a href="' + s.action + '" class="row" style="text-decoration:none;color:inherit">';
      html += svgIcon(s.icon, 20, 20);
      html += '<div class="row-text"><div class="row-title">' + s.name + '</div></div>';
      html += svgIcon(ICONS.chevron, 16, 16);
      html += '</a>';
    });
    html += '</div>';

    el.innerHTML = html;
  }
};
