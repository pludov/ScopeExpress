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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;

import javax.swing.AbstractButton;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFileChooser;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileNameExtensionFilter;

import org.apache.log4j.Logger;

import fr.pludov.cadrage.Cadrage;
import fr.pludov.cadrage.focus.MosaicListener;
import fr.pludov.cadrage.focus.MosaicListener.ImageAddedCause;
import fr.pludov.cadrage.ui.preferences.StringConfigItem;
import fr.pludov.cadrage.utils.EndUserException;
import fr.pludov.cadrage.utils.Ransac;
import fr.pludov.external.apt.AptComm;

public class ActionMonitor implements ActionListener {
	private static final Logger logger = Logger.getLogger(ActionMonitor.class);

	public final StringConfigItem lastMonitorLocation = new StringConfigItem(ActionOpen.class, "lastMonitorLocation", "");

	final FocusUi focusUi;
	File currentMonitoringPath;
	Thread monitoringThread;
	List<WeakReference<AbstractButton>> onOffMenus;
	List<WeakReference<JButton>> shootButtons;
	
	public ActionMonitor(FocusUi focusUi) {
		this.focusUi = focusUi;
		this.currentMonitoringPath = null;
		this.monitoringThread = null;
		this.onOffMenus = new ArrayList<WeakReference<AbstractButton>>();
		this.shootButtons = new ArrayList<WeakReference<JButton>>();
	}

	public void addPopupMenu(AbstractButton item)
	{
		onOffMenus.add(new WeakReference<AbstractButton>(item));
		item.addActionListener(this);
		
		refreshMenus();
	}
	
	public void makeShootButton(JButton button)
	{
		this.shootButtons.add(new WeakReference<JButton>(button));
		button.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
//				File exec = new File("dll/aptclick.exe");
//				if (!exec.exists()) {
//					logger.error("File not found: " + exec);
//					JOptionPane.showMessageDialog(
//							null, "file not found: " + exec, 
//							"file not found: " + exec, JOptionPane.ERROR_MESSAGE);
//					return;
//				}
//				try {
//					Runtime.getRuntime().exec(exec.getAbsolutePath());
//				} catch(Throwable t) {
//					logger.error("Unable to exec : " + exec.getAbsolutePath(), t);
//				}
				
				
				try {
					AptComm.getInstance().shoot();
				} catch (IOException e1) {
					new EndUserException(e1).report(focusUi.getFrmFocus());
				}
			}
		});
		refreshShootButtonStatus(button);
	}
	
	private void refreshShootButtonStatus(JButton shootButton)
	{
		shootButton.setEnabled(currentMonitoringPath != null);	
	}

	private void refreshMonitoringMenuStatus(AbstractButton jmenu) {
		jmenu.setToolTipText(currentMonitoringPath != null ?
					"Arrêter la surveillance du répertoire" :
					"Surveiller un répertoire");
		jmenu.setIcon(
				currentMonitoringPath != null ?
				new ImageIcon(FocusUiDesign.class.getResource("/fr/pludov/cadrage/ui/resources/icons/media-playback-pause-7.png"))
				:
				new ImageIcon(FocusUiDesign.class.getResource("/fr/pludov/cadrage/ui/resources/icons/media-playback-start-7.png"))
			);
	}

	public void refreshMenus()
	{
		for(Iterator<WeakReference<JButton>> it = shootButtons.iterator(); it.hasNext(); )
		{
			WeakReference<JButton> wr = it.next();
			JButton button = wr.get();
			if (button == null) {
				it.remove();
				continue;
			}
			refreshShootButtonStatus(button);
		}
		
		for(Iterator<WeakReference<AbstractButton>> it = onOffMenus.iterator(); it.hasNext(); )
		{
			WeakReference<AbstractButton> wr = it.next();
			AbstractButton jmenu = wr.get();
			if (jmenu == null) {
				it.remove();
				continue;
			}
			refreshMonitoringMenuStatus(jmenu);	
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
	
	private class Monitor
	{
		/// list des fichiers trouvés lors d'une ronde
		List<File> newItems;
		/// Liste des fichiers retirés lors d'une ronde.
		Set<File> removed;
		
		List<DirMonitor> directories = new ArrayList<DirMonitor>();
		File root;
		
		long previousRefresh = 0;
		
		private class DirMonitor
		{
			final File directory;
			final Set<String> known = new HashSet<String>();
			boolean visited;
			
			DirMonitor(File where)
			{
				this.directory = where;
				this.visited = false;
			}

			private void visit()
			{
				visited = true;
				String [] list = directory.list();	
				if (list == null) list = new String[0];
				
				Set<String> removedFName = new HashSet<String>(known);
				
				for(String fileName : list)
				{
					if (fileName.equals(".")) continue;
					if (fileName.equals("..")) continue;

					removedFName.remove(fileName);
					if (!known.add(fileName)) continue;
					
					File child = new File(directory, fileName);
					
					if (child.isDirectory()) {
						directories.add(new DirMonitor(child));
					} else {
						newItems.add(child);
					}
				}
				
				for(String r : removedFName)
				{
					removed.add(new File(directory, r));
				}
			}
		}
		
		
		void refresh()
		{
			newItems = new ArrayList<File>();
			removed = new HashSet<File>();
			
			long currentRefreshTime = System.currentTimeMillis();
			
			for(int i = 0; i < directories.size(); ++i)
			{
				DirMonitor dir = directories.get(i);
				
				if (dir.visited && dir.directory.lastModified() < previousRefresh) {
					continue;
				}
				
				dir.visit();
			}
			
			
			// On prend 2 secondes de marges (en cas de lag sur un FS réseau)
			previousRefresh = currentRefreshTime - 2000;
			
			// supprimer toutes les entrées disparues
			if (!removed.isEmpty()) {
				for(ListIterator<DirMonitor> dirIt = directories.listIterator(); dirIt.hasNext();)
				{
					DirMonitor dir = dirIt.next();
					if (removed.contains(dir.directory)) {
						dirIt.remove();
					}
				}
			}
		}
		
		void setRoot(File newRoot)
		{
			if (root != null && root.equals(newRoot)) {
				return;
			}
			
			
			directories.clear();
			newItems = null;
			removed = null;
			
			root = newRoot;
			
			directories.add(new DirMonitor(root));
			
			refresh();
		}
	}
	
	private void doMonitor() throws MonitoringStoppedException, InterruptedException
	{
		Monitor m = new Monitor();
		
		while(true)
		{
			m.setRoot(getCurrentPath());

			final File currentSearchPath = m.root;
			
			m.refresh();

			for(final File newItem : m.newItems)
			{
				if (!newItem.getName().toLowerCase().matches(".*\\.(cr.|fit|fits)")) {
					continue;
				}

				logger.info("Detected new file : " + newItem);						
				Thread.sleep(250);
				
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
						
						addImage(newItem);
					}
				});
			}
			
			synchronized(this)
			{
				getCurrentPath();
				wait(125);
				getCurrentPath();
			}
		}
		
