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

import java.io.IOException;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Calendar;
import java.util.Date;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Logger;
import name.prokop.bart.fps.FiscalPrinterException;
import name.prokop.bart.fps.datamodel.Invoice;
import name.prokop.bart.fps.datamodel.SaleLine;
import name.prokop.bart.fps.datamodel.Slip;
import name.prokop.bart.fps.datamodel.SlipPayment;
import name.prokop.bart.fps.datamodel.VATRate;
import name.prokop.bart.fps.util.BartDate;
import name.prokop.bart.fps.util.BitsAndBytes;
import name.prokop.bart.fps.util.ToString;

/**
 * Klasa implementująca obsługę drukarki fiskalnej POSNET THERMAL z protokołem w
 * wersji 1.01 wg. dokumentu: SPECYFIKACJA PROTOKOLU POSNET w Thermal FV EJ 1.01
 * sygnatura: DBC-I-DEV-45-012_specyfikacja_protokolu_Posnet_w_drukarkach.pdf
 *
 * @author Bartłomiej Piotr Prokop
 */
public class Posnet101Driver {

    private static final Logger logger = Logger.getLogger(Posnet101Driver.class.getName());
    private static final int STX = 0x02;
    private static final int ETX = 0x03;
    private static final char TAB = 0x09;
    private Map<VATRate, Integer> vatRates = new EnumMap<>(VATRate.class);
    private final InputStream inputStream;
    private final OutputStream outputStream;
    private String footerLine1 = "&b&c&hDziękujemy";
    private String footerLine2 = "&c&bZapraszamy ponownie";
    private String footerLine3 = "&i&cPosnet 1.01, (c) Bart Prokop";

    /**
     * Tworzy obiekt zdolny do wymuszenia na drukarce fiskalnej Posnet 1.01
     * wydruku paragonu fiskalnego, zapisanego w klasie Slip
     *
     * @param comPortName Nazwa portu szeregowego, do którego jest przyłączona
     * drukarka fiskalna.
     */
    Posnet101Driver(InputStream inputStream, OutputStream outputStream) {
        this.inputStream = inputStream;
        this.outputStream = outputStream;
    }

    /**
     * Służy do wydrukowania paragonu fiskalnego
     *
     * @param slip paragon do wydrukowania
     * @throws name.prokop.bart.fps.FiscalPrinterException w
     * przypadku niepowodzenia, wraz z opisem błędu
     */
    public synchronized void print(Slip slip) throws FiscalPrinterException {
        printSlip(slip);
    }

    public synchronized void print(Invoice invoice) throws FiscalPrinterException {
        printInvoice(invoice);
    }

    public synchronized void openDrawer() throws FiscalPrinterException {
        final String cmd = "opendrwr";
        try {
            send(cmd + TAB);
            if (!cmd.equals(receive().trim())) {
                logger.severe("Cannot open drawer");
                throw new FiscalPrinterException("Cannot open drawer");
            }
        } catch (IOException e) {
            logger.severe(e.getMessage());
            throw new FiscalPrinterException(e);
        }
    }

    public void printDailyReport() throws FiscalPrinterException {
        final String cmd = "dailyrep";
        try {
            send("prncancel" + TAB);
            logger.fine(receive());
            send(cmd + TAB + "da" + BartDate.getFormatedDate("yyyy-MM-dd", new Date()) + TAB);
            if (!cmd.equals(receive().trim())) {
                logger.severe("Cannot print daily report");
                throw new FiscalPrinterException("Cannot print daily report");
            }
        } catch (IOException ioe) {
            logger.severe(ioe.getMessage());
            throw new FiscalPrinterException(ioe);
        }
    }

    public void printMonthlyReport() throws FiscalPrinterException {
        final String cmd = "monthlyrep";
        try {
            send("prncancel" + TAB);
            logger.fine(receive());
            Calendar calendar = Calendar.getInstance();
            calendar.add(Calendar.MONTH, -1);
            send(cmd + TAB + "da" + BartDate.getFormatedDate("yyyy-MM-dd", calendar.getTime()) + TAB);
            if (!cmd.equals(receive().trim())) {
                logger.severe("Cannot print monthly report");
                throw new FiscalPrinterException("Cannot print monthly report");
            }
        } catch (IOException ioe) {
            logger.severe(ioe.getMessage());
            throw new FiscalPrinterException(ioe);
        }
    }

    public String getFooterLine1() {
        return footerLine1;
    }

    public void setFooterLine1(String footerLine1) {
        this.footerLine1 = footerLine1;
    }

    public String getFooterLine2() {
        return footerLine2;
    }

    public void setFooterLine2(String footerLine2) {
        this.footerLine2 = footerLine2;
    }

    public String getFooterLine3() {
        return footerLine3;
    }

    public void setFooterLine3(String footerLine3) {
        this.footerLine3 = footerLine3;
    }

    void printSlip(Slip slip) throws FiscalPrinterException {
        try {
            send("prncancel" + TAB);
            receive();
            canPrint();
            initVatRates();
            send(cnf(slip.getCashierName(), slip.getCashbox(), slip.getReference()));
            receive();
            send("trinit" + TAB);
            receive();
            for (SaleLine line : slip.getSlipLines()) {
                send(encodeLine(line));
                receive();
            }
            for (SlipPayment sp : slip.getSlipPayments()) {
                send(encodePayment(sp));
                receive();
            }
            int total = encodePrice(slip.getTotal());
            String fp = slip.getSlipPayments().size() > 0 ? "fp" + total + TAB : "";
            send("trend" + TAB + "to" + total + TAB + fp);
            receive();
            send("opendrwr" + TAB);
            receive();
        } catch (IOException e) {
            logger.severe(e.getMessage());
            throw new FiscalPrinterException(e);
        }
    }

