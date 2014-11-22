package fr.pludov.scopeexpress.ui.widgets;

import javax.swing.JButton;
import javax.swing.JMenuItem;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Insets;

import javax.swing.ImageIcon;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EtchedBorder;
import javax.swing.border.SoftBevelBorder;

import java.awt.event.ActionEvent;
import javax.swing.JToggleButton;

import fr.pludov.scopeexpress.ui.resources.IconProvider;
import fr.pludov.scopeexpress.ui.resources.IconProvider.IconSize;

import javax.swing.BoxLayout;

import net.miginfocom.swing.MigLayout;

import java.awt.GridLayout;
import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;

import javax.swing.SpringLayout;
import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.LayoutStyle.ComponentPlacement;

import java.awt.Dimension;

public class ToolbarButton extends AbstractIconButton {

	public ToolbarButton(String icon)
	{
		this(icon, false);
	}
	
	public ToolbarButton(String icon, boolean withPopupBton) {
		super(icon, IconSize.IconSizeButton, 28, 2, withPopupBton);
//		currentStatus = Status.DEFAULT;
//		
//		int w = 28 + (withPopupBton ? 12 : 0);
//		int h = 28;
//		setMaximumSize(new Dimension(w, h));
//		setSize(new Dimension(w, h));
//		setPreferredSize(new Dimension(w, h));
//		setMinimumSize(new Dimension(w, h));
//		setLayout(null);
//		
//		this.btnMain = new JButton("");
//		this.btnMain.setBounds(0, 0, 28, 28);
//		this.btnMain.setRolloverEnabled(true);
//		this.btnMain.setFocusable(false);
//		this.btnMain.setIcon(IconProvider.getIcon(icon, IconSize.IconSizeButton));
//		this.btnMain.setMargin(new Insets(2, 2, 2, 2));
//		add(this.btnMain);
//		
//		defaultBackground = this.btnMain.getBackground();
//		
//		if (withPopupBton) {
//			this.btnMore = new JButton("");
//			this.btnMore.setBounds(28, 0, 12, 28);
//			this.btnMore.setRolloverEnabled(true);
//			this.btnMore.setFocusable(false);
//			this.btnMore.setIcon(new ImageIcon(ToolbarButton.class.getResource("/fr/pludov/scopeexpress/ui/resources/icons/triangle1.png")));
//			this.btnMore.setMargin(new Insets(2, 2, 2, 2));
//			add(this.btnMore);
//		}
//		
//		initButtons();
	}
}
