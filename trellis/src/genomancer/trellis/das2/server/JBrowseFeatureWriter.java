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
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Stack;
import java.util.logging.Level;
import java.util.logging.Logger;


public class JBrowseFeatureWriter implements Das2FeatureWriterI {
    static Das2Format format = new Das2Format("jbrowse-feature-json", "application/json");
    static String FORWARD_STRAND = "1";
    static String REVERSE_STRAND = "-1";

    static MinMaxComparator3 sorter = new MinMaxComparator3();

    protected int temp = 1;

    public JBrowseFeatureWriter() {
	System.out.println("called JBrowseFeatureWriter constructor");
    }

    /** for now assuming any type can be represented as JBrowse JSON
     *  NEED TO FIX at some point,
     *  since this assumption does not hold for annotation trees with depth > 2
     *     and many other situations
     */
    public boolean acceptsType(Das2TypeI type) {
	return true;
    }

    public Das2FormatI getFormat()  { return format; }

    /**
     *   hack to flatten three-level gene / mRNA / exon hierarchy to 
     *     a two-level mRNA / exon hierarchy
     *     (need this because currently JBrowse only renders top two levels of a feature hierarchy)
     */
    public List<Das2FeatureI> stripGeneLevel(List<Das2FeatureI> annots)  {
	List<Das2FeatureI> stripped_annots = new ArrayList();
        for (Das2FeatureI annot: annots) {
            if (annot.getType().getLocalURIString().equalsIgnoreCase("gene"))  {
                for (Das2FeatureI gene_child : annot.getParts())  {
                    stripped_annots.add(gene_child);
                }
            }
            else  {
                stripped_annots.add(annot);
            }
        }
        return stripped_annots;
    }

    public boolean writeFeatures(Das2FeaturesResponseI feature_response, OutputStream ostr) {
	List<Das2FeatureI> annots = feature_response.getFeatures();
	annots = stripGeneLevel(annots);

	//	annots = addCDS(annots);
	Writer bw = new BufferedWriter(new OutputStreamWriter(ostr));
	// extract query seq(s) from feature_response?
	//	Das2FeaturesQueryI query = feature_response.getQuery();
	NCList3 toplist = NCList3.createNCList(annots);
	try {
          bw.write('[');
	  // if no features in the feature_response, then toplist returned from createNCList will be null
	  //     in that case return empty array
	  if (toplist != null)  {
	      List<NCList3> sublists = toplist.getSublists();
	      int slcount = 0;
	      for (NCList3 sublist : sublists)  {
		  if (slcount > 0)  { bw.write(','); }
		  writeNCList(bw, sublist);
		  slcount++;
	      }
	  }
          bw.write(']');
          bw.flush(); // ???
        } catch (IOException ex) {
            Logger.getLogger(JBrowseFeatureWriter.class.getName()).log(Level.SEVERE, null, ex);
        }
	return true;
    }

