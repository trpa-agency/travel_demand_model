/*
 * Copyright  2005 PB Consult Inc.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package com.pb.tahoe.util;

import com.pb.common.matrix.CSVMatrixReader;
import com.pb.common.matrix.Matrix;
import com.pb.common.matrix.ZipMatrixWriter;
import com.pb.common.util.ResourceUtil;
import org.apache.log4j.Logger;
import java.io.File;
import java.util.ResourceBundle;

/**
 * SkimConverter is a class that ...
 *
 * @author Christi Willison
 * @version 1.0,  Jul 11, 2006
 */
public class SkimConverter {

    static String skimsDir;
    static String matrixFormat;
    static String season;
    protected static Logger logger = Logger.getLogger(SkimConverter.class);

    static String[] driveDistancePropNames = {"sovTime", "sovDist"};

    static String[] drivePeakDistanceCores = { "\"AB_ConIVTT / BA_ConIVTT\"", "\"Length (Skim)\""};
    //static String[] drivePeakDistanceFiles = { "SummerAMPeakDriveDistanceSkim.csv", "SummerPMPeakDriveDistanceSkim.csv"};
    static String[] drivePeakDistanceFiles = { "AMPeakDriveDistanceSkim.csv", "PMPeakDriveDistanceSkim.csv"};

    static String[] driveOffPeakDistanceCores = { "\"AB_UnConIVTT / BA_UnConIVTT\"", "\"Length (Skim)\""};
//    static String[] driveOffPeakDistanceFiles = {"SummerMiddayDriveDistanceSkim.csv" , "SummerLateNightDriveDistanceSkim.csv"};
    static String[] driveOffPeakDistanceFiles = {"MiddayDriveDistanceSkim.csv" , "LateNightDriveDistanceSkim.csv"};


    static String[] peakTransitPropNames = {"wtWalk","wtWait1","wtWait2","wtLbs","wtGon","wtXfers","wtFare"};
    static String[] peakTransitCores = {"\"Walk Time\"","\"Initial Wait Time\"","\"Transfer Wait Time\"","\"Bus Time\"",
            "\"Gondola Time\"","\"Number of Transfers\"","\"Fare\""};
//    static String[] peakTransitFiles = {"SummerAMPeakTransitSkim.csv","SummerPMPeakTransitSkim.csv",
//            "SummerMiddayTransitSkim.csv", "SummerLateNightTransitSkim.csv"};
    static String[] peakTransitFiles = {"AMPeakTransitSkim.csv","PMPeakTransitSkim.csv", "MiddayTransitSkim.csv", "LateNightTransitSkim.csv"};

    static String[] drive2TransitPropNames = { "dtWalk","dtDrive","dtWait1","dtWait2","dtLbs","dtGon","dtXfers","dtFare"};
    static String[] drive2TransitCores = {"\"Walk Time\"","\"Access Drive Time\"","\"Initial Wait Time\"","\"Transfer Wait Time\"",
            "\"Bus Time\"","\"Gondola Time\"","\"Number of Transfers\"","\"Fare\""};
//    static String[] drive2TransitFiles = {"SummerAMPeakDrive2TransitSkim.csv","SummerPMPeakDrive2TransitSkim.csv",
//            "SummerMiddayDrive2TransitSkim.csv","SummerLateNightDrive2TransitSkim.csv"};
    static String[] drive2TransitFiles = {"AMPeakDrive2TransitSkim.csv","PMPeakDrive2TransitSkim.csv",
            "MiddayDrive2TransitSkim.csv","LateNightDrive2TransitSkim.csv"};
    
    static String[] nonMotorizedFiles = {"walkTime","walkDist","bikeTime","bikeDist"};
    static String[] nonMotorizedCores = {"\"WALK_TIME\"","\"WALK_DIST\"","\"BIKE_TIME\"","\"BIKE_DIST\""};

