import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Map;

/**
 * 
 * @author: Magesh Ramachandran
 * @author: Mayank Narashiman
 * @author: Narendran K.P
 * Description: This class converts the byte data from a valid mp3 audio file
 * into a canonical format suitable for analysis.
 * The canonical format used by the file is CD-quality audio with 16 bits per
 * sample, single channel, 44.1 Khz sampling rate represented as a double[]
 * array.
 * Prerequisites: Requires the software 'lame' to be pre-installed in the
 * path "/course/cs5500f14/bin/lame" which has to be accessible
 * 
 */
public class Mp3File extends AudioFile {

    private static String LAME_DECODER_PATH = "/course/cs5500f14/bin/lame";
    private static String CONVERTED_FILES_DIRECTORY = File.separator + "tmp"
            + File.separator + "dam-mmn";

    private Mp3decoder mp3Decoder;
    private AudioFile internalRepresentation;
    private Thread conversionProcess;

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
                convertedFileName =
                        CONVERTED_FILES_DIRECTORY + File.separator + paramNum
                                + File.separator + nameWithWavExtension;
                ProcessBuilder p =
                        new ProcessBuilder(LAME_DECODER_PATH, "--decode",
                                fileName, convertedFileName);
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
                    throwException(UNEXPECTED_ERROR);
                }
                proc.destroy();
                p = null;
                try {
                    convertedFile = new WavFile(convertedFileName);
                } catch (Exception e) {
                    // do nothing
                }
            } catch (IOException e) {
                throwException(UNEXPECTED_ERROR);
            }
        }
    }

    public Mp3File(String fName, boolean isDirectory, int paramNum)
            throws IOException {
        super(fName, false);
        mp3Decoder = new Mp3decoder(getShortName(), getFileName(), paramNum);
        conversionProcess = new Thread(mp3Decoder);
        conversionProcess.start();
    }

    @Override
    public Map<String, Object> getHeaderData() {
        setInternalRepresentation();
        return internalRepresentation.getHeaderData();
    }

    @Override
    public boolean areFileDurationsTheSame(AudioFile af2) {
        setInternalRepresentation();
        return internalRepresentation.areFileDurationsTheSame(af2);
    }

    @Override
    public double[] getChannelData() {
        setInternalRepresentation();
        return internalRepresentation.getChannelData();
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

    @Override
    public int getBps() {
        setInternalRepresentation();
        return internalRepresentation.getBps();
    }

    /**
     * Description - stores an internal representation of the converted MP3
     * file
     */
    private void setInternalRepresentation() {
        if (internalRepresentation == null) {
            try {
                conversionProcess.join();
            } catch (InterruptedException ie) {
                throwException(UNEXPECTED_ERROR);
            }
            internalRepresentation = mp3Decoder.getConvertedFile();
            if (internalRepresentation == null)
                throwException(String.format(UNSUPPORTED_FILE_FORMAT,
                        getShortName()));
            mp3Decoder = null;
            conversionProcess = null;
            fileData = null;
        }
    }

}
