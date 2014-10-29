import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author: Magesh Ramachandran
 * @author: Mayank Narashiman
 * @author: Narendran K.P This program is used to detect audio misappropriations
 * between the two given audio files in .wav format. The program needs
 * to be executed with the following command line parameters -f
 * <pathname> -f <pathname> where <pathname> is a path name that ends
 * in ".wav" for a file that already exists in the file system and is
 * in WAVE format with CD-quality parameters
 */
public class dam {

    private static String INVALID_COMMAND_ERROR = "ERROR: Invalid command line";
    private static String INVALID_FILE_FORMAT =
            "ERROR: The given file %s is of invalid format";
    private static String NO_MATCH = "NO MATCH";
    private static String MATCH = "MATCH %s %s";

    public static void main(String args[]) {
        try {
            Long st = System.currentTimeMillis();
            validateCommandLineArguments(args);
            AudioFile[] listOfFiles1;
            AudioFile[] listOfFiles2;
            Map<Integer, List<AnalyzableSamples>> mapOfAnalyzableSamples1ByDuration =
                    new HashMap<Integer, List<AnalyzableSamples>>();
            Map<Integer, List<AnalyzableSamples>> mapOfAnalyzableSamples2ByDuration =
                    new HashMap<Integer, List<AnalyzableSamples>>();
            if ("-f".equals(args[0])) {
                listOfFiles1 =
                        new AudioFile[]{AudioFiles
                                .makeAudioFileByExtension(args[1])};
            } else {
                listOfFiles1 = AudioFiles.makeAllAudioFilesInDirectory(args[1]);
            }

            if ("-f".equals(args[2])) {
                listOfFiles2 =
                        new AudioFile[]{AudioFiles
                                .makeAudioFileByExtension(args[3])};
            } else {
                listOfFiles2 = AudioFiles.makeAllAudioFilesInDirectory(args[3]);
            }

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

            Long et = System.currentTimeMillis();
            System.out.println("time: " + (et - st));
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println(e.getMessage());
            System.exit(0);
        } finally {

        }
    }

    private static void prepareMapOfAnalyzableSamplesByDuration(
            AudioFile[] listOfFiles1,
            Map<Integer, List<AnalyzableSamples>>
                    mapOfAnalyzableSamples1ByDuration) {
        int duration;
        for (AudioFile af : listOfFiles1) {
            duration = af.getDurationInSeconds();
            List<AnalyzableSamples> asl =
                    mapOfAnalyzableSamples1ByDuration.get(duration);
            AnalyzableSamples as =
                    AnalyzableSamplesFactory.make(af.extractChannelData());
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

    private static void handleNoMatch() {
        System.out.println(NO_MATCH);
        // System.exit(0);
    }

    private static void validateCommandLineArguments(String[] args) {
        if (args.length < 4) {
            throw new RuntimeException(INVALID_COMMAND_ERROR);
        }
        if (!("-f".equals(args[0]) || "-d".equals(args[0]))
                || !("-f".equals(args[2]) || "-d".equals(args[2]))) {
            throw new RuntimeException(INVALID_COMMAND_ERROR);
        }
        // if (!WavFile.isFileExtensionValid(args[1])) {
        // throw new RuntimeException(INVALID_COMMAND_ERROR);
        // }
        // if (!WavFile.isFileExtensionValid(args[3])) {
        // throw new RuntimeException(INVALID_COMMAND_ERROR);
        // }
    }

}
