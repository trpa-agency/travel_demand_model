package com.pb.tahoe.visitor;

import com.pb.common.datafile.TextFile;
import com.pb.common.datafile.TableDataSet;
import com.pb.common.util.ResourceUtil;
import com.pb.tahoe.visitor.structures.VisitorTour;
import com.pb.tahoe.visitor.structures.VisitorTourType;
import com.pb.tahoe.visitor.structures.VisitorPattern;
import com.pb.tahoe.visitor.structures.VisitorDataStructure;
import com.pb.tahoe.util.DataWriter;

import java.util.HashMap;
import java.util.ResourceBundle;
import java.io.*;

import org.apache.log4j.Logger;

/**
 * User: Chris
 * Date: Mar 13, 2007 - 12:58:47 PM
 */
public class OvernightVisitorPattern implements VisitorPattern {

    protected static Logger logger = Logger.getLogger(OvernightVisitorPattern.class);

    static ResourceBundle rb = ResourceUtil.getResourceBundle("tahoe");
    protected static boolean debug = false;

    static final String patternAlternativesKey = "overnight.pattern.alternative.set.file";
    static final String patternAlternativesDataKey = "overnight.pattern.alternative.data.file";

    static int patternCount = 0;

    /**
     * Singleton instance of this class. There is no reason to have any more than one of these around.
     */
    private static OvernightVisitorPattern instance = new OvernightVisitorPattern();

    private static int maxTours;

    private OvernightVisitorPattern() { }

    /**
     *
     * @return singleton instance.
     */
    public static OvernightVisitorPattern getInstance() {
        return instance;
    }

    public int getMaxTours() {
        //build patternToTourMap if it has not been done yet
        if (patternToTourMap == null) buildPatternToTourMap();
        return maxTours;
    }

    public int getPatternCount() {
        if (patternCount == 0)
            patternCount = getPatternAlts().size();
        return patternCount;
    }

    public void generatePatternAlternativeData() {
        TextFile patternAlts = getPatternAlts();

        //create headers for data set
        // The columns are:
        // alt: the alternative id
        // tours: number of tours in pattern
        // obStops: number of outbound stops in pattern
        // ibStops: number of inbound stops in pattern
        // gaming: number of gaming tours in pattern
        // rec: number of recreation tours in pattern
        // shop: number of shopping tours in pattern
        // firstGame: first tour is a gaming tour (1=yes, 0=no)
        // firstRec: first tour is a recreation tour (1=yes, 0=no)
        // lastRec: last tour is a recreation tour (1=yes, 0=no)
        // recBshop: existence of a recration tour before a shopping tour (1=yes, 0=no)
        String[] headings = {"a",
                             "tours",
                             "obStops",
                             "ibStops",
                             "gaming",
                             "rec",
                             "shop",
                             "firstGame",
                             "firstRec",
                             "lastRec",
                             "recBshop"};

        //create empty array for generating data set
        float[][] patternData = new float[patternAlts.size()][headings.length];
        //loop over pattern data and fill in array
        int counter = 0;
        String gameChar = String.valueOf(VisitorTourType.Gaming.getIDChar());
        String recChar = String.valueOf(VisitorTourType.Recreation.getIDChar());
        String shopChar = String.valueOf(VisitorTourType.Shopping.getIDChar());
        for (String s : patternAlts) {
            String[] patternInfo = s.split(",");
            String pattern = patternInfo[1].trim();
            patternData[counter][0] = Float.valueOf(patternInfo[0]);
            //Special for at home pattern
            if (pattern.equals("H")) {
                patternData[counter][1] = 0;
                patternData[counter][2] = 0;
                patternData[counter][3] = 0;
                patternData[counter][4] = 0;
                patternData[counter][5] = 0;
                patternData[counter][6] = 0;
                patternData[counter][7] = 0;
                patternData[counter][8] = 0;
                patternData[counter][9] = 0;
                patternData[counter][10] = 0;
                counter++;
                continue;
            }
            patternData[counter][1] = pattern.split(VisitorDataStructure.homeChar).length - 1;
            patternData[counter][2] = pattern.split(VisitorDataStructure.homeChar + VisitorDataStructure.stopChar).length - 1;
            patternData[counter][3] = (pattern + " ").split(VisitorDataStructure.stopChar + VisitorDataStructure.homeChar).length - 1;
            patternData[counter][4] = pattern.split(gameChar).length - 1;
            patternData[counter][5] = pattern.split(recChar).length - 1;
            patternData[counter][6] = pattern.split(shopChar).length - 1;
            patternData[counter][7] = (pattern.split(VisitorDataStructure.homeChar)[1] + " ").split(gameChar).length - 1;
            patternData[counter][8] = (pattern.split(VisitorDataStructure.homeChar)[1] + " ").split(recChar).length - 1;
            patternData[counter][9] = (pattern.split(VisitorDataStructure.homeChar)[(int) patternData[counter][1]] + " ").split(recChar).length - 1;
            int rbs = 0;
            int recIndex = 0;
            for (int i = 0; i < patternData[counter][5]; i++) {
                if (pattern.indexOf(gameChar,pattern.indexOf(recChar,recIndex)) > -1) {
                    rbs = 1;
                    break;
                }
                recIndex = pattern.indexOf(recChar,recIndex) + 1;
            }
            patternData[counter][10] = rbs;
            counter++;
        }
        //Write out results to alternatives table
        DataWriter dw = DataWriter.getInstance();
        dw.writeOutputFile(patternAlternativesDataKey, TableDataSet.create(patternData,headings));
    }


