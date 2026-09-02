<template>
  <div>
    <div class="card" style="text-align:center;padding:16px;">
      <canvas ref="canvas" :width="size" :height="size"
        style="max-width:256px;max-height:256px;border-radius:12px;image-rendering:-webkit-optimize-contrast;"></canvas>
    </div>

    <div class="control-section">
      <div class="control-section-title">图标内容</div>
      <div style="display:flex;gap:8px;">
        <button class="btn" :class="{ 'btn-primary': mode === 'text' }" @click="mode='text';draw()">文字</button>
        <button class="btn" :class="{ 'btn-primary': mode === 'image' }" @click="mode='image';draw()">图片</button>
      </div>
      <div v-if="mode === 'text'" style="margin-top:12px;">
        <input class="form-input" v-model="text" maxlength="2" placeholder="输入 1~2 个字" @input="draw" />
      </div>
      <div v-if="mode === 'image'" style="margin-top:12px;">
        <div class="upload-area" id="igen-uploadArea" @click="$refs.imageInput.click()"
          @dragover.prevent="dragActive=true" @dragleave="dragActive=false"
          @drop.prevent="onImageDrop" :style="dragActive ? {borderColor:'var(--primary)',background:'var(--primary-light)'} : {}">
          <template v-if="!uploadedImageSrc">
            <svg width="32" height="32" viewBox="0 0 24 24" fill="none" stroke="var(--text-muted)" stroke-width="1.5"><path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/><polyline points="17 8 12 3 7 8"/><line x1="12" y1="3" x2="12" y2="15"/></svg>
            <div style="font-size:13px;color:var(--text-muted);margin-top:8px;">点击或拖拽上传图片</div>
            <div style="font-size:12px;color:var(--text-muted);margin-top:4px;">支持 PNG / JPG / WebP</div>
          </template>
          <template v-else>
            <img :src="uploadedImageSrc" style="width:100%;display:block;border-radius:8px;" />
            <button style="position:absolute;top:8px;right:8px;width:28px;height:28px;border-radius:50%;background:rgba(0,0,0,0.6);color:#fff;border:none;cursor:pointer;font-size:16px;" @click.stop="clearImage">&times;</button>
          </template>
          <input ref="imageInput" type="file" accept="image/*" style="display:none;" @change="onImageSelect" />
        </div>
      </div>
    </div>

    <div class="control-section">
      <div class="control-section-title">配色方案</div>
      <div class="color-presets">
        <button v-for="s in colorSchemes" :key="s.label" class="color-scheme-btn" :class="{ active: activeScheme === s.label }"
          @click="applyScheme(s)">{{ s.label }}</button>
      </div>
      <div style="margin-top:12px;">
        <div class="color-row">
          <input type="color" v-model="bgColor" @input="activeScheme='';draw()" />
          <div style="flex:1;min-width:0;"><div class="color-hex">{{ bgColor.toUpperCase() }}</div><div class="color-label">背景色</div></div>
        </div>
        <div class="color-row" style="margin-top:8px;">
          <input type="color" v-model="fgColor" @input="activeScheme='';draw()" />
          <div style="flex:1;min-width:0;"><div class="color-hex">{{ fgColor.toUpperCase() }}</div><div class="color-label">文字色</div></div>
        </div>
      </div>
    </div>

    <div class="control-section">
      <div class="control-section-title">边框</div>
      <div style="display:flex;align-items:center;justify-content:space-between;">
        <span style="font-size:14px;font-weight:500;">显示边框（跟随文字色）</span>
        <div class="toggle" :class="{ active: showBorder }" @click="showBorder=!showBorder;draw()"></div>
      </div>
      <div style="margin-top:12px;">
        <div style="display:flex;justify-content:space-between;margin-bottom:6px;">
          <span style="font-size:13px;color:var(--text-muted);">边框宽度</span>
          <span style="font-size:13px;color:var(--text-muted);">{{ borderWidth }} px</span>
        </div>
        <input type="range" min="1" max="12" v-model.number="borderWidth" @input="draw" style="width:100%;height:6px;" />
      </div>
    </div>

    <div class="control-section">
      <div class="control-section-title">字体</div>
      <div style="display:flex;gap:8px;flex-wrap:wrap;">
        <button v-for="f in fontList" :key="f.value" class="btn btn-sm" :class="{ 'btn-primary': fontFamily === f.value }"
          @click="fontFamily=f.value;draw()">{{ f.label }}</button>
      </div>
      <div style="margin-top:12px;">
        <div style="font-size:13px;color:var(--text-muted);margin-bottom:8px;">字体粗细</div>
        <div style="display:flex;gap:8px;">
          <button v-for="w in ['400','700','900']" :key="w" class="btn btn-sm" :class="{ 'btn-primary': fontWeight === w }"
            @click="fontWeight=w;draw()">{{ w === '400' ? 'Regular' : w === '700' ? 'Bold' : 'Black' }}</button>
        </div>
      </div>
    </div>

    <div class="control-section">
      <div class="control-section-title">尺寸与样式</div>
      <div v-for="s in sizeSliders" :key="s.key" style="margin-bottom:10px;">
        <div style="display:flex;justify-content:space-between;margin-bottom:4px;">
          <span style="font-size:13px;color:var(--text-muted);">{{ s.label }}</span>
          <span style="font-size:13px;color:var(--text-muted);">{{ s.value }} {{ s.unit }}</span>
        </div>
        <input type="range" :min="s.min" :max="s.max" v-model.number="s.value" @input="draw" style="width:100%;height:6px;" />
      </div>
    </div>

    <div style="display:flex;gap:8px;">
      <button class="btn" style="flex:1;padding:12px;border-radius:8px;border:2px solid var(--border);background:var(--bg);font-size:14px;font-weight:600;min-height:48px;display:flex;align-items:center;justify-content:center;gap:8px;" @click="copyClipboard">
        <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="9" y="9" width="13" height="13" rx="2"/><path d="M5 15H4a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2h9a2 2 0 0 1 2 2v1"/></svg>
        复制
      </button>
      <button class="btn btn-primary" style="flex:1;padding:12px;border-radius:8px;font-size:14px;font-weight:600;min-height:48px;display:flex;align-items:center;justify-content:center;gap:8px;" @click="download">
        <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/><polyline points="7 10 12 15 17 10"/><line x1="12" y1="15" x2="12" y2="3"/></svg>
        下载 PNG
      </button>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, nextTick } from 'vue'
