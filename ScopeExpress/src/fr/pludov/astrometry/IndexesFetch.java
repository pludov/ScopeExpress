package fr.pludov.astrometry;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.GZIPInputStream;

import org.apache.commons.io.IOUtils;

import fr.pludov.cadrage.ui.focus.Configuration;
import fr.pludov.cadrage.ui.utils.SyncTask;
import fr.pludov.cadrage.utils.EndUserException;

public class IndexesFetch extends SyncTask{

	final String root = "http://data.astrometry.net/4200/";
	File targetDir;
	
	Integer level;
	
	long currentDownload;
	long totalDownload;
	
	public IndexesFetch(File targetDir, Integer level)
	{
		super("Téléchargement des indexes astrometry.net");
		this.targetDir = targetDir;
		this.level = level;
	}
	
	boolean exists(String path)
	{
		File target = new File(targetDir, path);
		return target.exists();
	}

	void download(String file) throws IOException, InterruptedException
	{
		String title = "Téléchargement de " + file;
		checkInterrupted();

		File target = new File(targetDir, file);
		File tempTarget = new File(targetDir, file + ".tmp");
		reportProgress(title);
		
		long position = 0;
		FileOutputStream fos = new FileOutputStream(tempTarget);
		try 
		{
			URL url = new URL(root + file);
		    URLConnection urlConnection = url.openConnection();
		    try {
				InputStream is = urlConnection.getInputStream();
				try {
					ReadableByteChannel rbc = Channels.newChannel(is);
					long xfered = fos.getChannel().transferFrom(rbc, position, Long.MAX_VALUE - position);
					position += xfered;
					currentDownload += xfered;
					checkInterrupted();
					reportProgress(title);
				} finally {
					is.close();
				}
		    } finally {
		    	IOUtils.close(urlConnection);
		    }
		} finally {
			fos.close();
		}
	
		tempTarget.renameTo(target);
	}

	private void reportProgress(String title) {
		
		setProgress(title, (int)(this.currentDownload / 1024), (int)(this.totalDownload / 1024));
	}
	
	@Override
	protected void run() throws InterruptedException, EndUserException {
		try {
			if (targetDir == null) {
				targetDir = new File(Configuration.getApplicationDataFolder(), "astronet");
				targetDir.mkdirs();
			}
			
			currentDownload = 0;
			totalDownload = 0;
			List<IndexFile> todo = new ArrayList<IndexFile>();
			for(IndexFile idf : files)
			{
				if (idf.level >= this.level) {
					if (!exists(idf.name)) {
						totalDownload += idf.size;
						todo.add(idf);
					}
				}
			}
			
			for(IndexFile idf : todo)
			{
				download(idf.name);
			}
			
		} catch(Throwable t) {			
			checkInterrupted();
			if (t instanceof InterruptedException) throw (InterruptedException)t;
			throw new EndUserException(t);
		}

	}

	@Override
	protected void done() {
		
	}
	
	public File getResult() {
		return targetDir;
	}

	private static class IndexFile {
		final String name;
		final long size;
		final int level;
		
		IndexFile(long size, String name)
		{
			this.level = Integer.parseInt(name.substring(8, 10));
			this.name = name;
			this.size = size;
		}
	}
	
	public static String [] getMandatoryIndexes()
	{
		List<String> result = new ArrayList<String>();
		for(IndexFile idf : files)
		{
			if (idf.level >= 9) {
				result.add(idf.name);
			}
		}
		return result.toArray(new String[result.size()]);
	}
	
