package com.pb.tahoe.visitor.structures;

import org.apache.log4j.Logger;

/**
 * User: Chris
 * Date: Feb 7, 2007 - 11:43:23 AM
 */
public class VisitorTour implements java.io.Serializable {

    protected static Logger logger = Logger.getLogger(VisitorTour.class);

    /**
     * The tour number (1 is the first tour of the day, 2 is the second, etc.).
     */
    private short tourNum;

    /**
     * The tour type of this tour.
     */
    private VisitorTourType tourType;

    /**
     * The destination taz associated with this tour.
     */
    private short destTAZ;

    /**
     * The walk access segment of this tour's destination. The following codes are used:
     * <ul>
     *   <li>0 - No walk to transit access.</li>
     *   <li>1 - Short walk to transit.</li>
     *   <li>2 - Long walk to transit.</li>
     * </ul>
     */
    private short destWalkSegment;

    /**
     * The time of day alternative chosen for this tour.
     */
    private short timeOfDayAlt;

    /**
     * The mode chosen for this tour.
     */
    private VisitorMode mode;

    /**
     * An indicator for the presence of an outbound stop on this tour.
     */
    private boolean outboundStop;

    /**
     * An indicator for the presence of an inbound stop on this tour.
     */
    private boolean inboundStop;

    /**
     * The destination taz for this tour's outbound stop.
     */
    private short obTAZ;

    /**
     * The walk segement for the outbound stop's destination.
     */
    private short obWalkSegment;

    /**
     * The destination taz for this tour's inbound stop.
     */
    private short ibTAZ;

    /**
     * The walk segement for the inbound stop's destination.
     */
    private short ibWalkSegment;

    /**
     * The mode selected for this tour's outbound stop.
     */
    private VisitorTripMode obMode;

    /**
     * The mode selected for this tour's inbound stop.
     */
    private VisitorTripMode ibMode;

    public void setTourNum(int tourNum) {
        this.tourNum = (short) tourNum;
    }

    public int getTourNum() {
        return tourNum;
    }

    public void setTourType(VisitorTourType tourType) {
        this.tourType = tourType;
    }

    public VisitorTourType getTourType() {
        return tourType;
    }

    public void setDestTAZ(int destTAZ) {
        this.destTAZ = (short) destTAZ;
    }

    public int getDestTAZ() {
        return destTAZ;
    }

    public void setDestWalkSegment(int destWalkSegment) {
        this.destWalkSegment = (short) destWalkSegment;
    }

    public int getDestWalkSegment() {
        return destWalkSegment;
    }

    public void setTimeOfDayAlt(int timeOfDayAlt) {
        this.timeOfDayAlt = (short) timeOfDayAlt;
    }

    public int getTimeOfDayAlt() {
        return timeOfDayAlt;
    }

    public void setMode(VisitorMode mode) {
        this.mode = mode;
    }

    public VisitorMode getMode() {
        return mode;
    }

    public void setOutboundStop(boolean outboundStop) {
        this.outboundStop = outboundStop;
    }

    public boolean getOutboundStop() {
        return outboundStop;
    }

    public void setInboundStop(boolean inboundStop) {
        this.inboundStop = inboundStop;
    }

    public boolean getInboundStop() {
        return inboundStop;
    }

    public void setObTAZ(int obTAZ) {
        this.obTAZ = (short) obTAZ;
    }

    public int getObTAZ() {
        return obTAZ;
    }

    public void setObWalkSegment(int obWalkSegment) {
        this.obWalkSegment = (short) obWalkSegment;
    }

    public int getObWalkSegment() {
        return obWalkSegment;
    }

    public void setIbTAZ(int ibTAZ) {
        this.ibTAZ = (short) ibTAZ;
    }

    public int getIbTAZ() {
        return ibTAZ;
    }

    public void setIbWalkSegment(int ibWalkSegment) {
        this.ibWalkSegment = (short) ibWalkSegment;
    }

    public int getIbWalkSegment() {
        return ibWalkSegment;
    }

    public void setObMode(VisitorTripMode obMode) {
        this.obMode = obMode;
    }

    public VisitorTripMode getObMode() {
        return obMode;
    }

    public void setIbMode(VisitorTripMode ibMode) {
        this.ibMode = ibMode;
    }

    public VisitorTripMode getIbMode() {
        return ibMode;
    }


    public String toString() {
        String outString = "\n";
        outString += "  Tour number: " + tourNum + "\n";
        String tourString = Character.toString(tourType.getIDChar());
        if (outboundStop) tourString = VisitorDataStructure.stopChar + tourString;
        if (inboundStop) tourString += VisitorDataStructure.stopChar;
        outString += "    Tour string: " + VisitorDataStructure.homeChar + tourString +
                VisitorDataStructure.homeChar + "\n";
        outString += "    Primary destination TAZ: " + destTAZ + "\n";
        outString += "    Primary destination walk segment: " + destWalkSegment + "\n";
        outString += "    Time of day alternative: " + timeOfDayAlt + "\n";
        outString += "    Primary mode: " + mode + "\n";
        if (outboundStop) {
            outString += "    Outbound destination TAZ: " + obTAZ + "\n";
            outString += "    Outbound walk segment: " + obWalkSegment + "\n";
            outString += "    Outbound mode: " + obMode + "\n";
        }
        if (inboundStop) {
            outString += "    Inbound destination TAZ: " + ibTAZ + "\n";
            outString += "    Inbound walk segment: " + ibWalkSegment + "\n";
            outString += "    Inbound mode: " + ibMode + "\n";
        }
        return outString;
    }
    
}
