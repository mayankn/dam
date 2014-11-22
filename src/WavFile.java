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
 *          </br> Description: This class converts the given audio sample with
 *          related byte data into suitable canonical form that is used for
 *          analysis. The canonical form is a CD-quality 16-bit PCM audio wav
 *          format with 44.1 Khz sampling rate, represented as a double[] array.
 * 
 */
public class WavFile extends AudioFile {
    private static final double CANONICAL_SAMPLING_RATE = 44100;
    private static final String CHUNK_RIFF = "RIFF";
    private static final String CHUNK_FMT = "fmt";
    private static final String CHUNK_DATA = "data";

    private double[] channelData;
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

    public WavFile(String fName) throws IOException {
        this.fileName = fName;
        File f = new File(fileName);
        if (!f.isFile()) {
            throwException(String.format(INVALID_FILE_PATH, fileName));
        }
        shortName = f.getName();
        rf = new RandomAccessFile(f, "r");
        int fileLength = (int) rf.length();
        if (fileLength < 44) {
            throwException(INSUFFICIENT_DATA);
        }
        byte[] headerData = new byte[80];
        rf.read(headerData);
        readHeaderChunks(headerData);
        rf.seek(dataChunkIdx);
    }

    public boolean hasNext() {
        return (dataLengthRead < totalDataLength);
    }

    public double[] getNext(int streamingLength) {
        try {
            int bytesToBeStreamed = streamingLength * bpsAggregate;
            int dataLeft = (totalDataLength - dataLengthRead) * bpsAggregate;
            bytesToBeStreamed = Math.min(bytesToBeStreamed, dataLeft);
            byte[] fileData = new byte[bytesToBeStreamed];
            rf.read(fileData);
            dataLengthRead = dataLengthRead + streamingLength;
            return extractChannelData(fileData, bytesToBeStreamed
                    / bpsAggregate);
        } catch (IOException e) {
            throw new RuntimeException("ERROR: Error while reading the file "
                    + shortName);
        }
    }

    public void close() {
        try {
            rf.close();
        } catch (IOException e) {
            throw new RuntimeException("ERROR: Error while closing the file"
                    + shortName);
        }
    }

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

    public int getTotalDataLength() {
        return totalDataLength;
    }

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

    private int readIntChunks(byte[] b, int fromidx, int toidx) {
        byte[] chunk = extractChunk(b, fromidx, toidx);
        ByteBuffer wrapped =
                ByteBuffer.wrap(chunk).order(ByteOrder.LITTLE_ENDIAN);
        return wrapped.getInt();
    }

    private byte[] extractChunk(byte[] b, int fromidx, int toidx) {
        byte[] chunk = new byte[4];
        System.arraycopy(b, fromidx, chunk, 0, toidx + 1 - fromidx);
        return chunk;
    }

    private String readStringChunks(byte[] b, int fromidx, int toidx) {
        byte[] chunk = extractChunk(b, fromidx, toidx);
        return new String(chunk);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<String, Object> getHeaderData() {
        return null;
    }

    public int getDurationInSeconds() {
        return (int) getFileDuration();
    }

    private double getFileDuration() {
        return noOfDataBytes / averageBps;
    }

    @Override
    public int getBps() {
        return significantBitsPerSecond;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double[] getChannelData() {
        return this.channelData;
    }

    /**
     * Converts the file data to canonical form
     */
    private double[] extractChannelData(byte[] fileData, int channelLength) {
        int lengthForAChannel, idx = 0, val = 0;
        lengthForAChannel = channelLength;
        boolean isSingleChannel = (noOfChannels == 1);
        double[] mergedSamples = new double[lengthForAChannel];
        double right = 0, left = 0;
        // System.out.println("fileData.length"+fileData.length);
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
     * To Convert the data to canonical format based on sampling rate and bit
     * rate
     * @param data
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
     * To down-sample audio data by the given conversion facor
     * @param data
     * @param conversionFactor
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
     * To up-sample audio data by the given conversion factor Currently supports
     * conversion factors of 2 and 4
     * @param data - single channel audio data
     * @param conversionFactor - factor by which the sample has to be upsampled
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
     * {@inheritDoc}
     */
    @Override
    public boolean isAudioFileFormatValid() {
        if (!riffType.equalsIgnoreCase("WAVE")) {
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
     * To validate if the given file name is valid and has .wav extension
     * 
     * @param fileName - file name
     * @return - true if the given file name is valid, false otherwise
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

}
