package fr.pludov.scopeexpress.openphd;

import java.io.*;
import java.net.*;
import java.nio.charset.*;
import java.util.*;

import javax.swing.*;

import org.apache.log4j.*;

import com.google.gson.*;

public class OpenPhdConnection {
	public static final Logger logger = Logger.getLogger(OpenPhdConnection.class);
	
	final OpenPhdDevice device;
	
	Socket socket;
	boolean closed;
	
	int nextOrderId;
	JsonStreamParser reader;
	
	// Les requetes à envoyer
	final List<OpenPhdQuery> pendingQueries;
	// Les requetes en attente de réponse
	final List<OpenPhdQuery> unrepliedQueries;
	
	public OpenPhdConnection(OpenPhdDevice device) {
		this.device = device;
		pendingQueries = new LinkedList<>();
		unrepliedQueries = new LinkedList<>();
	}
	
	void connect() throws IOException
	{
		socket = new Socket();
		socket.setReuseAddress(true);
		socket.connect(new InetSocketAddress("127.0.0.1", 4400), 30);
	}
	
	void proceed() throws UnsupportedEncodingException, IOException
	{
		new Thread("Phd writer") {
			@Override
			public void run() {
				try {
					while(true) write();
				} catch(Throwable e) {
					if (!device.normalClose) {
						logger.warn("Writer shutdown", e);
					}
					device.internalClose();
				}
			}
		}.start();
		
		reader = new JsonStreamParser(new InputStreamReader(socket.getInputStream(), "UTF-8"));
		
		
		try {
			while(read()) {}
		} catch(Throwable e) {
			logger.warn("Phd closed", e);
			if (!device.normalClose) {
				logger.warn("Reader shutdown", e);
			}
			device.internalClose();
		}

	}
	
	void close()
	{
		final List<OpenPhdQuery> toAbort = new ArrayList<>();
		synchronized(this) {
			if (closed) {
				return;
			}
			closed = true;
			notifyAll();
			toAbort.addAll(pendingQueries);
			toAbort.addAll(unrepliedQueries);
		}
		try {
			socket.close();
		} catch (IOException e) {
			logger.info("Close failed", e);
		}
		if (!toAbort.isEmpty()) {
			SwingUtilities.invokeLater(new Runnable() {
				@Override
				public void run() {
					for(OpenPhdQuery rq : toAbort) {
						try {
							rq.onFailure();
						} catch(Throwable t) {
							logger.error("Failed onfailure", t);
						}
					}
				}
			});
		}
	}
	
	void write() throws IOException, InterruptedException {
		OutputStream os = socket.getOutputStream();
		
		OpenPhdQuery toSend;
		synchronized(this)
		{
			while(pendingQueries.isEmpty()) {
				if (closed) {
					throw new IOException("phd connection closed");
				}
				wait();
			}
			
			toSend = pendingQueries.remove(0);
			unrepliedQueries.add(toSend);
		}
		os.write(toSend.getJson().getBytes(StandardCharsets.UTF_8));
		os.write('\n');
		os.flush();
	}
	
	boolean read() throws IOException {
		JsonElement message = reader.next();	
		if (message == null) {
			return false;
		}
		logger.info("PHD sent: " + message);
		if (!(message instanceof JsonObject)) {
			throw new IOException("Invalid json received:" + message);
		}
		final JsonObject messageMap = (JsonObject)message;
		
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				syncDispatchEvent(messageMap);
			}
		});
		return true;
	}
	
	/** Trouve et retire une requete en attnte de reponse */
	synchronized OpenPhdQuery findUnrepliedQuery(int id)
	{
		for(Iterator<OpenPhdQuery> it = unrepliedQueries.iterator(); it.hasNext();)
		{
			OpenPhdQuery rp = it.next();
			if (rp.uid == id) {
				it.remove();
				return rp;
			}
		}
		return null;
	}
	

	synchronized boolean cancelQuery(OpenPhdQuery query) {
		if (pendingQueries.remove(query)) {
			return true;
		}
		unrepliedQueries.remove(query);
		return false;
	}

	void syncDispatchEvent(JsonObject messageEvent)
	{
		if (messageEvent.has("jsonrpc")) {
			logger.info("Phd response:" + messageEvent);
			JsonElement e;
			int id = (messageEvent.get("id").getAsBigDecimal()).intValue();
			
			OpenPhdQuery rq = findUnrepliedQuery(id);
			if (rq != null) {
				rq.onReply(messageEvent);
			} else {
				logger.info("Received info for unknown request: " + id);
			}
			return;
		} else {
			logger.info("Phd event:" + messageEvent);
			JsonElement eventType = messageEvent.get("Event");
			if (eventType == null) {
				logger.warn("Malformed event (missing event)");
			}
			device.listeners.getTarget().onEvent(eventType.getAsString(), messageEvent);
		}
	}
	
	void sendQuery(OpenPhdQuery query)
	{
		boolean failed;
		synchronized(this) {
			if (closed) {
				failed = true;
			} else {
				failed = false;
				this.pendingQueries.add(query);
				notifyAll();
			}
		}
		if (failed) {
			query.onFailure();
		}
	}
//	
//	public static void main(String[] args) {
//		OpenPhdConnection ops = new OpenPhdConnection();
//		
//		try {
//			ops.connect();
//		} catch (IOException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//
//	
//		RpcQuery query = new RpcQuery(ops) {
//			@Override
//			void onFailure() {
//				logger.warn("failure");
//			}
//			@Override
//			void onReply(Map<?, ?> message) {
//				logger.info("Reply:" + message);	
//			}
//		};
//		query.put("method", "set_exposure");
//		query.put("params", new Object[]{2000});
//		query.send();
//	}

}
