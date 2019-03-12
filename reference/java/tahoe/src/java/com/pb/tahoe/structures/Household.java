package com.pb.tahoe.structures;

import com.pb.common.util.ObjectUtil;
import com.pb.common.util.ResourceUtil;
import com.pb.tahoe.stops.StopsModelBase;
import com.pb.tahoe.util.TODDataManager;
import com.pb.tahoe.util.ZonalDataManager;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.ArrayList;
import java.util.ResourceBundle;

/**
 * This class is used for ...
 *
 * @author Chris Frazier
 * @version Feb 16, 2006
 *          Created by IntelliJ IDEA.
 */
public class Household extends DecisionMakingUnit implements java.io.Externalizable {

    protected static Logger logger = Logger.getLogger(Household.class);
    private static int season = ResourceUtil.getBooleanProperty(ResourceUtil.getResourceBundle("tahoe"),"summer") ? 1 : 0;

//    private int ID;
//    private short taz;
//    private short origTaz;

    private short tourCategory;
    private short personID;
    private short tourID;
    private short subtourID;
//    private short chosenDest;
//    private short originWalkSegment;
//    private short chosenWalkSegment;
//    private short chosenTodAlt;
    private short chosenStartSkimPeriod;

    private float income;
    private short size;
    private short autoOwnership;
    private short hhType;

    private short maxAdultOverlaps;
    private short maxChildOverlaps;
    private short maxMixedOverlaps;
    private short maxAdultWindow;
    private short maxChildWindow;
    private short travellingAdults;
    private short travellingChildren;
    private short travellingNonPreschool;

    public Person[] persons;
    private short[] personsByType = new short[PersonType.TYPES+1];
    private int[][] personsByPersonTypeArray = new int[PersonType.TYPES+1][];

    public JointTour[] jointTours;
    private short[] jointToursByType = new short[TourType.TYPES+1];

    public Tour[] indivTours;
    private short[] indivToursByType = new short[TourType.TYPES+1];

    public Tour[] mandatoryTours;
    private short[] mandatoryToursByType = new short[TourType.TYPES+1];

    int stopLeg;
    


    public Household() {
    }


//     /**
//     * set id for household object
//     */
//    public void setID (int id) {
//        ID = id;
//    }
//
//    /**
//     * return id for household object
//     */
//    public int getID () {
//        return ID;
//    }
//
//
//
//    /**
//     * set TAZ for household object
//     */
//    public void setTazID (int arg) {
//        taz = (short)arg;
//    }
//
//        /**
//         * return TAZ for household object
//         */
//        public int getTazID () {
//            return taz;
//        }
//
//
//        /**
//         * set origin taz variable.
//         */
//        public void setOrigTaz ( int arg ) {
//            origTaz = (short)arg;
//        }
//
//        /**
//         * get origin taz value.
//         */
//        public int getOrigTaz () {
//            return origTaz;
//        }


//        public void setOriginWalkSegment (int segment) {
//            originWalkSegment = (short)segment;
//        }
//
//
//        public int getOriginWalkSegment () {
//            return originWalkSegment;
//        }
//
//
//        /**
//         * set walk segment (0-none, 1-short, 2-long walk to transit access) for the destination choice alternative for this tour
//         */
//        public void setChosenWalkSegment (int arg) {
//            chosenWalkSegment = (short)arg;
//        }
//
//        /**
//         * return the walk segment (0-none, 1-short, 2-long walk to transit access) for the destination choice alternative for this tour
//         */
//        public int getChosenWalkSegment () {
//            return chosenWalkSegment;
//        }


//        /**
//         * set TAZ for the chosen destination alternative for this tour
//         */
//        public void setChosenDest (int arg) {
//            chosenDest = (short)arg;
//        }
//
//        /**
//         * return TAZ for the chosen destination alternative for this tour
//         */
//        public int getChosenDest () {
//            return chosenDest;
//        }
//
//
//
//        /**
//         * set TOD alt for the chosen time of day alternative for this tour
//         */
//        public void setChosenTodAlt (int arg) {
//            chosenTodAlt = (short)arg;
//        }
//
//        /**
//         * return TOD alt for the chosen time of day alternative for this tour
//         */
//        public int getChosenTodAlt () {
//            return chosenTodAlt;
//        }

        /**
         * set skim period for the chosen time of day alternative for this tour
         */
        public void setChosenStartSkimPeriod (int arg) {
            chosenStartSkimPeriod = (short)arg;
        }

        /**
         * return starting  skim period for the chosen time of day alternative for this tour
         */
        public int getChosenStartSkimPeriod () {
            return chosenStartSkimPeriod;
        }





        /**
         * set Person ID used as a Person[] array reference for utility calculations for this household object
         */
        public void setPersonID (int arg) {
            personID = (short)arg;
        }

        /**
         * set Tour ID used as a Tour[] array reference for utility calculations for this household object
         */
        public void setTourID (int arg) {
            tourID = (short)arg;
        }

        /**
         * get Tour ID used as a Tour[] array reference for utility calculations for this household object
         */
        public int getTourID () {
            return tourID;
        }

        /**
         * set subtour ID used as a subtour[] array reference for utility calculations for this household object
         */
        public void setSubtourID (int arg) {
            subtourID = (short)arg;
        }

        /**
         * set tour type category
         */
        public void setTourCategory (int arg) {
            tourCategory = (short)arg;
        }



        /**
         * set hh size for household object
         */
        public void setHHSize (int arg) {
            size = (short)arg;
        }

        /**
         * return hh size for this household object
         */
        public int getHHSize () {
            return size;
        }



        /**
         * set hh auto ownership from AO model for household object
         */
        public void setAutoOwnership (int arg) {
            autoOwnership = (short)arg;
        }

        /**
         * return hh auto ownership from AO model for this household object
         */
        public int getAutoOwnership () {
            return (int)autoOwnership;
        }



        /**
         * set hh income for household object
         */
        public void setHHIncome (int arg) {
            income = (short)arg;
        }

        /**
         * return hh income for this household object
         */
        public int getHHIncome () {
            return (int)income;
        }


        /**
         * set maximum time window overlaps between adults in hh for Household object
         */
        public void setMaxAdultOverlaps (int arg) {
            maxAdultOverlaps = (short)arg;
        }

        /**
         * return maximum time window overlaps between adults in hh for Household object
         * called by method generated by UEC for models: 3.1,3.2
         */
        public float getMaxAdultOverlaps () {
            return maxAdultOverlaps;
        }



        /**
         * set maximum time window overlaps between children in hh for household object
         */
        public void setMaxChildOverlaps (int arg) {
            maxChildOverlaps = (short)arg;
        }

        /**
         * return maximum time window overlaps between children in hh for household object
         * called by method generated by UEC for models: 3.2
         */
        public float getMaxChildOverlaps () {
            return maxChildOverlaps;
        }



        /**
         * set maximum time window overlaps between adults/children in hh for household object
         */
        public void setMaxMixedOverlaps (int arg) {
            maxMixedOverlaps = (short)arg;
        }

        /**
         * return maximum time window overlaps between adults/children in hh for household object
         * called by method generated by UEC for models: 3.1,3.2
         */
        public float getMaxMixedOverlaps () {
            return maxMixedOverlaps;
        }



        /**
         * set the total number of adults making trips away from home for this household object
         */
        public void setTravelActiveAdults (int arg) {
            travellingAdults = (short)arg;
        }

        /**
         * return the total number of adults making trips away from home for this household object
         * called by method generated by UEC for models: 3.2
         */
        public float getTravelActiveAdults () {
            return travellingAdults;
        }



        /**
         * set the total number of children making trips away from home for this household object
         */
        public void setTravelActiveChildren (int arg) {
            travellingChildren = (short)arg;
        }

        /**
         * return the total number of children making trips away from home for this household object
         * called by method generated by UEC for models: 3.2
         */
        public float getTravelActiveChildren () {
            return travellingChildren;
        }



        /**
         * return the tour mode of the tour for which the UEC.solve() has been called
         */
        public int getTourMode () {
            int mode=0;

            if (tourCategory == TourType.MANDATORY_CATEGORY)
                mode = mandatoryTours[tourID].getMode();
            else if (tourCategory == TourType.JOINT_CATEGORY)
                mode = jointTours[tourID].getMode();
            else if (tourCategory == TourType.NON_MANDATORY_CATEGORY)
                mode = indivTours[tourID].getMode();
            else if (tourCategory == TourType.AT_WORK_CATEGORY)
                mode = mandatoryTours[tourID].getMode();

            return mode;
        }

        /**
         * return the atwork subtour mode of the tour for which the UEC.solve() has been called
         */
        public int getSubtourMode () {
            return mandatoryTours[tourID].subTours[subtourID].getMode();
        }

        /**
         * return the OB submode of the tour for which the UEC.solve() has been called
         */
        public int getSubmodeOB () {
            int mode=0;

            if (tourCategory == TourType.MANDATORY_CATEGORY)
                mode = mandatoryTours[tourID].getSubmodeOB();
            else if (tourCategory == TourType.JOINT_CATEGORY)
                mode = jointTours[tourID].getSubmodeOB();
            else if (tourCategory == TourType.NON_MANDATORY_CATEGORY)
                mode = indivTours[tourID].getSubmodeOB();
            else if (tourCategory == TourType.AT_WORK_CATEGORY)
                mode = mandatoryTours[tourID].subTours[subtourID].getSubmodeOB();

            return mode;
        }

        /**
         * return the IB submode of the tour for which the UEC.solve() has been called
         */
        public int getSubmodeIB () {
            int mode=0;

            if (tourCategory == TourType.MANDATORY_CATEGORY)
                mode = mandatoryTours[tourID].getSubmodeIB();
            else if (tourCategory == TourType.JOINT_CATEGORY)
                mode = jointTours[tourID].getSubmodeIB();
            else if (tourCategory == TourType.NON_MANDATORY_CATEGORY)
                mode = indivTours[tourID].getSubmodeIB();
            else if (tourCategory == TourType.AT_WORK_CATEGORY)
                mode = mandatoryTours[tourID].subTours[subtourID].getSubmodeIB();

            return mode;
        }

