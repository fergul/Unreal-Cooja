import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Observable;
import java.util.Observer;

import org.apache.log4j.Logger;
import UnrealCoojaMsg.Message;
import UnrealCoojaMsg.MsgType;
import com.google.flatbuffers.*;

import org.contikios.cooja.ClassDescription;

import org.contikios.cooja.RadioConnection;
import org.contikios.cooja.RadioMedium;
import org.contikios.cooja.interfaces.Radio;

/* Radio Event Observer
 *
 * A specialised event observer for a mote's radio interface events
 */
@ClassDescription("Radio Medium Event Observer")
public class RadioMediumEventObserver implements Observer {
	private byte NETWORK_PKT = 1;
	private RadioMedium network;
	protected CoojaEventObserver parent;
	protected static Logger logger = Logger.getLogger(InterfaceEventObserver.class);
	DatagramSocket clientSocket;
	InetAddress ipAddress;
	int port;
	DatagramPacket sendPacket;

	public RadioMediumEventObserver(CoojaEventObserver parent, RadioMedium network, InetAddress clientIPAddr, int clientPort){
		this.network = network;
		this.parent = parent;
		this.port = clientPort;
		try {
			clientSocket = new DatagramSocket();
			ipAddress = clientIPAddr;
		} catch (Exception e) {
			logger.error("RMEO>> " + e.getMessage());
		}
		this.network.addRadioTransmissionObserver(this);
			logger.info("Created radio medium observer");
	}

	@Override
	public void update(Observable obs, Object obj) {
//		parent.radioMediumEventHandler(network.getLastConnection());
		RadioConnection conn = network.getLastConnection();
		if (conn == null) return;
		//logger.info(conn);
		Radio[] dests = conn.getDestinations();
		if (dests.length == 0) return;
		FlatBufferBuilder builder = new FlatBufferBuilder(1024);
		int[] dsts = new int[dests.length];
		for (int i = 0; i < dests.length; i++) {
			dsts[i] = (byte) (dests[i].getMote().getID() - 1);
		}
		int rcvd = Message.createRcvdVector(builder, dsts);
    Message.startMessage(builder);
		Message.addType(builder, MsgType.RADIO);
		Message.addId(builder, conn.getSource().getMote().getID() - 1);
		Message.addRcvd(builder, rcvd);
		int msg = Message.endMessage(builder);
    Message.finishMessageBuffer(builder, msg);
    byte[] data = builder.sizedByteArray();
		java.nio.ByteBuffer buf = java.nio.ByteBuffer.wrap(data);
// Get an accessor to the root object inside the buffer.
		Message monster = Message.getRootAsMessage(buf);
		logger.info("MESSAGE:" + monster.type());
		try {
			sendPacket = new DatagramPacket(data, data.length, ipAddress, port);
			clientSocket.send(sendPacket);
		} catch (Exception e) {
			logger.info(e.getMessage());
		}
	}

	public void deleteObserver() {
		network.deleteRadioTransmissionObserver(this);
	}
}
