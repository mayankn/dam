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

	private static String CONVERT_TO_WAV_COMMAND =
            "/course/cs5500f14/bin/lame --decode %s %s";
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

    static class Mp3decoder implements Runnable{
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
    		String nameWithWavExtension = shortName.replace(".", "") + ".wav";
    		String cmd = String.format(CONVERT_TO_WAV_COMMAND, fileName,
    				CONVERTED_FILES_DIRECTORY + File.separator + paramNum
    						+ File.separator + nameWithWavExtension);
    		Process proc = Runtime.getRuntime().exec(cmd);
    		
    		 BufferedReader reader = new BufferedReader(
                     new InputStreamReader(proc.getInputStream()));
    		 BufferedReader errReader = new BufferedReader(
                     new InputStreamReader(proc.getErrorStream()));
             while (reader.readLine() != null && errReader.readLine() != null) {                 
             }
             reader.close();
             errReader.close();
    		int exitValue = 1;
    		try {
    			exitValue = proc.waitFor();
    		} catch (InterruptedException ie) {
    			throw new RuntimeException("ERROR: An unexpected error has occured");
    		}

    		if (exitValue == 0)
    			convertedFileName = CONVERTED_FILES_DIRECTORY + File.separator
    					+ paramNum + File.separator + nameWithWavExtension;
    		
				convertedFile = new WavFile(convertedFileName);
			} catch (IOException e) {
				throw new RuntimeException("ERROR: An unexpected error has occured");
			}
    	}
    }

    public Mp3File(String fName, boolean isDirectory, int paramNum) throws IOException {
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
       // convertFileToInternalRepresentation();
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

    private String readBitChunks(byte[] fileData, int si, int ei) {
        String a =
                Integer.toBinaryString(fileData[3] & 0xff)
                        + Integer.toBinaryString(fileData[2] & 0xff)
                        + Integer.toBinaryString(fileData[1] & 0xff)
                        + Integer.toBinaryString(fileData[0] & 0xff);
        return a.substring(si, ei + 1);     
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
		}
	}

}
