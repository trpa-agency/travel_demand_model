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
package com.pb.tahoe.individual_tour;

import com.pb.common.calculator.IndexValues;
import com.pb.common.calculator.UtilityExpressionCalculator;
import com.pb.common.model.ConcreteAlternative;
import com.pb.common.model.LogitModel;
import com.pb.common.util.ResourceUtil;
import com.pb.tahoe.structures.Household;
import com.pb.tahoe.structures.PatternType;
import com.pb.tahoe.structures.Person;
import com.pb.tahoe.structures.PersonType;
import com.pb.tahoe.structures.SubTourType;
import com.pb.tahoe.structures.Tour;
import com.pb.tahoe.structures.TourType;
import com.pb.tahoe.util.DataWriter;
import com.pb.tahoe.util.OutputDescription;
import org.apache.log4j.Logger;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.ResourceBundle;


/**
 * IndividualNonMandatoryToursModel is a class that ...
 *
 * @author Christi Willison
 * @version 1.0,  Sep 28, 2006
 */
public class IndividualNonMandatoryToursModel {
    static final int INDIV_DATA_SHEET = 0;
    static final int MAIN_FREQ_MODEL_SHEET = 1;
    static final int MAIN_ALLOC_MODEL_SHEET = 2;
    static final int DISC_WORK_FREQ_MODEL_SHEET = 3;
    static final int DISC_NOWORK_FREQ_MODEL_SHEET = 4;
    static final int DISC_CHILD_FREQ_MODEL_SHEET = 5;
    static final int ATWORK_FREQ_MODEL_SHEET = 6;

    static Logger logger = Logger.getLogger(IndividualNonMandatoryToursModel.class);

    private Household[] hh;

    private IndexValues index = new IndexValues();

    ResourceBundle rb;





    public IndividualNonMandatoryToursModel ( ResourceBundle propertyMap, Household[] hh ) {

        this.rb = propertyMap;
        this.hh = hh;

        index.setOriginZone( 0 );
        index.setDestZone( 0 );

    }



