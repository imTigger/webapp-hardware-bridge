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
Page instfiles

;--------------------------------

; The stuff to install
Section "" ;No components page, name is not important

  ; Set output path to the installation directory.
  SetOutPath $INSTDIR
  
  ; Put file there
  File /r out\artifacts\webapp_hardware_bridge_jar\*
  File /r jre
  
  File "install.nsi"
  
  ; Create shortcuts
  CreateShortcut "$DESKTOP\WebApp Hardware Bridge.lnk" "$INSTDIR\jre\bin\java.exe" "-cp webapp-hardware-bridge.jar tigerworkshop.webapphardwarebridge.Server"
  CreateShortcut "$DESKTOP\WebApp Hardware Bridge (Configurator).lnk" "$INSTDIR\jre\bin\javaw.exe" "-cp webapp-hardware-bridge.jar tigerworkshop.webapphardwarebridge.config.Configurator"
  CreateShortcut "$SMPROGRAMS\WebApp Hardware Bridge.lnk" "$INSTDIR\jre\bin\java.exe" "-cp webapp-hardware-bridge.jar tigerworkshop.webapphardwarebridge.Server"
  CreateShortcut "$SMPROGRAMS\WebApp Hardware Bridge (Configurator).lnk" "$INSTDIR\jre\bin\javaw.exe" "-cp webapp-hardware-bridge.jar tigerworkshop.webapphardwarebridge.config.Configurator"
  
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

Section "Uninstall"
  
  ; Remove registry keys
  DeleteRegKey HKCU "Software\Microsoft\Windows\CurrentVersion\Uninstall\WebApp Hardware Bridge"
  DeleteRegKey HKCU "SOFTWARE\WebApp Hardware Bridge"
  
  ; Delete shortcuts
  Delete "$DESKTOP\WebApp Hardware Bridge.lnk"
  Delete "$SMPROGRAMS\WebApp Hardware Bridge.lnk"
  Delete "$SMPROGRAMS\WebApp Hardware Bridge (Setting).lnk"
  
  ; Remove files and uninstaller
  RMDir /r $INSTDIR

SectionEnd