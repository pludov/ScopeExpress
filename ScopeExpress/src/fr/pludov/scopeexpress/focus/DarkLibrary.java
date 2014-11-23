package fr.pludov.scopeexpress.focus;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.io.FileUtils;

import fr.pludov.io.ImageProvider;
import fr.pludov.scopeexpress.ui.Configuration;
import fr.pludov.scopeexpress.ui.FocusUi;
import fr.pludov.scopeexpress.ui.utils.BackgroundTask;
import fr.pludov.scopeexpress.utils.Couple;
import fr.pludov.scopeexpress.utils.EndUserException;


public class DarkLibrary {
	/** Un dark ne doit pas exceder ce % de durée de l'image (105) */
	double durationRatio = 1.05;
	/** Un dark ne doit pas être plus chaud que ça */
	double temperatatureDltMax = 1.5;
	
	final List<Couple<DarkRequest, Image>> darkCollection;
	final File darkPath;
	
	public Image getDark(DarkRequest de)
	{
		double bestFitVal = 0.0;
		Couple<DarkRequest, Image> bestFit = null;
		for(Couple<DarkRequest, Image> candidate : darkCollection) {
			double f = fit(de, candidate.getA());
			if (Double.isNaN(f)) {
				continue;
			}
			if (f > bestFitVal) {
				bestFitVal = f;
				bestFit = candidate;
			}
			
		}
		if (bestFit != null) {
			return bestFit.getB();
		}
		return null;
	}

	public Image getDarkFor(Image image)
	{
		return getDark(new DarkRequest(image.getMetadata()));
	}
	
	/** Retourne une note entre 0 (impossiblement mauvais) et 1 (excellent) */
	private double fit(DarkRequest image, DarkRequest darkLib)
	{
		double ratio = 1.0;
		if (image.duration != null) {
			if (darkLib.duration != null) {
				// C'est linéaire, on ne veut que ça approche 1, sans dépasser.
				double durationRatio = darkLib.duration / image.duration;
				if (durationRatio > 1.05) {
					return Double.NaN;
				}
				// Aversion pour les darks trop longs
				if (durationRatio > 1.0) {
					durationRatio = durationRatio * durationRatio * durationRatio * durationRatio;
					durationRatio = 1.0 / durationRatio;
				}
				ratio *= durationRatio;
			}
		}
		if (image.temp != null) {
			if (darkLib.temp != null) {
				double tempDelta = image.temp - darkLib.temp;
				// On veut que çà approche zero, sans être négatif
				if (tempDelta < -temperatatureDltMax) {
					return Double.NaN;
				}
				// Aversion pour les dark trop chauds
				if (tempDelta < 0) tempDelta *= 4;
				tempDelta /= 6;
				
				tempDelta = Math.abs(tempDelta);
				// Pour chaque doublage de bruit température, le ratio est divisé par deux
				ratio *= Math.pow(2, -tempDelta);
			}
		}
		
		return ratio;
	}
	
	void addImage(Application application, File file)
	{
		Image dark = application.getImage(file);
		DarkRequest dr = new DarkRequest(dark.getMetadata());
		darkCollection.add(new Couple<>(dr, dark));
	}
	
	
	public DarkLibrary()
	{
		darkCollection = new ArrayList<>();
		darkPath = new File(Configuration.getApplicationDataFolder(), "darks");
		if (!darkPath.exists()) {
			darkPath.mkdir();
		}
	}
	
	/**
	 * Initialise la librairie de darks
	 */
	class InitDarkLibraryTask extends BackgroundTask
	{
		Application application;
		public InitDarkLibraryTask(Application application) {
			super("Chargement des darks");
			this.application = application;
		}
		
		@Override
		protected void proceed() throws BackgroundTaskCanceledException, Throwable {
			scanDir();
		}
		
		void scanDir() throws BackgroundTaskCanceledException
		{
			LinkedList<File> dirToCheck = new LinkedList<>();
			dirToCheck.add(darkPath);
			while(!dirToCheck.isEmpty()) {
				File dir = dirToCheck.removeFirst();
				checkInterrupted();
				if (dir.isDirectory()) {
						
					for(String fname : dir.list())
					{
						checkInterrupted();
						File file = new File(dir, fname);
						if (file.isDirectory()) {
							dirToCheck.add(file);
							continue;
						}
						
						addFile(file);
					}
				} else {
					addFile(dir);
				}
			}
		}

		private void addFile(File file) {
			if (ImageProvider.isFits(file) || ImageProvider.isCr2(file)) {
				addImage(application, file);
			}
		}
		
		@Override
		public int getResourceOpportunity() {
			return 0;
		}
	}

	/**
	 * Importe une entrée dans la librairie de darks (copie du fichier)
	 * @param images
	 * @return
	 */
	class ImportTask extends BackgroundTask
	{
		List<File> toImport;
		FocusUi focusUi;
		
		EndUserException error;
		
		ImportTask(FocusUi focusUi, List<File> files)
		{
			super("Import de darks");
			this.focusUi = focusUi;
			toImport = new ArrayList<>(files);
		}
		
		@Override
		public int getResourceOpportunity() {
			// TODO Auto-generated method stub
			return 0;
		}
		
		@Override
		protected void proceed() throws BackgroundTaskCanceledException, Throwable {
			for(File f : toImport)
			{
				try {
					setRunningDetails("Copie de " + f.getName());
					checkInterrupted();
					if (!(ImageProvider.isFits(f) || ImageProvider.isCr2(f))) {
						throw new EndUserException("Type de fichier non supporté: " + f);
					}
						
					File target = new File(darkPath, f.getName());
					target.delete();
					if (target.exists()) {
						throw new EndUserException("Le fichier " + target + " existe déjà");
					}
					FileUtils.copyFile(f, target);
					checkInterrupted();
					setRunningDetails("Import de " + f.getName());
					addImage(focusUi.getApplication(), target);
					checkInterrupted();
				} catch(EndUserException e) {
					error = e;
					break;
				} catch(Throwable t) {
					error = new EndUserException("Erreur lors de l'import de " + f.getName(), t);
				}
			}
		}
		
		@Override
		protected void onDone() {
			
			if (error != null) {
				error.report(focusUi.getMainWindow());
			}
		}
		
	}
	public BackgroundTask getImportTask(FocusUi focusUi, List<File> images) {
		return new ImportTask(focusUi, images);
	}
	
	public BackgroundTask getScanTask(Application application)
	{
		return new InitDarkLibraryTask(application);
	}
	
	private static DarkLibrary darkLibrary;
	
	public static synchronized DarkLibrary getInstance()
	{
		if (darkLibrary == null) {
			darkLibrary = new DarkLibrary();
		}
		return darkLibrary;
	}
}