    public void runMaintenanceFrequency() {

        PrintWriter outStream = null;


        logger.info ("Starting Individual Maintenance Tour Frequency");

        //open files named in tahoe.proerties and run fully joint tour frequency model
        String controlFile = rb.getString( "indiv.non.mandatory.control.file" );
        String outputFile = rb.getString( "indiv.non.mandatory.maintenance.frequency.output.file" );
        String summaryRequest = rb.getString( "indiv.non.mandatory.summary.output" );


        boolean summaryOutput = false;
        if (summaryRequest != null)
            if (summaryRequest.equals("true"))
                summaryOutput = true;

        ArrayList<String> tableHeadings = new ArrayList<String>();
        int[] choiceFreqs;
        int[] utilityAvailability;
        try {

            if (outputFile != null) {

                // open output stream for output file
                outStream = new PrintWriter (new BufferedWriter( new FileWriter(outputFile) ) );

                // define file names for .csv file being written
                tableHeadings.add(DataWriter.HHID_FIELD);
                tableHeadings.add(DataWriter.HHTAZID_FIELD);
                tableHeadings.add("indiv_main_freq");

                //Print field names to .csv file header
                outStream.print( (String)tableHeadings.get(0) );
                for (int i = 1; i < tableHeadings.size(); i++) {
                    outStream.print(",");
                    outStream.print( (String)tableHeadings.get(i) );
                }
                outStream.println();
            }

            // create a new UEC to get utilties for this logit model
            UtilityExpressionCalculator uec = new UtilityExpressionCalculator(new File(controlFile), MAIN_FREQ_MODEL_SHEET, INDIV_DATA_SHEET, rb, Household.class);
            int numberOfAlternatives = uec.getNumberOfAlternatives();
            String[] alternativeNames = uec.getAlternativeNames();

            // create and define a new LogitModel object
            LogitModel root= new LogitModel("root", numberOfAlternatives);
            ConcreteAlternative[] alts= new ConcreteAlternative[numberOfAlternatives];


            for(int i=0;i<numberOfAlternatives;i++){
                logger.debug("alternative "+(i+1)+" is "+alternativeNames[i] );
                alts[i]  = new ConcreteAlternative(alternativeNames[i], (i+1));
                root.addAlternative (alts[i]);
                logger.debug(alternativeNames[i]+" has been added to the root");
            }

            // set availabilities
            root.computeAvailabilities();
            root.writeAvailabilities();


            utilityAvailability = new int[numberOfAlternatives+1];
            Arrays.fill (utilityAvailability, 1);

            choiceFreqs = new int[numberOfAlternatives+1];

            int hh_id;
            int hh_taz_id;
            float[] tableData;
            // loop over all households in the hh table
            for (int i=1; i < hh.length; i++) {
                tableData = new float[tableHeadings.size()];

                hh_id = hh[i].getID();
                hh_taz_id = hh[i].getTazID();
                int alt = 0;

                int hhType = hh[i].getHHType();

                // apply individual maintenance tour frequency model only to households with at least 1 travel active person
                alt = 0;
                if ( hhType > 0 ) {

                    // get utilities for each alternative for this household
                    index.setZoneIndex(hh_taz_id);
                    index.setHHIndex(hh_id);
                    double[] utilities = uec.solve( index, hh[i], utilityAvailability);

                    //set utility for each alternative
                    for(int a=0;a < numberOfAlternatives;a++){
                        alts[a].setAvailability( utilityAvailability[a+1] == 1 );
                        if (utilityAvailability[a+1] == 1)
                            alts[a].setAvailability( (utilities[a] > -99.0) );
                        alts[a].setUtility(utilities[a]);
                    }
                    // set availabilities
                    root.computeAvailabilities();


                    root.getUtility();
                    root.calculateProbabilities();


                    ConcreteAlternative chosen = (ConcreteAlternative) root.chooseElementalAlternative();
                    String chosenAltName= chosen.getName();

                    // save chosen alternative in  householdChoice Array
                    for(int a=0; a < numberOfAlternatives; a++){
                        if (chosenAltName.equals(alternativeNames[a])) {
                            alt = a+1;
                            break;
                        }
                    }


                    switch (alt) {
                    case 1:                //no tour
                        hh[i].indivTours = null;
                        break;
                    case 2:               // one maintenance - other
                        hh[i].indivTours = new Tour[1];
                        hh[i].indivTours[0] = new Tour(hh[i].getHHSize());
                        hh[i].indivTours[0].setTourType (TourType.OTHER_MAINTENANCE);
                        hh[i].indivTours[0].setTourOrder (0);
                        hh[i].incrementIndivToursByType (TourType.OTHER_MAINTENANCE);
                        break;
                    case 3:         //2 maintenance-other
                        hh[i].indivTours = new Tour[2];
                        for (int k=0; k < 2; k++)
                            hh[i].indivTours[k] = new Tour(hh[i].getHHSize());
                        hh[i].indivTours[0].setTourType (TourType.OTHER_MAINTENANCE);
                        hh[i].indivTours[0].setTourOrder (1);
                        hh[i].indivTours[1].setTourType (TourType.OTHER_MAINTENANCE);
                        hh[i].indivTours[1].setTourOrder (2);
                        hh[i].incrementIndivToursByType (TourType.OTHER_MAINTENANCE);
                        hh[i].incrementIndivToursByType (TourType.OTHER_MAINTENANCE);
                        break;
                    case 4:      //3 maintenance-other
                        hh[i].indivTours = new Tour[3];
                        for (int k=0; k < 3; k++)
                            hh[i].indivTours[k] = new Tour(hh[i].getHHSize());
                        hh[i].indivTours[0].setTourType (TourType.OTHER_MAINTENANCE);
                        hh[i].indivTours[0].setTourOrder (1);
                        hh[i].indivTours[1].setTourType (TourType.OTHER_MAINTENANCE);
                        hh[i].indivTours[1].setTourOrder (2);
                        hh[i].indivTours[2].setTourType (TourType.OTHER_MAINTENANCE);
                        hh[i].indivTours[2].setTourOrder (3);
                        hh[i].incrementIndivToursByType (TourType.OTHER_MAINTENANCE);
                        hh[i].incrementIndivToursByType (TourType.OTHER_MAINTENANCE);
                        hh[i].incrementIndivToursByType (TourType.OTHER_MAINTENANCE);
                        break;
                    case 5:      //1 escort
                        hh[i].indivTours = new Tour[1];
                        hh[i].indivTours[0] = new Tour(hh[i].getHHSize());
                        hh[i].indivTours[0].setTourType (TourType.ESCORTING);
                        hh[i].indivTours[0].setTourOrder (0);
                        hh[i].incrementIndivToursByType (TourType.ESCORTING);
                        break;
                    case 6:       // 1 escort, 1 maintenance-other
                        hh[i].indivTours = new Tour[2];
                        for (int k=0; k < 2; k++)
                            hh[i].indivTours[k] = new Tour(hh[i].getHHSize());
                        hh[i].indivTours[0].setTourType (TourType.ESCORTING);
                        hh[i].indivTours[0].setTourOrder (0);
                        hh[i].indivTours[1].setTourType (TourType.OTHER_MAINTENANCE);
                        hh[i].indivTours[1].setTourOrder (0);
                        hh[i].incrementIndivToursByType (TourType.ESCORTING);
                        hh[i].incrementIndivToursByType (TourType.OTHER_MAINTENANCE);
                        break;
                    case 7:
                        hh[i].indivTours = new Tour[3];
                        for (int k=0; k < 3; k++)
                            hh[i].indivTours[k] = new Tour(hh[i].getHHSize());
                        hh[i].indivTours[0].setTourType (TourType.ESCORTING);
                        hh[i].indivTours[0].setTourOrder (0);
                        hh[i].indivTours[1].setTourType (TourType.OTHER_MAINTENANCE);
                        hh[i].indivTours[1].setTourOrder (1);
                        hh[i].indivTours[2].setTourType (TourType.OTHER_MAINTENANCE);
                        hh[i].indivTours[2].setTourOrder (2);
                        hh[i].incrementIndivToursByType (TourType.ESCORTING);
                        hh[i].incrementIndivToursByType (TourType.OTHER_MAINTENANCE);
                        hh[i].incrementIndivToursByType (TourType.OTHER_MAINTENANCE);
                        break;
                    case 8:
                        hh[i].indivTours = new Tour[4];
                        for (int k=0; k < 4; k++)
                            hh[i].indivTours[k] = new Tour(hh[i].getHHSize());
                        hh[i].indivTours[0].setTourType (TourType.ESCORTING);
                        hh[i].indivTours[0].setTourOrder (0);
                        hh[i].indivTours[1].setTourType (TourType.OTHER_MAINTENANCE);
                        hh[i].indivTours[1].setTourOrder (1);
                        hh[i].indivTours[2].setTourType (TourType.OTHER_MAINTENANCE);
                        hh[i].indivTours[2].setTourOrder (2);
                        hh[i].indivTours[3].setTourType (TourType.OTHER_MAINTENANCE);
                        hh[i].indivTours[3].setTourOrder (3);
                        hh[i].incrementIndivToursByType (TourType.ESCORTING);
                        hh[i].incrementIndivToursByType (TourType.OTHER_MAINTENANCE);
                        hh[i].incrementIndivToursByType (TourType.OTHER_MAINTENANCE);
                        hh[i].incrementIndivToursByType (TourType.OTHER_MAINTENANCE);
                        break;
                    case 9:
                        hh[i].indivTours = new Tour[2];
                        for (int k=0; k < 2; k++)
                            hh[i].indivTours[k] = new Tour(hh[i].getHHSize());
                        hh[i].indivTours[0].setTourType (TourType.ESCORTING);
                        hh[i].indivTours[0].setTourOrder (1);
                        hh[i].indivTours[1].setTourType (TourType.ESCORTING);
                        hh[i].indivTours[1].setTourOrder (2);
                        hh[i].incrementIndivToursByType (TourType.ESCORTING);
                        hh[i].incrementIndivToursByType (TourType.ESCORTING);
                        break;
                    case 10:
                        hh[i].indivTours = new Tour[3];
                        for (int k=0; k < 3; k++)
                            hh[i].indivTours[k] = new Tour(hh[i].getHHSize());
                        hh[i].indivTours[0].setTourType (TourType.ESCORTING);
                        hh[i].indivTours[0].setTourOrder (1);
                        hh[i].indivTours[1].setTourType (TourType.ESCORTING);
                        hh[i].indivTours[1].setTourOrder (2);
                        hh[i].indivTours[2].setTourType (TourType.OTHER_MAINTENANCE);
                        hh[i].indivTours[2].setTourOrder (0);
                        hh[i].incrementIndivToursByType (TourType.ESCORTING);
                        hh[i].incrementIndivToursByType (TourType.ESCORTING);
                        hh[i].incrementIndivToursByType (TourType.OTHER_MAINTENANCE);
                        break;
                    case 11:
                        hh[i].indivTours = new Tour[4];
                        for (int k=0; k < 4; k++)
                            hh[i].indivTours[k] = new Tour(hh[i].getHHSize());
                        hh[i].indivTours[0].setTourType (TourType.ESCORTING);
                        hh[i].indivTours[0].setTourOrder (1);
                        hh[i].indivTours[1].setTourType (TourType.ESCORTING);
                        hh[i].indivTours[1].setTourOrder (2);
                        hh[i].indivTours[2].setTourType (TourType.OTHER_MAINTENANCE);
                        hh[i].indivTours[2].setTourOrder (1);
                        hh[i].indivTours[3].setTourType (TourType.OTHER_MAINTENANCE);
                        hh[i].indivTours[3].setTourOrder (2);
                        hh[i].incrementIndivToursByType (TourType.ESCORTING);
                        hh[i].incrementIndivToursByType (TourType.ESCORTING);
                        hh[i].incrementIndivToursByType (TourType.OTHER_MAINTENANCE);
                        hh[i].incrementIndivToursByType (TourType.OTHER_MAINTENANCE);
                        break;
                    case 12:
                        hh[i].indivTours = new Tour[5];
                        for (int k=0; k < 5; k++)
                            hh[i].indivTours[k] = new Tour(hh[i].getHHSize());
                        hh[i].indivTours[0].setTourType (TourType.ESCORTING);
                        hh[i].indivTours[0].setTourOrder (1);
                        hh[i].indivTours[1].setTourType (TourType.ESCORTING);
                        hh[i].indivTours[1].setTourOrder (2);
                        hh[i].indivTours[2].setTourType (TourType.OTHER_MAINTENANCE);
                        hh[i].indivTours[2].setTourOrder (1);
                        hh[i].indivTours[3].setTourType (TourType.OTHER_MAINTENANCE);
                        hh[i].indivTours[3].setTourOrder (2);
                        hh[i].indivTours[4].setTourType (TourType.OTHER_MAINTENANCE);
                        hh[i].indivTours[4].setTourOrder (3);
                        hh[i].incrementIndivToursByType (TourType.ESCORTING);
                        hh[i].incrementIndivToursByType (TourType.ESCORTING);
                        hh[i].incrementIndivToursByType (TourType.OTHER_MAINTENANCE);
                        hh[i].incrementIndivToursByType (TourType.OTHER_MAINTENANCE);
                        hh[i].incrementIndivToursByType (TourType.OTHER_MAINTENANCE);
                        break;
                    case 13:
                        hh[i].indivTours = new Tour[1];
                        hh[i].indivTours[0] = new Tour(hh[i].getHHSize());
                        hh[i].indivTours[0].setTourType (TourType.SHOP);
                        hh[i].indivTours[0].setTourOrder (0);
                        hh[i].incrementIndivToursByType (TourType.SHOP);
                        break;
                    case 14:
                        hh[i].indivTours = new Tour[2];
                        for (int k=0; k < 2; k++)
                            hh[i].indivTours[k] = new Tour(hh[i].getHHSize());
                        hh[i].indivTours[0].setTourType (TourType.SHOP);
                        hh[i].indivTours[0].setTourOrder (0);
                        hh[i].indivTours[1].setTourType (TourType.OTHER_MAINTENANCE);
                        hh[i].indivTours[1].setTourOrder (0);
                        hh[i].incrementIndivToursByType (TourType.SHOP);
                        hh[i].incrementIndivToursByType (TourType.OTHER_MAINTENANCE);
                        break;
                    case 15:
                        hh[i].indivTours = new Tour[3];
                        for (int k=0; k < 3; k++)
                            hh[i].indivTours[k] = new Tour(hh[i].getHHSize());
                        hh[i].indivTours[0].setTourType (TourType.SHOP);
                        hh[i].indivTours[0].setTourOrder (0);
                        hh[i].indivTours[1].setTourType (TourType.OTHER_MAINTENANCE);
                        hh[i].indivTours[1].setTourOrder (1);
                        hh[i].indivTours[2].setTourType (TourType.OTHER_MAINTENANCE);
                        hh[i].indivTours[2].setTourOrder (2);
                        hh[i].incrementIndivToursByType (TourType.SHOP);
                        hh[i].incrementIndivToursByType (TourType.OTHER_MAINTENANCE);
                        hh[i].incrementIndivToursByType (TourType.OTHER_MAINTENANCE);
                        break;
                    case 16:
                        hh[i].indivTours = new Tour[4];
                        for (int k=0; k < 4; k++)
                            hh[i].indivTours[k] = new Tour(hh[i].getHHSize());
                        hh[i].indivTours[0].setTourType (TourType.SHOP);
                        hh[i].indivTours[0].setTourOrder (0);
                        hh[i].indivTours[1].setTourType (TourType.OTHER_MAINTENANCE);
                        hh[i].indivTours[1].setTourOrder (1);
                        hh[i].indivTours[2].setTourType (TourType.OTHER_MAINTENANCE);
                        hh[i].indivTours[2].setTourOrder (2);
                        hh[i].indivTours[3].setTourType (TourType.OTHER_MAINTENANCE);
                        hh[i].indivTours[3].setTourOrder (3);
                        hh[i].incrementIndivToursByType (TourType.SHOP);
                        hh[i].incrementIndivToursByType (TourType.OTHER_MAINTENANCE);
                        hh[i].incrementIndivToursByType (TourType.OTHER_MAINTENANCE);
                        hh[i].incrementIndivToursByType (TourType.OTHER_MAINTENANCE);
                        break;
                    case 17:
                        hh[i].indivTours = new Tour[2];
                        for (int k=0; k < 2; k++)
                            hh[i].indivTours[k] = new Tour(hh[i].getHHSize());
                        hh[i].indivTours[0].setTourType (TourType.ESCORTING);
                        hh[i].indivTours[0].setTourOrder (0);
                        hh[i].indivTours[1].setTourType (TourType.SHOP);
                        hh[i].indivTours[1].setTourOrder (0);
                        hh[i].incrementIndivToursByType (TourType.ESCORTING);
                        hh[i].incrementIndivToursByType (TourType.SHOP);
                        break;
                    case 18:
                        hh[i].indivTours = new Tour[3];
                        for (int k=0; k < 3; k++)
                            hh[i].indivTours[k] = new Tour(hh[i].getHHSize());
                        hh[i].indivTours[0].setTourType (TourType.ESCORTING);
                        hh[i].indivTours[0].setTourOrder (0);
                        hh[i].indivTours[1].setTourType (TourType.SHOP);
                        hh[i].indivTours[1].setTourOrder (0);
                        hh[i].indivTours[2].setTourType (TourType.OTHER_MAINTENANCE);
                        hh[i].indivTours[2].setTourOrder (0);
                        hh[i].incrementIndivToursByType (TourType.ESCORTING);
                        hh[i].incrementIndivToursByType (TourType.SHOP);
                        hh[i].incrementIndivToursByType (TourType.OTHER_MAINTENANCE);
                        break;
                    case 19:
                        hh[i].indivTours = new Tour[4];
                        for (int k=0; k < 4; k++)
                            hh[i].indivTours[k] = new Tour(hh[i].getHHSize());
                        hh[i].indivTours[0].setTourType (TourType.ESCORTING);
                        hh[i].indivTours[0].setTourOrder (0);
                        hh[i].indivTours[1].setTourType (TourType.SHOP);
                        hh[i].indivTours[1].setTourOrder (0);
                        hh[i].indivTours[2].setTourType (TourType.OTHER_MAINTENANCE);
                        hh[i].indivTours[2].setTourOrder (1);
                        hh[i].indivTours[3].setTourType (TourType.OTHER_MAINTENANCE);
                        hh[i].indivTours[3].setTourOrder (2);
                        hh[i].incrementIndivToursByType (TourType.ESCORTING);
                        hh[i].incrementIndivToursByType (TourType.SHOP);
                        hh[i].incrementIndivToursByType (TourType.OTHER_MAINTENANCE);
                        hh[i].incrementIndivToursByType (TourType.OTHER_MAINTENANCE);
                        break;
                    case 20:
                        hh[i].indivTours = new Tour[5];
                        for (int k=0; k < 5; k++)
                            hh[i].indivTours[k] = new Tour(hh[i].getHHSize());
                        hh[i].indivTours[0].setTourType (TourType.ESCORTING);
                        hh[i].indivTours[0].setTourOrder (0);
                        hh[i].indivTours[1].setTourType (TourType.SHOP);
                        hh[i].indivTours[1].setTourOrder (0);
                        hh[i].indivTours[2].setTourType (TourType.OTHER_MAINTENANCE);
                        hh[i].indivTours[2].setTourOrder (1);
                        hh[i].indivTours[3].setTourType (TourType.OTHER_MAINTENANCE);
                        hh[i].indivTours[3].setTourOrder (2);
                        hh[i].indivTours[4].setTourType (TourType.OTHER_MAINTENANCE);
                        hh[i].indivTours[4].setTourOrder (3);
                        hh[i].incrementIndivToursByType (TourType.ESCORTING);
                        hh[i].incrementIndivToursByType (TourType.SHOP);
                        hh[i].incrementIndivToursByType (TourType.OTHER_MAINTENANCE);
                        hh[i].incrementIndivToursByType (TourType.OTHER_MAINTENANCE);
                        hh[i].incrementIndivToursByType (TourType.OTHER_MAINTENANCE);
                        break;
                    case 21:
                        hh[i].indivTours = new Tour[3];
                        for (int k=0; k < 3; k++)
                            hh[i].indivTours[k] = new Tour(hh[i].getHHSize());
                        hh[i].indivTours[0].setTourType (TourType.ESCORTING);
                        hh[i].indivTours[0].setTourOrder (1);
                        hh[i].indivTours[1].setTourType (TourType.ESCORTING);
                        hh[i].indivTours[1].setTourOrder (2);
                        hh[i].indivTours[2].setTourType (TourType.SHOP);
                        hh[i].indivTours[2].setTourOrder (0);
                        hh[i].incrementIndivToursByType (TourType.ESCORTING);
                        hh[i].incrementIndivToursByType (TourType.ESCORTING);
                        hh[i].incrementIndivToursByType (TourType.SHOP);
                        break;
                    case 22:
                        hh[i].indivTours = new Tour[4];
                        for (int k=0; k < 4; k++)
                            hh[i].indivTours[k] = new Tour(hh[i].getHHSize());
                        hh[i].indivTours[0].setTourType (TourType.ESCORTING);
                        hh[i].indivTours[0].setTourOrder (1);
                        hh[i].indivTours[1].setTourType (TourType.ESCORTING);
                        hh[i].indivTours[1].setTourOrder (2);
                        hh[i].indivTours[2].setTourType (TourType.SHOP);
                        hh[i].indivTours[2].setTourOrder (0);
                        hh[i].indivTours[3].setTourType (TourType.OTHER_MAINTENANCE);
                        hh[i].indivTours[3].setTourOrder (0);
                        hh[i].incrementIndivToursByType (TourType.ESCORTING);
                        hh[i].incrementIndivToursByType (TourType.ESCORTING);
                        hh[i].incrementIndivToursByType (TourType.SHOP);
                        hh[i].incrementIndivToursByType (TourType.OTHER_MAINTENANCE);
                        break;
                    case 23:
                        hh[i].indivTours = new Tour[5];
                        for (int k=0; k < 5; k++)
                            hh[i].indivTours[k] = new Tour(hh[i].getHHSize());
                        hh[i].indivTours[0].setTourType (TourType.ESCORTING);
                        hh[i].indivTours[0].setTourOrder (1);
                        hh[i].indivTours[1].setTourType (TourType.ESCORTING);
                        hh[i].indivTours[1].setTourOrder (2);
                        hh[i].indivTours[2].setTourType (TourType.SHOP);
                        hh[i].indivTours[2].setTourOrder (0);
                        hh[i].indivTours[3].setTourType (TourType.OTHER_MAINTENANCE);
                        hh[i].indivTours[3].setTourOrder (1);
                        hh[i].indivTours[4].setTourType (TourType.OTHER_MAINTENANCE);
                        hh[i].indivTours[4].setTourOrder (2);
                        hh[i].incrementIndivToursByType (TourType.ESCORTING);
                        hh[i].incrementIndivToursByType (TourType.ESCORTING);
                        hh[i].incrementIndivToursByType (TourType.SHOP);
                        hh[i].incrementIndivToursByType (TourType.OTHER_MAINTENANCE);
                        hh[i].incrementIndivToursByType (TourType.OTHER_MAINTENANCE);
                        break;
                    case 24:
                        hh[i].indivTours = new Tour[6];
                        for (int k=0; k < 6; k++)
                            hh[i].indivTours[k] = new Tour(hh[i].getHHSize());
                        hh[i].indivTours[0].setTourType (TourType.ESCORTING);
                        hh[i].indivTours[0].setTourOrder (1);
                        hh[i].indivTours[1].setTourType (TourType.ESCORTING);
                        hh[i].indivTours[1].setTourOrder (2);
                        hh[i].indivTours[2].setTourType (TourType.SHOP);
                        hh[i].indivTours[2].setTourOrder (0);
                        hh[i].indivTours[3].setTourType (TourType.OTHER_MAINTENANCE);
                        hh[i].indivTours[3].setTourOrder (1);
                        hh[i].indivTours[4].setTourType (TourType.OTHER_MAINTENANCE);
                        hh[i].indivTours[4].setTourOrder (2);
                        hh[i].indivTours[5].setTourType (TourType.OTHER_MAINTENANCE);
                        hh[i].indivTours[5].setTourOrder (3);
                        hh[i].incrementIndivToursByType (TourType.ESCORTING);
                        hh[i].incrementIndivToursByType (TourType.ESCORTING);
                        hh[i].incrementIndivToursByType (TourType.SHOP);
                        hh[i].incrementIndivToursByType (TourType.OTHER_MAINTENANCE);
                        hh[i].incrementIndivToursByType (TourType.OTHER_MAINTENANCE);
                        hh[i].incrementIndivToursByType (TourType.OTHER_MAINTENANCE);
                        break;
                    case 25:
                        hh[i].indivTours = new Tour[2];
                        for (int k=0; k < 2; k++)
                            hh[i].indivTours[k] = new Tour(hh[i].getHHSize());
                        hh[i].indivTours[0].setTourType (TourType.SHOP);
                        hh[i].indivTours[0].setTourOrder (1);
                        hh[i].indivTours[1].setTourType (TourType.SHOP);
                        hh[i].indivTours[1].setTourOrder (2);
                        hh[i].incrementIndivToursByType (TourType.SHOP);
                        hh[i].incrementIndivToursByType (TourType.SHOP);
                        break;
                    case 26:
                        hh[i].indivTours = new Tour[3];
                        for (int k=0; k < 3; k++)
                            hh[i].indivTours[k] = new Tour(hh[i].getHHSize());
                        hh[i].indivTours[0].setTourType (TourType.SHOP);
                        hh[i].indivTours[0].setTourOrder (1);
                        hh[i].indivTours[1].setTourType (TourType.SHOP);
                        hh[i].indivTours[1].setTourOrder (2);
                        hh[i].indivTours[2].setTourType (TourType.OTHER_MAINTENANCE);
                        hh[i].indivTours[2].setTourOrder (0);
                        hh[i].incrementIndivToursByType (TourType.SHOP);
                        hh[i].incrementIndivToursByType (TourType.SHOP);
                        hh[i].incrementIndivToursByType (TourType.OTHER_MAINTENANCE);
                        break;
                    case 27:
                        hh[i].indivTours = new Tour[4];
                        for (int k=0; k < 4; k++)
                            hh[i].indivTours[k] = new Tour(hh[i].getHHSize());
                        hh[i].indivTours[0].setTourType (TourType.SHOP);
                        hh[i].indivTours[0].setTourOrder (1);
                        hh[i].indivTours[1].setTourType (TourType.SHOP);
                        hh[i].indivTours[1].setTourOrder (2);
                        hh[i].indivTours[2].setTourType (TourType.OTHER_MAINTENANCE);
                        hh[i].indivTours[2].setTourOrder (1);
                        hh[i].indivTours[3].setTourType (TourType.OTHER_MAINTENANCE);
                        hh[i].indivTours[3].setTourOrder (2);
                        hh[i].incrementIndivToursByType (TourType.SHOP);
                        hh[i].incrementIndivToursByType (TourType.SHOP);
                        hh[i].incrementIndivToursByType (TourType.OTHER_MAINTENANCE);
                        hh[i].incrementIndivToursByType (TourType.OTHER_MAINTENANCE);
                        break;
                    case 28:
                        hh[i].indivTours = new Tour[5];
                        for (int k=0; k < 5; k++)
                            hh[i].indivTours[k] = new Tour(hh[i].getHHSize());
                        hh[i].indivTours[0].setTourType (TourType.SHOP);
                        hh[i].indivTours[0].setTourOrder (1);
                        hh[i].indivTours[1].setTourType (TourType.SHOP);
                        hh[i].indivTours[1].setTourOrder (2);
                        hh[i].indivTours[2].setTourType (TourType.OTHER_MAINTENANCE);
                        hh[i].indivTours[2].setTourOrder (1);
                        hh[i].indivTours[3].setTourType (TourType.OTHER_MAINTENANCE);
                        hh[i].indivTours[3].setTourOrder (2);
                        hh[i].indivTours[4].setTourType (TourType.OTHER_MAINTENANCE);
                        hh[i].indivTours[4].setTourOrder (3);
                        hh[i].incrementIndivToursByType (TourType.SHOP);
                        hh[i].incrementIndivToursByType (TourType.SHOP);
                        hh[i].incrementIndivToursByType (TourType.OTHER_MAINTENANCE);
                        hh[i].incrementIndivToursByType (TourType.OTHER_MAINTENANCE);
                        hh[i].incrementIndivToursByType (TourType.OTHER_MAINTENANCE);
                        break;
                    case 29:
                        hh[i].indivTours = new Tour[3];
                        for (int k=0; k < 3; k++)
                            hh[i].indivTours[k] = new Tour(hh[i].getHHSize());
                        hh[i].indivTours[0].setTourType (TourType.ESCORTING);
                        hh[i].indivTours[0].setTourOrder (0);
                        hh[i].indivTours[1].setTourType (TourType.SHOP);
                        hh[i].indivTours[1].setTourOrder (1);
                        hh[i].indivTours[2].setTourType (TourType.SHOP);
                        hh[i].indivTours[2].setTourOrder (2);
                        hh[i].incrementIndivToursByType (TourType.ESCORTING);
                        hh[i].incrementIndivToursByType (TourType.SHOP);
                        hh[i].incrementIndivToursByType (TourType.SHOP);
                        break;
                    case 30:
                        hh[i].indivTours = new Tour[4];
                        for (int k=0; k < 4; k++)
                            hh[i].indivTours[k] = new Tour(hh[i].getHHSize());
                        hh[i].indivTours[0].setTourType (TourType.ESCORTING);
                        hh[i].indivTours[0].setTourOrder (0);
                        hh[i].indivTours[1].setTourType (TourType.SHOP);
                        hh[i].indivTours[1].setTourOrder (1);
                        hh[i].indivTours[2].setTourType (TourType.SHOP);
                        hh[i].indivTours[2].setTourOrder (2);
                        hh[i].indivTours[3].setTourType (TourType.OTHER_MAINTENANCE);
                        hh[i].indivTours[3].setTourOrder (0);
                        hh[i].incrementIndivToursByType (TourType.ESCORTING);
                        hh[i].incrementIndivToursByType (TourType.SHOP);
                        hh[i].incrementIndivToursByType (TourType.SHOP);
                        hh[i].incrementIndivToursByType (TourType.OTHER_MAINTENANCE);
                        break;
                    case 31:
                        hh[i].indivTours = new Tour[5];
                        for (int k=0; k < 5; k++)
                            hh[i].indivTours[k] = new Tour(hh[i].getHHSize());
                        hh[i].indivTours[0].setTourType (TourType.ESCORTING);
                        hh[i].indivTours[0].setTourOrder (0);
                        hh[i].indivTours[1].setTourType (TourType.SHOP);
                        hh[i].indivTours[1].setTourOrder (1);
                        hh[i].indivTours[2].setTourType (TourType.SHOP);
                        hh[i].indivTours[2].setTourOrder (2);
                        hh[i].indivTours[3].setTourType (TourType.OTHER_MAINTENANCE);
                        hh[i].indivTours[3].setTourOrder (1);
                        hh[i].indivTours[4].setTourType (TourType.OTHER_MAINTENANCE);
                        hh[i].indivTours[4].setTourOrder (2);
                        hh[i].incrementIndivToursByType (TourType.ESCORTING);
                        hh[i].incrementIndivToursByType (TourType.SHOP);
                        hh[i].incrementIndivToursByType (TourType.SHOP);
                        hh[i].incrementIndivToursByType (TourType.OTHER_MAINTENANCE);
                        hh[i].incrementIndivToursByType (TourType.OTHER_MAINTENANCE);
                        break;
                    case 32:
                        hh[i].indivTours = new Tour[6];
                        for (int k=0; k < 6; k++)
                            hh[i].indivTours[k] = new Tour(hh[i].getHHSize());
                        hh[i].indivTours[0].setTourType (TourType.ESCORTING);
                        hh[i].indivTours[0].setTourOrder (0);
                        hh[i].indivTours[1].setTourType (TourType.SHOP);
                        hh[i].indivTours[1].setTourOrder (1);
                        hh[i].indivTours[2].setTourType (TourType.SHOP);
                        hh[i].indivTours[2].setTourOrder (2);
                        hh[i].indivTours[3].setTourType (TourType.OTHER_MAINTENANCE);
                        hh[i].indivTours[3].setTourOrder (1);
                        hh[i].indivTours[4].setTourType (TourType.OTHER_MAINTENANCE);
                        hh[i].indivTours[4].setTourOrder (2);
                        hh[i].indivTours[5].setTourType (TourType.OTHER_MAINTENANCE);
                        hh[i].indivTours[5].setTourOrder (3);
                        hh[i].incrementIndivToursByType (TourType.ESCORTING);
                        hh[i].incrementIndivToursByType (TourType.SHOP);
                        hh[i].incrementIndivToursByType (TourType.SHOP);
                        hh[i].incrementIndivToursByType (TourType.OTHER_MAINTENANCE);
                        hh[i].incrementIndivToursByType (TourType.OTHER_MAINTENANCE);
                        hh[i].incrementIndivToursByType (TourType.OTHER_MAINTENANCE);
                        break;
                    case 33:
                        hh[i].indivTours = new Tour[4];
                        for (int k=0; k < 4; k++)
                            hh[i].indivTours[k] = new Tour(hh[i].getHHSize());
                        hh[i].indivTours[0].setTourType (TourType.ESCORTING);
                        hh[i].indivTours[0].setTourOrder (1);
                        hh[i].indivTours[1].setTourType (TourType.ESCORTING);
                        hh[i].indivTours[1].setTourOrder (2);
                        hh[i].indivTours[2].setTourType (TourType.SHOP);
                        hh[i].indivTours[2].setTourOrder (1);
                        hh[i].indivTours[3].setTourType (TourType.SHOP);
                        hh[i].indivTours[3].setTourOrder (2);
                        hh[i].incrementIndivToursByType (TourType.ESCORTING);
                        hh[i].incrementIndivToursByType (TourType.ESCORTING);
                        hh[i].incrementIndivToursByType (TourType.SHOP);
                        hh[i].incrementIndivToursByType (TourType.SHOP);
                        break;
                    case 34:
                        hh[i].indivTours = new Tour[5];
                        for (int k=0; k < 5; k++)
                            hh[i].indivTours[k] = new Tour(hh[i].getHHSize());
                        hh[i].indivTours[0].setTourType (TourType.ESCORTING);
                        hh[i].indivTours[0].setTourOrder (1);
                        hh[i].indivTours[1].setTourType (TourType.ESCORTING);
                        hh[i].indivTours[1].setTourOrder (2);
                        hh[i].indivTours[2].setTourType (TourType.SHOP);
                        hh[i].indivTours[2].setTourOrder (1);
                        hh[i].indivTours[3].setTourType (TourType.SHOP);
                        hh[i].indivTours[3].setTourOrder (2);
                        hh[i].indivTours[4].setTourType (TourType.OTHER_MAINTENANCE);
                        hh[i].indivTours[4].setTourOrder (0);
                        hh[i].incrementIndivToursByType (TourType.ESCORTING);
                        hh[i].incrementIndivToursByType (TourType.ESCORTING);
                        hh[i].incrementIndivToursByType (TourType.SHOP);
                        hh[i].incrementIndivToursByType (TourType.SHOP);
                        hh[i].incrementIndivToursByType (TourType.OTHER_MAINTENANCE);
                        break;
                    case 35:
                        hh[i].indivTours = new Tour[6];
                        for (int k=0; k < 6; k++)
                            hh[i].indivTours[k] = new Tour(hh[i].getHHSize());
                        hh[i].indivTours[0].setTourType (TourType.ESCORTING);
                        hh[i].indivTours[0].setTourOrder (1);
                        hh[i].indivTours[1].setTourType (TourType.ESCORTING);
                        hh[i].indivTours[1].setTourOrder (2);
                        hh[i].indivTours[2].setTourType (TourType.SHOP);
                        hh[i].indivTours[2].setTourOrder (1);
                        hh[i].indivTours[3].setTourType (TourType.SHOP);
                        hh[i].indivTours[3].setTourOrder (2);
                        hh[i].indivTours[4].setTourType (TourType.OTHER_MAINTENANCE);
                        hh[i].indivTours[4].setTourOrder (1);
                        hh[i].indivTours[5].setTourType (TourType.OTHER_MAINTENANCE);
                        hh[i].indivTours[5].setTourOrder (2);
                        hh[i].incrementIndivToursByType (TourType.ESCORTING);
                        hh[i].incrementIndivToursByType (TourType.ESCORTING);
                        hh[i].incrementIndivToursByType (TourType.SHOP);
                        hh[i].incrementIndivToursByType (TourType.SHOP);
                        hh[i].incrementIndivToursByType (TourType.OTHER_MAINTENANCE);
                        hh[i].incrementIndivToursByType (TourType.OTHER_MAINTENANCE);
                        break;
                    case 36:
                        hh[i].indivTours = new Tour[7];
                        for (int k=0; k < 7; k++)
                            hh[i].indivTours[k] = new Tour(hh[i].getHHSize());
                        hh[i].indivTours[0].setTourType (TourType.ESCORTING);
                        hh[i].indivTours[0].setTourOrder (1);
                        hh[i].indivTours[1].setTourType (TourType.ESCORTING);
                        hh[i].indivTours[1].setTourOrder (2);
                        hh[i].indivTours[2].setTourType (TourType.SHOP);
                        hh[i].indivTours[2].setTourOrder (1);
                        hh[i].indivTours[3].setTourType (TourType.SHOP);
                        hh[i].indivTours[3].setTourOrder (2);
                        hh[i].indivTours[4].setTourType (TourType.OTHER_MAINTENANCE);
                        hh[i].indivTours[4].setTourOrder (1);
                        hh[i].indivTours[5].setTourType (TourType.OTHER_MAINTENANCE);
                        hh[i].indivTours[5].setTourOrder (2);
                        hh[i].indivTours[6].setTourType (TourType.OTHER_MAINTENANCE);
                        hh[i].indivTours[6].setTourOrder (3);
                        hh[i].incrementIndivToursByType (TourType.ESCORTING);
                        hh[i].incrementIndivToursByType (TourType.ESCORTING);
                        hh[i].incrementIndivToursByType (TourType.SHOP);
                        hh[i].incrementIndivToursByType (TourType.SHOP);
                        hh[i].incrementIndivToursByType (TourType.OTHER_MAINTENANCE);
                        hh[i].incrementIndivToursByType (TourType.OTHER_MAINTENANCE);
                        hh[i].incrementIndivToursByType (TourType.OTHER_MAINTENANCE);
                        break;
                    }

                }


                if (outputFile != null) {
                    tableData[0] = hh_id;
                    tableData[1] = hh_taz_id;
                    tableData[2] = alt;
                    choiceFreqs[alt]++;

                    // write out .csv file record for this tour
                    outStream.print( tableData[0] );
                    for (int c=1; c < tableHeadings.size(); c++) {
                        outStream.print(",");
                        outStream.print( tableData[c] );
                    }
                    outStream.println();
                }
            }//next household

            if (outputFile != null) {
                logger.info ("finished writing Individual Non-mandatory freq choices output file.");
                outStream.close();
             }
        } catch (IOException e) {

               throw new RuntimeException("Error writing to " + outputFile, e);

        }


        if(summaryOutput){

            writeFreqSummaryToLogger ( "Maintenance Tour Frequency", "indiv_main_freq", choiceFreqs );

        }



    }


