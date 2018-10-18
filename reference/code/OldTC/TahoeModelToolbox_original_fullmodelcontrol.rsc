Macro "TahoeABModel"
    RunDbox("TahoeDbox")
EndMacro

Dbox "TahoeDbox" right, bottom title: "Tahoe Activity-Based Model" Toolbox NoKeyboard
	init do
		RunMacro("TCB Init")
		//Load state
		shared stateArray
		stateArray = RunMacro("LoadState")
                path = stateArray[1]
                basePath = stateArray[3]
                referencePath = basePath + "reference\\"
		javaPath = referencePath + "code\\"
		pathArray = RunMacro("updatePath",stateArray[3],stateArray[1])
		//transitNetworkPath = path + "gis\\Transit_Networks\\"
		//streetLayerPath = path + "gis\\Layers\\Streets\\"
		//networkPath = path + "gis\\Networks\\"
		//transitRoutesPath = path + "gis\\Transit_Route_System\\"
		//modeTablePath = path + "gis\\Skims\\Data_Files\\"
		//outputPath = path + "Data\\Skims\\"
		//mapPath = path + "gis\\Maps\\"
		//externalDistanceMatrixPath = modeTablePath
		//csvTripTablePath = path + "gis\\Skims\\Data_Files\\TripTables\\"
		//tripMatrixPath = path + "gis\\Skims\\Data_Files\\TripTables\\"
		//assignmentOutputPath = path + "gis\\Skims\\Traffic_Assignment\\"
		//pathArray = {streetLayerPath, transitNetworkPath, transitRoutesPath, modeTablePath, outputPath, networkPath, mapPath, javaPath, externalDistanceMatrixPath, csvTripTablePath, tripMatrixPath, assignmentOutputPath, basePath}
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
		
		RunMacro("updateproperty", referencePath,path,season_idx)
		enditem

	close do
	    RunMacro("CloseActions",path,season_idx,cr_user_iters,model_iters,javaPath,scenario_list,scenario_idx)
	    RunMacro("close everything")
            return()
            enditem

//Logo: User should see the TRPA logo	
	Button "icon_logo" 1, 1 icon: pathArray[13] + "reference\\img\\logoCrop.bmp" do
		ShowMessage("Travel Model Sponsored by TRPA.  For more info see www.trpa.org")
		enditem

// Reconfigure: User can specify path to Java code
    Button "Reconfigure" 56.5, 0.5, ,.75 do
        stateArray = RunDbox("configure",stateArray)
    enditem

//Create new scenario: User can create a new scenario
    Button "New Scenario Button" 52.2, 5.5, 14, 2 prompt: "New Scenario" do
        newScenarioList = RunDbox("NewScenarioCreator",pathArray,scenario_list)
        if newScenarioList.length > scenario_list.length then do
            scenario_list = newScenarioList
            scenario_idx = scenario_list.length
            path = basePath + "scenarios\\" + scenario_list[scenario_idx] + "\\"
            pathArray = RunMacro("updatePath",basePath,path)
            RunMacro("updateproperty",referencePath,path,season_idx)
        end
    endItem

