# travel_demand_model

https://trpa-agency.github.io/travel_demand_model


Model installation

1. Download the repo to any location in the computer.

2. Navigate to the folder reference\code and open the file TahoeModel.lst in a text editor.

3. Edit the file to update the model directory location. For example if the model is installed in the directory C:\TahoeModelFolder, then this file should have the following lines.

    + C:\TahoeModelFolder\reference\code\emfac\gisdk\Utilities.rsc 
    + C:\TahoeModelFolder\reference\code\emfac\gisdk\Macro.rsc 
    + C:\TahoeModelFolder\reference\code\emfac\gisdk\AQuaVisUtilities.rsc 
    + C:\TahoeModelFolder\reference\code\emfac\gisdk\AQuaVisNetwork.rsc 
    + C:\TahoeModelFolder\reference\code\emfac\gisdk\AQuaVisIntrazonal.rsc 
    + C:\TahoeModelFolder\reference\code\emfac\gisdk\AQuaVisTrips.rsc 
    + C:\TahoeModelFolder\reference\code\emfac\gisdk\TahoeAQuaVis.rsc 
    + C:\TahoeModelFolder\reference\code\TahoeModelToolbox_rev.rsc 

4. Compile the .lst file updated above. To do this follow the following steps

* Click on Tools > GISDK Developer's Kit
* In the GISDK toolkit click "Compile to UI" button
* Navigate to reference\code folder and select the .lst file updated in step 3.
* Save As window will open up. Navigate to reference\code\ui and save as tahoemodelui.dbd

5. (Open transcad in admin mode) Setup the TransCAD UI - Tools > Setup > addins  - Click add button

 + Type = Macro
 + Description = TahoeModel
 + Name = TahoeABModel
 + UI = Browse to reference\code\ui\tahoemodelui.dbd

6. Update the model shortcut icon

* Right click the "Tahoe Activity Based Travel Demand Model" icon and click properties
* Update the Target field with your local directory information

7. Copy the scenario_base.zip to the reference folder 