package fr.pludov.scopeexpress.script;

public class ConditionMeet {
	Object result;
	Object error;
	
	public Object getError()
	{
		return error;
	}
	
	public Object getResult()
	{
		return result;
	}
	
	public static ConditionMeet error(Object error) {
		assert(error != null);
		ConditionMeet rslt = new ConditionMeet();
		rslt.error = error;
		return rslt;
	}
	
	public static ConditionMeet success(Object result){
		ConditionMeet rslt = new ConditionMeet();
		rslt.result = result;
		return rslt;
	}
}