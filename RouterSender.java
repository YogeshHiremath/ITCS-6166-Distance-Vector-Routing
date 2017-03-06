import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class RouterSender extends Thread {
	private DatagramSocket outSocket;
	private RoutingTable tableBeingSent;
	private int PORT;
	DatagramPacket outPacket = null;

	public RouterSender(DatagramSocket updateSocket, RoutingTable tableBeingSent, int portNumber) {
		this.outSocket = updateSocket;
		this.tableBeingSent = tableBeingSent;
		this.PORT = portNumber;
	}

	public void run() {
		int updateCount = 0;
		try {
			while(true) {
				//wait 15 seconds before sending out
				Thread.sleep(15000);
				tableBeingSent.checkLinkCostChange();
				//multicasting address being used, NOTE: the groupAddress is unique for all mac machines, you might have to change the group address value to your machines multicasting address
				InetAddress address = InetAddress.getByName("224.0.0.251");
		        ByteArrayOutputStream boutStream = new ByteArrayOutputStream();
		  	    ObjectOutputStream ooutStream = new ObjectOutputStream(boutStream);
		  	    ooutStream.writeObject(tableBeingSent);
	            byte[] buff = boutStream.toByteArray();
	            outPacket = new DatagramPacket(buff, buff.length, address, PORT);
		        outSocket.send(outPacket);
		        System.out.println("Output Number "+ ++updateCount+":");
		        tableBeingSent.printTable(tableBeingSent.getTable());
		        System.out.println();
		        try {
		          Thread.sleep(500);
		        } catch (InterruptedException ie) {
		        }
			}
		}
		catch (IOException ioe) {
		      System.out.println(ioe);
		    } catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
