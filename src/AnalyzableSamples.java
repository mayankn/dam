import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 
 * @author: Magesh
 * @author: Mayank Narashiman
 * @author: Narendran K.P
 * 
 *          <br/>Description: This class is to represent audio sample data in a
 *          format that facilitates perceptual comparison with data from other
 *          audio files
 * 
 */
public abstract class AnalyzableSamples {
    private static Map<Integer, Integer> log2Map =
            new HashMap<Integer, Integer>(17);
    private static int samples_per_frame;
    private static int HALF_SAMPLE_FRAME_SIZE;
    private static int THREE_QUARTER_SAMPLE_FRAME_SIZE;
    private static int[] bitReverseArray;
    private static double[] preFactors;
    private static double[] hannWindow;
    private static int fftsize;
    private static int[] exp2Map;
    private static boolean isInitialized = false;

    /**
     *
     * @param size - FFT window size
     * @param framesize - size of the frame
     * Description - init method used to initialize various computation
     *                  parameters
     */
    public static void initialize(int size, int framesize) {
        samples_per_frame = framesize;
        HALF_SAMPLE_FRAME_SIZE = samples_per_frame / 2;
        THREE_QUARTER_SAMPLE_FRAME_SIZE =
                samples_per_frame + HALF_SAMPLE_FRAME_SIZE;
        fftsize = size;
        Precomputor.initialize(fftsize);
        bitReverseArray = Precomputor.getBitReverseIndex();
        preFactors = Precomputor.getPrecomputedFactors();
        exp2Map = Precomputor.getExpMap();
        log2Map = Precomputor.getLogMap();
        hannWindow = Precomputor.getHannWindow();
        isInitialized = true;
    }

    private double[] samples;
    private String fileName;
    private Map<Integer, List<Integer>> fingerprint =
            new HashMap<Integer, List<Integer>>();
    private int slen, bitRate;

    protected AnalyzableSamples(double[] samples) {
        if (!isInitialized) {
            throw new RuntimeException(
                    "ERROR: the class must be initialized before use");
        }
        this.samples = samples;
        computeFingerprint();
        freeMemory();
    }

    public int getSampleLength() {
        return slen;
    }

    private void freeMemory() {
        // to free memory
        samples = null;
    }

    public Map<Integer, List<Integer>> getFingerprint() {
        return this.fingerprint;
    }

    public void setFileName(String fname) {
        this.fileName = fname;
    }

    public String getFileName() {
        return this.fileName;
    }

    /**
     * To check if two AnalyzableSamples are a match(perceptually) *
     * @param aS2 - {@AnalyzableSamples}
     * @return
     */
    public abstract boolean isMatch(AnalyzableSamples aS2);

    /**
     * Returns the offset in seconds of the beginning of the matching segment
     * within the first file, along with the offset in seconds of the beginning
     * of the matching segment within the second file. If there is no match,
     * returns a null
     * @param aS2
     * @return
     */
    public abstract double[] getMatchPositionInSeconds(AnalyzableSamples aS2);

    /**
     * Description - Computes the audio fingerprint based on the chosen the
     * fingerprinting mechanism after applying the hanning window function to
     * the input audio in the frequency domain
     */
    private void computeFingerprint() {
        int slen = samples.length - THREE_QUARTER_SAMPLE_FRAME_SIZE;
        int counter = 0;
        double[] input = new double[fftsize];
        for (int i = 0; i < slen;) {
            applyHannWindow(input, i);
            AcousticAnalyzer.updateFingerprintForGivenSamples(
                    performFFT(input), counter++, fingerprint);
            applyHannWindow(input, i + HALF_SAMPLE_FRAME_SIZE);
            AcousticAnalyzer.updateFingerprintForGivenSamples(
                    performFFT(input), counter++, fingerprint);
            i = i + THREE_QUARTER_SAMPLE_FRAME_SIZE;
        }
    }

    /**
     *
     * @param input - audio samples corresponding to window size of the FFT
     * @param start
     * Description: applies the hanning window function
     */
    private void applyHannWindow(double[] input, int start) {
        int end = start + samples_per_frame + 1;
        for (int i = start, j = 0; i < end; i++, j++) {
            input[j] = samples[i] * hannWindow[j];
        }
        for (int i = samples_per_frame; i < fftsize; i++) {
            input[i] = 0;
        }
    }

    /**
     * Applies the hanning window function over each sample and constructs a bit
     * reversed array
     * @param input the audio samples present in window size amount of data
     * @return
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
     * Non recursive FFT - Translated by Magesh, Mayank, Naren from Pseudocode
     * in Introduction to Algorithms - Third Edition
     * 
     * @param samples - samples[i] -> real component, samples[i + fftsize] ->
     *            imaginary component
     * @return double[] : ft[i] -> real component, ft[i + fftsize] -> imaginary
     *         component
     * 
     */

    private double[] performFFT(double[] samples) {
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

    public int getBitRate() {
        return bitRate;
    }

    public void setBitRate(int bitRate) {
        this.bitRate = bitRate;
    }

}
