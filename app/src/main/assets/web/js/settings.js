var Settings = {
  render: function(el) {
    el.innerHTML = '<div class="empty"><div class="spinner" style="margin:0 auto"></div><p>Loading device info...</p></div>';

    var device, battery, storage, wifi, volume, brightness, rotation;

    function done() {
      if (!device || !battery || !storage || !wifi || !volume || !brightness || !rotation) return;
      renderAll();
    }

    API.get('/api/device').then(function(d) { device = d; done(); });
    API.get('/api/battery').then(function(d) { battery = d; done(); });
    API.get('/api/storage').then(function(d) { storage = d; done(); });
    API.get('/api/wifi-status').then(function(d) { wifi = d; done(); });
    API.get('/api/volume').then(function(d) { volume = d; done(); });
    API.get('/api/brightness').then(function(d) { brightness = d; done(); });
    API.get('/api/rotation').then(function(d) { rotation = d; done(); });

    function renderAll() {
      var h = '';

      // Device info card
      h += '<div class="card"><div class="card-header">Device Info</div>';
      h += row('Model', device.model);
      h += row('Manufacturer', device.manufacturer);
      h += row('Android', 'API ' + device.sdkInt + ' (' + device.release + ')');
      h += row('Device', device.device);
      h += row('Brand', device.brand);
      h += row('Board', device.board);
      h += '</div>';

      // Battery card
      var batPct = battery.level || 0;
      var batColor = batPct > 50 ? '#1e8e3e' : batPct > 20 ? '#f9ab00' : '#d93025';
      h += '<div class="card"><div class="card-header">Battery</div>';
      h += '<div class="row"><div class="row-text"><div class="row-title">' + batPct + '% &middot; ' + esc(battery.statusText) + '</div>';
      h += '<div style="height:6px;background:#e0e0e0;border-radius:3px;margin-top:6px"><div style="height:100%;width:' + batPct + '%;background:' + batColor + ';border-radius:3px"></div></div>';
      h += '<div class="row-sub">' + esc(battery.healthText) + ' &middot; ' + battery.temperature + '</div></div></div>';
      h += '</div>';

      // Storage card
      var stPct = storage.total > 0 ? Math.round(storage.used / storage.total * 100) : 0;
      h += '<div class="card"><div class="card-header">Storage</div>';
      h += '<div class="row"><div class="row-text"><div class="row-title">' + storage.usedHuman + ' / ' + storage.totalHuman + '</div>';
      h += '<div style="height:6px;background:#e0e0e0;border-radius:3px;margin-top:6px"><div style="height:100%;width:' + stPct + '%;background:#1a73e8;border-radius:3px"></div></div>';
      h += '<div class="row-sub">' + storage.availableHuman + ' free (' + stPct + '%)</div></div></div>';
      h += '</div>';

      // WiFi card
      h += '<div class="card"><div class="card-header">WiFi</div>';
      h += row('Status', wifi.stateText);
      if (wifi.ssid) h += row('SSID', esc(wifi.ssid));
      if (wifi.rssi) h += row('Signal', wifi.rssi + ' dBm');
      h += '</div>';

      // Volume control
      h += '<div class="card"><div class="card-header">Volume</div>';
      var streams = [
        { key: 'music', label: 'Media', icon: 'M9 19V6l12-3v13M9 19c0 1.105-1.343 2-3 2s-3-.895-3-2 1.343-2 3-2 3 .895 3 2zm12-3c0 1.105-1.343 2-3 2s-3-.895-3-2 1.343-2 3-2 3 .895 3 2z' },
        { key: 'ring', label: 'Ring', icon: 'M15.536 8.464a5 5 0 010 7.072m2.828-9.9a9 9 0 010 12.728M5.586 15H4a1 1 0 01-1-1v-4a1 1 0 011-1h1.586l4.707-4.707C10.923 3.663 12 4.109 12 5v14c0 .891-1.077 1.337-1.707.707L5.586 15z' },
        { key: 'notification', label: 'Notif', icon: 'M15 17h5l-1.405-1.405A2.032 2.032 0 0118 14.158V11a6.002 6.002 0 00-4-5.659V5a2 2 0 10-4 0v.341C7.67 6.165 6 8.388 6 11v3.159c0 .538-.214 1.055-.595 1.436L4 17h5m6 0v1a3 3 0 11-6 0v-1m6 0H9' },
        { key: 'alarm', label: 'Alarm', icon: 'M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z' }
      ];
      streams.forEach(function(s) {
        var v = volume[s.key] || {};
        var cur = v.current || 0;
        var max = v.max || 15;
        h += '<div class="row" style="flex-wrap:wrap;gap:8px">';
        h += '<div style="width:20px;color:#666">' + svgIcon(s.icon, 16, 16) + '</div>';
        h += '<div style="width:50px;font-size:12px;color:#666">' + s.label + '</div>';
        h += '<input type="range" min="0" max="' + max + '" value="' + cur + '" data-stream="' + s.key + '" '
          + 'style="flex:1;min-width:0;height:32px;accent-color:#1a73e8" oninput="Settings.setVolume(this)">';
        h += '<div style="width:24px;font-size:12px;text-align:right;color:#888" id="vol-' + s.key + '">' + cur + '</div>';
        h += '</div>';
      });
      h += '</div>';

      // Brightness control
      h += '<div class="card"><div class="card-header">Brightness</div>';
      h += '<div class="row" style="flex-wrap:wrap;gap:8px">';
      h += '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="16" height="16"><circle cx="12" cy="12" r="5"/><path d="M12 1v2M12 21v2M4.22 4.22l1.42 1.42M18.36 18.36l1.42 1.42M1 12h2M21 12h2M4.22 19.78l1.42-1.42M18.36 5.64l1.42-1.42"/></svg>';
      h += '<input type="range" min="0" max="255" value="' + brightness.value + '" id="brightnessSlider" '
        + 'style="flex:1;min-width:0;height:32px;accent-color:#1a73e8" oninput="Settings.setBrightness(this.value)">';
      h += '<div style="width:32px;font-size:12px;text-align:right;color:#888" id="brightVal">' + brightness.value + '</div>';
      h += '</div></div>';

      // Rotation toggle
      h += '<div class="card"><div class="card-header">Auto Rotation</div>';
      h += '<div class="row">';
      h += '<div class="row-text"><div class="row-title">Screen rotation</div>';
      h += '<div class="row-sub">' + (rotation.enabled ? 'Auto-rotate is ON' : 'Auto-rotate is OFF') + '</div></div>';
      h += '<label class="toggle">';
      h += '<input type="checkbox" ' + (rotation.enabled ? 'checked' : '') + ' onchange="Settings.setRotation(this.checked)">';
      h += '<span class="slider"></span>';
      h += '</label>';
      h += '</div></div>';

      // System settings links
      h += '<div class="card"><div class="card-header">System Settings</div>';
      var links = [
        { name: 'WiFi', action: 'android.settings.WIFI_SETTINGS', icon: ICONS.wifi },
        { name: 'Bluetooth', action: 'android.settings.BLUETOOTH_SETTINGS', icon: ICONS.phone },
        { name: 'Display', action: 'android.settings.DISPLAY_SETTINGS', icon: ICONS.display },
        { name: 'Sound', action: 'android.settings.SOUND_SETTINGS', icon: ICONS.sound },
        { name: 'Apps', action: 'android.settings.APPLICATION_SETTINGS', icon: ICONS.apps },
        { name: 'Developer', action: 'android.settings.APPLICATION_DEVELOPMENT_SETTINGS', icon: ICONS.code },
        { name: 'Battery', action: 'android.settings.BATTERY_SAVER_SETTINGS', icon: ICONS.battery },
        { name: 'Storage', action: 'android.settings.INTERNAL_STORAGE_SETTINGS', icon: ICONS.storage },
        { name: 'Notifications', action: 'android.settings.NOTIFICATION_LISTENER_SETTINGS', icon: ICONS.bell },
        { name: 'Location', action: 'android.settings.LOCATION_SOURCE_SETTINGS', icon: ICONS.pin },
        { name: 'Security', action: 'android.settings.SECURITY_SETTINGS', icon: ICONS.lock },
        { name: 'About', action: 'android.settings.DEVICE_INFO_SETTINGS', icon: ICONS.circle }
      ];
      links.forEach(function(s) {
        h += '<div class="row" style="cursor:pointer" onclick="Settings.openSettings(\'' + s.action + '\')">';
        h += svgIcon(s.icon, 20, 20);
        h += '<div class="row-text"><div class="row-title">' + s.name + '</div></div>';
        h += svgIcon(ICONS.chevron, 16, 16);
        h += '</div>';
      });
      h += '</div>';

      el.innerHTML = h;
    }

    function row(label, value) {
      return '<div class="row"><div class="row-text"><div class="row-sub">' + label + '</div><div class="row-title">' + esc(value || 'N/A') + '</div></div></div>';
    }
  },

  setVolume: function(el) {
    var stream = el.getAttribute('data-stream');
    var val = parseInt(el.value);
    var display = document.getElementById('vol-' + stream);
    if (display) display.textContent = val;
    API.post('/api/volume?stream=' + stream + '&value=' + val, null).catch(function() {});
  },

  setBrightness: function(val) {
    var display = document.getElementById('brightVal');
    if (display) display.textContent = val;
    API.post('/api/brightness?value=' + val, null).catch(function() {});
  },

  setRotation: function(enabled) {
    API.post('/api/rotation?enabled=' + enabled, null).catch(function() {});
  },

  openSettings: function(action) {
    API.post('/api/open-settings?action=' + encodeURIComponent(action)).then(function(r) {
      if (!r.success) showToast('Failed: ' + (r.error || 'Unknown'));
    }).catch(function() { showToast('Failed to open settings'); });
  }
};
