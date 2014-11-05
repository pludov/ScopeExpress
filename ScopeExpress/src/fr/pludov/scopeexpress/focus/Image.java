package fr.pludov.scopeexpress.focus;

import java.io.File;
import java.io.IOException;
import java.lang.ref.SoftReference;
import java.lang.ref.WeakReference;
import java.nio.file.Files;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;

import org.apache.log4j.Logger;

import com.drew.imaging.ImageMetadataReader;
import com.drew.metadata.Directory;
import com.drew.metadata.Metadata;
import com.drew.metadata.Tag;
import com.drew.metadata.exif.ExifSubIFDDirectory;

import fr.pludov.io.CameraFrame;
import fr.pludov.io.CameraFrameMetadata;
import fr.pludov.io.ImageProvider;
import fr.pludov.scopeexpress.ImageDisplayParameter.ImageDisplayMetaDataInfo;
import fr.pludov.scopeexpress.async.WorkStepResource;
import fr.pludov.scopeexpress.utils.Couple;

public class Image implements WorkStepResource {
	private static final Logger logger = Logger.getLogger(Image.class);
	
	final File path;
	
	SoftReference<CameraFrame> cameraFrame;
	CameraFrame cameraFrameLocked;
	
	int cameraFrameLockCount;
	boolean loading;
	
	// Pas de softref pour ça, c'est tout petit
	CameraFrameMetadata cameraFrameMetadata;

	volatile boolean hasSize;
	int width, height;
	
	/**
	 * Une image ne doit pas être construite directement.
	 * @see Application.getImage
	 */
	Image(File path) {
		this.path = path;
		this.cameraFrame = new SoftReference<CameraFrame>(null);
		this.cameraFrameLockCount = 0;
		this.cameraFrameLocked = null;
	}

	// FIXME: maintenant ça peut retourner null 
	public ImageDisplayMetaDataInfo getImageDisplayMetaDataInfo()
	{
		CameraFrameMetadata cfm = getMetadata();
		
		ImageDisplayMetaDataInfo result = new ImageDisplayMetaDataInfo();
		
		result.expositionDuration = cfm.getDuration();
		result.iso = cfm.getGain() != null ? (int)Math.round(cfm.getGain()) : 1600;
		result.epoch = cfm.getStartMsEpoch();
		return result;
	}
//	
//	private void loadMetadata()
//	{
//		if (hasMetadata) return;
//		pause = 1.0;
//		iso = 1600;
//		epoch = path.lastModified();
//		hasMetadata = true;
//		
//		try {
//			Metadata metadata = ImageMetadataReader.readMetadata(this.path);
//			Directory directory = metadata.getDirectory(ExifSubIFDDirectory.class);
//			if (directory != null) {
//				pause = directory.getDoubleObject(ExifSubIFDDirectory.TAG_EXPOSURE_TIME);
//				if (pause == null) pause = 1.0;
//				// ExifSubIFDDirectory.TAG_GAIN_CONTROL
//				iso = directory.getInteger(ExifSubIFDDirectory.TAG_ISO_EQUIVALENT);
//				if (iso == null) iso = 1600;
//			}
//			
//
//			for (Directory directory2 : metadata.getDirectories()) {
//			    for (Tag tag : directory2.getTags()) {
//			        System.out.println(tag);
//			    }
//			}
//			
//		} catch(Exception e) {
//			System.out.println("unable to read metadata for " + this.path);
//			e.printStackTrace();
//		}
//	}
	
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
					// TODO Auto-generated catch block
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
			result = new Couple<CameraFrame, CameraFrameMetadata>(null, new CameraFrameMetadata());
		} finally {
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
			synchronized(this) {
				if (!hasSize) {
					getCameraFrame();
				}
			}
			
		}
		return width;
	}

	public int getHeight() {
		if (!hasSize) {
			synchronized(this) {
				if (!hasSize) {
					getCameraFrame();
				}
			}
			
		}
		return height;
	}

}
