package com.pb.tahoe.structures;

import com.pb.common.util.ObjectUtil;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

/**
 * @author Freedman
 *
 */
//public class Tour implements java.io.Serializable {
public class Tour implements java.io.Externalizable {

    protected static Logger logger = Logger.getLogger(Tour.class);

    static final int WALK_SEGMENTS = 3;

    boolean[] personParticipation;
    short tourType;
    short subTourType;
    short subTourPerson;
    public Tour[] subTours;
    short[] subToursByType = new short[SubTourType.TYPES+1];
    short mode;
    short submodeOB=0;
    short submodeIB=0;
    short timeOfDayAlt;
    short origTaz;
    short destTaz;
    short parkTaz;
    short originShortWalk;
    short destShrtWlk;
    short tourOrder;
    short numPersons;
    short numAdults;
    short numChildren;
    short numPreschool;
    short numPredriv;
    short numUniv;
    short allWorkFull;
    short stopFreqAlt;
    short stopLocOB;
    short stopLocIB;
    short stopLocSubzoneOB;
    short stopLocSubzoneIB;
    short tripIkMode;
    short tripKjMode;
    short tripJkMode;
    short tripKiMode;


    float[] workMCLogsum;

    public Tour() {

    }


    public Tour (int numPersons) {
        this.personParticipation = new boolean[numPersons+1];
    }


    /**
     * set tour origin taz for this Tour object
     */
    public void setOrigTaz (int arg) {
        this.origTaz = (short)arg;
    }

    /**
     * return the tour origin taz for this Tour object
     */
    public int getOrigTaz () {
        return this.origTaz;
    }

    /**
     * set tour destination taz for this Tour object
     */
    public void setDestTaz (int arg) {
        this.destTaz = (short)arg;
    }

    /**
     * return the tour destination taz for this Tour object
     */
    public int getDestTaz () {
        return this.destTaz;
    }

    /**
     * set tour origin short walk segment for this Tour object
     */
    public void setOriginShrtWlk (int arg) {
        this.originShortWalk = (short)arg;
    }

    public int getOriginShrtWlk () {
        return this.originShortWalk;
    }

    /**
     * set tour destination short walk segment for this Tour object
     */
    public void setDestShrtWlk (int arg) {
        this.destShrtWlk = (short)arg;
    }

    /**
     * return the tour destination short walk segment for this Tour object
     */
    public int getDestShrtWlk () {
        return this.destShrtWlk;
    }

    /**
     * set TAZ for the chosen parking location alternative for this tour
     */
    public void setChosenPark (int arg) {
        parkTaz = (short)arg;
    }

    /**
     * return TAZ for the chosen parking location alternative for this tour
     */
    public int getChosenPark () {
        return parkTaz;
    }

    /**
     * set tour mode for this Tour object
     */
    public void setMode (int arg) {
        this.mode = (short)arg;
    }

    /**
     * return the tour mode for this Tour object
     */
    public int getMode () {
        return this.mode;
    }

    /**
     * set ik trip mode for this Tour object
     */
    public void setTripIkMode (int arg) {
        this.tripIkMode = (short)arg;
    }

    /**
     * return the ik trip mode for this Tour object
     */
    public int getTripIkMode () {
        return this.tripIkMode;
    }

    /**
     * set jk trip mode for this Tour object
     */
    public void setTripJkMode (int arg) {
        this.tripJkMode = (short)arg;
    }

    /**
     * return the jk trip mode for this Tour object
     */
    public int getTripJkMode () {
        return this.tripJkMode;
    }

    /**
     * set kj trip mode for this Tour object
     */
    public void setTripKjMode (int arg) {
        this.tripKjMode = (short)arg;
    }

    /**
     * return the kj trip mode for this Tour object
     */
    public int getTripKjMode () {
        return this.tripKjMode;
    }

    /**
     * set ki trip mode for this Tour object
     */
    public void setTripKiMode (int arg) {
        this.tripKiMode = (short)arg;
    }

