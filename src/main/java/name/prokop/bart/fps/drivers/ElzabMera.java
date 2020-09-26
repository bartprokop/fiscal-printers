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
package name.prokop.bart.fps.drivers;

import gnu.io.SerialPort;
import gnu.io.UnsupportedCommOperationException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import name.prokop.bart.fps.FiscalPrinter;
import name.prokop.bart.fps.FiscalPrinterException;
import name.prokop.bart.fps.datamodel.DiscountType;
import name.prokop.bart.fps.datamodel.Invoice;
import name.prokop.bart.fps.datamodel.SaleLine;
import name.prokop.bart.fps.datamodel.Slip;
import name.prokop.bart.fps.datamodel.SlipExamples;
import name.prokop.bart.fps.datamodel.SlipPayment;
import name.prokop.bart.fps.datamodel.VATRate;
import name.prokop.bart.fps.util.BitsAndBytes;
import name.prokop.bart.fps.util.PortEnumerator;
import name.prokop.bart.fps.util.ToString;

/**
 * Sterownik do drukarki ELZAB MERA
 *
 * @author Bart
 */
public class ElzabMera implements FiscalPrinter {

    /**
     * @param args the command line arguments
     * @throws Exception any exception
     */
    public static void main(String[] args) throws Exception {
        FiscalPrinter fp;

        if (args.length != 0) {
            fp = ElzabMera.getFiscalPrinter(args[0]);
        } else {
            fp = ElzabMera.getFiscalPrinter("COM1");
        }

        try {
            fp.print(SlipExamples.getOneCentSlip());
            //fp.print(Slip.getSampleSlip());
            //fp.print(Slip.getTestNoDiscountSlip());
            //fp.print(Slip.getOneCentSlip());
            //fp.openDrawer();
            //fp.printDailyReport();
        } catch (FiscalPrinterException e) {
            System.err.println(e);
        }
    }

    public static FiscalPrinter getFiscalPrinter(String comPortName) {
        return new ElzabMera(comPortName);
    }
    private String comPortName;
    private SerialPort serialPort = null;

    private ElzabMera(String comPortName) {
        this.comPortName = comPortName;
    }

    @Override
    public void print(Slip slip) throws FiscalPrinterException {
        slip = SlipExamples.demo(slip);

        try {
            prepareSerialPort();

            while (!checkPrinter()) {
                try {
                    Thread.sleep(2000);
                    System.err.println("Drukarka niegotowa");
                } catch (InterruptedException e) {
                }
            }

            readRates();

            sendToPrinter(new byte[]{0x1B, 0x21}); // OTWARCIE PARAGONU
            waitForAck();

            for (SaleLine line : slip.getSlipLines()) {
                printLine(line);
                if (line.getDiscountType() != DiscountType.NoDiscount) {
                    printDiscount(line);
                }
            }

            sendToPrinter(new byte[]{0x1B, 0x07}); // Koniec wszystkich pozycji sprzedaży
            int total = (int) (100 * slip.getTotal() + 0.000001);
            sendToPrinter(prepareInteger(total));

            // ***
            sendToPrinter(new byte[]{0x1B, 0x09});
            sendToPrinter(new byte[]{0x3C});
            sendToPrinter(slip.getReference().getBytes());
            sendToPrinter(new byte[]{0x0A});
            // ***
            // ***
            sendToPrinter(new byte[]{0x1B, 0x09});
            sendToPrinter(new byte[]{0x35});
            sendToPrinter(slip.getCashierName().getBytes());
            sendToPrinter(new byte[]{0x0A});
            // ***

            sendToPrinter(new byte[]{0x1B, 0x24}); // zakończenie paragonu
            waitForAck();
        } finally {
            if (serialPort != null) {
                serialPort.close();
            }
        }
    }

    public void print(Invoice invoice) throws FiscalPrinterException {
        throw new FiscalPrinterException(new UnsupportedOperationException("Not supported yet."));
    }

    @Override
    public void openDrawer() throws FiscalPrinterException {
        try {
            prepareSerialPort();
            sendToPrinter(new byte[]{0x1B, 0x57});
            waitForAck();
        } finally {
            if (serialPort != null) {
                serialPort.close();
            }
        }
    }

    @Override
    public void printDailyReport() throws FiscalPrinterException {
        try {
            prepareSerialPort();
            sendToPrinter(new byte[]{0x1B, 0x25});
            waitForAck();
        } finally {
            if (serialPort != null) {
                serialPort.close();
            }
        }
    }

