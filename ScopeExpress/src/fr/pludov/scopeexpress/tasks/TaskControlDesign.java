package fr.pludov.scopeexpress.tasks;

import java.awt.LayoutManager;

import javax.swing.JPanel;
import java.awt.BorderLayout;
import javax.swing.JButton;
import java.awt.FlowLayout;
import net.miginfocom.swing.MigLayout;
import javax.swing.JLabel;
import java.awt.Font;

public class TaskControlDesign extends JPanel {
	protected JPanel statusPanel;
	protected JPanel buttonPanel;
	protected JLabel lblShowTitle;
	protected JLabel lblStatus;
	protected JLabel lblDbutfin;
	protected JLabel lblShowStatus;
	protected JLabel lblShowTiming;
	protected JPanel detailsPanel;

	public TaskControlDesign() {
		setLayout(new BorderLayout(0, 0));
		
		this.statusPanel = new JPanel();
		add(this.statusPanel, BorderLayout.NORTH);
		this.statusPanel.setLayout(new MigLayout("", "[80px:n,grow 10][grow]", "[][][]"));
		
		this.lblShowTitle = new JLabel("Titre");
		this.lblShowTitle.setFont(this.lblShowTitle.getFont().deriveFont(this.lblShowTitle.getFont().getStyle() | Font.BOLD, this.lblShowTitle.getFont().getSize() + 3f));
		this.statusPanel.add(this.lblShowTitle, "cell 0 0 2 1,alignx center");
		
		this.lblStatus = new JLabel("Status:");
		this.statusPanel.add(this.lblStatus, "cell 0 1,alignx right");
		
		this.lblShowStatus = new JLabel("New label");
		this.lblShowStatus.setFont(this.lblShowStatus.getFont().deriveFont(this.lblShowStatus.getFont().getStyle() | Font.BOLD, this.lblShowStatus.getFont().getSize() + 3f));
		this.statusPanel.add(this.lblShowStatus, "cell 1 1");
		
		this.lblDbutfin = new JLabel("D\u00E9but/fin:");
		this.statusPanel.add(this.lblDbutfin, "cell 0 2,alignx right");
		
		this.lblShowTiming = new JLabel("New label");
		this.statusPanel.add(this.lblShowTiming, "cell 1 2");
		
		this.buttonPanel = new JPanel();
		FlowLayout flowLayout = (FlowLayout) this.buttonPanel.getLayout();
		flowLayout.setAlignment(FlowLayout.RIGHT);
		add(this.buttonPanel, BorderLayout.SOUTH);
		
		this.detailsPanel = new JPanel();
		add(this.detailsPanel, BorderLayout.CENTER);
		// TODO Auto-generated constructor stub
	}

	public JPanel getDetailsPanel() {
		return this.detailsPanel;
	}
}
