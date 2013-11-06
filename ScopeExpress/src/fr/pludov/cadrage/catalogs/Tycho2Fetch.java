package fr.pludov.cadrage.catalogs;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.zip.GZIPInputStream;

import org.apache.commons.io.IOUtils;

import fr.pludov.cadrage.ui.focus.Configuration;
import fr.pludov.cadrage.ui.utils.SyncTask;
import fr.pludov.cadrage.utils.EndUserException;

public abstract class Tycho2Fetch extends SyncTask{

	public Tycho2Fetch() {
		super("Téléchargement du catalogue Tycho-2");
	}
	
	class Tycho2File {
		final String targetName;
		final String [] files;
		
		Tycho2File(String targetName, String ... files)
		{
			this.targetName = targetName;
			this.files = files;
		}
	}
	
	static String [] interval(String format, int min, int max)
	{
		List<String> result = new ArrayList<String>();
		for(int i = min; i <= max; ++i) {
			result.add(String.format(format, i));
		}
		return result.toArray(new String[max - min + 1]);
	}
	
	File result;
	
	@Override
	protected void run() throws EndUserException, InterruptedException {
		String root = "http://cdsarc.u-strasbg.fr/vizier/ftp/cats/I/259/";
		
		List<Tycho2File> lists =
			Arrays.asList(
					new Tycho2File("index.dat", "index.dat.gz"),
					new Tycho2File("suppl_1.dat", "suppl_1.dat.gz"),
					new Tycho2File("suppl_2.dat", "suppl_2.dat.gz"),
					new Tycho2File("tyc2.dat", interval("tyc2.dat.%02d.gz", 0, 19)));
		
		try {
			File targetDir = new File(Configuration.getApplicationDataFolder(), "tycho2");
			targetDir.mkdirs();
			
			int fileCount = 0;
			for(Tycho2File t2f : lists)
			{
				fileCount += t2f.files.length;
				
			}
			
			int currentCount = 0;
			for(Tycho2File t2f : lists)
			{
				File target = new File(targetDir, t2f.targetName);
				result = target;
				FileOutputStream fos = new FileOutputStream(target);
				try 
				{
					checkInterrupted();
					long position = 0;
					for(String file : t2f.files)
					{
						setProgress("Téléchargement de " + file, currentCount++, fileCount);
						URL url = new URL(root + file);
					    URLConnection urlConnection = url.openConnection();
					    try {
							InputStream is = urlConnection.getInputStream();
							try {
								ReadableByteChannel rbc = Channels.newChannel(new GZIPInputStream(is));
								position += fos.getChannel().transferFrom(rbc, position, Long.MAX_VALUE - position);
								checkInterrupted();
							} finally {
								is.close();
							}
					    } finally {
					    	IOUtils.close(urlConnection);
					    }

					}
				} finally {
					fos.close();
				}
			}
		} catch(Throwable t) {
			checkInterrupted();
			if (t instanceof InterruptedException) throw (InterruptedException)t;
			throw new EndUserException(t);
		}
	}

	@Override
	protected abstract void done();

	public File getResult() {
		return result;
	}
		
}
