package fr.pludov.scopeexpress.ui.joystick;

import java.awt.Color;

import javax.swing.border.TitledBorder;

import fr.pludov.scopeexpress.ui.preferences.EnumConfigItem;
import fr.pludov.scopeexpress.ui.utils.Utils;
import fr.pludov.scopeexpress.utils.WeakListenerOwner;

public class JoystickBtonConfPanel extends JoystickBtonConfDesign implements PollingItem {
	private final String nothingTitle = "<Pas d'action>";
	final WeakListenerOwner owner = new WeakListenerOwner(this);
	
	final TriggerInput input;
	
	EnumConfigItem<ButtonAction> actionConfig;
	
	public JoystickBtonConfPanel(TriggerInput ctrl) {
		this.input = ctrl;
		
		((TitledBorder)this.getBorder()).setTitle(input.getUid());
		input.listeners.addListener(owner, new TriggerInputListener() {
			
			@Override
			public void stateChanged(Object oldState, Object newState) {
				update();
			}
		});
	
		this.actionConfig = JoystickHandler.getButtonConfigItem(ctrl.getUid());
		
		this.actionBox.addItem(nothingTitle);
		for(ButtonAction bi : ButtonAction.values())
		{
			this.actionBox.addItem(bi);
		}
		
		ButtonAction current = this.actionConfig.get();
		if (current == null) {
			this.actionBox.setSelectedItem(nothingTitle);
		} else {
			this.actionBox.setSelectedItem(current);
		}

		Utils.addComboChangeListener(this.actionBox, new Runnable() {
			@Override
			public void run() {
				Object cVal = actionBox.getSelectedItem();
				ButtonAction ba;
				if (!(cVal instanceof ButtonAction)) {
					ba = null;
				} else {
					ba = (ButtonAction)cVal;
				}
				
				actionConfig.set(ba);
			}
		});
		
		update();
	}
	
	@Override
	public void update() {
		Object o = input.getStatus();
		if (o == null) {
			this.panel.setBackground(Color.GRAY);
		} else {
			if ((Boolean)o) {
				this.panel.setBackground(Color.LIGHT_GRAY);
			} else {
				this.panel.setBackground(Color.RED);
			}
		}
	}
}
