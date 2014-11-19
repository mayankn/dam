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

    private static final int SAMPLES_PER_FRAME = 1764;
    private static final int ERROR_THRESHOLD = 5,
            FFT_WINDOW_SIZE = 2048,
            FRAME_COUNT_FOR_5_SECONDS = 163,
            ERROR_DENSITY = 10,
            MIN_HASH_COLLISIONS_FOR_MATCH = ERROR_THRESHOLD
                    + (int) ((double) FRAME_COUNT_FOR_5_SECONDS / ERROR_DENSITY)
                    + 2;

    private static double OFFSET_IN_SECONDS =
            ((double) SAMPLES_PER_FRAME * 3.0) / (4.0 * 44100.0);

    static {
        AnalyzableSamples.initialize(FFT_WINDOW_SIZE, SAMPLES_PER_FRAME);
    }

    /**
     * 
     * @param data - Array containing audio sample data
     * @return {@AnalyzableSamples}
     */
    public static AnalyzableSamples make(double[] data) {
        validateInputData(data);
        return new AnalyzableSamplesForFragmentMatching(data);
    }

    private static void validateInputData(double[] isamples) {
        if (isamples == null) {
            throw new IllegalArgumentException();
        }
        if (isamples.length < FFT_WINDOW_SIZE) {
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

        private int fftsize;

        private AnalyzableSamplesForFragmentMatching(double[] samples) {
            super(samples);
            this.fftsize = FFT_WINDOW_SIZE;
        }

        @Override
        public double[] getMatchPositionInSeconds(AnalyzableSamples aS2) {
            int errScaling = 1;
            if (this.getBitRate() == 8 ^ aS2.getBitRate() == 8) {
                errScaling = 5;
            }
            return computeFragmentMatchWithTime(this.getFingerprint(),
                    aS2.getFingerprint(), errScaling);
        }

        private double[] computeFragmentMatchWithTime(
                Map<Integer, List<Integer>> fp1,
                Map<Integer, List<Integer>> fp2,
                int errScaling) {
            Set<Integer> s = new HashSet<Integer>(), s2 =
                    new HashSet<Integer>();
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
            sindex2 = extractSequenceStartIndexForMatch(s2, errScaling);
            if (sindex2 == -1) {
                return null;
            }

            sindex1 = extractSequenceStartIndexForMatch(s, errScaling);
            if (sindex1 == -1) {
                return null;
            }

            return new double[] { (OFFSET_IN_SECONDS * sindex1),
                    (OFFSET_IN_SECONDS * sindex2) };
        }

        private int extractSequenceStartIndexForMatch(
                Set<Integer> s,
                int errScaling) {
            int size = s.size();
            int errors = 0, sofar = 0, seq = 0, prevseq = 0, rindex = -1;
            Integer[] sequence = new Integer[size];
            sequence = s.toArray(sequence);
            Arrays.sort(sequence);
            // System.out.println(Arrays.asList(sequence));
            if (sequence.length <=  (MIN_HASH_COLLISIONS_FOR_MATCH /
                    errScaling)) {
                return -1;
            }
            int cleanupidx = 0;
            int[] errarr = new int[size];
            int[] diffarr = new int[size];
            int diff = 0;
            for (int i = 0; i < sequence.length - 1; i++) {
                if (errors >= (ERROR_THRESHOLD +  (ERROR_DENSITY * errScaling
                        * seq))) {
                    i = i - 1;
                    sofar = sofar - diffarr[cleanupidx];
                    errors = errors - errarr[cleanupidx];
                    cleanupidx++;
                    seq = seq - 1;
                    continue;
                } else if (sofar > FRAME_COUNT_FOR_5_SECONDS) {
                    if (seq > prevseq) {
                        prevseq = seq;
                        rindex = Math.max(i - seq, 0);
                    }
                }
                diff = sequence[i + 1] - sequence[i];
                diffarr[i] = diff;
                if (diff > 1) {
                    errors = errors + diff;
                    errarr[i] = diff;
                }
                sofar = sofar + diff;
                seq++;
            }
            if (rindex == -1)
                return -1;
            else
                return sequence[rindex];
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
            int effLen1 = fp1.length - FRAME_COUNT_FOR_5_SECONDS;
            int effLen2 = fp2.length - FRAME_COUNT_FOR_5_SECONDS;
            int offsetforf1, offsetforf2;
            double distForFragment = 0;
            for (int i = 0; i <= effLen1; i++) {
                for (int j = 0; j <= effLen2; j++) {
                    for (int k = 0; k < FRAME_COUNT_FOR_5_SECONDS; k++) {
                        distForFragment =
                                distForFragment
                                        + (Math.abs(fp1[i + k] - fp2[j + k]));
                    }
                    distForFragment =
                            distForFragment * 100 / FRAME_COUNT_FOR_5_SECONDS;
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

        /**
         * {@inheritDoc}
         */
        @Deprecated
        @Override
        public boolean isMatch(AnalyzableSamples aS2) {
            int distanceThreshold = 200;
            if (distance(this.getFingerprint(),  aS2.getFingerprint()) <=
                    distanceThreshold) {
                return true;
            }
            return false;
        }

    }
}
