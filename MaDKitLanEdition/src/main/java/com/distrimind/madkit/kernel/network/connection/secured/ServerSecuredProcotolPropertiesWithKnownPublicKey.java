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

import java.util.HashMap;
import java.util.Map;

import com.distrimind.madkit.exceptions.BlockParserException;
import com.distrimind.madkit.exceptions.ConnectionException;
import com.distrimind.madkit.kernel.network.connection.ConnectionProtocolProperties;
import com.distrimind.util.crypto.ASymmetricEncryptionType;
import com.distrimind.util.crypto.ASymmetricKeyPair;
import com.distrimind.util.crypto.ASymmetricKeyWrapperType;
import com.distrimind.util.crypto.AbstractSecureRandom;
import com.distrimind.util.crypto.SecureRandomType;
import com.distrimind.util.crypto.SymmetricAuthentifiedSignatureType;
import com.distrimind.util.crypto.SymmetricEncryptionAlgorithm;
import com.distrimind.util.crypto.SymmetricEncryptionType;

import gnu.vm.jgnu.security.InvalidAlgorithmParameterException;
import gnu.vm.jgnu.security.NoSuchAlgorithmException;
import gnu.vm.jgnu.security.NoSuchProviderException;

/**
 * 
 * 
 * @author Jason Mahdjoub
 * @version 1.0
 * @since MadkitLanEdition 1.0
 */
