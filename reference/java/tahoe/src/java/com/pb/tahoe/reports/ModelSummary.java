package com.pb.tahoe.reports;

import com.pb.tahoe.visitor.PartyArrayManager;
import com.pb.tahoe.visitor.structures.*;
import com.pb.tahoe.ExternalWorkers.ExternalWorkerArrayManager;
import com.pb.tahoe.ExternalWorkers.ExternalWorker;
import com.pb.tahoe.util.HouseholdArrayManager;
import com.pb.tahoe.util.TODDataManager;
import com.pb.tahoe.structures.Household;
import com.pb.tahoe.structures.Tour;
import com.pb.common.util.ResourceUtil;
import com.pb.common.datafile.TableDataSet;
import com.pb.common.datafile.TextFile;

import java.util.*;
import java.io.File;

/**
 * User: Frazier
 * Date: Jun 17, 2007
 * Time: 11:53:08 AM
 * Created by IntelliJ IDEA.
 */
public class ModelSummary {

    private static ResourceBundle rb = ResourceUtil.getResourceBundle("tahoe");
    private HouseholdArrayManager ham = HouseholdArrayManager.getInstance();
    private ExternalWorkerArrayManager ewam = ExternalWorkerArrayManager.getInstance();
    private PartyArrayManager pam = PartyArrayManager.getInstance();

    public static void generateModelSummary() {
        ModelSummary ms = new ModelSummary();
        TextFile.writeTo(rb.getString("modelSummary"),ms.getSummary());
    }

    public ModelSummary(boolean buildFromScratch) {
        if (buildFromScratch) {
            ham.createBigHHArrayFromDiskObject("_afterAtWorkStops.doa");
            ewam.createExternalWorkerArray("external.worker.ot.results.file");
            String pamFileName = rb.getString("latest.party.array.manager.file");
            File partyArrayData = new File(pamFileName + "_afterThruVisitors.pam");
            File tempPartyArrayData = new File(pamFileName);
            partyArrayData.renameTo(tempPartyArrayData);
            pam.createPartyArray("latest.party.array.manager.file");
            tempPartyArrayData.renameTo(partyArrayData);
        }
    }

    public ModelSummary() {
        this(false);
    }

    public String getSummary() {
        StringBuffer sb = new StringBuffer();
        sb.append(residentPopulationSummary());
        sb.append(externalWorkerPopulationSummary());
        sb.append(visitorPopulationSummary());
        return sb.toString();
    }

    private String residentPopulationSummary() {
        TableDataSet residentData = buildResidentTable();
        List<MapEnum[]> resCross = new ArrayList<MapEnum[]>();
        resCross.add(new MapEnum[] {MapEnum.INCOME,MapEnum.HH_SIZE});
        resCross.add(new MapEnum[] {MapEnum.INCOME,MapEnum.WORKER});
        resCross.add(new MapEnum[] {MapEnum.HH_SIZE,MapEnum.WORKER});
        int[] widths = {15,15,15};
        String[] firstColNames = {"","",""};

        TableDataSet residentData2 = buildResidentTourTable();
        List<MapEnum[]> resCross2 = new ArrayList<MapEnum[]>();
        resCross2.add(new MapEnum[] {MapEnum.RESTOURTYPE,MapEnum.RESTOURMODE});
        int[] widths2 = {23};
        String[] firstColNames2 = {""};

        List<MapEnum[]> resCross3 = new ArrayList<MapEnum[]>();
        resCross3.add(new MapEnum[] {MapEnum.RESTOURTYPE,MapEnum.SKIMSTART});
        resCross3.add(new MapEnum[] {MapEnum.RESTOURTYPE,MapEnum.SKIMEND});
        resCross3.add(new MapEnum[] {MapEnum.RESTOURTYPE,MapEnum.STOP});
        int[] widths3 = {23,23,23};
        String[] firstColNames3 = {"","",""};

        List<MapEnum[]> resCross4 = new ArrayList<MapEnum[]>();
        resCross4.add(new MapEnum[] {MapEnum.RESTOURTYPE,MapEnum.EXTERNAL_STATION});


        return getSummaries(residentData,resCross,firstColNames,widths,"Residential Population Summary",true,true) +
                getSummaries(residentData2,resCross2,firstColNames2,widths2,"Residential Tour Summary",true,true) +
                getSummaries(residentData2,resCross3,firstColNames3,widths3,"",false,true) +
                getSummaries(residentData2,resCross4,firstColNames2,widths2,"",true,true);
    }

