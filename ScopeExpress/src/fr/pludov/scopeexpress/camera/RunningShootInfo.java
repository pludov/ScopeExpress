package fr.pludov.scopeexpress.camera;

import java.io.*;
import java.math.*;

import javax.swing.*;

import fr.pludov.scopeexpress.scope.ascom.*;
import fr.pludov.scopeexpress.tasks.shoot.*;
import fr.pludov.scopeexpress.ui.*;

public class RunningShootInfo extends ShootParameters {
	long startTime;
	boolean aborted;

	public RunningShootInfo(ShootParameters parameters) {
		super(parameters);
	}

	public long getStartTime() {
		return startTime;
	}

	public void setStartTime(long startTime) {
		this.startTime = startTime;
	}

	BigDecimal ccdTempSetAccum = BigDecimal.ZERO;
	int ccdTempSetCount = 0;
	
	BigDecimal ccdTempAccum = BigDecimal.ZERO;
	int ccdTempCount = 0;
	
	public synchronized void addTemp(double temp)
	{
		ccdTempAccum = ccdTempAccum.add(BigDecimal.valueOf(temp));
		ccdTempCount++;
	}
	
	public synchronized void addTempSet(double temp)
	{
		ccdTempSetAccum = ccdTempSetAccum.add(BigDecimal.valueOf(temp));
		ccdTempSetCount++;
		
	}
	
	public synchronized BigDecimal getCcdTemp()
	{
		if (ccdTempCount == 0) return null;
		return ccdTempAccum.divide(BigDecimal.valueOf(ccdTempCount), new MathContext(2));
	}
	
	public synchronized BigDecimal getCcdTempSet()
	{
		if (ccdTempSetCount == 0) return null;
		return ccdTempSetAccum.divide(BigDecimal.valueOf(ccdTempSetCount), new MathContext(2));
	}

	
	public File createTargetFile(String ext) throws IOException {
		String path = getPath() != null ? getPath() : Configuration.getCurrentConfiguration().getFitBase();
		String fileName = getFileName() != null ? getFileName() : Configuration.getCurrentConfiguration().getFitPattern();
		
		
		if (!new File(path).isDirectory()) {
			throw new IOException("Target directory not found : " + path);
		}
		
		// Appliquer les paramètres
		String [] newPath = new String[1];
		try {
			SwingUtilities.invokeAndWait(new Runnable() {
				@Override
				public void run() {
					try {
						newPath[0] = new FileNameGenerator(FocusUi.getInstance()).performFileNameExpansion(path, fileName, RunningShootInfo.this);
					} catch (IOException e) {
						throw new RuntimeException(e);
					}
				}
			});
		} catch (Exception e) {
			throw new IOException("Problem with file name expansion", e);
		}
		

		File target = new File(path + File.separator + newPath[0]);
		File parent = target.getParentFile();
		parent.mkdirs();
		if (!parent.isDirectory()) {
			throw new IOException("Unable to create target directory: " + target);
		}
		
		String baseName = target.getName();
		File targetFile = null;
		for(int i = 0; i < 10000; ++i) {
			File f;
			if (i == 0) {
				f = new File(parent, baseName + ext);
			} else {
				f = new File(parent, baseName + "-" + i + ext);
			}
			if (f.createNewFile()) {
				targetFile = f;
				break;
			}
		}
		if (targetFile == null) {
			AscomCamera.logger.warn("Unable to create new file for " + baseName);
		}
		return targetFile;
	}

	public boolean aborted() {
		return aborted;
	}

	public void setAborted()
	{
		this.aborted = true;
	}
}
