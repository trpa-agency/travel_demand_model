Macro "TahoeABModel"
    RunDbox("TahoeDbox")
EndMacro

Dbox "TahoeDbox" right, bottom , 50 , 26 title: "Tahoe Activity-Based Travel Demand Model" Toolbox NoKeyboard
	init do
		RunMacro("TCB Init")
		shared stateArray
		stateArray = RunMacro("LoadState")
                path = stateArray[1]
                basePath = stateArray[3]
                referencePath = basePath + "reference\\"
		javaPath = referencePath + "code\\"
		pathArray = RunMacro("updatePath",stateArray[3],stateArray[1])
		season_idx = stateArray[2]
		shared d_matrix_options // default options used for creating matrix editors
		shared cr_user_iters
		shared model_iters
		cr_user_iters = stateArray[4]
		model_iters = stateArray[5]
		res_array = {"Synthetic Resident Population","Auto Ownership","Daily Activity Pattern",
			     "Mandatory DTM","Joint Tour Generation","Joint Tour DTM","Non-Mandatory Tour Generation",
			     "Non-Mandatory Tour DTM","At-Work DTM","Mandatory Stops","Joint Stops",
			     "Non-Mandatory Stops","At-Work Stops"}
		res_id = 0
		ext_array = {"Synthetic External Worker Population","External Worker OT"}
		ext_id = 0
		vis_array = {"Synthetic Overnight Visitor Population","Synthetic Day Visitor Population",
		             "Visitor DTM","Visitor Stops","Thru-Visitors"}
		vis_id = 0
		
		scenario_file = OpenFile(javaPath + "scenario_list.txt", "r")
                scenario_list = readarray(scenario_file)
                CloseFile(scenario_file)
		scenario_idx = stateArray[6]
		vm_size = stateArray[7]
		
		RunMacro("updateproperty", referencePath,path,season_idx)
	    time_period = 1
	    asgn_type = 1
		enditem

	close do
        RunMacro("CloseActions",path,season_idx,cr_user_iters,model_iters,scenario_list,scenario_idx,pathArray,vm_size)
	    RunMacro("close everything")
            return()
            enditem

//Logo: User should see the TRPA logo	
	Button "icon_logo" 1, 1 icon: pathArray[13] + "reference\\img\\logoCrop.bmp" do
		ShowMessage("Travel Model Sponsored by TRPA.  For more info see www.trpa.org")
		enditem


//Scenario chooser: user will choose scenario.
    //Popdown Menu "Scenario Chooser" 15, 6, 35 prompt: "Choose Scenario" List: scenario_list variable: scenario_idx do
    Text "Select Scenario" .7, 5.3
    Popdown Menu "Scenario Chooser" 1, 6.7, 35 List: scenario_list variable: scenario_idx do
        path = basePath + "scenarios\\" + scenario_list[scenario_idx] + "\\"
        pathArray = RunMacro("updatePath",basePath,path)
        RunMacro("updateproperty",referencePath,path,season_idx)
    endItem
    
//Create new scenario: User can create a new scenario
    //Button "New Scenario Button" 52.2, 5.5, 14, 2 prompt: "New Scenario" do
    Button "New Scenario Button" 37.5, 6.5, 12, 1.3 prompt: "New Scenario" do
        newScenarioList = RunDbox("NewScenarioCreator",pathArray,scenario_list)
        if newScenarioList.length > scenario_list.length then do
            scenario_list = newScenarioList
            scenario_idx = scenario_list.length
            path = basePath + "scenarios\\" + scenario_list[scenario_idx] + "\\"
            pathArray = RunMacro("updatePath",basePath,path)
            RunMacro("updateproperty",referencePath,path,season_idx)
        end
    endItem

//Summer/Winter: User will select the season to which the model is applied.
	//Radio List .6, 8.5, 49.5, 2.7 Prompt:"Which season the model is applied to?"
	Radio List .6, 8.2, 49.5, 2.7 Prompt:"Season"
		Variable: season_idx
	Radio Button 8, 9.5 Prompt:"Summer" do
		value = RunMacro("updateproperty", referencePath,path,season_idx)
	endItem
	Radio Button 31, 9.5 Prompt:"Winter" do
		value = RunMacro("updateproperty", referencePath,path,season_idx)
	endItem


//Open map
    Button "       Open Scenario Map" 1,12,22,2 do
        SetItem("Open Map")
        OpenMap(pathArray[7] + "Tahoe.map",)
    endItem
    Button "Open Map" 1.5, 12.3 icon: pathArray[13] + "reference\\img\\MAP.bmp" 

//Open trip tables
    Button "       Open Trip Tables" 1,15,22,2 do
        if (RunMacro("TestIfRun",pathArray,scenario_list[scenario_idx],season_idx) = 0) then do
            SetItem("Open Trip Tables")
            //editorName = CreateMatrixEditor("Trips_AM",OpenMatrix(pathArray[11] + "Trips_PM.mtx",False),d_matrix_options)
            b = d_matrix_options
            for i = 1 to b.length do
                for j = 1 to b[i].length do
                    if b[i][j] = "Read Only" then do
                        b[i][j+1] = "True"
                    end
                end
            end
            timePeriodGroup = {"AM","MD","PM","LN"}
            CopyFile(pathArray[11] + "Trips_" + timePeriodGroup[time_period] + ".mtx",pathArray[11] + "Temp_Trips.mtx")
            m = OpenMatrix(pathArray[11] + "Temp_Trips.mtx",)
            periodList = {"AM Peak","Midday","PM Peak","Overnight"}
            RenameMatrix(m,periodList[time_period] + " Trips")
            RunMacro("RenameMatrixCores",pathArray[11] + "Temp_Trips.mtx")
            editorName = CreateMatrixEditor(periodList[time_period] + " Trips",m,b)
        end
    endItem
    Button "Open Trip Tables" 1.5, 15.3 icon: pathArray[13] + "reference\\img\\MTX.bmp" 

//Open trip table time period selector
    Popdown Menu "Time Period Chooser" 11.5, 17.7, 11 Prompt: "Trip Period" List: {"AM Peak","Midday","PM Peak","Overnight"} variable: time_period

//Create model summaries
    Button "       Model Summary" 26,12,22,2 do
        SetItem("Model Summary")
        if (RunMacro("TestIfRun",pathArray,scenario_list[scenario_idx],season_idx) = 0) then do
            MessageBox("TODO",)
        end
    endItem
    Button "Model Summary" 26.5, 12.3 icon: pathArray[13] + "reference\\img\\DVW.bmp" 

//Create assignment summaries
    Button "       Assignment Summary" 26,15,22,2 do
        SetItem("Assignment Summary")
        if (RunMacro("TestIfRun",pathArray,scenario_list[scenario_idx],season_idx) = 0) then do
            //MessageBox("TODO",)
            RunMacro("GenerateAssignmentSummary",pathArray,season_idx,scenario_list[scenario_idx],asgn_type)
        end
    endItem
    Button "Assignment Summary" 26.5, 15.3 icon: pathArray[13] + "reference\\img\\FIG.bmp" 

//Assignment summary selector
    Popdown Menu "Assignment Chooser" 37.5, 17.7, 11 Prompt: "Link Class" List: {"Total","Principle Arterial","Minor Arterial","Collector","Centroid","All"} variable: asgn_type


//All: User presses this button to run all components
 	Button "Run Model" 1, 20, 25, 2.5 do
        RunMacro("PreModelRunner",pathArray,season_idx)
        RunMacro("SaveLinkData",pathArray,scenario_list[scenario_idx],season_idx,0)
        for i = 1 to model_iters do
            RunMacro("ClearLastFile",pathArray,scenario_list[scenario_idx],season_idx)
		    RunMacro("RunJavaModel",pathArray,vm_size)
		    RunMacro("WaitTimer",pathArray,scenario_list[scenario_idx],season_idx)
		    RunMacro("CreateTripMatrices",pathArray)
		    RunMacro("TrafficAssignment",pathArray,cr_user_iters)
		    RunMacro("RunTransitSkims",pathArray,season_idx)
		    RunMacro("SaveLinkData",pathArray,scenario_list[scenario_idx],season_idx,i)
		end
    enditem

Radio List 27, 19.5, 23, 3 Prompt:"Iterations"
Text 46, 20.5 Prompt: "Highway Assignment:"  Variable: IntToString(cr_user_iters) 
Text 43, 21.5 Prompt: "Model Feedback:" Variable: IntToString(model_iters)
//Button "" 49.3, 22.4, .01, .01 do
Button "" 50, 25.5, .01, .01 do
    MessageBox("Hi",)
endItem

//Quit: User will press this button to exit
	Button "Quit" 15, 23.6, 20, 1.5 cancel do
	        RunMacro("CloseActions",path,season_idx,cr_user_iters,model_iters,scenario_list,scenario_idx,pathArray,vm_size)
		RunMacro("close everything")
		Return()
		Enditem

//This is only to leave some room after the "Quit" button.
	Text 64, 51.5,,1 prompt: "" 

EndDbox

Macro "WaitTimer" (pathArray,scenarioName,summer)
    while RunMacro("TestIfDone",pathArray,scenarioName,summer) = 1 do
        Sleep(3000)
    end
EndMacro

Macro "TestIfDone" (pathArray,scenarioName,summer)
    outputFile = pathArray[13] + "scenarios\\" + scenarioName + "\\outputs_winter\\TripSynthesize.last"
    if summer = 1 then do
        outputFile = pathArray[13] + "scenarios\\" + scenarioName + "\\outputs_summer\\TripSynthesize.last"
    end
    on notfound do
       on notfound default
       return(1)
    end
    f = OpenFile(outputFile,"r")
    CloseFile(f)
    on notfound default
    return(0)
EndMacro

Macro "TestIfRun" (pathArray,scenarioName,summer)
    on notfound do
        MessageBox("Model run for this scenario and season not completed!",)
        on notfound default
        return(1)
    end
    outputDirectory = pathArray[13] + "scenarios\\" + scenarioName + "\\outputs_winter\\"
    if summer = 1 then do
        outputDirectory = pathArray[13] + "scenarios\\" + scenarioName + "\\outputs_summer\\"
    end
    f = OpenFile(outputDirectory + "FullStreets_iter3.bin","r")
    CloseFile(f)
    return(0)
EndMacro