    private String externalWorkerPopulationSummary() {
        TableDataSet externalData = buildExternalWorkerTable();
        List<MapEnum[]> extCross = new ArrayList<MapEnum[]>();
        extCross.add(new MapEnum[] {MapEnum.ENTRANCE_STATION,MapEnum.SKIMSTART});
        extCross.add(new MapEnum[] {MapEnum.ENTRANCE_STATION,MapEnum.SKIMEND});
        int[] widths = {18,18};
        //String[] firstColNames = {"row","",""};
        String[] firstColNames = {"",""};
        return getSummaries(externalData,extCross,firstColNames,widths,"External Worker Population Summary",true,true);
    }

    private String visitorPopulationSummary() {
        StringBuffer sb = new StringBuffer();
        TableDataSet externalData = buildVisitorTable();
        TableDataSet overnightVisitorData = buildOvernightVisitorTourTable();
        TableDataSet dayVisitorData = buildDayVisitorTourTable();
        List<MapEnum[]> extCross = new ArrayList<MapEnum[]>();
        extCross.add(new MapEnum[] {MapEnum.VISITOR_STAY_TYPE,MapEnum.PARTY_SIZE});
        int[] widths = {15};
        String[] firstColNames = {""};
        sb.append(getSummaries(externalData,extCross,firstColNames,widths,"Overnight Visitor Population Summary",true,true));
        extCross.clear();
        extCross.add(new MapEnum[] {MapEnum.VISITOR_STAY_TYPE,MapEnum.VISITOR_TOUR_TYPE});
        extCross.add(new MapEnum[] {MapEnum.VISITOR_TOUR_TYPE,MapEnum.VISITOR_TOUR_MODE});
        int[] widths2 = {15,15};
        String[] firstColNames2 = {"",""};
        sb.append(getSummaries(overnightVisitorData,extCross,firstColNames2,widths2,"",true,true));
        extCross.clear();
        int[] widths3 = {18};
        int[] widths4 = {18,18,18};
        String[] firstColNames4 = {"","",""};
        extCross.add(new MapEnum[] {MapEnum.VISITOR_TOUR_TYPE,MapEnum.SKIMSTART});
        extCross.add(new MapEnum[] {MapEnum.VISITOR_TOUR_TYPE,MapEnum.SKIMEND});
        extCross.add(new MapEnum[] {MapEnum.VISITOR_TOUR_TYPE,MapEnum.STOP});
        sb.append(getSummaries(overnightVisitorData,extCross,firstColNames4,widths4,"",false,true));

        extCross.clear();
        extCross.add(new MapEnum[] {MapEnum.ENTRANCE_STATION,MapEnum.PARTY_SIZE});
        sb.append(getSummaries(externalData,extCross,firstColNames,widths3,"Day Visitor Population Summary",true,true));
        extCross.clear();
        extCross.add(new MapEnum[] {MapEnum.ENTRANCE_STATION,MapEnum.VISITOR_TOUR_TYPE});
        String[] firstColNames3 = {""};
        sb.append(getSummaries(dayVisitorData,extCross,firstColNames3,widths3,"",true,true));
        extCross.clear();
        extCross.add(new MapEnum[] {MapEnum.VISITOR_TOUR_TYPE,MapEnum.SKIMSTART});
        extCross.add(new MapEnum[] {MapEnum.VISITOR_TOUR_TYPE,MapEnum.SKIMEND});
        extCross.add(new MapEnum[] {MapEnum.VISITOR_TOUR_TYPE,MapEnum.STOP});
        sb.append(getSummaries(dayVisitorData,extCross,firstColNames4,widths4,"",false,true));

        extCross.clear();
        extCross.add(new MapEnum[] {MapEnum.THRU_ENTRANCE_STATION,MapEnum.THRU_EXIT_STATION});
        sb.append(getSummaries(externalData,extCross,firstColNames,widths3,"Thru Visitor Population Summary",true,true));
        return sb.toString();
    }

