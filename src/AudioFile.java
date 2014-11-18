import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Map;

/**
 * 
 * @author: Magesh Ramachandran
 * @author: Mayank Narashiman
 * @author: Narendran K.P
 * 
 */
public abstract class AudioFile {
    public enum FILE_TYPE {
        MP3, WAV
    };

    public static final String INVALID_FILE_PATH =
            "ERROR: Incorrect file path, file %s was not found";
    public static final String UNSUPPORTED_FILE_FORMAT =
            "ERROR: The file %s is of a format which is not supported";
    public static final String INVALID_PATH_COMMAND_LINE =
            "ERROR: invalid command line, the given path is not a directory";
    public static final String NO_FILES_IN_DIRECTORY =
            "ERROR: No files in the given directory";
    public static final String UNEXPECTED_ERROR =
            "ERROR: An unexpected error has occured";
    public static final String INSUFFICIENT_DATA =
            "ERROR: insufficient data in file";
    public static final String UNSUPPORTED_SAMPLING_RATE =
            "ERROR: Unsupported sampling rate";
    public static final String BPS_NOT_SUPPORTED =
            "ERROR: Bytes per sample of %d is not supported";
    private String fileName, shortName;
    protected byte[] fileData;

    AudioFile(String fName, boolean isFileReadNeeded) throws IOException {
        this.fileName = fName;
        File f = new File(fileName);
        if (!f.isFile()) {
            throwException(String.format(INVALID_FILE_PATH, fileName));
        }
        shortName = f.getName();
        if (isFileReadNeeded) {
            RandomAccessFile rf = new RandomAccessFile(f, "r");
            fileData = new byte[(int) rf.length()];
            rf.read(fileData);
            rf.close();
        }
    }

    /**
     * To extract header data from the audio file
     * 
     * @return - Map containing the key-value pair of header info, where the
     *         value is an Object representing a String or an Integer
     */
    public abstract Map<String, Object> getHeaderData();

    public abstract int getBps();

    /**
     * To compare if the duration of this file and the given AudioFile af2 are
     * the same
     * 
     * @param af2 - AudioFile for which the comparison has to be made against
     * @return - true if durations are same, false otherwise
     */
    public abstract boolean areFileDurationsTheSame(AudioFile af2);

    /**
     * To extract the audio sample data from the audio file
     * 
     * @return - array containing average amplitude for all samples of left +
     *         right channels for 2 channel audio, or samples of unmodified
     *         amplitude for single channel audio
     */
    public abstract double[] getChannelData();

    /**
     * To validate if the audio file is of the correct format as specified by
     * the subclass
     * 
     * @return - true if the format is valid, false otherwise
     */
    public abstract boolean isAudioFileFormatValid();

    /**
     * @return - Returns the short name of the audio file along with the file
     *         extension
     */
    public String getShortName() {
        return shortName;
    }

    public String getFileName() {
        return fileName;
    }

    public abstract int getDurationInSeconds();

    protected int readIntChunks(byte[] b, int fromidx, int toidx) {
        byte[] chunk = extractChunk(b, fromidx, toidx);
        ByteBuffer wrapped =
                ByteBuffer.wrap(chunk).order(ByteOrder.LITTLE_ENDIAN);
        return wrapped.getInt();
    }

    protected byte[] extractChunk(byte[] b, int fromidx, int toidx) {
        byte[] chunk = new byte[4];
        System.arraycopy(b, fromidx, chunk, 0, toidx + 1 - fromidx);
        return chunk;
    }

    protected String readStringChunks(byte[] b, int fromidx, int toidx) {
        byte[] chunk = extractChunk(b, fromidx, toidx);
        return new String(chunk);
    }

    protected static FILE_TYPE getFileTypeFromName(String fileName) {
        int fnameLength = fileName.length();
        if (fnameLength < 4) {
            throw new RuntimeException(String.format(UNSUPPORTED_FILE_FORMAT,
                    fileName));
        }
        if (fileName.substring(fnameLength - 4, fnameLength).equals(".wav")) {
            return FILE_TYPE.WAV;
        } else if (fileName.substring(fnameLength - 4, fnameLength).equals(
                ".mp3")) {
            return FILE_TYPE.MP3;
        } else {
            throw new RuntimeException(String.format(UNSUPPORTED_FILE_FORMAT,
                    fileName));
        }
    }

    protected static void throwException(String message) {
        throw new RuntimeException(message);
    }

}
