<template>
  <div>
    <div class="bento-grid">
      <div class="card">
        <div class="card-title">文件</div>
        <div class="card-value">{{ stats.fileCount || 0 }}</div>
      </div>
      <div class="card">
        <div class="card-title">存储</div>
        <div class="card-value" style="font-size:18px;">{{ stats.totalSizeHuman || '---' }}</div>
      </div>
      <div class="card">
        <div class="card-title">电池</div>
        <div class="card-value">{{ batteryLevel }}%</div>
      </div>
      <div class="card">
        <div class="card-title">WiFi</div>
        <div class="card-value" style="font-size:16px;">{{ wifiName || '未连接' }}</div>
      </div>
    </div>
    <div class="grid">
      <router-link to="/fm" class="grid-card">
        <svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M22 19a2 2 0 0 1-2 2H4a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h5l2 3h9a2 2 0 0 1 2 2z"/></svg>
        <div class="label">文件管理</div>
        <div class="desc">浏览和管理文件</div>
      </router-link>
      <router-link to="/apk" class="grid-card">
        <svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect x="5" y="2" width="14" height="20" rx="2" ry="2"/><line x1="12" y1="18" x2="12.01" y2="18"/></svg>
        <div class="label">APK 管理</div>
        <div class="desc">安装和管理应用</div>
      </router-link>
      <router-link to="/icons" class="grid-card">
        <svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect x="3" y="3" width="18" height="18" rx="2" ry="2"/><circle cx="8.5" cy="8.5" r="1.5"/><polyline points="21 15 16 10 5 21"/></svg>
        <div class="label">图标管理</div>
        <div class="desc">自定义应用图标</div>
      </router-link>
      <router-link to="/settings" class="grid-card">
        <svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="3"/><path d="M19.4 15a1.65 1.65 0 0 0 .33 1.82l.06.06a2 2 0 0 1 0 2.83 2 2 0 0 1-2.83 0l-.06-.06a1.65 1.65 0 0 0-1.82-.33 1.65 1.65 0 0 0-1 1.51V21a2 2 0 0 1-2 2 2 2 0 0 1-2-2v-.09A1.65 1.65 0 0 0 9 19.4a1.65 1.65 0 0 0-1.82.33l-.06.06a2 2 0 0 1-2.83 0 2 2 0 0 1 0-2.83l.06-.06A1.65 1.65 0 0 0 4.68 15a1.65 1.65 0 0 0-1.51-1H3a2 2 0 0 1-2-2 2 2 0 0 1 2-2h.09A1.65 1.65 0 0 0 4.6 9a1.65 1.65 0 0 0-.33-1.82l-.06-.06a2 2 0 0 1 0-2.83 2 2 0 0 1 2.83 0l.06.06A1.65 1.65 0 0 0 9 4.68a1.65 1.65 0 0 0 1-1.51V3a2 2 0 0 1 2-2 2 2 0 0 1 2 2v.09a1.65 1.65 0 0 0 1 1.51 1.65 1.65 0 0 0 1.82-.33l.06-.06a2 2 0 0 1 2.83 0 2 2 0 0 1 0 2.83l-.06.06A1.65 1.65 0 0 0 19.4 9a1.65 1.65 0 0 0 1.51 1H21a2 2 0 0 1 2 2 2 2 0 0 1-2 2h-.09a1.65 1.65 0 0 0-1.51 1z"/></svg>
        <div class="label">系统设置</div>
        <div class="desc">设备参数调节</div>
      </router-link>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getJSON } from '../api'

const stats = ref({})
const batteryLevel = ref(0)
const wifiName = ref('')

onMounted(async () => {
  try {
    const [s, b, w] = await Promise.all([
      getJSON('/api/stats'),
      getJSON('/api/battery'),
      getJSON('/api/wifi-status')
    ])
    stats.value = s
    batteryLevel.value = b.level || 0
    wifiName.value = w.ssid || ''
  } catch {}
})
</script>
