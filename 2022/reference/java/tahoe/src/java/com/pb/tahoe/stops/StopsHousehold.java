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

import com.pb.common.calculator.IndexValues;
import com.pb.common.calculator.UtilityExpressionCalculator;
import com.pb.tahoe.structures.Household;
import com.pb.tahoe.structures.TourModeType;
import com.pb.tahoe.structures.TourType;
import com.pb.tahoe.util.TODDataManager;
import com.pb.tahoe.util.ZonalDataManager;

import java.io.File;
import java.util.Arrays;
import java.util.ResourceBundle;

/**
 * StopsHousehold is a class that contains the methods for choosing
 * stop location frequency, location and trip modes for the mandatory,
 * joint, non-mandatory and at-work stops.
 *
 * @author Christi Willison
 * @version 1.0,  Oct 6, 2006
 */
public class StopsHousehold extends StopsModelBase {

    long logsumTime = 0;
    long freqTime = 0;
    long locTime = 0;
    long mcTime = 0;

    private int[] smcSample = null;   //OB-IB

    UtilityExpressionCalculator smcUEC;

    protected IndexValues index = new IndexValues();

    protected int hh_id;
    protected int hh_taz_id;
    protected int person;
    protected int tourType;

    float[] freqResultsRecord = new float[4];
    float[] locResultsRecord = new float[7];
    float[] modeResultsRecord = new float[7];





    public StopsHousehold ( ResourceBundle propertyMap, short tourTypeCategory, short[] tourTypes ) {

        super ( propertyMap, tourTypeCategory, tourTypes );


        // initialize the availabilty for each alternative to true for every household
        Arrays.fill(sfcAvailability, true);
        Arrays.fill(slcOBAvailability, true);
        Arrays.fill(slcIBAvailability, true);

        smcSample = new int[2];

        smcSample[0] = 1;
        smcSample[1] = 1;

        smcUEC = new UtilityExpressionCalculator(new File(propertyMap.getString( "stops.mode.choice.control.file") ),1, 0, this.rb, Household.class);

    }


    public void mandatoryTourSfcSlc ( Household hh ) {



        hh_id     = hh.getID();



        // get the array of mandatory tours for this household.
        if (hh.getMandatoryTours() == null)
            return;

        hh.setTourCategory( TourType.MANDATORY_CATEGORY );
        hh_taz_id = hh.getTazID();


        // loop over individual mandatory tours for the hh
        for (int t=0; t < hh.mandatoryTours.length; t++) {
            // if the primary mode for this mandatory tour is non-motorized or school bus, skip stop freq choice
            if (TourModeType.isNonmotor(hh.mandatoryTours[t].getMode()) ||
                hh.mandatoryTours[t].getMode() == TourModeType.SCHOOLBUS) {
                continue;
            }

            hh.setOrigTaz ( hh.mandatoryTours[t].getOrigTaz() );
            hh.setChosenDest ( hh.mandatoryTours[t].getDestTaz() );
            hh.setChosenWalkSegment(hh.mandatoryTours[t].getDestShrtWlk());

            person = hh.mandatoryTours[t].getTourPerson();
            hh.setPersonID ( person );
            hh.setTourID ( t );
            index.setOriginZone( hh.mandatoryTours[t].getOrigTaz() );
            index.setDestZone( hh.mandatoryTours[t].getDestTaz() );

            tourType = hh.mandatoryTours[t].getTourType();

            int autoTransit = TourModeType.isAuto(hh.mandatoryTours[t].getMode()) ? 0 : 1;

            setStopLocationAvailabilities(autoTransit, OB);

            // calculate stop location choice logsum for outbound halftours for the hh
            long markTime = System.currentTimeMillis();
            slc[OB][autoTransit][tourType].updateLogitModel ( hh, slcOBAvailability, slcSample[OB] );
            slcLogsum[OB] = (float)(slc[OB][autoTransit][tourType].getLogsum ());
            logsumTime += (System.currentTimeMillis()-markTime);



            // compute stop frequency choice proportions and choose alternative
            markTime = System.currentTimeMillis();
            sfc.updateLogitModel ( hh, sfcAvailability, sfcSample );
            int chosenAlt = sfc.getChoiceResult();
            // set the chosen value in tour objects
            hh.mandatoryTours[t].setStopFreqAlt (chosenAlt);
            freqTime += (System.currentTimeMillis()-markTime);



            if (chosenAlt ==3 || chosenAlt == 4) {   //means you have an inbound stop so you have to calculate those avails.
                setStopLocationAvailabilities(autoTransit, IB);
            }


            int chosen = 0;
            int chosenDestAlt = 0;
            int chosenShrtWlk = 0;

            markTime = System.currentTimeMillis();

            // determine the stop locations if the tour has stops
            switch (chosenAlt) {     //this is the stop frequency alternative

                // no stops for this tour
                case 1:

                    hh.mandatoryTours[t].setStopLocOB ( 0 );
                    hh.mandatoryTours[t].setStopLocIB ( 0 );

                    break;

                // 1 outbound, 0 inbound
                case 2:

                    // compute destination choice proportions and choose alternative
                    slc[OB][autoTransit][tourType].updateLogitModel ( hh, slcOBAvailability, slcSample[OB] );
                    if ( slc[OB][autoTransit][tourType].getAvailabilityCount() > 0 ) {
                        chosen = slc[OB][autoTransit][tourType].getChoiceResult();
                        chosenDestAlt = (int) slcUEC[OB][autoTransit][tourType].getAlternativeData().getIndexedValueAt(chosen, 2);
                        chosenShrtWlk = (int) slcUEC[OB][autoTransit][tourType].getAlternativeData().getIndexedValueAt(chosen, 3);
                    }
                    else {
                        logger.warn ( "no outbound mandatory slc alternatives available, hh_id=" + hh_id + ", person=" + person + ", tour=" + t + ".  Stop frequency for this tour set to 0.");
                        hh.mandatoryTours[t].setStopFreqAlt ( 1 );
                        hh.mandatoryTours[t].setStopLocOB ( 0 );
                        hh.mandatoryTours[t].setStopLocIB ( 0 );
                        break;
                    }

                    // set the chosen value in hh tour objects
                    hh.mandatoryTours[t].setStopLocOB (chosenDestAlt);
                    hh.mandatoryTours[t].setStopLocSubzoneOB (chosenShrtWlk);
                    hh.mandatoryTours[t].setStopLocIB ( 0 );
                    locTime += (System.currentTimeMillis() - markTime);

                    break;

                // 0 outbound, 1 inbound
                case 3:

                    // compute destination choice proportions and choose alternative
                    slc[IB][autoTransit][tourType].updateLogitModel ( hh, slcIBAvailability, slcSample[1] );
                    if ( slc[IB][autoTransit][tourType].getAvailabilityCount() > 0 ) {
                        chosen = slc[IB][autoTransit][tourType].getChoiceResult();
                        chosenDestAlt = (int) slcUEC[IB][autoTransit][tourType].getAlternativeData().getIndexedValueAt(chosen, 2);
                        chosenShrtWlk = (int) slcUEC[IB][autoTransit][tourType].getAlternativeData().getIndexedValueAt(chosen, 3);
                    }
                    else {
                        logger.warn ( "no inbound mandatory slc alternatives available, hh_id=" + hh_id + ", person=" + person + ", tour=" + t + ".  Stop frequency for this tour set to 0.");
                        hh.mandatoryTours[t].setStopFreqAlt ( 1 );
                        hh.mandatoryTours[t].setStopLocOB ( 0 );
                        hh.mandatoryTours[t].setStopLocIB ( 0 );
                        break;
                    }


                    // set the chosen value in hh tour objects
                    hh.mandatoryTours[t].setStopLocIB (chosenDestAlt);
                    hh.mandatoryTours[t].setStopLocSubzoneIB (chosenShrtWlk);
                    hh.mandatoryTours[t].setStopLocOB ( 0 );
                    locTime += (System.currentTimeMillis() - markTime);

                    break;

                // 1 outbound, 1 inbound
                case 4:

                    // compute destination choice proportions and choose alternative
                    slc[OB][autoTransit][tourType].updateLogitModel ( hh, slcOBAvailability, slcSample[0] );
                    if ( slc[OB][autoTransit][tourType].getAvailabilityCount() > 0 ) {
                        chosen = slc[OB][autoTransit][tourType].getChoiceResult();
                        chosenDestAlt = (int) slcUEC[OB][autoTransit][tourType].getAlternativeData().getIndexedValueAt(chosen, 2);
                        chosenShrtWlk = (int) slcUEC[OB][autoTransit][tourType].getAlternativeData().getIndexedValueAt(chosen, 3);
                    }
                    else {
                        logger.warn ( "no outbound mandatory slc alternatives available, hh_id=" + hh_id + ", person=" + person + ", tour=" + t + ".  Stop frequency for this tour set to 0.");
                        hh.mandatoryTours[t].setStopFreqAlt ( 1 );
                        hh.mandatoryTours[t].setStopLocOB ( 0 );
                        hh.mandatoryTours[t].setStopLocIB ( 0 );
                        break;
                    }

                    // set the chosen value in hh tour objects
                    hh.mandatoryTours[t].setStopLocOB (chosenDestAlt);
                    hh.mandatoryTours[t].setStopLocSubzoneOB (chosenShrtWlk);

                    slc[IB][autoTransit][tourType].updateLogitModel ( hh, slcIBAvailability, slcSample[1] );
                    if ( slc[IB][autoTransit][tourType].getAvailabilityCount() > 0 ) {
                        chosen = slc[IB][autoTransit][tourType].getChoiceResult();
                        chosenDestAlt = (int) slcUEC[IB][autoTransit][tourType].getAlternativeData().getIndexedValueAt(chosen, 2);
                        chosenShrtWlk = (int) slcUEC[IB][autoTransit][tourType].getAlternativeData().getIndexedValueAt(chosen, 3);
                    }
                    else {
                        logger.warn ( "no inbound mandatory slc alternatives available, hh_id=" + hh_id + ", person=" + person + ", tour=" + t + ".  Stop frequency for this tour set to 0.");
                        hh.mandatoryTours[t].setStopFreqAlt ( 1 );
                        hh.mandatoryTours[t].setStopLocOB ( 0 );
                        hh.mandatoryTours[t].setStopLocIB ( 0 );
                        break;
                    }

                    // set the chosen value in hh tour objects
                    hh.mandatoryTours[t].setStopLocIB (chosenDestAlt);
                    hh.mandatoryTours[t].setStopLocSubzoneIB (chosenShrtWlk);
                    locTime += (System.currentTimeMillis() - markTime);

                    break;

                default:

                    logger.error ("invalid mandatory stop frequency choice = " + chosenAlt + " for household id=" + hh.getID() + " in non-mandatory stop frequency choice." );
                    break;
            }

            freqResultsRecord[0] = hh.getID();
            locResultsRecord[0] = hh.getID();

            freqResultsRecord[1] = t;              //tourID
            locResultsRecord[1] = t;                //tourID

            freqResultsRecord[2] = hh.mandatoryTours[t].getTourType();              //tour purpose
            locResultsRecord[2] = hh.mandatoryTours[t].getTourType();                //tour purpose

            freqResultsRecord[3] = hh.mandatoryTours[t].getStopFreqAlt();

            locResultsRecord[3] = hh.mandatoryTours[t].getOrigTaz();
            locResultsRecord[4] = hh.mandatoryTours[t].getStopLocOB();
            locResultsRecord[5] = hh.mandatoryTours[t].getDestTaz();
            locResultsRecord[6] = hh.mandatoryTours[t].getStopLocIB();

            writeRecord(freqOutputStream, freqResultsRecord);
            writeRecord(locOutputStream, locResultsRecord);


        }

    }