Macro "RenameMatrixCores" (matrixName)
    Opts = null
    Opts.Input.[Input Matrix] = matrixName
    Opts.Input.[Target Core] = "DA" 
    Opts.Input.[Core Name] = "Drive Alone" 
    RunMacro("TCB Run Operation", 1, "Rename Matrix Core", Opts) 
    Opts.Input.[Target Core] = "SA" 
    Opts.Input.[Core Name] = "Shared Auto" 
    RunMacro("TCB Run Operation", 1, "Rename Matrix Core", Opts) 
    Opts.Input.[Target Core] = "DT" 
    Opts.Input.[Core Name] = "Drive to Transit" 
    RunMacro("TCB Run Operation", 1, "Rename Matrix Core", Opts) 
    Opts.Input.[Target Core] = "WT" 
    Opts.Input.[Core Name] = "Walk To Transit" 
    RunMacro("TCB Run Operation", 1, "Rename Matrix Core", Opts) 
    Opts.Input.[Target Core] = "SB" 
    Opts.Input.[Core Name] = "School Bus" 
    RunMacro("TCB Run Operation", 1, "Rename Matrix Core", Opts) 
    Opts.Input.[Target Core] = "Sh" 
    Opts.Input.[Core Name] = "Visitor Shuttle" 
    RunMacro("TCB Run Operation", 1, "Rename Matrix Core", Opts) 
    Opts.Input.[Target Core] = "NM" 
    Opts.Input.[Core Name] = "Non-Motorized" 
    RunMacro("TCB Run Operation", 1, "Rename Matrix Core", Opts) 
endMacro

Macro "LoadState"
    on notfound do
        stateArray = {"C:\\",1,"C:\\Chris_Stuff\\Tahoe\\TahoeActivityBasedModel\\Java_Code\\",50,4,""}
        stateArray = RunDbox("configure",stateArray)
        on notfound default
        return(stateArray)
    end
    backupFile = OpenFile("TahoeModelRunnerBackup.txt","r")
    stateArray = ReadArray(backupFile)
    CloseFile(backupFile)
    stateArray[2] = StringToInt(stateArray[2])
    stateArray[4] = StringToInt(stateArray[4])
    stateArray[5] = StringToInt(stateArray[5])
    stateArray[6] = StringToInt(stateArray[6])
    return(stateArray)
EndMacro

Macro "CloseActions" (scenarioPath,season_idx,cr_user_iters,model_iters,scenario_list,scenario_idx,pathArray,vm_size)
    shared stateArray
    stateArray[1] = scenarioPath
    stateArray[2] = season_idx
    stateArray[4] = cr_user_iters
    stateArray[5] = model_iters
    stateArray[6] = scenario_idx
    stateArray[7] = vm_size
    backupFile = OpenFile("TahoeModelRunnerBackup.txt","w")
    WriteArray(backupFile,stateArray)
    CloseFile(backupFile)
    scenario_file = OpenFile(pathArray[8] + "scenario_list.txt", "w")
    writearray(scenario_file,scenario_list)
    CloseFile(scenario_file)
	on error do
        on error default
        return()
    end
    DeleteFile(pathArray[11] + "Temp_Trips.mtx")
EndMacro

Macro "updatePath" (basePath,scenarioPath)
    referencePath = basePath + "reference\\"
    javaPath = referencePath + "code\\"
    transitNetworkPath = scenarioPath + "gis\\Transit_Networks\\"
    streetLayerPath = scenarioPath + "gis\\Layers\\Streets\\"
    networkPath = scenarioPath + "gis\\Networks\\"
    transitRoutesPath = scenarioPath + "gis\\Transit_Route_System\\"
    modeTablePath = scenarioPath + "gis\\Skims\\Data_Files\\"
    outputPath = scenarioPath + "gis\\Skims\\"
    mapPath = scenarioPath + "gis\\Maps\\"
    externalDistanceMatrixPath = modeTablePath
    csvTripTablePath = scenarioPath + "gis\\Skims\\Data_Files\\TripTables\\"
    tripMatrixPath = scenarioPath + "gis\\Skims\\Data_Files\\TripTables\\"
    assignmentOutputPath = scenarioPath + "gis\\Skims\\Traffic_Assignment\\"
    pathArray = {streetLayerPath, transitNetworkPath, transitRoutesPath, modeTablePath, outputPath, networkPath, mapPath, javaPath, externalDistanceMatrixPath, csvTripTablePath, tripMatrixPath, assignmentOutputPath, basePath}
    return(pathArray)
EndMacro

Dbox "configure"(stateArray)
    init do
        opt_idx = 0
    enditem
    Text 1, 1 Variable: "To configure the model runner, choose the path to the TahoeModel directory"
    Text 1, 2 Variable: " (e.g. C:\\TRPA\\TahoeModel\\)"
    Button "Browse..." 18, 4 , 15, 1 do
        path = ChooseDirectory("Browse to and choose TahoeModel directory", )
        stateArray[3] = path + "\\"
        tcLogxmlPath = path + "\\reference\\"
        logxmlPath = RunMacro("GenerifyPath",tcLogxmlPath)
        RunMacro("TemplateToFile",tcLogxmlPath + "code\\" + "log4j_mine_noScenario.template.xml",tcLogxmlPath + "code\\" + "log4j_mine_noScenario.xml",logxmlPath,"empty",0)
        return(stateArray)
    EndItem
EndDbox

Dbox "submodel_opt"(submodeltype, submodel_idx,submodel_id,submodel_array) title: "Warning!!!"
	init do
		opt_idx = 0
		enditem

	Text 1, 1 Variable: "The record shows that you haven't run the steps from " + submodel_array[submodel_id+1]
	Text 1, 2 Variable: " in the list yet. Error might occur if you continue."
		
	Radio List 1, 4, 65, 4 Prompt:"Do you still wish to  run this step?"
		Variable: opt_idx
	Radio Button 15, 6 Prompt:"Continue" do
		return(opt_idx)
		Enditem
	Radio Button 42, 6 Prompt:"Cancel" do
		return(opt_idx)
		Enditem

//This is only to leave some room at the bottom.
	Text 65, 9,,0.1 prompt: "" 
	
EndDbox


Macro "close everything"
	maps = GetMaps()
	if maps <> null then do
		for i = 1 to maps[1].length do
			SetMapSaveFlag(maps[1][i],"False")
			end
		end
	RunMacro("G30 File Close All")
	mtxs = GetMatrices()
	if mtxs <> null then do
		handles = mtxs[1]
		for i = 1 to handles.length do
			handles[i] = null
		end
	end
EndMacro

//This just copies the desired xml/properties files to the reference/code/ directory
Macro "updateproperty"(referencePath,path,season_idx)
    on notfound do
        ShowMessage("Scenario path invalid! Model cannot be run until valid path is chosen.")
        on notfound default
        return()
    end
    codeDirectory = referencePath + "code\\"
    scenarioCodeDirectory = path + "code\\"
    season = "_winter"
    if season_idx = 1 then do
        season = "_summer"
    end
    CopyFile(scenarioCodeDirectory + "tahoe" + season + ".properties",codeDirectory + "tahoe.properties")
    CopyFile(scenarioCodeDirectory + "log4j_mine" + season + ".xml",codeDirectory + "log4j_mine.xml")
EndMacro


//***********************Sumaries from travel assigment*****************************
Macro "GenerateAssignmentSummary" (pathArray,season_idx,scenarioName,type)
    outputDirectory = pathArray[13] + "scenarios\\" + scenarioName + "\\outputs_winter\\"
    if season_idx = 1 then do
        outputDirectory = pathArray[13] + "scenarios\\" + scenarioName + "\\outputs_summer\\"
    end
    view_name = OpenTable("AssignmentResults", "FFB",  {outputDirectory + "FullStreets_iter3.bin",})
    CreateExpression("AssignmentResults","AB_AM_Time","AB_AM_IVTT * AB_AM_Flow",)
    CreateExpression("AssignmentResults","AB_MD_Time","AB_MD_IVTT * AB_MD_Flow",)
    CreateExpression("AssignmentResults","AB_PM_Time","AB_PM_IVTT * AB_PM_Flow",)
    CreateExpression("AssignmentResults","AB_LN_Time","AB_LN_IVTT * AB_LN_Flow",)
    CreateExpression("AssignmentResults","BA_AM_Time","BA_AM_IVTT * BA_AM_Flow",)
    CreateExpression("AssignmentResults","BA_MD_Time","BA_MD_IVTT * BA_MD_Flow",)
    CreateExpression("AssignmentResults","BA_PM_Time","BA_PM_IVTT * BA_PM_Flow",)
    CreateExpression("AssignmentResults","BA_LN_Time","BA_LN_IVTT * BA_LN_Flow",)
    rslt1 = AggregateTable("ABAssignmentSummary", "AssignmentResults|", "FFB", outputDirectory + "FullStreets_iter3_absummary.bin", "AB_FC", 
        {{"AB_AM_Time","SUM"},{"AB_MD_Time","SUM"},{"AB_PM_Time","SUM"},{"AB_LN_Time","SUM"},{"AB_AM_VMT","SUM"},{"AB_MD_VMT","SUM"},{"AB_PM_VMT","SUM"},{"AB_LN_VMT","SUM"}}, )
    rslt2 = AggregateTable("BAAssignmentSummary", "AssignmentResults|", "FFB", outputDirectory + "FullStreets_iter3_basummary.bin", "BA_FC", 
        {{"BA_AM_Time","SUM"},{"BA_MD_Time","SUM"},{"BA_PM_Time","SUM"},{"BA_LN_Time","SUM"},{"BA_AM_VMT","SUM"},{"BA_MD_VMT","SUM"},{"BA_PM_VMT","SUM"},{"BA_LN_VMT","SUM"}}, )
    
    fc1sum = RunMacro("GetSummaryArray",1)
    fc2sum = RunMacro("GetSummaryArray",2)
    fc3sum = RunMacro("GetSummaryArray",3)
    fc9sum = RunMacro("GetSummaryArray",9)
    CloseView(view_name)
    CloseView(rslt1)
    CloseView(rslt2)
    
    time = 13
    vmt = 25
    vht = 25
    message = "Assignment Summary\n"
    
    if type = 6 or type = 1 then do
        fctemp = fc1sum
        for i = 1 to fctemp[1][2].length do
            fctemp[1][2][i][2] = fc1sum[1][2][i][2] + fc2sum[1][2][i][2] + fc3sum[1][2][i][2] + fc9sum[1][2][i][2]
            fctemp[2][2][i][2] = fc1sum[2][2][i][2] + fc2sum[2][2][i][2] + fc3sum[2][2][i][2] + fc9sum[2][2][i][2]
        end
        message = message + RunMacro("AssignmentTable",fctemp,"Total",time,vmt,vht)
    end
    
    if type = 6 or type = 2 then do
        message = message + RunMacro("AssignmentTable",fc1sum,"Principal Arterial",time,vmt,vht)
    end
    
    if type = 6 or type = 3 then do
        message = message + RunMacro("AssignmentTable",fc2sum,"Minor Arterial",time,vmt,vht)
    end
    
    if type = 6 or type = 4 then do
        message = message + RunMacro("AssignmentTable",fc3sum,"Collector",time,vmt,vht)
    end
    
    if type = 6 or type = 5 then do
        message = message + RunMacro("AssignmentTable",fc9sum,"Centroid",time,vmt,vht)
    end
    
    f = OpenFile(outputDirectory + "temp.txt", "w")
    WriteLine(f,message)
    CloseFile(f)
    LaunchProgram(pathArray[8] + "\\TextViewer\\TextViewer " + outputDirectory + "temp.txt -f -t \"Assignment Results\"")
    RunProgram("cmd /c del " + outputDirectory + "FullStreets_iter3_absummary.*",)
    RunProgram("cmd /c del " + outputDirectory + "FullStreets_iter3_basummary.*",)
    DeleteFile(outputDirectory + "temp.txt")
