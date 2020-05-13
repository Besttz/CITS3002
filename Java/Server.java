import java.net.*;
import java.util.ArrayList;
import java.io.*;

class Server {
    private int tcpPort;
    private int udpPort;
    private ArrayList<Integer> adjPort;
    private int adjStationNum;
    String sName;

    public Server(String name, int tcp, int udp) {
        sName = name;
        tcpPort = tcp;
        udpPort = udp;
        adjPort = new ArrayList<>();
    }

    public void addAdjancent(int port) {
        adjPort.add(port);
    }

    public String toString() {
        return "Station: " + sName + "\nTCP Port: " + tcpPort + "\nUDP Port: " + udpPort + "\nAdjancent Ports:"
                + adjPort.toString();
    }

}
