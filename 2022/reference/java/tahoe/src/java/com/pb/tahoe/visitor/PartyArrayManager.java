package com.pb.tahoe.visitor;

import org.apache.log4j.Logger;

import java.util.ResourceBundle;
import java.util.HashMap;
import java.util.ArrayList;
import java.io.File;
import java.io.IOException;

import com.pb.common.util.ResourceUtil;
import com.pb.common.datafile.TableDataSet;
import com.pb.common.datafile.CSVFileReader;
import com.pb.tahoe.visitor.structures.*;
import com.pb.tahoe.util.DataWriter;
import com.pb.tahoe.util.TODDataManager;

/**
 * User: Chris
 * Date: Feb 8, 2007 - 12:41:36 AM
 */
public class PartyArrayManager {

    static Logger logger = Logger.getLogger(PartyArrayManager.class);

    static ResourceBundle propertyMap = ResourceUtil.getResourceBundle("tahoe");

    //static names for the models for the finishedFlags keys
    public static final String PATTERN_KEY = "pattern";
    public static final String DC_KEY = "dc";
    public static final String TOD_KEY = "tod";
    public static final String MC_KEY = "mc";
    public static final String STOP_DC_KEY = "stopDC";
    public static final String STOP_MC_KEY = "stopMC";

    //This is the array of travel parties in the synthetic population
    public TravelParty[] parties = null;

    /**
     * A static instance of the PartyArrayManager.  This ensures all references to this class always get the same
     *  object instance.
     */
    public static PartyArrayManager instance = new PartyArrayManager();

    /**
     * This is the number of parties in the overnight visitor synpop.  This isn't set until createPartyArray is called,
     *  so if you get a zero, that is why.
     */
    public int partyCount = 0;

    //the following are flags which tell at what point the model run is at
    private HashMap<String,Boolean> finishedFlags = new HashMap<String,Boolean>();


    //Private constructor ensuring no duplicate object instances are instantiated.
    private PartyArrayManager() {
        finishedFlags.put(PATTERN_KEY,false);
        finishedFlags.put(DC_KEY,false);
        finishedFlags.put(TOD_KEY,false);
        finishedFlags.put(MC_KEY,false);
        finishedFlags.put(STOP_DC_KEY,false);
        finishedFlags.put(STOP_MC_KEY,false);
    }

