import sys
import threading
import socket

sName = sys.argv[1]
tcpPort = sys.argv[2]
udpPort = sys.argv[3]
adjPort = sys.argv[4:]
timeTable = []
routeName = []
routeTerminal = []
routeNext = []
de = ','


class Route:

    def __init__(self, a1, a2, a3, a4, a5, a6, a7, a8):
        self.fromS = a1
        self.departH = a2
        self.departM = a3
        self.name = a4
        self.platform = a5
        self.arriveH = a6
        self.arriveM = a7
        self.destination = a8


def readTT():
    timeTable.clear()
    routeName.clear()
    routeNext.clear()
    routeTerminal.clear()
    f = open("tt-" + sName)
    lines = f.readlines()
    firstLine = True
    for line in lines:
        if firstLine:
            firstLine = False
            continue
        routeNo = -1
        data = line.split(de)
        # for rName in routeName:
        for i in range(len(routeName)):
            if routeName[i] == data[1]:
                routeNo = i
        if routeNo == -1:  # Not Added Yet
            routeName.append(data[1])
            timeTable.append([])
            routeNo = len(routeTerminal)
            routeNext.append(data[4])
            routeTerminal.append(True)
        if routeTerminal[routeNo] and not routeNext[routeNo] == data[4]:
            routeTerminal[routeNo] = False
        # dH = (int)(data[0][0:2])
        newRoute = Route(sName, (int)(data[0][0:2]), (int)(data[0][3:5]),
                         data[1], data[2], (int)(data[3][0:2]),
                         (int)(data[3][3:5]), data[4])
        timeTable[routeNo].append(newRoute)

    f.close()


def parseRequest(request):
    # request = ""
    begin = request.find(' ')
    end = request.find(' ', begin + 1)
    if begin != -1 and end > begin:
        fullRequest = request[begin + 1:end]
        if fullRequest.find("/?to=") != -1:
            station = fullRequest[5:]
            return station
    return ""


def findNextRoute(h, m, routeNo, toRecordNextStation):
    cH = 6
    cM = 0
    if h != 0:
        cH = h
        cM = m
    cRoute = timeTable[routeNo]
    for i in range(len(cRoute)):
        checkR = cRoute[i]
        if checkR.departH < cH:
            continue
        if checkR.departH == cH and checkR.departM < cM:
            continue
        if toRecordNextStation and not checkR.destination == routeNext[routeNo]:
            continue
        if not toRecordNextStation and checkR.destination == routeNext[routeNo]:
            continue
        return i
    return -1


def genMsg(r, dest):
    msg = "0,"+(str)(r.arriveH)+de+(str)(r.arriveM)+de+(str)(r.name)+de+(str)(r.destination)+de+dest + \
        de+(str)(r.fromS)+de+(str)(r.departH)+de+(str)(r.departM)+de+(str)(r.platform)+de+(str)(udpPort)+de+sName
    return msg


def genTransMsg(oldMsg, r):
    totalRoute = (int)(oldMsg[0]) + 1
    msg = '' + str(totalRoute) + de
    for i in range(1, len(oldMsg) - 2):
        msg += oldMsg[i] + de
    msg += (str)(r.arriveH)+de+(str)(r.arriveM)+de+(str)(r.name)+de+(str)(r.destination)+de+oldMsg[5] \
        + de + sName + de+(str)(r.departH)+de+(str)(r.departM)+de+r.platform+de+(str)(udpPort)+de+sName
    return msg


def genForwardMsg(oldMsg, r):
    totalRoute = (int)(oldMsg[0])
    msg = ''
    for i in range(totalRoute * 9 + 1):
        msg += oldMsg[i] + de
    msg += (str)(r.arriveH)+de+(str)(r.arriveM)+de+(str)(r.name)+de+(str)(r.destination)+de+oldMsg[5] \
        + de + oldMsg[6 + totalRoute * 9] + de+oldMsg[7 + totalRoute * 9]+de + \
        oldMsg[8 + totalRoute * 9]+de + \
        oldMsg[9 + totalRoute * 9]+de+(str)(udpPort)+de+sName
    return msg


