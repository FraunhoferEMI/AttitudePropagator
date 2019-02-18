/*
Determine satellite attitude for a given orbit with respect to Earth and Sun. Lists accesses to ground station.

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

import org.orekit.attitudes.LofOffset;
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
        double SemiMajorAxis = Double.parseDouble(Set.GetValue("SatAltitude")) + Settings.EarthRadius;
        double SatEccentricity = Double.parseDouble(Set.GetValue("SatEccentricity"));
        double SatInclination = Double.parseDouble(Set.GetValue("SatInclination"));
        double SatOmega = Double.parseDouble(Set.GetValue("SatOmega"));
        double SatRAAN = Double.parseDouble(Set.GetValue("SatRAAN"));
        double SatMeanAnomaly = Double.parseDouble(Set.GetValue("SatMeanAnomaly"));
        InitialOrbit = new CircularOrbit(SemiMajorAxis, SatEccentricity * Math.cos(Math.toRadians(SatOmega)),
                SatEccentricity * Math.sin(Math.toRadians(SatOmega)), Math.toRadians(SatInclination),
                Math.toRadians(SatRAAN), Math.toRadians(SatMeanAnomaly), PositionAngle.MEAN,
                InertialFrame, TimeStart, Constants.EIGEN5C_EARTH_MU);
        if (Settings.fDisplayMessages) {
            System.out.println("Created satellite orbit:");
            System.out.println("\tSemiMajorAxis = " + SemiMajorAxis);
            System.out.println("\tSatEccentricity = " + SatEccentricity);
            System.out.println("\tSatInclination = " + SatInclination);
            System.out.println("\tSatOmega = " + SatOmega);
            System.out.println("\tSatRAAN = " + SatRAAN);
            System.out.println("\tSatMeanAnomaly = " + SatMeanAnomaly);
        }

        // Set up propagator
        EcksteinHechlerPropagator Propagator = new EcksteinHechlerPropagator(InitialOrbit,
                new LofOffset(InitialOrbit.getFrame(), LOFType.TNW),
                Constants.EIGEN5C_EARTH_EQUATORIAL_RADIUS,
                Constants.EIGEN5C_EARTH_MU,  Constants.EIGEN5C_EARTH_C20,
                Constants.EIGEN5C_EARTH_C30, Constants.EIGEN5C_EARTH_C40,
                Constants.EIGEN5C_EARTH_C50, Constants.EIGEN5C_EARTH_C60);
        Propagator.setSlaveMode();
        if (Settings.fDisplayMessages) {
            System.out.println("Created Eckstein-Hechler propagator.");
        }

        // Setting up files and print files header
        String FilePath = Set.GetValue("ResultsDirectory") + "/" + Set.GetValue("ExpFileSunAngles");
        SunAngles = new PrintWriter(FilePath,Set.GetValue("ExpTextFormat"));
        FilePath = Set.GetValue("ResultsDirectory") + "/" + Set.GetValue("ExpFileEarthAngles");
        EarthAngles = new PrintWriter(FilePath,Set.GetValue("ExpTextFormat"));
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
        Propagator.addEventDetector(StationVisibility);
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
            SpacecraftState CurrentSCState = Propagator.propagate(t);

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
            Vector3D Earth2Sun = Sun.getPVCoordinates(AbsDate, InertialFrame).getPosition();    // Sun position as seen from Earth
            Vector3D Sat2Earth = Earth.getPVCoordinates(AbsDate, SatelliteFrame).getPosition(); // Earth position as seen from satellite
            Vector3D Sat2Sun = Sun.getPVCoordinates(AbsDate, SatelliteFrame).getPosition();     // Sun position as seen from satellite
            Vector3D Earth2Sat = CurrentSCState.getPVCoordinates().getPosition();
            // Angle projected on xy plane in satellite geometry
            Vector3D Sat2SunProjXY = new Vector3D(Sat2Sun.getX(), Sat2Sun.getY(), 0.0);

            // Compute solar angles (deg)
            double Subsolar, SunAzimuth, SunElevation;
            Subsolar = Math.toDegrees(Vector3D.angle(Earth2Sun, Earth2Sat));
            SunAzimuth = Math.toDegrees(Vector3D.angle(Vector3D.PLUS_I, Sat2SunProjXY));
            SunElevation = Math.toDegrees(Vector3D.angle(Sat2Sun, Sat2SunProjXY));
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