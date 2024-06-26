Macro "GetSpeedCalcs" (speed_calcs,query,formula)
    speed_calcs = speed_calcs + {{query,formula}}
    return(speed_calcs)
EndMacro

Macro "GetZoneFileViewName"
    return("__tazview__")
EndMacro

Macro "_GetExpressionFieldName" (index)
    return("__exp_" + i2s(index))
EndMacro

Macro "_GetIntrazonalSpeeds" (zone_file,taz_number_field,speed_calcs)
    taz_layer = GetDBLayers(zone_file)
    taz_layer = AddLayerToWorkspace(RunMacro("GetZoneFileViewName"),zone_file,taz_layer[1])
    SetLayer(taz_layer)
    taz_view = GetView()
    taz_speeds = null
    for i = 1 to speed_calcs.length do
        set = "_taz_temp"
        expr = CreateExpression(taz_view,"__tempexp__",speed_calcs[i][2],)
        //SelectByQuery(set,"several",speed_calcs[i][1])
        SelectByQuery(set,"several",speed_calcs[i][1],)
        rh = GetFirstRecord(taz_view + "|" + set,)
        while rh <> null do
            taz_speeds.(i2s(taz_view.(taz_number_field))) = r2s(taz_view.(expr))
            rh = GetNextRecord(taz_view + "|" + set,,)
        end
        //DestroyExpression(GetFieldFullSpec(taz_view,expr))
        DestroyExpression(RunMacro("GetFieldFullSpec",taz_view,expr))
    end
    DropLayerFromWorkspace(taz_layer)
    return(taz_speeds)
EndMacro

Macro "_GetTazRegions" (zone_file,taz_number_field,region_field,region_mapping)
    taz_layer = GetDBLayers(zone_file)
    taz_layer = AddLayerToWorkspace(RunMacro("GetZoneFileViewName"),zone_file,taz_layer[1])
    SetLayer(taz_layer)
    taz_view = GetView()
    taz_regions = null
    rh = GetFirstRecord(taz_view + "|",)
    while rh <> null do
        region = taz_view.(region_field)
        if region_mapping <> null then do
            for reg = 1 to region_mapping.length do
                if region_mapping[reg][1] = region then do
                    region = region_mapping[reg][2]
                    reg = region_mapping.length + 1 //get out of here
                end
            end
        end
        taz_regions.(i2s(taz_view.(taz_number_field))) = region
        rh = GetNextRecord(taz_view + "|",,)
    end
    DropLayerFromWorkspace(taz_layer)
    return(taz_regions)
EndMacro

Macro "_GetSortedTazs" (taz_mapping)
    tazs = null
    for i = 1 to taz_mapping.length do
        tazs = tazs + {s2i(taz_mapping[i][1])}
    end
    tazs = SortArray(tazs)
    for i = 1 to tazs.length do
        tazs[i] = i2s(tazs[i])
    end
    return(tazs)
EndMacro

Macro "_GetIntrazonalDistances" (distance_opts)
    if distance_opts.matrix then do
        distances = RunMacro("_GetIntrazonalDistancesFromMatrix",distance_opts.distance_file,distance_opts.distance,distance_opts.origin,distance_opts.destination)
    end
    else do
        distances = RunMacro("_GetIntrazonalDistancesFromTable",distance_opts.distance_file,distance_opts.distance,distance_opts.origin,distance_opts.destination)
    end
    return(distances)
EndMacro

Macro "_GetIntrazonalDistancesFromMatrix" (matrix_file,distance_core,origin_index,destination_index)
    ShowMessage("Not implemented...")
    return(null)
EndMacro

Macro "_GetIntrazonalDistancesFromTable" (file,distance_column,origin_column,destination_column)
    view = RunMacro("_OpenTable",file,"__distance__")
    rh = GetFirstRecord(view + "|",)
    distances = null
    while rh <> null do
        if view.(origin_column) = view.(destination_column) then do
            distances.(i2s(view.(origin_column))) = r2s(view.(distance_column))
        end
        rh = GetNextRecord(view + "|",,)
    end
    CloseView(view)
    return(distances)
EndMacro

Macro "GetDistanceOpts" (distance_file,distance_name,origin_name,destination_name)
    opts = null
    opts.matrix = False
    if Lower(right(distance_file,3)) = "mtx" then do
        opts.matrix = True
    end
    opts.distance_file = distance_file
    opts.distance = distance_name
    opts.origin = origin_name
    opts.destination = destination_name
    return(opts)
EndMacro

Macro "GetDefaultOpts" (default_distance,default_speed)
    opts = null
    opts.distance = default_distance
    opts.speed = default_speed
    return(opts)
EndMacro

Macro "GetTazMappingOpts" (network_file,taz_field,taz_exclusion_value,region_field,region_field_mapping)
    opts = null
    opts.network_file = network_file
    opts.taz_field = taz_field
    opts.taz_exclusion_value = taz_exclusion_value
    opts.region_field = region_field
    opts.region_field_mapping = region_field_mapping
    return(opts)
EndMacro

Macro "ExportIntrazonalToCsv" (taz_file,taz_number_field,speed_calcs,distance_opts,taz_mapping_opts,default_opts,output_file)
    network_file = taz_mapping_opts.network_file
    taz_field = taz_mapping_opts.taz_field
    taz_exclusion_value = taz_mapping_opts.taz_exclusion_value
    region_field = taz_mapping_opts.region_field
    region_field_mapping = taz_mapping_opts.region_field_mapping
    taz_mapping = RunMacro("GetTazMapping",network_file,taz_field,taz_exclusion_value)
    speeds = RunMacro("_GetIntrazonalSpeeds",taz_file,taz_number_field,speed_calcs)
    regions = RunMacro("_GetTazRegions",taz_file,taz_number_field,region_field,region_field_mapping)
    tazs = RunMacro("_GetSortedTazs",taz_mapping)
    distances = RunMacro("_GetIntrazonalDistances",distance_opts)
    header = "zone,distance,speed,region"
    f = OpenFile(output_file,"w")
    WriteLine(f,header)
    for i = 1 to tazs.length do
        taz = tazs[i]
        d = distances.(taz)
        s = speeds.(taz)
        r = regions.(taz)
        if d = null then do
            d = default_opts.distance
        end
        if s = null then do
            s = default_opts.speed
        end
        WriteLine(f,RunMacro("JoinStrings",{taz,d,s,r},","))
    end
    CloseFile(f)
EndMacro