    public void mandatoryTourSmc ( Household hh ) {

        hh_id     = hh.getID();

        // get the array of mandatory tours for this household.
        if (hh.getMandatoryTours() == null)
            return;

        // loop over individual mandatory tours for the hh
        for (int t=0; t < hh.mandatoryTours.length; t++) {

            // if the primary mode for this mandatory tour is non-motorized or school bus, skip stop mode choice
            if (TourModeType.isNonmotor(hh.mandatoryTours[t].getMode()) ||
                hh.mandatoryTours[t].getMode() == TourModeType.SCHOOLBUS) {

                    continue;
            }

            index.setOriginZone( hh.mandatoryTours[t].getOrigTaz() );
            index.setDestZone( hh.mandatoryTours[t].getDestTaz() );
            // determine the half tour segment mode choices if the tour has stops
            long markTime = System.currentTimeMillis();
            switch ( hh.mandatoryTours[t].getStopFreqAlt() ) {

                // no stops for this tour
                case 1:

                    break;

                // 1 outbound, 0 inbound
                case 2:

                    index.setStopZone( hh.mandatoryTours[t].getStopLocOB() );
                    hh.setChosenStartSkimPeriod(TODDataManager.getTodStartPeriod( hh.mandatoryTours[t].getTimeOfDayAlt()));
                    hh.setStopLeg(IK_LEG);
                    double[] submodeUtility = smcUEC.solve(index, hh, smcSample);
                    hh.mandatoryTours[t].setTripIkMode ( getStopMode ( hh, hh.mandatoryTours[t].getMode(), hh.mandatoryTours[t].getSubmodeOB(), submodeUtility ) );

                    // set outbound (kj) submode utilities (returns a string of 0s and 1s
                    // indicating which submodes are available).
                    hh.setStopLeg(KJ_LEG);
                    submodeUtility = smcUEC.solve( index, hh, smcSample );
                    hh.mandatoryTours[t].setTripKjMode ( getStopMode ( hh, hh.mandatoryTours[t].getMode(), hh.mandatoryTours[t].getSubmodeOB(), submodeUtility ) );
                    mcTime += (System.currentTimeMillis() - markTime);

                    break;

                // 0 outbound, 1 inbound
                case 3:

                    index.setStopZone( hh.mandatoryTours[t].getStopLocIB() );
                    hh.setChosenStartSkimPeriod(TODDataManager.getTodEndPeriod( hh.mandatoryTours[t].getTimeOfDayAlt()));
                    hh.setStopLeg(JK_LEG);
                    // set the mode for each portion of the inbound half-tour
                    // set inbound (jk) submode utilities (returns a string of 0s and 1s
                    // indicating which submodes are available).
                    submodeUtility = smcUEC.solve( index, hh, smcSample );
                    hh.mandatoryTours[t].setTripJkMode ( getStopMode ( hh, hh.mandatoryTours[t].getMode(), hh.mandatoryTours[t].getSubmodeIB(), submodeUtility ) );

                    hh.setStopLeg(KI_LEG);
                    submodeUtility = smcUEC.solve( index, hh, smcSample );
                    hh.mandatoryTours[t].setTripKiMode ( getStopMode ( hh, hh.mandatoryTours[t].getMode(), hh.mandatoryTours[t].getSubmodeIB(), submodeUtility ) );
                    mcTime += (System.currentTimeMillis() - markTime);

                    break;

                // 1 outbound, 1 inbound
                case 4:

                    index.setStopZone( hh.mandatoryTours[t].getStopLocOB() );
                    hh.setChosenStartSkimPeriod(TODDataManager.getTodStartPeriod( hh.mandatoryTours[t].getTimeOfDayAlt()));
                    hh.setStopLeg(IK_LEG);
                    // set the mode for each portion of the outbound half-tour
                    submodeUtility = smcUEC.solve( index, hh, smcSample );
                    hh.mandatoryTours[t].setTripIkMode ( getStopMode ( hh, hh.mandatoryTours[t].getMode(), hh.mandatoryTours[t].getSubmodeOB(), submodeUtility ) );

                    // set outbound (kj) submode utilities (returns a string of 0s and 1s
                    // indicating which submodes are available).
                    hh.setStopLeg(KJ_LEG);
                    submodeUtility = smcUEC.solve( index, hh, smcSample );
                    hh.mandatoryTours[t].setTripKjMode ( getStopMode ( hh, hh.mandatoryTours[t].getMode(), hh.mandatoryTours[t].getSubmodeOB(), submodeUtility ) );


                    index.setStopZone( hh.mandatoryTours[t].getStopLocIB() );
                    hh.setChosenStartSkimPeriod(TODDataManager.getTodEndPeriod( hh.mandatoryTours[t].getTimeOfDayAlt()));
                    hh.setStopLeg(JK_LEG);
                    // set the mode for each portion of the inbound half-tour
                    // set inbound (jk) submode utilities (returns a string of 0s and 1s
                    // indicating which submodes are available).
                    submodeUtility = smcUEC.solve( index, hh, smcSample );
                    hh.mandatoryTours[t].setTripJkMode ( getStopMode ( hh, hh.mandatoryTours[t].getMode(), hh.mandatoryTours[t].getSubmodeIB(), submodeUtility ) );

                    // set inbound (ki) submode utilities (returns a string of 0s and 1s
                    // indicating which submodes are available).
                    hh.setStopLeg(KI_LEG);
                    submodeUtility = smcUEC.solve( index, hh, smcSample );
                    hh.mandatoryTours[t].setTripKiMode ( getStopMode ( hh, hh.mandatoryTours[t].getMode(), hh.mandatoryTours[t].getSubmodeIB(), submodeUtility ) );
                    mcTime += (System.currentTimeMillis() - markTime);


                    break;

            }

            modeResultsRecord[0] = hh.getID();
            modeResultsRecord[1] = t;
            modeResultsRecord[2] = hh.mandatoryTours[t].getMode();
            modeResultsRecord[3] = hh.mandatoryTours[t].getTripIkMode();
            modeResultsRecord[4] = hh.mandatoryTours[t].getTripKjMode();
            modeResultsRecord[5] = hh.mandatoryTours[t].getTripJkMode();
            modeResultsRecord[6] = hh.mandatoryTours[t].getTripKiMode();

            writeRecord(modeOutputStream, modeResultsRecord);

        }

    }




