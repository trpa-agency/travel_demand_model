Macro "TahoeABModel"
    RunDbox("TahoeDbox",)
EndMacro

Macro "ResolvePath" (path)
    if Trim(path) = "" then do
        return(path)
    end
    p = ParseString(path,"\\")
    rp = null
    for i = 1 to p.length do
        if p[i] = ".." then do
            rp = Subarray(rp,1,rp.length-1)
        end
        else if p[i] <> "." then do
            rp = rp + {p[i]}
        end
    end
    s = ""
    for i = 1 to rp.length do
        if i > 1 then do
            s = s + "\\"
        end
        s = s + rp[i]
    end
    return(s + "\\")
EndMacro

Dbox "TahoeDbox" (tempStateArray) right, bottom , 50 , 31.5 title: "Tahoe Activity-Based Travel Demand Model" Toolbox NoKeyboard
    init do
        RunMacro("TCB Init")
        shared model_file, root_path, stateArray
		
        model_file = RunMacro("GetModelFilePath")
		root_path = RunMacro("GetRootPath")
		
		
        stateArray = RunMacro("LoadState")
        stateArray[1] = RunMacro("ResolvePath",stateArray[1])
        stateArray[3] = RunMacro("ResolvePath",stateArray[3])
        path = stateArray[1]
        basePath = stateArray[3]
        referencePath = basePath + "reference\\"
        javaPath = referencePath + "code\\"
		model_file = javaPath + "TahoeStateFile.txt"
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
        
        if not scenario_idx = null then do
            if not scenario_list[scenario_idx] = null and not scenario_list[scenario_idx] = "" then do
                savedMaps = RunMacro("loadMapArray",pathArray,pathArray[7] + "saved_maps.txt")
                savedMapNames = RunMacro("getMapNames",savedMaps)
            end
        end
        
        preFormedMaps = RunMacro("loadMapArray",pathArray,pathArray[8] + "pre_formed_maps.txt")
        preFormedMapNames = RunMacro("getMapNames",preFormedMaps)
              
        baseSearchPath = pathArray[13] + "scenarios\\"
        inputFiles = RunMacro("loadInputFiles",pathArray[8] + "input_files.txt")
        inputFileMap = RunMacro("getInputFileMapBySeason",inputFiles,season_idx)
        inputFileNames = RunMacro("getInputFileList",inputFiles,inputFileMap)
        
        reportFiles = RunMacro("loadInputFiles",pathArray[8] + "report_files.txt")
        reportFileNames = RunMacro("getFileNamesList",reportFiles)
        
        outputFiles = RunMacro("loadInputFiles",pathArray[8] + "output_files.txt")
        outputfiletype_idx = 1
        outputFileMap = RunMacro("getOutputFileMapByType",outputFiles,outputfiletype_idx)
        outputFileNames = RunMacro("getInputFileList",outputFiles,outputFileMap)
        outputFileTypes = {"All","Zonal","Resident","External Worker","Visitor","Assignment","Logs"}
        
        fileAssociations = RunMacro("getAssociations",pathArray,inputFiles)
        
        if not scenario_idx = null then do
            scenarioDescription = RunMacro("getScenarioDescription",pathArray,scenario_list[scenario_idx])
        end
        
        if not tempStateArray = null then do
            baseSearchPath = tempStateArray[1]
            tab_idx = tempStateArray[2]
            inputfile_idx = tempStateArray[3]
            outputfile_idx = tempStateArray[4]
            reportfile_idx = tempStateArray[5]
            time_period = tempStateArray[6]
            outputfiletype_idx = tempStateArray[7]
            mapfile_idx = tempStateArray[8]
            genmap_idx = tempStateArray[9]
        end
        
        //read in EMFAC info
        emfac_years = RunMacro("GetEmfacYears",pathArray)
        emfac_year_idx = null
    enditem

    close do
        RunMacro("CloseActions",path,season_idx,cr_user_iters,model_iters,scenario_list,scenario_idx,pathArray,vm_size)
        RunMacro("close everything")
        return()
    enditem

    //Logo: User should see the TRPA logo - this is used to minimize dbox   
    Button "icon_logo" 1, 1 icon: pathArray[13] + "reference\\img\\logoCrop.bmp" do
        //ShowMessage("Travel Model Sponsored by TRPA.  For more info see www.trpa.org")
        RunMacro("SaveActions",path,season_idx,cr_user_iters,model_iters,scenario_list,scenario_idx,pathArray,vm_size)
        //create temporary state array - this is a fixed definition, but so what
        tempStateArray = {baseSearchPath,tab_idx,inputfile_idx,outputfile_idx,reportfile_idx,time_period,outputfiletype_idx,mapfile_idx,genmap_idx}
        RunDbox("TahoeDboxMin",stateArray,tempStateArray)
        return()
    enditem


    //Scenario chooser: user will choose scenario.
    Text "Select Scenario" .7, 5.3
    Popdown Menu "Scenario Chooser" 1, 6.7, 35 List: scenario_list variable: scenario_idx do
        path = basePath + "scenarios\\" + scenario_list[scenario_idx] + "\\"
        pathArray = RunMacro("updatePath",basePath,path)
        RunMacro("updateproperty",referencePath,path,season_idx)
        if not scenario_idx = null and not scenario_list[scenario_idx] = null and not scenario_list[scenario_idx] = "" then do
            scenarioDescription = RunMacro("getScenarioDescription",pathArray,scenario_list[scenario_idx])
            savedMaps = RunMacro("loadMapArray",pathArray,pathArray[7] + "saved_maps.txt")
            savedMapNames = RunMacro("getMapNames",savedMaps)
            savedMapFile = null
            mapfile_idx = null
        end
    endItem
    
    //Delete a scenario: User can delete a scenario
    Button "Delete Scenario Button" 37.5, 5.1, 12, .9 prompt: "Delete Scenario" do
        if not scenario_idx = null and not scenario_list[scenario_idx] = null and not scenario_list[scenario_idx] = "" then do
            scenarioName = scenario_list[scenario_idx]
            newScenarioList = RunDbox("ScenarioDeleter",pathArray,scenario_list,scenarioName)
            if newScenarioList.length < scenario_list.length or newScenarioList[1] = null then do
                scenario_list = newScenarioList
                if scenario_list[1] = null then do
                    scenario_idx = null
                    path = null
                    pathArray = RunMacro("updatePath",basePath,path)
                    RunMacro("updateproperty",referencePath,path,season_idx)
                    scenario_file = OpenFile(pathArray[8] + "scenario_list.txt", "w")
                    writearray(scenario_file,scenario_list)
                    CloseFile(scenario_file)
                    scenarioDescription = RunMacro("getScenarioDescription",pathArray,"")
                    savedMaps = null
                    savedMapNames = null
                    savedMapFile = null
                end
                else do
                    if scenario_idx > scenario_list.length then do
                        scenario_idx = scenario_list.length
                    end
                    path = basePath + "scenarios\\" + scenario_list[scenario_idx] + "\\"
                    pathArray = RunMacro("updatePath",basePath,path)
                    RunMacro("updateproperty",referencePath,path,season_idx)
                    scenario_file = OpenFile(pathArray[8] + "scenario_list.txt", "w")
                    writearray(scenario_file,scenario_list)
                    CloseFile(scenario_file)
                    scenarioDescription = RunMacro("getScenarioDescription",pathArray,scenario_list[scenario_idx])
                    savedMaps = RunMacro("loadMapArray",pathArray,pathArray[7] + "saved_maps.txt")
                    savedMapNames = RunMacro("getMapNames",savedMaps)
                    savedMapFile = pathArray[7] + savedMaps[1][1]
                end
            end
        end
        
        RunMacro("SaveActions",path,season_idx,cr_user_iters,model_iters,scenario_list,scenario_idx,pathArray,vm_size)
        
    endItem
    
    //Create new scenario: User can create a new scenario
    Button "New Scenario Button" 37.5, 6.5, 12, 1.3 prompt: "New Scenario" do
        newScenarioList = RunDbox("NewScenarioCreator",pathArray,scenario_list)
        if newScenarioList.length > scenario_list.length then do
            scenario_list = newScenarioList
            scenario_idx = scenario_list.length
            path = basePath + "scenarios\\" + scenario_list[scenario_idx] + "\\"
            pathArray = RunMacro("updatePath",basePath,path)
            RunMacro("updateproperty",referencePath,path,season_idx)
            scenario_file = OpenFile(pathArray[8] + "scenario_list.txt", "w")
            writearray(scenario_file,scenario_list)
            CloseFile(scenario_file)
            scenarioDescription = RunMacro("getScenarioDescription",pathArray,scenario_list[scenario_idx])
            savedMaps = RunMacro("loadMapArray",pathArray,pathArray[7] + "saved_maps.txt")
            savedMapNames = RunMacro("getMapNames",savedMaps)
            savedMapFile = pathArray[7] + savedMaps[1][1]
        end
    endItem

    //Summer/Winter: User will select the season to which the model is applied.
    Radio List .6, 8.2, 49.5, 2.7 Prompt:"Season" Variable: season_idx
    Radio Button 8, 9.5 Prompt:"Summer" do
        value = RunMacro("updateproperty", referencePath,path,season_idx)
        inputFileMap = RunMacro("getInputFileMapBySeason",inputFiles,season_idx)
        inputFileNames = RunMacro("getInputFileList",inputFiles,inputFileMap)
        if not inputfile_idx = null then do
            inputFilePath = pathArray[13] + "scenarios\\" + scenario_list[scenario_idx] + "\\" + inputFiles[inputFileMap[inputfile_idx]][3] + inputFileNames[inputfile_idx]
            inputFileOpenCommand = Substitute(RunMacro("getCommand",inputFiles[inputFileMap[inputfile_idx]][2],fileAssociations),"%1",inputFilePath,)
        end
        if not reportfile_idx = null then do
            reportFilePath = pathArray[13] + "scenarios\\" + scenario_list[scenario_idx] + "\\outputs_summer\\reports\\" + reportFileNames[reportfile_idx]
            inputFileOpenCommand = Substitute(Substitute(RunMacro("getCommand",reportFiles[reportfile_idx][2],fileAssociations),"%1",reportFilePath,),"%2",reportFileNames[reportfile_idx],)
        end
    endItem
    Radio Button 31, 9.5 Prompt:"Winter" do
        value = RunMacro("updateproperty", referencePath,path,season_idx)
        inputFileMap = RunMacro("getInputFileMapBySeason",inputFiles,season_idx)
        inputFileNames = RunMacro("getInputFileList",inputFiles,inputFileMap)
        if not inputfile_idx = null then do
            inputFilePath = pathArray[13] + "scenarios\\" + scenario_list[scenario_idx] + "\\" + inputFiles[inputFileMap[inputfile_idx]][3] + inputFileNames[inputfile_idx]
            inputFileOpenCommand = Substitute(RunMacro("getCommand",inputFiles[inputFileMap[inputfile_idx]][2],fileAssociations),"%1",inputFilePath,)
        end
        if not reportfile_idx = null then do
            reportFilePath = pathArray[13] + "scenarios\\" + scenario_list[scenario_idx] + "\\outputs_winter\\reports\\" + reportFileNames[reportfile_idx]
            inputFileOpenCommand = Substitute(Substitute(RunMacro("getCommand",reportFiles[reportfile_idx][2],fileAssociations),"%1",reportFilePath,),"%2",reportFileNames[reportfile_idx],)
        end
    endItem

    //Hidden button
    Button "" 50, 31.05, .01, .01 do
        MessageBox("Hi",)
    endItem

    //Quit: User will press this button to exit
    Button "Quit" 15, 29.1, 20, 1.5 cancel do
        RunMacro("CloseActions",path,season_idx,cr_user_iters,model_iters,scenario_list,scenario_idx,pathArray,vm_size)
        RunMacro("close everything")
        Return()
    Enditem 
    
    //Generate the sub-tabs
    //Tab List 0.25,11.5,49.5,15.5  variable:tab_idx
    Tab List 0.25,11.5,49.5,17  variable:tab_idx

    Tab prompt:"Execution"
        
        //Scenario description
        Text "Scenario Description:" 1,1
        Text 1, 2.5, 45, 5 Framed Variable: scenarioDescription
        
        //Change scenario description button
        Button "Change..." 38,1,,.8 do
            RunDbox("RedescribeScenario",pathArray,scenario_list[scenario_idx],scenarioDescription)
            scenarioDescription = RunMacro("getScenarioDescription",pathArray,scenario_list[scenario_idx])
        enditem

        //Run model button
        Button "Run Model" 1, 9.8, 20, 2.6 do

            runIt = True
            if (RunMacro("TestIfRun2",pathArray,scenario_list[scenario_idx],season_idx) = 0) then do
                btn = MessageBox("The model has already been run for this scenario and season combination!\n" +
                                 "Re-running the model will cause all of the data stored in this scenario and\n" +
                                 "season to be permanently deleted and replaced with the new results.\nAre you sure you want to continue?",
                                {{"Caption", "Warning!"},{"Buttons", "YesNo"}})
                if btn = "No" then do
                    runIt = False
                end
                else do
                    //Clear log file
                    if season_idx = 1 then do
						if GetFileInfo(pathArray[13] + "scenarios\\" + scenario_list[scenario_idx] + "\\outputs_summer\\logs\\event.log") <> null then do
							DeleteFile(pathArray[13] + "scenarios\\" + scenario_list[scenario_idx] + "\\outputs_summer\\logs\\event.log")
						end // BK added this error handling, as In Version 6.0 added the NotFound error. 
                    end
                    else do
						if GetFileInfo(pathArray[13] + "scenarios\\" + scenario_list[scenario_idx] + "\\outputs_winter\\logs\\event.log") <> null then do
							DeleteFile(pathArray[13] + "scenarios\\" + scenario_list[scenario_idx] + "\\outputs_winter\\logs\\event.log")
						end // BK added this error handling, as In Version 6.0 added the NotFound error.
                    end
                end
            end
            if runIt then do
                RunMacro("PreModelRunner",pathArray,season_idx)
                RunMacro("SaveLinkData",pathArray,scenario_list[scenario_idx],season_idx,0)
                for i = 1 to model_iters do
                    RunMacro("ClearLastFile",pathArray,scenario_list[scenario_idx],season_idx)
                    RunMacro("RunJavaModel",pathArray,vm_size)
                    
                    //RunMacro("RunModelOnlyPopulationSynthesis",pathArray,vm_size)
                    //RunMacro("RunPopulationTransfer",pathArray,scenario_list[scenario_idx],season_idx)
                    //RunMacro("RunModelSkippingPopulationSynthesis",pathArray,vm_size)
                    
                    //RunMacro("WaitTimer",pathArray,scenario_list[scenario_idx],season_idx)
                    RunMacro("CreateTripMatrices",pathArray)
                    RunMacro("TrafficAssignment",pathArray,cr_user_iters)
                    RunMacro("MSAAssignmentResults",pathArray,scenario_list[scenario_idx],season_idx,i)
                    RunMacro("RunTransitSkims",pathArray,season_idx)
                    RunMacro("SaveLinkData",pathArray,scenario_list[scenario_idx],season_idx,i)
                    RunMacro("TripSummarizer",pathArray,scenario_list[scenario_idx],season_idx,i)
                end
                if season_idx = 1 then do
                    RunMacro("TransitAssignment","Summer",pathArray)
                end
                else do
                    RunMacro("TransitAssignment","Winter",pathArray)
                end
            end
            
            
           RunMacro("CreateTripMatrices",pathArray)
           if season_idx = 1 then do
               RunMacro("TransitAssignment","Summer",pathArray)
           end
           else do
               RunMacro("TransitAssignment","Winter",pathArray)
           end
           
        enditem
        
        //This is just an information area
        Radio List 22.5, 9, 25, 3.5 Prompt:"Iterations"
        Text 43, 10.2 Prompt: "Highway Assignment:"  Variable: IntToString(cr_user_iters) 
        Text 40, 11.3 Prompt: "Model Feedback:" Variable: IntToString(model_iters)

        //Help: User will press this button to open documentation
        Button "Documentation" 34.5, 8.3, 12, .75 do
            RunDbox("OpenDocumentationFile",pathArray)
        Enditem 
    
    Tab prompt:"GIS"
        
        //Available map files popdown
        //Text "Saved Maps" 1,.6
        Popdown Menu "Map File" 12,1,25 List: savedMapNames Prompt: "Saved Maps" Variable: mapfile_idx do
            savedMapFile = pathArray[7] + savedMaps[mapfile_idx][1]
        enditem
        
        Button "Open Map" 38.8,1,8.5,1 do
            if savedMapFile = null then do
                ShowMessage("Please select a saved map to open.")
            end 
            else do
                currentMap = OpenMap(savedMapFile,)
                if Lower(savedMaps[mapfile_idx][1]) = "tahoe.map" then do
                    SaveMap(currentMap,pathArray[7] + "temp_base.map")
                end
                currentMapDescription = savedMaps[mapfile_idx][3]
            end
        endItem
        
        //Description of saved maps
        Text "Description:" 1.5,2.4
        Text 1.5,3.4,45,2 Variable: savedMaps[mapfile_idx][3]
        
        //Avalailable pre-generated maps
        Popdown Menu "Generate Map" 16,6.5,19.5 List: preFormedMapNames Prompt: "Pre-Formed Maps" Variable: genmap_idx do
            preFormedMapMacro = preFormedMaps[genmap_idx][1]
        enditem
        Button "Create Map" 37.3,6.5,10,1 do
            if preFormedMapMacro = null then do
                ShowMessage("Please select a pre-formed map to create.")
            end
            else do
                //TODO: more of these maps
                seasonDescription = "Winter"
                if season_idx = 1 then do
                    seasonDescription = "Summer"
                end
                currentMapDescription = preFormedMaps[genmap_idx][3]
                RunMacro(preFormedMapMacro,pathArray,scenario_list[scenario_idx],seasonDescription)
            end
        endItem
        
        //Description of pre-formed maps
        Text "Description:" 1.5,7.6
        Text 1.5,8.6,45,2 Variable: preFormedMaps[genmap_idx][3]
        
        //a button to save the current map
        Button "Save Current Map" 26, 11.5, 20, 1.5 do
            currentMaps = GetMaps()
            if not currentMaps = null then do
                if currentMapDescription = null then do
                    currentMapDescription = ""
                end
                saveMapOut = RunDbox("SaveCurrentMap",pathArray,savedMaps,currentMapDescription)
                if saveMapOut[1] > 0 then do
                    savedMaps = saveMapOut[2] 
                    savedMapNames = RunMacro("getMapNames",savedMaps)
                    if saveMapOut[1] = 1 then do
                        mapfile_idx = savedMapNames.length
                    end 
                    else do
                        mapfile_idx = saveMapOut[1] - 1
                    end
                end
            end 
            else do
                ShowMessage("A map must be opened to save.")
            end
        enditem 
        
        //a button to rebuild base map
        Button "Rebuild Base Map" 2.5,11.5,20,1.5 do
            sure = MessageBox("Are you sure you would like to rebuild the base map?",{{"Caption", "Confirm Rebuild Base Map"},{"Buttons", "YesNo"}})
                if sure = "Yes" then do
                    SetCursor("HourGlass") 
                    RunMacro("GenerateMap",pathArray,scenario_list[scenario_idx])
                    ResetCursor()
                end
        enditem

    Tab prompt:"Input Files"
    
        //popdown list of all input files
        Popdown Menu "Input File" 10, 1.5, 35 Prompt: "Input File" List: inputFileNames variable: inputfile_idx do
            inputFilePath = pathArray[13] + "scenarios\\" + scenario_list[scenario_idx] + "\\" + inputFiles[inputFileMap[inputfile_idx]][3] + inputFileNames[inputfile_idx]
            inputFileOpenCommand = Substitute(Substitute(RunMacro("getCommand",inputFiles[inputFileMap[inputfile_idx]][2],fileAssociations),"%1",inputFilePath,),"%2",inputFileNames[inputfile_idx],)
        endItem
    
        //a description of the currently selected input file
        Text "Description:" 2, 3.5
        Text 2, 4.5, 45, 6 Variable: inputFiles[inputFileMap[inputfile_idx]][5] 
        
        //a button to view the absolute path to the input file
        Button "Path to File" 1, 11, 10, 2 do
            if not inputfile_idx = null then do
                MessageBox(inputFilePath,{{"Caption", "Absolute Path to " + inputFileNames[inputfile_idx]}})
            end
            else do
                ShowMessage("Please select an input file")
            end
        enditem
        
        //a button to open the absolute path to the input file
        Button "Open File" 14, 11, 20, 2 do
            //ShowMessage(inputFileOpenCommand)
            if not inputfile_idx = null then do
                SetCursor("HourGlass")
                LaunchProgram(inputFileOpenCommand)
                //LaunchDocument(inputFilePath,)
                ResetCursor()
            end
            else do
                ShowMessage("Please select an input file")
            end
        enditem
    
        //a button to import the file from another scenario
        Button "Import File..." 37, 11, 10, 2 do
            if not inputfile_idx = null then do
                baseSearchPath = RunDbox("ImportFile",pathArray,scenario_list,scenario_list[scenario_idx],inputFileNames[inputfile_idx],inputFiles[inputFileMap[inputfile_idx]][3],baseSearchPath)
            end
        enditem
        
    Tab prompt:"Summaries"

        //Create model summaries
        Button "       Model Summary" 1,1.6,21,2 do
            SetItem("Model Summary")
            SetCursor("HourGlass")
            if (RunMacro("TestIfRun",pathArray,scenario_list[scenario_idx],season_idx) = 0) then do
                if season_idx = 1 then do
                    LaunchProgram(pathArray[8] + "\\TextViewer\\TextViewer " + pathArray[13] + "scenarios\\" + scenario_list[scenario_idx] + "\\outputs_summer\\reports\\modelSummary.txt -f fix -t \"Model Summary [" + scenario_list[scenario_idx] + "]\"")
                end
                else do
                    LaunchProgram(pathArray[8] + "\\TextViewer\\TextViewer " + pathArray[13] + "scenarios\\" + scenario_list[scenario_idx] + "\\outputs_winter\\reports\\modelSummary.txt -f fix -t \"Model Summary [" + scenario_list[scenario_idx] + "]\"")
                end
            end
            ResetCursor()
        endItem
        Button "Model Summary" 1.5, 1.9 icon: pathArray[13] + "reference\\img\\DVW.bmp" 

        //Create assignment summaries
        Button "        Assignment Summary" 25,1.6,22,2 do
            SetItem("Assignment Summary")
            SetCursor("HourGlass")
            if (RunMacro("TestIfRun",pathArray,scenario_list[scenario_idx],season_idx) = 0) then do
                //RunMacro("GenerateAssignmentSummary",pathArray,season_idx,scenario_list[scenario_idx],asgn_type)
                RunMacro("GenerateAssignmentSummary",pathArray,season_idx,scenario_list[scenario_idx],6)
            end
            ResetCursor()
        endItem
        Button "Assignment Summary" 25.5, 1.9 icon: pathArray[13] + "reference\\img\\FIG.bmp" 
    
        //Assignment summary selector
        //Popdown Menu "Assignment Chooser" 35, 4.2, 11 Prompt: "Link Class" List: {"Total","Principle Arterial","Minor Arterial","Collector","Centroid","All"} variable: asgn_type
        
        
        //popdown list of all report files
        Text "Report Files" 1, 5
        Popdown Menu "Report Files" 1, 6.5, 34  List: reportFileNames variable: reportfile_idx do
            reportFilePath = pathArray[13] + "scenarios\\" + scenario_list[scenario_idx] + "\\outputs_winter\\reports\\" + reportFileNames[reportfile_idx]
            if season_idx = 1 then do
                reportFilePath = pathArray[13] + "scenarios\\" + scenario_list[scenario_idx] + "\\outputs_summer\\reports\\" + reportFileNames[reportfile_idx]
            end
            inputFileOpenCommand = Substitute(Substitute(RunMacro("getCommand",reportFiles[reportfile_idx][2],fileAssociations),"%1",reportFilePath,),"%2",reportFileNames[reportfile_idx],)
            //ShowMessage(reportFilePath + "\n" + inputFileOpenCommand)
        endItem
    
        //a description of the currently selected input file
        Text "Description:" 2, 8.5
        Text 2, 9.5, 45, 4 Variable: reportFiles[reportfile_idx][5]
        
        //a button to open the file
        Button "Open Report" 36.8, 6.5, 10.5 do
            if reportfile_idx = null then do
                ShowMessage("Please select a report file to open.")
            end
            else do
                if (RunMacro("TestIfRun",pathArray,scenario_list[scenario_idx],season_idx) = 0) then do
                    LaunchProgram(inputFileOpenCommand)
                end
            end
        enditem
    
    Tab prompt:"Output Files"
    
        //Open trip tables
        Button "       Open Trip Tables" 1.4,1,22,2 do
            SetCursor("HourGlass")
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
                CopyFile(pathArray[11] + "Trips_" + timePeriodGroup[time_period] + ".mtx",pathArray[11] + "Temp_Trips" + timePeriodGroup[time_period] + ".mtx")
                m = OpenMatrix(pathArray[11] + "Temp_Trips" + timePeriodGroup[time_period] + ".mtx",)
                periodList = {"AM Peak","Midday","PM Peak","Overnight"}
                RenameMatrix(m,periodList[time_period] + " Trips")
                RunMacro("RenameMatrixCores",pathArray[11] + "Temp_Trips" + timePeriodGroup[time_period] + ".mtx")
                editorName = CreateMatrixEditor(periodList[time_period] + " Trips",m,b)
            end
            ResetCursor()
        endItem
        Button "Open Trip Tables" 1.9, 1.3 icon: pathArray[13] + "reference\\img\\MTX.bmp" 

        //Open trip table time period selector
        Popdown Menu "Time Period Chooser" 35.5, 1.5, 11 Prompt: "Trip Period" List: {"AM Peak","Midday","PM Peak","Overnight"} variable: time_period

        //popdown list of all output files
        Text "Output Files" 1, 4
        Popdown Menu "Output Files" 1, 5.5, 34  List: outputFileNames variable: outputfile_idx do
            seasonValue = "winter"
            if season_idx = 1 then do
                seasonValue = "summer"
            end
            outputFilePath = Substitute(pathArray[13] + "scenarios\\" + scenario_list[scenario_idx] + "\\" + outputFiles[outputFileMap[outputfile_idx]][3] + outputFileNames[outputfile_idx],"%%SEASON%%",seasonValue,)
            outputFileOpenCommand = Substitute(Substitute(RunMacro("getCommand",outputFiles[outputFileMap[outputfile_idx]][2],fileAssociations),"%1",outputFilePath,),"%2",outputFileNames[outputfile_idx],)
            //ShowMessage(reportFilePath + "\n" + inputFileOpenCommand)
        endItem
        
        //popdown list of output file types
        Text "Category" 36.5, 4
        Popdown Menu "Output File Type" 36.5, 5.5, 10  List: outputFileTypes variable: outputfiletype_idx do
            outputFileMap = RunMacro("getOutputFileMapByType",outputFiles,outputfiletype_idx)
            outputFileNames = RunMacro("getInputFileList",outputFiles,outputFileMap)
            if not outputfiletype_idx = oldOutputfiletype_idx then do
                outputfile_idx = null
            end
            oldOutputfiletype_idx = outputfiletype_idx
        enditem
    
        //a description of the currently selected output file
        Text "Description:" 2, 7.5
        Text 2, 8.5, 45, 2 Variable: outputFiles[outputFileMap[outputfile_idx]][5]
        
        //a button to open the file
        Button "Open File" 31.1, 11.6, 15.5, 1.5 do
            if outputfile_idx = null then do
                ShowMessage("Please select an output file to open.")
            end
            else do
                if (RunMacro("TestIfRun",pathArray,scenario_list[scenario_idx],season_idx) = 0) then do
                    //LaunchProgram(inputFileOpenCommand)
                    if outputFiles[outputFileMap[outputfile_idx]][2] = ".bin" then do
                        CreateEditor(outputFiles[outputFileMap[outputfile_idx]][1],OpenTable(outputFiles[outputFileMap[outputfile_idx]][1],"FFB",{outputFilePath,}) + "|",,)
                    end
                    else do
                        LaunchProgram(outputFileOpenCommand)
                    end
                end
            end
        enditem
    
    Tab prompt:"EMFAC"
        Text 2,1,45,2 variable: "From this control you may run the EMFAC 2007 emissions model using the results of the TRPA travel demand model."
        
        Popdown Menu "EmfacYear" 20,4.5,20 prompt: "Model (EMFAC) Year" list: emfac_years variable: emfac_year_idx
        
        Button "Run Emfac" 8,7.5,30,2 do
            if scenario_idx = null then do
                ShowMessage("Please select scenario.")
            end
            else do
                if season_idx = 1 then do
                    trip_file = pathArray[13] + "scenarios\\" + scenario_list[scenario_idx] + "\\outputs_summer\\trip_file.csv"
                end
                else do
                    trip_file = pathArray[13] + "scenarios\\" + scenario_list[scenario_idx] + "\\outputs_winter\\trip_file.csv"
                end
                if GetFileInfo(trip_file) = null then do
                    ShowMessage("Must run travel model before running EMFAC.")
                end
                else do
                    if emfac_year_idx = null then do
                        ShowMessage("Please select model/EMFAC year.")
                    end
                    else do
                        RunMacro("RunTahoeEmfacAQuaVis",pathArray,season_idx,scenario_list[scenario_idx],emfac_years[emfac_year_idx])
                    end
                end
            end
        EndItem
