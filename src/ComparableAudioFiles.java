import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 
 * Factory class with method(s) to create specific instances of
 * {@ComparableAudioFile}
 * 
 * @author: Magesh Ramachandran
 * @author: Mayank Narashiman
 * @author: Narendran K.P
 * 
 */
public class ComparableAudioFiles {

    /**
     * 
     * Enumeration of execution modes supported by the 
     * {@ComparableAudioFiles} class.
     * 
     * <pre>
     * FAST - Fast mode, faster execution time with reduced accuracy. 
     * NORMAL - Default mode, slower execution time with improved accuracy.
     * </pre>
     * 
     */
    public enum MODES {
        FAST, NORMAL
    }

    private static final int SAMPLES_PER_FRAME = 1764;
    private static final int FFT_WINDOW_SIZE = 2048;
    private static final int FRAGMENT_SIZE_TO_MATCH_IN_SECONDS = 5;

    private static MODES mode;

    /**
     * This is an optional method used to set the execution mode. If no mode is
     * set, the program will execute with the default mode
     * @param m - {@MODES}
     * 
     */
    public static void setMode(MODES m) {
        mode = m;
    }

    /**
     * This method takes a list of {@AudioFile} and returns a list
     * of {@ComparableAudioFile} that encapsulate the
     * given files in order to facilitate perceptual comparison.
     * 
     * @param listOfFiles - a list of {@AudioFile} to be analyzed
     * 
     * @return - list of {@ComparableAudioFile} that
     *         facilitates perceptual comparison of each audio file in the
     *         given list
     */
    public static List<ComparableAudioFile> makeListOfComparableAudioFile(
            AudioFile[] listOfFiles) {
        int duration;
        List<ComparableAudioFile> asl = new ArrayList<ComparableAudioFile>();
        for (AudioFile af : listOfFiles) {
            duration = af.getDurationInSeconds();
            // If the duration of the audio file is less than minimum duration
            // needed for a match, the file is skipped
            if (duration < FRAGMENT_SIZE_TO_MATCH_IN_SECONDS) {
                continue;
            }
            ComparableAudioFile as = make(af);
            as.setFileName(af.getShortName());
            asl.add(as);
        }
        return asl;
    }

    /**
     * static factory method to make new {@ComparableAudioFile
     * 
     * } instances, the implementation chosen is dependent
     * on the mode instance variable
     * 
     * @param audioFile - an {@AudioFile} to be analyzed
     * @return {@ComparableAudioFile} that facilitates
     *         perceptual comparison of the given audio file
     */
    public static ComparableAudioFile make(AudioFile audioFile) {
        if (MODES.FAST == mode) {
            return new ComparableAudioFileImplForFastMatch(audioFile);
        } else {
            return new ComparableAudioFileImpl(audioFile);
        }
    }

    /**
     * This implementation is used for representing audio samples in way that
     * facilitates perceptual comparison of segments that are 5 seconds or
     * longer, the implementation contains logic to find matching segments with
     * enhanced accuracy making it slightly more computationally intensive
     * 
     */
    private static class ComparableAudioFileImpl extends ComparableAudioFile {

        private static int half_sample_frame_size = SAMPLES_PER_FRAME / 2;
        private static int three_quarter_sample_frame_size = SAMPLES_PER_FRAME
                + half_sample_frame_size;
        private static int error_threshold = 8;
        private static double error_density = 4.3;
        private static int frame_count_for_5_seconds = 140;
        private static double offset_in_seconds = ((double) SAMPLES_PER_FRAME)
                / (2 * 44100.0);

        // configures the ComparableAudioFile class, so that it can be used for
        // a
        // given FFT size and a given length of analysis frame
        static {
            ComparableAudioFile.initialize(FFT_WINDOW_SIZE, SAMPLES_PER_FRAME,
                    SAMPLES_PER_FRAME, error_density, error_threshold,
                    frame_count_for_5_seconds, offset_in_seconds);
        }

        private AudioFile audioFile;
        private Map<Integer, List<Integer>> fingerprint =
                new HashMap<Integer, List<Integer>>();
        private double[] overlap;
        private int counter = 0;

        // Constructor
        private ComparableAudioFileImpl(AudioFile audioFile) {
            this.audioFile = audioFile;
            computeFingerprint();
        }

        private void computeFingerprint() {
            int streamingLength = SAMPLES_PER_FRAME * 32;
            while (audioFile.hasNext()) {
                computeFingerprintForStreamedChunk(audioFile
                        .getNext(streamingLength));
            }
            audioFile.close();
        }

