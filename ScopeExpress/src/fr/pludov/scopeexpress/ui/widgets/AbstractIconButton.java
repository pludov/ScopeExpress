package fr.pludov.scopeexpress.ui.widgets;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.LayoutManager;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.border.BevelBorder;
import javax.swing.border.Border;

import fr.pludov.scopeexpress.ui.resources.IconProvider;
import fr.pludov.scopeexpress.ui.resources.IconProvider.IconSize;

public class AbstractIconButton extends JPanel {

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
	}

	public static interface PopupProvider {
		public JPopupMenu popup();
	}
	
	final IconSize iconSizeButton;
	final int width;
	final int margin;

	public AbstractIconButton(String icon, IconSize iconSizeButton, int width, int margin)
	{
		this(icon, iconSizeButton, width, margin, false);
	}
	
	public AbstractIconButton(String icon, IconSize iconSizeButton, int width, int margin, boolean withPopupBton) {

		this.iconSizeButton = iconSizeButton;
		this.width = width;
		this.margin = margin;
		
		currentStatus = Status.DEFAULT;
		
		int w = width + (withPopupBton ? 12 : 0);
		int h = width;
		setMaximumSize(new Dimension(w, h));
		setSize(new Dimension(w, h));
		setPreferredSize(new Dimension(w, h));
		setMinimumSize(new Dimension(w, h));
		setLayout(null);
		
		this.btnMain = new JButton("");
		this.btnMain.setBounds(0, 0, width, width);
		this.btnMain.setRolloverEnabled(true);
		this.btnMain.setFocusable(false);
		this.btnMain.setIcon(IconProvider.getIcon(icon, iconSizeButton));
		this.btnMain.setMargin(new Insets(margin, margin, margin, margin));
		add(this.btnMain);
		
		defaultBackground = this.btnMain.getBackground();
		
		if (withPopupBton) {
			this.btnMore = new JButton("");
			this.btnMore.setBounds(width, 0, 12, width);
			this.btnMore.setRolloverEnabled(true);
			this.btnMore.setFocusable(false);
			this.btnMore.setIcon(new ImageIcon(ToolbarButton.class.getResource("/fr/pludov/scopeexpress/ui/resources/icons/triangle1.png")));
			this.btnMore.setMargin(new Insets(margin, margin, margin, margin));
			add(this.btnMore);
		}
		
		initButtons();
	}
	
	protected JButton btnMore;
	protected JButton btnMain;
	protected PopupProvider popupProvider;
	protected Color defaultBackground;
	protected Status currentStatus;
	int mouseInCount;
	int pressCount;

	private static Color littleBright(Color c) {
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

	void installMouseListener(final JButton jb) {
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
						if (jb.isEnabled() && jb == btnMore && popupProvider != null) {
							JPopupMenu popup = popupProvider.popup();
							if (popup != null) {
								popup.show(AbstractIconButton.this, 0, AbstractIconButton.this.getHeight());
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

	void refreshBorders() {
		Border border;
		if (mouseInCount > 0 && btnMain.isEnabled()) {
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

	protected void initButtons() {
		installMouseListener(btnMain);
		if (btnMore != null) {
			installMouseListener(btnMore);
		}
		refreshBorders();
	}

	public void addActionListener(ActionListener al) {
		this.btnMain.addActionListener(al);
	}

	public void setEnabled(boolean b) {
		this.btnMain.setEnabled(b);
		if (this.btnMore != null) {
			this.btnMore.setEnabled(b);
		}
		refreshBorders();
	}

	public void setStatus(Status status) {
		if (currentStatus.equals(status)) return;
		currentStatus = status;
		refreshBorders();
	}

	public Status getStatus() {
		return currentStatus;
	}

	public PopupProvider getPopupProvider() {
		return popupProvider;
	}

	public void setPopupProvider(PopupProvider popupProvider) {
		this.popupProvider = popupProvider;
	}

	public void setToolTipText(String text) {
		this.btnMain.setToolTipText(text);
		if (this.btnMore != null) {
			this.btnMore.setToolTipText(text);
		}
	}

}