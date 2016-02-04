package fr.pludov.scopeexpress.ui.utils;

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;

/** 
 * Empile simplement des dialogue de input
 *
 */
public class DialogLayout implements LayoutManager2 {
	class ComponentState {
		final Component component;
		
		Dimension minimumSize;
		Dimension preferedSize;
		Dimension maximumSize;
		
		
		ComponentState(Component component)
		{
			this.component = component;
		}


		public void invalidate() {
			minimumSize = null;
			maximumSize = null;
			preferedSize = null;
		}


		public Dimension getMinimumSize() {
			if (true && minimumSize == null) {
				minimumSize = component.getMinimumSize();
			}
			return minimumSize;
		}
		public Dimension getMaximumSize() {
			if (true && maximumSize == null) {
				maximumSize = component.getMaximumSize();
			}
			return maximumSize;
		}
		public Dimension getPreferedSize() {
			if (true && preferedSize == null) {
				preferedSize = component.getPreferredSize();
			}
			return preferedSize;
		}
	}
	List<ComponentState> states = new ArrayList<>();
	IdentityHashMap<Component, ComponentState> childs = new IdentityHashMap<>();
	
	public DialogLayout() {
	}

	ComponentState getState(Component child)
	{
		return childs.get(child);
	}
	
	@Override
	public void addLayoutComponent(String arg0, Component arg1) {
	}

	@Override
	public void addLayoutComponent(Component arg0, Object arg1) {
		ComponentState componentState = new ComponentState(arg0);
		childs.put(arg0, componentState);
		states.add(componentState);
	}
	
	@Override
	public void layoutContainer(Container parent) {
		
		Insets insets = parent.getInsets();
		int y = insets.top;
        int x0 = insets.left;
        int parentW = parent.getWidth() - insets.left - insets.right;
        
        int cpt = 0;
		for(ComponentState cs : states) {
			int w = parentW;
			Dimension min = cs.getMinimumSize();
			if (min.getWidth() > w) {
				w = min.width;
			}
			cs.component.setBounds(new Rectangle(x0, y, w, min.height));
			if (cs.component.isVisible()) {
				y += min.height;
			}
		}
	}
	@Override
	public void removeLayoutComponent(Component arg0) {
		ComponentState componentState = childs.remove(arg0);
		states.remove(componentState);
	}


	@Override
	public float getLayoutAlignmentX(Container arg0) {
		return 0;
	}

	@Override
	public float getLayoutAlignmentY(Container arg0) {
		return 0;
	}

	@Override
	public void invalidateLayout(Container arg0) {
		for(ComponentState cs : states) {
			cs.invalidate();
		}
	}


	@Override
	public Dimension minimumLayoutSize(Container parent) {
		// La somme des hauteur, et le max des largeurs min
		int h = 0;
		int w = 0;
		
		for(ComponentState cs : states) {
			Dimension d = cs.getMinimumSize();
			if (cs.component.isVisible()) {
				h += d.height;
			}
			if (w < d.width) {
				w = d.width;
			}
		}
        Insets insets = parent.getInsets();
        w+= insets.left + insets.right;
        h+= insets.top + insets.bottom;
		return new Dimension(w, h);
	}

	@Override
	public Dimension preferredLayoutSize(Container parent) {
		Insets insets = parent.getInsets();
		// La somme des hauteur, et le max des largeurs min
		int h = insets.top;
		int w = 0;
		int cpt= 0;
		for(ComponentState cs : states) {
			Dimension d = cs.getPreferedSize();
			if (w < d.width) {
				w = d.width;
			}
			if (cs.component.isVisible()) {
				h += cs.getMinimumSize().height;
			}
		}

        w+= insets.left + insets.right;
        h+=  insets.bottom;
		return new Dimension(w, h);
	}

	@Override
	public Dimension maximumLayoutSize(Container parent) {
		// La somme des hauteur, et le max des largeurs min
		int w = 0;
		
		for(ComponentState cs : states) {
			Dimension d = cs.getPreferedSize();
			if (w < d.width) {
				w = d.width;
			}
		}

		Insets insets = parent.getInsets();
        w+= insets.left + insets.right;
        
		return new Dimension(w, Integer.MAX_VALUE);
	}

}