    public void runMaintenanceAllocation () {

        int hh_id;
        int hh_taz_id;

        int[] choiceFreqs = null;
        int[] utilityAvailability;
        int[] tourChoice = null;
        float[] tableData = null;
        ArrayList<String> tableHeadings = null;


        PrintWriter outStream = null;


        logger.info ("Starting Individual Maintenance Tour Allocation");

        //open files named in morpc.proerties and run fully joint tour frequency model
        String controlFile = rb.getString( "indiv.non.mandatory.control.file");
        String outputFile = rb.getString( "indiv.non.mandatory.maintenance.allocation.output.file");
        String summaryRequest = rb.getString( "indiv.non.mandatory.summary.output" );


        boolean summaryOutput = false;
        if (summaryRequest != null)
            if (summaryRequest.equals("true"))
                summaryOutput = true;


        try {

            if (outputFile != null) {

                // open output stream for DTM output file
                outStream = new PrintWriter (new BufferedWriter( new FileWriter(outputFile) ) );


                // define filed names for .csv file being written
                tableHeadings = new ArrayList<String>();
                tableHeadings.add(DataWriter.HHID_FIELD);
                tableHeadings.add(DataWriter.HHTAZID_FIELD);
                tableHeadings.add("person_id");
                tableHeadings.add("indiv_tour_id");
                tableHeadings.add("indiv_main_alloc");
                tableHeadings.add("personType");
                tableHeadings.add("patternType");
                tableHeadings.add("tourType");
                tableHeadings.add("participation");


                // temp array for holding field values to be written
                tableData = new float[tableHeadings.size()];


                //Print field names to .csv file header record
                outStream.print( (String)tableHeadings.get(0) );
                for (int i = 1; i < tableHeadings.size(); i++) {
                    outStream.print(",");
                    outStream.print( (String)tableHeadings.get(i) );
                }
                outStream.println();

            }



            // create a new UEC to get utilties for this logit model
            UtilityExpressionCalculator uec = new UtilityExpressionCalculator(new File(controlFile), MAIN_ALLOC_MODEL_SHEET, INDIV_DATA_SHEET, rb, Household.class);
            int numberOfAlternatives = uec.getNumberOfAlternatives();
            String[] alternativeNames = uec.getAlternativeNames();

            // create and define a new LogitModel object
            LogitModel root= new LogitModel("root", numberOfAlternatives);
            ConcreteAlternative[] alts= new ConcreteAlternative[numberOfAlternatives];


            for(int i=0;i<numberOfAlternatives;i++){
                logger.debug("alternative "+(i+1)+" is "+alternativeNames[i] );
                alts[i]  = new ConcreteAlternative(alternativeNames[i], i+1);
                root.addAlternative (alts[i]);
                logger.debug(alternativeNames[i]+" has been added to the root");
            }

            // set availabilities
            root.computeAvailabilities();
            root.writeAvailabilities();


            utilityAvailability = new int[numberOfAlternatives+1];
            Arrays.fill (utilityAvailability, 1);


            choiceFreqs = new int[numberOfAlternatives+1];


            // loop over all households in the hh table
            for (int i=1; i < hh.length; i++) {

                hh_id = hh[i].getID();
                hh_taz_id = hh[i].getTazID();


                // get individual tours array for this household.
                Tour[] it = hh[i].getIndivTours();


                // get next household if no individual non-mandatory tours
                if (it == null){
                    continue;
                }


                // get person array for this household.
                Person[] persons = hh[i].getPersonArray();


                tourChoice= new int[it.length];


                // define an array for each person to keep track of participation in individual tours per person
                for (int p=1; p < persons.length; p++) {
                    persons[p].setIndividualTourParticipationArray(it.length);
                }



                // loop over individual tours array for the hh
                for (int j=0; j < it.length; j++) {

                    // this model only applicable for maintenance tours
                    int tourType = it[j].getTourType();
                    if (tourType != TourType.ESCORTING && tourType != TourType.SHOP && tourType != TourType.OTHER_MAINTENANCE) {
                        continue;
                    }


                    hh[i].setTourID(j);
                    hh[i].setTourCategory(TourType.NON_MANDATORY_CATEGORY);


                    // get utilities for each alternative for this household
                    index.setZoneIndex(hh_taz_id);
                    index.setHHIndex(hh_id);
                    double[] utilities = uec.solve( index, hh[i], utilityAvailability);

                    //set utility for each alternative
                    for(int a=0;a < numberOfAlternatives;a++){
                        alts[a].setAvailability( utilityAvailability[a+1] == 1 );
                        if (utilityAvailability[a+1] == 1)
                            alts[a].setAvailability( (utilities[a] > -99.0) );
                        alts[a].setUtility(utilities[a]);
                    }
                    // set availabilities
                    root.computeAvailabilities();


                    root.getUtility();
                    root.calculateProbabilities();


                    ConcreteAlternative chosen = (ConcreteAlternative) root.chooseElementalAlternative();
                    String chosenAltName= chosen.getName();

                    // find index of the chosen alternative
                    int alt=0;
                    for(int a=0;a < numberOfAlternatives;a++){
                        if (chosenAltName.equals(alternativeNames[a])) {
                            alt = a+1;
                            tourChoice[j]=alt;
                            break;
                        }
                    }

                    int[][] personsByPersonTypeArray = hh[i].getPersonsByPersonTypeArray();
                    int p = 0;

                    switch (alt) {
                    case 1:
                        p = personsByPersonTypeArray[PersonType.WORKER_F][0];
                        break;
                    case 2:
                        p = personsByPersonTypeArray[PersonType.WORKER_F][1];
                        break;
                    case 3:
                        p = personsByPersonTypeArray[PersonType.WORKER_F][2];
                        break;
                    case 4:
                        p = personsByPersonTypeArray[PersonType.WORKER_F][3];
                        break;
                    case 5:
                        p = personsByPersonTypeArray[PersonType.WORKER_P][0];
                        break;
                    case 6:
                        p = personsByPersonTypeArray[PersonType.WORKER_P][1];
                        break;
                    case 7:
                        p = personsByPersonTypeArray[PersonType.WORKER_P][2];
                        break;
                    case 8:
                        p = personsByPersonTypeArray[PersonType.WORKER_P][3];
                        break;
                    case 9:
                        p = personsByPersonTypeArray[PersonType.NONWORKER][0];
                        break;
                    case 10:
                        p = personsByPersonTypeArray[PersonType.NONWORKER][1];
                        break;
                    case 11:
                        p = personsByPersonTypeArray[PersonType.NONWORKER][2];
                        break;
                    case 12:
                        p = personsByPersonTypeArray[PersonType.NONWORKER][3];
                        break;
                    case 13:
                        p = personsByPersonTypeArray[PersonType.SCHOOL_PRED][0];
                        break;
                    case 14:
                        p = personsByPersonTypeArray[PersonType.SCHOOL_PRED][1];
                        break;
                    case 15:
                        p = personsByPersonTypeArray[PersonType.SCHOOL_PRED][2];
                        break;
                    case 16:
                        p = personsByPersonTypeArray[PersonType.SCHOOL_PRED][3];
                        break;
                    case 17:
                        p = personsByPersonTypeArray[PersonType.SCHOOL_DRIV][0];
                        break;
                    case 18:
                        p = personsByPersonTypeArray[PersonType.SCHOOL_DRIV][1];
                        break;
                    case 19:
                        p = personsByPersonTypeArray[PersonType.SCHOOL_DRIV][2];
                        break;
                    case 20:
                        p = personsByPersonTypeArray[PersonType.SCHOOL_DRIV][3];
                        break;
                    }

                    it[j].setPersonParticipation(p, true);
                    persons[p].setIndividualTourParticipation(j, true);

                    persons[p].setNumIndNonMandInceTours( persons[p].getNumIndNonMandInceTours() + 1 );

                    if (tourType != TourType.ESCORTING)
                        persons[p].setNumIndNonMandTours( persons[p].getNumIndNonMandTours() + 1 );

                }//end for individual tours



                if (outputFile != null) {

                    for (int p=1; p < persons.length; p++) {
                        if (it != null) {
                            for (int t=0; t < it.length; t++) {
                                tableData[0] = hh_id;
                                tableData[1] = hh_taz_id;
                                tableData[2] = persons[p].getID();
                                tableData[3] = t+1;
                                tableData[4] = tourChoice[t];
                                tableData[5] = persons[p].getPersonType();
                                tableData[6] = persons[p].getPatternType();
                                tableData[7] = ( it[t].getTourType() );
                                tableData[8] = ( it[t].getPersonParticipation(p) ? 1 : 2 );
                                choiceFreqs[tourChoice[t]]++;


                                // write out .csv file record for this tour
                                outStream.print( tableData[0] );
                                for (int c=1; c < tableHeadings.size(); c++) {
                                    outStream.print(",");
                                    outStream.print( tableData[c] );
                                }
                                outStream.println();
                            }
                        }
                    }
                }
            }//next household

            if (outputFile != null) {

                logger.info ("finished writing Indiv Non-mandatory Maintenance Allocation output file.");
                outStream.close();

            }

        }
        catch (IOException e) {

               throw new RuntimeException("Couldn't write to file " + outputFile, e);

        }


        if(summaryOutput){

            writeFreqSummaryToLogger ( "Individual Tour Participation", "indiv_main_freq", choiceFreqs );

        }
    }


