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
package com.distrimind.madkit.kernel.network.connection.secured;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.net.InetSocketAddress;
import java.util.Arrays;

import org.bouncycastle.crypto.InvalidWrappingException;

import gnu.vm.jgnu.security.InvalidAlgorithmParameterException;
import gnu.vm.jgnu.security.InvalidKeyException;
import gnu.vm.jgnu.security.NoSuchAlgorithmException;
import gnu.vm.jgnu.security.NoSuchProviderException;
import gnu.vm.jgnu.security.spec.InvalidKeySpecException;
import gnu.vm.jgnux.crypto.IllegalBlockSizeException;
import gnu.vm.jgnux.crypto.NoSuchPaddingException;

import com.distrimind.madkit.database.KeysPairs;
import com.distrimind.madkit.exceptions.BlockParserException;
import com.distrimind.madkit.exceptions.ConnectionException;
import com.distrimind.madkit.exceptions.MessageSerializationException;
import com.distrimind.madkit.kernel.MadkitProperties;
import com.distrimind.madkit.kernel.network.Block;
import com.distrimind.madkit.kernel.network.NetworkProperties;
import com.distrimind.madkit.kernel.network.PacketCounter;
import com.distrimind.madkit.kernel.network.PacketPartHead;
import com.distrimind.madkit.kernel.network.SubBlock;
import com.distrimind.madkit.kernel.network.SubBlockInfo;
import com.distrimind.madkit.kernel.network.SubBlockParser;
import com.distrimind.madkit.kernel.network.SystemMessage.Integrity;
import com.distrimind.madkit.kernel.network.connection.AskConnection;
import com.distrimind.madkit.kernel.network.connection.ConnectionFinished;
import com.distrimind.madkit.kernel.network.connection.ConnectionMessage;
import com.distrimind.madkit.kernel.network.connection.ConnectionProtocol;
import com.distrimind.madkit.kernel.network.connection.TransferedBlockChecker;
import com.distrimind.madkit.kernel.network.connection.UnexpectedMessage;
import com.distrimind.madkit.util.SerializationTools;
import com.distrimind.ood.database.DatabaseWrapper;

import com.distrimind.ood.database.exceptions.DatabaseException;
import com.distrimind.util.crypto.ASymmetricAuthentifiedSignatureCheckerAlgorithm;
import com.distrimind.util.crypto.ASymmetricAuthentifiedSignatureType;
import com.distrimind.util.crypto.ASymmetricAuthentifiedSignerAlgorithm;
import com.distrimind.util.crypto.ASymmetricKeyPair;
import com.distrimind.util.crypto.ASymmetricKeyWrapperType;
import com.distrimind.util.crypto.ASymmetricPublicKey;
import com.distrimind.util.crypto.AbstractSecureRandom;
import com.distrimind.util.crypto.Key;
import com.distrimind.util.crypto.SymmetricEncryptionAlgorithm;
import com.distrimind.util.crypto.SymmetricSecretKey;

/**
 * 
 * 
 * @author Jason Mahdjoub
 * @version 1.2
 * @since MadkitLanEdition 1.0
 */
public class P2PSecuredConnectionProtocolWithASymmetricKeyExchanger extends ConnectionProtocol<P2PSecuredConnectionProtocolWithASymmetricKeyExchanger> {
	Step current_step = Step.NOT_CONNECTED;

	private ASymmetricKeyPair myKeyPairForEncryption = null, myKeyPairForSignature;
	private ASymmetricPublicKey distant_public_key_for_encryption = null, distant_public_key_for_signature=null;
	private SymmetricSecretKey secret_key = null;
	
	protected SymmetricEncryptionAlgorithm symmetricEncryption = null;

	final int signature_size_bytes;
	private final SubBlockParser parser;

	private long aSymetricKeySizeExpiration;
	private final P2PSecuredConnectionProtocolWithASymmetricKeyExchangerProperties hproperties;
	private final ASymmetricKeyWrapperType keyWrapper;
	private final AbstractSecureRandom approvedRandom, approvedRandomForKeys;
	private boolean blockCheckerChanged = true;
	private boolean currentBlockCheckerIsNull = true;
	private ASymmetricAuthentifiedSignerAlgorithm signer=null;
	private ASymmetricAuthentifiedSignatureCheckerAlgorithm signatureChecker=null;
	private final PacketCounterForEncryptionAndSignature packetCounter;
	private boolean reinitSymmetricAlgorithm=true;
	
	private P2PSecuredConnectionProtocolWithASymmetricKeyExchanger(InetSocketAddress _distant_inet_address,
			InetSocketAddress _local_interface_address, ConnectionProtocol<?> _subProtocol,
			DatabaseWrapper sql_connection, MadkitProperties mkProperties, NetworkProperties _properties, int subProtocolLevel, boolean isServer,
			boolean mustSupportBidirectionnalConnectionInitiative) throws ConnectionException {
		super(_distant_inet_address, _local_interface_address, _subProtocol, sql_connection, _properties,
				subProtocolLevel, isServer, mustSupportBidirectionnalConnectionInitiative);
		hproperties = (P2PSecuredConnectionProtocolWithASymmetricKeyExchangerProperties) super.connection_protocol_properties;

		hproperties.checkProperties();

		signature_size_bytes = hproperties.signatureType.getSignatureSizeBytes(hproperties.aSymetricKeySize);
		keyWrapper=hproperties.keyWrapper;
		if (hproperties.aSymmetricKeyExpirationMs < 0)
			aSymetricKeySizeExpiration = hproperties.defaultASymmetricKeyExpirationMs;
		else
			aSymetricKeySizeExpiration = hproperties.aSymmetricKeyExpirationMs;
		try {
			approvedRandom=mkProperties.getApprovedSecureRandom();
			approvedRandomForKeys=mkProperties.getApprovedSecureRandomForKeys();

		} catch (NoSuchAlgorithmException | NoSuchProviderException e) {
			throw new ConnectionException(e);
		}
		this.packetCounter=new PacketCounterForEncryptionAndSignature(approvedRandom, hproperties.enableEncryption, false);
		
		if (hproperties.enableEncryption)
			parser = new ParserWithEncryption();
		else
			parser = new ParserWithNoEncryption();
	}
	