        /**
         * return the tour type of the tour for which the UEC.solve() has been called
         */
        public float getTourType () {
            int tourType=0;

            if (tourCategory == TourType.MANDATORY_CATEGORY)
                tourType = mandatoryTours[tourID].getTourType();
            else if (tourCategory == TourType.JOINT_CATEGORY)
                tourType = jointTours[tourID].getTourType();
            else if (tourCategory == TourType.NON_MANDATORY_CATEGORY)
                tourType = indivTours[tourID].getTourType();
            else if (tourCategory == TourType.AT_WORK_CATEGORY)
                tourType = mandatoryTours[tourID].subTours[subtourID].getTourType();

            return tourType;
        }

        /**
         * return the tour order of the tour for which the UEC.solve() has been called
         */
        public float getTourOrder () {
            int tourOrder=0;

            if (tourCategory == TourType.MANDATORY_CATEGORY)
                tourOrder = mandatoryTours[tourID].getTourOrder();
            else if (tourCategory == TourType.JOINT_CATEGORY)
                tourOrder = jointTours[tourID].getTourOrder();
            else if (tourCategory == TourType.NON_MANDATORY_CATEGORY)
                tourOrder = indivTours[tourID].getTourOrder();
            else if (tourCategory == TourType.AT_WORK_CATEGORY)
                tourOrder = mandatoryTours[tourID].subTours[subtourID].getTourOrder();

            return tourOrder;
        }

        /**
         * return the tour type of the at-work subtour for which the UEC.solve() has been called
         */
        public float getSubtourType () {
            return mandatoryTours[tourID].subTours[subtourID].getSubTourType();
        }

        /**
         * return the tour order of the at-work subtour for which the UEC.solve() has been called
         */
        public float getSubtourOrder () {
            return mandatoryTours[tourID].subTours[subtourID].getTourOrder();
        }

        public Person getPerson() {
            return persons[personID];
        }

        /**
         * return the person type of the person making the joint tour for which the UEC.solve() has been called
         */
        public float getPersonType () {
            return persons[personID].getPersonType();
        }

        /**
         * return the daily activity pattern type of the person making the joint tour for which the UEC.solve() has been called
         */
        public float getPatternType () {
            return persons[personID].getPatternType();
        }

        /**
         * return the daily activity pattern type of the model 4.2 person alternative making the maintenance tour for which the UEC.solve() has been called
         */
        public float getPatternTypeAlt (int alternative) {
            switch (alternative) {
                case 1:
                    if (personsByPersonTypeArray[PersonType.WORKER_F].length >= 1)
                        return persons[personsByPersonTypeArray[PersonType.WORKER_F][0]].getPatternType();
                    break;
                case 2:
                    if (personsByPersonTypeArray[PersonType.WORKER_F].length >= 2)
                        return persons[personsByPersonTypeArray[PersonType.WORKER_F][1]].getPatternType();
                    break;
                case 3:
                    if (personsByPersonTypeArray[PersonType.WORKER_F].length >= 3)
                        return persons[personsByPersonTypeArray[PersonType.WORKER_F][2]].getPatternType();
                    break;
                case 4:
                    if (personsByPersonTypeArray[PersonType.WORKER_F].length >= 4)
                        return persons[personsByPersonTypeArray[PersonType.WORKER_F][3]].getPatternType();
                    break;
                case 5:
                    if (personsByPersonTypeArray[PersonType.WORKER_P].length >= 1)
                        return persons[personsByPersonTypeArray[PersonType.WORKER_P][0]].getPatternType();
                    break;
                case 6:
                    if (personsByPersonTypeArray[PersonType.WORKER_P].length >= 2)
                        return persons[personsByPersonTypeArray[PersonType.WORKER_P][1]].getPatternType();
                    break;
                case 7:
                    if (personsByPersonTypeArray[PersonType.WORKER_P].length >= 3)
                        return persons[personsByPersonTypeArray[PersonType.WORKER_P][2]].getPatternType();
                    break;
                case 8:
                    if (personsByPersonTypeArray[PersonType.WORKER_P].length >= 4)
                        return persons[personsByPersonTypeArray[PersonType.WORKER_P][3]].getPatternType();
                    break;

                case 13:
                    if (personsByPersonTypeArray[PersonType.NONWORKER].length >= 1)
                        return persons[personsByPersonTypeArray[PersonType.NONWORKER][0]].getPatternType();
                    break;
                case 14:
                    if (personsByPersonTypeArray[PersonType.NONWORKER].length >= 2)
                        return persons[personsByPersonTypeArray[PersonType.NONWORKER][1]].getPatternType();
                    break;
                case 15:
                    if (personsByPersonTypeArray[PersonType.NONWORKER].length >= 3)
                        return persons[personsByPersonTypeArray[PersonType.NONWORKER][2]].getPatternType();
                    break;
                case 16:
                    if (personsByPersonTypeArray[PersonType.NONWORKER].length >= 4)
                        return persons[personsByPersonTypeArray[PersonType.NONWORKER][3]].getPatternType();
                    break;
                case 17:
                    if (personsByPersonTypeArray[PersonType.SCHOOL_PRED].length >= 1)
                        return persons[personsByPersonTypeArray[PersonType.SCHOOL_PRED][0]].getPatternType();
                    break;
                case 18:
                    if (personsByPersonTypeArray[PersonType.SCHOOL_PRED].length >= 2)
                        return persons[personsByPersonTypeArray[PersonType.SCHOOL_PRED][1]].getPatternType();
                    break;
                case 19:
                    if (personsByPersonTypeArray[PersonType.SCHOOL_PRED].length >= 3)
                        return persons[personsByPersonTypeArray[PersonType.SCHOOL_PRED][2]].getPatternType();
                    break;
                case 20:
                    if (personsByPersonTypeArray[PersonType.SCHOOL_PRED].length >= 4)
                        return persons[personsByPersonTypeArray[PersonType.SCHOOL_PRED][3]].getPatternType();
                    break;
                case 21:
                    if (personsByPersonTypeArray[PersonType.SCHOOL_DRIV].length >= 1)
                        return persons[personsByPersonTypeArray[PersonType.SCHOOL_DRIV][0]].getPatternType();
                    break;
                case 22:
                    if (personsByPersonTypeArray[PersonType.SCHOOL_DRIV].length >= 2)
                        return persons[personsByPersonTypeArray[PersonType.SCHOOL_DRIV][1]].getPatternType();
                    break;
                case 23:
                    if (personsByPersonTypeArray[PersonType.SCHOOL_DRIV].length >= 3)
                        return persons[personsByPersonTypeArray[PersonType.SCHOOL_DRIV][2]].getPatternType();
                    break;
                case 24:
                    if (personsByPersonTypeArray[PersonType.SCHOOL_DRIV].length >= 4)
                        return persons[personsByPersonTypeArray[PersonType.SCHOOL_DRIV][3]].getPatternType();
                    break;
            }

            return 0;
        }

        /**
         * return the number of joint tour participations of the model 4.2 person alternative making the maintenance tour for which the UEC.solve() has been called
         */
        public float getJointToursPersonAlt (int alternative) {
            switch (alternative) {
                case 1:
                    if (personsByPersonTypeArray[PersonType.WORKER_F].length >= 1)
                        return persons[personsByPersonTypeArray[PersonType.WORKER_F][0]].getJointTourCount();
                    break;
                case 2:
                    if (personsByPersonTypeArray[PersonType.WORKER_F].length >= 2)
                        return persons[personsByPersonTypeArray[PersonType.WORKER_F][1]].getJointTourCount();
                    break;
                case 3:
                    if (personsByPersonTypeArray[PersonType.WORKER_F].length >= 3)
                        return persons[personsByPersonTypeArray[PersonType.WORKER_F][2]].getJointTourCount();
                    break;
                case 4:
                    if (personsByPersonTypeArray[PersonType.WORKER_F].length >= 4)
                        return persons[personsByPersonTypeArray[PersonType.WORKER_F][3]].getJointTourCount();
                    break;
                case 5:
                    if (personsByPersonTypeArray[PersonType.WORKER_P].length >= 1)
                        return persons[personsByPersonTypeArray[PersonType.WORKER_P][0]].getJointTourCount();
                    break;
                case 6:
                    if (personsByPersonTypeArray[PersonType.WORKER_P].length >= 2)
                        return persons[personsByPersonTypeArray[PersonType.WORKER_P][1]].getJointTourCount();
                    break;
                case 7:
                    if (personsByPersonTypeArray[PersonType.WORKER_P].length >= 3)
                        return persons[personsByPersonTypeArray[PersonType.WORKER_P][2]].getJointTourCount();
                    break;
                case 8:
                    if (personsByPersonTypeArray[PersonType.WORKER_P].length >= 4)
                        return persons[personsByPersonTypeArray[PersonType.WORKER_P][3]].getJointTourCount();
                    break;

                case 13:
                    if (personsByPersonTypeArray[PersonType.NONWORKER].length >= 1)
                        return persons[personsByPersonTypeArray[PersonType.NONWORKER][0]].getJointTourCount();
                    break;
                case 14:
                    if (personsByPersonTypeArray[PersonType.NONWORKER].length >= 2)
                        return persons[personsByPersonTypeArray[PersonType.NONWORKER][1]].getJointTourCount();
                    break;
                case 15:
                    if (personsByPersonTypeArray[PersonType.NONWORKER].length >= 3)
                        return persons[personsByPersonTypeArray[PersonType.NONWORKER][2]].getJointTourCount();
                    break;
                case 16:
                    if (personsByPersonTypeArray[PersonType.NONWORKER].length >= 4)
                        return persons[personsByPersonTypeArray[PersonType.NONWORKER][3]].getJointTourCount();
                    break;
                case 17:
                    if (personsByPersonTypeArray[PersonType.SCHOOL_PRED].length >= 1)
                        return persons[personsByPersonTypeArray[PersonType.SCHOOL_PRED][0]].getJointTourCount();
                    break;
                case 18:
                    if (personsByPersonTypeArray[PersonType.SCHOOL_PRED].length >= 2)
                        return persons[personsByPersonTypeArray[PersonType.SCHOOL_PRED][1]].getJointTourCount();
                    break;
                case 19:
                    if (personsByPersonTypeArray[PersonType.SCHOOL_PRED].length >= 3)
                        return persons[personsByPersonTypeArray[PersonType.SCHOOL_PRED][2]].getJointTourCount();
                    break;
                case 20:
                    if (personsByPersonTypeArray[PersonType.SCHOOL_PRED].length >= 4)
                        return persons[personsByPersonTypeArray[PersonType.SCHOOL_PRED][3]].getJointTourCount();
                    break;
                case 21:
                    if (personsByPersonTypeArray[PersonType.SCHOOL_DRIV].length >= 1)
                        return persons[personsByPersonTypeArray[PersonType.SCHOOL_DRIV][0]].getJointTourCount();
                    break;
                case 22:
                    if (personsByPersonTypeArray[PersonType.SCHOOL_DRIV].length >= 2)
                        return persons[personsByPersonTypeArray[PersonType.SCHOOL_DRIV][1]].getJointTourCount();
                    break;
                case 23:
                    if (personsByPersonTypeArray[PersonType.SCHOOL_DRIV].length >= 3)
                        return persons[personsByPersonTypeArray[PersonType.SCHOOL_DRIV][2]].getJointTourCount();
                    break;
                case 24:
                    if (personsByPersonTypeArray[PersonType.SCHOOL_DRIV].length >= 4)
                        return persons[personsByPersonTypeArray[PersonType.SCHOOL_DRIV][3]].getJointTourCount();
                    break;
            }

            return 0;
        }