    private TableDataSet buildResidentTable() {
        ArrayList<String> headers = new ArrayList<String>();
        headers.add(MapEnum.HH_SIZE.toString());
        headers.add(MapEnum.INCOME.toString());
        headers.add(MapEnum.WORKER.toString());
        Household[] households = ham.getHouseholds();
        float[][] data = new float[households.length - 1][headers.size()];
        for (int i = 1; i < households.length; i++) {
            data[i-1][0] = households[i].getHHSize();
            data[i-1][1] = households[i].getHHIncome();
            data[i-1][2] = households[i].getFtwkPersons() + households[i].getPtwkPersons();
        }
        return TableDataSet.create(data,headers);
    }

    private TableDataSet buildResidentTourTable() {
        ArrayList<String> headers = new ArrayList<String>();
        headers.add(MapEnum.RESTOURTYPE.toString());
        headers.add(MapEnum.RESTOURMODE.toString());
        headers.add(MapEnum.SKIMSTART.toString());
        headers.add(MapEnum.SKIMEND.toString());
        headers.add(MapEnum.STOP.toString());
        headers.add(MapEnum.EXTERNAL_STATION.toString());
        Household[] households = ham.getHouseholds();
        ArrayList<Object> dataAL = new ArrayList<Object>();
        for (int i = 1; i < households.length; i++) {
            if (households[i].getMandatoryTours() != null)
                for (Tour tour : households[i].getMandatoryTours()) {
                    float[] tourData = new float[headers.size()];
                    tourData[0] = tour.getTourType();
                    tourData[1] = tour.getMode();
                    tourData[2] = TODDataManager.getTodStartSkimPeriod(tour.getTimeOfDayAlt());
                    tourData[3] = TODDataManager.getTodEndSkimPeriod(tour.getTimeOfDayAlt());
                    tourData[4] = tour.getStopFreqAlt();
                    tourData[5] = tour.getDestTaz();
                    dataAL.add(tourData);
                    if (tour.getSubTours() != null)
                        for (Tour stour : tour.getSubTours()) {
                            float[] stourData = new float[headers.size()];
                            stourData[0] = 12;
                            stourData[1] = stour.getMode();
                            stourData[2] = TODDataManager.getTodStartSkimPeriod(stour.getTimeOfDayAlt());
                            stourData[3] = TODDataManager.getTodEndSkimPeriod(stour.getTimeOfDayAlt());
                            stourData[4] = stour.getStopFreqAlt();
                            stourData[5] = stour.getDestTaz();
                            dataAL.add(stourData);
                        }
                }
            if (households[i].getJointTours() != null)
                for (Tour tour : households[i].getJointTours()) {
                    float[] tourData = new float[headers.size()];
                    tourData[0] = tour.getTourType() - 1;
                    tourData[1] = tour.getMode();
                    tourData[2] = TODDataManager.getTodStartSkimPeriod(tour.getTimeOfDayAlt());
                    tourData[3] = TODDataManager.getTodEndSkimPeriod(tour.getTimeOfDayAlt());
                    tourData[4] = tour.getStopFreqAlt();
                    tourData[5] = tour.getDestTaz();
                    dataAL.add(tourData);
                }
            if (households[i].getIndivTours() != null)
                for (Tour tour : households[i].getIndivTours()) {
                    float[] tourData = new float[headers.size()];
                    tourData[0] = tour.getTourType() + 3;
                    tourData[1] = tour.getMode();
                    tourData[2] = TODDataManager.getTodStartSkimPeriod(tour.getTimeOfDayAlt());
                    tourData[3] = TODDataManager.getTodEndSkimPeriod(tour.getTimeOfDayAlt());
                    tourData[4] = tour.getStopFreqAlt();
                    tourData[5] = tour.getDestTaz();
                    if (tourData[0] == 6)
                        tourData[0] = 11;
                    dataAL.add(tourData);
                }
        }
        float[][] data = new float[dataAL.size()][];
        int i = 0;
        for (Object o : dataAL) {
            data[i++] = (float[]) o;
        }
        return TableDataSet.create(data,headers);
    }

