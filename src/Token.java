/**
 * Created by Mahdi2016 on 12/7/2018.
 */
public class Token {
    public enum  TokenType{
        id,
        num,
        st,
        string,
        character,
        comment,
        keyword
    }
    private TokenType tokenType;
    private String value;

    public void setTokenType(TokenType tokenType) {this.tokenType = tokenType;}
    public void setValue(String value) {this.value = value;}

    public String getValue() {return value;}
    public TokenType getTokenType() {return tokenType;}
}