    /**
     * Gets the (only) existing instance of the PartyArrayManager.
     *
     * @return the array manager instance.
     */
    public static PartyArrayManager getInstance(){
        return instance;
    }
    /**
     * Fill up all of the TravelParty objects with all of the available data.
     *
     * @param partyData
     *        The data set containing travel party information.
     */
    public void createPartyArray(TableDataSet partyData) {

        //use a temporary array, so if something goes wrong, the actual parties array doesn't get written over
        //indexing is by party id, which starts at one, so the first element will be left null
        TravelParty[] tempParties = new TravelParty[partyData.getRowCount() + 1];

        //fill the array with travel parties
        for (int i = 1; i <= partyData.getRowCount(); i++) {
            //Do all of the data that should be there
            tempParties[i] = new TravelParty();
            tempParties[i].setID((int) partyData.getValueAt(i, VisitorDataStructure.ID_FIELD));
            tempParties[i].setSampn((int) partyData.getValueAt(i, VisitorDataStructure.SAMPN_FIELD));
            tempParties[i].setVisitorType(VisitorType.getVisitorType((int) partyData.getValueAt(i, VisitorDataStructure.VISITORTYPE_FIELD)));
            tempParties[i].setTazID((int) partyData.getValueAt(i, VisitorDataStructure.STAYTAZ_FIELD));
            tempParties[i].setStayWalkSegment((int) partyData.getValueAt(i, VisitorDataStructure.WALKSEGMENT_FIELD));
            tempParties[i].setStayType(StayType.getStayType((int) partyData.getValueAt(i, VisitorDataStructure.STAYTYPE_FIELD)));
            tempParties[i].setPersons((int) partyData.getValueAt(i, VisitorDataStructure.PERSONS_FIELD));
            tempParties[i].setChildren((int) partyData.getValueAt(i, VisitorDataStructure.CHILDREN_FIELD));
            tempParties[i].setFemaleAdult((((int) partyData.getValueAt(i, VisitorDataStructure.FEMALEADULT_FIELD)) == 1));
            tempParties[i].setSummer(ResourceUtil.getBooleanProperty(propertyMap,"summer"));

            //Next up is doing all of the tour stuff...
            //Create tour array from pattern
            if (partyData.getColumnPosition(VisitorDataStructure.PATTERN_FIELD) != -1) {
                VisitorPattern vp;
                if (VisitorType.getVisitorType(tempParties[i].getVisitorType()) == VisitorType.OVERNIGHT)
                    vp = OvernightVisitorPattern.getInstance();
                else
                    vp = DayVisitorPattern.getInstance();
                int patternID = (int) partyData.getValueAt(i, VisitorDataStructure.PATTERN_FIELD);
                tempParties[i].setPattern(patternID);
                tempParties[i].setTours(vp.getToursFromPatternID(patternID));
                tempParties[i].setPatternString(vp.getPatternFromID(patternID));
                if (!finishedFlags.get(PATTERN_KEY)) modelDone(PATTERN_KEY);

                //Fill in tour arrays with any available info
                //Don't even bother if primary destination field unavailable
                if (partyData.getColumnPosition(VisitorDataStructure.DEST_FIELD + "1") != -1) {
                    //loop over every tour in this party's pattern
                    for (int tour = 1; tour <= tempParties[i].getNumberOfTours(); tour++){
                        //Primary destination taz data
                        if (partyData.getColumnPosition(VisitorDataStructure.DEST_FIELD + tour) != -1) {
                            if (!finishedFlags.get(DC_KEY)) modelDone(DC_KEY);
                            int[] destData = VisitorDTM.getDestInfo(
                                    (int) partyData.getValueAt(i, VisitorDataStructure.DEST_FIELD + tour));
                            if (destData.length != 0) {
                                tempParties[i].getTours()[tour - 1].setDestTAZ(destData[0]);
                                tempParties[i].getTours()[tour - 1].setDestWalkSegment(destData[1]);
                            }
                        }
                        //TOD information
                        if (partyData.getColumnPosition(VisitorDataStructure.TOD_FIELD + tour) != -1) {
                            if (!finishedFlags.get(TOD_KEY)) modelDone(TOD_KEY);
                            tempParties[i].getTours()[tour - 1].setTimeOfDayAlt(
                                    (int) partyData.getValueAt(i, VisitorDataStructure.TOD_FIELD + tour));
                        }
                        //Mode information
                        if (partyData.getColumnPosition(VisitorDataStructure.MODE_FIELD + tour) != -1) {
                            if (!finishedFlags.get(MC_KEY)) modelDone(MC_KEY);
                            tempParties[i].getTours()[tour - 1].setMode(VisitorMode.getMode(
                                    (int) partyData.getValueAt(i, VisitorDataStructure.MODE_FIELD + tour)));
                        }
                        if (!finishedFlags.get(STOP_DC_KEY))
                            if (partyData.getColumnPosition(VisitorDataStructure.OBDEST_FIELD + tour) != -1)
                                modelDone(STOP_DC_KEY);
                        if (!finishedFlags.get(STOP_MC_KEY))
                            if (partyData.getColumnPosition(VisitorDataStructure.OBMODE_FIELD + tour) != -1)
                                modelDone(STOP_MC_KEY);
                        if (finishedFlags.get(STOP_DC_KEY)) {
                            //OB Stop
                            if (tempParties[i].getTours()[tour - 1].getOutboundStop()) {
                                // Destination taz data
                                if (partyData.getColumnPosition(VisitorDataStructure.OBDEST_FIELD + tour) != -1) {
                                    int[] destData = VisitorDTM.getDestInfo(
                                            (int) partyData.getValueAt(i, VisitorDataStructure.OBDEST_FIELD + tour));
                                    if (destData.length != 0) {
                                        tempParties[i].getTours()[tour - 1].setObTAZ(destData[0]);
                                        tempParties[i].getTours()[tour - 1].setObWalkSegment(destData[1]);
                                    }
                                }
                                //Mode information
                                if (partyData.getColumnPosition(VisitorDataStructure.OBMODE_FIELD + tour) != -1) {
                                    tempParties[i].getTours()[tour - 1].setObMode(VisitorTripMode.getMode(
                                            (int) partyData.getValueAt(i, VisitorDataStructure.OBMODE_FIELD + tour)));
                                }
                            }
                            //IB Stop
                            if (tempParties[i].getTours()[tour - 1].getInboundStop()) {
                                // Destination taz data
                                if (partyData.getColumnPosition(VisitorDataStructure.IBDEST_FIELD + tour) != -1) {
                                    int[] destData = VisitorDTM.getDestInfo(
                                            (int) partyData.getValueAt(i, VisitorDataStructure.IBDEST_FIELD + tour));
                                    if (destData.length != 0) {
                                        tempParties[i].getTours()[tour - 1].setIbTAZ(destData[0]);
                                        tempParties[i].getTours()[tour - 1].setIbWalkSegment(destData[1]);
                                    }
                                }
                                //Mode information
                                if (partyData.getColumnPosition(VisitorDataStructure.IBMODE_FIELD + tour) != -1) {
                                    tempParties[i].getTours()[tour - 1].setIbMode(VisitorTripMode.getMode(
                                            (int) partyData.getValueAt(i, VisitorDataStructure.IBMODE_FIELD + tour)));
                                }
                            }
                        }
                    }
                }
            }
        }

        partyCount = tempParties.length - 1;
        String tailMessage;
        if (parties == null) {
            tailMessage = "created.";
        } else {
            tailMessage = "updated.";
        }
        parties = tempParties;
        PartyArrayManager.logger.info("\tParty Array has been " + tailMessage);
    }

