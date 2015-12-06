package fr.pludov.scopeexpress.ui;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.util.IdentityHashMap;
import java.util.Map;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;

import fr.pludov.scopeexpress.tasks.BaseTask;
import fr.pludov.scopeexpress.tasks.BaseTaskDefinition;
import fr.pludov.scopeexpress.tasks.ITaskParameterView;
import fr.pludov.scopeexpress.tasks.ParameterFlag;
import fr.pludov.scopeexpress.tasks.TaskDefinitionRepository;
import fr.pludov.scopeexpress.tasks.TaskLauncherOverride;
import fr.pludov.scopeexpress.tasks.TaskParameterId;
import fr.pludov.scopeexpress.ui.utils.Utils;

public class TaskConfigurationPanel extends JPanel {
	final TaskDefinitionRepository repository;
	
	private DefaultTreeModel treeModel;
	private DefaultMutableTreeNode root;
	private JTree nodes;
	
	private JPanel configPlace;
	private final FocusUi focusUi;
	private IdentityHashMap<BaseTaskDefinition, TaskParameterPanel> parameterPanels;
	
	public TaskConfigurationPanel(FocusUi f, TaskDefinitionRepository rep) {
		this.repository = rep;
		this.focusUi = f;
		this.parameterPanels = new IdentityHashMap<>();
		setLayout(new GridLayout(1, 2));
		
		nodes = new JTree();
		nodes.setRootVisible(false);

		root = new DefaultMutableTreeNode("root", true);
		treeModel = new DefaultTreeModel(root);
		nodes.setModel(treeModel);
		add(new JScrollPane(nodes));
		
		
		configPlace = new JPanel();
		configPlace.setLayout(new BorderLayout());
		add(new JScrollPane(configPlace));
		
		
		for(BaseTaskDefinition td : repository.list())
		{
			DefaultMutableTreeNode newChild = new DefaultMutableTreeNode(td, false);
			treeModel.insertNodeInto(newChild, root, root.getChildCount());
		}
		nodes.expandPath(new TreePath(root.getPath()));		

		nodes.addTreeSelectionListener(new TreeSelectionListener() {
			
			@Override
			public void valueChanged(TreeSelectionEvent arg0) {

				DefaultMutableTreeNode node = (DefaultMutableTreeNode)
                        nodes.getLastSelectedPathComponent();
				
				configPlace.removeAll();
				if (node != null && ((node.getUserObject() instanceof BaseTaskDefinition))) {
					TaskParameterPanel tpp = buildPanel((BaseTaskDefinition) node.getUserObject());
					configPlace.add(tpp);
				}
				
				configPlace.revalidate();
				configPlace.repaint();
			}
		});
	}
	
	TaskParameterPanel buildPanel(BaseTaskDefinition btd)
	{
		TaskParameterPanel result = parameterPanels.get(btd);
		if (result == null) {
			result = new TaskParameterPanel(focusUi, btd) {
				@Override
				public boolean display(TaskParameterId<?> param, TaskLauncherOverride<?> override) {
					return override == null && param.is(ParameterFlag.PresentInConfig);
				};
			};
			result.loadAndEdit(focusUi.getApplication().getConfigurationTaskValues().getSubTaskView(btd.getId()));
			
			parameterPanels.put(btd, result);
		}
		
		return result;
	}
	
	void save()
	{
		for(Map.Entry<BaseTaskDefinition, TaskParameterPanel> item : parameterPanels.entrySet())
		{
			BaseTaskDefinition def = item.getKey();
			TaskParameterPanel tpp = item.getValue();
			
			ITaskParameterView subTaskView = focusUi.getApplication().getConfigurationTaskValues().getSubTaskView(def.getId());
			
			
			tpp.req.getDialogValues(subTaskView);
			
		}
	}
}