    public void jointTourSfcSlc ( Household hh ) {

        long markTime=0;

        hh_id     = hh.getID();

        // get the array of joint tours for this household.
        if (hh.getJointTours() == null)
            return;

        hh.setTourCategory( TourType.JOINT_CATEGORY );
        hh_taz_id = hh.getTazID();

        // loop over joint tours for the hh
        for (int t=0; t < hh.jointTours.length; t++) {

            hh.setOrigTaz ( hh.jointTours[t].getOrigTaz() );
            hh.setChosenDest ( hh.jointTours[t].getDestTaz() );
            hh.setChosenWalkSegment(hh.jointTours[t].getDestShrtWlk());

            index.setOriginZone( hh.jointTours[t].getOrigTaz() );
            index.setDestZone( hh.jointTours[t].getDestTaz() );

            // if the primary mode for this joint tour is non-motorized, skip stop freq choice
            if (TourModeType.isNonmotor(hh.jointTours[t].getMode())) {
                continue;
            }


            tourType = hh.jointTours[t].getTourType();

            person = hh.jointTours[t].getTourPerson();

            hh.setOrigTaz (hh_taz_id);
            hh.setPersonID ( person );
            hh.setTourID ( t );

            int autoTransit = TourModeType.isAuto(hh.jointTours[t].getMode() ) ? 0 : 1;

            //need to set inbound availabilities so that the inbound logsum
            //can be calculated as it is used in the utility expression for joint stop-frequency.
            setStopLocationAvailabilities(autoTransit, IB);

            // calculate inbound stop location choice logsum
            markTime = System.currentTimeMillis();

            // calculate stop location choice logsum for outbound halftours for the hh
            slc[1][autoTransit][tourType].updateLogitModel ( hh, slcIBAvailability, slcSample[1] );
            slcLogsum[1] = (float)(slc[1][autoTransit][tourType].getLogsum ());

            logsumTime += (System.currentTimeMillis()-markTime);



            // compute stop frequency choice proportions and choose alternative
            markTime = System.currentTimeMillis();
            sfc.updateLogitModel ( hh, sfcAvailability, sfcSample );
            int chosenAlt = sfc.getChoiceResult();
            // set the chosen value in hh tour objects
            hh.jointTours[t].setStopFreqAlt (chosenAlt);
            freqTime += (System.currentTimeMillis()-markTime);


            markTime = System.currentTimeMillis();
            // if chosen stop frequency is 2 or 4, calculate outbound soa and availabilities
            if ( chosenAlt == 2 || chosenAlt == 4 ) {     //no need to do this if no-stops (alt=1) or 1-inbound (alt 3) is chosen.
                setStopLocationAvailabilities(autoTransit, OB);
            }

            int chosen = 0;
            int chosenDestAlt = 0;
            int chosenShrtWlk = 0;
            // use distance UEC to get utilities(distances) for each tour and trip segment
            // determine the stop locations if the tour has stops
            switch (chosenAlt) {

                // no stops for this tour
                case 1:

                    hh.jointTours[t].setStopLocOB ( 0 );
                    hh.jointTours[t].setStopLocIB ( 0 );

                    break;

                // 1 outbound, 0 inbound
                case 2:

                    // compute destination choice proportions and choose alternative
                    markTime = System.currentTimeMillis();
                    slc[0][autoTransit][tourType].updateLogitModel ( hh, slcOBAvailability, slcSample[0] );
                    if ( slc[0][autoTransit][tourType].getAvailabilityCount() > 0 ) {
                        chosen = slc[0][autoTransit][tourType].getChoiceResult();
                        chosenDestAlt = (int) slcUEC[OB][autoTransit][tourType].getAlternativeData().getIndexedValueAt(chosen, 2);
                        chosenShrtWlk = (int) slcUEC[OB][autoTransit][tourType].getAlternativeData().getIndexedValueAt(chosen, 3);

                        //chosenDestAlt = (int)((chosen-1)/ZonalDataManager.WALK_SEGMENTS) + 1;
                        //chosenShrtWlk = chosen - (chosenDestAlt-1)*ZonalDataManager.WALK_SEGMENTS - 1;
                    }
                    else {
                        logger.warn ( "no outbound joint slc alternatives available, hh_id=" + hh_id + ", person=" + person + ", tour=" + t);
                        hh.jointTours[t].setStopFreqAlt ( 1 );
                        hh.jointTours[t].setStopLocOB ( 0 );
                        hh.jointTours[t].setStopLocIB ( 0 );
//						hh.writeContentToLogger(logger);
                        break;
                    }



                    // set the chosen value in hh tour objects
                    hh.jointTours[t].setStopLocOB (chosenDestAlt);
                    hh.jointTours[t].setStopLocSubzoneOB (chosenShrtWlk);
                    hh.jointTours[t].setStopLocIB (0);
                    locTime += (System.currentTimeMillis() - markTime);

                    break;

                // 0 outbound, 1 inbound
                case 3:

                    // compute destination choice proportions and choose alternative
                    markTime = System.currentTimeMillis();
                    slc[1][autoTransit][tourType].updateLogitModel ( hh, slcIBAvailability, slcSample[1] );
                    if ( slc[1][autoTransit][tourType].getAvailabilityCount() > 0 ) {
                        chosen = slc[1][autoTransit][tourType].getChoiceResult();
                        chosenDestAlt = (int) slcUEC[IB][autoTransit][tourType].getAlternativeData().getIndexedValueAt(chosen, 2);
                        chosenShrtWlk = (int) slcUEC[IB][autoTransit][tourType].getAlternativeData().getIndexedValueAt(chosen, 3);
                    }
                    else {
                        logger.warn ( "no inbound joint slc alternatives available, hh_id=" + hh_id + ", person=" + person + ", tour=" + t);
                        hh.jointTours[t].setStopFreqAlt ( 1 );
                        hh.jointTours[t].setStopLocOB ( 0 );
                        hh.jointTours[t].setStopLocIB ( 0 );
//						hh.writeContentToLogger(logger);
                        break;
                    }



                    // set the chosen value in hh tour objects
                    hh.jointTours[t].setStopLocIB (chosenDestAlt);
                    hh.jointTours[t].setStopLocSubzoneIB (chosenShrtWlk);
                    hh.jointTours[t].setStopLocOB (0);
                    locTime += (System.currentTimeMillis() - markTime);

                    break;

                // 1 outbound, 1 inbound
                case 4:

                    // compute destination choice proportions and choose alternative
                    markTime = System.currentTimeMillis();
                    slc[0][autoTransit][tourType].updateLogitModel ( hh, slcOBAvailability, slcSample[0] );
                    if ( slc[0][autoTransit][tourType].getAvailabilityCount() > 0 ) {
                        chosen = slc[0][autoTransit][tourType].getChoiceResult();
                        chosenDestAlt = (int) slcUEC[OB][autoTransit][tourType].getAlternativeData().getIndexedValueAt(chosen, 2);
                        chosenShrtWlk = (int) slcUEC[OB][autoTransit][tourType].getAlternativeData().getIndexedValueAt(chosen, 3);
                    }
                    else {
                        logger.warn ( "no outbound joint slc alternatives available, hh_id=" + hh_id + ", person=" + person + ", tour=" + t);
                        hh.jointTours[t].setStopFreqAlt ( 1 );
                        hh.jointTours[t].setStopLocOB ( 0 );
                        hh.jointTours[t].setStopLocIB ( 0 );
//						hh.writeContentToLogger(logger);
                        break;
                    }



                    // set the chosen value in hh tour objects
                    hh.jointTours[t].setStopLocOB (chosenDestAlt);
                    hh.jointTours[t].setStopLocSubzoneOB (chosenShrtWlk);
                    locTime += (System.currentTimeMillis() - markTime);



                    markTime = System.currentTimeMillis();
                    slc[1][autoTransit][tourType].updateLogitModel ( hh, slcIBAvailability, slcSample[1] );
                    if ( slc[1][autoTransit][tourType].getAvailabilityCount() > 0 ) {
                        chosen = slc[1][autoTransit][tourType].getChoiceResult();
                        chosenDestAlt = (int) slcUEC[IB][autoTransit][tourType].getAlternativeData().getIndexedValueAt(chosen, 2);
                        chosenShrtWlk = (int) slcUEC[IB][autoTransit][tourType].getAlternativeData().getIndexedValueAt(chosen, 3);
                    }
                    else {
                        logger.warn ( "no inbound joint slc alternatives available, hh_id=" + hh_id + ", person=" + person + ", tour=" + t);
                        hh.jointTours[t].setStopFreqAlt ( 1 );
                        hh.jointTours[t].setStopLocOB ( 0 );
                        hh.jointTours[t].setStopLocIB ( 0 );
                        break;
                    }



                    // set the chosen value in hh tour objects
                    hh.jointTours[t].setStopLocIB (chosenDestAlt);
                    hh.jointTours[t].setStopLocSubzoneIB (chosenShrtWlk);
                    locTime += (System.currentTimeMillis() - markTime);

                    break;

                default:

                    logger.error ("invalid joint stop frequency choice = " + chosenAlt + " for household id=" + hh.getID() + " in joint stop frequency choice." );
//					hh.writeContentToLogger(logger);

                    break;
            }

            freqResultsRecord[0] = hh.getID();
            locResultsRecord[0] = hh.getID();

            freqResultsRecord[1] = t;              //tourID
            locResultsRecord[1] = t;                //tourID

            freqResultsRecord[2] = hh.jointTours[t].getTourType();              //tour purpose
            locResultsRecord[2] = hh.jointTours[t].getTourType();                //tour purpose

            freqResultsRecord[3] = hh.jointTours[t].getStopFreqAlt();

            locResultsRecord[3] = hh.jointTours[t].getOrigTaz();
            locResultsRecord[4] = hh.jointTours[t].getStopLocOB();
            locResultsRecord[5] = hh.jointTours[t].getDestTaz();
            locResultsRecord[6] = hh.jointTours[t].getStopLocIB();

            writeRecord(freqOutputStream, freqResultsRecord);
            writeRecord(locOutputStream, locResultsRecord);


        }

    }




