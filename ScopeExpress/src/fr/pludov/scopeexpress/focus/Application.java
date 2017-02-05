package fr.pludov.scopeexpress.focus;

import java.io.*;
import java.lang.ref.*;
import java.util.*;

import fr.pludov.scopeexpress.*;
import fr.pludov.scopeexpress.async.*;
import fr.pludov.scopeexpress.database.*;
import fr.pludov.scopeexpress.database.content.*;
import fr.pludov.scopeexpress.irc.*;
import fr.pludov.scopeexpress.script.*;
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
	final TaskManager2 taskManager2;
	
	IRCServer ircServer;
	Supervisor supervisor;
	
	final ISafeTaskParameterView configurationTaskValues;
	final ITaskOptionalParameterView lastUsedTaskValues; 

	private final Database<Root> database;

	private OrientationModel orientationModel;
	
	int starRay;
	
	public Application() {
		this.starRay = 25;
		database = Database.loadWithDefault(Root.class, new File(Configuration.getApplicationDataFolder(), "database"));	 

		
		this.lastUsedTaskValues = new StoredTaskParameter(getDatabase().getRoot().getTaskPrevious(), "");
		this.configurationTaskValues = new StoredTaskParameter(getDatabase().getRoot().getTaskConfig(), "");
		
		this.images = new HashMap<File, WeakReference<Image>>();
		this.mosaics = new ArrayList<Mosaic>();
		
		this.workStepProcessor = new WorkStepProcessor();
		this.backgroundTaskQueue = new BackgroundTaskQueue();
		
		this.taskManager = new TaskManager();
		this.taskManager2 = new TaskManager2();
		
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

	public IRootParameterView<? extends ISafeTaskParameterView> getConfigurationTaskValues() {
		return ((StoredTaskParameter)configurationTaskValues).getRootParameterView();
	}

	public IRootParameterView<? extends ITaskOptionalParameterView> getLastUsedTaskValues() {
		return ((StoredTaskParameter)lastUsedTaskValues).getRootParameterView();
	}

	public Database<Root> getDatabase() {
		return database;
	}

	public TaskManager2 getTaskManager2() {
		return taskManager2;
	}

	public OrientationModel getOrientationModel() {
		return orientationModel;
	}

	public void setOrientationModel(OrientationModel orientationModel) {
		this.orientationModel = orientationModel;
	}

}
