var Icons = {
  _selected: null,

  render: function(el) {
    el.innerHTML = '<div class="empty"><div class="spinner" style="margin:0 auto"></div><p>Loading icons...</p></div>';

    API.get('/api/icons').then(function(data) {
      var current = data.currentIcons || {};
      var available = data.availableIcons || [];

      var h = '';

      // Current icon slots
      h += '<div class="card"><div class="card-header">Assigned Icons</div>';
      var slots = [
        { key: 'lock', label: 'Lock Screen', icon: ICONS.lock },
        { key: 'wifi', label: 'WiFi', icon: ICONS.wifi },
        { key: 'http', label: 'HTTP Server', icon: ICONS.phone }
      ];
      slots.forEach(function(slot) {
        var cur = current[slot.key] || {};
        var assignedIcon = cur.icon || slot.key;
        h += '<div class="row" style="cursor:pointer" data-slot="' + slot.key + '" onclick="Icons.selectSlot(this)">';
        h += '<div style="width:40px;height:40px;background:#f0f0f0;border-radius:8px;display:flex;align-items:center;justify-content:center">';
        if (cur.url) {
          h += '<img src="' + esc(cur.url) + '" width="40" height="40" style="border-radius:8px" alt="">';
        } else {
          h += svgIcon(slot.icon, 24, 24);
        }
        h += '</div>';
        h += '<div class="row-text"><div class="row-title">' + slot.label + '</div>';
        h += '<div class="row-sub">' + esc(assignedIcon) + '</div></div>';
        h += svgIcon(ICONS.chevron, 16, 16);
        h += '</div>';
      });
      h += '</div>';

      // Available icons
      h += '<div class="card"><div class="card-header">Available Icons</div><div class="icon-grid">';
      available.forEach(function(icon) {
        var url = icon.url || null;
        h += '<div class="icon-item" data-name="' + esc(icon.name) + '" data-type="' + esc(icon.type) + '" onclick="Icons.pickIcon(this)">';
        h += '<div class="icon-box">';
        if (url) {
          h += '<img src="' + esc(url) + '" width="48" height="48" style="border-radius:8px" alt="">';
        } else {
          h += svgIcon(ICONS.circle, 24, 24);
        }
        h += '</div><div class="name">' + esc(icon.name) + '</div>';
        if (icon.type === 'custom') {
          h += '<button class="btn btn-sm btn-danger" style="margin-top:4px" onclick="event.stopPropagation();Icons.deleteIcon(\'' + esc(icon.name) + '\')">Delete</button>';
        }
        h += '</div>';
      });
      h += '</div></div>';

      // Upload zone
      h += '<div class="upload-zone" id="uploadZone">';
      h += svgIcon(ICONS.upload, 24, 24);
      h += '<div style="font-size:13px;font-weight:500">Upload custom icon (PNG, 96x96)</div>';
      h += '<input type="file" id="iconUpload" accept="image/*" style="display:none">';
      h += '</div>';

      el.innerHTML = h;

      // Upload handler
      var zone = document.getElementById('uploadZone');
      var input = document.getElementById('iconUpload');
      if (zone && input) {
        zone.addEventListener('click', function() { input.click(); });
        input.addEventListener('change', function() {
          if (this.files.length > 0) {
            var fd = new FormData();
            fd.append('file', this.files[0]);
            API.post('/api/icons/upload', fd).then(function(r) {
              showToast(r.success ? 'Icon uploaded' : 'Upload failed');
              Icons.render(el);
            }).catch(function() { showToast('Upload failed'); });
          }
        });
      }
    }).catch(function() {
      el.innerHTML = '<div class="empty"><p>Failed to load icons</p></div>';
    });
  },

  selectSlot: function(row) {
    var slot = row.getAttribute('data-slot');
    this._selected = slot;
    showToast('Select an icon below for ' + slot);
    document.querySelectorAll('.icon-item').forEach(function(item) {
      item.style.outline = '2px solid #1a73e8';
    });
  },

  pickIcon: function(item) {
    var slot = this._selected;
    if (!slot) {
      showToast('Tap a slot above first');
      return;
    }
    var name = item.getAttribute('data-name');
    API.post('/api/icons/assign?slot=' + slot + '&icon=' + encodeURIComponent(name)).then(function(r) {
      showToast(r.success ? 'Icon assigned' : 'Failed');
      Icons._selected = null;
      var app = document.getElementById('app');
      Icons.render(app);
    }).catch(function() { showToast('Failed'); });
  },

  deleteIcon: function(name) {
    if (!confirm('Delete "' + name + '"?')) return;
    API.del('/api/icons/custom?name=' + encodeURIComponent(name)).then(function(r) {
      showToast(r.success ? 'Deleted' : 'Failed');
      var app = document.getElementById('app');
      Icons.render(app);
    }).catch(function() { showToast('Failed'); });
  }
};
