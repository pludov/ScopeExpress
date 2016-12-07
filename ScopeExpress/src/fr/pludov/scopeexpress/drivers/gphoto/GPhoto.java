package fr.pludov.scopeexpress.drivers.gphoto;

import java.io.*;
import java.lang.ProcessBuilder.*;
import java.util.*;
import java.util.regex.*;

import fr.pludov.scopeexpress.camera.*;

public class GPhoto {
	
	String gphotoDir = "C:\\tmp\\gphoto\\gphoto-2.4.14-win32-build2\\win32";
	
	Process gphoto;
	OutputStreamWriter stdin;
	InputStreamReader stdout;
	
	boolean connected;

	String lastCamStatus;
	String model;
	
	public GPhoto() {
		
	}

	void startProcess() throws IOException
	{
		List<String> args = new ArrayList<>();
		
		args.add(gphotoDir + File.separator + "gphoto2.exe");
		args.add("--force-overwrite");
		args.add("--auto-detect");
		args.add("--stdout");
		args.add("--stdout-size");
		args.add("--shell");
		
		ProcessBuilder pb = new ProcessBuilder(args.toArray(new String[0]));
		pb.environment().put("CAMLIBS", gphotoDir + File.separator + "camlibs");
		pb.environment().put("IOLIBS", gphotoDir + File.separator + "iolibs");
		pb.environment().put("LANG", "C");
		pb.environment().put("nodosfilewarning", "true");
		pb.redirectError(Redirect.INHERIT);

		gphoto = pb.start();
	}
	
//	void startProcess(Runnable onReady)
//	{
//		
//		forceStartRunnable(() -> { 
//
//			try {
//				waitInit();
//			} catch (CameraException e) {
//				throw new IOException("Initialisation failed", e);
//			}
//			onReady.run();
//		});
//	}
	
	String pathRequest = "lcd /";
	String pathExpect = "Local directory now '/'.\n";

	void send(String command) throws IOException
	{
		new Thread() {
			@Override
			public void run() {
				try {
					gphoto.getOutputStream().write(command.getBytes());
					gphoto.getOutputStream().flush();	
				} catch (IOException e) {
					e.printStackTrace();
				}
				
			};
		}.start();
		
	
	}
	
	byte [] content;
	int end;
	

	boolean finishByNewLine(String pattern) {
		return finishByNewLine(pattern, 0);
	}

	boolean finishByNewLine(String pattern, int offset)
	{
		int l = pattern.length();
		if (end < l + offset) return false;
		for(int i = 0; i < pattern.length(); ++i) {
			char pc = pattern.charAt(l - i - 1);
			byte cc = content[(4096 + end - i - 1 - offset) % 4096];
			if (pc != cc) return false;
		}
		if (end > l + offset) {
			if (content[(4096 + end - pattern.length() - 1 - offset) % 4096] != '\n') {
				return false;
			}
		}
		
		return true;
	}

	boolean finishBy(String pattern) {
		return finishBy(pattern, 0);
	}
	
	boolean finishBy(String pattern, int offset)
	{
		int l = pattern.length();
		if (end < l + offset) return false;
		for(int i = 0; i < pattern.length(); ++i) {
			char pc = pattern.charAt(l - i - 1);
			byte cc = content[(4096 + end - i - 1 - offset) % 4096];
			if (pc != cc) return false;
			
		}
		
		return true;
	}
	
	int lastChar(int offset)
	{
		if (end < offset) return Integer.MIN_VALUE;
		return content[(4096 + end - 1 - offset) % 4096];
	}
	
	static class CommandResult {
		byte [] cr2;
		String error;
		public String label;
		public String current;
		List<String> choices;
		List<String> data;
		
		public boolean hasData() {
			return data != null && !data.isEmpty();
		}

		public void addData(String data2) {
			if (data == null) data = new ArrayList<>();
			data.add(data2);
		}

		public boolean hasChoices() {
			return choices != null && !choices.isEmpty();
		}

