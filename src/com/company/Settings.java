/*
Settings container to read and write XML files
Last modified: 2018-11-20
Author: Max Gulde, max.gulde@emi.fraunhofer.de
 */

package com.company;

import java.io.*;
import java.util.Locale;
import java.util.Properties;
import java.lang.String;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;

public class Settings
{
    // Options
    public static boolean fDisplayMessages = true;      // Display messages in console.

    // Physical contants
    public static double EarthGM = 3.986004415e+14;     // Earth's gravitational parameter [m^3 s^-2]
    public static double EarthRadius = 6378140;               // Earth radius [m]

    // Files
    public static Locale LocalFormat = new Locale("EN", "US");
    private String FileSettings = "set.xml";

    // Properties
    public Properties AppSettings;

    // Constructor
    public Settings()
    {
        String PathRoot = "";
        String PathSettings = PathRoot + FileSettings;
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
            AppSettings.storeToXML(new FileOutputStream(path), String.format("File generated on " + Format.format(Now)));
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
        AppSettings.setProperty("SimMinElevation", "0");                               // (deg)

        // Export options
        String SatelliteName = "ERNST";
        String StrIdentifier = "i98_1929_a700";
        AppSettings.setProperty("SatelliteName", SatelliteName);
        AppSettings.setProperty("ExpFileSunAngles", SatelliteName + "_" + StrIdentifier + "_SunAngles.csv");
        AppSettings.setProperty("ExpFileEarthAngles", SatelliteName + "_" + StrIdentifier + "_EarthAngles.csv");
        AppSettings.setProperty("ExpFileAccessTimes", SatelliteName + "_" + StrIdentifier + "_AccessTimes.csv");
        AppSettings.setProperty("ExpTextFormat", "UTF-8");
    }
}