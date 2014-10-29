import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * @author: Magesh Ramachandran
 * @author: Mayank Narashiman
 * @author: Narendran K.P
 */
public class Mp3File extends AudioFile {
    private byte[] fileData;
    private String fileName;
    private String shortName;
    private Map<String, Object> headerMap = new HashMap<String, Object>();

    public Mp3File(String fName) throws IOException {
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

    /**
     * To extract the header data from wav file contained by this instance
     */
    private void extractHeaderData() {
        headerMap.put("FRAME_SYNC", readBitChunks(fileData, 21, 30));
        headerMap.put("AUDIO_VERSION", readBitChunks(fileData, 19, 20));
        headerMap.put("LAYER_DESCRIPTION", readBitChunks(fileData, 17, 18));
        headerMap.put("PROTECTION_BIT", readBitChunks(fileData, 16, 16));
        headerMap.put("BITRATE_INDEX", readBitChunks(fileData, 12, 15));
        headerMap.put("SAMPLING_FREQ_INDEX", readBitChunks(fileData, 10, 11));
        headerMap.put("PADDING_BIT", readBitChunks(fileData, 9, 9));
        headerMap.put("PRIVATE_NIT", readBitChunks(fileData, 8, 8));
        headerMap.put("CHANNEL_MODE", readBitChunks(fileData, 6, 7));
        headerMap.put("MODE_EXTN", readBitChunks(fileData, 4, 5));
        headerMap.put("COPYRIGHT", readBitChunks(fileData, 3, 3));
        headerMap.put("ORIGINAL", readBitChunks(fileData, 2, 2));
        headerMap.put("EMPHASIS", readBitChunks(fileData, 0, 1));
        // does not exist for this file
        // headerMap.put("FMT_EXTRA_FMT_BYTES", readIntChunks(fileData, 36,
        // 37));
        System.out.println(headerMap);
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
                    ByteBuffer.wrap(t).order(ByteOrder.BIG_ENDIAN);
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

    public static void main(String arg[]) throws IOException {
        AudioFile a = new Mp3File("D:\\audiosample\\Sor3508.mp3");
    }

    private String readBitChunks(byte[] fileData, int si, int ei) {
        byte[] cpy = Arrays.copyOfRange(fileData, 0, 4);
        String a = Integer.toBinaryString(cpy[3] & 0xff) + Integer.toBinaryString(cpy[2] & 0xff) +
                Integer.toBinaryString(cpy[1] & 0xff) + Integer.toBinaryString(cpy[0] & 0xff);
        //ByteBuffer wrapped =
        //      ByteBuffer.wrap(cpy).order(ByteOrder.LITTLE_ENDIAN);
        //int a = wrapped.getInt();
        //return Integer.toBinaryString(a).substring(si,ei+1);
        return a.substring(si, ei + 1);
        //  return wrapped.getInt();
    }

}
