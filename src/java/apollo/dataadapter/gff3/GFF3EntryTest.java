package apollo.dataadapter.gff3;

import apollo.datamodel.SeqFeatureI;
import apollo.datamodel.AnnotatedFeature;
import apollo.datamodel.Transcript;

import apollo.util.OBOParser;
import apollo.util.IOUtil;

import junit.framework.TestCase;

import java.io.StringReader;

import java.util.List;
import java.util.Map;
import java.util.Arrays;

public class GFF3EntryTest extends TestCase {

	//instance variables
	private String data1;
	private GFF3Entry entry1;
	
	public GFF3EntryTest(String testMethod)
	{
		super(testMethod);
	}
	
	public void setUp()
	{
		data1 = "refId1\tsrc1\tgene\t1\t100\t.\t+\t.\tID=id1;Name=gene_feat;Parent=parent_id";
		entry1 = new GFF3Entry(data1);
	}

	public void testGetReferenceId()
	{
		assertEquals("reference id", "refId1", entry1.getReferenceId());
	}
	
	public void testGetSource()
	{
		assertEquals("source", "src1", entry1.getSource());
	}

	public void testGetType()
	{
		assertEquals("type", "gene", entry1.getType());
	}

	public void testGetStart()
	{
		assertEquals("start", 1, entry1.getStart());
	}

	public void testSetStart()
	{
		entry1.setStart(10);
		assertEquals("start", 10, entry1.getStart());
	}

	public void testGetEnd()
	{
		assertEquals("end", 100, entry1.getEnd());
	}

	public void testSetEnd()
	{
		entry1.setEnd(1000);
		assertEquals("end", 1000, entry1.getEnd());
	}

	public void testGetStrand()
	{
		String data2 = "refId2\tsrc2\ttranscript\t2\t200\t.\t-\t.\tID=id2;Name=transcript_feat";
		String data3 = "refId3\tsrc3\tprotein\t3\t300\t.\t.\t1\tID=id3;Name=cds_feat";
		GFF3Entry entry2 = new GFF3Entry(data2);
		GFF3Entry entry3 = new GFF3Entry(data3);
		assertEquals("plus strand", 1, entry1.getStrand());
		assertEquals("minus strand", -1, entry2.getStrand());
		assertEquals("no strand", 0, entry3.getStrand());
	}
	
	public void testGetScore()
	{
		String data2 = "refId2\tsrc2\tmatch\t2\t200\t2000\t+\t.\tID=id2;Name=match_feat";
		GFF3Entry entry2 = new GFF3Entry(data2);
		assertEquals("no score", 0.0, entry1.getScore());
		assertEquals("score", 2000.0, entry2.getScore());
	}

	public void testSetScore()
	{
		entry1.setScore(1000);
		assertEquals("score", 1000.0, entry1.getScore());
	}
	
	public void testGetPhase()
	{
		String data2 = "refId2\tsrc2\tCDS\t2\t200\t.\t+\t1\tID=id2;Name=cds_feat";
		GFF3Entry entry2 = new GFF3Entry(data2);
		assertEquals("phase", 1, entry2.getPhase());
		assertEquals("no phase", 0, entry1.getPhase());
	}
	
	public void testSetPhase()
	{
		entry1.setPhase(3);
		assertEquals("phase", 3, entry1.getPhase());
	}

	public void testIsScoreSet()
	{
		String data2 = "refId2\tsrc2\tmatch\t2\t200\t2000\t+\t.\tID=id2;Name=match_feat";
		GFF3Entry entry2 = new GFF3Entry(data2);
		assertFalse("score not set", entry1.isScoreSet());
		assertTrue("score set", entry2.isScoreSet());
	}
	
	public void testIsPhaseSet()
	{
		String data2 = "refId2\tsrc2\tCDS\t2\t200\t.\t+\t1\tID=id2;Name=cds_feat";
		GFF3Entry entry2 = new GFF3Entry(data2);
		assertFalse("phase not set", entry1.isPhaseSet());
		assertTrue("phase set", entry2.isPhaseSet());
	}

	public void testGetAttributeValues()
	{
		List<String> parents = entry1.getAttributeValues("Parent");
		assertEquals("number of parents", 1, parents.size());
		assertEquals("parent", "parent_id", parents.get(0));
	}

	public void testAddAttributeValue()
	{
		entry1.addAttributeValue("Alias", "alias1");
		entry1.addAttributeValue("Alias", "alias2");
		List<String> aliases = entry1.getAttributeValues("Alias");
		assertEquals("number of aliases", 2, aliases.size());
		assertEquals("first alias", "alias1", aliases.get(0));
		assertEquals("second alias", "alias2", aliases.get(1));		
	}

	public void testGetAttributes()
	{
		Map<String, List<String> > attrs = entry1.getAttributes();
		int numMatches = 0;
		for (String key : attrs.keySet()) {
			if (key.equals("ID") || key.equals("Name") || key.equals("Parent")) {
				++numMatches;
			}
		}
		assertEquals("number of attribute types", attrs.size(), numMatches);
	}

	public void testGetId()
	{
		assertEquals("id", "id1", entry1.getId());
	}
	
	public void testSetId()
	{
		entry1.setId("new_id");
		assertEquals("id", "new_id", entry1.getId());
	}
	
	public void testGetName()
	{
		assertEquals("name", "gene_feat", entry1.getName());
	}

	public void testCreateFeature() throws Exception
	{
		String data2 = "refId2\tsrc2\tmRNA\t2\t200\t.\t+\t1\tID=id2;Name=mrna_feat";
		GFF3Entry entry2 = new GFF3Entry(data2);
		String []oboFiles = { IOUtil.findFile("so.obo"), IOUtil.findFile("apollo.obo") };
		OBOParser oboParser = new OBOParser(Arrays.asList(oboFiles));
		SeqFeatureI feat1 = entry1.createFeature(oboParser);
		SeqFeatureI feat2 = entry2.createFeature(oboParser);
		assertEquals("id", "id1", feat1.getId());
		assertEquals("name", "gene_feat", feat1.getName());
		assertEquals("featureType", "gene", feat1.getFeatureType());
		assertTrue("instanceof AnnotatedFeature", feat1 instanceof AnnotatedFeature);
		assertTrue("instanceof Transcript", feat2 instanceof Transcript);
	}
	
	public void testToString()
	{
		assertEquals("toString", data1, entry1.toString());
	}
	
	public void testIsOneLevelAnnotation()
	{
		String data2 = "refId2\tsrc2\tpromoter\t2\t200\t.\t+\t1\tID=id2;Name=promoter_feat";
		GFF3Entry entry2 = new GFF3Entry(data2);
		assertFalse("not one-level annotation", entry1.isOneLevelAnnotation());
		assertTrue("one-level annotation", entry2.isOneLevelAnnotation());
	}
	
	public void testIsThreeLevelAnnotation()
	{
		String data2 = "refId2\tsrc2\tpromoter\t2\t200\t.\t+\t1\tID=id2;Name=promoter_feat";
		GFF3Entry entry2 = new GFF3Entry(data2);
		assertFalse("not three-level annotation", entry2.isThreeLevelAnnotation());
		assertTrue("three-level annotation", entry1.isThreeLevelAnnotation());		
	}

	public void testIsAnnotation()
	{
		String data2 = "refId2\tsrc2\tmatch\t2\t200\t.\t+\t1\tID=id2;Name=match_feat";
		GFF3Entry entry2 = new GFF3Entry(data2);
		assertTrue("isAnnotation", entry1.isAnnotation());
		assertFalse("!isAnnotation", entry2.isAnnotation());
	}

}
