#.libPaths(c("../code/R-3.5.1/library"))
.libPaths(.libPaths()[2])
.libPaths()

args=commandArgs(trailingOnly=TRUE)

library(tidyverse)



scen_name = args[1]
scenFolder <- paste0('../../../scenarios/',scen_name)
print(scenFolder)
cw <- read_csv(paste0(scenFolder,"/gis/tahoe_geo_crosswalk.csv"))
cw[cw == "0"] <- NA
propFile  <- readLines(paste0(scenFolder,"/code/tahoe_summer.properties"))

checkReqString <- function(inpStr) {
    condCheck <- ifelse(substr(inpStr,1,min(34,nchar(inpStr)))=="include.externalDistance.in.report",1,0)
    if(condCheck==1){
        return(substr(inpStr,38,41))
    }
}
check <- map(propFile,checkReqString) %>% unlist()

includeExtDist <- 1*(check=='true')
print(includeExtDist)

PartyArray <- read_csv(paste0(scenFolder,"/outputs_summer/PartyArray_afterThruVisitors.pam"))

### read in trip file ###
trip_file_orig <- read_csv(paste0(scenFolder,"/outputs_summer/trip_file.csv")) #%>%     filter(startTaz>0 & endTaz>0)

bikeDist_col_types <- cols(
  TAZ = col_double(),
  `TAZ:1` = col_double(),
  BIKE_DIST = col_double() # Ensure BIKE_DIST is read as numeric
)

walkDist_col_types <- cols(
  TAZ = col_double(),
  `TAZ:1` = col_double(),
  WALK_DIST = col_double() # Ensure WALK_DIST is read as numeric
)
### incorporate new bike and walk skims ####
bikeDist<-read_csv(paste0(scenFolder,"/gis/Skims/bikeDist.csv"), col_types = bikeDist_col_types) %>%
  mutate(id=paste( TAZ, `TAZ:1`,sep="-"))
walkDist<-read_csv(paste0(scenFolder,"/gis/Skims/walkDist.csv"), col_types = walkDist_col_types)%>%
  mutate(id=paste( TAZ, `TAZ:1`,sep="-"))

trip_file<- trip_file_orig %>% 
  mutate(id=paste(startTaz, endTaz, sep="-"), id2=paste(startTaz, endTaz, skim, sep="-")) %>% 
  mutate(modeAgg = case_when(
    mode %in% c('drive alone','shared auto')~'drive',
    mode %in% c('drive to transit','walk to transit')~'transit',
    mode %in% c('visitor shuttle','school bus')~'other')) %>% 
  left_join(PartyArray %>% select(id,stayType,walkSegment), by=c("partyID"="id")) %>% 
  mutate(partyType=ifelse(partyType=='overnight visitor' & stayType==1,'seasonal',partyType)) %>%
  left_join(bikeDist %>% select(id, BIKE_DIST), by="id") %>%
  left_join(walkDist %>% select(id, WALK_DIST), by="id")

missing_ids <- trip_file %>%
  anti_join(bikeDist %>% select(id), by="id") %>%
  select(id)

print(missing_ids)

### read in and combine distances skim files ###
pm<-read_csv(paste0(scenFolder,"/gis/Skims/SummerPMPeakDriveDistanceSkim.csv")) %>%
  mutate(skim=2,id2=paste( TAZ,`TAZ:1`,skim, sep="-")) %>%
  rename(trip_time=`AB_PM_IVTT / BA_PM_IVTT`)
am<-read_csv(paste0(scenFolder,"/gis/Skims/SummerAMPeakDriveDistanceSkim.csv")) %>%
  mutate(skim=1,id2=paste(TAZ,`TAZ:1`,skim, sep="-")) %>%
  rename(trip_time=`AB_AM_IVTT / BA_AM_IVTT`)
