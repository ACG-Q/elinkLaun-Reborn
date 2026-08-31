var FM = {
  render: function(el, qp) {
    var currentPath = qp.path || '/';

    API.get('/api/files?path=' + encodeURIComponent(currentPath)).then(function(data) {
      var items = data.items || [];
      var parent = data.parentPath;

      var html = '';

      html += '<div class="card"><div class="row" style="font-size:12px;color:#888">'
        + svgIcon(ICONS.folder, 16, 16) + ' ' + esc(data.currentPath) + '</div></div>';

      html += '<div class="card">';
      if (items.length === 0) {
        html += '<div class="empty">' + svgIcon(ICONS.folder, 48, 48) + '<p>Empty directory</p></div>';
      } else {
        items.forEach(function(f) {
          html += '<div class="row">';
          if (f.isDir) {
            html += svgIcon(ICONS.folder, 20, 20);
            html += '<div class="row-text"><a href="#/fm?path=' + encodeURIComponent(f.path) + '" class="row-title">' + esc(f.name) + '</a></div>';
          } else {
            html += svgIcon(ICONS.file, 20, 20);
            html += '<div class="row-text"><a href="' + esc(f.path) + '" download class="row-title">' + esc(f.name) + '</a>';
            html += '<div class="row-sub">' + f.sizeHuman + '</div></div>';
          }
          html += '<div class="row-action"><button class="btn btn-sm btn-danger" data-path="' + esc(f.path) + '" data-name="' + esc(f.name) + '">Delete</button></div>';
          html += '</div>';
        });
      }
      html += '</div>';

      html += '<div class="upload-zone" id="uploadZone">'
        + svgIcon(ICONS.upload, 24, 24)
        + '<div style="font-size:13px;font-weight:500">Tap to upload files</div>'
        + '<input type="file" id="fileInput" multiple style="display:none">'
        + '</div>';

      el.innerHTML = html;

      el.querySelectorAll('.btn-danger').forEach(function(btn) {
        btn.addEventListener('click', function() {
          var p = this.getAttribute('data-path');
          var n = this.getAttribute('data-name');
          if (confirm('Delete "' + n + '"?')) {
            API.del('/api/files?path=' + encodeURIComponent(p)).then(function(r) {
              showToast(r.success ? 'Deleted' : 'Failed');
              if (r.success) FM.render(el, qp);
            });
          }
        });
      });

      var zone = document.getElementById('uploadZone');
      var input = document.getElementById('fileInput');
      if (zone && input) {
        zone.addEventListener('click', function() { input.click(); });
        input.addEventListener('change', function() {
          if (this.files.length > 0) {
            API.upload(currentPath, this.files).then(function(r) {
              showToast('Uploaded ' + r.uploaded + ' file(s)');
              FM.render(el, qp);
            });
          }
        });
      }
    }).catch(function() {
      el.innerHTML = '<div class="empty"><p>Failed to load files</p></div>';
    });
  }
};

function esc(s) {
  if (!s) return '';
  return s.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;');
}
