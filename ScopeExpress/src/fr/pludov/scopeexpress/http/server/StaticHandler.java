package fr.pludov.scopeexpress.http.server;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

public class StaticHandler implements HttpHandler {

	void doHandle(HttpExchange exchange) throws ServerException, IOException
	{
		
		String requestMethod = exchange.getRequestMethod();
		if (!requestMethod.equalsIgnoreCase("GET")) {
			throw new ServerException("only get supported");
		}
		
		URI wanted = exchange.getRequestURI();
    	String path = wanted.getPath();
    	if (path.contains(":") || path.contains("/.") || path.contains("<") || path.contains(">"))
    	{
    		throw new ServerException("invalid path");
    	}
    	
    	if (path.endsWith("/")) {
    		path = path + "index.html";
    	}
    
    	// Consume request body.
        InputStream is = exchange.getRequestBody();
        while (is.read() != -1) {
          is.skip(0x10000);
        }
        is.close();
        
        
        byte [] file;
        try(InputStream stream = StaticHandler.class.getResourceAsStream("/fr/pludov/scopeexpress/http/webroot" + path))
        {
	        if (stream == null) {
	        	throw new ServerException(404, "Not found");
	        }
	        ByteArrayOutputStream baos = new ByteArrayOutputStream();
	        byte [] buffer = new byte[4096];
	        int readed;
	        while((readed = stream.read(buffer)) > 0) {
	        	baos.write(buffer, 0, readed);
	        }
	        file = baos.toByteArray();
        }	    	
        
        Headers h = exchange.getResponseHeaders();
        if (path.endsWith(".html")) {
        	h.set("Content-Type", "text/html; charset=iso-8859-1");
        } else if (path.endsWith(".js")) {
        	h.set("Content-Type", "application/javascript; charset=iso-8859-1");
        } else if (path.endsWith(".png")) {
        	h.set("Content-Type", "image/png");
        }
        
        exchange.sendResponseHeaders(200, file.length);
        try(OutputStream os = exchange.getResponseBody()) {
        	os.write(file, 0, file.length);
        }
	}
	
	@Override
	public void handle(HttpExchange exchange) throws IOException {
		try {
			doHandle(exchange);
		} catch(ServerException e) {
			exchange.sendResponseHeaders(e.statusCode, 0);
		} finally {
			exchange.close();
		}
//		String requestMethod = exchange.getRequestMethod();
//	    if (requestMethod.equalsIgnoreCase("GET")) {
//	    	
//	    	
//	      Headers responseHeaders = exchange.getResponseHeaders();
//	      responseHeaders.set("Content-Type", "text/plain");
//	      exchange.sendResponseHeaders(200, 0);
//
//	      OutputStream responseBody = exchange.getResponseBody();
//	      Headers requestHeaders = exchange.getRequestHeaders();
//	      Set<String> keySet = requestHeaders.keySet();
//	      Iterator<String> iter = keySet.iterator();
//	      while (iter.hasNext()) {
//	        String key = iter.next();
//	        List values = requestHeaders.get(key);
//	        String s = key + " = " + values.toString() + "\n";
//	        responseBody.write(s.getBytes());
//	      }
//	      responseBody.close();
//	    }

	}

}