    private TableDataSet buildExternalWorkerTable() {
        ArrayList<String> headers = new ArrayList<String>();
        headers.add(MapEnum.ENTRANCE_STATION.toString());
        headers.add(MapEnum.EXTERNAL_WORKER.toString());
        headers.add(MapEnum.SKIMSTART.toString());
        headers.add(MapEnum.SKIMEND.toString());
        ExternalWorker[] workers = ewam.workers;
        float[][] data = new float[workers.length][headers.size()];
        for (int i = 1; i < workers.length; i++) {
            data[i-1][0] = workers[i].getHomeTaz();
            data[i-1][1] = 0;
            data[i-1][2] = workers[i].getSkimPeriodOut();
            data[i-1][3] = workers[i].getSkimPeriodIn();
        }
        return TableDataSet.create(data,headers);
    }

    private TableDataSet buildVisitorTable() {
        ArrayList<String> headers = new ArrayList<String>();
        headers.add(MapEnum.VISITOR_STAY_TYPE.toString());
        headers.add(MapEnum.OVERNIGHT_VISITOR.toString());
        headers.add(MapEnum.PARTY_SIZE.toString());
        headers.add(MapEnum.ENTRANCE_STATION.toString());
        headers.add(MapEnum.THRU_ENTRANCE_STATION.toString());
        headers.add(MapEnum.THRU_EXIT_STATION.toString());
        TravelParty[] parties = pam.parties;
        float[][] data = new float[parties.length][headers.size()];
        for (int i = 1; i < parties.length; i++) {
            data[i-1][0] = parties[i].getStayTypeID() + 10*parties[i].getVisitorType();
            data[i-1][1] = 0;
            data[i-1][2] = parties[i].getPersons();
            data[i-1][3] = parties[i].getTazID() * (parties[i].getVisitorType() - 1);
            data[i-1][4] = parties[i].getTazID() * (parties[i].getVisitorType() - 2);
            try {
                data[i-1][5] = parties[i].getTours()[0].getDestTAZ() * (parties[i].getVisitorType() - 2);
            } catch (ArrayIndexOutOfBoundsException e) {
                data[i-1][5] = -1;
            }
        }
        return TableDataSet.create(data,headers);
    }

    private TableDataSet buildOvernightVisitorTourTable() {
        ArrayList<String> headers = new ArrayList<String>();
        headers.add(MapEnum.VISITOR_STAY_TYPE.toString());
        headers.add(MapEnum.VISITOR_TOUR_TYPE.toString());
        headers.add(MapEnum.VISITOR_TOUR_MODE.toString());
        headers.add(MapEnum.STOP.toString());
        headers.add(MapEnum.SKIMSTART.toString());
        headers.add(MapEnum.SKIMEND.toString());
        TravelParty[] parties = pam.parties;
        ArrayList<Object> dataAL = new ArrayList<Object>();
        for (TravelParty party : parties) {
            if (party == null) continue;
            if (party.getVisitorType() > 1) continue;
            for (VisitorTour tour : party.getTours()) {
                float[] dataRow = new float[headers.size()];
                dataRow[0] = party.getStayTypeID() + 10*party.getVisitorType();
                dataRow[1] = tour.getTourType().getID();
                dataRow[2] = tour.getMode().getId();
                dataRow[3] = 1 + (tour.getInboundStop() ?  1 : 0) + (tour.getOutboundStop() ? 2 : 0);
                dataRow[4] = TODDataManager.getTodStartSkimPeriod(tour.getTimeOfDayAlt());
                dataRow[5] = TODDataManager.getTodEndSkimPeriod(tour.getTimeOfDayAlt());
                dataAL.add(dataRow);
            }
        }
        float[][] data = new float[dataAL.size()][];
        int i = 0;
        for (Object o : dataAL) {
            data[i++] = (float[]) o;
        }
        return TableDataSet.create(data,headers);
    }

