import java.util.ArrayList;
import java.util.Arrays;
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

    private static final int[] BARK_SCALE = new int[] { 5, 7, 9, 12, 14, 16,
            19, 21, 24, 26, 29, 33, 36, 39, 43, 46, 50, 54, 59, 64, 69, 74, 80,
            86, 93, 100, 108, 116, 125, 126 };

    public static void updateFingerprintForGivenSamples1(
            double[] audioSamples,
            int sttime,
            Map<Integer, List<Integer>> fingerprint) {
        double absValue;
        int frameSize = audioSamples.length;
        int halfFrameSize = frameSize / 2;
        int hash = 0, range = 10, i = 0, maxrange = 123;
        double pmax = 0;
        byte[] fmax = new byte[4];
        byte fpmax = 0;
        // TODO: replace with better range if possible - was 1, 120
        for (int fband = 2; fband < maxrange;) {
            if (fband < range) {
                absValue =
                        Math.pow(audioSamples[fband], 2)
                                + Math.pow(audioSamples[fband + halfFrameSize],
                                        2);
                if (absValue > pmax) {
                    pmax = absValue;
                    fpmax = (byte) fband;
                }
                fband++;
            } else {
                fmax[i++] = fpmax;
                pmax = 0;
                fpmax = 0;
                switch (range) {
                case 10:
                    range = 20;
                    break;
                case 20:
                    range = 60;
                    break;
                case 60:
                    range = maxrange - 1;
                    break;
                default:
                    fband = maxrange;
                }
            }
        }
        hash = bitwiseHash(fmax[0], fmax[1], fmax[2], fmax[3]);
        List<Integer> times = fingerprint.get(hash);
        if (times == null) {
            times = new ArrayList<Integer>();
            times.add(sttime);
            fingerprint.put(hash, times);
        } else {
            times.add(sttime);
        }
        sttime++;
    }

    public static void updateFingerprintForGivenSamples(
            double[] audioSamples, int sttime, Map<Integer,
            List<Integer>> fingerprint) {
        double absValue;
        int frameSize = audioSamples.length;
        int halfFrameSize = frameSize / 2;
        double[] bandPower = new double[33];
        int hash = 0, range = BARK_SCALE[0], i = 0, maxrange = 120;
        int counter = 0;
        double fpow = 0;
        // TODO: check
        for (int fband = 13; fband < maxrange;) {
            if (fband < range) {
                absValue = Math.sqrt(Math.pow(audioSamples[fband], 2)
                        + Math.pow(audioSamples[fband + halfFrameSize], 2));
                fpow = fpow + absValue;
                fband++;
                counter++;
            } else {
                bandPower[i] = fpow / counter;
                counter = 0;
                fpow = 0;
                i++;
                range = BARK_SCALE[i];
            }
        }
        hash = bitwiseHash(bandPower);
        List<Integer> times = fingerprint.get(hash);
        if (times == null) {
            times = new ArrayList<Integer>();
            times.add(sttime);
            fingerprint.put(hash, times);
        } else {
            times.add(sttime);
        }
        sttime++;
    }

    private static int bitwiseHash(byte fp1, byte fp2, byte fp3, byte fp4) {
        int hash = (fp1 & 0xFF) | fp2 << 8 | fp3 << 16 | fp4 << 24;
        return hash;
    }

    private static int bitwiseHash(double[] bandPower) {
        int hash = Integer.MAX_VALUE;
        for (int i = 1; i < bandPower.length; i++) {
            if (bandPower[i - 1] < bandPower[i]) {
                hash = (hash & ~(1 << i));
            }
        }
        return hash;
    }

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

}
