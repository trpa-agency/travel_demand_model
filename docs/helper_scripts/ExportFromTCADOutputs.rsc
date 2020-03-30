Macro "runThis"
	shared scenDr 
	scenDr = "C:\\Projects\\19_182803A_TAHOE_PHASE_II\\80_WorkArea\\Calibration\\Runs\\52_TestDelivery"
	RunMacro("Export Bin To CSV", scenDr+"\\gis\\skims\\Transit_Assignment\\SummerAMPeakboardAlight.bin", scenDr+"\\post\\SummerAMPeakboardAlight.csv")
	RunMacro("Export Bin To CSV", scenDr+"\\gis\\skims\\Transit_Assignment\\SummerAMPeaklinkFlow.bin", scenDr+"\\post\\SummerAMPeaklinkFlow.csv")
	RunMacro("Export Bin To CSV", scenDr+"\\gis\\skims\\Transit_Assignment\\SummerAMPeaktransitFlow.bin", scenDr+"\\post\\SummerAMPeaktransitFlow.csv")
	RunMacro("Export Bin To CSV", scenDr+"\\gis\\skims\\Transit_Assignment\\SummerLateNightboardAlight.bin", scenDr+"\\post\\SummerLateNightboardAlight.csv")
	RunMacro("Export Bin To CSV", scenDr+"\\gis\\skims\\Transit_Assignment\\SummerLateNightlinkFlow.bin", scenDr+"\\post\\SummerLateNightlinkFlow.csv")
	RunMacro("Export Bin To CSV", scenDr+"\\gis\\skims\\Transit_Assignment\\SummerLateNighttransitFlow.bin", scenDr+"\\post\\SummerLateNighttransitFlow.csv")
	RunMacro("Export Bin To CSV", scenDr+"\\gis\\skims\\Transit_Assignment\\SummerMiddayboardAlight.bin", scenDr+"\\post\\SummerMiddayboardAlight.csv")
	RunMacro("Export Bin To CSV", scenDr+"\\gis\\skims\\Transit_Assignment\\SummerMiddaylinkFlow.bin", scenDr+"\\post\\SummerMiddaylinkFlow.csv")
	RunMacro("Export Bin To CSV", scenDr+"\\gis\\skims\\Transit_Assignment\\SummerMiddaytransitFlow.bin", scenDr+"\\post\\SummerMiddaytransitFlow.csv")
	RunMacro("Export Bin To CSV", scenDr+"\\gis\\skims\\Transit_Assignment\\SummerPMPeakboardAlight.bin", scenDr+"\\post\\SummerPMPeakboardAlight.csv")
	RunMacro("Export Bin To CSV", scenDr+"\\gis\\skims\\Transit_Assignment\\SummerPMPeaklinkFlow.bin", scenDr+"\\post\\SummerPMPeaklinkFlow.csv")
	RunMacro("Export Bin To CSV", scenDr+"\\gis\\skims\\Transit_Assignment\\SummerPMPeaktransitFlow.bin", scenDr+"\\post\\SummerPMPeaktransitFlow.csv")
	RunMacro("Export Bin To CSV", scenDr+"\\outputs_summer\\FullStreets_iter1.bin", scenDr+"\\post\\FullStreets.csv")	
	RunMacro("ExportMatrix",scenDr+"\\gis\\skims\\Data_Files\\TripTables\\",scenDr+"\\post","Trips_AM")	
	RunMacro("ExportMatrix",scenDr+"\\gis\\skims\\Data_Files\\TripTables\\",scenDr+"\\post","Trips_LN")	
	RunMacro("ExportMatrix",scenDr+"\\gis\\skims\\Data_Files\\TripTables\\",scenDr+"\\post","Trips_MD")	
	RunMacro("ExportMatrix",scenDr+"\\gis\\skims\\Data_Files\\TripTables\\",scenDr+"\\post","Trips_PM")	
	RunMacro("ExportShape",scenDr,"FullStreets")
	RunMacro("Export Bin To CSV",scenDr+"\\gis\\Transit_Route_System\\Tahoe_TransitR.bin", scenDr+"\\post\\Tahoe_TransitR.csv")

EndMacro



Macro "ExportShape" (dName,fName)

	db_file = dName+"\\gis\\Layers\\Streets\\"+fName+".dbd"
	lyrs = RunMacro("TCB Add DB Layers", db_file) 
	link_lyr = lyrs[2] 	
	SetView(link_lyr)
	qry1 = "Select * where 1=1"
	n1 = SelectByQuery("Highway Only", "Several", qry1, )	
	field_list = GetFields(link_lyr, "All")
	ExportArcViewShape(link_lyr + "|Highway Only", dName+"\\post\\"+"Streets"+".shp",  {{"Fields", field_list[1]}})

	node_lyr = lyrs[1] 	
	SetView(node_lyr)
	qry1 = "Select * where 1=1"
	n1 = SelectByQuery("Highway Only", "Several", qry1, )	
	field_list = GetFields(node_lyr, "All")
	ExportArcViewShape(node_lyr + "|Highway Only", dName+"\\post\\"+"Nodes"+".shp",  {{"Fields", field_list[1]}})

	
	//MessageBox("Done2",)

EndMacro

Macro "Export Bin To CSV" (bin_file, csv_file)
  input_bin = OpenTable("input_bin", "FFB", {bin_file})
  
  //cleanup and reformat data
  field_info = GetTableStructure(input_bin)
  field_names = null
 

  first = 1
  for i = 1 to field_info.length do
	name = field_info[i][1]
	//type = field_info[i][2]

	field_names = field_names + {name}
	
	name = upper(name)
	

	if first then header = name else header = header + "," + name
	first = 0
  end
  
  temp_file = Substitute(csv_file, ".csv", "_temp.csv", )
  ExportView(input_bin + "|", "CSV", temp_file, field_names,)
  CloseView(input_bin)
  
  f = OpenFile(csv_file,"w")
  t = OpenFile(temp_file,"r")

  WriteLine(f, header)
  
  while not FileAtEOF(t) do
	line = ReadLine(t)
	WriteLine(f, line)
  end
  
  CloseFile(f)
  CloseFile(t)
  
  if GetFileInfo(temp_file) <> Null then DeleteFile(temp_file)
  if GetFileInfo(Substitute(temp_file, ".csv", ".DCC", )) <> Null then DeleteFile(Substitute(temp_file, ".csv", ".DCC", ))
  
EndMacro




Macro "ExportMatrix" (locDr,outDr,fname)
	inMat   = locDr+fname+".mtx"		
	mat = OpenMatrix(inMat,)
	SetMatrixIndex(mat, "Nodes", "Nodes")
	export_csv_file = outDr+"\\"+fname+".csv"	
	Opts = null
	Opts.[Complete]   = "Yes"
	CreateTableFromMatrix(mat, export_csv_file, "CSV", Opts) 
EndMacro