//		File path = null;
//		Set<String> known = new HashSet<String>();
//		
//		while(true)
//		{
//			File newPath = getCurrentPath();
//			
//			String [] list = newPath.list();
//			
//			if (!newPath.equals(path))
//			{
//				path = newPath;
//				known.clear();
//				
//				if (list != null) {
//					for(String item : path.list())
//					{
//						known.add(item);
//					}
//				}
//			} else {
//				// On trouve les nouveaux fichiers, on les ajoutes, et on dort
//				if (list != null) {
//					for(final String item : list)
//					{
//						if (!known.add(item)) continue;
//						
//						if (!item.toLowerCase().matches(".*\\.cr.")) {
//							continue;
//						}
//
//						final File currentSearchPath = path;
//						final File newItem = new File(currentSearchPath, item);
//						logger.info("Detected new file : " + newItem);						
//						// Essaye de locker le fichier
//						
//						boolean locked = false;
//						while(!locked && newItem.exists())
//						{
//							RandomAccessFile raf = null;
//							
//							try {
//								raf = new RandomAccessFile(newItem, "rw");
//								FileChannel fileChannel = raf.getChannel();
//								fileChannel.lock();
//								locked = true;
//								logger.info("File locked : " + newItem);
//							} catch(FileNotFoundException e) {
//								logger.debug("File probably already locked", e);
//								synchronized(this)
//								{
//									getCurrentPath();
//									wait(125);
//									getCurrentPath();
//								}				
//								continue;
//							} catch(IOException e) {
//								logger.warn("Failed to lock " + newItem, e);
//								break;
//							} catch(Throwable e) {
//								logger.error("General error with " + newItem, e);
//								break;
//							} finally {
//								if (raf != null) {
//									try {
//										raf.close();
//									} catch (IOException e) {
//										logger.warn("Failed to close", e);
//									}
//								}
//							}
//						}
//						if (!locked) continue;
//						SwingUtilities.invokeLater(new Runnable() {
//							@Override
//							public void run() {
//								// Vérifier que le répertoire est toujours en cours de monitoring
//								synchronized(ActionMonitor.this) {
//									if (ActionMonitor.this.currentMonitoringPath == null || 
//											!ActionMonitor.this.currentMonitoringPath.equals(currentSearchPath))
//									{
//										return;
//									}
//								}
//								
//								addImage(newItem);
//							}
//						});
//
//						break;
//					}
//				}
//		
//				synchronized(this)
//				{
//					getCurrentPath();
//					wait(125);
//					getCurrentPath();
//				}
//			}
//		}

	}
	
	public void addImage(File newItem)
	{
		fr.pludov.cadrage.focus.Image image = focusUi.getApplication().getImage(newItem);
		focusUi.getMosaic().addImage(image, MosaicListener.ImageAddedCause.AutoDetected);
		
		FindStarTask task = new FindStarTask(focusUi.getMosaic(), image);
		focusUi.getApplication().getBackgroundTaskQueue().addTask(task);
		
		CorrelateTask correlate = new CorrelateTask(focusUi.getMosaic(), focusUi.astrometryParameter.getParameter(), image);
		focusUi.getApplication().getBackgroundTaskQueue().addTask(correlate);	
	}
}
