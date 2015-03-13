package genomancer.util;

import java.io.*;
import java.text.DecimalFormat;

/** 
 *  work like a stopwatch -- 
 *     elapsed time accumulates between calls to start and stop
 */
public class TimeCheck {
    DecimalFormat formatter = new DecimalFormat("###,###.###");
    long start_time; // in milliseconds
    long elapsed_time;  // in milliseconds
    String name = "";
    String prefix = name;
    boolean timer_on;
    
    public TimeCheck()  {
	reset();
    }

    public TimeCheck(String name) {
	this();
        this.name = name;
	if (name != null && name.length() > 0)  {
	    prefix = name + ", ";
	}
    }
    
    public void reset()  {
	timer_on = false;
	start_time = 0;
	elapsed_time = 0;
    }

    public void start() {
	if (! timer_on)  {
	    start_time = System.currentTimeMillis();
	    timer_on = true;
	}
    }

    public void stop() {
	if (timer_on)  {
	    long stop_time = System.currentTimeMillis();
	    elapsed_time += (stop_time - start_time);
	    timer_on = false;
	}
    }

    /**
     *   Get elapsed time
     *   If TimeCheck is currently stopped, then just reports elapsed time
     *   If TimeCheck is currently running, then calls stop to calculate elapsed time, 
     *     then start() to keep running
     */
    public long getElapsedTime()  {
	if (timer_on)  {
	    stop();
	    start();
	}
	return elapsed_time;
    }
    
    public String getElapsedTimeString()  {
      long elapsed_ms = getElapsedTime();
      float elapsed_seconds = elapsed_ms / 1000f;
      return formatter.format(elapsed_seconds);
    }
    
    @Override
    public String toString()  {
        return (prefix + "Time Elapsed (s) = " + getElapsedTimeString());
    }
    
    public void report()  { System.out.println(this); }

}
