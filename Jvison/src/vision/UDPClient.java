package vision;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.*;


public class UDPClient {
	private DatagramSocket Socket;
	private InetAddress robrioIPAddress;
	private ByteArrayOutputStream streamOutput;
	private ObjectOutputStream opStream;
	private int VisionPort; 
	 

	public UDPClient( String roboNetworkName, int portNum ) {
		try {
			Socket = new DatagramSocket();
			robrioIPAddress = InetAddress.getByName(roboNetworkName); 
			streamOutput = new ByteArrayOutputStream();
			opStream = new ObjectOutputStream(streamOutput);
			VisionPort = portNum;
		}
		catch(SocketException e){
			e.printStackTrace();
		}
		catch(UnknownHostException e) {
			e.printStackTrace();
		}
		catch(IOException e) {
			e.printStackTrace();
		}
	}
	public void sendVisionPacket(double Heading, double Distance) {
		try {
		VisionObject visionObj = new VisionObject(Heading, Distance);
		
		opStream.writeObject(visionObj);
		//opStream.writeObject("hello world");
		//byte[] data = streamOutput.toByteArray();
		
		byte[] data = streamOutput.toByteArray();
		DatagramPacket sendPacket = new DatagramPacket(data, data.length, robrioIPAddress, VisionPort);
		Socket.send(sendPacket);
		}
		catch(IOException e) {
			e.printStackTrace();
		}
	}
}
