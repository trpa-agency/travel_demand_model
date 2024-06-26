package com.pb.tahoe.reports;

import com.pb.common.datafile.TableDataSet;
import com.pb.common.datafile.TableDataSetLoader;
import com.pb.common.datafile.TextFile;
import com.pb.common.matrix.Matrix;
import com.pb.common.matrix.ZipMatrixReader;
import com.pb.common.util.ResourceUtil;
import com.pb.tahoe.visitor.structures.VisitorDataStructure;
import com.pb.tahoe.visitor.structures.VisitorTourType;

import java.io.File;
import java.util.*;

/**
 * User: Frazier
 * Date: Sep 12, 2006
 * Time: 9:17:53 AM
 * Created by IntelliJ IDEA.
 */
public class TahoeReports {

    static ResourceBundle rb = ResourceUtil.getResourceBundle("tahoe");
    static TableDataSet districtTable = TableDataSetLoader.loadTableDataSet(rb, "zone.districts.file");

    public void dummyFilter(ReportMaker rm, String name, String colName) {
        //Creat dummy filter
        String[] dummyNames = {"None"};
        float[][] dummyValues = new float[1][1];
        dummyValues[0][0] = 1.0f;
        rm.addFilter(name,colName,dummyValues,dummyNames);

    }

    public void countyFilter(ReportMaker rm, String name, String colName) {
        //Create county linked hashMap
        LinkedHashMap<Float,String> countyNameList = new LinkedHashMap<Float,String>();
        countyNameList.put(1.0f,"Washoe");
        countyNameList.put(2.0f,"Carson City");
        countyNameList.put(3.0f,"Douglas");
        countyNameList.put(4.0f,"El Dorado");
        countyNameList.put(5.0f,"Placer");
        countyNameList.put(6.0f,"External");
        rm.addFilterFromTable(name,districtTable,"taz","county_district",countyNameList,colName);
    }

    public void nsFilter(ReportMaker rm, String name, String colName) {
        //Create north/south shore linked hashMap
        LinkedHashMap<Float,String> districtNameList = new LinkedHashMap<Float,String>();
        districtNameList.put(1.0f,"North Shore");
        districtNameList.put(2.0f,"South Shore");
        districtNameList.put(3.0f,"External");
        rm.addFilterFromTable(name,districtTable,"taz","district",districtNameList,colName);
    }

    public void intextFilter(ReportMaker rm, String name, String colName) {
        //Create int/ext linked hashMap
        LinkedHashMap<Float,String> intextNameList = new LinkedHashMap<Float,String>();
        intextNameList.put(1.0f,"Internal");
        intextNameList.put(2.0f,"Internal");
        intextNameList.put(3.0f,"External");
        rm.addFilterFromTable(name,districtTable,"taz","district",intextNameList,colName);
    }

    public void extFilter(ReportMaker rm, String name, String colName) {
        //Create int/ext zone linked hashMap
        LinkedHashMap<Float,String> extNameList = new LinkedHashMap<Float,String>();
        extNameList.put(0.0f,"Internal");
        extNameList.put(1.0f,"Reno");
        extNameList.put(2.0f,"Carson City");
        extNameList.put(3.0f,"Kingsbury Grade");
        extNameList.put(4.0f,"Kirkwood");
        extNameList.put(5.0f,"Placerville");
        extNameList.put(6.0f,"Squaw");
        extNameList.put(7.0f,"Truckee");
        rm.addFilterFromTable(name,districtTable,"taz","ext_zone",extNameList,colName);
    }

    public void incomeFilter(ReportMaker rm, String name, String colName) {
        //Create income filter
        String[] incomeNames = {"Low","Mid","High"};
        float[][] incomeValues = new float[3][1];
        incomeValues[0][0] = 0.0f;
        incomeValues[1][0] = 1.0f;
        incomeValues[2][0] = 2.0f;
        rm.addFilter(name,colName,incomeValues,incomeNames);
    }

    public void modeFilter(ReportMaker rm, String name, String colName) {
        //Create mode choice filter with school bus
        String[] modeNames = {"Drive Alone","Shared Ride","Drive to Transit","Walk to Transit","Non-Motorized","School Bus"};
        float[][] modeValues = new float[6][1];
        modeValues[0][0] = 1.0f;
        modeValues[1][0] = 2.0f;
        modeValues[2][0] = 4.0f;
        modeValues[3][0] = 3.0f;
        modeValues[4][0] = 5.0f;
        modeValues[5][0] = 6.0f;
        rm.addFilter(name,colName,modeValues,modeNames);
    }

    public void purposeFilter(ReportMaker rm, String name, String colName) {
        //Create purpose filter
        String[] purposeNames = {"Work","School"};
        float[][] purposeValues = new float[2][1];
        purposeValues[0][0] = 1.0f;
        purposeValues[1][0] = 2.0f;
        rm.addFilter(name,colName,purposeValues,purposeNames);
    }

    public void personFilter(ReportMaker rm, String name, String colName) {
        //Create person type filter
        String[] personNames = {"Full Time","Part Time","NonWorker","PreSchool","PreDriver","Driver"};
        float[][] personValues = new float[6][1];
        personValues[0][0] = 1.0f;
        personValues[1][0] = 2.0f;
        personValues[2][0] = 3.0f;
        personValues[3][0] = 4.0f;
        personValues[4][0] = 5.0f;
        personValues[5][0] = 6.0f;
        rm.addFilter(name,colName,personValues,personNames);
    }

    public void personYOFilter(ReportMaker rm, String name, String colName) {
        //Create simpleperson type filter
        String[] personYONames = {"Adult","Adult","Adult","Child","Child","Child"};
        float[][] personYOValues = new float[6][1];
        personYOValues[0][0] = 1.0f;
        personYOValues[1][0] = 2.0f;
        personYOValues[2][0] = 3.0f;
        personYOValues[3][0] = 4.0f;
        personYOValues[4][0] = 5.0f;
        personYOValues[5][0] = 6.0f;
        rm.addFilter(name,colName,personYOValues,personYONames);
    }

    public void distanceFilter(ReportMaker rm, String name, String colName) {
        //Create distance class filter
        String[] distanceNames = {"0-3","3-6","6-9","9-12","12-15","15-20","20-25","25-30","30-35","35+"};
        float[][] distanceValues = new float[10][1];
        distanceValues[0][0] = 0.0f;
        distanceValues[1][0] = 1.0f;
        distanceValues[2][0] = 2.0f;
        distanceValues[3][0] = 3.0f;
        distanceValues[4][0] = 4.0f;
        distanceValues[5][0] = 5.0f;
        distanceValues[6][0] = 6.0f;
        distanceValues[7][0] = 7.0f;
        distanceValues[8][0] = 8.0f;
        distanceValues[9][0] = 9.0f;
        rm.addFilter(name,colName,distanceValues,distanceNames);
    }

    public void timeFilter(ReportMaker rm, String name, String colName) {  //Create time filter
        String[] timeNames = {"Midnight","1 AM","2 AM","3 AM","4 AM","5 AM","6 AM","7 AM","8 AM",
                               "9 AM","10 AM","11 AM","Noon","1 PM","2 PM","3 PM","4 PM","5 PM",
                               "6 PM","7 PM","8 PM","9 PM","10 PM","11 PM"};
        float[][] timeValues = new float[24][1];
        timeValues[0][0] = 0.0f;
        timeValues[1][0] = 1.0f;
        timeValues[2][0] = 2.0f;
        timeValues[3][0] = 3.0f;
        timeValues[4][0] = 4.0f;
        timeValues[5][0] = 5.0f;
        timeValues[6][0] = 6.0f;
        timeValues[7][0] = 7.0f;
        timeValues[8][0] = 8.0f;
        timeValues[9][0] = 9.0f;
        timeValues[10][0] = 10.0f;
        timeValues[11][0] = 11.0f;
        timeValues[12][0] = 12.0f;
        timeValues[13][0] = 13.0f;
        timeValues[14][0] = 14.0f;
        timeValues[15][0] = 15.0f;
        timeValues[16][0] = 16.0f;
        timeValues[17][0] = 17.0f;
        timeValues[18][0] = 18.0f;
        timeValues[19][0] = 19.0f;
        timeValues[20][0] = 20.0f;
        timeValues[21][0] = 21.0f;
        timeValues[22][0] = 22.0f;
        timeValues[23][0] = 23.0f;
        rm.addFilter(name,colName,timeValues,timeNames);
    }

    public void durationFilter(ReportMaker rm, String name, String colName) {
        //Create duration filter
        float[][] timeValues = new float[24][1];
        timeValues[0][0] = 0.0f;
        timeValues[1][0] = 1.0f;
        timeValues[2][0] = 2.0f;
        timeValues[3][0] = 3.0f;
        timeValues[4][0] = 4.0f;
        timeValues[5][0] = 5.0f;
        timeValues[6][0] = 6.0f;
        timeValues[7][0] = 7.0f;
        timeValues[8][0] = 8.0f;
        timeValues[9][0] = 9.0f;
        timeValues[10][0] = 10.0f;
        timeValues[11][0] = 11.0f;
        timeValues[12][0] = 12.0f;
        timeValues[13][0] = 13.0f;
        timeValues[14][0] = 14.0f;
        timeValues[15][0] = 15.0f;
        timeValues[16][0] = 16.0f;
        timeValues[17][0] = 17.0f;
        timeValues[18][0] = 18.0f;
        timeValues[19][0] = 19.0f;
        timeValues[20][0] = 20.0f;
        timeValues[21][0] = 21.0f;
        timeValues[22][0] = 22.0f;
        timeValues[23][0] = 23.0f;
        String[] durNames = {"0","1","2","3","4","5","6","7","8","9","10","11","12","13","14","15",
                             "16","17","18","19","20","21","22","23"};
        rm.addFilter(name,colName,timeValues,durNames);
    }