        /**
         * return the available time window of the model 4.2 person alternative making the maintenance tour for which the UEC.solve() has been called
         */
        public float getTimeWindowAvailAlt (int alternative) {
            switch (alternative) {
                case 1:
                    if (personsByPersonTypeArray[PersonType.WORKER_F].length >= 1)
                        return persons[personsByPersonTypeArray[PersonType.WORKER_F][0]].getAvailableWindow();
                    break;
                case 2:
                    if (personsByPersonTypeArray[PersonType.WORKER_F].length >= 2)
                        return persons[personsByPersonTypeArray[PersonType.WORKER_F][1]].getAvailableWindow();
                    break;
                case 3:
                    if (personsByPersonTypeArray[PersonType.WORKER_F].length >= 3)
                        return persons[personsByPersonTypeArray[PersonType.WORKER_F][2]].getAvailableWindow();
                    break;
                case 4:
                    if (personsByPersonTypeArray[PersonType.WORKER_F].length >= 4)
                        return persons[personsByPersonTypeArray[PersonType.WORKER_F][3]].getAvailableWindow();
                    break;
                case 5:
                    if (personsByPersonTypeArray[PersonType.WORKER_P].length >= 1)
                        return persons[personsByPersonTypeArray[PersonType.WORKER_P][0]].getAvailableWindow();
                    break;
                case 6:
                    if (personsByPersonTypeArray[PersonType.WORKER_P].length >= 2)
                        return persons[personsByPersonTypeArray[PersonType.WORKER_P][1]].getAvailableWindow();
                    break;
                case 7:
                    if (personsByPersonTypeArray[PersonType.WORKER_P].length >= 3)
                        return persons[personsByPersonTypeArray[PersonType.WORKER_P][2]].getAvailableWindow();
                    break;
                case 8:
                    if (personsByPersonTypeArray[PersonType.WORKER_P].length >= 4)
                        return persons[personsByPersonTypeArray[PersonType.WORKER_P][3]].getAvailableWindow();
                    break;

                case 13:
                    if (personsByPersonTypeArray[PersonType.NONWORKER].length >= 1)
                        return persons[personsByPersonTypeArray[PersonType.NONWORKER][0]].getAvailableWindow();
                    break;
                case 14:
                    if (personsByPersonTypeArray[PersonType.NONWORKER].length >= 2)
                        return persons[personsByPersonTypeArray[PersonType.NONWORKER][1]].getAvailableWindow();
                    break;
                case 15:
                    if (personsByPersonTypeArray[PersonType.NONWORKER].length >= 3)
                        return persons[personsByPersonTypeArray[PersonType.NONWORKER][2]].getAvailableWindow();
                    break;
                case 16:
                    if (personsByPersonTypeArray[PersonType.NONWORKER].length >= 4)
                        return persons[personsByPersonTypeArray[PersonType.NONWORKER][3]].getAvailableWindow();
                    break;
                case 17:
                    if (personsByPersonTypeArray[PersonType.SCHOOL_PRED].length >= 1)
                        return persons[personsByPersonTypeArray[PersonType.SCHOOL_PRED][0]].getAvailableWindow();
                    break;
                case 18:
                    if (personsByPersonTypeArray[PersonType.SCHOOL_PRED].length >= 2)
                        return persons[personsByPersonTypeArray[PersonType.SCHOOL_PRED][1]].getAvailableWindow();
                    break;
                case 19:
                    if (personsByPersonTypeArray[PersonType.SCHOOL_PRED].length >= 3)
                        return persons[personsByPersonTypeArray[PersonType.SCHOOL_PRED][2]].getAvailableWindow();
                    break;
                case 20:
                    if (personsByPersonTypeArray[PersonType.SCHOOL_PRED].length >= 4)
                        return persons[personsByPersonTypeArray[PersonType.SCHOOL_PRED][3]].getAvailableWindow();
                    break;
                case 21:
                    if (personsByPersonTypeArray[PersonType.SCHOOL_DRIV].length >= 1)
                        return persons[personsByPersonTypeArray[PersonType.SCHOOL_DRIV][0]].getAvailableWindow();
                    break;
                case 22:
                    if (personsByPersonTypeArray[PersonType.SCHOOL_DRIV].length >= 2)
                        return persons[personsByPersonTypeArray[PersonType.SCHOOL_DRIV][1]].getAvailableWindow();
                    break;
                case 23:
                    if (personsByPersonTypeArray[PersonType.SCHOOL_DRIV].length >= 3)
                        return persons[personsByPersonTypeArray[PersonType.SCHOOL_DRIV][2]].getAvailableWindow();
                    break;
                case 24:
                    if (personsByPersonTypeArray[PersonType.SCHOOL_DRIV].length >= 4)
                        return persons[personsByPersonTypeArray[PersonType.SCHOOL_DRIV][3]].getAvailableWindow();
                    break;
            }

            return 0;
        }

        /**
         * return the party composition code of the joint tour for which the UEC.solve() has been called
         */
        public float getPartyComp () {
            return jointTours[tourID].getTourCompositionCode();
        }

        /**
         * return the party size of the joint tour for which the UEC.solve() has been called
         */
        public float getPartySize () {
            return jointTours[tourID].getNumPersons();
        }

        /**
         * get number of joint tours code for the household
         *	  No of joint tours chosen by HH based on the outcome of the model 3.1:
         *		jTours==0, if M31==1
         *		jTours==1, if M31==2,3,4,5
         *		jTours==2, if M31==6,7,8,9,10,11,12,13,14,15
         */
        public float getJTours () {
            if (jointTours == null)
                return 0;
            else
                return jointTours.length;
        }

        /**
         * return maximum pair-wise overlap of available time windows for this person with other HH adults,
         */
        public float getMaxPairwiseOverlapAdult () {
            return persons[personID].getMaxAdultOverlaps();
        }

        /**
         * return maximum pair-wise overlap of available time windows for this person with other HH Children,
         */
        public float getMaxPairwiseOverlapChild () {
            return persons[personID].getMaxChildOverlaps();
        }

        /**
         * return number of joint shopping tours for this household
         */
        public float getJointShopTours () {
            return getJointToursByType( TourType.SHOP );
        }

        /**
         * return number of joint maintenance tours for this household
         */
        public float getJointMaintTours () {
            return getJointToursByType( TourType.OTHER_MAINTENANCE );
        }

        /**
         * return number of joint eating out tours for this household
         */
        public float getJointEatTours () {
            return getJointToursByType( TourType.EAT );
        }

        /**
         * get the number of joint tours in the Household for the tour type.
         */
        private int getJointToursByType ( int tourType ) {
            return jointToursByType[tourType];
        }

        /**
         * return 1 if an adult stays home; 0 otherwise.
         */
        public float getAdultAtHome () {
            for (int i=1; i < persons.length; i++) {
                if (persons[i].getPatternType() == PatternType.HOME &&
                    (persons[i].getPersonType() == PersonType.NONWORKER ||
                        persons[i].getPersonType() == PersonType.WORKER_F ||
                        persons[i].getPersonType() == PersonType.WORKER_P ))
                            return 1;
            }
            return 0;
        }

        /**
         * return 1 if a preschool or predriving child stays home; 0 otherwise.
         */
        public float getPreschoolPredrivHome () {
            for (int i=1; i < persons.length; i++) {
                if (persons[i].getPatternType() == PatternType.HOME &&
                    (persons[i].getPersonType() == PersonType.PRESCHOOL ||
                        persons[i].getPersonType() == PersonType.SCHOOL_PRED) )
                            return 1;
            }
            return 0;
        }

        /**
         * return 1 if a preschool child stays home; 0 otherwise.
         */
        public float getPreschoolHome () {
            for (int i=1; i < persons.length; i++) {
                if (persons[i].getPatternType() == PatternType.HOME &&
                    persons[i].getPersonType() == PersonType.PRESCHOOL)
                            return 1;
            }
            return 0;
        }

        /**
         * return 1 if a predriving child stays home; 0 otherwise.
         */
        public float getPreDrivHome () {
            for (int i=1; i < persons.length; i++) {
                if (persons[i].getPatternType() == PatternType.HOME &&
                    persons[i].getPersonType() == PersonType.SCHOOL_PRED)
                            return 1;
            }
            return 0;
        }

