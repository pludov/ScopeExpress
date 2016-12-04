package fr.pludov.scopeexpress.camera;

import java.io.*;

import fr.pludov.scopeexpress.scope.ascom.*;

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

	public File createTargetFile(String ext) throws IOException {
		File target = new File(this.getPath() + "/" + this.getFileName());
		File parent = target.getParentFile();
		parent.mkdirs();
		
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
