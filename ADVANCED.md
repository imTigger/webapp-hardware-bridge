# Advanced Configurations

## Authentication

### Enable Authentication

Authentication is disabled by default, that any website can connect to your bridge and access local resources.

To prevent unauthorized access, set `server.authentication.enabled` to `true` and `server.authentication.token` to the value you want.

When enabled, connections without correct token will be rejected.

#### WebSocket

Point url to `ws://127.0.0.1:12212/serial/WEIGH?access_token=1234567890` if your `token` is "1234567890"

#### Web UI

Enter `token` as `Password` when prompted and leave `Username` empty.

#### Web API

Use header `Authorization: Bearer 1234567890`

## HTTPS/WSS Support

Some browser does not allow webpage with secure context (i.e. HTTPS)
to connect non-secured WebSocket server.

Either of below methods required to workaround this:

### Allow non-secure WebSocket server from HTTPS website (Not recommended)

Warning: These setting can open a security hole, use on development environment only

Firefox: Go to `about:config`, set `network.websocket.allowInsecureFromHTTPS` to `true`

Chrome: Add `--allow-running-insecure-content` to launching argument

### Enable WebSocket Secure (WSS) with self-signed certificate

WHB have built-in ability to generate self-signed certificate.

Set `server.tls.enabled` to true, `server.tls.selfSigned` to true in `setting.json` and relaunch the application.

Upon start, application should automatically generate a self-signed certificate

and start listening on `wss://127.0.0.1:12212` with secured connection.

On first setup, you must go to `https://127.0.0.1:12212` to accept that self-signed certificate.

After change, point url to `wss://127.0.0.1:12212` instead of `ws://127.0.0.1:12212`

### Enable WebSocket Secure (WSS) with real, user-provided certificate

Copy your certificate and private key to `tls` directory.

Set `server.tls.enabled` to true, `server.tls.selfSigned` to false, `server.tls.cert` and `server.tls.key` in `setting.json` and relaunch the application.

Upon start, application should pickup your certificate and start listening on `wss://127.0.0.1:12212` with secured connection.

After change, point url to `wss://127.0.0.1:12212` instead of `ws://127.0.0.1:12212`

#### How to obtain real TLS Certificate?

WHB is usually listening on 127.0.0.1. It's normally impossible to obtain valid certificates signed for that.

A common workaround is to point your (sub-)domain A Record to 127.0.0.1, and obtain certificate with that

e.g. Point `local.tiger-workshop.com` to `127.0.0.1`, then point your WebApp to `wss://local.tiger-workshop.com:12212`

### Why we can't provide certificate for you

Shipping private key with application is considered kind of "key-compromise".

The certificate will be revoked by CA. It's very easily detected especially for open-source projects.