package fr.pludov.scopeexpress.drivers.gphoto;

import java.io.*;
import java.lang.ProcessBuilder.*;
import java.util.*;

import fr.pludov.scopeexpress.camera.*;

public class GPhoto {
	
	String gphotoDir = "C:\\tmp\\gphoto\\gphoto-2.4.14-win32-build2\\win32";
	
	Process gphoto;
	OutputStreamWriter stdin;
	InputStreamReader stdout;
	
	boolean connected;
	
	public GPhoto() {
		
	}

	void startProcess(Runnable onReady)
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
		
		forceStartRunnable(() -> { 
			gphoto = pb.start();

			waitInit();
			onReady.run();
		});
	}
	
	String pathRequest = "cd /";
	String pathExpect = "Remote directory now '/'.";

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
//	class MegaBufferedInputStream extends BufferedInputStream {
//
//		public MegaBufferedInputStream(InputStream in) {
//			super(in);
//			super.in;
//			
//		}
//		static InputStream getUnbuffered(BufferedInputStream bus) {
//		}
//	
//	}
	
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
			if (content[(4096 + end - pattern.length() - 1) % 4096] != '\n') {
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
	
	GPhotoJob running;
	
	synchronized void startRunnable(GPhotoJob r) throws CameraException
	{
		if (running != null) {
			throw new CameraException("Busy");
		}
		forceStartRunnable(r);
	}
	
	private void forceStartRunnable(GPhotoJob r) {
		running = r;
		
		new Thread(()->{
			try {
				r.run();
			} catch(IOException | InterruptedException e) {
				e.printStackTrace();
				gphoto.destroy();
			} finally {
				synchronized(GPhoto.this) {
					if (running == r) {
						running = null;
					}
				}
			}
			
		}).start();
		
	}
//	
//	synchronized void startShoot(RunningShootInfo rsi) throws CameraException
//	{
//		startRunnable(()-> {
//			doShoot(rsi);	
//		});
//		
//	}
	
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
				
				while(startOffset < content.length && !finishByNewLine("*** ", startOffset)) {
					startOffset ++;
				}
				startOffset -= 4;
				StringBuilder errorStr = new StringBuilder();
				while(startOffset > 4) {
					errorStr.append((char)lastChar(startOffset));
					startOffset --;
				}
				System.out.println("Error: " + errorStr);
				if (result.error == null) {
					result.error = errorStr.toString();
				}
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
			}
			
			if (finishBy(expectStr)) {
				System.out.println("found: " + expectStr);
				return result;
			}
		}
		
	}
	
	
	
	CommandResult doCommand(String ... batch) throws IOException 
	{
		StringBuilder toSend = new StringBuilder();
		for(String b : batch) {
			toSend.append(b);
			toSend.append("\n");
		}
		toSend.append(pathRequest);
		toSend.append("\n");
		send(toSend.toString());
		return expect(pathExpect);
		
	}
	
	void waitInit() throws IOException, InterruptedException
	{
		expect("Model");
		doCommand("set-config uilock 1");
		doCommand("get-config shutterspeed");
		doCommand("set-config shutterspeed bulb");
//		doCommand("wait-event 10s");
//		doCommand("wait-event 10s");
//		doCommand("wait-event 10s");
//		doCommand("wait-event 10s");
		doCommand("set-config imageformat RAW");
		doCommand("set-config capturetarget 0");
		
		doCommand("get-config battery-level");
		
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
			gp.startProcess(()->{});
			gp.waitInit();
		}catch (IOException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	public void kill() {
		gphoto.destroy();
	}
}