EndDbox

//mini dbox used for minimizing the interface
Dbox "TahoeDboxMin" (stateArray,tempStateArray) right, bottom , 50 , 4.7 title: "Tahoe Activity-Based Travel Demand Model" Toolbox NoKeyboard
    init do
        RunMacro("TCB Init")
        stateArray = RunMacro("LoadState")
        pathArray = RunMacro("updatePath",stateArray[3],stateArray[1])
    enditem

    Button "icon_logo" 1, 0.5 icon: pathArray[13] + "reference\\img\\logoCrop.bmp" do
        RunDbox("TahoeDbox",tempStateArray)
        return()
    enditem
EndDBox

Dbox "OpenDocumentationFile" (pathArray) 40, 7 title: "Open Documenation"
    init do
        RunMacro("TCB Init")
        f = OpenFile(pathArray[8] + "doc_files.txt","r")
        a = ReadArray(f)
        CloseFile(f)
        desc = ""
        docArray = {}
        docFileArray = {}
        for i = 1 to a.length do
            p = Position(a[i],"%%")
            if i = 1 then do
                docArray = {SubString(a[i],0,p - 1)}
                docFileArray = {SubString(a[i],p + 2,)}
            end
            else do
                docArray = InsertArrayElements(docArray,docArray.length + 1,{SubString(a[i],0,p - 1)})
                docFileArray = InsertArrayElements(docFileArray,docFileArray.length + 1,{SubString(a[i],p + 2,)})
            end
        end
    enditem
    
    Text "Choose a documenation file to open" 1,0.5 
    
    Popdown Menu "" 1,1.7,35 List: docArray variable: doc_idx do
       
    enditem
    
    Button "Open" 3,4,12,1.5 do
        if doc_idx = null then do
            ShowMessage("Please select a document to open.")
        end
        else do
            LaunchProgram("cmd /c start " + pathArray[13] + docFileArray[doc_idx])
            return()
        end
    enditem
    
    Button "Cancel" 22,4,12,1.5 do
        return()
    enditem
EndDBox 

Dbox "ImportFile" (pathArray,scenarioList,scenarioName,inputFileName,relativePathExtension,baseSearchPath) center,center, 52, 9 title: "Import File"
    init do
        //RunMacro("TCB Init")
        headText = "Import \"" + inputFileName + "\""
        subScenarioArray = CopyArray(scenarioList)
        elementToRemove = 0
        for i = 1 to subScenarioArray.length do
            if subScenarioArray[i] = scenarioName then do
                elementToRemove = i
            end
        end
        import_all_idx = 0
        subScenarioArray = ExcludeArrayElements(subScenarioArray,elementToRemove,1)
        baseInputFilePath = pathArray[13] + "scenarios\\" + scenarioName + "\\" + relativePathExtension + inputFileName
        sureText = "This will replace " + inputFileName + " in the " + scenarioName + " scenario! \n" + 
                "Data in the current file will be replaced permenantly. This cannot be undone. \n" + 
                "Are you sure you want to continue?"
        sureAllText = "This will replace all input files in the " + scenarioName + " scenario! \n" + 
                "All data in the current file will be lost and this cannot be undone. \n" + 
                "Are you sure you want to continue?"
        
    enditem
    
    close do
        return(baseSearchPath)
    enditem
    
    //Just an information bar
    Text 1,0.5 variable: headText
    
    //Scenario chooser: user will choose scenario.
    Text "Select Scenario to Import File From" 1, 2.3
    Popdown Menu "Scenario Chooser" 1, 3.7, 35 List: subScenarioArray variable: subscenario_idx do
        newInputFilePath = pathArray[13] + "scenarios\\" + subScenarioArray[subscenario_idx] + "\\" + relativePathExtension + inputFileName
    endItem
    
    //import file button
    Button "Import File" 37.5, 3.6, 14, 1.2 do
        if subscenario_idx = null then do
            ShowMessage("Please select a scenario to import file from.")
        end
        else if import_all_idx = 1 then do
            sure = MessageBox(sureAllText,{{"Caption", "Warning!"},{"Buttons", "YesNo"}})
            if sure = "Yes" then do
                sourceFilePath = Substitute(newInputFilePath,inputFileName,"",)
                endFilePath = Substitute(baseInputFilePath,inputFileName,"",)
                //ShowMessage(sourceFilePath + "\n" + endFilePath)
                RunProgram("cmd /c copy /Y " + sourceFilePath + "*.csv " +  endFilePath,)
                return(baseSearchPath)
            end
        end
        else do
            //ShowMessage(baseInputFilePath + "\n" + newInputFilePath)
            sure = MessageBox(sureText,{{"Caption", "Warning!"},{"Buttons", "YesNo"}})
            if sure = "Yes" then do
                CopyFile(newInputFilePath,baseInputFilePath)
                return(baseSearchPath)
            end
        end
    enditem
    
    //Import all files check box
    Checkbox 37, 5.3 Prompt: "Import All Files" Variable: import_all_idx do
    
    enditem
    
    //Browse for file button
    Button "Browse for file..."1,7.3,20,1.2 do
        on escape do
            goto escaper
        end
        newInputFilePath = ChooseFile({{"Input File", inputFileName}},
                "Select Input File to Import", 
                {{"Initial Directory",baseSearchPath},{"Read Only",False}})
                //apparently "Read Only" doesn't do anything, but I don't want it there
	    baseSearchPath = Substitute(newInputFilePath,inputFileName,"",)
        sure = MessageBox(sureText,{{"Caption", "Warning!"},{"Buttons", "YesNo"}})
        if sure = "Yes" then do
            CopyFile(newInputFilePath,baseInputFilePath)
            return(baseSearchPath)
        end
        escaper: on escape default
    enditem
    
    //Cancel button
    Button "Cancel" 31,7.3,20,1.2 do
        return(baseSearchPath)
    enditem
