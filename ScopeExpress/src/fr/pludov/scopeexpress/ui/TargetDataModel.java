package fr.pludov.scopeexpress.ui;

import java.util.*;

import javax.swing.*;
import javax.swing.event.*;

import fr.pludov.scopeexpress.database.DatabaseItemCollection.*;
import fr.pludov.scopeexpress.database.content.*;
import fr.pludov.scopeexpress.database.content.Root.*;
import fr.pludov.scopeexpress.utils.*;

public class TargetDataModel implements ComboBoxModel<Object>{
	final FocusUi focusUi;
	final WeakListenerOwner listenerOwner = new WeakListenerOwner(this); 
	final List<ListDataListener> listeners;
	
	Runnable newItem = new Runnable() {
		
		@Override
		public void run() {
			Target t = new Target(focusUi.database);
			// FIXME: demander le nom, proposer la position de la photo sélectionnée ?
			t.setName("Cible sans nom");
			focusUi.database.getRoot().getTargets().add(t);
			focusUi.database.getRoot().setCurrentTarget(t);
		};
		
		@Override
		public String toString() {
			return "Nouvelle...";
		};
	};
	Runnable modifyItem = new Runnable() {
		@Override
		public void run() {
			// FIXME: regarder si currentTarget est modifiable
			// Créer un dialogue de modification
		};
		
		@Override
		public String toString() {
			return "Modifier...";
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
	
	public TargetDataModel(FocusUi focusUi) {
		this.focusUi = focusUi;

		this.focusUi.database.getRoot().getTargets().listeners.addListener(this.listenerOwner, new Listener<Target>() {

			@Override
			public void itemAdded(Target content) {
				currentContent.add(content);
				ListDataEvent lde = new ListDataEvent(TargetDataModel.this, ListDataEvent.INTERVAL_ADDED, currentContent.size() - 1, currentContent.size() - 1);
				for(ListDataListener ldl : listeners)
				{
					ldl.intervalAdded(lde);
				}
			}

			@Override
			public void itemRemoved(Target content) {
				int pos = currentContent.lastIndexOf(content);
				if (pos != -1) {
					currentContent.remove(pos);
					ListDataEvent lde = new ListDataEvent(TargetDataModel.this, ListDataEvent.INTERVAL_REMOVED, pos, pos);
					for(ListDataListener ldl : listeners)
					{
						ldl.intervalRemoved(lde);
					}
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
				ListDataEvent lde = new ListDataEvent(TargetDataModel.this, ListDataEvent.CONTENTS_CHANGED, -1, -1);

				for(ListDataListener ldl : listeners)
				{
					ldl.contentsChanged(lde);
				}
			}
		});
		
		currentContent = focusUi.database.getRoot().getTargets().getContent();
		listeners = new ArrayList<>();
	}

	@Override
	public void addListDataListener(ListDataListener arg0) {
		listeners.add(arg0);
	}

	@Override
	public void removeListDataListener(ListDataListener arg0) {
		listeners.remove(arg0);
	}
	
	@Override
	public Object getElementAt(int i) {
		if (i < currentContent.size()) {
			return currentContent.get(i);
		}
		return additionals[i - currentContent.size()];
	}

	@Override
	public int getSize() {
		return currentContent.size() + additionals.length;
	}

	@Override
	public Object getSelectedItem() {
		return currentTarget;
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
		}
	}
}
