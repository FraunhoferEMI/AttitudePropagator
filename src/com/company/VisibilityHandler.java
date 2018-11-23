/*
Visibility Handler to compute and write accesses.
Author: Max Gulde, max.gulde@emi.fraunhofer.de
Licensed under GNU GPL-2.0-only.
 */

package com.company;

import java.io.*;
import org.orekit.time.*;
import org.orekit.propagation.*;
import org.orekit.propagation.events.*;
import org.orekit.propagation.events.handlers.*;

// EventHandler
public class VisibilityHandler implements EventHandler<ElevationDetector> {

    private PrintWriter AccessWriter;
    private AbsoluteDate AccessBegin;
    private int AccessNum = 1;

    VisibilityHandler(Settings set)
    {
        // Write header to access file
        try {
            String FilePath = set.GetValue("ResultsDirectory") + "/" + set.GetValue("ExpFileAccessTimes");
            AccessWriter = new PrintWriter(FilePath, set.GetValue("ExpTextFormat"));
            AccessWriter.println("\"Access\",\"Start Time (UTCG)\",\"Stop Time (UTCG)\",\"Duration (sec)\"");
        }
        catch (Exception e)
        {
            System.out.println("Error: Could not setup access file:" + e.toString());
        }
    }

    public Action eventOccurred(final SpacecraftState s, final ElevationDetector detector, final boolean increasing) {
        // Satellite entering access zone
        if (increasing) {
            AccessBegin = s.getDate();
            return Action.CONTINUE;
        // Satellite is exiting access zone, write access
        } else {
            AbsoluteDate AccessEnd = s.getDate();
            // Extract date, time and duration of the access.
            try {
                double visDuration = AccessEnd.durationFrom(AccessBegin);
                DateTimeComponents compsBegin = AccessBegin.getComponents(0);
                DateTimeComponents compsEnd = AccessEnd.getComponents(0);
                DateComponents dateBegin = compsBegin.getDate();
                DateComponents dateEnd = compsEnd.getDate();
                TimeComponents timeBegin = compsBegin.getTime();
                TimeComponents timeEnd = compsEnd.getTime();

                // Write access into file.
                AccessWriter.printf(Settings.LocalFormat, "%d,%d %s %d %02d:%02d:%06.3f,%d %s %d %02d:%02d:%06.3f,%07.3f\n",
                        AccessNum++,
                        dateBegin.getDay(), dateBegin.getMonthEnum().getCapitalizedAbbreviation(),
                        dateBegin.getYear(), timeBegin.getHour(), timeBegin.getMinute(), timeBegin.getSecond(),
                        dateEnd.getDay(), dateEnd.getMonthEnum().getCapitalizedAbbreviation(), dateEnd.getYear(),
                        timeEnd.getHour(), timeEnd.getMinute(), timeEnd.getSecond(), visDuration);
            } catch (Exception e) {
                System.out.println("Error: Writing to access file failed:" + e.toString());
            }

            return Action.STOP;
        }
    }

    void CloseFile()
    {
        AccessWriter.close();
    }
}
