package genomancer.ucsc.das2;

/**
 *  Replicating functionality of parts of kent/src/inc/binRange.h and kent/src/lib/binRange.c
 *
 *  For now just doing "standard" binning scheme (128K, 1M, 8M, 64M, 512M), 
 *      still need to add "extended" binning scheme (standard + extra 4GB bin)
 *      (this will only matter for organisms with chromosomes larger than 512M)
 *
 *  
 */

public class BinRange  {
    static int BINRANGE_MAXEND_512M = 512*1024*1024;
    static int _binOffsetOldToExtended = 4681;  //  (4096 + 512 + 64 + 8 + 1 + 0)

    static int binOffsets[] = {512+64+8+1,   // = 585, min val for level 0 bins (128kb binsize)
			       64+8+1,       // =  73, min val for level 1 bins (1Mb binsize)
			       8+1,          // =   9, min val for level 2 bins (8Mb binsize)
			       1,            // =   1, min val for level 3 bins (64Mb binsize)
			       0};           // =   0, only val for level 4 bin (512Mb binsize)
    //    1:   0000 0000 0000 0001    1<<0       
    //    8:   0000 0000 0000 1000    1<<3
    //   64:   0000 0000 0100 0000    1<<6
    //  512:   0000 0010 0000 0000    1<<9
    
    static int _binFirstShift = 17;	// How much to shift to get to finest bin.
    static int _binNextShift = 3;	// How much to shift to get to next larger bin. 

    /** Return number of levels to bins. */
    public final static int binLevels()  {
	return binOffsets.length;
    }

    /** Return amount to shift a number to get to finest bin. */
    public final static int binFirstShift()  {
	return _binFirstShift;
    }

    /** Return amount to shift a number to get to next coarser bin. */
    public final static int binNextShift()  {
	return _binNextShift;
    }

    /** Return offset for bins of a given level. */    
    public final static int binOffset(int level)  {
	if ((level < 0) || (level >= binLevels())) {
	    return -1; 
	}
	return binOffsets[level];
    }

    /** 
     *   based on kent/src/hg/lib/hdb.c addBinToQueryStandard() method 
     *
     *  Assumptions:
     *     bin field in database table is always named "bin"
     *     clause is always "self-contained" but caller constructing query will supply any AND prefix/postfix needed
     *     
     *    get clause that will restrict to relevant bins to query. 
     */
    public static String getBinIndexingClause(int start, int end)  {
	StringBuffer buf = new StringBuffer(200);
	int bFirstShift = binFirstShift();
	int bNextShift = binNextShift();
	int startBin = (start >> bFirstShift);
	int endBin = ((end-1)>>bFirstShift);
	int levels = binLevels();
	buf.append("(");
	for (int i=0; i<levels; ++i)  {
	    int offset = binOffset(i);
	    if (i != 0)  { buf.append(" or "); }
	    if (startBin == endBin)  { 
		buf.append("bin=");
		buf.append(startBin + offset);
	    }
	    else  {
		buf.append("(bin>=");
		buf.append(startBin + offset);
		buf.append(" and bin<=");
		buf.append(endBin + offset);
		buf.append(")");
	    }
	    startBin >>= bNextShift;
	    endBin >>= bNextShift;
	}
	//	buf.append(" or bin="); buf.append(_binOffsetOldToExtended); buf.append(" )";
	buf.append(" )");
	//	buf.append(" and ");
	return buf.toString();
    }

    public static void main(String[] args)  {
	testBinIndexing(1000,50000);
	testBinIndexing(1000,12001000);
	testBinIndexing(21000100, 21300000);
    }

    public static void testBinIndexing(int start, int end)  {
	String result = getBinIndexingClause(start, end);
	System.out.println("Testing BinRange.getBinIndexingClause(), start = " + start + ", end = " + end);
	System.out.println("    result: " + result);
    }


