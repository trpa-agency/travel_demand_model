Macro "GetPropertiesFile" (model_dir,year,scenario_name,season)
    template = RunMacro("FormPath",{model_dir,"reference","code","emfac","emfac_base_template.properties"})
    output = RunMacro("FormPath",{model_dir,"scenarios",scenario_name,"code","emfac.properties"})
    CopyFile(template,output)
    model_dir = Substitute(model_dir,"\\","/",)
    if Right(model_dir,1) = "/" then do
        model_dir = Substring(model_dir,1,Len(model_dir)-1)
    end
    RunMacro("BuildPropertiesFile",output,"@@model_dir@@",model_dir)
    RunMacro("BuildPropertiesFile",output,"@@year@@",year)
    RunMacro("BuildPropertiesFile",output,"@@scenario_name@@",scenario_name)
    RunMacro("BuildPropertiesFile",output,"@@season@@",season)
    return(output)
EndMacro

Macro "BuildPropertiesFile" (file,key,value)
    f = OpenFile(file,"r")
    lines = null
    while not FileAtEOF(f) do
        line = ReadLine(f)
        line = Substitute(line,key,value,)
        lines = lines + {line}
    end
    CloseFile(f)
    f = OpenFile(file,"w")
    for i = 1 to lines.length do
        WriteLine(f,lines[i])
    end
    CloseFile(f)
EndMacro

Macro "RunTahoeAQuaVis" (model_dir,year,scenario_name,season)
    RunMacro("TCB Init")
    RunMacro("CloseAll")
    
    properties_file = RunMacro("GetPropertiesFile",model_dir,year,scenario_name,season)
    properties = RunMacro("ReadPropertiesFile",properties_file)
    {scenario_dir,season,emfac_output_dir} = RunMacro("GetPropertyData",properties)
    if GetFileInfo(properties.("emfac.2011.installation.dir")) = null then do
        error_message = "Cannot find EMFAC2011 program directory (" + properties.("emfac.2011.installation.dir") + ").\n"
        error_message = error_message + "Please install EMFAC2011 program, or change \"emfac.2011.installation.dir\" in\n" 
        error_message = error_message + RunMacro("FormPath",{model_dir,"reference","code","emfac","emfac_base_template.properties"})
        ShowMessage(error_message)
        return()
    end
    RunMacro("SetupAQuaVis",scenario_dir,season,emfac_output_dir)
    RunMacro("ExportTahoeNetwork",scenario_dir,season,emfac_output_dir,True)
    RunMacro("ExportTahoeIntrazonal",scenario_dir,season,emfac_output_dir)
    RunMacro("ExportTahoeTrips",scenario_dir,season,emfac_output_dir)
    
    RunMacro("CallJavaEmfac",properties,properties_file,scenario_dir,season,emfac_output_dir)
    ShowMessage("Finished with EMFAC run")
EndMacro

Macro "GetPropertyData" (properties)
    return({properties.("@scenario_dir@"),properties.("@season@"),properties.("@emfac_output_dir@")})    
EndMacro

Macro "GetModelOutputsDir" (scenario_dir,season)
    return(RunMacro("FormPath",{scenario_dir,"outputs_" + season}))
EndMacro

Macro "GetOutputsDir" (scenario_dir,season,emfac_output_dir)
    return(RunMacro("FormPath",{RunMacro("GetModelOutputsDir",scenario_dir,season),emfac_output_dir}))
EndMacro

Macro "SetupAQuaVis" (scenario_dir,season,emfac_output_dir)
    outputs_dir = RunMacro("GetOutputsDir",scenario_dir,season,emfac_output_dir)
    if GetFileInfo(outputs_dir) <> null then do
        RunMacro("ClearAndDeleteDirectory",outputs_dir)
    end
    CreateDirectory(outputs_dir)
EndMacro

Macro "GetHighestMsa" (model_outputs_dir)
    info = GetDirectoryInfo(RunMacro("FormPath",{model_outputs_dir,"MSA_iter*.bin"}),"File")
    max_iter = -1
    for i = 1 to info.length do
        f = info[i][1]
        max_iter = r2i(max(s2i(Substitute(Substitute(f,"MSA_iter","",),".bin","",)),max_iter))
    end
    return(RunMacro("FormPath",{model_outputs_dir,"MSA_iter" + i2s(max_iter) + ".bin"}))
