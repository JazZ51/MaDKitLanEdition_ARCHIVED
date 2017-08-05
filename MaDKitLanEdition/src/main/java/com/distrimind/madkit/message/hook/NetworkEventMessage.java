/*
 * MadKitLanEdition (created by Jason MAHDJOUB (jason.mahdjoub@distri-mind.fr)) Copyright (c)
 * 2015 is a fork of MadKit and MadKitGroupExtension. 
 * 
 * Copyright or © or Copr. Jason Mahdjoub, Fabien Michel, Olivier Gutknecht, Jacques Ferber (1997)
 * 
 * jason.mahdjoub@distri-mind.fr
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
package com.distrimind.madkit.message.hook;

import com.distrimind.madkit.kernel.network.Connection;
import com.distrimind.madkit.kernel.network.connection.ConnectionProtocol.ConnectionClosedReason;
import com.distrimind.madkit.message.hook.HookMessage.AgentActionEvent;

/**
 * This message is sent to agents that requested kernel's hook related to lan
 * events.
 * 
 * @author Jason Mahdjoub
 * @since MaDKitLanEdition 1.0
 * @version 1.0
 * @see AgentActionEvent
 * 
 */
public class NetworkEventMessage extends HookMessage {

	/**
	 * 
	 */
	private static final long serialVersionUID = 5183651413579513636L;

	private final Connection connection;

	public NetworkEventMessage(AgentActionEvent _hookType, Connection connection) {
		super(_hookType);
		if (_hookType == null)
			throw new NullPointerException("_hookType");
		if (!_hookType.equals(AgentActionEvent.CONNEXION_ESTABLISHED)
				&& !_hookType.equals(AgentActionEvent.CONNEXION_CLOSED_BECAUSE_OF_NETWORK_ANOMALY)
				&& !_hookType.equals(AgentActionEvent.CONNEXION_LOST)
				&& !_hookType.equals(AgentActionEvent.CONNEXION_PROPERLY_CLOSED))
			throw new IllegalArgumentException(
					"This agent action event is not managed by this message class : " + _hookType);
		if (connection == null)
			throw new NullPointerException("connection");
		this.connection = connection;

	}

	public NetworkEventMessage(ConnectionClosedReason connection_closed_reason, Connection connection) {
		this(connection_closed_reason.getAgentActionEvent(), connection);
	}

	public Connection getConnection() {
		return connection;
	}

	@Override
	public boolean equals(Object o) {
		if (o == null)
			return false;
		else if (o == this)
			return true;
		else if (o instanceof NetworkEventMessage) {
			NetworkEventMessage nem = (NetworkEventMessage) o;
			return nem.connection.equals(connection);
		} else
			return false;
	}

	@Override
	public int hashCode() {
		return connection.hashCode();
	}
}