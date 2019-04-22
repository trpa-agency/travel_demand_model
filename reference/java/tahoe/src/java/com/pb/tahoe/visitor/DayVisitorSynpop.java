package com.pb.tahoe.visitor;

import org.apache.log4j.Logger;

import java.util.ResourceBundle;
import java.util.HashMap;
import java.util.ArrayList;
import java.io.File;
import java.io.IOException;

import com.pb.common.util.ResourceUtil;
import com.pb.common.util.SeededRandom;
import com.pb.common.datafile.TableDataSet;
import com.pb.common.datafile.CSVFileReader;
import com.pb.tahoe.visitor.structures.VisitorDataStructure;
import com.pb.tahoe.visitor.structures.VisitorType;
import com.pb.tahoe.visitor.structures.StayType;

/**
 * User: Chris
 * Date: Mar 14, 2007 - 8:43:22 AM
 */
public class DayVisitorSynpop {

    protected static Logger logger = Logger.getLogger(DayVisitorSynpop.class);

    private ResourceBundle propertyMap = ResourceUtil.getResourceBundle("tahoe");

    private static final String zonalFileKey = "day.visitors.zonal.data.file";
    private static final String recordsFileKey = "day.visitors.records.file";

    private boolean debug = false;
    private int overnightVisitors;

    //Provides a mapping between a taz and the list of day visitor records filling it (identified by their unique id).
    private HashMap<Integer,ArrayList<Integer>> synpopShell;

    //Provides a mapping betweena taz and whether a particular day visitor record is a thru trip or not (true means it
    //is a thru trip).
    private HashMap<Integer,ArrayList<Boolean>> synpopThruMap;

    //A data set to hold day visitor records
    private TableDataSet dayVisitorRecords;

    private int nonThruDayVisitors;
    private int thruDayVisitors;
    //a data set to hold non-thru day visitors
    private TableDataSet nonThruDayVisitorSynpop = null;
    //a data set to hold thru day visitors
    private TableDataSet thruDayVisitorSynpop = null;

    public DayVisitorSynpop(int overnightVisitors, boolean debug) {
        this.overnightVisitors = overnightVisitors;
        this.debug = debug;
        synpopShell = new HashMap<Integer,ArrayList<Integer>>();
        synpopThruMap = new HashMap<Integer,ArrayList<Boolean>>();
    }

    public DayVisitorSynpop(int overnightVisitors) {
        this(overnightVisitors,false);
    }

    public void generateSynpop() {
        generateSynpopMaps();
        fillSynpopData();
    }

    public TableDataSet getNonThruSynpop() {
        if (nonThruDayVisitorSynpop == null) {
            logger.warn("Day visitor (non-thru) synpop has not been generated yet! Null value will be returned.");
        }
        return nonThruDayVisitorSynpop;
    }

    public TableDataSet getThruSynpop() {
        if (thruDayVisitorSynpop == null) {
            logger.warn("Day visitor (thru) synpop has not been generated yet! Null value will be returned.");
        }
        return thruDayVisitorSynpop;
    }


    private void generateSynpopMaps() {
        //Get zone table and day visitor records as table data sets
        TableDataSet zonalData = readTableFromFile(zonalFileKey);
        dayVisitorRecords = readTableFromFile(recordsFileKey);
        int visitorRecords = dayVisitorRecords.getRowCount();

        nonThruDayVisitors = 0;
        thruDayVisitors = 0;
        //loop over zone data and fill in synpop maps
        for (int i = 1; i <= zonalData.getRowCount(); i++) {
            int taz = (int) zonalData.getValueAt(i, VisitorDataStructure.DVZONAL_TAZ_FIELD);
            synpopShell.put(taz,new ArrayList<Integer>());
            synpopThruMap.put(taz,new ArrayList<Boolean>());
            //loop an make population by adding random records
            float thruPercent = zonalData.getValueAt(i, VisitorDataStructure.DVZONAL_THRUPERCENT_FIELD);
            int dayVisitors = Math.round(overnightVisitors * zonalData.getValueAt(i, VisitorDataStructure.DVZONAL_NIGHT2DAY_FIELD));
            if (debug) {
                logger.info("Number of day visitors for zone " + taz + ": " + dayVisitors);
            }
            for (int j = 0; j < dayVisitors; j++) {
                synpopShell.get(taz).add((int) dayVisitorRecords.getValueAt(
                        ((int) Math.floor(SeededRandom.getRandom()*visitorRecords))+1,
                        VisitorDataStructure.RECORDS_ID_FIELD));
                if (SeededRandom.getRandom() <= thruPercent) {
                    synpopThruMap.get(taz).add(true);
                    thruDayVisitors++;
                } else {
                    synpopThruMap.get(taz).add(false);
                    nonThruDayVisitors++;
                }
            }
        }
    }

