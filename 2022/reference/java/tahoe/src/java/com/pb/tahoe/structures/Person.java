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
//public class Person implements java.io.Serializable {
public class Person implements java.io.Externalizable {

    protected static Logger logger = Logger.getLogger(Person.class);

    short ID;
    short personType;
    short patternType;
    short freeParking;

    //an array indicated whether the person is available
    //for each hour of the day.
    boolean[] available = new boolean[TemporalType.HOURS+1];

    short maxAdultOverlaps;
    short maxChildOverlaps;
    short availWindow;

    boolean[] jointTourParticipation;
    boolean[] individualTourParticipation;
    boolean[] mandatoryTourParticipation;

    short numMandTours;
    short numJointTours;
    short numIndNonMandTours;
    short numIndNonMandInceTours;
    short numIndNonMandShopTours;
    short numIndNonMandMaintTours;
    short numIndNonMandDiscrTours;
    short numIndNonMandEatTours;


    public Person () {
        for (int i=0; i < available.length; i++)
            if (i < 5 || i > 23)
                available[i] = false;
            else
                available[i] = true;
    }


    /**
     * return number of mandatory tours for this person.
     */
    public int getNumMandTours () {
        return this.numMandTours;
    }


    /**
     * set number of mandatory tours for this person.
     */
    public void setNumMandTours (int numMandTours) {
        this.numMandTours = (short)numMandTours;
    }


    /**
     * return number of joint tours in which this person participates.
     */
    public int getNumJointTours () {
        return this.numJointTours;
    }


    /**
     * set number of joint tours in which this person participates.
     */
    public void setNumJointTours (int numJointTours) {
        this.numJointTours = (short)numJointTours;
    }


    /**
     * return number of individual non-mandatory tours in which this person participates.
     */
    public int getNumIndNonMandTours () {
        return this.numIndNonMandTours;
    }


    /**
     * set number of individual non-mandatory tours in which this person participates.
     */
    public void setNumIndNonMandTours (int numIndNonMandTours) {
        this.numIndNonMandTours = (short)numIndNonMandTours;
    }


    /**
     * return number of individual non-mandatory tours including escorting in which this person participates.
     */
    public int getNumIndNonMandInceTours () {
        return this.numIndNonMandInceTours;
    }


    /**
     * set number of individual non-mandatory tours including escorting in which this person participates.
     */
    public void setNumIndNonMandInceTours (int numIndNonMandInceTours) {
        this.numIndNonMandInceTours = (short)numIndNonMandInceTours;
    }


    /**
     * set number of individual non-mandatory shopping tours in which this person participates.
     */
    public void setNumIndNonMandShopTours (int numIndNonMandShopTours) {
        this.numIndNonMandShopTours = (short)numIndNonMandShopTours;
    }


    /**
     * return number of individual non-mandatory shopping tours in which this person participates.
     */
    public int getNumIndNonMandShopTours () {
        return this.numIndNonMandShopTours;
    }


    /**
     * set number of individual non-mandatory other maintenance tours in which this person participates.
     */
    public void setNumIndNonMandMaintTours (int numIndNonMandMaintTours) {
        this.numIndNonMandMaintTours = (short)numIndNonMandMaintTours;
    }


    /**
     * return number of individual non-mandatory other maintenance tours in which this person participates.
     */
    public int getNumIndNonMandMaintTours () {
        return this.numIndNonMandMaintTours;
    }


    /**
     * set number of individual non-mandatory discretionary tours in which this person participates.
     */
    public void setNumIndNonMandDiscrTours (int numIndNonMandDiscrTours) {
        this.numIndNonMandDiscrTours = (short)numIndNonMandDiscrTours;
    }


    /**
     * return number of individual non-mandatory discretionary tours in which this person participates.
     */
    public int getNumIndNonMandDiscrTours () {
        return this.numIndNonMandDiscrTours;
    }


    /**
     * set number of individual non-mandatory eating-out tours in which this person participates.
     */
    public void setNumIndNonMandEatTours (int numIndNonMandEatTours) {
        this.numIndNonMandEatTours = (short)numIndNonMandEatTours;
    }