    public void jointTourSmc ( Household hh ) {

        hh_id     = hh.getID();

        // get the array of joint tours for this household.
        if (hh.getJointTours() == null)
            return;


        // loop over joint tours for the hh
        // loop over individual joint tours for the hh
        for (int t=0; t < hh.jointTours.length; t++) {

            // if the primary mode for this joint tour is non-motorized, skip stop mode choice
            if (TourModeType.isNonmotor(hh.jointTours[t].getMode())) {
                 continue;
            }

            index.setOriginZone( hh.jointTours[t].getOrigTaz() );
            index.setDestZone( hh.jointTours[t].getDestTaz() );

            // determine the half tour segment mode choices if the tour has stops
            switch ( hh.jointTours[t].getStopFreqAlt() ) {

                // no stops for this tour
                case 1:

                    break;

                // 1 outbound, 0 inbound
                case 2:

                    index.setStopZone( hh.jointTours[t].getStopLocOB() );
                    hh.setChosenStartSkimPeriod(TODDataManager.getTodStartPeriod( hh.jointTours[t].getTimeOfDayAlt()));
                    hh.setStopLeg(IK_LEG);
                    // set outbound (ik) submode utilities (returns a string of 0s and 1s
                    // indicating which submodes are available).
                    long markTime = System.currentTimeMillis();
                    double[] submodeUtility = smcUEC.solve(index, hh, smcSample);
                    hh.jointTours[t].setTripIkMode ( getStopMode ( hh, hh.jointTours[t].getMode(), hh.jointTours[t].getSubmodeOB(), submodeUtility ) );

                    // set outbound (kj) submode utilities (returns a string of 0s and 1s
                    // indicating which submodes are available).
                    hh.setStopLeg(KJ_LEG);
                    submodeUtility = smcUEC.solve( index, hh, smcSample );
                    hh.jointTours[t].setTripKjMode ( getStopMode ( hh, hh.jointTours[t].getMode(), hh.jointTours[t].getSubmodeOB(), submodeUtility ) );
                    mcTime += (System.currentTimeMillis() - markTime);

                    break;

                // 0 outbound, 1 inbound
                case 3:

                    index.setStopZone( hh.jointTours[t].getStopLocIB() );
                    hh.setChosenStartSkimPeriod(TODDataManager.getTodEndPeriod( hh.jointTours[t].getTimeOfDayAlt()));
                    hh.setStopLeg(JK_LEG);
                    // set the mode for each portion of the inbound half-tour
                    markTime = System.currentTimeMillis();
                    // set inbound (jk) submode utilities (returns a string of 0s and 1s
                    // indicating which submodes are available).
                    submodeUtility = smcUEC.solve( index, hh, smcSample );
                    hh.jointTours[t].setTripJkMode ( getStopMode ( hh, hh.jointTours[t].getMode(), hh.jointTours[t].getSubmodeIB(), submodeUtility ) );

                    // set inbound (ki) submode utilities (returns a string of 0s and 1s
                    // indicating which submodes are available).
                    hh.setStopLeg(KI_LEG);
                    submodeUtility = smcUEC.solve( index, hh, smcSample );
                    hh.jointTours[t].setTripKiMode ( getStopMode ( hh, hh.jointTours[t].getMode(), hh.jointTours[t].getSubmodeIB(), submodeUtility ) );
                    mcTime += (System.currentTimeMillis() - markTime);

                    break;

                // 1 outbound, 1 inbound
                case 4:

                    index.setStopZone( hh.jointTours[t].getStopLocOB() );
                    hh.setChosenStartSkimPeriod(TODDataManager.getTodStartPeriod( hh.jointTours[t].getTimeOfDayAlt()));
                    hh.setStopLeg(IK_LEG);
                    // set the mode for each portion of the outbound half-tour
                    markTime = System.currentTimeMillis();
                    submodeUtility = smcUEC.solve( index, hh, smcSample );
                    hh.jointTours[t].setTripIkMode ( getStopMode ( hh, hh.jointTours[t].getMode(), hh.jointTours[t].getSubmodeOB(), submodeUtility ) );

                    // set outbound (kj) submode utilities (returns a string of 0s and 1s
                    // indicating which submodes are available).
                    hh.setStopLeg(KJ_LEG);
                    submodeUtility = smcUEC.solve( index, hh, smcSample );
                    hh.jointTours[t].setTripKjMode ( getStopMode ( hh, hh.jointTours[t].getMode(), hh.jointTours[t].getSubmodeOB(), submodeUtility ) );
                    mcTime += (System.currentTimeMillis() - markTime);


                    index.setStopZone( hh.jointTours[t].getStopLocIB() );
                    hh.setChosenStartSkimPeriod(TODDataManager.getTodEndPeriod( hh.jointTours[t].getTimeOfDayAlt()));
                    hh.setStopLeg(JK_LEG);
                    // set the mode for each portion of the inbound half-tour
                    markTime = System.currentTimeMillis();
                    // set inbound (jk) submode utilities (returns a string of 0s and 1s
                    // indicating which submodes are available).
                    hh.setStopLeg(JK_LEG);
                    submodeUtility = smcUEC.solve( index, hh, smcSample );
                    hh.jointTours[t].setTripJkMode ( getStopMode ( hh, hh.jointTours[t].getMode(), hh.jointTours[t].getSubmodeIB(), submodeUtility ) );

                    // set inbound (ki) submode utilities (returns a string of 0s and 1s
                    // indicating which submodes are available).
                    hh.setStopLeg(KI_LEG);
                    submodeUtility = smcUEC.solve( index, hh, smcSample );
                    hh.jointTours[t].setTripKiMode ( getStopMode ( hh, hh.jointTours[t].getMode(), hh.jointTours[t].getSubmodeIB(), submodeUtility ) );
                    mcTime += (System.currentTimeMillis() - markTime);


                    break;

            }
            modeResultsRecord[0] = hh.getID();
            modeResultsRecord[1] = t;
            modeResultsRecord[2] = hh.jointTours[t].getMode();
            modeResultsRecord[3] = hh.jointTours[t].getTripIkMode();
            modeResultsRecord[4] = hh.jointTours[t].getTripKjMode();
            modeResultsRecord[5] = hh.jointTours[t].getTripJkMode();
            modeResultsRecord[6] = hh.jointTours[t].getTripKiMode();

            writeRecord(modeOutputStream, modeResultsRecord);

        }

    }




