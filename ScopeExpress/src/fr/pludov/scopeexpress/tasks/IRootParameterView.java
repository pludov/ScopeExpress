package fr.pludov.scopeexpress.tasks;

public interface IRootParameterView<TYPE extends ITaskParameterBaseView> {

	TYPE getTaskView(BaseTaskDefinition btd);
}
