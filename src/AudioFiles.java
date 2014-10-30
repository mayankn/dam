import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * @author: Magesh Ramachandran
 * @author: Mayank Narashiman
 * @author: Narendran K.P
 */


public abstract class AudioFiles {
    private static String UNSUPPORTED_FILE_FORMAT =
            "ERROR: Unsupported file format";
    private static String INVALID_PATH_COMMAND_LINE =
            "ERROR: invalid command line, the given path is not a directory";
    private static String NO_FILES_IN_DIRECTORY =
            "ERROR: No files in the given directory";
    private static String CONVERT_TO_WAV_COMMAND =
            "/course/cs5500f14/bin/lame --decode %s %s";
    private static String INVALID_FILE_PATH =
            "ERROR: Incorrect file path, file %s was not found";
    private static String CONVERTED_FILES_DIRECTORY1 = File.separator + "tmp"
            + File.separator + "dam-mmn" + File.separator + "1";
    private static String CONVERTED_FILES_DIRECTORY2 = File.separator + "tmp"
            + File.separator + "dam-mmn" + File.separator + "2";

    // TODO: This is WORK IN PROGRESS
    public static AudioFile[] makeAudioFilesFromArg(String flag,
                                                    String fpath,
                                                    int paramNum)
            throws IOException, InterruptedException {
        AudioFile[] listOfFiles2;
        if ("-f".equals(flag)) {
            listOfFiles2 = new AudioFile[]{AudioFiles
                    .makeAudioFileByExtension(fpath)};
        } else {
            listOfFiles2 = AudioFiles.makeAllAudioFilesInDirectory(fpath);
        }
        return listOfFiles2;
    }

    public static AudioFile makeAudioFileByExtension(String fileName)
            throws IOException, InterruptedException {
        AudioFile.FILE_TYPE ftype = AudioFile.getFileTypeFromName(fileName);
        AudioFile af = null;
        if (ftype == AudioFile.FILE_TYPE.MP3) {
            String convertedFileName = convertAudioFileToWav(fileName);
            if (convertedFileName != null) {
                af = new WavFile(convertedFileName);
            }
        } else if (ftype == AudioFile.FILE_TYPE.WAV) {
            af = new WavFile(fileName);
        } else {
            throw new RuntimeException(UNSUPPORTED_FILE_FORMAT);
        }
        return af;
    }

    public static AudioFile[] makeAllAudioFilesInDirectory(String dirName)
            throws IOException, InterruptedException {
        File fi = new File(dirName);
        String[] fileNames;
        if (fi.isDirectory()) {
            fileNames = fi.list();
        } else {
            throw new RuntimeException(INVALID_PATH_COMMAND_LINE);
        }
        if (fileNames == null || fileNames.length == 0) {
            throw new RuntimeException(NO_FILES_IN_DIRECTORY);
        }
        AudioFile[] audioFiles = new AudioFile[fileNames.length];
        int idx = 0;
        for (String f : fileNames) {
            audioFiles[idx++] = AudioFiles.makeAudioFileByExtension(
                    fi.getAbsolutePath() + File.separator + f);
        }
        return audioFiles;
    }

    private static String convertAudioFileToWav(String fileName)
            throws IOException, InterruptedException {
        String convertedFileName = null;
        File f = new File(fileName);
        if (!f.isFile()) {
            throw new RuntimeException(String.format(INVALID_FILE_PATH,
                    fileName));
        }
        String shortName = f.getName();
        String nameWithWavExtension = shortName.replace(".", "") + ".wav";
        String cmd = String.format(CONVERT_TO_WAV_COMMAND, fileName,
                CONVERTED_FILES_DIRECTORY1 + File.separator
                        + nameWithWavExtension);
        Process proc = Runtime.getRuntime().exec(cmd);
        StreamConsumer outputStreamConsumer =
                new StreamConsumer(proc.getInputStream());
        StreamConsumer errorStreamConsumer =
                new StreamConsumer(proc.getErrorStream());

        // kick off both stream consumers
        Thread t1 = new Thread(outputStreamConsumer, "output");
        Thread t2 = new Thread(errorStreamConsumer, "error");
        t1.start();
        t2.start();

        int exitValue = proc.waitFor();
        //System.out.println("exit value: " + exitValue);
        if (exitValue == 0)
            convertedFileName = CONVERTED_FILES_DIRECTORY1 + File.separator
                    + nameWithWavExtension;
        return convertedFileName;
    }
}