EndDBox




Macro "GetEmfacYears" (pathArray)
    Dim years[46]
    for i = 1 to 46 do
        years[i] = i2s(1989 + i)
    end
    return(years)
EndMacro

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
    //f = OpenFile(outputDirectory + "FullStreets_iter3.bin","r")
    f = OpenFile(outputDirectory + "TripSynthesize.last","r")
    CloseFile(f)
    return(0)
EndMacro

Macro "TestIfRun2" (pathArray,scenarioName,summer)
    on notfound do
        on notfound default
        return(1)
    end
    outputDirectory = pathArray[13] + "scenarios\\" + scenarioName + "\\outputs_winter\\"
    if summer = 1 then do
        outputDirectory = pathArray[13] + "scenarios\\" + scenarioName + "\\outputs_summer\\"
    end
    //f = OpenFile(outputDirectory + "FullStreets_iter3.bin","r")
    f = OpenFile(outputDirectory + "TripSynthesize.last","r")
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
    RunMacro("TCB Run Operation", 1, "Rename Matrix Core", Opts) // JF: modified the GISDK file to handle the new modes and also update the base trip files input to the 0th iteration of the model to make them consistent 
    Opts.Input.[Target Core] = "WK" 
    Opts.Input.[Core Name] = "Walk" 
    Opts.Input.[Target Core] = "BK" 
    Opts.Input.[Core Name] = "Bike" 
    RunMacro("TCB Run Operation", 1, "Rename Matrix Core", Opts) 
endMacro

Macro "LoadState"
	shared model_file,root_path
    on notfound do
        stateArray = RunMacro("configure_macro",root_path)
        on notfound default
        return(stateArray)
    end
    backupFile = OpenFile(model_file,"r")
    stateArray = ReadArray(backupFile)
    CloseFile(backupFile)
    stateArray[2] = StringToInt(stateArray[2])
    stateArray[4] = StringToInt(stateArray[4])
    stateArray[5] = StringToInt(stateArray[5])
    stateArray[6] = StringToInt(stateArray[6])
    return(stateArray)
EndMacro

Macro "CloseActions" (scenarioPath,season_idx,cr_user_iters,model_iters,scenario_list,scenario_idx,pathArray,vm_size)
    RunMacro("SaveActions",scenarioPath,season_idx,cr_user_iters,model_iters,scenario_list,scenario_idx,pathArray,vm_size)
    RunProgram("cmd /c del " + pathArray[11] + "Temp_Trips*.mtx",)
EndMacro

Macro "SaveActions" (scenarioPath,season_idx,cr_user_iters,model_iters,scenario_list,scenario_idx,pathArray,vm_size)
    shared stateArray, model_file
    stateArray[1] = scenarioPath
    stateArray[2] = season_idx
    stateArray[4] = cr_user_iters
    stateArray[5] = model_iters
    stateArray[6] = scenario_idx
    stateArray[7] = vm_size
    backupFile = OpenFile(model_file,"w")
    WriteArray(backupFile,stateArray)
    CloseFile(backupFile)
    scenario_file = OpenFile(pathArray[8] + "scenario_list.txt", "w")
    writearray(scenario_file,scenario_list)
    CloseFile(scenario_file)
EndMacro
// BK: The path to the bike/walk layer, network and TAZ is defined here
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
	// BK: add the path to the bike line layer [14], bike network [15], and TAZ layer [16]
	bikeLayerPath = scenarioPath + "gis\\Layers\\Streets\\" 
	bikeNetworkPath = scenarioPath + "gis\\Networks\\"
	tazLayerPath = scenarioPath + "gis\\Layers\\TAZ\\"
    pathArray = {streetLayerPath, transitNetworkPath, transitRoutesPath, modeTablePath, outputPath, networkPath, mapPath,
				javaPath, externalDistanceMatrixPath, csvTripTablePath, tripMatrixPath, assignmentOutputPath, basePath, bikeLayerPath, bikeNetworkPath, tazLayerPath}
    return(pathArray)
EndMacro

Dbox "configure"
    init do
        opt_idx = 0
    enditem
    Text 1, 1 Variable: "To configure the model runner, choose the path to the TahoeModel directory"
    Text 1, 2 Variable: " (e.g. C:\\TRPA\\TahoeModel\\)"
    Button "Browse..." 18, 4 , 15, 1 do
        path = ChooseDirectory("Browse to and choose TahoeModel directory", )
		dim stateArray[7]
		stateArray[1] = path + "\\scenarios\\dummy_scenario"					// default scenario name
		stateArray[2] = 1                                                   // season id. summer - 1, winter - 2
		stateArray[3] = path + "\\"                                         // model directory
		stateArray[4] = 50                                                  // number of assignment iterations
		stateArray[5] = 3                                                   // model iterations (feedback loop)
		stateArray[6] = 1                                                   // index of scenario name in scenario list
		stateArray[7] = "1000m"                                             // vm size
		
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


Macro "configure_macro" (root_path)

	path = root_path
	dim stateArray[7]
	stateArray[1] = path + "\\scenarios\\dummy_scenario"					// default scenario name
	stateArray[2] = 1                                                   // season id. summer - 1, winter - 2
	stateArray[3] = path + "\\"                                         // model directory
	stateArray[4] = 50                                                  // number of assignment iterations
	stateArray[5] = 3                                                   // model iterations (feedback loop)
	stateArray[6] = 1                                                   // index of scenario name in scenario list
	stateArray[7] = "1000m"                                             // vm size
	
	tcLogxmlPath = path + "\\reference\\"
	logxmlPath = RunMacro("GenerifyPath",tcLogxmlPath)
	RunMacro("TemplateToFile",tcLogxmlPath + "code\\" + "log4j_mine_noScenario.template.xml",tcLogxmlPath + "code\\" + "log4j_mine_noScenario.xml",logxmlPath,"empty",0)
	return(stateArray)
EndMacro




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

//this loads the scenario description, if there is one
Macro "getScenarioDescription" (pathArray,scenarioName)
    on notfound do
        on notfound default
        //return("None. (Click \"Change...\" button to add a description)")
        return("<None>")
    end
    f = OpenFile(pathArray[13] + "scenarios\\" + scenarioName + "\\ScenarioDescription.txt","r")
    l = ReadLine(f)
    CloseFile(f)
    return(l)
EndMacro

//this records the scenario description
Macro "recordScenarioDescription" (pathArray,scenarioName,description)
    f = OpenFile(pathArray[13] + "scenarios\\" + scenarioName + "\\ScenarioDescription.txt","w")
    WriteLine(f,description)
    CloseFile(f)
EndMacro

//This allows one to change the scenario description
Dbox "RedescribeScenario" (pathArray,scenarioName,currentDescription) title: "Change Scenario Description"
    init do
        opt_idx = 0
        scenarioDescription = currentDescription
    enditem
    Text "Scenario Description" 1.5, 0.6
    Edit Text 2, 2, 45, 5 
        variable: scenarioDescription
    Button "OK" 2,8,15.5,2 do
        sure = MessageBox("Are you sure you want to change the scenario description?",{{"Caption", "Confirm"},{"Buttons", "YesNo"}})
        if sure = "Yes" then do
            RunMacro("recordScenarioDescription",pathArray,scenarioName,scenarioDescription)
            return()
        end
    endItem
    
    Button "Cancel" 30.5,8,15.5,2 do
        return()
    endItem
EndDbox

Macro "loadMapArray" (pathArray,inputFile)
    //if map file doesn't exist, create it
    on notfound do
        on notfound default
        f = OpenFile(pathArray[7] + "saved_maps.txt","w")
        WriteArray(f,{"Tahoe.map%%ONE%%Base Map%%TWO%%Base scenario map."})
        CloseFile(f)
        return(RunMacro("loadMapArray",pathArray,pathArray[7] + "saved_maps.txt"))
    end
    f = OpenFile(inputFile,"r")
    inputs = ReadArray(f)
    CloseFile(f)
    space1 = "%%ONE%%"
    space2 = "%%TWO%%"
    mapArray = {}
    for i = 1 to inputs.length do
        line = inputs[i]
        subArray = {{,,}}
        p1 = Position(line,space1)
        p2 = Position(line,space2)
        subArray[1][1] = SubString(line,0,p1 - 1)
        subArray[1][2] = SubString(line,p1 + Len(space1),p2 - p1 - Len(space1))
        subArray[1][3] = SubString(line,p2 + Len(space2),)
        if i = 1 then do
            mapArray[1] = subArray[1]
        end 
        else do
            mapArray = InsertArrayElements(mapArray,mapArray.length + 1,subArray)
        end
    end
    on notfound default
    return(mapArray)
EndMacro

Macro "getMapNames" (mapArray)
    mapNames = {}
    for i = 1 to mapArray.length do
        if i = 1 then do
            mapNames[i] = mapArray[i][2]
        end
        else do
            mapNames = InsertArrayElements(mapNames,mapNames.length + 1, {mapArray[i][2]})
        end
    end
    return(mapNames)
EndMacro

Dbox "SaveCurrentMap" (pathArray,savedMaps,mapDescription) title: "Save map"
    init do
        nameOK = 0
    enditem
    
    Text "Enter Map Name" 1.5,.6
    Edit Text 2, 2, 20
        variable: mapName
    Text "Map Description" 1.5, 3.6
    Edit Text 2, 5, 45, 2 
        variable: mapDescription
    Button "OK" 2,9,15.5,2 do
        SetCursor("HourGlass")
        mapFileName = Substitute(Substitute(mapName," ","_",),".map","",) + ".map"
        nameOK = 1
        if Lower(mapFileName) = "tahoe.map" then do
            ResetCursor()
            ShowMessage("Cannot replace base map!")
            nameOK = 0
        end
        else do
            for i = 1 to savedMaps.length do
                if Lower(mapFileName) = Lower(savedMaps[i][1]) then do
                    ResetCursor()
                    replace = MessageBox("Map already exists! Overwrite?",{{"Caption", "Warning!"},{"Buttons", "YesNo"}})
                    if replace = "No" then do
                        nameOK = 0
                    end  
                    else do
                        nameOK = 1 + i
                    end
                    SetCursor("HourGlass")    
                end
            end
        end
        if nameOK > 0 then do
            SaveMap(,pathArray[7] + mapFileName)
            if nameOK = 1 then do
                savedMaps = InsertArrayElements(savedMaps,savedMaps.length + 1,{{mapFileName,mapName,mapDescription}})
                f = OpenFile(pathArray[7] + "saved_maps.txt","a")
                WriteLine(f,mapFileName + "%%ONE%%" + mapName + "%%TWO%%" + mapDescription)
                CloseFile(f)
            end
            else do
                //have to rebuild array and map file
                savedMaps = ExcludeArrayElements(savedMaps,nameOK - 1,1)
                savedMaps = InsertArrayElements(savedMaps,nameOK - 1,{{mapFileName,mapName,mapDescription}})
                f = OpenFile(pathArray[7] + "saved_maps.txt","r")
                a = ReadArray(f)
                CloseFile(f)
                a = ExcludeArrayElements(a,nameOK - 1,1)
                a = InsertArrayElements(a,nameOK - 1,{mapFileName + "%%ONE%%" + mapName + "%%TWO%%" + mapDescription})
                f = OpenFile(pathArray[7] + "saved_maps.txt","w")
                WriteArray(f,a)
                CloseFile(f)
            end
            ResetCursor()
            return({nameOK,savedMaps})
        end
        ResetCursor()
    enditem
    
    Button "Cancel" 30.5,9,15.5,2 do
        return({nameOK,savedMaps})
    endItem
EndDbox

Macro "loadInputFiles" (inputFile)
    f = OpenFile(inputFile, "r")
    inputs = ReadArray(f)
    CloseFile(f)
    fileArray = {}
    space1 = "%%ONE%%"
    space2 = "%%TWO%%"
    space3 = "%%THREE%%"
    space4 = "%%FOUR%%"
    for i = 1 to inputs.length do
        line = Substitute(Substitute(Substitute(Substitute(inputs[i]," ",space1,1)," ",space2,1)," ",space3,1)," ",space4,1)
        p1 = Position(line,space1)
        p2 = Position(line,space2)
        p3 = Position(line,space3)
        p4 = Position(line,space4)
        subArray = {{,,,,}}
        subArray[1][1] = SubString(line,0,p1 - 1)
        subArray[1][2] = SubString(line,p1 + Len(space1),p2 - p1 - Len(space1))
        subArray[1][3] = SubString(line,p2 + Len(space2),p3 - p2 - Len(space2))
        subArray[1][4] = StringToInt(SubString(line,p3 + Len(space3),p4 - p3 - Len(space3)))
        subArray[1][5] = SubString(line,p4 + Len(space4),)
        if i = 1 then do
            fileArray[1] = subArray[1]
        end 
        else do
            fileArray = InsertArrayElements(fileArray,fileArray.length + 1,subArray)
        end
    end
    return(fileArray)
EndMacro

Macro "getInputFileMapBySeason" (inputFiles,season_idx)
    season = 2
    if season_idx = 1 then do
        season = 1
    end
    inputFileMap = {}
    first = True
    for i = 1 to inputFiles.length do
        if inputFiles[i][4] = 0 or inputFiles[i][4] = season then do
            if first then do
               inputFileMap[1] =  i
               first = False
            end
            else do
                inputFileMap = InsertArrayElements(inputFileMap,inputFileMap.length + 1,{i})
            end
        end
    end
    return(inputFileMap)
EndMacro

Macro "getOutputFileMapByType" (outputFiles,outputfiletype_idx)
    outputFileMap = {}
    first = True
    for i = 1 to outputFiles.length do
        if outputfiletype_idx = 1 or outputFiles[i][4] = outputfiletype_idx then do
            if first then do
                outputFileMap[1] = i
                first = False
            end
            else do
                outputFileMap = InsertArrayElements(outputFileMap,outputFileMap.length + 1, {i})
            end
        end
    end
    return(outputFileMap)
EndMacro

Macro "getInputFileList" (inputFiles,inputFileMap)
    inputFileList = {inputFiles[inputFileMap[1]][1] + inputFiles[inputFileMap[1]][2]}
    for i = 2 to inputFileMap.length do
        inputFileList = InsertArrayElements(inputFileList,inputFileList.length + 1, {inputFiles[inputFileMap[i]][1] + inputFiles[inputFileMap[i]][2]})
    end
    return(inputFileList)
EndMacro

Macro "getFileNamesList" (files)
    fileNameList = {files[1][1] + files[1][2]}
    for i = 2 to files.length do
        fileNameList = InsertArrayElements(fileNameList,fileNameList.length + 1, {files[i][1] + files[i][2]})
    end
    return(fileNameList)
EndMacro

//This macro gets a series of file associations - it links extensions to the program to run
Macro "getAssociations" (pathArray,inputFiles) 
    //initialize association array
    //fileAssociations = {{inputFiles[1][2],}}
    //for i = 2 to inputFiles.length do
    //    test = True
    //    for j = 1 to fileAssociations.length do
    //        if inputFiles[i][2] = fileAssociations[j][1] then do
    //            test = False
    //        end
    //    end
    //    if test then do
    //        fileAssociations = InsertArrayElements(fileAssociations,fileAssociations.length + 1,{{inputFiles[i][2],}})
    //    end
    //end
    //for i = 1 to fileAssociations.length do
    //    fileAssociations[i][2] = RunMacro("getFileAssociation",pathArray,fileAssociations[i][1])
    //end
    ////New way - implies default unless other use used////
    fileAssociations = {{".csv","cmd /c start %1"},
                        {".txt",pathArray[8] + "\\TextViewer\\TextViewer %1 -f fix -t \"%2\""},
                        {".pam","cmd /c start %1"},
                        {".log",pathArray[8] + "\\TextViewer\\TextViewer %1 -f fix -t \"%2\""},
                        {".bin","NOTHING"}}
    return(fileAssociations)
EndMacro

Macro "getFileAssociation" (pathArray,type)
    tempFile = pathArray[8] + "fileAssociations.tmp"
    RunProgram("cmd /c assoc " + type + " > " + tempFile,)
    f = OpenFile(tempFile,"r")
    line = ReadLine(f)
    CloseFile(f)
    line = Substitute(line,type + "=","",)
    //if no file association for type then make it a text file
    if Position(line,"not found") > 0 then do
        line = "txtfile"
    end
    RunProgram("cmd /c ftype " + line + " > " + tempFile,)
    f = OpenFile(tempFile,"r")
    line2 = ReadLine(f)
    CloseFile(f)
    if Position(line2,"%1") = 0 then do
        line2 = line2 + " %1"
    end
    line2 = Substitute(line2,line + "=","",)
    DeleteFile(tempFile)
    return(line2)
EndMacro

Macro "getCommand" (type,fileAssociations)
    for i = 1 to fileAssociations.length do
        if type = fileAssociations[i][1] then do
            return(fileAssociations[i][2])
        end
    end
EndMacro



