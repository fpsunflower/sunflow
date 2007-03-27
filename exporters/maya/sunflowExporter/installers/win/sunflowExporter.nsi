; sunflowExporter installer

!include "MUI.nsh"

Name "SUNFLOW FOR MAYA"
OutFile "sunflowExporter.exe"

XPStyle on

AddBrandingImage left 100

LicenseText "License page"
InstallDir "$PROGRAMFILES\Sunflow\mayaExporter"

; Pages

!insertmacro MUI_PAGE_LICENSE "..\..\..\..\..\LICENSE"
!insertmacro MUI_PAGE_COMPONENTS
Page instfiles
UninstPage uninstConfirm
UninstPage instfiles


; Sections

Section "Maya 8.5"	M85Sec
    CreateDirectory "$PROGRAMFILES\Sunflow\mayaExporter"
	SetOutPath "$PROGRAMFILES\Sunflow\mayaExporter\mel"
	CreateDirectory "$PROGRAMFILES\Sunflow\mayaExporter\mel"
	File "..\..\mel\*.mel"
	SetOutPath "$PROGRAMFILES\Sunflow\mayaExporter\bin"
	CreateDirectory "$PROGRAMFILES\Sunflow\mayaExporter\bin"
	File "..\..\bin\sunflowExport.mll"
	SetOutPath "$PROGRAMFILES\Sunflow\mayaExporter\icons"
	CreateDirectory "$PROGRAMFILES\Sunflow\mayaExporter\icons"
	File "..\..\icons\*.*"
	WriteUninstaller $PROGRAMFILES\Sunflow\mayaExporter\uninst.exe	
SectionEnd

;--------------------------------
;Descriptions

  ;Language strings
  LangString DESC_M85Sec ${LANG_ENGLISH} "Install sunflow exporter for Maya 8.5."

  ;Assign language strings to sections
  !insertmacro MUI_FUNCTION_DESCRIPTION_BEGIN
    !insertmacro MUI_DESCRIPTION_TEXT ${M85Sec} $(DESC_M85Sec)
  !insertmacro MUI_FUNCTION_DESCRIPTION_END
  
Section "Uninstall"
	ClearErrors
	MessageBox MB_YESNO "Uninstall Sunflow Exporter?" IDNO end
	Delete $PROGRAMFILES\Sunflow\mayaExporter\uninst.exe
	RMDir /r "$PROGRAMFILES\Sunflow\mayaExporter"	
	end:
SectionEnd


