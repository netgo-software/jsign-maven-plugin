/**
 * Copyright 2016 arxes-tolina GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.tolina.maven.plugins;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.util.Collection;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.StringUtils;

import net.jsign.DigestAlgorithm;
import net.jsign.PESigner;
import net.jsign.PVK;
import net.jsign.pe.PEFile;
import net.jsign.timestamp.TimestampingMode;

/**
 * <pre>
 * Signing and timestamp a Windows executable file.
 *
 * Parameters:
 * file &lt;FILE>          The file to sign
 * keystore &lt;FILE>      The keystore file
 * storepass &lt;PASSWORD> The password to open the keystore
 * storetype &lt;TYPE>     The type of the keystore:
 *                      - JKS: Java keystore (.jks files)
 *                      - PKCS12: Standard PKCS#12 keystore (.p12 or .pfx files)
 * alias &lt;NAME>         The alias of the certificate used for signing in the keystore.
 * keypass &lt;PASSWORD>   The password of the private key. When using a keystore,
 *                         this parameter can be omitted if the keystore shares the
 *                         same password.
 * keyfile &lt;FILE>       The file containing the private key. Only PVK files are
 *                         supported.
 * certfile &lt;FILE>      The file containing the PKCS#7 certificate chain
 *                         (.p7b or .spc files).
 * alg &lt;ALGORITHM>      The digest algorithm (SHA-1, SHA-256, SHA-384 or SHA-512)
 * tsaurl &lt;URL>         The URL of the timestamping authority.
 * tsmode &lt;MODE>        The timestamping mode (RFC3161 or Authenticode)
 * name &lt;NAME>          The name of the application
 * url &lt;URL>            The URL of the application
 * </pre>
 * @author Frank Jakop
 * @since 1.0
 */
@Mojo(name = "signexe", requiresProject = true)
public class PESignerMojo extends AbstractMojo {

	/** The file to be signed. */
	@Parameter(required = true, property = "file")
	File file;

	/** The program name embedded in the signature. */
	@Parameter(required = false, property = "name")
	String name;

	/** The program URL embedded in the signature. */
	@Parameter(required = false, property = "url")
	String url;

	/** The digest algorithm to use for the signature. */
	// not supported yet 
	// @Parameter(required = false)
	String algorithm;

	/** The keystore file or URL. Either {@link #keystore} or {@link #certfile} and {@link #keyfile} are required. */
	@Parameter(required = false, property = "keystore")
	String keystore;

	/** The password for the keystore. */
	@Parameter(required = false, property = "storepass")
	String storepass;

	/** The type of the keystore. */
	@Parameter(required = false, defaultValue = "JKS", property = "storetype")
	String storetype;

	/** The alias of the certificate in the keystore. Required if {@link #keystore} is specified. */
	@Parameter(required = false, property = "alias")
	String alias;

	/** The file containing the certificate chain (PKCS#7 format). 
	 * Either {@link #keystore} or {@link #certfile} and {@link #keyfile} are required. */
	@Parameter(required = false, property = "certfile")
	String certfile;

	/** The file containing the private key (PVK format) */
	@Parameter(required = false, property = "keyfile")
	String keyfile;

	/** The password for the key in the store (if different from the keystore password) or in the keyfile. 
	 * Either {@link #keystore} or {@link #certfile} and {@link #keyfile} are required. */
	@Parameter(required = false, property = "keypass")
	String keypass;

	/** The URL of the timestamping authority. */
	@Parameter(required = false, property = "tsaurl")
	String tsaurl;

	/** The protocol used for the timestamping. */
	@Parameter(required = false, defaultValue = "AUTHENTICODE", property = "tsmode")
	TimestampingMode tsmode;

