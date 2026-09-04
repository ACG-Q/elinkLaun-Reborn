import { g as getJSON, p as postAction, t as toast } from "./index-BnQGTW39.js";
import { o as onMounted, a as openBlock, c as createElementBlock, b as createBaseVNode, t as toDisplayString, n as normalizeStyle, F as Fragment, i as renderList, q as normalizeClass, h as createCommentVNode, r as ref, s as computed } from "./index-IHZWDMzB.js";
const _hoisted_1 = { class: "bento-grid" };
const _hoisted_2 = { class: "card" };
const _hoisted_3 = { style: { "font-size": "14px", "font-weight": "600" } };
const _hoisted_4 = { style: { "font-size": "12px", "color": "var(--text-muted)" } };
const _hoisted_5 = { class: "card" };
const _hoisted_6 = { style: { "display": "flex", "align-items": "center", "gap": "8px" } };
const _hoisted_7 = {
  class: "card-value",
  style: { "font-size": "18px" }
};
const _hoisted_8 = { style: { "flex": "1" } };
const _hoisted_9 = { class: "progress-bar" };
const _hoisted_10 = { style: { "font-size": "11px", "color": "var(--text-muted)", "margin-top": "4px" } };
const _hoisted_11 = { class: "card" };
const _hoisted_12 = {
  class: "card-value",
  style: { "font-size": "16px" }
};
const _hoisted_13 = {
  class: "progress-bar",
  style: { "margin-top": "6px" }
};
const _hoisted_14 = { style: { "font-size": "11px", "color": "var(--text-muted)", "margin-top": "4px" } };
const _hoisted_15 = { class: "card" };
const _hoisted_16 = {
  class: "card-value",
  style: { "font-size": "16px" }
};
const _hoisted_17 = { style: { "font-size": "11px", "color": "var(--text-muted)" } };
const _hoisted_18 = { class: "card" };
const _hoisted_19 = { style: { "width": "50px", "font-size": "12px", "color": "var(--text-muted)" } };
const _hoisted_20 = ["max", "value", "onInput"];
const _hoisted_21 = { style: { "width": "24px", "font-size": "12px", "text-align": "right", "color": "var(--text-muted)" } };
const _hoisted_22 = { class: "card" };
const _hoisted_23 = {
  class: "row",
  style: { "justify-content": "space-between" }
};
const _hoisted_24 = { style: { "font-size": "12px", "color": "var(--text-muted)" } };
const _hoisted_25 = ["value"];
const _hoisted_26 = { style: { "width": "32px", "font-size": "12px", "text-align": "right", "color": "var(--text-muted)" } };
const _hoisted_27 = { class: "card" };
const _hoisted_28 = {
  class: "row",
  style: { "justify-content": "space-between" }
};
const _hoisted_29 = { style: { "font-size": "12px", "color": "var(--text-muted)" } };
const _hoisted_30 = {
  class: "card",
  style: { "padding": "0" }
};
const _hoisted_31 = ["onClick"];
const _hoisted_32 = {
  width: "20",
  height: "20",
  viewBox: "0 0 24 24",
  fill: "none",
  stroke: "currentColor",
  "stroke-width": "2"
};
const _hoisted_33 = ["d"];
const _hoisted_34 = { class: "row-text" };
const _hoisted_35 = { class: "row-title" };
const _hoisted_36 = {
  key: 0,
  style: { "font-size": "11px", "color": "var(--text-muted)" }
};
const _sfc_main = {
  __name: "SettingsView",
  setup(__props) {
    const device = ref({});
    const battery = ref({});
    const storage = ref({});
    const wifi = ref({});
    const volume = ref({});
    const brightness = ref(128);
    const autoBrightness = ref(false);
    const autoRotate = ref(false);
    const batteryColor = computed(() => {
      const l = battery.value.level || 0;
      return l > 50 ? "#1e8e3e" : l > 20 ? "#f9ab00" : "#d93025";
    });
    const storagePct = computed(() => {
      const t = storage.value.total || 0;
      return t > 0 ? Math.round(storage.value.used / t * 100) : 0;
    });
    const volumeStreams = [
      { key: "music", label: "媒体", max: 15 },
      { key: "ring", label: "铃声", max: 7 },
      { key: "notification", label: "通知", max: 7 },
      { key: "alarm", label: "闹钟", max: 7 }
    ];
    function getVolumeValue(key) {
      const v = volume.value[key];
      if (v && typeof v === "object") return v.current || 0;
      return v || 0;
    }
    function getVolumeMax(key) {
      const v = volume.value[key];
      if (v && typeof v === "object") return v.max || 7;
      return 7;
    }
    const settingsLinks = [
      { name: "WiFi", action: "android.settings.WIFI_SETTINGS", icon: "M5 12.55a11 11 0 0 1 14.08 0M1.42 9a16 16 0 0 1 21.16 0M8.53 16.11a6 6 0 0 1 6.95 0M12 20h.01" },
      { name: "蓝牙", action: "android.settings.BLUETOOTH_SETTINGS", icon: "M6.5 6.5l11 11M12 2v20M17 7l-5 5-5-5" },
      { name: "显示", action: "android.settings.DISPLAY_SETTINGS", icon: "M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z" },
      { name: "声音", action: "android.settings.SOUND_SETTINGS", icon: "M9 18V5l12-3v13M9 19c0 1.1-1.3 2-3 2s-3-.9-3-2 1.3-2 3-2 3 .9 3 2z" },
      { name: "应用", action: "android.settings.APPLICATION_SETTINGS", icon: "M4 4h16v16H4z" },
      { name: "开发者选项", action: "android.settings.APPLICATION_DEVELOPMENT_SETTINGS", icon: "M16 18l6-6-6-6M8 6l-6 6 6 6", note: "需先在系统设置中启用" },
      { name: "电池", action: "android.settings.BATTERY_SAVER_SETTINGS", icon: "M13 2L3 14h9l-1 8 10-12h-9l1-8z" },
      { name: "存储", action: "android.settings.INTERNAL_STORAGE_SETTINGS", icon: "M21 16V8a2 2 0 0 0-1-1.73l-7-4a2 2 0 0 0-2 0l-7 4A2 2 0 0 0 3 8v8a2 2 0 0 0 1 1.73l7 4a2 2 0 0 0 2 0l7-4A2 2 0 0 0 21 16z" },
      { name: "通知", action: "android.settings.NOTIFICATION_LISTENER_SETTINGS", icon: "M18 8A6 6 0 0 0 6 8c0 7-3 9-3 9h18s-3-2-3-9M13.73 21a2 2 0 0 1-3.46 0" },
      { name: "位置", action: "android.settings.LOCATION_SOURCE_SETTINGS", icon: "M21 10c0 7-9 13-9 13s-9-6-9-13a9 9 0 0 1 18 0z" },
      { name: "安全", action: "android.settings.SECURITY_SETTINGS", icon: "M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z" },
      { name: "关于", action: "android.settings.DEVICE_INFO_SETTINGS", icon: "M12 22c5.5 0 10-4.5 10-10S17.5 2 12 2 2 6.5 2 12s4.5 10 10 10zM12 16v-4M12 8h.01" }
    ];
    async function loadAll() {
      try {
        const [d, b, s, w, v, br, r] = await Promise.all([
          getJSON("/api/device"),
          getJSON("/api/battery"),
          getJSON("/api/storage"),
          getJSON("/api/wifi-status"),
          getJSON("/api/volume"),
          getJSON("/api/brightness"),
          getJSON("/api/rotation")
        ]);
        device.value = d;
        battery.value = b;
        storage.value = s;
        wifi.value = w;
        volume.value = v;
        brightness.value = br.value || 128;
        autoBrightness.value = br.autoMode || false;
        autoRotate.value = r.enabled || false;
      } catch {
      }
    }
    async function setVolume(stream, val) {
      const numVal = parseInt(val);
      if (volume.value[stream] && typeof volume.value[stream] === "object") {
        volume.value[stream].current = numVal;
      } else {
        volume.value[stream] = numVal;
      }
      try {
        await postAction("/api/volume?stream=" + stream + "&value=" + val);
      } catch {
      }
    }
    async function setBrightness(val) {
      brightness.value = parseInt(val);
      try {
        const r = await postAction("/api/brightness?value=" + val);
        if (r && r.error) {
          toast(r.error, "error");
        }
      } catch {
      }
    }
    async function toggleAutoBrightness() {
      autoBrightness.value = !autoBrightness.value;
      try {
        const r = await postAction("/api/brightness?autoMode=" + autoBrightness.value);
        if (r && r.error) {
          toast(r.error, "error");
          autoBrightness.value = !autoBrightness.value;
        }
      } catch {
      }
    }
    async function toggleRotate() {
      autoRotate.value = !autoRotate.value;
      try {
        await postAction("/api/rotation?enabled=" + autoRotate.value);
      } catch {
      }
    }
    async function openSettings(action) {
      try {
        const r = await postAction("/api/open-settings?action=" + encodeURIComponent(action));
        if (!r.success) toast(r.error || "失败", "error");
      } catch {
        toast("失败", "error");
      }
    }
    onMounted(loadAll);
    return (_ctx, _cache) => {
      return openBlock(), createElementBlock("div", null, [
        createBaseVNode("div", _hoisted_1, [
          createBaseVNode("div", _hoisted_2, [
            _cache[1] || (_cache[1] = createBaseVNode("div", { class: "card-title" }, "设备", -1)),
            createBaseVNode("div", _hoisted_3, toDisplayString(device.value.model || "---"), 1),
            createBaseVNode("div", _hoisted_4, toDisplayString(device.value.manufacturer || "") + " | Android " + toDisplayString(device.value.release || ""), 1)
          ]),
          createBaseVNode("div", _hoisted_5, [
            _cache[2] || (_cache[2] = createBaseVNode("div", { class: "card-title" }, "电池", -1)),
            createBaseVNode("div", _hoisted_6, [
              createBaseVNode("div", _hoisted_7, toDisplayString(battery.value.level || 0) + "%", 1),
              createBaseVNode("div", _hoisted_8, [
                createBaseVNode("div", _hoisted_9, [
                  createBaseVNode("div", {
                    class: "progress-fill",
                    style: normalizeStyle({ width: (battery.value.level || 0) + "%", background: batteryColor.value })
                  }, null, 4)
                ])
              ])
            ]),
            createBaseVNode("div", _hoisted_10, toDisplayString(battery.value.statusText || "") + " | " + toDisplayString(battery.value.healthText || ""), 1)
          ]),
          createBaseVNode("div", _hoisted_11, [
            _cache[3] || (_cache[3] = createBaseVNode("div", { class: "card-title" }, "存储", -1)),
            createBaseVNode("div", _hoisted_12, toDisplayString(storage.value.usedHuman || "---") + " / " + toDisplayString(storage.value.totalHuman || "---"), 1),
            createBaseVNode("div", _hoisted_13, [
              createBaseVNode("div", {
                class: "progress-fill",
                style: normalizeStyle({ width: storagePct.value + "%" })
              }, null, 4)
            ]),
            createBaseVNode("div", _hoisted_14, toDisplayString(storage.value.availableHuman || "") + " 剩余", 1)
          ]),
          createBaseVNode("div", _hoisted_15, [
            _cache[4] || (_cache[4] = createBaseVNode("div", { class: "card-title" }, "WiFi", -1)),
            createBaseVNode("div", _hoisted_16, toDisplayString(wifi.value.ssid || "未连接"), 1),
            createBaseVNode("div", _hoisted_17, toDisplayString(wifi.value.stateText || "") + " " + toDisplayString(wifi.value.rssi ? wifi.value.rssi + " dBm" : ""), 1)
          ])
        ]),
        _cache[9] || (_cache[9] = createBaseVNode("h2", { style: { "margin": "20px 0 10px", "font-size": "15px" } }, "音量", -1)),
        createBaseVNode("div", _hoisted_18, [
          (openBlock(), createElementBlock(Fragment, null, renderList(volumeStreams, (s) => {
            return createBaseVNode("div", {
              key: s.key,
              class: "row",
              style: { "flex-wrap": "wrap", "gap": "8px" }
            }, [
              createBaseVNode("div", _hoisted_19, toDisplayString(s.label), 1),
              createBaseVNode("input", {
                type: "range",
                min: "0",
                max: getVolumeMax(s.key),
                value: getVolumeValue(s.key),
                onInput: (e) => setVolume(s.key, e.target.value),
                style: { "flex": "1", "min-width": "0" }
              }, null, 40, _hoisted_20),
              createBaseVNode("div", _hoisted_21, toDisplayString(getVolumeValue(s.key)), 1)
            ]);
          }), 64))
        ]),
        _cache[10] || (_cache[10] = createBaseVNode("h2", { style: { "margin": "20px 0 10px", "font-size": "15px" } }, "亮度", -1)),
        createBaseVNode("div", _hoisted_22, [
          createBaseVNode("div", _hoisted_23, [
            createBaseVNode("div", null, [
              _cache[5] || (_cache[5] = createBaseVNode("div", { style: { "font-size": "14px", "font-weight": "600" } }, "自动亮度", -1)),
              createBaseVNode("div", _hoisted_24, toDisplayString(autoBrightness.value ? "已开启" : "已关闭"), 1)
            ]),
            createBaseVNode("div", {
              class: normalizeClass(["toggle", { active: autoBrightness.value }]),
              onClick: toggleAutoBrightness
            }, null, 2)
          ]),
          createBaseVNode("div", {
            class: "row",
            style: normalizeStyle([{ "margin-top": "8px" }, { opacity: autoBrightness.value ? 0.4 : 1, pointerEvents: autoBrightness.value ? "none" : "auto" }])
          }, [
            createBaseVNode("input", {
              type: "range",
              min: "0",
              max: "255",
              value: brightness.value,
              onInput: _cache[0] || (_cache[0] = (e) => setBrightness(e.target.value)),
              style: { "flex": "1" }
            }, null, 40, _hoisted_25),
            createBaseVNode("div", _hoisted_26, toDisplayString(brightness.value), 1)
          ], 4),
          _cache[6] || (_cache[6] = createBaseVNode("div", { style: { "font-size": "11px", "color": "var(--text-muted)", "margin-top": "4px" } }, '如无法调节，请在系统设置中授予"修改系统设置"权限', -1))
        ]),
        _cache[11] || (_cache[11] = createBaseVNode("h2", { style: { "margin": "20px 0 10px", "font-size": "15px" } }, "屏幕旋转", -1)),
        createBaseVNode("div", _hoisted_27, [
          createBaseVNode("div", _hoisted_28, [
            createBaseVNode("div", null, [
              _cache[7] || (_cache[7] = createBaseVNode("div", { style: { "font-size": "14px", "font-weight": "600" } }, "自动旋转", -1)),
              createBaseVNode("div", _hoisted_29, toDisplayString(autoRotate.value ? "已开启" : "已关闭"), 1)
            ]),
            createBaseVNode("div", {
              class: normalizeClass(["toggle", { active: autoRotate.value }]),
              onClick: toggleRotate
            }, null, 2)
          ])
        ]),
        _cache[12] || (_cache[12] = createBaseVNode("h2", { style: { "margin": "20px 0 10px", "font-size": "15px" } }, "系统设置", -1)),
        createBaseVNode("div", _hoisted_30, [
          (openBlock(), createElementBlock(Fragment, null, renderList(settingsLinks, (s) => {
            return createBaseVNode("div", {
              key: s.action,
              class: "row",
              style: { "cursor": "pointer" },
              onClick: ($event) => openSettings(s.action)
            }, [
              (openBlock(), createElementBlock("svg", _hoisted_32, [
                createBaseVNode("path", {
                  d: s.icon
                }, null, 8, _hoisted_33)
              ])),
              createBaseVNode("div", _hoisted_34, [
                createBaseVNode("div", _hoisted_35, toDisplayString(s.name), 1),
                s.note ? (openBlock(), createElementBlock("div", _hoisted_36, toDisplayString(s.note), 1)) : createCommentVNode("", true)
              ]),
              _cache[8] || (_cache[8] = createBaseVNode("svg", {
                width: "16",
                height: "16",
                viewBox: "0 0 24 24",
                fill: "none",
                stroke: "var(--text-muted)",
                "stroke-width": "2"
              }, [
                createBaseVNode("polyline", { points: "9 18 15 12 9 6" })
              ], -1))
            ], 8, _hoisted_31);
          }), 64))
        ])
      ]);
    };
  }
};
export {
  _sfc_main as default
};
