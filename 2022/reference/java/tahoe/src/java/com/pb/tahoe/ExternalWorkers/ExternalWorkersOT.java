package com.pb.tahoe.ExternalWorkers;

import org.apache.log4j.Logger;

import java.util.ResourceBundle;
import java.util.Arrays;
import java.io.File;

import com.pb.common.util.ResourceUtil;
import com.pb.common.calculator.UtilityExpressionCalculator;
import com.pb.common.calculator.IndexValues;
import com.pb.common.model.LogitModel;
import com.pb.common.model.ConcreteAlternative;
import com.pb.common.datafile.TextFile;
import com.pb.tahoe.util.ZonalDataManager;

/**
 * User: Chris
 * Date: Mar 7, 2007 - 8:55:52 AM
 */
public class ExternalWorkersOT {

    protected static Logger logger = Logger.getLogger(ExternalWorkersOT.class);

    static ResourceBundle rb = ResourceUtil.getResourceBundle("tahoe");

    ZonalDataManager zdm = ZonalDataManager.getInstance();

    static final String ocControlFileKey = "external.worker.origin.choice.control.file";
    static final String todControlFileKey = "external.worker.time.of.day.control.file";

    static final int MODEL_SHEET = 1;
    static final int DATA_SHEET = 0;
    static final String ocAlternativesKey = "dc.alternative.set.file";

    private static boolean debug = false;
    private boolean shadowPriceIteration = false;

    private UtilityExpressionCalculator ocUEC;
    private UtilityExpressionCalculator todUEC;
    private LogitModel ocRoot;
    private LogitModel todRoot;
    private ConcreteAlternative[] ocAlts;
    private ConcreteAlternative[] todAlts;
    private String[] ocAlternativeNames = null;
    private String[] todAlternativeNames = null;

    private IndexValues index;
    private int[] ocSample;
    private int[] todSample;

    /**
     * Constructor.
     *
     * @param debugFlag
     *        The debug status of the model.
     */
    public ExternalWorkersOT(boolean debugFlag) {
        debug = debugFlag;
        writeTODAlternativeFile();
        defineOT();
    }

    /**
     * Convenience constructor for non-debug mode.
     */
    public ExternalWorkersOT() {
        this(false);
    }

    public void runExternalWorkerOCModel(ExternalWorkerArrayManager ewam) {
        //loop over all external workers and choose origin
        for (ExternalWorker ew : ewam.workers) {
            if (ew == null) continue;
            chooseOrigin(ew);
            if (ew.getID() % 1000 == 0 && !shadowPriceIteration) {
                logger.info("Origin chosen for " + ew.getID() + " external workers.");
            }
        }
    }

    public void runExternalWorkerTODModel(ExternalWorkerArrayManager ewam) {

        //loop over all external workers and choose tod
        for (ExternalWorker ew : ewam.workers) {
            if (ew == null) continue;
            chooseTOD(ew);
            if (ew.getID() % 1000 == 0 && !shadowPriceIteration) {
                logger.info("Time of day chosen for " + ew.getID() + " external workers.");
            }
        }
    }

    public void defineOT() {

        String ocControlFileName = rb.getString(ocControlFileKey);
        String todControlFileName = rb.getString(todControlFileKey);
        ocUEC = new UtilityExpressionCalculator(new File(ocControlFileName),MODEL_SHEET, DATA_SHEET, rb, ExternalWorker.class);
        todUEC = new UtilityExpressionCalculator(new File(todControlFileName),MODEL_SHEET, DATA_SHEET, rb, ExternalWorker.class);

        index = new IndexValues();

        ocSample = new int[ocUEC.getNumberOfAlternatives()+1];
        todSample = new int[todUEC.getNumberOfAlternatives()+1];
        Arrays.fill(ocSample,1);
        Arrays.fill(todSample,1);
        //Only external zones allowed in the oc sample
        for (int i = 1; i < ocSample.length; i++) {
            if (ZonalDataManager.zoneAlt[i] >= ZonalDataManager.firstInternalZoneNumber)
                ocSample[i] = 0;
        }

        ocAlternativeNames = ocUEC.getAlternativeNames();
        todAlternativeNames = todUEC.getAlternativeNames();

        //create logit models
        ocRoot = new LogitModel("root", ocUEC.getNumberOfAlternatives());
        todRoot = new LogitModel("root", todUEC.getNumberOfAlternatives());
        ocRoot.setDebug(debug);
        todRoot.setDebug(debug);

        //add model structures
        ocAlts = new ConcreteAlternative[ocUEC.getNumberOfAlternatives()];
        todAlts = new ConcreteAlternative[todUEC.getNumberOfAlternatives()];
        for (int i = 0; i < ocUEC.getNumberOfAlternatives(); i++) {
            ocAlts[i] = new ConcreteAlternative(ocAlternativeNames[i], (i+1));
            ocRoot.addAlternative(ocAlts[i]);
        }
        for (int i = 0; i < todUEC.getNumberOfAlternatives(); i++) {
            todAlts[i] = new ConcreteAlternative(todAlternativeNames[i], (i+1));
            todRoot.addAlternative(todAlts[i]);
        }
    }

