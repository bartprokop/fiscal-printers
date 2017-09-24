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
 * PosnetThermal101.java
 * wg. dokumentu: SPECYFIKACJA PROTOKOLU POSNET w Thermal FV EJ 1.01
 * sygnatura: DBC-I-DEV-45-012_specyfikacja_protokolu_Posnet_w_drukarkach.pdf
 *
 * Created on 22 styczeń 2005, 21:58
 */
package name.prokop.bart.fps.drivers;

import gnu.io.SerialPort;
import gnu.io.UnsupportedCommOperationException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import name.prokop.bart.fps.FiscalPrinter;
import name.prokop.bart.fps.FiscalPrinterException;
import name.prokop.bart.fps.datamodel.Invoice;
import name.prokop.bart.fps.datamodel.Slip;
import name.prokop.bart.fps.datamodel.SlipExamples;
import name.prokop.bart.fps.util.PortEnumerator;

/**
 * Klasa implementująca obsługę drukarki fiskalnej POSNET THERMAL z protokołem w
 * wersji 1.01 wg. dokumentu: SPECYFIKACJA PROTOKOLU POSNET w Thermal FV EJ 1.01
 * sygnatura: DBC-I-DEV-45-012_specyfikacja_protokolu_Posnet_w_drukarkach.pdf
 *
 * @author Bartłomiej Piotr Prokop
 */
public class Posnet101 implements FiscalPrinter {

    private String comPortName;
    private SerialPort serialPort;
    private final String footerLine1;
    private final String footerLine2;
    private final String footerLine3;

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        Posnet101 fp;

        if (args.length != 0) {
            fp = (Posnet101) Posnet101.getFiscalPrinter(args[0]);
        } else {
            fp = (Posnet101) Posnet101.getFiscalPrinter("COM1");
        }

        try {
            fp.print(SlipExamples.getOneCentSlip());
            //fp.print(Slip.getSampleSlip());
            //fp.print(Invoice.getTestInvoice());
            fp.openDrawer();
            //fp.printDailyReport();
        } catch (FiscalPrinterException e) {
            System.err.println(e);
        }
    }

    /**
     * Creates a new instance of PosnetThermal101
     */
    public static FiscalPrinter getFiscalPrinter(String comPortName) {
        return new Posnet101(comPortName);
    }

    /**
     * Tworzy obiekt zdolny do wymuszenia na drukarce fiskalnej Posnet 1.01
     * wydruku paragonu fiskalnego, zapisanego w klasie Slip
     *
     * @param comPortName Nazwa portu szeregowego, do którego jest przyłączona
     * drukarka fiskalna.
     */
    private Posnet101(String comPortName) {
        this.comPortName = comPortName;
        footerLine1 = "&b&c&hDziękujemy";
        footerLine2 = "&c&bZapraszamy ponownie";
        footerLine3 = "&i&cPosnet 1.01";
    }

    /**
     * Służy do wydrukowania paragonu fiskalnego
     *
     * @param slip paragon do wydrukowania
     * @throws name.prokop.bart.fps.FiscalPrinterException w
     * przypadku niepowodzenia, wraz z opisem błędu
     */
    @Override
    public synchronized void print(Slip slip) throws FiscalPrinterException {
        slip = SlipExamples.demo(slip);
        try {
            connect();
            Posnet101Driver driver = new Posnet101Driver(getInputStream(), getOutputStream());
            driver.setFooterLine1(footerLine1);
            driver.setFooterLine2(footerLine2);
            driver.setFooterLine3(footerLine3);
            driver.printSlip(slip);
        } catch (IOException ioe) {
            throw new FiscalPrinterException(ioe);
        } finally {
            disconnect();
        }
    }

    @Override
    public void print(Invoice invoice) throws FiscalPrinterException {
        try {
            connect();
            Posnet101Driver driver = new Posnet101Driver(getInputStream(), getOutputStream());
            driver.setFooterLine1(footerLine1);
            driver.setFooterLine2(footerLine2);
            driver.setFooterLine3(footerLine3);
            driver.printInvoice(invoice);
        } catch (IOException ioe) {
            throw new FiscalPrinterException(ioe);
        } finally {
            disconnect();
        }
    }

    @Override
    public synchronized void openDrawer() throws FiscalPrinterException {
        try {
            connect();
            Posnet101Driver driver = new Posnet101Driver(getInputStream(), getOutputStream());
            driver.openDrawer();
        } catch (IOException ioe) {
            throw new FiscalPrinterException(ioe);
        } finally {
            disconnect();
        }
    }

    @Override
    public void printDailyReport() throws FiscalPrinterException {
        try {
            connect();
            Posnet101Driver driver = new Posnet101Driver(getInputStream(), getOutputStream());
            driver.printDailyReport();
        } catch (IOException ioe) {
            throw new FiscalPrinterException(ioe);
        } finally {
            disconnect();
        }
    }

    private InputStream getInputStream() throws IOException {
        return serialPort.getInputStream();
    }

    private OutputStream getOutputStream() throws IOException {
        return serialPort.getOutputStream();
    }

    private void connect() throws FiscalPrinterException {
        try {
            serialPort = PortEnumerator.getSerialPort(comPortName);
        } catch (Exception e) {
            throw new FiscalPrinterException("Nie można otworzyć portu: " + e.getMessage());
        }

        try {
            serialPort.setSerialPortParams(9600, SerialPort.DATABITS_8, SerialPort.STOPBITS_1, SerialPort.PARITY_NONE);
            serialPort.setFlowControlMode(SerialPort.FLOWCONTROL_XONXOFF_OUT | SerialPort.FLOWCONTROL_XONXOFF_IN);
        } catch (UnsupportedCommOperationException e) {
            throw new FiscalPrinterException("Nie można otworzyć portu: UnsupportedCommOperationException: " + e.getMessage());
        }
    }

    private void disconnect() {
        if (serialPort != null) {
            serialPort.close();
        }
    }
}
