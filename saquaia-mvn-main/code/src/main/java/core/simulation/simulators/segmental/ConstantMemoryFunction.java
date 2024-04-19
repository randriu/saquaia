package core.simulation.simulators.segmental;

/**
 *
 * @author Martin
 */
public class ConstantMemoryFunction extends MemoryFunction{
    public int max;
    
    public ConstantMemoryFunction(int max) {
        super(Type.CONSTANT);
        this.max = max;
    }

    @Override
    public double f(long uses) {
        return max;
    }

    @Override
    public String abreviation() {
        return "c"+max;
    }
    
}
