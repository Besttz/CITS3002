import java.net.*;
import java.util.*;
import java.io.*;

class Server {
    private int tcpPort;
    private int udpPort;
    private ArrayList<Integer> adjPort;
    private ArrayList<Route> timeTable;
    // private int adjStationNum;
    String sName;

    public Server(String name, int tcp, int udp) {
        sName = name;
        tcpPort = tcp;
        udpPort = udp;
        adjPort = new ArrayList<>();
        timeTable = new ArrayList<>();
        readTT();
        // adjStationNum = 0;
    }

    public void addAdjancent(int port) {
        adjPort.add(port);
        // adjStationNum ++;
    }

    public void readTT(){
        try {
            File file = new File("tt-"+sName);
            Scanner myReader = new Scanner(file);
            while (myReader.hasNextLine()) {
              String data = myReader.nextLine();
              System.out.println(data);
            }
            myReader.close();
          } catch (FileNotFoundException e) {
            System.out.println("An error occurred.");
            e.printStackTrace();
          }
        
    }
    public String toString() {
        return "Station: " + sName + "\nTCP Port: " + tcpPort + "\nUDP Port: " + udpPort + "\nAdjancent Ports:"
                + adjPort.toString();
    }

}
