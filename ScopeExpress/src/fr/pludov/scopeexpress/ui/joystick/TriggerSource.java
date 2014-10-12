package fr.pludov.scopeexpress.ui.joystick;

import java.util.List;

public interface TriggerSource {
		
	public abstract List<TriggerInput> scan();
	
	public abstract TriggerInput getTriggerInput(String uid);
}
