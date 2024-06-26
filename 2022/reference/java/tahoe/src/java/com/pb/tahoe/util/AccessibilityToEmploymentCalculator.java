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

import com.pb.common.datafile.OLD_CSVFileReader;
import com.pb.common.datafile.TableDataSet;
import com.pb.common.matrix.Matrix;
import com.pb.common.matrix.ZipMatrixReader;
import com.pb.common.util.ResourceUtil;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.util.ResourceBundle;

/**
 * AccessibilityToEmploymentCalculator is a class that will calculate
 * how much employment is available within the time parameter specified by
 * the user.  The type of employment will also be specified by the user.
 * The idea is that you would use this class to create a column of data that could be
 * added to an existing TableDataSet or to a new TableDataSet..
 *
 * @author Christi Willison
 * @version 1.0,  May 23, 2006
 */
public class AccessibilityToEmploymentCalculator {
     static Logger logger = Logger.getLogger(AccessibilityToEmploymentCalculator.class);
    /**
     * It is assumed that the employment table has employment categories as columns
     * and that the first column is the zone numbers.  The table will be indexed by the first column
     * (the zones).  The type of employment that will be summed up will be based on the
     * header names.  The user should only include headers for the employment types that should
     * be summed up.
     *
     * The method will return an array that has the employment totals for each zone in the skim matrix.
     * @param timeParameter
     * @param skim
     * @param empTableHeaderNames
     * @param employmentTable
     * @return  employmentTotalsByZone
     */
    public static float[] calculateEmploymentByZone (float timeParameter, Matrix skim, String[] empTableHeaderNames, TableDataSet employmentTable){
        int nElements = employmentTable.getRowCount();
        float[] employmentTotalsByZone = new float[nElements];

        //Index the table by the TAZ column
        employmentTable.buildIndex(1);

        //For each row of the employment table,
        //go thru each destination zone and look at the skim time.
        //If it is <= the time parameter, the employment for each type
        //specified in the empTableHeaderNames array will be added
        //to the employmentTotalsByZone array.
        int[] originTazs = employmentTable.getColumnAsInt(1);
        int[] destTazs = employmentTable.getColumnAsInt(1);
        int index = 0;
        for(int origin: originTazs){
            for(int destination: destTazs){
                //logger.info("Getting employment for zone pair ( " + origin + "," + destination + ")" );
                float skimTime = skim.getValueAt(origin, destination);
                if ((skimTime >=0 && skimTime <= timeParameter) || (origin == destination)) {
                    for(String empType : empTableHeaderNames){
                        int columnPosition = employmentTable.getColumnPosition(empType);
                        employmentTotalsByZone[index] += employmentTable.getIndexedValueAt(destination, columnPosition);
                    }
                }
            }
            index ++;
        }

        return employmentTotalsByZone;
    }

    /**
     * User should pass in only the header names for the employment for which the total should be calculated.
     * @param empTableHeaderNames
     * @param employmentTable
     * @return total employment for type requested
     */
    public static int calculateTotalEmploymentInRegion(String[] empTableHeaderNames, TableDataSet employmentTable){
        int totalEmp = 0;
        for(int r=1; r <= employmentTable.getRowCount(); r++){
            for(String empType : empTableHeaderNames){
                        int columnPosition = employmentTable.getColumnPosition(empType);
                        totalEmp += employmentTable.getValueAt(r, columnPosition);
                    }
        }
        return totalEmp;
    }

    public static float[] calculatePercentEmploymentByZone(float[] empByZone, int totalEmp){
        float[] percentEmpByZone = new float[empByZone.length];
        for(int i=0; i< empByZone.length; i++){
            percentEmpByZone[i] = empByZone[i] /totalEmp;
        }
        return percentEmpByZone;
    }

