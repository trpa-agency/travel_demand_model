package com.pb.tahoe.util;

/**
 * User: Frazier
 * Date: Aug 2, 2006
 * Time: 12:24:35 PM
 * Created by IntelliJ IDEA.
 */

import com.pb.common.datafile.TableDataSet;
import com.pb.common.datafile.TableDataSetLoader;
import com.pb.common.datafile.TextFile;
import com.pb.common.matrix.*;
import com.pb.common.util.ResourceUtil;
import org.apache.log4j.Logger;

import java.io.File;
import java.util.*;

public class DestinationChoiceSummarizer {
    protected static Logger logger = Logger.getLogger(DestinationChoiceSummarizer.class);
    ResourceBundle rb;

    public DestinationChoiceSummarizer(ResourceBundle rb){
        this.rb = rb;
    }

    private TableDataSet readResultsFile(String fileID) {
        return TableDataSetLoader.loadTableDataSet(rb, fileID);
    }

    public Matrix initODMatrix() {
        //first read the set of external numbers from the zonal file
        TableDataSet socio = TableDataSetLoader.loadTableDataSet(rb, "taz.correspondence.file");
        int rowCount = socio.getRowCount();
        int[] externalNumbers = new int[rowCount + 1];
        for(int row = 1; row <= rowCount; row ++){
            externalNumbers[row] =  (int) socio.getValueAt(row, "taz");
        }

        //Create a new matrix and set the external number array.
        Matrix odMatrix = new Matrix(rowCount, rowCount);
        odMatrix.setExternalNumbers(externalNumbers);
        return odMatrix;
    }

    public void fillODMatrix(TableDataSet resultsTable, Matrix odMatrix, int purpose) {
        for (int i = 1; i <= resultsTable.getRowCount(); i++) {
            int iTAZ =  (int) resultsTable.getValueAt(i,"origTaz");
            int jTAZ =  (int) resultsTable.getValueAt(i,"destTaz");
            if (resultsTable.getValueAt(i,"purpose") == purpose)
                odMatrix.setValueAt(iTAZ,jTAZ,odMatrix.getValueAt(iTAZ,jTAZ) + 1);
        }
    }

    public void saveCsvODTable(Matrix odMatrix, File f) {
        MatrixWriter writer = MatrixWriter.createWriter(MatrixType.CSV, f);
        writer.writeMatrix(odMatrix);
    }

    public void saveZipODTable(Matrix odMatrix, File f) {
        MatrixWriter writer = MatrixWriter.createWriter(MatrixType.ZIP, f);
        writer.writeMatrix(odMatrix);
    }

