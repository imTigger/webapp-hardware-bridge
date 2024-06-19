# HTTP APIs

All endpoints have CORS configured to allow requests from any origin.

You can get or update the current configuration in your WebApp directly by using the `/config.json` endpoint.

## GET /config.json

Get content of `config.json` file.

## PUT /config.json

Update content of `config.json` file.

## GET /system/printers.json

Return list of available printers.

## GET /system/serials.json

Return list of available serial ports.

## POST /system/restart.json

Restart WebSocket/Web server