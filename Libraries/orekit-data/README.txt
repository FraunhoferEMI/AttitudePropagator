For convenience, a zip archive containing some configuration data is available for download. Similar files can be custom made by users with updated data. Configuring data loading is explained in the configuration page. For a start, the simplest configuration is to download the orekit-data-master.zip file from the forge, to unzip it anywhere you want, rename the orekit-data-master folder that will be created into orekit-data and add the following lines at the start of your program:

File orekitData = new File("/path/to/the/folder/orekit-data");
DataProvidersManager manager = DataProvidersManager.getInstance();
manager.addProvider(new DirectoryCrawler(orekitData));
This file contains the following data sets. Note that the data is updated only from time to time, so users must check by themselves they cover the time range needed for their computation.

leap seconds data,

IERS Earth orientation parameters from 1973 (both IAU-1980 and IAU-2000),

Marshall Solar Activity Future Estimation from 1999,

JPL DE 430 planetary ephemerides from 1990 to 2069,

Eigen 06S gravity field,

FES 2004 ocean tides model.

There are no guarantees that this file will be available indefinitely or that its content will be updated. It should be considered as a simple configuration example. Users are encouraged to set up their own configuration data.

The file is available by following the download link (https://gitlab.orekit.org/orekit/orekit-data/-/archive/master/orekit-data-master.zip) in the project dedicated to Orekit Data in the forge.