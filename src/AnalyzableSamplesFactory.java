import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author: Magesh Ramachandran
 * @author: Mayank Narashiman
 * @author: Narendran K.P
 * 
 *          </br>Description: Static factory class that instantiates the
 *          AnalyzableSamples object
 * 
 */
public class AnalyzableSamplesFactory {

	private static int FRAGMENT_SIZE_TO_MATCH_IN_SECONDS = 5;
	private static final int SAMPLES_PER_FRAME = 1764;
	private static final int ERROR_THRESHOLD = 5,
	        FFT_WINDOW_SIZE = 2048,
	        FRAME_COUNT_FOR_5_SECONDS = 225,
	        ERROR_DENSITY = 9,
	        MIN_HASH_COLLISIONS_FOR_MATCH = ERROR_THRESHOLD
	                + (int) ((double) FRAME_COUNT_FOR_5_SECONDS / ERROR_DENSITY)
	                + 2;

	private static double OFFSET_IN_SECONDS = ((double) SAMPLES_PER_FRAME)
	        / (2 * 44100.0);

	static {
		AnalyzableSamples.initialize(FFT_WINDOW_SIZE, SAMPLES_PER_FRAME);
	}

	/**
	 * 
	 * @param listOfFiles1
	 * @return - list of AnalyzableSamples
	 */
	public static List<AnalyzableSamples> makeListOfAnalyzableSamples(
	        AudioFile[] listOfFiles1) {
		int duration;
		List<AnalyzableSamples> asl = new ArrayList<AnalyzableSamples>();
		for (AudioFile af : listOfFiles1) {
			duration = af.getDurationInSeconds();
			if (duration < FRAGMENT_SIZE_TO_MATCH_IN_SECONDS) {
				continue;
			}
			AnalyzableSamples as = make(af);
			as.setFileName(af.getShortName());
			asl.add(as);
		}
		return asl;
	}

	/**
	 * 
	 * @param data - Array containing audio sample data
	 * @return {@AnalyzableSamples}
	 */
	public static AnalyzableSamples make(AudioFile audioFile) {
		return new AnalyzableSamplesForFragmentMatching(audioFile);
	}

	/**
	 * Description - This implementation is used for representing audio samples
	 * in way that facilitates perceptual comparison
	 * 
	 */
	private static class AnalyzableSamplesForFragmentMatching extends
	        AnalyzableSamples {

		private AnalyzableSamplesForFragmentMatching(AudioFile audioFile) {
			super(audioFile);
		}

		@Override
		public double[] getMatchPositionInSeconds(AnalyzableSamples aS2) {
			return computeFragmentMatchWithTime(this.getFingerprint(),
			        aS2.getFingerprint());
		}

		/**
		 * 
		 * @param fp1 - HashMap representing a fingerprint
		 * @param fp2 - HashMap representing another fingerprint
		 * @return - If there is a match, returns an array of two elements with
		 *         each element representing the time at which the match was
		 *         found. Otherwise, returns a null value.
		 */
		private double[] computeFragmentMatchWithTime(
		        Map<Integer, List<Integer>> fp1, Map<Integer, List<Integer>> fp2) {
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

			return new double[] { (OFFSET_IN_SECONDS * sindex1),
			        (OFFSET_IN_SECONDS * sindex2) };
		}

		/**
		 * 
		 * @param s - set of time offsets
		 * @return - start time of the sequence with a hash match
		 */
		private int extractSequenceStartIndexForMatch(Set<Integer> s) {
			int size = s.size();
			int errors = 0, sofar = 0, seq = 0, prevseq = 0, rindex = -1;
			Integer[] sequence = new Integer[size];
			sequence = s.toArray(sequence);
			Arrays.sort(sequence);
			// System.out.println(Arrays.asList(sequence));
			if (sequence.length <= MIN_HASH_COLLISIONS_FOR_MATCH) {
				return -1;
			}
			int cleanupidx = 0;
			int[] errarr = new int[size];
			int[] diffarr = new int[size];
			int diff = 0;
			for (int i = 0; i < sequence.length - 1;) {
				if (errors >= (ERROR_THRESHOLD + (ERROR_DENSITY * seq))) {
					sofar = sofar - diffarr[cleanupidx];
					errors = errors - errarr[cleanupidx];
					cleanupidx++;
					seq = seq - 1;
					continue;
				} else if (sofar > FRAME_COUNT_FOR_5_SECONDS) {
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

	}
}
