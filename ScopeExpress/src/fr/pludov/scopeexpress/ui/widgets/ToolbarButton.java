package fr.pludov.scopeexpress.ui.widgets;

import fr.pludov.scopeexpress.ui.resources.IconProvider.IconSize;

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