//Path: User will select their data directory.
//	Text 6, 6, 35 variable: path prompt: "Path" framed	
//	Button "Browse..." 43, 6  do
//		path0 = path
//		path = ChooseDirectory("Choose a Model Directory", )
//		path = path + "\\"
//		transitNetworkPath = path + "gis\\Transit_Networks\\"
//		streetLayerPath = path + "gis\\Layers\\Streets\\"
//		networkPath = path + "gis\\Networks\\"
//		transitRoutesPath = path + "gis\\Transit_Route_System\\"
//		modeTablePath = path + "gis\\Skims\\Data_Files\\"
//		outputPath = path + "Data\\Skims\\"
//		mapPath = path + "gis\\Maps\\"
//		externalDistanceMatrixPath = modeTablePath
//		csvTripTablePath = path + "gis\\Skims\\Data_Files\\TripTables\\"
//		tripMatrixPath = path + "gis\\Skims\\Data_Files\\TripTables\\"
//		assignmentOutputPath = path + "gis\\Skims\\Traffic_Assignment\\"
//		pathArray = {streetLayerPath, transitNetworkPath, transitRoutesPath, modeTablePath, outputPath, networkPath, mapPath, javaPath, externalDistanceMatrixPath, csvTripTablePath, tripMatrixPath, assignmentOutputPath, basePath}
//		if (path0 <> path) then do
//			value = RunMacro("updateproperty",referencePath,path,season_idx)
//			path0 = path
//		end
//	enditem
//Choose scenario
    Popdown Menu "Scenario Chooser" 15, 6, 35 prompt: "Choose Scenario" List: scenario_list variable: scenario_idx do
        path = basePath + "scenarios\\" + scenario_list[scenario_idx] + "\\"
        pathArray = RunMacro("updatePath",basePath,path)
        RunMacro("updateproperty",referencePath,path,season_idx)
    endItem

//Summer/Winter: User will select the season to which the model is applied.
	Radio List 1, 8, 50, 3 Prompt:"Which season the model is applied to?"
		Variable: season_idx
	Radio Button 8, 9.5 Prompt:"Summer" do
		value = RunMacro("updateproperty", referencePath,path,season_idx)
	endItem
	Radio Button 31, 9.5 Prompt:"Winter" do
		value = RunMacro("updateproperty", referencePath,path,season_idx)
	endItem

//Network Skimming: User will press the first button to see the highway and transit skims and the second button to create 
//highway/transit network and skim both networks.
	Button "icon_assg" 1, 13 icon: pathArray[13] + "reference\\img\\big-networkskim.bmp" do
		showmessage("Open some files")
		enditem
	Button "Run Network Skimming" 16, 13, 35, 2 do
		RunMacro("PreModelRunner",pathArray,season_idx)
		enditem

//Resident Sub-Model: User will press the first button to see the matrix (?) for resident trips and the second button 
//to run the sub-model. The scroll list below is for users to run any individual step in the sub-model.
	Button "icon_assg" 1, 16 icon: pathArray[13] + "reference\\img\\big-residentialloc.bmp" do
		showmessage("Open some files")
		enditem
	Button "Run Resident Sub-Model" 16, 16, 35, 2 do
		RunMacro("SubModelRunner",res_array,pathArray,season_idx,30,"1")
		enditem
	Text 16, 18.5 Variable: "Double-click to run each step in sequence"
	Scroll List "scroll1" 16, 19.5, 36, 5 List: res_array
		Variables: resident_idx, click 
		do
			if click = 1 then do  // double click
				if (res_id >= resident_idx - 1) then do
					RunMacro("SubModelRunner",res_array,pathArray,season_idx,resident_idx+10,"1")
					//showmessage("Run " + res_array[resident_idx] + ", and the index is " + string(resident_idx))
					res_id = resident_idx
					end
				else do
					return_value=RunDbox("submodel_opt", "resident",resident_idx,res_id,res_array)
					if return_value=1 then do
						RunMacro("SubModelRunner",res_array,pathArray,season_idx,resident_idx+10,"1")
						end
					end
				end
			EndItem


