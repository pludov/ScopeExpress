package fr.pludov.scopeexpress.tests;

import java.io.File;
import java.io.IOException;

import fr.pludov.io.CameraFrame;
import fr.pludov.io.ImageProvider;
import fr.pludov.scopeexpress.focus.Application;
import fr.pludov.scopeexpress.focus.Image;
import fr.pludov.utils.MultiStarFinder;

public class MultiStarFinderTest {

	public static void testFile(File file) throws IOException
	{
		final Application focus = new Application();
		
		Image image = focus.getImage(file);
		CameraFrame frame = image.getCameraFrame();
		
		MultiStarFinder msf = new MultiStarFinder(frame, image, null);
		
		msf.proceed();
	}

	public static void main(String[] args) {
		File location = new File("C:\\Documents and Settings\\utilisateur\\Mes documents\\workspace\\workspace-perso\\cadrage\\tests\\localisation");
		try {
			testFile(new File(location, "M51/IMG_0068.CR2"));
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
	
}
