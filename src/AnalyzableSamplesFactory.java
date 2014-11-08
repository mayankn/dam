/**
 * @author: Magesh Ramachandran
 * @author: Mayank Narashiman
 * @author: Narendran K.P Description:
 * 
 */
public class AnalyzableSamplesFactory {
    private static final int FFT_WINDOW_SIZE = 1024;
    private static final int OFFSET = 215;    
    private static double OFFSET_IN_SECONDS = (1024.0/44100.0);

    /**
     * 
     * @param isamples - Array containing audio sample data
     * @return {@AnalyzableSamples}
     */
    public static AnalyzableSamples make(double[] isamples) {
        int fftsize = FFT_WINDOW_SIZE;
        validateInputData(isamples, fftsize);
        AnalyzableSamples aS =
                new AnalyzableSamplesForFragmentMatching(isamples, fftsize);
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
    private static class AnalyzableSamplesForFragmentMatching extends
            AnalyzableSamples {

        private static final int DISTANCE_THRESHOLD = 200;
        private static final int UPPER_DISTANCE_THRESHOLD = 2100;
        private int fftsize;

        private AnalyzableSamplesForFragmentMatching(double[] samples,
                int fftsize) {
            super(samples, fftsize);
            this.fftsize = fftsize;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean isMatch(AnalyzableSamples aS2) {
            if (this.getFingerprint() == null
                    || aS2.getFingerprint() == null
                    || aS2.getFingerprint().length != aS2.getFingerprint().length)
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

        private int[] computeFragmentMatchWithTime(double[] fp1, double[] fp2) {
            int effLen1 = fp1.length - OFFSET;
            int effLen2 = fp2.length - OFFSET;
            int offsetforf1, offsetforf2;
            double distForFragment = 0;
            for (int i = 0; i <= effLen1; i++) {
                for (int j = 0; j <= effLen2; j++) {
                    for (int k = 0; k < OFFSET; k++) {
                        distForFragment =
                                distForFragment
                                        + (Math.abs(fp1[i + k] - fp2[j + k]));
                    }
                    distForFragment = distForFragment * 100 / OFFSET;
                    if ((int)distForFragment <= 10) {
                        offsetforf1 = (int)Math.ceil(OFFSET_IN_SECONDS * i);
                        offsetforf2 = (int)Math.ceil(OFFSET_IN_SECONDS * j);
                        return new int[] { offsetforf1, offsetforf2 };
                    }
                    distForFragment = 0;
                }
            }
            return null;
        }

        @Override
        public int[] getMatchPositionInSeconds(AnalyzableSamples aS2) {
            return computeFragmentMatchWithTime(this.getFingerprint(),
                    aS2.getFingerprint());
        }
    }
}
