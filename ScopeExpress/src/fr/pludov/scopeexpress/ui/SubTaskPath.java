package fr.pludov.scopeexpress.ui;

import java.util.*;

import fr.pludov.scopeexpress.tasks.*;

// Vide = la racine
public class SubTaskPath {
	private final ArrayList<TaskLauncherDefinition> path;
	
	public SubTaskPath() {
		path = new ArrayList<>(0);
	}
	
	private SubTaskPath(ArrayList<TaskLauncherDefinition> path, TaskLauncherDefinition child)
	{
		this.path = new ArrayList<>(path);
		this.path.add(child);
	}

	private SubTaskPath(List<TaskLauncherDefinition> path)
	{
		this.path = new ArrayList<>(path);
	}

	public SubTaskPath forChild(TaskLauncherDefinition child)
	{
		return new SubTaskPath(path, child);
	}
	
	public <TYPE> ParameterPath<TYPE> forParameter(TaskParameterId<TYPE> parameter)
	{
		return new ParameterPath<>(this, parameter);
	}
	
	public SubTaskPath getParent() {
		if (path.size() == 0) {
			return null;
		}
		return new SubTaskPath(path.subList(0, path.size() - 1));
		
	}
	
	public int getLength()
	{
		return path.size();
	}
	
	public TaskLauncherDefinition getElement(int i)
	{
		return path.get(i);
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((path == null) ? 0 : path.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		SubTaskPath other = (SubTaskPath) obj;
		if (path == null) {
			if (other.path != null)
				return false;
		} else if (!path.equals(other.path))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "SubTaskPath [path=" + path + "]";
	}

	public BaseTaskDefinition lastElement() {
		return getElement(getLength() - 1).getStartedTask();
	}

	public boolean isChildOrEquals(SubTaskPath wildCard) {
		SubTaskPath thiz = this;
		while(thiz != null) {
			if (wildCard.equals(thiz)) {
				return true;
			}
			thiz = thiz.getParent();
		}
		
		return false;
	}

}
