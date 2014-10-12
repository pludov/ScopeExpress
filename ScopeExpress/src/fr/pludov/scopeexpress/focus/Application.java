package fr.pludov.scopeexpress.focus;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import fr.pludov.scopeexpress.async.WorkStepProcessor;
import fr.pludov.scopeexpress.focus.MosaicListener.ImageAddedCause;
import fr.pludov.scopeexpress.ui.utils.BackgroundTaskQueue;
import fr.pludov.scopeexpress.utils.WeakListenerCollection;

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

	int starRay;
	
	public Application() {
		this.starRay = 25;
		this.images = new HashMap<File, WeakReference<Image>>();
		this.mosaics = new ArrayList<Mosaic>();
		
		this.workStepProcessor = new WorkStepProcessor();
		this.backgroundTaskQueue = new BackgroundTaskQueue();
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

}
