/**
 * @author: Magesh Ramachandran
 * @author: Mayank Narashiman
 * @author: Narendran K.P Description:
 * 
 */
public class AnalyzableSamplesFactory {
    private static final int FFT_WINDOW_SIZE = 1024;

    /**
     * 
     * @param isamples - Array containing audio sample data      
     * @return {@AnalyzableSamples}
     */
    public static AnalyzableSamples make(double[] isamples) {
        int fftsize = FFT_WINDOW_SIZE;
        validateInputData(isamples, fftsize);
        AnalyzableSamples aS =
                new AnalyzableSamplesForEqualDurationAudio(isamples, fftsize);
        return aS;
    }

    private static void validateInputData(double[] isamples, int fftsize) {
        if (isamples == null) {
            throw new IllegalArgumentException();
        }
        if (isamples.length < fftsize) {
            throw new RuntimeException(
                    "ERROR: Insufficient samples, cannot proceed");
        }
    }

    /**
     * Description - This implementation is used for representing and comparing
     * equal duration audio samples in way that facilitates perceptual
     * comparison
     * 
     */
    private static class AnalyzableSamplesForEqualDurationAudio extends
            AnalyzableSamples {

        private static final int DISTANCE_THRESHOLD = 200;
        private static final int UPPER_DISTANCE_THRESHOLD = 2100;
        private int fftsize;

        private AnalyzableSamplesForEqualDurationAudio(double[] samples,
                int fftsize) {
            super(samples, fftsize);
            this.fftsize = fftsize;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean isMatch(AnalyzableSamples aS2) {
            if (this.getFingerprint() == null || aS2.getFingerprint() == null
                    || (this.getFingerprint().length !=
                    aS2.getFingerprint().length))
                throw new RuntimeException("ERROR: Samples not comparable!");
            int distanceThreshold = DISTANCE_THRESHOLD;
            if ((this.getBitRate() == 8) ^ (aS2.getBitRate() == 8)) {
                distanceThreshold = UPPER_DISTANCE_THRESHOLD;
            }
            if (distance(this.getFingerprint(), aS2.getFingerprint()) < distanceThreshold) {
                return true;
            }
            return false;
        }

        private int distance(double[] fp1, double[] fp2) {
            int dist = 0;
            for (int i = 0; i < fp1.length; i++) {
                dist += (int) Math.abs(fp1[i] - fp2[i]);
            }
            dist = dist * 1000 / (getSampleLength() / fftsize);
            return dist;
        }
    }
}
