package global.tri.flyer_sample_app;
import java.io.DataOutputStream;
import java.io.IOException;

/**
 * Created by Rob Garbanati on 1/4/2017.
 */

public class WriterThread extends Thread {
    DataOutputStream dataOutputStream = null;

    WriterThread(DataOutputStream streamParameter) {
        dataOutputStream = streamParameter;
    }
    public void run() {
        System.out.println("WriterThread started.");
        if(dataOutputStream != null) {
            System.out.println("dataOutputStream not null!");
        } else {
            System.out.println("dataOutputStream is null :(");
        }
        try {
            dataOutputStream.writeUTF("Hello 1!\r\n");
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
