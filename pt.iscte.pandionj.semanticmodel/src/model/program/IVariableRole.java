package model.program;

public interface IVariableRole {
	String getName();
	
	IVariableRole NONE = new IVariableRole() {
		public String getName() {
			return "no role";
		}
	};
}