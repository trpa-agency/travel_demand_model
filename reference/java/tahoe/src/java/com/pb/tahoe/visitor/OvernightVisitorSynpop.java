package com.pb.tahoe.visitor;

import org.apache.log4j.Logger;
import com.pb.tahoe.visitor.structures.StayType;
import com.pb.tahoe.visitor.structures.VisitorDataStructure;
import com.pb.tahoe.visitor.structures.VisitorType;
import com.pb.tahoe.util.ZonalDataManager;
import com.pb.tahoe.util.ChoiceModelApplication;
import com.pb.tahoe.util.DataWriter;
import com.pb.common.datafile.TableDataSet;
import com.pb.common.datafile.CSVFileReader;
import com.pb.common.util.SeededRandom;
import com.pb.common.util.ResourceUtil;

import java.util.*;
import java.io.File;
import java.io.IOException;

/**
 * This class generates the overnight visitor synthetic population.  It takes as an input a "double" hash map which
 * links the zone/stay-type pairs to the number of units (of that stay type in that zone) that need to be filled.
 * It generates both a synthetic population table data set and a csv version of that data set.
 *
 * User: Chris
 * Date: Feb 7, 2007 - 4:07:28 PM
 */
public class OvernightVisitorSynpop {

    protected static Logger logger = Logger.getLogger(OvernightVisitorSynpop.class);

    private ResourceBundle propertyMap = ResourceUtil.getResourceBundle("tahoe");

    private static final String recordsFileKey = "overnight.visitors.records.file";
    private static final String synpopResultsFile = "overnight.visitor.synpop.results.file";

    private static boolean debug = false;

    // This links each stay type and zone combination to the number of units that need to filled by travel parties.
    //  This linkage is done via two {@code HashMap}s, the outer one linking to zone, the inner one to stay type.
    private HashMap<Integer,HashMap<StayType,Integer>> unitsToFill;

    // This data set holds all of the overnight visitor records from which we will sample and build our synthetic population.
    private TableDataSet overnightVisitorRecords;

    // This is the name of the column in the {@link #overnightVisitorRecords} data set holding the unique id of the records.
    private static String idColumn = VisitorDataStructure.RECORDS_ID_FIELD;

    // This is the name of the column in the {@link #overnightVisitorRecords} data set which identifies that record's
    //  stay type (via the stay type id). This will be used to stratify the sample when creating the synthetic
    //  population.
    private static String stayTypeColumn = VisitorDataStructure.RECORDS_STAYTYPE_FIELD;

    // This provides a mapping between the stay type classifications and the record ids from overnightVisitorRecords
    //  which correspond to it.
    private HashMap<StayType,Integer[]> stayTypeRecords;

    // This provides a mapping from the stay type classifications and the size of the samples from which the synthetic
    //  population will be constructed. */
    private HashMap<StayType,Integer> stayTypeSampleSize;

    // This provides a mapping from a zone to the parties selected to fill it (via their unique sample id) to make up
    //  the synthetic overnight visitor population.
    private HashMap<Integer,ArrayList<Integer>> synPopShell;

    // This provides a mapping for the synPopShell to tell what StayType each entry is supposed to be (a StayType's
    //  synpop may be sampled from multiple StayTypes, but in the synpop, those multiple StayTypes get mapped into
    //  the single StayType we're building the synpop for).
    private HashMap<Integer,ArrayList<StayType>> synPopShellMap;

    //This will be used to count the number of overnight visitor parties in the synthetic population.
    private int parties = 0;

    /**
     * Constructor.
     *
     * @param unitsToFill
     *        The double hash map described above in {@link #unitsToFill}.
     *
     * @param debugFlag
     *         The debug status of the module.
     */
    public OvernightVisitorSynpop(HashMap<Integer,HashMap<StayType,Integer>> unitsToFill, boolean debugFlag) {
        this.unitsToFill = unitsToFill;
        debug = debugFlag;
    }

    /**
     * Convenience constructor with debug set to {@code false}.
     *
     * @param unitsToFill
     *        The double hash map described above in {@link #unitsToFill}.
     */
    public OvernightVisitorSynpop(HashMap<Integer,HashMap<StayType,Integer>> unitsToFill) {
        this(unitsToFill, false);
    }

