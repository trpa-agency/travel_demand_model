library(sf)
library(maptools)
library(raster)
library(leaflet)
library(tidyverse)
library(kableExtra)
library(readxl)
library(rgdal)


print(getwd())
source("scenProperties.r")

convAttrToChar <- function(spdf){
  spdf@data <- spdf@data %>% mutate_if(is.factor,as.character)
  return(spdf)
}


streetNet <- st_read(paste0('Runs/',scen,'/post/Streets.shp')) %>% as('Spatial') %>% convAttrToChar()
streetNet@proj4string <- CRS("+init=epsg:4269")
streetNet2 <- elide(streetNet,shift=c(-0.00099, -0.00011))
# leaflet() %>% addTiles() %>% addPolylines(data=streetNet2)
streetNet2@proj4string <- CRS("+init=epsg:4269")

writeOGR(streetNet2,paste0('Runs/',scen,'/post/.'), layer = "Streets_offset", driver = "ESRI Shapefile")

nodeLyr <- st_read(paste0('Runs/',scen,'/post/Nodes.shp')) %>% as('Spatial') %>% convAttrToChar()
nodeLyr@proj4string <- CRS("+init=epsg:4269")
nodeLyr <- elide(nodeLyr,shift=c(-0.00099, -0.00011))
nodeLyr@proj4string <- CRS("+init=epsg:4269")
writeOGR(nodeLyr,paste0('Runs/',scen,'/post/.'), layer = "Nodes_offset", driver = "ESRI Shapefile")
