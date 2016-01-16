package fr.pludov.scopeexpress.focus;

import java.io.*;
import java.lang.ref.*;
import java.util.*;

import fr.pludov.scopeexpress.async.*;
import fr.pludov.scopeexpress.irc.*;
import fr.pludov.scopeexpress.supervision.*;
import fr.pludov.scopeexpress.tasks.*;
import fr.pludov.scopeexpress.ui.*;
import fr.pludov.scopeexpress.ui.utils.*;

/**
 * Les fonctions fournies:
 *   - faire en sorte que chaque image soit chargée une et une seule fois
 *   - centraliser les accès au files de traitement
 */
public class Application {
	final List<Mosaic> mosaics;
	final Map<File, WeakReference<Image>> images;
	
	// FIXME: il y a manifestement concurrence entre les deux...
	WorkStepProcessor workStepProcessor;
	final BackgroundTaskQueue backgroundTaskQueue;
	// Ceci est strictement exécuté dans le thread swing
	final TaskManager taskManager;
	IRCServer ircServer;
	Supervisor supervisor;
	
	final ITaskParameterView configurationTaskValues;
	final ITaskOptionalParameterView lastUsedTaskValues; 
	
	int starRay;
	
	public Application() {
		this.starRay = 25;
		
		this.lastUsedTaskValues = new PreviousTaskValues();
		this.configurationTaskValues = new TaskParameterView(null, null, null, null);
		
		this.images = new HashMap<File, WeakReference<Image>>();
		this.mosaics = new ArrayList<Mosaic>();
		
		this.workStepProcessor = new WorkStepProcessor();
		this.backgroundTaskQueue = new BackgroundTaskQueue();
		
		this.taskManager = new TaskManager();
		
		this.supervisor = new Supervisor();
		// FIXME: en conf
		// new PhdLogParser(this.supervisor).start();
		
		activateConfiguration();
	}

	public synchronized Image getImage(File path)
	{
		WeakReference<Image> result = this.images.get(path);
		
		if (result != null) {
			Image i = result.get();
			
			if (i != null) return i;
			this.images.remove(path);
		}
		
		Image i = new Image(path);
		result = new WeakReference<Image>(i);
		this.images.put(path, result);
		
		return i;
	}
	
	int getStarRay()
	{
		return starRay;
	}


	public WorkStepProcessor getWorkStepProcessor() {
		return workStepProcessor;
	}

	public BackgroundTaskQueue getBackgroundTaskQueue() {
		return backgroundTaskQueue;
	}

	private void activateConfiguration()
	{
		Configuration configuration = Configuration.getCurrentConfiguration();
		if (configuration.isIrcEnabled() != (ircServer != null)) {
			if (configuration.isIrcEnabled()) {
				ircServer = new IRCServer("scopeexpress");
				ircServer.start();
			} else {
				ircServer.close();
				ircServer = null;
			}
		}
	}
	
	public void configurationUpdated() {
		activateConfiguration();
	}

	public Supervisor getSupervisor() {
		return supervisor;
	}

	public TaskManager getTaskManager() {
		return taskManager;
	}

	public ITaskParameterView getConfigurationTaskValues() {
		return configurationTaskValues;
	}

	public ITaskOptionalParameterView getLastUsedTaskValues() {
		return lastUsedTaskValues;
	}

}
