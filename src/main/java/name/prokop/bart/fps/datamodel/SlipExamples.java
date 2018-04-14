package name.prokop.bart.fps.datamodel;

import java.util.Calendar;
import java.util.Random;
import name.prokop.bart.fps.util.StringGenerator;

public class SlipExamples {

    public static Slip demo(Slip slip) {
        if (Calendar.getInstance().get(Calendar.YEAR) >= 2019) {
            return getUnlicensed();
        } else {
            return slip;
        }
    }

    /**
     * Generuje testowy paragon, z obrotem 1 gr. i VAT wynoszącym 0,00 zł
     *
     * @return Przykładowy paragon
     */
    public static Slip getOneCentSlip() {
        Slip slip = new Slip();
        slip.setReference("R-k 0123456789");
        slip.setCashbox("XX99");
        slip.setCashierName("Bartek Prokop");
        slip.addLine("Test drukarki", 1, 0.01, VATRate.VAT23);
        slip.addPayment(SlipPayment.PaymentType.Cash, 0.01, null);
        return slip;
    }

    public static Slip getUnlicensed() {
        Slip slip = new Slip();
        slip.setReference("R-k 0123456789");
        slip.setCashbox("XX99");
        slip.setCashierName("Bartek Prokop");
        slip.addLine("Zaktualizuj serwer", 1, 0.01, VATRate.VAT23);
        slip.addPayment(SlipPayment.PaymentType.Cash, 0.01, null);
        return slip;
    }

    /**
     * Generuje testowy paragon, z obrotem 4 gr. i VAT wynoszącym 0,00 zł
     *
     * @return Przykladowy paragon
     */
    public static Slip getTestSlip() {
        Slip slip = new Slip();
        slip.setReference("R-k " + StringGenerator.generateRandomNumericId(6));
        slip.setCashbox("XX01");
        slip.setCashierName("Bartek Prokop");
        slip.addLine("Towar.23", 1, 0.01, VATRate.VAT23);
        slip.addLine("Towar.08RK", 1, 0.02, VATRate.VAT08, DiscountType.AmountDiscount, 0.01);
        slip.addLine("Towar.08RP", 1, 0.02, VATRate.VAT08, DiscountType.RateDiscount, 0.5);
//        slip.addLine("Towar.08", 1, 0.01, VATRate.VAT08);
//        slip.addLine("Test_05", 1, 0.01, VATRate.VAT05);
        slip.addLine("Towar.00", 1, 0.01, VATRate.VAT00);
        slip.addLine("Towar.ZW", 1, 0.01, VATRate.VATzw);
        return slip;
    }

    public static Slip getTestNoDiscountSlip() {
        Slip slip = new Slip();
        slip.setReference("R-k " + StringGenerator.generateRandomNumericId(6));
        slip.setCashbox("XX01");
        slip.setCashierName("Bartek Prokop");
        slip.addLine("Towar.23", 1, 0.01, VATRate.VAT23);
        slip.addLine("Towar.08", 1, 0.01, VATRate.VAT08);
        slip.addLine("Test_05", 1, 0.01, VATRate.VAT05);
        slip.addLine("Towar.00", 1, 0.01, VATRate.VAT00);
        slip.addLine("Towar.ZW", 1, 0.01, VATRate.VATzw);
        return slip;
    }

    /**
     * Generuje przykładowy paragon, do celów czysto testowych
     *
     * @return Przykładowy paragon
     */
    public static Slip getSampleSlip() {
        Random r = new Random();
        Slip slip = new Slip();

        // numer zewnetrzny paragonu z waszej aplikacji
        slip.setReference("R-k " + StringGenerator.generateRandomNumericId(6));

        for (int i = 1; i <= 2; i++) {
            slip.addLine("Losowy1", Toolbox.round(r.nextInt(10000) / 100.0 + 0.01, 2), 99.99, VATRate.VAT23);
            slip.addLine("Losowy2", Toolbox.round(r.nextInt(10000) / 100.0 + 0.01, 2), Toolbox.round(r.nextInt(10000) / 100.0 + 0.01, 2), VATRate.VAT23);
            slip.addLine("Losowy RABAT", Toolbox.round(r.nextInt(10000) / 100.0 + 0.01, 2), Toolbox.round(r.nextInt(10000) / 100.0 + 50, 2), VATRate.VAT23, DiscountType.AmountDiscount, Toolbox.round(r.nextInt(5000) / 100.0, 2));
            slip.addLine("Losowy3", 9.99, r.nextInt(10000) / 100.0 + 0.01, VATRate.VAT23);
            slip.addLine("Losowy4", Toolbox.round(r.nextInt(10000) / 10000.0 + 0.01, 4), Toolbox.round(r.nextInt(10000) / 100.0 + 1.01, 2), VATRate.VAT08);
            slip.addLine("Losowy4", Toolbox.round(r.nextInt(10000) / 10000.0 + 0.01, 4), Toolbox.round(r.nextInt(10000) / 100.0 + 1.01, 2), VATRate.VAT08);
            slip.addLine("Losowy4", Toolbox.round(r.nextInt(10000) / 10000.0 + 0.01, 4), Toolbox.round(r.nextInt(10000) / 100.0 + 1.01, 2), VATRate.VAT08);
            slip.addLine("Losowy4", Toolbox.round(r.nextInt(10000) / 10000.0 + 0.01, 4), Toolbox.round(r.nextInt(10000) / 100.0 + 1.01, 2), VATRate.VAT08);
            slip.addLine("LosowyZERO", Toolbox.round(r.nextInt(10000) / 10000.0 + 0.01, 4), Toolbox.round(r.nextInt(10000) / 100.0 + 1.01, 2), VATRate.VAT00);
            slip.addLine("LosowyZW", Toolbox.round(r.nextInt(10000) / 10000.0 + 0.01, 4), Toolbox.round(r.nextInt(10000) / 100.0 + 1.01, 2), VATRate.VATzw);
        }
        slip.setCashierName("Jacek Bielarski");
        slip.setCashbox("BP01");

        // mozna nie uzyc formy platnosci w ogole
        slip.addPayment(SlipPayment.PaymentType.Voucher, slip.getTotal(), "Karnet 653214");

        return slip;
    }

}
