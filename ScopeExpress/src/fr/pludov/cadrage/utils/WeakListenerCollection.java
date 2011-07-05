package fr.pludov.cadrage.utils;

import java.lang.ref.WeakReference;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class WeakListenerCollection<Interface> implements InvocationHandler {
	private Class<? extends Interface> clazz;
	private Interface proxy;
	private List<WeakReference<Interface>> listeners;
	
	public WeakListenerCollection(Class<? extends Interface> clazz)
	{
		listeners = new ArrayList<WeakReference<Interface>>();
		proxy = (Interface)Proxy.newProxyInstance(clazz.getClassLoader(), 
				new Class[]{clazz}, 
				this);
	}
	
	@Override
	public Object invoke(Object proxy, Method method, Object[] args) throws Throwable 
	{
		List<Interface> objectToNotify = new ArrayList<Interface>();
		// 1 - vider les références vide
		for(Iterator<WeakReference<Interface>> it = listeners.iterator(); it.hasNext();)
		{
			WeakReference<Interface> ref = it.next();
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
	
	public void addListener(Interface i)
	{
		removeListener(i);
		listeners.add(new WeakReference<Interface>(i));
	}
	
	public void removeListener(Interface i)
	{
		for(Iterator<WeakReference<Interface>> it = listeners.iterator(); it.hasNext();)
		{
			WeakReference<Interface> ref = it.next();
			Interface target = ref.get();
			if (target == null || target == i) {
				it.remove();
			}
		}
	}
	
	public Interface getTarget()
	{
		return proxy;
	}
}
