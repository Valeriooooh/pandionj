package pt.iscte.pandionj.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;

import org.eclipse.debug.core.DebugException;
import org.eclipse.jdt.debug.core.IJavaType;
import org.eclipse.jdt.debug.core.IJavaValue;
import org.eclipse.jdt.debug.core.IJavaVariable;

// TODO oberver?
public abstract class VariableModel<T extends IJavaValue> extends ModelElement<T> {

	public interface Observer {
		void valueChanged(Object oldValue, Object newValue);
	}
//	class ObserverList {
//		private List<Observer> observers = new ArrayList<>();
//		
//		public ObserverList() {
//			// TODO Auto-generated constructor stub
//		}
//	
//		void notifyObservers(Object oldValue, Object newValue) {
//			for(Observer o : observers)
//				o.valueChanged(oldValue, newValue);
//		}
//		
//		void registerObserver(Observer o) {
//			observers.add(o);
//		}
//	}
//	private ObserverList observerList = new ObserverList();
//	
//	public void registerObserver(Observer o){
//		observerList.registerObserver(o);
//	}
	
	
	protected IJavaVariable variable;
	private String name;
	private final boolean isInstance;
	private boolean outOfScope;
	
	private List<T> history;
	
	public VariableModel(IJavaVariable variable, boolean isInstance, StackFrameModel model) {
		super(model);
		assert variable != null;
		this.variable = variable;
		this.isInstance = isInstance;
		try {
			this.name = variable.getName();
		} catch (DebugException e) {
			e.printStackTrace();
		}
		history = new ArrayList<>();
		history.add(getContent());
	}
	
	
	
	public String getName() {		
		return name;
	}
	
	public boolean isInstance() {
		return isInstance;
	}
	
	public String getType() {
		try {
			return variable.getReferenceTypeName();
		} catch (DebugException e) {
			e.printStackTrace();
			return null;
		}
	}
	@SuppressWarnings("unchecked")
	@Override
	public T getContent() {
		try {
			return (T) variable.getValue();
		}
		catch(DebugException e) {
			e.printStackTrace();
			return null;
		}
	}
	
	public IJavaType getVariableType() {
		try {
			return variable.getJavaType();
		} catch (DebugException e) {
			e.printStackTrace();
			return null;
		}
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public void update(int step) {
		try {
			boolean equals = variable.getValue().equals(history.get(history.size()-1));
			if(!equals) {
				history.add((T) variable.getValue());
				setChanged();
				notifyObservers();
//				observerList.notifyObservers(history, newValue);
			}
		}
		catch(DebugException e) {
			e.printStackTrace();
		}
	}
	
	public String getCurrentValue() {
		return history.get(history.size()-1).toString();
	}
	
	public List<T> getHistory() {
		return Collections.unmodifiableList(history);
	}

	public void setOutOfScope() {
		outOfScope = true;
		setChanged();
		notifyObservers();
	}

	public boolean isOutOfScope() {
		return outOfScope;
	}

}
