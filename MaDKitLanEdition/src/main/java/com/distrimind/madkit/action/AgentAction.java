/*
 * Copyright or © or Copr. Fabien Michel, Olivier Gutknecht, Jacques Ferber (1997)
 * 
 * fmichel@lirmm.fr
 * olg@no-distance.net
 * ferber@lirmm.fr
 * 
 * This software is a computer program whose purpose is to
 * provide a lightweight Java library for designing and simulating Multi-Agent Systems (MAS).
 * This software is governed by the CeCILL-C license under French law and
 * abiding by the rules of distribution of free software.  You can  use,
 * modify and/ or redistribute the software under the terms of the CeCILL-C
 * license as circulated by CEA, CNRS and INRIA at the following URL
 * "http://www.cecill.info".
 * As a counterpart to the access to the source code and  rights to copy,
 * modify and redistribute granted by the license, users are provided only
 * with a limited warranty  and the software's author,  the holder of the
 * economic rights,  and the successive licensors  have only  limited
 * liability.
 * 
 * In this respect, the user's attention is drawn to the risks associated
 * with loading,  using,  modifying and/or developing or reproducing the
 * software by the user in light of its specific status of free software,
 * that may mean  that it is complicated to manipulate,  and  that  also
 * therefore means  that it is reserved for developers  and  experienced
 * professionals having in-depth computer knowledge. Users are therefore
 * encouraged to load and test the software's suitability as regards their
 * requirements in conditions enabling the security of their systems and/or
 * data to be ensured and,  more generally, to use and operate it in the
 * same conditions as regards security.
 * The fact that you are presently reading this means that you have had
 * knowledge of the CeCILL-C license and that you accept its terms.
 */
package com.distrimind.madkit.action;

import static java.awt.event.KeyEvent.VK_DOLLAR;
import static java.awt.event.KeyEvent.VK_E;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.ResourceBundle;

import javax.swing.Action;

import com.distrimind.madkit.i18n.I18nUtilities;
import com.distrimind.madkit.kernel.AbstractAgent;
import com.distrimind.madkit.message.EnumMessage;

/**
 * Enum representing agent actions
 * 
 * @author Fabien Michel
 * @since MaDKit 5.0.0.14
 * @version 0.9
 * 
 */
public enum AgentAction {

	LAUNCH_AGENT(KeyEvent.VK_U), RELOAD(VK_E), CREATE_GROUP(VK_DOLLAR), REQUEST_ROLE(VK_DOLLAR), LEAVE_ROLE(
			VK_DOLLAR), LEAVE_GROUP(
					VK_DOLLAR), SEND_MESSAGE(VK_DOLLAR), BROADCAST_MESSAGE(VK_DOLLAR), KILL_AGENT(KeyEvent.VK_K);

	final static private ResourceBundle messages = I18nUtilities.getResourceBundle(AgentAction.class.getSimpleName());

	private ActionInfo actionInfo;

	final private int keyEvent;

	/**
	 * @return the actionInfo corresponding to this constant
	 */
	public ActionInfo getActionInfo() {
		if (actionInfo == null)
			actionInfo = new ActionInfo(this, keyEvent, messages);
		return actionInfo;
	}

	AgentAction(int keyEvent) {
		this.keyEvent = keyEvent;
	}

	/**
	 * Builds an action that will make the agent do the corresponding behavior
	 * 
	 * @param agent
	 *            the agent on which this action will operate
	 * @param parameters
	 *            the info to be used
	 * @return the action corresponding to the enum
	 */
	public Action getActionFor(final AbstractAgent agent, final Object... parameters) {
		return new MDKAbstractAction(getActionInfo()) {
			/**
			 * 
			 */
			private static final long serialVersionUID = -3078505474395164899L;

			@Override
			public void actionPerformed(ActionEvent e) {// TODO I could do the check validity here for logging purpose
				if (agent.isAlive()) {
					agent.proceedEnumMessage(new EnumMessage<>(AgentAction.this, parameters));
				}
			}
		};
	}

}
