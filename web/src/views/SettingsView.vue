<template>
  <div>
    <div class="bento-grid">
      <div class="card"><div class="card-title">设备</div><div style="font-size:14px;font-weight:600;">{{ device.model || '---' }}</div><div style="font-size:12px;color:var(--text-muted);">{{ device.manufacturer || '' }} | Android {{ device.release || '' }}</div></div>
      <div class="card"><div class="card-title">电池</div><div style="display:flex;align-items:center;gap:8px;"><div class="card-value" style="font-size:18px;">{{ battery.level || 0 }}%</div><div style="flex:1;"><div class="progress-bar"><div class="progress-fill" :style="{ width: (battery.level||0)+'%', background: batteryColor }"></div></div></div></div><div style="font-size:11px;color:var(--text-muted);margin-top:4px;">{{ battery.statusText || '' }} | {{ battery.healthText || '' }}</div></div>
      <div class="card"><div class="card-title">存储</div><div class="card-value" style="font-size:16px;">{{ storage.usedHuman || '---' }} / {{ storage.totalHuman || '---' }}</div><div class="progress-bar" style="margin-top:6px;"><div class="progress-fill" :style="{ width: storagePct+'%' }"></div></div><div style="font-size:11px;color:var(--text-muted);margin-top:4px;">{{ storage.availableHuman || '' }} 剩余</div></div>
      <div class="card"><div class="card-title">WiFi</div><div class="card-value" style="font-size:16px;">{{ wifi.ssid || '未连接' }}</div><div style="font-size:11px;color:var(--text-muted);">{{ wifi.stateText || '' }} {{ wifi.rssi ? wifi.rssi + ' dBm' : '' }}</div></div>
    </div>

    <h2 style="margin:20px 0 10px;font-size:15px;">音量</h2>
    <div class="card">
      <div v-for="s in volumeStreams" :key="s.key" class="row" style="flex-wrap:wrap;gap:8px;">
        <div style="width:50px;font-size:12px;color:var(--text-muted);">{{ s.label }}</div>
        <input type="range" min="0" :max="s.max" :value="volume[s.key] || 0"
          @input="e => setVolume(s.key, e.target.value)" style="flex:1;min-width:0;" />
        <div style="width:24px;font-size:12px;text-align:right;color:var(--text-muted);">{{ volume[s.key] || 0 }}</div>
      </div>
    </div>

    <h2 style="margin:20px 0 10px;font-size:15px;">亮度</h2>
    <div class="card">
      <div class="row">
        <input type="range" min="0" max="255" :value="brightness" @input="e => setBrightness(e.target.value)" style="flex:1;" />
        <div style="width:32px;font-size:12px;text-align:right;color:var(--text-muted);">{{ brightness }}</div>
      </div>
    </div>

    <h2 style="margin:20px 0 10px;font-size:15px;">屏幕旋转</h2>
    <div class="card">
      <div class="row" style="justify-content:space-between;">
        <div>
          <div style="font-size:14px;font-weight:600;">自动旋转</div>
          <div style="font-size:12px;color:var(--text-muted);">{{ autoRotate ? '已开启' : '已关闭' }}</div>
        </div>
        <div class="toggle" :class="{ active: autoRotate }" @click="toggleRotate"></div>
      </div>
    </div>

    <h2 style="margin:20px 0 10px;font-size:15px;">系统设置</h2>
    <div class="card" style="padding:0;">
      <div v-for="s in settingsLinks" :key="s.action" class="row" style="cursor:pointer;" @click="openSettings(s.action)">
        <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path :d="s.icon"/></svg>
        <div class="row-text"><div class="row-title">{{ s.name }}</div></div>
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="var(--text-muted)" stroke-width="2"><polyline points="9 18 15 12 9 6"/></svg>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { getJSON, postAction, toast } from '../api'

const device = ref({})
const battery = ref({})
const storage = ref({})
const wifi = ref({})
const volume = ref({})
const brightness = ref(128)
const autoRotate = ref(false)

const batteryColor = computed(() => {
  const l = battery.value.level || 0
  return l > 50 ? '#1e8e3e' : l > 20 ? '#f9ab00' : '#d93025'
})
const storagePct = computed(() => {
  const t = storage.value.total || 0
  return t > 0 ? Math.round(storage.value.used / t * 100) : 0
})