//******************************Pre-Formed Maps************************************
//path array
//1 streetLayerPath
//2 transitNetworkPath
//3 transitRoutesPath
//4 modeTablePath
//5 outputPath
//6 networkPath
//7 mapPath
//8 javaPath
//9 externalDistanceMatrixPath
//10 csvTripTablePath
//11 tripMatrixPath
//12 assignmentOutputPath
//13 basePath
//14 bikeLayerPath
//15 bikeNetworkPath


//mapValues = {name, categories, category type, color start, color end, missing value color, zeros as missing values}
Dbox "defaultMapConfig" (mapValues, title) Title: title
    init do
        shared cc_Colors
        if mapValues = null then do
            dim mapValues[7]
        end
        dim mapCats[14]
        for i = 2 to 15 do
            mapCats[i-1] = i
            if mapCats[i-1] = mapValues[2] then do
                mapCategories = i-1
            end
        end
        
        nameText = mapValues[1]
        
        categoryTypes = {{"Equal Intervals","Equal Steps"},{"Equal Feature Count","Quantiles"},{"Categories","Categories"}}
        dim categoryTypeList[categoryTypes.length]
        for i = 1 to categoryTypes.length do
            categoryTypeList[i] = categoryTypes[i][1]
            if categoryTypes[i][2] = mapValues[3] then do
                mapCategoryType = i
            end
        end
      
        colors = {{"White",cc_Colors.White},
                  {"Black",cc_Colors.Black},
                  {"Gray",cc_Colors.Gray},
                  {"Red",cc_Colors.Red},
                  {"Green",cc_Colors.Green},
                  {"Yellow",cc_Colors.Yellow},
                  {"Gold",cc_Colors.Gold},
                  {"Brown",cc_Colors.Brown},
                  {"Cyan",cc_Colors.Cyan},
                  {"Blue",cc_Colors.Blue},
                  {"Orange",cc_Colors.Orange},
                  {"Purple",cc_Colors.Purple}}

        dim colorList[colors.length]
        for i = 1 to colors.length do
            colorList[i] = colors[i][1]
            if colors[i][2] = mapValues[4] then do
                startColor = i
            end
            if colors[i][2] = mapValues[5] then do
                endColor = i
            end
            if colors[i][2] = mapValues[6] then do
                missingColor = i
            end
        end
    
        solid_line = LineStyle({{{0, -1, 0}}})
        str1 = "XXXXXXXX"
        solid = FillStyle({str1, str1, str1, str1, str1, str1, str1, str1})
        
        if mapValues[7] = "True" then do
            zeroMissing = 1
        end
        else do
            zeroMissing = 0
        end
    enditem
    
    Text "Color Theme Name" 1,0.7 
    
    Edit Text 1, 2, 28  Variable: nameText do
        mapValues[1] = nameText
    enditem
    
    Popdown Menu 19,4,5 Prompt: "Number of Categories" List: mapCats Variable: mapCategories do
        mapValues[2] = mapCats[mapCategories]
    enditem
    
    Popdown Menu 13.6,6,20 Prompt: "Category Type" List: categoryTypeList Variable: mapCategoryType do
        mapValues[3] = categoryTypes[mapCategoryType][2]
    enditem
    
    Radio List 1, 8, 33, 7 Prompt: "Color Ramp" 

    Popdown Menu 16,9.5,7 Prompt: "Start (Low) Color" List: colorList Variable: startColor do
        mapValues[4] = colors[startColor][2]
    enditem
    
    Popdown Menu 16,11.5,7 Prompt: "End (High) Color" List: colorList Variable: endColor do
        mapValues[5] = colors[endColor][2]
    enditem
    
    Popdown Menu 16,13.5,7 Prompt: "Missing Value" List: colorList Variable: missingColor do
        mapValues[6] = colors[missingColor][2]
    enditem
    
	Sample 25, 9.5 transparent contents: SampleArea(3, solid_line, mapValues[4], solid, mapValues[4], )
	Sample 25, 11.5 transparent contents: SampleArea(3, solid_line, mapValues[5], solid, mapValues[5], )
	Sample 25, 13.5 transparent contents: SampleArea(3, solid_line, mapValues[6], solid, mapValues[6], )
	
	Checkbox 2,16 Prompt: "Treat zeros as missing values" Variable: zeroMissing do
	    if zeroMissing = 1 then do
	        mapValues[7] = "True"
	    end
	    else do
	        mapValues[7] = "False"
	    end
	enditem
    
    Button "OK" 1,18,10,1.5 do
        return(mapValues)
    enditem
    
    Button "Cancel" 23.5,18,10,1.5 do
        return()
    enditem
EndDbox

Macro "getColorRamp" (rgb1,rgb2,n)
    return(GeneratePalette(rgb1,rgb2,n-2,))    
EndMacro

Dbox "chooseType" (type,form,title,value) Title: title
    init do
        pair = {type[value],form[value]}
    enditem
    
    Text 1,0.7 Variable: "Choose " + title + " Category"
    
    Popdown Menu 1,2,12 List: type Variable: value do
        pair = {type[value],form[value]}
    enditem
    
    Button "OK" 1,4,6,1.5 do
        return(pair)
    enditem
    
    Button "Cancel" 10,4,6,1.5 do
        return()
    enditem
    
EndDbox

Macro "preFormCatMap" (pathArray,scenarioName,season,type,form,title,value,typeText)
    pair = RunDbox("chooseType",type,form,title,value)
    if not pair = null then do
        RunMacro("preFormPatternMap",pathArray,scenarioName,season,"Filled " + pair[1] + " " + typeText,
                RunMacro("getPreFormZoneJoins",season),
                pair[2],"TAZ",pair[1] + typeText)
    end
EndMacro

Macro "preFormAssignmentMap" (pathArray,scenarioName,season)
    if season = "Summer" then do
        season = 1
    end 
    else do
        season = 0
    end
    if (RunMacro("TestIfRun",pathArray,scenarioName,season) = 0) then do
        type = RunDbox("assignmentMapDefine")
        if type = null then do
            return()
        end
        hour = type[1]
        coloring = type[2]
        shared cc_Colors
        SetCursor("HourGlass")
        gisPath = pathArray[13] + "scenarios\\" + scenarioName + "\\gis\\"
        baseMap = OpenMap(gisPath + "maps\\tahoe.map",)
        //save this as a temp map so if someone hits the "save" button it doesn't overwrite the base map
        SaveMap(baseMap,gisPath + "maps\\temp_base.map")
        layers = GetMapLayers(baseMap, "All")
        for i = 1 to layers[1].length do
            SetLayerVisibility(baseMap + "|" + layers[1][i], "False")
        end  
        //SetLayerVisibility(baseMap + "|" + "TAZ", "True")
        SetLayerVisibility(baseMap + "|" + "FullStreets", "True")
        SetMap(baseMap)
        SetLayer("FullStreets")
        if hour = "Daily" then do
            pd_field_name = CreateExpression("FullStreets", hour + " V/C Ratio", "(DailyVolume)/(" + 
                    "AB_AM_Cap + BA_AM_Cap + " + 
                    "AB_MD_Cap + BA_MD_Cap + " + 
                    "AB_PM_Cap + BA_PM_Cap + " + 
                    "AB_LN_Cap + BA_LN_Cap" + 
                    ")" , )
           pd_field_name2 = CreateExpression("FullStreets", hour + " Daily Volume", "DailyVolume" , )
        end
        else do
            pd_field_name = CreateExpression("FullStreets", hour + " V/C Ratio", "(AB_" + hour + "_Flow + BA_" + hour + "_Flow)/(AB_" + hour + "_Cap + BA_" + hour + "_Cap)" , )
            pd_field_name2 = CreateExpression("FullStreets", hour + " Daily Volume", "AB_" + hour + "_Flow + BA_" + hour + "_Flow" , )
        end
        if coloring = 2 then do
            theme = CreateTheme(hour + " V/C Ratio", "FullStreets.[" + hour + " V/C Ratio]", "Quantiles", 9,) 
        end
        else do
            theme = CreateTheme(hour + " V/C Ratio", "FullStreets.[" + hour + " V/C Ratio]", "Manual", 5, 
                        {{"Values", 
                         {{0, "True", 0.6, "False"},
                          {0.6, "True", 0.8, "False"},
		                  {0.8, "True", 0.9, "False"},
		                  {0.9, "True", 1.0, "False"},
		                  {1.0, "True", 10.0, "True"}}
	                    }})
	    end
	    SetThemeLineColors(theme,{cc_Colors.Gray} + RunMacro("getColorRamp",cc_Colors.Green,cc_Colors.Red,5))
        ShowTheme(,theme)
        theme2 = CreateContinuousTheme(hour + " Flow", {"FullStreets.[" + hour + " Daily Volume]"},)
        ShowTheme(,theme2)
        RunMacro("G30 create legend",)
        ShowLegend(baseMap)
        ResetCursor()
    end
EndMacro

Dbox "assignmentMapDefine" 
    init do
        hours = {"AM","MD","PM","LN","Daily"}
        hoursD = {"AM Peak","Midday","PM Peak","Overnight","Entire Day"}
        coloring = 1
        hour_idx = 5
    enditem
    
    Text 1,0.7 Variable: "Choose Assignment Period"
    
    Popdown Menu 1,2,12 List: hoursD Variable: hour_idx do
    
    enditem
    
    Radio List 1, 4, 30, 3 Prompt: "VOC Coloring" Variable: coloring
        Radio Button 2, 5.5 Prompt: "Absolute"
        Radio Button 20, 5.5 Prompt: "Relative"
    
    Button "OK" 1,8,15,1.5 do
        return({hours[hour_idx],coloring})
    enditem
    
    Button "Cancel" 20,8,15,1.5 do
        return()
    enditem
    
EndDbox

Macro "preFormPopDMap" (pathArray,scenarioName,season)
    RunMacro("preFormPatternMap",pathArray,scenarioName,season,"Population Density",
            RunMacro("getPreFormZoneJoins",season),
            "total_persons3 / Area","TAZ","popDensity")
EndMacro

Macro "preFormPopAMap" (pathArray,scenarioName,season)
    RunMacro("preFormPatternMap",pathArray,scenarioName,season,"pop",
            RunMacro("getPreFormZoneJoins",season),
            ,"TAZ","total_persons3")
EndMacro

Macro "preFormEmpMap" (pathArray,scenarioName,season)
    empType = {"Retail","Service","Recreation","Gaming","Other","Total"}
    empForm = {"emp_retail3","emp_srvc3","emp_rec3","emp_game3","emp_other3","emp_retail3 + emp_srvc3 + emp_rec3 + emp_game3 + emp_other3"}
    RunMacro("preFormCatMap",pathArray,scenarioName,season,empType,empForm,"Employment",6,"Employment")
EndMacro

Macro "preFormNonResidentFillMap" (pathArray,scenarioName,season)
    nonResType = {"Hotel/Casino/Resort","Seasonal Home"," Visitor Non-Seasonal Home","Campsite","Total"}
    nonResForm = {"hotelmotel1 * hotelmotel2 + resort1 * resort2 + casino1 * casino2",
                  "(total_residential_units3 - total_occ_units3) * percentHouseSeasonal1 * seasonal2",
                  "(total_residential_units3 - total_occ_units3) * (1 - percentHouseSeasonal1) * seasonal2",
                  "campground1 * campground2",
                  "hotelmotel1 * hotelmotel2 + resort1 * resort2 + casino1 * casino2 + (total_residential_units3 - total_occ_units3)" + 
                  " * percentHouseSeasonal1 * seasonal2 + (total_residential_units3 - total_occ_units3) * (1 - percentHouseSeasonal1)" + 
                  " * seasonal2 + campground1 * campground2"}
    RunMacro("preFormCatMap",pathArray,scenarioName,season,nonResType,nonResForm,"Visitor Stay Type",5,"Units")
EndMacro

Macro "getPreFormZoneJoins" (season)
    return({{"zonal\\OvernightVisitorZonalData_" + season + ".csv","CSV","TAZ","taz1",True},
            {"zonal\\VisitorOccupancyRates_" + season + ".csv","CSV","TAZ","taz2",True},
            {"zonal\\SocioEcon_" + season + ".csv","CSV","TAZ","taz3",True},
            {"zonal\\SchoolEnrollment.csv","CSV","TAZ","taz4",False}
            })
EndMacro

Macro "createZonalJoins" (pathArray, scenarioName, joins,layer,season)
    if not RunMacro("isView",season + "ZonalJoin" + IntToString(joins.length)) then do
        for i = 1 to joins.length do
            subName = Substring(Substitute(Substitute(season + joins[i][1],"\\","_",),".csv","",),1,25)
            if not joins[i][5] then do
                subName = Substring(Substitute(Substitute(joins[i][1],"\\","_",),".csv","",),1,25)
            end
            if not joins[i][5] and RunMacro("isView",subName + IntToString(i)) then do
                themeView = subName + IntToString(i)
            end
            else do
                //only open view and rename if it doesn't exist already
                themeView = OpenTable(subName + IntToString(i),joins[i][2],{pathArray[13] + "scenarios\\" + scenarioName + "\\" + joins[i][1],})
                fields_array = GetFields(themeView, "All")
                for j = 1 to fields_array[2].length do
                    RenameField(fields_array[2][j],fields_array[1][j] + IntToString(i))
                end
            end
            if i = 1 then do
                themeViewFinal = JoinViews(season + "ZonalJoin" + IntToString(i), layer + "." + joins[i][3], themeView + "." + joins[i][4],)
            end
            else do
                themeViewFinal = JoinViews(season + "ZonalJoin" + IntToString(i), season + "ZonalJoin" + IntToString(i-1) + "." + joins[i][3], themeView + "." + joins[i][4],)
            end
        end
    end
    return(season + "ZonalJoin" + IntToString(joins.length))
EndMacro


//joins = [[file name starting at scenario name\\, type, original join field, file join field, season specific]]
Macro "preFormPatternMap" (pathArray,scenarioName,season,name,joins,formula,layer,field)
    shared cc_Colors
    themeValues = RunDbox("defaultMapConfig",{name + " (" + season + ")",
                                              5,"Equal Steps",cc_Colors.Green,cc_Colors.Red,cc_Colors.Gray,"True"},name + " Color Theme Configuration")
    if not themeValues = null then do
        SetCursor("HourGlass")
        gisPath = pathArray[13] + "scenarios\\" + scenarioName + "\\gis\\"
        baseMap = OpenMap(gisPath + "maps\\tahoe.map",)
        //save this as a temp map so if someone hits the "save" button it doesn't overwrite the base map
        SaveMap(baseMap,gisPath + "maps\\temp_base.map")
        layers = GetMapLayers(baseMap, "All")
        for i = 1 to layers[1].length do
            SetLayerVisibility(baseMap + "|" + layers[1][i], "False")
        end  
        SetLayerVisibility(baseMap + "|" + layer, "True")
        SetMap(baseMap)
        themeViewFinal = RunMacro("createZonalJoins",pathArray, scenarioName, joins,layer,season)
        if not formula = null then do
            pd_field_name = CreateExpression(themeViewFinal, field, formula, )
        end
        SetView(themeViewFinal)
        theme = CreateTheme(themeValues[1], themeViewFinal + "." + field, themeValues[3], themeValues[2], {{"Zero",themeValues[7]}})
        SetThemeFillColors(theme,{themeValues[6]} + RunMacro("getColorRamp",themeValues[4],themeValues[5],themeValues[2]))
        ShowTheme(,theme)
        RunMacro("G30 create legend",)
        ShowLegend(baseMap)
        ResetCursor()
    end
EndMacro


Macro "isView" (viewName)
    views = GetViews()
    isView = False
    for i = 1 to views[1].length do
        if viewName = views[1][i] then do
            isView = True
        end
    end
    return(isView)
EndMacro






//*********************************************************************************

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
        fctemp = CopyArray(fc1sum)
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
    LaunchProgram(pathArray[8] + "\\TextViewer\\TextViewer " + outputDirectory + "temp.txt -f fix -t \"Assignment Results\"")
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
    //vws = GetViewNames()
    //for i = 1 to vws.length do
    //    CloseView(vws[i])
    //end
    //RunMacro("CloseAll")
    
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

Macro "TripSummarizer" (pathArray,scenarioName,summer,iteration)
    RunProgram("cmd /s /c \"start \"cmd\" /D" + pathArray[8] + "summarizer\\ /WAIT \"runRSummarizer.cmd\" " + scenarioName + "\"",)
    RunProgram("cmd /s /c \"start \"cmd\" /D" + pathArray[8] + "summarizer\\ /WAIT \"runVMTVisualizer.cmd\" " + scenarioName + "\"",)
    files = {"vmt_summary","trip_length_summary","tour_length_summary","autoocc_summary"}
    outputDirectory = pathArray[13] + "scenarios\\" + scenarioName + "\\outputs_winter\\reports\\"
    if summer = 1 then do
        outputDirectory = pathArray[13] + "scenarios\\" + scenarioName + "\\outputs_summer\\reports\\"
    end
    for f = 1 to files.length do
        ff = outputDirectory + files[f]
        nf = ff + i2s(iteration) + ".csv"
        if GetFileInfo(nf) <> null then do
            DeleteFile(nf)
        end
        RenameFile(ff + ".csv",nf)
    end
EndMacro

