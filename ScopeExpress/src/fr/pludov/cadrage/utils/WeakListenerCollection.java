package fr.pludov.cadrage.utils;

import java.lang.ref.WeakReference;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Tant que le owner est en vie vis à vis du gc, garder le listener.
 * 
 * Attention, le listener peut également avoir une référence au owner.
 * 
 * => il faut que le owner ait une référence sur le listener
 */
public class WeakListenerCollection<Interface> implements InvocationHandler {
	private static class ControlledObject<Interface>
	{
		WeakReference<WeakListenerOwner> reference;
		// Si interface garde un lien sur object, rien de tout ça ne part à la poubelle...
		// il faudrait faire en sorte que object garde une référence sur l'interface
		WeakReference<Interface> listener;
		
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
	
	public WeakListenerCollection(Class<? extends Interface> clazz)
	{
		listeners = new ArrayList<ControlledObject<Interface>>();
		proxy = (Interface)Proxy.newProxyInstance(clazz.getClassLoader(), 
				new Class[]{clazz}, 
				this);
	}
	
	@Override
	public Object invoke(Object proxy, Method method, Object[] args) throws Throwable 
	{
		List<Interface> objectToNotify = new ArrayList<Interface>();
		// 1 - vider les références vide
		for(Iterator<ControlledObject<Interface>> it = listeners.iterator(); it.hasNext();)
		{
			ControlledObject<Interface> ref = it.next();
			Interface target = ref.get();
			if (target == null) {
				it.remove();
			} else {
				objectToNotify.add(target);
			}
		}
		
		Object result = null;
		for(Interface target  : objectToNotify)
		{
			try {
				Object r = method.invoke(target, args);
				if (r != null && result == null) {
					result = r;
				}
			} catch(Throwable t) {
				t.printStackTrace();
			}
		}
		
		return result;
	}
	
	public void addListener(WeakListenerOwner owner, Interface i)
	{
		removeListener(owner);
		listeners.add(new ControlledObject<Interface>(owner, i));
		owner.addListener(i);
	}
	
	public void removeListener(WeakListenerOwner owner)
	{
		for(Iterator<ControlledObject<Interface>> it = listeners.iterator(); it.hasNext();)
		{
			ControlledObject<Interface> ref = it.next();
			WeakListenerOwner refOwner = ref.getOwner();
			if (refOwner == null || refOwner == owner) {
				if (refOwner != null) refOwner.removeListener(ref.listener.get());
				it.remove();
			}
		}
	}
	
	public Interface getTarget()
	{
		return proxy;
	}
}