    void printInvoice(Invoice invoice) throws FiscalPrinterException {
        try {
            send("prncancel" + TAB);
            receive();
            canPrint();
            initVatRates();
            send(cnf(invoice.getCashierName(), invoice.getCashbox(), invoice.getReference()));
            receive();
            send("trfvinit" + TAB
                    + "nb" + invoice.getReference() + TAB
                    + "ni" + invoice.getNip() + TAB
                    + "na" + invoice.getHeader() + TAB
                    + "pd" + invoice.getPaymentDue() + TAB
                    + "pt" + invoice.getPaymentType() + TAB);
            receive();
            for (SaleLine sl : invoice.getSlipLines()) {
                send(encodeLine(sl));
                receive();
            }
            send("trend" + TAB + "to" + encodePrice(invoice.getTotal()) + TAB);
            receive(45 * 1000); // faktura wymaga potwierdzenia z klawiatury drukarki
        } catch (IOException e) {
            logger.severe(e.getMessage());
            throw new FiscalPrinterException(e);
        }
    }

    private void initVatRates() throws FiscalPrinterException, IOException {
        vatRates.clear();
        send("vatget" + TAB);
        Properties rates = decode(receive());
        for (Object key : rates.keySet()) {
            int rateNo = (int) ((String) key).substring(1).charAt(0) - 'a';
            if (rates.get(key).equals("22,00")) {
                vatRates.put(VATRate.VAT22, rateNo);
            }
            if (rates.get(key).equals("7,00")) {
                vatRates.put(VATRate.VAT07, rateNo);
            }
            if (rates.get(key).equals("3,00")) {
                vatRates.put(VATRate.VAT03, rateNo);
            }
            if (rates.get(key).equals("23,00")) {
                vatRates.put(VATRate.VAT23, rateNo);
            }
            if (rates.get(key).equals("8,00")) {
                vatRates.put(VATRate.VAT08, rateNo);
            }
            if (rates.get(key).equals("5,00")) {
                vatRates.put(VATRate.VAT05, rateNo);
            }
            if (rates.get(key).equals("0,00")) {
                vatRates.put(VATRate.VAT00, rateNo);
            }
            if (rates.get(key).equals("100,00")) {
                vatRates.put(VATRate.VATzw, rateNo);
            }
        }
        logger.fine("Rates assigment: " + vatRates);

        send("strns" + TAB);
        decode(receive());
        send("sfsk" + TAB);
        decode(receive());
        send("stot" + TAB);
        decode(receive());
        send("scnt" + TAB);
        decode(receive());
    }

    private int encodePrice(double p) {
        return (int) Toolbox.round(p * 100, 0);
    }

    private int encodeRate(double p) {
        return (int) Toolbox.round(p * 10000, 0);
    }

    private String encodeLine(SaleLine line) {
        StringBuilder sb = new StringBuilder();
        sb.append("trline").append(TAB);
        sb.append("na").append(line.getName()).append(TAB); // nazwa towaru
        sb.append("vt").append(vatRates.get(line.getTaxRate())).append(TAB); // stawka VAT 0-6
        sb.append("pr").append(encodePrice(line.getPrice())).append(TAB); // cena
        sb.append("il").append(line.getAmount()).append(TAB); // ilość towaru
//        sb.append("wa200" + TAB); // kwota total
        switch (line.getDiscountType()) {
            case AmountExtra:
                sb.append("rd0").append(TAB);
                sb.append("rw").append(encodePrice(line.getDiscount())).append(TAB);
                break;
            case AmountDiscount:
                sb.append("rd1").append(TAB);
                sb.append("rw").append(encodePrice(line.getDiscount())).append(TAB);
                break;
            case RateExtra:
                sb.append("rd0").append(TAB);
                sb.append("rp").append(encodeRate(line.getDiscount())).append(TAB);
                break;
            case RateDiscount:
                sb.append("rd1").append(TAB);
                sb.append("rp").append(encodeRate(line.getDiscount())).append(TAB);
                break;
        }
        return sb.toString();
    }

    private String encodePayment(SlipPayment payment) {
        StringBuilder sb = new StringBuilder();
        sb.append("trpayment").append(TAB);
        switch (payment.getType()) {
            // 0 – gotówka, 2 – karta, 3 – czek, 4 – bon, 5 – kredyt, 6 – inna, 7 – voucher, 8 – konto klienta
            case Cash:
                sb.append("ty0").append(TAB);
                break;
            case CreditCard:
                sb.append("ty2").append(TAB);
                break;
            case Cheque:
                sb.append("ty3").append(TAB);
                break;
            case Bond:
                sb.append("ty4").append(TAB);
                break;
            case Credit:
                sb.append("ty5").append(TAB);
                break;
            case Other:
                sb.append("ty6").append(TAB);
                break;
            case Voucher:
                sb.append("ty7").append(TAB);
                break;
            case Account:
                sb.append("ty8").append(TAB);
                break;
        }
        sb.append("wa").append(encodePrice(payment.getAmount())).append(TAB);
        if (payment.getName() != null && payment.getName().trim().length() > 0) {
            sb.append("na").append(payment.getName()).append(TAB);
        }

        return sb.toString();
    }

    private String cnf(String nazwaKasjera, String numerKasy, String reference) {
        StringBuilder sb = new StringBuilder();
        sb.append("ftrcfg").append(TAB);
        sb.append("cc").append(nazwaKasjera.length() > 32 ? nazwaKasjera.substring(0, 32) : nazwaKasjera).append(TAB);
        sb.append("cn").append(numerKasy.length() > 8 ? numerKasy.substring(0, 8) : numerKasy).append(TAB);
        sb.append("sn").append(reference.length() > 30 ? reference.substring(0, 30) : reference).append(TAB);
        sb.append("bc").append(reference.length() > 16 ? reference.substring(0, 16) : reference).append(TAB);
        sb.append("ln").append(footerLine1).append("\n").append(footerLine2).append("\n").append(footerLine3).append(TAB);
        return sb.toString();
    }