    private TableDataSet buildDayVisitorTourTable() {
        ArrayList<String> headers = new ArrayList<String>();
        headers.add(MapEnum.ENTRANCE_STATION.toString());
        headers.add(MapEnum.VISITOR_TOUR_TYPE.toString());
        headers.add(MapEnum.STOP.toString());
        headers.add(MapEnum.SKIMSTART.toString());
        headers.add(MapEnum.SKIMEND.toString());
        TravelParty[] parties = pam.parties;
        ArrayList<Object> dataAL = new ArrayList<Object>();
        for (TravelParty party : parties) {
            if (party == null) continue;
            if (party.getVisitorType() != 2) continue;
            for (VisitorTour tour : party.getTours()) {
                float[] dataRow = new float[headers.size()];
                dataRow[0] = party.getTazID();
                dataRow[1] = tour.getTourType().getID();
                dataRow[2] = 1 + (tour.getInboundStop() ?  1 : 0) + (tour.getOutboundStop() ? 2 : 0);
                dataRow[3] = TODDataManager.getTodStartSkimPeriod(tour.getTimeOfDayAlt());
                dataRow[4] = TODDataManager.getTodEndSkimPeriod(tour.getTimeOfDayAlt());
                dataAL.add(dataRow);
            }
        }
        float[][] data = new float[dataAL.size()][];
        int i = 0;
        for (Object o : dataAL) {
            data[i++] = (float[]) o;
        }
        return TableDataSet.create(data,headers);
    }

    private String getSummaries(TableDataSet data, List<MapEnum[]> crossTabs, String[] firstColNames, int[] widths, String title, boolean rowSum, boolean colSum) {
        StringBuffer summary = new StringBuffer();
        if (!title.equals("")) {
            summary.append("*****").append(title).append("*****").append("\n\n");
        }
        for (int i = 0; i < crossTabs.size(); i++) {
            MapEnum cross1 = crossTabs.get(i)[0];
            MapEnum cross2 = crossTabs.get(i)[1];
            summary.append(cross1.name).append(" by ").append(cross2.name).append("\n");
            summary.append(crossSummary(data,cross1,cross2,firstColNames[i],widths[i],rowSum,colSum));
        }
//        summary.append("*****").append(getChars('*',title.length())).append("*****\n\n");
        return summary.toString();
    }

    private String crossSummary(TableDataSet data, MapEnum cross1, MapEnum cross2, String firstColName, int width, boolean rowSum, boolean colSum) {
        LinkedHashMap<String,LinkedHashMap<String,Integer>> tableMap = initCrossMap(cross1,cross2);
        for (int i = 1; i <= data.getRowCount(); i++) {
            String cm1 = cross1.map((int) data.getValueAt(i,cross1.toString()));
            String cm2 = cross2.map((int) data.getValueAt(i,cross2.toString()));
            try {
                tableMap.get(cm1).put(cm2,tableMap.get(cm1).get(cm2) + 1);
            } catch (NullPointerException e) {
                //this is for items we wanted to ignore
            }
        }
        if (firstColName.equals("row")) {
            return buildTable(tableMap,cross1.name,width,rowSum,colSum);
        } else if (firstColName.equals("col")) {
            return buildTable(tableMap,cross2.name,width,rowSum,colSum);
        } else {
            return buildTable(tableMap,"",width,rowSum,colSum);
        }
    }


    private LinkedHashMap<String,LinkedHashMap<String,Integer>> initCrossMap(MapEnum cross1, MapEnum cross2) {
        LinkedHashMap<String,LinkedHashMap<String,Integer>> tableMap = new LinkedHashMap<String,LinkedHashMap<String,Integer>>();
        for (int i = cross1.min; i <= cross1.max; i++) {
            LinkedHashMap<String,Integer> subMap = new LinkedHashMap<String,Integer>();
            for (int j = cross2.min; j <= cross2.max; j++) {
                subMap.put(cross2.map(j),0);
            }
            tableMap.put(cross1.map(i),subMap);
        }
        return tableMap;
    }

