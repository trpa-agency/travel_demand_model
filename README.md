# travel_demand_model

https://trpa-agency.github.io/travel_demand_model


Model installation

1. Download the repo to any location in the computer.

2. Navigate to the folder reference\code and open the file TahoeModel.lst in a text editor.

3. Edit the file to update the model directory location. For example if the model is installed in the directory C:\TahoeModel\Release2019_v1, then this file should have the following lines.

    + C:\TahoeModel\Release2019_v1\reference\code\emfac\gisdk\Utilities.rsc 
    + C:\TahoeModel\Release2019_v1\reference\code\emfac\gisdk\Macro.rsc 
    + C:\TahoeModel\Release2019_v1\reference\code\emfac\gisdk\AQuaVisUtilities.rsc 
    + C:\TahoeModel\Release2019_v1\reference\code\emfac\gisdk\AQuaVisNetwork.rsc 
    + C:\TahoeModel\Release2019_v1\reference\code\emfac\gisdk\AQuaVisIntrazonal.rsc 
    + C:\TahoeModel\Release2019_v1\reference\code\emfac\gisdk\AQuaVisTrips.rsc 
    + C:\TahoeModel\Release2019_v1\reference\code\emfac\gisdk\TahoeAQuaVis.rsc 
    + C:\TahoeModel\Release2019_v1\reference\code\TahoeModelToolbox_rev.rsc 

4. Compile the .lst file updated above. To do this follow the following steps

    + navigate to eh transcad install directory in a command prompt window and execute the following command
        + rscc  -c -u C:\TahoeModel\Release2019_v1\reference\code\ui\tahoemodelui @c:\TahoeModel\Release2019_v1\reference\code\TahoeModel.lst

5. (Open transcad in admin mode) Setup the TransCAD UI - Tools > Setup > addins  - Click add button

    + Type = Macro
    + Description = TahoeModel
    + Name = TahoeABModel
    + UI = Browse to reference\code\ui\tahoemodelui.dbd

6. Update the model shortcut icon

    + Right click the "Tahoe Activity Based Travel Demand Model" icon and click properties
    + Update the Target field with your local directory information

7. Copy the scenario_base.zip to the reference folder 
