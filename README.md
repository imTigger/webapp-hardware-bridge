# WebApp Hardware Bridge

WebApp Hardware Bridge (succeeder of "Chrome Hardware Bridge / Chrome Direct Print")

Make it possible for webpages to silent print and access to serial ports.

Common use cases:
- Web-based POS PDF and receipt silent print
- Web-based WMS serial weight scale realtime reading

## Features

- [x] Direct print from webpage
- [x] Serial read/write from webpage
- [x] Support all modern broswers that implemented WebSocket (Chrome, Edge, Firefox... etc)
- [x] [JS SDK/Example included](demo)

### Web Direct Print
- 0-click direct printing in web browsers
- Support PDF/PNG/JPG Printing
- Support RAW/ESC-POS Printing

### Web Serial Access
- Bidirectional communication
- Serial weigh scale (AWH-SA30 support in SDK)
- Support multiple connection share same serial port

## TODOs
- [ ] Serial JS SDK
- [ ] Serial settings (Baudrate, data bits, stop bit, parity bit)
- [ ] Print result feedback
- [ ] Authentication

Any other ideas? Fork and PR are welcome!