    private String buildTable(LinkedHashMap<String,LinkedHashMap<String,Integer>> counts, String firstColName, int width, boolean rowSum, boolean colSum) {
        StringBuffer summary = new StringBuffer();
        String line = null;
        boolean first = true;
        HashMap<String,Integer> crossSums = new HashMap<String,Integer>();
        for (String key : counts.keySet()) {
            if (first) {
                first = false;
                if (rowSum) {
                    line = getLine(width,counts.get(key).keySet().size()+2) + "\n";
                } else {
                    line = getLine(width,counts.get(key).keySet().size()+1) + "\n";
                }
                summary.append(line).append("|").append(justify(firstColName,width,"center")).append("|");
                for (String key2 : counts.get(key).keySet()) {
                    summary.append(justify(key2,width,"center")).append("|");
                    crossSums.put(key2,0);
                }
                if (rowSum) {
                    summary.append(justify("Total",width,"center")).append("|");
                }
                summary.append("\n").append(line);
            }
            summary.append("|").append(justify(key,width,"center")).append("|");
            int sum = 0;
            for (String key2 : counts.get(key).keySet()) {
                Integer value = counts.get(key).get(key2);
                summary.append(justify(value.toString(),width,"center")).append("|");
                sum += value;
                crossSums.put(key2,crossSums.get(key2) + value);
            }
            if (rowSum) {
                summary.append(justify("" + sum,width,"center")).append("|");
            }
            summary.append("\n");
        }
        if (colSum) {
            summary.append("|").append(justify("Total",width,"center")).append("|");
            Integer crossSummed = 0;
            for (String key2 : counts.get(counts.keySet().iterator().next()).keySet()) {
                summary.append(justify(crossSums.get(key2).toString(),width,"center")).append("|");
                crossSummed += crossSums.get(key2);
            }
            if (rowSum) {
                summary.append(justify(crossSummed.toString(),width,"center")).append("|");
            }
        }
        summary.append("\n").append(line).append("\n");
        return summary.toString();
    }

    private String getChars(char c, int count) {
        StringBuffer s = new StringBuffer();
        for(int i = 0; i < count; i++) {
            s.append(c);
        }
        return s.toString();
    }

    private String justify(String s, int size, String type, int offset) {
        StringBuffer sb = new StringBuffer();
        int left = 0;
        if (type.equalsIgnoreCase("center")) {
            left = (size - s.length()) / 2;
        } else if (type.equalsIgnoreCase("right")) {
            left = size - offset - s.length();
        } else if (type.equalsIgnoreCase("left")) {
            left = offset;
        }
        int right = size - s.length() - left;
        sb.append(getChars(' ',left)).append(s).append(getChars(' ',right));
        return sb.toString();
    }

    private String justify(String s, int size, String type) {
        return justify(s,size,type,0);
    }

    private String getLine(int width,int cols) {
        StringBuffer sb = new StringBuffer("+");
        for (int i = 0; i < cols; i++) {
            sb.append(getChars('-',width)).append("+");
        }
        return sb.toString();
    }





    private enum MapEnum {
        INCOME(0,2,"Household Income") {
            String map(int i) {
                return incomeMap(i);
            }
        },
        HH_SIZE(1,4,"Household Size") {
            String map(int i) {
                return hhSizeMap(i);
            }
        },
        WORKER(0,3,"Workers in Household") {
            String map(int i) {
                return workerMap(i);
            }
        },
        RESTOURTYPE(1,12,"Residential Tour Type") {
            String map(int i) {
                return resTourTypeMap(i);
            }
        },
        RESTOURMODE(1,6,"Residential Tour Mode") {
            String map(int i) {
                return resTourModeMap(i);
            }
        },
        SKIMSTART(1,4,"Start Skim Period") {
            String map(int i) {
                return skimPeriodMap(i);
            }
        },
        SKIMEND(1,4,"End Skim Period") {
            String map(int i) {
                return skimPeriodMap(i);
            }
        },
        EXTERNAL_STATION(1,7,"External Destination") {
            String map(int i) {
                return externalMap(i);
            }
        },
        STOP(1,4,"Tour Stops") {
            String map(int i) {
                return stopMap(i);
            }
        },
        ENTRANCE_STATION(1,7,"Entrance Station") {
            String map(int i) {
                return externalMap(i);
            }
        },
        EXTERNAL_WORKER(1,1,"External Workers") {
            String map(int i) {
                return "External Workers";
            }
        },
        VISITOR_STAY_TYPE(11,16,"Stay Type") {
            String map(int i) {
                return stayTypeMap(i);
            }
        },
        OVERNIGHT_VISITOR(1,1,"Overnight Visitors") {
            String map(int i) {
                return "Overnight Visitors";
            }
        },
        PARTY_SIZE(1,4,"Travel Party Size") {
            String map(int i) {
                return hhSizeMap(i);
            }
        },
        THRU_ENTRANCE_STATION(1,7,"Entrance Station") {
            String map(int i) {
                return externalMap(i);
            }
        },
        THRU_EXIT_STATION(1,7,"Exit Station") {
            String map(int i) {
                return externalMap(i);
            }
        },
        VISITOR_TOUR_TYPE(1,4,"Visitor Tour Type") {
            String map(int i) {
                return VisitorTourType.getTourType(i).toString();
            }
        },
        VISITOR_TOUR_MODE(1,5,"Visitor Tour Mode") {
            String map(int i) {
                return VisitorMode.getMode(i).toString();
            }
        },


