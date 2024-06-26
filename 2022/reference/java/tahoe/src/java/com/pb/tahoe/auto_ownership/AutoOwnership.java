package com.pb.tahoe.auto_ownership;

import com.pb.common.datafile.TableDataSet;
import com.pb.common.util.ResourceUtil;
import com.pb.tahoe.util.DataWriter;
import com.pb.tahoe.util.OutputDescription;
import org.apache.log4j.Logger;

import java.util.ResourceBundle;

/**
 * This class is used for ...
 *
 * @author Chris Frazier
 * @version Feb 16, 2006
 * Created by IntelliJ IDEA.
 */
public class AutoOwnership {


    protected static Logger logger = Logger.getLogger(AutoOwnership.class);

    ResourceBundle rb;

    TableDataSet hhTable;


    public AutoOwnership( ResourceBundle rb ) {

        this.rb = rb;

    }


    public void runAutoOwnership() {

        int hh_id;
        int hh_taz_id;
        double[] utilities;


        //open files
        String controlFile = rb.getString( "auto.ownership.control.file" );



        // create a new UEC to get utilties for this logit model
        AutoOwnershipUEC uec = new AutoOwnershipUEC(controlFile, rb);
        uec.setDebug(false);

        // create a new auto ownership logit model object
        AutoOwnershipLM model = new AutoOwnershipLM();

        // get the household data table from the UEC control file
        hhTable = uec.getHouseholdData();
        if (hhTable == null) {
            logger.fatal(
                "Could not get householdData TableDataSet from UEC in AutoOwnershipUEC.run().");
            System.exit(1);
        }

        int hh_idPosition = hhTable.getColumnPosition(DataWriter.HHID_FIELD);
        if (hh_idPosition <= 0) {
            logger.fatal(DataWriter.HHID_FIELD
                    + " was not a field in the householdData TableDataSet returned from UEC in AutoOwnershipUEC.run().");
            System.exit(1);
        }
        int hh_taz_idPosition =
            hhTable.getColumnPosition(DataWriter.HHTAZID_FIELD);
        if (hh_taz_idPosition <= 0) {
            logger.fatal(
                DataWriter.HHTAZID_FIELD
                    + " was not a field in the householdData TableDataSet returned from UEC in AutoOwnershipUEC.run().");
            System.exit(1);
        }

        float[] chosenAlternatives = new float[hhTable.getRowCount()];

        // loop over all households in the hh table
        for (int i = 0; i < hhTable.getRowCount(); i++) {

//            baseLogger.info( "Auto Ownership Choice for hh " + (i + 1) + " of " + hhTable.getRowCount() );

            hh_id = (int) hhTable.getValueAt(i + 1, hh_idPosition);
            hh_taz_id = (int) hhTable.getValueAt(i + 1, hh_taz_idPosition);

            // get utilities for each alternative for this household
            utilities = uec.getUtilities(hh_taz_id, hh_id);

            // attach utilities to the logit model for this household
            model.attachUtilities(utilities);

            // get the set of multinomial logit choice proportions
            model.getProportions();

            chosenAlternatives[i] = (float) model.getAlternativeNumber();
        }

        hhTable.appendColumn(chosenAlternatives, DataWriter.AUTO_OWN_FIELD);

        logger.info("Printing Auto Ownership summary reports");
        String[] descriptions = OutputDescription.getDescriptions(DataWriter.AUTO_OWN_FIELD);

        TableDataSet.logColumnFreqReport("Auto Ownership", hhTable,
            hhTable.getColumnPosition(DataWriter.AUTO_OWN_FIELD), descriptions);




    }

    public void writeAutoOwnershipResults(){
        DataWriter writer = DataWriter.getInstance();
        writer.writeAOFile(hhTable);
    }



    public static void main(String[] args) {
        ResourceBundle rb = ResourceUtil.getResourceBundle("tahoe");
        AutoOwnership ao = new AutoOwnership(rb);
        ao.runAutoOwnership();
        ao.writeAutoOwnershipResults();
        logger.info("end of Auto Ownership Module");
    }
}