    /**
     * Fill up all of the TravelParty objects with all of the available data read from a csv file.
     *
     * @param property
     *        The property key of the data file.
     */
    public void createPartyArray(String property) {
        TableDataSet partyData = getTravelPartyArrayFromFile(property);

        if (partyData != null) {
            createPartyArray(partyData);
        } else {
            PartyArrayManager.logger.warn("\tError: Party Array has NOT been created/updated!");
        }
    }

    /**
     * Update a travel party (shouldn't need this, as no clone/deepcopy methods should be called on TravelParty objects.
     *
     * @param party
     *        The party to update.
     */
    public void updateParty(TravelParty party) {
        parties[party.getID()] = party;
    }

    /**
     * Same as {@link #updateParty} except it updates a collection of parties.
     *
     * @param parties
     *        An array of parties to update.
     */
    public void updateParties(TravelParty[] parties) {
        for (TravelParty party : parties) {
            updateParty(party);
        }
    }

    public void writePartyData(String outFileKey) {
        //Create base table headings
        String[] headings = {VisitorDataStructure.ID_FIELD,
                             VisitorDataStructure.VISITORTYPE_FIELD,
                             VisitorDataStructure.SAMPN_FIELD,
                             VisitorDataStructure.STAYTAZ_FIELD,
                             VisitorDataStructure.WALKSEGMENT_FIELD,
                             VisitorDataStructure.STAYTYPE_FIELD,
                             VisitorDataStructure.PERSONS_FIELD,
                             VisitorDataStructure.CHILDREN_FIELD,
                             VisitorDataStructure.FEMALEADULT_FIELD};
        //intialize base data table and the accessory columns - even if we don't need them
        float[][] baseData = new float[partyCount][headings.length];
        float[] patternData = new float[partyCount];
        //Overnight visitors will have the largest patterns
        int maxTours = OvernightVisitorPattern.getInstance().getMaxTours();
        float[][] dcData = new float[maxTours][partyCount];
        float[][] todData = new float[maxTours][partyCount];
        float[][] mcData = new float[maxTours][partyCount];
        float[][] obDCData = new float[maxTours][partyCount];
        float[][] obMCData = new float[maxTours][partyCount];
        float[][] ibDCData = new float[maxTours][partyCount];
        float[][] ibMCData = new float[maxTours][partyCount];

        for (int i = 1; i <= partyCount; i++) {
            TravelParty party = parties[i];
            //fill in base data
            baseData[i-1][0] = party.getID();
            baseData[i-1][1] = party.getVisitorType();
            baseData[i-1][2] = party.getSampn();
            baseData[i-1][3] = party.getTazID();
            baseData[i-1][4] = party.getStayWalkSegment();
            baseData[i-1][5] = party.getStayType().getID();
            baseData[i-1][6] = party.getPersons();
            baseData[i-1][7] = party.getChildren();
            if (party.getFemaleAdult())
                baseData[i-1][8] = 1;
            else
                baseData[i-1][8] = 0;
            //now do extra columns, if we can
            VisitorTour[] partyTours;
            if (finishedFlags.get(PATTERN_KEY)) {
                patternData[i-1] = party.getPattern();
                partyTours = party.getTours();

                if (finishedFlags.get(DC_KEY)) {
                    for (int j = 0; j < maxTours; j++) {
                        if (j < partyTours.length)
                            dcData[j][i-1] = VisitorDTM.getDestAlt(
                                    partyTours[j].getDestTAZ(),partyTours[j].getDestWalkSegment());
                        else
                            dcData[j][i-1] = -1;
                    }
                }
                if (finishedFlags.get(TOD_KEY)) {
                    for (int j = 0; j < maxTours; j++) {
                        if (j < partyTours.length)
                            todData[j][i-1] = partyTours[j].getTimeOfDayAlt();
                        else
                            todData[j][i-1] = -1;
                    }
                }
                if (finishedFlags.get(MC_KEY)) {
                    for (int j = 0; j < maxTours; j++) {
                        if (j < partyTours.length)
                            mcData[j][i-1] = partyTours[j].getMode().getId();
                        else
                            mcData[j][i-1] = -1;
                    }
                }
                if (finishedFlags.get(STOP_DC_KEY)) {
                    for (int j = 0; j < maxTours; j++) {
                        if ((j < partyTours.length) && partyTours[j].getOutboundStop()) {
                            obDCData[j][i-1] = VisitorDTM.getDestAlt(
                                    partyTours[j].getObTAZ(),partyTours[j].getObWalkSegment());
                            if (finishedFlags.get(STOP_MC_KEY))
                                obMCData[j][i-1] = partyTours[j].getObMode().getId();
                        } else {
                            obDCData[j][i-1] = -1;
                            obMCData[j][i-1] = -1;
                        }
                        if ((j < partyTours.length) && partyTours[j].getInboundStop()) {
                            ibDCData[j][i-1] = VisitorDTM.getDestAlt(
                                    partyTours[j].getIbTAZ(),partyTours[j].getIbWalkSegment());
                            if (finishedFlags.get(STOP_MC_KEY))
                                ibMCData[j][i-1] = partyTours[j].getIbMode().getId();
                        } else {
                            ibDCData[j][i-1] = -1;
                            ibMCData[j][i-1] = -1;
                        }
                    }
                }
            }
        }

        //now create table data set
        TableDataSet partyData = TableDataSet.create(baseData,headings);
        if (finishedFlags.get(PATTERN_KEY)) partyData.appendColumn(patternData, VisitorDataStructure.PATTERN_FIELD);
        for (int j = 1; j <= maxTours; j++) {
            if (finishedFlags.get(DC_KEY)) partyData.appendColumn(dcData[j-1], VisitorDataStructure.DEST_FIELD + j);
            if (finishedFlags.get(TOD_KEY)) partyData.appendColumn(todData[j-1], VisitorDataStructure.TOD_FIELD + j);
            if (finishedFlags.get(MC_KEY)) partyData.appendColumn(mcData[j-1], VisitorDataStructure.MODE_FIELD + j);
            if (finishedFlags.get(STOP_DC_KEY)) {
                partyData.appendColumn(obDCData[j-1], VisitorDataStructure.OBDEST_FIELD + j);
                if (finishedFlags.get(STOP_MC_KEY))
                    partyData.appendColumn(obMCData[j-1], VisitorDataStructure.OBMODE_FIELD + j);
                partyData.appendColumn(ibDCData[j-1], VisitorDataStructure.IBDEST_FIELD + j);
                if (finishedFlags.get(STOP_MC_KEY))
                    partyData.appendColumn(ibMCData[j-1], VisitorDataStructure.IBMODE_FIELD + j);
            }
        }

        
        DataWriter dw = DataWriter.getInstance();
        dw.writeOutputFile(outFileKey,partyData);
    }

