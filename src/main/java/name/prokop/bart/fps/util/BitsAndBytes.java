/*
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 * 
 * IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR
 * OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR
 * THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package name.prokop.bart.fps.util;

/**
 *
 * @author Bart
 */
public class BitsAndBytes {

    public static byte parse2hex(char a, char b) {
        int value = Character.digit(a, 16);
        value *= 16;
        value += Character.digit(b, 16);
        return castIntToByte(value);
    }

    public static byte[] parseString(String s) {
        byte[] retVal = new byte[s.length() / 2];
        for (int i = 0; i < s.length(); i += 2) {
            retVal[i / 2] = parse2hex(s.charAt(i), s.charAt(i + 1));
        }
        return retVal;
    }

    public static int buildInt(byte[] a) {
        //System.out.println(ToString.byteArrayToString(a));
        int i = 0;
        if (a.length == 4) {
            i |= promoteByteToInt(a[0]) << 24;
            i |= promoteByteToInt(a[1]) << 16;
            i |= promoteByteToInt(a[2]) << 8;
            i |= promoteByteToInt(a[3]);
            return i;
        }
        if (a.length == 2) {
            i |= promoteByteToInt(a[0]) << 8;
            i |= promoteByteToInt(a[1]);
            return i;
        }
        throw new IllegalArgumentException("a.length should be either 4 or 2.");
    }

    public static int buildInt(byte b3, byte b2, byte b1, byte b0) {
        int i = 0;
        i |= promoteByteToInt(b3) << 24;
        i |= promoteByteToInt(b2) << 16;
        i |= promoteByteToInt(b1) << 8;
        i |= promoteByteToInt(b0);
        return i;
    }

    public static byte castIntToByte(int i) {
        i &= 0xFF;
        return (byte) i;
    }

    public static char castIntToChar(int i) {
        i &= 0xFFFF;
        return (char) i;
    }

    public static byte castLongToByte(long i) {
        i &= 0xFF;
        return (byte) i;
    }

    public static int castLongToInt(long i) {
        i &= 0xFFFFFFFF;
        return (int) i;
    }

    public static byte castCharToByte(char c) {
        return (byte) (c & 0xFF);
    }

    public static long combine(int h, int l) {
        return ((long) h << 32) | l;
    }

    public static byte encodeAsBCD(int a, int b) {
        byte r = castIntToByte(b);
        r += 16 * castIntToByte(a);
        return r;
    }

    /**
     * @param value word with bytes
     * @param idx 0 for eight most right bits, 3 for most left bits
     * @return extracts n-byte from word
     */
    public static byte extractByte(int value, int idx) {
        value = (value >> (8 * idx)) & 0xFF;
        return castIntToByte(value);
    }

    public static byte extractByte(long value, int idx) {
        value = (value >> (8 * idx)) & 0xFF;
        return castLongToByte(value);
    }

    public static void fillBigEndian(byte[] a, int i) {
        a[0] = castIntToByte(i >> 24);
        a[1] = castIntToByte(i >> 16);
        a[2] = castIntToByte(i >> 8);
        a[3] = castIntToByte(i);
    }

    public static void fillLittleEndian(byte[] a, int i) {
        a[0] = castIntToByte(i);
        a[1] = castIntToByte(i >> 8);
        a[2] = castIntToByte(i >> 16);
        a[3] = castIntToByte(i >> 24);
    }

    public static byte[] toBigEndian(int i) {
        byte[] a = new byte[4];
        a[0] = castIntToByte(i >> 24);
        a[1] = castIntToByte(i >> 16);
        a[2] = castIntToByte(i >> 8);
        a[3] = castIntToByte(i);
        return a;
    }

    public static byte[] toLittleEndian(int i) {
        byte[] a = new byte[4];
        a[0] = castIntToByte(i);
        a[1] = castIntToByte(i >> 8);
        a[2] = castIntToByte(i >> 16);
        a[3] = castIntToByte(i >> 24);
        return a;
    }

    public static int high(long l) {
        l = l >> 32;
        l &= 0xFFFFFFFF;
        return (int) l;
    }

    public static int low(long l) {
        l &= 0xFFFFFFFF;
        return (int) l;
    }

    /**
     * Promotes byte b to unsigned 16 bit value (0-255)
     *
     * @param b byte to be converted to short
     * @return byte as positive short
     */
    public static char promoteByteToChar(byte b) {
        return (char) (b & 0xff);
    }

    /**
     * Promotes byte b to unsigned 32 bit value (0-255)
     *
     * @param b byte to be converted to int
     * @return byte as positive int
     */
    public static int promoteByteToInt(byte b) {
        return (int) (b & 0xff);
    }

    /**
     * Promotes byte b to unsigned 64 bit value (0-255)
     *
     * @param b byte to be converted to long
     * @return byte as positive long
     */
    public static long promoteByteToLong(byte b) {
        return (long) (b & 0xff);
    }

    public static int promoteCharToInt(char c) {
        return (int) (c & 0xffff);
    }

    public static void main(String[] args) {
        System.out.println((byte) 0xAA);
        System.out.println(promoteByteToInt((byte) 0xAA));
        System.out.println(castIntToByte(512));
        System.out.println(castIntToByte(255));
        System.out.println(castIntToByte(-1));
    }

    public static byte[] subArray(byte[] array, int startIndex, int endIndex) {
        byte[] retVal = new byte[endIndex - startIndex];
        for (int i = 0; i < retVal.length; i++) {
            retVal[i] = array[startIndex + i];
        }
        return retVal;
    }

    // revised # 1
    public static String byteToHexString(byte b) {
        int i = BitsAndBytes.promoteByteToInt(b);
        String retVal = "";
        switch (i / 16) {
            case 0:
                retVal += "0";
                break;
            case 1:
                retVal += "1";
                break;
            case 2:
                retVal += "2";
                break;
            case 3:
                retVal += "3";
                break;
            case 4:
                retVal += "4";
                break;
            case 5:
                retVal += "5";
                break;
            case 6:
                retVal += "6";
                break;
            case 7:
                retVal += "7";
                break;
            case 8:
                retVal += "8";
                break;
            case 9:
                retVal += "9";
                break;
            case 10:
                retVal += "A";
                break;
            case 11:
                retVal += "B";
                break;
            case 12:
                retVal += "C";
                break;
            case 13:
                retVal += "D";
                break;
            case 14:
                retVal += "E";
                break;
            case 15:
                retVal += "F";
                break;
        }
        switch (i % 16) {
            case 0:
                retVal += "0";
                break;
            case 1:
                retVal += "1";
                break;
            case 2:
                retVal += "2";
                break;
            case 3:
                retVal += "3";
                break;
            case 4:
                retVal += "4";
                break;
            case 5:
                retVal += "5";
                break;
            case 6:
                retVal += "6";
                break;
            case 7:
                retVal += "7";
                break;
            case 8:
                retVal += "8";
                break;
            case 9:
                retVal += "9";
                break;
            case 10:
                retVal += "A";
                break;
            case 11:
                retVal += "B";
                break;
            case 12:
                retVal += "C";
                break;
            case 13:
                retVal += "D";
                break;
            case 14:
                retVal += "E";
                break;
            case 15:
                retVal += "F";
                break;
        }
        return retVal;
    }
}