    private void canPrint() throws FiscalPrinterException, IOException {
        send("sid" + TAB);
        Properties p = decode(receive());
        String prnDesc = "Model: " + p.getProperty("nm") + " relase " + p.getProperty("vr");

        send("sdev" + TAB);
        p = decode(receive());
        if (!p.getProperty("ds").equals("0")) {
            if (p.getProperty("ds").equals("1")) {
                logger.severe("Drukarka niegotowa - w menu");
                throw new FiscalPrinterException("Drukarka niegotowa - w menu");
            }
            if (p.getProperty("ds").equals("2")) {
                logger.severe("Drukarka niegotowa - oczekiwanie na klawisz");
                throw new FiscalPrinterException("Drukarka niegotowa - oczekiwanie na klawisz");
            }
            if (p.getProperty("ds").equals("3")) {
                logger.severe("Drukarka niegotowa - oczekiwanie na reakcję użytkownika (wystąpił błąd)");
                throw new FiscalPrinterException("Drukarka niegotowa - oczekiwanie na reakcję użytkownika (wystąpił błąd)");
            }
        }

        send("sprn" + TAB);
        p = decode(receive());
        if (!p.getProperty("pr").equals("0")) {
            if (p.getProperty("pr").equals("1")) {
                throw new FiscalPrinterException("Drukarka niegotowa - podniesiona dźwignia");
            }
            if (p.getProperty("pr").equals("2")) {
                throw new FiscalPrinterException("Drukarka niegotowa - brak dostępu do mechanizmu");
            }
            if (p.getProperty("pr").equals("3")) {
                throw new FiscalPrinterException("Drukarka niegotowa - podniesiona pokrywa");
            }
            if (p.getProperty("pr").equals("4")) {
                throw new FiscalPrinterException("Drukarka niegotowa - brak papieru – kopia");
            }
            if (p.getProperty("pr").equals("5")) {
                throw new FiscalPrinterException("Drukarka niegotowa - brak papieru – oryginał");
            }
            if (p.getProperty("pr").equals("6")) {
                throw new FiscalPrinterException("Drukarka niegotowa - nieodpowiednia temperatura lub zasilanie");
            }
            if (p.getProperty("pr").equals("7")) {
                throw new FiscalPrinterException("Drukarka niegotowa - chwilowy zanik zasilania");
            }
            if (p.getProperty("pr").equals("8")) {
                throw new FiscalPrinterException("Drukarka niegotowa - błąd obcinacza");
            }
            if (p.getProperty("pr").equals("9")) {
                throw new FiscalPrinterException("Drukarka niegotowa - błąd zasilacza");
            }
            if (p.getProperty("pr").equals("10")) {
                throw new FiscalPrinterException("Drukarka niegotowa - podniesiona pokrywa przy obcinaniu");
            }
        }

        send("scomm" + TAB);
        p = decode(receive());
        prnDesc += " SN: " + p.getProperty("nu");
        logger.fine("Detected printer: " + prnDesc);

        if (!decodeBool(p.getProperty("hr").charAt(0))) {
            throw new FiscalPrinterException("Brak nagłówka");
        }
        if (!p.getProperty("ts").equals("0")) {
            logger.severe("Niezakończona poprzednia transakcja");
            throw new FiscalPrinterException("Niezakończona poprzednia transakcja");
        }
    }

    private Properties decode(String answer) {
        String[] props = answer.split("\t");
        Properties retVal = new Properties();
        //retVal.setProperty("CMD", props[0]);
        for (int i = 1; i < props.length; i++) {
            retVal.setProperty(props[i].substring(0, 2), props[i].substring(2).trim());
        }
        logger.finest("Rx: " + retVal.toString());
        return retVal;
    }

    private void send(String s) throws IOException {
        byte[] seq = s.getBytes("Cp1250");
        outputStream.write(STX);
        outputStream.write(seq);
        outputStream.write('#');
        outputStream.write(calcCRC(seq).getBytes());
        outputStream.write(ETX);
        logger.finest("Tx: " + s);
    }

    private String receive() throws IOException, FiscalPrinterException {
        return receive(10000);
    }

    private String receive(int timeout) throws IOException, FiscalPrinterException {
        while (--timeout > 0 && ((inputStream.available() > 0) ? (inputStream.read() != STX) : true)) {
            sleep(1);
        }
        if (timeout == 0) {
            logger.severe("timeout - no STX");
            throw new IOException("timeout - no STX");
        }
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        while (--timeout > 0) {
            if (inputStream.available() > 0) {
                int b = inputStream.read();
                if (b == '#') {
                    break;
                }
                baos.write(b);
            }
            sleep(1);
        }
        if (timeout == 0) {
            logger.severe("timeout - no #");
            throw new IOException("timeout - no #");
        }
        while (--timeout > 0 && inputStream.available() < 4) {
            sleep(1);
        }
        if (timeout == 0) {
            logger.severe("timeout - no CRC");
            throw new IOException("timeout - no CRC");
        } else {
            byte[] crc = new byte[4];
            inputStream.read(crc);
            if (!calcCRC(baos.toByteArray()).equalsIgnoreCase(new String(crc))) {
                logger.severe("Bad no CRC");
                throw new IOException("Bad CRC");
            }
        }
        while (--timeout > 0 && ((inputStream.available() > 0) ? (inputStream.read() != ETX) : true)) {
            sleep(1);
        }
        if (timeout == 0) {
            logger.severe("timeout - no ETX");
            throw new IOException("timeout - no ETX");
        }
        logger.finest("Rx (bin): " + ToString.byteArrayToString(baos.toByteArray()));
        String retVal = baos.toString("Cp1250");
        logger.finest("Rx (str): " + ToString.debugString(retVal));
        if (retVal.indexOf('?') != -1) {
            String err_no = retVal.substring(retVal.indexOf('?') + 1).trim();
            String cmd = retVal.substring(0, retVal.indexOf('?')).trim();
            logger.severe("Błąd nr " + err_no + " w rozkazie " + cmd + ". " + errors.get(err_no));
            throw new FiscalPrinterException("Błąd nr " + err_no + " w rozkazie " + cmd + ". " + errors.get(err_no));
        }
        return retVal;
    }

