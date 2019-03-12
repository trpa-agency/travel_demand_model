package com.pb.tahoe.visitor;

import org.apache.log4j.Logger;

import java.util.ResourceBundle;
import java.util.Arrays;
import java.io.*;

import com.pb.common.util.ResourceUtil;
import com.pb.common.calculator.UtilityExpressionCalculator;
import com.pb.common.calculator.IndexValues;
import com.pb.common.model.LogitModel;
import com.pb.common.model.ConcreteAlternative;
import com.pb.tahoe.visitor.structures.TravelParty;
import com.pb.tahoe.visitor.structures.VisitorType;
import com.pb.tahoe.visitor.structures.VisitorPattern;

/**
 * User: Chris
 * Date: Mar 13, 2007 - 11:51:06 AM
 */
public class VisitorPatternModel {

    protected static Logger logger = Logger.getLogger(VisitorPatternModel.class);

    static ResourceBundle rb = ResourceUtil.getResourceBundle("tahoe");
    protected static boolean debug = false;

    VisitorType type;
    VisitorPattern vp;

    static String controlFileKey;
    static String patternAlternativesResultsFile;
    static int MODEL_SHEET = 1;
    static int DATA_SHEET = 0;

    static final String overnightControlFileKey = "overnight.visitor.pattern.model.uec";
    static final String overnightPatternAlternativesResultsFile = "overnight.pattern.results.file";
    static final String dayControlFileKey = "day.visitor.pattern.model.uec";
    static final String dayPatternAlternativesResultsFile = "day.pattern.results.file";

    protected UtilityExpressionCalculator uec;
    protected LogitModel root;
    protected ConcreteAlternative[] alts;
    protected String[] alternativeNames = null;

    protected IndexValues index;
    protected int[] sample;

    /**
     * Constructor.
     *
     * @param type
     *        The type of visitor (day or overnight).
     *
     * @param debugFlag
     *        The debug status of the model.
     */
    public VisitorPatternModel(VisitorType type, boolean debugFlag) {
        debug = debugFlag;
        this.type = type;
        if (type == VisitorType.OVERNIGHT) {
            vp = OvernightVisitorPattern.getInstance();
        } else if (type == VisitorType.DAY) {
            vp = DayVisitorPattern.getInstance();
        }
    }

    /**
     * Convenience constructor for non-debug mode.
     *
     * @param type
     *        The type of visitor (day or overnight).
     */
    public VisitorPatternModel(VisitorType type) {
        this(type, false);
    }

    public void runVisitorPatternModel(PartyArrayManager pam) {
        definePatternModel();

        //loop over all parties and select their pattern
        int counter = 0;
        for (TravelParty party : pam.parties) {
            if (party== null) continue;
            choosePattern(party);
            if (++counter % 1000 == 0) {
                logger.info(type + " visitor pattern chosen for " + counter + " parties.");
            }
        }

        //tell the party array manager that the pattern model is done
        pam.modelDone(PartyArrayManager.PATTERN_KEY);

        //and write the results out to a results file
        pam.writePartyData(patternAlternativesResultsFile);
    }

    private void definePatternModel() {
        //Define data locations
        if (type == VisitorType.OVERNIGHT) {
            controlFileKey = overnightControlFileKey;
            patternAlternativesResultsFile = overnightPatternAlternativesResultsFile;
        } else if (type == VisitorType.DAY) {
            controlFileKey = dayControlFileKey;
            patternAlternativesResultsFile = dayPatternAlternativesResultsFile;
        }

        //generate pattern alternatives file and fill pattern data
        vp.generatePatternAlternativeData();

        //create uec
        String controlFileName = rb.getString(controlFileKey);
        uec = new UtilityExpressionCalculator ( new File(controlFileName), MODEL_SHEET, DATA_SHEET, rb, TravelParty.class );

        //create index
        index = new IndexValues();

        //create sample - all patterns are always available
        sample = new int[uec.getNumberOfAlternatives()+1];
        Arrays.fill(sample,1);

        //set alternative "names"
        alternativeNames = uec.getAlternativeNames();

        //create logit model
        root = new LogitModel ("root", uec.getNumberOfAlternatives());
	    root.setDebug (debug);
        //add model structure
        alts = new ConcreteAlternative[uec.getNumberOfAlternatives()];
        for(int i=0; i < uec.getNumberOfAlternatives(); i++) {
            alts[i]  = new ConcreteAlternative(alternativeNames[i], (i+1));
            root.addAlternative (alts[i]);
        }
    }

    private void choosePattern(TravelParty party) {
        //I don't know if I need these, but it doesn't hurt
        index.setHHIndex(party.getID());
        index.setZoneIndex(party.getTazID());

        double[] utilities = uec.solve( index, party, sample );

        //Set utilities
        for (int i = 0; i < alts.length; i++) {
            alts[i].setAvailability(utilities[i] > -99.0);
            alts[i].setUtility(utilities[i]);
            if (debug) {
                if (utilities[i] > -99.0) {
                    logger.info ( "Alternative=" + i + ", partyID=" + party.getID() + ", utilities[alt]=" + utilities[i]);
                }
            }
        }

        //Get exponentiated utilities and calculate probablilities
        root.computeAvailabilities();
        root.getUtility();
        root.calculateProbabilities();

        //Choose an alternative
        ConcreteAlternative chosen = (ConcreteAlternative) root.chooseElementalAlternative();

        //Set chosen alternative
        String chosenAltName= chosen.getName();
        // save chosen alternative in  householdChoice Array
        boolean error = true;
        for(int a=0; a < alts.length; a++) {
            if (chosenAltName.equals(alternativeNames[a])) {
                party.setPattern(a+1);
                party.setTours(vp.getToursFromPatternID(a+1));
                party.setPatternString(vp.getPatternFromID(a+1));
                error = false;
            }
        }
        if (error)
            logger.error("Party ID # " + party.getID() + " failed to pick a valid pattern in the " + type + " visitor pattern choice model!");
    }
}
