package fr.pludov.scopeexpress.script;

import java.io.*;
import java.net.*;

public class Utils {

	public static String getPath(Class<?> path)
	{
		ClassLoader classLoader = path.getClassLoader();
		
		String classFile = path.getName();
		classFile = classFile.replaceAll("\\.", "/");
		classFile += ".class";
		File file;
		try {
			file = new File(classLoader.getResource(classFile).toURI());
		} catch (URISyntaxException e) {
			throw new RuntimeException("unable to locate " + classFile, e);
		}
		return file.getParentFile().getAbsolutePath();
	
	}
}
