package fr.pludov.scopeexpress.focus;

import java.io.*;
import java.lang.ref.*;
import java.nio.file.*;
import java.nio.file.attribute.*;
import java.util.*;

import org.apache.log4j.*;

import fr.pludov.io.*;
import fr.pludov.scopeexpress.async.*;
import fr.pludov.scopeexpress.utils.*;
import fr.pludov.utils.*;

public class Image implements WorkStepResource {
	private static final Logger logger = Logger.getLogger(Image.class);
	
	final File path;
	
	SoftReference<CameraFrame> cameraFrame;
	/** On garde un softReference sur l'application de dark */
	final WeakHashMap<Image, SoftReference<CameraFrame>> darkedCameraFrame;
	CameraFrame cameraFrameLocked;
	
	int cameraFrameLockCount;
	boolean loading;
	
	// Pas de softref pour ça, c'est tout petit
	CameraFrameMetadata cameraFrameMetadata;

	volatile boolean hasSize;
	int width, height;
	boolean cfa;
	
	Map<Couple<Image, ChannelMode>, SoftReference<Histogram>> histograms;
	
	/**
	 * Une image ne doit pas être construite directement.
	 * @see Application.getImage
	 */
	Image(File path) {
		this.path = path;
		this.cameraFrame = new SoftReference<CameraFrame>(null);
		this.darkedCameraFrame = new WeakHashMap<>(1);
		this.histograms = new HashMap<>();
		this.cameraFrameLockCount = 0;
		this.cameraFrameLocked = null;
	}
	
	@Override
	public boolean lock() {
		synchronized(this)
		{
			if (cameraFrameLocked == null) {
				cameraFrameLocked = cameraFrame.get();
			}
			if (cameraFrameLocked != null) {
				cameraFrameLockCount++;
				return true;
			}
		}
		return false;
	}
	
	@Override
	public void unlock() {
		synchronized(this)
		{
			cameraFrameLockCount --;
			if (cameraFrameLockCount == 0) {
				cameraFrameLocked = null;
			}
		}
	}
	
	@Override
	public void produce() {
		synchronized(this)
		{
			while (loading) {
				try {
					wait(1000);
				} catch(InterruptedException e) {
					e.printStackTrace();
				}
			}
			if (cameraFrameLocked == null) {
				cameraFrameLocked = cameraFrame.get();
			}
			if (cameraFrameLocked != null) {
				cameraFrameLockCount++;
				return;
			}
			loading = true;
		}
		Couple<CameraFrame, CameraFrameMetadata> loaded = null;
		try {
			try {
				logger.info("Loading " + path);
				loaded = ImageProvider.readImage(path);
			} catch(IOException ex) {
				ex.printStackTrace();
			}
		} finally {
			if (loaded == null) {
				loaded = new Couple<CameraFrame, CameraFrameMetadata>(new CameraFrame(), null);
			}
			
			synchronized(this)
			{
				this.loading = false;
				CameraFrame frame = loaded.getA();
				setFrame(frame);
				if (loaded.getB() != null) {
					this.cameraFrameMetadata = loaded.getB();
				}
				this.cameraFrameLocked = frame;
				this.cameraFrameLockCount++;
				
				
				notifyAll();
			}
		}
	}

	private void setFrame(CameraFrame frame) {
		this.cameraFrame = new SoftReference<CameraFrame>(frame);
		this.hasSize = true;
		this.width = frame.getWidth();
		this.height = frame.getHeight();
		this.cfa = frame.isCfa();
	}
	
	/**
	 * Retourne le contenu du raw (caché si encore dans le heap)
	 * @return
	 */
	public CameraFrame getCameraFrame()
	{
		CameraFrame result;
		if (!lock()) {
			produce();
		}
		result = this.cameraFrameLocked;
		unlock();
		return result;
	}

	
	public CameraFrame getCameraFrameWithDark(Image darkFrame) {
		if (darkFrame == null) {
			return getCameraFrame();
		}
		
		CameraFrame result = getCachedFrameWithDark(darkFrame, null);
		if (result != null) return result;
		
		CameraFrame image = getCameraFrame();
		
		result = getCachedFrameWithDark(darkFrame, null);
		if (result != null) return result;
		
		CameraFrame dark = darkFrame.getCameraFrame();
		
		result = getCachedFrameWithDark(darkFrame, null);
		if (result != null) return result;
		
		// Substract...
		result = image;
		if (dark.getWidth() == image.getWidth() && dark.getHeight() == image.getHeight()) {
			result = result.substract(dark);
		}
		
		// Store the result, or get the current one (to avoid duplication)
		return getCachedFrameWithDark(darkFrame, result);
	}