    public void tourFilter(ReportMaker rm, String name, String colName) {
        //Create tour type filter
        String[] tourNames = {"Work","School","Escorting","Shop","Other-Maintenance","Discretionary","Eat","At Work"};
        float[][] tourValues = new float[8][1];
        tourValues[0][0] = 1.0f;
        tourValues[1][0] = 2.0f;
        tourValues[2][0] = 3.0f;
        tourValues[3][0] = 4.0f;
        tourValues[4][0] = 5.0f;
        tourValues[5][0] = 6.0f;
        tourValues[6][0] = 7.0f;
        tourValues[7][0] = 8.0f;
        rm.addFilter(name,colName,tourValues,tourNames);
    }

    public void atWorkTourFilter(ReportMaker rm, String name, String colName) {
        //Create tour type filter
        String[] tourNames = {"Work Related","Eat","Other"};
        float[][] tourValues = new float[3][1];
        tourValues[0][0] = 1.0f;
        tourValues[1][0] = 2.0f;
        tourValues[2][0] = 3.0f;
        rm.addFilter(name,colName,tourValues,tourNames);
    }

    public void jointTourFilter(ReportMaker rm, String name, String colName) {
        //Create joint tour type filter
        String[] jointTourNames = {"None","S","E","M","D","SS","SE","SM","SD","EE","EM","ED","MM","MD","DD"};
        float[][] jointTourValues = new float[15][1];
        jointTourValues[0][0] = 1.0f;
        jointTourValues[1][0] = 2.0f;
        jointTourValues[2][0] = 3.0f;
        jointTourValues[3][0] = 4.0f;
        jointTourValues[4][0] = 5.0f;
        jointTourValues[5][0] = 6.0f;
        jointTourValues[6][0] = 7.0f;
        jointTourValues[7][0] = 8.0f;
        jointTourValues[8][0] = 9.0f;
        jointTourValues[9][0] = 10.0f;
        jointTourValues[10][0] = 11.0f;
        jointTourValues[11][0] = 12.0f;
        jointTourValues[12][0] = 13.0f;
        jointTourValues[13][0] = 14.0f;
        jointTourValues[14][0] = 15.0f;
        rm.addFilter(name,colName,jointTourValues,jointTourNames);
    }

    public void tourCompFilter(ReportMaker rm, String name, String colName) {
        //Create tour composition type filter
        String[] tourCompNames = {"Adults","Children","Mixed"};
        float[][] tourCompValues = new float[3][1];
        tourCompValues[0][0] = 1.0f;
        tourCompValues[1][0] = 2.0f;
        tourCompValues[2][0] = 3.0f;
        rm.addFilter(name,colName,tourCompValues,tourCompNames);
    }

    public void tourPartFilter(ReportMaker rm, String name, String colName) {
        //Create tour composition type filter
        String[] tourPartNames = {"Shop","Eat","Other-Maintenance","Discretionary","Not Participate"};
        float[][] tourPartValues = new float[5][1];
        tourPartValues[0][0] = 4.0f;
        tourPartValues[1][0] = 7.0f;
        tourPartValues[2][0] = 5.0f;
        tourPartValues[3][0] = 6.0f;
        tourPartValues[4][0] = 0.0f;
        rm.addFilter(name,colName,tourPartValues,tourPartNames);
    }

    public void tourPartFilter2(ReportMaker rm, String name, String colName) {
        //Create tour participation type filter
        String[] tourPartNames = {"Participate","Not Participate"};
        float[][] tourPartValues = new float[2][1];
        tourPartValues[0][0] = 1.0f;
        tourPartValues[1][0] = 2.0f;
        rm.addFilter(name,colName,tourPartValues,tourPartNames);
    }

    public void hhSizePartFilter(ReportMaker rm, String name, String colName) {
        //Create tour composition type filter
        String[] hhsPartNames = {"2",">2",">2",">2",">2",">2",">2",">2",">2",">2",">2",">2",">2",">2"};
        float[][] hhsPartValues = new float[14][1];
        hhsPartValues[0][0] = 2.0f;
        hhsPartValues[1][0] = 3.0f;
        hhsPartValues[2][0] = 4.0f;
        hhsPartValues[3][0] = 5.0f;
        hhsPartValues[4][0] = 6.0f;
        hhsPartValues[5][0] = 7.0f;
        hhsPartValues[6][0] = 8.0f;
        hhsPartValues[7][0] = 9.0f;
        hhsPartValues[8][0] = 10.0f;
        hhsPartValues[9][0] = 11.0f;
        hhsPartValues[10][0] = 12.0f;
        hhsPartValues[11][0] = 13.0f;
        hhsPartValues[12][0] = 14.0f;
        hhsPartValues[13][0] = 15.0f;
        rm.addFilter(name,colName,hhsPartValues,hhsPartNames);

    }

    public void indMaintFreqFilter(ReportMaker rm, String name, String colName) {
        //Create individual maintenance frequency filter
        String[] maintFreqNames = {"M","S","P","MS","MSS","MSP/PP","MSP/PP","MSSP/PP","MSSP/PP","MP/PP","MP/PP","MM","MMS","MMSS",
                "MMSP/PP","MMSP/PP","MMSSP/PP","MMSSP/PP","MMP/PP","MMP/PP","MMM","MMMS","MMMSS","MMMSP/PP","MMMSP/PP",
        "MMMSSP/PP","MMMSSP/PP","MMMP/PP","MMMP/PP","SP","SPP","SS","SSP","SSPP","PP","N","N"};

        float[][] maintFreqValues = new float[37][1];
        maintFreqValues[0][0] = 2.0f;
        maintFreqValues[1][0] = 13.0f;
        maintFreqValues[2][0] = 5.0f;
        maintFreqValues[3][0] = 14.0f;
        maintFreqValues[4][0] = 26.0f;
        maintFreqValues[5][0] = 18.0f;
        maintFreqValues[6][0] = 22.0f;
        maintFreqValues[7][0] = 30.0f;
        maintFreqValues[8][0] = 34.0f;
        maintFreqValues[9][0] = 6.0f;
        maintFreqValues[10][0] = 10.0f;
        maintFreqValues[11][0] = 3.0f;
        maintFreqValues[12][0] = 15.0f;
        maintFreqValues[13][0] = 27.0f;
        maintFreqValues[14][0] = 19.0f;
        maintFreqValues[15][0] = 23.0f;
        maintFreqValues[16][0] = 31.0f;
        maintFreqValues[17][0] = 35.0f;
        maintFreqValues[18][0] = 7.0f;
        maintFreqValues[19][0] = 11.0f;
        maintFreqValues[20][0] = 4.0f;
        maintFreqValues[21][0] = 16.0f;
        maintFreqValues[22][0] = 28.0f;
        maintFreqValues[23][0] = 20.0f;
        maintFreqValues[24][0] = 24.0f;
        maintFreqValues[25][0] = 32.0f;
        maintFreqValues[26][0] = 36.0f;
        maintFreqValues[27][0] = 8.0f;
        maintFreqValues[28][0] = 12.0f;
        maintFreqValues[29][0] = 17.0f;
        maintFreqValues[30][0] = 21.0f;
        maintFreqValues[31][0] = 25.0f;
        maintFreqValues[32][0] = 29.0f;
        maintFreqValues[33][0] = 33.0f;
        maintFreqValues[34][0] = 9.0f;
        maintFreqValues[35][0] = 0.0f;
        maintFreqValues[35][0] = 1.0f;
        rm.addFilter(name,colName,maintFreqValues,maintFreqNames);
    }

    public void indDiscFreqFilter(ReportMaker rm, String name, String colName) {
        //Create individual maintenance frequency filter
        String[] discFreqNames = {"N","D","DD","E","ED"};

        float[][] discFreqValues = new float[5][1];
        discFreqValues[0][0] = 1.0f;
        discFreqValues[1][0] = 3.0f;
        discFreqValues[2][0] = 4.0f;
        discFreqValues[3][0] = 2.0f;
        discFreqValues[4][0] = 5.0f;
        rm.addFilter(name,colName,discFreqValues,discFreqNames);
    }

    public void indAtWorkFreqFilter(ReportMaker rm, String name, String colName) {
        //Create individual maintenance frequency filter
        String[] awFreqNames = {"E","O","R","RR","ER","N"};

        float[][] awFreqValues = new float[6][1];
        awFreqValues[0][0] = 2.0f;
        awFreqValues[1][0] = 4.0f;
        awFreqValues[2][0] = 3.0f;
        awFreqValues[3][0] = 5.0f;
        awFreqValues[4][0] = 6.0f;
        awFreqValues[5][0] = 1.0f;
        rm.addFilter(name,colName,awFreqValues,awFreqNames);
    }

    public void stopsFreqFilter(ReportMaker rm, String name, String colName) {
        //Create stops frequency filter
        String[] stopFreqNames = {"N","O","I","OI"};

        float[][] stopFreqValues = new float[4][1];
        stopFreqValues[0][0] = 1.0f;
        stopFreqValues[1][0] = 2.0f;
        stopFreqValues[2][0] = 3.0f;
        stopFreqValues[3][0] = 4.0f;
        rm.addFilter(name,colName,stopFreqValues,stopFreqNames);
    }

