package com.pb.tahoe.visitor;

import com.pb.tahoe.visitor.structures.StayType;
import com.pb.tahoe.visitor.structures.VisitorDataStructure;
import com.pb.tahoe.util.ZonalDataManager;
import com.pb.tahoe.synpop.ZonalData;
import com.pb.common.datafile.TableDataSet;
import com.pb.common.datafile.CSVFileReader;
import com.pb.common.util.ResourceUtil;

import java.util.HashMap;
import java.util.ResourceBundle;
import java.io.File;
import java.io.IOException;

import org.apache.log4j.Logger;

/**
 * User: Chris
 * Date: Feb 11, 2007 - 7:45:42 PM
 */
public class OvernightVisitorUnitsToFill {

    static Logger logger = Logger.getLogger(OvernightVisitorUnitsToFill.class);

    static ResourceBundle propertyMap = ResourceUtil.getResourceBundle("tahoe");

    public static final String unitsAvailableKey = "overnight.visitors.zonal.data.file";
    public static final String occupancyRatesKey = "overnight.visitor.occupancy.data.file";


    //The units availablel file should be formatted as such (by column):
    // taz, seasonal, hotelmotel, ..., campground
    // where the columns are named identical to the string name of the StayType enumeration
    // the taz column gives the taz, wheras the others give the # of units available for that stay
    // type in that zone
    // for the house category, there is a percentHouseSeasonal field, which says how many of the
    // "houses" should be seasonal, and how many should be overnight visitor "house" stay types
    private static TableDataSet readAvailableUnits() {
        TableDataSet unitsAvailableData = readDataSetFromFile(unitsAvailableKey);
        TableDataSet zonalData = ZonalDataManager.getInstance().getZonalTableDataSet();
        zonalData.buildIndex(zonalData.getColumnPosition(ZonalData.ZONE_FIELD));
        int[] seasonalUnits = new int[unitsAvailableData.getRowCount()];
        int[] overnightHouseUnits = new int[unitsAvailableData.getRowCount()];
        for (int i = 1; i <= unitsAvailableData.getRowCount(); i++) {
            int zone = (int) unitsAvailableData.getValueAt(i, VisitorDataStructure.OVZONAL_TAZ_FIELD);
            int houseUnits = (int) (zonalData.getIndexedValueAt(zone,ZonalData.TOTALHHS_FIELD) -
                    zonalData.getIndexedValueAt(zone,ZonalData.HHS_FIELD));
            seasonalUnits[i-1] = Math.round(houseUnits * unitsAvailableData.getValueAt(i, VisitorDataStructure.OVZONAL_SEASONALPERCENTAGE_FIELD));
            overnightHouseUnits[i-1] = houseUnits - seasonalUnits[i-1];
        }
        unitsAvailableData.appendColumn(seasonalUnits, StayType.SEASONAL.toString().toLowerCase());
        unitsAvailableData.appendColumn(overnightHouseUnits, StayType.HOUSE.toString().toLowerCase());
        return unitsAvailableData;
    }

    private static HashMap<Integer,HashMap<StayType,Integer>> generateUnitsToFill(TableDataSet unitsAvailable, HashMap<StayType,HashMap<Integer,Float>> percentsToFill) {
        HashMap<Integer,HashMap<StayType,Integer>> unitsToFill = new HashMap<Integer,HashMap<StayType,Integer>>();
        //loop over every element in the data set
        for (int row = 1; row <= unitsAvailable.getRowCount(); row++) {
            int taz = Math.round(unitsAvailable.getValueAt(row, "taz"));
            for (StayType st : StayType.values()) {
                //ignore if no units available
                if (unitsAvailable.getValueAt(row, st.toString().toLowerCase()) == 0)
                    continue;
                if (!unitsToFill.containsKey(taz))
                    unitsToFill.put(taz,new HashMap<StayType,Integer>());
                int units = Math.round(unitsAvailable.getValueAt(row, st.toString().toLowerCase()) *
                        percentsToFill.get(st).get(taz));
                unitsToFill.get(taz).put(st,units);
            }
        }

        return unitsToFill;
    }