	private void reinitSymmetricAlgorithmIfNecessary() throws InvalidKeyException, NoSuchAlgorithmException, NoSuchPaddingException, InvalidAlgorithmParameterException, NoSuchProviderException, InvalidKeySpecException
	{
		if (reinitSymmetricAlgorithm)
		{
			reinitSymmetricAlgorithm=false;
			symmetricEncryption=new SymmetricEncryptionAlgorithm(this.approvedRandom, this.secret_key, (byte)packetCounter.getMyEncryptionCounter().length);
		}
	}

	private void setPublicPrivateKeys() throws ConnectionException {
		blockCheckerChanged |= !(myKeyPairForEncryption == null || myKeyPairForSignature==null 
				|| (isCrypted() ? current_step.compareTo(Step.WAITING_FIRST_MESSAGE) <= 0
						: current_step.compareTo(Step.WAITING_FIRST_MESSAGE) < 0));
		try {

			if (sql_connection != null)
			{
				myKeyPairForEncryption = ((KeysPairs) sql_connection.getTableInstance(KeysPairs.class)).getKeyPair(
						distant_inet_address.getAddress(), NetworkProperties.connectionProtocolDatabaseUsingCodeForEncryption,
						hproperties.aSymetricEncryptionType, hproperties.aSymetricKeySize, approvedRandomForKeys,
						aSymetricKeySizeExpiration, network_properties.maximumNumberOfCryptoKeysForIpsSpectrum);
				myKeyPairForSignature = ((KeysPairs) sql_connection.getTableInstance(KeysPairs.class)).getKeyPair(
						distant_inet_address.getAddress(), NetworkProperties.connectionProtocolDatabaseUsingCodeForSignature,
						hproperties.signatureType, hproperties.aSymetricKeySize, approvedRandomForKeys,
						aSymetricKeySizeExpiration, network_properties.maximumNumberOfCryptoKeysForIpsSpectrum);
			}
			else
			{
				myKeyPairForEncryption = hproperties.aSymetricEncryptionType
						.getKeyPairGenerator(approvedRandomForKeys, hproperties.aSymetricKeySize).generateKeyPair();
				myKeyPairForSignature = hproperties.signatureType
						.getKeyPairGenerator(approvedRandomForKeys, hproperties.aSymetricKeySize).generateKeyPair();
			}
			this.signer = new ASymmetricAuthentifiedSignerAlgorithm(myKeyPairForSignature.getASymmetricPrivateKey());
			
		} catch (NoSuchAlgorithmException | DatabaseException | NoSuchProviderException | InvalidAlgorithmParameterException e) {
			myKeyPairForEncryption = null;
			myKeyPairForSignature = null;
			
			throw new ConnectionException(e);
		}

	}

	private void setNewPublicPrivateKeys() throws ConnectionException {
		blockCheckerChanged |= !(myKeyPairForEncryption == null || myKeyPairForSignature==null
				|| (isCrypted() ? current_step.compareTo(Step.WAITING_FIRST_MESSAGE) <= 0
						: current_step.compareTo(Step.WAITING_FIRST_MESSAGE) < 0));
		try {
			
			if (sql_connection != null)
			{
				myKeyPairForEncryption = (((KeysPairs) sql_connection.getTableInstance(KeysPairs.class)).getNewKeyPair(
						distant_inet_address.getAddress(), NetworkProperties.connectionProtocolDatabaseUsingCodeForEncryption,
						hproperties.aSymetricEncryptionType, hproperties.aSymetricKeySize, approvedRandomForKeys,
						aSymetricKeySizeExpiration, network_properties.maximumNumberOfCryptoKeysForIpsSpectrum));
				myKeyPairForSignature = (((KeysPairs) sql_connection.getTableInstance(KeysPairs.class)).getNewKeyPair(
						distant_inet_address.getAddress(), NetworkProperties.connectionProtocolDatabaseUsingCodeForSignature,
						hproperties.signatureType, hproperties.aSymetricKeySize, approvedRandomForKeys,
						aSymetricKeySizeExpiration, network_properties.maximumNumberOfCryptoKeysForIpsSpectrum));
				
			}
			else
			{
				myKeyPairForEncryption = hproperties.aSymetricEncryptionType
						.getKeyPairGenerator(approvedRandomForKeys, hproperties.aSymetricKeySize).generateKeyPair();
				myKeyPairForSignature = hproperties.signatureType
						.getKeyPairGenerator(approvedRandomForKeys, hproperties.aSymetricKeySize).generateKeyPair();
				
			}
			this.signer = new ASymmetricAuthentifiedSignerAlgorithm(myKeyPairForSignature.getASymmetricPrivateKey());
		} catch (NoSuchAlgorithmException | DatabaseException | NoSuchProviderException | InvalidAlgorithmParameterException e) {
			myKeyPairForEncryption = null;
			myKeyPairForSignature = null;

			
			throw new ConnectionException(e);
		}
	}

	/*private void checkAsymetricAlgorithm() throws ConnectionException {
		try {
			if (myKeyPairForEncryption != null && distant_public_key_for_encryption != null) {
				if (aSymmetricAlgorithm == null) {
					aSymmetricAlgorithm = new P2PASymmetricEncryptionAlgorithm(myKeyPairForEncryption, distant_public_key_for_encryption);
				}
			} else {
				aSymmetricAlgorithm = null;
			}
		} catch (InvalidKeyException | NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeySpecException
				| NoSuchProviderException e) {
			throw new ConnectionException(e);
		}
	}*/