    //hashmap structure defines what you will output:
    // There are 4 string keys: Trips, Attractions, Productions, Distances
    //For Trips, 4 values are allowed (ii,ie,ei,ee), which say which trip types to report in output (total is always reported)
    //For Attractions, 7 values are allowed(t,e,i,ie,ei,ii,ee)
    //   t will report every zone's attraction from every other zone
    //   e will report every external zone's attraction from every other zone
    //   i will report every internal zone's attraction from every other zone
    //   ie will report every external zone's attraction from every internal zone
    //   ei will report every internal zone's attraction from every external zone
    //   ii will report every internal zone's attraction from every other internal zone
    //   ee  will report every external zone's attraction from every other external zone
    //Productions is the same as attractions, only  replace "attraction" in the above description with production and "from" with "to"
    //For Distances,  5 values are allowed (t,ii,ei,ie,ee) and say which distance summary to report
    public void summaryOut(Matrix odMatrix, int[] externalZones, float[] distanceClasses, File distanceFile, HashMap<String,String[]> structure) {
        //if you want this to be saved, your array in each structure element should have a series of files appended to the end telling where to save each summary
        //also, add hashmap key of "Save"
        //So, if "Save" is in structure's keys, then the array for each element should be twice the number of summaries being run (except trips, which has one filename at end)
        HashMap<String,HashMap<String,String>> fileMap = new HashMap<String,HashMap<String,String>>();
        boolean saveOn = false;
        if (structure.containsKey("Save")) {
            saveOn = true;
            for (String s : structure.keySet()) {
                HashMap<String,String> temp = new HashMap<String,String>();
                if (s.equals("Save")) continue;
                if (s.equals("Trips")) {
                    temp.put("Trips",structure.get(s)[structure.get(s).length - 1]);
                    fileMap.put(s,temp);
                    continue;
                }
                int shift = structure.get(s).length/2;
                for (int i = 0; i < shift; i++) {
                    temp.put(structure.get(s)[i],structure.get(s)[i+shift]);
                }
                fileMap.put(s,temp);
            }
        }

        //Create internal zone set and total zone set
        Set<Integer> extArray = new HashSet<Integer>();
        for (int i : externalZones) extArray.add(i);
        int[] totalZones = new int[odMatrix.getExternalNumbers().length - 1];
        int[] internalZones = new int[odMatrix.getExternalNumbers().length - externalZones.length - 1];
        int counter = 0;
        int counter2 = 0;
        for (int i : odMatrix.getExternalNumbers()) {
            if (i == 0) continue;
            totalZones[counter2++] = i;
            if (!extArray.contains(i)) internalZones[counter++] = i;
        }

        //Trips summary
        String[] tripsArray = structure.get("Trips");
        Set<String> yTripsArray = new HashSet<String>();
        yTripsArray.add("Total Trips");
        for (String i : tripsArray) {
            if (i.equals("ii")) yTripsArray.add("Internal-Internal Trips");
            if (i.equals("ie")) yTripsArray.add("Internal-External Trips");
            if (i.equals("ei")) yTripsArray.add("External-Internal Trips");
            if (i.equals("ee")) yTripsArray.add("External-External Trips");
        }
        LinkedHashMap<String,Float> odIntExt = odIntExtBreakdown(odMatrix,externalZones);
        LinkedHashMap<String,Float> odIntExtFin = new LinkedHashMap<String,Float>();
        for (String key : odIntExt.keySet()) {
            if (yTripsArray.contains(key)) odIntExtFin.put(key,odIntExt.get(key));
        }
        String tripSummary;
        if (saveOn && (!fileMap.get("Trips").get("Trips").equals(""))) {
            tripSummary = tripsSummary(odIntExtFin,fileMap.get("Trips").get("Trips"));
        } else {
            tripSummary = tripsSummary(odIntExtFin);
        }

        //Attractions summary
        String[] attractionArray = structure.get("Attractions");
        Set<String> yAttArray = new HashSet<String>();
        if (!(attractionArray == null)) for (String i : attractionArray) yAttArray.add(i);
        String attSummary = "";

        if (yAttArray.contains("t")) {
            odIntExt.put("Total-Trips Trips",odIntExt.get("Total Trips"));
            if (saveOn && (!fileMap.get("Attractions").get("t").equals(""))) {
                attSummary = attSummary + "\n\n" + zonalTripsSummary(
                    zoneAttractions(odMatrix,totalZones,new int[0]), odIntExt,
                    "","Total-Trips","All Zones to All","Attraction",fileMap.get("Attractions").get("t"));
            } else {
                attSummary = attSummary + "\n\n" + zonalTripsSummary(
                    zoneAttractions(odMatrix,totalZones,new int[0]), odIntExt,
                    "","Total-Trips","All Zones to All","Attraction");
            }
        }
        if (yAttArray.contains("e")) {
            odIntExt.put("All-External Trips",odIntExt.get("Internal-External")+odIntExt.get("External-External"));
            if (saveOn && (!fileMap.get("Attractions").get("e").equals(""))) {
                attSummary = attSummary + "\n\n" + zonalTripsSummary(
                    zoneAttractions(odMatrix,externalZones,new int[0]), odIntExt,
                    "External","All-External","All Zones to External","Attraction",fileMap.get("Attractions").get("e"));
            } else {
                attSummary = attSummary + "\n\n" + zonalTripsSummary(
                    zoneAttractions(odMatrix,externalZones,new int[0]), odIntExt,
                    "External","All-External","All Zones to External","Attraction");
            }
        }
        if (yAttArray.contains("i")) {
            odIntExt.put("All-Internal Trips",odIntExt.get("Internal-Internal")+odIntExt.get("External-Internal"));
            if (saveOn && (!fileMap.get("Attractions").get("i").equals(""))) {
                attSummary = attSummary + "\n\n" + zonalTripsSummary(
                    zoneAttractions(odMatrix,internalZones,new int[0]), odIntExt,
                    "Internal","All-Internal","All Zones to Internal","Attraction",fileMap.get("Attractions").get("i"));
            } else {
                attSummary = attSummary + "\n\n" + zonalTripsSummary(
                    zoneAttractions(odMatrix,internalZones,new int[0]), odIntExt,
                    "Internal","All-Internal","All Zones to Internal","Attraction");
            }
        }
        if (yAttArray.contains("ie")) {
            if (saveOn && (!fileMap.get("Attractions").get("ie").equals(""))) {
                attSummary = attSummary + "\n\n" + zonalTripsSummary(
                    zoneAttractions(odMatrix,externalZones,externalZones), odIntExt,
                    "External","Internal-External","Internal Zones to External","Attraction",fileMap.get("Attractions").get("ie"));
            } else {
                attSummary = attSummary + "\n\n" + zonalTripsSummary(
                    zoneAttractions(odMatrix,externalZones,externalZones), odIntExt,
                    "External","Internal-External","Internal Zones to External","Attraction");
            }
        }
        if (yAttArray.contains("ei")) {
            if (saveOn && (!fileMap.get("Attractions").get("ei").equals(""))) {
                attSummary = attSummary + "\n\n" + zonalTripsSummary(
                    zoneAttractions(odMatrix,internalZones,internalZones), odIntExt,
                    "Internal","External-Internal","External Zones to Internal","Attraction",fileMap.get("Attractions").get("ei"));
            } else {
                attSummary = attSummary + "\n\n" + zonalTripsSummary(
                    zoneAttractions(odMatrix,internalZones,internalZones), odIntExt,
                    "Internal","External-Internal","External Zones to Internal","Attraction");
            }
        }
        if (yAttArray.contains("ii")) {
            if (saveOn && (!fileMap.get("Attractions").get("ii").equals(""))) {
                attSummary = attSummary + "\n\n" + zonalTripsSummary(
                    zoneAttractions(odMatrix,internalZones,externalZones), odIntExt,
                    "Internal","Internal-Internal","Internal Zones to Internal","Attraction",fileMap.get("Attractions").get("ii"));
            } else {
                attSummary = attSummary + "\n\n" + zonalTripsSummary(
                    zoneAttractions(odMatrix,internalZones,externalZones), odIntExt,
                    "Internal","Internal-Internal","Internal Zones to Internal","Attraction");
            }
        }
        if (yAttArray.contains("ee")) {
            if (saveOn && (!fileMap.get("Attractions").get("ee").equals(""))) {
                attSummary = attSummary + "\n\n" + zonalTripsSummary(
                    zoneAttractions(odMatrix,externalZones,internalZones), odIntExt,
                    "External","External-External","External Zones to External","Attraction",fileMap.get("Attractions").get("ee"));
            } else {
                attSummary = attSummary + "\n\n" + zonalTripsSummary(
                    zoneAttractions(odMatrix,externalZones,internalZones), odIntExt,
                    "External","External-External","External Zones to External","Attraction");
            }
        }

        //Productions summary
        String[] productionArray = structure.get("Productions");
        Set<String> yProdArray = new HashSet<String>();
        if (!(productionArray == null)) for (String i : productionArray) yProdArray.add(i);
        String prodSummary = "";

        if (yProdArray.contains("t")) {
            odIntExt.put("Total-Trips",odIntExt.get("Total Trips"));
            if (saveOn && (!fileMap.get("Productions").get("t").equals(""))) {
                attSummary = attSummary + "\n\n" + zonalTripsSummary(
                    zoneProductions(odMatrix,totalZones,new int[0]), odIntExt,
                    "","Total-Trips","All Zones to All","Production",fileMap.get("Productions").get("t"));
            } else {
                attSummary = attSummary + "\n\n" + zonalTripsSummary(
                    zoneProductions(odMatrix,totalZones,new int[0]), odIntExt,
                    "","Total-Trips","All Zones to All","Production");
            }
        }
        if (yProdArray.contains("e")) {
            odIntExt.put("All-External",odIntExt.get("Internal-External")+odIntExt.get("External-External"));
            if (saveOn && (!fileMap.get("Productions").get("e").equals(""))) {
                attSummary = attSummary + "\n\n" + zonalTripsSummary(
                    zoneProductions(odMatrix,externalZones,new int[0]), odIntExt,
                    "External","All-External","All Zones to External","Production",fileMap.get("Productions").get("e"));
            } else {
                attSummary = attSummary + "\n\n" + zonalTripsSummary(
                    zoneProductions(odMatrix,externalZones,new int[0]), odIntExt,
                    "External","All-External","All Zones to External","Production");
            }
        }
        if (yProdArray.contains("i")) {
            odIntExt.put("All-Internal",odIntExt.get("Internal-Internal")+odIntExt.get("External-Internal"));
            if (saveOn && (!fileMap.get("Productions").get("i").equals(""))) {
                attSummary = attSummary + "\n\n" + zonalTripsSummary(
                    zoneProductions(odMatrix,internalZones,new int[0]), odIntExt,
                    "Internal","All-Internal","Internal Zones to All","Production",fileMap.get("Productions").get("i"));
            } else {
                attSummary = attSummary + "\n\n" + zonalTripsSummary(
                    zoneProductions(odMatrix,internalZones,new int[0]), odIntExt,
                    "Internal","All-Internal","Internal Zones to All","Production");
            }
        }
        if (yProdArray.contains("ie")) {
            if (saveOn && (!fileMap.get("Productions").get("ie").equals(""))) {
                attSummary = attSummary + "\n\n" + zonalTripsSummary(
                    zoneProductions(odMatrix,internalZones,internalZones), odIntExt,
                    "Internal","Internal-External","Internal Zones to External","Production",fileMap.get("Productions").get("ie"));
            } else {
                attSummary = attSummary + "\n\n" + zonalTripsSummary(
                    zoneProductions(odMatrix,internalZones,internalZones), odIntExt,
                    "Internal","Internal-External","Internal Zones to External","Production");
            }
        }
        if (yProdArray.contains("ei")) {
            if (saveOn && (!fileMap.get("Productions").get("ei").equals(""))) {
                attSummary = attSummary + "\n\n" + zonalTripsSummary(
                    zoneProductions(odMatrix,externalZones,externalZones), odIntExt,
                    "External","External-Internal","External Zones to Internal","Production",fileMap.get("Productions").get("ei"));
            } else {
                attSummary = attSummary + "\n\n" + zonalTripsSummary(
                    zoneProductions(odMatrix,externalZones,externalZones), odIntExt,
                    "External","External-Internal","External Zones to Internal","Production");
            }
        }
        if (yProdArray.contains("ii")) {
            if (saveOn && (!fileMap.get("Productions").get("ii").equals(""))) {
                attSummary = attSummary + "\n\n" + zonalTripsSummary(
                    zoneProductions(odMatrix,internalZones,externalZones), odIntExt,
                    "Internal","Internal-Internal","Internal Zones to Internal","Production",fileMap.get("Productions").get("ii"));
            } else {
                attSummary = attSummary + "\n\n" + zonalTripsSummary(
                    zoneProductions(odMatrix,internalZones,externalZones), odIntExt,
                    "Internal","Internal-Internal","Internal Zones to Internal","Production");
            }
        }
        if (yProdArray.contains("ee")) {
            if (saveOn && (!fileMap.get("Productions").get("ee").equals(""))) {
                attSummary = attSummary + "\n\n" + zonalTripsSummary(
                    zoneProductions(odMatrix,externalZones,internalZones), odIntExt,
                    "External","External-External","External Zones to External","Production",fileMap.get("Productions").get("ee"));
            } else {
                attSummary = attSummary + "\n\n" + zonalTripsSummary(
                    zoneProductions(odMatrix,externalZones,internalZones), odIntExt,
                    "External","External-External","External Zones to External","Production");
            }
        }


        //Distances summary
        String[] distanceArray = structure.get("Distances");
        Set<String> yDistArray = new HashSet<String>();
        if (!(distanceArray == null)) for (String i : distanceArray) yDistArray.add(i);
        Matrix distanceMatrix = MatrixReader.readMatrix(distanceFile,"Distances");
        String distSummary = "";

        if (yDistArray.contains("t")) {
            if (saveOn && (!fileMap.get("Distances").get("t").equals(""))) {
                distSummary = distSummary + "\n\n" + tripDistanceSummary(
                        distanceDistribution(odMatrix, distanceClasses, new int[0],new int[0], distanceMatrix),
                        "All Zones to All Zones",fileMap.get("Distances").get("t"));
            } else {
                distSummary = distSummary + "\n\n" + tripDistanceSummary(
                        distanceDistribution(odMatrix, distanceClasses, new int[0],new int[0], distanceMatrix),
                        "All Zones to All Zones");
            }
        }
        if (yDistArray.contains("ii")) {
            if (saveOn && (!fileMap.get("Distances").get("ii").equals(""))) {
                distSummary = distSummary + "\n\n" + tripDistanceSummary(
                        distanceDistribution(odMatrix, distanceClasses, externalZones,externalZones, distanceMatrix),
                        "Internal to Internal",fileMap.get("Distances").get("ii"));
            } else {
                distSummary = distSummary + "\n\n" + tripDistanceSummary(
                        distanceDistribution(odMatrix, distanceClasses, externalZones,externalZones, distanceMatrix),
                        "Internal to Internal");
            }
        }
        if (yDistArray.contains("ie")) {
            if (saveOn && (!fileMap.get("Distances").get("ie").equals(""))) {
                distSummary = distSummary + "\n\n" + tripDistanceSummary(
                        distanceDistribution(odMatrix, distanceClasses, externalZones,internalZones, distanceMatrix),
                        "Internal to External",fileMap.get("Distances").get("ie"));
            } else {
                distSummary = distSummary + "\n\n" + tripDistanceSummary(
                        distanceDistribution(odMatrix, distanceClasses, externalZones,internalZones, distanceMatrix),
                        "Internal to External");
            }
        }
        if (yDistArray.contains("ei")) {
            if (saveOn && (!fileMap.get("Distances").get("ei").equals(""))) {
                distSummary = distSummary + "\n\n" + tripDistanceSummary(
                        distanceDistribution(odMatrix, distanceClasses, internalZones,externalZones, distanceMatrix),
                        "External to Internal",fileMap.get("Distances").get("ei"));
            } else {
                distSummary = distSummary + "\n\n" + tripDistanceSummary(
                        distanceDistribution(odMatrix, distanceClasses, internalZones,externalZones, distanceMatrix),
                        "External to Internal");
            }
        }
        if (yDistArray.contains("ee")) {
            if (saveOn && (!fileMap.get("Distances").get("ee").equals(""))) {
                distSummary = distSummary + "\n\n" + tripDistanceSummary(
                        distanceDistribution(odMatrix, distanceClasses, internalZones,internalZones, distanceMatrix),
                        "External to External",fileMap.get("Distances").get("ee"));
            } else {
                distSummary = distSummary + "\n\n" + tripDistanceSummary(
                        distanceDistribution(odMatrix, distanceClasses, internalZones,internalZones, distanceMatrix),
                        "External to External");
            }
        }

        logger.info("\n\n" + tripSummary + attSummary + prodSummary + distSummary);

    }