        /**
         * Computes and updates the acoustic fingerprint for a segment of
         * streaming audio after applying the Hanning window function to the
         * individual analysis frames. The method computes FFT for these
         * analysis frames with 50% overlap, and updates the fingerprint of
         * each of these frames to the main fingerprint corresponding to the
         * source audio file encapsulated by this instance
         * 
         * @param data - A segment of audio samples belonging to this instance
         *            for which the fingerprint is to be updated
         * 
         */
        private void computeFingerprintForStreamedChunk(double[] data) {
            double[] input = new double[FFT_WINDOW_SIZE];

            // appends the overlapping component from the previous segment to
            // the beginning of the current segment
            if (overlap != null) {
                double[] newdata = new double[data.length + SAMPLES_PER_FRAME];
                System.arraycopy(overlap, 0, newdata, 0, SAMPLES_PER_FRAME);
                System.arraycopy(data, 0, newdata, SAMPLES_PER_FRAME,
                        data.length);
                data = newdata;
            }
            int slen = data.length - three_quarter_sample_frame_size;
            for (int i = 0; i < slen;) {
                applyHannWindow(data, input, i);
                AcousticAnalyzer.updateFingerprintUsingAverageDeltaPowerDiff(
                        performFFT(input), counter++, fingerprint);
                applyHannWindow(data, input, i + half_sample_frame_size);
                AcousticAnalyzer.updateFingerprintUsingAverageDeltaPowerDiff(
                        performFFT(input), counter++, fingerprint);
                i = i + SAMPLES_PER_FRAME;
            }
            // to retain the overlapping component for the next segment
            overlap =
                    Arrays.copyOfRange(data, data.length - SAMPLES_PER_FRAME,
                            data.length);
        }

        /**
         * Getter to get the fingerprint of audio file encapsulated by this
         * instance
         */
        @Override
        public Map<Integer, List<Integer>> getFingerprint() {
            return this.fingerprint;
        }

    }

    /**
     * This implementation is used for representing audio samples in way that
     * facilitates perceptual comparison of segments that are 5 seconds or
     * longer, the implementation contains logic to find matching segments in a
     * quicker time with potentially less accurate results
     * 
     */
    private static class ComparableAudioFileImplForFastMatch extends
            ComparableAudioFile {

        private static int error_threshold = 8;
        private static double error_density = 8;
        private static int frame_count_for_5_seconds = 70;
        private static double offset_in_seconds =
                ((double) SAMPLES_PER_FRAME) / 44100.0;

        // configures the ComparableAudioFile class, so that it can be used for
        // a given FFT size and a given length of analysis frame
        static {
            ComparableAudioFile.initialize(FFT_WINDOW_SIZE, SAMPLES_PER_FRAME,
                    SAMPLES_PER_FRAME, error_density, error_threshold,
                    frame_count_for_5_seconds, offset_in_seconds);
        }

        private AudioFile audioFile;
        private Map<Integer, List<Integer>> fingerprint =
                new HashMap<Integer, List<Integer>>();
        private int counter = 0;

        // Constructor
        private ComparableAudioFileImplForFastMatch(AudioFile audioFile) {
            this.audioFile = audioFile;
            computeFingerprint();
        }

        // helper method that updates the fingerprint for the encapsulated
        // audio file on the fly by breaking it down to streaming chunks
        private void computeFingerprint() {
            int streamingLength = SAMPLES_PER_FRAME * 32;
            while (audioFile.hasNext()) {
                computeFingerprintForStreamedChunk(audioFile
                        .getNext(streamingLength));
            }
            audioFile.close();
        }

        /**
         * Computes and updates the acoustic fingerprint for a segment of
         * streamed audio after applying the Hanning window function to the
         * individual analysis frames. The method computes FFT for these
         * analysis frames with no overlap, and updates the fingerprint of each
         * of these frames to the main fingerprint corresponding to the source
         * audio file encapsulated by this instance
         * 
         * @param data - A segment of audio samples belonging to this instance
         *            for which the fingerprint is to be updated
         * 
         */
        private void computeFingerprintForStreamedChunk(double[] data) {
            double[] input = new double[FFT_WINDOW_SIZE];
            int slen = data.length;
            int ignore = slen % SAMPLES_PER_FRAME;
            slen = slen - ignore;
            for (int i = 0; i < slen;) {
                applyHannWindow(data, input, i);
                AcousticAnalyzer.updateFingerprintUsingAverageDeltaPowerDiff(
                        performFFT(input), counter++, fingerprint);
                i = i + SAMPLES_PER_FRAME;
            }

        }

        /**
         * Getter to get the fingerprint of audio file encapsulated by this
         * instance
         */
        @Override
        public Map<Integer, List<Integer>> getFingerprint() {
            return this.fingerprint;
        }

    }

}