	private void setDistantPublicKey(ASymmetricPublicKey _keyForEncryption, ASymmetricPublicKey _keyForSignature) throws ConnectionException {
		distant_public_key_for_encryption = _keyForEncryption;
		distant_public_key_for_signature = _keyForSignature;
		try
		{
			this.signatureChecker = new ASymmetricAuthentifiedSignatureCheckerAlgorithm(distant_public_key_for_signature);
		}
		catch(NoSuchAlgorithmException | NoSuchProviderException e)
		{
			throw new ConnectionException(e);
		}
		
	}

	private void resetPublicPrivateKeys() {
		blockCheckerChanged = true;
		myKeyPairForEncryption = null;
		myKeyPairForSignature = null;
		distant_public_key_for_encryption = null;
		distant_public_key_for_signature = null;
		secret_key = null;
	}

	private enum Step {
		NOT_CONNECTED, WAITING_FOR_PUBLIC_KEY, WAITING_FOR_SECRET_KEY, WAITING_FIRST_MESSAGE, WAITING_FOR_CONNECTION_CONFIRMATION, CONNECTED,
	}

	private void generateSecretKey() throws ConnectionException {
		try {
			secret_key = hproperties.symmetricEncryptionType.getKeyGenerator(approvedRandomForKeys, hproperties.SymmetricKeySizeBits)
					.generateKey();
			symmetricEncryption = new SymmetricEncryptionAlgorithm(approvedRandom, secret_key);
		} catch (NoSuchAlgorithmException | InvalidKeyException | NoSuchPaddingException
				| InvalidAlgorithmParameterException | NoSuchProviderException | InvalidKeySpecException e) {
			secret_key = null;
			symmetricEncryption = null;
			throw new ConnectionException(e);
		}
	}

	private byte[] encodeSecretKey() throws ConnectionException {
		try {
			return keyWrapper.wrapKey(approvedRandom, distant_public_key_for_encryption, secret_key);
		} catch (InvalidKeyException | InvalidAlgorithmParameterException | IOException | IllegalBlockSizeException
				| IllegalStateException | NoSuchAlgorithmException | InvalidKeySpecException
				| NoSuchProviderException | NoSuchPaddingException e) {
			throw new ConnectionException(e);
		}

	}

	private void decodeSecretKey(byte[] _secret_key) throws ConnectionException {
		try {
			
			secret_key = keyWrapper.unwrapKey(myKeyPairForEncryption.getASymmetricPrivateKey(), _secret_key);
			symmetricEncryption = new SymmetricEncryptionAlgorithm(approvedRandom, secret_key);
		} catch (InvalidKeyException | InvalidAlgorithmParameterException | IOException | NoSuchAlgorithmException
				| NoSuchPaddingException | NoSuchProviderException
				| IllegalArgumentException | InvalidKeySpecException | IllegalStateException | InvalidWrappingException e) {
			throw new ConnectionException(e);
		}
	}

