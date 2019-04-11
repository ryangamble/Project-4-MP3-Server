import java.io.*;
import java.lang.reflect.Array;
import java.net.Socket;
import java.io.IOException;
import java.net.ServerSocket;
import java.io.FileReader;
import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;


/**
 * A MP3 Server for sending mp3 files over a socket connection.
 */
public class MP3Server {

    private ServerSocket serverSocket;

    public static void main(String[] args) {
        MP3Server server;

        try {
            server = new MP3Server(4242);
        } catch (IOException e) {
            System.out.println("An mistake occured");
            System.out.println("Exception message : " + e.getMessage());
            System.out.println("Stopping the server");
            return;
        }
        server.serveClients();
    }

    public MP3Server(int port) throws IllegalArgumentException, IOException {
        if (port < 0) {
            throw new IllegalArgumentException("port argument is negative");
        } else {
            this.serverSocket = new ServerSocket(port);
        }
    }

    public void serveClients() {
        Socket clientSocket;
        ClientHandler requestHandler;

        System.out.println("Starting the server");

        while (true) {
            try {
                clientSocket = this.serverSocket.accept();

            } catch (IOException e) {
                System.out.println("Exception happened");
                System.out.println("Exception message : " + e.getMessage());
                System.out.println("Stopping the server");

                try {
                    this.serverSocket.close();
                } catch (IOException a) {
                    a.printStackTrace();
                }
                return;
            }
            System.out.println("Connect to a client");
            requestHandler = new ClientHandler(clientSocket);
            new Thread(requestHandler).start();
        }
    }


}


/**
 * Class - ClientHandler
 * <p>
 * This class implements Runnable, and will contain the logic for handling responses and requests to
 * and from a given client. The threads you create in MP3Server will be constructed using instances
 * of this class.
 */
final class ClientHandler implements Runnable {

    private ObjectInputStream inputStream;
    private ObjectOutputStream outputStream;
    private Socket clientSocket;

    public ClientHandler(Socket clientSocket) throws IllegalArgumentException {
        if (clientSocket == null)
            throw new IllegalArgumentException("clientSocket argument is null");
        else
            this.clientSocket = clientSocket;
    }

    /**
     * This method is the start of execution for the thread. See the handout for more details on what
     * to do here.
     */
    public void run() {
        ObjectInputStream ois;
        ObjectOutputStream oos;

        try {
            ois = new ObjectInputStream(clientSocket.getInputStream());
            oos = new ObjectOutputStream(clientSocket.getOutputStream());


            while (!clientSocket.isClosed()) {
                Object input = ois.readObject();
                SongRequest request = (SongRequest) input;
                String songName = request.getArtistName() + "-" + request.getSongName();

                if (request.isDownloadRequest()) {
                    if (fileInRecord(songName)) {
                        SongHeaderMessage respond = new SongHeaderMessage(true, "zsfd",
                                "szfd", -1);
                        oos.writeObject(respond);
                    } else {
                        byte[] songData = readSongData("");
                        SongHeaderMessage respond = new SongHeaderMessage(true, request.getArtistName(),
                                request.getSongName(),songData.length );
                        sendByteArray(songData);
                    }
                } // End of the program for the download request.
                else {
                    
                }

            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Searches the record file for the given filename.
     *
     * @param fileName the fileName to search for in the record file
     * @return true if the fileName is present in the record file, false if the fileName is not
     */
    private static boolean fileInRecord(String fileName) {
        BufferedReader fis;
        try {
            fis = new BufferedReader(new FileReader("record.txt"));
            String temp = fis.readLine();
            String record = "";
            while (temp != null) {
                record += temp;
                temp = fis.readLine();
            }
            fis.close();
            return record.contains(fileName);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Read the bytes of a file with the given name into a byte array.
     *
     * @param fileName the name of the file to read
     * @return the byte array containing all bytes of the file, or null if an error occurred
     */
    private static byte[] readSongData(String fileName) {
        FileInputStream fis;

        try {
            fis = new FileInputStream(fileName);
            return fis.readAllBytes();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Split the given byte array into smaller arrays of size 1000, and send the smaller arrays
     * to the client using SongDataMessages.
     *
     * @param songData the byte array to send to the client
     */
    private void sendByteArray(byte[] songData) {
        try {
            byte[] splitData = readSongData(""); // TODO The file name has not been entered
            int n = splitData.length;

            byte[] a = new byte[1000];
            for (int i = 0; i < splitData.length; i += 1000) {
                for (int c = i; c < i + 1000; c++) {
                    a[c] = splitData[i + 1000];
                }
                SongDataMessage transfer = new SongDataMessage(a);
            }
        } catch (Exception e) {
            System.out.println("Something unexpected happened");
            e.printStackTrace();
        }
    }

    /**
     * Read ''record.txt'' line by line again, this time formatting each line in a readable
     * format, and sending it to the client. Send a ''null'' value to the client when done, to
     * signal to the client that you've finished sending the record data.
     */
    private void sendRecordData() {
        ArrayList<String> record = new ArrayList<String>();
        try {
            BufferedReader read = new BufferedReader(new FileReader("record.txt"));
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(this.clientSocket.getOutputStream()));
            String line;
            while ((line = read.readLine()) != null) {
                record.add(line);
            }
            for (int i = 0; i < record.size(); i++) {
                writer.write(record.get(i));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
