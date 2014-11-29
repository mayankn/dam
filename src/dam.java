import java.util.List;

/**
 * 
 * This program is used to detect audio misappropriations between the two given
 * audio files in  format. The program needs to be executed with the
 * following command line parameters -f <pathname> -f <pathname> where
 * <pathname> is a path name that ends in ".wav" for a file that already exists
 * in the file system and is in WAVE format with CD-quality parameters
 * 
 * @author: Magesh Ramachandran
 * @author: Mayank Narashiman
 * @author: Narendran K.P
 * 
 */
public class dam {

    private static String INVALID_COMMAND_ERROR = "ERROR: Invalid command line";
    private static String MATCH = "MATCH %s %s %.1f %.1f";
    private static String UNEXPECTED_ERROR =
            "ERROR: An unexpected error has occured";

    public static boolean errorOccured;

    public static boolean isErrorOccured() {
        return errorOccured;
    }

    public static void setErrorOccured(boolean errorOccured) {
        dam.errorOccured = errorOccured;
    }

    public static void main(String args[]) {
        try {
            Long st = System.currentTimeMillis();
            validateCommandLineArguments(args);
            List<AnalyzableSamples> analyzableSamples1, analyzableSamples2;
            AnalyzableSamplesFactory.setMode(AnalyzableSamplesFactory.MODES.FAST);
            analyzableSamples1 =
                    AnalyzableSamplesFactory
                            .makeListOfAnalyzableSamples(AudioFiles
                                    .makeAudioFilesFromArg(args[0], args[1], 1));
            analyzableSamples2 =
                    AnalyzableSamplesFactory
                            .makeListOfAnalyzableSamples(AudioFiles
                                    .makeAudioFilesFromArg(args[2], args[3], 2));

            for (AnalyzableSamples aS1 : analyzableSamples1) {
                for (AnalyzableSamples aS2 : analyzableSamples2) {
                    double[] matchPosition = aS1.getMatchPositionInSeconds(aS2);
                    if (matchPosition != null) {
                        System.out.println(String.format(MATCH,
                                aS1.getFileName(), aS2.getFileName(),
                                matchPosition[0], matchPosition[1]));
                    }
                }
            }
            if (isErrorOccured()) {
                System.exit(1);
            }
            Long et = System.currentTimeMillis();
            System.out.println("time: " + (et - st));
        } catch (Exception e) {
            // TODO: remove before submission
            e.printStackTrace();
            String errMessage = e.getMessage();
            if (errMessage == null || errMessage.length() < 5
                    || !errMessage.substring(0, 5).equals("ERROR")) {
                errMessage = UNEXPECTED_ERROR;
            }
            System.err.println(errMessage);
            System.exit(1);
        }
    }

    /**
     * Validates the command line arguments passed to the program
     * 
     * @param args - an array of command line arguments
     */
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
