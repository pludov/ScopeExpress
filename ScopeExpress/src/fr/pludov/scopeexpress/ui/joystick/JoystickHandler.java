package fr.pludov.scopeexpress.ui.joystick;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.regex.Pattern;

import fr.pludov.scopeexpress.ui.ConfigurationEdit;
import fr.pludov.scopeexpress.ui.FocusUi;
import fr.pludov.scopeexpress.ui.preferences.ConfigItem;
import fr.pludov.scopeexpress.ui.preferences.EnumConfigItem;
import fr.pludov.scopeexpress.ui.speech.Speaker;
import fr.pludov.scopeexpress.ui.speech.SpeakerProvider;
import fr.pludov.scopeexpress.ui.utils.Utils;
import fr.pludov.scopeexpress.utils.EndUserException;
import fr.pludov.scopeexpress.utils.IdentityHashSet;
import fr.pludov.scopeexpress.utils.WeakActivableListenerCollection;
import fr.pludov.scopeexpress.utils.WeakListenerCollection;
import fr.pludov.scopeexpress.utils.WeakListenerOwner;

public class JoystickHandler {
	public static final String actionSuffix = ".cmd";
	
	final FocusUi focusUi;
	final IdentityHashMap<TriggerInput, ButtonAction> inputs;
	final TriggerSource ts = TriggerSourceProvider.getInstance();
	final WeakListenerOwner owner = new WeakListenerOwner(this);
	
	final EnumMap<ButtonAction, WeakActivableListenerCollection<JoystickListener>> listeners;
	
	public JoystickHandler(FocusUi focusUi) {
		this.focusUi = focusUi;
		this.inputs = new IdentityHashMap<TriggerInput, ButtonAction>();
		this.listeners = new EnumMap<ButtonAction, WeakActivableListenerCollection<JoystickListener>>(ButtonAction.class);
		for(ButtonAction ba : ButtonAction.values())
		{
			this.listeners.put(ba, new WeakActivableListenerCollection<JoystickListener>(JoystickListener.class));
		}
		
		reload();
	}
	
	public void reload()
	{
		IdentityHashSet<TriggerInput> oldHandles = new IdentityHashSet<TriggerInput>();
		oldHandles.addAll(inputs.keySet());
		inputs.clear();
		
		List<String> uidToHandles = ConfigItem.getKeyCollection(ButtonAction.class, Pattern.compile("(.*)" + Pattern.quote(actionSuffix)));
		for(String key : uidToHandles)
		{
			final String uid = key.substring(0, key.length() - actionSuffix.length());
			EnumConfigItem<ButtonAction> configItem = getButtonConfigItem(uid);
			ButtonAction action = configItem.get();
			if (action == null) continue;

			final TriggerInput ti = ts.getTriggerInput(uid);
			if (!oldHandles.remove(ti)) {
				ti.listeners.addListener(owner, new TriggerInputListener() {
					@Override
					public void stateChanged(Object oldValue, Object newValue) {
						if (oldValue == null || newValue == null) return;
						if (newValue instanceof Boolean && (Boolean)newValue) {
							changeForUid(ti);
						}
					}
				});	
			}
			inputs.put(ti, action);
		}
		
		for(TriggerInput old : oldHandles)
		{
			old.listeners.removeListener(this.owner);
		}
	}
	
	void changeForUid(TriggerInput uid)
	{
		ConfigurationEdit configEdit = Utils.getVisibleDialog(focusUi.getMainWindow(), ConfigurationEdit.class);
		if (configEdit != null && configEdit.getJoystickConfPanel().isShowing()) {
			return;
		}
		if (Utils.hasModalDialog(focusUi.getMainWindow()))
		{
			try {
				Speaker speaker;
				speaker = SpeakerProvider.getSpeaker();

				if (speaker != null) {
					speaker.enqueue("Erreur");
				}
			} catch (EndUserException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return;
		}
		ButtonAction ba = this.inputs.get(uid);
		
		if (ba == null) return;
		WeakListenerCollection<JoystickListener> listeners = this.listeners.get(ba);
		listeners.getTarget().triggered();
	}
	
	public WeakListenerCollection<JoystickListener> getListeners(ButtonAction ba)
	{
		return listeners.get(ba);
	}

	static EnumConfigItem<ButtonAction> getButtonConfigItem(String uid)
	{
		return new EnumConfigItem<ButtonAction>(ButtonAction.class, uid + actionSuffix, ButtonAction.class, null);
	}
}
