import java.util.ArrayList;

/**
 * Created by Mahdi2016 on 1/21/2019.
 */
public class Descriptor {
    private String name;
    private String descType;
    private String varType;
    //private String scope;
    private int address;

    //only for function
    private String codeRegion;
    private String returnType;

    public ArrayList<String> Registers = new ArrayList<String>();

    public String getName() {return name;}
    public void setName(String name) {this.name = name;}

    public String getDescType() {return descType;}
    public void setDescType(String descType) {this.descType = descType;}

    public String getVarType() {return varType;}
    public void setVarType(String varType) {this.varType = varType;}

    //public String getScope() {return scope;}
    //public void setScope(String scope) {this.scope = scope;}

    public int getAddress() {return address;}
    public void setAddress(int address) {this.address = address;}

    public String getCodeRegion() {return codeRegion;}
    public void setCodeRegion(String codeRegion) {this.codeRegion = codeRegion;}

    public String getReturnType() {return returnType;}
    public void setReturnType(String returnType) {this.returnType = returnType;}
}
