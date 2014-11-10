import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author: Magesh Ramachandran
 * @author: Mayank Narashiman
 * @author: Narendran K.P
 * 
 *          </br> Description: This Class contains static methods to extract
 *          audio finger print for every frame of the given frequency domain
 *          audio data.
 * 
 */
public class AcousticAnalyzer {
    /**
     * 
     * @param audioSamples - Complex number representing amplitude and phase in
     *            frequency domain, where audioSamples[i] represents a real
     *            component and audioSamples[i + frameSize] represents the
     *            corresponding imaginary component.
     * @param frameSize - The frame size in terms of number of samples for which
     *            the finger print has to be extracted.
     * @return
     */
    public static double[] extractRmsBasedFingerprint(
            double[] audioSamples,
            int frameSize) {
        frameSize = frameSize << 1;
        int samplesLength = audioSamples.length;
        int halfFrameSize = (frameSize >> 1);
        int quarterFrameSize = (halfFrameSize >> 1);
        int threeQuarterFrameSize = halfFrameSize + quarterFrameSize;
        int i = 0;
        double sum = 0;
        double[] rmsValues = new double[samplesLength / frameSize];
        double[] absValues = new double[samplesLength];
        for (int j = 0; j < samplesLength; j++) {
            absValues[j] =
                    Math.pow(audioSamples[j], 2)
                            + Math.pow(audioSamples[j + halfFrameSize], 2);
            sum += absValues[j];

            if (j % quarterFrameSize == 0 && j != 0) {
                rmsValues[i++] =
                        computeRms(
                                sum / quarterFrameSize,
                                Arrays.copyOfRange(absValues, j
                                        - quarterFrameSize, j));
                sum = 0;
                j = j + threeQuarterFrameSize;
            }
        }
        return rmsValues;
    }

    /**
     * 
     * @param avg : Average value used for Normalization
     * @param absValues : Absolute value representing amplitude of a sample
     * @return
     */
    private static double computeRms(double avg, double[] absValues) {
        double temp = 0;
        int len = absValues.length;
        for (int i = 0; i < len; i++) {
            temp += Math.pow(absValues[i] / avg, 2);
        }
        return Math.sqrt(temp / absValues.length);
    }

    public static Map<Integer, List<Integer>> extractFrequencyBasedFingerprint(
            double[] audioSamples,
            int frameSize) {
        frameSize = frameSize << 1;
        int samplesLength = audioSamples.length;
        int halfFrameSize = (frameSize >> 1);
        int quarterFrameSize = (halfFrameSize >> 1);
        int threeQuarterFrameSize = halfFrameSize + quarterFrameSize;
        // double[] absValues = new double[samplesLength];
        Map<Integer, List<Integer>> fingerprint =
                new HashMap<Integer, List<Integer>>();
        double absValue;
        int f1 = 0, f2 = 0, f3 = 0;
        double h1 = 0, h2 = 0, h3 = 0;
        int freqcounter = 0, freqkey = 0;
        List<Integer> a = new ArrayList<Integer>();
        int time = 0;
        for (int j = 0; j < samplesLength; j++) {
            absValue =
                    Math.pow(audioSamples[j], 2)
                            + Math.pow(audioSamples[j + halfFrameSize], 2);
            if(freqcounter!=512 && (freqcounter<1 || freqcounter>500)) {

                freqcounter++;
                continue;
            }            
           
            if (h1 < absValue) {
                h3 = h2;
                h2 = h1;
                h1 = absValue;
                f3 = f2;
                f2 = f1;
                f1 = freqcounter;
            } else if (h2 < absValue) {
                h3 = h2;
                h2 = absValue;
                f3 = f2;
                f2 = freqcounter;
            } else if (h3 < absValue) {
                h3 = absValue;
                f3 = freqcounter;
            }

            if (j % quarterFrameSize == 0 && j != 0) {
                // System.out.println("f1" + f1);
                // System.out.println("f2" + f2);
                freqkey = hash1(f1, f2, f3);
                a = fingerprint.get(freqkey);
                if (a == null) {
                    a = new ArrayList<Integer>();
                    a.add(time);
                    fingerprint.put(freqkey, a);
                } else {
                    a.add(time);
                }                
                time++;
                freqcounter = 0;
                j = j + threeQuarterFrameSize;
                f1 = 0;
                f2 = 0;
                f3 = 0;
                h1 = 0;
                h2 = 0;
                h3 = 0;

            }
            freqcounter++;
        }
        return fingerprint;
    }

    private static int hash1(int f1, int f2, int f3) {
        return (f1 * 100000) + (f2 * 1000) + f3;
    }

    private static int hash2(int f1, int f2, int f3) {
        int fuzzFactor = 2;
        return ((f1 - (f1 % fuzzFactor)) * 100000) + ((f2 - (f2 % fuzzFactor))
                * 1000) + (f3 - (f3 % fuzzFactor));
    }

}
