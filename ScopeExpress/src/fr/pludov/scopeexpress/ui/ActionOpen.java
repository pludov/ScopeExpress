package fr.pludov.scopeexpress.ui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

import javax.swing.JFileChooser;
import javax.swing.filechooser.FileNameExtensionFilter;

import fr.pludov.scopeexpress.Cadrage;
import fr.pludov.scopeexpress.focus.Image;
import fr.pludov.scopeexpress.focus.MosaicListener;
import fr.pludov.scopeexpress.focus.MosaicListener.ImageAddedCause;
import fr.pludov.scopeexpress.ui.preferences.StringConfigItem;

public class ActionOpen implements ActionListener {
	public final StringConfigItem lastOpenLocation = new StringConfigItem(ActionOpen.class, "lastOpenLocation", "");
	
	FocusUi focusUi;
	JFileChooser chooser;
	
	public ActionOpen(FocusUi focusUi) {
		this.focusUi = focusUi;
	}

	@Override
	public void actionPerformed(ActionEvent e) {
	
		if (chooser == null) {
			String lastOpenLocationValue = lastOpenLocation.get();
			File currentDir = "".equals(lastOpenLocationValue) ? null : new File(lastOpenLocationValue);
			chooser = new JFileChooser(currentDir);
			chooser.setFileFilter(new FileNameExtensionFilter("Canon raw", "cr2", "crw"));
			chooser.setFileFilter(new FileNameExtensionFilter("FITS", "fit", "fits"));
		}
		
		chooser.setMultiSelectionEnabled(true);
		
		if (chooser.showOpenDialog(focusUi.getFrmFocus()) != JFileChooser.APPROVE_OPTION) return;
		
		
		File currentDir = chooser.getCurrentDirectory();
		lastOpenLocation.set(currentDir == null ? "" : currentDir.toString());
		
		File[] files = chooser.getSelectedFiles();
		
		for(File file : files)
		{
			// Pour forcer une pseudo détection...
			Image image = focusUi.getApplication().getImage(file);
			focusUi.mosaic.addImage(image, MosaicListener.ImageAddedCause.Explicit);
		}
	}	
}
