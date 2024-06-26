
Macro "GetTripOpts" (trip_opts,trip_matrix_file,time_period,vehicle_class,origin_index,destination_index,cores)
    topts = trip_opts.(time_period)
    vopts = topts.(vehicle_class)
    vopts.file = trip_matrix_file
    vopts.origin = origin_index
    vopts.destination = destination_index
    vopts.cores = cores
    topts.(vehicle_class) = vopts
    trip_opts.(time_period) = topts
    return(trip_opts)
EndMacro

Macro "ExportTripsToCsvFromTripFile" (trip_file,intrazonal_file,output_file,temp_file,mode_map,period_map,skim_map,fields)
    //mode_map is spec: trip mode -> output mode
    //period_map is spec: hour -> skim
    //skim_map is spec: skim index -> skim
    {i_field,j_field,mode_field,hour_field,skim_field} = fields
    
    smap = null
    for i = 1 to skim_map.length do
        skim = skim_map[i][2]
        s = null
        for j = 1 to period_map.length do
            if period_map[j][2] = skim then do
                s = s + {period_map[j][1]}
            end
        end
        smap.(i2s(i)) = {1.0 / s.length} + s
    end
    modes = null
    for i = 1 to mode_map.length do
        m = null
        for j = 1 to modes.length do
            if modes[j] = mode_map[i][2] then do
                m = modes[j]
            end
        end
        if m = null then do
            modes = modes + {mode_map[i][2]}
        end
    end
    
    view = OpenTable("iz","CSV",{intrazonal_file})
    index_spec = {view + "|",view + ".zone","rc_index"}
    opts = null
    opts.[File Name] = temp_file
    opts.Type = "Float"
    hours = null
    for m = 1 to modes.length do
        for i = 0 to 23 do
            hours = hours + {modes[m] + i2s(i)}
        end
    end
    opts.Tables = hours
    mat = CreateMatrix(index_spec,,opts)
    for h = 1 to hours.length do
        mc = CreateMatrixCurrency(mat,hours[h],"rc_index","rc_index",)
        mc := Nz(mc)
    end
    f = OpenFile(trip_file,"r")
    //header = ParseString(Trim(ReadLine(f)),",",{{"Include Empty",True}})
    ln = Substitute(Substitute(Trim(ReadLine(f)),",,",", ,",),",,",", ,",)
    header = ParseString(ln,",")
    for i = 1 to header.length do
        if header[i] = " " then do
            header[i] = ""
        end
    end
    for i = 1 to header.length do
        if header[i] = mode_field then do
            mi = i
        end
        if header[i] = hour_field then do
            hi = i
        end
        if header[i] = i_field then do
            ii = i
        end
        if header[i] = j_field then do
            ji = i
        end
        if header[i] = skim_field then do
            si = i
        end
    end
    reset = False
    p = GetProgram()
    if p[5] < 5 then do
        reset = True
    end
    counter = 0
    while not FileAtEOF(f) do
        counter = counter + 1
        if reset then do
            pos = FileGetPosition(f)-1
            FileSetPosition(f,pos)
            line = ReadLine(f)
            dist = FileGetPosition(f) - pos - 2
            if dist <> Len(line) then do
                line = ReadLine(f)
            end
            line = Trim(line)
        end
        else do
            line = Trim(ReadLine(f))
        end
        if Len(line) > 0 then do
            //line = ParseString(line,",",{{"Include Empty",True}})
            ln = Substitute(Substitute(line,",,",", ,",),",,",", ,",)
            line = ParseString(ln,",")
            for i = 1 to line.length do
                if line[i] = " " then do
                    line[i] = ""
                end
            end
            if line.length < 15 then do
                l1 = ReadLine(f)
                l2 = ReadLine(f)
                ShowArray({ln,line,last_line,last_ln,counter,l1,l2})
            end
            mode = mode_map.(line[mi])
            if mode <> null then do
                i = line[ii]
                j = line[ji]
                hour = s2i(line[hi])
                if hour < 0 then do
                    s = smap.(line[si])
                    v = s[1]
                    for h = 2 to s.length do
                        sh = s[h]
                        mc = CreateMatrixCurrency(mat,mode + sh,"rc_index","rc_index",)
                        SetMatrixValue(mc,i,j,GetMatrixValue(mc,i,j)+v)
                    end
                end
                else do
                    mc = CreateMatrixCurrency(mat,mode + line[hi],"rc_index","rc_index",)
                    SetMatrixValue(mc,i,j,GetMatrixValue(mc,i,j)+1)
                end
            end
            last_line = line
            last_ln = ln
        end
    end
    CloseFile(f)
    
    header = "origin_zone,destination_zone,hour,time_period,vehicle_class,trips"
    f = OpenFile(output_file,"w")
    WriteLine(f,header)
    ids = GetMatrixIndexIDs(mat,"rc_index")
    for m = 1 to modes.length do
        mode = modes[m]
        for h = 0 to 23 do
            hh = i2s(h)
            skim = period_map.(hh)
            mc = CreateMatrixCurrency(mat,mode + hh,"rc_index","rc_index",)
            for i = 1 to ids.length do
                ii = i2s(ids[i])
                for j = 1 to ids.length do
                    jj = i2s(ids[j])
                    WriteLine(f,RunMacro("JoinStrings",{ii,jj,hh,skim,mode,r2s(GetMatrixValue(mc,ii,jj))},","))
                end
            end
        end
    end
    CloseFile(f)
EndMacro

Macro "ExportTripsToCsv" (trip_opts,output_file)
    header = "origin_zone,destination_zone,hour,time_period,vehicle_class,trips"
    f = OpenFile(output_file,"w")
    WriteLine(f,header)
    for i = 1 to trip_opts.length do
        time_period = trip_opts[i][1]
        sub_opts = trip_opts[i][2]
        for j = 1 to sub_opts.length do
            vehicle_class = sub_opts[j][1]
            opts = sub_opts[j][2]
            matrix = OpenMatrix(opts.file,)
            currencies = null
            for k = 1 to opts.cores.length do
                currencies = currencies + {CreateMatrixCurrency(matrix,opts.cores[k],opts.origin,opts.destination,)}
            end
            os = GetMatrixIndexIDs(matrix,opts.origin)
            ds = GetMatrixIndexIDs(matrix,opts.destination)
            for o = 1 to os.length do
                origin = i2s(os[o])
                for d = 1 to ds.length do
                    destination = i2s(ds[d])
                    trips = 0.0
                    for c = 1 to currencies.length do
                        trips = trips + GetMatrixValue(currencies[c],origin,destination)
                    end
                    WriteLine(f,RunMacro("JoinStrings",{origin,destination,"-1",time_period,vehicle_class,r2s(trips)},","))
                end
            end

        end
    end
    CloseFile(f)
EndMacro
