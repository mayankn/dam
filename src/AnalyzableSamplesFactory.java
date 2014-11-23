import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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

    private static int FRAGMENT_SIZE_TO_MATCH_IN_SECONDS = 5;
    private static final int SAMPLES_PER_FRAME = 1764;
    private static final int ERROR_THRESHOLD = 5,
            FFT_WINDOW_SIZE = 2048,
            FRAME_COUNT_FOR_5_SECONDS = 225,
            ERROR_DENSITY = 9,
            MIN_HASH_COLLISIONS_FOR_MATCH = ERROR_THRESHOLD
                    + (int) ((double) FRAME_COUNT_FOR_5_SECONDS / ERROR_DENSITY)
                    + 2;

    private static double OFFSET_IN_SECONDS = ((double) SAMPLES_PER_FRAME)
            / (2 * 44100.0);

    // configures the AnalyzableSamples class, so that it can be use for a given
    // FFT size and a given length of analysis frame
    static {
        AnalyzableSamples.initialize(FFT_WINDOW_SIZE, SAMPLES_PER_FRAME);
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
        return new AnalyzableSamplesForFragmentMatching(audioFile);
    }

    /**
     * This implementation is used for representing audio samples in way that
     * facilitates perceptual comparison of segments that are 5 seconds or
     * longer
     * 
     */
    private static class AnalyzableSamplesForFragmentMatching extends
            AnalyzableSamples {

        private AnalyzableSamplesForFragmentMatching(AudioFile audioFile) {
            super(audioFile);
        }

        /**
         * Compares the audio file encapsulated by this instance with the given
         * {@AnalyzableSamples} input to check if there are
         * any matching segments of at least 5 seconds.
         * 
         * @param aS2 - {@AnalyzableSamples} representing an
         *            audio file which is to be compared with this instance.
         * 
         * @return - If there is a match, returns an array of two elements with
         *         each element representing the time at which the match was
         *         found. Otherwise, returns a null value.
         * 
         */
        @Override
        public double[] getMatchPositionInSeconds(AnalyzableSamples aS2) {
            return computeFragmentMatchWithTime(this.getFingerprint(),
                    aS2.getFingerprint());
        }

        /**
         * 
         * @param fp1 - HashMap representing a fingerprint
         * @param fp2 - HashMap representing another fingerprint
         * @return - If there is a match, returns an array of two elements with
         *         each element representing the time at which the match was
         *         found. Otherwise, returns a null value.
         */
        private double[] computeFragmentMatchWithTime(
                Map<Integer, List<Integer>> fp1,
                Map<Integer, List<Integer>> fp2) {
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
            sindex2 = extractSequenceStartIndexForMatch(s2);
            if (sindex2 == -1) {
                return null;
            }

            sindex1 = extractSequenceStartIndexForMatch(s);
            if (sindex1 == -1) {
                return null;
            }

            return new double[] { (OFFSET_IN_SECONDS * sindex1),
                    (OFFSET_IN_SECONDS * sindex2) };
        }

        /**
         * To identify the presence of a sequence of values corresponding to a 5
         * second or longer intervals along with the time of occurrence of such
         * sequence if any.
         * 
         * Algorithm: the technique looks for a continuity in the given sequence
         * of numbers so that the relative time difference between start and end
         * values of the sequence is equal to are greater than 5 seconds. If
         * there are many such discontinuous sequences, keeps track of the
         * longest such sequence so far.
         * <p>
         * 1) sorts the values in the input set in ascending order
         * <p>
         * 2) traverses the list from an anchor point (starts with the first
         * element), if the difference between consecutive values is 1, the no
         * error is accumulated, otherwise the difference is added to errors
         * <p>
         * 3) step 2 is repeated as long as the error remains below the
         * threshold.
         * <p>
         * 4) if the error exceeds the threshold, the anchor point is moved to
         * the next element in the sequence and the error corresponding to the
         * particular element is removed from the accumulated errors
         * <p>
         * 5) step 4 is repeated till the error value drops below the threshold.
         * <p>
         * 6) steps 2-5 are repeated till the program reaches the end of
         * sequence.
         * <p>
         * 7) between steps 2-6, the algorithm keeps track of difference between
         * the element corresponding to the current index and the anchor point.
         * If the difference is greater that the value corresponding to 5
         * seconds and if the sequence count is greater than any such sequence
         * encountered so far, remembers the start index (anchor point)
         * corresponding to the sequence. At the end if no such sequence is
         * found , returns a -1. otherwise returns the value corresponding to
         * the anchor point of the sequence
         * 
         * 
         * @param s - set of values representing time offsets from the beginning
         * @return - starting value of a sequence that corresponds to a 5 second
         *         match. Returns -1 if no such sequence is found
         */
        private int extractSequenceStartIndexForMatch(Set<Integer> s) {
            int size = s.size();
            int errors = 0, sofar = 0, seq = 0, prevseq = 0, rindex = -1;
            Integer[] sequence = new Integer[size];
            sequence = s.toArray(sequence);
            Arrays.sort(sequence);
            // System.out.println(Arrays.asList(sequence));
            if (sequence.length <= MIN_HASH_COLLISIONS_FOR_MATCH) {
                return -1;
            }
            int cleanupidx = 0;
            int[] errarr = new int[size];
            int[] diffarr = new int[size];
            int diff = 0;
            for (int i = 0; i < sequence.length - 1;) {
                if (errors >= (ERROR_THRESHOLD + (ERROR_DENSITY * seq))) {
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
                i++;
            }
            if (rindex == -1)
                return -1;
            else
                return sequence[rindex];
        }

    }
}
