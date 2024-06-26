Macro "GetFieldOptions" (ab_field,ba_field)
    opts = null
    opts.ab_field = ab_field
    if ba_field = null then do //means same field for both directions
        opts.ba_field = ab_field
    end
    else do
        opts.ba_field = ba_field
    end
    return(opts)
EndMacro

Macro "GetJoinInfoOptions" (file_to_join,network_file_join_field,join_file_join_field)
    opts = null
    opts.join_file = file_to_join
    opts.network_field = network_file_join_field
    opts.join_field = join_file_join_field
    return(opts)
EndMacro

Macro "GetNewNetworkOptions" (node_id_field,id_field,length_field,link_class_field)
    opts = null
    opts.node_id = node_id_field
    opts.id = id_field
    opts.len = length_field
    opts.link_class = link_class_field
    opts.time_data = {}
    return(opts)
EndMacro

Macro "AddRegionDataToNetworkOptions" (network_options,region_field,region_mapping)
    //region mapping can be null, in which case reigon_field is used
    //if it exists, region mapping should be [[region_field_entry,region],...]
    network_options.region_field = region_field
    network_options.region_mapping = region_mapping
EndMacro

Macro "AddTimeDataToNetworkOptions" (network_options,time_period,vehicle_class,ivtt_field,volume_field,link_query,join_info)
    opts = null
    opts.vehicle_class = vehicle_class
    opts.ivtt = ivtt_field
    opts.volume = volume_field
    opts.link_query = link_query //this specifies the subset of links for this - set to null to use all links
    opts.join_info = join_info //this allows you to joing a table to the network for the specified data
    if network_options.time_data[1] = null then do
        sub_opts = null
        sub_opts.(vehicle_class) = opts
        time_opts = null
        time_opts.(time_period) = sub_opts
        network_options.time_data = time_opts
    end
    else do
        sub_opts = network_options.time_data.(time_period)
        sub_opts.(vehicle_class) = opts
        network_options.time_data.(time_period) = sub_opts
    end
EndMacro

Macro "GetNodeFileViewName"
    return("__nnodes__")
EndMacro

Macro "GetLinkFileViewName"
    return("__nlines__")
EndMacro

Macro "GetJoinFileViewName"
    return("__joinview__")
EndMacro

Macro "GetJoinedViewName"
    return("__networkjoin__")
EndMacro

Macro "GetTazMapping" (network_file,taz_field,taz_exclusion_value)
    node_mapping = null
    {node_layer,line_layer} = GetDBLayers(network_file)
    node_layer = AddLayerToWorkspace(RunMacro("GetNodeFileViewName"),network_file,node_layer)
    SetLayer(node_layer)
    view = GetView()
    rh = GetFirstRecord(node_layer + "|",)
    while rh <> null do
        taz = node_layer.(taz_field)
        if taz <> taz_exclusion_value then do
            node_mapping.(i2s(taz)) = i2s(node_layer.ID)
        end
        rh = GetNextRecord(node_layer + "|",,)
    end
    DropLayerFromWorkspace(node_layer)
    return(node_mapping)
EndMacro

Macro "_GetNodeMapping" (network_file,node_field)
    node_mapping = null
    {node_layer,line_layer} = GetDBLayers(network_file)
    node_layer = AddLayerToWorkspace(RunMacro("GetNodeFileViewName"),network_file,node_layer)
    SetLayer(node_layer)
    view = GetView()
    rh = GetFirstRecord(node_layer + "|",)
    while rh <> null do
        node_mapping.(i2s(node_layer.ID)) = i2s(node_layer.(node_field))
        rh = GetNextRecord(node_layer + "|",,)
    end
    DropLayerFromWorkspace(node_layer)
    return(node_mapping)
EndMacro

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

Macro "_GetJoinField" (fields_array,field,qualified_file_names)
    if RunMacro("GetArrayIndex",fields_array,field) = 0 then do
        tfield = qualified_file_names[1] + "." + field
        if RunMacro("GetArrayIndex",fields_array,tfield) = 0 then do
            tfield = qualified_file_names[1] + "." + field
            if RunMacro("GetArrayIndex",fields_array,tfield) = 0 then do
                ShowMessage("Cannot find " + field  + " with qualified names " + qualified_file_names[1] + " and " + qualified_file_names[2] + " in following list")
                ShowArray(fields_array)
                ShowMessage(2)
            end
        end
        field = tfield
    end
    return(field)
EndMacro

Macro "ExportNetworkIdToLengthCsv" (network_file,network_options,output_file)
    header = "id,length"
    f = OpenFile(output_file,"w")
    WriteLine(f,header)
    
    {node_layer,line_layer} = GetDBLayers(network_file)
    network_layer = AddLayerToWorkspace(RunMacro("GetLinkFileViewName"),network_file,line_layer)
    SetLayer(network_layer)
    view = GetView()
    SetView(view)
    rh = GetFirstRecord(view + "|",)
    while rh <> null do
        id = i2s(view.(network_options.id.ab_field))
        length = r2s(view.(network_options.len.ab_field))
        WriteLine(f,id + "," + length)
        rh = GetNextRecord(view + "|",,)
    end
    
    CloseFile(f)
    DropLayerFromWorkspace(network_layer)
EndMacro

