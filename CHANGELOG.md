# Changelogs

## From 0.x to 1.0

- 1.0 is a major rewrite, while maintain compatibility with existing WebApps

### Feature changes
- Settings will lost after upgrade, please reconfigure via "Web UI" or "Web API"
- Added per printer settings (Auto-rotate, DPI...)
- Added per serial port settings (Baudrate, data bits, stop bit, parity bit)
- Added "Web UI" for configuration, replacing "Configurator"
- Added "Web API" for WebApp to configured directly without "Web UI" or "Configurator"

### Internal changes
- Rewrite config code
- Config file renamed from "setting.json" to "config.json", which is in different format
- Removed "Configurator"
- Removed undocumented feature "Cloud Proxy"
- Removed usage of JavaFX
- Implementation of WebSocket changed from "Java-WebSocket" to "Javalin"
- Simplified code by using "Lombok"
- Upgrade from Java version from 8 to 21