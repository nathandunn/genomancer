package genomancer.util;

/**
 *   combines a MemCheck and a TimeCheck to simultaneously measure both time and memory usage
 */
public class TimeMemCheck  {
    TimeCheck tim;
    MemCheck mem;
    String name = "";
    
    public TimeMemCheck()  {
	this("");
    }

    public TimeMemCheck(String name)  {
	this(name, MemCheck.kB);
    }

    public TimeMemCheck(String name, long mem_units)  {
        this.name = name;
	tim = new TimeCheck();
	mem = new MemCheck("", mem_units);
    }

    public void reset()  {
	timeReset();
	memReset();
    }

    public void start()  {
	timeStart();
	memStart();
    }

    public void stop()  {
	timeStop();
	memStop();
    }

    public void timeReset()  { tim.reset(); }
    public void timeStart()  { tim.start(); }
    public void timeStop()  { tim.stop(); }
    public void memReset()  { mem.reset(); }
    public void memStart()  { mem.start(); }
    public void memStop()  { mem.stop(); }

    public String toString()  {
	return toString(name);
    }

    public String toString(String label)  {
	String prefix = label;
	if (prefix == null)  { prefix = name + ","; }
	return (prefix + "  " + tim.toString() + ", " + mem.toString());
    }


    public String toVerboseString()  {
	return (name + ", " + tim.toString() + ", " + mem.toVerboseString());
    }

    public void report()  { report(name); }

    public void report(String label)  { System.out.println(this.toString(label)); }

    public void reportVerbose()  { System.out.println(this.toVerboseString()); }

    
}