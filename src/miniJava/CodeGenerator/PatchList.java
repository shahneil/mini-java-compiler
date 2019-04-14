package miniJava.CodeGenerator;

import java.util.Stack;

import mJAM.Machine;
import miniJava.AbstractSyntaxTrees.Declaration;

public class PatchList {

	// (instruction address, declaration) tuple
	private class PatchEntry {

		public int addr;
		public Declaration d;

		public PatchEntry(int addr, Declaration d) {
			this.addr = addr;
			this.d = d;
		}
	}

	private Stack<PatchEntry> patchList;

	public PatchList() {
		this.patchList = new Stack<>();
	}

	/**
	 * Add method call site to patch list (to be back-patched when code generation
	 * is completed).
	 */
	public void add(int addr, Declaration d) {
		patchList.push(new PatchEntry(addr, d));
	}

	/**
	 * Back-patch stored method calls.
	 */
	public void patch() {
		while (!patchList.isEmpty()) {
			PatchEntry entry = patchList.pop();
			Machine.patch(entry.addr, ((KnownAddress) entry.d.entity).offset);
		}
	}

}
