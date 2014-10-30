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
    private int duplicatingFactor = 1;

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
        canonicalizeFile();
    }

    private void canonicalizeFile() {
        int sampleRate = (Integer)headerMap.get("FMT_SAMPLE_RATE");
        switch (sampleRate) {
            case 44100:
                break;
            case 22050:
            case 11025:
                duplicatingFactor = (CANONICAL_SAMPLING_RATE / sampleRate);
                //convertToCanonicalForm(sampleRate);
                break;
            case 48000:
                // TODO
                break;
        }

    }

    private void convertToCanonicalForm(int sampleRate) {
        //duplicatingFactor = (CANONICAL_SAMPLING_RATE / sampleRate);
        int duplicatingLength = (duplicatingFactor * (fileData.length - 44)) + 44;
        System.out.println("duplicating length: " + duplicatingLength);
        System.out.println("file data length: " + fileData.length);
        System.out.println("sample rate: " + sampleRate);
        byte[] newFileData = new byte[duplicatingLength];
        byte tmp;
        /*for (int i = 44; i < duplicatingLength; i = i + duplicatingFactor) {
            tmp = fileData[i/duplicatingFactor];
            //tmp = 0;
            switch (duplicatingFactor) {
                case 2:
                    newFileData[i] = tmp;
                    newFileData[i + 1] = (byte)0;
                    break;
                case 4:
                    newFileData[i] = tmp;
                    newFileData[i + 1] = tmp;
                    newFileData[i + 2] = tmp;
                    newFileData[i + 3] = tmp;
            }
        }*/
        int j = 44;
        for(int i = 44; i < fileData.length-1; i++) {
            tmp = fileData[i];
            tmp = (byte) 0;
            switch (duplicatingFactor) {
                case 2:
                    newFileData[j] = tmp;
                    newFileData[j+1] = 0;
                    j += duplicatingFactor;
                    break;
                case 4:
                    newFileData[j] = tmp;
                    newFileData[j+1] = 0;
                    newFileData[j+2] = 0;
                    newFileData[j+3] = 0;
                    j += duplicatingFactor;
                    break;
            }
        }
        fileData = newFileData;
        //headerMap.put("DATA_DWORD", ((Integer)headerMap.get("DATA_DWORD") * duplicatingFactor) - 4);
    }

    private double[] convertToCanonicalForm(double[] fileSamples) {
        int samplesLength = fileSamples.length;
        double[] newFileSamples = new double[samplesLength * duplicatingFactor];
        for(int i = 0; i < samplesLength-1; i++) {
            double tmp = fileSamples[i];
            switch (duplicatingFactor) {
                case 2:
                    newFileSamples[i*duplicatingFactor] = tmp;
                    newFileSamples[(i*duplicatingFactor) + 1] = tmp;
                    // this is for linear interpolation (thos works too)
                    //newFileSamples[(i*duplicatingFactor) + 1] = (tmp + fileSamples[i+1])/2;
                    break;
                case 4:
                    newFileSamples[i*duplicatingFactor] = tmp;
                    newFileSamples[(i*duplicatingFactor) + 1] = tmp;
                    newFileSamples[(i*duplicatingFactor) + 2] = tmp;
                    newFileSamples[(i*duplicatingFactor) + 3] = tmp;
                    break;
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
        channelDataLength = channelDataLength * duplicatingFactor;
        //System.out.println((Integer) headerMap.get("DATA_DWORD") + " " + bps);
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
                val = fileData[i];
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
        if (duplicatingFactor != 1)
            return convertToCanonicalForm(avg);
        return avg;
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
     * @param fileName
     *            - file name
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