	/**
	 * @param darkFrame le dark
	 * @param newValue Si il n'y a pas d'image en cache, on peut y mettre celle-ci
	 * @return
	 */
	private CameraFrame getCachedFrameWithDark(Image darkFrame, CameraFrame newValue) {
		synchronized(this.darkedCameraFrame) {
			SoftReference<CameraFrame> ref = this.darkedCameraFrame.get(darkFrame);
			if (ref != null) {
				CameraFrame cf = ref.get();
				if (cf != null) return cf;
			}
			
			if (newValue != null) {
				this.darkedCameraFrame.put(darkFrame, new SoftReference<>(newValue));
			}
			return newValue;
		}
	}

	public Histogram getHistogram(Image dark, fr.pludov.utils.ChannelMode channel, boolean nullIfNotAvailabel) {
		if (nullIfNotAvailabel) {
			Couple<Image, ChannelMode> id = new Couple<>(dark, channel);
			return getCachedHistogram(id, null);
		} else {
			return getHistogram(dark, channel);
		}
	}

	public Histogram getHistogram(Image dark, fr.pludov.utils.ChannelMode channel) {
		Couple<Image, ChannelMode> id = new Couple<>(dark, channel);
		Histogram result;
		result = getCachedHistogram(id, null);
		if (result != null) return result;
		
		// FIXME: eviter deux calculs en parallèle...
		
		CameraFrame frame = getCameraFrameWithDark(dark);
		result = getCachedHistogram(id, null);
		if (result != null) return result;
		
		result = new Histogram();
		result.calc(frame, 0, 0, frame.getWidth() - 1, frame.getHeight() - 1, channel);
		
		result = getCachedHistogram(id, result);
		return result;
	}
	
	private Histogram getCachedHistogram(Couple<Image, ChannelMode> id, Histogram newValue) {
		synchronized(this.histograms) {
			SoftReference<Histogram> ref = this.histograms.get(id);
			if (ref != null) {
				Histogram h = ref.get();
				if (h != null) return h;
			}
			if (newValue != null) {
				this.histograms.put(id,  new SoftReference<>(newValue));
			}
			return newValue;
		}
	}
	
	/** FIXME: pour l'instant ça peut etre null */
	public CameraFrameMetadata getMetadata()
	{
		synchronized(this) {
			if (this.cameraFrameMetadata != null) {
				return this.cameraFrameMetadata;
			}
			while(loading) {
				try {
					wait(1000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				if (this.cameraFrameMetadata != null) {
					return this.cameraFrameMetadata;
				}
			}
			loading = true;
		}
		Couple<CameraFrame, CameraFrameMetadata> result = null;
		try {
			result = ImageProvider.readImageMetadata(this.path);
		} catch(IOException e) {
			e.printStackTrace();
		} finally {
			if (result == null) {
				result = new Couple<CameraFrame, CameraFrameMetadata>(null, new CameraFrameMetadata());
			}
			// Eventuellement, on va chercher sur le filesystem (date de création, ...)
			metadataFallback(result.getB(), this.path, result);
			synchronized(this) {
				loading = false;
				if (result != null && this.cameraFrameMetadata == null) {
					this.cameraFrameMetadata = result.getB();
				}
				if (result != null && result.getA() != null && this.cameraFrameLockCount == 0) {
					// Assurer qu'on ait une camera frame
					setFrame(result.getA());
				}
				notifyAll();
			}
		}
		return result.getB();
	}
	
	private void metadataFallback(CameraFrameMetadata cfm, File path2, Couple<CameraFrame, CameraFrameMetadata> result) {
		try {
			BasicFileAttributes attr = Files.readAttributes(path2.toPath(), BasicFileAttributes.class);
			FileTime creationTime = attr.creationTime();
			if (creationTime != null) { 
				long epoch = creationTime.toMillis();
				if (cfm.getDuration() != null) {
					// On suppose que le fichier est toujours créé après la fin de la prise de vue.
					epoch -= cfm.getDuration() * 1000;
				}
				cfm.setStartMsEpoch(epoch);
			}
		} catch(IOException e) {
			e.printStackTrace();
		}
	}

	public boolean hasReadyCameraFrame() {
		synchronized(this)
		{
			return cameraFrame.get() != null;
		}
	}
	
	public boolean hasReadyMetadata()
	{
		synchronized(this) {
			return this.cameraFrameMetadata != null;
		}
	}
	
	public File getPath() {
		return path;
	}

	@Override
	public String toString() {
		return this.path.getName();
	}
	
	public int getWidth() {
		if (!hasSize) {
			getCameraFrame();
		}
		return width;
	}

	public int getHeight() {
		if (!hasSize) {
			getCameraFrame();
		}
		return height;
	}

	public CameraFrame getCameraFrameWithAutoDark() {
		Image dark = DarkLibrary.getInstance().getDarkFor(this);
		return getCameraFrameWithDark(dark);
	}


	public boolean isCfa() {
		if (!hasSize) {
			getCameraFrame();
		}
		return cfa;
	}
}
