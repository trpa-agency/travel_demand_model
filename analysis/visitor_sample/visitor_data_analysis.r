library(data.table)
library(dplyr)
library(readr)

setwd(dirname(rstudioapi::getSourceEditorContext()$path)) 
survey <- read.csv("travel_survey_data.csv")

survey <- survey %>%
  dplyr::select(survey_id, survey_rejected, person_type, lodging_type, travel_size, travel_size_children, gps_point_category)

#--------------------------------------------------------------------------#
#------------------------Assumptions and Notes-----------------------------# 
#--------------------------------------------------------------------------#


#more than six travel party/travel_size_children is termed as 7
#id field is given the survey ID string
#'NA' response in travel party filed is removed
#Female adult information is not provided in the survey. They are given '-1' value to all the responses like the previous version
#Uniform value of stayType = 2 and season = 1 is provided.As the day visiotors are not staying in the Tahoe basin, and they are 
#all provided same staytype. Entire survey responses are recorded in the month of August. Hence there is no variation in the value of season.


count <- data.frame("Count" = c("zero", "one", "two", "three","four", "five", "six", "more_than_six"),  "Num" = c(0, 1,2,3,4,5,6,7))


day_vis <- survey %>%
  dplyr:: filter(survey_rejected == "no",
         person_type == "Day Visitor", 
         gps_point_category == "survey location",
         !is.na(travel_size)) %>%
  dplyr::select(survey_id, travel_size, travel_size_children) %>%
  mutate(
    femaleAdult = -1,
    stayType = 2, 
    season = 1
    ) %>%
  dplyr:: left_join(count, by = c("travel_size" = "Count")) %>%
  dplyr:: left_join(count, by = c("travel_size_children" = "Count")) %>%
  dplyr:: select(survey_id, Num.x, Num.y, femaleAdult, stayType, season) %>%
  setnames(old = c('survey_id', 'Num.x', 'Num.y'), new = c('ID', 'person', 'children'))

write.csv(day_vis, "DayVisitorSampleRecords.csv", row.names=FALSE)
  


#overnight visitor
#In the previous version of the overnight file, female type information is provided. Not enough information in the current survey
#StayType/Lodging_type mapping is not very clear. Hence, left it as it is. It still has some 'NA'. Not sure if that needs to be removed or clubbed in other.

overn_vis <- survey %>%
  dplyr:: filter(survey_rejected == "no",
                 person_type == "Overnight Visitor", 
                 gps_point_category == "survey location",
                 !is.na(travel_size),
                 !is.na(travel_size_children)) %>%
  dplyr::select(survey_id, travel_size, travel_size_children, lodging_type) %>%
  mutate(
    femaleAdult = -1,
    season = 1
  ) %>%
  dplyr:: left_join(count, by = c("travel_size" = "Count")) %>%
  dplyr:: left_join(count, by = c("travel_size_children" = "Count")) %>%
  dplyr:: select(survey_id, Num.x, Num.y, femaleAdult, lodging_type, season,) %>%
  setnames(old = c('survey_id', 'Num.x', 'Num.y'), new = c('ID', 'person', 'children'))

write.csv(overn_vis, "OvernightVisitorSampleRecords.csv", row.names=FALSE)
