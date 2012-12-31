package fr.pludov.cadrage.focus;

import java.io.File;
import java.io.IOException;
import java.lang.ref.SoftReference;
import java.lang.ref.WeakReference;

import org.apache.log4j.Logger;

import com.drew.imaging.ImageMetadataReader;
import com.drew.metadata.Directory;
import com.drew.metadata.Metadata;
import com.drew.metadata.Tag;
import com.drew.metadata.exif.ExifSubIFDDirectory;

import fr.pludov.cadrage.ImageDisplayParameter.ImageDisplayMetaDataInfo;
import fr.pludov.cadrage.async.WorkStepResource;
import fr.pludov.io.CameraFrame;
import fr.pludov.io.ImageProvider;

public class Image implements WorkStepResource {
	private static final Logger logger = Logger.getLogger(Image.class);
	
	final File path;
	
	SoftReference<CameraFrame> cameraFrame;
	CameraFrame cameraFrameLocked;
	int cameraFrameLockCount;
	
	// Valide uniquement si cameraFrame a été obtenu !
	
	boolean hasMetadata;
	Metadata metadata;
	Double pause;
	Integer iso;
	
	public Image(File path) {
		this.path = path;
		this.cameraFrame = new SoftReference<CameraFrame>(null);
		this.cameraFrameLockCount = 0;
		this.cameraFrameLocked = null;
	}

	public ImageDisplayMetaDataInfo getImageDisplayMetaDataInfo()
	{
		ImageDisplayMetaDataInfo result = new ImageDisplayMetaDataInfo();
		loadMetadata();
		result.expositionDuration = this.pause;
		result.iso = this.iso;
		return result;
	}
	
	private void loadMetadata()
	{
		if (hasMetadata) return;
		pause = 1.0;
		iso = 1600;
		hasMetadata = true;
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
			if (cameraFrameLocked == null) {
				cameraFrameLocked = cameraFrame.get();
			}
			if (cameraFrameLocked != null) {
				cameraFrameLockCount++;
				return;
			}
		}
		CameraFrame loaded;
		try {
			System.out.println("Loading " + path);
			loaded = ImageProvider.readImage(path);
		} catch(IOException ex) {
			ex.printStackTrace();
			loaded = new CameraFrame();
		}
		
		synchronized(this)
		{
			this.cameraFrame = new SoftReference<CameraFrame>(loaded);
			this.cameraFrameLocked = loaded;
			this.cameraFrameLockCount++;
		}
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

	public File getPath() {
		return path;
	}

	@Override
	public String toString() {
		return this.path.getName();
	}
}
