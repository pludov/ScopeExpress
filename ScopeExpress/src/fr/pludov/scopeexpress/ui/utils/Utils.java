package fr.pludov.scopeexpress.ui.utils;

import java.awt.Component;
import java.awt.Dialog.ModalityType;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

public final class Utils {

	public static boolean equalsWithNullity(Object a, Object b)
	{
		return ((a == null) == (b == null)) && (a == null || a.equals(b)); 
	}
	
	private Utils() {
		// TODO Auto-generated constructor stub
	}

	public static void addCheckboxChangeListener(JCheckBox box, final Runnable listener)
	{
		box.addChangeListener(new ChangeListener() {
			
			@Override
			public void stateChanged(ChangeEvent e) {
				listener.run();
			}	
		});
	}
	
	public static void addComboChangeListener(JComboBox field, final Runnable listener)
	{
		field.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				listener.run();
			}	
		});
		field.addFocusListener(new FocusListener() {

			@Override
			public void focusLost(FocusEvent e) {
				listener.run();
			}
			
			@Override
			public void focusGained(FocusEvent e) {
				
			}
		});
	}
	
	public static void addTextFieldChangeListener(JTextField field, final Runnable listener)
	{
		field.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				listener.run();
			}	
		});
		field.addFocusListener(new FocusListener() {
			
			@Override
			public void focusLost(FocusEvent e) {
				listener.run();
			}
			
			@Override
			public void focusGained(FocusEvent e) {
				
			}
		});
	}

	public static boolean hasModalDialog(Component c) {
		Window window;
		if (c == null) {
			window = null;
		} else if (c instanceof Window) {
			window = (Window)c;
		} else {
			window = SwingUtilities.getWindowAncestor(c);
		}
		if (window == null) return false;
		for(Window w : window.getOwnedWindows())
		{
			if (!w.isVisible()) continue;
			if (w instanceof JDialog) {
				JDialog jd = (JDialog) w;
				if (jd.getModalityType() == ModalityType.MODELESS) continue;
				return true;
			} else {
				return true;
			}
		}

		return false;
	}

	public static interface WindowBuilder<DIALOG extends JDialog> {
		DIALOG build(Window w);
		boolean isInstance(Window w);
	}
	
	public static <DIALOG extends JDialog> DIALOG getVisibleDialog(Component c, Class<DIALOG> dialogClass)
	{
		Window window;
		if (c == null) {
			window = null;
		} else if (c instanceof Window) {
			window = (Window)c;
		} else {
			window = SwingUtilities.getWindowAncestor(c);
		}
		if (window != null) {
			for(Window w : window.getOwnedWindows())
			{
				if (w.isVisible() && dialogClass.isInstance(w)) {
					return (DIALOG)w;
				}
			}
		}
		return null;
		
	}
	
	public static <DIALOG extends JDialog> DIALOG openDialog(Component c, WindowBuilder<DIALOG> builder)
	{
		Window window;
		if (c == null) {
			window = null;
		} else if (c instanceof Window) {
			window = (Window)c;
		} else {
			window = SwingUtilities.getWindowAncestor(c);
		}
		if (window != null) {
			for(Window w : window.getOwnedWindows())
			{
				if (builder.isInstance(w)) {
					return (DIALOG)w;
				}
			}
		}
		return builder.build(window);
	}
	
	public static <DIALOG extends JDialog> DIALOG openDialog(Component c, Class<? extends DIALOG> clazz)
	{
		Window window;
		if (c == null) {
			window = null;
		} else if (c instanceof Window) {
			window = (Window)c;
		} else {
			window = SwingUtilities.getWindowAncestor(c);
		}
		if (window != null) {
			for(Window w : window.getOwnedWindows())
			{
				if (clazz.isInstance(w)) {
					return (DIALOG)w;
				}
			}
		}
		try {
			return clazz.getConstructor(Window.class).newInstance(window);
		} catch(IllegalAccessException e) {
			throw new RuntimeException("Il manque un constructeur avec Window", e);
		} catch (IllegalArgumentException e) {
			throw new RuntimeException("Il manque un constructeur avec Window", e);
		} catch (SecurityException e) {
			throw new RuntimeException("Il manque un constructeur avec Window", e);
		} catch (InstantiationException e) {
			throw new RuntimeException("Il manque un constructeur avec Window", e);
		} catch (InvocationTargetException e) {
			throw new RuntimeException("Il manque un constructeur avec Window", e);
		} catch (NoSuchMethodException e) {
			throw new RuntimeException("Il manque un constructeur avec Window", e);
		}
	}
	
	
	/**
	 * Présentation d'un angle (degrés) en Heure/Minute/Seconde (pour RA)
	 * @param deg
	 * @return
	 */
	public static String formatHourMinSec(double deg)
	{
		double raHourDouble = deg * 24.0 / 360;
		boolean negate = raHourDouble < 0;
		if (negate) raHourDouble = -raHourDouble;

		int raHour, raMin;
		double raSec;
		raHour = (int)Math.floor(raHourDouble);
		raMin = (int)Math.floor((raHourDouble - raHour) * 60);
		raSec = (raHourDouble - raHour - raMin / 60.0) * 3600;
		
		if (negate) {
			return String.format(Locale.US, "-%dh %02dm %02.2fs", raHour, raMin, raSec);
		} else {
			return String.format(Locale.US, "%dh %02dm %02.2fs", raHour, raMin, raSec);
		}
	}

	/**
	 * Présentation d'un angle (degrés) en Degrés/Minute/Seconde (pour Dec)
	 * @param deg
	 * @return
	 */
	public static String formatDegMinSec(double deg)
	{
		boolean negate = deg < 0;
		if (negate) deg = -deg;

		int raHour, raMin;
		double raSec;
		raHour = (int)Math.floor(deg);
		raMin = (int)Math.floor((deg - raHour) * 60);
		raSec = (deg - raHour - raMin / 60.0) * 3600;
		
		if (negate) {
			return String.format(Locale.US, "-%d° %02d' %02.2f\"", raHour, raMin, raSec);
		} else {
			return String.format(Locale.US, "%d° %02d' %02.2f\"", raHour, raMin, raSec);
		}
	}

	public static String doubleToString(double d, int pres)
	{
		return String.format("%." + Integer.toString(pres) +"f", d);
	}

	public static String repeat(char ch, int length) {
		char[] chars = new char[length];
	    Arrays.fill(chars, ch);
	    return new String(chars);
	}

	public static File getBaseInstallationPath()
	{
		String command = System.getProperty("java.class.path");
		if (command == null) return new File(".");
		
		File commandFile = new File(command);
		if (!commandFile.exists()) return new File(".");
		
		return commandFile.getParentFile();
	}
	
	
	public static void addDllLibraryPath()
	{
		File dllDir = new File(getBaseInstallationPath(), "dll");
		if (!dllDir.exists()) {
			System.err.println("unable to locate dll dir (missing " + dllDir +")");
			return;
		}
		
		String currentPath = System.getProperty("java.library.path");
		String separator = System.getProperty("path.separator");
		if (separator == null) separator = ";";
		
		String newPath = currentPath == null || currentPath.equals("") ?
				dllDir.toString() :
					currentPath + separator + dllDir.toString();
				
		System.out.println("setting path to " + newPath);
		System.setProperty("java.library.path", newPath);
		
	}

	public static File locateDll(String string) {
		File dllDir = new File(new File(getBaseInstallationPath(), "dll"), string);
		if (!dllDir.exists()) {
			throw new RuntimeException("dll not found : " + dllDir);
		}

		return dllDir;
	}

	public static Double getDegFromHourMinSec(String input) throws NumberFormatException
	{
		// Pattern hms = Pattern.compile("\\s*(\\d+)\\s*h(?:|\\s*(\\d+)\\s*m(?:|\\s*(\\d+\\.\\d+|\\d+)\\s*s))\\s*");
		Pattern hms = Pattern.compile("\\s*(\\+|\\-|)\\s*(?:(\\d+|\\d+\\.\\d*)\\s*h|)\\s*(?:(\\d+|\\d+\\.\\d*)\\s*m|)\\s*(?:(\\d+|\\d+\\.\\d*)\\s*s|)\\s*");
		
		Matcher hmsMatcher = hms.matcher(input);
		if (hmsMatcher.matches() && (hmsMatcher.group(2) != null || hmsMatcher.group(3) != null || hmsMatcher.group(4) != null)) {
			double result = 0;
			if (hmsMatcher.group(2) != null) {
				double h = Double.parseDouble(hmsMatcher.group(2));
				result += 360 * h / 24;
			}
			if (hmsMatcher.group(3) != null) {
				double m = Double.parseDouble(hmsMatcher.group(3));
				result += 360 * m / (60 * 24);
			}
	
			if (hmsMatcher.group(4) != null) {
				double s = Double.parseDouble(hmsMatcher.group(4));
				result += 360 * s / (60 * 60 * 24);
			}
			
			if (hmsMatcher.group(1) != null && hmsMatcher.group(1).equals("-")) {
				result = -result;
			}
			
			return result;
		}
			
		try {
			return (360 / 24) * Double.parseDouble(input);
		} catch(NumberFormatException e) {
		}
		
		return null;
	}

	public static Double getDegFromDegMinSec(String input) throws NumberFormatException
	{
		Pattern deg = Pattern.compile("\\s*(\\+|\\-|)\\s*(?:(\\d+|\\d+\\.\\d*)\\s*°|)\\s*(?:(\\d+|\\d+\\.\\d*)\\s*'|)\\s*(?:(\\d+|\\d+\\.\\d*)\\s*(?:''|\")|)\\s*");
		Matcher degMatcher = deg.matcher(input);
		if (degMatcher.matches() && (degMatcher.group(2) != null || degMatcher.group(3) != null || degMatcher.group(4) != null))
		{
			double result = 0;
			if (degMatcher.group(2) != null) {
				double d = Double.parseDouble(degMatcher.group(2));
				result += d;
			}
			if (degMatcher.group(3) != null) {
				double m = Double.parseDouble(degMatcher.group(3));
				result += m / (60);
			}
	
			if (degMatcher.group(4) != null) {
				double s = Double.parseDouble(degMatcher.group(4));
				result += s / (60 * 60);
			}
			
			if (degMatcher.group(1) != null && degMatcher.group(1).equals("-")) {
				result = -result;
			}
			return result;
		}
		
		try {
			return Double.parseDouble(input);
		} catch(NumberFormatException e) {
		}
		
		return null;
	}

	public static Double getDegFromInput(String input) throws NumberFormatException
	{
		// Pattern hms = Pattern.compile("\\s*(\\d+)\\s*h(?:|\\s*(\\d+)\\s*m(?:|\\s*(\\d+\\.\\d+|\\d+)\\s*s))\\s*");
		Pattern hms = Pattern.compile("\\s*(\\+|\\-|)\\s*(?:(\\d+|\\d+\\.\\d*)\\s*h|)\\s*(?:(\\d+|\\d+\\.\\d*)\\s*m|)\\s*(?:(\\d+|\\d+\\.\\d*)\\s*s|)\\s*");
		
		Matcher hmsMatcher = hms.matcher(input);
		if (hmsMatcher.matches() && (hmsMatcher.group(2) != null || hmsMatcher.group(3) != null || hmsMatcher.group(4) != null)) {
			double result = 0;
			if (hmsMatcher.group(2) != null) {
				double h = Double.parseDouble(hmsMatcher.group(2));
				result += 360 * h / 24;
			}
			if (hmsMatcher.group(3) != null) {
				double m = Double.parseDouble(hmsMatcher.group(3));
				result += 360 * m / (60 * 24);
			}
	
			if (hmsMatcher.group(4) != null) {
				double s = Double.parseDouble(hmsMatcher.group(4));
				result += 360 * s / (60 * 60 * 24);
			}
			
			if (hmsMatcher.group(1) != null && hmsMatcher.group(1).equals("-")) {
				result = -result;
			}
			
			return result;
		}
	
		Pattern deg = Pattern.compile("\\s*(\\+|\\-|)\\s*(?:(\\d+|\\d+\\.\\d*)\\s*°|)\\s*(?:(\\d+|\\d+\\.\\d*)\\s*'|)\\s*(?:(\\d+|\\d+\\.\\d*)\\s*(?:''|\")|)\\s*");
		Matcher degMatcher = deg.matcher(input);
		if (degMatcher.matches() && (degMatcher.group(2) != null || degMatcher.group(3) != null || degMatcher.group(4) != null))
		{
			double result = 0;
			if (degMatcher.group(2) != null) {
				double d = Double.parseDouble(degMatcher.group(2));
				result += d;
			}
			if (degMatcher.group(3) != null) {
				double m = Double.parseDouble(degMatcher.group(3));
				result += m / (60);
			}
	
			if (degMatcher.group(4) != null) {
				double s = Double.parseDouble(degMatcher.group(4));
				result += s / (60 * 60);
			}
			
			if (degMatcher.group(1) != null && degMatcher.group(1).equals("-")) {
				result = -result;
			}
			return result;
		}
		
		try {
			return Double.parseDouble(input);
		} catch(NumberFormatException e) {
		}
		
		try {
			return Double.parseDouble(input);
		} catch(NumberFormatException e) {
		}
		
		return null;
	}

	/** Retourne un angle en degrès dans l'interaval -180, 180 */ 
	public static double adjustDegDiff(double d) {
		if (d < 0) {
			d = 360 - ((-d) % 360);
		} else {
			d = d % 360;
		}
		return d > 180 ? d - 360 : d;
	}
	
	public static File getApplicationSettingsFolder()
	{
		String workingDirectory;
		String OS = (System.getProperty("os.name")).toUpperCase();
		//to determine what the workingDirectory is.
		//if it is some version of Windows
		if (OS.contains("WIN"))
		{
		    //it is simply the location of the "AppData" folder
		    workingDirectory = System.getenv("AppData");
		}
		//Otherwise, we assume Linux or Mac
		else
		{
		    //in either case, we would start in the user's home directory
		    workingDirectory = System.getProperty("user.home");
		    //if we are on a Mac, we are not done, we look for "Application Support"
		    // workingDirectory += "/Library/Application Support";
		}
		
		File result = new File(workingDirectory);
		result = new File(result, ".ScopeExpress");
		return result;
	}
}
