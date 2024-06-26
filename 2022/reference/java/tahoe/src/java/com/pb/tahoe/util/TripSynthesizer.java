package com.pb.tahoe.util;

import com.pb.common.util.ResourceUtil;
import com.pb.common.matrix.Matrix;
import com.pb.common.datafile.TextFile;
import com.pb.tahoe.structures.Tour;
import com.pb.tahoe.structures.Household;
import com.pb.tahoe.visitor.structures.*;
import com.pb.tahoe.visitor.PartyArrayManager;
import com.pb.tahoe.ExternalWorkers.ExternalWorker;
import com.pb.tahoe.ExternalWorkers.ExternalWorkerArrayManager;

import java.util.ResourceBundle;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.ArrayList;
import java.io.*;

import org.apache.log4j.Logger;

/**
 * User: Chris
 * Date: Mar 7, 2007 - 2:25:12 PM
 */
public class TripSynthesizer {

    static ResourceBundle rb = ResourceUtil.getResourceBundle("tahoe");
    static Logger logger = Logger.getLogger(TripSynthesizer.class);

    static ZonalDataManager zdm = ZonalDataManager.getInstance();
    static int[] tazList = null;
    static int numberOfTAZs = zdm.getNumberOfZones();

    //this will hold all of the trip matrices we'll need
    private HashMap<String,Matrix> trips = new HashMap<String,Matrix>();

    //This indicates that a trip file should be built concurrently with the trip tables
    private boolean buildTripFile = true;
    private int tripCount = 1;
    private int tourCount = 1;
    private String tripFile = rb.getString("trip.file");

    private StringBuffer tripFileText;

    public TripSynthesizer() {
        if (buildTripFile) {
            initializeTripFile();
        }
    }

    public void setBuildTripFile(boolean buildTripFile) {
        this.buildTripFile = buildTripFile;
    }

    public void setTripFile(String tripFile) {
        this.tripFile = tripFile;
    }

    public void synthesizeResidentTrips(HouseholdArrayManager ham) {
        for(Household hh : ham.getHouseholds()) {
            if(hh == null) continue;
            synthesizeHHTrips(hh);
        }
    }

    public void synthesizeVisitorTrips(PartyArrayManager pam) {
        for (TravelParty tp : pam.parties) {
            if (tp == null) continue;
            for (VisitorTour tour : tp.getTours()) {
                if (tp.getPattern() != -1)
                    //doesn't get all walk trips for drive tours with stops, it should get all transit trips correctly
                    if (tour.getMode().getId() == 1) {
                        addVisitorTourTrips(tour, tp);
                    } else {
                        for (int i = 1; i <= tp.getPersons(); i++) {
                            addVisitorTourTrips(tour, tp);
                        }
                    }
                else
                    addThruVisitorTourTrips(tour, tp);
            }
        }
    }

    public void synthesizeExternalWorkerTrips(ExternalWorkerArrayManager ewam) {
        for (ExternalWorker ew : ewam.workers) {
            if (ew == null) continue;
            addExternalWorkerTour(ew);
        }
    }

    public void writeAllSkimMatrices(String path, String fileNameStart) {
        int maxCases = 10;
        LinkedHashSet<String> skims = new LinkedHashSet<String>();
        for (int i = 1; i <= maxCases; i++) {
            skims.add(getSkimPeriodName(i));
        }

        //create filename array
        String[] files = new String[skims.size()];
        int counter = 0;
        for (String skim : skims) {
            files[counter++] = fileNameStart + skim + ".csv";
        }

        //create matrix tree
        String[][] matrixTree = new String[skims.size()][];
        String[][] headings = new String[skims.size()][];
        counter = 0;
        for (String skim : skims) {
            ArrayList<String> matrixBranch = new ArrayList<String>();
            ArrayList<String> headingsBranch = new ArrayList<String>();
            for (String key : trips.keySet()) {
                if (key.substring(key.length() - 2).equals(skim)) {
                    matrixBranch.add(key);
                    headingsBranch.add(key.substring(0,key.length() - 2));
                }
            matrixTree[counter] = (String[]) matrixBranch.toArray();
            headings[counter++] = (String[]) headingsBranch.toArray();
            }
        }

        //write files
        writeMatrices(path, files, matrixTree, headings, trips);
    }

