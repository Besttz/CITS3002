import java.net.*;
import java.util.*;
import java.io.*;

class Server implements Runnable {
    private int tcpPort;
    private int udpPort;
    private ArrayList<Integer> adjPort;
    private ArrayList<Route> timeTable;
    boolean tcpEstablished;
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
     * @param inputARG the INputStream from socket
     * @return the request URL
     */
    private String parseRequest(InputStream inputARG) {
        StringBuffer request = new StringBuffer(1024);
        int length;
        byte[] buffer = new byte[1024];
        // Using a byte buffer to store this HTTP request
        System.out.println("Start to parse"); // TEST
        try {
            // Read input stream to buffer and save length
            length = inputARG.read(buffer);
        } catch (IOException e) {
            // e.printStackTrace();
            return "";
            // length = -1;
        }
        for (int i = 0; i < length; i++) {
            request.append((char) buffer[i]);
        }
        String requestString = request.toString();
        // Get the real URI request from the full text
        int begin = requestString.indexOf(' ');
        int end = requestString.indexOf(' ', begin + 1);
        if (begin != -1 && end > begin) {
            String fullRequest = requestString.substring(begin + 1, end);
            System.out.println("TCP Request: " + fullRequest); // TEST
            if (fullRequest.contains("/?to=")) {
                // Get the name of station from localhost:port/?to=XXX
                String station = fullRequest.substring(5);
                System.out.println("TCP Request Parsed: " + station); // TEST
                return station;
            }
        }
        return "";
    }

    /**
     * Call this method to run the server
     */
    public void run() {
        if (!tcpEstablished) {
            tcpEstablished = true;
            ServerSocket server;
            try {
                server = new ServerSocket(tcpPort);
                System.out.println("Start to listen TCP at " + tcpPort);// TEST
                boolean running = true;
                while (running) {
                    Socket client = null;
                    InputStream input = null;
                    OutputStream output = null;
                    // BufferedReader in = null;
                    // PrintWriter out = null;
                    try {
                        // Waiting and create the client socket
                        client = server.accept();
                        client.setSoTimeout(500);
                        input = client.getInputStream();
                        output = client.getOutputStream();
                        // in = new BufferedReader(new InputStreamReader(client.getInputStream()));

                        System.out.println("TCP: New Connection"); // TEST

                        String request = parseRequest(input);
                        System.out.println("TCP Station Name: " + request); // TEST

                        if (request.length() > 0) {
                            // SEND UDP MESSAGE // TEST
                            byte[] buf = new byte[1024];
                            DatagramSocket ds = new DatagramSocket(9000);
                            DatagramPacket dp_receive = new DatagramPacket(buf, 1024);
                            String str_send = "Hello UDPserver";
                            InetAddress loc = InetAddress.getLocalHost();
                            DatagramPacket dp_send = new DatagramPacket(str_send.getBytes(), str_send.length(), loc,
                                    5001);
                            ds.send(dp_send);
                            boolean receivedResponse = true; //Skip // TEST
                            while (!receivedResponse) {
                                try {
                                    ds.receive(dp_receive);
                                    // if (!dp_receive.getAddress().equals(loc)) {
                                    // throw new IOException("Received packet from an umknown source");
                                    // }
                                    receivedResponse = true;
                                } catch (InterruptedIOException e) {
                                    // 如果接收数据时阻塞超时，重发并减少一次重发的次数
                                    // tries += 1;

                                    // System.out.println("Time out," + (MAXNUM - tries) + " more tries...");
                                }
                            }

                            // System.out.println("UDP: Client received data from server：");
                            // String str_receive = new String(dp_receive.getData(), 0, dp_receive.getLength()) + " from "
                            //         + dp_receive.getAddress().getHostAddress() + ":" + dp_receive.getPort();
                            // System.out.println(str_receive);
                            // dp_receive.setLength(1024);

                            ds.close();

                            // HTTP Response
                            String response = "HTTP/1.1 200 ok \n" + "Content-Type: text/html\n" + "Content-Length: "
                                    + request.length() + "\n\n" + request;
                            output.write(response.getBytes());
                            output.flush();
                            output.close();
                        } else {
                            // HTTP 404 Response
                            String response = "HTTP/1.1 404 Not Found \n" + "Content-Type: text/html\n" + "Content-Length: 29"
                                    + "\n\n"+"<h1>Request Not Correct!</h1>";
                            output.write(response.getBytes());
                            output.flush();
                            output.close();
                            System.out.println("TCP: 404 Responsed"); // TEST
                        }

                        System.out.println("TCP: Close Connection"); // TEST
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
        } else {
            try {
                // System.out.println("THIS IS UDP THREAD"); //TEST
                byte[] buf = new byte[1024];
                DatagramSocket ds = new DatagramSocket(udpPort);
                DatagramPacket dp_receive = new DatagramPacket(buf, 1024);
                System.out.println("Start to listen UDP at " + udpPort);
                boolean running = true;
                while (running) {
                    ds.receive(dp_receive);
                    System.out.println("UDP: Received data: ");
                    String str_receive = new String(dp_receive.getData(), 0, dp_receive.getLength()) + " from "
                            + dp_receive.getAddress().getHostAddress() + ":" + dp_receive.getPort();
                    System.out.println(str_receive);
                    String str_send = "Reply from UDP receiver.";
                    DatagramPacket dp_send = new DatagramPacket(str_send.getBytes(), str_send.length(),
                            dp_receive.getAddress(), 9000);
                    ds.send(dp_send);
                    // 由于dp_receive在接收了数据之后，其内部消息长度值会变为实际接收的消息的字节数，
                    // 所以这里要将dp_receive的内部消息长度重新置为1024
                    dp_receive.setLength(1024);
                }
                ds.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

    }

    public static void main(String args[]) {
        Server station = new Server(args[0], Integer.parseInt(args[1]), Integer.parseInt(args[2]));
        for (int i = 3; i < args.length; i++) {
            station.addAdjancent(Integer.parseInt(args[i]));
        }
        Thread tcp = new Thread(station);
        Thread udp = new Thread(station);
        tcp.start();
        udp.start();
        // System.out.println(station.toString());
    }
}
