package fr.pludov.cadrage.ui.joystick;

import fr.pludov.cadrage.utils.ActivableListener;

public interface JoystickListener extends ActivableListener {
	void triggered();
	
	@Override
	boolean isActive();
}
