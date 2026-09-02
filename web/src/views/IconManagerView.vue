<template>
  <div>
    <div class="search-bar">
      <input class="form-input" placeholder="搜索应用..." v-model="query" @input="filterIcons" />
    </div>

    <router-link to="/icon-gen" class="btn btn-primary" style="width:100%;display:flex;align-items:center;justify-content:center;gap:8px;text-decoration:none;margin-bottom:12px;">
      <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/><line x1="12" y1="8" x2="12" y2="16"/><line x1="8" y1="12" x2="16" y2="12"/></svg>
      圆角图片生成器
    </router-link>

    <div class="icon-grid">
      <div v-for="item in filtered" :key="item.packageName" class="icon-slot"
        @dragover.prevent @dragenter.prevent="dragPkg=item.packageName" @dragleave="dragPkg=''"
        @drop.prevent="onDrop($event, item.packageName)"
        @click="clickSlot(item.packageName)">
        <img :src="'/api/app-icon?pkg=' + encodeURIComponent(item.packageName)"
          style="width:100%;height:100%;object-fit:cover;border-radius:12px;position:absolute;top:0;left:0;" />
        <div class="icon-slot-name">{{ item.name }}</div>
        <div v-if="item.hasCustomIcon" class="icon-slot-badge">Custom</div>
        <input type="file" accept="image/*" :ref="el => fileInputs[item.packageName] = el" style="display:none;"
          @change="e => replaceIcon(item.packageName, e)" />
      </div>
    </div>

    <div v-if="filtered.length === 0 && !loading" class="empty-state">暂无应用</div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { getJSON, uploadFile, toast } from '../api'

const icons = ref([])
const filtered = ref([])
const query = ref('')
const loading = ref(false)
const dragPkg = ref('')
const fileInputs = reactive({})

async function loadIcons() {
  loading.value = true
  try {
    const data = await getJSON('/api/icons')
    icons.value = data.items || []
    filtered.value = icons.value
  } catch { icons.value = []; filtered.value = [] }
  loading.value = false
}

function filterIcons() {
  const q = query.value.toLowerCase()
  filtered.value = icons.value.filter(a =>
    a.name.toLowerCase().includes(q) || a.packageName.toLowerCase().includes(q)
  )
}

function clickSlot(pkg) {
  const input = fileInputs[pkg]
  if (input) input.click()
}

async function replaceIcon(pkg, e) {
  const file = e.target.files[0]
  if (!file) return
  try {
    await uploadFile('/upload/' + file.name, file, {
      action: 'icon-replace',
      pkg: pkg
    })
    toast('图标已上传', 'success')
    loadIcons()
  } catch { toast('上传失败', 'error') }
}

async function onDrop(e, pkg) {
  dragPkg.value = ''
  const file = e.dataTransfer.files[0]
  if (!file) return
  try {
    await uploadFile('/upload/' + file.name, file, {
      action: 'icon-replace',
      pkg: pkg
    })
    toast('图标已上传', 'success')
    loadIcons()
  } catch { toast('上传失败', 'error') }
}

onMounted(loadIcons)
</script>
