package genomancer.trellis.das2.server;

import genomancer.trellis.das2.model.Das2FeatureI;
import genomancer.trellis.das2.model.Das2FeatureWriterI;
import genomancer.trellis.das2.model.Das2FeaturesQueryI;
import genomancer.trellis.das2.model.Das2FeaturesResponseI;
import genomancer.trellis.das2.model.Das2FormatI;
import genomancer.trellis.das2.model.Das2LocationI;
import genomancer.trellis.das2.model.Das2SegmentI;
import genomancer.trellis.das2.model.Das2TypeI;
import genomancer.trellis.das2.model.Strand;
import genomancer.vine.das2.client.modelimpl.Das2Format;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;


public class JBrowseFlatFeatureWriter implements Das2FeatureWriterI {
    static Das2Format format = new Das2Format("jbrowse-flat-json", "application/json");

    static String FORWARD_STRAND = "1";
    static String REVERSE_STRAND = "-1";

    /** for now assuming any type can be represented as JBrowse JSON
     *  NEED TO FIX at some point, 
     *  since this assumption does not hold for annotation trees with depth > 2
     *     and many other situations
     */
    public boolean acceptsType(Das2TypeI type) { 
	return true;
    }

    public Das2FormatI getFormat()  { return format; }

    public boolean writeFeatures(Das2FeaturesResponseI feature_response, OutputStream ostr) {
	List<Das2FeatureI> annots = feature_response.getFeatures();
	Writer bw = new BufferedWriter(new OutputStreamWriter(ostr));
	// extract query seq(s) from feature_response?
	//	Das2FeaturesQueryI query = feature_response.getQuery();
	try {
          bw.write('[');
	    for (Das2FeatureI annot : annots)  {
		writeFeature(bw, annot);
	    }
          bw.write(']');
          bw.flush(); // ???
        } catch (IOException ex) {
            Logger.getLogger(JBrowseFlatFeatureWriter.class.getName()).log(Level.SEVERE, null, ex);
        }
	return true;
    }

    /**
     *   For now assume first location in locations list is correct one
     *   NEED TO FIX, this assumption is not likely to hold
     *
     *  [  [ min, max, strand, id, null ], ... ]
     *
     *  null at index 4 is where child index would go in JBrowse pre-computed JSON,
     *     but can't do it that way on the fly...
     *
     */
    public boolean writeFeature(Writer out, Das2FeatureI annot) throws IOException  {
	List<Das2LocationI> locs = annot.getLocations();
	if (locs == null || locs.size() <= 0)  { 
	    // no locations for annot, not writing out
	    return false;
	}
	Das2LocationI span = locs.get(0);
	Das2SegmentI seq = span.getSegment();
        int min = span.getMin();
        int max = span.getMax();
        out.write('[');
        out.write(Integer.toString(min));
        out.write(',');
        out.write(Integer.toString(max));
        out.write(',');
        if (min <= max)  {
            out.write(FORWARD_STRAND);
        }
        else {
            out.write(REVERSE_STRAND);
        }
        out.write(',');
        String annotid = annot.getLocalURIString();
        if (annotid == null)  {
            out.write("null");
        }
        else  {
            out.write('"');
            out.write(annotid);
            out.write('"');
        }
        out.write(',');
        out.write("null");
        out.write(']');
        out.write(',');
        // out.flush(); ???
        return true;
    }
//        out.write("
	// should add a Das2FeatureI.getPartsCount() method!
/*  id, strand, children, etc. bits from BED writer
 List<Das2FeatureI> parts = annot.getParts();
        int childcount;
	if (parts == null)  { childcount = 0; }
	else { childcount = parts.size(); }

	Strand orient = span.getStrand();

	if ( (orient != Strand.FORWARD) || 
	     (childcount > 0) || 
	     (annot.getLocalURIString() != null) ) {

	    out.write('\t');
	    out.write(annot.getLocalURIString());  // name
	    out.write('\t');
	    //          if (annot instanceof ScoredDas2FeatureI) {
	    //	      out.write(Float.toString(((ScoredDas2FeatureI) annot).getScore(0)));
	    //          }
	    //          else { 
	    out.write('0');   // score
	    //	  }
	    out.write('\t');
	    if (orient == Strand.REVERSE)  {
		out.write('-'); 
	    }
	    else { 
		out.write('+'); 
	    }
	    if (childcount > 0) {
		out.write('\t');
		// if has a CDS child, use it, otherwise fill whole span
		// for now assuming no CDS, just fill whole span
		out.write(Integer.toString(min));  // thickmin
		out.write('\t');
		out.write(Integer.toString(max));  // thickmax
		out.write('\t');
		out.write('0');  // itemRGB
		out.write('\t');
		out.write(Integer.toString(childcount));  // blockCount
		out.write('\t');
		int[] blockSizes = new int[childcount];
		int[] blockStarts = new int[childcount];
		// assumes all children of a located Das2FeatureI also have locations
		for (int i=0; i<childcount; i++)  {
		    Das2FeatureI child = parts.get(i);
		    List<Das2LocationI> child_locs = child.getLocations();
		    // 
		    Das2LocationI child_span = child_locs.get(0);
		    //blockSizes[i] = child_span.getLength();
		    blockSizes[i] = child_span.getMax() - child_span.getMin();
		    blockStarts[i] = child_span.getMin() - min;
		}
		for (int i=0; i<childcount; i++) {
		    out.write(Integer.toString(blockSizes[i]));
		    out.write(',');
		}
		out.write('\t');
		for (int i=0; i<childcount; i++) {
		    out.write(Integer.toString(blockStarts[i]));
		    out.write(',');
		}
	    } // END "if (childcount > 0)"
        }  // END if (orient != Strand.FORWARD) || (childcount > 0) || ...

        out.write('\n');
//        out.flush(); ???
        return true;
    }
*/

