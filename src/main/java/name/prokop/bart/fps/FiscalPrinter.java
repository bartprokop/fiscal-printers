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
 * Created on 2 kwiecień 2006, 11:09
 */
package name.prokop.bart.fps;

import java.lang.reflect.Method;
import name.prokop.bart.fps.datamodel.Invoice;
import name.prokop.bart.fps.datamodel.Slip;
import name.prokop.bart.fps.drivers.ConsoleDump;
import name.prokop.bart.fps.drivers.DFEmul;
import name.prokop.bart.fps.drivers.ElzabMera;
import name.prokop.bart.fps.drivers.ElzabOmega2;
import name.prokop.bart.fps.drivers.InnovaProfit451;
import name.prokop.bart.fps.drivers.OptimusVivo;
import name.prokop.bart.fps.drivers.Posnet101;
import name.prokop.bart.fps.drivers.Thermal101;
import name.prokop.bart.fps.drivers.Thermal203;
import name.prokop.bart.fps.drivers.Thermal301;
import name.prokop.bart.fps.drivers.ThermalOld;

/**
 *
 * @author Karo
 */
public interface FiscalPrinter {

    public static enum Type {

        Console(ConsoleDump.class, "Console"),
        DFEmul(DFEmul.class, "Emulator"),
        ElzabMera(ElzabMera.class, "Elzab Mera"),
        ElzabOmega2(ElzabOmega2.class, "Elzab Omega 2"),
        InnovaProfit451(InnovaProfit451.class, "Innova Profit 4.51"),
        OptimusVivo(OptimusVivo.class, "Optimus VIVO"),
        Posnet101(Posnet101.class, "Posnet nowy protokół"),
        Thermal101(Thermal101.class, "Posnet Thermal 1.01"),
        Thermal203(Thermal203.class, "Posnet Thermal 2.03"),
        Thermal301(Thermal301.class, "Posnet Thermal 3.01"),
        ThermalOld(ThermalOld.class, "Thermal stara homologacja");

        private final Class<?> driverClass;
        private final String friendlyName;

        private Type(Class<?> driverClass, String friendlyName) {
            this.driverClass = driverClass;
            this.friendlyName = friendlyName;
        }

        public FiscalPrinter getFiscalPrinter(String comPort) throws FiscalPrinterException {
            try {
                Method method = this.driverClass.getMethod("getFiscalPrinter", String.class);
                return (FiscalPrinter) method.invoke(null, comPort);
            } catch (Exception e) {
                throw new FiscalPrinterException(e);
            }
        }

        @Override
        public String toString() {
            return friendlyName;
        }
    }

    /**
     * Prints a slip
     *
     * @param slip - encapsulated slip data
     * @throws name.prokop.bart.fps.FiscalPrinterException
     */
    public void print(Slip slip) throws FiscalPrinterException;

    public void print(Invoice invoice) throws FiscalPrinterException;

    /**
     * Opens money drawer
     *
     * @throws name.prokop.bart.fps.FiscalPrinterException
     */
    public void openDrawer() throws FiscalPrinterException;

    /**
     * Print daily fiscal report
     *
     * @throws FiscalPrinterException
     */
    public void printDailyReport() throws FiscalPrinterException;
}
