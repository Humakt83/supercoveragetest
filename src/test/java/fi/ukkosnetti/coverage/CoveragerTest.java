package fi.ukkosnetti.coverage;

import java.io.IOException;

import org.junit.Test;

import fi.ukkosnetti.coverage.Coverager;

public class CoveragerTest {

	@Test
	public void testCoverager() throws IOException {
		new Coverager().coverage();
	}
	
}
