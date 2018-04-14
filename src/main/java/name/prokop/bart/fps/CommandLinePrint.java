/*
 * Copyright 2018 Bartłomiej P. Prokop
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package name.prokop.bart.fps;

import name.prokop.bart.fps.datamodel.SaleLine;
import name.prokop.bart.fps.datamodel.Slip;
import name.prokop.bart.fps.datamodel.SlipPayment;
import name.prokop.bart.fps.datamodel.VATRate;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 *
 * @author Bartłomiej Prokop
 */
public class CommandLinePrint {

    public static void main(String... args) throws Exception {
//        args = new String[]{"Console", "COM1", "{\"reference\":\"R-k 0123456789\",\"cashier\":\"Bartek Prokop\",\"register\":\"XX99\",\"items\":[{\"description\":\"Test drukarki\",\"amount\":1,\"unitPrice\":0.01,\"vatRate\":\"VAT23\"}]}"};
        if (args.length != 3) {
            printHelp();
            return;
        }

        final FiscalPrinter.Type type = FiscalPrinter.Type.valueOf(args[0]);
        final String comPort = args[1];

        System.out.println("Printer: " + type + ", port: " + comPort + ".");

        final Slip slip = parseSlip(args[2]);

        FiscalPrinter fiscalPrinter = type.getFiscalPrinter(comPort);
        fiscalPrinter.print(slip);
    }

    private static void printHelp() {
        System.out.println("Usage: PrinterType COMx JSON");
        System.out.println("Avaiable PrinterTypes:");
        for (FiscalPrinter.Type t : FiscalPrinter.Type.values()) {
            System.out.print(" " + t.name());
        }
    }

    private static Slip parseSlip(String string) {
        final JSONObject jsonReceipt = new JSONObject(string);

        Slip slip = new Slip();
        slip.setReference(jsonReceipt.optString("reference", null));
        slip.setCashierName(jsonReceipt.optString("cashier", null));
        slip.setCashbox(jsonReceipt.optString("register", null));

        final JSONArray jsonItems = jsonReceipt.optJSONArray("items");
        for (int i = 0; i < jsonItems.length(); i++) {
            final JSONObject jsonItem = jsonItems.getJSONObject(i);
            SaleLine sl = new SaleLine();
            sl.setName(jsonItem.optString("description", null));
            sl.setAmount(jsonItem.optDouble("amount", 1.0));
            sl.setPrice(jsonItem.optDouble("unitPrice", 0.01));
            sl.setTaxRate(VATRate.valueOf(jsonItem.optString("vatRate", "VAT23")));
            slip.addLine(sl);
        }

        slip.addPayment(SlipPayment.PaymentType.Cash, slip.getTotal(), null);

        return slip;
    }

}