public class ServerSecuredProcotolPropertiesWithKnownPublicKey
		extends ConnectionProtocolProperties<ServerSecuredConnectionProtocolWithKnwonPublicKey> {

	/**
	 * 
	 */
	private static final long serialVersionUID = -4979144000199527880L;

	public ServerSecuredProcotolPropertiesWithKnownPublicKey() {
		super(ServerSecuredConnectionProtocolWithKnwonPublicKey.class);
	}

	/**
	 * Generate and add an encryption profile with a new key pair, etc.
	 * 
	 * @param random
	 *            a secured random number generator
	 * @param as_type
	 *            tge asymmetric encryption type
	 * @param s_type
	 *            the symmetric encryption type (if null, use default encryption
	 *            type)
	 * @param keyWrapper the key wrapper type
	 * @return the encryption profile identifier
	 * @throws NoSuchAlgorithmException if the encryption algorithm was not found
	 * @throws InvalidAlgorithmParameterException if the encryption algorithm parameter was not valid
	 * @throws NoSuchProviderException if the encryption algorithm provider was not found 
	 */
	public int generateAndAddEncryptionProfile(AbstractSecureRandom random, ASymmetricEncryptionType as_type,
			SymmetricEncryptionType s_type, ASymmetricKeyWrapperType keyWrapper) throws NoSuchAlgorithmException, NoSuchProviderException, InvalidAlgorithmParameterException {
		return addEncryptionProfile(as_type.getKeyPairGenerator(random).generateKeyPair(), s_type, s_type.getDefaultKeySizeBits(), keyWrapper, null);
	}

	/**
	 * Generate and add an encryption profile with a new key pair, etc.
	 * 
	 * @param random
	 *            a secured random number generator
	 * @param as_type
	 *            tge asymmetric encryption type
	 * @param expirationTimeUTC
	 *            the UTC expiration time of the key pair
	 * @param asymmetricKeySizeBits
	 *            the asymmetric key size in bits
	 * 
	 * @param s_type
	 *            the symmetric encryption type (if null, use default encryption
	 *            type)
	 * @param symmetricKeySizeBits
	 *            the signature type (if null, use default signature type)
	 * @param keyWrapper the key wrapper type
	 * @param signatureType the signature type
	 * @return the encryption profile identifier
	 * @throws NoSuchAlgorithmException if the encryption algorithm was not found
	 * @throws InvalidAlgorithmParameterException if the encryption algorithm parameter was not valid
	 * @throws NoSuchProviderException if the encryption algorithm provider was not found
	 */
	public int generateAndAddEncryptionProfile(AbstractSecureRandom random, ASymmetricEncryptionType as_type,
			long expirationTimeUTC, short asymmetricKeySizeBits,
			SymmetricEncryptionType s_type, short symmetricKeySizeBits, ASymmetricKeyWrapperType keyWrapper, SymmetricAuthentifiedSignatureType signatureType) throws NoSuchAlgorithmException, NoSuchProviderException, InvalidAlgorithmParameterException {
		return addEncryptionProfile(
				as_type.getKeyPairGenerator(random, asymmetricKeySizeBits, expirationTimeUTC).generateKeyPair(),
				s_type, symmetricKeySizeBits, keyWrapper, signatureType);
	}

	/**
	 * Add an encryption profile with a new key pair, etc.
	 * 
	 * @param keyPairForEncryption
	 *            the key pair for encryption
	 * @param symmetricEncryptionType
	 *            the symmetric encryption type (if null, use default encryption
	 *            type)
	 * @param keyWrapper the key wrapper type
	 * @return the encryption profile identifier
	 */
	public int addEncryptionProfile(ASymmetricKeyPair keyPairForEncryption, SymmetricEncryptionType symmetricEncryptionType, ASymmetricKeyWrapperType keyWrapper) {
		return this.addEncryptionProfile(keyPairForEncryption, symmetricEncryptionType,
				symmetricEncryptionType == null ? (short) -1 : symmetricEncryptionType.getDefaultKeySizeBits(), keyWrapper, null);
	}

	/**
	 * Add an encryption profile with a new key pair, etc.
	 * 
	 * @param keyPairForEncryption
	 *            the key pair for encryption
	 * @param symmetricEncryptionType
	 *            the symmetric encryption type (if null, use default encryption
	 *            type)
	 * @param symmetricKeySizeBits
	 *            the symmetric key size in bits
	 * @param keyWrapper the key wrapper type
	 * @param signatureType
	 *            the signature type (if null, use default signature type)
	 * @return the encryption profile identifier
	 */
	public int addEncryptionProfile(ASymmetricKeyPair keyPairForEncryption,
			SymmetricEncryptionType symmetricEncryptionType, short symmetricKeySizeBits, ASymmetricKeyWrapperType keyWrapper, SymmetricAuthentifiedSignatureType signatureType) {
		if (keyPairForEncryption == null)
			throw new NullPointerException("keyPairForEncryption");
		keyPairsForEncryption.put(generateNewKeyPairIdentifier(), keyPairForEncryption);

		if (symmetricEncryptionType == null) {
			symmetricEncryptionType = SymmetricEncryptionType.DEFAULT;
			symmetricKeySizeBits = symmetricEncryptionType.getDefaultKeySizeBits();
		}
		symmetricEncryptionTypes.put(lastIdentifier, symmetricEncryptionType);
		symmetricEncryptionKeySizeBits.put(lastIdentifier, symmetricKeySizeBits);
		if (signatureType == null)
			signatures.put(lastIdentifier, symmetricEncryptionType.getDefaultSignatureAlgorithm());
		else
			signatures.put(lastIdentifier, signatureType);
		
		if (keyWrapper==null)
			keyWrapper=ASymmetricKeyWrapperType.DEFAULT;
		keyWrappers.put(lastIdentifier, keyWrapper);
			
		
		return lastIdentifier;
	}

	/**
	 * Gets the key pair used for encryption and attached to this connection protocol and the given profile
	 * identifier
	 * 
	 * @param profileIdentifier
	 *            the profile identifier
	 * @return the key pair attached to this connection protocol and the given
	 *         profile identifier
	 */
	public ASymmetricKeyPair getKeyPairForEncryption(int profileIdentifier) {
		return keyPairsForEncryption.get(profileIdentifier);
	}

	/**
	 * Gets the signature type attached to this connection protocol and the given
	 * profile identifier
	 * 
	 * @param profileIdentifier
	 *            the profile identifier
	 * @return the signature type attached to this connection protocol and the given
	 *         profile identifier
	 */
	public SymmetricAuthentifiedSignatureType getSignatureType(int profileIdentifier) {
		return signatures.get(profileIdentifier);
	}
	
	/**
	 * Gets the key wrapper attached to this connection protocol and the given profile identifier
	 * 
	 * @param profileIdentifier
	 *            the profile identifier
	 * @return the key wrapper attached to this connection protocol and the given profile identifier
	 */
	public ASymmetricKeyWrapperType getKeyWrapper(int profileIdentifier) {
		return keyWrappers.get(profileIdentifier);
	}
	

	public int getMaximumSignatureSizeBits() {
		int res = -1;
		for (SymmetricAuthentifiedSignatureType v : signatures.values()) {
			res = Math.max(res, v.getSignatureSizeInBits());
		}
		return res;
	}

	/**
	 * Gets the symmetric encryption type attached to this connection protocol and
	 * the given profile identifier
	 * 
	 * @param profileIdentifier
	 *            the profile identifier
	 * @return the symmetric encryption type attached to this connection protocol
	 *         and the given profile identifier
	 */
	public SymmetricEncryptionType getSymmetricEncryptionType(int profileIdentifier) {
		return symmetricEncryptionTypes.get(profileIdentifier);
	}

	/**
	 * Gets the symmetric encryption key size in bits attached to this connection
	 * protocol and the given profile identifier
	 * 
	 * @param profileIdentifier
	 *            the profile identifier
	 * @return the symmetric encryption key size in bits attached to this connection
	 *         protocol and the given profile identifier
	 */
	public short getSymmetricEncryptionKeySizeBits(int profileIdentifier) {
		return symmetricEncryptionKeySizeBits.get(profileIdentifier);
	}

	/**
	 * Gets the default key pair (for encryption) attached to this connection protocol and its
	 * default profile
	 * 
	 * @return the default key pair attached to this connection protocol and its
	 *         default profile
	 */
	public ASymmetricKeyPair getDefaultKeyPairForEncryption() {
		return keyPairsForEncryption.get(lastIdentifier);
	}

	/**
	 * Gets the default signature type attached to this connection protocol and its
	 * default profile
	 * 
	 * @return the default signature type attached to this connection protocol and
	 *         its default profile
	 */
	public SymmetricAuthentifiedSignatureType getDefaultSignatureType() {
		return signatures.get(lastIdentifier);
	}
	/**
	 * Gets the default key wrapper attached to this connection protocol and its
	 * default profile
	 * 
	 * @return the default key wrapper attached to this connection protocol and its
	 * default profile
	 */
	public ASymmetricKeyWrapperType getDefaultKeyWrapper() {
		return keyWrappers.get(lastIdentifier);
	}

	/**
	 * Gets the default symmetric encryption type type attached to this connection
	 * protocol and its default profile
	 * 
	 * @return the default symmetric encryption type attached to this connection
	 *         protocol and its default profile
	 */
	public SymmetricEncryptionType getDefaultSymmetricEncryptionType() {
		return symmetricEncryptionTypes.get(lastIdentifier);
	}

	/**
	 * Gets the default symmetric encryption key size in bits attached to this
	 * connection protocol and its default profile
	 * 
	 * @return the default symmetric encryption key size in bits attached to this
	 *         connection protocol and its default profile
	 */
	public short getDefaultSymmetricEncryptionKeySizeBits() {
		return symmetricEncryptionKeySizeBits.get(lastIdentifier);
	}

	/**
	 * Gets the last encryption profile identifier
	 * 
	 * @return the last encryption profile identifier
	 */
	public int getLastEncryptionProfileIdentifier() {
		return lastIdentifier;
	}

	/*
	 * public Map<Integer, ASymmetricPublicKey> getPublicKeys() { Map<Integer,
	 * ASymmetricPublicKey> res=new HashMap<>(); for (Map.Entry<Integer,
	 * ASymmetricKeyPair> e : keyPairs.entrySet()) { res.put(e.getKey(),
	 * e.getValue().getASymmetricPublicKey()); } return res; }
	 * 
	 * public Map<Integer, SignatureType> getSignatures() { Map<Integer,
	 * SignatureType> res=new HashMap<>(); for (Map.Entry<Integer, SignatureType> e
	 * : signatures.entrySet()) { res.put(e.getKey(), e.getValue()); } return res; }
	 */

	/**
	 * Tells if the connection must be encrypted or not. If not, only signature
	 * packet will be enabled.
	 */
	public boolean enableEncryption = true;


	/**
	 * The used key pairs for encryption
	 */
	private Map<Integer, ASymmetricKeyPair> keyPairsForEncryption = new HashMap<>();

	/**
	 * The used signatures
	 */
	private Map<Integer, SymmetricAuthentifiedSignatureType> signatures = new HashMap<>();

	/**
	 * The used key wrappers
	 */
	private Map<Integer, ASymmetricKeyWrapperType> keyWrappers = new HashMap<>();
	
	private int lastIdentifier = 0;

	private int generateNewKeyPairIdentifier() {
		return ++lastIdentifier;
	}

	/**
	 * The minimum asymetric cipher RSA Key size
	 */
	public final int minASymetricKeySizeBits = 2048;

	/**
	 * Symmetric encryption algorithm
	 */
	private Map<Integer, SymmetricEncryptionType> symmetricEncryptionTypes = new HashMap<>();

	/**
	 * Symmetric encryption key sizes bits
	 */
	private Map<Integer, Short> symmetricEncryptionKeySizeBits = new HashMap<>();

	/**
	 * Default duration of a public key before being regenerated. Must be greater or
	 * equal than 0.
	 */
	public final long defaultASymmetricKeyExpirationMs = 15552000000L;

	/**
	 * The duration of a public key before being regenerated. Must be greater or
	 * equal than 0.
	 */
	public long aSymmetricKeyExpirationMs = defaultASymmetricKeyExpirationMs;

	private boolean checkKeyPairs(Map<Integer, ASymmetricKeyPair> keyPairs) throws ConnectionException
	{
		if (keyPairs == null)
			throw new ConnectionException("The key pairs must defined");
		if (keyPairs.isEmpty())
			throw new ConnectionException("The key pairs must defined");
		boolean valid = false;
		for (Map.Entry<Integer, ASymmetricKeyPair> e : keyPairs.entrySet()) {
			if (e.getValue() == null)
				throw new NullPointerException();
			if (e.getValue().getTimeExpirationUTC() > System.currentTimeMillis()) {
				valid = true;
			}
			int tmp = e.getValue().getKeySizeBits();
			while (tmp != 1) {
				if (tmp % 2 == 0)
					tmp = tmp / 2;
				else
					throw new ConnectionException("The RSA key size have a size of " + e.getValue().getKeySizeBits()
							+ ". This number must correspond to this schema : _rsa_key_size=2^x.");
			}
			if (signatures.get(e.getKey()) == null)
				throw new NullPointerException("No signature found for identifier " + e.getKey());
			if (symmetricEncryptionTypes.get(e.getKey()) == null)
				throw new NullPointerException("No symmetric encryption type found for identifier " + e.getKey());
			if (symmetricEncryptionKeySizeBits.get(e.getKey()) == null)
				throw new NullPointerException(
						"No symmetric encryption key size bits found for identifier " + e.getKey());
		}
		if (keyPairs.get(this.lastIdentifier).getKeySizeBits() < minASymetricKeySizeBits)
			throw new ConnectionException("_rsa_key_size must be greater or equal than " + minASymetricKeySizeBits
					+ " . Moreover, this number must correspond to this schema : _rsa_key_size=2^x.");
		return valid;
	}
	
	void checkProperties() throws ConnectionException {
		boolean valid = checkKeyPairs(keyPairsForEncryption);
		
		if (!valid) {
			throw new ConnectionException("All given public keys has expired");
		}

	}

	@Override
	protected boolean needsServerSocketImpl() {
		return true;
	}

	@Override
	public boolean canTakeConnectionInitiativeImpl() {
		return false;
	}

	@Override
	public boolean supportBidirectionnalConnectionInitiativeImpl() {
		return false;
	}

	@Override
	protected boolean canBeServer() {
		return true;
	}
	private SymmetricEncryptionAlgorithm maxAlgo=null;
	int getMaximumOutputLengthForEncryption(int size) throws BlockParserException
	{
		try {
			if (maxAlgo==null)
			{
				int res=0;
				SymmetricEncryptionAlgorithm maxAlgo;
				
				for (Map.Entry<Integer, SymmetricEncryptionType> e : this.symmetricEncryptionTypes.entrySet())
				{
					maxAlgo=new SymmetricEncryptionAlgorithm(SecureRandomType.DEFAULT.getInstance(null), e.getValue().getKeyGenerator(SecureRandomType.DEFAULT.getInstance(null), this.symmetricEncryptionKeySizeBits.get(e.getKey())).generateKey());
					int v=maxAlgo.getOutputSizeForEncryption(size);
					if (v>=res)
					{
						res=v;
						this.maxAlgo=maxAlgo;
					}
				}
				return res;
			}
			return maxAlgo.getOutputSizeForEncryption(size);
		} catch (Exception e) {
			throw new BlockParserException(e);
		}
	}
	
}
