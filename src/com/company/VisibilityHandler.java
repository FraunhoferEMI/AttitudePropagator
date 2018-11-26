/*
Visibility Handler to compute and write accesses.

The Fraunhofer-Gesellschaft zur Foerderung der angewandten Forschung e.V.,
Hansastrasse 27c, 80686 Munich, Germany (further: Fraunhofer) is the holder
of all proprietary rights on this computer program. You can only use this
computer program if you have closed a license agreement with Fraunhofer or
you get the right to use the computer program from someone who is authorized
to grant you that right. Any use of the computer program without a valid
license is prohibited and liable to prosecution.

The use of this software is only allowed under the terms and condition of the
General Public License version 2.0 (GPL 2.0).

Copyright©2018 Gesellschaft zur Foerderung der angewandten Forschung e.V. acting
on behalf of its Fraunhofer Institut für  Kurzzeitdynamik. All rights reserved.

Contact: max.gulde@emi.fraunhofer.de
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