//External Workers Sub-Model: User will press the first button to see the matrix (?) for external worker trips and the second button 
//to run the sub-model. The scroll list below is for users to run any individual step in the sub-model.
	Button "icon_assg" 1, 25.5 icon: pathArray[13] + "reference\\img\\big-workplaceloc.bmp" do
		showmessage("Open some files")
		enditem
	Button "Run External Workers Sub-Model" 16, 25.5, 35, 2 do
		RunMacro("SubModelRunner",vis_array,pathArray,season_idx,110,"3")
		enditem
	Text 16, 28 Variable: "Double-click to run each step in sequence"
	Scroll List "scroll2" 16, 29, 36, 2.6 List: ext_array
		Variables: ext_idx, click 
		do
			if click = 1 then do  // double click
				if (ext_id >= visitor_idx - 1) then do
					RunMacro("SubModelRunner",ext_array,pathArray,season_idx,ext_idx+100,"3")
					//showmessage("Run " + ext_array[ext_idx] + ", and the index is " + string(ext_idx))
					ext_id = ext_idx
					end
				else do
					return_value=RunDbox("submodel_opt","external",ext_idx,ext_id,ext_array)
					if return_value=1 then do
						RunMacro("SubModelRunner",ext_array,pathArray,season_idx,ext_idx+100,"3")
						end
					end
				end
			EndItem


//Visitor Sub-Model: User will press the first button to see the matrix (?) for visitor trips and the second button 
//to run the sub-model. The scroll list below is for users to run any individual step in the sub-model.
	Button "icon_assg" 1, 33 icon: pathArray[13] + "reference\\img\\big-airplane.bmp" do
		showmessage("Open some files")
		enditem
	Button "Run Visitor Sub-Model" 16, 33, 35, 2 do
		RunMacro("SubModelRunner",vis_array,pathArray,season_idx,210,"2")
		enditem
	Text 16, 35.5 Variable: "Double-click to run each step in sequence"
	Scroll List "scroll2" 16, 36.5, 36, 5 List: vis_array
		Variables: visitor_idx, click 
		do
			if click = 1 then do  // double click
				if (vis_id >= visitor_idx - 1) then do
					RunMacro("SubModelRunner",vis_array,pathArray,season_idx,visitor_idx+200,"2")
					//showmessage("Run " + vis_array[visitor_idx] + ", and the index is " + string(visitor_idx))
					vis_id = visitor_idx
					end
				else do
					return_value=RunDbox("submodel_opt","visitor",visitor_idx,vis_id,vis_array)
					if return_value=1 then do
						RunMacro("SubModelRunner",vis_array,pathArray,season_idx,visitor_idx+200,"2")
						end
					end
				end
			EndItem
			


//ASSN: User will press the first button to see the OD matrix
//and the second button will perform the assignment.
	Button "icon_assg" 1, 42.5 icon: pathArray[13] + "reference\\img\\big-assignment.bmp" do
		showmessage("Open some files")
		enditem
	Button "Run Traffic Assignment" 16, 42.5, 35, 2 do
		//showmessage("Here's to run traffic assignment.")
		RunMacro("JavaSubModelCode",season, pathArray,301)
		RunMacro("CreateTripMatrices",pathArray)
		RunMacro("TrafficAssignment",pathArray,cr_user_iters)
		enditem
	//Text 62.5, 33,,1 prompt: "Max # Assignment Iterations" 
	Text 64.5, 42.5,,1 prompt: "Max Iterations" 
	Edit Int 55, 43.5, 4, 1 variable: cr_user_iters 
//	do
//		ShowMessage("Iterations for Capacity Restraint assignment was set to " + IntToString(cr_user_iters))
//		enditem

//All: User presses this button to run all components
 	//Button "icon_all" 1, 37 icon: pathArray[13] + "reference\\img\\big-combo.bmp" do
	//	showmessage("Open some files")
	//	enditem
 	Button "Run Full Model" 16, 46, 35, 2.5 do
		RunMacro("PreModelRunner",pathArray,season_idx)
		RunMacro("SaveLinkData",pathArray,scenario_list[scenario_idx],season_idx,0)
		for i = 1 to model_iters do
		    RunMacro("JavaSubModelCode",season, pathArray,401)
		    RunMacro("CreateTripMatrices",pathArray)
		    RunMacro("TrafficAssignment",pathArray,cr_user_iters)
		    RunMacro("RunTransitSkims",pathArray,season_idx)
		    RunMacro("SaveLinkData",pathArray,scenario_list[scenario_idx],season_idx,i)
		    //if (some sort of criterion check) then do
		    //    i = model_iters + 1
		    //end
		end
		enditem
	//Text 62.5, 35,,1 prompt: "Max # Feedback Iterations" 
	Text 62.5, 45.5,,1 prompt: "Feedback"
	Text 62.5, 46.5,,1 prompt: "Iterations" 
	Edit Int 55, 47.5, 4, 1 variable: model_iters
	
