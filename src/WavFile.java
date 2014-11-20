import java.io.IOException;
import java.util.Arrays;
import java.util.Map;

/**
 * 
 * @author: Magesh Ramachandran
 * @author: Mayank Narashiman
 * @author: Narendran K.P
 * Description: This class converts the given audio sample with related byte
 * data into suitable canonical form that is used for analysis purpose. The
 * canonical form is a CD-quality 16-bit PCM audio wav format with 44.1 Khz
 * sampling rate, represented as a double[] array.
 * 
 */
public class WavFile extends AudioFile {    
    private double[] channelData;
    private String riffType, dataChunk;
    private int noOfDataBytes;
    private int averageBps;
    private int samplingRate;
    private int noOfChannels;
    private int significantBitsPerSecond;
    private static final double CANONICAL_SAMPLING_RATE = 44100;

    public WavFile(String fName) throws IOException {
        super(fName, true);           
        if (fileData.length < 44) {
            throwException(INSUFFICIENT_DATA);
        }
        extractHeaderData();
        extractChannelData();
    }

    /**
     * To extract the header data from wav file contained by this instance
     */
    private void extractHeaderData() {
        byte[] headerData = Arrays.copyOf(fileData, 60);
        riffType = readStringChunks(headerData, 8, 11);
        dataChunk = readStringChunks(headerData, 36, 39);
        significantBitsPerSecond = readIntChunks(headerData, 34, 35);
        noOfChannels = readIntChunks(headerData, 22, 23);
        samplingRate = readIntChunks(headerData, 24, 27);
        averageBps = readIntChunks(headerData, 28, 31);
        noOfDataBytes = readIntChunks(headerData, 40, 43);
        // CHUNK_ID = readStringChunks(headerData, 0, 3));
        // CHUNK_DATA_SIZE = readIntChunks(headerData, 4, 7));
        // FMT_CHUNK_ID = readStringChunks(headerData, 12, 15));
        // FMT_CHUNK_DATA_SIZE = readIntChunks(headerData, 16, 19));
        // FMT_COMPRESSION_CODE = readIntChunks(headerData, 20, 21));
        // FMT_BLOCK_ALIGN = readIntChunks(headerData, 32, 33));
        // does not exist for this file
        // FMT_EXTRA_FMT_BYTES = readIntChunks(fileData, 36, 37));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<String, Object> getHeaderData() {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean areFileDurationsTheSame(AudioFile af2) {
        double file1duration = getDurationInSeconds();
        double file2duration = af2.getDurationInSeconds();
        if (file1duration != file2duration) {
            return false;
        }
        return true;
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
    private void extractChannelData() {
        int bytesPerChannel, bpsAggregate, lengthForAChannel, idx = 0, val = 0;
        bytesPerChannel = significantBitsPerSecond / 8;
        bpsAggregate = bytesPerChannel * noOfChannels;
        lengthForAChannel = noOfDataBytes / bpsAggregate;
        boolean isSingleChannel = (noOfChannels == 1);
        double[] mergedSamples = new double[lengthForAChannel];
        double right = 0, left = 0;
        // TODO : remove hard code if possible
        for (int i = 44; i < fileData.length; i = i + bytesPerChannel) {
            if (bytesPerChannel == 2) {
                val = (fileData[i] & 0xFF) | (fileData[i + 1]) << 8;
            } else if (bytesPerChannel == 4) {
                val =
                        (fileData[i] & 0xFF) | (fileData[i + 1]) << 8
                                | (fileData[i + 2]) << 16
                                | (fileData[i + 3]) << 24;
            } else if (bytesPerChannel == 1) {
                val = (fileData[i] & 0xFF);
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
        this.fileData = null;
        convertToCanonicalForm(mergedSamples);
    }


    /**
     * To Convert the data to canonical format based on sampling rate and
     * bit rate
     * @param data
     */
    private void convertToCanonicalForm(double[] data) {
        double conversionFactorExact = CANONICAL_SAMPLING_RATE / samplingRate;
        int conversionFactor = (int) conversionFactorExact;
        if (conversionFactorExact != 0.91875
                && conversionFactorExact != conversionFactor) {
            throwException(UNSUPPORTED_SAMPLING_RATE);
        }
        if (conversionFactor == 1) {
            this.channelData = data;
        } else if (conversionFactorExact == 0.91875) {
            downSample(data, conversionFactorExact);
        } else {
            upSample(data, conversionFactor);
        }

    }

    /**
     * To down-sample audio data by the given conversion facor
     * @param data
     * @param conversionFactor
     */
    private void downSample(double[] data, double conversionFactor) {
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
        this.channelData = downsampledData;
    }

    /**
     * To up-sample audio data by the given conversion factor
     * Currently supports conversion factors of 2 and 4
     * @param data - single channel audio data
     * @param conversionFactor - factor by which the sample has to be upsampled
     */
    private void upSample(double[] data, int conversionFactor) {
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
        this.channelData = upSampledData;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isAudioFileFormatValid() {
        if (!riffType.equalsIgnoreCase("WAVE")) {
            return false;
        }
        if (!dataChunk.equalsIgnoreCase("data")) {
            return false;
        }
        if (noOfChannels > 2) {
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
