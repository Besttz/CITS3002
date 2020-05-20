import sys

sName = sys.argv[1]
tcpPort = sys.argv[2]
udpPort = sys.argv[3]
adjPort = sys.argv[4:]
timeTable = []
routeName = []
routeTerminal = []
routeNext = []
delimeter = ','


class Route:
    def __init__(self, a1, a2, a3, a4, a5, a6, a7, a8):
        fromS = a1
        departH = a2
        departM = a3
        name = a4
        platform = a5
        arriveH = a6
        arriveM = a7
        destination = a8


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
        data = line.split(delimeter)
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
        newRoute = Route(sName, (int)(data[0][0:2]), (int)(
            data[0][3:5]), data[1], data[2], (int)(data[3][0:2]), (int)(data[3][3:5]), data[4])
        timeTable[routeNo].append(newRoute)

    f.close()


def parseRequest(request):
    # request = ""
    begin = request.index(' ')
    end = request.index(' ', begin+1)
    if begin != -1 and end > begin:
            fullRequest = request[begin + 1:end]
            if fullRequest.find("/?to=") != -1:
                station = fullRequest[5:]
                return station
    return ""

readTT()
print(timeTable)
re = input("Enter URI") #TEST
print(parseRequest(re)) #TEST