    /**
     * return the ki trip mode for this Tour object
     */
    public int getTripKiMode () {
        return this.tripKiMode;
    }

    /**
     * return number of persons participating in joint tours.
     */
    public int getNumPersons () {
        return this.numPersons;
    }

    /**
     * set number of persons participating in joint tours.
     */
    public void setNumPersons (int numPersons) {
        this.numPersons = (short)numPersons;
    }

    /**
     * return number of adults participating in joint tours.
     */
    public int getNumAdults () {
        return this.numAdults;
    }

    /**
     * set number of adults participating in joint tours.
     */
    public void setNumAdults (int numAdults) {
        this.numAdults = (short)numAdults;
    }

    /**
     * return number of children participating in joint tours.
     */
    public int getNumChildren () {
        return this.numChildren;
    }

    /**
     * set number of children participating in joint tours.
     */
    public void setNumChildren (int numChildren) {
        this.numChildren = (short)numChildren;
    }

    /**
     * return number of pre-school children participating in joint tours.
     */
    public int getNumPreschool () {
        return this.numPreschool;
    }

    /**
     * set number of pre-school children participating in joint tours.
     */
    public void setNumPreschool (int numPreschool) {
        this.numPreschool = (short)numPreschool;
    }

    /**
     * return number of pre-driving children participating in joint tours.
     */
    public int getNumPredriv () {
        return this.numPredriv;
    }

    /**
     * set number of pre-driving children participating in joint tours.
     */
    public void setNumPredriv (int numPredriv) {
        this.numPredriv = (short)numPredriv;
    }

    /**
     * return number of university students participating in joint tours.
     */
    public int getNumUniv () {
        return this.numUniv;
    }

    /**
     * set number of university students participating in joint tours.
     */
    public void setNumUniv (int numUniv) {
        this.numUniv = (short)numUniv;
    }

    /**
     * return 1 if all adults in joint tour are full time workers; 0 otherwise.
     */
    public int getAllAdultsWorkFull () {
        return this.allWorkFull;
    }

    /**
     * set 1 for all adults in joint tour are full time workers; 0 otherwise.
     */
    public void setAllAdultsWorkFull (int allWorkFull) {
        this.allWorkFull = (short)allWorkFull;
    }

    /**
     * set tour order for WORK_2 tour types
     */
    public void setTourOrder (int arg) {
        this.tourOrder = (short)arg;
    }

    /**
     * return the tour order for this Tour object
     */
    public int getTourOrder () {
        return this.tourOrder;
    }

    /**
     * set tour time of day for this Tour object
     */
    public void setTimeOfDayAlt (int arg) {
        this.timeOfDayAlt = (short)arg;
    }

    /**
     * return the tour time of day for this Tour object
     */
    public int getTimeOfDayAlt () {
        return this.timeOfDayAlt;
    }

    /**
     * set tour type for this Tour object
     */
    public void setTourType (short tourType) {
        this.tourType = tourType;
    }

    /**
     * set subtour type for this Tour object
     */
    public void setSubTourType (short tourType) {
        this.subTourType = tourType;
    }

    /**
     * set person making this subtour for this Tour object
     */
    public void setSubTourPerson (int person) {
        this.subTourPerson = (short)person;
    }

    /**
     * get tour type for this Tour object
     */
    public int getTourType () {
        return this.tourType;
    }

    /**
     * get subtour type for this Tour object
     */
    public int getSubTourType () {
        return this.subTourType;
    }

    /**
     * get person making this tour
     */
    public int getTourPerson () {
        int person = 0;

        for (int i=1; i < this.personParticipation.length; i++) {
            if (this.personParticipation[i]) {
                person = i;
                break;
            }
        }

        return person;
    }

    /**
     * get person making this subtour for this Tour object
     */
    public int getSubTourPerson () {
        return this.subTourPerson;
    }

    /**
     * set person participation for the person id passed in for this Tour object
     */
    public void setPersonParticipation (int p, boolean participate) {
        this.personParticipation[p] = participate;
    }

