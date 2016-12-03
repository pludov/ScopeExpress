package fr.pludov.scopeexpress.ui;

import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JMenuItem;

import fr.pludov.scopeexpress.focuser.Focuser;
import fr.pludov.scopeexpress.scope.ascom.AscomFocuserProvider;
import fr.pludov.scopeexpress.ui.utils.Utils;
import fr.pludov.scopeexpress.ui.utils.Utils.WindowBuilder;
import fr.pludov.scopeexpress.ui.widgets.ToolbarButton;

public class FocusUiFocuserManager extends DeviceManager<Focuser>{

	static class Labels extends DeviceManager.Labels {
		Labels() {
			CHOOSE = "Choisir un focuser...";
			DISCONNECT_TOOLTIP = "Déconnecter du focuser...";
			CONNECT_TOOLTIP = "Connecter au focuser...";
		}
	}
	
	public FocusUiFocuserManager(FocusUi app) {
		super(new Labels(), "Focuser", app, new AscomFocuserProvider());
	}

	@Override
	JMenuItem getMenuConnecter() {
		return focusUi.mntmFocuserConnecter;
	}

	@Override
	JMenuItem getMenuDeconnecter() {
		return focusUi.mntmFocuserDeconnecter;
	}

	@Override
	ToolbarButton getStatusButton() {
		return focusUi.focuserButton;
	}
	
	@Override
	public void addActionListener()
	{
		super.addActionListener();
		focusUi.mntmAutofocus.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent arg0) {
				AutoFocusDialog dialog = Utils.openDialog(focusUi.getMainWindow(), new WindowBuilder<AutoFocusDialog>() {
					@Override
					public AutoFocusDialog build(Window w) {
						return new AutoFocusDialog(w, focusUi);
					}
					
					@Override
					public boolean isInstance(Window w) {
						return w instanceof AutoFocusDialog;
					}
				});
				dialog.setVisible(true);
			}
		});
	}

}