    //Read the file with the overnight visitor records which will be sampled to generate the synpop
    private void readOvernightVisitorRecords() {
        overnightVisitorRecords = null;
        String recordsFile = propertyMap.getString(recordsFileKey);

        try {
            CSVFileReader reader = new CSVFileReader();
            overnightVisitorRecords = reader.readFile(new File(recordsFile));
        } catch (IOException e) {
            throw new RuntimeException("Error reading HH file " + recordsFile);
        }
    }

    // This method fills the {@link #stayTypeRecords} and {@link #stayTypeSampleSize} {@code HashMap}s.
    private void createStayTypeRecords() {
        // Create reverse sample array
        HashMap<StayType,ArrayList<StayType>> reverseStayTypeSampleStructure = new HashMap<StayType,ArrayList<StayType>>();
        for (StayType st : StayType.values()) {
            reverseStayTypeSampleStructure.put(st,new ArrayList<StayType>());
        }
        for (StayType st : VisitorDataStructure.synPopSampleStructure.keySet()) {
            for (StayType st2 : VisitorDataStructure.synPopSampleStructure.get(st)) {
                reverseStayTypeSampleStructure.get(st2).add(st);
            }
        }
        //summary of the stay type sample structure
        if (debug) {
           for (StayType st : VisitorDataStructure.synPopSampleStructure.keySet()) {
                logger.info("Stay type synpop " + st + " is created from these stay types: " + Arrays.toString(VisitorDataStructure.synPopSampleStructure.get(st)));
            }
            for (StayType st : reverseStayTypeSampleStructure.keySet()) {
                logger.info("Stay type " + st + " is used in creating these stay type's synpops: " + reverseStayTypeSampleStructure.get(st));
            }
        }
        //Initialize a hash map of resizable arrays
        HashMap<StayType, ArrayList<Integer>> stRecordsMap = new HashMap<StayType, ArrayList<Integer>>();
        for (StayType st : StayType.values()) {
            stRecordsMap.put(st,new ArrayList<Integer>());
        }
        //Fill hash map with record id's corresponding to their stay type values
        for (int i = 1; i <= overnightVisitorRecords.getRowCount(); i++) {
            int stayTypeID = (int) overnightVisitorRecords.getValueAt(i,stayTypeColumn);
            int recordID = (int) overnightVisitorRecords.getValueAt(i,idColumn);
            if (StayType.isIDValid(stayTypeID)) {
                for (StayType st : reverseStayTypeSampleStructure.get(StayType.getStayType(stayTypeID))) {
                    stRecordsMap.get(st).add(recordID);
                }
            } else {
                logger.warn("Stay type ID " + stayTypeID + " (record id# " + recordID + ") is invalid!");
            }
        }
        //Create final arrays
        stayTypeRecords = new HashMap<StayType,Integer[]>();
        stayTypeSampleSize = new HashMap<StayType,Integer>();
        for (StayType st : stRecordsMap.keySet()) {
            //logger.info(stRecordsMap.get(st));
            stayTypeRecords.put(st,stRecordsMap.get(st).toArray(new Integer[0]));
            stayTypeSampleSize.put(st,stayTypeRecords.get(st).length);
        }
    }

    // Selects a random party id for a given stay type.
    private int getRandomParty(StayType st) {
        //Create a random integer between 0 and sample size (random numbers are in [0,1), so we won't ever get invalid values)
        int randomLocation = (int) Math.floor(SeededRandom.getRandom()*stayTypeSampleSize.get(st));
        //return id associated with location
        if (debug)
            logger.info("Stay type: " + st + ", overnight visitor record # " +
                    stayTypeRecords.get(st)[randomLocation] + " added to synpop sample");
        return stayTypeRecords.get(st)[randomLocation];
    }

