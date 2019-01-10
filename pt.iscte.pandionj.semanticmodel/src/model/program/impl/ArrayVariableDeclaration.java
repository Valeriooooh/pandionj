package model.program.impl;

import java.util.List;

import model.program.IArrayElementAssignment;
import model.program.IArrayElementExpression;
import model.program.IArrayVariableDeclaration;
import model.program.IDataType;
import model.program.IExpression;

class ArrayVariableDeclaration extends VariableDeclaration implements IArrayVariableDeclaration {
	private final int numberOfDimensions;
	
	public ArrayVariableDeclaration(Block block, String name, IDataType type, boolean constant, int numberOfDimensions) {
		super(block, name, type, constant);
		this.numberOfDimensions = numberOfDimensions;
	}

	@Override
	public boolean isReference() {
		return true;
	}

	@Override
	public int getArrayDimensions() {
		return numberOfDimensions;
	}

	@Override
	public IArrayElementExpression elementExpression(List<IExpression> indexes) {
		return new ArrayElementExpression(this, indexes);
	}
	
	@Override
	public IArrayElementAssignment elementAssignment(IExpression expression, List<IExpression> indexes) {
		return new ArrayElementAssignment(getParent(), this, indexes, expression);
	}
	
	@Override
	public String toString() {
		String text = "var " + getIdentifier() + " (" + getType() + " ";
		for(int i = 0; i < numberOfDimensions; i++)
			text += "[]";
		return text + ")";
	}
}
