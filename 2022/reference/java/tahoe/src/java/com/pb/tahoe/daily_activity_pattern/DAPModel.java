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
package com.pb.tahoe.daily_activity_pattern;

import com.pb.common.calculator.IndexValues;
import com.pb.common.calculator.UtilityExpressionCalculator;
import com.pb.common.datafile.TableDataSet;
import com.pb.common.model.ConcreteAlternative;
import com.pb.common.model.LogitModel;
import com.pb.tahoe.structures.Household;
import com.pb.tahoe.util.DataWriter;
import org.apache.log4j.Logger;

import java.io.File;
import java.util.Arrays;
import java.util.ResourceBundle;

/**
 * This class is a generic version of the daily activity
 * pattern classes that were coded for MORPC.
 *
 * The user will pass in a personType and a column
 * name to the runDailyActivityPatternChoice method
 * and patterns will be chosen for all persons in all
 * households that are of the personType.
 *
 * @author    Christi Willison
 * @version   1.0, 4/4/2006
 *
 */
public class DAPModel {

    static Logger logger = Logger.getLogger(DAPModel.class);

    static final int MODEL_SHEET = 1;
    static final int DATA_SHEET = 0;

    ResourceBundle tahoeResources;
    public TableDataSet hhTable;


    public DAPModel(ResourceBundle rb) {
        this.tahoeResources = rb;
    }

    public DAPModel() {
    }


    /**
     * The method takes the name of the personType (e.g. PersonType.TYPE4) and
     * the column name that will be used from the synthetic population file
     * (e.g. DataWriter.PRESCHOOL_FIELD)
     * @param personType
     * @param columnName
     */
    public void runDailyActivityPatternChoice(String personType, String columnName) {

       //open files
        String controlFile =  tahoeResources.getString( personType + ".control.file" );

        logger.debug("Control File: " + controlFile);

        // create a new UEC to get utilties for this logit model
        UtilityExpressionCalculator uec = new UtilityExpressionCalculator ( new File(controlFile), MODEL_SHEET, DATA_SHEET, tahoeResources, Household.class );
        int numberOfAlternatives = uec.getNumberOfAlternatives();
        String[] alternativeNames = uec.getAlternativeNames();

        // create and define a new LogitModel object
        LogitModel root= new LogitModel("root", numberOfAlternatives);
        ConcreteAlternative[] alts= new ConcreteAlternative[numberOfAlternatives];

        for(int i=0; i<numberOfAlternatives; i++){
            logger.debug("alternative "+(i+1)+" is "+alternativeNames[i] );
            alts[i]  = new ConcreteAlternative(alternativeNames[i], (i+1));
            root.addAlternative (alts[i]);
            logger.debug(alternativeNames[i]+" has been added to the root");
        }

        // set availabilities
        root.computeAvailabilities();
        root.writeAvailabilities();

        // get the household data table from the UEC control file
        hhTable = uec.getHouseholdData();
        if (hhTable == null) {
            throw new RuntimeException("Could not get householdData TableDataSet from UEC.");
        }

        int hh_idPosition = hhTable.getColumnPosition( DataWriter.HHID_FIELD );
        if (hh_idPosition <= 0) {
            throw new RuntimeException(DataWriter.HHID_FIELD + " was not a field in the householdData TableDataSet.");
        }

        int hh_taz_idPosition = hhTable.getColumnPosition( DataWriter.HHTAZID_FIELD );
        if (hh_taz_idPosition <= 0) {
            throw new RuntimeException(DataWriter.HHTAZID_FIELD + " was not a field in the householdData TableDataSet.");
        }

        int columnPosition = hhTable.getColumnPosition( columnName );
        if (columnPosition <= 0) {
            throw new RuntimeException(columnName + " was not a field in the householdData TableDataSet.");
        }

        //define 2-d array which will hold results of linked attributes
        //which need to be appended to hhTable for use in next mode
        float[][] patternResults = new float[numberOfAlternatives][hhTable.getRowCount()];

        // loop over all households in the hh table
         int hh_id;
         int hh_taz_id;
         int[] sample = new int[uec.getNumberOfAlternatives()+1];
         IndexValues index = new IndexValues();

         for (int i=0; i < hhTable.getRowCount(); i++) {
             hh_id = (int)hhTable.getValueAt( i+1, hh_idPosition );
             hh_taz_id = (int)hhTable.getValueAt( i+1, hh_taz_idPosition );

             // check for persons of personType in the household
             if ( (int)hhTable.getValueAt( i+1, columnName ) > 0 ) {
                // get utilities for each alternative for this household

                index.setZoneIndex( hh_taz_id );
                index.setHHIndex( hh_id );
                Arrays.fill(sample, 1);
                double[] utilities = uec.solve( index, new Object(), sample );
                //set utility for each alternative
                for(int a=0; a < numberOfAlternatives; a++){
                    root.getAlternative(a).setAvailability( sample[a+1] == 1 );
                    if (sample[a+1] == 1)
                        root.getAlternative(a).setAvailability( (utilities[a] > -99.0) );
                    root.getAlternative(a).setUtility(utilities[a]);
                }
                 // set availabilities
                root.computeAvailabilities();

                root.getUtility();
                root.calculateProbabilities();

                //loop over number of specified person type in household
                for (int m=0; m < (int)hhTable.getValueAt( i+1, columnName ); m++){
                    ConcreteAlternative chosen = (ConcreteAlternative) root.chooseElementalAlternative();
                    String chosenAltName= chosen.getName();
                    for (int n = 0; n < alternativeNames.length; n++) {
                        if (chosenAltName.equals(alternativeNames[n])) {
                           patternResults[n][i] ++;
                           break;
                        }
                    }
                }//next person of personType in household
             }//if
        }//next household

        for(int rc=0; rc < numberOfAlternatives; rc++){
            hhTable.appendColumn(patternResults[rc], alternativeNames[rc]+"_"+personType);
        }
        

        logger.info("Finished daily activity pattern choice for all " + personType + "s");
    }

    public void writePatternChoicesToFile(){
        DataWriter writer = DataWriter.getInstance();
        writer.writeDAPFile(hhTable);
    }


}
