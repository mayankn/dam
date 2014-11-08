import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.HashMap;
import java.util.Map;

/**
 * 
 * @author: Magesh Ramachandran
 * @author: Mayank Narashiman
 * @author: Narendran K.P
 * 
 */
public class WavFile extends AudioFile {
    private byte[] fileData;
    private String fileName;
    private String shortName;
    private static final int CANONICAL_SAMPLING_RATE = 44100;
    private Map<String, Object> headerMap = new HashMap<String, Object>();

    public WavFile(String fName) throws IOException {
        this.fileName = fName;
        File f = new File(fileName);
        if (!f.isFile()) {
            throw new RuntimeException(String.format(INVALID_FILE_PATH,
                    fileName));
        }
        shortName = f.getName();
        RandomAccessFile rf = new RandomAccessFile(f, "r");
        fileData = new byte[(int) rf.length()];
        rf.read(fileData);
        rf.close();
        if (fileData.length < 44) {
            throw new RuntimeException("ERROR: insufficient data in file");
        }
        extractHeaderData();
    }

    private double[] convertToCanonicalForm(double[] data) {
        int dataLength = data.length;
        int sampleRate = (Integer) headerMap.get("FMT_SAMPLE_RATE");
        double duplicatingFactorOrig =
                (double) CANONICAL_SAMPLING_RATE / sampleRate;
        int duplicatingFactor = (int) duplicatingFactorOrig;
        if (duplicatingFactorOrig != 0.91875
                && duplicatingFactorOrig != duplicatingFactor) {
            throw new RuntimeException("ERROR: Unsupported sampling rate");
        }

        if (duplicatingFactor == 1) {
            return data;
        }
        double[] newFileSamples;
        if (duplicatingFactorOrig == 0.91875) {
            int newlen = (int) (dataLength * duplicatingFactorOrig);
            newFileSamples = new double[newlen];
            for (int i = 0; i < newlen; i++) {
                double currentPosition = i / duplicatingFactorOrig;
                int nearestLeftPosition = (int) currentPosition;
                int nearestRightPosition = nearestLeftPosition + 1;
                if (nearestRightPosition >= data.length) {
                    nearestRightPosition = data.length - 1;
                }
                double slope =
                        (data[nearestRightPosition] - data[nearestLeftPosition]);
                double positionFromLeft = currentPosition - nearestLeftPosition;
                newFileSamples[i] =
                        ((slope * positionFromLeft) + data[nearestLeftPosition]);
            }
        } else {
            newFileSamples = new double[dataLength * duplicatingFactor];
            for (int i = 0; i < dataLength - 1; i++) {
                double tmp = data[i];
                int j = i * duplicatingFactor;
                double offset = (data[i + 1] - tmp);
                switch (duplicatingFactor) {
                case 2:
                    newFileSamples[j] = tmp;
                    newFileSamples[j + 1] = (tmp + offset / 2);
                    break;
                case 4:
                    newFileSamples[j] = tmp;
                    newFileSamples[j + 1] = tmp + (offset / 4);
                    newFileSamples[j + 2] = tmp + (offset * (2 / 4));
                    newFileSamples[j + 3] = tmp + (offset * (3 / 4));
                    break;
                default:
                    throw new RuntimeException(
                            "ERROR: Unsupported sampling rate");
                }
            }
        }
        return newFileSamples;
    }

