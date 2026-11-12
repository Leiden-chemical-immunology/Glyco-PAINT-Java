; Glyco-PAINT installer for Windows (NSIS).
;
; Built on the Mac by packaging/pom.xml (makensis), from the assembled Glyco-PAINT folder:
; Glyco-PAINT.exe and the separate app .exe files, app\ (the JARs) and runtime\ (Java). The
; installer needs no Java and no administrator rights: it installs for the current user, by
; default in %LOCALAPPDATA%\Programs\Glyco-PAINT.
;
; Components: Glyco-PAINT (the Launcher with all apps) and each app on its own. They all share
; the one Java runtime, which is always installed. The Fiji plugin is copied into the plugins
; folder of a Fiji installation, which the installer looks for (by its contents, in the usual
; places where a Fiji download is unpacked) and lets the user confirm.
;
; Defines passed by the build:
;   SOURCE_DIR  the assembled Glyco-PAINT folder
;   PLUGIN_JAR  the Fiji plugin JAR
;   OUTFILE     the installer to write
;   VERSION     the project version, e.g. 0.0.139-SNAPSHOT
;   VIVERSION   the same as four numbers, e.g. 0.0.139.0 (Windows file version)

Unicode true
SetCompressor /SOLID lzma

!include "MUI2.nsh"
!include "LogicLib.nsh"
!include "Sections.nsh"
!include "StrFunc.nsh"
${Using:StrFunc} StrStr

!define APP_NAME   "Glyco-PAINT"
!define MENU_DIR   "$SMPROGRAMS\${APP_NAME}"
!define UNINST_KEY "Software\Microsoft\Windows\CurrentVersion\Uninstall\${APP_NAME}"

Name "${APP_NAME}"
OutFile "${OUTFILE}"
RequestExecutionLevel user
InstallDir "$LOCALAPPDATA\Programs\${APP_NAME}"
InstallDirRegKey HKCU "Software\${APP_NAME}" "InstallDir"

Var FijiDir   ; the Fiji folder the plugin goes into
Var Apps      ; the apps installed in this run, for the summary on the finish page
Var Summary   ; what was installed where, shown on the finish page
Var PluginFile ; the plugin as installed in Fiji; empty if it could not be installed

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
!define MUI_FINISHPAGE_TITLE "${APP_NAME} ${VERSION} has been installed"
!define MUI_FINISHPAGE_TEXT "$Summary"
!define MUI_FINISHPAGE_TEXT_LARGE

!insertmacro MUI_PAGE_WELCOME
!insertmacro MUI_PAGE_COMPONENTS
!insertmacro MUI_PAGE_DIRECTORY

!define MUI_PAGE_HEADER_TEXT "Fiji folder"
!define MUI_PAGE_HEADER_SUBTEXT "Where the Glyco-PAINT plugin for Fiji goes."
!define MUI_DIRECTORYPAGE_TEXT_TOP "The Glyco-PAINT plugin is copied into the plugins folder of this Fiji installation. An older Glyco-PAINT plugin there is replaced. Choose another folder if this is not your Fiji."
!define MUI_DIRECTORYPAGE_TEXT_DESTINATION "Fiji folder"
!define MUI_DIRECTORYPAGE_VARIABLE $FijiDir
!define MUI_PAGE_CUSTOMFUNCTION_PRE FijiPagePre
!define MUI_PAGE_CUSTOMFUNCTION_LEAVE FijiPageLeave
!insertmacro MUI_PAGE_DIRECTORY

!define MUI_PAGE_CUSTOMFUNCTION_PRE CheckNothingRunning
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
    StrCpy $Apps "$Apps$\r$\n      Glyco-PAINT (Launcher with all apps)"
    WriteRegStr HKCU "${UNINST_KEY}" "DisplayIcon" "$INSTDIR\Glyco-PAINT.exe"
SectionEnd

Section /o "Viewer" SecViewer
    !insertmacro APP_JAR "paint-viewer"
    !insertmacro APP_EXE "Viewer"
    StrCpy $Apps "$Apps$\r$\n      Viewer"
