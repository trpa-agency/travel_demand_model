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

import com.pb.common.datafile.CSVFileReader;
import com.pb.common.datafile.DiskObjectArray;
import com.pb.common.datafile.TableDataSet;
import com.pb.common.util.ResourceUtil;
import com.pb.tahoe.structures.*;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.util.ResourceBundle;

public class HouseholdArrayManager implements java.io.Serializable  {
    static Logger logger = Logger.getLogger(HouseholdArrayManager.class);

    public static HouseholdArrayManager instance = new HouseholdArrayManager();

    static ResourceBundle propertyMap;

    int hhsProcessed = 0;
    int totalHhsToProcess = 0;

    Household[] bigHHArray = null;



    private HouseholdArrayManager() {
        propertyMap = ResourceUtil.getResourceBundle("tahoe");

    }

    public static HouseholdArrayManager getInstance(){
        return instance;
    }


    public void createBigHHArray() {
        Tour[] it = null;

        String hhFile = propertyMap.getString("daily.activity.pattern.output.file");
        String personFile = propertyMap.getString("synthetic.person.file");


        int hh_id;


        // read household file
        TableDataSet hhTable = null;

        try {
            CSVFileReader reader = new CSVFileReader();
            hhTable = reader.readFile(new File(hhFile));
        } catch (IOException e) {
            throw new RuntimeException("Error reading HH file " + hhFile);
        }

        Household[] tempHHs = new Household[hhTable.getRowCount()+1];  //hh_id's start at 1, the index into the array is the hh number.
        Person[][] tempPersons = new Person[hhTable.getRowCount()+1][];    //person_id's start at 1, the index into the array is the hhnumber and the person number

        // get column positions of the fields needed from the household table
        // for either writing out or for aggregation required procedures
        int hh_idPosition = hhTable.getColumnPosition(DataWriter.HHID_FIELD);
        int hh_taz_idPosition = hhTable.getColumnPosition(DataWriter.HHTAZID_FIELD);
        int origWalkSegment = hhTable.getColumnPosition(DataWriter.HHORIGTAZWALKSEGMENT_FIELD);
        int workers_f_col = hhTable.getColumnPosition(DataWriter.WORKERS_F_FIELD);
        int workers_p_col = hhTable.getColumnPosition(DataWriter.WORKERS_P_FIELD);
        int nonworkers_col = hhTable.getColumnPosition(DataWriter.NONWORKERS_FIELD);
        int preschool_col = hhTable.getColumnPosition(DataWriter.PRESCHOOL_FIELD);
        int schoolpred_col = hhTable.getColumnPosition(DataWriter.SCHOOLPRED_FIELD);
        int schooldriv_col = hhTable.getColumnPosition(DataWriter.SCHOOLDRIV_FIELD);

        int income_col = hhTable.getColumnPosition(DataWriter.INCOME_INT_FIELD);

        int autoOwn_colPosition = hhTable.getColumnPosition(DataWriter.AUTO_OWN_FIELD);

        // loop over all households in the hh table and fill in info for
        // a new Household object in an array.
        boolean[] tempAvail;

        for (int r = 1; r <= hhTable.getRowCount(); r++) {
            int index = (int) hhTable.getValueAt(r, hh_idPosition);
            if(tempHHs[index] != null){
                throw new RuntimeException("There are 2 hhs with the id number " + index);
            }
            tempHHs[index] = new Household();
            tempHHs[index].setID((int) hhTable.getValueAt(r, hh_idPosition));
            tempHHs[index].setTazID((int) hhTable.getValueAt(r,
                    hh_taz_idPosition));
            tempHHs[index].setOriginWalkSegment((int) hhTable.getValueAt(r, origWalkSegment));
            tempHHs[index].setHHIncome((int) hhTable.getValueAt(r, income_col));

            int numOfWorkers_f = (int) hhTable.getValueAt(r, workers_f_col);
            int numOfWorkers_p = (int) hhTable.getValueAt(r, workers_p_col);
            int numOfNonworkers = (int) hhTable.getValueAt(r, nonworkers_col);
            int numOfPreschool = (int) hhTable.getValueAt(r, preschool_col);
            int numOfSchoolpred = (int) hhTable.getValueAt(r, schoolpred_col);
            int numOfSchooldriv = (int) hhTable.getValueAt(r, schooldriv_col);
            int hhSize = (int) (numOfWorkers_f + numOfWorkers_p + numOfNonworkers + numOfPreschool +
                numOfSchoolpred + numOfSchooldriv);

            tempHHs[index].setHHSize(hhSize);

            tempHHs[index].setAutoOwnership((int) hhTable.getValueAt(r, autoOwn_colPosition));

            tempPersons[index] = new Person[hhSize + 1];
        }
         //next household

        // read person file and count number of adults and children with travel active daily patterns
        TableDataSet personTable = null;

        try {
            CSVFileReader reader = new CSVFileReader();
            personTable = reader.readFile(new File(personFile));
        } catch (IOException e) {
            throw new RuntimeException("Couldn't read person file " + personFile, e);
        }

        int[] hhTravelActiveAdults = new int[hhTable.getRowCount()+1];
        int[] hhTravelActiveChildren = new int[hhTable.getRowCount()+1];
        int[] hhTravelActiveNonPreschool = new int[hhTable.getRowCount()+1];

        // loop over all persons in each hh of the person table, add Person objects and info
        // to Household objects
        hh_idPosition = personTable.getColumnPosition(DataWriter.HHID_FIELD);

        if (hh_idPosition <= 0) {
            HouseholdArrayManager.logger.fatal(DataWriter.HHID_FIELD +
                " was not a field in the householdData TableDataSet.");
            System.exit(1);
        }

        int person_idPosition = personTable.getColumnPosition(DataWriter.PERSONID_FIELD);
        int person_typePosition = personTable.getColumnPosition(DataWriter.PERSONTYPE_FIELD);
        int pattern_choicePosition = personTable.getColumnPosition(DataWriter.PATTERN_FIELD);

        for (int r = 1; r <= personTable.getRowCount(); r++) {
            hh_id = (int) personTable.getValueAt(r, hh_idPosition);

            int person_id = (int) personTable.getValueAt(r,
                    person_idPosition);
            int personType = (int) personTable.getValueAt(r,
                    person_typePosition);

            tempPersons[hh_id][person_id] = new Person();
            tempPersons[hh_id][person_id].setID(person_id);
            tempPersons[hh_id][person_id].setPersonType(personType);

            tempHHs[hh_id].incrementPersonsByType(personType);

            int patternType = (int) personTable.getValueAt(r, pattern_choicePosition);
            tempPersons[hh_id][person_id].setPatternType(patternType);

            if(patternType != PatternType.HOME){
                if(PersonType.isAdult((short)personType) ){
                    hhTravelActiveAdults[hh_id]++;
                    hhTravelActiveNonPreschool[hh_id]++;
                } else {
                    if(!PersonType.isPreschooler((short) personType)){
                        hhTravelActiveNonPreschool[hh_id]++;
                    }
                    hhTravelActiveChildren[hh_id]++;
                }
            }

        }

        // set the number of traveling adults and children in the Household objects
        // and the person objects in the Household objects.
        // create an array of mandatory tours for each person in hh based on patternType
        // determined in models 2.1-2.7.
        for (int r = 1; r <= hhTable.getRowCount(); r++) {
            tempHHs[r].setTravelActiveAdults(hhTravelActiveAdults[r]);
            tempHHs[r].setTravelActiveChildren(hhTravelActiveChildren[r]);
            tempHHs[r].setTravelActiveNonPreschool(hhTravelActiveNonPreschool[r]);

            tempHHs[r].setPersonArray(tempPersons[r]);

            // count the total number of mandatory tours in the hh.
            int numMandatoryTours = 0;
            Person[] persons = tempHHs[r].getPersonArray();

            for (int p = 1; p < persons.length; p++) {
                int patternType = persons[p].getPatternType();

                switch (patternType) {
                case PatternType.WORK_1:
                case PatternType.SCHOOL_1:
                    numMandatoryTours += 1;
                    break;

                case PatternType.WORK_2:
                case PatternType.SCHOOL_2:
                case PatternType.SCHOOL_WORK:
                    numMandatoryTours += 2;
                    break;

                }
            }

            // create the mandatory tours array and set person participation
            int k = 0;

            if (numMandatoryTours > 0) {
                it = new Tour[numMandatoryTours];
            } else {
                it = null;
            }

            for (int p = 1; p < persons.length; p++) {
                persons[p].setMandatoryTourParticipationArray(numMandatoryTours);

                int patternType = persons[p].getPatternType();

                switch (patternType) {
                case PatternType.WORK_1:
                    it[k] = new Tour(tempHHs[r].getHHSize());
                    it[k].setTourType(TourType.WORK);
                    it[k].setTourOrder(0);
                    it[k].setPersonParticipation(p, true);
                    persons[p].setMandatoryTourParticipation(k, true);
                    persons[p].setNumMandTours(1);
                    tempHHs[r].incrementMandatoryToursByType(TourType.WORK);
                    k++;

                    break;

                case PatternType.WORK_2:
                    it[k] = new Tour(tempHHs[r].getHHSize());
                    it[k].setTourType(TourType.WORK);
                    it[k].setTourOrder(1);
                    it[k].setPersonParticipation(p, true);
                    persons[p].setMandatoryTourParticipation(k, true);
                    tempHHs[r].incrementMandatoryToursByType(TourType.WORK);
                    k++;
                    it[k] = new Tour(tempHHs[r].getHHSize());
                    it[k].setTourType(TourType.WORK);
                    it[k].setTourOrder(2);
                    it[k].setPersonParticipation(p, true);
                    persons[p].setNumMandTours(2);
                    persons[p].setMandatoryTourParticipation(k, true);
                    tempHHs[r].incrementMandatoryToursByType(TourType.WORK);
                    k++;

                    break;

                case PatternType.SCHOOL_1:
                    it[k] = new Tour(tempHHs[r].getHHSize());
                    it[k].setTourType(TourType.SCHOOL);
                    it[k].setTourOrder(0);
                    it[k].setPersonParticipation(p, true);
                    persons[p].setMandatoryTourParticipation(k, true);
                    persons[p].setNumMandTours(1);
                    tempHHs[r].incrementMandatoryToursByType(TourType.SCHOOL);
                    k++;

                    break;

                case PatternType.SCHOOL_2:
                    it[k] = new Tour(tempHHs[r].getHHSize());
                    it[k].setTourType(TourType.SCHOOL);
                    it[k].setTourOrder(1);
                    it[k].setPersonParticipation(p, true);
                    persons[p].setMandatoryTourParticipation(k, true);
                    tempHHs[r].incrementMandatoryToursByType(TourType.SCHOOL);
                    k++;
                    it[k] = new Tour(tempHHs[r].getHHSize());
                    it[k].setTourType(TourType.SCHOOL);
                    it[k].setTourOrder(2);
                    it[k].setPersonParticipation(p, true);
                    persons[p].setMandatoryTourParticipation(k, true);
                    persons[p].setNumMandTours(2);
                    tempHHs[r].incrementMandatoryToursByType(TourType.SCHOOL);
                    k++;

                    break;

                case PatternType.SCHOOL_WORK:
                    it[k] = new Tour(tempHHs[r].getHHSize());
                    it[k].setTourType(TourType.SCHOOL);
                    it[k].setTourOrder(1);
                    it[k].setPersonParticipation(p, true);
                    persons[p].setMandatoryTourParticipation(k, true);
                    tempHHs[r].incrementMandatoryToursByType(TourType.SCHOOL);
                    k++;
                    it[k] = new Tour(tempHHs[r].getHHSize());
                    it[k].setTourType(TourType.WORK);
                    it[k].setTourOrder(2);
                    it[k].setPersonParticipation(p, true);
                    persons[p].setMandatoryTourParticipation(k, true);
                    persons[p].setNumMandTours(2);
                    tempHHs[r].incrementMandatoryToursByType(TourType.WORK);
                    k++;

                    break;


                }
            }

            // count the number and type of travelers in the hh.
            int nonPreschoolTravelers = 0;
            int preschoolTravelers = 0;

            for (int p = 1; p < persons.length; p++) {
                int patternType = persons[p].getPatternType();
                int personType = persons[p].getPersonType();

                if (patternType != PatternType.HOME) {
                    if (personType == PersonType.PRESCHOOL) {
                        preschoolTravelers++;
                    } else {
                        nonPreschoolTravelers++;
                    }
                } else {
                    // set all time windows unavailable for persons that chose at-home pattern
                    tempAvail = persons[p].getAvailable();

                    for (int j = 0; j < tempAvail.length; j++)
                        persons[p].setHourUnavailable(j);
                }
            }

            //TODO - talk this over with Tracey in regards to the usage in Joint tours.
            int hhType = 0;
            if (nonPreschoolTravelers == 0) {
                hhType = 0;
            } else if ((nonPreschoolTravelers == 1) &&
                    (preschoolTravelers == 0)) {
                hhType = 1;
            } else {
                hhType = 2;
            }

//            if (nonPreschoolTravelers == 0) {
//                hhType = 0;
//            } else if ((nonPreschoolTravelers == 1) &&
//                    (preschoolTravelers == 0)) {
//                hhType = 1;
//            } else {
//                hhType = 2;
//            }

            tempHHs[r].setHHType(hhType);

            tempHHs[r].setMandatoryTours(it);

            // set the array which records what person id belongs to each person type
            tempHHs[r].setPersonsByPersonTypeArray();

        }

        hhTable = null;

        totalHhsToProcess = tempHHs.length;

        bigHHArray = tempHHs;
        tempPersons = null;

        HouseholdArrayManager.logger.info("\tHousehold Array has been created");
    }



