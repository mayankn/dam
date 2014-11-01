import java.util.Arrays;

/**
 * @author: Magesh Ramachandran
 * @author: Mayank Narashiman
 * @author: Narendran K.P
 * Description: This Class contains static methods to extract audio fingerprint
 * for every frame of the given frequency domain audio data.
 * 
 */
public class AcousticAnalyzer {
    /**
     * 
     * @param audioSamples
     *            : Complex number representing amplitude and phase in frequency
     *            domain, where audioSamples[i][0] -> Real and
     *            audioSamples[i][1] -> Imaginary
     * @param frameSize
     *            : The frame size in terms of number of samples for which the
     *            fingerprint has to be extracted
     * @return
     */
    public static double[] extractRmsBasedFingerprint(
            double[] audioSamples,
            int frameSize) {
        frameSize = frameSize << 1;
        int samplesLength = audioSamples.length;
        int halfFrameSize = (frameSize >> 1);
        int quarterFrameSize = (halfFrameSize >> 1);
        int threeQuarterFrameSize = halfFrameSize+quarterFrameSize;
        int i = 0;
        double sum = 0;
        double[] rmsValues = new double[samplesLength / frameSize];
        double[] absValues = new double[samplesLength];
        for (int j = 0; j < samplesLength; j++) {            
            absValues[j] =
                    Math.pow(audioSamples[j], 2)
                            + Math.pow(audioSamples[j+halfFrameSize], 2);
            sum += absValues[j];

            if (j % quarterFrameSize == 0 && j != 0) {
                rmsValues[i++] =
                        computeRms(sum / quarterFrameSize, Arrays.copyOfRange(
                                absValues, j - quarterFrameSize, j));
                sum = 0;
                j = j + threeQuarterFrameSize;
            }
        }
        return rmsValues;
    }

    /**
     * 
     * @param avg : average value for Normalization 
     * @param absValues : absolute value representing amplitude of a sample
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