import { toast } from '../api'

const canvas = ref(null)
const mode = ref('text')
const text = ref('设置')
const bgColor = ref('#ffffff')
const fgColor = ref('#1a1a1a')
const showBorder = ref(true)
const borderWidth = ref(3)
const fontFamily = ref('system-ui')
const fontWeight = ref('700')
const size = ref(256)
const radius = ref(32)
const fontSize = ref(100)
const letterSpacing = ref(0)
const uploadedImage = ref(null)
const uploadedImageSrc = ref('')
const dragActive = ref(false)
const activeScheme = ref('白底黑字')

const colorSchemes = [
  { label: '白底黑字', bg: '#ffffff', fg: '#1a1a1a' },
  { label: '黑底白字', bg: '#1a1a1a', fg: '#ffffff' },
  { label: '蓝底白字', bg: '#1a73e8', fg: '#ffffff' },
  { label: '绿底白字', bg: '#1e8e3e', fg: '#ffffff' },
  { label: '红底白字', bg: '#d93025', fg: '#ffffff' },
  { label: '黄底黑字', bg: '#f9ab00', fg: '#1a1a1a' }
]

const fontList = [
  { label: '默认', value: 'system-ui' },
  { label: '宋体', value: 'serif' },
  { label: '黑体', value: 'sans-serif' },
  { label: '楷体', value: 'cursive' },
  { label: '等宽', value: 'monospace' }
]

const sizeSliders = reactive([
  { key: 'size', label: '图片尺寸', value: 256, min: 64, max: 1024, unit: 'px' },
  { key: 'radius', label: '圆角半径', value: 32, min: 0, max: 128, unit: 'px' },
  { key: 'fontSize', label: '字号', value: 100, min: 30, max: 200, unit: '%' },
  { key: 'letterSpacing', label: '字间距', value: 0, min: -10, max: 30, unit: 'px' }
])

