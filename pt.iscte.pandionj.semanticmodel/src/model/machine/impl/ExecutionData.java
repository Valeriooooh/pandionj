package model.machine.impl;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import model.machine.ICallStack;
import model.machine.IExecutionData;
import model.program.IProcedure;

public class ExecutionData implements IExecutionData  {
	private Map<IProcedure, Integer> assignmentCount = new HashMap<IProcedure, Integer>();
	private int callCount = 0;
	private int operationCount = 0;
	private int callStackMax = 0;
	
	@Override
	public Map<IProcedure, Integer> getAssignmentData() {
		return Collections.unmodifiableMap(assignmentCount);
	}

	@Override
	public int getTotalAssignments() {
		int s = 0;
		for(Integer i : assignmentCount.values())
			s += i;
		return s;
	}
	
	@Override
	public int getCallStackDepth() {
		return callStackMax;
	}
	
	public void countAssignment(IProcedure p) {
		Integer c = assignmentCount.get(p);
		if(c == null)
			c = 1;
		else
			c++;
		assignmentCount.put(p, c);
	}

	public void updateCallStackSize(ICallStack stack) {
		callStackMax = Math.max(callStackMax, stack.getSize());
	}

	@Override
	public String toString() {
		String text = 
				"call stack depth: " + getCallStackDepth() + "\n" +
				"procedure calls: " + callCount + "\n" +
				"assignments: " + getTotalAssignments() + "\n" +
				"operations: " + operationCount;
		return text;
	}

	public void countCall() {
		callCount++;
	}
	
	public void countOperation() {
		operationCount++;
	}
}
