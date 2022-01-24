package custom;

public class Kernel {

    private final String kernel;
    private final String functionName;

    public String getKernel() {
        return kernel;
    }

    public String getFunctionName() {
        return functionName;
    }

    public Kernel(String kernel, String functionName) {
        this.kernel = kernel;
        this.functionName = functionName;
    }
}
