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

/**
 *
 * @author Bart
 */
public class SaleLine {

    public SaleLine() {
    }

    /**
     * Creates a new instance of SlipLine na podstawie podanych parametrów
     *
     * @param name Nazwa towaru
     * @param amount Ilość towaru
     * @param price Cena towaru
     * @param taxRate Stawka VAT na dany towar
     */
    public SaleLine(String name, double amount, double price, VATRate taxRate) {
        this.name = name;
        this.amount = amount;
        this.price = Toolbox.roundCurrency(price);
        this.taxRate = taxRate;
    }

    public SaleLine(String name, double amount, double price, VATRate taxRate, DiscountType discountType, double discount) {
        this.name = name;
        this.amount = amount;
        this.price = Toolbox.roundCurrency(price);
        this.taxRate = taxRate;
        this.discountType = discountType;
        if (discountType == DiscountType.AmountDiscount || discountType == DiscountType.AmountExtra) {
            discount = Toolbox.roundCurrency(discount);
        }
        this.discount = discount;
    }

    /**
     * Zwraca wartość brutto danej pozycji paragonu (PRZED RABATEM)
     *
     * @return Wartość brutto danej pozycji paragonu
     */
    public double getGross() {
        return Toolbox.roundCurrency(amount * price);
    }

    public double getTax() {
        double r = getTotal();
        r /= 1 + taxRate.getVatRate();
        r *= taxRate.getVatRate();
        return Toolbox.roundCurrency(r);
    }

    /**
     * Zwraca wartość brutto danej pozycji paragonu (PO RABACIE)
     *
     * @return Wartość brutto danej pozycji paragonu
     */
    public double getTotal() {
        double retVal = amount * price;
        switch (discountType) {
            case AmountDiscount:
                retVal -= discount;
                break;
            case RateDiscount:
                retVal = retVal - retVal * discount;
                break;
            case AmountExtra:
                retVal += discount;
                break;
            case RateExtra:
                retVal = retVal + retVal * discount;
                break;
        }
        return Toolbox.roundCurrency(retVal);
    }
    private String name;
    private double amount;
    private double price;
    private VATRate taxRate;
    private DiscountType discountType = DiscountType.NoDiscount;
    private double discount = 0.0;

    @Override
    public String toString() {
        return ": " + getName() + " A:" + getAmount() + " P:" + getPrice() + " T:" + getTaxRate().name() + " G:" + getGross() + " DT:" + getDiscountType().ordinal() + " D:" + getDiscount() + " T:" + getTotal();
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        if (name != null) {
            name = name.trim();
        }
        this.name = name;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public VATRate getTaxRate() {
        return taxRate;
    }

    public void setTaxRate(VATRate taxRate) {
        this.taxRate = taxRate;
    }

    public double getDiscount() {
        return discount;
    }

    public void setDiscount(double discount) {
        this.discount = discount;
    }

    public DiscountType getDiscountType() {
        return discountType;
    }

    public void setDiscountType(DiscountType discountType) {
        this.discountType = discountType;
    }
}
