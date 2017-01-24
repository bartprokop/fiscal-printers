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
 * @author Bart Prokop
 */
public class Slip {

    /**
     * Dodaje pozycje do paragonu
     *
     * @param name Nazwa towaru
     * @param amount Ilość towaru
     * @param price Cena brutto towaru
     * @param taxRate Stawka VAT na towar
     */
    public final void addLine(String name, double amount, double price, VATRate taxRate) {
        addLine(new SaleLine(name, amount, price, taxRate));
    }

    public final void addLine(String name, double amount, double price, VATRate taxRate, DiscountType discountType, double discount) {
        addLine(new SaleLine(name, amount, price, taxRate, discountType, discount));
    }

    public final void addLine(SaleLine slipLine) {
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

    public boolean isUsingPayments() {
        return slipPayments.size() > 0;
    }

    /**
     * Pozwala ustawić formy płatnosci do wydruku na paragonie fiskalnym
     *
     * @param paymentForm forma płatności
     * @param amount kwota płatnosci
     * @param name dodatkowy opis (w przypadku formy Cash ignorowany)
     */
    public final void addPayment(SlipPayment.PaymentType paymentForm, double amount, String name) {
        if (name != null) {
            if (name.length() > 16) {
                name = name.substring(0, 16);
            }
            name = name.trim();
        }

        SlipPayment slipPayment = getPayment(paymentForm);
        if (slipPayment == null) {
            slipPayment = new SlipPayment();
            slipPayments.add(slipPayment);
            slipPayment.setType(paymentForm);
            slipPayment.setName(name);
        }

        slipPayment.setAmount(Toolbox.round(slipPayment.getAmount() + amount, 2));
    }

    public final void addPayment(SlipPayment payment) {
        SlipPayment slipPayment = getPayment(payment.getType());
        if (slipPayment == null) {
            slipPayments.add(payment);
        } else {
            slipPayment.setAmount(slipPayment.getAmount() + payment.getAmount());
        }
    }

    /**
     * Zwraca uprzednio ustawione formy płatności
     *
     * @param type forma płatności
     * @return wartość danej formy płatności
     */
    public SlipPayment getPayment(SlipPayment.PaymentType type) {
        for (SlipPayment slipPayment : slipPayments) {
            if (slipPayment.getType() == type) {
                return slipPayment;
            }
        }
        return null;
    }

    /**
     * Zwraca uprzednio ustawione formy płatności
     *
     * @param paymentForm forma płatności
     * @return wartość danej formy płatności
     */
    public double getPaymentAmount(SlipPayment.PaymentType paymentForm) {
        SlipPayment sp = getPayment(paymentForm);
        if (sp != null) {
            return sp.getAmount();
        } else {
            return 0.0;
        }
    }

    /**
     * Podaje uprzednio zdefiniowane opisy dla poszczególnych form płatności
     *
     * @param paymentForm typ formy płatności
     * @return dodatkowe określenie dla danej formy płatności
     */
    public String getPaymentName(SlipPayment.PaymentType paymentForm) {
        SlipPayment sp = getPayment(paymentForm);
        if (sp != null) {
            return sp.getName();
        } else {
            return "";
        }
    }

    /**
     * Służy pozyskaniu wartości brutto całego paragonu
     *
     * @return Wartość brutto całego paragonu
     */
    public double getTotal() {
        double sum = 0.0;
        for (int i = 0; i < slipLines.size(); i++) {
            sum += slipLines.get(i).getTotal();
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
     * Powiazane zaplaty do paragonu
     */
    private List<SlipPayment> slipPayments = new ArrayList<>();
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

    public List<SlipPayment> getSlipPayments() {
        return slipPayments;
    }

    public void setSlipPayments(List<SlipPayment> slipPayments) {
        this.slipPayments = slipPayments;
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

}
