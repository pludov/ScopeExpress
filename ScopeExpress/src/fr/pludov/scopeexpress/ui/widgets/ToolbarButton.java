package fr.pludov.scopeexpress.ui.widgets;

import javax.swing.JButton;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Insets;

import javax.swing.ImageIcon;
import javax.swing.border.BevelBorder;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EtchedBorder;
import javax.swing.border.SoftBevelBorder;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.JToggleButton;

import fr.pludov.scopeexpress.ui.resources.IconProvider;
import fr.pludov.scopeexpress.ui.resources.IconProvider.IconSize;
import javax.swing.BoxLayout;
import net.miginfocom.swing.MigLayout;
import java.awt.GridLayout;
import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.ColumnSpec;
import com.jgoodies.forms.layout.RowSpec;
import javax.swing.SpringLayout;
import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.LayoutStyle.ComponentPlacement;
import java.awt.Dimension;

public class ToolbarButton extends JPanel {

	public enum Status {
		DEFAULT(null, false),
		ACTIVATED(null, true),
		OK(Color.green, false),
		PENDING(Color.orange, false),
		ERROR(Color.red, false);
		
		final Color backgroundColor;
		final boolean backgroundAdjust;
		
		private Status(Color bg, boolean adj)
		{
			this.backgroundAdjust = adj;
			this.backgroundColor = bg;
		}

		public Color colorFrom(Color defaultBackground) {
			if (backgroundColor != null) return backgroundColor;
			if (backgroundAdjust) return littleBright(defaultBackground);
			return defaultBackground;
		}
	};
	
	public static interface PopupProvider {
		public JPopupMenu popup();
	};
	
	protected JButton btnMore;
	protected JButton btnMain;
	
	protected PopupProvider popupProvider;
	
	public ToolbarButton(String icon)
	{
		this(icon, false);
	}
	
	public ToolbarButton(String icon, boolean withPopupBton) {

		currentStatus = Status.DEFAULT;
		
		int w = 28 + (withPopupBton ? 12 : 0);
		int h = 28;
		setMaximumSize(new Dimension(w, h));
		setSize(new Dimension(w, h));
		setPreferredSize(new Dimension(w, h));
		setMinimumSize(new Dimension(w, h));
		setLayout(null);
		
		this.btnMain = new JButton("");
		this.btnMain.setBounds(0, 0, 28, 28);
		this.btnMain.setRolloverEnabled(true);
		this.btnMain.setFocusable(false);
		this.btnMain.setIcon(IconProvider.getIcon(icon, IconSize.IconSizeButton));
		this.btnMain.setMargin(new Insets(2, 2, 2, 2));
		add(this.btnMain);
		
		defaultBackground = this.btnMain.getBackground();
		
		if (withPopupBton) {
			this.btnMore = new JButton("");
			this.btnMore.setBounds(28, 0, 12, 28);
			this.btnMore.setRolloverEnabled(true);
			this.btnMore.setFocusable(false);
			this.btnMore.setIcon(new ImageIcon(ToolbarButton.class.getResource("/fr/pludov/scopeexpress/ui/resources/icons/triangle1.png")));
			this.btnMore.setMargin(new Insets(2, 2, 2, 2));
			add(this.btnMore);
		}
		
		initButtons();
	}
	
	Color defaultBackground;
	Status currentStatus;
	
	int mouseInCount;
	int pressCount;
	
	void installMouseListener(final JButton jb)
	{
		jb.addMouseListener(new MouseListener() {
			
			@Override
			public void mouseReleased(MouseEvent e) {
				if (e.getButton() == MouseEvent.BUTTON1) {
					pressCount--;
					refreshBorders();
				}
			}
			
			@Override
			public void mousePressed(MouseEvent e) {
				if (e.getButton() == MouseEvent.BUTTON1) {
					pressCount++;
					refreshBorders();
					if (jb == btnMore && popupProvider != null) {
						JPopupMenu popup = popupProvider.popup();
						if (popup != null) {
							popup.show(ToolbarButton.this, 0, ToolbarButton.this.getHeight());
						}
					}
				}
				
			}
			
			@Override
			public void mouseExited(MouseEvent e) {
				mouseInCount--;
				refreshBorders();
			}
			
			@Override
			public void mouseEntered(MouseEvent e) {
				mouseInCount++;
				refreshBorders();
			}
			
			@Override
			public void mouseClicked(MouseEvent e) {
//				if (twoState && jb == btnMain) {
//					twoStateSet = !twoStateSet;
//					// FIXME: un evenement!
//					refreshBorders();
//				}
			}
		});
		
	}
	
	private static Color littleBright(Color c)
	{
	    int r = c.getRed();
	    int g = c.getGreen();
	    int b = c.getBlue();
	
	    double FACTOR=0.9;
	    
	    /* From 2D group:
	     * 1. black.brighter() should return grey
	     * 2. applying brighter to blue will always return blue, brighter
	     * 3. non pure color (non zero rgb) will eventually return white
	     */
	    
	    int i = (int)(1.0/(1.0-FACTOR));
	    if ( r == 0 && g == 0 && b == 0) {
	       return new Color(i, i, i);
	    }
	    if ( r > 0 && r < i ) r = i;
	    if ( g > 0 && g < i ) g = i;
	    if ( b > 0 && b < i ) b = i;
	
	    return new Color(Math.min((int)(r/FACTOR), 255),
	                     Math.min((int)(g/FACTOR), 255),
	                     Math.min((int)(b/FACTOR), 255));
	}
	
	void refreshBorders()
	{
		Border border;
		if (mouseInCount > 0) {
			if (pressCount > 0) {
				border = new DiscreteBevelBorder(BevelBorder.LOWERED, defaultBackground);
			} else {
				border = new DiscreteBevelBorder(BevelBorder.RAISED, defaultBackground);
			}
		} else {
			border = new InvisibleBevelBorder(defaultBackground);
		}
		if (btnMore != null) {
			btnMore.setBorder(border);
		}
		if (currentStatus != Status.DEFAULT) {
			btnMain.setBorder(new DiscreteBevelBorder(BevelBorder.LOWERED, defaultBackground));
		} else {
			btnMain.setBorder(border);
		}
		btnMain.setBackground(currentStatus.colorFrom(defaultBackground));
	}
	
	void initButtons()
	{
		installMouseListener(btnMain);
		if (btnMore != null) {
			installMouseListener(btnMore);
		}
		refreshBorders();
	}

	public void addActionListener(ActionListener al)
	{
		this.btnMain.addActionListener(al);
	}
	
	public void setEnabled(boolean b)
	{
		this.btnMain.setEnabled(b);
		if (this.btnMore != null) {
			this.btnMore.setEnabled(b);
		}
	}
	
	public void setStatus(Status status)
	{
		if (currentStatus.equals(status)) return;
		currentStatus = status;
		refreshBorders();
	}
	
	public Status getStatus()
	{
		return currentStatus;
	}

	public PopupProvider getPopupProvider() {
		return popupProvider;
	}

	public void setPopupProvider(PopupProvider popupProvider) {
		this.popupProvider = popupProvider;
	}
	
	public void setToolTipText(String text)
	{
		this.btnMain.setToolTipText(text);
		if (this.btnMore != null) {
			this.btnMore.setToolTipText(text);
		}
	}
}