    /**
     * Marks in {@link #finishedFlags} that a certain model is done.  Keys are static final constants declared early
     * in this class.
     *
     * @param model
     *        The key indicating the model which has finished.
     */
    public void modelDone(String model) {
        if (finishedFlags.containsKey(model)) {
            finishedFlags.put(model,true);
        } else {
            logger.warn("Model name " + model + " not found in finishedFlags keySet!");
        }
    }

    /**
     * Tells the party array manager that no models have finished.
     */
    public void setAllModelsNotDone() {
        for (String model : finishedFlags.keySet()) {
            finishedFlags.put(model,false);
        }
    }

    /**
     * This method merges two population data sets into one.  It assumes the first (overnight) population has the
     * "master" list of columns.
     *
     * @param overnightPopulation
     *        The overnight visitor population.
     *
     * @param dayPopulation
     *        The day visitor population;
     *
     * @return the merged population.
     */
    public static TableDataSet mergeVisitorPopulations(TableDataSet overnightPopulation, TableDataSet dayPopulation) {
        //check to make sure that the columns exist in both files
        String[] columns = overnightPopulation.getColumnLabels();
        for (String column : columns) {
            assert dayPopulation.getColumnPosition(column) > -1;
        }
        float[][] fullPop = new float[overnightPopulation.getRowCount() + dayPopulation.getRowCount()][columns.length];
        int rowCounter = 0;
        for (TableDataSet tds : new TableDataSet[] {overnightPopulation,dayPopulation}) {
            for (int i = 1; i <= tds.getRowCount(); i++) {
                int colCounter = 0;
                for (String column : columns) {
                    //redo id fields
                    if (column.equals(VisitorDataStructure.ID_FIELD))
                        fullPop[rowCounter][colCounter++] = rowCounter+1;
                    else
                        fullPop[rowCounter][colCounter++] = tds.getValueAt(i,tds.getColumnPosition(column));
                }
                rowCounter++;
            }

        }
        //TableDataSet tp = TableDataSet.create(fullPop,columns);
        return TableDataSet.create(fullPop,columns);
    }