ln<-read_csv(paste0(scenFolder,"/gis/Skims/SummerLateNightDriveDistanceSkim.csv")) %>%
  mutate(skim=4,id2=paste(TAZ,`TAZ:1`,skim, sep="-")) %>%
  rename(trip_time=`AB_LN_IVTT / BA_LN_IVTT`)
md<-read_csv(paste0(scenFolder,"/gis/Skims/SummerMiddayDriveDistanceSkim.csv")) %>%
  mutate(skim=3, id2=paste(TAZ,`TAZ:1`,skim, sep="-")) %>%
  rename(trip_time=`AB_MD_IVTT / BA_MD_IVTT`)
dist <- bind_rows(pm,am,ln,md) %>%
  mutate(ext_dist=case_when(`TAZ:1`%in% c(1,2,3,4,5,6,7,10,20,30,40,50,60,70) & 
                              TAZ %in% c(1,2,3,4,5,6,7,10,20,30,40,50,60,70) ~ 0,
                            `TAZ:1` == 1 | TAZ == 1 ~ 25.1,
                            `TAZ:1` == 2 | TAZ == 2 ~ 21.6,
                            `TAZ:1` == 3 | TAZ == 3 ~ 17.2,
                            `TAZ:1` == 4 | TAZ == 4 ~ 16.7,
                            `TAZ:1` == 5 | TAZ == 5 ~ 30.9,
                            `TAZ:1` == 6 | TAZ == 6 ~ 19.0,
                            `TAZ:1` == 7 | TAZ == 7 ~ 15.1,
                            `TAZ:1` == 10 | TAZ == 10 ~ 136,
                            `TAZ:1` == 20 | TAZ == 20 ~ 114,
                            `TAZ:1` == 30 | TAZ == 30 ~ 424,
                            `TAZ:1` == 40 | TAZ == 40 ~ 408,
                            `TAZ:1` == 50 | TAZ == 50 ~ 112,
                            `TAZ:1` == 60 | TAZ == 60 ~ 130,
                            `TAZ:1` == 70 | TAZ == 70 ~ 122                            
  ),
  distance=`Length (Skim)`- ext_dist) %>%
  mutate(distance = ifelse(is.na(distance),`Length (Skim)`,distance),
         time_woExtDist = trip_time - (`Length (Skim)`-distance)/50*50) %>% 
  mutate(
    ext_dist = ifelse(`TAZ:1`%in% c(1,2,3,4,5,6,7,10,20,30,40,50,60,70) & 
                        TAZ %in% c(1,2,3,4,5,6,7,10,20,30,40,50,60,70),
                      ext_dist+(
                        case_when(`TAZ:1` == 1|`TAZ:1` == 10~25.1,
                                  `TAZ:1` == 2|`TAZ:1` == 20~21.6,
                                  `TAZ:1` == 3|`TAZ:1` == 30~17.2,
                                  `TAZ:1` == 4|`TAZ:1` == 40~16.7,
                                  `TAZ:1` == 5|`TAZ:1` == 50~30.9,
                                  `TAZ:1` == 6|`TAZ:1` == 60~19.0,
                                  `TAZ:1` == 7|`TAZ:1` == 70~15.1)
                      ),
                      ext_dist
    ),
    ext_dist = ifelse(`TAZ:1`%in% c(1,2,3,4,5,6,7,10,20,30,40,50,60,70) & 
                        TAZ %in% c(1,2,3,4,5,6,7,10,20,30,40,50,60,70),
                      ext_dist+(
                        case_when(TAZ == 1|TAZ == 10~25.1,
                                  TAZ == 2|TAZ == 20~21.6,
                                  TAZ == 3|TAZ == 30~17.2,
                                  TAZ == 4|TAZ == 40~16.7,
                                  TAZ == 5|TAZ == 50~30.9,
                                  TAZ == 6|TAZ == 60~19.0,
                                  TAZ == 7|TAZ == 70~15.1)
                      ),
                      ext_dist
    )
  ) %>% 
  mutate(
    `Length (Skim)` = ifelse(
      `TAZ:1`%in% c(1,2,3,4,5,6,7,10,20,30,40,50,60,70) & 
        TAZ %in% c(1,2,3,4,5,6,7,10,20,30,40,50,60,70),
      `Length (Skim)`+ext_dist,
      `Length (Skim)`
    ),
    trip_time = ifelse(
      `TAZ:1`%in% c(1,2,3,4,5,6,7,10,20,30,40,50,60,70) & 
        TAZ %in% c(1,2,3,4,5,6,7,10,20,30,40,50,60,70),
      trip_time+ext_dist,
      trip_time
    )
  )


