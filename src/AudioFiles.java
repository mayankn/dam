import java.io.File;
import java.io.IOException;
import java.util.Arrays;

/**
 * @author: Magesh Ramachandran
 * @author: Mayank Narashiman
 * @author: Narendran K.P
 * Description: This is an abstract class to handle and instantiate all the
 * supported audio file formats appropriately
 */

public abstract class AudioFiles {


    /**
     *
     * @param flag
     * @param fpath
     * @param paramNum
     * @return - list of AudioFile instances
     * @throws IOException
     * @throws InterruptedException
     */
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

    /**
     *
     * @param fileName
     * @param paramNum
     * @param isDirectory
     * @return - an AudioFile instance created from the input file path
     * @throws IOException
     * @throws InterruptedException
     */
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
            throw new RuntimeException(AudioFile.UNSUPPORTED_FILE_FORMAT);
        }
        return af;
    }

    /**
     *
     * @param dirName
     * @param paramNum
     * @return - a list of AudioFile instances created from the input directory
     * @throws IOException
     * @throws InterruptedException
     */
    public static AudioFile[] makeAllAudioFilesInDirectory(
            String dirName,
            int paramNum) throws IOException, InterruptedException {
        File fi = new File(dirName);
        String[] fileNames;
        if (fi.isDirectory()) {
            fileNames = fi.list();
        } else {
            throw new RuntimeException(AudioFile.INVALID_PATH_COMMAND_LINE);
        }
        if (fileNames == null || fileNames.length == 0) {
            throw new RuntimeException(AudioFile.NO_FILES_IN_DIRECTORY);
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