function draw() {
  const c = canvas.value
  if (!c) return
  const ctx = c.getContext('2d')
  const s = sizeSliders[0].value
  c.width = s; c.height = s
  const r = Math.min(sizeSliders[1].value, s / 2)
  ctx.clearRect(0, 0, s, s)

  // background
  ctx.beginPath()
  ctx.moveTo(r, 0); ctx.lineTo(s - r, 0)
  ctx.quadraticCurveTo(s, 0, s, r); ctx.lineTo(s, s - r)
  ctx.quadraticCurveTo(s, s, s - r, s); ctx.lineTo(r, s)
  ctx.quadraticCurveTo(0, s, 0, s - r); ctx.lineTo(0, r)
  ctx.quadraticCurveTo(0, 0, r, 0); ctx.closePath()
  ctx.fillStyle = bgColor.value; ctx.fill()

  // border
  if (showBorder.value && borderWidth.value > 0) {
    const bw = borderWidth.value
    ctx.save(); ctx.beginPath()
    const br = Math.max(0, r - bw / 2)
    ctx.moveTo(br + bw / 2, bw / 2)
    ctx.lineTo(s - br - bw / 2, bw / 2)
    ctx.quadraticCurveTo(s - bw / 2, bw / 2, s - bw / 2, br + bw / 2)
    ctx.lineTo(s - bw / 2, s - br - bw / 2)
    ctx.quadraticCurveTo(s - bw / 2, s - bw / 2, s - br - bw / 2, s - bw / 2)
    ctx.lineTo(br + bw / 2, s - bw / 2)
    ctx.quadraticCurveTo(bw / 2, s - bw / 2, bw / 2, s - br - bw / 2)
    ctx.lineTo(bw / 2, br + bw / 2)
    ctx.quadraticCurveTo(bw / 2, bw / 2, br + bw / 2, bw / 2)
    ctx.closePath()
    ctx.strokeStyle = fgColor.value; ctx.lineWidth = bw; ctx.stroke()
    ctx.restore()
  }

  // image mode
  if (mode.value === 'image' && uploadedImage.value) {
    const img = uploadedImage.value
    const scale = Math.min(s / img.width, s / img.height)
    const iw = img.width * scale, ih = img.height * scale
    ctx.drawImage(img, (s - iw) / 2, (s - ih) / 2, iw, ih)
    return
  }

  // text mode
  const t = text.value
  if (!t) return
  const charCount = t.length
  const baseFs = s * 0.42 * (sizeSliders[2].value / 100)
  const fs = charCount > 1 ? baseFs * 0.85 : baseFs
  ctx.fillStyle = fgColor.value
  ctx.textAlign = 'center'; ctx.textBaseline = 'middle'
  ctx.font = fontWeight.value + ' ' + fs + 'px ' + fontFamily.value
  const sp = sizeSliders[3].value
  if (charCount === 1) {
    ctx.fillText(t, s / 2, s / 2)
  } else {
    const chars = t.split('')
    const widths = chars.map(ch => ctx.measureText(ch).width)
    const totalW = widths.reduce((a, b) => a + b, 0) + sp * (charCount - 1)
    let cx = (s - totalW) / 2
    chars.forEach((ch, i) => {
      ctx.fillText(ch, cx + widths[i] / 2, s / 2)
      cx += widths[i] + sp
    })
  }
}

function applyScheme(s) {
  activeScheme.value = s.label
  bgColor.value = s.bg; fgColor.value = s.fg; draw()
}

function onImageSelect(e) {
  if (e.target.files[0]) loadImage(e.target.files[0])
}

function onImageDrop(e) {
  dragActive.value = false
  if (e.dataTransfer.files[0]) loadImage(e.dataTransfer.files[0])
}

function loadImage(file) {
  if (!file.type.startsWith('image/')) { toast('请上传图片文件', 'error'); return }
  const reader = new FileReader()
  reader.onload = e => {
    const img = new Image()
    img.onload = () => { uploadedImage.value = img; uploadedImageSrc.value = e.target.result; draw(); toast('图片已加载', 'success') }
    img.src = e.target.result
  }
  reader.readAsDataURL(file)
}

function clearImage() {
  uploadedImage.value = null; uploadedImageSrc.value = ''
  draw(); toast('已移除图片', 'success')
}

function download() {
  const c = canvas.value; if (!c) return
  const a = document.createElement('a')
  a.download = (mode.value === 'image' ? 'image' : (text.value || 'icon')) + '.png'
  a.href = c.toDataURL('image/png'); a.click()
  toast('已保存到下载目录', 'success')
}

async function copyClipboard() {
  const c = canvas.value; if (!c) return
  try {
    const blob = await new Promise(r => c.toBlob(r, 'image/png'))
    await navigator.clipboard.write([new ClipboardItem({ 'image/png': blob })])
    toast('已复制到剪贴板', 'success')
  } catch { toast('复制失败，请重试', 'error') }
}

onMounted(() => { nextTick(draw) })
</script>
