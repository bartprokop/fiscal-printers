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
 * Stawki podatku VAT używane przez logikę programu
 *
 * @author Bart
 */
public enum VATRate {

    /**
     * Stawka VAT 22%
     */
    VAT22(0.22),
    /**
     * Stawka VAT 7%
     */
    VAT07(0.07),
    /**
     * Stawka VAT 3%
     */
    VAT03(0.03),
    /**
     * Stawka VAT 0%
     */
    VAT00(0.00),
    /**
     * Stawka zwolniona
     */
    VATzw(0.00),
    /**
     * Nowa stawka 23%
     */
    VAT23(0.23),
    /**
     * Nowa stawka 8%
     */
    VAT08(0.08),
    /**
     * Nowa stawka 5%
     */
    VAT05(0.05);
    private final double vatRate;

    private VATRate(double vatRate) {
        this.vatRate = vatRate;
    }

    public double getVatRate() {
        return vatRate;
    }

    public double calcGross(double net) {
        return net * (1.0 + getVatRate());
    }

    public double calcNet(double gross) {
        return gross / (1.0 + getVatRate());
    }

    public double calcTaxFromNet(double net) {
        return net * getVatRate();
    }

    public double calcTaxFromGross(double gross) {
        return gross / (1.0 + getVatRate());
    }

    @Override
    public String toString() {
        switch (this) {
            case VATzw:
                return "ZW";
            default:
                return Toolbox.round(getVatRate() * 100.0, 0) + "%";
        }

    }

}
