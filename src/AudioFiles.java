import java.io.File;
import java.io.IOException;
import java.util.Arrays;

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

    public static AudioFile[] makeAudioFilesFromArg(
            String flag,
            String fpath,
            int paramNum) throws IOException, InterruptedException {
        AudioFile[] listOfFiles2;
        if ("-f".equals(flag)) {
            listOfFiles2 =
                    new AudioFile[] { AudioFiles.makeAudioFileByExtension(
                            fpath, paramNum, false) };
        } else {
            listOfFiles2 =
                    AudioFiles.makeAllAudioFilesInDirectory(fpath, paramNum);
        }
        return listOfFiles2;
    }

    public static AudioFile makeAudioFileByExtension(
            String fileName,
            int paramNum,
            boolean isDirectory) throws IOException, InterruptedException {
        AudioFile.FILE_TYPE ftype = AudioFile.getFileTypeFromName(fileName);
        AudioFile af = null;
        if (ftype == AudioFile.FILE_TYPE.MP3) {
            af = new Mp3File(fileName, isDirectory, paramNum);
        } else if (ftype == AudioFile.FILE_TYPE.WAV) {
            af = new WavFile(fileName);
        } else {
            throw new RuntimeException(UNSUPPORTED_FILE_FORMAT);
        }
        return af;
    }

    public static AudioFile[] makeAllAudioFilesInDirectory(
            String dirName,
            int paramNum) throws IOException, InterruptedException {
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
        int errcount = 0;
        for (String f : fileNames) {
            try {
                audioFiles[idx++] =
                        AudioFiles.makeAudioFileByExtension(
                                fi.getAbsolutePath() + File.separator + f,
                                paramNum, true);
            } catch (Exception e) {
                idx--;
                errcount++;
                System.err.println(e.getMessage());
                dam.setErrorOccured(true);
            }
        }
        if (errcount != 0) {
            audioFiles =
                    Arrays.copyOfRange(audioFiles, 0, audioFiles.length
                            - errcount);
        }
        return audioFiles;
    }

}