    public void stopDummyFilter(ReportMaker rm, String name, String colName) {
        //Create stops frequency filter
        String[] stopNames = {"N","Y"};

        float[][] stopValues = new float[2][1];
        stopValues[0][0] = 0.0f;
        stopValues[1][0] = 1.0f;
        rm.addFilter(name,colName,stopValues,stopNames);
    }

    public void stopDistanceDiffFilter(ReportMaker rm, String name, String colName) {
        //Create stops frequency filter
        String[] distDiffNames = {"<2.5","2.5-5.0","5.0-7.5","7.5-10.0","10.0-15.0","15.0+"};

        float[][] distDiffValues = new float[6][1];
        distDiffValues[0][0] = 0.0f;
        distDiffValues[1][0] = 1.0f;
        distDiffValues[2][0] = 2.0f;
        distDiffValues[3][0] = 3.0f;
        distDiffValues[4][0] = 4.0f;
        distDiffValues[5][0] = 5.0f;
        rm.addFilter(name,colName,distDiffValues,distDiffNames);
    }

    private void numberOfActivitiesFilter(ReportMaker rm, String name, String colName, String[] patternSet) {
        float[][] activityValues = new float[patternSet.length][1];
        String[] activityCountNames = new String[patternSet.length];
        for (int i = 0; i < patternSet.length; i++) {
            activityValues[i][0] = i + 1;
            activityCountNames[i] = String.valueOf(patternSet[i].length() - patternSet[i].split(VisitorDataStructure.homeChar).length);
        }
        rm.addFilter(name,colName,activityValues,activityCountNames);
    }

    private void presenceOfActivityFilter(ReportMaker rm, String name, String colName, String[] patternSet, char activityChar) {
        float[][] activityValues = new float[patternSet.length][1];
        String[] activityNames = {"Has " + activityChar + " activity","Has no " + activityChar + " activity"};
        String[] activityNameValues = new String[patternSet.length];
        for (int i = 1; i < patternSet.length; i++) {
            activityValues[i][0] = i + 1;
            activityNameValues[i] = activityNames[(patternSet[i].split(String.valueOf(activityChar)).length > 1 ? 0 : 1)];
        }
        rm.addFilter(name,colName,activityValues,activityNameValues);
    }

    public void numberOfStopsFilter(ReportMaker rm, String name, String colName, String[] patternSet) {
        //create number of stops in pattern filter
        float[][] stopsValues = new float[patternSet.length][1];
        TreeSet<Integer> stopsEnumeration = new TreeSet<Integer>();
        for (int i = 0; i < patternSet.length; i++) {
            stopsValues[i][0] = patternSet[i].split(VisitorDataStructure.stopChar).length - 1;
            stopsEnumeration.add((int) stopsValues[i][0]);
        }
        String[] stopsNames = new String[stopsEnumeration.size()];
        int counter = 0;
        for (Integer i : stopsEnumeration) {
            stopsNames[counter++] = i.toString();
        }
        rm.addFilter(name,colName,stopsValues,stopsNames);
    }

    private  String[] overnightVisitorPatternNames = {"H","HGH","HGHGH","HGHGTH","HGHRH","HGHRHOH","HGHTRH","HGHTRTH","HGTH","HGTHGH",
                "HGTHOH","HGTHTGH","HOH","HOHGH","HOHOH","HOHOHGH","HOHOHGHGH","HOHOHOH","HOHOHRH","HOHOHRHRHOH",
                "HOHOHSH","HOHOTH","HOHRH","HOHRHGH","HOHRHRH","HOHRHSH","HOHRTH","HOHSH","HOHSHRH","HOHSHSH","HOHTGH",
                "HOHTGTH","HOHTOTH","HOHTRH","HOHTRTH","HOHTSTH","HOTH","HOTHSTH","HOTHTGH","HRH","HRHGH","HRHGHGH",
                "HRHGTH","HRHOH","HRHOHGH","HRHOHOH","HRHOHRH","HRHRH","HRHRHGH","HRHRHOH","HRHRHRH","HRHSH","HRHSHOH",
                "HRHSHOHOH","HRHSTH","HRHTGH","HRHTRH","HRHTRTH","HRHTSTH","HRTH","HRTHGH","HRTHOH","HRTHRH","HRTHRTH",
                "HRTHTGTH","HRTHTRH","HSH","HSHOH","HSHOHGH","HSHOHOH","HSHRH","HSHRHGH","HSHRHOH","HSHRTH","HSHSH",
                "HSHSTH","HSHTRH","HSHTRTH","HSTH","HSTHGH","HSTHOH","HTGH","HTGHGH","HTGHGTH","HTGHOH","HTGTH",
                "HTGTHGH","HTGTHOH","HTGTHTGH","HTOH","HTOHRTH","HTOTH","HTOTHGH","HTOTHRH","HTRH","HTRHGH","HTRHGTH",
                "HTRHOH","HTRHOTH","HTRHRH","HTRHRTH","HTRHSH","HTRHTGH","HTRHTGTH","HTRHTRTH","HTRTH","HTRTHGH",
                "HTRTHOH","HTRTHTGH","HTSH","HTSHGH","HTSHOH","HTSHTOH","HTSHTSTH","HTSTH","HTSTHOH","HTSTHRH"};

    public void overnightVisitorPatternFilter(ReportMaker rm, String name, String colName) {
        //Create overnight pattern filter
        float[][] overnightPatternValues = new float[overnightVisitorPatternNames.length][1];
        for (int i = 0; i < overnightVisitorPatternNames.length; i++) {
            overnightPatternValues[i][0] = (float) i + 1;
        }
        rm.addFilter(name,colName,overnightPatternValues,overnightVisitorPatternNames);
    }

    public void overnightVisitorNumberOfActivitiesFilter(ReportMaker rm, String name, String colName) {
        numberOfActivitiesFilter(rm,name,colName,overnightVisitorPatternNames);
    }

    public void overnightVisitorPresenceOfActivityFilter(ReportMaker rm, String name, String colName, char activityChar) {
        presenceOfActivityFilter(rm,name,colName,overnightVisitorPatternNames,activityChar);
    }

    public void overnightVisitorNumberOfStopsFilter(ReportMaker rm, String name, String colName) {
        numberOfStopsFilter(rm,name,colName,overnightVisitorPatternNames);
    }

     private  String[] dayVisitorPatternNames = {"HGH","HGTH","HTGTH","HTGTH","HGTH","HTRH","HTRTH","HGTH","HOH","HTGH",
             "HTGTH","HTGTH","HOTH","HTOTH","HTRH","HTRTH","HTRTH","HTSH","HRH","HRTH","HRTH","HRTH","HTRTH","HTRTH",
             "HTRTH","HTRTH","HRTH","HSH","HTGH","HTGTH","HSTH","HTRH"};

    public void dayVisitorPatternFilter(ReportMaker rm, String name, String colName) {
        //Create overnight pattern filter
        float[][] dayPatternValues = new float[dayVisitorPatternNames.length][1];
        for (int i = 0; i < dayVisitorPatternNames.length; i++) {
            dayPatternValues[i][0] = (float) i + 1;
        }
        rm.addFilter(name,colName,dayPatternValues,dayVisitorPatternNames);
    }

    public void dayVisitorNumberOfActivitiesFilter(ReportMaker rm, String name, String colName) {
        numberOfActivitiesFilter(rm,name,colName,dayVisitorPatternNames);
    }

    public void dayVisitorPresenceOfActivityFilter(ReportMaker rm, String name, String colName, char activityChar) {
        presenceOfActivityFilter(rm,name,colName,dayVisitorPatternNames,activityChar);
    }

    public void dayVisitorNumberOfStopsFilter(ReportMaker rm, String name, String colName) {
        numberOfStopsFilter(rm,name,colName,dayVisitorPatternNames);
    }

    public void visitorTypeFilter(ReportMaker rm, String name, String colName) {
        String[] visitorTypeNames = {"Overnight", "Day", "Thru"};
        float[][] visitorTypeValues = new float[3][1];
        visitorTypeValues[0][0] = 1.0f;
        visitorTypeValues[1][0] = 2.0f;
        visitorTypeValues[2][0] = 3.0f;
        rm.addFilter(name,colName,visitorTypeValues,visitorTypeNames);
    }

    public void visitorTourTypeFilter(ReportMaker rm, String name, String colName) {
        String[] visitorTourTypeNames = {"Recreation","Gaming","Shopping","Other"};
        float[][] visitorTourTypeValues = new float[4][1];
        for (int i = 0; i < visitorTourTypeValues.length; i++) {
            visitorTourTypeValues[i][0] = (float) (i + 1);
        }
        rm.addFilter(name,colName,visitorTourTypeValues,visitorTourTypeNames);
    }

    public void visitorModeFilter(ReportMaker rm, String name, String colName) {
        //Create mode choice filter with school bus
        String[] modeNames = {"Drive","Shuttle","Drive to Transit","Walk to Transit","Non-Motorized"};
        float[][] modeValues = new float[5][1];
        modeValues[0][0] = 1.0f;
        modeValues[1][0] = 2.0f;
        modeValues[2][0] = 4.0f;
        modeValues[3][0] = 3.0f;
        modeValues[4][0] = 5.0f;
        rm.addFilter(name,colName,modeValues,modeNames);
    }

