package fr.pludov.scopeexpress.ui;

import java.util.*;

import fr.pludov.scopeexpress.tasks.*;
import fr.pludov.scopeexpress.tasks.DisplayOrderConstraint.*;
import fr.pludov.scopeexpress.ui.TaskParameterPanel.*;

public class TaskParameterPanelLayoutBuilder {

	class GroupDescription
	{
		final ArrayList<String> uid;
		String title;
		boolean forceVisible;
		// Liste des taches pour report d'erreur
		final Set<SubTaskPath> paths;
	
		TaskParameterGroup taskParameterGroup;
		
		// Les fils : soit des String (sous groupe), soit des TaskParameterPath
		final List<Object> childs;
		
		GroupDescription(ArrayList<String> uid)
		{
			this.uid = uid;
			this.paths = new LinkedHashSet<>();
			this.childs = new ArrayList<>();
		}
		
		TaskParameterGroup getGroup()
		{
			if (taskParameterGroup == null) {
				taskParameterGroup = new TaskParameterGroup(paths, title, forceVisible);
			}
			
			return taskParameterGroup;
		}
		
		void layoutChilds()
		{
			TaskParameterGroup tpg = getGroup();
			
			// Pour l'instant, bête et méchant, on ne réordonne rien
			for(Object child : childs)
			{
				if (child instanceof String) {
					ArrayList<String> childId = new ArrayList<>(uid);
					childId.add((String)child);
					
					GroupDescription childDesc = groups.get(childId);
					if (childDesc != null) {
						tpg.add(childDesc.getGroup());
					}
				} else if (child instanceof ParameterPath) {
					ParameterPath pp = (ParameterPath) child;
					ParameterStatus<Object> ps = (ParameterStatus<Object>) taskParameterPanel.fields.get(pp);
					if (ps != null) {
						tpg.add(ps.dialog, ps.status);
					}
				}
			}
			
		}

		public void moveChilds(List<String> fields, boolean atEnd) {
			Object [] selected = new Object[fields.size()];
			List<Object> notSelected = new ArrayList<>(childs.size());

			boolean sthSelected = false;
			for(Object c : childs)
			{
				String id;
				
				if (c instanceof String) {
					id = (String) c;
				} else if (c instanceof ParameterPath) {
					ParameterPath pp = (ParameterPath) c;
					id = pp.parameter.getId();
				} else {
					throw new RuntimeException("Unsupported class in childs: " + c);
				}
				
				int pos = fields.indexOf(id);
				if (pos == -1) {
					notSelected.add(c);
				} else {
					selected[pos] = c;
					sthSelected = true;
				}
			}

			if (!sthSelected) {
				return;
			}
			
			childs.clear();
			if (atEnd) {
				childs.addAll(notSelected);
			}
			for(Object s : selected) {
				if (s != null) {
					childs.add(s);
				}
			}
			if (!atEnd) {
				childs.addAll(notSelected);
			}
		}
	}
	
	final TaskParameterPanel taskParameterPanel;
	final Map<ArrayList<String>, GroupDescription> groups;
	final GroupDescription rootDescription;
	
	TaskParameterPanelLayoutBuilder(TaskParameterPanel tpp)
	{
		this.taskParameterPanel = tpp;
		this.groups = new HashMap<>();
		
		this.rootDescription = new GroupDescription(new ArrayList<>());
		this.rootDescription.forceVisible = true;
		this.groups.put(rootDescription.uid, rootDescription);
	}
	
	GroupDescription getGroupByLocation(ArrayList<String> location)
	{
		GroupDescription result = groups.get(location);
		if (result != null) {
			return result;
		}
		result = new GroupDescription(location);
		groups.put(location, result);
		
		// Assurer qu'il y ait les parents
		if (!location.isEmpty()) {
			ArrayList<String> parent = new ArrayList<String>(location.size() - 1);
			for(int i = 0; i < location.size() - 1; ++i) {
				parent.add(location.get(i));
			}
			GroupDescription parentGroup = getGroupByLocation(parent);
			if (parentGroup.childs.contains(location.get(location.size() - 1))) {
				System.out.println("c'est quoi ça");
			}
			parentGroup.childs.add(location.get(location.size() - 1));
		}
		return result;
		
	}
	
