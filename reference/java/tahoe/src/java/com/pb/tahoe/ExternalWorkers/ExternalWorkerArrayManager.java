package com.pb.tahoe.ExternalWorkers;

import org.apache.log4j.Logger;

import java.util.ResourceBundle;
import java.io.File;
import java.io.IOException;

import com.pb.common.util.ResourceUtil;
import com.pb.common.datafile.TableDataSet;
import com.pb.common.datafile.CSVFileReader;
import com.pb.tahoe.util.DataWriter;
import com.pb.tahoe.util.ZonalDataManager;

/**
 * User: Chris
 * Date: Mar 6, 2007 - 5:43:38 PM
 */
public class ExternalWorkerArrayManager {

    static Logger logger = Logger.getLogger(ExternalWorkerArrayManager.class);
    static ResourceBundle propertyMap = ResourceUtil.getResourceBundle("tahoe");

    public static ExternalWorkerArrayManager instance = new ExternalWorkerArrayManager();

    public int externalWorkersCount = 0;

    public ExternalWorker[] workers = null;

    //Names for data table
    public static final String EXTERNAL_WORKER_ID_FIELD = "id";
    public static final String EXTERNAL_WORKER_WORK_TAZ_FIELD = "workTaz";
    public static final String EXTERNAL_WORKER_HOME_TAZ_FIELD = "homeTaz";
    public static final String EXTERNAL_WORKER_SKIM_IN_FIELD = "skimIn";
    public static final String EXTERNAL_WORKER_SKIM_OUT_FIELD = "skimOut";

    private ExternalWorkerArrayManager() {
    }

    public static ExternalWorkerArrayManager getInstance() {
        return instance;
    }

    public void createExternalWorkerArray(TableDataSet workerData) {
        String arrayStatus = (workers == null) ? "created" : "updated";

        //Though the convention stinks, all the other XXXArrayManagers index from 1, not zero, so
        // this one will too
        externalWorkersCount = workerData.getRowCount();
        workers = new ExternalWorker[externalWorkersCount + 1];
        //Set external worker count in zonal data manager
        ZonalDataManager.getInstance().setExternalWorkers(externalWorkersCount);

        //fill the array with travel parties
        for (int i = 1; i <= externalWorkersCount; i++) {
            workers[i] = new ExternalWorker();
            workers[i].setID((int) workerData.getValueAt(i,EXTERNAL_WORKER_ID_FIELD));
            workers[i].setWorkTaz((int) workerData.getValueAt(i,EXTERNAL_WORKER_WORK_TAZ_FIELD));
            if (workerData.getColumnPosition(EXTERNAL_WORKER_HOME_TAZ_FIELD) > -1)
                workers[i].setHomeTaz((int) workerData.getValueAt(i,EXTERNAL_WORKER_HOME_TAZ_FIELD));
            if (workerData.getColumnPosition(EXTERNAL_WORKER_SKIM_IN_FIELD) > -1)
                workers[i].setSkimPeriodIn((int) workerData.getValueAt(i,EXTERNAL_WORKER_SKIM_IN_FIELD));
            if (workerData.getColumnPosition(EXTERNAL_WORKER_SKIM_OUT_FIELD) > -1)
                workers[i].setSkimPeriodOut((int) workerData.getValueAt(i,EXTERNAL_WORKER_SKIM_OUT_FIELD));
        }

        logger.info("External workers array manager " + arrayStatus + ".");
    }

    public void createExternalWorkerArray(String property) {
        TableDataSet workerData;
        String workerDataFile = propertyMap.getString(property);

        try {
            CSVFileReader reader = new CSVFileReader();
            workerData = reader.readFile(new File(workerDataFile));
        } catch (IOException e) {
            workerData = null;
            logger.warn("Error reading Travel Party file " + workerDataFile);
        }

        if (workerData != null) {
            createExternalWorkerArray(workerData);
        } else {
            logger.warn("\tError: External worker array has NOT been created/updated!");
        }
    }

    public void writeExternalWorkerData(String outFileKey) {
        //create base headings
        String[] headings = {EXTERNAL_WORKER_ID_FIELD,EXTERNAL_WORKER_WORK_TAZ_FIELD};

        //Check to see if the non-base data has been created
        boolean homeTazDone = workers[1].getHomeTaz() > 0;
        boolean skimOutDone = workers[1].getSkimPeriodOut() > 0;
        boolean skimInDone = workers[1].getSkimPeriodIn() > 0;

        float[][] baseData = new float[externalWorkersCount][headings.length];
        float[] homeTazData = new float[externalWorkersCount];
        float[] skimOutData = new float[externalWorkersCount];
        float[] skimInData = new float[externalWorkersCount];

        for (int i = 0; i < externalWorkersCount; i++) {
            ExternalWorker ew = workers[i+1];
            baseData[i][0] = ew.getID();
            baseData[i][1] = ew.getWorkTaz();
            if (homeTazDone)
                homeTazData[i] = ew.getHomeTaz();
            if (skimOutDone)
                skimOutData[i] = ew.getSkimPeriodOut();
            if (skimInDone)
                skimInData[i] = ew.getSkimPeriodIn();
        }

        TableDataSet workerData = TableDataSet.create(baseData,headings);
        if (homeTazDone)
            workerData.appendColumn(homeTazData,EXTERNAL_WORKER_HOME_TAZ_FIELD);
        if (skimOutDone)
            workerData.appendColumn(skimOutData,EXTERNAL_WORKER_SKIM_OUT_FIELD);
        if (skimInDone)
            workerData.appendColumn(skimInData,EXTERNAL_WORKER_SKIM_IN_FIELD);

        DataWriter dw = DataWriter.getInstance();
        dw.writeOutputFile(outFileKey,workerData);
    }

}