    public void externalWorkerSkimFilter(ReportMaker rm, String name, String colName) {
        //Create a filter for skim periods
        String[] skimNames = {"AM","MD","PM","LN"};
        float[][] skimValues = new float[4][1];
        skimValues[0][0] = 1.0f;
        skimValues[1][0] = 3.0f;
        skimValues[2][0] = 2.0f;
        skimValues[3][0] = 4.0f;
        rm.addFilter(name,colName,skimValues,skimNames);
    }



    //            End of filters             //


    public void addDistanceAndTimeToTable(TableDataSet baseTable) {
        //create distance mapping and attach to table data set
        // while we're at it, do duration calculation as well
        //define distance classes
        float[] distanceClasses = {0.0f,3.0f,6.0f,9.0f,12.0f,15.0f,20.0f,25.0f,30.0f,35.0f};
        //Read matrix from file
        File distanceFile = new File(rb.getString("skims.directory") + "/" + rb.getString("sovDistMd.file") + "." + rb.getString("skims.format"));
        Matrix distMat = ZipMatrixReader.readMatrix(distanceFile,"Distance Matrix");
        File timeFile = new File(rb.getString("skims.directory") + "/" + rb.getString("sovTimeMd.file") + "." + rb.getString("skims.format"));
        Matrix timeMat = ZipMatrixReader.readMatrix(timeFile,"Time Matrix");
        //create new column and add it to baseTable
        float[] distances = new float[baseTable.getRowCount()];
        float[] distanceVals = new float[baseTable.getRowCount()];
        float[] times = new float[baseTable.getRowCount()];
        float[] duration = new float[baseTable.getRowCount()];
        for (int i = 1; i <= baseTable.getRowCount(); i++) {
            duration[i-1] = baseTable.getValueAt(i,"TOD_EndHr") - baseTable.getValueAt(i,"TOD_StartHr");
            float distance = distMat.getValueAt((int) baseTable.getValueAt(i,"origTaz"),(int) baseTable.getValueAt(i,"destTaz"));
            distanceVals[i-1] = distance;
            times[i-1] = timeMat.getValueAt((int) baseTable.getValueAt(i,"origTaz"),(int) baseTable.getValueAt(i,"destTaz"));
            float counter = 0.0f;
            distances[i-1] = 9.0f;
            for (float j : distanceClasses) {
                if (distance < j) {
                    distances[i-1] = counter - 1.0f;
                    break;
                }
                counter += 1.0f;
            }
        }
        baseTable.appendColumn(distances,"distance");
        baseTable.appendColumn(duration,"duration");
        baseTable.appendColumn(distanceVals,"distances");
        baseTable.appendColumn(times,"travelTime");
    }

    public void addStopDistanceStuffToTable(TableDataSet baseTable) {
        //create the distance mapping between origin and destination TAZ, as well as from origin to stop to destinatio
        // and then calculate ratios and differences
        //define distance ratio classes
        float[] distanceDiffClasses = {0.0f,2.5f,5.0f,7.5f,10.0f,15.0f};
        File distanceFile = new File(rb.getString("skims.directory") + "/" + rb.getString("sovDistMd.file") + "." + rb.getString("skims.format"));
        Matrix distMat = ZipMatrixReader.readMatrix(distanceFile,"Distance Matrix");
        //create new columns and add them to base table
        float[] OBdistanceRatio = new float[baseTable.getRowCount()];
        float[] OBdistanceDiffClass = new float[baseTable.getRowCount()];
        float[] OBdistanceDiff = new float[baseTable.getRowCount()];
        float[] OBDummy = new float[baseTable.getRowCount()];
        float[] IBdistanceRatio = new float[baseTable.getRowCount()];
        float[] IBdistanceDiffClass = new float[baseTable.getRowCount()];
        float[] IBdistanceDiff = new float[baseTable.getRowCount()];
        float[] IBDummy = new float[baseTable.getRowCount()];
        for (int i = 1; i <= baseTable.getRowCount(); i++) {
            if ((int) baseTable.getValueAt(i,"OB_stop_taz") != 0) {
                float ijdistance = distMat.getValueAt((int) baseTable.getValueAt(i,"OB_start_taz"),(int) baseTable.getValueAt(i,"IB_start_taz"));
                float ikdistance = distMat.getValueAt((int) baseTable.getValueAt(i,"OB_start_taz"),(int) baseTable.getValueAt(i,"OB_stop_taz"));
                float kjdistance = distMat.getValueAt((int) baseTable.getValueAt(i,"OB_stop_taz"),(int) baseTable.getValueAt(i,"IB_start_taz"));
                OBdistanceRatio[i-1] = (ikdistance + kjdistance) / ijdistance;
                float distanceDiff = (ikdistance + kjdistance) - ijdistance;
                if (distanceDiff >= 0.0f) {
                    OBdistanceDiff[i-1] = distanceDiff;
                } else {
                    OBdistanceDiff[i-1] = 0.0f;
                }
                float counter = 0.0f;
                OBdistanceDiffClass[i-1] = 5.0f;
                for (float j : distanceDiffClasses) {
                    if (OBdistanceDiff[i-1] < j) {
                        OBdistanceDiffClass[i-1] = counter - 1.0f;
                        break;
                    }
                    counter += 1.0f;
                }
                OBDummy[i-1] = 1.0f;
            } else {
                OBdistanceRatio[i-1] = 0.0f;
                OBdistanceDiff[i-1] = 0.0f;
                OBdistanceDiffClass[i-1] = 0.0f;
                OBDummy[i-1] = 0.0f;
                baseTable.setValueAt(i, "OB_stop_taz", 1.0f);
            }
            if ((int) baseTable.getValueAt(i,"IB_stop_taz") != 0) {
                float jidistance = distMat.getValueAt((int) baseTable.getValueAt(i,"IB_start_taz"),(int) baseTable.getValueAt(i,"OB_start_taz"));
                float jkdistance = distMat.getValueAt((int) baseTable.getValueAt(i,"IB_start_taz"),(int) baseTable.getValueAt(i,"IB_stop_taz"));
                float kidistance = distMat.getValueAt((int) baseTable.getValueAt(i,"IB_stop_taz"),(int) baseTable.getValueAt(i,"OB_start_taz"));
                IBdistanceRatio[i-1] = (kidistance + jkdistance) / jidistance;
                float distanceDiff = (kidistance + jkdistance) - jidistance;
                if (distanceDiff >= 0.0f) {
                    IBdistanceDiff[i-1] = distanceDiff;
                } else {
                    IBdistanceDiff[i-1] = 0.0f;
                }
                float counter = 0.0f;
                IBdistanceDiffClass[i-1] = 5.0f;
                for (float j : distanceDiffClasses) {
                    if (IBdistanceRatio[i-1] < j) {
                        IBdistanceDiffClass[i-1] = counter - 1.0f;
                        break;
                    }
                    counter += 1.0f;
                }
                IBDummy[i-1] = 1.0f;
            } else {
                IBdistanceRatio[i-1] = 0.0f;
                IBdistanceDiff[i-1] = 0.0f;
                IBdistanceDiffClass[i-1] = 0.0f;
                IBDummy[i-1] = 0.0f;
                baseTable.setValueAt(i, "IB_stop_taz", 1.0f);
            }
        }
        baseTable.appendColumn(OBdistanceRatio,"OBDistanceRatio");
        baseTable.appendColumn(OBdistanceDiff,"OBDistanceDiff");
        baseTable.appendColumn(OBdistanceDiffClass,"OBDistanceDiffClass");
        baseTable.appendColumn(OBDummy,"OBDummy");
        baseTable.appendColumn(IBdistanceRatio,"IBDistanceRatio");
        baseTable.appendColumn(IBdistanceDiff,"IBDistanceDiff");
        baseTable.appendColumn(IBdistanceDiffClass,"IBDistanceDiffClass");
        baseTable.appendColumn(IBDummy,"IBDummy");
    }

    public void textCombiner(String[] fileList, String outputFile) {
        String outString = "";
        for (String f : fileList) {
            outString += TextFile.readFrom(f) + "\n";
        }
        TextFile.writeTo(outputFile, outString);
    }