    //Return a linked hashmap with description of trip type (internal-external, internal-internal, etc) and total number of trips
    public LinkedHashMap<String,Float> odIntExtBreakdown(Matrix odMatrix, int[] externalZones) {
        float odSum = (float) (odMatrix.getSum());
        float intextSum = 0.0f;
        float extintSum = 0.0f;
        float extextSum = 0.0f;
        float intintSum;
        for (int i : externalZones) {
            intextSum += odMatrix.getColumnSum(odMatrix.getInternalNumber(i) + 1);
            extintSum += odMatrix.getRowSum(odMatrix.getInternalNumber(i) + 1);
            for (int j : externalZones) {
                extextSum += odMatrix.getValueAt(odMatrix.getInternalNumber(i) + 1,odMatrix.getInternalNumber(j) + 1);
            }
        }
        intintSum = odSum - intextSum - extintSum + extextSum;
        LinkedHashMap<String,Float> odIntExt = new LinkedHashMap<String,Float>();
        odIntExt.put("Total Trips",odSum);
        odIntExt.put("Internal-Internal Trips",intintSum);
        odIntExt.put("Internal-External Trips",intextSum);
        odIntExt.put("External-Internal Trips",extintSum);
        odIntExt.put("External-External Trips",extextSum);
        return odIntExt;
    }

