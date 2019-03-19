function WebSocketPrinter(options) {
    var defaults = {
        url: "ws://127.0.0.1:12212/printer",
        onConnect: function () {
        },
        onDisconnect: function () {
        },
        onUpdate: function (message) {
        }
    };

    var settings = Object.assign({}, defaults, options);
    var websocket;

    var onMessage = function (evt) {
        settings.onUpdate(evt.data);
    };

    var onConnect = function () {
        settings.onConnect();
    };

    var onDisconnect = function () {
        settings.onDisconnect();
        reconnect();
    };

    var connect = function () {
        websocket = new WebSocket(settings.url);
        websocket.onopen = onConnect;
        websocket.onclose = onDisconnect;
        websocket.onmessage = onMessage;
    };

    var reconnect = function () {
        connect();
    };

    this.submit = function (data) {
        if (Array.isArray(data)) {
            data.forEach(function (element) {
                websocket.send(JSON.stringify(element));
            });
        } else {
            websocket.send(JSON.stringify(data));
        }
    };

    connect();
}