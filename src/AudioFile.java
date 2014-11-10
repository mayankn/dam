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

    public static String INVALID_FILE_PATH =
            "ERROR: Incorrect file path, file %s was not found";

    /**
     * To extract header data from the audio file
     * 
     * @return - Map containing the key-value pair of header info, where the
     *         value is an Object representing a String or an Integer
     */
    public abstract Map<String, Object> getHeaderData();

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
    public abstract double[] extractChannelData();

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
    public abstract String getShortName();

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
            throw new RuntimeException("ERROR: Invalid file format");
        }
        if (fileName.substring(fnameLength - 4, fnameLength).equals(".wav")) {
            return FILE_TYPE.WAV;
        } else if (fileName.substring(fnameLength - 4, fnameLength).equals(
                ".mp3")) {
            return FILE_TYPE.MP3;
        } else {
            throw new RuntimeException("ERROR: Invalid file format " +
                    fileName);
        }
    }

}
