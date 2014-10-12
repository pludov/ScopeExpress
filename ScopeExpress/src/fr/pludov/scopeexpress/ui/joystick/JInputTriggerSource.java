package fr.pludov.scopeexpress.ui.joystick;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.log4j.Logger;

import fr.pludov.astrometry.AstrometryProcess;
import fr.pludov.scopeexpress.utils.Couple;
import fr.pludov.scopeexpress.utils.IdentityHashSet;
import net.java.games.input.Component;
import net.java.games.input.Controller;
import net.java.games.input.ControllerEnvironment;
import net.java.games.input.DirectInputEnvironmentPlugin;
import net.java.games.input.EventQueue;

public class JInputTriggerSource implements TriggerSource {
	public static final Logger logger = Logger.getLogger(JInputTriggerSource.class);
	ControllerEnvironment ci;
	
	final Map<String, WeakReference<JInputTriggerInput>> triggers;
	final Map<String, Couple<Controller, Component>> topography;
	
	public JInputTriggerSource() {
		triggers = new HashMap<String, WeakReference<JInputTriggerInput>>();
		topography = new LinkedHashMap<String, Couple<Controller,Component>>();
		
		Thread t = new Thread("Joystick polling") {
			public void run() {
				while(true) {
					pollUsed();
					try {
						Thread.sleep(20);
					} catch(InterruptedException e) {
						return;
					} catch(Throwable t) {
					}
				}
			}
			
		};
		t.start();
	}

	public synchronized TriggerInput getTriggerInput(String uid)
	{
		buildEnvironnment();
		TriggerInput ti = null;
		WeakReference<JInputTriggerInput> wr = triggers.get(uid);
		if (wr != null) {
			ti = wr.get();
		}
		
		if (ti == null) {
			JInputTriggerInput result = new JInputTriggerInput(this, uid);
			result.refreshState();
			ti = result;
			triggers.put(uid, new WeakReference<JInputTriggerInput>(result));
		}
		return ti;
	}

	private synchronized JInputTriggerInput getExistingTrigger(String uid)
	{
		JInputTriggerInput ti = null;
		WeakReference<JInputTriggerInput> wr = triggers.get(uid);
		if (wr != null) {
			ti = wr.get();
			if (ti == null) {
				triggers.remove(uid);
			}
		}
		return ti;
	}
	
	
	
	@Override
	public synchronized List<TriggerInput> scan() {
		// FIXME: on ne devrait pas avoir à faire ça...
		ci = null;
		buildEnvironnment();
		List<TriggerInput> result = new ArrayList<TriggerInput>();
		for(String uid : this.topography.keySet())
		{
			result.add(getTriggerInput(uid));
		}
		return result;
	}
	
	
	private synchronized void buildEnvironnment()
	{
		if (ci != null) return;
		Set<String> oldActiveTriggers = new HashSet<String>(this.topography.keySet());
		this.topography.clear();
		
		ci = new DirectInputEnvironmentPlugin();
		for(Controller c : ci.getControllers())
		{
			if (isOfInterest(c)) {
				for(Component comp : c.getComponents())
				{
					if (isOfInterest(comp)) {
						String uid = buildUid(c, comp);
						this.topography.put(uid, new Couple<Controller, Component>(c, comp));
						JInputTriggerInput i = getExistingTrigger(uid);
						if (i != null) {
							i.refreshState(comp);
						}
					}
				}
			}
		}
		
		oldActiveTriggers.removeAll(this.topography.keySet());
		for(String inactiveTrigger : oldActiveTriggers)
		{
			JInputTriggerInput i = getExistingTrigger(inactiveTrigger);
			if (i == null) continue;
			i.refreshState(null);
		}
	}
	
	private synchronized void pollUsed()
	{
		// Retirer les entrées obsolètes
		for(Iterator<Map.Entry<String, WeakReference<JInputTriggerInput>>> it = this.triggers.entrySet().iterator(); it.hasNext(); )
		{
			Entry<String, WeakReference<JInputTriggerInput>> entry = it.next();
			if (entry.getValue().get() == null) {
				it.remove();
			}
		}
		IdentityHashSet<Controller> toUpdate = new IdentityHashSet<Controller>(); 
		for(String uid : this.triggers.keySet())
		{
			Couple<Controller, Component> c = this.topography.get(uid);
			if (c == null) continue;
			toUpdate.add(c.getA());
		}
		for(Controller c : toUpdate)
		{
			try {
				if (!c.poll()) {
					throw new Exception("polling failed");
				}
			} catch(Throwable t) {
				logger.error("Polling failed for " + c.getName(), t);
				removeFromTopology(c);
				continue;
			}
		}
		

		for(Iterator<Entry<String, Couple<Controller, Component>>> it = this.topography.entrySet().iterator(); it.hasNext();)
		{
			Entry<String, Couple<Controller, Component>> e = it.next();
			if (!toUpdate.contains(e.getValue().getA())) continue;
			JInputTriggerInput jiti = getExistingTrigger(e.getKey());
			if (jiti == null) continue;
			
			jiti.refreshState(e.getValue().getB());
			
		}
		
	}
	
	private synchronized void removeFromTopology(Controller c) {
		for(Iterator<Entry<String, Couple<Controller, Component>>> it = this.topography.entrySet().iterator(); it.hasNext();)
		{
			Entry<String, Couple<Controller, Component>> e = it.next();
			if (e.getValue().getA() == c) {
				JInputTriggerInput input = getExistingTrigger(e.getKey());
				if (input != null) {
					input.refreshState(null);
				}
				it.remove();
			}
		}
	}

	private synchronized Couple<Controller, Component> getComponent(TriggerInput ti)
	{
		buildEnvironnment();
		return this.topography.get(ti.getUid());
	}
	
	private boolean isOfInterest(Controller controller) 
	{
		return 
	        controller.getType() == Controller.Type.STICK || 
	        controller.getType() == Controller.Type.GAMEPAD || 
	        controller.getType() == Controller.Type.WHEEL ||
	        controller.getType() == Controller.Type.FINGERSTICK;
		
	}
	
	private boolean isOfInterest(Component c)
	{
		return !c.isAnalog();
	}
	
	private String buildUid(Controller cont, Component comp)
	{
		return cont.getName() + "#" + comp.getName();
	}
}
