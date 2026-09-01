var APK = {
  render: function(el) {
    var self = this;
    var html = '';
    html += '<div class="search-bar">';
    html += '<input type="text" id="app-search" placeholder="Search apps..." class="search-input">';
    html += '</div>';
    html += '<div id="app-list" class="grid"></div>';
    html += '<div class="card" style="margin-top:16px">';
    html += '<div class="card-title">Install APK</div>';
    html += '<div class="upload-area" id="apk-upload">';
    html += '<p>Click or drag APK file here</p>';
    html += '<input type="file" id="apk-file" accept=".apk" style="display:none">';
    html += '</div></div>';
    el.innerHTML = html;

    document.getElementById('app-search').addEventListener('input', function() {
      self.filter(this.value);
    });

    var uploadArea = document.getElementById('apk-upload');
    var fileInput = document.getElementById('apk-file');
    uploadArea.addEventListener('click', function() { fileInput.click(); });
    uploadArea.addEventListener('dragover', function(e) { e.preventDefault(); });
    uploadArea.addEventListener('drop', function(e) {
      e.preventDefault();
      if (e.dataTransfer.files.length) self.installApk(e.dataTransfer.files[0]);
    });
    fileInput.addEventListener('change', function() {
      if (this.files.length) self.installApk(this.files[0]);
    });

    self.loadApps();
  },

  loadApps: function() {
    var self = this;
    API.get('/api/apps').then(function(data) {
      self._items = data.items || [];
      self.renderList(self._items);
    }).catch(function() {
      document.getElementById('app-list').innerHTML = '<div class="empty"><p>Failed to load apps</p></div>';
    });
  },

  renderList: function(items) {
    var html = '';
    items.forEach(function(app) {
      html += '<div class="grid-item app-item" data-pkg="' + esc(app.packageName) + '">';
      html += '<img class="app-icon" src="/api/app-icon?pkg=' + encodeURIComponent(app.packageName) + '" ';
      html += 'onerror="this.src=\'data:image/svg+xml,<svg xmlns=\\\'http://www.w3.org/2000/svg\\\' viewBox=\\\'0 0 24 24\\\' fill=\\\'%23999\\\'><rect width=\\\'24\\\' height=\\\'24\\\' rx=\\\'4\\\'/></svg>\'">';
      html += '<div class="app-name">' + esc(app.name) + '</div>';
      html += '<div class="app-pkg">' + esc(app.packageName) + '</div>';
      html += '<div class="app-actions">';
      if (!app.isVirtual) {
        html += '<button class="btn btn-sm" onclick="APK.openApp(\'' + esc(app.packageName) + '\')">Open</button>';
        html += '<button class="btn btn-sm btn-danger" onclick="APK.uninstallApp(\'' + esc(app.packageName) + '\')">Uninstall</button>';
      }
      html += '</div></div>';
    });
    document.getElementById('app-list').innerHTML = html;
  },

  filter: function(q) {
    if (!this._items) return;
    var lower = q.toLowerCase();
    var filtered = this._items.filter(function(app) {
      return app.name.toLowerCase().indexOf(lower) >= 0 || app.packageName.toLowerCase().indexOf(lower) >= 0;
    });
    this.renderList(filtered);
  },

  openApp: function(pkg) {
    API.post('/api/app-open?pkg=' + encodeURIComponent(pkg), null).then(function(d) {
      showToast(d.success ? 'Opened' : (d.error || 'Failed'));
    });
  },

  uninstallApp: function(pkg) {
    if (!confirm('Uninstall ' + pkg + '?')) return;
    API.post('/api/app-uninstall?pkg=' + encodeURIComponent(pkg), null).then(function(d) {
      showToast(d.success ? 'Uninstall started' : (d.error || 'Failed'));
    });
  },

  installApk: function(file) {
    showToast('Uploading ' + file.name + '...');
    var fd = new FormData();
    fd.append('file', file);
    API.post('/api/upload?path=' + encodeURIComponent('/sdcard/Download'), fd).then(function(d) {
      if (d.success) {
        var path = d.path || '/sdcard/Download/' + file.name;
        API.post('/api/app-install?path=' + encodeURIComponent(path), null).then(function(d2) {
          showToast(d2.success ? 'Install started' : (d2.error || 'Failed'));
        });
      } else {
        showToast('Upload failed: ' + (d.error || 'Unknown'));
      }
    });
  }
};