		/** Libellé => index */
		public LinkedHashMap<String, Integer> decodeChoices() throws CameraException {
			if (!hasChoices()) {
				throw new CameraException("no value founds");
			}
			LinkedHashMap<String, Integer> result = new LinkedHashMap<String, Integer>();
			Pattern p = Pattern.compile("(\\d+) (.*)");
			for(String isoChoice : choices) {
				Matcher m = p.matcher(isoChoice);
				if (!m.matches()) {
					throw new CameraException("Malformed choice: " + isoChoice);
				}
				Integer id = Integer.parseInt(m.group(1));
				String value = m.group(2);
				result.put(value, id);
			}

			return result;
		}
		
	}
	
	void readChar() throws IOException {
		int ri = gphoto.getInputStream().read();
		if (ri == -1) {
			throw new IOException("Initialisation failed");
		}
		if (ri == '\n' || ri > 27) {
			System.out.write(ri);
		}
		byte c = (byte)ri;
		content[(end++) % 4096] = c;
	}
	
	String readToEOL() throws IOException {
		StringBuffer bf = new StringBuffer();
		while(true) {
			readChar();
			if (finishBy("\n")) {
				return bf.toString();
			}
			bf.append((char)lastChar(0));
		}
		
	}
	
	interface GPhotoJob {
		public void run() throws IOException, InterruptedException;
	}
	
	interface GPhotoCancelableJob extends GPhotoJob {
		/** Appellé quand le job n'a pas pu démarrer */
		public void canceled();
	}
	
	private GPhotoJob running;
	private final List<GPhotoJob> nexts = new LinkedList<>();
	
	synchronized GPhotoJob getRunning()
	{
		return running;
	}

	// Démarrer ou met en file d'attente.
	synchronized void startRunnable(GPhotoJob r)
	{
		forceStartRunnable(r);
	}
	
	private synchronized void forceStartRunnable(GPhotoJob r) {
		nexts.add(r);
		if (running != null) {
			return;
		}
		
		new Thread(()->{
			
			GPhotoJob toRun;
			while(true)
			{
				synchronized(GPhoto.this)
				{
					if (!nexts.isEmpty()) {
						running = nexts.remove(0);
					} else {
						running = null;
						return;
					}
					toRun = running;
				}
				
				try {
					toRun.run();
				} catch(IOException | InterruptedException e) {
					e.printStackTrace();

					kill();
					
					synchronized(GPhoto.this)
					{
						running = null;
						for(GPhotoJob job : nexts) {
							if (job instanceof GPhotoCancelableJob) {
								((GPhotoCancelableJob)job).canceled();
							}
						}
						nexts.clear();
						
						return;
					}
				}
			}
		}).start();
		
	}

	String extractString(int startOffset, int endOffset)
	{
		StringBuilder errorStr = new StringBuilder();
		while(startOffset > endOffset) {
			errorStr.append((char)lastChar(startOffset));
			startOffset --;
		}
		return errorStr.toString();
	}
	