EndMacro

Macro "GetSummaryArray" (fc) 
    rh = LocateRecord("ABAssignmentSummary|", "AB_FC", {fc}, )
    vals = GetRecordValues("ABAssignmentSummary", rh,)
    abamTime = RunMacro("GetFieldValue",vals,"ABAssignmentSummary.AB_AM_Time")
    abmdTime = RunMacro("GetFieldValue",vals,"ABAssignmentSummary.AB_MD_Time")
    abpmTime = RunMacro("GetFieldValue",vals,"ABAssignmentSummary.AB_PM_Time")
    ablnTime = RunMacro("GetFieldValue",vals,"ABAssignmentSummary.AB_LN_Time")
    abamVMT = RunMacro("GetFieldValue",vals,"ABAssignmentSummary.AB_AM_VMT")
    abmdVMT = RunMacro("GetFieldValue",vals,"ABAssignmentSummary.AB_MD_VMT")
    abpmVMT = RunMacro("GetFieldValue",vals,"ABAssignmentSummary.AB_PM_VMT")
    ablnVMT = RunMacro("GetFieldValue",vals,"ABAssignmentSummary.AB_LN_VMT")
    rh = LocateRecord("BAAssignmentSummary|", "BA_FC", {fc}, )
    vals = GetRecordValues("BAAssignmentSummary", rh,)
    baamTime = RunMacro("GetFieldValue",vals,"BAAssignmentSummary.BA_AM_Time")
    bamdTime = RunMacro("GetFieldValue",vals,"BAAssignmentSummary.BA_MD_Time")
    bapmTime = RunMacro("GetFieldValue",vals,"BAAssignmentSummary.BA_PM_Time")
    balnTime = RunMacro("GetFieldValue",vals,"BAAssignmentSummary.BA_LN_Time")
    baamVMT = RunMacro("GetFieldValue",vals,"BAAssignmentSummary.BA_AM_VMT")
    bamdVMT = RunMacro("GetFieldValue",vals,"BAAssignmentSummary.BA_MD_VMT")
    bapmVMT = RunMacro("GetFieldValue",vals,"BAAssignmentSummary.BA_PM_VMT")
    balnVMT = RunMacro("GetFieldValue",vals,"BAAssignmentSummary.BA_LN_VMT")
    fcsum = {{"Vehicle Miles Travelled",},{"Vehicle Hours Travelled",}}
    fcsum[1][2] = InsertArrayElements(fcsum[1][2],fcsum[1][2].length+1,{{"AM Peak",abamVMT + baamVMT}})
    fcsum[1][2] = InsertArrayElements(fcsum[1][2],fcsum[1][2].length+1,{{"Midday",abmdVMT + bamdVMT}})
    fcsum[1][2] = InsertArrayElements(fcsum[1][2],fcsum[1][2].length+1,{{"PM Peak",abpmVMT + bapmVMT}})
    fcsum[1][2] = InsertArrayElements(fcsum[1][2],fcsum[1][2].length+1,{{"Overnight",ablnVMT + balnVMT}})
    fcsum[1][2] = InsertArrayElements(fcsum[1][2],fcsum[1][2].length+1,{{"Entire Day",abamVMT + baamVMT + abmdVMT + bamdVMT + abpmVMT + bapmVMT + ablnVMT + balnVMT}})
    fcsum[2][2] = InsertArrayElements(fcsum[2][2],fcsum[2][2].length+1,{{"AM Peak",(abamTime + baamTime)/60}})    
    fcsum[2][2] = InsertArrayElements(fcsum[2][2],fcsum[2][2].length+1,{{"Midday",(abmdTime + bamdTime)/60}})
    fcsum[2][2] = InsertArrayElements(fcsum[2][2],fcsum[2][2].length+1,{{"PM Peak",(abpmTime + bapmTime)/60}})
    fcsum[2][2] = InsertArrayElements(fcsum[2][2],fcsum[2][2].length+1,{{"Overnight",(ablnTime + balnTime)/60}})
    fcsum[2][2] = InsertArrayElements(fcsum[2][2],fcsum[2][2].length+1,{{"Entire Day",(abamTime + baamTime + abmdTime + bamdTime + abpmTime + bapmTime + ablnTime + balnTime)/60}})
    return(fcsum)
EndMacro

Macro "GetFieldValue" (recordData,field)
    for i = 1 to recordData.length do
        if recordData[i][1] = field then do
            return(recordData[i][2])
        end
    end
    return(null)
EndMacro

Macro "GetSpaces" (num)
    b = ""
    for i = 1 to num do
        b = b + " "
    end
    return(b)
EndMacro

Macro "GetChars" (char,num)
    b = ""
    for i = 1 to num do
        b = b + char
    end
    return(b)
EndMacro

Macro "Justify" (text,width,type,amount)
    left = 0
    if type = "center" then do
        left = Round((width - Len(text)) / 2,0)
    end
    if type = "left" then do
        left = amount
    end
    if type = "right" then do
        left = width - Len(text) - amount
    end
    right = width - Len(text) - left
    return(RunMacro("GetChars"," ",left) + text + RunMacro("GetChars"," ",right))
EndMacro

Macro "AssignmentTable" (inputArray,title,time,vmt,vht)
    baseLine = "+" + RunMacro("GetChars","-",time) + "+" + RunMacro("GetChars","-",vmt) + "+" + RunMacro("GetChars","-",vht) + "+\n"
    message = "\n" + title +"\n" + baseLine
    message = message + "|" + RunMacro("Justify","Time Period",time,"left",1) + "|" + 
            RunMacro("Justify",inputArray[1][1],vmt,"center",) + "|"  + 
            RunMacro("Justify",inputArray[2][1],vmt,"center",) + "|\n"
    message = message + baseLine
    for i = 1 to inputArray[1][2].length do
       message = message + "|" + RunMacro("Justify",inputArray[1][2][i][1],time,"left",1) + "|" + 
                RunMacro("Justify",RealToString(Round(inputArray[1][2][i][2],0)),vmt,"right",6) + "|" +
                RunMacro("Justify",RealToString(Round(inputArray[2][2][i][2],0)),vmt,"right",6) + "|\n"
    end
    message = message + baseLine
    return(message)
EndMacro


//***********************************************************************************

//**********************************************************************************************
Macro "RunTransitSkims"(pathArray,season_idx)
//This can be used to update transit skims within a feedback loop
        if season_idx=1 then do
		season = "Summer"
		end
	else do
		season = "Winter"
		end

        RunMacro("CreateTripMatrices",pathArray)
	RunMacro("TrafficAssignment",pathArray,cr_user_iters)
        RunMacro("TransitSkimmer",season,pathArray)
        
endMacro
//**********************************************************************************************


//
Macro "SaveLinkData"(pathArray,scenarioName,summer,iteration)
    vws = GetViewNames()
    for i = 1 to vws.length do
        CloseView(vws[i])
    end
    //summer is 1 if summer, 0 otherwise
    outputDirectory = pathArray[13] + "scenarios\\" + scenarioName + "\\outputs_winter\\"
    if summer = 1 then do
        outputDirectory = pathArray[13] + "scenarios\\" + scenarioName + "\\outputs_summer\\"
    end
    fullStreets = OpenTable("FullStreets","FFB",{pathArray[1] + "fullstreets.bin"})
    SetView("FullStreets")
    ExportView("FullStreets|","FFB",outputDirectory + "FullStreets_iter" + IntToString(iteration) + ".bin",,)
    CloseView("FullStreets")
    
EndMacro
//

//***********************************************************************************************
Macro "PreModelRunner"(pathArray,season_idx)
///////////////////////////////////////////////////////////////////////////////////////////
//Define season and what to run                                                          //
///////////////////////////////////////////////////////////////////////////////////////////
	if season_idx=1 then do
		season = "Summer"
		end
	else do
		season = "Winter"
		end

        RunMacro("CreateDriveNetwork",pathArray)
        RunMacro("CopyBaseTripTables",pathArray)
	RunMacro("CreateTripMatrices",pathArray)
	RunMacro("TrafficAssignment",pathArray,cr_user_iters)
        RunMacro("BandsRun",season,pathArray)
        RunMacro("TransitSkimmer",season,pathArray)
        RunMacro("JavaPreModelCode",season,pathArray)

endMacro


///////////////////////////////////////////////////////////////////////////////////////////
//Create Scenario stuff below                                                            //
///////////////////////////////////////////////////////////////////////////////////////////


Dbox "NewScenarioCreator" (pathArray,scenarioList) title: "Create New Scenario"
    init do
        opt_idx = 0
    enditem
    Text "Enter New Scenario Name" 1.5,.6
    Edit Text 2, 2, 20
        variable: scenarioName
    Button "OK" 1,4,9.5,2 do
        //check for scenario existence, correctness of name
        //"CreateNewScenario" (pathArray,scenarioName)
        //set it as current scenario
        nameOK = 1
        scenario_name = Substitute(scenarioName," ","_",)
        for i = 1 to scenarioList.length do
            if Lower(scenarioList[i]) = Lower(scenario_name) then do
               ShowMessage("Scenario already exists!")
               nameOK = 0 
            end
        end
        if nameOK = 1 then do
            RunMacro("CreateNewScenario",pathArray,scenario_name)
            scenarioList = InsertArrayElements(scenarioList,scenarioList.length + 1,{scenario_name})
            return(scenarioList)
        end
    endItem
    Button "Cancel" 13.5,4,9.5,2 do
        return(scenarioList)
    endItem
EndDbox

