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
 *
 * Created on 22 styczeń 2005, 21:58
 */
package name.prokop.bart.fps.drivers;

import gnu.io.SerialPort;
import gnu.io.UnsupportedCommOperationException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import name.prokop.bart.fps.FiscalPrinter;
import name.prokop.bart.fps.FiscalPrinterException;
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
 * Klasa implementująca obsługę drukarki fiskalnej POSNET THERMAL z protokołem w
 * wersji 1.01
 *
 * @author Bartłomiej Piotr Prokop
 */
public class Thermal301 implements FiscalPrinter {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        Thermal301 fp;

        if (args.length != 0) {
            fp = (Thermal301) Thermal301.getFiscalPrinter(args[0]);
        } else {
            fp = (Thermal301) Thermal301.getFiscalPrinter("COM1");
        }

        try {
            fp.print(SlipExamples.getOneCentSlip());
        } catch (FiscalPrinterException e) {
            System.err.println(e);
        }
    }

    public static FiscalPrinter getFiscalPrinter(String comPortName) {
        return new Thermal301(comPortName);
    }
    /**
     * Creates a new instance of PosnetThermal101
     */
    String comPortName;

    /**
     * Tworzy obiekt zdolny do wymuszenia na drukarce fiskalnej Posnet 1.01
     * wydruku paragonu fiskalnego, zapisanego w klasie Slip
     *
     * @param comPortName Nazwa portu szeregowego, do którego jest przyłączona
     * drukarka fiskalna.
     */
    private Thermal301(String comPortName) {
        this.comPortName = comPortName;
    }
    private SerialPort serialPort;
    private OutputStream outputStream;
    private InputStream inputStream;

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
        //System.out.println("Paragon do druku: " + slip);
        try {
            connect();
            reset();
            flushStreams();

            // ustaw pełną, programową obsługę błędów
            sendLBSERM((byte) 3);

            // jeśli w stanie transakcji, to anuluj istniejącą transakcję
            if (pflPAR) {
                sendLBTREXITCAN();
            }

            // get all information (status, totals, serial, etc) from printer
            sendLBFSTRQ((byte) 23);
            printSlip(slip);
        } finally {
            disconnect();
            //Do testów jak się wysyła.
            //System.exit(-10);
        }
    }

    public void print(Invoice invoice) throws FiscalPrinterException {
        throw new FiscalPrinterException(new UnsupportedOperationException("Not supported yet."));
    }

    @Override
    public synchronized void openDrawer() throws FiscalPrinterException {
        try {
            connect();
            reset();
            flushStreams();

            // ustaw pełną, programową obsługę błędów
            sendLBSERM((byte) 3);
            // otwórz szufladę
            sendLBDSP();
        } finally {
            disconnect();
        }
    }

    @Override
    public void printDailyReport() throws FiscalPrinterException {
        try {
            connect();
            reset();
            flushStreams();

            // ustaw pełną, programową obsługę błędów
            sendLBSERM((byte) 3);
            // wydrukuj raport dobowy
            sendLBDAYREP();
        } finally {
            disconnect();
        }
    }

    private void printSlip(Slip slip) throws FiscalPrinterException {
        // rozpocznij transkację
        sendLBTRSHDR((byte) 0);
        // wyślij wszystkie linie paragonu
        for (int i = 0; i < slip.getNoOfLines(); i++) {
            SaleLine line = slip.getLine(i);
            //System.out.println("Sending line: " + line.toString());
            sendLBTRSLN(i + 1, line.getName(), line.getAmount(), line.getPrice(), findPTU(line.getTaxRate()), line.getGross(), line.getDiscountType().ordinal(), line.getDiscount());
        }
        // zakończ transakcję
        sendLBTRXEND1(slip.getTotal(), slip.getCashbox(), slip.getCashierName(), slip.getReference(), preparePayments(slip));

        try {
            Thread.sleep(250);
        } catch (InterruptedException ex) {
        }
        // otworz szuflade
        sendLBDSP();
    }

    private char findPTU(VATRate ptu) throws FiscalPrinterException {
        if (ptuG == ptu) {
            return 'G';
        }
        if (ptuF == ptu) {
            return 'F';
        }
        if (ptuE == ptu) {
            return 'E';
        }
        if (ptuD == ptu) {
            return 'D';
        }
        if (ptuC == ptu) {
            return 'C';
        }
        if (ptuB == ptu) {
            return 'B';
        }
        if (ptuA == ptu) {
            return 'A';
        }

        throw new FiscalPrinterException("Niezdefiniowana stawka w drukarce");
    }

    private List<Payment101> preparePayments(Slip slip) {
        if (!slip.isUsingPayments()) {
            return null;
        }

        List<Payment101> retVal = new ArrayList<Payment101>();
        if (slip.getPaymentAmount(SlipPayment.PaymentType.Cash) != 0.0) {
            retVal.add(new Payment101(0, slip.getPaymentAmount(SlipPayment.PaymentType.Cash), slip.getPaymentName(SlipPayment.PaymentType.Cash)));
        }
        if (slip.getPaymentAmount(SlipPayment.PaymentType.CreditCard) != 0.0) {
            retVal.add(new Payment101(1, slip.getPaymentAmount(SlipPayment.PaymentType.CreditCard), slip.getPaymentName(SlipPayment.PaymentType.CreditCard)));
        }
        if (slip.getPaymentAmount(SlipPayment.PaymentType.Cheque) != 0.0) {
            retVal.add(new Payment101(2, slip.getPaymentAmount(SlipPayment.PaymentType.Cheque), slip.getPaymentName(SlipPayment.PaymentType.Cheque)));
        }
        if (slip.getPaymentAmount(SlipPayment.PaymentType.Bond) != 0.0) {
            retVal.add(new Payment101(3, slip.getPaymentAmount(SlipPayment.PaymentType.Bond), slip.getPaymentName(SlipPayment.PaymentType.Bond)));
        }
        if (slip.getPaymentAmount(SlipPayment.PaymentType.Other) != 0.0) {
            retVal.add(new Payment101(4, slip.getPaymentAmount(SlipPayment.PaymentType.Other), slip.getPaymentName(SlipPayment.PaymentType.Other)));
        }
        if (slip.getPaymentAmount(SlipPayment.PaymentType.Credit) != 0.0) {
            retVal.add(new Payment101(5, slip.getPaymentAmount(SlipPayment.PaymentType.Credit), slip.getPaymentName(SlipPayment.PaymentType.Credit)));
        }
        if (slip.getPaymentAmount(SlipPayment.PaymentType.Account) != 0.0) {
            retVal.add(new Payment101(6, slip.getPaymentAmount(SlipPayment.PaymentType.Account), slip.getPaymentName(SlipPayment.PaymentType.Account)));
        }
        if (slip.getPaymentAmount(SlipPayment.PaymentType.Voucher) != 0.0) {
            retVal.add(new Payment101(7, slip.getPaymentAmount(SlipPayment.PaymentType.Voucher), slip.getPaymentName(SlipPayment.PaymentType.Voucher)));
        }
        //if (slip.getPaymentAmount(SlipPayment.PaymentType.Credit) != 0.0) {
        //    retVal.add(new Payment101(8, slip.getPaymentAmount(SlipPayment.PaymentType.Credit), slip.getPaymentName(SlipPayment.PaymentType.Credit)));
        //} WALUTA
        return retVal;
    }
    private static String prefix = new String(new byte[]{0x1B, 0x50});
    private static String suffix = new String(new byte[]{0x1B, 0x5C});

    private String readSeq(int timeout) {
        StringBuffer buffer = new StringBuffer();
        while (timeout-- != 0) {
            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
            }

            try {
                byte[] b;
                if (inputStream.available() > 0) {
                    b = new byte[1];
                    inputStream.read(b, 0, 1);
                    buffer.append(new String(b));
                }
            } catch (IOException e) {
            }

            if (buffer.indexOf(suffix) != -1) {
                break;
            }
        }

        if (buffer.indexOf(prefix) != -1) {
            buffer.delete(buffer.indexOf(prefix), buffer.indexOf(prefix) + 2);
        }

        if (buffer.indexOf(suffix) != -1) {
            buffer.delete(buffer.indexOf(suffix), buffer.indexOf(suffix) + 2);
        }

        if (timeout > 0) {
            return buffer.toString();
        } else {
            return "";
        }
    }

    private void flushStreams() {
        try {
            Thread.sleep(200);
        } catch (InterruptedException e) {
        }
        try {
            while (inputStream.available() > 0) {
                inputStream.read();
            }
        } catch (IOException e) {
        }
    }
    private boolean printerConnected;

    private void reset() {
        for (int i = 0; i < 7; i++) {
            sendCAN();
            try {
                Thread.sleep(25);
            } catch (InterruptedException e) {
            }
            sendCAN();
            try {
                Thread.sleep(25);
            } catch (InterruptedException e) {
            }
            sendDLE();
            try {
                Thread.sleep(25);
            } catch (InterruptedException e) {
            }
            sendENQ();
            try {
                Thread.sleep(25);
            } catch (InterruptedException e) {
            }
        }
    }

    private byte decodeLBERSTS(String seq) {
        if (seq.equals("")) {
            return -1;
        }

        seq = seq.substring(0, seq.indexOf("#Z"));
        // CO TO JEST ... Blad na nowych POSNETach?
        if (seq.length() == 2 && (int) seq.charAt(0) < 32) {
            System.err.println("#Z ERR: " + seq.length() + " -- " + seq + " : " + (int) seq.charAt(0));
            seq = seq.substring(1);
        }
        return Byte.parseByte(seq);
    }

    private void sendLBTREXITCAN() throws FiscalPrinterException {
        //System.err.println("Anulowanie transakcji.");
        if (!printerConnected) {
            return;
        }

        byte[] seq = ("0$e").getBytes();
        sendPrefix();
        try {
            outputStream.write(seq);
            outputStream.write(calculateCC(seq));
        } catch (IOException e) {
        }
        sendSuffix();

        byte err = decodeLBERSTS(readSeq(1000));
        if (err != 0) {
            throw new FiscalPrinterException("sendLBTREXITCAN " + this + " " + getErrDescription(err));
        }

        sendDLE();
        sendENQ();

        try {
            while (inputStream.available() > 0) {
                inputStream.read();
            }
        } catch (IOException e) {
        }
    }

    private void sendLBDSP() throws FiscalPrinterException {
        if (!printerConnected) {
            return;
        }

        byte[] seq = ("1$d").getBytes();
        sendPrefix();
        try {
            outputStream.write(seq);
            outputStream.write(calculateCC(seq));
        } catch (IOException e) {
        }
        sendSuffix();

        byte err = decodeLBERSTS(readSeq(1000));
        if (err != 0) {
            throw new FiscalPrinterException("sendLBDSP " + this + " " + getErrDescription(err));
        }

        sendDLE();
        sendENQ();
    }

    private void sendLBDAYREP() throws FiscalPrinterException {
        if (!printerConnected) {
            return;
        }

        Date now = new Date();
        @SuppressWarnings("deprecation")
        byte[] seq = ("1;" + (now.getYear() - 100) + ";" + (now.getMonth() + 1) + ";" + now.getDate() + "#r").getBytes();
        sendPrefix();
        try {
            outputStream.write(seq);
            outputStream.write(calculateCC(seq));
        } catch (IOException e) {
        }
        sendSuffix();

        byte err = decodeLBERSTS(readSeq(10000));
        if (err != 0) {
            throw new FiscalPrinterException("sendLBDSP " + this + " " + getErrDescription(err));
        }

        sendDLE();
        sendENQ();
    }

    ////////////////////////////////////////////////////////////////////////////
    ////////////// Sekwencje obslugo paragonu //////////////////////////////////
    private void sendLBTRSHDR(byte pl) throws FiscalPrinterException {
        if (!printerConnected) {
            return;
        }

        byte[] seq = (pl + "$h").getBytes();
        sendPrefix();
        try {
            //System.out.println("LBTRSHDR: " + new String(seq));
            outputStream.write(seq);
            outputStream.write(calculateCC(seq));
        } catch (IOException e) {
        }
        sendSuffix();

        byte err = decodeLBERSTS(readSeq(1000));
        if (err != 0) {
            throw new FiscalPrinterException("sendLBTRSHDR " + this + " " + getErrDescription(err));
        }

        sendDLE();
        sendENQ();
    }

    private String f2(double d) {
        NumberFormat instance = NumberFormat.getInstance(Locale.US);
        instance.setMaximumFractionDigits(2);
        instance.setMinimumFractionDigits(2);
        instance.setGroupingUsed(false);
        return instance.format(d);
    }

    private String f4(double d) {
        NumberFormat instance = NumberFormat.getInstance(Locale.US);
        instance.setMaximumFractionDigits(4);
        instance.setMinimumFractionDigits(4);
        instance.setGroupingUsed(false);
        return instance.format(d);
    }

    private void sendLBTRSLN(int slipLineNo, String name, double amount, double price, char vatRate, double gross, int discountType, double discount) throws FiscalPrinterException {
        if (!printerConnected) {
            return;
        }

        if (discountType == 2 || discountType == 4) {
            discount *= 100.0;
        }
        byte[] seq = ToString.string2Mazovia(slipLineNo + ";" + discountType + "$l" + name + "\r" + f4(amount) + "\r" + vatRate + "/" + f2(price) + "/" + f2(gross) + "/" + f2(discount) + "/");
        sendPrefix();
        try {
            outputStream.write(seq);
            outputStream.write(calculateCC(seq));
        } catch (IOException e) {
        }
        sendSuffix();

        byte err = decodeLBERSTS(readSeq(2000));
        if (err != 0) {
            throw new FiscalPrinterException("sendLBTRSLN " + this + " " + getErrDescription(err));
        }

        sendDLE();
        sendENQ();
    }

    private void sendLBTRXEND1(double total, String cashbox, String cashier, String reference, List<Payment101> payment) throws FiscalPrinterException {
        if (!printerConnected) {
            return;
        }

        double cash = 0.0;

        int noPayments = 0;
        String pfx = "";
        String pfn = "";
        String pfa = "";

        if (payment != null) {
            for (int i = 0; i < payment.size(); i++) {
                if (payment.get(i).type == 0) {
                    cash = payment.get(i).amount;
                    continue;
                }
                noPayments++;
                pfx += payment.get(i).type + ";";
                pfn += payment.get(i).name + "\r";
                pfa += f2(payment.get(i).amount) + "/";
            }
        }

        byte[] seq = ToString.string2Mazovia("3;" + // ilość dodatkowych linii umieszczanych w stopce paragonu, za logo fiskalnym, do których ma dostęp aplikacja = 0...3
                "0;" + // zachowanie ‘dotychczasowe’ tzn. zakończenie drukowania, wysunięcie papieru i zakończenie trybu transakcyjnego
                "1;" + // jeżeli tylko możliwe w jednej grupie to drukuj skrócone podsumowanie
                "0;" + // kwota DSP ujemna
                "0;" + // rodzaj rabatu
                "0;" + // nie występuje blok KAUCJA_POBRANA
                "0;" + // nie występuje blok KAUCJA_ZWROCONA
                "1;" + // występuje string <numer_systemowy>
                noPayments + ";" + // nie ma form płatności, nie występuje blok nazw form płatności
                "0;" + // kwota RESZTA jest ignorowana
                ((cash != 0.0) ? ("1;") : ("0;")) + // kwota kwota WPLATY jest ignorowana (wplata gotówki nie występuje)
                pfx + //  kwota FORM_PLAT jest drukowana, jest to inna forma płatności
                "$y" + // kod rozkazu
                cashbox + "\r" + cashier + "\r" + // kod kasy i kod kasjera
                //"" + reference + "\r" + //nr systemowy - bez kodu kreskowego
                "#" + stripReference(reference) + "#\r" + //nr systemowy - z kodem kreskowym
                footLine1 + "\r" + // linia dodatkowa 1
                footLine2 + "\r" + // linia dodatkowa 2
                footLine3 + "\r" + // linia dodatkowa 3
                pfn + // formy platnosci - nazwy
                f2(total) + "/" + // total
                f2(total) + // DSP
                "/0/" + // RABAT
                f2(cash) + "/" + // WPŁATA
                pfa + // Formy płanosci
                "0/" // RESZTA
        );

        sendPrefix();
        try {
            outputStream.write(seq);
            outputStream.write(calculateCC(seq));
        } catch (IOException e) {
        }
        sendSuffix();

        byte err = decodeLBERSTS(readSeq(10000));
        if (err != 0) {
            throw new FiscalPrinterException("sendLBTRXEND1 " + this + " " + getErrDescription(err));
        }

        sendDLE();
        sendENQ();
    }

    private String stripReference(String reference) {
        if (reference.length() > 16) {
            reference = reference.substring(0, 16);
        }
        return reference;
    }
    private final static String footLine1 = "Serwer wydruku fiskalnego wersja 2.0";
    private final static String footLine2 = "(c)2014 http://fps.bart.prokop.name/";
    private final static String footLine3 = "Sterownik: PosnetThermal 3.01";

    ////////////// Sekwencje obslugo paragonu //////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////
    private void sendLBSERM(byte ps) throws FiscalPrinterException {
        if (!printerConnected) {
            throw new FiscalPrinterException("Brak komunikacji z drukarką.");
        }

        byte[] seq = (ps + "#e").getBytes();
        sendPrefix();
        try {
            outputStream.write(seq);
            outputStream.write(calculateCC(seq));
        } catch (IOException e) {
        }
        sendSuffix();

        byte err = decodeLBERSTS(readSeq(1000));
        if (err != 0) {
            throw new FiscalPrinterException("sendLBSERM " + this + " " + getErrDescription(err));
        }

        sendDLE();
        sendENQ();
    }

    private void sendLBFSTRQ(byte ps) throws FiscalPrinterException {
        if (!printerConnected) {
            return;
        }

        byte[] seq = (ps + "#s").getBytes();
        sendPrefix();
        try {
            outputStream.write(seq);
            //outputStream.write(calculateCC(seq));
        } catch (IOException e) {
        }
        sendSuffix();

        decodeLBFSTRQ(readSeq(1000));

        byte err = decodeLBERSTS(readSeq(1000));
        if (err != 0) {
            throw new FiscalPrinterException("sendLBFSTRQ " + this + " " + getErrDescription(err));
        }

        sendDLE();
        sendENQ();
    }
    private VATRate ptuA;
    private VATRate ptuB;
    private VATRate ptuC;
    private VATRate ptuD;
    private VATRate ptuE;
    private VATRate ptuF;
    private VATRate ptuG;

    private void decodeLBFSTRQ(String seq) {
        seq = seq.substring(seq.indexOf('/') + 1);
        ptuA = decodeLBFSTRQ_PTU(seq.substring(0, seq.indexOf('/')));
        seq = seq.substring(seq.indexOf('/') + 1);
        ptuB = decodeLBFSTRQ_PTU(seq.substring(0, seq.indexOf('/')));
        seq = seq.substring(seq.indexOf('/') + 1);
        ptuC = decodeLBFSTRQ_PTU(seq.substring(0, seq.indexOf('/')));
        seq = seq.substring(seq.indexOf('/') + 1);
        ptuD = decodeLBFSTRQ_PTU(seq.substring(0, seq.indexOf('/')));
        seq = seq.substring(seq.indexOf('/') + 1);
        ptuE = decodeLBFSTRQ_PTU(seq.substring(0, seq.indexOf('/')));
        seq = seq.substring(seq.indexOf('/') + 1);
        ptuF = decodeLBFSTRQ_PTU(seq.substring(0, seq.indexOf('/')));
        seq = seq.substring(seq.indexOf('/') + 1);
        ptuG = decodeLBFSTRQ_PTU(seq.substring(0, seq.indexOf('/')));
        seq = seq.substring(seq.indexOf('/') + 1);
    }

    private VATRate decodeLBFSTRQ_PTU(String s) {
        if (s.equals("22.00")) {
            return VATRate.VAT22;
        }
        if (s.equals("07.00")) {
            return VATRate.VAT07;
        }
        if (s.equals("03.00")) {
            return VATRate.VAT03;
        }
        if (s.equals("23.00")) {
            return VATRate.VAT23;
        }
        if (s.equals("08.00")) {
            return VATRate.VAT08;
        }
        if (s.equals("05.00")) {
            return VATRate.VAT05;
        }
        if (s.equals("00.00")) {
            return VATRate.VAT00;
        }
        if (s.equals("100.00")) {
            return VATRate.VATzw;
        }
        if (s.equals("101.00")) {
            return null;
        }

        System.err.println("Nieobsługiwana stawka podatkowa: " + s);
        return null;
    }

    private boolean waitForOneByte() throws IOException {
        int t = 2000;
        while (t-- != 0) {
            if (inputStream.available() >= 1) {
                return true;
            }
            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
            }
        }
        return false;
    }

    /**
     * Informacyjna postać tekstowa o stanie urządzenia fiskalnego
     *
     * @return Zwraca stan drukarki - status flag.
     */
    @Override
    public String toString() {
        String retValue = "POSNet Thermal @ " + comPortName;

        if (!printerConnected) {
            retValue += " NOT FOUND";
        }

        // DLE flags:
        if (pflONL) {
            retValue += " ONL";
        }
        if (pflPE) {
            retValue += " PE";
        }
        if (pflERR) {
            retValue += " ERR";
        }

        // ENQ flags:
        if (pflFSK) {
            retValue += " FSK";
        }
        if (pflCMD) {
            retValue += " CMD";
        }
        if (pflPAR) {
            retValue += " PAR";
        }
        if (pflTRF) {
            retValue += " TRF";
        }

        return retValue;
    }

    private void sendENQ() {
        if (!printerConnected) {
            return;
        }

        try {
            outputStream.write(0x05);
            if (waitForOneByte()) {
                decodeENQ((byte) inputStream.read());
            }
        } catch (IOException e) {
        }
    }

    private void sendBEL() {
        try {
            outputStream.write(0x07);
        } catch (IOException e) {
        }
    }

    private void sendDLE() {
        try {
            outputStream.write(0x10);
            if (waitForOneByte()) {
                printerConnected = true;
                decodeDLE((byte) inputStream.read());
            } else {
                printerConnected = false;
            }
        } catch (IOException e) {
        }
    }

    private void sendCAN() {
        try {
            outputStream.write(0x18);
        } catch (IOException e) {
        }
    }
    // ENQ flags:
    private boolean pflFSK; // FSK is on if in fiscal mode
    private boolean pflCMD; // CMD is on, if last command executed successfully
    private boolean pflPAR; // PAR is on, when during transaction
    private boolean pflTRF; // TRF is on, when last transaction was successfull

    private void decodeENQ(byte enq) {
        pflFSK = ((enq & 8) == 8);
        pflCMD = ((enq & 4) == 4);
        pflPAR = ((enq & 2) == 2);
        pflTRF = ((enq & 1) == 1);
    }
    // DLE flags:
    private boolean pflONL; // on-line status
    private boolean pflPE;  // PE - paper empty state
    private boolean pflERR; // ERR - machinery / control error

    private void decodeDLE(byte dle) {
        pflONL = ((dle & 4) == 4);
        pflPE = ((dle & 2) == 2);
        pflERR = ((dle & 1) == 1);
    }

    /**
     *
     * @return
     */
    private void connect() throws FiscalPrinterException {
        try {
            serialPort = PortEnumerator.getSerialPort(comPortName);
        } catch (Exception e) {
            throw new FiscalPrinterException("Nie można otworzyć portu: " + e.getMessage());
        }

        try {
            serialPort.setSerialPortParams(9600,
                    SerialPort.DATABITS_8,
                    SerialPort.STOPBITS_1,
                    SerialPort.PARITY_NONE);
            serialPort.setFlowControlMode(SerialPort.FLOWCONTROL_NONE);

            outputStream = serialPort.getOutputStream();
            inputStream = serialPort.getInputStream();
        } catch (UnsupportedCommOperationException e) {
            throw new FiscalPrinterException("Nie można otworzyć portu: UnsupportedCommOperationException: " + e.getMessage());
        } catch (IOException e) {
            throw new FiscalPrinterException("Nie można otworzyć portu: IOException: " + e.getMessage());
        }
    }

    private void disconnect() {
        if (serialPort!=null)
            serialPort.close();
    }

    private void sendPrefix() {
        try {
            outputStream.write(0x1B);
            outputStream.write(0x50);
        } catch (IOException e) {
        }
    }

    private void sendSuffix() {
        try {
            outputStream.write(0x1B);
            outputStream.write(0x5C);
        } catch (IOException e) {
        }
    }

    /**
     *
     * @param seq
     * @return
     */
    private static byte[] calculateCC(byte[] seq) {
        byte check = (byte) 0xFF;

        for (int i = 0; i < seq.length; i++) {
            check ^= seq[i];
        }

        return BitsAndBytes.byteToHexString(check).getBytes();
    }

    private String getErrDescription(byte errNo) {
        switch (errNo) {
            case -1:
                return "TIMEOUT";
            case 0:
                return "Brak błędu";
            case 1:
                return "brak inicjalizacji RTC";
            case 2:
                return "błąd bajtu kontrolnego";
            case 3:
                return "zła ilość parametrów";
            case 4:
                return "błąd parametru / parametrów";
            case 5:
                return "błąd operacji z RTC";
            case 6:
                return "błąd operacji z modułem fiskalnym";
            case 7:
                return "błąd daty";
            case 8:
                return "niezerowe totalizery";
            case 9:
                return "błąd operacji IO";
            case 10:
                return "zmiana czasu poza dopuszczonym zakresem";
            case 11:
                return "zła ilość stawek PTU lub błąd liczby";
            case 12:
                return "błędny nagłówek";
            case 13:
                return "fiskalizacja urządzenia sfiskalizowanego";
            case 14:
                return "nagłówek do RAM dla urządzenia sfiskalizowanego";
            case 16:
                return "błąd pola <nazwa>";
            case 17:
                return "błąd pola <ilość>";
            case 18:
                return "błąd pola <PTU>";
            case 19:
                return "błąd pola <CENA>";
            case 20:
                return "błąd pola <BRUTTO>";
            case 21:
                return "wyłączony tryb transakcji";
            case 22:
                return "błąd STORNO";
            case 23:
                return "zła ilość rekordów";
            case 24:
                return "przepełnienie bufora druk.";
            case 25:
                return "błąd kodu kasy";
            case 26:
                return "błąd kwoty WPLATA";
            case 27:
                return "błąd kwoty TOTAL";
            case 28:
                return "przepełnienie totalizera";
            case 29:
                return "LBTREXIT bez LBTRSHDR";
            case 30:
                return "błąd kwoty WPLATA lub WYPLATA";
            case 31:
                return "nadmiar dodawania";
            case 32:
                return "ujemny wynik";
            case 33:
                return "błąd pola <zmiana>";
            case 34:
                return "błąd pola <kasjer>";
            case 35:
                return "zerowy stan totalizerow";
            case 36:
                return "już jest zapis o tej dacie";
            case 37:
                return "operacja przerwana z klawiatury";
            case 38:
                return "błąd pola <nazwa>";
            case 39:
                return "błąd pola <PTU>";
            case 40:
                return "dodatkowy błąd sekwencji LBTRSHDR: blokada transakcji po błędzie lub zapełnieniu modułu fiskalnego";
            case 41:
                return "błędy dla kart kredytowych wykorzystane kody 41..51";
            case 80:
                return "błąd bazy przy zmianie stawek VAT";
            case 81:
                return "zadanie wydrukowania bazy";
            case 82:
                return "niedozwolona funkcja w bieżącym stanie urządzenia";
            case 83:
                return "$z niezgodna kwota kaucji";
            case 90:
                return "trans tylko kaucjami nie może być towarów";
            case 91:
                return "została wysłana forma płatności nie może być tow";
            case 92:
                return "przepełnienie bazy towarowej";
            case 93:
                return "błąd anulowania formy płatności $b";
            case 94:
                return "przepełnienie kwoty sprzedaży";
            case 95:
                return "drukarka w trybie transakcji operacja niedozwolona";
            case 96:
                return "anulacja na skutek przekroczenia czasu";
            case 97:
                return "słaby akumulator, nie można sprzedawać";
            case 98:
                return "błąd zwory serwisowej";

            default:
                return "NIEZNAY BŁĄD";
        }
    }

    private class Payment101 {

        int type;
        String name;
        double amount;

        public Payment101(int type, double amount, String name) {
            this.type = type;
            this.amount = amount;
            this.name = name;
        }
    }
}
