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
public class SlipPayment {

    /**
     * Definiuje możliwe formy zapłaty za paragon
     */
    public enum PaymentType {
        // gotówka, karta kredytowa, czek, bon, inna forma, kredyt (kupiecki)

        /**
         * Gotówka
         */
        Cash,
        /**
         * Karta kredytowa
         */
        CreditCard,
        /**
         * Czek
         */
        Cheque,
        /**
         * Bon
         */
        Bond,
        /**
         * Kredyt
         */
        Credit,
        /**
         * Inna forma płatności
         */
        Other,
        /**
         * voucher
         */
        Voucher,
        /**
         * Konto klienta
         */
        Account;
    }
    private double amount;
    private PaymentType type;
    private String name;

    @Override
    public String toString() {
        return type + ": " + name + ": " + amount;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public PaymentType getType() {
        return type;
    }

    public void setType(PaymentType type) {
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