    // Generate the synthetic population by filling the {@link #synPopShell}. Also, count how many parties make up the
    // synthetic population.
    private void generateOvernightVisitorSynPop() {
        //initialize hash map
        synPopShell = new HashMap<Integer,ArrayList<Integer>>();
        synPopShellMap = new HashMap<Integer,ArrayList<StayType>>();
        for (Integer zone : unitsToFill.keySet()) {
            synPopShell.put(zone,new ArrayList<Integer>());
            synPopShellMap.put(zone,new ArrayList<StayType>());
            //fill each zone with random parties from staytype/zone pairs
            for (StayType st : unitsToFill.get(zone).keySet()) {
                for (int i = 0; i < unitsToFill.get(zone).get(st);i++) {
                    synPopShell.get(zone).add(getRandomParty(st));
                    synPopShellMap.get(zone).add(st);
                    parties++;
                }
            }
        }
    }

    //set walk segment (0-none, 1-short, 2-long walk to transit access) for the origin for this tour
    //This method is blatantly ripped off from the non-static one in {@link com.pb.tahoe.synpop.HH}
    private int getInitialOriginWalkSegment (int taz) {
        double[] proportions = new double[ZonalDataManager.WALK_SEGMENTS];
        for (int i=0; i < ZonalDataManager.WALK_SEGMENTS; i++)
            proportions[i] = ZonalDataManager.getWalkPct(i, taz);
        return ChoiceModelApplication.getMonteCarloSelection(proportions);
    }

    //Creates a data table from the synpop sample generated in generateOvernightVisitorSynPop
    private TableDataSet generateSynPopData() {

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
        overnightVisitorRecords.buildIndex(overnightVisitorRecords.getColumnPosition(idColumn));

        //Fill data set
        float[][] synPopData = new float[parties][headings.size()];
        //unique id counter
        int uid = 0;
        for (int zone : synPopShell.keySet()) {
            int synPopShellLocator = 0;
            for (int id : synPopShell.get(zone)) {
                synPopData[uid][1] = id;
                synPopData[uid][2] = VisitorType.OVERNIGHT.getID();
                synPopData[uid][3] = zone;
                synPopData[uid][4] = getInitialOriginWalkSegment(zone);
                synPopData[uid][5] = synPopShellMap.get(zone).get(synPopShellLocator).getID();
                synPopData[uid][6] = overnightVisitorRecords.getIndexedValueAt(id, VisitorDataStructure.RECORDS_PERSONS_FIELD);
                synPopData[uid][7] = overnightVisitorRecords.getIndexedValueAt(id, VisitorDataStructure.RECORDS_CHILDREN_FIELD);
                synPopData[uid][8] = overnightVisitorRecords.getIndexedValueAt(id, VisitorDataStructure.RECORDS_FEMALEADULT_FIELD);
                synPopData[uid][0] = 1 + uid++;

                //Check to make sure sample actually matches up to the synPopSampleStructure
                if (!Arrays.asList(VisitorDataStructure.synPopSampleStructure.get(synPopShellMap.get(zone).get(synPopShellLocator))).contains(
                        StayType.getStayType(Math.round(overnightVisitorRecords.getIndexedValueAt(id, VisitorDataStructure.RECORDS_STAYTYPE_FIELD)))))
                    logger.info("Zone: " + zone + ", StayType: " + synPopShellMap.get(zone).get(synPopShellLocator) +
                            " - Sample id: " + id + " has an incorrect Stay Type value (" +
                            StayType.getStayType(Math.round(overnightVisitorRecords.getIndexedValueAt(id, VisitorDataStructure.RECORDS_STAYTYPE_FIELD))) + ")");
                
                synPopShellLocator++;
            }
        }
        return TableDataSet.create(synPopData,headings);
    }

    //Writes synpop data to csv file
    private void writeResults(TableDataSet overnightVisitorSynPopTable) {
        DataWriter dw = DataWriter.getInstance();
        dw.writeOutputFile(synpopResultsFile,overnightVisitorSynPopTable);
    }

    /**
     * Build the overnight visitor sythetic population.
     *
     * @return the data set with the synthetic population and its attributes
     */
    public TableDataSet buildOvernightVisitorSynPop() {
        readOvernightVisitorRecords();
        createStayTypeRecords();
        generateOvernightVisitorSynPop();
        //TableDataSet overnightSynPopTable = generateSynPopData();
        //writeResults(overnightSynPopTable);
        //return overnightSynPopTable;
        return generateSynPopData(); 
    }

    
}