    private TableDataSet readTableFromFile(String key) {
        TableDataSet tds;
        try {
            CSVFileReader reader = new CSVFileReader();
            tds = reader.readFile(new File(propertyMap.getString(key)));
        } catch (IOException e) {
            throw new RuntimeException("Error reading Travel Party file " + propertyMap.getString(key));
        }
        return tds;
    }

    private void fillSynpopData() {
        //Create headings
        ArrayList<String> headings = new ArrayList<String>();
        headings.add(VisitorDataStructure.ID_FIELD);
        headings.add(VisitorDataStructure.SAMPN_FIELD);
        headings.add(VisitorDataStructure.VISITORTYPE_FIELD);
        headings.add(VisitorDataStructure.STAYTAZ_FIELD);
        headings.add(VisitorDataStructure.WALKSEGMENT_FIELD);
        headings.add(VisitorDataStructure.STAYTYPE_FIELD);
        headings.add(VisitorDataStructure.PERSONS_FIELD);
        headings.add(VisitorDataStructure.CHILDREN_FIELD);
        headings.add(VisitorDataStructure.FEMALEADULT_FIELD);

        //Build an index on the id column for easy access.
        dayVisitorRecords.buildIndex(dayVisitorRecords.getColumnPosition(VisitorDataStructure.RECORDS_ID_FIELD));

        //make two data sets for two synpops
        float[][] thruSynPopData = new float[thruDayVisitors][headings.size()];
        float[][] nonThruSynPopData = new float[nonThruDayVisitors][headings.size()];
        int ntuid = 0;
        int tuid = 0;
        int uid = 1;
        for (int taz : synpopShell.keySet()) {
            int synPopShellLocator = 0;
            for (int id : synpopShell.get(taz)) {
                float[] synPopData;
                if (synpopThruMap.get(taz).get(synPopShellLocator++)) {
                    synPopData = thruSynPopData[tuid++];
                    synPopData[2] = VisitorType.THRU.getID();
                } else {
                    synPopData = nonThruSynPopData[ntuid++];
                    synPopData[2] = VisitorType.DAY.getID();
                }
                synPopData[0] = uid++;
                synPopData[1] = id;
                synPopData[3] = taz;
                //no walk segment in external zones
                synPopData[4] = 0;
                //day visitors get hotel/motel stay type - doesn't really affect anything
                synPopData[5] = StayType.HOTELMOTEL.getID();
                synPopData[6] = dayVisitorRecords.getIndexedValueAt(id, VisitorDataStructure.RECORDS_PERSONS_FIELD);
                synPopData[7] = dayVisitorRecords.getIndexedValueAt(id, VisitorDataStructure.RECORDS_CHILDREN_FIELD);
                synPopData[8] = dayVisitorRecords.getIndexedValueAt(id, VisitorDataStructure.RECORDS_FEMALEADULT_FIELD);
            }
        }
        nonThruDayVisitorSynpop = TableDataSet.create(nonThruSynPopData,headings);
        thruDayVisitorSynpop = TableDataSet.create(thruSynPopData,headings);
    }

}