    private void prepareSerialPort() throws FiscalPrinterException {
        try {
            serialPort = PortEnumerator.getSerialPort(comPortName);
        } catch (Exception e) {
            throw new FiscalPrinterException("Nie można otworzyć portu: " + e.getMessage());
        }

        try {
            serialPort.setSerialPortParams(9600, SerialPort.DATABITS_8, SerialPort.STOPBITS_1, SerialPort.PARITY_EVEN);
            serialPort.setFlowControlMode(SerialPort.FLOWCONTROL_RTSCTS_OUT);
        } catch (UnsupportedCommOperationException e) {
            throw new FiscalPrinterException("Nie można otworzyć portu: UnsupportedCommOperationException: " + e.getMessage());
        }
    }

    private void sendToPrinter(byte[] data) throws FiscalPrinterException {
        int timeout = 5000;
        try {
            for (byte b : data) {
                int bb = BitsAndBytes.promoteByteToInt(b);
                while (!serialPort.isCTS()) {
                    try {
                        Thread.sleep(1);
                        timeout--;
                    } catch (InterruptedException iex) {
                    }
                    if (timeout == 0) {
                        throw new FiscalPrinterException("CTS is OFF - timeout");
                    }
                }
                serialPort.getOutputStream().write(bb);
            }
        } catch (IOException e) {
            throw new FiscalPrinterException("Błąd transmisji: " + e.getMessage());
        }

    }

    private void waitForAck() throws FiscalPrinterException {
        try {
            InputStream inputStream = serialPort.getInputStream();
            long timeOut = 10 * 1000;
            while (timeOut-- > 0) {
                try {
                    Thread.sleep(1);
                } catch (InterruptedException ex) {
                }
                if (inputStream.available() == 0) {
                    continue;
                }
                if (inputStream.read() == 0x06) {
                    return;
                }
                throw new FiscalPrinterException("NAK received");
            }
            throw new FiscalPrinterException("ACK Timeout");
        } catch (IOException e) {
            throw new FiscalPrinterException("Błąd transmisji: " + e.getMessage());
        }
    }
    private double rateA = 0.22;
    private double rateB = 0.07;
    private double rateC = 0.00;
    private double rateD = 0.03;
    private double rateE = 0x8000 / 10000.0;
    private double rateF = 0x8000 / 10000.0;
    private double rateG = 0x4000 / 10000.0;
    private static double TAX_RATE_ZW = 0x4000 / 10000.0;

    private void readRates() throws FiscalPrinterException {
        sendToPrinter(new byte[]{0x1B, (byte) 0xd1});
        waitForAck();
        try {
            InputStream inputStream = serialPort.getInputStream();
            long timeOut = 10 * 1000;
            while (timeOut-- > 0) {
                try {
                    Thread.sleep(1);
                } catch (InterruptedException ex) {
                }
                if (inputStream.available() != 14) {
                    continue;
                }
                byte[] rates = new byte[14];
                inputStream.read(rates);
                System.out.println(ToString.byteArrayToString(rates));
                rateA = Integer.parseInt(BitsAndBytes.byteToHexString(rates[0]) + BitsAndBytes.byteToHexString(rates[1]), 16) / 10000.0;
                rateB = Integer.parseInt(BitsAndBytes.byteToHexString(rates[2]) + BitsAndBytes.byteToHexString(rates[3]), 16) / 10000.0;
                rateC = Integer.parseInt(BitsAndBytes.byteToHexString(rates[4]) + BitsAndBytes.byteToHexString(rates[5]), 16) / 10000.0;
                rateD = Integer.parseInt(BitsAndBytes.byteToHexString(rates[6]) + BitsAndBytes.byteToHexString(rates[7]), 16) / 10000.0;
                rateE = Integer.parseInt(BitsAndBytes.byteToHexString(rates[8]) + BitsAndBytes.byteToHexString(rates[9]), 16) / 10000.0;
                rateF = Integer.parseInt(BitsAndBytes.byteToHexString(rates[10]) + BitsAndBytes.byteToHexString(rates[11]), 16) / 10000.0;
                rateG = Integer.parseInt(BitsAndBytes.byteToHexString(rates[12]) + BitsAndBytes.byteToHexString(rates[13]), 16) / 10000.0;
                System.out.println(rateA + ", " + rateB + ", " + rateC + ", " + rateD + ", " + rateE + ", " + rateF + ", " + rateG + ".");
                return;
            }
            throw new FiscalPrinterException("Rates timeout");
        } catch (IOException e) {
            throw new FiscalPrinterException("Błąd transmisji: " + e.getMessage());
        }
    }