    public void mandDTMReport(String type) {
        TableDataSet baseTable = TableDataSetLoader.loadTableDataSet(rb, "mandatory_dtm.choice.output.file");
        addDistanceAndTimeToTable(baseTable);

        ReportMaker rm = new ReportMaker(baseTable);
        rm.addOnes();
        this.countyFilter(rm, "OCounty","origTaz");
        rm.copyFilter("DCounty","OCounty","destTaz");
        this.nsFilter(rm, "ODistrict","origTaz");
        rm.copyFilter("DDistrict","ODistrict","destTaz");
        this.intextFilter(rm, "DIntExt","destTaz");
        this.extFilter(rm, "DExtDistrict","destTaz");
        this.incomeFilter(rm, "Income","income");
        this.modeFilter(rm, "Mode Choice", "MC");
        this.purposeFilter(rm, "Purpose", "purpose");
        this.personFilter(rm, "Person", "personType");
        this.personYOFilter(rm, "PersonYO", "personType");
        this.distanceFilter(rm, "Distance", "distance");
        this.timeFilter(rm, "DepTime", "TOD_StartHr");
        rm.copyFilter("ArrTime","DepTime","TOD_EndHr");
        this.durationFilter(rm, "Duration", "duration");

        if (type.equals("Work")) {
            String[] filterGroup = {"Purpose","DIntExt","Income","OCounty","Mode Choice"};
            rm.filter2File("ones",filterGroup,ResourceUtil.getProperty(rb,"mcOtaz"));

            String[] filterGroup2 = {"Purpose","DIntExt","Income","DCounty","Mode Choice"};
            rm.filter2File("ones",filterGroup2,ResourceUtil.getProperty(rb,"mcDtaz"));

            String[] filterGroupDCDist = {"Purpose","DIntExt","Income","Distance","OCounty"};
            rm.filter2File("ones",filterGroupDCDist,ResourceUtil.getProperty(rb,"dcDist"));

            String[] filterGroupDCCounty = {"Purpose","Income","OCounty","DCounty"};
            rm.filter2File("ones",filterGroupDCCounty,ResourceUtil.getProperty(rb,"dcCounty"));

            String[] filterGroupDCExt = {"Purpose","Income","OCounty","DExtDistrict"};
            rm.filter2File("ones",filterGroupDCExt,ResourceUtil.getProperty(rb,"dcExt"));

            String[] filterGroupTodDep = {"Purpose","DepTime","Person"};
            rm.filter2File("ones",filterGroupTodDep,ResourceUtil.getProperty(rb,"todDep"));

            String[] filterGroupTodArr = {"Purpose","ArrTime","Person"};
            rm.filter2File("ones",filterGroupTodArr,ResourceUtil.getProperty(rb,"todArr"));

            String[] filterGroupTodDur = {"Purpose","Duration","Person"};
            rm.filter2File("ones",filterGroupTodDur,ResourceUtil.getProperty(rb,"todDur"));

            //Summaries
            String[] filterGroupTest2 = {"Purpose","DIntExt","Income"};
            rm.filterSummary("distances",filterGroupTest2);
            rm.filterSummary("travelTime",filterGroupTest2);
        }
        if (type.equals("School")) {
            String[] filterGroup = {"Purpose","DIntExt","PersonYO","OCounty","Mode Choice"};
            rm.filter2File("ones",filterGroup,ResourceUtil.getProperty(rb,"mcOtaz"));

            String[] filterGroup2 = {"Purpose","DIntExt","PersonYO","DCounty","Mode Choice"};
            rm.filter2File("ones",filterGroup2,ResourceUtil.getProperty(rb,"mcDtaz"));

            String[] filterGroupDCCounty = {"Purpose","PersonYO","OCounty","DCounty"};
            rm.filter2File("ones",filterGroupDCCounty,ResourceUtil.getProperty(rb,"dcCounty"));

            String[] filterGroupDCDist = {"Purpose","DIntExt","PersonYO","Distance","OCounty"};
            rm.filter2File("ones",filterGroupDCDist,ResourceUtil.getProperty(rb,"dcDist"));

            String[] filterGroupDCExt = {"Purpose","PersonYO","OCounty","DExtDistrict"};
            rm.filter2File("ones",filterGroupDCExt,ResourceUtil.getProperty(rb,"dcExt"));

            String[] filterGroupTodDep = {"PersonYO","DepTime","Purpose"};
            rm.filter2File("ones",filterGroupTodDep,ResourceUtil.getProperty(rb,"todDep"));

            String[] filterGroupTodArr = {"PersonYO","ArrTime","Purpose"};
            rm.filter2File("ones",filterGroupTodArr,ResourceUtil.getProperty(rb,"todArr"));

            String[] filterGroupTodDur = {"PersonYO","Duration","Purpose"};
            rm.filter2File("ones",filterGroupTodDur,ResourceUtil.getProperty(rb,"todDur"));

            //Summaries
            String[] filterGroupTest = {"Purpose","DIntExt","PersonYO"};
            rm.filterSummary("distances",filterGroupTest);
            rm.filterSummary("travelTime",filterGroupTest);
        }

    }

    public void mandReport() {
        this.mandDTMReport("Work");
        String[] fileList = {rb.getString("mcOtaz"),
                 rb.getString("mcDtaz"),
                 rb.getString("dcDist"),
                 rb.getString("dcCounty"),
                 rb.getString("dcExt"),
                 rb.getString("todDep"),
                 rb.getString("todArr"),
                 rb.getString("todDur")};
        this.textCombiner(fileList, rb.getString("mandWorkFull"));
        this.mandDTMReport("School");
        this.textCombiner(fileList, rb.getString("mandSchoolFull"));
    }

    public void jointTourReport() {
        //JT Frequency
        TableDataSet baseTable = TableDataSetLoader.loadTableDataSet(rb, "joint.tour.freq.output.file");
        ReportMaker rm = new ReportMaker(baseTable);
        this.jointTourFilter(rm, "Joint Tour","JointTourFreq");
        this.hhSizePartFilter(rm, "HH Size", "hhSize");
        rm.addOnes();

        String[] jtFreqFilterGroup = {"HH Size","Joint Tour"};
        rm.filter2File("ones",jtFreqFilterGroup,ResourceUtil.getProperty(rb,"jtFreq"));

        //JT Composition
        baseTable = TableDataSetLoader.loadTableDataSet(rb, "joint.tour.comp.output.file");
        rm = new ReportMaker(baseTable);
        this.tourFilter(rm, "Tour", "tourType");
        this.hhSizePartFilter(rm, "HH Size", "hhSize");
        this.tourCompFilter(rm, "Tour Comp", "joint_tour_comp");
        rm.addOnes();

        String[] compFilterGroup = {"HH Size","Tour Comp","Tour"};
        rm.filter2File("ones",compFilterGroup,ResourceUtil.getProperty(rb,"jtComp"));

        //JT Participation
        baseTable = TableDataSetLoader.loadTableDataSet(rb, "joint.tour.participation.output.file");
        rm = new ReportMaker(baseTable);

        //calculate part coloumn
        float[] part = new float[baseTable.getRowCount()];
        for (int i = 1; i <= baseTable.getRowCount(); i++) {
            part[i-1] = baseTable.getValueAt(i,"tourType") * (2.0f - baseTable.getValueAt(i,"participation"));
        }
        baseTable.appendColumn(part,"part");

        this.personFilter(rm, "Person", "personType");
        this.tourPartFilter(rm, "Participation", "part");
        this.hhSizePartFilter(rm, "Participation Size", "hhSize");
        rm.addOnes();

        String[] partFilterGroup = {"Participation Size","Participation","Person"};
        rm.filter2File("ones",partFilterGroup,ResourceUtil.getProperty(rb,"jtPart"));

         //Combine files
         String[] fileList = {rb.getString("jtFreq"),
                 rb.getString("jtComp"),
                 rb.getString("jtPart")};
         this.textCombiner(fileList, rb.getString("jtFull"));
    }

    public void jointDTMReport() {
        TableDataSet baseTable = TableDataSetLoader.loadTableDataSet(rb, "joint_dtm.choice.output.file");
        addDistanceAndTimeToTable(baseTable);

        ReportMaker rm = new ReportMaker(baseTable);
        rm.addOnes();
        this.countyFilter(rm, "OCounty","origTaz");
        rm.copyFilter("DCounty","OCounty","destTaz");
        this.intextFilter(rm, "DIntExt","destTaz");
        this.extFilter(rm, "DExtDistrict","destTaz");
        this.modeFilter(rm, "Mode Choice", "MC");
        this.tourFilter(rm, "Purpose", "purpose");
        this.distanceFilter(rm, "Distance", "distance");
        this.timeFilter(rm, "DepTime", "TOD_StartHr");
        rm.copyFilter("ArrTime","DepTime","TOD_EndHr");
        this.durationFilter(rm, "Duration", "duration");

        String[] filterGroup = {"Purpose","DIntExt","OCounty","Mode Choice"};
        rm.filter2File("ones",filterGroup,ResourceUtil.getProperty(rb,"mcOtaz"));

        String[] filterGroup2 = {"Purpose","DIntExt","DCounty","Mode Choice"};
        rm.filter2File("ones",filterGroup2,ResourceUtil.getProperty(rb,"mcDtaz"));

        String[] filterGroupDCDist = {"Purpose","DIntExt","Distance","OCounty"};
        rm.filter2File("ones",filterGroupDCDist,ResourceUtil.getProperty(rb,"dcDist"));

        String[] filterGroupDCCounty = {"Purpose","OCounty","DCounty"};
        rm.filter2File("ones",filterGroupDCCounty,ResourceUtil.getProperty(rb,"dcCounty"));

        String[] filterGroupDCExt = {"Purpose","OCounty","DExtDistrict"};
        rm.filter2File("ones",filterGroupDCExt,ResourceUtil.getProperty(rb,"dcExt"));

        String[] filterGroupTodDep = {"DepTime","Purpose"};
        rm.filter2File("ones",filterGroupTodDep,ResourceUtil.getProperty(rb,"todDep"));

        String[] filterGroupTodArr = {"ArrTime","Purpose"};
        rm.filter2File("ones",filterGroupTodArr,ResourceUtil.getProperty(rb,"todArr"));

        String[] filterGroupTodDur = {"Duration","Purpose"};
        rm.filter2File("ones",filterGroupTodDur,ResourceUtil.getProperty(rb,"todDur"));

         //Combine files
         String[] fileList = {rb.getString("mcOtaz"),
                 rb.getString("mcDtaz"),
                 rb.getString("dcDist"),
                 rb.getString("dcCounty"),
                 rb.getString("dcExt"),
                 rb.getString("todDep"),
                 rb.getString("todArr"),
                 rb.getString("todDur")};
         this.textCombiner(fileList, rb.getString("jointDTMFull"));

        //Summaries
        String[] filterGroupTest = {"Purpose","DIntExt"};
        rm.filterSummary("distances",filterGroupTest);
        rm.filterSummary("travelTime",filterGroupTest);


    }