//step 1 - unzip scenario directory
//step 2 - generate map
//setp 3 - create summer/winter property/xml files
Macro "CreateNewScenario" (pathArray,scenarioName)
    RunMacro("JavaCreateScenario",pathArray,scenarioName)
    RunMacro("GenerateMap",pathArray,scenarioName)
    scenarioPath = RunMacro("GenerifyPath",pathArray[13] + "scenarios\\" + scenarioName + "\\")
    scenarioCodePath = scenarioPath + "code\\"
    referencePath = RunMacro("GenerifyPath",pathArray[13] + "reference\\")
    RunMacro("TemplateToFile",pathArray[8] + "tahoe.template.properties",scenarioCodePath + "tahoe_summer.properties",referencePath,scenarioPath,1)
    RunMacro("TemplateToFile",pathArray[8] + "tahoe.template.properties",scenarioCodePath + "tahoe_winter.properties",referencePath,scenarioPath,0)
    RunMacro("TemplateToFile",pathArray[8] + "log4j_mine.template.xml",scenarioCodePath + "log4j_mine_summer.xml",referencePath,scenarioPath,1)
    RunMacro("TemplateToFile",pathArray[8] + "log4j_mine.template.xml",scenarioCodePath + "log4j_mine_winter.xml",referencePath,scenarioPath,0)
EndMacro

Macro "JavaCreateScenario" (pathArray,scenarioName)
        RunMacro("TemplateToFile",pathArray[8] + "log4j_mine_noScenario.template.xml",pathArray[8] + "log4j_mine_noScenario.xml",RunMacro("GenerifyPath",pathArray[13] + "reference\\"),scenarioPath,1)
	javaClassPath = " -classpath \"" + Substring(pathArray[8],1,Len(pathArray[8]) - 1) + ";" + 
			pathArray[8] + "log4j-1.2.9.jar;" + 
			pathArray[8] + "jxl.jar;" + 
			pathArray[8] + "censusdata.jar;" + 
			pathArray[8] + "common-base.jar;" + 
			pathArray[8] + "synpop.jar;" + 
			pathArray[8] + "tahoe.jar\"" 
	javaVMArguments = " -Dlog4j.configuration=log4j_mine_noScenario.xml"
	javaCommand = " com.pb.tahoe.util.CreateNewScenario \"" + 
	                  RunMacro("GenerifyPath",pathArray[13] + "scenarios\\") + "\" \"" + 
	                  scenarioName + "\" \"" +  
	                  RunMacro("GenerifyPath",pathArray[13] + "reference\\scenario_base.zip") + "\""
	RunProgram("cmd /c java" + javaClassPath + javaVMArguments + javaCommand,)
EndMacro

Macro "GenerateMap" (pathArray,scenarioName)
    scenarioPath = pathArray[13] + "scenarios\\" + scenarioName + "\\"
    originalMapPath = scenarioPath + "gis\\Maps\\Tahoe.map"
    originalMap = OpenMap(originalMapPath,{{"Auto Project", "False"}})
    scope = GetMapScope(originalMap)
    CloseMap(originalMap)
    //DeleteFile(originalMapPath)
    newMap = CreateMap(scenarioName, {{"Scope", scope},{"Auto Project", "False"},{"Location", 100, 150}})
    newLayerPath = scenarioPath + "gis\\Layers\\"
    newTransitPath = scenarioPath + "gis\\Transit_Route_System\\"
    tazLayer = AddLayerEx(newMap,"TAZ",newLayerPath + "TAZ\\TAZ.dbd","TAZ",)
    //SetSearchPath(newLayerPath + "Streets\\")
    streetLayer = AddLayer(newMap,"FullStreets",newLayerPath + "Streets\\FullStreets.dbd","FullStreets")
    transitLayer = AddRouteSystemLayer(newMap,"Tahoe Transit Routes",newTransitPath + "Tahoe_Transit",)
    RunMacro("Set Default RS Style", transitLayer, "TRUE",)
    RunMacro("GenerateSelections")
    SaveMap(scenarioName,scenarioPath + "gis\\Maps\\Tahoe.map")
    CloseMap(scenarioName)
EndMacro

Macro "GenerateSelections" 
    //route selections
    SetLayer("Tahoe Transit Routes")
    seasonArray = {{"S","Summer"},{"W","Winter"}}
    timeArray = {" AM Peak"," Midday"," PM Peak"," Late Night"}
    selectS = "Select * where [Headway " 
    selectE = "] > 0"
    for i = 1 to seasonArray.length do
        seasonQuery = "Select * where "
        for j = 1 to timeArray.length do
            SelectByQuery(seasonArray[i][2] + timeArray[j],"Several",selectS + seasonArray[i][1] + timeArray[j] + selectE,)
            seasonQuery = seasonQuery + "[Headway " + seasonArray[i][1] + timeArray[j] + selectE + " or "
        end
        SelectByQuery(seasonArray[i][2],"Several",left(seasonQuery,len(seasonQuery) - 3),)
    end
    //link selections
    SetLayer("FullStreets")
    SelectByQuery("Drive Links","Several", "Select * where (AB_Cap > 0) OR (BA_Cap > 0)",)
EndMacro

Macro "GenerifyPath" (inputPath)
    inputPath = Substitute(inputPath,"\\","/",)
    inputPath = right(inputPath,len(inputPath)-2)
    return(inputPath)
EndMacro

//summer = 1 means summer, anything else is winter
Macro "TemplateToFile" (templateFile,destinationFile,referencePath,scenarioPath,summer)
    file_from = OpenFile(templateFile, "r")
    file_to = OpenFile(destinationFile, "w")
    season = "winter"
    summerBool = "false"
    schoolBool = "true"
    if summer=1 then do
        season = "summer"
        summerBool = "true"
        schoolBool = "false"
    end
    property=readarray(file_from)
    index = 1
    replacementArray = {{"@@summer@@",summerBool},
                        {"@@school@@",schoolBool},
                        {"@@reference@@",referencePath},
                        {"@@scenario@@",scenarioPath},
                        {"@@season@@",season}}
    while index<=ArrayLength(property) do
        for i = 1 to replacementArray.length do
            property[index] = Substitute(property[index],replacementArray[i][1],replacementArray[i][2],)
        end
        index = index + 1
    end
    writearray(file_to, property)
    CloseFile(file_from)
    CloseFile(file_to)
EndMacro



///////////////////////////////////////////////////////////////////////////////////////////
//Copy base trip tables stuff below                                                      //
///////////////////////////////////////////////////////////////////////////////////////////

Macro "CopyBaseTripTables" (pathArray)
    //This macro copies the base trip tables to the required trip table names
    timePeriods = {"AM","MD","PM","LN"}
    for i = 1 to timePeriods.length do
        matrixPath = pathArray[10] + "Trips_" + timePeriods[i]
        RunProgram("cmd /c del \"" + matrixPath + ".csv\"",)
        CopyFile(matrixPath + "_base.csv",matrixPath + ".csv")
    end
endMacro

///////////////////////////////////////////////////////////////////////////////////////////
//Walk bands stuff below                                                                 //
///////////////////////////////////////////////////////////////////////////////////////////

Macro "BandsRun" (season,pathArray)
    //This macro creates long & short walk bands for the tahoe region (by season)
    outputpath = pathArray[5]
    mapoutputPath = pathArray[7] + "Tahoe.map"
    
    RunMacro("BandsCalculator",outputpath,mapoutputPath,season,"ShortWalkAccess_" + season,0.25)
    RunMacro("BandsCalculator",outputpath,mapoutputPath,season,"LongWalkAccess_" + season,0.5)
    RunMacro("BandCombine",outputpath,season,"WalkAccess_" + season + ".csv")
    //Cleanup
    RunProgram("cmd /c del \"" + outputpath + "\\LongWalkAccess_" + season + ".bin\"",)
    RunProgram("cmd /c del \"" + outputpath + "\\LongWalkAccess_" + season + ".DCB\"",)
    RunProgram("cmd /c del \"" + outputpath + "\\ShortWalkAccess_" + season + ".bin\"",)
    RunProgram("cmd /c del \"" + outputpath + "\\ShortWalkAccess_" + season + ".DCB\"",)
    RunProgram("cmd /c del \"" + outputpath + "\\WalkAccess_" + season + ".DCC\"",)
    cleanupArray = {{"IntersectTest.bin","INTERSECTTEST.BX","IntersectTest.DCB","TAZOverlay.bin","TAZOverlay.DCB","TempWalkTable.bin","TempWalkTable.DCB"},
                     {".bdr",".bin",".BX",".cdd.",".cdk",".dbd",".DCB",".dsc",".dsk",".grp",".lok",".pnk",".pts",".r0",".r1"}}
    for i = 1 to cleanupArray[1].length do
      RunProgram("cmd /c del \"" + outputpath + "\\" + cleanupArray[1][i] + "\"",)
    end
    for i = 1 to cleanupArray[2].length do
      RunProgram("cmd /c del \"" + outputpath + "\\WalkBands" + cleanupArray[2][i] + "\"",)
    end
endMacro

Macro "BandsCalculator" (path, mapPath, routeSelection, outputName, distanceInMiles)
    OpenMap(mapPath,)
    SetLayer("Tahoe Transit Routes")
    //showmessage(path + "WalkBands.dbd")
    CreateBuffers(path + "WalkBands.dbd","Transit Band",{routeSelection},"Value",{distanceInMiles},{{"Exterior","Merged"},{"Interior","Merged"},{"Units","Miles"}})
    overlay = AddLayerEx(,"Overlay",path + "WalkBands.dbd","Transit Band",{})
    ComputeIntersectionPercentages({"TAZ","Overlay"},path + "IntersectTest.bin",{"Database",null})
    DropLayer(,"Overlay")
    intersection = OpenTable("Intersection","FFB",{path + "IntersectTest.bin"})
    SetView("Intersection")
    outside = CreateSet("Outsiders")
    n = SelectByQuery(outside,"Several","Select * where Area_2 = 0",)
    DeleteRecordsInSet(outside)
    joinview = JoinViews("TAZOverlay","TAZ.ID","Intersection.Area_1",{})
    ExportView("TAZOverlay|","FFB",path + "TAZOverlay.bin",{"TAZ","Percent_1","PopulatedArea"},)
    CloseView("Intersection")
    tazOverlay = OpenTable("OverlayTAZ","FFB",{path + "TAZOverlay.bin"})
    strct = GetTableStructure("OverlayTAZ")
    for i = 1 to strct.length do
      strct[i] = strct[i] + {strct[i][1]}
    end
    strct = strct + {{"TempField","Real",12,9,"False",,,,,,,null}} 
    ModifyTable("OverlayTAZ",strct)
    percent = CreateExpression("OverlayTAZ","Percent","min(1,Percent_1 / PopulatedArea)",)
    ravalue = CreateExpression("OverlayTAZ","Revalue","NulltoZero(TempField)",)
    SetView("OverlayTAZ")
    SetRecordsValues(null,{{"TempField"},null},"Formula",{"Percent"},)
    SetRecordsValues(null,{{"Percent_1"},null},"Formula",{"Revalue"},)
    ExportView("OverlayTAZ|","FFB",path + outputName + ".bin",{"TAZ","Percent_1"},)
    CloseView("OverlayTAZ")
    CloseMap()
