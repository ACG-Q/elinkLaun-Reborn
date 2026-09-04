import { g as getJSON } from "./index-BnQGTW39.js";
import { o as onMounted, a as openBlock, c as createElementBlock, b as createBaseVNode, t as toDisplayString, d as createVNode, w as withCtx, r as ref, e as resolveComponent } from "./index-IHZWDMzB.js";
const _hoisted_1 = { class: "bento-grid" };
const _hoisted_2 = { class: "card" };
const _hoisted_3 = { class: "card-value" };
const _hoisted_4 = { class: "card" };
const _hoisted_5 = {
  class: "card-value",
  style: { "font-size": "18px" }
};
const _hoisted_6 = { class: "card" };
const _hoisted_7 = { class: "card-value" };
const _hoisted_8 = { class: "card" };
const _hoisted_9 = {
  class: "card-value",
  style: { "font-size": "16px" }
};
const _hoisted_10 = { class: "grid" };
const _sfc_main = {
  __name: "HomeView",
  setup(__props) {
    const stats = ref({});
    const storageInfo = ref({});
    const batteryLevel = ref(0);
    const wifiName = ref("");
    onMounted(async () => {
      try {
        const [s, b, w, st] = await Promise.all([
          getJSON("/api/stats"),
          getJSON("/api/battery"),
          getJSON("/api/wifi-status"),
          getJSON("/api/storage")
        ]);
        stats.value = s;
        batteryLevel.value = b.level || 0;
        wifiName.value = w.ssid || "";
        storageInfo.value = st;
      } catch {
      }
    });
    return (_ctx, _cache) => {
      const _component_router_link = resolveComponent("router-link");
      return openBlock(), createElementBlock("div", null, [
        createBaseVNode("div", _hoisted_1, [
          createBaseVNode("div", _hoisted_2, [
            _cache[0] || (_cache[0] = createBaseVNode("div", { class: "card-title" }, "文件", -1)),
            createBaseVNode("div", _hoisted_3, toDisplayString(stats.value.fileCount || 0), 1)
          ]),
          createBaseVNode("div", _hoisted_4, [
            _cache[1] || (_cache[1] = createBaseVNode("div", { class: "card-title" }, "存储", -1)),
            createBaseVNode("div", _hoisted_5, toDisplayString(storageInfo.value.usedHuman || "---") + " / " + toDisplayString(storageInfo.value.totalHuman || "---"), 1)
          ]),
          createBaseVNode("div", _hoisted_6, [
            _cache[2] || (_cache[2] = createBaseVNode("div", { class: "card-title" }, "电池", -1)),
            createBaseVNode("div", _hoisted_7, toDisplayString(batteryLevel.value) + "%", 1)
          ]),
          createBaseVNode("div", _hoisted_8, [
            _cache[3] || (_cache[3] = createBaseVNode("div", { class: "card-title" }, "WiFi", -1)),
            createBaseVNode("div", _hoisted_9, toDisplayString(wifiName.value || "未连接"), 1)
          ])
        ]),
        createBaseVNode("div", _hoisted_10, [
          createVNode(_component_router_link, {
            to: "/fm",
            class: "grid-card"
          }, {
            default: withCtx(() => [..._cache[4] || (_cache[4] = [
              createBaseVNode("svg", {
                width: "28",
                height: "28",
                viewBox: "0 0 24 24",
                fill: "none",
                stroke: "currentColor",
                "stroke-width": "2",
                "stroke-linecap": "round",
                "stroke-linejoin": "round"
              }, [
                createBaseVNode("path", { d: "M22 19a2 2 0 0 1-2 2H4a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h5l2 3h9a2 2 0 0 1 2 2z" })
              ], -1),
              createBaseVNode("div", { class: "label" }, "文件管理", -1),
              createBaseVNode("div", { class: "desc" }, "浏览和管理文件", -1)
            ])]),
            _: 1
          }),
          createVNode(_component_router_link, {
            to: "/apk",
            class: "grid-card"
          }, {
            default: withCtx(() => [..._cache[5] || (_cache[5] = [
              createBaseVNode("svg", {
                width: "28",
                height: "28",
                viewBox: "0 0 24 24",
                fill: "none",
                stroke: "currentColor",
                "stroke-width": "2",
                "stroke-linecap": "round",
                "stroke-linejoin": "round"
              }, [
                createBaseVNode("rect", {
                  x: "5",
                  y: "2",
                  width: "14",
                  height: "20",
                  rx: "2",
                  ry: "2"
                }),
                createBaseVNode("line", {
                  x1: "12",
                  y1: "18",
                  x2: "12.01",
                  y2: "18"
                })
              ], -1),
              createBaseVNode("div", { class: "label" }, "APK 管理", -1),
              createBaseVNode("div", { class: "desc" }, "安装和管理应用", -1)
            ])]),
            _: 1
          }),
          createVNode(_component_router_link, {
            to: "/icons",
            class: "grid-card"
          }, {
            default: withCtx(() => [..._cache[6] || (_cache[6] = [
              createBaseVNode("svg", {
                width: "28",
                height: "28",
                viewBox: "0 0 24 24",
                fill: "none",
                stroke: "currentColor",
                "stroke-width": "2",
                "stroke-linecap": "round",
                "stroke-linejoin": "round"
              }, [
                createBaseVNode("rect", {
                  x: "3",
                  y: "3",
                  width: "18",
                  height: "18",
                  rx: "2",
                  ry: "2"
                }),
                createBaseVNode("circle", {
                  cx: "8.5",
                  cy: "8.5",
                  r: "1.5"
                }),
                createBaseVNode("polyline", { points: "21 15 16 10 5 21" })
              ], -1),
              createBaseVNode("div", { class: "label" }, "图标管理", -1),
              createBaseVNode("div", { class: "desc" }, "自定义应用图标", -1)
            ])]),
            _: 1
          }),
          createVNode(_component_router_link, {
            to: "/settings",
            class: "grid-card"
          }, {
            default: withCtx(() => [..._cache[7] || (_cache[7] = [
              createBaseVNode("svg", {
                width: "28",
                height: "28",
                viewBox: "0 0 24 24",
                fill: "none",
                stroke: "currentColor",
                "stroke-width": "2",
                "stroke-linecap": "round",
                "stroke-linejoin": "round"
              }, [
                createBaseVNode("circle", {
                  cx: "12",
                  cy: "12",
                  r: "3"
                }),
                createBaseVNode("path", { d: "M19.4 15a1.65 1.65 0 0 0 .33 1.82l.06.06a2 2 0 0 1 0 2.83 2 2 0 0 1-2.83 0l-.06-.06a1.65 1.65 0 0 0-1.82-.33 1.65 1.65 0 0 0-1 1.51V21a2 2 0 0 1-2 2 2 2 0 0 1-2-2v-.09A1.65 1.65 0 0 0 9 19.4a1.65 1.65 0 0 0-1.82.33l-.06.06a2 2 0 0 1-2.83 0 2 2 0 0 1 0-2.83l.06-.06A1.65 1.65 0 0 0 4.68 15a1.65 1.65 0 0 0-1.51-1H3a2 2 0 0 1-2-2 2 2 0 0 1 2-2h.09A1.65 1.65 0 0 0 4.6 9a1.65 1.65 0 0 0-.33-1.82l-.06-.06a2 2 0 0 1 0-2.83 2 2 0 0 1 2.83 0l.06.06A1.65 1.65 0 0 0 9 4.68a1.65 1.65 0 0 0 1-1.51V3a2 2 0 0 1 2-2 2 2 0 0 1 2 2v.09a1.65 1.65 0 0 0 1 1.51 1.65 1.65 0 0 0 1.82-.33l.06-.06a2 2 0 0 1 2.83 0 2 2 0 0 1 0 2.83l-.06.06A1.65 1.65 0 0 0 19.4 9a1.65 1.65 0 0 0 1.51 1H21a2 2 0 0 1 2 2 2 2 0 0 1-2 2h-.09a1.65 1.65 0 0 0-1.51 1z" })
              ], -1),
              createBaseVNode("div", { class: "label" }, "系统设置", -1),
              createBaseVNode("div", { class: "desc" }, "设备参数调节", -1)
            ])]),
            _: 1
          })
        ])
      ]);
    };
  }
};
export {
  _sfc_main as default
};
