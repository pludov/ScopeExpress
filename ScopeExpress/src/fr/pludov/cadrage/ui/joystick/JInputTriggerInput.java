package fr.pludov.cadrage.ui.joystick;

import javax.swing.SwingUtilities;

import net.java.games.input.Component;
import net.java.games.input.Controller;
import fr.pludov.cadrage.utils.Couple;

public class JInputTriggerInput extends TriggerInput {
	final JInputTriggerSource jts;
	volatile Boolean status;
	
	public JInputTriggerInput(JInputTriggerSource jts, String uid) {
		super(uid);
		this.jts = jts;
	}

	@Override
	public Object getStatus() {
		return this.status;
	}

	void refreshState(Component c)
	{
		final Boolean oldStatus = this.status;
		if (c == null) {
			this.status = null;
		} else {
			this.status = c.getPollData() > c.getDeadZone();
		}
		final Boolean newStatus = this.status;
		
		if (oldStatus != this.status && (oldStatus == null || status == null || !oldStatus.equals(status)))
		{
			SwingUtilities.invokeLater(new Runnable() {
				@Override
				public void run() {
					listeners.getTarget().stateChanged(oldStatus, newStatus);					
				}
			});

		}
	}
	
	public void refreshState() {
		synchronized(jts) {
			
			Couple<Controller, Component> controler = jts.topography.get(this.getUid());
			this.refreshState(controler != null ? controler.getB() : null);
		}
	}

}