	@Override
	protected ConnectionMessage getNextStep(ConnectionMessage _m) throws ConnectionException {
		switch (current_step) {
		case NOT_CONNECTED: {
			if (_m instanceof AskConnection) {
				AskConnection ask = (AskConnection) _m;
				setPublicPrivateKeys();
				if (ask.isYouAreAsking()) {
					current_step = Step.WAITING_FOR_PUBLIC_KEY;
					return new AskConnection(false);
				} else {
					current_step = Step.WAITING_FOR_PUBLIC_KEY;
					return new PublicKeyMessage(myKeyPairForEncryption.getASymmetricPublicKey(), distant_public_key_for_encryption, myKeyPairForSignature.getASymmetricPublicKey(), distant_public_key_for_signature, myKeyPairForSignature.getASymmetricPrivateKey());
				}
			} else if (_m instanceof ConnectionFinished) {
				if (((ConnectionFinished) _m).getState()
						.equals(ConnectionProtocol.ConnectionState.CONNECTION_ESTABLISHED)) {
					return new UnexpectedMessage(this.getDistantInetSocketAddress());
				} else
					return new ConnectionFinished(this.getDistantInetSocketAddress(),
							ConnectionClosedReason.CONNECTION_ANOMALY);
			} else {
				return new UnexpectedMessage(this.getDistantInetSocketAddress());
			}
		}
		case WAITING_FOR_PUBLIC_KEY: {
			if (_m instanceof PublicKeyMessage) {
				if (((PublicKeyMessage) _m).getPublicKeyForEncryption().equals(myKeyPairForEncryption.getASymmetricPublicKey())
						|| ((PublicKeyMessage) _m).getPublicKeyForSignature().equals(myKeyPairForSignature.getASymmetricPublicKey())) {
					current_step = Step.WAITING_FOR_PUBLIC_KEY;
					setNewPublicPrivateKeys();
					return new SimilarPublicKeysError();
				}
				
				setDistantPublicKey(((PublicKeyMessage) _m).getPublicKeyForEncryption(), ((PublicKeyMessage) _m).getPublicKeyForSignature());
				//checkAsymetricAlgorithm();
				if (isCurrentServerAskingConnection()) {
					current_step = Step.WAITING_FOR_SECRET_KEY;
					return new PublicKeyMessage(myKeyPairForEncryption.getASymmetricPublicKey(), distant_public_key_for_encryption, myKeyPairForSignature.getASymmetricPublicKey(), distant_public_key_for_signature, myKeyPairForSignature.getASymmetricPrivateKey());
				} else {
					// generateSecretKey();
					current_step = Step.WAITING_FIRST_MESSAGE;
					generateSecretKey();
					byte[] encoded_secret_key = encodeSecretKey();
					return new SecretKeyMessage(encoded_secret_key);
				}
			} else if (_m instanceof SimilarPublicKeysError) {
				setNewPublicPrivateKeys();
				return new PublicKeyMessage(myKeyPairForEncryption.getASymmetricPublicKey(), distant_public_key_for_encryption, myKeyPairForSignature.getASymmetricPublicKey(), distant_public_key_for_signature, myKeyPairForSignature.getASymmetricPrivateKey());
			} else if (_m instanceof ConnectionFinished) {
				if (((ConnectionFinished) _m).getState()
						.equals(ConnectionProtocol.ConnectionState.CONNECTION_ESTABLISHED)) {
					return new UnexpectedMessage(this.getDistantInetSocketAddress());
				} else
					return new ConnectionFinished(this.getDistantInetSocketAddress(),
							ConnectionClosedReason.CONNECTION_ANOMALY);
			} else {
				return new UnexpectedMessage(this.getDistantInetSocketAddress());
			}
		}
		case WAITING_FOR_SECRET_KEY: {
			if (_m instanceof SecretKeyMessage) {
				try {
					decodeSecretKey(((SecretKeyMessage) _m).secret_key);
				} catch (ConnectionException e) {
					return new IncomprehensibleSecretKey();
				}
				current_step = Step.WAITING_FIRST_MESSAGE;
				return new FirstMessage();
			} else if (_m instanceof ConnectionFinished) {
				if (((ConnectionFinished) _m).getState()
						.equals(ConnectionProtocol.ConnectionState.CONNECTION_ESTABLISHED)) {
					return new UnexpectedMessage(this.getDistantInetSocketAddress());
				} else
					return new ConnectionFinished(this.getDistantInetSocketAddress(),
							ConnectionClosedReason.CONNECTION_ANOMALY);
			} else {
				return new UnexpectedMessage(this.getDistantInetSocketAddress());
			}
		}
		case WAITING_FIRST_MESSAGE: {
			if (_m instanceof IncomprehensibleSecretKey) {
				generateSecretKey();
				current_step = Step.WAITING_FIRST_MESSAGE;
				byte[] encoded_secret_key = encodeSecretKey();
				return new SecretKeyMessage(encoded_secret_key);
			} else if (_m instanceof FirstMessage) {
				current_step = Step.WAITING_FOR_CONNECTION_CONFIRMATION;
				if (!isCurrentServerAskingConnection()) {
					return new FirstMessage();
				} else {
					return new ConnectionFinished(getDistantInetSocketAddress(), packetCounter.getMyEncodedCounters());
				}
			} else if (_m instanceof ConnectionFinished) {
				if (((ConnectionFinished) _m).getState()
						.equals(ConnectionProtocol.ConnectionState.CONNECTION_ESTABLISHED)) {
					return new UnexpectedMessage(this.getDistantInetSocketAddress());
				} else
					return new ConnectionFinished(this.getDistantInetSocketAddress(),
							ConnectionClosedReason.CONNECTION_ANOMALY);
			} else {
				return new UnexpectedMessage(this.getDistantInetSocketAddress());
			}
		}
		case WAITING_FOR_CONNECTION_CONFIRMATION: {
			if (_m instanceof ConnectionFinished && ((ConnectionFinished) _m).getState()
					.equals(ConnectionProtocol.ConnectionState.CONNECTION_ESTABLISHED)) {
				current_step = Step.CONNECTED;
				if (!packetCounter.setDistantCounters(((ConnectionFinished) _m).getInitialCounter()))
				{
					current_step=Step.NOT_CONNECTED;
					return new UnexpectedMessage(this.getDistantInetSocketAddress());
				}
				
				if (!isCurrentServerAskingConnection())
					return new ConnectionFinished(getDistantInetSocketAddress(), packetCounter.getMyEncodedCounters());
				else
					return null;
			} else if (_m instanceof ConnectionFinished) {
				return new UnexpectedMessage(this.getDistantInetSocketAddress());
			} else {
				return new UnexpectedMessage(this.getDistantInetSocketAddress());
			}

		}
		case CONNECTED: {
			if (_m instanceof ConnectionFinished) {
				ConnectionFinished cf = (ConnectionFinished) _m;
				if (!cf.getState().equals(ConnectionProtocol.ConnectionState.CONNECTION_ESTABLISHED)) {
					if (cf.getState().equals(ConnectionProtocol.ConnectionState.CONNECTION_CLOSED)) {
						return new ConnectionFinished(this.getDistantInetSocketAddress(),
								ConnectionClosedReason.CONNECTION_PROPERLY_CLOSED);
					} else {
						return new ConnectionFinished(this.getDistantInetSocketAddress(),
								ConnectionClosedReason.CONNECTION_LOST);
					}
				}
				else
				{
					if (!packetCounter.setDistantCounters(((ConnectionFinished) _m).getInitialCounter()))
					{
						current_step=Step.NOT_CONNECTED;
						return new UnexpectedMessage(this.getDistantInetSocketAddress());
					}
				}
				return null;
			} else {
				return new UnexpectedMessage(this.getDistantInetSocketAddress());
			}
		}
		}
		return null;
	}

	@Override
	public boolean isCrypted() {
		return hproperties.enableEncryption;
	}

	@Override
	protected void closeConnection(ConnectionClosedReason _reason) {
		if (_reason.equals(ConnectionClosedReason.CONNECTION_ANOMALY))
			resetPublicPrivateKeys();
		current_step = Step.NOT_CONNECTED;
	}

	private class ParserWithEncryption extends SubBlockParser {
		public ParserWithEncryption() {

		}

		@Override
		public int getBodyOutputSizeForEncryption(int size) throws BlockParserException {
			try {
				switch (current_step) {
				case NOT_CONNECTED:
				case WAITING_FOR_PUBLIC_KEY:
				case WAITING_FOR_SECRET_KEY:
				case WAITING_FIRST_MESSAGE: {
					return size;
				}
				case WAITING_FOR_CONNECTION_CONFIRMATION:
				case CONNECTED:
					if (packetCounter.isDistantActivated())
					{
						reinitSymmetricAlgorithmIfNecessary();
					}
					return symmetricEncryption.getOutputSizeForEncryption(size)+4;
				}
			} catch (Exception e) {
				throw new BlockParserException(e);
			}
			return size;

		}
		
