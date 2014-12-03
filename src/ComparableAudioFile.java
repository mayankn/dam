import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * This class is to represent audio sample data in a format that facilitates
 * perceptual comparison with data from other audio files
 * 
 * @author: Magesh
 * @author: Mayank Narashiman
 * @author: Narendran K.P
 * 
 */
public abstract class ComparableAudioFile {
    private static final String ERROR_UNINIIALIZED_CLASS =
            "ERROR: This class must be initialized before use";

    private static Map<Integer, Integer> log2Map =
            new HashMap<Integer, Integer>(17);
    private static int samples_per_frame;
    private static int[] bitReverseArray;
    private static double[] preFactors;
    private static double[] hannWindow;
    private static int fftsize;
    private static int[] exp2Map;
    private static boolean isInitialized = false;
    private static int error_threshold, frame_count_for_5_seconds,
            min_hash_collisions_for_match;
    private static double offset_in_seconds, error_density;

    private String fileName;

    private int bitRate;

    /**
     * 
     * Gets the pre-computed factors for the given size of FFT window and the
     * framesize which represents the aggregation of audio samples that are
     * analyzed together as a 'frame'
     * 
     * @param size - FFT window size
     * @param framesize -The number of samples analyzed together as a frame
     * @param hanningWindowSize - Size for the Hanning Window.
     * @param errorDensity - the error density that will be used by the matching
     *            algorithm
     * @param errThreshold - the initial error threshold that will be used by
     *            the matching algorithm
     * @param frameCountForMatch - number of sequential frames needed for a
     *            match
     * @param offsetInSeconds - fraction of time represented by a frame
     */
    public static void initialize(
            int size,
            int framesize,
            int hanningWindowSize,
            double errorDensity,
            int errThreshold,
            int frameCountForMatch,
            double offsetInSeconds) {

        // Initializes the Precomputer
        Precomputor.initialize(size, hanningWindowSize);

        samples_per_frame = framesize;
        error_threshold = errThreshold;
        frame_count_for_5_seconds = frameCountForMatch;
        error_density = errorDensity;
        min_hash_collisions_for_match =
                error_threshold
                        + (int) ((double) frame_count_for_5_seconds / error_density)
                        + 2;
        offset_in_seconds = offsetInSeconds;
        fftsize = size;
        bitReverseArray = Precomputor.getBitReverseIndex();
        preFactors = Precomputor.getPrecomputedFactors();
        exp2Map = Precomputor.getExpMap();
        log2Map = Precomputor.getLogMap();
        hannWindow = Precomputor.getHannWindow();
        isInitialized = true;
    }

    // constructor
    protected ComparableAudioFile() {
        if (!isInitialized) {
            throw new RuntimeException(ERROR_UNINIIALIZED_CLASS);
        }
    }

    /**
     * Applies the Hanning window function to the samples chosen by start index
     * and returns the input array after updating it with the computed value.
     * The method also contains the logic for applying zero padding
     * 
     * @param input - A double array in which the 'frame' corresponding to the
     *            given start index will be stored
     * @param start - The start index of the current 'frame'
     * 
     */
    protected void applyHannWindow(double[] data, double[] input, int start) {
        int end = start + samples_per_frame;
        for (int i = start, j = 0; i < end; i++, j++) {
            input[j] = data[i] * hannWindow[j];
        }
        for (int i = samples_per_frame; i < fftsize; i++) {
            input[i] = 0;
        }
    }

    /**
     * Non recursive FFT - Translated by Magesh, Mayank, Naren from Pseudocode
     * in Introduction to Algorithms - Third Edition
     * <p>
     * Enhancements: As this method is invoked repeatedly for a fixed size
     * input, all the common computations are retrieved from a cache.
     * 
     * @param samples - samples[i] -> real component, samples[i + fftsize] ->
     *            imaginary component, where 0< i < fftsize
     * @return double[] : ft[j] -> real component, ft[j + fftsize] -> imaginary
     *         component, where 0< j < fftsize
     * 
     */
    protected double[] performFFT(double[] samples) {
        int size = samples.length;
        int depth = log2Map.get(samples.length);
        int hsize = size;
        int htsize = preFactors.length >> 1;
        double[] brArr = bitReverseArray(samples);
        int ri = 0, kj, kji, kjm, kjmi;
        double tr, ti, ur, ui, pfr, pfi, wr, wi;
        for (int s = 1; s <= depth; s++) {
            int m = exp2Map[s];
            int halfm = m >> 1;
            for (int k = 0; k < size; k = k + m) {
                for (int j = 0; j < halfm; j++) {
                    kj = k + j;
                    kji = kj + hsize;
                    kjm = kj + halfm;
                    kjmi = kjm + hsize;
                    pfr = preFactors[ri];
                    pfi = preFactors[ri + htsize];
                    wr = brArr[kjm];
                    wi = brArr[kjmi];
                    ur = brArr[kj];
                    ui = brArr[kji];
                    tr = (wr * pfr) - (wi * pfi);
                    ti = (wr * pfi) + (wi * pfr);
                    brArr[kj] = tr + ur;
                    brArr[kji] = ti + ui;
                    brArr[kjm] = ur - tr;
                    brArr[kjmi] = ui - ti;
                    ri++;
                }
            }
        }
        return brArr;
    }