    /**
     * This mapping goes from a pattern id to a byte stream which can be deserialized to recreate a
     * {@code VisitorTour[]} array instance that is represented by that pattern.
     */
    private HashMap<Integer,byte[]> patternToTourMap;

    /**
     * Gets the pattern alternatives an {@code ArrayList} (via {@code TextFile}) of comma-delimited alternatives: (id,pattern).
     *
     * @return the alternatives.
     */
    private TextFile getPatternAlts() {
        //This basically creates an ArrayList<String> of text lines
        return new TextFile(rb.getString(patternAlternativesKey));
    }

    // This links a pattern # to a pattern string (for printing out summaries)
    private static HashMap<Integer,String> patternIDToString;

    /**
     * This method builds the {@link #patternToTourMap}.
     */
    private void buildPatternToTourMap() {
        patternToTourMap = new HashMap<Integer,byte[]>();
        patternIDToString = new HashMap<Integer,String>();

        TextFile patternAlts = getPatternAlts();

        //initialize maxTours and pattern count
        maxTours = 0;

        for (String line : patternAlts) {
            String[] altData = line.split(",");
            //create tours array and update maxTours if necessary
            VisitorTour[] patternTours = patternDecode(altData[1]);
            if (patternTours.length > maxTours)
                maxTours = patternTours.length;
            try {
                //write object to byte array
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                ObjectOutputStream out = new ObjectOutputStream(bos);
                out.writeObject(patternTours);
                out.flush();
                out.close();
                //create a byte array and put it into the hashmap
                patternToTourMap.put(Integer.valueOf(altData[0]),bos.toByteArray());
                patternIDToString.put(Integer.valueOf(altData[0]),altData[1]);
            } catch (IOException e) {
                e.printStackTrace();
            }
            //ensure that the patterns match the tours
            if (debug) {
                logger.info("Pattern: " + altData[1]);
                for (VisitorTour tour : patternTours) logger.info(tour);
            }
        }
    }

    /**
     * This method returns a new {@code VisitorTour[]} array from the byte array associated witht that
     * pattern's id.
     *
     * @param id
     *        The unique id of that pattern.
     *
     * @return the tour array associated with that pattern.
     */
    public VisitorTour[] getToursFromPatternID(int id) {
        //build patternToTourMap if it has not been done yet
        if (patternToTourMap == null) buildPatternToTourMap();
        VisitorTour[] tours = null;
        try {
            ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(patternToTourMap.get(id)));
            tours = (VisitorTour[]) in.readObject();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException cnfe) {
            cnfe.printStackTrace();
        }
        return tours;
    }

    public String getPatternFromID(int id) {
        //build patternToTourMap if it has not been done yet
        if (patternIDToString == null) buildPatternToTourMap();
        return patternIDToString.get(id);
    }

    /**
     * This method "decodes" a pattern and returns a tour array with the all the information that can be deduced
     * from the pattern string.
     *
     * @param pattern
     *        The string representation of the pattern.
     *
     * @return a tour array representing the pattern
     */
    public VisitorTour[] patternDecode(String pattern) {

        //If at home tour, then empty tour array
        if (pattern.equals("H")) return new VisitorTour[0];

        //Split pattern by home character - the first and last home must be lopped off to avoid making null beginning and ending tours
        String[] tourSet = pattern.substring(1,pattern.length() - 1).split(VisitorDataStructure.homeChar);

        //initialize tour array
        VisitorTour[] tours = new VisitorTour[tourSet.length];
        //fill tour array
        int counter = 0;
        for (String tour : tourSet) {
            tours[counter] = new VisitorTour();
            tours[counter].setTourNum(counter + 1);
            tours[counter].setOutboundStop(tour.substring(0, 1).equals(VisitorDataStructure.stopChar));
            tours[counter].setInboundStop(tour.substring(tour.length() - 1).equals(VisitorDataStructure.stopChar));
            //Strip stops from tour
            tour = tour.replaceAll(VisitorDataStructure.stopChar,"");
            if (tour.length() != 1) {
                logger.warn("Tour type " + tour + " should be a single character!");
            } else {
                char primact = tour.charAt(0);
                if (VisitorTourType.isIDCharValid(primact)) {
                    tours[counter].setTourType(VisitorTourType.getTourType(primact));
                } else {
                    logger.warn("Tour type char id " + primact + " not recognized!");
                }
            }
            counter++;
        }
        return tours;
    }
}
