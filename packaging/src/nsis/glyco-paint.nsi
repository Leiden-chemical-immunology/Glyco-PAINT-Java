; Glyco-PAINT installer for Windows (NSIS).
;
; Built on the Mac by packaging/pom.xml (makensis), from the assembled Glyco-PAINT folder:
; Glyco-PAINT.exe and the separate app .exe files, app\ (the JARs) and runtime\ (Java). The
; installer needs no Java and no administrator rights: it installs for the current user, by
; default in %LOCALAPPDATA%\Programs\Glyco-PAINT.
;
; Components: Glyco-PAINT (the Launcher with all apps) and each app on its own. They all share
; the one Java runtime, which is always installed.
;
; Defines passed by the build:
;   SOURCE_DIR  the assembled Glyco-PAINT folder
;   OUTFILE     the installer to write
;   VERSION     the project version, e.g. 0.0.139-SNAPSHOT
;   VIVERSION   the same as four numbers, e.g. 0.0.139.0 (Windows file version)

Unicode true
SetCompressor /SOLID lzma

!include "MUI2.nsh"

!define APP_NAME   "Glyco-PAINT"
!define MENU_DIR   "$SMPROGRAMS\${APP_NAME}"
!define UNINST_KEY "Software\Microsoft\Windows\CurrentVersion\Uninstall\${APP_NAME}"

Name "${APP_NAME}"
OutFile "${OUTFILE}"
RequestExecutionLevel user
InstallDir "$LOCALAPPDATA\Programs\${APP_NAME}"
InstallDirRegKey HKCU "Software\${APP_NAME}" "InstallDir"

VIProductVersion "${VIVERSION}"
VIAddVersionKey "ProductName"     "${APP_NAME}"
VIAddVersionKey "ProductVersion"  "${VERSION}"
VIAddVersionKey "FileVersion"     "${VERSION}"
VIAddVersionKey "FileDescription" "${APP_NAME} installer"
VIAddVersionKey "CompanyName"     "Glyco-PAINT"
VIAddVersionKey "LegalCopyright"  "MIT License"

; ------------------------------------------------------------------------------------------
; Pages
; ------------------------------------------------------------------------------------------
!define MUI_ABORTWARNING
!define MUI_COMPONENTSPAGE_SMALLDESC
!define MUI_FINISHPAGE_RUN
!define MUI_FINISHPAGE_RUN_TEXT "Start ${APP_NAME}"
!define MUI_FINISHPAGE_RUN_FUNCTION StartInstalledApp

!insertmacro MUI_PAGE_WELCOME
!insertmacro MUI_PAGE_COMPONENTS
!insertmacro MUI_PAGE_DIRECTORY
!insertmacro MUI_PAGE_INSTFILES
!insertmacro MUI_PAGE_FINISH

!insertmacro MUI_UNPAGE_CONFIRM
!insertmacro MUI_UNPAGE_INSTFILES

!insertmacro MUI_LANGUAGE "English"

; Installs one app JAR (any version of it from an earlier install is removed first, so no old
; JAR is left behind and other components keep their own JARs).
!macro APP_JAR PREFIX
    Delete "$INSTDIR\app\${PREFIX}-*.jar"
    SetOutPath "$INSTDIR\app"
    File "${SOURCE_DIR}/app/${PREFIX}-*.jar"
!macroend

; Installs one .exe with its Start menu entry (working folder: the install folder).
!macro APP_EXE NAME
    SetOutPath "$INSTDIR"
    File "${SOURCE_DIR}/${NAME}.exe"
    CreateDirectory "${MENU_DIR}"
    CreateShortcut "${MENU_DIR}\${NAME}.lnk" "$INSTDIR\${NAME}.exe"
!macroend

; ------------------------------------------------------------------------------------------
; Install
; ------------------------------------------------------------------------------------------

; Always: the Java runtime, the uninstaller and the entry under Settings > Apps
Section "-Java runtime" SecJava
    RMDir /r "$INSTDIR\runtime"
    SetOutPath "$INSTDIR"
    File /r "${SOURCE_DIR}/runtime"

    ; A single Start menu shortcut from an earlier version of this installer
    Delete "$SMPROGRAMS\${APP_NAME}.lnk"

    WriteUninstaller "$INSTDIR\Uninstall.exe"
    WriteRegStr HKCU "Software\${APP_NAME}" "InstallDir" "$INSTDIR"

    WriteRegStr   HKCU "${UNINST_KEY}" "DisplayName"     "${APP_NAME}"
    WriteRegStr   HKCU "${UNINST_KEY}" "DisplayVersion"  "${VERSION}"
    WriteRegStr   HKCU "${UNINST_KEY}" "Publisher"       "Glyco-PAINT"
    WriteRegStr   HKCU "${UNINST_KEY}" "InstallLocation" "$INSTDIR"
    WriteRegStr   HKCU "${UNINST_KEY}" "UninstallString" '"$INSTDIR\Uninstall.exe"'
    WriteRegDWORD HKCU "${UNINST_KEY}" "NoModify" 1
    WriteRegDWORD HKCU "${UNINST_KEY}" "NoRepair" 1
SectionEnd

