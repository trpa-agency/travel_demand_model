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

import com.pb.common.datafile.CSVFileWriter;
import com.pb.common.datafile.TableDataSet;
import com.pb.common.util.ResourceUtil;
import com.pb.tahoe.structures.PatternType;
import com.pb.tahoe.structures.PersonType;
import com.pb.tahoe.synpop.HH;
import com.pb.tahoe.synpop.ModelCategories;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.ResourceBundle;

/**
 * DataWriter is a class that writes out the results of the Tahoe model.  It will need a resource
 * bundle to consult for file paths and output file names.
 *
 * @author Christi Willison
 * @version 1.0,  Feb 21, 2006
 */
public class DataWriter {

    protected static Logger logger = Logger.getLogger(DataWriter.class);
    protected ResourceBundle tahoeResources;

    private static DataWriter instance = new DataWriter();

    public static final String HHID_FIELD = "hh_id";                      /** headers for the HH file */
    public static final String HHTAZID_FIELD = "hh_taz_id";
    public static final String HHORIGTAZWALKSEGMENT_FIELD = "hh_walk_subzone";
    public static final String RETIRED_FIELD = "ret";
    public static final String INCOME_INT_FIELD = "income";
    public static final String WORKERS_F_FIELD = "workers_f";
    public static final String WORKERS_P_FIELD = "workers_p";
    public static final String NONWORKERS_FIELD = "nonworkers";
    public static final String PRESCHOOL_FIELD = "preschool";
    public static final String SCHOOLPRED_FIELD = "schoolpred";
    public static final String SCHOOLDRIV_FIELD = "schooldriv";
    public static final String AUTO_OWN_FIELD = "auto_own";

    public static final String PERSONID_FIELD = "person_id";
    public static final String PERSONTYPE_FIELD = "person_type";
    public static final String PATTERN_FIELD = "pattern";


    private DataWriter (){
        tahoeResources = ResourceUtil.getResourceBundle("tahoe");
    }

    public static DataWriter getInstance(){
        return instance;
    }

    public void writeHHTableDataSet (HH[][] hhs) {
        TableDataSet hhTable = createHHTableDataSet(hhs);
        writeOutputFile("synthetic.household.file", hhTable);
        for (int i = 4; i <= 10; i++) {
            String[] descriptions = OutputDescription.getDescriptions(hhTable.getColumnLabel(i));
            TableDataSet.logColumnFreqReport("Synthetic Households", hhTable, i, descriptions);
        }

    }

    private TableDataSet createHHTableDataSet(HH[][] hhs) {

        ArrayList<String> headings = new ArrayList <String>();

        headings.add(HHID_FIELD);
        headings.add(HHTAZID_FIELD);
        headings.add(HHORIGTAZWALKSEGMENT_FIELD);
        headings.add(RETIRED_FIELD);
        headings.add(INCOME_INT_FIELD);
        headings.add(WORKERS_F_FIELD);
        headings.add(WORKERS_P_FIELD);
        headings.add(NONWORKERS_FIELD);
        headings.add(PRESCHOOL_FIELD);
        headings.add(SCHOOLPRED_FIELD);
        headings.add(SCHOOLDRIV_FIELD);

        int nHhs = 0;
        for (int i = 0; i < hhs.length; i++){
            if(hhs[i] == null) continue;        //if there were no HHs in a zone, then the array will be null, skip it and go to the next zone.
            nHhs += hhs[i].length;
        }

        float[][] tableData = new float[nHhs][headings.size()];

        int hh = 0;
        for (int i = 0; i <hhs.length; i++) {
            if (hhs[i] != null) {
                for (int j = 0; j < hhs[i].length; j++) {
                    tableData[hh][0] = hhs[i][j].getHHNumber();
                    tableData[hh][1] = hhs[i][j].getZoneNumber();
                    tableData[hh][2] = hhs[i][j].getInitialOriginWalkSegment(hhs[i][j].getZoneNumber());
                    tableData[hh][3] = hhs[i][j].isRetiredPersonPresent();
                    tableData[hh][4] = ModelCategories.getIncomeCategory(hhs[i][j].attribs[2]).ordinal();


                //counting up the persons of the various types
                    for (int k = 0; k < hhs[i][j].personTypes.length; k++)
                        tableData[hh][hhs[i][j].personTypes[k] + 4] = ((tableData[hh][hhs[i][j].personTypes[k] + 4]) + 1);

                    
                    hh++;
                }
            }
        }

        return TableDataSet.create(tableData, headings);
    }

