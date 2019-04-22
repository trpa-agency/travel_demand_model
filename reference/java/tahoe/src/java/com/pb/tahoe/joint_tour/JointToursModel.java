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
package com.pb.tahoe.joint_tour;

import com.pb.common.calculator.IndexValues;
import com.pb.common.calculator.UtilityExpressionCalculator;
import com.pb.common.model.ConcreteAlternative;
import com.pb.common.model.LogitModel;
import com.pb.tahoe.structures.Household;
import com.pb.tahoe.structures.JointTour;
import com.pb.tahoe.structures.Person;
import com.pb.tahoe.structures.PersonType;
import com.pb.tahoe.structures.TourType;
import com.pb.tahoe.util.DataWriter;
import com.pb.tahoe.util.OutputDescription;
import org.apache.log4j.Logger;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.ResourceBundle;

/**
 * JointToursModel is a class that chooses the joint tour frequency, the joint
 * tour composition and the joint tour participation.  It has been modified
 * from the MORPC original to work in the Tahoe framework
 *
 * @author Joel Freedman, modified by Christi Willison
 * @version 1.0,  Sep 5, 2006
 */
public class JointToursModel {
    static final int JT_DATA_SHEET = 0;
    static final int JTFreq_MODEL_SHEET = 1;
    static final int JTComp_MODEL_SHEET = 2;
    static final int JTPartic_MODEL_SHEET = 3;
    static Logger logger = Logger.getLogger(JointToursModel.class);
    private Household[] hh;
    ResourceBundle propertyMap;

        public JointToursModel(ResourceBundle propertyMap, Household[] hh) {
            this.propertyMap = propertyMap;
            this.hh = hh;
        }