    private static void sleep(int time) {
        try {
            Thread.sleep(time);
        } catch (InterruptedException ie) {
            ie.printStackTrace(System.out);
        }
    }
    private static final byte[] crc16htab = new byte[]{
        (byte) 0x00, (byte) 0x10, (byte) 0x20, (byte) 0x30, (byte) 0x40, (byte) 0x50, (byte) 0x60, (byte) 0x70,
        (byte) 0x81, (byte) 0x91, (byte) 0xa1, (byte) 0xb1, (byte) 0xc1, (byte) 0xd1, (byte) 0xe1, (byte) 0xf1,
        (byte) 0x12, (byte) 0x02, (byte) 0x32, (byte) 0x22, (byte) 0x52, (byte) 0x42, (byte) 0x72, (byte) 0x62,
        (byte) 0x93, (byte) 0x83, (byte) 0xb3, (byte) 0xa3, (byte) 0xd3, (byte) 0xc3, (byte) 0xf3, (byte) 0xe3,
        (byte) 0x24, (byte) 0x34, (byte) 0x04, (byte) 0x14, (byte) 0x64, (byte) 0x74, (byte) 0x44, (byte) 0x54,
        (byte) 0xa5, (byte) 0xb5, (byte) 0x85, (byte) 0x95, (byte) 0xe5, (byte) 0xf5, (byte) 0xc5, (byte) 0xd5,
        (byte) 0x36, (byte) 0x26, (byte) 0x16, (byte) 0x06, (byte) 0x76, (byte) 0x66, (byte) 0x56, (byte) 0x46,
        (byte) 0xb7, (byte) 0xa7, (byte) 0x97, (byte) 0x87, (byte) 0xf7, (byte) 0xe7, (byte) 0xd7, (byte) 0xc7,
        (byte) 0x48, (byte) 0x58, (byte) 0x68, (byte) 0x78, (byte) 0x08, (byte) 0x18, (byte) 0x28, (byte) 0x38,
        (byte) 0xc9, (byte) 0xd9, (byte) 0xe9, (byte) 0xf9, (byte) 0x89, (byte) 0x99, (byte) 0xa9, (byte) 0xb9,
        (byte) 0x5a, (byte) 0x4a, (byte) 0x7a, (byte) 0x6a, (byte) 0x1a, (byte) 0x0a, (byte) 0x3a, (byte) 0x2a,
        (byte) 0xdb, (byte) 0xcb, (byte) 0xfb, (byte) 0xeb, (byte) 0x9b, (byte) 0x8b, (byte) 0xbb, (byte) 0xab,
        (byte) 0x6c, (byte) 0x7c, (byte) 0x4c, (byte) 0x5c, (byte) 0x2c, (byte) 0x3c, (byte) 0x0c, (byte) 0x1c,
        (byte) 0xed, (byte) 0xfd, (byte) 0xcd, (byte) 0xdd, (byte) 0xad, (byte) 0xbd, (byte) 0x8d, (byte) 0x9d,
        (byte) 0x7e, (byte) 0x6e, (byte) 0x5e, (byte) 0x4e, (byte) 0x3e, (byte) 0x2e, (byte) 0x1e, (byte) 0x0e,
        (byte) 0xff, (byte) 0xef, (byte) 0xdf, (byte) 0xcf, (byte) 0xbf, (byte) 0xaf, (byte) 0x9f, (byte) 0x8f,
        (byte) 0x91, (byte) 0x81, (byte) 0xb1, (byte) 0xa1, (byte) 0xd1, (byte) 0xc1, (byte) 0xf1, (byte) 0xe1,
        (byte) 0x10, (byte) 0x00, (byte) 0x30, (byte) 0x20, (byte) 0x50, (byte) 0x40, (byte) 0x70, (byte) 0x60,
        (byte) 0x83, (byte) 0x93, (byte) 0xa3, (byte) 0xb3, (byte) 0xc3, (byte) 0xd3, (byte) 0xe3, (byte) 0xf3,
        (byte) 0x02, (byte) 0x12, (byte) 0x22, (byte) 0x32, (byte) 0x42, (byte) 0x52, (byte) 0x62, (byte) 0x72,
        (byte) 0xb5, (byte) 0xa5, (byte) 0x95, (byte) 0x85, (byte) 0xf5, (byte) 0xe5, (byte) 0xd5, (byte) 0xc5,
        (byte) 0x34, (byte) 0x24, (byte) 0x14, (byte) 0x04, (byte) 0x74, (byte) 0x64, (byte) 0x54, (byte) 0x44,
        (byte) 0xa7, (byte) 0xb7, (byte) 0x87, (byte) 0x97, (byte) 0xe7, (byte) 0xf7, (byte) 0xc7, (byte) 0xd7,
        (byte) 0x26, (byte) 0x36, (byte) 0x06, (byte) 0x16, (byte) 0x66, (byte) 0x76, (byte) 0x46, (byte) 0x56,
        (byte) 0xd9, (byte) 0xc9, (byte) 0xf9, (byte) 0xe9, (byte) 0x99, (byte) 0x89, (byte) 0xb9, (byte) 0xa9,
        (byte) 0x58, (byte) 0x48, (byte) 0x78, (byte) 0x68, (byte) 0x18, (byte) 0x08, (byte) 0x38, (byte) 0x28,
        (byte) 0xcb, (byte) 0xdb, (byte) 0xeb, (byte) 0xfb, (byte) 0x8b, (byte) 0x9b, (byte) 0xab, (byte) 0xbb,
        (byte) 0x4a, (byte) 0x5a, (byte) 0x6a, (byte) 0x7a, (byte) 0x0a, (byte) 0x1a, (byte) 0x2a, (byte) 0x3a,
        (byte) 0xfd, (byte) 0xed, (byte) 0xdd, (byte) 0xcd, (byte) 0xbd, (byte) 0xad, (byte) 0x9d, (byte) 0x8d,
        (byte) 0x7c, (byte) 0x6c, (byte) 0x5c, (byte) 0x4c, (byte) 0x3c, (byte) 0x2c, (byte) 0x1c, (byte) 0x0c,
        (byte) 0xef, (byte) 0xff, (byte) 0xcf, (byte) 0xdf, (byte) 0xaf, (byte) 0xbf, (byte) 0x8f, (byte) 0x9f,
        (byte) 0x6e, (byte) 0x7e, (byte) 0x4e, (byte) 0x5e, (byte) 0x2e, (byte) 0x3e, (byte) 0x0e, (byte) 0x1e};
    private static final byte[] crc16ltab = {
        (byte) 0x00, (byte) 0x21, (byte) 0x42, (byte) 0x63, (byte) 0x84, (byte) 0xa5, (byte) 0xc6, (byte) 0xe7,
        (byte) 0x08, (byte) 0x29, (byte) 0x4a, (byte) 0x6b, (byte) 0x8c, (byte) 0xad, (byte) 0xce, (byte) 0xef,
        (byte) 0x31, (byte) 0x10, (byte) 0x73, (byte) 0x52, (byte) 0xb5, (byte) 0x94, (byte) 0xf7, (byte) 0xd6,
        (byte) 0x39, (byte) 0x18, (byte) 0x7b, (byte) 0x5a, (byte) 0xbd, (byte) 0x9c, (byte) 0xff, (byte) 0xde,
        (byte) 0x62, (byte) 0x43, (byte) 0x20, (byte) 0x01, (byte) 0xe6, (byte) 0xc7, (byte) 0xa4, (byte) 0x85,
        (byte) 0x6a, (byte) 0x4b, (byte) 0x28, (byte) 0x09, (byte) 0xee, (byte) 0xcf, (byte) 0xac, (byte) 0x8d,
        (byte) 0x53, (byte) 0x72, (byte) 0x11, (byte) 0x30, (byte) 0xd7, (byte) 0xf6, (byte) 0x95, (byte) 0xb4,
        (byte) 0x5b, (byte) 0x7a, (byte) 0x19, (byte) 0x38, (byte) 0xdf, (byte) 0xfe, (byte) 0x9d, (byte) 0xbc,
        (byte) 0xc4, (byte) 0xe5, (byte) 0x86, (byte) 0xa7, (byte) 0x40, (byte) 0x61, (byte) 0x02, (byte) 0x23,
        (byte) 0xcc, (byte) 0xed, (byte) 0x8e, (byte) 0xaf, (byte) 0x48, (byte) 0x69, (byte) 0x0a, (byte) 0x2b,
        (byte) 0xf5, (byte) 0xd4, (byte) 0xb7, (byte) 0x96, (byte) 0x71, (byte) 0x50, (byte) 0x33, (byte) 0x12,
        (byte) 0xfd, (byte) 0xdc, (byte) 0xbf, (byte) 0x9e, (byte) 0x79, (byte) 0x58, (byte) 0x3b, (byte) 0x1a,
        (byte) 0xa6, (byte) 0x87, (byte) 0xe4, (byte) 0xc5, (byte) 0x22, (byte) 0x03, (byte) 0x60, (byte) 0x41,
        (byte) 0xae, (byte) 0x8f, (byte) 0xec, (byte) 0xcd, (byte) 0x2a, (byte) 0x0b, (byte) 0x68, (byte) 0x49,
        (byte) 0x97, (byte) 0xb6, (byte) 0xd5, (byte) 0xf4, (byte) 0x13, (byte) 0x32, (byte) 0x51, (byte) 0x70,
        (byte) 0x9f, (byte) 0xbe, (byte) 0xdd, (byte) 0xfc, (byte) 0x1b, (byte) 0x3a, (byte) 0x59, (byte) 0x78,
        (byte) 0x88, (byte) 0xa9, (byte) 0xca, (byte) 0xeb, (byte) 0x0c, (byte) 0x2d, (byte) 0x4e, (byte) 0x6f,
        (byte) 0x80, (byte) 0xa1, (byte) 0xc2, (byte) 0xe3, (byte) 0x04, (byte) 0x25, (byte) 0x46, (byte) 0x67,
        (byte) 0xb9, (byte) 0x98, (byte) 0xfb, (byte) 0xda, (byte) 0x3d, (byte) 0x1c, (byte) 0x7f, (byte) 0x5e,
        (byte) 0xb1, (byte) 0x90, (byte) 0xf3, (byte) 0xd2, (byte) 0x35, (byte) 0x14, (byte) 0x77, (byte) 0x56,
        (byte) 0xea, (byte) 0xcb, (byte) 0xa8, (byte) 0x89, (byte) 0x6e, (byte) 0x4f, (byte) 0x2c, (byte) 0x0d,
        (byte) 0xe2, (byte) 0xc3, (byte) 0xa0, (byte) 0x81, (byte) 0x66, (byte) 0x47, (byte) 0x24, (byte) 0x05,
        (byte) 0xdb, (byte) 0xfa, (byte) 0x99, (byte) 0xb8, (byte) 0x5f, (byte) 0x7e, (byte) 0x1d, (byte) 0x3c,
        (byte) 0xd3, (byte) 0xf2, (byte) 0x91, (byte) 0xb0, (byte) 0x57, (byte) 0x76, (byte) 0x15, (byte) 0x34,
        (byte) 0x4c, (byte) 0x6d, (byte) 0x0e, (byte) 0x2f, (byte) 0xc8, (byte) 0xe9, (byte) 0x8a, (byte) 0xab,
        (byte) 0x44, (byte) 0x65, (byte) 0x06, (byte) 0x27, (byte) 0xc0, (byte) 0xe1, (byte) 0x82, (byte) 0xa3,
        (byte) 0x7d, (byte) 0x5c, (byte) 0x3f, (byte) 0x1e, (byte) 0xf9, (byte) 0xd8, (byte) 0xbb, (byte) 0x9a,
        (byte) 0x75, (byte) 0x54, (byte) 0x37, (byte) 0x16, (byte) 0xf1, (byte) 0xd0, (byte) 0xb3, (byte) 0x92,
        (byte) 0x2e, (byte) 0x0f, (byte) 0x6c, (byte) 0x4d, (byte) 0xaa, (byte) 0x8b, (byte) 0xe8, (byte) 0xc9,
        (byte) 0x26, (byte) 0x07, (byte) 0x64, (byte) 0x45, (byte) 0xa2, (byte) 0x83, (byte) 0xe0, (byte) 0xc1,
        (byte) 0x1f, (byte) 0x3e, (byte) 0x5d, (byte) 0x7c, (byte) 0x9b, (byte) 0xba, (byte) 0xd9, (byte) 0xf8,
        (byte) 0x17, (byte) 0x36, (byte) 0x55, (byte) 0x74, (byte) 0x93, (byte) 0xb2, (byte) 0xd1, (byte) 0xf0};