endMacro

Macro "BandCombine" (path, name, outputName)
    firstView = OpenTable("ShortWalk","FFB",{path + "ShortWalkAccess_" + name + ".bin"})
    strct1 = GetTableStructure("ShortWalk")
    for i = 1 to strct1.length do
      strct1[i] = strct1[i] + {strct1[i][1]}
    end
    strct1[1][1] = "TAZ1"
    strct1[2][1] = "ShortPercent"
    ModifyTable("ShortWalk",strct1)
    secondView = OpenTable("LongWalk","FFB",{path + "LongWalkAccess_" + name + ".bin"})
    strct2 = GetTableStructure("LongWalk")
    for i = 1 to strct2.length do
      strct2[i] = strct2[i] + {strct2[i][1]}
    end
    strct2[2][1] = "LongPercent"
    ModifyTable("LongWalk",strct2)
    joinview = JoinViews("WalkPercents","ShortWalk.TAZ1","LongWalk.TAZ",)
    ExportView("WalkPercents|","FFB",path + "TempWalkTable.bin",{"TAZ","ShortPercent","LongPercent"},)
    CloseView("ShortWalk")
    CloseView("LongWalk")
    CloseView("WalkPercents")
    //Create absolute %s as well as for no walk
    fullView = OpenTable("FullWalk","FFB",{path + "TempWalkTable.bin"})
    strct = GetTableStructure("FullWalk")
    for i = 1 to strct.length do
      strct[i] = strct[i] + {strct[i][1]}
    end
    dim strct1[3,strct[1].length]
    for i = 1 to 3 do
      strct1[i] = strct[i]
    end
    strct1[2][1] = "ShortWalkPercent"
    strct1[3][1] = "LongWalkPercent"
    ModifyTable("FullWalk",strct1)
    longP = CreateExpression("FullWalk","LongP","LongWalkPercent - ShortWalkPercent",)
    SetView("FullWalk")
    SetRecordsValues(null,{{"LongWalkPercent"},null},"Formula",{"LongP"},)
    ExportView("FullWalk|","CSV",path + outputName,{"TAZ","ShortWalkPercent","LongWalkPercent"},{{"CSV Header"}})
    CloseView("FullWalk")
endMacro

/////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//Transit skim / Shortest drive path stuff below                                                               //
/////////////////////////////////////////////////////////////////////////////////////////////////////////////////

Macro "TransitSkimmer" (season,pathArray)
//showmessage("Run transit skim")

//Last array is {global fare, global transfer fare, global initial wait weight, global transfer wait weight, global transfer time penalty, vot}
peakPeriodTime = "_ConIVTT"
offPeakPeriodTime = "_UnConIVTT"
amPeakPeriodTime = "_AM_IVTT"
mdOffPeakPeriodTime = "_MD_IVTT"
pmPeakPeriodTime = "_PM_IVTT"
lnOffPeakPeriodTime = "_LN_IVTT"
//amPeakPeriodTime = "_ConIVTT"
//mdOffPeakPeriodTime = "_UnConIVTT"
//pmPeakPeriodTime = "_ConIVTT"
//lnOffPeakPeriodTime = "_UnConIVTT"

timeArray = {{{"SummerAMPeak","Summer AM Peak","[AB" + amPeakPeriodTime + " / BA" + amPeakPeriodTime + "]"," S AM Peak]",{100,0,2.18,2.18,6,13.38},"*" + amPeakPeriodTime,"SAMP","AB" + amPeakPeriodTime,"BA" + amPeakPeriodTime},
              {"SummerMidday","Summer Midday","[AB" + mdOffPeakPeriodTime + " / BA" + mdOffPeakPeriodTime + "]"," S Midday]",{100,0,2.18,2.18,6,13.38},"*" + mdOffPeakPeriodTime,"SM","AB" + mdOffPeakPeriodTime,"BA" + mdOffPeakPeriodTime},
              {"SummerPMPeak","Summer PM Peak","[AB" + pmPeakPeriodTime + " / BA" + pmPeakPeriodTime + "]"," S PM Peak]",{100,0,2.18,2.18,6,13.38},"*" + pmPeakPeriodTime,"SPMP","AB" + pmPeakPeriodTime,"BA" + pmPeakPeriodTime},
              {"SummerLateNight","Summer Late Night","[AB" + lnOffPeakPeriodTime + " / BA" + lnOffPeakPeriodTime + "]"," S Late Night]",{100,0,2.18,2.18,6,13.38},"*" + lnOffPeakPeriodTime,"SLN","AB" + lnOffPeakPeriodTime,"BA" + lnOffPeakPeriodTime}},
             {{"WinterAMPeak","Winter AM Peak","[AB" + amPeakPeriodTime + " / BA" + amPeakPeriodTime + "]"," W AM Peak]",{100,0,2.18,2.18,6,13.38},"*" + amPeakPeriodTime,"WAMP","AB" + amPeakPeriodTime,"BA" + amPeakPeriodTime},
              {"WinterMidday","Winter Midday","[AB" + mdOffPeakPeriodTime + " / BA" + mdOffPeakPeriodTime + "]"," W Midday]",{100,0,2.18,2.18,6,13.38},"*" + mdOffPeakPeriodTime,"WM","AB" + mdOffPeakPeriodTime,"BA" + mdOffPeakPeriodTime},
              {"WinterPMPeak","Winter PM Peak","[AB" + pmPeakPeriodTime + " / BA" + pmPeakPeriodTime + "]"," W PM Peak]",{100,0,2.18,2.18,6,13.38},"*" + pmPeakPeriodTime,"WPMP","AB" + pmPeakPeriodTime,"BA" + pmPeakPeriodTime},
              {"WinterLateNight","Winter Late Night","[AB" + lnOffPeakPeriodTime + " / BA" + lnOffPeakPeriodTime + "]"," W Late Night]",{100,0,2.18,2.18,6,13.38},"*" + lnOffPeakPeriodTime,"WLN","AB" + lnOffPeakPeriodTime,"BA" + lnOffPeakPeriodTime}}}

if season = "Summer" then do
  //RunMacro("CreateDriveNetwork",pathArray)
  for i = 1 to timeArray[1].length do
    RunMacro("CreateTransitNetwork", pathArray,timeArray[1][i][1] + "TransitNetwork.tnw",timeArray[1][i][2],timeArray[1][i][3],timeArray[1][i][4],timeArray[1][i][7],timeArray[1][i][8],timeArray[1][i][9])
    RunMacro("TransitSkim", "Transit",pathArray,timeArray[1][i][1] + "TransitNetwork.tnw",timeArray[1][i][2],timeArray[1][i][3],timeArray[1][i][4],timeArray[1][i][5],timeArray[1][i][1] + "TransitSkim")
    RunMacro("TransitSkim", "Drive2Transit",pathArray,timeArray[1][i][1] + "TransitNetwork.tnw",timeArray[1][i][2],timeArray[1][i][3],timeArray[1][i][4],timeArray[1][i][5],timeArray[1][i][1] + "Drive2TransitSkim")  
    RunMacro("DriveDistanceSkim",pathArray,timeArray[1][i][3],timeArray[1][i][2],timeArray[1][i][1] + "DriveDistanceSkim")
  end
end
if season = "Winter" then do
  //RunMacro("CreateDriveNetwork",pathArray)
  for i = 1 to timeArray[2].length do
    RunMacro("CreateTransitNetwork", pathArray,timeArray[2][i][1] + "TransitNetwork.tnw",timeArray[2][i][2],timeArray[2][i][3],timeArray[2][i][4],timeArray[2][i][7],timeArray[2][i][8],timeArray[2][i][9])
    RunMacro("TransitSkim", "Transit", pathArray,timeArray[2][i][1] + "TransitNetwork.tnw",timeArray[2][i][2],timeArray[2][i][3],timeArray[2][i][4],timeArray[2][i][5],timeArray[2][i][1] + "TransitSkim")
    RunMacro("TransitSkim", "Drive2Transit", pathArray,timeArray[2][i][1] + "TransitNetwork.tnw",timeArray[2][i][2],timeArray[2][i][3],timeArray[2][i][4],timeArray[2][i][5],timeArray[2][i][1] + "Drive2TransitSkim")
    RunMacro("DriveDistanceSkim",pathArray,timeArray[2][i][3],timeArray[2][i][2],timeArray[2][i][1] + "DriveDistanceSkim")
  end
end
  RunMacro("JavaSkimConverter",season,pathArray)
endMacro

Macro "CreateTransitNetwork" (pathArray, network, period, skimtime, fareheadway, stopselect, abskimtime, baskimtime) 
     RunMacro("TCB Init")
     Opts = null
     Opts.Input.[Transit RS] = pathArray[3] + "Tahoe_Transit.rts"
     Opts.Input.[RS Set] = {pathArray[3] + "Tahoe_Transit.rts|Tahoe Transit Routes", "Tahoe Transit Routes", period, "Select * where [Headway" + fareheadway + " > 0"}
     Opts.Input.[Walk Link Set] = {pathArray[1] + "fullstreets.DBD|FullStreets", "FullStreets", "Walk Links", "Select * where Mode = 1"}
     //Opts.Input.[Stop Set] = {pathArray[3] + "Tahoe_TransitS.DBD|Route Stops", "Route Stops", "Summer AM Peak", "Select * where " + stopselect + " > 0"}
     Opts.Input.[Stop Set] = {pathArray[3] + "Tahoe_TransitS.DBD|Route Stops", "Route Stops", period, "Select * where " + stopselect + " > 0"}
     Opts.Input.[Drive Set] = {pathArray[1] + "fullstreets.DBD|FullStreets", "FullStreets", "Drive Links", "Select * where (AB_Cap > 0) OR (BA_Cap > 0)"}
     Opts.Global.[Network Label] = "Tahoe Summer AM Peak Transit Network"
     Opts.Global.[Network Options].[Route Attributes] = {{"Route_ID", {"[Tahoe Transit Routes].Route_ID"}}, {"Mode", {"[Tahoe Transit Routes].Mode"}}, {"[Headway" + fareheadway, {"[Tahoe Transit Routes].[Headway" + fareheadway}}, {"[Fare" + fareheadway, {"[Tahoe Transit Routes].[Fare" + fareheadway}}}
     Opts.Global.[Network Options].[Stop Attributes].ID = {"[Route Stops].ID"}
     Opts.Global.[Network Options].[Stop Attributes].Longitude = {"[Route Stops].Longitude"}
     Opts.Global.[Network Options].[Stop Attributes].Latitude = {"[Route Stops].Latitude"}
     Opts.Global.[Network Options].[Stop Attributes].Route_ID = {"[Route Stops].Route_ID"}
     Opts.Global.[Network Options].[Stop Attributes].Pass_Count = {"[Route Stops].Pass_Count"}
     Opts.Global.[Network Options].[Stop Attributes].Milepost = {"[Route Stops].Milepost"}
     Opts.Global.[Network Options].[Stop Attributes].Physical_Stop_ID = {"[Route Stops].Physical_Stop_ID"}
     Opts.Global.[Network Options].[Stop Attributes].STOP_ID = {"[Route Stops].STOP_ID"}
     Opts.Global.[Network Options].[Stop Attributes].NearestNode = {"[Route Stops].NearestNode"}
     Opts.Global.[Network Options].[Street Attributes] = {{"Length", {"FullStreets.Length", "FullStreets.Length"}}, {skimtime, {"FullStreets.AB_WalkTime", "FullStreets.BA_WalkTime"}}}
     Opts.Global.[Network Options].Walk = "Yes"
     Opts.Global.[Network Options].Overide = {"[Route Stops].ID", "Route Stops.NearestNode"}
     Opts.Global.[Network Options].[Drive Links] = "FullStreets|Drive Links"
     Opts.Global.[Network Options].[Link Attributes] = {{"Length", {"FullStreets.Length", "FullStreets.Length"}, "SUMFRAC"}, {skimtime, {"FullStreets." + abskimtime, "FullStreets." + baskimtime}, "SUMFRAC"}}
     Opts.Global.[Network Options].[Mode Field] = "[Tahoe Transit Routes].Mode"
     //Opts.Global.[Network Options].[Walk Mode] = {"FullStreets.Mode", "FullStreets.Mode"}
     Opts.Global.[Network Options].[Walk Mode] = "FullStreets.Mode"
     Opts.Output.[Network File] = pathArray[2] + network
     ret_value = RunMacro("TCB Run Operation", 1, "Build Transit Network", Opts)
