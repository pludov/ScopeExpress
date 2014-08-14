package fr.pludov.cadrage.utils;

public class WeakActivableListenerCollection<Interface extends ActivableListener> extends WeakListenerCollection<Interface> {
	
	public WeakActivableListenerCollection(Class<? extends Interface> clazz)
	{
		super(clazz);
	}
	
	@Override
	boolean isActive(Interface target) {
		return target.isActive();
	}
}
