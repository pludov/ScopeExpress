package fr.pludov.scopeexpress.ui;

import javax.swing.*;

import fr.pludov.scopeexpress.camera.*;
import fr.pludov.scopeexpress.drivers.gphoto.*;
import fr.pludov.scopeexpress.scope.ascom.*;
import fr.pludov.scopeexpress.ui.widgets.*;


public class FocusUiCameraManager extends DeviceManager<Camera>{

	static class Labels extends DeviceManager.Labels {
		Labels() {
			CHOOSE = "Choisir une camera...";
			DISCONNECT_TOOLTIP = "Déconnecter de la caméra...";
			CONNECT_TOOLTIP = "Connecter à la caméra...";
		}
	}
	
	TemperatureAdjusterTask currentAdjuster;
	
	public FocusUiCameraManager(FocusUi focusUi) {
		super(new Labels(), "camera", focusUi, 
				new AscomCameraProvider(),
				new GPhotoCameraProvider());
	}

	@Override
	JMenuItem getMenuConnecter()
	{
		return focusUi.mntmConnecterCamera;
	}

	@Override
	JMenuItem getMenuDeconnecter()
	{
		return focusUi.mntmDconnecterCamera;
	}
	
	@Override
	ToolbarButton getStatusButton()
	{
		return focusUi.cameraButton;
	}

	public TemperatureAdjusterTask getCurrentAdjuster() {
		return currentAdjuster;
	}
	
	void setCurrentAdjuster(TemperatureAdjusterTask tat)
	{
		TemperatureAdjusterTask previous = currentAdjuster;
		currentAdjuster = tat;
		if (previous != null) {
			previous.listeners.removeListener(this.listenerOwner);
			previous.cancel();
		}
		if (currentAdjuster != null) {
			currentAdjuster.listeners.addListener(this.listenerOwner, new TemperatureAdjusterTask.Listener() {
				@Override
				public void onStatusChanged() {
					if (currentAdjuster.getStatus().isClosed()) {
						setCurrentAdjuster(null);
					}
				}
			});
			currentAdjuster.start(getDevice());
		}
		
	}
}
