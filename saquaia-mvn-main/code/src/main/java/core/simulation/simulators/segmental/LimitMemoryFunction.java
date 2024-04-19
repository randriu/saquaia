package core.simulation.simulators.segmental;

/**
 * For an interactive visualization see https://www.geogebra.org/classic/caf9tznk
 * @author Martin
 */
public class LimitMemoryFunction extends MemoryFunction{
    public final double limit, c;
    
    public LimitMemoryFunction(double limit, double c) {
        super(Type.LIMIT);
        this.limit = limit;
        this.c = c;
    }

    @Override
    public double f(long uses) {
        double uses_to_c = Math.pow(uses, c);
        double res = uses_to_c / (uses_to_c * c);
        return res;
    }

    @Override
    public String abreviation() {
        return "lim"+limit+"_"+c;
    }
    
}