    public void runDiscretionaryWorkerFrequency () {

        int hh_id;
        int hh_taz_id;

        int alt=0;
        int[] personChoice = null;
        int[] choiceFreqs = null;
        int[] utilityAvailability;
        float[] tableData = null;
        ArrayList<String> tableHeadings = null;


        PrintWriter outStream = null;


        logger.info ("Starting  Individual Discretionary Tour Frequency (Workers )");



        //open files named in morpc.proerties and run fully joint tour frequency model
        String controlFile = rb.getString( "indiv.non.mandatory.control.file");
        String outputFile = rb.getString( "indiv.non.mandatory.worker.disc.frequency.output.file");
        String summaryRequest = rb.getString( "indiv.non.mandatory.summary.output" );


        boolean summaryOutput = false;
        if (summaryRequest != null)
            if (summaryRequest.equals("true"))
                summaryOutput = true;


        try {

            if (outputFile != null) {

                // open output stream for DTM output file
                outStream = new PrintWriter (new BufferedWriter( new FileWriter(outputFile) ) );


                // define filed names for .csv file being written
                tableHeadings = new ArrayList<String>();
                tableHeadings.add(DataWriter.HHID_FIELD);
                tableHeadings.add(DataWriter.HHTAZID_FIELD);
                tableHeadings.add("person_id");
                tableHeadings.add("personType");
                tableHeadings.add("indiv_disc_freq");


                // temp array for holding field values to be written
                tableData = new float[tableHeadings.size()];


                //Print field names to .csv file header record
                outStream.print( (String)tableHeadings.get(0) );
                for (int i = 1; i < tableHeadings.size(); i++) {
                    outStream.print(",");
                    outStream.print( (String)tableHeadings.get(i) );
                }
                outStream.println();

            }


            // create a new UEC to get utilties for this logit model
            UtilityExpressionCalculator uec = new UtilityExpressionCalculator(new File(controlFile), DISC_WORK_FREQ_MODEL_SHEET, INDIV_DATA_SHEET, rb, Household.class);
            int numberOfAlternatives = uec.getNumberOfAlternatives();
            String[] alternativeNames = uec.getAlternativeNames();

            // create and define a new LogitModel object
            LogitModel root= new LogitModel("root", numberOfAlternatives);
            ConcreteAlternative[] alts= new ConcreteAlternative[numberOfAlternatives];


            for(int i=0;i<numberOfAlternatives;i++){
                logger.debug("alternative "+(i+1)+" is "+alternativeNames[i] );
                alts[i]  = new ConcreteAlternative(alternativeNames[i], (i+1));
                root.addAlternative (alts[i]);
                logger.debug(alternativeNames[i]+" has been added to the root");
            }

            // set availabilities
            root.computeAvailabilities();
            root.writeAvailabilities();




            utilityAvailability = new int[numberOfAlternatives+1];
            Arrays.fill (utilityAvailability, 1);


            choiceFreqs = new int[numberOfAlternatives+1];


            // loop over all households in the hh table
            for (int h=1; h < hh.length; h++) {

                hh_id = hh[h].getID();
                hh_taz_id = hh[h].getTazID();

                // get person array for this household.
                Person[] persons = hh[h].getPersonArray();


                personChoice = new int[persons.length];


                Tour[] it = hh[h].getIndivTours();


                // loop over persons array for the hh
                for (int p=1; p < persons.length; p++) {


                    // this model only applies to full-time and part-timeperson types with activities away from home.
                    int personType = persons[p].getPersonType();
                    int patternType = persons[p].getPatternType();
                    if (personType != PersonType.WORKER_F && personType != PersonType.WORKER_P || patternType == PatternType.HOME){
                        continue;
                    }

                    hh[h].setPersonID(p);
                    hh[h].setTourCategory(TourType.NON_MANDATORY_CATEGORY);


                    // if this person has NONMANDATORY pattern, but hasn't been allocated
                    // any non-mandatory tours so far, then alternative 1 (0 tours) is not available.
                    Arrays.fill (utilityAvailability, 1);
                    if ( patternType == PatternType.NON_MAND && persons[p].getNumJointTours() == 0 && persons[p].getNumIndNonMandInceTours() == 0 )
                        utilityAvailability[1] = 0;

                    // get utilities for each alternative for this household
                    index.setZoneIndex(hh_taz_id);
                    index.setHHIndex(hh_id);
                    double[] utilities = uec.solve( index, hh[h], utilityAvailability );

                    //set utility for each alternative
                    for(int a=0;a < numberOfAlternatives;a++){
                        alts[a].setAvailability( utilityAvailability[a+1] == 1 );
                        if (utilityAvailability[a+1] == 1)
                            alts[a].setAvailability( (utilities[a] > -99.0) );
                        alts[a].setUtility(utilities[a]);
                    }
                    // set availabilities
                    root.computeAvailabilities();


                    root.getUtility();
                    root.calculateProbabilities();


                    ConcreteAlternative chosen = (ConcreteAlternative) root.chooseElementalAlternative();
                    String chosenAltName= chosen.getName();

                    // find index of the chosen alternative
                    for(int a=0;a < numberOfAlternatives;a++){
                        if (chosenAltName.equals(alternativeNames[a])) {
                            alt = a+1;
                            personChoice[p-1] = alt;
                            break;
                        }
                    }



                    Tour[] it0;
                    Tour[] it1;
                    boolean[] tempParticipate;
                    int currentLength = 0;

                    choiceFreqs[alt]++;
                    switch (alt) {
                    case 1:
                        it1 = null;
                        break;
                    case 2:
                        // add this EAT tour to the array of individual tours for the household
                        it0 = hh[h].getIndivTours();
                        currentLength = (it0 == null ? 0 : it0.length);
                        it1 = new Tour[currentLength + 1];
                        for (int i=0; i < currentLength; i++)
                            it1[i] = it0[i];
                        it1[currentLength] = new Tour(hh[h].getHHSize());
                        it1[currentLength].setTourType (TourType.EAT);
                        it1[currentLength].setTourOrder (0);
                        it1[currentLength].setPersonParticipation(p, true);

                        // lengthen the individualTourParticipationArray for each person in the household
                        tempParticipate = new boolean[currentLength];
                        for (int q=1; q < persons.length; q++) {
                            for (int i=0; i < currentLength; i++)
                                tempParticipate[i] = persons[q].getIndividualTourParticipation(i);
                            persons[q].setIndividualTourParticipationArray(currentLength+1);
                            for (int i=0; i < currentLength; i++)
                                persons[q].setIndividualTourParticipation(i, tempParticipate[i]);
                            persons[q].setIndividualTourParticipation(currentLength, false);
                        }
                        persons[p].setIndividualTourParticipation(currentLength, true);
                        persons[p].setNumIndNonMandEatTours( persons[p].getNumIndNonMandEatTours() + 1 );
                        persons[p].setNumIndNonMandTours( persons[p].getNumIndNonMandTours() + 1 );
                        persons[p].setNumIndNonMandInceTours( persons[p].getNumIndNonMandInceTours() + 1 );

                        hh[h].incrementIndivToursByType (TourType.EAT);
                        hh[h].setIndivTours(it1);
                        break;
                    case 3:
                        // add this DISCRETIONARY tour to the array of individual tours for the household
                        it0 = hh[h].getIndivTours();
                        currentLength = (it0 == null ? 0 : it0.length);
                        it1 = new Tour[currentLength + 1];
                        for (int i=0; i < currentLength; i++)
                            it1[i] = it0[i];
                        it1[currentLength] = new Tour(hh[h].getHHSize());
                        it1[currentLength].setTourType (TourType.DISCRETIONARY);
                        it1[currentLength].setTourOrder (0);
                        it1[currentLength].setPersonParticipation(p, true);

                        // lengthen the individualTourParticipationArray for each person in the household
                        tempParticipate = new boolean[currentLength];
                        for (int q=1; q < persons.length; q++) {
                            for (int i=0; i < currentLength; i++)
                                tempParticipate[i] = persons[q].getIndividualTourParticipation(i);
                            persons[q].setIndividualTourParticipationArray(currentLength+1);
                            for (int i=0; i < currentLength; i++)
                                persons[q].setIndividualTourParticipation(i, tempParticipate[i]);
                            persons[q].setIndividualTourParticipation(currentLength, false);
                        }
                        persons[p].setIndividualTourParticipation(currentLength, true);
                        persons[p].setNumIndNonMandDiscrTours( persons[p].getNumIndNonMandDiscrTours() + 1 );
                        persons[p].setNumIndNonMandTours( persons[p].getNumIndNonMandTours() + 1 );
                        persons[p].setNumIndNonMandInceTours( persons[p].getNumIndNonMandInceTours() + 1 );

                        hh[h].incrementIndivToursByType (TourType.DISCRETIONARY);
                        hh[h].setIndivTours(it1);
                        break;
                    case 4:
                        // add two DISCRETIONARY tours to the array of individual tours for the household
                        it0 = hh[h].getIndivTours();
                        currentLength = (it0 == null ? 0 : it0.length);
                        it1 = new Tour[currentLength + 2];
                        for (int i=0; i < currentLength; i++)
                            it1[i] = it0[i];
                        it1[currentLength] = new Tour(hh[h].getHHSize());
                        it1[currentLength].setTourType (TourType.DISCRETIONARY);
                        it1[currentLength].setTourOrder (1);
                        it1[currentLength].setPersonParticipation(p, true);
                        it1[currentLength+1] = new Tour(hh[h].getHHSize());
                        it1[currentLength+1].setTourType (TourType.DISCRETIONARY);
                        it1[currentLength+1].setTourOrder (2);
                        it1[currentLength+1].setPersonParticipation(p, true);

                        // lengthen the individualTourParticipationArray for each person in the household
                        tempParticipate = new boolean[currentLength];
                        for (int q=1; q < persons.length; q++) {
                            for (int i=0; i < currentLength; i++)
                                tempParticipate[i] = persons[q].getIndividualTourParticipation(i);
                            persons[q].setIndividualTourParticipationArray(currentLength+2);
                            for (int i=0; i < currentLength; i++)
                                persons[q].setIndividualTourParticipation(i, tempParticipate[i]);
                            persons[q].setIndividualTourParticipation(currentLength, false);
                            persons[q].setIndividualTourParticipation(currentLength+1, false);
                        }
                        persons[p].setIndividualTourParticipation(currentLength, true);
                        persons[p].setIndividualTourParticipation(currentLength+1, true);
                        persons[p].setNumIndNonMandDiscrTours( persons[p].getNumIndNonMandDiscrTours() + 2 );
                        persons[p].setNumIndNonMandTours( persons[p].getNumIndNonMandTours() + 2 );
                        persons[p].setNumIndNonMandInceTours( persons[p].getNumIndNonMandInceTours() + 2 );

                        hh[h].incrementIndivToursByType (TourType.DISCRETIONARY);
                        hh[h].incrementIndivToursByType (TourType.DISCRETIONARY);
                        hh[h].setIndivTours(it1);
                        break;
                    case 5:
                        // add one DISCRETIONARY and one EAT tour to the array of individual tours for the household
                        it0 = hh[h].getIndivTours();
                        currentLength = (it0 == null ? 0 : it0.length);
                        it1 = new Tour[currentLength + 2];
                        for (int i=0; i < currentLength; i++)
                            it1[i] = it0[i];
                        it1[currentLength] = new Tour(hh[h].getHHSize());
                        it1[currentLength].setTourType (TourType.DISCRETIONARY);
                        it1[currentLength].setTourOrder (1);
                        it1[currentLength].setPersonParticipation(p, true);
                        it1[currentLength+1] = new Tour(hh[h].getHHSize());
                        it1[currentLength+1].setTourType (TourType.EAT);
                        it1[currentLength+1].setTourOrder (2);
                        it1[currentLength+1].setPersonParticipation(p, true);

                        // lengthen the individualTourParticipationArray for each person in the household
                        tempParticipate = new boolean[currentLength];
                        for (int q=1; q < persons.length; q++) {
                            for (int i=0; i < currentLength; i++)
                                tempParticipate[i] = persons[q].getIndividualTourParticipation(i);
                            persons[q].setIndividualTourParticipationArray(currentLength+2);
                            for (int i=0; i < currentLength; i++)
                                persons[q].setIndividualTourParticipation(i, tempParticipate[i]);
                            persons[q].setIndividualTourParticipation(currentLength, false);
                            persons[q].setIndividualTourParticipation(currentLength+1, false);
                        }
                        persons[p].setIndividualTourParticipation(currentLength, true);
                        persons[p].setIndividualTourParticipation(currentLength+1, true);
                        persons[p].setNumIndNonMandDiscrTours( persons[p].getNumIndNonMandDiscrTours() + 1 );
                        persons[p].setNumIndNonMandEatTours( persons[p].getNumIndNonMandEatTours() + 1 );
                        persons[p].setNumIndNonMandTours( persons[p].getNumIndNonMandTours() + 2 );
                        persons[p].setNumIndNonMandInceTours( persons[p].getNumIndNonMandInceTours() + 2 );

                        hh[h].incrementIndivToursByType (TourType.DISCRETIONARY);
                        hh[h].incrementIndivToursByType (TourType.EAT);
                        hh[h].setIndivTours(it1);
                        break;
                    }

                    if (outputFile != null) {
                        tableData[0] = hh_id;
                        tableData[1] = hh_taz_id;
                        tableData[2] = persons[p].getID();
                        tableData[3] = persons[p].getPersonType();
                        tableData[4] = alt;

                        // write out .csv file record for this tour
                        outStream.print( tableData[0] );
                        for (int c=1; c < tableHeadings.size(); c++) {
                            outStream.print(",");
                            outStream.print( tableData[c] );
                        }
                        outStream.println();
                    }

                }//end for persons

            }//next household

            if (outputFile != null) {

                logger.info ("finished writing Indiv Worker Discretionary Frequency output file.");
                outStream.close();

            }

        }
        catch (IOException e) {

               throw new RuntimeException("Couldn't open file " + outputFile, e);

        }


        if(summaryOutput){

            writeFreqSummaryToLogger ( "Individual Discretionary Tour Frequency (Workers)", "indiv_w_disc_freq", choiceFreqs );

        }
    }



