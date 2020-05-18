import java.net.*;
import java.util.*;
import java.io.*;

class Server implements Runnable {
    private int tcpPort;
    private int udpPort;
    private ArrayList<Integer> adjPort;
    // private ArrayList<Route> timeTable;
    private ArrayList<ArrayList<Route>> timeTable;
    private ArrayList<String> routeName;
    private ArrayList<Boolean> routeTerminal; // Yes if this route only has one side
    private ArrayList<String> routeNext; // Save the first next station when see a new route
    private boolean tcpEstablished;
    private String sName;
    private String delimeter = ",";

    public Server(String name, int tcp, int udp) {
        sName = name;
        tcpPort = tcp;
        udpPort = udp;
        adjPort = new ArrayList<>();
        timeTable = new ArrayList<>();
        routeName = new ArrayList<>();
        routeTerminal = new ArrayList<>();
        routeNext = new ArrayList<>();
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
                String[] data = line.split(delimeter);
                // Check if it's the first line
                if (firstLine) {
                    if (!data[0].equals(sName))
                        System.out.println("Time Table File is not for this station.");
                    firstLine = false;
                    continue;// Check if the file has right station info
                }
                // Check if this route already recorded
                int routeNo = -1; // The local no. of this route, -1 if unadd
                for (int i = 0; i < routeName.size(); i++)
                    if (routeName.get(i).equals(data[1]))
                        routeNo = i;

                if (routeNo == -1) { // Not Added Yet
                    routeName.add(data[1]);
                    timeTable.add(new ArrayList<>());
                    routeNo = routeTerminal.size();
                    routeNext.add(data[4]);
                    routeTerminal.add(true);
                }
                // Check if this "next station" different than recorded
                if (routeTerminal.get(routeNo) && !routeNext.get(routeNo).equals(data[4])) {
                    routeTerminal.set(routeNo, false);
                }
                int dH = Integer.parseInt(data[0].substring(0, 2));
                int dM = Integer.parseInt(data[0].substring(3, 5));
                int aH = Integer.parseInt(data[3].substring(0, 2));
                int aM = Integer.parseInt(data[3].substring(3, 5));
                timeTable.get(routeNo).add(new Route(sName, dH, dM, data[1], data[2], aH, aM, data[4]));
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
        // System.out.println("Start to parse"); // TEST
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
            // System.out.println("TCP Request: " + fullRequest); // TEST
            if (fullRequest.contains("/?to=")) {
                // Get the name of station from localhost:port/?to=
                String station = fullRequest.substring(5);
                // System.out.println("TCP Request Parsed: " + station); // TEST
                return station;
            }
        }
        return "";
    }

    private int findNextRoute(int routeNo, boolean toRecordNextStation) {
        int currentH = 6;
        int currentM = 0;
        ArrayList<Route> thisRoute = timeTable.get(routeNo);
        // int checkI = -1;
        // FIND THE index from currentH, then check down from this one
        for (int i = 0; i < thisRoute.size(); i++) {
            Route checkR = thisRoute.get(i);
            if (checkR.departH < currentH)
                continue;
            if (checkR.departH == currentH && checkR.departM < currentM)
                continue;
            if (toRecordNextStation && !checkR.destination.equals(routeNext.get(routeNo)))
                continue;
            if (!toRecordNextStation && checkR.destination.equals(routeNext.get(routeNo)))
                continue;
            return i;
        }
        return -1;
    }

    // 0TotalRoute, 1ArrivalH, 2ArrivalM, 3Route, 4This Dest Name, 5Dest, 6Dept,
    // 7DepartH, 8M,
    // 9Platform, 10LastPort, 11LastStopName
    // Total String = 1+ (tR+1)*9+2, (tR+1)*9+3
    // 11/19/27
    private String generateMessage(int totalRoute, int aH, int aM, String rN, String tdN, String dN, int dH, int dM,
            String pN) {
        StringBuffer message = new StringBuffer("");
        if (totalRoute == 0) {
            message.append("0,");
            message.append(aH + "," + aM + "," + rN + "," + tdN + "," + dN + "," + sName + "," + dH + "," + dM + ","
                    + pN + "," + udpPort + "," + sName);
        } else if (totalRoute == 1) {

        }
        return message.toString();
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

                        // System.out.println("TCP: New Connection"); // TEST

                        String request = parseRequest(input);
                        System.out.println("TCP Station Name: " + request); // TEST

                        if (request.length() > 0) {

                            // Generate Messages
                            ArrayList<String> msgs = new ArrayList<>();
                            for (int i = 0; i < routeName.size(); i++) {
                                int thisRouteNo = findNextRoute(i, true);
                                int thisRouteNo2 = -1;
                                if (!routeTerminal.get(i))
                                    thisRouteNo2 = findNextRoute(i, false);
                                // if (thisRouteNo == -1 && thisRouteNo2 == -1)
                                // continue;

                                if (thisRouteNo != -1) {
                                    Route cR = timeTable.get(i).get(thisRouteNo);
                                    msgs.add(generateMessage(0, cR.arriveH, cR.arriveM, cR.name, cR.destination,
                                            request, cR.departH, cR.departM, cR.platform));
                                }
                                if (thisRouteNo2 != -1) {
                                    Route cR = timeTable.get(i).get(thisRouteNo2);
                                    msgs.add(generateMessage(0, cR.arriveH, cR.arriveM, cR.name, cR.destination,
                                            request, cR.departH, cR.departM, cR.platform));
                                }
                            }
                            // System.out.println(msgs.toString()); // TEST

                            String answer;
                            if (msgs.size() == 0) { // Means no more routes
                                answer = "No more available trip today!";
                            } else {
                                // SEND UDP MESSAGE // TEST
                                byte[] buf = new byte[1024];
                                DatagramSocket ds = new DatagramSocket(9000);
                                DatagramPacket dp_receive = new DatagramPacket(buf, 1024);
                                InetAddress loc = InetAddress.getLocalHost();

                                for (int i = 0; i < adjPort.size(); i++) {
                                    for (int j = 0; j < msgs.size(); j++) {
                                        String str_send = msgs.get(j);
                                        DatagramPacket dp_send = new DatagramPacket(str_send.getBytes(),
                                                str_send.length(), loc, adjPort.get(i));
                                        ds.send(dp_send);
                                        System.out.println(
                                                "TCP-UDP: Send Message: " + str_send + " to " + adjPort.get(i)); // TEST
                                    }
                                }
                                boolean receivedResponse = false; // Skip // TEST
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
                                String reply = new String(dp_receive.getData(), 0, dp_receive.getLength());
                                System.out.println("TCP-UDP: Received data from destnation"); // TEST
                                String str_receive = new String(dp_receive.getData(), 0, dp_receive.getLength())
                                        + " from " + dp_receive.getPort();
                                System.out.println(str_receive);
                                dp_receive.setLength(1024);

                                ds.close();

                                // Generate Answer to TCP
                                // 0,6,31,busA_B,JunctionB,JunctionB,TerminalA,6,1,stopA,2001,JunctionB from
                                // 2001
                                String[] data = reply.split(delimeter);

                                StringBuffer finalResponse = new StringBuffer();
                                finalResponse.append("<h1>Total Route: " + 1 + "</h1>\n");
                                finalResponse.append("<h2>The Fastest Route: " + 1 + "</h2>\n");
                                for (int i = 0; i < 1; i++) {
                                    finalResponse.append("<p>Route: " + (i + 1) + ", Transfer " + 0 + " time(s): <br>"
                                            + data[7] + ":" + data[8] + " at " + data[6] + " , " + data[9] + " board "
                                            + data[3] + ", arrive " + data[4] + " at " + data[1] + ":" + data[2]
                                            + ".</p>\n");
                                }
                                answer = finalResponse.toString();
                            }
                            // HTTP Response
                            String response = "HTTP/1.1 200 ok \n" + "Content-Type: text/html\n" + "Content-Length: "
                                    + answer.length() + "\n\n" + answer;
                            output.write(response.getBytes());
                            output.flush();
                            output.close();
                        } else {
                            // HTTP 404 Response
                            String response = "HTTP/1.1 404 Not Found \n" + "Content-Type: text/html\n"
                                    + "Content-Length: 29" + "\n\n" + "<h1>Request Not Correct!</h1>";
                            output.write(response.getBytes());
                            output.flush();
                            output.close();
                            // System.out.println("TCP: 404 Responsed"); // TEST
                        }

                        // System.out.println("TCP: Close Connection"); // TEST
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
                    dp_receive.setLength(1024);
                    ds.receive(dp_receive);
                    System.out.println("UDP: Received data:");
                    String msg = new String(dp_receive.getData(), 0, dp_receive.getLength());
                    {
                        String str_receive = new String(dp_receive.getData(), 0, dp_receive.getLength()) + " from "
                                + dp_receive.getAddress().getHostAddress() + ":" + dp_receive.getPort();
                        System.out.println(str_receive);
                    } // TEST
                    String[] data = msg.split(delimeter);
                    // Check if this message send for me
                    if (!data[4].equals(sName))
                        continue;
                    // Check if this is the destination
                    int msgDestnation = -1;
                    String msgReply;
                    // if (data[5].equals(sName)) {
                    // Send message back to departs port
                    // All the same but the last two different
                    StringBuffer msgR = new StringBuffer("");
                    for (int i = 0; i < data.length - 2; i++) {
                        msgR.append(data[i] + ",");
                    }
                    msgR.append(udpPort + "," + sName);
                    msgReply = msgR.toString();
                    System.out.println("UDP: Reply msg: " + msgReply);

                    // } else {

                    // }

                    DatagramPacket dp_send = new DatagramPacket(msgReply.getBytes(), msgReply.length(),
                            dp_receive.getAddress(), 9000);
                    ds.send(dp_send);
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
    }
}
