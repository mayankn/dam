import java.util.HashMap;
import java.util.Map;

/**
 * @author: Magesh Ramachandran
 * @author: Mayank Narashiman
 * @author: Narendran K.P
 * Description: This class efficiently pre-computes all the redundant and
 * repetitive
 * computations needed by the FFT algorithm on each sample corresponding to
 * the size of the FFT window
 */
public class Precomputor {
    private static final int PS = 17;
    private static final double twoPi = 2 * Math.PI;
    private static double[] preFactors;
    private static int[] bitReverseArray;
    private static double[] tfactor;
    private static int size;
    private static int[] exp2Map = new int[17];
    private static int frameSize;
    private static double[] hannWindow;
    private static Map<Integer, Integer> log2Map =
            new HashMap<Integer, Integer>(17);

    static {
        tfactor = new double[PS << 1];
        intMaps(1, 0);
        intMaps(2, 1);
        intMaps(4, 2);
        intMaps(8, 3);
        intMaps(16, 4);
        intMaps(32, 5);
        intMaps(64, 6);
        intMaps(128, 7);
        intMaps(256, 8);
        intMaps(512, 9);
        intMaps(1024, 10);
        intMaps(2048, 11);
        intMaps(4096, 12);
        /*
         * intMaps(8192, 13); intMaps(16384, 14); intMaps(32768, 15);
         * intMaps(65536, 16);
         */
    }

    static void intMaps(int key, int val) {
        log2Map.put(key, val);
        exp2Map[val] = key;
        double c = twoPi / key;
        tfactor[val] = Math.cos(c);
        tfactor[val + PS] = Math.sin(c);
    }

    static void initialize(int fftsize, int fsize) {
        if (fftsize != size) {
            size = fftsize;
            frameSize = fsize;
            preComputeFFTFactors();
            precomputeHanningWindow();
            constructBitReverseIndexArray();
        }
    }

    /**
     * Description - pre-computes the values of the hanning window function
     * for a given sample corresponding to the FFT size
     */
    private static void precomputeHanningWindow() {
        double windowFactor = twoPi / (frameSize - 1);
        hannWindow = new double[frameSize];
        for (int n = 0; n < frameSize; n++) {
            hannWindow[n] = 0.5 * (1 - Math.cos(windowFactor * n));
        }
    }

    private static void preComputeFFTFactors() {
        try {
            int depth = log2Map.get(size);
            int ti = (size * depth);
            preFactors = new double[ti];
            int htlen = ti >> 1;
            int i = 0;
            int ht = tfactor.length >> 1;
            double wr, wi, w0r, w0i, w0rt, w0it;
            for (int s = 1; s <= depth; s++) {
                int m = exp2Map[s];
                int halfm = m >> 1;
                int jmax = halfm;
                wr = tfactor[s];
                wi = tfactor[s + ht];
                for (int k = 0; k < size; k = k + m) {
                    w0r = 1;
                    w0i = 0;
                    for (int j = 0; j < jmax; j++) {
                        preFactors[i] = w0r;
                        preFactors[i + htlen] = w0i;
                        w0rt = w0r;
                        w0it = w0i;
                        w0r = (w0rt * wr - w0it * wi);
                        w0i = (w0rt * wi + w0it * wr);
                        i++;
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Description - Constructs a bit reversed array of the size of FFT window
     */
    private static void constructBitReverseIndexArray() {
        int expVal = log2Map.get(size);
        bitReverseArray = new int[size];
        for (int i = 0; i < size; i++) {
            bitReverseArray[i] =
                    Integer.reverse(Integer.rotateRight(i, expVal));
        }
    }

    public static double[] getPrecomputedFactors() {
        return preFactors;
    }

    public static int[] getBitReverseIndex() {
        return bitReverseArray;
    }

    public static int[] getExpMap() {
        return exp2Map;
    }

    public static Map<Integer, Integer> getLogMap() {
        return log2Map;
    }

    public static double[] getHannWindow() {
        return hannWindow;
    }

}
