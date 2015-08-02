package fr.pludov.scopeexpress.utils;

public abstract class SubClassListenerCollection<PARENT, CHILD> implements IWeakListenerCollection<PARENT> {

	final IWeakListenerCollection<CHILD> childListenerCollection;
	
	public SubClassListenerCollection(IWeakListenerCollection<CHILD> childListener)
	{
		this.childListenerCollection = childListener;
	}

	protected abstract CHILD createListenerFor(PARENT i);
	
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
