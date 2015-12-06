package fr.pludov.scopeexpress.ui.utils;

import java.awt.Component;
import java.awt.Graphics;

import javax.swing.Icon;
import javax.swing.UIManager;

/** + et -.
 * http://stackoverflow.com/questions/6590462/java-jtree-with-plus-minus-icons-for-expansion-and-collapse
 */
public class NodeIcon implements Icon {

	public static final NodeIcon icon_plus = new NodeIcon('+');
	public static final NodeIcon icon_minus = new NodeIcon('-');
	
    private static final int SIZE = 9;

    private char type;

    public NodeIcon(char type) {
        this.type = type;
    }

    @Override
	public void paintIcon(Component c, Graphics g, int x, int y) {
        g.setColor(UIManager.getColor("Tree.background"));
        g.fillRect(x, y, SIZE - 1, SIZE - 1);

        g.setColor(UIManager.getColor("Tree.hash").darker());
        g.drawRect(x, y, SIZE - 1, SIZE - 1);

        g.setColor(UIManager.getColor("Tree.foreground"));
        g.drawLine(x + 2, y + SIZE / 2, x + SIZE - 3, y + SIZE / 2);
        if (type == '+') {
            g.drawLine(x + SIZE / 2, y + 2, x + SIZE / 2, y + SIZE - 3);
        }
    }

    @Override
	public int getIconWidth() {
        return SIZE;
    }

    @Override
	public int getIconHeight() {
        return SIZE;
    }
}