Macro "MSAAssignmentResults" (pathArray,scenarioName,summer,iteration) 
 
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
    msa_name = "MSA_iter" + IntToString(iteration)
    msa_file = outputDirectory + msa_name + ".bin"
    SetView("FullStreets")
    ExportView("FullStreets|","FFB",msa_file,,)
    CloseView("FullStreets")
    if iteration > 1 then do
        old_msa_name = "MSA_iter" + IntToString(iteration-1)
        periods = {"AM","PM","MD","LN"}
        dirs = {"AB","BA"}
        shift_list = {"IVTT","Flow"}
        msa_list = {"DailyVolume"}
        for i = 1 to periods.length do
            for j = 1 to dirs.length do
                for k = 1 to shift_list.length do
                    msa_list = msa_list + {dirs[j] + "_" + periods[i] + "_" + shift_list[k]}
                end
            end
        end
        
        Opts = null
        Opts.Input.[Dataview Set] = {{msa_file,outputDirectory + old_msa_name + ".bin", "ID", "ID"},"msa_join"}
        Opts.Global.Method = "Formula"
        for i = 1 to msa_list.length do
            Opts.Global.Fields = {msa_name + "." + msa_list[i]}
            Opts.Global.Parameter = "(" + old_msa_name + "." + msa_list[i] + "*" + i2s(iteration-1) + " + " + msa_name + "." + msa_list[i] + ")/" + i2s(iteration)
            RunMacro("TCB Run Operation",1,"Fill Dataview",Opts)
        end
        
        //fill link data
        Opts = null
        Opts.Input.[Dataview Set] = {{pathArray[1] + "FullStreets.dbd|FullStreets",msa_file,"ID","ID"},"msa_street_join"}
        Opts.Global.Method = "Formula"
        for i = 1 to msa_list.length do
            Opts.Global.Fields = {"FullStreets." + msa_list[i]}
            Opts.Global.Parameter = msa_name + "." + msa_list[i]
            RunMacro("TCB Run Operation",1,"Fill Dataview",Opts)
        end
    end
EndMacro
//

Macro "RunTahoeEmfacAQuaVis" (pathArray,season_idx,scenarioName,year)
    if season_idx = 1 then do
        season = "summer"
    end
    else do
        season = "winter"
    end
    RunMacro("RunTahoeAQuaVis",pathArray[13],year,scenarioName,season)
EndMacro

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

    RunMacro("UpdateFFSpeeds",pathArray)
    RunMacro("CreateDriveNetwork",pathArray)
	RunMacro("PreProcessBikePedLayer", pathArray) // BK added
	RunMacro("CreateBikePedNetwork", pathArray) // BK added
	RunMacro("BikePedExtTAZLookup", pathArray) // BK added
	RunMacro("WalkTimeSkim", season, pathArray, "WalkTimeSkim") // BK added
	RunMacro("WalkDistanceSkim", season, pathArray, "WalkDistanceSkim") // BK added
	RunMacro("BikeTimeSkim", season, pathArray, "BikeTimeSkim") // BK added
	RunMacro("BikeDistanceSkim", season, pathArray, "BikeDistanceSkim") // BK added
    RunMacro("CopyBaseTripTables",pathArray)
    RunMacro("CreateTripMatrices",pathArray)
    RunMacro("TrafficAssignment",pathArray,cr_user_iters)
    RunMacro("BandsRun",season,pathArray)
    RunMacro("TransitSkimmer",season,pathArray)
    RunMacro("JavaPreModelCode",season,pathArray)

endMacro

///////////////////////////////////////////////////////////////////////////////////////////
//Delete Scenario stuff below                                                            //
///////////////////////////////////////////////////////////////////////////////////////////
Dbox "ScenarioDeleter" (pathArray,scenarioList,scenarioName) title: "Delete Scenario"
    init do
    enditem
    Text "Are you sure you want to delete?" 1.5,.6 variable: "Are you sure you want to delete " + scenarioName + "?"
    Text "bb" 1.5,1.6 variable: "This will permanently remove scenario and cannot be undone!"
    
    Button "OK" 2,3,15.5,2 do
        newScenarioList = {}
        RunProgram("CMD /C RMDIR /S /Q " + pathArray[13] + "scenarios\\" + scenarioName,)
        if GetFileInfo(pathArray[13] + "scenarios\\" + scenarioName) = null then do
            for i = 1 to scenarioList.length do
                if scenarioList[i] <> scenarioName then do
                    if newScenarioList[1] = null then do
                        newScenarioList = {scenarioList[i]}
                    end
                    else do
                        newScenarioList = newScenarioList + {scenarioList[i]}
                    end
                end
            end
            ShowMessage("Scenario deletion successful!")
            return(newScenarioList)
        end
        ShowMessage("Scenario deletion failed!")
        return(scenarioList)
    endItem
    
    Button "Cancel" 30.5,3,15.5,2 do
        return(scenarioList)
    endItem
    
EndDBox

///////////////////////////////////////////////////////////////////////////////////////////
//Create Scenario stuff below                                                            //
///////////////////////////////////////////////////////////////////////////////////////////

Dbox "NewScenarioCreator" (pathArray,scenarioList) title: "Create New Scenario"
    init do
        opt_idx = 0
		source_inputs = 1
		source_inputs_list = {"2022_inputs","2035_inputs","2050_inputs"}
    enditem
    Text "Enter New Scenario Name" 1.5,.6
	Text "Select Source Scenario" 26.5,.6
    Edit Text 2, 2, 20
        variable: scenarioName
	Popdown Menu "Source Scenario Chooser" 30.5, 2, 10 List: source_inputs_list variable: source_inputs do
		
	endItem
	
	
    Text "Scenario Description" 1.5, 3.6
    Edit Text 2, 5, 45, 5 
        variable: scenarioDescription
    Button "OK" 2,11,15.5,2 do
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
        if scenario_name = "" or scenario_name = null then do
            ShowMessage("Enter a scenario name.")
            nameOK = 0
        end
        if nameOK = 1 then do
            RunMacro("CreateNewScenario",pathArray,scenario_name,source_inputs_list[source_inputs])
            scenarioList = InsertArrayElements(scenarioList,scenarioList.length + 1,{scenario_name})
            if not scenarioDescription = null and not scenarioDescription = "" then do
                RunMacro("recordScenarioDescription",pathArray,scenario_name,scenarioDescription)
            end
            return(scenarioList)
        end
    endItem
    
    Button "Cancel" 30.5,11,15.5,2 do
        return(scenarioList)
    endItem
EndDbox

//step 1 - unzip scenario directory
//step 2 - generate map
//setp 3 - create summer/winter property/xml files
Macro "CreateNewScenario" (pathArray,scenarioName,source_name)
    //RunMacro("JavaCreateScenario",pathArray,scenarioName)
	RunMacro("CopyScenarioFolder",pathArray,scenarioName,source_name)
    RunMacro("GenerateMap",pathArray,scenarioName)
    scenarioPath = RunMacro("GenerifyPath",pathArray[13] + "scenarios\\" + scenarioName + "\\")
    scenarioCodePath = scenarioPath + "code\\"
    referencePath = RunMacro("GenerifyPath",pathArray[13] + "reference\\")
    RunMacro("TemplateToFile",pathArray[8] + "tahoe.template.properties",scenarioCodePath + "tahoe_summer.properties",referencePath,scenarioPath,1)
    RunMacro("TemplateToFile",pathArray[8] + "tahoe.template.properties",scenarioCodePath + "tahoe_winter.properties",referencePath,scenarioPath,0)
    RunMacro("TemplateToFile",pathArray[8] + "log4j_mine.template.xml",scenarioCodePath + "log4j_mine_summer.xml",referencePath,scenarioPath,1)
    RunMacro("TemplateToFile",pathArray[8] + "log4j_mine.template.xml",scenarioCodePath + "log4j_mine_winter.xml",referencePath,scenarioPath,0)
EndMacro


Macro "CopyScenarioFolder" (pathArray,scenarioName,source_name)
	
	referencePath =pathArray[13] + "reference\\"
	scen_data_dir = pathArray[13] + "scenarios\\" + scenarioName + "\\"
	
	batch_file = referencePath + "copyDir.cmd"
	fptr = OpenFile(batch_file, "w")
	WriteLine(fptr, "@echo off")
  	WriteLine(fptr, "")
	
	
	line = "SET referencePath=" + Left(Substitute(referencePath, "\\", "/", ), StringLength(referencePath) - 1)				
  	WriteLine(fptr, line)
	WriteLine(fptr, "")

	line = "SET scenario_path=" + Left(Substitute(scen_data_dir, "\\", "/", ), StringLength(scen_data_dir) - 1)				
  	WriteLine(fptr, line)
	WriteLine(fptr, "")
	
	line = 'xcopy /I /E /Y "%referencePath%\\"'+source_name+ ' "%scenario_path%"'
	WriteLine(fptr, line)
		
	WriteLine(fptr, "")
	
	CloseFile(fptr)
	
	RunProgram(batch_file, {{"Maximize", "True"}})
	
	DeleteFile(batch_file)
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
    javaPath = pathArray[8] + "java\\jdk1.8.0_73\\bin\\java.exe"
    RunProgram("cmd /s /c \"start \"cmd\" /D" + RunMacro("GetBaseDrive",pathArray) + ":\\ /WAIT \"" + javaPath + "\"" + javaClassPath + javaVMArguments + javaCommand + "\"",)
EndMacro

Macro "GenerateMap" (pathArray,scenarioName)
    scenarioPath = pathArray[13] + "scenarios\\" + scenarioName + "\\"
	scope = Scope(Coord(-119947195, 38946737), 54.08997181860831, 47.20617866332869, 0)
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
    //note: if put into non-default directory (e.g. d: instead of c:) then comment next
    // line; things should still work, but haven't been tested
    inputPath = right(inputPath,len(inputPath)-2)
    return(inputPath)
EndMacro

//summer = 1 means summer, anything else is winter
Macro "TemplateToFile" (templateFile,destinationFile,referencePath,scenarioPath,summer)
    file_from = OpenFile(templateFile, "r")
    if left(destinationFile,1) = "\\" or left(destinationFile,1) = "/" then do
        file_to = OpenFile(RunMacro("GetBaseDriveFromDir",templateFile) + ":\\" + destinationFile, "w")
    end
    else do
        file_to = OpenFile(destinationFile, "w")
    end
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

Macro "UpdateFFSpeeds" (pathArray)
    RunMacro("TCB Init")
    Opts = null
    Opts.Input.[Dataview Set] = {pathArray[1] + "FullStreets.DBD|FullStreets", "FullStreets"}
    Opts.Global.Fields = {"AB_FF_TravelTime"}
    Opts.Global.Method = "Formula"
    Opts.Global.Parameter = "min(Length / (AB_Speed + .00001) * 60 * AB_TT_Multiplier,999)"
    RunMacro("TCB Run Operation", 1, "Fill Dataview", Opts)
    Opts = null
    Opts.Input.[Dataview Set] = {pathArray[1] + "FullStreets.DBD|FullStreets", "FullStreets"}
    Opts.Global.Fields = {"BA_FF_TravelTime"}
    Opts.Global.Method = "Formula"
    Opts.Global.Parameter = "min(Length / (BA_Speed + .00001) * 60 * BA_TT_Multiplier,999)"
    RunMacro("TCB Run Operation", 1, "Fill Dataview", Opts)
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

/////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//BK: Bike and Walk skim / Shortest BikePed path stuff below                                                               //
/////////////////////////////////////////////////////////////////////////////////////////////////////////////////

//BK: Macros need to be created:
//1- Macro/Procedure(s) to read the BikePed Links Layer (that has the connectors already): Macro "AddLayer"
//2- Macro to add two fields: BIKE_MINMILE and WALK_MINMILE: Macro "addfields" (dataview, newfldnames, typeflags)
//3- Macro to calculate the BIKE_MINMILE , based on bike_facil: Macro "BikePedMINMILE" 
//4- The Macro to create BikePedNetwork
//5- Two Macros to create BikeSkim, WalkSkim, with cores of BIKE_MINMILE and WALK_MINMILE included in the skims


// BK: this Macro Preprocess and create the Bike Network
Macro "PreProcessBikePedLayer" (pathArray)
    RunMacro("TCB Init")
	 file = pathArray[14] + "bike_ped_links.DBD"
	 BikePedLinksVW = RunMacro ("AddLayer", file , "Line")
	 RunMacro("addfields", BikePedLinksVW, {"BIKE_MINMILE", "WALK_MINMILE"}, {"r","r"})
	 RunMacro("BikePedMINMILE", BikePedLinksVW)
	 return (BikePedLinksVW) 
endMacro
	 
Macro "CreateBikePedNetwork" (pathArray)
    RunMacro("TCB Init")
     Opts = null
     Opts.Input.[Link Set] = {pathArray[14] + "bike_ped_links.DBD|bike_ped_links", "bike_ped_links"}
     Opts.Global.[Network Options].[Node ID] = "bike_ped_nodes.ID"
     Opts.Global.[Network Options].[Link ID] = "bike_ped_links.ID"
     Opts.Global.[Network Options].[Turn Penalties] = "No"
     Opts.Global.[Network Options].[Keep Duplicate Links] = "FALSE"
     Opts.Global.[Network Options].[Ignore Link Direction] = "FALSE"
     //Opts.Global.[Network Options].[Time Units] = "Minutes"
	 Opts.Global.[Link Options] = {{"Length", "bike_ped_links.Length", "bike_ped_links.Length"}, {"Dir", "bike_ped_links.Dir", "bike_ped_links.Dir"}, {"speed", "bike_ped_links.speed", "bike_ped_links.speed"}, {"BIKE_MINMILE", "bike_ped_links.BIKE_MINMILE", "bike_ped_links.BIKE_MINMILE"}, {"WALK_MINMILE", "bike_ped_links.WALK_MINMILE", "bike_ped_links.WALK_MINMILE"}}
	/* 
     Opts.Global.[Link Options].Length = {"bike_ped_links.Length", "bike_ped_links.Length", , , "False"}
     Opts.Global.[Link Options].osmid = {"bike_ped_links.osmid", "bike_ped_links.osmid", , , "False"}
     Opts.Global.[Link Options].speed = {"bike_ped_links.speed", "bike_ped_links.speed", , , "False"}
	 Opts.Global.[Link Options].BIKE_MINMILE = {"bike_ped_links.BIKE_MINMILE", "bike_ped_links.BIKE_MINMILE", , , "True"}
	 Opts.Global.[Link Options].WALK_MINMILE = {"bike_ped_links.WALK_MINMILE", "bike_ped_links.WALK_MINMILE", , , "True"}
     Opts.Global.[Link Options].Connector = {"bike_ped_links.Connector", "bike_ped_links.Connector", , , "False"} */
	 
	 Opts.Global.[Node Options].ID = "bike_ped_nodes.ID"
     Opts.Global.[Node Options].Longitude = "bike_ped_nodes.Longitude"
     Opts.Global.[Node Options].Latitude = "bike_ped_nodes.Latitude"
	 Opts.Global.[Node Options].Centroid = "bike_ped_nodes.Centroid"
     Opts.Global.[Node Options].Elevation = "bike_ped_nodes.Elevation"
     //Opts.Global.[Length Units] = "Miles"
     Opts.Output.[Network File] = pathArray[6] + "Tahoe_BikePed_Network.net"
     ret_value = RunMacro("TCB Run Operation", 1, "Build Highway Network", Opts)
	 maps = GetMapNames()
		
	 for i = 1 to maps.length do

		 CloseMap(maps[i])

		 end
	 
	 
endMacro


Macro "AddLayer" (file, type)
	RunMacro("TCB Init")
	//Adds .dbd to map as a layer
	//Type: "Point", "Line", or "Area" for geographic layers, "Image" for image layers, or "Image Library" for image libraries

	map_name = GetMap()
	layer_names = GetLayerNames()
	file_layers = GetDBLayers(file)
	file_info = GetDBInfo(file)
	if map_name = null then map_name = CreateMap("RSG", {{"Scope", file_info[1]}, {"Auto Project", "True"}})
	SetMapRedraw(map_name, "False")

	//Check if db already exists
	for i=1 to layer_names.length do
		//Skip if Type mismatch
		layer_type = GetLayerType(layer_names[i])
		if layer_type <> type then goto skip
		
		//Check for dbd match
		layer_info = GetLayerInfo(layer_names[i])
		layerdb = layer_info[10]
		if lower(layerdb) = lower(file) then do
			//ShowMessage("AddLayer: LayerDB already exists in map")
			Return(layer_names[i])
		end
		skip:
	end


	//Else, add file to map
	newlyr = AddLayer(null, file_layers[1], file, file_layers[1])
	if GetLayerType(newlyr) <> type then do
		newlyr = AddLayer( , file_layers[2], file, file_layers[2]) //Add lines if only nodes were loaded
	end
	RunMacro("G30 new layer default settings", newlyr)
	Return(newlyr)

	endhere:
	throw("AddLayer: Layer already exists in map!")

endMacro

Macro "addfields" (dataview, newfldnames, typeflags)
	RunMacro("TCB Init")
	 //Add a new field to a dataview; does not overwrite
	 //RunMacro("addfields", mvw.node, {"Delay", "Centroid", "Notes"}, {"r","i","c"})
	 fd = newfldnames.length
	 dim fldtypes[fd]
	
	 if TypeOf(typeflags) = "array" then do 
		for i = 1 to newfldnames.length do
			if typeflags[i] = "r" then fldtypes[i] = {"Real", 12, 2}
			if typeflags[i] = "i" then fldtypes[i] = {"Integer", 10, 3}
			if typeflags[i] = "c" then fldtypes[i] = {"String", 16, null}
		end
	 end
	
	 if TypeOf(typeflags) = "string" then do 
		for i = 1 to newfldnames.length do
			if typeflags = "r" then fldtypes[i] = {"Real", 12, 2}
			if typeflags = "i" then fldtypes[i] = {"Integer", 10, 3}
			if typeflags = "c" then fldtypes[i] = {"String", 16, null}
		end
	 end

	 SetView(dataview)
     struct = GetTableStructure(dataview)

	 dim snames[1]
     for i = 1 to struct.length do
        struct[i] = struct[i] + {struct[i][1]}
	  snames = snames + {struct[i][1]}
     end

	 modtab = 0
     for i = 1 to newfldnames.length do
        pos = ArrayPosition(snames, {newfldnames[i]}, )
        if pos = 0 then do
           newstr = newstr + {{newfldnames[i], fldtypes[i][1], fldtypes[i][2], fldtypes[i][3], 
					"false", null, null, null, null}}
           modtab = 1
        end
     end
   
     if modtab = 1 then do
        newstr = struct + newstr
        ModifyTable(dataview, newstr)
     end
	 