    /**
     *   For now assume first location in locations list is correct one
     *   NEED TO FIX, this assumption is not likely to hold
     *
     *  [  [ 0, min, max, strand, id, type, subfeatures ], ... ]
     *  0 at beginning of array is index into classes array for feature array definition
     *
     */
    public boolean writeNCList(Writer out, NCList3 curlist) throws IOException {
        Das2FeatureI annot = curlist.getDasFeature();
        List<Das2LocationI> locs = annot.getLocations();
        if (locs == null || locs.size() <= 0) {
            // no locations for annot, not writing out
            return false;
        }
        Das2LocationI span = locs.get(0);
        int min = span.getMin();
        int max = span.getMax();
        out.write('[');
	out.write(Integer.toString(0));
	out.write(',');
        out.write(Integer.toString(min));
        out.write(',');
        out.write(Integer.toString(max));
        out.write(',');
        if (span.getStrand() == Strand.REVERSE)  {
            out.write(REVERSE_STRAND);
        } else {
            out.write(FORWARD_STRAND);
        }
        out.write(',');
        String annotid = annot.getLocalURIString();
        if (annotid == null) {
            out.write("null");
        } else {
            out.write('"');
            out.write(annotid);
            out.write('"');
        }
        out.write(',');
	Das2TypeI type = annot.getType();
	String type_name = type.getLocalURIString();
	out.write("\"" + type_name + "\"");
	out.write(',');
        List<Das2FeatureI> subannots = annot.getParts();

        if (subannots != null && subannots.size() > 0) {
	    Collections.sort(subannots, sorter);
	    Das2FeatureI start_codon = null;
	    Das2FeatureI stop_codon = null;
	    boolean has_whole_cds = false;
            // assumes max 2-level annotation hierarchy
            out.write('[');
	    int sacount = 0;
            for (Das2FeatureI subannot : subannots) {
		Das2TypeI subtype = subannot.getType();
		String subtype_name = subtype.getLocalURIString();

                // currently for JSON output subannot represented by array data struct where
                //   subannot[0] = 1  ==> index into classes array for subfeature array definition
                //   subannot[1] = min
                //   subannot[2] = max
                //   subannot[3] = strand
                //   subannot[4] = type

		if (subtype_name.equalsIgnoreCase("start_codon"))  {
		    start_codon = subannot;
		    continue;
		}
		else if (subtype_name.equalsIgnoreCase("stop_codon"))  {
		    stop_codon = subannot;
		    continue;
		}
		else if (subtype_name.equalsIgnoreCase("wholeCDS")) {
		    has_whole_cds = true;
		}

		if (sacount > 0)  { out.write(','); }
		sacount++;

                List<Das2LocationI> sublocs = subannot.getLocations();
                Das2LocationI subspan = sublocs.get(0);
                out.write('[');
		out.write(Integer.toString(1));
		out.write(',');
                out.write(Integer.toString(subspan.getMin()));
                out.write(',');
                out.write(Integer.toString(subspan.getMax()));
                out.write(',');
                if (subspan.getStrand() == Strand.REVERSE) {
                    out.write(REVERSE_STRAND);
                } else {
                    out.write(FORWARD_STRAND);
                }
                out.write(',');
		out.write("\"" + subtype_name + "\"");
                out.write(']');
                // out.write(',');
            }
	    // handling conversion of start_codon / stop_codon to wholeCDS subfeature
	    if (start_codon != null  && 
		stop_codon != null && 
		!has_whole_cds)  {
		int transtart = start_codon.getLocations().get(0).getMin();
		Das2LocationI stoploc = stop_codon.getLocations().get(0);
		int transend;
		// want end of translation to be at beginning of stop codon??
		if (stoploc.getStrand() == Strand.REVERSE)  { transend = stoploc.getMax(); }
		else  { transend = stoploc.getMin();  }
                out.write(',');
                out.write('[');
		out.write(Integer.toString(1));
		out.write(',');
                out.write(Integer.toString(transtart));
		out.write(',');
		out.write(Integer.toString(transend));
                out.write(',');
                if (stoploc.getStrand() == Strand.REVERSE) {
                    out.write(REVERSE_STRAND);
                } else {
                    out.write(FORWARD_STRAND);
                }
                out.write(',');
		out.write("\"wholeCDS\"");
                out.write(']');
	    }
            out.write(']');
        }
        else  {  // no child annotations
            out.write("null");
        }
        List<NCList3> sublists = curlist.getSublists();
        if (sublists != null && sublists.size() > 0)  {
            out.write(',');
	    out.write(" {\"Sublist\": ");
            out.write('[');
	    int slcount = 0;
            for (NCList3 sublist : sublists)  {
		if (slcount > 0)  { out.write(','); }
		slcount++;
                writeNCList(out, sublist);
                // out.write(',');
            }
            out.write(']');
	    out.write('}');
        }
        out.write(']');
        // out.flush(); ???
        return true;
    }

}

  /*
    *  sorts by min and max of first location span
    *     first by ascending min  (lowest min first)
    *       if mins equal, then by descending max   (highest max first)
    * assumes all annots' first span are on same sequence
    */
   class MinMaxComparator3 implements Comparator {
        public int compare(Object o1, Object o2) {
            Das2FeatureI annot1 = (Das2FeatureI)o1;
            Das2FeatureI annot2 = (Das2FeatureI)o2;
            Das2LocationI span1 = annot1.getLocations().get(0);
            Das2LocationI span2 = annot2.getLocations().get(0);
            if (span1.getMin() != span2.getMin())  {
                return span1.getMin() - span2.getMin();
            }
            else  {
                return span2.getMax() - span1.getMax();
            }
        }
    }



  class NCList3  {
       public static boolean DEBUG_NCLIST = false;
       public static MinMaxComparator3 sorter = new MinMaxComparator3();
        Das2FeatureI annot;
        List<NCList3> sublists;


         // @param annotlist
         // assumes annotlist is already sorted
         //    first by ascending min  (lowest min first)
         //      if mins equal, then by descending max   (highest max first)
         // assumes all annots' first span are on same sequence

        public static NCList3 createNCList(List<Das2FeatureI> annotlist)  {
            if (annotlist == null || annotlist.size() == 0)  { return null; }
            System.out.println("building NCList, annot count: " + annotlist.size());
            // MinMaxComparator3 sorter = new MinMaxComparator3();
            Collections.sort(annotlist, sorter);
            if (DEBUG_NCLIST)  { System.out.println("   sorted input annot list"); }
            //            System.out.println("containment count: " + sorter.getContainCount());
            int contain_count = 0;
            int pop_count = 0;


            List<NCList3> ncannots = new ArrayList(annotlist.size());
            // each Das2FeatureI must be wrapped with an NCList3, so go ahead and do this first...
            for (Das2FeatureI annot : annotlist)  {
                ncannots.add(new NCList3(annot));
            }

            // top of NCList has no associated Das2FeatureI, recursively contains all other NCList3s
            NCList3 uberlist = new NCList3(null);
            // sorted by ascending start, then descending end, so therefore
            //    first nclist _must_ be sublist of toplist
            uberlist.addSublist(ncannots.get(0));

            if (DEBUG_NCLIST)  { System.out.println("uberlist: " + uberlist); }


            // ncstack is a list of all currently relevant sublists
            //   (one for each level of nesting)
            Stack<NCList3> ncstack = new Stack<NCList3>();
            ncstack.push(uberlist);

            for (int i=1;i<ncannots.size(); i++)  {
                NCList3 prev_ncannot = ncannots.get(i-1);
                NCList3 current_ncannot = ncannots.get(i);
                if (DEBUG_NCLIST)  { System.out.println("current annot: " + current_ncannot); }
                Das2FeatureI prev_annot = prev_ncannot.getDasFeature();
                Das2FeatureI current_annot = current_ncannot.getDasFeature();
                Das2LocationI prevloc = prev_annot.getLocations().get(0);
                Das2LocationI current_loc = current_annot.getLocations().get(0);
                // already sorted by ascending min, and secondarily by descending max,
                // so already know:
                //    prevmin <= minB
                //    if (prevmin == minB)
                //        then maxA >= maxB
                int prevmax = prevloc.getMax();
                int max = current_loc.getMax();
                if (max < prevmax)  {
                    // current annot is contained in prev annot,
                    //   and (by lemma 2) is therefore a new sublist of prev nclist
                    contain_count++;
                    // prev.addSublist(current) equivalent to:
                    // curList = new Array(curInterval);
                    // myIntervals[i - 1][sublistIndex] = curList;
                    if (DEBUG_NCLIST) {System.out.println("      adding to prev list: " + prev_ncannot);}
                    prev_ncannot.addSublist(current_ncannot);
                    //  equivalent to sublistStack.push(
                    ncstack.push(prev_ncannot);
                }
                else  {
                    // otherwise current annot is contained in an NCList on the
                    //   stack (including possibly the toplist)
                    NCList3 stacktop = ncstack.pop();
                    while (true)  {
                        if (stacktop == uberlist) {
                        // if not contained in any sublists, then it's contained in uberlist
                            if (DEBUG_NCLIST)  { System.out.println("    adding to uberlist");}
                            uberlist.addSublist(current_ncannot);
                            if (ncstack.empty()) { ncstack.push(stacktop); }
                            break;
                        }
                        else  {
                            if (DEBUG_NCLIST)  { System.out.println("     trying stacktop: " + stacktop); }
                            Das2FeatureI stack_annot = stacktop.getDasFeature();
                            Das2LocationI stack_loc = stack_annot.getLocations().get(0);
                            int stackmax = stack_loc.getMax();
                            if (max < stackmax)  {
                                // contained in stacktop, so add as sublist
                                if (DEBUG_NCLIST)  { System.out.println("         success, adding to current stacktop"); }
                                stacktop.addSublist(current_ncannot);
                                ncstack.push(stacktop);
                                break;
                            }
                            else  {
                                pop_count++;
                                stacktop = ncstack.pop();
                            }
                        }
                    }
                }

            }
	    if (DEBUG_NCLIST)  {
		System.out.println("finished building NCList3");
		System.out.println("containment count: " + contain_count);
		System.out.println("stack pop count: " + pop_count);
	    }
            return uberlist;
        }

        public NCList3(Das2FeatureI annot)  {
            this.annot = annot;
        }

        public Das2FeatureI getDasFeature()  { return annot; }

        public void addSublist(NCList3 sublist)  {
            if (sublists == null) { sublists = new ArrayList(); }
            sublists.add(sublist);
        }

        public String toString()  {
            String locstr = ((annot == null) ? "no annot" :
                ("min: " + annot.getLocations().get(0).getMin() +
                 ", max: " + annot.getLocations().get(0).getMax()));

            String liststr = ((sublists == null) ? "no sublists" :
                ("sublist_size: " + sublists.size()));
            return super.toString() + ":: "+ locstr + ", " + liststr;
        }

       public List<NCList3> getSublists() {
           return sublists;
       }
    }
