package com.pb.tahoe.visitor.structures;

/**
 * User: Chris
 * Date: Feb 7, 2007 - 11:52:48 AM
 */
public enum VisitorMode implements java.io.Serializable  {
    Drive(1),
    Shuttle(2),
    WalkToTransit(3),
    DriveToTransit(4),
    Walk(5),
	Bike(6);
	
    private int id;

    private VisitorMode(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }

    public static VisitorMode getMode(int id) {
        switch(id) {
            case 1 : return Drive;
            case 2 : return Shuttle;
            case 3 : return WalkToTransit;
            case 4 : return DriveToTransit;
            case 5 : return Walk;
            default : return Bike;
        }
    }

    public static boolean isIdValid(int id) {
        return ((id > 0) && (id < values().length));
    }
}
