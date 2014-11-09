import java.util.ArrayList;
import java.util.List;

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
    private static String MATCH = "MATCH %s %s %d %d";
    private static int FRAGMENT_SIZE_TO_MATCH = 5;

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
            List<AnalyzableSamples> analyzableSamples1 =prepareListOfAnalyzableSamples(listOfFiles1);
            List<AnalyzableSamples> analyzableSamples2 =prepareListOfAnalyzableSamples(listOfFiles2);
            
            for(AnalyzableSamples aS1 : analyzableSamples1) {
                for(AnalyzableSamples aS2 : analyzableSamples2) {
                    int[] matchPosition = aS1.getMatchPositionInSeconds(aS2);
                    if (matchPosition != null) {
                        System.out.println(String.format(MATCH,
                                aS1.getFileName(), aS2.getFileName(), matchPosition[0], matchPosition[1]));
                    }               
                }
            }
            if (isErrorOccured()) {
                System.exit(1);
            }
            // Long et = System.currentTimeMillis();
            // System.out.println("time: " + (et - st));
        } catch (Exception e) {
            e.printStackTrace();
            String errMessage = e.getMessage();
            if (errMessage == null || errMessage.length() < 5
                    || !errMessage.substring(0, 5).equals("ERROR")) {
                errMessage = "ERROR: An unexpected error has occured";
            }
            System.err.println(errMessage);
            System.exit(1);
        }
    }    
    
    private static List<AnalyzableSamples> prepareListOfAnalyzableSamples(AudioFile[] listOfFiles1) {
        int duration;
        List<AnalyzableSamples> asl = new ArrayList<AnalyzableSamples>();
        for (AudioFile af : listOfFiles1) {
            duration = af.getDurationInSeconds();
            if(duration < FRAGMENT_SIZE_TO_MATCH) {
                continue;
            }
            AnalyzableSamples as =
                    AnalyzableSamplesFactory.make(af.extractChannelData());
            as.setBitRate((Integer) af.getHeaderData().get(
                    "FMT_SIGNIFICANT_BPS"));
            as.setFileName(af.getShortName());
            asl.add(as);
        }
        return asl;
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