    //Return a linked hashmap with zone numbers and total trips to each (minus trips from excludeZones array)
    public LinkedHashMap<Integer,Float> zoneAttractions(Matrix odMatrix, int[] zones, int[] excludeZones) {
        Set<Integer> excludeZoneSet = new HashSet<Integer>();
        for (int i : excludeZones) excludeZoneSet.add(i);
        LinkedHashMap<Integer,Float> tripSum = new LinkedHashMap<Integer,Float>();
        for (int i : zones) {
            tripSum.put(i,0.0f);
            for (int j : odMatrix.getExternalNumbers()) {
                if (!excludeZoneSet.contains(j)) {
                    if (j != 0) {
                        tripSum.put(i,tripSum.get(i) + odMatrix.getValueAt(j,i));
                    }
                }
            }
        }
        return tripSum;
    }

    //Return a linked hashmap with zone numbers and total trips from each (minus trips from excludeZones array)
    public LinkedHashMap<Integer,Float> zoneProductions(Matrix odMatrix, int[] zones, int[] excludeZones) {
        Set<Integer> excludeZoneSet = new HashSet<Integer>();
        for (int i : excludeZones) excludeZoneSet.add(i);
        LinkedHashMap<Integer,Float> tripSum = new LinkedHashMap<Integer,Float>();
        for (int i : zones) {
            tripSum.put(i,0.0f);
            for (int j : odMatrix.getExternalNumbers()) {
                if (!excludeZoneSet.contains(j)) {
                    if (j != 0) {
                        tripSum.put(i,tripSum.get(i) + odMatrix.getValueAt(i,j));
                    }
                }
            }
        }
        return tripSum;
    }

