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
package com.pb.tahoe.stops;

import com.pb.common.calculator.UtilityExpressionCalculator;
import com.pb.tahoe.structures.Household;
import com.pb.tahoe.structures.SubModeType;
import com.pb.tahoe.structures.SubTourType;
import com.pb.tahoe.structures.TourModeType;
import com.pb.tahoe.structures.TourType;
import com.pb.tahoe.util.ChoiceModelApplication;
import com.pb.tahoe.util.DataWriter;
import org.apache.log4j.Logger;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.ResourceBundle;

public class StopsModelBase {

    protected static Logger logger = Logger.getLogger(StopsModelBase.class);
    protected ResourceBundle rb;

    //USED FOR INDEX VALUES
    public static int OB = 0;
    public static int IB = 1;
    public static int AUTO = 0;
    public static int TRANSIT = 1;
    public static int IK_LEG = 1;
    public static int KJ_LEG = 2;
    public static int JK_LEG = 3;
    public static int KI_LEG = 4;

    //STOP-FREQUENCY
    protected int stopFrequencyModelSheet  = 0;
    protected int stopFrequencyDataSheet  = 0;
    protected ChoiceModelApplication sfc;
    protected int[] sfcSample;   //indexed by alternative number
    protected boolean[] sfcAvailability;    //indexed by alternative number

    //STOP-LOCATION
    //Index description: [OB-IB][AUTO-TRANSIT][TourTypes]
    protected int[][][] stopLocationModelSheet = new int[2][2][TourType.TYPES+1];
    protected int stopLocationDataSheet  = 0;
    //Index description: [OB-IB][AUTO-TRANSIT][TourTypes]
    protected ChoiceModelApplication slc[][][];
    UtilityExpressionCalculator[][][] slcUEC;
    //Index description: [OB-IB][altNumber] - all alternatives are in our sample (different from MORPC)
     protected int[][] slcSample = new int[2][];
     protected boolean[] slcOBAvailability;          //could be combined into a single availability
    protected boolean[] slcIBAvailability;            //array indexed by [OB-IB]

    //Index description: [OB-IB]
    public static float[] slcLogsum = new float[2];

    //OTHER FIELDS
    protected short tourTypeCategory;
    protected short[] tourTypes;

    //OUTPUT FILES
    public PrintWriter freqOutputStream;
    public PrintWriter locOutputStream;
    public PrintWriter modeOutputStream;



    // this constructor used by a non-distributed application
    public StopsModelBase ( ResourceBundle propertyMap, short tourTypeCategory, short[] tourTypes ) {

        initStopsModelBase ( propertyMap, tourTypeCategory, tourTypes );

    }

