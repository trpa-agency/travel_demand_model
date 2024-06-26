package com.pb.tahoe.structures;

import org.apache.log4j.Logger;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

/**
 * @author Freedman
 *
 * A class for joint tours
 */
//public class JointTour extends Tour implements java.io.Serializable {
public class JointTour extends Tour implements java.io.Externalizable {

    protected static Logger logger = Logger.getLogger(JointTour.class);

    boolean adultsOnly;
    boolean childrenOnly;
    boolean adultsAndChildren;
    boolean participate;


    public JointTour() {
        super();
    }


    public JointTour (int numPersons) {
        super (numPersons);
    }


    /**
     * get array of person ids for persons making this joint tour
     */
    public int[] getJointTourPersons () {
        int[] persons = new int[numPersons];

        int k=0;
        for (int i=1; i < this.personParticipation.length; i++) {
            if (this.personParticipation[i]) {
                persons[k] = i;
                k++;
            }
        }

        int[] tempPersons = new int[k];
        for (int i=0; i < k; i++)
            tempPersons[i] = persons[i];

        return tempPersons;
    }


    /**
     * set tour composition type for this JointTour object
     */
    public void setTourComposition (int a) {

        this.adultsOnly = false;
        this.childrenOnly = false;
        this.adultsAndChildren = false;

        switch (a) {
            case 1:
                this.adultsOnly = true;
                break;
            case 2:
                this.childrenOnly = true;
                break;
            case 3:
                this.adultsAndChildren = true;
                break;
        }
    }


    /**
     * get tour composition type for this JointTour object
     */
    public int getTourCompositionCode () {

        if (this.adultsOnly)
            return 1;
        else if (this.childrenOnly)
            return 2;
        else
            return 3;

    }


    /**
     * return true or false for adults present in joint tour.
     * true if childrenOnly is false
     */
    public boolean areAdultsInTour () {
        return !this.childrenOnly;
    }


    /**
     * return true or false for adults present in joint tour.
     * true if adultsOnly is false
     */
    public boolean areChildrenInTour () {
        return !this.adultsOnly;
    }


    /**
     * return true or false for if joint tour party is valid based on tour composition.
     */
    public boolean isTourValid (int children, int adults) {

        if (this.childrenOnly && children > 1 && adults == 0)
            return true;
        else if (this.adultsOnly && children == 0 && adults > 1)
            return true;
        else if (this.adultsAndChildren && children > 0 && adults > 0)
            return true;

        return false;
    }


    /*
      * print the value of all the attributes in this object to the baseLogger
      */
    public void printJointTourState () {

        JointTour.logger.info( "");
        JointTour.logger.info( "Joint Tour object information");
        JointTour.logger.info( "-----------------------");

        JointTour.logger.info( "(boolean) adultsOnly = " + adultsOnly);
        JointTour.logger.info( "(boolean) childrenOnly = " + childrenOnly);
        JointTour.logger.info( "(boolean) adultsAndChildren = " + adultsAndChildren);
        JointTour.logger.info( "(boolean) participate = " + participate);

        printTourState();
    }


    /**
     * Each variable is read in the order that it written out in writeExternal().
     * When reading arrays, the length of the array is read first so the for-loop
     * can be initialized with the correct length.
     *
     * @see java.io.Externalizable#readExternal(java.io.ObjectInput)
     */
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        super.readExternal(in);

        adultsOnly = in.readBoolean();
        childrenOnly = in.readBoolean();
        adultsAndChildren = in.readBoolean();
        participate = in.readBoolean();
    }


    /**
     * Each variable is written out in the order that it was declared in the class.
     * When writing out arrays, the length of the array is written first so the
     * readExternal method knows how many elements to read.
     *
     * @see java.io.Externalizable#writeExternal(java.io.ObjectOutput)
     */
    public void writeExternal(ObjectOutput out) throws IOException {
        super.writeExternal(out);

        out.writeBoolean(adultsOnly);
        out.writeBoolean(childrenOnly);
        out.writeBoolean(adultsAndChildren);
        out.writeBoolean(participate);
    }

    public void writeContentToLogger(Logger logger){
      logger.info("adultsOnly:"+adultsOnly);
      logger.info("childrenOnly:"+childrenOnly);
      logger.info("adultsAndChildren:"+adultsAndChildren);
      logger.info("participate:"+participate);
    }

}