    /**
     * return true/false that person p participates in this tour
     */
    public boolean getPersonParticipation (int p) {
        return this.personParticipation[p];
    }

    /**
     * set the array of individual at-work subtours for this work tour object
     */
    public void setSubTours (Tour[] subTours) {
        this.subTours = subTours;
    }

    /**
     * return the array of individual at-work subtours for this work tour object
     */
    public Tour[] getSubTours () {
        return this.subTours;
    }

    /**
     * increment the number of subtours in the work tour by type.
     */
    public void incrementSubToursByType ( int tourType ) {
        this.subToursByType[tourType]++;
    }


    /**
     * set the stop frequency alternative number selected by model 8.1
     */
    public void setStopFreqAlt (int stopFreq) {
        this.stopFreqAlt = (short)stopFreq;
    }

    /**
     * return the stop frequency alternative number selected by model 8.1
     */
    public int getStopFreqAlt () {
        return this.stopFreqAlt;
    }

    /**
     * set the outbound stop location taz number selected by model 8.2
     */
    public void setStopLocOB (int stopLoc) {
        this.stopLocOB = (short)stopLoc;
    }

    /**
     * return the outbound stop location taz number selected by model 8.2
     */
    public int getStopLocOB () {
        return this.stopLocOB;
    }

    /**
     * set the inbound stop location taz number selected by model 8.2
     */
    public void setStopLocIB (int stopLoc) {
        this.stopLocIB = (short)stopLoc;
    }

    /**
     * return the inbound stop location taz number selected by model 8.2
     */
    public int getStopLocIB () {
        return this.stopLocIB;
    }

    /**
     * set the outbound stop location subzone number selected by model 8.2
     */
    public void setStopLocSubzoneOB (int stopLoc) {
        this.stopLocSubzoneOB = (short)stopLoc;
    }

    /**
     * get the outbound stop location subzone number selected by model 8.2
     */
    public int getStopLocSubzoneOB () {
        return this.stopLocSubzoneOB;
    }

    /**
     * set the inbound stop location subzone number selected by model 8.2
     */
    public void setStopLocSubzoneIB (int stopLoc) {
        this.stopLocSubzoneIB = (short)stopLoc;
    }

    /**
     * get the inbound stop location subzone number selected by model 8.2
     */
    public int getStopLocSubzoneIB () {
        return this.stopLocSubzoneIB;
    }

    /**
     * set the outbound transit submode for WT and DT tours
     */
    public void setSubmodeOB (int submode) {
        this.submodeOB = (short)submode;
    }

    /**
     * get the outbound transit submode for WT and DT tours
     */
    public int getSubmodeOB () {
        return this.submodeOB;
    }

    /**
     * set the inbound transit submode for WT and DT tours
     */
    public void setSubmodeIB (int submode) {
        this.submodeIB = (short)submode;
    }

    /**
     * get the inbound transit submode for WT and DT tours
     */
    public int getSubmodeIB () {
        return this.submodeIB;
    }

    public void setWorkMCLogsum(float[] workMCLogsum) {
        this.workMCLogsum = workMCLogsum;
    }

    public float[] getWorkMCLogsum() {
        return workMCLogsum;
    }