    public void nonMandatoryTourSfcSlc ( Household hh ) {

        hh_id     = hh.getID();


        // get the array of indiv tours for this household.
        if (hh.getIndivTours() == null)
            return;

        hh.setTourCategory( TourType.NON_MANDATORY_CATEGORY );
        hh_taz_id = hh.getTazID();


        // loop over individual tours for the hh
        for (int t=0; t < hh.indivTours.length; t++) {

            hh.setOrigTaz ( hh.indivTours[t].getOrigTaz() );
            hh.setChosenDest ( hh.indivTours[t].getDestTaz() );
            hh.setChosenWalkSegment(hh.indivTours[t].getDestShrtWlk());

            index.setOriginZone( hh.indivTours[t].getOrigTaz() );
            index.setDestZone( hh.indivTours[t].getDestTaz() );

            // if the primary mode for this indiv non-mandatory tour is non-motorized, skip stop freq choice
            if (TourModeType.isNonmotor(hh.indivTours[t].getMode())) {
                continue;
            }


            tourType = hh.indivTours[t].getTourType();

            person = hh.indivTours[t].getTourPerson();

            hh.indivTours[t].setOrigTaz (hh_taz_id);
            hh.setPersonID ( person );
            hh.setTourID ( t );

            int autoTransit = TourModeType.isAuto(hh.indivTours[t].getMode() ) ? 0 : 1;

            setStopLocationAvailabilities(autoTransit, IB);

            long markTime = System.currentTimeMillis();
            // compute stop location choice logsum for inbound halftours for the hh
            // calculate inbound stop location choice logsum
            slc[1][autoTransit][tourType].updateLogitModel ( hh, slcIBAvailability, slcSample[1] );
            slcLogsum[1] = (float)(slc[1][autoTransit][tourType].getLogsum ());
            logsumTime += (System.currentTimeMillis()-markTime);

            // compute stop frequency choice proportions and choose alternative
            markTime = System.currentTimeMillis();
            sfc.updateLogitModel ( hh, sfcAvailability, sfcSample );
            int chosenAlt = sfc.getChoiceResult();
            // set the chosen value in hh tour objects
            hh.indivTours[t].setStopFreqAlt (chosenAlt);
            freqTime += (System.currentTimeMillis()-markTime);



            markTime = System.currentTimeMillis();

            // if chosen stop frequency is 2 or 4, calculate outbound soa and availabilities
            if ( chosenAlt == 2 || chosenAlt == 4 ) {
                setStopLocationAvailabilities(autoTransit, OB);
            }

            int chosen = 0;
            int chosenDestAlt = 0;
            int chosenShrtWlk = 0;
            // determine the stop locations if the tour has stops
            switch (chosenAlt) {

                // no stops for this tour
                case 1:

                    hh.indivTours[t].setStopLocOB ( 0 );
                    hh.indivTours[t].setStopLocIB ( 0 );

                    break;

                // 1 outbound, 0 inbound
                case 2:

                    // compute destination choice proportions and choose alternative
                    markTime = System.currentTimeMillis();
                    slc[0][autoTransit][tourType].updateLogitModel ( hh, slcOBAvailability, slcSample[0] );
                    if ( slc[0][autoTransit][tourType].getAvailabilityCount() > 0 ) {
                        chosen = slc[0][autoTransit][tourType].getChoiceResult();
                        chosenDestAlt = (int) slcUEC[OB][autoTransit][tourType].getAlternativeData().getIndexedValueAt(chosen, 2);
                        chosenShrtWlk = (int) slcUEC[OB][autoTransit][tourType].getAlternativeData().getIndexedValueAt(chosen, 3);
                    }
                    else {
                        chosen = 1;
                        chosenDestAlt = 1;
                        chosenShrtWlk = 1;
                        logger.warn ( "no outbound non-mandatory slc alternatives available, hh_id=" + hh_id + ", person=" + person + ", tour=" + t);
                        hh.indivTours[t].setStopFreqAlt ( 1 );
                        hh.indivTours[t].setStopLocOB ( 0 );
                        hh.indivTours[t].setStopLocIB ( 0 );
                        break;
                    }



                    // set the chosen value in hh tour objects
                    hh.indivTours[t].setStopLocOB (chosenDestAlt);
                    hh.indivTours[t].setStopLocSubzoneOB (chosenShrtWlk);
                    hh.indivTours[t].setStopLocIB (0);
                    locTime += (System.currentTimeMillis() - markTime);

                    break;

                // 0 outbound, 1 inbound
                case 3:

                    // compute destination choice proportions and choose alternative
                    markTime = System.currentTimeMillis();
                    slc[1][autoTransit][tourType].updateLogitModel ( hh, slcIBAvailability, slcSample[1] );
                    if ( slc[1][autoTransit][tourType].getAvailabilityCount() > 0 ) {
                        chosen = slc[1][autoTransit][tourType].getChoiceResult();
                        chosenDestAlt = (int) slcUEC[IB][autoTransit][tourType].getAlternativeData().getIndexedValueAt(chosen, 2);
                        chosenShrtWlk = (int) slcUEC[IB][autoTransit][tourType].getAlternativeData().getIndexedValueAt(chosen, 3);
                    }
                    else {
                        logger.warn ( "no inbound non-mandatory slc alternatives available, hh_id=" + hh_id + ", person=" + person + ", tour=" + t);
                        hh.indivTours[t].setStopFreqAlt ( 1 );
                        hh.indivTours[t].setStopLocOB ( 0 );
                        hh.indivTours[t].setStopLocIB ( 0 );
                        break;
                    }



                    // set the chosen value in hh tour objects
                    hh.indivTours[t].setStopLocIB (chosenDestAlt);
                    hh.indivTours[t].setStopLocSubzoneIB (chosenShrtWlk);
                    hh.indivTours[t].setStopLocOB (0);
                    locTime += (System.currentTimeMillis() - markTime);

                    break;

                // 1 outbound, 1 inbound
                case 4:

                    // compute destination choice proportions and choose alternative
                    markTime = System.currentTimeMillis();
                    slc[0][autoTransit][tourType].updateLogitModel ( hh, slcOBAvailability, slcSample[0] );
                    if ( slc[0][autoTransit][tourType].getAvailabilityCount() > 0 ) {
                        chosen = slc[0][autoTransit][tourType].getChoiceResult();
                        chosenDestAlt = (int) slcUEC[OB][autoTransit][tourType].getAlternativeData().getIndexedValueAt(chosen, 2);
                        chosenShrtWlk = (int) slcUEC[OB][autoTransit][tourType].getAlternativeData().getIndexedValueAt(chosen, 3);
                    }
                    else {
                        logger.warn ( "no outbound non-mandatory slc alternatives available, hh_id=" + hh_id + ", person=" + person + ", tour=" + t);
                        hh.indivTours[t].setStopFreqAlt ( 1 );
                        hh.indivTours[t].setStopLocOB ( 0 );
                        hh.indivTours[t].setStopLocIB ( 0 );
                        break;
                    }



                    // set the chosen value in hh tour objects
                    hh.indivTours[t].setStopLocOB (chosenDestAlt);
                    hh.indivTours[t].setStopLocSubzoneOB (chosenShrtWlk);
                    locTime += (System.currentTimeMillis() - markTime);



                    markTime = System.currentTimeMillis();
                    slc[1][autoTransit][tourType].updateLogitModel ( hh, slcIBAvailability, slcSample[1] );
                    if ( slc[1][autoTransit][tourType].getAvailabilityCount() > 0 ) {
                        chosen = slc[1][autoTransit][tourType].getChoiceResult();
                        chosenDestAlt = (int) slcUEC[IB][autoTransit][tourType].getAlternativeData().getIndexedValueAt(chosen, 2);
                        chosenShrtWlk = (int) slcUEC[IB][autoTransit][tourType].getAlternativeData().getIndexedValueAt(chosen, 3);
                    }
                    else {
                        logger.warn ( "no inbound non-mandatory slc alternatives available, hh_id=" + hh_id + ", person=" + person + ", tour=" + t);
                        hh.indivTours[t].setStopFreqAlt ( 1 );
                        hh.indivTours[t].setStopLocOB ( 0 );
                        hh.indivTours[t].setStopLocIB ( 0 );
                        break;
                    }



                    // set the chosen value in hh tour objects
                    hh.indivTours[t].setStopLocIB (chosenDestAlt);
                    hh.indivTours[t].setStopLocSubzoneIB (chosenShrtWlk);
                    locTime += (System.currentTimeMillis() - markTime);

                    break;

                default:

                    logger.error ("invalid individual non-mandatory stop frequency choice = " + chosenAlt + " for household id=" + hh.getID() + " in non-mandatory stop frequency model");
                    break;
            }

            freqResultsRecord[0] = hh.getID();
            locResultsRecord[0] = hh.getID();

            freqResultsRecord[1] = t;              //tourID
            locResultsRecord[1] = t;                //tourID

            freqResultsRecord[2] = hh.indivTours[t].getTourType();              //tour purpose
            locResultsRecord[2] = hh.indivTours[t].getTourType();                //tour purpose

            freqResultsRecord[3] = hh.indivTours[t].getStopFreqAlt();

            locResultsRecord[3] = hh.indivTours[t].getOrigTaz();
            locResultsRecord[4] = hh.indivTours[t].getStopLocOB();
            locResultsRecord[5] = hh.indivTours[t].getDestTaz();
            locResultsRecord[6] = hh.indivTours[t].getStopLocIB();

            writeRecord(freqOutputStream, freqResultsRecord);
            writeRecord(locOutputStream, locResultsRecord);


        }

    }