     public void indTourReport() {
         //Ind Maint Frequency
         TableDataSet baseTable = TableDataSetLoader.loadTableDataSet(rb, "indiv.non.mandatory.maintenance.frequency.output.file");
         ReportMaker rm = new ReportMaker(baseTable);
         this.indMaintFreqFilter(rm, "Maintenance Tour Freq","indiv_main_freq");
         rm.addOnes();
         this.dummyFilter(rm, "Dummy", "ones");

         String[] indMaintFreqFilterGroup = {"Maintenance Tour Freq","Dummy"};
         rm.filter2File("ones",indMaintFreqFilterGroup,ResourceUtil.getProperty(rb,"indMaintFreq"));

         //Ind Maint Allocation
         baseTable = TableDataSetLoader.loadTableDataSet(rb, "indiv.non.mandatory.maintenance.allocation.output.file");
         rm = new ReportMaker(baseTable);
         this.tourFilter(rm, "Tour", "tourType");
         this.personFilter(rm, "Person", "personType");
         this.tourPartFilter2(rm, "Participate", "participation");
         rm.addOnes();

         String[] indMaintPartFilterGroup = {"Participate","Tour","Person"};
         rm.filter2File("ones",indMaintPartFilterGroup,ResourceUtil.getProperty(rb,"indMaintPart"));

         //Ind Worker Disc Frequency
         baseTable = TableDataSetLoader.loadTableDataSet(rb, "indiv.non.mandatory.worker.disc.frequency.output.file");
         rm = new ReportMaker(baseTable);
         this.indDiscFreqFilter(rm, "Discretionary Tour Freq","indiv_disc_freq");
         rm.addOnes();
         this.dummyFilter(rm, "Dummy", "ones");

         String[] indDiscFreqFilterGroup = {"Discretionary Tour Freq","Dummy"};
         rm.filter2File("ones",indDiscFreqFilterGroup,ResourceUtil.getProperty(rb,"indDiscWFreq"));

         //Ind Nonworker Disc Frequency
         baseTable = TableDataSetLoader.loadTableDataSet(rb, "indiv.non.mandatory.nonworker.disc.frequency.output.file");
         rm = new ReportMaker(baseTable);
         this.indDiscFreqFilter(rm, "Discretionary Tour Freq","indiv_disc_freq");
         rm.addOnes();
         this.dummyFilter(rm, "Dummy", "ones");

         rm.filter2File("ones",indDiscFreqFilterGroup,ResourceUtil.getProperty(rb,"indDiscNFreq"));

         //Ind Child Disc Frequency
         baseTable = TableDataSetLoader.loadTableDataSet(rb, "indiv.non.mandatory.child.disc.frequency.output.file");
         rm = new ReportMaker(baseTable);
         this.indDiscFreqFilter(rm, "Discretionary Tour Freq","indiv_disc_freq");
         rm.addOnes();
         this.dummyFilter(rm, "Dummy", "ones");

         rm.filter2File("ones",indDiscFreqFilterGroup,ResourceUtil.getProperty(rb,"indDiscCFreq"));

         //Ind At Work Frequency
         baseTable = TableDataSetLoader.loadTableDataSet(rb, "indiv.non.mandatory.atwork.frequency.output.file");
         rm = new ReportMaker(baseTable);
         this.indAtWorkFreqFilter(rm, "At Work Tour Freq","indiv_atwork_freq");
         rm.addOnes();

         String[] indAtWorkFreqFilterGroup = {"At Work Tour Freq"};
         rm.filter2File("ones",indAtWorkFreqFilterGroup,ResourceUtil.getProperty(rb,"indAWFreq"));

         //Combine files
         String[] fileList = {rb.getString("indMaintFreq"),
                 rb.getString("indMaintPart"),
                 rb.getString("indDiscWFreq"),
                 rb.getString("indDiscNFreq"),
                 rb.getString("indDiscCFreq"),
                 rb.getString("indAWFreq")};
         this.textCombiner(fileList, rb.getString("indTourFull"));
     }

    public void indDTMReport() {
        TableDataSet baseTable = TableDataSetLoader.loadTableDataSet(rb, "non-mandatory_dtm.choice.output.file");
        addDistanceAndTimeToTable(baseTable);

        ReportMaker rm = new ReportMaker(baseTable);
        rm.addOnes();
        this.countyFilter(rm, "OCounty","origTaz");
        rm.copyFilter("DCounty","OCounty","destTaz");
        this.intextFilter(rm, "DIntExt","destTaz");
        this.extFilter(rm, "DExtDistrict","destTaz");
        this.modeFilter(rm, "Mode Choice", "MC");
        this.tourFilter(rm, "Purpose", "purpose");
        this.distanceFilter(rm, "Distance", "distance");
        this.timeFilter(rm, "DepTime", "TOD_StartHr");
        rm.copyFilter("ArrTime","DepTime","TOD_EndHr");
        this.durationFilter(rm, "Duration", "duration");

        String[] filterGroup = {"Purpose","DIntExt","OCounty","Mode Choice"};
        rm.filter2File("ones",filterGroup,ResourceUtil.getProperty(rb,"mcOtaz"));

        String[] filterGroup2 = {"Purpose","DIntExt","DCounty","Mode Choice"};
        rm.filter2File("ones",filterGroup2,ResourceUtil.getProperty(rb,"mcDtaz"));

        String[] filterGroupDCDist = {"Purpose","DIntExt","Distance","OCounty"};
        rm.filter2File("ones",filterGroupDCDist,ResourceUtil.getProperty(rb,"dcDist"));

        String[] filterGroupDCCounty = {"Purpose","OCounty","DCounty"};
        rm.filter2File("ones",filterGroupDCCounty,ResourceUtil.getProperty(rb,"dcCounty"));

        String[] filterGroupDCExt = {"Purpose","OCounty","DExtDistrict"};
        rm.filter2File("ones",filterGroupDCExt,ResourceUtil.getProperty(rb,"dcExt"));

        String[] filterGroupTodDep = {"DepTime","Purpose"};
        rm.filter2File("ones",filterGroupTodDep,ResourceUtil.getProperty(rb,"todDep"));

        String[] filterGroupTodArr = {"ArrTime","Purpose"};
        rm.filter2File("ones",filterGroupTodArr,ResourceUtil.getProperty(rb,"todArr"));

        String[] filterGroupTodDur = {"Duration","Purpose"};
        rm.filter2File("ones",filterGroupTodDur,ResourceUtil.getProperty(rb,"todDur"));

         //Combine files
         String[] fileList = {rb.getString("mcOtaz"),
                 rb.getString("mcDtaz"),
                 rb.getString("dcDist"),
                 rb.getString("dcCounty"),
                 rb.getString("dcExt"),
                 rb.getString("todDep"),
                 rb.getString("todArr"),
                 rb.getString("todDur")};
         this.textCombiner(fileList, rb.getString("indDTMFull"));

        //Summaries
        String[] filterGroupTest = {"Purpose","DIntExt"};
        rm.filterSummary("distances",filterGroupTest);
        rm.filterSummary("travelTime",filterGroupTest);

    }

    public void indAtWorkReport() {
        String awOutText = TextFile.readFrom(rb.getString("at-work_dtm.choice.output.file"));
        awOutText = awOutText.replace("hh_id,hh_taz_id,income,person_id,personType,patternType,tour_id,tourCategory,purpose,origTaz,orig_WLKseg,destTaz,dest_WLKseg,TOD,TOD_StartHr,TOD_EndHr,TOD_StartPeriod,TOD_EndPeriod,TOD_Output_StartPeriod,TOD_Output_EndPeriod,MC" + System.getProperty("line.separator"),"");
        String origIndNonMandText = TextFile.readFrom(rb.getString("non-mandatory_dtm.choice.output.file"));
        TextFile.writeTo(rb.getString("non-mandatory_dtm.choice.output.file"),origIndNonMandText + awOutText);
        indDTMReport();
        TextFile.writeTo(rb.getString("non-mandatory_dtm.choice.output.file"),origIndNonMandText);
    }

    public void externalWorkerReport() {
        TableDataSet baseTable = TableDataSetLoader.loadTableDataSet(rb, "external.worker.ot.results.file");
        baseTable.appendColumn(baseTable.getColumnAsFloat("homeTaz"),"origTaz");
        baseTable.appendColumn(baseTable.getColumnAsFloat("workTaz"),"destTaz");
        baseTable.appendColumn(baseTable.getColumnAsFloat("skimOut"),"TOD_StartHr");
        baseTable.appendColumn(baseTable.getColumnAsFloat("skimIn"),"TOD_EndHr");
        addDistanceAndTimeToTable(baseTable);
        ReportMaker rm = new ReportMaker(baseTable);
        rm.addOnes();

        this.extFilter(rm, "OZone","homeTaz");
        this.countyFilter(rm, "DCounty","workTaz");
        this.externalWorkerSkimFilter(rm,"EndSkim","skimIn");
        this.externalWorkerSkimFilter(rm,"StartSkim","skimOut");
        this.distanceFilter(rm,"Distance","distance");
        this.dummyFilter(rm, "Dummy", "ones");

        String[] opfilterGroup = {"OZone","DCounty"};
        rm.filter2File("ones",opfilterGroup,ResourceUtil.getProperty(rb,"mcOtaz"));
        String[] opfilterGroup2 = {"StartSkim","EndSkim","Dummy"};
        rm.filter2File("ones",opfilterGroup2,ResourceUtil.getProperty(rb,"mcDtaz"));
        String[] opfilterGroup3 = {"Distance","OZone"};
        rm.filter2File("ones",opfilterGroup3,ResourceUtil.getProperty(rb,"dcDist"));

        //Combine files
        String[] fileListPat = {rb.getString("mcOtaz"),
                rb.getString("mcDtaz"),
                rb.getString("dcDist")};
        this.textCombiner(fileListPat, rb.getString("externalWorkerFull"));

        //Summaries
        String[] dtmFilterGroupTest = {"OZone"};
        rm.filterSummary("distances",dtmFilterGroupTest);
        rm.filterSummary("travelTime",dtmFilterGroupTest);

    }

