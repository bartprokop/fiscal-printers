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
package name.prokop.bart.fps.datamodel;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 *
 * @author Bart
 */
public class Invoice {

    /**
     * Dodaje pozycje do paragonu
     *
     * @param name Nazwa towaru
     * @param amount Ilość towaru
     * @param price Cena brutto towaru
     * @param taxRate Stawka VAT na towar
     */
    public void addLine(String name, double amount, double price, VATRate taxRate) {
        addLine(new SaleLine(name, amount, price, taxRate));
    }

    public void addLine(String name, double amount, double price, VATRate taxRate, DiscountType discountType, double discount) {
        addLine(new SaleLine(name, amount, price, taxRate, discountType, discount));
    }

    public void addLine(SaleLine slipLine) {
        slipLines.add(slipLine);
    }

    /**
     * Getter for property noOfLines.
     *
     * @return Value of property noOfLines.
     */
    public int getNoOfLines() {
        return slipLines.size();
    }

    /**
     * Indexed getter for property payment.
     *
     * @param index Index of the property.
     * @return Value of the property at <CODE>index</CODE>.
     */
    public SaleLine getLine(int index) {
        return slipLines.get(index);
    }

    /**
     * Służy pozyskaniu wartości brutto całego paragonu
     *
     * @return Wartość brutto całego paragonu
     */
    public double getTotal() {
        double sum = 0.0;
        for (SaleLine slipLine : slipLines) {
            sum += slipLine.getTotal();
        }
        return Toolbox.roundCurrency(sum);
    }

    /**
     * Enum dla okreslenia stanu danego paragonu
     */
    public enum PrintingState {

        /**
         * Nowo utworzony paragom
         */
        Created,
        /**
         * LOCK - podczas drukowania
         */
        DuringPrinting,
        /**
         * Wydrukowany - wszystko w porzadku jest
         */
        Printed,
        /**
         * Paragon z bledem
         */
        Errored;

        @Override
        public String toString() {
            switch (this) {
                case Created:
                    return "Nowy";
                case DuringPrinting:
                    return "Drukuje sie";
                case Errored:
                    return "Bledny";
                case Printed:
                    return "Wydrukowany";
                default:
                    throw new IllegalStateException();
            }
        }
    }

    @Override
    public String toString() {
        String retVal = "Slip: Reference: " + reference + " Kasa: " + getCashbox() + "\n";
        for (SaleLine sl : slipLines) {
            retVal += sl + "\n";
        }
        retVal += "Suma paragonu: " + getTotal() + " Kasjer: " + getCashierName();
        return retVal;
    }
    /**
     * referencja dla paragonu. musi byc unikalna
     */
    private String reference;
    /**
     * Data utworzenia paragonu
     */
    private Date created = new Date();
    /**
     * Data wydrukowania paragonu (jego fiskalizacji)
     */
    private Date printed = null;
    /**
     * Nazwa kasy - pole na paragonie określające nazwę kasy fiskalnej. Używane
     * również do rozróżnienia gdzie należy kolejkować wydruki.
     */
    private String cashbox;
    /**
     * Imie i Nazwisko kasjera
     */
    private String cashierName;
    /**
     * Linijki paragonu
     */
    private List<SaleLine> slipLines = new ArrayList<>();
    /**
     * Stan paragonu
     */
    private PrintingState printingState = PrintingState.Created;
    /**
     * Opis bledu w paragonie
     */
    private String errorNote = PrintingState.Created.toString();

    public Date getCreated() {
        return created;
    }

    public void setCreated(Date created) {
        this.created = created;
    }

    public Date getPrinted() {
        return printed;
    }

    public void setPrinted(Date printed) {
        this.printed = printed;
    }

    public List<SaleLine> getSlipLines() {
        return slipLines;
    }

    public void setSlipLines(List<SaleLine> slipLines) {
        this.slipLines = slipLines;
    }

    public String getCashbox() {
        return cashbox;
    }

    public void setCashbox(String cashbox) {
        if (cashbox != null) {
            cashbox = cashbox.trim();
        }
        this.cashbox = cashbox;
    }

    public String getCashierName() {
        return cashierName;
    }

    public void setCashierName(String cashierName) {
        if (cashierName != null) {
            cashierName = cashierName.trim();
        }
        this.cashierName = cashierName;
    }

    public PrintingState getPrintingState() {
        return printingState;
    }

    public void setPrintingState(PrintingState printingState) {
        this.printingState = printingState;
    }

    public String getReference() {
        return reference;
    }

    public void setReference(String reference) {
        if (reference != null) {
            reference = reference.trim();
        }
        this.reference = reference;
    }

    public String getErrorNote() {
        return errorNote;
    }

    public void setErrorNote(String errorNote) {
        if (errorNote != null) {
            errorNote = errorNote.trim();
        }
        this.errorNote = errorNote;
    }

    public String getNip() {
        return nip;
    }

    public void setNip(String nip) {
        this.nip = nip;
    }

    public String getPaymentDue() {
        return paymentDue;
    }

    public void setPaymentDue(String paymentDue) {
        this.paymentDue = paymentDue;
    }

    public String getPaymentType() {
        return paymentType;
    }

    public void setPaymentType(String paymentType) {
        this.paymentType = paymentType;
    }

    public String getHeader() {
        return header;
    }

    public void setHeader(String header) {
        this.header = header;
    }
    /**
     * "813-188-60-14"
     */
    private String nip;
    /**
     * "nigdy"
     */
    private String paymentDue;
    /**
     * "przelew"
     */
    private String paymentType;
    /**
     * "Firma\nul. Przemysłowa 9A\n35-111 Rzeszów"
     */
    private String header;
}