    //Return a linked hashmap with distance classes and total trips in each (minus from/to exclusion zone class arrays)
    public LinkedHashMap<String,Float> distanceDistribution(Matrix odMatrix, float[] distanceClasses,
                                                            int[] fromExcludeZones, int[] toExcludeZones,
                                                            Matrix distanceMatrix) {
        Set<Integer> fromExcludeZoneSet = new HashSet<Integer>();
        for (int i : fromExcludeZones) fromExcludeZoneSet.add(i);
        Set<Integer> toExcludeZoneSet = new HashSet<Integer>();
        for (int i : toExcludeZones) toExcludeZoneSet.add(i);
        float[] distanceTrips = new float[distanceClasses.length];
        LinkedHashMap<String,Float> distanceSum = new LinkedHashMap<String,Float>();
        //initialize hashmap
        for (int i = 0; i < distanceClasses.length; i++) {
            if (i == distanceClasses.length - 1) {
              distanceSum.put("" + distanceClasses[i] + "+",0.0f);
            } else {
                distanceSum.put("" + distanceClasses[i] + " - " + distanceClasses[i+1],0.0f);
            }
        }
        for (int i : odMatrix.getExternalNumbers()) {
            for (int j: odMatrix.getExternalNumbers()) {
                //skip exclusion zones
                if ((fromExcludeZoneSet.contains(i)) || (toExcludeZoneSet.contains(j))) continue;
                //skip if there are no trips
                if (odMatrix.getValueAt(i,j) == 0) continue;
                float distance = distanceMatrix.getValueAt(i,j);
                for (int k = 0; k < distanceClasses.length; k++) {
                    if (k == distanceClasses.length - 1) {
                        distanceSum.put("" + distanceClasses[k] + "+",
                                distanceSum.get("" + distanceClasses[k] + "+") + odMatrix.getValueAt(i,j));
                        distanceTrips[k] += odMatrix.getValueAt(i,j);
                        break;
                    }
                    if (distance <= distanceClasses[k+1]) {
                        distanceSum.put("" + distanceClasses[k] + " - " + distanceClasses[k+1],
                                distanceSum.get("" + distanceClasses[k] + " - " + distanceClasses[k+1])
                                        + odMatrix.getValueAt(i,j));
                        distanceTrips[k] += odMatrix.getValueAt(i,j);
                        break;
                    }
                }
            }
        }
        return distanceSum;
    }