def runTCP():
    serversocket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    # host = socket.gethostname()
    serversocket.bind(("0.0.0.0", (int)(tcpPort)))
    serversocket.listen(5)
    # serversocket.settimeout(5)
    print(("Start to listen TCP at " + tcpPort))
    while True:
        try:
            clientsocket, addr = serversocket.accept()
            clientsocket.settimeout(2)
            recvData = clientsocket.recv(1024)
            print("TCP Receive from: %s" % str(addr))
            # print("TCP Full Request: " + str(recvData))
            request = parseRequest(bytes.decode(recvData))
            print("TCP Station Name: " + request)
            if len(request) > 0:
                readTT()
                msg = []
                for i in range(len(routeName)):
                    tRouteNo = findNextRoute(0, 0, i, True)
                    tRouteNo2 = -1
                    if not routeTerminal[i]:
                        tRouteNo2 = findNextRoute(0, 0, i, False)
                    if tRouteNo != -1:
                        msg.append(genMsg(timeTable[i][tRouteNo], request))
                    if tRouteNo2 != -1:
                        msg.append(genMsg(timeTable[i][tRouteNo2], request))
                answer = ''
                if len(msg) == 0:
                    answer = "No more available trip today!"
                else:
                    udpTsocket = socket.socket(
                        socket.AF_INET, socket.SOCK_DGRAM)
                    udpTsocket.bind(("0.0.0.0", 9000))
                    for adj in adjPort:
                        for ms in msg:
                            udpTsocket.sendto(str.encode(ms), ('localhost', (int)(adj)))
                            print("T-UDP: Send "+ms+" to "+adj)
                    timeOut = False
                    finalR = []
                    udpTsocket.settimeout(2)
                    while not timeOut:
                        try:
                            data, addr = udpTsocket.recvfrom(1024)
                            finalR.append(bytes.decode(data))
                            print("T-UDP: Received: "+ finalR[-1])
                        except socket.timeout:
                            timeOut = True
                    if len(finalR) > 0:
                        # print("T-UDP: Received data from destnation")
                        answer += "<h1>Total Route: " + (str)(len(finalR)) + "</h1>\n"
                        tTime = []
                        for i in range(len(finalR)):
                            data = finalR[i].split(de)
                            transTime = (int)(data[0])
                            answer += "<p><br>Route: " + \
                                (str)(i + 1) + ", Transfer " + \
                                data[0] + " time(s): <br>"
                            for j in range(transTime+1):
                                answer += data[7 + j * 9] + ":" + data[8 + j * 9] + " at " + data[6 + j * 9] + " , " + data[9 + j * 9] + " board " + \
                                    data[3 + j * 9] + ", arrive " + data[4 + j * 9] + " at " + \
                                    data[1 + j * 9] + ":" + \
                                    data[2 + j * 9] + ".</p>\n"
                            usedH = (int)(
                                data[1 + transTime * 9])-(int)(data[7])
                            usedM = (int)(
                                data[2 + transTime * 9])-(int)(data[8])
                            tTime.append(60*usedH+usedM)
                        minTime = min(tTime)
                        minTimeNo = tTime.index(minTime)
                        answer += "<h2>The Fastest Route: " + \
                            (str)(minTimeNo + 1) + "</h2>\n"
                    else:
                        answer = "<h1>Can't Find a Route</h1>"
                    udpTsocket.close
                response = "HTTP/1.1 200 ok \n" + "Content-Type: text/html\n" + "Content-Length: " \
                    + (str)(len(answer)) + "\n\n" + answer
            else:
                response = "HTTP/1.1 404 Not Found \n" + "Content-Type: text/html\n" + \
                    "Content-Length: 29" + "\n\n" + "<h1>Request Not Correct!</h1>"
            clientsocket.send(response.encode('utf-8'))
            clientsocket.close()
        except socket.timeout:
            # err = e.args[0]
            # continue
            continue

    clientsocket.close()


def runUDP():
    udpsocket = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
    udpsocket.bind(("0.0.0.0", (int)(udpPort)))
    print("Start to listen UDP at " + udpPort)
    while True:
        # print('wating for message...')
        recvdata, addr = udpsocket.recvfrom(1024)
        readTT()
        print("UDP: Received: " + bytes.decode(recvdata))
        data = bytes.decode(recvdata).split(de)
        totalTrans = (int)(data[0])
        routeN = data[3 + 9 * totalTrans]
        targetStation = data[4 + 9 * totalTrans]
        finalDest = data[5 + 9 * totalTrans]
        arriveH = (int)(data[1 + 9 * totalTrans])
        arriveM = (int)(data[2 + 9 * totalTrans])
        if not targetStation == sName or totalTrans > 4:
            continue
        if finalDest == sName:
            msg = ""
            for i in range(len(data)-2):
                msg += data[i]+de
            msg += udpPort+de+sName
            print("UDP: Reply : " + msg)
            udpsocket.sendto(str.encode(msg), ('localhost', 9000))
        else:
            print("UDP: Ready to forward")
            routeNo = -1
            for routename in routeName:
                if routename == routeN:
                    routeNo = routeName.index(routename)
                    break
            forwardable = True
            if routeNo == -1 or (routeTerminal[routeNo] and routeNext[routeNo] == data[-1]):
                forwardable = False
            msgs = []
            if forwardable:
                toTheRecordStation = True
                if routeNext[routeNo] == data[-1]:
                    toTheRecordStation = False
                nextR = findNextRoute(
                    arriveH, arriveM, routeNo, toTheRecordStation)
                if nextR != -1:
                    cR = timeTable[routeNo][nextR]
                    msgs.append(genForwardMsg(data, cR))
            if len(routeName) > 1:
                print("UDP: Ready to gen transfer")
                for i in range(len(routeName)):
                    if i == routeNo:
                        continue
                    tRouteNo = findNextRoute(arriveH, arriveM, i, True)
                    tRouteNo2 = -1
                    if not routeTerminal[i]:
                        tRouteNo2 = findNextRoute(arriveH, arriveM, i, False)
                    if tRouteNo != -1:
                        cR = timeTable[i][tRouteNo]
                        msgs.append(genTransMsg(data, cR))
                    if tRouteNo2 != -1:
                        cR = timeTable[i][tRouteNo2]
                        msgs.append(genTransMsg(data, cR))
            if len(msgs) > 0:
                for adj in adjPort:
                    for ms in msgs:
                        udpsocket.sendto(str.encode(ms), ('localhost', (int)(adj)))
                        print("UDP: Send "+ms+" to "+adj)

        # udpsocket.sendto('[%s] %s'%(ctime(),data),addr)
        # print('...received from and retuned to:', addr)
    udpsocket.close()


readTT()
tcp = threading.Thread(target=runTCP, name='tcpThread')
udp = threading.Thread(target=runUDP, name='udpThread')
tcp.start()
udp.start()
tcp.join()
udp.join()
