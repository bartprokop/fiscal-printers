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
 * NewException.java
 *
 * Created on 28 marzec 2006, 09:49
 */
package name.prokop.bart.fps;

import java.io.IOException;

/**
 *
 * @author bart
 */
public class FiscalPrinterException extends IOException {

    /**
     * Constructs an instance of
     * <code>NewException</code> with the specified detail message.
     *
     * @param msg the detail message.
     */
    public FiscalPrinterException(String msg) {
        super(msg);
    }

    public FiscalPrinterException(Throwable t) {
        super(t);
    }
}
