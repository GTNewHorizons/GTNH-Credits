; NSIS installer script for GTNH Credits Editor.
;
; Built by the :creditsEditor:windowsInstaller Gradle task. The task copies
; this file into a staging directory alongside the fat jar, launcher .bat,
; and a generated .ico file, then invokes makensis with /D defines for the
; substitutable values (VERSION, OUTFILE).

!include "MUI2.nsh"

!define APP_NAME       "GTNH Credits Editor"
!define APP_PUBLISHER  "GTNewHorizons"
!define APP_EXE        "gtnh-credits-editor.bat"
!define APP_JAR        "gtnh-credits-editor.jar"
!define APP_ICON       "gtnh-credits-editor.ico"
!define APP_REGKEY     "Software\GTNH-Credits-Editor"
!define APP_UNINSTKEY  "Software\Microsoft\Windows\CurrentVersion\Uninstall\GTNH-Credits-Editor"

!ifndef VERSION
  !define VERSION "0.0.0"
!endif

!ifndef OUTFILE
  !define OUTFILE "gtnh-credits-editor-setup.exe"
!endif

Name "${APP_NAME}"
OutFile "${OUTFILE}"
InstallDir "$PROGRAMFILES64\GTNH Credits Editor"
InstallDirRegKey HKLM "${APP_REGKEY}" "InstallDir"
RequestExecutionLevel admin
SetCompressor /SOLID lzma
Unicode true

!define MUI_ICON   "${APP_ICON}"
!define MUI_UNICON "${APP_ICON}"
!define MUI_ABORTWARNING

!insertmacro MUI_PAGE_WELCOME
!insertmacro MUI_PAGE_DIRECTORY
!insertmacro MUI_PAGE_INSTFILES
!insertmacro MUI_PAGE_FINISH

!insertmacro MUI_UNPAGE_CONFIRM
!insertmacro MUI_UNPAGE_INSTFILES

!insertmacro MUI_LANGUAGE "English"

Section "Install"
    SetOutPath "$INSTDIR"

    File "${APP_JAR}"
    File "${APP_EXE}"
    File "${APP_ICON}"

    WriteRegStr HKLM "${APP_REGKEY}" "InstallDir" "$INSTDIR"

    WriteRegStr HKLM "${APP_UNINSTKEY}" "DisplayName"     "${APP_NAME}"
    WriteRegStr HKLM "${APP_UNINSTKEY}" "DisplayVersion"  "${VERSION}"
    WriteRegStr HKLM "${APP_UNINSTKEY}" "Publisher"       "${APP_PUBLISHER}"
    WriteRegStr HKLM "${APP_UNINSTKEY}" "DisplayIcon"     "$INSTDIR\${APP_ICON}"
    WriteRegStr HKLM "${APP_UNINSTKEY}" "InstallLocation" "$INSTDIR"
    WriteRegStr HKLM "${APP_UNINSTKEY}" "UninstallString" '"$INSTDIR\uninstall.exe"'
    WriteRegDWORD HKLM "${APP_UNINSTKEY}" "NoModify" 1
    WriteRegDWORD HKLM "${APP_UNINSTKEY}" "NoRepair" 1

    CreateDirectory "$SMPROGRAMS\${APP_NAME}"
    CreateShortcut "$SMPROGRAMS\${APP_NAME}\${APP_NAME}.lnk" \
        "$INSTDIR\${APP_EXE}" "" "$INSTDIR\${APP_ICON}" 0
    CreateShortcut "$SMPROGRAMS\${APP_NAME}\Uninstall.lnk" \
        "$INSTDIR\uninstall.exe" "" "$INSTDIR\uninstall.exe" 0
    CreateShortcut "$DESKTOP\${APP_NAME}.lnk" \
        "$INSTDIR\${APP_EXE}" "" "$INSTDIR\${APP_ICON}" 0

    WriteUninstaller "$INSTDIR\uninstall.exe"
SectionEnd

Section "Uninstall"
    Delete "$INSTDIR\${APP_JAR}"
    Delete "$INSTDIR\${APP_EXE}"
    Delete "$INSTDIR\${APP_ICON}"
    Delete "$INSTDIR\uninstall.exe"
    RMDir  "$INSTDIR"

    Delete "$SMPROGRAMS\${APP_NAME}\${APP_NAME}.lnk"
    Delete "$SMPROGRAMS\${APP_NAME}\Uninstall.lnk"
    RMDir  "$SMPROGRAMS\${APP_NAME}"
    Delete "$DESKTOP\${APP_NAME}.lnk"

    DeleteRegKey HKLM "${APP_REGKEY}"
    DeleteRegKey HKLM "${APP_UNINSTKEY}"
SectionEnd