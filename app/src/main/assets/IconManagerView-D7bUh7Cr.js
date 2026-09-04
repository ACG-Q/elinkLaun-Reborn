import { o as onMounted, a as openBlock, c as createElementBlock, b as createBaseVNode, m as withDirectives, v as vModelText, d as createVNode, w as withCtx, j as createTextVNode, F as Fragment, i as renderList, l as withModifiers, t as toDisplayString, h as createCommentVNode, r as ref, e as resolveComponent, p as reactive } from "./index-IHZWDMzB.js";
import { g as getJSON, u as uploadFile, t as toast } from "./index-BnQGTW39.js";
const _hoisted_1 = { class: "search-bar" };
const _hoisted_2 = { class: "icon-grid" };
const _hoisted_3 = ["onDragenter", "onDrop", "onClick"];
const _hoisted_4 = ["src"];
const _hoisted_5 = { class: "icon-slot-name" };
const _hoisted_6 = {
  key: 0,
  class: "icon-slot-badge"
};
const _hoisted_7 = ["onChange"];
const _hoisted_8 = {
  key: 0,
  class: "empty-state"
};
const _sfc_main = {
  __name: "IconManagerView",
  setup(__props) {
    const icons = ref([]);
    const filtered = ref([]);
    const query = ref("");
    const loading = ref(false);
    const dragPkg = ref("");
    const fileInputs = reactive({});
    async function loadIcons() {
      loading.value = true;
      try {
        const data = await getJSON("/api/icons");
        icons.value = data.items || [];
        filtered.value = icons.value;
      } catch {
        icons.value = [];
        filtered.value = [];
      }
      loading.value = false;
    }
    function filterIcons() {
      const q = query.value.toLowerCase();
      filtered.value = icons.value.filter(
        (a) => a.name.toLowerCase().includes(q) || a.packageName.toLowerCase().includes(q)
      );
    }
    function clickSlot(pkg) {
      const input = fileInputs[pkg];
      if (input) input.click();
    }
    async function replaceIcon(pkg, e) {
      const file = e.target.files[0];
      if (!file) return;
      try {
        await uploadFile("/upload/" + file.name, file, {
          action: "icon-replace",
          pkg
        });
        toast("图标已上传", "success");
        loadIcons();
      } catch {
        toast("上传失败", "error");
      }
    }
    async function onDrop(e, pkg) {
      dragPkg.value = "";
      const file = e.dataTransfer.files[0];
      if (!file) return;
      try {
        await uploadFile("/upload/" + file.name, file, {
          action: "icon-replace",
          pkg
        });
        toast("图标已上传", "success");
        loadIcons();
      } catch {
        toast("上传失败", "error");
      }
    }
    onMounted(loadIcons);
    return (_ctx, _cache) => {
      const _component_router_link = resolveComponent("router-link");
      return openBlock(), createElementBlock("div", null, [
        createBaseVNode("div", _hoisted_1, [
          withDirectives(createBaseVNode("input", {
            class: "form-input",
            placeholder: "搜索应用...",
            "onUpdate:modelValue": _cache[0] || (_cache[0] = ($event) => query.value = $event),
            onInput: filterIcons
          }, null, 544), [
            [vModelText, query.value]
          ])
        ]),
        createVNode(_component_router_link, {
          to: "/icon-gen",
          class: "btn btn-primary",
          style: { "width": "100%", "display": "flex", "align-items": "center", "justify-content": "center", "gap": "8px", "text-decoration": "none", "margin-bottom": "12px" }
        }, {
          default: withCtx(() => [..._cache[3] || (_cache[3] = [
            createBaseVNode("svg", {
              width: "18",
              height: "18",
              viewBox: "0 0 24 24",
              fill: "none",
              stroke: "currentColor",
              "stroke-width": "2"
            }, [
              createBaseVNode("circle", {
                cx: "12",
                cy: "12",
                r: "10"
              }),
              createBaseVNode("line", {
                x1: "12",
                y1: "8",
                x2: "12",
                y2: "16"
              }),
              createBaseVNode("line", {
                x1: "8",
                y1: "12",
                x2: "16",
                y2: "12"
              })
            ], -1),
            createTextVNode(" 圆角图片生成器 ", -1)
          ])]),
          _: 1
        }),
        createBaseVNode("div", _hoisted_2, [
          (openBlock(true), createElementBlock(Fragment, null, renderList(filtered.value, (item) => {
            return openBlock(), createElementBlock("div", {
              key: item.packageName,
              class: "icon-slot",
              onDragover: _cache[1] || (_cache[1] = withModifiers(() => {
              }, ["prevent"])),
              onDragenter: withModifiers(($event) => dragPkg.value = item.packageName, ["prevent"]),
              onDragleave: _cache[2] || (_cache[2] = ($event) => dragPkg.value = ""),
              onDrop: withModifiers(($event) => onDrop($event, item.packageName), ["prevent"]),
              onClick: ($event) => clickSlot(item.packageName)
            }, [
              createBaseVNode("img", {
                src: "/api/app-icon?pkg=" + encodeURIComponent(item.packageName),
                style: { "width": "100%", "height": "100%", "object-fit": "cover", "border-radius": "12px", "position": "absolute", "top": "0", "left": "0" }
              }, null, 8, _hoisted_4),
              createBaseVNode("div", _hoisted_5, toDisplayString(item.name), 1),
              item.hasCustomIcon ? (openBlock(), createElementBlock("div", _hoisted_6, "Custom")) : createCommentVNode("", true),
              createBaseVNode("input", {
                type: "file",
                accept: "image/*",
                ref_for: true,
                ref: (el) => fileInputs[item.packageName] = el,
                style: { "display": "none" },
                onChange: (e) => replaceIcon(item.packageName, e)
              }, null, 40, _hoisted_7)
            ], 40, _hoisted_3);
          }), 128))
        ]),
        filtered.value.length === 0 && !loading.value ? (openBlock(), createElementBlock("div", _hoisted_8, "暂无应用")) : createCommentVNode("", true)
      ]);
    };
  }
};
export {
  _sfc_main as default
};