		private SymmetricEncryptionAlgorithm maxAlgo=null;
		@Override
		public int getMaximumBodyOutputSizeForEncryption(int size) throws BlockParserException {
			try {
				if (maxAlgo==null)
				{
					maxAlgo=new SymmetricEncryptionAlgorithm(approvedRandom, hproperties.symmetricEncryptionType.getKeyGenerator(approvedRandom, hproperties.SymmetricKeySizeBits).generateKey());
				}
				return maxAlgo.getOutputSizeForEncryption(size)+4;
			} catch (Exception e) {
				throw new BlockParserException(e);
			}
		}

		@Override
		public int getBodyOutputSizeForDecryption(int size) throws BlockParserException {
			try {
				switch (current_step) {
				case NOT_CONNECTED:
				case WAITING_FOR_PUBLIC_KEY:
				case WAITING_FOR_SECRET_KEY: {
					return size;
				}
				case WAITING_FIRST_MESSAGE: {
					if (isCurrentServerAskingConnection())
					{
						if (getPacketCounter().isLocalActivated())
						{
							reinitSymmetricAlgorithmIfNecessary();
						}
						return symmetricEncryption.getOutputSizeForDecryption(size-4);
					}
					else
						return size;
				}
				case WAITING_FOR_CONNECTION_CONFIRMATION:
				case CONNECTED:
					if (getPacketCounter().isLocalActivated())
					{
						reinitSymmetricAlgorithmIfNecessary();
					}
					return symmetricEncryption.getOutputSizeForDecryption(size-4);

				}
			} catch (Exception e) {
				throw new BlockParserException(e);
			}
			return size;

		}

		@Override
		public SubBlockInfo getSubBlock(SubBlock _block) throws BlockParserException {

			switch (current_step) {
			case NOT_CONNECTED:
			case WAITING_FOR_PUBLIC_KEY:
			case WAITING_FOR_SECRET_KEY: {

				return getSubBlockWithNoEncryption(_block);
			}
			case WAITING_FIRST_MESSAGE:
				if (isCurrentServerAskingConnection()) {
					return getSubBlockWithEncryption(_block);
				} else {
					return getSubBlockWithNoEncryption(_block);
				}

			case WAITING_FOR_CONNECTION_CONFIRMATION:
			case CONNECTED: {
				return getSubBlockWithEncryption(_block);
			}

			}
			throw new BlockParserException("Unexpected exception");
		}

		public SubBlockInfo getSubBlockWithNoEncryption(SubBlock _block) throws BlockParserException {
			return new SubBlockInfo(new SubBlock(_block.getBytes(), _block.getOffset() + getSizeHead(),
					getBodyOutputSizeForDecryption(_block.getSize() - getSizeHead())), true, false);
		}

		public SubBlockInfo getSubBlockWithEncryption(SubBlock _block) throws BlockParserException {
			final int off=_block.getOffset() + getSizeHead();
			int offr=_block.getOffset()+_block.getSize();
			boolean excludedFromEncryption=_block.getBytes()[offr-1]==1;
			if (excludedFromEncryption)
			{
				int s=Block.getShortInt(_block.getBytes(), offr-4);
				if (s>Block.BLOCK_SIZE_LIMIT || s>_block.getSize()-getSizeHead()-4  || s<PacketPartHead.getHeadSize(false))
					throw new BlockParserException();
				try{
					
					

					SubBlock res = new SubBlock(_block.getBytes(), off,
							s);

					signatureChecker.init(_block.getBytes(), _block.getOffset(),
							signature_size_bytes);
					signatureChecker.update(_block.getBytes(),
							off, _block.getSize() - getSizeHead());
					boolean check = signatureChecker.verify();
			

					return new SubBlockInfo(res, check, !check);
				} catch (Exception e) {
					SubBlock res = new SubBlock(_block.getBytes(), off,
							getBodyOutputSizeForDecryption(_block.getSize() - getSizeHead()));
					return new SubBlockInfo(res, false, true);
				}
			}
			else
			{
				int s=_block.getSize() - getSizeHead()-4;
				
				try (ByteArrayInputStream bais = new ByteArrayInputStream(_block.getBytes(),
						off, s)) {
					final byte []tab=new byte[_block.getBytes().length];
					
					ConnectionProtocol.ByteArrayOutputStream os=new ConnectionProtocol.ByteArrayOutputStream(tab, off);
					
					
					boolean check;
					signatureChecker.init(_block.getBytes(), _block.getOffset(),
							signature_size_bytes);
					signatureChecker.update(_block.getBytes(),
							off, _block.getSize() - getSizeHead());
					check = signatureChecker.verify();
					SubBlock res;
					if (check)
					{
						if (getPacketCounter().isLocalActivated())
						{
							reinitSymmetricAlgorithmIfNecessary();
							symmetricEncryption.decode(bais, os, packetCounter.getMyEncryptionCounter());
						}
						else
							symmetricEncryption.decode(bais, os);
						res = new SubBlock(tab, off, os.getSize());
					}
					else 
					{
						res = new SubBlock(tab, off, symmetricEncryption.getOutputSizeForDecryption(s));
					}
					
					
					return new SubBlockInfo(res, check, !check);
				} catch (Exception e) {
					SubBlock res = new SubBlock(_block.getBytes(), _block.getOffset() + getSizeHead(),
							getBodyOutputSizeForDecryption(_block.getSize() - getSizeHead()));
					return new SubBlockInfo(res, false, true);
				}
			}

		}