    public void runDiscretionaryNonWorkerFrequency () {

        int hh_id;
        int hh_taz_id;


        int alt=0;
        int[] personChoice = null;
        int[] choiceFreqs = null;
        int[] utilityAvailability;
        float[] tableData = null;
        ArrayList<String> tableHeadings = null;


        PrintWriter outStream = null;


        logger.info ("Starting Individual Discretionary Tour Frequency (NonWorkers)");

        //open files named in morpc.proerties and run fully joint tour frequency model
        String controlFile = rb.getString( "indiv.non.mandatory.control.file");
        String outputFile = rb.getString( "indiv.non.mandatory.nonworker.disc.frequency.output.file");
        String summaryRequest = rb.getString( "indiv.non.mandatory.summary.output" );


        boolean summaryOutput = false;
        if (summaryRequest != null)
            if (summaryRequest.equals("true"))
                summaryOutput = true;


        try {

            if (outputFile != null) {

                // open output stream for DTM output file
                outStream = new PrintWriter (new BufferedWriter( new FileWriter(outputFile) ) );


                // define filed names for .csv file being written
                tableHeadings = new ArrayList<String>();
                tableHeadings.add(DataWriter.HHID_FIELD);
                tableHeadings.add(DataWriter.HHTAZID_FIELD);
                tableHeadings.add("person_id");
                tableHeadings.add("personType");
                tableHeadings.add("indiv_disc_freq");


                // temp array for holding field values to be written
                tableData = new float[tableHeadings.size()];


                //Print field names to .csv file header record
                outStream.print( (String)tableHeadings.get(0) );
                for (int i = 1; i < tableHeadings.size(); i++) {
                    outStream.print(",");
                    outStream.print( (String)tableHeadings.get(i) );
                }
                outStream.println();

            }



            // create a new UEC to get utilties for this logit model
            UtilityExpressionCalculator uec = new UtilityExpressionCalculator(new File(controlFile), DISC_NOWORK_FREQ_MODEL_SHEET, INDIV_DATA_SHEET, rb, Household.class);
            int numberOfAlternatives = uec.getNumberOfAlternatives();
            String[] alternativeNames = uec.getAlternativeNames();

            choiceFreqs = new int[numberOfAlternatives+1];

            // create and define a new LogitModel object
            LogitModel root= new LogitModel("root", numberOfAlternatives);
            ConcreteAlternative[] alts= new ConcreteAlternative[numberOfAlternatives];


            for(int i=0;i<numberOfAlternatives;i++){
                logger.debug("alternative "+(i+1)+" is "+alternativeNames[i] );
                alts[i]  = new ConcreteAlternative(alternativeNames[i], i+1);
                root.addAlternative (alts[i]);
                logger.debug(alternativeNames[i]+" has been added to the root");
            }

            // set availabilities
            root.computeAvailabilities();
            root.writeAvailabilities();



            utilityAvailability = new int[numberOfAlternatives+1];



            // loop over all households in the hh table
            for (int h=1; h < hh.length; h++) {

                hh_id = hh[h].getID();
                hh_taz_id = hh[h].getTazID();


                Person[] persons = hh[h].getPersonArray();
                personChoice = new int[persons.length];


                Tour[] it = hh[h].getIndivTours();

                // loop over individual tours array for the hh
                for (int p=1; p < persons.length; p++) {


                    // this model only applies to nonworker person types with activities away from home.
                    int personType = persons[p].getPersonType();
                    int patternType = persons[p].getPatternType();
                    if (personType != PersonType.NONWORKER || patternType == PatternType.HOME){
                        continue;
                    }

                    hh[h].setPersonID(p);
                    hh[h].setTourCategory(TourType.NON_MANDATORY_CATEGORY);


                    // if this person has NONMANDATORY pattern, but hasn't been allocated
                    // any non-mandatory tours so far, then alternative 1 (0 tours) is not available.
                    Arrays.fill (utilityAvailability, 1);
                    if ( patternType == PatternType.NON_MAND && persons[p].getNumJointTours() == 0 && persons[p].getNumIndNonMandInceTours() == 0 )
                        utilityAvailability[1] = 0;


                    // get utilities for each alternative for this household
                    index.setZoneIndex(hh_taz_id);
                    index.setHHIndex(hh_id);
                    double[] utilities = uec.solve( index, hh[h], utilityAvailability);

                    //set utility for each alternative
                    for(int a=0;a < numberOfAlternatives;a++){
                        alts[a].setAvailability( utilityAvailability[a+1] == 1 );
                        if (utilityAvailability[a+1] == 1)
                            alts[a].setAvailability( (utilities[a] > -99.0) );
                        alts[a].setUtility(utilities[a]);
                    }
                    // set availabilities
                    root.computeAvailabilities();


                    root.getUtility();
                    root.calculateProbabilities();


                    ConcreteAlternative chosen = (ConcreteAlternative) root.chooseElementalAlternative();
                    String chosenAltName= chosen.getName();

                    // find index of the chosen alternative
                    for(int a=0;a < numberOfAlternatives;a++){
                        if (chosenAltName.equals(alternativeNames[a])) {
                            alt = a+1;
                            personChoice[p-1] = alt;
                            break;
                        }
                    }



                    Tour[] it0;
                    Tour[] it1;
                    boolean[] tempParticipate;
                    int currentLength = 0;

                    choiceFreqs[alt]++;
                    switch (alt) {
                    case 1:
                        it1 = null;
                        break;
                    case 2:
                        // add this EAT tour to the array of individual tours for the household
                        it0 = hh[h].getIndivTours();
                        currentLength = (it0 == null ? 0 : it0.length);
                        it1 = new Tour[currentLength + 1];
                        for (int i=0; i < currentLength; i++)
                            it1[i] = it0[i];
                        it1[currentLength] = new Tour(hh[h].getHHSize());
                        it1[currentLength].setTourType (TourType.EAT);
                        it1[currentLength].setTourOrder (0);
                        it1[currentLength].setPersonParticipation(p, true);

                        // lengthen the individualTourParticipationArray for each person in the household
                        tempParticipate = new boolean[currentLength];
                        for (int q=1; q < persons.length; q++) {
                            for (int i=0; i < currentLength; i++)
                                tempParticipate[i] = persons[q].getIndividualTourParticipation(i);
                            persons[q].setIndividualTourParticipationArray(currentLength+1);
                            for (int i=0; i < currentLength; i++)
                                persons[q].setIndividualTourParticipation(i, tempParticipate[i]);
                            persons[q].setIndividualTourParticipation(currentLength, false);
                        }
                        persons[p].setIndividualTourParticipation(currentLength, true);
                        persons[p].setNumIndNonMandEatTours( persons[p].getNumIndNonMandEatTours() + 1 );
                        persons[p].setNumIndNonMandTours( persons[p].getNumIndNonMandTours() + 1 );
                        persons[p].setNumIndNonMandInceTours( persons[p].getNumIndNonMandInceTours() + 1 );

                        hh[h].incrementIndivToursByType (TourType.EAT);
                        hh[h].setIndivTours(it1);
                        break;
                    case 3:
                        // add this DISCRETIONARY tour to the array of individual tours for the household
                        it0 = hh[h].getIndivTours();
                        currentLength = (it0 == null ? 0 : it0.length);
                        it1 = new Tour[currentLength + 1];
                        for (int i=0; i < currentLength; i++)
                            it1[i] = it0[i];
                        it1[currentLength] = new Tour(hh[h].getHHSize());
                        it1[currentLength].setTourType (TourType.DISCRETIONARY);
                        it1[currentLength].setTourOrder (0);
                        it1[currentLength].setPersonParticipation(p, true);

                        // lengthen the individualTourParticipationArray for each person in the household
                        tempParticipate = new boolean[currentLength];
                        for (int q=1; q < persons.length; q++) {
                            for (int i=0; i < currentLength; i++)
                                tempParticipate[i] = persons[q].getIndividualTourParticipation(i);
                            persons[q].setIndividualTourParticipationArray(currentLength+1);
                            for (int i=0; i < currentLength; i++)
                                persons[q].setIndividualTourParticipation(i, tempParticipate[i]);
                            persons[q].setIndividualTourParticipation(currentLength, false);
                        }
                        persons[p].setIndividualTourParticipation(currentLength, true);
                        persons[p].setNumIndNonMandDiscrTours( persons[p].getNumIndNonMandDiscrTours() + 1 );
                        persons[p].setNumIndNonMandTours( persons[p].getNumIndNonMandTours() + 1 );
                        persons[p].setNumIndNonMandInceTours( persons[p].getNumIndNonMandInceTours() + 1 );

                        hh[h].incrementIndivToursByType (TourType.DISCRETIONARY);
                        hh[h].setIndivTours(it1);
                        break;
                    case 4:
                        // add two DISCRETIONARY tours to the array of individual tours for the household
                        it0 = hh[h].getIndivTours();
                        currentLength = (it0 == null ? 0 : it0.length);
                        it1 = new Tour[currentLength + 2];
                        for (int i=0; i < currentLength; i++)
                            it1[i] = it0[i];
                        it1[currentLength] = new Tour(hh[h].getHHSize());
                        it1[currentLength].setTourType (TourType.DISCRETIONARY);
                        it1[currentLength].setTourOrder (1);
                        it1[currentLength].setPersonParticipation(p, true);
                        it1[currentLength+1] = new Tour(hh[h].getHHSize());
                        it1[currentLength+1].setTourType (TourType.DISCRETIONARY);
                        it1[currentLength+1].setTourOrder (2);
                        it1[currentLength+1].setPersonParticipation(p, true);

                        // lengthen the individualTourParticipationArray for each person in the household
                        tempParticipate = new boolean[currentLength];
                        for (int q=1; q < persons.length; q++) {
                            for (int i=0; i < currentLength; i++)
                                tempParticipate[i] = persons[q].getIndividualTourParticipation(i);
                            persons[q].setIndividualTourParticipationArray(currentLength+2);
                            for (int i=0; i < currentLength; i++)
                                persons[q].setIndividualTourParticipation(i, tempParticipate[i]);
                            persons[q].setIndividualTourParticipation(currentLength, false);
                            persons[q].setIndividualTourParticipation(currentLength+1, false);
                        }
                        persons[p].setIndividualTourParticipation(currentLength, true);
                        persons[p].setIndividualTourParticipation(currentLength+1, true);
                        persons[p].setNumIndNonMandDiscrTours( persons[p].getNumIndNonMandDiscrTours() + 2 );
                        persons[p].setNumIndNonMandTours( persons[p].getNumIndNonMandTours() + 2 );
                        persons[p].setNumIndNonMandInceTours( persons[p].getNumIndNonMandInceTours() + 2 );

                        hh[h].incrementIndivToursByType (TourType.DISCRETIONARY);
                        hh[h].incrementIndivToursByType (TourType.DISCRETIONARY);
                        hh[h].setIndivTours(it1);
                        break;
                    case 5:
                        // add one DISCRETIONARY and one EAT tour to the array of individual tours for the household
                        it0 = hh[h].getIndivTours();
                        currentLength = (it0 == null ? 0 : it0.length);
                        it1 = new Tour[currentLength + 2];
                        for (int i=0; i < currentLength; i++)
                            it1[i] = it0[i];
                        it1[currentLength] = new Tour(hh[h].getHHSize());
                        it1[currentLength].setTourType (TourType.DISCRETIONARY);
                        it1[currentLength].setTourOrder (1);
                        it1[currentLength].setPersonParticipation(p, true);
                        it1[currentLength+1] = new Tour(hh[h].getHHSize());
                        it1[currentLength+1].setTourType (TourType.EAT);
                        it1[currentLength+1].setTourOrder (2);
                        it1[currentLength+1].setPersonParticipation(p, true);

                        // lengthen the individualTourParticipationArray for each person in the household
                        tempParticipate = new boolean[currentLength];
                        for (int q=1; q < persons.length; q++) {
                            for (int i=0; i < currentLength; i++)
                                tempParticipate[i] = persons[q].getIndividualTourParticipation(i);
                            persons[q].setIndividualTourParticipationArray(currentLength+2);
                            for (int i=0; i < currentLength; i++)
                                persons[q].setIndividualTourParticipation(i, tempParticipate[i]);
                            persons[q].setIndividualTourParticipation(currentLength, false);
                            persons[q].setIndividualTourParticipation(currentLength+1, false);
                        }
                        persons[p].setIndividualTourParticipation(currentLength, true);
                        persons[p].setIndividualTourParticipation(currentLength+1, true);
                        persons[p].setNumIndNonMandDiscrTours( persons[p].getNumIndNonMandDiscrTours() + 1 );
                        persons[p].setNumIndNonMandEatTours( persons[p].getNumIndNonMandEatTours() + 1 );
                        persons[p].setNumIndNonMandTours( persons[p].getNumIndNonMandTours() + 2 );
                        persons[p].setNumIndNonMandInceTours( persons[p].getNumIndNonMandInceTours() + 2 );

                        hh[h].incrementIndivToursByType (TourType.DISCRETIONARY);
                        hh[h].incrementIndivToursByType (TourType.EAT);
                        hh[h].setIndivTours(it1);
                        break;
                    }

                    if (outputFile != null) {
                        tableData[0] = hh_id;
                        tableData[1] = hh_taz_id;
                        tableData[2] = persons[p].getID();
                        tableData[3] = persons[p].getPersonType();
                        tableData[4] = alt;

                        // write out .csv file record for this tour
                        outStream.print( tableData[0] );
                        for (int c=1; c < tableHeadings.size(); c++) {
                            outStream.print(",");
                            outStream.print( tableData[c] );
                        }
                        outStream.println();
                    }


                }//end for persons

        }//next household

            if (outputFile != null) {

                logger.info ("finished writing output file.");
                outStream.close();

            }

        }
        catch (IOException e) {

               throw new RuntimeException("Can't write to file " + outputFile, e);

        }


        if(summaryOutput){

            writeFreqSummaryToLogger ( "Individual Discretionary Tour Frequency (Nonworkers)", "indiv_nw_disc_freq", choiceFreqs );

        }



    }



