/*
Determine satellite attitude for a given orbit with respect to Earth and Sun. Lists accesses to ground station.
Based on the SOA_Example from Javier Montemayor
Author: Max Gulde, max.gulde@emi.fraunhofer.de
Licensed under GNU GPL-2.0-only.
 */

package com.company;

import java.io.*;
import org.orekit.data.*;
import org.orekit.frames.*;
import org.orekit.time.*;
import org.orekit.orbits.*;
import org.orekit.propagation.*;
import org.orekit.propagation.analytical.*;
import org.orekit.bodies.*;
import org.orekit.utils.*;
import org.orekit.propagation.events.*;
import org.hipparchus.geometry.euclidean.threed.*;

public class Main
{
    public static void main(String[] args) throws Exception {

        // Fields
        CelestialBody Sun, Earth;
        Frame InertialFrame, EarthFrame;
        Orbit InitialOrbit;
        PrintWriter SunAngles, EarthAngles;
        AbsoluteDate TimeStart, TimeEnd;

        // Load parameters from settings file
        Settings Set = new Settings();

        // Performance timers
        long SimEndTime, SimStartTime = System.currentTimeMillis();

        // Orekit configuration
        File OrekitData = new File("Libraries/orekit-data"); // The argument indicates the path to the config folder
        DataProvidersManager Manager = DataProvidersManager.getInstance();
        Manager.addProvider(new DirectoryCrawler(OrekitData));
        if (Settings.fDisplayMessages) {
            System.out.println("Loaded orekit configuration.");
        }

        // Create inertial frame for Earth, celestial bodies, and ground station
        InertialFrame = FramesFactory.getEME2000(); // J2000, centered at Earth's center, moving with surface rotation
        Sun = CelestialBodyFactory.getSun();
        Earth = CelestialBodyFactory.getEarth();
        if (Settings.fDisplayMessages) {
            System.out.println("Created Earth inertial frame.");
        }

        // Set up time frame
        TimeStart = new AbsoluteDate(Integer.parseInt(Set.GetValue("SimStartYear")),
                Integer.parseInt(Set.GetValue("SimStartMonth")),
                Integer.parseInt(Set.GetValue("SimStartDay")),
                Integer.parseInt(Set.GetValue("SimStartHour")),
                Integer.parseInt(Set.GetValue("SimStartMinute")),
                Double.parseDouble(Set.GetValue("SimStartSecond")), TimeScalesFactory.getUTC());
        double SimDurationInSec = Double.parseDouble(Set.GetValue("SimDurationInDays")) * 3600 * 24;
        TimeEnd = TimeStart.shiftedBy(SimDurationInSec);

        // Set up satellite orbit
        double SatAltitude = Double.parseDouble(Set.GetValue("SatAltitude")) + Settings.EarthRadius;
        double SatEccentricity = Double.parseDouble(Set.GetValue("SatEccentricity"));
        double SatInclination = Double.parseDouble(Set.GetValue("SatInclination"));
        double SatOmega = Double.parseDouble(Set.GetValue("SatOmega"));
        double SatRAAN = Double.parseDouble(Set.GetValue("SatRAAN"));
        double SatMeanAnomaly = Double.parseDouble(Set.GetValue("SatMeanAnomaly"));
        InitialOrbit = new KeplerianOrbit(SatAltitude, SatEccentricity, Math.toRadians(SatInclination),
                Math.toRadians(SatOmega), Math.toRadians(SatRAAN), Math.toRadians(SatMeanAnomaly),
                PositionAngle.MEAN, InertialFrame, TimeStart, Settings.EarthGM);
        if (Settings.fDisplayMessages) {
            System.out.println("Created satellite orbit:");
            System.out.println("\tSatAltitude = " + SatAltitude);
            System.out.println("\tSatEccentricity = " + SatEccentricity);
            System.out.println("\tSatInclination = " + SatInclination);
            System.out.println("\tSatOmega = " + SatOmega);
            System.out.println("\tSatRAAN = " + SatRAAN);
            System.out.println("\tSatMeanAnomaly = " + SatMeanAnomaly);
        }

        // Set up Keplerian propagator
        KeplerianPropagator Kepler = new KeplerianPropagator(InitialOrbit);
        Kepler.setSlaveMode();
        if (Settings.fDisplayMessages) {
            System.out.println("Created Keplerian propagator.");
        }

        // Setting up files and print files header
        SunAngles = new PrintWriter(Set.GetValue("ExpFileSunAngles"),Set.GetValue("ExpTextFormat"));
        EarthAngles = new PrintWriter(Set.GetValue("ExpFileEarthAngles"),Set.GetValue("ExpTextFormat"));
        SunAngles.println("\"Time (UTCG)\",\"Azimuth (deg)\",\"Elevation (deg)\",\"Subsolar (deg)\"");
        EarthAngles.println("\"Time (UTCG)\",\"Azimuth (deg)\",\"Elevation (deg)\"");
        if (Settings.fDisplayMessages) {
            System.out.println("Created satellite orientation files.");
        }

        // Create ground station
        double TargetLon = Double.parseDouble(Set.GetValue("TargetLon"));
        double TargetLat = Double.parseDouble(Set.GetValue("TargetLat"));
        double TargetAlt = Double.parseDouble(Set.GetValue("TargetAlt"));
        GeodeticPoint GroundStation = new GeodeticPoint(Math.toRadians(TargetLat), Math.toRadians(TargetLon), TargetAlt);
        // Topocentric frame for the ground station.
        EarthFrame = FramesFactory.getITRF(IERSConventions.IERS_2010, true);
        BodyShape EarthShape = new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                Constants.WGS84_EARTH_FLATTENING,
                EarthFrame);
        TopocentricFrame GroundStationFrame = new TopocentricFrame(EarthShape, GroundStation, "Ground Station");
        if (Settings.fDisplayMessages) {
            System.out.println("Created ground station at Lat = " + Math.toDegrees(GroundStation.getLatitude()) + " deg, Lon = " + Math.toDegrees(GroundStation.getLongitude()) + " deg, Alt = " + GroundStation.getAltitude() + "m.");
        }

