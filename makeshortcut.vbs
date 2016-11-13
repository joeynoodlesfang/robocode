WScript.Echo("Creating shortcuts...")
Set Shell = CreateObject ("WScript.Shell")
Set fso = CreateObject("Scripting.FileSystemObject")
ProgramsPath = Shell.SpecialFolders("Programs")
if (not(fso.folderExists(ProgramsPath + "\\Robocode"))) Then
	fso.CreateFolder(ProgramsPath + "\\Robocode")
End If
Set link = Shell.CreateShortcut(ProgramsPath + "\\Robocode\\Robocode.lnk")
link.Arguments = ""
link.Description = "Robocode"
link.HotKey = ""
link.IconLocation = "C:\\robocode\\robocode.ico,0"
link.TargetPath = "C:\\robocode\\robocode.bat"
link.WindowStyle = 1
link.WorkingDirectory = "C:\\robocode"
link.Save()
DesktopPath = Shell.SpecialFolders("Desktop")
Set link = Shell.CreateShortcut(DesktopPath + "\\Robocode.lnk")
link.Arguments = ""
link.Description = "Robocode"
link.HotKey = ""
link.IconLocation = "C:\\robocode\\robocode.ico,0"
link.TargetPath = "C:\\robocode\\robocode.bat"
link.WindowStyle = 1
link.WorkingDirectory = "C:\\robocode"
link.Save()
WScript.Echo("Shortcuts created.")
