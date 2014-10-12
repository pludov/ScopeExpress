package fr.pludov.scopeexpress.ui.joystick;

import fr.pludov.scopeexpress.utils.ActivableListener;

public interface JoystickListener extends ActivableListener {
	void triggered();
	
	@Override
	boolean isActive();
}
