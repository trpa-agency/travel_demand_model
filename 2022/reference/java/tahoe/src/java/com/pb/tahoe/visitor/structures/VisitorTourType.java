package com.pb.tahoe.visitor.structures;

/**
 * User: Chris
 * Date: Feb 7, 2007 - 11:47:53 AM
 */
public enum VisitorTourType implements java.io.Serializable {
    Recreation(1,'R'),
    Gaming(2,'G'),
    Shopping(3,'S'),
    Other(4,'O');

    private int id;
    private char idChar;

    private VisitorTourType(int id, char idChar) {
        this.id = id;
        this.idChar = idChar;
    }

    public int getID() {
        return id;
    }

    public char getIDChar() {
        return idChar;
    }

    public static VisitorTourType getTourType(char idChar) {
        switch(idChar) {
            case 'R' : return Recreation;
            case 'G' : return Gaming;
            case 'S' : return Shopping;
            default : return Other;
        }
    }

    public static VisitorTourType getTourType(int id) {
        switch(id) {
            case 1 : return Recreation;
            case 2 : return Gaming;
            case 3 : return Shopping;
            default :  return Other;
        }
    }

    public static boolean isIDCharValid(char idChar) {
        for (VisitorTourType type : values()) {
            if (idChar == type.getIDChar()) return true;
        }
        return false;
    }

    public static boolean isIDValid(int id) {
        return ((id > 0) && (id <= values().length));
    }

    public static short[] getTourTypeArray() {
        VisitorTourType[] types = values();
        short[] tourTypeArray = new short[types.length];
        for (int i = 0; i < types.length; i++) {
            tourTypeArray[i] = (short) types[i].getID();
        }
        return tourTypeArray;
    }
}