        /**
         * return 1 if a full-time worker stays home; 0 otherwise.
         */
        public float getFtWorkerHome () {
            for (int i=1; i < persons.length; i++) {
                if (persons[i].getPatternType() == PatternType.HOME &&
                    persons[i].getPersonType() == PersonType.WORKER_F )
                        return 1;
            }
            return 0;
        }

        /**
         * return 1 if a part-time worker stays home; 0 otherwise.
         */
        public float getPtWorkerHome () {
            for (int i=1; i < persons.length; i++) {
                if (persons[i].getPatternType() == PatternType.HOME &&
                    persons[i].getPersonType() == PersonType.WORKER_P )
                        return 1;
            }
            return 0;
        }

        /**
         * return 1 if a part-time worker or nonwaorker stays home; 0 otherwise.
         */
        public float getPtNonworkerHome () {
            for (int i=1; i < persons.length; i++) {
                if (persons[i].getPatternType() == PatternType.HOME &&
                    (persons[i].getPersonType() == PersonType.WORKER_P ||
                    persons[i].getPersonType() == PersonType.NONWORKER ) )
                        return 1;
            }
            return 0;
        }

        /**
         * return 1 if a full-time or part-time worker stays home; 0 otherwise.
         */
        public float getFtptWorkerHome () {
            for (int i=1; i < persons.length; i++) {
                if (persons[i].getPatternType() == PatternType.HOME &&
                    (persons[i].getPersonType() == PersonType.WORKER_F ||
                        persons[i].getPersonType() == PersonType.WORKER_P) )
                            return 1;
            }
            return 0;
        }



        /**
         * return 1 if a nonworker stays home; 0 otherwise.
         */
        public float getNonWorkerHome () {
            for (int i=1; i < persons.length; i++) {
                if (persons[i].getPatternType() == PatternType.HOME &&
                    persons[i].getPersonType() == PersonType.NONWORKER )
                        return 1;
            }
            return 0;
        }

        /**
         * return a count of the number of joint shopping tours for this person
         */
        public float getPersonJointShop () {
            return getNumberOfJointParticipationsByType ( TourType.SHOP );
        }

        /**
         * return a count of the number of joint maintenece tours for this person
         */
        public float getPersonJointMaint () {
            return getNumberOfJointParticipationsByType ( TourType.OTHER_MAINTENANCE );
        }

        /**
         * return a count of the number of joint discretionary tours for this person
         */
        public float getPersonJointDiscr () {
            return getNumberOfJointParticipationsByType ( TourType.DISCRETIONARY );
        }

        /**
         * return a count of the number of joint eating out tours for this person
         */
        public float getPersonJointEat () {
            return getNumberOfJointParticipationsByType ( TourType.EAT );
        }

        /**
         * return a count of the total number of joint tours for this person
         */
        public int getPersonJointTotal () {
            int count = 0;
            if (jointTours != null) {
                for (int i=0; i < jointTours.length; i++) {
                    for (int p=1; p < persons.length; p++) {
                        if ( jointTours[i].getPersonParticipation(p) )
                                count++;
                    }
                }
            }
            return count;
        }

        /**
         * return a count of the number of joint tours for this person for the specified tour type
         */
        private int getNumberOfJointParticipationsByType ( int tourType) {
            int count = 0;
            if (jointTours != null) {
                for (int i=0; i < jointTours.length; i++) {
                    for (int p=1; p < persons.length; p++) {
                        if ( jointTours[i].getPersonParticipation(p) &&
                            jointTours[i].getTourType() == tourType )
                                count++;
                    }
                }
            }
            return count;
        }




        /**
         * return a count of the number of individual non-mandatory shopping tours for this person
         */
        public float getPersonNonMandatoryShop () {
            return getNumberOfNonMandatoryParticipationsByType (TourType.SHOP );
        }

        /**
         * return a count of the number of individual non-mandatory maintenance tours for this person
         */
        public float getPersonNonMandatoryMaint () {

            return getNumberOfNonMandatoryParticipationsByType ( TourType.ESCORTING )
                + getNumberOfNonMandatoryParticipationsByType ( TourType.SHOP )
                + getNumberOfNonMandatoryParticipationsByType ( TourType.OTHER_MAINTENANCE );
        }

        /**
         * return a count of the number of individual non-mandatory discretionary tours for this person
         */
        public float getPersonNonMandatoryDiscr () {
            return getNumberOfNonMandatoryParticipationsByType ( TourType.DISCRETIONARY );
        }

        /**
         * return a count of the number of individual non-mandatory eating out tours for this person
         */
        public float getPersonNonMandatoryEat () {
            return getNumberOfNonMandatoryParticipationsByType ( TourType.EAT );
        }

        /**
         * return a count of the number of individual non-mandatory tours for this person for the specified tour type
         */
        private int getNumberOfNonMandatoryParticipationsByType ( int tourType) {
            int count = 0;
            if (indivTours != null) {
                for (int i=0; i < indivTours.length; i++) {
                    for (int p=1; p < persons.length; p++) {
                        if ( indivTours[i].getPersonParticipation(p) &&
                            indivTours[i].getTourType() == tourType )
                                count++;
                    }
                }
            }
            return count;
        }

        /**
         * return a count of the total number of individual non-mandatory tours for this person
         */
        public float getPersonNonMandatoryTotal () {
            int count = 0;
            if (indivTours != null) {
                for (int j=0; j < indivTours.length; j++) {
                    if ( indivTours[j].getPersonParticipation(personID) )
                            count++;
                }
            }
            return count;
        }

        /**
         * return a count of the total number of individual non-mandatory tours excluding escorting for this person
         */
        public float getPersonNonMandatoryTotalNoEscort () {
            int count = 0;
            if (indivTours != null) {
                for (int j=0; j < indivTours.length; j++) {
                    if ( indivTours[j].getTourType() != TourType.ESCORTING ) {
                        if ( indivTours[j].getPersonParticipation(personID) )
                            count++;
                    }
                }
            }
            return count;
        }

        /**
         * return a count of the number of mandatory work tours for this person
         */
        public float getPersonWork () {
            int count = 0;
            if (mandatoryTours != null) {
                for (int j=0; j < mandatoryTours.length; j++) {
                    if ( mandatoryTours[j].getTourType() == TourType.WORK ) {
                        if ( mandatoryTours[j].getPersonParticipation(personID) )
                            count++;
                    }
                }
            }
            return count;
        }

        /**
         * return 1 if the number workers or students with non-mandatory tours is > 0; 0 otherwise.
         */
        public float getWorkStudNonMandatoryTours () {
            for (int p=1; p < persons.length; p++) {
                if (persons[p].getPatternType() == PatternType.NON_MAND &&
                    ( persons[p].getPersonType() == PersonType.WORKER_F || persons[p].getPersonType() == PersonType.WORKER_P ) )
                        return 1;
            }
            return 0;
        }

        /**
         * return a count of the total number of mandatory tours for this person
         */
        public float getPersonMandatoryTotal () {
            int count = 0;
            if (mandatoryTours != null) {
                for (int j=0; j < mandatoryTours.length; j++) {
                    if ( mandatoryTours[j].getPersonParticipation(personID) )
                            count++;
                }
            }
            return count;
        }

        /**
         * set maximum time window for adults in the hh for household object
         */
        public void setMaxAdultWindow (int arg) {
            maxAdultWindow = (short)arg;
        }

        /**
         * return maximum time window for adults in the hh for household object
         */
        public float getMaxAdultWindow () {
            return maxAdultWindow;
        }

        /**
         * return available time window for this person.
         */
        public float getAvailTimeWindow () {
            return persons[personID].getAvailableWindow();
        }

        /**
         * return 1 if tour is a work tour and sov was the mode used.
         */
        public float getWorkModeSOVDummy () {
            if ( mandatoryTours[tourID].getTourType() == TourType.WORK &&
                mandatoryTours[tourID].getMode() == TourModeType.SOV )
                    return 1;
            else
                    return 0;
        }


        /**
         * set maximum time window for children in the hh for household object
         */
        public void setMaxChildWindow (int arg) {
            maxChildWindow = (short)arg;
        }

        /**
         * return maximum time window for children in the hh for household object
         */
        public float getMaxChildWindow () {
            return maxChildWindow;
        }




        /**
         * set hhType for this household object - number of non-preschool travelers
         */
        public void setHHType (int arg) {
            hhType = (short)arg;
        }

        /**
         * return hhType for this household object - number of non-preschool travelers
         */
        public int getHHType () {
            return hhType;
        }

        /**
         * set the Person[] for household object
         */
        public void setPersonArray (Person[] persons) {
            this.persons = persons;
        }


        /**
         * return the Person[] for household object
         */
        public Person[] getPersonArray () {
            return persons;
        }

        /**
         * set the total number of non-preschoolers making trips away from home for this household object
         */
        public void setTravelActiveNonPreschool (int arg) {
            travellingNonPreschool = (short)arg;
        }

        /**
         * return the total number of non-preschoolers making trips away from home for this household object
         */
        public int getTravelActiveNonPreschool () {
            return travellingNonPreschool;
        }

        /**
         * return the array of joint tours for this household object
         */
        public JointTour[] getJointTours () {
            return jointTours;
        }

        /**
         * set the array of individual tours for this household object
         */
        public void setIndivTours (Tour[] indivTours) {
            this.indivTours = indivTours;
        }

        /**
         * return the array of individual tours for this household object
         */
        public Tour[] getIndivTours () {
            return indivTours;
        }

        /**
         * set the array of individual mandatory tours for this household object
         */
        public void setMandatoryTours (Tour[] mandatoryTours) {
            this.mandatoryTours = mandatoryTours;
        }

        /**
         * return the array of individual mandatory tours for this household object
         */
        public Tour[] getMandatoryTours () {
            return mandatoryTours;
        }

        /**
         * return the length of the array of individual non-mandatory tours for this household object
         */
        public int getNumberOfIndivNonMandTours () {
            if (indivTours == null)
                return 0;
            else
                return indivTours.length;
        }

