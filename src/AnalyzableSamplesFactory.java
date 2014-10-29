/**
 * @author: Magesh Ramachandran
 * @author: Mayank Narashiman
 * @author: Narendran K.P Description:
 */
public class AnalyzableSamplesFactory {
    private static final int FFT_WINDOW_SIZE = 1024;

    /**
     * @param isamples
     * @param fftsize
     * @return
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

    private static class AnalyzableSamplesForEqualDurationAudio extends
            AnalyzableSamples {

        private static final int DISTANCE_THRESHOLD = 100;
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
            if (getSamples() == null
                    || getSamples().length != aS2.getSamples().length)
                throw new RuntimeException("ERROR: Samples not comparable!");

            double[] fpForS1 =
                    AcousticAnalyzer.extractRmsBasedFingerprint(getFFTResult(),
                            fftsize);
            double[] fpForS2 =
                    AcousticAnalyzer.extractRmsBasedFingerprint(
                            aS2.getFFTResult(), fftsize);
            if (distance(fpForS1, fpForS2) < DISTANCE_THRESHOLD) {
                return true;
            }
            return false;
        }

        private int distance(double[] fp1, double[] fp2) {
            int dist = 0;
            for (int i = 0; i < fp1.length; i++) {
                dist += (int) Math.abs(fp1[i] - fp2[i]);
            }
            dist = dist * 1000 / (getSamples().length / fftsize);
            System.out.println("dist" + dist);
            return dist;
        }
    }
}
