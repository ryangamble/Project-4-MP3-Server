import java.io.*;
import java.net.Socket;
import java.util.Scanner;

/**
 * An MP3 Client to request .mp3 files from a server and receive them over the socket connection.
 */
public class MP3Client {

    public static void main(String[] args) throws IOException, InterruptedException {
        Scanner in = new Scanner(System.in);
        Boolean cont = true;
        String choice;
        String song;
        String artist;
        SongRequest request;
        Socket socket;
        ObjectOutputStream oos;

        while (cont) {
            do {
                socket = new Socket("mp3client.com", 4242);
                oos = new ObjectOutputStream(socket.getOutputStream());

                System.out.println("Select an option: ");
                System.out.println("1. View available songs");
                System.out.println("2. Download a song");
                System.out.println("3. Exit");
                choice = in.nextLine();
                choice = choice.toLowerCase();

                if ((!choice.equals("1")) && (!choice.equals("2")) && (!choice.equals("3"))
                        && (!choice.equals("exit"))) {
                    System.out.println("Invalid input, only 1, 2, 3, and exit are accepted\n");
                }
            } while ((!choice.equals("1")) && (!choice.equals("2")) && (!choice.equals("3"))
                    && (!choice.equals("exit")));

            switch (choice) {
                case "1":
                    request = new SongRequest(false);
                    oos.writeObject(request);
                    break;
                case "2":
                    System.out.print("Enter the song name: ");
                    song = in.nextLine();
                    System.out.print("Enter the artist name: ");
                    artist = in.nextLine();
                    request = new SongRequest(true, song, artist);
                    oos.writeObject(request);
                    break;
                case "3":
                    cont = false;
                    break;
            }

            if (cont) {
                Thread t = new Thread(new ResponseListener(socket));
                t.start();
                t.join();
                socket.close();
            }
        }
    }
}


/**
 * This class implements Runnable, and will contain the logic for listening for
 * server responses. The threads you create in MP3Server will be constructed using
 * instances of this class.
 */
final class ResponseListener implements Runnable {

    private ObjectInputStream ois;
    private Object message;
    private SongHeaderMessage headerMessage;
    private SongDataMessage dataMessage;
    private byte[] songData;
    private byte[] partialData;
    private int dataCount = 0;
    private String infoString = "";
    private String fileName;

    public ResponseListener(Socket clientSocket) throws IOException {
        ois = new ObjectInputStream(clientSocket.getInputStream());
    }

    /**
     * Listens for a response from the server.
     * <p>
     * Continuously tries to read a SongHeaderMessage. Gets the artist name, song name, and file size from that header,
     * and if the file size is not -1, that means the file exists. If the file does exist, the method then subsequently
     * waits for a series of SongDataMessages, takes the byte data from those data messages and writes it into a
     * properly named file.
     */
    public void run() {
        try {
            songData = new byte[6000000];

            while (true) {
                message = ois.readObject();
                if ((message != null) && ((message instanceof SongHeaderMessage) ||
                        (message instanceof SongDataMessage))) {
                    headerMessage = (SongHeaderMessage)message;

                    if ((headerMessage.isSongHeader()) && (headerMessage.getFileSize() != -1)) {
                        dataMessage = (SongDataMessage)ois.readObject();
                        while (dataMessage != null) {
                            partialData = dataMessage.getData();
                            for (int i = 0; i < dataMessage.getData().length; i++) {
                                songData[dataCount] = partialData[i];
                                dataCount += 1;
                            }
                        }
                        fileName = headerMessage.getArtistName() + " - " + headerMessage.getSongName() + ".mp3";
                        writeByteArrayToFile(songData, fileName);
                    } else {
                        message = ois.readObject();
                        while (message != null) {
                             infoString = (String)message;

                             System.out.println(infoString);
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.out.print(e.getMessage());
        }
    }

    /**
     * Writes the given array of bytes to a file whose name is given by the fileName argument.
     *
     * @param songBytes the byte array to be written
     * @param fileName  the name of the file to which the bytes will be written
     */
    private void writeByteArrayToFile(byte[] songBytes, String fileName) throws FileNotFoundException {
        File f = new File(fileName);

        FileOutputStream fos = new FileOutputStream(f, false);

        PrintWriter pw = new PrintWriter(fos);

        pw.print(songBytes);

        pw.close();
    }
}