    private void writeTODAlternativeFile() {
        StringBuffer altData = new StringBuffer("a,out,in\n");
        for (ExternalWorkerTODAlternative ewTODAlt : ExternalWorkerTODAlternative.values()) {
            altData.append(ewTODAlt.getAlternativeNumber()).
                    append(",").
                    append(ewTODAlt.getOutSkimPeriod()).
                    append(",").
                    append(ewTODAlt.getInSkimPeriod()).
                    append("\n");
        }
        TextFile.writeTo(ResourceUtil.getProperty(rb,"external.worker.tod.alternative.set.file"),altData.toString());
    }

    private void chooseOrigin(ExternalWorker ew) {
        index.setZoneIndex(ew.getWorkTaz());
        index.setOriginZone(ew.getWorkTaz());

        double[] utilities = ocUEC.solve(index, ew, ocSample);

        //Set utilities
        for (int i = 0; i < ocAlts.length; i++) {
            ocAlts[i].setAvailability(utilities[i] > -99.0 && (ocSample[i+1] == 1));
            ocAlts[i].setUtility(utilities[i]);
            if (debug) {
                if (utilities[i] > -99.0) {
                    logger.info ( "OC Alternative=" + i + ", externalWorkerID=" + ew.getID() + ", utilities[alt]=" + utilities[i]);
                }
            }
        }

        //Get exponentiated utilities and calculate probablilities
        ocRoot.computeAvailabilities();
        ocRoot.getUtility();
        ocRoot.calculateProbabilities();

        //choose an alternative
        ConcreteAlternative chosen = (ConcreteAlternative) ocRoot.chooseElementalAlternative();

        String chosenAltName = chosen.getName();
        boolean error = true;
        for (int a=0; a < ocAlts.length; a++) {
            if (chosenAltName.equals(ocAlternativeNames[a])) {
                ew.setHomeTaz(ZonalDataManager.zoneAlt[a+1]);
                zdm.addExternalWorkTour(ew.getHomeTaz());
                error = false;
                break;
            }
        }
        if (error)
            logger.error("External worker # " + ew.getID() + " failed to pick a valid origin zone in the external worker origin choice model!");
    }

    public void chooseTOD(ExternalWorker ew) {

        double[] utilities = todUEC.solve(index, ew, todSample);

        //Set utilities
        for (int i = 0; i < todAlts.length; i++) {
            todAlts[i].setAvailability(utilities[i] > -99.0 && (todSample[i+1] == 1));
            todAlts[i].setUtility(utilities[i]);
            if (debug) {
                if (utilities[i] > -99.0) {
                    logger.info ( "TOD Alternative=" + i + ", externalWorkerID=" + ew.getID() + ", utilities[alt]=" + utilities[i]);
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
                ew.setSkimPeriodOut(ExternalWorkerTODAlternative.getTODAlternative(a+1).getOutSkimPeriod());
                ew.setSkimPeriodIn(ExternalWorkerTODAlternative.getTODAlternative(a+1).getInSkimPeriod());
                error = false;
                break;
            }
        }
        if (error)
            logger.error("External worker # " + ew.getID() + " failed to pick a valid time of day in the external worker time of day choice model!");
    }

    public void setShadowPriceIteration (boolean shadowPriceIteration) {
        this.shadowPriceIteration = shadowPriceIteration;
    }

}
