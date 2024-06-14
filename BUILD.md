# Build Instructions

## Build from source

- JDK 21, [Eclipse Temurin 21](https://adoptium.net/en-GB/temurin/releases/) Recommanded
- Intelij IDEA (Both Community and Ultimate works)

1. An artifact config file is included in git repository.

2. Use Intelij IDEA to "Build artifact" to yield `out\artifacts\webapp_hardware_bridge_jar`.

## Windows Installer bundled with JRE

- JRE 21, [Eclipse Temurin 21](https://adoptium.net/en-GB/temurin/releases/) Recommanded
- [Nullsoft Scriptable Install System](https://nsis.sourceforge.io/) 

1. Follow "Build from source" instructions to yield `out\artifacts\webapp_hardware_bridge_jar`

2. Copy JRE 21 into `./jre` directory 

3. Run `install.nsi` with NSIS to yield `whb.exe`

## How to run

1. Start application
   - GUI: `javaw -cp webapp-hardware-bridge.jar tigerworkshop.webapphardwarebridge.GUI`
   - Server: `java -cp webapp-hardware-bridge.jar tigerworkshop.webapphardwarebridge.Server`