	CommandResult expect(String expectStr) throws IOException
	{
		CommandResult result = new CommandResult();
		

		content = new byte[4096];
		end = 0;
		while(true) {
			readChar();
			
			if (finishByNewLine("Label: ")) {
				result.label = readToEOL();
				continue;
			}
			if (finishByNewLine("Current: ")) {
				result.current = readToEOL();
				continue;
				
			}
			if (finishByNewLine("Choice: ")) {
				String choice = readToEOL();
				if (result.choices == null) {
					result.choices = new ArrayList<>();
				}
				result.choices.add(choice);
				continue;
				
			}
			
			if (finishBy(" ***\n")) {
				System.out.println("error ?");
				int startOffset = 4;
				
				while(startOffset < content.length && startOffset < end && !finishByNewLine("*** ", startOffset)) {
					startOffset ++;
				}
				startOffset --;
				String errorStr = extractString(startOffset, 4);
				System.out.println("error:" + errorStr);
				if (result.error == null) {
					result.error = errorStr;
				}
				continue;
			}
			
			if (	finishBy("CR\u0002")
//					   content[(4096 + end - 3) % 4096] == 'C'
//					&& content[(4096 + end - 2) % 4096] == 'R'
//					&& content[(4096 + end - 1) % 4096] == 2
					&& finishBy("\nII*", 8))
//					&& content[(4096 + end - 12) % 4096] == '\n'
//					&& content[(4096 + end - 11) % 4096] == 'I'
//					&& content[(4096 + end - 10) % 4096] == 'I'
//					&& content[(4096 + end - 9) % 4096] == '*')
			{
				String lengthStr = "";
				for(int i = 12; i < 23; ++i) {
					int b = lastChar(i);
					if (b == '\n' || b == Integer.MIN_VALUE) {
						break;
					}
					if (b >='0' && b<='9') {
						lengthStr = ((char)b) + lengthStr;
					} else {
						lengthStr = "";
						break;
					}
				}
				if (lengthStr.equals("")) {
					throw new RuntimeException("Invalid download found");
				}
				int length = Integer.parseInt(lengthStr);
				
				byte [] buff = new byte[length];
				for(int i = 0; i < 11; ++i) {
					buff[i] = (byte)lastChar(10 - i);
				}
				int off = 11;
				length -= 11;
				while(length > 0) {
					int rd = gphoto.getInputStream().read(buff, off, length);
					if (rd == -1) {
						throw new RuntimeException("Failed to read full cr2");
					}
					off += rd;
					length -= rd;
				}
				System.out.println("readed: " + buff.length);
				try(FileOutputStream fos = new FileOutputStream("c:\\tmp\\test.cr2")) {
					fos.write(buff);
				}
				result.cr2 = buff;
				// Retirer le début du CR2
				end -= 8 + 3;
				continue;
			}
			
			if (finishBy(expectStr)) {
				System.out.println("found: " + expectStr);
				return result;
			}
			
			if (finishBy("\n")) {
				int lineLength = 1;
				while(lineLength < end && lineLength < content.length &&  !finishByNewLine("", lineLength)) {
					lineLength ++;
				}
				String data = extractString(lineLength - 1, 0);
				if (!data.startsWith("gphoto2: {")) {
					System.out.println("Got data: " + data);
					
					Pattern camStatus = Pattern.compile("Camera Status (\\d+)$");
					Matcher m = camStatus.matcher(data);
					if (m.find()) {
						lastCamStatus = m.group(1);
						System.out.println("Cam status is now " + lastCamStatus);
					}
					result.addData(data);
				}
			}
			
		}
		
	}
	
	
	
	CommandResult doCommand(String ... batch) throws IOException 
	{
		long start = System.currentTimeMillis();
		try {
			StringBuilder toSend = new StringBuilder();
			for(String b : batch) {
				toSend.append(b);
				toSend.append("\n");
			}
			toSend.append(pathRequest);
			toSend.append("\n");
			send(toSend.toString());
			return expect(pathExpect);
		} finally {
			System.out.println("command " + Arrays.asList(batch) + " done in " + (System.currentTimeMillis() - start) + "ms");
		}
	}
	
	CommandResult noError(CommandResult cr) throws CameraException
	{
		if (cr.error != null) {
			throw new CameraException("Camera error: " + cr.error);
		}
		return cr;
	}
	
	void waitInit() throws IOException, InterruptedException, CameraException
	{
		noError(expect("Model"));
		CommandResult cr = noError(doCommand(""));
		if (cr.hasData() && cr.data.size() >= 3) {
			model = cr.data.get(2);
			if (model.length() > 31) {
				model = model.substring(0, 31);
			}
			model = model.trim();
			System.out.println("model = " + model);
		}
		
		int cpt = 0;
		while(noError(doCommand("wait-event 0s")).hasData()) {
			if (cpt++ > 100) {
				throw new IOException("Too many event to flush");
			}
		};

		// On ignore les erreur sur les deux premies 
		// (ils permettent de faire réussir le uilock 1, mais parfois ils lancent un busy)
		doCommand("set-config capture 0");
		doCommand("set-config uilock 0");
		noError(doCommand("set-config uilock 1"));
		noError(doCommand("get-config shutterspeed"));
		noError(doCommand("set-config shutterspeed bulb"));
		noError(doCommand("set-config imageformat RAW"));
		noError(doCommand("set-config capturetarget 0"));
		noError(doCommand("list-config"));
		noError(doCommand("get-config battery-level"));
		trashEvents();
		connected = true;
		// doShoot(500);
//		doCommand("set-config output 1", 
//					"wait-event 2s", 
//					"set-config bulb 1", 
//					"wait-event 25ms", 
//					"set-config bulb 0",
//					"set-config output 0",
//					"wait-event-and-download 3s");
		
	}
	