    /**
     * print the value of all the attributes in this object to the baseLogger
     */
    public void printTourState () {

        Tour.logger.info( "");
        Tour.logger.info( "Tour object information");
        Tour.logger.info( "-----------------------");

        Tour.logger.info( "(short) tourType = " + tourType);
        Tour.logger.info( "(short) subTourType = " + subTourType);
        Tour.logger.info( "(short) subTourPerson = " + subTourPerson);
        Tour.logger.info( "(short) mode = " + mode);
        Tour.logger.info( "(short) submodeOB = " + submodeOB);
        Tour.logger.info( "(short) submodeIB = " + submodeIB);
        Tour.logger.info( "(short) timeOfDayAlt = " + timeOfDayAlt);
        Tour.logger.info( "(short) origTaz = " + origTaz);
        Tour.logger.info( "(short) destTaz = " + destTaz);
        Tour.logger.info( "(short) destShrtWlk = " + destShrtWlk);
        Tour.logger.info( "(short) tourOrder = " + tourOrder);
        Tour.logger.info( "(short) numPersons = " + numPersons);
        Tour.logger.info( "(short) numAdults = " + numAdults);
        Tour.logger.info( "(short) numChildren = " + numChildren);
        Tour.logger.info( "(short) numPreschool = " + numPreschool);
        Tour.logger.info( "(short) numPredriv = " + numPredriv);
        Tour.logger.info( "(short) numUniv = " + numUniv);
        Tour.logger.info( "(short) allWorkFull = " + allWorkFull);
        Tour.logger.info( "(short) stopFreqAlt = " + stopFreqAlt);
        Tour.logger.info( "(short) stopLocOB = " + stopLocOB);
        Tour.logger.info( "(short) stopLocIB = " + stopLocIB);
        Tour.logger.info( "(short) stopLocSubzoneOB = " + stopLocSubzoneOB);
        Tour.logger.info( "(short) stopLocSubzoneIB = " + stopLocSubzoneIB);

        Tour.logger.info( "(boolean[]) personParticipation = ");
        if (personParticipation != null) {
            for (int i=0; i < personParticipation.length; i++)
                Tour.logger.info( "personParticipation[" + i + "] =" + personParticipation[i]);
        }
        else {
            Tour.logger.info( "personParticipation = null");
        }

        Tour.logger.info( "(short[]) subToursByType = ");
        if (subToursByType != null) {
            for (int i=0; i < subToursByType.length; i++)
                Tour.logger.info( "subToursByType[" + i + "] =" + subToursByType[i]);
        }
        else {
            Tour.logger.info( "subToursByType = null");
        }

        if (subTours != null) {
            for (int i=0; i < subTours.length; i++) {
                if (subTours[i] != null)
                    subTours[i].printTourState();
                else
                    Tour.logger.info( "subTours[" + i + "] = null");
            }
        }
        else {
            Tour.logger.info( "subTours = null");
        }

    }


    /**
     * Each variable is read in the order that it written out in writeExternal().
     * When reading arrays, the length of the array is read first so the for-loop
     * can be initialized with the correct length.
     *
     * @see java.io.Externalizable#readExternal(java.io.ObjectInput)
     */
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        personParticipation = ObjectUtil.readBooleanArray(in);

        tourType = in.readShort();
        subTourType = in.readShort();
        subTourPerson = in.readShort();

        //subTours[]
        int length = in.readInt();
        if (length > 0) {
            subTours = new Tour[length];
            for (int i=0; i < length; i++) {
                subTours[i] = (Tour) in.readObject();
            }
        }

        subToursByType = ObjectUtil.readShortArray(in);