    /*
    public String getRangeQuery( // left out DB connection, for now just building query string
				// String rootTable, 
				Das2Type type, 
				String chrom, 
				int start,
				int end, 
				String extraWhere,
				boolean order, 
				String fields
				// left out retRowOffset arg, not clear if needed
				)  {
	StringBuffer buf = new StringBuffer(1000);
	

	return buf.toString();
    }
    */

}



/*
in kent source code, binning call stack main branch:
   das.c --> doFeatures()
   	 hdb.c --> hRangeQuery(...)
	         --> hExtendedRangeQuery(...)
		   --> hAddBinToQuery(start, end, query)
		     --> hAddBinToQueryGeneral("bin", start, end, query) 
		     	      binRange.h usage:  BINRANGE_MAXEND_512M   /-- don't understand this use --/
		       --> hAddBinToQueryStandard("bin", start, end, query, TRUE)
		       	     binRange.c / binRange.h usages:
		       	      binFirstShift()
			      binNextShift()
			      binLevels()
			      _binOffsetOldToExtended  /--don't understand this use --/

struct sqlResult *hRangeQuery(struct sqlConnection *conn,
	char *rootTable, char *chrom,
	int start, int end, char *extraWhere, int *retRowOffset)
/-- Construct and make a query to tables that may be split and/or binned. --/
{
return hExtendedRangeQuery(conn, rootTable, chrom, start, end, 
	extraWhere, FALSE, NULL, retRowOffset);
}

struct sqlResult *hExtendedRangeQuery(
	struct sqlConnection *conn,  /-- Open SQL connection. --/
	char *rootTable, 	     /-- Table (not including any chrN_) --/
	char *chrom, int start, int end,  /-- Range. --/
	char *extraWhere,            /-- Extra things to add to where clause. --/
	boolean order, 	   /-- If true order by start position (can be slow). --/
	char *fields,      /-- If non-NULL comma separated field list. --/
	int *retRowOffset) /-- Returns offset past bin field. --/
/-- Range query with lots of options. --/
{
char *db = sqlGetDatabase(conn);
struct hTableInfo *hti = hFindTableInfoDb(db, chrom, rootTable);
struct sqlResult *sr = NULL;
struct dyString *query = newDyString(1024);
char *table = NULL;
int rowOffset = 0;

if (fields == NULL) fields = "*";
if (hti == NULL)
    {
    warn("table %s doesn't exist or hFindTableInfoDb failed", rootTable);
    }
else
    {
    dyStringPrintf(query, "select %s from ", fields);
    if (hti->isSplit)
	{
	char fullTable[HDB_MAX_TABLE_STRING];
	safef(fullTable, sizeof(fullTable), "%s_%s", chrom, rootTable);
	if (!hTableExistsDb(db, fullTable))
	     warn("%s doesn't exist", fullTable);
	else
	    {
	    table = fullTable;
	    dyStringPrintf(query, "%s where ", table);
	    }
	}
    else
        {
	table = rootTable;
	dyStringPrintf(query, "%s where %s='%s' and ", 
	    table, hti->chromField, chrom);
	}
    }
if (table != NULL)
    {
    if (hti->hasBin)
        {
           hAddBinToQuery(start, end, query);
	   rowOffset = 1;
	}
	dyStringPrintf(query, "%s<%u and %s>%u", 
    	hti->startField, end, hti->endField, start);
	if (extraWhere)
        {
        /-- allow more flexible additions to where clause --/
        if (!startsWith("order", extraWhere) && 
	!startsWith("limit", extraWhere))
	dyStringAppend(query, " and ");
        dyStringPrintf(query, " %s", extraWhere);
        }
    if (order)
        dyStringPrintf(query, " order by %s", hti->startField);
    sr = sqlGetResult(conn, query->string);
    }
freeDyString(&query);
if (retRowOffset != NULL)
    *retRowOffset = rowOffset;
return sr;
}


void hAddBinToQuery(int start, int end, struct dyString *query)
/-- Add clause that will restrict to relevant bins to query. --/
{
hAddBinToQueryGeneral("bin", start, end, query);
}

void hAddBinToQueryGeneral(char *binField, int start, int end, 
	struct dyString *query)
/-- Add clause that will restrict to relevant bins to query. --/
{
if (end <= BINRANGE_MAXEND_512M)
    hAddBinToQueryStandard(binField, start, end, query, TRUE);
else
    hAddBinToQueryExtended(binField, start, end, query);
}

static void hAddBinToQueryStandard(char *binField, int start, int end, 
	struct dyString *query, boolean selfContained)
/-- Add clause that will restrict to relevant bins to query. --/
{
int bFirstShift = binFirstShift(), bNextShift = binNextShift();
int startBin = (start>>bFirstShift), endBin = ((end-1)>>bFirstShift);
int i, levels = binLevels();

if (selfContained)
    dyStringAppend(query, "(");
for (i=0; i<levels; ++i)
    {
    int offset = binOffset(i);
    if (i != 0)
        dyStringAppend(query, " or ");
    if (startBin == endBin)
        dyStringPrintf(query, "%s=%u", binField, startBin + offset);
    else
        dyStringPrintf(query, "%s>=%u and %s<=%u", 
		binField, startBin + offset, binField, endBin + offset);
    startBin >>= bNextShift;
    endBin >>= bNextShift;
    }
if (selfContained)
    {
    dyStringPrintf(query, " or %s=%u )", binField, _binOffsetOldToExtended);
    dyStringAppend(query, " and ");
    }
}

*/