SectionEnd

Section /o "Generate Squares" SecGenerateSquares
    !insertmacro APP_JAR "paint-generate-squares"
    !insertmacro APP_EXE "Generate Squares"
    StrCpy $Apps "$Apps$\r$\n      Generate Squares"
SectionEnd

Section /o "Create Experiment" SecCreateExperiment
    !insertmacro APP_JAR "paint-create-experiment"
    !insertmacro APP_EXE "Create Experiment"
    StrCpy $Apps "$Apps$\r$\n      Create Experiment"
SectionEnd

Section /o "Get Omero" SecGetOmero
    !insertmacro APP_JAR "paint-get-omero"
    !insertmacro APP_EXE "Get Omero"
    StrCpy $Apps "$Apps$\r$\n      Get Omero"
SectionEnd

; The Fiji plugin: replaces an older Glyco-PAINT plugin in Fiji's plugins folder. Windows cannot
; delete a JAR that a running Fiji has open; the new plugin would then end up next to the old one
; and Fiji might load the old one. So the old one must be gone first: the user is asked to close
; Fiji and retry, or to skip the plugin.
Section "Fiji plugin" SecFiji
    StrCpy $PluginFile ""
  fiji_remove_old:
    Delete "$FijiDir\plugins\paint-fiji-plugin*.jar"
    FindFirst $0 $1 "$FijiDir\plugins\paint-fiji-plugin*.jar"
    FindClose $0
    ${If} $1 != ""
        MessageBox MB_RETRYCANCEL|MB_ICONEXCLAMATION "The older Glyco-PAINT plugin in Fiji cannot be removed:$\r$\n$FijiDir\plugins\$1$\r$\n$\r$\nFiji is probably open. Close Fiji and click Retry, or click Cancel to skip the Fiji plugin." /SD IDCANCEL IDRETRY fiji_remove_old
        DetailPrint "Fiji plugin not installed: $FijiDir\plugins\$1 could not be removed (Fiji open?)"
        Return
    ${EndIf}
    SetOutPath "$FijiDir\plugins"
    File "${PLUGIN_JAR}"
    ; the old plugin is gone, so the only match is the one just installed
    FindFirst $0 $1 "$FijiDir\plugins\paint-fiji-plugin*.jar"
    FindClose $0
    StrCpy $PluginFile "$FijiDir\plugins\$1"
    WriteRegStr HKCU "Software\${APP_NAME}" "FijiDir" "$FijiDir"
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

; Last: what was installed where, for the finish page (and the details list)
Section "-Summary" SecSummary
    ${If} $Apps == ""
        StrCpy $Summary "Apps: none selected."
    ${Else}
        StrCpy $Summary "Apps, in $INSTDIR:$Apps"
    ${EndIf}
    ${If} ${SectionIsSelected} ${SecFiji}
        ${If} $PluginFile != ""
            StrCpy $Summary "$Summary$\r$\n$\r$\nFiji plugin:$\r$\n      $PluginFile"
        ${Else}
            StrCpy $Summary "$Summary$\r$\n$\r$\nFiji plugin: NOT installed, the older plugin in $FijiDir\plugins could not be removed. Close Fiji and run this installer again."
        ${EndIf}
    ${Else}
        StrCpy $Summary "$Summary$\r$\n$\r$\nFiji plugin: not selected."
    ${EndIf}
    DetailPrint "Summary:"
    DetailPrint "$Summary"
SectionEnd

!insertmacro MUI_FUNCTION_DESCRIPTION_BEGIN
    !insertmacro MUI_DESCRIPTION_TEXT ${SecLauncher}         "The Glyco-PAINT Launcher, from which all apps can be started."
    !insertmacro MUI_DESCRIPTION_TEXT ${SecViewer}           "The Viewer as a separate app."
    !insertmacro MUI_DESCRIPTION_TEXT ${SecGenerateSquares}  "Generate Squares as a separate app."
    !insertmacro MUI_DESCRIPTION_TEXT ${SecCreateExperiment} "Create Experiment as a separate app."
    !insertmacro MUI_DESCRIPTION_TEXT ${SecGetOmero}         "Get Omero as a separate app."
    !insertmacro MUI_DESCRIPTION_TEXT ${SecFiji}             "The Glyco-PAINT plugin for Fiji (TrackMate), copied into Fiji's plugins folder."
    !insertmacro MUI_DESCRIPTION_TEXT ${SecDesktop}          "Desktop shortcuts for the installed apps."