        // Set up event detector
        VisibilityHandler VisH = new VisibilityHandler(Set);
        double MinElevation = Double.parseDouble(Set.GetValue("SimMinElevation"));
        double MaxCheckInterval = Double.parseDouble(Set.GetValue("SimMaxCheck"));
        double MaxDivThreshold = Double.parseDouble(Set.GetValue("SimDivThreshold"));
        EventDetector StationVisibility = new ElevationDetector(MaxCheckInterval,MaxDivThreshold,
                GroundStationFrame).withConstantElevation(Math.toRadians(MinElevation)).withHandler(VisH);
        // Add event detector to the propagator.
        Kepler.addEventDetector(StationVisibility);
        if (Settings.fDisplayMessages) {
            System.out.println("Created elevation detector with min elevation = " + MinElevation + " deg, max check interval = " + MaxCheckInterval + " s, divergence threshold = " + MaxDivThreshold + " s.");
        }

        // Start propagation
        double dt = Double.parseDouble(Set.GetValue("SimTimeStep"));
        double TimeSteps = Math.round(SimDurationInSec / dt);
        if (Settings.fDisplayMessages) {
            System.out.println("Start time: " + TimeStart.toString());
            System.out.println("End time: " + TimeEnd.toString());
            System.out.println("Time step: " + dt);
            System.out.print("Propagating ");
        }

