var Home = {
  render: function(el) {
    API.get('/api/stats').then(function(data) {
      el.innerHTML = ''
        + '<div class="status-bar">'
        + '<div class="stat"><div class="val">' + data.fileCount + '</div><div class="lbl">Files</div></div>'
        + '<div class="stat"><div class="val">' + data.totalSizeHuman + '</div><div class="lbl">Total Size</div></div>'
        + '</div>'
        + '<div class="grid">'
        + '<a href="#/fm" class="grid-card">' + svgIcon(ICONS.folder, 28, 28) + '<div class="label">File Manager</div><div class="desc">Browse & manage files</div></a>'
        + '<a href="#/apk" class="grid-card">' + svgIcon(ICONS.phone, 28, 28) + '<div class="label">APK Manager</div><div class="desc">Install apps</div></a>'
        + '<a href="#/icons" class="grid-card">' + svgIcon(ICONS.image, 28, 28) + '<div class="label">Icon Manager</div><div class="desc">Customize icons</div></a>'
        + '<a href="#/settings" class="grid-card">' + svgIcon(ICONS.gear, 28, 28) + '<div class="label">System Settings</div><div class="desc">Device settings</div></a>'
        + '</div>';
    }).catch(function() {
      el.innerHTML = '<div class="empty"><p>Failed to load stats</p></div>';
    });
  }
};
