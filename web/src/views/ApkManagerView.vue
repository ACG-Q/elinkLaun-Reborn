<template>
  <div>
    <div class="search-bar">
      <input class="form-input" placeholder="搜索应用..." v-model="query" @input="filterApps" />
    </div>

    <div class="app-grid">
      <div v-for="app in filtered" :key="app.packageName" class="app-item">
        <img class="app-icon" :src="'/api/app-icon?pkg=' + encodeURIComponent(app.packageName)" @error="e => e.target.style.display='none'" />
        <div class="app-name">{{ app.name }}</div>
        <div class="app-pkg">{{ app.packageName }}</div>
        <div class="app-actions" v-if="!app.isVirtual">
          <button class="btn btn-sm" @click="openApp(app.packageName)">打开</button>
          <button class="btn btn-sm btn-danger" @click="uninstallApp(app)">卸载</button>
        </div>
      </div>
    </div>

    <div class="card" style="margin-top:16px;">
      <div class="card-title">安装 APK</div>
      <div class="upload-area" @click="$refs.apkInput.click()" @dragover.prevent @drop.prevent="onDrop">
        <svg width="32" height="32" viewBox="0 0 24 24" fill="none" stroke="var(--text-muted)" stroke-width="1.5"><path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/><polyline points="17 8 12 3 7 8"/><line x1="12" y1="3" x2="12" y2="15"/></svg>
        <div style="font-size:13px;color:var(--text-muted);">点击或拖拽 APK 文件</div>
        <input ref="apkInput" type="file" accept=".apk" style="display:none;" @change="onApkSelect" />
        <div v-if="uploading" class="upload-progress">
          <div class="upload-progress-bar" :style="{ width: uploadPct + '%' }"></div>
          <div class="upload-progress-text">{{ uploadPct }}%</div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getJSON, postAction, uploadFile, toast } from '../api'

const apps = ref([])
const filtered = ref([])
const query = ref('')
const uploading = ref(false)
const uploadPct = ref(0)

async function loadApps() {
  try {
    const data = await getJSON('/api/apps')
    apps.value = data.items || []
    filtered.value = apps.value
  } catch { apps.value = []; filtered.value = [] }
}

function filterApps() {
  const q = query.value.toLowerCase()
  filtered.value = apps.value.filter(a =>
    a.name.toLowerCase().includes(q) || a.packageName.toLowerCase().includes(q)
  )
}

async function openApp(pkg) {
  try {
    const d = await postAction('/api/app-open?pkg=' + encodeURIComponent(pkg))
    toast(d.success ? '已打开' : (d.error || '失败'), d.success ? 'success' : 'error')
  } catch { toast('失败', 'error') }
}

async function uninstallApp(app) {
  if (!confirm('卸载 ' + app.name + '?')) return
  try {
    const d = await postAction('/api/app-uninstall?pkg=' + encodeURIComponent(app.packageName))
    toast(d.success ? '卸载已开始' : (d.error || '失败'), d.success ? 'success' : 'error')
  } catch { toast('失败', 'error') }
}

async function onApkSelect(e) {
  if (e.target.files.length) await installApk(e.target.files[0])
}

async function onDrop(e) {
  if (e.dataTransfer.files.length) await installApk(e.dataTransfer.files[0])
}

async function installApk(file) {
  uploading.value = true
  uploadPct.value = 0
  try {
    const r = await uploadFile('/upload/' + file.name, file, {
      targetPath: '/sdcard/Download',
      action: 'install',
      onProgress: pct => { uploadPct.value = pct }
    })
    const r2 = await postAction('/api/app-install?path=' + encodeURIComponent(r.path))
    toast(r2.success ? '安装已开始' : (r2.error || '失败'), r2.success ? 'success' : 'error')
  } catch { toast('上传/安装失败', 'error') }
  uploading.value = false
}

onMounted(loadApps)
</script>
