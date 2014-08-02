package fr.pludov.external.apt;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.charset.Charset;


/**
 * https://groups.yahoo.com/neo/groups/Astroplace/conversations/topics/2223
 */
public class AptComm {

	Socket socket;
	InputStream readFrom;
	OutputStream writeTo;
	
	AptComm()
	{
		this.socket = null;
		this.readFrom = null;
		this.writeTo = null;
	}

	void ensureConnected() throws UnknownHostException, IOException
	{
		if (socket != null && socket.isClosed()) {
			try {
				this.socket.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
			this.socket = null;
			this.readFrom = null;
			this.writeTo = null;
		}
		if (socket == null) {
			Socket s = new Socket();
			s.setKeepAlive(true);
			s.setReuseAddress(true);
			s.connect(new InetSocketAddress("127.0.0.1", 21701), 10000);
			socket = s;
			readFrom = s.getInputStream();
			writeTo = s.getOutputStream();
			
		}
	}
	
	public void shoot() throws IOException
	{
		ensureConnected();
		
		writeTo.write("C104".getBytes(Charset.forName("ASCII")));
		StringBuilder resp = new StringBuilder();
		for(int i = 0; i < 3; ++i) {
			resp.append((char)readFrom.read());
		}
		System.out.println(resp.toString());
	}
	
	private static AptComm aptcomm = null;
	
	public static AptComm getInstance()
	{
		if (aptcomm == null) {
			aptcomm = new AptComm();
		}
		return aptcomm;
	}
}
