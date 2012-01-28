package apollo.dataadapter.gff3;

import junit.framework.TestCase;
import junit.framework.Test;
import junit.framework.TestSuite;
import junit.extensions.TestSetup;

import java.io.*;

import apollo.dataadapter.ApolloAdapterException;
import apollo.dataadapter.ApolloDataAdapterI;
import apollo.dataadapter.DataInput;
import apollo.config.Config;
import apollo.datamodel.*;

public class GFF3AdapterTest extends TestCase {

	private static String tmpFile;
	private GFF3Adapter adapter;
	
	public GFF3AdapterTest(String methodName)
	{
		super(methodName);
	}
	
	protected void setUp() throws Exception
	{
		adapter = new GFF3Adapter();
		adapter.setGffFilename(tmpFile);
		adapter.setFastaFromGff(true);
	}
	
	public void testGetCurationSet() throws ApolloAdapterException {
		CurationSet curation = adapter.getCurationSet();
		assertEquals("curation.getName()", "test_seq:1-21", curation.getName());
		assertEquals("curation.getRefSequence().getLength()", 21, curation.getRefSequence().getLength());
		assertEquals("curation.getAnnots().size()", 1, curation.getAnnots().size());
		assertEquals("curation.getResults().size()", 2, curation.getResults().size());
	}

	public void testSetFastaFromGff() {
		adapter.setFastaFromGff(false);
		assertFalse("!isFastaFromGff()", adapter.isFastaFromGff());
		adapter.setFastaFromGff(true);
		assertTrue("isFastaFromGff()", adapter.isFastaFromGff());
	}

	public void testIsFastaFromGff() {
		assertTrue("isFastaFromGff()", adapter.isFastaFromGff());
	}

	public void testGetSupportedOperations() {
		assertEquals("getSupportedOperations()[0]", ApolloDataAdapterI.OP_READ_DATA, adapter.getSupportedOperations()[0]);
		assertEquals("getSupportedOperations()[1]", ApolloDataAdapterI.OP_WRITE_DATA, adapter.getSupportedOperations()[1]);
	}

	public void testGetGffFilename() {
		assertEquals("getGffFilename()", tmpFile, adapter.getGffFilename());
	}

	public void testSetGffFilename() {
		adapter.setGffFilename("foo.gff");
		assertEquals("getGffFilename()", "foo.gff", adapter.getGffFilename());
	}

	public void testGetSeqFilename() {
		adapter.setSeqFilename("foo.fasta");
		assertEquals("getGffFilename()", "foo.fasta", adapter.getSeqFilename());
	}

	public void testSetSeqFilename() {
		adapter.setSeqFilename("foo.fasta");
		assertEquals("getGffFilename()", "foo.fasta", adapter.getSeqFilename());
	}

	public void testWrite() throws Exception {
		File in = new File(tmpFile);
		File out = File.createTempFile("GFF3AdapterTest", ".gff");
		CurationSet curation = adapter.getCurationSet();
		adapter.setGffFilename(out.getPath());
		adapter.write(curation);
		assertEquals("output size", in.length(), out.length());
		out.delete();
	}

	public static Test suite() throws Exception
	{
		final File tmp = File.createTempFile("GFF3AdapterTest", ".gff");
		tmpFile = tmp.getPath();
		TestSetup setup = new TestSetup(new TestSuite(GFF3AdapterTest.class)) {
			protected void setUp() throws Exception
			{
				PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(tmp)));
				out.println("##gff-version 3");
				out.println("##sequence-region test_seq 1 21");
				out.println("test_seq\t.\tgene\t1\t21\t.\t+\t.\tID=test_gene;Name=test_gene");
				out.println("test_seq\t.\tmRNA\t1\t21\t.\t+\t.\tID=test_mrna;Name=test_mrna;Parent=test_gene");
				out.println("test_seq\t.\texon\t1\t21\t.\t+\t.\tID=test_exon;Name=test_exon;Parent=test_mrna");
				out.println("test_seq\t.\tmatch\t1\t21\t.\t+\t.\tID=test_match_1;Name=test_match_1");
				out.println("test_seq\t.\tmatch_part\t1\t21\t100.0\t+\t.\tID=test_match_1_part;Name=test_match_1_part;Parent=test_match_1;Target=hit_seq_1 1 21 +");
				out.println("test_seq\t.\tmatch\t1\t21\t.\t+\t.\tID=test_match_2;Name=test_match_2");
				out.println("test_seq\t.\tmatch_part\t1\t21\t100.0\t+\t.\tID=test_match_2_part;Name=test_match_2_part;Parent=test_match_2;Target=hit_seq_2 1 21 +");
				out.println("###");
				out.println(">test_seq");
				out.println("ATGAGCATGGACGTAGCGATG");
				out.println(">hit_seq_1");
				out.println("ATGAGCATGGACGTAGCGATG");
				out.println(">hit_seq_2");
				out.println("ATGAGCATGGACGTAGCGATG");
				out.close();
				//initialize configuration
				Config.supressMacJvmPopupMessage();
			}
			protected void tearDown() throws Exception
			{
				tmp.delete();
			}
		};
		return setup;
	}
	
}