     public void writeModeSkimMatrices(String path, String fileNameStart) {
        int maxCases = 10;
        char[] levels = {'r','o'};
        LinkedHashSet<String> modes = new LinkedHashSet<String>();
        LinkedHashSet<String> skims = new LinkedHashSet<String>();
        for (int i = 1; i <= maxCases; i++) {
            for (char level : levels) {
                modes.add(getModeName(i,level));
            }
            skims.add(getSkimPeriodName(i));
        }

        //Create condensed trips map
        HashMap<String,Matrix> condensedTrips = condenseMatricesToModeSkim();

        //create filename array
        String[] files = new String[skims.size()];
        int counter = 0;
        for (String skim : skims) {
            files[counter++] = fileNameStart + skim + ".csv";
        }

        //create matrix tree
        String[][] matrixTree = new String[skims.size()][];
        String[][] headings = new String[skims.size()][];
        counter = 0;
        for (String skim : skims) {
            ArrayList<String> matrixBranch = new ArrayList<String>();
            ArrayList<String> headingsBranch = new ArrayList<String>();
            for (String mode : modes) {
                //make a dummy (empty) matrix if one doesn't exist
                if (!condensedTrips.containsKey(mode + "_" + skim)) {
                    Matrix m = (Matrix) condensedTrips.get(condensedTrips.keySet().toArray()[0]).clone();
                    m.fill(0.0f);
                    condensedTrips.put(mode + "_" + skim,m);
                }
                matrixBranch.add(mode + "_" + skim);
                headingsBranch.add(mode);
            }
            matrixTree[counter] = matrixBranch.toArray(new String[matrixBranch.size()]);
            headings[counter++] = headingsBranch.toArray(new String[headingsBranch.size()]);
        }

        //write files
        writeMatrices(path, files, matrixTree, headings, condensedTrips);
    }

    public void writeTripFile() {
        if (buildTripFile)
            TextFile.writeTo(tripFile,tripFileText.toString());
    }

    private void initializeTripFile() {
        /*
        Trip file column list:
        tripId

         */
        String[] columnList = {
                "tripID",
                "tourID",
                "partyTypeID",
                "partyType",
                "partyID",
                "persons",
                "personList",
                "tripTypeID",
                "tripType",
                "leg",
                "startTaz",
                "endTaz",
                "time",
                "skim",
                "mode"
        };
        tripFileText = new StringBuffer(columnList[0]);
        for (int i = 1; i < columnList.length; i++)
            tripFileText.append(",").append(columnList[i]);
        tripFileText.append("\n");
    }

    private void initTazList() {
        tazList = new int[numberOfTAZs + 1];
        int counter = 0;
        for(int i : zdm.getZonalTableDataSet().getColumnAsInt("taz")) {
            if(counter++==0) {
                tazList[0] = 0;
            }
            tazList[counter] = i;
        }
    }

    private void addMatrix(String matrixName) {
        if (tazList == null)
            initTazList();
        Matrix tripMatrix = new Matrix(numberOfTAZs,numberOfTAZs);
        tripMatrix.setExternalNumbers(tazList);
        tripMatrix.fill(0.0f);
        trips.put(matrixName,tripMatrix);
    }

    private void addTrip(String matrixName, int fromTaz, int toTaz) {
        if (!trips.containsKey(matrixName))
            addMatrix(matrixName);
        Matrix tripMatrix = trips.get(matrixName);
        tripMatrix.setValueAt(fromTaz, toTaz, tripMatrix.getValueAt(fromTaz, toTaz) + 1.0f);
    }

