/**
 * 
 * @author: Magesh Ramachandran
 * @author: Mayank Narashiman
 * @author: Narendran K.P
 * 
 */
public abstract class AudioFile {
    public enum FILE_TYPE {
        MP3, WAV, OGG
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

    /**
     * To get the bits per second of the audio file
     * @return - bits per second of the audio file
     */
    public abstract int getBps();

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
     * To get the duration of the audio files in seconds
     * @return - duration of audio file in seconds
     */
    public abstract int getDurationInSeconds();

    /**
     * To get the next canonicalized segment of length specified by the
     * parameter streamingLength from the audio file
     * 
     * @param streamingLength - the length of canonicalized segments to be read
     *            from the audio file specified by count (represents the count
     *            of 16 bit canonicalized values to be read from the file)
     * 
     * @return double array of length given by the parameter streamingLength in
     *         which each value represents a sample from the canonicalized
     *         format of audio samples encapsulated by this instance. The method
     *         may return an array of size < streamingLength if there are not
     *         enough unread samples in the encapsulated audio
     */
    public abstract double[] getNext(int streamingLength);

    /**
     * To get the name of the encapsulated file without its path
     * @return name of the encapsulated file
     */
    public abstract String getShortName();

    /**
     * To check if the encapsulated audio file has any unread data
     * @return true if there is unread data, false otherwise
     */
    public abstract boolean hasNext();

    /**
     * To close the audio file encapsulated by this instance
     */
    public abstract void close();

    /**
     * To verify if the given input is of a format supported by the program
     * based on the file extension. If the file is of a valid format, returns an
     * ENUM representing the corresponding file type. Otherwise, throws a
     * RuntimeException
     * @param fileName - a file name with extension as a String
     * @return {@FILE_TYPE}
     */
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
        } else if (fileName.substring(fnameLength - 4, fnameLength).equals(
                ".ogg")) {
            return FILE_TYPE.OGG;
        } else {
            throw new RuntimeException(String.format(UNSUPPORTED_FILE_FORMAT,
                    fileName));
        }
    }

    /**
     * Convenience method to throw a RuntimeException encapsulating the given
     * message
     * @param message
     */
    protected static void throwException(String message) {
        throw new RuntimeException(message);
    }

}