        public void runFrequencyModel() {

            logger.info("Starting Model 3.1 -- Joint Tour Frequency");

            //Read control file for  joint tour frequency model and create UEC
            String controlFile = propertyMap.getString("joint.tour.control.file");

            // create a new UEC to get utilties for this logit model
            UtilityExpressionCalculator uec = new UtilityExpressionCalculator(new File(controlFile),
                    JTFreq_MODEL_SHEET, JT_DATA_SHEET, propertyMap, Household.class);
            int numberOfAlternatives = uec.getNumberOfAlternatives();
            String[] alternativeNames = uec.getAlternativeNames();

            // create and define a new LogitModel object
            LogitModel root = new LogitModel("root", numberOfAlternatives);
            ConcreteAlternative[] alts = new ConcreteAlternative[numberOfAlternatives];

            for (int i = 0; i < numberOfAlternatives; i++) {
                logger.debug("alternative " + (i + 1) + " is " + alternativeNames[i]);
                alts[i] = new ConcreteAlternative(alternativeNames[i], (i + 1));
                root.addAlternative(alts[i]);
                logger.debug(alternativeNames[i] + " has been added to the root");
            }

            // set availabilities
            root.computeAvailabilities();
            root.writeAvailabilities();

            int[] availability = new int[numberOfAlternatives + 1];
            Arrays.fill(availability, 1);

            //Get output file initialized
            PrintWriter outStream = null;

            //M31.csv file
            File file = new File(propertyMap.getString("joint.tour.freq.output.file"));
            //M31.csv heading
            String[] tableHeadings31 = {DataWriter.HHID_FIELD, DataWriter.HHTAZID_FIELD,
                "MaxAdultOverlaps", "MaxChildOverlaps", "MaxMixedOverlaps",
                "TravelActiveAdults", "TravelActiveChildren",
                "TravelActiveNonPreschool", "JointTourFreq","hhSize"};

            //create an array to hold summary results.
            int[] choiceFreqs = new int[numberOfAlternatives+1];
             //current record

            float[] tempRecord = new float[10];
            try {
                outStream = new PrintWriter(new BufferedWriter(new FileWriter(file)));

                writeRecord(outStream, tableHeadings31);

                // loop over all households in the hh table, write out current record
                for (int i = 1; i < hh.length; i++) {
                    tempRecord[0] = hh[i].getID();
                    tempRecord[1] = hh[i].getTazID();

                    // apply joint tour model only to households with at least 2 travel active persons, at least 1 traveling no-proeschool person,
                    // and either adult or mixed non-zero overlaps.
                    int hhType = hh[i].getHHType();

                    if ((hhType > 1) &&  ((hh[i].getMaxAdultOverlaps() > 0.0) ||  (hh[i].getMaxMixedOverlaps() > 0))) {
                        tempRecord[2] = hh[i].getMaxAdultOverlaps();
                        tempRecord[3] = hh[i].getMaxChildOverlaps();
                        tempRecord[4] = hh[i].getMaxMixedOverlaps();
                        tempRecord[5] = hh[i].getTravelActiveAdults();
                        tempRecord[6] = hh[i].getTravelActiveChildren();
                        tempRecord[7] = hh[i].getTravelActiveNonPreschool();

                        // get utilities for each alternative for this household
                        IndexValues index = new IndexValues();
                        index.setZoneIndex(hh[i].getTazID());
                        index.setHHIndex(hh[i].getID());

                        double[] utilities = uec.solve(index, hh[i], availability);

                        //set utility for each alternative
                        for (int a = 0; a < numberOfAlternatives; a++) {
                            alts[a].setAvailability(availability[a + 1] == 1);

                            if (availability[a + 1] == 1) {
                                alts[a].setAvailability((utilities[a] > -99.0));
                            }

                            alts[a].setUtility(utilities[a]);
                        }

                        // set availabilities
                        root.computeAvailabilities();

                        root.getUtility();
                        root.calculateProbabilities();

                        ConcreteAlternative chosen = (ConcreteAlternative) root.chooseElementalAlternative();
                        String chosenAltName = chosen.getName();

                        // save chosen alternative in resultsArray
                        for (int a = 0; a < numberOfAlternatives; a++) {
                            if (chosenAltName.equals(alternativeNames[a])) {

                                if (a == 0) {
                                    hh[i].jointTours = null;
                                } else if ((a > 0) && (a < 5)) {
                                    hh[i].jointTours = new JointTour[1];
                                    hh[i].jointTours[0] = new JointTour(hh[i].getHHSize());
                                } else if (a >= 5) {
                                    hh[i].jointTours = new JointTour[2];
                                    hh[i].jointTours[0] = new JointTour(hh[i].getHHSize());
                                    hh[i].jointTours[1] = new JointTour(hh[i].getHHSize());
                                }

                                switch (a) {
                                case 0:
                                    break;

                                case 1:
                                    hh[i].jointTours[0].setTourType(TourType.SHOP);
                                    hh[i].jointTours[0].setTourOrder(0);
                                    hh[i].incrementJointToursByType(TourType.SHOP);

                                    break;

                                case 2:
                                    hh[i].jointTours[0].setTourType(TourType.EAT);
                                    hh[i].jointTours[0].setTourOrder(0);
                                    hh[i].incrementJointToursByType(TourType.EAT);

                                    break;

                                case 3:
                                    hh[i].jointTours[0].setTourType(TourType.OTHER_MAINTENANCE);
                                    hh[i].jointTours[0].setTourOrder(0);
                                    hh[i].incrementJointToursByType(TourType.OTHER_MAINTENANCE);

                                    break;

                                case 4:
                                    hh[i].jointTours[0].setTourType(TourType.DISCRETIONARY);
                                    hh[i].jointTours[0].setTourOrder(0);
                                    hh[i].incrementJointToursByType(TourType.DISCRETIONARY);

                                    break;

                                case 5:
                                    hh[i].jointTours[0].setTourType(TourType.SHOP);
                                    hh[i].jointTours[0].setTourOrder(1);
                                    hh[i].incrementJointToursByType(TourType.SHOP);
                                    hh[i].jointTours[1].setTourType(TourType.SHOP);
                                    hh[i].jointTours[1].setTourOrder(2);
                                    hh[i].incrementJointToursByType(TourType.SHOP);

                                    break;

                                case 6:
                                    hh[i].jointTours[0].setTourType(TourType.SHOP);
                                    hh[i].jointTours[0].setTourOrder(0);
                                    hh[i].incrementJointToursByType(TourType.SHOP);
                                    hh[i].jointTours[1].setTourType(TourType.EAT);
                                    hh[i].jointTours[1].setTourOrder(0);
                                    hh[i].incrementJointToursByType(TourType.EAT);

                                    break;

                                case 7:
                                    hh[i].jointTours[0].setTourType(TourType.OTHER_MAINTENANCE);
                                    hh[i].jointTours[0].setTourOrder(0);
                                    hh[i].incrementJointToursByType(TourType.OTHER_MAINTENANCE);
                                    hh[i].jointTours[1].setTourType(TourType.SHOP);
                                    hh[i].jointTours[1].setTourOrder(0);
                                    hh[i].incrementJointToursByType(TourType.SHOP);

                                    break;

                                case 8:
                                    hh[i].jointTours[0].setTourType(TourType.SHOP);
                                    hh[i].jointTours[0].setTourOrder(0);
                                    hh[i].incrementJointToursByType(TourType.SHOP);
                                    hh[i].jointTours[1].setTourType(TourType.DISCRETIONARY);
                                    hh[i].jointTours[1].setTourOrder(0);
                                    hh[i].incrementJointToursByType(TourType.DISCRETIONARY);

                                    break;

                                case 9:
                                    hh[i].jointTours[0].setTourType(TourType.EAT);
                                    hh[i].jointTours[0].setTourOrder(1);
                                    hh[i].incrementJointToursByType(TourType.EAT);
                                    hh[i].jointTours[1].setTourType(TourType.EAT);
                                    hh[i].jointTours[1].setTourOrder(2);
                                    hh[i].incrementJointToursByType(TourType.EAT);

                                    break;

                                case 10:
                                    hh[i].jointTours[0].setTourType(TourType.OTHER_MAINTENANCE);
                                    hh[i].jointTours[0].setTourOrder(0);
                                    hh[i].incrementJointToursByType(TourType.OTHER_MAINTENANCE);
                                    hh[i].jointTours[1].setTourType(TourType.EAT);
                                    hh[i].jointTours[1].setTourOrder(0);
                                    hh[i].incrementJointToursByType(TourType.EAT);

                                    break;

                                case 11:
                                    hh[i].jointTours[0].setTourType(TourType.DISCRETIONARY);
                                    hh[i].jointTours[0].setTourOrder(0);
                                    hh[i].incrementJointToursByType(TourType.DISCRETIONARY);
                                    hh[i].jointTours[1].setTourType(TourType.EAT);
                                    hh[i].jointTours[1].setTourOrder(0);
                                    hh[i].incrementJointToursByType(TourType.EAT);

                                    break;

                                case 12:
                                    hh[i].jointTours[0].setTourType(TourType.OTHER_MAINTENANCE);
                                    hh[i].jointTours[0].setTourOrder(1);
                                    hh[i].incrementJointToursByType(TourType.OTHER_MAINTENANCE);
                                    hh[i].jointTours[1].setTourType(TourType.OTHER_MAINTENANCE);
                                    hh[i].jointTours[1].setTourOrder(2);
                                    hh[i].incrementJointToursByType(TourType.OTHER_MAINTENANCE);

                                    break;

                                case 13:
                                    hh[i].jointTours[0].setTourType(TourType.OTHER_MAINTENANCE);
                                    hh[i].jointTours[0].setTourOrder(0);
                                    hh[i].incrementJointToursByType(TourType.OTHER_MAINTENANCE);
                                    hh[i].jointTours[1].setTourType(TourType.DISCRETIONARY);
                                    hh[i].jointTours[1].setTourOrder(0);
                                    hh[i].incrementJointToursByType(TourType.DISCRETIONARY);

                                    break;

                                case 14:
                                    hh[i].jointTours[0].setTourType(TourType.DISCRETIONARY);
                                    hh[i].jointTours[0].setTourOrder(1);
                                    hh[i].incrementJointToursByType(TourType.DISCRETIONARY);
                                    hh[i].jointTours[1].setTourType(TourType.DISCRETIONARY);
                                    hh[i].jointTours[1].setTourOrder(2);
                                    hh[i].incrementJointToursByType(TourType.DISCRETIONARY);

                                    break;
                                }
                                tempRecord[8] = a+1;
                                tempRecord[9] = hh[i].getHHSize();
                                choiceFreqs[a+1]++;

                                //write current record to M31.csv
                                writeRecord(outStream, tempRecord);

                                break; // break out of for loop after finding chosen alternative
                            }
                        }
                    }
                }

                outStream.close();
            } catch (IOException e) {
                logger.error("IO exception when writing JointTourFrequency.csv");
            }

            //summary request status
            String summaryRequest = (String) propertyMap.getString("joint.tour.freq.summary");
            boolean summaryOutput = false;

            if (summaryRequest != null) {
                if (summaryRequest.equals("true")) {
                    summaryOutput = true;
                }
            }

            if (summaryOutput) {
                writeFreqSummaryToLogger ( "Joint Tour Frequency", "JT_Freq", choiceFreqs );
            }

            logger.info("End of Joint Tour Frequency");
        }

