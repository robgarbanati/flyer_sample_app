package global.tri.flyer_sample_app;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.ArrayList;

/**
 * Created by Rob Garbanati on 1/4/2017.
 */

public class FlyerManagerThread extends Thread {
    private boolean alive = true;
    private double speed = 0;
    private double direction = 0;
    private ServerSocket serverSocket = null;
    private ArrayList<FlyerConnectionThread> connectionThreads = new ArrayList<FlyerConnectionThread>();

    private Object readLock = new Object();

    private static double clamp(double input, double min, double max) {
        if(input < min) return min;
        if(input > max) return max;
        return input;
    }

    private void writeState() {
        writeString("{\"speed\": " + speed + ", \"direction\": " + direction + "}");
    }

    public void setSpeedAndDirection(double newSpeed, double newDirection) {
        speed = clamp(newSpeed, -1.0, 1.0);
        direction = clamp(newDirection, -1.0, 1.0);
        writeState();
    }

    // Write string to all connectionThreads.
    private void writeString(String string) {
        Object array[] = null;
        if(string.contains("\n")) {
            throw new Error("Invalid packet. Contains \\n");
        }
        if(string.contains("\r")) {
            throw new Error("Invalid packet. Contains \\r");
        }


        synchronized (connectionThreads) {
            array = connectionThreads.toArray();
        }

        for (Object f : array) {
            ((FlyerConnectionThread) f).writeString(string);
        }

    }

    private void readCallback(String packet) {
        System.out.println(packet);
        synchronized (readLock) {
            writeString(packet);
        }
    }

    private class FlyerConnectionThread extends Thread {
        private Socket socket;
        private boolean connectionAlive = true;
        private boolean isFinished = false;
        DataInputStream dataInputStream = null;
        DataOutputStream dataOutputStream = null;

        boolean getFinished() {
            return isFinished;
        }

        public FlyerConnectionThread(Socket socketParam) {
            super();
            socket = socketParam;
        }

        public void writeString(String string) {
            DataOutputStream o = dataOutputStream;
            if (alive && connectionAlive && (o != null)) {
                try {
                    o.write(string.getBytes());
                    o.write("\r\n".getBytes());
                } catch (IOException e) {
                    //e.printStackTrace();
                }
            }
        }

        public void run() {
            byte byteBuffer[] = new byte[1600];
            try {
                socket.setSoTimeout(100);
            } catch (SocketException e) {
                System.out.println("Failed to set data socket timeout. Connection ignored.");
                connectionAlive = false;
            }

            try {
                // Create readable stream from new socket.
                dataInputStream = new DataInputStream(socket.getInputStream());
                // Create writable stream from new socket.
                dataOutputStream = new DataOutputStream(socket.getOutputStream());
            } catch (IOException e) {
                System.out.println("Can't create input and output streams for data socket. Connection ignored.");
                connectionAlive = false;
            }

            String buffer = "";

            // Only work while alive
            while(alive && connectionAlive) {
                // Read from adb then write a message.
                try {
                    // Read data from socket.
                    int bytesRead = dataInputStream.read(byteBuffer);
                    if(bytesRead > 0) {
                        String readString = new String(byteBuffer, 0, bytesRead, "UTF-8");
                        // Merge packets.
                        buffer += readString.replace('\r', '\n');
                        while (buffer.contains("\n")) {
                            // extract packet from buffer.
                            String packet = buffer.substring(0, buffer.indexOf("\n"));
                            if(packet.length() > 0) {
                                readCallback(packet);
                            }
                            // remove packet from front of buffer.
                            buffer = buffer.substring(buffer.indexOf("\n") + 1);
                        }
                        if(buffer.length() > 65535) {
                            System.out.println("Warning deleting data. Packet > 65535 bytes.");
                            buffer = buffer.substring(buffer.length() - 65535);
                        }
                    }

                } catch (SocketTimeoutException e) {
                    continue;
                } catch (IOException e) {
                    connectionAlive = false;
                }
            }

            if (socket != null) {
                try {
                    socket.close();
                } catch (IOException ee) {
                }
            }

            if (dataInputStream != null) {
                try {
                    dataInputStream.close();
                } catch (IOException ee) {
                }
            }

            if (dataOutputStream != null) {
                try {
                    dataOutputStream.close();
                } catch (IOException ee) {
                }
            }
            isFinished = true;
        }
    }

    public synchronized void shutdown() {
        alive = false;
        try {
            this.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        synchronized (connectionThreads) {
            for (FlyerConnectionThread f : connectionThreads) {
                try {
                    f.join();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void deleteDeadThreads() {
        while(alive) {
            FlyerConnectionThread dead = null;

            // Clean up any dead threads.
            synchronized (connectionThreads) {
                for (FlyerConnectionThread f : connectionThreads) {
                    if (f.getFinished()) {
                        dead = f;
                    }
                }
            }

            if (dead == null) break;
            try {
                dead.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            synchronized (connectionThreads) {
                connectionThreads.remove(dead);
            }
        }
    }

    public FlyerManagerThread() throws FlyerManagerStartupException {
        try {
            serverSocket = new ServerSocket(1337);
            System.out.println("Listening :1337");
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            throw new FlyerManagerStartupException("Couldn't listen for connection on port 1337. Likely due to another thread or application on same port.");
        }

        try {
            serverSocket.setSoTimeout(100);
        } catch (SocketException e) {
            e.printStackTrace();
            throw new FlyerManagerStartupException("Couldn't set timeout for socket on port 1337.");
        }

        super.start();
    }

    public void run() {
        Socket socket = null;

        while (alive) {
            deleteDeadThreads();

            // Wait to make new flyerConnectionThread if we have a valid socket.
            try {
                // Get socket generated by accept.
                // TODO only allows for 1 socket at a time?
                socket = serverSocket.accept();
                if (socket == null) continue;

                FlyerConnectionThread flyerConnectionThread = new FlyerConnectionThread(socket);
                flyerConnectionThread.start();

                synchronized (connectionThreads) {
                    connectionThreads.add(flyerConnectionThread);
                }

            } catch (SocketTimeoutException e) {
                continue;
            } catch (IOException e) {
            }
        }
        try {
            serverSocket.close();
        } catch (IOException e) {
        }
    }

    public class FlyerManagerStartupException extends Exception {
        public FlyerManagerStartupException(String string) {
            super(string);
        }
    }
}
