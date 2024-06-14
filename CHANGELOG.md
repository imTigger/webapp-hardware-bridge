# WebApp Hardware Bridge

## Introduction

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
- Per printer settings

### Web Serial Access
- Bidirectional communication
- Support multiple ports, mapped by key
- Support multiple connection share same serial port
- Serial weigh scale (AWH-SA30 supported out-of-box in JS SDK)
- Per port settings (Baudrate, data bits, stop bit, parity bit)

## How to use?

### Client Side

1. Install and setup mapping via Web UI

2. Start "WebApp Hardware Bridge" and start using your WebApp

### WebApp Side

1. Check [JS SDK/Example](demo)

## How it works?

WebApp Hardware Bridge is a Java based application, which have more access to hardwares.

It exposes a WebSocket server on localhost which accept print jobs and serial connection.


For print jobs, PDF/Images job are downloaded/decoded and then sent to mapped printer.

Raw job are sent to mapped printer directly.


For serial port, serial port are opened by Java and "proxied" as WebSocket stream,

which allow bidirectional communications.

Web UI is provided to set up mappings between keys and printers/serials.

Therefore, web apps do not need to care about the actual printer names.

## Advanced Usages

- [HTTPS/WSS Support](../../wiki/HTTPS-WSS-Support)
- [Authentication](../../wiki/Authentication)

## FAQs

- Configurator/GUI do not run? Install [vc_redist.x64.exe](https://www.microsoft.com/en-US/download/details.aspx?id=48145)

- [Build from source](../../wiki/Build-from-source)

## Upgrade

- Settings will lost after upgrade from 0.x to 1.0, please reconfigure via "Web UI" or "Web API"

## Changelogs

- [Changelogs](CHANGELOG.md)