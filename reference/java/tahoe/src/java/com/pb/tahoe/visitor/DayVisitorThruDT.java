package com.pb.tahoe.visitor;

import org.apache.log4j.Logger;
import com.pb.tahoe.visitor.structures.TravelParty;
import com.pb.tahoe.visitor.structures.VisitorTour;
import com.pb.tahoe.visitor.structures.VisitorMode;
import com.pb.common.util.ResourceUtil;
import com.pb.common.calculator.UtilityExpressionCalculator;
import com.pb.common.calculator.IndexValues;
import com.pb.common.model.LogitModel;
import com.pb.common.model.ConcreteAlternative;

import java.io.File;
import java.util.Arrays;
import java.util.ResourceBundle;

/**
 * User: Chris
 * Date: Mar 14, 2007 - 2:07:29 PM
 */
public class DayVisitorThruDT {

    protected static Logger logger = Logger.getLogger(DayVisitorThruDT.class);

    static ResourceBundle rb = ResourceUtil.getResourceBundle("tahoe");

    final String Thru_Full_Synpop_Key = "visitor.synpop.full.results.file";

    private DayVisitorPattern dvp = DayVisitorPattern.getInstance();

    static final String dcControlFileKey = "visitor.thru.destination.choice.control.file";
    static final String todControlFileKey = "visitor.thru.time.of.day.control.file";

    static final int MODEL_SHEET = 1;
    static final int DATA_SHEET = 0;

    private boolean debug;

    private UtilityExpressionCalculator dcUEC;
    private UtilityExpressionCalculator todUEC;
    private LogitModel dcRoot;
    private LogitModel todRoot;
    private ConcreteAlternative[] dcAlts;
    private ConcreteAlternative[] todAlts;
    private String[] dcAlternativeNames = null;
    private String[] todAlternativeNames = null;

    private IndexValues index;
    private int[] dcSample;
    private int[] todSample;

    public DayVisitorThruDT(boolean debug) {
        this.debug = debug;
    }

    public DayVisitorThruDT() {
        this(false);
    }

    public void doWork(PartyArrayManager pam) {
        logger.info("Running thru visitor destination and time-of-day choice models...");
        //define UECs
        defineDT();
        //loop over all travel parties
        int nParties = 0;
        for (TravelParty party : pam.parties) {
            if (party== null) continue;
            choosePatDestTODAndMode(party);
            nParties++;
            if (nParties % 1000 == 0) {
                logger.info("Pattern, destination, and TOD chosen for " + nParties + " day visitor thru-trip travel parties.");
            }
        }
        //tell pam we're done
        pam.modelDone(PartyArrayManager.PATTERN_KEY);
        pam.modelDone(PartyArrayManager.DC_KEY);
        pam.modelDone(PartyArrayManager.TOD_KEY);
        pam.modelDone(PartyArrayManager.MC_KEY);
        pam.modelDone(PartyArrayManager.STOP_DC_KEY);
        pam.modelDone(PartyArrayManager.STOP_MC_KEY);
        pam.writePartyData(Thru_Full_Synpop_Key);
    }

    private void choosePatDestTODAndMode(TravelParty tp) {
        tp.setPattern(-1);
        tp.setTours(dvp.getToursFromPatternID(-1));
        VisitorTour vt = tp.getTours()[0];
        chooseDest(tp,vt);
        tp.setChosenDest(vt.getDestTAZ());
        chooseTOD(tp,vt);
        vt.setMode(VisitorMode.Drive);
    }

