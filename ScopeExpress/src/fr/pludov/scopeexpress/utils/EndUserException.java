package fr.pludov.scopeexpress.utils;

import java.awt.Component;
import java.awt.Container;
import java.awt.Window;
import java.lang.ref.WeakReference;

import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import org.apache.log4j.Logger;

import fr.pludov.scopeexpress.Cadrage;

/**
 * Cette exception est propre a être affichée à l'utilisateur, sous forme de message d'erreur.
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

	public void report(Component parentComponent)
	{
		Window parent;
		if (parentComponent == null) {
			parent = null;
		} else if (parentComponent instanceof Window) {
			parent = (Window) parentComponent;
		} else {
			parent = SwingUtilities.getWindowAncestor(parentComponent);
		}
		reportToContainer(parent);
	}

	private void reportToContainer(final Container forParentContainer)
	{
		logger.info("Erreur", this);
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				Container parentContainer = forParentContainer;
				if (parentContainer == null) {
					WeakReference<Container> currentDefault = defaultContainer;
					
					parentContainer = currentDefault != null ? currentDefault.get() : null;
				}
				
				String message = getMessage();
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