# dist %>% head(20) %>% data.frame()
### trip_length_summary ###

if(includeExtDist==1){
trip_len_sum <- trip_file %>% left_join(dist, by="id2") %>%
    mutate(Time=case_when(skim.x==1 ~ "AM",
                          skim.x==2 ~ "PM",
                          skim.x==3 ~ "MD",
                          skim.x==4 ~ "LN")) %>%
  group_by(partyType, tripType, Time, modeAgg) %>%
  summarise(Average_Trip_Length_Miles=mean(`Length (Skim)`, na.rm=T),
            Average_Trip_Time_Minutes=mean(trip_time, na.rm=T),
            count=sum(!is.na(trip_time))) %>%
  select(partyType, tripType, Time,modeAgg, Average_Trip_Length_Miles,Average_Trip_Time_Minutes, count)

} else{
trip_len_sum <- trip_file %>% left_join(dist, by="id2") %>%
    mutate(Time=case_when(skim.x==1 ~ "AM",
                          skim.x==2 ~ "PM",
                          skim.x==3 ~ "MD",
                          skim.x==4 ~ "LN")) %>%
  group_by(partyType, tripType, Time, modeAgg) %>%
  summarise(Average_Trip_Length_Miles=mean(distance, na.rm=T),
            Average_Trip_Time_Minutes=mean(time_woExtDist, na.rm=T),
            count=sum(!is.na(trip_time))) %>%
  select(partyType, tripType, Time,modeAgg, Average_Trip_Length_Miles,Average_Trip_Time_Minutes, count)
}
 
### export trip summary file ### 

write.csv(trip_len_sum,paste0(scenFolder,"/outputs_summer/reports/trip_length_summary.csv"),row.names = F)

### tour_length_summary ###

if(includeExtDist==1){
tour_len_sum <- trip_file %>% left_join(dist, by="id2") %>%
  group_by(tourID, modeAgg, tripType, partyType) %>% 
    summarise(trip_time=sum(trip_time, na.rm=T),Length_Skim=sum(`Length (Skim)`, na.rm=T)) %>%
  group_by(partyType, tripType, modeAgg) %>%
  summarise(Average_Tour_Length_Miles=mean(Length_Skim, na.rm=T),
            Average_Tour_Time_Minutes=mean(trip_time, na.rm=T),
            count=n())

} else{
tour_len_sum <- trip_file %>% left_join(dist, by="id2") %>%
  group_by(tourID, modeAgg, tripType, partyType) %>% 
    summarise(trip_time=sum(time_woExtDist, na.rm=T),Length_Skim=sum(distance, na.rm=T)) %>%
  group_by(partyType, tripType, modeAgg) %>%
  summarise(Average_Tour_Length_Miles=mean(Length_Skim, na.rm=T),
            Average_Tour_Time_Minutes=mean(trip_time, na.rm=T),
            count=n())
}



### export tour summary file ### 

write.csv(tour_len_sum,paste0(scenFolder,"/outputs_summer/reports/tour_length_summary.csv"),row.names=F)
 

### vmt summary ###