    //Formatted string output of trips summary
    public String tripsSummary(LinkedHashMap<String,Float> odIntExt) {
        String tripLine = "+---------------------------+-----------------+";
        String summary = "\nTrip Summary\n" + tripLine + "\n";
        for (String tripType : odIntExt.keySet()) {
            summary += String.format("| %1$-26s|%2$6.0f (%3$6.2f%%) |\n",
                    tripType,odIntExt.get(tripType),odIntExt.get(tripType)/odIntExt.get("Total Trips")*100);
        }
        summary += tripLine;
        return summary;

    }

    //Save result of trips summary
    public String tripsSummary(LinkedHashMap<String,Float> odIntExt, String outFile) {
        //Create header
        String outString = "Trip_Type,Trips,Percentage\n";
        //Add data
        for (String tripType : odIntExt.keySet()) {
            outString = outString + tripType + "," + odIntExt.get(tripType) + "," + odIntExt.get(tripType)/odIntExt.get("Total Trips")*100 + "\n";
        }
        TextFile.writeTo(outFile,outString);
        return tripsSummary(odIntExt);
    }

    //Formatted string output of zonal attractions or productions
    public String zonalTripsSummary(LinkedHashMap<Integer,Float> tripSum,
                                    LinkedHashMap<String,Float> odIntExt, String zoneType, String tripType, String tripTypeName, String summaryType) {
        String tripLine = "+----------------+----------------+----------------+----------------+";
        String ftType = tripType;
        String etType = "";
        if ((tripType.length() > 7) && (tripType.contains("-"))) {
            ftType = tripType.substring(0,tripType.indexOf("-") + 1);
            etType = tripType.substring(tripType.indexOf("-") + 1);
        }
        String summary = "\n" + tripTypeName + " Zones " + summaryType + " Summary\n" + tripLine + "\n" +
                String.format("|%1$15s |%2$15s |%3$15s |%4$15s |",zoneType, ftType, "% of Total", "% of " + ftType);
        String subSummary = String.format("|%1$15s |%2$15s |%3$15s |%4$15s |","Zone", etType + " Trips", "Trips", etType + " Trips");
        summary = summary + "\n" + subSummary + "\n" + tripLine + "\n";
        for (int i : tripSum.keySet()) {
            summary = summary +  String.format("|%1$15d |%2$15.0f |%3$14.2f%% |%4$14.2f%% |",
                    i,
                    tripSum.get(i),
                    tripSum.get(i)/odIntExt.get("Total Trips")*100,
                    tripSum.get(i)/odIntExt.get(tripType + " Trips")*100);
            summary += "\n";
        }
        summary += tripLine;
        return summary;
    }

