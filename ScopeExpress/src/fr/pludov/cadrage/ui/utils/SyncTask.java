package fr.pludov.cadrage.ui.utils;

import java.awt.Dialog;
import java.awt.Frame;
import java.awt.GraphicsConfiguration;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JDialog;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

import fr.pludov.cadrage.utils.EndUserException;

public abstract class SyncTask
{
	final String windowTitle;
	
	public SyncTask(String title)
	{
		this.windowTitle = title;
	}
	
	protected abstract void run() throws InterruptedException, EndUserException;
	
	protected abstract void done();
	
	
	protected void checkInterrupted() throws InterruptedException
	{
		if (interrupted) {
			throw new InterruptedException();
		};
	}
	
	private volatile boolean interrupted = false;
	
	ModalProgressWindow window;
	Window owner;
	
	final Object titleSynchronizer = new Object();
	// Les paramètres à mettre en place (accès synchronizés sur titleSynchronizer 
	String actionTitle;
	int current, max;
	boolean updatePending;
	
	Thread t;
	
	protected void setProgress(String actionTitle, int current, int max)
	{
		synchronized(titleSynchronizer)
		{
			if (Utils.equalsWithNullity(actionTitle, this.actionTitle) && current == this.current && max == this.max) {
				return;
			}
			
			this.actionTitle = actionTitle;
			this.current = current;
			this.max = max;
			if (!updatePending) {
				updatePending = true;
				SwingUtilities.invokeLater(new Runnable() {
					public void run() {
						synchronized(SyncTask.this.titleSynchronizer) {
							window.getProgressLabel().setText(SyncTask.this.actionTitle);
							window.getProgressBar().setMaximum(SyncTask.this.max);
							window.getProgressBar().setValue(SyncTask.this.current);
							updatePending = false;
						}
					};						
				});
			}
		}
	}
	
	private void onSuccess() {
		try {
			window.getCancelButton().setEnabled(false);
			
			window.getProgressLabel().setText("Terminé avec succès");
			window.getProgressBar().setMaximum(100);
			window.getProgressBar().setMinimum(0);
			window.getProgressBar().setValue(100);
			
			done();
			
			window.getOkButton().setEnabled(true);
			window.getCancelButton().setEnabled(false);
			window.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		} catch(Throwable t) {
			onError(t);
		
		}
	}
	
	private void onError(Throwable t) {
		window.setVisible(false);
		if (!(t instanceof InterruptedException)) {
			EndUserException errorReport;
			if (t instanceof EndUserException) {
				errorReport = (EndUserException) t;
			} else {
				errorReport = new EndUserException(t);
			}
			errorReport.report(owner);
		}
		window.dispose();
	}
			
	public final void execute(Window owner)
	{
		this.owner = owner;
		window = new ModalProgressWindow(owner);
		window.setVisible(true);
		window.getOkButton().setEnabled(false);
		window.getCancelButton().setEnabled(true);
		window.setTitle(windowTitle);
		window.getProgressBar().setMaximum(100);
		window.getProgressBar().setValue(0);
		window.getProgressLabel().setText(actionTitle);
		window.getOkButton().addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				window.setVisible(false);
				window.dispose();
			}
		});
		
		window.getCancelButton().addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				window.getCancelButton().setEnabled(false);
				interrupted = true;
				if (t != null) {
					t.interrupt();
				}
			}
		});
		
		t = new Thread(windowTitle) {
			@Override
			public void run() {
				try {
					SyncTask.this.run();
					
					SwingUtilities.invokeLater(new Runnable() {
						@Override
						public void run() {
							onSuccess();
						}
					});
					
				} catch(final Throwable t) {
					SwingUtilities.invokeLater(new Runnable() {
						@Override
						public void run() {
							onError(t);
						}
					});
				}
			}
		};
		
		t.start();
	}

	private class ModalProgressWindow extends ModalProgressWindowDesign {
		private ModalProgressWindow(Window owner) {
			super(owner);
			setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
		}
	}
}