	void directShoot(int ms) throws IOException, InterruptedException {
		
		
	}
	
//	void doShoot(RunningShootInfo rsi) throws IOException, InterruptedException
//	{
//		int ms = (int)Math.floor(rsi.getExp() * 1000);
//		
//		doCommand("set-config output 1", 
//				"wait-event 2s");
//		doCommand("set-config bulb 1");
//		doCommand("wait-event 1");
//		// expect("UNKNOWN Camera Status 1");
//		long t = System.currentTimeMillis();
//		rsi.setStartTime(t);
//		long elapsed = System.currentTimeMillis() - t;
//		long toSleep = ms - elapsed;
//		if (toSleep > 0) {
//			System.out.println("Sleeping :" + toSleep);
//			Thread.sleep(toSleep);
//		} else {
//			System.out.println("Too late :" + toSleep);
//		}
//		CommandResult cr = doCommand("set-config bulb 0",
//				"set-config output 0",
//				"wait-event-and-download 3s");
//		File targetFile = rsi.createTargetFile();
//		if (targetFile != null) {
//			try(FileOutputStream fos = new FileOutputStream(targetFile))
//			{
//				fos.write(cr.cr2);
//				
//			}
//			logger.info("saved: " + targetFile.getAbsolutePath());
//			this.listeners.getTarget().onShootDone(rsi, targetFile);
//		}
//	}
	
	
	public static void main(String[] args) {
		GPhoto gp = new GPhoto();
		try {
			gp.startProcess();
			gp.waitInit();
		}catch (Exception e) {
			e.printStackTrace();
		}

	}

	public void kill() {
		Process p = gphoto;
		connected = false;
		if (p != null) {
			gphoto = null;
			p.destroy();
			try {
				p.getErrorStream().close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			try {
				p.getInputStream().close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			try {
				p.getOutputStream().close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	/** Ecoute les evenements 
	 * @throws IOException 
	 * @throws CameraException */
	public void trashEvents() throws IOException {
		int cpt = 0;
		CommandResult cr;
		try {
			int consecutiveCamBusy = 0;
			long consecutiveCamBusyStart = 0L;
			while((cr = noError(doCommand("wait-event 1s"))).hasData() || "1".equals(lastCamStatus)) {
				if ("1".equals(lastCamStatus)) {
					if (consecutiveCamBusy == 0) {
						consecutiveCamBusyStart = System.currentTimeMillis();		
					}
					// On a un camera status 1 en attente. Il faut attendre un peu plus...
					consecutiveCamBusy++;
					// On attend jusque 5 secondes !
					if (System.currentTimeMillis() > consecutiveCamBusyStart + 20000) {
						// FIXME: erreur de protocole ? La cam devrait être réinitialisée ?
						// On peut peut être essayer de faire un reset en gphoto ?
						throw new IOException("Camera failed to become idle within 20s");
					}
				}
				
				for(String s : cr.data) {
					Pattern fileAdded = Pattern.compile("^FILEADDED (.*) /");
					Matcher m = fileAdded.matcher(s);
					if (m.matches()) {
						System.err.println("dropping file added : " + m.group(1));
						doCommand("delete " + m.group(1));
					}
				}
					
				if (cpt++ > 100) {
					throw new IOException("Too many event to flush");
				}
			};
		} catch(CameraException e) {
			throw new IOException("Protocol error", e);
		}
	}
}

