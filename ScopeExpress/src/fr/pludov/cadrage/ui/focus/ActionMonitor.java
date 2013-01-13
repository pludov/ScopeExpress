package fr.pludov.cadrage.ui.focus;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFileChooser;
import javax.swing.JMenuItem;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileNameExtensionFilter;

import fr.pludov.cadrage.Cadrage;
import fr.pludov.cadrage.focus.MosaicListener;
import fr.pludov.cadrage.focus.MosaicListener.ImageAddedCause;
import fr.pludov.cadrage.ui.preferences.StringConfigItem;

public class ActionMonitor implements ActionListener {
	public final StringConfigItem lastMonitorLocation = new StringConfigItem(ActionOpen.class, "lastMonitorLocation", "");

	final FocusUi focusUi;
	File currentMonitoringPath;
	Thread monitoringThread;
	List<WeakReference<JMenuItem>> menus;
	
	public ActionMonitor(FocusUi focusUi) {
		this.focusUi = focusUi;
		this.currentMonitoringPath = null;
		this.monitoringThread = null;
		this.menus = new ArrayList<WeakReference<JMenuItem>>();
	}

	public void addPopupMenu(JMenuItem item)
	{
		menus.add(new WeakReference<JMenuItem>(item));
		item.addActionListener(this);
		
		refreshMenus();
	}
	
	public void refreshMenus()
	{
		for(Iterator<WeakReference<JMenuItem>> it = menus.iterator(); it.hasNext(); )
		{
			WeakReference<JMenuItem> wr = it.next();
			JMenuItem jmenu = wr.get();
			if (jmenu == null) {
				it.remove();
				continue;
			}
			jmenu.setText(currentMonitoringPath != null ?
						"Arrêter la surveillance du répertoire" :
							"Surveiller un répertoire");	
		}
	}
	
	@Override
	public void actionPerformed(ActionEvent e) {
		if (currentMonitoringPath == null) {
			JFileChooser chooser = new JFileChooser();
			chooser.setMultiSelectionEnabled(false);
			chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
			chooser.setDialogTitle("Importer automatiquement les nouvelles images d'un dossier");
	
			String lastOpenLocationValue = lastMonitorLocation.get();
			File currentDir = "".equals(lastOpenLocationValue) ? null : new File(lastOpenLocationValue);
			chooser.setSelectedFile(currentDir);
			
			if (chooser.showOpenDialog(focusUi.getFrmFocus()) != JFileChooser.APPROVE_OPTION) return;
			
			currentDir = chooser.getSelectedFile();
			lastMonitorLocation.set(currentDir.toString());

			
			setStatus(currentDir);
		} else {
			setStatus(null);
		}	
	}
	
	synchronized void setStatus(File monitoringPath)
	{
		synchronized(this)
		{
			if (monitoringPath == null) {
				// Arreter le monitoring.
				this.currentMonitoringPath = null;
			} else {
				// On va monitorer ce chemin
				this.currentMonitoringPath = monitoringPath;
				if (this.monitoringThread == null) {
					this.monitoringThread = new Thread("Folder monitoring") {
						@Override
						public void run() {
							try {
								doMonitor();
							} catch(MonitoringStoppedException e){
								
							} catch(InterruptedException e) {
								
							}
						}
					};
					this.monitoringThread.start();
				}
			}
			notifyAll();
		}
		
		refreshMenus();
	}

	static class MonitoringStoppedException extends Exception
	{
		
	}
	
	private File getCurrentPath() throws MonitoringStoppedException {
		synchronized(this)
		{
			if (this.currentMonitoringPath == null) {
				this.monitoringThread = null;
				throw new MonitoringStoppedException();
			}
			
			return this.currentMonitoringPath;
		}
	}
	
	private void doMonitor() throws MonitoringStoppedException, InterruptedException
	{
		File path = null;
		Set<String> known = new HashSet<String>();
		
		while(true)
		{
			File newPath = getCurrentPath();
			
			String [] list = newPath.list();
			
			if (!newPath.equals(path))
			{
				path = newPath;
				known.clear();
				
				if (list != null) {
					for(String item : path.list())
					{
						known.add(item);
					}
				}
			} else {
				// On trouve les nouveaux fichiers, on les ajoutes, et on dort
				if (list != null) {
					for(final String item : list)
					{
						if (!known.add(item)) continue;
						
						if (!item.matches(".*\\.cr.")) {
							continue;
						}

						final File currentSearchPath = path;
						SwingUtilities.invokeLater(new Runnable() {
							@Override
							public void run() {
								// Vérifier que le répertoire est toujours en cours de monitoring
								synchronized(ActionMonitor.this) {
									if (ActionMonitor.this.currentMonitoringPath == null || 
											!ActionMonitor.this.currentMonitoringPath.equals(currentSearchPath))
									{
										return;
									}
								}
								fr.pludov.cadrage.focus.Image image = focusUi.getApplication().getImage(new File(currentSearchPath, item));
								focusUi.getMosaic().addImage(image, MosaicListener.ImageAddedCause.AutoDetected);
							}
						});

						break;
					}
				}
		
				synchronized(this)
				{
					getCurrentPath();
					wait(250);
					getCurrentPath();
				}
			}
		}

	}
}