	static IndexFile [] files = { 
		new IndexFile(132480,   "index-4219.fits"),
		new IndexFile(164160,   "index-4218.fits"),
		new IndexFile(213120,   "index-4217.fits"),
		new IndexFile(339840,   "index-4216.fits"),
		new IndexFile(596160,   "index-4215.fits"),
		new IndexFile(1100160,  "index-4214.fits"),
		new IndexFile(2157120,  "index-4213.fits"),
		new IndexFile(4167360,  "index-4212.fits"),
		new IndexFile(8017920,  "index-4211.fits"),
		new IndexFile(20517120, "index-4210.fits"),
		new IndexFile(41178240, "index-4209.fits"),
		new IndexFile(81835200, "index-4208.fits"),

		new IndexFile(13786560, "index-4207-00.fits"),
		new IndexFile(13786560, "index-4207-01.fits"),
		new IndexFile(13786560, "index-4207-02.fits"),
		new IndexFile(13786560, "index-4207-03.fits"),
		new IndexFile(13786560, "index-4207-04.fits"),
		new IndexFile(13786560, "index-4207-05.fits"),
		new IndexFile(13786560, "index-4207-06.fits"),
		new IndexFile(13786560, "index-4207-07.fits"),
		new IndexFile(13786560, "index-4207-08.fits"),
		new IndexFile(13786560, "index-4207-09.fits"),
		new IndexFile(13786560, "index-4207-10.fits"),
		new IndexFile(13786560, "index-4207-11.fits"),

		new IndexFile(27354240, "index-4206-00.fits"),
		new IndexFile(27354240, "index-4206-01.fits"),
		new IndexFile(27354240, "index-4206-02.fits"),
		new IndexFile(27354240, "index-4206-03.fits"),
		new IndexFile(27354240, "index-4206-04.fits"),
		new IndexFile(27354240, "index-4206-05.fits"),
		new IndexFile(27354240, "index-4206-06.fits"),
		new IndexFile(27354240, "index-4206-07.fits"),
		new IndexFile(27354240, "index-4206-08.fits"),
		new IndexFile(27354240, "index-4206-09.fits"),
		new IndexFile(27354240, "index-4206-10.fits"),
		new IndexFile(27354240, "index-4206-11.fits"),
		
		new IndexFile(54924480, "index-4205-00.fits"),
		new IndexFile(54924480, "index-4205-01.fits"),
		new IndexFile(54924480, "index-4205-02.fits"),
		new IndexFile(54924480, "index-4205-03.fits"),
		new IndexFile(54924480, "index-4205-04.fits"),
		new IndexFile(54924480, "index-4205-05.fits"),
		new IndexFile(54924480, "index-4205-06.fits"),
		new IndexFile(54924480, "index-4205-07.fits"),
		new IndexFile(54924480, "index-4205-08.fits"),
		new IndexFile(54924480, "index-4205-09.fits"),
		new IndexFile(54924480, "index-4205-10.fits"),
		new IndexFile(54924480, "index-4205-11.fits"),

		new IndexFile(27354240, "index-4204-00.fits"),
		new IndexFile(27354240, "index-4204-01.fits"),
		new IndexFile(27354240, "index-4204-02.fits"),
		new IndexFile(27354240, "index-4204-03.fits"),
		new IndexFile(27354240, "index-4204-04.fits"),
		new IndexFile(27354240, "index-4204-05.fits"),
		new IndexFile(27354240, "index-4204-06.fits"),
		new IndexFile(27354240, "index-4204-07.fits"),
		new IndexFile(27354240, "index-4204-08.fits"),
		new IndexFile(27345600, "index-4204-09.fits"),
		new IndexFile(27354240, "index-4204-10.fits"),
		new IndexFile(27354240, "index-4204-11.fits"),
		new IndexFile(27354240, "index-4204-12.fits"),
		new IndexFile(27354240, "index-4204-13.fits"),
		new IndexFile(27354240, "index-4204-14.fits"),
		new IndexFile(27354240, "index-4204-15.fits"),
		new IndexFile(27354240, "index-4204-16.fits"),
		new IndexFile(27354240, "index-4204-17.fits"),
		new IndexFile(27354240, "index-4204-18.fits"),
		new IndexFile(27354240, "index-4204-19.fits"),
		new IndexFile(27354240, "index-4204-20.fits"),
		new IndexFile(27354240, "index-4204-21.fits"),
		new IndexFile(27354240, "index-4204-22.fits"),
		new IndexFile(27354240, "index-4204-23.fits"),
		new IndexFile(27354240, "index-4204-24.fits"),
		new IndexFile(27354240, "index-4204-25.fits"),
		new IndexFile(27354240, "index-4204-26.fits"),
		new IndexFile(27345600, "index-4204-27.fits"),
		new IndexFile(27354240, "index-4204-28.fits"),
		new IndexFile(27354240, "index-4204-29.fits"),
		new IndexFile(27354240, "index-4204-30.fits"),
		new IndexFile(27354240, "index-4204-31.fits"),
		new IndexFile(27354240, "index-4204-32.fits"),
		new IndexFile(27354240, "index-4204-33.fits"),
		new IndexFile(27354240, "index-4204-34.fits"),
		new IndexFile(27354240, "index-4204-35.fits"),
		new IndexFile(27354240, "index-4204-36.fits"),
		new IndexFile(27354240, "index-4204-37.fits"),
		new IndexFile(27354240, "index-4204-38.fits"),
		new IndexFile(27354240, "index-4204-39.fits"),
		new IndexFile(27354240, "index-4204-40.fits"),
		new IndexFile(27354240, "index-4204-41.fits"),
		new IndexFile(27354240, "index-4204-42.fits"),
		new IndexFile(27354240, "index-4204-43.fits"),
		new IndexFile(27354240, "index-4204-44.fits"),
		new IndexFile(27354240, "index-4204-45.fits"),
		new IndexFile(27354240, "index-4204-46.fits"),
		new IndexFile(27354240, "index-4204-47.fits"),
	};
}