if(includeExtDist==1){

vmt_summary <- trip_file %>% filter(modeAgg=='drive') %>% 
  left_join(dist, by="id2") %>% 
  mutate(Time=case_when(skim.x==1 ~ "AM",
                        skim.x==2 ~ "PM",
                        skim.x==3 ~ "MD",
                        skim.x==4 ~ "LN")) %>%
  group_by(partyType, tripType,Time) %>%
  summarise(VMT=sum(`Length (Skim)`, na.rm=T), VHT=sum(trip_time/60, na.rm=T))


} else{

vmt_summary <- trip_file %>% filter(modeAgg=='drive') %>% 
  left_join(dist, by="id2") %>% 
  mutate(Time=case_when(skim.x==1 ~ "AM",
                        skim.x==2 ~ "PM",
                        skim.x==3 ~ "MD",
                        skim.x==4 ~ "LN")) %>%
  group_by(partyType, tripType,Time) %>%
  summarise(VMT=sum(distance, na.rm=T), VHT=sum(time_woExtDist/60, na.rm=T))
  
}

write.csv(vmt_summary,paste0(scenFolder,"/outputs_summer/reports/vmt_summary.csv"),row.names=F)


### auto occupancy ###

occ_tour <- trip_file %>% group_by(tourID) %>% 
    filter(row_number()==1) %>% 
    filter(mode %in% c("shared auto", "drive alone")) %>%
    arrange(partyType, tripType, skim, tourID) %>% 
    select(partyType, tripType, skim, tourID,persons) %>% 
  mutate(Time=case_when(skim==1 ~ "AM",
                        skim==2 ~ "PM",
                        skim==3 ~ "MD",
                        skim==4 ~ "LN")) %>%
  group_by(partyType, tripType, Time) %>%
  summarise(`Tour Auto Occupancy`=mean(persons), `Tour Count`=n())


occ_trip <- trip_file %>% filter(mode %in% c("shared auto", "drive alone")) %>%
  mutate(Time=case_when(skim==1 ~ "AM",
                        skim==2 ~ "PM",
                        skim==3 ~ "MD",
                        skim==4 ~ "LN")) %>%
  group_by(partyType, tripType, Time) %>%
  summarise(`Trip Auto Occupancy`=mean(persons),`Trip Count`=n())

auto_occ_sum <- bind_cols(occ_tour, occ_trip) %>%
select(-c(partyType1, tripType1, Time1)) %>% 
    select(partyType,tripType,Time,`Trip Auto Occupancy`,`Tour Auto Occupancy`,`Trip Count`,`Tour Count`)

write.csv(auto_occ_sum,paste0(scenFolder,"/outputs_summer/reports/autoocc_summary.csv"),row.names=F)


#### new trip file ####

trip_new<-trip_file %>% #filter(modeAgg=='drive') %>% 
  left_join(dist, by="id2") %>% 
  left_join(cw, by=c("startTaz"="TAZ")) %>% 
  rename(startTract=TRACT_2000,startCounty=COUNTY,startCDP=CDP,startCity=SOUTH_LAKE) %>%
  left_join(cw, by=c("endTaz"="TAZ")) %>% 
  rename(endTract=TRACT_2000,endCounty=COUNTY,endCDP=CDP,endCity=SOUTH_LAKE) %>%
  mutate(Time=case_when(skim.x==1 ~ "AM",
                        skim.x==2 ~ "PM",
                        skim.x==3 ~ "MD",
                        skim.x==4 ~ "LN"),
                        startTazJurisdiction = NA, # place holder for jurisdiction, will modify this based upon taz to jurisdiction crosswalk
                        endTazJurisdiction = NA) %>% # place holder for jurisdiction, will modify this based upon taz to jurisdiction crosswalk
  rename(total_distance = `Length (Skim)`, internal_distance = distance, external_distance = ext_dist) %>%
  select(tripID, tourID, startTaz, startTract, startCounty,startCDP,startCity, endTaz, endTract, endCounty,endCDP,endCity, partyType, persons, tripType, Time, mode, modeAgg, stayType, walkSegment, trip_time, internal_distance, external_distance, total_distance, BIKE_DIST, WALK_DIST)

write.csv(trip_new,paste0(scenFolder,"/outputs_summer/reports/trip_file_juris.csv"),row.names=F)