    /**
     * return number of individual non-mandatory eating-out tours in which this person participates.
     */
    public int getNumIndNonMandEatTours () {
        return this.numIndNonMandEatTours;
    }


    /**
     * return maximum time window overlaps between this person and other adult persons in the household.
     */
    public int getMaxAdultOverlaps () {
        return this.maxAdultOverlaps;
    }


    /**
     * set maximum time window overlaps between this person and other adult persons in the household.
     */
    public void setMaxAdultOverlaps (int overlaps) {
        this.maxAdultOverlaps = (short)overlaps;
    }


    /**
     * return maximum time window overlaps between this person and other children in the household.
     */
    public int getMaxChildOverlaps () {
        return this.maxChildOverlaps;
    }


    /**
     * set maximum time window overlaps between this person and other children in the household.
     */
    public void setMaxChildOverlaps (int overlaps) {
        this.maxChildOverlaps = (short)overlaps;
    }


    /**
     * return available time window for this person in the household.
     */
    public int getAvailableWindow () {
        return this.availWindow;
    }


    /**
     * set available time window for this person in the household.
     */
    public void setAvailableWindow (int availWindow) {
        this.availWindow = (short)availWindow;
    }


    /**
     * return true/false that person participates in joint tour id
     */
    public boolean getJointTourParticipation (int id) {
        return this.jointTourParticipation[id];
    }


    /**
     * set true/false that person participates in joint tour id
     */
    public void setJointTourParticipation (int id, boolean participation) {
        this.jointTourParticipation[id] = participation;
    }


    /**
     * define the array which holds true/false that person participates in joint tour
     */
    public void setJointTourParticipationArray (int numJointTours) {
        this.jointTourParticipation = new boolean[numJointTours];
    }


    /**
     * return true/false that person participates in individual tour id
     */
    public boolean getIndividualTourParticipation (int id) {
        return this.individualTourParticipation[id];
    }


    /**
     * set true/false that person participates in individual tour
     */
    public void setIndividualTourParticipation (int id, boolean participation) {
        this.individualTourParticipation[id] = participation;
    }


    /**
     * return true/false that person participates in individual mandatory tour
     */
    public boolean getMandatoryTourParticipation (int id) {
        return this.mandatoryTourParticipation[id];
    }


    /**
     * set true/false that person participates in individual mandatory tour
     */
    public void setMandatoryTourParticipation (int id, boolean participation) {
        this.mandatoryTourParticipation[id] = participation;
    }


    /**
     * define the array which holds true/false that person participates in individual tour
     */
    public void setIndividualTourParticipationArray (int numIndivTours) {
        this.individualTourParticipation = new boolean[numIndivTours];
    }


    /**
     * define the array which holds true/false that person participates in individual mandatory tour
     */
    public void setMandatoryTourParticipationArray (int numMandatoryTours) {
        this.mandatoryTourParticipation = new boolean[numMandatoryTours];
    }


    /**
     * return number of mandatory tours in which person participates
     */
    public int getMandatoryTourCount () {

        int count=0;
        if (this.mandatoryTourParticipation != null) {
            for (int i=0; i < this.mandatoryTourParticipation.length; i++) {
                if (this.mandatoryTourParticipation[i])
                    count++;
            }
        }

        return count;
    }


    /**
     * return number of non-mandatory tours in which person participates
     */
    public int getNonMandatoryTourCount () {

        int count=0;

        if (this.individualTourParticipation != null) {
            for (int i=0; i < this.individualTourParticipation.length; i++) {
                if (this.individualTourParticipation[i])
                    count++;
            }
        }

        return count;
    }


    /**
     * return number of joint tours in which person participates
     */
    public int getJointTourCount () {

        int count=0;
        if (this.jointTourParticipation != null) {
            for (int i=0; i < this.jointTourParticipation.length; i++) {
                if (this.jointTourParticipation[i])
                    count++;
            }
        }

        return count;
    }


    /**
     * set id for person object
     */
    public void setID (int id) {
        this.ID = (short)id;
    }

    /**
     * return id for person object
     */
    public int getID () {
        return this.ID;
    }

