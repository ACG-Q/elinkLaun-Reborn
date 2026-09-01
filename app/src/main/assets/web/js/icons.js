var Icons = {
  render: function(el) {
    var self = this;
    var html = '';
    html += '<div class="search-bar">';
    html += '<input type="text" id="icon-search" placeholder="Search apps..." class="search-input">';
    html += '</div>';
    html += '<div id="icon-list" class="grid"></div>';
    html += '<div class="card" style="margin-top:16px">';
    html += '<div class="card-title">Upload Custom Icon</div>';
    html += '<div class="upload-area" id="icon-upload">';
    html += '<p>Click or drag image here, then select target app</p>';
    html += '<input type="file" id="icon-file" accept="image/*" style="display:none">';
    html += '</div></div>';
    el.innerHTML = html;

    document.getElementById('icon-search').addEventListener('input', function() {
      self.filter(this.value);
    });

    var uploadArea = document.getElementById('icon-upload');
    var fileInput = document.getElementById('icon-file');
    uploadArea.addEventListener('click', function() { fileInput.click(); });
    uploadArea.addEventListener('dragover', function(e) { e.preventDefault(); });
    uploadArea.addEventListener('drop', function(e) {
      e.preventDefault();
      if (e.dataTransfer.files.length) self.selectFileForUpload(e.dataTransfer.files[0]);
    });
    fileInput.addEventListener('change', function() {
      if (this.files.length) self.selectFileForUpload(this.files[0]);
    });

    self.loadIcons();
  },

  loadIcons: function() {
    var self = this;
    API.get('/api/icons').then(function(data) {
      self._items = data.items || [];
      self.renderList(self._items);
    }).catch(function() {
      document.getElementById('icon-list').innerHTML = '<div class="empty"><p>Failed to load icons</p></div>';
    });
  },

  renderList: function(items) {
    var html = '';
    items.forEach(function(app) {
      html += '<div class="grid-item icon-item" data-pkg="' + esc(app.packageName) + '">';
      html += '<div class="icon-wrapper" data-pkg="' + esc(app.packageName) + '">';
      html += '<img class="app-icon" src="/api/app-icon?pkg=' + encodeURIComponent(app.packageName) + '" ';
      html += 'onerror="this.src=\'data:image/svg+xml,<svg xmlns=\\\'http://www.w3.org/2000/svg\\\' viewBox=\\\'0 0 24 24\\\' fill=\\\'%23999\\\'><rect width=\\\'24\\\' height=\\\'24\\\' rx=\\\'4\\\'/></svg>\'">';
      if (app.hasCustomIcon) {
        html += '<span class="custom-badge">Custom</span>';
      }
      html += '</div>';
      html += '<div class="app-name">' + esc(app.name) + '</div>';
      html += '<div class="icon-actions">';
      html += '<button class="btn btn-sm" onclick="Icons.uploadFor(\'' + esc(app.packageName) + '\')">Replace</button>';
      if (app.hasCustomIcon) {
        html += '<button class="btn btn-sm btn-danger" onclick="Icons.resetIcon(\'' + esc(app.packageName) + '\')">Reset</button>';
      }
      html += '</div></div>';
    });
    document.getElementById('icon-list').innerHTML = html;

    document.querySelectorAll('.icon-wrapper').forEach(function(el) {
      el.addEventListener('dragover', function(e) { e.preventDefault(); });
      el.addEventListener('drop', function(e) {
        e.preventDefault();
        e.stopPropagation();
        var pkg = el.getAttribute('data-pkg');
        if (e.dataTransfer.files.length && pkg) {
          Icons.replaceIcon(pkg, e.dataTransfer.files[0]);
        }
      });
    });
  },

  filter: function(q) {
    if (!this._items) return;
    var lower = q.toLowerCase();
    var filtered = this._items.filter(function(app) {
      return app.name.toLowerCase().indexOf(lower) >= 0 || app.packageName.toLowerCase().indexOf(lower) >= 0;
    });
    this.renderList(filtered);
  },

  replaceIcon: function(pkg, file) {
    showToast('Uploading icon for ' + pkg + '...');
    var fd = new FormData();
    fd.append('file', file);
    API.post('/api/icons/replace?pkg=' + encodeURIComponent(pkg), fd).then(function(d) {
      if (d.success) {
        showToast('Icon replaced');
        Icons.loadIcons();
      } else {
        showToast('Failed: ' + (d.error || 'Unknown'));
      }
    }).catch(function() {
      showToast('Upload failed');
    });
  },

  resetIcon: function(pkg) {
    if (!confirm('Reset icon for ' + pkg + '?')) return;
    API.del('/api/icons/replace?pkg=' + encodeURIComponent(pkg)).then(function(d) {
      showToast(d.success ? 'Icon reset' : 'Failed');
      Icons.loadIcons();
    });
  },

  selectFileForUpload: function(file) {
    this._pendingFile = file;
    showToast('File selected. Click "Replace" on any app.');
  },

  uploadFor: function(pkg) {
    if (!this._pendingFile) {
      showToast('Select an image file first');
      return;
    }
    this.replaceIcon(pkg, this._pendingFile);
    this._pendingFile = null;
  }
};
