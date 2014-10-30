import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * @author: Magesh Ramachandran
 * @author: Mayank Narashiman
 * @author: Narendran K.P
 */

public class StreamConsumer implements Runnable {
    InputStream inputStream;

    StreamConsumer(InputStream is) {
        this.inputStream = is;
    }

    @Override
    public void run() {
        try {
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(inputStream));
            String line = null;
            while ((line = reader.readLine()) != null) {
                // do nothing, just consume and clear the stream
            }
        } catch (IOException e) {
            //e.printStackTrace();
        }
    }
}