    /**
     * To extract the header data from wav file contained by this instance
     */
    private void extractHeaderData() {
        headerMap.put("CHUNK_ID", readStringChunks(fileData, 0, 3));
        headerMap.put("CHUNK_DATA_SIZE", readIntChunks(fileData, 4, 7));
        headerMap.put("RIFF_TYPE", readStringChunks(fileData, 8, 11));
        headerMap.put("FMT_CHUNK_ID", readStringChunks(fileData, 12, 15));
        headerMap.put("FMT_CHUNK_DATA_SIZE", readIntChunks(fileData, 16, 19));
        headerMap.put("FMT_COMPRESSION_CODE", readIntChunks(fileData, 20, 21));
        headerMap.put("FMT_NO_OF_CHANNELS", readIntChunks(fileData, 22, 23));
        headerMap.put("FMT_SAMPLE_RATE", readIntChunks(fileData, 24, 27));
        headerMap.put("FMT_AVERAGE_BPS", readIntChunks(fileData, 28, 31));
        headerMap.put("FMT_BLOCK_ALIGN", readIntChunks(fileData, 32, 33));
        headerMap.put("FMT_SIGNIFICANT_BPS", readIntChunks(fileData, 34, 35));
        headerMap.put("DATA_CHUNK_ID", readStringChunks(fileData, 36, 39));
        headerMap.put("DATA_DWORD", readIntChunks(fileData, 40, 43));
        // does not exist for this file
        // headerMap.put("FMT_EXTRA_FMT_BYTES", readIntChunks(fileData, 36,
        // 37));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<String, Object> getHeaderData() {
        return headerMap;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean areFileDurationsTheSame(AudioFile af2) {
        double file1duration = extractDurationFromHeader(headerMap);
        double file2duration = extractDurationFromHeader(af2.getHeaderData());
        if (file1duration != file2duration) {
            return false;
        }
        return true;
    }

    public int getDurationInSeconds() {
        return (int) extractDurationFromHeader(headerMap);
    }

    /**
     * {@inheritDoc}
     */
    private double extractDurationFromHeader(Map<String, Object> fileHeader) {
        int datalength = (Integer) fileHeader.get("DATA_DWORD");
        int averagebps = (Integer) fileHeader.get("FMT_AVERAGE_BPS");
        return datalength / averagebps;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double[] extractChannelData() {
        int numChannels = (Integer) headerMap.get("FMT_NO_OF_CHANNELS");
        int bpsPerChannel = (Integer) headerMap.get("FMT_SIGNIFICANT_BPS") / 8;
        int bps = bpsPerChannel * numChannels;
        int channelDataLength = ((Integer) headerMap.get("DATA_DWORD")) / bps;
        boolean isSingleChannel = (numChannels == 1);
        double[] avg = new double[channelDataLength];
        int idx = 0;
        byte[] t = new byte[bpsPerChannel];
        double right = 0, left = 0;
        // TODO : remove hard code if possible
        for (int i = 44; i < fileData.length; i = i + bpsPerChannel) {
            System.arraycopy(fileData, i - bpsPerChannel, t, 0, bpsPerChannel);

            ByteBuffer wrapped =
                    ByteBuffer.wrap(t).order(ByteOrder.LITTLE_ENDIAN);
            int val = 0;

            if (bpsPerChannel == 2) {
                val = wrapped.getShort();
            } else if (bpsPerChannel == 4) {
                val = wrapped.getInt();
            } else if (bpsPerChannel == 1) {
                val = wrapped.get();
            } else {
                throw new RuntimeException("ERROR: Bytes per sample of "
                        + bpsPerChannel + " is not supported ");
            }
            // TODO: verify
            if (i % bps == 0) {
                right = val;
                if (isSingleChannel) {
                    avg[idx++] = right;
                }
            } else {
                left = val;
                avg[idx++] = (right + left) / 2;
            }
        }
        return convertToCanonicalForm(avg);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isAudioFileFormatValid() {
        // System.out.println(headerMap.toString());
        if (!headerMap.get("RIFF_TYPE").toString().equalsIgnoreCase("WAVE")) {
            return false;
        }
        if (!headerMap.get("DATA_CHUNK_ID").toString().equalsIgnoreCase("data")) {
            return false;
        }
        if ((Integer) headerMap.get("FMT_NO_OF_CHANNELS") > 2) {
            return false;
        }
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getShortName() {
        return shortName;
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