endMacro

Macro "BikePedMINMILE" (BikePedLinksVW)
	RunMacro("TCB Init")
	 {BikeFacType, Length} = GetDataVectors(BikePedLinksVW + "|", {"bike_facil", "Length"}, {{"Sort Order",{{BikePedLinksVW + ".ID","Ascending"}}}})
	 // Bike Facility Type; speed class distinction 
	 BIKE_MINMILE = if (BikeFacType = 'class 1') then (2.0 * Length)	else 
	 if (BikeFacType = 'class 2') then (3.1 * Length) else 
	 if (BikeFacType = 'class 3') then (4.9 * Length) else (6.0 * Length)	
	 SetDataVector(BikePedLinksVW+"|", "BIKE_MINMILE", BIKE_MINMILE, {{"Sort Order",{{BikePedLinksVW + ".ID","Ascending"}}}})
	 WALK_MINMILE =  20.0 * Length
	 SetDataVector(BikePedLinksVW+"|", "WALK_MINMILE", WALK_MINMILE, {{"Sort Order",{{BikePedLinksVW + ".ID","Ascending"}}}})
	 
endMacro

Macro "BikePedExtTAZLookup" (pathArray)
/*
This Macro adds the external zones to the TAZ and creates a lookup bin file for Bike & Ped use, and also creates the ExternalDistanceMatrixBikePed 
*/

	RunMacro("TCB Init")
	 file= pathArray[16] + "TAZ.bin"

	 table = CreateObject("Table", {FileName: file})

	 file_copy = Substitute(file, ".bin", "_BikePedExtTAZLookup.bin",)

	 table.Export({FileName: file_copy})

	 //tazLookupFile= file_copy 

	 tazLookupTable = OpenTable("tazLookupTable","FFB", {file_copy}, {{"Shared", "True"}})
	 SetView(tazLookupTable)
	 rh = AddRecords("tazLookupTable",

     {"ID", "TAZ"},
     {
     { 12567,1},
     { 12566,2},
     { 12565,3},
     { 12564,4},
	 {12569,5},
	 {12563,6},
	 {12562,7},
	 {12602,10},
	 {12603,20},
	 {12604,30},
	 {12605,40},
	 {12606,50},
	 {12607,60},
	 {12608,70}
     }, null)
	 //{vw, set} = SplitString(tazLookupTable)
     //n = GetRecordCount(vw, set)
     //vec = Vector(n, "Double", {{"Constant", 0}})
     //SetDataVector(vw_set, "ExtDist", vec,)
	 
	 mat =CreateMatrix({tazLookupTable+"|", tazLookupTable+".TAZ" , "TAZ"},

                 {tazLookupTable+"|", tazLookupTable+".TAZ" , "TAZ"},

                 {{"File Name", pathArray[9] + "ExternalDistanceMatrixBikePed.mtx"}, {"Type", "Float"}})

	 mc = CreateMatrixCurrency(mat, "Table", "TAZ", "TAZ", )
	 //mc := 0.00
	 //FillMatrix(mc,,,{"Copy",0},)
	 CloseView(tazLookupTable)
endMacro

// BK: this Macro creates the BikeSkim (Length and MINMILE): 
Macro "BikeDistanceSkim" (season, pathArray, outputname)
    RunMacro("TCB Init")
    //Set up and run shortest path skim
     Opts = null
     Opts.Input.Network = pathArray[15] + "Tahoe_BikePed_Network.net"
     Opts.Input.[Origin Set] = {pathArray[14] + "bike_ped_links.DBD|bike_ped_nodes", "bike_ped_nodes", "Centroids", "Select * where Centroid <> null"}
     Opts.Input.[Destination Set] = {pathArray[1] + "bike_ped_links.DBD|bike_ped_nodes", "bike_ped_nodes", "Centroids", "Select * where Centroid <> null"}
     Opts.Input.[Via Set] = {pathArray[14] + "bike_ped_links.DBD|bike_ped_nodes", "bike_ped_nodes"}
     Opts.Field.Minimize = "Length"
     Opts.Field.Nodes = "bike_ped_nodes.ID"
     //Opts.Field.[Skim Fields].Length = "All" 
     Opts.Output.[Output Matrix].Label = "Bike_Distance_Matrix"
     //Opts.Output.[Output Matrix].Compression = 1
     Opts.Output.[Output Matrix].[File Name] = pathArray[5] + outputname + ".mtx"
     ret_value = RunMacro("TCB Run Procedure", 1, "TCSPMAT", Opts)
     
	 //Create intrazonal
     Opts = null
	 Opts.Input.[Matrix Currency] = {pathArray[5] + outputname + ".mtx", "Length",,}
	 Opts.Global.Factor = 1
     Opts.Global.Neighbors = 3
     Opts.Global.Operation = 1
     Opts.Global.[Treat Missing] = 1
     ret_value = RunMacro("TCB Run Procedure", 1, "Intrazonal", Opts)

	 //Add TAZ Matrix Index on Row and Column
	 skimMatrix = OpenMatrix(pathArray[5] + outputname + ".mtx","True")
	 mtxinx = GetMatrixIndexNames(skimMatrix)
	 for i = 1 to mtxinx[1].length do
		if mtxinx[1][i] = "TAZ" then goto skiphere
	 end

     Opts = null
     Opts.Input.[Current Matrix] = skimMatrix
     Opts.Input.[Index Type] = "Both"
     Opts.Input.[View Set] = {{pathArray[14] + "bike_ped_links.dbd|bike_ped_nodes", pathArray[16] + "TAZ_BikePedExtTAZLookup.bin", {"Centroid"}, {"ID"}}, "bike_ped_nodes+TAZ", "Centroids", "Select * where Centroid <> null"}
     Opts.Input.[Old ID Field] = {{pathArray[14] + "bike_ped_links.dbd|bike_ped_nodes", pathArray[16] + "TAZ_BikePedExtTAZLookup.bin", {"Centroid"}, {"ID"}}, "bike_ped_nodes.ID"}
     Opts.Input.[New ID Field] = {{pathArray[14] + "bike_ped_links.dbd|bike_ped_nodes", pathArray[16] + "TAZ_BikePedExtTAZLookup.bin", {"Centroid"}, {"ID"}}, "TAZ"}
     Opts.Output.[New Index] = "TAZ"
     ret_value = RunMacro("TCB Run Operation", "Add Matrix Index", Opts, &Ret)
	 
	 skiphere:

	 // Adding the External Zones skimmed
	 // Filled all cells with zeros to indicate infeasible Bike/Ped routes from external zones to Lake Tahoe internal zones by Ped/Bike (this will be taken care of in the UEC)
	 m2 = OpenMatrix(pathArray[9] + "ExternalDistanceMatrixBikePed.mtx", )
	 mc1 = CreateMatrixCurrency(skimMatrix, "Length", "TAZ", "TAZ", )
	 mc2 = CreateMatrixCurrency(m2, "Table", "TAZ", "TAZ", )
	 new_mat = CombineMatrices({mc1, mc2}, {{"File Name",pathArray[5] + outputname + "withExt" + ".mtx"},
     {"Label", ""},
     {"Operation", "Union"}})
	 m2 = null
	 mc1 = null
	 mc2 = null
	 
	 m = OpenMatrix(pathArray[5] + outputname + "withExt" + ".mtx", )
	 SetMatrixIndexNames(m, {{"TAZ"}, {"TAZ"}})
	 
	 mc1 = CreateMatrixCurrency(m, "Length", , , )
	 mc1 := Nz(mc1)

	 Opts = null
	 Opts.Input.[Input Matrix] = new_mat 
	 Opts.global.[Drop Core] = {"Table"}
	 RunMacro("TCB Run Operation", "Drop Matrix Core", Opts, &Ret)
	 
     //Save matrix core "Length" as "bikeDist" in a csv using correct matrix index
	 Opts = null
	 Opts.Input.[Input Matrix] = pathArray[5] + outputname + "withExt" + ".mtx" 
	 Opts.Input.[Target Core] = "Length" 
	 Opts.Input.[Core Name] = "BIKE_DIST"
	 RunMacro("TCB Run Operation", "Rename Matrix Core", Opts)

	 SetMatrixCore(new_mat, "BIKE_DIST")
     SetMatrixIndex(new_mat,"TAZ","TAZ")
     AddMatrixCore(m,"Blank")
     mc = CreateMatrixCurrency(m,"Blank",,,)
     FillMatrix(mc,,,{"Copy",0},)
	 
	 matrix_indices = GetMatrixIndexNames(m)
     CreateTableFromMatrix(m, pathArray[5] + outputname + ".bin","FFB",{{"Complete","Yes"}})
     tempTable = OpenTable("TempTable","FFB",{pathArray[5] + outputname + ".bin",})
	 tableInfo = GetTableStructure(tempTable)
     dim newTableInfo[tableInfo.length - 1]
     for i = 1 to (tableInfo.length - 1) do
       newTableInfo[i] = tableInfo[i] + {tableInfo[i][1]}
     end     
     ModifyTable(tempTable,newTableInfo)
	 SetView(tempTable)
	 //curr_dec = GetFieldDecimals("tempTable.BIKE_DIST")
	 //SetFieldDecimals("tempTable.BIKE_DIST", curr_dec + 2)
     ExportView("TempTable|","CSV",pathArray[5] + "bikeDist" + ".csv",,{{"CSV Header"},
																		{"Indexed Fields", {"TAZ", "TAZ:1"}},
																		{"Row Order", {{"TAZ", "Ascending"},
																		{"TAZ:1", "Ascending"}}}
																		})  
     CloseView(tempTable)
	 
     //RunProgram("cmd /c del \"" + pathArray[5] + outputname + ".mtx\"",)
     RunProgram("cmd /c del \"" + pathArray[5] + outputname + ".DCC\"",)
	// This loop closes all views:

	vws = GetViewNames()

	for i = 1 to vws.length do

		 CloseView(vws[i])

		 end
     RunProgram("cmd /c del \"" + pathArray[5] + outputname + ".mtx\"",)
	 RunProgram("cmd /c del \"" + pathArray[5] + outputname + "withExt" + ".mtx\"",)
     RunProgram("cmd /c del \"" + pathArray[5] + "bikeDist" + ".DCC\"",)
	 RunProgram("cmd /c del \"" + pathArray[5] + outputname + ".bin\"",)
     RunProgram("cmd /c del \"" + pathArray[5] + outputname + ".DCB\"",)
endMacro

Macro "BikeTimeSkim" (season, pathArray, outputname)
    RunMacro("TCB Init")
    //Set up and run shortest path skim
     Opts = null
     Opts.Input.Network = pathArray[15] + "Tahoe_BikePed_Network.net"
     Opts.Input.[Origin Set] = {pathArray[14] + "bike_ped_links.DBD|bike_ped_nodes", "bike_ped_nodes", "Centroids", "Select * where Centroid <> null"}
     Opts.Input.[Destination Set] = {pathArray[1] + "bike_ped_links.DBD|bike_ped_nodes", "bike_ped_nodes", "Centroids", "Select * where Centroid <> null"}
     Opts.Input.[Via Set] = {pathArray[14] + "bike_ped_links.DBD|bike_ped_nodes", "bike_ped_nodes"}
     Opts.Field.Minimize = "BIKE_MINMILE"
     Opts.Field.Nodes = "bike_ped_nodes.ID"
     //Opts.Field.[Skim Fields].Length = "All" 
     Opts.Output.[Output Matrix].Label = "Bike_Time_Matrix"
     //Opts.Output.[Output Matrix].Compression = 1
     Opts.Output.[Output Matrix].[File Name] = pathArray[5] + outputname + ".mtx"
     ret_value = RunMacro("TCB Run Procedure", 1, "TCSPMAT", Opts)
     
	 //Create intrazonal
     Opts = null
     Opts.Input.[Matrix Currency] = {pathArray[5] + outputname + ".mtx", "BIKE_MINMILE",,}
	 Opts.Global.Factor = 1
     Opts.Global.Neighbors = 3
     Opts.Global.Operation = 1
     Opts.Global.[Treat Missing] = 1
     ret_value = RunMacro("TCB Run Procedure", 1, "Intrazonal", Opts)

	 
	  //Add TAZ Matrix Index on Row and Column
	 skimMatrix = OpenMatrix(pathArray[5] + outputname + ".mtx","True")
	 mtxinx = GetMatrixIndexNames(skimMatrix)
	 for i = 1 to mtxinx[1].length do
		if mtxinx[1][i] = "TAZ" then goto skiphere
	 end

     Opts = null
     Opts.Input.[Current Matrix] = skimMatrix
     Opts.Input.[Index Type] = "Both"
     Opts.Input.[View Set] = {{pathArray[14] + "bike_ped_links.dbd|bike_ped_nodes", pathArray[16] + "TAZ_BikePedExtTAZLookup.bin", {"Centroid"}, {"ID"}}, "bike_ped_nodes+TAZ", "Centroids", "Select * where Centroid <> null"}
     Opts.Input.[Old ID Field] = {{pathArray[14] + "bike_ped_links.dbd|bike_ped_nodes", pathArray[16] + "TAZ_BikePedExtTAZLookup.bin", {"Centroid"}, {"ID"}}, "bike_ped_nodes.ID"}
     Opts.Input.[New ID Field] = {{pathArray[14] + "bike_ped_links.dbd|bike_ped_nodes", pathArray[16] + "TAZ_BikePedExtTAZLookup.bin", {"Centroid"}, {"ID"}}, "TAZ"}
     Opts.Output.[New Index] = "TAZ"
     ret_value = RunMacro("TCB Run Operation", "Add Matrix Index", Opts, &Ret)
	 
	 skiphere:
	 
	 // Adding the External Zones skimmed
	 // Filled all cells with zeros to indicate infeasible Bike/Ped routes from external zones to Lake Tahoe internal zones by Ped/Bike (this will be taken care of in the UEC)
	 m2 = OpenMatrix(pathArray[9] + "ExternalDistanceMatrixBikePed.mtx", )
	 mc1 = CreateMatrixCurrency(skimMatrix, "BIKE_MINMILE", "TAZ", "TAZ", )
	 mc2 = CreateMatrixCurrency(m2, "Table", "TAZ", "TAZ", )
	 new_mat = CombineMatrices({mc1, mc2}, {{"File Name",pathArray[5] + outputname + "withExt" + ".mtx"},
     {"Label", ""},
     {"Operation", "Union"}})
	 m2 = null
	 mc1 = null
	 mc2 = null
	 
	 m = OpenMatrix(pathArray[5] + outputname + "withExt" + ".mtx", )
	 SetMatrixIndexNames(m, {{"TAZ"}, {"TAZ"}})
	 mc1 = CreateMatrixCurrency(m, "BIKE_MINMILE", , , )
	 mc1 := Nz(mc1)

	 Opts = null
	 Opts.Input.[Input Matrix] = pathArray[5] + outputname + "withExt" + ".mtx" 
	 Opts.global.[Drop Core] = {"Table"}
	 RunMacro("TCB Run Operation", "Drop Matrix Core", Opts, &Ret)
	  //Save matrix core "Time" as csv using correct matrix index
     Opts = null
	 Opts.Input.[Input Matrix] = new_mat 
	 Opts.Input.[Target Core] = "BIKE_MINMILE" 
	 Opts.Input.[Core Name] = "BIKE_TIME"
	 RunMacro("TCB Run Operation", "Rename Matrix Core", Opts)
	 
	 SetMatrixCore(new_mat, "BIKE_TIME")
     SetMatrixIndex(new_mat,"TAZ","TAZ")
     AddMatrixCore(m,"Blank")
     mc = CreateMatrixCurrency(m,"Blank",,,)
     FillMatrix(mc,,,{"Copy",0},)
	 
	 matrix_indices = GetMatrixIndexNames(m)
     CreateTableFromMatrix(m, pathArray[5] + outputname + ".bin","FFB",{{"Complete","Yes"}})
     tempTable = OpenTable("TempTable","FFB",{pathArray[5] + outputname + ".bin",})
	 tableInfo = GetTableStructure(tempTable)
     dim newTableInfo[tableInfo.length - 1]
     for i = 1 to (tableInfo.length - 1) do
       newTableInfo[i] = tableInfo[i] + {tableInfo[i][1]}
     end     
     ModifyTable(tempTable,newTableInfo)
	 SetView(tempTable)
	 //curr_dec = GetFieldDecimals("tempTable.BIKE_TIME")
	 //SetFieldDecimals("tempTable.BIKE_TIME", curr_dec + 2)
     ExportView("TempTable|","CSV",pathArray[5] + "bikeTime" + ".csv",,{{"CSV Header"},
																		{"Indexed Fields", {"TAZ", "TAZ:1"}},
																		{"Row Order", {{"TAZ", "Ascending"},
																		{"TAZ:1", "Ascending"}}}
																		})
     CloseView(tempTable)
	  //RunProgram("cmd /c del \"" + pathArray[5] + outputname + ".mtx\"",)
     RunProgram("cmd /c del \"" + pathArray[5] + outputname + ".DCC\"",)
	// This loop closes all views:

	vws = GetViewNames()

	for i = 1 to vws.length do

		 CloseView(vws[i])

		 end
     RunProgram("cmd /c del \"" + pathArray[5] + outputname + ".mtx\"",)
	 RunProgram("cmd /c del \"" + pathArray[5] + outputname + "withExt" + ".mtx\"",)
     RunProgram("cmd /c del \"" + pathArray[5] + "bikeTime" + ".DCC\"",)
	 RunProgram("cmd /c del \"" + pathArray[5] + outputname + ".bin\"",)
     RunProgram("cmd /c del \"" + pathArray[5] + outputname + ".DCB\"",)
