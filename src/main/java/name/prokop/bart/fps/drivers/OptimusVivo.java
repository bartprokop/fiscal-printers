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
 * OptimusVivo.java
 *
 * Created on 22 styczeń 2005, 21:58
 */
package name.prokop.bart.fps.drivers;

import gnu.io.SerialPort;
import gnu.io.UnsupportedCommOperationException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
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
public class OptimusVivo implements FiscalPrinter {

    private final String footerLine1 = "Dziękujemy";
    private final String footerLine2 = "Zapraszamy ponownie";
    private final String footerLine3 = "Driver: OptimusVivo";

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        FiscalPrinter fp;

        if (args.length != 0) {
            fp = OptimusVivo.getFiscalPrinter(args[0]);
        } else {
            fp = OptimusVivo.getFiscalPrinter("COM1");
        }

        try {
            //fp.openDrawer();
            fp.print(SlipExamples.getOneCentSlip());
        } catch (FiscalPrinterException e) {
            System.err.println(e);
        }
    }

    public static FiscalPrinter getFiscalPrinter(String comPortName) {
        return new OptimusVivo(comPortName);
    }
    /**
     * Creates a new instance of OptimusVivo
     */
    String comPortName;

    /**
     * Tworzy obiekt zdolny do wymuszenia na drukarce fiskalnej Optimus VIVO
     * wydruku paragonu fiskalnego, zapisanego w klasie Slip
     *
     * @param comPortName Nazwa portu szeregowego, do którego jest przyłączona
     * drukarka fiskalna.
     */
    private OptimusVivo(String comPortName) {
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
        try {
            connect();
            flushStreams();
            reset();

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

    @Override
    public void print(Invoice invoice) throws FiscalPrinterException {
        throw new FiscalPrinterException(new UnsupportedOperationException("Not supported yet."));
    }

    @Override
    public synchronized void openDrawer() throws FiscalPrinterException {
        try {
            connect();
            flushStreams();
            reset();

            // ustaw pełną, programową obsługę błędów
            sendLBSERM((byte) 3);
            // otwórz szufladę
            sendLBDSP();
        } finally {
            disconnect();
            //Do testów jak się wysyła.
            //System.exit(-10);
        }
    }

    private void printSlip(Slip slip) throws FiscalPrinterException {
        // rozpocznij transkację
        sendLBTRSHDR((byte) 0);
        // wyślij wszystkie linie paragonu
        for (int i = 0; i < slip.getNoOfLines(); i++) {
            SaleLine line = slip.getLine(i);
            sendLBTRSLN(i + 1, line.getName(), line.getAmount(), line.getPrice(), findPTU(line.getTaxRate()), line.getGross(), line.getDiscountType().ordinal(), line.getDiscount());
        }
        // zakończ transakcję
        sendLBTRXEND1(slip.getTotal(), slip.getCashbox(), slip.getCashierName(), slip.getReference(), preparePayments(slip));

        // otwórz szufladę
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

    private List<PaymentVivo> preparePayments(Slip slip) {
        if (!slip.isUsingPayments()) {
            return null;
        }

        List<PaymentVivo> retVal = new ArrayList<>();
        if (slip.getPaymentAmount(SlipPayment.PaymentType.Cash) != 0.0) {
            retVal.add(new PaymentVivo(0, slip.getPaymentAmount(SlipPayment.PaymentType.Cash), slip.getPaymentName(SlipPayment.PaymentType.Cash)));
        }
        if (slip.getPaymentAmount(SlipPayment.PaymentType.CreditCard) != 0.0) {
            retVal.add(new PaymentVivo(1, slip.getPaymentAmount(SlipPayment.PaymentType.CreditCard), slip.getPaymentName(SlipPayment.PaymentType.CreditCard)));
        }
        if (slip.getPaymentAmount(SlipPayment.PaymentType.Cheque) != 0.0) {
            retVal.add(new PaymentVivo(2, slip.getPaymentAmount(SlipPayment.PaymentType.Cheque), slip.getPaymentName(SlipPayment.PaymentType.Cheque)));
        }
        if (slip.getPaymentAmount(SlipPayment.PaymentType.Voucher) != 0.0) {
            retVal.add(new PaymentVivo(3, slip.getPaymentAmount(SlipPayment.PaymentType.Voucher), slip.getPaymentName(SlipPayment.PaymentType.Voucher)));
        }
        if (slip.getPaymentAmount(SlipPayment.PaymentType.Other) != 0.0) {
            retVal.add(new PaymentVivo(4, slip.getPaymentAmount(SlipPayment.PaymentType.Other), slip.getPaymentName(SlipPayment.PaymentType.Other)));
        }
        if (slip.getPaymentAmount(SlipPayment.PaymentType.Credit) != 0.0) {
            retVal.add(new PaymentVivo(5, slip.getPaymentAmount(SlipPayment.PaymentType.Credit), slip.getPaymentName(SlipPayment.PaymentType.Credit)));
        }
        return retVal;
    }
    private static String prefix = new String(new byte[]{0x1B, 0x50});
    private static String suffix = new String(new byte[]{0x1B, 0x5C});

    private String readSeq(int timeout) {
        StringBuilder buffer = new StringBuilder();
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

        //System.err.println(buffer);
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
        for (int i = 0; i < 3; i++) {
            sendCAN();
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
            }
        }

        for (int i = 0; i < 2; i++) {
            sendDLE();
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
            }
            sendENQ();
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
            }
        }
    }

    private byte decodeLBERSTS(String seq) {
        if (seq.equals("")) {
            return -1;
        }

        seq = seq.substring(0, seq.indexOf("#Z"));
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

    private void sendLBTRSHDR(byte pl) throws FiscalPrinterException {
        if (!printerConnected) {
            return;
        }

        byte[] seq = (pl + "$h").getBytes();
        sendPrefix();
        try {
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

    private void sendLBTRSLN(int slipLineNo, String name, double amount, double price, char vatRate, double gross, int discountType, double discount) throws FiscalPrinterException {
        if (!printerConnected) {
            return;
        }

        //byte[] seq = (slipLineNo+"$l"+name+"\r"+amount+"\r"+vatRate+"/"+price+"/"+gross+"/").getBytes();
        //byte[] seq = ToString.string2Mazovia(slipLineNo+"$l"+name+"\r"+amount+"\r"+vatRate+"/"+price+"/"+gross+"/");
        if (discountType == 2 || discountType == 4) {
            discount *= 100.0;
        }
        byte[] seq = ToString.string2Mazovia(slipLineNo + ";" + discountType + "$l" + name + "\r" + amount + "\r" + vatRate + "/" + price + "/" + gross + "/" + discount + "/");
        sendPrefix();
        try {
            //System.err.println(new String(seq));
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

    private void sendLBTRXEND1(double total, String cashbox, String cashier, String reference, List<PaymentVivo> payment) throws FiscalPrinterException {
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
                pfa += payment.get(i).amount + "/";
                //System.out.println(pfx);
                //System.out.println(pfn);
                //System.out.println(pfa + "  " + payment.get(i).amount);
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
                reference + "\r" + //nr systemowy
                footerLine1 + "\r" + // linia dodatkowa 1
                footerLine2 + "\r" + // linia dodatkowa 2
                footerLine3 + "\r" + // linia dodatkowa 3
                pfn + // formy platnosci - nazwy
                total + "/" + // total
                total + // DSP
                "/0/" + // RABAT
                cash + "/" + // WPŁATA
                pfa + // Formy płanosci
                "0/" // RESZTA
        );

        sendPrefix();
        try {
            //System.err.println(new String(seq));
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

    private void showSerialStatus() {
        try {
            Thread.sleep(2000);
            System.out.println("Wolne bajty: " + inputStream.available());
        } catch (IOException | InterruptedException e) {
        }
    }

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

        //showSerialStatus();
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

//        byte err = decodeLBERSTS(readSeq(5000));
//        if (err != 0)
//            throw new FiscalPrinterException("sendLBFSTRQ " + this + " "+ getErrDescription(err));
//            System.err.println("1111");
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
        //System.err.println();
        //System.err.println(seq);
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
                decodeENQ(readOneByteAnswer());
            }
        } catch (IOException e) {
        }

        //String seq = readSeq(1000);
        //System.err.println(">"+seq+"<");
    }

    private byte readOneByteAnswer() {
        byte answer = 0x00;
        try {
            if (inputStream.available() == 0) {
                Thread.sleep(150);
            }
            answer = (byte) inputStream.read();
            Thread.sleep(100);
            if (answer == 0x1b) {
                while (inputStream.available() > 0) {
                    answer = (byte) inputStream.read();
                }
            } else {
                while (inputStream.available() > 0) {
                    inputStream.read();
                }
            }
        } catch (IOException | InterruptedException e) {
        }
        return answer;
    }

    private void sendBEL() {
        try {
            outputStream.write(0x07);
        } catch (IOException e) {
        }
        readOneByteAnswer();
    }

    private void sendDLE() {
        try {
            outputStream.write(0x10);
            if (waitForOneByte()) {
                printerConnected = true;
                decodeDLE(readOneByteAnswer());
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
        readOneByteAnswer();
    }
    // ENQ flags:
    private boolean pflFSK; // FSK is on if in fiscal mode
    private boolean pflCMD; // CMD is on, if last command executed successfully
    private boolean pflPAR; // PAR is on, when during transaction
    private boolean pflTRF; // TRF is on, when last transaction was successfull

    private void decodeENQ(byte enq) {
        //System.err.println("ENQ: " + ToString.byteToHexString(enq));
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
        //System.err.println("DLE: " + ToString.byteToHexString(dle));
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

        //System.out.println(name.prokop.bart.util.ToString.byteToHexString(check));
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

    @Override
    public void printDailyReport() throws FiscalPrinterException {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}

class PaymentVivo {

    int type;
    String name;
    double amount;

    public PaymentVivo(int type, double amount, String name) {
        this.type = type;
        this.amount = amount;
        this.name = name;
    }
}
