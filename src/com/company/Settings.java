/*
Settings container to read and write XML files.

The Fraunhofer-Gesellschaft zur Foerderung der angewandten Forschung e.V.,
Hansastrasse 27c, 80686 Munich, Germany (further: Fraunhofer) is the holder
of all proprietary rights on this computer program. You can only use this
computer program if you have closed a license agreement with Fraunhofer or
you get the right to use the computer program from someone who is authorized
to grant you that right. Any use of the computer program without a valid
license is prohibited and liable to prosecution.

The use of this software is only allowed under the terms and condition of the
General Public License version 2.0 (GPL 2.0).

Copyright©2019 Gesellschaft zur Foerderung der angewandten Forschung e.V. acting
on behalf of its Fraunhofer Institut für  Kurzzeitdynamik. All rights reserved.

Contact: max.gulde@emi.fraunhofer.de
 */

package com.company;

import java.io.*;
import java.util.Locale;
import java.util.Properties;
import java.lang.String;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;

class Settings
{
    // Options
    static boolean fDisplayMessages = true;      // Display messages in console.

    // Physical constants
    static double EarthRadius = 6378140;               // Earth radius [m]

    // Files
    static Locale LocalFormat = new Locale("EN", "US");

    // Properties
    private Properties AppSettings;

    // Constructor
    Settings()
    {
        String PathRoot = "";
        String PathSettings = PathRoot + "set.xml";
        AppSettings = new Properties();
        // Try to load from file. If not successful, use default values.
        try
        {
            AppSettings.loadFromXML(new FileInputStream(PathSettings));
            System.out.println("Read settings file <" + PathSettings + ">.");
        }
        catch (FileNotFoundException e)
        {
            System.out.println("Warning: Settings file not found. Using default settings. " + e.toString());
            SetDefaultSettings();
            SaveToXML(PathSettings);
        }
        catch (IOException e)
        {
            System.out.println("Warning: Error reading settings file. Using default settings. " + e.toString());
            SetDefaultSettings();
            SaveToXML(PathSettings);
        }
    }

    // Make properties accessible within package.
    String GetValue(String key)
    {
        try {
            return AppSettings.getProperty(key);
        }
        catch (Exception e)
        {
            System.out.println("Error: Could not receive key <" + key + "> from properties database: " + e.toString());
            return AppSettings.getProperty(key);
        }
    }

    // Save properties to XML.
    private void SaveToXML(String path)
    {
        SimpleDateFormat Format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Timestamp Now = new Timestamp(System.currentTimeMillis());
        try
        {
            AppSettings.storeToXML(new FileOutputStream(path), "File generated on " + Format.format(Now));
        }
        catch (Exception e)
        {
            System.out.println("Warning: Could not write to settings file: " + e.toString());
        }
    }

    // Define default settings.
    private void SetDefaultSettings()
    {
        // Satellite orbit definition
        AppSettings.setProperty("SatAltitude", "700000");                               // (m)
        AppSettings.setProperty("SatEccentricity", "0");
        AppSettings.setProperty("SatInclination","98.1929");                            // (deg)
        AppSettings.setProperty("SatOmega", "0");                                       // (deg)
        AppSettings.setProperty("SatRAAN", "10.5834");                                  // (deg)
        AppSettings.setProperty("SatMeanAnomaly", "0");                                 // (deg)

        // Target definition
        AppSettings.setProperty("TargetLon", "7.846619");                               // (deg)
        AppSettings.setProperty("TargetLat", "48.001081");                              // (deg)
        AppSettings.setProperty("TargetAlt", "320");                                    // (m)

        // Simulation parameters
        AppSettings.setProperty("SimStartYear", "2020");                                // (a)
        AppSettings.setProperty("SimStartMonth", "01");                                 // (m)
        AppSettings.setProperty("SimStartDay", "01");                                   // (d)
        AppSettings.setProperty("SimStartHour", "00");                                  // (h)
        AppSettings.setProperty("SimStartMinute", "00");                                // (min)
        AppSettings.setProperty("SimStartSecond", "00.000");                            // (s.ms)
        AppSettings.setProperty("SimDurationInDays", "1.0");                            // (decimal days)
        AppSettings.setProperty("SimTimeStep", "60");                                   // (s)
        AppSettings.setProperty("SimMaxCheck", "60");                                   // (s)
        AppSettings.setProperty("SimDivThreshold", "0.001");                            // (s)
        AppSettings.setProperty("SimMinElevation", "0");                                // (deg)

        // Export options
        String SatelliteName = "ERNST";
        String StrIdentifier = "i98_a700";
        AppSettings.setProperty("SatelliteName", SatelliteName);
        AppSettings.setProperty("ExpFileSunAngles", SatelliteName + "_" + StrIdentifier + "_SunAngles.csv");
//        AppSettings.setProperty("ExpFileEarthAngles", SatelliteName + "_" + StrIdentifier + "_EarthAngles.csv");
        AppSettings.setProperty("ExpFileAccessTimes", SatelliteName + "_" + StrIdentifier + "_AccessTimes.csv");
        AppSettings.setProperty("ExpTextFormat", "UTF-8");
        AppSettings.setProperty("ResultsDirectory", "U:/3 Plattform/Thermal/2018-11 EQM Analyse/Data");
    }
}