package core.simulation.simulators.segmental;

/**
 *
 * @author Martin
 */
public class LinearMemoryFunction extends MemoryFunction{
    public double a;
    
    public LinearMemoryFunction(double a) {
        super(Type.LINEAR);
        this.a = a;
    }

    @Override
    public double f(long uses) {
        return a * uses;
    }

    @Override
    public String abreviation() {
        return "lin"+a;
    }
    
}