        public void runCompositionModel() {
            PrintWriter outStream = null;
            String[] tableHeadings32 = {
                DataWriter.HHID_FIELD, DataWriter.HHTAZID_FIELD,
                "joint_tour_id", "joint_tour_comp","tourType","hhSize"
            };
            float[] tempRecord = new float[6];
            int[] choiceFreqs = null;

            int hh_id;
            int hh_taz_id;

            logger.info("Starting  Joint Tour Party Composition Model");

            //open files named in morpc.proerties and run fully joint tour frequency model
            String controlFile = propertyMap.getString("joint.tour.control.file");
            String outputFile32 = propertyMap.getString("joint.tour.comp.output.file");
            File file = new File(outputFile32);

            // create a new UEC to get utilties for this logit model
            UtilityExpressionCalculator uec = new UtilityExpressionCalculator(new File(
                        controlFile), JTComp_MODEL_SHEET, JT_DATA_SHEET, propertyMap,
                    Household.class);
            int numberOfAlternatives = uec.getNumberOfAlternatives();
            String[] alternativeNames = uec.getAlternativeNames();

            choiceFreqs = new int[numberOfAlternatives+1];

            // create and define a new LogitModel object
            LogitModel root = new LogitModel("root", numberOfAlternatives);
            ConcreteAlternative[] alts = new ConcreteAlternative[numberOfAlternatives];

            for (int i = 0; i < numberOfAlternatives; i++) {
                logger.debug("alternative " + (i + 1) + " is " +
                    alternativeNames[i]);
                alts[i] = new ConcreteAlternative(alternativeNames[i],(i + 1));
                root.addAlternative(alts[i]);
                logger.debug(alternativeNames[i] + " has been added to the root");
            }

            // set availabilities
            root.computeAvailabilities();
            root.writeAvailabilities();

            int[] availability = new int[numberOfAlternatives + 1];
            Arrays.fill(availability, 1);

            // loop over all households in the hh table

            for (int i = 1; i < hh.length; i++) {
                hh_id = hh[i].getID();
                hh_taz_id = hh[i].getTazID();

                // loop through joint tours for this household if there are any.
                JointTour[] jt = hh[i].getJointTours();

                if (jt != null) {
                    for (int j = 0; j < jt.length; j++) {

                        hh[i].setTourID(j);
                        hh[i].setTourCategory(TourType.JOINT_CATEGORY);

                        // get utilities for each alternative for this household
                        IndexValues index = new IndexValues();
                        index.setZoneIndex(hh[i].getTazID());
                        index.setHHIndex(hh[i].getID());

                        double[] utilities = uec.solve(index, hh[i], availability);

                        //set utility for each alternative
                        for (int a = 0; a < numberOfAlternatives; a++) {
                            alts[a].setAvailability(availability[a + 1] == 1);

                            if (availability[a + 1] == 1) {
                                alts[a].setAvailability((utilities[a] > -99.0));
                            }

                            alts[a].setUtility(utilities[a]);
                        }

                        // set availabilities
                        root.computeAvailabilities();

                        root.getUtility();
                        root.calculateProbabilities();

                        ConcreteAlternative chosen = (ConcreteAlternative) root.chooseElementalAlternative();
                        String chosenAltName = chosen.getName();

                        // save chosen alternative in resultsArray
                        for (int a = 0; a < numberOfAlternatives; a++) {
                            if (chosenAltName.equals(alternativeNames[a])) {
                                // set tour composition: 1-adults, 2-children, 3-both
                                jt[j].setTourComposition(a + 1);

                                break;
                            }
                        }
                    }
                }
            }

            //next household
            String summaryRequest = propertyMap.getString("joint.tour.comp.summary");
            boolean summaryOutput = false;

            if (summaryRequest != null) {
                if (summaryRequest.equals("true")) {
                    summaryOutput = true;
                }
            }

            if (outputFile32 != null) {
                try {
                    outStream = new PrintWriter(new BufferedWriter(
                                new FileWriter(file)));

                        logger.info("Writing output file to: " + outputFile32);

                        //write M32.csv heading
                        writeRecord(outStream, tableHeadings32);

                        // loop over all households in the hh table
                        for (int i = 1; i < hh.length; i++) {
                            hh_id = hh[i].getID();
                            hh_taz_id = hh[i].getTazID();

                            JointTour[] jt = hh[i].getJointTours();

                            if (jt != null) {
                                for (int j = 0; j < jt.length; j++) {
                                    tempRecord[0] = hh_id;
                                    tempRecord[1] = hh_taz_id;
                                    tempRecord[2] = j + 1;
                                    tempRecord[3] = jt[j].getTourCompositionCode();
                                    tempRecord[4] = jt[j].getTourType();
                                    tempRecord[5] = hh[i].getHHSize();
                                    choiceFreqs[jt[j].getTourCompositionCode()]++;

                                    //write current record to M32.csv
                                    writeRecord(outStream, tempRecord);
                                }
                            }
//                            else {
//                                tempRecord[0] = hh_id;
//                                tempRecord[1] = hh_taz_id;
//                                tempRecord[2] = 0;
//                                tempRecord[3] = 0;
//                                choiceFreqs[0]++;
//
//                                //write current record to M32.csv
//                                writeRecord(outStream, tempRecord);
//                            }

                        }

                    outStream.close();
                } catch (IOException e) {
                    logger.error("IO Exception when writing csv");
                }
            }


            if (summaryOutput) {
                writeFreqSummaryToLogger ( "Party Composition", "JT_Comp", choiceFreqs );
            }


            logger.info("End of Joint Tour Composition");

        }

