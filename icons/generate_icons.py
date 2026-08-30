from PIL import Image, ImageDraw, ImageFont
import os

OUT = r"C:\Users\LiuJi\Desktop\新建文件夹 (3)\E-Ink-Launcher\icons"
SIZE = 512
BG = 0
FG = 255
M = 80

def new():
    return Image.new("L", (SIZE, SIZE), BG)

def save(img, name):
    img.save(os.path.join(OUT, name), "PNG")

# 1. Lock - padlock
def icon_lock():
    img = new()
    d = ImageDraw.Draw(img)
    cx, cy = SIZE//2, SIZE//2 + 30
    # shackle (arc)
    d.arc([cx-80, cy-140, cx+80, cy-10], 180, 0, fill=FG, width=20)
    # body
    d.rounded_rectangle([cx-90, cy-40, cx+90, cy+110], radius=16, fill=FG)
    # keyhole
    d.ellipse([cx-16, cy+10, cx+16, cy+42], fill=BG)
    d.rectangle([cx-6, cy+30, cx+6, cy+70], fill=BG)
    save(img, "Lock.png")

# 2. WiFi On - signal waves
def icon_wifi_on():
    img = new()
    d = ImageDraw.Draw(img)
    cx, cy = SIZE//2, SIZE//2 + 40
    # dot
    d.ellipse([cx-20, cy-20, cx+20, cy+20], fill=FG)
    # waves
    for i, r in enumerate([60, 100, 140]):
        d.arc([cx-r, cy-r, cx+r, cy+r], 225, 315, fill=FG, width=18)
    save(img, "WifiOn.png")

# 3. WiFi Off - signal waves with slash
def icon_wifi_off():
    img = new()
    d = ImageDraw.Draw(img)
    cx, cy = SIZE//2, SIZE//2 + 40
    d.ellipse([cx-20, cy-20, cx+20, cy+20], fill=FG)
    for r in [60, 100, 140]:
        d.arc([cx-r, cy-r, cx+r, cy+r], 225, 315, fill=FG, width=18)
    # slash
    d.line([M, SIZE-M, SIZE-M, M], fill=FG, width=22)
    save(img, "WifiOff.png")

# 4. KOReader - open book
def icon_koreader():
    img = new()
    d = ImageDraw.Draw(img)
    cx, cy = SIZE//2, SIZE//2
    # left page
    d.rectangle([M+20, M+30, cx-10, SIZE-M-30], outline=FG, width=8)
    # right page
    d.rectangle([cx+10, M+30, SIZE-M-20, SIZE-M-30], outline=FG, width=8)
    # spine
    d.line([cx, M+20, cx, SIZE-M-20], fill=FG, width=8)
    # text lines left
    for y in range(M+80, SIZE-M-60, 40):
        d.line([M+60, y, cx-50, y], fill=FG, width=4)
    # text lines right
    for y in range(M+80, SIZE-M-60, 40):
        d.line([cx+50, y, SIZE-M-60, y], fill=FG, width=4)
    save(img, "org.koreader.launcher.png")

# 5. Settings - gear
def icon_settings():
    img = new()
    d = ImageDraw.Draw(img)
    cx, cy = SIZE//2, SIZE//2
    teeth = 8
    outer_r = 160
    inner_r = 120
    hole_r = 50
    import math
    pts = []
    for i in range(teeth * 2):
        angle = math.pi * i / teeth - math.pi / 2
        r = outer_r if i % 2 == 0 else inner_r
        pts.append((cx + r * math.cos(angle), cy + r * math.sin(angle)))
    d.polygon(pts, fill=FG)
    d.ellipse([cx-hole_r, cy-hole_r, cx+hole_r, cy+hole_r], fill=BG)
    save(img, "com.android.settings.png")

# 6. Messages - speech bubble
def icon_messages():
    img = new()
    d = ImageDraw.Draw(img)
    # bubble
    d.rounded_rectangle([M+30, M+40, SIZE-M-30, SIZE-M-80], radius=40, fill=FG)
    # tail
    d.polygon([(M+100, SIZE-M-80), (M+160, SIZE-M-80), (M+80, SIZE-M-20)], fill=FG)
    # text lines
    for y in range(M+110, SIZE-M-140, 45):
        d.line([M+100, y, SIZE-M-100, y], fill=BG, width=6)
    save(img, "com.android.messaging.png")

# 7. Phone - handset
def icon_phone():
    img = new()
    d = ImageDraw.Draw(img)
    cx, cy = SIZE//2, SIZE//2
    # handset shape - rounded rectangle rotated
    d.rounded_rectangle([cx-140, cy-50, cx+140, cy+50], radius=50, fill=FG)
    # earpiece
    d.rounded_rectangle([cx-140, cy-90, cx-60, cy+90], radius=40, fill=FG)
    # mouthpiece
    d.rounded_rectangle([cx+60, cy-90, cx+140, cy+90], radius=40, fill=FG)
    save(img, "com.android.dialer.png")

# 8. Contacts - person silhouette
def icon_contacts():
    img = new()
    d = ImageDraw.Draw(img)
    cx, cy = SIZE//2, SIZE//2
    # head
    d.ellipse([cx-60, M+60, cx+60, M+180], fill=FG)
    # body
    d.ellipse([cx-120, M+180, cx+120, SIZE-M+20], fill=FG)
    save(img, "com.android.contacts.png")

# 9. EinkBro - spider/bro icon (stylized E + B)
def icon_einkbro():
    img = new()
    d = ImageDraw.Draw(img)
    cx, cy = SIZE//2, SIZE//2
    # circle border
    d.ellipse([M+20, M+20, SIZE-M-20, SIZE-M-20], outline=FG, width=12)
    # E
    d.rectangle([cx-100, cy-80, cx-20, cy-50], fill=FG)
    d.rectangle([cx-100, cy-20, cx-30, cy+10], fill=FG)
    d.rectangle([cx-100, cy+40, cx-20, cy+70], fill=FG)
    d.rectangle([cx-100, cy-80, cx-70, cy+70], fill=FG)
    # B
    d.rectangle([cx+20, cy-80, cx+50, cy+70], fill=FG)
    d.arc([cx+20, cy-80, cx+100, cy+10], 90, 270, fill=FG, width=12)
    d.arc([cx+20, cy-10, cx+100, cy+80], 90, 270, fill=FG, width=12)
    save(img, "info.plateaukao.einkbro.png")

icon_lock()
icon_wifi_on()
icon_wifi_off()
icon_koreader()
icon_settings()
icon_messages()
icon_phone()
icon_contacts()
icon_einkbro()

print("All 9 icons generated in:", OUT)
for f in sorted(os.listdir(OUT)):
    if f.endswith(".png"):
        sz = os.path.getsize(os.path.join(OUT, f))
        print(f"  {f} ({sz} bytes)")
