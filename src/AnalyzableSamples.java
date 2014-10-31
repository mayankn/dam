import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * 
 * @author Magesh
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
    double[] preFactors;
    private String fileName;
    private double[] fingerprint;
    private int slen;

    protected AnalyzableSamples(double[] samples, int fftsize) {
        slen = samples.length;
        int bufferedlen = slen + (fftsize - slen % fftsize);
        this.samples = new double[bufferedlen];
        this.fftResult = new double[bufferedlen << 1];
        System.arraycopy(samples, 0, this.samples, 0, slen);
        this.fftsize = fftsize;
        // Long st = System.currentTimeMillis();
        FFTPreComputor.initialize(fftsize);
        bitReverseArray = FFTPreComputor.getBitReverseIndex();
        preFactors = FFTPreComputor.getPrecomputedFactors();
        exp2Map = FFTPreComputor.getExpMap();
        log2Map = FFTPreComputor.getLogMap();
        // Long et = System.currentTimeMillis();
        // System.out.println("ini time: " + (et - st));
        performFFT();
        computeFingerprint();
    }
    
    public int getSampleLength() {
    	return slen;
    }
    
    private void computeFingerprint() {
    	fingerprint = AcousticAnalyzer.extractRmsBasedFingerprint(fftResult,
                fftsize);
    	//to free memory    	
    	exp2Map = null;
    	bitReverseArray = null;    	
    	samples = null;
    	fftResult = null;
    	preFactors = null;
    }
    
    public double[] getFingerprint() {
    	return this.fingerprint;
    }

    public void setFileName(String fname) {
        this.fileName = fname;
    }
    
    public String getFileName(){
        return this.fileName;
    }
    /**
     * To check if two AnalyzableSamples are a match(perceptually)
     * 
     * @param aS2
     * @return
     */
    public abstract boolean isMatch(AnalyzableSamples aS2);

    /**
     * 
     * @return
     */
    public double[] getSamples() {
    	if(samples == null) {
    		throw new IllegalStateException("Error: sample data no longer exists");
    	}
        return this.samples;
    }

    /**
     * 
     * @return
     */
    public double[] getFFTResult() {
    	if(fftResult == null) {
    		throw new IllegalStateException("Error: FFT result data no longer exists");
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

    private double[] bitReverseArray(double[] input) {
        double[] brArr = new double[input.length << 1];
        for (int i = 0; i < input.length; i++) {
            brArr[bitReverseArray[i]] = input[i];
        }
        return brArr;
    }

    /**
     * Non recursive FFT - Translated by Magesh, Mayank, Naren from Pseudocode
     * in Introduction to Algorithms - Third Edition
     * 
     * @param samples
     *            -> samples[i][0] - real, samples[i][1] - imaginary
     * @return double[][] : transform -> transform[i][0] - real, transform[i][1]
     *         - imaginary
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
}