        for (int i = 0; i < TimeSteps; i++) {
            if (i % (int)Math.round(TimeSteps / 10) == 0)
            {
                System.out.print(".");
            }
            AbsoluteDate t = TimeStart.shiftedBy(i * dt);

            // Propagate S/C
            SpacecraftState CurrentSCState = Kepler.propagate(t);

            // Get the date & time
            AbsoluteDate AbsDate = CurrentSCState.getDate();
            DateComponents DateComps = AbsDate.getComponents(0).getDate();
            TimeComponents TimeComps = AbsDate.getComponents(0).getTime();

            // Get month string.
            String StrMonth = DateComps.getMonthEnum().getCapitalizedAbbreviation();

            // Get the satellite reference frame.
            LocalOrbitalFrame SatelliteFrame = new LocalOrbitalFrame(InertialFrame, LOFType.VVLH,
                            CurrentSCState.getPVCoordinates().toTaylorProvider(InertialFrame), "Satellite Frame");

            // Get position vectors.
            Vector3D Earth2Sun = Sun.getPVCoordinates(AbsDate, InertialFrame).getPosition();
            Vector3D Sat2Earth = Earth.getPVCoordinates(AbsDate, SatelliteFrame).getPosition();
            Vector3D Sat2Sun = Sun.getPVCoordinates(AbsDate, SatelliteFrame).getPosition();
            Vector3D SatPosition = CurrentSCState.getPVCoordinates().getPosition();

            // Compute solar angles (deg)
            double Subsolar, SunAzimuth, SunElevation;
            Subsolar = Math.toDegrees(Vector3D.angle(Earth2Sun, SatPosition));
            SunAzimuth = Math.toDegrees(Vector3D.angle(Vector3D.PLUS_I, Sat2Sun.add(new Vector3D(0.0, 0.0, -Sat2Sun.getZ()))));
            SunElevation = Math.toDegrees(Vector3D.angle(Sat2Sun, new Vector3D(Sat2Sun.getX(), Sat2Sun.getY(), 0.0)));
            // Check sign
            if (Sat2Sun.getY() < 0) {
                SunAzimuth = 360 - SunAzimuth;
            }
            if (Sat2Sun.getZ() < 0) {
                SunElevation = -SunElevation;
            }
            // Write to solar angles file.
            SunAngles.printf(Settings.LocalFormat, "%d %s %d %02d:%02d:%06.3f,%07.3f,%07.3f,%07.3f\n",
                    DateComps.getDay(), StrMonth, DateComps.getYear(), TimeComps.getHour(), TimeComps.getMinute(),
                    TimeComps.getSecond(), SunAzimuth, SunElevation, Subsolar);

            // Compute Earth angles (deg)
            double EarthAzimuth, EarthElevation;
            if (Sat2Earth.getX() == 0 & Sat2Earth.getY() == 0) { // Prevents crashing when X and Y are both 0
                EarthAzimuth = 0.0;
                EarthElevation = 90.0;
            } else {
                EarthAzimuth = Math.toDegrees(Vector3D.angle(Vector3D.PLUS_I, Sat2Earth.add(new Vector3D(0.0, 0.0, -Sat2Earth.getZ()))));
                EarthElevation = Math.toDegrees(Vector3D.angle(Sat2Earth, new Vector3D(Sat2Earth.getX(), Sat2Earth.getY(), 0.0)));
            }
            // Check sign
            if (Sat2Earth.getY() < 0) {
                EarthAzimuth = 360 - EarthAzimuth;
            }
            if (Sat2Earth.getZ() < 0) {
                EarthElevation = -EarthElevation;
            }

            // Write to Earth angles file.
            EarthAngles.printf(Settings.LocalFormat, "%d %s %d %02d:%02d:%06.3f,%07.3f,%07.3f\n",
                    DateComps.getDay(), StrMonth, DateComps.getYear(), TimeComps.getHour(), TimeComps.getMinute(),
                    TimeComps.getSecond(),EarthAzimuth, EarthElevation);
        }

        // Close files.
        SunAngles.close();
        EarthAngles.close();
        VisH.CloseFile();

        // Calculate and print elapsed time.
        SimEndTime = System.currentTimeMillis();
        System.out.println(" done (Elapsed time: " + (SimEndTime - SimStartTime)/1000.0 + " s).");
    }
}