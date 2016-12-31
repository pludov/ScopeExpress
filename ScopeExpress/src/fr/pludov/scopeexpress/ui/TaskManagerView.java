package fr.pludov.scopeexpress.ui;

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.tree.*;

import fr.pludov.scopeexpress.script.*;
import fr.pludov.scopeexpress.script.TaskChildListener;
import fr.pludov.scopeexpress.script.TaskManager2.*;
import fr.pludov.scopeexpress.script.TaskStatusListener;
import fr.pludov.scopeexpress.tasks.*;
import fr.pludov.scopeexpress.ui.resources.*;
import fr.pludov.scopeexpress.ui.resources.IconProvider.*;
import fr.pludov.scopeexpress.ui.utils.*;
import fr.pludov.scopeexpress.ui.widgets.*;
import fr.pludov.scopeexpress.utils.*;

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
	final TaskManager2 tm;
	final DefaultMutableTreeNode root;
	final DefaultTreeModel treeModel;
	final ToolbarButton pauseButton;
	final ToolbarButton resumeButton;
	final ToolbarButton cancelButton;
	final ToolbarButton restartButton;
	final ToolbarButton removeButton;
	final ToolbarButton removeAllButton;
	
	final JTree jtree;
	final TaskControl taskControl;

	UIElement taskUi;
	UIElement taskLog;
	final JTabbedPane taskDetailsTabbedPane;
	final JPanel taskUiContainer;
	final JPanel taskLogContainer;
	
	
	public TaskManagerView(FocusUi ui, TaskManager2 taskManager) {
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
				for(TaskOrGroup bt : getTaskSelection())
				{
					throw new RuntimeException("pausing ?");
					// Fixme: pause ?
//					 bt.requestPause();
				}
			}
		});
		leftToolBar.add(pauseButton);
		

		resumeButton = new ToolbarButton("play");
		resumeButton.setToolTipText("Reprendre les taches sélectionnées");
		resumeButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				for(TaskOrGroup bt : getTaskSelection())
				{
					throw new RuntimeException("resuming ?");
					// FIXME: resume ?
//					bt.resume();
				}
			}
		});
		leftToolBar.add(resumeButton);
		
		
		cancelButton = new ToolbarButton("stop");
		cancelButton.setToolTipText("Interrompre les taches sélectionnées");
		cancelButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				for(TaskOrGroup bt : getTaskSelection())
				{
					// FIXME: multiple cancel ?
					if ((!bt.isTerminated()) /*&& !bt.hasPendingCancelation()*/) {
						throw new RuntimeException("interrupt not supported");
						// bt.requestCancelation(BaseStatus.Canceled);
					}
				}
			}
		});
		leftToolBar.add(cancelButton);
		
		restartButton = new ToolbarButton("restart");
		restartButton.setToolTipText("Relancer les taches sélectionnées");
