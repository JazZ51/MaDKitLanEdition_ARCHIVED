/*
 * Copyright 1997-2011 Fabien Michel, Olivier Gutknecht, Jacques Ferber
 * 
 * This file is part of MadKit.
 * 
 * MadKit is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * MadKit is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with MadKit. If not, see <http://www.gnu.org/licenses/>.
 */
package madkit.testing.util.agent;

import java.util.logging.Level;

import madkit.kernel.AbstractAgent;
import madkit.kernel.Madkit;
import madkit.kernel.Madkit.BooleanOption;
import madkit.kernel.Madkit.LevelOption;

/**
 * @author Fabien Michel
 * @since MadKit 5.0.0.5
 * @version 0.9
 * 
 */
@SuppressWarnings("serial")
public class FaultyAgent extends DoItDuringLifeCycleAgent {

	/**
	 * @param inActivate
	 * @param inLive
	 * @param inEnd
	 */
	public FaultyAgent(boolean inActivate, boolean inLive, boolean inEnd) {
		super(inActivate, inLive, inEnd);
	}

	public FaultyAgent(boolean inActivate, boolean inLive) {
		super(inActivate, inLive);
	}

	public FaultyAgent(boolean inActivate) {
		super(inActivate);
	}

	public FaultyAgent() {
	}

	@SuppressWarnings("null")
	@Override
	public void doIt() {
		Object o = null;
		o.toString();
	}
	
	public static void main(String[] args) {
		String[] myArgs = {LevelOption.agentLogLevel.toString(),Level.ALL.toString()};
		AbstractAgent.executeThisAgent(myArgs);
	}
}