Macro "ExportNetworkToCsv" (network_file,network_options,output_file)
    header = "from_node,to_node,length,link_class,time_period,vehicle_class,assigned_speed,volume,region"
    f = OpenFile(output_file,"w")
    WriteLine(f,header)
    
    node_mapping = RunMacro("_GetNodeMapping",network_file,network_options.node_id)
    {node_layer,line_layer} = GetDBLayers(network_file)
    network_layer = AddLayerToWorkspace(RunMacro("GetLinkFileViewName"),network_file,line_layer)
    SetLayer(network_layer)
    view = GetView()
    
    region_mapping = opts.region_mapping
    
    //loop over all time periods, then vehicle classes, and write data
    for i = 1 to network_options.time_data.length do
        time_opts = network_options.time_data[i][2]
        time_period = network_options.time_data[i][1]
        for j = 1 to time_opts.length do
            vehicle_class = time_opts[j][1]
            opts = time_opts[j][2]
            jview = view
            join_view = null
            id_field = "ID"
            if opts.join_info <> null then do
                join_view = RunMacro("_OpenTable",opts.join_info.join_file,RunMacro("GetJoinFileViewName"))
                //jview = JoinViews(RunMacro("GetJoinedViewName"),GetFieldFullSpec(view,opts.join_info.network_field),GetFieldFullSpec(join_view,opts.join_info.join_field),)
                jview = JoinViews(RunMacro("GetJoinedViewName"),RunMacro("GetFieldFullSpec",view,opts.join_info.network_field),RunMacro("GetFieldFullSpec",join_view,opts.join_info.join_field),)
                
                fields_array = GetFields(jview,"All")
                fields_array = fields_array[1]
                qual = {"[" + view + "]","[" + jview + "]"}
                id_field = RunMacro("_GetJoinField",fields_array,id_field,qual)
                network_options.len.ab_field = RunMacro("_GetJoinField",fields_array,network_options.len.ab_field,qual)
                network_options.link_class.ab_field = RunMacro("_GetJoinField",fields_array,network_options.link_class.ab_field,qual)
                opts.ivtt.ab_field = RunMacro("_GetJoinField",fields_array,opts.ivtt.ab_field,qual)
                opts.volume.ab_field = RunMacro("_GetJoinField",fields_array,opts.volume.ab_field,qual)
                network_options.len.ba_field = RunMacro("_GetJoinField",fields_array,network_options.len.ba_field,qual)
                network_options.link_class.ba_field = RunMacro("_GetJoinField",fields_array,network_options.link_class.ba_field,qual)
                opts.ivtt.ba_field = RunMacro("_GetJoinField",fields_array,opts.ivtt.ba_field,qual)
                opts.volume.ba_field = RunMacro("_GetJoinField",fields_array,opts.volume.ba_field,qual)
                network_options.region_field = RunMacro("_GetJoinField",fields_array,network_options.region_field,qual)
            end
            SetView(jview)
            
            set = ""
            if opts.link_query <> null then do
                set = "time_network"
                //SelectByQuery(set,"several",opts.link_query)
                SelectByQuery(set,"several",opts.link_query,)
            end
            rh = GetFirstRecord(jview + "|" + set,)
            while rh <> null do
                {a_node,b_node} = GetEndpoints(jview.(id_field))
                a_node = node_mapping.(i2s(a_node))
                b_node = node_mapping.(i2s(b_node))
                dir = GetDirection(jview.(id_field))
                region = jview.(network_options.region_field)
                if region_mapping <> null then do
                    for reg = 1 to region_mapping.length do
                        if region_mapping[reg][1] = region then do
                            region = region_mapping[reg][2]
                            reg = region_mapping.length + 1 //get out of here
                        end
                    end
                end
                if dir = 1 or dir = 0 then do
                    length = r2s(jview.(network_options.len.ab_field))
                    link_class = i2s(jview.(network_options.link_class.ab_field))
                    time = jview.(opts.ivtt.ab_field)
                    if time <> null and time > 0 then do
                        assigned_speed = r2s(jview.(network_options.len.ab_field) / time * 60.0)
                        volume = jview.(opts.volume.ab_field)
                        if volume <> null then do
                            volume = r2s(volume)
                            WriteLine(f,RunMacro("JoinStrings",{a_node,b_node,length,link_class,time_period,vehicle_class,assigned_speed,volume,region},","))
                        end
                    end
                end
                if dir = -1 or dir = 0 then do
                    length = r2s(jview.(network_options.len.ba_field))
                    link_class = i2s(jview.(network_options.link_class.ba_field))
                    time = jview.(opts.ivtt.ba_field)
                    if time <> null and time > 0 then do
                        assigned_speed = r2s(jview.(network_options.len.ba_field) / time * 60.0)
                        volume = jview.(opts.volume.ba_field)
                        if volume <> null then do
                            volume = r2s(volume)
                            WriteLine(f,RunMacro("JoinStrings",{b_node,a_node,length,link_class,time_period,vehicle_class,assigned_speed,volume,region},","))
                        end
                    end
                end
                rh = GetNextRecord(jview + "|" + set,,)
            end
            if join_view <> null then do
                CloseView(jview)
                CloseView(join_view)
            end
        end
    end
    CloseFile(f)
    DropLayerFromWorkspace(network_layer)
EndMacro

