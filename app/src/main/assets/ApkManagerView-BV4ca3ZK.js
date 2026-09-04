import { o as onMounted, a as openBlock, c as createElementBlock, b as createBaseVNode, m as withDirectives, v as vModelText, F as Fragment, i as renderList, t as toDisplayString, h as createCommentVNode, l as withModifiers, k as createStaticVNode, n as normalizeStyle, r as ref } from "./index-IHZWDMzB.js";
import { g as getJSON, p as postAction, t as toast, u as uploadFile } from "./index-BnQGTW39.js";
const _hoisted_1 = { class: "search-bar" };
const _hoisted_2 = { class: "app-grid" };
const _hoisted_3 = ["src"];
const _hoisted_4 = { class: "app-name" };
const _hoisted_5 = { class: "app-pkg" };
const _hoisted_6 = {
  key: 0,
  class: "app-actions"
};
const _hoisted_7 = ["onClick"];
const _hoisted_8 = ["onClick"];
const _hoisted_9 = {
  class: "card",
  style: { "margin-top": "16px" }
};
const _hoisted_10 = {
  key: 0,
  class: "upload-progress"
};
const _hoisted_11 = { class: "upload-progress-text" };
const _sfc_main = {
  __name: "ApkManagerView",
  setup(__props) {
    const apps = ref([]);
    const filtered = ref([]);
    const query = ref("");
    const uploading = ref(false);
    const uploadPct = ref(0);
    async function loadApps() {
      try {
        const data = await getJSON("/api/apps");
        apps.value = data.items || [];
        filtered.value = apps.value;
      } catch {
        apps.value = [];
        filtered.value = [];
      }
    }
    function filterApps() {
      const q = query.value.toLowerCase();
      filtered.value = apps.value.filter(
        (a) => a.name.toLowerCase().includes(q) || a.packageName.toLowerCase().includes(q)
      );
    }
    async function openApp(pkg) {
      try {
        const d = await postAction("/api/app-open?pkg=" + encodeURIComponent(pkg));
        toast(d.success ? "已打开" : d.error || "失败", d.success ? "success" : "error");
      } catch {
        toast("失败", "error");
      }
    }
    async function uninstallApp(app) {
      if (!confirm("卸载 " + app.name + "?")) return;
      try {
        const d = await postAction("/api/app-uninstall?pkg=" + encodeURIComponent(app.packageName));
        toast(d.success ? "卸载已开始" : d.error || "失败", d.success ? "success" : "error");
      } catch {
        toast("失败", "error");
      }
    }
    async function onApkSelect(e) {
      if (e.target.files.length) await installApk(e.target.files[0]);
    }
    async function onDrop(e) {
      if (e.dataTransfer.files.length) await installApk(e.dataTransfer.files[0]);
    }
    async function installApk(file) {
      uploading.value = true;
      uploadPct.value = 0;
      try {
        const r = await uploadFile("/upload/" + file.name, file, {
          targetPath: "/sdcard/Download",
          action: "install",
          onProgress: (pct) => {
            uploadPct.value = pct;
          }
        });
        const r2 = await postAction("/api/app-install?path=" + encodeURIComponent(r.path));
        toast(r2.success ? "安装已开始" : r2.error || "失败", r2.success ? "success" : "error");
      } catch {
        toast("上传/安装失败", "error");
      }
      uploading.value = false;
    }
    onMounted(loadApps);
    return (_ctx, _cache) => {
      return openBlock(), createElementBlock("div", null, [
        createBaseVNode("div", _hoisted_1, [
          withDirectives(createBaseVNode("input", {
            class: "form-input",
            placeholder: "搜索应用...",
            "onUpdate:modelValue": _cache[0] || (_cache[0] = ($event) => query.value = $event),
            onInput: filterApps
          }, null, 544), [
            [vModelText, query.value]
          ])
        ]),
        createBaseVNode("div", _hoisted_2, [
          (openBlock(true), createElementBlock(Fragment, null, renderList(filtered.value, (app) => {
            return openBlock(), createElementBlock("div", {
              key: app.packageName,
              class: "app-item"
            }, [
              createBaseVNode("img", {
                class: "app-icon",
                src: "/api/app-icon?pkg=" + encodeURIComponent(app.packageName),
                onError: _cache[1] || (_cache[1] = (e) => e.target.style.display = "none")
              }, null, 40, _hoisted_3),
              createBaseVNode("div", _hoisted_4, toDisplayString(app.name), 1),
              createBaseVNode("div", _hoisted_5, toDisplayString(app.packageName), 1),
              !app.isVirtual ? (openBlock(), createElementBlock("div", _hoisted_6, [
                createBaseVNode("button", {
                  class: "btn btn-sm",
                  onClick: ($event) => openApp(app.packageName)
                }, "打开", 8, _hoisted_7),
                createBaseVNode("button", {
                  class: "btn btn-sm btn-danger",
                  onClick: ($event) => uninstallApp(app)
                }, "卸载", 8, _hoisted_8)
              ])) : createCommentVNode("", true)
            ]);
          }), 128))
        ]),
        createBaseVNode("div", _hoisted_9, [
          _cache[5] || (_cache[5] = createBaseVNode("div", { class: "card-title" }, "安装 APK", -1)),
          createBaseVNode("div", {
            class: "upload-area",
            onClick: _cache[2] || (_cache[2] = ($event) => _ctx.$refs.apkInput.click()),
            onDragover: _cache[3] || (_cache[3] = withModifiers(() => {
            }, ["prevent"])),
            onDrop: withModifiers(onDrop, ["prevent"])
          }, [
            _cache[4] || (_cache[4] = createStaticVNode('<svg width="32" height="32" viewBox="0 0 24 24" fill="none" stroke="var(--text-muted)" stroke-width="1.5"><path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"></path><polyline points="17 8 12 3 7 8"></polyline><line x1="12" y1="3" x2="12" y2="15"></line></svg><div style="font-size:13px;color:var(--text-muted);">点击或拖拽 APK 文件</div>', 2)),
            createBaseVNode("input", {
              ref: "apkInput",
              type: "file",
              accept: ".apk",
              style: { "display": "none" },
              onChange: onApkSelect
            }, null, 544),
            uploading.value ? (openBlock(), createElementBlock("div", _hoisted_10, [
              createBaseVNode("div", {
                class: "upload-progress-bar",
                style: normalizeStyle({ width: uploadPct.value + "%" })
              }, null, 4),
              createBaseVNode("div", _hoisted_11, toDisplayString(uploadPct.value) + "%", 1)
            ])) : createCommentVNode("", true)
          ], 32)
        ])
      ]);
    };
  }
};
export {
  _sfc_main as default
};
