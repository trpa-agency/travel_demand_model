Macro "_OpenTable" (file,view_name)
    ext = Lower(Right(file,3))
    if ext = "csv" then do
        type = "CSV"
    end
    else if ext = "bin" then do
        type = "FFB"
    end
    else do
        ShowMessage("Cannot open table of type " + ext)
        ShowMessage(2)
    end
    return(OpenTable(view_name,type,{file,}))
EndMacro