    /**
     * Sprawdzenie gotowości do pracy
     *
     * @return drukarka gotowa do wydruku paragonu
     * @throws FiscalPrinterException
     */
    private boolean checkPrinter() throws FiscalPrinterException {
        boolean retVal = true;
        int byte0, byte1, byte2, byte3, byte4;

        sendToPrinter(new byte[]{0x1B, 0x5B});
        waitForAck();
        try {
            Thread.sleep(10);
            byte0 = serialPort.getInputStream().read();
        } catch (Exception e) {
            throw new FiscalPrinterException(comPortName);
        }

        sendToPrinter(new byte[]{0x1B, 0x54});
        waitForAck();
        try {
            Thread.sleep(10);
            byte1 = serialPort.getInputStream().read();
        } catch (Exception e) {
            throw new FiscalPrinterException(comPortName);
        }

        sendToPrinter(new byte[]{0x1B, 0x55});
        waitForAck();
        try {
            Thread.sleep(10);
            byte2 = serialPort.getInputStream().read();
        } catch (Exception e) {
            throw new FiscalPrinterException(comPortName);
        }

        sendToPrinter(new byte[]{0x1B, 0x56});
        waitForAck();
        try {
            Thread.sleep(10);
            byte3 = serialPort.getInputStream().read();
        } catch (Exception e) {
            throw new FiscalPrinterException(comPortName);
        }

        sendToPrinter(new byte[]{0x1B, 0x5f});
        waitForAck();
        try {
            Thread.sleep(10);
            byte4 = serialPort.getInputStream().read();
        } catch (Exception e) {
            throw new FiscalPrinterException(comPortName);
        }

        System.out.println(Integer.toHexString(byte0) + ":" + Integer.toHexString(byte1) + ":" + Integer.toHexString(byte2) + ":" + Integer.toHexString(byte3) + ":" + Integer.toHexString(byte4));

        if ((byte1 & 1) == 1) {
            System.out.println("brak wolnego miejsca w bazie kontrolnej nazw i stawek");
        }
        if ((byte1 & 2) == 2) {
            System.out.println("w pamięci znajduje się dokument do wydrukowania");
            retVal = false;
        }
        if ((byte1 & 4) == 4) {
            System.out.println("w pamięci fiskalnej zostało mniej niż 30 rekordów do zapisania");
        }
        if ((byte1 & 8) == 8) {
            System.out.println("nie został wykonany raport dobowy za poprzedni dzień sprzedaży (nie można wystawić paragonu fiskalnego)");
        }
        if ((byte1 & 16) == 16) {
            System.out.println("błąd w pamięci RAM (RAM był kasowany)");
        }
        if ((byte1 & 32) == 32) {
            System.out.println("nastąpiło zablokowanie nazwy towaru w paragonie");
        }
        if ((byte1 & 64) == 64) {
            System.out.println("brak wyświetlacza klienta");
        }
        if ((byte1 & 128) == 128) {
            System.out.println("brak komunikacji z kontrolerem drukarki");
        }

        if ((byte2 & 1) == 1) {
            System.out.println("w buforze drukowania są znaki do wydrukowania");
            retVal = false;
        }
        if ((byte2 & 2) == 2) {
            System.out.println("brak papieru lub podniesiona głowica");
        }
        if ((byte2 & 4) == 4) {
            System.out.println("awaria drukarki");
        }
        if ((byte2 & 8) == 8) {
            System.out.println("za niskie napięcie akumulatora / baterii podtrzymującej, dalsza praca możliwa ale powiadom serwis");
        }
        if ((byte2 & 16) == 16) {
            System.out.println("nastąpiło unieważnienie paragonu");
        }
        if ((byte2 & 32) == 32) {
            System.out.println("w pamięci paragonu pozostało mniej niż 1 kB miejsca");
        }
        if ((byte2 & 64) == 64) {
            System.out.println("wydruk dokumentu zatrzymany z powodu braku papieru");
        }
        if ((byte2 & 128) == 128) {
            System.out.println("brak komunikacji z kontrolerem drukarki");
        }

        if ((byte2 & 2) == 2) {
            throw new FiscalPrinterException("brak papieru lub podniesiona głowica");
        }

        if ((byte1 & 8) == 8) {
            throw new FiscalPrinterException("nie został wykonany raport dobowy za poprzedni dzień sprzedaży (nie można wystawić paragonu fiskalnego)");
        }

        if (((byte2 & 64) == 64)) {
            sendToPrinter(new byte[]{0x1B, 0x2a});
            waitForAck();
            return false;
        }

        return retVal;
    }

