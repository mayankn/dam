import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.HashMap;
import java.util.Map;

/**
 * 
 * @author: Magesh Ramachandran
 * @author: Mayank Narashiman
 * @author: Narendran K.P
 * 
 */
public class Mp3File extends WavFile {

    private Map<String, Object> headerMap = new HashMap<String, Object>();
    private byte[] fileData;
    private String shortName;

    private static String getModifiedName(String fName, boolean isDirectory) {
        String mpName;
        if (isDirectory) {
            mpName =
                    fName.substring(fName.lastIndexOf(File.separator,
                            fName.lastIndexOf(File.separator) - 1));
        } else {
            mpName = fName.substring(fName.lastIndexOf(File.separator));
        }
        System.out.println(mpName);
        mpName =
                File.separator + "tmp" + File.separator
                        + mpName.replaceAll("(.mp3)$", ".wav");
        System.out.println(mpName);
        return mpName;
    }

    public Mp3File(String fName, boolean isDirectory) throws IOException {
        super(getModifiedName(fName, isDirectory));
        File f = new File(fName);
        if (!f.isFile()) {
            throw new RuntimeException(String.format(INVALID_FILE_PATH, fName));
        }
        shortName = f.getName();
        RandomAccessFile rf = new RandomAccessFile(f, "r");
        fileData = new byte[4];
        rf.read(fileData);
        rf.close();
        if (fileData.length < 4) {
            throw new RuntimeException("ERROR: insufficient data in file");
        }
        extractHeaderData();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getShortName() {
        return shortName;
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
        System.out.println(headerMap);
    }

    public static void main(String arg[]) throws IOException {
        AudioFile a = new Mp3File("D:\\audiosample\\Sor3508.mp3", true);
    }

    private String readBitChunks(byte[] fileData, int si, int ei) {
        String a =
                Integer.toBinaryString(fileData[3] & 0xff)
                        + Integer.toBinaryString(fileData[2] & 0xff)
                        + Integer.toBinaryString(fileData[1] & 0xff)
                        + Integer.toBinaryString(fileData[0] & 0xff);
        // ByteBuffer wrapped =
        // ByteBuffer.wrap(cpy).order(ByteOrder.LITTLE_ENDIAN);
        // int a = wrapped.getInt();
        // return Integer.toBinaryString(a).substring(si,ei+1);
        return a.substring(si, ei + 1);
        // return wrapped.getInt();
    }

}