    public void writeTargetsTableDataSet(float[][] data) {
        TableDataSet targetTable = createTargetsTableDataSet(data);
        writeOutputFile("zonal.targets.file", targetTable);
    }

    private TableDataSet createTargetsTableDataSet(float[][] data ) {
        ArrayList<String> headings = new ArrayList<String>();

        headings.add("ZONE");

        String[] labels = ModelCategories.getSizeLabels();
        for (String label : labels) headings.add("HHSIZE_" + label);

        labels = ModelCategories.getWorkerLabels();
        for (String label1 : labels) headings.add("WORKER_" + label1);

        labels = ModelCategories.getIncomeLabels();
        for (String label2 : labels) headings.add("INCOME_" + label2);

        return TableDataSet.create(data, headings);

    }

    public void writeAOFile(TableDataSet hhTable){
        writeOutputFile("auto.ownership.output.file", hhTable);

    }

    public void writeSynPopPTableDataSet(TableDataSet hhTable){
        TableDataSet personTable = createPersonDataTable(hhTable);
        writeOutputFile("synthetic.person.file", personTable);
    }

    private TableDataSet createPersonDataTable(TableDataSet hhTable) {

		int workers_f_col=hhTable.getColumnPosition(WORKERS_F_FIELD);
	    int workers_p_col=hhTable.getColumnPosition(WORKERS_P_FIELD);
	    int nonworkers_col=hhTable.getColumnPosition(NONWORKERS_FIELD);
	    int preschool_col=hhTable.getColumnPosition(PRESCHOOL_FIELD);
	    int schoolpred_col=hhTable.getColumnPosition(SCHOOLPRED_FIELD);
	    int schooldriv_col=hhTable.getColumnPosition(SCHOOLDRIV_FIELD);

        float numOfWorkers_f = hhTable.getColumnTotal(workers_f_col);
	    float numOfWorkers_p = hhTable.getColumnTotal(workers_p_col);
	    float numOfNonworkers = hhTable.getColumnTotal(nonworkers_col);
	    float numOfPreschoolers = hhTable.getColumnTotal(preschool_col);
	    float numOfSchoolpredrivers = hhTable.getColumnTotal(schoolpred_col);
	    float numOfSchooldrivers = hhTable.getColumnTotal(schooldriv_col);

	    int numOfPersons = (int)(numOfWorkers_f + numOfWorkers_p  + numOfNonworkers
	                        + numOfPreschoolers + numOfSchoolpredrivers + numOfSchooldrivers);
        logger.info("Total number of persons: " + numOfPersons);

        ArrayList<String> tableHeadings=new ArrayList<String>();
        tableHeadings.add(HHID_FIELD);
	    tableHeadings.add(PERSONID_FIELD);
	    tableHeadings.add(PERSONTYPE_FIELD);
	    tableHeadings.add(PATTERN_FIELD);
        float[][] tableData = new float[numOfPersons][tableHeadings.size()];
        String[] patternDescriptionColumn = new String[numOfPersons];
        String[] personDescriptionColumn = new String[numOfPersons];


        //look thru SynPopH file and find the headers that
        //correspond to the daily activity pattern choices
        //for the various person types.
        ArrayList<String>[] patternHeaders = new ArrayList[PersonType.TYPES];
        for(int l=0; l < PersonType.TYPES; l++){
             patternHeaders[l] = new ArrayList<String>();
        }

        for(int h=0; h< hhTable.getColumnLabels().length; h++){
            String label = hhTable.getColumnLabels()[h];
            if(label.contains("_" + PersonType.FULL_TIME))
                patternHeaders[0].add(label);
            else if (label.contains("_" + PersonType.PART_TIME))
                patternHeaders[1].add(label);
            else if (label.contains("_" + PersonType.NON_WORKER))
                patternHeaders[2].add(label);
            else if (label.contains("_" + PersonType.PRESCHOOLER))
                patternHeaders[3].add(label);
            else if (label.contains("_" + PersonType.PREDRIVER))
                patternHeaders[4].add(label);
            else if (label.contains("_" + PersonType.DRIVER))
                patternHeaders[5].add(label);
        }

        //initialize the array that holds the number of persons
        //in each household of each type that has a particular
        //pattern.
        int[][] patternFrequency = new int[PersonType.TYPES][];
        for (int t = 0; t<PersonType.TYPES; t++){
            patternFrequency[t] = new int[patternHeaders[t].size()];
        }

        int rowPointer=0;
        int[] nPersonsOfPersonTypeInHH = new int[PersonType.TYPES];

        for(int i=1; i<=hhTable.getRowCount(); i++) {
            //get the number of persons in each household of each type that
            //has a particular pattern.
            for(int p=0; p < PersonType.TYPES; p++){
                for(int f=0; f< patternHeaders[p].size(); f++){
                    patternFrequency[p][f] = (int) hhTable.getValueAt(i,patternHeaders[p].get(f));
                }
            }

            nPersonsOfPersonTypeInHH[PersonType.WORKER_F-1]=(int)hhTable.getValueAt(i,workers_f_col);
            nPersonsOfPersonTypeInHH[PersonType.WORKER_P-1]=(int)hhTable.getValueAt(i,workers_p_col);
            nPersonsOfPersonTypeInHH[PersonType.NONWORKER-1]=(int)hhTable.getValueAt(i,nonworkers_col);
            nPersonsOfPersonTypeInHH[PersonType.PRESCHOOL-1]=(int)hhTable.getValueAt(i,preschool_col);
            nPersonsOfPersonTypeInHH[PersonType.SCHOOL_PRED-1]=(int)hhTable.getValueAt(i,schoolpred_col);
            nPersonsOfPersonTypeInHH[PersonType.SCHOOL_DRIV-1]=(int)hhTable.getValueAt(i,schooldriv_col);

            float hhID=hhTable.getValueAt(i,hhTable.getColumnPosition(HHID_FIELD));
            float personID=1;
            //loop thru the person types:
            // 1. check to see if there are any persons of that type.
            // 2.  if there are persons of that type, loop thru the list of daily
            //     activity patterns (the pattern headers) and check to see if the
            //     frequency (pattern frequency array) is greater than 0.
            // 3.  if there are people of that pattern, create a data row and add it
            //     to the table data set and increment the rowPointer index.
            for(int pt=0; pt<PersonType.TYPES; pt++){
                if(nPersonsOfPersonTypeInHH[pt]>0) {     //figure out their pattern
                    int personType = pt+1;
                    for(int l=0; l<patternHeaders[pt].size(); l++) { //loop thru the pattern headers until you find a pattern for everyone
                        if(patternFrequency[pt][l] > 0) {   //someone must have chosen this pattern - was it everybody?
                            short patternType = PatternType.getPatternType(patternHeaders[pt].get(l));
                            for(int pf=1; pf<=patternFrequency[pt][l];pf++){
                                tableData[rowPointer]=new float[]{hhID, personID, personType, patternType};
                                patternDescriptionColumn[rowPointer] = PatternType.getDescription(patternType);
                                personDescriptionColumn[rowPointer] = PersonType.getDescription((short)personType);
                                rowPointer++;
                                personID++;

                            }
                        }
                    }
                }  //else go to the next pattern type
            }

        }
        TableDataSet personTable = TableDataSet.create(tableData,tableHeadings);
        personTable.appendColumn(personDescriptionColumn, "person_descr");
        personTable.appendColumn(patternDescriptionColumn, "pattern_descr");

        return personTable;
    }

    public void writeDAPFile(TableDataSet hhTable){
        writeOutputFile("daily.activity.pattern.output.file", hhTable);
    }

    public void writeOutputFile(String propName, TableDataSet dataTable) {

        try {
            String path = tahoeResources.getString(propName);
            if(path == null) throw new RuntimeException("Property " + propName + " does not exist in resource bundle");
            logger.info("\t\tWriting output file: " + path);
            File outputFile = new File (path) ;
            CSVFileWriter writer = new CSVFileWriter();
            writer.writeFile(dataTable, outputFile, new DecimalFormat("#.00"));
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }
}
