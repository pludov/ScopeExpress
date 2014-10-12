package fr.pludov.scopeexpress.ui.utils;

import java.io.Serializable;

public class ListKey<Target, EffectiveListEntry extends ListEntry<Target, ?>> implements Serializable {
	private static final long serialVersionUID = -1249731330662232341L;
	final Target viewPort;

	ListKey(Target image) {
		this.viewPort = image;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (obj instanceof ListKey) {
			return ((ListKey<Target, EffectiveListEntry>)obj).viewPort == this.viewPort;
		}
		return super.equals(obj);
	}
	
	@Override
	public int hashCode() {
		return System.identityHashCode(viewPort);
	}

	public Target getTarget() {
		return viewPort;
	}
}