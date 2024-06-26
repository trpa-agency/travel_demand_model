set objWSHShell = WScript.CreateObject("WScript.Shell" )
' strDesktop = WshShell.SpecialFolders("AllUsersDesktop" )

' arguments are output dir, shortcut name, shortcut target, shortcut description, shortcut working directory, shortcut icon path
argCount = WScript.Arguments.Count
outDir = objWSHShell.ExpandEnvironmentStrings(WScript.Arguments.Item(0))
name = objWSHShell.ExpandEnvironmentStrings(WScript.Arguments.Item(1))
target = objWSHShell.ExpandEnvironmentStrings(WScript.Arguments.Item(2))
args = objWSHShell.ExpandEnvironmentStrings(WScript.Arguments.Item(3))
desc = objWSHShell.ExpandEnvironmentStrings(WScript.Arguments.Item(4))
wd = objWSHShell.ExpandEnvironmentStrings(WScript.Arguments.Item(5))
If argCount > 7 Then icon = objWSHShell.ExpandEnvironmentStrings(WScript.Arguments.Item(6))



' set oShellLink = WshShell.CreateShortcut(strDesktop & "\" & name & ".lnk" )
set oShellLink = objWSHShell.CreateShortcut(outDir & "\" & name & ".lnk" )
oShellLink.TargetPath = Replace(target,"'", chr(34))
oShellLink.Arguments = Replace(args,"'", chr(34))
oShellLink.WindowStyle = 1
oShellLink.Description = desc
oShellLink.WorkingDirectory = wd
If argCount > 7 Then oShellLink.IconLocation = icon
oShellLink.Save