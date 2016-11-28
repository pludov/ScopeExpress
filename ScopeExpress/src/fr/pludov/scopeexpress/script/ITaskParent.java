package fr.pludov.scopeexpress.script;

public interface ITaskParent {
	BaseTask getFirst();
	void setFirst(BaseTask bt);
	BaseTask getLast();
	void setLast(BaseTask bt);
}
