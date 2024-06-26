package com.pb.tahoe.structures;

import org.apache.log4j.Logger;

/**
 * User: Chris
 * Date: Feb 27, 2007 - 12:51:50 PM
 */
public abstract class DecisionMakingUnit {
    int ID;
    short taz;
    short origTaz;
    short chosenDest;
    short originWalkSegment;
    short chosenWalkSegment;
    short chosenTodAlt;
    short chosenMode;

    /*
     * set id for dmu object
     */
    public void setID (int id) {
        ID = id;
    }

    /*
     * return id for dmu object
     */
    public int getID () {
        return ID;
    }

    /*
     * set (home) TAZ for dmu object
     */
    public void setTazID (int arg) {
        taz = (short)arg;
    }

    /*
     * return (home) TAZ for dmu object
     */
    public int getTazID () {
        return taz;
    }


    /*
     * set origin taz variable.
     */
    public void setOrigTaz ( int arg ) {
        origTaz = (short)arg;
    }

    /*
     * get origin taz value.
     */
    public int getOrigTaz () {
        return origTaz;
    }    

    /*
     * set TAZ for the chosen destination alternative for this tour
     */
    public void setChosenDest (int arg) {
        chosenDest = (short)arg;
    }

     /*
     * return TAZ for the chosen destination alternative for this tour
     */
    public int getChosenDest () {
        return chosenDest;
    }

    public void setOriginWalkSegment (int segment) {
        originWalkSegment = (short)segment;
    }


    public int getOriginWalkSegment () {
        return originWalkSegment;
    }


    /*
     * set walk segment (0-none, 1-short, 2-long walk to transit access) for the destination choice alternative for this tour
     */
    public void setChosenWalkSegment (int arg) {
        chosenWalkSegment = (short)arg;
    }

    /*
     * return the walk segment (0-none, 1-short, 2-long walk to transit access) for the destination choice alternative for this tour
     */
    public int getChosenWalkSegment () {
        return chosenWalkSegment;
    }

    /*
     * set TOD alt for the chosen time of day alternative for this tour
     */
    public void setChosenTodAlt (int arg) {
        chosenTodAlt = (short)arg;
    }

    /*
     * return TOD alt for the chosen time of day alternative for this tour
     */
    public int getChosenTodAlt () {
        return chosenTodAlt;
    }

    /*
     * set the chosen mode for this tour
     */
    public void setChosenMode (int mode) {
        chosenMode = (short) mode;
    }

    /*
     * get the chosen mode for this tour
     */
    public int getChosenMode() {
        return chosenMode;
    }

    public abstract void writeContentToLogger(Logger logger);
}