	ArrayList<String> rewriteParameterPath(ArrayList<String> parameterPath, List<BaseTaskDefinition> parentTasks)
	{
		// normalement, parameterPath.size() == parentTasks.size() + 1;
		assert(parameterPath.size() == parentTasks.size() + 1);
		
		for(int parentId = parentTasks.size() - 1; parentId >= 0; parentId--) 
		{
			BaseTaskDefinition btd = parentTasks.get(parentId);
			for(DisplayOrderConstraint doc : btd.getDisplayOrderConstraints())
			{
				List<String> replacement = doc.rewritePath(parameterPath.subList(parentId, parameterPath.size()));
				if (replacement != null) {
					ArrayList<String> completeReplacement = new ArrayList<>(parentId + replacement.size());
					completeReplacement.addAll(parameterPath.subList(0, parentId));
					completeReplacement.addAll(replacement);
					parameterPath = completeReplacement;
				}
			}
		}
		return parameterPath;
	}
	
	void recuAddFields(SubTaskPath taskPath, ArrayList<BaseTaskDefinition> parentTasks)
	{
		BaseTaskDefinition taskDef = taskPath.getLength() == 0 ? taskParameterPanel.root : taskPath.lastElement();

		try {
			parentTasks.add(taskDef);
		
			ArrayList<String> location = new ArrayList<>();
			for(int i = 0; i < taskPath.getLength(); ++i)
			{
				location.add(taskPath.getElement(i).getId());
			}
			
			for(String childId : taskDef.getChildIds())
			{
				ArrayList<String> childLoc = new ArrayList<>(location);
				childLoc.add(childId);
				
				// Réécrire childLoc avec parentTasks
				childLoc = rewriteParameterPath(childLoc, parentTasks);
				
				TaskParameterId<?> tpi;
				if ((tpi = taskDef.getTaskParameterById(childId)) != null)
				{
					GroupDescription parent = getGroupByLocation(new ArrayList<>(childLoc.subList(0, childLoc.size() - 1)));
					parent.childs.add(taskPath.forParameter(tpi));
				}
	
				TaskLauncherDefinition child;
				if ((child = taskDef.getLauncherById(childId)) != null)
				{
					GroupDescription childGroup = getGroupByLocation(childLoc);
					childGroup.paths.add(taskPath.forChild(child));
					if (childGroup.title == null) {
						childGroup.title = child.getTitle();
					}
					
					recuAddFields(taskPath.forChild(child), parentTasks);
				}
			}
		} finally {
			parentTasks.remove(parentTasks.size() - 1);
		}
		
	}

	void recuAddConstraints(SubTaskPath taskPath, ArrayList<BaseTaskDefinition> parentTasks)
	{
		BaseTaskDefinition taskDef = taskPath.getLength() == 0 ? taskParameterPanel.root : taskPath.lastElement();

		try {
			parentTasks.add(taskDef);
		
			ArrayList<String> location = new ArrayList<>();
			for(int i = 0; i < taskPath.getLength(); ++i)
			{
				location.add(taskPath.getElement(i).getId());
			}
			
			for(String childId : taskDef.getChildIds())
			{
				TaskLauncherDefinition child;
				if ((child = taskDef.getLauncherById(childId)) != null)
				{
					recuAddConstraints(taskPath.forChild(child), parentTasks);
				}
			}
			
			for(DisplayOrderConstraint doc : taskDef.getDisplayOrderConstraints())
			{
				if (doc instanceof DisplayOrderConstraint.GroupDetails)
				{
					DisplayOrderConstraint.GroupDetails groupDetails = (GroupDetails) doc;
					
					ArrayList<String> realPath = rewriteParameterPath(groupDetails.getSourcePath(), parentTasks.subList(0, parentTasks.size() - 1));
					
					GroupDescription gd = groups.get(realPath);
					if (gd == null) {
						// bizarre...
						continue;
					}
					
					if (groupDetails.hasTitle()) {
						gd.title = groupDetails.getTitle();
					}
					if (groupDetails.getFirstFields() != null) {
						gd.moveChilds(groupDetails.getFirstFields(), false);
					}
					if (groupDetails.getLastFields() != null) {
						gd.moveChilds(groupDetails.getLastFields(), true);
					}
				}
			}
			
		} finally {
			parentTasks.remove(parentTasks.size() - 1);
		}
	}
		
		
	/** Positionne les champs dans des groupe et retourne la racine */
	TaskParameterGroup doLayout() {
		
		recuAddFields(new SubTaskPath(), new ArrayList<>());
		recuAddConstraints(new SubTaskPath(), new ArrayList<>());
		
		for(GroupDescription gd : groups.values())
		{
			gd.layoutChilds();
		}
		
		return rootDescription.getGroup();
	}
	
}
