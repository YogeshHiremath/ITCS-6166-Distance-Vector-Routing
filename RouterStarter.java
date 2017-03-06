import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.SocketException;
import java.util.ArrayList;

public class RouterStarter {

	public static void main(String[] args) throws SocketException, ClassNotFoundException {
		//to enable multicasting in mac, NOTE: the groupAddress is unique for all mac machines, you might have to change the group address value to your machines multicasting address
		System.setProperty("java.net.preferIPv4Stack", "true");

		if(args.length < 2) {
	      System.out.println("USAGE: java RouteNode <listen-port> <table-file>");
	      System.exit(0);
	    }
		
		ByteArrayInputStream bInStream;
		ObjectInputStream oInStream;
		MulticastSocket socket = null;
		DatagramSocket outSocket = new DatagramSocket();
		//multicasting address being used, NOTE: the groupAddress is unique for all mac machines, you might have to change the group address value to your machines multicasting address
		InetAddress groupAddress;
		byte[] inBuf = new byte[10240];
		DatagramPacket inPacket = new DatagramPacket(inBuf, inBuf.length);;

		try {
			socket = new MulticastSocket(Integer.parseInt(args[0]));
			//multicasting address being used, NOTE: the groupAddress is unique for all mac machines, you might have to change the group address value to your machines multicasting address
			groupAddress = InetAddress.getByName("224.0.0.251");
			socket.joinGroup(groupAddress);

			//Router object that will be sent
			RoutingTable rObj = new RoutingTable();
			rObj.constructInitialTable(args[1]);
			//starting the sending out process
			new RouterSender(outSocket, rObj, Integer.parseInt(args[0])).start();

			while(true) {
				socket.receive(inPacket);
				byte[] data = inPacket.getData();
				bInStream = new ByteArrayInputStream(data);
				oInStream = new ObjectInputStream(bInStream);
				//read the vector table and update
				RoutingTable objectReceived = (RoutingTable) oInStream.readObject();
				//same router is also in immediate neighbors
				ArrayList<String> neigh = new ArrayList<String>(rObj.getImmediateNeighbors());
				//neigh has all immediate neighbors after removing the router itself
				neigh.remove(rObj.getRouterName());
				//only accept the packets if from immediate neighbors
				if(neigh.contains(objectReceived.getRouterName())) {
					rObj.updateRouterTable(rObj, objectReceived);
				}
			}
		}
		catch (IOException ioe) {
			System.out.println(ioe);
		}
	}
}
