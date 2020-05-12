import java.net.*;
import java.io.*;

class Server {

	public static void main(String args[]) {
		int myport = 1234; // assume a fixed, known port
		ServerSocket sock;
		Socket newconn;
		try { // attempt to construct and instantiate a ServerSocket

			sock = new ServerSocket(myport);
			System.out.println("Now listening on port " + myport);
			newconn = sock.accept();

		} catch (Exception e) { // handle the many possible errors (see C example)

			System.out.println("Err: " + e);
			System.exit(1);
		}
	}
}