        mode = in.readShort();
        submodeOB = in.readShort();
        submodeIB = in.readShort();
        timeOfDayAlt = in.readShort();
        origTaz = in.readShort();
        destTaz = in.readShort();
        parkTaz = in.readShort();
        originShortWalk = in.readShort();
        destShrtWlk = in.readShort();
        tourOrder = in.readShort();
        numPersons = in.readShort();
        numAdults = in.readShort();
        numChildren = in.readShort();
        numPreschool = in.readShort();
        numPredriv = in.readShort();
        numUniv = in.readShort();
        allWorkFull = in.readShort();
        stopFreqAlt = in.readShort();
        stopLocOB = in.readShort();
        stopLocIB = in.readShort();
        stopLocSubzoneOB = in.readShort();
        stopLocSubzoneIB = in.readShort();
        tripIkMode = in.readShort();
        tripKjMode = in.readShort();
        tripJkMode = in.readShort();
        tripKiMode = in.readShort();
    }


    /**
     * Each variable is written out in the order that it was declared in the class.
     * When writing out arrays, the length of the array is written first so the
     * readExternal method knows how many elements to read.
     *
     * @see java.io.Externalizable#writeExternal(java.io.ObjectOutput)
     */
    public void writeExternal(ObjectOutput out) throws IOException {
        ObjectUtil.writeBooleanArray(out, personParticipation);

        out.writeShort(tourType);
        out.writeShort(subTourType);
        out.writeShort(subTourPerson);

        ObjectUtil.writeObjectArray(out, subTours);
        ObjectUtil.writeShortArray(out, subToursByType);

        out.writeShort(mode);
        out.writeShort(submodeOB);
        out.writeShort(submodeIB);
        out.writeShort(timeOfDayAlt);
        out.writeShort(origTaz);
        out.writeShort(destTaz);
        out.writeShort(parkTaz);
        out.writeShort(originShortWalk);
        out.writeShort(destShrtWlk);
        out.writeShort(tourOrder);
        out.writeShort(numPersons);
        out.writeShort(numAdults);
        out.writeShort(numChildren);
        out.writeShort(numPreschool);
        out.writeShort(numPredriv);
        out.writeShort(numUniv);
        out.writeShort(allWorkFull);
        out.writeShort(stopFreqAlt);
        out.writeShort(stopLocOB);
        out.writeShort(stopLocIB);
        out.writeShort(stopLocSubzoneOB);
        out.writeShort(stopLocSubzoneIB);
        out.writeShort(tripIkMode);
        out.writeShort(tripKjMode);
        out.writeShort(tripJkMode);
        out.writeShort(tripKiMode);
    }

    public void writeContentToLogger(Logger logger, String description){

        logger.info(description);
        logger.info("tourType:"+tourType);
        logger.info("subTourType:"+subTourType);
        logger.info("subTourPerson:"+subTourPerson);

        if (personParticipation != null) {
              for(int i=0; i<personParticipation.length; i++){
                logger.info("personParticipation["+i+"]="+personParticipation[i]);
              }
        }
        else {
            logger.info("personParticipation[]=null");
        }

        if (subTours != null) {
            for(int i=0; i<subTours.length; i++){
                Tour temp=subTours[i];
                temp.writeContentToLogger(logger, "subtour " + i);
            }
        }
        else {
            logger.info("subTours[]=null");
        }

      if (subToursByType != null) {
          for(int i=0; i<subToursByType.length; i++){
            logger.info("subToursByType["+i+"]="+subToursByType[i]);
          }
      }
      else {
          logger.info("subToursByType[]=null");
      }

      logger.info("mode:"+mode);
      logger.info("submodeOB:"+submodeOB);
      logger.info("submodeIB:"+submodeIB);
      logger.info("timeOfDayAlt:"+timeOfDayAlt);
      logger.info("origTaz:"+origTaz);
      logger.info("destTaz:"+destTaz);
      logger.info("parkTaz:"+parkTaz);
      logger.info("originShortWalk:"+originShortWalk);
      logger.info("destShrtWlk:"+destShrtWlk);
      logger.info("tourOrder:"+tourOrder);
      logger.info("numPersons:"+numPersons);
      logger.info("numAdults:"+numAdults);
      logger.info("numChildren:"+numChildren);
      logger.info("numPreschool:"+numPreschool);
      logger.info("numPredriv:"+numPredriv);
      logger.info("numUniv:"+numUniv);
      logger.info("stopFreqAlt:"+stopFreqAlt);
      logger.info("stopLocOB:"+stopLocOB);
      logger.info("stopLocIB:"+stopLocIB);
      logger.info("stopLocSubzoneOB:"+stopLocSubzoneOB);
      logger.info("stopLocSubzoneIB:"+stopLocSubzoneIB);
      logger.info("tripIkMode:"+tripIkMode);
      logger.info("tripKjMode:"+tripKjMode);
      logger.info("tripJkMode:"+tripJkMode);
      logger.info("tripKiMode:"+tripKiMode);
    }
}
