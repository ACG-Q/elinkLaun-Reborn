var Icons = {
  render: function(el) {
    API.get('/api/icons').then(function(data) {
      var current = data.currentIcons || {};
      var available = data.availableIcons || [];

      var html = '';

      html += '<div class="card"><div class="card-header">Current Icons</div>';
      Object.keys(current).forEach(function(key) {
        var info = current[key];
        html += '<div class="row">';
        html += svgIcon(ICONS.circle, 20, 20);
        html += '<div class="row-text"><div class="row-title">' + esc(info.label) + '</div>';
        html += '<div class="row-sub">' + esc(key) + '.png</div></div>';
        html += '<div class="row-action"><button class="btn btn-sm" onclick="showToast(\'Click an icon below to replace\')">Change</button></div>';
        html += '</div>';
      });
      html += '</div>';

      html += '<div class="card"><div class="card-header">Available Icons</div><div class="icon-grid">';
      available.forEach(function(icon) {
        var url = icon.url ? icon.url : null;
        html += '<div class="icon-item" data-name="' + esc(icon.name) + '">';
        html += '<div class="icon-box">';
        if (url) {
          html += '<img src="' + esc(url) + '" width="48" height="48" style="border-radius:8px" alt="">';
        } else {
          html += svgIcon(ICONS.circle, 24, 24);
        }
        html += '</div><div class="name">' + esc(icon.name) + '</div></div>';
      });
      html += '</div></div>';

      html += '<div class="upload-zone" id="uploadZone">';
      html += svgIcon(ICONS.upload, 24, 24);
      html += '<div style="font-size:13px;font-weight:500">Upload custom icon (PNG, 96x96)</div>';
      html += '<input type="file" id="iconUpload" accept="image/*" style="display:none">';
      html += '</div>';
      html += '<p style="font-size:12px;color:#888;margin-top:8px;text-align:center">Icons are stored in the app cache directory.<br>Recommended: 96x96px PNG.</p>';

      el.innerHTML = html;

      el.querySelectorAll('.icon-item').forEach(function(item) {
        item.addEventListener('click', function() {
          var name = this.getAttribute('data-name');
          showToast('Selected: ' + name);
        });
      });

      var zone = document.getElementById('uploadZone');
      var input = document.getElementById('iconUpload');
      if (zone && input) {
        zone.addEventListener('click', function() { input.click(); });
      }
    }).catch(function() {
      el.innerHTML = '<div class="empty"><p>Failed to load icons</p></div>';
    });
  }
};
