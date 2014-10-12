package fr.pludov.scopeexpress.ui.utils;

import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.border.BevelBorder;
import javax.swing.border.Border;
import javax.swing.border.SoftBevelBorder;

/**
 * Assure que le look d'un JPanel parent s'adapte automatiquement au focus du child
 */
public class PanelFocusBorderHandler implements FocusListener {
	JPanel imageViewPanel;
	JComponent principal;
	boolean firstCall;
	
	public PanelFocusBorderHandler(JPanel parent, JComponent child) {
		parent.setFocusable(false);
		this.imageViewPanel = parent;
		this.principal = child;
		this.firstCall = true;
		child.addFocusListener(this);
		setBorder();
	}
	
	private void setBorder()
	{
		// FIXME: isFocusOwner est un peut fort, on voudrait juste par rapport à la fenêtre !
		int wantedType = principal.isFocusOwner() ? BevelBorder.LOWERED : BevelBorder.RAISED;
		SoftBevelBorder sbb;
		if (!firstCall) {
			Border currentBorder = imageViewPanel.getBorder();
			sbb = (SoftBevelBorder)currentBorder;
			if (sbb.getBevelType() == wantedType) return;
		}
		firstCall = false;
		sbb = new SoftBevelBorder(wantedType);
		imageViewPanel.setBorder(sbb);
	}

	@Override
	public void focusGained(FocusEvent e) {
		setBorder();
	}
	
	@Override
	public void focusLost(FocusEvent e) {
		setBorder();
	}

}
