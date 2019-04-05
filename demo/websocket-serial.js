function WebSocketSerial(options) {
    var defaults = {
        url: 'ws://127.0.0.1:12212/serial/DISPLAY',
        onConnect: function () {
        },
        onDisconnect: function () {
        },
        onMessage: function (message) {
        }
    };

    var settings = Object.assign({}, defaults, options);
    var websocket;
    var buffer = '';

    var onMessage = function (evt) {
        var chr = evt.data;
        settings.onMessage(chr);
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

    this.send = function (message) {
        websocket.send(message);
    };

    connect();
}