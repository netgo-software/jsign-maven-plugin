/*
 *  (c) tolina GmbH, 2016
 */
package de.tolina.maven.plugins;

import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

@SuppressWarnings("javadoc")
public class PESignerMojoTest {

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	@Mock
	private MavenProject project;

	@Mock
	private Log log;


	private PESignerMojo mojo;

	@Before
	public void before() {
		MockitoAnnotations.initMocks(this);
		mojo = new PESignerMojo();
		mojo.setLog(log);
	}


	@Test
	public void testMissingKeyStore() throws Exception {
		thrown.expect(MojoFailureException.class);
		thrown.expectMessage("Keystore attribute or keyfile and certfile attributes must be set");
		mojo.execute();
	}

	@Test
	public void testUnsupportedKeyStoreType() throws Exception {
		mojo.keystore = "keystore.jks";
		mojo.storetype = "ABC";
		thrown.expect(MojoFailureException.class);
		thrown.expectMessage("Keystore type 'ABC' is not supported");
		mojo.execute();
	}

	@Test
	public void testKeyStoreNotFound() throws Exception {
		mojo.keystore = "keystore.jks";
		mojo.storetype = "JKS";
		thrown.expect(MojoFailureException.class);
		thrown.expectMessage("Unable to load the keystore 'keystore.jks'");
		mojo.execute();
	}

	public void testMissingAlias() {
		// tbd
	}

	public void testAliasNotFound() {
		// tbd
	}

	public void testCertificateNotFound() {
		// tbd
	}

	public void testMissingFile() {
		// tbd
	}

	public void testFileNotFound() {
		// tbd
	}

	public void testCorruptedFile() {
		// tbd
	}

	public void testConflictingAttributes() {
		// tbd
	}

	public void testMissingCertFile() {
		// tbd
	}

	public void testMissingKeyFile() {
		// tbd
	}

	public void testCertFileNotFound() {
		// tbd
	}

	public void testKeyFileNotFound() {
		// tbd
	}

	public void testCorruptedCertFile() {
		// tbd
	}

	public void testCorruptedKeyFile() {
		// tbd
	}

	public void testUnsupportedDigestAlgorithm() {
		// tbd
	}

	public void testSigning() throws Exception {
		// tbd
	}

	public void testSigningPKCS12() throws Exception {
		// tbd
	}

	public void testSigningPVKSPC() throws Exception {
		// tbd
	}

	public void testTimestampingAuthenticode() throws Exception {
		// tbd
	}

	public void testTimestampingRFC3161() throws Exception {
		// tbd
	}
}
