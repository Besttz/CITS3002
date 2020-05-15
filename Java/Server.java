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
     * Analysis the HTTP request from browser and return the URL
     * 
     * 
     * @param inputARG the INputStream from socket
     * @return the request URL
     */
    private String parseRequest(InputStream inputARG) {
        StringBuffer request = new StringBuffer(4096);
        int length;
        byte[] buffer = new byte[4096];
        // Using a byte buffer to store this HTTP request
        try {
            // Read input stream to buffer and save length
            length = inputARG.read(buffer);
        } catch (IOException e) {
            e.printStackTrace();
            length = -1;
        }
        for (int i = 0; i < length; i++) {
            request.append((char) buffer[i]);
        }
        String requestString = request.toString();

        // Get the real URI request from the full text
        int begin = requestString.indexOf(' ');
        int end = requestString.indexOf(' ', begin + 1);
        if (begin != -1 && end > begin)
            return requestString.substring(begin + 1, end);
        return "";
    }

    /**
     * Call this method to run the server
     */
    public void run() {
        ServerSocket server;
        try {
            server = new ServerSocket(tcpPort);
            System.out.println("Start to listen TCP");// TEST
            boolean running = true;
            while (running) {
                Socket client = null;
                InputStream input = null;
                OutputStream output = null;

                try {
                    // Waiting and create the client socket
                    client = server.accept();
                    input = client.getInputStream();
                    output = client.getOutputStream();

                    System.out.println("New Connection"); //TEST
                    
                    String request = parseRequest(input);
                    System.out.println(request); //TEST
                    
                    // output.write(request.getBytes());
                    String  response= "HTTP/1.1 200 ok \n" + "Content-Type: text/html\n" + "Content-Length: "
                    + request.length() + "\n\n" + request;
                    output.write(response.getBytes());
                    
                    System.out.println("Close Connection"); //TEST
                    // Close this connection
                    client.close();

                } catch (Exception e) {
                    e.printStackTrace();
                    continue;
                }
            }
            server.close();
        } catch (IOException e) {
            System.out.println("Cannot establish TCP connection");
            e.printStackTrace();
        }
    }

    public static void main(String args[]) {
        Server station = new Server(args[0], Integer.parseInt(args[1]), Integer.parseInt(args[2]));
        for (int i = 3; i < args.length; i++) {
            station.addAdjancent(Integer.parseInt(args[i]));
        }
        station.run();
        System.out.println(station.toString());
    }
}
