package com.pb.tahoe.util;

import com.pb.common.datafile.CSVFileReader;
import com.pb.common.datafile.TableDataSet;
import com.pb.common.util.ResourceUtil;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.util.ResourceBundle;

public class TODDataManager implements java.io.Serializable {

    protected static Logger logger = Logger.getLogger(TODDataManager.class);

    public static TODDataManager instance = new TODDataManager();

    protected static TableDataSet todAltsTable;

    protected static int startPosition;
    protected static int endPosition;


    protected ResourceBundle propertyMap;

    public static float[] logsumTcEAEA;
    public static float[] logsumTcEAAM;
    public static float[] logsumTcEAMD;
    public static float[] logsumTcEAPM;
    public static float[] logsumTcEANT;
    public static float[] logsumTcAMAM;
    public static float[] logsumTcAMMD;
    public static float[] logsumTcAMPM;
    public static float[] logsumTcAMNT;
    public static float[] logsumTcMDMD;
    public static float[] logsumTcMDPM;
    public static float[] logsumTcMDNT;
    public static float[] logsumTcPMPM;
    public static float[] logsumTcPMNT;
    public static float[] logsumTcNTNT;


    private TODDataManager ( ) {

        propertyMap = ResourceUtil.getResourceBundle("tahoe");


        // build the TOD data table from the TOD choice alternatives file
        String todFile = propertyMap.getString( "tod.alternative.set.file" );

        try {
            CSVFileReader reader = new CSVFileReader();
            todAltsTable = reader.readFile(new File(todFile));
        }
        catch (IOException e) {
            throw new RuntimeException("Problems reading file " + todFile, e);
        }




        startPosition = todAltsTable.getColumnPosition( "start" );
        if (startPosition <= 0) {
            logger.fatal( "start was not a field in the tod choice alternatives TableDataSet.");
            System.exit(1);
        }

        endPosition = todAltsTable.getColumnPosition( "end" );
        if (endPosition <= 0) {
            logger.fatal( "end was not a field in the tod choice alternatives TableDataSet.");
            System.exit(1);
        }



        int numTcAlternatives = todAltsTable.getRowCount();

        logsumTcEAEA = new float[numTcAlternatives+1];
        logsumTcEAAM = new float[numTcAlternatives+1];
        logsumTcEAMD = new float[numTcAlternatives+1];
        logsumTcEAPM = new float[numTcAlternatives+1];
        logsumTcEANT = new float[numTcAlternatives+1];
        logsumTcAMAM = new float[numTcAlternatives+1];
        logsumTcAMMD = new float[numTcAlternatives+1];
        logsumTcAMPM = new float[numTcAlternatives+1];
        logsumTcAMNT = new float[numTcAlternatives+1];
        logsumTcMDMD = new float[numTcAlternatives+1];
        logsumTcMDPM = new float[numTcAlternatives+1];
        logsumTcMDNT = new float[numTcAlternatives+1];
        logsumTcPMPM = new float[numTcAlternatives+1];
        logsumTcPMNT = new float[numTcAlternatives+1];
        logsumTcNTNT = new float[numTcAlternatives+1];

    }

    public static TODDataManager getInstance(){
        return instance;
    }



    public static int getTodPeriod (int hour) {
        int timePeriod=0;

        if ( hour <= 6 )
            timePeriod = 1;
        else if ( hour <= 9 )
            timePeriod = 2;
        else if ( hour <= 12 )
            timePeriod = 3;
        else if ( hour <= 15 )
            timePeriod = 4;
        else if ( hour <= 18 )
            timePeriod = 5;
        else if ( hour <= 21 )
            timePeriod = 6;
        else
            timePeriod = 7;

        return timePeriod;
    }


    public static int getTodSkimPeriod (int period) {
        int skimPeriod=0;

        switch (period) {
            // am skims
            case(2):
                skimPeriod=1;
                break;
            // pm skims
            case(5):
                skimPeriod=2;
                break;
            // md skims
            case(3):
            case(4):
                skimPeriod=3;
                break;
            // nt skims
            default:
                skimPeriod=4;
        }

        return skimPeriod;
    }




    public static int getTodStartHour ( int todAlt ) {
        return (int)todAltsTable.getValueAt( todAlt, startPosition );
    }


    public static int getTodEndHour ( int todAlt ) {
        return (int)todAltsTable.getValueAt( todAlt, endPosition );
    }


    public static int getTodStartPeriod ( int todAlt ) {
        return getTodPeriod ( getTodStartHour ( todAlt ) );
    }


    public static int getTodEndPeriod ( int todAlt ) {
        return getTodPeriod ( getTodEndHour ( todAlt ) );
    }


    public static int getTodStartSkimPeriod ( int todAlt ) {
        return getTodSkimPeriod ( getTodStartPeriod ( todAlt ) );
    }


    public static int getTodEndSkimPeriod ( int todAlt ) {
        return getTodSkimPeriod ( getTodEndPeriod ( todAlt ) );
    }









}
