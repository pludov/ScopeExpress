package fr.pludov.scopeexpress.tasks;

import javax.swing.JPanel;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.ColumnSpec;
import com.jgoodies.forms.layout.RowSpec;
import java.awt.FlowLayout;
import net.miginfocom.swing.MigLayout;
import java.awt.GridLayout;
import java.awt.BorderLayout;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import java.awt.Component;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

public class TestLayout extends JPanel {
	protected JPanel panel;
	protected JPanel panel_1;
	protected JPanel panel_2;
	protected JLabel lblCoucou;
	protected JLabel lblCoucou_1;
	protected JLabel lblNewLabel;
	protected JTextField textField;
	protected JLabel lblVeryLongLong;

	/**
	 * Create the panel.
	 */
	public TestLayout() {
		setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
		
		this.panel_1 = new JPanel();
		this.panel_1.setAlignmentY(Component.TOP_ALIGNMENT);
		add(this.panel_1);
		this.panel_1.setLayout(new MigLayout("ins 0", "[120px:120px,trailing][grow,fill][left]", "[][]"));
		
		this.lblCoucou = new JLabel("coucou");
		this.panel_1.add(this.lblCoucou, "cell 0 0,alignx trailing");
		
		this.textField = new JTextField();
		this.panel_1.add(this.textField, "cell 1 0,growx");
		this.textField.setColumns(10);
		
		this.lblVeryLongLong = new JLabel("<html>Very long long long <b>long</b> long long long label:</html>");
		this.lblVeryLongLong.setHorizontalAlignment(SwingConstants.TRAILING);
		this.panel_1.add(this.lblVeryLongLong, "cell 0 1");
		
		this.panel = new JPanel();
		add(this.panel);
		this.panel.setLayout(new MigLayout("", "[]", "[]"));
		
		this.lblCoucou_1 = new JLabel("coucou2");
		this.panel.add(this.lblCoucou_1, "cell 0 0");
		
		this.panel_2 = new JPanel();
		add(this.panel_2);
		this.panel_2.setLayout(new MigLayout("", "[]", "[]"));
		
		this.lblNewLabel = new JLabel("New label");
		this.panel_2.add(this.lblNewLabel, "cell 0 0");

	}

}
