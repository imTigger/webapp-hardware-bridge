# Changelogs

## From 0.x to 1.0

- 1.0 is a major rewrite, while maintain compatibility with existing WebApps
- Settings will lost after upgrade, please reconfigure via "Web UI" or "Web API"

### Feature changes
- Added per printer settings (Auto-rotate, DPI...)
- Added per serial port settings (Baud-rate, data bits, stop bit, parity bit)
- Added "Web UI" for configuration, replacing "Configurator"
- Added "Web API", a HTTP API for WebApp to configure directly without using "Web UI" or "Configurator"
- Config file renamed from "setting.json" to "config.json", which is in different format

### Internal changes
- Removed "Configurator"
- Removed undocumented feature "Cloud Proxy"
- Removed usage of JavaFX
- Rewrite config code
- Implementation of WebSocket changed from "Java-WebSocket" to "Javalin"
- Internal dataflow optimization
- Simplified code by using "Lombok"
- Upgrade Java version from 8 to 21
- Many dependencies upgrades and security fixes