		@Override
		public SubBlock getParentBlock(final SubBlock _block, boolean excludeFromEncryption) throws BlockParserException {
			try {
				switch (current_step) {
				case NOT_CONNECTED:
				case WAITING_FOR_PUBLIC_KEY:
				case WAITING_FOR_SECRET_KEY:
				case WAITING_FIRST_MESSAGE: {
					return getParentBlockWithNoTreatments(_block);
				}
				case WAITING_FOR_CONNECTION_CONFIRMATION:
				case CONNECTED: {
					
					
					int outputSize = getBodyOutputSizeForEncryption(_block.getSize());
					int s=outputSize + getSizeHead();
					if (excludeFromEncryption)
					{
						final SubBlock res = new SubBlock(_block.getBytes(), _block.getOffset() - getSizeHead(),s);
						int off=_block.getSize()+_block.getOffset();
						byte[] tab=res.getBytes();
						Arrays.fill(tab, off, outputSize+_block.getOffset()-4, (byte)0);
						
						int offr=res.getOffset()+res.getSize();
						tab[offr-1]=1;
						Block.putShortInt(tab, offr-4, _block.getSize());
						signer.init();
						signer.update(res.getBytes(), _block.getOffset(),
								outputSize);
						
						signer.getSignature(res.getBytes(), res.getOffset());

						return res;
					}
					else
					{
						final SubBlock res = new SubBlock(new byte[_block.getBytes().length], _block.getOffset() - getSizeHead(),s);
						
						res.getBytes()[res.getOffset()+res.getSize()-1]=0;
						if (packetCounter.isDistantActivated())
						{
							reinitSymmetricAlgorithmIfNecessary();
							symmetricEncryption.encode(_block.getBytes(), _block.getOffset(), _block.getSize(), null, 0, 0, new ConnectionProtocol.ByteArrayOutputStream(res.getBytes(), _block.getOffset()), packetCounter.getOtherEncryptionCounter());
						}
						else
							symmetricEncryption.encode(_block.getBytes(), _block.getOffset(), _block.getSize(), null, 0, 0, new ConnectionProtocol.ByteArrayOutputStream(res.getBytes(), _block.getOffset()));
						
						//System.arraycopy(tmp, 0, res.getBytes(), _block.getOffset(), tmp.length);
						signer.init();
						signer.update(res.getBytes(), _block.getOffset(),
								outputSize);
						
						signer.getSignature(res.getBytes(), res.getOffset());
						return res;
					}
					
				}
				}

			} catch (Exception e) {
				throw new BlockParserException(e);
			}
			throw new BlockParserException("Unexpected exception");

		}

		@Override
		public int getSizeHead() {
			return P2PSecuredConnectionProtocolWithASymmetricKeyExchanger.this.signature_size_bytes;
		}

		@Override
		public int getMaximumSizeHead() {
			return getSizeHead();
		}


		private SubBlockInfo checkEntrantPointToPointTransferedBlockWithNoEncryption(SubBlock _block) throws BlockParserException
		{
			return new SubBlockInfo(new SubBlock(_block.getBytes(), _block.getOffset() + getSizeHead(),
					_block.getSize() - getSizeHead()), true, false);
			
		}
		private SubBlockInfo checkEntrantPointToPointTransferedBlockWithEncryption(SubBlock _block) throws BlockParserException
		{
			try {
				SubBlock res = new SubBlock(_block.getBytes(), _block.getOffset() + getSizeHead(),
						_block.getSize() - getSizeHead());

				boolean check = signatureChecker
						.verify(res.getBytes(), res.getOffset(), res.getSize(), _block.getBytes(),
								_block.getOffset(), P2PSecuredConnectionProtocolWithASymmetricKeyExchanger.this.signature_size_bytes);
				
				return new SubBlockInfo(res, check, !check);
			} catch (Exception e) {
				SubBlock res = new SubBlock(_block.getBytes(), _block.getOffset() + getSizeHead(),
						getBodyOutputSizeForDecryption(_block.getSize() - getSizeHead()));
				return new SubBlockInfo(res, false, true);
			}
			
		}
		
		@Override
		public SubBlockInfo checkEntrantPointToPointTransferedBlock(SubBlock _block) throws BlockParserException {
			switch (current_step) {
			case NOT_CONNECTED:
			case WAITING_FOR_PUBLIC_KEY:
			case WAITING_FOR_SECRET_KEY: {

				return checkEntrantPointToPointTransferedBlockWithNoEncryption(_block);
			}
			case WAITING_FIRST_MESSAGE:
				if (isCurrentServerAskingConnection()) {
					return checkEntrantPointToPointTransferedBlockWithEncryption(_block);
				} else {
					return checkEntrantPointToPointTransferedBlockWithNoEncryption(_block);
				}

			case WAITING_FOR_CONNECTION_CONFIRMATION:
			case CONNECTED: {
				return checkEntrantPointToPointTransferedBlockWithEncryption(_block);
			}

			}
			throw new BlockParserException("Unexpected exception");
		}

		@Override
		public SubBlock signIfPossibleSortantPointToPointTransferedBlock(SubBlock _block) throws BlockParserException {
			try {
				SubBlock res = new SubBlock(_block.getBytes(), _block.getOffset() - getSizeHead(),
						_block.getSize() + getSizeHead());
				
				switch (current_step) {
				case NOT_CONNECTED:
				case WAITING_FOR_PUBLIC_KEY:
				case WAITING_FOR_SECRET_KEY:
				case WAITING_FIRST_MESSAGE: {
					byte[] tab=res.getBytes();
					for (int i=res.getOffset();i<_block.getOffset();i++)
						tab[i]=0;

					return res;
				}
				case WAITING_FOR_CONNECTION_CONFIRMATION:
				case CONNECTED: {

					signer.sign(_block.getBytes(), _block.getOffset(), _block.getSize(),
							res.getBytes(), res.getOffset(), P2PSecuredConnectionProtocolWithASymmetricKeyExchanger.this.signature_size_bytes);
					return res;
				}
				}

			} catch (Exception e) {
				throw new BlockParserException(e);
			}
			throw new BlockParserException("Unexpected exception");

		}

	}

