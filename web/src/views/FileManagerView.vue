<template>
  <div>
    <div class="card" style="font-size:12px;color:var(--text-muted);">
      <div style="display:flex;align-items:center;gap:6px;">
        <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M22 19a2 2 0 0 1-2 2H4a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h5l2 3h9a2 2 0 0 1 2 2z"/></svg>
        <span>{{ currentPath }}</span>
      </div>
    </div>

    <div class="card" style="padding:0;">
      <router-link v-if="parentPath && parentPath !== currentPath"
        :to="{ path: '/fm', query: { path: parentPath } }"
        class="row" style="text-decoration:none;color:var(--text);">
        <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="15 18 9 12 15 6"/></svg>
        <div class="row-text"><span class="row-title" style="color:var(--primary);">..</span></div>
      </router-link>
      <div v-for="f in items" :key="f.path" class="row">
        <template v-if="f.isDir">
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="var(--primary)" stroke-width="2"><path d="M22 19a2 2 0 0 1-2 2H4a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h5l2 3h9a2 2 0 0 1 2 2z"/></svg>
          <div class="row-text">
            <router-link :to="{ path: '/fm', query: { path: f.path } }" class="row-title" style="text-decoration:none;">{{ f.name }}</router-link>
          </div>
        </template>
        <template v-else>
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="var(--text-muted)" stroke-width="2"><path d="M13 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V9z"/><polyline points="13 2 13 9 20 9"/></svg>
          <div class="row-text">
            <a :href="f.path" download class="row-title" style="text-decoration:none;">{{ f.name }}</a>
            <div class="row-sub">{{ f.sizeHuman }}</div>
          </div>
        </template>
        <div class="row-action">
          <button class="btn btn-sm btn-danger" @click="deleteFile(f)">删除</button>
        </div>
      </div>
      <div v-if="items.length === 0 && !loading" class="empty-state">空目录</div>
    </div>

    <div class="upload-area" id="uploadZone" @click="$refs.fileInput.click()" @dragover.prevent @drop.prevent="onDrop">
      <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="var(--text-muted)" stroke-width="2"><path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/><polyline points="17 8 12 3 7 8"/><line x1="12" y1="3" x2="12" y2="15"/></svg>
      <div style="font-size:13px;font-weight:500;">点击或拖拽上传文件</div>
      <input ref="fileInput" type="file" multiple style="display:none;" @change="onFileSelect" />
      <div v-if="uploading" class="upload-progress">
        <div class="upload-progress-bar" :style="{ width: uploadPct + '%' }"></div>
        <div class="upload-progress-text">{{ uploadPct }}%</div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, watch } from 'vue'
import { useRoute } from 'vue-router'
import { getJSON, deleteAction, uploadFile, toast } from '../api'

const route = useRoute()
const items = ref([])
const currentPath = ref('/sdcard')
const parentPath = ref('')
const loading = ref(false)
const uploading = ref(false)
const uploadPct = ref(0)

async function loadFiles() {
  loading.value = true
  currentPath.value = route.query.path || '/sdcard'
  try {
    const data = await getJSON('/api/files?path=' + encodeURIComponent(currentPath.value))
    items.value = data.items || []
    parentPath.value = data.parentPath || ''
  } catch { items.value = [] }
  loading.value = false
}

async function deleteFile(f) {
  if (!confirm('删除 "' + f.name + '"?')) return
  try {
    const r = await deleteAction('/api/files?path=' + encodeURIComponent(f.path))
    toast(r.success ? '已删除' : '删除失败', r.success ? 'success' : 'error')
    if (r.success) loadFiles()
  } catch { toast('删除失败', 'error') }
}

async function onFileSelect(e) {
  const files = e.target.files
  if (!files.length) return
  await uploadFiles(files)
}

async function onDrop(e) {
  const files = e.dataTransfer.files
  if (!files.length) return
  await uploadFiles(files)
}

async function uploadFiles(files) {
  for (const file of files) {
    uploading.value = true
    uploadPct.value = 0
    try {
      await uploadFile('/upload/' + file.name, file, {
        targetPath: currentPath.value,
        onProgress: pct => { uploadPct.value = pct }
      })
      toast('已上传: ' + file.name, 'success')
    } catch { toast('上传失败: ' + file.name, 'error') }
  }
  uploading.value = false
  loadFiles()
}

watch(() => route.query.path, loadFiles)
onMounted(loadFiles)
</script>
