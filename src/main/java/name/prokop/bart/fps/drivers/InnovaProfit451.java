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
 * InnovaProfit451.java
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
public class InnovaProfit451 implements FiscalPrinter {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        FiscalPrinter fp;

        if (args.length != 0) {
            fp = InnovaProfit451.getFiscalPrinter(args[0]);
        } else {
            fp = InnovaProfit451.getFiscalPrinter("COM1");
        }

        try {
            fp.print(SlipExamples.getOneCentSlip());
        } catch (FiscalPrinterException e) {
            System.err.println(e);
        }
    }

    public static FiscalPrinter getFiscalPrinter(String comPortName) {
        return new InnovaProfit451(comPortName);
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
    private InnovaProfit451(String comPortName) {
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

    private void printSlip(Slip slip) throws FiscalPrinterException {
        // rozpocznij transkację
        sendLBTRSHDR((byte) 0);
        // wyślij wszystkie linie paragonu
        for (int i = 0; i < slip.getNoOfLines(); i++) {
            SaleLine line = slip.getLine(i);
            sendLBTRSLN(i + 1, line.getName(), line.getAmount(), line.getPrice(), findPTU(line.getTaxRate()), line.getTotal());
        }
        // zakończ transakcję
        //sendLBTREXIT(slip.getTotal());
        sendLBTRXEND(slip.getTotal(), slip.getCashbox(), slip.getCashierName(), slip.getReference(), preparePayments(slip));

        //Drukarka jest wolna
        try {
            Thread.sleep(4000);
        } catch (InterruptedException e) {
        }

        // otwórz szufladę
        sendLBDSP();
    }

    // for future development
    private synchronized void creditCardSlip() throws FiscalPrinterException {
        try {
            connect();
            reset();
            flushStreams();

            // ustaw pełną, programową obsługę błędów
            sendLBSERM((byte) 3);
            // otwórz szufladę
            sendLBTRSCARD("4 5435", "Bartłomiej Prokop", "12345678", "VISA Classic", "1234 5678 9012 3456", 300, false);
        } finally {
            disconnect();
        }
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

    private PaymentInnova451 preparePayments(Slip slip) {
        PaymentInnova451 retVal = new PaymentInnova451();
        if (!slip.isUsingPayments()) {
            return retVal;
        }

        if (slip.getPaymentAmount(SlipPayment.PaymentType.Cash) != 0.0) {
            retVal.iWPLATA = 1;
            retVal.dWPLATA = slip.getPaymentAmount(SlipPayment.PaymentType.Cash);
        }
        if (slip.getPaymentAmount(SlipPayment.PaymentType.CreditCard) != 0.0) {
            retVal.iKARTA = 1;
            retVal.dKARTA = slip.getPaymentAmount(SlipPayment.PaymentType.CreditCard);
            retVal.nKARTA = slip.getPaymentName(SlipPayment.PaymentType.CreditCard);
        }
        if (slip.getPaymentAmount(SlipPayment.PaymentType.Cheque) != 0.0) {
            retVal.iCZEK = 1;
            retVal.dCZEK = slip.getPaymentAmount(SlipPayment.PaymentType.Cheque);
            retVal.nCZEK = slip.getPaymentName(SlipPayment.PaymentType.Cheque);
        }
        if (slip.getPaymentAmount(SlipPayment.PaymentType.Voucher) != 0.0) {
            retVal.iBON = 1;
            retVal.dBON = slip.getPaymentAmount(SlipPayment.PaymentType.Voucher);
            retVal.nBON = slip.getPaymentName(SlipPayment.PaymentType.Voucher);
        }
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
        for (int i = 0; i < 5; i++) {
            sendCAN();
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
            }
        }

        for (int i = 0; i < 3; i++) {
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

    private void sendLBTRSLN(int slipLineNo, String name, double amount, double price, char vatRate, double gross) throws FiscalPrinterException {
        if (!printerConnected) {
            return;
        }

        //byte[] seq = (slipLineNo+"$l"+name+"\r"+amount+"\r"+vatRate+"/"+price+"/"+gross+"/").getBytes();
        byte[] seq = ToString.string2Mazovia(slipLineNo + "$l" + name + "\r" + amount + "\r" + vatRate + "/" + price + "/" + gross + "/");
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

    private void sendLBTREXIT(double total) throws FiscalPrinterException {
        if (!printerConnected) {
            return;
        }

        byte[] seq = ToString.string2Mazovia(
                "1;" + // pozytywne zakończenie transakcji.
                "0;" + // rabat procentowy dla całej transakcji,
                "3;" + // ilość dodatkowych linii umieszczanych w stopce paragonu, za logo fiskalnym
                "0;" + // zakończenie drukowania i zakończenie trybu transakcyjnego
                "0;" + // brak rabatu
                "1;" + // jeżeli sprzedaż tylko w jednej grupie podatkowej to podsumowanie w formie skróconej
                "$e000\r"
                + "Serwer wydruku fiskalnego wersja 1.1\r" + // linia dodatkowa 1
                "(c) 2001-2007 Bart Prokop.\r" + // linia dodatkowa 2
                "https://bart.prokop.name\r" + // linia dodatkowa 3
                "0/" + // wplata
                total + "/" // total
        );

        sendPrefix();
        try {
            //System.err.println(new String(seq));
            outputStream.write(seq);
            outputStream.write(calculateCC(seq));
        } catch (IOException e) {
        }
        sendSuffix();

        byte err = decodeLBERSTS(readSeq(15000));
        if (err != 0) {
            throw new FiscalPrinterException("sendLBTRXEND1 " + this + " " + getErrDescription(err));
        }

        sendDLE();
        sendENQ();
    }

    private void sendLBTRXEND(double total, String cashbox, String cashier, String reference, PaymentInnova451 payments) throws FiscalPrinterException {
        if (!printerConnected) {
            return;
        }

        byte[] seq = ToString.string2Mazovia(
                "3;" + // ilość dodatkowych linii umieszczanych w stopce paragonu, za logo fiskalnym, do których ma dostęp aplikacja = 0...3
                "0;" + // zakończenie drukowania i odcięcie paragonu i zakończenie trybu transakcyjnego
                "0;" + // jeżeli tylko możliwe w jednej grupie to drukuj skrócone podsumowanie
                "0;" + // rodzaj rabatu
                payments.iWPLATA + ";" + // czy WPLATA
                payments.iKARTA + ";" + // czy KARTA
                payments.iCZEK + ";" + // czy CZEK
                payments.iBON + ";" + // czy BON
                "0;" + // Przyjęcie
                "0;" + // Wydanie
                "0" + // ignoruj RESZTA
                "$x" + "003" + "\r"
                + "Serwer wydruku fiskalnego wersja 1.1\r" + // linia dodatkowa 1
                "(c) 2001-2007 TT Soft Sp. z o.o., BPP.\r" + // linia dodatkowa 2
                "http://www.tt-soft.com/\r" + // linia dodatkowa 3
                "\r" + // LINIA 4 (nie drukowana)
                "\r" + // LINIA 5 (nie drukowana)
                payments.nKARTA + "\r" + // NAZWA KARTA
                payments.nCZEK + "\r" + // NAZWA CZEK
                payments.nBON + "\r" + // NAZWA BON
                total + "/" + // TOTAL
                "0/" + // RABAT
                payments.dWPLATA + "/" + // WPŁATA
                payments.dKARTA + "/" + // KARTA
                payments.dCZEK + "/" + // CZEK
                payments.dBON + "/" + // BON
                "0/" + // Przyjęcie
                "0/" + // Wydanie
                "0/" // Reszta
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
            throw new FiscalPrinterException("sendLBTRXEND " + this + " " + getErrDescription(err));
        }

        sendDLE();
        sendENQ();
    }

    private void sendLBTRSCARD(String salesReference, String cardHolder, String posId, String cardName, String cardNumber, double amount, boolean pinRequired) throws FiscalPrinterException {
        if (!printerConnected) {
            return;
        }

        if (salesReference.length() > 12) {
            salesReference = salesReference.substring(0, 12).trim();
        }

        if (cardHolder.length() > 15) {
            cardHolder = cardHolder.substring(0, 15).trim();
        }

        if (posId.length() > 8) {
            posId = posId.substring(0, 8).trim();
        }

        if (cardName.length() > 16) {
            cardName = cardName.substring(0, 16).trim();
        }

        if (cardNumber.length() > 20) {
            cardNumber = cardNumber.substring(0, 20).trim();
        }

        byte[] seq = ToString.string2Mazovia(
                "1;" + // linia zawierająca numer kasy i kasjera nie jest drukowana, parametry <numer_kasy> i <numer_kasjera> muszą wystąpić.
                "1;" + // zakończenie drukowania i odcięcie paragonu i zakończenie trybu transakcyjnego
                ((pinRequired) ? ("1;") : ("0;")) + // PIN or signature
                "#g" + // rodzaj rabatu
                "01\r" + // nr kasy
                "Jasiu\r" + // ID kasjera
                salesReference + "\r" + // maksymalnie 12 cyfr lub spacji identyfikujący numer potwierdzenia
                cardHolder + "\r" + // maksymalnej długości 15 znaków identyfikujący kontrahenta.
                posId + "\r" + // Łańcuch zawierający maksymalnie 8 cyfr lub spacji identyfikujący numer terminala kart płatniczych.
                cardName + "\r" + // Łańcuch o maksymalnej długości 16 znaków reprezentujący nazwę karty płatniczej.
                cardNumber + "\r" + // Łańcuch o maksymalnej długości 20 znaków reprezentujący numer karty płatniczej
                "01\r" + // Łańcuch zawierający maksymalnie 2 cyfry lub spacje identyfikujący numer miesiąca ważności karty płatniczej
                "01\r" + // Łańcuch zawierający maksymalnie 2 cyfry lub spacje identyfikujący rok ważności karty płatniczej
                "3AL2455\r" + //Łańcuch o maksymalnej długości 9 znaków identyfikujący numer autoryzacji
                amount + "/" // Kwota/wartość transakcji
        );

        sendPrefix();
        try {
            System.err.println(new String(seq));
            outputStream.write(seq);
            outputStream.write(calculateCC(seq));
        } catch (IOException e) {
        }
        sendSuffix();

        byte err = decodeLBERSTS(readSeq(10000));
        if (err != 0) {
            throw new FiscalPrinterException("sendLBTRSCARD " + this + " " + getErrDescription(err));
        }

        sendDLE();
        sendENQ();
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
            System.err.println("Ostatni bład: " + err + " : " + getErrDescription(err));
        }
        //    throw new FiscalPrinterException("sendLBSERM " + this + " " + getErrDescription(err));

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
        //System.err.println(seq);

        ptuA = null;
        ptuB = null;
        ptuC = null;
        ptuD = null;
        ptuE = null;
        ptuF = null;
        ptuG = VATRate.VATzw;

        String s;

        seq = seq.substring(seq.indexOf('/') + 1);
        s = seq.substring(0, seq.indexOf('/'));
        if (s.indexOf('.') != -1) {
            ptuA = decodeLBFSTRQ_PTU(s);
        } else {
            return;
        }

        seq = seq.substring(seq.indexOf('/') + 1);
        s = seq.substring(0, seq.indexOf('/'));
        if (s.indexOf('.') != -1) {
            ptuB = decodeLBFSTRQ_PTU(seq.substring(0, seq.indexOf('/')));
        } else {
            return;
        }

        seq = seq.substring(seq.indexOf('/') + 1);
        s = seq.substring(0, seq.indexOf('/'));
        if (s.indexOf('.') != -1) {
            ptuC = decodeLBFSTRQ_PTU(seq.substring(0, seq.indexOf('/')));
        } else {
            return;
        }

        seq = seq.substring(seq.indexOf('/') + 1);
        s = seq.substring(0, seq.indexOf('/'));
        if (s.indexOf('.') != -1) {
            ptuD = decodeLBFSTRQ_PTU(seq.substring(0, seq.indexOf('/')));
        } else {
            return;
        }

        seq = seq.substring(seq.indexOf('/') + 1);
        s = seq.substring(0, seq.indexOf('/'));
        if (s.indexOf('.') != -1) {
            ptuE = decodeLBFSTRQ_PTU(seq.substring(0, seq.indexOf('/')));
        } else {
            return;
        }

        seq = seq.substring(seq.indexOf('/') + 1);
        s = seq.substring(0, seq.indexOf('/'));
        if (s.indexOf('.') != -1) {
            ptuF = decodeLBFSTRQ_PTU(seq.substring(0, seq.indexOf('/')));
        } else {
            return;
        }

        seq = seq.substring(seq.indexOf('/') + 1);
        s = seq.substring(0, seq.indexOf('/'));
        if (s.indexOf('.') != -1) {
            ptuG = decodeLBFSTRQ_PTU(seq.substring(0, seq.indexOf('/')));
        } else {
            return;
        }

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
                return errNo + " : TIMEOUT";

            case 0:
                return errNo + " : Brak błędu";

            case 1:
                return errNo + " : brak inicjalizacji RTC";
            case 2:
                return errNo + " : błąd bajtu kontrolnego";
            case 3:
                return errNo + " : zła ilość parametrów";
            case 4:
                return errNo + " : błąd parametru / parametrów";
            case 5:
                return errNo + " : błąd operacji z RTC";
            case 6:
                return errNo + " : błąd operacji z modułem fiskalnym";
            case 7:
                return errNo + " : błąd daty";
            case 8:
                return errNo + " : niezerowe totalizery";
            case 9:
                return errNo + " : błąd operacji IO";

            case 10:
                return errNo + " : zmiana czasu poza dopuszczonym zakresem";
            case 11:
                return errNo + " : zła ilość stawek PTU lub błąd liczby";
            case 12:
                return errNo + " : błędny nagłówek";
            case 13:
                return errNo + " : fiskalizacja urządzenia sfiskalizowanego";
            case 14:
                return errNo + " : nagłówek do RAM dla urządzenia sfiskalizowanego";

            case 16:
                return errNo + " : błąd pola <nazwa>";
            case 17:
                return errNo + " : błąd pola <ilość>";
            case 18:
                return errNo + " : błąd pola <PTU>";
            case 19:
                return errNo + " : błąd pola <CENA>";
            case 20:
                return errNo + " : błąd pola <BRUTTO>";
            case 21:
                return errNo + " : wyłączony tryb transakcji";
            case 22:
                return errNo + " : błąd STORNO";

            case 23:
                return errNo + " : zła ilość rekordów";
            case 24:
                return errNo + " : przepełnienie bufora druk.";
            case 25:
                return errNo + " : błąd kodu kasy";
            case 26:
                return errNo + " : błąd kwoty WPLATA";
            case 27:
                return errNo + " : błąd kwoty TOTAL";
            case 28:
                return errNo + " : przepełnienie totalizera";
            case 29:
                return errNo + " : LBTREXIT bez LBTRSHDR";
            case 30:
                return errNo + " : błąd kwoty WPLATA lub WYPLATA";
            case 31:
                return errNo + " : nadmiar dodawania";
            case 32:
                return errNo + " : ujemny wynik";
            case 33:
                return errNo + " : błąd pola <zmiana>";
            case 34:
                return errNo + " : błąd pola <kasjer>";
            case 35:
                return errNo + " : zerowy stan totalizerow";
            case 36:
                return errNo + " : już jest zapis o tej dacie";
            case 37:
                return errNo + " : operacja przerwana z klawiatury";
            case 38:
                return errNo + " : błąd pola <nazwa>";
            case 39:
                return errNo + " : błąd pola <PTU>";
            case 40:
                return errNo + " : dodatkowy błąd sekwencji LBTRSHDR: blokada transakcji po błędzie lub zapełnieniu modułu fiskalnego";
            case 41:
                return errNo + " : błędy dla kart kredytowych wykorzystane kody 41..51";

            case 80:
                return errNo + " : błąd bazy przy zmianie stawek VAT";
            case 81:
                return errNo + " : zadanie wydrukowania bazy";
            case 82:
                return errNo + " : niedozwolona funkcja w bieżącym stanie urządzenia";
            case 83:
                return errNo + " : $z niezgodna kwota kaucji";

            case 90:
                return errNo + " : trans tylko kaucjami nie może być towarów";
            case 91:
                return errNo + " : została wysłana forma płatności nie może być tow";
            case 92:
                return errNo + " : przepełnienie bazy towarowej";
            case 93:
                return errNo + " : błąd anulowania formy płatności $b";
            case 94:
                return errNo + " : przepełnienie kwoty sprzedaży";
            case 95:
                return errNo + " : drukarka w trybie transakcji operacja niedozwolona";
            case 96:
                return errNo + " : anulacja na skutek przekroczenia czasu";
            case 97:
                return errNo + " : słaby akumulator, nie można sprzedawać";
            case 98:
                return errNo + " : błąd zwory serwisowej";

            default:
                return errNo + " : NIEZNAY BŁĄD";
        }
    }

    @Override
    public void printDailyReport() throws FiscalPrinterException {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}

class PaymentInnova451 {

    int iWPLATA = 0;
    int iKARTA = 0;
    int iCZEK = 0;
    int iBON = 0;
    double dWPLATA = 0.0;
    double dKARTA = 0.0;
    double dCZEK = 0.0;
    double dBON = 0.0;
    String nKARTA = "";
    String nCZEK = "";
    String nBON = "";
}
