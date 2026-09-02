var API = (function() {
  function get(url) {
    return fetch(url).then(function(r) { return r.json(); });
  }
  function post(url, body, opts) {
    var fetchOpts = { method: 'POST', body: body };
    if (opts && opts.headers) fetchOpts.headers = opts.headers;
    return fetch(url, fetchOpts).then(function(r) { return r.json(); });
  }
  function del(url) {
    return fetch(url, { method: 'DELETE' }).then(function(r) { return r.json(); });
  }
  function upload(path, files) {
    var fd = new FormData();
    for (var i = 0; i < files.length; i++) fd.append('file', files[i]);
    return post('/api/upload?path=' + encodeURIComponent(path), fd);
  }
  function uploadChunked(options) {
    var file = options.file;
    var onProgress = options.onProgress;
    var action = options.action || 'file';
    var targetPath = options.targetPath || '';
    var pkg = options.pkg || '';
    var concurrency = options.concurrency || 3;
    var maxRetries = options.maxRetries || 3;

    return post('/api/upload/start', JSON.stringify({
      filename: file.name,
      size: file.size,
      action: action,
      targetPath: targetPath,
      pkg: pkg
    }), {
      headers: { 'Content-Type': 'application/json' }
    }).then(function(startResp) {
      if (!startResp.success) throw new Error(startResp.error || 'Failed to start upload');
      var sessionId = startResp.sessionId;
      var totalChunks = startResp.totalChunks;
      var chunkSize = startResp.chunkSize;
      var confirmed = new Array(totalChunks).fill(false);
      var completedCount = 0;

      function getChunk(index) {
        var start = index * chunkSize;
        var end = Math.min(start + chunkSize, file.size);
        return file.slice(start, end);
      }

      function uploadChunk(index, retries) {
        return new Promise(function(resolve, reject) {
          var blob = getChunk(index);
          var xhr = new XMLHttpRequest();
          xhr.open('POST', '/api/upload/chunk?sessionId=' + sessionId + '&chunkIndex=' + index);
          xhr.setRequestHeader('Content-Type', 'application/octet-stream');
          xhr.responseType = 'json';
          xhr.onload = function() {
            var resp = xhr.response;
            if (resp && resp.success) {
              confirmed[index] = true;
              completedCount++;
              if (onProgress) {
                var pct = Math.round(completedCount / totalChunks * 100);
                onProgress(pct);
              }
              resolve(resp);
            } else {
              reject(new Error(resp ? resp.error : 'Chunk upload failed'));
            }
          };
          xhr.onerror = function() { reject(new Error('Network error')); };
          xhr.send(blob);
        });
      }

      function uploadChunkWithRetry(index) {
        return new Promise(function(resolve, reject) {
          var retries = 0;
          function attempt() {
            uploadChunk(index, retries).then(resolve).catch(function(err) {
              retries++;
              if (retries <= maxRetries) {
                setTimeout(attempt, 1000 * Math.pow(2, retries - 1));
              } else {
                reject(err);
              }
            });
          }
          attempt();
        });
      }

      function runQueue() {
        var queue = [];
        for (var i = 0; i < totalChunks; i++) {
          if (!confirmed[i]) queue.push(i);
        }
        var active = 0;
        var idx = 0;
        var errors = [];
        var total = queue.length;

        return new Promise(function(resolve, reject) {
          function next() {
            while (active < concurrency && idx < queue.length) {
              var chunkIdx = queue[idx++];
              active++;
              uploadChunkWithRetry(chunkIdx).then(function() {
                active--;
                if (errors.length > 0) return;
                if (idx >= queue.length && active === 0) {
                  resolve();
                } else {
                  next();
                }
              }).catch(function(err) {
                active++;
                errors.push(err);
              });
            }
            if (errors.length > 0 && active === 0) {
              reject(errors[0]);
            }
          }
          next();
        });
      }

      return runQueue().then(function() {
        return post('/api/upload/complete?sessionId=' + sessionId);
      }).then(function(completeResp) {
        if (!completeResp.success) throw new Error(completeResp.error || 'Upload complete failed');
        return completeResp;
      });
    });
  }
  function uploadStatus(sessionId) {
    return get('/api/upload/status?sessionId=' + encodeURIComponent(sessionId));
  }
  return { get: get, post: post, del: del, upload: upload, uploadChunked: uploadChunked, uploadStatus: uploadStatus };
})();

function showToast(msg) {
  var t = document.getElementById('toast');
  t.textContent = msg;
  t.classList.add('show');
  setTimeout(function() { t.classList.remove('show'); }, 3000);
}

function svgIcon(d, w, h) {
  return '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="' + (w||20) + '" height="' + (h||20) + '"><path d="' + d + '"/></svg>';
}