/*
-------------------------------------------------------------
     From kent/src/inc/binRange.h
-------------------------------------------------------------
#ifndef BINRANGE_H
#define BINRANGE_H

/-- binRange Stuff to handle binning - which helps us restrict 
 * our attention to the parts of database that contain info
 * about a particular window on a chromosome. This scheme
 * will work without modification for chromosome sizes up
 * to half a gigaBase.  The finest sized bin is 128k (1<<17).
 * The next coarsest is 8x as big (1<<13).  There's a hierarchy
 * of bins with the chromosome itself being the final bin.
 * Features are put in the finest bin they'll fit in. 
 *
 * This file is copyright 2002 Jim Kent, but license is hereby
 * granted for all use - public, private or commercial. --/

#define BINRANGE_MAXEND_512M (512*1024*1024)
#define _binOffsetOldToExtended  4681

int binLevelsExtended();
/-- Return number of levels to bins. --/

int binLevels();
/-- Return number of levels to bins. --/

int binFirstShift();
/-- Return amount to shift a number to get to finest bin. --/

int binNextShift();
/-- Return amount to shift a number to get to next coarser bin. --/

int binOffsetExtended(int level);
/-- Return offset for bins of a given level. --/

int binOffset(int level);
/-- Return offset for bins of a given level. --/

/--****  And now for some higher level stuff - useful for binning
 *****  things in memory. *****--/

int binFromRange(int start, int end);
/-- Given start,end in chromosome coordinates assign it
 * a bin.   There's a bin for each 128k segment, for each
 * 1M segment, for each 8M segment, for each 64M segment,
 * and for each chromosome (which is assumed to be less than
 * 512M.)  A range goes into the smallest bin it will fit in. --/

struct binElement
/-- An element in a bin. --/
    {
    struct binElement *next;
    int start, end;		/-- 0 based, half open range --/
    void *val;			/-- Actual bin item. --/
    };

int binElementCmpStart(const void *va, const void *vb);
/-- Compare to sort based on start. --/

struct binKeeper
/-- This keeps things in bins in memory --/
    {
    struct binKeeper *next;
    int minPos;		/-- Minimum position to bin. --/
    int maxPos;		/-- Maximum position to bin. --/
    int binCount;	/-- Count of bins. --/
    struct binElement **binLists; /-- A list for each bin. --/
    };

struct binKeeperCookie
/-- used by binKeeperFirst/binKeeperNext in tracking location in traversing bins --/
    {
    struct binKeeper *bk;       /-- binKeeper we are associated with --/
    int blIdx;                  /-- current bin list index --/
    struct binElement *nextBel; /-- next binElement --/
    };

struct binKeeper *binKeeperNew(int minPos, int maxPos);
/-- Create new binKeeper that can cover range. --/

void binKeeperFree(struct binKeeper **pBk);
/-- Free up a bin keeper. --/

void binKeeperAdd(struct binKeeper *bk, int start, int end, void *val);
/-- Add item to binKeeper. --/ 

void binKeeperRemove(struct binKeeper *bk, int start, int end, void *val);
/-- Remove item from binKeeper. --/ 

struct binElement *binKeeperFind(struct binKeeper *bk, int start, int end);
/-- Return a list of all items in binKeeper that intersect range.
 * Free this list with slFreeList. --/

struct binElement *binKeeperFindSorted(struct binKeeper *bk, int start, int end);
/-- Like binKeeperFind, but sort list on start coordinates. --/

struct binElement *binKeeperFindAll(struct binKeeper *bk);
/-- Get all elements sorted. --/

boolean binKeeperAnyOverlap(struct binKeeper *bk, int start, int end);
/-- Return TRUE if start/end overlaps with any items in binKeeper. --/

void binKeeperReplaceVal(struct binKeeper *bk, int start, int end,
	void *oldVal, void *newVal);
/-- Replace occurences of old val in range from start->end with newVal --/

struct binElement *binKeeperFindLowest(struct binKeeper *bk, int start, int end);
/-- Find the lowest overlapping range. Quick even if search range large --/

void binKeeperRemove(struct binKeeper *bk, int start, int end, void *val);
/-- Remove item from binKeeper. --/ 

struct binKeeperCookie binKeeperFirst(struct binKeeper *bk);
/-- Return an object to use by binKeeperNext() to traverse the binElements.
 * The first call to binKeeperNext() will return the first entry in the
 * table. --/

struct binElement* binKeeperNext(struct binKeeperCookie *cookie);
/-- Return the next entry in the binKeeper table.  --/

#endif /-- BINRANGE_H --/



-------------------------------------------------------------
   From kent/src/lib/binRange.c
-------------------------------------------------------------
/-- binRange Stuff to handle binning - which helps us restrict 
 * our attention to the parts of database that contain info
 * about a particular window on a chromosome. This scheme
 * will work without modification for chromosome sizes up
 * to half a gigaBase.  The finest sized bin is 128k (1<<17).
 * The next coarsest is 8x as big (1<<13).  There's a hierarchy
 * of bins with the chromosome itself being the final bin.
 * Features are put in the finest bin they'll fit in. 
 *
 * This file is copyright 2002 Jim Kent, but license is hereby
 * granted for all use - public, private or commercial. --/

#include "common.h"
#include "binRange.h"

static char const rcsid[] = "$Id: binRange.c,v 1.20 2006/07/01 05:01:25 kent Exp $";

/-- add one new level to get coverage past chrom sizes of 512 Mb
 *	effective limit is now the size of an integer since chrom start
 *	and end coordinates are always being used in int's == 2Gb-1 --/
static int binOffsetsExtended[] =
	{4096+512+64+8+1, 512+64+8+1, 64+8+1, 8+1, 1, 0};

static int binOffsets[] = {512+64+8+1, 64+8+1, 8+1, 1, 0};
#define _binFirstShift 17	/-- How much to shift to get to finest bin. --/
#define _binNextShift 3		/-- How much to shift to get to next larger bin. --/

int binLevelsExtended()
/-- Return number of levels to bins. --/
{
return ArraySize(binOffsetsExtended);
}

int binLevels()
/-- Return number of levels to bins. --/
{
return ArraySize(binOffsets);
}

int binFirstShift()
/-- Return amount to shift a number to get to finest bin. --/
{
return _binFirstShift;
}

int binNextShift()
/-- Return amount to shift a number to get to next coarser bin. --/
{
return _binNextShift;
}

int binOffsetExtended(int level)
/-- Return offset for bins of a given level. --/
{
assert(level >= 0 && level < ArraySize(binOffsetsExtended));
return binOffsetsExtended[level] + _binOffsetOldToExtended;
}

int binOffset(int level)
/-- Return offset for bins of a given level. --/
{
assert(level >= 0 && level < ArraySize(binOffsets));
return binOffsets[level];
}

static int binFromRangeStandard(int start, int end)
/-- Given start,end in chromosome coordinates assign it
 * a bin.   There's a bin for each 128k segment, for each
 * 1M segment, for each 8M segment, for each 64M segment,
 * and for each chromosome (which is assumed to be less than
 * 512M.)  A range goes into the smallest bin it will fit in. --/
{
int startBin = start, endBin = end-1, i;
startBin >>= _binFirstShift;
endBin >>= _binFirstShift;
for (i=0; i<ArraySize(binOffsets); ++i)
    {
    if (startBin == endBin)
        return binOffsets[i] + startBin;
    startBin >>= _binNextShift;
    endBin >>= _binNextShift;
    }
errAbort("start %d, end %d out of range in findBin (max is 512M)", start, end);
return 0;
}

static int binFromRangeExtended(int start, int end)
/-- Given start,end in chromosome coordinates assign it
 * a bin.   There's a bin for each 128k segment, for each
 * 1M segment, for each 8M segment, for each 64M segment,
 * for each 512M segment, and one top level bin for 4Gb.
 *	Note, since start and end are int's, the practical limit
 *	is up to 2Gb-1, and thus, only four result bins on the second
 *	level.
 * A range goes into the smallest bin it will fit in. --/
{
int startBin = start, endBin = end-1, i;
startBin >>= _binFirstShift;
endBin >>= _binFirstShift;
for (i=0; i<ArraySize(binOffsetsExtended); ++i)
    {
    if (startBin == endBin)
	return _binOffsetOldToExtended + binOffsetsExtended[i] + startBin;
    startBin >>= _binNextShift;
    endBin >>= _binNextShift;
    }
errAbort("start %d, end %d out of range in findBin (max is 2Gb)", start, end);
return 0;
}

int binFromRange(int start, int end)
/-- return bin that this start-end segment is in --/
{
if (end <= BINRANGE_MAXEND_512M)
    return binFromRangeStandard(start, end);
else
    return binFromRangeExtended(start, end);
}

static int binFromRangeBinKeeperExtended(int start, int end)
/-- This is just like binFromRangeExtended() above, but it doesn't limit
 * the answers to the range from _binOffsetOldToExtended and up.
 *	It simply uses the whole new bin scheme as if it was the only
 *	one.
 --/
{
int startBin = start, endBin = end-1, i;
startBin >>= _binFirstShift;
endBin >>= _binFirstShift;
for (i=0; i<ArraySize(binOffsetsExtended); ++i)
    {
    if (startBin == endBin)
	return binOffsetsExtended[i] + startBin;
    startBin >>= _binNextShift;
    endBin >>= _binNextShift;
    }
errAbort("start %d, end %d out of range in findBin (max is 2Gb)", start, end);
return 0;
}

struct binKeeper *binKeeperNew(int minPos, int maxPos)
/-- Create new binKeeper that can cover range. --/
{
int binCount;
struct binKeeper *bk;
if (minPos < 0 || maxPos < 0 || minPos > maxPos)
    errAbort("bad range %d,%d in binKeeperNew", minPos, maxPos);

binCount = binFromRangeBinKeeperExtended(maxPos-1, maxPos) + 1;
AllocVar(bk);
bk->minPos = minPos;
bk->maxPos = maxPos;
bk->binCount = binCount;
AllocArray(bk->binLists, binCount);
return bk;
}

void binKeeperFree(struct binKeeper **pBk)
/-- Free up a bin keeper. --/
{
struct binKeeper *bk = *pBk;
if (bk != NULL)
    {
    int i;
    for (i=0; i<bk->binCount; ++i)
	slFreeList(&bk->binLists[i]);
    freeMem(bk->binLists);
    freez(pBk);
    }
}

void binKeeperAdd(struct binKeeper *bk, int start, int end, void *val)
/-- Add item to binKeeper. --/ 
{
int bin;
struct binElement *el;
if (start < bk->minPos || end > bk->maxPos || start > end)
    errAbort("(%d %d) out of range (%d %d) in binKeeperAdd", 
    	start, end, bk->minPos, bk->maxPos);
bin = binFromRangeBinKeeperExtended(start, end);
assert(bin < bk->binCount);
AllocVar(el);
el->start = start;
el->end = end;
el->val = val;
slAddHead(&bk->binLists[bin], el);
}

int binElementCmpStart(const void *va, const void *vb)
/-- Compare to sort based on start. --/
{
const struct binElement *a = *((struct binElement **)va);
const struct binElement *b = *((struct binElement **)vb);
return a->start - b->start;
}

struct binElement *binKeeperFind(struct binKeeper *bk, int start, int end)
/-- Return a list of all items in binKeeper that intersect range.
 * Free this list with slFreeList. --/
{
struct binElement *list = NULL, *newEl, *el;
int startBin, endBin;
int i,j;

if (start < bk->minPos) start = bk->minPos;
if (end > bk->maxPos) end = bk->maxPos;
if (start >= end) return NULL;
startBin = (start>>_binFirstShift);
endBin = ((end-1)>>_binFirstShift);
for (i=0; i<ArraySize(binOffsetsExtended); ++i)
    {
    int offset = binOffsetsExtended[i];
    for (j=startBin+offset; j<=endBin+offset; ++j)
        {
	for (el=bk->binLists[j]; el != NULL; el = el->next)
	    {
	    if (rangeIntersection(el->start, el->end, start, end) > 0)
	        {
		newEl = CloneVar(el);
		slAddHead(&list, newEl);
		}
	    }
	}
    startBin >>= _binNextShift;
    endBin >>= _binNextShift;
    }
return list;
}

boolean binKeeperAnyOverlap(struct binKeeper *bk, int start, int end)
/-- Return TRUE if start/end overlaps with any items in binKeeper. --/
{
struct binElement *el;
int startBin, endBin;
int i,j;

if (start < bk->minPos) start = bk->minPos;
if (end > bk->maxPos) end = bk->maxPos;
if (start >= end) return FALSE;
startBin = (start>>_binFirstShift);
endBin = ((end-1)>>_binFirstShift);
for (i=0; i<ArraySize(binOffsetsExtended); ++i)
    {
    int offset = binOffsetsExtended[i];
    for (j=startBin+offset; j<=endBin+offset; ++j)
        {
	for (el=bk->binLists[j]; el != NULL; el = el->next)
	    {
	    if (rangeIntersection(el->start, el->end, start, end) > 0)
	        {
		return TRUE;
		}
	    }
	}
    startBin >>= _binNextShift;
    endBin >>= _binNextShift;
    }
return FALSE;
}

void binKeeperReplaceVal(struct binKeeper *bk, int start, int end,
	void *oldVal, void *newVal)
/-- Replace occurences of old val in range from start->end with newVal --/
{
struct binElement *el;
int startBin, endBin;
int i,j;

if (start < bk->minPos) start = bk->minPos;
if (end > bk->maxPos) end = bk->maxPos;
if (start >= end) return;
startBin = (start>>_binFirstShift);
endBin = ((end-1)>>_binFirstShift);
for (i=0; i<ArraySize(binOffsetsExtended); ++i)
    {
    int offset = binOffsetsExtended[i];
    for (j=startBin+offset; j<=endBin+offset; ++j)
        {
	for (el=bk->binLists[j]; el != NULL; el = el->next)
	    {
	    if (rangeIntersection(el->start, el->end, start, end) > 0)
	        {
		if (el->val == oldVal)
		    {
		    el->val = newVal;
		    }
		}
	    }
	}
    startBin >>= _binNextShift;
    endBin >>= _binNextShift;
    }
}


struct binElement *binKeeperFindSorted(struct binKeeper *bk, int start, int end)
/-- Like binKeeperFind, but sort list on start coordinates. --/
{
struct binElement *list = binKeeperFind(bk, start, end);
slSort(&list, binElementCmpStart);
return list;
}

struct binElement *binKeeperFindAll(struct binKeeper *bk)
/-- Get all elements sorted. --/
{
return binKeeperFindSorted(bk, bk->minPos, bk->maxPos);
}

struct binElement *binKeeperFindLowest(struct binKeeper *bk, int start, int end)
/-- Find the lowest overlapping range. Quick even if search range large --/
{
struct binElement *first = NULL, *el;
int startBin = (start>>_binFirstShift), endBin = ((end-1)>>_binFirstShift);
int i,j;

/-- Search the possible range of bins at each level, looking for lowest.  Once
 * an overlaping range is found at a level, continue with next level, however
 * must search an entire bin as they are not ordered. --/
for (i=0; i<ArraySize(binOffsetsExtended); ++i)
    {
    int offset = binOffsetsExtended[i];
    boolean foundOne = FALSE;
    for (j=startBin+offset; (j<=endBin+offset) && (!foundOne); ++j)
        {
	for (el=bk->binLists[j]; el != NULL; el = el->next)
	    {
            if ((rangeIntersection(el->start, el->end, start, end) > 0)
                && ((first == NULL) || (el->start < first->start)
                    || ((el->start == first->start)
                        && (el->end < first->end))))
                {
                first = el;
                foundOne = TRUE;
		}
	    }
	}
    startBin >>= _binNextShift;
    endBin >>= _binNextShift;
    }
return first;
}


void binKeeperRemove(struct binKeeper *bk, int start, int end, void *val)
/-- Remove item from binKeeper. --/ 
{
int bin = binFromRangeBinKeeperExtended(start, end);
struct binElement **pList = &bk->binLists[bin], *newList = NULL, *el, *next;
for (el = *pList; el != NULL; el = next)
    {
    next = el->next;
    if (el->val == val && el->start == start && el->end == end)
        {
	freeMem(el);
	}
    else
        {
	slAddHead(&newList, el);
	}
    }
slReverse(&newList);
*pList = newList;
}

struct binKeeperCookie binKeeperFirst(struct binKeeper *bk)
/-- Return an object to use by binKeeperNext() to traverse the binElements.
 * The first call to binKeeperNext() will return the first entry in the
 * table. --/
{
struct binKeeperCookie cookie;
cookie.bk = bk;
cookie.blIdx = 0;
cookie.nextBel = bk->binLists[0];
return cookie;
}

struct binElement* binKeeperNext(struct binKeeperCookie *cookie)
/-- Return the next entry in the binKeeper table.  --/
{
/-- if we don't have a next, move down bin list until we find one --/
while ((cookie->nextBel == NULL)
       && (++cookie->blIdx < cookie->bk->binCount))
    cookie->nextBel = cookie->bk->binLists[cookie->blIdx];
if (cookie->blIdx >= cookie->bk->binCount)
    return NULL;  /-- no more --/
else
    {
    struct binElement* bel = cookie->nextBel;
    cookie->nextBel = cookie->nextBel->next;
    return bel;
    }
}

*/