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

import name.prokop.bart.fps.FiscalPrinterException;
import name.prokop.bart.fps.datamodel.SlipExamples;
import name.prokop.bart.fps.drivers.ElzabMera;

/**
 *
 * @author bart
 */
public class HeavyTesting {

    public static void main(String[] args) {
        //Thermal301 fp = (Thermal301) Thermal301.getFiscalPrinter("COM1");
        //Posnet101 fp = (Posnet101) Posnet101.getFiscalPrinter("COM1");
        ElzabMera fp = (ElzabMera) ElzabMera.getFiscalPrinter("COM1");

        for (int i = 0; i < 50; i++) {
            try {
                //fp.print(Slip.getTestSlip());
                fp.print(SlipExamples.getSampleSlip());
                fp.openDrawer();
                System.out.println("Wydruk OK");
            } catch (FiscalPrinterException e) {
                e.printStackTrace();
            }
        }
    }
}
