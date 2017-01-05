package global.tri.flyer_sample_app;

import java.io.DataInputStream;
import java.io.IOException;

/**
 * Created by Rob Garbanati on 1/4/2017.
 */

public class ReaderThread extends Thread {
    DataInputStream dataInputStream = null;

    ReaderThread(DataInputStream streamParameter) {
        dataInputStream = streamParameter;
    }
    public void run() {
        System.out.println("ReaderThread started.");
        if(dataInputStream != null) {
            System.out.println("dataInputStream not null!");
        } else {
            System.out.println("dataInputStream is null :(");
        }
        try {
            System.out.println("message: " + dataInputStream.read());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