EndMacro

Macro "ExportTahoeNetwork" (scenario_dir,season,emfac_output_dir,use_msa)
    model_outputs_dir = RunMacro("GetModelOutputsDir",scenario_dir,season)
    outputs_dir = RunMacro("GetOutputsDir",scenario_dir,season,emfac_output_dir)
    network_file = RunMacro("FormPath",{scenario_dir,"gis","Layers","Streets","FullStreets.dbd"})

    network_options = RunMacro("GetNewNetworkOptions","ID",RunMacro("GetFieldOptions","ID",),RunMacro("GetFieldOptions","Length",),RunMacro("GetFieldOptions","AB_FC","BA_FC"))
    RunMacro("AddRegionDataToNetworkOptions",network_options,"Region",)
    if use_msa then do
        join_file = RunMacro("GetHighestMsa",model_outputs_dir)
        join_info = RunMacro("GetJoinInfoOptions",join_file,"ID","ID")
        query = "SELECT * WHERE [" + RunMacro("GetLinkFileViewName") + "].AB_FC <> 7"
    end
    else do
        join_info = null
        query = "SELECT * WHERE AB_FC <> 7"
    end
    jv = "[" + RunMacro("GetJoinFileViewName") + "]."
    periods = {"AM","MD","PM","LN"}
    for p = 1 to periods.length do
        RunMacro("AddTimeDataToNetworkOptions",network_options,Lower(periods[p]),"auto",RunMacro("GetFieldOptions",jv + "AB_" + periods[p] + "_IVTT",jv + "BA_" + periods[p] + "_IVTT"),RunMacro("GetFieldOptions",jv + "AB_" + periods[p] + "_Flow",jv + "BA_" + periods[p] + "_Flow"),query,join_info)
    end
    RunMacro("ExportNetworkToCsv",network_file,network_options,RunMacro("FormPath",{outputs_dir,"AQuaVisNet.csv"}))
    //not needed anymore
    //RunMacro("ExportNetworkIdToLengthCsv",network_file,network_options,RunMacro("FormPath",{outputs_dir,"AQuaVisNetIdLength.csv"}))
EndMacro

Macro "ExportTahoeIntrazonal" (scenario_dir,season,emfac_output_dir)
    outputs_dir = RunMacro("GetOutputsDir",scenario_dir,season,emfac_output_dir)
    taz_file = RunMacro("FormPath",{scenario_dir,"gis","Layers","TAZ","TAZ.dbd"})
    network_file = RunMacro("FormPath",{scenario_dir,"gis","Layers","Streets","FullStreets.dbd"})
    
    calcs = {{"1","45.0"},{"2","35.0"},{"3","15.0"}}
    speed_calcs = null
    for i = 1 to calcs.length do
        speed_calcs = RunMacro("GetSpeedCalcs",speed_calcs,"SELECT * WHERE AreaType = " + calcs[i][1],calcs[i][2])
    end
    
    distance_file = RunMacro("FormPath",{scenario_dir,"gis","Skims",season + "MiddayDriveDistanceSkim.csv"})
    distance_opts = RunMacro("GetDistanceOpts",distance_file,"Length (Skim)","TAZ:1","TAZ")
    RunMacro("ExportIntrazonalToCsv",taz_file,"TAZ",speed_calcs,distance_opts,RunMacro("GetTazMappingOpts",network_file,"TAZ",0,"Region",),RunMacro("GetDefaultOpts","-1","45.0"),RunMacro("FormPath",{outputs_dir,"AQuaVisIntrazonal.csv"}))
EndMacro