    private static TableDataSet readDataSetFromFile(String fileKey) {
        TableDataSet data;
        String file = propertyMap.getString(fileKey);
        try {
            CSVFileReader reader = new CSVFileReader();
            data = reader.readFile(new File(file));
        } catch (IOException e) {
            throw new RuntimeException("Error reading Units Available file " + file);
        }
        return data;
    }

    /**
     * This method builds the units to fill double hash map by taking a percentage (stratified by {@code StayType}) of
     * the units available as the units to fill.
     *
     * @param globalPercentsToFill
     *        Percent of each {@code StayType} to fill.
     *
     * @return the units to fill double hash map required to generate the overnight visitor synthetic population.
     */
    public static HashMap<Integer,HashMap<StayType,Integer>> generateUnitsToFill (HashMap<StayType,Float> globalPercentsToFill) {
        TableDataSet availableUnits = readAvailableUnits();
        HashMap<StayType,HashMap<Integer,Float>> percentsToFill = new HashMap<StayType,HashMap<Integer,Float>>();
        for (int i = 1; i <= availableUnits.getRowCount(); i++) {
            for (StayType st : globalPercentsToFill.keySet()) {
                if (!percentsToFill.containsKey(st))
                    percentsToFill.put(st,new HashMap<Integer,Float>());
                percentsToFill.get(st).put((int) availableUnits.getValueAt(i, VisitorDataStructure.OVZONAL_TAZ_FIELD),
                        globalPercentsToFill.get(st));
            }
        }
        return generateUnitsToFill(availableUnits, percentsToFill);
    }

    /**
     * This method builds the units to fill double hash map from a table data set which is formatted the same as the units
     * available file, except it is filled with the percent of units to fill (not the available units)
     *
     * @param percentUnitsToFill
     *        Data set telling how many units to fill.
     *
     * @return the units to fill double hash map required to generate the overnight visitor synthetic population.
     */
    public static HashMap<Integer,HashMap<StayType,Integer>> generateUnitsToFill (TableDataSet percentUnitsToFill) {
        HashMap<StayType,HashMap<Integer,Float>> percentsToFill = new HashMap<StayType,HashMap<Integer,Float>>();
        for (int row = 1; row <= percentUnitsToFill.getRowCount(); row++) {
            int taz = (int) percentUnitsToFill.getValueAt(row, VisitorDataStructure.OVZONAL_TAZ_FIELD);
            for (StayType st : StayType.values()) {
                if (!percentsToFill.containsKey(st))
                    percentsToFill.put(st,new HashMap<Integer,Float>());
                percentsToFill.get(st).put(taz, percentUnitsToFill.getValueAt(row, st.toString().toLowerCase()));
            }
        }
        return generateUnitsToFill(readAvailableUnits(),percentsToFill);
    }

    /**
     * Builds the units to fil double hash map for a csv file containing the percent of units to fill by stay type and
     * zone
     *
     * @param percentUnitsToFillKey
     *        The property pointing to the percent of units to fill file.
     *
     * @return the units to fill double hash map required to generate the overnight visitor synthetic population.
     */
    public static HashMap<Integer,HashMap<StayType,Integer>> generateUnitsToFill (String percentUnitsToFillKey) {
        return generateUnitsToFill(readDataSetFromFile(percentUnitsToFillKey));
    }

    /**
     * Refactors a units to fill double hash map by multiplying all the units to fill numbers by a given float value.
     *
     * @param unitsToFill
     *        The original input double hash map.
     *
     * @param refactor
     *        The refactor value.
     *
     * @return the refactored units to fill double hash map required to generate the overnight visitor synthetic population.
     */
    public static HashMap<Integer,HashMap<StayType,Integer>> refactorUnitsToFill (HashMap<Integer,HashMap<StayType,Integer>> unitsToFill, Float refactor) {
        for (Integer taz : unitsToFill.keySet()) {
            for (StayType st : unitsToFill.get(taz).keySet()) {
                unitsToFill.get(taz).put(st,(int) (unitsToFill.get(taz).get(st) * refactor));
            }
        }
            return unitsToFill;
    }
}
