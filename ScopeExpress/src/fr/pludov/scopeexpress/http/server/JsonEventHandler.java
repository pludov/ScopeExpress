package fr.pludov.scopeexpress.http.server;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.swing.SwingUtilities;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import fr.pludov.io.CameraFrameMetadata;
import fr.pludov.scopeexpress.focus.Image;
import fr.pludov.scopeexpress.focus.MosaicImageParameter;
import fr.pludov.scopeexpress.focus.StarOccurence;
import fr.pludov.scopeexpress.http.server.jsonbean.EventData;
import fr.pludov.scopeexpress.http.server.jsonbean.EventRequest;
import fr.pludov.scopeexpress.http.server.jsonbean.EventResponse;
import fr.pludov.scopeexpress.http.server.jsonbean.GlobalStatus;
import fr.pludov.scopeexpress.http.server.jsonbean.ImageStatus;
import fr.pludov.scopeexpress.supervision.Event;
import fr.pludov.scopeexpress.supervision.EventSource;

public class JsonEventHandler implements HttpHandler {
	final String path;
	final Server server;
	
	JsonEventHandler(String path, Server server)
	{
		this.path = path;
		this.server = server;
	}
	
	Object getResult() throws ServerException
	{
		GlobalStatus gs = new GlobalStatus();
		gs.images = new ArrayList<>();
		
		if (server.mosaic != null) {
			
			for(Image image : server.mosaic.getImages())
			{
				ImageStatus is = new ImageStatus();
				is.name = image.getPath().getName();
				
				CameraFrameMetadata metadata = image.getMetadata();
				
				Long epoch = metadata.getStartMsEpoch();
				is.date = epoch != null ? epoch.doubleValue() : null;
				is.duration = metadata.getDuration();
				is.temp = metadata.getCcdTemp();
				
				MosaicImageParameter mip = server.mosaic.getMosaicImageParameter(image);
				
				is.starSearched = mip.getStarDetectionStatus() == null ? false : true;
				if (is.starSearched) {
					List<StarOccurence> starOccurences = server.mosaic.getStarOccurences(image);
					is.starCount = starOccurences.size();
					
					if (starOccurences.size() > 0)
					{
						double fwhmSum = 0;
						int count = 0;
						for(StarOccurence sc : starOccurences)
						{
							if (sc.isSaturationDetected()) continue;
							if (!sc.isAnalyseDone()) continue;
							if (sc.getStar().isExcludeFromStat()) continue;
							fwhmSum += sc.getFwhm();
							count++;
						}
						if (count == 0) {
							is.fwhm = null;
						} else {
							is.fwhm = fwhmSum / count;
						}
					}
				}
				
				
				
				gs.images.add(is);
			}
				
		}
		
		return gs;
	}
	
	class Producer implements Runnable {
		Object result;
		ServerException exception;
		@Override
		public void run() {
			try {
				result = getResult();
			} catch (ServerException e) {
				exception = e;
			}
		}
		
	}
	
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
    	
    	if (!path.equals(this.path)) {
    		throw new ServerException(404, "Not found");
    	}
    	// Consume request body.
    	Gson gson = new GsonBuilder().setPrettyPrinting().create();
        /*InputStream is = exchange.getRequestBody();
        
        EventRequest request = gson.fromJson(new InputStreamReader(is, StandardCharsets.UTF_8), EventRequest.class);
        is.close();*/
        
        EventSource es = server.application.getSupervisor().getSourceById("phd.guiding");
        EventResponse er = new EventResponse();
        er.setEvents(new HashMap<String, List<EventData>>());
        List<EventData> eventDataList = new ArrayList<EventData>();
        for(Event e : es.getEvents()) {
        	eventDataList.add(new EventData(e));
        }
        
        er.getEvents().put("phd.guiding", eventDataList);
        
        String resultString = gson.toJson(er);
		byte [] resultBytes = resultString.getBytes(StandardCharsets.UTF_8);
		
		Headers h = exchange.getResponseHeaders();
		h.set("Content-Type", "application/json; charset=utf-8");
        
        
        exchange.sendResponseHeaders(200, resultBytes.length);
        try(OutputStream os = exchange.getResponseBody()) {
        	os.write(resultBytes, 0, resultBytes.length);
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
