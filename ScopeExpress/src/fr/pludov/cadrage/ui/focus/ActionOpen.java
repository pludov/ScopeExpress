package fr.pludov.cadrage.ui.focus;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

import javax.swing.JFileChooser;
import javax.swing.filechooser.FileNameExtensionFilter;

import fr.pludov.cadrage.Cadrage;
import fr.pludov.cadrage.focus.FocusListener.ImageAddedCause;
import fr.pludov.cadrage.focus.Image;
import fr.pludov.cadrage.ui.preferences.StringConfigItem;

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
		}
		
		chooser.setMultiSelectionEnabled(true);
		
		if (chooser.showOpenDialog(focusUi.getFrmFocus()) != JFileChooser.APPROVE_OPTION) return;
		
		
		File currentDir = chooser.getCurrentDirectory();
		lastOpenLocation.set(currentDir == null ? "" : currentDir.toString());
		
		File[] files = chooser.getSelectedFiles();
		
		for(File file : files)
		{
			// Pour forcer une pseudo détection...
			Image image = new Image(file);
			focusUi.focus.addImage(image, ImageAddedCause.Explicit);
		}
	}	
}