    public void nonMandatoryTourSmc ( Household hh ) {

        hh_id     = hh.getID();

        // get the array of indiv tours for this household.
        if (hh.getIndivTours() == null)
            return;


        // loop over indiv tours for the hh
        // loop over individual indiv tours for the hh
        for (int t=0; t < hh.indivTours.length; t++) {

            // if the primary mode for this indiv non-mandatory tour is non-motorized, skip trip mode choice
            if (TourModeType.isNonmotor(hh.indivTours[t].getMode())) {

                    continue;
            }

            index.setOriginZone( hh.indivTours[t].getOrigTaz() );
            index.setDestZone( hh.indivTours[t].getDestTaz() );

            // determine the half tour segment mode choices if the tour has stops
            switch ( hh.indivTours[t].getStopFreqAlt() ) {

                // no stops for this tour
                case 1:

                    break;

                // 1 outbound, 0 inbound
                case 2:

                    index.setStopZone( hh.indivTours[t].getStopLocOB() );
                    hh.setChosenStartSkimPeriod(TODDataManager.getTodStartPeriod( hh.indivTours[t].getTimeOfDayAlt()));
                    hh.setStopLeg(IK_LEG);
                    // set outbound (ik) submode utilities (returns a string of 0s and 1s
                    // indicating which submodes are available).
                    long markTime = System.currentTimeMillis();
                    double[] submodeUtility = smcUEC.solve(index, hh, smcSample);
                    hh.indivTours[t].setTripIkMode ( getStopMode ( hh, hh.indivTours[t].getMode(), hh.indivTours[t].getSubmodeOB(), submodeUtility ) );

                    // set outbound (kj) submode utilities (returns a string of 0s and 1s
                    // indicating which submodes are available).
                    hh.setStopLeg(KJ_LEG);
                    submodeUtility = smcUEC.solve( index, hh, smcSample );
                    hh.indivTours[t].setTripKjMode ( getStopMode ( hh, hh.indivTours[t].getMode(), hh.indivTours[t].getSubmodeOB(), submodeUtility ) );
                    mcTime += (System.currentTimeMillis() - markTime);

                    break;

                // 0 outbound, 1 inbound
                case 3:

                    index.setStopZone( hh.indivTours[t].getStopLocIB() );
                    hh.setChosenStartSkimPeriod(TODDataManager.getTodEndPeriod( hh.indivTours[t].getTimeOfDayAlt()));
                    hh.setStopLeg(JK_LEG);
                    // set the mode for each portion of the inbound half-tour
                    markTime = System.currentTimeMillis();
                    // set inbound (jk) submode utilities (returns a string of 0s and 1s
                    // indicating which submodes are available).
                    submodeUtility = smcUEC.solve( index, hh, smcSample );
                    hh.indivTours[t].setTripJkMode ( getStopMode ( hh, hh.indivTours[t].getMode(), hh.indivTours[t].getSubmodeIB(), submodeUtility ) );

                    // set inbound (ki) submode utilities (returns a string of 0s and 1s
                    // indicating which submodes are available).
                    hh.setStopLeg(KI_LEG);
                    submodeUtility = smcUEC.solve( index, hh, smcSample );
                    hh.indivTours[t].setTripKiMode ( getStopMode ( hh, hh.indivTours[t].getMode(), hh.indivTours[t].getSubmodeIB(), submodeUtility ) );
                    mcTime += (System.currentTimeMillis() - markTime);

                    break;

                // 1 outbound, 1 inbound
                case 4:

                    index.setStopZone( hh.indivTours[t].getStopLocOB() );
                    hh.setChosenStartSkimPeriod(TODDataManager.getTodStartPeriod( hh.indivTours[t].getTimeOfDayAlt()));
                    hh.setStopLeg(IK_LEG);
                    // set the mode for each portion of the outbound half-tour
                    markTime = System.currentTimeMillis();
                    submodeUtility = smcUEC.solve( index, hh, smcSample );
                    hh.indivTours[t].setTripIkMode ( getStopMode ( hh, hh.indivTours[t].getMode(), hh.indivTours[t].getSubmodeOB(), submodeUtility ) );

                    // set outbound (kj) submode utilities (returns a string of 0s and 1s
                    // indicating which submodes are available).
                    hh.setStopLeg(KJ_LEG);
                    submodeUtility = smcUEC.solve( index, hh, smcSample );
                    hh.indivTours[t].setTripKjMode ( getStopMode ( hh, hh.indivTours[t].getMode(), hh.indivTours[t].getSubmodeOB(), submodeUtility ) );
                    mcTime += (System.currentTimeMillis() - markTime);


                    index.setStopZone( hh.indivTours[t].getStopLocIB() );
                    hh.setChosenStartSkimPeriod(TODDataManager.getTodEndPeriod( hh.indivTours[t].getTimeOfDayAlt()));
                    hh.setStopLeg(JK_LEG);
                    // set the mode for each portion of the inbound half-tour
                    markTime = System.currentTimeMillis();
                    // set inbound (jk) submode utilities (returns a string of 0s and 1s
                    // indicating which submodes are available).
                    submodeUtility = smcUEC.solve( index, hh, smcSample );
                    hh.indivTours[t].setTripJkMode ( getStopMode ( hh, hh.indivTours[t].getMode(), hh.indivTours[t].getSubmodeIB(), submodeUtility ) );

                    // set inbound (ki) submode utilities (returns a string of 0s and 1s
                    // indicating which submodes are available).
                    hh.setStopLeg(KI_LEG);
                    submodeUtility = smcUEC.solve( index, hh, smcSample );
                    hh.indivTours[t].setTripKiMode ( getStopMode ( hh, hh.indivTours[t].getMode(), hh.indivTours[t].getSubmodeIB(), submodeUtility ) );
                    mcTime += (System.currentTimeMillis() - markTime);


                    break;

            }

            modeResultsRecord[0] = hh.getID();
            modeResultsRecord[1] = t;
            modeResultsRecord[2] = hh.indivTours[t].getMode();
            modeResultsRecord[3] = hh.indivTours[t].getTripIkMode();
            modeResultsRecord[4] = hh.indivTours[t].getTripKjMode();
            modeResultsRecord[5] = hh.indivTours[t].getTripJkMode();
            modeResultsRecord[6] = hh.indivTours[t].getTripKiMode();

            writeRecord(modeOutputStream, modeResultsRecord);

        }

    }