  /**
   *  Writes bed file format.
   *  WARNING. This currently assumes that each child symmetry contains
   *     a span on the seq given as an argument.
   */
    /*
 public static void writeBedFormat(Writer out, SeqSymmetry sym, BioSeq seq)
    throws IOException {
    if ((sym instanceof UcscBedSym) && (sym.getSpan(seq) != null)) {
      UcscBedSym bedsym = (UcscBedSym)sym;
      if (seq == bedsym.getBioSeq()) {
        bedsym.outputBedFormat(out);
      }
    }
    else {
      if (DEBUG) {System.out.println("writing sym: " + sym);}
      SeqSpan span = sym.getSpan(seq);
      SymWithProps propsym = null;
      if (sym instanceof SymWithProps) {
        propsym = (SymWithProps)sym;
      }
      if (span != null) {
        int childcount = sym.getChildCount();
        out.write(seq.getID());
        out.write('\t');
        int min = span.getMin();
        int max = span.getMax();
        out.write(Integer.toString(min));
        out.write('\t');
        out.write(Integer.toString(max));
        if ( (! span.isForward()) || (childcount > 0) || (propsym != null) ) {
          out.write('\t');
          if (propsym != null) {
            if (propsym.getProperty("name") != null) { out.write((String)propsym.getProperty("name")); }
            else if (propsym.getProperty("id") != null) { out.write((String)propsym.getProperty("id")); }
          }
          out.write('\t');
          if ((propsym != null)  && (propsym.getProperty("score") != null))  {
            out.write(propsym.getProperty("score").toString());
          } else if (sym instanceof Scored) {
            out.write(Float.toString(((Scored) sym).getScore()));
          }
          else { out.write('0'); }
          out.write('\t');
          if (span.isForward()) { out.write('+'); }
          else { out.write('-'); }
          if (childcount > 0) {
            out.write('\t');
            if ((propsym != null) && (propsym.getProperty("cds min") != null)) {
              out.write(propsym.getProperty("cds min").toString());
            }
            else { out.write(Integer.toString(min)); }
            out.write('\t');
            if ((propsym != null) && (propsym.getProperty("cds max") != null))  {
              out.write(propsym.getProperty("cds max").toString());
            }
            else { out.write(Integer.toString(max)); }
            out.write('\t');
            out.write('0');
            out.write('\t');
            out.write(Integer.toString(childcount));
            out.write('\t');
            int[] blockSizes = new int[childcount];
            int[] blockStarts = new int[childcount];
            for (int i=0; i<childcount; i++) {
              SeqSymmetry csym = sym.getChild(i);
              SeqSpan cspan = csym.getSpan(seq);
              blockSizes[i] = cspan.getLength();
              blockStarts[i] = cspan.getMin() - min;
            }
            for (int i=0; i<childcount; i++) {
              out.write(Integer.toString(blockSizes[i]));
              out.write(',');
            }
            out.write('\t');
            for (int i=0; i<childcount; i++) {
              out.write(Integer.toString(blockStarts[i]));
              out.write(',');
            }
          }  // END "if (childcount > 0)"
        }  // END "if ( (! span.isForward()) || (childcount > 0) || (propsym != null) )"

        out.write('\n');
      }   // END "if (span != null)"
    }
  }
    */



}