endMacro

Macro "TransitSkim" (type, pathArray, network, period, skimtime, fareheadway, costArray, outputname)
    RunMacro("TCB Init")
    //Set up transit skim
     Opts = null
     Opts.Input.[Transit RS] = pathArray[3] + "Tahoe_Transit.rts"
     Opts.Input.[Transit Network] = pathArray[2] + network
     Opts.Input.[Mode Table] = {pathArray[4] + "TahoeModeTable.bin"}
     Opts.Field.[Link Impedance] = skimtime
     Opts.Field.[Link Drive Time] = skimtime
     Opts.Field.[Route Fare] = "[Fare" + fareheadway
     Opts.Field.[Route Headway] = "[Headway" + fareheadway
     Opts.Field.[Mode Imp Weight] = "TahoeModeTable.Speed_Factor"
     Opts.Field.[Mode Speed] = "TahoeModeTable.Speed"
     Opts.Field.[Mode Used] = "TahoeModeTable.Mode_Used"
     Opts.Global.[Global Fare Value] = costArray[1]
     Opts.Global.[Global Xfer Fare] = costArray[2]
     Opts.Global.[Global IWait Weight] = costArray[3]
     Opts.Global.[Global XWait Weight] = costArray[4]
     Opts.Global.[Global Xfer Time] = costArray[5]
     Opts.Global.[Value of Time] =  costArray[6]
     Opts.Global.[Max Trip Time] = 999999
     Opts.Flag.[Use All Walk Path] = "No"
     Opts.Flag.[Use Mode] = "Yes"
     if type = "Drive2Transit" then do
       Opts.Input.[Parking Node Set] = {pathArray[1] + "FullStreets.DBD|Node", "Node", "PNRStop", "Select * where PNRStop > 0"}
       Opts.Input.[Driving Link Set] = {pathArray[1] + "FullStreets.DBD|FullStreets", "FullStreets"}
       Opts.Flag.[Use Park and Ride] = "Yes"
     end
     ret_value = RunMacro("TCB Run Operation", 1, "Transit Network Setting PF", Opts)
    
    //Run transit skim
     Opts = null
     Opts.Input.Database = pathArray[1] + "FullStreets.DBD"
     Opts.Input.Network = pathArray[2] + network
     Opts.Input.[Origin Set] = {pathArray[1] + "FullStreets.DBD|Node", "Node", "Centroids", "Select * where TAZ > 0"}
     Opts.Input.[Destination Set] = {pathArray[1] + "FullStreets.DBD|Node", "Node", "Centroids"}
     Opts.Global.[Skim Var] = {"Fare", "In-Vehicle Time", "Initial Wait Time", "Transfer Wait Time", "Transfer Penalty Time", "Transfer Walk Time", "Access Walk Time", "Egress Walk Time", "Access Drive Time", "Dwelling Time", "Number of Transfers", skimtime}
     Opts.Global.[OD Layer Type] = 2
     Opts.Global.[Skim Modes] = {1, 2, 3}
     if type = "Transit" then Opts.Output.[Skim Matrix].Label = period + " Transit Skim Matrix"
     if type = "Drive2Transit" then Opts.Output.[Skim Matrix].Label = period + " Drive2Transit Skim Matrix"
     //Opts.Output.[Skim Matrix].Compression = 1
     Opts.Output.[Skim Matrix].[File Name] = pathArray[5] + outputname + ".mtx"
     if type = "Transit" then do
       Opts.Output.[Skim Matrix].Label = period + " Transit Skim Matrix"
       ret_value = RunMacro("TCB Run Procedure", 1, "Transit Skim PF", Opts)
     end
     if type = "Drive2Transit" then do
       Opts.Output.[Skim Matrix].Label = period + " Drive2Transit Skim Matrix"
       ret_value = RunMacro("TCB Run Procedure", 2, "Transit Skim PF", Opts)
     end
     
     skimMatrix = OpenMatrix(pathArray[5] + outputname + ".mtx","True")
     matrixInfo = GetMatrixInfo(skimMatrix)
     //So that reading back in binary file in ReIndex doesn't bomb over names
     SetMatrixCoreName(skimMatrix,matrixInfo[6][7][2][12],"Walk Time")
     SetMatrixCoreName(skimMatrix,matrixInfo[6][7][2][13],"Bus Time")
     SetMatrixCoreName(skimMatrix,matrixInfo[6][7][2][14],"Gondola Time")
     
     //ReIndex matrix
     //RunMacro("ReIndex",pathArray,outputname)
     //RunProgram("cmd /c del \"" + pathArray[5] + outputname + ".mtx\"",)
     //RunProgram("cmd /c ren \"" + pathArray[5] + outputname + "2.mtx\" " + outputname + ".mtx ",)
     
     //Save matrix as csv
     RunMacro("ToCSV",pathArray,outputname)
     RunProgram("cmd /c del \"" + pathArray[5] + outputname + ".mtx\"",)
     RunProgram("cmd /c del \"" + pathArray[5] + outputname + ".DCC\"",)

endMacro

Macro "CreateDriveNetwork" (pathArray)
     RunMacro("TCB Init")
     Opts = null
     Opts.Input.[Link Set] = {pathArray[1] + "fullstreets.DBD|FullStreets", "FullStreets", "Drive Links", "Select * where (AB_Cap > 0) OR (BA_Cap > 0)"}
     Opts.Global.[Network Options].[Node ID] = "Node.ID"
     Opts.Global.[Network Options].[Link ID] = "FullStreets.ID"
     Opts.Global.[Network Options].[Turn Penalties] = "No"
     Opts.Global.[Network Options].[Keep Duplicate Links] = "FALSE"
     Opts.Global.[Network Options].[Ignore Link Direction] = "FALSE"
     //Opts.Global.[Link Options] = {{"Length", "FullStreets.Length", "FullStreets.Length"}, {"ID", "FullStreets.ID", "FullStreets.ID"}, {"Dir", "FullStreets.Dir", "FullStreets.Dir"}, {"[AB_UnConIVTT / BA_UnConIVTT]", "FullStreets.AB_UnConIVTT", "FullStreets.BA_UnConIVTT"}, {"[AB_ConIVTT / BA_ConIVTT]", "FullStreets.AB_ConIVTT", "FullStreets.BA_ConIVTT"}, {"[AB_Cap / BA_Cap]", "FullStreets.AB_Cap", "FullStreets.BA_Cap"}}
     Opts.Global.[Link Options] = {{"Length", "FullStreets.Length", "FullStreets.Length"}, {"ID", "FullStreets.ID", "FullStreets.ID"}, {"Dir", "FullStreets.Dir", "FullStreets.Dir"}, {"FC", "FullStreets.AB_FC", "FullStreets.BA_FC"}, {"Speed", "FullStreets.AB_Speed", "FullStreets.BA_Speed"}, {"[AB_AM_IVTT / BA_AM_IVTT]", "FullStreets.AB_AM_IVTT", "FullStreets.BA_AM_IVTT"}, {"[AB_MD_IVTT / BA_MD_IVTT]", "FullStreets.AB_MD_IVTT", "FullStreets.BA_MD_IVTT"}, {"[AB_PM_IVTT / BA_PM_IVTT]", "FullStreets.AB_PM_IVTT", "FullStreets.BA_PM_IVTT"}, {"[AB_LN_IVTT / BA_LN_IVTT]", "FullStreets.AB_LN_IVTT", "FullStreets.BA_LN_IVTT"}, {"[AB_Cap / BA_Cap]", "FullStreets.AB_Cap", "FullStreets.BA_Cap"},{"[AB_AM_Cap / BA_AM_Cap]", "FullStreets.AB_AM_Cap", "FullStreets.BA_AM_Cap"},{"[AB_MD_Cap / BA_MD_Cap]", "FullStreets.AB_MD_Cap", "FullStreets.BA_MD_Cap"},{"[AB_PM_Cap / BA_PM_Cap]", "FullStreets.AB_PM_Cap", "FullStreets.BA_PM_Cap"},{"[AB_LN_Cap / BA_LN_Cap]", "FullStreets.AB_LN_Cap", "FullStreets.BA_LN_Cap"},{"FF Travel Time","FullStreets.AB_FF_TravelTime","FullStreets.BA_FF_TravelTime"},{"Alpha","FullStreets.AB_Alpha","FullStreets.BA_Alpha"},{"Beta","FullStreets.AB_Beta","FullStreets.BA_Beta"}}
     Opts.Global.[Node Options].ID = "Node.ID"
     Opts.Global.[Node Options].Longitude = "Node.Longitude"
     Opts.Global.[Node Options].Latitude = "Node.Latitude"
     Opts.Global.[Node Options].TAZ = "Node.TAZ"
     Opts.Global.[Node Options].PNRStop = "Node.PNRStop"
     Opts.Output.[Network File] = pathArray[6] + "Tahoe_Drive_Network.net"
     ret_value = RunMacro("TCB Run Operation", 1, "Build Highway Network", Opts)
