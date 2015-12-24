package fr.pludov.scopeexpress.ui;

import javax.swing.JMenuItem;

import fr.pludov.scopeexpress.openphd.OpenPhdDevice;
import fr.pludov.scopeexpress.openphd.OpenPhdProvider;
import fr.pludov.scopeexpress.scope.ascom.AscomFilterWheelProvider;
import fr.pludov.scopeexpress.ui.FocusUiFilterWheelManager.Labels;
import fr.pludov.scopeexpress.ui.widgets.ToolbarButton;

public class FocusUiGuiderManager extends DeviceManager<OpenPhdDevice> {

	static class Labels extends DeviceManager.Labels {
		Labels() {
			CHOOSE = "Choisir...";
			DISCONNECT_TOOLTIP = "Déconnecter de Phd Guiding...";
			CONNECT_TOOLTIP = "Connecter à Phd Guiding...";
		}
	}
	
	public FocusUiGuiderManager(FocusUi focusUi)
	{
		super(new OpenPhdProvider(), new Labels(), "openphd", focusUi);
	}
	
	@Override
	ToolbarButton getStatusButton() {
		return focusUi.guidingButton;
	}

	@Override
	JMenuItem getMenuConnecter() {
		return focusUi.mntmGuiderConnecter;
	}
	
	@Override
	JMenuItem getMenuDeconnecter() {
		return focusUi.mntmGuiderDeconnecter;
	}
}
