import { f as watch, o as onMounted, a as openBlock, c as createElementBlock, b as createBaseVNode, t as toDisplayString, g as createBlock, w as withCtx, h as createCommentVNode, F as Fragment, i as renderList, d as createVNode, j as createTextVNode, k as createStaticVNode, n as normalizeStyle, l as withModifiers, r as ref, e as resolveComponent, u as useRoute } from "./index-IHZWDMzB.js";
import { g as getJSON, d as deleteAction, t as toast, u as uploadFile } from "./index-BnQGTW39.js";
const _hoisted_1 = {
  class: "card",
  style: { "font-size": "12px", "color": "var(--text-muted)" }
};
const _hoisted_2 = { style: { "display": "flex", "align-items": "center", "gap": "6px" } };
const _hoisted_3 = {
  class: "card",
  style: { "padding": "0" }
};
const _hoisted_4 = { class: "row-text" };
const _hoisted_5 = { class: "row-text" };
const _hoisted_6 = ["href"];
const _hoisted_7 = { class: "row-sub" };
const _hoisted_8 = { class: "row-action" };
const _hoisted_9 = ["onClick"];
const _hoisted_10 = {
  key: 1,
  class: "empty-state"
};
const _hoisted_11 = {
  key: 0,
  class: "upload-progress"
};
const _hoisted_12 = { class: "upload-progress-text" };
const _sfc_main = {
  __name: "FileManagerView",
  setup(__props) {
    const route = useRoute();
    const items = ref([]);
    const currentPath = ref("/sdcard");
    const parentPath = ref("");
    const loading = ref(false);
    const uploading = ref(false);
    const uploadPct = ref(0);
    async function loadFiles() {
      loading.value = true;
      currentPath.value = route.query.path || "/sdcard";
      try {
        const data = await getJSON("/api/files?path=" + encodeURIComponent(currentPath.value));
        items.value = data.items || [];
        parentPath.value = data.parentPath || "";
      } catch {
        items.value = [];
      }
      loading.value = false;
    }
    async function deleteFile(f) {
      if (!confirm('删除 "' + f.name + '"?')) return;
      try {
        const r = await deleteAction("/api/files?path=" + encodeURIComponent(f.path));
        toast(r.success ? "已删除" : "删除失败", r.success ? "success" : "error");
        if (r.success) loadFiles();
      } catch {
        toast("删除失败", "error");
      }
    }
    async function onFileSelect(e) {
      const files = e.target.files;
      if (!files.length) return;
      await uploadFiles(files);
    }
    async function onDrop(e) {
      const files = e.dataTransfer.files;
      if (!files.length) return;
      await uploadFiles(files);
    }
    async function uploadFiles(files) {
      for (const file of files) {
        uploading.value = true;
        uploadPct.value = 0;
        try {
          await uploadFile("/upload/" + file.name, file, {
            targetPath: currentPath.value,
            onProgress: (pct) => {
              uploadPct.value = pct;
            }
          });
          toast("已上传: " + file.name, "success");
        } catch {
          toast("上传失败: " + file.name, "error");
        }
      }
      uploading.value = false;
      loadFiles();
    }
    watch(() => route.query.path, loadFiles);
    onMounted(loadFiles);
    return (_ctx, _cache) => {
      const _component_router_link = resolveComponent("router-link");
      return openBlock(), createElementBlock("div", null, [
        createBaseVNode("div", _hoisted_1, [
          createBaseVNode("div", _hoisted_2, [
            _cache[2] || (_cache[2] = createBaseVNode("svg", {
              width: "14",
              height: "14",
              viewBox: "0 0 24 24",
              fill: "none",
              stroke: "currentColor",
              "stroke-width": "2"
            }, [
              createBaseVNode("path", { d: "M22 19a2 2 0 0 1-2 2H4a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h5l2 3h9a2 2 0 0 1 2 2z" })
            ], -1)),
            createBaseVNode("span", null, toDisplayString(currentPath.value), 1)
          ])
        ]),
        createBaseVNode("div", _hoisted_3, [
          parentPath.value && parentPath.value !== currentPath.value ? (openBlock(), createBlock(_component_router_link, {
            key: 0,
            to: { path: "/fm", query: { path: parentPath.value } },
            class: "row",
            style: { "text-decoration": "none", "color": "var(--text)" }
          }, {
            default: withCtx(() => [..._cache[3] || (_cache[3] = [
              createBaseVNode("svg", {
                width: "20",
                height: "20",
                viewBox: "0 0 24 24",
                fill: "none",
                stroke: "currentColor",
                "stroke-width": "2"
              }, [
                createBaseVNode("polyline", { points: "15 18 9 12 15 6" })
              ], -1),
              createBaseVNode("div", { class: "row-text" }, [
                createBaseVNode("span", {
                  class: "row-title",
                  style: { "color": "var(--primary)" }
                }, "..")
              ], -1)
            ])]),
            _: 1
          }, 8, ["to"])) : createCommentVNode("", true),
          (openBlock(true), createElementBlock(Fragment, null, renderList(items.value, (f) => {
            return openBlock(), createElementBlock("div", {
              key: f.path,
              class: "row"
            }, [
              f.isDir ? (openBlock(), createElementBlock(Fragment, { key: 0 }, [
                _cache[4] || (_cache[4] = createBaseVNode("svg", {
                  width: "20",
                  height: "20",
                  viewBox: "0 0 24 24",
                  fill: "none",
                  stroke: "var(--primary)",
                  "stroke-width": "2"
                }, [
                  createBaseVNode("path", { d: "M22 19a2 2 0 0 1-2 2H4a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h5l2 3h9a2 2 0 0 1 2 2z" })
                ], -1)),
                createBaseVNode("div", _hoisted_4, [
                  createVNode(_component_router_link, {
                    to: { path: "/fm", query: { path: f.path } },
                    class: "row-title",
                    style: { "text-decoration": "none" }
                  }, {
                    default: withCtx(() => [
                      createTextVNode(toDisplayString(f.name), 1)
                    ]),
                    _: 2
                  }, 1032, ["to"])
                ])
              ], 64)) : (openBlock(), createElementBlock(Fragment, { key: 1 }, [
                _cache[5] || (_cache[5] = createBaseVNode("svg", {
                  width: "20",
                  height: "20",
                  viewBox: "0 0 24 24",
                  fill: "none",
                  stroke: "var(--text-muted)",
                  "stroke-width": "2"
                }, [
                  createBaseVNode("path", { d: "M13 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V9z" }),
                  createBaseVNode("polyline", { points: "13 2 13 9 20 9" })
                ], -1)),
                createBaseVNode("div", _hoisted_5, [
                  createBaseVNode("a", {
                    href: f.path,
                    download: "",
                    class: "row-title",
                    style: { "text-decoration": "none" }
                  }, toDisplayString(f.name), 9, _hoisted_6),
                  createBaseVNode("div", _hoisted_7, toDisplayString(f.sizeHuman), 1)
                ])
              ], 64)),
              createBaseVNode("div", _hoisted_8, [
                createBaseVNode("button", {
                  class: "btn btn-sm btn-danger",
                  onClick: ($event) => deleteFile(f)
                }, "删除", 8, _hoisted_9)
              ])
            ]);
          }), 128)),
          items.value.length === 0 && !loading.value ? (openBlock(), createElementBlock("div", _hoisted_10, "空目录")) : createCommentVNode("", true)
        ]),
        createBaseVNode("div", {
          class: "upload-zone",
          id: "uploadZone",
          onClick: _cache[0] || (_cache[0] = ($event) => _ctx.$refs.fileInput.click()),
          onDragover: _cache[1] || (_cache[1] = withModifiers(() => {
          }, ["prevent"])),
          onDrop: withModifiers(onDrop, ["prevent"])
        }, [
          _cache[6] || (_cache[6] = createStaticVNode('<svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="var(--text-muted)" stroke-width="2"><path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"></path><polyline points="17 8 12 3 7 8"></polyline><line x1="12" y1="3" x2="12" y2="15"></line></svg><div style="font-size:13px;font-weight:500;">点击或拖拽上传文件</div>', 2)),
          createBaseVNode("input", {
            ref: "fileInput",
            type: "file",
            multiple: "",
            style: { "display": "none" },
            onChange: onFileSelect
          }, null, 544),
          uploading.value ? (openBlock(), createElementBlock("div", _hoisted_11, [
            createBaseVNode("div", {
              class: "upload-progress-bar",
              style: normalizeStyle({ width: uploadPct.value + "%" })
            }, null, 4),
            createBaseVNode("div", _hoisted_12, toDisplayString(uploadPct.value) + "%", 1)
          ])) : createCommentVNode("", true)
        ], 32)
      ]);
    };
  }
};
export {
  _sfc_main as default
};