    public void atWorkTourSfcSlc ( Household hh ) {


        hh_id     = hh.getID();

        // get the array of mandatory tours for this household.
        if (hh.getMandatoryTours() == null)
            return;

        hh.setTourCategory( TourType.AT_WORK_CATEGORY );
        hh_taz_id = hh.getTazID();

        // loop over individual tours of the tour purpose of interest for the hh
        for (int t=0; t < hh.mandatoryTours.length; t++) {

            // get the array of subtours for this work tour
            if (hh.mandatoryTours[t].getSubTours() == null)
                continue;


            tourType = TourType.ATWORK;

            person = hh.mandatoryTours[t].getTourPerson();

            hh.setPersonID ( person );
            hh.setTourID ( t );


            // loop over subtours
            for (int s=0; s < hh.mandatoryTours[t].subTours.length; s++) {

                // the origin for the at-work tour is the destination of the primary work tour
                hh.setSubtourID ( s );
                hh.setOrigTaz ( hh.mandatoryTours[t].subTours[s].getOrigTaz() );
                hh.setChosenDest( hh.mandatoryTours[t].subTours[s].getDestTaz() );
                hh.setChosenWalkSegment(hh.mandatoryTours[t].getDestShrtWlk());

                index.setOriginZone( hh.mandatoryTours[t].subTours[s].getOrigTaz() );
                index.setDestZone( hh.mandatoryTours[t].subTours[s].getDestTaz() );

                // if the primary mode for this at-work subtour is non-motorized, skip stop frequency choice
                if (TourModeType.isNonmotor(hh.mandatoryTours[t].subTours[s].getMode())) {
                    continue;
                }


                int autoTransit = TourModeType.isAuto(hh.mandatoryTours[t].subTours[s].getMode() ) ? 0 : 1;
                int subtourType = hh.mandatoryTours[t].subTours[s].getSubTourType();

                // compute stop frequency choice proportions and choose alternative
                long markTime = System.currentTimeMillis();
                sfc.updateLogitModel ( hh, sfcAvailability, sfcSample );
                int chosenAlt = sfc.getChoiceResult();
                freqTime += (System.currentTimeMillis()-markTime);

                if (chosenAlt == 0) {
                    logger.error ("at-work stop frequency choice == 0 household id=" + hh.getID() );
                }

                // set the chosen value in hh tour objects
                hh.mandatoryTours[t].subTours[s].setStopFreqAlt (chosenAlt);

                if ( chosenAlt == 2 || chosenAlt == 4 ) {

                    setStopLocationAvailabilities(autoTransit, OB);
                    if(chosenAlt == 4){
                        setStopLocationAvailabilities(autoTransit, IB);
                    }

                } else if (chosenAlt ==3){
                    setStopLocationAvailabilities(autoTransit, OB);
                }


                int chosen = 0;
                int chosenDestAlt = 0;
                int chosenShrtWlk = 0;


                // use distance UEC to get utilities(distances) for each tour and trip segment

                // determine the stop locations if the tour has stops
                switch (chosenAlt) {

                    // no stops for this tour
                    case 1:

                        hh.mandatoryTours[t].subTours[s].setStopLocOB ( 0 );
                        hh.mandatoryTours[t].subTours[s].setStopLocIB ( 0 );

                        index.setStopZone( 0 );

                        break;

                    // 1 outbound, 0 inbound
                    case 2:

                        // compute destination choice proportions and choose alternative
                        markTime = System.currentTimeMillis();
                        slc[0][autoTransit][subtourType].updateLogitModel ( hh, slcOBAvailability, slcSample[0] );
                        if ( slc[0][autoTransit][subtourType].getAvailabilityCount() > 0 ) {
                            chosen = slc[0][autoTransit][subtourType].getChoiceResult();
                            chosenDestAlt = (int) slcUEC[OB][autoTransit][subtourType].getAlternativeData().getIndexedValueAt(chosen, 2);
                            chosenShrtWlk = (int) slcUEC[OB][autoTransit][subtourType].getAlternativeData().getIndexedValueAt(chosen, 3);
                        }
                        else {
                            logger.warn ( "no outbound atwork slc alternatives available, hh_id=" + hh_id + ", person=" + person + ", tour=" + t);
                            hh.mandatoryTours[t].subTours[s].setStopFreqAlt ( 1 );
                            hh.mandatoryTours[t].subTours[s].setStopLocOB ( 0 );
                            hh.mandatoryTours[t].subTours[s].setStopLocIB ( 0 );
                            break;
                        }



                        // set the chosen value in hh tour objects
                        hh.mandatoryTours[t].subTours[s].setStopLocOB (chosenDestAlt);
                        hh.mandatoryTours[t].subTours[s].setStopLocSubzoneOB (chosenShrtWlk);
                        hh.mandatoryTours[t].subTours[s].setStopLocIB (0);
                        locTime += (System.currentTimeMillis() - markTime);

                        break;

                    // 0 outbound, 1 inbound
                    case 3:

                        // compute destination choice proportions and choose alternative
                        markTime = System.currentTimeMillis();
                        slc[IB][autoTransit][subtourType].updateLogitModel ( hh, slcIBAvailability, slcSample[1] );
                        if ( slc[IB][autoTransit][subtourType].getAvailabilityCount() > 0 ) {
                            chosen = slc[1][autoTransit][subtourType].getChoiceResult();
                            chosenDestAlt = (int) slcUEC[IB][autoTransit][subtourType].getAlternativeData().getIndexedValueAt(chosen, 2);
                            chosenShrtWlk = (int) slcUEC[IB][autoTransit][subtourType].getAlternativeData().getIndexedValueAt(chosen, 3);
                        }
                        else {
                            logger.warn ( "no inbound atwork slc alternatives available, hh_id=" + hh_id + ", person=" + person + ", tour=" + t);
                            hh.mandatoryTours[t].subTours[s].setStopFreqAlt ( 1 );
                            hh.mandatoryTours[t].subTours[s].setStopLocOB ( 0 );
                            hh.mandatoryTours[t].subTours[s].setStopLocIB ( 0 );
                            break;
                        }



                        // set the chosen value in hh tour objects
                        hh.mandatoryTours[t].subTours[s].setStopLocIB (chosenDestAlt);
                        hh.mandatoryTours[t].subTours[s].setStopLocSubzoneIB (chosenShrtWlk);
                        hh.mandatoryTours[t].subTours[s].setStopLocOB (0);
                        locTime += (System.currentTimeMillis() - markTime);

                        break;

                    // 1 outbound, 1 inbound
                    case 4:

                        // compute destination choice proportions and choose alternative
                        markTime = System.currentTimeMillis();
                        slc[OB][autoTransit][subtourType].updateLogitModel ( hh, slcOBAvailability, slcSample[0] );
                        if ( slc[OB][autoTransit][subtourType].getAvailabilityCount() > 0 ) {
                            chosen = slc[OB][autoTransit][subtourType].getChoiceResult();
                            chosenDestAlt = (int) slcUEC[OB][autoTransit][subtourType].getAlternativeData().getIndexedValueAt(chosen, 2);
                            chosenShrtWlk = (int) slcUEC[OB][autoTransit][subtourType].getAlternativeData().getIndexedValueAt(chosen, 3);
                        }
                        else {
                            logger.warn ( "no outbound atwork slc alternatives available, hh_id=" + hh_id + ", person=" + person + ", tour=" + t);
                            hh.mandatoryTours[t].subTours[s].setStopFreqAlt ( 1 );
                            hh.mandatoryTours[t].subTours[s].setStopLocOB ( 0 );
                            hh.mandatoryTours[t].subTours[s].setStopLocIB ( 0 );
                            break;
                        }



                        // set the chosen value in hh tour objects
                        hh.mandatoryTours[t].subTours[s].setStopLocOB (chosenDestAlt);
                        hh.mandatoryTours[t].subTours[s].setStopLocSubzoneOB (chosenShrtWlk);
                        locTime += (System.currentTimeMillis() - markTime);



                        markTime = System.currentTimeMillis();
                        slc[IB][autoTransit][subtourType].updateLogitModel ( hh, slcIBAvailability, slcSample[1] );
                        if ( slc[IB][autoTransit][subtourType].getAvailabilityCount() > 0 ) {
                            chosen = slc[IB][autoTransit][subtourType].getChoiceResult();
                            chosenDestAlt = (int) slcUEC[IB][autoTransit][subtourType].getAlternativeData().getIndexedValueAt(chosen, 2);
                            chosenShrtWlk = (int) slcUEC[IB][autoTransit][subtourType].getAlternativeData().getIndexedValueAt(chosen, 3);
                        }
                        else {
                            logger.warn ( "no inbound atwork slc alternatives available, hh_id=" + hh_id + ", person=" + person + ", tour=" + t);
                            hh.mandatoryTours[t].subTours[s].setStopFreqAlt ( 1 );
                            hh.mandatoryTours[t].subTours[s].setStopLocOB ( 0 );
                            hh.mandatoryTours[t].subTours[s].setStopLocIB ( 0 );
                            break;
                        }



                        // set the chosen value in hh tour objects
                        hh.mandatoryTours[t].subTours[s].setStopLocIB (chosenDestAlt);
                        hh.mandatoryTours[t].subTours[s].setStopLocSubzoneIB (chosenShrtWlk);
                        locTime += (System.currentTimeMillis() - markTime);

                        break;

                    default:

                        logger.error ("invalid at-work stop frequency choice = " + chosenAlt + " for household id=" + hh.getID() + " in at-work stop frequency choice." );
//						hh.writeContentToLogger(logger);

                        break;
                }

                freqResultsRecord[0] = hh.getID();
                locResultsRecord[0] = hh.getID();

                freqResultsRecord[1] = t;              //tourID
                locResultsRecord[1] = t;                //tourID

                freqResultsRecord[2] = hh.mandatoryTours[t].subTours[s].getSubTourType();              //tour purpose
                locResultsRecord[2] = hh.mandatoryTours[t].subTours[s].getSubTourType();                //tour purpose

                freqResultsRecord[3] = hh.mandatoryTours[t].subTours[s].getStopFreqAlt();

                locResultsRecord[3] = hh.mandatoryTours[t].subTours[s].getOrigTaz();
                locResultsRecord[4] = hh.mandatoryTours[t].subTours[s].getStopLocOB();
                locResultsRecord[5] = hh.mandatoryTours[t].subTours[s].getDestTaz();
                locResultsRecord[6] = hh.mandatoryTours[t].subTours[s].getStopLocIB();

                writeRecord(freqOutputStream, freqResultsRecord);
                writeRecord(locOutputStream, locResultsRecord);


            }

        }

    }