        public void runParticipationModel() {
          logger.info("Starting Joint Tour Participation Model");
          PrintWriter outStream = null;
          File file = null;

          //M33.csv heading
          String[] tableHeadings33 = {DataWriter.HHID_FIELD,DataWriter.HHTAZID_FIELD,"person_id","joint_tour_id","personType","patternType","tourType","composition","participation","personAvailTimeWindow","hhSize"};
          //current record
          float[] tempRecord = new float[11];
          int[] choiceFreqs = null;

            int hh_id;
            int hh_taz_id;
            int hh_size;

            int personType;
            boolean validParty;



          //open files named in morpc.proerties and run fully joint tour frequency model
            String controlFile = propertyMap.getString("joint.tour.control.file");
            String outputFile33 = propertyMap.getString("joint.tour.participation.output.file");
            file=new File(outputFile33);

            // create a new UEC to get utilties for this logit model
            UtilityExpressionCalculator uec = new UtilityExpressionCalculator(new File(
                        controlFile), JTPartic_MODEL_SHEET, JT_DATA_SHEET, propertyMap,
                    Household.class);
            int numberOfAlternatives = uec.getNumberOfAlternatives();
            String[] alternativeNames = uec.getAlternativeNames();

            choiceFreqs = new int[numberOfAlternatives+1];

            // create and define a new LogitModel object
            LogitModel root = new LogitModel("root", numberOfAlternatives);
            ConcreteAlternative[] alts = new ConcreteAlternative[numberOfAlternatives];

            for (int i = 0; i < numberOfAlternatives; i++) {
                logger.debug("alternative " + (i + 1) + " is " +
                    alternativeNames[i]);
                alts[i] = new ConcreteAlternative(alternativeNames[i], (i + 1));
                root.addAlternative(alts[i]);
                logger.debug(alternativeNames[i] + " has been added to the root");
            }

            // set availabilities
            root.computeAvailabilities();
            root.writeAvailabilities();

            int[] availability = new int[numberOfAlternatives + 1];
            Arrays.fill(availability, 1);

            // loop over all households in the hh table
            for (int i = 1; i < hh.length; i++) {
                // get joint tours array for this household.
                JointTour[] jt = hh[i].getJointTours();

                if (jt == null) {
                    continue;
                }

                hh_id = hh[i].getID();
                hh_taz_id = hh[i].getTazID();

                // get person array for this household.
                Person[] persons = hh[i].getPersonArray();

                // loop through joint tours for this household and set person participation
                for (int j = 0; j < jt.length; j++) {
                    hh[i].setTourID(j);
                    hh[i].setTourCategory(TourType.JOINT_CATEGORY);

                    // make sure each joint tour has a valid composition before going to the next one.
                    validParty = false;

                    while (!validParty) {
                        int adults = 0;
                        int children = 0;

                        for (int p = 1; p < persons.length; p++) {
                            hh[i].setPersonID(p);

                            // define an array in the person objects to hold whether or not person
                            // participates in each joint tour.
                            persons[p].setJointTourParticipationArray(jt.length);

                            personType = persons[p].getPersonType();

                            // if person type is inconsistent with tour composition, participation is by definition no.
                            if (((personType > 3) && jt[j].areChildrenInTour()) ||
                                    ((personType <= 3) && jt[j].areAdultsInTour())) {
                                // get utilities for each alternative for this household
                                IndexValues index = new IndexValues();
                                index.setZoneIndex(hh[i].getTazID());
                                index.setHHIndex(hh[i].getID());

                                double[] utilities = uec.solve(index, hh[i],
                                        availability);

                                //set utility for each alternative
                                for (int a = 0; a < numberOfAlternatives; a++) {
                                    alts[a].setAvailability(availability[a + 1] == 1);

                                    if (availability[a + 1] == 1) {
                                        alts[a].setAvailability((utilities[a] > -99.0));
                                    }

                                    alts[a].setUtility(utilities[a]);
                                }

                                // set availabilities
                                root.computeAvailabilities();

                                root.getUtility();
                                root.calculateProbabilities();

                                ConcreteAlternative chosen = (ConcreteAlternative) root.chooseElementalAlternative();
                                String chosenAltName = chosen.getName();

                                // find index of the chosen alternative
                                int alt = 0;

                                for (int a = 0; a < numberOfAlternatives; a++) {
                                    if (chosenAltName.equals(alternativeNames[a])) {
                                        alt = a + 1;

                                        break;
                                    }
                                }

                                if (alt == 1) {
                                    // set person participation to true for this tour
                                    jt[j].setPersonParticipation(p, true);
                                    persons[p].setJointTourParticipation(j, true);

                                    if (personType > 3) {
                                        children++;
                                    } else {
                                        adults++;
                                    }
                                } else {
                                    // set person participation for this tour to false
                                    jt[j].setPersonParticipation(p, false);
                                    persons[p].setJointTourParticipation(j, false);
                                }
                            } else {
                                // set person participation for this tour to false
                                jt[j].setPersonParticipation(p, false);
                                persons[p].setJointTourParticipation(j, false);
                            }
                        }

                        //end for persons
                        validParty = jt[j].isTourValid(children, adults);
                    }

                    // end while
                }

                //end for joint tours
                // save number of joint tours in which person participates
                for (int p = 1; p < persons.length; p++) {
                    int numJointTours = 0;

                    for (int j = 0; j < jt.length; j++) {
                        if (jt[j].getPersonParticipation(p)) {
                            numJointTours++;
                        }
                    }

                    persons[p].setNumJointTours(numJointTours);
                }

                // save number of persons in joint tours
                for (int j = 0; j < jt.length; j++) {
                    int numPersons = 0;
                    int numAdults = 0;
                    int numChildren = 0;
                    int numPreschool = 0;
                    int numPredriv = 0;
                    int allWorkFull = 1;

                    for (int p = 1; p < persons.length; p++) {
                        if (jt[j].getPersonParticipation(p)) {
                            numPersons++;

                            if ((persons[p].getPersonType() == PersonType.PRESCHOOL) ||
                                    (persons[p].getPersonType() == PersonType.SCHOOL_DRIV) ||
                                    (persons[p].getPersonType() == PersonType.SCHOOL_PRED)) {
                                numChildren++;
                            } else {
                                numAdults++;

                                if (persons[p].getPersonType() != PersonType.WORKER_F) {
                                    allWorkFull = 0;
                                }
                            }

                            if (persons[p].getPersonType() == PersonType.PRESCHOOL) {
                                numPreschool++;
                            } else if (persons[p].getPersonType() == PersonType.SCHOOL_PRED) {
                                numPredriv++;
                            }
                        }
                    }

                    jt[j].setNumPersons(numPersons);
                    jt[j].setNumAdults(numAdults);
                    jt[j].setNumChildren(numChildren);

                    jt[j].setNumPreschool(numPreschool);
                    jt[j].setNumPredriv(numPredriv);
                    jt[j].setAllAdultsWorkFull(allWorkFull);
                }
            }//next household

            //M33.csv summary request status
            String summaryRequest = propertyMap.getString("joint.tour.participation.summary");
            boolean summaryOutput = false;

            if (summaryRequest != null) {
                if (summaryRequest.equals("true")) {
                    summaryOutput = true;
                }
            }

            if (outputFile33 != null) {
                // loop over all households in the hh table to count number of output records
                try {
                  outStream = new PrintWriter(new BufferedWriter(new FileWriter(file)));

                    logger.info("Writing output file to: " +
                                     outputFile33);

                    //write M33.csv heading
                    writeRecord(outStream, tableHeadings33);

                    // loop over all households in the hh table
                    for (int i = 1; i < hh.length; i++) {
                      hh_id = hh[i].getID();
                      hh_taz_id = hh[i].getTazID();
                      hh_size = hh[i].getHHSize();

                      JointTour[] jt = hh[i].getJointTours();
                      Person[] persons = hh[i].getPersonArray();

                      for (int p = 1; p < persons.length; p++) {
                        if (jt != null) {
                          for (int j = 0; j < jt.length; j++) {
                            tempRecord[0] = hh_id;
                            tempRecord[1] = hh_taz_id;
                            tempRecord[2] = persons[p].getID();
                            tempRecord[3] = j + 1;
                            tempRecord[4] = persons[p].getPersonType();
                            tempRecord[5] = persons[p].getPatternType();
                            tempRecord[6] = (jt[j].getTourType());
                            tempRecord[7] = (jt[j].getTourCompositionCode());
                            tempRecord[8] = (jt[j].getPersonParticipation(p) ? 1 : 2);
                            tempRecord[9] = (persons[p].getAvailableWindow());
                            tempRecord[10] = hh_size;
                            if (jt[j].getPersonParticipation(p))
                                choiceFreqs[1]++;
                            else
                                choiceFreqs[2]++;

                            //write current record to M33.csv
                            writeRecord(outStream, tempRecord);
                          }
                        }
//                        else {
//                          tempRecord[0] = hh_id;
//                          tempRecord[1] = hh_taz_id;
//                          tempRecord[2] = persons[p].getID();
//                          tempRecord[3] = 0;
//                          tempRecord[4] = persons[p].getPersonType();
//                          tempRecord[5] = persons[p].getPatternType();
//                          tempRecord[6] = 0;
//                          tempRecord[7] = 0;
//                          tempRecord[8] = 0;
//                          tempRecord[9] = (persons[p].getAvailableWindow());
//                          choiceFreqs[0]++;
//
//                          //write current record to M33.csv
//                          writeRecord(outStream, tempRecord);
//                        }
                      }
                    }
                  outStream.close();
                }catch(IOException e){
                  logger.error("IO exception when writing output file");
                }
            }

            if (summaryOutput) {
                writeFreqSummaryToLogger ( "Joint Tour Participation", "JT_Partic", choiceFreqs );
            }


            logger.info("End of Joint Tour Participation");
        }

