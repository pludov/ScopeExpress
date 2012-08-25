package fr.pludov.cadrage.utils;

import java.lang.ref.WeakReference;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class WeakListenerCollection<Interface> implements InvocationHandler {
	private static class ControlledObject<Interface>
	{
		WeakReference<Object> reference;
		Interface listener;
		
		ControlledObject(Object object, Interface itf)
		{
			this.reference = new WeakReference<Object>(object);
			if (object != itf) {
				this.listener = itf;
			}
		}
		
		Interface get()
		{
			if (listener == null) {
				return (Interface)reference.get();
			} else {
				if (reference.get() != null) {
					return listener;
				}
				return null;
			}
		}
		
		Object getOwner()
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
	
	public void addListener(Object owner, Interface i)
	{
		removeListener(owner);
		listeners.add(new ControlledObject<Interface>(owner, i));
	}
	
	public void removeListener(Object owner)
	{
		for(Iterator<ControlledObject<Interface>> it = listeners.iterator(); it.hasNext();)
		{
			ControlledObject<Interface> ref = it.next();
			Object refOwner = ref.getOwner();
			if (refOwner == null || refOwner == owner) {
				it.remove();
			}
		}
	}
	
	public Interface getTarget()
	{
		return proxy;
	}
}
