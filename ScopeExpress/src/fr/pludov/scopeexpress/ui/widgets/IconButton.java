package fr.pludov.scopeexpress.ui.widgets;

import fr.pludov.scopeexpress.ui.resources.IconProvider.IconSize;

/**
 * Tout petit bouton de controle (pas pour les toolbar...)
 */
public class IconButton extends AbstractIconButton {

	public IconButton(String icon) {
		this(icon, false);
	}

	public IconButton(String icon, boolean withPopupBton) {
		super(icon, IconSize.IconSizeSmallButton, 16, 0, withPopupBton);
	}

}