        private void writeRecord(PrintWriter outStream, String[] record) {
            for (int i = 0; i < record.length; i++) {
                if (i != 0) {
                    outStream.print(",");
                }

                outStream.print(record[i]);
            }

            outStream.println();
        }

        private void writeRecord(PrintWriter outStream, float[] record) {
            for (int i = 0; i < record.length; i++) {
                if (i != 0) {
                    outStream.print(",");
                }

                outStream.print(record[i]);
            }

            outStream.println();
        }


        private void writeFreqSummaryToLogger ( String tableTitle, String fieldName, int[] freqs ) {

            // print a simple summary table
            logger.info( "Frequency Report table: " + tableTitle );
            logger.info( "Frequency for field " + fieldName );
            logger.info(String.format("%8s", "Value") + "  " + String.format("%-20s", "Description") + "  " + String.format("%11s", "Frequency"));

            int total = 0;
            for (int i = 0; i < freqs.length; i++) {
                if (freqs[i] > 0) {
                    String description = OutputDescription.getDescription(fieldName, i);
                    logger.info( String.format("%8d", i) + "  " + String.format("%-20s", description) + "  " + String.format("%11d", freqs[i] ) );
                    total += freqs[i];
                }
            }

            logger.info(String.format("%8s", "Total") + String.format("%35d\n\n\n", total));
        }
    }

