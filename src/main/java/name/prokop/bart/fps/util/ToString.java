/*
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 * 
 * IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR
 * OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR
 * THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 * 
 * ToString.java
 *
 * Created on 2 maj 2004, 19:01
 */
package name.prokop.bart.fps.util;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.text.DateFormat;
import java.text.NumberFormat;
import java.util.Date;
import java.util.List;
import java.util.Set;

/**
 *
 * @author Bart
 */
public class ToString {

    public static String debugString(String s) {
        StringBuilder sb = new StringBuilder();
        for (Character c : s.toCharArray()) {
            if (c < 32 || c >= 128) {
                sb.append(" 0x").append(Integer.toHexString(c)).append("h ");
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    public static String toString(Set<? extends Object> set) {
        StringBuilder sb = new StringBuilder();
        for (Object o : set) {
            sb.append(o);
            sb.append(", ");
        }
        if (sb.length() > 2) {
            return "[" + sb.substring(0, sb.length() - 2) + "]";
        } else {
            return "[]";
        }
    }

    public static String url2String(String url) throws IOException {
        String result = "";
        URL server = new URL(url);
        InputStreamReader isr = new InputStreamReader(server.openStream(), "UTF-8");
        StringBuffer b = new StringBuffer();
        char[] c = new char[100];
        int l;
        do {
            l = isr.read(c);
            if (l != -1) {
                b.append(c, 0, l);
            }
        } while (l != -1);
        isr.close();
        return b.toString();
    }

    public static String listOfStrings2String(List<String> listOfStrings) {
        StringBuffer b = new StringBuffer();
        for (int i = 0; i < listOfStrings.size(); i++) {
            b.append(listOfStrings.get(i) + '\n');
        }
        return b.toString();
    }

    public static String byteArrayToString(byte[] a) {
        return byteArrayToString(a, 0, a.length);
    }

    public static String byteArrayToString(byte[] a, int from, int to) {
        String retVal = "";
        for (int i = from; i < to; i++) {
            retVal += BitsAndBytes.byteToHexString(a[i]) + " ";
        }
        return "[ " + retVal + "]";
    }

    public static String intArrayToString(int[] a) {
        String retVal = "";
        for (int i = 0; i < a.length; i++) {
            retVal += Integer.toString(a[i]) + " ";
        }
        return "[ " + retVal + "]";
    }

    public static String arrayToString(Object[] a) {
        StringBuilder retVal = new StringBuilder();
        for (int i = 0; i < a.length; i++) {
            retVal.append(a[i].toString() + ", ");
        }
        return retVal.delete(retVal.length() - 2, retVal.length()).toString();
    }

    public static String byteToIntString(byte b) {
        String s = "" + ((b < 0) ? (256 + b) : (b));
        while (s.length() < 3) {
            s = "0" + s;
        }
        return s;
    }

    public static String double2String(double val) {
        NumberFormat nf = NumberFormat.getNumberInstance();
        nf.setMinimumFractionDigits(2);
        String retVal = nf.format(val);
        return retVal;
    }

    public static String date2String(Date val) {
        DateFormat df = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT);
        String retVal = df.format(val);
        return retVal;
    }

    /**
     * Standard Ą Ć Ę Ł Ń Ó Ś Ź Ż ą ć ę ł ń ó ś ź ż Mazovia 143 149 144 156 165
     * 163 152 160 161 134 141 145 146 164 162 158 166 167
     *
     * @param s String value to convert from UNICODE to Mazovia
     * @return table of characters encoded in MAZOVIA.
     */
    public static byte[] string2Mazovia(String s) {
        byte[] retVal = new byte[s.length()];
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case 'Ą':
                    retVal[i] = (byte) 143;
                    break;

                case 'Ć':
                    retVal[i] = (byte) 149;
                    break;

                case 'Ę':
                    retVal[i] = (byte) 144;
                    break;

                case 'Ł':
                    retVal[i] = (byte) 156;
                    break;

                case 'Ń':
                    retVal[i] = (byte) 165;
                    break;

                case 'Ó':
                    retVal[i] = (byte) 163;
                    break;

                case 'Ś':
                    retVal[i] = (byte) 152;
                    break;

                case 'Ź':
                    retVal[i] = (byte) 160;
                    break;

                case 'Ż':
                    retVal[i] = (byte) 161;
                    break;

                case 'ą':
                    retVal[i] = (byte) 134;
                    break;

                case 'ć':
                    retVal[i] = (byte) 141;
                    break;

                case 'ę':
                    retVal[i] = (byte) 145;
                    break;

                case 'ł':
                    retVal[i] = (byte) 146;
                    break;

                case 'ń':
                    retVal[i] = (byte) 164;
                    break;

                case 'ó':
                    retVal[i] = (byte) 162;
                    break;

                case 'ś':
                    retVal[i] = (byte) 158;
                    break;

                case 'ź':
                    retVal[i] = (byte) 166;
                    break;

                case 'ż':
                    retVal[i] = (byte) 167;
                    break;

                default:
                    retVal[i] = (byte) c;
            }
        }
        return retVal;
    }

    public static String string2noAccents(String s) {
        String retVal = "";
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case 'Ą':
                    retVal += 'A';
                    break;

                case 'Ć':
                    retVal += 'C';
                    break;

                case 'Ę':
                    retVal += 'E';
                    break;

                case 'Ł':
                    retVal += 'L';
                    break;

                case 'Ń':
                    retVal += 'N';
                    break;

                case 'Ó':
                    retVal += 'O';
                    break;

                case 'Ś':
                    retVal += 'S';
                    break;

                case 'Ź':
                    retVal += 'Z';
                    break;

                case 'Ż':
                    retVal += 'Z';
                    break;

                case 'ą':
                    retVal += 'a';
                    break;

                case 'ć':
                    retVal += 'c';
                    break;

                case 'ę':
                    retVal += 'e';
                    break;

                case 'ł':
                    retVal += 'l';
                    break;

                case 'ń':
                    retVal += 'n';
                    break;

                case 'ó':
                    retVal += 'o';
                    break;

                case 'ś':
                    retVal += 's';
                    break;

                case 'ź':
                    retVal += 'z';
                    break;

                case 'ż':
                    retVal += 'z';
                    break;

                /**
                 * Umlauty
                 */
                case 'ü':
                    retVal += 'u';
                    break;

                default:
                    retVal += c;
            }
        }
        return retVal;
    }
}
