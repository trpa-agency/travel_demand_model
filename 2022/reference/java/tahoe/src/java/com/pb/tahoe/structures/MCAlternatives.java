/*
 * Copyright  2005 PB Consult Inc.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package com.pb.tahoe.structures;

import java.io.Serializable;

/**
 * @author Wu Sun
 * <sunw@pbworld.com>
 *
 */
public final class MCAlternatives implements Serializable {

    public static final String [] MCAlts={"SOV","HOV","Walk_trn","Drive_trn","Non_motor","Schl_bus"};

    public static int getNoMCAlternatives(){
        return MCAlternatives.MCAlts.length;
    }

    /**
     * get MC alternative by index
     * @param index, importatn 1-based
     * @return mode alternative
     */
    public static String getMCAlt(int index){
        return MCAlternatives.MCAlts[index-1];
    }

    /**
     * given MC alternative name, get alternative index
     * @param altName
     * @return index of alternative
     */
    public static int getMCAltIndex(String altName){
        int index=-1;
        for(int i=0; i<MCAlternatives.MCAlts.length; i++){
            if(altName.equalsIgnoreCase(MCAlternatives.MCAlts[i])){
                index=i+1;
                break;
            }
        }
        return index;
    }
}
