import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;

/**
 * 
 * This class converts the given audio sample with related byte data into
 * suitable canonical form that is used for analysis. The canonical form is a
 * CD-quality 16-bit PCM audio wav format with 44.1 Khz sampling rate,
 * represented as a double[] array.
 * 
 * @author: Magesh Ramachandran
 * @author: Mayank Narashiman
 * @author: Narendran K.P
 * 
 */
public class WavFile extends AudioFile {
    private static final double CANONICAL_SAMPLING_RATE = 44100;
    private static final String CHUNK_RIFF = "RIFF";
    private static final String CHUNK_FMT = "fmt";
    private static final String CHUNK_DATA = "data";

    private String riffType;
    private int noOfDataBytes;
    private int averageBps;
    private int samplingRate;
    private int noOfChannels;
    private int significantBitsPerSecond;
    private int dataChunkIdx;
    private int totalDataLength;
    private int dataLengthRead;
    private int bytesPerChannel;
    private int bpsAggregate;
    private String fileName, shortName;
    private RandomAccessFile rf;
    private FileChannel ch;
    byte[] fileData;
    ByteBuffer byteBufferForStreaming;
    boolean isFirstOrLastAccess = true;

    // constructor
    public WavFile(String fName) throws IOException {
        this.fileName = fName;
        File f = new File(fileName);
        if (!f.isFile()) {
            throwException(String.format(INVALID_FILE_PATH, fileName));
        }
        shortName = f.getName();
        rf = new RandomAccessFile(f, "r");
        int fileLength = (int) f.length();
        if (fileLength < 44) {
            throwException(INSUFFICIENT_DATA);
        }

        byte[] headerData = new byte[80];
        rf.read(headerData);
        readHeaderChunks(headerData);
        if (!isAudioFileFormatValid()) {
            rf.close();
            throwException(String.format(UNSUPPORTED_FILE_FORMAT, fileName));
        }
        ch = rf.getChannel();
        rf.seek(dataChunkIdx);
    }

    /**
     * To check if there is more data to be streamed
     * @return - true if there is more data to be streamed, false otherwise
     */
    public boolean hasNext() {
        return (dataLengthRead < totalDataLength);
    }

    /**
     * Reads the no of bytes corresponding to the requested number of samples
     * and returns the requested number of samples in the canonicalized form
     * @return - requested number of samples in the canonicalized form
     */
    public double[] getNext(int streamingLength) {
        int extractLen = streamingLength;
        int bytesToBeStreamed = streamingLength * bpsAggregate;
        int dataLeft = (totalDataLength - dataLengthRead) * bpsAggregate;
        if (bytesToBeStreamed > dataLeft) {
            bytesToBeStreamed = dataLeft;
            extractLen = bytesToBeStreamed / bpsAggregate;
            isFirstOrLastAccess = true;
        }
        if (isFirstOrLastAccess) {
            fileData = new byte[bytesToBeStreamed];
            byteBufferForStreaming = ByteBuffer.wrap(fileData);
            isFirstOrLastAccess = false;
        }
        byteBufferForStreaming.clear();
        try {
            ch.read(byteBufferForStreaming);
        } catch (Exception e) {
            // do nothing
        }
        dataLengthRead = dataLengthRead + streamingLength;
        return extractChannelData(fileData, extractLen);
    }

    /**
     * closes the file channel and the RandomAccessFile objects used for
     * reading the audio file
     */
    public void close() {
        try {
            ch.close();
            rf.close();
        } catch (IOException e) {
            throw new RuntimeException("ERROR: Error while closing the file"
                    + shortName);
        }
    }

    /**
     * Extracts the different header chunks dynamically from the given
     * headerData until the data chunk is found
     * @param headerData - byte data containing the header information
     */
    private void readHeaderChunks(byte[] headerData) {
        int chunkDataSize;
        String chunkId;
        for (int idx = 0; idx < 80;) {
            chunkId = readStringChunks(headerData, idx, idx + 3);
            if (chunkId != null) {
                chunkId = chunkId.trim();
            }
            chunkDataSize = extractChunkData(headerData, chunkId, idx);
            if (noOfDataBytes > 0) {
                break;
            }
            idx = idx + chunkDataSize;
        }
    }

