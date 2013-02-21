package fr.pludov.cadrage.ui.focus;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.lang.ref.WeakReference;
import java.nio.channels.FileChannel;
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

import org.apache.log4j.Logger;

import fr.pludov.cadrage.Cadrage;
import fr.pludov.cadrage.focus.MosaicListener;
import fr.pludov.cadrage.focus.MosaicListener.ImageAddedCause;
import fr.pludov.cadrage.ui.preferences.StringConfigItem;
import fr.pludov.cadrage.utils.Ransac;

public class ActionMonitor implements ActionListener {
	private static final Logger logger = Logger.getLogger(ActionMonitor.class);

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
								
							} catch(Throwable t) {
								logger.error("Monitoring died", t);
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
						final File newItem = new File(currentSearchPath, item);
						logger.info("Detected new file : " + newItem);						
						// Essaye de locker le fichier
						
						boolean locked = false;
						while(!locked && newItem.exists())
						{
							RandomAccessFile raf = null;
							
							try {
								raf = new RandomAccessFile(newItem, "rw");
								FileChannel fileChannel = raf.getChannel();
								fileChannel.lock();
								locked = true;
								logger.info("File locked : " + newItem);
							} catch(FileNotFoundException e) {
								logger.debug("File probably already locked", e);
								synchronized(this)
								{
									getCurrentPath();
									wait(125);
									getCurrentPath();
								}				
								continue;
							} catch(IOException e) {
								logger.warn("Failed to lock " + newItem, e);
								break;
							} catch(Throwable e) {
								logger.error("General error with " + newItem, e);
								break;
							} finally {
								if (raf != null) {
									try {
										raf.close();
									} catch (IOException e) {
										logger.warn("Failed to close", e);
									}
								}
							}
						}
						if (!locked) continue;
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
								fr.pludov.cadrage.focus.Image image = focusUi.getApplication().getImage(newItem);
								focusUi.getMosaic().addImage(image, MosaicListener.ImageAddedCause.AutoDetected);
								
								FindStarTask task = new FindStarTask(focusUi.getMosaic(), image);
								focusUi.getApplication().getBackgroundTaskQueue().addTask(task);
								
								CorrelateTask correlate = new CorrelateTask(focusUi.getMosaic(), image);
								focusUi.getApplication().getBackgroundTaskQueue().addTask(correlate);
								
							}
						});

						break;
					}
				}
		
				synchronized(this)
				{
					getCurrentPath();
					wait(125);
					getCurrentPath();
				}
			}
		}

	}
}