	private class ParserWithNoEncryption extends SubBlockParser {
		public ParserWithNoEncryption() {

		}

		@Override
		public int getBodyOutputSizeForEncryption(int size) {
			return size;
		}

		@Override
		public SubBlockInfo getSubBlock(SubBlock _block) throws BlockParserException {

			switch (current_step) {
			case NOT_CONNECTED:
			case WAITING_FOR_PUBLIC_KEY:
				return new SubBlockInfo(new SubBlock(_block.getBytes(), _block.getOffset() + getSizeHead(),
						getBodyOutputSizeForDecryption(_block.getSize() - getSizeHead())), true, false);
			case WAITING_FOR_SECRET_KEY: {
				if (!isCurrentServerAskingConnection()) {
					return new SubBlockInfo(new SubBlock(_block.getBytes(), _block.getOffset() + getSizeHead(),
							getBodyOutputSizeForDecryption(_block.getSize() - getSizeHead())), true, false);
				} 
			}
			case WAITING_FIRST_MESSAGE:
				
			case WAITING_FOR_CONNECTION_CONFIRMATION:
			case CONNECTED: {
				SubBlock res = new SubBlock(_block.getBytes(), _block.getOffset() + getSizeHead(),
						getBodyOutputSizeForDecryption(_block.getSize() - getSizeHead()));
				try {
					signatureChecker.init(_block.getBytes(),
							_block.getOffset(), signature_size_bytes);
					signatureChecker.update(res.getBytes(), res.getOffset(), _block.getSize() - getSizeHead());
					boolean check = signatureChecker.verify();

					return new SubBlockInfo(res, check, !check);
				} catch (Exception e) {
					return new SubBlockInfo(res, false, true);
				}
			}

			}
			throw new BlockParserException("Unexpected exception");
		}

		@Override
		public SubBlock getParentBlock(SubBlock _block, boolean excludeFromEncryption) throws BlockParserException {
			try {

				switch (current_step) {
				case NOT_CONNECTED:
				case WAITING_FOR_PUBLIC_KEY:
				case WAITING_FOR_SECRET_KEY: {
					return getParentBlockWithNoTreatments(_block);
				}
				case WAITING_FIRST_MESSAGE:
				case WAITING_FOR_CONNECTION_CONFIRMATION:
				case CONNECTED: {
					int output=getBodyOutputSizeForEncryption(_block.getSize());
					SubBlock res = new SubBlock(_block.getBytes(), _block.getOffset() - getSizeHead(),
							output + getSizeHead());
					int off=_block.getSize()+_block.getOffset();
					byte[] tab=res.getBytes();
					Arrays.fill(tab, off, output+_block.getOffset(), (byte)0);
					
					signer.init();
					signer.update(_block.getBytes(), _block.getOffset(), output);
					
					signer.getSignature(res.getBytes(), res.getOffset());
					return res;
				}
				}

			} catch (Exception e) {
				throw new BlockParserException(e);
			}
			throw new BlockParserException("Unexpected exception");

		}

		@Override
		public int getSizeHead() {
			return P2PSecuredConnectionProtocolWithASymmetricKeyExchanger.this.signature_size_bytes;
		}

		@Override
		public int getBodyOutputSizeForDecryption(int _size) {
			return _size;
		}

		@Override
		public int getMaximumSizeHead() {
			return getSizeHead();
		}

		private SubBlockInfo checkEntrantPointToPointTransferedBlockWithNoEncryption(SubBlock _block) throws BlockParserException
		{
			return new SubBlockInfo(new SubBlock(_block.getBytes(), _block.getOffset() + getSizeHead(),
					_block.getSize() - getSizeHead()), true, false);
			
		}
		private SubBlockInfo checkEntrantPointToPointTransferedBlockWithEncryption(SubBlock _block) throws BlockParserException
		{
			try {
				SubBlock res = new SubBlock(_block.getBytes(), _block.getOffset() + getSizeHead(),
						_block.getSize() - getSizeHead());

				boolean check = signatureChecker
						.verify(res.getBytes(), res.getOffset(), res.getSize(), _block.getBytes(),
								_block.getOffset(), P2PSecuredConnectionProtocolWithASymmetricKeyExchanger.this.signature_size_bytes);
				return new SubBlockInfo(res, check, !check);
			} catch (Exception e) {
				SubBlock res = new SubBlock(_block.getBytes(), _block.getOffset() + getSizeHead(),
						getBodyOutputSizeForDecryption(_block.getSize() - getSizeHead()));
				return new SubBlockInfo(res, false, true);
			}
			
		}
		
		@Override
		public SubBlockInfo checkEntrantPointToPointTransferedBlock(SubBlock _block) throws BlockParserException {
			switch (current_step) {
			case NOT_CONNECTED:
			case WAITING_FOR_PUBLIC_KEY:
			case WAITING_FOR_SECRET_KEY: 
			case WAITING_FIRST_MESSAGE:{

				return checkEntrantPointToPointTransferedBlockWithNoEncryption(_block);
			}
			case WAITING_FOR_CONNECTION_CONFIRMATION:
			case CONNECTED: {
				return checkEntrantPointToPointTransferedBlockWithEncryption(_block);
			}

			}
			throw new BlockParserException("Unexpected exception");
		}