    /**
     * This is a helper method used to prepare the bit reversed array needed by
     * the non-recursive FFT implementation. It returns an array of twice the
     * length, where the second half of the array represents the imaginary
     * components. It makes use of the pre-computed bitReverseArray
     * 
     * @param input - A double array of size 'fftsize'
     * @return - A double array of twice the length as input with values store
     *         in bit reversed order
     */
    private double[] bitReverseArray(double[] input) {
        int inputSize = input.length;
        double[] brArr = new double[inputSize << 1];
        for (int i = 0; i < inputSize; i++) {
            brArr[bitReverseArray[i]] = input[i];
        }
        return brArr;
    }

    /**
     * Returns the offset in seconds of the beginning of the matching segment
     * within the first file, along with the offset in seconds of the beginning
     * of the matching segment within the second file. If there is no match,
     * returns a null
     * 
     * @param aS2 - {@ComparableAudioFile} object which
     *            encapsulates the audio file to be compared with
     * @return - a double[2], where, result[0] and result[1] corresponds to the
     *         times at which the match(if any) has occurred
     */
    public double[] getMatchPositionInSeconds(ComparableAudioFile aS2) {
        return computeFragmentMatchWithTime(this.getFingerprint(),
                aS2.getFingerprint());
    }

    /**
     * This method extracts two sets of time sequences (one for each
     * fingerprint) containing the time instances at which there were hash
     * collisions. It then identifies if there is a match and the time at which
     * the match has occurred by identifying the presence of sequences in the
     * two sets
     * @param fp1 - HashMap representing a fingerprint
     * @param fp2 - HashMap representing another fingerprint
     * @return - If there is a match, returns an array of two elements with each
     *         element representing the time at which the match was found.
     *         Otherwise, returns a null value.
     */
    protected double[] computeFragmentMatchWithTime(
            Map<Integer, List<Integer>> fp1,
            Map<Integer, List<Integer>> fp2) {
        Set<Integer> s = new HashSet<Integer>(), s2 = new HashSet<Integer>();
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

        return new double[] { (offset_in_seconds * sindex1),
                (offset_in_seconds * sindex2) };
    }

    /**
     * To identify the presence of a sequence of values corresponding to a 5
     * second or longer intervals along with the time of occurrence of such
     * sequence if any.
     * 
     * Algorithm: the technique looks for a continuity in the given sequence of
     * numbers so that the relative time difference between start and end values
     * of the sequence is equal to are greater than 5 seconds. If there are many
     * such discontinuous sequences, keeps track of the longest such sequence so
     * far.
     * <p>
     * 1) sorts the values in the input set in ascending order
     * <p>
     * 2) traverses the list from an anchor point (starts with the first
     * element), if the difference between consecutive values is 1, the no error
     * is accumulated, otherwise the difference is added to errors
     * <p>
     * 3) step 2 is repeated as long as the error remains below the threshold.
     * <p>
     * 4) if the error exceeds the threshold, the anchor point is moved to the
     * next element in the sequence and the error corresponding to the
     * particular element is removed from the accumulated errors
     * <p>
     * 5) step 4 is repeated till the error value drops below the threshold.
     * <p>
     * 6) steps 2-5 are repeated till the program reaches the end of sequence.
     * <p>
     * 7) between steps 2-6, the algorithm keeps track of difference between the
     * element corresponding to the current index and the anchor point. If the
     * difference is greater that the value corresponding to 5 seconds and if
     * the sequence count is greater than any such sequence encountered so far,
     * remembers the start index (anchor point) corresponding to the sequence.
     * At the end if no such sequence is found , returns a -1. otherwise returns
     * the value corresponding to the anchor point of the sequence
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
        // if the number of values in the sequence is less than the min values
        // needed for match, exit early
        if (sequence.length <= min_hash_collisions_for_match) {
            return -1;
        }
        int cleanupidx = 0;
        int[] errarr = new int[size];
        int[] diffarr = new int[size];
        int diff = 0;
        for (int i = 0; i < sequence.length - 1;) {
            if (errors >= (error_threshold + (error_density * seq))) {
                if (cleanupidx == sequence.length)
                    break;
                sofar = sofar - diffarr[cleanupidx];
                errors = errors - errarr[cleanupidx];
                cleanupidx++;
                seq = seq - 1;
                continue;
            } else if (sofar > frame_count_for_5_seconds) {
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

    /**
     * Returns the bit rate of the audio file encapsulated by this instance
     * 
     * @return bitRate
     */
    public int getBitRate() {
        return bitRate;
    }

    /**
     * Setter for the bit rate of the audio file encapsulated by this instance
     * 
     * @param bitRate
     */
    public void setBitRate(int bitRate) {
        this.bitRate = bitRate;
    }

    /**
     * 
     * @return - the fingerprint corresponding to this instance
     */
    public abstract Map<Integer, List<Integer>> getFingerprint();

    /**
     * 
     * @param fname - file name
     */
    public void setFileName(String fname) {
        this.fileName = fname;
    }

    /**
     * 
     * @return - file name
     */
    public String getFileName() {
        return this.fileName;
    }

}
