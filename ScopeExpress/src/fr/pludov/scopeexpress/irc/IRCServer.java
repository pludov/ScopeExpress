package fr.pludov.scopeexpress.irc;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import fr.pludov.scopeexpress.notifs.INotifier;
import fr.pludov.scopeexpress.notifs.NotificationChannel;
import fr.pludov.scopeexpress.notifs.NotifierManager;
import fr.pludov.scopeexpress.utils.EndUserException;

public class IRCServer implements INotifier {

	final Map<String, Connection> connectionMap = new HashMap<String, Connection>();
	final Map<String, Channel> channelMap = new HashMap<String, Channel>();
	final String globalServerName;
	final Object mutex = new Object();
	ServerSocket ss;
	Connection bot;
	
	public IRCServer(String globalServerName) {
		this.globalServerName = globalServerName;
		channelMap.put("scope", new Channel(this, "scope"));
		channelMap.put("debug", new Channel(this, "debug"));
	}

	public static void main(String[] args) throws Throwable
	{
		if (args.length == 0)
		{
			System.out.println("Usage: java jw.jircs.Connection <servername>");
			return;
		}
		new IRCServer(args[0]).start();
	}
	
	public void start()
	{
		try {
			ss = new ServerSocket(6667);
			NotifierManager.addNotifier(this);
			bot = new Connection(IRCServer.this, null);
			// Pas besoin de out queue : ça n'interesse personne en fait.
			bot.outQueue = null;
    		bot.username = "bot";
    		bot.nick = "bot";
    		bot.hostname = "scopeexpress";
    		
			new Thread() {
		    	public void run() {
		    		
		    		while(true) {
		    			try {
		    				if (ss.isClosed()) {
		    					return;
		    				}
		    				emit(NotificationChannel.Debug, "Serveur IRC toujours en vie !");

		    				Thread.sleep(15000);
		    			} catch(Exception e) {
		    			}
		    		}
		    	};
		    }.start();
		    
			new Thread() {
				@Override
				public void run() {
					try {
						runServer();
					} catch(Throwable t) {
						new EndUserException("Crash du serveur IRC", t).report(null);
						try {
							ss.close();
							ss = null;
						} catch (IOException e) {
							e.printStackTrace();
						}
					}
				};
			}.start();

		} catch (IOException e) {
			new EndUserException("Problème de démarrage du serveur IRC", e).report(null);
		}
	}
	
	public void close()
	{
		NotifierManager.removeNotifier(this);
		try {
			ss.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		List<Connection> connections;
		synchronized(mutex) {
			connections = new ArrayList<>(connectionMap.values());
		}
		for(Connection c : connections) {
			try {
				c.socket.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	private void runServer() throws Throwable
	{
/*	    */
	    
	    while (true)
	    {
	        Socket s = ss.accept();
	        Connection jircs = new Connection(IRCServer.this, s);
	        Thread thread = new Thread(jircs);
	        thread.start();
	    }
	}

	public static String delimited(String[] items, String delimiter)
	{
	    StringBuffer response = new StringBuffer();
	    boolean first = true;
	    for (String s : items)
	    {
	        if (first)
	            first = false;
	        else
	            response.append(delimiter);
	        response.append(s);
	    }
	    return response.toString();
	}
	
	@Override
	public void emit(NotificationChannel nc, String mlDetails)
	{
		for(String line : mlDetails.split("\n")) {
			if (nc == NotificationChannel.Alert) {
				List<String> pseudos = new ArrayList<>();
		        synchronized (mutex)
		        {
		        	// Trouver tous les salons
		        	// Sur chaque salon, tous les pseudo
		        	// Envoyer un message à chacun!
		        	pseudos.addAll(connectionMap.keySet());
		        }
				
		        for(String pseudo: pseudos) {
		        	try {
						Command.PRIVMSG.run(bot, null, new String[]{pseudo, line});
					} catch (Exception e) {
						e.printStackTrace();
					}
		        }
			} else {
				try {
					Command.PRIVMSG.run(bot, null, new String[]{"#" + nc.getTitle(), line});
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
	}

}
