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
        routeName.clear();
        routeNext.clear();
        routeTerminal.clear();
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

    private int findNextRoute(int h, int m, int routeNo, boolean toRecordNextStation) {
        int currentH = 6;
        int currentM = 0;
        if (h!=0) {
            currentH = h;
            currentM = m;
        }

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
    private String generateMessage(int totalRoute, int aH, int aM, String rN, String tdN, String dN, String departN,
            int dH, int dM, String pN) {
        StringBuffer message = new StringBuffer("");
        if (totalRoute == 0) {
            message.append("0,");
            message.append(aH + "," + aM + "," + rN + "," + tdN + "," + dN + "," + departN + "," + dH + "," + dM + ","
                    + pN + "," + udpPort + "," + sName);
        }
        return message.toString();
    }

    private String genTransMsg(String[] oldMsg, Route route) {
        StringBuffer message = new StringBuffer("");
        int totalRoute = Integer.parseInt(oldMsg[0]) + 1;
        message.append(totalRoute + ",");
        for (int i = 1; i < oldMsg.length - 2; i++)
            message.append(oldMsg[i] + ",");
        message.append(route.arriveH + "," + route.arriveM + "," + route.name + "," + route.destination  +","+ oldMsg[5]
                + "," + sName + "," + route.departH + "," + route.departM + "," + route.platform + "," + sName +","
                + udpPort);
        return message.toString();
    }

    private String genForwardMsg(String[] oldMsg, Route route) {
        StringBuffer message = new StringBuffer("");
        int totalRoute = Integer.parseInt(oldMsg[0]);
        message.append(oldMsg[0] + ",");
        // Copy the previous route info
        for (int i = 1; i < totalRoute * 9 + 1; i++)
            message.append(oldMsg[i] + ",");
        // Generate the new route
        message.append(route.arriveH + "," + route.arriveM + "," + route.name + "," + route.destination + ","
                + oldMsg[5] + "," + oldMsg[6 + totalRoute * 9] + "," + oldMsg[7 + totalRoute * 9] + ","
                + oldMsg[8 + totalRoute * 9] + "," + oldMsg[9 + totalRoute * 9] + "," + sName +","+ udpPort);
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
                            readTT();

                            // Generate Messages
                            ArrayList<String> msgs = new ArrayList<>();
                            for (int i = 0; i < routeName.size(); i++) {
                                int thisRouteNo = findNextRoute(0,0,i, true);
                                int thisRouteNo2 = -1;
                                if (!routeTerminal.get(i))
                                    thisRouteNo2 = findNextRoute(0,0,i, false);
                                // if (thisRouteNo == -1 && thisRouteNo2 == -1)
                                // continue;

                                if (thisRouteNo != -1) {
                                    Route cR = timeTable.get(i).get(thisRouteNo);
                                    msgs.add(generateMessage(0, cR.arriveH, cR.arriveM, cR.name, cR.destination,
                                            request, sName, cR.departH, cR.departM, cR.platform));
                                }
                                if (thisRouteNo2 != -1) {
                                    Route cR = timeTable.get(i).get(thisRouteNo2);
                                    msgs.add(generateMessage(0, cR.arriveH, cR.arriveM, cR.name, cR.destination,
                                            request, sName, cR.departH, cR.departM, cR.platform));
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
                                ds.setSoTimeout(1000);

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
                                // boolean receivedResponse = false; // Skip // TEST
                                boolean timeOut = false;
                                ArrayList<String> finalRoute = new ArrayList<>();
                                // while (!receivedResponse && !timeOut) {
                                while (!timeOut) {
                                    try {
                                        ds.receive(dp_receive);
                                        // if (!dp_receive.getAddress().equals(loc)) {
                                        // throw new IOException("Received packet from an umknown source");
                                        // }
                                        // receivedResponse = true;
                                        finalRoute.add(new String(dp_receive.getData(), 0, dp_receive.getLength()));
                                    } catch (InterruptedIOException e) {
                                        timeOut = true;
                                        // System.out.println("Time out," + (MAXNUM - tries) + " more tries...");
                                    }
                                }
                                if (finalRoute.size() > 0) {
                                    // String reply = new String(dp_receive.getData(), 0, dp_receive.getLength());
                                    System.out.println("TCP-UDP: Received data from destnation"); // TEST
                                    String str_receive = new String(dp_receive.getData(), 0, dp_receive.getLength())
                                            + " from " + dp_receive.getPort();
                                    System.out.println(str_receive);
                                    // Generate Answer to TCP
                                    // 0,6,31,busA_B,JunctionB,JunctionB,TerminalA,6,1,stopA,2001,JunctionB from
                                    // 2001

                                    StringBuffer finalResponse = new StringBuffer();
                                    finalResponse.append("<h1>Total Route: " + finalRoute.size() + "</h1>\n");
                                    ArrayList<Integer> tTime = new ArrayList<>();
                                    for (int i = 0; i < finalRoute.size(); i++) {
                                        String[] data = finalRoute.get(i).split(delimeter);
                                        int transTime = Integer.parseInt(data[0]);
                                        finalResponse.append("<p><br>Route: " + (i + 1) + ", Transfer " + data[0]
                                                + " time(s): <br>");
                                        for (int j = 0; j <= transTime; j++) {
                                            finalResponse.append(data[7 + j * 9] + ":" + data[8 + j * 9] + " at "
                                                    + data[6 + j * 9] + " , " + data[9 + j * 9] + " board "
                                                    + data[3 + j * 9] + ", arrive " + data[4 + j * 9] + " at "
                                                    + data[1 + j * 9] + ":" + data[2 + j * 9] + ".</p>\n");
                                        }
                                        int usedH = Integer.parseInt(data[1 + transTime * 9])
                                                - Integer.parseInt(data[7]);
                                        int usedM = Integer.parseInt(data[2 + transTime * 9])
                                                - Integer.parseInt(data[8]);
                                        tTime.add(usedH * 60 + usedM);
                                    }
                                    int minTime = tTime.get(0);
                                    int minTimeNo = 0;
                                    for (int i = 1; i < tTime.size(); i++) {
                                        if (tTime.get(i) < minTime) {
                                            minTime = tTime.get(i);
                                            minTimeNo = i;
                                        }
                                    }
                                    finalResponse.append("<h2>The Fastest Route: " + minTimeNo + 1 + "</h2>\n");
                                    answer = finalResponse.toString();
                                } else {
                                    answer = "<h1>Can't Find a Route</h1>";
                                }

                                dp_receive.setLength(1024);

                                ds.close();

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
                InetAddress loc = InetAddress.getLocalHost();
                boolean running = true;
                while (running) {
                    dp_receive.setLength(1024);
                    ds.receive(dp_receive);
                    readTT();
                    System.out.println("UDP: Received data:");
                    String msg = new String(dp_receive.getData(), 0, dp_receive.getLength());
                    {
                        String str_receive = new String(dp_receive.getData(), 0, dp_receive.getLength()) + " from "
                                + dp_receive.getAddress().getHostAddress() + ":" + dp_receive.getPort();
                        System.out.println(str_receive);
                    } // TEST
                    String[] data = msg.split(delimeter);
                    // Check if this message send for me
                    int totalTrans = Integer.parseInt(data[0]);
                    String targetStation = data[4 + 9 * totalTrans];
                    String finalDest = data[5 + 9 * totalTrans];
                    int arriveH = Integer.parseInt(data[1 + 9 * totalTrans]);
                    int arriveM = Integer.parseInt(data[2 + 9 * totalTrans]);
                    if (!targetStation.equals(sName)) {
                        System.out.println("UDP: Message not for me");
                        continue;
                    }
                    if (totalTrans>4) {
                        System.out.println("Drop route more than 5 transfer.");
                        continue;
                    }
                    // Check if this is the destination
                    // int msgDestnation = -1;
                    String msgReply;
                    if (finalDest.equals(sName)) {
                        // Send message back to departs port
                        // All the same but the last two different
                        System.out.println("UDP: The Destination reply to source");
                        StringBuffer msgR = new StringBuffer("");
                        for (int i = 0; i < data.length - 2; i++) {
                            msgR.append(data[i] + ",");
                        }
                        msgR.append(udpPort + "," + sName);
                        msgReply = msgR.toString();
                        System.out.println("UDP: Reply msg: " + msgReply);

                        DatagramPacket dp_send = new DatagramPacket(msgReply.getBytes(), msgReply.length(),
                                dp_receive.getAddress(), 9000);
                        ds.send(dp_send);
                        // } else if (routeTerminal.get(routeName.indexOf(data[3]))) {
                        //
                        // continue;
                    } else {
                        // Send msg to the next station of this route
                        // Find the route no
                        System.out.println("UDP: Ready to forward msg");
                        int routeNo = -1;
                        for (int i = 0; i < routeTerminal.size(); i++) {
                            if (routeName.get(i).equals(data[3 + 9 * totalTrans])) {
                                routeNo = i;
                                break;
                            }
                        }
                        boolean forwardable = true;
                        // Check if this is the terminal of this route if the destination is not here
                        if (routeNo == -1
                                || (routeTerminal.get(routeNo)) && routeNext.get(routeNo).equals(data[data.length - 1]))
                            forwardable = false;
                        ArrayList<String> msgs = new ArrayList<>();
                        if (forwardable) {
                            boolean toTheRecordStation = true;
                            if (routeNext.get(routeNo).equals(data[data.length - 1]))
                                toTheRecordStation = false;
                            int nextR = findNextRoute(arriveH,arriveM, routeNo, toTheRecordStation);
                            if (nextR != -1) {
                                Route cR = timeTable.get(routeNo).get(nextR);
                                msgs.add(genForwardMsg(data, cR));
                                // msgs.add(generateMessage(0, cR.arriveH, cR.arriveM, cR.name, cR.destination,
                                // data[5],
                                // data[6], Integer.parseInt(data[7]), Integer.parseInt(data[8]), data[9]));
                            }
                        }
                        // Check if there're transferable routes
                        if (routeName.size() > 1) {
                            System.out.println("UDP: Ready to gen transfer msg");
                            for (int i = 0; i < routeName.size(); i++) {
                                if (i == routeNo)
                                    continue;
                                int thisRouteNo = findNextRoute(arriveH,arriveM, i, true);
                                int thisRouteNo2 = -1;
                                if (!routeTerminal.get(i))
                                    thisRouteNo2 = findNextRoute(arriveH,arriveM, i, false);
                                // if (thisRouteNo == -1 && thisRouteNo2 == -1)
                                // continue;

                                if (thisRouteNo != -1) {
                                    Route cR = timeTable.get(i).get(thisRouteNo);
                                    msgs.add(genTransMsg(data, cR));
                                }
                                if (thisRouteNo2 != -1) {
                                    Route cR = timeTable.get(i).get(thisRouteNo2);
                                    msgs.add(genTransMsg(data, cR));
                                }
                            }
                        }
                        // Forwoad all msgs
                        if (msgs.size() > 0) {
                            for (int i = 0; i < adjPort.size(); i++) {
                                for (int j = 0; j < msgs.size(); j++) {
                                    String str_send = msgs.get(j);
                                    DatagramPacket dp_send = new DatagramPacket(str_send.getBytes(), str_send.length(),
                                            loc, adjPort.get(i));
                                    ds.send(dp_send);
                                    System.out.println("UDP: Send Message: " + str_send + " to " + adjPort.get(i)); // TEST
                                }
                            }
                        }
                    }
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
