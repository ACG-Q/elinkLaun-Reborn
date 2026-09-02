import { createRouter, createWebHashHistory } from 'vue-router'

const routes = [
  { path: '/', name: 'home', component: () => import('../views/HomeView.vue') },
  { path: '/fm', name: 'fm', component: () => import('../views/FileManagerView.vue') },
  { path: '/apk', name: 'apk', component: () => import('../views/ApkManagerView.vue') },
  { path: '/icons', name: 'icons', component: () => import('../views/IconManagerView.vue') },
  { path: '/settings', name: 'settings', component: () => import('../views/SettingsView.vue') },
  { path: '/icon-gen', name: 'icon-gen', component: () => import('../views/IconGenView.vue') }
]

const router = createRouter({
  history: createWebHashHistory(),
  routes
})

export default router
