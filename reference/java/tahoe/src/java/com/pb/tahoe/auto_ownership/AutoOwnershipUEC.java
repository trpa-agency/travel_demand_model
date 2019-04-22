package com.pb.tahoe.auto_ownership;

import org.apache.log4j.Logger;
import com.pb.common.calculator.UtilityExpressionCalculator;
import com.pb.common.calculator.IndexValues;
import com.pb.common.datafile.TableDataSet;
import com.pb.tahoe.structures.Household;
import java.util.Arrays;
import java.util.ResourceBundle;
import java.io.File;

/**
 * This class is used for ...
 *
 * @author Chris Frazier
 * @version Feb 16, 2006
 *          Created by IntelliJ IDEA.
 */
public class AutoOwnershipUEC {


    static Logger logger = Logger.getLogger(AutoOwnershipUEC.class);

    static final int MODEL_1_SHEET = 1;
    static final int DATA_1_SHEET = 0;

    private UtilityExpressionCalculator uec;
    private boolean debug = false;

    private IndexValues index = new IndexValues();
    private int[] sample;

    public AutoOwnershipUEC(String controlFileName, ResourceBundle rb) {
        uec = new UtilityExpressionCalculator ( new File(controlFileName), MODEL_1_SHEET, DATA_1_SHEET, rb, Household.class );

        sample = new int[uec.getNumberOfAlternatives()+1];
    }

    public TableDataSet getHouseholdData () {
        return  uec.getHouseholdData ();
    }

    public int getNumberOfAlternatives () {
        return uec.getNumberOfAlternatives();
    }

    public void setDebug (boolean debug) {
        this.debug = debug;
    }

    public double[] getUtilities (int hh_taz_id, int hh_id) {

        index.setZoneIndex( hh_taz_id );
        index.setHHIndex( hh_id );

        Arrays.fill(sample, 1);
        double[] utilities = uec.solve( index, new Object(), sample );


         if(debug) {
            logger.info( "utilities[] array for hh_taz_id=" + hh_taz_id + ", hh_id=" + hh_id );
            logger.info( String.format("%6s", " ") );
            for (int c=0; c < utilities.length; c++)
                logger.info( String.format("%12s", ("Alt " + (c+1)) ) );
            logger.info ( "" );

            logger.info( String.format("%6s", " ") );
            for (double utility : utilities) logger.info(String.format("%12.5f", utility));
            logger.info ( "" );
            logger.info ( "" );
    }


        return utilities;
    }

    //A user defined object with public fields that are accessed for values.
    //Note: All fields should be declared double.
    public class DMU {
        // e.g.:
        // double schooldriv = 5;

        // DMU not used for auto ownershiop model
    }



}