    private static String calcCRC(byte[] s) {
        int hi = 0, lo = 0, index;
        for (byte b : s) {
            index = hi ^ BitsAndBytes.promoteByteToInt(b);
            hi = lo ^ BitsAndBytes.promoteByteToInt(crc16htab[index]);
            lo = BitsAndBytes.promoteByteToInt(crc16ltab[index]);
        }
        index = BitsAndBytes.buildInt((byte) 0, (byte) 0, (byte) hi, (byte) lo);

        String retVal = Integer.toHexString(index);
        while (retVal.length() < 4) {
            retVal = "0" + retVal;
        }
        return retVal;
    }
    private static final Map<String, String> errors = new HashMap<String, String>();

    static {
        errors.put("10", "błąd nietypowy - rezygnacja, przerwanie funkcji");
        errors.put("50", "Błąd wykonywania operacji przez kasę.");
        errors.put("51", "Błąd wykonywania operacji przez kasę.");
        errors.put("52", "Błąd wykonywania operacji przez kasę.");
        errors.put("53", "Błąd wykonywania operacji przez kasę.");
        errors.put("54", "Błąd wykonywania operacji przez kasę.");
        errors.put("55", "Błąd wykonywania operacji przez kasę.");
        errors.put("56", "Błąd wykonywania operacji przez kasę.");
        errors.put("323", "Funkcja zablokowana w konfiguracji");
        errors.put("360", "znaleziono zworę serwisową");
        errors.put("361", "nie znaleziono zwory");
        errors.put("362", "błąd weryfikacji danych klucza");
        errors.put("363", "upłynął czas na odpowiedź od klucza");
        errors.put("382", "próba wykonania raportu zerowego");
        errors.put("383", "Brak raportu dobowego.");
        errors.put("384", "Brak rekordu w pamięci.");
        errors.put("400", "błędna wartość");
        errors.put("404", "Wprowadzono nieprawidłowy błąd kontrolny");
        errors.put("460", "błąd zegara w trybie fiskalnym");
        errors.put("461", "błąd zegara w trybie niefiskalnym");
        errors.put("480", "drukarka już autoryzowana, bezterminowo");
        errors.put("481", "nie rozpoczęto jeszcze autoryzacji");
        errors.put("482", "kod już wprowadzony");
        errors.put("483", "próba wprowadzenia błędnych wartości");
        errors.put("484", "minął czas pracy kasy, sprzedaż zablokowana");
        errors.put("485", "błędny kod autoryzacji");
        errors.put("486", "Blokada autoryzacji. Wprowadź kod z klawiatury.");
        errors.put("487", "Użyto już maksymalnej liczby kodów");
        errors.put("500", "przepełnienie statystyki minimalnej");
        errors.put("501", "przepełnienie statystyki maksymalnej");
        errors.put("502", "Przepełnienie stanu kasy");
        errors.put("503", "Wartość stanu kasy po wypłacie staje się ujemna (przyjmuje się stan zerowy kasy)");
        errors.put("700", "błędny adres IP");
        errors.put("701", "błąd numeru tonu");
        errors.put("702", "błąd długości impulsu szuflady");
        errors.put("703", "błąd stawki VAT");
        errors.put("704", "błąd czasu wylogowania");
        errors.put("705", "błąd czasu uśpienia");
        errors.put("706", "błąd czasu wyłączenia");
        errors.put("713", "Błędne parametry konfiguracji");
        errors.put("714", "błędna wartość kontrastu wyświetlacza");
        errors.put("715", "błędna wartość podświetlenia wyświetlacza");
        errors.put("716", "błędna wartość czasu zaniku podświetlenia");
        errors.put("717", "za długa linia nagłówka albo stopki");
        errors.put("718", "błędna konfiguracja komunikacji");
        errors.put("719", "błędna konfiguracja protokołu kom.");
        errors.put("720", "błędny identyfikator portu");
        errors.put("721", "błędny numer tekstu reklamowego");
        errors.put("722", "podany czas wychodzi poza wymagany zakres");
        errors.put("723", "podana data/czas niepoprawne");
        errors.put("724", "inna godzina w różnicach czasowych 0<=>23");
        errors.put("726", "błędna zawartość tekstu w linii wyświetlacza");
        errors.put("727", "błędna wartość dla przewijania na wyświetlaczu");
        errors.put("728", "błędna konfiguracja portu");
        errors.put("729", "błędna konfiguracja monitora transakcji");
        errors.put("738", "Nieprawidłowa konfiguracja Ethernetu");
        errors.put("739", "Nieprawidłowy typ wyświetlacza");
        errors.put("740", "Dla tego typu wyświetlacza nie można ustawić czasu zaniku podświetlenia");
        errors.put("820", "negatywny wynik testu");
        errors.put("821", "Brak testowanej opcji w konfiguracji");
        errors.put("857", "brak pamięci na inicjalizację bazy drukarkowej");
        errors.put("1000", "błąd fatalny modułu fiskalnego.");
        errors.put("1001", "wypięta pamięć fiskalna");
        errors.put("1002", "błąd zapisu");
        errors.put("1003", "błąd nie ujęty w specyfikacji bios");
        errors.put("1004", "błędne sumy kontrolne");
        errors.put("1005", "błąd w pierwszym bloku kontrolnym");
        errors.put("1006", "błąd w drugim bloku kontrolnym");
        errors.put("1007", "błędny id rekordu");
        errors.put("1008", "błąd inicjalizacji adresu startowego");
        errors.put("1009", "adres startowy zainicjalizowany");
        errors.put("1010", "numer unikatowy już zapisany");
        errors.put("1011", "brak numeru w trybie fiskalnym");
        errors.put("1012", "błąd zapisu numeru unikatowego");
        errors.put("1013", "przepełnienie numerów unikatowych");
        errors.put("1014", "błędny język w numerze unikatowym");
        errors.put("1015", "więcej niż jeden NIP");
        errors.put("1016", "drukarka w trybie do odczytu bez rekordu fiskalizacji");
        errors.put("1017", "przekroczono liczbę zerowań RAM");
        errors.put("1018", "przekroczono liczbę raportów dobowych");
        errors.put("1019", "błąd weryfikacji numeru unikatowego");
        errors.put("1020", "błąd weryfikacji statystyk z RD.");
        errors.put("1021", "błąd odczytu danych z NVR do weryfikacji FM");
        errors.put("1022", "błąd zapisu danych z NVR do weryfikacji FM");
        errors.put("1023", "pamięć fiskalna jest mała 1Mb zamiast 2Mb");
        errors.put("1024", "nie zainicjalizowany obszar danych w pamięci fiskalnej");
        errors.put("1025", "błędny format numeru unikatowego");
        errors.put("1026", "za dużo błędnych bloków w FM");
        errors.put("1027", "błąd oznaczenia błędnego bloku");
        errors.put("1028", "rekord w pamięci fiskalnej nie istnieje - obszar pusty");
        errors.put("1029", "rekord w pamięci fiskalnej z datą późniejszą od poprzedniego");
        errors.put("1030", "błąd odczytu skrótu raportu dobowego.");
        errors.put("1031", "błąd zapisu skrótu raportu dobowego.");
        errors.put("1032", "błąd odczytu informacji o weryfikacji skrótu raportu dobowego.");
        errors.put("1033", "błąd zapisu informacji o weryfikacji skrótu raportu dobowego.");
        errors.put("1034", "błąd odczytu etykiety nośnika.");
        errors.put("1035", "błąd zapisu etykiety nośnika.");
        errors.put("1036", "niezgodność danych kopii elektronicznej.");
        errors.put("1037", "błędne dane w obszarze bitów faktur, brak ciągłości, zaplątany gdzieś bit lub podobne");
        errors.put("1038", "błąd w obszarze faktur. Obszar nie jest pusty.");
        errors.put("1039", "brak miejsca na nowe faktury");
        errors.put("1040", "Suma faktur z raportów dobowych jest większa od licznika faktur.");
        errors.put("1950", "przekroczony zakres totalizerów paragonu.");
        errors.put("1951", "wpłata formą płatności przekracza max. wpłatę.");
        errors.put("1952", "suma form płatności przekracza max. wpłatę.");
        errors.put("1953", "formy płatności pokrywają już do zapłaty.");
        errors.put("1954", "wpłata reszty przekracza max. wpłatę.");
        errors.put("1955", "suma form płatności przekracza max. wpłatę.");
        errors.put("1956", "przekroczony zakres total.");
        errors.put("1957", "przekroczony maksymalny zakres paragonu.");
        errors.put("1958", "przekroczony zakres wartości opakowań.");
        errors.put("1959", "przekroczony zakres wartości opakowań przy stornowaniu.");
        errors.put("1961", "wpłata reszty zbyt duża");
        errors.put("1962", "wpłata formą płatności wartości 0");
        errors.put("1980", "przekroczony zakres kwoty bazowej rabatu/narzutu");
        errors.put("1981", "przekroczony zakres kwoty po rabacie / narzucie");
        errors.put("1982", "błąd obliczania rabatu/narzutu");
        errors.put("1983", "wartość bazowa ujemna lub równa 0");
        errors.put("1984", "wartość rabatu/narzutu zerowa");
        errors.put("1985", "wartość po rabacie ujemna lub równa 0");
        errors.put("1990", "Niedozwolone stornowanie towaru. Błędny stan transakcji.");
        errors.put("1991", "Niedozwolony rabat/narzut. Błędny stan transakcji.");
        errors.put("2000", "błąd pola VAT.");
        errors.put("2002", "brak nagłówka");
        errors.put("2003", "zaprogramowany nagłówek");
        errors.put("2004", "brak aktywnych stawek VAT.");
        errors.put("2005", "brak trybu transakcji.");
        errors.put("2006", "błąd pola cena ( cena <= 0 )");
        errors.put("2007", "błąd pola ilość ( ilość <= 0 )");
        errors.put("2008", "błąd kwoty total");
        errors.put("2009", "błąd kwoty total, równa zero");
        errors.put("2010", "przekroczony zakres totalizerów dobowych.");
        errors.put("2021", "próba ponownego ustawienia zegara.");
        errors.put("2022", "zbyt duża różnica dat");
        errors.put("2023", "różnica większa niż godzina w trybie użytkownika w trybie fiskalnym.");
        errors.put("2024", "zły format daty (np. 13 miesiąc )");
        errors.put("2025", "data wcześniejsza od ostatniego zapisu do modułu");
        errors.put("2026", "błąd zegara.");
        errors.put("2027", "przekroczono maksymalną liczbę zmian stawek VAT");
        errors.put("2028", "próba zdefiniowana identycznych stawek VAT");
        errors.put("2029", "błędne wartości stawek VAT");
        errors.put("2030", "próba zdefiniowania stawek VAT wszystkich nieaktywnych");
        errors.put("2031", "błąd pola NIP.");
        errors.put("2032", "błąd numeru unikatowego pamięci fiskalnej.");
        errors.put("2033", "urządzenie w trybie fiskalnym.");
        errors.put("2034", "urządzenie w trybie niefiskalnym.");
        errors.put("2035", "niezerowe totalizery.");
        errors.put("2036", "urządzenie w stanie tylko do odczytu.");
        errors.put("2037", "urządzenie nie jest w stanie tylko do odczytu.");
        errors.put("2038", "urządzenie w trybie transakcji.");
        errors.put("2039", "zerowe totalizery.");
        errors.put("2040", "Błąd obliczeń walut, przepełnienie przy mnożeniu lub dzieleniu.");
        errors.put("2041", "próba zakończenia pozytywnego paragonu z wartością 0");
        errors.put("2042", "błędy format daty początkowej");
        errors.put("2043", "błędy format daty końcowej");
        errors.put("2044", "próba wykonania raportu miesięcznego w danym miesiącu");
        errors.put("2045", "data początkowa późniejsza od bieżącej daty");
        errors.put("2046", "data końcowa wcześniejsza od daty fiskalizacji");
        errors.put("2047", "numer początkowy lub końcowy równy zero");
        errors.put("2048", "numer początkowy większy od numeru końcowego");
        errors.put("2049", "numer raportu zbyt duży");
        errors.put("2050", "data początkowa późniejsza od daty końcowej");
        errors.put("2051", "brak pamięci w buforze tekstów.");
        errors.put("2052", "brak pamięci w buforze transakcji");
        errors.put("2054", "Formy płatności nie pokrywają kwoty do zapłaty lub reszty.");
        errors.put("2055", "błędna linia");
        errors.put("2057", "przekroczony rozmiar lub przekroczona liczba znaków formatujących");
        errors.put("2058", "błędna liczba linii.");
        errors.put("2060", "błędny stan transakcji");
        errors.put("2062", "jest wydrukowana część jakiegoś dokumentu");
        errors.put("2063", "błąd parametru");
        errors.put("2064", "brak rozpoczęcia wydruku lub transakcji.");
        errors.put("2067", "błąd ustawień konfiguracyjnych wydruków / drukarki");
        errors.put("2070", "Data przeglądu wcześniejsza od systemowej");
        errors.put("2101", "Zapełnienie bazy");
        errors.put("2102", "Stawka nieaktywna");
        errors.put("2103", "Nieprawidłowa stawka VAT");
        errors.put("2104", "Błąd nazwy");
        errors.put("2105", "Błąd przypisania stawki");
        errors.put("2106", "Zablokowany");
        errors.put("2107", "Nie znaleziono w bazie drukarkowej");
        errors.put("2108", "baza nie jest zapełniona");
        errors.put("2501", "Błędny identyfikator raportu");
        errors.put("2502", "Błędny identyfikator linii raportu");
        errors.put("2503", "Błędny identyfikator nagłówka raportu");
        errors.put("2504", "Zbyt mało parametrów raportu");
        errors.put("2505", "Raport nie rozpoczęty");
        errors.put("2506", "Raport rozpoczęty");
        errors.put("2507", "Błędny identyfikator komendy");
        errors.put("2521", "Raport już rozpoczęty");
        errors.put("2522", "Raport nie rozpoczęty");
        errors.put("2523", "Błędna stawka VAT");
        errors.put("2532", "Błędna liczba kopii faktur");
        errors.put("2533", "Pusty numer faktury");
        errors.put("2600", "Błędny typ rabatu/narzutu");
        errors.put("2601", "wartość rabatu/narzutu spoza zakresu");
        errors.put("2701", "Błąd identyfikatora stawki podatkowej.");
        errors.put("2702", "Błędny identyfikator dodatkowej stopki.");
        errors.put("2703", "Przekroczona liczba dodatkowych stopek.");
        errors.put("2704", "Zbyt słaby akumulator.");
        errors.put("2705", "Błędny identyfikator typu formy płatności.");
        errors.put("2710", "Usługa o podanym identyfikatorze nie jest uruchomiona.");
        errors.put("2801", "Błąd weryfikacji wartości rabatu/narzutu");
        errors.put("2802", "Błąd weryfikacji wartości linii sprzedaży");
        errors.put("2803", "Błąd weryfikacji wartości opakowania");
        errors.put("2804", "Błąd weryfikacji wartości formy płatności");
        errors.put("2805", "Błąd weryfikacji wartości fiskalnej");
        errors.put("2806", "Błąd weryfikacji wartości opakowań dodatnich");
        errors.put("2807", "Błąd weryfikacji wartości opakowań ujemnych");
        errors.put("2808", "Błąd weryfikacji wartości wpłaconych form płatności");
        errors.put("2809", "Błąd weryfikacji wartości reszt");
        errors.put("2851", "Błąd stornowania, błędna ilość");
        errors.put("2852", "Błąd stornowania, błędna wartość");
        errors.put("2900", "Stan kopii elektronicznej nie pozwala na wydrukowanie tego dokumentu.");
        errors.put("2903", "Pamięć podręczna kopii elektronicznej zawiera zbyt dużą ilość danych.");
        errors.put("2911", "Brak pliku na nośniku.");
        errors.put("2913", "Nieprawidłowy wynik testu.");
        errors.put("3051", "Nie można zmienić 2 raz waluty ewidencyjnej po RD.");
        errors.put("3052", "Próba ustawienia już ustawionej waluty.");
        errors.put("3053", "Błędna nazwa waluty.");
        errors.put("3054", "Automatyczna zmiana waluty.");
        errors.put("3055", "Błędna wartość przelicznika kursu.");
    }

    private static boolean decodeBool(Character c) {
        if (c == '1' || Character.toUpperCase(c) == 'T' || Character.toUpperCase(c) == 'Y') {
            return true;
        }
        if (c == '0' || Character.toUpperCase(c) == 'N') {
            return false;
        }
        throw new IllegalStateException();
    }
}
