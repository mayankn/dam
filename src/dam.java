import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author: Magesh Ramachandran
 * @author: Mayank Narashiman
 * @author: Narendran K.P
 * 
 *          </br>This program is used to detect audio misappropriations between
 *          the two given audio files in .wav format. The program needs to be
 *          executed with the following command line parameters -f <pathname> -f
 *          <pathname> where <pathname> is a path name that ends in ".wav" for a
 *          file that already exists in the file system and is in WAVE format
 *          with CD-quality parameters
 */
public class dam {

    private static String INVALID_COMMAND_ERROR = "ERROR: Invalid command line";
    private static String MATCH = "MATCH %s %s";

    public static boolean errorOccured;

    public static boolean isErrorOccured() {
        return errorOccured;
    }

    public static void setErrorOccured(boolean errorOccured) {
        dam.errorOccured = errorOccured;
    }

    public static void main(String args[]) {
        try {
            // Long st = System.currentTimeMillis();
            validateCommandLineArguments(args);
            AudioFile[] listOfFiles1 =
                    AudioFiles.makeAudioFilesFromArg(args[0], args[1], 1);
            AudioFile[] listOfFiles2 =
                    AudioFiles.makeAudioFilesFromArg(args[2], args[3], 2);
            Map<Integer, List<AnalyzableSamples>> mapOfAnalyzableSamples1ByDuration =
                    new HashMap<Integer, List<AnalyzableSamples>>();
            Map<Integer, List<AnalyzableSamples>> mapOfAnalyzableSamples2ByDuration =
                    new HashMap<Integer, List<AnalyzableSamples>>();
            prepareMapOfAnalyzableSamplesByDuration(listOfFiles1,
                    mapOfAnalyzableSamples1ByDuration);
            prepareMapOfAnalyzableSamplesByDuration(listOfFiles2,
                    mapOfAnalyzableSamples2ByDuration);

            for (int duration : mapOfAnalyzableSamples1ByDuration.keySet()) {
                List<AnalyzableSamples> asl1 =
                        mapOfAnalyzableSamples1ByDuration.get(duration);
                List<AnalyzableSamples> asl2 =
                        mapOfAnalyzableSamples2ByDuration.get(duration);
                if (asl1 == null || asl2 == null)
                    continue;
                for (AnalyzableSamples as1 : asl1) {
                    for (AnalyzableSamples as2 : asl2) {
                        if (as1.isMatch(as2)) {
                            System.out.println(String.format(MATCH,
                                    as1.getFileName(), as2.getFileName()));
                        }
                    }
                }
            }
            if (isErrorOccured()) {
                System.exit(1);
            }
            // Long et = System.currentTimeMillis();
            // System.out.println("time: " + (et - st));
        } catch (Exception e) {
            String errMessage = e.getMessage();
            if (errMessage == null || errMessage.length() < 5
                    || !errMessage.substring(0, 5).equals("ERROR")) {
                errMessage = "ERROR: An unexpected error has occured";
            }
            System.err.println(errMessage);
            System.exit(1);
        }
    }

    private static
            void
            prepareMapOfAnalyzableSamplesByDuration(
                    AudioFile[] listOfFiles1,
                    Map<Integer, List<AnalyzableSamples>> mapOfAnalyzableSamples1ByDuration) {
        int duration;
        for (AudioFile af : listOfFiles1) {
            duration = af.getDurationInSeconds();
            List<AnalyzableSamples> asl =
                    mapOfAnalyzableSamples1ByDuration.get(duration);
            AnalyzableSamples as =
                    AnalyzableSamplesFactory.make(af.extractChannelData());
            as.setBitRate((Integer) af.getHeaderData().get(
                    "FMT_SIGNIFICANT_BPS"));
            as.setFileName(af.getShortName());
            if (asl != null) {
                asl.add(as);
            } else {
                asl = new ArrayList<AnalyzableSamples>();
                asl.add(as);
                mapOfAnalyzableSamples1ByDuration.put(duration, asl);
            }
        }
    }

    private static void validateCommandLineArguments(String[] args) {
        if (args.length < 4) {
            throw new RuntimeException(INVALID_COMMAND_ERROR);
        }
        if (!("-f".equals(args[0]) || "-d".equals(args[0]))
                || !("-f".equals(args[2]) || "-d".equals(args[2]))) {
            throw new RuntimeException(INVALID_COMMAND_ERROR);
        }
    }

}
