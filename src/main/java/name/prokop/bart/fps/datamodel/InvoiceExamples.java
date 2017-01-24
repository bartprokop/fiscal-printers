package name.prokop.bart.fps.datamodel;

import name.prokop.bart.fps.util.StringGenerator;

public class InvoiceExamples {

    /**
     * Generuje testowy paragon, z obrotem 4 gr. i VAT wynoszącym 0,00 zł
     *
     * @return Przykladowy paragon
     */
    public static Invoice getTestInvoice() {
        Invoice slip = new Invoice();
        slip.setReference("R-k " + StringGenerator.generateRandomNumericId(6));
        slip.setCashbox("BP01");
        slip.setCashierName("Bartek Prokop");
        slip.addLine("Towar.22", 1, 0.01, VATRate.VAT22);
//        slip.addLine("Towar.07RK", 1, 0.02, VATRate.VAT07, DiscountType.AmountDiscount, 0.01);
//        slip.addLine("Towar.07RP", 1, 0.02, VATRate.VAT07, DiscountType.RateDiscount, 0.5);
        slip.addLine("Towar.07", 1, 0.01, VATRate.VAT07);
//        slip.addLine("Test_03", 1, 0.01, VATRate.VAT03);
        slip.addLine("Towar.00", 1, 0.01, VATRate.VAT00);
        slip.addLine("Towar.ZW", 1, 0.01, VATRate.VATzw);
        return slip;
    }

    /**
     * Generuje przyk┼éadowy paragon, do cel├│w czysto testowych
     *
     * @return Przyk┼éadowy paragon
     */
    public static Invoice getSampleSlip() {
        Invoice slip = new Invoice();

        // numer zewnetrzny paragonu z waszej aplikacji
        slip.setReference("R-k " + StringGenerator.generateRandomNumericId(6));

        // poszczególne linijki paragonu
        slip.addLine("Deska drewniania", 0.999, 22.13, VATRate.VAT22);
        //slip.addLine("Lejek metalowy", 1.0, 18.01, VATRate.VAT07, DiscountType.AmountDiscount, 18.0);
        slip.addLine("Lejek metalowy", 1.0, 18.01, VATRate.VAT07, DiscountType.RateDiscount, 0.5);
        slip.addLine("Pampers", 1.0, 39.99, VATRate.VAT00);
        //linijka wywo┼éa wyj─ůtek - cena bruto == zero
        //slip.addLine("Drut 15 mm", 0.123456, 0.01, SlipLine.VATRate.VATzw);
        slip.addLine("LiteryĄĆĘÓąćęó", 123, 0.12, VATRate.VAT22);
        slip.addLine("Cement", 9.99, 99.99, VATRate.VAT22);
        slip.addLine("Pustak", 9.99, 0.01, VATRate.VAT07);
        slip.addLine("Beton", 48.0011, 1005.01, VATRate.VAT07);
        //slip.addLine("Mleko szkolne", 5, 3, SlipLine.VATRate.VAT03);
        slip.addLine("Paliwo", 34.87, 4.37, VATRate.VAT07);
        slip.addLine("Paliwo", 34.871, 4.37, VATRate.VAT07);
        slip.addLine("Paliwo", 34.872, 4.37, VATRate.VAT07);
        slip.addLine("Paliwo", 34.873, 4.37, VATRate.VAT07);
        slip.addLine("Paliwo", 34.874, 4.37, VATRate.VAT07);
        slip.addLine("Paliwo", 34.875, 4.37, VATRate.VAT07);
        slip.addLine("Paliwo", 34.876, 4.37, VATRate.VAT07);
        slip.addLine("Paliwo", 34.877, 4.37, VATRate.VAT07);
        slip.addLine("Paliwo", 34.878, 4.37, VATRate.VAT07);
        slip.addLine("Paliwo", 34.879, 4.37, VATRate.VAT07);
        slip.addLine("Paliwo", 34.880, 4.37, VATRate.VAT07);

        slip.setCashierName("Jacek Bielarski");
        slip.setCashbox("BP01");
        return slip;
    }

}