    public void atWorkTourSmc ( Household hh ) {

        hh_id     = hh.getID();

        // get the array of mandatory tours for this household.
        if (hh.getMandatoryTours() == null)
            return;


        // loop over individual tours of the tour purpose of interest for the hh
        for (int t=0; t < hh.mandatoryTours.length; t++) {

            // get the array of subtours for this work tour
            if (hh.mandatoryTours[t].getSubTours() == null)
                continue;


            // loop over subtours
            for (int s=0; s < hh.mandatoryTours[t].subTours.length; s++) {

                // if the primary mode for this at-work subtour is non-motorized, skip stop mode choice
                if (TourModeType.isNonmotor(hh.mandatoryTours[t].subTours[s].getMode())) {

                    continue;
                }

                index.setOriginZone( hh.mandatoryTours[t].subTours[s].getOrigTaz() );
                index.setDestZone( hh.mandatoryTours[t].subTours[s].getDestTaz() );

                // determine the half tour segment mode choices if the tour has stops
                switch ( hh.mandatoryTours[t].subTours[s].getStopFreqAlt() ) {

                    // no stops for this tour
                    case 1:

                        break;

                    // 1 outbound, 0 inbound
                    case 2:

                        index.setStopZone( hh.mandatoryTours[t].subTours[s].getStopLocOB() );
                        hh.setChosenStartSkimPeriod(TODDataManager.getTodStartPeriod( hh.mandatoryTours[t].subTours[s].getTimeOfDayAlt()));
                        hh.setStopLeg(IK_LEG);
                        // set outbound (ik) submode utilities (returns a string of 0s and 1s
                        // indicating which submodes are available).
                        long markTime = System.currentTimeMillis();
                        double[] submodeUtility = smcUEC.solve(index, hh, smcSample);
                        hh.mandatoryTours[t].subTours[s].setTripIkMode ( getStopMode ( hh, hh.mandatoryTours[t].subTours[s].getMode(), hh.mandatoryTours[t].subTours[s].getSubmodeOB(), submodeUtility ) );

                        // set outbound (kj) submode utilities (returns a string of 0s and 1s
                        // indicating which submodes are available).
                        hh.setStopLeg(KJ_LEG);
                        submodeUtility = smcUEC.solve( index, hh, smcSample );
                        hh.mandatoryTours[t].subTours[s].setTripKjMode ( getStopMode ( hh, hh.mandatoryTours[t].subTours[s].getMode(), hh.mandatoryTours[t].subTours[s].getSubmodeOB(), submodeUtility ) );
                        mcTime += (System.currentTimeMillis() - markTime);

                        break;

                    // 0 outbound, 1 inbound
                    case 3:

                        index.setStopZone( hh.mandatoryTours[t].subTours[s].getStopLocIB() );
                        hh.setChosenStartSkimPeriod(TODDataManager.getTodEndPeriod( hh.mandatoryTours[t].subTours[s].getTimeOfDayAlt()));
                        hh.setStopLeg(JK_LEG);
                        // set the mode for each portion of the inbound half-tour
                        markTime = System.currentTimeMillis();
                        // set inbound (jk) submode utilities (returns a string of 0s and 1s
                        // indicating which submodes are available).
                        submodeUtility = smcUEC.solve( index, hh, smcSample );
                        hh.mandatoryTours[t].subTours[s].setTripJkMode ( getStopMode ( hh, hh.mandatoryTours[t].subTours[s].getMode(), hh.mandatoryTours[t].subTours[s].getSubmodeIB(), submodeUtility ) );

                        // set inbound (ki) submode utilities (returns a string of 0s and 1s
                        // indicating which submodes are available).
                        hh.setStopLeg(KI_LEG);
                        submodeUtility = smcUEC.solve( index, hh, smcSample );
                        hh.mandatoryTours[t].subTours[s].setTripKiMode ( getStopMode ( hh, hh.mandatoryTours[t].subTours[s].getMode(), hh.mandatoryTours[t].subTours[s].getSubmodeIB(), submodeUtility ) );
                        mcTime += (System.currentTimeMillis() - markTime);

                        break;

                    // 1 outbound, 1 inbound
                    case 4:

                        index.setStopZone( hh.mandatoryTours[t].subTours[s].getStopLocOB() );
                        hh.setChosenStartSkimPeriod(TODDataManager.getTodStartPeriod( hh.mandatoryTours[t].subTours[s].getTimeOfDayAlt()));
                        hh.setStopLeg(IK_LEG);
                        // set the mode for each portion of the outbound half-tour
                        markTime = System.currentTimeMillis();
                        submodeUtility = smcUEC.solve( index, hh, smcSample );
                        hh.mandatoryTours[t].subTours[s].setTripIkMode ( getStopMode ( hh, hh.mandatoryTours[t].subTours[s].getMode(), hh.mandatoryTours[t].subTours[s].getSubmodeOB(), submodeUtility ) );

                        // set outbound (kj) submode utilities (returns a string of 0s and 1s
                        // indicating which submodes are available).
                        hh.setStopLeg(KJ_LEG);
                        submodeUtility = smcUEC.solve( index, hh, smcSample );
                        hh.mandatoryTours[t].subTours[s].setTripKjMode ( getStopMode ( hh, hh.mandatoryTours[t].subTours[s].getMode(), hh.mandatoryTours[t].subTours[s].getSubmodeOB(), submodeUtility ) );
                        mcTime += (System.currentTimeMillis() - markTime);


                        index.setStopZone( hh.mandatoryTours[t].subTours[s].getStopLocIB() );
                        hh.setChosenStartSkimPeriod(TODDataManager.getTodEndPeriod( hh.mandatoryTours[t].subTours[s].getTimeOfDayAlt()));
                        hh.setStopLeg(JK_LEG);
                        // set the mode for each portion of the inbound half-tour
                        markTime = System.currentTimeMillis();
                        // set inbound (jk) submode utilities (returns a string of 0s and 1s
                        // indicating which submodes are available).
                        submodeUtility = smcUEC.solve( index, hh, smcSample );
                        hh.mandatoryTours[t].subTours[s].setTripJkMode ( getStopMode ( hh, hh.mandatoryTours[t].subTours[s].getMode(), hh.mandatoryTours[t].subTours[s].getSubmodeIB(), submodeUtility ) );

                        // set inbound (ki) submode utilities (returns a string of 0s and 1s
                        // indicating which submodes are available).
                        hh.setStopLeg(KI_LEG);
                        submodeUtility = smcUEC.solve( index, hh, smcSample );
                        hh.mandatoryTours[t].subTours[s].setTripKiMode ( getStopMode ( hh, hh.mandatoryTours[t].subTours[s].getMode(), hh.mandatoryTours[t].subTours[s].getSubmodeIB(), submodeUtility ) );
                        mcTime += (System.currentTimeMillis() - markTime);


                        break;

                }

                modeResultsRecord[0] = hh.getID();
                modeResultsRecord[1] = t;
                modeResultsRecord[2] = hh.mandatoryTours[t].subTours[s].getMode();
                modeResultsRecord[3] = hh.mandatoryTours[t].subTours[s].getTripIkMode();
                modeResultsRecord[4] = hh.mandatoryTours[t].subTours[s].getTripKjMode();
                modeResultsRecord[5] = hh.mandatoryTours[t].subTours[s].getTripJkMode();
                modeResultsRecord[6] = hh.mandatoryTours[t].subTours[s].getTripKiMode();

                writeRecord(modeOutputStream, modeResultsRecord);


            }

        }

    }

    private void setStopLocationAvailabilities(int mode, int direction){

        if ( mode == AUTO ) {
            if (direction == OB) {
                Arrays.fill(slcOBAvailability, true);
                Arrays.fill(slcSample[OB], 1);
                for (int k=1; k < slcOBAvailability.length; k++) {
                    // set destination choice alternative availability to false if size <= 0 for the segment.
                    if ( ZonalDataManager.getStopSizeByTourType(OB, tourType, k) <= 0.0 ) {
                        slcOBAvailability[k] = false;
                        slcSample[OB][k] = 0;
                    }
                }
            } else {
                Arrays.fill(slcIBAvailability, true);
                Arrays.fill(slcSample[IB], 1);
                for (int k=1; k < slcIBAvailability.length; k++) {
                    // set destination choice alternative availability to false if size <= 0 for the segment.
                    if ( ZonalDataManager.getStopSizeByTourType(IB, tourType, k) <= 0.0 ) {
                        slcIBAvailability[k] = false;
                        slcSample[IB][k] = 0;
                    }
                }
            }

        } else {

            if (direction == OB) {
                Arrays.fill(slcOBAvailability, true);
                Arrays.fill(slcSample[OB], 1);
                for (int k=1; k < slcOBAvailability.length; k++) {
                    // set destination choice alternative availability to true if size > 0 for the segment.
                    if ( ZonalDataManager.getStopSizeByTourType(OB, tourType, k) <= 0.0 || ZonalDataManager.zonalShortAccess[k] == 0 ) {
                        slcOBAvailability[k] = false;
                        slcSample[OB][k] = 0;
                    }
                }
            } else {
                Arrays.fill(slcIBAvailability, true);
                Arrays.fill(slcSample[IB], 1);
                for (int k=1; k < slcIBAvailability.length; k++) {
                    // set destination choice alternative availability to true if size > 0 for the segment.
                    if ( ZonalDataManager.getStopSizeByTourType(IB, tourType, k) <= 0.0 || ZonalDataManager.zonalShortAccess[k] == 0 ) {
                        slcIBAvailability[k] = false;
                        slcSample[IB][k] = 0;
                    }
                }
            }
        }
    }

    public void closeSlfSlcOutputStreams(){
        freqOutputStream.close();
        locOutputStream.close();

    }

    public void closeSmcOutputStreams(){
        modeOutputStream.close();
     }


    public void printTimes ( short tourTypeCategory ) {

        for (short i=1; i < 5; i++) {

            if ( tourTypeCategory == i ) {

                logger.info ( "Stops Model Component Times for " + TourType.getCategoryLabelForCategory(i) + " tours:");
                logger.info ( "total seconds processing stop location choice logsums = " + (float)logsumTime/1000);
                logger.info ( "total seconds processing stop frequency choice = " + (float)freqTime/1000);
                logger.info ( "total seconds processing stop location choice = " + (float)locTime/1000);
                logger.info ( "total seconds processing stop mode choice = " + (float)mcTime/1000);
                logger.info ( "");

            }
        }

    }
}
