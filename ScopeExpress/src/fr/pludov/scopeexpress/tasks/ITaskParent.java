package fr.pludov.scopeexpress.tasks;

public interface ITaskParent {
	BaseTask getFirst();
	void setFirst(BaseTask bt);
	BaseTask getLast();
	void setLast(BaseTask bt);
}