Section "Glyco-PAINT (Launcher with all apps)" SecLauncher
    !insertmacro APP_JAR "paint-launcher"
    !insertmacro APP_JAR "paint-get-omero"
    !insertmacro APP_JAR "paint-create-experiment"
    !insertmacro APP_JAR "paint-generate-squares"
    !insertmacro APP_JAR "paint-viewer"
    !insertmacro APP_EXE "Glyco-PAINT"
    WriteRegStr HKCU "${UNINST_KEY}" "DisplayIcon" "$INSTDIR\Glyco-PAINT.exe"
SectionEnd

Section /o "Viewer" SecViewer
    !insertmacro APP_JAR "paint-viewer"
    !insertmacro APP_EXE "Viewer"
SectionEnd

Section /o "Generate Squares" SecGenerateSquares
    !insertmacro APP_JAR "paint-generate-squares"
    !insertmacro APP_EXE "Generate Squares"
SectionEnd

Section /o "Create Experiment" SecCreateExperiment
    !insertmacro APP_JAR "paint-create-experiment"
    !insertmacro APP_EXE "Create Experiment"
SectionEnd

Section /o "Get Omero" SecGetOmero
    !insertmacro APP_JAR "paint-get-omero"
    !insertmacro APP_EXE "Get Omero"
SectionEnd

; Desktop shortcuts for whatever is installed (also from an earlier run of the installer)
Section /o "Desktop shortcuts" SecDesktop
    SetOutPath "$INSTDIR"
    IfFileExists "$INSTDIR\Glyco-PAINT.exe"       0 +2
        CreateShortcut "$DESKTOP\Glyco-PAINT.lnk"       "$INSTDIR\Glyco-PAINT.exe"
    IfFileExists "$INSTDIR\Viewer.exe"            0 +2
        CreateShortcut "$DESKTOP\Viewer.lnk"            "$INSTDIR\Viewer.exe"
    IfFileExists "$INSTDIR\Generate Squares.exe"  0 +2
        CreateShortcut "$DESKTOP\Generate Squares.lnk"  "$INSTDIR\Generate Squares.exe"
    IfFileExists "$INSTDIR\Create Experiment.exe" 0 +2
        CreateShortcut "$DESKTOP\Create Experiment.lnk" "$INSTDIR\Create Experiment.exe"
    IfFileExists "$INSTDIR\Get Omero.exe"         0 +2
        CreateShortcut "$DESKTOP\Get Omero.lnk"         "$INSTDIR\Get Omero.exe"
SectionEnd

!insertmacro MUI_FUNCTION_DESCRIPTION_BEGIN
    !insertmacro MUI_DESCRIPTION_TEXT ${SecLauncher}         "The Glyco-PAINT Launcher, from which all apps can be started."
    !insertmacro MUI_DESCRIPTION_TEXT ${SecViewer}           "The Viewer as a separate app."
    !insertmacro MUI_DESCRIPTION_TEXT ${SecGenerateSquares}  "Generate Squares as a separate app."
    !insertmacro MUI_DESCRIPTION_TEXT ${SecCreateExperiment} "Create Experiment as a separate app."
    !insertmacro MUI_DESCRIPTION_TEXT ${SecGetOmero}         "Get Omero as a separate app."
    !insertmacro MUI_DESCRIPTION_TEXT ${SecDesktop}          "Desktop shortcuts for the installed apps."
!insertmacro MUI_FUNCTION_DESCRIPTION_END

; "Start Glyco-PAINT" on the finish page: the Launcher if installed, otherwise the first app
Function StartInstalledApp
    SetOutPath "$INSTDIR"
    IfFileExists "$INSTDIR\Glyco-PAINT.exe" 0 +3
        Exec '"$INSTDIR\Glyco-PAINT.exe"'
        Return
    IfFileExists "$INSTDIR\Viewer.exe" 0 +3
        Exec '"$INSTDIR\Viewer.exe"'
        Return
    IfFileExists "$INSTDIR\Generate Squares.exe" 0 +3
        Exec '"$INSTDIR\Generate Squares.exe"'
        Return
    IfFileExists "$INSTDIR\Create Experiment.exe" 0 +3
        Exec '"$INSTDIR\Create Experiment.exe"'
        Return
    IfFileExists "$INSTDIR\Get Omero.exe" 0 +2
        Exec '"$INSTDIR\Get Omero.exe"'
FunctionEnd

; ------------------------------------------------------------------------------------------
; Uninstall: only what the installer put there (the folder may have been chosen by the user)
; ------------------------------------------------------------------------------------------
Section "Uninstall"
    RMDir /r "${MENU_DIR}"
    Delete "$SMPROGRAMS\${APP_NAME}.lnk"
    Delete "$DESKTOP\Glyco-PAINT.lnk"
    Delete "$DESKTOP\Viewer.lnk"
    Delete "$DESKTOP\Generate Squares.lnk"
    Delete "$DESKTOP\Create Experiment.lnk"
    Delete "$DESKTOP\Get Omero.lnk"

    Delete "$INSTDIR\Glyco-PAINT.exe"
    Delete "$INSTDIR\Viewer.exe"
    Delete "$INSTDIR\Generate Squares.exe"
    Delete "$INSTDIR\Create Experiment.exe"
    Delete "$INSTDIR\Get Omero.exe"
    RMDir /r "$INSTDIR\app"
    RMDir /r "$INSTDIR\runtime"
    Delete "$INSTDIR\Uninstall.exe"
    RMDir "$INSTDIR"

    DeleteRegKey HKCU "${UNINST_KEY}"
    DeleteRegKey HKCU "Software\${APP_NAME}"
SectionEnd