//Clear: Uncheck everything done in the interface
	Button "Clear" 6, 50, 20, 1.5 do
		season_idx = 1
		resident_idx = 0
		visitor_idx = 0
		cr_user_iters = 50
		model_iters = 5
		//path = path0
		path = ""
		scenario_idx = 0
	Enditem

//Quit: User will press this button to exit
	Button "Quit" 34, 50, 20, 1.5 cancel do
	        RunMacro("CloseActions",path,season_idx,cr_user_iters,model_iters,javaPath,scenario_list,scenario_idx)
		RunMacro("close everything")
		Return()
		Enditem

//This is only to leave some room after the "Quit" button.
	Text 64, 51.5,,1 prompt: "" 

EndDbox

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

Macro "CloseActions" (scenarioPath,season_idx,cr_user_iters,model_iters,javaPath,scenario_list,scenario_idx)
    shared stateArray
    stateArray[1] = scenarioPath
    stateArray[2] = season_idx
    stateArray[4] = cr_user_iters
    stateArray[5] = model_iters
    stateArray[6] = scenario_idx
    backupFile = OpenFile("TahoeModelRunnerBackup.txt","w")
    WriteArray(backupFile,stateArray)
    CloseFile(backupFile)
    scenario_file = OpenFile(javaPath + "scenario_list.txt", "w")
    writearray(scenario_file,scenario_list)
    CloseFile(scenario_file)
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
	//javaCommand = " com.pb.tahoe.util.CreateNewScenario \"" + 
	//                 pathArray[13] + "scenarios\\ " + 
	//                  scenarioName + " " +  
	//                  pathArray[13] + "reference\\scenario_base.zip\""
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
      //update hwy network
      
      RunMacro("CreateDriveNetwork",pathArray)
      
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

Macro "JavaSubModelCode" (season, pathArray,run_id)
	javaClassPath = " -classpath \"" + pathArray[8] + ";" + 
			pathArray[8] + "log4j-1.2.9.jar;" + 
			pathArray[8] + "jxl.jar;" + 
			pathArray[8] + "censusdata.jar;" + 
			pathArray[8] + "common-base.jar;" + 
			pathArray[8] + "synpop.jar;" + 
			pathArray[8] + "tahoe.jar\"" 
	javaVMArguments = " -Dlog4j.configuration=log4j_mine.xml -Xms1100m -Xmx1200m"
	javaCommand = null
	
        if run_id=11 then do
//		javaCommand = " com.pb.tahoe.synpop.test.SynPopTest"
            javaCommand = " com.pb.tahoe.util.TahoeModelComponentRunner Synpop"
        end
        if run_id=12 then do
//		javaCommand = " com.pb.tahoe.auto_ownership.AutoOwnership"
            javaCommand = " com.pb.tahoe.util.TahoeModelComponentRunner AutoOwnership"
        end
        if run_id=13 then do
//		javaCommand = " com.pb.tahoe.daily_activity_pattern_test.DAPTest"
            javaCommand = " com.pb.tahoe.util.TahoeModelComponentRunner DailyActivityPattern"
        end
        if run_id=14 then do
//		javaCommand = " com.pb.tahoe.dest_time_mode_test.MandatoryDTMTest"
            javaCommand = " com.pb.tahoe.util.TahoeModelComponentRunner MandatoryDTM"
        end
        if run_id=15 then do