    //Save result of zonal attractions or productions summary
    public String zonalTripsSummary(LinkedHashMap<Integer,Float> tripSum,
                                    LinkedHashMap<String,Float> odIntExt, String zoneType,
                                    String tripType, String tripTypeName, String summaryType, String outFile) {
        //Create header
        String outString = "Zone,Trips,Percent_Total,Percent_";
        outString = outString + tripType.replace("-","_") + "\n";
        //Add data
        for (int i : tripSum.keySet()) {
            outString = outString + i + "," + tripSum.get(i) + "," +
                    tripSum.get(i)/odIntExt.get("Total Trips")*100 + "," +
                    tripSum.get(i)/odIntExt.get(tripType + " Trips")*100 + "\n";
        }
        TextFile.writeTo(outFile,outString);
        return zonalTripsSummary(tripSum, odIntExt, zoneType, tripType, tripTypeName, summaryType);
    }

    //Formatted string output of distance distribution
    public String tripDistanceSummary(LinkedHashMap<String,Float> distanceSum, String tripType) {
        String summary = "\nDistance Distribution Summary (" + tripType + ")\n";
        String distanceLine = "+----------------+-----------------+";
        float distanceSumSum = 0.0f;
        for (String type : distanceSum.keySet()) distanceSumSum += distanceSum.get(type);
        summary = summary + distanceLine + "\n" +
                String.format("|%1$15s |%2$16s |",
                        "Distance Class","Trips (%)") + "\n" + distanceLine;
        for (String type : distanceSum.keySet()) {
            summary = summary + String.format("\n|%1$15s |%2$7.0f (%3$5.2f%%) |",
                    type, distanceSum.get(type), distanceSum.get(type)/distanceSumSum*100);
        }
        summary = summary + "\n" + distanceLine;
        return summary;
    }

    //save results of distance distribution
     public String tripDistanceSummary(LinkedHashMap<String,Float> distanceSum, String tripType, String outFile) {
        //Create header
        String outString = "Distance_Class,Trips,Percent\n";
        //Add data
        float distanceSumSum = 0.0f;
        for (String type : distanceSum.keySet()) distanceSumSum += distanceSum.get(type);
        for (String type : distanceSum.keySet()) {
            outString = outString + type + "," + distanceSum.get(type) + "," + distanceSum.get(type)/distanceSumSum*100 + "\n";
        }
        TextFile.writeTo(outFile,outString);
        return tripDistanceSummary(distanceSum, tripType);
    }


    /**
     * This method will read in the DestinationChoice output file that lists
     * each persons destination choice and will populate a trip matrix
     * @param dcFileID
     * @param outputFilename
     * @param purpose
     */
    public void generateDCResults(String dcFileID, String outputFilename, int purpose) {

        //Read the results
        TableDataSet resultsTable = readResultsFile(dcFileID);

        //Initialize the matrix to be filled.  Be sure to initialize the external number array.
        Matrix odMatrix = initODMatrix();

        //Translate the results from a table to a matrix
        fillODMatrix(resultsTable, odMatrix, purpose);

        //Write out the resulting matrix as a CSV Matrix file.
        File f = new File(ResourceUtil.getProperty(rb, outputFilename) + ".csv");
        saveCsvODTable(odMatrix,f);

        //Write out the resulting matrix as a ZIP Matrix file.
        File zf = new File(ResourceUtil.getProperty(rb, outputFilename) + ".zip");
        saveZipODTable(odMatrix,zf);
    }