        ;

        private int min;
        private int max;
        private String name;
        private MapEnum(int min, int max, String name) {
            this.min = min;
            this.max = max;
            this.name = name;
        }

        abstract String map(int i);
    }

    private static String incomeMap(int i) {
        switch (i) {
            case 0 : return "Low Income";
            case 1 : return "Medium Income";
            default: return "High Income";
        }
    }

    private static String hhSizeMap(int i) {
        switch (i) {
            case 1 : return "1 Person";
            case 2 : return "2 Persons";
            case 3 : return "3 Persons";
            default : return "4+ Persons";
        }
    }

    private static String workerMap(int i) {
        switch (i) {
            case 0 : return "No Workers";
            case 1 : return "1 Worker";
            case 2 : return "2 Workers";
            default : return "3+ Workers";
        }
    }

    private static String resTourTypeMap(int i) {
        switch(i) {
            case 1 : return "Work";
            case 2 : return "School";
            case 3 : return "Joint Shop";
            case 4 : return "Joint Maintenance";
            case 5 : return "Joint Discretionary";
            case 6 : return "Joint Eat";
            case 7 : return "Indiv Shop";
            case 8 : return "Indiv Maintenance";
            case 9 : return "Indiv Discretionary";
            case 10 : return "Indiv Eat";
            case 11 : return "Indiv Escort";
            case 12 : return "At-Work";
            default : return "Not a tour";
        }
    }

    private static String resTourModeMap(int i) {
        switch(i) {
            case 1 : return "Drive Alone";
            case 2 : return "Shared Auto";
            case 3 : return "Walk to Transit";
            case 4 : return "Drive to Transit";
            case 5 : return "Non-Motorized";
            case 6 : return "School Bus";
            default : return "Not a mode";
        }
    }

    private static String skimPeriodMap(int i) {
        switch(i) {
            case 1 : return "AM Peak";
            case 2 : return "PM Peak";
            case 3 : return "Midday";
            case 4 : return "Overnight";
            default : return "Not a period";
        }
    }

    private static String stopMap(int i) {
        switch(i) {
            case 1 : return "No Stops";
            case 2 : return "Outbound Stop";
            case 3 : return "Inbound Stop";
            case 4 : return "In & Outbound Stop";
            default : return "Not a stop";
        }
    }

    private static String externalMap(int i) {
        switch (i) {
            case 1 : return "Reno";
            case 2 : return "Carson City";
            case 3 : return "Kingsbury";
            case 4 : return "Kirkwood";
            case 5 : return "Placerville";
            case 6 : return "Truckee/Squaw";
            case 7 : return "Truckee/Northstar";
            default : return "Internal";
        }
    }

    private static String stayTypeMap(int i) {
        switch(i) {
            case 11 : return "Seasonal";
            case 12 : return "Hotel/Motel";
            case 13 : return "Casino";
            case 14 : return "Resort";
            case 15 : return "House";
            case 16 : return "Campground";
            default : return "OTHER";
        }
    }

    public static void main(String[] args) {
        ModelSummary ms = new ModelSummary(true);
        System.out.println(ms.getSummary());
    }

}
