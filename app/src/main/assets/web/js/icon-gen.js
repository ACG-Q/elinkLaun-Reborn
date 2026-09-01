var IconGen = {
  render: function(el) {
    el.innerHTML = [
      '<div class="igen-preview">',
      '  <canvas id="igen-preview" width="256" height="256" style="max-width:256px;max-height:256px;border-radius:12px;image-rendering:-webkit-optimize-contrast"></canvas>',
      '</div>',

      '<div class="igen-section">',
      '  <div class="igen-section-title">\u56FE\u6807\u5185\u5BB9</div>',
      '  <div class="igen-mode-tabs">',
      '    <button class="igen-mode-tab igen-mode-tab-active" data-mode="text">\u6587\u5B57</button>',
      '    <button class="igen-mode-tab" data-mode="image">\u56FE\u7247</button>',
      '  </div>',
      '  <div class="igen-control" style="margin-top:12px" id="igen-textMode">',
      '    <input type="text" class="igen-input" id="igen-text" value="\u8BBE\u7F6E" maxlength="2" placeholder="\u8F93\u5165 1~2 \u4E2A\u5B57">',
      '  </div>',
      '  <div class="igen-control" id="igen-imageMode" style="display:none;margin-top:12px">',
      '    <div class="igen-upload" id="igen-uploadArea">',
      '      <input type="file" id="igen-imageInput" accept="image/*" style="display:none">',
      '      <svg width="32" height="32" viewBox="0 0 24 24" fill="none" stroke="#888" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"><path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/><polyline points="17 8 12 3 7 8"/><line x1="12" y1="3" x2="12" y2="15"/></svg>',
      '      <div style="font-size:13px;color:#666;margin-top:8px">\u70B9\u51FB\u6216\u62D6\u62FD\u4E0A\u4F20\u56FE\u7247</div>',
      '      <div style="font-size:12px;color:#888;margin-top:4px" id="igen-uploadHint">\u652F\u6301 PNG / JPG / WebP</div>',
      '    </div>',
      '  </div>',
      '</div>',

      '<div class="igen-section">',
      '  <div class="igen-section-title">\u914D\u8272\u65B9\u6848</div>',
      '  <div class="igen-presets" id="igen-colorScheme">',
      '    <button class="igen-preset igen-preset-active" data-bg="#ffffff" data-fg="#1a1a1a">\u767D\u5E95\u9ED1\u5B57</button>',
      '    <button class="igen-preset" data-bg="#1a1a1a" data-fg="#ffffff">\u9ED1\u5E95\u767D\u5B57</button>',
      '    <button class="igen-preset" data-bg="#1a73e8" data-fg="#ffffff">\u84DD\u5E95\u767D\u5B57</button>',
      '    <button class="igen-preset" data-bg="#1e8e3e" data-fg="#ffffff">\u7EFF\u5E95\u767D\u5B57</button>',
      '    <button class="igen-preset" data-bg="#d93025" data-fg="#ffffff">\u7EA2\u5E95\u767D\u5B57</button>',
      '    <button class="igen-preset" data-bg="#f9ab00" data-fg="#1a1a1a">\u9EC4\u5E95\u9ED1\u5B57</button>',
      '  </div>',
      '  <div style="margin-top:12px">',
      '    <div style="font-size:13px;color:#888;margin-bottom:8px">\u81EA\u5B9A\u4E49\u989C\u8272</div>',
      '    <div class="igen-color-row">',
      '      <input type="color" class="igen-color-input" id="igen-bgColor" value="#ffffff">',
      '      <div style="flex:1;min-width:0">',
      '        <div class="igen-color-hex" id="igen-bgHex">#FFFFFF</div>',
      '        <div class="igen-color-label">\u80CC\u666F\u8272</div>',
      '      </div>',
      '    </div>',
      '    <div class="igen-color-row" style="margin-top:8px">',
      '      <input type="color" class="igen-color-input" id="igen-fgColor" value="#1a1a1a">',
      '      <div style="flex:1;min-width:0">',
      '        <div class="igen-color-hex" id="igen-fgHex">#1A1A1A</div>',
      '        <div class="igen-color-label">\u6587\u5B57\u8272</div>',
      '      </div>',
      '    </div>',
      '  </div>',
      '</div>',

      '<div class="igen-section">',
      '  <div class="igen-section-title">\u8FB9\u6846</div>',
      '  <div style="display:flex;align-items:center;justify-content:space-between">',
      '    <span style="font-size:14px;font-weight:500">\u663E\u793A\u8FB9\u6846\uFF08\u8DDF\u968F\u6587\u5B57\u8272\uFF09</span>',
      '    <label class="igen-toggle">',
      '      <input type="checkbox" id="igen-borderEnabled" checked>',
      '      <span class="igen-toggle-track"></span>',
      '    </label>',
      '  </div>',
      '  <div style="margin-top:12px">',
      '    <div style="display:flex;justify-content:space-between;margin-bottom:6px">',
      '      <span style="font-size:13px;color:#888">\u8FB9\u6846\u5BBD\u5EA6</span>',
      '      <span class="igen-range-val" id="igen-borderWidthVal">3 px</span>',
      '    </div>',
      '    <input type="range" id="igen-borderWidth" min="1" max="12" step="1" value="3" style="width:100%;height:6px;-webkit-appearance:none;background:#e0e0e0;border-radius:3px;outline:none">',
      '  </div>',
      '</div>',

      '<div class="igen-section">',
      '  <div class="igen-section-title">\u5B57\u4F53</div>',
      '  <div style="font-size:13px;color:#888;margin-bottom:8px">\u5B57\u4F53\u98CE\u683C</div>',
      '  <div class="igen-chips" id="igen-fontList">',
      '    <button class="igen-chip igen-chip-active" data-font="system-ui">\u9ED8\u8BA4</button>',
      '    <button class="igen-chip" data-font="serif">\u5B8B\u4F53</button>',
      '    <button class="igen-chip" data-font="sans-serif">\u9ED1\u4F53</button>',
      '    <button class="igen-chip" data-font="cursive">\u6977\u4F53</button>',
      '    <button class="igen-chip" data-font="monospace">\u7B49\u5BBD</button>',
      '  </div>',
      '  <div style="margin-top:12px">',
      '    <div style="font-size:13px;color:#888;margin-bottom:8px">\u5B57\u4F53\u7C97\u7EC6</div>',
      '    <div class="igen-presets" id="igen-fontWeight">',
      '      <button class="igen-preset" data-weight="400">Regular</button>',
      '      <button class="igen-preset igen-preset-active" data-weight="700">Bold</button>',
      '      <button class="igen-preset" data-weight="900">Black</button>',
      '    </div>',
      '  </div>',
      '</div>',

      '<div class="igen-section">',
      '  <div class="igen-section-title">\u5C3A\u5BF8\u4E0E\u6837\u5F0F</div>',
      '  <div style="margin-bottom:10px">',
      '    <div style="display:flex;justify-content:space-between;margin-bottom:4px">',
      '      <span style="font-size:13px;color:#888">\u56FE\u7247\u5C3A\u5BF8</span>',
      '      <span class="igen-range-val" id="igen-sizeVal">256 px</span>',
      '    </div>',
      '    <input type="range" id="igen-size" min="64" max="1024" step="1" value="256" style="width:100%;height:6px;-webkit-appearance:none;background:#e0e0e0;border-radius:3px;outline:none">',
      '  </div>',
      '  <div style="margin-bottom:10px">',
      '    <div style="display:flex;justify-content:space-between;margin-bottom:4px">',
      '      <span style="font-size:13px;color:#888">\u5706\u89D2\u534A\u5F84</span>',
      '      <span class="igen-range-val" id="igen-radiusVal">32 px</span>',
      '    </div>',
      '    <input type="range" id="igen-radius" min="0" max="128" step="1" value="32" style="width:100%;height:6px;-webkit-appearance:none;background:#e0e0e0;border-radius:3px;outline:none">',
      '  </div>',
      '  <div style="margin-bottom:10px">',
      '    <div style="display:flex;justify-content:space-between;margin-bottom:4px">',
      '      <span style="font-size:13px;color:#888">\u5B57\u53F7</span>',
      '      <span class="igen-range-val" id="igen-fontSizeVal">100 %</span>',
      '    </div>',
      '    <input type="range" id="igen-fontSize" min="30" max="200" step="1" value="100" style="width:100%;height:6px;-webkit-appearance:none;background:#e0e0e0;border-radius:3px;outline:none">',
      '  </div>',
      '  <div>',
      '    <div style="display:flex;justify-content:space-between;margin-bottom:4px">',
      '      <span style="font-size:13px;color:#888">\u5B57\u95F4\u8DDD</span>',
      '      <span class="igen-range-val" id="igen-letterSpacingVal">0 px</span>',
      '    </div>',
      '    <input type="range" id="igen-letterSpacing" min="-10" max="30" step="1" value="0" style="width:100%;height:6px;-webkit-appearance:none;background:#e0e0e0;border-radius:3px;outline:none">',
      '  </div>',
      '</div>',

      '<div class="igen-actions">',
      '  <div class="igen-actions-inner">',
      '    <button class="btn" id="igen-copyBtn" style="flex:1;padding:12px;border-radius:8px;border:2px solid #e0e0e0;background:#fff;font-size:14px;font-weight:600;cursor:pointer;min-height:48px;display:flex;align-items:center;justify-content:center;gap:8px">',
      '      <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect x="9" y="9" width="13" height="13" rx="2" ry="2"/><path d="M5 15H4a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2h9a2 2 0 0 1 2 2v1"/></svg>',
      '      \u590D\u5236',
      '    </button>',
      '    <button class="btn btn-primary" id="igen-downloadBtn" style="flex:1;padding:12px;border-radius:8px;font-size:14px;font-weight:600;cursor:pointer;min-height:48px;display:flex;align-items:center;justify-content:center;gap:8px">',
      '      <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/><polyline points="7 10 12 15 17 10"/><line x1="12" y1="15" x2="12" y2="3"/></svg>',
      '      \u4E0B\u8F7D PNG',
      '    </button>',
      '  </div>',
      '</div>'
    ].join('');

    var canvas = document.getElementById('igen-preview');
    var ctx = canvas.getContext('2d');

    var state = {
      text: '设置', bg: '#ffffff', fg: '#1a1a1a',
      font: 'system-ui', weight: '700', size: 256, radius: 32,
      fontSize: 100, letterSpacing: 0, borderEnabled: true,
      borderWidth: 3, mode: 'text', image: null
    };

    function roundedRect(x, y, w, h, r) {
      ctx.beginPath();
      ctx.moveTo(x + r, y);
      ctx.lineTo(x + w - r, y);
      ctx.quadraticCurveTo(x + w, y, y + r);
      ctx.lineTo(x + w, y + h - r);
      ctx.quadraticCurveTo(x + w, y + h, x + w - r, y + h);
      ctx.lineTo(x + r, y + h);
      ctx.lineTo(x, y + h - r);
      ctx.quadraticCurveTo(x, y + h, x + r, y + h);
      ctx.lineTo(x, y + r);
      ctx.quadraticCurveTo(x, y, x + r, y);
      ctx.closePath();
    }

    function draw() {
      var s = state.size;
      canvas.width = s;
      canvas.height = s;
      var display = Math.min(s, 256);
      canvas.style.width = display + 'px';
      canvas.style.height = display + 'px';

      var r = Math.min(state.radius, s / 2);
      ctx.clearRect(0, 0, s, s);

      roundedRect(0, 0, s, s, r);
      ctx.fillStyle = state.bg;
      ctx.fill();

      if (state.borderEnabled && state.borderWidth > 0) {
        var bw = state.borderWidth;
        ctx.save();
        ctx.beginPath();
        roundedRect(bw / 2, bw / 2, s - bw, s - bw, Math.max(0, r - bw / 2));
        ctx.strokeStyle = state.fg;
        ctx.lineWidth = bw;
        ctx.stroke();
        ctx.restore();
      }

      var text = state.text;
      if (state.mode === 'image' && state.image) {
        var img = state.image;
        var scale = Math.min(s / img.width, s / img.height);
        var iw = img.width * scale;
        var ih = img.height * scale;
        ctx.drawImage(img, (s - iw) / 2, (s - ih) / 2, iw, ih);
        return;
      }

      if (!text) return;

      var charCount = text.length;
      var baseFontSize = s * 0.42 * (state.fontSize / 100);
      var fs = charCount > 1 ? baseFontSize * 0.85 : baseFontSize;

      ctx.fillStyle = state.fg;
      ctx.textAlign = 'center';
      ctx.textBaseline = 'middle';
      ctx.font = state.weight + ' ' + fs + 'px ' + state.font;

      var spacing = parseInt(state.letterSpacing) || 0;
      if (charCount === 1) {
        ctx.fillText(text, s / 2, s / 2);
      } else {
        var chars = text.split('');
        var charWidths = [];
        var totalWidth = 0;
        chars.forEach(function(ch) {
          var w = ctx.measureText(ch).width;
          charWidths.push(w);
          totalWidth += w;
        });
        totalWidth += spacing * (charCount - 1);
        var cx = (s - totalWidth) / 2;
        chars.forEach(function(ch, i) {
          var cw = charWidths[i];
          ctx.fillText(ch, cx + cw / 2, s / 2);
          cx += cw + spacing;
        });
      }
    }

    function download() {
      var name = state.mode === 'image' && state.image ? 'image' : (state.text || 'icon');
      var a = document.createElement('a');
      a.download = name + '.png';
      a.href = canvas.toDataURL('image/png');
      a.click();
      showToast('\u5DF2\u4FDD\u5B58\u5230\u4E0B\u8F7D\u76EE\u5F55');
    }

    function copyClipboard() {
      canvas.toBlob(function(blob) {
        if (navigator.clipboard && navigator.clipboard.write) {
          navigator.clipboard.write([new ClipboardItem({'image/png': blob})]).then(function() {
            showToast('\u5DF2\u590D\u5236\u5230\u526A\u8D34\u677F');
          }).catch(function() {
            showToast('\u590D\u5236\u5931\u8D25\uFF0C\u8BF7\u91CD\u8BD5');
          });
        } else {
          showToast('\u5F53\u524D\u6D4F\u89C8\u5668\u4E0D\u652F\u6301\u590D\u5236');
        }
      });
    }

    function loadImage(file) {
      if (!file.type.startsWith('image/')) {
        showToast('\u8BF7\u4E0A\u4F20\u56FE\u7247\u6587\u4EF6');
        return;
      }
      var reader = new FileReader();
      reader.onload = function(e) {
        var img = new Image();
        img.onload = function() {
          state.image = img;
          var area = document.getElementById('igen-uploadArea');
          if (area) {
            area.innerHTML = '<img src="' + e.target.result + '" style="width:100%;display:block;border-radius:8px"><button id="igen-clearImg" style="position:absolute;top:8px;right:8px;width:28px;height:28px;border-radius:50%;background:rgba(0,0,0,0.6);color:#fff;border:none;cursor:pointer;font-size:16px;line-height:1">&times;</button>';
            area.style.position = 'relative';
            area.style.padding = '0';
            area.style.overflow = 'hidden';
            var clearBtn = document.getElementById('igen-clearImg');
            if (clearBtn) {
              clearBtn.addEventListener('click', function(ev) {
                ev.stopPropagation();
                clearImage();
              });
            }
          }
          var hint = document.getElementById('igen-uploadHint');
          if (hint) hint.textContent = Math.round(img.width) + '\u00D7' + Math.round(img.height);
          draw();
          showToast('\u56FE\u7247\u5DF2\u52A0\u8F7D');
        };
        img.src = e.target.result;
      };
      reader.readAsDataURL(file);
    }

    function clearImage() {
      state.image = null;
      var input = document.getElementById('igen-imageInput');
      if (input) input.value = '';
      var area = document.getElementById('igen-uploadArea');
      if (area) {
        area.style.position = '';
        area.style.padding = '';
        area.style.overflow = '';
        area.innerHTML = '<svg width="32" height="32" viewBox="0 0 24 24" fill="none" stroke="#888" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"><path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/><polyline points="17 8 12 3 7 8"/><line x1="12" y1="3" x2="12" y2="15"/></svg><div style="font-size:13px;color:#666;margin-top:8px">\u70B9\u51FB\u6216\u62D6\u62FD\u4E0A\u4F20\u56FE\u7247</div><div style="font-size:12px;color:#888;margin-top:4px" id="igen-uploadHint">\u652F\u6301 PNG / JPG / WebP</div>';
      }
      draw();
      showToast('\u5DF2\u79FB\u9664\u56FE\u7247');
    }

    function updateHexDisplays() {
      var bgHex = document.getElementById('igen-bgHex');
      var fgHex = document.getElementById('igen-fgHex');
      if (bgHex) bgHex.textContent = state.bg.toUpperCase();
      if (fgHex) fgHex.textContent = state.fg.toUpperCase();
    }

    function clearSchemeActive() {
      document.querySelectorAll('#igen-colorScheme .igen-preset').forEach(function(b) { b.classList.remove('igen-preset-active'); });
    }

    function clearFontActive() {
      document.querySelectorAll('#igen-fontList .igen-chip').forEach(function(b) { b.classList.remove('igen-chip-active'); });
    }

    function clearWeightActive() {
      document.querySelectorAll('#igen-fontWeight .igen-preset').forEach(function(b) { b.classList.remove('igen-preset-active'); });
    }

    function bindEvents() {
      var textInput = document.getElementById('igen-text');
      if (textInput) textInput.addEventListener('input', function() { state.text = this.value; draw(); });

      var bgColor = document.getElementById('igen-bgColor');
      if (bgColor) bgColor.addEventListener('input', function() { state.bg = this.value; updateHexDisplays(); clearSchemeActive(); draw(); });

      var fgColor = document.getElementById('igen-fgColor');
      if (fgColor) fgColor.addEventListener('input', function() { state.fg = this.value; updateHexDisplays(); clearSchemeActive(); draw(); });

      var borderEnabled = document.getElementById('igen-borderEnabled');
      if (borderEnabled) borderEnabled.addEventListener('change', function() { state.borderEnabled = this.checked; draw(); });

      var borderWidth = document.getElementById('igen-borderWidth');
      if (borderWidth) borderWidth.addEventListener('input', function() { state.borderWidth = parseInt(this.value); var v = document.getElementById('igen-borderWidthVal'); if (v) v.textContent = this.value + ' px'; draw(); });

      var sizeSlider = document.getElementById('igen-size');
      if (sizeSlider) sizeSlider.addEventListener('input', function() { state.size = parseInt(this.value); var v = document.getElementById('igen-sizeVal'); if (v) v.textContent = this.value + ' px'; var maxR = Math.floor(state.size / 2); var radius = document.getElementById('igen-radius'); if (radius) { radius.max = maxR; if (state.radius > maxR) { state.radius = maxR; radius.value = maxR; var rv = document.getElementById('igen-radiusVal'); if (rv) rv.textContent = maxR + ' px'; } } draw(); });

      var radiusSlider = document.getElementById('igen-radius');
      if (radiusSlider) radiusSlider.addEventListener('input', function() { state.radius = parseInt(this.value); var v = document.getElementById('igen-radiusVal'); if (v) v.textContent = this.value + ' px'; draw(); });

      var fontSizeSlider = document.getElementById('igen-fontSize');
      if (fontSizeSlider) fontSizeSlider.addEventListener('input', function() { state.fontSize = parseInt(this.value); var v = document.getElementById('igen-fontSizeVal'); if (v) v.textContent = this.value + ' %'; draw(); });

      var letterSpacingSlider = document.getElementById('igen-letterSpacing');
      if (letterSpacingSlider) letterSpacingSlider.addEventListener('input', function() { state.letterSpacing = parseInt(this.value); var v = document.getElementById('igen-letterSpacingVal'); if (v) v.textContent = this.value + ' px'; draw(); });

      var presets = document.querySelectorAll('#igen-colorScheme .igen-preset');
      presets.forEach(function(btn) {
        btn.addEventListener('click', function() {
          clearSchemeActive();
          this.classList.add('igen-preset-active');
          state.bg = this.getAttribute('data-bg');
          state.fg = this.getAttribute('data-fg');
          var bgInput = document.getElementById('igen-bgColor');
          var fgInput = document.getElementById('igen-fgColor');
          if (bgInput) bgInput.value = state.bg;
          if (fgInput) fgInput.value = state.fg;
          updateHexDisplays();
          draw();
        });
      });

      var fontBtns = document.querySelectorAll('#igen-fontList .igen-chip');
      fontBtns.forEach(function(btn) {
        btn.addEventListener('click', function() {
          clearFontActive();
          this.classList.add('igen-chip-active');
          state.font = this.getAttribute('data-font');
          draw();
        });
      });

      var weightBtns = document.querySelectorAll('#igen-fontWeight .igen-preset');
      weightBtns.forEach(function(btn) {
        btn.addEventListener('click', function() {
          clearWeightActive();
          this.classList.add('igen-preset-active');
          state.weight = this.getAttribute('data-weight');
          draw();
        });
      });

      var modeTabs = document.querySelectorAll('.igen-mode-tab');
      modeTabs.forEach(function(tab) {
        tab.addEventListener('click', function() {
          modeTabs.forEach(function(t) { t.classList.remove('igen-mode-tab-active'); });
          this.classList.add('igen-mode-tab-active');
          state.mode = this.getAttribute('data-mode');
          var textMode = document.getElementById('igen-textMode');
          var imageMode = document.getElementById('igen-imageMode');
          if (state.mode === 'text') {
            if (textMode) textMode.style.display = 'block';
            if (imageMode) imageMode.style.display = 'none';
          } else {
            if (textMode) textMode.style.display = 'none';
            if (imageMode) imageMode.style.display = 'block';
          }
          draw();
        });
      });

      var uploadArea = document.getElementById('igen-uploadArea');
      var imageInput = document.getElementById('igen-imageInput');
      if (uploadArea && imageInput) {
        uploadArea.addEventListener('click', function(e) {
          if (e.target.id === 'igen-clearImg') return;
          imageInput.click();
        });
        imageInput.addEventListener('change', function() {
          if (this.files && this.files[0]) loadImage(this.files[0]);
        });
        uploadArea.addEventListener('dragover', function(e) { e.preventDefault(); this.style.borderColor = '#1a73e8'; this.style.background = '#e8f0fe'; });
        uploadArea.addEventListener('dragleave', function() { this.style.borderColor = ''; this.style.background = ''; });
        uploadArea.addEventListener('drop', function(e) {
          e.preventDefault();
          this.style.borderColor = '';
          this.style.background = '';
          if (e.dataTransfer.files && e.dataTransfer.files[0]) loadImage(e.dataTransfer.files[0]);
        });
      }

      var copyBtn = document.getElementById('igen-copyBtn');
      if (copyBtn) copyBtn.addEventListener('click', copyClipboard);
      var downloadBtn = document.getElementById('igen-downloadBtn');
      if (downloadBtn) downloadBtn.addEventListener('click', download);
    }

    updateHexDisplays();
    bindEvents();
    draw();
  }
};
