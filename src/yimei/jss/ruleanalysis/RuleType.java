package yimei.jss.ruleanalysis;


import java.util.HashMap;
import java.util.Map;

/**
 * The type of rule.
 * Created by YiMei on 1/10/16.
 */
public enum RuleType {

    SIMPLE_RULE("simple-rule"),
	//fzhang 17.11.2018  test rule in multi-objective
	MULTIOBJECTIVE_RULE("multiobjective-rule");

    private final String name;

    RuleType(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    // Reverse-lookup map
    private static final Map<String, RuleType> lookup = new HashMap<>();

    static {
        for (RuleType a : RuleType.values()) {
            lookup.put(a.getName(), a);
        }
    }

    public static RuleType get(String name) {
        return lookup.get(name);
    }

    public boolean isMultiobjective() 
    {
        switch (this) {
            case SIMPLE_RULE:
                return false;
            //fzhang 17.11.2018  test rule in multi-objective
            case MULTIOBJECTIVE_RULE:
            	return true;
            default:
                return false;
        }
    }
}
