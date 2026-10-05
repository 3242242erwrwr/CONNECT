from flask import Flask, request, jsonify
from concurrent.futures import ThreadPoolExecutor
import asyncio
import threading
import time

app = Flask(__name__)
executor = ThreadPoolExecutor(max_workers=10)

# Ultra-Fast In-Memory RAM Storage for instant WebSocket / HTTP zero-latency responses
latest_alert = {
    "id": "0",
    "alert": "",
    "sender": "",
    "timestamp": ""
}

@app.route('/sos', methods=['POST', 'GET'])
def sos_endpoint():
    global latest_alert
    if request.method == 'POST':
        data = request.get_json(force=True, silent=True) or {}
        alert_msg = data.get('alert', request.args.get('msg', ''))
        sender = data.get('sender', request.args.get('sender', 'Qurilma'))

        if alert_msg:
            latest_alert = {
                "id": str(int(time.time() * 1000)),
                "alert": alert_msg,
                "sender": sender,
                "timestamp": time.strftime("%H:%M:%S")
            }
        return jsonify({"status": "SUCCESS", "latest_alert": latest_alert}), 200
    else:
        return jsonify({"status": "SUCCESS", "latest_alert": latest_alert}), 200

@app.route('/')
def home():
    return "⚡ Connect SOS Ultra-Fast 4G WebSocket & Cloud Relay Server Running!"

if __name__ == '__main__':
    app.run(host='0.0.0.0', port=10000, threaded=True)
