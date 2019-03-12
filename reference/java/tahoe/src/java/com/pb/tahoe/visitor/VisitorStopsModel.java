package com.pb.tahoe.visitor;

import org.apache.log4j.Logger;

import java.util.ResourceBundle;
import java.util.Arrays;
import java.io.File;

import com.pb.tahoe.util.ChoiceModelApplication;
import com.pb.tahoe.util.ZonalDataManager;
import com.pb.tahoe.visitor.structures.TravelParty;
import com.pb.tahoe.visitor.structures.VisitorTour;
import com.pb.tahoe.visitor.structures.VisitorMode;
import com.pb.tahoe.visitor.structures.VisitorTripMode;
import com.pb.common.util.ResourceUtil;
import com.pb.common.calculator.UtilityExpressionCalculator;
import com.pb.common.calculator.IndexValues;

/**
 * User: Chris
 * Date: Mar 5, 2007 - 11:12:05 AM
 */
public class VisitorStopsModel {
    protected static Logger logger = Logger.getLogger(VisitorStopsModel.class);

    long dcTime = 0;
    long mcTime = 0;

    int everyNth;

    final String Stops_DC_ModelSheet_Key = "visitor.stops.destination.choice.control.file";
    final String Stops_MC_ModelSheet_Key = "visitor.stops.mode.choice.control.file";
    final String Stops_Output_Key = "visitor.stops.choice.output.file";
    final String Stops_Synpop_Key = "visitor.synpop.stops.results.file";

    final int Stops_DC_DataSheet  = 0;
    final int Stops_DC_ModelSheet  = 1;
    final int Stops_MC_DataSheet  = 0;
    final int Stops_MC_ModelSheet  = 1;


    ChoiceModelApplication dc;
    UtilityExpressionCalculator dcUEC;
    UtilityExpressionCalculator mcUEC;

    boolean[] dcAvailability;
    int[] dcSample;
    int[] mcSample;

    IndexValues index = new IndexValues();

    public VisitorStopsModel(ResourceBundle rb) {
        initOvernightVisitorStopsModel(rb);
    }

    private void initOvernightVisitorStopsModel(ResourceBundle rb) {
        logger.info("Running Visitors Stops Model.");

        everyNth =  ResourceUtil.getIntegerProperty(rb,"everyNth", 1);

        dc =  new ChoiceModelApplication(Stops_DC_ModelSheet_Key, ResourceUtil.getProperty(rb,Stops_Output_Key), rb);

        dcUEC = dc.getUEC(Stops_DC_ModelSheet,Stops_DC_DataSheet, TravelParty.class);
        mcUEC = new UtilityExpressionCalculator(new File(rb.getString( Stops_MC_ModelSheet_Key) ),Stops_MC_ModelSheet, Stops_MC_DataSheet, rb, TravelParty.class);

        dc.createLogitModel();

        int numDcAlternatives = dcUEC.getNumberOfAlternatives();

        dcAvailability = new boolean[numDcAlternatives+1];
        dcSample = new int[numDcAlternatives+1];
        mcSample = new int[] {0,1};
    }

    public void doWork(PartyArrayManager pam) {
        //loop over all travel parties
        int nParties = 0;
        for (TravelParty party : pam.parties) {
            if (party== null) continue;
            chooseDestAndMode(party);
            nParties++;
            if (nParties % 1000 == 0) {
                logger.info("Stop location and mode chosen for " + nParties + " travel parties.");
            }
        }
        //tell pam we're done
        pam.modelDone(PartyArrayManager.STOP_DC_KEY);
        pam.modelDone(PartyArrayManager.STOP_MC_KEY);
        //write out results
        pam.writePartyData(Stops_Synpop_Key);
    }