//Macro "ExportTahoeTrips" (scenario_dir,season,emfac_output_dir)
//    index_name = "TAZs"
//    outputs_dir = RunMacro("GetOutputsDir",scenario_dir,season,emfac_output_dir)
//    taz_file = RunMacro("FormPath",{scenario_dir,"gis","Layers","TAZ","TAZ.dbd"})
//    trip_opts = null
//    periods = {"AM","MD","PM","LN"}
//    for p = 1 to periods.length do
//        period = periods[p]
//        file = RunMacro("FormPath",{scenario_dir,"gis","Skims","Data_Files","TripTables","Trips_" + period + ".mtx"})
//        trip_matrix_file = RunMacro("FormPath",{scenario_dir,"gis","Skims","Data_Files","TripTables","Trips_" + period + ".mtx"})
//        trip_opts = RunMacro("GetTripOpts",trip_opts,file,Lower(period),"auto","i","j",{"DA","SA"})
//    end
//    RunMacro("ExportTripsToCsv",trip_opts,RunMacro("FormPath",{outputs_dir,"AQuaVisTrips.csv"}))
//EndMacro

Macro "ExportTahoeTrips" (scenario_dir,season,emfac_output_dir)
    skim_map = null
    skim_map.("1") = "am"
    skim_map.("2") = "pm"
    skim_map.("3") = "md"
    skim_map.("4") = "ln"
    period_map = null
    period_map.("0") = "ln"
    period_map.("1") = "ln"
    period_map.("2") = "ln"
    period_map.("3") = "ln"
    period_map.("4") = "ln"
    period_map.("5") = "ln"
    period_map.("6") = "ln"
    period_map.("7") = "ln"
    period_map.("8") = "am"
    period_map.("9") = "am"
    period_map.("10") = "md"
    period_map.("11") = "md"
    period_map.("12") = "md"
    period_map.("13") = "md"
    period_map.("14") = "md"
    period_map.("15") = "md"
    period_map.("16") = "md"
    period_map.("17") = "pm"
    period_map.("18") = "pm"
    period_map.("19") = "ln"
    period_map.("20") = "ln"
    period_map.("21") = "ln"
    period_map.("22") = "ln"
    period_map.("23") = "ln"
    mode_map = {{"shared auto","auto"},{"drive alone","auto"}}
    tmp_mat = RunMacro("FormPath",{RunMacro("GetOutputsDir",scenario_dir,season,emfac_output_dir),"temp.mtx"})
    RunMacro("ExportTripsToCsvFromTripFile",
        RunMacro("FormPath",{RunMacro("GetModelOutputsDir",scenario_dir,season),"trip_file.csv"}),
        RunMacro("FormPath",{RunMacro("GetOutputsDir",scenario_dir,season,emfac_output_dir),"AQuaVisIntrazonal.csv"}),
        RunMacro("FormPath",{RunMacro("GetOutputsDir",scenario_dir,season,emfac_output_dir),"AQuaVisTrips.csv"}),
        tmp_mat,
        mode_map,
        period_map,skim_map,{"startTaz","endTaz","mode","time","skim"}) 
    DeleteFile(tmp_mat)
EndMacro

Macro "CallJavaEmfac" (properties,properties_file,scenario_dir,season,emfac_output_dir)
    outputs_dir = RunMacro("GetOutputsDir",scenario_dir,season,emfac_output_dir)
    java = "\"" + properties.("java.executable") + "\""
    classpath = "\"" + RunMacro("FormClasspath",properties.("java.emfac.classpath.dir")) + "\""
    program = properties.("java.emfac.class")
    args = "\"" + properties_file + "\""
    output_file = RunMacro("FormPath",{outputs_dir,"emfac_out.txt"})
    call = "cmd /c \"" + java + " -Xmx1000m  -XX:+UseConcMarkSweepGC -cp " + classpath + " " + program + " " + args + " > \"" + output_file + "\" 2>&1\""
    RunProgram(call,)
EndMacro

Macro "FormClasspath" (jar_dir)
    cp = ""
    info = GetDirectoryInfo(RunMacro("FormPath",{jar_dir,"*.jar"}), "File")
    for i = 1 to info.length do
        if Len(cp) > 0 then do
            cp = cp + ";"
        end
        cp = cp + RunMacro("FormPath",{jar_dir,info[i][1]})
    end
    return(cp)
EndMacro