    public static void calculateAccessibilitiesForTahoe() {
        ResourceBundle rb = ResourceUtil.getResourceBundle("tahoe");
        String[] headers = new String[] {"emp_retail", "emp_srvc", "emp_rec", "emp_game", "emp_other"};


        //First calculate total employment of all types and total employment of retail type
        String empTablePath = rb.getString("socio.economic.data.file");
        TableDataSet empTable = null;
        OLD_CSVFileReader reader = new OLD_CSVFileReader();
        try {
            empTable = reader.readFile(new File(empTablePath));
        } catch (IOException e) {
            throw new RuntimeException("Could not read file " + empTablePath);
        }
        int all_totalEmp = AccessibilityToEmploymentCalculator.calculateTotalEmploymentInRegion(headers, empTable);
        int retail_totalEmp = AccessibilityToEmploymentCalculator.calculateTotalEmploymentInRegion(new String[]{"emp_retail"}, empTable);

        logger.info("\t\tAll -Total Employment = " + all_totalEmp);
        logger.info("\t\tRetail - Total Employment = " + retail_totalEmp);

        //AUTO ACCESSIBLE EMPLOYMENT
        //Next read in the auto skims and calc accessibility to all employment within 30 minutes of each zone.
        float timeParam = 30.0f;
        File distanceSkimFile = new File(rb.getString("skims.directory") + "/" + rb.getString("sovDistAm.file") + "." + rb.getString("skims.format"));
        Matrix distanceSkim = ZipMatrixReader.readMatrix(distanceSkimFile,"Distance Skim Matrix");
        File autoSkimFile = new File(rb.getString("skims.directory") + "/" + rb.getString("sovTimeAm.file") + "." + rb.getString("skims.format"));
        Matrix autoSkim = ZipMatrixReader.readMatrix(autoSkimFile,"Auto Skim Matrix");



        float[] allEmpAuto30 = AccessibilityToEmploymentCalculator.calculateEmploymentByZone(timeParam, autoSkim, headers, empTable);
        float[] retailEmpAuto30 = AccessibilityToEmploymentCalculator.calculateEmploymentByZone(timeParam, autoSkim, new String[]{"emp_retail"}, empTable);

        //Now calculate the percentage of employment by zone (these will be added to the zoneMappings file below)
        float[] percentAllEmpAuto30 = AccessibilityToEmploymentCalculator.calculatePercentEmploymentByZone(allEmpAuto30, all_totalEmp);
        float[] percentRetailEmpAuto30 = AccessibilityToEmploymentCalculator.calculatePercentEmploymentByZone(retailEmpAuto30, retail_totalEmp);


        //WALK ACCESSIBLE EMPLOYMENT
        //Next read the walk skims and calculate accessibility to retail and all employment within 20 minutes of each zone.
        timeParam = 20.0f;

        Matrix walkSkim = distanceSkim.multiply(20.0f);

        float[] allEmpWalk20 = AccessibilityToEmploymentCalculator.calculateEmploymentByZone(timeParam, walkSkim, headers, empTable);
        float[] retailEmpWalk20 = AccessibilityToEmploymentCalculator.calculateEmploymentByZone(timeParam, walkSkim, new String[]{"emp_retail"}, empTable);

        //Now calculate the percentage of employment by zone (these will be added to the zoneMappings file below)
        float[] percentAllEmpWalk20 = AccessibilityToEmploymentCalculator.calculatePercentEmploymentByZone(allEmpWalk20, all_totalEmp);
        float[] percentRetailEmpWalk20 = AccessibilityToEmploymentCalculator.calculatePercentEmploymentByZone(retailEmpWalk20, retail_totalEmp);




        //TRANSIT ACCESSIBLE EMPLOYMENT
        timeParam = 20.0f;
        File dtDriveFile = new File(rb.getString("skims.directory") + "/" + rb.getString("dtDriveAm.file") + "." + rb.getString("skims.format"));
        Matrix dtDriveSkim = ZipMatrixReader.readMatrix(dtDriveFile,"Drive to Transit Skim Matrix");
        File dtGonFile = new File(rb.getString("skims.directory") + "/" + rb.getString("dtGonAm.file") + "." + rb.getString("skims.format"));
        Matrix dtGonSkim = ZipMatrixReader.readMatrix(dtGonFile,"Gondola Transit Skim Matrix");
        File dtLBSFile = new File(rb.getString("skims.directory") + "/" + rb.getString("dtLbsAm.file") + "." + rb.getString("skims.format"));
        Matrix dtLBSSkim = ZipMatrixReader.readMatrix(dtLBSFile,"LBS Transit Skim Matrix");
        File dtWait1File = new File(rb.getString("skims.directory") + "/" + rb.getString("dtWait1Am.file") + "." + rb.getString("skims.format"));
        Matrix dtWait1Skim = ZipMatrixReader.readMatrix(dtWait1File,"Wait1 Transit Skim Matrix");
        File dtWait2File = new File(rb.getString("skims.directory") + "/" + rb.getString("dtWait2Am.file") + "." + rb.getString("skims.format"));
        Matrix dtWait2Skim = ZipMatrixReader.readMatrix(dtWait2File,"Wait2 Transit Skim Matrix");
        File dtWalkFile = new File(rb.getString("skims.directory") + "/" + rb.getString("dtWalkAm.file") + "." + rb.getString("skims.format"));
        Matrix dtWalkSkim = ZipMatrixReader.readMatrix(dtWalkFile,"Walk Transit Skim Matrix");
        File dtXfersFile = new File(rb.getString("skims.directory") + "/" + rb.getString("dtXfersAm.file") + "." + rb.getString("skims.format"));
        Matrix dtXfersSkim = ZipMatrixReader.readMatrix(dtXfersFile,"Xfers Transit Skim Matrix");

        Matrix transitSkim = dtDriveSkim.add(dtGonSkim.add(dtLBSSkim.add(dtWait1Skim.add(dtWait2Skim.add(dtWalkSkim.add(dtXfersSkim))))));

        float[] allEmpTransit20 = AccessibilityToEmploymentCalculator.calculateEmploymentByZone(timeParam, transitSkim, headers, empTable);
        float[] retailTransit30 = AccessibilityToEmploymentCalculator.calculateEmploymentByZone(30.0f, transitSkim, new String[]{"emp_retail"}, empTable);

        //Now calculate the percentage of employment by zone (these will be added to the zoneMappings file below)
        float[] percentAllEmpTransit20 = AccessibilityToEmploymentCalculator.calculatePercentEmploymentByZone(allEmpTransit20, all_totalEmp);
        float[] percentRetailTransit30 = AccessibilityToEmploymentCalculator.calculatePercentEmploymentByZone(retailTransit30, retail_totalEmp);


        //Now read in the existing zoneMappings file and append the columns that have been calculated.
        TableDataSet resultsTable = new TableDataSet();
        resultsTable.appendColumn(empTable.getColumnAsInt("taz"), "taz");

        resultsTable.appendColumn(percentAllEmpWalk20, "work_walk_20");
        resultsTable.appendColumn(percentRetailEmpWalk20, "retail_walk_20");
        resultsTable.appendColumn(percentAllEmpAuto30, "work_a_30");
        resultsTable.appendColumn(percentAllEmpTransit20, "work_tr_20");
        resultsTable.appendColumn(percentRetailTransit30, "retail_tr_30");
        resultsTable.appendColumn(percentRetailEmpAuto30, "retail_a_30");


        DataWriter writer = DataWriter.getInstance();
        writer.writeOutputFile("accessibility.to.employment.file", resultsTable);

        
    }

