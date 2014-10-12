package fr.pludov.scopeexpress.async;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import fr.pludov.scopeexpress.Cadrage;

public abstract class AsyncOperation {

	// Demande d'annulation à prendre en compte. Arrête l'opération en cours
	boolean running;
	
	// En cas d'erreur.
	Throwable error;
	
	// Cette opération est lancée à la suite.
	AsyncOperation next;
	
	final String title;
	
	// Cette action est exécutée dans le thread swing.
	public abstract void init() throws Exception;
	
	// Run dans un thread asynchrone.
	public abstract void async() throws Exception;
	
	// Exécuté après run, dans le thread swing.
	public abstract void terminate()  throws Exception;
	
	private void reportEnd()
	{
		if (error != null) {
			error.printStackTrace();
			String message = error.getLocalizedMessage();
			if (message == null) message = error.getMessage();
			message+="\n";
			
			message = "Erreur: " + message; 
			JOptionPane.showMessageDialog(
				Cadrage.mainFrame, message, 
				title, JOptionPane.ERROR_MESSAGE);
		} else {
			if (next != null) {
				next.start();
			}
		}
	}
	
	private void reportEndFromAsync()
	{
		try {
			SwingUtilities.invokeLater(new Runnable() {
				@Override
				public void run() {
					reportEnd();
				};
				
			});
		} catch(Exception t2) {
			t2.printStackTrace();
		}
	}
	
	public final void start()
	{
		running = true;
		
		try {
			init();
		} catch(Throwable t) {
			error = t;
			running = false;
			
			reportEnd();
			return;
		}
		
		new Thread() {
			public void run() {
				try {
					async();
				} catch(Throwable t) {
					error =t;
					running = false;
					
					reportEndFromAsync();
					return;
				}
				
				try {
					SwingUtilities.invokeAndWait(new Runnable() {
						@Override
						public void run() {
							try {
								terminate();
							} catch(Throwable t) {
								error = t;
								running = false;
								reportEnd();
							}
						}
					});
				} catch(Throwable t) {
					if (running) {
						error = t;
						running = false;
						reportEndFromAsync();
					}
				}
				if (running) {
					running = false;
					reportEndFromAsync();
				}
			};
		}.start();
		
	}
	
	public void queue(AsyncOperation op)
	{
		AsyncOperation t = this;
		while(t.next != null) {
			t = t.next;
		}
		t.next = op;
	}
	
	public AsyncOperation(String title)
	{
		this.title = title;
	}
}
