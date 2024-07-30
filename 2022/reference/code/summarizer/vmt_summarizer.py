# -*- coding: utf-8 -*-


import pandas as pd
import numpy as np
import geopandas as gpd
import os, sys
import shutil
import warnings
import openpyxl
import re
import matplotlib.pyplot as plt
import matplotlib.ticker as ticker
from openpyxl.styles import colors
from openpyxl.styles import Font, Color
import yaml
import folium
from folium import FeatureGroup, LayerControl, Map, Marker
from folium.features import DivIcon
from shutil import copyfile
from bokeh.layouts import column
from bokeh.plotting import figure, output_file, save
from bokeh.models import ColumnDataSource, Select, TableColumn, DataTable, Paragraph, NumberFormatter, Div, LabelSet
from bokeh.io import show
from bokeh.models.layouts import TabPanel, Tabs
from bokeh.io import output_file, show
from bokeh.layouts import row
from bokeh.models.callbacks import CustomJS

warnings.filterwarnings("ignore")  
_join = os.path.join
#TODO for documentation add that model must be setup before lu_converter is used


class VMTSummary(object):

    def __init__(self, scen_name):
        self.skim_trip_file = pd.DataFrame()
        self.full_trip_file = pd.DataFrame()
        self.xwalk = pd.DataFrame()
        self.res_hh = pd.DataFrame()
        self.res_pop = pd.DataFrame()
        self.vis_on = pd.DataFrame()
        self.vis_day = pd.DataFrame()
        self.vis_thru = pd.DataFrame()
        self.ext_pop = pd.DataFrame()
        self.TAZ = gpd.GeoDataFrame()
        self.scen_name = scen_name
        self.mdldir = _join(os.getcwd(),'..','..','..','scenarios',self.scen_name)

    def __call__(self):
        self.read_inputs()
        
        



    def add_choro_group(self,map,df,group,Name, col, color,  level = 'TAZ'):
        """Add choropleth group to map"""
        style_function = lambda x: {'fillColor': '#ffffff', 
                            'color':'#000000', 
                            'fillOpacity': 0.1, 
                            'weight': 0.1}
        highlight_function = lambda x: {'fillColor': '#000000', 
                                'color':'#000000', 
                                'fillOpacity': 0.50, 
                                'weight': 0.1}
        
        geo = self.TAZ.merge(self.xwalk, how = 'left', on = 'TAZ').dissolve(by = level)
        df = geo.merge(df[df[col] > 0][[level,col]], how = 'left', on = level)
        df = df.dropna()
        df[col] = df[col].map(lambda x: round(x,2))
        if len(df) > 0:
            mapgroup = folium.Choropleth(
                geo_data = df,
                name = Name,
                data = df,
                columns = [level, col],
                key_on = 'feature.properties.{}'.format(level),
                fill_color = color,
                fill_opacity = 0.7,
                line_opacity = 0.2,
                legend_name = Name, 
                highlight = True
            )
            mapgroup.geojson.add_to(group)
            
            tool_tip = folium.features.GeoJson(
                df,
                style_function=style_function, 
                control=False,
                highlight_function=highlight_function, 
                tooltip=folium.features.GeoJsonTooltip(
                    fields=[level, col],
                    aliases=['{}: '.format(level),'{}: '.format(col)],
                    style=("background-color: white; color: #333333; font-family: arial; font-size: 12px; padding: 10px;") 
                )
            )


            tool_tip.add_to(group)
            group.add_to(map)

    def add_boundary_group(self, map, df, group, color, col):
        """ Add boundary layer to map"""
        mgra_bound = folium.GeoJson(df, 
                      style_function=lambda feature: {
                      'fillColor': None,
                      'color' : color,
                      'weight' : 2,
                      'fillOpacity' : 0,
                      }).add_to(group)
        for i,row in df.iterrows():
                lat = row['geometry'].centroid.y
                lon = row['geometry'].centroid.x
                text = row[col]
                folium.map.Marker(
                    [lat, lon],
                    icon=DivIcon(
                        icon_size=(150,36),
                        icon_anchor=(0,0),
                        html='<div style="font-size: 8pt">%s</div>' % text,
                        )
                    ).add_to(group)
        group.add_to(map)


        
    def read_inputs(self):    
        """ Read inputs for the tool """
        self.TAZ = gpd.read_file(_join(self.mdldir,'gis','layers','TAZ','TAZ.shp' ))
        self.TAZ = self.TAZ.to_crs({'init':'epsg:4326'})
        self.TAZ = self.TAZ.drop('REGION',axis = 1)
        self.TAZ['x'] = self.TAZ.geometry.representative_point().x
        self.TAZ['y'] = self.TAZ.geometry.representative_point().y
        self.xwalk = pd.read_csv(_join(self.mdldir,'gis','tahoe_geo_crosswalk.csv' ))
        self.skim_trip_file = pd.read_csv(_join(self.mdldir,'outputs_summer','reports','trip_file_juris.csv' ))
        self.full_trip_file = pd.read_csv(_join(self.mdldir,'outputs_summer','trip_file.csv' ))
        self.res_hh = pd.read_csv(_join(self.mdldir,'outputs_summer','SynPopH_full.csv' ))
        self.res_pop = pd.read_csv(_join(self.mdldir,'outputs_summer','SynPopP.csv' ))
        self.vis_on = pd.read_csv(_join(self.mdldir,'outputs_summer','OvernightVisitorSynpopWithPattern.csv' ))
        self.vis_day = pd.read_csv(_join(self.mdldir,'outputs_summer','DayVisitorSynpopWithPattern.csv' ))
        self.vis_thru = pd.read_csv(_join(self.mdldir,'outputs_summer','ThruVisitorSynpopWithDTM.csv' ))
        self.ext_pop = pd.read_csv(_join(self.mdldir,'outputs_summer','ExternalWorkerSynpopWithOT.csv' ))
        self.xwalk['REGION'] = 'Lake Tahoe Region'
        



    def mode_share(self, level = 'REGION', pop = 'all'):
        """ calculate mode_share at different Geographic layers"""
        order_dict = {'drive alone':1, 'drive to transit':3, 'non motorized':5, 'school bus':6,
               'shared auto':2, 'visitor shuttle':7, 'walk to transit':4}
        trips = self.full_trip_file.merge(self.xwalk, how = 'left', left_on = 'startTaz', right_on = 'TAZ')
        trips['REGION'] = 'Lake Tahoe Region'
        if pop == 'all': 
            mode_share = trips.groupby([level,'mode'],as_index = False)[['tripID']].count()
            mode_share.columns = [level,'Trip Mode','Trips']

        else:
            try:
                trips = trips[trips.partyType.isin(pop)]
                assert not len(trips) == 0
                mode_share = trips.groupby([level,'mode'],as_index = False)[['tripID']].count()
                mode_share.columns = [level,'Trip Mode','Trips']
            except:
                print("No Party Type {}, try party type in {}".format(pop,self.full_trip_file.partyType.unique()))
        mode_share['order'] = mode_share['Trip Mode'].map(order_dict)
        mode_share = mode_share.sort_values('order')
        mode_share = mode_share.drop('order',axis = 1)
        mode_share['Trips'] = mode_share['Trips']/mode_share['Trips'].sum()
        return mode_share.drop(level, axis = 1).set_index('Trip Mode')
    def population_density(self, level = 'TAZ', residents = True):
        """ show distribution of residents and visitors """
        if residents:
            pop = pd.merge(self.res_pop, self.res_hh, how = 'left', on = 'hh_id')
            group_col = 'hh_taz_id'
            id_col = 'hh_id'
        else:
            pop = self.vis_on.copy()
            group_col = 'stayTAZ'
            id_col = 'id'
        taz = self.TAZ.merge(self.xwalk, how = 'left', on = 'TAZ')
        geo = taz.dissolve(by = level)
        pop = pop.merge(self.xwalk, how = 'left', left_on = group_col, right_on = 'TAZ')
        pop = pop.groupby(level,as_index = False)[id_col].count()
        pop.columns = [level,'Population']
        geo = geo.merge(pop, how = 'right', on = level)
        return geo

    def vmt_per_capita(self, level = 'TAZ', residents = True, external_distance = False):
        """ calculate resident and overnight visitor VMT per capita at different Geographic layers"""
        trips = self.skim_trip_file.merge(self.full_trip_file[['tripID','partyID']], how = 'inner', on = 'tripID')
        # trips = trips.merge(self.xwalk, left_on = 'startTaz', right_on = 'TAZ')
        if residents:
            pop = pd.merge(self.res_pop, self.res_hh, how = 'left', on = 'hh_id')
            poptype = 'resident'
            taz_col = 'hh_taz_id'
            id_col = 'hh_id'
        else:
            pop = self.vis_on.copy()
            poptype = 'overnight visitor'
            taz_col = 'stayTAZ'
            id_col = 'id'
        len_col = 'internal_distance'
        if external_distance:
            len_col = 'total_distance'
        pop = pop.merge(self.xwalk, how = 'inner', left_on = taz_col, right_on = 'TAZ')
        trips = trips[trips.partyType == poptype].merge(pop[[id_col,taz_col,level]].drop_duplicates(), how = 'inner', left_on = 'partyID', right_on = id_col)
        trips = trips.groupby(level, as_index = False).agg({'tripID':'count',len_col:'sum'})
        pop = pop.groupby(level, as_index = False)[[id_col]].count()
        trips.columns = [level, 'Trips','VMT']
        pop.columns = [level, 'Population']
        vmt_per_capita = trips.merge(pop, how='left', on=level)
        vmt_per_capita['VMT per Capita'] = vmt_per_capita['VMT']/vmt_per_capita['Population']
        
        taz = self.TAZ.merge(self.xwalk, how = 'left', on = 'TAZ')
        taz = taz.dissolve(by=level).merge(vmt_per_capita, how = 'inner', on = level)
        return taz
        
    def regional_vmt(self, total_label='Grand Total',
                     index_sequence=['resident', 'overnight visitor', 'day visitor', 'thru visitor', 
                                     'seasonal', 'external worker'],
                     col_relabel={'tripID': 'Trips',
                                  'internal_distance': 'Internal VMT', 
                                  'external_distance': 'External VMT', 
                                  'total_distance': 'Total VMT'},
                     col_sequence=['Persons', 'Trips', 'Trip Rate', 'Avg Int Distance', 
                                   'Internal VMT', 'Int VMT Share', 'Avg Ext Distance', 
                                   'External VMT', 'Ext VMT Share', 'Avg Trip Distance', 'Total VMT', 
                                   'Total VMT Share']):
        
        """ calculate region-wide summary table by partyType including vmt, avg vmt, share and trip rate """
        trips = self.skim_trip_file.merge(self.full_trip_file[['tripID', 'partyID']], how = 'inner', on = 'tripID')
        trips_pivot = pd.pivot_table(trips, 
                                     values=['tripID', 'internal_distance', 'external_distance', 'total_distance'], 
                                     index='partyType', 
                                     aggfunc={'tripID':'count', 
                                              'internal_distance': 'sum', 
                                              'external_distance': 'sum', 
                                              'total_distance': 'sum'}, 
                                     margins=False, 
                                     margins_name=total_label)
        
        trips_pivot['Persons'] = 1
        trips_pivot.loc['resident', 'Persons'] = len(self.res_pop)
        trips_pivot.loc['overnight visitor', 'Persons'] = len(self.vis_on[self.vis_on['stayType']!=1])
        trips_pivot.loc['day visitor', 'Persons'] = len(self.vis_day)
        trips_pivot.loc['thru visitor', 'Persons'] = len(self.vis_thru)
        trips_pivot.loc['seasonal', 'Persons'] = len(self.vis_on[self.vis_on['stayType']==1])
        trips_pivot.loc['external worker', 'Persons'] = len(self.ext_pop)
        
        trips_pivot.loc[total_label] = trips_pivot.sum()
        # trips_pivot = trips_pivot.round().astype(int).rename(col_relabel, axis=1)
        trips_pivot.rename(col_relabel, axis=1, inplace=True)
        
        trips_pivot['Avg Int Distance'] = trips_pivot['Internal VMT']/trips_pivot['Trips']
        trips_pivot['Int VMT Share'] = trips_pivot['Internal VMT']/trips_pivot.loc[total_label, 'Internal VMT']
        trips_pivot['Avg Ext Distance'] = trips_pivot['External VMT']/trips_pivot['Trips']
        trips_pivot['Ext VMT Share'] = trips_pivot['External VMT']/trips_pivot.loc[total_label, 'External VMT']
        trips_pivot['Avg Trip Distance'] = trips_pivot['Total VMT']/trips_pivot['Trips']
        trips_pivot['Total VMT Share'] = trips_pivot['Total VMT']/trips_pivot.loc[total_label, 'Total VMT']
                
        trips_pivot['Trip Rate'] = trips_pivot['Trips']/trips_pivot['Persons']
        
        trips_pivot = trips_pivot.loc[index_sequence + [total_label], col_sequence]
        trips_pivot.reset_index(inplace=True)
                
        return trips_pivot
    
    def vmt_per_geo(self, level = 'TAZ'):
        """ calculate resident and overnight visitor VMT per capita at different Geographic layers"""
        trips = self.trip_file.copy()
        residents = pd.merge(self.res_pop, self.res_hh, how = 'left', on = 'hh_id')
        visitors = pd.concat([self.vis_on, self.vis_day, self.vis_thru], ignore_index = True)
        taz = self.TAZ.merge(self.xwalk, how = 'left', on = 'TAZ')

    def qa_qc_map(self):
        """ Create HTML QA/QC maps to show VMT and Mode Share """
        #format final compare dataframes
        mode_share = self.mode_share(level = 'REGION')
        mode_share_vis = self.mode_share(level = 'REGION', pop = ['overnight visitor'])
        mode_share_res = self.mode_share(level = 'REGION', pop = ['resident'])
        mode_share_xw = self.mode_share(level = 'REGION', pop = ['external worker'])
        regional_vmt_tab = self.regional_vmt()
        

        # Sample data
        data = mode_share.reset_index()[['Trip Mode','Trips']]
        data2 = mode_share_vis.reset_index()[['Trip Mode','Trips']]
        data3 = mode_share_res.reset_index()[['Trip Mode','Trips']]
        data4 = mode_share_xw.reset_index()[['Trip Mode','Trips']]
        data['category'] = 'All_Trips'
        data2['category'] = 'Overnight_Visitor_Trips'
        data3['category'] = 'Resident_Trips'
        data4['category'] = 'External_Worker_Trips'
        
        final_data = pd.concat([data, data2,data3,data4],ignore_index = True)
        final_data = final_data.rename(columns = {'Trip Mode':'Trip_Mode'})
        final_data['label'] = final_data['Trips'].map(lambda x: '{}%'.format(round(x*100,2)))
 
        # Get unique categories
        categories = ['dummy'] + list(final_data['category'].unique())

        # Create separate ColumnDataSources for each category
        data_sources = {}
        for category in categories:
            if category == 'dummy':
                data_sources[category] = ColumnDataSource(data=final_data[final_data['category'] == 'All_Trips'])
            else:
                data_sources[category] = ColumnDataSource(data=final_data[final_data['category'] == category])
        # Create initial plot with the first category
        selected_category = categories[0]
        source = data_sources[selected_category]

        # Create figure
        p = figure(x_range=source.data['Trip_Mode'],  title="Trip_Mode and Trips by Category",
                   toolbar_location=None, tools="")
        
        # Add vbar glyphs
        p.vbar(x='Trip_Mode', top='Trips', width=0.9, source=source, legend_label="Trips")
        labels = LabelSet(x='Trip_Mode', y='Trips', text='label', level='glyph',
                  x_offset=-13.5, y_offset=0, source=source)
        p.add_layout(labels)
        # Dropdown to select category
        dropdown = Select(title="Select Category:", value=selected_category, options=list(categories[1:]))

        # JavaScript code to handle Dropdown change event
        dropdown.js_on_change('value', CustomJS(args=dict(data_sources=data_sources, source=source, p=p, labels=labels), code="""
            var selected_category = cb_obj.value;
            var new_source = data_sources[selected_category];
            
            // Update the data source
            source.data = new_source.data;
            
            // Update the x_range factors to the new Trip Modes
            p.x_range.factors = new_source.data['Trip_Mode'];
            
            // Update the y_range to accommodate the new data
            p.y_range.start = 0;
            p.y_range.end = Math.max.apply(null, new_source.data['Trips']) * 1.1;
            
            // Update labels
            labels.source = source;
            
            // Emit changes
            source.change.emit();
            p.change.emit();
        """))

        # Layout
        layout = column(dropdown, p)
        # Create a TabPanel with the combined layout
        tab = TabPanel(child=layout, title="Mode Share")
        
        title1 = Paragraph(text="Aggregated Internal, External, and Total VMT by Party Type")
        source1 = ColumnDataSource(regional_vmt_tab)
        columns1 = []
        for col in regional_vmt_tab.columns:
            if col in ['Persons', 'Trips', 'Internal VMT', 'External VMT', 'Total VMT']:
                columns1 += [TableColumn(field=col, title=col, formatter=NumberFormatter(text_align = "right"))]
            elif col in ['Int VMT Share', 'Ext VMT Share', 'Total VMT Share']:
                columns1 += [TableColumn(field=col, title=col, formatter=NumberFormatter(format="0.0%", text_align = "right"))]
            elif col in ['partyType']:
                columns1 += [TableColumn(field=col, title=col)]
            else:
                columns1 += [TableColumn(field=col, title=col, formatter=NumberFormatter(format="0.0", text_align = "right"))]
                
        table1 = DataTable(source=source1, columns=columns1, index_position=None, margin=(5, 5, -5, 5), width=1200)#, autosize_mode="fit_viewport", height=table_height)
        layout1 = column(title1, table1)
        tab_regional = TabPanel(child=layout1, title="Regional VMT")
        
        tablist = [tab, tab_regional]
        
        ### Write summaries to excel workbook
        writer = pd.ExcelWriter(_join(self.mdldir, 'outputs_summer', 'reports', '{}_workbook.xlsx'.format(self.scen_name)), engine='xlsxwriter')
        workbook = writer.book
                
        for geography in self.xwalk.columns:
            #Create HTML map to visualize vmt and population density changes
            update_map = folium.Map(location=[self.TAZ['y'].mean(), self.TAZ['x'].mean()],  tiles = 'cartodbpositron')
            sw = self.TAZ[['y', 'x']].min().values.tolist()
            ne = self.TAZ[['y', 'x']].max().values.tolist()
    
            update_map.fit_bounds([sw, ne]) 
            loc = 'Land Use Updates'
            title_html = '''
                          <h3 align="center" style="font-size:16px"><b>{}</b></h3>
                          '''.format(loc)   
    
            update_map.get_root().html.add_child(folium.Element(title_html))
            res_group_taz        = FeatureGroup(name = "Residents", show = True)
            vis_group_taz        = FeatureGroup(name = "Overnight Visitors", show = False)
            res_pop_group_taz    = FeatureGroup(name = "Resident Population", show = False)
            vis_pop_group_taz    = FeatureGroup(name = "Visitor Population", show = False)
            res_ext_group_taz    = FeatureGroup(name = "Resident with External", show = False)
            vis_ext_group_taz    = FeatureGroup(name = "Visitor with External", show = False)
            
            ### Calculate data for choro groups
            res_group_df        = self.vmt_per_capita(level = geography, residents = True)
            vis_group_df        = self.vmt_per_capita(level = geography, residents = False)
            res_pop_group_df    = self.population_density(level = geography, residents = True)
            vis_pop_group_df    = self.population_density(level = geography, residents = False)
            res_ext_group_df    = self.vmt_per_capita(level = geography, residents = True, external_distance=True)
            vis_ext_group_df    = self.vmt_per_capita(level = geography, residents = False, external_distance=True)
            
            ### Merge all groups to one dataframe
            output_df = pd.merge(res_group_df[[geography, 'VMT per Capita']].rename({'VMT per Capita': 'VMT per Capita [Residents]'}, axis=1), 
                                 vis_group_df[[geography, 'VMT per Capita']].rename({'VMT per Capita': 'VMT per Capita [Overnight Visitors]'}, axis=1), 
                                 how='outer', on=geography)
            output_df = pd.merge(output_df, 
                                 res_ext_group_df[[geography, 'VMT per Capita']].rename({'VMT per Capita': 'VMT per Capita [Resident with External]'}, axis=1), 
                                 how='outer', on=geography)
            output_df = pd.merge(output_df, 
                                 vis_ext_group_df[[geography, 'VMT per Capita']].rename({'VMT per Capita': 'VMT per Capita [Visitor with External]'}, axis=1), 
                                 how='outer', on=geography)
            output_df = pd.merge(output_df, 
                                 res_pop_group_df[[geography, 'Population']].rename({'Population': 'Resident Population'}, axis=1), 
                                 how='outer', on=geography)
            output_df = pd.merge(output_df, 
                                 res_pop_group_df[[geography, 'Population']].rename({'Population': 'Visitor Population'}, axis=1), 
                                 how='outer', on=geography)
            
            ### Save data to excel workbook
            worksheet_name = '{} VMT Map'.format(geography) # Worksheet name must be less than 32 characters
            worksheet = workbook.add_worksheet(worksheet_name)
            writer.sheets[worksheet_name] = worksheet
            output_df.to_excel(writer, sheet_name=worksheet_name, startrow=0, startcol=0, index=False)
        
            #TODO Add VMT per Capita for Residents and visitors WITH external distance
            self.add_choro_group(update_map, res_group_df, res_group_taz, 'Resident VMT per Capita', 'VMT per Capita', 'YlOrRd', level = geography)
            self.add_choro_group(update_map, vis_group_df, vis_group_taz, 'Visitor VMT per Capita', 'VMT per Capita', 'YlOrRd', level = geography)
            self.add_choro_group(update_map, res_pop_group_df, res_pop_group_taz, 'Residents', 'Population', 'YlOrRd', level = geography)
            self.add_choro_group(update_map, vis_pop_group_df, vis_pop_group_taz, 'Visitor', 'Population', 'YlOrRd', level = geography)
            self.add_choro_group(update_map, res_ext_group_df, res_ext_group_taz, 'Resident with External VMT per Capita', 'VMT per Capita', 'YlOrRd', level = geography)
            self.add_choro_group(update_map, vis_ext_group_df, vis_ext_group_taz, 'Visitor with External VMT per Capita', 'VMT per Capita', 'YlOrRd', level = geography)
            
            
            folium.LayerControl(collapsed = False).add_to(update_map)
            f = folium.Figure(width = 1800, height = 900)
            m = update_map.add_to(f)
    
            div = Div(text=m._repr_html_())
    
            layoutvmt = column(div, sizing_mode="stretch_both")
            tab2 = TabPanel(child = layoutvmt, title = '{} VMT Map'.format(geography))
            tablist = tablist + [tab2]        # Create Tabs
        tabs = Tabs(tabs=tablist)
        
        ### Close excel workbook
        # writer.save()
        writer.close()
                
        # Output the result
        output_file(_join(self.mdldir,'outputs_summer','reports','{}_visualizer.html'.format(self.scen_name)))
        show(tabs)
        
if __name__ == "__main__":
    luc = VMTSummary(sys.argv[1])
    luc()
    luc.qa_qc_map()