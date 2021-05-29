import java.io.IOException;
import java.net.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

public class Client {
    private static DatagramSocket socket;
    private static HashMap<Endpoint, ArrayList<Packetdata>> endpointMap;
    private static HashMap<Endpoint, TimeManager> timerMap;

    public static void main(String[] args) {
        try {

            //init
            socket = new DatagramSocket();
            InetAddress address = InetAddress.getByName("localhost");
            endpointMap = new HashMap<>();
            timerMap = new HashMap<>();


            //Serverports abfragen
            boolean eingabe = true;
            while(eingabe) {
                System.out.println("Auf welchem Port laeuft der Server?");
                Scanner sc = new Scanner(System.in);
                int port = sc.nextInt();

                boolean vorhanden = false;
                for (Map.Entry<Endpoint, ArrayList<Packetdata>> entry : endpointMap.entrySet()) {
                    if (entry.getKey().getPort() == port) {
                        System.out.println("Server wird bereits angepingt");
                        vorhanden = true;
                        break;
                    }
                }

                if (!vorhanden) {
                    endpointMap.put(new Endpoint(address, port), new ArrayList<>());
                }

                System.out.println("Noch ein Server? (j/n)");
                String antwort = sc.next();
                if (!antwort.equals("j")) {
                    eingabe = false;
                }

            }

            // Für jeden Server wird TimeManager erstellt und gewisse Anzahl an Messages verschickt
            for (Map.Entry<Endpoint, ArrayList<Packetdata>> entry : endpointMap.entrySet()) {
                timerMap.put(entry.getKey(), new TimeManager());
                for (int i = 0; i < Help.WINDOW_SIZE; i++) {
                    sendMessage(entry.getKey());
                }
            }

            socket.setSoTimeout(Help.SOCKET_TIMEOUT);

            while (true) {

                byte[] receiveData = new byte[1024];

                try {
                    DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
                    //Blockiert solange, bis ein Packet auf Socket ankommt oder Timeout eintritt
                    socket.receive(receivePacket);
                    long timeEnd = System.currentTimeMillis();

                    Endpoint e = null;

                    // Sender in Map finden
                    for (Map.Entry<Endpoint, ArrayList<Packetdata>> entry : endpointMap.entrySet()) {
                        if (entry.getKey().getAddress().getHostAddress().equals(receivePacket.getAddress().getHostName())
                                && entry.getKey().getPort() == receivePacket.getPort()) {
                            e = entry.getKey();
                            timerMap.get(e).resetTimeout();
                            timerMap.get(e).interruptTimer();
                            break;
                        }
                    }

                    //Message aus Packet
                    String message = new String(receivePacket.getData()).substring(0, receivePacket.getLength());
                    Message receiveMessage = Message.parse(message);

                    if (e != null && receiveMessage != null) {
                        ArrayList<Packetdata> packets = endpointMap.get(e);

                        //Wenn PONG-Message
                        if (receiveMessage.getContent().equals("PONG")) {
                            System.out.println("<---------- " + receiveMessage + " Port: " + receivePacket.getPort());

                            //Durch packets iterieren und Packet mit seqnum von PONG-Nachricht löschen
                            for (int i = 0; i < packets.size(); i++) {

                                //Passende PING zu PONG-Nachricht aus packets löschen
                                if (packets.get(i).getMessage().getSeqnum() == receiveMessage.getSeqnum()) {
                                    System.out.println("RTT " + receiveMessage + " : " + (timeEnd - packets.get(i).getTimeStart()) + " ms");
                                    packets.remove(packets.get(i));
                                    //Nächste PINGs schicken
                                    while (packets.size() < Help.WINDOW_SIZE && e.getCurSeqnum() < Help.NUMBER_PACKAGES) {
                                        sendMessage(e);
                                    }

                                    break;
                                }
                            }
                        } else {
                            System.out.println("Keine PONG-Nachricht");
                        }
                    }

                    //Auf Timer-TIMEOUT prüfen
                    for (Map.Entry<Endpoint, ArrayList<Packetdata>> entry : endpointMap.entrySet()) {
                        testTimer(entry.getKey());
                    }


                } catch (SocketTimeoutException e) {

                    if (!endpointMap.isEmpty()) {
                        boolean finishedTotal = true;
                        // Für jeden Server
                        for (Map.Entry<Endpoint, ArrayList<Packetdata>> entry : endpointMap.entrySet()) {
                            timerMap.get(entry.getKey()).interruptTimer();
                            timerMap.get(entry.getKey()).resetTimeout();
                            boolean finished = deletePackets(entry.getKey());
                            if (!finished) {
                                finishedTotal = false;
                            }
                        }
                        if (finishedTotal) {
                            break;
                        }
                    } else {
                        break;
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }


            socket.close();
            System.exit(0);

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    //-------------------
    public static void sendMessage(Endpoint endpoint) {

        try {
            //PING-Nachricht mit aktueller Seqnum erstellen
            Message pingMessage = new Message("PING", endpoint.getCurSeqnum());

            //Message für Senden vorbereiten
            byte[] sendData = (pingMessage.serialize()).getBytes();
            DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, endpoint.getAddress(), endpoint.getPort());

            //Netzwerkfehler simulieren
            if (Help.isFakeNetworkError()) {
                //Fehler
                System.out.println("Fehler beim Senden von " + pingMessage + " an Port: " + sendPacket.getPort());
            } else {
                //Kein Fehler
                //Packet auf Netzwerk schreiben
                socket.send(sendPacket);
                System.out.println(pingMessage + " ----------> Port: " + sendPacket.getPort());
            }

            //Timer Starten
            long timeStart = System.currentTimeMillis();
            timerMap.get(endpoint).startTimer();

            //Packet zu packets hinzufügen
            Packetdata pd = new Packetdata(pingMessage, timeStart);
            endpointMap.get(endpoint).add(pd);
            endpoint.nextSeqnum();

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public static void testTimer(Endpoint endpoint) {
        if (timerMap.get(endpoint).isTimeout()) {
            timerMap.get(endpoint).resetTimeout();
            timerMap.get(endpoint).interruptTimer();
            deletePackets(endpoint);
        }
    }

    public static boolean deletePackets(Endpoint endpoint) {
        ArrayList<Packetdata> packets = endpointMap.get(endpoint);
        boolean finished = true;

        if (packets.size() > 0) {
            finished = false;

            //alle Packets von Endpoint löschen
            for (Packetdata packet : packets) {
                System.out.println("Verlorene Nachricht: " + packet.getMessage().toString() + " Port: " + endpoint.getPort());
            }
            packets.clear();

            //Nächste PINGs schicken
            while (packets.size() < Help.WINDOW_SIZE && endpoint.getCurSeqnum() < Help.NUMBER_PACKAGES) {
                sendMessage(endpoint);
            }
        }

        return finished;
    }

}
