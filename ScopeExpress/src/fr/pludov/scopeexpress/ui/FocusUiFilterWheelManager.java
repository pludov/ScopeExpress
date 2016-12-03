package fr.pludov.scopeexpress.ui;

import javax.swing.JMenuItem;

import fr.pludov.scopeexpress.camera.Camera;
import fr.pludov.scopeexpress.camera.TemperatureAdjusterTask;
import fr.pludov.scopeexpress.filterwheel.FilterWheel;
import fr.pludov.scopeexpress.scope.ascom.AscomCameraProvider;
import fr.pludov.scopeexpress.scope.ascom.AscomFilterWheelProvider;
import fr.pludov.scopeexpress.ui.widgets.ToolbarButton;


public class FocusUiFilterWheelManager extends DeviceManager<FilterWheel>{

	static class Labels extends DeviceManager.Labels {
		Labels() {
			CHOOSE = "Choisir une roue � filtre...";
			DISCONNECT_TOOLTIP = "D�connecter de la roue � filtre...";
			CONNECT_TOOLTIP = "Connecter � la roue � filtre...";
		}
	}
	
	public FocusUiFilterWheelManager(FocusUi focusUi) {
		super(new Labels(), "filterWheel", focusUi, new AscomFilterWheelProvider());
	}

	@Override
	JMenuItem getMenuConnecter()
	{
		return focusUi.mntmFilterWheelConnecter;
	}

	@Override
	JMenuItem getMenuDeconnecter()
	{
		return focusUi.mntmFilterWheelDeconnecter;
	}
	
	@Override
	ToolbarButton getStatusButton()
	{
		return focusUi.filterWheelButton;
	}
}