    public static void main(String[] args) {

        ResourceBundle rb = ResourceUtil.getResourceBundle("tahoe");
        skimsDir = rb.getString("skims.directory");
        matrixFormat = rb.getString("skims.format");
        if (ResourceUtil.getBooleanProperty(rb,"summer")) {
            season = "Summer";
        } else {
            season = "Winter";
        }
        //add season to key names
        String[][] keyArrays = {drivePeakDistanceFiles,driveOffPeakDistanceFiles,peakTransitFiles,drive2TransitFiles};
        for (String[] keyArray : keyArrays) {
            for (int i = 0; i < keyArray.length; i++) {
                keyArray[i] = season + keyArray[i];
            }
        }

        //Peak Drive Distance
        Matrix[] matrices;
        String outputFileSuffix = "";
        for(String ddFileName: drivePeakDistanceFiles){
            if(ddFileName.contains("AM")) outputFileSuffix = "Am";
            else if(ddFileName.contains("PM")) outputFileSuffix = "Pm";
            else if(ddFileName.contains("Midday")) outputFileSuffix = "Md";
            else if(ddFileName.contains("LateNight")) outputFileSuffix = "Ln";
            File file = new File(skimsDir + "/" + ddFileName );
            System.out.println("Reading file " + file);
            CSVMatrixReader reader = new CSVMatrixReader(file);
            matrices = reader.readMatrices();
//            matrices[1] = reader.readMatrix(drivePeakDistanceCores[1]);
//            System.out.println("Max of matrix " + drivePeakDistanceCores[1] + " is " + matrices[1].getMax());
            for(int i=0; i< drivePeakDistanceCores.length; i++){
                logger.info("Max of matrix " + drivePeakDistanceCores[i] + " is " + matrices[i].getMax());
                File outputFile = new File(skimsDir + "/" + driveDistancePropNames[i] + outputFileSuffix + "." + matrixFormat);
                logger.info("Writing file " + outputFile);
                ZipMatrixWriter writer = new ZipMatrixWriter(outputFile);
                writer.writeMatrix(matrices[i]);
            }
        }

        //OffPeak Drive Distance
        outputFileSuffix = "";
        for(String ddopFileName: driveOffPeakDistanceFiles){
            if(ddopFileName.contains("AM")) outputFileSuffix = "Am";
            else if(ddopFileName.contains("PM")) outputFileSuffix = "Pm";
            else if(ddopFileName.contains("Midday")) outputFileSuffix = "Md";
            else if(ddopFileName.contains("LateNight")) outputFileSuffix = "Ln";
            File file = new File(skimsDir + "/" + ddopFileName );
            logger.info("Reading file " + file);
            CSVMatrixReader reader = new CSVMatrixReader(file);
            matrices = reader.readMatrices();
            for(int i=0; i< driveOffPeakDistanceCores.length; i++){
                File outputFile = new File(skimsDir + "/" + driveDistancePropNames[i] + outputFileSuffix + "." + matrixFormat);
                logger.info("Writing file " + outputFile);
                ZipMatrixWriter writer = new ZipMatrixWriter(outputFile);
                writer.writeMatrix(matrices[i]);
            }
        }

        //Transit
        matrices = new Matrix[peakTransitCores.length];
        outputFileSuffix = "";
        for(String ptFileName: peakTransitFiles){
            if(ptFileName.contains("AM")) outputFileSuffix = "Am";
            else if(ptFileName.contains("PM")) outputFileSuffix = "Pm";
            else if(ptFileName.contains("Midday")) outputFileSuffix = "Md";
            else if(ptFileName.contains("LateNight")) outputFileSuffix = "Ln";
            File file = new File(skimsDir + "/" + ptFileName );
            for(int i=0; i< peakTransitCores.length; i++){
            	logger.info("Reading file " + file);
                CSVMatrixReader reader = new CSVMatrixReader(file);
                matrices[i] = reader.readMatrix(peakTransitCores[i]);
                File outputFile = new File(skimsDir + "/" + peakTransitPropNames[i] + outputFileSuffix + "." + matrixFormat);
                ZipMatrixWriter writer = new ZipMatrixWriter(outputFile);
                writer.writeMatrix(matrices[i]);
                logger.info("Writing file " + outputFile);
            }
        }

        //New Transit  Method
        outputFileSuffix = "";
        for(String ptFileName: peakTransitFiles){
            if(ptFileName.contains("AM")) outputFileSuffix = "Am";
            else if(ptFileName.contains("PM")) outputFileSuffix = "Pm";
            else if(ptFileName.contains("Midday")) outputFileSuffix = "Md";
            else if(ptFileName.contains("LateNight")) outputFileSuffix = "Ln";
            File file = new File(skimsDir + "/" + ptFileName );
            logger.info("Reading file " + file);
            CSVMatrixReader reader = new CSVMatrixReader(file);
            matrices = reader.readMatrices();
            int count = 0;
            for(int i=0; i< peakTransitCores.length; i++){
                for(Matrix m : matrices){
                    if(m.getName().equals(peakTransitCores[i])){
                        File outputFile = new File(skimsDir + "/" + peakTransitPropNames[i] + outputFileSuffix + "." + matrixFormat);
                        ZipMatrixWriter writer = new ZipMatrixWriter(outputFile);
                        writer.writeMatrix(m);
                        count++;
                        logger.info("Writing file " + outputFile);
                    }
                }
            }
            assert(count == peakTransitPropNames.length);
        }

        //Drive2Transit
        matrices = new Matrix[drive2TransitCores.length];
        outputFileSuffix = "";
        for(String ptFileName: drive2TransitFiles){
            if(ptFileName.contains("AM")) outputFileSuffix = "Am";
            else if(ptFileName.contains("PM")) outputFileSuffix = "Pm";
            else if(ptFileName.contains("Midday")) outputFileSuffix = "Md";
            else if(ptFileName.contains("LateNight")) outputFileSuffix = "Ln";
            File file = new File(skimsDir + "/" + ptFileName );
            for(int i=0; i< drive2TransitCores.length; i++){
            	logger.info("Reading file " + file);
                CSVMatrixReader reader = new CSVMatrixReader(file);
                matrices[i] = reader.readMatrix(drive2TransitCores[i]);
                File outputFile = new File(skimsDir + "/" + drive2TransitPropNames[i] + outputFileSuffix + "." + matrixFormat);
                ZipMatrixWriter writer = new ZipMatrixWriter(outputFile);
                writer.writeMatrix(matrices[i]);
                logger.info("Writing file " + outputFile);
            }
        }
        
        //non-motorized
        for(int i = 0; i<nonMotorizedFiles.length;++i) {
        	File inFile = new File(skimsDir + "/"+nonMotorizedFiles[i]+".csv");
        	File outFile = new File(skimsDir + "/"+nonMotorizedFiles[i]+ "." + matrixFormat);
        	logger.info("Reading file "+inFile);
        	CSVMatrixReader reader = new CSVMatrixReader(inFile);
        	Matrix m = reader.readMatrix(nonMotorizedCores[i]);
        	logger.info("Done reading file "+inFile);
            ZipMatrixWriter writer = new ZipMatrixWriter(outFile);
            logger.info("Writing file " + outFile);
            writer.writeMatrix(m);
        	logger.info("Done writing file "+outFile);
        }

    }


}