    public void stopsReport(String type) {
        String baseFreq = "";
        String baseLoc = "";

        if (type.equals("mand")) {
            baseFreq = "mandatory.stops.frequency.output.file";
            baseLoc = "mandatory.stops.location.output.file";
        } else if (type.equals("joint")) {
            baseFreq = "joint.stops.frequency.output.file";
            baseLoc = "joint.stops.location.output.file";
        } else if (type.equals("nonmand")) {
            baseFreq = "non-mandatory.stops.frequency.output.file";
            baseLoc = "non-mandatory.stops.location.output.file";
        }  else if (type.equals("atwork")) {
            baseFreq = "at-work.stops.frequency.output.file";
            baseLoc = "at-work.stops.location.output.file";
        }

        String outFreq = type + "StopFreq";
        String outLocOB =  type + "OutStopLoc";
        String outLocIB =  type + "InStopLoc";
        String outDistOB =  type + "OutStopDist";
        String outDistIB =  type + "InStopDist";

        //Frequency stuff
        TableDataSet baseTable = TableDataSetLoader.loadTableDataSet(rb, baseFreq);
        ReportMaker rm = new ReportMaker(baseTable);

        if (!type.equals("atwork")) {
            this.tourFilter(rm, "Purpose", "TourPurpose");
        } else {
            this.atWorkTourFilter(rm, "Purpose", "TourPurpose");
        }
        this.stopsFreqFilter(rm, "Stop Frequency", "FreqChoice");
        rm.addOnes();

        String[] stopFreqFilterGroup = {"Stop Frequency","Purpose"};
        rm.filter2File("ones",stopFreqFilterGroup,ResourceUtil.getProperty(rb,outFreq));

        //Location stuff
        baseTable = TableDataSetLoader.loadTableDataSet(rb, baseLoc);
        this.addStopDistanceStuffToTable(baseTable);
        rm = new ReportMaker(baseTable);

        this.tourFilter(rm, "Purpose", "TourPurpose");
        this.stopDummyFilter(rm, "OBStop", "OBDummy");
        this.stopDummyFilter(rm, "IBStop", "IBDummy");
        this.countyFilter(rm, "iCounty","OB_start_taz");
        this.countyFilter(rm, "jCounty","IB_start_taz");
        this.countyFilter(rm, "OutkCounty","OB_stop_taz");
        this.countyFilter(rm, "InkCounty","IB_stop_taz");
        this.stopDistanceDiffFilter(rm, "Out Dist Diff", "OBDistanceDiffClass");
        this.stopDistanceDiffFilter(rm, "In Dist Diff", "IBDistanceDiffClass");
        this.dummyFilter(rm, "Dummy", "ones");
        rm.addOnes();

        if (!type.equals("atwork")) {
            this.tourFilter(rm, "Purpose", "TourPurpose");
            String[] stopLocFilterGroupOB = {"OBStop","Purpose","iCounty","jCounty","OutkCounty"};
            rm.filter2File("ones",stopLocFilterGroupOB,ResourceUtil.getProperty(rb,outLocOB));
            String[] stopLocFilterGroupIB = {"IBStop","Purpose","iCounty","jCounty","InkCounty"};
            rm.filter2File("ones",stopLocFilterGroupIB,ResourceUtil.getProperty(rb,outLocIB));
            String[] stopDistFilterGroupOB = {"OBStop","Purpose","Out Dist Diff","Dummy"};
            rm.filter2File("ones",stopDistFilterGroupOB,ResourceUtil.getProperty(rb,outDistOB));
            String[] stopDistFilterGroupIB = {"IBStop","Purpose","In Dist Diff","Dummy"};
            rm.filter2File("ones",stopDistFilterGroupIB,ResourceUtil.getProperty(rb,outDistIB));

            //Combine files
            String[] fileList = {rb.getString(outFreq),
                 rb.getString(outLocOB),
                 rb.getString(outLocIB),
                 rb.getString(outDistOB),
                 rb.getString(outDistIB)};
            this.textCombiner(fileList, rb.getString(type + "StopsFull"));

            //Summaries
            String[] filterGroupTest = {"OBStop","Purpose"};
            rm.filterSummary("OBDistanceDiff",filterGroupTest);
            String[] filterGroupTest2 = {"IBStop","Purpose"};
            rm.filterSummary("IBDistanceDiff",filterGroupTest2);
        } else {
            this.atWorkTourFilter(rm, "Purpose", "TourPurpose");
            String[] stopDistFilterGroupOB = {"OBStop","Purpose","Out Dist Diff","Dummy"};
            rm.filter2File("ones",stopDistFilterGroupOB,ResourceUtil.getProperty(rb,outDistOB));
            String[] stopDistFilterGroupIB = {"IBStop","Purpose","In Dist Diff","Dummy"};
            rm.filter2File("ones",stopDistFilterGroupIB,ResourceUtil.getProperty(rb,outDistIB));

            //Combine files
            String[] fileList = {rb.getString(outFreq),
                 rb.getString(outDistOB),
                 rb.getString(outDistIB)};
            this.textCombiner(fileList, rb.getString(type + "StopsFull"));

            String[] filterGroupTest = {"OBStop","Purpose"};
            rm.filterSummary("OBDistanceDiff",filterGroupTest);
            String[] filterGroupTest2 = {"IBStop","Purpose"};
            rm.filterSummary("IBDistanceDiff",filterGroupTest2);
        }
    }
    