    private void writeMatrices(String path, String[] files, String[][] matrixTree, String[][] headings, HashMap<String,Matrix> matrixSet) {
        assert files.length == matrixTree.length;
        assert files.length == headings.length;
        for (int i = 0; i < files.length; i++) {
            assert headings[i].length == matrixTree[i].length;
            Matrix[] matrixArray = new Matrix[matrixTree[i].length];
            for (int j = 0; j < matrixTree[i].length; j++) {
                matrixArray[j] = matrixSet.get(matrixTree[i][j]);
            }
            writeMatrices(path + files[i], matrixArray, headings[i]);
        }
    }

    private void writeMatrices(String fileName, Matrix[] matrixArray, String[] headings) {
        if (matrixArray.length == 0) return;
        assert matrixArray.length + 2 == headings.length;
        PrintWriter outStream = null;
        try {
            outStream = new PrintWriter(
                    new BufferedWriter(new FileWriter(new File(fileName))));
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
        //print headings
        String heading = "i,j";
        for (String head : headings)
            heading += "," + head;
        outStream.println(heading);
        //determine matrix index map
        int[] matrixIndex = matrixArray[0].getExternalColumnNumbers();
        int[] matrixIndix = matrixArray[0].getInternalColumnNumbers();
        int counter = 1;
        for (int i = 1; i < matrixIndix.length; i++) {
            if (matrixIndix[i] != -1) matrixIndex[counter++] = i;
        }
        //print matrices
        logger.info("Writing matrix: " + fileName);
        for (int i = 0; i < (matrixIndex.length - 1); i++) {
            for (int j = 0; j < (matrixIndex.length - 1); j++) {
                int itaz = matrixIndex[i+1];
                int jtaz = matrixIndex[j+1];
                String outString = itaz + "," + jtaz;
                for (Matrix m : matrixArray) {
                    outString += "," + m.getValueAt(itaz,jtaz);
                }
                outStream.println(outString);
            }
            outStream.flush();
        }
    }

    private String getSkimClassName(char level, int tourType, int mode, int skim, String tourClass) {
        return tourClass + getTourTypeName(tourType,level) + "_" + getModeName(mode,level) + "_" + getSkimPeriodName(skim);
    }

    /*
    private String getSkimPeriodName2(int skimPeriod) {
        switch(skimPeriod) {
            case 1 : return "LN";
            case 2 : return "AM";
            case 3 : return "MD";
            case 4 : return "MD";
            case 5 : return "PM";
            default : return "LN";
        }
    }
    */

    private String getSkimPeriodName(int skimPeriod) {
        assert (skimPeriod > 0 && skimPeriod < 5);
        switch (skimPeriod) {
            case 1 : return "AM";
            case 2 : return "PM";
            case 3 : return "MD";
            default : return "LN";
        }
    }

    private String getModeName(int mode, char level) {
        switch (level) {
            case 'r' : return getResidentModeName(mode);
            case 'o' : return getVisitorModeName(mode);
            case 'e' : return getExternalWorkerModeName();
            case 't' : return getThruTripModeName();
            default : return "";
        }
    }

    private String getTourTypeName(int tourType, char level) {
        switch (level) {
            case 'r' : return getResidentTourTypeName(tourType);
            case 'o' : return getVisitorTourTypeName(tourType);
            case 'e' : return getExternalWorkerTourTypeName();
            case 't' : return getThruTripTourTypeName();
            default : return "";
        }
    }

    private String getVisitorModeName(int mode) {
        switch (mode) {
            case 1 : return "SA";
            case 2 : return "SH";
            case 3 : return "WT";
            case 4 : return "DT";
            case 5 : return "NM";
            default : return "DA";
        }
    }

    private String getResidentModeName(int mode) {
        switch (mode) {
            case 1 : return "DA";
            case 2 : return "SA";
            case 3 : return "WT";
            case 4 : return "DT";
            case 5 : return "NM";
            case 6 : return "SB";
            default : return "WT";
        }
    }

    private String getExternalWorkerModeName() {
        return "DA";
    }

    private String getThruTripModeName() {
        return "DA";
    }

    private String getLongModeName(String mode) {
        if (mode.equals("DA"))
            return "drive alone";
        else if (mode.equals("SA"))
            return "shared auto";
        else if (mode.equals("WT"))
            return "walk to transit";
        else if (mode.equals("DT"))
            return "drive to transit";
        else if (mode.equals("NM"))
            return "non motorized";
        else if (mode.equals("SB"))
            return "school bus";
        else if (mode.equals("SH"))
            return "visitor shuttle";
        else
            return "unknown mode";
    }

    private String getVisitorTourTypeName(int tourType) {
        return VisitorTourType.getTourType(tourType).toString();
    }

    private String getResidentTourTypeName(int tourType) {
        switch (tourType) {
            case 1 : return "Work";
            case 2 : return "School";
            case 3 : return "Escort";
            case 4 : return "Shop";
            case 5 : return "Maintenance";
            case 6 : return "Discretionary";
            case 7 : return "Eat";
            case 9 : return "AtWorkRelated";
            case 10 : return "AtWorkEat";
            default : return "AtWorkOther";
        }
    }

    private String getExternalWorkerTourTypeName() {
        return "ExternalWork";
    }

    private String getThruTripTourTypeName() {
        return "Thru";
    }

    private void synthesizeHHTrips(Household hh) {
        if(hh.mandatoryTours != null) {
            for(Tour t : hh.mandatoryTours) {
                addResidentTourTrips(t, hh, "R_");
                //Do at work tours
                if(t.getSubTours() != null) {
                    for(Tour st : t.getSubTours()) {
                        addResidentTourTrips(st, hh, "R_");
                    }
                }
            }
        }
        if(hh.jointTours != null) {
            for(Tour t : hh.jointTours) {
                addResidentTourTrips(t, hh, "R_Joint");
            }
        }
        if(hh.indivTours != null) {
            for(Tour t : hh.indivTours) {
                addResidentTourTrips(t, hh, "R_Ind");
            }
        }
    }

    private void addResidentTourTrips(Tour t, Household hh, String tourClass) {
        int tourType = t.getTourType();
        if (tourType == 8)
            tourType += t.getSubTourType();
        int skimOut = TODDataManager.getTodStartSkimPeriod(t.getTimeOfDayAlt());
        int skimIn = TODDataManager.getTodEndSkimPeriod(t.getTimeOfDayAlt());
        int i = t.getOrigTaz();
        int j = t.getDestTaz();
        int ko = t.getStopLocOB();
        int ki = t.getStopLocIB();
        String tripBase = "";
        if (buildTripFile) {
            StringBuffer tripBaseBuffer = new StringBuffer();
            tripBaseBuffer.append(tourCount++).append(",");
            tripBaseBuffer.append(0).append(",");
            tripBaseBuffer.append("resident").append(",");
            tripBaseBuffer.append(hh.getID()).append(",");
            if (t.getNumPersons() != 0)
                tripBaseBuffer.append(t.getNumPersons()).append(",");
            else
                tripBaseBuffer.append("1,");
            for (int p = 1; p <= hh.getHHSize(); p++) {
                if (t.getPersonParticipation(p)) {
                    tripBaseBuffer.append(p).append(" ");
                }
            }
            tripBaseBuffer.setLength(tripBaseBuffer.length() - 1);
            tripBaseBuffer.append(",");
            tripBaseBuffer.append(t.getTourType()).append(",");
            tripBaseBuffer.append(getResidentTourTypeName(t.getTourType())).append(",");
            tripBase = tripBaseBuffer.toString();
        }
        if (ko != 0) {
            addTrip(getSkimClassName('r',tourType, t.getTripIkMode(),skimOut,tourClass),i,ko);
            addTrip(getSkimClassName('r',tourType, t.getTripKjMode(),skimOut,tourClass),ko,j);
            if (buildTripFile) {
                tripFileText.append(tripCount++).append(",").append(tripBase).append("is").append(",").
                        append(i).append(",").
                        append(ko).append(",").
                        append(TODDataManager.getTodStartHour(t.getTimeOfDayAlt())).append(",").
                        append(skimOut).append(",").
                        append(getLongModeName(getModeName(t.getTripIkMode(),'r'))).append("\n");
                tripFileText.append(tripCount++).append(",").append(tripBase).append("sj").append(",").
                        append(ko).append(",").
                        append(j).append(",").
                        append(TODDataManager.getTodStartHour(t.getTimeOfDayAlt())).append(",").
                        append(skimOut).append(",").
                        append(getLongModeName(getModeName(t.getTripKjMode(),'r'))).append("\n");
            }
        } else {
            addTrip(getSkimClassName('r',tourType, t.getMode(),skimOut,tourClass),i,j);
            if (buildTripFile) {
                tripFileText.append(tripCount++).append(",").append(tripBase).append("ij").append(",").
                        append(i).append(",").
                        append(j).append(",").
                        append(TODDataManager.getTodStartHour(t.getTimeOfDayAlt())).append(",").
                        append(skimOut).append(",").
                        append(getLongModeName(getModeName(t.getMode(),'r'))).append("\n");
            }
        }
        if (ki != 0) {
            addTrip(getSkimClassName('r',tourType, t.getTripJkMode(),skimIn,tourClass),j,ki);
            addTrip(getSkimClassName('r',tourType, t.getTripKiMode(),skimIn,tourClass),ki,i);
            if (buildTripFile) {
                tripFileText.append(tripCount++).append(",").append(tripBase).append("js").append(",").
                        append(j).append(",").
                        append(ki).append(",").
                        append(TODDataManager.getTodEndHour(t.getTimeOfDayAlt())).append(",").
                        append(skimIn).append(",").
                        append(getLongModeName(getModeName(t.getTripJkMode(),'r'))).append("\n");
                tripFileText.append(tripCount++).append(",").append(tripBase).append("si").append(",").
                        append(ki).append(",").
                        append(i).append(",").
                        append(TODDataManager.getTodEndHour(t.getTimeOfDayAlt())).append(",").
                        append(skimOut).append(",").
                        append(getLongModeName(getModeName(t.getTripKiMode(),'r'))).append("\n");
            }
        } else {
            addTrip(getSkimClassName('r',tourType, t.getMode(),skimIn,tourClass),j,i);
            if (buildTripFile) {
                tripFileText.append(tripCount++).append(",").append(tripBase).append("ji").append(",").
                        append(j).append(",").
                        append(i).append(",").
                        append(TODDataManager.getTodEndHour(t.getTimeOfDayAlt())).append(",").
                        append(skimIn).append(",").
                        append(getLongModeName(getModeName(t.getMode(),'r'))).append("\n");
            }
        }
    }

    private void addVisitorTourTrips(VisitorTour t, TravelParty tp) {
        int tourType = t.getTourType().getID();
        int skimOut = TODDataManager.getTodStartSkimPeriod(t.getTimeOfDayAlt());
        int skimIn = TODDataManager.getTodEndSkimPeriod(t.getTimeOfDayAlt());
        int i = tp.getTazID();
        int j = t.getDestTAZ();
        int mode = t.getMode().getId();
        //for drive alone
        if (tp.getPersons() == 1 && mode == 1)
            mode = 99;
        String tripBase = "";
        if (buildTripFile) {
            StringBuffer tripBaseBuffer = new StringBuffer();
            tripBaseBuffer.append(tourCount++).append(",");
            tripBaseBuffer.append(tp.getVisitorType()).append(",");
            tripBaseBuffer.append(VisitorType.getVisitorType(tp.getVisitorType()).toString().toLowerCase()).append(" visitor,");
            tripBaseBuffer.append(tp.getID()).append(",");
            tripBaseBuffer.append(tp.getPersons()).append(",");
            for (int p = 1; p <= tp.getPersons(); p++) {
                if (p==1)
                    tripBaseBuffer.append(p);
                else
                    tripBaseBuffer.append(" ").append(p);
            }
            tripBaseBuffer.append(",");
            tripBaseBuffer.append(t.getTourType().getID()).append(",");
            tripBaseBuffer.append(t.getTourType()).append(",");
            tripBase = tripBaseBuffer.toString();
        }
        if (t.getOutboundStop()) {
            int ko = t.getObTAZ();
            int outMode = t.getObMode().getId();
            int ikMode;
            int kjMode;
            if (outMode == 0 || outMode == 2) {
                addTrip(getSkimClassName('o',tourType, mode,skimOut,"OV_"),i,ko);
                ikMode = mode;
            } else {
                addTrip(getSkimClassName('o',tourType, VisitorMode.NonMotorized.getId(),skimOut,"OV_"),i,ko);
                ikMode = VisitorMode.NonMotorized.getId();
            }
            if (outMode == 0 || outMode == 1) {
                addTrip(getSkimClassName('o',tourType, mode,skimOut,"OV_"),ko,j);
                kjMode = mode;
            } else {
                addTrip(getSkimClassName('o',tourType, VisitorMode.NonMotorized.getId(),skimOut,"OV_"),ko,j);
                kjMode = VisitorMode.NonMotorized.getId();
            }
            if (buildTripFile) {
               tripFileText.append(tripCount++).append(",").append(tripBase).append("is").append(",").
                        append(i).append(",").
                        append(ko).append(",").
                        append(TODDataManager.getTodEndHour(t.getTimeOfDayAlt())).append(",").
                        append(skimIn).append(",").
                        append(getLongModeName(getModeName(ikMode,'o'))).append("\n");
                tripFileText.append(tripCount++).append(",").append(tripBase).append("sj").append(",").
                        append(ko).append(",").
                        append(j).append(",").
                        append(TODDataManager.getTodEndHour(t.getTimeOfDayAlt())).append(",").
                        append(skimOut).append(",").
                        append(getLongModeName(getModeName(kjMode,'o'))).append("\n");
            }
        } else {
            addTrip(getSkimClassName('o',tourType, mode,skimOut,"OV_"),i,j);
            if (buildTripFile) {
                tripFileText.append(tripCount++).append(",").append(tripBase).append("ij").append(",").
                        append(i).append(",").
                        append(j).append(",").
                        append(TODDataManager.getTodEndHour(t.getTimeOfDayAlt())).append(",").
                        append(skimIn).append(",").
                        append(getLongModeName(getModeName(mode,'o'))).append("\n");
            }
        }
        if (t.getInboundStop()) {
            int ki = t.getIbTAZ();
            int inMode = t.getIbMode().getId();
            int jkMode;
            int kiMode;
            if (inMode == 0 || inMode == 2) {
                addTrip(getSkimClassName('o',tourType, mode,skimIn,"OV_"),j,ki);
                jkMode = mode;
            } else {
                addTrip(getSkimClassName('o',tourType, VisitorMode.NonMotorized.getId(),skimIn,"OV_"),j,ki);
                jkMode = VisitorMode.NonMotorized.getId();
            }
            if (inMode == 0 || inMode == 1) {
                addTrip(getSkimClassName('o',tourType, mode,skimIn,"OV_"),ki,i);
                kiMode = mode;
            } else {
                addTrip(getSkimClassName('o',tourType, VisitorMode.NonMotorized.getId(),skimIn,"OV_"),ki,i);
                kiMode = VisitorMode.NonMotorized.getId();
            }
            if (buildTripFile) {
                tripFileText.append(tripCount++).append(",").append(tripBase).append("js").append(",").
                        append(j).append(",").
                        append(ki).append(",").
                        append(TODDataManager.getTodEndHour(t.getTimeOfDayAlt())).append(",").
                        append(skimIn).append(",").
                        append(getLongModeName(getModeName(jkMode,'o'))).append("\n");
                tripFileText.append(tripCount++).append(",").append(tripBase).append("si").append(",").
                        append(ki).append(",").
                        append(i).append(",").
                        append(TODDataManager.getTodEndHour(t.getTimeOfDayAlt())).append(",").
                        append(skimOut).append(",").
                        append(getLongModeName(getModeName(kiMode,'o'))).append("\n");
            }
        } else {
            addTrip(getSkimClassName('o',tourType, mode,skimIn,"OV_"),j,i);
            if (buildTripFile) {
                tripFileText.append(tripCount++).append(",").append(tripBase).append("ji").append(",").
                        append(i).append(",").
                        append(j).append(",").
                        append(TODDataManager.getTodEndHour(t.getTimeOfDayAlt())).append(",").
                        append(skimIn).append(",").
                        append(getLongModeName(getModeName(mode,'o'))).append("\n");
            }
        }
    }

    private void addThruVisitorTourTrips(VisitorTour tour, TravelParty tp) {
        addTrip(getSkimClassName('t',0,0,tour.getTimeOfDayAlt(),"THRU_"),tp.getTazID(),tour.getDestTAZ());
        if (buildTripFile) {
            tripFileText.append(tripCount++).append(",");
            tripFileText.append(tourCount++).append(",");
            tripFileText.append(tp.getVisitorType()).append(",");
            tripFileText.append(VisitorType.getVisitorType(tp.getVisitorType()).toString().toLowerCase()).append(" visitor,");
            tripFileText.append(tp.getID()).append(",");
            tripFileText.append("1").append(",");
            tripFileText.append("-1").append(",");
            tripFileText.append("99").append(",");
            tripFileText.append("Thru").append(",");
            tripFileText.append("t").append(",");
            tripFileText.append(tp.getTazID()).append(",");
            tripFileText.append(tour.getDestTAZ()).append(",");
            tripFileText.append("-1").append(",");
            tripFileText.append(tour.getTimeOfDayAlt()).append(",");
            tripFileText.append(getLongModeName(getModeName(0,'t')));
            tripFileText.append("\n");
        }
    }

    private void addExternalWorkerTour(ExternalWorker ew) {
        int skimOut = ew.getSkimPeriodOut();
        int skimIn = ew.getSkimPeriodIn();
        int i = ew.getHomeTaz();
        int j = ew.getWorkTaz();
        addTrip(getSkimClassName('e',0,0,skimOut,"EW_"),i,j);
        addTrip(getSkimClassName('e',0,0,skimIn,"EW_"),j,i);
        if (buildTripFile) {
            StringBuffer tripBaseBuffer = new StringBuffer();
            tripBaseBuffer.append(tourCount++).append(",");
            tripBaseBuffer.append(9).append(",");
            tripBaseBuffer.append("external worker").append(",");
            tripBaseBuffer.append(ew.getID()).append(",");
            tripBaseBuffer.append("1").append(",");
            tripBaseBuffer.append("-1").append(",");
            tripBaseBuffer.append("50").append(",");
            tripBaseBuffer.append("External Work").append(",");
            String tripBase = tripBaseBuffer.toString();
            tripFileText.append(tripCount++).append(",").append(tripBase).
                    append("ij").append(",").
                    append(i).append(",").
                    append(j).append(",").
                    append("-1").append(",").
                    append(skimOut).append(",").
                    append(getLongModeName(getModeName(0,'t'))).append("\n");
            tripFileText.append(tripCount++).append(",").append(tripBase).
                    append("ji").append(",").
                    append(j).append(",").
                    append(i).append(",").
                    append("-1").append(",").
                    append(skimIn).append(",").
                    append(getLongModeName(getModeName(0,'t'))).append("\n");
        }

    }

    //I think this method ruins the original matrix map, so only use it if you're done with it
    private HashMap<String,Matrix> condenseMatricesToModeSkim() {

        HashMap<String,Matrix> condensedTrips = new HashMap<String,Matrix>();
        for (String key : trips.keySet()) {
            String newKey = key.substring(key.length() - 5);
            if (condensedTrips.containsKey(newKey)) {
                condensedTrips.put(newKey,condensedTrips.get(newKey).add(trips.get(key)));
            } else {
                condensedTrips.put(newKey,trips.get(key));
            }
        }

        return condensedTrips;
    }

}
