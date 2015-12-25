package fr.pludov.scopeexpress.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.*;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.DropMode;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JToolBar;
import javax.swing.JTree;
import javax.swing.UIManager;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.MutableTreeNode;
import javax.swing.tree.TreeCellRenderer;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;

import fr.pludov.scopeexpress.tasks.BaseStatus;
import fr.pludov.scopeexpress.tasks.BaseTask;
import fr.pludov.scopeexpress.tasks.BaseTaskDefinition;
import fr.pludov.scopeexpress.tasks.ChildLauncher;
import fr.pludov.scopeexpress.tasks.TaskChildListener;
import fr.pludov.scopeexpress.tasks.TaskControl;
import fr.pludov.scopeexpress.tasks.TaskDetailView;
import fr.pludov.scopeexpress.tasks.TaskLauncherDefinition;
import fr.pludov.scopeexpress.tasks.TaskManager;
import fr.pludov.scopeexpress.tasks.TaskStatusListener;
import fr.pludov.scopeexpress.tasks.TaskManager.TaskManagerListener;
import fr.pludov.scopeexpress.ui.resources.IconProvider;
import fr.pludov.scopeexpress.ui.resources.IconProvider.IconSize;
import fr.pludov.scopeexpress.ui.utils.NodeIcon;
import fr.pludov.scopeexpress.ui.widgets.IconButton;
import fr.pludov.scopeexpress.ui.widgets.ToolbarButton;
import fr.pludov.scopeexpress.utils.WeakListenerOwner;

/**
 * Présente un tableau des taches en cours, arborescent
 * 
 * Nom de la tache    Type     Etat
 * 
 * @author utilisateur
 *
 */
public class TaskManagerView extends JSplitPane {
	protected final WeakListenerOwner listenerOwner = new WeakListenerOwner(this);

	final FocusUi focusUi;
	final TaskManager tm;
	final DefaultMutableTreeNode root;
	final DefaultTreeModel treeModel;
	final ToolbarButton pauseButton;
	final ToolbarButton resumeButton;
	final ToolbarButton cancelButton;
	final ToolbarButton restartButton;
	final ToolbarButton removeButton;
	final ToolbarButton removeAllButton;
	
	final JTree jtree;
	final JPanel details;
	final TaskControl taskControl;
	TaskDetailView currentView;
	
	JComponent currentViewPanel;
	
	public TaskManagerView(FocusUi ui, TaskManager taskManager) {
		super(JSplitPane.HORIZONTAL_SPLIT);
		this.tm = taskManager;
		this.focusUi = ui;
		
		JPanel leftPanel = new JPanel();
		leftPanel.setLayout(new BorderLayout());
		leftPanel.setMinimumSize(new Dimension(120,200));
		
		JToolBar leftToolBar = new JToolBar();
		

		
		pauseButton = new ToolbarButton("pause");
		pauseButton.setToolTipText("Suspendre les taches sélectionnées");
		pauseButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				for(BaseTask bt : getTaskSelection())
				{
					bt.requestPause();
				}
			}
		});
		leftToolBar.add(pauseButton);
		

		resumeButton = new ToolbarButton("play");
		resumeButton.setToolTipText("Reprendre les taches sélectionnées");
		resumeButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				for(BaseTask bt : getTaskSelection())
				{
					bt.resume();
				}
			}
		});
		leftToolBar.add(resumeButton);
		
		
		cancelButton = new ToolbarButton("stop");
		cancelButton.setToolTipText("Interrompre les taches sélectionnées");
		cancelButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				for(BaseTask bt : getTaskSelection())
				{
					if ((!bt.getStatus().isTerminal()) && !bt.hasPendingCancelation()) {
						bt.requestCancelation();
					}
				}
			}
		});
		leftToolBar.add(cancelButton);
		
		restartButton = new ToolbarButton("restart");
		restartButton.setToolTipText("Relancer les taches sélectionnées");
		restartButton.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent arg0) {
				for(BaseTask bt : getTaskSelection())
				{
					if (bt.getParentLauncher() == null) {
						tm.startTask(focusUi, bt.getDefinition(), bt.getParameters().clone());
					}
				}
				
			}
		});
		leftToolBar.add(restartButton);

		
		
		removeButton = new ToolbarButton("close");
		removeButton.setToolTipText("Oublier les traces des taches sélectionnées");
		removeButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				for(DefaultMutableTreeNode node: getNodeSelection())
				{
					((BaseTask) node.getUserObject()).forget();
//					removeTask((DefaultMutableTreeNode)node.getParent(), (BaseTask) node.getUserObject());
				}
			}
		});
		leftToolBar.add(removeButton);
		removeAllButton = new ToolbarButton("closeall");
		removeAllButton.setToolTipText("Oublier les traces des toutes les taches terminées");
		removeAllButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				for(DefaultMutableTreeNode node: getAllNodes())
				{
					((BaseTask) node.getUserObject()).forget();
//					removeTask((DefaultMutableTreeNode)node.getParent(), (BaseTask) node.getUserObject());
				}
			}
		});
		leftToolBar.add(removeAllButton);

		//		removeAllButton = new IconButton();
