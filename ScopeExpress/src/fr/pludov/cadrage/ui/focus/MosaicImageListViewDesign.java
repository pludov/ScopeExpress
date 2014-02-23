package fr.pludov.cadrage.ui.focus;

import javax.swing.JPanel;
import net.miginfocom.swing.MigLayout;
import javax.swing.BoxLayout;
import java.awt.GridLayout;
import java.awt.Color;
import javax.swing.border.SoftBevelBorder;
import javax.swing.border.BevelBorder;
import java.awt.Dimension;
import javax.swing.UIManager;
import javax.swing.JSplitPane;
import javax.swing.JLabel;
import javax.swing.border.TitledBorder;

public class MosaicImageListViewDesign extends JPanel {
	protected JPanel imageViewPanel;
	protected JPanel imageListPanel;
	protected JSplitPane splitPane;
	protected JPanel rightPanel;
	protected JPanel leftPanel;
	protected JPanel zoomPanel;
	protected JPanel viewParameterPanel;
	protected JLabel lblStatus;
	protected JPanel astrometryParameterPanel;

	/**
	 * Create the panel.
	 */
	public MosaicImageListViewDesign() {
		setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
		
		this.splitPane = new JSplitPane();
		this.splitPane.setResizeWeight(0.8);
		add(this.splitPane);
		
		this.rightPanel = new JPanel();
		this.splitPane.setRightComponent(this.rightPanel);
		this.rightPanel.setLayout(new MigLayout("", "[128px:250px,grow,fill]", "[128px:n,grow,fill][grow,fill]"));
		
		this.zoomPanel = new JPanel();
		this.rightPanel.add(this.zoomPanel, "cell 0 0,grow");
		this.zoomPanel.setLayout(new BoxLayout(this.zoomPanel, BoxLayout.X_AXIS));
		
		this.imageListPanel = new JPanel();
		this.rightPanel.add(this.imageListPanel, "cell 0 1,grow");
		this.imageListPanel.setBackground(UIManager.getColor("Button.background"));
		this.imageListPanel.setLayout(new BoxLayout(this.imageListPanel, BoxLayout.X_AXIS));
		
		this.leftPanel = new JPanel();
		this.splitPane.setLeftComponent(this.leftPanel);
		this.leftPanel.setLayout(new MigLayout("insets 1", "[150px:n,grow,fill][150px:n,grow,fill]", "[200px:n,grow,fill][][fill]"));
		
		this.imageViewPanel = new JPanel();
		this.imageViewPanel.setBorder(new SoftBevelBorder(BevelBorder.LOWERED, null, null, null, null));
		this.leftPanel.add(this.imageViewPanel, "cell 0 0 2 1,grow");
		this.imageViewPanel.setLayout(new GridLayout(1, 0, 0, 0));
		
		this.lblStatus = new JLabel("");
		this.leftPanel.add(this.lblStatus, "cell 0 1 2 1,alignx left");
		
		this.viewParameterPanel = new JPanel();
		this.viewParameterPanel.setBorder(new TitledBorder(null, "Visualisation", TitledBorder.LEADING, TitledBorder.TOP, null, null));
		this.leftPanel.add(this.viewParameterPanel, "cell 0 2,grow");
		this.viewParameterPanel.setLayout(new BoxLayout(this.viewParameterPanel, BoxLayout.X_AXIS));
		
		this.astrometryParameterPanel = new JPanel();
		this.leftPanel.add(this.astrometryParameterPanel, "cell 1 2,grow");
		this.astrometryParameterPanel.setLayout(new BoxLayout(this.astrometryParameterPanel, BoxLayout.X_AXIS));

	}
}
