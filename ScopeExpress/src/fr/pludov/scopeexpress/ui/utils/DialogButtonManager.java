package fr.pludov.scopeexpress.ui.utils;

import javax.swing.*;

public class DialogButtonManager {
	final JButton okButton;

	public DialogButtonManager(JButton okButton) {
		super();
		this.okButton = okButton;
	}

	public JButton getOkButton() {
		return okButton;
	}
}
