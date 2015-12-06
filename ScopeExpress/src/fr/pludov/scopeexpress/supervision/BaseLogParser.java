package fr.pludov.scopeexpress.supervision;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import javax.swing.JFileChooser;

/**
 * 
 * Produit:
 *   status: idle, calibration, guiding 
 *   
 * @author utilisateur
 *
 */
public abstract class BaseLogParser extends Thread {
	final Supervisor supervisor;
	
	public BaseLogParser(Supervisor sup) {
		this.supervisor = sup;
		
	}
	
	abstract File getLogDir();
	abstract String getPattern();
	
	private File getLatestFile()
	{
		
		File phd2Dir = getLogDir();
		if (phd2Dir.exists()) {
			String [] candidataes = phd2Dir.list();
			List<String> allTimeCandidates = new ArrayList<>();
			for(String c : candidataes) {
				if (c.matches(getPattern())) {
					allTimeCandidates.add(c);
				}
			}
			
			if (!allTimeCandidates.isEmpty()) {
				Collections.sort(allTimeCandidates);
			
				File result = new File(phd2Dir, allTimeCandidates.get(allTimeCandidates.size() - 1));
				// if (result.lastModified() > System.currentTimeMillis() - 3600000 * 4) {
					return result;
				//}
			}
		}
		return null;
	}
	
	File current;
	FileInputStream currentIs;
	int currentLineLength;
	final byte [] currentLine = new byte[4096]; // Max line length...
	
	void finishCurrent()
	{
		try {
			currentIs.read();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			try {
				currentIs.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
			currentIs = null;
			current = null;
			currentLineLength = 0;
		}
	}
	
	boolean firstIteration;
	
	@Override
	public void run() {
		firstIteration = true;
		
		while(true) {
			try {
				doIterate();
			} catch(IOException e) {
				e.printStackTrace();
			} finally {
				firstIteration = false;
			}
		}
	}
	
	
	protected abstract void fileStarted(File file) throws Exception;
	protected abstract void lineReceived(byte [] line, boolean emitEvent) throws Exception;
	
	private void currentLineCompleted(boolean emitEvent)
	{
		try {
			lineReceived(Arrays.copyOf(currentLine, Math.min(currentLine.length, currentLineLength)), emitEvent);
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	private void readAsMuchAsAvailable(boolean emitEvent) throws IOException
	{
		int available = currentIs.available();
		if (available > 0) {
			if (available > 4096) available = 4096;
			
			byte [] content = new byte[available];
			int readed = currentIs.read(content);
			for(int i = 0; i < readed; ++i) {
				byte b = content[i];
				if (b == '\n' || b == '\r') {
					// La ligne est finie !
					currentLineCompleted(emitEvent);
					currentLineLength = 0;
				} else {
					if (currentLineLength < currentLine.length) {
						currentLine[currentLineLength] = b;
					}
					currentLineLength ++;
				}
			}
		}
	}
	
	private void doIterate() throws IOException {
		File oldCurrent = current;
		File newCurrent = getLatestFile();
		if (!Objects.equals(current, newCurrent)) {
			if (current != null) {
				finishCurrent();
			}
			try {
				if (newCurrent != null) {
					currentIs = new FileInputStream(newCurrent);
				}
				current = newCurrent;
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} finally {
				if (!Objects.equals(newCurrent, oldCurrent)) {
					try {
						fileStarted(current);
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}
			if (current != null) {
				// Il faut parser tout le contenu, pour retrouver l'heure de début
				readAsMuchAsAvailable(!firstIteration);
			}
		}
		if (currentIs != null) {
			readAsMuchAsAvailable(true);
		}
		try {
			Thread.sleep(200);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
