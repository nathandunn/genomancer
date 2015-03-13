package genomancer.ivy.das.model;

public enum Das1Phase {

    PHASE0 ("0"), 
    PHASE1 ("1"), 
    PHASE2 ("2"), 
    NOT_APPLICABLE ("-");

    protected final String label;

    Das1Phase(String str)  {
	this.label = str;
    }

    public String toString()  { return label; }

    public static Das1Phase getPhase(String label)  {
	if (label.equals("-"))   { return NOT_APPLICABLE; }
	else if (label.equals("0"))  { return PHASE0; }
	else if (label.equals("1"))  { return PHASE1; }
	else if (label.equals("2"))  { return PHASE2; }
	else { return null; }
    }

 }