    /**
     * This method merges two population data sets into one.  It assumes the first (overnight) population has the
     * "master" list of columns.
     *
     * @param overnightPopulationProperty
     *        The resource bundle property pointing to the overnight visitor population csv file.
     *
     * @param dayPopulationProperty
     *        The resource bundle property pointing to the day visitor population csv file.
     *
     * @return the merged population.
     */
    public static TableDataSet mergeVisitorPopulations(String overnightPopulationProperty, String dayPopulationProperty) {
        return mergeVisitorPopulations(getTravelPartyArrayFromFile(overnightPopulationProperty),
                getTravelPartyArrayFromFile(dayPopulationProperty));
    }

    /**
     * Get a travel party array from a csv file.
     *
     * @param property
     *        The resource bundle property pointing to the visitor population csv file.
     *
     * @return the data set containing the array.
     */
    public static TableDataSet getTravelPartyArrayFromFile(String property) {
        TableDataSet partyData;
        String partyDataFile = propertyMap.getString(property);

        try {
            CSVFileReader reader = new CSVFileReader();
            partyData = reader.readFile(new File(partyDataFile));
        } catch (IOException e) {
            logger.warn("Error reading Travel Party file " + partyDataFile);
            partyData = null;
        }

        return partyData;
    }

    /**
     * This method transforms the party array into a table data set with one row for each tour (no tours get no rows) and
     * outputs into the specified file.
     *
     * @param outputFileProperty
     *        The resource bundle property containing the output file location.
     */
    public void partyArrayToReportsFile(String outputFileProperty) {
        //Create base table headings
        String[] headings = {VisitorDataStructure.ID_FIELD,
                             VisitorDataStructure.VISITORTYPE_FIELD,
                             VisitorDataStructure.SAMPN_FIELD,
                             VisitorDataStructure.ORIG_REPORTS_FIELD,
                             VisitorDataStructure.WALKSEGMENT_FIELD,
                             VisitorDataStructure.STAYTYPE_FIELD,
                             VisitorDataStructure.PERSONS_FIELD,
                             VisitorDataStructure.CHILDREN_FIELD,
                             VisitorDataStructure.FEMALEADULT_FIELD,
                             VisitorDataStructure.TOURTYPE_REPORTS_FIELD,
                             VisitorDataStructure.DEST_REPORTS_FIELD,
                             VisitorDataStructure.TOD_FIELD,
                             VisitorDataStructure.DEPHR_REPORTS_FIELD,
                             VisitorDataStructure.ARRHR_REPORTS_FIELD,
                             VisitorDataStructure.MODE_FIELD,
                             VisitorDataStructure.OBSTART_REPORTS_FIELD,
                             VisitorDataStructure.OBSTOP_REPORTS_FIELD,
                             VisitorDataStructure.OBMODE_FIELD,
                             VisitorDataStructure.IBSTART_REPORTS_FIELD,
                             VisitorDataStructure.IBSTOP_REPORTS_FIELD,
                             VisitorDataStructure.IBMODE_FIELD};
        //intialize base data table and the accessory columns - even if we don't need them
        ArrayList<ArrayList<Float>> reportData = new ArrayList<ArrayList<Float>>();

        TODDataManager toddm = TODDataManager.getInstance();
        for (TravelParty tp : parties) {
            if (tp == null) continue;
            VisitorTour[] tours = tp.getTours();
            for (VisitorTour tour : tours) {
                ArrayList<Float> dataRow = new ArrayList<Float>();
                dataRow.add((float) tp.getID());
                dataRow.add((float) tp.getVisitorType());
                dataRow.add((float) tp.getSampn());
                dataRow.add((float) tp.getTazID());
                dataRow.add((float) tp.getStayWalkSegment());
                dataRow.add((float) tp.getStayType().getID());
                dataRow.add((float) tp.getPersons());
                dataRow.add((float) tp.getChildren());
                if (tp.getFemaleAdult()) dataRow.add(1.0f);
                else dataRow.add(0.0f);
                dataRow.add((float) tour.getTourType().getID());
                dataRow.add((float) tour.getDestTAZ());
                int tod = tour.getTimeOfDayAlt();
                dataRow.add((float) tod);
                dataRow.add((float) TODDataManager.getTodStartHour(tod));
                dataRow.add((float) TODDataManager.getTodEndHour(tod));
                dataRow.add((float) tour.getMode().getId());
                dataRow.add((float) tp.getTazID());
                if (tour.getOutboundStop()) {
                    dataRow.add((float) tour.getObTAZ());
                    dataRow.add((float) tour.getObMode().getId());
                } else {
                    dataRow.add(0.0f);
                    dataRow.add(-1.0f);
                }
                dataRow.add((float) tour.getDestTAZ());
                if (tour.getInboundStop()) {
                    dataRow.add((float) tour.getIbTAZ());
                    dataRow.add((float) tour.getIbMode().getId());
                } else {
                    dataRow.add(0.0f);
                    dataRow.add(-1.0f);
                }
                reportData.add(dataRow);
            }
        }
        float[][] solidReportData = new float[reportData.size()][];
        int counter = 0;
        for (ArrayList<Float> al : reportData) {
            float[] dataRow = new float[headings.length];
            int subCounter = 0;
            for (Float f : al) {
                dataRow[subCounter++] = f;
            }
            solidReportData[counter++] = dataRow;
        }

        DataWriter dw = DataWriter.getInstance();
        dw.writeOutputFile(outputFileProperty,TableDataSet.create(solidReportData,headings));
    }

}
