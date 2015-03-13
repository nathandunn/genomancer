package genomancer.ucsc.das2;

import java.util.*;

public enum TrackType { 
    BED3, 
	BED4, 
	BED5, 
	BED6, 
	BED8, 
	BED9, 
	BED12, 
	BED15, 
	GENEPRED, 
	PSL, 
	RMSK, 
	WIGGLE, 
	WIGGLE_MAF, 
	NETALIGN, 
	CHAIN, 


	// funky BED variants
	BED5_FLOATSCORE, 
	BED5_FLOATSCORE_FDR, 
	BEDGRAPH4, 
	BEDGRAPH5, 
	EXPRATIO, 
	ID2, 

	// rare formats
	ALTGRAPHX, 
	CLONEPOS, 
	CTGPOS, 
	COLOREDEXON, 
	
	// older formats (not present in hg18?)
	GL, 
	CHROMGRAPH, 
	LINKEDFEATURES, 
	UNKNOWN;

    
    /**
     *  map of format names to TableTypeFormatters
     */
    //    Map format2handler = new LinkedHashMap();

    /**
     *  map of mime-types to TableTypeFormatters
     */
    //    Map mimetype2handler = new LinkedHashMap();
    //    Set formats = new HashSet();

    /**
     *  if multiple handlers with same formats/mimetypes are added, 
     *  then handler added last gets precedence
     */
    /*
      public boolean addHandler(FormatHandler handler)  {
      if (! (handler.getTrackTypes().contains(this)))  { return false; }
      Iterator entries = handler.getOutputFormats().entrySet().iterator();
      while (entries.hasNext())  {
      Map.Entry entry = (Map.Entry)entries.next();
      String format = (String)entry.getKey();
      String mimetype = (String)entry.getValue();
      format2handler.put(format, handler);
      mimetype2handler.put(mimetype, handler);
      formats.add(format);
      }
      return true;      
      }
    */

    /*
     *    getFormatter format arg can either be a format or mimetype 
     *    returns null if no Formatter for the input format/mimetype is found
     *
     */
    /*
      public FormatHandler getHandler(String format)  {
      FormatHandler formatter = (FormatHandler)format2handler.get(format);
      if (formatter == null)  {  formatter = (FormatHandler)mimetype2handler.get(format); }
      return formatter;
      }

      public Iterator<String> getSupportedFormats()  {
      return formats.iterator();
      }
    */
	

};

