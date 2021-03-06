package fr.pludov.scopeexpress.ui;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import javax.swing.JFileChooser;
import javax.swing.TransferHandler;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;

import org.apache.log4j.Logger;

import fr.pludov.scopeexpress.focus.Image;
import fr.pludov.scopeexpress.focus.MosaicListener;
import fr.pludov.scopeexpress.focus.MosaicListener.ImageAddedCause;
import fr.pludov.scopeexpress.ui.preferences.StringConfigItem;

public class ActionOpen implements ActionListener {
	private static final Logger logger = Logger.getLogger(ActionOpen.class);
	
	public final StringConfigItem lastOpenLocation = new StringConfigItem(ActionOpen.class, "lastOpenLocation", "");
	public final StringConfigItem lastFilter = new StringConfigItem(ActionOpen.class, "lastOpenFilter", "");
	
	FocusUi focusUi;
	JFileChooser chooser;
	
	List<FileNameExtensionFilter> filters;
	
	public ActionOpen(FocusUi focusUi) {
		this.focusUi = focusUi;
		this.filters = Arrays.asList(
				new FileNameExtensionFilter("Canon raw", "cr2", "crw"),
				new FileNameExtensionFilter("FITS", "fit", "fits")
		);
				
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		
		if (chooser == null) {
			String lastOpenLocationValue = lastOpenLocation.get();
			File currentDir = "".equals(lastOpenLocationValue) ? null : new File(lastOpenLocationValue);
			chooser = new JFileChooser(currentDir);
			for(FileNameExtensionFilter filter : filters) {
				chooser.addChoosableFileFilter(filter);
			}
			String currentExt = lastFilter.get();
			if (currentExt != null && !"".equals(currentExt)) {
				if (currentExt.equals("*")) {
					chooser.setFileFilter(chooser.getAcceptAllFileFilter());
				} else {
					for(FileNameExtensionFilter filter : filters) {
						if (filter.getExtensions()[0].equals(currentExt)) {
							chooser.setFileFilter(filter);
						}
					}
				}
			}
		}
		
		chooser.setMultiSelectionEnabled(true);
		
		try {
			if (chooser.showOpenDialog(focusUi.getFrmFocus()) != JFileChooser.APPROVE_OPTION) return;
		} finally {
			// Retenir l'extension
			FileFilter currentFilter = chooser.getFileFilter();
			if (currentFilter instanceof FileNameExtensionFilter) {
				FileNameExtensionFilter fi = (FileNameExtensionFilter) currentFilter;
				lastFilter.set(fi.getExtensions()[0]);
			} else {
				if (currentFilter.equals(chooser.getAcceptAllFileFilter())) {
					lastFilter.set("*");
				} else {
					logger.warn("unknown filter:" + currentFilter);
				}
			}
			
			// Retenir aussi le r�pertoire
			File currentDir = chooser.getCurrentDirectory();
			lastOpenLocation.set(currentDir == null ? "" : currentDir.toString());
		}
		
		
		openFiles(Arrays.asList(chooser.getSelectedFiles()));
	}

	private void openFiles(List<File> files) {
		for(File file : files)
		{
			// Pour forcer une pseudo d�tection...
			Image image = focusUi.getApplication().getImage(file);
			focusUi.mosaic.addImage(image, MosaicListener.ImageAddedCause.Explicit);
		}
	}
	
	private class OpenTransferHandler extends TransferHandler
	{
		public OpenTransferHandler() {
		}
		public boolean canImport(TransferHandler.TransferSupport support) {
			if (!support.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
				return false;
			}

			support.setDropAction(LINK);

			return true;
		}

		public boolean importData(TransferHandler.TransferSupport support) {
			if (!canImport(support)) {
				return false;
			}

			Transferable t = support.getTransferable();

			try {
				List<File> l = (List<File>)t.getTransferData(DataFlavor.javaFileListFlavor);

				openFiles(l);

			} catch (UnsupportedFlavorException e) {
				return false;
			} catch (IOException e) {
				return false;
			}

			return true;
		}
	}
	
	public TransferHandler createTransferHandler()
	{
		return new OpenTransferHandler(); 
	}
}