//		javaCommand = " com.pb.tahoe.joint_tour_test.JointToursModelTest"
            javaCommand = " com.pb.tahoe.util.TahoeModelComponentRunner JointTourGeneration"
        end
        if run_id=16 then do
//		javaCommand = " com.pb.tahoe.dest_time_mode_test.JointDTMTest"
            javaCommand = " com.pb.tahoe.util.TahoeModelComponentRunner JointDTM"
        end
        if run_id=17 then do
//		javaCommand = " com.pb.tahoe.individual_tour_test.IndivNonMandToursTest"
            javaCommand = " com.pb.tahoe.util.TahoeModelComponentRunner NonMandatoryTourGeneration"
        end
        if run_id=18 then do
//		javaCommand = " com.pb.tahoe.dest_time_mode_test.IndivNonMandDTMTest"
            javaCommand = " com.pb.tahoe.util.TahoeModelComponentRunner NonMandatoryDTM"
        end
        if run_id=19 then do
//		javaCommand = " com.pb.tahoe.dest_time_mode_test.AtWorkDTMTest"
            javaCommand = " com.pb.tahoe.util.TahoeModelComponentRunner AtWorkDTM"
        end
        if run_id=20 then do
            javaCommand = " com.pb.tahoe.util.TahoeModelComponentRunner MandatoryStops"
        end
        if run_id=21 then do
            javaCommand = " com.pb.tahoe.util.TahoeModelComponentRunner JointStops"
        end
        if run_id=22 then do
            javaCommand = " com.pb.tahoe.util.TahoeModelComponentRunner NonMandatoryStops"
        end
        if run_id=23 then do
            javaCommand = " com.pb.tahoe.util.TahoeModelComponentRunner AtWorkStops"
        end
        //entire visitor model id = 30
        if run_id=30 then do
            javaCommand = " com.pb.tahoe.util.TahoeModelComponentRunner Resident"
        end
        if run_id=101 then do
            javaCommand = " com.pb.tahoe.util.TahoeModelComponentRunner ExternalWorkersSynpop"
        end
        if run_id=102 then do
            javaCommand = " com.pb.tahoe.util.TahoeModelComponentRunner ExternalWorkersOT"
        end
        //entire external workers id = 110
        if run_id=110 then do
            javaCommand = " com.pb.tahoe.util.TahoeModelComponentRunner ExternalWorker"
        end
        if run_id=201 then do
            javaCommand = " com.pb.tahoe.util.TahoeModelComponentRunner OvernightVisitorSynpopAndPattern"
        end
        if run_id=202 then do
            javaCommand = " com.pb.tahoe.util.TahoeModelComponentRunner DayVisitorSynpopAndPattern"
        end
        if run_id=203 then do
            javaCommand = " com.pb.tahoe.util.TahoeModelComponentRunner VisitorDTM"
        end
        if run_id=204 then do
            javaCommand = " com.pb.tahoe.util.TahoeModelComponentRunner VisitorStops"
        end
        if run_id=205 then do
            javaCommand = " com.pb.tahoe.util.TahoeModelComponentRunner ThruVisitors"
        end
        //entire visitor model id = 210
        if run_id=210 then do
            javaCommand = " com.pb.tahoe.util.TahoeModelComponentRunner Visitor"
        end
        if run_id=301 then do
            javaCommand = " com.pb.tahoe.util.TahoeModelComponentRunner TripSynthesize"
        end
        //entire model id=401
        if run_id=401 then do
            javaCommand = " com.pb.tahoe.util.TahoeModelComponentRunner TahoeModel"
        end

	//showmessage(string(run_id))
	//showmessage(javaCommand)
	//showmessage("cmd /c java" + javaClassPath + javaVMArguments + javaCommand)
	if javaCommand<>null then do
		return_value = RunProgram("cmd /c java" + javaClassPath + javaVMArguments + javaCommand,)
        end
	return(return_value)
endMacro


