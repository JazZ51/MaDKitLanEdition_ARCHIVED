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
package com.distrimind.madkit.kernel.network.connection.access;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Collection;

import gnu.vm.jgnu.security.InvalidAlgorithmParameterException;
import gnu.vm.jgnu.security.InvalidKeyException;
import gnu.vm.jgnu.security.NoSuchAlgorithmException;
import gnu.vm.jgnu.security.NoSuchProviderException;
import gnu.vm.jgnu.security.spec.InvalidKeySpecException;
import gnu.vm.jgnux.crypto.BadPaddingException;
import gnu.vm.jgnux.crypto.IllegalBlockSizeException;
import gnu.vm.jgnux.crypto.NoSuchPaddingException;
import gnu.vm.jgnux.crypto.ShortBufferException;

import com.distrimind.madkit.exceptions.MessageSerializationException;
import com.distrimind.madkit.kernel.network.NetworkProperties;
import com.distrimind.madkit.util.SerializationTools;
import com.distrimind.madkit.util.ExternalizableAndSizable;
import com.distrimind.util.crypto.P2PASymmetricSecretMessageExchanger;

/**
 * 
 * @author Jason Mahdjoub
 * @version 1.0
 * @since MadkitLanEdition 1.0
 */
@SuppressWarnings("ExternalizableWithoutPublicNoArgConstructor")
class IdPwMessage extends AccessMessage {

	/**
	 * 
	 */
	private static final long serialVersionUID = -4294113307376791980L;

	private boolean identifiersIsEncrypted;
	private Identifier[] identifiers;
	private EncryptedPassword[] passwords;
	private final transient short nbAnomalies;

	@SuppressWarnings("unused")
	IdPwMessage()
	{
		nbAnomalies=0;
	}
	
	
	@Override
	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
		identifiersIsEncrypted=in.readBoolean();
		ExternalizableAndSizable[] s=SerializationTools.readExternalizableAndSizables(in, NetworkProperties.GLOBAL_MAX_SHORT_DATA_SIZE, false);
		assert s != null;
		identifiers=new Identifier[s.length];
		int total=5;
		for (int i=0;i<s.length;i++)
		{
			if (identifiersIsEncrypted)
			{
				if (!(s[i] instanceof EncryptedIdentifier))
					throw new MessageSerializationException(Integrity.FAIL_AND_CANDIDATE_TO_BAN);
			}
			else if (!(s[i] instanceof Identifier))
				throw new MessageSerializationException(Integrity.FAIL_AND_CANDIDATE_TO_BAN);
			identifiers[i]=(Identifier)s[i];
			total+=identifiers[i].getInternalSerializedSize();
		}
		s=SerializationTools.readExternalizableAndSizables(in, NetworkProperties.GLOBAL_MAX_SHORT_DATA_SIZE-total, false);
		assert s != null;
		passwords=new EncryptedPassword[s.length];
		for (int i=0;i<s.length;i++)
		{
			if (!(s[i] instanceof EncryptedPassword))
				throw new MessageSerializationException(Integrity.FAIL_AND_CANDIDATE_TO_BAN);
			passwords[i]=(EncryptedPassword)s[i];
		}
		if (identifiers.length != passwords.length)
			throw new MessageSerializationException(Integrity.FAIL_AND_CANDIDATE_TO_BAN);

		
	}

	@Override
	public void writeExternal(ObjectOutput oos) throws IOException {
		oos.writeBoolean(identifiersIsEncrypted);
		SerializationTools.writeExternalizableAndSizables(oos, identifiers, NetworkProperties.GLOBAL_MAX_SHORT_DATA_SIZE, false);
		SerializationTools.writeExternalizableAndSizables(oos, passwords, NetworkProperties.GLOBAL_MAX_SHORT_DATA_SIZE, false);

	}
	
	public IdPwMessage(Collection<IdentifierPassword> _id_pws, P2PASymmetricSecretMessageExchanger cipher,
			boolean encryptIdentifiers, short nbAnomalies) throws InvalidKeyException, IOException,
			IllegalBlockSizeException, BadPaddingException, NoSuchAlgorithmException, InvalidKeySpecException,
			NoSuchPaddingException, InvalidAlgorithmParameterException, NoSuchProviderException, IllegalStateException, ShortBufferException {
		identifiersIsEncrypted = encryptIdentifiers;
		identifiers = new Identifier[_id_pws.size()];
		passwords = new EncryptedPassword[_id_pws.size()];
		int index = 0;
		for (IdentifierPassword ip : _id_pws) {
			if (identifiersIsEncrypted)
				identifiers[index] = new EncryptedIdentifier(ip.getIdentifier(), cipher);
			else
				identifiers[index] = ip.getIdentifier();
			passwords[index] = new EncryptedPassword(ip.getPassword(), cipher);
			index++;
		}
		this.nbAnomalies = nbAnomalies;
	}

	@Override
	public short getNbAnomalies() {
		return nbAnomalies;
	}
	
	

	public short getAcceptedIdentifiers(LoginData lp, P2PASymmetricSecretMessageExchanger cipher,
			Collection<PairOfIdentifiers> acceptedIdentifiers, Collection<PairOfIdentifiers> deniedIdentifiers,
			Collection<IdentifierPassword> localIdentifiersAndPasswords)
			throws AccessException, InvalidKeyException, IllegalAccessException, IOException, IllegalBlockSizeException,
			BadPaddingException, NoSuchAlgorithmException, InvalidKeySpecException, NoSuchProviderException {
		acceptedIdentifiers.clear();
		deniedIdentifiers.clear();
		localIdentifiersAndPasswords.clear();

		for (int i = 0; i < identifiers.length; i++) {
			Identifier id = identifiers[i];
			Identifier decodedID;
			if (identifiersIsEncrypted)
				decodedID = lp.getIdentifier((EncryptedIdentifier) id, cipher);
			else
				decodedID = id;
			if (decodedID != null) {
				Identifier localID = lp.localiseIdentifier(decodedID);
				if (localID != null) {
					PairOfIdentifiers pair = new PairOfIdentifiers(localID, decodedID);

					EncryptedPassword ep = passwords[i];
					PasswordKey opw = lp.getPassword(decodedID);
					if (opw == null) {
						deniedIdentifiers.add(pair);
					} else {
						if (ep.verifyWithLocalPassword(opw, cipher)) {
							acceptedIdentifiers.add(pair);
							localIdentifiersAndPasswords.add(new IdentifierPassword(localID, opw));
						} else
							deniedIdentifiers.add(pair);
					}
				}
			}
		}

		return identifiers.length - acceptedIdentifiers.size() > Short.MAX_VALUE ? Short.MAX_VALUE
				: (short) (identifiers.length - acceptedIdentifiers.size());
	}


	@Override
	public boolean checkDifferedMessages() {
		return false;
	}

	

	
}