    public void runDiscretionaryChildFrequency () {

        int hh_id;
        int hh_taz_id;


        int alt=0;
        int[] personChoice = null;
        int[] choiceFreqs = null;
        int[] utilityAvailability;
        float[] tableData = null;
        ArrayList<String> tableHeadings = null;


        PrintWriter outStream = null;


        logger.info ("Starting Individual Discretionary Tour Frequency (School age children)");

        //open files named in morpc.proerties and run fully joint tour frequency model
        String controlFile = rb.getString( "indiv.non.mandatory.control.file");
        String outputFile = rb.getString( "indiv.non.mandatory.child.disc.frequency.output.file");
        String summaryRequest = rb.getString( "indiv.non.mandatory.summary.output" );


        boolean summaryOutput = false;
        if (summaryRequest != null)
            if (summaryRequest.equals("true"))
                summaryOutput = true;


        try {

            if (outputFile != null) {

                // open output stream for DTM output file
                outStream = new PrintWriter (new BufferedWriter( new FileWriter(outputFile) ) );


                // define filed names for .csv file being written
                tableHeadings = new ArrayList<String>();
                tableHeadings.add(DataWriter.HHID_FIELD);
                tableHeadings.add(DataWriter.HHTAZID_FIELD);
                tableHeadings.add("person_id");
                tableHeadings.add("personType");
                tableHeadings.add("indiv_disc_freq");


                // temp array for holding field values to be written
                tableData = new float[tableHeadings.size()];


                //Print field names to .csv file header record
                outStream.print( (String)tableHeadings.get(0) );
                for (int i = 1; i < tableHeadings.size(); i++) {
                    outStream.print(",");
                    outStream.print( (String)tableHeadings.get(i) );
                }
                outStream.println();

            }



            // create a new UEC to get utilties for this logit model
            UtilityExpressionCalculator uec = new UtilityExpressionCalculator(new File(controlFile), DISC_CHILD_FREQ_MODEL_SHEET, INDIV_DATA_SHEET, rb, Household.class);
            int numberOfAlternatives = uec.getNumberOfAlternatives();
            String[] alternativeNames = uec.getAlternativeNames();

            choiceFreqs = new int[numberOfAlternatives+1];

            // create and define a new LogitModel object
            LogitModel root= new LogitModel("root", numberOfAlternatives);
            ConcreteAlternative[] alts= new ConcreteAlternative[numberOfAlternatives];


            for(int i=0;i<numberOfAlternatives;i++){
                logger.debug("alternative "+(i+1)+" is "+alternativeNames[i] );
                alts[i]  = new ConcreteAlternative(alternativeNames[i], i+1);
                root.addAlternative (alts[i]);
                logger.debug(alternativeNames[i]+" has been added to the root");
            }

            // set availabilities
            root.computeAvailabilities();
            root.writeAvailabilities();



            utilityAvailability = new int[numberOfAlternatives+1];



            // loop over all households in the hh table
            for (int h=1; h < hh.length; h++) {

                hh_id = hh[h].getID();
                hh_taz_id = hh[h].getTazID();


                // get person array for this household.
                Person[] persons = hh[h].getPersonArray();

                personChoice = new int[persons.length];


                Tour[] it = hh[h].getIndivTours();

                // loop over individual tours array for the hh
                for (int p=1; p < persons.length; p++) {


                    // this model only applies to school aged person types with activities away from home.
                    int personType = persons[p].getPersonType();
                    int patternType = persons[p].getPatternType();
                    if (personType != PersonType.SCHOOL_DRIV && personType != PersonType.SCHOOL_PRED || patternType == PatternType.HOME){
                        continue;
                    }

                    hh[h].setPersonID(p);
                    hh[h].setTourCategory(TourType.NON_MANDATORY_CATEGORY);


                    // if this person has NONMANDATORY pattern, but hasn't been allocated
                    // any non-mandatory tours so far, then alternative 1 (0 tours) is not available.
                    Arrays.fill (utilityAvailability, 1);
                    if ( patternType == PatternType.NON_MAND && persons[p].getNumJointTours() == 0 && persons[p].getNumIndNonMandInceTours() == 0 )
                        utilityAvailability[1] = 0;

                    // get utilities for each alternative for this household
                    index.setZoneIndex(hh_taz_id);
                    index.setHHIndex(hh_id);
                    double[] utilities = uec.solve( index, hh[h], utilityAvailability);

                    //set utility for each alternative
                    for(int a=0;a < numberOfAlternatives;a++){
                        alts[a].setAvailability( utilityAvailability[a+1] == 1 );
                        if (utilityAvailability[a+1] == 1)
                            alts[a].setAvailability( (utilities[a] > -99.0) );
                        alts[a].setUtility(utilities[a]);
                    }
                    // set availabilities
                    root.computeAvailabilities();


                    root.getUtility();
                    root.calculateProbabilities();


                    ConcreteAlternative chosen = (ConcreteAlternative) root.chooseElementalAlternative();
                    String chosenAltName= chosen.getName();

                    // find index of the chosen alternative
                    for(int a=0;a < numberOfAlternatives;a++){
                        if (chosenAltName.equals(alternativeNames[a])) {
                            alt = a+1;
                            personChoice[p-1] = alt;
                            break;
                        }
                    }



                    Tour[] it0;
                    Tour[] it1;
                    boolean[] tempParticipate;
                    int currentLength = 0;

                    choiceFreqs[alt]++;
                    switch (alt) {
                    case 1:
                        it1 = null;
                        break;
                    case 2:
                        // add this EAT tour to the array of individual tours for the household
                        it0 = hh[h].getIndivTours();
                        currentLength = (it0 == null ? 0 : it0.length);
                        it1 = new Tour[currentLength + 1];
                        for (int i=0; i < currentLength; i++)
                            it1[i] = it0[i];
                        it1[currentLength] = new Tour(hh[h].getHHSize());
                        it1[currentLength].setTourType (TourType.EAT);
                        it1[currentLength].setTourOrder (0);
                        it1[currentLength].setPersonParticipation(p, true);

                        // lengthen the individualTourParticipationArray for each person in the household
                        tempParticipate = new boolean[currentLength];
                        for (int q=1; q < persons.length; q++) {
                            for (int i=0; i < currentLength; i++)
                                tempParticipate[i] = persons[q].getIndividualTourParticipation(i);
                            persons[q].setIndividualTourParticipationArray(currentLength+1);
                            for (int i=0; i < currentLength; i++)
                                persons[q].setIndividualTourParticipation(i, tempParticipate[i]);
                            persons[q].setIndividualTourParticipation(currentLength, false);
                        }
                        persons[p].setIndividualTourParticipation(currentLength, true);
                        persons[p].setNumIndNonMandEatTours( persons[p].getNumIndNonMandEatTours() + 1 );
                        persons[p].setNumIndNonMandTours( persons[p].getNumIndNonMandTours() + 1 );
                        persons[p].setNumIndNonMandInceTours( persons[p].getNumIndNonMandInceTours() + 1 );

                        hh[h].incrementIndivToursByType (TourType.EAT);
                        hh[h].setIndivTours(it1);
                        break;
                    case 3:
                        // add this DISCRETIONARY tour to the array of individual tours for the household
                        it0 = hh[h].getIndivTours();
                        currentLength = (it0 == null ? 0 : it0.length);
                        it1 = new Tour[currentLength + 1];
                        for (int i=0; i < currentLength; i++)
                            it1[i] = it0[i];
                        it1[currentLength] = new Tour(hh[h].getHHSize());
                        it1[currentLength].setTourType (TourType.DISCRETIONARY);
                        it1[currentLength].setTourOrder (0);
                        it1[currentLength].setPersonParticipation(p, true);

                        // lengthen the individualTourParticipationArray for each person in the household
                        tempParticipate = new boolean[currentLength];
                        for (int q=1; q < persons.length; q++) {
                            for (int i=0; i < currentLength; i++)
                                tempParticipate[i] = persons[q].getIndividualTourParticipation(i);
                            persons[q].setIndividualTourParticipationArray(currentLength+1);
                            for (int i=0; i < currentLength; i++)
                                persons[q].setIndividualTourParticipation(i, tempParticipate[i]);
                            persons[q].setIndividualTourParticipation(currentLength, false);
                        }
                        persons[p].setIndividualTourParticipation(currentLength, true);
                        persons[p].setNumIndNonMandDiscrTours( persons[p].getNumIndNonMandDiscrTours() + 1 );
                        persons[p].setNumIndNonMandTours( persons[p].getNumIndNonMandTours() + 1 );
                        persons[p].setNumIndNonMandInceTours( persons[p].getNumIndNonMandInceTours() + 1 );

                        hh[h].incrementIndivToursByType (TourType.DISCRETIONARY);
                        hh[h].setIndivTours(it1);
                        break;
                    case 4:
                        // add two DISCRETIONARY tours to the array of individual tours for the household
                        it0 = hh[h].getIndivTours();
                        currentLength = (it0 == null ? 0 : it0.length);
                        it1 = new Tour[currentLength + 2];
                        for (int i=0; i < currentLength; i++)
                            it1[i] = it0[i];
                        it1[currentLength] = new Tour(hh[h].getHHSize());
                        it1[currentLength].setTourType (TourType.DISCRETIONARY);
                        it1[currentLength].setTourOrder (1);
                        it1[currentLength].setPersonParticipation(p, true);
                        it1[currentLength+1] = new Tour(hh[h].getHHSize());
                        it1[currentLength+1].setTourType (TourType.DISCRETIONARY);
                        it1[currentLength+1].setTourOrder (2);
                        it1[currentLength+1].setPersonParticipation(p, true);

                        // lengthen the individualTourParticipationArray for each person in the household
                        tempParticipate = new boolean[currentLength];
                        for (int q=1; q < persons.length; q++) {
                            for (int i=0; i < currentLength; i++)
                                tempParticipate[i] = persons[q].getIndividualTourParticipation(i);
                            persons[q].setIndividualTourParticipationArray(currentLength+2);
                            for (int i=0; i < currentLength; i++)
                                persons[q].setIndividualTourParticipation(i, tempParticipate[i]);
                            persons[q].setIndividualTourParticipation(currentLength, false);
                            persons[q].setIndividualTourParticipation(currentLength+1, false);
                        }
                        persons[p].setIndividualTourParticipation(currentLength, true);
                        persons[p].setIndividualTourParticipation(currentLength+1, true);
                        persons[p].setNumIndNonMandDiscrTours( persons[p].getNumIndNonMandDiscrTours() + 1 );
                        persons[p].setNumIndNonMandDiscrTours( persons[p].getNumIndNonMandDiscrTours() + 1 );
                        persons[p].setNumIndNonMandTours( persons[p].getNumIndNonMandTours() + 2 );
                        persons[p].setNumIndNonMandInceTours( persons[p].getNumIndNonMandInceTours() + 2 );

                        hh[h].incrementIndivToursByType (TourType.DISCRETIONARY);
                        hh[h].incrementIndivToursByType (TourType.DISCRETIONARY);
                        hh[h].setIndivTours(it1);
                        break;
                    case 5:
                        // add one DISCRETIONARY and one EAT tour to the array of individual tours for the household
                        it0 = hh[h].getIndivTours();
                        currentLength = (it0 == null ? 0 : it0.length);
                        it1 = new Tour[currentLength + 2];
                        for (int i=0; i < currentLength; i++)
                            it1[i] = it0[i];
                        it1[currentLength] = new Tour(hh[h].getHHSize());
                        it1[currentLength].setTourType (TourType.DISCRETIONARY);
                        it1[currentLength].setTourOrder (1);
                        it1[currentLength].setPersonParticipation(p, true);
                        it1[currentLength+1] = new Tour(hh[h].getHHSize());
                        it1[currentLength+1].setTourType (TourType.EAT);
                        it1[currentLength+1].setTourOrder (2);
                        it1[currentLength+1].setPersonParticipation(p, true);

                        // lengthen the individualTourParticipationArray for each person in the household
                        tempParticipate = new boolean[currentLength];
                        for (int q=1; q < persons.length; q++) {
                            for (int i=0; i < currentLength; i++)
                                tempParticipate[i] = persons[q].getIndividualTourParticipation(i);
                            persons[q].setIndividualTourParticipationArray(currentLength+2);
                            for (int i=0; i < currentLength; i++)
                                persons[q].setIndividualTourParticipation(i, tempParticipate[i]);
                            persons[q].setIndividualTourParticipation(currentLength, false);
                            persons[q].setIndividualTourParticipation(currentLength+1, false);
                        }
                        persons[p].setIndividualTourParticipation(currentLength, true);
                        persons[p].setIndividualTourParticipation(currentLength+1, true);
                        persons[p].setNumIndNonMandDiscrTours( persons[p].getNumIndNonMandDiscrTours() + 1 );
                        persons[p].setNumIndNonMandEatTours( persons[p].getNumIndNonMandEatTours() + 1 );
                        persons[p].setNumIndNonMandTours( persons[p].getNumIndNonMandTours() + 2 );
                        persons[p].setNumIndNonMandInceTours( persons[p].getNumIndNonMandInceTours() + 2 );

                        hh[h].incrementIndivToursByType (TourType.DISCRETIONARY);
                        hh[h].incrementIndivToursByType (TourType.EAT);
                        hh[h].setIndivTours(it1);
                        break;
                    }

                    if (outputFile != null) {
                        tableData[0] = hh_id;
                        tableData[1] = hh_taz_id;
                        tableData[2] = persons[p].getID();
                        tableData[3] = persons[p].getPersonType();
                        tableData[4] = alt;

                        // write out .csv file record for this tour
                        outStream.print( tableData[0] );
                        for (int c=1; c < tableHeadings.size(); c++) {
                            outStream.print(",");
                            outStream.print( tableData[c] );
                        }
                        outStream.println();
                    }


                }//end for persons



                

            }//next household

            if (outputFile != null) {

                logger.info ("finished writing output file.");
                outStream.close();

            }

        }
        catch (IOException e) {

               throw new RuntimeException("Couldn't write to file " + outputFile, e);

        }


        if(summaryOutput){

            writeFreqSummaryToLogger ( "Individual Discretionary Tour Frequency (Nonworkers)", "indiv_c_disc_freq", choiceFreqs );

        }
    }