endMacro		 

// BK: this Macro defines the WalkSkim
Macro "WalkDistanceSkim" (season, pathArray, outputname)
    RunMacro("TCB Init")
    //Set up and run shortest path skim
     Opts = null
     Opts.Input.Network = pathArray[15] + "Tahoe_BikePed_Network.net"
     Opts.Input.[Origin Set] = {pathArray[14] + "bike_ped_links.DBD|bike_ped_nodes", "bike_ped_nodes", "Centroids", "Select * where Centroid <> null"}
     Opts.Input.[Destination Set] = {pathArray[1] + "bike_ped_links.DBD|bike_ped_nodes", "bike_ped_nodes", "Centroids"}
     Opts.Input.[Via Set] = {pathArray[14] + "bike_ped_links.DBD|bike_ped_nodes", "bike_ped_nodes"}
     Opts.Field.Minimize = "Length" 
     Opts.Field.Nodes = "bike_ped_nodes.ID"
     //Opts.Field.[Skim Fields].Length = "All"
     Opts.Output.[Output Matrix].Label = "Walk_Distance_Matrix"
     //Opts.Output.[Output Matrix].Compression = 1
     Opts.Output.[Output Matrix].[File Name] = pathArray[5] + outputname + ".mtx"
     ret_value = RunMacro("TCB Run Procedure", 1, "TCSPMAT", Opts)
     
     //Create intrazonal distances
     Opts = null
     Opts.Input.[Matrix Currency] = {pathArray[5] + outputname + ".mtx", "Length",,}
	 Opts.Global.Factor = 1
     Opts.Global.Neighbors = 3
     Opts.Global.Operation = 1
     Opts.Global.[Treat Missing] = 1
     ret_value = RunMacro("TCB Run Procedure", 1, "Intrazonal", Opts)
	 
     Opts = null
	 Opts.Input.[Matrix Currency] = {pathArray[5] + outputname + ".mtx", "Length",,}
	 Opts.Global.Factor = 1
     Opts.Global.Neighbors = 3
     Opts.Global.Operation = 1
     Opts.Global.[Treat Missing] = 1
     ret_value = RunMacro("TCB Run Procedure", 1, "Intrazonal", Opts)

	 
	 //Add TAZ Matrix Index on Row and Column
	 skimMatrix = OpenMatrix(pathArray[5] + outputname + ".mtx","True")
	 mtxinx = GetMatrixIndexNames(skimMatrix)
	 for i = 1 to mtxinx[1].length do
		if mtxinx[1][i] = "TAZ" then goto skiphere
	 end

     Opts = null
     Opts.Input.[Current Matrix] = skimMatrix
     Opts.Input.[Index Type] = "Both"
     Opts.Input.[View Set] = {{pathArray[14] + "bike_ped_links.dbd|bike_ped_nodes", pathArray[16] + "TAZ_BikePedExtTAZLookup.bin", {"Centroid"}, {"ID"}}, "bike_ped_nodes+TAZ", "Centroids", "Select * where Centroid <> null"}
     Opts.Input.[Old ID Field] = {{pathArray[14] + "bike_ped_links.dbd|bike_ped_nodes", pathArray[16] + "TAZ_BikePedExtTAZLookup.bin", {"Centroid"}, {"ID"}}, "bike_ped_nodes.ID"}
     Opts.Input.[New ID Field] = {{pathArray[14] + "bike_ped_links.dbd|bike_ped_nodes", pathArray[16] + "TAZ_BikePedExtTAZLookup.bin", {"Centroid"}, {"ID"}}, "TAZ"}
     Opts.Output.[New Index] = "TAZ"
     ret_value = RunMacro("TCB Run Operation", "Add Matrix Index", Opts, &Ret)

	 skiphere:

	 // Adding the External Zones skimmed
	 // Filled all cells with zeros to indicate infeasible Bike/Ped routes from external zones to Lake Tahoe internal zones by Ped/Bike (this will be taken care of in the UEC)
	 m2 = OpenMatrix(pathArray[9] + "ExternalDistanceMatrixBikePed.mtx", )
	 mc1 = CreateMatrixCurrency(skimMatrix, "Length", "TAZ", "TAZ", )
	 mc2 = CreateMatrixCurrency(m2, "Table", "TAZ", "TAZ", )
	 new_mat = CombineMatrices({mc1, mc2}, {{"File Name",pathArray[5] + outputname + "withExt" + ".mtx"},
     {"Label", ""},
     {"Operation", "Union"}})
	 m2 = null
	 mc1 = null
	 mc2 = null
	 
	 m = OpenMatrix(pathArray[5] + outputname + "withExt" + ".mtx", )
	 SetMatrixIndexNames(m, {{"TAZ"}, {"TAZ"}})
	 mc1 = CreateMatrixCurrency(m, "Length", , , )
	 mc1 := Nz(mc1)
	 
	 Opts = null
	 Opts.Input.[Input Matrix] = new_mat 
	 Opts.global.[Drop Core] = {"Table"}
	 RunMacro("TCB Run Operation", "Drop Matrix Core", Opts, &Ret)
     //Save matrix core "Length" as "walkDist" in a csv using correct matrix index
	 Opts = null
	 Opts.Input.[Input Matrix] = pathArray[5] + outputname + "withExt" + ".mtx" 
	 Opts.Input.[Target Core] = "Length" 
	 Opts.Input.[Core Name] = "WALK_DIST"
	 RunMacro("TCB Run Operation", "Rename Matrix Core", Opts)
	 
	 SetMatrixCore(new_mat, "WALK_DIST")
     SetMatrixIndex(new_mat,"TAZ","TAZ")
     AddMatrixCore(m,"Blank")
     mc = CreateMatrixCurrency(m,"Blank",,,)
     FillMatrix(mc,,,{"Copy",0},)
	 
	 matrix_indices = GetMatrixIndexNames(m)
     CreateTableFromMatrix(m, pathArray[5] + outputname + ".bin","FFB",{{"Complete","Yes"}})
     tempTable = OpenTable("TempTable","FFB",{pathArray[5] + outputname + ".bin",})
	 tableInfo = GetTableStructure(tempTable)
     dim newTableInfo[tableInfo.length - 1]
     for i = 1 to (tableInfo.length - 1) do
       newTableInfo[i] = tableInfo[i] + {tableInfo[i][1]}
     end     
     ModifyTable(tempTable,newTableInfo)
	 SetView(tempTable)
	 //curr_dec = GetFieldDecimals("tempTable.WALK_DIST")
	 //SetFieldDecimals("tempTable.WALK_DIST", curr_dec + 2)
     ExportView("TempTable|","CSV",pathArray[5] + "walkDist" + ".csv",,{{"CSV Header"},
																		{"Indexed Fields", {"TAZ", "TAZ:1"}},
																		{"Row Order", {{"TAZ", "Ascending"},
																		{"TAZ:1", "Ascending"}}}
																		})  
     CloseView(tempTable)
	 
     //RunProgram("cmd /c del \"" + pathArray[5] + outputname + ".mtx\"",)
     RunProgram("cmd /c del \"" + pathArray[5] + outputname + ".DCC\"",)
	// This loop closes all views:

	vws = GetViewNames()

	for i = 1 to vws.length do

		 CloseView(vws[i])

		 end
     RunProgram("cmd /c del \"" + pathArray[5] + outputname + ".mtx\"",)
	 RunProgram("cmd /c del \"" + pathArray[5] + outputname + "withExt" + ".mtx\"",)
     RunProgram("cmd /c del \"" + pathArray[5] + "walkDist" + ".DCC\"",)
	 RunProgram("cmd /c del \"" + pathArray[5] + outputname + ".bin\"",)
     RunProgram("cmd /c del \"" + pathArray[5] + outputname + ".DCB\"",)
endMacro

Macro "WalkTimeSkim" (season, pathArray, outputname)
    RunMacro("TCB Init")
    //Set up and run shortest path skim
     Opts = null
     Opts.Input.Network = pathArray[15] + "Tahoe_BikePed_Network.net"
     Opts.Input.[Origin Set] = {pathArray[14] + "bike_ped_links.DBD|bike_ped_nodes", "bike_ped_nodes", "Centroids", "Select * where Centroid <> null"}
     Opts.Input.[Destination Set] = {pathArray[1] + "bike_ped_links.DBD|bike_ped_nodes", "bike_ped_nodes", "Centroids"}
     Opts.Input.[Via Set] = {pathArray[14] + "bike_ped_links.DBD|bike_ped_nodes", "bike_ped_nodes"}
     Opts.Field.Minimize = "WALK_MINMILE" 
     Opts.Field.Nodes = "bike_ped_nodes.ID"
     //Opts.Field.[Skim Fields].Length = "All"
     Opts.Output.[Output Matrix].Label = "Walk_Time_Matrix"
     //Opts.Output.[Output Matrix].Compression = 1
     Opts.Output.[Output Matrix].[File Name] = pathArray[5] + outputname + ".mtx"
     ret_value = RunMacro("TCB Run Procedure", 1, "TCSPMAT", Opts)
     
     //Create intrazonal distances
     Opts = null
     Opts.Input.[Matrix Currency] = {pathArray[5] + outputname + ".mtx", "WALK_MINMILE",,}
	 Opts.Global.Factor = 1
     Opts.Global.Neighbors = 3
     Opts.Global.Operation = 1
     Opts.Global.[Treat Missing] = 1
     ret_value = RunMacro("TCB Run Procedure", 1, "Intrazonal", Opts)
	 
     Opts = null
	 Opts.Input.[Matrix Currency] = {pathArray[5] + outputname + ".mtx", "WALK_MINMILE",,}
	 Opts.Global.Factor = 1
     Opts.Global.Neighbors = 3
     Opts.Global.Operation = 1
     Opts.Global.[Treat Missing] = 1
     ret_value = RunMacro("TCB Run Procedure", 1, "Intrazonal", Opts)
	 
	 //Add TAZ Matrix Index on Row and Column
	 skimMatrix = OpenMatrix(pathArray[5] + outputname + ".mtx","True")
	 mtxinx = GetMatrixIndexNames(skimMatrix)
	 for i = 1 to mtxinx[1].length do
		if mtxinx[1][i] = "TAZ" then goto skiphere
	 end

     Opts = null
     Opts.Input.[Current Matrix] = skimMatrix
     Opts.Input.[Index Type] = "Both"
     Opts.Input.[View Set] = {{pathArray[14] + "bike_ped_links.dbd|bike_ped_nodes", pathArray[16] + "TAZ_BikePedExtTAZLookup.bin", {"Centroid"}, {"ID"}}, "bike_ped_nodes+TAZ", "Centroids", "Select * where Centroid <> null"}
     Opts.Input.[Old ID Field] = {{pathArray[14] + "bike_ped_links.dbd|bike_ped_nodes", pathArray[16] + "TAZ_BikePedExtTAZLookup.bin", {"Centroid"}, {"ID"}}, "bike_ped_nodes.ID"}
     Opts.Input.[New ID Field] = {{pathArray[14] + "bike_ped_links.dbd|bike_ped_nodes", pathArray[16] + "TAZ_BikePedExtTAZLookup.bin", {"Centroid"}, {"ID"}}, "TAZ"}
     Opts.Output.[New Index] = "TAZ"
     ret_value = RunMacro("TCB Run Operation", "Add Matrix Index", Opts, &Ret)

	 skiphere:
	 
	 	 
	 // Adding the External Zones skimmed
	 // Filled all cells with zeros to indicate infeasible Bike/Ped routes from external zones to Lake Tahoe internal zones by Ped/Bike (this will be taken care of in the UEC)
	 m2 = OpenMatrix(pathArray[9] + "ExternalDistanceMatrixBikePed.mtx", )
	 mc1 = CreateMatrixCurrency(skimMatrix, "WALK_MINMILE", "TAZ", "TAZ", )
	 mc2 = CreateMatrixCurrency(m2, "Table", "TAZ", "TAZ", )
	 new_mat = CombineMatrices({mc1, mc2}, {{"File Name", pathArray[5] + outputname + "withExt" + ".mtx"},
     {"Label", ""},
     {"Operation", "Union"}})
	 m2 = null
	 mc1 = null
	 mc2 = null
	 
	 m = OpenMatrix(pathArray[5] + outputname + "withExt" + ".mtx", )
	 SetMatrixIndexNames(m, {{"TAZ"}, {"TAZ"}})
	 mc1 = CreateMatrixCurrency(m, "WALK_MINMILE","TAZ" ,"TAZ" , )
	 mc1 := Nz(mc1)
	 
	 Opts = null
	 Opts.Input.[Input Matrix] = new_mat 
	 Opts.global.[Drop Core] = {"Table"}
	 RunMacro("TCB Run Operation", "Drop Matrix Core", Opts, &Ret)
	  //Save matrix core "BIKE_MINMILE" as csv using correct matrix index
     Opts = null
	 Opts.Input.[Input Matrix] = pathArray[5] + outputname + "withExt" + ".mtx" 
	 Opts.Input.[Target Core] = "WALK_MINMILE" 
	 Opts.Input.[Core Name] = "WALK_TIME"
	 RunMacro("TCB Run Operation", "Rename Matrix Core", Opts)
	 
	 SetMatrixCore(m, "WALK_TIME")
     SetMatrixIndex(m,"TAZ","TAZ")
	 AddMatrixCore(m,"Blank")
     mc = CreateMatrixCurrency(m,"Blank",,,)
     FillMatrix(mc,,,{"Copy",0},)
	 
	 matrix_indices = GetMatrixIndexNames(m)
     CreateTableFromMatrix(m, pathArray[5] + outputname + ".bin","FFB",{{"Complete","Yes"}})
     tempTable = OpenTable("TempTable","FFB",{pathArray[5] + outputname + ".bin",})
	 tableInfo = GetTableStructure(tempTable)
     dim newTableInfo[tableInfo.length - 1]
     for i = 1 to (tableInfo.length - 1) do
       newTableInfo[i] = tableInfo[i] + {tableInfo[i][1]}
     end     
     ModifyTable(tempTable,newTableInfo)
	 
	 SetView(tempTable) 
     ExportView("TempTable|","CSV",pathArray[5] + "walkTime" + ".csv",,{{"CSV Header"},
																		{"Indexed Fields", {"TAZ", "TAZ:1"}},
																		{"Row Order", {{"TAZ", "Ascending"},
																		{"TAZ:1", "Ascending"}}}
																		}) 
	 /*
	 
																		*/
     CloseView(tempTable)
	 
     RunProgram("cmd /c del \"" + pathArray[5] + outputname + ".mtx\"",)
	 RunProgram("cmd /c del \"" + pathArray[5] + outputname + "withExt" + ".mtx\"",)
     RunProgram("cmd /c del \"" + pathArray[5] + "walkTime" + ".DCC\"",)
	 RunProgram("cmd /c del \"" + pathArray[5] + outputname + ".bin\"",)
     RunProgram("cmd /c del \"" + pathArray[5] + outputname + ".DCB\"",)
	// This loop closes all views:
	 vws = GetViewNames()

	 for i = 1 to vws.length do

		 CloseView(vws[i])

		 end
		 

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
	javaPath = pathArray[8] + "java\\jdk1.8.0_73\\bin\\java.exe"
    RunProgram("cmd /s /c \"start \"cmd\" /D" + RunMacro("GetBaseDrive",pathArray) + ":\\ /WAIT \"" + javaPath + "\"" + javaClassPath + javaVMArguments + javaCommand + "\"",)

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
	javaPath = pathArray[8] + "java\\jdk1.8.0_73\\bin\\java.exe"
    RunProgram("cmd /s /c \"start \"cmd\" /D" + RunMacro("GetBaseDrive",pathArray) + ":\\ /WAIT \"" + javaPath + "\"" + javaClassPath + javaVMArguments + javaCommand + "\"",)
  
endMacro

//***********************************************************************************************


//***********************************************************************************************
///////////////////////////////////////////////////////////////////////////////////////////
//Traffic Assingment stuff below                                                         //
///////////////////////////////////////////////////////////////////////////////////////////

Macro "CreateTripMatrices"(pathArray)
    timePeriods = {"AM","MD","PM","LN"}
    modeSet = {"DA","SA","SH","WT","DT","WK","BK","SB"} // BK: If Walk and Bike will be assigned, "NM" should be replaced by "Bike" and "Walk", instead.
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
	if !ret_value then MessageBox("Assignment failed",)
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
    RunMacro("RunJavaModelPart",pathArray,vmSize,"TahoeModel")
EndMacro

Macro "RunModelOnlyPopulationSynthesis" (pathArray,vmSize)
    parts = {"Synpop",
             "AutoOwnership"}
    for i = 1 to parts.length do
        RunMacro("RunJavaModelPart",pathArray,vmSize,parts[i])
    end
