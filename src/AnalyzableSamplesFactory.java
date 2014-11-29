import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 
 * Factory class with method(s) to create specific instances of
 * {@AnalyzableSamples}
 * 
 * @author: Magesh Ramachandran
 * @author: Mayank Narashiman
 * @author: Narendran K.P
 * 
 */
public class AnalyzableSamplesFactory {

    public enum MODES {
        FAST, NORMAL
    }

    private static final int SAMPLES_PER_FRAME = 1764;
    private static final int FFT_WINDOW_SIZE = 2048;
    private static final int FRAGMENT_SIZE_TO_MATCH_IN_SECONDS = 5;

    private static MODES mode;

    public static void setMode(MODES m) {
        mode = m;
    }

    /**
     * This method takes a list of {@AudioFile} and returns a list
     * of {@AnalyzableSamples} that encapsulate the given
     * files in order to facilitate perceptual comparison.
     * 
     * @param listOfFiles - a list of {@AudioFile} to be analyzed
     * 
     * @return - list of {@AnalyzableSamples} that
     *         facilitates perceptual comparison of each audio file in the given
     *         list
     */
    public static List<AnalyzableSamples> makeListOfAnalyzableSamples(
            AudioFile[] listOfFiles) {
        int duration;
        List<AnalyzableSamples> asl = new ArrayList<AnalyzableSamples>();
        for (AudioFile af : listOfFiles) {
            duration = af.getDurationInSeconds();
            if (duration < FRAGMENT_SIZE_TO_MATCH_IN_SECONDS) {
                continue;
            }
            AnalyzableSamples as = make(af);
            as.setFileName(af.getShortName());
            asl.add(as);
        }
        return asl;
    }

    /**
     * static factory method to make new {@AnalyzableSamles}
     * instances
     * 
     * @param audioFile - an {@AudioFile} to be analyzed
     * @return {@AnalyzableSamples} that facilitates
     *         perceptual comparison of the given audio file
     */
    public static AnalyzableSamples make(AudioFile audioFile) {
        if (MODES.FAST == mode) {
            return new AnalyzableSamplesImplForFastMatch(audioFile);
        } else {
            return new AnalyzableSamplesImpl(audioFile);
        }
    }

    /**
     * This implementation is used for representing audio samples in way that
     * facilitates perceptual comparison of segments that are 5 seconds or
     * longer
     * 
     */
    private static class AnalyzableSamplesImpl extends AnalyzableSamples {

        private static int half_sample_frame_size = SAMPLES_PER_FRAME / 2;
        private static int error_threshold = 5;
        private static int three_quarter_sample_frame_size = SAMPLES_PER_FRAME
                + half_sample_frame_size;
        private static double error_density = 9;
        private static int frame_count_for_5_seconds = 220;
        private static double offset_in_seconds = ((double) SAMPLES_PER_FRAME)
                / (2 * 44100.0);

        // configures the AnalyzableSamples class, so that it can be use for a
        // given FFT size and a given length of analysis frame
        static {
            Precomputor.initialize(FFT_WINDOW_SIZE, SAMPLES_PER_FRAME);
            AnalyzableSamples.initialize(FFT_WINDOW_SIZE, SAMPLES_PER_FRAME,
                    error_density, error_threshold, frame_count_for_5_seconds,
                    offset_in_seconds);
        }

        private AudioFile audioFile;
        private Map<Integer, List<Integer>> fingerprint =
                new HashMap<Integer, List<Integer>>();
        private double[] overlap;
        private int counter = 0;

        private AnalyzableSamplesImpl(AudioFile audioFile) {
            this.audioFile = audioFile;
            computeFingerprint();
        }

        private void computeFingerprint() {
            int streamingLength = SAMPLES_PER_FRAME * 8;
            while (audioFile.hasNext()) {
                computeFingerprintForStreamedChunk(audioFile
                        .getNext(streamingLength));
            }
            audioFile.close();
            // System.out.println("counter"+counter);
        }

        /**
         * Computes and updates the acoustic fingerprint for a segment of
         * streaming audio after applying the Hanning window function to the
         * individual analysis frames. The method computes FFT for these
         * analysis frames with 50% overlap, and updates the fingerprint of each
         * of these frames to the main fingerprint corresponding to the source
         * audio file encapsulated by this instance
         * 
         * @param data - A segment of audio samples belonging to this instance
         *            for which the fingerprint is to be updated
         * 
         */
        private void computeFingerprintForStreamedChunk(double[] data) {
            double[] input = new double[FFT_WINDOW_SIZE];

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
                AcousticAnalyzer
                        .updateFingerprintForGivenSamplesUsingAverageDeltaPowerDiff(
                                performFFT(input), counter++, fingerprint);
                applyHannWindow(data, input, i + half_sample_frame_size);
                AcousticAnalyzer
                        .updateFingerprintForGivenSamplesUsingAverageDeltaPowerDiff(
                                performFFT(input), counter++, fingerprint);
                i = i + SAMPLES_PER_FRAME;
            }
            overlap =
                    Arrays.copyOfRange(data, data.length - SAMPLES_PER_FRAME,
                            data.length);
        }

        @Override
        public Map<Integer, List<Integer>> getFingerprint() {
            return this.fingerprint;
        }
        
    }

    /**
     * This implementation is used for representing audio samples in way that
     * facilitates perceptual comparison of segments that are 5 seconds or
     * longer
     * 
     */
    private static class AnalyzableSamplesImplForFastMatch extends
            AnalyzableSamples {

        private static int error_threshold = 5;
        private static double error_density = 18;
        private static int frame_count_for_5_seconds = 100;
        private static double offset_in_seconds =
                ((double) SAMPLES_PER_FRAME) / 44100.0;

        // configures the AnalyzableSamples class, so that it can be use for a
        // given FFT size and a given length of analysis frame
        static {
            Precomputor.initialize(FFT_WINDOW_SIZE, FFT_WINDOW_SIZE);
            AnalyzableSamples.initialize(FFT_WINDOW_SIZE, SAMPLES_PER_FRAME,
                    error_density, error_threshold, frame_count_for_5_seconds,
                    offset_in_seconds);
        }

        private AudioFile audioFile;
        private Map<Integer, List<Integer>> fingerprint =
                new HashMap<Integer, List<Integer>>();
        private int counter = 0;

        private AnalyzableSamplesImplForFastMatch(AudioFile audioFile) {
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
                AcousticAnalyzer
                        .updateFingerprintForGivenSamplesUsingAverageDeltaPowerDiff(
                                performFFT(input), counter++, fingerprint);
                i = i + SAMPLES_PER_FRAME;
            }

        }

        @Override
        public Map<Integer, List<Integer>> getFingerprint() {
            return this.fingerprint;
        }
    
    }

}