    public void visitorReport() {
        TableDataSet baseTable = TableDataSetLoader.loadTableDataSet(rb, "overnight.pattern.results.file");
        ReportMaker rm = new ReportMaker(baseTable);
        rm.addOnes();
        this.overnightVisitorPatternFilter(rm,"Pattern",VisitorDataStructure.PATTERN_FIELD);
        this.overnightVisitorNumberOfActivitiesFilter(rm,"Activities",VisitorDataStructure.PATTERN_FIELD);
        this.overnightVisitorPresenceOfActivityFilter(rm,"Recreation",VisitorDataStructure.PATTERN_FIELD, VisitorTourType.Recreation.getIDChar());
        this.overnightVisitorPresenceOfActivityFilter(rm,"Gaming",VisitorDataStructure.PATTERN_FIELD, VisitorTourType.Gaming.getIDChar());
        this.overnightVisitorPresenceOfActivityFilter(rm,"Shopping",VisitorDataStructure.PATTERN_FIELD, VisitorTourType.Shopping.getIDChar());
        this.overnightVisitorPresenceOfActivityFilter(rm,"Other",VisitorDataStructure.PATTERN_FIELD, VisitorTourType.Other.getIDChar());
        this.visitorTypeFilter(rm,"Visitor Type",VisitorDataStructure.VISITORTYPE_FIELD);
        this.dummyFilter(rm, "Dummy", "ones");

        String[] opfilterGroup = {"Visitor Type","Pattern","Dummy"};
        rm.filter2File("ones",opfilterGroup,ResourceUtil.getProperty(rb,"mcOtaz"));
        String[] opfilterGroup2 = {"Visitor Type","Activities","Dummy"};
        rm.filter2File("ones",opfilterGroup2,ResourceUtil.getProperty(rb,"mcDtaz"));
        String[] opfilterGroup3 = {"Visitor Type","Recreation","Dummy"};
        rm.filter2File("ones",opfilterGroup3,ResourceUtil.getProperty(rb,"dcDist"));
        String[] opfilterGroup4 = {"Visitor Type","Gaming","Dummy"};
        rm.filter2File("ones",opfilterGroup4,ResourceUtil.getProperty(rb,"dcCounty"));
        String[] opfilterGroup5 = {"Visitor Type","Shopping","Dummy"};
        rm.filter2File("ones",opfilterGroup5,ResourceUtil.getProperty(rb,"dcExt"));
        String[] opfilterGroup6 = {"Visitor Type","Other","Dummy"};
        rm.filter2File("ones",opfilterGroup6,ResourceUtil.getProperty(rb,"todDep"));

        baseTable = TableDataSetLoader.loadTableDataSet(rb, "day.pattern.results.file");
        rm = new ReportMaker(baseTable);
        rm.addOnes();
        this.dayVisitorPatternFilter(rm,"Pattern",VisitorDataStructure.PATTERN_FIELD);
        this.dayVisitorNumberOfActivitiesFilter(rm,"Activities",VisitorDataStructure.PATTERN_FIELD);
        this.dayVisitorPresenceOfActivityFilter(rm,"Recreation",VisitorDataStructure.PATTERN_FIELD, VisitorTourType.Recreation.getIDChar());
        this.dayVisitorPresenceOfActivityFilter(rm,"Gaming",VisitorDataStructure.PATTERN_FIELD, VisitorTourType.Gaming.getIDChar());
        this.dayVisitorPresenceOfActivityFilter(rm,"Shopping",VisitorDataStructure.PATTERN_FIELD, VisitorTourType.Shopping.getIDChar());
        this.dayVisitorPresenceOfActivityFilter(rm,"Other",VisitorDataStructure.PATTERN_FIELD, VisitorTourType.Other.getIDChar());
        this.visitorTypeFilter(rm,"Visitor Type",VisitorDataStructure.VISITORTYPE_FIELD);
        this.dummyFilter(rm, "Dummy", "ones");

        rm.filter2File("ones",opfilterGroup,ResourceUtil.getProperty(rb,"todArr"));
        rm.filter2File("ones",opfilterGroup2,ResourceUtil.getProperty(rb,"todDur"));
        rm.filter2File("ones",opfilterGroup3,ResourceUtil.getProperty(rb,"jtComp"));
        rm.filter2File("ones",opfilterGroup4,ResourceUtil.getProperty(rb,"jtPart"));
        rm.filter2File("ones",opfilterGroup5,ResourceUtil.getProperty(rb,"jtFreq"));
        rm.filter2File("ones",opfilterGroup6,ResourceUtil.getProperty(rb,"indMaintFreq"));

        //Combine files
        String[] fileListPat = {rb.getString("mcOtaz"),
                rb.getString("mcDtaz"),
                rb.getString("dcDist"),
                rb.getString("dcCounty"),
                rb.getString("dcExt"),
                rb.getString("todDep"),
                rb.getString("todArr"),
                rb.getString("todDur"),
                rb.getString("jtComp"),
                rb.getString("jtPart"),
                rb.getString("jtFreq"),
                rb.getString("indMaintFreq")};
        this.textCombiner(fileListPat, rb.getString("visitorPatternFull"));

        baseTable = TableDataSetLoader.loadTableDataSet(rb, "visitor.reports.data.file");
        addDistanceAndTimeToTable(baseTable);
        addStopDistanceStuffToTable(baseTable);

        rm = new ReportMaker(baseTable);
        rm.addOnes();
        //DTM and etc. filters
        this.countyFilter(rm, "OCounty",VisitorDataStructure.ORIG_REPORTS_FIELD);
        rm.copyFilter("DCounty","OCounty",VisitorDataStructure.DEST_REPORTS_FIELD);
        this.intextFilter(rm, "DIntExt",VisitorDataStructure.DEST_REPORTS_FIELD);
        this.extFilter(rm, "DExtDistrict",VisitorDataStructure.DEST_REPORTS_FIELD);
        rm.copyFilter("OExtDistrict","DExtDistrict",VisitorDataStructure.ORIG_REPORTS_FIELD);
        this.visitorModeFilter(rm, "Mode Choice", VisitorDataStructure.MODE_FIELD);
        this.visitorTourTypeFilter(rm, "Purpose", VisitorDataStructure.TOURTYPE_REPORTS_FIELD);
        this.distanceFilter(rm, "Distance", "distance");
        this.timeFilter(rm, "DepTime", VisitorDataStructure.DEPHR_REPORTS_FIELD);
        rm.copyFilter("ArrTime","DepTime",VisitorDataStructure.ARRHR_REPORTS_FIELD);
        this.durationFilter(rm, "Duration", "duration");
        this.visitorTypeFilter(rm,"Visitor Type",VisitorDataStructure.VISITORTYPE_FIELD);
        //Stops filters
        this.stopDummyFilter(rm, "OBStop", "OBDummy");
        this.stopDummyFilter(rm, "IBStop", "IBDummy");
        this.countyFilter(rm, "iCounty",VisitorDataStructure.OBSTART_REPORTS_FIELD);
        this.countyFilter(rm, "jCounty",VisitorDataStructure.IBSTART_REPORTS_FIELD);
        this.countyFilter(rm, "OutkCounty",VisitorDataStructure.OBSTOP_REPORTS_FIELD);
        this.countyFilter(rm, "InkCounty",VisitorDataStructure.IBSTOP_REPORTS_FIELD);
        this.stopDistanceDiffFilter(rm, "Out Dist Diff", "OBDistanceDiffClass");
        this.stopDistanceDiffFilter(rm, "In Dist Diff", "IBDistanceDiffClass");
        this.dummyFilter(rm, "Dummy", "ones");

        String[] filterGroup = {"Visitor Type","Purpose","DIntExt","OCounty","Mode Choice"};
        rm.filter2File("ones",filterGroup,ResourceUtil.getProperty(rb,"mcOtaz"));

        String[] filterGroup2 = {"Visitor Type","Purpose","DIntExt","DCounty","Mode Choice"};
        rm.filter2File("ones",filterGroup2,ResourceUtil.getProperty(rb,"mcDtaz"));

        String[] filterGroupDCDist = {"Visitor Type","Purpose","DIntExt","Distance","OCounty"};
        rm.filter2File("ones",filterGroupDCDist,ResourceUtil.getProperty(rb,"dcDist"));

        String[] filterGroupDCCounty = {"Visitor Type","Purpose","OCounty","DCounty"};
        rm.filter2File("ones",filterGroupDCCounty,ResourceUtil.getProperty(rb,"dcCounty"));

        String[] filterGroupDCExt = {"Visitor Type","Purpose","OCounty","DExtDistrict"};
        rm.filter2File("ones",filterGroupDCExt,ResourceUtil.getProperty(rb,"dcExt"));

        String[] filterGroupTodDep = {"Visitor Type","DepTime","Purpose"};
        rm.filter2File("ones",filterGroupTodDep,ResourceUtil.getProperty(rb,"todDep"));

        String[] filterGroupTodArr = {"Visitor Type","ArrTime","Purpose"};
        rm.filter2File("ones",filterGroupTodArr,ResourceUtil.getProperty(rb,"todArr"));

        String[] filterGroupTodDur = {"Visitor Type","Duration","Purpose"};
        rm.filter2File("ones",filterGroupTodDur,ResourceUtil.getProperty(rb,"todDur"));

        String[] filterGroupThru = {"Visitor Type","OExtDistrict","DExtDistrict"};
        rm.filter2File("ones",filterGroupThru,ResourceUtil.getProperty(rb,"jtComp"));

        //Combine files
        String[] fileList = {rb.getString("mcOtaz"),
                rb.getString("mcDtaz"),
                rb.getString("dcDist"),
                rb.getString("dcCounty"),
                rb.getString("dcExt"),
                rb.getString("todDep"),
                rb.getString("todArr"),
                rb.getString("todDur"),
                rb.getString("jtComp")};
        this.textCombiner(fileList, rb.getString("visitorDTMFull"));

        //Stops
        String type = "joint";
        String outLocOB =  type + "OutStopLoc";
        String outLocIB =  type + "InStopLoc";
        String outDistOB =  type + "OutStopDist";
        String outDistIB =  type + "InStopDist";
        //String[] stopLocFilterGroupOB = {"Visitor Type","OBStop","Purpose","iCounty","jCounty","OutkCounty"};
        String[] stopLocFilterGroupOB = {"Visitor Type","OBStop","Purpose","iCounty","jCounty","OutkCounty"};
        rm.filter2File("ones",stopLocFilterGroupOB,ResourceUtil.getProperty(rb,outLocOB));
        String[] stopLocFilterGroupIB = {"Visitor Type","IBStop","Purpose","iCounty","jCounty","InkCounty"};
        rm.filter2File("ones",stopLocFilterGroupIB,ResourceUtil.getProperty(rb,outLocIB));
        String[] stopDistFilterGroupOB = {"Visitor Type","OBStop","Purpose","Out Dist Diff","Dummy"};
        rm.filter2File("ones",stopDistFilterGroupOB,ResourceUtil.getProperty(rb,outDistOB));
        String[] stopDistFilterGroupIB = {"Visitor Type","IBStop","Purpose","In Dist Diff","Dummy"};
        rm.filter2File("ones",stopDistFilterGroupIB,ResourceUtil.getProperty(rb,outDistIB));

        //Combine files
        String[] fileList2 = {rb.getString(outLocOB),
             rb.getString(outLocIB),
             rb.getString(outDistOB),
             rb.getString(outDistIB)};
        this.textCombiner(fileList2, rb.getString("visitorStopsFull"));



        //Summaries
        String[] dtmFilterGroupTest = {"Visitor Type","Purpose","DIntExt"};
        rm.filterSummary("distances",dtmFilterGroupTest);
        rm.filterSummary("travelTime",dtmFilterGroupTest);
        //String[] stopFilterGroupTest = {"Visitor Type","OBStop","Purpose"};
        String[] stopFilterGroupTest = {"Visitor Type","OBStop"};
        rm.filterSummary("OBDistanceDiff",stopFilterGroupTest);
        //String[] stopFilterGroupTest2 = {"Visitor Type","IBStop","Purpose"};
        String[] stopFilterGroupTest2 = {"Visitor Type","IBStop"};
        rm.filterSummary("IBDistanceDiff",stopFilterGroupTest2);
    }

    public static void main(String[] args) {
        TahoeReports tr = new TahoeReports();
        //tr.stopsReport("nonmand");
        //tr.mandDTMReport("Work");
        //tr.mandDTMReport("School");
        //tr.jointTourReport();
        //tr.jointDTMReport();
        //tr.indDTMReport();
        //tr.indAtWorkReport();
        tr.mandReport();
        //tr.visitorReport();
        //tr.externalWorkerReport();
    }
}
