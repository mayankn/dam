import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author: Magesh Ramachandran
 * @author: Mayank Narashiman
 * @author: Narendran K.P Description:
 * 
 */
public class AnalyzableSamplesFactory {
    private static final int FFT_WINDOW_SIZE = 1024;
    private static final int OFFSET = 215;
    private static double OFFSET_IN_SECONDS = (1024.0 / 44100.0);

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

        // private static final int DISTANCE_THRESHOLD = 200;
        // private static final int UPPER_DISTANCE_THRESHOLD = 2100;
        private int fftsize;

        private AnalyzableSamplesForFragmentMatching(double[] samples,
                int fftsize) {
            super(samples, fftsize);
            this.fftsize = fftsize;
        }

        /**
         * {@inheritDoc}
         */
        @Deprecated        
        @Override
        public boolean isMatch(AnalyzableSamples aS2) {
            // if (this.getFingerprint() == null
            // || aS2.getFingerprint() == null
            // || aS2.getFingerprint().length != aS2.getFingerprint().length)
            // throw new RuntimeException("ERROR: Samples not comparable!");
            // int distanceThreshold = DISTANCE_THRESHOLD;
            // if ((this.getBitRate() == 8) ^ (aS2.getBitRate() == 8)) {
            // distanceThreshold = UPPER_DISTANCE_THRESHOLD;
            // }
            int distanceThreshold = 200;
            if (distance(this.getFingerprint(), aS2.getFingerprint()) <= distanceThreshold) {
                return true;
            }
            return false;
        }

        @Override
        public int[] getMatchPositionInSeconds(AnalyzableSamples aS2) {
            return computeFragmentMatchWithTime(this.getFingerprint(),
                    aS2.getFingerprint());
        }

        private int[] computeFragmentMatchWithTime(
                Map<Integer, List<Integer>> fp1,
                Map<Integer, List<Integer>> fp2) {
            Set<Integer> s = new HashSet<Integer>();
            Set<Integer> s2 = new HashSet<Integer>();
            for (int k : fp1.keySet()) {
                List<Integer> t1 = fp1.get(k);
                List<Integer> t2 = fp2.get(k);
                if (t2 == null) {
                    continue;
                }
                s.addAll(t1);
                s2.addAll(t2);
            }
            int sindex1 = -1, sindex2 = -1;
            sindex1 = extractSequenceStartIndexForMatch(s);
            sindex2 = extractSequenceStartIndexForMatch(s2);
            if (sindex1 == -1 || sindex2 == -1)
                return null;
            else
                return new int[] { (int) (OFFSET_IN_SECONDS * sindex1),
                        (int) (OFFSET_IN_SECONDS * sindex2) };
        }

        private int extractSequenceStartIndexForMatch(Set<Integer> s) {
            Integer[] sequence = new Integer[s.size()];
            sequence = s.toArray(sequence);
            Arrays.sort(sequence);

            int errors = 0, seq = 0, sindex = -1;
            int start = sequence[0];
            int starti = 0;
            int seqcount = 1;
            int m = 210;
            int errordensity = 5;
            for (int i = 0; i < sequence.length - 1; i++) {
                if (errors > (errordensity * seqcount)) {
                    sindex = -1;
                    seq = 0;
                    errors = 0;
                    start = sequence[i];
                    i = starti;
                    starti++;
                    seqcount = 0;
                }
                if (seq >= m) {
                    sindex = start;
                    break;
                }

                if (!(sequence[i + 1] - sequence[i] == 1)) {
                    errors = errors + (sequence[i + 1] - sequence[i]);
                }
                seq = sequence[i + 1] - start;
                seqcount++;
            }
            return sindex;
        }

        @Deprecated
        private int distance(
                Map<Integer, List<Integer>> fp1,
                Map<Integer, List<Integer>> fp2) {
            int dm1 = 0, dm2 = 0;
            for (int k : fp1.keySet()) {
                List<Integer> fpl2 = fp2.get(k);
                List<Integer> fpl1 = fp1.get(k);
                dm1 = dm1 + computeMismatch(fpl1, fpl2);
                dm2 = dm2 + computeMismatch(fpl2, fpl1);
            }
            int dist = (dm1 >= dm2) ? dm1 : dm2;
            System.out.println("dist" + dist);
            return dist;
        }

        @Deprecated
        private int computeMismatch(List<Integer> fpl1, List<Integer> fpl2) {
            int degreeOfMismatch = 0;
            if (fpl2 == null) {
                degreeOfMismatch = degreeOfMismatch + fpl1.size();
            } else if (fpl1 == null) {
                degreeOfMismatch = degreeOfMismatch + fpl2.size();
            } else {
                for (int j : fpl1) {
                    if (fpl2.contains(j)) {

                    } else {
                        degreeOfMismatch++;
                    }
                }
            }
            return degreeOfMismatch;
        }

        @Deprecated
        private int distance(double[] fp1, double[] fp2) {
            int dist = 0;
            for (int i = 0; i < fp1.length; i++) {
                dist += (int) Math.abs(fp1[i] - fp2[i]);
            }
            dist = dist * 1000 / (getSampleLength() / fftsize);
            return dist;
        }

        @Deprecated
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
                    if ((int) distForFragment <= 10) {
                        offsetforf1 = (int) Math.ceil(OFFSET_IN_SECONDS * i);
                        offsetforf2 = (int) Math.ceil(OFFSET_IN_SECONDS * j);
                        return new int[] { offsetforf1, offsetforf2 };
                    }
                    distForFragment = 0;
                }
            }
            return null;
        }
    }
}
