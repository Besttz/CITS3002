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

    /**
     * To add adjancent station's UDP port while initialisation
     * 
     * @param port: The new adjancent port to add
     */
    public void addAdjancent(int port) {
        adjPort.add(port);
    }

    /**
     * Clear timeTable then Read tt-<Station Name> file to update
     */
    private void readTT() {
        timeTable.clear();
        try {
            File file = new File("tt-" + sName); // Open the TimeTable File
            Scanner myReader = new Scanner(file);
            Boolean firstLine = true;
            while (myReader.hasNextLine()) {
                String line = myReader.nextLine();// Get a new line
                // System.out.println("Opened File"); //TEST
                String delimeter = ",";
                String[] data = line.split(delimeter);
                // Check if it's the first line
                if (firstLine) {
                    if (!data[0].equals(sName))
                        System.out.println("Time Table File is not for this station.");
                    firstLine = false;
                    continue;// Check if the file has right station info
                }
                int dH = Integer.parseInt(data[0].substring(0, 2));
                int dM = Integer.parseInt(data[0].substring(3, 5));
                int aH = Integer.parseInt(data[3].substring(0, 2));
                int aM = Integer.parseInt(data[3].substring(3, 5));
                timeTable.add(new Route(sName, dH, dM, data[1], data[2], aH, aM, data[4]));
                // System.out.println(line);//TEST
            }
            myReader.close();
        } catch (FileNotFoundException e) {
            System.out.println("An error occurred.");
            e.printStackTrace();
        }

    }

    /**
     * Print Station Description to help developer to check.
     */
    public String toString() {
        return "Station: " + sName + "\nTCP Port: " + tcpPort + "\nUDP Port: " + udpPort + "\nAdjancent Ports:"
                + adjPort.toString();
    }

    /**
     * Call this method to run the server
     */
    public void run() {
        ServerSocket server;
        try {
            server = new ServerSocket(tcpPort);
            System.out.println("Start to listen TCP");//TEST
            Socket client = null;
            boolean f = true;
            while(f){
                //Waiting for the TCP connection
                client = server.accept();
                System.out.println("Connected!");//TEST
                // new Thread(new ServerThread(client)).start();
            }
            server.close();
        } catch (IOException e) {
            System.out.println("Cannot establish TCP connection");
            e.printStackTrace();
        }
    }
}
