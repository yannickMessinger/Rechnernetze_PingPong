import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Scanner;

public class Server {
    private static DatagramSocket socket = null;

    public static void main(String[] args) {

        try {
            //Port-Eingabe
            System.out.println("Auf welchem Port soll der Server laufen?");
            Scanner sc = new Scanner(System.in);
            int port = sc.nextInt();

            socket = new DatagramSocket(port);

            while (true) {

                System.out.println("\nWartet auf Nachricht...\n");
                byte[] receiveData = new byte[1024];

                DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
                //Blockiert solange, bis ein Packet auf Socket ankommt
                socket.receive(receivePacket);

                //Message aus Packet
                String message = new String(receivePacket.getData()).substring(0, receivePacket.getLength());
                Message receiveMessage = Message.parse(message);
                InetAddress address = receivePacket.getAddress();
                int receivePort = receivePacket.getPort();

                if (receiveMessage != null) {
                    //Wenn PING-Message
                    if (receiveMessage.getContent().equals("PING")) {
                        System.out.println("<---------- " + receiveMessage + " Port: " + receivePacket.getPort());
                        answer(address, receivePort, receiveMessage);
                    } else {
                        System.out.println("Keine PING-Nachricht");
                    }
                }

            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void answer(InetAddress address, int port, Message receiveMessage) {

        try {
            //PONG-Nachricht mit passender Seqnum erstellen
            Message pongMessage = new Message("PONG", receiveMessage.getSeqnum());

            //Message für Senden vorbereiten
            byte[] sendData = (pongMessage.serialize()).getBytes();
            DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, address, port);

            //Netzwerkfehler simulieren
            if (Help.isFakeNetworkError()) {
                //Fehler
                System.out.println("Fehler beim Senden von " + pongMessage + " an Port: " + sendPacket.getPort());
            } else {
                //Kein Fehler
                //Packet auf Netzwerk schreiben
                socket.send(sendPacket);
                System.out.println(pongMessage + " ----------> Port: " + sendPacket.getPort());
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
