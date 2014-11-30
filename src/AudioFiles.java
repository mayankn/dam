import java.io.File;
import java.io.IOException;
import java.util.Arrays;

/**
 * This is an abstract class that contains static factory methods to instantiate
 * an instance or a collection of {@AudioFile} based on the input
 * parameters
 * 
 * @author: Magesh Ramachandran
 * @author: Mayank Narashiman
 * @author: Narendran K.P
 * 
 */

public abstract class AudioFiles {

    /**
     * Makes an array of {@AudioFile} based on the given parameters
     * 
     * @param flag - indicates if the given fpath is a directory or a file, '-f'
     *            -> file, '-d' -> directory
     * @param fpath - file path including the file name, or a file directory
     * @param paramNum - the sub-folder of the temporary path to which the
     *            temporary file(s) if any must be written to
     * @return - an array of {@AudioFile}
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
                            fpath, paramNum) };
        } else {
            listOfFiles2 =
                    AudioFiles.makeAllAudioFilesInDirectory(fpath, paramNum);
        }
        return listOfFiles2;
    }

    /**
     * Makes a single instance of {@AudioFile} based on the file
     * extension from the given file name. If the file extension is of a format
     * that is not supported, throws a RuntimeException
     * 
     * @param fileName - file name including the extension
     * @param paramNum - the sub-folder folder to which the temporarily created
     *            files will be stored
     * @return - an {@AudioFile} instance created from the input
     *         file path
     * @throws IOException
     * @throws InterruptedException
     */
    private static AudioFile makeAudioFileByExtension(
            String fileName,
            int paramNum) throws IOException, InterruptedException {
        AudioFile.FILE_TYPE ftype = AudioFile.getFileTypeFromName(fileName);
        AudioFile af = null;
        if (ftype == AudioFile.FILE_TYPE.MP3) {
            af = new Mp3File(fileName, paramNum);
        } else if (ftype == AudioFile.FILE_TYPE.WAV) {
            af = new WavFile(fileName);
        } else if (ftype == AudioFile.FILE_TYPE.OGG) {
            af = new OggFile(fileName, paramNum);
        } else {
            throw new RuntimeException(AudioFile.UNSUPPORTED_FILE_FORMAT);
        }
        return af;
    }

    /**
     * Makes an array of {@AudioFile} for all the files of supported
     * file type that are present in the given file directory. If there are any
     * files of unsupported format or if there are other sub-directories, the
     * method prints an error message and sets the exit status of the main
     * program to -1
     * 
     * @param dirName - directory name
     * @param paramNum - sub-folder under which the temporary files created by
     *            the program must be stored
     * @return - an array of {@AudioFile} created based on the input
     * @throws IOException
     * @throws InterruptedException
     */
    private static AudioFile[] makeAllAudioFilesInDirectory(
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
        // If any of the files in the directory is of an unsupported format,
        // prints an error message and registers an error with the dam program
        // without exiting immediately
        for (String f : fileNames) {
            try {
                audioFiles[idx++] =
                        AudioFiles.makeAudioFileByExtension(
                                fi.getAbsolutePath() + File.separator + f,
                                paramNum);
            } catch (Exception e) {
                idx--;
                errcount++;
                System.err.println(e.getMessage());
                dam.setErrorOccured();
            }
        }
        // If error(s) had occurred while parsing files from the given
        // directory, the size of the array is reduced as such file(s) are
        // not considered for further processing
        if (errcount != 0) {
            audioFiles =
                    Arrays.copyOfRange(audioFiles, 0, audioFiles.length
                            - errcount);
        }
        return audioFiles;
    }

}