    public void initStopsModelBase ( ResourceBundle propertyMap, short tourTypeCategory, short[] tourTypes ) {
        logger.info( "Initializing stops model base for category " + tourTypeCategory);

        this.rb = propertyMap;
        this.tourTypeCategory = tourTypeCategory;
        this.tourTypes = tourTypes;

        // create choice model objects and UECs for stop frequency and stop location choices, inbound and outbound.
        slcUEC   = new UtilityExpressionCalculator[2][2][TourType.TYPES+1];
        slc   = new ChoiceModelApplication[2][2][TourType.TYPES+1];


        defineUECModelSheets (tourTypeCategory);

        sfc =  new ChoiceModelApplication("stops.frequency.choice.control.file",
                TourType.getCategoryLabelForCategory(tourTypeCategory).toLowerCase() + ".stops.frequency.output.file", propertyMap);
        UtilityExpressionCalculator sfcUEC = sfc.getUEC(stopFrequencyModelSheet, stopFrequencyDataSheet);
        sfc.createLogitModel();
        int numSfcAlternatives = sfcUEC.getNumberOfAlternatives();

        int numSlcAlternatives = 0;
        for (int i=0; i < 2; i++) {  //inbound, outbound
            for (int j=0; j < 2; j++) {  //ma or mt  (auto or transit)

                if (tourTypeCategory != TourType.AT_WORK_CATEGORY) {
                    for (short tourType1 : tourTypes) {

                        slc[i][j][tourType1] = new ChoiceModelApplication("stops.location.choice.control.file",
                                TourType.getCategoryLabelForCategory(tourTypeCategory).toLowerCase() + ".stops.location..output.file", propertyMap);
                        slcUEC[i][j][tourType1] = slc[i][j][tourType1].getUEC(stopLocationModelSheet[i][j][tourType1], stopLocationDataSheet);
                        slc[i][j][tourType1].createLogitModel();

                        numSlcAlternatives = slcUEC[i][j][tourType1].getNumberOfAlternatives();

                    }
                }
                else {
                    for (short aSUB_TOUR_TYPES : SubTourType.SUB_TOUR_TYPES) {

                        slc[i][j][aSUB_TOUR_TYPES] = new ChoiceModelApplication("stops.location.choice.control.file",
                                TourType.getCategoryLabelForCategory(tourTypeCategory).toLowerCase() + ".stops.location.output.file", propertyMap);
                        slcUEC[i][j][aSUB_TOUR_TYPES] = slc[i][j][aSUB_TOUR_TYPES].getUEC(stopLocationModelSheet[i][j][aSUB_TOUR_TYPES], stopLocationDataSheet);
                        slc[i][j][aSUB_TOUR_TYPES].createLogitModel();

                        numSlcAlternatives = slcUEC[i][j][aSUB_TOUR_TYPES].getNumberOfAlternatives();

                    }
                }
            }
        }



        sfcAvailability = new boolean[numSfcAlternatives+1];
        slcOBAvailability = new boolean[numSlcAlternatives+1];
        slcIBAvailability = new boolean[numSlcAlternatives+1];

        slcSample[OB] = new int[numSlcAlternatives+1];
        slcSample[IB] = new int[numSlcAlternatives+1];
        sfcSample = new int[numSfcAlternatives+1];
        Arrays.fill ( slcSample[OB], 1 );
        Arrays.fill ( slcSample[IB], 1 );
        Arrays.fill ( sfcSample, 1 );

        String freqFileName = TourType.getCategoryLabelForCategory(tourTypeCategory).toLowerCase() + ".stops.frequency.output.file";
        File freqFile = new File(propertyMap.getString(freqFileName));
        String[] freqTableHeadings = {DataWriter.HHID_FIELD, "TourID", "TourPurpose", "FreqChoice"};
        try {
            freqOutputStream = new PrintWriter(new BufferedWriter(new FileWriter(freqFile)));
            writeRecord(freqOutputStream, freqTableHeadings);
        } catch (IOException e) {
            throw new RuntimeException("Could not write to file " + freqFileName);
        }

        String locFileName = TourType.getCategoryLabelForCategory(tourTypeCategory).toLowerCase() + ".stops.location.output.file";
        File locFile = new File(propertyMap.getString(locFileName));
        String[] locTableHeadings = {DataWriter.HHID_FIELD, "TourID", "TourPurpose", "OB_start_taz", "OB_stop_taz", "IB_start_taz", "IB_stop_taz"};
        try {
            locOutputStream = new PrintWriter(new BufferedWriter(new FileWriter(locFile)));
            writeRecord(locOutputStream, locTableHeadings);
        } catch (IOException e) {
            throw new RuntimeException("Could not write to file " + locFileName);
        }

        String modeFileName = TourType.getCategoryLabelForCategory(tourTypeCategory).toLowerCase() + ".stops.mode.output.file";
        File modeFile = new File(propertyMap.getString(modeFileName));
        String[] modeTableHeadings = {DataWriter.HHID_FIELD, "TourID", "IJ_mode", "IK_mode", "KJ_mode", "JK_mode", "KI_mode"};
        try {
            modeOutputStream = new PrintWriter(new BufferedWriter(new FileWriter(modeFile)));
            writeRecord(modeOutputStream, modeTableHeadings);
        } catch (IOException e) {
            throw new RuntimeException("Could not write to file " + modeFileName);
        }

    }



    protected float getSlcOBLogsums ( Household hh, int mode, int tourType ) {

        slc[OB][mode][tourType].updateLogitModel ( hh, slcOBAvailability, slcSample[OB] );

        return (float)slc[OB][mode][tourType].getLogsum ();
    }


