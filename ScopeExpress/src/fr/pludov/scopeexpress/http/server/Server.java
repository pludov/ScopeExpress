package fr.pludov.scopeexpress.http.server;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

import fr.pludov.scopeexpress.focus.Mosaic;





import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

public class Server {
	// Les accès à ça doivent être executé dans le thread swing.
	Mosaic mosaic;
	HttpServer server;
	
	public void start(int port) throws IOException
	{
	    InetSocketAddress addr = new InetSocketAddress(port);
		server = HttpServer.create(addr, 0);
		
		server.createContext("/", new StaticHandler());
		server.createContext("/ajax/status.json", new JsonHandler("/ajax/status.json", this));
	    server.setExecutor(Executors.newCachedThreadPool());
	    server.start();
	}
	
	public void stop()
	{
		if (server != null) {
			server.stop(0);
			server = null;
		}
	}

	public void setMosaic(Mosaic mosaic)
	{
		this.mosaic = mosaic;
		// FIXME: emettre une notification "reset"
	}
	
	
	public Server()
	{
	}
}