const volumeStreams = [
  { key: 'music', label: '媒体', max: 15 },
  { key: 'ring', label: '铃声', max: 7 },
  { key: 'notification', label: '通知', max: 7 },
  { key: 'alarm', label: '闹钟', max: 7 }
]

const settingsLinks = [
  { name: 'WiFi', action: 'android.settings.WIFI_SETTINGS', icon: 'M5 12.55a11 11 0 0 1 14.08 0M1.42 9a16 16 0 0 1 21.16 0M8.53 16.11a6 6 0 0 1 6.95 0M12 20h.01' },
  { name: '蓝牙', action: 'android.settings.BLUETOOTH_SETTINGS', icon: 'M6.5 6.5l11 11M12 2v20M17 7l-5 5-5-5' },
  { name: '显示', action: 'android.settings.DISPLAY_SETTINGS', icon: 'M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z' },
  { name: '声音', action: 'android.settings.SOUND_SETTINGS', icon: 'M9 18V5l12-3v13M9 19c0 1.1-1.3 2-3 2s-3-.9-3-2 1.3-2 3-2 3 .9 3 2z' },
  { name: '应用', action: 'android.settings.APPLICATION_SETTINGS', icon: 'M4 4h16v16H4z' },
  { name: '开发者选项', action: 'android.settings.APPLICATION_DEVELOPMENT_SETTINGS', icon: 'M16 18l6-6-6-6M8 6l-6 6 6 6' },
  { name: '电池', action: 'android.settings.BATTERY_SAVER_SETTINGS', icon: 'M13 2L3 14h9l-1 8 10-12h-9l1-8z' },
  { name: '存储', action: 'android.settings.INTERNAL_STORAGE_SETTINGS', icon: 'M21 16V8a2 2 0 0 0-1-1.73l-7-4a2 2 0 0 0-2 0l-7 4A2 2 0 0 0 3 8v8a2 2 0 0 0 1 1.73l7 4a2 2 0 0 0 2 0l7-4A2 2 0 0 0 21 16z' },
  { name: '通知', action: 'android.settings.NOTIFICATION_LISTENER_SETTINGS', icon: 'M18 8A6 6 0 0 0 6 8c0 7-3 9-3 9h18s-3-2-3-9M13.73 21a2 2 0 0 1-3.46 0' },
  { name: '位置', action: 'android.settings.LOCATION_SOURCE_SETTINGS', icon: 'M21 10c0 7-9 13-9 13s-9-6-9-13a9 9 0 0 1 18 0z' },
  { name: '安全', action: 'android.settings.SECURITY_SETTINGS', icon: 'M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z' },
  { name: '关于', action: 'android.settings.DEVICE_INFO_SETTINGS', icon: 'M12 22c5.5 0 10-4.5 10-10S17.5 2 12 2 2 6.5 2 12s4.5 10 10 10zM12 16v-4M12 8h.01' }
]

async function loadAll() {
  try {
    const [d, b, s, w, v, br, r] = await Promise.all([
      getJSON('/api/device'), getJSON('/api/battery'), getJSON('/api/storage'),
      getJSON('/api/wifi-status'), getJSON('/api/volume'), getJSON('/api/brightness'), getJSON('/api/rotation')
    ])
    device.value = d; battery.value = b; storage.value = s; wifi.value = w
    volume.value = v; brightness.value = br.value || 128; autoRotate.value = r.enabled || false
  } catch {}
}

async function setVolume(stream, val) {
  volume.value[stream] = parseInt(val)
  try { await postAction('/api/volume?stream=' + stream + '&value=' + val) } catch {}
}

async function setBrightness(val) {
  brightness.value = parseInt(val)
  try { await postAction('/api/brightness?value=' + val) } catch {}
}

async function toggleRotate() {
  autoRotate.value = !autoRotate.value
  try { await postAction('/api/rotation?enabled=' + autoRotate.value) } catch {}
}

async function openSettings(action) {
  try {
    const r = await postAction('/api/open-settings?action=' + encodeURIComponent(action))
    if (!r.success) toast(r.error || '失败', 'error')
  } catch { toast('失败', 'error') }
}

onMounted(loadAll)
</script>