    /**
     * Extracts relevant information from FMT, RIFF and DATA chunks that are
     * used by the program, It also extracts the index at which the data chunk
     * starts
     * @param headerData - byte data containing the header information
     * @param chunkId - chunk id to identify the current chunk
     * @param idx - starting index of the current chunk
     * @return - no of bytes used by the current chunk
     */
    private int extractChunkData(byte[] headerData, String chunkId, int idx) {
        int chunkDataSize = readIntChunks(headerData, idx + 4, idx + 7);
        idx = idx + 8;
        if (CHUNK_RIFF.equalsIgnoreCase(chunkId)) {
            riffType = readStringChunks(headerData, idx, idx + 3);
            return 12;
        } else if (CHUNK_FMT.equalsIgnoreCase(chunkId)) {
            noOfChannels = readIntChunks(headerData, idx + 2, idx + 3);
            samplingRate = readIntChunks(headerData, idx + 4, idx + 7);
            averageBps = readIntChunks(headerData, idx + 8, idx + 11);
            significantBitsPerSecond =
                    readIntChunks(headerData, idx + 14, idx + 15);
        } else if (CHUNK_DATA.equalsIgnoreCase(chunkId)) {
            dataChunkIdx = idx;
            noOfDataBytes = chunkDataSize;
            bytesPerChannel = significantBitsPerSecond / 8;
            bpsAggregate = bytesPerChannel * noOfChannels;
            totalDataLength = noOfDataBytes / bpsAggregate;
        }
        return chunkDataSize + 8;
    }

    /**
     * Helper method to read little endian int data located between the given
     * from and to indexes of the given byte array
     * @param b - byte array
     * @param fromidx - from index
     * @param toidx - to index
     * @return - data read as an int
     */
    private int readIntChunks(byte[] b, int fromidx, int toidx) {
        byte[] chunk = extractChunk(b, fromidx, toidx);
        ByteBuffer wrapped =
                ByteBuffer.wrap(chunk).order(ByteOrder.LITTLE_ENDIAN);
        return wrapped.getInt();
    }

    /**
     * Helper method to read little endian String data located between the
     * given from and to indexes of the given byte array
     * @param b - byte array
     * @param fromidx - from index
     * @param toidx - to index
     * @return - data read as an String
     */
    private String readStringChunks(byte[] b, int fromidx, int toidx) {
        byte[] chunk = extractChunk(b, fromidx, toidx);
        return new String(chunk);
    }

    /**
     * Helper method to return copy of a part of the given byte array between
     * the given from and to indexes
     * @param b - byte array
     * @param fromidx - from index
     * @param toidx - to index
     * @return - copy of a part of the given byte array between the given from
     *         and to indexes
     */
    private byte[] extractChunk(byte[] b, int fromidx, int toidx) {
        byte[] chunk = new byte[4];
        System.arraycopy(b, fromidx, chunk, 0, toidx + 1 - fromidx);
        return chunk;
    }

    /**
     * Converts the given data to a fixed canonical form. It parses little
     * endian data into a signed 16-bit int and combines left and right
     * channels by averaging them together.
     * @param fileData
     * @param lengthForAChannel
     * @return - double[] of audio sample data in canonical form
     */
    private double[] extractChannelData(byte[] fileData,
            int lengthForAChannel) {
        int idx = 0, val = 0;
        boolean isSingleChannel = (noOfChannels == 1);
        double[] mergedSamples = new double[lengthForAChannel];
        double right = 0, left = 0;
        for (int i = 0; i < fileData.length; i = i + bytesPerChannel) {
            if (bytesPerChannel == 2) {
                val = (fileData[i] & 0xFF) | (fileData[i + 1]) << 8;
            } else if (bytesPerChannel == 1) {
                val =
                        (fileData[i] & 0x80) > 0 ? fileData[i] + 128
                                : fileData[i] - 128;
            } else {
                throwException(String
                        .format(BPS_NOT_SUPPORTED, bytesPerChannel));
            }
            if (i % bpsAggregate == 0) {
                right = val;
                if (isSingleChannel) {
                    mergedSamples[idx++] = right;
                }
            } else {
                left = val;
                mergedSamples[idx++] = (right + left) / 2;
            }
        }
        return convertToCanonicalForm(mergedSamples);
    }

