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
                try {
                convertedFile = new WavFile(convertedFileName);
                } catch(Exception e) {
                	//do nothing
                }
            } catch (IOException e) {                
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
       // System.out.println(headerMap);
    }

    @Override
    public Map<String, Object> getHeaderData() {
    	//TODO: to be modified
    	setInternalRepresentation();
    	return internalRepresentation.getHeaderData();
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
  
    private void setInternalRepresentation() {
        if (internalRepresentation == null) {
            try {
                thisConverter.join();
            } catch (InterruptedException ie) {
                throw new RuntimeException(
                        "ERROR: an unexpected error has occured");
            }
            internalRepresentation = mp3Decoder.getConvertedFile();
            if(internalRepresentation == null)
            	throw new RuntimeException("ERROR: mp3 file format is invalid");
            mp3Decoder = null;
            thisConverter = null;
            fileData = null;
        }
    }

}
