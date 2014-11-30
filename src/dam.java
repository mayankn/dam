import java.util.List;

/**
 * 
 * This program is used to detect audio misappropriations between the two given
 * set of audio files in .wav, .mp3 or .ogg format. The program needs to be
 * executed with the following command line parameters in any of the following
 * forms
 * 
 * <pre>
 *  -f <pathname> -f <pathname> <mode%optional>
 *  -d <pathname> -d <pathname> <mode%optional>
 *  -f <pathname> -d <pathname> <mode%optional>
 *  -d <pathname> -f <pathname> <mode%optional>
 * </pre>
 * 
 * where <pathname> is a path name and <mode%optional> is an optional argument
 * that takes a value -fast; other values are ignored by the program. If
 * specified, the program executes a code path that provides faster but
 * potentially less accurate results.
 * 
 * If a <pathname> is preceded by "-f", then the <pathname> must end in must
 * name a file that already exists on the file system. If a <pathname> is
 * preceded by "-d", it must name a directory that already exists on the file
 * system and contains nothing but files whose pathnames would be legal
 * following a "-f" option.
 * 
 * If a <pathname> preceded by the "-f" option ends in ".wav", that file must be
 * in little-endian (RIFF) WAVE format with PCM encoding (AudioFormat 1), stereo
 * or mono, 8- or 16-bit samples, with a sampling rate of 11.025, 22.05, 44.1,
 * or 48 kHz.
 * 
 * If the <pathname> ends in ".mp3", that file must be in the MPEG-1 Audio Layer
 * III format (MP3).
 * 
 * If the <pathname> ends in ".ogg", that file must be in a format that version
 * 1.4.0 of the oggdec program will decode into a supported WAVE format without
 * the use of any command-line options.
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

    /**
     * To check if an error has occurred so far
     * @return - true if an error has occurred, false otherwise
     */
    public static boolean isErrorOccured() {
        return errorOccured;
    }

    /**
     * This method is invoked to indicate that an error has occurred during
     * execution. If this method is invoked, the error message should already be
     * printed by the code which invoked the method as the program will exit
     * without printing additional error messages
     */
    public static void setErrorOccured() {
        dam.errorOccured = true;
    }

    public static void main(String args[]) {
        try {
            Long st = System.currentTimeMillis();
            // validates if all the command line arguments are in an acceptable
            // format
            validateCommandLineArguments(args);

            List<ComparableAudioFile> analyzableSamples1, analyzableSamples2;

            // sets the optional execution mode
            if (args.length == 5 && "-fast".equals(args[4])) {
                ComparableAudioFiles
                        .setMode(ComparableAudioFiles.MODES.FAST);
            }

            // creates a list of AnalyzableSample instances for all the file(s)
            // represented by or belonging to a folder given by arg[1]
            analyzableSamples1 =
                    ComparableAudioFiles
                            .makeListOfAnalyzableSamples(AudioFiles
                                    .makeAudioFilesFromArg(args[0], args[1], 1));
            // creates a list of AnalyzableSample instances for all the file(s)
            // represented by or belonging to a folder given by arg[3]
            analyzableSamples2 =
                    ComparableAudioFiles
                            .makeListOfAnalyzableSamples(AudioFiles
                                    .makeAudioFilesFromArg(args[2], args[3], 2));

            // compares each ComparableAudioFile corresponding to arg[1] to every
            // ComparableAudioFile corresponding to arg[3] for check for a
            // matching sequence of audio. If there is a match, prints "MATCH"
            // along with the time in seconds at which the match has occurred
            for (ComparableAudioFile aS1 : analyzableSamples1) {
                for (ComparableAudioFile aS2 : analyzableSamples2) {
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
            String errMessage = e.getMessage();
            // if the error message is in an unusual format, changes it to an
            // appropriate message as required by the spec
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
