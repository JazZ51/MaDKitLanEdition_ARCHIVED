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

import com.distrimind.madkit.exceptions.ConnectionException;
import com.distrimind.madkit.kernel.network.connection.ConnectionProtocolProperties;
import com.distrimind.util.crypto.ASymmetricPublicKey;
import com.distrimind.util.crypto.ASymmetricSignatureType;
import com.distrimind.util.crypto.EllipticCurveDiffieHellmanType;
import com.distrimind.util.crypto.SecureRandomType;
import com.distrimind.util.crypto.SymmetricEncryptionType;
import com.distrimind.util.crypto.SymmetricSignatureType;

/**
 * {@inheritDoc}
 * 
 * @author Jason Mahdjoub
 * @version 1.0
 * @since MadkitLanEdition 1.0
 */
public class ClientSecuredProtocolPropertiesWithKnownPublicKeyWithECDHAlgorithm
		extends ConnectionProtocolProperties<ClientSecuredConnectionProtocolWithKnownPublicKeyWithECDHAlgorithm> {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1775803204716519459L;

	public ClientSecuredProtocolPropertiesWithKnownPublicKeyWithECDHAlgorithm() {
		super(ClientSecuredConnectionProtocolWithKnownPublicKeyWithECDHAlgorithm.class);
	}

	/**
	 * Tells if the connection must be encrypted or not. If not, only signature
	 * packet will be enabled.
	 */
	public boolean enableEncryption = true;

	/**
	 * Set the encryption profile according properties given by the server side
	 * properties
	 * 
	 * @param identifier
	 *            the encryption identifier
	 * @param symmetricEncryptionType
	 *            the symmetric encryption type (if null, use default symmetric
	 *            encryption type and default key size)
	 * @param symmetricSignatureType
	 * 			the symmetric signature type (if null, use default symmetric
	 *            encryption type and default key size)
	 * @param publicKeyForEncryption
	 *            the public key for encryption
	 * @param publicKeyForSignature
	 *            the public key for signature
	 */
	public void setEncryptionProfile(int identifier, SymmetricEncryptionType symmetricEncryptionType,SymmetricSignatureType symmetricSignatureType,
			ASymmetricPublicKey publicKeyForSignature, EllipticCurveDiffieHellmanType ellipticCurveDiffieHellmanType) {
		this.setEncryptionProfile(identifier, symmetricEncryptionType,symmetricSignatureType,
				symmetricEncryptionType == null ? -1 : symmetricEncryptionType.getDefaultKeySizeBits(),publicKeyForSignature,
				null, ellipticCurveDiffieHellmanType);
	}

	/**
	 * Set the encryption profile according properties given by the server side
	 * properties
	 * 
	 * @param identifier
	 *            the encryption identifier
	 * @param symmetricEncryptionType
	 *            the symmetric encryption type (if null, use default symmetric
	 *            encryption type and default key size)
	 * @param symmetricKeySizeBits
	 *            the symmetric encryption key size in bits
	 * @param publicKeyForEncryption
	 *            the public key for encryption
	 * @param publicKeyForSignature
	 *            the public key for signature
	 * @param signatureType
	 *            the signature type (if null, use default signature type)
	 */
	public void setEncryptionProfile(int identifier, SymmetricEncryptionType symmetricEncryptionType,SymmetricSignatureType symmetricSignatureType,
			short symmetricKeySizeBits, ASymmetricPublicKey publicKeyForSignature, ASymmetricSignatureType signatureType, EllipticCurveDiffieHellmanType ellipticCurveDiffieHellmanType) {
		if (symmetricSignatureType == null)
			throw new NullPointerException("symmetricSignatureType");
		if (publicKeyForSignature == null)
			throw new NullPointerException("publicKey");
		if (publicKeyForSignature.getKeySize() < minASymetricKeySize)
			throw new IllegalArgumentException("The public key size must be greater than " + minASymetricKeySize);
		if (ellipticCurveDiffieHellmanType==null)
			throw new NullPointerException("ellipticCurveDiffieHellmanType");
		this.publicKeyForSignature=publicKeyForSignature;
		if (signatureType == null)
			signatureType = publicKeyForSignature.getAlgorithmType().getDefaultSignatureAlgorithm();
		this.asymmetricSignatureType = signatureType;
		keyIdentifier = identifier;
		if (symmetricEncryptionType != null) {
			this.symmetricEncryptionType = symmetricEncryptionType;
			this.SymmetricKeySizeBits = symmetricKeySizeBits;
			this.symmetricSignatureType=this.symmetricEncryptionType.getDefaultSignatureAlgorithm();
		}
		if(symmetricSignatureType!=null)
		{
			this.symmetricSignatureType=symmetricSignatureType;
		}
		this.ellipticCurveDiffieHellmanType=ellipticCurveDiffieHellmanType;
	}

	/**
	 * Set the encryption profile according properties given by the server side
	 * properties
	 * 
	 * @param serverProperties
	 *            the server side properties
	 */
	public void setEncryptionProfile(ServerSecuredProcotolPropertiesWithKnownPublicKeyWithECDHAlgorithm serverProperties) {
		enableEncryption = serverProperties.enableEncryption;
		setEncryptionProfile(serverProperties.getLastEncryptionProfileIdentifier(),
				serverProperties.getDefaultSymmetricEncryptionType(),
				serverProperties.getDefaultSymmetricSignatureType(),
				serverProperties.getDefaultSymmetricEncryptionKeySizeBits(),
				serverProperties.getDefaultKeyPairForSignature().getASymmetricPublicKey(),
				serverProperties.getDefaultASymmetricSignatureType(),
				serverProperties.getDefaultEllipticCurveDiffieHellmanType());
	}


	/**
	 * Gets the publicKey attached to this connection protocol
	 * 
	 * @return the publicKey attached to this connection protocol
	 */
	public ASymmetricPublicKey getPublicKeyForSignature() {
		return publicKeyForSignature;
	}

	/**
	 * Gest the signature attached to this connection protocol
	 * 
	 * @return the signature attached to this connection protocol
	 */
	public ASymmetricSignatureType getASymmetricSignatureType() {
		return asymmetricSignatureType;
	}

	/**
	 * Gets the encryption profile attached to this connection protocol
	 * 
	 * @return the encryption profile attached to this connection protocol
	 */
	public int getEncryptionProfileIndentifier() {
		return keyIdentifier;
	}

	/**
	 * Gets the symmetric encryption type attached to this connection protocol
	 * 
	 * @return the symmetric encryption type attached to this connection protocol
	 */
	public SymmetricEncryptionType getSymmetricEncryptionType() {
		return symmetricEncryptionType;
	}

	/**
	 * Gets the symmetric encryption key size in bits type attached to this
	 * connection protocol
	 * 
	 * @return the symmetric encryption key size in bits type attached to this
	 *         connection protocol
	 */
	public short getSymmetricKeySizeBits() {
		return SymmetricKeySizeBits;
	}


	/**
	 * The used public key
	 */
	private ASymmetricPublicKey publicKeyForSignature;

	
	
	/**
	 * key identifier
	 */
	private int keyIdentifier = 0;

	/**
	 * The minimum asymetric cipher RSA Key size
	 */
	public final int minASymetricKeySize = 1024;

	
	/**
	 * Symmetric encryption algorithm
	 */
	private SymmetricEncryptionType symmetricEncryptionType = SymmetricEncryptionType.DEFAULT;

	/**
	 * Symmetric signature algorithm
	 */
	private SymmetricSignatureType symmetricSignatureType=symmetricEncryptionType.getDefaultSignatureAlgorithm();

	/**
	 * The symmetric key size in bits
	 */
	private short SymmetricKeySizeBits = symmetricEncryptionType.getDefaultKeySizeBits();

	/**
	 * Elliptic Curve Diffie Hellman Type, used for symmetric key exchange
	 */
	private EllipticCurveDiffieHellmanType ellipticCurveDiffieHellmanType=EllipticCurveDiffieHellmanType.DEFAULT;
	
	/**
	 * Signature type
	 */
	private ASymmetricSignatureType asymmetricSignatureType = null;

	/**
	 * Default duration of a public key before being regenerated. Must be greater or
	 * equal than 0.
	 */
	public final long defaultASymmetricKeyExpirationMs = 15552000000l;

	/**
	 * The duration of a public key before being regenerated. Must be greater or
	 * equal than 0.
	 */
	public long aSymmetricKeyExpirationMs = defaultASymmetricKeyExpirationMs;

	private void checkPublicKey(ASymmetricPublicKey publicKey) throws ConnectionException
	{
		if (publicKey == null)
			throw new ConnectionException("The public key must defined");
		if (publicKey.getKeySize() < minASymetricKeySize)
			throw new ConnectionException("_rsa_key_size must be greater or equal than " + minASymetricKeySize
					+ " . Moreover, this number must correspond to this schema : _rsa_key_size=2^x.");
		if (publicKey.getTimeExpirationUTC() < System.currentTimeMillis()) {
			throw new ConnectionException("The distant public key has expired !");
		}

		if (publicKey.getTimeExpirationUTC() < System.currentTimeMillis()) {
			throw new ConnectionException("The given public key has expired");
		}
		int tmp = publicKey.getKeySize();
		while (tmp != 1) {
			if (tmp % 2 == 0)
				tmp = tmp / 2;
			else
				throw new ConnectionException("The RSA key size have a size of " + publicKey.getKeySize()
						+ ". This number must correspond to this schema : _rsa_key_size=2^x.");
		}
		
	}
	
	void checkProperties() throws ConnectionException {
		
		checkPublicKey(publicKeyForSignature);
		if (asymmetricSignatureType == null)
			asymmetricSignatureType = publicKeyForSignature.getAlgorithmType().getDefaultSignatureAlgorithm();
		if (symmetricEncryptionType==null)
			throw new ConnectionException(new NullPointerException());
		if (symmetricSignatureType==null)
			symmetricSignatureType=this.symmetricEncryptionType.getDefaultSignatureAlgorithm();
			
		if (ellipticCurveDiffieHellmanType==null)
			throw new ConnectionException(new NullPointerException());

	}

	@Override
	protected boolean needsServerSocketImpl() {
		return false;
	}

	@Override
	public boolean canTakeConnectionInitiativeImpl() {
		return true;
	}

	@Override
	public boolean supportBidirectionnalConnectionInitiativeImpl() {
		return false;
	}

	@Override
	protected boolean canBeServer() {
		return false;
	}

	public SymmetricSignatureType getSymmetricSignatureType() {
		return symmetricSignatureType;
	}

	public EllipticCurveDiffieHellmanType getEllipticCurveDiffieHellmanType() {
		return ellipticCurveDiffieHellmanType;
	}

	public ASymmetricSignatureType getAsymmetricSignatureType() {
		return asymmetricSignatureType;
	}
	/**
	 * Secure Random type
	 */
	public SecureRandomType secureRandomType;
}