	/** The currently executed project */
	@Parameter(readonly = true, required = true, defaultValue = "${project}")
	MavenProject project;

	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {

		final PrivateKey privateKey;
		final Certificate[] chain;

		// some exciting parameter validation...
		if (keystore == null && keyfile == null && certfile == null) {
			throw new MojoFailureException("Keystore attribute or keyfile and certfile attributes must be set");
		}
		if (keystore != null && (keyfile != null || certfile != null)) {
			throw new MojoFailureException("Keystore attribute can't be mixed with keyfile or certfile");
		}

		if (keystore != null) {
			final String storeName = keystore.toLowerCase();
			if (storeName.endsWith(".p12") || storeName.endsWith(".pfx")) {
				getLog().warn(String.format("The file extension overrides storetype setting '%s'.", storetype));
				storetype = "PKCS12";
			}

			// JKS or PKCS12 keystore 
			final KeyStore ks = loadKeystore(storetype, keystore, storepass);

			if (alias == null) {
				throw new MojoFailureException("Alias attribute must be set");
			}

			try {
				chain = ks.getCertificateChain(alias);
			} catch (final KeyStoreException e) {
				throw new MojoFailureException("", e);
			}
			if (chain == null) {
				throw new MojoFailureException("No certificate found under the alias '" + alias + "' in the keystore " + keystore);
			}

			final char[] password;
			if (keypass != null) {
				password = keypass.toCharArray();
			} else if (storepass != null) {
				password = storepass.toCharArray();
			} else {
				password = new char[0];
			}

			try {
				privateKey = (PrivateKey) ks.getKey(alias, password);
			} catch (final Exception e) {
				throw new MojoFailureException("Failed to retrieve the private key from the keystore", e);
			}

		} else {
			// separate private key and certificate files (PVK/SPC)
			if (keyfile == null) {
				throw new MojoFailureException("Keyfile attribute must be set");
			}

			final File key = new File(keyfile);
			if (!key.exists()) {
				throw new MojoFailureException("The keyfile " + keyfile + " couldn't be found");
			}
			if (certfile == null) {
				throw new MojoFailureException("Certfile attribute must be set");
			}
			final File cert = new File(certfile);
			if (!cert.exists()) {
				throw new MojoFailureException("The certfile " + certfile + " couldn't be found");
			}

			// load the certificate chain
			try {
				chain = loadCertificateChain(cert);
			} catch (final Exception e) {
				throw new MojoFailureException("Failed to load the certificate from " + certfile, e);
			}

			// load the private key
			try {
				privateKey = PVK.parse(key, keypass);
			} catch (final Exception e) {
				throw new MojoFailureException("Failed to load the private key from " + keyfile, e);
			}
		}

		if (algorithm != null && DigestAlgorithm.of(algorithm) == null) {
			throw new MojoFailureException("The digest algorithm " + algorithm + " is not supported");
		}

		if (file == null) {
			throw new MojoFailureException("File attribute must be set");
		}
		if (!file.exists()) {
			throw new MojoFailureException("The file " + file + " couldn't be found");
		}

		final PEFile peFile;
		try {
			peFile = new PEFile(file);
		} catch (final IOException e) {
			throw new MojoFailureException("Couldn't open the executable file " + file, e);
		}

		// and now the actual work!
		final PESigner signer = new PESigner(chain, privateKey) //
				.withProgramName(name) //
				.withProgramURL(url) // 
				.withDigestAlgorithm(DigestAlgorithm.of(algorithm)) //
				.withTimestamping(tsaurl != null) //
				.withTimestampingMode(tsmode) //
				.withTimestampingAutority(tsaurl);

		try {
			String filePath = file.getCanonicalPath();
			final String projectPath = project.getBasedir().getCanonicalPath();
			if (filePath.startsWith(projectPath)) {
				filePath = filePath.substring(projectPath.length());
			}
			filePath = StringUtils.stripStart(filePath, String.valueOf(File.separatorChar));
			getLog().info(String.format("Adding signature to %s", filePath));
			signer.sign(peFile);
		} catch (final Exception e) {
			throw new MojoFailureException("Couldn't sign " + file, e);
		} finally {
			try {
				peFile.close();
			} catch (final IOException e) {
				getLog().warn("Couldn't close " + file, e);
			}
		}
	}

	private KeyStore loadKeystore(final String type, final String store, final String pass) throws MojoFailureException {
		final KeyStore ks;
		try {
			ks = KeyStore.getInstance(type);
		} catch (final KeyStoreException e) {
			throw new MojoFailureException("Keystore type '" + type + "' is not supported", e);
		}

		InputStream in = null;
		try {
			if (new File(store).exists()) {
				// keystore is a file
				in = new FileInputStream(store);
			} else {
				// keystore is URL
				in = new URL(store).openStream();
			}
			ks.load(in, pass != null ? pass.toCharArray() : null);

		} catch (final Exception e) {
			throw new MojoFailureException(String.format("Unable to load the keystore '%s'", store), e);
		} finally {
			try {
				if (in != null) {
					in.close();
				}
			} catch (final IOException e) {
				// ignore
			}
		}
		return ks;
	}

	/**
	 * Load the certificate chain from the specified PKCS#7 files.
	 */
	@SuppressWarnings("unchecked")
	private Certificate[] loadCertificateChain(final File value) throws IOException, CertificateException {
		FileInputStream in = null;
		try {
			in = new FileInputStream(value);
			final CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
			final Collection<Certificate> certificates = (Collection<Certificate>) certificateFactory.generateCertificates(in);
			return certificates.toArray(new Certificate[certificates.size()]);
		} finally {
			if (in != null) {
				in.close();
			}
		}
	}
}
