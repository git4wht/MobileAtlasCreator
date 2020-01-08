/*******************************************************************************
 * Copyright (c) MOBAC developers
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package mobac.program.download;

import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

import org.apache.commons.codec.binary.Hex;
import org.apache.log4j.Logger;

import mobac.exceptions.NotImplementedException;
import mobac.program.model.Settings;

public class MobacTrustManager implements X509TrustManager {

	private static Logger log = Logger.getLogger(MobacTrustManager.class);

	private final X509TrustManager defaultTrustManager;

	private final Set<X509Certificate> certCache = ConcurrentHashMap.newKeySet();

	public MobacTrustManager() {
		super();
		TrustManagerFactory tmf;
		try {
			tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
			tmf.init((KeyStore) null);
		} catch (KeyStoreException | NoSuchAlgorithmException e) {
			throw new RuntimeException(e);
		}
		X509TrustManager defaultTm = null;
		for (TrustManager tm : tmf.getTrustManagers()) {
			if (tm instanceof X509TrustManager) {
				defaultTm = (X509TrustManager) tm;
				break;
			}
		}
		if (defaultTm == null) {
			throw new RuntimeException("Failed to get default Trustmanager");
		}
		defaultTrustManager = defaultTm;
	}

	@Override
	public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
		throw new NotImplementedException();
	}

	@Override
	public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
		try {
			defaultTrustManager.checkClientTrusted(chain, authType);
		} catch (CertificateException e) {
			X509Certificate cert = chain[0]; // get the leaf certificate
			if (certCache.contains(cert)) {
				return;
			}
			log.error("SSL error: " + e.getMessage());
			synchronized (this) {
				if (certCache.contains(cert)) {
					return;
				}
				String hash = getCertificateHash(cert);
				if (Settings.getInstance().trustedCertificates.contains(hash)) {
					return; // certificate is trusted
				}
				// TODO: Add GUI for manually adding this certificate as trusted.
				String message = "Untrusted certificate encountered: " + hash + " issued for " + cert.getSubjectDN();
				throw new CertificateException(message);
			}
		}
	}

	private String getCertificateHash(X509Certificate cert) {
		try {
			return Hex.encodeHexString(MessageDigest.getInstance("SHA-256").digest(cert.getEncoded())).toLowerCase();
		} catch (CertificateEncodingException | NoSuchAlgorithmException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public X509Certificate[] getAcceptedIssuers() {
		return defaultTrustManager.getAcceptedIssuers();
	}

}
