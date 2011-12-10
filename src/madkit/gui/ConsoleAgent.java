/*
 * Copyright 1997-2011 Fabien Michel, Olivier Gutknecht, Jacques Ferber
 * 
 * This file is part of MadKit.
 * 
 * MadKit is free software: you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation, either version 3 of the License, or (at your option) any
 * later version.
 * 
 * MadKit is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with MadKit. If not, see <http://www.gnu.org/licenses/>.
 */
package madkit.gui;

import java.io.PrintStream;

import javax.swing.JFrame;

import madkit.kernel.AbstractAgent;

/**
 * @author Fabien Michel
 * @since MadKit 5.0.0.14
 * @version 0.9
 * 
 */
public class ConsoleAgent extends AbstractAgent {
	
	final static private PrintStream systemOut = System.out;
	final static private PrintStream systemErr = System.err;
	
	@Override
	public void setupFrame(final JFrame frame) {
		OutputPanel outP;
		frame.add(outP = new OutputPanel(this));
		System.setErr(new PrintStream(outP.getOutputStream()));
		System.setOut(new PrintStream(outP.getOutputStream()));
		frame.setSize(800, 500);
	}
	
	@Override
	protected void end() {
		System.setErr(systemErr);
		System.setOut(systemOut);
	}

	
}