    private byte[] prepareName(String name) {
        //System.err.println(name + " : " + name.length());
        name = name.trim();
        //System.err.println(name + " : " + name.length());
        if (name.length() > 28) {
            name = name.substring(0, 28);
        }
        //System.err.println(name + " : " + name.length());
        while (name.length() < 5) {
            name += '.';
        }
        //System.err.println(name + " : " + name.length());
        while (name.length() < 28) {
            name += ' ';
        }
        //System.err.println(name + " : " + name.length());
        try {
            return name.getBytes("cp1250");
        } catch (UnsupportedEncodingException e) {
            return name.getBytes();
        }
    }

    private byte[] prepareInteger(int i) {
        byte[] retVal = new byte[4];

        retVal[0] = (byte) ((i >> 0) & 0xFF);
        retVal[1] = (byte) ((i >> 8) & 0xFF);
        retVal[2] = (byte) ((i >> 16) & 0xFF);
        retVal[3] = (byte) ((i >> 24) & 0xFF);

        return retVal;
    }

    private static int countAmountPrecision(double d) throws FiscalPrinterException {
        if (Toolbox.round(d, 4) != d) {
            throw new FiscalPrinterException("Reduce amount precision");
        }
        if (Toolbox.round(d, 0) == d) {
            return 0;
        }
        if (Toolbox.round(d, 1) == d) {
            return 1;
        }
        if (Toolbox.round(d, 2) == d) {
            return 2;
        }
        if (Toolbox.round(d, 3) == d) {
            return 3;
        }
        if (Toolbox.round(d, 4) == d) {
            return 4;
        }
        return 0;
    }

    private byte findRate(VATRate rate) throws FiscalPrinterException {
        if (rate == VATRate.VATzw) {
            return 5; // G
        }
        if (rate.getVatRate() == rateA) {
            return 1;
        }
        if (rate.getVatRate() == rateB) {
            return 2;
        }
        if (rate.getVatRate() == rateC) {
            return 3;
        }
        if (rate.getVatRate() == rateD) {
            return 4;
        }
        if (rate.getVatRate() == rateE) {
            return 6;
        }
        if (rate.getVatRate() == rateF) {
            return 7;
        }
        throw new FiscalPrinterException("Niezdefiniowna stawka: " + rate);
    }

    /**
     * Wysyła linijkę paragonu do drukarki
     *
     * @param slipLine linijka paragonu
     * @throws FiscalPrinterException
     */
    private void printLine(SaleLine slipLine) throws FiscalPrinterException {
        double amount = slipLine.getAmount();
        int amountPrecision = countAmountPrecision(amount);
        int amountInteger = (int) (amount * Math.pow(10, amountPrecision) + 0.000001);
        int price = (int) (100 * slipLine.getPrice() + 0.000001);
        int total = (int) (100 * slipLine.getGross() + 0.000001);

        //System.out.println(amount + " / " + amountPrecision + " " + amountInteger);
        sendToPrinter(new byte[]{0x1B, 0x06, 0x20}); // Esc,06H,20H
        sendToPrinter(prepareName(slipLine.getName())); // K1,....K28
        sendToPrinter(new byte[]{0x00}); // A1
        sendToPrinter(prepareInteger(amountInteger)); // I1,I2,I3,I4
        sendToPrinter(new byte[]{(byte) amountPrecision}); // M
        sendToPrinter("szt.".getBytes()); // J1,J2,J3,J4
        sendToPrinter(prepareInteger(price)); // C1,C2,C3,C4
        sendToPrinter(new byte[]{0x1B}); // Esc
        sendToPrinter(new byte[]{findRate(slipLine.getTaxRate())}); // stawka VAT
        sendToPrinter(prepareInteger(total)); // W1,W2,W3,W4
    }

    private void printDiscount(SaleLine slipLine) throws FiscalPrinterException {
        int discount;
        switch (slipLine.getDiscountType()) {
            case AmountDiscount:
                discount = (int) (100 * slipLine.getDiscount() + 0.000001);
                sendToPrinter(new byte[]{0x1B, 0x18, 0x2d}); // Esc, 2DH
                break;
            case AmountExtra:
                discount = (int) (100 * slipLine.getDiscount() + 0.000001);
                sendToPrinter(new byte[]{0x1B, 0x18, 0x20}); // Esc, 20H
                break;
            case RateDiscount:
                discount = (int) (100 * (slipLine.getDiscount() * slipLine.getGross()) + 0.000001);
                sendToPrinter(new byte[]{0x1B, 0x18, 0x2d}); // Esc, 2DH
                break;
            case RateExtra:
                discount = (int) (100 * (slipLine.getDiscount() * slipLine.getGross()) + 0.000001);
                sendToPrinter(new byte[]{0x1B, 0x18, 0x20}); // Esc, 20H
                break;
            default:
                throw new IllegalStateException();
        }
        sendToPrinter(prepareInteger(discount)); // 
    }
}
