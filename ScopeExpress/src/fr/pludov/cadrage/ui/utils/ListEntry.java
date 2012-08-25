package fr.pludov.cadrage.ui.utils;

import java.io.Serializable;

public class ListEntry<Target, EffectiveListEntry extends ListEntry<Target, ?>> extends ListKey<Target, EffectiveListEntry> implements Serializable
{
	private static final long serialVersionUID = -1449389197953723429L;
	Integer rowId;
	
	
	protected ListEntry(Target image) {
		super(image);
	}

	public Integer getRowId() {
		return rowId;
	}
	
	protected void addedToGenericList(GenericList<Target, EffectiveListEntry> list) {}
	protected void removedFromGenericList(GenericList<Target, EffectiveListEntry> list) {}
}