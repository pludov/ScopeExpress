package fr.pludov.scopeexpress.utils;

import java.lang.reflect.*;

public final class SubClassListenerCollection<PARENT, CHILD extends PARENT> implements IWeakListenerCollection<PARENT> {

	final IWeakListenerCollection<CHILD> childListenerCollection;
	final Class<PARENT> parentClass;
	final Class<CHILD> childClass;
	
	public SubClassListenerCollection(IWeakListenerCollection<CHILD> childListener, Class<PARENT> parentClazz, Class<CHILD> childClazz)
	{
		this.childListenerCollection = childListener;
		this.parentClass = parentClazz;
		this.childClass = childClazz;
	}

	
	
	protected final CHILD createListenerFor(PARENT i) {
		InvocationHandler callParent = new InvocationHandler() {
			@Override
			public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
				if (method.getDeclaringClass().isAssignableFrom(parentClass)) {
					return method.invoke(i, args);
				}
				return null;
			}
		
		};
		
		CHILD proxy = (CHILD)Proxy.newProxyInstance(childClass.getClassLoader(), 
				new Class[]{childClass}, 
				callParent);
		return proxy;
	}
	
	@Override
	public void addListener(WeakListenerOwner owner, PARENT i) {
		CHILD childListener = createListenerFor(i);
		childListenerCollection.addListener(owner, childListener);
	}

	@Override
	public void removeListener(WeakListenerOwner owner) {
		childListenerCollection.removeListener(owner);
	}
}