        /**
         * return the number of school tours for this household object
         */
        public int getNumberOfSchoolTours () {

            int count=0;
            if (mandatoryTours != null) {
                for (int i=0; i < mandatoryTours.length; i++)
                    if (mandatoryTours[i].getTourType() == TourType.SCHOOL)
                        count++;
            }
            return count;
        }

        /**
         * return the number of adults participating in this joint tour
         */
        public float getAdultsInJointTour () {
            return jointTours[tourID].getNumAdults();
        }

        /**
         * return the number of children participating in this joint tour
         */
        public float getChildrenInJointTour () {
            return jointTours[tourID].getNumAdults();
        }

        /**
         * return 1 if at least one preschool or predriving child participates in the joint tour
         */
        public float getPreschoolPredrivingInJointTour () {
            return ( jointTours[tourID].getNumPreschool() + jointTours[tourID].getNumPredriv() ) > 0 ? 1 : 0;
        }


        /**
         * return 1 if all adults in this joint tour are full time workers
         */
        public float getAllWorkFull () {
            return jointTours[tourID].getAllAdultsWorkFull() > 0 ? 1 : 0;
        }

        /**
         * return 1 if all adults in this joint tour are full time workers
         */
        public float getAllAdultsMakeWorkTour () {
            return allAdultsMakeWorkTour();
        }




        /**
         * increment the number of persons in joint tours in the Household by person type.
         */
        public void incrementPersonsByType ( int personType ) {
            personsByType[personType]++;
        }

        /**
         * increment the number of joint tours in the Household by tour type.
         */
        public void incrementJointToursByType ( int tourType ) {
            jointToursByType[tourType]++;
        }

        /**
         * increment the number of individual tours in the Household by tour type.
         */
        public void incrementIndivToursByType ( int tourType ) {
            indivToursByType[tourType]++;
        }

        /**
         * increment the number of mandatory individual tours in the Household by tour type.
         */
        public void incrementMandatoryToursByType ( int tourType ) {
            mandatoryToursByType[tourType]++;
        }

        /**
         * get the number of full time worker persons in the Household.
         */
        public int getFtwkPersons () {
            return personsByType[PersonType.WORKER_F];
        }

        /**
         * get the number of part time worker persons in the Household.
         */
        public int getPtwkPersons () {
            return personsByType[PersonType.WORKER_P];
        }



        /**
         * get the number of nonworker persons in the Household.
         */
        public int getNonwPersons () {
            return personsByType[PersonType.NONWORKER];
        }

        /**
         * get the number of preschool children in the Household.
         */
        public int getChpsPersons () {
            return personsByType[PersonType.PRESCHOOL];
        }

        /**
         * get the number of pre-driving children in the Household.
         */
        public int getChpdPersons () {
            return personsByType[PersonType.SCHOOL_PRED];
        }

        /**
         * get the number of driving age children in the Household.
         */
        public int getChdrPersons () {
            return personsByType[PersonType.SCHOOL_DRIV];
        }

        /**
         * get the number of persons in the Household for the person type.
         */
        public int getPersonsByType ( int personType ) {
            return personsByType[personType];
        }

        /**
         * get the number of individual tours in the Household for the tour type.
         */
        public int getIndivToursByType ( int tourType ) {
            return indivToursByType[tourType];
        }


        /**
         * set the array of person ids within each person type for this household object
         */
        public void setPersonsByPersonTypeArray () {

            // create an ArrayList of personTypes to accumulate person ids
            ArrayList[] personTypes = new ArrayList[PersonType.TYPES+1];
            for (int i=0; i < personTypes.length; i++)
                personTypes[i] = new ArrayList();

            // loop through persons in the household and add them to the ArrayList
            // corresponding to their personType.
            for (int p=1; p < persons.length; p++)
                personTypes[persons[p].getPersonType()].add( Integer.toString(p) );

            // save the ArrayList values into int arrays
            for (int t=1; t <= PersonType.TYPES; t++) {

                // allocate an int array for each personType
                personsByPersonTypeArray[t] = new int[personTypes[t].size()];

                // retrieve the objects from the ArrayLists and store them in an int array
                // for the personType
                for (int p=0; p < personTypes[t].size(); p++) {
                    String s = (String)( personTypes[t].get(p) );
                    personsByPersonTypeArray[t][p] = Integer.parseInt(s);
                }
            }

        }

        /**
         * return the array of person ids within each person type for this household object
         */
        public int[][] getPersonsByPersonTypeArray () {
            return personsByPersonTypeArray;
        }


        public int getPersonsAtHome (int personType){
            int count=0;
            for (int i=1; i<persons.length; i++) {
                if (personType == persons[i].getPersonType()) {
                    if (persons[i].getPatternType() == PatternType.HOME){
                          count++;
                    }
                }

            }
            return count;
        }


    /**
     * return 1 if every adult in HH makes at least one work tour; 0 otherwise or if no adults.
     */
    public int allAdultsMakeWorkTour () {

        int p;
        int adultCount = 0;
        boolean[] adultMakesWorkTour = new boolean[persons.length];


        if (mandatoryTours != null) {

            // create a person array with true if person is adult and makes a work tour
            for (int i=0; i < mandatoryTours.length; i++) {
                if (mandatoryTours[i].getTourType() == TourType.WORK) {
                    p = mandatoryTours[i].getTourPerson();
                    if (persons[p].getPersonType() == PersonType.WORKER_F ||
                        persons[p].getPersonType() == PersonType.WORKER_P ||
                        persons[p].getPersonType() == PersonType.NONWORKER)
                            adultMakesWorkTour[p] = true;
                }
            }

            // loop through persons, if they're adults and adultMakesWorkTour[] is false, then return 0
            for (p=1; p < persons.length; p++) {
                if ( persons[p].getPersonType() == PersonType.WORKER_F ||
                       persons[p].getPersonType() == PersonType.WORKER_P  ||
                     persons[p].getPersonType() == PersonType.NONWORKER ) {
                         adultCount++;
                        if (!adultMakesWorkTour[p] )
                            return 0;
                 }
            }

        }

        // if we get here then either all adults made a work tour, or no adults were in household
        if (adultCount == 0)
            return 0;
        else
            return 1;
    }




    /**
     * return the total size for the given destination choice alternative for work purpose
    */
    public float getWorkDcSizeAlt (int alt) {
        return getDcSizeAlt ( TourType.WORK, alt );
    }



    /**
     * return the total size for the given destination choice alternative for school purpose
    */
    public float getSchoolDcSizeAlt (int alt) {
        return getDcSizeAlt ( TourType.SCHOOL, alt );
    }

    /**
     * added by crf to allow the getting of the school enrollment by taz and by school type
     */
    public float getElementaryEnrollmentDcAlt (int alt) {
        return ZonalDataManager.schoolEnrollment[0][ZonalDataManager.zoneAlt[alt]];
    }
    public float getMiddleSchoolEnrollmentDcAlt (int alt) {
        return ZonalDataManager.schoolEnrollment[1][ZonalDataManager.zoneAlt[alt]];
    }
    public float getHighSchoolEnrollmentDcAlt (int alt) {
        return ZonalDataManager.schoolEnrollment[2][ZonalDataManager.zoneAlt[alt]];
    }
    public float getCollegeEnrollmentDcAlt (int alt) {
        return ZonalDataManager.schoolEnrollment[3][ZonalDataManager.zoneAlt[alt]];
    }

    /**
     * return the total size for the given destination choice alternative for escorting purpose
    */
    public float getEscortDcSizeAlt (int alt) {
        return getDcSizeAlt ( TourType.ESCORTING, alt );
    }

    /**
     * return the total size for the given destination choice alternative for shopping purpose
    */
    public float getShopDcSizeAlt (int alt) {
        return getDcSizeAlt ( TourType.SHOP, alt );
    }

    /**
     * return the total size for the given destination choice alternative for maintenance purpose
    */
    public float getMaintDcSizeAlt (int alt) {
        return getDcSizeAlt ( TourType.OTHER_MAINTENANCE, alt );
    }

    /**
     * return the total size for the given destination choice alternative for discretionary purpose
    */
    public float getDiscrDcSizeAlt (int alt) {
        return getDcSizeAlt ( TourType.DISCRETIONARY, alt );
    }

    /**
     * return the total size for the given destination choice alternative for eating out purpose
    */
    public float getEatDcSizeAlt (int alt) {
        return getDcSizeAlt ( TourType.EAT, alt );
    }

    /**
     * return the total size for the given destination choice alternative for at-work purpose
    */
    public float getAtWorkDcSizeAlt (int alt) {
        return getDcSizeAlt ( TourType.ATWORK, alt );
    }

    /**
     * return the total size for the given destination choice alternative for the given purpose
    */
    private float getDcSizeAlt (int purpose, int alt) {
        return ZonalDataManager.totSize[purpose][alt];
    }



    /**
     * return the size for the given ob stop location choice alternative for work purpose
    */
    public float getSizeStopWorkOBAlt (int alt) {
        return getSizeStopAlt (0, TourType.WORK, alt);
    }


    /**
     * return the size for the given ob stop location choice alternative for school purpose
    */
    public float getSizeStopSchoolOBAlt (int alt) {
        return getSizeStopAlt (0, TourType.SCHOOL, alt);
    }

    /**
     * return the size for the given ob stop location choice alternative for escort purpose
    */
    public float getSizeStopEscortOBAlt (int alt) {
        return getSizeStopAlt (0, TourType.ESCORTING, alt);
    }

    /**
     * return the size for the given ob stop location choice alternative for shop purpose
    */
    public float getSizeStopShopOBAlt (int alt) {
        return getSizeStopAlt (0, TourType.SHOP, alt);
    }

    /**
     * return the size for the given ob stop location choice alternative for maintenance purpose
    */
    public float getSizeStopMaintOBAlt (int alt) {
        return getSizeStopAlt (0, TourType.OTHER_MAINTENANCE, alt);
    }

    /**
     * return the size for the given ob stop location choice alternative for discretionary purpose
    */
    public float getSizeStopDiscrOBAlt (int alt) {
        return getSizeStopAlt (0, TourType.DISCRETIONARY, alt);
    }

