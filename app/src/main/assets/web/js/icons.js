var Icons = {
  render: function(el) {
    var html = '';
    html += '<div class="search-bar">';
    html += '<input type="text" id="icon-search" placeholder="Search apps..." class="search-input">';
    html += '</div>';
    html += '<div style="margin-bottom:12px">';
    html += '<button class="btn btn-primary" onclick="Icons.openGenerator()" style="width:100%">';
    html += '<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"/><line x1="12" y1="8" x2="12" y2="16"/><line x1="8" y1="12" x2="16" y2="12"/></svg>';
    html += '圆角图片生成器';
    html += '</button>';
    html += '</div>';
    html += '<div id="icon-list" class="icon-grid"></div>';
    el.innerHTML = html;

    document.getElementById('icon-search').addEventListener('input', function() {
      Icons.filter(this.value);
    });

    Icons.loadIcons();
  },

  openGenerator: function() {
    window.location.hash = '/icon-gen';
  },

  loadIcons: function() {
    API.get('/api/icons').then(function(data) {
      Icons._items = data.items || [];
      Icons.renderList(Icons._items);
    }).catch(function() {
      document.getElementById('icon-list').innerHTML = '<div class="empty"><p>Failed to load icons</p></div>';
    });
  },

  renderList: function(items) {
    var html = '';
    items.forEach(function(app) {
      var iconUrl = '/api/app-icon?pkg=' + encodeURIComponent(app.packageName);
      html += '<div class="icon-slot" data-pkg="' + esc(app.packageName) + '" ';
      html += 'style="background-image:url(\'' + iconUrl + '\')" ';
      html += 'onclick="Icons.clickSlot(\'' + esc(app.packageName) + '\')">';
      html += '<input type="file" accept="image/*" style="display:none" data-pkg="' + esc(app.packageName) + '">';
      html += '<div class="icon-slot-name">' + esc(app.name) + '</div>';
      if (app.hasCustomIcon) {
        html += '<div class="icon-slot-badge">Custom</div>';
      }
      html += '</div>';
    });
    document.getElementById('icon-list').innerHTML = html;

    document.querySelectorAll('.icon-slot').forEach(function(slot) {
      slot.addEventListener('dragover', function(e) { e.preventDefault(); e.stopPropagation(); });
      slot.addEventListener('dragleave', function(e) { e.currentTarget.classList.remove('drag-over'); });
      slot.addEventListener('dragenter', function(e) { e.currentTarget.classList.add('drag-over'); });
      slot.addEventListener('drop', function(e) {
        e.preventDefault();
        e.stopPropagation();
        e.currentTarget.classList.remove('drag-over');
        var pkg = e.currentTarget.getAttribute('data-pkg');
        if (e.dataTransfer.files.length && pkg) {
          Icons.replaceIcon(pkg, e.dataTransfer.files[0]);
        }
      });
    });
  },

  filter: function(q) {
    if (!Icons._items) return;
    var lower = q.toLowerCase();
    var filtered = Icons._items.filter(function(app) {
      return app.name.toLowerCase().indexOf(lower) >= 0 || app.packageName.toLowerCase().indexOf(lower) >= 0;
    });
    Icons.renderList(filtered);
  },

  clickSlot: function(pkg) {
    var input = document.querySelector('.icon-slot[data-pkg="' + pkg + '"] input[type="file"]');
    if (input) {
      input.onchange = function() {
        if (this.files.length) Icons.replaceIcon(pkg, this.files[0]);
      };
      input.click();
    }
  },

  replaceIcon: function(pkg, file) {
    showToast('Uploading icon...');
    API.uploadChunked({
      action: 'icon-replace',
      pkg: pkg,
      file: file,
      onProgress: function(pct) {
        if (pct >= 100) showToast('Processing...');
      }
    }).then(function(d) {
      if (d.success) {
        showToast('Icon replaced');
        Icons.loadIcons();
      } else {
        showToast('Failed: ' + (d.error || 'Unknown'));
      }
    }).catch(function() {
      showToast('Upload failed');
    });
  }
};
