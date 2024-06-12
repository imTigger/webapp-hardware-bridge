; The name of the installer
Name "WebApp Hardware Bridge"

; The file to write
OutFile "whb.exe"

; The default installation directory
InstallDir "$LOCALAPPDATA\WebApp Hardware Bridge"

; Request application privileges for Windows Vista
RequestExecutionLevel user

;--------------------------------

; Pages

;Page directory
Page components
Page instfiles

;--------------------------------

; The stuff to install
Section "!Main Application" ;No components page, name is not important
  SectionIn RO

  ; Set output path to the installation directory.
  SetOutPath $INSTDIR
  
  ; Remove old version
  RMDir /r "$INSTDIR\jre"
  Delete "$INSTDIR\*.jar"
  Delete "$INSTDIR\setting.default.json"
  Delete "$DESKTOP\WebApp Hardware Bridge (GUI).lnk"
  Delete "$DESKTOP\WebApp Hardware Bridge (CLI).lnk"
  Delete "$DESKTOP\WebApp Hardware Bridge (Configurator).lnk"
  Delete "$SMPROGRAMS\WebApp Hardware Bridge (GUI).lnk"
  Delete "$SMPROGRAMS\WebApp Hardware Bridge (CLI).lnk"
  Delete "$SMPROGRAMS\WebApp Hardware Bridge (Configurator).lnk"
  
  ; Put file there
  File /r out\artifacts\webapp_hardware_bridge_jar\*
  File /r jre
  File config.default.json
  
  File "install.nsi"
  
  ; Delete shortcuts  
  Delete "$DESKTOP\WebApp Hardware Bridge.lnk"
  Delete "$DESKTOP\WebApp Hardware Bridge (CLI Websocket).lnk"
  Delete "$DESKTOP\WebApp Hardware Bridge (CLI Web).lnk"
  Delete "$SMPROGRAMS\WebApp Hardware Bridge.lnk"
  Delete "$SMPROGRAMS\WebApp Hardware Bridge (CLI Websocket).lnk"
  Delete "$SMPROGRAMS\WebApp Hardware Bridge (CLI Web).lnk"
  
  ; Create shortcuts
  CreateShortcut "$DESKTOP\WebApp Hardware Bridge.lnk" "$INSTDIR\jre\bin\javaw.exe" "--module-path . --add-modules=javafx.controls -cp webapp-hardware-bridge.jar tigerworkshop.webapphardwarebridge.GUI"
  CreateShortcut "$SMPROGRAMS\WebApp Hardware Bridge.lnk" "$INSTDIR\jre\bin\javaw.exe" "--module-path . --add-modules=javafx.controls -cp webapp-hardware-bridge.jar tigerworkshop.webapphardwarebridge.GUI"

  ; Write the installation path into the registry
  WriteRegStr HKCU "SOFTWARE\WebApp Hardware Bridge" "Install_Dir" "$INSTDIR"
  
  ; Write the uninstall keys for Windows
  WriteRegStr HKCU "Software\Microsoft\Windows\CurrentVersion\Uninstall\WebApp Hardware Bridge" "DisplayName" "WebApp Hardware Bridge"
  WriteRegStr HKCU "Software\Microsoft\Windows\CurrentVersion\Uninstall\WebApp Hardware Bridge" "UninstallString" '"$INSTDIR\uninstall.exe"'
  WriteRegDWORD HKCU "Software\Microsoft\Windows\CurrentVersion\Uninstall\WebApp Hardware Bridge" "NoModify" 1
  WriteRegDWORD HKCU "Software\Microsoft\Windows\CurrentVersion\Uninstall\WebApp Hardware Bridge" "NoRepair" 1
  WriteUninstaller "uninstall.exe"

  ; Auto close when finished
  SetAutoClose true
SectionEnd ; end the section

Section /o "Additional Shortcuts" shortcuts
  CreateShortcut "$DESKTOP\WebApp Hardware Bridge (CLI WebSocket).lnk" "$INSTDIR\jre\bin\java.exe" "-cp webapp-hardware-bridge.jar tigerworkshop.webapphardwarebridge.WebSocketServer"
  CreateShortcut "$DESKTOP\WebApp Hardware Bridge (CLI Web).lnk" "$INSTDIR\jre\bin\javaw.exe" "-cp webapp-hardware-bridge.jar tigerworkshop.webapphardwarebridge.WebAPIServer"
  
  CreateShortcut "$SMPROGRAMS\WebApp Hardware Bridge (CLI WebSocket).lnk" "$INSTDIR\jre\bin\java.exe" "-cp webapp-hardware-bridge.jar tigerworkshop.webapphardwarebridge.WebSocketServer"
  CreateShortcut "$SMPROGRAMS\WebApp Hardware Bridge (CLI Web).lnk" "$INSTDIR\jre\bin\javaw.exe" "-cp webapp-hardware-bridge.jar tigerworkshop.webapphardwarebridge.WebAPIServer"
SectionEnd

Section "Auto-start" autostart
  CreateShortcut "$SMSTARTUP\WebApp Hardware Bridge.lnk" "$INSTDIR\jre\bin\javaw.exe" "--module-path . --add-modules=javafx.controls -cp webapp-hardware-bridge.jar tigerworkshop.webapphardwarebridge.GUI"
SectionEnd

Section "Uninstall"
  ; Remove registry keys
  DeleteRegKey HKCU "Software\Microsoft\Windows\CurrentVersion\Uninstall\WebApp Hardware Bridge"
  DeleteRegKey HKCU "SOFTWARE\WebApp Hardware Bridge"
  
  ; Delete shortcuts
  Delete "$DESKTOP\WebApp Hardware Bridge.lnk"
  Delete "$DESKTOP\WebApp Hardware Bridge (CLI Websocket).lnk"
  Delete "$DESKTOP\WebApp Hardware Bridge (CLI Web).lnk"
  Delete "$SMPROGRAMS\WebApp Hardware Bridge.lnk"
  Delete "$SMPROGRAMS\WebApp Hardware Bridge (CLI Websocket).lnk"
  Delete "$SMPROGRAMS\WebApp Hardware Bridge (CLI Web).lnk"
  
  ; Remove files and uninstaller
  RMDir /r $INSTDIR
SectionEnd

Function .onInstSuccess
  ExecShell "" "$DESKTOP\WebApp Hardware Bridge.lnk"
FunctionEnd