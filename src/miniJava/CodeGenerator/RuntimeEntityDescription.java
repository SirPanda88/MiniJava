package miniJava.CodeGenerator;

public class RuntimeEntityDescription {
    public int displacement;

    // offsets are relative, what they are relative to depends on the declaration it is associated with

    // offset relative to LB for local variables and parameter variables
    // offset relative to SB for static fields
    // offset relative to OB for instance variables
    // offset relative to CB for methods

    public RuntimeEntityDescription (int displacement) {
        this.displacement = displacement;
    }
}
