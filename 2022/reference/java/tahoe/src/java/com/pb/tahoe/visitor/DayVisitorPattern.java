package com.pb.tahoe.visitor;

import com.pb.tahoe.visitor.structures.VisitorTour;
import com.pb.tahoe.visitor.structures.VisitorTourType;
import com.pb.tahoe.visitor.structures.VisitorPattern;
import com.pb.tahoe.visitor.structures.VisitorDataStructure;
import com.pb.tahoe.util.DataWriter;
import com.pb.common.util.ResourceUtil;
import com.pb.common.datafile.TextFile;
import com.pb.common.datafile.TableDataSet;
import org.apache.log4j.Logger;

import java.util.ResourceBundle;
import java.util.Arrays;
import java.io.*;

/**
 * User: Chris
 * Date: Mar 13, 2007 - 1:56:29 PM
 */
public class DayVisitorPattern implements VisitorPattern {

    protected static Logger logger = Logger.getLogger(DayVisitorPattern.class);

    static ResourceBundle rb = ResourceUtil.getResourceBundle("tahoe");
    protected static boolean debug = false;

    static final String patternAlternativesKey = "day.pattern.alternative.set.file";
    static final String patternAlternativesDataKey = "day.pattern.alternative.data.file";

    private static DayVisitorPattern instance = new DayVisitorPattern();

    private String[] originalPatterns;
    private String[] overnightEquivalentPatterns;
    //This maps the day visitor pattern id to the equivalent overnight visitor pattern id
    private int[] overnightEquivalentPattern;

    private byte[] thruTripArray = null;

    OvernightVisitorPattern ovp = OvernightVisitorPattern.getInstance();

    private DayVisitorPattern() {
        generatePatternArrays();
    }

    public static DayVisitorPattern getInstance() {
        return instance;
    }

    public void generatePatternAlternativeData() {

        //create headers for data set
        // The columns are:
        // a: the alternative id
        // acts: the number of activities
        // rec: recreation tour
        // game: gaming tour
        // shop: shop tour
        // other: other tour
        // obStop: presene of an outbound stop
        // twoStops: presence of two stops
        // recStop: presence of recreation stops
        // gamStop: presence of gaming stop
        // shpStop: presence of shopping stop
        // onlyRec: only recreation activities
        String[] headings = {"a",
                             "acts",
                             "rec",
                             "game",
                             "shop",
                             "other",
                             "stops",
                             "obStop",
                             "twoStops",
                             "recStop",
                             "gameStop",
                             "shopStop",
                             "onlyRec"};

        //create empty array for generating data set
        float[][] patternData = new float[originalPatterns.length - 1][headings.length];
        //loop over pattern data and fill in array
        String gameChar = String.valueOf(VisitorTourType.Gaming.getIDChar());
        String recChar = String.valueOf(VisitorTourType.Recreation.getIDChar());
        String shopChar = String.valueOf(VisitorTourType.Shopping.getIDChar());
        String otherChar = String.valueOf(VisitorTourType.Other.getIDChar());
        int counter = 0;
        int patternCounter = -1;
        for (String pattern : overnightEquivalentPatterns) {
            patternCounter++;
            if (pattern == null) continue;
            Arrays.fill(patternData[counter],0.0f);
            //if (pattern.contains(gameChar)) primact =
            patternData[counter][1] = pattern.length() - 2;
            if (pattern.contains(recChar))
                patternData[counter][2] = 1;
            if (pattern.contains(gameChar))
                patternData[counter][3] = 1;
            if (pattern.contains(shopChar))
                patternData[counter][4] = 1;
            if (pattern.contains(otherChar))
                patternData[counter][5] = 1;
            if (pattern.contains(VisitorDataStructure.stopChar))
                patternData[counter][6] = 1;
            if (pattern.substring(1,2).equals(VisitorDataStructure.stopChar))
                patternData[counter][7] = 1;
            if ((patternData[counter][7] == 1) &&
                    (pattern.substring(3,4).equals(VisitorDataStructure.stopChar)))
                patternData[counter][8] = 1;
            if (originalPatterns[patternCounter].split(recChar).length > patternData[counter][2] + 1)
                patternData[counter][9] = 1;
            if (originalPatterns[patternCounter].split(gameChar).length > patternData[counter][3] + 1)
                patternData[counter][10] = 1;
            if (originalPatterns[patternCounter].split(shopChar).length > patternData[counter][4] + 1)
                patternData[counter][11] = 1;
            if (originalPatterns[patternCounter].replaceAll(recChar,"").replaceAll(VisitorDataStructure.homeChar,"").equals(""))
                patternData[counter][12] = 1;
            patternData[counter++][0] = counter;

        }
        //Write out results to alternatives table
        DataWriter dw = DataWriter.getInstance();
        dw.writeOutputFile(patternAlternativesDataKey, TableDataSet.create(patternData,headings));
    }

    //get tour array
    public VisitorTour[] getToursFromPatternID(int id) {
        if (id == -1) {
            VisitorTour[] thruTour = null;
            try {
                ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(getThruTripTourArray()));
                thruTour = (VisitorTour[]) in.readObject();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (ClassNotFoundException cnfe) {
                cnfe.printStackTrace();
            }
            return thruTour;
        }
        return ovp.getToursFromPatternID(overnightEquivalentPattern[id]);
    }

    //get string pattern
    public String getPatternFromID(int id) {
        if (id == -1) return "THRU";
        return originalPatterns[id];
    }

    private void generatePatternArrays() {
        TextFile patternData;
        patternData = new TextFile(rb.getString(patternAlternativesKey));
        originalPatterns = new String[patternData.size()+1];
        overnightEquivalentPatterns = new String[patternData.size()+1];
        overnightEquivalentPattern = new int[patternData.size()+1];
        for (String line : patternData) {
            String[] lineSplit = line.split(",");
            //skip if a hanging eol
            if (lineSplit.length != 3) continue;
            originalPatterns[Integer.valueOf(lineSplit[0])] = lineSplit[1];
            overnightEquivalentPatterns[Integer.valueOf(lineSplit[0])] = lineSplit[2];
            for (int i = 1; i <= ovp.getPatternCount(); i++) {
                if (ovp.getPatternFromID(i).equals(lineSplit[2])) {
                    overnightEquivalentPattern[Integer.valueOf(lineSplit[0])] = i;
                    break;
                }
            }
        }
    }

    private byte[] getThruTripTourArray() {
        if (thruTripArray == null) {
            VisitorTour[] thruTripTourArray = new VisitorTour[1];
            VisitorTour thru = new VisitorTour();
            thru.setTourNum(1);
            thru.setOutboundStop(false);
            thru.setInboundStop(false);
            thru.setTourType(VisitorTourType.Other);
            thruTripTourArray[0] = thru;
            try {
                //write object to byte array
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                ObjectOutputStream out = new ObjectOutputStream(bos);
                out.writeObject(thruTripTourArray);
                out.flush();
                out.close();
                thruTripArray = bos.toByteArray();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return thruTripArray;
    }

}