    /**
     * set personType for person object
     */
    public void setPersonType (int personType) {
        this.personType = (short)personType;
    }

    /**
     * get personType for person object
     */
    public int getPersonType () {
        return this.personType;
    }

    /**
     * set patternType for person object
     */
    public void setPatternType (int patternType) {
        this.patternType = (short)patternType;
    }

    /**
     * set patternType for person object
     */
    public int getPatternType () {
        return this.patternType;
    }

    /**
     * set freeParking for person object
     */
    public void setFreeParking (int arg) {
        this.freeParking = (short)arg;
    }

    /**
     * get freeParking for person object
     */
    public int getFreeParking () {
        return this.freeParking;
    }

    /**
     * set the hour passed in as available for travel for the person object
     */
    public void setHourAvailable (int arg) {
        this.available[arg] = true;
    }

    /**
     * set the hour passed in as unavailable for travel for the person object
     */
    public void setHourUnavailable (int arg) {
        this.available[arg] = false;
    }

    /**
     * return true/false that the hour passed in is available for travel
     */
    public boolean getHourIsAvailable (int arg) {
        return this.available[arg];
    }

    /**
     * return available array
     */
    public boolean[] getAvailable () {
        return this.available;
    }

    /*
      * print the value of all the attributes in this object to the baseLogger
      */
    public void printPersonState () {

        Person.logger.info( "");
        Person.logger.info( "Person object information for person ID = " + ID);
        Person.logger.info( "---------------------------------------------");

        Person.logger.info( "(short) personType = " + personType);
        Person.logger.info( "(short) patternType = " + patternType);
        Person.logger.info( "(short) freeParking = " + freeParking);

        Person.logger.info( "(short) maxAdultOverlaps = " + maxAdultOverlaps);
        Person.logger.info( "(short) maxChildOverlaps = " + maxChildOverlaps);
        Person.logger.info( "(short) availWindow = " + availWindow);

        Person.logger.info( "(short) numMandTours = " + numMandTours);
        Person.logger.info( "(short) numJointTours = " + numJointTours);
        Person.logger.info( "(short) numIndNonMandTours = " + numIndNonMandTours);
        Person.logger.info( "(short) numIndNonMandInceTours = " + numIndNonMandInceTours);
        Person.logger.info( "(short) numIndNonMandShopTours = " + numIndNonMandShopTours);
        Person.logger.info( "(short) numIndNonMandMaintTours = " + numIndNonMandMaintTours);
        Person.logger.info( "(short) numIndNonMandDiscrTours = " + numIndNonMandDiscrTours);
        Person.logger.info( "(short) numIndNonMandEatTours = " + numIndNonMandEatTours);

        Person.logger.info( "(boolean[]) available = ");
        if (available != null) {
            for (int i=0; i < available.length; i++)
                Person.logger.info( "available[" + i + "] =" + available[i]);
        }
        else {
            Person.logger.info( "available = null");
        }

        Person.logger.info( "(boolean[]) jointTourParticipation = ");
        if (jointTourParticipation != null) {
            for (int i=0; i < jointTourParticipation.length; i++)
                Person.logger.info( "jointTourParticipation[" + i + "] =" + jointTourParticipation[i]);
        }
        else {
            Person.logger.info( "jointTourParticipation = null");
        }

        Person.logger.info( "(boolean[]) individualTourParticipation = ");
        if (individualTourParticipation != null) {
            for (int i=0; i < individualTourParticipation.length; i++)
                Person.logger.info( "individualTourParticipation[" + i + "] =" + individualTourParticipation[i]);
        }
        else {
            Person.logger.info( "individualTourParticipation = null");
        }

        Person.logger.info( "(boolean[]) mandatoryTourParticipation = ");
        if (mandatoryTourParticipation != null) {
            for (int i=0; i < mandatoryTourParticipation.length; i++)
                Person.logger.info( "mandatoryTourParticipation[" + i + "] =" + mandatoryTourParticipation[i]);
        }
        else {
            Person.logger.info( "mandatoryTourParticipation = null");
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
        ID = in.readShort();
        personType = in.readShort();
        patternType = in.readShort();
        freeParking = in.readShort();

        available = ObjectUtil.readBooleanArray(in);

        maxAdultOverlaps = in.readShort();
        maxChildOverlaps = in.readShort();
        availWindow = in.readShort();

        jointTourParticipation = ObjectUtil.readBooleanArray(in);
        individualTourParticipation = ObjectUtil.readBooleanArray(in);
        mandatoryTourParticipation = ObjectUtil.readBooleanArray(in);

        numMandTours = in.readShort();
        numJointTours = in.readShort();
        numIndNonMandTours = in.readShort();
        numIndNonMandInceTours = in.readShort();
        numIndNonMandShopTours = in.readShort();
        numIndNonMandMaintTours = in.readShort();
        numIndNonMandDiscrTours = in.readShort();
        numIndNonMandEatTours = in.readShort();
    }


    /**
     * Each variable is written out in the order that it was declared in the class.
     * When writing out arrays, the length of the array is written first so the
     * readExternal method knows how many elements to read.
     *
     * @see java.io.Externalizable#writeExternal(java.io.ObjectOutput)
     */
    public void writeExternal(ObjectOutput out) throws IOException {
        out.writeShort(ID);
        out.writeShort(personType);
        out.writeShort(patternType);
        out.writeShort(freeParking);

        ObjectUtil.writeBooleanArray(out, available);

        out.writeShort(maxAdultOverlaps);
        out.writeShort(maxChildOverlaps);
        out.writeShort(availWindow);

        ObjectUtil.writeBooleanArray(out, jointTourParticipation);
        ObjectUtil.writeBooleanArray(out, individualTourParticipation);
        ObjectUtil.writeBooleanArray(out, mandatoryTourParticipation);

        out.writeShort(numMandTours);
        out.writeShort(numJointTours);
        out.writeShort(numIndNonMandTours);
        out.writeShort(numIndNonMandInceTours);
        out.writeShort(numIndNonMandShopTours);
        out.writeShort(numIndNonMandMaintTours);
        out.writeShort(numIndNonMandDiscrTours);
        out.writeShort(numIndNonMandEatTours);
    }

    public void writeContentToLogger(Logger logger){
      logger.info("Contents of Person object with index:"+ID);
      logger.info("personType:"+personType);
      logger.info("patternType:"+patternType);
      logger.info("freeParking:"+freeParking);

      if (available != null) {
          for(int i=0; i<available.length; i++){
              logger.info("available["+i+"]="+available[i]);
          }
      }
      else {
          logger.info("available[]=null");
      }

      logger.info("maxAdultOverlaps:"+maxAdultOverlaps);
      logger.info("maxChildOverlaps:"+maxChildOverlaps);
      logger.info("availWindow:"+availWindow);

      if (jointTourParticipation != null) {
          for(int i=0; i<jointTourParticipation.length; i++){
              logger.info("jointTourParticipation["+i+"]="+jointTourParticipation[i]);
          }
      }
      else {
          logger.info("jointTourParticipation[]=null");
      }

      if (individualTourParticipation != null) {
          for(int i=0; i<individualTourParticipation.length; i++){
            logger.info("individualTourParticipation["+i+"]="+individualTourParticipation[i]);
          }
      }
      else {
          logger.info("individualTourParticipation[]=null");
      }

      if (mandatoryTourParticipation != null) {
          for(int i=0; i<mandatoryTourParticipation.length; i++){
            logger.info("mandatoryTourParticipation["+i+"]="+mandatoryTourParticipation[i]);
          }
      }
      else {
          logger.info("mandatoryTourParticipation[]=null");
      }


      logger.info("numMandTours:"+numMandTours);
      logger.info("numJointTours:"+numJointTours);
      logger.info("numIndNonMandTours:"+numIndNonMandTours);
      logger.info("numIndNonMandInceTours:"+numIndNonMandInceTours);
      logger.info("numIndNonMandShopTours:"+numIndNonMandShopTours);
      logger.info("numIndNonMandMaintTours:"+numIndNonMandMaintTours);
      logger.info("numIndNonMandDiscrTours:"+numIndNonMandDiscrTours);
      logger.info("numIndNonMandEatTours:"+numIndNonMandEatTours);
    }
}