    protected float getSlcIBLogsums ( Household hh, int mode, int tourType ) {

        slc[IB][mode][tourType].updateLogitModel ( hh, slcIBAvailability, slcSample[IB] );

        return (float)slc[IB][mode][tourType].getLogsum ();
    }


    private void defineUECModelSheets (int tourCategory) {

        final int STOP_FREQ_MANDATORY_MODEL_SHEET = 1;
        final int STOP_FREQ_NON_MANDATORY_MODEL_SHEET = 2;
        final int STOP_FREQ_JOINT_MODEL_SHEET = 3;
        final int STOP_FREQ_ATWORK_MODEL_SHEET = 4;


        // assign the model sheet numbers for the stop frequency choice sheets
        stopFrequencyDataSheet = 0;
        stopLocationDataSheet = 0;
        if (tourCategory == TourType.MANDATORY_CATEGORY)
            stopFrequencyModelSheet  = STOP_FREQ_MANDATORY_MODEL_SHEET;
        else if (tourCategory == TourType.NON_MANDATORY_CATEGORY)
            stopFrequencyModelSheet  = STOP_FREQ_NON_MANDATORY_MODEL_SHEET;
        else if (tourCategory == TourType.JOINT_CATEGORY)
            stopFrequencyModelSheet  = STOP_FREQ_JOINT_MODEL_SHEET;
        else if (tourCategory == TourType.AT_WORK_CATEGORY)
            stopFrequencyModelSheet  = STOP_FREQ_ATWORK_MODEL_SHEET;


        if (tourCategory == TourType.MANDATORY_CATEGORY) {
            //outbound
            stopLocationModelSheet[OB][AUTO][TourType.WORK] = 1;
            stopLocationModelSheet[OB][TRANSIT][TourType.WORK] = 2;
            stopLocationModelSheet[OB][AUTO][TourType.SCHOOL] = 3;
            stopLocationModelSheet[OB][TRANSIT][TourType.SCHOOL] = 4;
            //inbound
            stopLocationModelSheet[IB][AUTO][TourType.WORK] = 29;
            stopLocationModelSheet[IB][TRANSIT][TourType.WORK] = 30;
            stopLocationModelSheet[IB][AUTO][TourType.SCHOOL] = 31;
            stopLocationModelSheet[IB][TRANSIT][TourType.SCHOOL] = 32;
        }
        else if (tourCategory == TourType.NON_MANDATORY_CATEGORY) {
            stopLocationModelSheet[OB][AUTO][TourType.ESCORTING] = 5;
            stopLocationModelSheet[OB][TRANSIT][TourType.ESCORTING] = 6;
            stopLocationModelSheet[OB][AUTO][TourType.SHOP] = 7;
            stopLocationModelSheet[OB][TRANSIT][TourType.SHOP] = 8;
            stopLocationModelSheet[OB][AUTO][TourType.OTHER_MAINTENANCE] = 9;
            stopLocationModelSheet[OB][TRANSIT][TourType.OTHER_MAINTENANCE] = 10;
            stopLocationModelSheet[OB][AUTO][TourType.DISCRETIONARY] = 11;
            stopLocationModelSheet[OB][TRANSIT][TourType.DISCRETIONARY] = 12;
            stopLocationModelSheet[OB][AUTO][TourType.EAT] = 13;
            stopLocationModelSheet[OB][TRANSIT][TourType.EAT] = 14;
            stopLocationModelSheet[IB][AUTO][TourType.ESCORTING] = 33;
            stopLocationModelSheet[IB][TRANSIT][TourType.ESCORTING] = 34;
            stopLocationModelSheet[IB][AUTO][TourType.SHOP] = 35;
            stopLocationModelSheet[IB][TRANSIT][TourType.SHOP] = 36;
            stopLocationModelSheet[IB][AUTO][TourType.OTHER_MAINTENANCE] = 37;
            stopLocationModelSheet[IB][TRANSIT][TourType.OTHER_MAINTENANCE] = 38;
            stopLocationModelSheet[IB][AUTO][TourType.DISCRETIONARY] = 39;
            stopLocationModelSheet[IB][TRANSIT][TourType.DISCRETIONARY] = 40;
            stopLocationModelSheet[IB][AUTO][TourType.EAT] = 41;
            stopLocationModelSheet[IB][TRANSIT][TourType.EAT] = 42;
        }
        else if (tourCategory == TourType.JOINT_CATEGORY) {
            stopLocationModelSheet[OB][AUTO][TourType.SHOP] = 15;
            stopLocationModelSheet[OB][TRANSIT][TourType.SHOP] = 16;
            stopLocationModelSheet[OB][AUTO][TourType.OTHER_MAINTENANCE] = 17;
            stopLocationModelSheet[OB][TRANSIT][TourType.OTHER_MAINTENANCE] = 18;
            stopLocationModelSheet[OB][AUTO][TourType.DISCRETIONARY] = 19;
            stopLocationModelSheet[OB][TRANSIT][TourType.DISCRETIONARY] = 20;
            stopLocationModelSheet[OB][AUTO][TourType.EAT] = 21;
            stopLocationModelSheet[OB][TRANSIT][TourType.EAT] = 22;
            stopLocationModelSheet[IB][AUTO][TourType.SHOP] = 43;
            stopLocationModelSheet[IB][TRANSIT][TourType.SHOP] = 44;
            stopLocationModelSheet[IB][AUTO][TourType.OTHER_MAINTENANCE] = 45;
            stopLocationModelSheet[IB][TRANSIT][TourType.OTHER_MAINTENANCE] = 46;
            stopLocationModelSheet[IB][AUTO][TourType.DISCRETIONARY] = 47;
            stopLocationModelSheet[IB][TRANSIT][TourType.DISCRETIONARY] = 48;
            stopLocationModelSheet[IB][AUTO][TourType.EAT] = 49;
            stopLocationModelSheet[IB][TRANSIT][TourType.EAT] = 50;
        }
        else if (tourCategory == TourType.AT_WORK_CATEGORY) {
            stopLocationModelSheet[OB][AUTO][SubTourType.WORK] = 23;
            stopLocationModelSheet[OB][TRANSIT][SubTourType.WORK] = 24;
            stopLocationModelSheet[OB][AUTO][SubTourType.OTHER] = 25;
            stopLocationModelSheet[OB][TRANSIT][SubTourType.OTHER] = 26;
            stopLocationModelSheet[OB][AUTO][SubTourType.EAT] = 27;
            stopLocationModelSheet[OB][TRANSIT][SubTourType.EAT] = 28;
            stopLocationModelSheet[IB][AUTO][SubTourType.WORK] = 51;
            stopLocationModelSheet[IB][TRANSIT][SubTourType.WORK] = 52;
            stopLocationModelSheet[IB][AUTO][SubTourType.OTHER] = 53;
            stopLocationModelSheet[IB][TRANSIT][SubTourType.OTHER] = 54;
            stopLocationModelSheet[IB][AUTO][SubTourType.EAT] = 55;
            stopLocationModelSheet[IB][TRANSIT][SubTourType.EAT] = 56;
        }

    }

    public int getStopMode ( Household hh, int tourMode, int tourSubmode, double[] submodeUtility ) {

        int legMode = 0;

        if ( (tourMode == TourModeType.WALKTRANSIT) || (tourMode==TourModeType.DRIVETRANSIT)) {

            if (submodeUtility[0] == 0) {

                legMode = SubModeType.LBS.ordinal();

            } else {

                legMode = TourModeType.NONMOTORIZED;

            }

        } else {

            legMode = tourMode;
        }

        return legMode;

    }


    public void writeRecord(PrintWriter outStream, float[] record) {
        for (int i = 0; i < record.length; i++) {
            if (i != 0) {
                outStream.print(",");
            }

            outStream.print(record[i]);
        }

        outStream.println();
    }

    public void writeRecord(PrintWriter outStream, String[] record) {
        for (int i = 0; i < record.length; i++) {
            if (i != 0) {
                outStream.print(",");
            }

            outStream.print(record[i]);
        }

        outStream.println();
    }




}