endMacro

Macro "DriveDistanceSkim" (pathArray, time, period, outputname)
    RunMacro("TCB Init")
    //Set up and run shortest path skim
     Opts = null
     Opts.Input.Network = pathArray[6] + "Tahoe_Drive_Network.net"
     Opts.Input.[Origin Set] = {pathArray[1] + "FullStreets.DBD|Node", "Node", "Centroids", "Select * where TAZ > 0"}
     Opts.Input.[Destination Set] = {pathArray[1] + "FullStreets.DBD|Node", "Node", "Centroids"}
     Opts.Input.[Via Set] = {pathArray[1] + "FullStreets.DBD|Node", "Node"}
     //Opts.Field.Minimize = "[AB_ConIVTT / BA_ConIVTT]"
     Opts.Field.Minimize = time
     Opts.Field.Nodes = "Node.ID"
     Opts.Field.[Skim Fields].Length = "All"
     Opts.Output.[Output Matrix].Label = period + " Shortest Path Drive Skim"
     //Opts.Output.[Output Matrix].Compression = 1
     Opts.Output.[Output Matrix].[File Name] = pathArray[5] + outputname + ".mtx"
     ret_value = RunMacro("TCB Run Procedure", 1, "TCSPMAT", Opts)
     
     //Create intrazonal times and distances
     Opts = null
     Opts.Input.[Matrix Currency] = {pathArray[5] + outputname + ".mtx", time,,}
     ret_value = RunMacro("TCB Run Procedure", 1, "Intrazonal", Opts)
     Opts.Input.[Matrix Currency] = {pathArray[5] + outputname + ".mtx", "Length (Skim)",,}
     ret_value = RunMacro("TCB Run Procedure", 1, "Intrazonal", Opts)
     
     //Add external times and distances     
     //Opts = null
     //Opts.Input.[Matrix Currency] = {pathArray[5] + outputname + ".mtx", time,,}
     //Opts.Input.[Formula Currencies] = {{pathArray[9] + "ExternalDistanceMatrix.mtx", "FIELD_3",,}}
     //Opts.Global.Method = 11
     //Opts.Global.[Cell Range] = 2
     ////Assumes 50 mph currently for traversing external distance
     //Opts.Global.[Expression Text] = "[" + time + "] + ( [ExternalDistanceMatrix Matrix File].[FIELD_3] * 60 / 50 )"
     //Opts.Global.[Formula Labels] = {"ExternalDistanceMatrix Matrix File"}
     //Opts.Global.[Force Missing] = "Yes"
     //ret_value = RunMacro("TCB Run Operation", 1, "Fill Matrices", Opts)
     //Opts = null
     //Opts.Input.[Matrix Currency] = {pathArray[5] + outputname + ".mtx", "Length (Skim)",,}
     //Opts.Input.[Formula Currencies] = {{pathArray[9] + "ExternalDistanceMatrix.mtx", "FIELD_3",,}}
     //Opts.Global.Method = 11
     //Opts.Global.[Cell Range] = 2
     //Opts.Global.[Expression Text] = "[Length (Skim)] + [ExternalDistanceMatrix Matrix File].[FIELD_3]"
     //Opts.Global.[Formula Labels] = {"ExternalDistanceMatrix Matrix File"}
     //Opts.Global.[Force Missing] = "Yes"
     //ret_value = RunMacro("TCB Run Operation", 1, "Fill Matrices", Opts)
     Opts = null
     Opts.Input.[Matrix Currency] = {pathArray[5] + outputname + ".mtx", time,,}
     Opts.Input.[Core Currencies] = {{pathArray[5] + outputname + ".mtx", time,,}, 
                                     {pathArray[9] + "ExternalDistanceMatrix.mtx", "FIELD_3",,}}
     Opts.Global.Method = 7
     Opts.Global.[Cell Range] = 2
     Opts.Global.[Matrix K] = {1, 1.2}
     Opts.Global.[Force Missing] = "Yes"
     ret_value = RunMacro("TCB Run Operation", 1, "Fill Matrices", Opts)
     Opts = null
     Opts.Input.[Matrix Currency] = {pathArray[5] + outputname + ".mtx", "Length (Skim)",,}
     Opts.Input.[Core Currencies] = {{pathArray[5] + outputname + ".mtx", "Length (Skim)",,}, 
                                     {pathArray[9] + "ExternalDistanceMatrix.mtx", "FIELD_3",,}}
     Opts.Global.Method = 7
     Opts.Global.[Cell Range] = 2
     Opts.Global.[Matrix K] = {1, 1}
     Opts.Global.[Force Missing] = "Yes"
     ret_value = RunMacro("TCB Run Operation", 1, "Fill Matrices", Opts)
     
     
     
     
     ////ReIndex matrix
     //RunMacro("ReIndex",pathArray,outputname)
     //RunProgram("cmd /c del " + pathArray[5] + outputname + ".mtx",)
     //RunProgram("cmd /c ren " + pathArray[5] + outputname + "2.mtx " + outputname + ".mtx ",)
     
     
     //Save matrix as csv
     RunMacro("ToCSV",pathArray,outputname)
     RunProgram("cmd /c del \"" + pathArray[5] + outputname + ".mtx\"",)
     RunProgram("cmd /c del \"" + pathArray[5] + outputname + ".DCC\"",)

endMacro

Macro "ReIndex" (pathArray, matrixName)
    RunMacro("TCB Init")

    //Create correct matrix index
     Opts = null
     Opts.Input.[Current Matrix] = pathArray[5] + matrixName + ".mtx"
     Opts.Input.[Index Type] = "Both"
     Opts.Input.[View Set] = {pathArray[1] + "FullStreets.DBD|Node", "Node", "Centroids", "Select * where TAZ > 0"}
     Opts.Input.[Old ID Field] = {pathArray[1] + "FullStreets.DBD|Node", "ID"}
     Opts.Input.[New ID Field] = {pathArray[1] + "FullStreets.DBD|Node", "TAZ"}
     Opts.Output.[New Index] = "TAZ"
     ret_value = RunMacro("TCB Run Operation", 1, "Add Matrix Index", Opts)
     
   //Export matrix to bin using correct matrix index
     skimMatrix = OpenMatrix(pathArray[5] + matrixName + ".mtx","True")
     SetMatrixIndex(skimMatrix,"TAZ","TAZ")
     matrixInfo = GetMatrixInfo(skimMatrix)
     CreateTableFromMatrix(skimMatrix,pathArray[5] + matrixName + ".bin","FFB",{{"Complete","Yes"}})
   
   //Import matrix using same matrix name
     newMatrix = OpenTable("NewMatrix","FFB",{pathArray[5] + matrixName + ".bin"})
     skimMatrixNew = CreateMatrixFromView(matrixInfo[4],"NewMatrix|","[TAZ:1]","TAZ",matrixInfo[6][7][2],
                        {{"File Name",pathArray[5] + matrixName + "2.mtx"},{matrixInfo[6][4]},{matrixInfo[6][1]},
                         {matrixInfo[6][2]},{matrixInfo[6][3]},{matrixInfo[6][8]}})
   //Cleanup
     CloseView("NewMatrix")
     RunProgram("cmd /c del \"" + pathArray[5] + matrixName + ".bin\"",)
     RunProgram("cmd /c del \"" + pathArray[5] + matrixName + ".dcb\"",)

endMacro

Macro "ToCSV" (pathArray, matrixName)
    //This macro saves a matrix file as a csv
    RunMacro("TCB Init")

    //Create correct matrix index
     Opts = null
     Opts.Input.[Current Matrix] = pathArray[5] + matrixName + ".mtx"
     Opts.Input.[Index Type] = "Both"
     Opts.Input.[View Set] = {pathArray[1] + "FullStreets.DBD|Node", "Node", "Centroids", "Select * where TAZ > 0"}
     Opts.Input.[Old ID Field] = {pathArray[1] + "FullStreets.DBD|Node", "ID"}
     Opts.Input.[New ID Field] = {pathArray[1] + "FullStreets.DBD|Node", "TAZ"}
     Opts.Output.[New Index] = "TAZ"
     ret_value = RunMacro("TCB Run Operation", 1, "Add Matrix Index", Opts)
    
    //Export matrix to csv using correct matrix index
     skimMatrix = OpenMatrix(pathArray[5] + matrixName + ".mtx","True")
     SetMatrixIndex(skimMatrix,"TAZ","TAZ")
     AddMatrixCore(skimMatrix,"Blank")
     mc = CreateMatrixCurrency(skimMatrix,"Blank",,,)
     FillMatrix(mc,,,{"Copy",0},)
     
     CreateTableFromMatrix(skimMatrix,pathArray[5] + matrixName + ".bin","FFB",{{"Complete","Yes"}})
     tempTable = OpenTable("TempTable","FFB",{pathArray[5] + matrixName + ".bin",})
     tableInfo = GetTableStructure(tempTable)
     dim newTableInfo[tableInfo.length - 1]
     for i = 1 to (tableInfo.length - 1) do
       newTableInfo[i] = tableInfo[i] + {tableInfo[i][1]}
     end     
     ModifyTable(tempTable,newTableInfo)
     
     //Set null values to 0
     //viewInfo = GetViewStructure(tempTable)
     //SetView(tempTable)
     //for i = 1 to viewInfo.length do
     //   revalue = CreateExpression("TempTable","Revalue","NulltoZero(" + viewInfo[i][1] + ")",)
     //   SetRecordsValues(null,{{viewInfo[i][1]},null},"Formula",{"Revalue"},)
     //   DestroyExpression("TempTable.Revalue")
     //end
     
     ExportView("TempTable|","CSV",pathArray[5] + matrixName + ".csv",,{{"CSV Header"}})
     CloseView(tempTable)
     RunProgram("cmd /c del \"" + pathArray[5] + matrixName + ".bin\"",)
     RunProgram("cmd /c del \"" + pathArray[5] + matrixName + ".DCB\"",)

endMacro

///////////////////////////////////////////////////////////////////////////////////////////
//Java pre-model code below                                                              //
///////////////////////////////////////////////////////////////////////////////////////////

Macro "JavaPreModelCode" (season, pathArray)
	javaClassPath = " -classpath \"" + pathArray[8] + ";" + 
			pathArray[8] + "log4j-1.2.9.jar;" + 
			pathArray[8] + "jxl.jar;" + 
			pathArray[8] + "censusdata.jar;" + 
			pathArray[8] + "common-base.jar;" + 
			pathArray[8] + "synpop.jar;" + 
			pathArray[8] + "tahoe.jar\"" 
	javaVMArguments = " -Dlog4j.configuration=log4j_mine.xml"
	javaCommand = " com.pb.tahoe.dest_time_mode.DCAlternativeSet"
	RunProgram("cmd /c java" + javaClassPath + javaVMArguments + javaCommand,)