//		restartButton.addActionListener(new ActionListener() {
//			
//			@Override
//			public void actionPerformed(ActionEvent arg0) {
//				for(BaseTask bt : getTaskSelection())
//				{
//					if (bt.getParentLauncher() == null) {
//						tm.startTask(focusUi, bt.getDefinition(), bt.getParameters().clone());
//					}
//				}
//				
//			}
//		});
		leftToolBar.add(restartButton);

		
		
		removeButton = new ToolbarButton("close");
		removeButton.setToolTipText("Oublier les traces des taches sélectionnées");
		removeButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				for(DefaultMutableTreeNode node: getNodeSelection())
				{
					// FIXME: forget ???
//					((TaskOrGroup) node.getUserObject()).forget();
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
//					((TaskOrGroup) node.getUserObject()).forget();
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
		
		for(TaskGroup bt : tm.getGroups()) {
			taskAdded(root, null, bt);
		}
		
		this.tm.statusListeners.addListener(this.listenerOwner, new TaskManagerListener() {
			
			@Override
			public void childRemoved(TaskOrGroup bt) {
				taskRemoved(root, bt);
			}
			
			@Override
			public void childAdded(TaskOrGroup bt, TaskOrGroup after) {
				taskAdded(root, null, bt);
			}
			
			@Override
			public void childMoved(TaskOrGroup ref, TaskOrGroup movedAfter) {
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
		this.taskLogContainer = new JPanel();
		this.taskLogContainer.setLayout(new BorderLayout());
		this.taskUiContainer = new JPanel();
		this.taskUiContainer.setLayout(new BorderLayout());
		this.taskDetailsTabbedPane = new JTabbedPane(JTabbedPane.TOP);
		
		this.taskDetailsTabbedPane.add("details", this.taskUiContainer);
		this.taskDetailsTabbedPane.add("logs", this.taskLogContainer);
		
		taskControl.getDetailsPanel().setLayout(new BorderLayout());
		taskControl.getDetailsPanel().add(this.taskDetailsTabbedPane);

		jtree.addTreeSelectionListener(new TreeSelectionListener() {
			
			@Override
			public void valueChanged(TreeSelectionEvent arg0) {
				DefaultMutableTreeNode node = (DefaultMutableTreeNode)
						jtree.getLastSelectedPathComponent();
				
				
				TaskOrGroup newTarget = node == null ? null : (TaskOrGroup)node.getUserObject();
				syncSelectedTask(newTarget);
			}
		});
		
		setLeftComponent(leftPanel);
		setRightComponent(taskControl);
		updateBtonStatus();
	}
	
	void syncSelectedTask(TaskOrGroup newTarget)
	{
		if (taskUi != null) {
			taskUiContainer.remove(taskUi.getComponent());
			taskUi.dispose();
			taskUi = null;
		}
		if (taskLog != null) {
			taskLogContainer.remove(taskLog.getComponent());
			taskLog.dispose();
			taskLog = null;
		}
		
		if (newTarget == null) {
			taskControl.setCurrentTask(null);
		} else {
			// FIXME: taskControl.setCurrentTask(newTarget);
			if (newTarget instanceof TaskGroup) {
				taskUi = ((TaskGroup)newTarget).buildCustomUi();
				if (taskUi != null) {
					taskUiContainer.add(taskUi.getComponent());
				}

				taskLog = new UIElement(new TaskGroupConsoleView((TaskGroup) newTarget));
				taskLogContainer.add(taskLog.getComponent());
				
			} else {
				// FIXME: ils pourraient fournir leur ui...
				taskUi = new UIElement(new JLabel("jsTask:" + newTarget));
//				BaseTaskDefinition btdef = ((TaskOrGroup)rawbt).getDefinition();
				taskUiContainer.add(taskUi.getComponent(), BorderLayout.CENTER);
//				currentView = btdef.getViewer(TaskManagerView.this.focusUi);
			}
		}
		updateBtonStatus();
		
		taskUiContainer.revalidate();
		taskLogContainer.revalidate();
		taskUiContainer.repaint();
		taskLogContainer.repaint();
		
	}
	
	List<TaskOrGroup> getTaskSelection()
	{
		List<TaskOrGroup> result = new ArrayList<>();
		for(DefaultMutableTreeNode item : getNodeSelection()) {
			result.add((TaskOrGroup) item.getUserObject());
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
						&& ((DefaultMutableTreeNode)value).getUserObject() instanceof TaskOrGroup)
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
					&& ((DefaultMutableTreeNode)value).getUserObject() instanceof TaskOrGroup)
			{
				result.add((DefaultMutableTreeNode)value);
			}
		}
		return result;
	}
	
	List<TaskOrGroup> getAllTasks()
	{
		List<TaskOrGroup> result = new ArrayList<>();
		for(DefaultMutableTreeNode item : getAllNodes()) {
			result.add((TaskOrGroup) item.getUserObject());
		}
		return result;
	}
	
	List<TaskGroup> getAllGroups()
	{
		List<TaskGroup> result = new ArrayList<>();
		for(DefaultMutableTreeNode item : getAllNodes()) {
			TaskOrGroup tog = (TaskOrGroup) item.getUserObject();
			if (tog instanceof TaskGroup) {
				result.add((TaskGroup) tog);
			}
		}
		return result;
	}
	
	void removeNode(TaskOrGroup task)
	{
		if (!(task instanceof TaskGroup)) {
			return;
		}
		if (!task.isTerminated()) {
			return;
		}
		tm.removeTask((TaskGroup)task);
	}
	
	void updateDetailTabbedPane()
	{
		taskDetailsTabbedPane.setEnabledAt(0, this.taskUi != null);
		taskDetailsTabbedPane.setEnabledAt(1, this.taskLog != null);
		int current = taskDetailsTabbedPane.getSelectedIndex();
		if (current == -1) {
			for(int i = 0; i < 2; ++i) {
				if (taskDetailsTabbedPane.isEnabledAt(i)) {
					taskDetailsTabbedPane.setSelectedIndex(i);
					break;
				}
			}
		} else if (!taskDetailsTabbedPane.isEnabledAt(current)) {
			boolean matched = false;
			for(int i = 0; i < 2; ++i) {
				if (taskDetailsTabbedPane.isEnabledAt(i)) {
					taskDetailsTabbedPane.setSelectedIndex(i);
					matched = true;
					break;
				}
			}
			if (!matched) taskDetailsTabbedPane.setSelectedIndex(-1);
			
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
		
		List<TaskOrGroup> selection = getTaskSelection();
		
		for(TaskOrGroup bt : selection)
		{
			// FIXME: multiple cancel .
			if ((!bt.isTerminated()) /* && (!bt.hasPendingCancelation())*/) {
				canCancel = true;
			}
			if (bt.isTerminated()) {
				canRemove = true;
			}
			// FIXME: pas de canRestart
			canRestart |= false;
			// FIXME: pour la pause, on rêve ?
			canPause |= false;
			// FIXME: pareil... Il faudrait des hook au niveau des scripts.
			canResume |= false;
		}
		for(TaskOrGroup bt : getAllTasks())
		{
			if (bt.isTerminated()) {
				canRemoveAll = true;
				break;
			}
		}
		
		updateDetailTabbedPane();
		
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
				&& ((DefaultMutableTreeNode)value).getUserObject() instanceof TaskOrGroup)
			{
				// On va afficher une icone en fonction de l'état de la tache
				TaskOrGroup bt = (TaskOrGroup) ((DefaultMutableTreeNode)value).getUserObject();
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
				
				String iconId = bt.getStatusIconId();
					
				
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
				if (!bt.isTerminated()) {
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
	
	
	void taskAdded(DefaultMutableTreeNode parent, TaskOrGroup parentTask, final TaskOrGroup child)
	{
		// child.statusListeners.addListener(this.lis, i);
		final DefaultMutableTreeNode childNode = new DefaultMutableTreeNode(child);
//		parent.add(childNode);
		child.getStatusListeners().addListener(this.listenerOwner, new TaskStatusListener() {
			
			@Override
			public void statusChanged() {
				updateStatus(child, childNode);
			}
		});
//		childNode.setParent(parent);
		
		int insertIndex;
		if (child instanceof TaskGroup) {
			int childPos = tm.getGroups().indexOf(child);
			assert(childPos != -1);
			TaskOrGroup previous = childPos > 0 ? tm.getGroups().get(childPos - 1) : null;
			
			insertIndex = getInsertAfterIndex(parent, previous);
		} else {
			// On le met après ceux qu'on connait déjà
			insertIndex = parent.getChildCount();
		}
		treeModel.insertNodeInto(childNode, parent, insertIndex);
		updateStatus(child, childNode);
		
		//Make sure the user can see the lovely new node.
		// if (shouldBeVisible) {
		scrollPathToVisible(new TreePath(childNode.getPath()));
		//}
		
		
		child.getChildListeners().addListener(this.listenerOwner, new TaskChildListener() {
			
			@Override
			public void childRemoved(Task task) {
				taskRemoved(childNode, task);
			}
			
			@Override
			public void childAdded(Task task) {
				taskAdded(childNode, child, task);
			}
		});
		
		for(TaskOrGroup childOfChild : child.getChilds())
		{
			taskAdded(childNode, child, childOfChild);
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
	
	
	void removeTask(final TaskOrGroup child)
	{
		if (!child.isTerminated()) {
			return;
		}
		if (child.getParent() == null) {
			this.tm.removeTask((TaskGroup)child);
		} else {
			// Les taches dégagent toutes seules de leur parent.
			// FIXME !
		}
	}
	
	void taskRemoved(DefaultMutableTreeNode parent, final TaskOrGroup child)
	{
		DefaultMutableTreeNode nodeForChild = getNodeForTask(parent, child);
		if (nodeForChild != null) {
			treeModel.removeNodeFromParent(nodeForChild);
		} else {
			System.out.println("bizarre, il n'y est pas");
		}
	}

	private DefaultMutableTreeNode getNodeForRootTask(TaskOrGroup child)
	{
		return getNodeForTask(root, child);
		
	}
	
	private DefaultMutableTreeNode getNodeForTask(DefaultMutableTreeNode parent, final TaskOrGroup child) {
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

	private void updateStatus(TaskOrGroup child, DefaultMutableTreeNode childNode) {
		treeModel.nodeChanged(childNode);
		updateBtonStatus();
	}

	TreePath findNode(TaskOrGroup bt)
	{
		TreePath parentPath;
		if (bt.getParent() != null) {
			parentPath = findNode(bt.getParent());
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
	
	public void displayNewTask(TaskOrGroup task) {
		TreePath dmt = findNode(task);
		if (dmt != null) {
//			if (task.getStatus() != TaskGroup.Status.Pending) {
//				jtree.setSelectionPath(dmt);
//			}
			scrollPathToVisible(dmt);
		}
	}

	private int getInsertAfterIndex(DefaultMutableTreeNode parent, TaskOrGroup ref) {
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
