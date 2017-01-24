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

import java.util.Random;

/**
 *
 * @author Bart
 */
public class StringGenerator {
    
    public static String generateRandomStringId(int length) {
        char [] charArr = new char[length];
        Random rnd = new Random();
        for (int i=0; i<length; i++)
            charArr[i] = (char) (65+rnd.nextInt(26));
        return new String(charArr);
    }

    public static String generateRandomNumericId(int length) {
        char [] charArr = new char[length];
        Random rnd = new Random();
        for (int i=0; i<length; i++)
            charArr[i] = (char) ('0'+rnd.nextInt(10));
        return new String(charArr);
    }
    
}
