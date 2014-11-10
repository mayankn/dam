import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 
 * @author: Magesh
 * @author: Mayank Narashiman
 * @author: Narendran K.P
 * 
 *          </br>Description: This class is to represent audio sample data in a
 *          format that facilitates perceptual comparison with data from other
 *          audio files
 * 
 */
public abstract class AnalyzableSamples {
    private static Map<Integer, Integer> log2Map =
            new HashMap<Integer, Integer>(17);
    private int[] exp2Map;
    private int[] bitReverseArray;
    private int fftsize;
    private double[] samples;
    private double[] fftResult;
    private double[] hannWindow;
    double[] preFactors;
    private String fileName;
    private Map<Integer, List<Integer>> fingerprint;
    private int slen;
    private int bitRate;

    protected AnalyzableSamples(double[] samples, int fftsize) {
        slen = samples.length;
        int bufferedlen = slen + (fftsize - slen % fftsize);
        this.samples = new double[bufferedlen];
        this.fftResult = new double[bufferedlen << 1];
        System.arraycopy(samples, 0, this.samples, 0, slen);
        this.fftsize = fftsize;
        FFTPreComputor.initialize(fftsize);
        bitReverseArray = FFTPreComputor.getBitReverseIndex();
        preFactors = FFTPreComputor.getPrecomputedFactors();
        exp2Map = FFTPreComputor.getExpMap();
        log2Map = FFTPreComputor.getLogMap();
        hannWindow = FFTPreComputor.getHannWindow();
        performFFT();
        computeFingerprint();        
    }

    public int getSampleLength() {
        return slen;
    }

    private void computeFingerprint() {
        fingerprint =
                AcousticAnalyzer.extractFrequencyBasedFingerprint(fftResult, fftsize);
        // to free memory
        exp2Map = null;
        bitReverseArray = null;
        samples = null;
        fftResult = null;
        preFactors = null;
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
     * To check if two AnalyzableSamples are a match(perceptually)     * 
     * @param aS2 - {@AnalyzableSamples} 
     * @return
     */
    public abstract boolean isMatch(AnalyzableSamples aS2);
    
    /**
     * Returns  the offset in seconds of the beginning of the matching segment
     * within the first file, along with the
     * offset in seconds of the beginning of the matching segment
     * within the second file. If there is no match, returns a null
     * @param aS2
     * @return
     */
    public abstract double[] getMatchPositionInSeconds(AnalyzableSamples aS2);
    

    /**
     * 
     * @return
     */
    public double[] getSamples() {
        if (samples == null) {
            throw new IllegalStateException(
                    "Error: sample data no longer exists");
        }
        return this.samples;
    }

    /**
     * 
     * @return
     */
    public double[] getFFTResult() {
        if (fftResult == null) {
            throw new IllegalStateException(
                    "Error: FFT result data no longer exists");
        }
        return this.fftResult;
    }

    private void performFFT() {
        int slen = samples.length;
        int resultsize = fftsize << 1;
        for (int i = 0, nexti = 0; i < slen;) {
            nexti = i + fftsize;
            System.arraycopy(performFFT(Arrays.copyOfRange(samples, i, nexti)),
                    0, fftResult, i << 1, resultsize);
            i = nexti;
        }
    }

    /**
     * Applies the hanning window function over each sample and constructs a
     * bit reversed array
     * @param input the audio samples present in window size amount of data
     * @return
     */
    private double[] bitReverseArray(double[] input) {
        int inputSize = input.length;
        double[] brArr = new double[inputSize << 1];
        for (int i = 0; i < inputSize; i++) {
            brArr[bitReverseArray[i]] = (input[i] * hannWindow[i]);
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