		@Override
		public SubBlock signIfPossibleSortantPointToPointTransferedBlock(SubBlock _block) throws BlockParserException {
			try {
				SubBlock res = new SubBlock(_block.getBytes(), _block.getOffset() - getSizeHead(),
						_block.getSize() + getSizeHead());
				
				switch (current_step) {
				case NOT_CONNECTED:
				case WAITING_FOR_PUBLIC_KEY:
				case WAITING_FOR_SECRET_KEY:
				{
					byte[] tab=res.getBytes();
					for (int i=res.getOffset();i<_block.getOffset();i++)
						tab[i]=0;

					return res;
				}
				case WAITING_FIRST_MESSAGE: 
				case WAITING_FOR_CONNECTION_CONFIRMATION:
				case CONNECTED: {

					signer.sign(_block.getBytes(), _block.getOffset(), _block.getSize(),
							res.getBytes(), res.getOffset(), P2PSecuredConnectionProtocolWithASymmetricKeyExchanger.this.signature_size_bytes);
					return res;
				}
				}

			} catch (Exception e) {
				throw new BlockParserException(e);
			}
			throw new BlockParserException("Unexpected exception");

		}

		@Override
		public int getMaximumBodyOutputSizeForEncryption(int size) {
			return size;
		}
		
	}

	@Override
	public SubBlockParser getParser() {
		return parser;
	}

	@Override
	protected TransferedBlockChecker getTransferedBlockChecker(TransferedBlockChecker subBlockChercker)
			throws ConnectionException {
		try {
			blockCheckerChanged = false;
			if (myKeyPairForEncryption == null || myKeyPairForSignature == null || (isCrypted() ? current_step.compareTo(Step.WAITING_FIRST_MESSAGE) <= 0
					: current_step.compareTo(Step.WAITING_FIRST_MESSAGE) < 0)) {
				currentBlockCheckerIsNull = true;
				return new ConnectionProtocol.NullBlockChecker(subBlockChercker, this.isCrypted(),
						(short) parser.getSizeHead());
			} else {
				currentBlockCheckerIsNull = false;
				return new BlockChecker(subBlockChercker, this.hproperties.signatureType,
						this.myKeyPairForSignature.getASymmetricPublicKey(), this.signature_size_bytes, this.isCrypted());
			}
		} catch (Exception e) {
			blockCheckerChanged = true;
			throw new ConnectionException(e);
		}
	}

	@SuppressWarnings("ExternalizableWithoutPublicNoArgConstructor")
	static class BlockChecker extends TransferedBlockChecker {
		/**
		 * 
		 */
		private static final long serialVersionUID = 4833363274931312673L;
		
		private ASymmetricAuthentifiedSignatureType signatureType;
		private int signatureSize;
		private transient ASymmetricAuthentifiedSignatureCheckerAlgorithm signatureChecker;
		private ASymmetricPublicKey publicKey;
		@SuppressWarnings("unused")
		BlockChecker()
		{
			
		}
		@Override
		public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
			super.readExternal(in);
			Enum<?> e=SerializationTools.readEnum(in, false);
			if (!(e instanceof ASymmetricAuthentifiedSignatureType))
				throw new MessageSerializationException(Integrity.FAIL_AND_CANDIDATE_TO_BAN);
			signatureType=(ASymmetricAuthentifiedSignatureType)e;
			signatureSize=in.readInt();
			Key k=SerializationTools.readKey(in, false);
			if (!(k instanceof ASymmetricPublicKey))
				throw new MessageSerializationException(Integrity.FAIL_AND_CANDIDATE_TO_BAN);
			publicKey=(ASymmetricPublicKey)k;
			try
			{
				initSignature();
			}
			catch(Exception e2)
			{
				throw new MessageSerializationException(Integrity.FAIL_AND_CANDIDATE_TO_BAN, e2);
			}
		}

		@Override
		public void writeExternal(ObjectOutput oos) throws IOException {
			super.writeExternal(oos);
			SerializationTools.writeEnum(oos, signatureType, false);
			oos.writeInt(signatureSize);
			SerializationTools.writeKey(oos, publicKey, false);
		}
		
		@Override
		public int getInternalSerializedSize() {
			return SerializationTools.getInternalSize(signatureType, 0)+4+SerializationTools.getInternalSize(publicKey, 0);
		}
		
		protected BlockChecker(TransferedBlockChecker _subChecker, ASymmetricAuthentifiedSignatureType signatureType,
				ASymmetricPublicKey publicKey, int signatureSize, boolean isCrypted) throws NoSuchAlgorithmException, NoSuchProviderException {
			super(_subChecker, !isCrypted);
			this.signatureType = signatureType;
			this.publicKey = publicKey;
			this.signatureSize = signatureSize;
			initSignature();
		}

		

		private void initSignature() throws NoSuchAlgorithmException, NoSuchProviderException {
			this.signatureChecker = new ASymmetricAuthentifiedSignatureCheckerAlgorithm(publicKey);
		}

	

		@Override
		public SubBlockInfo checkSubBlock(SubBlock _block) throws BlockParserException {
			try {
				SubBlock res = new SubBlock(_block.getBytes(), _block.getOffset() + signatureSize,
						_block.getSize() - signatureSize);
				
				
				
				boolean check = signatureChecker.verify(res.getBytes(), res.getOffset(), res.getSize(), _block.getBytes(), _block.getOffset(), signatureSize);
				return new SubBlockInfo(res, check, !check);
			} catch (Exception e) {
				throw new BlockParserException(e);
			}
		}

		

		

	}

	@Override
	public boolean needsMadkitLanEditionDatabase() {
		return true;
	}

	@Override
	public boolean isTransferBlockCheckerChangedImpl() {
		if (myKeyPairForEncryption == null || myKeyPairForSignature == null || (isCrypted() ? current_step.compareTo(Step.WAITING_FIRST_MESSAGE) <= 0
				: current_step.compareTo(Step.WAITING_FIRST_MESSAGE) < 0)) {
			return !currentBlockCheckerIsNull || blockCheckerChanged;
		} else
			return currentBlockCheckerIsNull || blockCheckerChanged;

	}

	@Override
	public PacketCounter getPacketCounter() {
		
		return packetCounter;
	}

}
