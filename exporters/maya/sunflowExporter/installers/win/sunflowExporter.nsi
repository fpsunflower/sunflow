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
Page instfiles
UninstPage uninstConfirm
UninstPage instfiles


; Sections

Section ""	
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

Section "Uninstall"
	ClearErrors
	MessageBox MB_YESNO "Uninstall Sunflow Exporter?" IDNO end
	Delete $PROGRAMFILES\Sunflow\mayaExporter\uninst.exe
	RMDir /r "$PROGRAMFILES\Sunflow\mayaExporter"	
	end:
SectionEnd


