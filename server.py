from flask import Flask, request, jsonify
import time

app = Flask(__name__)

# Global storage for the latest SOS alert
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
                "timestamp": time.strftime("%H:%mm:%ss")
            }
        return jsonify({"status": "SUCCESS", "latest_alert": latest_alert})
    else:
        return jsonify({"status": "SUCCESS", "latest_alert": latest_alert})

@app.route('/')
def home():
    return "🚀 Connect SOS Global Cloud Relay Server is Running Active!"

if __name__ == '__main__':
    app.run(host='0.0.0.0', port=10000)
