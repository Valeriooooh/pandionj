package pt.iscte.pandionj.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import org.eclipse.debug.core.DebugException;

import pt.iscte.pandionj.extensibility.IEntityModel;
import pt.iscte.pandionj.extensibility.IReferenceModel;
import pt.iscte.pandionj.extensibility.IStackFrameModel;
import pt.iscte.pandionj.extensibility.IVariableModel;

public class StaticRefsContainer implements IStackFrameModel {
	// la.la.Class.VAR -> 
	private Map<String, IVariableModel<?>> map;
	private RuntimeModel runtime;
	
	public StaticRefsContainer(RuntimeModel runtime) {
		this.runtime = runtime;
		map = new HashMap<>();
	}
	
	public boolean existsVar(StackFrameModel stackFrameModel, String varName) {
		return get(stackFrameModel, varName) != null;
	}
	
	public void add(StackFrameModel frame, IVariableModel<?> v) {
		try {
			String id = frame.getStackFrame().getDeclaringTypeName() + "." + v.getName();
			map.put(id,v);
		}
		catch (DebugException e) {
			e.printStackTrace();
		}
	}
	
	public IVariableModel<?> get(StackFrameModel frame, String varName) {
		try {
			String id = frame.getStackFrame().getDeclaringTypeName() + "." + varName;
			return map.get(id);
		} catch (DebugException e) {
			e.printStackTrace();
		}
		return null;
	}

	@Override
	public Collection<IVariableModel<?>> getStackVariables() {
		// TODO filter by stack
		StackFrameModel topFrame = runtime.getTopFrame();
		try {
			String type = topFrame.getStackFrame().getDeclaringTypeName();
			System.out.println("??? " + map.entrySet().stream()
					.filter((e) -> e.getKey().substring(0, e.getKey().lastIndexOf('.')).equals(type))
					.map((e) -> e.getValue())
					.collect(Collectors.toList()));
			return map.entrySet().stream()
					.filter((e) -> e.getKey().substring(0, e.getKey().lastIndexOf('.')).equals(type))
					.map((e) -> e.getValue())
					.collect(Collectors.toList());
		} catch (DebugException e1) {
			e1.printStackTrace();
		}
		return Collections.emptyList();
	}

	@Override
	public Collection<IVariableModel<?>> getAllVariables() {
		return map.values();
	}

	@Override
	public Collection<IReferenceModel> getReferencesTo(IEntityModel e) {
		Collection<IReferenceModel> refs = new ArrayList<>();
		for(IVariableModel<?> v : map.values())
			if(v instanceof IReferenceModel && ((IReferenceModel) v).getModelTarget() == e)
				refs.add((IReferenceModel) v);

		return refs;
	}

	@Override
	public RuntimeModel getRuntime() {
		return runtime;
	}

	
	
}