    private void defineDT() {

        logger.info("Creating thru trips DT UEC");
        dcUEC = new UtilityExpressionCalculator(new File(rb.getString(dcControlFileKey)),MODEL_SHEET, DATA_SHEET, rb, TravelParty.class);
        logger.info("Creating thru trips TOD UEC");
        todUEC = new UtilityExpressionCalculator(new File(rb.getString(todControlFileKey)),MODEL_SHEET, DATA_SHEET, rb, TravelParty.class);

        index = new IndexValues();

        dcSample = new int[dcUEC.getNumberOfAlternatives()+1];
        todSample = new int[todUEC.getNumberOfAlternatives()+1];
        Arrays.fill(dcSample,1);
        Arrays.fill(todSample,1);

        dcAlternativeNames = dcUEC.getAlternativeNames();
        todAlternativeNames = todUEC.getAlternativeNames();

        //create logit models
        dcRoot = new LogitModel("root", dcUEC.getNumberOfAlternatives());
        todRoot = new LogitModel("root", todUEC.getNumberOfAlternatives());
        dcRoot.setDebug(debug);
        todRoot.setDebug(debug);

        //add model structures
        dcAlts = new ConcreteAlternative[dcUEC.getNumberOfAlternatives()];
        todAlts = new ConcreteAlternative[todUEC.getNumberOfAlternatives()];
        for (int i = 0; i < dcUEC.getNumberOfAlternatives(); i++) {
            dcAlts[i] = new ConcreteAlternative(dcAlternativeNames[i], (i+1));
            dcRoot.addAlternative(dcAlts[i]);
        }
        for (int i = 0; i < todUEC.getNumberOfAlternatives(); i++) {
            todAlts[i] = new ConcreteAlternative(todAlternativeNames[i], (i+1));
            todRoot.addAlternative(todAlts[i]);
        }
    }

    private void chooseDest(TravelParty tp,VisitorTour vt) {
//        vt.setDestTAZ((int) Math.ceil(SeededRandom.getRandom()*7));
//        vt.setDestWalkSegment(0);
        tp.setOrigTaz(tp.getTazID());
        index.setZoneIndex(tp.getTazID());
        index.setOriginZone(tp.getTazID());

        double[] utilities = dcUEC.solve(index, tp, dcSample);

        //Set utilities
        for (int i = 0; i < dcAlts.length; i++) {
            dcAlts[i].setAvailability(utilities[i] > -99.0 && (dcSample[i+1] == 1));
            dcAlts[i].setUtility(utilities[i]);
            if (debug) {
                if (utilities[i] > -99.0) {
                    logger.info ( "DC Alternative=" + i + ", thru visitor ID=" + tp.getID() + ", utilities[alt]=" + utilities[i]);
                }
            }
        }

        //Get exponentiated utilities and calculate probablilities
        dcRoot.computeAvailabilities();
        dcRoot.getUtility();
        dcRoot.calculateProbabilities();

        //choose an alternative
        ConcreteAlternative chosen = (ConcreteAlternative) dcRoot.chooseElementalAlternative();

        String chosenAltName = chosen.getName();
        boolean error = true;
        for (int a=0; a < dcAlts.length; a++) {
            if (chosenAltName.equals(dcAlternativeNames[a])) {
                vt.setDestTAZ(a+1);
                vt.setDestWalkSegment(0);
                error = false;
                break;
            }
        }
        if (error)
            logger.error("Travel party # " + tp.getID() + " failed to pick a valid destination zone in the thru trip destination choice model!");
    }

    private void chooseTOD(TravelParty tp,VisitorTour vt) {
        //vt.setTimeOfDayAlt((int) Math.ceil(SeededRandom.getRandom()*4));

        double[] utilities = todUEC.solve(index, tp, todSample);

        //Set utilities
        for (int i = 0; i < todAlts.length; i++) {
            todAlts[i].setAvailability(utilities[i] > -99.0 && (todSample[i+1] == 1));
            todAlts[i].setUtility(utilities[i]);
            if (debug) {
                if (utilities[i] > -99.0) {
                    logger.info ( "TOD Alternative=" + i + ", (thru) travel party ID=" + tp.getID() + ", utilities[alt]=" + utilities[i]);
                }
            }
        }

        //Get exponentiated utilities and calculate probablilities
        todRoot.computeAvailabilities();
        todRoot.getUtility();
        todRoot.calculateProbabilities();

        //choose an alternative
        ConcreteAlternative chosen = (ConcreteAlternative) todRoot.chooseElementalAlternative();

        String chosenAltName = chosen.getName();
        boolean error = true;
        for (int a=0; a < todAlts.length; a++) {
            if (chosenAltName.equals(todAlternativeNames[a])) {
                vt.setTimeOfDayAlt(a+1);
                error = false;
                break;
            }
        }
        if (error)
            logger.error("Travel party # " + tp.getID() + " failed to pick a valid time of day in the thru trip time of day choice model!");
    }
}
