import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
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
public class Mp3File extends AudioFile {

    private static String LAME_DECODER_PATH = "/course/cs5500f14/bin/lame";
    private static String INVALID_FILE_PATH =
            "ERROR: Incorrect file path, file %s was not found";
    private static String CONVERTED_FILES_DIRECTORY = File.separator + "tmp"
            + File.separator + "dam-mmn";

    private Map<String, Object> headerMap = new HashMap<String, Object>();
    private byte[] fileData;
    private String shortName;
    private String fileName;
    private Mp3decoder mp3Decoder;
    private AudioFile internalRepresentation;
    private Thread thisConverter;

    static class Mp3decoder implements Runnable {
        String shortName;
        String fileName;
        int paramNum;
        AudioFile convertedFile;

        private Mp3decoder(String shortName, String fileName, int paramNum) {
            this.shortName = shortName;
            this.fileName = fileName;
            this.paramNum = paramNum;
        }

        public AudioFile getConvertedFile() {
            return this.convertedFile;
        }

        public void run() {
            try {
                String convertedFileName = null;
                String nameWithWavExtension =
                        shortName.replaceAll("(.mp3)$", ".wav");
                ProcessBuilder p =
                        new ProcessBuilder(LAME_DECODER_PATH, "--decode",
                                fileName, CONVERTED_FILES_DIRECTORY
                                        + File.separator + paramNum
                                        + File.separator + nameWithWavExtension);
                p.redirectErrorStream(true);
                Process proc = p.start();
                BufferedReader reader =
                        new BufferedReader(new InputStreamReader(
                                proc.getInputStream()));
                while (reader.readLine() != null) {
                }
                reader.close();
                try {
                    proc.waitFor();
                } catch (InterruptedException ie) {
                    throw new RuntimeException(
                            "ERROR: An unexpected error has occured");
                }
                proc.destroy();
                p = null;
                convertedFileName =
                        CONVERTED_FILES_DIRECTORY + File.separator + paramNum
                                + File.separator + nameWithWavExtension;
                convertedFile = new WavFile(convertedFileName);
            } catch (IOException e) {
                e.printStackTrace();
                throw new RuntimeException(
                        "ERROR: An unexpected error has occured");
            }
        }
    }

    public Mp3File(String fName, boolean isDirectory, int paramNum)
            throws IOException {
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
        this.fileName = fName;
        validateFileFormat();
        mp3Decoder = new Mp3decoder(shortName, fileName, paramNum);
        thisConverter = new Thread(mp3Decoder);
        thisConverter.start();
    }

    private void validateFileFormat() {

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
    /*private void extractHeaderData() {
        headerMap.put("FRAME_SYNC", readBitChunks(fileData, 0, 10));
        headerMap.put("AUDIO_VERSION", readBitChunks(fileData, 11, 12));
        headerMap.put("LAYER_DESCRIPTION", readBitChunks(fileData, 13, 14));
        headerMap.put("PROTECTION_BIT", readBitChunks(fileData, 15, 15));
        headerMap.put("BIT_RATE_INDEX", readBitChunks(fileData, 16, 19));
        headerMap.put("SAMPLING_RATE_INDEX", readBitChunks(fileData, 20, 21));
        headerMap.put("PADDING_BIT", readBitChunks(fileData, 22, 22));
        headerMap.put("PRIVATE_BIT", readBitChunks(fileData, 23, 23));
        headerMap.put("CHANNEL_MODE", readBitChunks(fileData, 24, 25));
        headerMap.put("MODE_EXTN", readBitChunks(fileData, 26, 27));
        headerMap.put("COPYRIGHT", readBitChunks(fileData, 28, 28));
        headerMap.put("ORIGINAL", readBitChunks(fileData, 29, 29));
        headerMap.put("EMPHASIS", readBitChunks(fileData, 30, 30));
        System.out.println(headerMap);
    }*/

    /*private String readBitChunks(byte[] fileData, int si, int ei) {
        String a =
                Integer.toBinaryString(fileData[0] & 0xff)
                        + Integer.toBinaryString(fileData[1] & 0xff)
                        + Integer.toBinaryString(fileData[2] & 0xff)
                        + Integer.toBinaryString(fileData[3] & 0xff);
        return a.substring(si, ei + 1);
    }*/

    private void extractHeaderData() {
        String FRAME_SYNC =
                Integer.toBinaryString(fileData[0] & 0xff)
                        + Integer.toBinaryString(fileData[1] & 0xff);
        FRAME_SYNC = FRAME_SYNC.substring(0, 11);
        headerMap.put("FRAME_SYNC", FRAME_SYNC);
        headerMap.put("AUDIO_VERSION", ((fileData[1] >> 3) & 3));
        headerMap.put("LAYER_DESCRIPTION", ((fileData[1] >> 1) & 3));
        headerMap.put("PROTECTION_BIT", (fileData[1] & 1));
        headerMap.put("BIT_RATE", ((fileData[2] >> 4) & 15));
        headerMap.put("SAMPLING_RATE", ((fileData[2] >> 2) & 3));
        headerMap.put("PADDING_BIT", ((fileData[2] >> 1) & 1));
        headerMap.put("PRIVATE_BIT", (fileData[2] & 1));
        headerMap.put("CHANNEL_MODE", ((fileData[3] >> 6) & 3));
        headerMap.put("MODE_EXTN", ((fileData[3] >> 4) & 3));
        headerMap.put("COPYRIGHT", ((fileData[3] >> 3) & 1));
        headerMap.put("ORIGINAL", ((fileData[3] >> 2) & 1));
        headerMap.put("EMPHASIS", (fileData[3] & 3));
        System.out.println(headerMap);
    }

    @Override
    public Map<String, Object> getHeaderData() {
        return headerMap;
    }

    @Override
    public boolean areFileDurationsTheSame(AudioFile af2) {
        setInternalRepresentation();
        return internalRepresentation.areFileDurationsTheSame(af2);
    }

    @Override
    public double[] extractChannelData() {
        setInternalRepresentation();
        return internalRepresentation.extractChannelData();
    }

    @Override
    public boolean isAudioFileFormatValid() {
        setInternalRepresentation();
        return internalRepresentation.isAudioFileFormatValid();
    }

    @Override
    public int getDurationInSeconds() {
        setInternalRepresentation();
        return internalRepresentation.getDurationInSeconds();
    }

    public static void main(String arg[]) throws IOException {
        AudioFile a = new Mp3File("D:\\audiosample\\Sor3508.mp3", true, 1);
    }

    private void setInternalRepresentation() {
        if (internalRepresentation == null) {
            try {
                thisConverter.join();
            } catch (InterruptedException ie) {
                throw new RuntimeException(
                        "ERROR: an unexpected error has occured");
            }
            internalRepresentation = mp3Decoder.getConvertedFile();
            mp3Decoder = null;
            thisConverter = null;
            fileData = null;
        }
    }

}