endMacro

Macro "JavaSkimConverter" (season, pathArray)
	javaClassPath = " -classpath \"" + pathArray[8] + ";" + 
			pathArray[8] + "log4j-1.2.9.jar;" + 
			pathArray[8] + "jxl.jar;" + 
			pathArray[8] + "censusdata.jar;" + 
			pathArray[8] + "common-base.jar;" + 
			pathArray[8] + "synpop.jar;" + 
			pathArray[8] + "tahoe.jar\"" 
	javaVMArguments = " -Dlog4j.configuration=log4j_mine.xml"
	javaCommand = " com.pb.tahoe.util.SkimConverter"
	RunProgram("cmd /c java" + javaClassPath + javaVMArguments + javaCommand,)
  
endMacro

//***********************************************************************************************


//***********************************************************************************************
///////////////////////////////////////////////////////////////////////////////////////////
//Traffic Assingment stuff below                                                         //
///////////////////////////////////////////////////////////////////////////////////////////

Macro "CreateTripMatrices"(pathArray)
    timePeriods = {"AM","MD","PM","LN"}
    modeSet = {"DA","SA","SH","WT","DT","NM","SB"}
    driveCoreSet = {"DA","SA"}
    
    for i = 1 to timePeriods.length do
          matrixName = "Trips_" + timePeriods[i]
          matrixFileName = pathArray[11] + matrixName + ".mtx"
          //open trip table and create matrix
          RunMacro("CreateMatrixFromCSV",pathArray[10] + matrixName + ".csv",
              matrixFileName, matrixName, "i", "j", modeSet)
          RunProgram("cmd /c del \"" + pathArray[10] + matrixName + ".DCC\"",)
          outMatrix = OpenMatrix(matrixFileName,)
          //add indices
          RunMacro("AddIndexToMatrix",matrixFileName,pathArray[1] + "fullstreets.DBD|Node","TAZ","ID")
          //Sum drive cores to get drive trips
          RunMacro("AddCoresToNewCore",outMatrix,matrixFileName,"Total Drive Trips",driveCoreSet,"Nodes","Nodes")
    end

    //Cleanup
    RunProgram("cmd /c del \"" + pathArray[10] + "*.DCC\"",)

endMacro

Macro "CreateMatrixFromCSV"(csvFile, matrixFile, matrixName, iIndex, jIndex, coreNames)
    tab = OpenTable("table", "CSV", {csvFile})
    m = CreateMatrixFromView(matrixName,
        "table|", iIndex, jIndex,
        coreNames,
        {{ "File Name", matrixFile },
        {"Type", "Float" },
        {"Sparse", "No" },
        {"Column Major", "No" },
        {"File Based", "Yes" }})
    CloseView(tab)
    RenameMatrix(m,matrixName)
endMacro

    
Macro "AddIndexToMatrix"(matrixFile,originsDataFile,oldIndexColumn,newIndexColumn)
    RunMacro("TCB Init")
    Opts = null
    Opts.Input.[Current Matrix] = matrixFile
    Opts.Input.[Index Type] = "Both"
    Opts.Input.[View Set] = {originsDataFile, "Centroids"}
    Opts.Input.[Old ID Field] = {originsDataFile, oldIndexColumn}
    Opts.Input.[New ID Field] = {originsDataFile, newIndexColumn}
    Opts.Output.[New Index] = "Nodes"
    ret_value = RunMacro("TCB Run Operation", 1, "Add Matrix Index", Opts)
endMacro

Macro "AddCoresToNewCore"(matrixHandle,matrixFile,newCore,oldCores,iIndex,jIndex)
    AddMatrixCore(matrixHandle, newCore)
    coreSum = ""
    for i = 1 to oldCores.length do
        if i = 1 then do
            coreSum = "[" + oldCores[i] + "]"
        end
        if i > 1 then do
            coreSum = coreSum + " + [" + oldCores[i] + "]"
        end
    end
    RunMacro("TCB Init")
        Opts = null
        Opts.Input.[Matrix Currency] ={matrixFile, newCore, iIndex, jIndex}
        Opts.Global.Method = 11
        Opts.Global.[Cell Range] = 2
        Opts.Global.[Expression Text] = coreSum
        Opts.Global.[Force Missing] = "Yes"
        ret_value = RunMacro("TCB Run Operation", 1, "Fill Matrices", Opts)
endMacro



Macro "TrafficAssignment"(pathArray,iterations)
    
    //Setup Highway Network
      RunMacro("TCB Init")
          Opts = null
          Opts.Input.Database = pathArray[1] + "fullstreets.DBD"
          Opts.Input.Network = pathArray[6] + "Tahoe_Drive_Network.net"
          Opts.Input.[Centroids Set] = {pathArray[1] + "fullstreets.DBD|Node", "Node", "Centroids", "Select * where TAZ > 0"}
          Opts.Field.[Link type] = "FC"
          //Opts.Flag.[Centroids in Network] = 1
          Opts.Flag.[Use Link Types] = "True"
          ret_value = RunMacro("TCB Run Operation", 1, "Highway Network Setting", Opts)
    
      timePeriods = {"AM","MD","PM","LN"}
      for i = 1 to timePeriods.length do
          RunMacro("RunAssignment",iterations, timePeriods[i], pathArray[1], pathArray[6], pathArray[12], pathArray[11])
      end
      
      directions = {"AB","BA"}
      assFields = {"Time","Flow","VMT","VOC"}
      fsFields = {"IVTT","Flow","VMT","VOC"}
      
      for i = 1 to timePeriods.length do
          for j = 1 to directions.length do
              for k = 1 to fsFields.length do
                  RunMacro("TransferData",directions[j],timePeriods[i],fsFields[k],assFields[k],pathArray[1],pathArray[12])
              end
          end
      end
      
      RunMacro("AggregateFlows",pathArray[1])
      
endMacro

Macro "RunAssignment" (iterations, period, fsPath, dnPath, outPath, matPath)
    RunMacro("TCB Init")
    Opts = null
    Opts.Input.Database = fsPath + "fullstreets.DBD"
    Opts.Input.Network = dnPath + "Tahoe_Drive_Network.net"
    //Opts.Input.[OD Matrix Currency] = {matPath + period + "_Trips.mtx", "Total Drive Trips", "Nodes", "Nodes"}
    Opts.Input.[OD Matrix Currency] = {matPath + "Trips_" + period + ".mtx", "Total Drive Trips", "Nodes", "Nodes"}
   // Opts.Field.[VDF Fld Names] = {"FF Travel Time", "[AB_Cap / BA_Cap]", "Alpha", "Beta", "Length", "Speed", "None"}
    Opts.Field.[VDF Fld Names] = {"FF Travel Time", "[AB_" + period + "_Cap / BA_" + period + "_Cap]", "Alpha", "Beta", "Length", "Speed", "None"}
    Opts.Global.Convergence = 0.0001
    //Opts.Global.Iterations = 50
    Opts.Global.Iterations = iterations
    Opts.Global.[Cost Function File] = "bpr.vdf"
    Opts.Global.[VDF Defaults] = {, , 0.15, 4, , , 0}
    Opts.Output.[Flow Table] = outPath + period + "_LinkFlow.bin"
    ret_value = RunMacro("TCB Run Procedure", 2, "Assignment", Opts)
endMacro

Macro "TransferData" (direction, period, fullStreetsField, assignmentResult, fullStreetsPath, resultsPath)
    RunMacro("TCB Init")
    Opts = null
    Opts.Input.[Dataview Set] = {{fullStreetsPath + "FULLSTREETS.DBD|FullStreets", resultsPath + period + "_LinkFlow.bin", "ID", "ID1"}, "FullStreets+" + period + "_LinkFlow"}
    Opts.Global.Fields = {direction + "_" + period + "_" + fullStreetsField}
    Opts.Global.Method = "Formula"
    Opts.Global.Parameter = direction + "_" + assignmentResult
    ret_value = RunMacro("TCB Run Operation", 1, "Fill Dataview", Opts)
endMacro

Macro "AggregateFlows" (fullStreetsPath)
    RunMacro("TCB Init")
    Opts = null
    Opts.Input.[Dataview Set] = {fullStreetsPath + "FULLSTREETS.DBD|FullStreets", "FullStreets"}
    Opts.Global.Fields = {"DailyVolume"}
    Opts.Global.Method = "Formula"
    Opts.Global.Parameter = "AB_AM_Flow + AB_MD_Flow + AB_PM_Flow + AB_LN_Flow + BA_AM_Flow + BA_MD_Flow + BA_PM_Flow + BA_LN_Flow"
    ret_value = RunMacro("TCB Run Operation", 1, "Fill Dataview", Opts)
endMacro

//***********************************************************************************************


///////////////////////////////////////////////////////////////////////////////////////////
//Java Resident and Visitor sub-model code below                                         //
///////////////////////////////////////////////////////////////////////////////////////////
Macro "SubModelRunner"(stepArray,pathArray,season,run_id,idx)
     ret_value=RunMacro("JavaSubModelCode",season,pathArray,run_id)
    if ret_value<>0 then do
        showMessage("Error in model run! Check logs for details")
        goto quit
    end
      
    ret_value = 0
    quit:
    return(!ret_value)

endMacro

Macro "ClearLastFile" (pathArray,scenarioName, summer)
    outputDirectory = pathArray[13] + "scenarios\\" + scenarioName + "\\outputs_winter\\"
    if summer = 1 then do
        outputDirectory = pathArray[13] + "scenarios\\" + scenarioName + "\\outputs_summer\\"
    end
    RunProgram("cmd /c del " + outputDirectory + "*.last",)
EndMacro

Macro "RunJavaModel" (pathArray,vmSize)
    javaClassPath = " -classpath \"" + pathArray[8] + ";" + 
        pathArray[8] + "log4j-1.2.9.jar;" + 
        pathArray[8] + "jxl.jar;" + 
        pathArray[8] + "censusdata.jar;" + 
        pathArray[8] + "common-base.jar;" + 
        pathArray[8] + "synpop.jar;" + 
        pathArray[8] + "tahoe.jar\"" 
    javaVMArguments = " -Dlog4j.configuration=log4j_mine.xml -Xms" + vmSize + "m -Xmx" + vmSize + "m"
    LaunchProgram("java" + javaClassPath + javaVMArguments + " com.pb.tahoe.util.TahoeModelComponentRunner TahoeModel")
EndMacro