    public void runAtWorkFrequency () {

        int hh_id;
        int hh_taz_id;
        int person=0;
        int personType=0;

        int[] choiceFreqs = null;
        int[] utilityAvailability;
        int[] tourChoice = null;
        float[][] tourMaker = null;
        float[] tableData = null;
        ArrayList<String> tableHeadings = null;


        PrintWriter outStream = null;


        logger.info ("Starting  Individual AtWork Tour Frequency");


        //open files named in morpc.proerties and run fully joint tour frequency model
        String controlFile = rb.getString( "indiv.non.mandatory.control.file" );
        String outputFile = rb.getString( "indiv.non.mandatory.atwork.frequency.output.file" );
        String summaryRequest = rb.getString( "indiv.non.mandatory.summary.output" );


        boolean summaryOutput = false;
        if (summaryRequest != null)
            if (summaryRequest.equals("true"))
                summaryOutput = true;


        try {

            if (outputFile != null) {

                // open output stream for DTM output file
                outStream = new PrintWriter (new BufferedWriter( new FileWriter(outputFile) ) );


                // define filed names for .csv file being written
                tableHeadings = new ArrayList<String>();
                tableHeadings.add(DataWriter.HHID_FIELD);
                tableHeadings.add(DataWriter.HHTAZID_FIELD);
                tableHeadings.add("man_tour_id");
                tableHeadings.add("man_tour_type");
                tableHeadings.add("workTourMaker_ID");
                tableHeadings.add("workTourMaker_type");
                tableHeadings.add("indiv_atwork_freq");


                // temp array for holding field values to be written
                tableData = new float[tableHeadings.size()];


                //Print field names to .csv file header record
                outStream.print( (String)tableHeadings.get(0) );
                for (int i = 1; i < tableHeadings.size(); i++) {
                    outStream.print(",");
                    outStream.print( (String)tableHeadings.get(i) );
                }
                outStream.println();

            }



            // create a new UEC to get utilties for this logit model
            UtilityExpressionCalculator uec = new UtilityExpressionCalculator(new File(controlFile), ATWORK_FREQ_MODEL_SHEET, INDIV_DATA_SHEET, rb, Household.class);
            int numberOfAlternatives = uec.getNumberOfAlternatives();
            String[] alternativeNames = uec.getAlternativeNames();

            // create and define a new LogitModel object
            LogitModel root= new LogitModel("root", numberOfAlternatives);
            ConcreteAlternative[] alts= new ConcreteAlternative[numberOfAlternatives];


            for(int i=0;i<numberOfAlternatives;i++){
                logger.debug("alternative "+(i+1)+" is "+alternativeNames[i] );
                alts[i]  = new ConcreteAlternative(alternativeNames[i], i+1);
                root.addAlternative (alts[i]);
                logger.debug(alternativeNames[i]+" has been added to the root");
            }

            // set availabilities
            root.computeAvailabilities();
            root.writeAvailabilities();

            utilityAvailability = new int[numberOfAlternatives+1];
            Arrays.fill (utilityAvailability, 1);

            choiceFreqs = new int[numberOfAlternatives+1];


            // loop over all households in the hh table
            for (int h=1; h < hh.length; h++) {
                hh_id = hh[h].getID();
                hh_taz_id = hh[h].getTazID();

                // get person array for this household.
                Person[] persons = hh[h].getPersonArray();


                Tour[] mt = hh[h].getMandatoryTours();
                if (mt != null) {
                    tourChoice = new int[mt.length];
                    tourMaker = new float[mt.length][2];


                    // loop over mandatory tours array for the hh
                    for (int t=0; t < mt.length; t++) {

                        // this model only applies to work tours.
                        int tourType = mt[t].getTourType();
                        if (tourType != TourType.WORK){
                            continue;
                        }

                        // determine which person made this work tour
                        personType = 0;
                        for (int p=1; p < persons.length; p++) {
                            if (mt[t].getPersonParticipation(p)) {
                                personType = persons[p].getPersonType();
                                person = p;
                                break;
                            }
                        }
                        tourMaker[t][0]=person;
                        tourMaker[t][1]=personType;

                        if (personType == 0) {
                            logger.debug ("error in IndividualNonMandatoryToursModel.runAtWorkFrequency()");
                            logger.debug ("no person in persons[] had participation == true");
                            System.exit(1);
                        }


                        hh[h].setPersonID(person);
                        hh[h].setTourID(t);
                        hh[h].setTourCategory(TourType.MANDATORY_CATEGORY);



                        // get utilities for each alternative for this household
                        index.setZoneIndex(mt[t].getDestTaz());
                        index.setHHIndex(hh_id);
                        double[] utilities = uec.solve( index, hh[h], utilityAvailability);

                         //set utility for each alternative
                        for(int a=0;a < numberOfAlternatives;a++){
                            alts[a].setAvailability( utilityAvailability[a+1] == 1 );
                            if (utilityAvailability[a+1] == 1)
                                alts[a].setAvailability( (utilities[a] > -99.0) );
                            alts[a].setUtility(utilities[a]);
                        }
                        // set availabilities
                        root.computeAvailabilities();


                        root.getUtility();
                        root.calculateProbabilities();


                        ConcreteAlternative chosen = (ConcreteAlternative) root.chooseElementalAlternative();
                        String chosenAltName= chosen.getName();

                        // find index of the chosen alternative
                        int alt=0;
                        for(int a=0;a < numberOfAlternatives;a++){
                            if (chosenAltName.equals(alternativeNames[a])) {
                                alt = a+1;
                                tourChoice[t]=alt;
                                break;
                            }
                        }



                        Tour[] it1;
                        switch (alt) {
                        case 1:
                            it1 = null;
                            break;
                        case 2:
                            // add this EAT at-work subtour to the array of subtours for this work tour
                            it1 = new Tour[1];
                            it1[0] = new Tour(hh[h].getHHSize());
                            it1[0].setTourType (TourType.ATWORK);
                            it1[0].setSubTourType (SubTourType.EAT);
                            it1[0].setTourOrder (0);
                            it1[0].setSubTourPerson(person);
                            it1[0].setPersonParticipation(person, true);
                            mt[t].incrementSubToursByType (SubTourType.EAT);
                            mt[t].setSubTours(it1);
                            break;
                        case 3:
                            // add this WORK at-work subtour to the array of subtours for this work tour
                            it1 = new Tour[1];
                            it1[0] = new Tour(hh[h].getHHSize());
                            it1[0].setTourType (TourType.ATWORK);
                            it1[0].setSubTourType (SubTourType.WORK);
                            it1[0].setTourOrder (0);
                            it1[0].setSubTourPerson(person);
                            it1[0].setPersonParticipation(person, true);
                            mt[t].incrementSubToursByType (SubTourType.WORK);
                            mt[t].setSubTours(it1);
                            break;
                        case 4:
                            // add this OTHER at-work subtour to the array of subtours for this work tour
                            it1 = new Tour[1];
                            it1[0] = new Tour(hh[h].getHHSize());
                            it1[0].setTourType (TourType.ATWORK);
                            it1[0].setTourType (TourType.ATWORK);
                            it1[0].setSubTourType (SubTourType.OTHER);
                            it1[0].setTourOrder (0);
                            it1[0].setSubTourPerson(person);
                            it1[0].setPersonParticipation(person, true);
                            mt[t].incrementSubToursByType (SubTourType.OTHER);
                            mt[t].setSubTours(it1);
                            break;
                        case 5:
                            // add these two WORK at-work subtours to the array of subtours for this work tour
                            it1 = new Tour[2];
                            it1[0] = new Tour(hh[h].getHHSize());
                            it1[0].setTourType (TourType.ATWORK);
                            it1[0].setSubTourType (SubTourType.WORK);
                            it1[0].setTourOrder (1);
                            it1[0].setSubTourPerson(person);
                            it1[0].setPersonParticipation(person, true);
                            it1[1] = new Tour(hh[h].getHHSize());
                            it1[1].setTourType (TourType.ATWORK);
                            it1[1].setSubTourType (SubTourType.WORK);
                            it1[1].setTourOrder (2);
                            it1[1].setSubTourPerson(person);
                            it1[1].setPersonParticipation(person, true);
                            mt[t].incrementSubToursByType (SubTourType.WORK);
                            mt[t].incrementSubToursByType (SubTourType.WORK);
                            mt[t].setSubTours(it1);
                            break;
                        case 6:
                            // add these WORK and EAT at-work subtours to the array of subtours for this work tour
                            it1 = new Tour[2];
                            it1[0] = new Tour(hh[h].getHHSize());
                            it1[0].setTourType (TourType.ATWORK);
                            it1[0].setSubTourType (SubTourType.EAT);
                            it1[0].setTourOrder (1);
                            it1[0].setSubTourPerson(person);
                            it1[0].setPersonParticipation(person, true);
                            it1[1] = new Tour(hh[h].getHHSize());
                            it1[1].setTourType (TourType.ATWORK);
                            it1[1].setSubTourType (SubTourType.WORK);
                            it1[1].setTourOrder (2);
                            it1[1].setSubTourPerson(person);
                            it1[1].setPersonParticipation(person, true);
                            mt[t].incrementSubToursByType (SubTourType.EAT);
                            mt[t].incrementSubToursByType (SubTourType.WORK);
                            mt[t].setSubTours(it1);
                            break;
                        }

                    }//end for individual mandatory tours
            }


                if (outputFile != null) {

                    if (mt != null && mt.length > 0) {
                        for (int t=0; t < mt.length; t++) {
                            int tourType = mt[t].getTourType();
                            if (tourType == TourType.WORK){
                                tableData[0] = hh_id;
                                tableData[1] = hh_taz_id;
                                tableData[2] = t+1;
                                tableData[3] = mt[t].getTourType();
                                tableData[4] = tourMaker[t][0];
                                tableData[5] = tourMaker[t][1];
                                tableData[6] = tourChoice[t];
                                choiceFreqs[tourChoice[t]]++;

                                // write out .csv file record for this tour
                                outStream.print( tableData[0] );
                                for (int c=1; c < tableHeadings.size(); c++) {
                                    outStream.print(",");
                                    outStream.print( tableData[c] );
                                }
                                outStream.println();
                            }
                        }
                    }
                }

            }//next household

            if (outputFile != null) {

                logger.info ("Finished writing output file.");
                outStream.close();

            }

        }
        catch (IOException e) {

               throw new RuntimeException("Couldn't write to file " + outputFile, e);

        }


        if(summaryOutput){

            writeFreqSummaryToLogger ( "At-work Tour Frequency", "indiv_atwork_freq", choiceFreqs );

        }



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
