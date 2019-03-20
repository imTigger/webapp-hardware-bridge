# WebApp Hardware Bridge

WebApp Hardware Bridge (succeeder of "Chrome Hardware Bridge / Chrome Direct Print")

Make it possible for webpages to silent print and access to serial ports.

Common use cases:
- Web-based POS PDF and receipt silent print
- Web-based WMS serial weight scale realtime reading

## Features

- [x] Direct print from webpage
- [x] Serial read/write from webpage
- [x] Support all modern broswers that implemented WebSocket (Chrome, Firefox, Edge... etc)
- [x] [JS SDK/Example included](demo)

### Web Direct Print
- 0-click silent printing in web browsers
- Support printers supported, mapped by key
- Support PDF/PNG/JPG Printing
- Support RAW/ESC-POS Printing

### Web Serial Access
- Bidirectional communication
- Serial weigh scale (AWH-SA30 support in SDK)
- Support multiple printers, mapped by key
- Support multiple connection share same serial port

## How to use?

### Client Side

1. Install and setup mapping via Configurator

2. Start "WebApp Hardware Bridge" and it should works

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

## TODOs
- [ ] Better GUI
- [ ] Serial JS SDK
- [ ] Serial settings (Baudrate, data bits, stop bit, parity bit)
- [ ] Print result feedback
- [ ] Authentication

Any other ideas? Fork and PR are welcome!
