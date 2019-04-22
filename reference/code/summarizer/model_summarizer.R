#.libPaths(c("../code/R-3.5.1/library"))
.libPaths(.libPaths()[2])
.libPaths()

args=commandArgs(trailingOnly=TRUE)

library(tidyverse)


scen_name = args[1]
scenFolder <- paste0('../../../scenarios/',scen_name)
print(scenFolder)

includeExtDist <- 0

PartyArray <- read_csv(paste0(scenFolder,"/outputs_summer/PartyArray_afterThruVisitors.pam"))

### read in trip file ###
trip_file_orig <- read_csv(paste0(scenFolder,"/outputs_summer/trip_file.csv")) #%>%     filter(startTaz>0 & endTaz>0)


trip_file<- trip_file_orig %>% 
  mutate(id=paste(startTaz, endTaz, sep="-"), id2=paste(startTaz, endTaz, skim, sep="-")) %>% 
  mutate(modeAgg = case_when(
    mode %in% c('drive alone','shared auto')~'drive',
    mode %in% c('drive to transit','walk to transit')~'transit',
    mode %in% c('non motorized')~'non-motorized',
    mode %in% c('visitor shuttle','school bus')~'other')) %>% 
  left_join(PartyArray %>% select(id,stayType,walkSegment), by=c("partyID"="id")) %>% 
    mutate(partyType=ifelse(partyType=='overnight visitor' & stayType==1,'seasonal',partyType))


### read in and combine distances skim files ###
pm<-read_csv(paste0(scenFolder,"/gis/Skims/SummerPMPeakDriveDistanceSkim.csv")) %>%
  mutate(skim=2,id2=paste(`TAZ:1`, TAZ,skim, sep="-")) %>%
  rename(trip_time=`AB_PM_IVTT / BA_PM_IVTT`)
am<-read_csv(paste0(scenFolder,"/gis/Skims/SummerAMPeakDriveDistanceSkim.csv")) %>%
  mutate(skim=1,id2=paste(`TAZ:1`, TAZ,skim, sep="-")) %>%
  rename(trip_time=`AB_AM_IVTT / BA_AM_IVTT`)
ln<-read_csv(paste0(scenFolder,"/gis/Skims/SummerLateNightDriveDistanceSkim.csv")) %>%
  mutate(skim=4,id2=paste(`TAZ:1`, TAZ, skim, sep="-")) %>%
  rename(trip_time=`AB_LN_IVTT / BA_LN_IVTT`)
md<-read_csv(paste0(scenFolder,"/gis/Skims/SummerMiddayDriveDistanceSkim.csv")) %>%
  mutate(skim=3, id2=paste(`TAZ:1`, TAZ, skim, sep="-")) %>%
  rename(trip_time=`AB_MD_IVTT / BA_MD_IVTT`)
dist <- bind_rows(pm,am,ln,md) %>%
  mutate(ext_dist=case_when(`TAZ:1`%in% c(1,2,3,4,5,6,7) & TAZ %in% c(1,2,3,4,5,6,7) ~ 0,
                            `TAZ:1` == 1 | TAZ == 1 ~ 12.5,
                            `TAZ:1` == 2 | TAZ == 2 ~ 10,
                            `TAZ:1` == 3 | TAZ == 3 ~ 11,
                            `TAZ:1` == 4 | TAZ == 4 ~ 16.5,
                            `TAZ:1` == 5 | TAZ == 5 ~ 40,
                            `TAZ:1` == 6 | TAZ == 6 ~ 11,
                            `TAZ:1` == 7 | TAZ == 7 ~ 9.5),
         distance=`Length (Skim)`- ext_dist) %>%
		 mutate(distance = ifelse(is.na(distance),`Length (Skim)`,distance),
		        time_woExtDist = trip_time - (`Length (Skim)`-distance)/50*50)


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