//		leftToolBar.add(removeAllButton);
		
		
		leftPanel.add(leftToolBar, BorderLayout.NORTH);
		
		jtree = new JTree(new DefaultTreeModel(new DefaultMutableTreeNode("root")));
		jtree.setCellRenderer(new TaskCellRenderer());
//		jtree.setShowsRootHandles(false);
		this.root = (DefaultMutableTreeNode) jtree.getModel().getRoot();
		this.treeModel = (DefaultTreeModel) jtree.getModel();

		jtree.setRootVisible(false);
		jtree.setTransferHandler(new TaskManagerDragDropHandler(this, this.root, this.tm));
		jtree.setDragEnabled(true);
		jtree.setDropMode(DropMode.INSERT);

		
		JScrollPane scroller = new JScrollPane(jtree, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		leftPanel.add(scroller, BorderLayout.CENTER);
		
		for(BaseTask bt : tm.getRunningTasks()) {
			taskAdded(root, null, null, bt);
		}
		
		this.tm.statusListeners.addListener(this.listenerOwner, new TaskManagerListener() {
			
			@Override
			public void childRemoved(BaseTask bt) {
				taskRemoved(root, bt);
			}
			
			@Override
			public void childAdded(BaseTask bt, BaseTask after) {
				taskAdded(root, null, null, bt);
			}
			
			@Override
			public void childMoved(BaseTask ref, BaseTask movedAfter) {
				DefaultMutableTreeNode toMove = getNodeForRootTask(movedAfter);
				if (toMove == null) {
					return;
				}
				toMove.removeFromParent();
				
				root.insert(toMove, getInsertAfterIndex(root, ref));
				
				treeModel.nodeStructureChanged(root);
			}
		});
		taskControl = new TaskControl();
		details = taskControl.getDetailsPanel();
		details.setLayout(new BorderLayout());

		jtree.addTreeSelectionListener(new TreeSelectionListener() {
			
			@Override
			public void valueChanged(TreeSelectionEvent arg0) {
				if (currentView != null) {
					currentView = null;
				}
				if (currentViewPanel != null) {
					details.remove(currentViewPanel);
					currentViewPanel = null;
					
				}
					
				DefaultMutableTreeNode node = (DefaultMutableTreeNode)
                        jtree.getLastSelectedPathComponent();
				
				if (node == null) {
					taskControl.setCurrentTask(null);
				} else {
					Object rawbt = node.getUserObject();
					if (rawbt instanceof BaseTask) {
						BaseTaskDefinition btdef = ((BaseTask)rawbt).getDefinition();
						currentView = btdef.getViewer(TaskManagerView.this.focusUi);
						if (currentView == null) {
							currentView = new DefaultTaskView(TaskManagerView.this.focusUi);
						}
						if (currentView != null) {
							currentView.setTask((BaseTask)rawbt);
							currentViewPanel = currentView.getMainPanel();
							details.add(currentViewPanel, BorderLayout.CENTER);
						}
						taskControl.setCurrentTask((BaseTask)rawbt);
					} else {
						taskControl.setCurrentTask(null);
					}
				}
				updateBtonStatus();
				details.revalidate();
				details.repaint();
			}
		});
		
		setLeftComponent(leftPanel);
		setRightComponent(taskControl);
		updateBtonStatus();
	}
	
	List<BaseTask> getTaskSelection()
	{
		List<BaseTask> result = new ArrayList<>();
		for(DefaultMutableTreeNode item : getNodeSelection()) {
			result.add((BaseTask) item.getUserObject());
		}
		return result;
	}
	

	List<DefaultMutableTreeNode> getNodeSelection()
	{
		List<DefaultMutableTreeNode> result = new ArrayList<>();
		TreePath[] selectionPaths = jtree.getSelectionPaths();
		if (selectionPaths != null) {
			for(TreePath tp : selectionPaths)
			{
				Object value = tp.getLastPathComponent();
				if ((value instanceof DefaultMutableTreeNode)
						&& ((DefaultMutableTreeNode)value).getUserObject() instanceof BaseTask)
				{
					result.add(((DefaultMutableTreeNode)value));
				}
				
			}
		}
		return result;
	}
	
	List<DefaultMutableTreeNode> getAllNodes()
	{
		List<DefaultMutableTreeNode> result = new ArrayList<>();

		for(Enumeration e = root.preorderEnumeration(); e.hasMoreElements(); )
		{
			Object value = e.nextElement();
			if ((value instanceof DefaultMutableTreeNode)
					&& ((DefaultMutableTreeNode)value).getUserObject() instanceof BaseTask)
			{
				result.add((DefaultMutableTreeNode)value);
			}
		}
		return result;
	}
	
	List<BaseTask> getAllTasks()
	{
		List<BaseTask> result = new ArrayList<>();
		for(DefaultMutableTreeNode item : getAllNodes()) {
			result.add((BaseTask) item.getUserObject());
		}
		return result;
	}
	
	void removeNode(BaseTask task)
	{
		if (!task.getStatus().isTerminal()) {
			return;
		}
		
		if (task.getParentLauncher() != null) {
			
		} else {
			tm.removeTask(task);
		}
	}
	
	void updateBtonStatus()
	{
		boolean canCancel = false;
		boolean canRestart = false;
		boolean canRemove = false;
		boolean canRemoveAll = false;
		boolean canPause = false;
		boolean canResume = false;
		
		List<BaseTask> selection = getTaskSelection();
		
		for(BaseTask bt : selection)
		{
			if ((!bt.getStatus().isTerminal()) && (!bt.hasPendingCancelation())) {
				canCancel = true;
			}
			if (bt.getStatus().isTerminal()) {
				canRemove = true;
			}
			canRestart |= bt.getParentLauncher() == null;
			canPause |= bt.pausable();
			canResume |= bt.getStatus() == BaseStatus.Paused;
		}
		for(BaseTask bt : getAllTasks())
		{
			if (bt.getStatus().isTerminal()) {
				canRemoveAll = true;
				break;
			}
		}
		
		cancelButton.setEnabled(canCancel);
		restartButton.setEnabled(canRestart);
		removeButton.setEnabled(canRemove);
		removeAllButton.setEnabled(canRemoveAll);
		pauseButton.setEnabled(canPause);
		resumeButton.setEnabled(canResume);
	}
	
	public class TaskCellRenderer implements TreeCellRenderer {
		@Override
		public Component getTreeCellRendererComponent(JTree tree, Object value,
				boolean sel,
				boolean expanded, boolean leaf, int row, boolean hasFocus) {
			if ((value instanceof DefaultMutableTreeNode)
				&& ((DefaultMutableTreeNode)value).getUserObject() instanceof BaseTask)
			{
				// On va afficher une icone en fonction de l'état de la tache
				BaseTask bt = (BaseTask) ((DefaultMutableTreeNode)value).getUserObject();
				JPanel result = new JPanel();
				result.setLayout(new BoxLayout(result, BoxLayout.LINE_AXIS));
				
				if ((!leaf) && ((DefaultMutableTreeNode)value).getParent() == root) {
					Icon sign;
					if (expanded) {
						sign = NodeIcon.icon_minus;
					} else {
						sign = NodeIcon.icon_plus;
					}
					JButton icon = new JButton(sign);
//					icon.setMinimumSize(new Dimension(24, 24));
//					icon.setPreferredSize(new Dimension(24, 24));
					icon.setRolloverEnabled(true);
					icon.setFocusable(false);
					icon.setBorderPainted(false);
					icon.setIcon(sign);
					icon.setMargin(new Insets(0, 0, 0, 0));
					icon.setOpaque(false);
					result.add(icon);

				}
				
				String iconId;
				if (bt.getStatus() == BaseStatus.Canceled) {
					iconId = "status-canceled";
				} else if (bt.getStatus() == BaseStatus.Error) {
					iconId = "status-error";
				} else if (bt.getStatus() == BaseStatus.Pending){
					iconId = "status-pending";
				} else if (bt.getStatus().isTerminal()) {
					iconId = "status-ok";
				} else {
					iconId = "status-running";
				}
					
				
				JButton icon = new JButton("");
				icon.setRolloverEnabled(true);
				icon.setFocusable(false);
				icon.setBorderPainted(false);
				icon.setIcon(IconProvider.getIcon(iconId, IconSize.IconSizeSmallButton));
				icon.setMargin(new Insets(0, 2, 2, 2));
				icon.setOpaque(false);
				result.add(icon);
				
				
				JLabel jl = new JLabel(bt.getTitle());
				result.add(jl);
				if (!bt.getStatus().isTerminal()) {
					jl.setFont(jl.getFont().deriveFont(Font.BOLD));
				}
				// jl.setForeground(bt.getStatus().getColor());
				if (sel) {
					Color c = UIManager.getColor("Tree.selectionBackground");
					result.setBackground(c);
					Color foreground = UIManager.getColor("Tree.selectionForeground");
					jl.setForeground(foreground);
				} else {
					result.setOpaque(false);
				}
				if (hasFocus) {
					result.setBorder(BorderFactory.createDashedBorder(null, 1F, 0.5F));
				} else {
					result.setBorder(BorderFactory.createEmptyBorder(1,  1,  1,  1));
				}
				
//				if (selected)
//				{
//					jl.setBackground(getBackgroundSelectionColor());
//					jl.setForeground(getTextSelectionColor());
//
//					if (hasFocus)
//						jl.setBorderSelectionColor(UIManager.getLookAndFeelDefaults().
//								getColor("Tree.selectionBorderColor"));
//					else
//						jl.setBorderSelectionColor(null);
//				}
//				else
//				{
//					jl.setBackground(getBackgroundNonSelectionColor());
//					jl.setForeground(getTextNonSelectionColor());
//					jl.setBorderSelectionColor(null);
//				}
				return result;
			} else {
				return new JLabel("N/A");
			}
		}
	}
	
	
	void taskAdded(DefaultMutableTreeNode parent, BaseTask parentTask, TaskLauncherDefinition parentLauncher, final BaseTask child)
	{
		// child.statusListeners.addListener(this.lis, i);
		final DefaultMutableTreeNode childNode = new DefaultMutableTreeNode(child);
//		parent.add(childNode);
		child.statusListeners.addListener(this.listenerOwner, new TaskStatusListener() {
			
			@Override
			public void statusChanged() {
				updateStatus(child, childNode);
			}
		});
//		childNode.setParent(parent);
		
		
		treeModel.insertNodeInto(childNode, parent, getInsertAfterIndex(parent, child.getPrevious()));
		updateStatus(child, childNode);
		
		//Make sure the user can see the lovely new node.
		// if (shouldBeVisible) {
		scrollPathToVisible(new TreePath(childNode.getPath()));
		//}
		
		
		child.childListeners.addListener(this.listenerOwner, new TaskChildListener() {
			
			@Override
			public void childRemoved(ChildLauncher launcher) {
				taskRemoved(childNode, /*child, launcher.getLaunched(), */launcher.getTask());
			}
			
			@Override
			public void childAdded(ChildLauncher launcher) {
				taskAdded(childNode, child, launcher.getLaunched(), launcher.getTask());
			}
		});
		
		for(ChildLauncher childLauncher : child.getStartedTask())
		{
			taskAdded(childNode, child, childLauncher.getLaunched(), childLauncher.getTask());
		}
		
		updateBtonStatus();
	}

	private void scrollPathToVisible(TreePath treePath) {
	    if (treePath != null) {
	        jtree.makeVisible(treePath);

	        Rectangle bounds = jtree.getPathBounds(treePath);

	        if (bounds != null) {
	            bounds.x = 0;
	            jtree.scrollRectToVisible(bounds);
	        }
	    }
	}
	
	
	void removeTask(final BaseTask child)
	{
		if (!child.getStatus().isTerminal()) {
			return;
		}
		if (child.getParentLauncher() == null) {
			this.tm.removeTask(child);
		} else {
			BaseTask parentTask = child.getParentLauncher().getFrom();
			parentTask.forgetStartedTask(child);
		}
	}
	
	void taskRemoved(DefaultMutableTreeNode parent, final BaseTask child)
	{
		DefaultMutableTreeNode nodeForChild = getNodeForTask(parent, child);
		if (nodeForChild != null) {
			treeModel.removeNodeFromParent(nodeForChild);
		} else {
			System.out.println("bizarre, il n'y est pas");
		}
	}

	private DefaultMutableTreeNode getNodeForRootTask(BaseTask child)
	{
		return getNodeForTask(root, child);
		
	}
	
	private DefaultMutableTreeNode getNodeForTask(DefaultMutableTreeNode parent, final BaseTask child) {
		DefaultMutableTreeNode nodeForChild = null;
		for(int i = 0; i < parent.getChildCount(); ++i)
		{
			DefaultMutableTreeNode childTreeNode = (DefaultMutableTreeNode) parent.getChildAt(i);
			if (childTreeNode.getUserObject() == child) {
				nodeForChild = childTreeNode;
				break;
			}
		}
		return nodeForChild;
	}

	private void updateStatus(BaseTask child, DefaultMutableTreeNode childNode) {
		treeModel.nodeChanged(childNode);
		updateBtonStatus();
	}

	TreePath findNode(BaseTask bt)
	{
		TreePath parentPath;
		if (bt.getParentLauncher() != null) {
			parentPath = findNode(bt.getParentLauncher().getFrom());
		} else {
			parentPath = new TreePath(this.root);
		}
		if (parentPath == null) {
			return null;
		}
		DefaultMutableTreeNode parent = (DefaultMutableTreeNode) parentPath.getLastPathComponent(); 
		for(int i = 0; i < parent.getChildCount(); ++i) {
			DefaultMutableTreeNode child = (DefaultMutableTreeNode) parent.getChildAt(i);
			if (child.getUserObject() == bt) {
				return parentPath.pathByAddingChild(child);
			}
		}
		return null;
	}
	
	public void selectTask(BaseTask task) {
		TreePath dmt = findNode(task);
		if (dmt != null) {
			jtree.setSelectionPath(dmt);
		}
	}

	private int getInsertAfterIndex(DefaultMutableTreeNode parent, BaseTask ref) {
		int where;
		if (ref == null) {
			where = 0;
		} else {
			DefaultMutableTreeNode refNode = getNodeForTask(parent, ref);
			if (refNode == null) {
				where = parent.getChildCount();
			} else {
				where = parent.getIndex(refNode);
				if (where == -1) {
					where = parent.getChildCount();
				} else {
					where++;
				}
			}
		}
		return where;
	}
}
