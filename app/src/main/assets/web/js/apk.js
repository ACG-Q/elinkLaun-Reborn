var APK = {
  render: function(el) {
    API.get('/api/apks').then(function(data) {
      var items = data.items || [];
      var html = '';

      if (items.length === 0) {
        html += '<div class="empty">' + svgIcon(ICONS.phone, 48, 48) + '<p>No APK files found in Download folders</p></div>';
      } else {
        html += '<div class="status-bar"><div class="stat"><div class="val">' + items.length + '</div><div class="lbl">APK Files</div></div></div>';
        html += '<div class="card">';
        items.forEach(function(f) {
          html += '<div class="row">';
          html += svgIcon(ICONS.phone, 20, 20);
          html += '<div class="row-text"><div class="row-title">' + esc(f.name) + '</div>';
          html += '<div class="row-sub">' + f.sizeHuman + ' &middot; ' + esc(f.parent) + '</div></div>';
          html += '<div class="row-action"><a href="' + esc(f.path) + '" download class="btn btn-sm btn-primary">Install</a></div>';
          html += '</div>';
        });
        html += '</div>';
      }

      el.innerHTML = html;
    }).catch(function() {
      el.innerHTML = '<div class="empty"><p>Failed to load APKs</p></div>';
    });
  }
};
