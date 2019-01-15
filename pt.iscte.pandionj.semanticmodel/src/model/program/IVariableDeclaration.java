package model.program;

import model.machine.ICallStack;

public interface IVariableDeclaration extends IStatement, IIdentifiableElement {
	IProcedure getProcedure();
	
	IDataType getType();

	boolean isReference();
	boolean isConstant();
	boolean isParameter();
	
	default IVariableRole getRole() {
		return IVariableRole.NONE;
	}
	
	default boolean isGatherer() {
		return getRole() instanceof IGatherer;
	}
	
	IVariableAssignment assignment(IExpression exp);
	
	IVariableExpression expression();
	
	@Override
	default boolean execute(ICallStack callStack) {
		return true;
	}
	
	enum Flag {
		REFERENCE, CONSTANT, PARAM 
	}
}