    private void chooseDestAndMode(TravelParty tp) {
        VisitorTour[] tours = tp.getTours();

        //loop over all of the tours
        for (VisitorTour tour : tours) {
            //do outbound stop
            if (tour.getOutboundStop()) {
                tp.setObStop(true);
                tp.setOrigTaz(tp.getTazID());
                tp.setChosenDest(tour.getDestTAZ());
                tp.setChosenMode(tour.getMode().getId());
                tp.setChosenTodAlt(tour.getTimeOfDayAlt());
                //select destination and mode
                int chosen = chooseStopDest(tp,tour);
                //eliminate stop or set destination
                if (chosen==0) {
                    tour.setOutboundStop(false);
                } else {
                    int[] chosenDest = VisitorDTM.getDestInfo(chosen);
                    tour.setObTAZ(chosenDest[0]);
                    tour.setObWalkSegment(chosenDest[1]);
                    index.setStopZone(chosenDest[0]);
                    tour.setObMode(VisitorTripMode.getMode(chooseStopMode(tp,tour)));
                }
            }
            if (tour.getInboundStop()) {
                tp.setObStop(false);
                tp.setOrigTaz(tour.getDestTAZ());
                tp.setChosenDest(tp.getTazID());
                tp.setChosenMode(tour.getMode().getId());
                tp.setChosenTodAlt(tour.getTimeOfDayAlt());
                //select destination and mode
                int chosen = chooseStopDest(tp,tour);
                //eliminate stop or set destination
                if (chosen==0) {
                    tour.setInboundStop(false);
                } else {
                    int[] chosenDest = VisitorDTM.getDestInfo(chosen);
                    tour.setIbTAZ(chosenDest[0]);
                    tour.setIbWalkSegment(chosenDest[1]);
                    index.setStopZone(chosenDest[0]);
                    tour.setIbMode(VisitorTripMode.getMode(chooseStopMode(tp,tour)));
                }
            }
        }
    }

    private int chooseStopDest(TravelParty tp, VisitorTour tour) {
        long markTime = System.currentTimeMillis();
        index.setOriginZone(tp.getOrigTaz());
        index.setDestZone( tp.getChosenDest() );
        //set availabilities
        Arrays.fill(dcAvailability,true);
        Arrays.fill(dcSample,1);
        for (int i = 1; i < dcAvailability.length; i++) {
            if (ZonalDataManager.getVisitorStopSize(i) <= 0.0f) {
                dcAvailability[i] = false;
                dcSample[i] = 0;
            }
            if (tp.getChosenMode() == VisitorMode.DriveToTransit.getId() || tp.getChosenMode() == VisitorMode.WalkToTransit.getId()) {
                if (VisitorDTM.getDestInfo(i)[1] == 0) {
                    dcAvailability[i] = false;
                    dcSample[i] = 0;
                }
            }
        }
        //select destination
        dc.updateLogitModel(tp,dcAvailability,dcSample);
        int chosen = 0;
        if (dc.getAvailabilityCount() > 0) {
            chosen = dc.getChoiceResult();
        } else {
//            System.out.println(tour.getMode());
            String ob = (tp.getObStop() == 1) ? "outbound" : "inbound";
            logger.warn("No stop destination available for travel party " + tp.getID() +
                    ", tour " + tour.getTourNum() + " (" + ob + "). Stop will be eliminated!");
        }
        dcTime += (System.currentTimeMillis() - markTime);
        return chosen;
    }

    private int chooseStopMode(TravelParty tp, VisitorTour tour) {
        long markTime = System.currentTimeMillis();
        int chosenMode = 0;
        if (tour.getMode()== VisitorMode.DriveToTransit || tour.getMode()== VisitorMode.WalkToTransit) {
            chosenMode = (int) Math.round(mcUEC.solve(index, tp, mcSample)[0]);
        }
        mcTime += (System.currentTimeMillis() - markTime);
        return chosenMode;
    }

    public void printTimes() {
        logger.info ( "Stops Model Component Times for visitor tours:");
        logger.info ( "total seconds processing stop location destination choice model " + (float) dcTime/1000);
        logger.info ( "total seconds processing stop mode choice = " + (float) mcTime/1000);                        
    }

}
