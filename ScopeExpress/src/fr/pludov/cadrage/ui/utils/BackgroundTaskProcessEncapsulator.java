package fr.pludov.cadrage.ui.utils;

import java.io.IOException;
import java.io.InputStream;

import org.apache.log4j.Logger;

import fr.pludov.cadrage.ui.utils.BackgroundTask.BackgroundTaskCanceledException;
import fr.pludov.cadrage.ui.utils.BackgroundTask.Status;

/**
 * Permet à une instance de BackgroundTask d'executer un process externe
 * 
 * Le process externe sera automatiquement tué en cas d'interruption de la tache
 * Le process sera également enregistré auprès de AtExitProcessKiller
 * La sortie du process sera loguée
 */
public class BackgroundTaskProcessEncapsulator
{
	private static final Logger logger = Logger.getLogger(BackgroundTaskProcessEncapsulator.class);

	final Process p;
	final InputStream os;
	final BackgroundTask bt;
	
	public BackgroundTaskProcessEncapsulator(Process p, BackgroundTask bt) throws IOException {
		this.p = p;
		AtExitProcessKiller.getInstance().add(p);
		this.bt = bt;
		os = p.getInputStream();
	}
	
	public void start()
	{
		Thread inputThread = new Thread("input consumer") {
			public void run() {
				consumeInput();
			};
		};
		inputThread.start();

		if (bt != null) {
			Thread killThread = new Thread("kill on abort") {
				public void run()
				{
					try {
						killOnInterrupt();
					} catch (InterruptedException e) {
						logger.error("kill on abort thread was interrupted", e);
					}
				}
			};
			
			killThread.start();
		}
	}
	
	private void consumeInput() {
		try 
		{
			int c;
			try {
				while((c = os.read()) != -1) {
					System.out.write(c);
				}
			} finally {
				os.close();
			}
		} catch (Exception e) {
			logger.error("Error reading output from astrometry process", e);
		}
	}

	private void killOnInterrupt() throws InterruptedException
	{
		do {
			bt.waitForEndOfRunningStatus();
		} while(bt.getStatus() == Status.Running);
		try {
			p.exitValue();
		} catch(IllegalThreadStateException e) {
			logger.info("Killing external process");
			p.destroy();
		}
	}
	
	public void waitEnd() throws InterruptedException, BackgroundTaskCanceledException
	{
		p.waitFor();
		AtExitProcessKiller.getInstance().remove(p);
		bt.checkInterrupted();
	}
}