package com.pb.tahoe.util;

import com.pb.common.datafile.CSVFileReader;
import com.pb.common.datafile.CSVFileWriter;
import com.pb.common.datafile.TableDataSet;
import com.pb.common.datafile.TableDataSetLoader;
import com.pb.common.matrix.AlphaToBeta;
import com.pb.common.util.ResourceUtil;
import com.pb.tahoe.structures.TourType;
import com.pb.tahoe.synpop.ZonalData;
import com.pb.tahoe.visitor.structures.VisitorTourType;
import com.pb.tahoe.visitor.structures.StayType;
import org.apache.log4j.Logger;

import java.io.*;
import java.util.*;

/**
 * @author Jim Hicks/Christi Willison
 *
 * Class for managing zonal data.
 * e.g. size variables, zonal TableDataSet, attractions model, stop density model.
 */
public class ZonalDataManager implements java.io.Serializable {
    protected static Logger logger = Logger.getLogger(ZonalDataManager.class);
    private ResourceBundle propertyMap;

    public static ZonalDataManager instance = new ZonalDataManager();

    //field names we use from the Tahoe ZoneMappings.csv file
    public static final String TAZ = "taz";
    public static final String SQ_AREA_IN_MILES = "area_in_sq_miles";

    //field names we use from the Tahoe Accessibility.csv file
    public static final String ALL_EMP_WALK_20 = "work_walk_20";
    public static final String RETAIL_EMP_WALK_20 = "retail_walk_20";
    public static final String ALL_EMP_AUTO_30 = "work_a_30" ;
    public static final String ALL_EMP_TRANSIT_30 = "work_tr_30";
    public static final String RETAIL_EMP_AUTO_30 = "retail_a_30" ;
    public static final String RETAIL_EMP_TRANSIT_30 = "retail_tr_30";

    //field names we use from the LandType.csv file
    public static final String URB_TYPE = "urbtype";

    //field names we use from the SchoolEnrollment.csv file
    public static final String ES_ENROLL = "elementary_school_enrollment";
    public static final String MS_ENROLL = "middle_school_enrollment";
    public static final String HS_ENROLL = "high_school_enrollment";
    public static final String CL_ENROLL = "college_enrollment";

    //field names to use from the ExternalZoneSizeCoefficients.csv file
    public static String EXTERNAL_SIZE_COEFFICIENT_INTWORKER_FIELD;
    public static String EXTERNAL_SIZE_COEFFICIENT_EXTWORKER_FIELD;
    public static final String EXTERNAL_SIZE_COEFFICIENT_INTWORKER_FIELD_SUMMER = "intWorkSizeCoeffSummer";
    public static final String EXTERNAL_SIZE_COEFFICIENT_EXTWORKER_FIELD_SUMMER = "extWorkSizePercentSummer";
    public static final String EXTERNAL_SIZE_COEFFICIENT_INTWORKER_FIELD_WINTER = "intWorkSizeCoeffWinter";
    public static final String EXTERNAL_SIZE_COEFFICIENT_EXTWORKER_FIELD_WINTER = "extWorkSizePercentWinter";

    public static HashMap<Integer,String> overnightVisitorSizeTermFields;
    public static final String OV_DC_SIZE_PERCENT_REC_FIELD = "ovDCSizePercentRec";
    public static final String OV_DC_SIZE_PERCENT_GAMING_FIELD = "ovDCSizePercentGam";
    public static final String OV_DC_SIZE_PERCENT_SHOP_FIELD = "ovDCSizePercentShp";
    public static final String OV_DC_SIZE_PERCENT_OTHER_FIELD = "ovDCSizePercentOth";
    public static final String OV_DC_STOP_SIZE_PERCENT_FIELD = "ovStpDCSizePercent";


    public static final int WALK_SEGMENTS = 3;

    public static int numberOfZones;
    public static int numberOfExternalZones;  // will be set to 8.  zones 1,2,3,4,5,6,7,8 are external zones
    public static int numberOfInternalZones;         //numberOfZones - external zones
    public static int firstInternalZoneNumber;
    int numberOfSubzones;
    int maxTazNumber;

    private boolean debug = false;
    private TableDataSet zoneDataCheck = new TableDataSet();
    private TableDataSet subzoneDataCheck = new TableDataSet();
    private TableDataSet externalCoefficients = new TableDataSet();

    private TableDataSet zoneTable;
    private TableDataSet subzonesTable; //this is the file that holds the dc alternatives

    public static float[][] walkPctArray;
    public static float[][] totSize;
    public static float[] urbType;
    public static float[] cnty;
    public static float[] zonalShortAccess;
    public static double[] odUtilModeAlt;
    public static float[] logsumDcAMPM;
    public static float[] logsumDcAMMD;
    public static float[] logsumDcMDMD;
    public static float[] logsumDcPMNT;
    public static float[] logsumDcNTNT;
   //added by crf to have a mapping from alternative to zone
    public static int[] zoneAlt;
    public static float[][] schoolEnrollment;
    //Index description: [OB-IB][TourTypes][altNumber (nRows in dcAlternatives.csv file)]
    public static float[][][] stopSize;
    //Index description: [OB-IB][altNumber]
    public static float[][] stopTotSize;
    private float[][] attractions;
    //added by crf to add shadow price functionality to work destination choice model
    //  will do it by zone and not subzone to make things simpler and avoid non-integer workers or rounding effects
    //  (from multiplying zonal employment by subzone fractions)
    public static float totalEmployment = 0;
    public static float[] zonalEmployment;
    public static float[] zonalWorkTrips;
    public static float[] shadowPrice;
    //added by crf to add shadow price functionality to external worker origin choice model
    public static float totalExternalWorkers = 0;
    public float[] externalWorkerSize;
    public float[] zonalExternalWorkerCount;
    public float[] zonalExternalWorkTours;
    public float[] externalWorkerShadowPrice;

    //added by crf because its needed by visitor model
    public static boolean[] southShore;
    public static float[][] totVisitorAttractions;
    public static float[][] totVisitorSize;
    public static float[] totVisitorStopAttractions;
    public static float[] totVisitorStopSize;



