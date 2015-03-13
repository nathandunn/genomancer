package genomancer.util;

import java.util.*;
import java.text.DecimalFormat;


/**
 *   Works like TimeCheck (like a stopwatch), but with memory instead of time
 *
 *   So deltas are cumulative between calls to start and stop  (at least for delta_used)
 */
public class MemCheck {
    static DecimalFormat int_formatter = new DecimalFormat("###,###,###");
    static DecimalFormat float_formatter = new DecimalFormat("###,###,###.##");
    static Map unit2string = new LinkedHashMap();
    //  kB, etc. is somewhat ambiguous nowadays -- either 1024 or 1000
    public static final long B = 1L;             // 1 byte
    public static final long kB = 1000L;         // 1 kB = 1000 bytes (10^3)
    public static final long KB = 1024L;         // 1 KB = 1024 bytes (2^10)
    public static final long mB = kB * kB;       // 1 mB = 1000 kb = 1,000,000 bytes (10^6)
    public static final long MB = KB * KB;       // 1 MB = 1024 KB = 1,048,576 bytes (2^20)
    public static final long gB = kB * kB * kB;  // 1 gB = 1000 mB = 1000000000 bytes (10^6)
    public static final long GB = KB * KB * KB;  // 1 GB = 1024 MB = 1,073,741,824 bytes (10^9)

    Runtime run_time;
    long units = kB;
    long start_free;
    long start_total; 
    long start_used;
    long delta_used;
    String name = "";
    String prefix = name;
    boolean started = false;

    static  {
	// using autoboxing of longs to Longs for hash table
	unit2string.put(B, "B");
	unit2string.put(kB, "kB");
	unit2string.put(KB, "KB");
	unit2string.put(mB, "mB");
	unit2string.put(MB, "MB");
	unit2string.put(gB, "gB");
	unit2string.put(GB, "GB");
    }

    public MemCheck() {
	run_time = Runtime.getRuntime();
	reset();
    }

    public MemCheck(String name)  {
	this();
	this.name = name;
	if (name != null && name.length() > 0)  {
	    prefix = name + ", ";
	}
    }

    public MemCheck(String name, long units)  {
	this(name);
	this.units = units;
    }
	
	
    public void reset()  {
	started = false;
	delta_used = 0;
    }

    public void start()  {
	if (! started)  {
	    start_free = run_time.freeMemory();
	    start_total = run_time.totalMemory();
	    start_used = start_total - start_free;
	    started = true;
	}
    }

    public void stop()  {
	if (started)  {
	    long current_free = run_time.freeMemory();
	    long current_total = run_time.totalMemory();
	    long current_used = current_total - current_free;
	    delta_used += (current_used - start_used);
	    started = false;
	}
    }

    public long getMemoryUsage()  {
	if (started)  {
	    stop();
	    start();
	}
	return delta_used;
    }

    public String getMemoryUsageString()  {
	long byte_usage = getMemoryUsage();
	float units_usage = (float)byte_usage / (float)units;  // usage scaled, by default to kilobytes unless was specified in constructor
	DecimalFormat formatter = getFormatter();
	return formatter.format(units_usage);
    }

    protected DecimalFormat getFormatter()  {
	DecimalFormat formatter;
	if (units == B || units == kB || units == KB)  {  formatter = int_formatter; }
	else  { formatter = float_formatter; }
	return formatter;
    }

    protected String getUnitString()  {
	String ustr = (String)unit2string.get(units);
	if (ustr == null)  { ustr = Long.toString(units); }
	return ustr;
    }
 
    public String toString()  {
	String ustr = getUnitString();
        return (prefix + "Memory Change (" + ustr + ") = " + getMemoryUsageString());
    }
    
    public void report()  { System.out.println(this); }


    public String toVerboseString()  {
	double current_free = (double)run_time.freeMemory() / (double)units;
	double current_total = (double)run_time.totalMemory() / (double)units;
	double current_used = current_total - current_free;
	double current_max = (double)run_time.maxMemory() / (double)units;
	double current_used_delta = (double)getMemoryUsage() / (double)units;
	DecimalFormat formatter = getFormatter();
	String ustr = getUnitString();
	
	return 	    
	    (prefix + "used memory delta (" + ustr + ") = " + formatter.format(current_used_delta) + 
	     ",  used = " + formatter.format(current_used) +
	     ",  free = " + formatter.format(current_free) +
	     ",  total = " + formatter.format(current_total) +
	     ",  max = " + formatter.format(current_max) );
    }

    public void reportVerbose()  { System.out.println(this.toVerboseString()); }

}
