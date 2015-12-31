package fr.pludov.scopeexpress.ui.utils;

import java.awt.*;

import javax.swing.*;

/** Permet d'afficher des objets dans une liste avec un titre différent de toString */
public final class ToStringListCellRenderer<OBJECT> implements ListCellRenderer<OBJECT> {
    private final ListCellRenderer originalRenderer;
    private final ToString toString;

    public ToStringListCellRenderer(final ListCellRenderer originalRenderer, final ToString toString) {
        this.originalRenderer = originalRenderer;
        this.toString = toString;
    }

    @Override
	public Component getListCellRendererComponent(final JList list,
            final OBJECT value, final int index, final boolean isSelected,
            final boolean cellHasFocus) {
        return originalRenderer.getListCellRendererComponent(list,
            toString.toString(value), index, isSelected, cellHasFocus);
    }


	public static interface ToString {
	    public String toString(Object object);
	}
}