    private ZonalDataManager() {
        propertyMap = ResourceUtil.getResourceBundle("tahoe");

        //Create socioeconomic file with labor force
        LaborForceCalculator.generateLaborForce();

        //Create the dcAlternatives file
        DCAlternativeListCreator.createDCAlternativeSet(propertyMap);

        CSVFileReader reader = new CSVFileReader();

        //Determine number of zones and also map the alpha zones to the counties
        String zonalMappingsFile = propertyMap.getString("taz.correspondence.file");
        AlphaToBeta zonalMap = new AlphaToBeta(new File(zonalMappingsFile), "taz", "county_code");
        numberOfZones = zonalMap.getNumAlphaZones();
        numberOfExternalZones = 70;                //zones numbered 1-7.
        numberOfInternalZones = numberOfZones - numberOfExternalZones;
        firstInternalZoneNumber = 109;

        maxTazNumber = zonalMap.getMaxAlphaZone();

        //Set external size terms data set
        externalCoefficients = TableDataSetLoader.loadTableDataSet(propertyMap, "external.size.terms.coefficients.file" );

        //Determine the number of subzones in the model
        String dcAlternativesFile = propertyMap.getString("dc.alternative.set.file");
        try {
            subzonesTable = reader.readFile(new File(dcAlternativesFile));
            numberOfSubzones = subzonesTable.getRowCount();
        } catch (IOException e) {
            throw new RuntimeException("Error reading file" , e);
        }

        //Create the urbtype file
        logger.info("\tCreating the UrbanType file");
        UrbanTypeCalculator.createUrbTypeTable();

        //Create the AccessibilityToEmployment file
        logger.info("\tCalculating the accessibility to employment and writing to file");
        AccessibilityToEmploymentCalculator.calculateAccessibilitiesForTahoe();

        //Create a ZonalData table that combines all various zonal files into a single file
        logger.info("\tCombining Zonal files into MergedZonalData file");
        zoneTable = combineZonalData();
        zoneTable.buildIndex(1);

        logger.info("\tFilling urbtype, walkpct and zonalShortAccess arrays with zonal data");
        getZoneRelatedData();

        //do the zonal employment/worker initialization
        zonalEmployment = new float[maxTazNumber + 1];
        zonalWorkTrips = new float[maxTazNumber + 1];
        clearZonalWorkTrips();
        //new  File(propertyMap.getString("shadow.price.start.values"))
        if ((new  File(propertyMap.getString("shadow.price.start.values"))).exists()) {
            try {
                ObjectInputStream in = new ObjectInputStream(new FileInputStream(new  File(propertyMap.getString("shadow.price.start.values"))));
                shadowPrice = (float[]) in.readObject();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (ClassNotFoundException cnfe) {
                cnfe.printStackTrace();
            }
        } else {
            shadowPrice = new float[maxTazNumber + 1];
            Arrays.fill(shadowPrice, 1.0f);
        }
        externalWorkerShadowPrice = new float[firstInternalZoneNumber];
        Arrays.fill(externalWorkerShadowPrice, 1.0f);
        zonalExternalWorkTours = new float[firstInternalZoneNumber];

        //Do southShore variable
        southShore = new boolean[maxTazNumber + 1];
        Arrays.fill(southShore,false);
        HashSet<Integer> southShoreCounties = new HashSet<Integer>();
        southShoreCounties.add(5);
        southShoreCounties.add(17);
        for (int taz : zoneTable.getColumnAsInt(TAZ)) {
            if (southShoreCounties.contains(zonalMap.getAlphaToBeta()[taz])) {
                southShore[taz] = true;
            }
        }

        //define external size term coeffients
        if (ResourceUtil.getBooleanProperty(propertyMap,"summer")) {
            EXTERNAL_SIZE_COEFFICIENT_INTWORKER_FIELD = EXTERNAL_SIZE_COEFFICIENT_INTWORKER_FIELD_SUMMER;
            EXTERNAL_SIZE_COEFFICIENT_EXTWORKER_FIELD = EXTERNAL_SIZE_COEFFICIENT_EXTWORKER_FIELD_SUMMER;
        } else {
            EXTERNAL_SIZE_COEFFICIENT_INTWORKER_FIELD = EXTERNAL_SIZE_COEFFICIENT_INTWORKER_FIELD_WINTER;
            EXTERNAL_SIZE_COEFFICIENT_EXTWORKER_FIELD = EXTERNAL_SIZE_COEFFICIENT_EXTWORKER_FIELD_WINTER;
        }

        overnightVisitorSizeTermFields = new HashMap<Integer,String>();
        overnightVisitorSizeTermFields.put(VisitorTourType.Recreation.getID(),OV_DC_SIZE_PERCENT_REC_FIELD);
        overnightVisitorSizeTermFields.put(VisitorTourType.Gaming.getID(),OV_DC_SIZE_PERCENT_GAMING_FIELD);
        overnightVisitorSizeTermFields.put(VisitorTourType.Shopping.getID(),OV_DC_SIZE_PERCENT_SHOP_FIELD);
        overnightVisitorSizeTermFields.put(VisitorTourType.Other.getID(),OV_DC_SIZE_PERCENT_OTHER_FIELD);

        // calculate attractions for use in size variable calculations
        logger.info("\tCalculating Attractions for internal and external zones");
        calculateAttractions();

        logger.info("\tCalculating Size Terms for internal and external zones");
        calculateSizeTerms();

       stopSize = new float[2][TourType.TYPES+1][];
       stopTotSize = new float[2][];
        calculateStopDensity();

        logsumDcAMPM = new float[numberOfSubzones + 1];
        logsumDcAMMD = new float[numberOfSubzones + 1];
        logsumDcMDMD = new float[numberOfSubzones + 1];
        logsumDcPMNT = new float[numberOfSubzones + 1];
        logsumDcNTNT = new float[numberOfSubzones + 1];

        if (debug) {
            CSVFileWriter writer = new CSVFileWriter();
            try {
                writer.writeFile(zoneDataCheck, new File(propertyMap.getString("zone.data.debug.file")));
                writer.writeFile(subzoneDataCheck, new File(propertyMap.getString("subzone.data.debug.file")));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static ZonalDataManager getInstance(){
        return instance;
    }

    private TableDataSet combineZonalData() {

        String[][] columnsToCombine = new String[4][];
        String zonalFile1 = propertyMap.getString("socio.economic.data.file");
        columnsToCombine[0] = ResourceUtil.getArray(propertyMap,"socio_columns_used");
        String zonalFile2 = propertyMap.getString("accessibility.to.employment.file");
        columnsToCombine[1] = ResourceUtil.getArray(propertyMap,"access_columns_used");
        String zonalFile3 = propertyMap.getString("urban.type.file");
        columnsToCombine[2] = ResourceUtil.getArray(propertyMap,"urban_columns_used");
        String zonalFile4 = propertyMap.getString("school.enrollment.file");
        columnsToCombine[3] = ResourceUtil.getArray(propertyMap,"school_columns_used");
        String zonalCombined = propertyMap.getString("merged.taz.data.file");

        TableDataSet comboFile = new TableDataSet();
        CSVFileReader reader = new CSVFileReader();
        int[] zones;
        try {
            String zonalMappingsFile = propertyMap.getString("taz.correspondence.file");
            TableDataSet tazMap = reader.readFile(new File(zonalMappingsFile));
            zones = tazMap.getColumnAsInt(ZonalDataManager.TAZ);

            comboFile.appendColumn(zones, ZonalDataManager.TAZ);    //first column is the list of all zones

            List<TableDataSet> tables = new ArrayList<TableDataSet>();
            tables.add(reader.readFile(new File(zonalFile1)));
            tables.add(reader.readFile(new File(zonalFile2)));
            tables.add(reader.readFile(new File(zonalFile3)));
            tables.add(reader.readFile(new File(zonalFile4)));

            //For each table, take what we need
            for(int i=0; i < tables.size(); i++){
                TableDataSet table = (TableDataSet) tables.get(i);
                 table.buildIndex(1);          // in case the tables aren't sorted the same

                String[] columns = columnsToCombine[i];
                float[][] values;
                String[] headers;
                if(columns[0].equalsIgnoreCase("all")){
                    values = new float[table.getColumnCount() - 1][zones.length];  //don't include the first column (taz number)
                    headers = new String[table.getColumnCount() - 1];
                    int index = 0;
                    for(int zone : zones){
                        try {
                            float[] rowValues = table.getIndexedRowValuesAt(zone);
                            for (int j = 1; j <rowValues.length; j++) {
                                values[j-1][index] = rowValues[j];
                            }
                        } catch (Exception e) {
                            for(int c = 0; c < values.length; c++){
                                values[c][index] = -999;
                            }
                        }
                        index++;
                    }

                    for(int c = 2; c <= table.getColumnCount(); c++){
                        headers[c-2] = table.getColumnLabel(c);
                    }

                } else{
                    values = new float[columns.length][zones.length];
                    headers = columns;
                    int index = 0;
                    for(int zone : zones){
                        try {
                            for(int cs = 0; cs < columns.length; cs++){
                                values[cs][index] = table.getIndexedValueAt(zone, columns[cs]);
                            }
                        } catch (Exception e) {
                            for(int c = 0; c < values.length; c++){
                                values[c][index] = -999;
                            }
                        }
                        index++;
                    }
                }

                for (int dataColumn = 0; dataColumn< values.length; dataColumn++){
                    String colName = headers[dataColumn];
                    if(!isColExist(comboFile, colName)){
                        comboFile.appendColumn(values[dataColumn], colName);
                    }
                }
            }

            boolean schoolDay = ResourceUtil.getBooleanProperty(propertyMap, "school.day");
            int[] schoolFlag = new int[comboFile.getRowCount()];  //initialized to 0s implies that it is NOT a school day
            if(schoolDay) Arrays.fill(schoolFlag, 1);
            comboFile.appendColumn(schoolFlag, "school_day");

            CSVFileWriter writer = new CSVFileWriter();
            writer.writeFile(comboFile, new File(zonalCombined));

        } catch (IOException e) {
            throw new RuntimeException("Error reading or writing tables" , e);
        }

        return comboFile;
    }



    private void getZoneRelatedData() {
        int k;

        //we will use this list of tazs to index into the zone file.  Some zones have
        //3 subzones but others only have 1 or 2 so the code has to change from the MORPC
        //code.
        int[] subTazs = subzonesTable.getColumnAsInt("dtaz");

        k = 1;

        float uType;
        urbType = new float[numberOfSubzones+1];
        zoneAlt = new int[numberOfSubzones+1];
        //Taz numbers are repeated for the number of subzones that are in that zone
        for (int subTaz: subTazs) {
            uType = zoneTable.getIndexedValueAt(subTaz, zoneTable.getColumnPosition(ZonalDataManager.URB_TYPE));
            urbType[k] = uType;
            zoneAlt[k] = subTaz;
            k++;
        }

        if(debug) {
            int[] subZoneAltColumn = new int[numberOfSubzones + 1];
            int[] tazColumn = new int[numberOfSubzones + 1];
            for(int z=0; z< numberOfSubzones; z++){
                subZoneAltColumn[z+1] = subzonesTable.getColumnAsInt(1)[z];
                tazColumn[z+1] = subzonesTable.getColumnAsInt(2)[z];
            }
            subzoneDataCheck.appendColumn(subZoneAltColumn, "subZoneAlt");
            subzoneDataCheck.appendColumn(tazColumn, "taz");
            subzoneDataCheck.appendColumn(urbType, "Urbtype");
        }

        // get file names from properties file
        //String walkAccessFile = propertyMap.getString("zonal.walk.percents.file");
        String walkAccessFile = propertyMap.getString("zonal.walk.percents.file");
        int taz;
        float shrtPct;
        float longPct;
        walkPctArray = new float[3][maxTazNumber+1];
        //since the external zones are not in the zonal.walk.percents.file
        //this initialization is important.  The external zones will be
        //100% no-walk (walkPctArray[0] = 1.0
        Arrays.fill(walkPctArray[0], 1.0f);
        Arrays.fill(walkPctArray[1], 0.0f);
        Arrays.fill(walkPctArray[2], 0.0f);


        TableDataSet wa = null;
        try {
            CSVFileReader reader = new CSVFileReader();
            wa = reader.readFile(new File(walkAccessFile));
        } catch (IOException e) {
            throw new RuntimeException("Trouble reading file " + walkAccessFile);
        }

        int tazPosition = wa.getColumnPosition("TAZ");
        int shrtPosition = wa.getColumnPosition("ShortWalkPercent");
        int longPosition = wa.getColumnPosition("LongWalkPercent");

        //set internal zone walk percents.
        for (int j = 1; j <= wa.getRowCount(); j++) {
            taz = (int) wa.getValueAt(j, tazPosition);
            shrtPct = wa.getValueAt(j, shrtPosition);
            longPct = wa.getValueAt(j, longPosition);
            walkPctArray[1][taz] = shrtPct;
            walkPctArray[2][taz] = longPct;
            walkPctArray[0][taz] = (float) (1.0 -(shrtPct + longPct));      //no walk to transit
        }



        // set 0/1 values for zone doesn't/does have short walk access for all dc alternatives
        k = 1;
        zonalShortAccess = new float[numberOfSubzones + 1];

        for (int subTaz2: subTazs) {
            if ((walkPctArray[1][subTaz2] > 0.0)) zonalShortAccess[k] = 1;
            else zonalShortAccess[k] = 0;
            k++;
         }

        if(debug){
            int[] tazColumn = new int[maxTazNumber + 1];
            int[] tazs = zoneTable.getColumnAsInt(1);
            for(int t : tazs)  tazColumn[t] = t;
            zoneDataCheck.appendColumn(tazColumn, "taz");
            zoneDataCheck.appendColumn(walkPctArray[0], "pct_no_walk");
            zoneDataCheck.appendColumn(walkPctArray[1], "pct_short_walk");
            zoneDataCheck.appendColumn(walkPctArray[2], "pct_long_walk");

            subzoneDataCheck.appendColumn(zonalShortAccess, "shortAccess?");
        }


    }

    /**
     * This method reads in several files.  The "include.in.size.term.file is a table of 1's and 0's
     * specificing which terms will be used to calculate the size terms for the internal zones.
     * The choices are nHhs, total employment, retail employment, service employment,
     * gaming employment, recreation employment, other employment, school enrollment,
     * and zones that have more than 50 hhs.
     * For the external zones, there is a file called "ExternalSizeTerms.csv" that lists the sizes
     * for each external zone.
     * Because it is convenient, this method is also used to fill up the zonalEmployment array
     */
    private void calculateAttractions() {
        int purp;

        // load the 2 files we need
        TableDataSet includes = TableDataSetLoader.loadTableDataSet(propertyMap, "include.in.size.term.file" );
        externalCoefficients.buildIndex(1);
//        TableDataSet externals;
//        if (ResourceUtil.getBooleanProperty(propertyMap,"summer",true)) {
//            externals = TableDataSetLoader.loadTableDataSet(propertyMap, "external.size.terms.summer" );
//        } else {
//            externals = TableDataSetLoader.loadTableDataSet(propertyMap, "external.size.terms.winter" );
//        }
//        externals.buildIndex(1);

        TableDataSet visitorIncludes = TableDataSetLoader.loadTableDataSet(propertyMap, "include.in.size.term.visitor.file");
        TableDataSet visitorData = TableDataSetLoader.loadTableDataSet(propertyMap, "overnight.visitors.zonal.data.file" );
        visitorData.buildIndex(1);

        int purpFieldPosition = includes.getColumnPosition("purpose");
        int totpopFieldPosition = includes.getColumnPosition(ZonalData.HHS_FIELD);
        int totempFieldPosition = includes.getColumnPosition("emp_total");
        int empretFieldPosition = includes.getColumnPosition(ZonalData.RETAIL_EMP);
        int empsrvcFieldPosition = includes.getColumnPosition(ZonalData.SERVICE_EMP);
        int empgameFieldPosition = includes.getColumnPosition(ZonalData.GAMING_EMP);
        int emprecFieldPosition = includes.getColumnPosition(ZonalData.RECREATION_EMP);
        int empotherFieldPosition = includes.getColumnPosition(ZonalData.OTHER_EMP);
        int schenrFieldPosition = includes.getColumnPosition("school_enrollment");
        int hhMoreThan50FieldPosition = includes.getColumnPosition("moreThan50HHs");

        

        double[][] coeff = new double[TourType.TYPES + 1][includes.getColumnCount()-2];

        for (int i = 1; i <= includes.getRowCount(); i++) {
            purp = (int) includes.getValueAt(i, purpFieldPosition);

            coeff[purp][0] = includes.getValueAt(i,
                    totpopFieldPosition);
            coeff[purp][1] = includes.getValueAt(i,
                    totempFieldPosition);
            coeff[purp][2] = includes.getValueAt(i,
                    empretFieldPosition);
            coeff[purp][3] = includes.getValueAt(i,
                    empsrvcFieldPosition);
            coeff[purp][4] = includes.getValueAt(i,
                    empgameFieldPosition);
            coeff[purp][5] = includes.getValueAt(i,
                    emprecFieldPosition);
            coeff[purp][6] = includes.getValueAt(i,
                    empotherFieldPosition);
            coeff[purp][7] = includes.getValueAt(i,
                    schenrFieldPosition);
            coeff[purp][8] = includes.getValueAt(i,
                    hhMoreThan50FieldPosition);
        }


        int totempVisitorFieldPosition = visitorIncludes.getColumnPosition("emp_total");
        int empretVisitorFieldPosition = visitorIncludes.getColumnPosition(ZonalData.RETAIL_EMP);
        int empsrvcVisitorFieldPosition = visitorIncludes.getColumnPosition(ZonalData.SERVICE_EMP);
        int empgameVisitorFieldPosition = visitorIncludes.getColumnPosition(ZonalData.GAMING_EMP);
        int emprecVisitorFieldPosition = visitorIncludes.getColumnPosition(ZonalData.RECREATION_EMP);
        int empotherVisitorFieldPosition = visitorIncludes.getColumnPosition(ZonalData.OTHER_EMP);
        int campVisitorFieldPosition = visitorIncludes.getColumnPosition(StayType.CAMPGROUND.toString().toLowerCase());
        int beachVisitorFieldPosition = visitorIncludes.getColumnPosition(ZonalData.BEACH_FIELD);

        String seasonIdentifier = "w";
        if (ResourceUtil.getBooleanProperty(propertyMap,"summer"))
            seasonIdentifier = "s";

        //element 0 is the stops coefficients
        double[][] visitorCoeff = new double[VisitorTourType.values().length + 1][visitorIncludes.getColumnCount() - 2];
        for (int i = 1; i <= visitorIncludes.getRowCount(); i++) {
            String vpurp = visitorIncludes.getStringValueAt(i,purpFieldPosition);
            //skip if wrong season
            if (!vpurp.substring(vpurp.length() - 1).equals(seasonIdentifier))
                continue;
            vpurp = vpurp.substring(0,vpurp.length() - 1);
            int ind = 0;
            if (!vpurp.equals("stop")) {
                ind = Integer.valueOf(vpurp);
            }
            visitorCoeff[ind][0] = visitorIncludes.getValueAt(i,totempVisitorFieldPosition);
            visitorCoeff[ind][1] = visitorIncludes.getValueAt(i,empretVisitorFieldPosition);
            visitorCoeff[ind][2] = visitorIncludes.getValueAt(i,empsrvcVisitorFieldPosition);
            visitorCoeff[ind][3] = visitorIncludes.getValueAt(i,empgameVisitorFieldPosition);
            visitorCoeff[ind][4] = visitorIncludes.getValueAt(i,emprecVisitorFieldPosition);
            visitorCoeff[ind][5] = visitorIncludes.getValueAt(i,empotherVisitorFieldPosition);
            visitorCoeff[ind][6] = visitorIncludes.getValueAt(i,campVisitorFieldPosition);
            visitorCoeff[ind][7] = visitorIncludes.getValueAt(i,beachVisitorFieldPosition);
        }

        // read the zoneTable TableDataSet to get zonal fields for the attraction models
        totpopFieldPosition = zoneTable.getColumnPosition(ZonalData.HHS_FIELD);
        empretFieldPosition = zoneTable.getColumnPosition(ZonalData.RETAIL_EMP);
        empsrvcFieldPosition = zoneTable.getColumnPosition(ZonalData.SERVICE_EMP);
        empgameFieldPosition = zoneTable.getColumnPosition(ZonalData.GAMING_EMP);
        emprecFieldPosition = zoneTable.getColumnPosition(ZonalData.RECREATION_EMP);
        empotherFieldPosition = zoneTable.getColumnPosition(ZonalData.OTHER_EMP);
        int elenrFieldPosition = zoneTable.getColumnPosition(ZonalDataManager.ES_ENROLL);
        int msenrFieldPosition = zoneTable.getColumnPosition(ZonalDataManager.MS_ENROLL);
        int hsenrFieldPosition = zoneTable.getColumnPosition(ZonalDataManager.HS_ENROLL);
        int colenrFieldPosition = zoneTable.getColumnPosition(ZonalDataManager.CL_ENROLL);

        campVisitorFieldPosition = visitorData.getColumnPosition(StayType.CAMPGROUND.toString().toLowerCase());
        beachVisitorFieldPosition = visitorData.getColumnPosition(ZonalData.BEACH_FIELD);
        



        float[] field = new float[includes.getColumnCount()-2];
        float[] visitorField = new float[visitorIncludes.getColumnCount() - 2];

        this.attractions = new float[TourType.TYPES + 1][maxTazNumber +1];
        totVisitorAttractions = new float[VisitorTourType.values().length + 1][maxTazNumber + 1];
        totVisitorStopAttractions = new float[maxTazNumber + 1];

        //While we're here, fill up the school enrollement array
        schoolEnrollment = new float[4][maxTazNumber +1];

        //unfortunately we have to loop over taz's twice, because I need the total basin employment in the next loop
        for (int taz : zoneTable.getColumnAsInt(TAZ)) {
            if (taz >= firstInternalZoneNumber) {
                totalEmployment += zoneTable.getIndexedValueAt(taz, empretFieldPosition) +
                        zoneTable.getIndexedValueAt(taz, empsrvcFieldPosition) +
                        zoneTable.getIndexedValueAt(taz, empgameFieldPosition) +
                        zoneTable.getIndexedValueAt(taz, emprecFieldPosition) +
                        zoneTable.getIndexedValueAt(taz, empotherFieldPosition);
            }
        }

        for (int taz : zoneTable.getColumnAsInt(TAZ)) {
            schoolEnrollment[0][taz] = zoneTable.getIndexedValueAt(taz, elenrFieldPosition);
            schoolEnrollment[1][taz] = zoneTable.getIndexedValueAt(taz, msenrFieldPosition);
            schoolEnrollment[2][taz] = zoneTable.getIndexedValueAt(taz, hsenrFieldPosition);
            schoolEnrollment[3][taz] = zoneTable.getIndexedValueAt(taz, colenrFieldPosition);
            if (taz >= firstInternalZoneNumber) {
                field[0] = zoneTable.getIndexedValueAt(taz, totpopFieldPosition);
                field[2] = zoneTable.getIndexedValueAt(taz, empretFieldPosition);
                field[3] = zoneTable.getIndexedValueAt(taz, empsrvcFieldPosition);
                field[4] = zoneTable.getIndexedValueAt(taz, empgameFieldPosition);
                field[5] = zoneTable.getIndexedValueAt(taz, emprecFieldPosition);
                field[6] = zoneTable.getIndexedValueAt(taz, empotherFieldPosition);

                field[1] = field[2] + field[3] + field[4] +  field[5] + field[6];

                field[7] = zoneTable.getIndexedValueAt(taz, elenrFieldPosition) + zoneTable.getIndexedValueAt(taz, msenrFieldPosition) +
                    zoneTable.getIndexedValueAt(taz, hsenrFieldPosition) + zoneTable.getIndexedValueAt(taz, colenrFieldPosition);

                field[8] = (field[0] >= 50) ? field[0] : 0.0f;

                //field 1 is total employment
                zonalEmployment[taz] = field[1];

                for (int p = 1; p <= TourType.TYPES; p++) {
                    for (int j = 0; j < field.length; j++)
                        attractions[p][taz] += (field[j] * coeff[p][j]);
                }

                //visitor stuff
                for (int i = 0; i < 6; i++) {
                    visitorField[i] = field[i + 1];
                }
                visitorField[6] = visitorData.getIndexedValueAt(taz,campVisitorFieldPosition);
                visitorField[7] = visitorData.getIndexedValueAt(taz,beachVisitorFieldPosition);

                for (int i = 1; i <= VisitorTourType.values().length; i++) {
                    for (int j= 0; j < visitorField.length; j++) {
                        totVisitorAttractions[i][taz] += visitorField[j] * visitorCoeff[i][j];
                        if (i == 1) {
                            totVisitorStopAttractions[taz] += visitorField[j] * visitorCoeff[0][j];
                        }
                    }
                }

            } else {
                for (int p = 1; p <= TourType.TYPES; p++) {
                    attractions[p][taz] = totalEmployment*externalCoefficients.getIndexedValueAt(taz, EXTERNAL_SIZE_COEFFICIENT_INTWORKER_FIELD);
                    //Add external zone "employment"
                    if (p == TourType.WORK)
                        zonalEmployment[taz] = attractions[p][taz];
                }
                for (int i = 1; i <= VisitorTourType.values().length; i++) {
                    totVisitorAttractions[i][taz] = totalEmployment*
                            externalCoefficients.getIndexedValueAt(taz, EXTERNAL_SIZE_COEFFICIENT_INTWORKER_FIELD)*
                            externalCoefficients.getIndexedValueAt(taz, overnightVisitorSizeTermFields.get(i));
                }
                totVisitorStopAttractions[taz] = totalEmployment*
                        externalCoefficients.getIndexedValueAt(taz, EXTERNAL_SIZE_COEFFICIENT_INTWORKER_FIELD)*
                        externalCoefficients.getIndexedValueAt(taz, OV_DC_STOP_SIZE_PERCENT_FIELD);

//                OV_DC_SIZE_PERCENT_REC_FIELD = "ovDCSizePercentRec";
//    public static final String OV_DC_SIZE_PERCENT_GAMING_FIELD = "ovDCSizePercentGam";
//    public static final String OV_DC_SIZE_PERCENT_SHOP_FIELD = "ovDCSizePercentShp";
//    public static final String OV_DC_SIZE_PERCENT_OTHER_FIELD = "ovDCSizePercentOth";
//    public static final String OV_DC_STOP_SIZE_PERCENT_FIELD = "ovStpDCSizePercenst";


//                for (int p = 1; p <= TourType.TYPES; p++) {
//                    attractions[p][taz] = externals.getIndexedValueAt(taz, "attraction");
//                    //Add external zone "employment"
//                    if (p == TourType.WORK)
//                        zonalEmployment[taz] = attractions[p][taz];
//                }
//                for (int i = 1; i <= VisitorTourType.values().length; i++) {
//                    totVisitorAttractions[i][taz] = externals.getIndexedValueAt(taz, "attraction");
//                    totVisitorStopAttractions[taz] = externals.getIndexedValueAt(taz, "attraction");
//                }
            }

        }

        if(debug){
            for(int t=1; t<=TourType.TYPES; t++ ){

                zoneDataCheck.appendColumn(attractions[t], "attr_" + TourType.getTypeLabel((short) t));
            }
        }
    }


    /**
     * This method takes the attractions that were previously calculated and
     * proportions them to the subzones based on the walk percentages of
     * each subzone.
     */
    private void calculateSizeTerms() {

        totSize = new float[TourType.TYPES + 1][];

        for (int tourType = 1; tourType <= TourType.TYPES; tourType++) {
            totSize[tourType] = new float[numberOfSubzones + 1];

            float regionalSize = 0.0f;
            float externalSize = 0.0f;

            int dcAltNum = 1;
            int[] walkCategory = subzonesTable.getColumnAsInt("shortWalk");
            int[] tazList = subzonesTable.getColumnAsInt("dtaz");
            for (int taz: tazList) {
                totSize[tourType][dcAltNum] = attractions[tourType][taz] * walkPctArray[walkCategory[dcAltNum-1]][taz];
                if(taz >= firstInternalZoneNumber) regionalSize += totSize[tourType][dcAltNum];
                else externalSize += totSize[tourType][dcAltNum];
                dcAltNum++;
            }

            if(debug){
                subzoneDataCheck.appendColumn(totSize[tourType], "size_" + TourType.getTypeLabel((short) tourType));
            }

            if (debug) {
                logger.debug("total regional destination choice size for purpose " +
                    tourType + " = " + regionalSize);

                logger.debug("total external destination choice size for purpose " +
                    tourType + " = " + externalSize);
            }
        }

        totVisitorSize = new float[VisitorTourType.values().length + 1][numberOfSubzones + 1];
        totVisitorStopSize = new float[numberOfSubzones + 1];
        int[] walkCategory = subzonesTable.getColumnAsInt("shortWalk");
        int[] tazList = subzonesTable.getColumnAsInt("dtaz");
        for (int tourType = 1; tourType <= VisitorTourType.values().length; tourType++) {
            int dcAltNum = 1;
            for (int taz: tazList) {
                totVisitorSize[tourType][dcAltNum] = totVisitorAttractions[tourType][taz] * walkPctArray[walkCategory[dcAltNum-1]][taz];
                if (tourType ==1) {
                    totVisitorStopSize[dcAltNum] = totVisitorStopAttractions[taz] * walkPctArray[walkCategory[dcAltNum-1]][taz];
                }
                dcAltNum++;
            }
        }
    }

    private void calculateStopDensity ( ) {

        final int MAX_PURPOSE_CODE = 21;         //See "IncludeInStopSizeTerm.csv (stops directory)
        final int NUMBER_OF_FIELDS = 7;

        int purp;
        double[][] coeff = new double[MAX_PURPOSE_CODE+1][NUMBER_OF_FIELDS];


        // get file name from properties file
        String stopDensityModelsFile = propertyMap.getString(  "include.in.stop.size.term.file");

        // read the stop density models file to get field coefficients
        if (stopDensityModelsFile != null) {
            try {
                CSVFileReader reader = new CSVFileReader();
                TableDataSet sd = reader.readFile(new File(stopDensityModelsFile));

                int purpFieldPosition = sd.getColumnPosition( "purpose" );
                if (purpFieldPosition <= 0) {
                    logger.fatal( "purpose was not a field in the stop density model TableDataSet.");
                    System.exit(1);
                }

                int totpopFieldPosition = sd.getColumnPosition( "total_occ_units" );
                if (totpopFieldPosition <= 0) {
                    logger.fatal( "totalpop was not a field in the stop density model TableDataSet.");
                    System.exit(1);
                }

                int emptotalFieldPosition = sd.getColumnPosition( "emp_total" );
                if (emptotalFieldPosition <= 0) {
                    logger.fatal( "emp_total was not a field in the stop density model TableDataSet.");
                    System.exit(1);
                }

                int empretgFieldPosition = sd.getColumnPosition( "emp_retail" );
                if (empretgFieldPosition <= 0) {
                    logger.fatal( "retail_g was not a field in the stop density model TableDataSet.");
                    System.exit(1);
                }

                int empretsFieldPosition = sd.getColumnPosition( "emp_srvc" );
                if (empretsFieldPosition <= 0) {
                    logger.fatal( "retail_s was not a field in the stop density model TableDataSet.");
                    System.exit(1);
                }

                int empgameFieldPosition = sd.getColumnPosition( "emp_game" );
                if (empgameFieldPosition <= 0) {
                    logger.fatal( "emp_game was not a field in the stop density model TableDataSet.");
                    System.exit(1);
                }

                int empotherFieldPosition = sd.getColumnPosition( "emp_other" );
                if (empotherFieldPosition <= 0) {
                    logger.fatal( "office_e was not a field in the stop density model TableDataSet.");
                    System.exit(1);
                }

                int schenrFieldPosition = sd.getColumnPosition( "school_enrollment" );
                if (schenrFieldPosition <= 0) {
                    logger.fatal( "school_enrollment was not a field in the stop density model TableDataSet.");
                    System.exit(1);
                }

                for (int i=1; i <= sd.getRowCount(); i++) {

                    purp = (int)sd.getValueAt( i, purpFieldPosition );

                    coeff[purp][0] = sd.getValueAt( i, totpopFieldPosition );
                    coeff[purp][1] = sd.getValueAt( i, emptotalFieldPosition );
                    coeff[purp][2] = sd.getValueAt( i, empretgFieldPosition );
                    coeff[purp][3] = sd.getValueAt( i, empretsFieldPosition );
                    coeff[purp][4] = sd.getValueAt( i, empgameFieldPosition );
                    coeff[purp][5] = sd.getValueAt( i, empotherFieldPosition );
                    coeff[purp][6] = sd.getValueAt( i, schenrFieldPosition );
                }
            }
            catch (IOException e) {
                throw new RuntimeException("Could not read the SizeTerm file");
            }
        }
        else {
            throw new RuntimeException( "no stop density model specification file was named in properties file.");
        }



        int hhpopFieldPosition = zoneTable.getColumnPosition(ZonalData.HHS_FIELD);
        if (hhpopFieldPosition <= 0) {
            logger.fatal( ZonalData.HHS_FIELD + " was not a field in the zoneData TableDataSet.");
            System.exit(1);
        }

        int empretgFieldPosition = zoneTable.getColumnPosition( ZonalData.RETAIL_EMP );
        if (empretgFieldPosition <= 0) {
            logger.fatal( ZonalData.RETAIL_EMP + " was not a field in the zoneData TableDataSet.");
            System.exit(1);
        }

        int empretsFieldPosition = zoneTable.getColumnPosition( ZonalData.SERVICE_EMP );
        if (empretsFieldPosition <= 0) {
            logger.fatal( ZonalData.SERVICE_EMP + " was not a field in the zoneData TableDataSet.");
            System.exit(1);
        }

        int empgameFieldPosition = zoneTable.getColumnPosition( ZonalData.GAMING_EMP );
        if (empgameFieldPosition <= 0) {
            logger.fatal( ZonalData.GAMING_EMP + " was not a field in the zoneData TableDataSet.");
            System.exit(1);
        }

        int empotherFieldPosition = zoneTable.getColumnPosition( ZonalData.OTHER_EMP );
        if (empotherFieldPosition <= 0) {
            logger.fatal( ZonalData.OTHER_EMP + " was not a field in the zoneData TableDataSet.");
            System.exit(1);
        }

        int elenrFieldPosition = zoneTable.getColumnPosition(ZonalDataManager.ES_ENROLL);
        if (elenrFieldPosition <= 0) {
            logger.fatal( ZonalDataManager.ES_ENROLL + " was not a field in the zoneData TableDataSet.");
            System.exit(1);
        }

        int hsenrFieldPosition = zoneTable.getColumnPosition( ZonalDataManager.HS_ENROLL );
        if (hsenrFieldPosition <= 0) {
            logger.fatal( ZonalDataManager.HS_ENROLL + " was not a field in the zoneData TableDataSet.");
            System.exit(1);
        }

        int unenrFieldPosition = zoneTable.getColumnPosition( ZonalDataManager.CL_ENROLL );
        if (unenrFieldPosition <= 0) {
            logger.fatal( ZonalDataManager.CL_ENROLL + " was not a field in the zoneData TableDataSet.");
            System.exit(1);
        }


        float[] field = new float[NUMBER_OF_FIELDS];

        float[][] stopAttractions = new float[MAX_PURPOSE_CODE + 1][maxTazNumber +1];

        for (int taz : zoneTable.getColumnAsInt(TAZ)) {

            field[0] = zoneTable.getIndexedValueAt( taz, hhpopFieldPosition );
            field[2] = zoneTable.getIndexedValueAt( taz, empretgFieldPosition );
            field[3] = zoneTable.getIndexedValueAt( taz, empretsFieldPosition );
            field[4] = zoneTable.getIndexedValueAt(taz, empgameFieldPosition);
            field[5] = zoneTable.getIndexedValueAt( taz, empotherFieldPosition );
            field[6] = zoneTable.getIndexedValueAt( taz, elenrFieldPosition )
                    + zoneTable.getIndexedValueAt( taz, hsenrFieldPosition ) +
                    zoneTable.getIndexedValueAt( taz, unenrFieldPosition );
            field[1] = field[2] + field[3] + field[4] + field[5];

        for (int p=1; p <= MAX_PURPOSE_CODE; p++) {

                stopAttractions[p][taz] = 0.0f;
                for (int j=0; j < NUMBER_OF_FIELDS; j++)
                    stopAttractions[p][taz] += field[j]*coeff[p][j];

            }
        }

        for (int p=1; p <= TourType.TYPES; p++) {
            stopSize[0][p] = new float[numberOfSubzones+1];
            stopSize[1][p] = new float[numberOfSubzones+1];
        }
        stopTotSize[0] = new float[numberOfSubzones+1];
        stopTotSize[1] = new float[numberOfSubzones+1];

        float totAttrsOB = 0;
        float totAttrsIB = 0;

        int dcAltNum = 1;
        int[] walkCategory = subzonesTable.getColumnAsInt("shortWalk");
        int[] tazList = subzonesTable.getColumnAsInt("dtaz");
        for (int taz: tazList) {
            totAttrsOB = stopAttractions[10][taz] + stopAttractions[20][taz] + stopAttractions[3][taz] +
                        stopAttractions[4][taz] + stopAttractions[5][taz] + stopAttractions[6][taz] +
                        stopAttractions[7][taz] + stopAttractions[8][taz];

            totAttrsIB = stopAttractions[11][taz] + stopAttractions[21][taz]  + stopAttractions[3][taz] +
                        stopAttractions[4][taz] + stopAttractions[5][taz] + stopAttractions[6][taz] +
                        stopAttractions[7][taz] + stopAttractions[8][taz];

                stopSize[0][1][dcAltNum] = stopAttractions[10][taz]*walkPctArray[walkCategory[dcAltNum-1]][taz];
                stopSize[1][1][dcAltNum] = stopAttractions[11][taz]*walkPctArray[walkCategory[dcAltNum-1]][taz];
                stopSize[0][2][dcAltNum] = stopAttractions[20][taz]*walkPctArray[walkCategory[dcAltNum-1]][taz];
                stopSize[1][2][dcAltNum] = stopAttractions[21][taz]*walkPctArray[walkCategory[dcAltNum-1]][taz];
                stopSize[0][3][dcAltNum] = stopAttractions[3][taz]*walkPctArray[walkCategory[dcAltNum-1]][taz];
                stopSize[1][3][dcAltNum] = stopAttractions[3][taz]*walkPctArray[walkCategory[dcAltNum-1]][taz];
                stopSize[0][4][dcAltNum] = stopAttractions[4][taz]*walkPctArray[walkCategory[dcAltNum-1]][taz];
                stopSize[1][4][dcAltNum] = stopAttractions[4][taz]*walkPctArray[walkCategory[dcAltNum-1]][taz];
                stopSize[0][5][dcAltNum] = stopAttractions[5][taz]*walkPctArray[walkCategory[dcAltNum-1]][taz];
                stopSize[1][5][dcAltNum] = stopAttractions[5][taz]*walkPctArray[walkCategory[dcAltNum-1]][taz];
                stopSize[0][6][dcAltNum] = stopAttractions[6][taz]*walkPctArray[walkCategory[dcAltNum-1]][taz];
                stopSize[1][6][dcAltNum] = stopAttractions[6][taz]*walkPctArray[walkCategory[dcAltNum-1]][taz];
                stopSize[0][7][dcAltNum] = stopAttractions[7][taz]*walkPctArray[walkCategory[dcAltNum-1]][taz];
                stopSize[1][7][dcAltNum] = stopAttractions[7][taz]*walkPctArray[walkCategory[dcAltNum-1]][taz];
                stopSize[0][8][dcAltNum] = stopAttractions[8][taz]*walkPctArray[walkCategory[dcAltNum-1]][taz];
                stopSize[1][8][dcAltNum] = stopAttractions[8][taz]*walkPctArray[walkCategory[dcAltNum-1]][taz];

                stopTotSize[0][dcAltNum] = totAttrsOB*walkPctArray[walkCategory[dcAltNum-1]][taz];
                stopTotSize[1][dcAltNum] = totAttrsIB*walkPctArray[walkCategory[dcAltNum-1]][taz];

                dcAltNum++;
         }
        //(re)build external stop size terms
        dcAltNum = 1;
        for (int taz : tazList) {
            if (taz < firstInternalZoneNumber) {
                //for all tour types
                for (int i = 1; i <= TourType.TYPES; i++) {
                    //for in and outbound stops
                    for (int j = 0; j < 2; j++) {
                        stopSize[j][i][dcAltNum] = attractions[i][dcAltNum];
                    }
                }
            } else {
                break;
            }
            dcAltNum++;
        }
//
//
//        TableDataSet externals;
//        if (ResourceUtil.getBooleanProperty(propertyMap,"summer",true)) {
//            externals = TableDataSetLoader.loadTableDataSet(propertyMap, "external.size.terms.summer" );
//        } else {
//            externals = TableDataSetLoader.loadTableDataSet(propertyMap, "external.size.terms.winter" );
//        }
//        externals.buildIndex(1);
//        dcAltNum = 1;
//        for (int taz : tazList) {
//            if (taz < firstInternalZoneNumber) {
//                //for all tour types
//                for (int i = 1; i < 9; i++) {
//                    //for in and outbound stops
//                    for (int j = 0; j < 2; j++) {
//                        stopSize[j][i][dcAltNum] = externals.getIndexedValueAt(taz, "attraction");
//                    }
//                }
//            } else {
//                break;
//            }
//            dcAltNum++;
//        }

        if(debug){
                subzoneDataCheck.appendColumn(stopSize[0][1], "stopSizeWork_OB");
                subzoneDataCheck.appendColumn(stopSize[1][1], "stopSizeWork_IB");
                subzoneDataCheck.appendColumn(stopSize[0][2], "stopSizeSchool_OB");
                subzoneDataCheck.appendColumn(stopSize[1][2], "stopSizeSchool_IB");
                subzoneDataCheck.appendColumn(stopSize[0][3], "stopSizeEscort_OB");
                subzoneDataCheck.appendColumn(stopSize[1][3], "stopSizeEscort_IB");
                subzoneDataCheck.appendColumn(stopSize[0][4], "stopSizeShop_OB");
                subzoneDataCheck.appendColumn(stopSize[1][4], "stopSizeShop_IB");
                subzoneDataCheck.appendColumn(stopSize[0][5], "stopSizeMaint_OB");
                subzoneDataCheck.appendColumn(stopSize[1][5], "stopSizeMaint_IB");
                subzoneDataCheck.appendColumn(stopSize[0][6], "stopSizeDisc_OB");
                subzoneDataCheck.appendColumn(stopSize[1][6], "stopSizeDisc_IB");
                subzoneDataCheck.appendColumn(stopSize[0][7], "stopSizeEat_OB");
                subzoneDataCheck.appendColumn(stopSize[1][7], "stopSizeEat_IB");
                subzoneDataCheck.appendColumn(stopSize[0][8], "stopSizeAtWork_OB");
                subzoneDataCheck.appendColumn(stopSize[1][8], "stopSizeAtWork_IB");
            }


    }

    //This method checks for if another shadow pricing loop is necessary, and updates the shadow price
    //  if simple is true, then it just makes sure that the employment in a zone can't be overfilled (up to some threshold)
    public boolean checkShadowPriceNecessity(float epsilon) {
        boolean necessary = false;
        float totalWorkTours = 0.0f;
        float totalInvalidEmployment = 0.0f;
        int totalInvalidTAZs = 0;
        for (int taz : zoneTable.getColumnAsInt(TAZ)) {
            totalWorkTours += zonalWorkTrips[taz];
        }           

        ////////This is code to write out the shadow price values to a serialized array////////
//        try {
//            //write object to byte array
//            ByteArrayOutputStream bos = new ByteArrayOutputStream();
//            FileOutputStream outStream = new FileOutputStream(new  File(propertyMap.getString("shadow.price.start.values")));
//            //ObjectOutputStream out = new ObjectOutputStream(bos);
//            ObjectOutputStream out = new ObjectOutputStream(outStream);
//            out.writeObject(shadowPrice);
//            out.flush();
//            out.close();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
        /////////////////////

        for (int taz : zoneTable.getColumnAsInt(TAZ)) {
            if (zonalEmployment[taz] < zonalWorkTrips[taz]) {
                totalInvalidEmployment += zonalWorkTrips[taz] - zonalEmployment[taz];
                totalInvalidTAZs++;
            }
            //have to add 1 to everything to avoid nastiness associated with 0s
            //necessary = necessary | (zonalEmployment[taz] + 1 < zonalWorkTrips[taz] + 1);
            if (zonalEmployment[taz] * epsilon >= 1) {
                necessary = necessary | ((zonalWorkTrips[taz] - zonalEmployment[taz]) / zonalEmployment[taz] > epsilon);
            } else {
                necessary = necessary | (zonalWorkTrips[taz] - zonalEmployment[taz] > (zonalEmployment[taz]*2*epsilon + 1));
            }
            //make sure the shadow price isn't getting too big (for really small employment zones) so it doesn't overshoot
            shadowPrice[taz] *= Math.min(Math.exp(.25),(zonalEmployment[taz] + 1) / (zonalWorkTrips[taz] + 1));
            if ((debug) && (zonalEmployment[taz] + 1 < zonalWorkTrips[taz] + 1)) {
                logger.info("Zonal employment < zonal work trips: Zone " + taz +
                        ", Employment = " + zonalEmployment[taz] + ", Work trips: " + zonalWorkTrips[taz]);
            }
        }
        
        //return totalInvalidEmployment / totalWorkTours > epsilon;
        return necessary | ((float) totalInvalidTAZs / (float) numberOfZones > epsilon);
    }

    //This will calculate the shadow price necessity for external workers
    // assumes that totalExternalWorkers has been set!
    public boolean checkExternalWorkerShadowPriceNecessity(float epsilon) {
        boolean spRequired = false;
        //Yeah we are looping over all tazs, and we only need external ones, but the price to pay is smaller then
        // adding a variable to formally get the external zones
        for (int taz : zoneTable.getColumnAsInt(TAZ)) {
            if (taz >= firstInternalZoneNumber) continue;
            spRequired = spRequired |
                    (Math.abs(zonalExternalWorkerCount[taz] - zonalExternalWorkTours[taz]) / zonalExternalWorkerCount[taz]
                            > epsilon);
            externalWorkerShadowPrice[taz] *= (zonalExternalWorkerCount[taz] + 1) / (zonalExternalWorkTours[taz] + 1);
            if (debug) {
                logger.info("External shadow price, taz " + taz + ": " + externalWorkerShadowPrice[taz]);
                logger.info("Exeternal worker tours, taz" + taz + ": " + zonalExternalWorkTours[taz]);
            }
        }
        return spRequired;
    }


    /**
     * Check if a column already exists
     * @param table: A TableDataSet table
     * @param colName: A column name to be checked
     * @return true is column exists, false otherwise
     */
    private boolean isColExist(TableDataSet table, String colName) {
        boolean result = false;

        for (int i = 0; i < table.getColumnCount(); i++) {
            if (colName.equals(table.getColumnLabel(i))) {
                result = true;
            }
        }

        return result;
    }



    public TableDataSet getZonalTableDataSet() {
        return zoneTable;
    }

    public static float getWalkPct(int subzoneIndex, int taz) {
        return walkPctArray[subzoneIndex][taz];
    }



    public static float getTotSize ( int tourTypeIndex, int altIndex ) {
        return totSize[tourTypeIndex][altIndex];
    }

    public static float getStopSizeByTourType (int dir, int tourTypeIndex, int altIndex){
        return stopSize[dir][tourTypeIndex][altIndex];
    }

    public static float getTotalStopSize (int dir, int altIndex){
        return stopTotSize[dir][altIndex];
    }

    public static float getVisitorSize (int tourTypeIndex, int altIndex) {
        return totVisitorSize[tourTypeIndex][altIndex];
    }

    public static float getVisitorStopSize (int altIndex) {
        return totVisitorStopSize[altIndex];
    }



    public int getNumberOfZones () {
        return numberOfZones;
    }



    public static void setLogsumDcAMPM (int altIndex, float logsum ) {
        logsumDcAMPM[altIndex] = logsum;
    }


    public static void setLogsumDcAMMD ( int altIndex, float logsum ) {
        logsumDcAMMD[altIndex] = logsum;
    }


    public static void setLogsumDcMDMD ( int altIndex, float logsum ) {
        logsumDcMDMD[altIndex] = logsum;
    }


    public static void setLogsumDcPMNT ( int altIndex, float logsum ) {
        logsumDcPMNT[altIndex] = logsum;
    }


    public static void setLogsumDcNTNT ( int altIndex, float logsum ) {
        logsumDcNTNT[altIndex] = logsum;
    }


    public static void setOdUtilModeAlt ( double[] ModalUtilities ) {
        odUtilModeAlt = ModalUtilities;
    }

    public static void clearStaticLogsumMatrices(){
        Arrays.fill(logsumDcAMMD, 0);
        Arrays.fill(logsumDcAMPM, 0);
        Arrays.fill(logsumDcMDMD, 0);
        Arrays.fill(logsumDcPMNT, 0);
        Arrays.fill(logsumDcNTNT, 0);
    }

    public void clearModeUtilityMatrix(){
        Arrays.fill(odUtilModeAlt, 0);
    }

    public static void addWorkTrip(int taz) {
        zonalWorkTrips[taz]++;
    }

    public static void clearZonalWorkTrips() {
        Arrays.fill(zonalWorkTrips, 0.0f);
    }

    public void setExternalWorkers(float workers) {
        totalExternalWorkers = workers;
        externalCoefficients.buildIndex(1);
        //fill in the external worker count array (which says the amount of external workers that should be coming from
        // each external station) and the size terms, which currently equal the unrounded external worker count
        zonalExternalWorkerCount = new float[numberOfExternalZones+1];
        externalWorkerSize = new float[numberOfExternalZones+1];
        for (int i = 1; i <= externalCoefficients.getRowCount(); i++) {
            externalWorkerSize[i] = workers *
                    externalCoefficients.getIndexedValueAt(i,EXTERNAL_SIZE_COEFFICIENT_EXTWORKER_FIELD);
            zonalExternalWorkerCount[i] = Math.round(externalWorkerSize[i]);
        }
    }

    public void addExternalWorkTour(int taz) {
        assert taz < firstInternalZoneNumber;
        zonalExternalWorkTours[taz]++;
    }

    public void clearExternalWorkTours() {
        Arrays.fill(zonalExternalWorkTours,0.0f);
    }


    //for testing purpose
    public static void main(String[] args) {
        ZonalDataManager zm = ZonalDataManager.getInstance();
//        logger.info("Creating Household Array ...");
//        HouseholdArrayManager ham = HouseholdArrayManager.getInstance();
//        ham.createBigHHArray();
//        logger.info("Household Array created");
//
//        //Balance Size Variables based on zonal productions and attractions
//        //can't actually do this for real until the daily activity pattern model has
//        //run.
//        zm.balanceMandatorySizeVariables(ham.getHouseholds());


    }

}