EndMacro

Macro "RunModelSkippingPopulationSynthesis" (pathArray,vmSize)
    parts = {"DailyActivityPattern",
             "MandatoryDTM",
             "JointTourGeneration",
             "JointDTM",
             "NonMandatoryTourGeneration",
             "NonMandatoryDTM",
             "AtWorkDTM",
             "MandatoryStops",
             "JointStops",
             "NonMandatoryStops",
             "AtWorkStops",
             "ExternalWorkersSynpop",
             "ExternalWorkersOT",
             "OvernightVisitorSynpopAndPattern",
             "DayVisitorSynpopAndPattern",
             "VisitorDTM",
             "VisitorStops",
             "ThruVisitors",
             "TripSynthesize"}
    for i = 1 to parts.length do
        RunMacro("RunJavaModelPart",pathArray,vmSize,parts[i])
    end
EndMacro

Macro "RunJavaModelPart" (pathArray,vmSize,part)
    javaClassPath = " -classpath \"" + pathArray[8] + ";" + 
        pathArray[8] + "log4j-1.2.9.jar;" + 
        pathArray[8] + "jxl.jar;" + 
        pathArray[8] + "censusdata.jar;" + 
        pathArray[8] + "common-base.jar;" + 
        pathArray[8] + "synpop.jar;" + 
        pathArray[8] + "tahoe.jar\"" 
    javaVMArguments = " -Dlog4j.configuration=log4j_mine.xml -Xms" + vmSize + " -Xmx" + vmSize
	javaPath = pathArray[8] + "java\\jdk1.8.0_73\\bin\\java.exe"
    RunProgram("cmd /s /c \"start \"cmd\" /d" + RunMacro("GetBaseDrive",pathArray) + ":\\ /WAIT \"" + javaPath + "\"" + javaClassPath + javaVMArguments + " com.pb.tahoe.util.TahoeModelComponentRunner " + part + "\"",)
EndMacro

Macro "RunPopulationTransfer" (pathArray,scenarioName,summer)
    outputDirectory = pathArray[13] + "scenarios\\" + scenarioName + "\\outputs_winter\\"
    if summer = 1 then do
        outputDirectory = pathArray[13] + "scenarios\\" + scenarioName + "\\outputs_summer\\"
    end
    RunProgram("cmd /s /c \"python " + pathArray[8] + "tahoe_population_shift.py " + outputDirectory + "\"",)
EndMacro

Macro "GetBaseDrive" (pathArray)
    return(RunMacro("GetBaseDriveFromDir",pathArray[13]))
EndMacro

Macro "GetBaseDriveFromDir" (dir)
    return(left(dir,1))
EndMacro

Macro "TransitAssignment" (season,pathArray)
    amPeakPeriodTime = "_AM_IVTT"
    mdOffPeakPeriodTime = "_MD_IVTT"
    pmPeakPeriodTime = "_PM_IVTT"
    lnOffPeakPeriodTime = "_LN_IVTT"

    timeArray = {{{"SummerAMPeak","AM","[AB" + amPeakPeriodTime + " / BA" + amPeakPeriodTime + "]"," S AM Peak]",{100,0,2.18,2.18,6,13.38},"*" + amPeakPeriodTime,"SAMP","AB" + amPeakPeriodTime,"BA" + amPeakPeriodTime},
                  {"SummerMidday","MD","[AB" + mdOffPeakPeriodTime + " / BA" + mdOffPeakPeriodTime + "]"," S Midday]",{100,0,2.18,2.18,6,13.38},"*" + mdOffPeakPeriodTime,"SM","AB" + mdOffPeakPeriodTime,"BA" + mdOffPeakPeriodTime},
                  {"SummerPMPeak","PM","[AB" + pmPeakPeriodTime + " / BA" + pmPeakPeriodTime + "]"," S PM Peak]",{100,0,2.18,2.18,6,13.38},"*" + pmPeakPeriodTime,"SPMP","AB" + pmPeakPeriodTime,"BA" + pmPeakPeriodTime},
                  {"SummerLateNight","LN","[AB" + lnOffPeakPeriodTime + " / BA" + lnOffPeakPeriodTime + "]"," S Late Night]",{100,0,2.18,2.18,6,13.38},"*" + lnOffPeakPeriodTime,"SLN","AB" + lnOffPeakPeriodTime,"BA" + lnOffPeakPeriodTime}},
                 {{"WinterAMPeak","AM","[AB" + amPeakPeriodTime + " / BA" + amPeakPeriodTime + "]"," W AM Peak]",{100,0,2.18,2.18,6,13.38},"*" + amPeakPeriodTime,"WAMP","AB" + amPeakPeriodTime,"BA" + amPeakPeriodTime},
                  {"WinterMidday","MD","[AB" + mdOffPeakPeriodTime + " / BA" + mdOffPeakPeriodTime + "]"," W Midday]",{100,0,2.18,2.18,6,13.38},"*" + mdOffPeakPeriodTime,"WM","AB" + mdOffPeakPeriodTime,"BA" + mdOffPeakPeriodTime},
                  {"WinterPMPeak","PM","[AB" + pmPeakPeriodTime + " / BA" + pmPeakPeriodTime + "]"," W PM Peak]",{100,0,2.18,2.18,6,13.38},"*" + pmPeakPeriodTime,"WPMP","AB" + pmPeakPeriodTime,"BA" + pmPeakPeriodTime},
                  {"WinterLateNight","LN","[AB" + lnOffPeakPeriodTime + " / BA" + lnOffPeakPeriodTime + "]"," W Late Night]",{100,0,2.18,2.18,6,13.38},"*" + lnOffPeakPeriodTime,"WLN","AB" + lnOffPeakPeriodTime,"BA" + lnOffPeakPeriodTime}}}
    
    

    if season = "Summer" then do
      for i = 1 to timeArray[1].length do
        RunMacro("RunTransitAssignment", "Transit",pathArray,timeArray[1][i][1] + "TransitNetwork.tnw",timeArray[1][i][2],timeArray[1][i][3],timeArray[1][i][4],timeArray[1][i][5],timeArray[1][i][1] + "_walk_to_transit_assignment")
        RunMacro("RunTransitAssignment", "Drive2Transit",pathArray,timeArray[1][i][1] + "TransitNetwork.tnw",timeArray[1][i][2],timeArray[1][i][3],timeArray[1][i][4],timeArray[1][i][5],timeArray[1][i][1] + "_drive_to_transit_assignment")  
        
        RunMacro("AggregateTransitAssignment",RunMacro("GetTAggArray",pathArray,timeArray[1][i][1],"BoardAlight"))
        RunMacro("AggregateTransitAssignment",RunMacro("GetTAggArray",pathArray,timeArray[1][i][1],"DriveFlow"))
        RunMacro("AggregateTransitAssignment",RunMacro("GetTAggArray",pathArray,timeArray[1][i][1],"TranFlow"))
        
      end
    end
    if season = "Winter" then do
      for i = 1 to timeArray[2].length do
        RunMacro("RunTransitAssignment", "Transit",pathArray,timeArray[2][i][1] + "TransitNetwork.tnw",timeArray[2][i][2],timeArray[2][i][3],timeArray[2][i][4],timeArray[2][i][5],timeArray[2][i][1] + "_walk_to_transit_assignment")
        RunMacro("RunTransitAssignment", "Drive2Transit",pathArray,timeArray[2][i][1] + "TransitNetwork.tnw",timeArray[2][i][2],timeArray[2][i][3],timeArray[2][i][4],timeArray[2][i][5],timeArray[2][i][1] + "_drive_to_transit_assignment")  
        
        RunMacro("AggregateTransitAssignment",RunMacro("GetTAggArray",pathArray,timeArray[2][i][1],"BoardAlight"))
        RunMacro("AggregateTransitAssignment",RunMacro("GetTAggArray",pathArray,timeArray[2][i][1],"DriveFlow"))
        RunMacro("AggregateTransitAssignment",RunMacro("GetTAggArray",pathArray,timeArray[2][i][1],"TranFlow"))
      end
    end
endMacro

Macro "RunTransitAssignment" (type, pathArray, network, period, skimtime, fareheadway, costArray, outputname)
    RunMacro("TCB Init")
     //Setup Transit network for assignment
     Opts = null
     Opts.Input.[Transit RS] = pathArray[3] + "Tahoe_Transit.rts"
     Opts.Input.[Transit Network] = pathArray[2] + network
     Opts.Input.[Mode Table] = {pathArray[4] + "TahoeModeTable.bin"}
     Opts.Field.[Mode Used] = "TahoeModeTable.Mode_Used"
     Opts.Field.[Link Impedance] = skimtime
     Opts.Field.[Link Drive Time] = skimtime
     Opts.Field.[Route Fare] = "[Fare" + fareheadway
     Opts.Field.[Route Headway] = "[Headway" + fareheadway
     Opts.Field.[Mode Imp Weight] = "TahoeModeTable.Speed_Factor"
     Opts.Field.[Mode Speed] = "TahoeModeTable.Speed"
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
     

     //Run transit assignment
     Opts = null
     Opts.Input.[Transit RS] = pathArray[3] + "Tahoe_Transit.rts"
     Opts.Input.Network = pathArray[2] + network
     shortType = "WT"
     if type = "Drive2Transit" then do
        shortType = "DT"
     end
     Opts.Input.[OD Matrix Currency] = {pathArray[11] + "Trips_" + period + ".mtx", shortType, "Nodes", "Nodes"}
     Opts.Output.[Flow Table] = pathArray[5] + "Transit_Assignment\\" + outputname + "_flow.bin"
     Opts.Output.[Walk Flow Table] = pathArray[5] + "Transit_Assignment\\" + outputname + "_walk_flow.bin"
     Opts.Output.[OnOff Table] = pathArray[5] + "Transit_Assignment\\" + outputname + "_boardings_alightings.bin"
     ret_value = RunMacro("TCB Run Procedure", 2, "Transit Assignment PF", Opts)
     //CloseMap()

endMacro

Macro "GetTAggArray" (pathArray,timePeriod,type)
    if type = "BoardAlight" then do
        tAggArray = {
         {pathArray[5] + "Transit_Assignment\\" + timePeriod + "_walk_to_transit_assignment" + "_boardings_alightings",
          pathArray[5] + "Transit_Assignment\\" + timePeriod + "_drive_to_transit_assignment" + "_boardings_alightings"
         },
         {
          {"BoardStart","WalkAccessOn + [WalkAccessOn:1] + DriveAccessOn"},
          {"BoardStartW","WalkAccessOn + [WalkAccessOn:1]"},
          {"BoardStartD","DriveAccessOn"},
          {"AlightEnd","EgressOff + [EgressOff:1]"},
          {"BoardTrnsf","WalkTransferOn + [WalkTransferOn:1] + DirectTransferOn + [DirectTransferOn:1]"},
          {"BoardWTrnsf","WalkTransferOn + [WalkTransferOn:1]"},
          {"BoardDTrnsf","DirectTransferOn + [DirectTransferOn:1]"},
          {"AlightTrnsf","WalkTransferOff + [WalkTransferOff:1] + DirectTransferOff + [DirectTransferOff:1]"},
          {"AlightWTrnsf","WalkTransferOff + [WalkTransferOff:1]"},
          {"AlightDTrnsf","DirectTransferOff + [DirectTransferOff:1]"}      
         },
         pathArray[5] + "Transit_Assignment\\" + timePeriod + "boardAlight",
         2,
         {"STOP"}
        }
        return(tAggArray)    
    end
    if type = "DriveFlow" then do
        tAggArray = {
         {pathArray[5] + "Transit_Assignment\\" + timePeriod + "_walk_to_transit_assignment" + "_walk_flow",
          pathArray[5] + "Transit_Assignment\\" + timePeriod + "_drive_to_transit_assignment" + "_walk_flow"
         },
         {
          {"AB_Drive","AB_Drive_Flow"},
          {"BA_Drive","BA_Drive_Flow"},
          {"Tot_Drive","TOT_Drive_Flow"}
          //,
          //{"AB_Walk","AB_Walk_Flow + AB_Flow"},
          //{"BA_Walk","BA_Walk_Flow + BA_Flow"},
          //{"Tot_Walk","TOT_Walk_Flow + TOT_Flow"}     
         },
         pathArray[5] + "Transit_Assignment\\" + timePeriod + "linkFlow",
         1,
         {"ID1"}
        }
        return(tAggArray)    
    end
    if type = "TranFlow" then do
        tAggArray = {
         {pathArray[5] + "Transit_Assignment\\" + timePeriod + "_walk_to_transit_assignment" + "_flow",
          pathArray[5] + "Transit_Assignment\\" + timePeriod + "_drive_to_transit_assignment" + "_flow"
         },
         {
          {"TransitFlow","FLOW + [FLOW:1]"}
         },
         pathArray[5] + "Transit_Assignment\\" + timePeriod + "transitFlow",
         6,
         {"UID","FROM_STOP*10000 + TO_STOP"}
        }
        return(tAggArray)    
    end
endMacro

Macro "AggregateTransitAssignment" (tAggArray)
    
    wt = OpenTable("WalkToTransit","FFB",{tAggArray[1][1] + ".bin",})
    dt = OpenTable("DriveToTransit","FFB",{tAggArray[1][2] + ".bin",})
    
    if tAggArray[5].length > 1 then do
        //add index field to data
        ta = {wt,dt}
        for i = 1 to ta.length do
            stct = GetTableStructure(ta[i])
            for j = 1 to stct.length do
                stct[j] = stct[j] + {stct[j][1]}
            end 
            stct = stct + {{tAggArray[5][1], "Real", 12, 2, "True", , , , , , , null}}
            ModifyTable(ta[i],stct)
            RunMacro("TCB Init")
            Opts = null
            Opts.Input.[Dataview Set] = {tAggArray[1][i] + ".bin", "TotalTransit"}
            Opts.Global.Fields = {tAggArray[5][1]}
            Opts.Global.Method = "Formula"
            Opts.Global.Parameter = tAggArray[5][2]
            RunMacro("TCB Run Operation", 1, "Fill Dataview", Opts)
        end
        wt = OpenTable("WalkToTransit","FFB",{tAggArray[1][1] + ".bin",})
        dt = OpenTable("DriveToTransit","FFB",{tAggArray[1][2] + ".bin",})
    end
    
    
    tt = JoinViews("TestView",wt + "." + tAggArray[5][1],dt + "." + tAggArray[5][1],)
    ExportView(tt + "|","FFB",tAggArray[3] + "temp.bin",,)
    tt = OpenTable("TotalTransit","FFB",{tAggArray[3] + "temp.bin",})
    strct = GetTableStructure(tt)
    origLength = strct.length
    for i = 1 to origLength do
        strct[i] = strct[i] + {strct[i][1]}
    end
    for i = 1 to tAggArray[2].length do
        strct = strct + {{tAggArray[2][i][1], "Real", 12, 2, "True", , , , , , , null}}
    end
    ModifyTable(tt,strct)
    for i = 1 to tAggArray[2].length do
        RunMacro("TCB Init")
        Opts = null
        Opts.Input.[Dataview Set] = {tAggArray[3] + "temp.bin", "TotalTransit"}
        Opts.Global.Fields = {tAggArray[2][i][1]}
        Opts.Global.Method = "Formula"
        Opts.Global.Parameter = tAggArray[2][i][2]
        RunMacro("TCB Run Operation", 1, "Fill Dataview", Opts)
    end
    
    names = {}
    for i = 1 to tAggArray[4] do
        temp = strct[i][1]
        if Substring(temp,StringLength(temp) - 3,) = ":1]" then do
            temp = Substring(temp,2,StringLength(temp) - 4)
        end
        if i = 1 then do
            names = {temp}
        end
        else do
            names = names + {temp}
        end
    end
    for i = (origLength + 1) to strct.length do
        names = names + {strct[i][1]}
    end
    
    ExportView(tt + "|","FFB",tAggArray[3] + ".bin",names,)
    
    vws = GetViews()
    for i = 1 to vws[1].length do
        CloseView(vws[1][i])
    end
    DeleteFile(tAggArray[3] + "temp.bin")
    DeleteFile(tAggArray[3] + "temp.dcb")
    DeleteFile(tAggArray[3] + "temp.bx")
    DeleteFile(tAggArray[1][1] + ".bin")
    DeleteFile(tAggArray[1][1] + ".dcb")
    DeleteFile(tAggArray[1][2] + ".bin")
    DeleteFile(tAggArray[1][2] + ".dcb")
    if tAggArray[5].length > 1 then do
        DeleteFile(tAggArray[1][1] + ".bx")
        DeleteFile(tAggArray[1][2] + ".bx")
    end

endMacro

Macro "GetModelFilePath"
    program = GetInterface()
    uipaths = SplitPath(program)	
    model_file = uipaths[1]
    
    paths = ParseString(uipaths[2], "\\")
    for i = 1 to (paths.length - 1) do				//leave the last element, which is the interface folder name
    	model_file = model_file + "\\" + paths[i]
    end
    model_file = model_file + "\\TahoeStateFile.txt"
	return(model_file)
EndMacro

Macro "GetRootPath"
    program = GetInterface()
    uipaths = SplitPath(program)	
    root_folder = uipaths[1]
    
    paths = ParseString(uipaths[2], "\\")
    for i = 1 to (paths.length - 3) do				
    	root_folder = root_folder + "\\" + paths[i]
    end
	return(root_folder)
EndMacro