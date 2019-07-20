# WebApp Hardware Bridge

WebApp Hardware Bridge (succeeder of "Chrome Hardware Bridge / Chrome Direct Print")

Make it possible for WebApp to perform silent print and access to serial ports.

Common use cases:
- Web-based POS - PDF and ESC/POS receipt silent print
- Web-based WMS - Serial weight scale real-time reading, delivery Note/packing List silent print
- WebApp that need to read/write to serial ports

## Features

- [x] Direct print from webpage
- [x] Serial read/write from webpage
- [x] Support all modern broswers that implemented WebSocket (Chrome, Firefox, Edge... etc)
- [x] [JS SDK/Example included](demo)

### Web Direct Print
- 0-click silent printing in web browsers
- Download via URL / Base64 encoded file / Base64 encoded binary raw command
- Support multiple printers, mapped by key
- Support PDF/PNG/JPG Printing
- Support RAW/ESC-POS Printing

### Web Serial Access
- Bidirectional communication
- Support multiple ports, mapped by key
- Support multiple connection share same serial port
- Serial weigh scale (AWH-SA30 supported out-of-box in JS SDK)

## How to use?

### Client Side

1. Install and setup mapping via Configurator

2. Start "WebApp Hardware Bridge" and start using your WebApp

### WebApp Side

1. Check [JS SDK/Example](demo)

## How it works?

WebApp Hardware Bridge is a Java based application, which have more direct access to hardwares.

It expose a WebSocket server on localhost which accept print jobs and serial connection.


For print jobs, PDF/Images job are downloaded/decoded and then sent to mapped printer.

Raw job are sent to mapped directly.


For serial port, serial port are opened by Java and "proxified" as WebSocket stream,

which allow bidirectional communcations.


Configurator is provided to setup mappings between keys and printers/serials.

Therefore web apps do not need to care about the actual printer names.

## Known Issue

- Browser refuse to connect to non-Secure WebSocket server
  Firefox Workaround: Change "network.websocket.allowInsecureFromHTTPS" to true in about:config

## FAQs

- [Build from source](../../wiki/Build-from-source)

## Advanced Usages

- [HTTPS/WSS Support](../../wiki/HTTPS-WSS-Support)
- [Authentication](../../wiki/Authentication)

## TODOs
- [ ] Better GUI
- [ ] Serial settings (Baudrate, data bits, stop bit, parity bit)
- [ ] Authentication
- [ ] HTTPS / WSS support

Any other ideas? Fork and PR are welcome!
