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
package com.pb.tahoe.dest_time_mode;

import com.pb.common.util.ResourceUtil;
import com.pb.tahoe.structures.Household;
import com.pb.tahoe.structures.JointTour;
import com.pb.tahoe.structures.Person;
import com.pb.tahoe.structures.Tour;
import com.pb.tahoe.structures.TourType;
import com.pb.tahoe.util.DataWriter;
import com.pb.tahoe.util.HouseholdArrayManager;
import com.pb.tahoe.util.TODDataManager;
import org.apache.log4j.Logger;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.ResourceBundle;

/**
 * @author Jim Hicks  / Christi Willison
 *
 * Model runner class for running destination, time of day, and mode choice for
 * individual tours
 */
public class DTMOutput implements java.io.Serializable {

    static Logger logger = Logger.getLogger(DTMOutput.class);



    public void writeDTMChoiceResults(short tourCategory, File outputFile, Household[] hhs, int everyNth){
        ArrayList<String> tableHeadings = new ArrayList<String>();
        tableHeadings.add(DataWriter.HHID_FIELD);
        tableHeadings.add(DataWriter.HHTAZID_FIELD);
        tableHeadings.add(DataWriter.INCOME_INT_FIELD);
        tableHeadings.add("person_id");
        tableHeadings.add("personType");
        tableHeadings.add("patternType");
        tableHeadings.add("tour_id");
        tableHeadings.add("tourCategory");
        tableHeadings.add("purpose");
        tableHeadings.add("origTaz");
        tableHeadings.add("orig_WLKseg");
        tableHeadings.add("destTaz");
        tableHeadings.add("dest_WLKseg");
        tableHeadings.add("TOD");
        tableHeadings.add("TOD_StartHr");
        tableHeadings.add("TOD_EndHr");
        tableHeadings.add("TOD_StartPeriod");
        tableHeadings.add("TOD_EndPeriod");
        tableHeadings.add("TOD_Output_StartPeriod");
        tableHeadings.add("TOD_Output_EndPeriod");
        tableHeadings.add("MC");

        // define an array for use in writing output file
		float[] tableData = new float[tableHeadings.size()];

        // write trips for individual mandatory tours
        int hh_id;
        int hh_taz_id;
        int hh_inc;
        Tour[] it;
        try {
            PrintWriter outStream = new PrintWriter (new BufferedWriter( new FileWriter(outputFile) ) );
            //Print titles
            outStream.print( (String)tableHeadings.get(0) );
            for (int i = 1; i < tableHeadings.size(); i++) {
                outStream.print(",");
                outStream.print( (String)tableHeadings.get(i) );
            }
            outStream.println();
            for (int i=1; i < hhs.length; i++) {

                if (i % everyNth == 0) {
                    int table_col = 0;
                    hh_id = hhs[i].getID();
                    hh_taz_id = hhs[i].getTazID();
                    hh_inc = hhs[i].getHHIncome();
                    Person[] persons = hhs[i].getPersonArray();

                    tableData[table_col++] = hh_id;
                    tableData[table_col++] = hh_taz_id;
                    tableData[table_col++] = hh_inc;
                    int table_sub_col;
                    if(tourCategory == TourType.MANDATORY_CATEGORY)
                            it = hhs[i].getMandatoryTours();
                    else if(tourCategory == TourType.NON_MANDATORY_CATEGORY)
                            it=hhs[i].getIndivTours();
                    else throw new RuntimeException("this method does not apply for tour category " + TourType.getCategoryLabelForCategory(tourCategory));
                    if (it != null) {
                        for (int t=0; t < it.length; t++) {
                            table_sub_col = table_col;

                            try {   tableData[table_sub_col++] = it[t].getTourPerson();
                                    tableData[table_sub_col++] = persons[it[t].getTourPerson()].getPersonType();
                                    tableData[table_sub_col++] = persons[it[t].getTourPerson()].getPatternType();
                                    tableData[table_sub_col++] = t+1;
                                    tableData[table_sub_col++] = tourCategory;
                                    tableData[table_sub_col++] = it[t].getTourType();
                                    tableData[table_sub_col++] = it[t].getOrigTaz();
                                    tableData[table_sub_col++] = it[t].getOriginShrtWlk();
                                    tableData[table_sub_col++] = it[t].getDestTaz();
                                    tableData[table_sub_col++] = it[t].getDestShrtWlk();
                                    tableData[table_sub_col++] = it[t].getTimeOfDayAlt();
                                    tableData[table_sub_col++] = TODDataManager.getTodStartHour( it[t].getTimeOfDayAlt() );
                                    tableData[table_sub_col++] = TODDataManager.getTodEndHour( it[t].getTimeOfDayAlt() );
                                    tableData[table_sub_col++] = TODDataManager.getTodStartPeriod( it[t].getTimeOfDayAlt() );
                                    tableData[table_sub_col++] = TODDataManager.getTodEndPeriod( it[t].getTimeOfDayAlt() );
                                    tableData[table_sub_col++] = TODDataManager.getTodStartSkimPeriod( it[t].getTimeOfDayAlt() );
                                    tableData[table_sub_col++] = TODDataManager.getTodEndSkimPeriod( it[t].getTimeOfDayAlt() );
                                    tableData[table_sub_col++] = it[t].getMode();

                                    outStream.print( tableData[0] );
                                    for (int c=1; c < tableHeadings.size(); c++) {
                                        outStream.print(",");
                                        outStream.print( tableData[c] );
                                    }
                                    outStream.println();
                                } catch (Exception e) {
//                                    if (hh_id < 60 && hh_id > 49) {
//                                        hhs[i].printHouseholdState();
//                                        logger.info(e);
//                                        for (StackTraceElement se : e.getStackTrace())
//                                            logger.info(se);
//                                        logger.info(it[t].getTourPerson());
//                                        logger.info(it[t].getDestTaz());
//                                        System.exit(0);
//                                    }
                                    logger.warn("Household (ID) " + hh_id + " has a DTM problem!");
                                }
                            }
                        }
                    }
                }
                outStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public void writeJointDTMChoiceResults(File outputFile, Household[] hhs, int everyNth){

        int maxPartySize = 0;
        for (int i=1; i < hhs.length; i++) {
            if (hhs[i].jointTours != null) {
                for (int j=0; j < hhs[i].jointTours.length; j++) {
                    if (hhs[i].jointTours[j].getNumPersons() > maxPartySize)
                        maxPartySize = hhs[i].jointTours[j].getNumPersons();
                }
            }
        }
        ArrayList<String> tableHeadings = new ArrayList<String>();
        tableHeadings.add(DataWriter.HHID_FIELD);
        tableHeadings.add(DataWriter.HHTAZID_FIELD);
        tableHeadings.add(DataWriter.INCOME_INT_FIELD);
        tableHeadings.add("jt_party_size");
        for (int i=0; i < maxPartySize; i++) {
            tableHeadings.add("jt_person_" + i + "_id");
            tableHeadings.add("jt_person_" + i + "_type");
        }
        tableHeadings.add("tour_id");
        tableHeadings.add("tourCategory");
        tableHeadings.add("purpose");
        tableHeadings.add("origTaz");
        tableHeadings.add("orig_WLKseg");
        tableHeadings.add("destTaz");
        tableHeadings.add("dest_WLKseg");
        tableHeadings.add("TOD");
        tableHeadings.add("TOD_StartHr");
        tableHeadings.add("TOD_EndHr");
        tableHeadings.add("TOD_StartPeriod");
        tableHeadings.add("TOD_EndPeriod");
        tableHeadings.add("TOD_Output_StartPeriod");
        tableHeadings.add("TOD_Output_EndPeriod");
        tableHeadings.add("MC");

        // define an array for use in writing output file
		float[] tableData = new float[tableHeadings.size()];

        // write trips for individual mandatory tours
        int hh_id;
        int hh_taz_id;
        int hh_inc;
        JointTour[] jt;
        try {
            PrintWriter outStream = new PrintWriter (new BufferedWriter( new FileWriter(outputFile) ) );
            //Print titles
            outStream.print( (String)tableHeadings.get(0) );
            for (int i = 1; i < tableHeadings.size(); i++) {
                outStream.print(",");
                outStream.print( (String)tableHeadings.get(i) );
            }
            outStream.println();
            for (int i=1; i < hhs.length; i++) {

                if (i % everyNth == 0) {
                    int table_col = 0;
                    hh_id = hhs[i].getID();
                    hh_taz_id = hhs[i].getTazID();
                    hh_inc = hhs[i].getHHIncome();
                    Person[] persons = hhs[i].getPersonArray();

                    tableData[table_col++] = hh_id;
                    tableData[table_col++] = hh_taz_id;
                    tableData[table_col++] = hh_inc;
                    int table_sub_col;

                    // write joint tours in the output table
                    jt = hhs[i].getJointTours();
                    if (jt != null) {
                        for (int t=0; t < jt.length; t++) {
                            table_sub_col = table_col;
                            int[] jtPersons = jt[t].getJointTourPersons();

                            try {
                                tableData[table_sub_col++] = jtPersons.length;
						        for (int j=0; j < jtPersons.length; j++) {
							        tableData[table_sub_col++] = jtPersons[j];
							        tableData[table_sub_col++] = persons[jtPersons[j]].getPersonType();
						        }
                                //if the number of persons in this joint tour is less than the max party size
                                //table_sub_col will not have been incremented correctly so we have to set it to
                                //the correct value.
                                table_sub_col = 4 + (2*maxPartySize);
                                tableData[table_sub_col++] = t+1;
                                tableData[table_sub_col++] = 2;
                                tableData[table_sub_col++] = jt[t].getTourType();
                                tableData[table_sub_col++] = jt[t].getOrigTaz();
                                tableData[table_sub_col++] = jt[t].getOriginShrtWlk();
                                tableData[table_sub_col++] = jt[t].getDestTaz();
                                tableData[table_sub_col++] = jt[t].getDestShrtWlk();
                                tableData[table_sub_col++] = jt[t].getTimeOfDayAlt();
                                tableData[table_sub_col++] = TODDataManager.getTodStartHour( jt[t].getTimeOfDayAlt() );
                                tableData[table_sub_col++] = TODDataManager.getTodEndHour( jt[t].getTimeOfDayAlt() );
                                tableData[table_sub_col++] = TODDataManager.getTodStartPeriod( jt[t].getTimeOfDayAlt() );
                                tableData[table_sub_col++] = TODDataManager.getTodEndPeriod( jt[t].getTimeOfDayAlt() );
                                tableData[table_sub_col++] = TODDataManager.getTodStartSkimPeriod( jt[t].getTimeOfDayAlt() );
                                tableData[table_sub_col++] = TODDataManager.getTodEndSkimPeriod( jt[t].getTimeOfDayAlt() );
                                tableData[table_sub_col++] = jt[t].getMode();

                                outStream.print( tableData[0] );
                                for (int c=1; c < tableHeadings.size(); c++) {
                                    outStream.print(",");
                                    outStream.print( tableData[c] );
                                }
                                outStream.println();
                                } catch (Exception e) {
                                    //hhs[i].printHouseholdState();
                                    if (hh_id == 80) {
                                        hhs[i].printHouseholdState();
                                    }
                                    if (hh_id == 200) {
                                        hhs[i].printHouseholdState();
                                    }
                                    logger.warn("Household (ID) " + hh_id + " has a DTM problem!");
                                }
                            }
                        }
                    }
                }
                outStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public void writeMandatoryDTMChoiceResults(File outputFile, Household[] hhs, int everyNth){
        writeDTMChoiceResults(TourType.MANDATORY_CATEGORY, outputFile, hhs, everyNth);
    }



    public void writeIndivNonMandDTMChoiceResults(File outputFile, Household[] hhs, int everyNth){
        writeDTMChoiceResults(TourType.NON_MANDATORY_CATEGORY, outputFile, hhs, everyNth);
    }

    public void writeAtWorkDTMChoiceResults(File outputFile, Household[] hhs, int everyNth){
        ArrayList<String> tableHeadings = new ArrayList<String>();
        tableHeadings.add(DataWriter.HHID_FIELD);
        tableHeadings.add(DataWriter.HHTAZID_FIELD);
        tableHeadings.add(DataWriter.INCOME_INT_FIELD);
        tableHeadings.add("person_id");
        tableHeadings.add("personType");
        tableHeadings.add("patternType");
        tableHeadings.add("tour_id");
        tableHeadings.add("tourCategory");
        tableHeadings.add("purpose");
        tableHeadings.add("origTaz");
        tableHeadings.add("orig_WLKseg");
        tableHeadings.add("destTaz");
        tableHeadings.add("dest_WLKseg");
        tableHeadings.add("TOD");
        tableHeadings.add("TOD_StartHr");
        tableHeadings.add("TOD_EndHr");
        tableHeadings.add("TOD_StartPeriod");
        tableHeadings.add("TOD_EndPeriod");
        tableHeadings.add("TOD_Output_StartPeriod");
        tableHeadings.add("TOD_Output_EndPeriod");
        tableHeadings.add("MC");

        // define an array for use in writing output file
		float[] tableData = new float[tableHeadings.size()];

        // write trips for at work tours
        int hh_id;
        int hh_taz_id;
        int hh_inc;
        Tour[] it;
        Tour[] st;
        try {
            PrintWriter outStream = new PrintWriter (new BufferedWriter( new FileWriter(outputFile) ) );
            //Print titles
            outStream.print( (String)tableHeadings.get(0) );
            for (int i = 1; i < tableHeadings.size(); i++) {
                outStream.print(",");
                outStream.print( (String)tableHeadings.get(i) );
            }
            outStream.println();
            for (int i=1; i < hhs.length; i++) {

                if (i % everyNth == 0) {
                    int table_col = 0;
                    hh_id = hhs[i].getID();
                    hh_taz_id = hhs[i].getTazID();
                    hh_inc = hhs[i].getHHIncome();
                    Person[] persons = hhs[i].getPersonArray();

                    tableData[table_col++] = hh_id;
                    tableData[table_col++] = hh_taz_id;
                    tableData[table_col++] = hh_inc;
                    int table_sub_col;
                    it = hhs[i].getMandatoryTours();
                    if (it != null) {
                        for (int t=0; t < it.length; t++) {
                            if (it[t].getTourType() == TourType.WORK) {
							    st = it[t].getSubTours();
							    if (st != null) {
                                    for (int s=0; s < st.length; s++) {

                                        table_sub_col = table_col;

                                        try {
                                            tableData[table_sub_col++] = st[s].getTourPerson();
                                            tableData[table_sub_col++] = persons[st[s].getTourPerson()].getPersonType();
                                            tableData[table_sub_col++] = persons[st[s].getTourPerson()].getPatternType();
                                            tableData[table_sub_col++] = (t+1)*10 + (s+1);
                                            tableData[table_sub_col++] = TourType.AT_WORK_CATEGORY;
                                            tableData[table_sub_col++] = st[s].getTourType();
                                            tableData[table_sub_col++] = st[s].getOrigTaz();
                                            tableData[table_sub_col++] = st[s].getOriginShrtWlk();
                                            tableData[table_sub_col++] = st[s].getDestTaz();
                                            tableData[table_sub_col++] = st[s].getDestShrtWlk();
                                            tableData[table_sub_col++] = st[s].getTimeOfDayAlt();
                                            tableData[table_sub_col++] = TODDataManager.getTodStartHour( st[s].getTimeOfDayAlt() );
                                            tableData[table_sub_col++] = TODDataManager.getTodEndHour( st[s].getTimeOfDayAlt() );
                                            tableData[table_sub_col++] = TODDataManager.getTodStartPeriod( st[s].getTimeOfDayAlt() );
                                            tableData[table_sub_col++] = TODDataManager.getTodEndPeriod( st[s].getTimeOfDayAlt() );
                                            tableData[table_sub_col++] = TODDataManager.getTodStartSkimPeriod( st[s].getTimeOfDayAlt() );
                                            tableData[table_sub_col++] = TODDataManager.getTodEndSkimPeriod( st[s].getTimeOfDayAlt() );
                                            tableData[table_sub_col++] = st[s].getMode();

                                            outStream.print( tableData[0] );
                                            for (int c=1; c < tableHeadings.size(); c++) {
                                                outStream.print(",");
                                                outStream.print( tableData[c] );
                                            }
                                            outStream.println();
                                        } catch (Exception e) {
                                        //hhs[i].printHouseholdState();
                                            if (hh_id == 80) {
                                                hhs[i].printHouseholdState();
                                            }
                                            if (hh_id == 200) {
                                                hhs[i].printHouseholdState();
                                            }
                                            logger.warn("Household (ID) " + hh_id + " has a DTM problem!");
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
                outStream.close();
        } catch (IOException e) {
            throw new RuntimeException("Error writing to file " + outputFile);
        }
    }


    public static void main(String[] args) {
        ResourceBundle rb = ResourceUtil.getResourceBundle("tahoe");
        HouseholdArrayManager ham = HouseholdArrayManager.getInstance();
        ham.createBigHHArrayFromDiskObject("_afterMandatoryDTM.doa");
//        ham.createBigHHArray();
//        File outputFile = new File(rb.getString("mandatory_dtm.choice.output.file"));
//        DTMOutput dtmOutput = new DTMOutput();
//        dtmOutput.writeMandatoryDTMChoiceResults(outputFile, ham.getHouseholds(), 1);

        File outputFile = new File(rb.getString("joint_dtm.choice.output.file"));
        DTMOutput dtmOutput = new DTMOutput();
        dtmOutput.writeJointDTMChoiceResults(outputFile, ham.getHouseholds(), 1);
    }




}

