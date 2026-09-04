import { o as onMounted, x as nextTick, a as openBlock, c as createElementBlock, b as createBaseVNode, q as normalizeClass, m as withDirectives, v as vModelText, h as createCommentVNode, n as normalizeStyle, l as withModifiers, F as Fragment, k as createStaticVNode, i as renderList, t as toDisplayString, j as createTextVNode, r as ref, p as reactive } from "./index-IHZWDMzB.js";
import { t as toast } from "./index-BnQGTW39.js";
const _hoisted_1 = {
  class: "card",
  style: { "text-align": "center", "padding": "16px" }
};
const _hoisted_2 = ["width", "height"];
const _hoisted_3 = { class: "control-section" };
const _hoisted_4 = { style: { "display": "flex", "gap": "8px" } };
const _hoisted_5 = {
  key: 0,
  style: { "margin-top": "12px" }
};
const _hoisted_6 = {
  key: 1,
  style: { "margin-top": "12px" }
};
const _hoisted_7 = ["src"];
const _hoisted_8 = { class: "control-section" };
const _hoisted_9 = { class: "color-presets" };
const _hoisted_10 = ["onClick"];
const _hoisted_11 = { style: { "margin-top": "12px" } };
const _hoisted_12 = { class: "color-row" };
const _hoisted_13 = { style: { "flex": "1", "min-width": "0" } };
const _hoisted_14 = { class: "color-hex" };
const _hoisted_15 = {
  class: "color-row",
  style: { "margin-top": "8px" }
};
const _hoisted_16 = { style: { "flex": "1", "min-width": "0" } };
const _hoisted_17 = { class: "color-hex" };
const _hoisted_18 = { class: "control-section" };
const _hoisted_19 = { style: { "display": "flex", "align-items": "center", "justify-content": "space-between" } };
const _hoisted_20 = { style: { "margin-top": "12px" } };
const _hoisted_21 = { style: { "display": "flex", "justify-content": "space-between", "margin-bottom": "6px" } };
const _hoisted_22 = { style: { "font-size": "13px", "color": "var(--text-muted)" } };
const _hoisted_23 = { class: "control-section" };
const _hoisted_24 = { style: { "display": "flex", "gap": "8px", "flex-wrap": "wrap" } };
const _hoisted_25 = ["onClick"];
const _hoisted_26 = { style: { "margin-top": "12px" } };
const _hoisted_27 = { style: { "display": "flex", "gap": "8px" } };
const _hoisted_28 = ["onClick"];
const _hoisted_29 = { class: "control-section" };
const _hoisted_30 = { style: { "display": "flex", "justify-content": "space-between", "margin-bottom": "4px" } };
const _hoisted_31 = { style: { "font-size": "13px", "color": "var(--text-muted)" } };
const _hoisted_32 = { style: { "font-size": "13px", "color": "var(--text-muted)" } };
const _hoisted_33 = ["min", "max", "onUpdate:modelValue"];
const _sfc_main = {
  __name: "IconGenView",
  setup(__props) {
    const canvas = ref(null);
    const mode = ref("text");
    const text = ref("设置");
    const bgColor = ref("#ffffff");
    const fgColor = ref("#1a1a1a");
    const showBorder = ref(true);
    const borderWidth = ref(3);
    const fontFamily = ref("system-ui");
    const fontWeight = ref("700");
    const size = ref(256);
    const uploadedImage = ref(null);
    const uploadedImageSrc = ref("");
    const dragActive = ref(false);
    const activeScheme = ref("白底黑字");
    const colorSchemes = [
      { label: "白底黑字", bg: "#ffffff", fg: "#1a1a1a" },
      { label: "黑底白字", bg: "#1a1a1a", fg: "#ffffff" },
      { label: "蓝底白字", bg: "#1a73e8", fg: "#ffffff" },
      { label: "绿底白字", bg: "#1e8e3e", fg: "#ffffff" },
      { label: "红底白字", bg: "#d93025", fg: "#ffffff" },
      { label: "黄底黑字", bg: "#f9ab00", fg: "#1a1a1a" }
    ];
    const fontList = [
      { label: "默认", value: "system-ui" },
      { label: "宋体", value: "serif" },
      { label: "黑体", value: "sans-serif" },
      { label: "楷体", value: "cursive" },
      { label: "等宽", value: "monospace" }
    ];
    const sizeSliders = reactive([
      { key: "size", label: "图片尺寸", value: 256, min: 64, max: 1024, unit: "px" },
      { key: "radius", label: "圆角半径", value: 32, min: 0, max: 128, unit: "px" },
      { key: "fontSize", label: "字号", value: 100, min: 30, max: 200, unit: "%" },
      { key: "letterSpacing", label: "字间距", value: 0, min: -10, max: 30, unit: "px" }
    ]);
    function draw() {
      const c = canvas.value;
      if (!c) return;
      const ctx = c.getContext("2d");
      const s = sizeSliders[0].value;
      c.width = s;
      c.height = s;
      const r = Math.min(sizeSliders[1].value, s / 2);
      ctx.clearRect(0, 0, s, s);
      ctx.beginPath();
      ctx.moveTo(r, 0);
      ctx.lineTo(s - r, 0);
      ctx.quadraticCurveTo(s, 0, s, r);
      ctx.lineTo(s, s - r);
      ctx.quadraticCurveTo(s, s, s - r, s);
      ctx.lineTo(r, s);
      ctx.quadraticCurveTo(0, s, 0, s - r);
      ctx.lineTo(0, r);
      ctx.quadraticCurveTo(0, 0, r, 0);
      ctx.closePath();
      ctx.fillStyle = bgColor.value;
      ctx.fill();
      if (showBorder.value && borderWidth.value > 0) {
        const bw = borderWidth.value;
        ctx.save();
        ctx.beginPath();
        const br = Math.max(0, r - bw / 2);
        ctx.moveTo(br + bw / 2, bw / 2);
        ctx.lineTo(s - br - bw / 2, bw / 2);
        ctx.quadraticCurveTo(s - bw / 2, bw / 2, s - bw / 2, br + bw / 2);
        ctx.lineTo(s - bw / 2, s - br - bw / 2);
        ctx.quadraticCurveTo(s - bw / 2, s - bw / 2, s - br - bw / 2, s - bw / 2);
        ctx.lineTo(br + bw / 2, s - bw / 2);
        ctx.quadraticCurveTo(bw / 2, s - bw / 2, bw / 2, s - br - bw / 2);
        ctx.lineTo(bw / 2, br + bw / 2);
        ctx.quadraticCurveTo(bw / 2, bw / 2, br + bw / 2, bw / 2);
        ctx.closePath();
        ctx.strokeStyle = fgColor.value;
        ctx.lineWidth = bw;
        ctx.stroke();
        ctx.restore();
      }
      if (mode.value === "image" && uploadedImage.value) {
        const img = uploadedImage.value;
        const scale = Math.min(s / img.width, s / img.height);
        const iw = img.width * scale, ih = img.height * scale;
        ctx.drawImage(img, (s - iw) / 2, (s - ih) / 2, iw, ih);
        return;
      }
      const t = text.value;
      if (!t) return;
      const charCount = t.length;
      const baseFs = s * 0.42 * (sizeSliders[2].value / 100);
      const fs = charCount > 1 ? baseFs * 0.85 : baseFs;
      ctx.fillStyle = fgColor.value;
      ctx.textAlign = "center";
      ctx.textBaseline = "middle";
      ctx.font = fontWeight.value + " " + fs + "px " + fontFamily.value;
      const sp = sizeSliders[3].value;
      if (charCount === 1) {
        ctx.fillText(t, s / 2, s / 2);
      } else {
        const chars = t.split("");
        const widths = chars.map((ch) => ctx.measureText(ch).width);
        const totalW = widths.reduce((a, b) => a + b, 0) + sp * (charCount - 1);
        let cx = (s - totalW) / 2;
        chars.forEach((ch, i) => {
          ctx.fillText(ch, cx + widths[i] / 2, s / 2);
          cx += widths[i] + sp;
        });
      }
    }
    function applyScheme(s) {
      activeScheme.value = s.label;
      bgColor.value = s.bg;
      fgColor.value = s.fg;
      draw();
    }
    function onImageSelect(e) {
      if (e.target.files[0]) loadImage(e.target.files[0]);
    }
    function onImageDrop(e) {
      dragActive.value = false;
      if (e.dataTransfer.files[0]) loadImage(e.dataTransfer.files[0]);
    }
    function loadImage(file) {
      if (!file.type.startsWith("image/")) {
        toast("请上传图片文件", "error");
        return;
      }
      const reader = new FileReader();
      reader.onload = (e) => {
        const img = new Image();
        img.onload = () => {
          uploadedImage.value = img;
          uploadedImageSrc.value = e.target.result;
          draw();
          toast("图片已加载", "success");
        };
        img.src = e.target.result;
      };
      reader.readAsDataURL(file);
    }
    function clearImage() {
      uploadedImage.value = null;
      uploadedImageSrc.value = "";
      draw();
      toast("已移除图片", "success");
    }
    function download() {
      const c = canvas.value;
      if (!c) return;
      const a = document.createElement("a");
      a.download = (mode.value === "image" ? "image" : text.value || "icon") + ".png";
      a.href = c.toDataURL("image/png");
      a.click();
      toast("已保存到下载目录", "success");
    }
    async function copyClipboard() {
      const c = canvas.value;
      if (!c) return;
      try {
        const blob = await new Promise((r) => c.toBlob(r, "image/png"));
        await navigator.clipboard.write([new ClipboardItem({ "image/png": blob })]);
        toast("已复制到剪贴板", "success");
      } catch {
        toast("复制失败，请重试", "error");
      }
    }
    onMounted(() => {
      nextTick(draw);
    });
    return (_ctx, _cache) => {
      return openBlock(), createElementBlock("div", null, [
        createBaseVNode("div", _hoisted_1, [
          createBaseVNode("canvas", {
            ref_key: "canvas",
            ref: canvas,
            width: size.value,
            height: size.value,
            style: { "max-width": "256px", "max-height": "256px", "border-radius": "12px", "image-rendering": "-webkit-optimize-contrast" }
          }, null, 8, _hoisted_2)
        ]),
        createBaseVNode("div", _hoisted_3, [
          _cache[13] || (_cache[13] = createBaseVNode("div", { class: "control-section-title" }, "图标内容", -1)),
          createBaseVNode("div", _hoisted_4, [
            createBaseVNode("button", {
              class: normalizeClass(["btn", { "btn-primary": mode.value === "text" }]),
              onClick: _cache[0] || (_cache[0] = ($event) => {
                mode.value = "text";
                draw();
              })
            }, "文字", 2),
            createBaseVNode("button", {
              class: normalizeClass(["btn", { "btn-primary": mode.value === "image" }]),
              onClick: _cache[1] || (_cache[1] = ($event) => {
                mode.value = "image";
                draw();
              })
            }, "图片", 2)
          ]),
          mode.value === "text" ? (openBlock(), createElementBlock("div", _hoisted_5, [
            withDirectives(createBaseVNode("input", {
              class: "form-input",
              "onUpdate:modelValue": _cache[2] || (_cache[2] = ($event) => text.value = $event),
              maxlength: "2",
              placeholder: "输入 1~2 个字",
              onInput: draw
            }, null, 544), [
              [vModelText, text.value]
            ])
          ])) : createCommentVNode("", true),
          mode.value === "image" ? (openBlock(), createElementBlock("div", _hoisted_6, [
            createBaseVNode("div", {
              class: "upload-area",
              id: "igen-uploadArea",
              onClick: _cache[3] || (_cache[3] = ($event) => _ctx.$refs.imageInput.click()),
              onDragover: _cache[4] || (_cache[4] = withModifiers(($event) => dragActive.value = true, ["prevent"])),
              onDragleave: _cache[5] || (_cache[5] = ($event) => dragActive.value = false),
              onDrop: withModifiers(onImageDrop, ["prevent"]),
              style: normalizeStyle(dragActive.value ? { borderColor: "var(--primary)", background: "var(--primary-light)" } : {})
            }, [
              !uploadedImageSrc.value ? (openBlock(), createElementBlock(Fragment, { key: 0 }, [
                _cache[12] || (_cache[12] = createStaticVNode('<svg width="32" height="32" viewBox="0 0 24 24" fill="none" stroke="var(--text-muted)" stroke-width="1.5"><path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"></path><polyline points="17 8 12 3 7 8"></polyline><line x1="12" y1="3" x2="12" y2="15"></line></svg><div style="font-size:13px;color:var(--text-muted);margin-top:8px;">点击或拖拽上传图片</div><div style="font-size:12px;color:var(--text-muted);margin-top:4px;">支持 PNG / JPG / WebP</div>', 3))
              ], 64)) : (openBlock(), createElementBlock(Fragment, { key: 1 }, [
                createBaseVNode("img", {
                  src: uploadedImageSrc.value,
                  style: { "width": "100%", "display": "block", "border-radius": "8px" }
                }, null, 8, _hoisted_7),
                createBaseVNode("button", {
                  style: { "position": "absolute", "top": "8px", "right": "8px", "width": "28px", "height": "28px", "border-radius": "50%", "background": "rgba(0,0,0,0.6)", "color": "#fff", "border": "none", "cursor": "pointer", "font-size": "16px" },
                  onClick: withModifiers(clearImage, ["stop"])
                }, "×")
              ], 64)),
              createBaseVNode("input", {
                ref: "imageInput",
                type: "file",
                accept: "image/*",
                style: { "display": "none" },
                onChange: onImageSelect
              }, null, 544)
            ], 36)
          ])) : createCommentVNode("", true)
        ]),
        createBaseVNode("div", _hoisted_8, [
          _cache[16] || (_cache[16] = createBaseVNode("div", { class: "control-section-title" }, "配色方案", -1)),
          createBaseVNode("div", _hoisted_9, [
            (openBlock(), createElementBlock(Fragment, null, renderList(colorSchemes, (s) => {
              return createBaseVNode("button", {
                key: s.label,
                class: normalizeClass(["color-scheme-btn", { active: activeScheme.value === s.label }]),
                onClick: ($event) => applyScheme(s)
              }, toDisplayString(s.label), 11, _hoisted_10);
            }), 64))
          ]),
          createBaseVNode("div", _hoisted_11, [
            createBaseVNode("div", _hoisted_12, [
              withDirectives(createBaseVNode("input", {
                type: "color",
                "onUpdate:modelValue": _cache[6] || (_cache[6] = ($event) => bgColor.value = $event),
                onInput: _cache[7] || (_cache[7] = ($event) => {
                  activeScheme.value = "";
                  draw();
                })
              }, null, 544), [
                [vModelText, bgColor.value]
              ]),
              createBaseVNode("div", _hoisted_13, [
                createBaseVNode("div", _hoisted_14, toDisplayString(bgColor.value.toUpperCase()), 1),
                _cache[14] || (_cache[14] = createBaseVNode("div", { class: "color-label" }, "背景色", -1))
              ])
            ]),
            createBaseVNode("div", _hoisted_15, [
              withDirectives(createBaseVNode("input", {
                type: "color",
                "onUpdate:modelValue": _cache[8] || (_cache[8] = ($event) => fgColor.value = $event),
                onInput: _cache[9] || (_cache[9] = ($event) => {
                  activeScheme.value = "";
                  draw();
                })
              }, null, 544), [
                [vModelText, fgColor.value]
              ]),
              createBaseVNode("div", _hoisted_16, [
                createBaseVNode("div", _hoisted_17, toDisplayString(fgColor.value.toUpperCase()), 1),
                _cache[15] || (_cache[15] = createBaseVNode("div", { class: "color-label" }, "文字色", -1))
              ])
            ])
          ])
        ]),
        createBaseVNode("div", _hoisted_18, [
          _cache[19] || (_cache[19] = createBaseVNode("div", { class: "control-section-title" }, "边框", -1)),
          createBaseVNode("div", _hoisted_19, [
            _cache[17] || (_cache[17] = createBaseVNode("span", { style: { "font-size": "14px", "font-weight": "500" } }, "显示边框（跟随文字色）", -1)),
            createBaseVNode("div", {
              class: normalizeClass(["toggle", { active: showBorder.value }]),
              onClick: _cache[10] || (_cache[10] = ($event) => {
                showBorder.value = !showBorder.value;
                draw();
              })
            }, null, 2)
          ]),
          createBaseVNode("div", _hoisted_20, [
            createBaseVNode("div", _hoisted_21, [
              _cache[18] || (_cache[18] = createBaseVNode("span", { style: { "font-size": "13px", "color": "var(--text-muted)" } }, "边框宽度", -1)),
              createBaseVNode("span", _hoisted_22, toDisplayString(borderWidth.value) + " px", 1)
            ]),
            withDirectives(createBaseVNode("input", {
              type: "range",
              min: "1",
              max: "12",
              "onUpdate:modelValue": _cache[11] || (_cache[11] = ($event) => borderWidth.value = $event),
              onInput: draw,
              style: { "width": "100%", "height": "6px" }
            }, null, 544), [
              [
                vModelText,
                borderWidth.value,
                void 0,
                { number: true }
              ]
            ])
          ])
        ]),
        createBaseVNode("div", _hoisted_23, [
          _cache[21] || (_cache[21] = createBaseVNode("div", { class: "control-section-title" }, "字体", -1)),
          createBaseVNode("div", _hoisted_24, [
            (openBlock(), createElementBlock(Fragment, null, renderList(fontList, (f) => {
              return createBaseVNode("button", {
                key: f.value,
                class: normalizeClass(["btn btn-sm", { "btn-primary": fontFamily.value === f.value }]),
                onClick: ($event) => {
                  fontFamily.value = f.value;
                  draw();
                }
              }, toDisplayString(f.label), 11, _hoisted_25);
            }), 64))
          ]),
          createBaseVNode("div", _hoisted_26, [
            _cache[20] || (_cache[20] = createBaseVNode("div", { style: { "font-size": "13px", "color": "var(--text-muted)", "margin-bottom": "8px" } }, "字体粗细", -1)),
            createBaseVNode("div", _hoisted_27, [
              (openBlock(), createElementBlock(Fragment, null, renderList(["400", "700", "900"], (w) => {
                return createBaseVNode("button", {
                  key: w,
                  class: normalizeClass(["btn btn-sm", { "btn-primary": fontWeight.value === w }]),
                  onClick: ($event) => {
                    fontWeight.value = w;
                    draw();
                  }
                }, toDisplayString(w === "400" ? "Regular" : w === "700" ? "Bold" : "Black"), 11, _hoisted_28);
              }), 64))
            ])
          ])
        ]),
        createBaseVNode("div", _hoisted_29, [
          _cache[22] || (_cache[22] = createBaseVNode("div", { class: "control-section-title" }, "尺寸与样式", -1)),
          (openBlock(true), createElementBlock(Fragment, null, renderList(sizeSliders, (s) => {
            return openBlock(), createElementBlock("div", {
              key: s.key,
              style: { "margin-bottom": "10px" }
            }, [
              createBaseVNode("div", _hoisted_30, [
                createBaseVNode("span", _hoisted_31, toDisplayString(s.label), 1),
                createBaseVNode("span", _hoisted_32, toDisplayString(s.value) + " " + toDisplayString(s.unit), 1)
              ]),
              withDirectives(createBaseVNode("input", {
                type: "range",
                min: s.min,
                max: s.max,
                "onUpdate:modelValue": ($event) => s.value = $event,
                onInput: draw,
                style: { "width": "100%", "height": "6px" }
              }, null, 40, _hoisted_33), [
                [
                  vModelText,
                  s.value,
                  void 0,
                  { number: true }
                ]
              ])
            ]);
          }), 128))
        ]),
        createBaseVNode("div", { style: { "display": "flex", "gap": "8px" } }, [
          createBaseVNode("button", {
            class: "btn",
            style: { "flex": "1", "padding": "12px", "border-radius": "8px", "border": "2px solid var(--border)", "background": "var(--bg)", "font-size": "14px", "font-weight": "600", "min-height": "48px", "display": "flex", "align-items": "center", "justify-content": "center", "gap": "8px" },
            onClick: copyClipboard
          }, [..._cache[23] || (_cache[23] = [
            createBaseVNode("svg", {
              width: "18",
              height: "18",
              viewBox: "0 0 24 24",
              fill: "none",
              stroke: "currentColor",
              "stroke-width": "2"
            }, [
              createBaseVNode("rect", {
                x: "9",
                y: "9",
                width: "13",
                height: "13",
                rx: "2"
              }),
              createBaseVNode("path", { d: "M5 15H4a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2h9a2 2 0 0 1 2 2v1" })
            ], -1),
            createTextVNode(" 复制 ", -1)
          ])]),
          createBaseVNode("button", {
            class: "btn btn-primary",
            style: { "flex": "1", "padding": "12px", "border-radius": "8px", "font-size": "14px", "font-weight": "600", "min-height": "48px", "display": "flex", "align-items": "center", "justify-content": "center", "gap": "8px" },
            onClick: download
          }, [..._cache[24] || (_cache[24] = [
            createBaseVNode("svg", {
              width: "18",
              height: "18",
              viewBox: "0 0 24 24",
              fill: "none",
              stroke: "currentColor",
              "stroke-width": "2"
            }, [
              createBaseVNode("path", { d: "M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4" }),
              createBaseVNode("polyline", { points: "7 10 12 15 17 10" }),
              createBaseVNode("line", {
                x1: "12",
                y1: "15",
                x2: "12",
                y2: "3"
              })
            ], -1),
            createTextVNode(" 下载 PNG ", -1)
          ])])
        ])
      ]);
    };
  }
};
export {
  _sfc_main as default
};