    /**
     * modifies the number of samples of the given data so that it conforms to
     * the canonical form
     * 
     * @param data - 16 bit audio sample data
     * @return - double[] of audio sample in canonical form
     */
    private double[] convertToCanonicalForm(double[] data) {
        double conversionFactorExact = CANONICAL_SAMPLING_RATE / samplingRate;
        int conversionFactor = (int) conversionFactorExact;
        if (conversionFactorExact != 0.91875
                && conversionFactorExact != conversionFactor) {
            throwException(UNSUPPORTED_SAMPLING_RATE);
        }
        if (conversionFactor == 1) {
            return data;
        } else if (conversionFactorExact == 0.91875) {
            return downSample(data, conversionFactorExact);
        } else {
            return upSample(data, conversionFactor);
        }
    }

    /**
     * To down-sample audio data by the given conversion factor
     * @param data - 16 bit audio sample data
     * @param conversionFactor - factor by which the given data is to be
     *            downsampled
     * @return - double[] of downsampled data
     */
    private double[] downSample(double[] data, double conversionFactor) {
        int noOfSamplesOneLess = data.length - 1;
        int newlen = (int) (data.length * conversionFactor);
        double currentIndex, sampleAtRight, sampleAtLeft, slope;
        int leftIndex, rightIndex;
        double[] downsampledData = new double[newlen];
        for (int i = 0; i < newlen; i++) {
            currentIndex = i / conversionFactor;
            leftIndex = (int) currentIndex;
            rightIndex = Math.min(leftIndex + 1, noOfSamplesOneLess);
            sampleAtRight = data[rightIndex];
            sampleAtLeft = data[leftIndex];
            slope = sampleAtRight - sampleAtLeft;
            downsampledData[i] =
                    slope * (currentIndex - leftIndex) + sampleAtLeft;
        }
        return downsampledData;
    }

    /**
     * To up-sample audio data by the given conversion factor. Currently
     * supports conversion factors of 2 and 4
     * @param data - 16 bit audio sample data
     * @param conversionFactor - factor by which the given data has to be
     *            upsampled
     * @return - double[] of upsampled data
     */
    private double[] upSample(double[] data, int conversionFactor) {
        int dataLength = data.length;
        if (conversionFactor != 2 && conversionFactor != 4) {
            throwException(UNSUPPORTED_SAMPLING_RATE);
        }
        double[] upSampledData = new double[dataLength * conversionFactor];
        for (int i = 0; i < dataLength - 1; i++) {
            double tmp = data[i];
            int j = i * conversionFactor;
            upSampledData[j] = tmp;
        }
        return upSampledData;
    }

    /**
     * To validate if the encapsulated audio file is a valid .wav file
     * @return - true if the audio file is a valid .wav file, false otherwise
     */
    @Override
    public boolean isAudioFileFormatValid() {
        if (!"WAVE".equalsIgnoreCase(riffType)) {
            return false;
        }
        if (dataChunkIdx == 0) {
            return false;
        }
        if (noOfChannels > 2 || noOfChannels < 1) {
            return false;
        }
        return true;
    }

    /**
     * To validate if the given file name has a .wav extension
     * 
     * @param fileName - file name with extension
     * @return - true if the given file has a valid extension, false otherwise
     */
    public static boolean isFileExtensionValid(String fileName) {
        int fnameLength = fileName.length();
        if (fnameLength < 4) {
            return false;
        }
        if (!fileName.substring(fnameLength - 4, fnameLength).equals(".wav")) {
            return false;
        }
        return true;
    }

    /**
     * @return - the short name of the encapsulated .wav file along with the
     *         file extension
     */
    public String getShortName() {
        return shortName;
    }

    /**
     * @return - the full file name of the encapsulated .wav file along with
     *         the path
     */
    public String getFileName() {
        return fileName;
    }

    /**
     * @return - the duration of the encapsulated .wav file in seconds
     */
    public int getDurationInSeconds() {
        return (int) (noOfDataBytes / averageBps);
    }

    /**
     * @return - bits per second of the encapsulated .wav file
     */
    @Override
    public int getBps() {
        return significantBitsPerSecond;
    }

}
