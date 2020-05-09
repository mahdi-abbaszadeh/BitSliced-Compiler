import java.util.ArrayList;

/**
 * Created by Mahdi2016 on 12/7/2018.
 */
public class GToken {
    public enum  GTokenType{
        variable,
        terminal
    }
    private GTokenType gtokenType;
    private String value;
    private ArrayList<GToken> first = new ArrayList<GToken>();
    private ArrayList<GToken> follow = new ArrayList<GToken>();
    private boolean nullable = false;

    public void setGtokenType(GTokenType gtokenType) {this.gtokenType = gtokenType;}
    public void setValue(String value) {this.value = value;}
    public void addFirst(GToken gToken){first.add(gToken);}
    public void addFollow(GToken gToken){follow.add(gToken);}
    public void setNullable(boolean nullable) {this.nullable = nullable;}


    public String getValue() {return value;}
    public GTokenType getGtokenType() {return gtokenType;}
    public ArrayList<GToken> getFirst() {return first;}
    public ArrayList<GToken> getFollow() {return follow;}
    public boolean isNullable() {return nullable;}

}