    /**
     * return the size for the given ob stop location choice alternative for eat out purpose
    */
    public float getSizeStopEatOBAlt (int alt) {
        return getSizeStopAlt (0, TourType.EAT, alt);
    }

    /**
     * return the size for the given ob stop location choice alternative for at-work purpose
    */
    public float getSizeStopAtworkOBAlt (int alt) {
        return getSizeStopAlt (0, TourType.ATWORK, alt);
    }

    /**
     * return the total size for the given destination choice alternative for the given purpose
    */
    public float getSizeStopTotalOBAlt (int alt) {
        return ZonalDataManager.stopTotSize[0][alt];
    }

    /**
     * return the size for the given ib stop location choice alternative for work purpose
    */
    public float getSizeStopWorkIBAlt (int alt) {
        return getSizeStopAlt (1, TourType.WORK, alt);
    }

    /**
     * return the size for the given ib stop location choice alternative for school purpose
    */
    public float getSizeStopSchoolIBAlt (int alt) {
        return getSizeStopAlt (1, TourType.SCHOOL, alt);
    }

    /**
     * return the size for the given ib stop location choice alternative for escort purpose
    */
    public float getSizeStopEscortIBAlt (int alt) {
        return getSizeStopAlt (1, TourType.ESCORTING, alt);
    }

    /**
     * return the size for the given ib stop location choice alternative for shop purpose
    */
    public float getSizeStopShopIBAlt (int alt) {
        return getSizeStopAlt (1, TourType.SHOP, alt);
    }

    /**
     * return the size for the given ib stop location choice alternative for maintenance purpose
    */
    public float getSizeStopMaintIBAlt (int alt) {
        return getSizeStopAlt (1, TourType.OTHER_MAINTENANCE, alt);
    }

    /**
     * return the size for the given ib stop location choice alternative for discretionary purpose
    */
    public float getSizeStopDiscrIBAlt (int alt) {
        return getSizeStopAlt (1, TourType.DISCRETIONARY, alt);
    }

    /**
     * return the size for the given ib stop location choice alternative for eat out purpose
    */
    public float getSizeStopEatIBAlt (int alt) {
        return getSizeStopAlt (1, TourType.EAT, alt);
    }

    /**
     * return the size for the given ib stop location choice alternative for at-work purpose
    */
    public float getSizeStopAtworkIBAlt (int alt) {
        return getSizeStopAlt (1, TourType.ATWORK, alt);
    }

    /**
     * return the total size for the given destination choice alternative for the given purpose
    */
    public float getSizeStopTotalIBAlt (int alt) {
        return ZonalDataManager.stopTotSize[1][alt];
    }

    /**
     * return the total size for the given destination choice alternative for the given purpose
    */
    private float getSizeStopAlt (int dir, int purpose, int alt) {
        return ZonalDataManager.stopSize[dir][purpose][alt];
    }



    /**
     * return the OD related mode choice utility for the given mode choice alternative
    */
    public float getODUtilModeAlt (int alt) {
        return (float)ZonalDataManager.odUtilModeAlt[alt-1];
    }

    //added by crf to give access to taz number from alternative number in destination choice model
    /**
     * return the taz number associated with a particular dc alternative number
     */
    public int getAltZone (int alt) {
        return ZonalDataManager.zoneAlt[alt];
    }

    //added by crf to give access to the shadow price variable
    /**
     * returns the shadow price associated with a particular zone
     */
    public float getShadowPrice (int alt) {
        return ZonalDataManager.shadowPrice[ZonalDataManager.zoneAlt[alt]];
    }

     /**
     * return the short walk access for the given destination choice alternative
    */
    public float getZonalShortWalkAccessDestAlt (int alt) {
        return ZonalDataManager.zonalShortAccess[alt];
    }

    public float getZonalShortWalkAccessOrig () {
            return originWalkSegment;
    }

    /**
	 * return the short walk access for the chosen destination alternative for this tour
	*/
	public float getZonalShortWalkAccessDest () {
		return chosenWalkSegment;
	}

    /**
     * return the urban type for the given destination choice alternative
    */
    public float getUrbTypeDestAlt (int alt) {
        return ZonalDataManager.urbType[alt];
    }

    public float getUrbTypeDest () {
        return ZonalDataManager.urbType[chosenDest];
    }

    /**
     * return the urban type for the origin
     */
    public float getUrbTypeOrig () {
        return ZonalDataManager.urbType[origTaz];
    }     

    /**
     * return the number of joint + non mandatory tours for this household
    */
    public float getNumJointNonMandToursHH () {
        int count = 0;
        if (jointTours != null)
            count += jointTours.length;
        if (indivTours != null)
            count += indivTours.length;
        return count;
    }

    /**
     * return the number of joint + non mandatory tours for this person
    */
    public float getNumJointNonMandToursPerson () {
        return persons[personID].getNumJointTours() + persons[personID].getNumIndNonMandTours();
    }

    /**
     * return the pattern type of the first person participating in this joint tour
    */
    public float getJointTourPatternTypePerson1 () {
        int[] jtPersons = jointTours[tourID].getJointTourPersons();
        return persons[jtPersons[0]].getPatternType();
    }

    /**
     * return the pattern type of the second person participating in this joint tour
    */
    public float getJointTourPatternTypePerson2 () {
        int[] jtPersons = jointTours[tourID].getJointTourPersons();
        return persons[jtPersons[1]].getPatternType();
    }


    /**
     * return the am/pm mode choice logsum for the given destination choice alternative
    */
    public float getLogsumAMPMDestAlt (int alt) {
        return ZonalDataManager.logsumDcAMPM[alt];
    }

    /**
     * return the am/md mode choice logsum for the given destination choice alternative
    */
    public float getLogsumAMMDDestAlt (int alt) {
        return ZonalDataManager.logsumDcAMMD[alt];
    }

    /**
     * return the md/md mode choice logsum for the given destination choice alternative
    */
    public float getLogsumMDMDDestAlt (int alt) {
        return ZonalDataManager.logsumDcMDMD[alt];
    }

    /**
     * return the pm/nt mode choice logsum for the given destination choice alternative
    */
    public float getLogsumPMNTDestAlt (int alt) {
        return ZonalDataManager.logsumDcPMNT[alt];
    }

    /**
     * return the pm/nt mode choice logsum for the given destination choice alternative
    */
    public float getLogsumNTNTDestAlt (int alt) {
        return ZonalDataManager.logsumDcNTNT[alt];
    }

    /**
     * return the ea/ea mode choice logsum for the given time-of-day choice alternative
    */
    public float getLogsumEAEATODAlt (int alt) {
        return TODDataManager.logsumTcEAEA[alt];
    }

    /**
     * return the ea/am mode choice logsum for the given time-of-day choice alternative
    */
    public float getLogsumEAAMTODAlt (int alt) {
        return TODDataManager.logsumTcEAAM[alt];
    }

    /**
     * return the ea/md mode choice logsum for the given time-of-day choice alternative
    */
    public float getLogsumEAMDTODAlt (int alt) {
        return TODDataManager.logsumTcEAMD[alt];
    }

    /**
     * return the ea/pm mode choice logsum for the given time-of-day choice alternative
    */
    public float getLogsumEAPMTODAlt (int alt) {
        return TODDataManager.logsumTcEAPM[alt];
    }

    /**
     * return the ea/nt mode choice logsum for the given time-of-day choice alternative
    */
    public float getLogsumEANTTODAlt (int alt) {
        return TODDataManager.logsumTcEANT[alt];
    }

    /**
     * return the am/am mode choice logsum for the given time-of-day choice alternative
    */
    public float getLogsumAMAMTODAlt (int alt) {
        return TODDataManager.logsumTcAMAM[alt];
    }

    /**
     * return the am/md mode choice logsum for the given time-of-day choice alternative
    */
    public float getLogsumAMMDTODAlt (int alt) {
        return TODDataManager.logsumTcAMMD[alt];
    }

    /**
     * return the am/pm mode choice logsum for the given time-of-day choice alternative
    */
    public float getLogsumAMPMTODAlt (int alt) {
        return TODDataManager.logsumTcAMPM[alt];
    }

    /**
     * return the am/nt mode choice logsum for the given time-of-day choice alternative
    */
    public float getLogsumAMNTTODAlt (int alt) {
        return TODDataManager.logsumTcAMNT[alt];
    }

    /**
     * return the md/md mode choice logsum for the given time-of-day choice alternative
    */
    public float getLogsumMDMDTODAlt (int alt) {
        return TODDataManager.logsumTcMDMD[alt];
    }

    /**
     * return the md/pm mode choice logsum for the given time-of-day choice alternative
    */
    public float getLogsumMDPMTODAlt (int alt) {
        return TODDataManager.logsumTcMDPM[alt];
    }

    /**
     * return the md/nt mode choice logsum for the given time-of-day choice alternative
    */
    public float getLogsumMDNTTODAlt (int alt) {
        return TODDataManager.logsumTcMDNT[alt];
    }

    /**
     * return the pm/pm mode choice logsum for the given time-of-day choice alternative
    */
    public float getLogsumPMPMTODAlt (int alt) {
        return TODDataManager.logsumTcPMPM[alt];
    }

    /**
     * return the pm/nt mode choice logsum for the given time-of-day choice alternative
    */
    public float getLogsumPMNTTODAlt (int alt) {
        return TODDataManager.logsumTcPMNT[alt];
    }

    /**
     * return the nt/nt mode choice logsum for the given time-of-day choice alternative
    */
    public float getLogsumNTNTTODAlt (int alt) {
        return TODDataManager.logsumTcNTNT[alt];
    }


    /**
     * return the ob stop location choice logsum for the given stop frequency choice alternative
    */
    public float getLogsumSlcOB () {
        return StopsModelBase.slcLogsum[0];
    }

    /**
     * return the ib stop location choice logsum for the given stop frequency choice alternative
    */
    public float getLogsumSlcIB () {
        return StopsModelBase.slcLogsum[1];
    }


