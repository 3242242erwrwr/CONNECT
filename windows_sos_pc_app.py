import tkinter as tk
from tkinter import messagebox
import socket
import threading
import json
import time
import winsound
import requests

CLOUD_URL = "https://ntfy.sh/connect_family_sos_global_channel_2026"
MY_DEVICE_ID = "WINDOWS_PC_CLIENT"

is_siren_playing = False

def play_windows_siren():
    global is_siren_playing
    is_siren_playing = True
    def siren_thread():
        while is_siren_playing:
            try:
                winsound.Beep(2500, 400)
                time.sleep(0.1)
                winsound.Beep(1800, 400)
                time.sleep(0.1)
            except Exception:
                break
    threading.Thread(target=siren_thread, daemon=True).start()

def stop_windows_siren():
    global is_siren_playing
    is_siren_playing = False
    messagebox.showinfo("Connect SOS", "🛑 Windows PC: Sirena ovozi to'xtatildi!")

def send_sos_alert(alert_text):
    full_msg = f"{alert_text} (Kimdan: Windows PC Client)"

    # 1. Send Cloud Push Request
    def cloud_send():
        try:
            requests.post(CLOUD_URL, data=full_msg.encode('utf-8'), headers={"Title": "🚨 SHOSHILINCH SOS SIGNAL!", "Priority": "5"}, timeout=3)
        except Exception:
            pass

    threading.Thread(target=cloud_send, daemon=True).start()

    # 2. Send UDP Local Broadcast
    def udp_send():
        try:
            sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
            sock.setsockopt(socket.SOL_SOCKET, socket.SO_BROADCAST, 1)
            payload = f"SOS_PACKET::{MY_DEVICE_ID}::{full_msg}".encode('utf-8')
            for target_ip in ["255.255.255.255", "192.168.100.255", "192.168.43.255"]:
                try:
                    sock.sendto(payload, (target_ip, 8888))
                except Exception:
                    pass
            sock.close()
        except Exception:
            pass

    threading.Thread(target=udp_send, daemon=True).start()
    messagebox.showinfo("Connect SOS", f"📡 Signal Yuborildi: {alert_text}")

# UDP Listener Background Thread
def start_udp_listener(status_label):
    try:
        sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
        sock.bind(('0.0.0.0', 8888))
        while True:
            data, addr = sock.recvfrom(1024)
            msg = data.decode('utf-8', errors='ignore')
            if msg.startswith("SOS_PACKET::"):
                parts = msg.split("::")
                if len(parts) >= 3 and parts[1] != MY_DEVICE_ID:
                    alert_content = parts[2]
                    status_label.config(text=f"🚨 KELGAN SIGNAL: {alert_content}", fg="#FF5252")
                    play_windows_siren()
    except Exception:
        pass

# Cloud Stream Listener Thread
def start_cloud_listener(status_label):
    last_msg = ""
    while True:
        try:
            response = requests.get(f"{CLOUD_URL}/json", stream=True, timeout=10)
            for line in response.iter_lines():
                if line:
                    try:
                        data = json.loads(line.decode('utf-8'))
                        if data.get("event") == "message":
                            msg_text = data.get("message", "")
                            if msg_text and msg_text != last_msg and "Windows PC" not in msg_text:
                                last_msg = msg_text
                                status_label.config(text=f"🌐 4G CLOUD SIGNAL: {msg_text}", fg="#FF5252")
                                play_windows_siren()
                    except Exception:
                        pass
        except Exception:
            pass
        time.sleep(2)

# Build Windows PC GUI
def main():
    root = tk.Tk()
    root.title("Connect SOS — Windows PC Client v1.0")
    root.geometry("450x550")
    root.configure(bg="#0F1116")

    # Header
    header_frame = tk.Frame(root, bg="#1E222B", pady=10, px=10)
    header_frame.pack(fill="x")

    title_label = tk.Label(header_frame, text="🚨 Connect SOS — Windows PC Client", font=("Segoe UI", 13, "bold"), fg="#FFFFFF", bg="#1E222B")
    title_label.pack(anchor="w")

    subtitle_label = tk.Label(header_frame, text="Windows PC ↔ Android (4G/5G/Wi-Fi Cross Platform)", font=("Segoe UI", 9), fg="#A0AAB8", bg="#1E222B")
    subtitle_label.pack(anchor="w")

    status_label = tk.Label(root, text="🟢 WINDOWS PC ONLINE — SIGNAL KUTILMOQDA...", font=("Segoe UI", 10, "bold"), fg="#4CAF50", bg="#0F1116", pady=15)
    status_label.pack()

    # SOS Buttons Frame
    btn_frame = tk.Frame(root, bg="#0F1116", pady=10)
    btn_frame.pack(fill="x", px=20)

    btn1 = tk.Button(btn_frame, text="📢 SAIDBEKKA QARA", font=("Segoe UI", 11, "bold"), bg="#E53935", fg="white", height=2, command=lambda: send_sos_alert("🚨 SAIDBEKKA QARA!"))
    btn1.pack(fill="x", pady=5)

    btn2 = tk.Button(btn_frame, text="🔔 JASMINAHON QANI", font=("Segoe UI", 11, "bold"), bg="#FB8C00", fg="white", height=2, command=lambda: send_sos_alert("🚨 JASMINAHON QANI?"))
    btn2.pack(fill="x", pady=5)

    btn3 = tk.Button(btn_frame, text="🆘 UYGA SHOSHILINCH KELING", font=("Segoe UI", 10, "bold"), bg="#8E24AA", fg="white", height=2, command=lambda: send_sos_alert("🆘 UYGA SHOSHILINCH KELING!"))
    btn3.pack(fill="x", pady=5)

    # STOP SIREN BUTTON
    stop_btn = tk.Button(root, text="🛑 STOP SIRENA (OVOZNI TO'XTATISH)", font=("Segoe UI", 12, "bold"), bg="#D32F2F", fg="white", height=2, command=stop_windows_siren)
    stop_btn.pack(fill="x", px=20, pady=15)

    # Start Background Threads
    threading.Thread(target=start_udp_listener, args=(status_label,), daemon=True).start()
    threading.Thread(target=start_cloud_listener, args=(status_label,), daemon=True).start()

    root.mainloop()

if __name__ == "__main__":
    main()
