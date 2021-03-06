package fr.pludov.scopeexpress.utils;

import java.awt.Component;
import java.awt.Container;
import java.awt.Window;
import java.lang.ref.WeakReference;

import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import org.apache.log4j.Logger;

import fr.pludov.scopeexpress.focus.Application;

/**
 * Cette exception est propre a �tre affich�e � l'utilisateur, sous forme de message d'erreur.
 */
public class EndUserException extends Exception {
	private static final Logger logger = Logger.getLogger(EndUserException.class);
	
	public EndUserException() {
	}

	public EndUserException(String message) {
		super(message);
	}

	public EndUserException(Throwable cause) {
		super(cause);
	}

	public EndUserException(String message, Throwable cause) {
		super(message, cause);
	}

	public void report(final Component parentComponent)
	{
		logger.info("Erreur", this);
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				Window parent;
				if (parentComponent == null) {
					parent = null;
				} else if (parentComponent instanceof Window) {
					parent = (Window) parentComponent;
				} else {
					parent = SwingUtilities.getWindowAncestor(parentComponent);
				}
						
				Container parentContainer = parent;
				if (parentContainer == null) {
					WeakReference<Container> currentDefault = defaultContainer;
					
					parentContainer = currentDefault != null ? currentDefault.get() : null;
				}
				
				String message = getMessage();
				Throwable cause = getCause();
				int cpt = 0;
				while(cause != null) {
					message += "\n";
					message += cause.getMessage();
					cause = cause.getCause();
					if (cpt++ > 20) break;
//					message += "java.library.path = " + System.getProperty("java.library.path");
				}
				JOptionPane.showMessageDialog(
						parentContainer, message, "Erreur de traitement", JOptionPane.ERROR_MESSAGE);		
			};
		});
	}
	
	
	private static volatile WeakReference<Container> defaultContainer = null;
	
	public static void setDefaultErrorParent(Container container)
	{
		if (container != null) {
			defaultContainer = new WeakReference<Container>(container);
		} else {
			container = null;
		}
	}
}