    public float getAllAdultsAtHome() {
        boolean allAdultsAtHome = true;
        for (Person p : persons) {
            if (p == null) continue;
            if (p.getPersonType() < 4) {
                allAdultsAtHome = allAdultsAtHome && (p.getPatternType() == 7);
            }
        }
        return allAdultsAtHome ? 1.0f : 0.0f;
    }






    /**
     * return the TOD period for the outbound half-tour of this tour
    */
    public float getTodOut () {
        int todOut = 0;

        if (tourCategory == TourType.MANDATORY_CATEGORY)
            todOut = TODDataManager.getTodStartPeriod( mandatoryTours[tourID].getTimeOfDayAlt() );
        else if (tourCategory == TourType.JOINT_CATEGORY)
            todOut = TODDataManager.getTodStartPeriod( jointTours[tourID].getTimeOfDayAlt() );
        else if (tourCategory == TourType.NON_MANDATORY_CATEGORY)
            todOut = TODDataManager.getTodStartPeriod( indivTours[tourID].getTimeOfDayAlt() );
        else if (tourCategory == TourType.AT_WORK_CATEGORY)
            todOut = TODDataManager.getTodStartPeriod( mandatoryTours[tourID].subTours[subtourID].getTimeOfDayAlt() );

        return todOut;
    }

    /**
     * return the TOD period for the inbound half-tour of this tour
    */
    public float getTodIn () {
        int todIn = 0;

        if (tourCategory == TourType.MANDATORY_CATEGORY)
            todIn = TODDataManager.getTodEndPeriod( mandatoryTours[tourID].getTimeOfDayAlt() );
        else if (tourCategory == TourType.JOINT_CATEGORY)
            todIn = TODDataManager.getTodEndPeriod( jointTours[tourID].getTimeOfDayAlt() );
        else if (tourCategory == TourType.NON_MANDATORY_CATEGORY)
            todIn = TODDataManager.getTodEndPeriod( indivTours[tourID].getTimeOfDayAlt() );
        else if (tourCategory == TourType.AT_WORK_CATEGORY)
            todIn = TODDataManager.getTodEndPeriod( mandatoryTours[tourID].subTours[subtourID].getTimeOfDayAlt() );

        return todIn;
    }

    /**
     * return tour duration determined from the TOD model
     */
    public float getTourDuration () {
        int startHr = 0;
        int endHr = 0;

        if (tourCategory == TourType.MANDATORY_CATEGORY) {
            startHr = TODDataManager.getTodStartHour( mandatoryTours[tourID].getTimeOfDayAlt() );
            endHr = TODDataManager.getTodEndHour( mandatoryTours[tourID].getTimeOfDayAlt() );
        }
        else if (tourCategory == TourType.JOINT_CATEGORY) {
            startHr = TODDataManager.getTodStartHour( jointTours[tourID].getTimeOfDayAlt() );
            endHr = TODDataManager.getTodEndHour( jointTours[tourID].getTimeOfDayAlt() );
        }
        else if (tourCategory == TourType.NON_MANDATORY_CATEGORY) {
            startHr = TODDataManager.getTodStartHour( indivTours[tourID].getTimeOfDayAlt() );
            endHr = TODDataManager.getTodEndHour( indivTours[tourID].getTimeOfDayAlt() );
        }
        else if (tourCategory == TourType.AT_WORK_CATEGORY) {
            startHr = TODDataManager.getTodStartHour( mandatoryTours[tourID].subTours[subtourID].getTimeOfDayAlt() );
            endHr = TODDataManager.getTodEndHour( mandatoryTours[tourID].subTours[subtourID].getTimeOfDayAlt() );
        }

        return endHr - startHr;
    }




    /**
     * set the default TOD alternatives for this mandatory tour.
     * the start period, end period, and duration derived from these alternative numbers are the defaults.
     */
    public void setTODDefaults ( int tourcategory, String TimePeriodCombo ) {

        Tour tour=null;

        if ( tourCategory == TourType.MANDATORY_CATEGORY )
            tour = mandatoryTours[tourID];
        else if ( tourCategory == TourType.JOINT_CATEGORY )
            tour = jointTours[tourID];
        else if ( tourCategory == TourType.NON_MANDATORY_CATEGORY )
            tour = indivTours[tourID];
        else if ( tourCategory == TourType.AT_WORK_CATEGORY )
            tour = mandatoryTours[tourID].subTours[subtourID];


        if (TimePeriodCombo.equals("EaEa")) {
            tour.setTimeOfDayAlt ( 2 );
            setChosenTodAlt ( 2 );
        }
        else if (TimePeriodCombo.equals("EaAm")) {
            tour.setTimeOfDayAlt ( 4 );
            setChosenTodAlt ( 4 );
        }
        else if (TimePeriodCombo.equals("EaMd")) {
            tour.setTimeOfDayAlt ( 8 );
            setChosenTodAlt ( 8 );
        }
        else if (TimePeriodCombo.equals("EaPm")) {
            tour.setTimeOfDayAlt ( 12 );
            setChosenTodAlt ( 12 );
        }
        else if (TimePeriodCombo.equals("EaNt")) {
            tour.setTimeOfDayAlt ( 17 );
            setChosenTodAlt ( 17 );
        }
        else if (TimePeriodCombo.equals("AmAm")) {
            tour.setTimeOfDayAlt ( 40 );
            setChosenTodAlt ( 40 );
        }
        else if (TimePeriodCombo.equals("AmMd")) {
            tour.setTimeOfDayAlt ( 60 );
            setChosenTodAlt ( 60 );
        }
        else if (TimePeriodCombo.equals("AmPm")) {
            tour.setTimeOfDayAlt ( 64 );
            setChosenTodAlt ( 64 );
        }
        else if (TimePeriodCombo.equals("AmNt")) {
            tour.setTimeOfDayAlt ( 68 );
            setChosenTodAlt ( 68 );
        }
        else if (TimePeriodCombo.equals("MdMd")) {
            tour.setTimeOfDayAlt ( 104 );
            setChosenTodAlt ( 104 );
        }
        else if (TimePeriodCombo.equals("MdPm")) {
            tour.setTimeOfDayAlt ( 106 );
            setChosenTodAlt ( 106 );
        }
        else if (TimePeriodCombo.equals("MdNt")) {
            tour.setTimeOfDayAlt ( 122 );
            setChosenTodAlt ( 122 );
        }
        else if (TimePeriodCombo.equals("PmPm")) {
            tour.setTimeOfDayAlt ( 158 );
            setChosenTodAlt ( 158 );
        }
        else if (TimePeriodCombo.equals("PmNt")) {
            tour.setTimeOfDayAlt ( 167 );
            setChosenTodAlt ( 167 );
        }
        else if (TimePeriodCombo.equals("NtNt")) {
            tour.setTimeOfDayAlt ( 179 );
            setChosenTodAlt ( 179 );
        }

    }


    /**
     * return 1 if work tour has more than 1 subtour; 0 otherwise.
    */
    public float getTwoAtworkBinary () {
        int returnValue=0;

        if (mandatoryTours[tourID].subTours.length > 1)
            returnValue = 1;
        else
            returnValue = 0;

        return returnValue;
    }

     public void setStopLeg (int leg){
         this.stopLeg = leg;
     }

     public int getStopLeg(){
         return stopLeg;
     }

    public int getSeason() {
        return season;
    }

    /**
     * print the value of all the attributes in this object to the baseLogger
     */
    public void printHouseholdState () {

        logger.info( "");
        logger.info( "Household object information for hh ID = " + ID);
        logger.info( "---------------------------------------------");

        logger.info( "(int) taz = " + taz);
        logger.info( "(float) income = " + income);
        logger.info( "(short) size = " + size);
        logger.info( "(short) hhType = " + hhType);
        logger.info( "(short) maxAdultOverlaps = " + maxAdultOverlaps);
        logger.info( "(short) maxChildOverlaps = " + maxChildOverlaps);
        logger.info( "(short) maxMixedOverlaps = " + maxMixedOverlaps);
        logger.info( "(short) maxAdultWindow = " + maxAdultWindow);
        logger.info( "(short) maxChildWindow = " + maxChildWindow);
        logger.info( "(short) travellingAdults = " + travellingAdults);
        logger.info( "(short) travellingChildren = " + travellingChildren);
        logger.info( "(short) travellingNonPreschool = " + travellingNonPreschool);

        logger.info( "(short[]) mandatoryToursByType = ");
        if (mandatoryToursByType != null) {
            for (int i=0; i < mandatoryToursByType.length; i++)
                logger.info( "mandatoryToursByType[" + i + "] =" + mandatoryToursByType[i]);
        }
        else {
            logger.info( "mandatoryToursByType = null");
        }

        logger.info( "(short[]) jointToursByType = ");
        if (jointToursByType != null) {
            for (int i=0; i < jointToursByType.length; i++)
                logger.info( "jointToursByType[" + i + "] =" + jointToursByType[i]);
        }
        else {
            logger.info( "jointToursByType = null");
        }

        logger.info( "(short[]) indivToursByType = ");
        if (indivToursByType != null) {
            for (int i=0; i < indivToursByType.length; i++)
                logger.info( "indivToursByType[" + i + "] =" + indivToursByType[i]);
        }
        else {
            logger.info( "indivToursByType = null");
        }

        logger.info( "(int[][]) personsByPersonTypeArray = ");
        if (personsByPersonTypeArray != null) {
            for (int i=0; i < personsByPersonTypeArray.length; i++) {
                if (personsByPersonTypeArray[i] != null) {
                    for (int j=0; j < personsByPersonTypeArray[i].length; j++)
                        logger.info( "personsByPersonTypeArray[" + i + "][" + j + "] =" + personsByPersonTypeArray[i][j]);
                }
                else {
                    logger.info( "personsByPersonTypeArray[" + i + "] = null");
                }
            }
        }
        else {
            logger.info( "personsByPersonTypeArray = null");
        }

        if (persons != null) {
            for (int i=1; i < persons.length; i++) {
                if (persons[i] != null)
                    persons[i].printPersonState();
                else
                    logger.info( "persons[" + i + "] = null");
            }
        }
        else {
            logger.info( "persons = null");
        }

        if (mandatoryTours != null) {
            for (int i=0; i < mandatoryTours.length; i++) {
                if (mandatoryTours[i] != null)
                    mandatoryTours[i].printTourState();
                else
                    logger.info( "mandatoryTours[" + i + "] = null");
            }
        }
        else {
            logger.info( "mandatoryTours = null");
        }

        if (jointTours != null) {
            for (int i=0; i < jointTours.length; i++) {
                if (jointTours[i] != null)
                    jointTours[i].printJointTourState();
                else
                    logger.info( "jointTours[" + i + "] = null");
            }
        }
        else {
            logger.info( "jointTours = null");
        }

        if (indivTours != null) {
            for (int i=0; i < indivTours.length; i++) {
                if (indivTours[i] != null)
                    indivTours[i].printTourState();
                else
                    logger.info( "indivTours[" + i + "] = null");
            }
        }
        else {
            logger.info( "indivTours = null");
        }
    }