!insertmacro MUI_FUNCTION_DESCRIPTION_END

; ------------------------------------------------------------------------------------------
; Finding Fiji
; ------------------------------------------------------------------------------------------

; A Fiji folder is recognised by its contents, not its name: it has both a plugins and a jars
; folder. Fiji Latest is a zip the user unpacks anywhere (e.g. Downloads\fiji-latest-win64-jdk\Fiji,
; Desktop\Fiji, C:\Fiji), so likely places are searched up to two folder levels deep.

; Sets $FijiDir to $R9 if that is a Fiji folder and none was found yet
Function CheckFijiDir
    ${If} $FijiDir == ""
    ${AndIf} ${FileExists} "$R9\plugins\*.*"
    ${AndIf} ${FileExists} "$R9\jars\*.*"
        StrCpy $FijiDir $R9
    ${EndIf}
FunctionEnd

; Looks for Fiji in the folder $R0, its subfolders and their subfolders
Function SearchFijiIn
    ${If} $FijiDir != ""
    ${OrIfNot} ${FileExists} "$R0\*.*"
        Return
    ${EndIf}
    StrCpy $R9 $R0
    Call CheckFijiDir

    FindFirst $R1 $R2 "$R0\*.*"
    ${DoWhile} $R2 != ""
        ${If} $FijiDir != ""
            ${ExitDo}
        ${EndIf}
        ${If} $R2 != "."
        ${AndIf} $R2 != ".."
        ${AndIf} ${FileExists} "$R0\$R2\*.*"
            StrCpy $R9 "$R0\$R2"
            Call CheckFijiDir

            FindFirst $R3 $R4 "$R0\$R2\*.*"
            ${DoWhile} $R4 != ""
                ${If} $FijiDir != ""
                    ${ExitDo}
                ${EndIf}
                ${If} $R4 != "."
                ${AndIf} $R4 != ".."
                    StrCpy $R9 "$R0\$R2\$R4"
                    Call CheckFijiDir
                ${EndIf}
                FindNext $R3 $R4
            ${Loop}
            FindClose $R3
        ${EndIf}
        FindNext $R1 $R2
    ${Loop}
    FindClose $R1
FunctionEnd

!macro SEARCH_FIJI_IN BASE
    StrCpy $R0 "${BASE}"
    Call SearchFijiIn
!macroend

; ------------------------------------------------------------------------------------------
; Nothing may be running that the installer replaces
; ------------------------------------------------------------------------------------------