var ICONS = {
  home: 'M3 12l2-2m0 0l7-7 7 7M5 10v10a1 1 0 001 1h3m10-11l2 2m-2-2v10a1 1 0 01-1 1h-3m-4 0a1 1 0 01-1-1v-4a1 1 0 011-1h2a1 1 0 011 1v4a1 1 0 01-1 1h-2z',
  folder: 'M3 7v10a2 2 0 002 2h14a2 2 0 002-2V9a2 2 0 00-2-2h-6l-2-2H5a2 2 0 00-2 2z',
  phone: 'M12 18h.01M8 21h8a2 2 0 002-2V5a2 2 0 00-2-2H8a2 2 0 00-2 2v14a2 2 0 002 2z',
  image: 'M4 16l4.586-4.586a2 2 0 012.828 0L16 16m-2-2l1.586-1.586a2 2 0 012.828 0L20 14m-6-6h.01M6 20h12a2 2 0 002-2V6a2 2 0 00-2-2H6a2 2 0 00-2 2v12a2 2 0 002 2z',
  gear: 'M10.325 4.317c.426-1.756 2.924-1.756 3.35 0a1.724 1.724 0 002.573 1.066c1.543-.94 3.31.826 2.37 2.37a1.724 1.724 0 001.066 2.573c1.756.426 1.756 2.924 0 3.35a1.724 1.724 0 00-1.066 2.573c.94 1.543-.826 3.31-2.37 2.37a1.724 1.724 0 00-2.573 1.066c-.426 1.756-2.924 1.756-3.35 0a1.724 1.724 0 00-2.573-1.066c-1.543.94-3.31-.826-2.37-2.37a1.724 1.724 0 00-1.066-2.573c-1.756-.426-1.756-2.924 0-3.35a1.724 1.724 0 001.066-2.573c-.94-1.543.826-3.31 2.37-2.37.996.608 2.296.07 2.573-1.066z',
  gearInner: 'circle cx="12" cy="12" r="3"',
  back: 'M15 19l-7-7 7-7',
  chevron: 'M9 5l7 7-7 7',
  file: 'M7 21h10a2 2 0 002-2V9.414a1 1 0 00-.293-.707l-5.414-5.414A1 1 0 0012.586 3H7a2 2 0 00-2 2v14a2 2 0 002 2z',
  trash: 'M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16',
  upload: 'M12 5v14m-7-7h14',
  circle: 'M12 15v2m-6 4h12a2 2 0 002-2v-6a2 2 0 00-2-2H6a2 2 0 00-2 2v6a2 2 0 002 2zm10-10V7a4 4 0 00-8 0v4h8z',
  wifi: 'M17.657 16.657L13.414 20.9a1.998 1.998 0 01-2.827 0l-4.244-4.243a8 8 0 1111.314 0z',
  display: 'M12 3v1m0 16v1m9-9h-1M4 12H3m15.364 6.364l-.707-.707M6.343 6.343l-.707-.707m12.728 0l-.707.707M6.343 17.657l-.707.707M16 12a4 4 0 11-8 0 4 4 0 018 0z',
  sound: 'M15.536 8.464a5 5 0 010 7.072m2.828-9.9a9 9 0 010 12.728M5.586 15H4a1 1 0 01-1-1v-4a1 1 0 011-1h1.586l4.707-4.707C10.923 3.663 12 4.109 12 5v14c0 .891-1.077 1.337-1.707.707L5.586 15z',
  apps: 'M9.75 17L9 20l-1 1h8l-1-1-.75-3M3 13h18M5 17h14a2 2 0 002-2V5a2 2 0 00-2-2H5a2 2 0 00-2 2v10a2 2 0 002 2z',
  code: 'M10 20l4-16m4 4l4 4-4 4M6 16l-4-4 4-4',
  battery: 'M17 6h-2V4a2 2 0 00-2-2H9a2 2 0 00-2 2v2H5a2 2 0 00-2 2v10a2 2 0 002 2h14a2 2 0 002-2V8a2 2 0 00-2-2z',
  storage: 'M4 7v10c0 2.21 3.582 4 8 4s8-1.79 8-4V7M4 7c0 2.21 3.582 4 8 4s8-1.79 8-4M4 7c0-2.21 3.582-4 8-4s8 1.79 8 4m0 5c0 2.21-3.582 4-8 4s-8-1.79-8-4',
  bell: 'M15 17h5l-1.405-1.405A2.032 2.032 0 0118 14.158V11a6.002 6.002 0 00-4-5.659V5a2 2 0 10-4 0v.341C7.67 6.165 6 8.388 6 11v3.159c0 .538-.214 1.055-.595 1.436L4 17h5m6 0v1a3 3 0 11-6 0v-1m6 0H9',
  pin: 'M17.657 16.657L13.414 20.9a1.998 1.998 0 01-2.827 0l-4.244-4.243a8 8 0 1111.314 0z',
  lock: 'M12 15v2m-6 4h12a2 2 0 002-2v-6a2 2 0 00-2-2H6a2 2 0 00-2 2v6a2 2 0 002 2zm10-10V7a4 4 0 00-8 0v4h8z',
  rect: 'M4 7v10a2 2 0 002 2h14a2 2 0 002-2V9a2 2 0 00-2-2h-6l-2-2H5a2 2 0 00-2 2z'
};
