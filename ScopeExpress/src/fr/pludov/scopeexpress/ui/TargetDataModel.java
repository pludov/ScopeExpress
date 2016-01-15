package fr.pludov.scopeexpress.ui;

import java.util.*;

import javax.swing.*;
import javax.swing.event.*;

import fr.pludov.scopeexpress.database.DatabaseItemCollection.*;
import fr.pludov.scopeexpress.database.content.*;
import fr.pludov.scopeexpress.database.content.Root.*;
import fr.pludov.scopeexpress.ui.utils.*;
import fr.pludov.scopeexpress.utils.*;

public class TargetDataModel implements ComboBoxModel<Object>{
	final FocusUi focusUi;
	final WeakListenerOwner listenerOwner = new WeakListenerOwner(this); 
	final List<ListDataListener> listeners;
	
	JComboPlaceHolder placeHolder = new JComboPlaceHolder("Pas de cible...");
	
	Runnable newItem = new Runnable() {
		
		@Override
		public void run() {
			Target t = new Target(focusUi.database);
			t.setCreationDate(System.currentTimeMillis());
			
			TargetPanel.createNewDialog(focusUi, t, 1.0);
		};
		
		@Override
		public String toString() {
			return "Nouvelle...";
		};
	};
	Runnable modifyItem = new Runnable() {
		@Override
		public void run() {
			if (currentTarget == null) {
				return;
			}
			// Créer un dialogue de modification
			TargetPanel.createModifyDialog(focusUi, currentTarget, 1.0);
		};
		
		@Override
		public String toString() {
			return "Détails...";
		};
	};
	Runnable deleteItem = new Runnable() {
		@Override
		public void run() {
			// FIXME: confirmer la suppression
			Target toRemove = currentTarget;
			if (toRemove == null) {
				return;
			}
			if (focusUi.database.getRoot().getCurrentTarget() == toRemove) {
				focusUi.database.getRoot().setCurrentTarget(null);
			}
			
			focusUi.database.getRoot().getTargets().remove(toRemove);
			
			focusUi.database.asyncSave();
		};
		
		@Override
		public String toString() {
			return "Supprimer...";
		};
	};
	Runnable [] additionals = new Runnable[]
	{
			newItem,
			modifyItem,
			deleteItem
	};
	List<Target> currentContent;
	Target currentTarget;
	boolean placeHolderVisible = true;
	
	
	public TargetDataModel(FocusUi focusUi) {
		this.focusUi = focusUi;

		this.focusUi.database.getRoot().getTargets().listeners.addListener(this.listenerOwner, new Listener<Target>() {

			@Override
			public void itemAdded(Target content) {
				
				currentContent.add(content);
				fireEvent(ListDataEvent.INTERVAL_ADDED, (placeHolderVisible ? 1 : 0) + currentContent.size() - 1, (placeHolderVisible ? 1 : 0) + currentContent.size() - 1);
			}

			@Override
			public void itemRemoved(Target content) {
				int pos = currentContent.lastIndexOf(content);
				if (pos != -1) {
					currentContent.remove(pos);
					fireEvent(ListDataEvent.INTERVAL_REMOVED, (placeHolderVisible ? 1 : 0) + pos, (placeHolderVisible ? 1 : 0) + pos);
				}
			}
		});
		this.focusUi.database.getRoot().currentTargetListeners.addListener(this.listenerOwner, new CurrentTargetListener() {
			
			@Override
			public void currentTargetChanged(Target newValue) {
				if (currentTarget == newValue) {
					return;
				}
				
				currentTarget = newValue;
				fireEvent(ListDataEvent.CONTENTS_CHANGED, -1, -1);
			}
		});

		currentContent = focusUi.database.getRoot().getTargets().getContent();
		currentTarget = focusUi.database.getRoot().getCurrentTarget();
		listeners = new ArrayList<>();
	}
	
	private void fireEvent(int kind, int min, int max)
	{
		ListDataEvent lde = new ListDataEvent(TargetDataModel.this, ListDataEvent.INTERVAL_ADDED, min, max);
		for(ListDataListener ldl : listeners)
		{
			switch(kind) {
			case ListDataEvent.INTERVAL_ADDED:
				ldl.intervalAdded(lde);
				break;
			case ListDataEvent.INTERVAL_REMOVED:
				ldl.intervalRemoved(lde);
				break;
			case ListDataEvent.CONTENTS_CHANGED:
				ldl.contentsChanged(lde);
				break;
			}
		}
	}

	@Override
	public void addListDataListener(ListDataListener arg0) {
		listeners.add(arg0);
	}

	@Override
	public void removeListDataListener(ListDataListener arg0) {
		listeners.remove(arg0);
	}
	
	boolean hasPlaceHolder()
	{
		return currentContent.isEmpty();
	}
	
	@Override
	public Object getElementAt(int i) {
		if (placeHolderVisible) {
			if (i == 0) {
				return placeHolder;
			}
			i--;
		}
			
		
		if (i < currentContent.size()) {
			return currentContent.get(i);
		}
		i -= currentContent.size();
		
		return additionals[i];
	}

	@Override
	public int getSize() {
		return currentContent.size() + additionals.length + (placeHolderVisible ? 1 : 0);
	}

	@Override
	public Object getSelectedItem() {
		if (currentTarget != null) {
			return currentTarget;
		} else if (placeHolderVisible) {
			return placeHolder;
		} else {
			return null;
		}
	}

	@Override
	public void setSelectedItem(final Object arg0) {
		if (arg0 instanceof Runnable) {
			SwingUtilities.invokeLater(new Runnable() {
				@Override
				public void run() {
					((Runnable)arg0).run();
				}
			});
		} else if (arg0 instanceof Target) {
			focusUi.database.getRoot().setCurrentTarget((Target) arg0);
		} else {
			focusUi.database.getRoot().setCurrentTarget(null);
		}
	}
}
