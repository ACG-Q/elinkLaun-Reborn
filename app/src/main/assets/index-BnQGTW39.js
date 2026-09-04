const BASE = "";
async function request(url, options = {}) {
  const res = await fetch(BASE + url, options);
  if (!res.ok) throw new Error(`HTTP ${res.status}`);
  return res;
}
async function getJSON(url) {
  const res = await request(url);
  return res.json();
}
async function postAction(url) {
  const res = await request(url, { method: "POST" });
  return res.json();
}
async function deleteAction(url) {
  const res = await request(url, { method: "DELETE" });
  return res.json();
}
async function uploadFile(path, file, options = {}) {
  const onProgress = options.onProgress;
  const action = options.action || "file";
  const targetPath = options.targetPath || "";
  const pkg = options.pkg || "";
  const startRes = await request("/api/upload/start", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({
      filename: file.name,
      size: file.size,
      action,
      targetPath,
      pkg
    })
  });
  const startData = await startRes.json();
  if (!startData.success) throw new Error(startData.error || "Failed to start upload");
  const sessionId = startData.sessionId;
  const totalChunks = startData.totalChunks;
  const chunkSize = startData.chunkSize;
  let uploaded = 0;
  for (let i = 0; i < totalChunks; i++) {
    const start = i * chunkSize;
    const end = Math.min(start + chunkSize, file.size);
    const chunk = file.slice(start, end);
    await new Promise((resolve, reject) => {
      const xhr = new XMLHttpRequest();
      xhr.open("POST", "/api/upload/chunk?sessionId=" + sessionId + "&chunkIndex=" + i);
      xhr.setRequestHeader("Content-Type", "application/octet-stream");
      xhr.responseType = "json";
      xhr.onload = () => {
        if (xhr.response && xhr.response.success) {
          uploaded++;
          if (onProgress) onProgress(Math.round(uploaded / totalChunks * 100));
          resolve();
        } else {
          reject(new Error(xhr.response ? xhr.response.error : "Chunk upload failed"));
        }
      };
      xhr.onerror = () => reject(new Error("Network error"));
      xhr.send(chunk);
    });
  }
  const completeRes = await request("/api/upload/complete?sessionId=" + sessionId, { method: "POST" });
  return completeRes.json();
}
function toast(msg, type = "info") {
  const el = document.createElement("div");
  el.className = `toast toast-${type}`;
  el.textContent = msg;
  document.body.appendChild(el);
  requestAnimationFrame(() => el.classList.add("show"));
  setTimeout(() => {
    el.classList.remove("show");
    setTimeout(() => el.remove(), 2e3);
  }, 2e3);
}
export {
  deleteAction as d,
  getJSON as g,
  postAction as p,
  toast as t,
  uploadFile as u
};
