import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * This Class contains static methods to extract audio finger print for every
 * frame of the given frequency domain audio data
 * 
 * @author: Magesh Ramachandran
 * @author: Mayank Narashiman
 * @author: Narendran K.P
 * 
 */
public class AcousticAnalyzer {

    private static final int[] BARK_SCALE = new int[] { /* 5, 7, 9, 12, */14,
            16, 19, 21, 24, 26, 29, 33, 36, 39, 43, 46, 50, 54, 59, 64, 69, 74,
            80, 86, 93, 100, 108, 116, 125, 134, 146, 157, 171, 185, 186 };

    /**
     * 
     * This method computes the fingerprint for the given segment of audio by
     * calculating a hash based on the frequency components corresponding to the
     * signal with the peak amplitude for several predefined frequency ranges
     * and stores the same in the given hash map
     * 
     * @param audioSegment - an array representing the segment of audio for
     *            which the fingerprint has to be computed
     * 
     * @param sttime - a number representing the relative time of occurrence of
     *            the given audio segment
     * 
     * @param fingerprint - the hash map of the main sequence processed so far
     *            into which the new fingerprint computed will be stored
     * 
     * 
     */
    public static void updateFingerprintForGivenSamples1(
            double[] audioSegment,
            int sttime,
            Map<Integer, List<Integer>> fingerprint) {
        double absValue;
        int frameSize = audioSegment.length;
        int halfFrameSize = frameSize / 2;
        int hash = 0, range = 10, i = 0, maxrange = 123;
        double pmax = 0;
        byte[] fmax = new byte[4];
        byte fpmax = 0;
        // TODO: replace with better range if possible - was 1, 120
        for (int fband = 2; fband < maxrange;) {
            if (fband < range) {
                absValue =
                        Math.pow(audioSegment[fband], 2)
                                + Math.pow(audioSegment[fband + halfFrameSize],
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

    /**
     * This method computes the fingerprint for the given segment of audio by
     * computing the delta of the average power in the frequency sub-bands
     * defined by the Bark Scale and stores the same in the given hash map
     * 
     * @param audioSegment - An array representing the segment of audio for
     *            which the fingerprint has to be computed
     * 
     * @param sttime - A number representing the relative time of occurrence of
     *            the given audio segment
     * 
     * @param fingerprint - hash map of the main sequence processed so far into
     *            which the new fingerprint computed will be stored
     * 
     * 
     */
    public static void updateFingerprintForGivenSamples(
            double[] audioSegment,
            int sttime,
            Map<Integer, List<Integer>> fingerprint) {
        double absValue;
        int frameSize = audioSegment.length;
        int halfFrameSize = frameSize / 2;
        double[] bandPower = new double[33];
        int hash = 0, range = BARK_SCALE[0], i = 0, maxrange = 126;
        int counter = 0;
        double fpow = 0;
        // TODO: check
        for (int fband = 12; fband < maxrange;) {
            if (fband < range) {
                absValue =
                        Math.sqrt(Math.pow(audioSegment[fband], 2)
                                + Math.pow(audioSegment[fband + halfFrameSize],
                                        2));
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

    /**
     * Computes a hash code by combining the bits of all the input bytes
     * 
     * @return - Hash computed from the by combining the bits in all of the
     *         given input bytes.
     * 
     */
    private static int bitwiseHash(byte fp1, byte fp2, byte fp3, byte fp4) {
        int hash = (fp1 & 0xFF) | fp2 << 8 | fp3 << 16 | fp4 << 24;
        return hash;
    }

    /**
     * Computes the hash code by calculating the delta between each array
     * elements of the given input array
     * 
     * @return - Hash computed by calculating the delta between each array
     *         elements of the given input array
     * 
     * 
     */
    private static int bitwiseHash(double[] input) {
        int hash = Integer.MAX_VALUE;
        for (int i = 1; i < input.length; i++) {
            if (input[i - 1] < input[i]) {
                hash = (hash & ~(1 << i));
            }
        }
        return hash;
    }

}
