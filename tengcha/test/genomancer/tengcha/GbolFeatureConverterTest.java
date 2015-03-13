/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package genomancer.tengcha;


import genomancer.trellis.das2.model.Das2FeatureI;
import genomancer.trellis.das2.model.Das2LocationI;
import genomancer.trellis.das2.model.Strand;
import genomancer.vine.das2.client.modelimpl.Das2Feature;
import genomancer.vine.das2.client.modelimpl.Das2Segment;
import genomancer.vine.das2.client.modelimpl.Das2Type;
import java.net.URI;
import java.net.URISyntaxException;
import java.sql.Timestamp;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.gmod.gbol.bioObject.*;
import org.gmod.gbol.bioObject.conf.BioObjectConfiguration;
import org.gmod.gbol.simpleObject.Feature;
import org.gmod.gbol.simpleObject.FeatureLocation;
import org.gmod.gbol.simpleObject.Organism;
import org.gmod.gbol.simpleObject.SimpleObjectIteratorInterface;
import org.junit.*;
import static org.junit.Assert.*;

/**
 *
 * @author gregg
 */
public class GbolFeatureConverterTest {
    private Organism organism;
    private BioObjectConfiguration conf;
    private URI xml_base;

	    
    public GbolFeatureConverterTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }
    
    @Before
    public void setUp() {
	organism = new Organism("Foomus", "barius");
	System.out.println("Current working directory: " + System.getProperty("user.dir"));
	conf = new BioObjectConfiguration("testSupport/gbolThree.mapping.xml");
        try {
            xml_base = new URI("http://localhost/test_base_uri");
        } catch (URISyntaxException ex) {
            Logger.getLogger(GbolFeatureConverterTest.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    @After
    public void tearDown() {
    }


    /**
     * Test of convertGbolFeature method, of class GbolFeatureConverter.
     */
    @Test
    public void testConvertGbolFeature() {
        System.out.println("convertGbolFeature");
        Gene testgene = createGene(1);
        this.printGene(testgene);
        SimpleObjectIteratorInterface iter = testgene.getWriteableSimpleObjects(conf);
        Feature gbolFeat = (Feature)iter.next();
        Feature seqGbolFeat = gbolFeat.getFeatureLocations().iterator().next().getSourceFeature();
        Das2Segment seq = new Das2Segment(xml_base, "testseq", "testseq_title", 
                                          null, //  URI reference, 
                                          100000, 
                                          null // String info_url
                                         );
        Das2Type type = new Das2Type(xml_base, "testtype", "testtype_title", null, null, null, null, true);
        gbolFeat.setFeatureId(12345);
        Das2Feature expResult = null;

	try {
	    Das2Feature result = GbolFeatureConverter.convertGbolFeature(gbolFeat, seqGbolFeat, seq, type);
	    printDas2Feature(result);
	}
	catch (Exception e){	    
	    e.printStackTrace();
	}
    }
    
    public static void printDas2Feature(Das2FeatureI feat)  {
        printDas2Feature(feat, "");
    }
    
    public static void printDas2Feature(Das2FeatureI feat, String indent) {
        List<Das2FeatureI> parents = feat.getParents();
        String parent_name = null;
        if (parents != null && parents.size() > 0) {
            parent_name = parents.get(0).getTitle();
        }
        System.out.println(indent + "type: " + feat.getType().getLocalURIString() + 
                           " id: " + feat.getLocalURIString() + 
                           ", title: " + feat.getTitle() + 
                           ", parent: " + parent_name);
        for (Das2LocationI loc : feat.getLocations())  {
            System.out.println(indent + "  [" + loc.getSegment().getLocalURIString() + "," + loc.getMin() + "-"+ loc.getMax() + ":" + loc.getStrand() + "]");
        }
        indent += "    ";
        if (feat.getParts() != null)  {
            for (Das2FeatureI cfeat : feat.getParts())  {
                printDas2Feature(cfeat, indent);
            }
        }
    }
    /**
     * Test of convertGbolStrand method, of class GbolFeatureConverter.
     */
    @Test
    public void testConvertGbolStrand() {
        System.out.println("convertGbolStrand");

        Strand plus = GbolFeatureConverter.convertGbolStrand(1);
        Strand minus = GbolFeatureConverter.convertGbolStrand(-1);
        Strand unknown = GbolFeatureConverter.convertGbolStrand(0);
        
        assertEquals(Strand.FORWARD, plus);
        assertEquals(Strand.REVERSE, minus);
        assertEquals(Strand.UNKNOWN, unknown);
    }

    /**
     * Test of convertGbolLocation method, of class GbolFeatureConverter.
     */
/**    @Test
   public void testConvertGbolLocation() {
        System.out.println("convertGbolLocation");
        FeatureLocation loc = null;
        Das2Feature expResult = null;
        Das2Feature result = GbolFeatureConverter.convertGbolLocation(loc);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }
*/

    private Chromosome createChromosome(int strand) {
		Chromosome chromosome = new Chromosome(organism, "chromosome", false, false, new Timestamp(0), conf);
		if (strand == -1) {
			chromosome.setResidues("ATTAACAAAAACTCAAATATTCACATTTCATGAAACAAAAATTACACCAGGTGTTTCTGCAAGAAACCAAATCTCCATTTACATGATAGATTACTACGCTTCCTTCTCTAGTGGTGCAGCTTCTCCTTGATCTTAGTTATGATTCCCTTCTTGTGAGGTGCAGCACCAGTATCGGTGGCACCATAACCAGATCCGGTCTCGGTCGTATCGTAGTCTGATCCAAGTCCGGTTTTGTTATCAATCGGGTCTTCAAATCCTTTTCCCCTCCTCTCATCGTAATCAGTGTCGTGATGGCCCAATCCAGTGACACCGCTATCGTTACCGTGATGGCCAAGATCAGTAACTGCATCGACACCACATTACCATCAATCAAGTTAACACGAAGGAATGAGCACCACAACAGCGGAATCACCGATTTTCCAAAACCATGAAGCACCACCAATTTAGCAGANTTCAACAAGCTGCCTAAACTTCGAAATAATTTGCAAGAAGATATAGCATGGTAGACTCACGATGATGTCCAGCACTCTTGGTCATATCATATCCAGGTTCAACACGGCGCCCGCCTACCATAGTCTCAGGAATATGGCCACTTTGAAGAGTAACATTATCTAGCAGACCGTGATGTTGGCGACCATCGTGTCCAAAACCATGGCGGTCTACATTCTCACGTCCACCTAAGATCGAGGGCTCGTTCACGCCAGTGATTCTGTCTTGCATACCAGGGGGATGGTTGCCATGCACGTAAGCGTCGGTTCCGGTGGGTGTGCGGCTGTCATATCCGGAGTTTGGGCCAGTACCGATTGCGTCCCTCACCTTGTCACCAATTCCCTCGTGTTTTCGGTCCCCATAGGTGTCATAGCCGGGCCGGTTGTTATAGCCAGTGCCGGAATTGGGGCCCATATCAAAGTCATCCTTCGCCTTGTCAAGCATGCCTTCGTGCCTTGGGTTGCCATAACTGTCATAATCAGACTGGTTGTTGTAGCCAGTTCCAGAGTTTGGTCCCATCCCCACAGCGTCTTTCGCTCTATCCACGATTCCCTCTTGCCTAGGGTTGCCATAGTTGTCGTAGCTGGGCTGGTTGTGGTAGCCAGTACCCGAGTGGGGGCCAACACCGACTGCATCCTTGGCTTTGTCCACAATGCCCTCACGATTGCCGTAGTTGTCATATCCGGGCTGATTGTTGTAACCTGAGTTAGGCCCCATACCTACAGCATCCTTCGCCCTGTCTACCACTCCTTCGCGTCTTGGGTTACCGTAGTTGTCATATCCGGGCTGATTGTTATAGCCGGTGCCCGAATTGGGGCCCATGCCGACGGCATCCTTTGCTCTATCTACCAATCCTTCCTGTCTCCGGGTACCATAACTGTCATAACCAGGCTGGTTGTTGTAGCCAGTTCCTGAATTCGGACCCATGCCTACCGCATCCTTCGCCTTGTCCACAACGCCCTCACGATTGCCGTAGTTGTCATATCCAGGCTGGTGGTTGTAACCTGAGTTAGGCCCCATACCTACAGCATCCTTCGCTCTGTCTGCCAATCCTTCATGCCTTCGGTCACCGTAATTGTCGTACCCAGGCTGGTTGTTGTAGCCAGTTCCTGAATTCGGACCCATCCCTACCGCATCTTTCGCCCTGTCCACAATGCCCTCACGATTCCCGTAACTGTCATAACCAGGCTGGTTATTGTAGCCAGTTCCTAAACTCGGACCCATGCCCACGGCGTCCTTCGCCTTGTCTACTAATCCTTCTTGCCTTGGGTTACCGTAATTGTCATAACCAGGCTGATTGTTGTAGCCGGTTCCTGAACTGGGGCCCATGCCTACGGCATTTTTCACCTTGTCCATTATACCCTCTTGCCTGGGATTGCCGTATTCATCGCGATGTCCTACACCATCAGAAGATGGATCGTTAGCGAGAACAAATGAGCTACACCAAAGATTGCGAGGGGTTCCGTGCCGAAAATAAAAAAGACCTTTGAGGATCGTTCATCATCAATCCACGGAAAACAGTGTTTAAAATCGCAAAAAAAACATTTCATTACCTGTACCAGAGCCGACGAGGCCAGTATCTTGCTGTTCTCTTCCGTACTGATTCATGATTGTCGATGATTTCTATATACTCTGCAAAAGAAATGGAAGCATTGTTGTTAAGAAATCGCTACATTAGACCACTGTATGCTTCAAATATCCTCGATCCCAAACATGCATGGCAAACAAAGACTCTAATGGAATCTTTAATGGTAGAAAGATCATCACAATTATTTGACGATCGAAAAGTTCTTTAGCAGGAGAAATAGTCTTCGCAATGCTCATTGATTCCAAAAGCAGATCTAACCACAAACAAACATACAGAAGAACTCCCTCTACGGAGAAACATCGGAAACCCAGAGCCCCATCGACAGATCGACCCTACAGAACAAGTCAAATGTGTCCCAACCAGCCTACATTACGCACCAGAGGCTCTCTACATCCACACAACGAATCAGACAGATCGCAAACTAGGTACGAAACTGTGAACCAAGCAACATTGACCTAGCATTTCACAAACTCGCATCGACACGAACATCCTACAGTGGCCGAGAGAGACACACTCACACAGCATGCGACATAATCGCGCCTAACGCCAAAAGGCACGATCGATGAACACTCTCTCCGACACACCGAAAAGTCTCGCGATTCTTCTTCCACGTCTAAAGTGCATACAAGTCCTCTGCAACGATTGTGAGAAAAGATAT");
		}
		else {
			chromosome.setResidues("ATATCTTTTCTCACAATCGTTGCAGAGGACTTGTATGCACTTTAGACGTGGAAGAAGAATCGCGAGACTTTTCGGTGTGTCGGAGAGAGTGTTCATCGATCGTGCCTTTTGGCGTTAGGCGCGATTATGTCGCATGCTGTGTGAGTGTGTCTCTCTCGGCCACTGTAGGATGTTCGTGTCGATGCGAGTTTGTGAAATGCTAGGTCAATGTTGCTTGGTTCACAGTTTCGTACCTAGTTTGCGATCTGTCTGATTCGTTGTGTGGATGTAGAGAGCCTCTGGTGCGTAATGTAGGCTGGTTGGGACACATTTGACTTGTTCTGTAGGGTCGATCTGTCGATGGGGCTCTGGGTTTCCGATGTTTCTCCGTAGAGGGAGTTCTTCTGTATGTTTGTTTGTGGTTAGATCTGCTTTTGGAATCAATGAGCATTGCGAAGACTATTTCTCCTGCTAAAGAACTTTTCGATCGTCAAATAATTGTGATGATCTTTCTACCATTAAAGATTCCATTAGAGTCTTTGTTTGCCATGCATGTTTGGGATCGAGGATATTTGAAGCATACAGTGGTCTAATGTAGCGATTTCTTAACAACAATGCTTCCATTTCTTTTGCAGAGTATATAGAAATCATCGACAATCATGAATCAGTACGGAAGAGAACAGCAAGATACTGGCCTCGTCGGCTCTGGTACAGGTAATGAAATGTTTTTTTTGCGATTTTAAACACTGTTTTCCGTGGATTGATGATGAACGATCCTCAAAGGTCTTTTTTATTTTCGGCACGGAACCCCTCGCAATCTTTGGTGTAGCTCATTTGTTCTCGCTAACGATCCATCTTCTGATGGTGTAGGACATCGCGATGAATACGGCAATCCCAGGCAAGAGGGTATAATGGACAAGGTGAAAAATGCCGTAGGCATGGGCCCCAGTTCAGGAACCGGCTACAACAATCAGCCTGGTTATGACAATTACGGTAACCCAAGGCAAGAAGGATTAGTAGACAAGGCGAAGGACGCCGTGGGCATGGGTCCGAGTTTAGGAACTGGCTACAATAACCAGCCTGGTTATGACAGTTACGGGAATCGTGAGGGCATTGTGGACAGGGCGAAAGATGCGGTAGGGATGGGTCCGAATTCAGGAACTGGCTACAACAACCAGCCTGGGTACGACAATTACGGTGACCGAAGGCATGAAGGATTGGCAGACAGAGCGAAGGATGCTGTAGGTATGGGGCCTAACTCAGGTTACAACCACCAGCCTGGATATGACAACTACGGCAATCGTGAGGGCGTTGTGGACAAGGCGAAGGATGCGGTAGGCATGGGTCCGAATTCAGGAACTGGCTACAACAACCAGCCTGGTTATGACAGTTATGGTACCCGGAGACAGGAAGGATTGGTAGATAGAGCAAAGGATGCCGTCGGCATGGGCCCCAATTCGGGCACCGGCTATAACAATCAGCCCGGATATGACAACTACGGTAACCCAAGACGCGAAGGAGTGGTAGACAGGGCGAAGGATGCTGTAGGTATGGGGCCTAACTCAGGTTACAACAATCAGCCCGGATATGACAACTACGGCAATCGTGAGGGCATTGTGGACAAAGCCAAGGATGCAGTCGGTGTTGGCCCCCACTCGGGTACTGGCTACCACAACCAGCCCAGCTACGACAACTATGGCAACCCTAGGCAAGAGGGAATCGTGGATAGAGCGAAAGACGCTGTGGGGATGGGACCAAACTCTGGAACTGGCTACAACAACCAGTCTGATTATGACAGTTATGGCAACCCAAGGCACGAAGGCATGCTTGACAAGGCGAAGGATGACTTTGATATGGGCCCCAATTCCGGCACTGGCTATAACAACCGGCCCGGCTATGACACCTATGGGGACCGAAAACACGAGGGAATTGGTGACAAGGTGAGGGACGCAATCGGTACTGGCCCAAACTCCGGATATGACAGCCGCACACCCACCGGAACCGACGCTTACGTGCATGGCAACCATCCCCCTGGTATGCAAGACAGAATCACTGGCGTGAACGAGCCCTCGATCTTAGGTGGACGTGAGAATGTAGACCGCCATGGTTTTGGACACGATGGTCGCCAACATCACGGTCTGCTAGATAATGTTACTCTTCAAAGTGGCCATATTCCTGAGACTATGGTAGGCGGGCGCCGTGTTGAACCTGGATATGATATGACCAAGAGTGCTGGACATCATCGTGAGTCTACCATGCTATATCTTCTTGCAAATTATTTCGAAGTTTAGGCAGCTTGTTGAANTCTGCTAAATTGGTGGTGCTTCATGGTTTTGGAAAATCGGTGATTCCGCTGTTGTGGTGCTCATTCCTTCGTGTTAACTTGATTGATGGTAATGTGGTGTCGATGCAGTTACTGATCTTGGCCATCACGGTAACGATAGCGGTGTCACTGGATTGGGCCATCACGACACTGATTACGATGAGAGGAGGGGAAAAGGATTTGAAGACCCGATTGATAACAAAACCGGACTTGGATCAGACTACGATACGACCGAGACCGGATCTGGTTATGGTGCCACCGATACTGGTGCTGCACCTCACAAGAAGGGAATCATAACTAAGATCAAGGAGAAGCTGCACCACTAGAGAAGGAAGCGTAGTAATCTATCATGTAAATGGAGATTTGGTTTCTTGCAGAAACACCTGGTGTAATTTTTGTTTCATGAAATGTGAATATTTGAGTTTTTGTTAAT");
		}
		return chromosome;
	}
	
	private Gene createGene(int strand) {
		Chromosome chromosome = createChromosome(strand);
		chromosome.setFeatureLocation(0, 2735, 1, null);
		Gene gene = new Gene(organism, "gene", false, false, new Timestamp(0), conf);
		gene.setFeatureLocation(0, 2735, strand, chromosome);
		Transcript transcript = createTranscript(638, 2628, strand, "transcript", chromosome);
		gene.addTranscript(transcript);
		transcript.addExon(createExon(638, 693, strand, "exon1", chromosome));
		transcript.addExon(createExon(849, 2223, strand, "exon2", chromosome));
		transcript.addExon(createExon(2392, 2628, strand, "exon3", chromosome));
		CDS cds = createCDS(638, 2628, strand, "cds", chromosome);
		transcript.setCDS(cds);
		return gene;
	}

    private Das2Feature createDasGene()  {
	return null;
    }

	private Transcript createTranscript(int fmin, int fmax, int strand, String name, Chromosome src) {
		Transcript transcript = new Transcript(organism, name, false, false, new Timestamp(0), conf);
		if (strand == -1) {
			int tmp = fmax;
			fmax = src.getLength() - fmin;
			fmin = src.getLength() - tmp;
		}
		transcript.setFeatureLocation(fmin, fmax, strand, src);
		return transcript;
	}
	
	private Exon createExon(int fmin, int fmax, int strand, String name, Chromosome src) {
		Exon exon = new Exon(organism, name, false, false, new Timestamp(0), conf);
		if (strand == -1) {
			int tmp = fmax;
			fmax = src.getLength() - fmin;
			fmin = src.getLength() - tmp;
		}
		exon.setFeatureLocation(fmin, fmax, strand, src);
		return exon;
	}
	
	private CDS createCDS(int fmin, int fmax, int strand, String name, Chromosome src) {
		CDS cds = new CDS(organism, name, false, false, new Timestamp(0), conf);
		if (strand == -1) {
			int tmp = fmax;
			fmax = src.getLength() - fmin;
			fmin = src.getLength() - tmp;
		}
		cds.setFeatureLocation(fmin, fmax, strand, src);
		return cds;
	}
	
	private void printGene(Gene gene) {
		printFeatureInfo(gene, 0);
		for (Transcript transcript : gene.getTranscripts()) {
			printFeatureInfo(transcript, 1);
			if (transcript.getCDS() != null) {
				printFeatureInfo(transcript.getCDS(), 1);
			}
			for (Exon exon : transcript.getExons()) {
				printFeatureInfo(exon, 2);
			}
		}
	}
	
	private void printFeatureInfo(AbstractSingleLocationBioFeature feature, int indent)
	{
		for (int i = 0; i < indent; ++i) {
			System.out.print("\t");
		}
		System.out.printf("%s\t(%d,%d,%d)%n", feature.getUniqueName(), feature.getFeatureLocation().getFmin(),
				feature.getFeatureLocation().getFmax(), feature.getFeatureLocation().getStrand());
	}


}
