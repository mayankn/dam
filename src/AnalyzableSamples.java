import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This class is to represent audio sample data in a format that facilitates
 * perceptual comparison with data from other audio files
 * 
 * @author: Magesh
 * @author: Mayank Narashiman
 * @author: Narendran K.P
 * 
 * 
 */
public abstract class AnalyzableSamples {
    private static final String ERROR_UNINIIALIZED_CLASS =
            "ERROR: This class must be initialized before use";

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
    private AudioFile audioFile;
    private double[] overlap;
    private int counter = 0;

    private String fileName;
    private Map<Integer, List<Integer>> fingerprint =
            new HashMap<Integer, List<Integer>>();
    private int slen, bitRate;

    /**
     * Initializes the pre-computed factors based on the size of FFT window and
     * the framesize that represents the aggregation of audio samples that are
     * analyzed together as a 'frame'
     * 
     * @param size - FFT window size
     * @param framesize -The number of samples analyzed together as a frame
     */
    public static void initialize(int size, int framesize) {
        samples_per_frame = framesize;
        HALF_SAMPLE_FRAME_SIZE = samples_per_frame / 2;
        THREE_QUARTER_SAMPLE_FRAME_SIZE =
                samples_per_frame + HALF_SAMPLE_FRAME_SIZE;
        fftsize = size;
        Precomputor.initialize(fftsize, samples_per_frame);
        bitReverseArray = Precomputor.getBitReverseIndex();
        preFactors = Precomputor.getPrecomputedFactors();
        exp2Map = Precomputor.getExpMap();
        log2Map = Precomputor.getLogMap();
        hannWindow = Precomputor.getHannWindow();
        isInitialized = true;
    }

    protected AnalyzableSamples(AudioFile aFile) {
        if (!isInitialized) {
            throw new RuntimeException(ERROR_UNINIIALIZED_CLASS);
        }
        audioFile = aFile;
        int streamingLength = samples_per_frame * 800;
        while (audioFile.hasNext()) {
            computeFingerprint(audioFile.getNext(streamingLength));
            // computeFingerprintWithoutOverlap(audioFile.getNext(streamingLength));
        }
        audioFile.close();
    }

    public int getSampleLength() {
        return slen;
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
     * Returns the offset in seconds of the beginning of the matching segment
     * within the first file, along with the offset in seconds of the beginning
     * of the matching segment within the second file. If there is no match,
     * returns a null
     * 
     * @param aS2 - {@AnalyzableSamples} object which
     *            encapsulates the audio file to be compared with
     * @return - a double[2], where,
     */
    public abstract double[] getMatchPositionInSeconds(AnalyzableSamples aS2);

    /**
     * Computes and updates the acoustic fingerprint for a segment of streaming
     * audio after applying the Hanning window function to the individual
     * analysis frames. The method computes FFT for these analysis frames with
     * 50% overlap, and updates the fingerprint of each of these frames to the
     * main fingerprint corresponding to the source audio file encapsulated by
     * this instance
     * 
     * @param data - A segment of audio samples belonging to this instance for
     *            which the fingerprint is to be updated
     * 
     */
    private void computeFingerprint(double[] data) {
        double[] input = new double[fftsize];

        if (overlap != null) {
            double[] newdata = new double[data.length + samples_per_frame];
            System.arraycopy(overlap, 0, newdata, 0, samples_per_frame);
            System.arraycopy(data, 0, newdata, samples_per_frame, data.length);
            data = newdata;
        }
        int slen = data.length - THREE_QUARTER_SAMPLE_FRAME_SIZE;
        for (int i = 0; i < slen;) {
            applyHannWindow(data, input, i);
            AcousticAnalyzer.updateFingerprintForGivenSamples(
                    performFFT(input), counter++, fingerprint);
            applyHannWindow(data, input, i + HALF_SAMPLE_FRAME_SIZE);
            AcousticAnalyzer.updateFingerprintForGivenSamples(
                    performFFT(input), counter++, fingerprint);
            i = i + samples_per_frame;
        }
        overlap =
                Arrays.copyOfRange(data, data.length - samples_per_frame,
                        data.length);
    }

    /**
     * Computes and updates the acoustic fingerprint for a segment of streaming
     * audio after applying the Hanning window function to the individual
     * analysis frames. The method computes FFT for these analysis frames
     * without overlap and updates the fingerprint of each of these frames to
     * the main fingerprint corresponding of the source audio file encapsulated
     * by this instance
     * 
     * @param data - A segment of audio samples belonging to this instance for
     *            which the fingerprint is to be updated
     * 
     */
    private void computeFingerprintWithoutOverlap(double[] data) {
        double[] input = new double[fftsize];

        int slen = data.length;
        for (int i = 0; i < slen - samples_per_frame;) {
            applyHannWindow(data, input, i);
            AcousticAnalyzer.updateFingerprintForGivenSamples(
                    performFFT(input), counter++, fingerprint);
            i = i + samples_per_frame;
        }
    }

    /**
     * Applies the Hanning window function to the samples chosen by start index
     * and returns the input array after updating it with the computed value
     * 
     * @param input - A double array in which the 'frame' corresponding to the
     *            given start index will be stored
     * @param start - The start index of the current 'frame'
     * 
     */
    private void applyHannWindow(double[] data, double[] input, int start) {
        int end = start + samples_per_frame;
        for (int i = start, j = 0; i < end; i++, j++) {
            input[j] = data[i] * hannWindow[j];
        }
        for (int i = samples_per_frame; i < fftsize; i++) {
            input[i] = 0;
        }
    }

    /**
     * This is a helper method used to prepare the bit reversed array needed by
     * the non-recursive FFT implementation
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
     * Non recursive FFT - Translated by Magesh, Mayank, Naren from Pseudocode
     * in Introduction to Algorithms - Third Edition
     * <p>
     * Enhancements: As this method is invoked repeatedly for a fixed size
     * input, all the common computations are precomputed and stored in arrays.
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

}