    /**
     * This method will read in the DestinationChoice output file that lists
     * each persons destination choice and will populate a trip matrix
     * as well as print desired summaries (and save them, if specified)
     * @param dcFileID
     * @param outputFilename
     * @param purpose
     * @param externalZones
     * @param distanceClasses
     * @param distanceMatrixFile
     * @param structure
     */
    public void generateDCResults(String dcFileID, String outputFilename, int purpose, int[] externalZones,
                                  float[] distanceClasses, File distanceMatrixFile, HashMap<String,String[]> structure) {
        generateDCResults(dcFileID, outputFilename, purpose);
        Matrix odMatrix = MatrixReader.readMatrix(new File(ResourceUtil.getProperty(rb, outputFilename) + ".zip"),"Distances");
        summaryOut(odMatrix, externalZones, distanceClasses, distanceMatrixFile,structure);

    }

    /**
     * This method will print desired summaries (and save them, if specified) of a specified od matrix
     * @param odMatrix
     * @param externalZones
     * @param distanceClasses
     * @param distanceMatrixFile
     * @param structure
     */
    public void generateDCResults(Matrix odMatrix, int[] externalZones, float[] distanceClasses,
                                  File distanceMatrixFile, HashMap<String,String[]> structure) {
        summaryOut(odMatrix, externalZones, distanceClasses, distanceMatrixFile,structure);
    }

    //This method will compress a given od matrix down using specified AlphaToBeta mapping
    public Matrix compressMatrix(Matrix odMatrix, AlphaToBeta a2b) {
        MatrixCompression compressor = new MatrixCompression(a2b);
        return compressor.getCompressedMatrix(odMatrix,"SUM");
    }


    public static void main(String[] args) {

        ResourceBundle rb = ResourceUtil.getResourceBundle("tahoe");

        DestinationChoiceSummarizer dcSum = new DestinationChoiceSummarizer(rb);
        //dcSum.generateDCResults("mandatory_destination.choice.output.file", "work_od.matrix.file",  1);

        //Summarize matrix contents and put out to screen
        int[] externalZones = {1,2,3,4,5,6,7};
        float[] distanceClasses = {0.0f,3.0f,6.0f,9.0f,12.0f,15.0f,20.0f,25.0f,30.0f,35.0f};
        HashMap<String,String[]> structure = new HashMap<String,String[]>();

        String[] tripsArray = {"ii","ie","C:/Documents and Settings/cfrazier/Desktop/tripsSummary.csv"};
        String[] attArray = {"ie","C:/Documents and Settings/cfrazier/Desktop/ieAttractions.csv",""};
        String[] prodArray = {};
        String[] distArray = {"ii","ie","t","C:/Documents and Settings/cfrazier/Desktop/iiDistanceSummary.csv","",""};

        structure.put("Trips",tripsArray);
        structure.put("Attractions",attArray);
        structure.put("Productions",prodArray);
        structure.put("Distances",distArray);
        //structure.put("Save",new String[0]);
        File distanceFile = new File(rb.getString("skims.directory") + "/" + rb.getString("sovDistMd.file") + "." + rb.getString("skims.format"));

        dcSum.generateDCResults("mandatory_destination.choice.output.file", "work_od.matrix.file",  1,
                externalZones, distanceClasses,distanceFile,structure);


        //Test out summing down to districts, and then looking at results
//        AlphaToBeta a2b = new AlphaToBeta(new File("C:/Models/Tahoe/Data/TAZ_District.csv"),"taz","county_district");
//        Matrix odMatrix = MatrixReader.readMatrix(new File(ResourceUtil.getProperty(rb, "work_od.matrix.file") + ".zip"),"Distances");;
//        Matrix distODMatrix = dcSum.compressMatrix(odMatrix,a2b);
//        int[] distExternalZones = {6};
//        dcSum.generateDCResults(distODMatrix,distExternalZones,distanceClasses,distanceFile,structure);
//        File zf = new File("C:/Documents and Settings/cfrazier/Desktop/DistrictOD.zip");
//        dcSum.saveZipODTable(distODMatrix,zf);

    }

}
