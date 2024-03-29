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


public class JBrowseNCListFullFeatureWriter implements Das2FeatureWriterI {
    static Das2Format format = new Das2Format("jbrowse-nclist-full-json", "application/json");
    static String FORWARD_STRAND = "1";
    static String REVERSE_STRAND = "-1";

    protected int temp = 1;
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
      NCList2 toplist = NCList2.createNCList(annots);
	try {
          bw.write('[');
	  // if no features in the feature_response, then toplist returned from createNCList will be null
	  //     in that case return empty array
	  if (toplist != null)  {
	      List<NCList2> sublists = toplist.getSublists();
	      for (NCList2 sublist : sublists)  {
		  writeNCList(bw, sublist);
		  bw.write(',');
	      }
	  }
          bw.write(']');
          bw.flush(); // ???
        } catch (IOException ex) {
            Logger.getLogger(JBrowseNCListFullFeatureWriter.class.getName()).log(Level.SEVERE, null, ex);
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
    public boolean writeNCList(Writer out, NCList2 curlist) throws IOException {
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
        List<Das2FeatureI> subannots = annot.getParts();
        if (subannots != null && subannots.size() > 0) {
            // assumes max 2-level annotation hierarchy
            out.write('[');
            for (Das2FeatureI subannot : subannots) {
                // currently for JSON output subannot represented by array data struct where
                //   subannot[0] = min
                //   subannot[1] = max
                //   subannot[2] = strand
                //   subannot[3] = type
                List<Das2LocationI> sublocs = subannot.getLocations();
                Das2LocationI subspan = sublocs.get(0);
                out.write('[');
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
		Das2TypeI subtype = subannot.getType();
		// TODO: should probably base CDS testing on sequence ontology CDS term rather
		//    than URI
		//		if (subtype.getLocalURIString().equals("CDS"))  { // pass through CDS types
		//		    out.write("\"CDS\"");
		//		}
		//		else  {  // for now, treat all others as UTRs
		//		    out.write("\"UTR\"");
		//		}
		String type_name = subtype.getLocalURIString();
		out.write("\"" + type_name + "\"");
                out.write(']');
                out.write(',');
            }
            out.write(']');
        }
        else  {  // no child annotations
            out.write("null");
        }
        List<NCList2> sublists = curlist.getSublists();
        if (sublists != null && sublists.size() > 0)  {
            out.write(',');
            out.write('[');
            for (NCList2 sublist : sublists)  {
                writeNCList(out, sublist);
                out.write(',');
            }
            out.write(']');
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
   class MinMaxComparator2 implements Comparator {
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



  class NCList2  {
       public static boolean DEBUG_NCLIST = false;
        Das2FeatureI annot;
        List<NCList2> sublists;


         // @param annotlist
         // assumes annotlist is already sorted
         //    first by ascending min  (lowest min first)
         //      if mins equal, then by descending max   (highest max first)
         // assumes all annots' first span are on same sequence

        public static NCList2 createNCList(List<Das2FeatureI> annotlist)  {
            if (annotlist == null || annotlist.size() == 0)  { return null; }
            System.out.println("building NCList2(), annot count: " + annotlist.size());
            MinMaxComparator2 sorter = new MinMaxComparator2();
            Collections.sort(annotlist, sorter);
            System.out.println("   sorted input annot list");
            //            System.out.println("containment count: " + sorter.getContainCount());
            int contain_count = 0;
            int pop_count = 0;


            List<NCList2> ncannots = new ArrayList(annotlist.size());
            // each Das2FeatureI must be wrapped with an NCList2, so go ahead and do this first...
            for (Das2FeatureI annot : annotlist)  {
                ncannots.add(new NCList2(annot));
            }

            // top of NCList has no associated Das2FeatureI, recursively contains all other NCList2s
            NCList2 uberlist = new NCList2(null);
            // sorted by ascending start, then descending end, so therefore
            //    first nclist _must_ be sublist of toplist
            uberlist.addSublist(ncannots.get(0));

            if (DEBUG_NCLIST)  { System.out.println("uberlist: " + uberlist); }


            // ncstack is a list of all currently relevant sublists
            //   (one for each level of nesting)
            Stack<NCList2> ncstack = new Stack<NCList2>();
            ncstack.push(uberlist);

            for (int i=1;i<ncannots.size(); i++)  {
                NCList2 prev_ncannot = ncannots.get(i-1);
                NCList2 current_ncannot = ncannots.get(i);
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
                    NCList2 stacktop = ncstack.pop();
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
            System.out.println("finished building NCList2");
            System.out.println("containment count: " + contain_count);
            System.out.println("stack pop count: " + pop_count);
            return uberlist;
        }

        public NCList2(Das2FeatureI annot)  {
            this.annot = annot;
        }

        public Das2FeatureI getDasFeature()  { return annot; }

        public void addSublist(NCList2 sublist)  {
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

       public List<NCList2> getSublists() {
           return sublists;
       }
    }
