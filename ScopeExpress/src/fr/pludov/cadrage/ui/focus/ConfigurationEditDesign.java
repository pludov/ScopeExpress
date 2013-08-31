package fr.pludov.cadrage.ui.focus;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Window;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import javax.swing.JTabbedPane;
import net.miginfocom.swing.MigLayout;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import java.awt.Font;
import java.awt.Color;
import javax.swing.JTextField;

public class ConfigurationEditDesign extends JDialog {

	private final JPanel contentPanel = new JPanel();
	protected JTabbedPane tabbedPane;
	protected JPanel geoPanel;
	protected JLabel lblLatitude;
	protected JLabel lblLongitude;
	protected JTextField fieldLatitude;
	protected JTextField fieldLongitude;
	protected JLabel fieldLatitudeErr;
	protected JLabel fieldLongitudeErr;
	private JButton okButton;
	private JButton cancelButton;

	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		try {
			ConfigurationEditDesign dialog = new ConfigurationEditDesign(null);
			dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
			dialog.setVisible(true);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Create the dialog.
	 * @param parent 
	 */
	public ConfigurationEditDesign(Window parent) {
		super(parent);
		
		setBounds(100, 100, 450, 300);
		getContentPane().setLayout(new BorderLayout());
		this.contentPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
		getContentPane().add(this.contentPanel, BorderLayout.CENTER);
		contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.X_AXIS));
		
		this.tabbedPane = new JTabbedPane(JTabbedPane.TOP);
		contentPanel.add(this.tabbedPane);
		
		this.geoPanel = new JPanel();
		this.tabbedPane.addTab("G\u00E9ographie", null, this.geoPanel, null);
		this.geoPanel.setLayout(new MigLayout("", "[][grow][]", "[][]"));
		
		this.lblLatitude = new JLabel("Latitude : ");
		this.geoPanel.add(this.lblLatitude, "cell 0 0,alignx trailing");
		
		this.fieldLatitude = new JTextField();
		this.geoPanel.add(this.fieldLatitude, "cell 1 0,growx");
		this.fieldLatitude.setColumns(10);
		
		this.fieldLatitudeErr = new JLabel("(!)");
		this.fieldLatitudeErr.setForeground(Color.RED);
		this.fieldLatitudeErr.setFont(new Font("Tahoma", Font.BOLD, 10));
		this.geoPanel.add(this.fieldLatitudeErr, "cell 2 0");
		
		this.lblLongitude = new JLabel("Longitude : ");
		this.geoPanel.add(this.lblLongitude, "cell 0 1,alignx trailing");
		
		this.fieldLongitude = new JTextField();
		this.geoPanel.add(this.fieldLongitude, "cell 1 1,growx");
		this.fieldLongitude.setColumns(10);
		
		this.fieldLongitudeErr = new JLabel("(!)");
		this.fieldLongitudeErr.setForeground(Color.RED);
		this.fieldLongitudeErr.setFont(new Font("Tahoma", Font.BOLD, 10));
		this.geoPanel.add(this.fieldLongitudeErr, "cell 2 1");
		{
			JPanel buttonPane = new JPanel();
			buttonPane.setLayout(new FlowLayout(FlowLayout.RIGHT));
			getContentPane().add(buttonPane, BorderLayout.SOUTH);
			{
				okButton = new JButton("OK");
				okButton.setActionCommand("OK");
				buttonPane.add(okButton);
				getRootPane().setDefaultButton(okButton);
			}
			{
				cancelButton = new JButton("Cancel");
				cancelButton.setActionCommand("Cancel");
				buttonPane.add(cancelButton);
			}
		}
	}

	public JButton getOkButton() {
		return this.okButton;
	}
	public JButton getCancelButton() {
		return this.cancelButton;
	}
}
