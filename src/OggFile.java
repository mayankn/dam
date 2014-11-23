import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * <p>
 * This class converts the byte data from a valid Ogg audio file into a
 * canonical format suitable for analysis. The canonical format used by the file
 * is CD-quality audio with 16 bits per sample, single channel, 44.1 Khz
 * sampling rate represented as a double[] array.
 * 
 * <p>
 * Prerequisites: Requires the software 'oggdec' to be pre-installed in the path
 * "/usr/bin/oggdec" which has to be accessible
 * 
 * @author: Magesh Ramachandran
 * @author: Mayank Narashiman
 * @author: Narendran K.P
 * 
 */
public class OggFile extends AudioFile {

    private static String OGG_DECODER_PATH = "/usr/bin/oggdec";
    private static String CONVERTED_FILES_DIRECTORY = File.separator + "tmp"
            + File.separator + "dam-mmn";

    private OggDecoder oggDecoder;
    private AudioFile internalRepresentation;
    private Thread conversionProcess;
    private String fileName, shortName;

    static class OggDecoder implements Runnable {
        String shortName;
        String fileName;
        int paramNum;
        AudioFile convertedFile;

        private OggDecoder(String shortName, String fileName, int paramNum) {
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
                boolean hasErrorOccured = false;
                String nameWithWavExtension =
                        shortName.replaceAll("(.mp3)$", ".wav");
                convertedFileName =
                        CONVERTED_FILES_DIRECTORY + File.separator + paramNum
                                + File.separator + AudioFile.FILE_TYPE.MP3
                                + File.separator + nameWithWavExtension;
                ProcessBuilder p =
                        new ProcessBuilder(OGG_DECODER_PATH, "--bits 16",
                                fileName, "--output", convertedFileName);
                p.redirectErrorStream(true);
                Process proc = p.start();
                BufferedReader reader =
                        new BufferedReader(new InputStreamReader(
                                proc.getInputStream()));
                String str;
                while ((str = reader.readLine()) != null) {
                    if (str.contains("ERROR:")) {
                        hasErrorOccured = true;
                    }
                }
                reader.close();
                if (hasErrorOccured) {
                    return;
                }
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

    public OggFile(String fName , int paramNum)
            throws IOException {
        this.fileName = fName;
        File f = new File(fileName);
        if (!f.isFile()) {
            throwException(String.format(INVALID_FILE_PATH, fileName));
        }
        shortName = f.getName();
        oggDecoder = new OggDecoder(shortName, fileName, paramNum);
        conversionProcess = new Thread(oggDecoder);
        conversionProcess.start();
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
     * Description - stores an internal representation of the converted MP3 file
     */
    private void setInternalRepresentation() {
        if (internalRepresentation == null) {
            try {
                conversionProcess.join();
            } catch (InterruptedException ie) {
                throwException(UNEXPECTED_ERROR);
            }
            internalRepresentation = oggDecoder.getConvertedFile();
            if (internalRepresentation == null)
                throwException(String
                        .format(UNSUPPORTED_FILE_FORMAT, shortName));
            oggDecoder = null;
            conversionProcess = null;
            // fileData = null;
        }
    }

    @Override
    public double[] getNext(int streamingLength) {
        setInternalRepresentation();
        return internalRepresentation.getNext(streamingLength);
    }

    public String getShortName() {
        return this.shortName;
    }

    @Override
    public boolean hasNext() {
        setInternalRepresentation();
        return internalRepresentation.hasNext();
    }

    @Override
    public void close() {
        setInternalRepresentation();
        internalRepresentation.close();
    }

}