    public void writeContentToLogger(Logger logger){
      logger.info("Contents of Household object with Index: "+ID);
      logger.info("taz: "+taz);
      logger.info("origTaz: "+origTaz);
      logger.info("tourCategory: "+tourCategory);
      logger.info("personID: "+personID);
      logger.info("tourID: "+tourID);
      logger.info("subtourID: "+subtourID);
      logger.info("chosenDest: "+chosenDest);
      logger.info("originWalkSegment: "+originWalkSegment);
      logger.info("chosenWalkSegment: "+chosenWalkSegment);
      logger.info("chosenTodAlt: "+chosenTodAlt);
      logger.info("income: "+income);
      logger.info("size: "+size);
      logger.info("autoOwnership: "+autoOwnership);
      logger.info("hhType: "+hhType);
      logger.info("maxAdultOverlaps: "+maxAdultOverlaps);
      logger.info("maxChildOverlaps: "+maxChildOverlaps);
      logger.info("maxMixedOverlaps: "+maxMixedOverlaps);
      logger.info("maxAdultWindow: "+maxAdultWindow);
      logger.info("maxChildWindow: "+maxChildWindow);
      logger.info("travellingAdults: "+travellingAdults);
      logger.info("travellingChildren: "+travellingChildren);
      logger.info("travellingNonPreschool: "+travellingNonPreschool);
      logger.info("maxChildOverlaps: "+maxChildOverlaps);

      if (persons != null) {
          for(int i=1; i<persons.length; i++){
            Person temp=persons[i];
            if (temp != null)
                temp.writeContentToLogger(logger);
          }
      }
      else {
          logger.info("persons[]=null");
      }

      if (personsByType != null) {
          for(int i=0; i<personsByType.length; i++){
              logger.info("personsByType["+i+"]="+personsByType[i]);
          }
      }
      else {
          logger.info("personsByType[]=null");
      }

      if (personsByPersonTypeArray != null) {
          int nRows=personsByPersonTypeArray.length;
          for(int i=0; i<nRows; i++){
            int nCols=0;
            if(personsByPersonTypeArray[i]!=null){
                nCols = personsByPersonTypeArray[i].length;
            }else{
                logger.info("personsByPersonTypeArray["+i+"]=null");
            }
            for(int j=0; j<nCols; j++){
                logger.info("personsByPersonTypeArray["+i+"]["+j+"]="+personsByPersonTypeArray[i][j]);
            }
          }
      }
      else {
          logger.info("personsByPersonTypeArray[]=null");
      }

     if (jointTours != null) {
         for(int i=0; i<jointTours.length; i++){
           JointTour temp=jointTours[i];
           if (temp != null)
               temp.writeContentToLogger(logger, "joint tour " + i);
         }
     }
     else {
         logger.info("jointTours[]=null");
     }

     if (jointToursByType != null) {
         for(int i=0; i<jointToursByType.length; i++){
           logger.info("jointToursByType["+i+"]="+jointToursByType[i]);
         }
     }
     else {
        logger.info("jointToursByType[]=null");
     }

     if (indivTours != null) {
         for(int i=0; i<indivTours.length; i++){
           Tour temp=indivTours[i];
           if (temp != null)
               temp.writeContentToLogger(logger, "indiv tour " + i);
         }
     }
     else {
        logger.info("indivTours[]=null");
     }

     if (indivToursByType != null) {
         for(int i=0; i<indivToursByType.length; i++){
           logger.info("indivToursByType["+i+"]="+indivToursByType[i]);
         }
     }
     else {
        logger.info("indivToursByType[]=null");
     }

     if (mandatoryTours != null) {
         for(int i=0; i<mandatoryTours.length; i++){
           Tour temp=mandatoryTours[i];
           if (temp != null)
               temp.writeContentToLogger(logger, "mandatory tour " + i);
         }
     }
     else {
         logger.info("mandatoryTours[]=null");
     }

     if (mandatoryToursByType != null) {
         for(int i=0; i<mandatoryToursByType.length; i++){
           logger.info("mandatoryToursByType["+i+"]="+mandatoryToursByType[i]);
         }
     }
     else {
         logger.info("mandatoryToursByType[]=null");
     }


    }

    /**
     *
     * @see java.io.Externalizable#readExternal(java.io.ObjectInput)
     */
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        int length;

        ID = in.readInt();
        taz = in.readShort();
        origTaz = in.readShort();

        tourCategory = in.readShort();
        personID = in.readShort();
        tourID = in.readShort();
        subtourID = in.readShort();
        chosenDest = in.readShort();
        originWalkSegment = in.readShort();
        chosenWalkSegment = in.readShort();
        chosenTodAlt = in.readShort();

        income = in.readFloat();
        size = in.readShort();
        autoOwnership = in.readShort();
        hhType = in.readShort();

        maxAdultOverlaps = in.readShort();
        maxChildOverlaps = in.readShort();
        maxMixedOverlaps = in.readShort();
        maxAdultWindow = in.readShort();
        maxChildWindow = in.readShort();
        travellingAdults = in.readShort();
        travellingChildren = in.readShort();
        travellingNonPreschool = in.readShort();

        //persons[]
        length = in.readInt();
        if (length > 0) {
            persons = new Person[length];
            for (int i=0; i < length; i++) {
                persons[i] = (Person) in.readObject();
            }
        }

        personsByType = ObjectUtil.readShortArray(in);

        //personsByPersonTypeArray[]
        int nRows = in.readInt();
        if (nRows == -1) {
            //do nothing - array was null
        }
        else {
            for (int i=0; i < nRows; i++) {
                int nCols = in.readInt();
                if (nCols == -1)
                    continue;
                personsByPersonTypeArray[i] = new int[nCols];
                for (int j=0; j < nCols; j++) {
                    personsByPersonTypeArray[i][j] = in.readInt();
                }
            }
        }

        //jointTours[]
        length = in.readInt();
        if (length > 0) {
            jointTours = new JointTour[length];
            for (int i=0; i < length; i++) {
                jointTours[i] = (JointTour) in.readObject();
            }
        }

        jointToursByType = ObjectUtil.readShortArray(in);

        //indivTours[]
        length = in.readInt();
        if (length > 0) {
            indivTours = new Tour[length];
            for (int i=0; i < length; i++) {
                indivTours[i] = (Tour) in.readObject();
            }
        }

        indivToursByType = ObjectUtil.readShortArray(in);

        //mandatoryTours[]
        length = in.readInt();
        if (length > 0) {
            mandatoryTours = new Tour[length];
            for (int i=0; i < length; i++) {
                mandatoryTours[i] = (Tour) in.readObject();
            }
        }

        mandatoryToursByType = ObjectUtil.readShortArray(in);

    }





    /**
     * Each variable is written out in the order that it was declared in the class.
     *
     * @see java.io.Externalizable#writeExternal(java.io.ObjectOutput)
     */
    public void writeExternal(ObjectOutput out) throws IOException {
        out.writeInt(ID);
        out.writeShort(taz);
        out.writeShort(origTaz);

        out.writeShort(tourCategory);
        out.writeShort(personID);
        out.writeShort(tourID);
        out.writeShort(subtourID);
        out.writeShort(chosenDest);
        out.writeShort(originWalkSegment);
        out.writeShort(chosenWalkSegment);
        out.writeShort(chosenTodAlt);

        out.writeFloat(income);
        out.writeShort(size);
        out.writeShort(autoOwnership);
        out.writeShort(hhType);

        out.writeShort(maxAdultOverlaps);
        out.writeShort(maxChildOverlaps);
        out.writeShort(maxMixedOverlaps);
        out.writeShort(maxAdultWindow);
        out.writeShort(maxChildWindow);
        out.writeShort(travellingAdults);
        out.writeShort(travellingChildren);
        out.writeShort(travellingNonPreschool);

        ObjectUtil.writeObjectArray(out, persons);
        ObjectUtil.writeShortArray(out, personsByType);

        //personsByPersonTypeArray - a two-dimensional array
        if (personsByPersonTypeArray == null) {
            out.writeInt(-1);  //signal null array
        }
        else {
            int nRows = personsByPersonTypeArray.length;
            out.writeInt(nRows);
            for (int i=0; i < nRows; i++) {
                int nCols = -1;
                if (personsByPersonTypeArray[i] != null)
                    nCols = personsByPersonTypeArray[i].length;
                out.writeInt(nCols);
                for (int j=0; j < nCols; j++) {
                    out.writeInt(personsByPersonTypeArray[i][j]);
                }
            }
        }

        ObjectUtil.writeObjectArray(out, jointTours);
        ObjectUtil.writeShortArray(out, jointToursByType);
        ObjectUtil.writeObjectArray(out, indivTours);
        ObjectUtil.writeShortArray(out, indivToursByType);
        ObjectUtil.writeObjectArray(out, mandatoryTours);
        ObjectUtil.writeShortArray(out, mandatoryToursByType);


    }

}