; Running Glyco-PAINT apps or Fiji (Fiji Latest's launcher, or the older ImageJ launcher) would keep
; their files locked, so an old plugin or app could stay behind next to the new one. Checked at the
; start and again just before installing; the user closes them and retries, or the installer quits.
; tasklist prints the image name only for a match (its "no tasks" message is localised, the name is not).
!macro IF_RUNNING EXE
    nsExec::ExecToStack 'tasklist /NH /FI "IMAGENAME eq ${EXE}"'
    Pop $0
    Pop $1
    ${StrStr} $2 $1 "${EXE}"
    ${If} $2 != ""
        StrCpy $R5 "$R5$\r$\n      ${EXE}"
    ${EndIf}
!macroend

Function CheckNothingRunning
  check_again:
    StrCpy $R5 ""
    !insertmacro IF_RUNNING "Glyco-PAINT.exe"
    !insertmacro IF_RUNNING "Viewer.exe"
    !insertmacro IF_RUNNING "Generate Squares.exe"
    !insertmacro IF_RUNNING "Create Experiment.exe"
    !insertmacro IF_RUNNING "Get Omero.exe"
    !insertmacro IF_RUNNING "fiji-windows-x64.exe"
    !insertmacro IF_RUNNING "fiji-windows-arm64.exe"
    !insertmacro IF_RUNNING "ImageJ-win64.exe"
    ${If} $R5 != ""
        MessageBox MB_RETRYCANCEL|MB_ICONEXCLAMATION "These programs are running and must be closed before Glyco-PAINT can be installed:$\r$\n$R5$\r$\n$\r$\nClose them and click Retry, or click Cancel to stop the installation." /SD IDCANCEL IDRETRY check_again
        Quit
    ${EndIf}
FunctionEnd

; The Fiji folder of a previous installation, otherwise the first Fiji found in the likely places
Function .onInit
    Call CheckNothingRunning
    ReadRegStr $FijiDir HKCU "Software\${APP_NAME}" "FijiDir"
    ${IfNot} ${FileExists} "$FijiDir\plugins\*.*"
        StrCpy $FijiDir ""
    ${EndIf}
    !insertmacro SEARCH_FIJI_IN "$PROFILE\Downloads"
    !insertmacro SEARCH_FIJI_IN "$DESKTOP"
    !insertmacro SEARCH_FIJI_IN "$DOCUMENTS"
    !insertmacro SEARCH_FIJI_IN "$PROFILE"
    !insertmacro SEARCH_FIJI_IN "$LOCALAPPDATA\Programs"
    !insertmacro SEARCH_FIJI_IN "$LOCALAPPDATA"
    !insertmacro SEARCH_FIJI_IN "$PROGRAMFILES64"
    !insertmacro SEARCH_FIJI_IN "$PROGRAMFILES32"
    ; the root of the system drive (C:)
    StrCpy $0 $WINDIR 3
    !insertmacro SEARCH_FIJI_IN "$0"
FunctionEnd

; Show the Fiji page only when the plugin is to be installed, with a warning if no Fiji was found
Function FijiPagePre
    ${IfNot} ${SectionIsSelected} ${SecFiji}
        Abort
    ${EndIf}
    ${If} $FijiDir == ""
        MessageBox MB_OK|MB_ICONEXCLAMATION "No Fiji installation was found (a folder with a plugins and a jars folder, in Downloads, the Desktop, Documents, your user folder, Program Files or the C: drive). Fiji may not be installed.$\r$\n$\r$\nIf Fiji is installed elsewhere, choose its folder on the next page. Otherwise go back, untick 'Fiji plugin', and install Fiji first (https://fiji.sc)."
    ${EndIf}
FunctionEnd

; The chosen folder must be a Fiji folder that this installer (no administrator rights) can write to
Function FijiPageLeave
    ${IfNot} ${FileExists} "$FijiDir\plugins\*.*"
        MessageBox MB_OK|MB_ICONEXCLAMATION "This is not a Fiji folder: it has no plugins folder.$\r$\n$\r$\n$FijiDir"
        Abort
    ${EndIf}
    ClearErrors
    FileOpen $0 "$FijiDir\plugins\glyco-paint-write-test.tmp" w
    ${If} ${Errors}
        MessageBox MB_OK|MB_ICONEXCLAMATION "The installer cannot write to the plugins folder of this Fiji (it may need administrator rights, e.g. under Program Files).$\r$\n$\r$\nChoose another Fiji, or untick 'Fiji plugin' and copy the plugin by hand."
        Abort
    ${EndIf}
    FileClose $0
    Delete "$FijiDir\plugins\glyco-paint-write-test.tmp"
FunctionEnd

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

    ; The Fiji plugin, if this installer put one there
    ReadRegStr $0 HKCU "Software\${APP_NAME}" "FijiDir"
    ${If} $0 != ""
        Delete "$0\plugins\paint-fiji-plugin*.jar"
    ${EndIf}

    DeleteRegKey HKCU "${UNINST_KEY}"
    DeleteRegKey HKCU "Software\${APP_NAME}"
SectionEnd
