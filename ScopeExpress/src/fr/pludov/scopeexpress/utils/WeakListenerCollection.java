package fr.pludov.scopeexpress.utils;

import java.lang.ref.*;
import java.lang.reflect.*;
import java.util.*;

import javax.swing.*;

import fr.pludov.scopeexpress.tasks.*;

/**
 * Tant que le owner est en vie vis à vis du gc, garder le listener.
 * 
 * Attention, le listener peut également avoir une référence au owner.
 * 
 * => il faut que le owner ait une référence sur le listener
 */
public class WeakListenerCollection<Interface> implements InvocationHandler, IWeakListenerCollection<Interface> {
	public enum AsyncKind { 
		// Listener invoqué dans le thread appellant
		CallerThread, 
		// Listener invoqué via SwingUtilities.invokeLatter
		SwingQueue,
		// Listener invoqué via SwingUtilities.invokeLatter si le thread appelant n'est pas le Thread swing
		SwingQueueIfRequired 
	};
	
	private static class ControlledObject<Interface>
	{
		final WeakReference<WeakListenerOwner> reference;
		// Si interface garde un lien sur object, rien de tout ça ne part à la poubelle...
		// il faudrait faire en sorte que object garde une référence sur l'interface
		final WeakReference<Interface> listener;
		
		// Lorsqu'un controlled object est retiré, il est marqué "dead"
		boolean dead;
		
		ControlledObject(WeakListenerOwner owner, Interface itf)
		{
			this.reference = new WeakReference<WeakListenerOwner>(owner);
			this.listener = new WeakReference<Interface>(itf);
		}
		
		Interface get()
		{
			return listener.get();
		}
		
		WeakListenerOwner getOwner()
		{
			return this.reference.get();
		}
	}
	
	private Class<? extends Interface> clazz;
	private Interface proxy;
	// private List<WeakReference<Interface>> listeners;
	private List<ControlledObject<Interface>> listeners;
	
	private final AsyncKind asyncKind;
	
	public WeakListenerCollection(Class<? extends Interface> clazz)
	{
		this(clazz, false);
	}
	
	public WeakListenerCollection(Class<? extends Interface> clazz, boolean async)
	{
		this(clazz, async ? AsyncKind.SwingQueue : AsyncKind.CallerThread);
	}
	
	public WeakListenerCollection(Class<? extends Interface> clazz, AsyncKind asyncKind) {
		listeners = new ArrayList<ControlledObject<Interface>>();
		proxy = (Interface)Proxy.newProxyInstance(clazz.getClassLoader(), 
				new Class[]{clazz}, 
				this);
		this.asyncKind = asyncKind;
	}
	
	boolean isActive(Interface target)
	{
		return true;
	}
	
	@Override
	public Object invoke(Object proxy, final Method method, final Object[] args) throws Throwable 
	{
		final List<ControlledObject<Interface>> objectToNotify = getObjectsToNotify();
		
		if (asyncKind == AsyncKind.CallerThread || (asyncKind == AsyncKind.SwingQueueIfRequired && SwingUtilities.isEventDispatchThread()))
		{
			Object result = null;
			for(ControlledObject<Interface> ct : objectToNotify) {
				Object r = notifyControlledObject(ct, method, args);
				if (r != null && result == null) {
					result = r;
				}
			}
			return result;
		} else {
			SwingUtilities.invokeLater(new Runnable() {
				@Override
				public void run() {
					for(ControlledObject<Interface> ct : objectToNotify) {
						notifyControlledObject(ct, method, args);
					}
				}

			});
			return null;
		}
//		
//		Object result = null;
//		for(final Interface target  : objectToNotify)
//		{
//			if (!isActive(target)) {
//				continue;
//			}
//			
//			try {
//				if (asyncKind == AsyncKind.CallerThread || (asyncKind == AsyncKind.SwingQueueIfRequired && SwingUtilities.isEventDispatchThread())) {
//					Object r = method.invoke(target, args);
//					if (r != null && result == null) {
//						result = r;
//					}
//				} else {
//					SwingUtilities.invokeLater(new Runnable() {
//						@Override
//						public void run() {
//							try {
//								method.invoke(target, args);
//							} catch (Throwable e) {
//								if ((e instanceof InvocationTargetException) && (e.getCause() instanceof TaskInterruptedException)) {
//									// Ignorer simplement
//								} else {
//									e.printStackTrace();
//								}
//							}
//						}
//					});
//				}
//			} catch(Throwable t) {
//				t.printStackTrace();
//			}
//		}
//		
//		return result;
	}

	private Object notifyControlledObject(ControlledObject<Interface> ct, Method method, Object[] args) {
		Interface target = ct.get();
		if (target == null) {
			return null;
		}
		if (!isActive(target)) {
			return null;
		}
		
		try {
			return method.invoke(target, args);
		} catch (Throwable e) {
			if ((e instanceof InvocationTargetException) && (e.getCause() instanceof TaskInterruptedException)) {
				// Ignorer simplement
			} else {
				e.printStackTrace();
			}
			return null;
		}
	}
	

	private List<ControlledObject<Interface>> getObjectsToNotify() {
		synchronized(listeners) {
			List<ControlledObject<Interface>> objectToNotify = new ArrayList<>();
			// 1 - vider les références vide
			for(Iterator<ControlledObject<Interface>> it = listeners.iterator(); it.hasNext();)
			{
				ControlledObject<Interface> ref = it.next();
				Interface target = ref.get();
				if (target == null) {
					it.remove();
				} else {
					WeakListenerOwner owner = ref.getOwner();
					if (owner == null || owner.dead) {
						it.remove();
					} else {
						objectToNotify.add(ref);
					}
				}
			}
			return objectToNotify;
		}
	}
	
	@Override
	public void addListener(WeakListenerOwner owner, Interface i)
	{
		synchronized(listeners) {
			removeListener(owner);
			listeners.add(new ControlledObject<Interface>(owner, i));
			owner.addListener(i);
		}
	}
	
	@Override
	public void removeListener(WeakListenerOwner owner)
	{
		synchronized(listeners) {
			for(Iterator<ControlledObject<Interface>> it = listeners.iterator(); it.hasNext();)
			{
				ControlledObject<Interface> ref = it.next();
				WeakListenerOwner refOwner = ref.getOwner();
				if (refOwner == null || refOwner == owner) {
					ref.dead = true;
					if (refOwner != null) refOwner.removeListener(ref.listener.get());
					it.remove();
				}
			}
		}
	}
	
	public Interface getTarget()
	{
		return proxy;
	}
}