    public Household[] getHouseholds() {
        // return the array of hh objects.
        return bigHHArray;
    }



    public void sendResults(Household[] hh) {
        int hhId;

        for (int i = 1; i < hh.length; i++) {
            hhId = hh[i].getID();
            bigHHArray[hhId] = hh[i];
        }
    }

    public int getNumberOfHouseoldsToProcess() {
        return totalHhsToProcess;
    }

    public void writeDiskObjectArray( String fileName ) {

    	int NoHHs=bigHHArray.length;

    	try{
    		DiskObjectArray diskObjectArray = new DiskObjectArray(fileName, NoHHs, 10000);
    		//write each hh to disk object array
    		for(int i=0; i<NoHHs; i++){
        		diskObjectArray.add(i,bigHHArray[i]);         
        	}
    	}catch(IOException e){
    		logger.fatal("can not open disk object array file for writing");
    	}
    }

    public void createBigHHArrayFromDiskObject(String suffix){
    	String diskObjectArrayFile=propertyMap.getString("DiskObjectArrayInput.file");

    	try{
    		DiskObjectArray diskObjectArray=new DiskObjectArray(diskObjectArrayFile + suffix);
    		int NoHHs=diskObjectArray.getArraySize();
    		bigHHArray=new Household[NoHHs];
    		for(int i=0; i<NoHHs; i++){
    			bigHHArray[i]=(Household)diskObjectArray.get(i);
    		}
    		totalHhsToProcess=bigHHArray.length;
            diskObjectArray.close();
        }catch(IOException e){
    		logger.error("can not open disk object array file for reading.");
    	}
    }

    public void clearTODWindows() {
        for (Household hh : bigHHArray) {
            if (hh == null)
                continue;
            for (Person p : hh.persons) {
                if (p==null)
                    continue;
                for (int i = 1; i <= TemporalType.HOURS; i++) {
                    p.setHourAvailable(i);
                }
            }
        }
    }

    public static void main(String[] args) {
        HouseholdArrayManager ham = HouseholdArrayManager.getInstance();
        ham.createBigHHArray();
    }

}
