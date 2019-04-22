package com.pb.tahoe.visitor.structures;

import com.pb.tahoe.visitor.structures.VisitorTour;

/**
 * User: Chris
 * Date: Mar 13, 2007 - 12:43:40 PM
 */
public interface VisitorPattern {
    void generatePatternAlternativeData();
    VisitorTour[] getToursFromPatternID(int id);
    String getPatternFromID(int id);
}
