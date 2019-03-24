package miniJava.ContextualAnalyzer;

import java.util.HashMap;
import java.util.Stack;

import miniJava.AbstractSyntaxTrees.Declaration;

public class IdentificationTable {

	Stack<HashMap<String, Declaration>> table;

	public IdentificationTable() {
		table = new Stack<>();
		openScope();
	}

	/**
	 * Creates a new entry in the identification table for the given identifier and
	 * declaration on the current level. If there is already an entry for the same
	 * identifier at the current level, returns false. Otherwise, if successful,
	 * returns true.
	 * 
	 * @param id   Identifier
	 * @param decl Declaration
	 * @return false if an entry for id exists at current level, true otherwise
	 */
	public boolean enter(String id, Declaration decl) {
		HashMap<String, Declaration> top = table.peek();

		// If level >= 4, check level 3 up until top level for duplicates
		int level = getLevel();
		if (level >= 4) {
			for (int i = 3; i < level; i++) {
				if (table.get(i).containsKey(id)) {
					return false;
				}
			}
		}

		// Check top level for duplicates
		if (!top.containsKey(id)) {
			top.put(id, decl);
			return true;
		}

		return false;
	}

	/**
	 * Finds an entry for the given identifier, if any. If there are several entries
	 * for the identifier, fetches the entry at the highest level.
	 * 
	 * @param id Identifier
	 * @return null if no entry is found, otherwise returns the declaration at the
	 *         highest level.
	 */
	public Declaration retrieve(String id) {

		// Iterate through each level, from highest to lowest.
		for (int level = table.size() - 1; level >= 0; level--) {
			HashMap<String, Declaration> current = table.get(level);
			if (current.containsKey(id)) {
				return current.get(id);
			}
		}

		return null;
	}

	public void openScope() {
		table.push(new HashMap<String, Declaration>());
	}

	public void closeScope() {
		table.pop();
	}

	private int getLevel() {
		return table.isEmpty() ? 0 : table.size() - 1;
	}
}