    public static void main(String[] args) {

        AccessibilityToEmploymentCalculator.calculateAccessibilitiesForTahoe();
//        ResourceBundle rb = ResourceUtil.getResourceBundle("tahoe");
//        String[] all_headers = new String[] {"emp_retail", "emp_srvc", "emp_rec", "emp_game", "emp_other"};
//        String[] retail_header = new String[] {"emp_retail"};
//
//
//        //First calculate total employment of all types and total employment of retail type
//        String empTablePath = rb.getString("socio.economic.data.file");
//        TableDataSet empTable = null;
//        OLD_CSVFileReader reader = new OLD_CSVFileReader();
//        try {
//            empTable = reader.readFile(new File(empTablePath));
//        } catch (IOException e) {
//            throw new RuntimeException("Could not read file " + empTablePath);
//        }
//        int all_totalEmp = AccessibilityToEmploymentCalculator.calculateTotalEmploymentInRegion(all_headers, empTable);
//        int retail_totalEmp = AccessibilityToEmploymentCalculator.calculateTotalEmploymentInRegion(retail_header, empTable);
//
//        logger.info("All -Total Employment = " + all_totalEmp);
//        logger.info("Retail - Total Employment = " + retail_totalEmp);
//
//        //AUTO ACCESSIBLE EMPLOYMENT
//        //Next read in the auto skims and calc accessibility to all employment within 30 minutes of each zone.
//        float[] timeParams = {20.0f, 30.0f, 40.0f};
//        String autoSkimPath = rb.getString("peak.time.auto.skim");
//        Matrix autoSkim = MatrixReader.readMatrix(new File(autoSkimPath), null);
//        float[][] percentAllEmpAuto = new float[timeParams.length][];
//        float[][] percentRetailEmpAuto = new float[timeParams.length][];
//
//        for(int i=0; i<timeParams.length; i++){
//            percentAllEmpAuto[i] = AccessibilityToEmploymentCalculator.calculateEmploymentByZone(timeParams[i], autoSkim, all_headers, empTable);
//            percentRetailEmpAuto[i] = AccessibilityToEmploymentCalculator.calculateEmploymentByZone(timeParams[i], autoSkim, retail_header, empTable);
//
//            percentAllEmpAuto[i] = AccessibilityToEmploymentCalculator.calculatePercentEmploymentByZone(percentAllEmpAuto[i], all_totalEmp);
//            percentRetailEmpAuto[i] = AccessibilityToEmploymentCalculator.calculatePercentEmploymentByZone(percentRetailEmpAuto[i], retail_totalEmp);
//        }
//
//
//        //WALK ACCESSIBLE EMPLOYMENT
//        //Next read the walk skims and calculate accessibility to retail and all employment within 20 minutes of each zone.
//        float timeParam = 20.0f;
//
//        String walkSkimPath = rb.getString("peak.time.walk.skim");
//        Matrix walkSkim = MatrixReader.readMatrix(new File(walkSkimPath), null);
//
//        float[] allEmpWalk20 = AccessibilityToEmploymentCalculator.calculateEmploymentByZone(timeParam, walkSkim, all_headers, empTable);
//        float[] retailEmpWalk20 = AccessibilityToEmploymentCalculator.calculateEmploymentByZone(timeParam, walkSkim, retail_header, empTable);
//
//        //Now calculate the percentage of employment by zone (these will be added to the zoneMappings file below)
//        float[] percentAllEmpWalk20 = AccessibilityToEmploymentCalculator.calculatePercentEmploymentByZone(allEmpWalk20, all_totalEmp);
//        float[] percentRetailEmpWalk20 = AccessibilityToEmploymentCalculator.calculatePercentEmploymentByZone(retailEmpWalk20, retail_totalEmp);
//
//
//
//
//        //TRANSIT ACCESSIBLE EMPLOYMENT
//        String transitSkimPath = rb.getString("peak.time.transit.skim");
//        Matrix transitSkim = MatrixReader.readMatrix(new File(transitSkimPath), null);
//
//        float[][] percentAllEmpTransit = new float[timeParams.length][];
//        float[][] percentRetailEmpTransit = new float[timeParams.length][];
//
//        for(int i=0; i< timeParams.length; i++){
//            percentAllEmpTransit[i] = AccessibilityToEmploymentCalculator.calculateEmploymentByZone(timeParams[i], transitSkim, all_headers, empTable);
//            percentRetailEmpTransit[i] = AccessibilityToEmploymentCalculator.calculateEmploymentByZone(timeParams[i], transitSkim, retail_header, empTable);
//
//            percentAllEmpTransit[i] = AccessibilityToEmploymentCalculator.calculatePercentEmploymentByZone(percentAllEmpTransit[i], all_totalEmp);
//            percentRetailEmpTransit[i] = AccessibilityToEmploymentCalculator.calculatePercentEmploymentByZone(percentRetailEmpTransit[i], retail_totalEmp);
//        }
//
//
//
//
//        //Now read in the existing zoneMappings file and append the columns that have been calculated.
//        String zoneMappingsPath = rb.getString("taz.correspondence.file");
//        TableDataSet resultsTable = null;
//        try {
//            resultsTable = reader.readFile(new File(zoneMappingsPath));
//        } catch (IOException e) {
//            throw new RuntimeException ("Could not open " + zoneMappingsPath);
//        }
//
//        resultsTable.appendColumn(percentAllEmpWalk20, "work_walk_20_java");
//        resultsTable.appendColumn(percentRetailEmpWalk20, "retail_walk_20_java");
//        for(int i=0; i<timeParams.length; i++){
//            String header = "work_a_" + timeParams[i] + "_java";
//            resultsTable.appendColumn(percentAllEmpAuto[i], header);
//        }
//
//        for(int i=0; i<timeParams.length; i++){
//            String header = "retail_a_" + timeParams[i] + "_java";
//            resultsTable.appendColumn(percentRetailEmpAuto[i], header);
//        }
//
//        for(int i=0; i<timeParams.length; i++){
//            String header = "work_tr_" + timeParams[i] + "_java";
//            resultsTable.appendColumn(percentAllEmpTransit[i], header);
//        }
//
//        for(int i=0; i<timeParams.length; i++){
//            String header = "retail_tr_" + timeParams[i] + "_java";
//            resultsTable.appendColumn(percentRetailEmpTransit[i], header);
//        }
//
//
//        CSVFileWriter writer = new CSVFileWriter();
//        try {
//            writer.writeFile(resultsTable, new File(zoneMappingsPath),  new DecimalFormat("#.##"));
//        } catch (IOException e) {
//            throw new RuntimeException("Could not write to " + zoneMappingsPath);
//        }

    }

}
