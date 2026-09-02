const BASE = ''

async function request(url, options = {}) {
  const res = await fetch(BASE + url, options)
  if (!res.ok) throw new Error(`HTTP ${res.status}`)
  return res
}

export async function getJSON(url) {
  const res = await request(url)
  return res.json()
}

export async function postAction(url) {
  const res = await request(url, { method: 'POST' })
  return res.json()
}

export async function deleteAction(url) {
  const res = await request(url, { method: 'DELETE' })
  return res.json()
}

export async function uploadFile(path, file, onProgress) {
  const CHUNK_SIZE = 256 * 1024
  const totalChunks = Math.ceil(file.size / CHUNK_SIZE)

  const startRes = await request('/api/upload/start', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ path, totalChunks, fileName: file.name })
  })
  const { sessionId } = await startRes.json()

  for (let i = 0; i < totalChunks; i++) {
    const start = i * CHUNK_SIZE
    const end = Math.min(start + CHUNK_SIZE, file.size)
    const chunk = file.slice(start, end)

    const formData = new FormData()
    formData.append('sessionId', sessionId)
    formData.append('index', i)
    formData.append('chunk', chunk)

    await request('/api/upload/chunk', { method: 'POST', body: formData })
    if (onProgress) onProgress(Math.round(((i + 1) / totalChunks) * 100))
  }

  const completeRes = await request('/api/upload/complete', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ sessionId })
  })
  return completeRes.json()
}

export function toast(msg, type = 'info') {
  const el = document.createElement('div')
  el.className = `toast toast-${type}`
  el.textContent = msg
  document.body.appendChild(el)
  requestAnimationFrame(() => el.classList.add('show'))
  setTimeout(() => {
    el.classList.remove('show')
    setTimeout(() => el.remove(), 300)
  }, 2000)
}
